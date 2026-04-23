# Schema Governance Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为数据库对象、seed 包和真实库审计建立首批可运行的通用治理框架，先覆盖 `alarm` 域的 `risk_point_highway_detail` archived 样板。

**Architecture:** 保持 `schema/` 继续作为结构真相源，新建独立的 `schema-governance/` 作为治理真相源。首轮通过 Python loader/checker、域级审计脚本、通用对象备份脚本和治理附录渲染脚本形成最小闭环，并把已有单对象 archived 审计脚本收口到通用实现。

**Tech Stack:** Python 3、`unittest`、`pymysql`、JSON、Markdown、现有 `schema` registry 工具链

---

## File Structure

- Create: `schema-governance/alarm-domain.json`  
  Responsibility: 首批治理域包，登记 `risk_point_highway_detail` 与高速 archived seed 包的治理阶段、审计 profile、备份要求和删除前置条件。
- Create: `scripts/governance/load_governance_registry.py`  
  Responsibility: 加载并校验治理 registry，校验对象引用、阶段合法性、seed 包引用和结构 registry 对齐。
- Create: `scripts/governance/check_governance_registry.py`  
  Responsibility: 一次性执行治理 registry 校验并输出明确结果。
- Create: `scripts/governance/domain_audit_support.py`  
  Responsibility: 提供通用真实库连接、MySQL archived 对象审计、JSON/CSV/SQL 导出和删除就绪判断。
- Create: `scripts/governance/run_domain_audit.py`  
  Responsibility: 按业务域执行审计并输出域级 JSON 报告。
- Create: `scripts/governance/export_object_backup.py`  
  Responsibility: 对单对象执行通用备份导出，不再按对象堆一次性脚本。
- Create: `scripts/governance/render_governance_docs.py`  
  Responsibility: 从治理 registry 生成治理附录目录。
- Create: `scripts/tests/test_governance_registry.py`  
  Responsibility: 锁定治理 registry 契约、阶段合法性、对象引用与 seed 包要求。
- Create: `scripts/tests/test_governance_tools.py`  
  Responsibility: 锁定域级审计、删除阻塞判断、备份导出与治理附录渲染。
- Modify: `scripts/audit-risk-point-highway-archive.py`  
  Responsibility: 收口为通用治理脚本的兼容包装入口。
- Modify: `docs/04-数据库设计与初始化数据.md`  
  Responsibility: 补入通用治理框架入口、治理附录引用和首批 `alarm` 域样板说明。
- Modify: `docs/08-变更记录与技术债清单.md`  
  Responsibility: 记录通用治理框架落地与 archived 对象治理收口。
- Modify: `README.md`  
  Responsibility: 增加 `schema-governance/` 和治理脚本链说明。
- Modify: `AGENTS.md`  
  Responsibility: 把通用治理 registry 流程固化为协作规则。
- Create: `docs/appendix/database-schema-governance-catalog.generated.md`  
  Responsibility: 由治理 registry 渲染的治理对象目录。

### Task 1: 锁定治理 registry 与工具链失败测试

**Files:**
- Create: `scripts/tests/test_governance_registry.py`
- Create: `scripts/tests/test_governance_tools.py`

- [ ] **Step 1: 写治理 registry 契约失败测试**

```python
def test_alarm_governance_domain_should_register_highway_archive_sample(self):
    registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    alarm_domain = registry.domains["alarm"]
    self.assertIn("risk_point_highway_detail", alarm_domain.objects)
    self.assertEqual(
        "archived",
        alarm_domain.objects["risk_point_highway_detail"].governance_stage,
    )
    self.assertIn(
        "highway_archived_risk_points_seed",
        alarm_domain.objects["risk_point_highway_detail"].seed_packages,
    )
```

- [ ] **Step 2: 运行治理 registry 测试，确认当前先红灯**

Run: `python -m unittest scripts.tests.test_governance_registry -v`  
Expected: FAIL，提示 `schema-governance/` 或治理 loader 尚不存在。

- [ ] **Step 3: 写通用审计/导出失败测试**

```python
def test_evaluate_governance_status_should_block_when_capability_binding_exists(self):
    audit = {
        "table_exists": True,
        "row_count_total": 65,
        "capability_binding_count": 9,
        "risk_point_device_active_binding_count": 0,
        "risk_point_join": {"missing_parent_rows": 0},
    }
    decision = tools.evaluate_deletion_readiness(audit)
    self.assertFalse(decision["ready"])
    self.assertIn("CAPABILITY_BINDINGS_STILL_PRESENT", decision["blocking_reasons"])
```

- [ ] **Step 4: 运行工具测试，确认当前先红灯**

Run: `python -m unittest scripts.tests.test_governance_tools -v`  
Expected: FAIL，提示治理工具模块尚不存在。

### Task 2: 实现治理 registry 与 checker

**Files:**
- Create: `schema-governance/alarm-domain.json`
- Create: `scripts/governance/load_governance_registry.py`
- Create: `scripts/governance/check_governance_registry.py`

- [ ] **Step 1: 落首个 alarm 域治理样板**

```json
{
  "domain": "alarm",
  "objects": [
    {
      "objectName": "risk_point_highway_detail",
      "storageType": "mysql_table",
      "governanceStage": "archived",
      "ownerModule": "spring-boot-iot-alarm",
      "businessDomain": "alarm",
      "seedPackages": ["highway_archived_risk_points_seed"],
      "realEnvAuditProfile": "mysql_archived_object_with_seed",
      "backupRequirements": {
        "exportFormats": ["json", "csv", "sql"],
        "mustCaptureRowCount": true
      },
      "deletionPrerequisites": [
        "no_active_capability_bindings",
        "seed_removed_from_init_data",
        "docs_and_registry_updated"
      ],
      "manualChecklist": [
        "确认扩展字段已无人工核查需求或已迁入新真相源"
      ],
      "evidenceRefs": [
        "docs/04-数据库设计与初始化数据.md",
        "docs/08-变更记录与技术债清单.md"
      ],
      "notes": "高速项目归档观察对象，不进入默认主链路。"
    }
  ],
  "seedPackages": [
    {
      "name": "highway_archived_risk_points_seed",
      "sourceFiles": ["sql/init-data.sql"],
      "usage": "历史归档",
      "allowedInInitData": true,
      "boundObjects": ["risk_point", "risk_point_highway_detail"]
    }
  ]
}
```

- [ ] **Step 2: 实现治理 loader，强校验对象引用与阶段合法性**

```python
VALID_GOVERNANCE_STAGES = {
    "active",
    "freeze_candidate",
    "archived",
    "pending_delete",
    "dropped",
}
```

```python
structure_registry = load_registry(structure_schema_root)
if object_name not in structure_registry.mysql and object_name not in structure_registry.mysql_views and object_name not in structure_registry.tdengine:
    raise ValueError(f"Governance object {object_name!r} not found in structure registry")
```

- [ ] **Step 3: 实现 checker 入口**

```python
load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
print("governance registry check passed")
```

- [ ] **Step 4: 重新运行治理 registry 测试，确认转绿**

Run: `python -m unittest scripts.tests.test_governance_registry -v`  
Expected: PASS

### Task 3: 实现通用审计、导出与兼容包装脚本

**Files:**
- Create: `scripts/governance/domain_audit_support.py`
- Create: `scripts/governance/run_domain_audit.py`
- Create: `scripts/governance/export_object_backup.py`
- Modify: `scripts/audit-risk-point-highway-archive.py`

- [ ] **Step 1: 先把当前单对象审计能力下沉为可复用函数**

```python
def audit_mysql_archived_object(connection_args: dict[str, object], object_name: str, sample_limit: int) -> dict[str, object]:
    with pymysql.connect(...):
        ...
        return {
            "objectName": object_name,
            "table_exists": ...,
            "row_count_total": ...,
            "capability_binding_count": ...,
            "risk_point_join": ...,
            "export_columns": column_names,
            "export_rows": rows,
        }
```

- [ ] **Step 2: 写域级审计入口，只支持首个 profile**

```python
if obj.real_env_audit_profile == "mysql_archived_object_with_seed":
    audit = audit_mysql_archived_object(conn_args, obj.object_name, args.sample_limit)
else:
    audit = {"objectName": obj.object_name, "audit_error": f"unsupported profile: {obj.real_env_audit_profile}"}
```

- [ ] **Step 3: 写通用导出入口**

```python
if "json" in export_formats:
    write_json(...)
if "csv" in export_formats:
    export_rows_to_csv(...)
if "sql" in export_formats:
    write_sql(...)
```

- [ ] **Step 4: 把旧脚本改成兼容包装**

```python
from scripts.governance.domain_audit_support import ...

# 保持原 CLI 用法，但内部改走通用治理支持模块
```

- [ ] **Step 5: 运行工具测试，确认转绿**

Run: `python -m unittest scripts.tests.test_governance_tools -v`  
Expected: PASS

### Task 4: 渲染治理附录并更新权威文档

**Files:**
- Create: `scripts/governance/render_governance_docs.py`
- Create: `docs/appendix/database-schema-governance-catalog.generated.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 先写治理附录渲染输出**

```markdown
# Database Schema Governance Catalog

| Domain | Object | Stage | Seed Packages | Audit Profile | Owner Module | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| alarm | risk_point_highway_detail | archived | highway_archived_risk_points_seed | mysql_archived_object_with_seed | spring-boot-iot-alarm | 高速项目归档观察对象，不进入默认主链路。 |
```

- [ ] **Step 2: 在 `docs/04` 增加治理框架入口**

```md
- 通用治理真相源固定为 `schema-governance/`，结构真相仍固定为 `schema/`。
- archived / pending_delete 对象、seed 包和真实库审计统一通过 `scripts/governance/*.py` 手动驱动。
- 当前首批治理样板见 `alarm` 域与治理附录。
```

- [ ] **Step 3: 更新 `README.md` 与 `AGENTS.md` 协作规则**

```md
- 当任务涉及 archived / pending_delete 对象、seed 退场或真实库删除前置条件时，必须同时更新 `schema-governance/`，并执行治理 checker / 域级审计。
```

- [ ] **Step 4: 渲染治理附录并校验文档引用**

Run: `python scripts/governance/render_governance_docs.py --write`  
Expected: 生成 `docs/appendix/database-schema-governance-catalog.generated.md`

### Task 5: 端到端验证

**Files:**
- Test: `scripts/tests/test_governance_registry.py`
- Test: `scripts/tests/test_governance_tools.py`

- [ ] **Step 1: 运行治理测试集**

Run:

```powershell
python -m unittest scripts.tests.test_governance_registry scripts.tests.test_governance_tools -v
```

Expected: PASS

- [ ] **Step 2: 运行治理 checker**

Run:

```powershell
python scripts/governance/check_governance_registry.py
```

Expected:

```text
governance registry check passed
```

- [ ] **Step 3: 运行首个域级真实库审计**

Run:

```powershell
python scripts/governance/run_domain_audit.py --domain alarm
```

Expected:

```text
JSON 输出中包含 risk_point_highway_detail、archived、ARCHIVE_ROWS_STILL_PRESENT、CAPABILITY_BINDINGS_STILL_PRESENT
```

- [ ] **Step 4: 跑通对象级导出**

Run:

```powershell
python scripts/governance/export_object_backup.py --domain alarm --object risk_point_highway_detail
```

Expected:

```text
成功导出 json/csv/sql 备份，且不做任何删除动作
```

- [ ] **Step 5: 运行空白检查**

Run:

```powershell
git diff --check
```

Expected: PASS
