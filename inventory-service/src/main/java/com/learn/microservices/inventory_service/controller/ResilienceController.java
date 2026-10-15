package com.learn.microservices.inventory_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@RestController
public class ResilienceController {

	private final Logger logger = LoggerFactory.getLogger(ResilienceController.class);

	@GetMapping("/sample-retry")
	@Retry(name = "sample-retry", fallbackMethod = "fallbackResponse")
	public String sampleRetry() {
		logger.info("Sample retry API call received");
		throw new RuntimeException("Simulated Service Outage");
	}

	@GetMapping("/sample-circuit-breaker")
	@CircuitBreaker(name = "sample-circuit-breaker", fallbackMethod = "fallbackResponse")
	public String sampleCircuitBreaker() {
		logger.info("Sample circuit breaker call received");
		throw new RuntimeException("Simulated Failure");
	}

	public String fallbackResponse(Throwable t) {
		logger.info("Fallback triggered due to: {}", t.getMessage());
		return "fallback-response: [" + t.getClass().getSimpleName() + "] " + t.getMessage();
	}
}

