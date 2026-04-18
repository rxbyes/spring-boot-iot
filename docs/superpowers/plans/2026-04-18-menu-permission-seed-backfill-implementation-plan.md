# Menu Permission Seed Backfill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 只在 `sql/init-data.sql` 与既有文档内补齐当前真实业务工作页、按钮和重页面内部功能点到 `sys_menu / sys_role_menu`，不改前端路由、导航结构和后端权限逻辑。

**Architecture:** 本轮继续走最小迁移：新增一个正式页面菜单 `/protocol-governance`，把现有协议治理与密钥治理权限挂回真实承载页，再为重页面补齐隐藏 `type=2 / menu_type=2` 权限节点。新增 `sys_role_menu` 绑定不重做角色矩阵，而是按父页面当前已有角色做一次继承补齐；超级管理员继续依赖运行态全量 active menu 逻辑。

**Tech Stack:** MySQL seed SQL, PowerShell static verification, Markdown docs

---

## Scope Check

本计划只覆盖已经批准的最小范围：

1. `sql/init-data.sql` 中的 `sys_menu / sys_role_menu` 种子补齐
2. `docs/02`、`docs/04`、`docs/08` 的原位同步更新
3. `/protocol-governance` 正式页面菜单补齐
4. 当前真实业务工作页、按钮和重页面内部功能点的隐藏权限补齐

本计划明确不做：

1. 一级菜单或二级菜单重构
2. 前端路由、页面结构、导航和按钮显隐逻辑改造
3. 后端权限判断、首页跳转和角色模型改造
4. schema registry、`sql/init.sql`、`sql/init-tdengine.sql`、运行时 bootstrap 变更
5. `/device-access`、`/risk-disposal`、`/risk-config`、`/system-management`、`/quality-workbench`
6. `/automation-assets`、`/automation-test`、`/future-lab`、`/risk-enhance`、`/device-onboarding`

## File Structure

### Seed

- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

### Docs

- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

### Review Only

- Review: `E:\idea\ghatg\spring-boot-iot\README.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

## Execution Guardrails

1. 只能在 `codex/dev` 上执行：

```powershell
git branch --show-current
```

Expected: 输出 `codex/dev`

2. 当前工作区已经是脏的，且 `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql` 已混入不属于本任务的 `/device-onboarding` 在途改动。执行本计划时只能修改本计划列出的菜单权限 seed，不能回退、覆盖或顺带提交 `/device-onboarding` 相关行。

3. 本任务只允许改：

- `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`
- `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

4. 本任务不跑 schema 渲染链，因为没有 schema 变更；如果执行过程中发现需要改 `schema/`、`sql/init.sql` 或 runtime bootstrap，说明已经超出批准范围，必须停止并回报。

### Task 1: Add the `/protocol-governance` page seed and reparent existing governance permissions

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

- [ ] **Step 1: Verify the current gap and wrong parent bindings**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern '93001009|93001026, 1, 93001001|93001027, 1, 93001001|93001028, 1, 93001001|93001030, 1, 93001001|93001031, 1, 93001001' `
  -Encoding UTF8
```

Expected:
- 没有 `93001009` 页面行
- `93001026/93001027/93001028` 仍挂在 `93001001`
- `93001030/93001031` 仍挂在 `93001001`

- [ ] **Step 2: Insert the new page row and correct the parent IDs in the governance permission block**

在 `sys_menu` 首个页面菜单 `INSERT` 块里加入 `/protocol-governance` 页面行，并在治理细粒度权限块里把协议治理与密钥治理权限挂回真实页面：

```sql
(93001009, 1, 93000001, '协议治理工作台', 'iot:protocol-governance', '/protocol-governance', 'ProtocolGovernanceWorkbenchView', 'guide', '{"caption":"协议族、解密档案与协议模板治理"}', 11, 1, 1, '/protocol-governance', 'iot:protocol-governance', 11, 1, 1, 1, NOW(), 1, NOW(), 0),
```

```sql
    (93001026, 1, 93003023, '密钥托管查看', 'iot:secret-custody:view', '', '', '', '{"caption":"查看密钥托管与轮换记录"}', 1126, 2, 2, '', 'iot:secret-custody:view', 1126, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001027, 1, 93003023, '密钥轮换执行', 'iot:secret-custody:rotate', '', '', '', '{"caption":"执行设备密钥轮换"}', 1127, 2, 2, '', 'iot:secret-custody:rotate', 1127, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001028, 1, 93003023, '密钥轮换复核', 'iot:secret-custody:approve', '', '', '', '{"caption":"关键密钥轮换动作双人复核"}', 1128, 2, 2, '', 'iot:secret-custody:approve', 1128, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001030, 1, 93001009, '协议治理执行', 'iot:protocol-governance:edit', '', '', '', '{"caption":"维护协议族定义与解密档案草稿"}', 1130, 2, 2, '', 'iot:protocol-governance:edit', 1130, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001031, 1, 93001009, '协议治理复核', 'iot:protocol-governance:approve', '', '', '', '{"caption":"协议治理发布与回滚双人复核"}', 1131, 2, 2, '', 'iot:protocol-governance:approve', 1131, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    permission = VALUES(permission),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;
```

- [ ] **Step 3: Verify the new page row and corrected parent bindings**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern '93001009, 1, 93000001|93001026, 1, 93003023|93001027, 1, 93003023|93001028, 1, 93003023|93001030, 1, 93001009|93001031, 1, 93001009' `
  -Encoding UTF8
```

Expected:
- 命中 6 行
- `/protocol-governance` 页面存在
- `iot:secret-custody:*` 已迁到 `93003023`
- `iot:protocol-governance:*` 已迁到 `93001009`

- [ ] **Step 4: Re-check the local seed rows without touching the existing `/device-onboarding` entry**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern '93001008, 1, 93000001|93001009, 1, 93000001|93001026, 1, 93003023|93001030, 1, 93001009|93001031, 1, 93001009' `
  -Encoding UTF8
```

Expected:
- `93001008` 这条现有 `/device-onboarding` 行仍保持存在
- 本任务只新增 `93001009` 并修正 `93001026/27/28/30/31` 的父子关系
- 不要在本任务里编辑 `93001008` 这条既有在途行

### Task 2: Backfill access-side workbench and hidden function permissions

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

- [ ] **Step 1: Confirm the access-side function permissions are still missing**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern 'iot:protocol-governance:family-draft|iot:products:workbench-overview|iot:devices:detail|iot:reporting:replay-workspace|iot:message-trace:detail' `
  -Encoding UTF8
```

Expected: 无输出

- [ ] **Step 2: Insert the protocol, product, device, reporting, and message-trace hidden permissions**

把以下块插到治理细粒度权限段之后、IoT 演示数据段之前：

```sql
INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93001032, 1, 93001009, '协议族草稿', 'iot:protocol-governance:family-draft', '', '', '', '{"caption":"维护协议族定义草稿"}', 1132, 2, 2, '', 'iot:protocol-governance:family-draft', 1132, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001033, 1, 93001009, '协议族发布', 'iot:protocol-governance:family-publish', '', '', '', '{"caption":"提交协议族发布审批"}', 1133, 2, 2, '', 'iot:protocol-governance:family-publish', 1133, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001034, 1, 93001009, '协议族回滚', 'iot:protocol-governance:family-rollback', '', '', '', '{"caption":"提交协议族回滚审批"}', 1134, 2, 2, '', 'iot:protocol-governance:family-rollback', 1134, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001035, 1, 93001009, '解密档案草稿', 'iot:protocol-governance:decrypt-draft', '', '', '', '{"caption":"维护解密档案草稿"}', 1135, 2, 2, '', 'iot:protocol-governance:decrypt-draft', 1135, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001036, 1, 93001009, '解密命中试算', 'iot:protocol-governance:decrypt-preview', '', '', '', '{"caption":"查看解密命中试算结果"}', 1136, 2, 2, '', 'iot:protocol-governance:decrypt-preview', 1136, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001037, 1, 93001009, '解密回放', 'iot:protocol-governance:decrypt-replay', '', '', '', '{"caption":"执行解密链路回放"}', 1137, 2, 2, '', 'iot:protocol-governance:decrypt-replay', 1137, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001038, 1, 93001009, '协议模板草稿', 'iot:protocol-governance:template-draft', '', '', '', '{"caption":"维护协议模板草稿"}', 1138, 2, 2, '', 'iot:protocol-governance:template-draft', 1138, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001039, 1, 93001009, '模板回放', 'iot:protocol-governance:template-replay', '', '', '', '{"caption":"执行协议模板回放"}', 1139, 2, 2, '', 'iot:protocol-governance:template-replay', 1139, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001040, 1, 93001009, '模板发布', 'iot:protocol-governance:template-publish', '', '', '', '{"caption":"发布协议模板快照"}', 1140, 2, 2, '', 'iot:protocol-governance:template-publish', 1140, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001041, 1, 93001001, '总览工作区', 'iot:products:workbench-overview', '', '', '', '{"caption":"切换产品总览工作区"}', 1141, 2, 2, '', 'iot:products:workbench-overview', 1141, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001042, 1, 93001001, '契约字段工作区', 'iot:products:workbench-models', '', '', '', '{"caption":"切换契约字段工作区"}', 1142, 2, 2, '', 'iot:products:workbench-models', 1142, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001043, 1, 93001001, '关联设备工作区', 'iot:products:workbench-devices', '', '', '', '{"caption":"切换关联设备工作区"}', 1143, 2, 2, '', 'iot:products:workbench-devices', 1143, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001044, 1, 93001001, '产品编辑工作区', 'iot:products:workbench-edit', '', '', '', '{"caption":"切换产品编辑工作区"}', 1144, 2, 2, '', 'iot:products:workbench-edit', 1144, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001045, 1, 93001001, '契约版本台账', 'iot:product-contract:ledger', '', '', '', '{"caption":"查看正式合同发布台账"}', 1145, 2, 2, '', 'iot:product-contract:ledger', 1145, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001046, 1, 93001001, '批次差异', 'iot:product-contract:diff', '', '', '', '{"caption":"查看合同批次差异"}', 1146, 2, 2, '', 'iot:product-contract:diff', 1146, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001047, 1, 93001001, '映射规则建议', 'iot:vendor-mapping-rule:suggestion', '', '', '', '{"caption":"查看厂商字段映射规则建议"}', 1147, 2, 2, '', 'iot:vendor-mapping-rule:suggestion', 1147, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001048, 1, 93001001, '映射规则台账', 'iot:vendor-mapping-rule:ledger', '', '', '', '{"caption":"查看厂商字段映射规则台账"}', 1148, 2, 2, '', 'iot:vendor-mapping-rule:ledger', 1148, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001049, 1, 93001001, '映射命中预览', 'iot:vendor-mapping-rule:preview', '', '', '', '{"caption":"预览映射规则命中结果"}', 1149, 2, 2, '', 'iot:vendor-mapping-rule:preview', 1149, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001050, 1, 93001001, '映射回放', 'iot:vendor-mapping-rule:replay', '', '', '', '{"caption":"回放映射规则治理证据"}', 1150, 2, 2, '', 'iot:vendor-mapping-rule:replay', 1150, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001051, 1, 93001002, '设备详情', 'iot:devices:detail', '', '', '', '{"caption":"打开设备详情抽屉"}', 1251, 2, 2, '', 'iot:devices:detail', 1251, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001052, 1, 93001002, '导出列设置', 'iot:devices:export-config', '', '', '', '{"caption":"维护设备导出列设置"}', 1252, 2, 2, '', 'iot:devices:export-config', 1252, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001053, 1, 93001002, '导出选中', 'iot:devices:export-selected', '', '', '', '{"caption":"导出选中的设备结果"}', 1253, 2, 2, '', 'iot:devices:export-selected', 1253, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001054, 1, 93001002, '导出当前结果', 'iot:devices:export-current', '', '', '', '{"caption":"导出当前筛选结果"}', 1254, 2, 2, '', 'iot:devices:export-current', 1254, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001055, 1, 93001002, '对象洞察跳转', 'iot:devices:insight', '', '', '', '{"caption":"从设备资产中心跳转对象洞察"}', 1255, 2, 2, '', 'iot:devices:insight', 1255, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001056, 1, 93001003, '结果复盘工作区', 'iot:reporting:replay-workspace', '', '', '', '{"caption":"切换结果复盘工作区"}', 1356, 2, 2, '', 'iot:reporting:replay-workspace', 1356, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001057, 1, 93001003, '模拟上报工作区', 'iot:reporting:simulate-workspace', '', '', '', '{"caption":"切换模拟上报工作区"}', 1357, 2, 2, '', 'iot:reporting:simulate-workspace', 1357, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001058, 1, 93001003, '最近会话工作区', 'iot:reporting:recent-workspace', '', '', '', '{"caption":"切换最近会话工作区"}', 1358, 2, 2, '', 'iot:reporting:recent-workspace', 1358, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001059, 1, 93001003, '继续链路追踪', 'iot:reporting:jump-message-trace', '', '', '', '{"caption":"从链路验证跳转到链路追踪"}', 1359, 2, 2, '', 'iot:reporting:jump-message-trace', 1359, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001060, 1, 93001003, '复制实际Payload', 'iot:reporting:copy-actual-payload', '', '', '', '{"caption":"复制实际 payload"}', 1360, 2, 2, '', 'iot:reporting:copy-actual-payload', 1360, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001061, 1, 93001003, '复制响应', 'iot:reporting:copy-response', '', '', '', '{"caption":"复制模拟响应"}', 1361, 2, 2, '', 'iot:reporting:copy-response', 1361, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001062, 1, 93001007, '链路详情', 'iot:message-trace:detail', '', '', '', '{"caption":"打开链路详情工作台"}', 1562, 2, 2, '', 'iot:message-trace:detail', 1562, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001063, 1, 93001007, '处理时间线', 'iot:message-trace:timeline', '', '', '', '{"caption":"查看处理时间线"}', 1563, 2, 2, '', 'iot:message-trace:timeline', 1563, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001064, 1, 93001007, 'Payload 对比', 'iot:message-trace:payload-comparison', '', '', '', '{"caption":"查看 payload 对比"}', 1564, 2, 2, '', 'iot:message-trace:payload-comparison', 1564, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    permission = VALUES(permission),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;
```

- [ ] **Step 3: Verify the access-side hidden permissions landed under the right parent pages**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern '93001032, 1, 93001009|93001041, 1, 93001001|93001051, 1, 93001002|93001056, 1, 93001003|93001062, 1, 93001007' `
  -Encoding UTF8
```

Expected:
- 命中 5 组父子关系
- 所有新增行都是 `type = 2`、`menu_type = 2`

- [ ] **Step 4: Review the access-side diff only**

```powershell
git diff -- 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' | Select-String -Pattern '9300103[2-9]|9300104[0-9]|9300105[0-9]|9300106[0-4]'
```

Expected: 只出现本任务新增的 access-side 权限范围

### Task 3: Backfill risk, governance, and quality workbench permissions

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

- [ ] **Step 1: Confirm the risk and governance permissions are still missing**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern 'risk:point:binding-maintain|risk:alarm:detail|risk:event:dispatch|system:organization:add|system:dict-item:export|system:governance-task:decision-context|system:governance-approval:simulation|system:business-acceptance:launch|system:automation-execution:copy-command' `
  -Encoding UTF8
```

Expected: 无输出

- [ ] **Step 2: Insert the risk-side button and hidden function permissions**

```sql
INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93002031, 1, 93002003, '新增风险点', 'risk:point:add', '', '', '', '{"caption":"新增风险点"}', 3131, 2, 2, '', 'risk:point:add', 3131, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002032, 1, 93002003, '编辑风险点', 'risk:point:update', '', '', '', '{"caption":"编辑风险点"}', 3132, 2, 2, '', 'risk:point:update', 3132, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002033, 1, 93002003, '删除风险点', 'risk:point:delete', '', '', '', '{"caption":"删除风险点"}', 3133, 2, 2, '', 'risk:point:delete', 3133, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002034, 1, 93002003, '绑定设备', 'risk:point:bind-device', '', '', '', '{"caption":"为风险点绑定设备"}', 3134, 2, 2, '', 'risk:point:bind-device', 3134, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002035, 1, 93002003, '维护绑定', 'risk:point:binding-maintain', '', '', '', '{"caption":"维护正式风险绑定"}', 3135, 2, 2, '', 'risk:point:binding-maintain', 3135, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002036, 1, 93002003, '待治理转正', 'risk:point:pending-promote', '', '', '', '{"caption":"处理待治理绑定转正"}', 3136, 2, 2, '', 'risk:point:pending-promote', 3136, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002037, 1, 93002003, '风险点详情', 'risk:point:detail', '', '', '', '{"caption":"查看风险点详情"}', 3137, 2, 2, '', 'risk:point:detail', 3137, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002038, 1, 93002003, '治理历史', 'risk:point:history', '', '', '', '{"caption":"查看风险绑定治理历史"}', 3138, 2, 2, '', 'risk:point:history', 3138, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002039, 1, 93002001, '告警详情', 'risk:alarm:detail', '', '', '', '{"caption":"查看告警详情"}', 2339, 2, 2, '', 'risk:alarm:detail', 2339, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002040, 1, 93002001, '告警确认', 'risk:alarm:confirm', '', '', '', '{"caption":"确认告警"}', 2340, 2, 2, '', 'risk:alarm:confirm', 2340, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002041, 1, 93002001, '告警抑制', 'risk:alarm:suppress', '', '', '', '{"caption":"抑制告警"}', 2341, 2, 2, '', 'risk:alarm:suppress', 2341, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002042, 1, 93002001, '告警关闭', 'risk:alarm:close', '', '', '', '{"caption":"关闭告警"}', 2342, 2, 2, '', 'risk:alarm:close', 2342, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002052, 1, 93002002, '事件详情', 'risk:event:detail', '', '', '', '{"caption":"查看事件详情"}', 2452, 2, 2, '', 'risk:event:detail', 2452, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002053, 1, 93002002, '事件派发', 'risk:event:dispatch', '', '', '', '{"caption":"派发事件工单"}', 2453, 2, 2, '', 'risk:event:dispatch', 2453, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002054, 1, 93002002, '事件关闭', 'risk:event:close', '', '', '', '{"caption":"关闭事件"}', 2454, 2, 2, '', 'risk:event:close', 2454, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    permission = VALUES(permission),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;
```

- [ ] **Step 3: Insert the governance and quality-workbench permissions**

```sql
INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93003601, 1, 93003001, '新增组织', 'system:organization:add', '', '', '', '{"caption":"新增组织节点"}', 4101, 2, 2, '', 'system:organization:add', 4101, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003602, 1, 93003001, '编辑组织', 'system:organization:update', '', '', '', '{"caption":"编辑组织节点"}', 4102, 2, 2, '', 'system:organization:update', 4102, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003603, 1, 93003001, '删除组织', 'system:organization:delete', '', '', '', '{"caption":"删除组织节点"}', 4103, 2, 2, '', 'system:organization:delete', 4103, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003604, 1, 93003001, '新增下级组织', 'system:organization:add-child', '', '', '', '{"caption":"新增下级组织节点"}', 4104, 2, 2, '', 'system:organization:add-child', 4104, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003605, 1, 93003001, '导出组织', 'system:organization:export', '', '', '', '{"caption":"导出组织树结果"}', 4105, 2, 2, '', 'system:organization:export', 4105, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003606, 1, 93003004, '新增区域', 'system:region:add', '', '', '', '{"caption":"新增区域节点"}', 4506, 2, 2, '', 'system:region:add', 4506, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003607, 1, 93003004, '编辑区域', 'system:region:update', '', '', '', '{"caption":"编辑区域节点"}', 4507, 2, 2, '', 'system:region:update', 4507, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003608, 1, 93003004, '删除区域', 'system:region:delete', '', '', '', '{"caption":"删除区域节点"}', 4508, 2, 2, '', 'system:region:delete', 4508, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003609, 1, 93003004, '新增下级区域', 'system:region:add-child', '', '', '', '{"caption":"新增下级区域节点"}', 4509, 2, 2, '', 'system:region:add-child', 4509, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003610, 1, 93003004, '导出区域', 'system:region:export', '', '', '', '{"caption":"导出区域树结果"}', 4510, 2, 2, '', 'system:region:export', 4510, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003611, 1, 93003005, '新增字典', 'system:dict:add', '', '', '', '{"caption":"新增字典类型"}', 4611, 2, 2, '', 'system:dict:add', 4611, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003612, 1, 93003005, '编辑字典', 'system:dict:update', '', '', '', '{"caption":"编辑字典类型"}', 4612, 2, 2, '', 'system:dict:update', 4612, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003613, 1, 93003005, '删除字典', 'system:dict:delete', '', '', '', '{"caption":"删除字典类型"}', 4613, 2, 2, '', 'system:dict:delete', 4613, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003614, 1, 93003005, '导出字典', 'system:dict:export', '', '', '', '{"caption":"导出字典类型结果"}', 4614, 2, 2, '', 'system:dict:export', 4614, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003615, 1, 93003005, '新增字典项', 'system:dict-item:add', '', '', '', '{"caption":"新增字典项"}', 4615, 2, 2, '', 'system:dict-item:add', 4615, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003616, 1, 93003005, '编辑字典项', 'system:dict-item:update', '', '', '', '{"caption":"编辑字典项"}', 4616, 2, 2, '', 'system:dict-item:update', 4616, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003617, 1, 93003005, '删除字典项', 'system:dict-item:delete', '', '', '', '{"caption":"删除字典项"}', 4617, 2, 2, '', 'system:dict-item:delete', 4617, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003618, 1, 93003005, '导出字典项', 'system:dict-item:export', '', '', '', '{"caption":"导出字典项结果"}', 4618, 2, 2, '', 'system:dict-item:export', 4618, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003619, 1, 93003006, '新增渠道', 'system:channel:add', '', '', '', '{"caption":"新增通知渠道"}', 4719, 2, 2, '', 'system:channel:add', 4719, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003620, 1, 93003006, '编辑渠道', 'system:channel:update', '', '', '', '{"caption":"编辑通知渠道"}', 4720, 2, 2, '', 'system:channel:update', 4720, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003621, 1, 93003006, '删除渠道', 'system:channel:delete', '', '', '', '{"caption":"删除通知渠道"}', 4721, 2, 2, '', 'system:channel:delete', 4721, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003622, 1, 93003006, '渠道测试', 'system:channel:test', '', '', '', '{"caption":"执行通知渠道测试"}', 4722, 2, 2, '', 'system:channel:test', 4722, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003623, 1, 93003006, '导出渠道', 'system:channel:export', '', '', '', '{"caption":"导出通知渠道结果"}', 4723, 2, 2, '', 'system:channel:export', 4723, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003624, 1, 93003010, '消息详情', 'system:in-app-message:detail', '', '', '', '{"caption":"查看站内消息详情"}', 4824, 2, 2, '', 'system:in-app-message:detail', 4824, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003625, 1, 93003010, '桥接统计', 'system:in-app-message:bridge-stats', '', '', '', '{"caption":"查看未读桥接统计"}', 4825, 2, 2, '', 'system:in-app-message:bridge-stats', 4825, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003626, 1, 93003010, '桥接结果', 'system:in-app-message:bridge-results', '', '', '', '{"caption":"查看未读桥接结果"}', 4826, 2, 2, '', 'system:in-app-message:bridge-results', 4826, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003627, 1, 93003010, '桥接详情', 'system:in-app-message:bridge-detail', '', '', '', '{"caption":"查看未读桥接详情"}', 4827, 2, 2, '', 'system:in-app-message:bridge-detail', 4827, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003628, 1, 93003024, '决策说明', 'system:governance-task:decision-context', '', '', '', '{"caption":"查看任务排序与决策说明"}', 5028, 2, 2, '', 'system:governance-task:decision-context', 5028, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003629, 1, 93003024, '去处理', 'system:governance-task:dispatch', '', '', '', '{"caption":"从控制面派发到领域工作台"}', 5029, 2, 2, '', 'system:governance-task:dispatch', 5029, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003630, 1, 93003024, '治理复盘', 'system:governance-task:replay', '', '', '', '{"caption":"打开治理链路复盘"}', 5030, 2, 2, '', 'system:governance-task:replay', 5030, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003631, 1, 93003024, '确认任务', 'system:governance-task:ack', '', '', '', '{"caption":"确认治理任务"}', 5031, 2, 2, '', 'system:governance-task:ack', 5031, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003632, 1, 93003024, '阻塞任务', 'system:governance-task:block', '', '', '', '{"caption":"标记治理任务阻塞"}', 5032, 2, 2, '', 'system:governance-task:block', 5032, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003633, 1, 93003024, '关闭任务', 'system:governance-task:close', '', '', '', '{"caption":"关闭治理任务"}', 5033, 2, 2, '', 'system:governance-task:close', 5033, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003634, 1, 93003024, '提交复盘结论', 'system:governance-task:replay-feedback', '', '', '', '{"caption":"提交治理复盘结论"}', 5034, 2, 2, '', 'system:governance-task:replay-feedback', 5034, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003635, 1, 93003025, '告警复盘', 'system:governance-ops:replay', '', '', '', '{"caption":"打开治理运维复盘"}', 5135, 2, 2, '', 'system:governance-ops:replay', 5135, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003636, 1, 93003025, '确认告警', 'system:governance-ops:ack', '', '', '', '{"caption":"确认治理运维告警"}', 5136, 2, 2, '', 'system:governance-ops:ack', 5136, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003637, 1, 93003025, '抑制告警', 'system:governance-ops:suppress', '', '', '', '{"caption":"抑制治理运维告警"}', 5137, 2, 2, '', 'system:governance-ops:suppress', 5137, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003638, 1, 93003025, '关闭告警', 'system:governance-ops:close', '', '', '', '{"caption":"关闭治理运维告警"}', 5138, 2, 2, '', 'system:governance-ops:close', 5138, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003639, 1, 93003025, '提交复盘结论', 'system:governance-ops:replay-feedback', '', '', '', '{"caption":"提交治理运维复盘结论"}', 5139, 2, 2, '', 'system:governance-ops:replay-feedback', 5139, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003640, 1, 93003022, '审批详情', 'system:governance-approval:detail', '', '', '', '{"caption":"查看审批详情"}', 5240, 2, 2, '', 'system:governance-approval:detail', 5240, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003641, 1, 93003022, '审批通过', 'system:governance-approval:approve', '', '', '', '{"caption":"审批通过治理主单"}', 5241, 2, 2, '', 'system:governance-approval:approve', 5241, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003642, 1, 93003022, '审批驳回', 'system:governance-approval:reject', '', '', '', '{"caption":"审批驳回治理主单"}', 5242, 2, 2, '', 'system:governance-approval:reject', 5242, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003643, 1, 93003022, '撤销审批', 'system:governance-approval:cancel', '', '', '', '{"caption":"撤销治理审批主单"}', 5243, 2, 2, '', 'system:governance-approval:cancel', 5243, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003644, 1, 93003022, '原单重提', 'system:governance-approval:resubmit', '', '', '', '{"caption":"按原单重新提交审批"}', 5244, 2, 2, '', 'system:governance-approval:resubmit', 5244, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003645, 1, 93003022, '审批预演', 'system:governance-approval:simulation', '', '', '', '{"caption":"查看审批预演结果"}', 5245, 2, 2, '', 'system:governance-approval:simulation', 5245, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003646, 1, 93003022, '影响分析', 'system:governance-approval:impact', '', '', '', '{"caption":"查看审批影响分析"}', 5246, 2, 2, '', 'system:governance-approval:impact', 5246, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003701, 1, 93003020, '发起验收', 'system:business-acceptance:launch', '', '', '', '{"caption":"发起业务验收"}', 5701, 2, 2, '', 'system:business-acceptance:launch', 5701, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003702, 1, 93003020, '打开结果', 'system:business-acceptance:open-result', '', '', '', '{"caption":"打开最近一次验收结果"}', 5702, 2, 2, '', 'system:business-acceptance:open-result', 5702, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003703, 1, 93003016, '刷新盘点', 'system:rd-automation-inventory:refresh', '', '', '', '{"caption":"刷新页面盘点结果"}', 5303, 2, 2, '', 'system:rd-automation-inventory:refresh', 5303, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003704, 1, 93003016, '勾选未覆盖', 'system:rd-automation-inventory:select-uncovered', '', '', '', '{"caption":"勾选未覆盖页面"}', 5304, 2, 2, '', 'system:rd-automation-inventory:select-uncovered', 5304, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003705, 1, 93003016, '生成脚手架', 'system:rd-automation-inventory:generate-scaffold', '', '', '', '{"caption":"一键生成自动化脚手架"}', 5305, 2, 2, '', 'system:rd-automation-inventory:generate-scaffold', 5305, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003706, 1, 93003017, '新增页面冒烟模板', 'system:rd-automation-templates:add-page-smoke', '', '', '', '{"caption":"新增页面冒烟模板"}', 5406, 2, 2, '', 'system:rd-automation-templates:add-page-smoke', 5406, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003707, 1, 93003017, '新增表单提交模板', 'system:rd-automation-templates:add-form-submit', '', '', '', '{"caption":"新增表单提交模板"}', 5407, 2, 2, '', 'system:rd-automation-templates:add-form-submit', 5407, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003708, 1, 93003017, '新增列表详情模板', 'system:rd-automation-templates:add-list-detail', '', '', '', '{"caption":"新增列表详情模板"}', 5408, 2, 2, '', 'system:rd-automation-templates:add-list-detail', 5408, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003709, 1, 93003018, '导入计划', 'system:rd-automation-plans:import', '', '', '', '{"caption":"导入自动化计划"}', 5509, 2, 2, '', 'system:rd-automation-plans:import', 5509, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003710, 1, 93003018, '导出计划', 'system:rd-automation-plans:export', '', '', '', '{"caption":"导出自动化计划 JSON"}', 5510, 2, 2, '', 'system:rd-automation-plans:export', 5510, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003711, 1, 93003018, '重置计划', 'system:rd-automation-plans:reset', '', '', '', '{"caption":"恢复默认自动化计划"}', 5511, 2, 2, '', 'system:rd-automation-plans:reset', 5511, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003712, 1, 93003019, '复制命令', 'system:rd-automation-handoff:copy-command', '', '', '', '{"caption":"复制交付命令"}', 5612, 2, 2, '', 'system:rd-automation-handoff:copy-command', 5612, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003713, 1, 93003019, '导出计划文件', 'system:rd-automation-handoff:export-plan', '', '', '', '{"caption":"导出交付计划"}', 5613, 2, 2, '', 'system:rd-automation-handoff:export-plan', 5613, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003714, 1, 93003013, '复制执行命令', 'system:automation-execution:copy-command', '', '', '', '{"caption":"复制执行中心命令"}', 5714, 2, 2, '', 'system:automation-execution:copy-command', 5714, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    permission = VALUES(permission),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;
```

- [ ] **Step 4: Verify the risk/governance/quality rows are present under the intended parent pages**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern '93002035, 1, 93002003|93002040, 1, 93002001|93002053, 1, 93002002|93003601, 1, 93003001|93003624, 1, 93003010|93003628, 1, 93003024|93003635, 1, 93003025|93003640, 1, 93003022|93003701, 1, 93003020|93003714, 1, 93003013' `
  -Encoding UTF8
```

Expected: 命中 10 组父子关系

### Task 4: Add parent-page role inheritance for the new page and hidden permissions

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

- [ ] **Step 1: Confirm the current seed only hand-wires governance fine-grained permissions**

```powershell
Get-Content -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' -Encoding UTF8 | Select-Object -Skip 450 -First 70
```

Expected:
- 能看到 `SET @extra_role_menu_id := 96010950;`
- 只能看到手写 `UNION ALL SELECT ...` 的治理细粒度权限绑定
- 看不到针对本轮新增 page/button/function 权限的继承块

- [ ] **Step 2: Add one generic `sys_role_menu` inheritance block for the new IDs**

把下面 SQL 放到治理权限 `@extra_role_menu_id` 块之后、IoT 演示数据之前：

```sql
SET @menu_permission_role_menu_id := 96011000;
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@menu_permission_role_menu_id := @menu_permission_role_menu_id + 1), 1, parent_rm.role_id, child.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu child
JOIN sys_menu parent
  ON parent.id = child.parent_id
 AND parent.tenant_id = 1
 AND parent.deleted = 0
JOIN sys_role_menu parent_rm
  ON parent_rm.menu_id = parent.id
 AND parent_rm.tenant_id = 1
 AND parent_rm.deleted = 0
WHERE child.tenant_id = 1
  AND child.deleted = 0
  AND child.id IN (
      93001009,
      93001032, 93001033, 93001034, 93001035, 93001036, 93001037, 93001038, 93001039, 93001040,
      93001041, 93001042, 93001043, 93001044, 93001045, 93001046, 93001047, 93001048, 93001049, 93001050,
      93001051, 93001052, 93001053, 93001054, 93001055,
      93001056, 93001057, 93001058, 93001059, 93001060, 93001061,
      93001062, 93001063, 93001064,
      93002031, 93002032, 93002033, 93002034, 93002035, 93002036, 93002037, 93002038,
      93002039, 93002040, 93002041, 93002042,
      93002052, 93002053, 93002054,
      93003601, 93003602, 93003603, 93003604, 93003605,
      93003606, 93003607, 93003608, 93003609, 93003610,
      93003611, 93003612, 93003613, 93003614, 93003615, 93003616, 93003617, 93003618,
      93003619, 93003620, 93003621, 93003622, 93003623,
      93003624, 93003625, 93003626, 93003627,
      93003628, 93003629, 93003630, 93003631, 93003632, 93003633, 93003634,
      93003635, 93003636, 93003637, 93003638, 93003639,
      93003640, 93003641, 93003642, 93003643, 93003644, 93003645, 93003646,
      93003701, 93003702, 93003703, 93003704, 93003705, 93003706, 93003707, 93003708,
      93003709, 93003710, 93003711, 93003712, 93003713, 93003714
  )
  AND parent_rm.role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existed
      WHERE existed.tenant_id = 1
        AND existed.role_id = parent_rm.role_id
        AND existed.menu_id = child.id
        AND existed.deleted = 0
  );
```

- [ ] **Step 3: Verify the inheritance block is present and anchored before the IoT demo data section**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' `
  -Pattern '@menu_permission_role_menu_id := 96011000|93001009,|93003714|-- 2\\) IoT 产品/设备/消息基线' `
  -Encoding UTF8
```

Expected:
- 命中 `@menu_permission_role_menu_id := 96011000`
- 命中 `93001009` 和 `93003714`
- 继承块位于 `-- 2) IoT 产品/设备/消息基线` 之前

### Task 5: Update the business and database docs in place

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Review: `E:\idea\ghatg\spring-boot-iot\README.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: Add the menu-baseline update to `docs/02-业务功能与流程说明.md`**

在“当前菜单结构与角色化工作台基线”附近补入下面两条：

```md
- `2026-04-18` 起，菜单权限种子继续按“最小迁移”补齐数据库真相：`/protocol-governance` 已升格为正式页面菜单，`/products`、`/devices`、`/reporting`、`/message-trace`、`/risk-point`、`/governance-task`、`/governance-ops`、`/governance-approval`、`/in-app-message` 以及质量工场执行/编排页的关键工作区、按钮与重功能点，现统一以下沉权限项进入 `sys_menu / sys_role_menu`。
- 同日起，本轮仍明确排除总览页、兼容入口、规划页和试验页新增页面化落库：`/device-access`、`/risk-disposal`、`/risk-config`、`/system-management`、`/quality-workbench`、`/automation-assets`、`/automation-test`、`/future-lab`、`/risk-enhance`、`/device-onboarding` 继续不作为本轮新增页面菜单。
```

- [ ] **Step 2: Add the seed-baseline update to `docs/04-数据库设计与初始化数据.md`**

在“工作台菜单基线”附近补入下面三条：

```md
- `2026-04-18` 起，`sql/init-data.sql` 正式补齐 `93001009 / iot:protocol-governance / /protocol-governance` 页面菜单；原先挂在 `产品定义中心` 下的 `iot:protocol-governance:edit / approve` 已迁回该页，`iot:secret-custody:view / rotate / approve` 已迁回 `权限与密钥治理` 页面。
- 同日起，`sql/init-data.sql` 继续为当前真实业务页补齐隐藏权限节点：接入侧新增 `/protocol-governance`、`/products`、`/devices`、`/reporting`、`/message-trace` 的工作区/回放/详情权限，风险与平台治理侧新增 `risk-point / alarm-center / event-disposal / organization / region / dict / channel / in-app-message / governance-task / governance-ops / governance-approval / business-acceptance / rd-automation-* / automation-execution` 等按钮与功能点。
- 上述新增节点仍使用 `sys_menu.type=2 / menu_type=2` 表达隐藏按钮或内嵌工作区，不新增三级菜单；对应 `sys_role_menu` 绑定按父页面现有角色自动补齐，不再为每个角色手写一套重复矩阵。
```

- [ ] **Step 3: Add the top changelog entry to `docs/08-变更记录与技术债清单.md`**

把下面条目插到 `## 1. 当前有效变更摘要` 下方靠前位置：

```md
- 2026-04-18：菜单权限种子本轮继续按“最小迁移”补齐数据库真相，不改前端导航/路由或后端鉴权逻辑。`sql/init-data.sql` 将新增 `93001009 / iot:protocol-governance` 页面菜单，并为 `/products`、`/devices`、`/reporting`、`/message-trace`、`/risk-point`、`/governance-task`、`/governance-ops`、`/governance-approval`、`/in-app-message`、组织/区域/字典/通知以及质量工场执行页补齐按钮和隐藏功能点；`iot:protocol-governance:*` 会迁回 `/protocol-governance`，`iot:secret-custody:*` 会迁回 `/governance-security`，新增 `sys_role_menu` 绑定统一按父页面现有角色继承。`/device-onboarding`、总览页、兼容入口与规划页继续排除，不作为本轮新增页面菜单。
```

- [ ] **Step 4: Verify the doc snippets landed, and keep `README.md` / `AGENTS.md` as review-only**

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md','E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md','E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md' `
  -Pattern '2026-04-18|/protocol-governance|最小迁移|父页面现有角色继承' `
  -Encoding UTF8
```

Expected:
- 三个文档都能命中 `2026-04-18`
- `README.md`、`AGENTS.md` 保持 review-only，无需本轮改动

### Task 6: Run static verification and prepare a clean delivery note

**Files:**
- Verify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`
- Verify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Verify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Verify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

- [ ] **Step 1: Run a one-shot menu code uniqueness check for every new code in this plan**

```powershell
$path = 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql'
$text = Get-Content -Path $path -Raw -Encoding UTF8
$codes = @(
  'iot:protocol-governance',
  'iot:protocol-governance:family-draft','iot:protocol-governance:family-publish','iot:protocol-governance:family-rollback',
  'iot:protocol-governance:decrypt-draft','iot:protocol-governance:decrypt-preview','iot:protocol-governance:decrypt-replay',
  'iot:protocol-governance:template-draft','iot:protocol-governance:template-replay','iot:protocol-governance:template-publish',
  'iot:products:workbench-overview','iot:products:workbench-models','iot:products:workbench-devices','iot:products:workbench-edit',
  'iot:product-contract:ledger','iot:product-contract:diff',
  'iot:vendor-mapping-rule:suggestion','iot:vendor-mapping-rule:ledger','iot:vendor-mapping-rule:preview','iot:vendor-mapping-rule:replay',
  'iot:devices:detail','iot:devices:export-config','iot:devices:export-selected','iot:devices:export-current','iot:devices:insight',
  'iot:reporting:replay-workspace','iot:reporting:simulate-workspace','iot:reporting:recent-workspace','iot:reporting:jump-message-trace','iot:reporting:copy-actual-payload','iot:reporting:copy-response',
  'iot:message-trace:detail','iot:message-trace:timeline','iot:message-trace:payload-comparison',
  'risk:point:add','risk:point:update','risk:point:delete','risk:point:bind-device','risk:point:binding-maintain','risk:point:pending-promote','risk:point:detail','risk:point:history',
  'risk:alarm:detail','risk:alarm:confirm','risk:alarm:suppress','risk:alarm:close',
  'risk:event:detail','risk:event:dispatch','risk:event:close',
  'system:organization:add','system:organization:update','system:organization:delete','system:organization:add-child','system:organization:export',
  'system:region:add','system:region:update','system:region:delete','system:region:add-child','system:region:export',
  'system:dict:add','system:dict:update','system:dict:delete','system:dict:export',
  'system:dict-item:add','system:dict-item:update','system:dict-item:delete','system:dict-item:export',
  'system:channel:add','system:channel:update','system:channel:delete','system:channel:test','system:channel:export',
  'system:in-app-message:detail','system:in-app-message:bridge-stats','system:in-app-message:bridge-results','system:in-app-message:bridge-detail',
  'system:governance-task:decision-context','system:governance-task:dispatch','system:governance-task:replay','system:governance-task:ack','system:governance-task:block','system:governance-task:close','system:governance-task:replay-feedback',
  'system:governance-ops:replay','system:governance-ops:ack','system:governance-ops:suppress','system:governance-ops:close','system:governance-ops:replay-feedback',
  'system:governance-approval:detail','system:governance-approval:approve','system:governance-approval:reject','system:governance-approval:cancel','system:governance-approval:resubmit','system:governance-approval:simulation','system:governance-approval:impact',
  'system:business-acceptance:launch','system:business-acceptance:open-result',
  'system:rd-automation-inventory:refresh','system:rd-automation-inventory:select-uncovered','system:rd-automation-inventory:generate-scaffold',
  'system:rd-automation-templates:add-page-smoke','system:rd-automation-templates:add-form-submit','system:rd-automation-templates:add-list-detail',
  'system:rd-automation-plans:import','system:rd-automation-plans:export','system:rd-automation-plans:reset',
  'system:rd-automation-handoff:copy-command','system:rd-automation-handoff:export-plan',
  'system:automation-execution:copy-command'
)
$bad = foreach ($code in $codes) {
  $count = ([regex]::Matches($text, [regex]::Escape("'$code'"))).Count
  if ($count -ne 2) { "$code=$count" }
}
if ($bad.Count -gt 0) { throw "menu_code count mismatch: $($bad -join ', ')" }
'seed row code counts OK'
```

Expected: 输出 `seed row code counts OK`

- [ ] **Step 2: Check the planned exclusions and parent corrections did not drift**

```powershell
$path = 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql'
Select-String -Path $path -Pattern '93001009, 1, 93000001|93001026, 1, 93003023|93001030, 1, 93001009|93001031, 1, 93001009' -Encoding UTF8
Select-String -Path $path -Pattern '/device-access|/risk-disposal|/risk-config|/system-management|/quality-workbench|/automation-assets|/automation-test|/future-lab|/risk-enhance|/device-onboarding' -Encoding UTF8
```

Expected:
- 第一条命令命中新的页面行和修正后的父子关系
- 第二条命令只会命中既有说明或既有兼容入口，不会出现本轮新增的页面化 seed

- [ ] **Step 3: Run whitespace and syntax-adjacent checks on the edited files**

```powershell
git diff --check -- 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' 'E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md' 'E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md' 'E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md'
```

Expected: 无输出

- [ ] **Step 4: Prepare the delivery note without creating a mixed commit**

交付说明必须明确写出：

```md
1. 本轮只补齐 `sys_menu / sys_role_menu` 与文档，不改前端路由、页面结构和后端权限逻辑。
2. `/protocol-governance` 已成为正式页面菜单；`iot:protocol-governance:*` 已迁回该页，`iot:secret-custody:*` 已迁回 `权限与密钥治理`。
3. 新增 page/button/function 权限统一按父页面现有角色继承，未额外手写超管矩阵。
4. 已完成静态校验：目标菜单存在、父子关系正确、menu_code 唯一、`git diff --check` 通过。
5. 未完成运行态验证：本轮未连接真实库执行 seed，也未改前端消费逻辑。
6. 当前工作区已有与 `/device-onboarding` 相关的在途脏改动，不能把本任务与那些改动混成同一次提交。
```

Expected: 最终说明能准确区分“已静态验证”与“未实库执行”的边界
