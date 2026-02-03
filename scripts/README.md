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
