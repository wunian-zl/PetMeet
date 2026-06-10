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
            log.error("Failed to ensure oms_order schema for delete flags", e);
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

        jdbcTemplate.update("UPDATE `oms_order` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `oms_order` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL");

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
        log.info("Added column {}.{}", tableName, columnName);
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
}
