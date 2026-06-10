# PetMeet 全量回归测试（API）

脚本：`scripts/full_regression_test.mjs`

## 覆盖范围
- 用户/管理端登录与基础健康检查
- 用户资料、地址、购物车
- 订单主链路：下单 -> 支付 -> 发货 -> 收货 -> 评价
- 售后链路：申请 -> 管理员处理 -> 用户完成
- 退款链路：已支付取消 -> 退款处理
- 笔记链路：发布 -> 审核 -> 置顶/推荐/屏蔽
- 点赞/收藏/评论/关注/投诉/反馈
- 通知已读流程
- 管理端 Banner/分类/用户/商品 CRUD（含批量接口）
- 管理端看板统计
- 上传接口（单图、批量）

## 运行
```bash
node scripts/full_regression_test.mjs
```

默认会拒绝执行写入操作。只有显式确认后才会真正跑全量回归。

## 可选环境变量
```bash
PETMEET_BASE_URL=http://localhost:8080
PETMEET_ADMIN_USERNAME=admin
PETMEET_ADMIN_PASSWORD=<your-admin-password>
PETMEET_USER_USERNAME=auto_u_xxx
PETMEET_USER_PASSWORD=PetMeetTest2026
PETMEET_ALLOW_MUTATION=true
```

## 说明
- 脚本默认阻止写库，必须显式设置 `PETMEET_ALLOW_MUTATION=true`。
- 脚本会写入测试数据（测试订单、测试笔记等）。
- 对临时创建的 Banner/分类/管理端测试用户/测试商品会自动尝试清理删除。
- 脚本结束会输出每个模块的 `PASS/FAIL` 矩阵与关键产物 ID，便于回溯。
