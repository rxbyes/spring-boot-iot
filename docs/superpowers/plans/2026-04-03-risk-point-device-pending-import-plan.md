# Risk Point Device Pending Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a safe staging path for importing Excel risk-point-to-device relationships when `metric_identifier` is still unknown, so the data can be landed, reviewed, and promoted later without polluting `risk_point_device`.

**Architecture:** Keep `risk_point_device` as the formal runtime binding table. Add a new pending-governance table in MySQL, a repository-local Python 3 script that parses the Excel file and generates staging inserts by matching `risk_point_name` and `device_code` against the real environment, and document the new workflow in the existing risk and database docs.

**Tech Stack:** MySQL DDL in `sql/init.sql`, schema sync in `scripts/run-real-env-schema-sync.py`, Python 3 standard library plus existing `pymysql`, Markdown docs under `docs/`.

---

### Task 1: Lock the Script Contract with Tests

**Files:**
- Create: `scripts/test_export_risk_point_device_pending_bindings.py`
- Create: `scripts/export-risk-point-device-pending-bindings.py`

- [ ] **Step 1: Write the failing test for Excel parsing**

```python
class ParseWorkbookTest(unittest.TestCase):
    def test_reads_header_and_rows_from_ooxml_workbook(self) -> None:
        workbook = create_workbook([
            ("风险点名称", "设备编号"),
            ("北坡风险点", "DEV-001"),
            ("南坡风险点", "DEV-002"),
        ])
        rows = module.read_excel_rows(workbook)
        self.assertEqual(
            [("北坡风险点", "DEV-001", 2), ("南坡风险点", "DEV-002", 3)],
            [(row.risk_point_name, row.device_code, row.source_row_no) for row in rows],
        )
```

- [ ] **Step 2: Run the parsing test and verify it fails**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: FAIL because `export-risk-point-device-pending-bindings.py` and `read_excel_rows` do not exist yet.

- [ ] **Step 3: Write the failing test for status resolution and SQL rendering**

```python
def test_render_insert_sql_marks_missing_metric_governance_and_unresolved_rows(self) -> None:
    rows = [
        module.ExcelBindingRow("北坡风险点", "DEV-001", 2),
        module.ExcelBindingRow("缺失风险点", "DEV-404", 3),
    ]
    risk_points = {"北坡风险点": [{"id": 11, "risk_point_code": "RP-011"}]}
    devices = {"DEV-001": {"id": 21, "device_name": "北坡位移仪"}}
    records = module.resolve_pending_rows(
        rows=rows,
        risk_points_by_name=risk_points,
        devices_by_code=devices,
        tenant_id=1,
        batch_no="batch-001",
        source_file_name="设备信息.xls",
        operator_id=1,
    )
    sql = module.render_insert_sql(records)
    self.assertIn("PENDING_METRIC_GOVERNANCE", sql)
    self.assertIn("RISK_POINT_NOT_FOUND;DEVICE_NOT_FOUND", sql)
```

- [ ] **Step 4: Run the rendering test and verify it fails**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: FAIL because `resolve_pending_rows` and `render_insert_sql` are still missing.

- [ ] **Step 5: Implement the minimal script APIs needed by the tests**

```python
@dataclass(frozen=True)
class ExcelBindingRow:
    risk_point_name: str
    device_code: str
    source_row_no: int


@dataclass(frozen=True)
class PendingBindingRecord:
    batch_no: str
    source_file_name: str
    source_row_no: int
    risk_point_name: str
    risk_point_id: int | None
    risk_point_code: str | None
    device_code: str
    device_id: int | None
    device_name: str | None
    resolution_status: str
    resolution_note: str | None
    tenant_id: int
    create_by: int | None
```

- [ ] **Step 6: Run the tests and verify they pass**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: PASS

### Task 2: Add Real Environment Resolution and CLI Output

**Files:**
- Modify: `scripts/export-risk-point-device-pending-bindings.py`
- Modify: `scripts/test_export_risk_point_device_pending_bindings.py`

- [ ] **Step 1: Write the failing test for CLI SQL export**

```python
def test_cli_writes_sql_file_with_summary(self) -> None:
    result = subprocess.run(
        [
            "python3",
            str(SCRIPT_PATH),
            "--excel",
            str(self.workbook_path),
            "--sql-output",
            str(self.output_path),
            "--batch-no",
            "batch-002",
            "--skip-db-lookup",
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    self.assertEqual(0, result.returncode)
    self.assertIn("rows=2", result.stdout)
    self.assertTrue(self.output_path.exists())
```

- [ ] **Step 2: Run the CLI test and verify it fails**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: FAIL because the CLI and `--skip-db-lookup` mode are not implemented yet.

- [ ] **Step 3: Implement CLI argument parsing, app-dev defaults, DB lookup, and skip-db mode**

```python
def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate pending risk-point-device governance SQL from an Excel workbook."
    )
    parser.add_argument("--excel", required=True, help="Path to the OOXML workbook.")
    parser.add_argument("--sql-output", help="Write generated SQL to this file.")
    parser.add_argument("--batch-no", help="Explicit import batch number.")
    parser.add_argument("--tenant-id", type=int, default=1)
    parser.add_argument("--operator-id", type=int, default=1)
    parser.add_argument("--skip-db-lookup", action="store_true")
    parser.add_argument("--jdbc-url")
    parser.add_argument("--user")
    parser.add_argument("--password")
    return parser.parse_args()
```

- [ ] **Step 4: Re-run the script tests**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: PASS

### Task 3: Add the Pending Governance Table to Schema Baselines

**Files:**
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`

- [ ] **Step 1: Write the failing schema test**

```python
def test_sql_baseline_contains_pending_binding_table(self) -> None:
    sql_text = Path("sql/init.sql").read_text(encoding="utf-8")
    self.assertIn("CREATE TABLE risk_point_device_pending_binding", sql_text)
```

- [ ] **Step 2: Run the schema test and verify it fails**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: FAIL because the new table is not in `sql/init.sql` yet.

- [ ] **Step 3: Add the table to `sql/init.sql` and the schema sync map**

```sql
CREATE TABLE risk_point_device_pending_binding (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no VARCHAR(64) NOT NULL COMMENT '导入批次号',
    source_file_name VARCHAR(255) DEFAULT NULL COMMENT '来源文件名',
    source_row_no INT NOT NULL COMMENT '来源行号',
    risk_point_name VARCHAR(128) NOT NULL COMMENT '来源风险点名称',
    risk_point_id BIGINT DEFAULT NULL COMMENT '匹配到的风险点ID',
    risk_point_code VARCHAR(64) DEFAULT NULL COMMENT '匹配到的风险点编号',
    device_code VARCHAR(64) NOT NULL COMMENT '来源设备编码',
    device_id BIGINT DEFAULT NULL COMMENT '匹配到的设备ID',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '匹配到的设备名称',
    resolution_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE' COMMENT '治理状态',
    resolution_note VARCHAR(500) DEFAULT NULL COMMENT '治理说明',
    metric_identifier VARCHAR(64) DEFAULT NULL COMMENT '后续补录测点标识',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '后续补录测点名称',
    promoted_binding_id BIGINT DEFAULT NULL COMMENT '转正后的正式绑定ID',
    promoted_time DATETIME DEFAULT NULL COMMENT '转正时间',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no),
    KEY idx_pending_binding_status (tenant_id, resolution_status, deleted),
    KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted),
    KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备待治理导入表';
```

- [ ] **Step 4: Re-run the script tests**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: PASS

### Task 4: Update Existing Documentation in Place

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Add the governance workflow note to the business doc**

```markdown
- 当设备与风险点关系已明确、但 `metric_identifier / metric_name` 仍需等待真实上报治理时，先落 `risk_point_device_pending_binding`；只有测点确认后才转正写入 `risk_point_device`。
```

- [ ] **Step 2: Add the new table dictionary and import workflow to the database doc**

```markdown
##### `risk_point_device_pending_binding`

- 作用：在缺少正式测点标识时，先承接 Excel 导入的“风险点名称 + 设备编号”待治理关系。
- 导入方式：通过 `python3 scripts/export-risk-point-device-pending-bindings.py --excel <path> --sql-output <path>` 生成 SQL，再在真实环境执行。
```

- [ ] **Step 3: Add the change log entry**

```markdown
- 2026-04-03：新增 `risk_point_device_pending_binding` 与 `scripts/export-risk-point-device-pending-bindings.py`，用于把缺少测点标识的风险点设备关系先落待治理表，避免把占位测点写入正式 `risk_point_device`。
```

- [ ] **Step 4: Run focused verification**

Run: `python3 -m unittest scripts/test_export_risk_point_device_pending_bindings.py -v`
Expected: PASS

Run: `python3 scripts/export-risk-point-device-pending-bindings.py --excel /Users/rxbyes/Downloads/设备信息.xls --sql-output /tmp/risk-point-device-pending.sql --skip-db-lookup`
Expected: exits `0`, prints summary, writes SQL file.
