# CAS SMS login

SMS login is a session workflow, not a direct `loginType=2` form submission.

## Required sequence

1. Load the CAS login page and retain its `SESSION` cookie.
2. Send the SMS code:

```text
POST /authserver/smsLogin/sendSms
Content-Type: application/x-www-form-urlencoded
X-Requested-With: XMLHttpRequest

request_username=<phone>
```

Observed response:

```json
{"success":true}
```

Failures use `success:false` with `errormsg`. The observed cooldown is about 60 seconds.

3. Verify the code in the same CAS session:

```text
GET /authserver/maccountNoLoginValid/smsValid?phone=<phone>&smsCode=<code>
```

Observed successful shape:

```json
{
  "code": 1,
  "isExitMultipleAccount": false,
  "data": [
    {"code":"student-number","name":"display-name","category":"identity","isDefault":true}
  ],
  "msg": "..."
}
```

4. If multiple accounts are available, select the requested identity:

```text
GET /authserver/maccountNoLoginValid/accountValid?loginName=<student-number>&isDefault=<boolean>
```

For this endpoint, observed `code == 0` means failure and a non-zero code means success.

5. Reload the CAS login page in the same authenticated session. It should redirect to the service callback with a ticket.
6. Exchange the ticket for JWSESSION, query `my/index`, and verify that the returned student number matches the student number requested by the user.

## Non-obvious constraints

- `sendSms`, `smsValid`, optional `accountValid`, and the final page reload must share one cookie jar. Losing `SESSION` produces school errors such as `认证信息无效。`.
- The phone number may map to multiple school identities. Do not silently use the first identity.
- The account list uses `data[].code` for the student/staff number. This differs from `my/index`, where the authoritative field is `data.username` with `data.number` as fallback.
- SMS codes and phone numbers are sensitive personal data. Do not include codes in logs, exceptions, fixtures, or committed files.
- Do not resend automatically in a loop; respect the server cooldown and explicit user action.
