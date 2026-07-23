# Surf Spots API

A Spring Boot REST API for managing surf spots, trips, and user data. Built with Spring Boot 3.3.5, Java 21, Maven, and PostgreSQL.

## Quick Reference - Common Commands

**Recommended local loop (Postgres + Mailpit in Docker, API on the host):**
```bash
cd surf-spots-api
docker compose -f docker-compose.dev.yml up -d
./mvnw spring-boot:run
```

On Windows PowerShell (outside WSL), use `.\mvnw.cmd spring-boot:run` instead of `./mvnw`.

API: http://localhost:8080 — DevTools reloads on save. Mailpit UI: http://localhost:8025

**Stop Docker deps:**
```bash
docker compose -f docker-compose.dev.yml down
```

**Optional: API in Docker too** (slower edit loop; bind mounts do not auto-compile `src` into container `target`):
```bash
docker compose -f docker-compose.dev.yml --profile docker-api up --build
docker compose -f docker-compose.dev.yml --profile docker-api logs -f api
```

**Run all tests:**
```bash
docker compose -f docker-compose.dev.yml --profile tests run --rm tests
```

**Run specific test:**
```bash
docker compose -f docker-compose.dev.yml --profile tests run --rm tests sh -c "mvn test -Dtest=SurfSpotsApplicationTests"
```

---

## Quick Start (recommended)

**Docker for Postgres + Mailpit. Run the API on the host** so Spring DevTools hot-reloads Java and email templates.

1. **Docker Desktop running:** `docker ps` should not error.
2. **Java 21** on the machine where you run the API, plus the Maven wrapper (`./mvnw`; Windows PowerShell: `.\mvnw.cmd`).
3. **Copy `.env.example` → `.env`** and set at least `DB_PASSWORD` (default `postgres` is fine) and `SESSION_SECRET` if you use the frontend.
4. **Start deps:**
   ```bash
   cd surf-spots-api
   docker compose -f docker-compose.dev.yml up -d
   ```
   Starts **postgres** (5432) and **mailpit** (SMTP 1025, UI 8025). Does **not** start the API container.
5. **If an old API container is still bound to 8080**, stop it:
   ```bash
   docker compose -f docker-compose.dev.yml --profile docker-api stop api
   ```
6. **Run the API on the host:**
   ```bash
   ./mvnw spring-boot:run
   ```
   Or run `SurfSpotsApplication` from the IDE with profile `dev`.
7. **API:** http://localhost:8080 — **Mail preview:** http://localhost:8080/api/dev/mail-preview/

**What's included by default:**
- PostgreSQL 16 on `localhost:5432`
- Mailpit for captured mail (`MAIL_ENABLED=true` in `.env` to actually send to it)
- Host API with DevTools (edit `src` → restart classpath automatically)

### Optional: API inside Docker

```bash
docker compose -f docker-compose.dev.yml --profile docker-api up --build
```

Use when you do not want Java on the host. Expect a slower edit loop: only `./src` is mounted; you must rebuild classpath inside the container for changes to apply.
## Table of Contents

- [Quick Reference - Common Commands](#quick-reference---common-commands)
- [Quick Start](#quick-start-recommended)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Setup](#database-setup)
- [Seed Data Management](#seed-data-management)
- [WSL Championship Tour schedule](scripts/contest-import/README.md)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)

## Prerequisites

### Recommended: Docker deps + host JDK

- **Docker Desktop** — Postgres + Mailpit (`winget install Docker.DockerDesktop` or [download](https://www.docker.com/products/docker-desktop/))
- **Java 21** — run the API on the host with the Maven wrapper (`./mvnw`; on Windows PowerShell `.\mvnw.cmd`)

**Why this split:** DevTools hot reload works on the host. Bind-mounting `src` into a Linux API container does not reliably auto-compile into `target`.

### Optional: API in Docker too

With `--profile docker-api`, Compose also runs the Spring Boot app in a Maven container (no host JDK). Prefer the host API when iterating on code.

### Fully manual (no Docker)

- **Java 21**, **Maven 3.6+**, **PostgreSQL 12+**, **Git**

### Quick Start with Docker (Recommended)

1. **Install Docker Desktop** (if not already installed)
   - Download from: https://www.docker.com/products/docker-desktop/
   - Start Docker Desktop

2. **Set the database password** (see [Configuration → Environment variables](#environment-variables)): in your shell run `export DB_PASSWORD=postgres` (or use a `.env` file if you use Docker Compose).

3. **Start PostgreSQL with Docker:**
   ```bash
   cd surf-spots-api
   docker-compose -f docker-compose.dev.yml up -d
   ```

4. **Verify PostgreSQL is running:**
   ```bash
   docker ps  # Should show surf-spots-postgres-dev container
   ```

5. **That's it!** PostgreSQL is now running. Continue to [Running the Application](#running-the-application)

The database will be available at `localhost:5432` with:
- Database: `surf_spots_db`
- Username: `postgres`
- Password: `postgres` (or whatever you set in `DB_PASSWORD`)

## Manual Installation (Alternative)

### Windows Installation

#### Option 1: Using winget (Recommended)

If you have Windows Package Manager (winget) installed, you can install all prerequisites with:

```powershell
# Install Java 21 (OpenJDK)
winget install Microsoft.OpenJDK.21

# Install Maven
winget install Apache.Maven

# Install PostgreSQL
winget install PostgreSQL.PostgreSQL
```

#### Option 2: Manual Installation

**Java 21 (JDK):**
1. Download OpenJDK 21 from [Adoptium](https://adoptium.net/) or [Microsoft OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download)
2. Run the installer and follow the setup wizard
3. Add Java to your PATH environment variable:
   - Add `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot\bin` (or your installation path) to PATH
4. Verify installation: `java -version`

**Maven:**
1. Download Maven from [Apache Maven Downloads](https://maven.apache.org/download.cgi)
2. Extract to a directory (e.g., `C:\Program Files\Apache\maven`)
3. Add Maven to your PATH:
   - Add `C:\Program Files\Apache\maven\bin` to PATH
4. Set `JAVA_HOME` environment variable to your JDK installation path
5. Verify installation: `mvn -version`

**PostgreSQL:**
1. Download PostgreSQL from [PostgreSQL Downloads](https://www.postgresql.org/download/windows/)
2. Run the installer
3. During installation:
   - Remember the password you set for the `postgres` user (you'll need this for `DB_PASSWORD`)
   - Note the port (default is 5432)
   - Keep the default installation options
4. Verify installation: `psql --version`

### macOS Installation

Using Homebrew:

```bash
# Install Java 21
brew install openjdk@21
brew link --overwrite openjdk@21

# Install Maven
brew install maven

# Install PostgreSQL
brew install postgresql@16
brew services start postgresql@16
```

### Linux Installation

**Ubuntu/Debian:**

```bash
# Install Java 21
sudo apt update
sudo apt install openjdk-21-jdk

# Install Maven
sudo apt install maven

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## Database Setup

### Using Docker (Recommended)

**The database is automatically created when you start Docker Compose!**

When you run:
```bash
docker-compose -f docker-compose.dev.yml up
```

The PostgreSQL container will:
- Create the `surf_spots_db` database automatically
- Set up the `postgres` user with your `DB_PASSWORD`
- Be ready to accept connections

**No manual database setup needed!**

### Manual Database Setup

If you installed PostgreSQL manually:

1. **Create the Database:**

   ```bash
   # Connect to PostgreSQL (you'll be prompted for the postgres user password)
   psql -U postgres

   # Create the database
   CREATE DATABASE surf_spots_db;

   # Exit psql
   \q
   ```

   Or from the command line:

   ```bash
   psql -U postgres -c "CREATE DATABASE surf_spots_db;"
   ```

### 2. Database Migrations

The project uses **Flyway** for database migrations in production, but in development mode, Hibernate will automatically update the schema.

**For Development:**
- Flyway is disabled (see `application-dev.yml`)
- Hibernate will automatically create/update tables when you run the application
- No manual migration steps needed

**For Production:**
- Flyway migrations are located in `src/main/resources/db/migration/`
- Migrations run automatically when the application starts with the `prod` profile

### 3. Verify Database Connection

Test your database connection:

```bash
psql -U postgres -d surf_spots_db -c "SELECT version();"
```

## Seed Data Management

The application uses seed data to populate the database with initial data (continents, countries, regions, sub-regions, and surf spots). Seed data is managed through Google Sheets for easy editing, then exported to JSON files that the `SeedService` uses.

### Workflow

1. **Edit data in Google Sheets**
2. **Export to JSON** - When data is ready, export from Google Sheets to JSON files using the python script (export_sheets_to_json.py)
3. **Commit to Git** - Review and commit the generated JSON files
4. **Automatic seeding** - `SeedService` automatically uses the JSON files on application startup, to seed or adjust existing records

### Google Sheets Setup

The seed data is maintained in a Google Sheet with the following tabs:
- **Continents** - Continent data
- **Countries** - Country data with continent references and emergency service details
- **Regions** - Region data with country references
- **SubRegions** - Sub-region data with region references
- **SurfSpots** - Surf spot data with region/sub-region references

**Spreadsheet ID:** `1m0L9qPYYjxYLMuFilUdrOdaq3kzmULoN5Zv29J0eyZ0`

### Exporting from Google Sheets

When you're ready to update the seed data:

1. **Install Python dependencies** (if not already done):
   
   ```bash
   cd surf-spots-api/scripts
   pip install -r requirements.txt
   ```
   
   **Note:** If you don't have `pip` installed, or prefer using a virtual environment:
   - **Virtual environment (recommended):**
     ```bash
     python3 -m venv venv
     source venv/bin/activate  # On Windows: venv\Scripts\activate
     pip install -r requirements.txt
     ```
   - **System-wide installation:** Use `pip3` or `python3 -m pip` as appropriate for your system

2. **Set up credentials**:
   - Ensure `GOOGLE_APPLICATION_CREDENTIALS` environment variable points to your service account JSON file
   - Or place `surfspots-439420-115e3f376e26.json` in the monorepo root (one level up from surf-spots-api)
   - Make sure the service account has access to the Google Sheet

3. **Export the sheets to JSON**:
   ```bash
   cd surf-spots-api/scripts
   python3 export_sheets_to_json.py
   ```

   This will:
   - Read all data from Google Sheets
   - Convert name-based foreign keys to ID-based references
   - Set `status: "Approved"` for all surf spots automatically
   - Export JSON files to `src/main/resources/static/seedData/`

5. **Review the generated files**:
   - Check the JSON files in `src/main/resources/static/seedData/`
   - Verify the data looks correct

6. **Commit to Git**:

### How SeedService Works

On startup (if `app.seed.enabled=true`), `SeedService` runs **only when the database has no continents yet** (one-time fill for an empty DB). If reference data already exists, it skips.

- Reads JSON from `src/main/resources/static/seedData/`
- Resolves foreign keys using country/continent names where present, otherwise JSON position (ID = index in the ordered export list)
- Order: swell seasons → continents → countries → regions → sub-regions → surf spots

Ongoing reference-data changes in production should use **database migrations**, not re-seeding.

### Backup and Recovery

Before exporting new data, backups are automatically created:
- Backup location: `src/main/resources/static/seedData.backup/`
- Files are saved as `.backup` before replacement

To restore from backup:
```bash
cp src/main/resources/static/seedData.backup/*.json src/main/resources/static/seedData/
```

## WSL Championship Tour schedule

CT event dates and surf-spot links are managed via a **manual CLI** (not HTTP endpoints). Save the WSL schedule page in your browser, then run `--contest-sync --file=...`. See the operator guide.

Full operator guide: **[scripts/contest-import/README.md](scripts/contest-import/README.md)**

```powershell
docker compose -f docker-compose.dev.yml run --rm --no-deps `
  -v "${PWD}/scripts/contest-import/snapshots:/snapshots" `
  api mvn -B "-Dmaven.test.skip=true" spring-boot:run `
  "-Dspring-boot.run.profiles=dev,event-cli" `
  "-Dspring-boot.run.arguments=--contest-sync --file=/snapshots/ct-2026.html --year=2026"
```

## Configuration

Full guide to **transactional email**, **Mailpit**, **dev HTML previews**, and env vars: **[docs/EMAIL.md](docs/EMAIL.md)**.

### Environment Variables (use a `.env` file)

You can put all config in a **`.env` file** in the project root (same folder as `pom.xml`). The app loads it when you run locally (Maven or IDE), and Docker Compose reads the same file when you run with `docker-compose up`. **Do not commit `.env`** (it’s in `.gitignore`).

**Setup:** Copy `.env.example` to `.env` and fill in values:

```bash
cp .env.example .env
```

**Example `.env` in project root:**

```env
# Database (required for local run)
DB_PASSWORD=postgres

# Email (optional)
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_ENABLED=false

# Scaleway Object Storage (for media upload)
S3_ACCESS_KEY=your_scaleway_access_key
S3_SECRET_KEY=your_scaleway_secret_key
S3_BUCKET=surf-spots-media
```

**Variables used by the app:**

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_PASSWORD` | Yes (when not using Docker default) | PostgreSQL password for user `postgres`. |
| `SESSION_SECRET` | Yes (for cookie-authenticated requests) | Must match frontend `SESSION_SECRET` so the API can verify signed `session` cookies. |
| `MAIL_USERNAME` | No | SMTP username (default: empty). |
| `MAIL_PASSWORD` | No | SMTP password (default: empty). |
| `MAIL_ENABLED` | No | Send email when `true`. In **dev** profile defaults to `false`; set `true` to use Mailpit locally. Non-dev defaults come from `application.yml`. |
| `S3_ACCESS_KEY` | For media upload | Scaleway Object Storage API key. |
| `S3_SECRET_KEY` | For media upload | Scaleway Object Storage API key. |
| `S3_BUCKET` | No | Bucket name (default: `surf-spots-media`). |
| `S3_ENDPOINT`, `S3_REGION` | No | Override endpoint/region if not Paris. |

WSL CT schedule: manual CLI — [scripts/contest-import/README.md](scripts/contest-import/README.md). No env vars required.

**Other ways to set them:** You can still use your shell (`export DB_PASSWORD=postgres`) or your IDE run configuration; real environment variables override values from `.env`. For **deployment** (e.g. Scaleway), set variables in the platform’s environment settings (not a file).

### Application Profiles

The application supports multiple profiles:

- **dev** (default) - Local development; Flyway migrations on; email off unless `MAIL_ENABLED=true` (use Mailpit, see below)
- **test** - Integration tests; Postgres test database (`application-test.yml`, defaults in `src/test/resources/application.properties`)
- **prod** - Production profile with Flyway migrations enabled
- **event-cli** - One-off contest schedule sync/link commands; ([scripts/contest-import/README.md](scripts/contest-import/README.md))

The active profile is set in `application.yml` and can be overridden:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Running the Application

### Option 1: Host API + Docker Postgres/Mailpit (recommended)

Same as [Quick Start](#quick-start-recommended):

```bash
docker compose -f docker-compose.dev.yml up -d
./mvnw spring-boot:run
```

Stop deps: `docker compose -f docker-compose.dev.yml down`

### Mailpit (optional — capture outbound email in dev)

The default Compose stack starts **[Mailpit](https://mailpit.axllent.org/)** with Postgres: SMTP **1025**, UI **http://localhost:8025**.

1. In `.env`, set **`MAIL_ENABLED=true`**.
2. Restart the **host** API (`./mvnw spring-boot:run`) so Spring picks it up. (`SPRING_MAIL_HOST=localhost` is already the dev default.)
3. Trigger an email from the app (e.g. forgot password, verify email).
4. Open **http://localhost:8025** and read the message.

With **`MAIL_ENABLED=false`** (default), the API does not send mail; `EmailService` only logs that sending is disabled.

If the API runs in Docker (`--profile docker-api`), Compose sets `SPRING_MAIL_HOST=mailpit` for you; restart that container after changing `MAIL_ENABLED`.

### Option 2: API inside Docker

```bash
docker compose -f docker-compose.dev.yml --profile docker-api up --build
```

Slower edit loop (bind-mounted `src` does not auto-compile into container `target`). Prefer the host API flow above when iterating on code or email templates.

### Option 3: Local Development (host API — same as Quick Start)

**Prerequisites:**
- Java 21 installed
- Maven wrapper (`./mvnw`, or `.\mvnw.cmd` in Windows PowerShell)
- Postgres + Mailpit: `docker compose -f docker-compose.dev.yml up -d`

```bash
./mvnw spring-boot:run
```

### Option 4: Using Maven (if installed globally)

```bash
mvn spring-boot:run
```

### Option 3: Build and Run JAR

```bash
# Build the project
mvn clean package

# Run the JAR
java -jar target/surf-spots-api-0.0.1-SNAPSHOT.jar
```

### Option 4: Using IDE

1. Import the project into your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Ensure Java 21 is configured as the project SDK
3. Run the `SurfSpotsApplication` main class

### Verifying the Application

Once the application starts, you should see:

```
Started SurfSpotsApplication in X.XXX seconds
```

The API will be available at:
- **Base URL:** http://localhost:8080
- **Health Check:** http://localhost:8080/actuator/health (if actuator is enabled)

## Testing

### Docker (recommended)

The **`api`** service (profile **`docker-api`**) sets **`SPRING_DATASOURCE_*`** for the **dev** database. Spring Boot gives those env vars high precedence, so **do not run `mvn test` inside that container** or tests would inherit the dev URL.

Use the **`tests`** service (compose profile **`tests`**): same image, **`SPRING_PROFILES_ACTIVE=test`**, and **`SPRING_DATASOURCE_*`** pointing at **`surf_spots_test_db`**.

```bash
docker compose -f docker-compose.dev.yml up -d postgres
docker compose -f docker-compose.dev.yml --profile tests run --rm tests
```

Specific class or JaCoCo:

```bash
docker compose -f docker-compose.dev.yml --profile tests run --rm tests sh -c "mvn test -Dtest=SurfSpotsApplicationTests"
docker compose -f docker-compose.dev.yml --profile tests run --rm tests sh -c "mvn test jacoco:report"
```

### Local Maven (Java on the host)

```bash
./mvnw test
./mvnw test -Dtest=SurfSpotsApplicationTests
./mvnw test jacoco:report
```

Surefire sets **`spring.profiles.active=test`**. Main **`application.yml`** uses **`spring.profiles.default: dev`** (not `active`) so **dev** is not locked on while tests run. JDBC defaults: **`src/test/resources/application.properties`** and **`application-test.yml`**; Docker **`tests`** service sets **`SPRING_DATASOURCE_*`** to the test DB host.

Create **`surf_spots_test_db`** once if it is missing (e.g. `docker exec surf-spots-postgres-dev psql -U postgres -c "CREATE DATABASE surf_spots_test_db;"`, or `db-init/` on a new Docker volume).

Integration tests (`SeedServiceIntegrationTest`, `SurfEventFilterIntegrationTest`, `ContestScheduleSyncWorkflowIntegrationTest`) require Postgres — use the Docker **`tests`** service above.

To run a subset (e.g. WSL parser and service tests):

```bash
./mvnw test -Dtest=ContestScheduleHtmlParserTests,ContestScheduleSyncServiceTests,ContestVenueLinkServiceTests,EventStatusTests,ContestDateRangeParserTests,EventNotificationServiceTests
```

### Why tests can appear to hang

- **`@SpringBootTest` without Postgres** — Hikari waits on connection (now **5s** fail-fast in `application-test.yml`).
- **Full `./mvnw test`** — 12+ Spring context boots; can take **many minutes** even when healthy.
- **Do not use `-q`** when debugging; it hides progress.
- **Do not run `mvn test` inside the `api` dev container** — it inherits the dev DB URL (see Docker section above).

### How this maps to Spring Boot

- **`SPRING_DATASOURCE_URL`** / username / password are the [documented](https://docs.spring.io/spring-boot/reference/features/external-config.html) way to supply JDBC from the environment.
- **`application-dev.yml`** and **`application-test.yml`** hold sensible **defaults** for local runs without those env vars.
- **`src/main/resources/application.yml`** does not set a JDBC URL; each profile supplies one.

## Project Structure

```
surf-spots-api/
├── src/
│   ├── main/
│   │   ├── java/com/lovettj/surfspotsapi/
│   │   │   ├── config/          # Configuration classes (Security, CORS, etc.)
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── enums/            # Enumeration types
│   │   │   ├── exceptions/       # Custom exceptions
│   │   │   ├── repository/       # JPA repositories
│   │   │   ├── requests/         # Request DTOs
│   │   │   ├── response/         # Response DTOs
│   │   │   ├── security/         # Security utilities
│   │   │   ├── service/          # Business logic services
│   │   │   ├── util/             # Utility classes
│   │   │   └── validators/       # Custom validators
│   │   └── resources/
│   │       ├── db/migration/     # Flyway migration scripts
│   │       ├── static/seedData/  # Seed data JSON files
│   │       ├── templates/        # Email templates
│   │       ├── application.yml   # Main configuration
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/                     # Test classes
├── pom.xml                       # Maven dependencies
├── Dockerfile                    # Docker configuration
└── README.md                     # This file
```

## Technology Stack

- **Framework:** Spring Boot 3.3.5
- **Java Version:** 21
- **Build Tool:** Maven 3.9+
- **Database:** PostgreSQL 12+
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security with OAuth2
- **Migrations:** Flyway
- **Email:** Spring Mail with Thymeleaf templates
- **Caching:** Spring Cache

## Troubleshooting

### Common Issues

**1. "java: command not found"**
- Ensure Java 21 is installed and added to your PATH
- Verify with `java -version`

**2. "mvn: command not found"**
- Use the Maven Wrapper instead: `./mvnw` (no need to install Maven globally)
- Or ensure Maven is installed and added to your PATH
- Set `JAVA_HOME` environment variable
- Verify with `mvn -version`

**3. Database Connection Errors**

*If using Docker:*
- Check if container is running: `docker ps`
- Check container logs: `docker-compose -f docker-compose.dev.yml logs postgres`
- Restart the container: `docker-compose -f docker-compose.dev.yml restart postgres`
- Verify `DB_PASSWORD` environment variable matches what's in docker-compose
- **`FATAL: sorry, too many clients already`:** Dev Postgres allows more connections (`max_connections=200` in `docker-compose.dev.yml`). **Recreate** the Postgres container after pulling changes so that setting applies: `docker-compose -f docker-compose.dev.yml up -d --force-recreate postgres`, then restart the API. Also avoid running multiple Spring Boot processes (host + Docker, several test JVMs) against the same `localhost:5432` at once; the `test` profile uses a smaller Hikari pool per process.

*If using manual PostgreSQL:*
- Verify PostgreSQL is running: `psql -U postgres`
- Check database exists: `psql -U postgres -l`
- Verify `DB_PASSWORD` environment variable is set correctly
- Check connection string in `application.yml`

**3a. Docker Issues**
- Make sure Docker Desktop is running
- Check Docker is working: `docker ps`
- If port 5432 is already in use, stop other PostgreSQL instances or change the port in `docker-compose.dev.yml`

**4. Port 8080 Already in Use**
- Change the port in `application.yml`:
  ```yaml
  server:
    port: 8081
  ```
- Or stop the process using port 8080

**5. Maven Build Fails**
- Clear Maven cache: `mvn clean`
- Delete `~/.m2/repository` and rebuild
- Ensure you have internet connection for dependency downloads

**6. Hibernate Schema Errors**
- In dev mode, Hibernate auto-updates the schema
- If issues persist, drop and recreate the database:
  ```sql
  DROP DATABASE surf_spots_db;
  CREATE DATABASE surf_spots_db;
  ```

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Maven Documentation](https://maven.apache.org/guides/)

## Support

For issues or questions, please refer to the project's issue tracker or contact the development team.

