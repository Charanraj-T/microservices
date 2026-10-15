package com.learn.microservices.order_service.dto;

public record InventoryResponse(
	String skuCode,
	boolean inStock,
	int quantity,
	String environmentPort
) {}
