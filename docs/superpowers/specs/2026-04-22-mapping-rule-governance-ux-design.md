# 映射规则治理体验增强设计

## 背景

当前映射规则系统已具备完整的 CRUD、建议面板、台账面板和运行时解析引擎，但在日常治理流程中存在 4 个痛点：

1. **未正式字段无快捷入口**：insight/快照/趋势里"未形成正式字段"只是静态文本，无法一键跳转到映射规则页补录名称/单位
2. **无保存前预览**：用户必须先创建规则才能试命中，无法在创建前预判生效情况
3. **缺待治理候选区**：建议列表存在但无"高频未正式化字段"汇总视图
4. **老规则沉积**：已被正式字段覆盖的运行态规则无标记，持续参与解析

## 方案选型

采用**方案 A：最小增量**，4 项功能全部复用现有后端 API，仅做 1 处后端增强（覆盖打标 VO 字段）。

---

## 功能 1：一键补录——从 insight 跳转映射规则页并预填

### 目标

insight 属性快照表中，未正式字段的"未形成正式字段"文本替换为"补名称/单位"链接按钮，点击后深链跳转到映射规则页并预填。

### 前端改动

**DeviceInsightView.vue**：

- 属性快照表"治理操作"列（`el-table-column label="治理操作"`）：
  - `canEditFormalField = false` 时，将 `<span>未形成正式字段</span>` 替换为 `<StandardButton link @click="handlePromoteToMappingRule(row)">补名称/单位</StandardButton>`
- 新增 `handlePromoteToMappingRule(row: PropertySnapshotRow)` 函数：
  ```typescript
  function handlePromoteToMappingRule(row: PropertySnapshotRow) {
    const productId = device.value?.productId;
    if (productId === undefined || productId === null) return;
    void router.push({
      path: buildProductWorkbenchSectionPath(productId, 'mapping-rules'),
      query: {
        rawIdentifier: row.identifier,
        scope: 'PRODUCT',
        source: 'insight'
      }
    });
  }
  ```

**ProductVendorMappingSuggestionPanel.vue**：

- 组件挂载时读取 `route.query.rawIdentifier`：
  - 如果匹配到某条建议，自动滚动到该条目并添加高亮样式
  - scope 下拉预填 `route.query.scope` 的值
- 高亮样式：添加 `.product-vendor-mapping-suggestion-panel__item--highlight` CSS 类，3 秒后自动移除

**productWorkbenchRoutes.ts**：

- 无需改动，`buildProductWorkbenchSectionPath` 已支持 `'mapping-rules'` section

### 后端改动

无。suggestion API 已返回 `rawIdentifier` 和 `recommendedScopeType`。

---

## 功能 2：保存前生效预览

### 目标

在采纳建议/创建规则前，并发调用 replay + preview-hit，展示结构化预览信息，让用户在保存前知道影响范围。

### 前端改动

**ProductVendorMappingSuggestionPanel.vue**：

- 每条可采纳建议（`canAccept(suggestion) = true`）的操作区新增"预览生效"按钮
- 点击后并发调用：
  - `previewVendorMetricMappingRuleHit(productId, { rawIdentifier, logicalChannelCode })`
  - `replayVendorMetricMappingRule(productId, { rawIdentifier, logicalChannelCode, scopeType, protocolCode, scenarioCode, deviceFamily })`
- 展示 4 项结构化预览信息（内联在建议条目下方）：

| 信息项 | 数据来源 | 判断逻辑 |
|--------|----------|----------|
| 会命中哪条规则 | replay `matched` + `matchedScopeType` + `canonicalIdentifier` | 直接展示 |
| 是否与现有规则冲突 | replay `targetNormativeIdentifier` vs 当前表单 `targetNormativeIdentifier` | 不一致时标红 |
| 是否已被正式字段覆盖 | preview-hit `matched = true` 且 `hitSource = SNAPSHOT` | 匹配时显示橙色警告 |
| 影响范围说明 | 当前表单 `scopeType` | 文案映射：产品级→该产品所有设备，协议级→该产品该协议设备，场景级→该产品该场景设备，设备族级→该产品该设备族设备，租户默认→租户下所有产品 |

- 预览结果用 `previewStateBySuggestionKey` ref 存储，key 为 `suggestionKey(suggestion)`

**ProductVendorMappingRuleLedgerPanel.vue**：

- 已有试命中和回放功能，将结果展示调整为统一的 4 项结构化信息格式（复用同一个渲染逻辑）

### 后端改动

无，复用已有 `preview-hit` 和 `replay` API。

---

## 功能 3：待治理候选区

### 目标

在映射规则页新增"待治理候选"区，汇总高频未正式化字段，预填名称/单位草稿，由人工确认。

### 前端改动

**新增组件 `ProductVendorMappingGovernanceCandidatePanel.vue`**：

- 调用 `listVendorMetricMappingRuleSuggestions(productId, { includeCovered: false, includeIgnored: false, minEvidenceCount: 2 })`
- 前端过滤：只保留 `status === 'READY_TO_CREATE'`
- 按 `evidenceCount` 降序排列，展示前 10 条
- 每条候选显示：
  - `rawIdentifier`
  - `evidenceCount`（证据次数）
  - `confidence`（置信度）
  - `lastSeenTime`（最近出现时间）
  - 推荐 `scopeType`
- 预填草稿：名称默认取 `targetNormativeIdentifier`，单位留空
- "去采纳"按钮：emit 事件 `go-accept`，父组件滚动到 suggestion panel 对应条目
- 底部"查看全部 N 条建议"链接：emit 事件 `view-all-suggestions`

**ProductModelDesignerWorkspace.vue**（mapping-rules 视图）：

- 在 suggestion panel 之前插入 `<ProductVendorMappingGovernanceCandidatePanel>`
- 监听 `go-accept` 和 `view-all-suggestions` 事件，滚动到 suggestion panel 并高亮目标条目

### 后端改动

无，复用已有 suggestion API。

---

## 功能 4：正式字段覆盖打标 + 一键停用

### 目标

对已被正式字段覆盖的运行态规则打橙色标签，并支持一键停用。

### 后端改动

**VendorMetricMappingRuleVO** — 新增 1 个字段：

```java
private Boolean coveredByFormalField;
```

> 注：前端台账列表调用 `GET /vendor-mapping-rules`（`pageRules`），返回 `VendorMetricMappingRuleVO`，前端再映射为 `VendorMetricMappingRuleLedgerRow`。因此字段加在 `VendorMetricMappingRuleVO` 而非 `VendorMetricMappingRuleLedgerRowVO`。

**VendorMetricMappingRuleServiceImpl** — `toVO()` 方法增加覆盖判断入参，`pageRules()` 批量路径增加覆盖检查：

- `pageRules()` 方法在查完规则列表和发布快照后，批量检查覆盖状态：
  1. 收集所有不重复的 `targetNormativeIdentifier`
  2. 一次查询 `NormativeMetricDefinition`：`WHERE identifier IN (...)` + 产品对应 scenario/deviceFamily 过滤
  3. 一次查询 `PublishedProductContractSnapshot`：匹配 `productId` 的发布快照是否包含对应 identifier
  4. 为每条规则在 `toVO()` 时设置 `coveredByFormalField`
- `toVO()` 方法签名增加 `Set<String> coveredIdentifiers` 参数（或单独的覆盖检查方法）
- `createAndGet()` / `updateAndGet()` 单条路径也需设置 `coveredByFormalField`（可复用同一判断逻辑）

避免 N+1 查询。

### 前端改动

**api.ts** — `VendorMetricMappingRule` 和 `VendorMetricMappingRuleLedgerRow` 均新增字段：

```typescript
coveredByFormalField?: boolean | null
```

**vendorMetricMappingRule.ts** — `listVendorMetricMappingRuleLedger` 映射时透传 `coveredByFormalField`：

```typescript
coveredByFormalField: row.coveredByFormalField,
```

**ProductVendorMappingRuleLedgerPanel.vue**：

- 规则卡片标题区（`product-vendor-rule-ledger__title`）：
  - 如果 `row.coveredByFormalField === true`，在 scope 标签旁显示橙色标签 `"已被正式字段覆盖"`
- 新增"一键停用"按钮：
  - 仅当 `row.coveredByFormalField === true && row.draftStatus === 'ACTIVE'` 时显示
  - 点击后弹出确认框："此规则已被正式字段覆盖，停用后不再参与运行时解析。确认停用？"
  - 确认后调用 `batchUpdateVendorMetricMappingRuleStatus(productId, { ruleIds: [row.ruleId], targetStatus: 'DISABLED' })`
  - 成功后刷新台账列表

---

## 改动范围汇总

| 文件 | 改动类型 |
|------|----------|
| `DeviceInsightView.vue` | 修改：替换"未形成正式字段"为深链按钮 |
| `ProductVendorMappingSuggestionPanel.vue` | 修改：读取路由参数预填+预览生效按钮 |
| `ProductVendorMappingRuleLedgerPanel.vue` | 修改：覆盖标签+一键停用+预览格式统一 |
| `ProductModelDesignerWorkspace.vue` | 修改：插入候选 Panel |
| `ProductVendorMappingGovernanceCandidatePanel.vue` | **新增**：待治理候选区组件 |
| `api.ts` | 修改：LedgerRow 新增 `coveredByFormalField` |
| `VendorMetricMappingRuleVO.java` | 修改：新增 `coveredByFormalField` 字段 |
| `VendorMetricMappingRuleServiceImpl.java` | 修改：`toVO()` + `pageRules()` 增加覆盖检查 |
| `vendorMetricMappingRule.ts` | 修改：台账映射透传 `coveredByFormalField` |

共 8 个文件修改 + 1 个文件新增，后端改动限定在 2 个 Java 文件。
