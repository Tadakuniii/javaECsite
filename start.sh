#!/bin/sh
set -eu

PROJECT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$PROJECT_DIRECTORY/backend/docker-compose.dev.yml"

export BACKEND_PORT="${BACKEND_PORT:-8081}"

docker compose -p javaecsite -f "$COMPOSE_FILE" up -d --remove-orphans

echo ""
echo "サイトを起動しました: http://127.0.0.1:5173/"
echo "バックエンド: http://127.0.0.1:${BACKEND_PORT}/"
echo "停止するときは ./stop.sh を実行してください。"
