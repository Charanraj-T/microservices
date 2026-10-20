package com.learn.microservices.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

	@Bean
	public NewTopic orderEventsTopic(@Value("${app.kafka.topics.order-events:order-events}") String topic) {
		return TopicBuilder.name(topic)
				.partitions(3)
				.replicas(1)
				.build();
	}
}
