# k8s Demo - Kubernetes Command Deck

This app is a specialized management console for orchestrating containerized applications within a Kubernetes cluster. It provides a bridge between high-level application definitions and low-level K8s resources.

## Features
* **Blueprint Management**: Create and store application configurations (image, CPU/RAM requests, ports) in a persistent database.
* **Dynamic Orchestration**: Deploy, scale, restart, or delete K8s Deployments and Services directly from the UI.
* **Real-time Monitoring**: Track the state of active deployments, including ready vs. available replicas, directly from the cluster API.
* **Multi-Container Architecture**: The console runs as a self-contained unit with a sidecar PostgreSQL database for metadata persistence.
* **RBAC Integrated**: Built-in ServiceAccount and Role configurations to securely manage cluster resources.

## Tech Stack
* **Backend**: Spring Boot (Java 17), Spring Data JPA.
* **Database**: PostgreSQL.
* **K8s Integration**: Fabric8 Kubernetes Client.
* **Frontend**: Vanilla JS, CSS Variables, and HTML5.

## Deployment
The application is containerized using a multi-stage Dockerfile and deployed via a standard Kubernetes manifest.

```bash
# Apply RBAC permissions
kubectl apply -f rbac.yaml

# Deploy the Atlas Deck console
kubectl apply -f app-deploy.yml
```

![k8s-demo](https://github.com/user-attachments/assets/3725c867-2875-4c26-802b-0803fe4fb4ca)
