# Surf Spots API

A Spring Boot REST API for managing surf spots, trips, and user data. Built with Spring Boot 3.3.5, Java 21, Maven, and PostgreSQL.

## 📋 Quick Reference - Common Commands

**Start everything (PostgreSQL + API):**
```bash
cd surf-spots-api
docker-compose -f docker-compose.dev.yml up
```

**Start in background:**
```bash
docker-compose -f docker-compose.dev.yml up -d
```

**Stop everything:**
```bash
docker-compose -f docker-compose.dev.yml down
```

**View API logs:**
```bash
docker-compose -f docker-compose.dev.yml logs -f api
```

**Run all tests:**
```bash
docker-compose -f docker-compose.dev.yml exec api mvn test
```

**Run specific test:**
```bash
docker-compose -f docker-compose.dev.yml exec api mvn test -Dtest=SurfSpotsApplicationTests
```

**Restart API container:**
```bash
docker-compose -f docker-compose.dev.yml restart api
```

**Rebuild and restart:**
```bash
docker-compose -f docker-compose.dev.yml up --build api
```

---

## 🚀 Quick Start (Docker - Recommended)

**Everything runs in Docker - no need to install Java, Maven, or PostgreSQL!**

1. **Make sure Docker Desktop is running:**
   ```bash
   docker ps  # Should not error
   ```

2. **Set the database password** (optional; defaults to `postgres`): in your shell run `export DB_PASSWORD=postgres`, or see [Configuration](#configuration).

3. **Start everything (PostgreSQL + API):**
   ```bash
   cd surf-spots-api
   docker-compose -f docker-compose.dev.yml up --build
   ```

   The `--build` flag builds the Spring Boot app the first time. After that, you can use:
   ```bash
   docker-compose -f docker-compose.dev.yml up
   ```

4. **That's it!** The API will be running at http://localhost:8080

5. **Stop everything when done:**
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

**What's included:**
- ✅ PostgreSQL 16 database (auto-configured)
- ✅ Spring Boot API (Java 21 + Maven - all in Docker!)
- ✅ Hot reload support (code changes require container restart: `docker-compose restart api`)
- ✅ No local Java/Maven installation needed!

**View logs:**
```bash
docker-compose -f docker-compose.dev.yml logs -f api
```

## Table of Contents

- [Quick Reference - Common Commands](#-quick-reference---common-commands)
- [Quick Start](#-quick-start-docker---recommended)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Setup](#database-setup)
- [Seed Data Management](#seed-data-management)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)

## Prerequisites

### Option 1: Docker Setup (Recommended - Easiest)

**Everything runs in Docker - no local installations needed!**

You only need:
- **Docker Desktop** - Install with: `winget install Docker.DockerDesktop` or [download here](https://www.docker.com/products/docker-desktop/)

**What Docker provides:**
- PostgreSQL 16 database (no installation needed)
- Java 21 JDK (included in Maven image)
- Maven 3.9 (included in build image)
- Spring Boot application (runs in container)

**Installing Docker Desktop:**
- **Windows (winget):** `winget install Docker.DockerDesktop`
- **Manual:** Download from https://www.docker.com/products/docker-desktop/
- After installation, restart your computer and start Docker Desktop
- Verify with: `docker --version`

**Why Docker?** 
- No need to install Java, Maven, or PostgreSQL locally
- Consistent environment across all developers
- Easy to start/stop/clean up
- Production-like setup

### Option 2: Manual Installation

If you prefer not to use Docker, you'll need:
- **Java 21** (JDK) - Required
- **Maven 3.6+** - Required for building the project
- **PostgreSQL 12+** - Required for the database
- **Git** - For cloning the repository

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
- **Countries** - Country data with continent references
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

The `SeedService` automatically runs on application startup (if `app.seed.enabled=true`):

- Reads JSON files from `src/main/resources/static/seedData/`
- Resolves foreign key references by position (JSON ID = position in seeded list)
- **Creates new entities** or **updates existing ones** (matched by name)
- Handles relationships automatically (continent → country → region → sub-region → surf spot)

### Backup and Recovery

Before exporting new data, backups are automatically created:
- Backup location: `src/main/resources/static/seedData.backup/`
- Files are saved as `.backup` before replacement

To restore from backup:
```bash
cp src/main/resources/static/seedData.backup/*.json src/main/resources/static/seedData/
```

## Configuration

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
| `MAIL_USERNAME` | No | SMTP username (default: empty). |
| `MAIL_PASSWORD` | No | SMTP password (default: empty). |
| `MAIL_ENABLED` | No | Enable email (default: `true`). |
| `S3_ACCESS_KEY` | For media upload | Scaleway Object Storage API key. |
| `S3_SECRET_KEY` | For media upload | Scaleway Object Storage API key. |
| `S3_BUCKET` | No | Bucket name (default: `surf-spots-media`). |
| `S3_ENDPOINT`, `S3_REGION` | No | Override endpoint/region if not Paris. |

**Other ways to set them:** You can still use your shell (`export DB_PASSWORD=postgres`) or your IDE run configuration; real environment variables override values from `.env`. For **deployment** (e.g. Scaleway), set variables in the platform’s environment settings (not a file).

### Application Profiles

The application supports multiple profiles:

- **dev** (default) - Local development with Hibernate auto-update, email disabled
- **test** - Testing profile with in-memory database
- **prod** - Production profile with Flyway migrations enabled

The active profile is set in `application.yml` and can be overridden:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Running the Application

### Option 1: Docker (Recommended - Everything in Docker)

**No local Java/Maven installation needed!**

1. **Make sure Docker Desktop is running**

2. **Set the database password** (optional; see [Configuration](#configuration)).

3. **Start everything:**
   ```bash
   cd surf-spots-api
   docker-compose -f docker-compose.dev.yml up --build
   ```

   First time will take longer (downloads images, builds app). Subsequent starts:
   ```bash
   docker-compose -f docker-compose.dev.yml up
   ```

4. **API will be available at:** http://localhost:8080

5. **View logs:**
   ```bash
   docker-compose -f docker-compose.dev.yml logs -f api
   ```

6. **Stop everything:**
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

**Note:** Code changes require restarting the API container:
```bash
docker-compose -f docker-compose.dev.yml restart api
```

### Option 2: Local Development (Requires Java 21 + Maven)

**If you prefer to run the app locally (for better IDE integration, debugging, etc.):**

**Prerequisites:**
- Java 21 installed
- Maven installed (or use `./mvnw` wrapper)
- PostgreSQL running (use Docker: `docker-compose -f docker-compose.dev.yml up -d postgres`)

**Run with Maven Wrapper:**

The project includes Maven Wrapper, so you don't need Maven installed globally:

**Windows:**
```bash
.\mvnw.cmd spring-boot:run
```

**macOS/Linux:**
```bash
./mvnw spring-boot:run
```

### Option 2: Using Maven (if installed globally)

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

### Running Tests with Docker

Since everything runs in Docker, you have a few options:

#### Option 1: Run Tests in the API Container (Recommended)

If your API container is running, execute tests inside it:

```bash
# Make sure PostgreSQL is running first
docker-compose -f docker-compose.dev.yml up -d postgres

# Run all tests (uses 'test' profile automatically)
docker-compose -f docker-compose.dev.yml exec api mvn test

# Run specific test class
docker-compose -f docker-compose.dev.yml exec api mvn test -Dtest=SurfSpotsApplicationTests

# Run tests with coverage
docker-compose -f docker-compose.dev.yml exec api mvn test jacoco:report
```

**Note:** Tests automatically use the `test` profile which connects to the PostgreSQL service in Docker.

#### Option 2: Run Tests in a One-Off Container

Run tests in a temporary container (useful if the API isn't running):

```bash
# Run all tests
docker-compose -f docker-compose.dev.yml run --rm api mvn test

# Run specific test class
docker-compose -f docker-compose.dev.yml run --rm api mvn test -Dtest=SurfSpotsApplicationTests
```

The `--rm` flag automatically removes the container after tests complete.

#### Option 3: Use Maven Wrapper (If Java is Installed Locally)

If you have Java 21 installed locally, you can use the Maven wrapper:

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SurfSpotsApplicationTests

# Run tests with coverage
./mvnw test jacoco:report
```

**Note:** Tests use the `test` profile which connects to a test database. Make sure PostgreSQL is running (via Docker) for integration tests.

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

