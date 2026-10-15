package com.learn.microservices.order_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "orders")
public record Order(
	@Id String id,
	String orderNumber,
	String skuCode,
	int quantity,
	double price,
	String inventoryServicePort
) {
	public Order(String orderNumber, String skuCode, int quantity, double price, String inventoryServicePort) {
		this(null, orderNumber, skuCode, quantity, price, inventoryServicePort);
	}
}
