#!/usr/bin/env bash
set -euo pipefail

cd /opt/leadflow
set -a
source .env
set +a

base_url='http://127.0.0.1'
login_response="$(python3 -c 'import json, os; print(json.dumps({"email": os.environ["BOOTSTRAP_ADMIN_EMAIL"], "password": os.environ["BOOTSTRAP_ADMIN_PASSWORD"]}))' | curl -fsS -H 'Content-Type: application/json' --data-binary @- "$base_url/api/auth/login")"
token="$(printf '%s' "$login_response" | python3 -c 'import json, sys; print(json.load(sys.stdin)["token"])')"
workspace_response="$(curl -fsS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' --data-binary '{"name":"QA browser queue","industry":"Deployment test","description":"Temporary workspace for queue verification"}' "$base_url/api/workspaces")"
workspace_id="$(printf '%s' "$workspace_response" | python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])')"
payload="$(python3 -c 'import json, sys; print(json.dumps({"workspaceId": int(sys.argv[1]), "postUrl": "https://www.facebook.com/MongoDB/posts/pfbid022J6v86quPNMybab29sL3UAHfX3wRy396jhoQuC8ZRmbR4vobv7pLJR83jG8GxvuFl", "maxComments": 1, "includeReplies": False}))' "$workspace_id")"

ids=()
concurrency="${FACEBOOK_MAX_CONCURRENT_TABS:-1}"
if ! [[ "$concurrency" =~ ^[1-9][0-9]*$ ]]; then
  echo 'FACEBOOK_MAX_CONCURRENT_TABS must be a positive integer.' >&2
  exit 1
fi
job_count=$((concurrency + 1))
for number in $(seq 1 "$job_count"); do
  response="$(curl -fsS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' --data-binary "$payload" "$base_url/api/facebook/extract-jobs")"
  ids+=("$(printf '%s' "$response" | python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])')")
done

running=0
queued=0
for id in "${ids[@]}"; do
  snapshot="$(curl -fsS -H "Authorization: Bearer $token" "$base_url/api/facebook/extract-jobs/$id")"
  status="$(printf '%s' "$snapshot" | python3 -c 'import json, sys; print(json.load(sys.stdin)["status"])')"
  [[ "$status" == RUNNING ]] && running=$((running + 1))
  [[ "$status" == QUEUED ]] && queued=$((queued + 1))
done

echo "$job_count submitted jobs: running=$running queued=$queued (configured limit=$concurrency)"
if (( running > concurrency )); then
  echo 'More jobs were running than the configured limit.' >&2
  exit 1
fi
if (( queued < 1 )); then
  echo 'The excess job was not observed in the queue; check worker timing.' >&2
  exit 1
fi

for attempt in $(seq 1 30); do
  unfinished=0
  for id in "${ids[@]}"; do
    status="$(curl -fsS -H "Authorization: Bearer $token" "$base_url/api/facebook/extract-jobs/$id" | python3 -c 'import json, sys; print(json.load(sys.stdin)["status"])')"
    [[ "$status" == QUEUED || "$status" == RUNNING ]] && unfinished=$((unfinished + 1))
  done
  (( unfinished == 0 )) && break
  sleep 3
done
if (( unfinished != 0 )); then
  echo 'Test jobs did not finish; QA workspace was kept for inspection.' >&2
  exit 1
fi
latest_before="$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" "$base_url/api/facebook/extract-jobs/latest?workspaceId=$workspace_id")"
[[ "$latest_before" == 200 ]]
curl -fsS -o /dev/null -X DELETE -H "Authorization: Bearer $token" "$base_url/api/facebook/extract-jobs?workspaceId=$workspace_id"
latest_after="$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" "$base_url/api/facebook/extract-jobs/latest?workspaceId=$workspace_id")"
[[ "$latest_after" == 204 ]]
curl -fsS -o /dev/null -X DELETE -H "Authorization: Bearer $token" "$base_url/api/workspaces/$workspace_id"
echo 'Browser concurrency limit and clear-result API verified; temporary QA workspace removed.'
