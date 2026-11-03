# Order Service

> Core commerce order placement engine with synchronous inventory verification, document persistence, and dynamic feature-toggled Avro event streaming.

---

## 1. Overview & Architecture

`OrderServiceApplication` orchestrates end-to-end checkout processing. It connects synchronous REST RPC (via OpenFeign) with asynchronous event streaming (via Apache Kafka and Confluent Schema Registry).

```
   POST /orders
        |
        v
+------------------+
|  order-service   |
|  Port: 8100      |
+--------+---------+
         |
         | 1. Synchronous Stock Check (OpenFeign)
         v
+------------------+
|inventory-service |  -> Returns stock availability & port
+------------------+
         |
         | 2. Persist to MongoDB (orders collection)
         v
+------------------+
| MongoDB: 27017   |
+------------------+
         |
         | 3. Feature Flag Check (app.kafka.enabled)
         +------------------------------------+
         | (true)                             | (false)
         v                                    v
+-----------------------------+     +-----------------------------+
| Schema Registry (:8081)     |     | Log: "[KAFKA DISABLED]      |
| Kafka Broker (:9092)        |     |  Skipping event publishing" |
| Topic: order-events (Avro)  |     +-----------------------------+
+-----------------------------+
```

### Key Capabilities
- **Synchronous Dependency Check**: Uses declarative `InventoryClient` (`@FeignClient(name = "inventory-service")`) with direct Kubernetes CoreDNS resolution (`http://inventory-service:8000`).
- **Transactional Consistency Boundary**: If inventory verification indicates insufficient stock, order creation aborts immediately (`HTTP 400 Bad Request`).
- **Dynamic Feature Flag**: Controlled by `app.kafka.enabled` (`true` / `false`), enabling flexible operation in either **Lightweight Mode** (zero Kafka/Schema Registry infrastructure required) or **Full Event-Driven Mode**.
- **Binary Avro Event Streaming**: Uses Confluent `KafkaAvroSerializer` with Schema Registry on port `8081`, ensuring strict schema governance, contract compatibility, and minimal network serialization payload.

---

## 2. Tech Stack

| Component | Technology | Version / Specification |
|---|---|---|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.x Starter Parent |
| Synchronous RPC | Spring Cloud OpenFeign | Declarative HTTP Client (`http://inventory-service:8000`) |
| Messaging | Spring Kafka & Apache Avro | Binary event serialization |
| Schema Registry | Confluent Schema Registry | Contract validation (`io.confluent:kafka-avro-serializer`) |
| Persistence | Spring Data MongoDB | Document DB (`order_db`) |
| Configuration | Kubernetes ConfigMaps | Mounted into `/config/application.yml` |
| Service Discovery | Kubernetes CoreDNS | Native cluster DNS endpoint resolution |
| Observability | Micrometer & Zipkin | Distributed trace propagation |

---

## 3. Configuration & Ports

- **Port**: `8100`
- **Application Name**: `order-service`
- **Database**: MongoDB at `localhost:27017/order_db`
- **Kafka Brokers**: `localhost:9092` / `kafka:29092`
- **Schema Registry**: `http://localhost:8081`
- **Feature Flag**: `app.kafka.enabled` (`true` / `false`)

---

## 4. Available APIs

| Method | Endpoint | Request Body / Parameters | Description |
|---|---|---|---|
| `POST` | `/orders` | `{"skuCode":"IPHONE_16","quantity":1,"price":999.00}` | Places order: checks inventory, saves to DB, emits Avro event. |
| `GET` | `/orders` | _None_ | Returns list of all orders stored in MongoDB. |
| `GET` | `/orders/{id}` | MongoDB Document ID | Returns order details by ID. |
| `GET` | `/actuator/health` | _None_ | Health check status. |

### Sample Requests

```bash
# 1. Place a successful order
curl -X POST http://localhost:8100/orders \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"IPHONE_16","quantity":1,"price":999.00}'

# 2. Place an order for an out-of-stock item (triggers HTTP 400 rejection)
curl -X POST http://localhost:8100/orders \
  -H "Content-Type: application/json" \
  -d '{"skuCode":"PIXEL_9","quantity":1,"price":799.00}'

# 3. List all orders
curl -s http://localhost:8100/orders
```
