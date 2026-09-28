package com.stockpulse.repository;

import com.stockpulse.domain.InventorySnapshot;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ChangeDirection;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepositoryTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Test
    void testProductRepositorySeedDataAndQueries() {
        List<Product> all = productRepository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.size()).isGreaterThanOrEqualTo(8);

        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        assertThat(activeProducts).isNotEmpty();

        List<Product> electronics = productRepository.findByCategory(Category.ELECTRONICS);
        assertThat(electronics).isNotEmpty();

        List<Product> activeElectronics = productRepository.findByStatusAndCategory(ProductStatus.ACTIVE, Category.ELECTRONICS);
        assertThat(activeElectronics).isNotEmpty();
        assertThat(activeElectronics).allMatch(p -> p.getStatus() == ProductStatus.ACTIVE && p.getCategory() == Category.ELECTRONICS);

        Optional<Product> prod1 = productRepository.findById("prod-1");
        assertThat(prod1).isPresent();
        assertThat(prod1.get().getCostPrice()).isNotNull();
        assertThat(prod1.get().getMarginFloor()).isNotNull();
        assertThat(prod1.get().getSupplierId()).isEqualTo("SUP-A1");
    }

    @Test
    void testPricingSuggestionRepository() {
        List<PricingSuggestion> suggestions = pricingSuggestionRepository.findByProductIdAndStatus("prod-3", SuggestionStatus.PENDING);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getChangeDirection()).isEqualTo(ChangeDirection.INCREASE);
        assertThat(suggestions.get(0).getTriggerReason()).isEqualTo(TriggerReason.DEMAND_SPIKE);

        boolean exists = pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                "prod-3", TriggerReason.DEMAND_SPIKE, SuggestionStatus.PENDING);
        assertThat(exists).isTrue();

        boolean notExists = pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                "prod-3", TriggerReason.INITIAL, SuggestionStatus.PENDING);
        assertThat(notExists).isFalse();
    }

    @Test
    void testReorderSuggestionRepository() {
        List<ReorderSuggestion> suggestions = reorderSuggestionRepository.findByProductIdAndStatus("prod-1", SuggestionStatus.PENDING);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getRecommendedQuantity()).isEqualTo(50);
        assertThat(suggestions.get(0).getTriggerReason()).isEqualTo(TriggerReason.INVENTORY_LOW);
        assertThat(suggestions.get(0).getLeadTimeDays()).isEqualTo(7);

        boolean exists = reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                "prod-1", TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING);
        assertThat(exists).isTrue();

        boolean notExists = reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                "prod-1", TriggerReason.DEMAND_SPIKE, SuggestionStatus.ACCEPTED);
        assertThat(notExists).isFalse();
    }

    @Test
    void testCreateAndSaveEntities() {
        Product newProduct = new Product(
                "prod-test", "TEST-SKU-999", "Test Product", Category.HOME,
                BigDecimal.valueOf(29.99), 10, 5, 2, ProductStatus.ACTIVE,
                BigDecimal.valueOf(15.00), BigDecimal.valueOf(20.00), "SUP-TEST"
        );
        productRepository.save(newProduct);

        Optional<Product> retrieved = productRepository.findById("prod-test");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Test Product");
    }

    @Test
    void testInventorySnapshotPersistence() {
        InventorySnapshot snapshot = new InventorySnapshot("prod-1", 15, 8);
        InventorySnapshot saved = inventorySnapshotRepository.save(snapshot);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo("prod-1");
        assertThat(saved.getStockLevel()).isEqualTo(15);
        assertThat(saved.getDemandVelocity()).isEqualTo(8);
        assertThat(saved.getCapturedAt()).isNotNull();

        List<InventorySnapshot> snapshots = inventorySnapshotRepository.findByProductId("prod-1");
        assertThat(snapshots).isNotEmpty();
        assertThat(snapshots).anyMatch(s -> s.getId().equals(saved.getId()));
    }
}
