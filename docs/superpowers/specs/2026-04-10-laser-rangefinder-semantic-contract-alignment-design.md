# 南方测绘激光测距产品语义契约收口设计说明

> 日期：2026-04-10
> 适用产品：`nf-monitor-laser-rangefinder-v1`
> 适用厂商：南方测绘
> 代表父设备：`SK00EA0D1307988`、`SK00EA0D1307967`、`SK00EA0D1307986`、`SK00EA0D1307987`、`SK00EA0D1308009`、`SK00EA0D1307984`、`SK00EA0D1308006`、`SK00EA0D1307968`、`SK00E90D1307874`、`SK00EA0D1307992`
> 目标：在不新增独立激光规范场景的前提下，复用现有 `phase1-crack` 底层治理链，把激光测距产品的正式语义、展示语义、对象洞察和治理支撑数据收口为独立产品基线

## 1. 背景与范围

当前仓库和共享 `dev` 环境已经形成以下事实：

1. `L1_LF_*` 复合子设备场景已经在协议、关系主数据和 `/products` 契约字段工作台中稳定收口为：
   - 业务样本：`L1_LF_* -> value`
   - 状态样本：`S1_ZT_1.sensor_state.L1_LF_* -> sensor_state`
   - 关系策略：`LF_VALUE + SENSOR_STATE`
2. 共享 `dev` 库中已经存在独立激光产品：
   - `product_id = 202603192100560258`
   - `product_key = nf-monitor-laser-rangefinder-v1`
   - `product_name = 南方测绘 监测型 激光测距仪`
3. 共享 `dev` 库中，用户提供的 `10` 组父设备与 `54` 条 `L1_LF_*` 关系已经全部指向该激光产品。
4. 共享 `dev` 库中，激光产品已有 `62` 台设备实例，其中大部分是从父采集器拆分出来的子设备实例。

同时，当前还存在 4 个明显不一致：

1. 激光产品虽然已经独立建档，但正式字段中文名仍沿用裂缝语义：
   - `value -> 裂缝值`
   - `sensor_state -> 裂缝状态`
2. 激光产品 `iot_product.metadata_json.objectInsight.customMetrics[]` 为空，尚未形成对象洞察重点指标基线。
3. 激光产品缺少和现有裂缝、GNSS 场景同层级的治理支撑数据闭环：
   - `iot_vendor_metric_evidence`
   - `iot_product_contract_release_batch`
   - `risk_metric_catalog`
4. `ProductModelNormativeMatcher` 当前只识别 `phase1-crack` 和 `phase2-gnss`，不能把激光产品路由到现有规范链，因此 compare、证据沉淀和发布批次不会自动走通。

本设计只处理以下问题：

1. 激光测距产品是否继续复用现有 `L1_LF_*` 的 `value / sensor_state` 底层契约。
2. 如何在复用底层语义的同时，避免页面、正式字段和 compare 元数据继续显示“裂缝”文案。
3. 如何把激光产品在共享 `dev` 库中的正式物模型、对象洞察、证据、发布批次和风险目录补齐到可维护基线。
4. 如何保证现有裂缝直连设备和裂缝产品不被误伤。

本设计明确不处理以下内容：

1. 新增 `phase3-laser` 或其他激光专属规范场景。
2. 新增数据库表或调整既有表结构。
3. 改造 `L1_LF_* -> value` 与 `sensor_state` 的底层 canonical 规则。
4. 重做风险点绑定、阈值规则、联动预案或对象洞察新页面。

## 2. 核心结论

本轮固定采用“复用底层治理链，隔离产品展示语义”的方案，核心结论如下：

1. 激光测距产品继续保留独立产品身份，不并回裂缝产品。
2. 激光测距产品继续复用 `phase1-crack` 的底层规范链，不新开激光专属场景。
3. 激光测距产品的正式 `identifier` 仍固定为：
   - `value`
   - `sensor_state`
4. 激光测距产品对外展示语义必须独立于裂缝产品：
   - 正式字段中文名不得再显示“裂缝值 / 裂缝状态”
   - compare 结果中的 `normativeName` 不得再显示“裂缝监测值”
   - 对象洞察中的重点指标中文名必须走激光测距语义
5. 激光产品默认只把 `value` 纳入对象洞察趋势重点项，不自动把 `sensor_state` 加入趋势。
6. 风险闭环继续复用裂缝当前规则：
   - `value` 可以进入风险目录
   - `sensor_state` 只保留治理语义，不进入风险目录
7. 共享 `dev` 库中的激光产品要同步补齐正式模型、对象洞察、证据、发布批次与风险目录，形成与现有裂缝最小切片同等级的可维护状态。

## 3. 方案选择与取舍

本轮评估过 3 条路径：

### 3.1 路径 A：只修数据库展示文案

只修激光产品 `iot_product_model.model_name` 和 `metadata_json.objectInsight`，不改后端场景识别。

问题：

1. compare、证据沉淀、发布批次和风险目录仍不会自动走通。
2. 代码仍把激光视为“无规范场景”，后续再次 compare 仍会出现治理断点。
3. 只能修一次库表，不能形成稳定可维护逻辑。

### 3.2 路径 B：复用裂缝底层链，但为激光增加产品级语义别名

这是本轮选定方案。

优点：

1. 不新增第三条规范场景，实施成本最低。
2. 保留现有 `L1_LF_* -> value / sensor_state` 稳定链路，不影响协议、关系主数据和风险桥层。
3. 可以通过产品级语义别名把对客展示和治理说明收口为“激光测距”，避免继续串用裂缝文案。
4. 能顺手把共享 `dev` 库中已经存在的激光产品补到正式状态。

代价：

1. `phase1-crack` 底层语义不再只服务“裂缝”这个客户词汇，而是提升为“单值位移类 `value / sensor_state` 规范底座”。
2. 代码里需要额外处理“规范标识复用，但展示名称按产品族隔离”的逻辑。

### 3.3 路径 C：新开激光测距专属规范场景

优点：

1. 场景最纯粹，语义最独立。

问题：

1. 需要同时扩 `ProductModelNormativeMatcher`、规范字段库、证据、发布批次、风险目录、映射规则和测试矩阵。
2. 对当前“能复用就复用”的目标来说，收益不足以覆盖改造成本。
3. 会把本轮“收口激光产品”变成“新建第三条治理主线”。

## 4. 目标状态

### 4.1 产品与关系层

激光测距产品继续固定为：

1. 产品标识：`nf-monitor-laser-rangefinder-v1`
2. 产品名称：`南方测绘 监测型 激光测距仪`
3. 父子关系策略：
   - `relationType = collector_child`
   - `canonicalizationStrategy = LF_VALUE`
   - `statusMirrorStrategy = SENSOR_STATE`

本轮不改你提供的 `10` 组父设备与 `54` 条关系，只要求它们继续稳定指向激光产品。

### 4.2 正式物模型层

激光产品正式 `property` 固定为 `2` 条：

| identifier | modelName | dataType | 说明 |
|---|---|---|---|
| `value` | 激光测距值 | `double` | 激光测距主监测值，来自 `L1_LF_*` 逻辑通道业务值 |
| `sensor_state` | 传感器状态 | `integer` | 激光测距传感器状态，来自 `S1_ZT_1.sensor_state.L1_LF_*` |

补充口径：

1. `identifier` 不改，继续复用底层规范字段 `value / sensor_state`。
2. `modelName` 改成激光语义，不再出现“裂缝值 / 裂缝状态”。
3. 不把父采集器自身状态字段纳入激光子产品正式模型。

### 4.3 compare 与规范元信息层

激光产品 compare 返回值需满足以下要求：

1. `normativeIdentifier` 继续返回底层规范标识：
   - `value`
   - `sensor_state`
2. `riskReady` 继续复用裂缝场景规则：
   - `value = true`
   - `sensor_state = false`
3. `rawIdentifiers[]` 继续保留真实原始字段别名：
   - `L1_LF_1`
   - `S1_ZT_1.sensor_state.L1_LF_1`
4. `normativeName` 对激光产品必须按激光语义展示，不得复用“裂缝监测值”这类客户文案。

映射固定如下：

| productKey | normativeIdentifier | 返回给页面的 normativeName |
|---|---|---|
| `nf-monitor-laser-rangefinder-v1` | `value` | 激光测距值 |
| `nf-monitor-laser-rangefinder-v1` | `sensor_state` | 传感器状态 |
| 其他 `phase1-crack` 产品 | `value` | 裂缝监测值 |
| 其他 `phase1-crack` 产品 | `sensor_state` | 传感器状态 |

### 4.4 对象洞察层

激光产品 `iot_product.metadata_json.objectInsight.customMetrics[]` 需要补齐为正式真相源。

本轮默认只写入 `1` 条：

```json
{
  "objectInsight": {
    "customMetrics": [
      {
        "identifier": "value",
        "displayName": "激光测距值",
        "group": "measure",
        "includeInTrend": true,
        "includeInExtension": false,
        "enabled": true,
        "sortNo": 10
      }
    ]
  }
}
```

补充口径：

1. `sensor_state` 仍保留正式字段，但默认不加入趋势。
2. 若后续业务明确要求“激光状态趋势”，再通过既有 `/products -> 当前已生效字段` 快捷动作加入，不在本轮默认扩出。

### 4.5 风险目录层

激光产品继续复用 `phase1-crack` 风险目录发布规则：

1. `value` 发布到 `risk_metric_catalog`
2. `sensor_state` 不发布到 `risk_metric_catalog`

`risk_metric_catalog` 对激光产品的目录行仍使用：

1. `contract_identifier = value`
2. `normative_identifier = value`
3. 风险指标名称走激光语义，例如 `激光测距值`

## 5. 代码设计

### 5.1 场景识别：让激光产品进入既有规范链

`ProductModelNormativeMatcher` 当前只识别：

1. `phase1-crack`
2. `phase2-gnss`

本轮需要新增“激光测距产品复用裂缝场景”的识别规则。

匹配条件固定为：

1. `productKey` 包含：
   - `laser-rangefinder`
   - `laser_rangefinder`
   - `south_laser_rangefinder`
2. `productName` 或 `description` 包含：
   - `激光`
   - `测距`
   - `激光测距`

命中后统一返回：

`phase1-crack`

这样以下链路都能自动激活：

1. compare 规范元信息装饰
2. manual evidence 沉淀
3. vendor mapping rule 场景过滤
4. release batch 创建
5. risk catalog 发布

### 5.2 规范展示别名：底层复用，页面隔离

当前 `normativeName` 直接取自 `iot_normative_metric_definition.display_name`。

但共享 `dev` 库和 `sql/init-data.sql` 里的 `phase1-crack` 规范字段名当前是：

1. `value -> 裂缝监测值`
2. `sensor_state -> 传感器状态`

本轮需要在 compare 结果装饰阶段新增产品级展示别名逻辑。

实现位置固定为：

1. 继续在 `ProductModelServiceImpl.decorateCompareResultWithNormativeMetadata(...)` 里处理
2. 保留 `normativeIdentifier` 不变
3. 对 `nf-monitor-laser-rangefinder-v1` 单独覆写 `normativeName`

规则固定为：

1. 激光产品 `value -> 激光测距值`
2. 激光产品 `sensor_state -> 传感器状态`

这样可以保证：

1. compare 卡片不会再显示“裂缝监测值”
2. 风险与目录底层仍按 `value` 统一
3. 不需要改 UI 组件结构

### 5.3 正式字段命名与对象洞察命名同步

当前前端 `/products`、`/insight` 和对象洞察配置都以正式 `modelName` 与 `metadata_json.objectInsight.customMetrics[].displayName` 为主展示基线。

因此本轮除了返回正确的 compare `normativeName`，还必须同步保证：

1. 激光产品 `iot_product_model.model_name` 改成激光语义
2. 激光产品 `metadata_json.objectInsight.customMetrics[].displayName` 改成激光语义

否则 compare 页面修好了，正式字段列表和对象洞察仍会露出旧裂缝文案。

### 5.4 不改动的部分

本轮明确不改：

1. `canonicalizeCompositeIdentifier(...)` 中 `L1_LF_* -> value` 的规则
2. `inferCompositeCanonicalizationStrategy(...)` 默认 `LF_VALUE`
3. `S1_ZT_1.sensor_state.L1_LF_* -> sensor_state` 的镜像规则
4. 现有 `ProductModelGovernanceCompareTable.vue` 的展示结构

原因：

1. 激光与裂缝当前都能稳定复用这条底层 canonical 链。
2. 真正的问题在“场景识别”和“展示语义”，不是 canonical 本身。

## 6. 数据落库设计

### 6.1 需要修正的共享 dev 基线

共享 `dev` 库当前至少需要补齐以下数据：

1. `iot_normative_metric_definition`
   - 当前查询结果为空表，必须先对齐 `sql/init-data.sql` 中已存在的 `phase1-crack / phase2-gnss` 种子数据
2. 激光产品 `iot_product_model`
   - 把 `value / sensor_state` 中文名修正为激光语义
3. 激光产品 `iot_product.metadata_json`
   - 补齐对象洞察重点指标配置
4. 激光产品 `iot_vendor_metric_evidence`
   - 补齐或回补 `value / sensor_state` 相关证据
5. 激光产品 `iot_product_contract_release_batch`
   - 若不存在正式发布批次，则补一笔激光产品正式发布批次
6. 激光产品 `risk_metric_catalog`
   - 若不存在目录指标，则补一笔 `value` 风险目录

### 6.2 正式模型修正原则

激光产品 `iot_product_model` 的修正必须满足：

1. 不新增额外字段
2. 不把 `L1_LF_1`、`L1_LF_2` 一类原始通道名写进正式模型
3. 只保留 `value / sensor_state`
4. 只改中文名和必要的 `specs_json`

### 6.3 对象洞察修正原则

激光产品 `metadata_json.objectInsight.customMetrics[]` 必须与正式字段同步：

1. 仅保留 `value`
2. `group = measure`
3. `displayName = 激光测距值`
4. 不把 `sensor_state` 作为默认重点趋势

### 6.4 证据与批次补账原则

激光产品当前缺少治理支撑表闭环，但裂缝产品已有现成模式。

本轮补账原则：

1. 若激光产品已有证据，则尽量更新而不是重建
2. 若激光产品尚无正式发布批次，则补建 `1` 个批次
3. 若激光产品尚无 `risk_metric_catalog(value)`，则补建 `1` 条目录指标
4. `sensor_state` 不进入目录

### 6.5 对裂缝产品的保护边界

共享 `dev` 库中裂缝产品 `nf-monitor-crack-meter-v1` 当前仍有：

1. `3` 台直连设备
2. 已存在的 `phase1-crack` 发布批次
3. 已存在的厂商字段证据

这些数据本轮不迁移、不并表、不重命名。

本轮只处理：

1. 激光产品自身收口
2. 激光产品能复用裂缝底层场景

## 7. 测试与验收设计

### 7.1 后端单测

至少补以下测试：

1. `ProductModelNormativeMatcher`：
   - 激光产品 key/name/description 能识别并返回 `phase1-crack`
2. `ProductModelServiceImplTest`：
   - 激光复合业务样本仍归一为 `value`
   - 激光状态样本仍只镜像 `sensor_state`
   - 激光 compare 返回 `normativeIdentifier = value / sensor_state`
   - 激光 compare 返回 `normativeName = 激光测距值 / 传感器状态`
   - 激光 `value` 仍保持 `riskReady = true`
3. `ProductMetricEvidenceServiceImplTest`：
   - 激光产品 manual/runtime evidence 能按 `phase1-crack` 沉淀

### 7.2 数据验证

共享 `dev` 库验收至少包含以下查询结果：

1. 激光产品存在且 `product_key = nf-monitor-laser-rangefinder-v1`
2. 激光产品 `iot_product_model` 只有 `value / sensor_state`
3. 激光产品 `model_name` 已是激光语义
4. 激光产品 `metadata_json.objectInsight.customMetrics[]` 已存在且只含 `value`
5. 激光产品 `iot_vendor_metric_evidence` 已有 `value / sensor_state` 相关记录
6. 激光产品 `iot_product_contract_release_batch` 至少存在 `1` 条有效发布批次
7. 激光产品 `risk_metric_catalog` 至少存在 `value` 目录记录
8. 你提供的 `10` 组父设备关系继续全部指向激光产品

### 7.3 页面与接口验收

手工或浏览器验收至少验证以下场景：

1. `/products` 打开激光产品工作台
2. `当前已生效字段` 中不再显示“裂缝值 / 裂缝状态”
3. compare 卡片中的 `规范字段` 不再显示“裂缝监测值”
4. `设为监测数据 / 设为状态事件 / 设为运行参数` 仍能正常更新对象洞察配置
5. `/insight` 若读取激光子设备，趋势与快照中文名使用激光语义

## 8. 风险与防守

### 8.1 风险：共享 dev 库规范字段库为空

影响：

1. compare 虽然能识别场景，但拿不到 `normativeName / riskReady`
2. 激光闭环无法真正复用裂缝最小切片

防守：

1. 先把共享 `dev` 库对齐到 `sql/init-data.sql` 里的规范字段种子
2. 再执行激光产品收口 SQL

### 8.2 风险：前端仍直接展示后端返回的旧 `normativeName`

影响：

1. compare 卡片会继续出现裂缝文案

防守：

1. 在后端 compare 装饰阶段直接返回激光语义 `normativeName`
2. 不依赖前端做产品特判

### 8.3 风险：补批次时误伤裂缝产品现有数据

影响：

1. 可能造成裂缝产品批次和目录错乱

防守：

1. 补账 SQL 严格限定 `product_id = 202603192100560258`
2. 不修改裂缝产品已有批次和目录

## 9. 回退策略

本轮回退原则如下：

1. 所有数据库修正使用幂等 SQL 或定向更新，不做全表清理。
2. 正式字段若发现错误，优先通过既有发布批次与快照回滚，不引入手工硬删。
3. 激光产品新增的证据、批次和目录都限定在自身产品范围内，便于后续单产品回退。
4. 若后续业务明确要求“激光场景完全独立”，本轮结果仍可作为迁移基线继续演进，不会阻断后续拆场景。

## 10. 文档同步要求

实施阶段如果按本设计落地，至少需要同步更新以下文档：

1. `README.md`
2. `AGENTS.md`
3. `docs/02-业务功能与流程说明.md`
4. `docs/03-接口规范与接口清单.md`
5. `docs/04-数据库设计与初始化数据.md`
6. `docs/08-变更记录与技术债清单.md`

文档更新重点：

1. 明确激光测距产品复用 `phase1-crack` 底层治理链，但正式展示语义独立于裂缝产品
2. 明确激光产品的正式字段口径为 `value / sensor_state`
3. 明确对象洞察默认只纳入 `value`
4. 明确共享 `dev` 库已补齐激光产品治理基线
