# Domain Governance Ledger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为数据库治理新增一份按域聚合的自动台账附录，统一汇总结构真相、血缘摘要和治理对象信息，并继续把真实库审计事实留在权威文档中。

**Architecture:** 保持 `schema/` 与 `schema-governance/` 双真相源边界不变，把新产物接入 `scripts/governance/render_governance_docs.py`，由治理渲染链同时输出“治理对象目录”和“域级治理台账”。首版只消费现有 registry，不新增快照文件，不修改真实库审计契约。测试先锁定渲染输出，再补最小实现与文档入口。

**Tech Stack:** Python 3、`unittest`、Markdown、现有 `schema` / `schema-governance` registry 与治理渲染脚本

---

## File Structure

- Modify: `scripts/governance/render_governance_docs.py`
  Responsibility: 在现有治理目录渲染基础上新增“域级治理台账”渲染、写盘与 CLI 输出。
- Modify: `scripts/tests/test_governance_tools.py`
  Responsibility: 先写失败测试，再锁定域级治理台账的最小输出内容与格式。
- Create: `docs/appendix/database-schema-domain-governance.generated.md`
  Responsibility: 自动生成的域级治理台账附录。
- Modify: `docs/04-数据库设计与初始化数据.md`
  Responsibility: 增加域级治理台账入口，说明真实库审计事实仍在权威文档人工维护。
- Modify: `docs/08-变更记录与技术债清单.md`
  Responsibility: 记录域级治理台账生成能力落地。
- Modify: `README.md`
  Responsibility: 补充治理生成链的新附录说明。
- Modify: `AGENTS.md`
  Responsibility: 固化域级治理台账也是治理渲染链生成物，禁止手改。

### Task 1: 先补域级治理台账失败测试

**Files:**
- Modify: `scripts/tests/test_governance_tools.py`

- [ ] **Step 1: 在治理工具测试里新增失败测试**

```python
def test_render_domain_governance_ledger_should_include_alarm_summary_and_governance_object(self):
    registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    markdown = render_tools.render_domain_governance_ledger(registry)

    self.assertIn("# Database Schema Domain Governance Ledger", markdown)
    self.assertIn("## Domain alarm", markdown)
    self.assertIn("| Lifecycle | Count |", markdown)
    self.assertIn("| archived | 1 |", markdown)
    self.assertIn("| risk_point_highway_detail | archived |", markdown)
    self.assertIn("当前域如有真实库审计结论，请查看 `docs/04`", markdown)
```

- [ ] **Step 2: 运行单测，确认当前先红灯**

Run:

```powershell
python -m unittest scripts.tests.test_governance_tools.GovernanceToolsTest.test_render_domain_governance_ledger_should_include_alarm_summary_and_governance_object -v
```

Expected: FAIL，提示 `render_domain_governance_ledger` 尚不存在或输出缺少目标内容。

### Task 2: 在治理渲染链里实现域级治理台账

**Files:**
- Modify: `scripts/governance/render_governance_docs.py`
- Create: `docs/appendix/database-schema-domain-governance.generated.md`

- [ ] **Step 1: 给治理渲染脚本增加第二个输出路径常量**

```python
OUTPUT_PATH = REPO_ROOT / "docs" / "appendix" / "database-schema-governance-catalog.generated.md"
DOMAIN_LEDGER_OUTPUT_PATH = REPO_ROOT / "docs" / "appendix" / "database-schema-domain-governance.generated.md"
```

- [ ] **Step 2: 实现按域聚合的统计 helper**

```python
def build_domain_governance_rows(registry: GovernanceRegistry) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for domain_name in sorted({*registry.structure_registry.mysql.keys(), *registry.domains.keys()}):
        ...
        rows.append(
            {
                "domain": domain_name,
                "structureObjects": structure_objects,
                "governanceObjects": governance_objects,
                "lifecycleCounts": lifecycle_counts,
                "relationCount": relation_count,
                "ownerModules": owner_modules,
                "lineageRoles": lineage_roles,
            }
        )
    return rows
```

- [ ] **Step 3: 实现域级治理台账 Markdown 渲染函数**

```python
def render_domain_governance_ledger(registry: GovernanceRegistry) -> str:
    lines = [
        "# Database Schema Domain Governance Ledger",
        "",
        "Generated from the schema and schema-governance registries. Do not edit by hand.",
    ]
    ...
    return "\\n".join(lines).strip() + "\\n"
```

要求输出至少包含：
- 域摘要
- 生命周期统计表
- 对象清单
- 治理对象清单
- 关系/血缘摘要入口
- “真实库审计结论请查看 `docs/04` / `docs/08`” 的固定提示

- [ ] **Step 4: 扩展 `--write` 逻辑，同时写两份治理附录**

```python
if args.write:
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(markdown, encoding="utf-8", newline="\\n")
    DOMAIN_LEDGER_OUTPUT_PATH.write_text(domain_markdown, encoding="utf-8", newline="\\n")
```

- [ ] **Step 5: 重新运行单测，确认转绿**

Run:

```powershell
python -m unittest scripts.tests.test_governance_tools.GovernanceToolsTest.test_render_domain_governance_ledger_should_include_alarm_summary_and_governance_object -v
```

Expected: PASS

### Task 3: 补齐治理渲染测试覆盖

**Files:**
- Modify: `scripts/tests/test_governance_tools.py`

- [ ] **Step 1: 补充“无治理对象域也要显式输出”测试**

```python
def test_render_domain_governance_ledger_should_show_no_governance_objects_for_plain_domain(self):
    registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    markdown = render_tools.render_domain_governance_ledger(registry)

    self.assertIn("## Domain device", markdown)
    self.assertIn("当前无登记治理对象", markdown)
```

- [ ] **Step 2: 补充“关系摘要入口”测试**

```python
def test_render_domain_governance_ledger_should_include_relation_summary_for_alarm_domain(self):
    registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    markdown = render_tools.render_domain_governance_ledger(registry)

    self.assertIn("risk_point（belongs_to:risk_point_id）", markdown)
    self.assertIn("```mermaid", markdown)
```

- [ ] **Step 3: 运行治理工具测试集，确认全部转绿**

Run:

```powershell
python -m unittest scripts.tests.test_governance_tools -v
```

Expected: PASS

### Task 4: 更新权威文档入口与协作规则

**Files:**
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 在 `docs/04` 增加域级治理台账入口**

```md
- `docs/appendix/database-schema-domain-governance.generated.md` 由治理渲染链生成，用于按域汇总对象规模、生命周期、治理对象与血缘摘要。
- 真实库审计事实仍继续原位维护在本文与 `docs/08`，不直接嵌入自动附录正文。
```

- [ ] **Step 2: 在 `docs/08` 记录本轮能力落地**

```md
- 2026-04-16：治理渲染链新增域级治理台账 `docs/appendix/database-schema-domain-governance.generated.md`，用于按域汇总结构对象、生命周期、治理对象与血缘摘要；真实库审计事实继续保留在 `docs/04` / `docs/08` 手工维护。
```

- [ ] **Step 3: 在 `README.md` 更新治理附录说明**

```md
- 治理渲染链当前会同时生成治理对象目录与域级治理台账；后者用于按域汇总结构真相、治理对象与血缘摘要。
```

- [ ] **Step 4: 在 `AGENTS.md` 固化生成物规则**

```md
- `docs/appendix/database-schema-governance-catalog.generated.md` 与 `docs/appendix/database-schema-domain-governance.generated.md` 都属于治理渲染链生成物，禁止手改而不回写脚本或 registry。
```

### Task 5: 生成附录并完成端到端校验

**Files:**
- Test: `scripts/tests/test_governance_tools.py`
- Generate: `docs/appendix/database-schema-governance-catalog.generated.md`
- Generate: `docs/appendix/database-schema-domain-governance.generated.md`

- [ ] **Step 1: 生成治理附录**

Run:

```powershell
python scripts/governance/render_governance_docs.py --write
```

Expected: 成功写出治理对象目录与域级治理台账两份附录。

- [ ] **Step 2: 运行治理测试集**

Run:

```powershell
python -m unittest scripts.tests.test_governance_registry scripts.tests.test_governance_tools scripts.tests.test_risk_point_highway_archive_audit -v
```

Expected: PASS

- [ ] **Step 3: 运行治理 checker**

Run:

```powershell
python scripts/governance/check_governance_registry.py
```

Expected:

```text
governance registry check passed
```

- [ ] **Step 4: 检查生成物与空白错误**

Run:

```powershell
git diff --check
```

Expected: PASS
