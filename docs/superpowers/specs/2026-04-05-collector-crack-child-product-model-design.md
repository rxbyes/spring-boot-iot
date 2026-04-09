# 采集型遥测终端挂载裂缝子设备的产品与物模型设计说明

> 日期：2026-04-05
> 适用场景：`$dp` 主题下由采集型遥测终端集中上报多路裂缝子设备数据
> 代表父设备：`SK00EA0D1307986`、`SK00EA0D1307992`
> 代表子设备：`202018143`
> 目标：统一“父采集终端 + 裂缝子设备”场景的产品定义、正式物模型、设备关系与实例建档口径

## 1. 背景与范围

当前仓库已经形成以下稳定事实：

1. legacy `$dp` 场景支持“父设备一包多逻辑通道”解析。
2. `iot_device_relation` 已成为“父设备逻辑通道 -> 子设备”正式主数据入口。
3. 裂缝类 `L1_LF_*` 通道当前已具备稳定 canonical 口径：
   - `L1_LF_* -> value`
   - `S1_ZT_1.sensor_state.L1_LF_* -> sensor_state`
4. `/products` 物模型治理当前已经支持 `sourceDeviceCode + extractMode=relation_child`，把父设备样本归一为子产品正式字段。

本说明不处理以下内容：

1. 网关代子设备 Topic 通信本身的协议实现。
2. 风险点绑定、规则判级和报表统计细则。
3. 子设备更换流程的页面交互细节。
4. 新增数据库表或修改既有表结构。

本说明只回答 5 个问题：

1. 这类父设备应定义为`直连设备`还是`网关设备`。
2. 裂缝子设备是否可以共用同一套产品。
3. `L1_LF_n`、`202018143`、`value` 三者各自处于哪一层。
4. 当父设备挂载数量不同，如 `9` 路与 `3` 路时，产品是否需要拆分。
5. 当裂缝值不是绝对工程量时，基准值和偏移量应放在哪一层。

## 2. 核心结论

本轮建议固定为以下 7 条规则：

1. 采集型遥测终端父产品定义为`直连设备`，不是`网关设备`。
2. 裂缝子设备统一定义为`1`个裂缝子产品，不因挂载数量不同拆产品。
3. `L1_LF_n` 是父设备上的`逻辑通道编码`，不进入正式产品物模型。
4. `202018143` 是裂缝子设备的`设备实例编号`，不进入产品字段定义。
5. 裂缝子产品正式 `property` 统一收口为：
   - `value`
   - `sensor_state`
6. `9` 路、`3` 路、`16` 路的差异属于`实例关系规模差异`，不属于产品差异。
7. 若 `value` 是相对值或原始值，其零点、偏移量、槽位号等解释参数应进入`实例元数据`或`关系元数据`，而不是进入产品物模型。

## 3. 场景解读

### 3.1 父设备报文特征

当前样例中，父设备 `SK00EA0D1307986` 会在一条 `$dp` 报文中同时上报：

1. 裂缝业务数据
   - `L1_LF_1`
   - `L1_LF_2`
   - `...`
   - `L1_LF_9`
2. 父设备状态数据
   - `S1_ZT_1.ext_power_volt`
   - `S1_ZT_1.temp`
   - `S1_ZT_1.humidity`
   - `S1_ZT_1.signal_4g`
   - `S1_ZT_1.sw_version`
   - `S1_ZT_1.sensor_state.L1_LF_n`

`SK00EA0D1307992` 的结构与之相同，只是当前挂载了 `3` 路裂缝子设备。

### 3.2 业务本质

这类设备不是“9 个正式字段写在父产品里”的单设备一体机，也不是“真正通过 gateway/sub-device Topic 通信”的网关代子设备场景。

它的业务本质是：

1. 父设备自己负责采集与集中上报。
2. 父报文里每个 `L1_LF_n` 代表一条裂缝测点逻辑通道。
3. 平台再根据 `iot_device_relation` 把每条逻辑通道归属到一台真实裂缝子设备实例。

因此该场景在平台中应被表达为：

`父采集终端 + 裂缝子产品 + collector_child 关系`

而不是：

1. `父产品直接内置 9 个裂缝字段`
2. `按 3 路 / 9 路拆多个父产品`
3. `强行套用真正网关拓扑的产品语义`

## 4. 产品层设计

### 4.1 父产品：采集型遥测终端

建议定义：

1. `productKey` 示例：`nf-collector-rtu-v1`
2. `productName` 示例：`南方采集型遥测终端`
3. `protocolCode`：按现场协议填写，如 `mqtt-json`
4. `nodeType`：`直连设备`

父产品正式物模型只保留终端自身状态字段，不纳入 `L1_LF_n` 裂缝主监测值。

建议首批正式 `property`：

1. `ext_power_volt`
2. `solar_volt`
3. `battery_dump_energy`
4. `battery_volt`
5. `supply_power`
6. `consume_power`
7. `temp`
8. `humidity`
9. `temp_out`
10. `humidity_out`
11. `lon`
12. `lat`
13. `signal_4g`
14. `signal_NB`
15. `signal_db`
16. `sw_version`

本轮不建议把以下字段纳入父产品正式模型：

1. `L1_LF_1`
2. `L1_LF_2`
3. `...`
4. `L1_LF_n`
5. `S1_ZT_1.sensor_state.L1_LF_n`

原因：

1. `L1_LF_n` 属于子设备主监测值。
2. `S1_ZT_1.sensor_state.L1_LF_n` 属于子设备健康态镜像。
3. 一旦把这批字段固化进父产品，父产品就会被通道数量绑死。

### 4.2 子产品：裂缝传感器

建议定义：

1. `productKey` 示例：`nf-monitor-crack-sensor-v1`
2. `productName` 示例：`南方裂缝传感器`
3. `protocolCode`：可与父设备产品保持一致，便于档案一致性维护
4. `nodeType`：当前建议继续使用`直连设备`

这里不建议当前把子产品建成 `nodeType=3`，原因是：

1. 当前平台里的 `nodeType=3` 仍对应“网关子设备”语义。
2. 你这个场景的正式关系语义已经是 `collector_child`。
3. 若直接把采集型父设备挂载的裂缝子设备也解释成“网关子设备”，会把采集汇聚场景和真正 gateway/sub-device 拓扑混淆。

后续若平台专门补齐“采集器子设备”节点语义，再考虑升级子产品节点类型；本轮先不借用“网关子设备”概念。

### 4.3 子产品正式物模型

裂缝子产品首批正式 `property` 建议只保留 2 个：

| identifier | modelName | dataType | 说明 |
|---|---|---|---|
| `value` | 裂缝监测值 | `double` | 当前主监测值，来自父报文 `L1_LF_n` 标量 |
| `sensor_state` | 传感器状态 | `integer` | 当前测点健康态，来自父报文 `S1_ZT_1.sensor_state.L1_LF_n` |

补充口径：

1. `value` 的展示名当前固定为`裂缝监测值`，保持业务中性。
2. 若未来确认所有这类设备上报的都是“相对裂缝值”，可在展示名或说明中升级为`裂缝相对值`，但不建议轻易改 `identifier`。
3. 当前不把 `L1_LF_1`、`L1_LF_2` 这样的协议通道名写成子产品正式字段。

## 5. 设备层设计

### 5.1 父设备实例

父设备实例是采集型终端的真实资产，例如：

1. `SK00EA0D1307986`
2. `SK00EA0D1307992`

父设备实例属于父产品，不因当前挂了 `3` 路还是 `9` 路而变更产品。

### 5.2 子设备实例

子设备实例是裂缝传感器的真实资产，例如：

1. `202018143`
2. `202018135`
3. `202018121`
4. `202018137`
5. `202018142`
6. `202018130`
7. `202018127`
8. `202018118`
9. `202018139`

子设备实例都属于同一个裂缝子产品。

因此，对 `202018143` 的正确理解不是：

1. 它的物模型叫 `L1_LF_1`

而是：

1. 它是一台裂缝子设备实例
2. 它属于裂缝子产品
3. 它的正式物模型是 `value + sensor_state`
4. 它通过设备关系被声明为父设备 `SK00EA0D1307986` 的 `L1_LF_1` 通道实例

## 6. 关系层设计

### 6.1 关系是该场景的正式主语义

当前场景最关键的不是父设备或子设备单独怎么建，而是：

`父设备 + 逻辑通道 + 子设备`

这三者必须一起被表达。

建议每条关系至少包含：

1. `parentDeviceCode`
2. `logicalChannelCode`
3. `childDeviceCode`
4. `relationType`
5. `canonicalizationStrategy`
6. `statusMirrorStrategy`
7. `enabled`

### 6.2 正式关系取值

本场景统一使用：

1. `relationType = collector_child`
2. `canonicalizationStrategy = LF_VALUE`
3. `statusMirrorStrategy = SENSOR_STATE`

### 6.3 关系示例

对 `SK00EA0D1307986`：

1. `SK00EA0D1307986 -> L1_LF_1 -> 202018143`
2. `SK00EA0D1307986 -> L1_LF_2 -> 202018135`
3. `SK00EA0D1307986 -> L1_LF_3 -> 202018121`
4. `SK00EA0D1307986 -> L1_LF_4 -> 202018137`
5. `SK00EA0D1307986 -> L1_LF_5 -> 202018142`
6. `SK00EA0D1307986 -> L1_LF_6 -> 202018130`
7. `SK00EA0D1307986 -> L1_LF_7 -> 202018127`
8. `SK00EA0D1307986 -> L1_LF_8 -> 202018118`
9. `SK00EA0D1307986 -> L1_LF_9 -> 202018139`

对 `SK00EA0D1307992`：

1. `SK00EA0D1307992 -> L1_LF_1 -> <子设备A>`
2. `SK00EA0D1307992 -> L1_LF_2 -> <子设备B>`
3. `SK00EA0D1307992 -> L1_LF_3 -> <子设备C>`

这再次说明：

1. `9` 路和 `3` 路的差异发生在关系条数上。
2. 不需要因此创建“9 路采集终端产品”和“3 路采集终端产品”两个产品。

## 7. 运行时数据流

### 7.1 父报文进入平台

父设备上报 `$dp` 后，平台先识别父设备，如：

1. `deviceCode = SK00EA0D1307986`

### 7.2 逻辑通道拆分

当报文中出现：

1. `L1_LF_1 = 10.86`

平台不应把它固化为父产品正式字段，而应：

1. 查询 `iot_device_relation`
2. 命中 `SK00EA0D1307986 + L1_LF_1 -> 202018143`
3. 生成子设备侧标准化结果：
   - `deviceCode = 202018143`
   - `properties.value = 10.86`

### 7.3 状态镜像

若同包中还存在：

1. `S1_ZT_1.sensor_state.L1_LF_1 = 0`

则继续镜像为：

1. `deviceCode = 202018143`
2. `properties.sensor_state = 0`

### 7.4 落库结果

最终子设备 `202018143` 应看到的是：

1. `value = 10.86`
2. `sensor_state = 0`

而父设备侧只保留：

1. 原始消息日志
2. 父设备自身状态字段
3. 必要的运行期拆分证据

父设备正式物模型中不应再出现：

1. `L1_LF_1`
2. `L1_LF_2`
3. `...`
4. `L1_LF_n`

## 8. 非绝对值处理

### 8.1 为什么不拆子产品

即使 `value` 不是绝对工程量，只要所有同类裂缝子设备上报结构一致、业务语义一致，仍可以共用同一套裂缝子产品。

不应因为以下差异就拆产品：

1. 某台设备零点不同
2. 某台设备偏移量不同
3. 某台设备安装位置不同
4. 某台设备挂在 `L1_LF_1` 还是 `L1_LF_9`

这些都属于实例差异，不属于产品差异。

### 8.2 应放在实例层的信息

以下信息不应进入裂缝子产品正式物模型，应优先进入子设备 `metadataJson` 或后续关系扩展字段：

1. `baselineValue`
2. `offset`
3. `initialReading`
4. `slotIndex`
5. `monitorPointCode`
6. `installPosition`

推荐顺序：

1. `logicalChannelCode` 放在 `iot_device_relation`
2. `baselineValue / offset / initialReading` 优先放子设备 `metadataJson`
3. 若未来确认“同一子设备换到不同父设备时，基准值也跟着关系变化”，再把这类参数上移为关系扩展字段

### 8.3 正式字段命名建议

当前建议：

1. `identifier = value`
2. `modelName = 裂缝监测值`

这样可以兼容以下几种现实：

1. 当前值可能是相对值
2. 当前值可能是原始采样值
3. 当前值未来可能经规范治理后被解释为工程量

在没有统一业务结论前，不建议现在就把正式字段名硬定成：

1. `absolute_value`
2. `relative_value`
3. `raw_value`

## 9. 建档模板

### 9.1 产品建档模板

父产品建议：

1. `productKey = nf-collector-rtu-v1`
2. `productName = 南方采集型遥测终端`
3. `nodeType = 1`
4. 正式 `property` 只保留终端自身状态字段

子产品建议：

1. `productKey = nf-monitor-crack-sensor-v1`
2. `productName = 南方裂缝传感器`
3. `nodeType = 1`
4. 正式 `property`：
   - `value`
   - `sensor_state`

### 9.2 设备建档模板

父设备：

1. `deviceCode = SK00EA0D1307986`
2. `productKey = nf-collector-rtu-v1`

子设备：

1. `deviceCode = 202018143`
2. `productKey = nf-monitor-crack-sensor-v1`
3. `metadataJson` 可选保存：
   - `baselineValue`
   - `offset`
   - `slotIndex`
   - `monitorPointCode`

### 9.3 关系建档模板

关系示例：

```json
{
  "parentDeviceCode": "SK00EA0D1307986",
  "logicalChannelCode": "L1_LF_1",
  "childDeviceCode": "202018143",
  "relationType": "collector_child",
  "canonicalizationStrategy": "LF_VALUE",
  "statusMirrorStrategy": "SENSOR_STATE",
  "enabled": 1
}
```

## 10. 错误处理与边界规则

### 10.1 缺失关系

若父报文出现 `L1_LF_n`，但平台中尚未建立对应 `iot_device_relation`，则：

1. 不应把该字段自动沉淀成父产品正式字段。
2. 应视为“关系主数据未完善”问题。
3. 后续应通过设备关系治理补齐 `collector_child` 关系。

### 10.2 关系冲突

对同一父设备：

1. 同一个 `logicalChannelCode` 只能映射到一台子设备。
2. 不允许同一父设备下同时存在两条 `L1_LF_1` 关系。

### 10.3 路数变化

父设备从 `3` 路扩容到 `9` 路时：

1. 不改父产品
2. 不改子产品
3. 只新增设备实例和关系条目

父设备从 `9` 路减到 `3` 路时：

1. 不改产品
2. 视现场资产情况停用或解绑多余子设备关系

## 11. 验证与验收建议

本设计落地后，至少应验证以下 5 类结果：

1. 产品治理验证
   - 在父产品上下文中，不再把 `L1_LF_1` 这类字段当成正式 `property` 候选
   - 在子产品上下文中，父设备样本可经 `relation_child` 归一出 `value / sensor_state`
2. 关系治理验证
   - 同一父设备下 `L1_LF_n` 唯一
   - 不同父设备可以各自拥有自己的 `L1_LF_1`
3. 运行时验证
   - 父报文中的 `L1_LF_1` 能正确落到 `202018143.value`
   - 父报文中的 `S1_ZT_1.sensor_state.L1_LF_1` 能正确落到 `202018143.sensor_state`
4. 扩展性验证
   - `SK00EA0D1307986` 挂 `9` 路与 `SK00EA0D1307992` 挂 `3` 路时，共用同一套父产品与子产品
5. 实例元数据验证
   - 基准值、偏移量等实例解释参数不会污染产品正式物模型

## 12. 最终统一话术

推荐团队统一使用以下表述：

1. `SK00EA0D1307986` 是采集型遥测终端父设备，产品类型按`直连设备`建档。
2. `202018143` 是裂缝子设备实例，属于统一的裂缝子产品。
3. `L1_LF_1` 是父设备逻辑通道，不是正式物模型字段。
4. 裂缝子产品的正式物模型固定为 `value + sensor_state`。
5. `3` 路、`9` 路的区别是关系数量不同，不是产品不同。

## 13. 关联依据

1. [05-protocol.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/05-protocol.md)
2. [03-接口规范与接口清单.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/03-接口规范与接口清单.md)
3. [02-业务功能与流程说明.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/02-业务功能与流程说明.md)
4. [2026-04-04-product-model-boundary-and-role-design.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/specs/2026-04-04-product-model-boundary-and-role-design.md)
5. [2026-04-04-phase5-iot-architecture-evolution-design.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/specs/2026-04-04-phase5-iot-architecture-evolution-design.md)
