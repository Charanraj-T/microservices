# API Gateway

> Non-blocking, reactive edge gateway managing client routing, rate limiting, circuit breaking, path rewrites, and distributed tracing.

---

## 1. Overview & Architecture

`ApiGatewayApplication` acts as the unified reverse proxy and security perimeter for all client applications. Built on Project Reactor and Netty, it handles concurrent requests with low latency and non-blocking I/O.

```
 Client Request (curl / web)
             |
             v
   +-------------------+
   | API Gateway: 8765 |
   +---------+---------+
             |
     [Logging Filter]  -> Correlates distributed traces & logs URI
             |
   [Redis Rate Limiter]-> Token Bucket algorithm (3 req/sec, burst 6)
             |
 [Resilience4j Filter] -> Downstream health monitor & fallback handler
             |
     [Routing Logic]   -> Routes & rewrites path to target services
             |
    +--------+--------+
    |                 |
    v (lb://)         v (lb://)
+---------------+ +-------------------+
| order-service | | inventory-service |
+---------------+ +-------------------+
```

### Key Capabilities
- **Reactive Non-Blocking Engine**: Built on Spring Cloud Gateway WebFlux and Netty, avoiding thread-per-request overhead.
- **Dynamic Load Balancing**: Resolves downstream microservice IP addresses via Netflix Eureka using `lb://<service-name>`.
- **Redis Token Bucket Rate Limiting**: Configured with `KeyResolver` per client IP. Enforces 3 requests/sec replenishment and burst capacity of 6. Excess calls receive `HTTP 429 Too Many Requests`.
- **Resilience4j Circuit Breaker & Fallbacks**: Downstream outages or latency spikes trip the circuit breaker, rerouting traffic to `FallbackController` with `HTTP 503 SERVICE_DEGRADED` responses.
- **Automated Retries**: Retries idempotent requests up to 3 times on `BAD_GATEWAY`, `SERVICE_UNAVAILABLE`, or `GATEWAY_TIMEOUT` with exponential backoff.
- **Distributed Tracing**: Automatically injects and propagates W3C trace headers to Zipkin (`localhost:9411`).

---

## 2. Tech Stack

| Component | Technology | Version / Specification |
|---|---|---|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.x Starter Parent |
| Edge Engine | Spring Cloud Gateway Server WebFlux | Netty & Project Reactor |
| Rate Limiter | Spring Data Redis Reactive | Redis Token Bucket Algorithm |
| Fault Tolerance | Resilience4j Reactor Circuit Breaker | `spring-cloud-starter-circuitbreaker-reactor-resilience4j` |
| Discovery Client | Spring Cloud Netflix Eureka Client | Dynamic service registry lookup |
| Observability | Micrometer Tracing & Zipkin | Distributed span tracking |

---

## 3. Configuration & Ports

- **Port**: `8765`
- **Application Name**: `api-gateway`
- **Redis Cache (Rate Limiter)**: `localhost:6379`
- **Eureka Server**: `http://localhost:8761/eureka`
- **Zipkin Tracing**: `http://localhost:9411/api/v2/spans`

---

## 4. Routing Table & Available Endpoints

### Ingress Routes

| Ingress Route | Target Service | Rewritten Path | Features Applied |
|---|---|---|---|
| `/orders/**` | `lb://order-service` | Pass-through (`/orders/**`) | Redis RateLimiter, CircuitBreaker (`orderCircuitBreaker`), Fallback |
| `/api/v1/orders/**` | `lb://order-service` | `/orders${segment}` | Path Rewrite, Redis RateLimiter, CircuitBreaker, Fallback |
| `/inventory/**` | `lb://inventory-service` | Pass-through (`/inventory/**`) | Redis RateLimiter, CircuitBreaker (`inventoryCircuitBreaker`), Fallback |

### Fallback Endpoints

| Method | Endpoint | HTTP Status | Description |
|---|---|---|---|
| `GET`/`POST` | `/fallback/orders` | `HTTP 503` | Returned when `order-service` is unreachable or times out. |
| `GET`/`POST` | `/fallback/inventory` | `HTTP 503` | Returned when `inventory-service` is down or overloaded. |

### Gateway Management Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/gateway/routes` | Returns runtime configuration of all active routes, filters, and predicates. |
| `GET` | `/actuator/health` | Health status of Gateway, Redis connection, and Eureka discovery. |

### Sample Requests
```bash
# Query Inventory via Gateway
curl -i http://localhost:8765/inventory/IPHONE_16

# Place Order via Gateway standard route
curl -i -X POST http://localhost:8765/orders \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"IPHONE_16","quantity":1,"price":999.00}'

# Place Order via Gateway rewritten route (/api/v1/orders -> /orders)
curl -i -X POST http://localhost:8765/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"IPHONE_16","quantity":1,"price":999.00}'
```
