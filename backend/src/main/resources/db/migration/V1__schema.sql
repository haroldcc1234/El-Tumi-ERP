CREATE TABLE partners (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 kind VARCHAR(255),
 name VARCHAR(255),
 document VARCHAR(255),
 phone VARCHAR(255),
 email VARCHAR(255),
 address VARCHAR(255),
 bank VARCHAR(255),
 bank_account VARCHAR(255),
 contact VARCHAR(255),
 status VARCHAR(255)
);

CREATE TABLE warehouses (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name VARCHAR(255),
 location VARCHAR(255),
 category VARCHAR(255),
 manager VARCHAR(255),
 status VARCHAR(255)
);

CREATE TABLE products (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 category VARCHAR(255),
 unit VARCHAR(255),
 tax_type VARCHAR(255),
 tax_rate DECIMAL(18,2),
 average_cost DECIMAL(18,2),
 barcode VARCHAR(255),
 image_id VARCHAR(80),
 status VARCHAR(255)
);

CREATE TABLE stocks (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 product_id BIGINT,
 warehouse_id BIGINT,
 quantity DECIMAL(18,4)
);

CREATE TABLE stock_moves (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 product_id BIGINT,
 warehouse_id BIGINT,
 kind VARCHAR(255),
 quantity DECIMAL(18,4),
 unit_cost DECIMAL(18,2),
 date DATE,
 reference VARCHAR(255),
 notes TEXT,
 balance DECIMAL(18,4)
);

CREATE TABLE stock_rules (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 product_id BIGINT,
 warehouse_id BIGINT,
 supplier_id BIGINT,
 minimum DECIMAL(18,4),
 maximum DECIMAL(18,4)
);

CREATE TABLE purchases (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 partner_id BIGINT,
 warehouse_id BIGINT,
 date DATE,
 due_date DATE,
 budget_id BIGINT,
 status VARCHAR(255),
 total DECIMAL(18,2),
 notes TEXT
);

CREATE TABLE receipts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 purchase_id BIGINT,
 date DATE,
 total DECIMAL(18,2),
 file_id VARCHAR(80),
 reference VARCHAR(255)
);

CREATE TABLE accounts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 type VARCHAR(255),
 nature VARCHAR(255),
 parent_id BIGINT,
 postable VARCHAR(255),
 status VARCHAR(255)
);

CREATE TABLE journals (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 date DATE,
 description VARCHAR(255),
 reference VARCHAR(255),
 status VARCHAR(255),
 total DECIMAL(18,2)
);

CREATE TABLE periods (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 period VARCHAR(255),
 notes TEXT
);

CREATE TABLE receivables (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 party VARCHAR(255),
 document VARCHAR(255),
 date DATE,
 due_date DATE,
 total DECIMAL(18,2),
 paid DECIMAL(18,2),
 credit DECIMAL(18,2),
 status VARCHAR(255),
 source VARCHAR(255),
 notes TEXT,
 source_id BIGINT
);

CREATE TABLE payables (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 party VARCHAR(255),
 document VARCHAR(255),
 date DATE,
 due_date DATE,
 total DECIMAL(18,2),
 paid DECIMAL(18,2),
 credit DECIMAL(18,2),
 status VARCHAR(255),
 source VARCHAR(255),
 notes TEXT,
 source_id BIGINT
);

CREATE TABLE cash_accounts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name VARCHAR(255),
 kind VARCHAR(255),
 balance DECIMAL(18,2),
 status VARCHAR(255)
);

CREATE TABLE treasury (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 cash_account_id BIGINT,
 kind VARCHAR(255),
 amount DECIMAL(18,2),
 date DATE,
 description VARCHAR(255),
 reference VARCHAR(255),
 file_id VARCHAR(80)
);

CREATE TABLE budgets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 area VARCHAR(255),
 category VARCHAR(255),
 start_date DATE,
 end_date DATE,
 amount DECIMAL(18,2),
 executed DECIMAL(18,2),
 manager VARCHAR(255)
);

CREATE TABLE assets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 category VARCHAR(255),
 date DATE,
 amount DECIMAL(18,2),
 useful_months DECIMAL(18,4),
 residual DECIMAL(18,2),
 depreciated DECIMAL(18,2),
 manager VARCHAR(255),
 warranty_years DECIMAL(18,4),
 partner_id BIGINT,
 budget_id BIGINT,
 file_id VARCHAR(80),
 status VARCHAR(255)
);

CREATE TABLE services (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 partner_id BIGINT,
 date DATE,
 due_date DATE,
 amount DECIMAL(18,2),
 budget_id BIGINT,
 next_date DATE,
 file_id VARCHAR(80),
 notes TEXT
);

CREATE TABLE obligations (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 party VARCHAR(255),
 date DATE,
 due_date DATE,
 amount DECIMAL(18,2),
 budget_id BIGINT,
 file_id VARCHAR(80),
 notes TEXT
);

CREATE TABLE employees (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 last_name VARCHAR(255),
 document VARCHAR(255),
 birth_date DATE,
 address VARCHAR(255),
 phone VARCHAR(255),
 marital_status VARCHAR(255),
 insurance VARCHAR(255),
 file_id VARCHAR(80),
 status VARCHAR(255)
);

CREATE TABLE dependents (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 kind VARCHAR(255),
 name VARCHAR(255),
 document VARCHAR(255),
 birth_date DATE,
 address VARCHAR(255),
 phone VARCHAR(255),
 file_id VARCHAR(80)
);

CREATE TABLE contracts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 start_date DATE,
 end_date DATE,
 position VARCHAR(255),
 regime VARCHAR(255),
 salary DECIMAL(18,2),
 vacation_days DECIMAL(18,4),
 bonus_factor DECIMAL(18,2),
 cts_factor DECIMAL(18,2),
 pension_rate DECIMAL(18,2),
 health_rate DECIMAL(18,2),
 status VARCHAR(255)
);

CREATE TABLE attendance (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 date DATE,
 check_in TIMESTAMP,
 check_out TIMESTAMP,
 late_minutes DECIMAL(18,4),
 extra_hours DECIMAL(18,4),
 status VARCHAR(255),
 notes TEXT
);

CREATE TABLE leaves (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 kind VARCHAR(255),
 start_date DATE,
 end_date DATE,
 days DECIMAL(18,4),
 compensable_hours DECIMAL(18,4),
 file_id VARCHAR(80),
 notes TEXT,
 status VARCHAR(255)
);

CREATE TABLE payrolls (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 period VARCHAR(255),
 salary DECIMAL(18,2),
 overtime DECIMAL(18,2),
 family_allowance DECIMAL(18,2),
 other_bonus DECIMAL(18,2),
 lateness DECIMAL(18,2),
 income_tax DECIMAL(18,2),
 pension DECIMAL(18,2),
 advances DECIMAL(18,2),
 loans DECIMAL(18,2),
 net DECIMAL(18,2),
 employer_health DECIMAL(18,2),
 status VARCHAR(255)
);

CREATE TABLE fees (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 party VARCHAR(255),
 document VARCHAR(255),
 start_date DATE,
 end_date DATE,
 amount DECIMAL(18,2),
 waiver VARCHAR(255),
 withheld DECIMAL(18,2),
 net DECIMAL(18,2),
 file_id VARCHAR(80),
 status VARCHAR(255),
 notes TEXT
);

CREATE TABLE benefits (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 period VARCHAR(255),
 kind VARCHAR(255),
 months DECIMAL(18,4),
 days DECIMAL(18,4),
 amount DECIMAL(18,2),
 status VARCHAR(255),
 notes TEXT
);

CREATE TABLE loans (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 date DATE,
 amount DECIMAL(18,2),
 months DECIMAL(18,4),
 annual_rate DECIMAL(18,2),
 installment DECIMAL(18,2),
 balance DECIMAL(18,2),
 first_period VARCHAR(255),
 status VARCHAR(255),
 file_id VARCHAR(80),
 notes TEXT
);

CREATE TABLE advances (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 date DATE,
 period VARCHAR(255),
 amount DECIMAL(18,2),
 status VARCHAR(255),
 file_id VARCHAR(80),
 notes TEXT
);

CREATE TABLE settlements (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 employee_id BIGINT,
 date DATE,
 reason VARCHAR(255),
 vacation_days DECIMAL(18,4),
 cts_months DECIMAL(18,4),
 bonus_months DECIMAL(18,4),
 other DECIMAL(18,2),
 amount DECIMAL(18,2),
 status VARCHAR(255),
 file_id VARCHAR(80),
 notes TEXT
);

CREATE TABLE areas (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name VARCHAR(255),
 max_tables DECIMAL(18,4)
);

CREATE TABLE dining_tables (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name VARCHAR(255),
 area_id BIGINT,
 capacity DECIMAL(18,4),
 status VARCHAR(255)
);

CREATE TABLE dishes (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 name VARCHAR(255),
 category VARCHAR(255),
 price DECIMAL(18,2),
 tax_type VARCHAR(255),
 tax_rate DECIMAL(18,2),
 description TEXT,
 image_id VARCHAR(80),
 status VARCHAR(255)
);

CREATE TABLE discounts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 code VARCHAR(255),
 percent DECIMAL(18,2),
 dish_id BIGINT,
 start_date DATE,
 end_date DATE,
 status VARCHAR(255)
);

CREATE TABLE quotes (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 customer VARCHAR(255),
 document VARCHAR(255),
 date DATE,
 rental DECIMAL(18,2),
 discount DECIMAL(18,2),
 total DECIMAL(18,2),
 advance DECIMAL(18,2),
 status VARCHAR(255),
 notes TEXT,
 order_id BIGINT
);

CREATE TABLE orders (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 table_id BIGINT,
 customer VARCHAR(255),
 quote_id BIGINT,
 warehouse_id BIGINT,
 date DATE,
 status VARCHAR(255),
 total DECIMAL(18,2),
 discount DECIMAL(18,2),
 rental DECIMAL(18,2),
 discount_code VARCHAR(255),
 notes TEXT
);

CREATE TABLE invoices (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 number VARCHAR(255),
 order_id BIGINT,
 customer VARCHAR(255),
 document VARCHAR(255),
 document_type VARCHAR(255),
 payment_type VARCHAR(255),
 date DATE,
 subtotal DECIMAL(18,2),
 tax DECIMAL(18,2),
 total DECIMAL(18,2),
 status VARCHAR(255),
 cancel_reason VARCHAR(255),
 cash_account_id BIGINT,
 paid_now DECIMAL(18,2) DEFAULT 0,
 series VARCHAR(10),
 correlative BIGINT,
 address VARCHAR(255),
 company_snapshot TEXT,
 lines_snapshot TEXT
);

CREATE TABLE cash_sessions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 cash_account_id BIGINT,
 opening DECIMAL(18,2),
 expected DECIMAL(18,2),
 counted DECIMAL(18,2),
 difference DECIMAL(18,2),
 status VARCHAR(255),
 opened_at VARCHAR(255),
 closed_at VARCHAR(255),
 opened_by BIGINT,
 closed_by BIGINT,
 opening_movement_id BIGINT
);

CREATE TABLE users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name VARCHAR(255),
 email VARCHAR(255),
 role VARCHAR(255),
 status VARCHAR(255),
 phone VARCHAR(255),
 password_hash VARCHAR(255) NOT NULL,
 failed_attempts INT NOT NULL DEFAULT 0,
 locked_until TIMESTAMP NULL,
 totp_secret VARCHAR(255),
 totp_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE company (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name VARCHAR(255),
 trade_name VARCHAR(255),
 ruc VARCHAR(255),
 address VARCHAR(255),
 ubigeo VARCHAR(255),
 department VARCHAR(255),
 province VARCHAR(255),
 district VARCHAR(255),
 phone VARCHAR(255),
 logo_id VARCHAR(80),
 id_threshold DECIMAL(18,2),
 digital_threshold DECIMAL(18,2)
);

CREATE TABLE audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 username VARCHAR(255),
 action VARCHAR(255),
 entity VARCHAR(255),
 entity_id DECIMAL(18,4),
 detail TEXT
);

ALTER TABLE stocks ADD CONSTRAINT fk_stocks_product_id FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE stocks ADD CONSTRAINT fk_stocks_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE stock_moves ADD CONSTRAINT fk_stock_moves_product_id FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE stock_moves ADD CONSTRAINT fk_stock_moves_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE stock_rules ADD CONSTRAINT fk_stock_rules_product_id FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE stock_rules ADD CONSTRAINT fk_stock_rules_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE stock_rules ADD CONSTRAINT fk_stock_rules_supplier_id FOREIGN KEY (supplier_id) REFERENCES partners(id);

ALTER TABLE purchases ADD CONSTRAINT fk_purchases_partner_id FOREIGN KEY (partner_id) REFERENCES partners(id);

ALTER TABLE purchases ADD CONSTRAINT fk_purchases_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE purchases ADD CONSTRAINT fk_purchases_budget_id FOREIGN KEY (budget_id) REFERENCES budgets(id);

ALTER TABLE receipts ADD CONSTRAINT fk_receipts_purchase_id FOREIGN KEY (purchase_id) REFERENCES purchases(id);

ALTER TABLE treasury ADD CONSTRAINT fk_treasury_cash_account_id FOREIGN KEY (cash_account_id) REFERENCES cash_accounts(id);

ALTER TABLE assets ADD CONSTRAINT fk_assets_partner_id FOREIGN KEY (partner_id) REFERENCES partners(id);

ALTER TABLE assets ADD CONSTRAINT fk_assets_budget_id FOREIGN KEY (budget_id) REFERENCES budgets(id);

ALTER TABLE services ADD CONSTRAINT fk_services_partner_id FOREIGN KEY (partner_id) REFERENCES partners(id);

ALTER TABLE services ADD CONSTRAINT fk_services_budget_id FOREIGN KEY (budget_id) REFERENCES budgets(id);

ALTER TABLE obligations ADD CONSTRAINT fk_obligations_budget_id FOREIGN KEY (budget_id) REFERENCES budgets(id);

ALTER TABLE dependents ADD CONSTRAINT fk_dependents_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE attendance ADD CONSTRAINT fk_attendance_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE leaves ADD CONSTRAINT fk_leaves_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE payrolls ADD CONSTRAINT fk_payrolls_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE benefits ADD CONSTRAINT fk_benefits_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE loans ADD CONSTRAINT fk_loans_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE advances ADD CONSTRAINT fk_advances_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE settlements ADD CONSTRAINT fk_settlements_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE dining_tables ADD CONSTRAINT fk_dining_tables_area_id FOREIGN KEY (area_id) REFERENCES areas(id);

ALTER TABLE discounts ADD CONSTRAINT fk_discounts_dish_id FOREIGN KEY (dish_id) REFERENCES dishes(id);

ALTER TABLE orders ADD CONSTRAINT fk_orders_table_id FOREIGN KEY (table_id) REFERENCES dining_tables(id);

ALTER TABLE orders ADD CONSTRAINT fk_orders_quote_id FOREIGN KEY (quote_id) REFERENCES quotes(id);

ALTER TABLE orders ADD CONSTRAINT fk_orders_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE invoices ADD CONSTRAINT fk_invoices_order_id FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE cash_sessions ADD CONSTRAINT fk_cash_sessions_cash_account_id FOREIGN KEY (cash_account_id) REFERENCES cash_accounts(id);

CREATE UNIQUE INDEX uq_products_code ON products (code);

CREATE UNIQUE INDEX uq_accounts_code ON accounts (code);

CREATE UNIQUE INDEX uq_purchases_code ON purchases (code);

CREATE UNIQUE INDEX uq_budgets_code ON budgets (code);

CREATE UNIQUE INDEX uq_assets_code ON assets (code);

CREATE UNIQUE INDEX uq_services_code ON services (code);

CREATE UNIQUE INDEX uq_obligations_code ON obligations (code);

CREATE UNIQUE INDEX uq_employees_code ON employees (code);

CREATE UNIQUE INDEX uq_employees_document ON employees (document);

CREATE UNIQUE INDEX uq_dishes_code ON dishes (code);

CREATE UNIQUE INDEX uq_discounts_code ON discounts (code);

CREATE UNIQUE INDEX uq_users_email ON users (email);

CREATE UNIQUE INDEX uq_periods_period ON periods (period);

CREATE UNIQUE INDEX uq_invoices_number ON invoices (number);

CREATE UNIQUE INDEX uq_stocks_product_id ON stocks (product_id, warehouse_id);

CREATE UNIQUE INDEX uq_stock_rules_product_id ON stock_rules (product_id, warehouse_id);

CREATE UNIQUE INDEX uq_payrolls_employee_id ON payrolls (employee_id, period);

CREATE UNIQUE INDEX uq_attendance_employee_id ON attendance (employee_id, date);


CREATE TABLE purchase_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, purchase_id BIGINT NOT NULL, product_id BIGINT NOT NULL, quantity DECIMAL(18,4) NOT NULL, received DECIMAL(18,4) NOT NULL DEFAULT 0, unit_cost DECIMAL(18,4) NOT NULL, tax_rate DECIMAL(8,2) NOT NULL, FOREIGN KEY(purchase_id) REFERENCES purchases(id), FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE receipt_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, receipt_id BIGINT NOT NULL, purchase_line_id BIGINT NOT NULL, quantity DECIMAL(18,4) NOT NULL, unit_cost DECIMAL(18,4) NOT NULL, FOREIGN KEY(receipt_id) REFERENCES receipts(id), FOREIGN KEY(purchase_line_id) REFERENCES purchase_lines(id));
CREATE TABLE recipes (id BIGINT AUTO_INCREMENT PRIMARY KEY, dish_id BIGINT NOT NULL, product_id BIGINT NOT NULL, quantity DECIMAL(18,4) NOT NULL, process TEXT, UNIQUE(dish_id,product_id), FOREIGN KEY(dish_id) REFERENCES dishes(id), FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE quote_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, quote_id BIGINT NOT NULL, dish_id BIGINT NOT NULL, quantity DECIMAL(18,4) NOT NULL, unit_price DECIMAL(18,2) NOT NULL, tax_rate DECIMAL(8,2) NOT NULL, FOREIGN KEY(quote_id) REFERENCES quotes(id), FOREIGN KEY(dish_id) REFERENCES dishes(id));
CREATE TABLE order_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, dish_id BIGINT NOT NULL, quantity DECIMAL(18,4) NOT NULL, unit_price DECIMAL(18,2) NOT NULL, tax_rate DECIMAL(8,2) NOT NULL, discount DECIMAL(18,2) NOT NULL DEFAULT 0, status VARCHAR(40) NOT NULL DEFAULT 'INGRESADO', notes TEXT, consumed BOOLEAN NOT NULL DEFAULT FALSE, recipe_snapshot TEXT NOT NULL, FOREIGN KEY(order_id) REFERENCES orders(id), FOREIGN KEY(dish_id) REFERENCES dishes(id));
CREATE TABLE journal_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, journal_id BIGINT NOT NULL, account_id BIGINT NOT NULL, debit DECIMAL(18,2) NOT NULL DEFAULT 0, credit DECIMAL(18,2) NOT NULL DEFAULT 0, FOREIGN KEY(journal_id) REFERENCES journals(id), FOREIGN KEY(account_id) REFERENCES accounts(id));
CREATE TABLE payments (id BIGINT AUTO_INCREMENT PRIMARY KEY, side VARCHAR(25) NOT NULL, debt_id BIGINT NOT NULL, amount DECIMAL(18,2) NOT NULL, cash_account_id BIGINT NOT NULL, treasury_id BIGINT NOT NULL, date DATE NOT NULL, file_id VARCHAR(80), FOREIGN KEY(cash_account_id) REFERENCES cash_accounts(id), FOREIGN KEY(treasury_id) REFERENCES treasury(id));
CREATE TABLE installments (id BIGINT AUTO_INCREMENT PRIMARY KEY, loan_id BIGINT NOT NULL, period VARCHAR(7) NOT NULL, amount DECIMAL(18,2) NOT NULL, principal DECIMAL(18,2) NOT NULL, interest DECIMAL(18,2) NOT NULL, status VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE', payroll_id BIGINT, FOREIGN KEY(loan_id) REFERENCES loans(id), FOREIGN KEY(payroll_id) REFERENCES payrolls(id));
CREATE TABLE depreciation_runs (id BIGINT AUTO_INCREMENT PRIMARY KEY, asset_id BIGINT NOT NULL, period VARCHAR(7) NOT NULL, amount DECIMAL(18,2) NOT NULL, UNIQUE(asset_id,period), FOREIGN KEY(asset_id) REFERENCES assets(id));
CREATE TABLE sessions (id BIGINT AUTO_INCREMENT PRIMARY KEY, token_hash VARCHAR(64) NOT NULL UNIQUE, user_id BIGINT NOT NULL, expires_at TIMESTAMP NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id));
CREATE TABLE permissions (id BIGINT AUTO_INCREMENT PRIMARY KEY, role VARCHAR(30) NOT NULL, module VARCHAR(30) NOT NULL, can_read BOOLEAN NOT NULL DEFAULT FALSE, can_write BOOLEAN NOT NULL DEFAULT FALSE, can_approve BOOLEAN NOT NULL DEFAULT FALSE, can_export BOOLEAN NOT NULL DEFAULT FALSE, UNIQUE(role,module));
CREATE TABLE files (id VARCHAR(80) PRIMARY KEY, original_name VARCHAR(255) NOT NULL, mime VARCHAR(80) NOT NULL, module VARCHAR(30) NOT NULL, owner_id BIGINT NOT NULL, size_bytes BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE document_sequences (series VARCHAR(10) PRIMARY KEY, next_value BIGINT NOT NULL);
CREATE INDEX ix_movements_product ON stock_moves(product_id,warehouse_id,date);
CREATE INDEX ix_treasury_date ON treasury(date);
CREATE INDEX ix_debt_due ON payables(due_date,status);
CREATE INDEX ix_order_state ON orders(status,date);
