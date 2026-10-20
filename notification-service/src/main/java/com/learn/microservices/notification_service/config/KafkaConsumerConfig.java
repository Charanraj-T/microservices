package com.learn.microservices.notification_service.config;

import java.util.Map;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.util.backoff.FixedBackOff;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

@Configuration
public class KafkaConsumerConfig {

	private final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

	@Bean
	public NewTopic orderDlqTopic(@Value("${app.kafka.topics.dlq:order-events.dlq}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}

	@Bean
	public ProducerFactory<String, Object> producerFactory(
			@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
			@Value("${spring.kafka.consumer.properties.schema.registry.url:http://localhost:8081}") String schemaRegistryUrl
	) {
		KafkaAvroSerializer avroSerializer = new KafkaAvroSerializer();
		avroSerializer.configure(Map.of("schema.registry.url", schemaRegistryUrl), false);

		DelegatingByTypeSerializer byTypeSerializer = new DelegatingByTypeSerializer(
				Map.of(
						byte[].class, new ByteArraySerializer(),
						SpecificRecord.class, avroSerializer
				),
				true
		);

		Map<String, Object> producerProps = Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
				"schema.registry.url", schemaRegistryUrl
		);

		DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(producerProps);
		factory.setValueSerializer(byTypeSerializer);
		return factory;
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
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

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
		errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

		errorHandler.setRetryListeners((record, exception, deliveryAttempt) ->
				logger.warn("[RETRY ATTEMPT {}] Consumer failure for key={}: {}",
						deliveryAttempt, record.key(), exception.getMessage())
		);

		return errorHandler;
	}
}
