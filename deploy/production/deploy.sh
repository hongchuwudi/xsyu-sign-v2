#!/usr/bin/env bash
set -Eeuo pipefail

readonly IMAGE_REPOSITORY="ghcr.io/hongchuwudi/xsyu-sign-v2"
readonly CONTAINER_NAME="qq-robot"
readonly BACKUP_CONTAINER_NAME="qq-robot-backup"
readonly ENV_FILE="/etc/xsyu-sign.env"
readonly CONFIG_DIR="/etc/xsyu-sign/config"
readonly LOG_DIR="/home/hongchu/qqrobot"
readonly HEALTH_URL="http://127.0.0.1:11451/"

image_tag="${1:-}"
if [[ ! "$image_tag" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$ ]]; then
  echo "Invalid or missing image tag" >&2
  exit 2
fi

readonly TARGET_IMAGE="${IMAGE_REPOSITORY}:${image_tag}"
had_previous_container=false
deployment_succeeded=false

require_path() {
  local expected_path="$1"
  if [[ ! -e "$expected_path" ]]; then
    echo "Required path does not exist: $expected_path" >&2
    exit 1
  fi
}

rollback() {
  local exit_code=$?
  trap - ERR
  set +e

  if [[ "$deployment_succeeded" == "true" ]]; then
    exit "$exit_code"
  fi

  echo "Deployment failed; restoring the previous container" >&2
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

  if [[ "$had_previous_container" == "true" ]] && docker inspect "$BACKUP_CONTAINER_NAME" >/dev/null 2>&1; then
    docker rename "$BACKUP_CONTAINER_NAME" "$CONTAINER_NAME"
    docker start "$CONTAINER_NAME"
  fi

  exit "$exit_code"
}
trap rollback ERR

require_path "$ENV_FILE"
require_path "$CONFIG_DIR"
mkdir -p "$LOG_DIR"

echo "Pulling ${TARGET_IMAGE}"
docker pull "$TARGET_IMAGE"

if docker inspect "$BACKUP_CONTAINER_NAME" >/dev/null 2>&1; then
  docker rm -f "$BACKUP_CONTAINER_NAME"
fi

if docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  had_previous_container=true
  docker stop "$CONTAINER_NAME"
  docker rename "$CONTAINER_NAME" "$BACKUP_CONTAINER_NAME"
fi

docker run -d \
  --name "$CONTAINER_NAME" \
  --restart=always \
  -p 11451:11451 \
  -v "$LOG_DIR:/app/logs" \
  -v "$CONFIG_DIR:/app/config:ro" \
  --env-file "$ENV_FILE" \
  -e TZ=Asia/Shanghai \
  "$TARGET_IMAGE"

for attempt in $(seq 1 45); do
  if curl --fail --silent --show-error --max-time 3 "$HEALTH_URL" >/dev/null 2>&1; then
    deployment_succeeded=true
    break
  fi

  if [[ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null || true)" != "true" ]]; then
    echo "New container stopped before becoming healthy" >&2
    docker logs --tail 120 "$CONTAINER_NAME" >&2 || true
    false
  fi

  sleep 2
done

if [[ "$deployment_succeeded" != "true" ]]; then
  echo "Health check timed out: $HEALTH_URL" >&2
  docker logs --tail 120 "$CONTAINER_NAME" >&2 || true
  false
fi

trap - ERR
if docker inspect "$BACKUP_CONTAINER_NAME" >/dev/null 2>&1; then
  docker rm "$BACKUP_CONTAINER_NAME"
fi

echo "Deployment succeeded: ${TARGET_IMAGE}"
