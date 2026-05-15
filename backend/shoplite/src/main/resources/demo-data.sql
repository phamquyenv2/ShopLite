-- ShopLite demo dataset for MySQL.
-- Run after the Spring Boot app has started once, so DatabaseInitializer has
-- already created roles, permissions, menus, and the default login 1 / 1.

START TRANSACTION;

SET @admin_user_id = (SELECT id FROM users WHERE username = '1' LIMIT 1);
SET @admin_password = (SELECT password FROM users WHERE id = @admin_user_id LIMIT 1);
SET @store_id = (
    SELECT sm.store_id
    FROM store_members sm
    WHERE sm.user_id = @admin_user_id AND sm.status = 'ACTIVE'
    ORDER BY sm.joined_at DESC
    LIMIT 1
);

-- Safety guard: this script expects the built-in admin account to exist.
-- If these variables are NULL, start the backend once first.

UPDATE stores
SET name = 'ShopLite Demo Store'
WHERE id = @store_id;

INSERT INTO offices (store_id, name, office_lat, office_lng, radius)
SELECT @store_id, 'Chi nhanh trung tam', 10.77620900, 106.70076200, 250
WHERE NOT EXISTS (
    SELECT 1 FROM offices WHERE store_id = @store_id AND name = 'Chi nhanh trung tam'
);

SET @office_id = (SELECT id FROM offices WHERE store_id = @store_id ORDER BY id LIMIT 1);

-- Master data: categories, units, customers, suppliers.
INSERT INTO categories (store_id, name)
SELECT @store_id, 'Do uong'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE store_id = @store_id AND name = 'Do uong');

INSERT INTO categories (store_id, name)
SELECT @store_id, 'Do an nhanh'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE store_id = @store_id AND name = 'Do an nhanh');

INSERT INTO categories (store_id, name)
SELECT @store_id, 'Gia dung'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE store_id = @store_id AND name = 'Gia dung');

INSERT INTO units (store_id, name, description)
SELECT @store_id, 'Chai', 'Don vi cho do uong dong chai'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE store_id = @store_id AND name = 'Chai');

INSERT INTO units (store_id, name, description)
SELECT @store_id, 'Goi', 'Don vi cho hang dong goi'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE store_id = @store_id AND name = 'Goi');

INSERT INTO units (store_id, name, description)
SELECT @store_id, 'Cai', 'Don vi dem san pham'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE store_id = @store_id AND name = 'Cai');

INSERT INTO suppliers (store_id, name, phone, address, email, version, created_at)
SELECT @store_id, 'Nha phan phoi An Phat', '0909000001', 'Quan 1, TP.HCM', 'sales@anphat.demo', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE store_id = @store_id AND name = 'Nha phan phoi An Phat');

INSERT INTO suppliers (store_id, name, phone, address, email, version, created_at)
SELECT @store_id, 'Cong ty Thuc Pham Xanh', '0909000002', 'Thu Duc, TP.HCM', 'hello@thucphamxanh.demo', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE store_id = @store_id AND name = 'Cong ty Thuc Pham Xanh');

INSERT INTO customers (store_id, name, phone, points, version)
SELECT @store_id, 'Khach le', '0000000000', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE store_id = @store_id AND phone = '0000000000');

INSERT INTO customers (store_id, name, phone, points, version)
SELECT @store_id, 'Nguyen Minh Chau', '0901111222', 125, 0
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE store_id = @store_id AND phone = '0901111222');

INSERT INTO customers (store_id, name, phone, points, version)
SELECT @store_id, 'Tran Bao Anh', '0903333444', 40, 0
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE store_id = @store_id AND phone = '0903333444');

SET @cat_drink = (SELECT id FROM categories WHERE store_id = @store_id AND name = 'Do uong' LIMIT 1);
SET @cat_food = (SELECT id FROM categories WHERE store_id = @store_id AND name = 'Do an nhanh' LIMIT 1);
SET @cat_home = (SELECT id FROM categories WHERE store_id = @store_id AND name = 'Gia dung' LIMIT 1);
SET @unit_bottle = (SELECT id FROM units WHERE store_id = @store_id AND name = 'Chai' LIMIT 1);
SET @unit_pack = (SELECT id FROM units WHERE store_id = @store_id AND name = 'Goi' LIMIT 1);
SET @unit_piece = (SELECT id FROM units WHERE store_id = @store_id AND name = 'Cai' LIMIT 1);

-- Products and stock.
INSERT INTO products (store_id, category_id, unit_id, name, sku, barcode, stock, cost_price, selling_price, min_stock, max_stock, status, version, image, is_deleted, created_at, updated_at)
SELECT @store_id, @cat_drink, @unit_bottle, 'Nuoc suoi Lavie 500ml', 'DEMO-LAVIE-500', '893DEMOLAVIE500', 120, 3500, 6000, 20, 300, 'ACTIVE', 0, NULL, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = @store_id AND sku = 'DEMO-LAVIE-500');

INSERT INTO products (store_id, category_id, unit_id, name, sku, barcode, stock, cost_price, selling_price, min_stock, max_stock, status, version, image, is_deleted, created_at, updated_at)
SELECT @store_id, @cat_drink, @unit_bottle, 'Tra xanh khong do 455ml', 'DEMO-TEA-455', '893DEMOTEA455', 80, 6500, 10000, 15, 200, 'ACTIVE', 0, NULL, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = @store_id AND sku = 'DEMO-TEA-455');

INSERT INTO products (store_id, category_id, unit_id, name, sku, barcode, stock, cost_price, selling_price, min_stock, max_stock, status, version, image, is_deleted, created_at, updated_at)
SELECT @store_id, @cat_food, @unit_pack, 'Mi hao hao tom chua cay', 'DEMO-NOODLE-001', '893DEMONOODLE001', 200, 3200, 5500, 30, 500, 'ACTIVE', 0, NULL, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = @store_id AND sku = 'DEMO-NOODLE-001');

INSERT INTO products (store_id, category_id, unit_id, name, sku, barcode, stock, cost_price, selling_price, min_stock, max_stock, status, version, image, is_deleted, created_at, updated_at)
SELECT @store_id, @cat_home, @unit_piece, 'Khan giay hop 180 to', 'DEMO-TISSUE-180', '893DEMOTISSUE180', 45, 12000, 19000, 10, 100, 'ACTIVE', 0, NULL, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = @store_id AND sku = 'DEMO-TISSUE-180');

SET @p_lavie = (SELECT id FROM products WHERE store_id = @store_id AND sku = 'DEMO-LAVIE-500' LIMIT 1);
SET @p_tea = (SELECT id FROM products WHERE store_id = @store_id AND sku = 'DEMO-TEA-455' LIMIT 1);
SET @p_noodle = (SELECT id FROM products WHERE store_id = @store_id AND sku = 'DEMO-NOODLE-001' LIMIT 1);
SET @p_tissue = (SELECT id FROM products WHERE store_id = @store_id AND sku = 'DEMO-TISSUE-180' LIMIT 1);

-- Demo staff accounts. Password is copied from admin account, so use the same password as user 1.
INSERT INTO users (username, phone, password, is_active)
SELECT 'Demo Thu Ngan', '0910000001', @admin_password, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE phone = '0910000001');

INSERT INTO users (username, phone, password, is_active)
SELECT 'Demo Nhan Vien Kho', '0910000002', @admin_password, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE phone = '0910000002');

SET @cashier_user_id = (SELECT id FROM users WHERE phone = '0910000001' LIMIT 1);
SET @warehouse_user_id = (SELECT id FROM users WHERE phone = '0910000002' LIMIT 1);
SET @cashier_role_id = (SELECT id FROM roles WHERE name = 'CASHIER' LIMIT 1);
SET @warehouse_role_id = (SELECT id FROM roles WHERE name = 'WAREHOUSE' LIMIT 1);

INSERT INTO store_members (store_id, user_id, role_id, status, joined_at)
SELECT @store_id, @cashier_user_id, @cashier_role_id, 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM store_members WHERE store_id = @store_id AND user_id = @cashier_user_id);

INSERT INTO store_members (store_id, user_id, role_id, status, joined_at)
SELECT @store_id, @warehouse_user_id, @warehouse_role_id, 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM store_members WHERE store_id = @store_id AND user_id = @warehouse_user_id);

SET @cashier_member_id = (SELECT id FROM store_members WHERE store_id = @store_id AND user_id = @cashier_user_id LIMIT 1);
SET @warehouse_member_id = (SELECT id FROM store_members WHERE store_id = @store_id AND user_id = @warehouse_user_id LIMIT 1);

INSERT INTO employees (store_id, store_member_id, office_id, salary_rate, qr, deleted, note)
SELECT @store_id, @cashier_member_id, @office_id, 35000, 'DEMO-CASHIER-QR', 0, 'Nhan vien demo: thu ngan'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE store_member_id = @cashier_member_id);

INSERT INTO employees (store_id, store_member_id, office_id, salary_rate, qr, deleted, note)
SELECT @store_id, @warehouse_member_id, @office_id, 40000, 'DEMO-WAREHOUSE-QR', 0, 'Nhan vien demo: kho'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE store_member_id = @warehouse_member_id);

SET @cashier_employee_id = (SELECT id FROM employees WHERE store_member_id = @cashier_member_id LIMIT 1);
SET @warehouse_employee_id = (SELECT id FROM employees WHERE store_member_id = @warehouse_member_id LIMIT 1);

-- Fund accounts.
INSERT INTO fund_accounts (store_id, name, type, balance, opening_balance, is_active, created_at, updated_at)
SELECT @store_id, 'Demo tien mat', 'CASH', 2000000.00, 2000000.00, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM fund_accounts WHERE store_id = @store_id AND name = 'Demo tien mat');

INSERT INTO fund_accounts (store_id, name, type, balance, opening_balance, is_active, created_at, updated_at)
SELECT @store_id, 'Demo ngan hang', 'BANK', 10000000.00, 10000000.00, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM fund_accounts WHERE store_id = @store_id AND name = 'Demo ngan hang');

SET @cash_fund_id = (SELECT id FROM fund_accounts WHERE store_id = @store_id AND name = 'Demo tien mat' LIMIT 1);
SET @bank_fund_id = (SELECT id FROM fund_accounts WHERE store_id = @store_id AND name = 'Demo ngan hang' LIMIT 1);

-- Sales orders: completed, pending payment, and draft.
SET @customer_chau = (SELECT id FROM customers WHERE store_id = @store_id AND phone = '0901111222' LIMIT 1);
SET @customer_anh = (SELECT id FROM customers WHERE store_id = @store_id AND phone = '0903333444' LIMIT 1);

INSERT INTO orders (store_id, request_id, user_id, customer_id, code, total_amount, discount, status, created_at, paid_at, confirmed_at)
SELECT @store_id, 'DEMO-REQ-ORDER-001', @admin_user_id, @customer_chau, 'DEMO-ORDER-001', 36500, 0, 'COMPLETED', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE store_id = @store_id AND code = 'DEMO-ORDER-001');

INSERT INTO orders (store_id, request_id, user_id, customer_id, code, total_amount, discount, status, created_at, paid_at, confirmed_at)
SELECT @store_id, 'DEMO-REQ-ORDER-002', @admin_user_id, @customer_anh, 'DEMO-ORDER-002', 29000, 1000, 'PENDING_PAYMENT', NOW() - INTERVAL 1 HOUR, NULL, NOW() - INTERVAL 1 HOUR
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE store_id = @store_id AND code = 'DEMO-ORDER-002');

INSERT INTO orders (store_id, request_id, user_id, customer_id, code, total_amount, discount, status, created_at, paid_at, confirmed_at)
SELECT @store_id, 'DEMO-REQ-ORDER-003', @admin_user_id, NULL, 'DEMO-ORDER-003', 24500, 0, 'DRAFT', NOW() - INTERVAL 20 MINUTE, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE store_id = @store_id AND code = 'DEMO-ORDER-003');

SET @order_1 = (SELECT id FROM orders WHERE store_id = @store_id AND code = 'DEMO-ORDER-001' LIMIT 1);
SET @order_2 = (SELECT id FROM orders WHERE store_id = @store_id AND code = 'DEMO-ORDER-002' LIMIT 1);

INSERT INTO order_items (order_id, product_id, product_name, quantity, price, total_price)
SELECT @order_1, @p_lavie, 'Nuoc suoi Lavie 500ml', 2, 6000, 12000
WHERE NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = @order_1 AND product_id = @p_lavie);

INSERT INTO order_items (order_id, product_id, product_name, quantity, price, total_price)
SELECT @order_1, @p_noodle, 'Mi hao hao tom chua cay', 3, 5500, 16500
WHERE NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = @order_1 AND product_id = @p_noodle);

INSERT INTO order_items (order_id, product_id, product_name, quantity, price, total_price)
SELECT @order_1, @p_tea, 'Tra xanh khong do 455ml', 1, 10000, 10000
WHERE NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = @order_1 AND product_id = @p_tea);

INSERT INTO order_items (order_id, product_id, product_name, quantity, price, total_price)
SELECT @order_2, @p_tissue, 'Khan giay hop 180 to', 1, 19000, 19000
WHERE NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = @order_2 AND product_id = @p_tissue);

INSERT INTO order_items (order_id, product_id, product_name, quantity, price, total_price)
SELECT @order_2, @p_tea, 'Tra xanh khong do 455ml', 1, 10000, 10000
WHERE NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = @order_2 AND product_id = @p_tea);

INSERT INTO payments (store_id, reference_type, reference_id, payment_method, amount, transfer_content, provider, provider_transaction_id, status, paid_at, expires_at, created_by, notification_sent, created_at, updated_at)
SELECT @store_id, 'ORDER', @order_1, 'CASH', 36500.00, 'Thanh toan demo order 001', 'DEMO', 'DEMO-PAY-ORDER-001', 'COMPLETED', NOW() - INTERVAL 2 HOUR, NOW() + INTERVAL 1 DAY, '1', 0, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE provider_transaction_id = 'DEMO-PAY-ORDER-001');

SET @pay_order_1 = (SELECT id FROM payments WHERE provider_transaction_id = 'DEMO-PAY-ORDER-001' LIMIT 1);

INSERT INTO transactions (store_id, type, direction, amount, content, payment_id, fund_account_id, balance_before, balance_after, transaction_code, transaction_time, created_at)
SELECT @store_id, 'REVENUE', 'IN', 36500.00, 'Thu tien ban hang DEMO-ORDER-001', @pay_order_1, @cash_fund_id, 2000000.00, 2036500.00, 'DEMO-TXN-ORDER-001', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE transaction_code = 'DEMO-TXN-ORDER-001');

-- Import order and inventory logs.
SET @supplier_anphat = (SELECT id FROM suppliers WHERE store_id = @store_id AND name = 'Nha phan phoi An Phat' LIMIT 1);

INSERT INTO import_orders (store_id, supplier_id, tax, discount, total_amount, amount_paid, status, return_status, note, created_at)
SELECT @store_id, @supplier_anphat, 0, 0, 770000, 770000, 'COMPLETED', 'UNRETURNED', 'DEMO-IMPORT-001: nhap hang dau ky', NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM import_orders WHERE store_id = @store_id AND note LIKE 'DEMO-IMPORT-001:%');

SET @import_1 = (SELECT id FROM import_orders WHERE store_id = @store_id AND note LIKE 'DEMO-IMPORT-001:%' LIMIT 1);

INSERT INTO import_items (import_id, product_id, quantity, import_price, sub_total, returned_quantity)
SELECT @import_1, @p_lavie, 100, 3500, 350000, 0
WHERE NOT EXISTS (SELECT 1 FROM import_items WHERE import_id = @import_1 AND product_id = @p_lavie);

INSERT INTO import_items (import_id, product_id, quantity, import_price, sub_total, returned_quantity)
SELECT @import_1, @p_noodle, 100, 3200, 320000, 0
WHERE NOT EXISTS (SELECT 1 FROM import_items WHERE import_id = @import_1 AND product_id = @p_noodle);

SET @import_item_lavie = (SELECT id FROM import_items WHERE import_id = @import_1 AND product_id = @p_lavie LIMIT 1);
SET @import_item_noodle = (SELECT id FROM import_items WHERE import_id = @import_1 AND product_id = @p_noodle LIMIT 1);

INSERT INTO payments (store_id, reference_type, reference_id, payment_method, amount, transfer_content, provider, provider_transaction_id, status, paid_at, expires_at, created_by, notification_sent, created_at, updated_at)
SELECT @store_id, 'IMPORT_ORDER', @import_1, 'BANK_TRANSFER', 770000.00, 'Thanh toan nhap hang DEMO-IMPORT-001', 'DEMO', 'DEMO-PAY-IMPORT-001', 'COMPLETED', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, '1', 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE provider_transaction_id = 'DEMO-PAY-IMPORT-001');

SET @pay_import_1 = (SELECT id FROM payments WHERE provider_transaction_id = 'DEMO-PAY-IMPORT-001' LIMIT 1);

INSERT INTO transactions (store_id, type, direction, amount, content, payment_id, fund_account_id, balance_before, balance_after, transaction_code, transaction_time, created_at)
SELECT @store_id, 'EXPENSE', 'OUT', 770000.00, 'Chi tien nhap hang DEMO-IMPORT-001', @pay_import_1, @bank_fund_id, 10000000.00, 9230000.00, 'DEMO-TXN-IMPORT-001', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE transaction_code = 'DEMO-TXN-IMPORT-001');

INSERT INTO inventory_logs (store_id, product_id, import_item_id, quantity_in, quantity_out, balance_after, current_stock, type, created_at)
SELECT @store_id, @p_lavie, @import_item_lavie, 100, 0, 120, 120, 'IMPORT', NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM inventory_logs WHERE store_id = @store_id AND import_item_id = @import_item_lavie);

INSERT INTO inventory_logs (store_id, product_id, import_item_id, quantity_in, quantity_out, balance_after, current_stock, type, created_at)
SELECT @store_id, @p_noodle, @import_item_noodle, 100, 0, 200, 200, 'IMPORT', NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM inventory_logs WHERE store_id = @store_id AND import_item_id = @import_item_noodle);

-- Inventory adjustment demo.
INSERT INTO inventory_adjustments (store_id, reason, note, created_by, created_at)
SELECT @store_id, 'Kiem kho demo', 'DEMO-ADJUST-001: dieu chinh lech kho cuoi ngay', '1', NOW() - INTERVAL 3 HOUR
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustments WHERE store_id = @store_id AND note LIKE 'DEMO-ADJUST-001:%');

SET @adjust_1 = (SELECT id FROM inventory_adjustments WHERE store_id = @store_id AND note LIKE 'DEMO-ADJUST-001:%' LIMIT 1);

INSERT INTO inventory_logs (store_id, product_id, adjustment_id, quantity_in, quantity_out, balance_after, current_stock, type, created_at)
SELECT @store_id, @p_tissue, @adjust_1, 5, 0, 45, 45, 'ADJUST', NOW() - INTERVAL 3 HOUR
WHERE NOT EXISTS (SELECT 1 FROM inventory_logs WHERE store_id = @store_id AND adjustment_id = @adjust_1 AND product_id = @p_tissue);

-- Workforce: roster, attendance, payroll.
INSERT INTO rosters (employee_id, working_day, start_time, end_time, check_in_allowed_from, check_in_allowed_to, check_out_allowed_from, check_out_allowed_to, expected_hours, type, unpaid_break_minutes, note)
SELECT @cashier_employee_id, CURDATE(), '08:00:00', '17:00:00', '07:30:00', '08:30:00', '16:30:00', '17:30:00', 8.0, 'WORKING', 60, 'Ca demo hom nay'
WHERE NOT EXISTS (SELECT 1 FROM rosters WHERE employee_id = @cashier_employee_id AND working_day = CURDATE() AND type = 'WORKING');

INSERT INTO rosters (employee_id, working_day, start_time, end_time, check_in_allowed_from, check_in_allowed_to, check_out_allowed_from, check_out_allowed_to, expected_hours, type, unpaid_break_minutes, note)
SELECT @warehouse_employee_id, CURDATE(), '09:00:00', '18:00:00', '08:30:00', '09:30:00', '17:30:00', '18:30:00', 8.0, 'WORKING', 60, 'Ca kho demo hom nay'
WHERE NOT EXISTS (SELECT 1 FROM rosters WHERE employee_id = @warehouse_employee_id AND working_day = CURDATE() AND type = 'WORKING');

SET @cashier_roster_today = (SELECT id FROM rosters WHERE employee_id = @cashier_employee_id AND working_day = CURDATE() AND type = 'WORKING' LIMIT 1);

INSERT INTO attendances (office_id, employee_id, roster_id, check_in, check_out, worked_minutes, payable_minutes, working_day, walk_in, latitude, longitude, distance, checkout_latitude, checkout_longitude, checkout_distance, late_minutes, early_leave_minutes, closed_automatically, status, created_at, updated_at)
SELECT @office_id, @cashier_employee_id, @cashier_roster_today, CONCAT(CURDATE(), ' 08:05:00'), CONCAT(CURDATE(), ' 17:02:00'), 537, 477, CURDATE(), 0, 10.77620900, 106.70076200, 12.5, 10.77620900, 106.70076200, 10.0, 0, 0, 0, 'VALID', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM attendances WHERE employee_id = @cashier_employee_id AND working_day = CURDATE());

INSERT INTO payrolls (employee_id, period, salary_rate, total_hours, bonus, penalty, total_salary)
SELECT @cashier_employee_id, DATE_FORMAT(CURDATE(), '%Y-%m-01'), 35000, 176, 200000, 0, 6360000
WHERE NOT EXISTS (SELECT 1 FROM payrolls WHERE employee_id = @cashier_employee_id AND period = DATE_FORMAT(CURDATE(), '%Y-%m-01'));

INSERT INTO payrolls (employee_id, period, salary_rate, total_hours, bonus, penalty, total_salary)
SELECT @warehouse_employee_id, DATE_FORMAT(CURDATE(), '%Y-%m-01'), 40000, 168, 0, 100000, 6620000
WHERE NOT EXISTS (SELECT 1 FROM payrolls WHERE employee_id = @warehouse_employee_id AND period = DATE_FORMAT(CURDATE(), '%Y-%m-01'));

-- Keep fund balances aligned with demo transactions.
UPDATE fund_accounts
SET balance = 2036500.00, updated_at = NOW()
WHERE id = @cash_fund_id;

UPDATE fund_accounts
SET balance = 9230000.00, updated_at = NOW()
WHERE id = @bank_fund_id;

COMMIT;

SELECT
    'ShopLite demo data ready' AS message,
    @store_id AS store_id,
    (SELECT COUNT(*) FROM products WHERE store_id = @store_id AND is_deleted = 0) AS products,
    (SELECT COUNT(*) FROM orders WHERE store_id = @store_id) AS orders,
    (SELECT COUNT(*) FROM employees e JOIN store_members sm ON sm.id = e.store_member_id WHERE sm.store_id = @store_id AND e.deleted = 0) AS employees;
