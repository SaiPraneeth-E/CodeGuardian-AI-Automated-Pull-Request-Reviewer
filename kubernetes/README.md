# CodeGuardian AI — Kubernetes Deployment Guide

This directory contains the Kubernetes manifests required to deploy the **CodeGuardian AI** full-stack application (Spring Boot backend, React Dashboard frontend, PostgreSQL, Redis, Qdrant, and Kafka).

---

## 1. Folder Structure

```text
kubernetes/
├── namespace.yaml           # Isolation namespace (codeguardian)
├── configmap.yaml           # Non-sensitive application configuration
├── secrets.yaml             # Sensitive configuration credentials
├── postgres.yaml            # Relational Database Deployment, Service, and PVC
├── redis.yaml               # Cache Layer Deployment, Service, and PVC
├── qdrant.yaml              # Vector Database Deployment, Services, and PVC
├── kafka.yaml               # Zookeeper/Kafka Event Broker Deployments and Services
├── app.yaml                 # Spring Boot Backend API Deployment and Service
├── dashboard.yaml           # React Dashboard UI Deployment and Service
├── ingress.yaml             # Nginx Ingress Controller routing rules
└── README.md                # This documentation
```

---

## 2. Deployment Instructions

### Prerequisites
- A running Kubernetes cluster (e.g., Minikube, Docker Desktop, EKS, GKE, AKS)
- Kubernetes CLI `kubectl` configured to point to your cluster
- Nginx Ingress Controller enabled (e.g., `minikube addons enable ingress`)

### Step 1: Create Namespace and Configs
Apply the namespace, configuration maps, and secrets:
```bash
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/secrets.yaml
```

> [!IMPORTANT]
> In production, you must edit `kubernetes/secrets.yaml` to include your actual base64 encoded credentials for the GitHub webhook secret, GitHub PAT, and Gemini API Key.

### Step 2: Deploy Backing Infrastructure
Deploy the persistent storage volumes, databases, caches, and message queues:
```bash
kubectl apply -f kubernetes/postgres.yaml
kubectl apply -f kubernetes/redis.yaml
kubectl apply -f kubernetes/qdrant.yaml
kubectl apply -f kubernetes/kafka.yaml
```
Verify that the backing service pods are starting up and healthy:
```bash
kubectl get pods -n codeguardian -w
```

### Step 3: Deploy Application Services
Once databases and message queues are healthy, deploy the Spring Boot backend and React dashboard:
```bash
kubectl apply -f kubernetes/app.yaml
kubectl apply -f kubernetes/dashboard.yaml
```

### Step 4: Deploy Ingress Controller Routing
Expose the services externally using the Ingress manifest:
```bash
kubectl apply -f kubernetes/ingress.yaml
```

### Step 5: Configure Host Resolution
Add the ingress IP to your local hosts file to test the application locally.
1. Retrieve the Ingress IP:
   ```bash
   kubectl get ingress -n codeguardian
   ```
2. Add the host mappings to `/etc/hosts` (macOS/Linux) or `C:\Windows\System32\drivers\etc\hosts` (Windows):
   ```text
   <INGRESS-IP> codeguardian.local
   ```
3. Open your browser and navigate to `http://codeguardian.local/` to access the application dashboard.

---

## 3. Architecture Explanations

### Scaling Strategy
- **Stateless Services (`app` and `dashboard`)**:
  These services are completely stateless and can scale horizontally using a **Horizontal Pod Autoscaler (HPA)**. We configure the deployment replicas to 2 by default. In production, we configure an HPA to monitor CPU and Memory utilization:
  ```yaml
  apiVersion: autoscaling/v2
  kind: HorizontalPodAutoscaler
  metadata:
    name: app-hpa
    namespace: codeguardian
  spec:
    scaleTargetRef:
      apiVersion: apps/v1
      kind: Deployment
      name: app
    minReplicas: 2
    maxReplicas: 10
    metrics:
      - type: Resource
        resource:
          name: cpu
          target:
            type: Utilization
            averageUtilization: 75
  ```
- **Stateful Backing Services (`postgres`, `redis`, `qdrant`, `kafka`)**:
  These services are deployed as single-replica setups with local PVCs for demonstration. In a production cluster:
  - **PostgreSQL**: Deploy using a cluster operator like **CloudNativePG** or **Patroni** to manage primary-standby replication and automatic failover.
  - **Redis**: Deploy in **Sentinel** or **Redis Cluster** mode.
  - **Qdrant**: Set up in distributed clustering mode (deploying as a StatefulSet with internal coordination on ports `6335`).
  - **Kafka**: Scale by configuring a Kafka cluster with multiple brokers (StatefulSet) and partition topics to distribute consumer group workloads across brokers.

### Resource Requests and Limits
We define resource requests and limits to ensure Kubernetes can schedule pods efficiently and prevent single containers from consuming all node resources:
- **Requests**: The minimum resources guaranteed to the container. Kubernetes uses this value to decide which node has enough capacity to run the pod.
- **Limits**: The maximum resources the container is allowed to consume. If memory limit is breached, the container is OOMKilled; if CPU limit is exceeded, the container is throttled.
- **JVM Memory Management**: The Java application is configured with `JAVA_OPTS: "-XX:MaxRAMPercentage=75.0"`. This ensures the JVM respects the container memory limit (1Gi) and prevents JVM heap allocations from exceeding memory boundaries, preventing host-level OOM terminations.

### Rolling Updates
Stateless apps use the `RollingUpdate` strategy to ensure zero-downtime deployments:
- **`maxSurge: 1`**: Instructs the scheduler to spin up at most 1 pod above the desired count during an update.
- **`maxUnavailable: 0`**: Ensures that at least the desired number of pods are running and ready at all times.
- Stateful databases use the `Recreate` strategy. Since they mount a ReadWriteOnce (RWO) persistent volume, only one pod can connect to the storage volume at a time. The old pod must be completely terminated (`Recreate`) before a new pod can mount the storage volume and start up.

### High Availability (HA)
- **Multi-Replica Deployments**: The stateless `app` and `dashboard` deployments run with `replicas: 2`.
- **Pod Anti-Affinity**: In production, we add `podAntiAffinity` rules to ensure that replicas of the same service are scheduled on different physical nodes. This ensures that the loss of a single node does not impact application availability.
- **Health Checks & Probes**: 
  - **Liveness Probes**: Automatically restart unhealthy or deadlocked containers.
  - **Readiness Probes**: Temporarily remove pods from active service traffic endpoints if they are initializing or running slow, avoiding sending requests to broken instances.
- **Cloud Managed Services (Recommended for Production)**: For mission-critical production systems, local database deployments should be replaced with managed cloud offerings (e.g., AWS RDS, Amazon ElastiCache, managed Qdrant Cloud, MSK) to handle backups, multi-AZ high availability, and failovers natively.

---

## 4. Interview Questions & Answers

### Q1: Why did you choose a Deployment with `Recreate` strategy instead of a `RollingUpdate` for PostgreSQL, Qdrant, and Redis?
**Answer**: These databases write state to local persistent volumes mounted via a `ReadWriteOnce` (RWO) PersistentVolumeClaim. RWO volumes can only be mounted by a single node/pod at any given time. If we used `RollingUpdate`, Kubernetes would try to spin up the new database pod while the old one was still running. The new pod would fail to start because it wouldn't be able to mount the PVC still locked by the old pod. By using `Recreate`, we ensure the old pod is completely terminated and releases the volume lock before the new pod begins initialization, preventing volume mount errors.

### Q2: How does the Java application handle container limits, and why is `-XX:MaxRAMPercentage=75.0` important?
**Answer**: Historically, Java ran inside containers without respecting container cgroup memory limits, leading to JVM allocating heap based on the underlying VM's memory and crashing due to OOM kills. Modern Java (JDK 10+) is container-aware. By specifying `-XX:MaxRAMPercentage=75.0`, we tell the JVM to restrict its max heap size to 75% of the container's memory limit (which is `1Gi` in our `app.yaml`). The remaining 25% is reserved for metaspace, thread stacks, JVM overhead, and OS processes, ensuring the pod never exceeds its cgroup limits and gets OOMKilled by the kernel.

### Q3: Why did we map the Ingress to the Dashboard service on `/` and how does the frontend communicate with the backend API in this setup?
**Answer**: The React Dashboard container includes an Nginx reverse-proxy configuration (`nginx.conf`) that forwards any request matching `/api/*` to `http://app:8080/`. Because both pods reside in the same Kubernetes namespace and our Spring Boot service is named `app`, Kubernetes DNS resolves the hostname `app` directly to the backend service. This architecture means we only need to expose the `dashboard` service via Ingress at the root `/`. External clients make requests to `http://codeguardian.local/api/` which hit the dashboard service, and Nginx proxies them internally, avoiding Cross-Origin Resource Sharing (CORS) issues and simplifying ingress setups.

### Q4: If Kafka needs to scale to multiple brokers, what changes would you make to the manifests?
**Answer**: A simple Deployment is insufficient to run a multi-broker Kafka cluster because each broker requires a unique identifier (broker ID), distinct config directories, and stable network identifiers. We would convert the Kafka and Zookeeper manifests into **StatefulSets** rather than Deployments, configure a headless service to provide predictable DNS network addresses (e.g., `kafka-0.kafka`, `kafka-1.kafka`), and use **VolumeClaimTemplates** to dynamically provision individual storage volumes for each broker pod. Alternatively, in production, we would use operators like **Strimzi** to manage cluster lifecycle automatically.

---

## 5. Git Commit Message

```text
feat(k8s): implement kubernetes manifests for full-stack deployment

- Create namespace manifest for resource isolation.
- Define ConfigMap and Secret templates for configuration.
- Implement manifests for backing databases (PostgreSQL, Qdrant, Redis, Kafka).
- Deploy multi-replica Spring Boot app and React Dashboard with resource limits.
- Add Ingress routing rules for single-point external access.
- Include architectural documentation on HA, scaling, and rolling updates.
```
