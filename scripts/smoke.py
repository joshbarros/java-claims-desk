#!/usr/bin/env python3
"""Run the claims saga against a local Compose stack."""

INTAKE = "http://localhost:8101"
ADJ = "http://localhost:8102"
MGMT = "http://localhost:15675"
AUTH = "platform:platform"

import base64
import json
import sys
import time
import urllib.error
import urllib.request


def call(method, url, body=None, auth=None):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("content-type", "application/json")
    if auth:
        token = base64.b64encode(auth.encode()).decode()
        request.add_header("Authorization", "Basic " + token)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            raw = response.read().decode()
            parsed = json.loads(raw) if raw else None
            return response.status, parsed
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        return ex.code, parsed


def wait_ok(url, seconds=180, auth=None):
    deadline = time.time() + seconds
    while time.time() < deadline:
        try:
            status, _ = call("GET", url, auth=auth)
            if status == 200:
                print(f"up  {url}")
                return
        except Exception:
            pass
        time.sleep(2)
    sys.exit(f"timed out waiting for {url}")


def until(label, fn, seconds=45):
    deadline = time.time() + seconds
    last = None
    while time.time() < deadline:
        last = fn()
        if last is not None:
            return last
        time.sleep(1)
    sys.exit(f"{label} failed, last={last!r}")


def queue_depth(name):
    status, body = call("GET", f"{MGMT}/api/queues/%2F/{name}", auth=AUTH)
    if status != 200 or not isinstance(body, dict):
        return 0
    return body.get("messages", 0)


def submit(claimant, document, amount):
    status, body = call(
        "POST",
        f"{INTAKE}/claims",
        {"claimantName": claimant, "documentName": document, "amountCents": amount},
    )
    if status != 201 or not isinstance(body, dict):
        sys.exit(f"submit {document} -> {status} {body}")
    return body["id"]


def view(claim_id):
    status, body = call("GET", f"{ADJ}/claims/{claim_id}")
    if status == 200 and isinstance(body, dict):
        return body
    return None


def main():
    wait_ok(f"{INTAKE}/actuator/health")
    wait_ok(f"{ADJ}/actuator/health")
    wait_ok(f"{MGMT}/api/overview", seconds=60, auth=AUTH)

    healthy = submit("Mina", "policy.pdf", 2500)

    def healthy_ready():
        body = view(healthy)
        if body and body.get("decision") == "APPROVED" and body.get("documentStatus") == "CHECKED":
            return body
        return None

    until("healthy claim", healthy_ready)

    corrupt = submit("Otto", "scan-corrupt.pdf", 2500)

    def corrupt_ready():
        body = view(corrupt)
        depth = queue_depth("claims.documents.dlq")
        if not body or body.get("decision") != "APPROVED":
            return None
        if body.get("documentStatus") not in (None, ""):
            return None
        if depth < 1:
            return None
        return body

    until("corrupt document", corrupt_ready, seconds=60)
    time.sleep(2)
    later = view(corrupt)
    if later.get("documentStatus") not in (None, ""):
        sys.exit(f"corrupt document was marked checked: {later}")

    large = submit("Rita", "invoice.pdf", 2000000)

    def large_ready():
        body = view(large)
        if body and body.get("decision") == "REVIEW" and body.get("documentStatus") == "CHECKED":
            return body
        return None

    until("large claim", large_ready)
    print("claims desk smoke passed")


if __name__ == "__main__":
    main()
