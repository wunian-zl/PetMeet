# PetMeet腾讯云轻量应用服务器部署指南

##1.当前服务器

已按`124.220.91.57`确认：

- 系统：Ubuntu22.04.5LTS
- 用户：`ubuntu`
- 配置：2核2GB内存、50GBSSD
- 状态：未安装JDK17、Nginx、MySQL、Redis
- Swap：未启用

我的判断：该配置适合把`Nginx+SpringBoot+MySQL8+Redis`部署在同一台轻量应用服务器上，用于简历演示足够。数据库端口不要公网开放。

##2.推荐架构

```text
浏览器
->Nginx:80
->Vue3用户端/
->Vue3管理端/admin/
->/api反向代理SpringBoot:8080
->SpringBoot连接MySQL:3306和Redis:6379
```

腾讯云轻量防火墙只需要放行：

- `22`：SSH
- `80`：HTTP
- `443`：HTTPS，后续配置域名证书时再用

不要放行`3306`、`6379`、`8080`。

##3.服务器初始化

```bash
sudo apt update
sudo apt install -y openjdk-17-jre nginx mysql-server redis-server unzip rsync
```

2GB内存建议开启2GBswap：

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

创建应用目录和运行用户：

```bash
sudo useradd -r -m -d /opt/petmeet -s /usr/sbin/nologin petmeet || true
sudo mkdir -p /opt/petmeet/uploads /etc/petmeet /var/www/petmeet/user /var/www/petmeet/admin
sudo chown -R petmeet:petmeet /opt/petmeet
```

##4.本机构建部署包

在Windows本机项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-deploy-artifacts.ps1
```

脚本会生成：

```text
outputs\petmeet-deploy.zip
```

上传到服务器：

```powershell
scp outputs\petmeet-deploy.zip ubuntu@124.220.91.57:/home/ubuntu/
```

##5.服务器解压部署包

```bash
sudo rm -rf /tmp/petmeet-deploy
sudo unzip /home/ubuntu/petmeet-deploy.zip -d /tmp/petmeet-deploy
sudo cp /tmp/petmeet-deploy/PetMeet-backend.jar /opt/petmeet/PetMeet-backend.jar
sudo rm -rf /opt/petmeet/sql
sudo cp -r /tmp/petmeet-deploy/sql /opt/petmeet/
sudo rsync -a --delete /tmp/petmeet-deploy/uploads/demo/ /opt/petmeet/uploads/demo/
sudo rsync -a --delete /tmp/petmeet-deploy/frontend/user/ /var/www/petmeet/user/
sudo rsync -a --delete /tmp/petmeet-deploy/frontend/admin/ /var/www/petmeet/admin/
sudo chown -R petmeet:petmeet /opt/petmeet
```

##6.初始化MySQL

首次部署可直接执行完整初始化脚本。注意：`ddl.sql`会重建业务表，只适合全新环境。

```bash
sudo mysql < /opt/petmeet/sql/ddl.sql
sudo mysql
```

进入MySQL后执行，密码请替换成你自己的强密码：

```sql
CREATE USER IF NOT EXISTS 'petmeet'@'localhost' IDENTIFIED BY '替换成强密码';
GRANT ALL PRIVILEGES ON petmeet.* TO 'petmeet'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

##7.配置后端环境变量

```bash
sudo cp /tmp/petmeet-deploy/deploy/petmeet.env.example /etc/petmeet/petmeet.env
sudo nano /etc/petmeet/petmeet.env
sudo chmod 600 /etc/petmeet/petmeet.env
```

至少修改：

- `PETMEET_DB_PASSWORD`：和上一步MySQL密码一致
- `PETMEET_ADMIN_PASSWORD`：管理端`admin`账号初始密码

Redis默认本机无密码即可：

```text
PETMEET_REDIS_HOST=127.0.0.1
PETMEET_REDIS_PORT=6379
PETMEET_REDIS_PASSWORD=
```

##8.启动SpringBoot后端

```bash
sudo cp /tmp/petmeet-deploy/deploy/systemd/petmeet-backend.service /etc/systemd/system/petmeet-backend.service
sudo systemctl daemon-reload
sudo systemctl enable --now petmeet-backend
sudo systemctl status petmeet-backend
```

查看日志：

```bash
sudo journalctl -u petmeet-backend -f
```

##9.启用Nginx

```bash
sudo cp /tmp/petmeet-deploy/deploy/nginx/petmeet.conf /etc/nginx/sites-available/petmeet.conf
sudo ln -sf /etc/nginx/sites-available/petmeet.conf /etc/nginx/sites-enabled/petmeet.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

访问地址：

- 用户端：`http://124.220.91.57/`
- 管理端：`http://124.220.91.57/admin/login`
- 接口文档：`http://124.220.91.57/api/doc.html`

##10.常用排错命令

```bash
sudo systemctl status petmeet-backend
sudo journalctl -u petmeet-backend -n 100 --no-pager
sudo systemctl status nginx
sudo nginx -t
sudo systemctl status mysql
sudo systemctl status redis-server
curl -I http://127.0.0.1:8080/doc.html
curl -I http://127.0.0.1/
```

##11.简历写法

`项目部署：将PetMeet全栈项目部署至腾讯云轻量应用服务器，使用Nginx托管Vue3用户端/管理端并反向代理SpringBootRESTAPI，通过systemd管理后端进程，集成MySQL8、Redis和上传文件持久化目录，完成公网可访问演示环境。`
