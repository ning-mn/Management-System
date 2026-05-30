# 会议室预定管理系统 (Meeting Room Booking System)

基于 Spring Boot 3 + MyBatis Plus + JWT + Vue 3 + Element Plus 的全栈会议室预定管理系统。

## 技术栈

### 后端
- **Spring Boot 3.2.0** - 应用框架
- **MyBatis Plus 3.5.5** - ORM 框架
- **JWT (jjwt 0.12.3)** - 身份认证
- **MySQL** - 数据库
- **Spring Security** - 安全框架
- **Lombok** - 代码简化
- **Hutool** - 工具类库

### 前端
- **Vue 3** - 前端框架
- **Vite 5** - 构建工具
- **Element Plus** - UI 组件库
- **Axios** - HTTP 请求库
- **Vue Router 4** - 路由管理

## 项目结构

```
Test/
├── sql/
│   └── init.sql                    # 数据库初始化脚本
├── backend/                        # 后端项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/meetingroom/
│       │   ├── MeetingRoomApplication.java
│       │   ├── config/             # 配置类
│       │   │   ├── CorsConfig.java
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── JwtUserDetails.java
│       │   │   ├── JwtUtil.java
│       │   │   ├── MyMetaObjectHandler.java
│       │   │   ├── MybatisPlusConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   └── SecurityContextHelper.java
│       │   ├── controller/         # 控制器
│       │   │   ├── BookingController.java
│       │   │   ├── MeetingRoomController.java
│       │   │   └── UserController.java
│       │   ├── dto/                # 数据传输对象
│       │   │   ├── BookingRequest.java
│       │   │   ├── LoginRequest.java
│       │   │   ├── RegisterRequest.java
│       │   │   ├── Result.java
│       │   │   ├── RoomRequest.java
│       │   │   └── StatusRequest.java
│       │   ├── entity/             # 实体类
│       │   │   ├── Booking.java
│       │   │   ├── MeetingRoom.java
│       │   │   └── User.java
│       │   ├── mapper/             # 数据访问层
│       │   │   ├── BookingMapper.java
│       │   │   ├── MeetingRoomMapper.java
│       │   │   └── UserMapper.java
│       │   └── service/            # 业务逻辑层
│       │       ├── BookingService.java
│       │       ├── MeetingRoomService.java
│       │       └── UserService.java
│       └── resources/
│           └── application.yml     # 应用配置
├── frontend/                       # 前端项目
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── api/                    # API 接口
│       │   ├── booking.js
│       │   ├── room.js
│       │   └── user.js
│       ├── router/
│       │   └── index.js
│       ├── utils/
│       │   └── axios.js            # Axios 配置与拦截器
│       └── views/                  # 页面组件
│           ├── Login.vue
│           ├── Register.vue
│           ├── RoomList.vue
│           ├── BookingForm.vue
│           ├── MyBookings.vue
│           └── Admin.vue
└── README.md
```

## 快速开始 - 本地开发

### 1. 准备工作

确保已安装以下环境：
- **Windows 开发环境**: JDK 17+, Maven 3.6+, Node.js 16+, npm/yarn
- **Linux 虚拟机 (192.168.200.131)**: JDK 17+, MySQL 8.0+, Node.js 16+, npm/yarn

### 2. 数据库初始化（在 Linux VM 上执行）

```bash
# SSH 登录到 Linux 虚拟机
ssh root@192.168.200.131

# 登录 MySQL
mysql -u root -p

# 执行初始化脚本（会创建数据库和表，并插入示例数据）
source /opt/meeting-room/sql/init.sql;
exit
```

### 3. 三种部署方案

#### 方案 A：全部在 Linux VM 上运行（推荐）
将整个项目部署到 Linux 虚拟机，详情请查看 `DEPLOY_LINUX.md`。

#### 方案 B：后端在 Linux，前端在 Windows 开发
```bash
# 在 Linux VM 上启动后端
cd /opt/meeting-room/backend
mvn spring-boot:run    # 后端在 192.168.200.131:8080

# 在 Windows 本地启动前端（已配置代理到 192.168.200.131:8080）
cd frontend
npm install
npm run dev            # 前端在 localhost:5173
```

#### 方案 C：全栈本地运行 + 连接 Linux 的 MySQL
编辑 `backend/src/main/resources/application.yml`，将数据库地址改为 Linux VM 的 IP：
```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.200.131:3306/meeting_room_db?...
```

然后在本地启动后端和前端即可。

### 4. 访问地址

| 服务 | 地址 |
|------|------|
| 后端 API | http://192.168.200.131:8080/api |
| 前端页面 | http://192.168.200.131:5173 |
| 默认管理员 | admin / admin123 |

> **详细部署到 Linux 虚拟机请参考 `DEPLOY_LINUX.md` 文件。**

## API 接口文档

### 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 用户模块

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|---------|
| POST | /api/user/register | 注册 | 否 |
| POST | /api/user/login | 登录 | 否 |
| GET | /api/user/info | 获取当前用户信息 | 是 |

### 会议室模块

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|---------|
| GET | /api/room/list | 分页查询会议室列表 | 否 |
| GET | /api/room/{id} | 查询单个会议室 | 否 |
| POST | /api/room | 新增会议室 | 是(管理员) |
| PUT | /api/room/{id} | 修改会议室 | 是(管理员) |
| DELETE | /api/room/{id} | 删除会议室 | 是(管理员) |
| GET | /api/room/{id}/bookings | 查询会议室某日预定 | 否 |

### 预定模块

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|---------|
| POST | /api/booking | 创建预定 | 是 |
| DELETE | /api/booking/{id} | 取消预定 | 是 |
| GET | /api/booking/my | 我的预定列表 | 是 |
| GET | /api/booking/all | 所有预定列表 | 是(管理员) |
| PUT | /api/booking/{id}/status | 审批预定 | 是(管理员) |

## 功能特性

- ✅ 用户注册/登录（JWT 认证）
- ✅ 会议室列表展示（支持设备、人数筛选）
- ✅ 会议室预定时间轴可视化
- ✅ 预定冲突检测（409 返回）
- ✅ 预定时间选择（08:00~20:00，30分钟间隔）
- ✅ 我的预定管理与取消
- ✅ 管理员后台（会议室CRUD、预定审批）
- ✅ 全局异常处理与权限控制
- ✅ 前端路由守卫与角色菜单动态展示
- ✅ Axios 拦截器统一处理 Token 和错误