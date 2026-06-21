# PetMeet

基于Spring Boot和Vue3开发的宠物社区与宠物用品商城系统，包含用户端、管理端和后端服务。项目覆盖内容社区、商品交易、订单售后、投诉审核、运营统计等业务，适用于Java全栈开发的学习与交流。

![Java](https://img.shields.io/badge/Java-17-E76F00)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.7-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3.5-42B883)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)
![License](https://img.shields.io/badge/License-MIT-green)

## 项目简介

PetMeet将宠物内容社区与宠物用品商城整合在同一套系统中。普通用户可以浏览和发布宠物笔记、互动关注、购买商品并处理订单售后；管理员可以在独立管理端完成内容审核、用户管理、商品管理、订单处理、投诉处理和数据统计。

项目采用前后端分离架构：

- 用户端：面向普通用户的社区与商城。
- 管理端：面向平台运营人员的后台管理系统。
- 后端：提供RESTful API、身份认证、业务逻辑和数据持久化。

## 功能模块

### 用户端

- 用户注册、登录、退出和个人资料维护。
- 宠物笔记发布、浏览、点赞、收藏、评论和上下架。
- 用户关注、粉丝列表和关注列表。
- 商品分类、商品筛选、商品详情和广告跳转。
- 购物车增删改、商品勾选和数量统计。
- 收货地址、订单提交、模拟支付、取消订单和确认收货。
- 商品评价、退款售后申请及售后进度查看。
- 内容投诉、处理结果查看和用户反馈。
- 站内通知和个人中心数据展示。

### 管理端

- 运营仪表盘、趋势统计、分类销售占比和热门商品排行。
- 用户查询、禁用、解禁、删除、强制下线和密码重置。
- 笔记审核、推荐、置顶、屏蔽、删除和批量操作。
- 商品新增、编辑、上下架、分类管理和批量操作。
- 订单查询、发货、退款、取消、修改地址和导出。
- 售后申请处理、投诉处理和评论管理。
- 广告新增、编辑、启停、排序和可视化跳转目标配置。

> 支付功能为本地模拟支付，不接入真实第三方支付平台。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot3.3.7、Java17 |
| 数据访问 | MyBatis-Plus3.5.5、Druid |
| 身份认证 | Sa-Token、Redis |
| 数据库 | MySQL8.0 |
| 接口文档 | Knife4j、OpenAPI3 |
| 用户端 | Vue3、Vite、Element Plus、Pinia、Vue Router |
| 管理端 | Vue3、Vite、Element Plus、ECharts、Pinia |
| 测试 | JUnit5、Spring Boot Test、Mockito |

## 项目亮点

- 实现社区内容与商城交易两条业务链路，并在订单、评价和内容推荐之间建立关联。
- 使用Sa-Token完成登录认证和角色鉴权，管理接口同时校验登录状态与`admin`角色。
- 业务数据根据当前登录用户进行归属校验，避免越权读取或操作其他用户的数据。
- 订单提交、支付、取消和售后等关键流程使用事务处理，并维护商品库存和订单状态。
- 使用Redis支持登录状态及部分高频数据处理。
- 管理端广告跳转目标通过商品类型和上架时间生成，减少人工填写URL的操作成本。
- 数据库密码和管理员初始密码通过环境变量注入，避免在配置文件中保存明文凭据。
- 已补充管理员权限、订单归属和管理员初始化等自动化测试。

## 目录结构

```text
PetMeet
├─PetMeet-backend                 Spring Boot后端
│  ├─sql                          建表、迁移和修复脚本
│  └─src
│     ├─main                      业务代码与配置
│     └─test                      自动化测试
├─PetMeet-frontend
│  ├─PetMeet-user                 用户端
│  └─PetMeet-admin                管理端
├─uploads
│  └─demo                         可随仓库分发的演示图片
└─scripts                         联调、回归和辅助脚本
```

## 环境要求

- JDK17
- Maven3.8+
- Node.js18+
- MySQL8.0
- Redis

## 快速启动

### 1.克隆项目

```bash
git clone https://github.com/wunian-zl/PetMeet.git
cd PetMeet
```

### 2.初始化数据库

确保MySQL服务已经启动，然后执行：

```bash
mysql -u root -p < PetMeet-backend/sql/ddl.sql
```

`ddl.sql`是新环境的完整初始化入口，会创建`petmeet`数据库、16张业务表，并写入演示用户、笔记、商品、分类和商城广告数据，不需要再逐个执行`migration_*.sql`。

演示数据引用的图片位于`uploads/demo`，这些文件已纳入Git版本控制。后端启动后会通过`http://localhost:8080/images/demo/...`提供访问，因此其他人克隆仓库并按上述步骤初始化数据库后，可以直接看到笔记图片、商品图片和商城横幅。

用户后续上传的其他文件仍保存在`uploads`中并被`.gitignore`忽略，避免把运行时数据提交到仓库。

> `ddl.sql`会删除同名业务表后重新创建，只适合全新环境。已有数据库升级时应按需执行`PetMeet-backend/sql`目录中的迁移脚本，避免丢失数据。

### 3.启动Redis

启动本机Redis服务，默认连接地址为：

```text
localhost:6379
```

> 仓库不包含本机的`PetMeet-backend/Redis`目录，该目录已被`.gitignore`排除。其他人克隆项目后，需要自行安装Redis或准备兼容的Redis服务。

Windows环境中，如果`redis-server`和`redis-cli`已经加入系统`PATH`，可以使用项目中的辅助脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-redis-if-needed.ps1
```

如果Redis未加入`PATH`，请通过`PETMEET_REDIS_DIR`指定包含`redis-server.exe`和`redis-cli.exe`的目录：

```powershell
$env:PETMEET_REDIS_DIR="C:\path\to\Redis"
powershell -ExecutionPolicy Bypass -File scripts/start-redis-if-needed.ps1
```

也可以通过WSL或Docker运行Redis，只需确保Windows本机能够访问`127.0.0.1:6379`。

启动后可执行以下命令进行验证，正常结果应为`PONG`：

```powershell
redis-cli -h 127.0.0.1 -p 6379 ping
```

### 4.启动后端

后端启动前必须通过环境变量提供MySQL密码。全新数据库首次启动时，还需提供管理员初始密码。

```powershell
cd PetMeet-backend
$env:PETMEET_DB_PASSWORD="你的MySQL密码"
$env:PETMEET_ADMIN_PASSWORD="管理员初始密码"
mvn spring-boot:run
```

如果数据库中已经存在`admin`账号，则只需配置`PETMEET_DB_PASSWORD`。

本地开发时，如果希望只启动后端就自动启动Redis、用户端和管理端，可以额外开启开发辅助启动：

```powershell
cd PetMeet-backend
$env:PETMEET_DB_PASSWORD="你的MySQL密码"
$env:PETMEET_ADMIN_PASSWORD="管理员初始密码"
$env:PETMEET_DEV_LAUNCH_ALL="true"
mvn spring-boot:run
```

该开关默认关闭，仅建议本地开发使用。后端启动完成后会先执行`scripts/start-redis-if-needed.ps1`检查Redis，再通过`scripts/dev-frontends.mjs`启动两个Vite开发服务。后端退出时会尝试一并关闭前端开发进程。

后端默认地址：

```text
http://localhost:8080
```

接口文档：

```text
http://localhost:8080/doc.html
```

### 5.启动用户端

新开一个终端：

```bash
cd PetMeet-frontend/PetMeet-user
npm install
npm run dev
```

用户端默认地址：

```text
http://localhost:5173
```

### 6.启动管理端

再新开一个终端：

```bash
cd PetMeet-frontend/PetMeet-admin
npm install
npm run dev
```

管理端默认地址：

```text
http://localhost:5174/admin/login
```

管理员用户名默认为`admin`，密码为首次启动时通过`PETMEET_ADMIN_PASSWORD`设置的值。

## 配置说明

核心环境变量：

| 环境变量 | 是否必填 | 说明 |
| --- | --- | --- |
| `PETMEET_DB_PASSWORD` | 是 | 本机MySQL密码 |
| `PETMEET_ADMIN_PASSWORD` | 首次启动必填 | 数据库中不存在管理员时，用于创建初始管理员 |
| `PETMEET_DB_USER` | 辅助脚本可选 | MySQL用户名，默认`root` |
| `PETMEET_DB_NAME` | 辅助脚本可选 | 数据库名，默认`petmeet` |
| `PETMEET_REDIS_DIR` | Redis辅助脚本可选 | Windows版Redis可执行文件所在目录；Redis已加入`PATH`时无需配置 |

不要将真实密码写入`application.yml`或提交到Git仓库。IDEA配置方式可参考[后端环境变量说明](PetMeet-backend/ENVIRONMENT.md)。

## 测试与构建

运行后端测试：

```bash
cd PetMeet-backend
mvn test
```

构建后端：

```bash
mvn clean package -DskipTests
```

构建用户端：

```bash
cd PetMeet-frontend/PetMeet-user
npm run build
```

构建管理端：

```bash
cd PetMeet-frontend/PetMeet-admin
npm run build
```

当前自动化测试重点覆盖：

- 普通用户无法访问管理员接口。
- 无Token、无效Token和非管理员角色请求被正确拒绝。
- 用户只能操作自己的订单。
- 订单提交时校验商品、数量、价格和收货地址。
- 已有管理员时无需重复提供初始密码。
- 全新数据库缺少管理员初始密码时阻止不安全启动。

## 安全设计

- 用户密码使用BCrypt哈希存储。
- 管理接口统一进行身份和角色校验。
- 数据库密码和管理员初始密码使用环境变量管理。
- 用户资源操作使用登录用户身份进行归属校验。
- 文件上传接口区分普通用户与管理员业务场景。
- 订单等关键业务使用事务保证数据一致性。

## 后续计划

- 引入Docker Compose统一管理MySQL、Redis和应用服务。
- 增加GitHub Actions持续集成。
- 继续完善接口测试和前端端到端测试。
- 优化前端代码分包和首屏加载性能。

## 项目说明

本项目主要用于学习与交流。

## 开源许可证

本项目基于[MIT License](LICENSE)开源。
