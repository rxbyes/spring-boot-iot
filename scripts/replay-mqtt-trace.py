#!/usr/bin/env python3
"""Replay a historical MQTT trace against the real environment."""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import socket
import struct
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import ProxyHandler, build_opener

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
APP_BASE_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application.yml"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Replay a historical MQTT trace via the real environment.")
    parser.add_argument("--trace-id", required=True, help="Historical traceId to replay.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--mqtt-broker-url", help="Override MQTT broker url. Defaults to application-dev.yml / env.")
    parser.add_argument("--mqtt-username", help="Override MQTT username. Defaults to application-dev.yml / env.")
    parser.add_argument("--mqtt-password", help="Override MQTT password. Defaults to application-dev.yml / env.")
    parser.add_argument("--app-base-url", help="Override app base url, for example http://127.0.0.1:9999.")
    parser.add_argument("--timeout-seconds", type=int, default=15, help="Polling timeout after publish.")
    parser.add_argument("--poll-interval-seconds", type=int, default=1, help="Polling interval after publish.")
    return parser.parse_args()


def load_dev_defaults() -> Dict[str, str]:
    dev_text = APP_DEV_PATH.read_text(encoding="utf-8")
    app_text = APP_BASE_PATH.read_text(encoding="utf-8")
    port_match = re.search(r"server:\s*\n\s+port:\s*(\d+)", app_text)
    context_match = re.search(r"context-path:\s*(.+)", app_text)
    port = port_match.group(1) if port_match else "9999"
    context_path = context_match.group(1).strip() if context_match else "/"
    if context_path == "/":
        context_path = ""
    return {
        "jdbc_url": extract_default(dev_text, "IOT_MYSQL_URL"),
        "user": extract_default(dev_text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(dev_text, "IOT_MYSQL_PASSWORD"),
        "mqtt_broker_url": extract_default(dev_text, "IOT_MQTT_BROKER_URL"),
        "mqtt_username": extract_default(dev_text, "IOT_MQTT_USERNAME"),
        "mqtt_password": extract_default(dev_text, "IOT_MQTT_PASSWORD"),
        "app_base_url": f"http://127.0.0.1:{port}{context_path}",
    }


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from configuration")
    return match.group(1).strip()


def resolve_runtime_args(args: argparse.Namespace) -> Dict[str, str]:
    defaults = load_dev_defaults()
    jdbc_url = args.jdbc_url or os.getenv("IOT_MYSQL_URL") or defaults["jdbc_url"]
    user = args.user or os.getenv("IOT_MYSQL_USERNAME") or defaults["user"]
    password = args.password or os.getenv("IOT_MYSQL_PASSWORD") or defaults["password"]
    mqtt_broker_url = args.mqtt_broker_url or os.getenv("IOT_MQTT_BROKER_URL") or defaults["mqtt_broker_url"]
    mqtt_username = args.mqtt_username or os.getenv("IOT_MQTT_USERNAME") or defaults["mqtt_username"]
    mqtt_password = args.mqtt_password or os.getenv("IOT_MQTT_PASSWORD") or defaults["mqtt_password"]
    app_base_url = args.app_base_url or defaults["app_base_url"]

    parsed = urlparse(jdbc_url.replace("jdbc:mysql://", "mysql://", 1))
    database = parsed.path.lstrip("/")
    if not parsed.hostname or not database:
        raise RuntimeError(f"Invalid jdbc url: {jdbc_url}")
    return {
        "db_host": parsed.hostname,
        "db_port": str(parsed.port or 3306),
        "db_name": database,
        "db_user": user,
        "db_password": password,
        "mqtt_broker_url": mqtt_broker_url,
        "mqtt_username": mqtt_username,
        "mqtt_password": mqtt_password,
        "app_base_url": app_base_url.rstrip("/"),
    }


def open_db(runtime: Dict[str, str]) -> pymysql.connections.Connection:
    return pymysql.connect(
        host=runtime["db_host"],
        port=int(runtime["db_port"]),
        user=runtime["db_user"],
        password=runtime["db_password"],
        database=runtime["db_name"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=10,
        read_timeout=30,
        write_timeout=30,
    )


def fetch_original_trace(cur: pymysql.cursors.Cursor, trace_id: str) -> Dict[str, object]:
    cur.execute(
        """
        SELECT id, trace_id, device_code, product_key, message_type, topic, payload, report_time
        FROM iot_device_message_log
        WHERE trace_id = %s
        ORDER BY report_time ASC, id ASC
        LIMIT 1
        """,
        (trace_id,),
    )
    row = cur.fetchone()
    if row:
        row["sourceTable"] = "iot_device_message_log"
        row["payloadSource"] = "payload"
        if not row.get("topic") or row.get("payload") is None:
            raise RuntimeError(f"Trace {trace_id} in iot_device_message_log does not have replayable topic/payload")
        return row

    cur.execute(
        """
        SELECT id,
               trace_id,
               device_code,
               product_key,
               message_type,
               topic,
               raw_payload AS payload,
               create_time AS report_time,
               failure_stage
        FROM iot_device_access_error_log
        WHERE trace_id = %s
        ORDER BY create_time ASC, id ASC
        LIMIT 1
        """,
        (trace_id,),
    )
    row = cur.fetchone()
    if row:
        row["sourceTable"] = "iot_device_access_error_log"
        row["payloadSource"] = "raw_payload"
        if not row.get("topic") or row.get("payload") is None:
            raise RuntimeError(f"Trace {trace_id} in iot_device_access_error_log does not have replayable topic/payload")
        return row

    raise RuntimeError(
        "Trace not found in iot_device_message_log or iot_device_access_error_log: "
        f"{trace_id}"
    )


def decode_logged_payload(payload_text: str) -> bytes:
    if payload_text is None:
        raise RuntimeError("Trace payload is empty")
    try:
        payload_json = json.loads(payload_text)
    except json.JSONDecodeError:
        return payload_text.encode("utf-8")

    if isinstance(payload_json, dict) and payload_json.get("payloadBase64"):
        return base64.b64decode(payload_json["payloadBase64"])
    if isinstance(payload_json, dict) and "rawText" in payload_json:
        return str(payload_json["rawText"]).encode("utf-8")
    return payload_text.encode("utf-8")


def build_payload_candidates(payload_text: str | None) -> list[str]:
    if payload_text is None:
        return []
    candidates = [payload_text]
    try:
        compact = json.dumps(json.loads(payload_text), ensure_ascii=False, separators=(",", ":"))
    except json.JSONDecodeError:
        compact = None
    if compact and compact not in candidates:
        candidates.append(compact)
    return candidates


def fetch_health(runtime: Dict[str, str]) -> Dict[str, object]:
    health_url = runtime["app_base_url"] + "/actuator/health/mqttConsumer"
    try:
        opener = build_opener(ProxyHandler({}))
        with opener.open(health_url, timeout=5) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        raise RuntimeError(
            f"Unable to access mqttConsumer health endpoint {health_url}: HTTP {exc.code}. "
            "If this endpoint is missing, restart the app with the latest code."
        ) from exc
    except URLError as exc:
        raise RuntimeError(
            f"Unable to access mqttConsumer health endpoint {health_url}: {exc.reason}"
        ) from exc


def ensure_consumer_healthy(health: Dict[str, object]) -> None:
    details = health.get("details") or {}
    if health.get("status") != "UP" or not details.get("connected"):
        raise RuntimeError(f"mqttConsumer health is not ready: {json.dumps(health, ensure_ascii=False)}")


def encode_remaining_length(length: int) -> bytes:
    encoded = bytearray()
    while True:
        digit = length % 128
        length //= 128
        if length > 0:
            digit |= 0x80
        encoded.append(digit)
        if length == 0:
            return bytes(encoded)


def mqtt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack("!H", len(encoded)) + encoded


def connect_packet(client_id: str, username: str, password: str) -> bytes:
    variable_header = mqtt_string("MQTT") + bytes([4, 0b11000010]) + struct.pack("!H", 60)
    payload = mqtt_string(client_id) + mqtt_string(username) + mqtt_string(password)
    fixed_header = bytes([0x10]) + encode_remaining_length(len(variable_header) + len(payload))
    return fixed_header + variable_header + payload


def publish_packet(topic: str, payload: bytes, packet_id: int) -> bytes:
    variable_header = mqtt_string(topic) + struct.pack("!H", packet_id)
    return bytes([0x32]) + encode_remaining_length(len(variable_header) + len(payload)) + variable_header + payload


def disconnect_packet() -> bytes:
    return bytes([0xE0, 0x00])


def publish_mqtt(runtime: Dict[str, str], topic: str, payload: bytes) -> None:
    broker = urlparse(runtime["mqtt_broker_url"])
    if broker.scheme not in ("tcp", "mqtt"):
        raise RuntimeError(f"Only tcp MQTT broker urls are supported, got {runtime['mqtt_broker_url']}")
    host = broker.hostname
    port = broker.port or 1883
    if not host:
        raise RuntimeError(f"Invalid mqtt broker url: {runtime['mqtt_broker_url']}")

    client_id = f"codex-replay-{int(time.time())}"
    packet_id = 1
    with socket.create_connection((host, port), timeout=10) as sock:
        sock.settimeout(10)
        sock.sendall(connect_packet(client_id, runtime["mqtt_username"], runtime["mqtt_password"]))
        connack = sock.recv(4)
        if len(connack) < 4 or connack[0] != 0x20 or connack[3] != 0x00:
            raise RuntimeError(f"MQTT connect failed: {connack!r}")
        sock.sendall(publish_packet(topic, payload, packet_id))
        puback = sock.recv(4)
        if len(puback) < 4 or puback[0] != 0x40 or puback[1] != 0x02:
            raise RuntimeError(f"MQTT publish did not receive PUBACK: {puback!r}")
        if struct.unpack("!H", puback[2:4])[0] != packet_id:
            raise RuntimeError(f"MQTT publish PUBACK packet id mismatch: {puback!r}")
        time.sleep(0.2)
        sock.sendall(disconnect_packet())


def query_replay_results(cur: pymysql.cursors.Cursor,
                         original: Dict[str, object],
                         start_time: datetime) -> Dict[str, object]:
    device_code = original.get("device_code")
    product_key = original.get("product_key")
    topic = original["topic"]
    payload_candidates = build_payload_candidates(original.get("payload"))

    message_where = ["report_time >= %s", "topic = %s"]
    message_params: list[object] = [start_time, topic]
    error_where = ["create_time >= %s", "topic = %s"]
    error_params: list[object] = [start_time, topic]

    if device_code:
        message_where.append("device_code = %s")
        message_params.append(device_code)
        error_where.append("device_code = %s")
        error_params.append(device_code)
    elif product_key:
        message_where.append("product_key = %s")
        message_params.append(product_key)
        error_where.append("product_key = %s")
        error_params.append(product_key)

    if payload_candidates:
        message_where.append(
            "payload IN (" + ", ".join(["%s"] * len(payload_candidates)) + ")"
        )
        message_params.extend(payload_candidates)
        error_where.append(
            "raw_payload IN (" + ", ".join(["%s"] * len(payload_candidates)) + ")"
        )
        error_params.extend(payload_candidates)

    cur.execute(
        f"""
        SELECT id, trace_id, device_code, product_key, message_type, topic, report_time
        FROM iot_device_message_log
        WHERE {' AND '.join(message_where)}
        ORDER BY report_time DESC, id DESC
        LIMIT 10
        """,
        message_params,
    )
    message_rows = cur.fetchall()

    cur.execute(
        f"""
        SELECT id, trace_id, device_code, product_key, failure_stage, error_message, contract_snapshot, create_time
        FROM iot_device_access_error_log
        WHERE {' AND '.join(error_where)}
        ORDER BY create_time DESC, id DESC
        LIMIT 10
        """,
        error_params,
    )
    access_error_rows = cur.fetchall()
    return {"messageRows": message_rows, "accessErrorRows": access_error_rows}


def poll_results(conn: pymysql.connections.Connection,
                 original: Dict[str, object],
                 start_time: datetime,
                 timeout_seconds: int,
                 poll_interval_seconds: int) -> Dict[str, object]:
    deadline = time.time() + max(timeout_seconds, 1)
    with conn.cursor() as cur:
        while time.time() <= deadline:
            result = query_replay_results(cur, original, start_time)
            if result["messageRows"] or result["accessErrorRows"]:
                return result
            time.sleep(max(poll_interval_seconds, 1))
        return query_replay_results(cur, original, start_time)


def main() -> int:
    args = parse_args()
    runtime = resolve_runtime_args(args)
    conn = open_db(runtime)
    try:
        with conn.cursor() as cur:
            original = fetch_original_trace(cur, args.trace_id)
        payload = decode_logged_payload(original["payload"])
        health = fetch_health(runtime)
        ensure_consumer_healthy(health)

        start_time = datetime.now()
        publish_mqtt(runtime, original["topic"], payload)
        result = poll_results(conn, original, start_time, args.timeout_seconds, args.poll_interval_seconds)

        status = "timeout"
        if result["messageRows"]:
            status = "success"
        elif result["accessErrorRows"]:
            status = "failure"

        print(json.dumps({
            "status": status,
            "health": health,
            "original": original,
            "publishedAt": start_time.isoformat(sep=" ", timespec="seconds"),
            "result": result,
        }, ensure_ascii=False, default=str, indent=2))
        return 0 if status == "success" else 1
    except Exception as exc:
        print(f"[replay-mqtt-trace] {exc}", file=sys.stderr)
        return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
