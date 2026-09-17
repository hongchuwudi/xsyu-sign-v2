# 🚀 QQ Robot Sign - 自动化签到机器人

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen?style=for-the-badge&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)

## 📖 项目简介

**QQ Robot Sign** 是一个基于 Spring Boot 3.5.7 和 Java 17 开发的自动化签到系统，专门为西安石油大学的学生设计。系统通过模拟移动端请求，实现自动化的位置签到功能，并支持定时任务执行,本项目还在持续更新中,计划发展为一款QQ机器人,加入班级群和AI元素丰富聊天化。

### ✨ 核心特性

- 🕒 **数据库驱动定时任务** - 通过 TaskConfig 动态管理 cron，无需重启即可调整
- 🔴 **Redis 延迟队列** - 随机延迟分散签到请求，避免并发风控
- 📍 **位置模拟签到** - 支持校区位置自动识别
- 🔐 **安全加密存储** - RSA + AES 双重加密用户凭证
- 🔄 **多用户支持** - 支持多个用户独立配置签到日和时段
- 📊 **SPA 管理面板** - 纯前端 SPA，可视化配置任务和查看队列
- 📢 **公告系统** - 支持后台发布公告，前端弹窗展示

## 🛠 技术栈

**后端框架**
- <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" height="20"> Java 17
- <img src="https://img.shields.io/badge/Spring%20Boot-3.5.7-6DB33F?style=flat-square&logo=springboot&logoColor=white" height="20"> Spring Boot 3.5.7

**数据存储**
- <img src="https://img.shields.io/badge/MySQL-8.0+-4479A1?style=flat-square&logo=mysql&logoColor=white" height="20"> MySQL 8.0+
- <img src="https://img.shields.io/badge/Redis-7.0+-DC382D?style=flat-square&logo=redis&logoColor=white" height="20"> Redis 7.0+
- <img src="https://img.shields.io/badge/MyBatis%20Plus-3.5.10.1-000000?style=flat-square&logo=apache&logoColor=white" height="20"> MyBatis Plus 3.5.10.1

**网络通信**
- <img src="https://img.shields.io/badge/WebClient-Reactive-4A154B?style=flat-square&logo=spring&logoColor=white" height="20"> WebClient
- <img src="https://img.shields.io/badge/Spring%20Task-Scheduling-6DB33F?style=flat-square&logo=spring&logoColor=white" height="20"> Spring Task + CronTrigger

**前端**
- <img src="https://img.shields.io/badge/Vanilla%20JS-SPA-F7DF1E?style=flat-square&logo=javascript&logoColor=black" height="20"> 原生 JS SPA
- <img src="https://img.shields.io/badge/Tailwind%20CSS-3.4-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white" height="20"> Tailwind CSS 3.4

**构建工具**
- <img src="https://img.shields.io/badge/Maven-3.6+-C71A36?style=flat-square&logo=apache-maven&logoColor=white" height="20"> Maven 3.6+

## ⚙️ 配置文件说明

由于安全考虑，配置文件已添加到 `.gitignore`，请按以下说明创建配置文件：

### 2. 开发环境配置 `application-dev.yml`

```yaml
hc:
  datasource:
    host: your-mysql-host
    port: your-mysql-port
    username: your-username
    password: your-password
    db: your-database-name
  redis:
     host: your-redis-host
     port: your-redis-port
     password: your-redis-pwd
     db: 1
  mail:
     username: your-email-username  # 发件邮箱
     password: your-email-pwd  # 授权码
```

### 3. 签到信息配置 `application-signInfo.yml`

```yaml
hc:
  sign-infos:
    in-area: 1
    area-json: '{"id":"170002","name":"鄠邑校区"}'
    latitude: 34.098273   # 纬度
    longitude: 108.656693 # 经度
  urls:
    base-url: "https://gwxg.xsyu.edu.cn"
    uri-pri: "/sign/mobile/receive"
    uri-get-all-sign: "/getMySignLogs"
    uri-get-one-sign: "/getSignLog"
    uri-sign: "/doSignByArea"
  test-user:
    username: "your-student-id"
    password: "your-password"
```

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### 2. 数据库初始化

```sql
-- 用户表 
-- auto-generated definition
create table user
(
   id                  bigint unsigned auto_increment comment '用户ID'
      primary key,
   name                varchar(255)                         null comment '姓名',
   email               varchar(255)                         null comment '邮箱(用于发送通知)',
   username            varchar(50)                          not null comment '用户名',
   password            varbinary(255)                       not null comment '加密密码',
   jws                 text                                 null comment 'JWT令牌',
   sign_random_delay   int                                  null comment '随机延迟时间(秒)',
   sign_scheduled_time datetime                             null comment '计划签到时间',
   created_at          timestamp  default CURRENT_TIMESTAMP null comment '创建时间',
   updated_at          timestamp  default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
   auto_sign           tinyint(1) default 0                 not null comment '是否参加自动签到',
   constraint username
      unique (username)
)
   comment '用户表';

create index idx_username
   on user (username);
```

### 3. 配置步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-username/xsyu-sign.git
   cd xsyu-sign/xsyu-sign-server
   ```

2. **创建配置文件**
   ```bash
   # 在 src/main/resources/ 目录下创建：
   # application-dev.yml
   # application-signInfo.yml
   ```

3. **修改配置参数**
    - 在 `application-dev.yml` 中配置你的数据库连接
    - 在 `application-signInfo.yml` 中配置你的学号和密码

4. **运行项目**
   ```bash
   mvn spring-boot:run
   ```

## 📋 API 接口

> 前端实际调用的完整接口清单见 `xsyu-sign-web/src/utils/api.js`

### 认证（学号即账号，无需注册）
- `POST /user/xsy-login` - 学号+学校密码登录（触发风控验证码时返回 1004，管理员走本地密码）
- `POST /user/sms/send` - 发送短信验证码
- `POST /user/sms/login` - 短信登录（按学号匹配账号）
- `POST /user/qr/create` / `POST /user/qr/poll` - 扫码登录（创建二维码 / 轮询状态）
- `POST /user/bind/password` - 更新学校密码（登录态，供 JWS 自动续签）
- `GET /user/public-key` - 获取 RSA 公钥（前端加密密码用）

### 用户
- `GET /user/info` / `PUT /user/info/` - 获取/修改个人信息
- `PUT /user/auto-sign/{isAuto}` - 自动签到开关
- `PUT /user/sign-days` - 设置签到日期
- `POST /user/logout` / `POST /user/unregister` - 退出 / 注销

### 签到功能
- `GET /sign/allSign` - 获取签到列表
- `GET /sign/oneSign/{signId}/{schoolId}` - 获取签到详情
- `POST /sign/all` - 当前用户一键签到
- `POST /sign/all-admin/{username}` - 管理员为指定用户一键签到
- `POST /sign/all-all` - 管理员为所有用户一键签到
- `POST /sign/one` - 处理单个签到

### 任务配置（管理员）
- `GET /admin/task-config` - 获取所有任务配置
- `GET /admin/task-config/{taskKey}` - 获取单个任务配置
- `PUT /admin/task-config/{taskKey}` - 更新任务配置（动态调整 cron）
- `POST /admin/task-config/schedule-users/immediate` - 立即执行调度
- `GET /admin/task-config/schedule-users/calendar` - 调度日历

### Redis 队列监控（管理员）
- `GET /admin/redis-queue` - 查看延迟队列中的待签到用户
- `DELETE /admin/redis-queue` - 清空延迟队列

## ⏰ 定时任务架构

系统使用 **TaskConfig（数据库配置） + SchedulingManager + Redis 延迟队列** 三级架构：

```
schedule_users (cron: 每天18:31)
    │  筛选 autoSign=true 且今天在 signDays 内的用户
    │  为每个用户计算随机延迟时间
    ▼
Redis Sorted Set (sign:queue)
    │  score = 执行时间戳
    │  每分钟轮询到期用户
    ▼
interval_sign (cron: 每1分钟, 18:00-20:59)
    │  从 Redis 取出到期用户
    │  逐个执行 signAll()
    ▼
签到完成 → 邮件通知
```

### 关键 Cron 配置

| 任务 | 默认 cron | 说明 |
|------|-----------|------|
| schedule_users | `0 31 18 * * ?` | 每天 18:31 调度用户到 Redis 队列 |
| interval_sign | `0 */1 18-20 * * ?` | 18:00-20:59 每分钟轮询执行 |
| refresh_jws | `0 0 18 * * ?` | 每天 18:00 检查 JWS 续签 |

> ⚠️ **容器部署注意**：Java 读取 `/etc/timezone` 而非 `TZ` 环境变量，Docker 容器需同时设置两者，否则 cron 触发时间会偏移。

## 🔒 安全特性

### 加密方案
- **AES-256-GCM** 加密用户密码
- **PBKDF2** 密钥派生算法
- **随机盐值** 每次加密不同
- **GCM认证** 防篡改保护

```java
// 加密示例
String encrypted = StrongCryptoUtils.encrypt("userPassword");
// 结果格式: 盐值:IV:密文 (自包含，无需额外存储)
```

## 🗂 项目结构

```
src/
├── main/
│   ├── java/com/hongchu/qqrobotsign/
│   │   ├── config/          # 配置类
│   │   ├── content/         # 常量类
│   │   ├── controller/      # 控制器层
│   │   ├── exception/       # 全局异常
│   │   ├── mapper/          # 持久化层
│   │   ├── pojo/            # 数据类型层
│   │   ├── result/          # 响应结构类型
│   │   ├── intercepter/     # 拦截层
│   │   ├── service/         # 业务逻辑层
│   │   ├── webClient/       # HTTP客户端
│   │   ├── task/            # 定时任务
│   │   └── utils/           # 工具类
│   └── resources/
│       ├── mapper                    # XML映射文件
│       ├── static                    # 静态资源文件
│       ├── logback-spring.xml        # 日志配置文件
│       ├── application.yml           # 主配置
│       ├── application-dev.yml       # 开发配置 (需手动创建)
│       ├── application-prod.yml      # 生产配置 (需手动创建)
│       └── application-signInfo.yml  # 签到配置 (需手动创建)

└── test/                    # 测试代码
```

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request


## ⚠️ 免责声明

本项目仅用于学习和研究目的，请遵守学校相关规定，合理使用自动化工具。开发者不对因使用本项目而产生的任何问题负责。

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**
