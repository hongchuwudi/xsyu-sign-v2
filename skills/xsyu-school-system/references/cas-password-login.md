# CAS password login and captcha

## Endpoints and session bootstrap

CAS base: `https://ids.xsyu.edu.cn/authserver`.

Load the login page with the encoded campus callback as `service`:

```text
GET /authserver/login?service=<encoded-service-url>
```

The response is normally HTTP 200 HTML and creates an HttpOnly `SESSION` cookie scoped to `/authserver/`. Extract the current hidden `execution` value from this page. `execution` is single-use and bound to that `SESSION`.

## Password form

Submit to the same login URL using form encoding:

| Field | Value |
| --- | --- |
| `username` | student or staff number |
| `password` | password representation expected by the chosen flow |
| `execution` | latest value from the current response |
| `_eventId` | `submit` |
| `loginType` | `1` |
| `rememberMe` | normally `true` |
| `encrypted` | school page uses `true` when client-side RSA was applied |
| `authcode` | captcha value, only when required |

The observed school JavaScript uses RSA exponent `010001`, 1024-bit modulus, and emits a 256-character hexadecimal ciphertext. The server was also observed accepting plaintext when `encrypted` was omitted. Follow current project behavior instead of introducing a new password representation casually.

Expected outcomes:

- Success: HTTP 302 with `Location: <service>?ticket=ST-...`.
- Failure: HTTP 200 login HTML with a fresh `execution` and an error under `<span id="msg1">`.

Always replace the previous `execution` with the one from the latest failure page before another submission.

## Captcha

Captcha image:

```text
GET /authserver/captcha.jpg
```

It is observed as a 60x20 JPEG containing four digits and is bound to the current `SESSION`. A cache-busting `?tt=<random>` may be used when refreshing it.

The submission field is exactly `authcode`. A field named `captcha` is ignored by the school server.

Reliable activation detection is the actual active `authcode` input or equivalent active markup. Do not search for the Chinese word “验证码”; inactive pages may contain that word in commented or hidden UI.

## Error classification

Known message meanings:

| School text | Classification |
| --- | --- |
| `账号或密码错误。` | bad username or password |
| `验证码信息无效。` | missing, wrong, or expired captcha |
| `您的账号被锁定，请联系管理员。` | risk-control account lock |

Recommended decision order:

1. Detect explicit lock text and stop retries.
2. Detect active captcha markup and return a captcha-required result.
3. Parse `#msg1` and classify bad credentials or captcha errors from its text.
4. Preserve unknown school text as an external-system error rather than guessing “locked”.

Do not infer an account lock merely because captcha is active or credentials failed.

## Observed risk control

- Approximately three failed attempts from the same IP/session can activate captcha.
- Approximately five or six failures can lock the account.
- Continued retries can worsen the risk-control state.

These are observations from September 2026, not a contractual API. Code should react to the returned page, not hard-code a retry count as truth.

## Request fidelity

Use a browser-like User-Agent and the login page as `Referer`. Keep redirects disabled while determining whether CAS issued a service ticket; follow the later ticket chain deliberately with shared cookies.
