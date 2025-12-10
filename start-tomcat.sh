#!/bin/sh
set -eu

# Variables que puedes sobreescribir desde docker-compose
DB_HOST=${DB_HOST:-ubicuabbdd}
DB_PORT=${DB_PORT:-5432}
WAIT_TIMEOUT=${WAIT_TIMEOUT:-60}  # segundos

echo "Esperando a la BBDD en ${DB_HOST}:${DB_PORT} (timeout ${WAIT_TIMEOUT}s)..."

elapsed=0
while ! nc -z "$DB_HOST" "$DB_PORT"; do
  sleep 1
  elapsed=$((elapsed+1))
  if [ "$elapsed" -ge "$WAIT_TIMEOUT" ]; then
    echo "ERROR: Timeout esperando a ${DB_HOST}:${DB_PORT} tras ${WAIT_TIMEOUT}s"
    exit 1
  fi
done

echo "BBDD disponible en ${DB_HOST}:${DB_PORT} — arrancando Tomcat"
exec catalina.sh run
