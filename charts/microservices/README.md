# Microservices Helm 3 Chart

Enterprise Helm chart packaging the complete Spring Boot microservices mesh with Envoy Gateway, Kafka KRaft, Confluent Schema Registry, MongoDB, and Horizontal Pod Autoscaling (HPA v2).

---

## 1. Directory Structure

```text
charts/microservices/
├── Chart.yaml                          # Chart metadata & API version (v2)
├── README.md                           # Helm usage guide and values specification
├── values.yaml                         # Default parameters (local / Kind / Docker Desktop)
├── values-dev.yaml                     # Development profile (single-replica, low compute)
├── values-prod.yaml                    # Production profile (HA 3+ replicas, strict limits)
├── tls-secret.yaml.example             # Example self-signed/CA TLS certificate template
└── templates/
    ├── _helpers.tpl                    # Standard Kubernetes labels and selector helpers
    ├── config/
    │   ├── inventory-configmap.yaml    # Spring Boot /config/application.yml for inventory
    │   ├── order-configmap.yaml        # Spring Boot /config/application.yml for order
    │   └── notification-configmap.yaml # Spring Boot /config/application.yml for notification
    ├── infrastructure/
    │   ├── mongodb.yaml                # Guarded by .Values.infrastructure.mongodb.enabled
    │   ├── kafka.yaml                  # Guarded by .Values.infrastructure.kafka.enabled
    │   └── schema-registry.yaml        # Guarded by .Values.infrastructure.schemaRegistry.enabled
    ├── apps/
    │   ├── inventory-service.yaml      # Deployment & Service (Port 8000)
    │   ├── order-service.yaml          # Deployment & Service (Port 8100)
    │   ├── notification-service.yaml   # Deployment & Service (Port 8200)
    │   └── pdb.yaml                    # PodDisruptionBudget (guarantees availability on drain)
    ├── gateway/
    │   ├── gateway-class.yaml          # GatewayClass (eg)
    │   ├── gateway.yaml                # Gateway (eg-gateway, HTTP 80 & HTTPS 443)
    │   ├── http-routes.yaml            # HTTPRoutes (/inventory, /orders, /api/v1/orders)
    │   └── backend-traffic-policy.yaml # Circuit Breaker & Outlier Detection
    └── autoscaling/
        └── hpa.yaml                    # HPA v2 policies (CPU 70%, Memory 80%)
```

---

## 2. Quickstart

### Prerequisites
* Helm 3.8+ / Helm 4
* Kubernetes cluster (Docker Desktop, Kind, or Azure AKS)
* Envoy Gateway installed (`eg` GatewayClass)
* TLS Secret in the target namespace (for HTTPS port 443):
  ```bash
  kubectl create secret tls microservices-tls \
    --cert=/path/to/tls.crt \
    --key=/path/to/tls.key \
    -n microservices
  ```

### Install or Upgrade Chart (Local / Default)
```bash
helm upgrade --install microservices ./charts/microservices -n microservices --create-namespace
```

### Install or Upgrade with Development Overlay (Minimal Resources)
```bash
helm upgrade --install microservices ./charts/microservices \
  -f ./charts/microservices/values-dev.yaml \
  -n microservices --create-namespace
```

### Install or Upgrade with Production Profile (Azure AKS / Cloud)
```bash
helm upgrade --install microservices ./charts/microservices \
  -f ./charts/microservices/values-prod.yaml \
  -n microservices --create-namespace
```

---

## 3. Configuration Reference

| Parameter | Description | Default |
| :--- | :--- | :--- |
| `global.environment` | Active environment identifier (`local`, `dev`, `prod`) | `local` |
| `infrastructure.mongodb.enabled` | Deploy in-cluster MongoDB pod | `true` |
| `infrastructure.mongodb.uri` | Connection string for MongoDB | `mongodb://mongo:27017` |
| `infrastructure.kafka.enabled` | Deploy in-cluster Kafka KRaft broker | `true` |
| `infrastructure.kafka.bootstrapServers` | Bootstrap address for Kafka clients | `kafka:29092` |
| `infrastructure.schemaRegistry.enabled` | Deploy in-cluster Schema Registry | `true` |
| `infrastructure.schemaRegistry.url` | URL for Schema Registry Avro schema lookups | `http://schema-registry:8081` |
| `inventory.replicaCount` | Replicas for `inventory-service` | `2` |
| `inventory.image.repository` | Docker image repository | `charanraj165/inventory-service` |
| `inventory.image.tag` | Docker image tag | `latest` |
| `order.replicaCount` | Replicas for `order-service` | `2` |
| `order.image.repository` | Docker image repository | `charanraj165/order-service` |
| `notification.replicaCount` | Replicas for `notification-service` | `2` |
| `gateway.gatewayClassName` | Kubernetes GatewayClass controller reference | `eg` |
| `gateway.name` | Name of the Gateway resource | `eg-gateway` |
| `gateway.httpPort` | Cleartext HTTP listener port | `80` |
| `gateway.https.enabled` | Enable TLS termination listener | `true` |
| `gateway.https.port` | HTTPS listener port | `443` |
| `gateway.https.tlsSecretName` | Kubernetes TLS Secret holding `tls.crt` and `tls.key` | `microservices-tls` |
| `gateway.circuitBreaker.maxConnections` | Envoy C++ circuit breaker maximum connections | `1024` |
| `gateway.circuitBreaker.maxParallelRequests`| Envoy C++ circuit breaker concurrent requests | `1024` |
| `autoscaling.enabled` | Deploy HorizontalPodAutoscaler (HPA v2) resources | `true` |
| `autoscaling.minReplicas` | Minimum pod count per autoscaled service | `2` |
| `autoscaling.maxReplicas` | Maximum pod count per autoscaled service | `5` |
| `autoscaling.targetCPUUtilizationPercentage` | CPU percentage threshold for autoscaling | `70` |
| `podDisruptionBudget.enabled` | Enforce minimum available pods during node drain | `false` (default) / `true` (prod) |
| `podDisruptionBudget.minAvailable` | Minimum pods required to be online during disruptions | `1` |

---

## 4. Upgrades and Rollbacks

### Atomic Rolling Upgrade (e.g. Updating Image Version)
```bash
helm upgrade microservices ./charts/microservices \
  --set order.image.tag=v1.1.0 \
  -n microservices
```

### Rollback to Previous Release Revision
```bash
# View release history
helm history microservices -n microservices

# Roll back to revision 1
helm rollback microservices 1 -n microservices
```

### Uninstall
```bash
helm uninstall microservices -n microservices
```

---

## 5. Related Documentation

- [**Root Architecture & Overview**](../../README.md)
- [**GitOps with ArgoCD**](../../gitops/README.md)
- [**Order Service**](../../order-service/README.md)
- [**Inventory Service**](../../inventory-service/README.md)
- [**Notification Service**](../../notification-service/README.md)


