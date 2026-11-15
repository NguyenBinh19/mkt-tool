#!/usr/bin/env bash
set -euo pipefail

cd /opt/leadflow
set -a
source .env
set +a

base_url='http://127.0.0.1'
host_header='Host: stayhubb2b.site'

frontend_status="$(curl -sS -o /dev/null -w '%{http_code}' -H "$host_header" "$base_url/")"
unauthorized_status="$(curl -sS -o /dev/null -w '%{http_code}' -H "$host_header" "$base_url/api/workspaces")"
if [[ "$frontend_status" != 200 || "$unauthorized_status" != 401 ]]; then
  echo "Unexpected HTTP status: frontend=$frontend_status unauthenticated_api=$unauthorized_status" >&2
  exit 1
fi

login_response="$(python3 -c 'import json, os; print(json.dumps({"email": os.environ["BOOTSTRAP_ADMIN_EMAIL"], "password": os.environ["BOOTSTRAP_ADMIN_PASSWORD"]}))' | curl -fsS -H "$host_header" -H 'Content-Type: application/json' --data-binary @- "$base_url/api/auth/login")"
token="$(printf '%s' "$login_response" | python3 -c 'import json, sys; print(json.load(sys.stdin).get("token", ""))')"
if [[ -z "$token" ]]; then
  echo 'Admin login did not return a token.' >&2
  exit 1
fi

profile="$(curl -fsS -H "$host_header" -H "Authorization: Bearer $token" "$base_url/api/auth/me")"
role="$(printf '%s' "$profile" | python3 -c 'import json, sys; print(json.load(sys.stdin).get("role", ""))')"
if [[ "$role" != ADMIN ]]; then
  echo "Unexpected account role: $role" >&2
  exit 1
fi

workspace_count="$(curl -fsS -H "$host_header" -H "Authorization: Bearer $token" "$base_url/api/workspaces" | python3 -c 'import json, sys; print(len(json.load(sys.stdin)))')"
if [[ "$workspace_count" -lt 1 ]]; then
  echo 'Admin has no workspace.' >&2
  exit 1
fi

if [[ "${TEST_FACEBOOK_SESSION:-0}" == 1 ]]; then
  browser_status="$(curl -fsS -H "$host_header" -H "Authorization: Bearer $token" "$base_url/api/facebook/browser/status" | python3 -c 'import json, sys; print(json.load(sys.stdin)["browser"])')"
  if [[ "$browser_status" != READY ]]; then
    echo "Facebook browser is $browser_status" >&2
    exit 1
  fi
fi

echo "Smoke test passed: frontend=$frontend_status unauthenticated_api=$unauthorized_status admin_role=$role workspaces=$workspace_count"
