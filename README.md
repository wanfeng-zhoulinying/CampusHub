# CampusHub

## 项目简介

CampusHub 是一个面向校园场景的场地预约与活动报名平台，当前版本采用前后端分离的单体架构实现。

项目定位：

- 学生用户可以查看场地、预约场地、报名活动、签到核销
- 管理端后续可扩展场地管理、活动审核、用户管理等能力
- 当前阶段重点完成核心业务闭环与 JWT 登录态接入

一句话概括：

> 面向学生和社团的场地预约、活动报名、签到核销一体化平台。

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- MyBatis
- MySQL 8
- Redis
- Lombok
- JWT

### 前端

- `campushub-user`：Vue 3 + Vite
- `campushub-admin`：Vue 3 + Vite

### 工具

- IntelliJ IDEA
- VSCode
- Apifox
- Git / GitHub

## 当前已完成功能

### 用户模块

- 用户注册
- 用户登录
- JWT 鉴权
- 获取当前登录用户信息

### 场地模块

- 场地列表查询
- 场地详情查询
- 场地时间段查询

### 预约模块

- 创建预约
- 查询我的预约
- 取消预约
- 场地预约核销

### 活动模块

- 活动列表查询
- 活动详情查询
- 活动报名
- 查询我的报名
- 取消报名
- 活动签到

## 项目结构

```text
CampusHub
├─ campushub-server
├─ campushub-user
└─ campushub-admin
```

### 后端目录结构

```text
campushub-server
└─ src/main/java/com/campushub
   ├─ common
   ├─ config
   ├─ constant
   ├─ controller
   │  ├─ activity
   │  ├─ booking
   │  ├─ user
   │  └─ venue
   ├─ dto
   ├─ entity
   ├─ exception
   ├─ interceptor
   ├─ mapper
   ├─ service
   ├─ utils
   └─ vo
```

## 核心业务说明

### 角色

- 学生用户：浏览场地、预约场地、报名活动、查看个人记录
- 管理员：当前版本未正式开发管理接口，后续作为二期扩展

### 预约业务

- 用户先查看场地与时间段
- 选择时间段后创建预约
- 可查询自己的预约记录
- 可取消预约或完成核销

### 活动业务

- 用户浏览活动列表与活动详情
- 选择活动后完成报名
- 可查询自己的报名记录
- 可取消报名或完成签到

## 当前接口分组

Apifox 已建议按以下分组维护：

- User
- Venue
- Booking
- Activity

JWT 使用方式：

1. 调用登录接口获取 token
2. 需要登录的接口在请求头中携带：

```http
Authorization: Bearer 你的token
```

## 本地启动说明

### 1. 启动数据库与 Redis

- 准备 MySQL 8
- 准备 Redis
- 按本地环境修改 `campushub-server/src/main/resources/application.yml`

### 2. 初始化数据库

按项目中的 SQL 脚本初始化表结构与测试数据。

当前已用到的测试数据脚本包括：

- `sql/venue`
- `sql/booking`
- `sql/activity`
- `sql/user`

### 3. 启动后端

在 IDEA 中运行：

`com.campushub.CampusHubApplication`

默认访问地址：

- `http://localhost:8080`

### 4. 启动前端用户端

```bash
cd campushub-user
npm install
npm run dev
```

### 5. 启动前端管理端

```bash
cd campushub-admin
npm install
npm run dev
```

## 当前测试建议

### 用户测试

- 注册
- 登录
- `/user/me`

### 场地测试

- 场地列表
- 场地详情
- 场地时间段

### 预约测试

- 创建预约
- 我的预约
- 取消预约
- 核销预约

### 活动测试

- 活动列表
- 活动详情
- 活动报名
- 我的报名
- 取消报名
- 活动签到

## 当前版本特点

当前版本属于第一版核心闭环，重点放在：

- 完成完整业务链路
- 建立清晰的分层结构
- 接入 JWT 登录态
- 能通过 Apifox 完整演示核心接口

## 当前已知边界

以下内容属于后续优化方向，当前版本暂未完全增强：

- 活动报名时间窗口校验
- 活动开始后不可取消报名
- 已签到记录不可取消
- 活动结束后的签到与取消约束
- 活动取消后重新报名逻辑进一步优化
- Redis 缓存优化
- 高并发保护与限流
- 消息通知模块
- 后台管理模块

## 后续规划

建议按以下顺序继续迭代：

1. 完善 README、接口文档与测试说明
2. 做一轮代码复查与命名整理
3. 开发 Admin 后台第一版
4. 补活动业务规则约束
5. 再做 Redis 与并发优化

## 仓库说明

当前仓库包含三个子项目：

- `campushub-server`：后端服务
- `campushub-user`：用户端前端
- `campushub-admin`：管理端前端

本 README 面向当前第一版开发阶段，后续会随着模块迭代继续更新。
