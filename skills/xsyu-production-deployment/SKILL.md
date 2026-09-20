---
name: xsyu-production-deployment
description: Inspect, build, release, or troubleshoot XSYU Sign CI/CD and production deployment, including GitHub Actions, GHCR, Docker, Nginx, TLS, runtime configuration, health checks, and rollback. Use only when deployment or production operations are in scope.
---

# XSYU Sign production deployment

## Current architecture

```text
Browser HTTPS
  -> nginx-https container
  -> host port 11451
  -> qq-robot Spring Boot container
  -> Vue static files embedded in the Spring Boot JAR + backend APIs
```

For `xsyusign.hongchu.xyz`, current Nginx proxies `location /` to `http://172.17.0.1:11451`. It does not independently host the Vue build and does not distinguish frontend paths from API paths. The frontend therefore uses hash routing.

## Build image

The root `Dockerfile` is multi-stage:

1. Node 22 runs `npm ci` and builds `xsyu-sign-web`.
2. Maven/Java 17 copies the frontend `dist` into Spring Boot static resources and packages the JAR.
3. Java 17 JRE runs `/app/app.jar` on port 11451.

Image repository: `ghcr.io/hongchuwudi/xsyu-sign-v2`.

## CI/CD behavior

- CI builds frontend, backend, and Docker image.
- A push to `main` publishes GHCR tags including `latest` and `sha-<commit>`.
- Production deployment is manually triggered through the `Deploy production` GitHub Actions workflow.
- Prefer an immutable full SHA image tag for production.

The deployment workflow connects over SSH and runs `/opt/xsyu-sign/deploy.sh <tag>`.

## Server runtime

| Item | Current value |
| --- | --- |
| App container | `qq-robot` |
| Temporary rollback container | `qq-robot-backup` |
| App port | `11451` |
| Runtime env file | `/etc/xsyu-sign.env` |
| External config | `/etc/xsyu-sign/config/` mounted read-only |
| Persistent logs | `/home/hongchu/qqrobot/` |
| Internal health check | `http://127.0.0.1:11451/` |
| Public health check | `https://xsyusign.hongchu.xyz/` |

The script pulls the requested image, preserves the old container, starts the new one, polls health, deletes the backup on success, and restores it on failure. It does not roll back SQL migrations, Nginx, certificates, MySQL, Redis, or other services.

## Operational rules

- Production inspection is read-only unless the user explicitly asks to deploy or change server state.
- Apply required database migrations before starting code that expects new columns/tables. Prefer backward-compatible migrations because application rollback does not roll back SQL.
- Never display or commit `/etc/xsyu-sign.env`, database passwords, SSH keys, GitHub secrets, OSS keys, JWT keys, or encryption master keys.
- Resolve and validate exact container/config targets before destructive Docker or filesystem commands.
- After deployment, verify container image/tag, running state, internal health, public HTTPS, and recent startup logs.
- Nginx certificate renewal/reload is separate from the app deployment script.

## Repository references

- `.github/workflows/ci.yml`
- `.github/workflows/deploy-production.yml`
- `deploy/production/deploy.sh`
- `deploy/production/xsyu-sign.env.example`
- `Dockerfile`
- `docs/cicd-deployment.md`

Use the environment-provided server access skill when it is available; this repository skill intentionally contains no private key path or credentials.
