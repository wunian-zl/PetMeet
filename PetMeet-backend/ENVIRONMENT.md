# PetMeet后端环境变量

后端启动前必须提供MySQL密码：

```text
PETMEET_DB_PASSWORD=<本机MySQL密码>
```

如果数据库中还不存在`admin`账号，还需要提供：

```text
PETMEET_ADMIN_PASSWORD=<管理员初始密码>
```

已有管理员账号时不需要配置`PETMEET_ADMIN_PASSWORD`。它只用于首次创建管理员，不会修改已有管理员密码。

## IDEA配置

打开`Run/Debug Configurations`，选择后端启动配置，在`Environment variables`中填写：

```text
PETMEET_DB_PASSWORD=你的MySQL密码
```

全新数据库首次启动时再追加`;PETMEET_ADMIN_PASSWORD=管理员初始密码`。

不要把真实密码写回`application.yml`，也不要提交包含真实密码的本地配置文件。

## PowerShell启动

```powershell
$env:PETMEET_DB_PASSWORD="你的MySQL密码"
mvn spring-boot:run
```

全新数据库首次启动时，还需要先执行：

```powershell
$env:PETMEET_ADMIN_PASSWORD="管理员初始密码"
```

## 本地开发辅助启动

如果希望启动后端时同时启动Redis、用户端和管理端，可以开启：

```powershell
$env:PETMEET_DEV_LAUNCH_ALL="true"
mvn spring-boot:run
```

默认行为：

- 执行仓库根目录下的`scripts/start-redis-if-needed.ps1`，Redis已运行时不会重复启动。
- 执行`scripts/dev-frontends.mjs`，启动用户端`5173`和管理端`5174`。
- 后端进程退出时，自动结束前端开发进程树。

可选开关：

```powershell
$env:PETMEET_DEV_LAUNCH_REDIS="false"
$env:PETMEET_DEV_LAUNCH_FRONTENDS="false"
```

这些开关只建议本地开发使用，不要在部署环境中开启。
