# Account Service

This service handles RideLink account creation.

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
- role: `PASSENGER` or `DRIVER`
- iat and exp: expires 60 minutes after issuance

## Local signing key setup

Do not store a real private key in the repository. Supply one externally via environment variable or property.

PowerShell example:

```powershell
$env:ACCOUNT_JWT_PRIVATE_KEY = @'
-----BEGIN PRIVATE KEY-----
...replace with your real PKCS#8 RSA private key...
-----END PRIVATE KEY-----
'@
```

Or in `application.properties` (not committed to source control):

```properties
account.jwt.private-key=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

The same private key is used to sign JWTs. Other services should verify the token using the matching public key. Example:

```bash
openssl rsa -in private.pem -pubout -out public.pem
```

The public key should be distributed to other services for RS256 verification, while the private key stays only in the Account Service environment.
