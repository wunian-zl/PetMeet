-- Add evidence image urls to content complaints.
-- Usage:
--   mysql -u root -p petmeet < sql\migration_add_complaint_evidence_images.sql

SET @db := DATABASE();

SELECT IF(
  COUNT(*) = 0,
  'ALTER TABLE `cms_complaint` ADD COLUMN `evidence_images` TEXT DEFAULT NULL COMMENT ''投诉凭证图片列表（JSON 数组）'' AFTER `content`',
  'SELECT ''cms_complaint.evidence_images already exists'''
) INTO @sql_add_complaint_evidence_images
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'cms_complaint'
  AND COLUMN_NAME = 'evidence_images';

PREPARE stmt FROM @sql_add_complaint_evidence_images;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
