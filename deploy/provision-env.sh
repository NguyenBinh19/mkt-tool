#!/usr/bin/env bash
set -euo pipefail

cd /opt/leadflow
if [[ -e .env ]]; then
  echo 'Existing .env retained.'
  exit 0
fi

umask 077
db_password="$(openssl rand -hex 32)"
root_password="$(openssl rand -hex 32)"
admin_password="$(openssl rand -hex 24)"

printf '%s\n' \
  'MYSQL_DATABASE=leadflow' \
  'MYSQL_USER=leadflow' \
  "MYSQL_PASSWORD=$db_password" \
  "MYSQL_ROOT_PASSWORD=$root_password" \
  'BOOTSTRAP_ADMIN_EMAIL=admin@stayhubb2b.site' \
  "BOOTSTRAP_ADMIN_PASSWORD=$admin_password" \
  'AUTH_TOKEN_TTL_HOURS=24' \
  'HTTP_PORT=80' \
  'PLAYWRIGHT_PROFILE_DIRECTORY=./playwright-profile' > .env

chmod 600 .env
mkdir -p playwright-profile
chmod 700 playwright-profile
echo 'Production .env created with random secrets and an initial admin account.'
