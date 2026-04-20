#!/usr/bin/env python3
"""Real-environment schema sync for Phase 4 smoke acceptance.

This script aligns known schema gaps in rm_iot without dropping tables.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Dict, List, Tuple

import pymysql


CreateSqlMap = Dict[str, str]
ColumnSpecMap = Dict[str, List[Tuple[str, str]]]
IndexSpecMap = Dict[str, List[Tuple[str, str]]]
IndexShapeMap = Dict[Tuple[str, str], Tuple[bool, Tuple[str, ...]]]

GOVERNANCE_REVIEWER_USERNAME = "governance_reviewer"
GOVERNANCE_REVIEWER_ROLE_CODE = "SUPER_ADMIN"
GOVERNANCE_REVIEWER_PREFERRED_ID = 99000001
GOVERNANCE_REVIEWER_PASSWORD_HASH = "$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq"
GOVERNANCE_REVIEWER_NICKNAME = "治理复核专员"
GOVERNANCE_REVIEWER_REAL_NAME = "治理复核专员"
GOVERNANCE_REVIEWER_PHONE = "13800009900"
GOVERNANCE_REVIEWER_EMAIL = "governance-reviewer@ghlzm.com"
GOVERNANCE_REVIEWER_REMARK = "系统级固定治理复核人账号，负责产品契约与映射规则发布/回滚审批。"
GOVERNANCE_APPROVAL_POLICY_SEEDS = (
    {
        "preferred_id": 99001001,
        "tenant_id": 0,
        "scope_type": "GLOBAL",
        "action_code": "PRODUCT_CONTRACT_RELEASE_APPLY",
        "approver_mode": "FIXED_USER",
        "remark": "产品契约发布固定复核人",
    },
    {
        "preferred_id": 99001002,
        "tenant_id": 0,
        "scope_type": "GLOBAL",
        "action_code": "PRODUCT_CONTRACT_ROLLBACK",
        "approver_mode": "FIXED_USER",
        "remark": "产品契约回滚固定复核人",
    },
    {
        "preferred_id": 99001003,
        "tenant_id": 0,
        "scope_type": "GLOBAL",
        "action_code": "VENDOR_MAPPING_RULE_PUBLISH",
        "approver_mode": "FIXED_USER",
        "remark": "映射规则发布固定复核人",
    },
    {
        "preferred_id": 99001004,
        "tenant_id": 0,
        "scope_type": "GLOBAL",
        "action_code": "VENDOR_MAPPING_RULE_ROLLBACK",
        "approver_mode": "FIXED_USER",
        "remark": "映射规则回滚固定复核人",
    },
)

COLLECTOR_CHILD_BASELINE_PRODUCTS = (
    {
        "id": 202603192100560259,
        "product_key": "nf-collect-rtu-v1",
        "product_name": "南方测绘 采集型 遥测终端",
        "manufacturer": "南方测绘",
        "description": "采集型遥测终端设备，协议 mqtt-json，直连接入",
        "remark": "shared dev collector-child baseline",
    },
    {
        "id": 202603192100560258,
        "product_key": "nf-monitor-laser-rangefinder-v1",
        "product_name": "南方测绘 监测型 激光测距仪",
        "manufacturer": "南方测绘",
        "description": "监测型激光测距设备，协议 mqtt-json，直连接入",
        "remark": "shared dev collector-child baseline",
    },
    {
        "id": 202603192100560250,
        "product_key": "nf-monitor-deep-displacement-v1",
        "product_name": "南方测绘 监测型 深部位移监测仪",
        "manufacturer": "南方测绘",
        "description": "监测型深部位移监测设备，协议 mqtt-json，直连接入",
        "remark": "shared dev collector-child baseline",
    },
    {
        "id": 202603192100560253,
        "product_key": "nf-monitor-tipping-bucket-rain-gauge-v1",
        "product_name": "南方测绘 监测型 翻斗式雨量计",
        "manufacturer": "南方测绘",
        "description": "监测型翻斗式雨量监测设备，协议 mqtt-json，直连接入",
        "remark": "shared dev rain-gauge governance baseline",
    },
)

COLLECTOR_CHILD_BASELINE_PRODUCT_MODELS = (
    {
        "id": 202604110200001,
        "product_id": 202603192100560259,
        "identifier": "temp",
        "model_name": "温度",
        "data_type": "double",
        "specs_json": {"unit": "℃", "category": "collector_status"},
        "sort_no": 1,
        "description": "collector runtime temperature",
    },
    {
        "id": 202604110200002,
        "product_id": 202603192100560259,
        "identifier": "humidity",
        "model_name": "湿度",
        "data_type": "double",
        "specs_json": {"unit": "%", "category": "collector_status"},
        "sort_no": 2,
        "description": "collector runtime humidity",
    },
    {
        "id": 202604110200003,
        "product_id": 202603192100560259,
        "identifier": "signal_4g",
        "model_name": "4G信号强度",
        "data_type": "int",
        "specs_json": {"unit": "dBm", "category": "collector_status"},
        "sort_no": 3,
        "description": "collector runtime 4g signal",
    },
    {
        "id": 202604110200011,
        "product_id": 202603192100560258,
        "identifier": "value",
        "model_name": "激光测距值",
        "data_type": "double",
        "specs_json": {"unit": "mm", "precision": 4},
        "sort_no": 1,
        "description": "laser rangefinder measurement",
    },
    {
        "id": 202604110200012,
        "product_id": 202603192100560258,
        "identifier": "sensor_state",
        "model_name": "传感器状态",
        "data_type": "int",
        "specs_json": {"category": "state"},
        "sort_no": 2,
        "description": "laser sensor state",
    },
    {
        "id": 202604110200021,
        "product_id": 202603192100560250,
        "identifier": "dispsX",
        "model_name": "顺滑动方向累计变形量",
        "data_type": "double",
        "specs_json": {"unit": "mm", "precision": 4},
        "sort_no": 1,
        "description": "deep displacement along slope",
    },
    {
        "id": 202604110200022,
        "product_id": 202603192100560250,
        "identifier": "dispsY",
        "model_name": "垂直坡面方向累计变形量",
        "data_type": "double",
        "specs_json": {"unit": "mm", "precision": 4},
        "sort_no": 2,
        "description": "deep displacement perpendicular to slope",
    },
    {
        "id": 202604110200023,
        "product_id": 202603192100560250,
        "identifier": "sensor_state",
        "model_name": "传感器状态",
        "data_type": "int",
        "specs_json": {"category": "state"},
        "sort_no": 3,
        "description": "deep displacement sensor state",
    },
    {
        "id": 202604110200031,
        "product_id": 202603192100560253,
        "identifier": "value",
        "model_name": "当前雨量",
        "data_type": "double",
        "specs_json": {"unit": "mm", "precision": 2},
        "sort_no": 1,
        "description": "rain gauge current rainfall",
    },
    {
        "id": 202604110200032,
        "product_id": 202603192100560253,
        "identifier": "totalValue",
        "model_name": "累计雨量",
        "data_type": "double",
        "specs_json": {"unit": "mm", "precision": 2},
        "sort_no": 2,
        "description": "rain gauge cumulative rainfall",
    },
)

COLLECTOR_CHILD_BASELINE_METADATA = {
    "nf-collect-rtu-v1": {
        "customMetrics": [
            {"identifier": "temp", "displayName": "温度", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 10},
            {"identifier": "humidity", "displayName": "湿度", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 20},
            {"identifier": "signal_4g", "displayName": "4G信号强度", "enabled": True, "includeInTrend": False, "includeInExtension": True, "sortNo": 30},
        ]
    },
    "nf-monitor-laser-rangefinder-v1": {
        "customMetrics": [
            {"identifier": "value", "displayName": "激光测距值", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 10},
            {"identifier": "sensor_state", "displayName": "传感器状态", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 20},
        ]
    },
    "nf-monitor-deep-displacement-v1": {
        "customMetrics": [
            {"identifier": "dispsX", "displayName": "顺滑动方向累计变形量", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 10},
            {"identifier": "dispsY", "displayName": "垂直坡面方向累计变形量", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 20},
            {"identifier": "sensor_state", "displayName": "传感器状态", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 30},
        ]
    },
    "nf-monitor-tipping-bucket-rain-gauge-v1": {
        "customMetrics": [
            {"identifier": "value", "displayName": "当前雨量", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 10},
            {"identifier": "totalValue", "displayName": "累计雨量", "enabled": True, "includeInTrend": True, "includeInExtension": True, "sortNo": 20},
        ]
    },
}

COLLECTOR_CHILD_BASELINE_NORMATIVE_METRICS = (
    {
        "id": 920021,
        "scenario_code": "phase3-deep-displacement",
        "device_family": "DEEP_DISPLACEMENT",
        "identifier": "dispsX",
        "display_name": "顺滑动方向累计变形量",
        "unit": "mm",
        "precision_digits": 4,
        "monitor_content_code": "L1",
        "monitor_type_code": "SW",
        "risk_enabled": 1,
        "trend_enabled": 1,
        "metric_dimension": "displacement",
        "threshold_type": "absolute",
        "semantic_direction": "HIGHER_IS_RISKIER",
        "gis_enabled": 1,
        "insight_enabled": 1,
        "analytics_enabled": 1,
        "status": "ACTIVE",
        "version_no": 1,
        "metadata_json": {"thresholdKind": "absolute", "riskCategory": "DEEP_DISPLACEMENT", "metricRole": "PRIMARY"},
    },
    {
        "id": 920022,
        "scenario_code": "phase3-deep-displacement",
        "device_family": "DEEP_DISPLACEMENT",
        "identifier": "dispsY",
        "display_name": "垂直坡面方向累计变形量",
        "unit": "mm",
        "precision_digits": 4,
        "monitor_content_code": "L1",
        "monitor_type_code": "SW",
        "risk_enabled": 1,
        "trend_enabled": 1,
        "metric_dimension": "displacement",
        "threshold_type": "absolute",
        "semantic_direction": "HIGHER_IS_RISKIER",
        "gis_enabled": 1,
        "insight_enabled": 1,
        "analytics_enabled": 1,
        "status": "ACTIVE",
        "version_no": 1,
        "metadata_json": {"thresholdKind": "absolute", "riskCategory": "DEEP_DISPLACEMENT", "metricRole": "PRIMARY"},
    },
    {
        "id": 920023,
        "scenario_code": "phase3-deep-displacement",
        "device_family": "DEEP_DISPLACEMENT",
        "identifier": "sensor_state",
        "display_name": "传感器状态",
        "unit": None,
        "precision_digits": 0,
        "monitor_content_code": "S1",
        "monitor_type_code": "ZT",
        "risk_enabled": 0,
        "trend_enabled": 0,
        "metric_dimension": "health_state",
        "threshold_type": "enum",
        "semantic_direction": "STATE_IS_RISK",
        "gis_enabled": 0,
        "insight_enabled": 1,
        "analytics_enabled": 0,
        "status": "ACTIVE",
        "version_no": 1,
        "metadata_json": {"usage": "health_state", "riskCategory": "DEVICE_HEALTH", "metricRole": "STATE"},
    },
    {
        "id": 920031,
        "scenario_code": "phase4-rain-gauge",
        "device_family": "RAIN_GAUGE",
        "identifier": "value",
        "display_name": "当前雨量",
        "unit": "mm",
        "precision_digits": 2,
        "monitor_content_code": "L3",
        "monitor_type_code": "YL",
        "risk_enabled": 1,
        "trend_enabled": 1,
        "metric_dimension": "rainfall",
        "threshold_type": "absolute",
        "semantic_direction": "HIGHER_IS_RISKIER",
        "gis_enabled": 0,
        "insight_enabled": 1,
        "analytics_enabled": 1,
        "status": "ACTIVE",
        "version_no": 1,
        "metadata_json": {"thresholdKind": "absolute", "riskCategory": "RAIN_GAUGE", "metricRole": "PRIMARY"},
    },
    {
        "id": 920032,
        "scenario_code": "phase4-rain-gauge",
        "device_family": "RAIN_GAUGE",
        "identifier": "totalValue",
        "display_name": "累计雨量",
        "unit": "mm",
        "precision_digits": 2,
        "monitor_content_code": "L3",
        "monitor_type_code": "YL",
        "risk_enabled": 0,
        "trend_enabled": 1,
        "metric_dimension": "rainfall",
        "threshold_type": "cumulative",
        "semantic_direction": "HIGHER_IS_RISKIER",
        "gis_enabled": 0,
        "insight_enabled": 1,
        "analytics_enabled": 1,
        "status": "ACTIVE",
        "version_no": 1,
        "metadata_json": {"thresholdKind": "cumulative", "riskCategory": "RAIN_GAUGE", "metricRole": "CONTEXT"},
    },
)

COLLECTOR_CHILD_BASELINE_MAPPING_RULES = (
    {
        "id": 202604110800001,
        "product_id": 202603192100560253,
        "protocol_code": "mqtt-json",
        "scenario_code": "phase4-rain-gauge",
        "device_family": "RAIN_GAUGE",
        "raw_identifier": "L3_YL_1.value",
        "logical_channel_code": "L3_YL_1",
        "target_normative_identifier": "value",
    },
    {
        "id": 202604110800002,
        "product_id": 202603192100560253,
        "protocol_code": "mqtt-json",
        "scenario_code": "phase4-rain-gauge",
        "device_family": "RAIN_GAUGE",
        "raw_identifier": "L3_YL_1.totalValue",
        "logical_channel_code": "L3_YL_1",
        "target_normative_identifier": "totalValue",
    },
)

LASER_COLLECTOR_DEVICE_IDS = {
    "SK00EA0D1307988": 202604110300001,
    "SK00EA0D1307967": 202604110300002,
    "SK00EA0D1307986": 202604110300003,
    "SK00EA0D1307987": 202604110300004,
    "SK00EA0D1308009": 202604110300005,
    "SK00EA0D1307984": 202604110300006,
    "SK00EA0D1308006": 202604110300007,
    "SK00EA0D1307968": 202604110300008,
    "SK00E90D1307874": 202604110300009,
    "SK00EA0D1307992": 202604110300010,
}

DEEP_COLLECTOR_DEVICE_IDS = {
    "SK00FB0D1310195": 202604110300101,
    "SK00FB0D1310216": 202604110300102,
    "SK00FB0D1310215": 202604110300103,
    "SK00FB0D1310000": 202604110300104,
}

SINGLE_DEEP_DEVICE_ID = 202604110300201

LASER_RELATION_MAPPINGS = {
    "SK00EA0D1307988": {
        "L1_LF_1": "202018108",
        "L1_LF_2": "202018109",
        "L1_LF_3": "202018110",
        "L1_LF_4": "202018111",
        "L1_LF_5": "202018112",
        "L1_LF_6": "202018113",
        "L1_LF_7": "202018114",
        "L1_LF_8": "202018115",
        "L1_LF_9": "202018116",
    },
    "SK00EA0D1307967": {
        "L1_LF_1": "202018134",
        "L1_LF_2": "202018123",
        "L1_LF_3": "202018129",
        "L1_LF_4": "202018122",
        "L1_LF_5": "202018120",
        "L1_LF_6": "202018138",
        "L1_LF_7": "202018140",
        "L1_LF_8": "202018125",
        "L1_LF_9": "202018132",
    },
    "SK00EA0D1307986": {
        "L1_LF_1": "202018143",
        "L1_LF_2": "202018135",
        "L1_LF_3": "202018121",
        "L1_LF_4": "202018137",
        "L1_LF_5": "202018142",
        "L1_LF_6": "202018130",
        "L1_LF_7": "202018127",
        "L1_LF_8": "202018118",
        "L1_LF_9": "202018139",
    },
    "SK00EA0D1307987": {
        "L1_LF_1": "202018124",
        "L1_LF_2": "202018131",
        "L1_LF_3": "202018128",
        "L1_LF_4": "202018117",
        "L1_LF_5": "202018141",
        "L1_LF_6": "202018133",
        "L1_LF_7": "202018119",
        "L1_LF_8": "202018126",
        "L1_LF_9": "202018136",
    },
    "SK00EA0D1308009": {
        "L1_LF_1": "202018189",
        "L1_LF_2": "202018144",
        "L1_LF_3": "202018145",
    },
    "SK00EA0D1307984": {
        "L1_LF_1": "202018197",
        "L1_LF_2": "202018193",
        "L1_LF_3": "202018198",
    },
    "SK00EA0D1308006": {
        "L1_LF_1": "202018186",
        "L1_LF_2": "202018196",
        "L1_LF_3": "202018194",
    },
    "SK00EA0D1307968": {
        "L1_LF_1": "202018195",
        "L1_LF_2": "202018192",
        "L1_LF_3": "202018190",
    },
    "SK00E90D1307874": {
        "L1_LF_1": "202018188",
        "L1_LF_2": "202018200",
        "L1_LF_3": "202018191",
    },
    "SK00EA0D1307992": {
        "L1_LF_1": "202018187",
        "L1_LF_2": "202018203",
        "L1_LF_3": "202018201",
    },
}

DEEP_RELATION_MAPPINGS = {
    "SK00FB0D1310195": {
        "L1_SW_1": "84330701",
        "L1_SW_2": "84330695",
        "L1_SW_3": "84330697",
        "L1_SW_4": "84330699",
        "L1_SW_5": "84330686",
        "L1_SW_6": "84330687",
        "L1_SW_7": "84330691",
        "L1_SW_8": "84330696",
    },
    "SK00FB0D1310216": {
        "L1_SW_1": "84330643",
        "L1_SW_2": "84330640",
        "L1_SW_3": "84330637",
        "L1_SW_4": "84330673",
        "L1_SW_5": "84330674",
        "L1_SW_6": "84330675",
        "L1_SW_7": "84330672",
        "L1_SW_8": "84330677",
    },
    "SK00FB0D1310215": {
        "L1_SW_1": "84330671",
        "L1_SW_2": "84330641",
        "L1_SW_3": "84330635",
        "L1_SW_4": "84330630",
        "L1_SW_5": "84330627",
        "L1_SW_6": "84330619",
        "L1_SW_7": "84330676",
        "L1_SW_8": "84330634",
    },
    "SK00FB0D1310000": {
        "L1_SW_1": "84330693",
        "L1_SW_2": "84330707",
        "L1_SW_3": "84330705",
        "L1_SW_4": "84330708",
        "L1_SW_5": "84330706",
        "L1_SW_6": "84330702",
        "L1_SW_7": "84330709",
        "L1_SW_8": "89908808",
    },
}

LASER_SAMPLE_LATEST = {
    "202018143": 10.86,
    "202018135": 6.95,
    "202018121": 2473.72,
    "202018137": 2473.72,
    "202018142": 6.73,
    "202018130": 2473.72,
    "202018127": 2473.72,
    "202018118": 6.82,
    "202018139": 10.80,
}

DEEP_SAMPLE_LATEST = {
    "84330701": (-0.0446, 0.0293),
    "84330695": (-0.0295, 0.0328),
    "84330697": (-0.0255, 0.0403),
    "84330699": (-0.0173, 0.0422),
    "84330686": (-0.0249, 0.0272),
    "84330687": (-0.0235, 0.0108),
    "84330691": (-0.0365, 0.0009),
    "84330696": (-0.0453, -0.0164),
}


def collector_child_device_seeds() -> List[Tuple[int, int, str, str, str, str]]:
    seeds: List[Tuple[int, int, str, str, str, str]] = []
    for device_code, device_id in LASER_COLLECTOR_DEVICE_IDS.items():
        seeds.append((device_id, 202603192100560259, device_code, f"NF-COLLECTOR-{device_code}", "shared-dev-laser", "laser-collector"))
    for device_code, device_id in DEEP_COLLECTOR_DEVICE_IDS.items():
        seeds.append((device_id, 202603192100560259, device_code, f"NF-COLLECTOR-{device_code}", "shared-dev-deep", "deep-collector"))
    seeds.append(
        (
            SINGLE_DEEP_DEVICE_ID,
            202603192100560250,
            "SK00EB0D1308310",
            "NF-DEEP-SINGLE-SK00EB0D1308310",
            "shared-dev-deep-single",
            "deep-single",
        )
    )
    for mappings in LASER_RELATION_MAPPINGS.values():
        for child_code in mappings.values():
            seeds.append((int(child_code), 202603192100560258, child_code, f"NF-LASER-{child_code}", "shared-dev-laser-child", "laser-child"))
    for mappings in DEEP_RELATION_MAPPINGS.values():
        for child_code in mappings.values():
            seeds.append((int(child_code), 202603192100560250, child_code, f"NF-DEEP-{child_code}", "shared-dev-deep-child", "deep-child"))
    unique_by_id: Dict[int, Tuple[int, int, str, str, str, str]] = {}
    for seed in seeds:
        unique_by_id.setdefault(seed[0], seed)
    return list(unique_by_id.values())


def collector_child_relation_seeds() -> List[Tuple[int, str, str, str, int, str, str, str]]:
    seeds: List[Tuple[int, str, str, str, int, str, str, str]] = []
    relation_id = 202604110500001
    for parent_code, mappings in LASER_RELATION_MAPPINGS.items():
        for logical_code, child_code in mappings.items():
            seeds.append(
                (
                    relation_id,
                    parent_code,
                    logical_code,
                    child_code,
                    202603192100560258,
                    "nf-monitor-laser-rangefinder-v1",
                    "LF_VALUE",
                    "SENSOR_STATE",
                )
            )
            relation_id += 1
    for parent_code, mappings in DEEP_RELATION_MAPPINGS.items():
        for logical_code, child_code in mappings.items():
            seeds.append(
                (
                    relation_id,
                    parent_code,
                    logical_code,
                    child_code,
                    202603192100560250,
                    "nf-monitor-deep-displacement-v1",
                    "LEGACY",
                    "SENSOR_STATE",
                )
            )
            relation_id += 1
    return seeds


def collector_child_property_seeds() -> List[Tuple[int, str, str, str, str, str]]:
    seeds: List[Tuple[int, str, str, str, str, str]] = []
    property_id = 202604110700001
    for device_code, identifier, property_name, property_value, value_type in (
        ("SK00EA0D1307986", "temp", "温度", "20.31", "double"),
        ("SK00EA0D1307986", "humidity", "湿度", "89.04", "double"),
        ("SK00EA0D1307986", "signal_4g", "4G信号强度", "-71", "int"),
        ("SK00FB0D1310195", "temp", "温度", "19.82", "double"),
        ("SK00FB0D1310195", "humidity", "湿度", "71.55", "double"),
        ("SK00FB0D1310195", "signal_4g", "4G信号强度", "-69", "int"),
        ("SK00EB0D1308310", "dispsX", "顺滑动方向累计变形量", "-0.0166", "double"),
        ("SK00EB0D1308310", "dispsY", "垂直坡面方向累计变形量", "-0.0368", "double"),
        ("SK00EB0D1308310", "sensor_state", "传感器状态", "0", "int"),
    ):
        seeds.append((property_id, device_code, identifier, property_name, property_value, value_type))
        property_id += 1
    for child_code, value in LASER_SAMPLE_LATEST.items():
        seeds.append((property_id, child_code, "value", "激光测距值", str(value), "double"))
        property_id += 1
        seeds.append((property_id, child_code, "sensor_state", "传感器状态", "0", "int"))
        property_id += 1
    for child_code, (disps_x, disps_y) in DEEP_SAMPLE_LATEST.items():
        seeds.append((property_id, child_code, "dispsX", "顺滑动方向累计变形量", str(disps_x), "double"))
        property_id += 1
        seeds.append((property_id, child_code, "dispsY", "垂直坡面方向累计变形量", str(disps_y), "double"))
        property_id += 1
        seeds.append((property_id, child_code, "sensor_state", "传感器状态", "0", "int"))
        property_id += 1
    return seeds


REPO_ROOT = Path(__file__).resolve().parents[1]
SCHEMA_SYNC_MANIFEST_PATH = REPO_ROOT / "schema" / "generated" / "mysql-schema-sync.json"


def _sync_create_table_sql(sql: str) -> str:
    return sql.replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ", 1)


def _sync_add_column_definition(column: dict) -> str:
    definition = str(column["definition"]).strip()
    comment = str(column["commentZh"]).replace("'", "''")
    return f"{definition} COMMENT '{comment}'"


def _sync_add_index_sql(table: str, index: dict) -> str:
    columns = ", ".join(f"`{column}`" for column in index["columns"])
    kind = str(index["kind"]).upper()
    name = str(index["name"])
    if kind == "PRIMARY":
        return f"ALTER TABLE `{table}` ADD PRIMARY KEY ({columns})"
    if kind == "UNIQUE":
        return f"ALTER TABLE `{table}` ADD UNIQUE INDEX `{name}` ({columns})"
    return f"ALTER TABLE `{table}` ADD INDEX `{name}` ({columns})"


def load_schema_sync_manifest() -> dict[str, object]:
    with SCHEMA_SYNC_MANIFEST_PATH.open("r", encoding="utf-8") as handle:
        raw_manifest = json.load(handle)

    create_table_sql: CreateSqlMap = {
        entry["name"]: _sync_create_table_sql(entry["sql"])
        for entry in raw_manifest.get("createTableSql", [])
    }
    columns_to_add: ColumnSpecMap = {
        entry["name"]: [
            (item["name"], _sync_add_column_definition(item))
            for item in entry.get("columns", [])
        ]
        for entry in raw_manifest.get("columnsToAdd", [])
    }
    indexes_to_add: IndexSpecMap = {
        entry["name"]: [
            (item["name"], _sync_add_index_sql(entry["name"], item))
            for item in entry.get("indexes", [])
            if str(item["kind"]).upper() != "PRIMARY"
        ]
        for entry in raw_manifest.get("indexesToAdd", [])
        if any(str(item["kind"]).upper() != "PRIMARY" for item in entry.get("indexes", []))
    }
    view_sql: CreateSqlMap = {
        entry["name"]: entry["sql"]
        for entry in raw_manifest.get("viewSql", [])
    }
    table_lifecycle: Dict[str, str] = {
        str(name): str(lifecycle)
        for name, lifecycle in raw_manifest.get("tableLifecycle", {}).items()
    }
    unique_index_duplicate_guards: Dict[Tuple[str, str], Tuple[str, ...]] = {
        (entry["name"], item["name"]): tuple(item["columns"])
        for entry in raw_manifest.get("indexesToAdd", [])
        for item in entry.get("indexes", [])
        if str(item["kind"]).upper() == "UNIQUE"
    }
    expected_index_shapes: IndexShapeMap = {
        (entry["name"], item["name"]): (
            str(item["kind"]).upper() == "UNIQUE",
            tuple(item["columns"]),
        )
        for entry in raw_manifest.get("indexesToAdd", [])
        for item in entry.get("indexes", [])
        if str(item["kind"]).upper() != "PRIMARY"
    }
    return {
        "createTableSql": create_table_sql,
        "columnsToAdd": columns_to_add,
        "indexesToAdd": indexes_to_add,
        "viewSql": view_sql,
        "tableLifecycle": table_lifecycle,
        "uniqueIndexDuplicateGuards": unique_index_duplicate_guards,
        "expectedIndexShapes": expected_index_shapes,
    }


_SCHEMA_SYNC_MANIFEST = load_schema_sync_manifest()
CREATE_TABLE_SQL: CreateSqlMap = _SCHEMA_SYNC_MANIFEST["createTableSql"]
VIEW_SQL: CreateSqlMap = _SCHEMA_SYNC_MANIFEST["viewSql"]
COLUMNS_TO_ADD: ColumnSpecMap = _SCHEMA_SYNC_MANIFEST["columnsToAdd"]
INDEXES_TO_ADD: IndexSpecMap = _SCHEMA_SYNC_MANIFEST["indexesToAdd"]
TABLE_LIFECYCLE: Dict[str, str] = _SCHEMA_SYNC_MANIFEST["tableLifecycle"]
UNIQUE_INDEX_DUPLICATE_GUARDS: Dict[Tuple[str, str], Tuple[str, ...]] = _SCHEMA_SYNC_MANIFEST[
    "uniqueIndexDuplicateGuards"
]
EXPECTED_INDEX_SHAPES: IndexShapeMap = _SCHEMA_SYNC_MANIFEST["expectedIndexShapes"]


BINDING_INDEX_EXPECTED_SHAPES: Dict[Tuple[str, str], Tuple[bool, Tuple[str, ...]]] = {
    ("risk_metric_linkage_binding", "uk_risk_metric_linkage_active"): (
        True,
        ("tenant_id", "risk_metric_id", "linkage_rule_id", "deleted"),
    ),
    ("risk_metric_linkage_binding", "idx_risk_metric_linkage_rule"): (
        False,
        ("linkage_rule_id", "binding_status", "deleted"),
    ),
    ("risk_metric_linkage_binding", "idx_risk_metric_linkage_metric"): (
        False,
        ("risk_metric_id", "binding_status", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "uk_risk_metric_plan_active"): (
        True,
        ("tenant_id", "risk_metric_id", "emergency_plan_id", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_rule"): (
        False,
        ("emergency_plan_id", "binding_status", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_metric"): (
        False,
        ("risk_metric_id", "binding_status", "deleted"),
    ),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run rm_iot schema sync for real environment.")
    parser.add_argument("--host", default=os.getenv("IOT_MYSQL_HOST", "8.130.107.120"))
    parser.add_argument("--port", type=int, default=int(os.getenv("IOT_MYSQL_PORT", "3306")))
    parser.add_argument("--db", default=os.getenv("IOT_MYSQL_DB", "rm_iot"))
    parser.add_argument("--user", default=os.getenv("IOT_MYSQL_USERNAME", "root"))
    parser.add_argument("--password", default=os.getenv("IOT_MYSQL_PASSWORD", "mI8%pB1*gD"))
    return parser.parse_args()


def table_exists(cur: pymysql.cursors.Cursor, db: str, table: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s
        LIMIT 1
        """,
        (db, table),
    )
    return cur.fetchone() is not None


def column_exists(cur: pymysql.cursors.Cursor, db: str, table: str, column: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND COLUMN_NAME=%s
        LIMIT 1
        """,
        (db, table, column),
    )
    return cur.fetchone() is not None


def index_exists(cur: pymysql.cursors.Cursor, db: str, table: str, index: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        LIMIT 1
        """,
        (db, table, index),
    )
    return cur.fetchone() is not None


def load_index_shape(
    cur: pymysql.cursors.Cursor, db: str, table: str, index: str
) -> Tuple[bool, Tuple[str, ...]] | None:
    cur.execute(
        """
        SELECT NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        ORDER BY SEQ_IN_INDEX
        """,
        (db, table, index),
    )
    rows = cur.fetchall()
    if not rows:
        return None
    is_unique = int(rows[0][0]) == 0
    columns = tuple(row[1] for row in rows)
    return is_unique, columns


def ensure_existing_index_shape(cur: pymysql.cursors.Cursor, db: str, table: str, index: str) -> None:
    expected = EXPECTED_INDEX_SHAPES.get((table, index))
    if expected is None:
        return
    actual = load_index_shape(cur, db, table, index)
    if actual is None:
        return
    if actual != expected:
        expected_unique, expected_columns = expected
        actual_unique, actual_columns = actual
        expected_kind = "UNIQUE" if expected_unique else "INDEX"
        actual_kind = "UNIQUE" if actual_unique else "INDEX"
        raise RuntimeError(
            f"Existing index {table}.{index} drifts from expected shape: "
            f"expected {expected_kind} {expected_columns}, got {actual_kind} {actual_columns}."
        )


def ensure_dict_defaults(cur: pymysql.cursors.Cursor) -> None:
    cur.execute(
        "ALTER TABLE sys_dict MODIFY COLUMN dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy dict value'"
    )
    cur.execute(
        "ALTER TABLE sys_dict MODIFY COLUMN dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'legacy dict label'"
    )
    cur.execute("UPDATE sys_dict SET dict_value='' WHERE dict_value IS NULL")
    cur.execute("UPDATE sys_dict SET dict_label='' WHERE dict_label IS NULL")


def next_table_id(cur: pymysql.cursors.Cursor, table: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX(id), 0) + 1 FROM `{table}`")
    return int(cur.fetchone()[0])


def next_preferred_id(cur: pymysql.cursors.Cursor, table: str, preferred_id: int | None) -> int:
    if preferred_id is None:
        return next_table_id(cur, table)
    cur.execute(f"SELECT COUNT(1) FROM `{table}` WHERE id = %s", (preferred_id,))
    if int(cur.fetchone()[0]) == 0:
        return preferred_id
    return next_table_id(cur, table)


def ensure_device_relation_integrity(cur: pymysql.cursors.Cursor, db: str) -> None:
    required_tables = ("iot_device_relation", "iot_device", "iot_product")
    for table in required_tables:
        if not table_exists(cur, db, table):
            print(f"[skip] device relation integrity: table missing {table}")
            return

    cur.execute(
        """
        DELETE rel
        FROM iot_device_relation rel
        INNER JOIN iot_device_relation newer
            ON newer.tenant_id = rel.tenant_id
           AND newer.parent_device_code = rel.parent_device_code
           AND newer.logical_channel_code = rel.logical_channel_code
           AND newer.deleted = rel.deleted
           AND rel.deleted = 0
           AND (
               COALESCE(rel.update_time, '1970-01-01 00:00:00') < COALESCE(newer.update_time, '1970-01-01 00:00:00')
               OR (
                   COALESCE(rel.update_time, '1970-01-01 00:00:00') = COALESCE(newer.update_time, '1970-01-01 00:00:00')
                   AND COALESCE(rel.create_time, '1970-01-01 00:00:00') < COALESCE(newer.create_time, '1970-01-01 00:00:00')
               )
               OR (
                   COALESCE(rel.update_time, '1970-01-01 00:00:00') = COALESCE(newer.update_time, '1970-01-01 00:00:00')
                   AND COALESCE(rel.create_time, '1970-01-01 00:00:00') = COALESCE(newer.create_time, '1970-01-01 00:00:00')
                   AND rel.id < newer.id
               )
           )
        """
    )
    print(f"[repair] iot_device_relation duplicate rows deleted: {cur.rowcount}")

    cur.execute(
        """
        UPDATE iot_device_relation rel
        INNER JOIN iot_device parent_device
            ON parent_device.tenant_id = rel.tenant_id
           AND parent_device.device_code = rel.parent_device_code
           AND parent_device.deleted = 0
        INNER JOIN iot_device child_device
            ON child_device.tenant_id = rel.tenant_id
           AND child_device.device_code = rel.child_device_code
           AND child_device.deleted = 0
        LEFT JOIN iot_product child_product
            ON child_product.id = child_device.product_id
           AND child_product.deleted = 0
        SET rel.parent_device_id = parent_device.id,
            rel.child_device_id = child_device.id,
            rel.child_product_id = child_device.product_id,
            rel.child_product_key = child_product.product_key,
            rel.update_by = 1,
            rel.update_time = NOW()
        WHERE rel.deleted = 0
          AND (
              rel.parent_device_id <> parent_device.id
              OR rel.child_device_id <> child_device.id
              OR COALESCE(rel.child_product_id, -1) <> COALESCE(child_device.product_id, -1)
              OR COALESCE(rel.child_product_key, '') <> COALESCE(child_product.product_key, '')
          )
        """
    )
    print(f"[repair] iot_device_relation device references realigned: {cur.rowcount}")

    cur.execute(
        """
        SELECT COUNT(1)
        FROM iot_device_relation rel
        LEFT JOIN iot_device parent_device
            ON parent_device.tenant_id = rel.tenant_id
           AND parent_device.device_code = rel.parent_device_code
           AND parent_device.deleted = 0
        LEFT JOIN iot_device child_device
            ON child_device.tenant_id = rel.tenant_id
           AND child_device.device_code = rel.child_device_code
           AND child_device.deleted = 0
        WHERE rel.deleted = 0
          AND (parent_device.id IS NULL OR child_device.id IS NULL)
        """
    )
    unresolved_count = int(cur.fetchone()[0])
    if unresolved_count > 0:
        raise RuntimeError(
            f"iot_device_relation still contains {unresolved_count} active rows whose parent/child device_code "
            "cannot be resolved to active iot_device records."
        )


def cleanup_duplicate_level_dicts(
    cur: pymysql.cursors.Cursor,
    canonical_dict_id: int,
    dict_code: str,
) -> None:
    cur.execute(
        """
        UPDATE sys_dict
        SET status = 0,
            deleted = 1,
            update_by = 1,
            update_time = NOW()
        WHERE tenant_id = 1
          AND dict_code = %s
          AND id <> %s
        """,
        (dict_code, canonical_dict_id),
    )
    cur.execute(
        """
        UPDATE sys_dict_item
        SET status = 0,
            deleted = 1,
            update_by = 1,
            update_time = NOW()
        WHERE tenant_id = 1
          AND dict_id IN (
              SELECT id FROM (
                  SELECT id
                  FROM sys_dict
                  WHERE tenant_id = 1
                    AND dict_code = %s
                    AND id <> %s
              ) duplicated
          )
        """,
        (dict_code, canonical_dict_id),
    )


def ensure_level_dict(
    cur: pymysql.cursors.Cursor,
    db: str,
    *,
    dict_code: str,
    dict_name: str,
    sort_no: int,
    dict_remark: str,
    preferred_dict_id: int | None,
    target_items: List[Tuple[str, str, int, str, List[str], int | None]],
) -> None:
    if not table_exists(cur, db, "sys_dict") or not table_exists(cur, db, "sys_dict_item"):
        return

    cur.execute(
        """
        SELECT id
        FROM sys_dict
        WHERE tenant_id = 1 AND dict_code = %s
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """,
        (dict_code,),
    )
    existing_dict = cur.fetchone()
    dict_id = int(existing_dict[0]) if existing_dict else next_preferred_id(cur, "sys_dict", preferred_dict_id)
    cleanup_duplicate_level_dicts(cur, dict_id, dict_code)

    if existing_dict:
        cur.execute(
            """
            UPDATE sys_dict
            SET dict_name=%s,
                dict_type='text',
                status=1,
                sort_no=%s,
                remark=%s,
                update_by=1,
                update_time=NOW(),
                deleted=0
            WHERE id=%s
            """,
            (dict_name, sort_no, dict_remark, dict_id),
        )
    else:
        cur.execute(
            """
            INSERT INTO sys_dict (
                id, tenant_id, dict_name, dict_code, dict_type, status, sort_no, remark,
                create_by, create_time, update_by, update_time, deleted
            ) VALUES (%s, 1, %s, %s, 'text', 1, %s, %s, 1, NOW(), 1, NOW(), 0)
            """,
            (dict_id, dict_name, dict_code, sort_no, dict_remark),
        )

    cur.execute(
        """
        SELECT id, item_value
        FROM sys_dict_item
        WHERE tenant_id = 1 AND dict_id = %s
        ORDER BY deleted ASC, sort_no ASC, id ASC
        """,
        (dict_id,),
    )
    existing_items = cur.fetchall()
    items_by_value: Dict[str, List[int]] = {}
    for item_id, item_value in existing_items:
        normalized_value = (item_value or "").strip().lower()
        items_by_value.setdefault(normalized_value, []).append(int(item_id))

    retained_ids: List[int] = []

    for value, label, item_sort_no, remark, legacy_values, preferred_item_id in target_items:
        candidate_id = None
        for candidate_value in [value, *legacy_values]:
            if not candidate_value:
                continue
            candidate_ids = items_by_value.get(candidate_value, [])
            while candidate_ids:
                maybe_id = candidate_ids.pop(0)
                if maybe_id not in retained_ids:
                    candidate_id = maybe_id
                    break
            if candidate_id is not None:
                break

        if candidate_id is None:
            candidate_id = next_preferred_id(cur, "sys_dict_item", preferred_item_id)
            cur.execute(
                """
                INSERT INTO sys_dict_item (
                    id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (%s, 1, %s, %s, %s, 'string', 1, %s, %s, 1, NOW(), 1, NOW(), 0)
                """,
                (candidate_id, dict_id, label, value, item_sort_no, remark),
            )
        else:
            cur.execute(
                """
                UPDATE sys_dict_item
                SET dict_id=%s,
                    item_name=%s,
                    item_value=%s,
                    item_type='string',
                    status=1,
                    sort_no=%s,
                    remark=%s,
                    update_by=1,
                    update_time=NOW(),
                    deleted=0
                WHERE id=%s
                """,
                (dict_id, label, value, item_sort_no, remark, candidate_id),
            )
        retained_ids.append(candidate_id)

    if retained_ids:
        retained_placeholders = ", ".join(["%s"] * len(retained_ids))
        cur.execute(
            f"""
            UPDATE sys_dict_item
            SET status=0,
                deleted=1,
                update_by=1,
                update_time=NOW()
            WHERE tenant_id = 1
              AND dict_id = %s
              AND id NOT IN ({retained_placeholders})
            """,
            (dict_id, *retained_ids),
        )


def level_dict_targets() -> Dict[str, Dict[str, object]]:
    return {
        "risk_point_level": {
            "dict_name": "风险点等级",
            "sort_no": 1,
            "dict_remark": "风险点档案等级字典",
            "preferred_dict_id": 7201,
            "target_items": [
                ("level_1", "一级风险点", 1, "风险点等级-一级风险点", [], 7301),
                ("level_2", "二级风险点", 2, "风险点等级-二级风险点", [], 7302),
                ("level_3", "三级风险点", 3, "风险点等级-三级风险点", [], 7303),
            ],
        },
        "alarm_level": {
            "dict_name": "告警等级",
            "sort_no": 2,
            "dict_remark": "告警等级四色字典",
            "preferred_dict_id": 7202,
            "target_items": [
                ("red", "红色", 1, "告警等级-红色", ["critical"], 7304),
                ("orange", "橙色", 2, "告警等级-橙色", ["warning", "high"], 7305),
                ("yellow", "黄色", 3, "告警等级-黄色", ["medium"], 7306),
                ("blue", "蓝色", 4, "告警等级-蓝色", ["info", "low"], 7307),
            ],
        },
        "risk_level": {
            "dict_name": "风险态势等级",
            "sort_no": 3,
            "dict_remark": "运行态风险颜色字典",
            "preferred_dict_id": 7203,
            "target_items": [
                ("red", "红色", 1, "风险态势等级-红色", ["critical"], 7308),
                ("orange", "橙色", 2, "风险态势等级-橙色", ["warning"], 7309),
                ("yellow", "黄色", 3, "风险态势等级-黄色", [], 7310),
                ("blue", "蓝色", 4, "风险态势等级-蓝色", ["info"], 7311),
            ],
        },
    }


def ensure_level_dicts(cur: pymysql.cursors.Cursor, db: str) -> None:
    for dict_code, definition in level_dict_targets().items():
        ensure_level_dict(cur, db, dict_code=dict_code, **definition)


def system_governance_dict_targets() -> Dict[str, Dict[str, object]]:
    return {
        "help_doc_category": {
            "dict_name": "帮助文档分类",
            "sort_no": 4,
            "dict_remark": "帮助中心分类字典",
            "preferred_dict_id": 7204,
            "target_items": [
                ("business", "业务类", 1, "帮助文档分类-业务类", [], 7312),
                ("technical", "技术类", 2, "帮助文档分类-技术类", [], 7313),
                ("faq", "常见问题", 3, "帮助文档分类-常见问题", [], 7314),
            ],
        },
        "notification_channel_type": {
            "dict_name": "通知渠道类型",
            "sort_no": 5,
            "dict_remark": "通知渠道类型字典",
            "preferred_dict_id": 7205,
            "target_items": [
                ("email", "邮件", 1, "通知渠道类型-邮件", [], 7315),
                ("sms", "短信", 2, "通知渠道类型-短信", [], 7316),
                ("webhook", "Webhook", 3, "通知渠道类型-Webhook", [], 7317),
                ("wechat", "微信", 4, "通知渠道类型-微信", [], 7318),
                ("feishu", "飞书", 5, "通知渠道类型-飞书", [], 7319),
                ("dingtalk", "钉钉", 6, "通知渠道类型-钉钉", [], 7320),
            ],
        },
    }


def ensure_system_governance_dicts(cur: pymysql.cursors.Cursor, db: str) -> None:
    for dict_code, definition in system_governance_dict_targets().items():
        ensure_level_dict(cur, db, dict_code=dict_code, **definition)


def normalize_color_case(column: str) -> str:
    return f"""
    CASE LOWER(TRIM({column}))
        WHEN 'critical' THEN 'red'
        WHEN 'high' THEN 'orange'
        WHEN 'warning' THEN 'orange'
        WHEN 'medium' THEN 'yellow'
        WHEN 'info' THEN 'blue'
        WHEN 'low' THEN 'blue'
        WHEN 'red' THEN 'red'
        WHEN 'orange' THEN 'orange'
        WHEN 'yellow' THEN 'yellow'
        WHEN 'blue' THEN 'blue'
        ELSE LOWER(TRIM({column}))
    END
    """


def migrate_level_values(cur: pymysql.cursors.Cursor, db: str) -> None:
    if table_exists(cur, db, "risk_point") and column_exists(cur, db, "risk_point", "current_risk_level"):
        cur.execute(
            f"""
            UPDATE `risk_point`
            SET current_risk_level = {normalize_color_case('current_risk_level')}
            WHERE current_risk_level IS NOT NULL
              AND TRIM(current_risk_level) <> ''
            """
        )
        if column_exists(cur, db, "risk_point", "risk_level"):
            cur.execute(
                f"""
                UPDATE `risk_point`
                SET current_risk_level = {normalize_color_case('risk_level')}
                WHERE (current_risk_level IS NULL OR TRIM(current_risk_level) = '')
                  AND risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )
            cur.execute(
                f"""
                UPDATE `risk_point`
                SET risk_level = {normalize_color_case('risk_level')}
                WHERE risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )

    if table_exists(cur, db, "iot_event_record") and column_exists(cur, db, "iot_event_record", "risk_level"):
        cur.execute(
            f"""
            UPDATE `iot_event_record`
            SET risk_level = {normalize_color_case('risk_level')}
            WHERE risk_level IS NOT NULL
              AND TRIM(risk_level) <> ''
            """
        )

    for table in ("rule_definition", "iot_alarm_record", "iot_event_record"):
        if not table_exists(cur, db, table) or not column_exists(cur, db, table, "alarm_level"):
            continue
        cur.execute(
            f"""
            UPDATE `{table}`
            SET alarm_level = {normalize_color_case('alarm_level')}
            WHERE alarm_level IS NOT NULL
              AND TRIM(alarm_level) <> ''
            """
        )

    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "alarm_level"):
        cur.execute(
            f"""
            UPDATE `emergency_plan`
            SET alarm_level = {normalize_color_case('alarm_level')}
            WHERE alarm_level IS NOT NULL
              AND TRIM(alarm_level) <> ''
            """
        )
        if column_exists(cur, db, "emergency_plan", "risk_level"):
            cur.execute(
                f"""
                UPDATE `emergency_plan`
                SET alarm_level = {normalize_color_case('risk_level')}
                WHERE (alarm_level IS NULL OR TRIM(alarm_level) = '')
                  AND risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )
            cur.execute(
                f"""
                UPDATE `emergency_plan`
                SET risk_level = {normalize_color_case('risk_level')}
                WHERE risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )


def ensure_audit_defaults(cur: pymysql.cursors.Cursor) -> None:
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN log_type VARCHAR(16) NOT NULL DEFAULT 'manual'"
    )
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN operation_uri VARCHAR(255) NOT NULL DEFAULT ''"
    )
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN operation_method VARCHAR(255) NOT NULL DEFAULT ''"
    )


def ensure_legacy_phase4_defaults(cur: pymysql.cursors.Cursor, db: str) -> None:
    if table_exists(cur, db, "iot_command_record") and column_exists(cur, db, "iot_command_record", "message_id"):
        cur.execute("ALTER TABLE iot_command_record MODIFY COLUMN message_id VARCHAR(64) NULL DEFAULT NULL")

    if table_exists(cur, db, "iot_event_work_order") and column_exists(
        cur, db, "iot_event_work_order", "work_order_type"
    ):
        cur.execute(
            "ALTER TABLE iot_event_work_order MODIFY COLUMN work_order_type VARCHAR(32) NOT NULL DEFAULT 'event-dispatch'"
        )

    if table_exists(cur, db, "risk_point_device") and column_exists(cur, db, "risk_point_device", "id"):
        cur.execute("ALTER TABLE risk_point_device MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT")

    if table_exists(cur, db, "rule_definition") and column_exists(cur, db, "rule_definition", "rule_code"):
        cur.execute("ALTER TABLE rule_definition MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL")
    if table_exists(cur, db, "rule_definition") and column_exists(
        cur, db, "rule_definition", "condition_expression"
    ):
        cur.execute(
            "ALTER TABLE rule_definition MODIFY COLUMN condition_expression VARCHAR(255) NOT NULL DEFAULT ''"
        )

    if table_exists(cur, db, "linkage_rule") and column_exists(cur, db, "linkage_rule", "rule_code"):
        cur.execute("ALTER TABLE linkage_rule MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL")

    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "plan_code"):
        cur.execute("ALTER TABLE emergency_plan MODIFY COLUMN plan_code VARCHAR(64) NULL DEFAULT NULL")
    if table_exists(cur, db, "emergency_plan") and column_exists(
        cur, db, "emergency_plan", "applicable_scenario"
    ):
        cur.execute(
            "ALTER TABLE emergency_plan MODIFY COLUMN applicable_scenario VARCHAR(255) NOT NULL DEFAULT ''"
        )
    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "disposal_steps"):
        cur.execute("ALTER TABLE emergency_plan MODIFY COLUMN disposal_steps JSON NULL")


def ensure_menu_compat(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu"):
        return

    # 兼容历史 sys_menu 字段命名，保证权限菜单查询可落在统一实体字段上。
    if column_exists(cur, db, "sys_menu", "menu_code") and column_exists(cur, db, "sys_menu", "permission"):
        cur.execute(
            "UPDATE sys_menu SET menu_code = permission WHERE (menu_code IS NULL OR menu_code = '') AND permission IS NOT NULL AND permission <> ''"
        )
    if column_exists(cur, db, "sys_menu", "menu_code"):
        cur.execute("UPDATE sys_menu SET menu_code = CONCAT('menu-', id) WHERE menu_code IS NULL OR menu_code = ''")

    if column_exists(cur, db, "sys_menu", "path") and column_exists(cur, db, "sys_menu", "route_path"):
        cur.execute("UPDATE sys_menu SET path = route_path WHERE (path IS NULL OR path = '') AND route_path IS NOT NULL")

    if column_exists(cur, db, "sys_menu", "sort") and column_exists(cur, db, "sys_menu", "sort_no"):
        cur.execute("UPDATE sys_menu SET sort = sort_no WHERE sort IS NULL")

    if column_exists(cur, db, "sys_menu", "type") and column_exists(cur, db, "sys_menu", "menu_type"):
        cur.execute("UPDATE sys_menu SET type = menu_type WHERE type IS NULL")


def ensure_legacy_governance_write_permissions(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu") or not table_exists(cur, db, "sys_role_menu"):
        return
    if not column_exists(cur, db, "sys_menu", "menu_code"):
        return
    if not all(
        column_exists(cur, db, "sys_role_menu", column) for column in ("menu_id", "deleted", "update_by", "update_time")
    ):
        return
    if not all(column_exists(cur, db, "sys_menu", column) for column in ("id", "deleted", "update_by", "update_time")):
        return

    legacy_codes = "', '".join(
        (
            "risk:rule-definition:write",
            "risk:linkage-rule:write",
            "risk:emergency-plan:write",
        )
    )
    cur.execute(
        f"""
        UPDATE sys_role_menu rm
        INNER JOIN sys_menu m ON m.id = rm.menu_id
        SET rm.deleted = 1,
            rm.update_by = 1,
            rm.update_time = NOW()
        WHERE m.menu_code IN ('{legacy_codes}')
          AND rm.deleted = 0
        """
    )
    cur.execute(
        f"""
        UPDATE sys_menu
        SET deleted = 1,
            update_by = 1,
            update_time = NOW()
        WHERE menu_code IN ('{legacy_codes}')
          AND deleted = 0
        """
    )


def ensure_governance_fine_grained_permissions(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu") or not table_exists(cur, db, "sys_role") or not table_exists(cur, db, "sys_role_menu"):
        return

    required_menu_columns = (
        "id",
        "tenant_id",
        "parent_id",
        "menu_name",
        "menu_code",
        "path",
        "component",
        "icon",
        "meta_json",
        "sort",
        "type",
        "menu_type",
        "route_path",
        "permission",
        "sort_no",
        "visible",
        "status",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    required_role_columns = ("id", "tenant_id", "role_code", "deleted")
    required_role_menu_columns = ("id", "tenant_id", "role_id", "menu_id", "create_by", "create_time", "update_by", "update_time", "deleted")
    if not all(column_exists(cur, db, "sys_menu", column) for column in required_menu_columns):
        print("[skip] governance permission seeds: sys_menu columns missing")
        return
    if not all(column_exists(cur, db, "sys_role", column) for column in required_role_columns):
        print("[skip] governance permission seeds: sys_role columns missing")
        return
    if not all(column_exists(cur, db, "sys_role_menu", column) for column in required_role_menu_columns):
        print("[skip] governance permission seeds: sys_role_menu columns missing")
        return

    menu_seeds = (
        {
            "preferred_id": 93001029,
            "parent_id": 93001001,
            "menu_name": "规范库复核",
            "menu_code": "iot:normative-library:approve",
            "permission": "iot:normative-library:approve",
            "meta_json": '{"caption":"规范库关键写动作复核授权"}',
            "sort_no": 1129,
            "role_codes": ("MANAGEMENT_STAFF",),
        },
        {
            "preferred_id": 93002051,
            "parent_id": 93002003,
            "menu_name": "风险指标复核",
            "menu_code": "risk:metric-catalog:approve",
            "permission": "risk:metric-catalog:approve",
            "meta_json": '{"caption":"风险指标标注关键写动作双人复核"}',
            "sort_no": 3145,
            "role_codes": ("MANAGEMENT_STAFF", "OPS_STAFF"),
        },
    )

    for seed in menu_seeds:
        cur.execute(
            """
            SELECT id
            FROM sys_menu
            WHERE tenant_id = 1
              AND menu_code = %s
            ORDER BY deleted ASC, id ASC
            LIMIT 1
            """,
            (seed["menu_code"],),
        )
        existing_menu = cur.fetchone()
        menu_id = int(existing_menu[0]) if existing_menu else next_preferred_id(cur, "sys_menu", int(seed["preferred_id"]))
        if existing_menu:
            cur.execute(
                """
                UPDATE sys_menu
                SET parent_id = %s,
                    menu_name = %s,
                    path = '',
                    component = '',
                    icon = '',
                    meta_json = %s,
                    sort = %s,
                    type = 2,
                    menu_type = 2,
                    route_path = '',
                    permission = %s,
                    sort_no = %s,
                    visible = 1,
                    status = 1,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    int(seed["parent_id"]),
                    seed["menu_name"],
                    seed["meta_json"],
                    int(seed["sort_no"]),
                    seed["permission"],
                    int(seed["sort_no"]),
                    menu_id,
                ),
            )
        else:
            cur.execute(
                """
                INSERT INTO sys_menu (
                    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json,
                    sort, type, menu_type, route_path, permission, sort_no, visible, status,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (
                    %s, 1, %s, %s, %s, '', '', '', %s,
                    %s, 2, 2, '', %s, %s, 1, 1,
                    1, NOW(), 1, NOW(), 0
                )
                """,
                (
                    menu_id,
                    int(seed["parent_id"]),
                    seed["menu_name"],
                    seed["menu_code"],
                    seed["meta_json"],
                    int(seed["sort_no"]),
                    seed["permission"],
                    int(seed["sort_no"]),
                ),
            )

        for role_code in seed["role_codes"]:
            cur.execute(
                """
                SELECT id
                FROM sys_role
                WHERE tenant_id = 1
                  AND role_code = %s
                  AND deleted = 0
                ORDER BY id ASC
                LIMIT 1
                """,
                (role_code,),
            )
            role_row = cur.fetchone()
            if role_row is None:
                continue
            role_id = int(role_row[0])
            cur.execute(
                """
                SELECT id
                FROM sys_role_menu
                WHERE tenant_id = 1
                  AND role_id = %s
                  AND menu_id = %s
                ORDER BY deleted ASC, id ASC
                LIMIT 1
                """,
                (role_id, menu_id),
            )
            existing_binding = cur.fetchone()
            if existing_binding:
                cur.execute(
                    """
                    UPDATE sys_role_menu
                    SET deleted = 0,
                        update_by = 1,
                        update_time = NOW()
                    WHERE id = %s
                    """,
                    (int(existing_binding[0]),),
                )
            else:
                binding_id = next_table_id(cur, "sys_role_menu")
                cur.execute(
                    """
                    INSERT INTO sys_role_menu (
                        id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted
                    ) VALUES (%s, 1, %s, %s, 1, NOW(), 1, NOW(), 0)
                    """,
                    (binding_id, role_id, menu_id),
                )


def ensure_governance_approval_policy_defaults(cur: pymysql.cursors.Cursor, db: str) -> None:
    required_tables = (
        "sys_user",
        "sys_role",
        "sys_user_role",
        "sys_governance_approval_policy",
    )
    if any(not table_exists(cur, db, table) for table in required_tables):
        print("[skip] governance approval policy seeds: required tables missing")
        return

    required_user_columns = (
        "id",
        "tenant_id",
        "username",
        "password",
        "nickname",
        "real_name",
        "phone",
        "email",
        "status",
        "is_admin",
        "remark",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    required_role_columns = ("id", "tenant_id", "role_code", "deleted")
    required_user_role_columns = (
        "id",
        "tenant_id",
        "user_id",
        "role_id",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    required_policy_columns = (
        "id",
        "tenant_id",
        "scope_type",
        "action_code",
        "approver_mode",
        "approver_user_id",
        "enabled",
        "remark",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    if not all(column_exists(cur, db, "sys_user", column) for column in required_user_columns):
        print("[skip] governance approval policy seeds: sys_user columns missing")
        return
    if not all(column_exists(cur, db, "sys_role", column) for column in required_role_columns):
        print("[skip] governance approval policy seeds: sys_role columns missing")
        return
    if not all(column_exists(cur, db, "sys_user_role", column) for column in required_user_role_columns):
        print("[skip] governance approval policy seeds: sys_user_role columns missing")
        return
    if not all(column_exists(cur, db, "sys_governance_approval_policy", column) for column in required_policy_columns):
        print("[skip] governance approval policy seeds: sys_governance_approval_policy columns missing")
        return

    reviewer_org_id = None
    if table_exists(cur, db, "sys_organization") and all(
        column_exists(cur, db, "sys_organization", column) for column in ("id", "tenant_id", "deleted")
    ):
        cur.execute(
            """
            SELECT id
            FROM sys_organization
            WHERE tenant_id = 1
              AND deleted = 0
            ORDER BY id ASC
            LIMIT 1
            """
        )
        org_row = cur.fetchone()
        reviewer_org_id = int(org_row[0]) if org_row else None

    cur.execute(
        """
        SELECT id
        FROM sys_user
        WHERE tenant_id = 1
          AND username = %s
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """,
        (GOVERNANCE_REVIEWER_USERNAME,),
    )
    existing_user = cur.fetchone()
    reviewer_user_id = (
        int(existing_user[0])
        if existing_user
        else next_preferred_id(cur, "sys_user", GOVERNANCE_REVIEWER_PREFERRED_ID)
    )
    if existing_user:
        if reviewer_org_id is not None:
            cur.execute(
                """
                UPDATE sys_user
                SET org_id = %s,
                    nickname = %s,
                    real_name = %s,
                    phone = %s,
                    email = %s,
                    status = 1,
                    is_admin = 1,
                    remark = %s,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    reviewer_org_id,
                    GOVERNANCE_REVIEWER_NICKNAME,
                    GOVERNANCE_REVIEWER_REAL_NAME,
                    GOVERNANCE_REVIEWER_PHONE,
                    GOVERNANCE_REVIEWER_EMAIL,
                    GOVERNANCE_REVIEWER_REMARK,
                    reviewer_user_id,
                ),
            )
        else:
            cur.execute(
                """
                UPDATE sys_user
                SET nickname = %s,
                    real_name = %s,
                    phone = %s,
                    email = %s,
                    status = 1,
                    is_admin = 1,
                    remark = %s,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    GOVERNANCE_REVIEWER_NICKNAME,
                    GOVERNANCE_REVIEWER_REAL_NAME,
                    GOVERNANCE_REVIEWER_PHONE,
                    GOVERNANCE_REVIEWER_EMAIL,
                    GOVERNANCE_REVIEWER_REMARK,
                    reviewer_user_id,
                ),
            )
    else:
        cur.execute(
            """
            INSERT INTO sys_user (
                id, tenant_id, org_id, username, password, nickname, real_name, phone, email,
                status, is_admin, remark, create_by, create_time, update_by, update_time, deleted
            ) VALUES (
                %s, 1, %s, %s, %s, %s, %s, %s, %s,
                1, 1, %s, 1, NOW(), 1, NOW(), 0
            )
            """,
            (
                reviewer_user_id,
                reviewer_org_id,
                GOVERNANCE_REVIEWER_USERNAME,
                GOVERNANCE_REVIEWER_PASSWORD_HASH,
                GOVERNANCE_REVIEWER_NICKNAME,
                GOVERNANCE_REVIEWER_REAL_NAME,
                GOVERNANCE_REVIEWER_PHONE,
                GOVERNANCE_REVIEWER_EMAIL,
                GOVERNANCE_REVIEWER_REMARK,
            ),
        )

    cur.execute(
        """
        SELECT id
        FROM sys_role
        WHERE tenant_id = 1
          AND role_code = %s
          AND deleted = 0
        ORDER BY id ASC
        LIMIT 1
        """,
        (GOVERNANCE_REVIEWER_ROLE_CODE,),
    )
    role_row = cur.fetchone()
    if role_row is None:
        print("[skip] governance approval policy seeds: SUPER_ADMIN role missing")
        return
    reviewer_role_id = int(role_row[0])

    cur.execute(
        """
        SELECT id
        FROM sys_user_role
        WHERE tenant_id = 1
          AND user_id = %s
          AND role_id = %s
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """,
        (reviewer_user_id, reviewer_role_id),
    )
    existing_binding = cur.fetchone()
    if existing_binding:
        cur.execute(
            """
            UPDATE sys_user_role
            SET deleted = 0,
                update_by = 1,
                update_time = NOW()
            WHERE id = %s
            """,
            (int(existing_binding[0]),),
        )
    else:
        binding_id = next_table_id(cur, "sys_user_role")
        cur.execute(
            """
            INSERT INTO sys_user_role (
                id, tenant_id, user_id, role_id, create_by, create_time, update_by, update_time, deleted
            ) VALUES (%s, 1, %s, %s, 1, NOW(), 1, NOW(), 0)
            """,
            (binding_id, reviewer_user_id, reviewer_role_id),
        )

    for seed in GOVERNANCE_APPROVAL_POLICY_SEEDS:
        cur.execute(
            """
            SELECT id
            FROM sys_governance_approval_policy
            WHERE scope_type = %s
              AND tenant_id = %s
              AND action_code = %s
            ORDER BY deleted ASC, id ASC
            LIMIT 1
            """,
            (seed["scope_type"], int(seed["tenant_id"]), seed["action_code"]),
        )
        existing_policy = cur.fetchone()
        policy_id = (
            int(existing_policy[0])
            if existing_policy
            else next_preferred_id(cur, "sys_governance_approval_policy", int(seed["preferred_id"]))
        )
        if existing_policy:
            cur.execute(
                """
                UPDATE sys_governance_approval_policy
                SET approver_mode = %s,
                    approver_user_id = %s,
                    enabled = 1,
                    remark = %s,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    seed["approver_mode"],
                    reviewer_user_id,
                    seed["remark"],
                    policy_id,
                ),
            )
        else:
            cur.execute(
                """
                INSERT INTO sys_governance_approval_policy (
                    id, tenant_id, scope_type, action_code, approver_mode, approver_user_id, enabled, remark,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, 1, %s,
                    1, NOW(), 1, NOW(), 0
                )
                """,
                (
                    policy_id,
                    int(seed["tenant_id"]),
                    seed["scope_type"],
                    seed["action_code"],
                    seed["approver_mode"],
                    reviewer_user_id,
                    seed["remark"],
                ),
            )


def ensure_indexes(cur: pymysql.cursors.Cursor, db: str) -> None:
    for table, specs in INDEXES_TO_ADD.items():
        if not table_exists(cur, db, table):
            print(f"[skip] table missing for indexes: {table}")
            continue
        for index_name, ddl in specs:
            if index_exists(cur, db, table, index_name):
                expected_shape = BINDING_INDEX_EXPECTED_SHAPES.get((table, index_name))
                if expected_shape and binding_index_shape_drifts(cur, db, table, index_name, expected_shape):
                    raise RuntimeError(
                        f"Existing index {table}.{index_name} drifts from expected shape and must be "
                        "corrected before schema sync can continue."
                    )
                ensure_existing_index_shape(cur, db, table, index_name)
                continue
            unique_columns = UNIQUE_INDEX_DUPLICATE_GUARDS.get((table, index_name))
            if unique_columns and has_duplicate_unique_key_rows(cur, table, unique_columns):
                raise RuntimeError(
                    f"Cannot add unique index {table}.{index_name}: duplicate rows detected; "
                    "duplicate rows must be cleaned before schema sync can continue."
                )
            cur.execute(ddl)
            print(f"[index] {table}.{index_name} added")


def has_duplicate_unique_key_rows(
    cur: pymysql.cursors.Cursor, table: str, unique_columns: Tuple[str, ...]
) -> bool:
    group_columns = ", ".join(f"`{column}`" for column in unique_columns)
    cur.execute(
        f"""
        SELECT 1
        FROM `{table}`
        GROUP BY {group_columns}
        HAVING COUNT(1) > 1
        LIMIT 1
        """
    )
    return cur.fetchone() is not None


def binding_index_shape_drifts(
    cur: pymysql.cursors.Cursor,
    db: str,
    table: str,
    index: str,
    expected_shape: Tuple[bool, Tuple[str, ...]],
) -> bool:
    existing_shape = load_index_shape(cur, db, table, index)
    if existing_shape is None:
        return True
    return existing_shape != expected_shape


def load_index_shape(
    cur: pymysql.cursors.Cursor, db: str, table: str, index: str
) -> Tuple[bool, Tuple[str, ...]] | None:
    cur.execute(
        """
        SELECT NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        ORDER BY SEQ_IN_INDEX ASC
        """,
        (db, table, index),
    )
    rows = cur.fetchall()
    if not rows:
        return None
    is_unique = int(rows[0][0]) == 0
    columns = tuple(str(row[1]) for row in rows)
    return is_unique, columns


def find_multi_risk_point_conflicts(cur: pymysql.cursors.Cursor, db: str) -> List[Tuple[int, str, str, str]]:
    required_tables = ("iot_device", "risk_point_device", "risk_point")
    if any(not table_exists(cur, db, table) for table in required_tables):
        return []

    cur.execute(
        """
        SELECT
            d.id,
            d.device_code,
            GROUP_CONCAT(DISTINCT CAST(rp.id AS CHAR) ORDER BY rp.id SEPARATOR ',') AS risk_point_ids,
            GROUP_CONCAT(DISTINCT COALESCE(rp.risk_point_name, CONCAT('risk-point-', rp.id)) ORDER BY rp.id SEPARATOR ' / ') AS risk_point_names
        FROM iot_device d
        INNER JOIN risk_point_device rpd
            ON rpd.deleted = 0
           AND (
               (rpd.device_id IS NOT NULL AND rpd.device_id = d.id)
               OR (rpd.device_id IS NULL AND rpd.device_code IS NOT NULL AND rpd.device_code = d.device_code)
           )
        INNER JOIN risk_point rp
            ON rp.id = rpd.risk_point_id
           AND rp.deleted = 0
        WHERE d.deleted = 0
        GROUP BY d.id, d.device_code
        HAVING COUNT(DISTINCT rp.id) > 1
        ORDER BY d.id
        """
    )
    return [
        (int(device_id), str(device_code), str(risk_point_ids), str(risk_point_names))
        for device_id, device_code, risk_point_ids, risk_point_names in cur.fetchall()
    ]


def ensure_device_org_backfill(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "iot_device"):
        print("[skip] table missing: iot_device")
        return
    if not column_exists(cur, db, "iot_device", "org_id") or not column_exists(cur, db, "iot_device", "org_name"):
        print("[skip] iot_device org columns missing")
        return

    conflicts = find_multi_risk_point_conflicts(cur, db)
    if conflicts:
        print("[error] detected active multi-risk-point bindings. aborting device org backfill.")
        for device_id, device_code, risk_point_ids, risk_point_names in conflicts:
            print(
                f"  - device_id={device_id}, device_code={device_code}, risk_point_ids={risk_point_ids}, risk_point_names={risk_point_names}"
            )
        raise RuntimeError("device org backfill blocked by multi-risk-point bindings")

    cur.execute(
        """
        UPDATE iot_device d
        INNER JOIN (
            SELECT
                d_ref.id AS device_id,
                MAX(rp.org_id) AS org_id,
                MAX(rp.org_name) AS org_name
            FROM iot_device d_ref
            INNER JOIN risk_point_device rpd
                ON rpd.deleted = 0
               AND (
                   (rpd.device_id IS NOT NULL AND rpd.device_id = d_ref.id)
                   OR (rpd.device_id IS NULL AND rpd.device_code IS NOT NULL AND rpd.device_code = d_ref.device_code)
               )
            INNER JOIN risk_point rp
                ON rp.id = rpd.risk_point_id
               AND rp.deleted = 0
            WHERE d_ref.deleted = 0
              AND rp.org_id IS NOT NULL
              AND rp.org_name IS NOT NULL
            GROUP BY d_ref.id
        ) src
            ON src.device_id = d.id
        SET d.org_id = src.org_id,
            d.org_name = src.org_name
        WHERE d.deleted = 0
          AND d.org_id IS NULL
        """
    )
    print(f"[backfill] iot_device org fields updated from risk points: {cur.rowcount}")


def ensure_collector_child_dev_baseline(cur: pymysql.cursors.Cursor, db: str) -> None:
    required_tables = (
        "iot_product",
        "iot_product_model",
        "iot_normative_metric_definition",
        "iot_vendor_metric_mapping_rule",
        "iot_device",
        "iot_device_relation",
        "iot_device_property",
    )
    for table in required_tables:
        if not table_exists(cur, db, table):
            print(f"[skip] collector-child baseline: table missing {table}")
            return

    for seed in COLLECTOR_CHILD_BASELINE_PRODUCTS:
        cur.execute(
            """
            INSERT INTO iot_product (
                id, tenant_id, product_key, product_name, protocol_code, node_type, data_format,
                manufacturer, description, status, remark, create_by, create_time, update_by, update_time, deleted
            ) VALUES (%s, 1, %s, %s, 'mqtt-json', 1, 'JSON', %s, %s, 1, %s, 1, NOW(), 1, NOW(), 0)
            ON DUPLICATE KEY UPDATE
                product_name = VALUES(product_name),
                protocol_code = VALUES(protocol_code),
                node_type = VALUES(node_type),
                data_format = VALUES(data_format),
                manufacturer = VALUES(manufacturer),
                description = VALUES(description),
                status = VALUES(status),
                remark = VALUES(remark),
                update_by = 1,
                update_time = NOW(),
                deleted = 0
            """,
            (
                int(seed["id"]),
                str(seed["product_key"]),
                str(seed["product_name"]),
                str(seed["manufacturer"]),
                str(seed["description"]),
                str(seed["remark"]),
            ),
        )

    if column_exists(cur, db, "iot_product", "metadata_json"):
        for product_key, object_insight in COLLECTOR_CHILD_BASELINE_METADATA.items():
            cur.execute(
                """
                UPDATE iot_product
                SET metadata_json = JSON_SET(COALESCE(metadata_json, JSON_OBJECT()), '$.objectInsight', CAST(%s AS JSON)),
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE tenant_id = 1
                  AND product_key = %s
                """,
                (json.dumps(object_insight, ensure_ascii=False), product_key),
            )

    for seed in COLLECTOR_CHILD_BASELINE_PRODUCT_MODELS:
        cur.execute(
            """
            INSERT INTO iot_product_model (
                id, tenant_id, product_id, model_type, identifier, model_name, data_type, specs_json,
                sort_no, required_flag, description, create_time, update_time, deleted
            ) VALUES (%s, 1, %s, 'property', %s, %s, %s, CAST(%s AS JSON), %s, 0, %s, NOW(), NOW(), 0)
            ON DUPLICATE KEY UPDATE
                model_name = VALUES(model_name),
                data_type = VALUES(data_type),
                specs_json = VALUES(specs_json),
                sort_no = VALUES(sort_no),
                required_flag = VALUES(required_flag),
                description = VALUES(description),
                update_time = NOW(),
                deleted = 0
            """,
            (
                int(seed["id"]),
                int(seed["product_id"]),
                str(seed["identifier"]),
                str(seed["model_name"]),
                str(seed["data_type"]),
                json.dumps(seed["specs_json"], ensure_ascii=False),
                int(seed["sort_no"]),
                str(seed["description"]),
            ),
        )

    for seed in COLLECTOR_CHILD_BASELINE_NORMATIVE_METRICS:
        cur.execute(
            """
            INSERT INTO iot_normative_metric_definition (
                id, tenant_id, scenario_code, device_family, identifier, display_name, unit,
                precision_digits, monitor_content_code, monitor_type_code, risk_enabled, trend_enabled,
                metric_dimension, threshold_type, semantic_direction, gis_enabled, insight_enabled, analytics_enabled,
                status, version_no, metadata_json
            ) VALUES (
                %s, 1, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s,
                %s, %s, CAST(%s AS JSON)
            )
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                unit = VALUES(unit),
                precision_digits = VALUES(precision_digits),
                monitor_content_code = VALUES(monitor_content_code),
                monitor_type_code = VALUES(monitor_type_code),
                risk_enabled = VALUES(risk_enabled),
                trend_enabled = VALUES(trend_enabled),
                metric_dimension = VALUES(metric_dimension),
                threshold_type = VALUES(threshold_type),
                semantic_direction = VALUES(semantic_direction),
                gis_enabled = VALUES(gis_enabled),
                insight_enabled = VALUES(insight_enabled),
                analytics_enabled = VALUES(analytics_enabled),
                status = VALUES(status),
                version_no = VALUES(version_no),
                metadata_json = VALUES(metadata_json),
                deleted = 0
            """,
            (
                int(seed["id"]),
                str(seed["scenario_code"]),
                str(seed["device_family"]),
                str(seed["identifier"]),
                str(seed["display_name"]),
                seed["unit"],
                int(seed["precision_digits"]),
                str(seed["monitor_content_code"]),
                str(seed["monitor_type_code"]),
                int(seed["risk_enabled"]),
                int(seed["trend_enabled"]),
                str(seed["metric_dimension"]),
                str(seed["threshold_type"]),
                str(seed["semantic_direction"]),
                int(seed["gis_enabled"]),
                int(seed["insight_enabled"]),
                int(seed["analytics_enabled"]),
                str(seed["status"]),
                int(seed["version_no"]),
                json.dumps(seed["metadata_json"], ensure_ascii=False),
            ),
        )

    for seed in COLLECTOR_CHILD_BASELINE_MAPPING_RULES:
        cur.execute(
            """
            INSERT INTO iot_vendor_metric_mapping_rule (
                id, tenant_id, scope_type, product_id, protocol_code, scenario_code, device_family,
                raw_identifier, logical_channel_code, relation_condition_json, normalization_rule_json,
                target_normative_identifier, status, version_no, approval_order_id,
                create_by, create_time, update_by, update_time, deleted
            ) VALUES (
                %s, 1, 'PRODUCT', %s, %s, %s, %s,
                %s, %s, NULL, NULL,
                %s, 'ACTIVE', 1, NULL,
                1, NOW(), 1, NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                product_id = VALUES(product_id),
                protocol_code = VALUES(protocol_code),
                scenario_code = VALUES(scenario_code),
                device_family = VALUES(device_family),
                raw_identifier = VALUES(raw_identifier),
                logical_channel_code = VALUES(logical_channel_code),
                relation_condition_json = VALUES(relation_condition_json),
                normalization_rule_json = VALUES(normalization_rule_json),
                target_normative_identifier = VALUES(target_normative_identifier),
                status = VALUES(status),
                version_no = VALUES(version_no),
                approval_order_id = VALUES(approval_order_id),
                update_by = 1,
                update_time = NOW(),
                deleted = 0
            """,
            (
                int(seed["id"]),
                int(seed["product_id"]),
                str(seed["protocol_code"]),
                str(seed["scenario_code"]),
                str(seed["device_family"]),
                str(seed["raw_identifier"]),
                str(seed["logical_channel_code"]),
                str(seed["target_normative_identifier"]),
            ),
        )

    for device_id, product_id, device_code, device_name, address, seed_family in collector_child_device_seeds():
        cur.execute(
            """
            INSERT INTO iot_device (
                id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id,
                username, password, protocol_code, node_type, online_status, activate_status, device_status,
                firmware_version, ip_address, last_online_time, last_report_time, longitude, latitude, address,
                metadata_json, remark, create_by, create_time, update_by, update_time, deleted
            ) VALUES (
                %s, 1, 7101, '平台运维中心', %s, %s, %s, '123456', %s, %s, '123456',
                'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE),
                DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, %s, CAST(%s AS JSON),
                'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                org_id = VALUES(org_id),
                org_name = VALUES(org_name),
                product_id = VALUES(product_id),
                device_name = VALUES(device_name),
                protocol_code = VALUES(protocol_code),
                online_status = VALUES(online_status),
                activate_status = VALUES(activate_status),
                device_status = VALUES(device_status),
                firmware_version = VALUES(firmware_version),
                ip_address = VALUES(ip_address),
                last_online_time = VALUES(last_online_time),
                last_report_time = VALUES(last_report_time),
                longitude = VALUES(longitude),
                latitude = VALUES(latitude),
                address = VALUES(address),
                metadata_json = VALUES(metadata_json),
                remark = VALUES(remark),
                update_by = 1,
                update_time = NOW(),
                deleted = 0
            """,
            (
                int(device_id),
                int(product_id),
                device_name,
                device_code,
                device_code,
                device_code,
                address,
                json.dumps({"seedFamily": seed_family}, ensure_ascii=False),
            ),
        )

    for (
        relation_id,
        parent_device_code,
        logical_code,
        child_device_code,
        child_product_id,
        child_product_key,
        canonicalization_strategy,
        status_mirror_strategy,
    ) in collector_child_relation_seeds():
        cur.execute(
            """
            INSERT INTO iot_device_relation (
                id, tenant_id, parent_device_id, parent_device_code, logical_channel_code, child_device_id,
                child_device_code, child_product_id, child_product_key, relation_type,
                canonicalization_strategy, status_mirror_strategy, enabled, remark, create_by, create_time,
                update_by, update_time, deleted
            )
            SELECT
                %s, 1, parent_device.id, %s, %s, child_device.id,
                %s, %s, %s, 'collector_child', %s, %s,
                1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0
            FROM iot_device parent_device
            INNER JOIN iot_device child_device
                ON child_device.tenant_id = parent_device.tenant_id
               AND child_device.device_code = %s
               AND child_device.deleted = 0
            WHERE parent_device.tenant_id = 1
              AND parent_device.device_code = %s
              AND parent_device.deleted = 0
            ON DUPLICATE KEY UPDATE
                parent_device_id = VALUES(parent_device_id),
                parent_device_code = VALUES(parent_device_code),
                child_device_id = VALUES(child_device_id),
                child_device_code = VALUES(child_device_code),
                child_product_id = VALUES(child_product_id),
                child_product_key = VALUES(child_product_key),
                relation_type = VALUES(relation_type),
                canonicalization_strategy = VALUES(canonicalization_strategy),
                status_mirror_strategy = VALUES(status_mirror_strategy),
                enabled = VALUES(enabled),
                remark = VALUES(remark),
                update_by = 1,
                update_time = NOW(),
                deleted = 0
            """,
            (
                int(relation_id),
                parent_device_code,
                logical_code,
                child_device_code,
                int(child_product_id),
                child_product_key,
                canonicalization_strategy,
                status_mirror_strategy,
                child_device_code,
                parent_device_code,
            ),
        )

    for property_id, device_code, identifier, property_name, property_value, value_type in collector_child_property_seeds():
        cur.execute(
            """
            INSERT INTO iot_device_property (
                id, tenant_id, device_id, identifier, property_name, property_value, value_type, report_time, create_time, update_time
            )
            SELECT COALESCE(existing_property.id, %s), 1, device.id, %s, %s, %s, %s, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()
            FROM iot_device device
            LEFT JOIN iot_device_property existing_property
                ON existing_property.device_id = device.id
               AND existing_property.identifier = %s
            WHERE device.tenant_id = 1
              AND device.device_code = %s
              AND device.deleted = 0
            ON DUPLICATE KEY UPDATE
                device_id = VALUES(device_id),
                identifier = VALUES(identifier),
                property_name = VALUES(property_name),
                property_value = VALUES(property_value),
                value_type = VALUES(value_type),
                report_time = VALUES(report_time),
                update_time = NOW()
            """,
            (int(property_id), identifier, property_name, property_value, value_type, identifier, device_code),
        )


def main() -> int:
    args = parse_args()

    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        charset="utf8mb4",
        autocommit=True,
        connect_timeout=20,
        read_timeout=60,
        write_timeout=60,
    )

    try:
        try:
            with conn.cursor() as cur:
                cur.execute(f"USE `{args.db}`")

                for table, ddl in CREATE_TABLE_SQL.items():
                    cur.execute(ddl)
                    print(f"[table] ensured {table}")

                for table, specs in COLUMNS_TO_ADD.items():
                    if not table_exists(cur, args.db, table):
                        print(f"[skip] table missing: {table}")
                        continue
                    for column, definition in specs:
                        if column_exists(cur, args.db, table, column):
                            continue
                        cur.execute(f"ALTER TABLE `{table}` ADD COLUMN `{column}` {definition}")
                        print(f"[column] {table}.{column} added")

                if table_exists(cur, args.db, "sys_dict"):
                    if column_exists(cur, args.db, "sys_dict", "dict_value") and column_exists(
                        cur, args.db, "sys_dict", "dict_label"
                    ):
                        ensure_dict_defaults(cur)
                        print("[dict] sys_dict default constraints aligned")
                    ensure_level_dicts(cur, args.db)
                    print("[dict] risk_point_level / alarm_level / risk_level items aligned")
                    ensure_system_governance_dicts(cur, args.db)
                    print("[dict] help_doc_category / notification_channel_type items aligned")

                if table_exists(cur, args.db, "sys_audit_log"):
                    ensure_audit_defaults(cur)
                    print("[audit] sys_audit_log legacy constraints aligned")

                ensure_legacy_phase4_defaults(cur, args.db)
                print("[phase4] legacy strict columns aligned")

                migrate_level_values(cur, args.db)
                print("[phase4] risk point and alarm semantics values migrated")

                ensure_device_org_backfill(cur, args.db)

                ensure_menu_compat(cur, args.db)
                print("[menu] sys_menu legacy columns aligned")
                ensure_legacy_governance_write_permissions(cur, args.db)
                print("[menu] legacy governance write permissions cleaned")
                ensure_governance_fine_grained_permissions(cur, args.db)
                print("[menu] governance fine-grained permission seeds aligned")
                ensure_governance_approval_policy_defaults(cur, args.db)
                print("[governance] fixed reviewer and approval policy seeds aligned")
                ensure_collector_child_dev_baseline(cur, args.db)
                print("[seed] collector-child shared dev baseline aligned")
                ensure_device_relation_integrity(cur, args.db)
                print("[repair] device relation duplicates and stale device references aligned")

                ensure_indexes(cur, args.db)

                for view_name, ddl in VIEW_SQL.items():
                    cur.execute(ddl)
                    print(f"[view] ensured {view_name}")

            print("Schema sync completed.")
            return 0
        except Exception as exc:
            print(f"[fatal] {exc}")
            return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
