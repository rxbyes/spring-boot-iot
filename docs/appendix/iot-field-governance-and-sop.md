# IoT Field Governance And SOP

> 文档定位：面向业务人员的字段治理与建档联调补充指引。
> 适用角色：业务、交付、实施、联调、运维。
> 权威级别：附录补充；业务语义仍以 [../02-业务功能与流程说明.md](../02-业务功能与流程说明.md) 为准，交付边界仍以 [../21-业务功能清单与验收标准.md](../21-业务功能清单与验收标准.md) 与 [../19-第四阶段交付边界与复验进展.md](../19-第四阶段交付边界与复验进展.md) 为准。
> 更新时间：2026-04-23

## 1. 使用方式

本附录只回答三件事：

1. 如何把监测规范表转成平台字段治理标准。
2. 业务人员建产品、建设备、配模型时该填什么。
3. 联调时应该检查 latest、telemetry、message-log 哪些结果。

建议业务人员按以下顺序使用本附录：

1. 先看“变形监测字段治理标准表”，明确监测内容、监测类型和字段集合。
2. 再看“建档前准备”，确认产品、设备、物模型需要准备的录入信息。
3. 联调时按“业务建档与联调 SOP”和“联调核对清单”逐项执行。

## 2. 变形监测字段治理标准表

### 2.1 使用原则

下表用于把“监测内容编码 / 监测类型编码 / 数据字段 / 单位 / 备注”收口为平台可执行的字段标准。

业务上固定这样使用：

1. 先按 `监测内容编码` 找到业务大类。
2. 再按 `监测类型编码` 找到该类设备的字段集合。
3. 再判断每个字段属于：
   - 必传字段
   - 可选字段
   - 核心判定字段
   - 原始资料字段
   - 周期聚合字段
4. 最后再决定这些字段是否进入：
   - 产品正式物模型
   - latest 展示
   - telemetry 落库
   - 风险规则
   - 报表分析

### 2.2 首批字段标准

| 监测内容编码 | 监测类型编码 | 字段标识 | 中文名称 | 单位 | latest | telemetry | 核心判定字段 | 平台计算 | 上报要求 | 业务解释 |
|---|---|---|---|---|---|---|---|---|---|---|
| L1 | LF | value | 裂缝张开度 | mm | 是 | 是 | 是 | 否 | 必传 | 裂缝位移随时间的累计变化量 |
| L1 | GP | gpsInitial | GNSS 原始观测基础数据 | - | 可选 | 可选 | 否 | 否 | 条件必传 | 作为累计位移的计算基础，不直接作为判警主指标 |
| L1 | GP | gpsTotalX | X 方向累计位移量 | mm | 是 | 是 | 是 | 是 | 建议必传 | 相对初始位置的累计变形量 |
| L1 | GP | gpsTotalY | Y 方向累计位移量 | mm | 是 | 是 | 是 | 是 | 建议必传 | 相对初始位置的累计变形量 |
| L1 | GP | gpsTotalZ | Z 方向累计位移量 | mm | 是 | 是 | 是 | 是 | 建议必传 | 相对初始位置的累计变形量 |
| L1 | SW | dispsX | 顺滑方向累计变形量 | mm | 是 | 是 | 是 | 是 | 必传 | 深部位移主判定指标 |
| L1 | SW | dispsY | 垂直坡面方向累计变形量 | mm | 是 | 是 | 是 | 是 | 必传 | 深部位移主判定指标 |
| L1 | JS | gX | X 轴加速度周期代表值 | m/s² | 是 | 是 | 视规则而定 | 是 | 必传 | 一个上传周期内绝对值最大时刻的轴向值 |
| L1 | JS | gY | Y 轴加速度周期代表值 | m/s² | 是 | 是 | 视规则而定 | 是 | 必传 | 一个上传周期内绝对值最大时刻的轴向值 |
| L1 | JS | gZ | Z 轴加速度周期代表值 | m/s² | 是 | 是 | 视规则而定 | 是 | 必传 | 一个上传周期内绝对值最大时刻的轴向值 |

### 2.3 未形成正式字段时的名称/单位治理

当现场已经看到 latest / history / `/insight` 中出现 raw identifier，但该字段还没有进入正式合同时，当前统一按下面规则处理：

1. 不把 `sys_dict` 当作字段名称/单位真相源，也不在采集链路上强制把上报字段改成中文名称。
2. 正式字段一旦形成，名称和单位仍只以 `iot_product_model` / 已发布合同快照为准。
3. 正式字段尚未形成前，如需先补中文名称或单位，统一到 `/products/:productId/mapping-rules -> 运行态名称/单位治理` 维护。
4. 运行态治理只影响读侧展示，不会把 raw identifier 直接写成正式字段，也不会替代 `contracts` 页的 compare/apply。
5. 对象洞察、属性快照和历史趋势当前统一按 `正式字段 > 运行态显示规则 > latest 属性 > raw identifier` 的顺序取展示信息。

### 2.4 字段分类规则

为避免业务、交付、研发对同一字段有不同理解，字段必须归类：

1. `核心判定字段`
   - 直接进入阈值规则、风险判定、告警和报表。
2. `辅助分析字段`
   - 用于详情解释、趋势补充，不直接触发规则。
3. `原始资料字段`
   - 用于保留原始基础数据，不应直接作为主判定字段。
4. `周期聚合字段`
   - 表示一个上传周期内提炼后的代表值，而非原始连续波形。

### 2.5 字段治理时必须额外补齐的列

如果后续把这张表继续扩为正式模板，建议再增加这些列：

1. 中文名称
2. 数据类型
3. 是否必传
4. 是否 latest
5. 是否 telemetry
6. 是否核心判定字段
7. 是否平台计算
8. 是否允许空值
9. 风险规则是否直接消费
10. 报表是否直接消费
11. 责任归属
    - 设备侧计算 / 平台侧计算 / 人工维护

### 2.6 南方测绘多维检测仪单设备多能力导入口径

针对 `SK11E80D1307426AZ` 这类“一台设备同时承载倾角、加速度、裂缝”的场景，当前推荐固定为：

1. `1 个产品`
2. `N 台设备`
3. `24 个 property 定义`

不拆父子设备，不拆多个产品。样例中的 `SK11E80D1307426AZ` 只是该产品下的一台设备实例；同类设备继续复用同一产品，并按同一套 property 标识建档和上报。

推荐产品档案：

1. `productName`：`南方测绘多维检测仪`
2. `productKey`：`south-survey-multi-detector-v1`
3. `manufacturer`：`南方测绘`
4. `description`：`单台设备同时承载倾角、加速度、裂缝三类监测能力，状态组统一通过 S1_ZT_1 上报。`

### 2.7 可直接录入 `iot_product_model` 的字段清单

监测测点字段：

| sortNo | identifier | modelName | dataType | requiredFlag | specsJson 模板 | description |
|---|---|---|---|---|---|---|
| 10 | `L1_QJ_1.X` | 倾角测点 X 轴倾角 | `double` | `1` | `qjCore` | 倾角测点 X 轴与水平面的夹角，建议进入 latest 和 telemetry。 |
| 20 | `L1_QJ_1.Y` | 倾角测点 Y 轴倾角 | `double` | `1` | `qjCore` | 倾角测点 Y 轴与水平面的夹角，建议进入 latest 和 telemetry。 |
| 30 | `L1_QJ_1.Z` | 倾角测点 Z 轴倾角 | `double` | `1` | `qjCore` | 倾角测点 Z 轴与水平面的夹角，建议进入 latest 和 telemetry。 |
| 40 | `L1_QJ_1.angle` | 倾角测点平面夹角 | `double` | `1` | `qjCore` | X/Y 轴形成平面与水平面的夹角。 |
| 50 | `L1_QJ_1.AZI` | 倾角测点方位角 | `double` | `1` | `qjAzi` | X 轴在水平面的投影与磁北夹角。 |
| 60 | `L1_JS_1.gX` | 加速度测点 X 轴加速度 | `double` | `1` | `jsCycle` | 一个上传周期内绝对值最大时刻的 X 轴代表值。 |
| 70 | `L1_JS_1.gY` | 加速度测点 Y 轴加速度 | `double` | `1` | `jsCycle` | 一个上传周期内绝对值最大时刻的 Y 轴代表值。 |
| 80 | `L1_JS_1.gZ` | 加速度测点 Z 轴加速度 | `double` | `1` | `jsCycle` | 一个上传周期内绝对值最大时刻的 Z 轴代表值。 |
| 90 | `L1_LF_1.value` | 裂缝测点张开度 | `double` | `1` | `lfCore` | 裂缝张开度，表示累计变化量。 |

设备状态字段：

| sortNo | identifier | modelName | dataType | requiredFlag | specsJson 模板 | description |
|---|---|---|---|---|---|---|
| 110 | `S1_ZT_1.battery_dump_energy` | 电池剩余电量 | `integer` | `0` | `statusPercent` | 电池剩余电量，用于设备健康判断。 |
| 120 | `S1_ZT_1.ext_power_volt` | 外部供电电压 | `double` | `0` | `statusVolt` | 外部供电电压。 |
| 130 | `S1_ZT_1.humidity` | 设备湿度 | `double` | `0` | `statusHumidity` | 终端环境湿度。 |
| 140 | `S1_ZT_1.temp` | 设备温度 | `double` | `0` | `statusTemp` | 终端环境温度。 |
| 150 | `S1_ZT_1.signal_4g` | 4G 信号强度 | `integer` | `0` | `status4gSignal` | 4G 通信强度。 |
| 160 | `S1_ZT_1.signal_NB` | NB 信号强度 | `integer` | `0` | `statusNbSignal` | NB 通信强度。 |
| 170 | `S1_ZT_1.signal_bd` | 北斗信号指标 | `integer` | `0` | `statusBdSignal` | 北斗通信或定位相关状态指标。 |
| 180 | `S1_ZT_1.solar_volt` | 太阳能电压 | `double` | `0` | `statusVolt` | 太阳能供电电压。 |
| 190 | `S1_ZT_1.sw_version` | 软件版本 | `string` | `0` | `statusVersion` | 固件或软件版本号。 |
| 200 | `S1_ZT_1.pa_state` | PA 状态 | `bool` | `0` | `statusBool` | 设备开关类状态。 |
| 210 | `S1_ZT_1.lat` | 纬度 | `double` | `0` | `statusLatitude` | 设备定位纬度。样例为字符串时也建议按 `double` 入正式模型。 |
| 220 | `S1_ZT_1.lon` | 经度 | `double` | `0` | `statusLongitude` | 设备定位经度。样例为字符串时也建议按 `double` 入正式模型。 |
| 230 | `S1_ZT_1.sensor_state.L1_QJ_1` | 倾角测点传感器状态 | `integer` | `0` | `sensorStateQj` | 倾角测点健康状态码。 |
| 240 | `S1_ZT_1.sensor_state.L1_JS_1` | 加速度测点传感器状态 | `integer` | `0` | `sensorStateJs` | 加速度测点健康状态码。 |
| 250 | `S1_ZT_1.sensor_state.L1_LF_1` | 裂缝测点传感器状态 | `integer` | `0` | `sensorStateLf` | 裂缝测点健康状态码。 |

### 2.8 `specsJson` 模板

`qjCore`

```json
{
  "unit": "°",
  "precision": 4,
  "monitorContentCode": "L1",
  "monitorTypeCode": "QJ",
  "sensorCode": "L1_QJ_1",
  "latest": true,
  "telemetry": true,
  "metricCategory": "telemetry",
  "coreMetric": true
}
```

`qjAzi`

```json
{
  "unit": "°",
  "precision": 4,
  "monitorContentCode": "L1",
  "monitorTypeCode": "QJ",
  "sensorCode": "L1_QJ_1",
  "latest": true,
  "telemetry": true,
  "metricCategory": "telemetry",
  "coreMetric": false,
  "valueNature": "azimuth"
}
```

`jsCycle`

```json
{
  "unit": "m/s²",
  "precision": 4,
  "monitorContentCode": "L1",
  "monitorTypeCode": "JS",
  "sensorCode": "L1_JS_1",
  "latest": true,
  "telemetry": true,
  "metricCategory": "telemetry",
  "coreMetric": false,
  "valueNature": "cycle_representative",
  "ruleUsage": "conditional"
}
```

`lfCore`

```json
{
  "unit": "mm",
  "precision": 4,
  "monitorContentCode": "L1",
  "monitorTypeCode": "LF",
  "sensorCode": "L1_LF_1",
  "latest": true,
  "telemetry": true,
  "metricCategory": "telemetry",
  "coreMetric": true,
  "valueNature": "cumulative_change"
}
```

`statusPercent`

```json
{
  "unit": "%",
  "precision": 0,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`statusVolt`

```json
{
  "unit": "V",
  "precision": 2,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`statusHumidity`

```json
{
  "unit": "%RH",
  "precision": 1,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`statusTemp`

```json
{
  "unit": "℃",
  "precision": 1,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`status4gSignal`

```json
{
  "unit": "dBm",
  "precision": 0,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`statusNbSignal`

```json
{
  "unit": "dBm",
  "precision": 0,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`statusBdSignal`

```json
{
  "unit": "-",
  "precision": 0,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false
}
```

`statusVersion`

```json
{
  "unit": "-",
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false,
  "valueNature": "version"
}
```

`statusBool`

```json
{
  "unit": "-",
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "coreMetric": false,
  "valueNature": "boolean_state"
}
```

`statusLatitude`

```json
{
  "unit": "°",
  "precision": 6,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "location",
  "coreMetric": false
}
```

`statusLongitude`

```json
{
  "unit": "°",
  "precision": 6,
  "sensorCode": "S1_ZT_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "location",
  "coreMetric": false
}
```

`sensorStateQj`

```json
{
  "unit": "状态码",
  "sensorCode": "S1_ZT_1",
  "relatedSensorCode": "L1_QJ_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "statusDomain": "sensor_state",
  "coreMetric": false
}
```

`sensorStateJs`

```json
{
  "unit": "状态码",
  "sensorCode": "S1_ZT_1",
  "relatedSensorCode": "L1_JS_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "statusDomain": "sensor_state",
  "coreMetric": false
}
```

`sensorStateLf`

```json
{
  "unit": "状态码",
  "sensorCode": "S1_ZT_1",
  "relatedSensorCode": "L1_LF_1",
  "latest": true,
  "telemetry": false,
  "metricCategory": "device_status",
  "statusDomain": "sensor_state",
  "coreMetric": false
}
```

### 2.9 物模型治理模块的规范优先口径

当前产品物模型治理模块默认按照“规范证据 + 报文证据 + 正式模型基线 -> compare -> apply”执行，不再把手动样本提炼作为默认 compare 主入口。

当前首批规范预设：

1. 预设编码：`landslide-integrated-tilt-accel-crack-v1`
2. 工作台标题：`倾角 / 加速度 / 裂缝一体机`
3. 默认勾选字段：
   - `L1_QJ_1.X`
   - `L1_QJ_1.Y`
   - `L1_QJ_1.Z`
   - `L1_QJ_1.angle`
   - `L1_QJ_1.AZI`
4. 可继续扩展到：
   - `L1_JS_1.gX / gY / gZ`
   - `L1_LF_1.value`
   - `S1_ZT_1.signal_4g`

补充说明：

1. 规范模式下 compare 行当前围绕规范化 `identifier` 建行，原始字段名只保留在 `rawIdentifiers[]` 中。
2. 报文证据命中别名映射后会直接按规范 `identifier` 归一展示，例如原始字段 `X` 会以 `L1_QJ_1.X` 进入 compare 行。
3. 样本 JSON 与人工补录仍保留，但当前只作为辅助证据入口，不再默认主导 compare。

#### 2.8.1 手动样本提炼口径

手动提炼固定规则：

1. 只服务于当前选中的产品。
2. 单次只支持 `1` 个设备样本。
3. 样本类型只允许：
   - `business`
   - `status`
   - `other`
4. 请求体通过 `POST /api/device/product/{productId}/model-candidates/manual-extract` 提交：

```json
{
  "sampleType": "business",
  "samplePayload": "{\"SK11E80D1307426AZ\":{\"L1_QJ_1\":{\"2026-03-31T04:05:55.000Z\":{\"AZI\":-8.6772,\"X\":-0.0376,\"Y\":-0.0567,\"Z\":-0.0292,\"angle\":83.0074}},\"L1_JS_1\":{\"2026-03-31T04:05:55.000Z\":{\"gX\":-0.2396,\"gY\":-1.1563,\"gZ\":-0.3125}},\"L1_LF_1\":{\"2026-03-31T04:05:55.000Z\":{\"value\":0.0305}}}}"
}
```

补充说明：

1. `samplePayload` 必须是 JSON 字符串，且根节点只能有 `1` 个设备编码。
2. 时间戳层级会自动剥离，例如 `2026-03-31T04:05:55.000Z` 不会进入正式 `identifier`。
3. 对象会继续下钻到标量叶子后再生成属性候选，例如 `sensor_state.L1_QJ_1` 会提炼为 `S1_ZT_1.sensor_state.L1_QJ_1`。
4. 数组、空对象和不可识别结构不会生成属性，只会累计到 `ignoredFieldCount`。
5. `other` 类型提炼出的字段会默认标记 `needsReview=true`，必须先人工归类后再确认写库。
6. 手动提炼当前只自动生成 `property` 候选；事件和服务仍由正式模型人工补录。

当前 `/products -> 契约字段` 样本提取的正式字段口径补充如下：

| 场景 | 正式字段标识怎么收口 | 典型示例 |
| --- | --- | --- |
| 单台单能力 / 规范产品 | 以 direct canonical 字段为主，不再额外保留监测类型前缀。 | `value`、`sensor_state`、`gpsTotalX` |
| 单台多能力产品 | 保留 `监测类型编码 + 数据字段` 全路径，避免多个测点/能力挤压成同名字段。 | `L1_QJ_1.angle`、`L1_JS_1.gX`、`S1_ZT_1.sensor_state.L1_LF_1` |
| 复合父设备 | 父设备自身状态字段继续留在父产品，正式合同仍以 direct 字段为主；监测类型编码 / 全路径只留在归一线索和原始证据。 | `ext_power_volt`、`temp`、`signal_4g` |
| 复合子设备 | 逻辑通道编码只用于归属与证据，最终按关系映射策略归一为子产品 canonical direct 字段。 | `value`、`sensor_state`、`dispsX`、`dispsY` |

额外约束：

1. `sampleType + deviceStructure` 只决定解析路径，不单独决定正式字段命名。
2. 当单台状态样本本身承载多能力状态组，或上报 4G、剩余电量、温湿度、电压等设备自有状态参数时，应按产品正式契约形态保留全路径，而不是强行压成统一短标识。
3. 当复合样本命中 `relationMappings / iot_device_relation` 时，`logicalChannelCode` 只作为“这条字段属于哪个子设备”的证据，不直接作为最终正式字段名。
4. compare 请求体允许隐藏透传 `manualExtract.contractIdentifierMode=DIRECT|FULL_PATH` 作为强制覆盖；页面默认不新增切换，继续按“单台设备（按产品形态自动识别）”由后端解析。
5. compare 里运行态单独命中的“继续观察”字段，必须与本次 compare 已解析出的 `resolvedContractIdentifierMode` 保持一致；若正式/手动样本保留全路径，运行态补证也必须保留同一全路径，不得再退化成尾字段短标识。

#### 2.8.2 未来设备接入执行清单（提炼前 / 提炼后 / 发布前）

以下清单用于“新设备尚未完整接入时”的扩展治理，默认目标是先做到“按上报编码自动识别”，再按需人工提炼补齐。

**A. 提炼前检查（样本准备与编码判定）**

1. 确认样本是否为 `1` 台设备 JSON，且样本类型已明确为 `business` 或 `status`。
2. 先识别测点编码是否符合 `Lx_XX_n / Sx_XX_n`，并记录 `监测内容编码` 与 `监测类型编码`。
3. 识别上报结构是“标量值”还是“对象叶子”，对象叶子需明确字段叶子（如 `value/temp/totalValue`）。
4. 确认该产品是否已命中专用规范场景；未命中时，改走“编码前缀 + 叶子字段”兜底识别。
5. 对照当前正式字段，确认同一语义是否已存在正式 `identifier`，避免重复提炼为第二套标识。

**B. 提炼后检查（compare 结果与候选治理）**

1. 核对 compare 行是否回填 `normativeIdentifier / normativeName / riskReady / rawIdentifiers`。
2. 若同一语义出现两行（如 `L4_NW_1` 与 `L4_NW_1.value`），必须先做语义归一再 apply。
3. 核对 `resolvedContractIdentifierMode` 是否在同次 compare 内保持一致，避免手动样本与运行态补证标识风格分裂。
4. 对无法自动命中的字段，使用手动提炼确认中文名、单位和归属，并同步评估是否需要新增映射规则或运行态显示规则。
5. 对状态镜像字段（如 `S1_ZT_1.sensor_state.<logicalChannelCode>`），继续按状态治理语义处理，不把其误收口为业务测值字段。

**C. 发布前检查（合同发布与风险闭环）**

1. 复核“正式字段标识唯一性”：同一语义只保留一条正式字段，不允许 path/bare 双轨并存。
2. 复核单位与中文名：正式字段以 `iot_product_model` 为真相；未成正式字段仅通过运行态显示规则治理。
3. 复核风险边界：仅 `riskEnabled=1` 指标进入风险闭环；`riskEnabled=0` 指标只用于展示与分析。
4. 复核对象洞察影响：确认趋势和快照不会因新字段引入重复行或字段别名冲突。
5. 完成 `contracts` 发布前最后核对：确认本次 apply 仅包含“可直接生效”且语义已统一的字段。

补充口径：

1. 新设备接入初期优先保证“能识别、可解释、可追溯”，不要求一次性把全部字段都纳入风险闭环。
2. 若字段暂不进入正式合同，可先通过 `/products/:productId/mapping-rules` 的“运行态名称/单位治理”保障读侧可用。
3. 一旦形成正式字段并发布，后续读侧展示与风险消费都应以正式字段真相为准，不再回退到临时别名。

### 2.10 调用接口时直接复用的请求体资产

当前接口 `POST /api/device/product/{productId}/models` 一次只接受 `1` 条 `ProductModelUpsertDTO`，不支持数组批量提交。

为避免手抄 `24` 次请求体，仓库已补充可直接循环提交的资产文件：

- [south-survey-multi-detector-product-model-request-bodies.json](./south-survey-multi-detector-product-model-request-bodies.json)

使用方式：

1. 先把目标产品建好，拿到 `productId`
2. 再按数组顺序逐条调用 `POST /api/device/product/{productId}/models`
3. 若中途已有同名 `identifier`，接口会按当前唯一约束返回冲突

Windows PowerShell 示例：

```powershell
$productId = 10001
$token = '<JWT_TOKEN>'
$baseUrl = 'http://localhost:8080'
$items = Get-Content -Path 'docs/appendix/south-survey-multi-detector-product-model-request-bodies.json' -Encoding UTF8 | ConvertFrom-Json

foreach ($item in $items) {
  Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/device/product/$productId/models" `
    -Headers @{
      Authorization = "Bearer $token"
      'Content-Type' = 'application/json'
    } `
    -Body ($item | ConvertTo-Json -Compress)
}
```

说明：

1. 资产文件中的每一项都已经是接口实际需要的请求体结构。
2. `specsJson` 当前按服务端约束保持为“合法 JSON 字符串”，不是嵌套对象。
3. 若只想先落监测主字段，可以先提交 `sortNo 10~90` 的 `9` 条测点属性，再补 `110~250` 的状态属性。

## 3. 建档前准备

### 3.1 产品建档前必须准备

业务人员至少要准备：

1. `productKey`
2. `productName`
3. `manufacturer`
4. `protocolCode`
5. `nodeType`
6. `dataFormat`
7. 产品业务说明
8. 该产品对应哪些监测类型

产品命名和拆分规则：

1. `productKey` 建议采用 `厂商编码-场景-品类[-协议或版本]`。
2. `productName` 面向人读，建议采用“厂商 + 场景/用途 + 品类”。
3. 如果只是项目名、站点名、安装位置变化，不应新建产品。
4. 如果协议、物模型、Topic、命令能力变化，应评估新建产品。

### 3.2 设备建档前必须准备

业务人员至少要准备：

1. `deviceCode`
2. 所属产品
3. `deviceName`
4. 安装位置
5. 区域归属
6. 组织归属
7. 设备状态
8. 必要的认证信息或扩展信息

如存在父子关系，还需准备：

1. `parentDeviceId` 或 `parentDeviceCode`
2. 父子关系说明
3. 哪个对象承担真实风险责任

### 3.3 物模型建档前必须准备

业务人员至少要准备：

1. `identifier`
2. 中文名称
3. `modelType`
4. `dataType`
5. 单位
6. 是否必填
7. 业务解释
8. 该字段是否 latest
9. 该字段是否 telemetry
10. 该字段是否用于风险规则
11. 若走手动提炼，至少准备 `1` 份当前产品下的单设备样本 JSON

## 4. 业务建档与联调 SOP

### 4.1 建档顺序

1. 先确认监测内容编码和监测类型编码。
2. 创建产品，固定 `productKey / productName / manufacturer / protocolCode / nodeType / dataFormat`。
3. 先通过手动提炼或人工录入配置正式物模型，优先确认核心判定字段。
4. 创建设备，明确 `deviceCode`、区域、组织、安装位置和父子关系。
5. 发起联调，验证 latest、telemetry、message-log 三类结果。
6. 完成风险点绑定和规则配置，再验证告警 / 事件 / 工单闭环。

### 4.2 联调最小接口集

联调时建议至少检查：

1. `GET /api/device/{deviceCode}/properties`
2. `GET /api/device/{deviceCode}/message-logs`
3. `POST /api/device/product/{productId}/model-candidates/manual-extract`
4. `GET /api/telemetry/latest`
5. `GET /api/device/message-flow/session/{sessionId}`
6. `GET /api/device/message-flow/trace/{traceId}`

### 4.3 父设备 / 子设备联调要点

如果是基准站一包多测点场景，联调时应固定核对：

1. 父设备是否保留原始上报和在线状态
2. 子设备是否写入实际业务字段，如 `dispsX / dispsY`
3. 风险点是否绑定在承担监测责任的设备或子设备上
4. 告警 / 事件 / 工单是否围绕正确的责任对象生成

## 5. 联调核对清单

### 5.1 latest 检查

1. latest 字段名是否与物模型一致
2. latest 单位是否与字段标准一致
3. latest 当前值是否和实际上报一致

### 5.2 telemetry 检查

1. telemetry 是否按预期落库
2. telemetry 时间是否与实际上报时间一致
3. 核心判定字段是否已进入时序数据

### 5.3 message-log 检查

1. message-log 是否保留原始报文
2. 是否能看到处理阶段或失败阶段
3. 成功与失败样本是否都能留痕

### 5.4 风险绑定检查

1. 核心判定字段是否已绑定风险点
2. 规则消费的是不是正确字段
3. 父设备 / 子设备责任是否清晰
4. 告警 / 事件 / 工单是否围绕正确主体生成

## 6. 业务人员常见误区

1. 把产品当成一台设备
   - 正确口径：产品是接入模板，设备才是资产实例。
2. 把原始厂家字段直接当正式物模型
   - 正确口径：只有长期消费的字段才进入正式模型。
3. 只看 latest，不看 message-log
   - 正确口径：latest 看结果，message-log 查证据。
4. 把父设备直接当风险责任主体
   - 正确口径：谁承接真实监测值，谁才应承担风险绑定责任。

## 7. 风险点 Pending 人工复核清单

### 7.1 2026-04-04 批量治理后的剩余口径

`2026-04-04` 批量治理执行后，`risk_point_device_pending_binding` 还剩 `17` 条 `manual_review` 记录未自动转正。

这 `17` 条当前有三个共同点：

1. 当前候选都只出现 `gX / gY / gZ`。
2. 当前都还没有正式 `risk_point_device` 绑定。
3. 当前都不建议把 `gX / gY / gZ` 直接写入正式风险绑定。

业务上固定按下面三条判断：

1. `GNSS 位移监测仪`
   - 如果当前只有 `gX / gY / gZ`，先保留 pending，不转正。
   - 优先等待真实运行证据补齐 `gpsTotalX / gpsTotalY / gpsTotalZ`，或先纠正产品归属、设备命名和字段语义。
2. `GNSS 基准站`
   - 如果当前只有 `gX / gY / gZ`，先确认这三个字段是否只是姿态、健康或内部辅助参数。
   - 未确认前不纳入正式风险测点。
3. `倾角仪`
   - 如果当前只有 `gX / gY / gZ`，先确认是否应该继续收口到 `AZI / X / Y / Z / angle`。
   - 未确认前不要把 `gX / gY / gZ` 直接转成正式风险点绑定。

### 7.2 分组概览

| 分组 | 数量 | 当前建议 |
|---|---:|---|
| 倾角仪 `gX/gY/gZ` 复核组 | 4 | 保留 pending，优先补规范字段映射 |
| GNSS 位移监测仪 `gX/gY/gZ` 复核组 | 12 | 保留 pending，等待 `gpsTotalX/Y/Z` 证据或纠正产品归属 |
| GNSS 基准站 `gX/gY/gZ` 复核组 | 1 | 保留 pending，先确认是否属于姿态/健康字段 |

### 7.3 明细清单

| pendingId | 分组 | 风险点 | 设备 | 产品 | 候选测点 | 系统建议 | 业务需确认 |
|---|---|---|---|---|---|---|---|
| 418 | 倾角仪 `gX/gY/gZ` 复核组 | G30连霍高速 K1731+077 | 倾角仪 (`15522566`) | 中海达 监测型 倾角仪 | `gX, gY, gZ` | 暂不转正，等待规范字段或补映射 | 确认该设备是否应继续收口到 `AZI / X / Y / Z / angle` |
| 417 | 倾角仪 `gX/gY/gZ` 复核组 | G30连霍高速 K1731+077 | 倾角仪 (`15522597`) | 中海达 监测型 倾角仪 | `gX, gY, gZ` | 暂不转正，等待规范字段或补映射 | 确认该设备是否应继续收口到 `AZI / X / Y / Z / angle` |
| 416 | 倾角仪 `gX/gY/gZ` 复核组 | G30连霍高速 K1731+077 | 倾角仪 (`15522761`) | 中海达 监测型 倾角仪 | `gX, gY, gZ` | 暂不转正，等待规范字段或补映射 | 确认该设备是否应继续收口到 `AZI / X / Y / Z / angle` |
| 415 | 倾角仪 `gX/gY/gZ` 复核组 | G30连霍高速 K1731+077 | 倾角仪 (`15522772`) | 中海达 监测型 倾角仪 | `gX, gY, gZ` | 暂不转正，等待规范字段或补映射 | 确认该设备是否应继续收口到 `AZI / X / Y / Z / angle` |
| 370 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G22青兰高速平定段K1652+855水毁 | GNSS (`SJ11F1148730978A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 324 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G6京藏高速兰海段K1652+225崩塌 | 浅表位移检测（GNSS）基准点 (`SJ11F1148730969A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 323 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G6京藏高速兰海段K1652+225崩塌 | 浅表位移检测（GNSS）1 (`SJ11F2148734255A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 279 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G568兰永一级K78+965-K79+140 | 固定测斜仪9 (`SJ11F4148737700A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 266 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G75兰海高速兰临段K22+200-K22+400滑坡 | 浅表位移检测（GNSS）基准点 (`SJ11F2148734245A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 263 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G75兰海高速兰临段K22+200-K22+400滑坡 | 浅表位移检测（GNSS）3 (`SJ11F2148734247A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 204 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G30连霍高速宝天段K1329+165 | SK1329+240 基准点 (`SJ11F1148730947`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 178 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G22青兰高速雷西段K1374+670-K1374+780 | GNSS (`SJ11F2148734232A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 43 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G30连霍高速树徐段K1740+300 | 浅表位移检测（GNSS）基准点 (`SJ11F2148734259A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 12 | GNSS 基准站 `gX/gY/gZ` 复核组 | G7011十天高速K595 | DB6-基准点（GNSS） (`SJ11F2148734260A`) | 南方测绘 监测型 GNSS基准站 | `gX, gY, gZ` | 暂不转正，待确认是否单独建基准站规则 | 确认该基准站 `gX/gY/gZ` 是否仅为姿态或健康参数 |
| 11 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G7011十天高速K595 | DB4(GNSS) (`SJ11F1148730971A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 9 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G7011十天高速K595 | DB5(GNSS) (`SJ11F2148734249A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |
| 7 | GNSS 位移监测仪 `gX/gY/gZ` 复核组 | G7011十天高速K595 | DB1(GNSS) (`SJ11F2148734243A`) | 南方测绘 监测型 GNSS位移监测仪 | `gX, gY, gZ` | 暂不转正，不把 `gX/gY/gZ` 直接写入正式绑定 | 核对设备归属与字段语义，若仍属 GNSS 位移监测则等待 `gpsTotalX/Y/Z` |

### 7.4 业务确认后的推进建议

1. 如果业务确认这些记录本质上属于 `gpsTotalX / gpsTotalY / gpsTotalZ` 或 `AZI / X / Y / Z / angle`，先补产品映射或字段归一，再转正。
2. 如果业务确认这些字段只是姿态、健康、调试或厂家内部参数，则保持 pending 或直接排除，不进入正式 `risk_point_device`。
3. 如果业务确认产品归属挂错，先改产品或设备归属，再重新收集运行期证据，不要直接沿用当前 `gX / gY / gZ` 结果转正。

### 7.5 脚本化运营动作

当前推荐把 `manual_review` 的业务确认固定收口为下面两步：

1. 先导出业务确认模板
   - 命令：`python3 scripts/manage-risk-point-pending-governance.py export-manual-review --manifest <manifest> --output-prefix <prefix>`
   - 产物：`<prefix>.json`、`<prefix>.csv`、`<prefix>.md`
   - 用途：给业务、实施、交付逐条确认“继续等待 / 补规范映射 / 修正产品归属 / 直接排除”
2. 再回写分组化复核备注
   - 命令：`python3 scripts/manage-risk-point-pending-governance.py annotate-manual-review --manifest <manifest>`
   - 默认：只做 dry-run 预览，不改库
   - 实际写回：`python3 scripts/manage-risk-point-pending-governance.py annotate-manual-review --manifest <manifest> --apply`
   - 作用：只更新 `risk_point_device_pending_binding.resolution_note`，不改变 `resolution_status`
3. 业务填完 CSV 后再执行决策
   - 命令：`python3 scripts/manage-risk-point-pending-governance.py apply-manual-review-decisions --csv <csv>`
   - 默认：只做 dry-run 预览，不改库
   - 实际执行：`python3 scripts/manage-risk-point-pending-governance.py apply-manual-review-decisions --csv <csv> --apply`
   - 作用：把 `PROMOTE / IGNORE / KEEP_PENDING` 三类决策真正执行到系统中

这一步的业务边界必须固定：

1. `annotate-manual-review` 不是转正命令。
2. `annotate-manual-review` 不是忽略命令。
3. 它只负责把“为什么暂不转正、下一步该核对什么”写回复核备注，方便后续业务确认和批量追踪。

业务填写 CSV 时固定遵守下面的列规则：

1. `business_decision`
   - 允许值：`PROMOTE`、`IGNORE`、`KEEP_PENDING`
2. `canonical_metrics`
   - 仅 `PROMOTE` 必填
   - 示例：`gpsTotalX,gpsTotalY,gpsTotalZ`
3. `IGNORE / KEEP_PENDING`
   - `canonical_metrics` 必须留空
4. `notes`
   - 建议写明业务确认原因，后续会进入执行备注
