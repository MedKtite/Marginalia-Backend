#!/usr/bin/env python3
"""Uploads every email template listed in brevo/templates.csv to Brevo (name, subject, HTML), named
"Readsake · VERIFY_EMAIL". Designs: Figma page "Readsake · Emails".

A template that already exists in Brevo with the same name is updated, not duplicated, so the script can be
run again after editing the HTML or a subject. The Brevo ID of every template is written to
src/main/resources/email/brevo-templates.properties, where AccountEmailService reads it.

    # BREVO_API_KEY=xkeysib-... in the backend .env (Brevo → SMTP & API → API keys), or exported
    python3 email-template/upload_to_brevo.py
    python3 email-template/upload_to_brevo.py --dry-run          # shows what would be uploaded, sends nothing
    python3 email-template/upload_to_brevo.py --only WELCOME     # one event only

The sender (no-reply@readsake.io by default) must be a verified sender in Brevo, or on an authenticated domain.
The header logo is LOGO_URL below (Brevo → Files); --logo-url "" sets the name in serif text instead.
Only the Python standard library is used.
"""
import argparse
import csv
import json
import os
import re
import ssl
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

API = "https://api.brevo.com/v3"
# Header logo (mark + "Readsake", 129×30), hosted in Brevo → Files
LOGO_URL = "https://img.mailinblue.com/12550494/images/content_library/original/6af870a54ba171fc96573cb9.png"
HERE = Path(__file__).resolve().parent
# Read by AccountEmailService to pick the template of each email
IDS_FILE = HERE.parent / "src/main/resources/email/brevo-templates.properties"
IDS_HEADER = ("# Brevo template ID of each account email: <EVENT>=<id>.\n"
              "# Written by email-template/upload_to_brevo.py: re-upload rather than edit by hand.\n")

# Python from python.org on macOS has no CA certificates until "Install Certificates.command" is run:
# fall back to the system's bundle so HTTPS works either way
SSL_CONTEXT = ssl.create_default_context()
if not SSL_CONTEXT.get_ca_certs() and Path("/etc/ssl/cert.pem").exists():
    SSL_CONTEXT.load_verify_locations("/etc/ssl/cert.pem")


def call(method, path, api_key, body=None):
    data = json.dumps(body).encode() if body is not None else None
    for attempt in range(5):
        request = urllib.request.Request(API + path, data=data, method=method, headers={
            "api-key": api_key, "accept": "application/json", "content-type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=30, context=SSL_CONTEXT) as response:
                payload = response.read()
                return json.loads(payload) if payload else {}
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < 4:  # rate limited: wait and retry
                time.sleep(2 ** attempt)
                continue
            sys.exit(f"Brevo {method} {path} failed: HTTP {e.code} {e.read().decode(errors='replace')}")


def env_value(name):
    """From the environment, else from the backend's .env."""
    if os.environ.get(name):
        return os.environ[name]
    env_file = HERE.parent / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            key, sep, value = line.partition("=")
            value = value.strip().strip('"').strip("'")
            if sep and key.strip() == name and value:
                return value
    return None


def existing_templates(api_key):
    """Name → id of every transactional template already in the account."""
    templates, offset = {}, 0
    while True:
        page = call("GET", f"/smtp/templates?limit=1000&offset={offset}", api_key)
        for t in page.get("templates", []):
            templates[t["name"]] = t["id"]
        offset += 1000
        if offset >= page.get("count", 0):
            return templates


def render(html, logo_url):
    """Header: the logo image (mark + name) when there is a URL, else the name set in serif text."""
    if logo_url:
        return re.sub(r"<!--wordmark-->.*?<!--/wordmark-->", "", html.replace("%%LOGO_URL%%", logo_url), flags=re.S)
    return re.sub(r"<!--logo-->.*?<!--/logo-->", "", html, flags=re.S)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sender-email", default="no-reply@readsake.io")
    parser.add_argument("--sender-name", default="Readsake")
    parser.add_argument("--logo-url", default=env_value("READSAKE_EMAIL_LOGO_URL") or LOGO_URL,
                        help="public URL of the header logo, 129×30 (default: the one in Brevo's file manager)")
    parser.add_argument("--only", help="upload one event only (e.g. WELCOME)")
    parser.add_argument("--dry-run", action="store_true", help="list the changes without calling Brevo")
    args = parser.parse_args()

    with open(HERE / "brevo" / "templates.csv", encoding="utf-8-sig") as f:
        rows = [r for r in csv.DictReader(f) if not args.only or args.only.upper() == r["Event"]]
    if not rows:
        sys.exit(f"No template matches --only {args.only}")

    api_key = env_value("BREVO_API_KEY")
    if not args.dry_run and not api_key:
        sys.exit("Set BREVO_API_KEY in the backend .env (Brevo → SMTP & API → API keys), or use --dry-run.")

    existing = existing_templates(api_key) if not args.dry_run else {}
    results = {}
    for row in rows:
        name = row["Brevo template name"]
        body = {
            "templateName": name,
            "subject": row["Subject"],
            "htmlContent": render((HERE / row["HTML file"]).read_text(encoding="utf-8"), args.logo_url),
            "sender": {"name": args.sender_name, "email": args.sender_email},
            "isActive": True,
        }
        if args.dry_run:
            print(f"would upload  {name}")
            continue
        if name in existing:
            template_id = existing[name]
            call("PUT", f"/smtp/templates/{template_id}", api_key, body)
            print(f"updated  #{template_id}  {name}")
        else:
            template_id = call("POST", "/smtp/templates", api_key, body)["id"]
            print(f"created  #{template_id}  {name}")
        results[row["Event"]] = str(template_id)

    if results:
        # Keep the IDs of templates not uploaded this run (--only)
        saved = {}
        if IDS_FILE.exists():
            for line in IDS_FILE.read_text(encoding="utf-8").splitlines():
                key, sep, value = line.partition("=")
                if sep and not key.startswith("#"):
                    saved[key] = value
        saved.update(results)
        IDS_FILE.parent.mkdir(parents=True, exist_ok=True)
        IDS_FILE.write_text(IDS_HEADER + "".join(f"{key}={saved[key]}\n" for key in sorted(saved)), encoding="utf-8")
        print(f"\n{len(results)} templates in Brevo; IDs saved to {IDS_FILE.relative_to(HERE.parent)}")
    elif args.dry_run:
        print(f"\n{len(rows)} templates would be uploaded")


if __name__ == "__main__":
    main()
