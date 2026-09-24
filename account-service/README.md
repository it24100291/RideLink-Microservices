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

Updates only the authenticated account's own name.

Example request JSON:

```json
{
  "name": "Alice Updated"
}
```

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
