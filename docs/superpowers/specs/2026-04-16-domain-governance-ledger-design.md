# Domain Governance Ledger Design

> 文档定位：数据库域级治理台账设计说明。
> 适用范围：`schema/` 结构真相源、`schema-governance/` 治理真相源及数据库附录生成链。
> 更新时间：2026-04-16

## 1. 背景

当前数据库治理已经具备三块基础能力：

- `schema/` 负责结构真相，包括对象结构、中文注释、生命周期、关系、`lineageRole` 与 `businessBoundary`。
- `schema-governance/` 负责退场治理真相，包括 archived / pending_delete 对象、seed 包、真实库审计 profile、备份要求与删除前置条件。
- 自动附录已经分别覆盖数据库对象目录与血缘目录，能够回答“有哪些对象”和“对象之间如何关联”。

但目前仍缺一份面向“域级治理”的总览台账，无法在同一页上快速回答下面这些长期维护问题：

- 每个业务域到底有多少对象、生命周期如何分布。
- 每个业务域当前有哪些对象仍在主链路、哪些已经进入治理/退场阶段。
- 每个业务域的主血缘关系、关系复杂度和业务边界是什么。
- 真实库审计结论应该从哪里查，而不把手工结论和自动生成目录混在一起。

因此需要补一份域级治理台账，把结构真相与治理真相按“业务域”聚合起来，形成可持续维护的治理入口。

## 2. 目标

新增一份自动生成的域级治理台账附录，统一按域汇总：

- 域内对象规模、生命周期分布、关系边数量与角色分布。
- 域内对象的结构清单与业务边界说明。
- 域内治理对象的阶段、seed 归属、审计 profile 与删除前置条件。
- 域内主血缘关系摘要。

同时保持真实库审计事实继续原位维护在权威文档中，不单独落审计 JSON 快照文件，不把真实库动态结果混入自动附录正文。

## 3. 约束与设计取舍

### 3.1 保持双真相源边界

- `schema/` 仍是结构真相源。
- `schema-governance/` 仍是治理真相源。
- 不新增第三类 registry，不修改现有 registry JSON 契约。

### 3.2 采用“静态台账 + 文档内人工审计结论”模式

本轮用户已明确选择方案 `2`，即：

- 新台账只消费 `schema/ + schema-governance/`。
- 不把 `run_domain_audit.py` 的真实库结果落成机读快照文件。
- 真实库审计事实继续人工回写到 `docs/04-数据库设计与初始化数据.md` 与 `docs/08-变更记录与技术债清单.md`。

该选择的代价也需要显式记录：

- 审计结果不会形成可自动比对的历史快照。
- 长期一致性弱于“静态台账 + 审计快照双产物”模式。
- 需要把“真实库审计后同步回写权威文档”继续固化为协作规则。

### 3.3 新台账放入治理渲染链

新产物不放入 `scripts/schema/render_artifacts.py`，而放入 `scripts/governance/render_governance_docs.py`，原因如下：

- 该台账是“结构真相 + 治理真相”的汇总，不再是纯结构产物。
- 把它归到治理侧，可以保持 schema 渲染链只负责纯结构产物。
- 避免 `render_artifacts.py` 继续膨胀成混合职责脚本。

## 4. 产物设计

新增附录：

- `docs/appendix/database-schema-domain-governance.generated.md`

该附录为自动生成文件，不允许手工编辑，固定由治理渲染链生成。

## 5. 台账内容结构

每个业务域固定输出以下五部分。

### 5.1 域摘要

用于快速展示域级复杂度，至少包含：

- 对象总数
- `active / archived / pending_delete` 数量
- 关系边数量
- `ownerModule` 分布
- `lineageRole` 分布

### 5.2 对象分层清单

按对象角色分组，首版可直接使用 `lineageRole` 分组，不额外发明新的分层字段。

每个对象至少展示：

- 对象名
- storage type
- lifecycle
- 是否进 init
- 是否进 schema sync
- runtime bootstrap 策略
- owner module
- 中文说明

这一部分用于回答“域内有哪些对象、哪些还在主链路、哪些已经退出默认链路”。

### 5.3 治理对象清单

只列当前在 `schema-governance/` 中登记过的对象，至少展示：

- object name
- governance stage
- seed packages
- audit profile
- deletion prerequisites
- notes

如果某个域当前没有治理对象，也要显式写出“当前无登记治理对象”，而不是留空。

### 5.4 主血缘链摘要

不复制完整血缘附录正文，只输出当前域的关系摘要和一个精简 Mermaid 图。

目的不是替代完整血缘文档，而是提供域级入口，让维护者快速判断：

- 这个域改动时最可能影响哪些上下游对象。
- 域内关系是简单链路、树状还是高耦合网状。

### 5.5 人工审计结论入口

自动附录中不内嵌真实库动态结果，只写清楚：

- 当前域如有真实库审计结论，请查看 `docs/04` 对应对象条目。
- 带日期的最近审计事实与阶段性决策，请查看 `docs/08`。

这一段的目标是建立入口，而不是重复人工事实。

## 6. 生成链路

### 6.1 结构变更链

当任务涉及结构对象变化时，继续沿用：

1. 修改 `schema/`
2. 执行 `python scripts/schema/render_artifacts.py --write`
3. 执行 `python scripts/schema/check_schema_registry.py`
4. 同步更新 `docs/04` 与必要的权威文档

### 6.2 治理变更链

当任务涉及治理对象、治理附录或域级治理台账时，固定执行：

1. 修改 `schema-governance/` 或治理渲染脚本
2. 执行 `python scripts/governance/render_governance_docs.py --write`
3. 执行 `python scripts/governance/check_governance_registry.py`
4. 如涉及真实库审计，继续执行对应审计脚本
5. 把真实库事实回写到 `docs/04` / `docs/08`

## 7. 最小实现范围

本轮只实现最小可运行切片：

- 给 `scripts/governance/render_governance_docs.py` 增加一个新产物：
  - `docs/appendix/database-schema-domain-governance.generated.md`
- 该产物只消费现有 `schema/` 与 `schema-governance/`
- 不新增新的 registry 字段
- 不新增真实库审计快照文件
- 不改动现有 `run_domain_audit.py` 结果契约

## 8. 测试与校验策略

### 8.1 先写失败测试

先在治理侧补测试，锁定新产物至少包含：

- `alarm` 域摘要
- `risk_point_highway_detail` 的治理对象信息
- 域内对象数量或生命周期统计
- 域内血缘/关系摘要入口

### 8.2 最终校验

至少执行以下命令：

```powershell
python -m unittest scripts.tests.test_governance_registry scripts.tests.test_governance_tools -v
python scripts/governance/render_governance_docs.py --write
python scripts/governance/check_governance_registry.py
git diff --check
```

如测试中已覆盖新渲染逻辑，可把对应测试文件纳入同一条 `unittest` 命令。

## 9. 失败边界

- 如果发现现有 `schema-governance/` 数据不足以支撑域级治理台账，先显式暴露缺口，不凭空 invent 新字段。
- 如果自动附录与 `docs/04` / `docs/08` 的现有权威口径冲突，以权威文档为准，先修入口再扩逻辑。
- 不把真实库实时查询塞进渲染脚本，避免生成链依赖共享 `dev` 环境。

## 10. 预期收益

补齐域级治理台账后，数据库治理会形成四层稳定入口：

1. 对象目录：回答“有哪些对象”
2. 血缘目录：回答“对象如何关联”
3. 域级治理台账：回答“这个域现在治理状态如何”
4. 权威文档中的人工审计结论：回答“真实库现场事实是什么”

这样既保留自动生成的稳定性，也保留真实环境审计的人为确认边界，更符合长期扩展、持续更新和后续维护的治理目标。
