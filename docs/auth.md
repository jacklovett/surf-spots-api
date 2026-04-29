# Authentication and session security

This document describes how authentication works across **surf-spots** (Remix app) and **surf-spots-api** (Spring Boot API). It is aimed at developers and operators maintaining or reviewing the system.

## Goals

- Authenticate users with a **signed HTTP-only session cookie** shared between the Remix server and the API (`credentials: 'include'`).
- Avoid **IDOR**: the API derives the current user from the verified session, not from client-supplied user ids on protected operations.
- Align with common **OWASP** guidance: minimal session payload, password policy (NIST SP 800-63B style), no account enumeration on login or forgot-password, rate limiting, safe password-reset tokens, and **defence in depth** against CSRF for cookie-backed requests.

## High-level architecture

1. **Remix** issues and reads a **`session` cookie** (signed with `SESSION_SECRET`). The cookie stores only **minimal identity** (`id`, `email`, `name`) as `SessionUser`.
2. **Remix loaders/actions** call the API with `Cookie` forwarded and `credentials: 'include'` where applicable so the same cookie reaches the API.
3. The **API** verifies the cookie with **`SessionCookieFilter`** / **`SessionCookieVerifier`** (HMAC, shared `SESSION_SECRET`), then exposes the user id as the Spring Security principal.
4. **Rich profile data** (country, emergency contacts, settings, etc.) is loaded on demand via **`GET /api/user/me`**, not stored in the session cookie.

## Session cookie (Remix)

Configured in `surf-spots/app/services/session.server.ts` via `createCookieSessionStorage`:

| Attribute   | Purpose |
|------------|---------|
| `httpOnly` | JavaScript cannot read the token (mitigates XSS token theft). |
| `secure`   | HTTPS-only in production. |
| `sameSite: 'lax'` | Browser does not send the cookie on cross-site POSTs; primary CSRF mitigation for typical browsing. |
| `path: '/'` | Cookie available to the Remix app origin. |
| Signed with `SESSION_SECRET` | Tampering invalidates the cookie. |

**Secret:** `SESSION_SECRET` must be set in the Remix environment and **must match** the API `app.auth.session-secret` (see API `.env.example`). Never commit real secrets.

## Session verification (API)

- **`SessionCookieFilter`** runs early in the Spring Security chain and verifies the `session` cookie.
- **`AuthenticatedUserResolver`** is the supported way to read `requireCurrentUserId()` / `currentUserIdOrNull()` in controllers and services.
- Protected routes require an authenticated principal; **`/api/auth/**`** and public surf-spot catalogue routes stay permit-all as configured in `SecurityConfig`.

## CSRF and cross-origin policy

We use a **cross-origin** browser app (e.g. `https://app.example`) talking to a **separate API origin** (e.g. `https://api.example`). Spring Security’s **synchronizer-token CSRF** is **disabled** because the SPA does not embed a server-generated CSRF token in each form.

Instead we use **defence in depth**:

1. **SameSite=Lax** on the session cookie (see above).
2. **Strict CORS** via **`AllowedOrigins`** (`CorsConfig`), driven by `cors.allowed-origins` (comma-separated list; see API `.env.example` for `CORS_ALLOWED_ORIGINS`).
3. **`CsrfOriginFilter`**: for every **non-safe** method (not `GET`, `HEAD`, `OPTIONS`), require **`Origin`** or **`Referer`** to match an entry in **`AllowedOrigins`**. Otherwise respond **403** with `ApiErrors.INVALID_ORIGIN`.

**Password reset** additionally validates the **`Origin`** header against the same list before building reset links (prevents arbitrary origins from influencing emailed URLs).

### Remix SSR and `Origin`

Browsers set `Origin` automatically on mutating fetches. **Node’s `fetch` (Remix server)** does not. The frontend **`networkService`** sets `Origin` from **`BASE_URL`** on server-side API calls when missing, so SSR actions/loaders are not blocked by `CsrfOriginFilter`.

**Operations checklist:** every deployed frontend base URL must appear in the API allowlist, and **`BASE_URL`** in Remix must equal that same public origin (scheme + host + port).

## Authorization (no IDOR)

- API endpoints that act on a user’s data resolve **`userId` from the session** via `AuthenticatedUserResolver`, not from path or body parameters that the client could forge.
- DTOs such as `SurfSessionRequest` omit redundant `userId` where the server derives it.
- **`AuthenticatedUserInterceptor`** enforces consistency where legacy patterns still pass a `userId`; specific routes are excluded when the path semantics require it (document in code when adding exceptions).

## Passwords and login

- Passwords are stored with **BCrypt**.
- **Policy** (NIST SP 800-63B style): minimum length **8**, maximum **128**, no mandatory character-class rules. Violations use `ApiErrors.PASSWORD_POLICY_VIOLATION`.
- **Login** returns the same generic **`ApiErrors.INVALID_CREDENTIALS`** for unknown email and wrong password, and uses a **dummy bcrypt comparison** when the user does not exist to reduce timing-based enumeration signal.

## Forgot password and reset

- **Forgot password** always returns a generic success-style message (`ApiErrors.FORGOT_PASSWORD_ACCEPTED`) whether or not the email exists (no enumeration).
- **Rate limits** apply per IP and per email (`RateLimiter.Bucket.FORGOT_PASSWORD`).
- Reset tokens: **cryptographically random** value sent once by email; only **SHA-256 hash** (hex) is stored (`PasswordResetToken.tokenHash`). Consumption hashes the incoming token and looks up by hash. Invalid, expired, or reused tokens share one message: `ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED`.

## Rate limiting (login / register / forgot-password)

**In-process** `RateLimiter` with separate buckets (e.g. login vs register vs forgot-password). Keys include **client IP** (`ClientIpExtractor`, `X-Forwarded-For` aware) and **email** where relevant. Successful login **resets** the login counters for that IP/email.

This is **not** distributed: multiple API instances each have their own counters. Replacing the backing store (e.g. Redis) is a future scalability step if you horizontally scale the API.

## OAuth (Google / Facebook)

OAuth completes in the Remix app; the API continues to trust the **same session cookie** for subsequent API calls once the user is registered or linked. Provider-specific rules live in `UserService` / `AuthController` as implemented in the codebase.

## Environment variables (cheat sheet)

| Location | Variable | Role |
|----------|----------|------|
| **surf-spots** | `SESSION_SECRET` | Signs the session cookie; must match API. |
| **surf-spots** | `BASE_URL` | Public site origin for SSR API `Origin` header (must match API CORS allowlist). |
| **surf-spots** | `VITE_API_URL` | Base URL for API calls (e.g. `http://localhost:8080/api`). |
| **surf-spots-api** | `SESSION_SECRET` / `app.auth.session-secret` | Verifies the same signed cookie as Remix. |
| **surf-spots-api** | `CORS_ALLOWED_ORIGINS` / `cors.allowed-origins` | Comma-separated trusted origins for CORS + `CsrfOriginFilter` + reset origin checks. |

See each repo’s **`.env.example`** for the authoritative list and comments.

## Key source files (index)

| Area | surf-spots-api | surf-spots |
|------|----------------|------------|
| Security chain / CSRF | `config/SecurityConfig.java`, `config/CsrfOriginFilter.java` | `app/services/networkService.ts` |
| Origins | `config/AllowedOrigins.java`, `config/CorsConfig.java` | `BASE_URL` |
| Session verify | `security/SessionCookieFilter.java`, `security/SessionCookieVerifier.java` | `app/services/session.server.ts`, `app/services/auth.server.ts` |
| Principal | `security/AuthenticatedUserResolver.java`, `config/AuthenticatedUserInterceptor.java` | loaders/actions forwarding `Cookie` |
| Profile | `controller/UserController.java` (`GET /api/user/me`) | `requireFullUserProfile` in `session.server.ts` |
| Auth endpoints | `controller/AuthController.java` | auth routes and `auth.server.ts` |
| Password / reset | `service/UserService.java`, `service/PasswordResetService.java`, `security/TokenHasher.java` | reset/forgot routes, `useFormValidation` |
| Rate limit | `security/RateLimiter.java`, `security/ClientIpExtractor.java` | (server-side only; IP from incoming Remix request when calling API) |

Account deletion is `DELETE /api/user/account/{userId}` so it does not share a path pattern with `GET /api/user/me` (which previously caused **405 Method Not Allowed** when delete used `/{userId}`).

## References (external)

- OWASP **CSRF Prevention Cheat Sheet** (SameSite, Origin checks, synchronizer token patterns).
- **NIST SP 800-63B** (memorized secret length and composition guidance).

When changing auth behaviour, update **`docs/auth.md`** (this file) and the corresponding tests under `src/test/`.
