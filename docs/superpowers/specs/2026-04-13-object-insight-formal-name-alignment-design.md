# 对象洞察设备属性快照正式字段命名对齐设计

**日期：** 2026-04-13  
**范围：** `spring-boot-iot-ui`、文档口径  
**问题类型：** 产品治理正式字段与对象洞察设备属性快照命名不一致

---

## 1. 背景与问题

用户在 `产品定义中心 -> 契约字段 -> 当前已生效字段` 与 `对象洞察台 -> 设备属性快照` 之间比对同一产品时，发现同一属性字段会出现不同中文名和定义口径。

当前产品治理语义已经明确：

- `当前已生效字段` 承载正式契约真相；
- 对象洞察台应消费正式治理结果，而不是再维护一套独立命名体系。

但当前 `/insight` 的设备属性快照仍然保留多来源命名兜底，因此在 formal identifier 与 runtime/raw identifier 不完全一致时，会退回对象洞察配置名或运行态属性名，造成与产品经营工作台不一致。

---

## 2. 根因结论

### 2.1 直接根因

`spring-boot-iot-ui/src/views/DeviceInsightView.vue` 中，`propertyTableRows` 的 `displayName` 当前按以下顺序兜底：

1. `productModelDisplayNameMap.get(identifier)`
2. `configuredMetric?.displayName`
3. `property?.propertyName`
4. `series?.displayName`

其中 `productModelDisplayNameMap` 只按**完全相同的 identifier** 查正式字段名。

### 2.2 为什么会失效

对象洞察正式配置当前已经逐步收口到 canonical identifier，例如：

- 正式字段：`signal_4g`
- 运行态属性：`S1_ZT_1.signal_4g`

当 `/insight` 读取到的是 runtime/raw identifier，而正式字段映射只按 exact key 查询时，就无法命中正式字段名，随后退回：

- `customMetrics[].displayName`
- `iot_device_property.property_name`
- telemetry point `displayName`

这就形成了“产品经营工作台一套名、对象洞察设备属性快照另一套名”的双真相。

### 2.3 影响范围

本次问题集中体现在：

- `/products -> 当前已生效字段`
- `/insight -> 设备属性快照`

趋势图例、综合分析等区域当前也使用相近的命名辅助逻辑，但本轮用户明确反馈的是**设备属性快照**与正式字段不一致，因此本次先收口设备属性快照的正式命名真相，不额外扩大到页面其它展示块。

---

## 3. 目标

1. 让 `/insight -> 设备属性快照` 对同一正式字段始终优先显示 `iot_product_model.model_name`。  
2. 支持 formal identifier 与 runtime/raw identifier 不完全一致时，仍能通过 canonical/suffix 规则命中正式字段名。  
3. 在存在正式字段名时，不再让 `customMetrics[].displayName` 或运行态 `propertyName` 覆盖设备属性快照名称。  
4. 保持对象洞察页面结构、趋势分组和现有 API 不变。  
5. 同步更新业务文档与变更记录，明确“设备属性快照以正式字段名为准”的口径。

---

## 4. 非目标

1. 不调整 `/insight` 的整体页面布局。  
2. 不新增后端接口，不修改 `/api/device/{deviceCode}/properties` 合同。  
3. 不重构对象洞察全部命名逻辑，只处理本轮正式字段与属性快照的一致性。  
4. 不在本轮新增对象洞察配置编辑入口。  
5. 不触碰风险目录、趋势分组或对象洞察配置保存流程。

---

## 5. 设计决策

### 5.1 单一真相源

`设备属性快照` 的名称真相源固定为：

1. 正式字段中文名 `iot_product_model.model_name`
2. 若不存在正式字段，再退回运行态 `propertyName`
3. 若运行态也没有，再退回对象洞察配置 `displayName`
4. 最后才走现有命名辅助规则

这意味着 `customMetrics[].displayName` 继续保留对象洞察编排作用，但**不再覆盖属性快照中已存在正式字段的中文名**。

### 5.2 canonical/suffix 命中

前端在 `/insight` 内补一个轻量 identifier 解析规则：

- 先按 exact identifier 查询正式字段名；
- exact 未命中时，再按末段 canonical identifier 查询，例如：
  - `S1_ZT_1.signal_4g -> signal_4g`
  - `L1_LF_1.value -> value`
  - `S1_ZT_1.sensor_state.L1_LF_1 -> L1_LF_1` 不做额外业务改写，只按末段兜底

本轮只把这套规则用于**正式字段 displayName / dataType / unit** 的前端读侧复用，不在这里发明新规范库。

### 5.3 收口范围

本轮实现只改：

- `DeviceInsightView.vue` 中设备属性快照名称解析
- 同页 formal dataType / formal unit 的辅助查询一致性
- 对应前端测试

其它区域如趋势图例、综合分析仍保持现有展示策略，只要 formal exact/canonical 命中自然会一起受益，但不把它们作为本轮主验收目标。

---

## 6. 涉及文件

### 前端实现

- `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

### 前端测试

- `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

### 文档

- `docs/02-业务功能与流程说明.md`
- `docs/08-变更记录与技术债清单.md`

本轮会检查 `README.md` 与 `AGENTS.md` 是否需要同步；若无新增全局规则，只记录“检查后无需更新”。

---

## 7. 测试与验收

### 7.1 自动化测试

至少覆盖以下场景：

1. 正式字段 identifier 为 canonical，运行态属性 identifier 为 raw alias 时，设备属性快照仍显示正式字段名。  
2. 当正式字段存在时，设备属性快照不能再被 `customMetrics[].displayName` 覆盖。  
3. 当正式字段不存在时，仍允许退回运行态属性名或对象洞察配置名，保持现有兼容行为。

### 7.2 页面验收口径

同一字段在以下两处必须一致：

1. `/products -> 契约字段 -> 当前已生效字段`
2. `/insight -> 设备属性快照`

---

## 8. 最终决策

采用“**设备属性快照以正式字段名为唯一优先真相源，并补 canonical/suffix 命中**”方案：

- 不再把对象洞察配置名当成正式字段替代品；
- 不新增后端复杂治理逻辑；
- 通过最小前端读侧修复，保证产品经营工作台与对象洞察设备属性快照的命名一致。
