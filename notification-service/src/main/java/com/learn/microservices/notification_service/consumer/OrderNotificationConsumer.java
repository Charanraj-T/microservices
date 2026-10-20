package com.learn.microservices.notification_service.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.learn.microservices.notification_service.event.OrderPlacedEvent;

@Component
public class OrderNotificationConsumer {

	private final Logger logger = LoggerFactory.getLogger(OrderNotificationConsumer.class);

	@KafkaListener(topics = "${app.kafka.topics.order-events:order-events}")
	public void handleOrderPlacedEvent(
			@Payload(required = false) OrderPlacedEvent event,
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
			@Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
			@Header(name = KafkaHeaders.OFFSET, required = false) Long offset
	) {
		if (event == null) {
			logger.warn("[CORRUPT RECORD SKIPPED] Received null payload for key={}, partition={}, offset={}", key, partition, offset);
			return;
		}

		logger.info("[NOTIFICATION DISPATCHED] Order #{} confirmed! SKU: '{}', Quantity: {}, Total: ${}, Partition: {}, Offset: {}",
				event.orderNumber(),
				event.skuCode(),
				event.quantity(),
				event.price() * event.quantity(),
				partition,
				offset);

		logger.info("[EMAIL/SMS SENT] Customer confirmation sent successfully for Order #{}.", event.orderNumber());
	}
}
