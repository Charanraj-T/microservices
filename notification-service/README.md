# Notification Service

> Event-driven notification consumer with enterprise Dead Letter Queue (DLQ) routing, handling both poison-pill deserialization errors and domain validation exceptions.

---

## 1. Overview & Architecture

`NotificationServiceApplication` operates as an asynchronous consumer for checkout events. It isolates user communication (email, SMS) from the core order checkout flow.

```
                  +---------------------------+
                  |  Kafka: order-events      |
                  +-------------+-------------+
                                |
                                v
               +--------------------------------+
               |  ErrorHandlingDeserializer     |
               |  (KafkaAvroDeserializer)       |
               +----------------+---------------+
                                |
                +---------------+---------------+
                |                               |
       [Valid Avro Record]             [Malformed / Non-Avro]
                |                         (Poison Pill byte[])
                v                               |
    +-----------------------+                   |
    |OrderNotification-     |                   |
    |Consumer               |                   |
    +-----------+-----------+                   |
                |                               |
        +-------+-------+                       |
        |               |                       |
   (Price > 0)    (Price <= 0)                  |
        |          Domain Error                 |
        v               |                       v
 [Dispatch Alert]       +----------> [DefaultErrorHandler]
                                     (3 Retries / Backoff)
                                                |
                                                v
                              +-----------------------------------+
                              | DeadLetterPublishingRecoverer     |
                              | DelegatingByTypeSerializer        |
                              |  * byte[].class -> ByteArray      |
                              |  * SpecificRecord -> AvroSerializer
                              +-----------------+-----------------+
                                                |
                                                v
                                  +---------------------------+
                                  | Kafka: order-events.dlq   |
                                  +---------------------------+
```

### Key Capabilities
- **Strict Schema Deserialization**: Consumes binary Avro records resolved via Confluent Schema Registry (`SPECIFIC_AVRO_READER_CONFIG: true`).
- **Resilient Poison Pill Handling**: Employs Spring Kafka's `ErrorHandlingDeserializer`. If unparseable non-Avro data is published, the consumer does not crash; it wraps the payload as raw `byte[]`.
- **Hybrid DLQ Publishing (`DelegatingByTypeSerializer`)**:
  - Malformed raw byte streams are forwarded to `order-events.dlq` via `ByteArraySerializer`.
  - Legitimate Avro records that fail business domain checks (e.g. `price <= 0`) are forwarded to `order-events.dlq` via `KafkaAvroSerializer`.
  - Eliminates consumer thread crashes (`ClassCastException` and `SerializationException`).

---

## 2. Tech Stack

| Component | Technology | Version / Specification |
|---|---|---|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.x Starter Parent |
| Messaging | Spring Kafka | `@KafkaListener` consumer container |
| Serialization | Confluent Kafka Avro Serializer | `KafkaAvroDeserializer` + `KafkaAvroSerializer` |
| Schema Registry | Confluent Schema Registry | Port `8081` schema resolution |
| Fault Tolerance | Spring Kafka DLQ Recoverer | `DeadLetterPublishingRecoverer` + `DelegatingByTypeSerializer` |
| Discovery Client | Spring Cloud Netflix Eureka Client | Registers as `NOTIFICATION-SERVICE` |
| Config Client | Spring Cloud Config Client | Pulls config from `config-server:8888` |

---

## 3. Configuration & Ports

- **Port**: `8200`
- **Application Name**: `notification-service`
- **Consumer Group**: `notification-service-group`
- **Source Topic**: `order-events`
- **Dead Letter Topic (DLQ)**: `order-events.dlq`
- **Kafka Brokers**: `localhost:9092`
- **Schema Registry**: `http://localhost:8081`

---

## 4. Consumer Flow & DLQ Scenarios

### Scenario A: Happy Path
1. Valid `OrderPlacedEvent` arrives on `order-events`.
2. Consumer logs confirmation and simulates email/SMS dispatch.
3. Message offset is committed.

### Scenario B: Deserialization Failure (Poison Pill)
1. Malformed or non-Avro byte stream arrives on `order-events`.
2. `ErrorHandlingDeserializer` intercepts the error safely without failing the container.
3. `DeadLetterPublishingRecoverer` routes raw `byte[]` to `order-events.dlq` using `ByteArraySerializer`.
4. Message is inspected via Redpanda Console without consumer interruptions.

### Scenario C: Business Domain Failure
1. Valid Avro record arrives with invalid data (e.g. `price <= 0`).
2. Consumer throws `IllegalArgumentException`.
3. `DefaultErrorHandler` retries 3 times with 1-second backoff.
4. Exhausted retries route the `OrderPlacedEvent` to `order-events.dlq` using `KafkaAvroSerializer`.

---

## 5. Resilient Dead Letter Queue (DLQ) Implementation

To prevent poison-pill message loops and serialization mismatches in production, `KafkaConsumerConfig` configures `DeadLetterPublishingRecoverer` with Spring Kafka's `DelegatingByTypeSerializer`:

```
Incoming Record -> ErrorHandlingDeserializer -> KafkaListener -> Recoverer
                                                                      |
                   +--------------------------------------------------+
                   |
     Is raw byte[] (Poison Pill / Non-Avro)?  ---> ByteArraySerializer    ---> order-events.dlq
     Is SpecificRecord (Domain/Business Bug)? ---> KafkaAvroSerializer    ---> order-events.dlq
```

- **Poison Pills**: Messages with corrupted headers or plain text bypass schema parsing into raw `byte[]` and land directly on `order-events.dlq`.
- **Domain Bugs**: Legitimate Avro records that fail business assertions (e.g. `price <= 0`) are retried 3 times and then forwarded to `order-events.dlq` using the registered Avro schema.
- **Result**: Zero consumer thread crashes, zero `ClassCastException`, and full visibility into dead letters via Redpanda Console.
