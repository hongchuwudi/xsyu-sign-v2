# XSYU Sign（油签机）

> 面向西安石油大学校园签到场景的 Web 系统，提供学校统一认证登录、签到查询与执行、自动签到、后台运维、公告发布、邮件通知和资源管理能力。

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42B883?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3.4-06B6D4?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![CI](https://github.com/hongchuwudi/xsyu-sign-v2/actions/workflows/ci.yml/badge.svg)](https://github.com/hongchuwudi/xsyu-sign-v2/actions/workflows/ci.yml)

- 当前仓库：<https://github.com/hongchuwudi/xsyu-sign-v2>
- 生产站点：<https://xsyusign.hongchu.xyz>
- 旧仓库：<https://github.com/hongchuwudi/xsyu-sign>

## 项目定位

XSYU Sign 采用前后端分离开发、单镜像生产部署的 Monorepo 结构。普通用户以学号作为唯一账号，通过学校统一身份认证系统完成密码、短信或扫码登录；管理员使用本地密码登录，并通过独立的管理端维护用户、任务、签到队列、公告和操作日志。

系统不是一个独立的学校账号体系：

- 普通用户不需要在本站注册，首次成功通过学校认证后自动创建本地用户记录。
- 普通用户身份始终以学校认证返回的学号为准。
- 管理员账号是唯一例外，使用本地 PBKDF2 密码校验。
- 学校接口产生的 JWS/JWSESSION 用于调用校园签到接口，并由系统按配置续期。

本项目仅供学习、研究和个人效率工具开发。使用者应遵守学校规章、网络服务条款及适用法律，并自行承担自动化操作带来的风险。

## 核心功能

### 用户端

- 学号 + 学校密码登录，支持学校验证码风控流程。
- 手机短信验证码登录，支持手机号关联多个学校身份时按学号匹配。
- 校园应用扫码登录，轮询确认后校验返回身份与输入学号是否一致。
- 查看个人资料、JWS 有效状态和最近签到记录。
- 查看签到列表、签到详情并执行签到。
- 开启或关闭自动签到，配置每周参与签到的日期。
- 更新学校密码，为后续 JWS 自动续期提供凭据。
- 查看管理员发布的 Markdown 公告。

### 管理端

- 用户列表、统计、详情、编辑、新增、删除和签到记录查询。
- 为单个用户刷新 JWS、切换自动签到或执行签到。
- 数据库驱动的动态定时任务配置，无需修改代码即可调整 Cron。
- Redis 延迟队列查看、清理和立即调度。
- Markdown 公告新增、编辑、删除和预览。
- 公告图片粘贴、拖拽和文件选择上传。
- 操作日志查询和清理。
- 后端提供邮件模板、用户组、收件人、立即发送和定时发送任务接口。

### 平台能力

- JWT 登录态、管理员角色校验、接口限流和统一异常响应。
- MySQL 持久化用户、公告、任务、日志、邮件和媒体元数据。
- Redis Sorted Set 实现带随机延迟的签到任务队列。
- 阿里云 OSS 保存公告图片，MySQL 只保存资源元数据和引用关系。
- GitHub Actions 自动验证和构建，GHCR 发布镜像，生产环境手动部署。
- 新容器健康检查失败时自动恢复上一版本容器。

## 技术栈

| 层级 | 技术 | 用途 |
| --- | --- | --- |
| 后端运行时 | Java 17、Spring Boot 3.5.7 | Web API、配置、任务调度和应用生命周期 |
| 持久化 | MyBatis-Plus 3.5.6、MySQL | 业务数据、资源元数据和管理配置 |
| 缓存与队列 | Spring Data Redis、Redis | 登录会话辅助、限流和签到延迟队列 |
| HTTP 集成 | Spring WebFlux WebClient | CAS、校园签到系统等外部 HTTP 调用 |
| 认证与安全 | JWT、RSA、PBKDF2、AES-GCM | 登录态、传输加密、本地密码和敏感凭据存储 |
| 对象存储 | Aliyun OSS SDK 3.18.2 | Markdown 图片对象存储 |
| Markdown | flexmark-java 0.64.8 | 后端通过 AST 提取图片节点并维护资源引用 |
| 前端 | Vue 3.5、Vue Router、Pinia | 独立 SPA、路由和会话状态 |
| UI | Tailwind CSS 3.4 | 页面布局与组件样式 |
| 编辑器 | md-editor-v3 7 | Markdown 编辑、预览和图片上传 |
| 构建 | Maven、Vite、npm | 后端打包和前端构建 |
| 交付 | Docker、GitHub Actions、GHCR | 单镜像构建、发布和生产部署 |

## 系统架构

```text
浏览器
  │
  │ HTTPS
  ▼
Nginx / xsyusign.hongchu.xyz
  │
  ▼
Spring Boot :11451
  ├── Vue SPA 静态资源
  ├── REST API
  ├── JWT / Admin / RateLimit 拦截器
  ├── 动态任务调度器
  ├── MyBatis-Plus ─────────────► MySQL
  ├── RedisTemplate ────────────► Redis
  ├── WebClient ────────────────► 学校 CAS 与校园签到系统
  ├── JavaMailSender ───────────► SMTP
  └── ObjectStorageService ─────► 阿里云 OSS
```

开发阶段由 Vite 提供前端开发服务器，并将 API 请求代理到 Spring Boot。生产镜像通过多阶段 Dockerfile 先构建 Vue，再将 `dist` 放入 Spring Boot 静态资源目录，最终只运行一个 Java 17 JRE 容器。

## 登录与用户模型设计

### 身份模型

`user.username` 保存学号，是普通用户的唯一业务身份。普通用户首次登录时，后端从学校返回的身份信息中读取学号、姓名和手机号，再执行 `findOrCreateUser`。系统不存在面向普通用户的本地注册入口。

主要凭据用途如下：

| 字段/令牌 | 用途 |
| --- | --- |
| `username` | 学号和本站普通用户唯一标识 |
| `password` | 管理员本地登录密码的 PBKDF2 哈希；普通用户可为空 |
| `stu_password` | 加密保存的学校密码，仅用于 JWS 自动续期 |
| `jws` | 调用校园业务接口的会话凭据 |
| JWT | 本站前后端登录态，包含用户 ID 和角色 |
| `role` | 区分普通用户与 `ADMIN` 管理员 |

### 三种学校登录方式

```text
密码登录：学号 + RSA 加密密码 ─► CAS 登录页/验证码 ─► ticket ─► JWSESSION
短信登录：手机号发送验证码 ─► 保持同一 CAS SESSION ─► 按学号匹配身份 ─► JWSESSION
扫码登录：创建二维码 ─► 校园应用确认 ─► 后端轮询 ─► 校验学号 ─► JWSESSION
```

CAS 登录失败通常仍返回 HTTP 200，因此不能只依赖 HTTP 状态码。后端会读取 CAS 页面中的错误节点，区分账号或密码错误、验证码无效、账号锁定等状态；触发验证码时通过业务码通知前端展示验证码输入流程。

管理员登录不访问 CAS。配置的管理员用户名由后端执行本地密码校验，旧加密格式可在成功登录后迁移到 PBKDF2。

### 请求保护

拦截器按以下顺序工作：

1. `RateLimitInterceptor`：对应用请求执行限流。
2. `JwtInterceptor`：校验本站 JWT，并将当前用户写入请求上下文。
3. `AdminAuthInterceptor`：保护 `/admin/**`、管理员代签和全员签到接口。

登录、获取 RSA 公钥、短信和扫码初始化接口不要求 JWT；管理端接口同时要求有效 JWT 和管理员角色。

## 自动签到设计

系统使用“数据库任务配置 + 动态调度器 + Redis 延迟队列”分离任务生成和实际执行：

```text
schedule_users
  │ 筛选 auto_sign=true 且当天位于 sign_days 的用户
  │ 计算每人的随机延迟
  ▼
Redis Sorted Set: sign:queue
  │ member = 学号
  │ score  = 计划执行时间戳
  ▼
interval_sign
  │ 周期性取出 score <= 当前时间的用户
  ▼
SignService.signAll(username)
  ├── JWS 无效时尝试续期
  ├── 查询待签到项目
  ├── 按配置的位置参数执行签到
  └── 写入操作日志 / 触发通知
```

动态任务由 `task_config` 表维护，当前识别以下任务键：

| 任务键 | 责任 |
| --- | --- |
| `schedule_users` | 将符合条件的用户按随机时间放入 Redis 队列 |
| `interval_sign` | 周期性消费已到期的签到任务 |
| `refresh_jws` | 按配置周期刷新用户 JWS |

修改任务配置后，`SchedulingManager` 会取消旧任务并按数据库中的 Cron 重新注册。任务执行结果写入操作日志，便于管理员观察成功、失败和部分成功状态。

## Markdown 公告与 OSS 资源设计

管理员公告使用 `md-editor-v3` 编辑。粘贴、拖拽或选择 JPEG、PNG、GIF、WebP 图片后，前端调用：

```http
POST /admin/assets/images
Content-Type: multipart/form-data
```

上传请求携带 `draftToken`。后端校验实际文件类型和大小，禁止 SVG，然后通过 `ObjectStorageService` 上传。当前实现 `AliyunOssStorageService`，对象键采用日期和 UUID：

```text
xsyu-sign/media/yyyy/MM/dd/{uuid}.{extension}
```

浏览器插入的是 OSS Bucket 的公开 HTTPS URL。Endpoint 可切换为北京地域内网地址以减少 ECS 上传流量，但公开访问地址仍使用外网 Bucket 域名。

### 资源生命周期

```text
上传图片
  │
  ▼
TEMP（带 draftToken）
  │ 公告保存，flexmark AST 找到 Markdown 图片节点
  ▼
ACTIVE + content_asset_ref
  │ 图片从公告移除，或公告被删除
  ▼
ORPHAN（记录 orphaned_at）
  │ 超过保留期且仍无引用
  ▼
删除 OSS 对象和数据库元数据
```

- 未保存草稿产生的 `TEMP` 图片超过 24 小时后清理。
- 无引用的 `ORPHAN` 图片超过 7 天后清理。
- 清理任务默认每天 03:30 执行，可通过 Cron 环境变量调整。
- Markdown 图片使用 flexmark AST 解析，不使用正则表达式识别节点。
- `media_asset` 记录上传者、文件名、MIME、大小、SHA-256、对象键、URL 和状态。
- `content_asset_ref` 以 `(biz_type, biz_id, asset_id)` 唯一约束维护内容与资源的关系。
- 图片二进制不写入 MySQL。

## 前端结构

前端页面按公共、用户和管理员三组组织：

```text
xsyu-sign-web/src/
├── components/              # 通用组件、布局、弹窗和消息组件
├── composables/             # 可复用业务动作
├── router/                  # 路由和登录/角色守卫
├── stores/                  # Pinia 会话状态
├── utils/
│   ├── api.js               # 统一后端 API 调用入口
│   └── message.js           # 全局消息提示
└── views/
    ├── common/
    │   ├── login/           # 登录页
    │   └── about/           # 关于页
    ├── user/
    │   ├── home/            # 用户首页
    │   ├── announcement/    # 公告
    │   ├── sign/            # 签到列表与详情
    │   └── mine/            # 个人设置
    └── admin/
        ├── users/           # 用户管理
        ├── task/            # 定时任务配置
        ├── queue/           # Redis 队列
        ├── announcement/    # Markdown 公告管理
        └── operation-logs/  # 操作日志
```

主要路由：

| 路由 | 角色 | 页面 |
| --- | --- | --- |
| `/login` | 公开 | 登录 |
| `/about` | 已登录用户 | 关于 |
| `/` | 普通用户 | 首页与账号状态 |
| `/announcement` | 普通用户 | 最新公告 |
| `/sign` | 普通用户 | 签到列表 |
| `/mine` | 普通用户 | 用户设置 |
| `/admin/*` | 管理员 | 管理后台 |

路由守卫会将未登录访问重定向到 `/login`，阻止普通用户进入管理端，并将已登录管理员默认引导到用户管理页。

## 后端结构

```text
xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/
├── annotation/              # 操作日志等业务注解
├── aspect/                  # AOP 日志记录
├── config/                  # Web、Redis、任务和属性配置
├── context/                 # 当前请求用户上下文
├── controller/              # REST API
├── exception/               # 业务异常和统一异常处理
├── intercepter/             # 限流、JWT、管理员权限
├── mapper/                  # MyBatis-Plus Mapper
├── pojo/
│   ├── DTO/                 # 请求对象
│   ├── VO/                  # 响应对象
│   └── entity/              # 数据库实体
├── result/                  # 统一响应结构
├── service/                 # 业务接口及实现
├── storage/                 # 对象存储抽象与阿里云 OSS 实现
├── task/                    # 动态签到与邮件任务
├── utils/                   # JWT、RSA、加密等工具
└── webClient/               # CAS 和校园业务系统客户端
```

关键 Controller：

| Controller | 主要职责 |
| --- | --- |
| `UserController` | 密码/短信/扫码登录、个人信息、自动签到配置 |
| `SignController` | 签到列表、详情、用户签到和管理员代签 |
| `AdminController` | 用户管理、统计和 JWS 管理 |
| `TaskConfigController` | 动态任务配置和立即调度 |
| `RedisQueueController` | 延迟队列监控与清理 |
| `AnnouncementController` | 用户公告读取和管理员 CRUD |
| `MediaAssetController` | 管理员图片上传 |
| `OperationLogController` | 操作日志查询与清理 |
| `AdminEmailController` | 邮件模板、分组和发送任务 |

## API 概览

前端实际调用定义以 [`xsyu-sign-web/src/utils/api.js`](xsyu-sign-web/src/utils/api.js) 为准。以下仅列出主要业务入口。

### 认证与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/user/public-key` | 获取登录密码传输所用 RSA 公钥 |
| `POST` | `/user/xsy-login` | 学号密码登录；管理员账号走本地校验 |
| `POST` | `/user/sms/send` | 发送学校短信验证码 |
| `POST` | `/user/sms/login` | 短信校验并登录 |
| `POST` | `/user/qr/create` | 创建扫码登录会话 |
| `POST` | `/user/qr/poll` | 轮询扫码状态 |
| `GET` | `/user/info` | 当前用户信息 |
| `PUT` | `/user/info/` | 更新用户资料 |
| `POST` | `/user/bind/password` | 验证并更新学校密码 |
| `PUT` | `/user/auto-sign/{isAuto}` | 自动签到开关 |
| `PUT` | `/user/sign-days` | 更新签到星期配置 |
| `POST` | `/user/unregister` | 注销并删除当前用户信息 |

### 签到与公告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/sign/allSign` | 获取当前用户签到列表 |
| `GET` | `/sign/oneSign/{signId}/{schoolId}` | 获取签到详情 |
| `POST` | `/sign/all` | 当前用户执行全部待签到项 |
| `POST` | `/sign/one` | 执行单个签到 |
| `GET` | `/user/announcement/latest` | 获取最新公告 |
| `GET/POST` | `/admin/announcements` | 管理员查询/创建公告 |
| `GET/PUT/DELETE` | `/admin/announcements/{id}` | 公告详情、更新和删除 |
| `POST` | `/admin/assets/images` | 上传公告图片 |

### 管理端

所有 `/admin/**` 接口均要求管理员权限。

- `/admin/users`：用户查询、统计、新增、编辑和删除。
- `/admin/task-config`：任务配置、调度日历和立即调度。
- `/admin/redis-queue`：延迟队列查看与清空。
- `/admin/operation-logs`：操作日志查询与删除。
- `/admin/email/*`：邮件模板、用户组、任务和收件人管理。
- `/sign/all-admin/{username}`：管理员为指定用户签到。
- `/sign/all-all`：管理员为全部用户签到。

## 数据库与迁移

SQL 迁移统一位于 [`xsyu-sign-server/migrations/`](xsyu-sign-server/migrations/)，应用不会自动执行这些生产迁移。

| 文件 | 用途 |
| --- | --- |
| `add_stu_columns.sql` | 学号用户模型、学校密码、手机号、角色等字段 |
| `add_sign_days_column.sql` | 用户每周签到日配置 |
| `add_email_notification_tables.sql` | 邮件模板、用户组、通知任务和收件人 |
| `add_media_asset_tables.sql` | OSS 媒体资源及内容引用关系 |

部署包含数据库结构变更的版本前：

1. 备份目标数据库。
2. 按迁移文件的依赖顺序人工执行 SQL。
3. 检查字段类型和目标库名称。
4. 确认迁移对旧容器向后兼容。
5. 再手动触发生产部署。

特别注意：现有 `user.id` 是 `BIGINT UNSIGNED`。任何引用它的外键字段（如 `media_asset.created_by`）也必须使用 `BIGINT UNSIGNED`，否则 MySQL 会报外键字段类型不兼容。

## 目录结构

```text
xsyu-sign/
├── .github/workflows/
│   ├── ci.yml                        # 自动验证、构建和发布 GHCR 镜像
│   └── deploy-production.yml         # 手动生产部署
├── deploy/production/
│   ├── deploy.sh                     # 服务端部署和自动回滚脚本
│   └── xsyu-sign.env.example         # 生产环境变量模板
├── docs/                              # 设计、分析、部署和长期工程记忆
├── xsyu-sign-server/                  # Spring Boot 后端
│   ├── migrations/                    # 手工 SQL 迁移
│   └── src/
├── xsyu-sign-web/                     # Vue 3 前端
│   └── src/
├── Dockerfile                         # 前后端单镜像多阶段构建
└── README.md
```

旧静态前端已经删除。`xsyu-sign-server/src/main/resources/static/` 在生产构建阶段由 Vue 构建产物填充，不再作为独立维护的前端源码目录。

## 本地开发

### 环境要求

- JDK 17
- Maven 3.9+（或兼容版本）
- Node.js 22
- npm 10+
- MySQL 8.x
- Redis 6/7

### 1. 克隆仓库

```bash
git clone https://github.com/hongchuwudi/xsyu-sign-v2.git
cd xsyu-sign-v2
```

### 2. 准备数据库

创建开发数据库并执行项目所需的基础表结构和 [`xsyu-sign-server/migrations/`](xsyu-sign-server/migrations/) 中尚未应用的迁移。该项目当前不使用 Flyway/Liquibase 自动迁移，不能只启动应用而跳过建表。

### 3. 配置后端

后端主配置位于 `xsyu-sign-server/src/main/resources/application.yml`，环境配置由 Spring Profile 和外部配置覆盖。不要把数据库密码、Redis 密码、邮箱授权码、RSA 私钥、测试账号或 OSS AccessKey 提交到 Git。

推荐在本机使用未跟踪的配置文件、IDE 环境变量或系统环境变量覆盖配置。Spring Boot 支持将诸如 `hc.datasource.host` 映射为 `HC_DATASOURCE_HOST`。

常见配置分组：

- `hc.datasource.*`：MySQL。
- `hc.redis.*`：Redis。
- `hc.mail.*`：SMTP 发件账号。
- `hc.jwt.*`：JWT 密钥、有效期和 Header 名称。
- `hc.admin.*`：管理员身份配置。
- `hc.rsa.*`：前端登录密码传输所用 RSA 密钥对。
- `hc.sign-infos.*`：签到位置和范围参数。
- `hc.urls.*`：校园业务接口地址。
- `xsyu.storage.*`：对象存储和清理任务。

### 4. 启动后端

```bash
cd xsyu-sign-server
mvn spring-boot:run
```

默认端口为 `11451`。

### 5. 启动前端

```bash
cd xsyu-sign-web
npm ci
npm run dev
```

Vite 开发地址通常为 `http://localhost:5173`。开发代理配置负责将 API 请求转发到后端。

## OSS 配置

阿里云 OSS 凭据只能从运行环境注入，不能写入前端、Git 跟踪配置、Dockerfile 或镜像层。

| 环境变量 | 默认值/说明 |
| --- | --- |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 必填，RAM 用户 AccessKey ID |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 必填，RAM 用户 AccessKey Secret |
| `ALIYUN_OSS_ENDPOINT` | 默认北京公网 Endpoint；北京 ECS 可使用内网 Endpoint |
| `ALIYUN_OSS_BUCKET` | 默认 `hc-base` |
| `ALIYUN_OSS_PUBLIC_BASE_URL` | 默认 Bucket 公网 HTTPS 域名 |
| `ALIYUN_OSS_OBJECT_PREFIX` | 默认 `xsyu-sign/media` |
| `ALIYUN_OSS_MAX_FILE_SIZE` | 默认 `10MB` |
| `ALIYUN_OSS_MAX_REQUEST_SIZE` | 默认 `11MB` |
| `ALIYUN_OSS_CLEANUP_CRON` | 默认每天 03:30 清理过期资源 |

上传所用 RAM 身份应遵循最小权限，只允许目标 Bucket/前缀下必要的对象写入、读取元数据和删除操作。Bucket 是否公开读取应按实际访问方案配置；不要授予 Bucket 管理、RAM 管理或其他无关权限。

## 构建与验证

### 前端构建

```bash
cd xsyu-sign-web
npm ci
npm run build
```

产物位于 `xsyu-sign-web/dist/`。

### 后端测试与打包

```bash
cd xsyu-sign-server
mvn test
mvn package -DskipTests
```

部分历史测试属于连接真实 CAS、数据库或邮件服务的“可观测测试”，可能产生外部请求或副作用。运行完整测试前请先阅读测试代码并准备隔离配置。CI 当前只运行无外部副作用的指定测试，然后执行跳过测试的完整打包以验证编译。

### Docker 镜像

```bash
docker build -t xsyu-sign:local .
docker run --rm -p 11451:11451 --env-file /path/to/xsyu-sign.env xsyu-sign:local
```

根目录 Dockerfile 包含三个阶段：

1. Node 22 构建 Vue 前端。
2. Maven + Java 17 将前端产物写入 Spring Boot 静态资源并打包 JAR。
3. Java 17 JRE 仅保留运行所需的 JAR。

## CI/CD 与生产部署

### 自动持续集成

推送到 `main` 或创建指向 `main` 的 Pull Request 时，[`.github/workflows/ci.yml`](.github/workflows/ci.yml) 会：

1. 使用 Node.js 22 安装并构建前端。
2. 使用 Java 17 运行 CI 允许的后端测试。
3. 打包 Spring Boot 应用。
4. 构建前后端一体化 Docker 镜像。
5. 仅在 `main` push 时将 `latest` 和 `sha-<commit>` 标签推送到 GHCR。

镜像仓库：

```text
ghcr.io/hongchuwudi/xsyu-sign-v2
```

### 手动生产部署

推送代码不会自动更新生产环境。生产发布必须在 GitHub Actions 中手动运行 `Deploy production` 工作流，并输入要部署的镜像标签。生产环境推荐使用不可变的完整 SHA 标签：

```text
sha-<完整提交哈希>
```

工作流通过专用 SSH 凭据连接服务器，执行 `/opt/xsyu-sign/deploy.sh`：

1. 校验镜像标签和运行环境。
2. 拉取目标 GHCR 镜像。
3. 停止并保留现有 `qq-robot` 容器为备份。
4. 使用相同端口、日志卷、外部配置和环境变量启动新容器。
5. 在 90 秒内轮询 `http://127.0.0.1:11451/`。
6. 健康后删除备份容器。
7. 启动失败或超时则删除新容器并恢复旧容器。
8. 最后验证公网 HTTPS 地址。

生产运行时关键路径：

| 路径 | 用途 |
| --- | --- |
| `/opt/xsyu-sign/deploy.sh` | 部署与自动回滚脚本 |
| `/etc/xsyu-sign.env` | 运行时环境变量，权限应为 `600` |
| `/etc/xsyu-sign/config/` | 只读挂载的外部 Spring Boot 配置 |
| `/home/hongchu/qqrobot/` | 持久化应用日志 |

容器部署脚本只管理应用容器，不修改 Nginx、TLS 证书、MySQL、Redis 或其他容器。自动回滚也不会回滚 SQL，因此数据库迁移必须保持向后兼容。

详细步骤见 [CI/CD 与生产部署说明](docs/cicd-deployment.md)。

## 安全注意事项

- 不要提交 AccessKey、数据库密码、Redis 密码、邮箱授权码、JWT 密钥、RSA 私钥或真实测试账号。
- 前端只持有 RSA 公钥；私钥只能存在后端运行环境。
- 普通用户学校密码只能按既定加密方案保存，日志不得输出明文密码、验证码或完整会话凭据。
- `/admin/**` 必须同时经过 JWT 和管理员角色校验。
- OSS 图片必须验证真实 MIME/文件签名，不能仅按扩展名判断；SVG 明确禁止上传。
- 生产环境变量文件和外部配置应限制为部署用户或 root 可读。
- GitHub Secrets 只保存部署所需凭据，生产应用秘密保留在服务器运行环境。
- 提交日志、截图或排障文档前，应清理 Cookie、JWT、JWS、AccessKey 和个人信息。

如果凭据曾出现在聊天、日志、提交历史或公开页面，应立即在对应平台撤销并轮换，不能只从当前文件中删除。

## 文档索引

- [CI/CD 与生产部署](docs/cicd-deployment.md)
- [CI/CD 设计](docs/superpowers/specs/2026-09-19-cicd-design.md)
- [校园系统接口分析](docs/xsyu-school-system-analysis.md)
- [CAS 登录错误分类修复设计](docs/superpowers/specs/2026-09-18-cas-login-error-classification-fix-design.md)
- [管理员公告浏览器草稿设计](docs/superpowers/specs/2026-09-18-admin-announcement-browser-draft-design.md)
- [AI 记忆索引](docs/ai-memory/README.md)
- [CAS 登录行为记忆](docs/ai-memory/cas-login-behavior.md)
- [用户模型重构记忆](docs/ai-memory/user-model-refactor.md)
- [前端迁移接手记录](docs/ai-memory/frontend-handoff.md)
- [后端历史说明](xsyu-sign-server/README.md)

## 贡献约定

1. 从 `main` 创建功能分支。
2. 保持 Spring Boot、MyBatis-Plus、Vue 3、Vite 和 Tailwind CSS 3 的现有风格。
3. 数据库结构变化必须新增迁移文件，不直接依赖生产库手工状态。
4. 前端 API 统一通过 `src/utils/api.js` 管理，消息反馈使用现有全局消息机制。
5. 提交前至少验证受影响模块能够构建。
6. Pull Request 合并到 `main` 后等待 CI 和 GHCR 镜像完成；生产部署仍由维护者手动触发。

## 当前状态

- Vue 3 用户端和管理员端已成为当前前端实现，旧静态前端已删除。
- 密码、短信、扫码三种登录方式已接入。
- 用户、签到、任务、队列、公告、操作日志和邮件管理已具备后端能力。
- Markdown 公告图片已接入阿里云 OSS，并包含 TEMP/ACTIVE/ORPHAN 生命周期清理。
- CI 自动构建和 GHCR 发布已启用。
- 生产部署采用手动触发、健康检查和应用容器自动回滚。

项目仍依赖学校外部系统的页面结构、风控策略和接口可用性。修改 CAS 或签到请求逻辑前，请先阅读 `docs/ai-memory/` 和校园系统分析文档，并优先保留现有可观测性。
