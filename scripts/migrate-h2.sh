#!/usr/bin/env bash
set -euo pipefail

# Migrates an existing H2 1.4.x database to H2 2.x without data loss
# by exporting the legacy database to SQL and re-importing it with the
# newer engine. The original database files are retained with a .legacy
# suffix for safety.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_BASE="${1:-$ROOT_DIR/db/fiets}"
LEGACY_JAR="$ROOT_DIR/build/h2/h2-1.4.200.jar"
NEW_JAR="$ROOT_DIR/build/h2/h2-2.3.232.jar"
SCRIPT_SQL="$ROOT_DIR/build/h2/export.sql"
LEGACY_DB_FILE="${DB_BASE}.mv.db"
LEGACY_TRACE_FILE="${DB_BASE}.trace.db"

mkdir -p "$(dirname "$LEGACY_JAR")"

fetch() {
  local url="$1" dest="$2"
  if [ ! -f "$dest" ]; then
    echo "Downloading $(basename "$dest")..."
    curl -fsSL "$url" -o "$dest"
  fi
}

fetch "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar" "$LEGACY_JAR"
fetch "https://repo1.maven.org/maven2/com/h2database/h2/2.3.232/h2-2.3.232.jar" "$NEW_JAR"

if [ ! -f "$LEGACY_DB_FILE" ]; then
  echo "Legacy database file '$LEGACY_DB_FILE' not found; nothing to migrate."
  exit 1
fi

echo "Exporting legacy database with H2 1.4.200..."
java -cp "$LEGACY_JAR" org.h2.tools.Script \
  -url "jdbc:h2:file:$DB_BASE" \
  -user sa \
  -password "" \
  -script "$SCRIPT_SQL"

echo "Backing up legacy files..."
mv "$LEGACY_DB_FILE" "${LEGACY_DB_FILE}.legacy"
if [ -f "$LEGACY_TRACE_FILE" ]; then
  mv "$LEGACY_TRACE_FILE" "${LEGACY_TRACE_FILE}.legacy"
fi

if [ -f "${DB_BASE}.mv.db" ]; then
  echo "A new-format database already exists at ${DB_BASE}.mv.db; aborting to avoid overwriting."
  exit 1
fi

echo "Importing into H2 2.x format..."
java -cp "$NEW_JAR" org.h2.tools.RunScript \
  -url "jdbc:h2:file:$DB_BASE;MODE=LEGACY;DATABASE_TO_LOWER=TRUE" \
  -user sa \
  -password "" \
  -script "$SCRIPT_SQL"

echo "Migration complete. New database stored at ${DB_BASE}.mv.db."
echo "Legacy backup retained at ${LEGACY_DB_FILE}.legacy."
