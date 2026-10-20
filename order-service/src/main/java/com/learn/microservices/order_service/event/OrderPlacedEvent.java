package com.learn.microservices.order_service.event;

public record OrderPlacedEvent(
		String orderNumber,
		String skuCode,
		int quantity,
		double price,
		String timestamp
) {
}

