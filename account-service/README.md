# Account Service

This service handles RideLink account creation.

## Register an account

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
