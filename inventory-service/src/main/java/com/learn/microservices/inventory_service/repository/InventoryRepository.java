package com.learn.microservices.inventory_service.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.learn.microservices.inventory_service.model.InventoryItem;

public interface InventoryRepository extends MongoRepository<InventoryItem, String> {
	Optional<InventoryItem> findBySkuCode(String skuCode);
}
