import requests
import json
import os
import sys

# Default instance id - you can override by setting BEAMS_INSTANCE environment variable
INSTANCE = os.environ.get("BEAMS_INSTANCE", "8c4f8907-19a5-4d60-8de2-39344b7156da")
# IMPORTANT: put your Beams server secret into the BEAMS_SECRET environment variable
SECRET = os.environ.get("BEAMS_SECRET")

if not SECRET:
    print("ERROR: BEAMS_SECRET environment variable is not set.\nSet it with: set BEAMS_SECRET=your_secret_here (cmd) or $env:BEAMS_SECRET='your_secret' (PowerShell)")
    sys.exit(1)

url = f"https://{INSTANCE}.pushnotifications.pusher.com/publish_api/v1/instances/{INSTANCE}/publishes/interests"

payload = {
  "interests": ["user_123"],
  "fcm": {
    "notification": {
      "title": "Test notification",
      "body": "This is a test sent to interest user_123"
    },
    "data": {
      "type": "test",
      "request_id": 999
    }
  }
}

headers = {
    "Authorization": f"Bearer {SECRET}",
    "Content-Type": "application/json"
}

try:
    resp = requests.post(url, json=payload, headers=headers, timeout=15)
    print("Status:", resp.status_code)
    try:
        print(json.dumps(resp.json(), indent=2))
    except Exception:
        print(resp.text)
except Exception as e:
    print("Request failed:", e)
