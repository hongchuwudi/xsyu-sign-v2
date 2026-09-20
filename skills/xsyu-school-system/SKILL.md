---
name: xsyu-school-system
description: Diagnose or modify XSYU CAS authentication, captcha, SMS/QR login, ticket-to-JWSESSION exchange, campus identity lookup, or campus sign APIs in this repository. Use for school-system protocol behavior; do not use for unrelated local admin authentication or generic frontend work.
---

# XSYU School System

Use this skill before changing code that communicates with `ids.xsyu.edu.cn` or `gwxg.xsyu.edu.cn`.

## Route to the relevant reference

- Password login, captcha, `execution`, error classification, or account lock: read [references/cas-password-login.md](references/cas-password-login.md).
- SMS authentication or multiple-account selection: read [references/sms-login.md](references/sms-login.md).
- QR polling, face login, or other CAS entry points: read [references/qr-login.md](references/qr-login.md).
- Cookies, redirects, ticket exchange, JWSESSION, or current-school-user lookup: read [references/session-and-jws.md](references/session-and-jws.md).
- Sign list, sign detail, area sign, or discovery of other campus APIs: read [references/campus-sign-api.md](references/campus-sign-api.md).

Read only the references relevant to the current task. For a cross-flow authentication change, read all authentication references.

## Repository touchpoints

- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/utils/XSYULoginUtil.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/utils/CasLoginResult.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/webClient/BaseSignService.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/service/impl/UserServiceImpl.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/service/impl/SignServiceImpl.java`
- `docs/xsyu-school-system-analysis.md`
- `docs/ai-memory/cas-login-behavior.md`

Inspect current code before applying historical notes. The source code and latest migrations are authoritative when old memory files disagree with the current model.

The five references contain the operationally relevant protocol details. When a task needs an observation or endpoint not summarized there, read `docs/xsyu-school-system-analysis.md` completely; it is the exhaustive captured reverse-engineering record and should remain the single canonical raw catalog rather than being duplicated and allowed to drift.

## Invariants

- A CAS `SESSION`, its `execution`, captcha, SMS verification, and QR state belong to one browser-like session. Do not split a flow across independent cookie stores.
- Match response header names case-insensitively; the CAS proxy has returned lowercase `set-cookie`.
- Do not classify login outcomes from HTTP status alone. CAS failures commonly return HTTP 200 HTML.
- Verify the school identity returned by `my/index` after SMS or QR login before storing a JWSESSION.
- Treat school endpoints and UI structure as external, changeable dependencies. Preserve diagnostic information without logging credentials or full session tokens.

## Safety

- Never log or commit school passwords, SMS/captcha values, `SESSION`, `CASTGC`, tickets, or full `JWSESSION` values.
- Do not repeatedly test a real account with bad credentials. Observed risk control activates captcha after roughly three failures and may lock the account after roughly five or six.
- When a response indicates lock, stop automatic retries.
- Real CAS tests are observable integration tests and can affect an account or send SMS. Run them only when the user explicitly requests that external action.
