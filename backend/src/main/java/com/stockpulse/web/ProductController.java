package com.stockpulse.web;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        List<Product> products = productService.getProducts(status, category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        try {
            Product updated = productService.updateProduct(id, product);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productService.getProductById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pricing-suggestions")
    public ResponseEntity<List<PricingSuggestion>> getPricingSuggestions(
            @PathVariable String id,
            @RequestParam(required = false) SuggestionStatus status) {
        if (productService.getProductById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.getPricingSuggestions(id, status));
    }

    @PostMapping("/{id}/pricing-suggestions")
    public ResponseEntity<PricingSuggestion> createPricingSuggestion(
            @PathVariable String id) {
        return productService.suggestPricing(id)
                .map(suggestion -> ResponseEntity.status(HttpStatus.CREATED).body(suggestion))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pricing-suggestions/{suggestionId}/accept")
    public ResponseEntity<PricingSuggestion> acceptPricingSuggestion(
            @PathVariable String id, @PathVariable String suggestionId) {
        try {
            return ResponseEntity.ok(productService.acceptPricingSuggestion(id, suggestionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{id}/pricing-suggestions/{suggestionId}/reject")
    public ResponseEntity<PricingSuggestion> rejectPricingSuggestion(
            @PathVariable String id, @PathVariable String suggestionId) {
        try {
            return ResponseEntity.ok(productService.rejectPricingSuggestion(id, suggestionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/{id}/reorder-suggestions")
    public ResponseEntity<List<ReorderSuggestion>> getReorderSuggestions(
            @PathVariable String id,
            @RequestParam(required = false) SuggestionStatus status) {
        if (productService.getProductById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.getReorderSuggestions(id, status));
    }

    @PostMapping("/{id}/reorder-suggestions")
    public ResponseEntity<ReorderSuggestion> createReorderSuggestion(
            @PathVariable String id) {
        return productService.suggestReorder(id)
                .map(suggestion -> ResponseEntity.status(HttpStatus.CREATED).body(suggestion))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reorder-suggestions/{suggestionId}/accept")
    public ResponseEntity<ReorderSuggestion> acceptReorderSuggestion(
            @PathVariable String id, @PathVariable String suggestionId) {
        try {
            return ResponseEntity.ok(productService.acceptReorderSuggestion(id, suggestionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{id}/reorder-suggestions/{suggestionId}/reject")
    public ResponseEntity<ReorderSuggestion> rejectReorderSuggestion(
            @PathVariable String id, @PathVariable String suggestionId) {
        try {
            return ResponseEntity.ok(productService.rejectReorderSuggestion(id, suggestionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
