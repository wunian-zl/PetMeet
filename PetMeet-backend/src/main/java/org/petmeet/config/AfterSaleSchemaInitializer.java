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
            log.error("Failed to ensure oms_after_sale schema for delete flags", e);
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

        jdbcTemplate.update("UPDATE `oms_after_sale` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `oms_after_sale` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL");
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
