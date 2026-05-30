# 会议室预定管理系统 — 软件设计文档

> 版本：v1.0
> 日期：2026年5月27日
> 技术栈：Spring Boot 3 + MyBatis Plus + JWT + Vue 3 + Element Plus + MySQL

---

## 目录

1. [需求分析](#1-需求分析)
2. [系统架构设计](#2-系统架构设计)
3. [数据库设计（含ER图）](#3-数据库设计含er图)
4. [模块详细设计](#4-模块详细设计)
5. [接口设计](#5-接口设计)
6. [安全设计](#6-安全设计)
7. [前端设计](#7-前端设计)

---

## 1. 需求分析

### 1.1 项目背景

企业内部会议室资源有限，经常出现以下问题：
- 会议室使用情况不透明，员工难以找到空闲会议室
- 会议室被预约后无人使用，造成资源浪费
- 管理员无法有效管理会议室设备维护和预定审批

因此需要开发一套「会议室预定管理系统」，实现会议室在线预定、审批、管理的全流程数字化。

### 1.2 功能需求

#### 1.2.1 用户模块
| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| U-01 | 用户注册 | 员工通过用户名、密码、真实姓名注册账号 | P0 |
| U-02 | 用户登录 | 用户名+密码登录，返回JWT Token | P0 |
| U-03 | 查看个人信息 | 已登录用户查看自己的账号信息 | P0 |

#### 1.2.2 会议室模块
| 编号 | 功能 | 描述 | 角色 | 优先级 |
|------|------|------|------|--------|
| R-01 | 分页查询会议室列表 | 展示所有可用会议室，支持容量/设备筛选 | 所有用户 | P0 |
| R-02 | 查看会议室详情 | 查看单个会议室的详细信息 | 所有用户 | P0 |
| R-03 | 查看会议室预定时间轴 | 查看指定日期各会议室的已占用时间段 | 所有用户 | P0 |
| R-04 | 新增会议室 | 添加新的会议室（名称、容量、设备、状态） | 管理员 | P0 |
| R-05 | 修改会议室 | 编辑会议室信息 | 管理员 | P0 |
| R-06 | 删除会议室 | 删除会议室，需检查是否有未来预定 | 管理员 | P0 |

#### 1.2.3 预定模块
| 编号 | 功能 | 描述 | 角色 | 优先级 |
|------|------|------|------|--------|
| B-01 | 创建预定 | 选择会议室、日期、时间段，提交预定，需冲突检测 | 已登录用户 | P0 |
| B-02 | 取消预定 | 自己取消/管理员取消任意预定，仅限未开始的预定 | 用户/管理员 | P0 |
| B-03 | 查看我的预定 | 查看当前用户的所有预定记录，支持状态筛选 | 已登录用户 | P0 |
| B-04 | 查看所有预定 | 管理员查看系统所有预定，支持多条件筛选 | 管理员 | P0 |
| B-05 | 审批预定 | 管理员对 pending 状态的预定通过/拒绝 | 管理员 | P0 |

### 1.3 非功能需求

| 编号 | 需求 | 说明 |
|------|------|------|
| N-01 | 安全性 | 密码BCrypt加密存储，JWT无状态认证 |
| N-02 | 并发控制 | 同会议室同时间段不能同时被预定（冲突检测） |
| N-03 | 响应时间 | API响应时间<500ms |
| N-04 | 可用性 | 系统7x24小时运行，支持定期维护 |
| N-05 | 可维护性 | 前后端分离，模块化设计，统一异常处理 |

### 1.4 用例图

```text
┌─────────────────────────────────────────────────────────────────┐
│                    会议室预定管理系统                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────┐            ┌──────────────┐                    │
│  │  未登录   │            │   普通员工   │                    │
│  │  用户     │            │  (employee)  │                    │
│  └─────┬─────┘            └──────┬───────┘                    │
│        │                        │                             │
│        ├── 注册 ────────────────┤                             │
│        │                        ├── 预定会议室                 │
│        ├── 查看会议室列表 ──────┤                             │
│        │                        ├── 查看我的预定               │
│        └── 查看已占用时间 ──────┤                             │
│                                 ├── 取消自己的预定             │
│                                 │                             │
│                        ┌───────┴────────┐                    │
│                        │   管理员       │                    │
│                        │   (admin)      │                    │
│                        └───────┬────────┘                    │
│                                │                             │
│                                ├── 会议室增删改               │
│                                ├── 审批预定（通过/拒绝）      │
│                                ├── 查看所有预定               │
│                                └── 取消任意预定               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 系统架构设计

### 2.1 技术架构

```text
┌─────────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client)                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Vue 3 + Vite + Element Plus (SPA)                        │   │
│  │  Axios (HTTP 请求) + Vue Router (路由)                     │   │
│  │  localStorage (JWT 存储)                                    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │ HTTP (JSON)                          │
│                             │ Authorization: Bearer <token>        │
├─────────────────────────────┼───────────────────────────────────────┤
│                        服务层 (Server)                             │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │          Nginx (反向代理，可选)                              │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                     │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │  Spring Boot 3 + Spring Security                            │   │
│  │                                                            │   │
│  │  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │   │
│  │  │Controller│ │  Service  │ │  Mapper  │ │   Config     │   │   │
│  │  │ 层      │ │  层      │ │  层     │ │  JWT/CORS    │   │   │
│  │  └────┬────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘   │   │
│  │       │           │            │              │           │   │
│  │       └───────────┴────────────┴──────────────┘           │   │
│  │                     MyBatis Plus (ORM)                    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                     │
├─────────────────────────────┼───────────────────────────────────────┤
│                      数据层 (Data)                                │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │  MySQL 8.0                                                  │   │
│  │  数据库: meeting_room_db                                    │   │
│  │  表: user / meeting_room / booking                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 后端包结构

```text
com.meetingroom
├── MeetingRoomApplication.java      # Spring Boot 启动类
├── config/                          # 配置层
│   ├── SecurityConfig.java          # Spring Security 配置
│   ├── JwtUtil.java                 # JWT 生成/解析工具
│   ├── JwtAuthenticationFilter.java # JWT 认证过滤器
│   ├── JwtUserDetails.java          # 当前用户对象
│   ├── SecurityContextHelper.java   # 安全上下文工具类
│   ├── CorsConfig.java              # 跨域配置
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   ├── MybatisPlusConfig.java       # MyBatis Plus 插件配置
│   └── MyMetaObjectHandler.java     # 自动填充处理器
├── controller/                      # 控制层（接收请求）
│   ├── UserController.java          # 用户模块 API
│   ├── MeetingRoomController.java   # 会议室模块 API
│   └── BookingController.java       # 预定模块 API
├── service/                         # 业务逻辑层
│   ├── UserService.java             # 用户注册/登录业务
│   ├── MeetingRoomService.java      # 会议室CRUD业务
│   └── BookingService.java          # 预定/冲突检测/审批业务
├── mapper/                          # 数据访问层（接口）
│   ├── UserMapper.java
│   ├── MeetingRoomMapper.java
│   └── BookingMapper.java           # 含冲突检测SQL
├── entity/                          # 实体类（对应数据库表）
│   ├── User.java
│   ├── MeetingRoom.java
│   └── Booking.java
└── dto/                             # 数据传输对象
    ├── Result.java                  # 统一返回格式 {code, message, data}
    ├── LoginRequest.java            # 登录请求
    ├── RegisterRequest.java         # 注册请求
    ├── RoomRequest.java             # 会议室请求
    ├── BookingRequest.java          # 预定请求
    └── StatusRequest.java           # 状态更新请求
```

### 2.3 前端组件树

```text
App.vue
├── 导航栏 (el-menu)
│   ├── 会议室列表 (/)
│   ├── 我的预定 (/my-bookings)
│   └── 管理员后台 (/admin, 仅admin)
├── Login.vue (/login)
├── Register.vue (/register)
├── RoomList.vue (/)                # 首页 - 会议室列表
│   ├── 搜索栏 (关键字/容量/日期筛选)
│   ├── 会议室卡片列表 (el-card)
│   │   └── 时间轴 (已占用时间段显示)
│   └── 分页 (el-pagination)
├── BookingForm.vue (/booking)      # 预定表单
│   ├── 日期选择器 (el-date-picker)
│   ├── 时间选择器 (el-select, 08:00~20:00)
│   └── 备注输入
├── MyBookings.vue (/my-bookings)   # 我的预定
│   ├── 状态筛选 (el-select)
│   ├── 预定表格 (el-table)
│   └── 取消按钮
└── Admin.vue (/admin)              # 管理员后台
    ├── Tab1: 会议室管理
    │   ├── 会议室表格 (el-table)
    │   └── 新增/编辑弹窗 (el-dialog)
    └── Tab2: 预定管理
        ├── 多条件筛选
        ├── 预定表格 (el-table)
        ├── 通过/拒绝/取消按钮
        └── 分页 (el-pagination)
```

---

## 3. 数据库设计（含ER图）

### 3.1 ER 图（实体-关系图）

```text
┌─────────────────────────────┐
│           User              │
│  ─────────────────────────  │
│  PK  id: INT               │──────┐
│      username: VARCHAR(50)  │      │ 1
│      password: VARCHAR(255) │      │
│      role: ENUM             │      │
│      real_name: VARCHAR(50) │      │
│      created_at: TIMESTAMP  │      │
└─────────────────────────────┘      │
                                     │ 拥有（1:N）
                                     │
                                     │
┌─────────────────────────────┐      │
│         Booking             │      │
│  ─────────────────────────  │      │
│  PK  id: INT               │      │
│  FK  user_id: INT ─────────┘      │
│  FK  room_id: INT ───────────┐    │
│      start_time: DATETIME    │    │
│      end_time: DATETIME      │    │
│      status: ENUM            │    │
│      remark: TEXT            │    │
│      created_at: TIMESTAMP   │    │
└─────────────────────────────┘    │
                                   │
                                   │ N
                                   │
┌─────────────────────────────┐    │
│       MeetingRoom           │    │
│  ─────────────────────────  │    │
│  PK  id: INT               │────┘
│      name: VARCHAR(100)    │
│      capacity: INT         │
│      equipment: VARCHAR(255)│
│      status: ENUM          │
└─────────────────────────────┘

关系说明：
- User 1 ──── N Booking （一个用户可以有多个预定）
- MeetingRoom 1 ──── N Booking （一个会议室可以有多个预定）
- Booking 是 User 和 MeetingRoom 之间的关联表
```

### 3.2 逻辑模型（关系模式，3NF）

```
User (<u>id</u>, username, password, role, real_name, created_at)
  - UK: username

MeetingRoom (<u>id</u>, name, capacity, equipment, status)

Booking (<u>id</u>, user_id#, room_id#, start_time, end_time, status, remark, created_at)
  - FK: (user_id) → User(id)
  - FK: (room_id) → MeetingRoom(id)
  - 业务约束: (room_id, start_time, end_time) 不能有冲突的 status in ('pending','confirmed')
```

### 3.3 物理模型（DDL）

#### 3.3.1 用户表 (user)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt加密密码 |
| role | ENUM('employee','admin') | NOT NULL, DEFAULT 'employee' | 角色 |
| real_name | VARCHAR(50) | NULL | 真实姓名 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**: PRIMARY KEY (id), UNIQUE KEY uk_username (username)

#### 3.3.2 会议室表 (meeting_room)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 会议室ID |
| name | VARCHAR(100) | NOT NULL | 会议室名称 |
| capacity | INT | NOT NULL | 容纳人数 |
| equipment | VARCHAR(255) | NULL | 设备描述 |
| status | ENUM('available','maintenance') | NOT NULL, DEFAULT 'available' | 状态 |

**索引**: PRIMARY KEY (id)

#### 3.3.3 预定表 (booking)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 预定ID |
| user_id | INT | NOT NULL, FK → user(id) | 预定用户 |
| room_id | INT | NOT NULL, FK → meeting_room(id) | 会议室 |
| start_time | DATETIME | NOT NULL | 开始时间 |
| end_time | DATETIME | NOT NULL | 结束时间 |
| status | ENUM('pending','confirmed','cancelled','rejected') | NOT NULL, DEFAULT 'pending' | 状态 |
| remark | TEXT | NULL | 备注 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**: 
- PRIMARY KEY (id)
- KEY idx_user_id (user_id) — 加速按用户查询
- KEY idx_room_id (room_id) — 加速按会议室查询
- KEY idx_status (status) — 加速按状态筛选
- KEY idx_start_time (start_time) — 加速时间范围查询
- KEY idx_end_time (end_time) — 加速时间范围查询

**外键约束**:
- FK: user_id → user(id) ON DELETE CASCADE
- FK: room_id → meeting_room(id) ON DELETE CASCADE

### 3.4 业务约束说明

| 约束 | 实现方式 |
|------|---------|
| 同一个会议室同一时间段不能有两条 status 为 pending 或 confirmed 的预定 | 应用层SQL检测：`SELECT COUNT(*) FROM booking WHERE room_id=? AND status IN ('pending','confirmed') AND start_time < ? AND end_time > ?` |
| start_time < end_time | 前端 + 后端双重校验 |
| 不允许预定过去的时间 | 前端日期禁用 + 后端 `isBefore(LocalDateTime.now())` 校验 |
| 只能取消未开始的预定 | 后端 `start_time.isBefore(now)` 校验 |

### 3.5 状态流转图

```text
创建预定
    │
    ▼
┌─────────┐    管理员通过    ┌───────────┐
│ Pending │───────────────→│ Confirmed │
│ 待审批   │                │ 已确认    │
└────┬────┘                └───────────┘
     │                           │
     │ 管理员拒绝                │ 用户/管理员取消
     ▼                           ▼
┌─────────┐              ┌───────────┐
│ Rejected│              │ Cancelled │
│ 已拒绝  │              │ 已取消    │
└─────────┘              └───────────┘

     ┌────────────────────────────────────┐
     │ 只有 Pending 状态可被审批          │
     │    → Confirmed 或 Rejected        │
     │ 只有 Pending/Confirmed 可被取消    │
     │    → Cancelled                    │
     │ 一旦 Cancelled/Rejected 不可再修改 │
     └────────────────────────────────────┘
```

---

## 4. 模块详细设计

### 4.1 用户模块

#### 4.1.1 注册流程

```text
客户端 POST /api/user/register         服务端
  │                                      │
  │ 请求体: {username, password,         │
  │         realName}                    │
  │───────────────────────────────────→  │
  │                                      │
  │                                      ├─ 1. 校验参数是否为空
  │                                      ├─ 2. 查询 username 是否已存在
  │                                      │    SELECT COUNT(*) FROM user
  │                                      │    WHERE username = ?
  │                                      │
  │                                      ├─ 3. 已存在 → 返回 400 "用户名已存在"
  │                                      │
  │                                      ├─ 4. 未存在 → BCrypt.encode(password)
  │                                      ├─ 5. INSERT INTO user
  │                                      │      (username, password, role, real_name)
  │                                      │
  │  ←───────────────────────────────────┼─ 6. 返回 {code:200, message:"注册成功"}
```

#### 4.1.2 登录流程

```text
客户端 POST /api/user/login             服务端
  │                                      │
  │ 请求体: {username, password}         │
  │───────────────────────────────────→  │
  │                                      │
  │                                      ├─ 1. 根据 username 查询用户
  │                                      │    SELECT * FROM user WHERE username = ?
  │                                      │
  │                                      ├─ 2. 用户不存在 → 400 "用户名或密码错误"
  │                                      │
  │                                      ├─ 3. passwordEncoder.matches(password, user.password)
  │                                      │    BCrypt 校验密码
  │                                      │
  │                                      ├─ 4. 密码错误 → 400 "用户名或密码错误"
  │                                      │
  │                                      ├─ 5. JWTUtil.generateToken(userId, username, role)
  │                                      │    载荷: {sub: userId, username, role}
  │                                      │    密钥: HMAC-SHA256(secret)
  │                                      │    过期: 24小时
  │                                      │
  │  ←───────────────────────────────────┼─ 6. 返回 {code:200, data:{token, userId,
  │                                              username, role, realName}}
```

### 4.2 预定模块 — 核心业务逻辑

#### 4.2.1 冲突检测算法

```text
输入: roomId, startTime, endTime
输出: 是否有冲突 (true/false)

算法:
  1. 查询 booking 表中符合以下条件的记录数:
     - room_id = roomId
     - status IN ('pending', 'confirmed')
     - start_time < endTime
     - end_time > startTime

  2. 如果 count > 0 → 冲突 (返回 true)
     如果 count = 0 → 无冲突 (返回 false)

  原理: 两个时间段 [A, B] 和 [C, D] 重叠的条件是:
        A < D 且 C < B
```

#### 4.2.2 创建预定完整流程

```text
客户端 POST /api/booking                服务端
  │                                      │
  │ 请求头: Authorization: Bearer <jwt>  │
  │ 请求体: {roomId, startTime,          │
  │         endTime, remark}             │
  │───────────────────────────────────→  │
  │                                      │
  │                                      ├─ 1. JWT认证 → 获取 userId
  │                                      │
  │                                      ├─ 2. 检查会议室是否存在
  │                                      │    SELECT * FROM meeting_room WHERE id=?
  │                                      │
  │                                      ├─ 3. 校验 startTime < endTime
  │                                      │
  │                                      ├─ 4. 校验 startTime > now (不能预定过去)
  │                                      │
  │                                      ├─ 5. ⭐冲突检测⭐
  │                                      │    SELECT COUNT(*) FROM booking
  │                                      │    WHERE room_id=? AND status IN ('pending','confirmed')
  │                                      │    AND start_time < ? AND end_time > ?
  │                                      │
  │                                      ├─ 6. 有冲突 → 返回 409 "该时间段已被占用"
  │                                      │
  │                                      ├─ 7. 无冲突 → INSERT INTO booking
  │                                      │      (user_id, room_id, start_time, end_time, status, remark)
  │                                      │      VALUES (?, ?, ?, ?, 'pending', ?)
  │                                      │
  │  ←───────────────────────────────────┼─ 8. 返回 {code:200, message:"预定成功"}
```

---

## 5. 接口设计

### 5.1 通用约定

**基础路径**: `http://192.168.200.131:8080/api`

**统一返回格式**:
```json
{
    "code": 200,        // 200成功, 400参数错误, 401未认证, 403无权限, 404不存在, 409冲突, 500服务器错误
    "message": "success",
    "data": {}          // 具体数据
}
```

**认证方式**: JWT Bearer Token
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 5.2 接口详细设计

#### 5.2.1 用户接口

| 接口 | 方法 | URL | 请求参数 | 响应 | 说明 |
|------|------|-----|----------|------|------|
| 注册 | POST | /api/user/register | `{"username":"","password":"","realName":""}` | code:200 | 需校验username唯一 |
| 登录 | POST | /api/user/login | `{"username":"","password":""}` | code:200, data: {...token...} | 返回JWT |
| 用户信息 | GET | /api/user/info | Header: Bearer Token | code:200, data: User | 密码字段置空 |

#### 5.2.2 会议室接口

| 接口 | 方法 | URL | 请求参数 | 响应 | 权限 |
|------|------|-----|----------|------|------|
| 分页列表 | GET | /api/room/list | page, pageSize, capacityMin, equipment | {records, total, page, pageSize} | 公开 |
| 详情 | GET | /api/room/{id} | Path: id | MeetingRoom | 公开 |
| 新增 | POST | /api/room | `{"name","capacity","equipment","status"}` | code:200 | Admin |
| 修改 | PUT | /api/room/{id} | Path: id + Body | code:200 | Admin |
| 删除 | DELETE | /api/room/{id} | Path: id | code:200 | Admin |
| 预定时间轴 | GET | /api/room/{id}/bookings | Path: id, Query: date | Booking[] | 公开 |

#### 5.2.3 预定接口

| 接口 | 方法 | URL | 请求参数 | 响应 | 权限 |
|------|------|-----|----------|------|------|
| 创建预定 | POST | /api/booking | `{"roomId","startTime","endTime","remark"}` | code:200/409 | 登录 |
| 取消预定 | DELETE | /api/booking/{id} | Path: id | code:200 | 本人/Admin |
| 我的预定 | GET | /api/booking/my | page, pageSize, status | 分页数据 | 登录 |
| 所有预定 | GET | /api/booking/all | page, pageSize, roomId, status, startTimeBegin, startTimeEnd | 分页数据 | Admin |
| 审批预定 | PUT | /api/booking/{id}/status | Path: id, Body: `{"status":"confirmed"/"rejected"}` | code:200 | Admin |

---

## 6. 安全设计

### 6.1 JWT 认证流程

```text
┌──────────┐          ┌──────────┐          ┌──────────┐
│  Client  │          │  Filter  │          │  Server  │
└────┬─────┘          └────┬─────┘          └────┬─────┘
     │                     │                     │
     │ ① POST /api/login   │                     │
     │ {username,password} │                     │
     │─────────────────────┼─────────────────────│──→ 校验凭证
     │                     │                     │
     │←────────────────────┼─────────────────────│── 生成JWT
     │ {token}             │                     │
     │                     │                     │
     │ ② GET /api/xxx      │                     │
     │ Authorization:      │                     │
     │ Bearer <token>      │                     │
     │─────────────────────│──→                  │
     │                     │  ③ 提取Token        │
     │                     │  ④ 校验签名+过期    │
     │                     │  ⑤ 解析claims       │
     │                     │  ⑥ 设置Authentication│
     │                     │─────────────────────│──→
     │                     │                     │  ⑦ 处理请求
     │←────────────────────│─────────────────────│── 返回数据
```

### 6.2 权限矩阵

| 接口 | 未登录 | Employee | Admin |
|------|--------|----------|-------|
| POST /api/user/register | ✅ | ✅ | ✅ |
| POST /api/user/login | ✅ | ✅ | ✅ |
| GET /api/user/info | ❌ 401 | ✅ | ✅ |
| GET /api/room/list | ✅ | ✅ | ✅ |
| POST/PUT/DELETE /api/room/* | ❌ 401 | ❌ 403 | ✅ |
| POST /api/booking | ❌ 401 | ✅ | ✅ |
| DELETE /api/booking/{id} | ❌ 401 | ✅ (本人) | ✅ (全部) |
| GET /api/booking/my | ❌ 401 | ✅ | ✅ |
| GET /api/booking/all | ❌ 401 | ❌ 403 | ✅ |
| PUT /api/booking/{id}/status | ❌ 401 | ❌ 403 | ✅ |

### 6.3 密码安全

- 使用 BCrypt 算法对密码进行加密存储
- BCrypt 自动加盐，抵抗彩虹表攻击
- 密码字段不返回给前端（`user.setPassword(null)`）

---

## 7. 前端设计

### 7.1 路由设计

| 路径 | 组件 | 是否需要登录 | 是否需要管理员 |
|------|------|-------------|--------------|
| /login | Login.vue | ❌ | ❌ |
| /register | Register.vue | ❌ | ❌ |
| / | RoomList.vue | ✅ | ❌ |
| /booking | BookingForm.vue | ✅ | ❌ |
| /my-bookings | MyBookings.vue | ✅ | ❌ |
| /admin | Admin.vue | ✅ | ✅ |

### 7.2 状态管理

由于该项目规模较小，前端使用 `localStorage` 管理全局状态：

```text
localStorage.setItem('token', '...')    # JWT Token
localStorage.setItem('user', JSON.stringify({
    userId: 1,
    username: 'admin',
    role: 'admin',
    realName: '系统管理员'
}))
```

### 7.3 Axios 拦截器设计

```
请求拦截器(Request Interceptor)
  ┌─────────────────────────────────────────┐
  │ 从 localStorage 获取 token              │
  │ 如果 token 存在                         │
  │   → 设置 header: Authorization: Bearer  │
  └─────────────────────────────────────────┘

响应拦截器(Response Interceptor)
  ┌─────────────────────────────────────────┐
  │ 如果 code = 200  → 返回 data            │
  │ 如果 status = 401 → 清除 token          │
  │                     跳转 /login          │
  │ 如果 status = 403 → 提示"权限不足"      │
  │ 如果 status = 409 → 提示"时间冲突"      │
  │ 其他错误 → 提示具体错误信息              │
  └─────────────────────────────────────────┘
```

### 7.4 页面交互流程

```text
用户打开系统
    │
    ▼
┌──────────┐    有 Token?    ┌───────────┐
│ 登录页   │──────────────→  │ 会议室列表 │
│ /login   │←── 无 Token ─── │ 首页 /    │
└────┬─────┘                └─────┬─────┘
     │ 注册                        │ 选择日期
     ▼                             │ 点击"预定"
┌──────────┐                      │
│ 注册页   │                      ▼
│ /register│               ┌──────────────┐
│ 注册成功  │              │ 预定表单页    │
│ → /login  │              │ /booking     │
└──────────┘              └──────┬───────┘
                                  │ 提交成功
                                  ▼
                          ┌──────────────┐
                          │ 我的预定页    │
                          │ /my-bookings │
                          └──────┬───────┘
                                  │
                            ┌─────┴─────┐
                            │ 管理员可进入 │
                            ▼
                      ┌──────────────┐
                      │ 管理员后台    │
                      │ /admin       │
                      │ 2个Tab       │
                      └──────────────┘
```

---

## 附录 A：技术选型理由

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| Spring Boot | 3.2.0 | 最新的稳定版本，自动配置、内嵌Tomcat、丰富的生态 |
| MyBatis Plus | 3.5.5 | 简化MyBatis开发，代码生成器、分页插件、Lambda查询 |
| JWT (jjwt) | 0.12.3 | 无状态认证，适合前后端分离架构 |
| Vue 3 | 3.4.0 | 组合式API(Composition API)更灵活，性能更好 |
| Element Plus | 2.4.4 | Vue 3版本的Element UI，丰富的企业级组件 |
| Vite | 5.0.10 | 极速冷启动，HMR热更新，构建速度快 |
| Axios | 1.6.2 | Promise-based HTTP客户端，拦截器机制成熟 |
| MySQL | 8.0+ | 成熟稳定，广泛使用，支持事务和ACID |
| BCrypt | Spring Security自带 | 密码加密标准，自动加盐，抗彩虹表 |

## 附录 B：部署架构

```text
┌──────────────────────────────────────────────────┐
│  Linux 虚拟机 (192.168.200.131)                 │
│                                                  │
│  ┌─────────────────────┐                        │
│  │  前端 (Vite Dev)    │ 端口 5173              │
│  │  自动代理 /api →    │                        │
│  └─────────┬───────────┘                        │
│            │                                     │
│            ▼                                     │
│  ┌─────────────────────┐                        │
│  │  后端 (Spring Boot) │ 端口 8080              │
│  └─────────┬───────────┘                        │
│            │                                     │
│            ▼                                     │
│  ┌─────────────────────┐                        │
│  │  MySQL 8.0          │ 端口 3306              │
│  │  meeting_room_db    │                        │
│  └─────────────────────┘                        │
└──────────────────────────────────────────────────┘