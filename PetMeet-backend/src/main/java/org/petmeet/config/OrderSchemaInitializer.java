package org.petmeet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            ensureOrderSchema();
        } catch (Exception e) {
            log.error("初始化oms_order删除标记字段失败", e);
        }
    }

    private void ensureOrderSchema() {
        if (!tableExists("oms_order")) {
            return;
        }

        ensureColumn(
                "oms_order",
                "user_deleted",
                "TINYINT NOT NULL DEFAULT 0 COMMENT 'user soft delete flag, 0=no,1=yes'"
        );
        ensureColumn(
                "oms_order",
                "admin_deleted",
                "TINYINT NOT NULL DEFAULT 0 COMMENT 'admin soft delete flag, 0=no,1=yes'"
        );
        ensureColumn(
                "oms_order",
                "refund_amount",
                "DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'refunded amount'"
        );
        ensureColumn(
                "oms_order",
                "pay_type",
                "TINYINT DEFAULT NULL COMMENT 'pay type:1=alipay,2=wechat mock'"
        );
        ensureColumn(
                "oms_order",
                "pay_sn",
                "VARCHAR(64) DEFAULT NULL COMMENT 'system pay serial number'"
        );
        ensureColumn(
                "oms_order",
                "trade_no",
                "VARCHAR(128) DEFAULT NULL COMMENT 'third party trade number'"
        );
        ensureColumn(
                "oms_order",
                "remark",
                "VARCHAR(500) DEFAULT NULL COMMENT 'order remark'"
        );
        ensureIndex("oms_order", "idx_pay_sn", "`pay_sn`");
        createPaymentTables();
        ensureUniquePayTradeNo();

        jdbcTemplate.update("UPDATE `oms_order` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `oms_order` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `oms_order` SET `refund_amount` = 0.00 WHERE `refund_amount` IS NULL");

        // 修正历史数据里的状态不一致问题：
        // - 已有物流信息，但状态还是待付款/已支付/null -> 改成已发货
        // - 已有支付时间，但状态还是待付款/null 且没有物流信息 -> 改成已支付
        jdbcTemplate.update("""
                UPDATE `oms_order`
                SET `status` = 2
                WHERE (`status` IS NULL OR `status` IN (0, 1))
                  AND (`ship_time` IS NOT NULL OR `tracking_no` IS NOT NULL OR `ship_company` IS NOT NULL)
                """);

        jdbcTemplate.update("""
                UPDATE `oms_order`
                SET `status` = 1
                WHERE (`status` IS NULL OR `status` = 0)
                  AND `pay_time` IS NOT NULL
                  AND `ship_time` IS NULL
                  AND `tracking_no` IS NULL
                  AND `ship_company` IS NULL
                """);
    }

    private void createPaymentTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `oms_pay_log` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pay log id',
                    `pay_sn` VARCHAR(64) NOT NULL COMMENT 'system pay serial number',
                    `order_id` BIGINT NOT NULL COMMENT 'order id',
                    `order_sn` VARCHAR(64) NOT NULL COMMENT 'order serial number',
                    `user_id` BIGINT NOT NULL COMMENT 'user id',
                    `pay_type` TINYINT NOT NULL COMMENT 'pay type:1=alipay,2=wechat mock',
                    `pay_mode` TINYINT NOT NULL DEFAULT 1 COMMENT 'pay mode:1=qr code',
                    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT 'pay amount',
                    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT 'pay status:0=pending,1=success,2=failed,3=closed',
                    `trade_no` VARCHAR(128) DEFAULT NULL COMMENT 'third party trade number',
                    `qr_code_url` VARCHAR(500) DEFAULT NULL COMMENT 'qr code content',
                    `pay_page_url` TEXT DEFAULT NULL COMMENT 'pay page content',
                    `expire_time` DATETIME NOT NULL COMMENT 'expire time',
                    `pay_time` DATETIME DEFAULT NULL COMMENT 'pay time',
                    `callback_time` DATETIME DEFAULT NULL COMMENT 'callback time',
                    `callback_content` TEXT DEFAULT NULL COMMENT 'callback content',
                    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT 'error message',
                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_pay_sn` (`pay_sn`),
                    KEY `idx_order_id` (`order_id`),
                    KEY `idx_order_sn` (`order_sn`),
                    KEY `idx_user_id` (`user_id`),
                    UNIQUE KEY `uk_pay_trade_no` (`pay_type`, `trade_no`),
                    KEY `idx_pay_status` (`pay_status`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pay log'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `oms_refund_log` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'refund log id',
                    `refund_sn` VARCHAR(64) NOT NULL COMMENT 'refund serial number',
                    `order_id` BIGINT NOT NULL COMMENT 'order id',
                    `order_sn` VARCHAR(64) NOT NULL COMMENT 'order serial number',
                    `pay_log_id` BIGINT DEFAULT NULL COMMENT 'pay log id',
                    `after_sale_id` BIGINT DEFAULT NULL COMMENT 'after sale id',
                    `user_id` BIGINT NOT NULL COMMENT 'user id',
                    `pay_type` TINYINT DEFAULT NULL COMMENT 'pay type:1=alipay,2=wechat mock',
                    `refund_amount` DECIMAL(10,2) NOT NULL COMMENT 'refund amount',
                    `refund_reason` VARCHAR(200) DEFAULT NULL COMMENT 'refund reason',
                    `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT 'refund status:0=pending,1=success,2=failed',
                    `trade_no` VARCHAR(128) DEFAULT NULL COMMENT 'refund trade number',
                    `refund_time` DATETIME DEFAULT NULL COMMENT 'refund time',
                    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT 'error message',
                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_refund_sn` (`refund_sn`),
                    KEY `idx_order_id` (`order_id`),
                    KEY `idx_after_sale_id` (`after_sale_id`),
                    KEY `idx_refund_status` (`refund_status`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='refund log'
                """);
    }

    private void ensureUniquePayTradeNo() {
        if (!tableExists("oms_pay_log") || uniqueIndexExists("oms_pay_log", "uk_pay_trade_no")) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE `oms_pay_log` loser
                JOIN `oms_pay_log` winner
                  ON winner.`pay_type` = loser.`pay_type`
                 AND winner.`trade_no` = loser.`trade_no`
                 AND (
                      (winner.`pay_status` = 1 AND loser.`pay_status` <> 1)
                      OR (winner.`pay_status` = loser.`pay_status` AND winner.`id` < loser.`id`)
                 )
                SET loser.`pay_status` = IF(loser.`pay_status` = 1, loser.`pay_status`, 3),
                    loser.`error_msg` = CONCAT('duplicate third-party trade number:', loser.`trade_no`),
                    loser.`trade_no` = NULL,
                    loser.`update_time` = NOW()
                WHERE loser.`trade_no` IS NOT NULL
                """);
        if (indexExists("oms_pay_log", "uk_pay_trade_no")) {
            jdbcTemplate.execute("ALTER TABLE `oms_pay_log` DROP INDEX `uk_pay_trade_no`");
        }
        jdbcTemplate.execute("ALTER TABLE `oms_pay_log` ADD UNIQUE KEY `uk_pay_trade_no` (`pay_type`, `trade_no`)");
        log.info("宸茶ˉ鍏呯储寮晎}.{}", "oms_pay_log", "uk_pay_trade_no");
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

    private boolean uniqueIndexExists(String tableName, String indexName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? AND NON_UNIQUE = 0",
                Integer.class,
                tableName,
                indexName
        );
        return cnt != null && cnt > 0;
    }
}
