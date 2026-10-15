package com.learn.microservices.api_gateway.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

	@RequestMapping("/orders")
	public Mono<ResponseEntity<Map<String, Object>>> orderServiceFallback() {
		return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
				"status", "SERVICE_DEGRADED",
				"service", "order-service",
				"message", "Order Service is currently experiencing delays or is offline. Please try again in a few moments.",
				"timestamp", LocalDateTime.now().toString()
		)));
	}

	@RequestMapping("/inventory")
	public Mono<ResponseEntity<Map<String, Object>>> inventoryServiceFallback() {
		return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
				"status", "SERVICE_DEGRADED",
				"service", "inventory-service",
				"message", "Inventory Service is currently offline. Stock verification is temporarily unavailable.",
				"timestamp", LocalDateTime.now().toString()
		)));
	}
}

