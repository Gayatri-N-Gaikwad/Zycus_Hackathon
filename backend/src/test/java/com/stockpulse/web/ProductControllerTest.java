package com.stockpulse.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)));
    }

    @Test
    void testFilterProductsByStatusAndCategory() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("status", "ACTIVE")
                        .param("category", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetProductByIdFound() throws Exception {
        mockMvc.perform(get("/api/products/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-1"))
                .andExpect(jsonPath("$.sku").value("ELEC-HDPH-001"))
                .andExpect(jsonPath("$.category").value("ELECTRONICS"));
    }

    @Test
    void testGetProductByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/products/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateProduct() throws Exception {
        Product newProduct = new Product(
                "prod-new-1", "TEST-SKU-NEW", "Brand New Item", Category.APPAREL,
                BigDecimal.valueOf(59.99), 50, 10, 3, ProductStatus.ACTIVE
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("prod-new-1"))
                .andExpect(jsonPath("$.name").value("Brand New Item"));
    }

    @Test
    void testUpdateProduct() throws Exception {
        Product update = new Product();
        update.setName("Updated Headphones Name");
        update.setStockLevel(30);

        mockMvc.perform(put("/api/products/prod-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Headphones Name"))
                .andExpect(jsonPath("$.stockLevel").value(30));
    }

    @Test
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/prod-2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/prod-2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPricingSuggestions() throws Exception {
        mockMvc.perform(get("/api/products/prod-3/pricing-suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recommendedPrice").value(89.99));
    }

    @Test
    void testGetReorderSuggestions() throws Exception {
        mockMvc.perform(get("/api/products/prod-1/reorder-suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recommendedQuantity").value(50));
    }
}
