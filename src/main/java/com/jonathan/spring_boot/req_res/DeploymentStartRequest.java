package com.jonathan.spring_boot.req_res;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeploymentStartRequest {
	private String deploymentId;
	private String applicationName;
	private Integer replicas;
}
