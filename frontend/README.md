# Frontend

## Gateway + OIDC Setup (Local Dev)

This UI is configured to call the API gateway only and authenticate with Keycloak.

### Environment

Set the following in `frontend/.env.local`:

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=trade-settlement
VITE_KEYCLOAK_CLIENT_ID=trade-api
```

### Behavior

- All API traffic goes through `VITE_API_BASE_URL`.
- A bearer token is attached on each axios request.
- On `401`, the app redirects back into Keycloak login.
- `/api/v1/ai/*` calls are made only for users with the `admin` role.
- Commentary approval controls are available only to `compliance-officer` and `admin`.

### Local run order

1. Start infra and backend services first (`8082`, `8083`, `8084`, then gateway `8080`).
2. Ensure Keycloak realm/client are available at `http://localhost:8180`.
3. Run frontend with `npm run dev`; UI should use gateway ingress at `http://localhost:8080`.
