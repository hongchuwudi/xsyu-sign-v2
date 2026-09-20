---
name: xsyu-frontend
description: Build or modify the XSYU Sign Vue 3 frontend, including routes, Pinia session state, API calls, admin/user pages, Tailwind styling, messages, and modals. Use for current frontend work; do not revive the deleted legacy static SPA.
---

# XSYU Sign frontend

## Stack and location

- App: `xsyu-sign-web/`
- Vue 3 SFC, Vite, Pinia, Vue Router, Tailwind CSS 3.
- Do not upgrade to Tailwind 4 or introduce daisyUI 5.
- User-facing pages are grouped under `src/views/common`, `src/views/user`, and `src/views/admin`.

## Current routing

Vue Router uses `createWebHashHistory()`. Public URLs therefore use fragments such as `/#/admin/users`.

Hash history is intentional in the current single-JAR architecture: Nginx proxies every path to Spring Boot, and several frontend page paths overlap backend endpoints. A direct refresh of history-mode `/admin/users` reached the protected backend API and returned 401. Do not switch back to history mode without first separating frontend routes from API paths and adding a working SPA fallback.

The router guard reads persisted Pinia state and enforces login/admin routes. Login data is stored in `localStorage` under `userInfo`.

## Shared boundaries

- Add and reuse HTTP calls through `src/utils/api.js`; do not scatter Axios instances across views.
- Use `showMessage` from the existing message utility for user feedback.
- Reuse `BaseModal`, `ConfirmActionModal`, and existing admin/page components before creating alternatives.
- Shared profile/logout/binding actions belong in `src/composables/useUserActions.js`.
- Pinia logout must reset `userInfo` to a safe empty object rather than `null`, because current components may re-render before navigation completes.
- Use real backend endpoints; do not add mocks to production paths.

## API/session behavior

The Axios request interceptor reads the persisted JWT and sends `Authorization: Bearer <jwt>`. A genuine API 401 clears local session state. Static assets and page navigation must not be sent through protected API paths.

Never place OSS AccessKeys, database credentials, JWT secrets, school passwords, or RSA private keys in frontend code or Vite environment variables.

## UI work

Follow the existing compact pink/rose Tailwind design, responsive desktop/mobile layouts, and accessible labels. Preserve established component and message behavior unless the user requests a redesign.

The source-code page at `/code/XSYUOneKeySign.html` is a maintained static tool delivered with the application; avoid breaking static `/code/**` access when changing routing or backend interceptors.

## Validation

Run `npm run build` in `xsyu-sign-web/` after frontend changes. Do not run Playwright unless the user explicitly requests browser automation.
