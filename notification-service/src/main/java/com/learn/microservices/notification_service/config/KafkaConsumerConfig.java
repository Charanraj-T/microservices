package com.learn.microservices.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

	private final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

	@Bean
	public NewTopic orderDlqTopic(@Value("${app.kafka.topics.dlq:order-events.dlq}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}

	@Bean
	public CommonErrorHandler errorHandler(
			KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${app.kafka.topics.dlq:order-events.dlq}") String dlqTopic
	) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
				kafkaTemplate,
				(record, exception) -> new TopicPartition(dlqTopic, record.partition())
		);

		FixedBackOff backOff = new FixedBackOff(1000L, 3L);
		DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

		errorHandler.setRetryListeners((record, exception, deliveryAttempt) ->
				logger.warn("[RETRY ATTEMPT {}] Consumer failure for key={}: {}",
						deliveryAttempt, record.key(), exception.getMessage())
		);

		return errorHandler;
	}
}

