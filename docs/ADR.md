# StockPulse Architecture Decision Record

StockPulse is a Spring Boot backend with a small React/Vite demonstration client. This record describes the architecture that exists in the hackathon implementation.

## Decision 1: Commerce logic placement
### Context
Commerce recommendations combine product state, inventory thresholds, demand velocity, pricing, and reorder calculations. The system has synchronous HTTP/manual recommendation callers and asynchronous trigger-driven callers.

Embedding this logic in `ProductController`, `ProductService`, or an event listener would couple transport and workflow code to one recommendation implementation.

### Options Considered
- Embed pricing and reorder rules directly in controllers or services.
- Put all recommendation logic in the asynchronous listener.
- Define a `CommerceStrategy` abstraction and keep recommendation implementations behind it.

### Decision
Use `com.stockpulse.commerce.CommerceStrategy` as the recommendation boundary. It accepts a `Product` and `TriggerReason`, and returns a `CommerceRecommendation` containing both pricing and reorder results.

HTTP/manual callers use the strategy through `ProductService`. Asynchronous agentic callers use the same contract through `CommerceRecommendationEventListener`.

### Tradeoffs
This adds an interface and explicit dependency wiring, but keeps controllers, product mutation code, and event processing independent of recommendation details. It also makes strategy-specific testing possible. The current implementation remains intentionally small and does not introduce a general plugin or workflow framework.

## Decision 2: Unified commerce contract
### Context
A product recommendation event needs both a pricing recommendation and a reorder recommendation. The current persistence flow also creates both suggestion types from one strategy call.

### Options Considered
- Use separate `PricingStrategy` and `ReorderStrategy` contracts.
- Use one unified contract that returns both recommendations.
- Return an untyped map or provider-specific response object.

### Decision
Use one `CommerceStrategy` contract returning `CommerceRecommendation`, with `PricingRecommendation` and `ReorderRecommendation` as separate result values.

Separate pricing and reorder strategy contracts would provide more independent composition, but would require coordinating two strategy calls and two trigger decisions for the current workflow. The unified contract fits the hackathon requirement that one trigger produces both outputs while preserving future extension through additional `CommerceStrategy` implementations and typed recommendation objects.

### Tradeoffs
The contract couples the current strategy call to both commerce concerns, even when a caller needs only one output. In exchange, the event listener gets consistent trigger context and a single recommendation operation, reducing partial-result handling in the demo.

## Decision 3: Runtime strategy / AI vs rule-based behavior
### Context
The code contains both `RuleBasedCommerceStrategy` and `AICommerceStrategy`, and both implement `CommerceStrategy`. The application does not contain runtime strategy switching or a strategy selection property.

### Options Considered
- Select a strategy dynamically at runtime through configuration.
- Use the deterministic rule strategy everywhere.
- Use explicit dependency qualifiers for the current synchronous and asynchronous paths.

### Decision
Use explicit wiring as implemented:

- `ProductService` injects `@Qualifier("RULE_BASED") CommerceStrategy`, so manual HTTP recommendation endpoints use the deterministic rule-based strategy.
- `CommerceRecommendationEventListener` injects `@Qualifier("AI") CommerceStrategy`, so trigger-driven asynchronous recommendations use the AI strategy.
- `AICommerceStrategy` injects `@Qualifier("RULE_BASED")` as its fallback.

There is no claim of runtime switching without restart. Changing which bean is used requires a code/configuration change and application restart; no runtime selector currently exists.

### Tradeoffs
Rule-based recommendations are deterministic, explainable, and available without an external provider. AI recommendations can provide richer context-sensitive reasoning but depend on configuration, network availability, provider behavior, and response validity. Explicit qualifiers keep the current demo behavior clear, but the split between manual and asynchronous callers is not a general runtime selection mechanism.

## Decision 4: LLM failure and safety
### Context
An external LLM can time out, return an HTTP error, be unavailable because configuration is incomplete, return invalid JSON, or return values that are unsafe for a product recommendation.

### Options Considered
- Persist whatever the provider returns.
- Drop the asynchronous recommendation when the provider fails.
- Validate the response and fall back to deterministic rules.

### Decision
`LLMGateway` uses configured provider, API key, model, base URL, and timeout values. It does not hardcode credentials. `AICommerceStrategy` treats gateway errors, timeouts, invalid JSON, missing fields, invalid directions, and invalid numeric values as AI failure.

Before creating a `CommerceRecommendation`, the AI response is validated:

- Recommended price must be positive and remain within 50% to 150% of the current price.
- Recommended quantity must be at least 1.
- Pricing and reorder confidence values must be between 0 and 1.
- Required reasoning and response fields must be present and valid.

On failure, `AICommerceStrategy` delegates to `RuleBasedCommerceStrategy`. This keeps an asynchronous trigger from silently producing no recommendation: the listener receives a deterministic recommendation and persists both pending suggestion types.

### Tradeoffs
The bounded price range rejects potentially useful but aggressive AI recommendations, but limits unsafe demo output. Fallback preserves availability and explainability, while hiding provider-specific failure details from the recommendation consumer unless additional observability is added later.

## Decision 5: Agentic event-driven decoupling
### Context
Stock and product mutation requests must remain responsive. Low-stock and demand-spike detection can invoke strategy work, external LLM calls, and two persistence operations.

### Options Considered
- Perform recommendation generation directly inside the stock or product request.
- Publish an application event and process it asynchronously.
- Add a broker or distributed event platform for the hackathon flow.

### Decision
`ProductService` publishes `CommerceRecommendationEvent` after a saved product meets a trigger condition:

- `stockLevel < reorderThreshold` publishes `INVENTORY_LOW` and takes precedence.
- Otherwise, `demandVelocity > 3 * categoryAverageDemandVelocity` publishes `DEMAND_SPIKE`.

`CommerceRecommendationEventListener` handles the event with `@Async` and `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. The listener uses `@Transactional(propagation = Propagation.REQUIRES_NEW)` because the original request transaction has already committed and the listener needs its own transaction for recommendation persistence.

The listener generates both recommendations and checks for existing `PENDING` suggestions by product, trigger reason, and suggestion type before saving. An in-process per-product/trigger lock reduces duplicate work from concurrent events.

### Tradeoffs
The request path stays fast and external AI work is decoupled from the HTTP response, but suggestions are eventually consistent and do not appear until asynchronous processing and frontend polling complete. The lock is process-local; it does not provide distributed duplicate prevention across multiple application instances. No external broker is introduced for the hackathon.

## Decision 6: Human approval checkpoint
### Context
Recommendations are advisory. Automatically changing live price or stock would remove merchandising control and could apply an incorrect AI or rule-based result without review.

### Options Considered
- Apply recommendations immediately when they are generated.
- Persist recommendations as pending and require explicit acceptance.
- Allow acceptance and rejection without product mutation.

### Decision
Persist pricing and reorder recommendations as `PENDING` and require explicit human action:

- Accepting a pricing suggestion updates `Product.currentPrice`, then marks the suggestion `ACCEPTED` in the same transactional service operation.
- Accepting a reorder suggestion increases `Product.stockLevel`, restores an out-of-stock product to `ACTIVE` when applicable, then marks the suggestion `ACCEPTED`.
- Rejecting either suggestion marks it `REJECTED` and leaves the product unchanged.
- Already finalized suggestions cannot be accepted or rejected again and return a conflict response through the API.

### Tradeoffs
The checkpoint adds a human step and means recommendations do not change inventory or price immediately. It provides a clear merchandising control, supports audit-friendly suggestion state, and prevents unreviewed AI output from directly changing product data.

## Extensibility Points

The current code provides these concrete extension points:

- Additional `CommerceStrategy` implementations can implement the `CommerceStrategy` interface and return the existing unified recommendation types.
- `LLMGateway` isolates OpenAI-compatible HTTP interaction and reads provider, model, base URL, API key, and timeout configuration from `application.properties`/environment variables.
- `TriggerReason` supports `INITIAL`, `INVENTORY_LOW`, `DEMAND_SPIKE`, and `MANUAL` contexts.
- `SuggestionStatus` supports `PENDING`, `ACCEPTED`, and `REJECTED` lifecycle states.
- `Product` includes optional `costPrice`, `marginFloor`, and `supplierId` fields in addition to core inventory and demand fields.
- JPA entities and repositories provide persistence extension points for products, pricing suggestions, reorder suggestions, and inventory snapshots.
- The typed `PricingRecommendation`, `ReorderRecommendation`, and `CommerceRecommendation` objects provide a stable boundary for additional recommendation metadata.

## Deliberate Scope Exclusions

The current hackathon implementation intentionally does not include:

- Storefront, cart, checkout, or payment workflows.
- Authentication or authorization around product and approval APIs.
- Server-sent events or push notification delivery; the demo frontend polls.
- Advanced analytics, charts, forecasting, or reporting dashboards.
- Runtime strategy switching or a restart-free AI/rule selection mechanism.
- A distributed broker, distributed lock, or database uniqueness constraint for multi-instance event deduplication.
- A dedicated order endpoint; the demo uses the existing product update path to simulate a sale.
