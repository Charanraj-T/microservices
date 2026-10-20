# Config Server

> Centralized, profile-driven configuration management server for all microservices in the distributed architecture.

---

## 1. Overview & Architecture

`ConfigServerApplication` acts as the single source of truth for externalized application properties across environments (`dev`, `prod`, `default`).

```
                +-----------------------------------------+
                |         config-repo/ Directory          |
                |  (order-service.yml, api-gateway.yml)  |
                +--------------------+--------------------+
                                     |
                                     v
                       +---------------------------+
                       |    Config Server (:8888)  |
                       +-------------+-------------+
                                     |
         +---------------------------+---------------------------+
         |                           |                           |
         v                           v                           v
+-----------------+         +-----------------+         +--------------------+
|  order-service  |         |inventory-service|         |notification-service|
+-----------------+         +-----------------+         +--------------------+
```

### Key Capabilities
- **Decoupled Configuration**: Microservices pull environment parameters at bootstrap time, eliminating hardcoded configuration in build artifacts.
- **Native File Backend**: Reads configuration files directly from the local `config-repo/` directory via Spring Cloud `native` search locations.
- **Zero-Downtime Hot Reloading**: Integrates with Spring Cloud `@RefreshScope` on downstream clients; updates in `config-repo/` apply immediately upon calling `/actuator/refresh` without restarting JVMs.

---

## 2. Tech Stack

| Component | Technology | Version / Specification |
|---|---|---|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.x Starter Parent |
| Config Engine | Spring Cloud Config Server | Spring Cloud 2025.x |
| Observability | Spring Boot Actuator | Health & Metrics |
| Storage Backend | Local Filesystem | Native profile (`config-repo/`) |

---

## 3. Configuration & Ports

- **Port**: `8888`
- **Application Name**: `config-server`
- **Configuration Directory**: `config-repo/`
- **Active Search Path**: `file:///${user.dir}/config-repo`

---

## 4. Available APIs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/{application}/{profile}` | Returns JSON property sources for a given service and profile (e.g. `/order-service/default`). |
| `GET` | `/{application}-{profile}.yml` | Returns properties formatted as raw YAML. |
| `GET` | `/{application}-{profile}.properties` | Returns properties formatted as standard Java properties. |
| `GET` | `/actuator/health` | Service health status. |

### Sample Request
```bash
curl -s http://localhost:8888/order-service/default
```
