-- 确保管理端软删除时同步写入逻辑删除标记
DROP TRIGGER IF EXISTS trg_cms_note_soft_delete_bi;
DROP TRIGGER IF EXISTS trg_cms_note_soft_delete_bu;

DELIMITER $$
CREATE TRIGGER trg_cms_note_soft_delete_bi
BEFORE INSERT ON cms_note
FOR EACH ROW
BEGIN
  IF NEW.status = 6 THEN
    SET NEW.is_deleted = 1;
  END IF;
END$$

CREATE TRIGGER trg_cms_note_soft_delete_bu
BEFORE UPDATE ON cms_note
FOR EACH ROW
BEGIN
  IF NEW.status = 6 THEN
    SET NEW.is_deleted = 1;
  END IF;
END$$
DELIMITER ;

-- 修正历史上状态与删除标记不一致的数据
UPDATE cms_note
SET is_deleted = 1
WHERE status = 6 AND is_deleted = 0;
