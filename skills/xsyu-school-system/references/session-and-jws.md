# CAS sessions, tickets, and JWSESSION

## Cookie roles

| Cookie | Typical scope | Purpose |
| --- | --- | --- |
| `SESSION` | `ids.xsyu.edu.cn/authserver` | CAS workflow state; binds execution, captcha, SMS, and QR state |
| `CASTGC` | CAS domain | remember-me / ticket-granting state observed after authentication |
| `JSESSIONID` | callback application | intermediate callback session during redirects |
| `JWSESSION` | `gwxg.xsyu.edu.cn` / school domain | campus API authentication |
| `WZXYSESSION` | observed related domain | additional campus product session |

Parse `Set-Cookie` header names case-insensitively. The CAS proxy has returned the header as lowercase `set-cookie`; direct lookup in a case-sensitive header map silently loses the session.

## Ticket exchange

CAS password, SMS, or QR authentication eventually returns:

```text
302 Location: <service-url>?ticket=ST-...
```

The project service callback is the gwxg CAS login endpoint under `/basicinfo/mobile/login/casLogin`.

The observed ticket-to-JWSESSION chain can contain five to eight redirects and may bounce from gwxg back to CAS for another ticket before JWSESSION is issued. A redirect cap of five previously caused false “redirect chain too long” failures; current code allows more headroom.

Follow the chain with one cookie jar, inspect every response for cookies, resolve relative `Location` values correctly, and stop at a bounded limit. Never print ticket or cookie values in normal logs.

Observed edge cases:

- Visiting the callback without a ticket redirects back to CAS.
- An invalid ticket may produce an HTTP 500 JSON response.
- JWSESSION can be issued near the end of the chain rather than on the first callback response.

## Campus API authentication

Campus APIs accept JWSESSION as a request header; the observed platform is case-insensitive and the project uses `JWSESSION`. A missing or expired session commonly returns:

```json
{"code":103,"message":"未登录,请重新登录"}
```

Do not equate the project JWT with school JWSESSION:

- Project JWT authenticates the user to XSYU Sign.
- JWSESSION authenticates XSYU Sign to the external campus service for that user.

## Verify session ownership

After SMS or QR authentication, call:

```text
POST https://gwxg.xsyu.edu.cn/basicinfo/mobile/my/index
JWSESSION: <session>
Content-Type: application/json

{}
```

The successful payload contains school identity data. Student/staff number priority:

1. `data.username`
2. `data.number` as fallback

Do not read `data.code`; that field belongs to a different SMS account-list shape.

Compare the returned number with the number the user requested before storing the session. Capture `data.name` and other profile fields only as needed by the project.

## Lifecycle in this project

The project stores JWSESSION for sign operations and tracks its refresh time. Automatic renewal uses the stored school password when available. Accounts created only through SMS or QR may have no stored school password and therefore cannot auto-renew until the user completes password binding/login once.

When renewal fails due to a school login outcome, retain the precise classification. Do not rewrite “bad password” as “locked”, and do not retry locked accounts continuously.
