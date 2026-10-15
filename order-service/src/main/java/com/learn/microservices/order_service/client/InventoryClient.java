package com.learn.microservices.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.learn.microservices.order_service.dto.InventoryResponse;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

	@GetMapping("/inventory/{skuCode}")
	InventoryResponse checkStock(@PathVariable String skuCode);
}
