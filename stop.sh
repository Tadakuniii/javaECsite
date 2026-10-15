#!/bin/sh
set -eu

PROJECT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

docker compose -p javaecsite -f "$PROJECT_DIRECTORY/backend/docker-compose.dev.yml" down

echo "サイトを停止しました。"
