package com.jonathan.spring_boot;

import com.jonathan.spring_boot.req_res.ActiveDeploymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deployment")
@RequiredArgsConstructor
public class DeploymentController {

	private final DeploymentService deploymentService;

	@PostMapping("/all")
	public void deployApplications() {
		deploymentService.deployAllApplications();
	}

	@PostMapping("/{applicationId}")
	public void deployApplication(@PathVariable UUID applicationId) {
		deploymentService.createDeployment(applicationId);
	}

	@GetMapping
	public ResponseEntity<List<ActiveDeploymentResponse>> getDeployments() {
		return ResponseEntity.ok(deploymentService.listDeployments());
	}

	@GetMapping("/active")
	public ResponseEntity<List<ActiveDeploymentResponse>> getActiveDeployments() {
		return ResponseEntity.ok(deploymentService.listActiveDeploymentsWithDbInfo());
	}

	@DeleteMapping("/{applicationId}")
	public ResponseEntity<String> deleteDeployment(@PathVariable UUID applicationId) {
		if (applicationId == null || applicationId.toString().isEmpty()) {
			throw new IllegalArgumentException("Application name must be provided");
		}
		if (!deploymentService.deleteDeployment(applicationId)) {
			return ResponseEntity.status(404).body("Deployment not found for application: " + applicationId);
		}
		return ResponseEntity.ok("Deleted deployment for application: " + applicationId);
	}

	@PostMapping("/{applicationId}/service")
	public void exposeApplication(
			@PathVariable UUID applicationId,
			@RequestParam(defaultValue = "NodePort") String type
	) {
		deploymentService.exposeApplication(applicationId, type);
	}

	@PostMapping("/{applicationId}/start")
	public ResponseEntity<String> startDeployment(
			@PathVariable UUID applicationId,
			@RequestParam(defaultValue = "1") int replicas
	) {
		if (applicationId == null || applicationId.toString().isEmpty()) {
			throw new IllegalArgumentException("Application name must be provided");
		}
		if (replicas < 0) {
			throw new IllegalArgumentException("Replicas must be >= 0");
		}
		if (!deploymentService.startDeployment(applicationId, replicas)) {
			return ResponseEntity.status(404).body("Deployment not found for application: " + applicationId);
		}
		return ResponseEntity.ok("Scaled deployment for application: " + applicationId);
	}

	@PostMapping("/{applicationId}/stop")
	public ResponseEntity<String> stopDeployment(@PathVariable UUID applicationId) {
		if (applicationId == null || applicationId.toString().isEmpty()) {
			throw new IllegalArgumentException("Application name must be provided");
		}
		if (!deploymentService.stopDeployment(applicationId)) {
			return ResponseEntity.status(404).body("Deployment not found for application: " + applicationId);
		}
		return ResponseEntity.ok("Stopped deployment for application: " + applicationId);
	}

	@PostMapping("/{applicationId}/restart")
	public ResponseEntity<String> restartDeployment(@PathVariable UUID applicationId) {
		if (applicationId == null || applicationId.toString().isEmpty()) {
			throw new IllegalArgumentException("Application name must be provided");
		}
		if (!deploymentService.restartDeployment(applicationId)) {
			return ResponseEntity.status(404).body("Deployment not found for application: " + applicationId);
		}
		return ResponseEntity.ok("Restarted deployment for application: " + applicationId);
	}
}
