# Naming Server (Netflix Eureka)

> Dynamic service registry and discovery engine enabling client-side load balancing and resilient service-to-service communication.

---

## 1. Overview & Architecture

`NamingServerApplication` operates Netflix Eureka Server. It provides automatic registry and heartbeat detection, eliminating the need to hardcode hostnames, IP addresses, or ports between services.

```
                   +-----------------------------+
                   |  Eureka Naming Server       |
                   |  Port: 8761                 |
                   +--------------+--------------+
                                  ^
          Heartbeat Registration  |  Instance Discovery (Polling)
         +------------------------+------------------------+
         |                                                 |
+-------------------+                           +-------------------+
|   order-service   | ---- OpenFeign (lb://) -> | inventory-service |
+-------------------+                           +-------------------+
```

### Key Capabilities
- **Dynamic Service Registration**: Each microservice registers its hostname, IP, and health check URL upon startup (`eureka.client.service-url.defaultZone`).
- **Heartbeat Monitoring**: Services send recurring heartbeats every 30 seconds. Instances failing heartbeats are evicted after lease expiration.
- **Client-Side Load Balancing**: Clients (Spring Cloud Gateway, OpenFeign) cache instance lists locally and load-balance calls directly across healthy replicas (`lb://<service-name>`).
- **Self-Preservation Mode**: Prevents mass de-registrations during temporary network partitions or high latency spikes.

---

## 2. Tech Stack

| Component | Technology | Version / Specification |
|---|---|---|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.x Starter Parent |
| Discovery Engine | Spring Cloud Netflix Eureka Server | Spring Cloud 2025.x |
| Observability | Spring Boot Actuator | Health & Metrics |

---

## 3. Configuration & Ports

- **Port**: `8761`
- **Application Name**: `naming-server`
- **Web UI Dashboard**: `http://localhost:8761/`
- **Registration Settings**: Standalone mode (`register-with-eureka: false`, `fetch-registry: false`)

---

## 4. Available APIs & Dashboard

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Web UI dashboard displaying registered instances, memory metrics, and status. |
| `GET` | `/eureka/apps` | Returns JSON/XML list of all registered applications and active nodes. |
| `GET` | `/eureka/apps/{appId}` | Returns metadata and host status for a specific application cluster. |
| `GET` | `/actuator/health` | Health probe for orchestrators and container healthchecks. |

### Sample Request
```bash
# Query registry for all active instances of order-service
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps/ORDER-SERVICE
```
