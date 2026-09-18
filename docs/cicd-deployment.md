# XSYU Sign CI/CD 与生产部署说明

本文记录 XSYU Sign 新仓库的持续集成、镜像发布和生产部署方式。设计背景见 [2026-09-19-cicd-design.md](superpowers/specs/2026-09-19-cicd-design.md)。

## 仓库与环境

- 当前仓库：<https://github.com/hongchuwudi/xsyu-sign-v2>
- 旧仓库：<https://github.com/hongchuwudi/xsyu-sign>
- 默认分支：`main`
- 镜像仓库：`ghcr.io/hongchuwudi/xsyu-sign-v2`
- 生产服务器：`49.232.16.204`
- 生产域名：<https://xsyusign.hongchu.xyz>
- 应用容器名：`qq-robot`
- 应用端口：`11451:11451`

本地 Git 中：

- `origin` 指向新仓库 `xsyu-sign-v2`。
- `old-origin` 指向旧仓库 `xsyu-sign`。

## 工作流

### 自动构建

文件：[ci.yml](../.github/workflows/ci.yml)

触发条件：

- 推送到 `main`
- 创建或更新目标为 `main` 的 Pull Request

流水线执行：

1. 使用 Node.js 22 安装前端依赖。
2. 执行 `npm run build`。
3. 使用 Java 17 运行 `XSYULoginUtilTest`。
4. 执行后端 Maven 打包。
5. 构建包含 Vue 前端和 Spring Boot 后端的单一 Docker 镜像。
6. `main` 推送时将镜像发布到 GHCR。

现有部分测试会连接真实数据库、发送邮件或访问学校 CAS，因此不在 CI 中运行。CI 只运行无外部副作用的单元测试，再通过完整打包验证编译。

### 手动生产部署

文件：[deploy-production.yml](../.github/workflows/deploy-production.yml)

入口：<https://github.com/hongchuwudi/xsyu-sign-v2/actions/workflows/deploy-production.yml>

部署生产：

1. 打开工作流页面。
2. 点击 `Run workflow`。
3. 确认分支为 `main`。
4. 输入镜像标签。
5. 点击运行并等待健康检查完成。

常用镜像标签：

- `latest`：`main` 最近一次成功构建。
- `sha-<完整提交号>`：不可变版本，推荐生产部署使用。

例如：

```text
sha-cef2fabdc16e869ba6437c8fb2fbe25c2c07d7c7
```

生产部署使用 GitHub `production` Environment，同一时间只允许一个部署任务运行。推送代码不会自动部署生产。

## 镜像结构

根目录 [Dockerfile](../Dockerfile) 使用多阶段构建：

1. Node 阶段构建 `xsyu-sign-web`。
2. Maven 阶段将前端 `dist` 放入 Spring Boot 静态资源目录并打包。
3. Java 17 JRE 阶段只保留运行所需 JAR。

[.dockerignore](../.dockerignore) 会排除本地配置文件、构建产物、Git 元数据和开发工具目录，防止生产密钥进入镜像。

## GitHub 配置

GitHub `production` Environment 中配置了以下 Secrets：

- `PROD_SSH_HOST`
- `PROD_SSH_PORT`
- `PROD_SSH_USER`
- `PROD_SSH_PRIVATE_KEY`
- `PROD_SSH_KNOWN_HOSTS`

不得把这些 Secret 的真实值写入 Git、文档、Issue 或 Actions 日志。

部署使用独立的 Ed25519 SSH 密钥：

- 私钥只保存在 GitHub Environment Secret 中。
- 公钥安装在生产服务器 root 用户的 `authorized_keys` 中。
- 创建过程使用的本机临时私钥已经删除。

GHCR 登录使用工作流运行期间的短期 `GITHUB_TOKEN`，不在服务器长期保存个人 GitHub Token。

## 服务器目录

```text
/opt/xsyu-sign/deploy.sh
/etc/xsyu-sign.env
/etc/xsyu-sign/config/application-dev.yml
/etc/xsyu-sign/config/application-signInfo.yml
/home/hongchu/qqrobot/
```

用途：

- `/opt/xsyu-sign/deploy.sh`：生产部署和自动回滚脚本。
- `/etc/xsyu-sign.env`：OSS 等运行时环境变量，权限应为 `600`。
- `/etc/xsyu-sign/config/`：从原生产 JAR 外置出的 Spring Boot 配置，目录和文件仅允许 root 读取。
- `/home/hongchu/qqrobot/`：挂载到容器 `/app/logs` 的持久化日志目录。

服务器首次配置备份位于类似目录：

```text
/root/xsyu-sign-cicd-backup-YYYYMMDD-HHMMSS/
```

## 部署与回滚流程

生产脚本：[deploy.sh](../deploy/production/deploy.sh)

执行流程：

1. 校验镜像标签格式。
2. 检查环境变量文件和外置配置目录。
3. 拉取目标 GHCR 镜像。
4. 将原 `qq-robot` 停止并改名为 `qq-robot-backup`。
5. 使用新镜像启动 `qq-robot`。
6. 最多等待 90 秒，轮询 `http://127.0.0.1:11451/`。
7. 健康检查成功后删除备份容器。
8. 启动失败或健康检查超时时，删除新容器并恢复旧容器。

拉取镜像发生失败时，旧容器不会停止或删除。该边界已经使用不存在的镜像标签在生产服务器验证，验证后容器 ID、运行状态和 11451 健康检查均保持不变。

新容器运行参数包括：

```text
--restart=always
-p 11451:11451
-v /home/hongchu/qqrobot:/app/logs
-v /etc/xsyu-sign/config:/app/config:ro
--env-file /etc/xsyu-sign.env
```

Nginx、证书、MySQL、Redis 和其他容器不会被应用部署脚本修改。

## 数据库迁移

容器自动回滚不会撤销 SQL。涉及数据库结构变化时：

1. 先备份数据库。
2. 执行 `xsyu-sign-server/migrations/` 中对应迁移。
3. 迁移应尽量向后兼容旧容器。
4. 确认旧容器仍能运行后，再手动部署新镜像。

不要在 GitHub Actions 中未经确认自动执行生产 SQL。

## 发布前检查

- 需要上线的本地改动已经提交并推送，未提交文件不会进入镜像。
- 对应 `main` CI 已成功。
- GHCR 已生成目标 SHA 标签。
- 生产 SQL 已执行且有备份。
- `/etc/xsyu-sign.env` 和 `/etc/xsyu-sign/config/` 配置完整。
- 当前没有其他生产部署正在运行。

## 常用排查

查看 CI：

```bash
gh run list --repo hongchuwudi/xsyu-sign-v2
```

查看生产工作流：

```bash
gh workflow view deploy-production.yml --repo hongchuwudi/xsyu-sign-v2
```

查看容器：

```bash
docker ps --filter name=qq-robot
docker logs --tail 200 qq-robot
```

本机健康检查：

```bash
curl -fsS http://127.0.0.1:11451/ >/dev/null
```

公网检查：

```bash
curl -I https://xsyusign.hongchu.xyz/
```

手动执行服务器部署脚本：

```bash
/opt/xsyu-sign/deploy.sh sha-<完整提交号>
```

手动执行前必须先登录 GHCR。日常发布应优先使用 GitHub Actions，避免绕过部署记录和并发锁。

## 已验证记录

- `d4d6002`：增加 GHCR 构建和手动生产部署。
- `642a270`：升级 GitHub 官方 Actions 运行时版本。
- `cef2fab`：修复镜像拉取失败时的回滚边界。
- GitHub Actions 运行 `35383194045`：最终 CI、Docker 构建和 GHCR 推送成功。
- 生产服务器当前应用未由 CI/CD 自动替换；首次正式部署仍需手动触发。
