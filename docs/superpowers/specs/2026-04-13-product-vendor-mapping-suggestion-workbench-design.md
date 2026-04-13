# 产品契约工作台映射规则建议区设计

> 日期：2026-04-13
> 状态：已在会话内确认设计，待用户审阅书面 spec
> 适用范围：`spring-boot-iot-ui`、`spring-boot-iot-device` 既有映射规则接口消费层、`/products -> 契约字段` 工作区、文档体系
> 目标：把后端已完成的厂商字段映射规则 suggestions 能力接入 `/products -> 契约字段`，并补齐“人工采纳建议 -> 创建 DRAFT 规则”的最小前端治理闭环。

## 1. 背景

截至 `2026-04-13`，仓库已经具备以下稳定事实：

1. 后端已提供只读接口 `GET /api/device/product/{productId}/vendor-mapping-rule-suggestions`。
2. 该接口会基于 `iot_vendor_metric_evidence`、已发布 canonical resolver snapshot、规范字段库和现有 mapping rules，返回 `READY_TO_CREATE / ALREADY_COVERED / CONFLICTS_WITH_EXISTING / LOW_CONFIDENCE / IGNORED_*`。
3. 映射规则对象 `iot_vendor_metric_mapping_rule` 已成为一级治理对象，并已被 compare、apply、审批执行与运行时 `PAYLOAD_APPLY` 消费。
4. 当前前端 `/products -> 契约字段` 仍只承接手工样本 compare / apply / 正式字段维护，没有把运行态映射规则建议接进同一工作区。

因此当前存在一个明显断层：

1. 后端已经能回答“哪些 rawIdentifier 值得建规则”，但一线在产品工作台内看不到。
2. 即使研发或测试直接调到 suggestions，也没有“采纳建议并创建 DRAFT 草稿规则”的前端操作闭环。
3. 如果继续要求人工在工作台外部抄写 `rawIdentifier / logicalChannelCode / targetNormativeIdentifier` 再录规则，零代码接入治理效率会长期停留在半手工状态。

## 2. 目标

本轮只做以下事情：

1. 在 `/products -> 契约字段` 同页接入厂商字段映射规则 suggestions 区块。
2. 明确展示每条建议的状态、证据、原因和已有规则命中情况。
3. 支持对 `READY_TO_CREATE` 与 `LOW_CONFIDENCE` 建议执行“采纳为草稿规则”。
4. 采纳动作直接复用既有 `POST /api/device/product/{productId}/vendor-mapping-rules`，创建 `status=DRAFT` 的 `PRODUCT` scope 规则。
5. 采纳成功后刷新 suggestions，并明确提示“若要让本次识别结果使用新规则，请重新执行识别”。

## 3. 非目标

本轮明确不做：

1. 不新增自动创建规则。
2. 不新增自动审批、自动发布或自动回滚。
3. 不支持从建议区直接创建 `DEVICE_FAMILY / SCENARIO / PROTOCOL` 规则。
4. 不在建议区直接编辑 `relationConditionJson / normalizationRuleJson`。
5. 不做批量采纳。
6. 不在采纳成功后自动重跑 compare。
7. 不新增第二个治理页面、抽屉路由或控制面页面。

## 4. 方案比较

### 4.1 方案 A：在契约字段页新增独立 suggestions 区块

放置位置：

- `样本输入 -> 识别结果 -> 映射规则建议 -> 本次生效 -> 当前已生效字段`

优点：

1. suggestions 与 compare/apply 同属治理候选，放在同一工作区最自然。
2. 不会把“规则候选”和“正式字段真相”混在一起。
3. 一线可以在同一上下文内先看字段识别，再看映射建议，再决定是否采纳。

缺点：

1. `契约字段` 工作区会新增一个治理区块，页面复杂度略增。

结论：

- 采用本方案。

### 4.2 方案 B：把 suggestions 并入“识别结果”

优点：

1. 页面更紧凑。

缺点：

1. “正式字段候选”和“映射规则候选”是两套不同治理对象，混合展示会模糊语义。
2. 后续状态过滤、行级动作和错误提示会让 compare 卡片进一步膨胀。

结论：

- 拒绝。

### 4.3 方案 C：用抽屉或弹层承载 suggestions

优点：

1. 主工作区更干净。

缺点：

1. 会打断 `/products -> 契约字段` 当前已收口的同页治理语义。
2. 需要额外切换上下文，不利于一线连续操作。

结论：

- 延后。除非后续 suggestions 能力成长为独立控制面，否则不采用。

## 5. 设计结论

本轮固定采用：

1. 在 `/products -> 契约字段` 新增独立“映射规则建议”区块。
2. 该区块默认显示真正需要人工处理的 suggestions，不把已覆盖和已忽略项混入首屏。
3. suggestions 的采纳动作只创建 `PRODUCT + DRAFT` 规则，不提升 scope，不自动审批。
4. 采纳后只刷新 suggestions，并提示 compare 结果可能已过时；是否重新识别由用户显式决定。

## 6. 页面结构设计

### 6.1 区块位置

新增区块固定放在：

1. `样本输入`
2. `识别结果`
3. `映射规则建议`
4. `本次生效`
5. `当前已生效字段`

原因：

1. suggestions 属于治理候选，不属于正式真相。
2. 它和 compare 行一样都服务于“本轮治理会话”，但不应挤进 compare 卡片内部。

### 6.2 首屏展示策略

默认只展示以下状态：

1. `READY_TO_CREATE`
2. `CONFLICTS_WITH_EXISTING`
3. `LOW_CONFIDENCE`

默认不展示：

1. `ALREADY_COVERED`
2. `IGNORED_*`

页面提供两个轻量开关：

1. `显示已覆盖`
2. `显示已忽略`

切换开关后，前端重新请求 suggestions，而不是在首屏结果上做本地伪过滤。

### 6.3 单条建议最小展示内容

每条建议至少展示：

1. `rawIdentifier`
2. `logicalChannelCode`
3. `targetNormativeIdentifier`
4. `status`
5. `confidence`
6. `evidenceCount`
7. `sampleValue`
8. `reason`
9. `existingRuleId`
10. `existingTargetNormativeIdentifier`

显示语义固定为：

1. 回答“原始字段是什么”
2. 回答“建议归一到哪个 canonical”
3. 回答“为什么会给出这个建议”
4. 回答“当前是不是已经有规则，或与旧规则冲突”

## 7. 数据流设计

### 7.1 suggestions 读侧加载

进入某产品的 `契约字段` 工作区后，前端并行加载：

1. 现有 `compare / apply / release / 正式字段` 所需数据
2. `vendor-mapping-rule-suggestions`

触发时机固定为：

1. 打开 `契约字段`
2. 切换产品
3. 采纳建议成功后
4. 用户切换“显示已覆盖 / 显示已忽略”开关后

默认查询参数固定为：

1. `includeCovered=false`
2. `includeIgnored=false`
3. `minEvidenceCount=1`

### 7.2 采纳建议写侧

前端不新增专用后端写接口，直接复用：

- `POST /api/device/product/{productId}/vendor-mapping-rules`

首版构造的 payload 固定最小化为：

1. `scopeType=PRODUCT`
2. `rawIdentifier`
3. `logicalChannelCode`
4. `targetNormativeIdentifier`
5. `status=DRAFT`

首版不允许用户在该入口选择：

1. 更高 scope
2. `relationConditionJson`
3. `normalizationRuleJson`

原因：

1. 本轮目标只是把 suggestions 转成最小草稿规则，而不是做完整规则设计器。

### 7.3 采纳成功后的联动

采纳成功后前端执行：

1. 刷新 suggestions 区块
2. 在主工作区显示轻量提示：`映射规则草稿已创建。若要让本次识别结果使用新规则，请重新执行识别。`

本轮明确不做：

1. 自动重跑 compare
2. 自动清空本次 compare 结果
3. 自动跳转到映射规则维护页

原因：

1. compare 结果属于用户当前样本上下文，自动重算会在用户无感知的情况下改写会话中部结果，容易产生理解断层。

## 8. 状态与动作规则

### 8.1 可操作状态

首版动作规则固定为：

1. `READY_TO_CREATE`
   - 展示：`采纳为草稿规则`
2. `LOW_CONFIDENCE`
   - 展示：`采纳为草稿规则`
   - 但采纳前需二次确认
3. `CONFLICTS_WITH_EXISTING`
   - 不展示采纳按钮，只展示冲突信息
4. `ALREADY_COVERED`
   - 默认不展示；若用户主动显示，也不提供采纳动作
5. `IGNORED_*`
   - 默认不展示；若用户主动显示，也不提供采纳动作

### 8.2 低置信度确认

当用户采纳 `LOW_CONFIDENCE` 时，必须出现二次确认，确认文案回答：

1. 当前证据次数较低
2. 本次只会创建 `DRAFT` 草稿规则
3. 不会自动生效

### 8.3 冲突语义

`CONFLICTS_WITH_EXISTING` 的展示必须至少回答：

1. 当前产品已有规则命中
2. 该旧规则归一到哪个 `existingTargetNormativeIdentifier`
3. 因此当前建议不能直接采纳

该状态不支持“强制覆盖”。

## 9. 前端组件边界

### 9.1 API 边界

当前 `spring-boot-iot-ui/src/api/product.ts` 不继续塞映射规则接口。

建议新增独立 API 文件：

- `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts`

职责只包括：

1. `listSuggestions(productId, query)`
2. `createRule(productId, payload)`

### 9.2 组件边界

建议新增子组件：

- `spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue`

该组件只负责：

1. suggestions 列表渲染
2. 过滤开关
3. 行级采纳按钮
4. 本区块的 loading / empty / error / retry

`ProductModelDesignerWorkspace.vue` 只负责：

1. 提供当前 product 上下文
2. 承接 suggestions 区块位置
3. 在采纳成功后显示“compare 结果可能过时”的提示
4. 触发 suggestions 刷新

### 9.3 状态归属

主工作区维护：

1. 当前产品上下文
2. compare 过期提示
3. suggestions 成功采纳后的联动刷新信号

suggestions 面板维护：

1. `includeCovered`
2. `includeIgnored`
3. suggestions loading
4. suggestions error
5. suggestions empty
6. 单行采纳 loading

## 10. 错误处理

### 10.1 建议区块加载失败

若 suggestions 加载失败：

1. 只在 suggestions 区块内显示轻量错误态和“重试”
2. 不阻断整个 `契约字段` 工作区
3. 不覆盖已有 compare / apply / 正式字段内容

### 10.2 采纳失败

采纳失败时：

1. 保留当前 suggestions 列表和开关状态
2. 优先显示后端返回的业务错误 message
3. 若请求层错误已被标记 handled，则前端不再补第二条 toast

### 10.3 compare 结果过期提示

采纳成功后，只给出会话内提示，不强制打断其他操作：

1. 该提示属于“提醒用户可重新识别”
2. 不应被实现成全局错误态

## 11. 测试策略

### 11.1 新增面板级测试

建议新增：

- `spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts`

至少覆盖：

1. 默认只显示需处理 suggestions
2. 切换 `显示已覆盖 / 显示已忽略` 会重新加载
3. `READY_TO_CREATE` 可采纳
4. `LOW_CONFIDENCE` 采纳前会二次确认
5. `CONFLICTS_WITH_EXISTING` 不显示采纳按钮
6. handled error 不重复 toast

### 11.2 主工作区集成测试

继续在：

- `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

补最小集成断言：

1. suggestions 区块出现在 `识别结果` 与 `本次生效` 之间
2. 采纳成功后出现 compare 过期提示
3. 切换 product 时 suggestions 会重新加载

本轮不把所有 suggestions 细节继续塞进工作区大测试中。

## 12. 文档影响

若本设计落地，至少需要同步：

1. `docs/02-业务功能与流程说明.md`
2. `docs/03-接口规范与接口清单.md`
3. `docs/08-变更记录与技术债清单.md`
4. `docs/15-前端优化与治理计划.md`

原因：

1. 这是 `/products` 工作区新增治理区块，不只是后端接口补充。

## 13. 验收口径

完成后必须能回答：

1. 一线能否在 `/products -> 契约字段` 内直接看到当前产品的映射规则建议？
2. 是否能在同页把 `READY_TO_CREATE` 或 `LOW_CONFIDENCE` 建议采纳为 `DRAFT` 规则？
3. 采纳成功后，用户是否能立即看到 suggestions 刷新结果？
4. 页面是否明确提示“若要让本次识别结果使用新规则，请重新执行识别”？

并继续保持以下边界：

1. 不自动建规则
2. 不自动审批
3. 不自动重跑 compare
4. 不把 suggestions 混入正式字段真相区

## 14. 后续演进顺序

本设计落地后，建议下一轮顺序固定为：

1. 同页 suggestions 接入与单条采纳
2. 批量采纳
3. 场景继承 / 共享 scope 提升
4. 映射规则独立审批与发布谱系
5. 更高阶自动创建 / 自动绑定策略

原因：

1. 当前最缺的是“建议可见 + 可采纳”的最小闭环。
2. 在没有稳定前端闭环前，继续做共享提升或自动化治理，收益会明显低于成本。
