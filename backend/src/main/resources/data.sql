-- Seed Data for StockPulse Database

-- Products
MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-1', 'ELEC-HDPH-001', 'Ultra-Clear Noise-Cancelling Headphones', 'ELECTRONICS', 199.99, 15, 25, 8, 'ACTIVE', 110.00, 140.00, 'SUP-A1');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-2', 'ELEC-TV-002', '55-inch 4K HDR Smart OLED TV', 'ELECTRONICS', 899.99, 8, 10, 3, 'ACTIVE', 550.00, 699.00, 'SUP-A2');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-3', 'ELEC-MOU-003', 'Wireless Gaming Mouse Pro', 'ELECTRONICS', 79.99, 45, 20, 15, 'ACTIVE', 35.00, 50.00, 'SUP-A1');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-4', 'ELEC-WAT-004', 'Smart Fitness Watch Gen 4', 'ELECTRONICS', 149.99, 0, 30, 12, 'OUT_OF_STOCK', 75.00, 99.00, 'SUP-A3');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-5', 'APP-SHT-001', 'Classic Oxford Cotton Button-Down Shirt', 'APPAREL', 49.50, 60, 20, 5, 'ACTIVE', 18.00, 30.00, 'SUP-B1');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-6', 'APP-JKT-002', 'Waterproof Hooded Rain Jacket', 'APPAREL', 119.00, 12, 15, 9, 'PRICE_REVIEW_PENDING', 48.00, 75.00, 'SUP-B2');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-7', 'HOME-MAT-001', 'Ergonomic Memory Foam Mattress Queen', 'HOME', 499.00, 18, 10, 4, 'ACTIVE', 240.00, 350.00, 'SUP-C1');

MERGE INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, cost_price, margin_floor, supplier_id) KEY(id)
VALUES ('prod-8', 'HOME-COF-002', 'Modern Ceramic Pour-Over Coffee Maker', 'HOME', 34.99, 5, 15, 7, 'ACTIVE', 12.00, 22.00, 'SUP-C2');

-- Pricing Suggestions
MERGE INTO pricing_suggestions (id, product_id, current_price, recommended_price, change_direction, confidence, reasoning, status, trigger_reason, created_at) KEY(id)
VALUES ('psug-1', 'prod-3', 79.99, 89.99, 'INCREASE', 0.88, 'Demand velocity has spiked to 15 units/day with healthy inventory buffer. Margin can be optimized without reducing conversion.', 'PENDING', 'DEMAND_SPIKE', CURRENT_TIMESTAMP);

MERGE INTO pricing_suggestions (id, product_id, current_price, recommended_price, change_direction, confidence, reasoning, status, trigger_reason, created_at) KEY(id)
VALUES ('psug-2', 'prod-6', 119.00, 109.00, 'DECREASE', 0.82, 'Competitor seasonal promotions have lowered category average. Modest price discount will accelerate inventory turnover before season end.', 'PENDING', 'MANUAL', CURRENT_TIMESTAMP);

-- Reorder Suggestions
MERGE INTO reorder_suggestions (id, product_id, current_stock, recommended_quantity, suggested_lead_time_days, confidence, reasoning, status, trigger_reason, created_at) KEY(id)
VALUES ('rsug-1', 'prod-1', 15, 50, 7, 0.94, 'Stock level (15) is below reorder threshold (25) with consistent sales velocity of 8 units/day. Estimated stockout in 2 days without replenishment.', 'PENDING', 'INVENTORY_LOW', CURRENT_TIMESTAMP);

MERGE INTO reorder_suggestions (id, product_id, current_stock, recommended_quantity, suggested_lead_time_days, confidence, reasoning, status, trigger_reason, created_at) KEY(id)
VALUES ('rsug-2', 'prod-4', 0, 100, 10, 0.98, 'Item is currently OUT_OF_STOCK with high historical demand velocity (12 units/day). Urgent replenishment recommended.', 'PENDING', 'INVENTORY_LOW', CURRENT_TIMESTAMP);

MERGE INTO reorder_suggestions (id, product_id, current_stock, recommended_quantity, suggested_lead_time_days, confidence, reasoning, status, trigger_reason, created_at) KEY(id)
VALUES ('rsug-3', 'prod-8', 5, 40, 5, 0.91, 'Inventory (5) is critically below reorder threshold (15). Lead time is 5 days, stock will deplete in less than 24 hours.', 'PENDING', 'INVENTORY_LOW', CURRENT_TIMESTAMP);
