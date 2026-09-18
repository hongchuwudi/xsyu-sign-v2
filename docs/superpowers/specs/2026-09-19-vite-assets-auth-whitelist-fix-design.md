# Vite 静态资源 401 修复设计

## 问题

生产首页能够返回 `index.html`，但其中引用的 `/assets/*.js` 和 `/assets/*.css` 被 `JwtInterceptor` 拦截并返回 401。现有静态资源白名单仍使用旧前端的 `/js/**`、`/css/**` 等路径，没有覆盖 Vite 默认输出目录 `/assets/**`。

## 方案

在 `WebConfig` 中同时将 `/assets/**` 加入 `RateLimitInterceptor` 和 `JwtInterceptor` 的排除路径。管理员拦截器只匹配 `/admin/**` 等业务接口，无需修改。

不调整 Vite 输出目录，不按 `.js`、`.css` 等扩展名做全局放行，也不改变业务 API 的认证规则。这样只开放 Spring Boot 静态资源目录下的 Vite 资源路径，保持修复范围最小。

## 验证

1. 运行后端 Maven 打包，确认配置修改可编译。
2. 推送提交，等待 CI 构建新的 Docker 镜像。
3. 手动部署对应 SHA 镜像。
4. 验证 `/`、实际 `/assets/*.js` 和 `/assets/*.css` 均返回 HTTP 200。
5. 验证未携带 JWT 的业务接口仍返回未授权，避免扩大认证边界。

## 回滚

若部署健康检查失败，现有部署脚本自动恢复旧容器。代码层回滚只需移除两个拦截器白名单中的 `/assets/**`。
