package com.jonathan.spring_boot.req_res;

import lombok.*;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationResponse {
	private UUID id;
	private String name;
	private String image;
	private Integer desiredReplicas;   // optional, default handled in service
	private Integer containerPort;     // optional
	private Boolean serviceEnabled;    // optional
	private JsonNode env;               // json string, optional
	private String resources_cpu;
	private String resources_ram;
	private Instant createdAt;
	private Instant updatedAt;
}
