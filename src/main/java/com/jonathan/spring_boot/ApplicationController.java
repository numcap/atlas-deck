package com.jonathan.spring_boot;

import com.jonathan.spring_boot.req_res.ApplicationCreateRequest;
import com.jonathan.spring_boot.req_res.ApplicationResponse;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class ApplicationController {

	private final ApplicationService applicationService;

	@GetMapping
	public List<ApplicationResponse> getApplications() {
		return applicationService.list();
	}

	@PostMapping
	public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationCreateRequest request) {
		if (applicationService.existsByName(request.getName())) {
			return ResponseEntity.status(409).build(); // Conflict
		}
		return ResponseEntity.ok(applicationService.createApplication(request));
	}

	@PostMapping("/update/{applicationId}")
	public ApplicationResponse updateApplication(@PathVariable UUID applicationId, @RequestBody ApplicationCreateRequest request) {
		return applicationService.updateApplication(applicationId, request);
	}

	@DeleteMapping("/{applicationId}")
	public ResponseEntity<String> deleteApplication(@PathVariable UUID applicationId) {
		applicationService.delete(applicationId);
		return ResponseEntity.ok("Deleted application with Id: " + applicationId);
	}

}
