# CAS QR and alternate login modes

## QR login

The CAS login page exposes a generated QR URL similar to:

```text
/authserver/generateQRCode?loginLT=<session-specific-value>
```

Fetch it with the current CAS `SESSION`; the observed response is a 200x200 PNG. The user scans and confirms it with the campus application.

Poll within the same CAS session:

```text
POST /authserver/analogLogin
X-Requested-With: XMLHttpRequest
```

Observed states:

- Empty HTTP 200 body: still waiting.
- `success`: confirmed.
- Other text such as `userlimit`: explicit school-side terminal or exceptional state.

After `success`, reload the login page in the same session. The authenticated CAS session then redirects to the configured service with a ticket. Exchange the ticket, query `my/index`, and reject the login if its student number differs from the number entered before QR creation.

The school page polls around every two seconds. Application code should also enforce its own expiry and avoid writing an operation log entry for every waiting poll.

## Face login observations

The school login page also contains a `loginType=4` face flow:

- Browser camera capture and client-side human-face checks.
- `POST /cas/faceValid/checkHumanImg` for face frame validation.
- Form fields include `faceData`, `face_username`, and `loginType=4`.
- Cookie `sudy_face` tracks failures; the observed page forces mode reselection after three failures.

XSYU Sign does not currently implement face login. Treat this section as reverse-engineering context, not an instruction to add the feature without a user request.

## Other observed CAS entry points

The page has contained QQ, Weibo, WeLink, and mini-program login integrations. They are outside the current product login contract. Do not couple project authentication code to them unless the user explicitly scopes that work and the current school page is re-inspected.

## Sensitive values

Do not log or commit QR session IDs, QR images tied to a live session, CAS cookies, tickets, or the resulting JWSESSION.
