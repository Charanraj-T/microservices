# Inventory Service

> Stock management microservice providing real-time inventory queries, inventory reservation checks, and resilience pattern references.

---

## 1. Overview & Architecture

`InventoryServiceApplication` manages product stock levels. It is queried synchronously by `order-service` via OpenFeign prior to confirming any customer checkout.

```
       Incoming Query (from order-service via Feign)
                           |
                           v
              +--------------------------+
              | Inventory Service (:8000)|
              +-------------+------------+
                            |
           +----------------+----------------+
           |                                 |
           v                                 v
+-----------------------+        +-----------------------+
|  MongoDB (27017)      |        |  Resilience4j Layer   |
|  inventory_db         |        |  (CircuitBreaker &    |
|  collection: inventory|        |   Retry Fallbacks)    |
+-----------------------+        +-----------------------+
```

### Key Capabilities
- **Real-Time Stock Verification**: Evaluates SKU availability and returns `InventoryResponse` containing stock status and server port.
- **Port Transparency for Load Balancing**: Embeds `local.server.port` in the response body so callers can inspect client-side round-robin load balancing across replicas.
- **Auto-Bootstrapped Sample Data**: Seeds initial catalog records (`IPHONE_16`, `MACBOOK_PRO`, `PIXEL_9`) on startup via `@PostConstruct`.
- **Resilience4j Sandbox**: Includes dedicated demo endpoints demonstrating Retry and Circuit Breaker state transitions under simulated failure conditions.

---

## 2. Tech Stack

| Component | Technology | Version / Specification |
|---|---|---|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.x Starter Parent |
| Persistence | Spring Data MongoDB | Document DB (`inventory_db`) |
| Fault Tolerance | Resilience4j | CircuitBreaker, Retry, RateLimiter |
| Service Discovery | Spring Cloud Netflix Eureka Client | Registers as `INVENTORY-SERVICE` |
| Config Client | Spring Cloud Config Client | Pulls config from `config-server:8888` |
| Observability | Micrometer & Zipkin | Distributed tracing export |

---

## 3. Configuration & Ports

- **Port**: `8000`
- **Application Name**: `inventory-service`
- **Database**: MongoDB at `localhost:27017/inventory_db`
- **Config Server**: `http://localhost:8888`
- **Eureka Server**: `http://localhost:8761/eureka`

---

## 4. Available APIs

### Inventory APIs

| Method | Endpoint | Description | Request / Response Sample |
|---|---|---|---|
| `GET` | `/inventory/{skuCode}` | Check if SKU is in stock and fetch remaining quantity. | `{"skuCode":"IPHONE_16","inStock":true,"quantity":100,"environmentPort":"8000"}` |
| `POST` | `/inventory` | Create or update inventory item. | Body: `{"skuCode":"IPAD_AIR","quantity":25}` |

### Resilience Demo APIs

| Method | Endpoint | Behavior |
|---|---|---|
| `GET` | `/sample-retry` | Simulates consecutive failures; retries up to 3 times before returning fallback response. |
| `GET` | `/sample-circuit-breaker` | Simulates high failure rate; trips circuit breaker from CLOSED to OPEN state. |

### Sample Requests
```bash
# Check stock for iPhone 16
curl -s http://localhost:8000/inventory/IPHONE_16

# Check out-of-stock item (Pixel 9 has quantity 0)
curl -s http://localhost:8000/inventory/PIXEL_9

# Add new inventory item
curl -X POST http://localhost:8000/inventory \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"GALAXY_S24","quantity":30}'
```
