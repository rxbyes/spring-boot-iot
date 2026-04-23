# 运行态名称/单位治理体验增强设计

- 日期：2026-04-21
- 状态：设计已确认，进入实施
- 适用仓库：`spring-boot-iot`
- 相关模块：`spring-boot-iot-ui`

## 1. 背景

`/products/:productId/mapping-rules` 已具备 `运行态名称/单位治理` 最小闭环，但从一线使用视角看，仍有四个明显的断点：

1. `/insight` 里看到“未形成正式字段”后，只能记下标识再手动去 `mapping-rules` 搜索和填写，治理链路太跳。
2. `mapping-rules` 面板只有规则列表和编辑表单，缺少“我为什么此刻要治理这条字段”的上下文承接。
3. 保存前看不到本次规则会影响哪些读侧位置，也看不到是否已经和正式字段职责重叠。
4. 规则一旦被正式字段覆盖，列表里没有明显提醒，也缺少就地停用动作。

这些问题不是后端真相缺失，而是前端交互没有把“对象洞察发现问题 -> 映射规则治理 -> 正式字段收口”串成一条低摩擦路径。

## 2. 已确认约束

- 本轮只做 `A 方案`。
- 只做体验增强，不扩展新的治理真相模型。
- 正式真相仍然是 `iot_product_model / 正式合同发布 / resolver snapshot`。
- `iot_runtime_metric_display_rule` 继续只做读侧展示治理，不反写正式字段。
- 不新增重型后端候选聚合接口。
- 不新增删除 API；停用继续复用现有 `status=DISABLED` 更新链路。
- 正式字段覆盖提醒只按 `identifier` 精确匹配，不做 alias 猜测。
- 候选采纳默认优先落到 `PRODUCT` 作用域，减少一线操作负担。

## 3. 方案比较

### 3.1 方案 A：补齐现有运行态治理链路的低摩擦入口（推荐）

优点：

- 不改后端真相，只补前端行为和可视化反馈。
- 一线可以从 `/insight` 直接跳到治理表单，降低来回切页成本。
- 能同时回答“这条规则为什么要配、配完影响哪里、何时应该停用”。

缺点：

- 规则候选和预览都属于静态前端推导，不会回答更复杂的命中范围统计。

### 3.2 方案 B：新增后端候选聚合与命中预估接口

优点：

- 预览可以更完整，理论上还能给出更精确的覆盖范围。

缺点：

- 本轮明显超出需求，且会扩展新的读写契约与维护成本。

### 3.3 方案 C：把运行态治理再拆成独立工作台

优点：

- 页面职责看起来更独立。

缺点：

- 会把当前已经收口到 `mapping-rules` 的治理入口重新打散，不符合本轮“少跳转、同页闭环”的目标。

## 4. 确认设计

### 4.1 `/insight` 为非正式字段提供“一键去治理”

设备属性快照表中，未命中正式字段的行不再只显示“未形成正式字段”提示，而是提供 `去治理名称/单位` 行级动作。

- 点击后直接跳转到 `/products/:productId/mapping-rules`。
- 同时通过 query 透传当前候选上下文：`rawIdentifier / displayName / unit / deviceCode / source=insight / runtimeGovernanceDraft=1`。
- 已命中正式字段的行继续保留现有 `修改名称/单位`，仍然跳 `contracts`，不混用两套治理语义。

### 4.2 `运行态名称/单位治理` 面板顶部增加轻候选区

`ProductRuntimeMetricDisplayRulePanel` 需要读取来自 route query 的候选上下文，并在编辑器前方展示单条轻量候选卡。

- 候选卡需要回答来源、设备编码、raw identifier、当前显示名、当前单位。
- 提供 `采纳到表单` 与 `忽略` 两个动作。
- `采纳到表单` 只回填当前表单，不直接保存。
- `忽略` 只隐藏当前候选，不写任何后端状态。
- 候选默认使用 `PRODUCT` scope，其他 scope 仍由用户在表单中改选。

### 4.3 保存前增加静态“本次生效预览”

编辑器在提交按钮前需要新增静态预览区，基于“当前表单 + 已加载规则 + 当前正式字段列表”回答：

- 当前会写入哪个 scope 签名。
- 预期影响的读侧位置：`设备属性快照 / 历史趋势 / 对象洞察`。
- 是否与现有同 scope 同 rawIdentifier 规则冲突。
- 当前 rawIdentifier 是否已被正式字段精确覆盖。

这里的“预览”只做静态推导，不显示设备数量、历史命中次数或伪精度影响统计。

### 4.4 规则列表增加“正式字段已覆盖”提醒与快捷停用

规则列表卡片需要补齐两个运营动作：

- 当 `row.rawIdentifier` 与当前正式 `property.identifier` 精确相等时，卡片显示 `已被正式字段覆盖` 提醒。
- 对这类规则提供 `快捷停用` 动作，实际仍走现有更新接口，把 `status` 改成 `DISABLED`。
- 未覆盖规则继续保留 `编辑` 动作；已停用规则仍可重新编辑并恢复。

### 4.5 仍然保留的边界

- 不新增批量导入、批量停用或批量采纳。
- 不改 `/products/:productId/contracts` 的正式字段治理入口。
- 不改 `/insight` 的读侧展示优先级顺序。
- 不改后端 `RuntimeMetricDisplayRuleServiceImpl` 的匹配算法。

## 5. 影响文件

预计至少涉及：

- `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- `spring-boot-iot-ui/src/components/product/ProductRuntimeMetricDisplayRulePanel.vue`
- `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductRuntimeMetricDisplayRulePanel.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- `README.md`
- `AGENTS.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`
- `docs/21-业务功能清单与验收标准.md`
- `docs/appendix/iot-field-governance-and-sop.md`

## 6. 测试与验证

实现阶段至少覆盖以下验证：

- `DeviceInsightView.test.ts` 锁定非正式字段会出现 `去治理名称/单位`，且点击后携带 query 跳转 `mapping-rules`。
- `ProductRuntimeMetricDisplayRulePanel.test.ts` 锁定候选卡渲染、采纳回填、静态预览、正式字段覆盖提醒和快捷停用。
- `ProductModelDesignerWorkspace.test.ts` 锁定 `mapping-rules` 工作区会把正式字段标识传给运行态治理面板。
- 定向跑 `vitest` 与前端 `build`，确认页面行为和类型收口。

## 7. 文档更新要求

本次实现完成后必须原位更新：

- `README.md`
- `AGENTS.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`
- `docs/21-业务功能清单与验收标准.md`
- `docs/appendix/iot-field-governance-and-sop.md`
