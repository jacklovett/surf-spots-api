#!/usr/bin/env python3
"""
Export Google Sheets to JSON files for seeding.

Sheet structure (row 1 = header, data from row 2):
  Continents:  A=name, B=description
  Countries:   A=name, B=description, C=continent_name, D=Emergency numbers
  Regions:     A=name, B=description, C=country_name, D=bounding_box
  SubRegions:  A=name, B=description, C=region_name, D=bounding_box

Output order: continents A-Z, then countries by continent A-Z, then regions by country A-Z, then sub-regions by region A-Z.

Usage:
    python scripts/export_sheets_to_json.py

Requires:
    - google-auth and google-api-python-client packages
    - GOOGLE_APPLICATION_CREDENTIALS environment variable set
    - Service account with access to the spreadsheet
"""

import json
import os
import sys
from pathlib import Path

# Column indices (match sheet columns above)
CONTINENTS_NAME, CONTINENTS_DESC = 0, 1
COUNTRIES_NAME, COUNTRIES_DESC, COUNTRIES_CONTINENT, COUNTRIES_EMERGENCY = 0, 1, 2, 3
REGIONS_NAME, REGIONS_DESC, REGIONS_COUNTRY, REGIONS_BBOX = 0, 1, 2, 3
SUBREGIONS_NAME, SUBREGIONS_DESC, SUBREGIONS_REGION, SUBREGIONS_BBOX = 0, 1, 2, 3

# Google API imports (requires: pip install -r requirements.txt)
from google.oauth2.service_account import Credentials  # type: ignore
from googleapiclient.discovery import build  # type: ignore
from googleapiclient.errors import HttpError  # type: ignore

# Spreadsheet ID
SPREADSHEET_ID = '1m0L9qPYYjxYLMuFilUdrOdaq3kzmULoN5Zv29J0eyZ0'

# Get script directory and project root
SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
OUTPUT_DIR = PROJECT_ROOT / 'src' / 'main' / 'resources' / 'static' / 'seedData'
BACKUP_DIR = PROJECT_ROOT / 'src' / 'main' / 'resources' / 'static' / 'seedData.backup'

# Try to find credentials file
CREDENTIALS_FILE = os.getenv('GOOGLE_APPLICATION_CREDENTIALS')
if not CREDENTIALS_FILE:
    # Try common locations
    possible_paths = [
        PROJECT_ROOT.parent / 'surfspots-439420-115e3f376e26.json',
        PROJECT_ROOT / 'surfspots-439420-115e3f376e26.json',
    ]
    for path in possible_paths:
        if path.exists():
            CREDENTIALS_FILE = str(path)
            break


def get_sheets_client():
    """Initialize Google Sheets API client"""
    if not CREDENTIALS_FILE:
        raise FileNotFoundError(
            "GOOGLE_APPLICATION_CREDENTIALS not set and credentials file not found. "
            "Set the environment variable or place surfspots-439420-115e3f376e26.json in the project root."
        )
    
    creds = Credentials.from_service_account_file(
        CREDENTIALS_FILE,
        scopes=['https://www.googleapis.com/auth/spreadsheets.readonly']
    )
    return build('sheets', 'v4', credentials=creds)


def get_sheet_data(sheets, sheet_name):
    """Get all data from a sheet (excluding header row)"""
    try:
        result = sheets.spreadsheets().values().get(
            spreadsheetId=SPREADSHEET_ID,
            range=f'{sheet_name}!A2:ZZ10000'  # Start from row 2, skip header
        ).execute()
        return result.get('values', [])
    except HttpError as error:
        print(f"Error reading {sheet_name}: {error}")
        return []


def parse_boolean(value):
    """Parse boolean values"""
    if not value:
        return None
    str_value = str(value).lower().strip()
    return str_value in ('true', '1', 'yes')


def parse_comma_separated(value):
    """Parse comma-separated values into list"""
    if not value:
        return None
    values = [v.strip() for v in str(value).split(',')]
    return [v for v in values if v]


def parse_bounding_box(value):
    """Parse bounding box from string"""
    if not value:
        return None
    try:
        bbox = json.loads(value)
        if isinstance(bbox, list) and len(bbox) == 4:
            return bbox
    except (json.JSONDecodeError, ValueError):
        pass
    return None


def save_json_file(filename, data):
    """Save JSON file with backup"""
    file_path = OUTPUT_DIR / filename
    backup_path = BACKUP_DIR / filename
    
    # Create directories
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    
    # Backup existing file if it exists
    if file_path.exists():
        import shutil
        shutil.copy2(file_path, backup_path)
        print(f"    Backed up existing {filename} to seedData.backup/")
    
    # Write new file
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load_json_file(filename):
    """Load JSON file"""
    file_path = OUTPUT_DIR / filename
    if not file_path.exists():
        return []
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)


def export_continents(sheets):
    """Export Continents. Sorted A-Z by name."""
    print('Exporting Continents...')
    data = get_sheet_data(sheets, 'Continents')
    rows = [
        {'name': row[CONTINENTS_NAME].strip(), 'description': (row[CONTINENTS_DESC] if len(row) > CONTINENTS_DESC else '').strip()}
        for row in data if row and row[0]
    ]
    rows.sort(key=lambda r: r['name'].lower())
    continents = [{'id': i + 1, 'name': r['name'], 'description': r['description']} for i, r in enumerate(rows)]
    save_json_file('continents.json', continents)
    print(f"  Exported {len(continents)} continents\n")
    return continents


def parse_emergency_numbers(cell_value):
    """Parse 'Emergency numbers' column: 'Label: Number; Label: Number'. Returns list of {label, number}."""
    if not cell_value or not str(cell_value).strip():
        return []
    parts = [p.strip() for p in str(cell_value).split(';') if p.strip()]
    result = []
    for part in parts:
        if ':' in part:
            label, _, number = part.partition(':')
            label, number = label.strip(), number.strip()
            if label and number:
                result.append({'label': label, 'number': number})
    return result


def export_countries(sheets, continents):
    """Export Countries. Sorted by continent then A-Z by name. Column D = Emergency numbers."""
    print('Exporting Countries...')
    data = get_sheet_data(sheets, 'Countries')
    continent_map = {c['name'].strip(): c['id'] for c in continents}

    countries = []
    for row in data:
        if not row or not row[COUNTRIES_NAME]:
            continue
        continent_name = (row[COUNTRIES_CONTINENT] if len(row) > COUNTRIES_CONTINENT else '').strip()
        continent_id = continent_map.get(continent_name)
        if continent_name and not continent_id:
            print(f"  Warning: unknown continent '{continent_name}' for country {row[COUNTRIES_NAME]}")
        country = {
            'name': row[COUNTRIES_NAME].strip(),
            'description': (row[COUNTRIES_DESC] if len(row) > COUNTRIES_DESC else '').strip(),
            'continent': {'id': continent_id, 'name': continent_name} if continent_id else None,
        }
        if len(row) > COUNTRIES_EMERGENCY and row[COUNTRIES_EMERGENCY]:
            country['emergencyNumbers'] = parse_emergency_numbers(row[COUNTRIES_EMERGENCY])
        countries.append(country)

    countries.sort(key=lambda c: (c['continent']['id'] if c.get('continent') else 9999, c['name'].lower()))
    save_json_file('countries.json', countries)
    print(f"  Exported {len(countries)} countries\n")
    return countries


def export_regions(sheets, countries):
    """Export Regions. Sorted by country then A-Z by name. Deduplicates by (country, region name)."""
    print('Exporting Regions...')
    data = get_sheet_data(sheets, 'Regions')
    country_map = {c['name'].strip(): i + 1 for i, c in enumerate(countries)}

    seen = set()  # (country_name, region_name) for deduplication
    regions = []
    for row in data:
        if not row or not row[REGIONS_NAME]:
            continue
        region_name = row[REGIONS_NAME].strip()
        country_name = (row[REGIONS_COUNTRY] if len(row) > REGIONS_COUNTRY else '').strip()
        key = (country_name or '', region_name)
        if key in seen:
            print(f"  Warning: duplicate region skipped (country='{country_name}', region='{region_name}')")
            continue
        seen.add(key)

        country_id = country_map.get(country_name)
        if country_name and not country_id:
            print(f"  Warning: unknown country '{country_name}' for region {region_name}")
        region = {
            'name': region_name,
            'description': (row[REGIONS_DESC] if len(row) > REGIONS_DESC else '').strip(),
            'country': {'id': country_id, 'name': country_name} if country_id else None,
        }
        if len(row) > REGIONS_BBOX and row[REGIONS_BBOX]:
            bbox = parse_bounding_box(row[REGIONS_BBOX])
            if bbox:
                region['boundingBox'] = bbox
        regions.append(region)

    regions.sort(key=lambda r: (r['country']['id'] if r.get('country') else 99999, r['name'].lower()))
    save_json_file('regions.json', regions)
    print(f"  Exported {len(regions)} regions\n")
    return regions


def export_sub_regions(sheets, regions):
    """Export SubRegions. Sorted by region then A-Z by name."""
    print('Exporting SubRegions...')
    data = get_sheet_data(sheets, 'SubRegions')
    region_map = {r['name'].strip(): i + 1 for i, r in enumerate(regions)}

    sub_regions = []
    for row in data:
        if not row or not row[SUBREGIONS_NAME]:
            continue
        region_name = (row[SUBREGIONS_REGION] if len(row) > SUBREGIONS_REGION else '').strip()
        region_id = region_map.get(region_name)
        if region_name and not region_id:
            print(f"  Warning: unknown region '{region_name}' for sub-region {row[SUBREGIONS_NAME]}")
        sub_region = {
            'name': row[SUBREGIONS_NAME].strip(),
            'description': (row[SUBREGIONS_DESC] if len(row) > SUBREGIONS_DESC else '').strip(),
            'region': {'id': region_id} if region_id else None,
        }
        if len(row) > SUBREGIONS_BBOX and row[SUBREGIONS_BBOX]:
            bbox = parse_bounding_box(row[SUBREGIONS_BBOX])
            if bbox:
                sub_region['boundingBox'] = bbox
        sub_regions.append(sub_region)

    sub_regions.sort(key=lambda s: (s['region']['id'] if s.get('region') else 99999, s['name'].lower()))
    save_json_file('sub-regions.json', sub_regions)
    print(f"  Exported {len(sub_regions)} sub-regions\n")
    return sub_regions


def export_surf_spots(sheets, regions, sub_regions):
    """Export SurfSpots"""
    print('Exporting SurfSpots...')
    
    data = get_sheet_data(sheets, 'SurfSpots')
    
    # Create name to ID mappings
    region_map = {r['name']: index + 1 for index, r in enumerate(regions)}
    sub_region_map = {sr['name']: index + 1 for index, sr in enumerate(sub_regions)}
    
    surf_spots = []
    for row in data:
        if not row or not row[0]:  # Skip empty rows
            continue
        
        # Column indices (status column removed). Sheet order: ... 24=isWavepool, 25=wavepoolUrl, 26=isRiverWave, 27=swellSeasonName, 28=forecasts, 29=createdBy
        region_name = row[4] if len(row) > 4 else ''  # region_name column (index 4)
        sub_region_name = row[5] if len(row) > 5 else ''  # sub_region_name column (index 5)
        region_id = region_map.get(region_name)
        sub_region_id = sub_region_map.get(sub_region_name)
        
        spot = {
            'name': row[0],
            'status': 'Approved',  # All seeded data is approved
            'description': row[1] if len(row) > 1 else None,
            'latitude': float(row[2]) if len(row) > 2 and row[2] else None,
            'longitude': float(row[3]) if len(row) > 3 and row[3] else None,
            'region': {'id': region_id} if region_id else None,
            'subRegion': {'id': sub_region_id} if sub_region_id else None,
            'type': row[6] if len(row) > 6 else None,
            'beachBottomType': row[7] if len(row) > 7 else None,
            'swellDirection': row[8] if len(row) > 8 else None,
            'windDirection': row[9] if len(row) > 9 else None,
            'skillLevel': row[10] if len(row) > 10 else None,
            'tide': row[11] if len(row) > 11 else None,
            'waveDirection': row[12] if len(row) > 12 else None,
            'minSurfHeight': float(row[13]) if len(row) > 13 and row[13] else None,
            'maxSurfHeight': float(row[14]) if len(row) > 14 and row[14] else None,
            'rating': int(row[15]) if len(row) > 15 and row[15] else None,
            'foodNearby': parse_boolean(row[16]) if len(row) > 16 else None,
            'foodOptions': parse_comma_separated(row[17]) if len(row) > 17 else None,
            'accommodationNearby': parse_boolean(row[18]) if len(row) > 18 else None,
            'accommodationOptions': parse_comma_separated(row[19]) if len(row) > 19 else None,
            'facilities': parse_comma_separated(row[20]) if len(row) > 20 else None,
            'hazards': parse_comma_separated(row[21]) if len(row) > 21 else None,
            'parking': row[22] if len(row) > 22 else None,
            'boatRequired': parse_boolean(row[23]) if len(row) > 23 else None,
            'isWavepool': parse_boolean(row[24]) if len(row) > 24 else None,
            'wavepoolUrl': row[25] if len(row) > 25 else None,
            'isRiverWave': parse_boolean(row[26]) if len(row) > 26 else None,
            'swellSeasonName': row[27] if len(row) > 27 else None,
            'forecasts': row[28] if len(row) > 28 else None,
            'createdBy': row[29] if len(row) > 29 else None
        }
        
        # Remove null/undefined/empty string values to keep JSON clean
        cleaned = {
            k: v for k, v in spot.items()
            if v is not None and v != '' and not (isinstance(v, list) and len(v) == 0)
        }
        
        # Ensure subRegion matches JSON format
        if 'subRegion' in cleaned and not cleaned['subRegion'].get('id'):
            del cleaned['subRegion']
        
        surf_spots.append(cleaned)
    
    save_json_file('surf-spots.json', surf_spots)
    print(f"  Exported {len(surf_spots)} surf spots\n")


def main():
    """Main export function"""
    print('Starting Google Sheets export to JSON...\n')
    print('WARNING: This will REPLACE existing JSON files in:')
    print(f"   {OUTPUT_DIR}\n")
    
    # Check if files exist and warn
    files_to_export = ['continents.json', 'countries.json', 'regions.json', 'sub-regions.json', 'surf-spots.json']
    existing_files = [f for f in files_to_export if (OUTPUT_DIR / f).exists()]
    
    if existing_files:
        print('Existing files that will be replaced:')
        for f in existing_files:
            print(f"   - {f}")
        print('\nBackups will be created in seedData.backup/\n')
    
    try:
        sheets = get_sheets_client()
        
        # Export in dependency order
        continents = export_continents(sheets)
        countries = export_countries(sheets, continents)
        regions = export_regions(sheets, countries)
        sub_regions = export_sub_regions(sheets, regions)
        export_surf_spots(sheets, regions, sub_regions)
        
        print('All sheets exported successfully!')
        print(f"Files saved to: {OUTPUT_DIR}")
        print('\nNext steps:')
        print('   1. Review the generated JSON files')
        print('   2. Check seedData.backup/ if you need to restore')
        print('   3. Commit them to git when ready')
        print('   4. Run your application - SeedService will use these files')
        
    except FileNotFoundError as e:
        print(f"Error: {e}")
        sys.exit(1)
    except HttpError as error:
        print(f"Error exporting sheets: {error}")
        sys.exit(1)


if __name__ == '__main__':
    main()
