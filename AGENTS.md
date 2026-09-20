# XSYU Sign Agent Notes

This repository is the `D:\code\xsyu-sign` monorepo for XSYU Sign.

## Project Shape

- `xsyu-sign-server/`: Spring Boot 3.5 + Java 17 backend.
- `xsyu-sign-web/`: Vue 3 + Vite + Pinia + Tailwind frontend.
- `docs/`: system analysis, migration notes, and long-lived project memory.
- Old static frontend still exists under `xsyu-sign-server/src/main/resources/static/` during the transition period.

## Commands

- Backend dev: run `mvn spring-boot:run` in `xsyu-sign-server/`.
- Frontend dev: run `npm run dev` in `xsyu-sign-web/`.
- Frontend build: run `npm run build` in `xsyu-sign-web/`.
- Backend package: run `mvn package -DskipTests` in `xsyu-sign-server/`.

## Current Direction

- The application model is "student number is username"; there is no separate local user registration flow for ordinary users.
- Login supports school password, SMS, and QR. Admin login is the exception and uses local password verification.
- The independent Vue frontend is replacing the old static SPA. Keep new work in `xsyu-sign-web/` unless explicitly maintaining the old static frontend.
- User-facing pages are organized by route groups: `common`, `user`, and `admin`.

## Long-Lived Context

Read these before touching login, user identity, CAS, JWS refresh, or the frontend migration:

- `docs/ai-memory/cas-login-behavior.md`
- `docs/ai-memory/user-model-refactor.md`
- `docs/ai-memory/frontend-handoff.md`
- `docs/xsyu-school-system-analysis.md`

Sensitive local environment notes are intentionally ignored by git in `docs/ai-memory/local-private.md`.

## Repository Skills

Project-specific reusable instructions live under `skills/`. Read the matching `SKILL.md` before working in these areas:

- `skills/xsyu-school-system/SKILL.md`: CAS password/captcha, SMS, QR, ticket/JWSESSION, identity lookup, and campus sign API reverse engineering.
- `skills/xsyu-auth-user-model/SKILL.md`: user schema, login methods, password encryption, project JWT, admin authorization, and JWS refresh.
- `skills/xsyu-frontend/SKILL.md`: Vue 3 frontend structure, Hash routing, Pinia session state, API conventions, and Tailwind 3 UI work.
- `skills/xsyu-production-deployment/SKILL.md`: GitHub Actions, GHCR, Docker deployment, Nginx topology, health checks, and rollback.
- `skills/xsyu-storage-media/SKILL.md`: Aliyun OSS, Markdown image uploads, media tables, asset association, and cleanup lifecycle.

The school-system skill uses focused references under its `references/` directory. Load only the references relevant to the task. Never copy secrets or values from ignored local notes into a skill or tracked document.
