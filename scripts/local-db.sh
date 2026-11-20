#!/usr/bin/env bash
# Runs a private Postgres instance for local development, stored in ./.pgdata on port 5433.
# It does not touch any other Postgres server on this machine.
# Usage: scripts/local-db.sh start|stop|status
set -euo pipefail
cd "$(dirname "$0")/.."

DATA=".pgdata"
PORT=5433
DB=donetimeline

case "${1:-}" in
  start)
    if [ ! -d "$DATA" ]; then
      initdb -D "$DATA" -U postgres --auth=trust --encoding=UTF8 >/dev/null
      echo "Initialised $DATA"
    fi
    if ! pg_ctl -D "$DATA" status >/dev/null 2>&1; then
      pg_ctl -D "$DATA" -o "-p $PORT -k /tmp" -l "$DATA/server.log" -w start >/dev/null
    fi
    createdb -h localhost -p $PORT -U postgres "$DB" 2>/dev/null || true
    echo "Postgres running: postgres://postgres@localhost:$PORT/$DB"
    ;;
  stop)
    pg_ctl -D "$DATA" -m fast stop
    ;;
  status)
    pg_ctl -D "$DATA" status
    ;;
  *)
    echo "usage: $0 start|stop|status" >&2
    exit 1
    ;;
esac
