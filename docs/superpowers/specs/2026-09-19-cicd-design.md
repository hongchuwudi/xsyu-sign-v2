# XSYU Sign CI/CD 设计

## 目标

为 `hongchuwudi/xsyu-sign-v2` 建立两段式流水线：

- 推送或提交 PR 到 `main` 时自动验证和构建。
- 生产部署只能从 GitHub Actions 手动触发。
- 构建产物使用 GHCR Docker 镜像，生产服务器不再现场编译源码或等待人工上传 JAR。
- 部署失败时自动恢复部署前的 `qq-robot` 容器。

## 当前生产环境

- 应用容器：`qq-robot`
- 对外端口：`11451:11451`
- Nginx：`xsyusign.hongchu.xyz` 反向代理到宿主机 `11451`
- 日志目录：`/home/hongchu/qqrobot` 挂载到容器 `/app/logs`
- 运行时环境变量文件：`/etc/xsyu-sign.env`
- 当前镜像由服务器上的 `/home/hongchu/docker-sign/Dockerfile` 手工构建。

Nginx、Let's Encrypt 证书、MySQL、Redis 不属于应用流水线的发布范围。

## 方案选择

采用 GitHub Actions + GHCR + SSH 部署：

1. GitHub Actions 构建 Vue 前端和 Spring Boot 后端。
2. 生成单个运行镜像并推送到 `ghcr.io/hongchuwudi/xsyu-sign-v2`。
3. 手动触发生产部署工作流。
4. 工作流通过 SSH 调用服务器部署脚本。
5. 新容器未通过健康检查时，恢复旧容器。

不采用服务器现场构建，避免 Maven、Node.js 和源码进入生产机。不采用自托管 Runner，避免 GitHub 工作流任务直接获得生产服务器常驻执行能力。

## 镜像构建

仓库增加多阶段 `Dockerfile`：

1. Node 阶段执行 `npm ci` 和 `npm run build`。
2. 将 `xsyu-sign-web/dist` 复制到 Spring Boot 的 `src/main/resources/static` 构建上下文。
3. Maven 阶段使用 Java 17 执行测试后的生产打包。
4. 运行阶段只包含 JRE、应用 JAR 和必要系统文件。

镜像不得包含 AccessKey、数据库密码、邮件授权码、JWT 密钥或本地忽略的生产配置文件。生产配置统一从 `/etc/xsyu-sign.env` 注入。

镜像标签：

- `sha-<完整或短提交号>`：不可变部署版本。
- `latest`：`main` 最新成功构建，仅作默认选择，不作为回滚依据。

## 自动构建工作流

文件：`.github/workflows/ci.yml`

触发条件：

- `pull_request` 指向 `main`
- `push` 到 `main`

执行内容：

1. 检出源码。
2. 使用锁文件安装前端依赖并构建 Vue。
3. 使用 Java 17 运行不访问真实外部系统的后端单元测试并完成打包。现有会连接真实数据库、邮件和 CAS 的观测测试不进入 CI。
4. 构建 Docker 镜像。
5. PR 只验证构建，不推送镜像。
6. `main` 推送使用 `GITHUB_TOKEN` 登录 GHCR，并推送 SHA 标签和 `latest`。

工作流使用最小权限：默认只读；仅镜像发布任务授予 `packages: write`。

## 手动生产部署工作流

文件：`.github/workflows/deploy-production.yml`

触发方式：`workflow_dispatch`，输入待部署镜像标签，默认使用 `latest`。工作流归属 GitHub `production` Environment，便于后续增加审批人和部署记录。

部署步骤：

1. SSH 连接 `49.232.16.204`。
2. 拉取指定 GHCR 镜像。
3. 记录当前 `qq-robot` 容器及镜像信息。
4. 停止旧容器并改名为 `qq-robot-backup`，暂不删除。
5. 使用原端口、日志目录和 `/etc/xsyu-sign.env` 启动新 `qq-robot`，并设置 `--restart=always`。
6. 最多等待 90 秒，轮询 `http://127.0.0.1:11451/`。
7. 健康检查成功后删除备份容器。
8. 健康检查失败时删除新容器，将备份容器恢复原名并启动，然后让工作流失败。

使用部署并发锁，确保同一时间只有一个生产部署任务。

## GitHub Secrets

生产 Environment 中配置：

- `PROD_SSH_HOST`：`49.232.16.204`
- `PROD_SSH_PORT`：`22`
- `PROD_SSH_USER`：`root`
- `PROD_SSH_PRIVATE_KEY`：生产部署专用 SSH 私钥

优先创建仅用于部署的新 SSH 密钥，不直接复用个人管理密钥。公钥放入服务器 `authorized_keys`，私钥仅存 GitHub Environment Secret。

若 GHCR 包保持私有，服务器还需配置只读 Packages 凭据；凭据只保存在服务器 root 可读文件中，不通过命令行参数或仓库文件传递。若包设置为公开，则服务器可直接拉取。

## 服务器部署脚本

仓库维护 `deploy/production/deploy.sh`，并同步到服务器 `/opt/xsyu-sign/deploy.sh`。脚本必须：

- 使用 `set -Eeuo pipefail`。
- 只接受符合预期格式的镜像标签。
- 检查 `/etc/xsyu-sign.env` 和日志目录存在。
- 保留旧容器直到新版本健康。
- 捕获失败并自动执行回滚。
- 不打印环境变量内容。
- 不删除数据库、Redis、Nginx 或其他容器。

## 配置与密钥边界

- Git 仓库：只保存配置模板和变量名。
- GitHub Secrets：只保存部署 SSH 凭据。
- 服务器 `/etc/xsyu-sign.env`：保存应用运行时配置和密钥，权限设为 `600`。
- Docker 镜像：不得携带任何环境专用秘密。

现有生产配置需要在首次 CI/CD 部署前核对并补齐到 `/etc/xsyu-sign.env`，防止当前 JAR 内嵌配置在新镜像中缺失。

## 验证与回滚

上线前验证：

- 前端 `npm run build`
- 后端测试
- Docker 镜像构建
- 部署脚本 Shell 语法检查

上线后验证：

- 容器为运行状态。
- `127.0.0.1:11451` 在 90 秒内返回成功 HTTP 状态。
- `https://xsyusign.hongchu.xyz/` 可访问。

自动回滚只恢复应用容器，不回滚数据库迁移。因此包含不兼容 SQL 迁移的版本必须先执行向后兼容迁移，并在部署说明中明确标注。

## 首次接入顺序

1. 增加 Dockerfile、忽略规则和两套 GitHub Actions 工作流。
2. 增加生产部署脚本和配置模板。
3. 在服务器补齐环境变量并安装部署脚本。
4. 创建专用 SSH 部署密钥并配置 GitHub `production` Environment。
5. 推送一次 `main`，验证 CI 和 GHCR 镜像。
6. 手动触发首次生产部署，确认健康检查与回滚路径。
