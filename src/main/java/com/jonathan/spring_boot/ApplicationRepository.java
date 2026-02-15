package com.jonathan.spring_boot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
	Optional<Application> findByName(String name);

	boolean existsByName(String name);
}
