package com.stockpulse.repository;

import com.stockpulse.domain.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, String> {
    List<InventorySnapshot> findByProductId(String productId);
    List<InventorySnapshot> findByProductIdOrderByCapturedAtDesc(String productId);
}
