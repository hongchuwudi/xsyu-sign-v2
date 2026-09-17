# 油签机（XSYU Sign）

西安石油大学校园签到辅助系统 —— Monorepo

## 📁 目录结构

```
xsyu-sign/
├── xsyu-sign-server/   # 后端：Spring Boot 3.5 + Java 17（Maven）
├── xsyu-sign-web/      # 前端：Vue 3 + Vite + Pinia + Tailwind（新，开发中）
├── docs/               # 文档（校园系统接口分析等）
├── migrations/         # 数据库迁移脚本
├── switch-env.ps1      # 一键切换环境（dev/test/prod/prods）
└── .env                # 环境配置（gitignore，switch-env 读取）
```

> ⚠️ 过渡期说明：旧前端仍在 `xsyu-sign-server/src/main/resources/static/`（随 jar 部署）。
> 新前端 `xsyu-sign-web` 开发完成后替换它，届时把 Vite 构建输出指向 `static/`。

## 🚀 本地开发

**后端**（:11451）
```bash
cd xsyu-sign-server
mvn spring-boot:run
```

**前端**（:5173，API 自动代理到 :11451）
```bash
cd xsyu-sign-web
npm install
npm run dev
```

## 📦 构建部署

**后端打包**（当前生产方式：内置旧前端）
```bash
cd xsyu-sign-server
mvn package -DskipTests
```

**前端构建**（新前端接管后）
```bash
cd xsyu-sign-web
npm run build        # 产物在 dist/
```

**切换环境**（改数据库 profile + 前端 API 地址）
```powershell
.\switch-env.ps1 prod
```

## 📚 文档

- [校园系统接口分析](docs/xsyu-school-system-analysis.md) — CAS 登录/验证码/短信/扫码实测细节
- [后端说明](xsyu-sign-server/README.md) — 架构、定时任务、API 清单
