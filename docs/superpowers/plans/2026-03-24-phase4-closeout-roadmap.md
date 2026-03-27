# Phase 4 收口与下一阶段排期 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 先完成 Phase 4 正式收口，再以最小风险开启 Phase 5 的设备中心与质量门禁增强。

**Architecture:** 本计划遵循“真实环境优先、文档边界同步、工程化渐进补齐”的推进方式。顺序固定为：先解风险监测/GIS 的真实环境阻塞并完成验收，再同步 `19 / 21 / README` 交付口径，随后处理 `iot_message_log` 命名治理、设备中心下一子项和质量门禁基线，避免功能面继续扩散而交付边界继续模糊。

**Tech Stack:** Spring Boot 4, Java 17, Vue 3, MySQL, Redis, MQTT, TDengine, Maven, Vitest, PowerShell

---

## 任务摘要

### 推荐顺序

| 优先级 | 建议时长 | 责任建议 | 目标 | 完成标志 |
|---|---|---|---|---|
| P0 | 3-5 天 | 后端 + 前端 + 测试 | 风险监测 / GIS 从“代码已落地”推进到“真实环境复验完成” | `/api/risk-monitoring/*` 非 SQL 500，页面联调通过，文档状态更新 |
| P0 | 1-2 天 | 后端 / 文档 | 收口 Phase 4 交付边界 | `19 / 21 / README` 对交付范围表述一致 |
| P1 | 2-4 天 | 后端 + 文档 | 收口 `iot_message_log` 命名主口径 | 新文档和新验收口径统一使用 `iot_message_log` |
| P1 | 3-5 天 | 产品 / 后端 / 前端 | 冻结设备中心下一子项，优先推荐“产品物模型设计器” | 下一轮只开一个设备中心增强口子并形成单独任务卡 |
| P2 | 2-4 天 | 前端 + 后端 + 测试 | 固化质量门禁最小基线 | 形成统一本地质量门禁脚本并回写 `05 / 08` |

### 受影响模块

- `spring-boot-iot-alarm`
- `spring-boot-iot-ui`
- `spring-boot-iot-device`
- `spring-boot-iot-message`
- `spring-boot-iot-protocol`
- `spring-boot-iot-telemetry`
- `spring-boot-iot-system`
- `docs/`
- `sql/upgrade/`
- `scripts/`

### 假设

1. 真实环境仍以 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 为唯一验收基线。
2. 当前目标不是新增大功能面，而是优先收口已存在但未正式完成交付的能力。
3. 若共享环境 schema、MQTT、Redis 或 TDengine 不可用，应记为环境阻塞，不允许回退到已废弃 H2 路线。

**2026-03-25 回填说明：** Task 2~5 已根据当前主干代码、`docs/19` / `docs/21` / `README.md` 口径及本会话 fresh 验证结果回填；各 Task 的 `提交` 步骤仍保留未勾选，因为当前 worktree 尚未做最终提交。Task 1 已在本会话补做 schema 脚本覆盖核对、真实库 schema 直查与 live API 复验；共享 dev 环境当前不再需要重复执行 schema sync，但页面手工巡检本会话未重新操作，因此只回填已自动化复核的步骤。

## Task 1: 风险监测 / GIS 解阻并完成真实环境复验

**Files:**
- Inspect: `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql`
- Inspect: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskMonitoringController.java`
- Inspect: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMonitoringServiceImpl.java`
- Inspect: `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- Inspect: `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
- Inspect: `spring-boot-iot-ui/src/views/RiskGisView.vue`
- Inspect: `spring-boot-iot-ui/src/components/RiskMonitoringDetailDrawer.vue`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/真实环境测试与验收手册.md`

- [x] **Step 1: 先确认 schema 对齐脚本覆盖阻塞项**

Run:

```bash
rg -n "risk_point_device|create_by|update_by" sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql
```

Expected:
- 命中 `risk_point_device`
- 命中 `create_by`
- 命中 `update_by`

2026-03-25 复核结果：
- `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql` 已确认同时命中 `risk_point_device`、`create_by`、`update_by`。

- [ ] **Step 2: 在真实环境执行 schema sync**

Run:

```bash
mysql -h <host> -u <user> -p<password> rm_iot < sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql
```

Expected:
- 无 SQL 报错
- 共享环境不再缺 `risk_point_device`
- `risk_point` 对齐字段补齐

2026-03-25 当前状态说明：
- 本会话未重复执行该脚本到共享 dev 库，因为直连 `information_schema` 已确认当前 `rm_iot` 里 `risk_point` / `risk_point_device` 已具备 `create_by`、`update_by`，且 `risk_point_device` 当前已有 `28` 条记录。
- 该步骤现已从“当前环境阻塞项”降级为“历史库 / 旧环境首次联调前置脚本”；若切到未对齐环境，仍需显式执行。

- [x] **Step 3: 先做后端接口复验，再做前端联调**

Run:

```bash
curl -X POST http://127.0.0.1:9999/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

Then verify:

```bash
curl -H "Authorization: Bearer <token>" http://127.0.0.1:9999/api/risk-monitoring/realtime/list
curl -H "Authorization: Bearer <token>" http://127.0.0.1:9999/api/risk-monitoring/realtime/<bindingId>
curl -H "Authorization: Bearer <token>" http://127.0.0.1:9999/api/risk-monitoring/gis/points
```

Expected:
- 不再返回 SQL 500
- 实时监测列表可返回业务数据或明确空态
- 详情接口可返回趋势、最近告警、最近事件
- GIS 点位接口可返回点位或明确空态

2026-03-25 复核结果：
- `POST /api/auth/login` 返回 `200`，可正常取得 Bearer token。
- `GET /api/risk-monitoring/realtime/list?pageNum=1&pageSize=10` 返回 `200`，当前 total=`16`。
- `GET /api/risk-monitoring/realtime/8110` 返回 `200`，详情包含 `trendPoints`、`recentAlarms`、`recentEvents` 等字段。
- `GET /api/risk-monitoring/gis/points` 返回 `200`，当前点位数量=`28`。
- `2026-03-27` 再复核时，发现本地 `127.0.0.1:9999` 已无监听进程；随后按 `scripts/start-backend-acceptance.ps1` 基于当前 worktree 重新拉起 `dev` 实例，`/actuator/health` 返回 `UP`，且风险监测三组接口再次返回 `200 / total=16 / gis=28`。

- [ ] **Step 4: 前端按真实环境复验页面**

Run:

```bash
cd spring-boot-iot-ui
npm run build
```

Manual verification:
- `/risk-monitoring` 列表、筛选、详情抽屉可用
- `/risk-monitoring-gis` 点位渲染、未定位列表、详情抽屉可用
- 继续保持“两条独立路由 + 统一详情抽屉”口径

2026-03-25 当前状态说明：
- `spring-boot-iot-ui` 已执行 `npm run build` 并通过，`RealTimeMonitoringView` / `RiskGisView` 产物正常进入构建输出。
- 本会话未重新打开浏览器手工巡检页面，因此手工联调证据继续以 `docs/19-第四阶段交付边界与复验进展.md` 中 2026-03-24 的真实环境记录为准。

- [ ] **Step 5: 回写交付边界文档并提交**

Update:
- `docs/19-第四阶段交付边界与复验进展.md`
- `docs/21-业务功能清单与验收标准.md`
- `docs/真实环境测试与验收手册.md`

Commit:

```bash
git add sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md docs/真实环境测试与验收手册.md spring-boot-iot-alarm spring-boot-iot-ui
git commit -m "feat: close out risk monitoring real-environment acceptance"
```

## Task 2: 收口 Phase 4 交付边界与 README 入口

**Files:**
- Modify: `README.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Inspect: `AGENTS.md`
- Test: `scripts/docs/check-topology.mjs`

- [x] **Step 1: 以 `19` 为唯一权威核对当前边界**

Checklist:
- 风险监测 / GIS 是否已通过真实环境复验
- 首页驾驶舱是否继续维持“不纳入真实环境验收”
- 设备中心增强是否仍保持“未开始 / 待后续”

- [x] **Step 2: 同步更新交付矩阵和总入口**

Update:
- `README.md` 当前交付基线
- `docs/21-业务功能清单与验收标准.md` 能力矩阵
- `docs/02-业务功能与流程说明.md` 灰度或正式纳入口径

- [x] **Step 3: 运行文档拓扑校验**

Run:

```bash
node scripts/docs/check-topology.mjs
```

Expected:
- 无坏链
- 无旧兼容页回流
- `README.md`、`docs/README.md`、`AGENTS.md` 口径不冲突

- [x] **Step 4: 只在行为口径变化时同步 `AGENTS.md`**

Rule:
- 若只是阶段状态变化，不强制改 `AGENTS.md`
- 若交付边界、最小阅读集或任务入口变化，再同步更新

- [ ] **Step 5: 提交**

```bash
git add README.md docs/02-业务功能与流程说明.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md scripts/docs/check-topology.mjs AGENTS.md
git commit -m "docs: align phase 4 delivery boundary"
```

## Task 3: 收口 `iot_message_log` 命名治理

**Files:**
- Inspect: `sql/upgrade/20260316_iot_message_log_view.sql`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/DeviceMessageLogMapper.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceMessageLog.java`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [x] **Step 1: 盘点双命名现状**

Run:

```bash
rg -n "iot_device_message_log|iot_message_log" README.md docs spring-boot-iot-device sql
```

Expected:
- 能区分“物理表名”与“主命名口径”
- 找出仍会误导后续开发的旧表名表述

- [x] **Step 2: 固定治理规则**

Rule:
- 对外文档、验收口径、页面说明统一使用 `iot_message_log`
- 物理实现仍保留 `iot_device_message_log` 时，必须显式说明“兼容视图关系”
- 不在新功能文档中继续扩散旧物理表名

- [x] **Step 3: 更新数据库与接口文档**

Update:
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/21-业务功能清单与验收标准.md`

- [x] **Step 4: 保留兼容 SQL 入口**

Verify:

```bash
sed -n '1,120p' sql/upgrade/20260316_iot_message_log_view.sql
```

Expected:
- 仍通过视图暴露 `iot_message_log`
- 不破坏 Phase 1~3 已验证主链路

- [ ] **Step 5: 提交**

```bash
git add sql/upgrade/20260316_iot_message_log_view.sql docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: standardize iot message log naming"
```

## Task 4: 冻结设备中心下一子项，优先推荐“产品物模型设计器”

**Files:**
- Inspect: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Inspect: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductController.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceController.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductModelMapper.java`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [x] **Step 1: 明确只开一个设备中心增强口子**

Candidates:
- 产品物模型设计器
- 上下架审批
- 远程维护
- 维修工单联动

Decision:
- 默认优先“产品物模型设计器”
- 原因：它对风险监测字段治理、协议契约演进和设备资产主数据最基础

- [x] **Step 2: 核对现有产品 / 设备中心边界是否足够承接**

Check:
- 产品中心是否已具备稳定台账和详情增强
- 设备中心是否已具备稳定资产闭环
- 当前缺口是否集中在“物模型设计与治理”，而不是先做更重的运维闭环

- [x] **Step 3: 形成单独任务卡，不与 Phase 4 收口混做**

Output:
- 一份单独的产品物模型设计器任务卡
- 明确不与风险监测/GIS、命名治理、质量门禁并行混改

- [x] **Step 4: 只在确认选型后回写文档**

Update:
- `docs/02-业务功能与流程说明.md`
- `docs/19-第四阶段交付边界与复验进展.md`
- `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 5: 提交**

```bash
git add docs/02-业务功能与流程说明.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductModelMapper.java
git commit -m "plan: freeze next device center enhancement lane"
```

## Task 5: 固化最小质量门禁基线

**Files:**
- Inspect: `spring-boot-iot-ui/package.json`
- Inspect: `scripts/start-backend-acceptance.ps1`
- Inspect: `scripts/start-frontend-acceptance.ps1`
- Inspect: `scripts/run-message-flow-acceptance.py`
- Inspect: `scripts/docs/check-topology.mjs`
- Create: `scripts/run-quality-gates.ps1`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [x] **Step 1: 固定当前已存在的门禁命令**

Current commands:

```bash
mvn clean package -DskipTests
cd spring-boot-iot-ui && npm run build
cd spring-boot-iot-ui && npm run component:guard
cd spring-boot-iot-ui && npm run list:guard
cd spring-boot-iot-ui && npm run style:guard
node scripts/docs/check-topology.mjs
```

Expected:
- 先把“已有命令”收口为统一最小门禁
- 暂不强行引入 ESLint / Stylelint / CI 平台

- [x] **Step 2: 新增统一质量门禁脚本**

Create:
- `scripts/run-quality-gates.ps1`

Script responsibility:
- 顺序执行 Maven 构建、前端 build、前端 guard、文档拓扑校验
- 任何一步失败即返回非零退出码

- [x] **Step 3: 明确真实环境验收仍是最终判定**

Run:

```bash
powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1
powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1
python scripts/run-message-flow-acceptance.py --expired-trace-id <已过期TraceId>
```

Expected:
- 本地质量门禁通过
- 真实环境验收仍作为最终通过标准

- [x] **Step 4: 回写测试与技术债文档**

Update:
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`

Clarify:
- 当前已形成统一本地质量门禁脚本
- `DOC-Q-005` 是否仍然开放
- `DOC-Q-004` 的浏览器自动化阻断等级是否需要继续待确认

- [ ] **Step 5: 提交**

```bash
git add scripts/run-quality-gates.ps1 docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md spring-boot-iot-ui/package.json scripts/start-backend-acceptance.ps1 scripts/start-frontend-acceptance.ps1 scripts/run-message-flow-acceptance.py scripts/docs/check-topology.mjs
git commit -m "chore: establish local quality gate baseline"
```

## 完成标准

1. 风险监测 / GIS 从“代码完成待复验”转为“真实环境已完成复验”或明确记录环境阻塞。
2. `README.md`、`docs/19-第四阶段交付边界与复验进展.md`、`docs/21-业务功能清单与验收标准.md` 对交付边界保持一致。
3. `iot_message_log` 成为文档与验收主口径，兼容关系有清晰说明。
4. 设备中心下一子项只冻结一个实施方向，不再并行打开多个新口子。
5. 形成统一本地质量门禁脚本，但系统最终通过结论仍以真实环境验收为准。

## 暂不建议现在做的事

1. 不在 Phase 4 未收口前继续扩展 GIS SDK、底图、热力图或驾驶舱内嵌风险监测。
2. 不把通知中心扩展成新的多渠道产品。
3. 不在 CI、ESLint、Stylelint、Secret Scan 都未定型前，一次性引入多套新门禁并行改造。
