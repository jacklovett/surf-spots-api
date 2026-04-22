# Seed Data Management Scripts

These utility scripts manage seed data for the Surf Spots application. They export data from Google Sheets to JSON files that the Java backend's `SeedService` uses for database seeding.

**Note:** These are Python utility scripts located in the `surf-spots-api/scripts/` directory. They require Python 3 and the Google API client libraries.

## Workflow

1. **Edit data in Google Sheets** - Non-developers can edit the spreadsheet
2. **Export to JSON** - Run this script when data is ready
3. **Commit to Git** - Review and commit the generated JSON files
4. **Seed database** - `SeedService` automatically uses the JSON files on startup

## Setup

1. **Install Python dependencies:**
   
   **Using a virtual environment (recommended):**
   ```bash
   cd surf-spots-api/scripts
   python3 -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   pip install -r requirements.txt
   ```
   
   **System-wide installation:**
   ```bash
   cd surf-spots-api/scripts
   pip install -r requirements.txt
   ```
   
   **Note:** 
   - Use `python3` or `python` depending on your system
   - Use `pip3` or `pip` depending on your system
   - On Windows, use `venv\Scripts\activate` instead of `source venv/bin/activate`
   - If using a virtual environment, remember to activate it before running scripts

2. Set up credentials:
   - Ensure `GOOGLE_APPLICATION_CREDENTIALS` environment variable points to your service account JSON file
   - Or place `surfspots-439420-115e3f376e26.json` in the monorepo root (one level up from surf-spots-api)

3. Make sure the service account has access to the Google Sheet

## Usage

### Set bucket CORS (Dockerized, reusable for all environments)

Use `set-bucket-cors.ps1` (PowerShell) or `set-bucket-cors.sh` (WSL/Linux/macOS) to apply and verify Object Storage CORS without installing AWS CLI locally.
Both scripts auto-load `../.env` by default, so no manual `source` is required.

From `surf-spots-api/scripts`:

```powershell
.\set-bucket-cors.ps1
```

```bash
./set-bucket-cors.sh
```

The script reads these environment variables:

- `SCW_ACCESS_KEY` (required)
- `SCW_SECRET_KEY` (required)
- `S3_BUCKET` (required; bucket name)
- `SCW_ENDPOINT` (optional, default `https://s3.fr-par.scw.cloud`)
- `SCW_REGION` (optional, default `fr-par`)
- `CORS_ALLOWED_ORIGINS` (optional, comma-separated; default `http://localhost:5173`)

You can override values directly:

```powershell
.\set-bucket-cors.ps1 -BucketName "surf-spots-media-dev" -Origins "http://localhost:5173,https://your-prod-domain.com"
```

```bash
./set-bucket-cors.sh "surf-spots-media-dev" "http://localhost:5173,https://your-prod-domain.com"
```

Custom env file path:

```powershell
.\set-bucket-cors.ps1 -EnvFile "C:\dev\surf-spots-api\.env"
```

```bash
ENV_FILE=/mnt/c/dev/surf-spots-api/.env ./set-bucket-cors.sh
```

This command is idempotent and safe to rerun for dev/staging/prod.

### Fix data and order (export then restart API)

**Step 1 – Export from Google Sheets**

In a terminal (PowerShell or Command Prompt):

```powershell
cd c:\dev\surf-spots-api\scripts
```

If you use a venv, activate it (optional):

```powershell
.\venv\Scripts\activate
```

Install deps once if needed:

```powershell
pip install -r requirements.txt
```

Run the export (credentials are auto-found if `surfspots-439420-115e3f376e26.json` is in `c:\dev\`):

```powershell
python export_sheets_to_json.py
```

Or set the key explicitly:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "c:\dev\surfspots-439420-115e3f376e26.json"
python export_sheets_to_json.py
```

**Step 2 – Restart the API so the seed runs**

- **Docker:** From `c:\dev\surf-spots-api` run:
  ```powershell
  docker-compose -f docker-compose.dev.yml up --build api
  ```
  (or `restart api` if already built)
- **Local (Maven):** From `c:\dev\surf-spots-api` run:
  ```powershell
  mvn spring-boot:run
  ```
  Stop and start again if it was already running.

The seed runs on startup and updates the DB from the new JSON (links, emergency numbers, order).

---

**One-off run (no venv):**

```bash
cd surf-spots-api/scripts
# Activate virtual environment if using one:
# source venv/bin/activate  # On Windows: venv\Scripts\activate

python3 export_sheets_to_json.py
```

The script will:
- Read data from all sheets (Continents, Countries, Regions, SubRegions, SurfSpots)
- Convert name-based foreign keys to ID-based references
- Export JSON files to `src/main/resources/static/seedData/`

## Output Files

- `continents.json` - All continents with sequential IDs
- `countries.json` - Countries with continent references
- `regions.json` - Regions with country references and bounding boxes
- `sub-regions.json` - Sub-regions with region references
- `surf-spots.json` - Surf spots with all fields and references

## Data Format

The script converts:
- **Foreign keys**: Name-based (e.g., `continent_name: "Africa"`) → ID-based (e.g., `continent: { id: 1 }`)
- **Boolean fields**: String values ("TRUE"/"FALSE") → Boolean values
- **Arrays**: Comma-separated strings → JSON arrays
- **Typical crowd** (`crowd_level`, last SurfSpots column): optional; one of `EMPTY`, `FEW`, `BUSY`, `PACKED` (matches `CrowdLevel` in the API)
- **Numbers**: String numbers → Numeric values
- **Bounding boxes**: JSON string arrays → Actual arrays

## Integration with SeedService

The exported JSON files are automatically used by `SeedService` on application startup. The service:
- Reads JSON files from `src/main/resources/static/seedData/`
- Resolves foreign key references by position
- **Creates new entities** or **updates existing ones** (matched by name)
- Handles relationships automatically

**Update Behavior:**
- If you modify data in Google Sheets and re-export, existing entities in the database will be updated
- Entities are matched by name - if a name exists, the entity is updated; otherwise, a new one is created

## Best Practices

1. **Review before committing**: Always review the generated JSON files before committing
2. **Test seeding**: Run the application locally to verify the data seeds correctly
3. **Version control**: Commit JSON files to git so the database can be seeded consistently
4. **Regular exports**: Export from Google Sheets whenever significant data changes are made
