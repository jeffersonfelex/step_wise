"""Create a short-lived Gemini Live token on the developer computer, never in the APK."""
import argparse
from datetime import datetime, timedelta, timezone
import getpass
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="New private token file; must not already exist")
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Choose a new output file; existing files are not overwritten")
    key = os.environ.get("GEMINI_API_KEY") or getpass.getpass("Gemini API key (hidden): ")
    if not key.strip():
        parser.error("An API key is required")
    now = datetime.now(timezone.utc)
    payload = {
        "uses": 1,
        "expireTime": (now + timedelta(minutes=30)).isoformat(),
        "newSessionExpireTime": (now + timedelta(minutes=10)).isoformat(),
    }
    request = Request("https://generativelanguage.googleapis.com/v1beta/auth_tokens",
        data=json.dumps(payload).encode(), method="POST",
        headers={"Content-Type": "application/json", "x-goog-api-key": key.strip()})
    try:
        with urlopen(request, timeout=20) as response:
            token = json.load(response)["name"]
        if not isinstance(token, str) or not token:
            raise ValueError("Invalid token response")
        fd = os.open(args.output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as output:
            output.write(token)
        print(f"Token saved to {args.output}. Start one session within 10 minutes. Do not commit or share the file.")
        return 0
    except HTTPError as error:
        print(f"Token request failed (HTTP {error.code}). Check API access and credentials.", file=sys.stderr)
    except (URLError, OSError, ValueError, KeyError):
        print("Could not create or save the token. Check network, API response and output path.", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
