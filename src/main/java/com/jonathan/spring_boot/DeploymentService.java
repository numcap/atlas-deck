package com.jonathan.spring_boot;

import com.jonathan.spring_boot.req_res.ActiveDeploymentResponse;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class DeploymentService {
	private static final String DEFAULT_NAMESPACE = "default";
	private final KubernetesClient kubernetesClient;
	private final ApplicationRepository applicationRepository;

	public void createDeployment(UUID blueprintId) {
		String finalNs = DEFAULT_NAMESPACE;

		Application app = getApplication(blueprintId);

		if (app.getImage() == null || app.getImage().isEmpty()) {
			throw new IllegalArgumentException("Application image must be provided");
		}

		if (!deploymentExists(app.getName())) {
			System.out.println("Creating deployment for application: " + app.getName());
		} else {
			System.out.println("Deployment already exists for application: " + app.getName());
			return;
		}

		Deployment deployment = new DeploymentBuilder()
				.withNewMetadata()
				.withName(app.getName())
				.addToLabels("app", app.getName())
				.endMetadata()
				.withNewSpec()
				.withReplicas(app.getDesiredReplicas())
				.withNewSelector()
				.addToMatchLabels("app", app.getName())
				.endSelector()
				.withNewTemplate()
				.withNewMetadata()
				.addToLabels("app", app.getName())
				.endMetadata()
				.withNewSpec()
				.addNewContainer()
				.withName(app.getName())
				.withImage(app.getImage())
				.addNewPort()
				.withContainerPort(app.getContainerPort())
				.endPort()
				.withResources(new ResourceRequirements().edit()
						.addToLimits("memory", Quantity.parse(app.getResources_ram()))
						.addToRequests("cpu", Quantity.parse(app.getResources_cpu()))
						.build())
				.withEnv(getEnvVars(app.getEnv()))
				.endContainer()
				.endSpec()
				.endTemplate()
				.endSpec()
				.build();

		kubernetesClient.apps().deployments().inNamespace(finalNs).resource(deployment).create();
		System.out.println("Deployment created for application: " + app.getName());

		if (!app.getServiceEnabled()) return;
		createOrUpdateService(app, finalNs, "ClusterIP");
	}

	public void exposeApplication(UUID blueprintId, String type) {
		String finalNs = "default";

		Application app = getApplication(blueprintId);

		createOrUpdateService(app, finalNs, type);
	}

	private Application getApplication(UUID blueprintId) {
		return applicationRepository.findById(blueprintId)
				.orElseThrow(() -> new RuntimeException("Application not found: " + blueprintId));
	}

	private void createOrUpdateService(Application app, String namespace, String type) {
		String svcType = (type == null || type.isBlank()) ? "ClusterIP" : type;

		ServicePort servicePort = new ServicePortBuilder()
				.withName("http")
				.withProtocol("TCP")
				.withPort(app.getContainerPort())
				.withTargetPort(new IntOrString(app.getContainerPort()))
				.build();

		Service service = new ServiceBuilder()
				.withNewMetadata()
				.withName(app.getName())
				.addToLabels("app", app.getName())
				.endMetadata()
				.withNewSpec()
				.withType(svcType)
				.addToSelector("app", app.getName())
				.addToPorts(servicePort)
				.endSpec()
				.build();

		Service existing = kubernetesClient.services().inNamespace(namespace).withName(app.getName()).get();
		if (existing == null) {
			kubernetesClient.services().inNamespace(namespace).resource(service).create();
			System.out.println("Service created for application: " + app.getName() + " (type=" + svcType + ")");
			return;
		}

		Service updated = new ServiceBuilder(existing)
				.editMetadata()
				.addToLabels("app", app.getName())
				.endMetadata()
				.editSpec()
				.withType(svcType)
				.withPorts(servicePort)
				.withSelector(Collections.singletonMap("app", app.getName()))
				.endSpec()
				.build();

		kubernetesClient.services().inNamespace(namespace).resource(updated).replace();
		System.out.println("Service updated for application: " + app.getName() + " (type=" + svcType + ")");
	}


	private List<EnvVar> getEnvVars(Object envJsonObj) {
		if (envJsonObj == null) {
			return Collections.emptyList();
		}

		JsonNode envJson;
		if (envJsonObj instanceof JsonNode) {
			envJson = (JsonNode) envJsonObj;
		} else {
			try {
				envJson = new tools.jackson.databind.ObjectMapper().readTree(envJsonObj.toString());
			} catch (Exception e) {
				return Collections.emptyList();
			}
		}

		if (envJson == null || envJson.isNull() || envJson.isMissingNode()) {
			return Collections.emptyList();
		}
		if (!envJson.isObject() || envJson.isEmpty()) {
			return Collections.emptyList();
		}

		return envJson.properties().stream()
				.map(entry -> {
					JsonNode value = entry.getValue();
					String v = (value == null || value.isNull()) ? "" : value.asString();
					return new EnvVar(entry.getKey(), v, null);
				})
				.collect(Collectors.toList());
	}


	public boolean startDeployment(UUID applicationId, int replicas) {
		String finalNs = DEFAULT_NAMESPACE;
		Application app = getApplication(applicationId);
		if (!deploymentExists(app.getName())) {
			return false;
		}
		kubernetesClient.apps().deployments()
				.inNamespace(finalNs)
				.withName(app.getName())
				.scale(replicas);
		return true;
	}

	public boolean stopDeployment(UUID applicationId) {
		Application app = getApplication(applicationId);
		if (!deploymentExists(app.getName())) {
			return false;
		}
		kubernetesClient.apps().deployments()
				.inNamespace(DEFAULT_NAMESPACE)
				.withName(app.getName())
				.scale(0);
		return true;
	}

	public boolean restartDeployment(UUID applicationId) {
		Application app = getApplication(applicationId);
		if (!deploymentExists(app.getName())) {
			return false;
		}
		kubernetesClient.apps().deployments()
				.inNamespace(DEFAULT_NAMESPACE)
				.withName(app.getName())
				.rolling()
				.restart();
		return true;
	}

	public void deployAllApplications() {
		List<Application> applications = applicationRepository.findAll();
		for (Application app : applications) {
			createDeployment(app.getId());
		}
	}

	public List<ActiveDeploymentResponse> listDeployments() {
		String finalNs = DEFAULT_NAMESPACE;

		List<Deployment> deployments = kubernetesClient.apps().deployments()
				.inNamespace(finalNs)
				.list()
				.getItems();

		if (deployments == null || deployments.isEmpty()) {
			return Collections.emptyList();
		}

		return deployments.stream()
				.map(d -> {
					String name = (d.getMetadata() != null) ? d.getMetadata().getName() : null;
					Container container = (d.getSpec() != null
							&& d.getSpec().getTemplate() != null
							&& d.getSpec().getTemplate().getSpec() != null
							&& d.getSpec().getTemplate().getSpec().getContainers() != null
							&& !d.getSpec().getTemplate().getSpec().getContainers().isEmpty())
							? d.getSpec().getTemplate().getSpec().getContainers().get(0)
							: null;

					String image = (container != null) ? container.getImage() : null;
					int containerPort = (container != null
							&& container.getPorts() != null
							&& !container.getPorts().isEmpty()
							&& container.getPorts().get(0).getContainerPort() != null)
							? container.getPorts().get(0).getContainerPort()
							: 0;

					int specReplicas = (d.getSpec() != null && d.getSpec().getReplicas() != null)
							? d.getSpec().getReplicas()
							: 0;
					int readyReplicas = (d.getStatus() != null && d.getStatus().getReadyReplicas() != null)
							? d.getStatus().getReadyReplicas()
							: 0;
					int availableReplicas = (d.getStatus() != null && d.getStatus().getAvailableReplicas() != null)
							? d.getStatus().getAvailableReplicas()
							: 0;
					int updatedReplicas = (d.getStatus() != null && d.getStatus().getUpdatedReplicas() != null)
							? d.getStatus().getUpdatedReplicas()
							: 0;
					boolean serviceEnabled = (name != null)
							&& kubernetesClient.services().inNamespace(finalNs).withName(name).get() != null;

					return new ActiveDeploymentResponse(
							name,
							image,
							specReplicas,
							containerPort,
							serviceEnabled,
							true,
							specReplicas,
							readyReplicas,
							availableReplicas,
							updatedReplicas
					);
				})
				.collect(Collectors.toList());
	}

	public List<ActiveDeploymentResponse> listActiveDeploymentsWithDbInfo() {
		String finalNs = DEFAULT_NAMESPACE;
		List<Application> apps = applicationRepository.findAll();

		return apps.stream()
				.map(app -> {
					Deployment d = kubernetesClient.apps().deployments()
							.inNamespace(finalNs)
							.withName(app.getName())
							.get();
					if (d == null) return null;

					int specReplicas = (d.getSpec() != null && d.getSpec().getReplicas() != null)
							? d.getSpec().getReplicas()
							: 0;
					int readyReplicas = (d.getStatus() != null && d.getStatus().getReadyReplicas() != null)
							? d.getStatus().getReadyReplicas()
							: 0;
					int availableReplicas = (d.getStatus() != null && d.getStatus().getAvailableReplicas() != null)
							? d.getStatus().getAvailableReplicas()
							: 0;
					int updatedReplicas = (d.getStatus() != null && d.getStatus().getUpdatedReplicas() != null)
							? d.getStatus().getUpdatedReplicas()
							: 0;

					boolean active = (specReplicas > 0) || (availableReplicas > 0);
					if (!active) return null;

					// NOTE: Adjust this constructor if your ApplicationResponse has a different shape.
					return new ActiveDeploymentResponse(
							app.getName(),
							app.getImage(),
							app.getDesiredReplicas(),
							app.getContainerPort(),
							app.getServiceEnabled(),
							true,
							specReplicas,
							readyReplicas,
							availableReplicas,
							updatedReplicas
					);
				})
				.filter(r -> r != null)
				.collect(Collectors.toList());
	}

	public boolean deleteDeployment(UUID applicationId) {
		Application app = getApplication(applicationId);
		if (!deploymentExists(app.getName())) {
			return false;
		}
		kubernetesClient.apps().deployments()
				.inNamespace(DEFAULT_NAMESPACE)
				.withName(app.getName())
				.delete();
		return true;
	}

	private boolean deploymentExists(String name) {
		String finalNs = DEFAULT_NAMESPACE;
		return kubernetesClient.apps().deployments()
				.inNamespace(finalNs)
				.withName(name)
				.get() != null;

	}
}
