package com.jonathan.spring_boot;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class K8sConfig {

	@Bean
	public KubernetesClient kubernetesClient() {
		return new KubernetesClientBuilder().build();
	}


}
