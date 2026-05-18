# Email in Surf Spots API

This document describes **what** the API sends, **how** it is rendered and delivered, and **how to test** mail in development without touching production SMTP.

---

## Stack (tools and libraries)

| Piece | Role |
|--------|------|
| **Spring `JavaMailSender`** | Sends MIME messages over SMTP when sending is enabled. |
| **Thymeleaf** | Renders HTML templates under `src/main/resources/templates/`. |
| **`EmailService`** | Central entry: merges shared variables (logo, app URL), renders template, sends or logs. |
| **Scaleway SMTP** (non-dev defaults) | `application.yml` defaults: `smtp.scaleway.com`, port **587**, auth + STARTTLS. Used when credentials are set and mail is enabled. |
| **Mailpit** (optional, local Docker) | Fake SMTP server + inbox UI. Defined in `docker-compose.dev.yml`. Does not deliver to the internet. |
| **Dev mail preview** | `MailPreviewController` (`dev` profile only): renders the same Thymeleaf templates in a browser with sample data—no SMTP. |

---

## Configuration (environment and YAML)

Values are usually set in a **`.env`** file at the API project root (same folder as `pom.xml`). See `.env.example` for names and short comments.

| Concern | Property / env | Notes |
|--------|------------------|--------|
| Turn sending on/off | `app.mail.enabled` | Dev profile: **`MAIL_ENABLED`** (default `false` via `${MAIL_ENABLED:false}` in `application-dev.yml`). Base `application.yml` uses `${MAIL_ENABLED:true}` for other profiles. When disabled, `EmailService` logs that it would send and returns without SMTP. |
| From address | `MAIL_FROM` → `app.mail.from` | Optional. Many providers require a verified sender. |
| Contact inbox | `MAIL_CONTACT_TO` → `app.mail.contact-to` | Recipient for contact-form relay emails (default `hello@surfspots.com` if unset). |
| Frontend base URL (links in mail) | `APP_URL` → `app.url` | No trailing slash. Used for trip links, footer links, reset link base in some flows, and default logo URL when `app.email.logo-url` is empty. |
| API base for verify-email link | `APP_PUBLIC_API_URL` → `app.public-api-base-url` | No trailing slash. Verification link targets `{publicApiBase}/api/auth/verify-email?token=...` so the browser hits the API first, then redirects to the app. |
| Logo image in HTML | `APP_EMAIL_LOGO_URL` → `app.email.logo-url` | Optional full URL to the masthead image. If empty, `EmailLayoutVariables` falls back to `{app.url}/images/png/logo.png` (expects the **frontend** to serve that path). **Dev profile** sets a localhost API URL in `application-dev.yml` so previews work without the Vite app; ensure that URL actually serves the asset or override with `APP_EMAIL_LOGO_URL`. |
| Production SMTP auth | `MAIL_USERNAME`, `MAIL_PASSWORD` | Used with Scaleway host in default `application.yml`. |
| Dev SMTP target | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | `application-dev.yml` defaults to **localhost:1025** (Mailpit). Docker Compose sets host **`mailpit`** for the API container. |
| Forgot-password / resend trust | CORS allowlist | `PasswordResetService` and `EmailVerificationService` accept **`Origin` or `Referer`** when resolving a trusted app base (same hosts as `cors.allowed-origins`). Node `fetch` from Remix often sends `Origin`; navigations may send only `Referer`. |

Session cookies on the API are unrelated to SMTP; invalid `session` cookie warnings in logs when opening mail preview in a browser that already has an app cookie are expected and harmless for public dev routes.

---

## Transactional emails we send

All are **HTML** only (no separate plain-text part). Shared layout (masthead logo, footer) lives in `templates/email/fragments.html`; individual bodies live as top-level templates.

| Thymeleaf template | Typical subject | Recipient | When it is sent |
|--------------------|-----------------|-----------|------------------|
| `verify-email` | `Verify your email for Surf Spots` | New user’s email | After registration (and resend flows) while `emailVerified` is false. Link: `{app.public-api-base-url}/api/auth/verify-email?token=...`. Sending can be **async** via `EmailVerificationSendScheduler` so HTTP registration returns quickly. After **GET** verify, the browser is redirected to `{app.url}/auth?verified=true` or `?verifyError=missing|invalid|rate_limit|server`. |
| `reset-password` | `Password Reset Request` | User’s email | Forgot-password flow. Reset link uses a **trusted Origin or Referer** base URL plus `/reset-password?token=...` (must match allowed origins). |
| `trip-invitation` | `{inviterName} invited you to join a surf trip on Surf Spots` | Invitee | Trip invite with sign-up link containing invite token. |
| `trip-member-added` | `You've been added to a trip: {tripTitle}` | Member’s email | User added to an existing trip. |
| `contact-message` | `Contact Form: {subject}` | `app.mail.contact-to` | `POST /api/contact` with name, email, subject, message in the template body. |

Template files on disk (Thymeleaf logical names are defined once in `com.lovettj.surfspotsapi.email.TransactionalEmailTemplate`; preview and sends both use that enum):

- `templates/verify-email.html`
- `templates/reset-password.html`
- `templates/trip-invitation.html`
- `templates/trip-member-added.html`
- `templates/contact-message.html`
- `templates/email/fragments.html` (shared masthead/footer)

---

## Development: three ways to work with email

### 1. Sending disabled (default)

With `MAIL_ENABLED` unset or `false` in dev, the API **does not** open SMTP. Logs contain a line that sending is disabled; at **DEBUG**, `EmailService` can log generated HTML. Good for DB/API work without Mailpit.

### 2. Mailpit (capture real SMTP in Docker)

1. Start the dev stack: `docker compose -f docker-compose.dev.yml up` (includes **Mailpit**).
2. Set **`MAIL_ENABLED=true`** in `.env` and restart the **`api`** container.
3. Compose already points the API at **`SPRING_MAIL_HOST=mailpit`** and port **1025**.
4. Open **http://localhost:8025** to read captured messages.

If you run the API **on the host** (`mvn spring-boot:run`) but only Mailpit in Docker:

```bash
docker compose -f docker-compose.dev.yml up -d mailpit
```

Keep **`SPRING_MAIL_HOST=localhost`** and **`SPRING_MAIL_PORT=1025`** (dev defaults) and set **`MAIL_ENABLED=true`**.

### 3. Browser preview (no SMTP, dev profile only)

With **`spring.profiles.active=dev`** (default for local work):

- Index: **http://localhost:8080/api/dev/mail-preview/**
- Per template: **http://localhost:8080/api/dev/mail-preview/{templateName}**  
  where `{templateName}` is a `TransactionalEmailTemplate` logical name (same values as in production sends; see the enum for the authoritative list).

Security: `SecurityConfig` permits `/api/dev/mail-preview/**`. **Do not enable this profile** on a public production host without removing or protecting these routes.

---

## Production

- Use **`prod`** (or your host’s equivalent) with **`MAIL_ENABLED=true`**, **`MAIL_USERNAME`**, **`MAIL_PASSWORD`**, and a verified **`MAIL_FROM`** as required by Scaleway (or override SMTP host in YAML if you change provider).
- Set **`APP_URL`** and **`APP_PUBLIC_API_URL`** to real HTTPS origins so links in email and verification redirects are correct.

---

## Tests and code references

- **Controller tests**: `MailPreviewControllerTest` exercises dev preview routes under MockMvc.
- **Implementation**: `EmailService`, `EmailVerificationService`, `PasswordResetService`, `ContactController`, trip-related services calling `EmailService`.
- **Transactional template names**: `TransactionalEmailTemplate` is the single source of truth for Thymeleaf logical names (preview routes and `EmailService` sends).
- **Layout helpers**: `com.lovettj.surfspotsapi.email.EmailLayoutVariables` (normalized app URL and logo resolution).

For Docker env wiring and variable names, see **`README.md`** (Configuration + Mailpit subsection) and **`.env.example`**.
