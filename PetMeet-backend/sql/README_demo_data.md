# 线上演示数据增强

适用场景：公网演示站数据量太少，或旧补数据脚本导致笔记、商品图文不匹配。

## 脚本效果

`seed_public_demo_enrichment.sql`会做这些事：

- 修复一批已知的笔记图文错配。
- 修复旧补充商品中标题和图片不一致的记录。
- 新增10个演示用户。
- 新增28条图文一致的笔记。
- 新增约112条评论和回复。
- 新增点赞、收藏、评论点赞、关注关系。
- 新增36笔订单，覆盖待付款、待发货、待收货、已完成、退款中等状态。
- 新增售后、退款流水、投诉、站内通知数据。

脚本不会重建业务表，不会清空生产数据。重复执行时只会清理并重建本脚本生成的数据：

- 用户名以`pm_seed_`开头的演示用户。
- 订单号以`PMD20260621`开头的演示订单。

## 推荐执行方式

在本机项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\apply_public_demo_enrichment.ps1
```

默认连接：

```text
ubuntu@124.220.91.57
```

如果用户名或地址不同：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\apply_public_demo_enrichment.ps1 -HostName 124.220.91.57 -User ubuntu
```

辅助脚本会：

1.上传SQL到服务器`/tmp/seed_public_demo_enrichment.sql`。
2.执行`sudo mysql --default-character-set=utf8mb4`导入数据。
3.清理Redis中的笔记列表和点赞缓存。
4.输出当前笔记、评论、订单数量。

## 手动执行方式

如果不使用辅助脚本，可以手动上传并执行：

```powershell
scp PetMeet-backend\sql\seed_public_demo_enrichment.sql ubuntu@124.220.91.57:/tmp/
ssh ubuntu@124.220.91.57
```

服务器上先备份：

```bash
sudo mysqldump --default-character-set=utf8mb4 petmeet > /home/ubuntu/petmeet_backup_$(date +%Y%m%d_%H%M%S).sql
```

再执行：

```bash
sudo mysql --default-character-set=utf8mb4 < /tmp/seed_public_demo_enrichment.sql
```

清理缓存：

```bash
redis-cli INCR cache:note:list:ver
redis-cli --scan --pattern 'cache:note:list:*' | xargs -r redis-cli DEL
redis-cli --scan --pattern 'note:like:count:*' | xargs -r redis-cli DEL
redis-cli --scan --pattern 'note:like:set:*' | xargs -r redis-cli DEL
```

检查数量：

```bash
sudo mysql --default-character-set=utf8mb4 -N -e "
USE petmeet;
SELECT 'published_notes', COUNT(*) FROM cms_note WHERE status = 1;
SELECT 'comments', COUNT(*) FROM cms_comment WHERE status = 0;
SELECT 'orders', COUNT(*) FROM oms_order WHERE admin_deleted = 0;
SELECT 'seed_orders', COUNT(*) FROM oms_order WHERE order_sn LIKE 'PMD20260621%';
"
```

## 注意事项

- 线上服务器必须已经有`/opt/petmeet/uploads/demo`图片目录，否则新笔记会有图片路径但文件不存在。
- 如果图片缺失，重新上传部署包里的`uploads/demo`目录到`/opt/petmeet/uploads/demo`。
- 如果执行后首页仍显示旧数据，优先检查Redis缓存清理是否成功。
