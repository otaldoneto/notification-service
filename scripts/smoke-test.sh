#!/usr/bin/env bash
# End-to-end check against a running stack (docker compose up): queue a notification, then wait until
# the API reports it as SENT and the e-mail is in the Mailpit inbox.
set -euo pipefail

API=${API:-http://localhost:8080}
MAILPIT=${MAILPIT:-http://localhost:8025}

echo "Waiting for the API..."
for _ in $(seq 1 40); do
  curl -sf --max-time 5 "$API/actuator/health" > /dev/null && break
  sleep 3
done
curl -sf --max-time 5 "$API/actuator/health" > /dev/null || { echo "The API did not become ready"; exit 1; }

subject="Smoke test $(date +%s)"
id=$(curl -sf --max-time 10 -X POST "$API/notifications" -H 'Content-Type: application/json' \
  -d "{\"to\":\"smoke@example.com\",\"subject\":\"$subject\",\"body\":\"Hello from the smoke test\"}" \
  | python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])')
echo "Queued notification $id"

for _ in $(seq 1 20); do
  status=$(curl -sf --max-time 5 "$API/notifications/$id" | python3 -c 'import json, sys; print(json.load(sys.stdin)["status"])')
  if [ "$status" = "SENT" ]; then
    if curl -sf --max-time 5 "$MAILPIT/api/v1/messages" | grep -q "$subject"; then
      echo "Notification $id was sent and is in the inbox"
      exit 0
    fi
  fi
  sleep 1
done

echo "Notification $id was not delivered (last status: ${status:-unknown})"
exit 1
