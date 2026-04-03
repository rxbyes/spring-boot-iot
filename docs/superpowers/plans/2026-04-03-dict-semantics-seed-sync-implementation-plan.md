# Dict Semantics Seed Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the data dictionary stop showing legacy risk/alarm values by aligning the authoritative seeds in `sql/init-data.sql` and enforcing the same semantics in historical environments through `scripts/run-real-env-schema-sync.py`.

**Architecture:** Keep the fix at the data-source layer rather than hiding old values in the UI. `sql/init-data.sql` remains the authoritative seed for fresh databases, while `scripts/run-real-env-schema-sync.py` becomes the canonical “align and prune” path for existing environments by merging legacy dict items into the current target set and normalizing referenced business values.

**Tech Stack:** SQL seed scripts, Python 3 standard library `unittest`, PyMySQL, project docs

---

### Task 1: Lock Script-Level Dict Alignment Behavior With Failing Tests

**Files:**
- Create: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `scripts/run-real-env-schema-sync.py`

- [ ] **Step 1: Write the failing tests**

Create `scripts/tests/test_run_real_env_schema_sync.py` with import-by-path coverage for the current script:

```python
import importlib.util
import pathlib
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "run-real-env-schema-sync.py"
SPEC = importlib.util.spec_from_file_location("schema_sync", SCRIPT_PATH)
schema_sync = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(schema_sync)


class NormalizeColorCaseTest(unittest.TestCase):
    def test_normalize_color_case_maps_legacy_and_runtime_values(self):
        sql = schema_sync.normalize_color_case("alarm_level")
        self.assertIn("WHEN 'critical' THEN 'red'", sql)
        self.assertIn("WHEN 'warning' THEN 'orange'", sql)
        self.assertIn("WHEN 'medium' THEN 'yellow'", sql)
        self.assertIn("WHEN 'low' THEN 'blue'", sql)


class DictTargetDefinitionTest(unittest.TestCase):
    def test_alarm_level_targets_only_keep_four_color_values(self):
        targets = schema_sync.level_dict_targets()["alarm_level"]["target_items"]
        self.assertEqual(
            [item[0] for item in targets],
            ["red", "orange", "yellow", "blue"],
        )
        self.assertEqual(targets[0][4], ["critical"])
        self.assertEqual(targets[1][4], ["warning", "high"])
        self.assertEqual(targets[2][4], ["medium"])
        self.assertEqual(targets[3][4], ["info", "low"])

    def test_risk_level_targets_do_not_keep_legacy_values_as_visible_items(self):
        targets = schema_sync.level_dict_targets()["risk_level"]["target_items"]
        self.assertEqual(
            [item[0] for item in targets],
            ["red", "orange", "yellow", "blue"],
        )
        flattened_legacy = [legacy for _, _, _, _, legacy_values, _ in targets for legacy in legacy_values]
        self.assertCountEqual(flattened_legacy, ["critical", "warning", "info"])
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected:
- FAIL because `level_dict_targets()` does not exist yet.
- The failure proves the script has no testable central source for the target dict definitions.

- [ ] **Step 3: Write the minimal implementation support**

Refactor `scripts/run-real-env-schema-sync.py` to expose the target definitions through a single helper instead of duplicating literals inside `ensure_level_dicts()`:

```python
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
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected:
- PASS
- The script now has a single testable source of truth for the three dict definitions.

### Task 2: Reproduce and Fix Duplicate Dict Row Cleanup

**Files:**
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `scripts/run-real-env-schema-sync.py`

- [ ] **Step 1: Write the failing duplicate-row test**

Extend `scripts/tests/test_run_real_env_schema_sync.py` with a fake cursor that records SQL and simulates two active `sys_dict` rows for the same `dict_code`:

```python
class FakeCursor:
    def __init__(self):
        self.executed = []
        self._results = [
            (7202,),  # canonical sys_dict row selected by ensure_level_dict
            [         # duplicated sys_dict rows with same dict_code
                (7202,),
                (9902,),
            ],
            [         # existing sys_dict_item rows under canonical dict_id
                (7304, "critical"),
                (7305, "warning"),
                (7306, "medium"),
                (7307, "info"),
            ],
        ]

    def execute(self, sql, params=None):
        self.executed.append((sql, params))

    def fetchone(self):
        result = self._results.pop(0)
        if isinstance(result, list):
            raise AssertionError("fetchone called for list result")
        return result

    def fetchall(self):
        result = self._results.pop(0)
        if not isinstance(result, list):
            raise AssertionError("fetchall called for scalar result")
        return result


class DictDuplicateCleanupTest(unittest.TestCase):
    def test_ensure_level_dict_marks_duplicate_dict_rows_deleted(self):
        cursor = FakeCursor()
        schema_sync.ensure_level_dict(
            cursor,
            "rm_iot",
            dict_code="alarm_level",
            **schema_sync.level_dict_targets()["alarm_level"],
        )

        duplicate_cleanup = [
            (sql, params)
            for sql, params in cursor.executed
            if "UPDATE sys_dict" in sql and "id <> %s" in sql
        ]
        self.assertEqual(len(duplicate_cleanup), 1)
        cleanup_sql, cleanup_params = duplicate_cleanup[0]
        self.assertIn("dict_code = %s", cleanup_sql)
        self.assertEqual(cleanup_params, ("alarm_level", 7202))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync.DictDuplicateCleanupTest
```

Expected:
- FAIL because `ensure_level_dict()` currently updates only the first matching `sys_dict` row and does not mark duplicate rows for the same `dict_code` as deleted.

- [ ] **Step 3: Write the minimal implementation**

Refactor `ensure_level_dict()` so it cleans up duplicate dict rows before processing dict items:

```python
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
```

Then call `cleanup_duplicate_level_dicts(cur, dict_id, dict_code)` inside `ensure_level_dict()` immediately after the canonical `dict_id` is chosen.

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected:
- PASS
- The script now guarantees each level dict code collapses onto one canonical `sys_dict` row before item alignment.

### Task 3: Reproduce and Fix Seed Re-Run Cleanup Drift in `sql/init-data.sql`

**Files:**
- Modify: `sql/init-data.sql`
- Create: `scripts/tests/test_dict_seed_snapshot.py`

- [ ] **Step 1: Write the failing seed cleanup test**

Create `scripts/tests/test_dict_seed_snapshot.py` that asserts the seed script contains both the authoritative inserts and a cleanup block for re-run scenarios:

```python
import pathlib
import unittest


INIT_DATA_SQL = (pathlib.Path(__file__).resolve().parents[1] / ".." / "sql" / "init-data.sql").resolve()


class DictSeedSnapshotTest(unittest.TestCase):
    def test_seed_contains_current_level_dict_definitions(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("'risk_point_level'", content)
        self.assertIn("'level_1'", content)
        self.assertIn("'alarm_level'", content)
        self.assertIn("'red'", content)
        self.assertIn("'risk_level'", content)
        self.assertIn("'黄色'", content)

    def test_seed_soft_deletes_duplicate_level_dict_rows_and_non_target_items(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("UPDATE sys_dict", content)
        self.assertIn("dict_code IN ('risk_point_level', 'alarm_level', 'risk_level')", content)
        self.assertIn("UPDATE sys_dict_item", content)
        self.assertIn("item_value NOT IN ('level_1', 'level_2', 'level_3')", content)
        self.assertIn("item_value NOT IN ('red', 'orange', 'yellow', 'blue')", content)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
python -m unittest scripts.tests.test_dict_seed_snapshot
```

Expected:
- FAIL because `sql/init-data.sql` currently seeds the right values but does not proactively soft-delete duplicate dict rows or non-target items when rerun against a historical environment.

- [ ] **Step 3: Write the minimal seed fix**

Keep the existing authoritative inserts, then add explicit cleanup SQL for rerun scenarios:

```sql
INSERT INTO sys_dict (id, tenant_id, dict_name, dict_code, dict_type, status, sort_no, remark, create_by, create_time, update_by, update_time, deleted)
VALUES
    (7201, 1, '风险点等级', 'risk_point_level', 'text', 1, 1, '风险点档案等级字典', 1, NOW(), 1, NOW(), 0),
    (7202, 1, '告警等级', 'alarm_level', 'text', 1, 2, '告警等级四色字典', 1, NOW(), 1, NOW(), 0),
    (7203, 1, '风险态势等级', 'risk_level', 'text', 1, 3, '运行态风险颜色字典', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    dict_name = VALUES(dict_name),
    dict_type = VALUES(dict_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

UPDATE sys_dict
SET status = 0,
    deleted = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_code IN ('risk_point_level', 'alarm_level', 'risk_level')
  AND id NOT IN (7201, 7202, 7203);

INSERT INTO sys_dict_item (id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark, create_by, create_time, update_by, update_time, deleted)
VALUES
    (7301, 1, 7201, '一级风险点', 'level_1', 'string', 1, 1, '风险点等级-一级风险点', 1, NOW(), 1, NOW(), 0),
    (7302, 1, 7201, '二级风险点', 'level_2', 'string', 1, 2, '风险点等级-二级风险点', 1, NOW(), 1, NOW(), 0),
    (7303, 1, 7201, '三级风险点', 'level_3', 'string', 1, 3, '风险点等级-三级风险点', 1, NOW(), 1, NOW(), 0),
    (7304, 1, 7202, '红色', 'red', 'string', 1, 1, '告警等级-红色', 1, NOW(), 1, NOW(), 0),
    (7305, 1, 7202, '橙色', 'orange', 'string', 1, 2, '告警等级-橙色', 1, NOW(), 1, NOW(), 0),
    (7306, 1, 7202, '黄色', 'yellow', 'string', 1, 3, '告警等级-黄色', 1, NOW(), 1, NOW(), 0),
    (7307, 1, 7202, '蓝色', 'blue', 'string', 1, 4, '告警等级-蓝色', 1, NOW(), 1, NOW(), 0),
    (7308, 1, 7203, '红色', 'red', 'string', 1, 1, '风险态势等级-红色', 1, NOW(), 1, NOW(), 0),
    (7309, 1, 7203, '橙色', 'orange', 'string', 1, 2, '风险态势等级-橙色', 1, NOW(), 1, NOW(), 0),
    (7310, 1, 7203, '黄色', 'yellow', 'string', 1, 3, '风险态势等级-黄色', 1, NOW(), 1, NOW(), 0),
    (7311, 1, 7203, '蓝色', 'blue', 'string', 1, 4, '风险态势等级-蓝色', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name),
    item_value = VALUES(item_value),
    item_type = VALUES(item_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

UPDATE sys_dict_item
SET status = 0,
    deleted = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND (
      (dict_id = 7201 AND item_value NOT IN ('level_1', 'level_2', 'level_3'))
      OR (dict_id = 7202 AND item_value NOT IN ('red', 'orange', 'yellow', 'blue'))
      OR (dict_id = 7203 AND item_value NOT IN ('red', 'orange', 'yellow', 'blue'))
  );
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
python -m unittest scripts.tests.test_dict_seed_snapshot
```

Expected:
- PASS
- The seed file remains a reliable fresh-install baseline and also cleans up rerun drift for these dicts.

### Task 4: Verify End-to-End Dict Alignment and Sync Minimal Docs

**Files:**
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `sql/init-data.sql`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `scripts/tests/test_dict_seed_snapshot.py`

- [ ] **Step 1: Update the change log**

Add a 2026-04-03 bullet to `docs/08-变更记录与技术债清单.md` that states:

```md
- 2026-04-03：继续收口数据字典中的风险治理历史项。`sql/init-data.sql` 与 `scripts/run-real-env-schema-sync.py` 当前已把 `risk_point_level / alarm_level / risk_level` 三套字典统一收口到业务语义目标集；共享环境执行 schema sync 后，不再继续把 `critical / warning / info / high / medium / low` 暴露为独立字典项。
```

- [ ] **Step 2: Run focused verification**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync scripts.tests.test_dict_seed_snapshot
```

Expected:
- PASS
- The new tests prove both fresh seeds and historical sync behavior stay aligned.

- [ ] **Step 3: Run the real schema sync script in dry usage mode**

Run:

```bash
python scripts/run-real-env-schema-sync.py --help
```

Expected:
- Exit code `0`
- Script usage prints normally, confirming refactoring did not break CLI entry.

- [ ] **Step 4: Review the final diff**

Run:

```bash
git diff -- sql/init-data.sql scripts/run-real-env-schema-sync.py scripts/tests/test_run_real_env_schema_sync.py scripts/tests/test_dict_seed_snapshot.py docs/08-变更记录与技术债清单.md
```

Expected:
- Only the three target dicts, their legacy-pruning logic, tests, and the minimal change-log note appear.
- No frontend-only hiding logic or unrelated dict behavior changes are introduced.
