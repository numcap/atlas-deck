package com.jonathan.spring_boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jonathan.spring_boot.req_res.ApplicationCreateRequest;
import com.jonathan.spring_boot.req_res.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {
	private static final ObjectMapper JACKSON2_MAPPER = new ObjectMapper();
	private static final tools.jackson.databind.ObjectMapper JACKSON3_MAPPER = new tools.jackson.databind.ObjectMapper();

	private final ApplicationRepository applicationRepository;

	public ApplicationResponse createApplication(ApplicationCreateRequest applicationCreateRequest) {
		if (applicationRepository.existsByName(applicationCreateRequest.getName())) {
			throw new RuntimeException("Application with name " + applicationCreateRequest.getName() + " already exists");
		}

		Application application = Application.builder()
				.name(applicationCreateRequest.getName())
				.image(applicationCreateRequest.getImage())
				.desiredReplicas(applicationCreateRequest.getDesiredReplicas() != null ? applicationCreateRequest.getDesiredReplicas() : 1)
				.containerPort(applicationCreateRequest.getContainerPort() != null ? applicationCreateRequest.getContainerPort() : 80)
				.serviceEnabled(applicationCreateRequest.getServiceEnabled() != null ? applicationCreateRequest.getServiceEnabled() : false)
				.env(toJackson2(applicationCreateRequest.getEnv()))
				.resources_cpu(applicationCreateRequest.getResources_cpu())
				.resources_ram(applicationCreateRequest.getResources_ram())
				.build();

		Application saved = applicationRepository.save(application);
		return toResponse(saved);
	}

	public ApplicationResponse updateApplication(UUID id, ApplicationCreateRequest applicationCreateRequest) {
		Application existingApp = applicationRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Application not found: " + id));

		existingApp.setName(applicationCreateRequest.getName());
		existingApp.setImage(applicationCreateRequest.getImage());
		existingApp.setDesiredReplicas(applicationCreateRequest.getDesiredReplicas() != null ? applicationCreateRequest.getDesiredReplicas() : existingApp.getDesiredReplicas());
		existingApp.setContainerPort(applicationCreateRequest.getContainerPort() != null ? applicationCreateRequest.getContainerPort() : existingApp.getContainerPort());
		existingApp.setServiceEnabled(applicationCreateRequest.getServiceEnabled() != null ? applicationCreateRequest.getServiceEnabled() : existingApp.getServiceEnabled());
		existingApp.setEnv(toJackson2(applicationCreateRequest.getEnv()));
		existingApp.setResources_cpu(applicationCreateRequest.getResources_cpu());
		existingApp.setResources_ram(applicationCreateRequest.getResources_ram());

		Application updated = applicationRepository.save(existingApp);
		return toResponse(updated);
	}

	public List<ApplicationResponse> list() {
		return applicationRepository.findAll().stream().map(this::toResponse).toList();
	}

	public ApplicationResponse get(UUID id) {
		Application app = applicationRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Application not found: " + id));
		return toResponse(app);
	}

	public Boolean existsByName(String name) {
		return applicationRepository.existsByName(name);
	}

	public void delete(UUID id) {
		if (!applicationRepository.existsById(id)) {
			throw new RuntimeException("Application not found: " + id);
		}
		applicationRepository.deleteById(id);
	}

	private ApplicationResponse toResponse(Application saved) {
		return ApplicationResponse.builder()
				.id(saved.getId())
				.name(saved.getName())
				.image(saved.getImage())
				.desiredReplicas(saved.getDesiredReplicas())
				.containerPort(saved.getContainerPort())
				.serviceEnabled(saved.getServiceEnabled())
				.env(toJackson3(saved.getEnv()))
				.resources_cpu(saved.getResources_cpu())
				.resources_ram(saved.getResources_ram())
				.createdAt(saved.getCreatedAt())
				.updatedAt(saved.getUpdatedAt())
				.build();
	}

	private static JsonNode toJackson2(tools.jackson.databind.JsonNode env) {
		if (env == null || env.isNull() || env.isMissingNode()) {
			return JsonNodeFactory.instance.objectNode();
		}
		try {
			return JACKSON2_MAPPER.readTree(env.toString());
		} catch (Exception e) {
			return JsonNodeFactory.instance.objectNode();
		}
	}

	private static tools.jackson.databind.JsonNode toJackson3(JsonNode env) {
		if (env == null || env.isNull() || env.isMissingNode()) {
			return tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
		}
		try {
			return JACKSON3_MAPPER.readTree(env.toString());
		} catch (Exception e) {
			return tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
		}
	}

}
