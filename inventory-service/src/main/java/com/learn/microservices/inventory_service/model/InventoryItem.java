package com.learn.microservices.inventory_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventory")
public record InventoryItem(
	@Id String id,
	String skuCode,
	int quantity
) {
	public InventoryItem(String skuCode, int quantity) {
		this(null, skuCode, quantity);
	}
}
