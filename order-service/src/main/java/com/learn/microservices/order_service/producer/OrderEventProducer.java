package com.learn.microservices.order_service.producer;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.learn.microservices.event.OrderPlacedEvent;

@Service
public class OrderEventProducer {

	private final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);
	private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
	private final String topic;

	public OrderEventProducer(
			KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
			@Value("${app.kafka.topics.order-events:order-events}") String topic
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	public void sendOrderPlacedEvent(OrderPlacedEvent event) {
		logger.info("Publishing OrderPlacedEvent for orderNumber: {}", event.getOrderNumber());

		CompletableFuture<SendResult<String, OrderPlacedEvent>> future =
				kafkaTemplate.send(this.topic, event.getOrderNumber(), event);

		future.whenComplete((result, ex) -> {
			if (ex == null) {
				logger.info("OrderPlacedEvent successfully published to topic '{}', partition: {}, offset: {}",
						result.getRecordMetadata().topic(),
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			} else {
				logger.error("Failed to publish OrderPlacedEvent for orderNumber '{}' due to: {}",
						event.getOrderNumber(), ex.getMessage());
			}
		});
	}
}
