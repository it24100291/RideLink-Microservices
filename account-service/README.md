# Account Service

This service handles RideLink account creation, login, and protected account operations.

## Register an account

Base URL:

http://localhost:8083

Endpoint:

POST /api/accounts/register

Example request JSON:

```json
{
  "name": "Alice Johnson",
  "email": "alice@example.com",
  "password": "Secret123",
  "role": "PASSENGER"
}
```

Notes:
- `PASSENGER` and `DRIVER` are allowed.
- `ADMIN` is not allowed for public registration.
- Passwords are stored as BCrypt hashes.
- The API never returns the password.

## Log in and receive a token

Endpoint:

POST /api/auth/login

Example request JSON:

```json
{
  "email": "alice@example.com",
  "password": "Secret123"
}
```

Example response:

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "expiresAt": "2026-09-24T12:00:00Z"
}
```

The token is RS256 signed and includes:
- issuer: `ridelink-account-service`
- audience: `ridelink-api`
- subject: the stable Account Service account ID as a string
- role: `PASSENGER`, `DRIVER`, or `ADMIN`
- iat and exp: expires 60 minutes after issuance

## Protected account endpoints

Use the token in the `Authorization` header as a Bearer token.

### GET /api/accounts/me

Returns the authenticated account profile using the subject in the verified token.

Example:

```http
GET /api/accounts/me
Authorization: Bearer <token>
```

### PATCH /api/accounts/me

Updates the authenticated account's own name and/or email. Omitted or null fields are left unchanged; at least one name or email must be supplied. Blank values are rejected. IDs, roles, and status cannot be changed through this endpoint.

Example request JSON:

```json
{
  "name": "Alice Updated",
  "email": "alice.updated@example.com",
  "currentPassword": "Secret123"
}
```

- A name-only request remains supported and does not require the password.
- Changing email requires the current password. Email addresses are trimmed, normalized to lowercase, and must be unique (case-insensitive).
- Log in using the new email after a change. This endpoint does not verify ownership of the new email through an email message.
- Invalid input or an incorrect current password returns `400`; a duplicate email or concurrent update returns `409`.
- Responses contain only `id`, `name`, `email`, `role`, and `status`.

### PATCH /api/accounts/me/password

Requires the authenticated account's bearer token and current password:

```json
{
  "currentPassword": "Secret123",
  "newPassword": "NewSecret456!"
}
```

Returns `204` with no response body. The new password must differ from the old one and contain at least 6 characters and at most 72 UTF-8 bytes (the BCrypt limit). It is stored as a BCrypt hash. Missing/incorrect current passwords and invalid new passwords return `400`.

After success, all older tokens for this account are rejected by Account Service. Log in again with the new password.

### PATCH /api/accounts/{id}/role

Requires a bearer token belonging to an active administrator. The administrator's current database role determines permission.

```json
{
  "role": "DRIVER"
}
```

Allowed roles: `PASSENGER`, `DRIVER`, `ADMIN`. Returns the updated profile (`200`). Public registration as ADMIN remains prohibited. Administrators cannot demote themselves (`409`); another active administrator must make that change. Non-admin callers receive `403`, missing targets `404`, and invalid/missing roles `400`.

A role change invalidates the target account's older tokens in Account Service. The target must log in again to receive a JWT with its new role. Assigning the existing role leaves tokens valid.

### Token invalidation and integration

Tokens include `tokenVersion`. Account Service checks this against the account record on protected requests. Password and role changes increment it. Legacy tokens without this claim are treated as version zero and stop working after the first increment.

Other services that only verify JWT signatures cannot detect these changes immediately. Driver and Ride integration must also check account/token state if immediate revocation is required. Changing an account role does not create or remove a Driver profile or alter existing rides.

The account entity also uses optimistic locking: competing updates return `409` rather than silently overwriting newer account data. The default H2 database is in memory; accounts and these versions reset when the service restarts.

### PATCH /api/accounts/{id}/status

Requires an authenticated `ADMIN` token. Valid status values are `ACTIVE` and `SUSPENDED`.

Example request JSON:

```json
{
  "status": "SUSPENDED"
}
```

Response codes:
- `200` for successful status changes
- `401` for missing, invalid, expired, or malformed tokens
- `403` for valid tokens without admin permission
- `404` for a missing target account

## Local signing key setup

Do not store a real private key or public key in the repository. Supply them externally via environment variables or properties.

PowerShell example:

```powershell
$env:ACCOUNT_JWT_PRIVATE_KEY = @'
-----BEGIN PRIVATE KEY-----
...replace with your real PKCS#8 RSA private key...
-----END PRIVATE KEY-----
'@

$env:ACCOUNT_JWT_PUBLIC_KEY = @'
-----BEGIN PUBLIC KEY-----
...replace with the matching RSA public key...
-----END PUBLIC KEY-----
'@
```

Or in `application.properties` (not committed to source control):

```properties
account.jwt.private-key=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
account.jwt.public-key=-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----
```

The private key signs JWTs. The matching public key is used to verify incoming bearer tokens, including RS256 algorithm, issuer `ridelink-account-service`, audience `ridelink-api`, and expiration checks.

Example:

```bash
openssl rsa -in private.pem -pubout -out public.pem
```

The public key should be distributed to services that validate tokens, while the private key stays only in the Account Service environment.

## Bootstrap the first ADMIN for local demonstration

This bootstrap is intentionally disabled in the default profile. It runs only when:
- the Spring profile is `dev`, and
- `account.bootstrap.admin.enabled=true`

The email and password must be supplied in environment variables for the local shell session only.

Safe PowerShell setup:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:ACCOUNT_BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:ACCOUNT_BOOTSTRAP_ADMIN_PASSWORD = "A-Strong-Local-Password-Change-Later!"
$env:ACCOUNT_BOOTSTRAP_ADMIN_ENABLED = "true"
# or set the equivalent property in a local-only application-dev.properties file
# account.bootstrap.admin.enabled=true
# account.bootstrap.admin.email=admin@example.com
# account.bootstrap.admin.password=A-Strong-Local-Password-Change-Later!
```

Then start the service without committing any of this into source control:

```powershell
Set-Location "C:\Users\thula\it3130-git-basics-lab\RideLink-Microservices\account-service"
mvn spring-boot:run
```

The bootstrap creates exactly one `ACTIVE` `ADMIN` account if no matching email already exists. It hashes the password with BCrypt and never prints the password or hash. After the first successful run, disable it with:

```powershell
$env:ACCOUNT_BOOTSTRAP_ADMIN_ENABLED = "false"
# or remove the dev profile and the admin bootstrap env vars from the session
Remove-Item Env:ACCOUNT_BOOTSTRAP_ADMIN_EMAIL, Env:ACCOUNT_BOOTSTRAP_ADMIN_PASSWORD, Env:ACCOUNT_BOOTSTRAP_ADMIN_ENABLED
```

Do not store credentials in the repo, and do not enable this in the default profile.
