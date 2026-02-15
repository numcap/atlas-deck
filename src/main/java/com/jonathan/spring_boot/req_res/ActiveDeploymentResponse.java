package com.jonathan.spring_boot.req_res;


import lombok.*;

import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveDeploymentResponse {
	private String name;
	private String image;
	private int desiredReplicas;
	private int containerPort;
	private boolean serviceEnabled;
	private boolean deploymentExists;
	private int specReplicas;
	private int readyReplicas;
	private int availableReplicas;
	private int updatedReplicas;
}