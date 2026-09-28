package com.stockpulse.service;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    public ProductService(ProductRepository productRepository,
                          PricingSuggestionRepository pricingSuggestionRepository,
                          ReorderSuggestionRepository reorderSuggestionRepository) {
        this.productRepository = productRepository;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> getProducts(ProductStatus status, Category category) {
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            return productRepository.findByStatus(status);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        if (productDetails.getSku() != null) {
            product.setSku(productDetails.getSku());
        }
        if (productDetails.getName() != null) {
            product.setName(productDetails.getName());
        }
        if (productDetails.getCategory() != null) {
            product.setCategory(productDetails.getCategory());
        }
        if (productDetails.getCurrentPrice() != null) {
            product.setCurrentPrice(productDetails.getCurrentPrice());
        }
        if (productDetails.getStockLevel() != null) {
            product.setStockLevel(productDetails.getStockLevel());
        }
        if (productDetails.getReorderThreshold() != null) {
            product.setReorderThreshold(productDetails.getReorderThreshold());
        }
        if (productDetails.getDemandVelocity() != null) {
            product.setDemandVelocity(productDetails.getDemandVelocity());
        }
        if (productDetails.getStatus() != null) {
            product.setStatus(productDetails.getStatus());
        }
        if (productDetails.getCostPrice() != null) {
            product.setCostPrice(productDetails.getCostPrice());
        }
        if (productDetails.getMarginFloor() != null) {
            product.setMarginFloor(productDetails.getMarginFloor());
        }
        if (productDetails.getSupplierId() != null) {
            product.setSupplierId(productDetails.getSupplierId());
        }

        return productRepository.save(product);
    }

    public Product updateStock(String id, Integer newStockLevel) {
        if (newStockLevel < 0) {
            throw new IllegalArgumentException("Stock level cannot be negative");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        product.setStockLevel(newStockLevel);
        if (newStockLevel == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return productRepository.save(product);
    }

    public Product processOrder(String id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        int remaining = product.getStockLevel() - quantity;
        if (remaining < 0) {
            throw new IllegalStateException("Insufficient stock for product id: " + id);
        }
        product.setStockLevel(remaining);
        if (remaining == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestion> getPricingSuggestions(String productId, SuggestionStatus status) {
        if (status != null) {
            return pricingSuggestionRepository.findByProductIdAndStatus(productId, status);
        }
        return pricingSuggestionRepository.findAll().stream()
                .filter(s -> s.getProductId().equals(productId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestion> getReorderSuggestions(String productId, SuggestionStatus status) {
        if (status != null) {
            return reorderSuggestionRepository.findByProductIdAndStatus(productId, status);
        }
        return reorderSuggestionRepository.findAll().stream()
                .filter(s -> s.getProductId().equals(productId))
                .toList();
    }

    public PricingSuggestion createPricingSuggestion(PricingSuggestion suggestion) {
        return pricingSuggestionRepository.save(suggestion);
    }

    public ReorderSuggestion createReorderSuggestion(ReorderSuggestion suggestion) {
        return reorderSuggestionRepository.save(suggestion);
    }
}
