package org.petmeet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterSaleSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            ensureAfterSaleSchema();
        } catch (Exception e) {
            log.error("初始化oms_after_sale删除标记字段失败", e);
        }
    }

    private void ensureAfterSaleSchema() {
        if (!tableExists("oms_after_sale")) {
            return;
        }

        ensureColumn(
                "oms_after_sale",
                "user_deleted",
                "TINYINT NOT NULL DEFAULT 0 COMMENT 'user soft delete flag, 0=no,1=yes'"
        );

        ensureColumn(
                "oms_after_sale",
                "admin_deleted",
                "TINYINT NOT NULL DEFAULT 0 COMMENT 'admin soft delete flag, 0=no,1=yes'"
        );
        ensureColumn(
                "oms_after_sale",
                "refund_amount",
                "DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'after sale refund amount'"
        );
        ensureColumn(
                "oms_after_sale",
                "return_address",
                "VARCHAR(500) DEFAULT NULL COMMENT 'merchant return address'"
        );
        ensureColumn(
                "oms_after_sale",
                "return_company",
                "VARCHAR(100) DEFAULT NULL COMMENT 'buyer return logistics company'"
        );
        ensureColumn(
                "oms_after_sale",
                "return_tracking_no",
                "VARCHAR(100) DEFAULT NULL COMMENT 'buyer return tracking number'"
        );
        ensureColumn(
                "oms_after_sale",
                "return_ship_time",
                "DATETIME DEFAULT NULL COMMENT 'buyer return ship time'"
        );
        ensureColumn(
                "oms_after_sale",
                "return_receive_time",
                "DATETIME DEFAULT NULL COMMENT 'merchant receive return time'"
        );
        ensureColumn(
                "oms_after_sale",
                "exchange_company",
                "VARCHAR(100) DEFAULT NULL COMMENT 'exchange logistics company'"
        );
        ensureColumn(
                "oms_after_sale",
                "exchange_tracking_no",
                "VARCHAR(100) DEFAULT NULL COMMENT 'exchange tracking number'"
        );
        ensureColumn(
                "oms_after_sale",
                "exchange_ship_time",
                "DATETIME DEFAULT NULL COMMENT 'exchange ship time'"
        );

        createAfterSaleLogTable();
        ensureIndex("oms_after_sale", "idx_order_status", "`order_id`, `status`");
        ensureIndex("oms_after_sale", "idx_type_status", "`type`, `status`");

        jdbcTemplate.update("UPDATE `oms_after_sale` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `oms_after_sale` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `oms_after_sale` SET `refund_amount` = 0.00 WHERE `refund_amount` IS NULL");
    }

    private void createAfterSaleLogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `oms_after_sale_log` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'after sale log id',
                    `after_sale_id` BIGINT NOT NULL COMMENT 'after sale id',
                    `from_status` TINYINT DEFAULT NULL COMMENT 'from status',
                    `to_status` TINYINT NOT NULL COMMENT 'to status',
                    `action` VARCHAR(50) NOT NULL COMMENT 'action code',
                    `operator_type` VARCHAR(20) NOT NULL COMMENT 'operator type',
                    `operator_id` BIGINT DEFAULT NULL COMMENT 'operator id',
                    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'remark',
                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                    PRIMARY KEY (`id`),
                    KEY `idx_after_sale_id` (`after_sale_id`),
                    KEY `idx_create_time` (`create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='after sale operation log'
                """);
    }

    private boolean tableExists(String tableName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return cnt != null && cnt > 0;
    }

    private void ensureColumn(String tableName, String columnName, String definitionSql) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definitionSql;
        jdbcTemplate.execute(sql);
        log.info("已补充字段{}.{}", tableName, columnName);
    }

    private void ensureIndex(String tableName, String indexName, String columnsSql) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD INDEX `" + indexName + "` (" + columnsSql + ")");
        log.info("已补充索引{}.{}", tableName, indexName);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return cnt != null && cnt > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return cnt != null && cnt > 0;
    }
}
