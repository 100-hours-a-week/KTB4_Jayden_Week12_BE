#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.prod.yaml"
ENV_FILE="$SCRIPT_DIR/.env.prod"
LOCK_FILE="$SCRIPT_DIR/.deploy.lock"
HEALTH_TIMEOUT_SECONDS=${HEALTH_TIMEOUT_SECONDS:-120}
DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME:-jhjhkkk}
DOCKERHUB_TOKEN_PARAMETER=${DOCKERHUB_TOKEN_PARAMETER:-/hobbyloop/production/dockerhub-read-token}
BACKEND_IMAGE=${BACKEND_IMAGE:-jhjhkkk/hobbyloop-backend:latest}
FRONTEND_IMAGE=${FRONTEND_IMAGE:-jhjhkkk/hobbyloop-frontend:latest}

usage() {
    echo "Usage: $0 <backend|frontend> [image-ref]" >&2
    exit 2
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    usage
fi

SERVICE=$1

case "$SERVICE" in
    backend)
        IMAGE_REF=${2:-$BACKEND_IMAGE}
        BACKEND_IMAGE=$IMAGE_REF
        ROLLBACK_REF="jhjhkkk/hobbyloop-backend:rollback"
        ;;
    frontend)
        IMAGE_REF=${2:-$FRONTEND_IMAGE}
        FRONTEND_IMAGE=$IMAGE_REF
        ROLLBACK_REF="jhjhkkk/hobbyloop-frontend:rollback"
        ;;
    *)
        usage
        ;;
esac

export BACKEND_IMAGE FRONTEND_IMAGE

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

dockerhub_login() {
    if ! command -v aws >/dev/null 2>&1; then
        echo "AWS CLI is required on the EC2 host." >&2
        return 1
    fi

    if ! DOCKERHUB_TOKEN=$(aws ssm get-parameter \
        --name "$DOCKERHUB_TOKEN_PARAMETER" \
        --with-decryption \
        --query 'Parameter.Value' \
        --output text \
        --no-cli-pager); then
        echo "Could not read the Docker Hub token from Parameter Store." >&2
        return 1
    fi

    if [ -z "$DOCKERHUB_TOKEN" ] || [ "$DOCKERHUB_TOKEN" = "None" ]; then
        echo "Docker Hub token parameter is empty." >&2
        unset DOCKERHUB_TOKEN
        return 1
    fi

    if ! printf '%s' "$DOCKERHUB_TOKEN" \
        | docker login --username "$DOCKERHUB_USERNAME" --password-stdin; then
        unset DOCKERHUB_TOKEN
        return 1
    fi

    unset DOCKERHUB_TOKEN
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

dockerhub_login

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
