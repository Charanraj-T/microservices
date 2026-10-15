package com.learn.microservices.order_service.dto;

public record OrderResponse(
	String orderId,
	String orderNumber,
	String skuCode,
	int quantity,
	double price,
	String status,
	String message,
	String inventoryServicePort
) {}
