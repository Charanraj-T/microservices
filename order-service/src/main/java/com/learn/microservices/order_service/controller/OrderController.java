package com.learn.microservices.order_service.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learn.microservices.order_service.client.InventoryClient;
import com.learn.microservices.order_service.dto.InventoryResponse;
import com.learn.microservices.order_service.dto.OrderRequest;
import com.learn.microservices.order_service.dto.OrderResponse;
import com.learn.microservices.event.OrderPlacedEvent;
import com.learn.microservices.order_service.model.Order;
import com.learn.microservices.order_service.producer.OrderEventProducer;
import com.learn.microservices.order_service.repository.OrderRepository;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderRepository orderRepository;
	private final InventoryClient inventoryClient;
	private final OrderEventProducer orderEventProducer;

	public OrderController(
			OrderRepository orderRepository,
			InventoryClient inventoryClient,
			OrderEventProducer orderEventProducer
	) {
		this.orderRepository = orderRepository;
		this.inventoryClient = inventoryClient;
		this.orderEventProducer = orderEventProducer;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
		InventoryResponse inventoryResponse = inventoryClient.checkStock(request.skuCode());

		if (!inventoryResponse.inStock() || inventoryResponse.quantity() < request.quantity()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new OrderResponse(
							null,
							null,
							request.skuCode(),
							request.quantity(),
							request.price(),
							"FAILED",
							"Item is out of stock or requested quantity exceeds available stock.",
							inventoryResponse.environmentPort()
					));
		}

		String orderNumber = UUID.randomUUID().toString();
		Order order = new Order(
				orderNumber,
				request.skuCode(),
				request.quantity(),
				request.price(),
				inventoryResponse.environmentPort()
		);

		Order savedOrder = orderRepository.save(order);

		OrderPlacedEvent event = OrderPlacedEvent.newBuilder()
				.setOrderNumber(savedOrder.orderNumber())
				.setSkuCode(savedOrder.skuCode())
				.setQuantity(savedOrder.quantity())
				.setPrice(savedOrder.price())
				.build();
		orderEventProducer.sendOrderPlacedEvent(event);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new OrderResponse(
						savedOrder.id(),
						savedOrder.orderNumber(),
						savedOrder.skuCode(),
						savedOrder.quantity(),
						savedOrder.price(),
						"SUCCESS",
						"Order placed successfully.",
						inventoryResponse.environmentPort()
				));
	}

	@GetMapping
	public ResponseEntity<List<Order>> getAllOrders() {
		return ResponseEntity.ok(orderRepository.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Order> getOrderById(@PathVariable String id) {
		return orderRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
