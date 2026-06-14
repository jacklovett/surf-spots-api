# WSL Championship Tour schedule

Operator guide for syncing the WSL CT schedule and linking tour stops to surf spots. This is **not** part of the public API — only you run these commands.

## What this does

- **Parses** a CT schedule page you saved in your browser and upserts rows in `surf_event` + `surf_event_contest_detail`
- Links each WSL **venue** (by location, not sponsor event name) to a surf spot in the database
- Powers the app:
  - **Watchlist feed** — event notifications when a linked CT stop is within its waiting period
  - **Surf guide filter** — "WSL Championship Tour" checkbox (past CT venues and current-season linked stops)

The API **never contacts** worldsurfleague.com. You open the schedule in your browser and save the HTML locally.

## Local vs production database

The CLI uses the **same Spring datasource configuration** as the running API. Profile `event-cli` only disables the web server and seeding; it does **not** choose which database to connect to.

| Target | How |
|--------|-----|
| **Local dev DB** | `dev,event-cli` profiles + local datasource env vars |
| **Production DB** | `prod,event-cli` profiles + `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` |

**Always confirm the JDBC URL before running sync or link against production.**

Flyway migrations must have been applied before the CLI can write surf event rows.

## Concepts

| Concept | Meaning |
|---------|---------|
| **Historical WSL tour stop** | Break has hosted a linked CT venue. Stored as `surf_spot.is_wsl_tour_stop` (never cleared). |
| **Active contest this season** | Break has a linked `CONTEST` event for the current season year. |
| **Venue location key** | Normalized slug from the WSL location string (e.g. `saquarema-rio-de-janeiro-brazil`). Stored on `surf_event_contest_detail` — **not** the sponsor event name. |

Contest names change every year; location strings are stable.

## When to run

Run **manually** when WSL publishes or updates the schedule:

1. Save the CT page in your browser
2. `--contest-sync --file=...` (optionally `--dry-run` first)
3. `--contest-link` once per new venue

Returning venues auto-link if they were linked in a prior year.

## CLI commands

Commands use Spring profile **`event-cli`**. The app connects to the DB, runs one command, and exits.

### Sync schedule (`--contest-sync --file=...`)

1. Open in your browser: `https://www.worldsurfleague.com/events/{year}/ct?all=1`
2. Save the page (`Ctrl+S` → "Webpage, HTML only" or "Webpage, Complete") to e.g. `scripts/contest-import/snapshots/ct-2026.html`
3. Preview (optional) — **one line** (works in bash/zsh and PowerShell):

```bash
docker compose -f docker-compose.dev.yml run --rm --no-deps -v "${PWD}/scripts/contest-import/snapshots:/snapshots" api mvn -B "-Dmaven.test.skip=true" spring-boot:run "-Dspring-boot.run.profiles=dev,event-cli" "-Dspring-boot.run.arguments=--contest-sync --file=/snapshots/ct-2026.html --year=2026 --dry-run"
```

Multi-line (bash/zsh — continuation is `\`, not PowerShell's `` ` ``):

```bash
docker compose -f docker-compose.dev.yml run --rm --no-deps \
  -v "${PWD}/scripts/contest-import/snapshots:/snapshots" \
  api mvn -B "-Dmaven.test.skip=true" spring-boot:run \
  "-Dspring-boot.run.profiles=dev,event-cli" \
  "-Dspring-boot.run.arguments=--contest-sync --file=/snapshots/ct-2026.html --year=2026 --dry-run"
```

4. Upsert to the database (remove `--dry-run`):

```bash
docker compose -f docker-compose.dev.yml run --rm --no-deps -v "${PWD}/scripts/contest-import/snapshots:/snapshots" api mvn -B "-Dmaven.test.skip=true" spring-boot:run "-Dspring-boot.run.profiles=dev,event-cli" "-Dspring-boot.run.arguments=--contest-sync --file=/snapshots/ct-2026.html --year=2026"
```

Run from the **`surf-spots-api/`** directory (same folder as `docker-compose.dev.yml`). Postgres must be up (`docker compose -f docker-compose.dev.yml up -d postgres`).

**Why it looks stuck:** the first minute is usually Maven compiling ~200 main sources with little output. `-B` shows progress; `-Dmaven.test.skip=true` skips compiling 60+ test files. After compile, Spring Boot starts, runs the command, and exits.

**Faster repeat runs:** if the `api` container is already up, recreate it once so snapshots are mounted (`docker compose -f docker-compose.dev.yml up -d --force-recreate api`), then use `docker compose exec` with `/app/scripts/contest-import/snapshots/...` instead of `run` + `-v`.

**Host Maven (optional):** if you have Java 21 and Maven locally, use `.\mvnw` with `--file=./scripts/contest-import/snapshots/...` instead of mounting `/snapshots`.

`--file` is **required**. Omit `--year` to use the current calendar year.

Log output: `created`, `updated`, `auto-linked` counts.

### Link venue to surf spot

Run once per break when a venue has no prior-year link to copy forward:

```bash
docker compose -f docker-compose.dev.yml run --rm --no-deps api mvn -B "-Dmaven.test.skip=true" spring-boot:run "-Dspring-boot.run.profiles=dev,event-cli" "-Dspring-boot.run.arguments=--contest-link --venue-key=pipeline-oahu-hawaii --spot-id=123"
```

This sets `surf_spot_id` on matching contest events and sets `is_wsl_tour_stop = true` on the spot.

### Venue location key

Derived from `locationName`: lowercase, strip punctuation, spaces to hyphens.

| locationName | venue_location_key |
|--------------|-------------------|
| `Saquarema, Rio de Janeiro, Brazil` | `saquarema-rio-de-janeiro-brazil` |
| `Pipeline, Oahu, Hawaii` | `pipeline-oahu-hawaii` |

## Manual sync against production

**What this means:** the local dev commands above write to your **Docker Postgres** (`surf_spots_db` on localhost). Production sync is the **same CLI**, but you point it at the **live database** your real app uses, so users see updated tour dates and filters.

You run the command **from your laptop** — it is not a deploy step and it does not hit the WSL website. It only connects to Postgres and upserts rows.

### Before you run

1. **Migrations are live** — deploy the API (or run Flyway) so prod has `surf_event`, `surf_event_contest_detail`, and `is_wsl_tour_stop` (V37–V38). The sync will fail if those are missing.
2. **You can reach prod Postgres** — your IP may need to be allowed in the host firewall (e.g. Scaleway). If you cannot connect with `psql` or a DB client, the CLI will fail too.
3. **You have prod credentials** — the same `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` the production API uses (from your host’s env / secrets, not committed to git).
4. **Save the WSL HTML locally** — same file as dev (`scripts/contest-import/snapshots/ct-2026.html`).

### Dry run first (recommended)

Use `--dry-run` with prod credentials to confirm the file parses. Dry run does **not** write to the database, but it still **connects** to prod Postgres to start Spring (Flyway validate, etc.) — so double-check the URL.

```bash
export DATABASE_URL="jdbc:postgresql://your-prod-host:5432/your_db"
export DATABASE_USERNAME="your_user"
export DATABASE_PASSWORD="your_password"

docker compose -f docker-compose.dev.yml run --rm --no-deps \
  -v "${PWD}/scripts/contest-import/snapshots:/snapshots" \
  -e SPRING_DATASOURCE_URL="$DATABASE_URL" \
  -e SPRING_DATASOURCE_USERNAME="$DATABASE_USERNAME" \
  -e SPRING_DATASOURCE_PASSWORD="$DATABASE_PASSWORD" \
  api mvn -B "-Dmaven.test.skip=true" spring-boot:run \
  "-Dspring-boot.run.profiles=prod,event-cli" \
  "-Dspring-boot.run.arguments=--contest-sync --file=/snapshots/ct-2026.html --year=2026 --dry-run"
```

PowerShell: set `$env:DATABASE_URL = "..."` etc., and use `` ` `` (backtick) at end of each line for continuation — not `\`.

The `SPRING_DATASOURCE_*` overrides are required when using the dev Compose `api` service — otherwise it would keep pointing at local Docker Postgres.

### Sync for real

Remove `--dry-run` from the arguments above. Log output shows `created`, `updated`, and `auto-linked` counts.

### Link new venues

If the sync reports venues that are not linked to surf spots yet, run `--contest-link` with the same env vars and profile (same `SPRING_DATASOURCE_*` overrides if using Docker):

```bash
docker compose -f docker-compose.dev.yml run --rm --no-deps \
  -e SPRING_DATASOURCE_URL="$DATABASE_URL" \
  -e SPRING_DATASOURCE_USERNAME="$DATABASE_USERNAME" \
  -e SPRING_DATASOURCE_PASSWORD="$DATABASE_PASSWORD" \
  api mvn -B "-Dmaven.test.skip=true" spring-boot:run \
  "-Dspring-boot.run.profiles=prod,event-cli" \
  "-Dspring-boot.run.arguments=--contest-link --venue-key=saquarema-rio-de-janeiro-brazil --spot-id=123"
```

Replace `venue-key` with the slug from dry-run output and `spot-id` with the surf spot’s ID in prod.

### Typical order (once per season update)

1. Deploy API (migrations applied).
2. Save WSL schedule HTML.
3. Prod dry-run → check 12 events and venue keys.
4. Prod sync (no `--dry-run`).
5. `--contest-link` for any new breaks not auto-linked from a prior year.
6. Spot-check in the live app (filter + chips on linked spots).

## Sync behaviour

- **Upserts** by `(venue_location_key, year)` — re-running updates existing rows, no duplicates
- **Does not** auto-cancel venues missing from a partial HTML save
- If WSL changes page HTML structure, sync fails until the parser is updated
- `is_wsl_tour_stop` on surf spots is historical and is never cleared automatically

## Legal and trademarks

**Not legal advice.**

- You browse and save the schedule page yourself — the server only parses your local file
- WSL [Terms of Use](https://www.worldsurfleague.com/pages/terms-of-use) restrict automated fetching; this workflow avoids server-side requests to WSL
- **World Surf League** / **WSL** are trademarks; we use nominative references only (filter label, chips). No WSL logos without a license
- Event names are sponsor titles stored as factual schedule data

## Database

- `V37__Add_Wsl_Tour_Stop_Flag_To_Surf_Spot.sql` — `surf_spot.is_wsl_tour_stop`
- `V38__Create_Surf_Event.sql` — `surf_event`, `surf_event_contest_detail`, and status check constraint

WSL import writes `event_type = CONTEST`, `organizer = WSL`, `series = Championship Tour`, `source = CONTEST_HTML_IMPORT`.

## Testing

```bash
docker compose -f docker-compose.dev.yml --profile tests run --rm tests
```

| Area | Test class |
|------|------------|
| HTML parsing | `ContestScheduleHtmlParserTests` |
| File sync + upsert | `ContestScheduleSyncServiceTests` |
| Filter | `SurfEventFilterIntegrationTest` |
| Notifications | `EventNotificationServiceTests`, `EventNotificationIntegrationTest` |
| End-to-end workflow | `ContestScheduleSyncWorkflowIntegrationTest` |
| Venue link | `ContestVenueLinkServiceTests` |

## Related code

| Area | Location |
|------|----------|
| CLI | `config/EventCommandRunner.java` |
| HTML parser | `util/ContestScheduleHtmlParser.java` |
| Sync / upsert | `service/ContestScheduleSyncService.java` |
| Link | `service/ContestVenueLinkService.java` |
| Notifications | `service/EventNotificationService.java` |

## Phase 2 (not implemented)

Email on event start date (`eventEmails` user setting).
