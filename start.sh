#!/bin/sh
set -eu

PROJECT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$PROJECT_DIRECTORY/backend/docker-compose.dev.yml"

export BACKEND_PORT="${BACKEND_PORT:-8081}"

if [ -f "$PROJECT_DIRECTORY/.env" ]; then
  docker compose --env-file "$PROJECT_DIRECTORY/.env" -p javaecsite -f "$COMPOSE_FILE" up -d --remove-orphans
else
  docker compose -p javaecsite -f "$COMPOSE_FILE" up -d --remove-orphans
fi

echo ""
echo "サイトを起動しました: http://127.0.0.1:5173/"
echo "バックエンド: http://127.0.0.1:${BACKEND_PORT}/"
echo "停止するときは ./stop.sh を実行してください。"
