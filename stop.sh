#!/bin/sh
set -eu

PROJECT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$PROJECT_DIRECTORY/backend/docker-compose.dev.yml"

if [ -f "$PROJECT_DIRECTORY/.env" ]; then
  docker compose --env-file "$PROJECT_DIRECTORY/.env" -p javaecsite -f "$COMPOSE_FILE" down
else
  docker compose -p javaecsite -f "$COMPOSE_FILE" down
fi

echo "サイトを停止しました。"
