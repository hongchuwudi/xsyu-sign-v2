---
name: xsyu-auth-user-model
description: Modify or diagnose XSYU Sign users, login methods, password storage, project JWT authorization, JWS refresh, or user schema migrations. Use for repository authentication and identity work after the external school protocol is understood.
---

# XSYU Sign authentication and user model

## Current model

- `user.username` is the student/staff number and the ordinary user’s unique identity.
- Ordinary users do not have a separate local registration flow. First successful school login creates the record.
- Supported school login methods are password, SMS, and QR.
- The configured administrator is the exception: it uses local PBKDF2 password verification and must have role `ADMIN`.
- Project JWT carries the user identity and role for XSYU Sign API authorization. It is distinct from the school JWSESSION.

## Password column

The current schema uses one `user.password` BLOB:

- `ADMIN`: PBKDF2 password hash.
- ordinary user with stored school password: AES-GCM ciphertext using `XSYU_STUDENT_PASSWORD_MASTER_KEY`.
- SMS/QR-only user who never bound a school password: `NULL`.

The old `stu_password` column was removed. Do not reintroduce it or trust old memory that still describes a two-column model. Before any password migration, back up the table, count affected rows, validate decryption with the old key, generate replacement values, and only then update production data. Never use a migration that clears ordinary-user passwords as an intermediate step.

`user.id` is `BIGINT UNSIGNED`; referencing columns must match exactly.

## Main code

- `UserServiceImpl`: login, account creation, password binding, JWS refresh, JWT creation.
- `AdminServiceImpl`: administrator operations and user creation.
- `CryptoUtils`: password hashing and AES-GCM helpers.
- `CredentialEncryptionProperties`: master-key binding.
- `JwtInterceptor` and `AdminAuthInterceptor`: request authorization.
- `UserController`: password, SMS, QR, and binding endpoints.

For CAS behavior, use the sibling `xsyu-school-system` skill and read the relevant reference.

## Login outcomes

Keep school outcomes distinguishable:

- bad account/password;
- captcha required or invalid;
- account locked;
- SMS/QR waiting, expired, mismatched identity, or external failure.

Do not infer “locked” from a generic failure. Preserve the school error type through service and controller layers so the frontend can display the correct message.

## JWS refresh

- Refresh uses `username` plus the decrypted ordinary-user school password.
- If `password` is `NULL`, do not blindly retry; the user must bind/login with a school password first.
- Never run the school-password path for an administrator.
- Update `jwsRefreshedAt` only when a new JWSESSION is successfully obtained.

## Security

- Master keys, JWT secrets, RSA private keys, school passwords, captchas, SMS codes, JWTs, and JWSESSION values must come from runtime configuration and must not enter Git or normal logs.
- Frontend may receive only the RSA public key used for transport encryption.
- `/admin/**` requires both a valid project JWT and administrator-role verification.
- Authentication tests that contact the real school are observable and potentially state-changing; inspect tests before running them.

## Verification

After changes, build the backend with `mvn package -DskipTests`. Run real CAS tests only when explicitly requested and stop on captcha or account-lock responses.
