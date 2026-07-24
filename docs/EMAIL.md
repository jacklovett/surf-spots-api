# Email in Surf Spots API

How transactional email works in this project: what gets sent, how to preview it locally, and which env vars matter.

**Template HTML** lives under `src/main/resources/templates/`. This file is developer docs, not the email copy users see.

---

## Quick start (pick one)

### Option A: Browser preview (easiest, no SMTP)

Best when you want to see what an email looks like without sending anything.

1. Run the API with the **`dev`** profile (default for `./mvnw spring-boot:run` / IDE).
2. Open **http://localhost:8080/api/dev/mail-preview/** for the index.
3. Or open a specific template, for example:
   - http://localhost:8080/api/dev/mail-preview/verify-email
   - http://localhost:8080/api/dev/mail-preview/session-started
   - http://localhost:8080/api/dev/mail-preview/session-overdue

Valid `{templateName}` values match `TransactionalEmailTemplate` in code: `verify-email`, `reset-password`, `trip-invitation`, `trip-member-added`, `contact-message`, `session-started`, `session-ended`, `session-overdue`, `new-surf-spot`, `watch-list-alert`, `nearby-surf-spots`.

### Option B: Mailpit (capture real sends in dev)

Best when you want to trigger a flow in the app and read the message that would have been sent.

1. Start deps: `docker compose -f docker-compose.dev.yml up -d` (Postgres + Mailpit).
2. Run the API on the host (`./mvnw spring-boot:run`). In `.env`, set **`MAIL_ENABLED=true`** and restart the API process.
3. Trigger mail from the app (register, forgot password, start a shared live session, etc.).
4. Open **http://localhost:8025** to read captured messages.

Mailpit only (if Postgres already up):

```bash
docker compose -f docker-compose.dev.yml up -d mailpit
```

Keep `SPRING_MAIL_HOST=localhost`, `SPRING_MAIL_PORT=1025`, and `MAIL_ENABLED=true` in `.env`.

### Option C: Sending off (default)

With `MAIL_ENABLED=false` (dev default), the API does not open SMTP. `EmailService` logs that sending is disabled. Use this for everyday API/DB work when you do not care about mail.

---

## What we send

All messages are **HTML only** (no separate plain-text part). Shared header/footer: `templates/email/fragments.html`.

### Account

| Template | Who gets it | When |
|----------|-------------|------|
| `verify-email` | New user | After sign-up (or resend) while email is unverified |
| `reset-password` | User | Forgot-password flow |

### Trips

| Template | Who gets it | When |
|----------|-------------|------|
| `trip-invitation` | Invitee | Someone is invited to a trip |
| `trip-member-added` | Member | User is added to an existing trip |

### Other

| Template | Who gets it | When |
|----------|-------------|------|
| `contact-message` | `MAIL_CONTACT_TO` (default `hello@surfspots.com`) | Contact form submission |

### Live sessions (emergency contact)

| Template | Who gets it | When |
|----------|-------------|------|
| `session-started` | Emergency contact | User starts a live session with sharing enabled |
| `session-ended` | Emergency contact | User ends that session |
| `session-overdue` | Emergency contact | Scheduled job: session still in progress past expected return time (once per session) |

### Settings-gated alerts

| Template | Who gets it | When |
|----------|-------------|------|
| `new-surf-spot` | Users with `newSurfSpotEmails` | Surf spot becomes `APPROVED`; includes Mapbox static map pin when coords + `MAPBOX_ACCESS_TOKEN` are set |
| `watch-list-alert` | Users with `swellSeasonEmails` / `eventEmails` | Daily job: swell/event watch-list alerts (deduped) |
| `nearby-surf-spots` | Users with `nearbySurfSpotsEmails` | Signed-in browser location jumps ~200 km+; multi-pin Mapbox map when `MAPBOX_ACCESS_TOKEN` is set |

Template files: `templates/{name}.html` for each row above. Names are defined once in `TransactionalEmailTemplate` so previews and sends stay in sync.

---

## Env vars that matter

Set these in `.env` at the API project root. See `.env.example` for the full list.

| Variable | What it does |
|----------|----------------|
| `MAIL_ENABLED` | `true` to send via SMTP; `false` in dev to skip sending (default in dev profile) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Production SMTP credentials (Scaleway by default) |
| `MAIL_FROM` | Sender address (often must be verified with your provider) |
| `MAIL_CONTACT_TO` | Inbox for contact-form emails |
| `APP_URL` | Frontend base URL for links in mail (no trailing slash) |
| `APP_PUBLIC_API_URL` | API base URL for verify-email links (browser hits API, then redirects to app) |
| `APP_EMAIL_LOGO_URL` | Optional logo URL in the masthead; defaults to `{APP_URL}/images/png/logo.png` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | Dev SMTP target; Docker Compose uses `mailpit:1025`, host defaults to `localhost:1025` |

YAML mapping: `MAIL_*` and `APP_*` map to `app.mail.*`, `app.url`, and `app.public-api-base-url` in `application.yml` / `application-dev.yml`.

---

## How it fits together

- **Thymeleaf** renders HTML from `templates/`.
- **`EmailService`** merges layout variables (logo, app URL), renders the template, then sends or logs.
- **Spring `JavaMailSender`** delivers over SMTP when sending is enabled.
- **Scaleway SMTP** (`smtp.scaleway.com:587`) is the default for non-dev when credentials are set.
- **Mailpit** in `docker-compose.dev.yml` is a fake SMTP server for local capture (UI on port 8025).
- **`MailPreviewController`** (`dev` profile only) serves the same templates in the browser with sample data.

Invalid `session` cookie warnings when opening mail preview in a browser that already has an app cookie are harmless for these public dev routes.

---

## Production checklist

- Profile: **`prod`** (or your host equivalent), not `dev` (dev exposes `/api/dev/mail-preview/**`).
- `MAIL_ENABLED=true`, `MAIL_USERNAME`, `MAIL_PASSWORD`, verified `MAIL_FROM`.
- `APP_URL` and `APP_PUBLIC_API_URL` set to real HTTPS origins so links and redirects work.

---

## Code references

| Area | Location |
|------|----------|
| Send + render | `EmailService` |
| Template names | `TransactionalEmailTemplate` |
| Layout (logo, URLs) | `EmailLayoutVariables` |
| Verify / reset flows | `EmailVerificationService`, `PasswordResetService` |
| Dev previews | `MailPreviewController`, `MailPreviewControllerTest` |
| Trip mail | Trip-related services calling `EmailService` |
| Contact form | `ContactController` |

Docker wiring and duplicate env notes also appear in **`README.md`** (Configuration) and **`.env.example`**.
