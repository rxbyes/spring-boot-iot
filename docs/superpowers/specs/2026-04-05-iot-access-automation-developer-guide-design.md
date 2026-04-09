# 接入智维自动化接入手册设计

> 日期：2026-04-05
> 范围：`docs/05-自动化测试与质量保障.md`、`docs/21-业务功能清单与验收标准.md`
> 主题：把“接入智维”自动化专项沉淀为一份面向开发人员的接入手册，并支持后续新增入口复用
> 状态：设计已确认，待写入正式文档

## 1. 背景

当前仓库已经完成“接入智维”自动化专项首轮落地，核心资产包括：

1. `config/automation/iot-access-web-smoke-plan.json`
2. `config/automation/acceptance-registry.json`
3. `scripts/auto/run-acceptance-registry.mjs`
4. `scripts/run-browser-acceptance.test.mjs`
5. `scripts/run-acceptance-registry.test.mjs`

同时，当前专项范围已明确收口到 6 个现有入口：

1. `/products`
2. `/devices`
3. `/reporting`
4. `/system-log`
5. `/message-trace`
6. `/file-debug`

但现状仍缺一份面向开发人员的正式接入手册。研发虽然能看到现有 plan、registry 和测试文件，却缺少一份统一说明，明确回答以下问题：

1. 新增一个“接入智维”入口时，应该补哪种自动化骨架。
2. 什么时候应该扩 `browserPlan`，什么时候应该扩 `apiSmoke` 或 `messageFlow`。
3. 新入口是否需要新增 plan 文件或新增 registry scenario。
4. 最低必须补哪些测试与验证命令，才算完成自动化接入。

因此，本次设计不是再实现一套新功能，而是把已落地能力整理为一份权威的研发接入手册，并要求后续新增“接入智维”入口能够继续复用同一规范。

## 2. 目标

1. 在现有文档体系内新增“接入智维自动化接入指南”，不创建平行说明文档。
2. 让开发人员看完文档后，至少可以独立完成“新增一个入口并补齐自动化骨架”。
3. 文档同时覆盖当前 6 个入口的现状说明，以及后续新增入口的通用接入规范。
4. 明确接入智维新增入口的自动化准入门槛、命名规则、接入步骤和验证命令。

## 3. 非目标

1. 本次不扩展新的自动化 runner 类型。
2. 本次不把接入手册改写成业务验收手册。
3. 本次不要求新增入口一开始就具备全量页面、全按钮、全异常分支自动化。
4. 本次不改变当前“只覆盖 6 个现有入口”的专项落地范围。

## 4. 方案对比

### 4.1 方案 A：在 `docs/05` 新增独立接入章节

做法：

1. 在 `docs/05-自动化测试与质量保障.md` 中新增“接入智维自动化接入指南”章节。
2. 把接入判断、骨架规则、操作步骤、示例映射、提交门槛集中写在同一处。
3. 在 `docs/21-业务功能清单与验收标准.md` 只补一条引用关系，说明接入智维新增入口自动化准入以 `docs/05` 为准。

优势：

1. 研发接入时只需看一份权威文档。
2. 与 `docs/05` 当前“测试策略、测试资产、质量门禁”的定位完全一致。
3. 能把现有专项结果和后续扩展规则统一沉淀到同一处。

代价：

1. `docs/05` 会变长。

### 4.2 方案 B：把接入步骤写进真实环境验收手册

问题：

1. 会把“自动化接入规范”和“真实环境执行步骤”混在一起。
2. 不利于后续维护自动化准入规则。

### 4.3 方案 C：拆成单独新文档

问题：

1. 违反现有文档维护规则，不利于保持单点权威。
2. 研发容易在多份说明之间来回查找。

### 4.4 最终决策

采用方案 A。

即：

1. 主体内容写入 `docs/05-自动化测试与质量保障.md`。
2. `docs/21-业务功能清单与验收标准.md` 只做最小引用补充。

## 5. 文档结构设计

计划在 `docs/05` 中新增一节“接入智维自动化接入指南”，包含以下 5 个部分：

1. 适用范围
2. 标准接入骨架与命名规则
3. 研发实际操作步骤
4. 当前 6 个入口的对照示例
5. 提交门槛与不允许事项

## 6. Section 1：适用范围

本节要明确：

1. 该指南只服务“接入智维”入口自动化接入，不负责风险运营、风险策略、平台治理或质量工场其他模块。
2. 该指南既覆盖当前 6 个已落地入口，也覆盖后续新增“接入智维”入口的通用接入规范。
3. 该指南的最低成功标准不是“全量自动化覆盖”，而是“新增一个入口并补齐自动化骨架”。

## 7. Section 2：标准接入骨架与命名规则

### 7.1 两层骨架

新增一个“接入智维”入口时，默认按两层骨架接入：

1. 页面场景层
2. 注册表编排层

### 7.2 页面场景层规则

默认优先扩展 `config/automation/iot-access-web-smoke-plan.json`，一个入口对应一个 `scenario`。

规则如下：

1. `key` 命名统一为 `iot-access-<route-slug>`。
2. `route` 必须写真实入口路径；若靠 query 切 tab，可保留 query，并用 `expectedPath` 锁定实际路由。
3. `readySelector` 必须优先选择稳定的 `id` 或共享工作台稳定控件，不得依赖易漂移的文案或临时 DOM 结构。
4. `scope` 只允许使用 `delivery` 或 `baseline`。
5. `steps.id` 统一为 `<entry>-<action>`。
6. 需要跨页串联时，统一通过 `captures` 回写变量。

### 7.3 注册表编排层规则

默认优先复用以下 3 个现有专项包：

1. `iot-access.browser-smoke`
2. `iot-access.api-smoke`
3. `iot-access.message-flow`

规则如下：

1. 新入口只是页面主链路补点时，优先扩 `iot-access.browser-smoke` 对应 plan，不新增 registry id。
2. 新入口需要补后端烟测点时，优先扩 `iot-access.api-smoke.runner.pointFilters`。
3. 新入口需要过期 trace、链路回放或独立 trace 取证时，优先扩 `iot-access.message-flow`。
4. 只有当现有三类专项包无法表达该入口时，才允许新增新的 `iot-access.<purpose>` registry scenario。

### 7.4 命名与引用规则

1. browser 场景 key 统一为 `iot-access-<entry>`。
2. registry id 统一为 `iot-access.<runner-purpose>`。
3. `module` 固定为 `iot-access`。
4. `title` 使用“接入智维 + 能力名 + 专项”的表达。
5. `docRef` 优先指向稳定文档锚点，避免长期指向临时设计稿。

### 7.5 允许拆独立 plan 的例外条件

只有出现以下情况，才允许从 `iot-access-web-smoke-plan.json` 拆出独立 plan：

1. 入口需要完全不同的登录前置或环境前置。
2. 单个 plan 的场景数量已经明显失去可维护性。
3. 该入口需要独立执行、独立失败口径或独立证据目录。

## 8. Section 3：研发实际操作步骤

本节收口为一套 6 步接入清单：

1. 判断入口归属  
   先判断新入口应接 `browserPlan`、`apiSmoke` 还是 `messageFlow`。
2. 补页面场景或 runner 入参  
   页面入口优先补 `iot-access-web-smoke-plan.json` 中的 `scenario`；接口类优先扩现有 `pointFilters`。
3. 挂到统一 registry  
   默认复用现有 `iot-access.browser-smoke / iot-access.api-smoke / iot-access.message-flow`。
4. 补自动化回归测试  
   plan 结构变更补 `scripts/run-browser-acceptance.test.mjs`；registry 编排变更补 `scripts/run-acceptance-registry.test.mjs`。
5. 跑最小验证命令  
   至少执行以下命令：
   - `node --test scripts/run-browser-acceptance.test.mjs`
   - `node --test scripts/run-acceptance-registry.test.mjs`
   - `node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/iot-access-web-smoke-plan.json`
   - `node scripts/auto/run-acceptance-registry.mjs --list --scope=delivery`
   - `node scripts/docs/check-topology.mjs`
   - 若新入口属于 `baseline`，再补跑 `node scripts/auto/run-acceptance-registry.mjs --list --scope=baseline`
6. 同步文档  
   至少更新 `docs/05`；若能力矩阵或命令入口变化，再同步 `docs/21`。

## 9. Section 4：当前 6 个入口的对照示例

本节把当前已落地的 6 个入口写成对照表，供后续新增入口直接参考：

1. `/products`
   - 归属：`iot-access.browser-smoke`
   - 作用：验证产品工作台可进入、可创建产品、可捕获 `productId`
2. `/devices`
   - 归属：`iot-access.browser-smoke`
   - 作用：验证设备建档可走通、可选择产品、可捕获 `deviceId`
3. `/reporting`
   - 归属：`iot-access.browser-smoke`
   - 作用：验证设备查询、HTTP 模拟上报、`traceId` 捕获
   - 特殊点：使用 `/reporting?tab=simulate`，但 `expectedPath` 仍锁 `/reporting`
4. `/system-log`
   - 归属：`baseline` 范围内的 `iot-access.browser-smoke`
   - 作用：使用 `traceId` 回查系统日志
5. `/message-trace`
   - 归属：`iot-access.browser-smoke`，并复用 `iot-access.message-flow`
   - 作用：页面按 `traceId` 复核链路，脚本侧做链路专项取证
6. `/file-debug`
   - 归属：`baseline` 范围内的 `iot-access.browser-smoke`
   - 作用：按设备编码触发文件调试刷新，并同时等待 `file-snapshots` 与 `firmware-aggregates`

这一节要传达的原则是：同一个入口可以被不同 runner 视角复用，但默认优先复用现有专项包，不按页面数量线性膨胀自动化体系。

## 10. Section 5：提交门槛与不允许事项

### 10.1 准入门槛

研发为新增“接入智维”入口补自动化时，至少需要满足：

1. 新入口能被归类到现有 `browserPlan / apiSmoke / messageFlow` 之一。
2. 已补 plan 或 registry 的最小骨架。
3. 已补对应测试。
4. 已执行 `dry-run`、`--list` 与 `check-topology`。
5. 已更新 `docs/05`；必要时同步更新 `docs/21`。

### 10.2 不允许事项

1. 不允许为单个接入智维入口新造第二套自动化框架。
2. 不允许只改页面不补自动化骨架。
3. 不允许使用脆弱选择器作为长期 `readySelector`。
4. 不允许只跑 `delivery` 命令却声称已经覆盖 `baseline` 场景。
5. 不允许改了自动化接入口径却不更新文档。

## 11. 实施落点

本次设计确认后，正式实施只改两处：

1. `docs/05-自动化测试与质量保障.md`
   - 新增“接入智维自动化接入指南”完整章节
2. `docs/21-业务功能清单与验收标准.md`
   - 补充一句引用关系，说明接入智维新增入口的自动化准入以 `docs/05` 为准

## 12. 完成定义

本设计落地后，应满足以下结果：

1. 开发人员可以根据 `docs/05` 独立完成新增一个“接入智维”入口的自动化骨架接入。
2. 当前 6 个入口的接入方式在文档中有明确对照示例。
3. 后续新增入口时，研发不需要再口头追问“该补哪个 runner、该不该新建 registry id、该跑哪些最小验证命令”。
