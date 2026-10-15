package com.learn.microservices.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter {

	private final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		logger.info("Incoming Gateway request path -> {}", exchange.getRequest().getPath());

		return chain.filter(exchange).then(Mono.fromRunnable(() -> {
			logger.info("Outgoing Gateway response status -> {}", exchange.getResponse().getStatusCode());
		}));
	}
}
