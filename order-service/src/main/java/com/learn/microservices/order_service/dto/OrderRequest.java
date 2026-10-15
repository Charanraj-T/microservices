package com.learn.microservices.order_service.dto;

public record OrderRequest(
	String skuCode,
	int quantity,
	double price
) {}
