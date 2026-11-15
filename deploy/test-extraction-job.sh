#!/usr/bin/env bash
set -euo pipefail

cd /opt/leadflow
set -a
source .env
set +a

base_url='http://127.0.0.1'
login_response="$(python3 -c 'import json, os; print(json.dumps({"email": os.environ["BOOTSTRAP_ADMIN_EMAIL"], "password": os.environ["BOOTSTRAP_ADMIN_PASSWORD"]}))' | curl -fsS -H 'Content-Type: application/json' --data-binary @- "$base_url/api/auth/login")"
token="$(printf '%s' "$login_response" | python3 -c 'import json, sys; print(json.load(sys.stdin)["token"])')"
workspace_id="$(curl -fsS -H "Authorization: Bearer $token" "$base_url/api/workspaces" | python3 -c 'import json, sys; print(json.load(sys.stdin)[0]["id"])')"

job_payload="$(python3 -c 'import json, sys; print(json.dumps({"workspaceId": int(sys.argv[1]), "postUrl": "https://www.facebook.com/MongoDB/posts/pfbid022J6v86quPNMybab29sL3UAHfX3wRy396jhoQuC8ZRmbR4vobv7pLJR83jG8GxvuFl", "maxComments": 1, "includeReplies": False}))' "$workspace_id")"
job_response="$(curl -fsS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' --data-binary "$job_payload" "$base_url/api/facebook/extract-jobs")"
job_id="$(printf '%s' "$job_response" | python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])')"

for attempt in $(seq 1 30); do
  snapshot="$(curl -fsS -H "Authorization: Bearer $token" "$base_url/api/facebook/extract-jobs/$job_id")"
  status="$(printf '%s' "$snapshot" | python3 -c 'import json, sys; print(json.load(sys.stdin)["status"])')"
  if [[ "$status" == COMPLETED || "$status" == FAILED ]]; then
    echo "Extraction job $job_id reached $status"
    printf '%s' "$snapshot" | python3 -c 'import json,sys; x=json.load(sys.stdin); print("result_status=" + str((x.get("result") or {}).get("status")) + " error=" + str(x.get("error")))'
    exit 0
  fi
  sleep 3
done

echo "Extraction job $job_id did not finish within 90 seconds" >&2
exit 1
