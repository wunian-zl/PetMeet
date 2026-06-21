package org.petmeet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 确保本地做增量升级时，投诉表结构始终存在。
 * 如果生产环境已经接入 Flyway/Liquibase 这类正式迁移工具，
 * 可以移除或停用这个初始化器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplaintSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            ensureComplaintSchema();
        } catch (Exception e) {
            log.error("初始化cms_complaint表结构失败,请执行SQL迁移:sql/migration_extend_cms_complaint_feedback.sql", e);
        }
    }

    private void ensureComplaintSchema() {
        if (!tableExists("cms_complaint")) {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `cms_complaint` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
                        `note_id` BIGINT NOT NULL COMMENT 'Note ID',
                        `target_type` VARCHAR(20) NOT NULL DEFAULT 'note' COMMENT 'Target type',
                        `comment_id` BIGINT DEFAULT NULL COMMENT 'Comment ID',
                        `parent_id` BIGINT DEFAULT NULL COMMENT 'Parent complaint id',
                        `user_id` BIGINT NOT NULL COMMENT 'Reporter ID',
                        `reason` VARCHAR(50) NOT NULL COMMENT 'Reason',
                        `content` VARCHAR(500) DEFAULT NULL COMMENT 'Detail',
                        `evidence_images` TEXT DEFAULT NULL COMMENT 'Evidence image urls JSON array',
                        `status` TINYINT DEFAULT 0 COMMENT '0=pending,1=handled,2=rejected',
                        `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0=visible,1=user-deleted',
                        `admin_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0=visible,1=admin-deleted',
                        `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT 'Handle remark/result',
                        `feedback_status` TINYINT DEFAULT 0 COMMENT '0=none,1=satisfied,2=unsatisfied',
                        `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT 'User feedback content',
                        `feedback_time` DATETIME DEFAULT NULL COMMENT 'User feedback time',
                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
                        `handle_time` DATETIME DEFAULT NULL COMMENT 'Handle time',
                        `handler_id` BIGINT DEFAULT NULL COMMENT 'Handler',
                        PRIMARY KEY (`id`),
                        KEY `idx_note` (`note_id`),
                        KEY `idx_target` (`target_type`, `note_id`, `comment_id`),
                        KEY `idx_user` (`user_id`),
                        KEY `idx_parent` (`parent_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Note complaints';
                    """);
            log.info("创建成功 table cms_complaint");
            return;
        }

        ensureColumn("cms_complaint", "parent_id", "BIGINT DEFAULT NULL COMMENT 'Parent complaint id' AFTER `note_id`");
        ensureColumn("cms_complaint", "target_type", "VARCHAR(20) NOT NULL DEFAULT 'note' COMMENT 'Target type' AFTER `note_id`");
        ensureColumn("cms_complaint", "comment_id", "BIGINT DEFAULT NULL COMMENT 'Comment ID' AFTER `target_type`");
        ensureColumn("cms_complaint", "handle_remark", "VARCHAR(500) DEFAULT NULL COMMENT 'Handle remark/result'");
        ensureColumn("cms_complaint", "evidence_images", "TEXT DEFAULT NULL COMMENT 'Evidence image urls JSON array' AFTER `content`");
        ensureColumn("cms_complaint", "feedback_status", "TINYINT DEFAULT 0 COMMENT '0=none,1=satisfied,2=unsatisfied'");
        ensureColumn("cms_complaint", "feedback_content", "VARCHAR(500) DEFAULT NULL COMMENT 'User feedback content'");
        ensureColumn("cms_complaint", "feedback_time", "DATETIME DEFAULT NULL COMMENT 'User feedback time'");
        ensureColumn("cms_complaint", "user_deleted", "TINYINT NOT NULL DEFAULT 0 COMMENT '0=visible,1=user-deleted'");
        ensureColumn("cms_complaint", "admin_deleted", "TINYINT NOT NULL DEFAULT 0 COMMENT '0=visible,1=admin-deleted'");
        jdbcTemplate.update("UPDATE `cms_complaint` SET `user_deleted` = 0 WHERE `user_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `cms_complaint` SET `admin_deleted` = 0 WHERE `admin_deleted` IS NULL");
        jdbcTemplate.update("UPDATE `cms_complaint` SET `target_type` = 'note' WHERE `target_type` IS NULL OR `target_type` = ''");
        ensureIndex("cms_complaint", "idx_target", "`target_type`, `note_id`, `comment_id`");
        ensureIndex("cms_complaint", "idx_parent", "`parent_id`");
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

    private boolean columnExists(String tableName, String columnName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return cnt != null && cnt > 0;
    }

    private void ensureIndex(String tableName, String indexName, String columnsSql) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (cnt != null && cnt > 0) {
            return;
        }
        String sql = "ALTER TABLE `" + tableName + "` ADD INDEX `" + indexName + "` (" + columnsSql + ")";
        jdbcTemplate.execute(sql);
        log.info("已补充索引{}到{}", indexName, tableName);
    }
}
