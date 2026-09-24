# Book Worm — E-Commerce Bookstore Platform

> **Capstone Project:** Full-Stack Cloud & Specialist Bookstore Application  
> **Backend:** Spring Boot 3.3.4 &bull; Java 17 &bull; Spring Security &bull; JJWT &bull; Spring Data JPA &bull; H2 / PostgreSQL  
> **Frontend:** Tailwind CSS &bull; Hydrangea Floral Pastel Minimalism &bull; Sharp Edges &bull; Responsive 3D Covers  
> **API Spec:** OpenAPI 3.0 (`e-bookstore-openapi (1).yaml`)

---

## Overview

**Book Worm** is a production-grade full-stack bookstore application faithfully translating the wireframes and customer journeys from Slides 5–10 of the *AI Specialist - Cloud FullStack Capstone* presentation.

### Key Capabilities

1. **Hydrangea Design System**:
   - Palette: Off-white canvas (`#FAF8F5`), charcoal text (`#1E2024`), soft peach panels (`#FFEAE6`), rose pink highlights (`#FF8DA1`), orchid pink category badges (`#FF9CE9`), and bold purple accents (`#AD56C4` / `#74318A`).
   - Geometric styling: Universal sharp edges (`border-radius: 0 !important`).

2. **Interactive 3D Dual Book Covers (Slide 7)**:
   - Front Cover: Bestseller edition tag, dynamic typography, emblem, spine crease, and textured book pattern.
   - Back Cover: Mirrored spine crease, quote, blurb excerpt, author bio, publisher colophon logo, and authentic vector ISBN-13 barcode sticker.

3. **Cart & Loyalty Points Checkout (Slide 8)**:
   - Live cart totals calculation, delivery address selection, and 1:1 gift point redemption ($1\text{ pt} = ₹1.00$).

4. **Idempotent Payment Simulation (Slide 9)**:
   - `Idempotency-Key` header enforcement prevents duplicate charges or stock decrement under retries and concurrent submissions.
   - Evaluator toggle supporting simulated `APPROVED` and `DECLINED` payment scenarios with automatic point and inventory rollbacks.

5. **Order History & One-Click Buy Again (Slide 10)**:
   - Complete price snapshots freeze purchase-time title and unit prices against future catalogue updates.
   - Buy Again adds available books from historic orders to the active cart at current prices.

---

## Architecture & Project Structure

```
ebookstore/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/ebookstore/
│   │   │   ├── config/          # SecurityConfig, JwtAuthFilter, DataSeeder
│   │   │   ├── controller/      # REST API Controllers (Catalogue, Cart, Checkout, Order, Auth)
│   │   │   ├── dto/             # Data Transfer Objects & DtoMapper
│   │   │   ├── entity/          # JPA Entities (Book, Category, Cart, Order, Payment, Address, User)
│   │   │   ├── exception/       # GlobalExceptionHandler, Custom Exceptions
│   │   │   ├── repository/      # Spring Data JPA Repositories
│   │   │   └── service/         # Business Logic & Transactional Services
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/          # Single Page Application
│   │           ├── index.html   # Tailwind CSS Layout
│   │           ├── css/style.css# 3D Book Transforms & Sharp Edge Rules
│   │           └── js/app.js    # Client Controller & Dynamic DOM Engine
│   └── test/java/com/example/ebookstore/
│       ├── BookstoreJourneyTest.java
│       ├── CatalogueServiceTest.java
│       ├── CartServiceTest.java
│       ├── CheckoutServiceTest.java
│       ├── OrderServiceTest.java
│       ├── PaymentServiceTest.java
│       └── AccountServiceTest.java
```

---

## Getting Started

### Prerequisites
* Java Development Kit (JDK) 17 or higher
* Apache Maven 3.8+ (or use included `mvnw`)

### Build & Test

Run the full automated test suite (55 integration and unit tests):

```powershell
mvn test
```

### Run Locally (Default H2 In-Memory DB)

```powershell
mvn spring-boot:run
```

Once started:
* **Web UI:** [http://localhost:8080/](http://localhost:8080/)
* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Docs:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Run with PostgreSQL Profile

To connect to a live PostgreSQL instance:

```powershell
# Set environment variables
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="ebookstoredb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="yourpassword"

# Launch application
mvn spring-boot:run
```

---

## Seeded Demo Credentials

On startup, the application pre-seeds the database with test fixtures:
* **Demo User:** `demo@ebookstore.com`
* **Demo Password:** `demo1234`
* **Starting Points:** 500 Gift Points (₹500.00 purchasing power)
* **Pre-seeded Addresses:**
  1. `12 MG Road, Apt 4B, Bengaluru, Karnataka - 560001`
  2. `42 Anna Salai, Chennai, Tamil Nadu - 600002`
* **Catalogue:** 21 curated titles including *The Joy of Minimalism* (₹149), *The Path to Success* (₹359), and *The Art of Focus* (₹399).

---

## REST API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/auth/login` | `POST` | Authenticate with email/password; returns JWT token |
| `/api/v1/me` | `GET` | Fetch authenticated user profile & points balance |
| `/api/v1/categories` | `GET` | List all book categories |
| `/api/v1/publishers` | `GET` | List all book publishers / brands |
| `/api/v1/books` | `GET` | Paginated search, category, and publisher filter |
| `/api/v1/books/{id}` | `GET` | Retrieve complete details for a book |
| `/api/v1/books/{id}/related` | `GET` | Related recommendations in same category |
| `/api/v1/me/cart` | `GET` | Retrieve customer's active shopping cart |
| `/api/v1/me/cart/items` | `POST` | Add book to cart with stock validation |
| `/api/v1/me/cart/items/{bookId}` | `PUT` | Update quantity of cart item |
| `/api/v1/me/cart/items/{bookId}` | `DELETE` | Remove book from cart |
| `/api/v1/me/addresses` | `GET` / `POST` | List or create delivery addresses |
| `/api/v1/me/checkout/quote` | `POST` | Calculate subtotal, points discount & final total |
| `/api/v1/me/orders` | `POST` | Place order (requires `Idempotency-Key` header) |
| `/api/v1/me/orders/{id}/pay` | `POST` | Execute payment (requires `Idempotency-Key` header) |
| `/api/v1/me/orders` | `GET` | List customer order history (newest first) |
| `/api/v1/me/orders/{id}` | `GET` | Get details and price snapshots of an order |
| `/api/v1/me/orders/{id}/buy-again` | `POST` | Re-add available order items to cart |
| `/api/v1/me/orders/{id}/cancel` | `POST` | Cancel order within 48h; restore points & stock |
| `/api/v1/me/recommendations` | `GET` | Category-based recommendations from prior orders |

---

## Quality & Test Matrix

All **55 JUnit integration and service tests** pass with **100% pass rate** (0 failures, 0 errors):
* `BookstoreJourneyTest`: End-to-end multi-step purchase and order lifecycle validation.
* `OrderServiceTest`: Snapshots, Buy Again, 48-hour order cancellation, points restoration.
* `PaymentServiceTest`: Approved/declined simulated outcomes, idempotency key replays.
* `CheckoutServiceTest`: Gift point balance bounds, address validation, stock depletion checks.
* `CartServiceTest`: Add, update quantity, remove, and subtotal arithmetic.
* `CatalogueServiceTest`: Case-insensitive search, pagination, category filtering.
* `AccountServiceTest`: User profile, address management, and ownership protection.
