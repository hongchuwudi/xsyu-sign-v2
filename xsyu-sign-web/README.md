# xsyu-sign-web

油签机前端 —— Vue 3 + Vite + Pinia + Vue Router + Tailwind CSS 3

## 开发

```bash
npm install
npm run dev        # http://localhost:5173，API 自动代理到 :11451
```

## 构建

```bash
npm run build      # 产物在 dist/
```

## 结构

```
src/
├── main.js            # 入口：createApp + Pinia + Router
├── App.vue            # <router-view />
├── router/index.js    # 路由
├── stores/user.js     # 用户状态（Pinia，localStorage 持久化）
├── utils/api.js       # axios 实例（自动带 JWT、401 自动登出）
├── views/             # 页面
└── components/        # 组件
```

## API 地址

- 开发：走 Vite 代理（`vite.config.js`），无需配置
- 生产：`switch-env.ps1` 写入 `.env.production` 的 `VITE_API_BASE`；
  若与后端同域部署（构建产物拷入 Spring Boot static），留空即可
