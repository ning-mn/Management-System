# Linux 虚拟机部署指南

将「会议室预定管理系统」部署到 Linux 虚拟机 (192.168.200.131)

---

## 方案一：全量部署到 Linux VM（推荐）

将 **MySQL + 后端 + 前端** 全部部署到 Linux 虚拟机上。

### 1. 准备工作

确保 Linux 虚拟机已安装：
```bash
# JDK 17
java -version

# Maven 3.6+
mvn -version

# MySQL 8.0+
mysql --version

# Node.js 16+
node -v
npm -v

# Git
git --version
```

### 2. 将项目传到 Linux 虚拟机

**方法一：使用 Git（推荐）**
在 Linux 虚拟机上执行：
```bash
# 如果代码在 Git 仓库中
git clone <仓库地址>
```

**方法二：使用 SCP 从 Windows 拷贝**
在 Windows 终端执行（将项目从Windows拷贝到Linux虚拟机）：
```bash
# 将整个 Test 目录拷贝到 Linux 虚拟机
scp -r C:\Users\86158\Desktop\network2321\network2321\服务器虚拟化应用部署\Test root@192.168.200.131:/opt/meeting-room/
```

### 3. 初始化数据库

登录 Linux 虚拟机并执行：
```bash
ssh root@192.168.200.131

# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source /opt/meeting-room/sql/init.sql;
exit
```

### 4. 将项目放到 Linux 上的目录结构

```bash
# 创建项目目录
mkdir -p /opt/meeting-room
cd /opt/meeting-room
```

把 Test 目录下的内容传到 `/opt/meeting-room/`。

### 5. 配置后端 (`application.yml`)

编辑 `/opt/meeting-room/backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080
  address: 0.0.0.0

spring:
  datasource:
    # 如果 MySQL 也在同一台 Linux 虚拟机上，用 localhost
    # 如果 MySQL 在另一台机器，改为对应的 IP
    url: jdbc:mysql://localhost:3306/meeting_room_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456    # 改为你的 MySQL 密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 6. 配置前端 (`vite.config.js`)

编辑 `/opt/meeting-room/frontend/vite.config.js`：

```javascript
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',    # 允许所有网络接口访问
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://192.168.200.131:8080',  # 后端地址
        changeOrigin: true
      }
    }
  }
})
```

### 7. 启动后端服务

```bash
cd /opt/meeting-room/backend
mvn clean install -DskipTests
nohup mvn spring-boot:run > /opt/meeting-room/backend.log 2>&1 &

# 查看启动日志
tail -f /opt/meeting-room/backend.log
```

### 8. 启动前端服务

```bash
cd /opt/meeting-room/frontend
npm install
nohup npm run dev > /opt/meeting-room/frontend.log 2>&1 &

# 查看启动日志
tail -f /opt/meeting-room/frontend.log
```

### 9. 防火墙设置

确保 Linux 防火墙开放端口：
```bash
# 开放 8080（后端）和 5173（前端）端口
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --add-port=5173/tcp --permanent
firewall-cmd --reload

# 或者直接关闭防火墙（不推荐）
systemctl stop firewalld
```

### 10. 访问系统

打开浏览器访问：**http://192.168.200.131:5173**

---

## 方案二：仅数据库在 Linux，后端/前端在 Windows

### 1. 修改后端配置

编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.200.131:3306/meeting_room_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    # 把 localhost 改为 192.168.200.131
    username: root
    password: 123456
```

### 2. 在 Linux 上初始化数据库

```bash
# 从 Windows 复制 SQL 到 Linux
scp C:\Users\86158\Desktop\network2321\network2321\服务器虚拟化应用部署\Test\sql\init.sql root@192.168.200.131:/tmp/

# SSH 登录并执行
ssh root@192.168.200.131
mysql -u root -p < /tmp/init.sql
```

### 3. 启动后端（在 Windows）

```bash
# 在 Windows 的 backend 目录下
cd backend
mvn spring-boot:run
# 后端监听 0.0.0.0:8080
```

### 4. 启动前端（在 Windows）

```bash
# 在 Windows 的 frontend 目录下
cd frontend
npm run dev
# 前端监听 0.0.0.0:5173
```

访问：**http://192.168.200.131:5173**（从 Linux 或其他机器访问）
访问：**http://localhost:5173**（在本机访问）

---

## 方案三：后端在 Linux，前端在 Windows 开发

### 1. Linux 上启动后端

```bash
cd /opt/meeting-room/backend
mvn spring-boot:run
# 后端在 0.0.0.0:8080
```

### 2. Windows 上修改前端代理

编辑 `frontend/vite.config.js`：
```javascript
proxy: {
  '/api': {
    target: 'http://192.168.200.131:8080',  # 指向 Linux 上的后端
    changeOrigin: true
  }
}
```

### 3. Windows 上启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：**http://localhost:5173**

---

## 注意事项

| 问题 | 解决方案 |
|------|---------|
| Linux 端口被占用 | `lsof -i:8080` 查看进程，`kill -9 PID` 结束进程 |
| 连接不上 MySQL | 检查 MySQL 是否允许远程连接：`grant all on *.* to 'root'@'%' identified by '密码';` |
| 前端报 502 | 检查后端是否启动成功，检查 `vite.config.js` 的 proxy target 是否正确 |
| 后端报数据库连接失败 | 检查 `application.yml` 中的数据库地址、用户名、密码 |
| SELinux 阻止访问 | `setenforce 0` 临时关闭，或配置 SELinux 规则 |

---

## 验证部署

```bash
# 在 Linux 虚拟机上验证后端是否启动
curl http://192.168.200.131:8080/api/room/list

# 会返回 JSON 数据，例如：
# {"code":200,"message":"success","data":{"records":[...],"total":5}}