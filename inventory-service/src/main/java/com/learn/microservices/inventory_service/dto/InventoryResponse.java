package com.learn.microservices.inventory_service.dto;

public record InventoryResponse(
	String skuCode,
	boolean inStock,
	int quantity,
	String environmentPort
) {}
