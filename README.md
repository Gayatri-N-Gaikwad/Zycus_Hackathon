# StockPulse - Reactive AI Commerce Advisor

StockPulse is a reactive commerce advisor backend built with Spring Boot. This system provides intelligent inventory recommendations for retail businesses through product tracking, pricing analysis, and automated suggestions.

**Note:** This project represents the completed Phase 1 implementation consisting of domain entities, repositories, and seed data. No commerce strategy or AI components have been implemented at this stage.

## Current Architecture and Project Structure

The backend consists of the following layers:

### Domain Layer
- **Entities**: Core business objects (`Product`, `PricingSuggestion`, `ReorderSuggestion`, `InventorySnapshot`)
- **Enums**: Classification types (`Category`, `ProductStatus`, `SuggestionDirection`, `SuggestionStatus`, `TriggerReason`)

### Repository Layer
- **Repositories**: JPA interfaces for data persistence (`ProductRepository`, `PricingSuggestionRepository`, `ReorderSuggestionRepository`)

### Service Layer
- **Services**: Business logic implementation (`ProductService`)

### Web Layer
- **Controllers**: REST API endpoints (`ProductController`)

### Data Initialization
- **Seed Data**: Initial product catalog and suggestions loaded via `data.sql`
- **Database**: H2 in-memory database for development

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.4
- **Build Tool**: Apache Maven
- **Data Access**: Spring Data JPA with Hibernate
- **Database**: H2 In-Memory Database
- **Testing**: JUnit 5 with Spring Boot Test

## Running the Application Locally

1. Ensure you have Java 21 and Maven installed
2. Navigate to the `backend` directory
3. Run the application:

```bash
mvn spring-boot:run
```

Or build and run the jar:

```bash
mvn clean package
java -jar target/stockpulse-backend-0.0.1-SNAPSHOT.jar
```

The application will start on port 8080.

To access the H2 console (for debugging):
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:stockpulsedb
- Username: sa
- Password: (leave empty)

## Available API Endpoints

### Products API

**Get all products**
```
GET /api/products
```

**Get products with filters**
```
GET /api/products?status=ACTIVE&category=ELECTRONICS
```

**Get a specific product**
```
GET /api/products/{id}
```

**Create a new product**
```
POST /api/products
```

**Update an existing product**
```
PUT /api/products/{id}
```

**Delete a product**
```
DELETE /api/products/{id}
```

### Pricing Suggestions API

**Get pricing suggestions for a product**
```
GET /api/products/{id}/pricing-suggestions
GET /api/products/{id}/pricing-suggestions?status=PENDING
```

**Create a pricing suggestion for a product**
```
POST /api/products/{id}/pricing-suggestions
```

### Reorder Suggestions API

**Get reorder suggestions for a product**
```
GET /api/products/{id}/reorder-suggestions
GET /api/products/{id}/reorder-suggestions?status=PENDING
```

**Create a reorder suggestion for a product**
```
POST /api/products/{id}/reorder-suggestions
```

## Current Status

✅ Domain entities implemented (`Product`, `PricingSuggestion`, `ReorderSuggestion`, `InventorySnapshot`)  
✅ Repositories implemented (`ProductRepository`, `PricingSuggestionRepository`, `ReorderSuggestionRepository`)  
✅ Seed data available (8 sample products with initial pricing and reorder suggestions)  
✅ Automated tests passing (15/15 tests passing)  
✅ REST API endpoints functional  

---

## Verification Results

- Maven Tests: ✅ 15 tests passing, 0 failures, 0 errors
- Build Status: ✅ BUILD SUCCESS

## Recommended Next Stage

Phase 2: Commerce Engine Strategy Layer
