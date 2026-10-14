package com.learn.microservices.inventory_service.controller;

import java.util.Optional;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learn.microservices.inventory_service.dto.InventoryResponse;
import com.learn.microservices.inventory_service.model.InventoryItem;
import com.learn.microservices.inventory_service.repository.InventoryRepository;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

	private final InventoryRepository inventoryRepository;
	private final Environment environment;

	public InventoryController(InventoryRepository inventoryRepository, Environment environment) {
		this.inventoryRepository = inventoryRepository;
		this.environment = environment;
	}

	@PostConstruct
	public void loadSampleData() {
		if (inventoryRepository.count() == 0) {
			inventoryRepository.save(new InventoryItem("IPHONE_16", 100));
			inventoryRepository.save(new InventoryItem("MACBOOK_PRO", 50));
			inventoryRepository.save(new InventoryItem("PIXEL_9", 0));
		}
	}

	@GetMapping("/{skuCode}")
	public ResponseEntity<InventoryResponse> isInStock(@PathVariable String skuCode) {
		String port = environment.getProperty("local.server.port");

		Optional<InventoryItem> itemOptional = inventoryRepository.findBySkuCode(skuCode);
		if (itemOptional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new InventoryResponse(skuCode, false, 0, port));
		}

		InventoryItem item = itemOptional.get();
		boolean inStock = item.quantity() > 0;
		return ResponseEntity.ok(new InventoryResponse(skuCode, inStock, item.quantity(), port));

	}

	@PostMapping
	public ResponseEntity<InventoryItem> addItem(@RequestBody InventoryItem item) {
		InventoryItem saved = inventoryRepository.save(item);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}
}
