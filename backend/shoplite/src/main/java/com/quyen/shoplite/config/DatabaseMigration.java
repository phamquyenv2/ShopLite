package com.quyen.shoplite.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigration {
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try {
            jdbcTemplate.execute("ALTER TABLE payments MODIFY COLUMN method VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE payments MODIFY COLUMN status VARCHAR(50)");
            log.info("Successfully altered payments table lengths");
        } catch (Exception e) {
            log.warn("Could not alter payments table (maybe already altered): {}", e.getMessage());
        }

        migrateStoreScopedTables();
    }

    private void migrateStoreScopedTables() {
        Long defaultStoreId = findDefaultStoreId();
        if (defaultStoreId == null) {
            log.warn("Skipping store-scope backfill because no store exists yet");
            return;
        }

        addStoreColumnAndBackfill("categories", defaultStoreId);
        addStoreColumnAndBackfill("units", defaultStoreId);
        addStoreColumnAndBackfill("customers", defaultStoreId);
        addStoreColumnAndBackfill("suppliers", defaultStoreId);
        addStoreColumnAndBackfill("products", defaultStoreId);
        addStoreColumnAndBackfill("orders", defaultStoreId);
        addStoreColumnAndBackfill("offices", defaultStoreId);
        addStoreColumnAndBackfill("fund_accounts", defaultStoreId);
        addStoreColumnAndBackfill("employees", defaultStoreId);
        addStoreColumnAndBackfill("import_orders", defaultStoreId);
        addStoreColumnAndBackfill("import_return_orders", defaultStoreId);
        addStoreColumnAndBackfill("inventory_adjustments", defaultStoreId);
        addStoreColumnAndBackfill("inventory_logs", defaultStoreId);
        addStoreColumnAndBackfill("payments", defaultStoreId);
        addStoreColumnAndBackfill("transactions", defaultStoreId);
        backfillEmployeeSalaryHistories(defaultStoreId);
    }

    private Long findDefaultStoreId() {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM stores ORDER BY id LIMIT 1", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void addStoreColumnAndBackfill(String tableName, Long defaultStoreId) {
        executeIgnoringFailure("ALTER TABLE " + tableName + " ADD COLUMN store_id BIGINT NULL");
        executeIgnoringFailure("UPDATE " + tableName + " SET store_id = " + defaultStoreId + " WHERE store_id IS NULL");
        executeIgnoringFailure("ALTER TABLE " + tableName + " ADD INDEX idx_" + tableName + "_store_id (store_id)");
        executeIgnoringFailure("ALTER TABLE " + tableName
                + " ADD CONSTRAINT fk_" + tableName + "_store FOREIGN KEY (store_id) REFERENCES stores(id)");
        executeIgnoringFailure("ALTER TABLE " + tableName + " MODIFY COLUMN store_id BIGINT NOT NULL");
    }

    private void backfillEmployeeSalaryHistories(Long defaultStoreId) {
        executeIgnoringFailure("""
                INSERT INTO employee_salary_histories
                    (store_id, employee_id, salary_type, base_rate, allowance, commission,
                     recurring_bonus, recurring_deduction, effective_from, created_by, created_at)
                SELECT
                    COALESCE(e.store_id, %d),
                    e.id,
                    'HOURLY',
                    COALESCE(e.salary_rate, 0),
                    0,
                    0,
                    0,
                    0,
                    CURRENT_DATE,
                    'migration',
                    CURRENT_TIMESTAMP
                FROM employees e
                LEFT JOIN employee_salary_histories h
                    ON h.employee_id = e.id
                    AND h.store_id = COALESCE(e.store_id, %d)
                WHERE h.id IS NULL
                """.formatted(defaultStoreId, defaultStoreId));
    }

    private void executeIgnoringFailure(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("Migration SQL skipped: {} ({})", sql, e.getMessage());
        }
    }
}
