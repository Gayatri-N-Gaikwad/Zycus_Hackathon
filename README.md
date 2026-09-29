# StockPulse Architecture and Hackathon Demo

## Project Overview

StockPulse is a Spring Boot inventory and commerce recommendation system with a React/Vite demo dashboard. It monitors product stock and demand, generates pricing and reorder suggestions, and keeps a human approval step before product data changes.

This is a recommendation and merchandising system. It is not a storefront, cart, checkout, or payment system.

## Problem

Retail teams need timely answers to two operational questions:

- When should inventory be replenished before a stockout?
- How should price respond to constrained inventory or unusual demand?

Manual analysis can delay decisions and make it difficult to apply consistent pricing and reorder logic.

## Solution

StockPulse combines product data, deterministic commerce rules, optional LLM-backed recommendations, asynchronous trigger processing, persisted suggestions, and human approval APIs. The demo frontend polls the backend and exposes the recommendation workflow in one dashboard.

## Architecture / Main Flow

1. A product is created or updated through the REST API. The demo's **Record sale** action uses `PUT /api/products/{id}` to lower `stockLevel`.
2. `ProductService` saves the product and checks triggers after the save.
3. Stock below `reorderThreshold` publishes `CommerceRecommendationEvent` with `INVENTORY_LOW`. This check takes precedence.
4. Otherwise, demand velocity greater than three times the category average publishes `DEMAND_SPIKE`.
5. `CommerceRecommendationEventListener` handles the event asynchronously after the original transaction commits.
6. The listener calls `AICommerceStrategy`, which falls back to `RuleBasedCommerceStrategy` on AI failure.
7. Both pricing and reorder recommendations are persisted as `PENDING`, unless an equivalent pending suggestion already exists.
8. The frontend polls every two seconds and displays pending suggestions for human review.

## Domain Model

### Product

The central inventory item: `id`, `sku`, `name`, `category`, `currentPrice`, `stockLevel`, `reorderThreshold`, `demandVelocity`, and `status`. Optional extension fields are `costPrice`, `marginFloor`, and `supplierId`.

### PricingSuggestion

Stores `currentPrice`, `recommendedPrice`, `changeDirection`, confidence, reasoning, `status`, and `triggerReason` for a product.

### ReorderSuggestion

Stores `currentStock`, `recommendedQuantity`, `suggestedLeadTimeDays`, confidence, reasoning, `status`, and `triggerReason` for a product.

### InventorySnapshot

An implemented JPA entity for a point-in-time product inventory record: product ID, stock level, demand velocity, and capture time. It is persisted through `InventorySnapshotRepository` and is not currently exposed as a dedicated REST workflow.

### Enums

- `ProductStatus`: `ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`
- `SuggestionStatus`: `PENDING`, `ACCEPTED`, `REJECTED`
- `TriggerReason`: `INITIAL`, `INVENTORY_LOW`, `DEMAND_SPIKE`, `MANUAL`
- `ChangeDirection`: `INCREASE`, `DECREASE`, `HOLD`
- `Category`: `ELECTRONICS`, `APPAREL`, `HOME`

## Commerce Engine

### CommerceStrategy

`CommerceStrategy` is the unified contract:

```java
CommerceRecommendation recommend(Product product, TriggerReason triggerReason)
```

The result contains both a `PricingRecommendation` and a `ReorderRecommendation`.

### RuleBasedCommerceStrategy

`RuleBasedCommerceStrategy` is deterministic:

- Low stock: current price multiplied by 1.10, `INCREASE`.
- Demand spike: current price multiplied by 1.05, `INCREASE`.
- Normal conditions: current price retained, `HOLD`.
- Reorder quantity: `max(1, (reorderThreshold * 3) - currentStock)`.

Within the strategy, low stock takes pricing precedence and demand-spike pricing uses a two-times category-average comparison. The event trigger itself uses the stricter three-times category-average threshold described in the main flow.

## AI Advisor

`AICommerceStrategy` implements the same `CommerceStrategy` contract and is used by the asynchronous event listener. It sends different prompts for `INVENTORY_LOW` and `DEMAND_SPIKE`, requesting both pricing and reorder JSON fields.

`LLMGateway` uses Spring `RestClient` to call an OpenAI-compatible `/chat/completions` endpoint. It reads provider, API key, model, base URL, and timeout from configuration. AI output is validated before it becomes a recommendation, including required fields, confidence bounds, quantity minimums, and a price range between 50% and 150% of the current price.

AI errors, timeouts, invalid JSON, and invalid values fall back to the deterministic rule-based strategy.

## Agentic Recommendation Loop

`ProductService` publishes an application event after product persistence when a trigger is met. `CommerceRecommendationEventListener` uses:

- `@Async` for asynchronous processing.
- `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` so the listener does not race the product update transaction.
- `@Transactional(propagation = Propagation.REQUIRES_NEW)` for its own persistence transaction.

The listener generates both suggestion types and checks for an existing `PENDING` suggestion for the same product, trigger reason, and type. Duplicate prevention is process-local; distributed locking is not implemented.

## Human Approval

Recommendations never directly change a product price or stock.

- Accept pricing: sets `Product.currentPrice` to the recommended price and marks the suggestion `ACCEPTED` atomically.
- Accept reorder: increases product stock by the recommended quantity and marks the suggestion `ACCEPTED` atomically.
- Reject either type: marks the suggestion `REJECTED` and leaves the product unchanged.
- Already finalized suggestions cannot be accepted or rejected again.

## REST API

Base URL: `http://localhost:8080`

### Products

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/products` | List products |
| `GET` | `/api/products?status=ACTIVE&category=ELECTRONICS` | Filter products |
| `GET` | `/api/products/{id}` | Get one product |
| `POST` | `/api/products` | Create a product |
| `PUT` | `/api/products/{id}` | Update a product; the frontend uses this to simulate a sale by lowering stock |
| `DELETE` | `/api/products/{id}` | Delete a product |

### Pricing Suggestions

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/products/{id}/pricing-suggestions` | List pricing suggestions |
| `GET` | `/api/products/{id}/pricing-suggestions?status=PENDING` | List pending pricing suggestions |
| `POST` | `/api/products/{id}/pricing-suggestions` | Generate a manual pricing suggestion |
| `POST` | `/api/products/{id}/pricing-suggestions/{suggestionId}/accept` | Accept a pricing suggestion |
| `POST` | `/api/products/{id}/pricing-suggestions/{suggestionId}/reject` | Reject a pricing suggestion |

### Reorder Suggestions

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/products/{id}/reorder-suggestions` | List reorder suggestions |
| `GET` | `/api/products/{id}/reorder-suggestions?status=PENDING` | List pending reorder suggestions |
| `POST` | `/api/products/{id}/reorder-suggestions` | Generate a manual reorder suggestion |
| `POST` | `/api/products/{id}/reorder-suggestions/{suggestionId}/accept` | Accept a reorder suggestion |
| `POST` | `/api/products/{id}/reorder-suggestions/{suggestionId}/reject` | Reject a reorder suggestion |

## Frontend

The React/Vite dashboard displays products, stock, thresholds, price, demand velocity, status, and the first pending pricing and reorder suggestion for each product. It shows trigger, confidence, reasoning, and recommended values, and provides accept, reject, and Record sale actions.

The frontend polls the suggestion endpoints every two seconds. Vite uses port `5173` by default and may use `5174` if `5173` is already occupied. The backend explicitly allows both local frontend origins through `@CrossOrigin`.

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Spring Web and Spring `RestClient`
- Spring Data JPA and Hibernate
- H2 in-memory database
- Spring application events and `@Async`
- React and Vite
- Maven and npm

## Environment Configuration

Backend defaults are in `backend/src/main/resources/application.properties`:

```text
server.port=8080
llm.provider=${LLM_PROVIDER:}
llm.api-key=${LLM_API_KEY:}
llm.model=${LLM_MODEL:}
llm.base-url=${LLM_BASE_URL:}
llm.timeout-ms=${LLM_TIMEOUT_MS:5000}
```

The LLM values are optional for rule-based operation. Never commit a real API key. The frontend can override its backend origin with `VITE_API_BASE_URL`; otherwise it uses `http://localhost:8080`.

## Running Locally

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Start the frontend in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the Vite URL shown in the terminal, normally `http://localhost:5173` or `http://localhost:5174`.

## Demo Walkthrough

1. Start the backend and frontend.
2. Open the dashboard and choose a product whose stock can be lowered below its reorder threshold.
3. Set a sale quantity and click **Record sale**.
4. The product stock decreases through the existing product update API.
5. The backend publishes an `INVENTORY_LOW` event after commit and processes it asynchronously.
6. Pricing and reorder suggestions are persisted as `PENDING`.
7. Frontend polling displays the pending suggestions, including trigger, confidence, reasoning, and recommended values.
8. Click **Accept** on the pricing suggestion.
9. The product price changes to the recommended price and the pricing suggestion becomes `ACCEPTED`.

## Testing

Run backend tests:

```bash
cd backend
mvn test
```

The test suite covers rule-based recommendations, AI prompt/parsing/fallback behavior, event triggers and duplicate prevention, approval APIs, repository behavior, CORS, and the end-to-end recommendation demo flow.

Build the frontend:

```bash
cd frontend
npm run build
```

## Project Structure

```text
backend/
	src/main/java/com/stockpulse/
		ai/          LLM gateway and AI strategy
		commerce/    strategies, recommendations, and async event listener
		domain/      JPA entities and enums
		repository/  Spring Data repositories
		service/     product and suggestion workflows
		web/         REST controller
	src/main/resources/
		application.properties
		data.sql
frontend/
	src/
		App.jsx
		index.css
		main.jsx
	package.json
docs/
	ADR.md
```