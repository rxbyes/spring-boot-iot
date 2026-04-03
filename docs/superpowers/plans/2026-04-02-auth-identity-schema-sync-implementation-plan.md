# Auth Identity Schema Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the shared-dev MySQL schema sync baseline so `sys_user.org_id` and `sys_role.data_scope_type` are auto-aligned, then update authority docs and re-verify login plus `/api/auth/me` on the real `dev` environment.

**Architecture:** Keep `scripts/run-real-env-schema-sync.py` as the only MySQL schema catch-up entrypoint for historical shared environments. Add explicit identity-governance coverage in the script, back it with a small Python regression harness, then align the database/runtime/acceptance docs so troubleshooting and live verification all point to the same flow.

**Tech Stack:** Python 3, PyMySQL, MySQL `information_schema`, Spring Boot auth endpoints, Markdown docs

---

## File Map

- `scripts/run-real-env-schema-sync.py`
  Adds `sys_user.org_id`, `sys_role.data_scope_type`, an index-existence helper, and an identity-defaults helper wired into the main schema-sync flow.
- `scripts/tests/test_run_real_env_schema_sync.py`
  New regression harness that loads the schema-sync script by file path and checks the new identity coverage without needing a live MySQL instance.
- `docs/04-数据库设计与初始化数据.md`
  Database authority doc; add `sys_user.org_id` and `sys_role.data_scope_type` field semantics.
- `docs/07-部署运行与配置说明.md`
  Runtime guide; expand the documented coverage of `run-real-env-schema-sync.py`.
- `docs/真实环境测试与验收手册.md`
  Acceptance runbook; add login/auth-context troubleshooting for missing identity columns.
- `docs/08-变更记录与技术债清单.md`
  Change log; record that the shared-dev identity schema sync path now covers login/auth-context recovery after live verification succeeds.
- `README.md` / `AGENTS.md`
  Verification-only in this plan. Inspect for stale schema-sync wording; leave untouched unless they explicitly contradict the new authority-doc wording.

### Task 1: Add Regression Coverage And Identity Schema Sync Logic

**Files:**
- Create: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `scripts/run-real-env-schema-sync.py:199-253`
- Modify: `scripts/run-real-env-schema-sync.py:305-328`
- Modify: `scripts/run-real-env-schema-sync.py:549-624`
- Test: `scripts/tests/test_run_real_env_schema_sync.py`

- [ ] **Step 1: Write the failing regression test for identity schema coverage**

```python
# scripts/tests/test_run_real_env_schema_sync.py
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch


SCRIPT_PATH = Path(__file__).resolve().parents[1] / "run-real-env-schema-sync.py"
SPEC = importlib.util.spec_from_file_location("schema_sync", SCRIPT_PATH)
schema_sync = importlib.util.module_from_spec(SPEC)
assert SPEC is not None and SPEC.loader is not None
SPEC.loader.exec_module(schema_sync)


class RecordingCursor:
    def __init__(self) -> None:
        self.executed: list[tuple[str, tuple | None]] = []

    def execute(self, sql: str, params: tuple | None = None) -> None:
        self.executed.append((sql, params))

    def fetchone(self):
        return None


class SchemaSyncIdentityTests(unittest.TestCase):
    def test_identity_columns_are_part_of_schema_sync_baseline(self) -> None:
        self.assertIn(
            ("org_id", "BIGINT DEFAULT NULL COMMENT '主机构ID'"),
            schema_sync.COLUMNS_TO_ADD["sys_user"],
        )
        self.assertIn(
            ("data_scope_type", "VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '数据范围类型'"),
            schema_sync.COLUMNS_TO_ADD["sys_role"],
        )

    @patch.object(schema_sync, "index_exists", return_value=False, create=True)
    @patch.object(schema_sync, "column_exists")
    @patch.object(schema_sync, "table_exists", return_value=True)
    def test_identity_helper_adds_index_and_builtin_role_defaults(
        self,
        table_exists_mock,
        column_exists_mock,
        index_exists_mock,
    ) -> None:
        available_columns = {
            ("sys_user", "org_id"),
            ("sys_role", "data_scope_type"),
        }
        column_exists_mock.side_effect = (
            lambda cur, db, table, column: (table, column) in available_columns
        )

        cursor = RecordingCursor()
        schema_sync.ensure_identity_governance_defaults(cursor, "rm_iot")

        statements = [sql for sql, _ in cursor.executed]
        self.assertIn(
            "ALTER TABLE `sys_user` ADD KEY `idx_user_org_id` (`org_id`)",
            statements,
        )

        super_admin_sql = next(
            sql for sql, params in cursor.executed if params == ("ALL", "SUPER_ADMIN")
        )
        self.assertIn("data_scope_type = 'TENANT'", super_admin_sql)

        self.assertTrue(
            any(params == ("ORG_AND_CHILDREN", "MANAGEMENT_STAFF") for _, params in cursor.executed)
        )
        self.assertTrue(
            any(params == ("SELF", "BUSINESS_STAFF") for _, params in cursor.executed)
        )

    @patch.object(schema_sync, "index_exists", return_value=True, create=True)
    @patch.object(schema_sync, "column_exists", return_value=False)
    @patch.object(schema_sync, "table_exists", return_value=True)
    def test_identity_helper_is_noop_without_required_columns(
        self,
        table_exists_mock,
        column_exists_mock,
        index_exists_mock,
    ) -> None:
        cursor = RecordingCursor()
        schema_sync.ensure_identity_governance_defaults(cursor, "rm_iot")
        self.assertEqual([], cursor.executed)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the regression test to confirm it fails before the script change**

Run:

```bash
python -m unittest discover -s scripts/tests -p 'test_run_real_env_schema_sync.py' -v
```

Expected: FAIL with an assertion such as `KeyError: 'sys_user'` or `AssertionError` because `COLUMNS_TO_ADD` does not yet declare `sys_user` / `sys_role` identity columns, and `ensure_identity_governance_defaults` does not exist yet.

- [ ] **Step 3: Implement the minimal schema-sync code to make the test pass**

```python
# scripts/run-real-env-schema-sync.py
# Insert these two entries into COLUMNS_TO_ADD directly after "iot_command_record"
    "sys_user": [
        ("org_id", "BIGINT DEFAULT NULL COMMENT '主机构ID'"),
    ],
    "sys_role": [
        ("data_scope_type", "VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '数据范围类型'"),
        ("description", "VARCHAR(500) DEFAULT NULL COMMENT 'description'"),
    ],
```

```python
# scripts/run-real-env-schema-sync.py
def index_exists(cur: pymysql.cursors.Cursor, db: str, table: str, index_name: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        LIMIT 1
        """,
        (db, table, index_name),
    )
    return cur.fetchone() is not None


def ensure_identity_governance_defaults(cur: pymysql.cursors.Cursor, db: str) -> None:
    if table_exists(cur, db, "sys_user") and column_exists(cur, db, "sys_user", "org_id"):
        if not index_exists(cur, db, "sys_user", "idx_user_org_id"):
            cur.execute("ALTER TABLE `sys_user` ADD KEY `idx_user_org_id` (`org_id`)")

    if not table_exists(cur, db, "sys_role") or not column_exists(cur, db, "sys_role", "data_scope_type"):
        return

    role_defaults = (
        ("SUPER_ADMIN", "ALL", True),
        ("MANAGEMENT_STAFF", "ORG_AND_CHILDREN", True),
        ("BUSINESS_STAFF", "SELF", True),
        ("OPS_STAFF", "TENANT", False),
        ("DEVELOPER_STAFF", "TENANT", False),
    )
    for role_code, scope_type, replace_default_tenant in role_defaults:
        if replace_default_tenant:
            cur.execute(
                """
                UPDATE sys_role
                SET data_scope_type=%s,
                    update_by=1,
                    update_time=NOW(),
                    deleted=0
                WHERE tenant_id = 1
                  AND role_code = %s
                  AND (
                        data_scope_type IS NULL
                        OR TRIM(data_scope_type) = ''
                        OR data_scope_type = 'TENANT'
                  )
                """,
                (scope_type, role_code),
            )
        else:
            cur.execute(
                """
                UPDATE sys_role
                SET data_scope_type=%s,
                    update_by=1,
                    update_time=NOW(),
                    deleted=0
                WHERE tenant_id = 1
                  AND role_code = %s
                  AND (
                        data_scope_type IS NULL
                        OR TRIM(data_scope_type) = ''
                  )
                """,
                (scope_type, role_code),
            )
```

```python
# scripts/run-real-env-schema-sync.py main() excerpt
            for table, specs in COLUMNS_TO_ADD.items():
                if not table_exists(cur, args.db, table):
                    print(f"[skip] table missing: {table}")
                    continue
                for column, definition in specs:
                    if column_exists(cur, args.db, table, column):
                        continue
                    cur.execute(f"ALTER TABLE `{table}` ADD COLUMN `{column}` {definition}")
                    print(f"[column] {table}.{column} added")

            ensure_identity_governance_defaults(cur, args.db)
            print("[identity] sys_user.org_id and sys_role.data_scope_type aligned")

            if table_exists(cur, args.db, "sys_dict"):
                if column_exists(cur, args.db, "sys_dict", "dict_value") and column_exists(
                    cur, args.db, "sys_dict", "dict_label"
                ):
                    ensure_dict_defaults(cur)
                    print("[dict] sys_dict default constraints aligned")
```

- [ ] **Step 4: Re-run the regression test and make sure it passes**

Run:

```bash
python -m unittest discover -s scripts/tests -p 'test_run_real_env_schema_sync.py' -v
```

Expected: PASS with `Ran 3 tests` and `OK`.

- [ ] **Step 5: Commit the script and regression coverage**

```bash
git add scripts/run-real-env-schema-sync.py scripts/tests/test_run_real_env_schema_sync.py
git commit -m "feat: sync auth identity schema defaults"
```

### Task 2: Align Authority Docs With The New Identity Schema Sync Baseline

**Files:**
- Modify: `docs/04-数据库设计与初始化数据.md:104-126`
- Modify: `docs/07-部署运行与配置说明.md:443-451`
- Modify: `docs/真实环境测试与验收手册.md:93-107`
- Modify: `docs/真实环境测试与验收手册.md:789`

- [ ] **Step 1: Update the database authority doc for `sys_user` and `sys_role`**

```md
##### `sys_user`

- 作用：平台登录账号与人员主数据。
- 字段：
  - `id` / `tenant_id`：用户主键和所属租户。
  - `org_id`：主机构 ID，用于登录身份上下文、账号中心归属展示和后续组织范围收口。
  - `username` / `password`：登录用户名和密码摘要。
  - `nickname` / `real_name`：展示昵称和真实姓名。
  - `phone` / `email` / `avatar`：联系信息和头像地址。
  - `status`：账号启停状态。
  - `is_admin`：是否为管理员标记。
  - `last_login_ip` / `last_login_time`：最近登录来源与时间。
  - `remark`：账号说明。
  - `create_by/create_time/update_by/update_time/deleted`：审计字段和软删除标记。

##### `sys_role`

- 作用：角色与权限域分组定义。
- 字段：
  - `id` / `tenant_id`：角色主键和租户范围。
  - `role_name` / `role_code`：角色名称和唯一编码；授权回填脚本主要按 `role_code` 识别真实角色。
  - `description`：角色职责说明。
  - `data_scope_type`：数据范围类型；当前内置基线包括 `ALL / TENANT / ORG_AND_CHILDREN / SELF`。
  - `status`：角色启停状态。
  - `create_by/create_time/update_by/update_time/deleted`：审计字段和软删除标记。
```

- [ ] **Step 2: Update the runtime doc so schema-sync coverage includes identity-governance fields**

```md
### 5.2 历史库对齐

仓库当前不再保留独立增量 SQL 文件。历史库若需对齐当前代码，请按以下顺序处理：
1. 优先使用最新 `sql/init.sql` + `sql/init-data.sql`，并在启用 TDengine 时补充执行 `sql/init-tdengine.sql`，完成目标环境初始化。
2. 无法重建时，人工比对当前 `sql/init.sql` 的表、列、索引和兼容视图，或执行 `python scripts/run-real-env-schema-sync.py` 做 MySQL schema 对齐。
3. 若访问 `/api/system/help-doc/**` 或 `/api/system/in-app-message/**` 时收到“系统内容依赖表缺失，请先按最新 sql/init.sql、sql/init-data.sql 对齐当前初始化基线”类业务错误，说明真实库仍缺少系统内容表、列或桥接治理结构，应优先完成基线对齐。
4. TDengine 兼容表 `iot_device_telemetry_point`、telemetry v2 raw stable 与 `iot_agg_measure_hour` 当前优先通过 `sql/init-tdengine.sql` 初始化；其中 `spring-boot-iot-telemetry` 仍只对兼容表与 raw stable 保留运行时自动补齐能力，`iot_agg_measure_hour` 继续要求脚本手动初始化。
5. 若历史共享环境还需要把旧 TDengine 数据补齐到 telemetry v2 raw/latest，请在 schema 初始化完成后再调用 `POST /api/telemetry/migrate-history`；不要期待启动过程自动回灌。
6. 只适用于历史共享环境或现场专项设备的种子数据，不再以独立 SQL 文件保留，需按真实环境业务数据单独治理。
7. `python scripts/run-real-env-schema-sync.py` 当前已覆盖 `iot_device_online_session`、`iot_device_metric_latest`、`iot_device_invalid_report_state`、`sys_user.org_id`、`sys_role.data_scope_type`、`risk_point.org_id / org_name` 以及 `iot_message_log` 兼容视图的对齐补齐。
8. 当共享 `dev` 环境登录或鉴权上下文因缺列报错时，优先执行该脚本，再复测 `POST /api/auth/login`、`GET /api/auth/me` 与账号中心链路。
```

- [ ] **Step 3: Update the acceptance runbook so login/auth-context failures map to the same schema-sync flow**

```md
验收前先确认：
- 新库：已执行 `sql/init.sql`（如需样例数据再执行 `sql/init-data.sql`）
- 历史库：优先按最新 `sql/init.sql`、`sql/init-data.sql` 重建；无法重建时，人工比对当前基线或执行 `python scripts/run-real-env-schema-sync.py`
- 验收系统内容能力前，历史库至少已对齐 `sys_help_document`、`sys_in_app_message`、`sys_in_app_message_read`
- 验收设备接入失败归档能力前，历史库至少已对齐 `iot_device_access_error_log`
- 如本轮要查看失败归档中的设备契约快照，先确认目标库已具备 `contract_snapshot` 字段；无法重建时可执行 `python scripts/run-real-env-schema-sync.py`
- 当前共享开发环境已于 `2026-03-24` 完成风险监测 / GIS 真实环境复验；若复用该共享环境，不再把风险监测 schema sync 视为默认阻塞项
- `2026-03-31 12:10` 已在当前共享开发环境按双文件基线执行一次 `python scripts/run-real-env-schema-sync.py`，补齐 `iot_device_invalid_report_state` 与 `iot_message_log` 兼容视图，并复查 `35` 张表、`1` 个兼容视图与关键治理表字段全部齐备；证据见 `logs/acceptance/schema-baseline-health-20260331121007.json`
- `2026-04-02` 起，若登录接口在真实环境报 `Unknown column 'org_id' in 'field list'`，说明共享库尚未对齐 `sys_user.org_id`；若 `/api/auth/me`、账号中心或权限上下文报 `data_scope_type` 缺失，也应执行 `python scripts/run-real-env-schema-sync.py` 补齐 `sys_role.data_scope_type` 后再复测。
- `2026-04-02` 起，若风险点接口在真实环境报 `Unknown column 'org_id' in 'field list'`，说明共享库尚未对齐“所属组织”字段；可直接执行 `python scripts/run-real-env-schema-sync.py` 补齐 `risk_point.org_id / org_name` 后再复测。
```

```md
6. 若大量接口返回 `500`，优先检查真实库 schema 是否与当前代码一致；必要时先按最新 `sql/init.sql` / `sql/init-data.sql` 对齐，或执行 `python scripts/run-real-env-schema-sync.py` 后再复测。若登录接口报 `Unknown column 'org_id' in 'field list'`，说明 `sys_user.org_id` 尚未补齐；若 `/api/auth/me` 或账号中心报 `data_scope_type` 缺失，说明 `sys_role.data_scope_type` 尚未补齐；若风险点接口报同样的 `org_id` 错误，则说明 `risk_point.org_id / org_name` 尚未补齐。
```

- [ ] **Step 4: Check `README.md` and `AGENTS.md` for stale schema-sync wording, but leave them unchanged unless they contradict the updated authority docs**

Run:

```bash
rg -n "run-real-env-schema-sync.py|org_id|data_scope_type|账号中心" README.md AGENTS.md
```

Expected: no stale wording that narrows schema-sync coverage to the old set of fields. Leave `README.md` and `AGENTS.md` untouched if the grep only shows generic schema-alignment guidance.

- [ ] **Step 5: Verify the three authority docs mention the new identity fields and commit them**

Run:

```bash
rg -n "sys_user.org_id|sys_role.data_scope_type|/api/auth/me|Unknown column 'org_id'" \
  docs/04-数据库设计与初始化数据.md \
  docs/07-部署运行与配置说明.md \
  docs/真实环境测试与验收手册.md
```

Expected: hits in all three files, with both identity fields and the login/auth-context troubleshooting wording present.

```bash
git add \
  docs/04-数据库设计与初始化数据.md \
  docs/07-部署运行与配置说明.md \
  docs/真实环境测试与验收手册.md
git commit -m "docs: align auth identity schema sync guidance"
```

### Task 3: Re-Verify The Shared Dev Environment And Record The Recovery

**Files:**
- Modify: `docs/08-变更记录与技术债清单.md:17-25`

- [ ] **Step 1: Confirm the Spring Boot `dev` backend is reachable before running live verification**

Run:

```bash
lsof -iTCP:9999 -sTCP:LISTEN
```

Expected: one Java process is listening on port `9999`.

If the command returns nothing, start the backend in a separate terminal with:

```bash
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: startup logs finish with Tomcat listening on `9999` and the app using `application-dev.yml`.

- [ ] **Step 2: Run the real-environment schema sync against the shared dev baseline**

Run:

```bash
python scripts/run-real-env-schema-sync.py
```

Expected: the command ends with `Schema sync completed.` and includes the new identity log line:

```text
[identity] sys_user.org_id and sys_role.data_scope_type aligned
```

The command is allowed to be idempotent on rerun; if the shared database was already patched once, it should still complete cleanly without reporting an error.

- [ ] **Step 3: Query `information_schema` and the built-in roles directly to prove the shared dev schema is aligned**

```python
python - <<'PY'
import os
import pymysql

conn = pymysql.connect(
    host=os.getenv("IOT_MYSQL_HOST", "8.130.107.120"),
    port=int(os.getenv("IOT_MYSQL_PORT", "3306")),
    user=os.getenv("IOT_MYSQL_USERNAME", "root"),
    password=os.getenv("IOT_MYSQL_PASSWORD", "mI8%pB1*gD"),
    database=os.getenv("IOT_MYSQL_DB", "rm_iot"),
    charset="utf8mb4",
    autocommit=True,
)

with conn.cursor() as cur:
    cur.execute(
        """
        SELECT COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='sys_user' AND COLUMN_NAME='org_id'
        """,
        (os.getenv("IOT_MYSQL_DB", "rm_iot"),),
    )
    assert cur.fetchone(), "sys_user.org_id missing"

    cur.execute(
        """
        SELECT COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='sys_role' AND COLUMN_NAME='data_scope_type'
        """,
        (os.getenv("IOT_MYSQL_DB", "rm_iot"),),
    )
    assert cur.fetchone(), "sys_role.data_scope_type missing"

    cur.execute(
        """
        SELECT role_code, data_scope_type
        FROM sys_role
        WHERE tenant_id = 1
          AND role_code IN ('SUPER_ADMIN', 'MANAGEMENT_STAFF', 'BUSINESS_STAFF', 'OPS_STAFF', 'DEVELOPER_STAFF')
        ORDER BY role_code
        """
    )
    rows = cur.fetchall()
    expected = {
        "SUPER_ADMIN": "ALL",
        "MANAGEMENT_STAFF": "ORG_AND_CHILDREN",
        "BUSINESS_STAFF": "SELF",
        "OPS_STAFF": "TENANT",
        "DEVELOPER_STAFF": "TENANT",
    }
    actual = {role_code: scope for role_code, scope in rows}
    assert actual == expected, actual

print("identity schema verified")
PY
```

Expected: the script prints `identity schema verified` and exits with code `0`.

- [ ] **Step 4: Verify live login plus `/api/auth/me` on the running Spring Boot app**

```python
python - <<'PY'
import json
import urllib.request


def request_json(url, payload=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers)
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)


login = request_json(
    "http://127.0.0.1:9999/api/auth/login",
    {
        "loginType": "account",
        "username": "admin",
        "password": "123456",
    },
)
assert login["code"] == 200, login
token = login["data"]["token"]

me = request_json(
    "http://127.0.0.1:9999/api/auth/me",
    token=token,
)
assert me["code"] == 200, me
assert me["data"]["orgId"] is not None, me
assert me["data"]["dataScopeType"] == "ALL", me

print(json.dumps({"login": login["code"], "me": me["code"], "scope": me["data"]["dataScopeType"]}, ensure_ascii=False))
PY
```

Expected: the script prints JSON similar to `{"login": 200, "me": 200, "scope": "ALL"}` and the browser login page is able to leave `/login` after a manual retry.

- [ ] **Step 5: Record the verified recovery in the change log and commit it**

Only do this step after Step 4 returns `200` for both login and `/api/auth/me`.

```md
### 1.2 系统内容与帮助中心

- 2026-04-02：共享 `dev` 环境身份治理 schema sync 已补齐 `sys_user.org_id` 与 `sys_role.data_scope_type`。`python scripts/run-real-env-schema-sync.py` 现在会同时补列、补 `idx_user_org_id` 索引并回填内置角色的数据范围默认值；完成对齐后，`POST /api/auth/login`、`GET /api/auth/me` 与依赖 `authContext` 的账号中心链路已恢复真实环境可用。README.md 与 AGENTS.md 本轮已检查，无需同步改动。
```

```bash
git add docs/08-变更记录与技术债清单.md
git commit -m "docs: record auth identity schema sync recovery"
```
