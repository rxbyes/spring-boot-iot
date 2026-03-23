#!/usr/bin/env python3
"""Run real-environment message-flow acceptance against the current dev baseline."""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional
from urllib.parse import urlparse
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import ProxyHandler, Request, build_opener, urlopen


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
APP_BASE_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application.yml"


class AcceptanceError(RuntimeError):
    """Acceptance assertion error."""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify message-flow HTTP, MQTT and expired-trace behavior against the real dev environment."
    )
    parser.add_argument("--base-url", help="Override app base url. Defaults to application.yml port/context path.")
    parser.add_argument("--username", default="admin", help="Login username for protected APIs.")
    parser.add_argument("--password", default="123456", help="Login password for protected APIs.")
    parser.add_argument(
        "--mqtt-timeout-seconds",
        type=int,
        help="MQTT session polling timeout. Defaults to session-match-window-seconds + 10.",
    )
    parser.add_argument("--poll-interval-seconds", type=float, default=2.0, help="Polling interval in seconds.")
    parser.add_argument(
        "--expired-trace-id",
        help="Optional expired traceId. When provided, the script verifies GET /trace/{traceId} returns data=null.",
    )
    parser.add_argument("--timeout-seconds", type=int, default=15, help="Single request timeout in seconds.")
    return parser.parse_args()


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise AcceptanceError(f"Unable to resolve {env_name} from application-dev.yml")
    return match.group(1).strip()


def load_defaults() -> Dict[str, Any]:
    dev_text = APP_DEV_PATH.read_text(encoding="utf-8")
    app_text = APP_BASE_PATH.read_text(encoding="utf-8")
    port_match = re.search(r"server:\s*\n\s+port:\s*(\d+)", app_text)
    context_match = re.search(r"context-path:\s*(.+)", app_text)
    port = port_match.group(1) if port_match else "9999"
    context_path = context_match.group(1).strip() if context_match else "/"
    if context_path == "/":
        context_path = ""
    return {
        "base_url": f"http://127.0.0.1:{port}{context_path}",
        "session_match_window_seconds": int(extract_default(dev_text, "IOT_OBSERVABILITY_MESSAGE_FLOW_SESSION_MATCH_WINDOW_SECONDS")),
    }


def now_text() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def short_json(value: Any, limit: int = 320) -> str:
    text = json.dumps(value, ensure_ascii=False, default=str)
    if len(text) <= limit:
        return text
    return text[:limit] + "..."


def request_json(
    base_url: str,
    path: str,
    method: str = "GET",
    payload: Optional[Dict[str, Any]] = None,
    headers: Optional[Dict[str, str]] = None,
    timeout_seconds: int = 15,
) -> Dict[str, Any]:
    url = base_url.rstrip("/") + path
    encoded_body = None
    request_headers = {"Accept": "application/json"}
    if headers:
        request_headers.update(headers)
    if payload is not None:
        encoded_body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        request_headers["Content-Type"] = "application/json; charset=utf-8"
    req = Request(url, data=encoded_body, method=method.upper(), headers=request_headers)
    try:
        parsed_base_url = urlparse(base_url)
        local_target = parsed_base_url.hostname in {"127.0.0.1", "localhost"}
        opener = build_opener(ProxyHandler({})) if local_target else None
        open_fn = opener.open if opener is not None else urlopen
        with open_fn(req, timeout=timeout_seconds) as resp:
            body = resp.read().decode("utf-8")
    except HTTPError as ex:
        body = ex.read().decode("utf-8", errors="replace")
        raise AcceptanceError(f"{method.upper()} {path} failed: HTTP {ex.code}, body={body}")
    except URLError as ex:
        raise AcceptanceError(f"{method.upper()} {path} failed: {ex.reason}") from ex
    try:
        return json.loads(body)
    except json.JSONDecodeError as ex:
        raise AcceptanceError(f"{method.upper()} {path} returned non-JSON body: {body}") from ex


def ensure_envelope_ok(response: Dict[str, Any], path: str) -> Dict[str, Any]:
    if response.get("code") != 200:
        raise AcceptanceError(f"{path} returned code={response.get('code')}, msg={response.get('msg')}")
    return response


def require_field(data: Dict[str, Any], field_name: str, path: str) -> Any:
    value = data.get(field_name)
    if value in (None, ""):
        raise AcceptanceError(f"{path} missing required field: {field_name}")
    return value


def stage_names(timeline: Dict[str, Any]) -> List[str]:
    return [str(step.get("stage")) for step in timeline.get("steps") or []]


def ensure_timeline(timeline: Optional[Dict[str, Any]], path: str) -> Dict[str, Any]:
    if not isinstance(timeline, dict):
        raise AcceptanceError(f"{path} did not return a timeline object")
    steps = timeline.get("steps") or []
    if not steps:
        raise AcceptanceError(f"{path} returned an empty timeline")
    return timeline


def build_log_path() -> Path:
    out_dir = REPO_ROOT / "logs" / "acceptance"
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d%H%M%S")
    return out_dir / f"message-flow-acceptance-{stamp}.json"


def append_result(results: List[Dict[str, Any]], step: str, status: str, detail: str, extra: Optional[Dict[str, Any]] = None) -> None:
    item = {"at": now_text(), "step": step, "status": status, "detail": detail}
    if extra:
        item["extra"] = extra
    results.append(item)
    print(f"[{item['at']}] [{status}] {step}: {detail}")


def login(base_url: str, username: str, password: str, timeout_seconds: int) -> str:
    response = ensure_envelope_ok(
        request_json(
            base_url,
            "/api/auth/login",
            method="POST",
            payload={"username": username, "password": password},
            timeout_seconds=timeout_seconds,
        ),
        "/api/auth/login",
    )
    token = ((response.get("data") or {}).get("token"))
    if not token:
        raise AcceptanceError("/api/auth/login returned success but token is missing")
    return token


def find_recent_session(items: List[Dict[str, Any]], session_id: str) -> Optional[Dict[str, Any]]:
    for item in items:
        if str(item.get("sessionId")) == session_id:
            return item
    return None


def main() -> int:
    args = parse_args()
    defaults = load_defaults()
    base_url = (args.base_url or defaults["base_url"]).rstrip("/")
    mqtt_timeout_seconds = args.mqtt_timeout_seconds or (int(defaults["session_match_window_seconds"]) + 10)
    poll_interval_seconds = max(args.poll_interval_seconds, 0.5)
    results: List[Dict[str, Any]] = []
    log_path = build_log_path()

    stamp = datetime.now().strftime("%Y%m%d%H%M%S")
    product_key = f"accept-msgflow-product-{stamp}"
    device_code = f"accept-msgflow-device-{stamp}"
    topic = f"/sys/{product_key}/{device_code}/thing/property/post"
    http_payload = {"messageType": "property", "properties": {"temperature": 26.5, "humidity": 68}}
    mqtt_payload = {"messageType": "property", "properties": {"temperature": 28.1, "humidity": 59}}

    try:
        append_result(results, "env.baseUrl", "PASS", f"using base url {base_url}")

        token = login(base_url, args.username, args.password, args.timeout_seconds)
        auth_headers = {"Authorization": f"Bearer {token}"}
        append_result(results, "auth.login", "PASS", "login succeeded and token acquired")

        ensure_envelope_ok(
            request_json(
                base_url,
                "/api/device/product/add",
                method="POST",
                payload={
                    "productKey": product_key,
                    "productName": f"message-flow-acceptance-{stamp}",
                    "protocolCode": "mqtt-json",
                    "nodeType": 1,
                    "dataFormat": "JSON",
                },
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/device/product/add",
        )
        append_result(results, "iot.product", "PASS", f"created product {product_key}")

        ensure_envelope_ok(
            request_json(
                base_url,
                "/api/device/add",
                method="POST",
                payload={
                    "productKey": product_key,
                    "deviceName": f"message-flow-device-{stamp}",
                    "deviceCode": device_code,
                    "deviceSecret": "123456",
                    "clientId": device_code,
                    "username": device_code,
                    "password": "123456",
                },
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/device/add",
        )
        append_result(results, "iot.device", "PASS", f"created device {device_code}")

        http_submit = ensure_envelope_ok(
            request_json(
                base_url,
                "/api/message/http/report",
                method="POST",
                payload={
                    "protocolCode": "mqtt-json",
                    "productKey": product_key,
                    "deviceCode": device_code,
                    "payload": json.dumps(http_payload, ensure_ascii=False, separators=(",", ":")),
                    "topic": topic,
                    "clientId": device_code,
                    "tenantId": "1",
                },
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/message/http/report",
        )
        http_data = http_submit.get("data") or {}
        http_session_id = require_field(http_data, "sessionId", "/api/message/http/report")
        http_trace_id = require_field(http_data, "traceId", "/api/message/http/report")
        if http_data.get("timelineAvailable") is not True:
            raise AcceptanceError("HTTP submit did not return timelineAvailable=true")
        append_result(
            results,
            "messageFlow.httpSubmit",
            "PASS",
            f"sessionId={http_session_id}, traceId={http_trace_id}, status={http_data.get('status')}",
        )

        session_response = ensure_envelope_ok(
            request_json(
                base_url,
                f"/api/device/message-flow/session/{http_session_id}",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            f"/api/device/message-flow/session/{http_session_id}",
        )
        session_data = session_response.get("data")
        if not isinstance(session_data, dict):
            raise AcceptanceError("HTTP session lookup returned data=null")
        if session_data.get("traceId") != http_trace_id:
            raise AcceptanceError("HTTP session lookup traceId does not match submit result")
        session_timeline = ensure_timeline(session_data.get("timeline"), f"/api/device/message-flow/session/{http_session_id}")
        append_result(
            results,
            "messageFlow.httpSession",
            "PASS",
            f"timeline stages={stage_names(session_timeline)}",
        )

        trace_response = ensure_envelope_ok(
            request_json(
                base_url,
                f"/api/device/message-flow/trace/{http_trace_id}",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            f"/api/device/message-flow/trace/{http_trace_id}",
        )
        trace_timeline = ensure_timeline(trace_response.get("data"), f"/api/device/message-flow/trace/{http_trace_id}")
        append_result(
            results,
            "messageFlow.httpTrace",
            "PASS",
            f"trace timeline loaded with {len(trace_timeline.get('steps') or [])} steps",
        )

        overview_before_mqtt = ensure_envelope_ok(
            request_json(
                base_url,
                "/api/device/message-flow/ops/overview",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/device/message-flow/ops/overview",
        ).get("data")
        if not isinstance(overview_before_mqtt, dict) or not (overview_before_mqtt.get("stageMetrics") or []):
            raise AcceptanceError("ops overview is empty after HTTP submit")
        append_result(
            results,
            "messageFlow.opsOverview.http",
            "PASS",
            f"stageMetrics={len(overview_before_mqtt.get('stageMetrics') or [])}",
        )

        recent_query = urlencode({"size": 20, "deviceCode": device_code})
        recent_response = ensure_envelope_ok(
            request_json(
                base_url,
                f"/api/device/message-flow/recent?{recent_query}",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/device/message-flow/recent",
        )
        recent_items = recent_response.get("data") or []
        http_recent = find_recent_session(recent_items, str(http_session_id))
        if http_recent is None:
            raise AcceptanceError("recent sessions did not include the HTTP session")
        if http_recent.get("timelineAvailable") is not True:
            raise AcceptanceError("recent HTTP session did not expose timelineAvailable=true")
        append_result(results, "messageFlow.recent.http", "PASS", f"recent list size={len(recent_items)}")

        mqtt_submit = ensure_envelope_ok(
            request_json(
                base_url,
                "/api/message/mqtt/report/publish",
                method="POST",
                payload={
                    "protocolCode": "mqtt-json",
                    "productKey": product_key,
                    "deviceCode": device_code,
                    "topic": topic,
                    "payload": json.dumps(mqtt_payload, ensure_ascii=False, separators=(",", ":")),
                },
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/message/mqtt/report/publish",
        )
        mqtt_data = mqtt_submit.get("data") or {}
        mqtt_session_id = require_field(mqtt_data, "sessionId", "/api/message/mqtt/report/publish")
        if mqtt_data.get("status") != "PUBLISHED":
            raise AcceptanceError("MQTT publish did not return status=PUBLISHED")
        if mqtt_data.get("correlationPending") is not True:
            raise AcceptanceError("MQTT publish did not return correlationPending=true")
        append_result(results, "messageFlow.mqttSubmit", "PASS", f"mqtt sessionId={mqtt_session_id}")

        mqtt_session_data: Optional[Dict[str, Any]] = None
        mqtt_trace_id: Optional[str] = None
        deadline = time.time() + mqtt_timeout_seconds
        while time.time() < deadline:
            polled = ensure_envelope_ok(
                request_json(
                    base_url,
                    f"/api/device/message-flow/session/{mqtt_session_id}",
                    headers=auth_headers,
                    timeout_seconds=args.timeout_seconds,
                ),
                f"/api/device/message-flow/session/{mqtt_session_id}",
            ).get("data")
            if isinstance(polled, dict) and polled.get("traceId"):
                mqtt_trace_id = str(polled["traceId"])
                timeline = polled.get("timeline")
                if isinstance(timeline, dict) and (timeline.get("steps") or []):
                    mqtt_session_data = polled
                    break
            time.sleep(poll_interval_seconds)
        if not mqtt_trace_id or not isinstance(mqtt_session_data, dict):
            raise AcceptanceError(
                f"MQTT session {mqtt_session_id} did not expose traceId + timeline within {mqtt_timeout_seconds} seconds"
            )

        mqtt_timeline = ensure_timeline(
            mqtt_session_data.get("timeline"),
            f"/api/device/message-flow/session/{mqtt_session_id}",
        )
        append_result(
            results,
            "messageFlow.mqttSession",
            "PASS",
            f"bound traceId={mqtt_trace_id}, stages={stage_names(mqtt_timeline)}",
        )

        mqtt_trace_response = ensure_envelope_ok(
            request_json(
                base_url,
                f"/api/device/message-flow/trace/{mqtt_trace_id}",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            f"/api/device/message-flow/trace/{mqtt_trace_id}",
        )
        ensure_timeline(mqtt_trace_response.get("data"), f"/api/device/message-flow/trace/{mqtt_trace_id}")
        append_result(results, "messageFlow.mqttTrace", "PASS", f"trace {mqtt_trace_id} is queryable")

        recent_response_after_mqtt = ensure_envelope_ok(
            request_json(
                base_url,
                f"/api/device/message-flow/recent?{recent_query}",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/device/message-flow/recent",
        )
        recent_items_after_mqtt = recent_response_after_mqtt.get("data") or []
        mqtt_recent = find_recent_session(recent_items_after_mqtt, str(mqtt_session_id))
        if mqtt_recent is None:
            raise AcceptanceError("recent sessions did not include the MQTT session")
        append_result(results, "messageFlow.recent.mqtt", "PASS", f"recent list size={len(recent_items_after_mqtt)}")

        overview_after_mqtt = ensure_envelope_ok(
            request_json(
                base_url,
                "/api/device/message-flow/ops/overview",
                headers=auth_headers,
                timeout_seconds=args.timeout_seconds,
            ),
            "/api/device/message-flow/ops/overview",
        ).get("data")
        correlation_counts = {
            str(item.get("result")): item.get("count")
            for item in (overview_after_mqtt.get("correlationCounts") or [])
        }
        if "published" not in correlation_counts or "matched" not in correlation_counts:
            raise AcceptanceError(
                f"ops overview correlationCounts missing published/matched: {short_json(correlation_counts)}"
            )
        append_result(
            results,
            "messageFlow.opsOverview.mqtt",
            "PASS",
            f"correlationCounts={short_json(correlation_counts)}",
        )

        if args.expired_trace_id:
            expired_response = ensure_envelope_ok(
                request_json(
                    base_url,
                    f"/api/device/message-flow/trace/{args.expired_trace_id}",
                    headers=auth_headers,
                    timeout_seconds=args.timeout_seconds,
                ),
                f"/api/device/message-flow/trace/{args.expired_trace_id}",
            )
            if expired_response.get("data") is not None:
                raise AcceptanceError(
                    f"expired trace {args.expired_trace_id} still returned timeline data; expected data=null"
                )
            append_result(
                results,
                "messageFlow.expiredTrace",
                "PASS",
                f"trace {args.expired_trace_id} returned data=null as expected",
            )
        else:
            append_result(
                results,
                "messageFlow.expiredTrace",
                "PASS",
                "skipped because --expired-trace-id was not provided",
            )

        append_result(
            results,
            "summary",
            "PASS",
            "message-flow acceptance completed",
            extra={
                "productKey": product_key,
                "deviceCode": device_code,
                "httpSessionId": http_session_id,
                "httpTraceId": http_trace_id,
                "mqttSessionId": mqtt_session_id,
                "mqttTraceId": mqtt_trace_id,
                "logPath": str(log_path),
            },
        )
        payload = {"success": True, "results": results}
        log_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Acceptance log written to {log_path}")
        return 0
    except Exception as ex:  # pragma: no cover - top-level failure handling
        append_result(results, "summary", "FAIL", str(ex))
        payload = {"success": False, "results": results}
        log_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Acceptance log written to {log_path}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
