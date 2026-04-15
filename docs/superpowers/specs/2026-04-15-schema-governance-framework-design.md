# Schema Governance Framework Design

**Date:** 2026-04-15  
**Status:** Approved in session  
**Audience:** 后端 / DBA / 运维 / 交付 / AI 协作  
**Scope:** 为数据库对象、seed 包和真实库审计建立一套长期可维护的通用治理框架，先覆盖 `alarm` 域的 archived 对象治理闭环。

## 1. 背景

当前仓库已经完成第一轮数据库结构治理：

1. 结构真相源已经收口到 [schema/](../../../schema) registry。
2. `sql/init.sql`、`sql/init-tdengine.sql`、MySQL schema sync manifest、运行时 bootstrap manifest 都已由结构 registry 统一生成。
3. [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 已补齐对象总览、生命周期和主血缘链。

但围绕“无用表保守清理、seed 退场、真实库审计、后续长期维护”这一层，当前仍缺一套通用治理框架。现状问题集中在以下几类：

1. 结构真相已经收口，但“对象为什么处于 archived、什么时候能进入 pending_delete、删除前还差什么”仍主要靠人工判断。
2. `sql/init-data.sql` 中的 seed 包还没有独立治理语义，后续很难回答“某个 seed 是共享环境基线、演示数据，还是纯历史归档”。
3. 真实库审计目前还是按单对象脚本零散推进，例如 [scripts/audit-risk-point-highway-archive.py](../../../scripts/audit-risk-point-highway-archive.py) 只覆盖 `risk_point_highway_detail`，还不是通用框架。
4. archived / pending_delete 的生命周期虽然在文档和结构 registry 中已经挂牌，但缺少独立的治理真相源来承接审计规则、备份要求、人工检查项和删除前置条件。

本轮已得到一组真实现场证据，足以说明为什么需要通用治理框架：

1. `risk_point_highway_detail` 已被结构 registry 降级为 `archived`，且不再进入默认 init / schema sync / runtime bootstrap。
2. 共享 `dev` 审计确认该表仍有 `65` 条 `deleted=0` 数据，且全部关联现存 `risk_point`。
3. 其中 `RP-HW-SLOPE-045` 与 `RP-HW-SLOPE-046` 两个风险点仍存在 `9` 条有效 `risk_point_device_capability_binding`。

这说明后续治理目标不能只停留在“写一支导出脚本”，而要建立一条长期可维护的通用链路，让任意业务域都能一致回答：

1. 当前对象处于哪个治理阶段。
2. 对应 seed 包是否仍应保留。
3. 真实库是否仍在消费或挂接该对象。
4. 备份、审计、文档、删除前置条件是否都已满足。

## 2. 用户已确认的关键决策

本次设计已在会话中确认以下边界，后续实现不得偏离：

1. 覆盖范围采用“数据库对象 + seed + 真实库审计”，不只治理结构本身。
2. 运行方式采用“手动驱动”，不在本轮引入 CI / 定时巡检。
3. 生命周期采用“阶段化模型”，而不是只保留三层静态标签。
4. 治理单元按“业务域包”维护，而不是按单表零散维护。
5. 治理真相源独立放在 `schema-governance/`，不把复杂治理规则塞回 `schema/` 结构 registry。

## 3. 目标

本轮设计目标如下：

1. 建立独立于结构 registry 的 `schema-governance/` 治理真相源。
2. 用业务域包统一登记对象治理阶段、seed 包归属、真实库审计规则、备份要求、删除前置条件和人工检查项。
3. 把单对象脚本收口为可复用的域级治理脚本链，而不是继续按对象堆一次性脚本。
4. 让文档能够稳定回答“哪些对象被冻结、哪些已归档、哪些可待删、为什么还不能删”。
5. 在不引入重型流程平台的前提下，为后续 `device / governance / system / tdengine` 域扩展保留统一模式。

## 4. 非目标

本轮不做以下事情：

1. 不直接实现物理删表、删 seed、删真实库数据动作。
2. 不在本轮引入审批单、工单流、CI 门禁或定时巡检。
3. 不改变现有 `schema/` registry 的职责，不让结构真相源承担复杂治理规则。
4. 不一次性把全域所有对象全部接入治理框架。
5. 不把历史审计结果永久写回结构 registry。

## 5. 备选方案

### 5.1 方案 A：文档 + 脚本轻量拼装

做法：

1. 继续沿用 `schema/` 作为唯一结构真相源。
2. 对每个 archived 对象单独补脚本。
3. 在 `docs/04` 中手写治理状态和删除前置条件。

优点：

1. 落地快，改动少。

问题：

1. 生命周期、seed 包、审计规则会再次分散到脚本和文档。
2. 接第二个、第三个对象时会快速失控。
3. 无法形成业务域级统一治理入口。

### 5.2 方案 B：独立治理 registry + 手动治理脚本

做法：

1. 结构真相保留在 `schema/`。
2. 新建 `schema-governance/`，按业务域维护对象治理元数据。
3. 新建 `scripts/governance/`，统一提供校验、域审计、对象导出、文档渲染。
4. 文档由治理 registry 渲染或强校验，不再靠零散手写补齐状态。

优点：

1. 结构真相与治理真相职责清晰。
2. 同时覆盖对象、seed、真实库审计三类治理目标。
3. 手动驱动成本可控，适合当前仓库阶段。
4. 后续可逐域扩展，不需要重做框架。

问题：

1. 首轮需要补齐 loader、checker、文档渲染和域级报告。

### 5.3 方案 C：治理 registry + 工单化流程平台

做法：

1. 在方案 B 基础上，引入任务状态、责任人、审批结果和巡检批次。

优点：

1. 流程最完整。

问题：

1. 当前过重，明显超出本轮“先把通用治理框架搭起来”的目标。

### 5.4 结论

本轮采用 **方案 B：独立治理 registry + 手动治理脚本**。

## 6. 总体架构

建议把数据库治理拆成四层：

### 6.1 `schema/`

继续只负责结构真相：

1. 表 / 视图 / stable 定义。
2. 字段、索引、中文注释。
3. 关系、业务边界、bootstrap 策略。

### 6.2 `schema-governance/`

新增独立治理真相源，负责：

1. 生命周期阶段。
2. seed 包归属。
3. 真实库审计 profile。
4. 备份要求。
5. 删除前置条件。
6. 人工检查项。
7. 历史证据引用。

### 6.3 `scripts/governance/`

从治理 registry 统一执行：

1. registry 校验。
2. 域级真实库审计。
3. 对象或 seed 备份导出。
4. 治理文档渲染。

### 6.4 `docs/`

继续作为治理结果的权威可读出口，稳定回答：

1. 哪些对象处于什么阶段。
2. 哪些对象被什么条件阻塞。
3. 哪些 seed 包仍应保留。
4. 哪些对象已满足进入 `pending_delete` 的条件。

## 7. 生命周期模型

结构 registry 当前仍保留 `active / archived / pending_delete` 基础标签；治理框架则引入更细的阶段化模型：

1. `active`
   - 正常主链路对象或仍需持续保留的 seed 包。
2. `freeze_candidate`
   - 已发现边界模糊或疑似应退场，但尚未完成真实库审计与依赖确认。
3. `archived`
   - 已退出默认主链路，只允许审计、导出、文档说明和人工核查。
4. `pending_delete`
   - 结构、seed、真实库、人工检查均已满足删除前置条件，等待正式删除动作。
5. `dropped`
   - 已完成代码与环境退场，仅保留历史证据与变更记录引用。

采用这套模型的原因是：

1. 现实问题不是“该不该删”，而是“当前退场到哪一步”。
2. `freeze_candidate` 可以承接“疑似废弃但还没核实”的中间阶段。
3. `dropped` 可以承接真正退场后的留痕，避免未来重复审计。

## 8. 治理单元与数据模型

治理单元采用“业务域包”模式。建议首轮新增：

1. `schema-governance/alarm-domain.json`
2. `schema-governance/device-domain.json`
3. `schema-governance/governance-domain.json`
4. `schema-governance/system-domain.json`
5. `schema-governance/tdengine-domain.json`

每个域包不重复结构定义，只登记治理信息。对象项建议包含：

1. `objectName`
2. `storageType`
3. `governanceStage`
4. `ownerModule`
5. `businessDomain`
6. `seedPackages`
7. `realEnvAuditProfile`
8. `backupRequirements`
9. `deletionPrerequisites`
10. `manualChecklist`
11. `evidenceRefs`
12. `notes`

示例：

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
  ]
}
```

## 9. Seed 包治理

治理框架必须显式引入 `seedPackages` 概念，因为后续很多退场问题本质上不是“表还在不在”，而是“seed 还该不该继续保留”。

建议规则如下：

1. seed 包作为独立治理对象存在，但隶属于业务域包。
2. 一个对象可以挂多个 seed 包，一个 seed 包也可以覆盖多张表。
3. seed 包至少要声明：
   - 来源文件
   - 适用环境
   - 当前用途（共享环境基线 / 演示数据 / 历史归档）
   - 是否允许继续写入 `sql/init-data.sql`
   - 与哪些对象绑定治理

对于当前首批场景：

1. `risk_point_highway_detail` 对应的高速 archived 风险点 seed 要被显式建模。
2. 它不能只在 `sql/init-data.sql` 中作为一段孤立 SQL 存在。
3. 删除评估必须同时审计表和 seed 包，而不是只看表行数。

## 10. 脚本链路

手动驱动下，治理操作统一收口成以下链路：

### 10.1 `check_governance_registry.py`

用途：

1. 校验 `schema-governance/` 文件完整性。
2. 检查对象唯一性、阶段合法性、seed 包引用合法性、审计 profile 是否存在。

### 10.2 `run_domain_audit.py --domain <domain>`

用途：

1. 按业务域批量审计真实库对象、seed 包和删除前置条件。
2. 输出域级 JSON 报告。

报告至少要回答：

1. 哪些对象处于 `freeze_candidate`。
2. 哪些对象被阻塞在 `archived`。
3. 哪些对象已满足进入 `pending_delete` 的条件。
4. 哪些对象因真实库不可达或规则缺失而审计失败。

### 10.3 `export_object_backup.py --domain <domain> --object <name>`

用途：

1. 为单对象或单 seed 包统一导出 `json / csv / sql` 备份。
2. 替代按对象定制的一次性导出脚本。

### 10.4 `render_governance_docs.py --write`

用途：

1. 根据治理 registry 与最新审计结果回写治理清单。
2. 让权威文档直接回答对象阶段、阻塞原因和删除前置条件。

## 11. 错误处理与运行边界

治理脚本必须满足以下边界：

1. 默认全部只读，不提供删除入口。
2. 某个对象审计失败，不中断整个域审计；报告中明确标记 `audit_error`。
3. 真实环境不可达时，必须明确报告环境阻塞，不回退到假数据。
4. 对象没有 seed 包、没有真实库表、没有审计 profile 时，都必须显式输出，不允许静默跳过。
5. 文档渲染只能覆盖生成区域，不得覆盖手写正文。

## 12. 测试策略

建议测试分三层：

### 12.1 registry 单测

覆盖：

1. 域包完整性。
2. 阶段合法性。
3. 对象与 seed 包唯一性。
4. 审计 profile、证据引用、删除前置条件格式合法性。

### 12.2 渲染/文档单测

覆盖：

1. 域级 JSON 报告格式。
2. 治理清单文档渲染结果。
3. 文档生成区漂移检查。

### 12.3 审计脚本单测

覆盖：

1. 删除阻塞原因判定。
2. 备份导出内容。
3. 审计 profile 解析。
4. 环境阻塞报错。

## 13. 首批落地范围

首批只做最小闭环，不一次铺满全域：

### Phase 1：治理 registry 基础

1. 新建 `schema-governance/`。
2. 实现 loader / checker / 基础数据模型。

### Phase 2：接入 `alarm` 域

1. 首个对象为 `risk_point_highway_detail`。
2. 同时把高速 archived 风险点 seed 包纳入治理。

### Phase 3：通用脚本链

1. 实现 `check_governance_registry.py`。
2. 实现 `run_domain_audit.py`。
3. 实现 `export_object_backup.py`。

### Phase 4：文档收口

1. 更新 `docs/04`、`docs/08`。
2. 更新 `README.md` 与 `AGENTS.md` 的协作规则。

### Phase 5：逐域扩展

后续再逐步纳入：

1. `device`
2. `governance`
3. `system`
4. `tdengine`

## 14. 对当前 `risk_point_highway_detail` 的直接意义

采用本框架后，`risk_point_highway_detail` 不再只是一个“被临时标成 archived 的对象”，而会成为治理框架中的首个标准样板：

1. 它的结构真相继续留在 [schema/mysql/alarm-domain.json](../../../schema/mysql/alarm-domain.json)。
2. 它的治理阶段、seed 包、审计规则、备份要求、删除前置条件移入 `schema-governance/alarm-domain.json`。
3. 它当前仍被 `9` 条有效能力绑定阻塞，因此只能停在 `archived`，不能进入 `pending_delete`。
4. 后续若这些阻塞被解除，推进的不是“直接删表”，而是：
   - 更新治理阶段
   - 重新执行域审计
   - 更新文档
   - 再决定是否进入 `pending_delete`

## 15. 成功标准

首轮成功标准如下：

1. 仓库中出现独立的 `schema-governance/` 真相源。
2. `alarm` 域至少有一套完整治理样板，覆盖对象、seed、审计、备份、删除前置条件。
3. 手动执行域审计时，能稳定输出域级 JSON 报告和对象级备份。
4. `docs/04` 能回答各对象所处治理阶段与阻塞原因。
5. 后续新增第二个 archived 对象时，不需要再重写一套一次性脚本和文档流程。
