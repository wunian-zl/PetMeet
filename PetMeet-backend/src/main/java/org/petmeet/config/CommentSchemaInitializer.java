package org.petmeet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            ensureCommentSchema();
        } catch (Exception e) {
            log.error("初始化cms_comment表结构失败,请执行SQL迁移:sql/migration_extend_cms_comment_thread.sql", e);
        }
    }

    private void ensureCommentSchema() {
        if (!tableExists("cms_comment")) {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `cms_comment` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Comment ID',
                        `note_id` BIGINT NOT NULL COMMENT 'Note ID',
                        `parent_id` BIGINT DEFAULT NULL COMMENT 'Root comment ID',
                        `reply_to_id` BIGINT DEFAULT NULL COMMENT 'Reply target comment ID',
                        `user_id` BIGINT NOT NULL COMMENT 'User ID',
                        `content` VARCHAR(500) NOT NULL COMMENT 'Content',
                        `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
                        `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
                        `delete_time` DATETIME DEFAULT NULL COMMENT 'Delete time',
                        PRIMARY KEY (`id`),
                        KEY `idx_note_id` (`note_id`),
                        KEY `idx_user_id` (`user_id`),
                        KEY `idx_parent_id` (`parent_id`),
                        KEY `idx_reply_to_id` (`reply_to_id`),
                        KEY `idx_note_parent_status_time` (`note_id`, `parent_id`, `status`, `create_time`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comments';
                    """);
            log.info("创建成功 table cms_comment");
            return;
        }

        ensureColumn("cms_comment", "parent_id", "BIGINT DEFAULT NULL COMMENT 'Root comment ID' AFTER `note_id`");
        ensureColumn("cms_comment", "reply_to_id", "BIGINT DEFAULT NULL COMMENT 'Reply target comment ID' AFTER `parent_id`");
        ensureColumn("cms_comment", "like_count", "INT NOT NULL DEFAULT 0 COMMENT 'Like count' AFTER `content`");
        ensureColumn("cms_comment", "status", "TINYINT NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted' AFTER `like_count`");
        ensureColumn("cms_comment", "delete_time", "DATETIME DEFAULT NULL COMMENT 'Delete time' AFTER `create_time`");
        jdbcTemplate.update("UPDATE `cms_comment` SET `like_count` = 0 WHERE `like_count` IS NULL");
        jdbcTemplate.update("UPDATE `cms_comment` SET `status` = 0 WHERE `status` IS NULL");
        ensureIndex("cms_comment", "idx_parent_id", "`parent_id`");
        ensureIndex("cms_comment", "idx_reply_to_id", "`reply_to_id`");
        ensureIndex("cms_comment", "idx_note_parent_status_time", "`note_id`, `parent_id`, `status`, `create_time`");
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
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definitionSql);
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
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD INDEX `" + indexName + "` (" + columnsSql + ")");
        log.info("已补充索引{}到{}", indexName, tableName);
    }
}
