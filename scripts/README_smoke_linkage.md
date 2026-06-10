# PetMeet 联动冒烟测试

脚本：`scripts/smoke_linkage_test.mjs`

## 覆盖链路
- 用户登录
- 管理端登录
- 用户下单（购物车 -> 提交）
- 用户支付
- 管理端发货
- 用户确认收货 + 评价
- 用户发布种草笔记（关联已购商品）
- 管理端审核通过笔记
- 用户通知和管理端订单查询联动校验

## 运行方式
```bash
node scripts/smoke_linkage_test.mjs
```

默认会拒绝执行写入操作。只有显式确认后才会真正跑联调。

## 可选环境变量
```bash
PETMEET_BASE_URL=http://localhost:8080
PETMEET_ADMIN_USERNAME=admin
PETMEET_ADMIN_PASSWORD=<your-admin-password>
PETMEET_USER_USERNAME=autotest_user_xxx
PETMEET_USER_PASSWORD=PetMeetTest2026
PETMEET_KEEP_CART=false
PETMEET_ALLOW_MUTATION=true
```

说明：
- 管理员密码必须通过`PETMEET_ADMIN_PASSWORD`提供。
- 默认用户账号会自动生成，不会和现有账号冲突。
- 脚本默认阻止写库，必须显式设置 `PETMEET_ALLOW_MUTATION=true`。
- 即使 `BASE_URL` 是本机地址，也不要对主开发库直接执行。
