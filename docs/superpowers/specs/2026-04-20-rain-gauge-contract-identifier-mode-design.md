# 雨量计契约字段双模式设计

**Goal:** 统一 nf-monitor-tipping-bucket-rain-gauge-v1 的契约字段提炼口径，避免“正式表示”与“继续观察”在状态数据上出现 full-path / 短标识双套规则。

**Architecture:** 页面继续默认按产品形态自动识别，不新增显式切换控件。后端在 compare/apply 进入统一识别上下文后，按 `deviceStructure + relationMappings + 已发布合同口径 + 样本特征` 决定本次 `DIRECT / FULL_PATH`，并把结果写入 compare 返回与 apply 校验链路。复合设备继续以直接字段为主，监测类型编码只作为归一线索和原始证据保留在 `rawIdentifiers`。

**Tech Stack:** Spring Boot 4, Java 17, Vue 3, TypeScript, JUnit 5, Mockito

---

## 背景

雨量计 `nf-monitor-tipping-bucket-rain-gauge-v1` 的状态数据在 compare 中出现两种口径：
- 正式表示使用 `S1_ZT_1.ext_power_volt`、`S1_ZT_1.humidity`
- 继续观察出现 `ext_power_volt`、`humidity`

这与当前设计不一致。单台多能力产品应保留 full-path；单台单能力/规范产品才收口为直接字段。复合设备正式合同仍以直接字段为主。

## 设计结论

1. 页面默认仍是“按产品形态自动识别”，不新增 `DIRECT / FULL_PATH` 切换。
2. API/DTO 增加可选 `contractIdentifierMode`，仅作为自动识别覆盖参数。
3. compare 统一先 flatten，再按 `deviceStructure + contractIdentifierMode + relationMappings + 已发布合同口径` 归一。
4. apply 只接受 compare 产出的正式口径，并拒绝手工绕过 compare 注入的错误 identifier。

## 规则

- 单台单能力 / 规范产品：正式字段使用 `DIRECT`。
- 单台多能力：正式字段保留 full-path，例如 `S1_ZT_1.ext_power_volt`、`S1_ZT_1.humidity`。
- 复合设备：正式合同以直接字段为主，例如 `value / totalValue / sensor_state / dispsX / dispsY`。
- `rawIdentifiers` 始终保留原始证据，不因正式字段收口而丢失。
- `S1_ZT_1.sensor_state.<logicalChannelCode>` 仍只作为子设备镜像状态，不进入父产品正式字段。

## 接口影响

- `ProductModelGovernanceCompareDTO.ManualExtractInput` 新增 `contractIdentifierMode?`
- `ProductModelCandidateSummaryVO` 新增 `resolvedContractIdentifierMode?`
- 前端类型同步补齐，但页面不新增控件

## 验收标准

- 雨量计状态数据在 compare 中的“正式表示”和“继续观察”口径一致
- 复合设备 compare/apply 仍只输出直接字段主口径
- 采集器父设备状态叶子字段不再被静默截短为短标识
- 相关文档原位同步更新
