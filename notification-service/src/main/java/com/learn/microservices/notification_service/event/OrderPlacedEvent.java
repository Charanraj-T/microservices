package com.learn.microservices.notification_service.event;

public record OrderPlacedEvent(
		String orderNumber,
		String skuCode,
		int quantity,
		double price,
		String timestamp
) {
}

