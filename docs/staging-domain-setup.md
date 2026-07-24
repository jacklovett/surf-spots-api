# Staging domain setup (later — correct auth approach)

**Status: deferred.** Do this when ready to replace the interim BFF.

**Today:** Remix **`/api/backend` BFF** (Backend For Frontend) is intentional so we can deploy and verify the map/session bug is fixed on `*.vercel.app` + Scaleway **without** DNS cutover yet. Code is marked `TODO(auth-cookie-domain)` — **not** the permanent design.

**Correct end state:** app + API under one parent domain, shared session cookie `Domain`, browser → API directly, **delete the BFF**. This doc is that cutover checklist.

---

Step-by-step for that future cutover.

**Problem (without BFF):** Remix sets a host-only `session` cookie on the frontend host. The browser does not send that cookie to a different API host (e.g. `*.vercel.app` → Scaleway). List view can still work (Remix SSR forwards `Cookie`). Map `within-bounds` runs in the browser and gets approved-only / empty results.

**End-state fix:** Put app and API under the same parent domain, set `SESSION_COOKIE_DOMAIN`, remove the BFF, redeploy, log in again.

This guide uses a **staging** layout so the site is not branded as a public launch. Swap names later for production.

Related: [auth.md](./auth.md) (cookie, CORS, CSRF, current BFF).

---

## Target layout (staging)

| Piece | Hostname / value |
|-------|------------------|
| Domain you own | `surfspots.io` |
| Frontend (Vercel) | `https://staging.surfspots.io` |
| API (Scaleway) | `https://api.staging.surfspots.io` |
| Cookie domain | `.staging.surfspots.io` |
| API path prefix | `/api` → full base `https://api.staging.surfspots.io/api` |

Localhost: do **not** set `SESSION_COOKIE_DOMAIN`. Leave it unset.

---

## Prerequisites

- [ ] Domain `surfspots.io` registered and you can edit DNS
- [ ] Frontend deployed on **Vercel** (or wherever the app runs)
- [ ] API deployed on **Scaleway**
- [ ] Same `SESSION_SECRET` on both sides
- [ ] Code change ready: add optional `SESSION_COOKIE_DOMAIN` to `session.server.ts`, point browser `networkService` at `VITE_API_URL` again, **delete** `services/backendProxy.server.ts`, `routes/api.backend.$.ts`, `BACKEND_PROXY_PREFIX` / browser rewrite in `networkService.ts`, and related tests

---

## Step 0 — Code cutover (same PR as DNS/env)

1. Session cookie: if `SESSION_COOKIE_DOMAIN` is set, pass it as cookie `domain` in `createCookieSessionStorage`.
2. `networkService`: browser Spring paths call `VITE_API_URL/...` directly (no `/api/backend` rewrite).
3. Delete BFF files listed above.
4. Document `SESSION_COOKIE_DOMAIN` in `.env.example`.

Do not merge that PR until Steps 1–6 below are ready to deploy in the same window (or you will break browser auth again).

---

## Step 1 — Add the frontend domain on Vercel

1. Open the **surf-spots** project on Vercel.
2. Go to **Settings → Domains**.
3. Add: `staging.surfspots.io`.
4. Vercel will show the DNS record to create (usually a **CNAME** to `cname.vercel-dns.com`, or their current documented target).
5. Copy that record; you will add it in Step 3.
6. Leave this tab open until Vercel shows the domain as **Valid** (after DNS propagates).

Optional (recommended while not ready for public users):

- Enable **Deployment Protection** / **Password Protection** on the project so random visitors cannot browse freely.

---

## Step 2 — Point the API hostname at Scaleway

Exact clicks depend on how the API is hosted (Serverless Container, Instance + reverse proxy, Load Balancer, etc.). Goal is the same:

1. In Scaleway, attach custom domain / TLS for: `api.staging.surfspots.io`.
2. Note the DNS target Scaleway gives you (`CNAME` or `A` / `AAAA`).
3. Confirm later (Step 7) that HTTPS works for the API.

If Scaleway only gives you an existing public URL today (e.g. a long `*.scw.cloud` host), you still need a **custom domain** on that service (or a reverse proxy / LB in front) so the hostname is under `surfspots.io`. Cookie sharing requires the shared parent domain; pointing DNS at Scaleway without configuring TLS/host on the service is not enough.

---

## Step 3 — Create DNS records at your registrar

Wherever you bought `surfspots.io` (Scaleway Domains, Cloudflare, Namecheap, etc.), add:

| Type | Name / host | Value | Notes |
|------|-------------|--------|------|
| CNAME (typical) | `staging` | Vercel target from Step 1 | Frontend |
| CNAME or A/AAAA | `api.staging` | Scaleway target from Step 2 | API |

DNS UI differences:

- Some UIs want `staging.surfspots.io` as the full name; others want only `staging`.
- TTL: default / 300s is fine.

Wait for propagation (minutes to a few hours). Check:

```text
staging.surfspots.io        → resolves (Vercel)
api.staging.surfspots.io    → resolves (Scaleway)
```

Online checkers or `nslookup` / `dig` are fine.

---

## Step 4 — Set Vercel environment variables

In Vercel → **Settings → Environment Variables**, set at least for **Production** (and Preview if you use protected previews with this domain):

| Variable | Example value |
|----------|----------------|
| `BASE_URL` | `https://staging.surfspots.io` |
| `VITE_API_URL` | `https://api.staging.surfspots.io/api` |
| `SESSION_COOKIE_DOMAIN` | `.staging.surfspots.io` |
| `SESSION_SECRET` | *(same value as the API)* |

Also update OAuth callback URLs if Google/Facebook are enabled:

| Variable | Example value |
|----------|----------------|
| `GOOGLE_CALLBACK_URL` | `https://staging.surfspots.io/auth/google` |
| `FACEBOOK_CALLBACK_URL` | `https://staging.surfspots.io/auth/facebook` |

Then add the same redirect URIs in the Google / Facebook developer consoles.

**Important:** `VITE_*` vars are baked in at **build** time. After changing them, trigger a **Redeploy** (not only restart).

---

## Step 5 — Set Scaleway (API) environment variables

On the API service:

| Variable | Example value |
|----------|----------------|
| `CORS_ALLOWED_ORIGINS` | `https://staging.surfspots.io` |
| `APP_URL` | `https://staging.surfspots.io` |
| `APP_PUBLIC_API_URL` | `https://api.staging.surfspots.io` |
| `SESSION_SECRET` | *(same value as Vercel)* |

Notes:

- `CORS_ALLOWED_ORIGINS` must include the exact frontend origin (scheme + host, no path). Comma-separate if you temporarily keep the old Vercel URL too.
- Prefer dropping `https://surf-spots-five.vercel.app` from the allowlist once you only use staging, so you do not test the wrong host by mistake.
- Restart / redeploy the API after changing env.

If you use Scaleway Object Storage for media uploads, update **bucket CORS** allowed origins to include `https://staging.surfspots.io` (see API `scripts/` CORS helpers).

---

## Step 6 — Redeploy both

1. Redeploy **surf-spots** on Vercel (required after `VITE_*` / cookie domain changes).
2. Restart or redeploy **surf-spots-api** on Scaleway.
3. Confirm Vercel domain status for `staging.surfspots.io` is Valid and HTTPS works.

---

## Step 7 — Smoke-check hosts

In a browser (or curl):

1. `https://staging.surfspots.io` — frontend loads.
2. `https://api.staging.surfspots.io/api/...` — API responds (any known public health/docs/public route you use).
3. Do **not** use `https://surf-spots-five.vercel.app` for this verification; that host will not get the staging cookie domain setup.

---

## Step 8 — New login (required)

Old cookies were host-only on the previous frontend host. They will not magically gain `Domain=.staging.surfspots.io`.

1. Open `https://staging.surfspots.io` only.
2. Log out if already “logged in”.
3. Prefer a private window, or clear cookies for `staging.surfspots.io` and `.staging.surfspots.io`.
4. Log in again.

### Check the cookie

DevTools → **Application** (Chrome) / **Storage** (Firefox) → Cookies → `https://staging.surfspots.io`:

| Check | Expected |
|-------|----------|
| Name | `session` |
| Domain | `.staging.surfspots.io` |
| Secure | yes (HTTPS) |
| HttpOnly | yes |
| SameSite | `Lax` |

---

## Step 9 — Confirm the original bug is fixed

1. Stay logged in on `https://staging.surfspots.io`.
2. Open the map view that loads spots for the viewport.
3. DevTools → **Network**.
4. Find `within-bounds` (or the request to `.../surf-spots/within-bounds`).
5. Confirm:

| Check | Expected |
|-------|----------|
| Request URL host | `api.staging.surfspots.io` |
| Request headers include `Cookie` | contains `session=...` |
| Response | your spots (including pending/private you own), not always `[]` when zoomed on them |

If the request has **no** `Cookie` header, DNS/env/cookie domain is still wrong, or you are on the wrong frontend URL / stale cookie.

---

## Checklist (print / tick)

- [ ] DNS: `staging` → Vercel
- [ ] DNS: `api.staging` → Scaleway
- [ ] TLS valid on both hosts
- [ ] Vercel env set + **redeployed**
- [ ] Scaleway env set + restarted
- [ ] `SESSION_SECRET` matches on both
- [ ] Fresh login on `https://staging.surfspots.io`
- [ ] Cookie Domain = `.staging.surfspots.io`
- [ ] `within-bounds` sends `session` cookie
- [ ] Map shows expected spots

---

## When you are ready for a public / “real” URL later

Same pattern; new names only. Example:

| Piece | Production example |
|-------|--------------------|
| App | `https://app.surfspots.io` or `https://www.surfspots.io` |
| API | `https://api.surfspots.io` |
| Cookie domain | `.surfspots.io` |

Update DNS, env vars, OAuth callbacks, CORS, redeploy both, log in again. Code stays the same.

---

## Common failures

| Symptom | Likely cause |
|---------|----------------|
| Cookie Domain is `staging.surfspots.io` without parent / missing | `SESSION_COOKIE_DOMAIN` not set or app not redeployed |
| `within-bounds` has no Cookie | Wrong frontend URL, stale cookie, or Domain not covering API host |
| CORS / 403 invalid origin | `CORS_ALLOWED_ORIGINS` / `BASE_URL` mismatch |
| 401/empty as anonymous | `SESSION_SECRET` mismatch between Vercel and API |
| Works on list, not map | Classic symptom of missing browser cookie to API — re-check Steps 8–9 |
| Still testing `*.vercel.app` | Stop; use `staging.surfspots.io` only |

---

## Out of scope (later)

- Moving the Remix app off Vercel onto Scaleway
- Public launch marketing, apex `surfspots.io` landing page
- Map empty-state UX when the viewport truly has no spots

Those do not block fixing session cookies for staging.
