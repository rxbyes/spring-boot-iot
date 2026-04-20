# Product Governance Reset Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a safe real-environment reset script that clears product-governance derived business data while preserving `iot_product`, `iot_device`, `iot_device_relation`, and existing device-product bindings.

**Architecture:** Add a standalone Python management script under `scripts/` that reads `application-dev.yml`, builds a deterministic cleanup plan, supports `dry-run / backup / execute`, and performs ordered `DELETE / UPDATE` operations with explicit scope filters. Cover the script with `unittest` before allowing any real-environment execution.

**Tech Stack:** Python 3, `argparse`, `json`, `pymysql`, `unittest`, Spring Boot `application-dev.yml`

---

### Task 1: Scaffold CLI parsing and cleanup target registry

**Files:**
- Create: `scripts/reset-product-governance-data.py`
- Create: `scripts/test_reset_product_governance_data.py`

- [ ] **Step 1: Write the failing registry test**

```python
class ProductGovernanceResetScriptTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_module()

    def test_build_cleanup_targets_keeps_master_tables(self) -> None:
        targets = self.module.build_cleanup_targets()
        self.assertEqual("risk_metric_linkage_binding", targets["delete_tables"][0])
        self.assertIn("iot_product_model", targets["delete_tables"])
        self.assertIn("rule_definition", targets["conditional_delete_tables"])
        self.assertIn("iot_product", targets["keep_tables"])
        self.assertNotIn("iot_product", targets["delete_tables"])

    def test_parse_args_defaults_to_dry_run(self) -> None:
        args = self.module.parse_args([])
        self.assertEqual("dry-run", args.mode)
        self.assertFalse(args.execute)
        self.assertFalse(args.confirm)
        self.assertEqual([], args.product_ids)
```

- [ ] **Step 2: Run the test and confirm failure**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

Expected: missing `build_cleanup_targets` / `parse_args`.

- [ ] **Step 3: Implement the minimal script skeleton**

```python
REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"

DELETE_TABLES = [
    "risk_metric_linkage_binding",
    "risk_metric_emergency_plan_binding",
    "risk_point_device_pending_promotion",
    "risk_point_device_pending_binding",
    "risk_point_device_capability_binding",
    "risk_point_device",
    "risk_metric_catalog",
    "iot_product_metric_resolver_snapshot",
    "iot_product_contract_release_snapshot",
    "iot_product_contract_release_batch",
    "iot_vendor_metric_mapping_rule_snapshot",
    "iot_vendor_metric_mapping_rule",
    "iot_vendor_metric_evidence",
    "iot_product_model",
]

CONDITIONAL_DELETE_TABLES = [
    "rule_definition",
    "iot_governance_work_item",
    "sys_governance_approval_transition",
    "sys_governance_approval_order",
]

KEEP_TABLES = ["iot_product", "iot_device", "iot_device_relation"]

def build_cleanup_targets() -> dict[str, list[str]]:
    return {
        "delete_tables": DELETE_TABLES[:],
        "conditional_delete_tables": CONDITIONAL_DELETE_TABLES[:],
        "keep_tables": KEEP_TABLES[:],
    }

def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Reset product governance derived data")
    parser.add_argument("--mode", choices=("dry-run", "backup", "execute"), default="dry-run")
    parser.add_argument("--tenant-id")
    parser.add_argument("--product-ids", nargs="*", default=[])
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--confirm", action="store_true")
    return parser.parse_args(argv)
```

- [ ] **Step 4: Re-run the test and confirm pass**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

- [ ] **Step 5: Commit**

```powershell
git add scripts/reset-product-governance-data.py scripts/test_reset_product_governance_data.py
git commit -m "feat: scaffold product governance reset script"
```

### Task 2: Add config loading, scope resolution, and dry-run manifest output

**Files:**
- Modify: `scripts/reset-product-governance-data.py`
- Modify: `scripts/test_reset_product_governance_data.py`

- [ ] **Step 1: Write the failing dry-run plan test**

```python
    def test_build_cleanup_plan_renders_scope_and_metadata_reset_targets(self) -> None:
        plan = self.module.build_cleanup_plan("2001", ["1001", "1002"], "dry-run")
        self.assertEqual("2001", plan["scope"]["tenantId"])
        self.assertEqual(["1001", "1002"], plan["scope"]["productIds"])
        self.assertEqual("iot_product", plan["metadata_reset"]["table"])
        self.assertIn("objectInsight.customMetrics", plan["metadata_reset"]["paths"])
        self.assertEqual("risk_metric_linkage_binding", plan["operations"][0]["table"])

    def test_load_dev_defaults_extracts_mysql_connection_info(self) -> None:
        defaults = self.module.load_dev_defaults()
        self.assertIn("jdbc_url", defaults)
        self.assertIn("user", defaults)
        self.assertIn("password", defaults)
```

- [ ] **Step 2: Run the test and confirm failure**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

- [ ] **Step 3: Implement config loading and plan generation**

```python
def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()

def load_dev_defaults() -> dict[str, str]:
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }

def build_cleanup_plan(tenant_id: str | None, product_ids: list[str], mode: str) -> dict[str, object]:
    operations = [{"table": table, "action": "delete"} for table in DELETE_TABLES]
    operations.extend({"table": table, "action": "conditional-delete"} for table in CONDITIONAL_DELETE_TABLES)
    operations.append({"table": "iot_product", "action": "reset-metadata"})
    operations.append({"table": "iot_device_onboarding_case", "action": "reset-release-batch"})
    return {
        "mode": mode,
        "scope": {
            "tenantId": tenant_id,
            "productIds": [item for item in product_ids if str(item).strip()],
            "scopeType": "product_ids" if product_ids else ("tenant" if tenant_id else "all"),
        },
        "operations": operations,
        "metadata_reset": {
            "table": "iot_product",
            "paths": ["objectInsight.customMetrics"],
        },
    }
```

- [ ] **Step 4: Add `main()` dry-run output**

```python
def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    plan = build_cleanup_plan(args.tenant_id, args.product_ids, args.mode)
    print(json.dumps(plan, ensure_ascii=False, indent=2))
    return 0
```

- [ ] **Step 5: Re-run the test and confirm pass**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

- [ ] **Step 6: Commit**

```powershell
git add scripts/reset-product-governance-data.py scripts/test_reset_product_governance_data.py
git commit -m "feat: add product governance reset dry-run planning"
```

### Task 3: Add backup mode, execute mode, and ordered SQL execution

**Files:**
- Modify: `scripts/reset-product-governance-data.py`
- Modify: `scripts/test_reset_product_governance_data.py`

- [ ] **Step 1: Write the failing execution test**

```python
    def test_execute_cleanup_plan_runs_delete_then_update_in_order(self) -> None:
        statements = []

        class FakeCursor:
            def execute(self, sql, params=()):
                statements.append((sql.strip(), tuple(params)))

        class FakeConnection:
            def cursor(self):
                return FakeCursor()
            def commit(self):
                statements.append(("COMMIT", ()))
            def __enter__(self):
                return self
            def __exit__(self, exc_type, exc, tb):
                return False

        self.module.execute_cleanup_plan(FakeConnection(), self.module.build_cleanup_plan("2001", ["1001"], "execute"))
        self.assertTrue(statements[0][0].startswith("DELETE FROM risk_metric_linkage_binding"))
        self.assertTrue(any("UPDATE iot_product" in sql for sql, _ in statements))
        self.assertEqual("COMMIT", statements[-1][0])
```

- [ ] **Step 2: Run the test and confirm failure**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

- [ ] **Step 3: Implement backup and execute helpers**

```python
def write_backup_manifest(output_dir: Path, plan: dict[str, object]) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / f"product-governance-reset-{datetime.now():%Y%m%d%H%M%S}.json"
    path.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    return path

def operation_sql(operation: dict[str, str]) -> str:
    if operation["action"] in {"delete", "conditional-delete"}:
        return f"DELETE FROM {operation['table']} WHERE deleted = 0"
    if operation["table"] == "iot_product":
        return "UPDATE iot_product SET metadata_json = NULL WHERE deleted = 0"
    return "UPDATE iot_device_onboarding_case SET release_batch_id = NULL WHERE deleted = 0"

def execute_cleanup_plan(connection, plan: dict[str, object]) -> None:
    with connection:
        cursor = connection.cursor()
        for operation in plan["operations"]:
            cursor.execute(operation_sql(operation))
        connection.commit()
```

- [ ] **Step 4: Add execute safety gate**

```python
def assert_execute_allowed(args: argparse.Namespace) -> None:
    if args.mode == "execute" and (not args.execute or not args.confirm):
        raise RuntimeError("Execute mode requires both --execute and --confirm")
```

- [ ] **Step 5: Re-run the test and confirm pass**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

- [ ] **Step 6: Commit**

```powershell
git add scripts/reset-product-governance-data.py scripts/test_reset_product_governance_data.py
git commit -m "feat: add product governance reset backup and execute flow"
```

### Task 4: Add scope-aware SQL filters, docs, and operator verification

**Files:**
- Modify: `scripts/reset-product-governance-data.py`
- Modify: `scripts/test_reset_product_governance_data.py`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Write the failing scope filter test**

```python
    def test_render_scope_clause_supports_all_tenant_and_product_filters(self) -> None:
        self.assertEqual("", self.module.render_scope_clause(None, []))
        self.assertIn("tenant_id = %s", self.module.render_scope_clause("2001", []))
        self.assertIn("product_id IN (%s,%s)", self.module.render_scope_clause(None, ["1001", "1002"]))
```

- [ ] **Step 2: Run the test and confirm failure**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
```

- [ ] **Step 3: Implement scope-aware SQL**

```python
def render_scope_clause(tenant_id: str | None, product_ids: list[str]) -> str:
    if product_ids:
        placeholders = ",".join(["%s"] * len(product_ids))
        return f" AND product_id IN ({placeholders})"
    if tenant_id:
        return " AND tenant_id = %s"
    return ""
```

- [ ] **Step 4: Update docs with script usage and acceptance wording**

```markdown
- 新增 `scripts/reset-product-governance-data.py`，用于在保留 `iot_product` / `iot_device` / `iot_device_relation` 的前提下重置产品治理派生数据。
- 脚本支持 `dry-run`、`backup`、`execute`；`execute` 必须显式携带 `--execute --confirm`。
- 执行后 `/products/:productId/contracts`、`/products/:productId/mapping-rules`、`/products/:productId/releases` 均应进入空态且不报错。
```

- [ ] **Step 5: Run regression and operator dry-run**

```powershell
python -m unittest scripts/test_reset_product_governance_data.py -v
python scripts/reset-product-governance-data.py --mode dry-run --product-ids 1001 1002
```

- [ ] **Step 6: Commit**

```powershell
git add scripts/reset-product-governance-data.py scripts/test_reset_product_governance_data.py docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "feat: add scoped product governance reset workflow"
```
