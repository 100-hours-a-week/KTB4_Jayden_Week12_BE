#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.prod.yaml"
ENV_FILE="$SCRIPT_DIR/.env.prod"
LOCK_FILE="$SCRIPT_DIR/.deploy.lock"
HEALTH_TIMEOUT_SECONDS=${HEALTH_TIMEOUT_SECONDS:-120}

usage() {
    echo "Usage: $0 <backend|frontend>" >&2
    exit 2
}

if [ "$#" -ne 1 ]; then
    usage
fi

SERVICE=$1

case "$SERVICE" in
    backend)
        IMAGE_REF="jhjhkkk/hobbyloop-backend:latest"
        ROLLBACK_REF="jhjhkkk/hobbyloop-backend:rollback"
        ;;
    frontend)
        IMAGE_REF="jhjhkkk/hobbyloop-frontend:latest"
        ROLLBACK_REF="jhjhkkk/hobbyloop-frontend:rollback"
        ;;
    *)
        usage
        ;;
esac

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "Compose file not found: $COMPOSE_FILE" >&2
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "Environment file not found: $ENV_FILE" >&2
    exit 1
fi

if ! command -v flock >/dev/null 2>&1; then
    echo "flock is required on the EC2 host." >&2
    exit 1
fi

exec 9>"$LOCK_FILE"
flock 9

compose() {
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

container_health() {
    container_id=$1
    docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true
}

wait_until_healthy() {
    container_id=$1
    elapsed=0

    while [ "$elapsed" -lt "$HEALTH_TIMEOUT_SECONDS" ]; do
        status=$(container_health "$container_id")

        case "$status" in
            healthy|running)
                return 0
                ;;
            unhealthy|exited|dead)
                return 1
                ;;
        esac

        sleep 5
        elapsed=$((elapsed + 5))
    done

    return 1
}

rollback() {
    if [ "$HAS_ROLLBACK" != "true" ]; then
        echo "Deployment failed and no previous image is available for rollback." >&2
        return 1
    fi

    echo "Deployment failed. Restoring $SERVICE from $ROLLBACK_REF." >&2
    docker image tag "$ROLLBACK_REF" "$IMAGE_REF"

    if ! compose up -d --no-deps --force-recreate --pull never "$SERVICE"; then
        echo "Rollback container could not be started." >&2
        return 1
    fi

    rollback_container_id=$(compose ps -q "$SERVICE")
    if [ -z "$rollback_container_id" ] || ! wait_until_healthy "$rollback_container_id"; then
        echo "Rollback container did not become healthy." >&2
        return 1
    fi

    echo "Rollback completed for $SERVICE." >&2
    return 0
}

CURRENT_CONTAINER_ID=$(compose ps -q "$SERVICE")
HAS_ROLLBACK=false

if [ -n "$CURRENT_CONTAINER_ID" ]; then
    CURRENT_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$CURRENT_CONTAINER_ID")
    docker image tag "$CURRENT_IMAGE_ID" "$ROLLBACK_REF"
    HAS_ROLLBACK=true
fi

echo "Pulling $IMAGE_REF."
compose pull "$SERVICE"

if ! compose up -d --no-deps --force-recreate --pull never "$SERVICE"; then
    rollback || true
    exit 1
fi

NEW_CONTAINER_ID=$(compose ps -q "$SERVICE")

if [ -z "$NEW_CONTAINER_ID" ] || ! wait_until_healthy "$NEW_CONTAINER_ID"; then
    rollback || true
    exit 1
fi

echo "$SERVICE deployment completed successfully."
