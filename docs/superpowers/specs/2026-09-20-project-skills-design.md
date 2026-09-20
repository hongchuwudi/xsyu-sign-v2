# XSYU Sign 项目技能设计

## 目标

在仓库根目录增加 `skills/`，把西安石油大学校园系统逆向资料和 XSYU Sign 的长期项目知识整理为可被后续 Codex/Agent 按需读取的本地技能。

技能只保存可提交的技术知识、项目约束和操作流程，不包含 AccessKey、数据库密码、JWT 密钥、SSH 私钥、用户密码或本地私密环境配置。

## 目录结构

```text
skills/
├─ xsyu-school-system/
│  ├─ SKILL.md
│  └─ references/
│     ├─ cas-password-login.md
│     ├─ sms-login.md
│     ├─ qr-login.md
│     ├─ session-and-jws.md
│     └─ campus-sign-api.md
├─ xsyu-auth-user-model/
│  └─ SKILL.md
├─ xsyu-frontend/
│  └─ SKILL.md
├─ xsyu-production-deployment/
│  └─ SKILL.md
└─ xsyu-storage-media/
   └─ SKILL.md
```

每个技能都使用标准 YAML frontmatter，名称与目录一致。技能默认允许自动发现，不设置 explicit-only 策略。

## 学校系统主技能

`xsyu-school-system` 是完整的学校系统知识入口，适用于 CAS 登录、验证码、短信登录、扫码登录、票据跳转、JWSESSION、用户身份接口和校园签到接口相关任务。

`SKILL.md` 只保留：

- 使用场景和边界。
- 已验证的关键事实。
- 修改相关代码前必须检查的项目文件。
- 按任务类型读取 reference 的路由说明。
- 禁止泄露密码、验证码、会话 Cookie 和有效 JWSESSION 的安全约束。

详细资料拆分到五个 reference，避免每次任务加载全部逆向信息。内容以 `docs/xsyu-school-system-analysis.md` 和 `docs/ai-memory/cas-login-behavior.md` 为事实来源，但整理为独立、可直接使用的规范，不仅放置易失效的链接。

## 项目技能

### xsyu-auth-user-model

记录当前用户模型和认证约束：学号即用户名、普通用户无独立注册、三种学校登录方式、管理员本地登录、统一 `password` 字段、JWS 续签规则、登录错误分类和敏感字段规则。

### xsyu-frontend

记录 Vue 3、Vite、Pinia、Tailwind 3 的前端边界，包括页面目录、API 封装、消息与模态框复用、Hash 路由、登录持久化和禁止恢复旧静态前端等约束。

### xsyu-production-deployment

记录 GHCR、GitHub Actions、手动生产部署、Nginx 到 Spring Boot 的当前链路、Docker 容器和部署验证流程。涉及生产修改时仍必须取得用户授权；技能本身不包含 SSH 私钥或环境变量值。

### xsyu-storage-media

记录阿里云 OSS 图片管理设计，包括配置来源、对象前缀、公开 URL、媒体表、草稿关联、Markdown AST 解析、孤立资源清理、图片类型校验和部署前 SQL 要求。

## AGENTS.md 集成

更新根目录 `AGENTS.md`，增加项目技能索引和触发建议，让后续代理在处理对应领域前读取相关 `SKILL.md`。保留现有项目说明和长效文档索引。

## 验证

- 使用 skill-creator 自带的 `quick_validate.py` 验证每个技能。
- 检查所有 reference 都可从对应 `SKILL.md` 发现。
- 搜索密钥、密码、私钥和有效 Token，确保没有敏感值进入技能目录。
- 检查无占位符、无空目录和无未替换脚手架内容。
- 只提交本次新增技能、设计文档和 `AGENTS.md` 变更，不提交工作区已有无关修改。
