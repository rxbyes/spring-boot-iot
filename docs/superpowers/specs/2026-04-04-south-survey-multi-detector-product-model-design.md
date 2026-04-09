# 南方测绘多维监测设备产品物模型设计说明

> 日期：2026-04-04
> 设备样本：`SJ11F4148737700A`
> 设备类型：南方测绘多维监测型设备
> 当前确认能力：倾角、加速度、设备状态
> 目标：基于规范截图与真实上报数据，整理该类产品当前建议维护的 `property / event / service` 正式物模型

## 1. 背景与范围

本说明服务于“把某一类设备真实上报内容收口为产品正式物模型”的目标，不处理设备实例建档、消息链路、规则判级或风险绑定本身。

本轮只根据以下证据整理物模型：

1. 南方测绘厂家提供的两段真实上报 JSON
   - `监测数据`
   - `状态数据`
2. 用户提供的两张规范截图
   - 设备状态参数表与传感器状态码表
   - 倾角 / 加速度监测参数表
3. 仓库当前已确认的产品物模型边界与治理口径
   - [产品物模型在系统中的定位、边界与作用设计说明](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/specs/2026-04-04-product-model-boundary-and-role-design.md)
   - [产品物模型规范证据与报文证据分批治理设计](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/specs/2026-04-03-product-contract-normative-evidence-governance-design.md)
4. 仓库当前规范预设与协议 flatten 口径
   - [ProductModelNormativePresetRegistry.java](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java)
   - [05-protocol.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/05-protocol.md)

本轮不新增事件定义、服务定义或命令闭环设计；若后续厂家补充了独立事件报文或下行控制协议，应另起一轮治理。

## 2. 核心结论

对这台南方测绘多维监测型设备，当前建议正式物模型先按以下结论落地：

1. `property` 是当前正式物模型主体。
2. 当前存在两组 `property`：
   - `监测属性`
   - `设备状态属性`
3. 当前没有足够证据支持建立正式 `event`。
4. 当前没有足够证据支持建立正式 `service`。
5. 正式 `identifier` 必须收口为规范化叶子路径，如：
   - `L1_QJ_1.X`
   - `L1_JS_1.gY`
   - `S1_ZT_1.ext_power_volt`
6. 原始 payload 中的设备编码和时间戳层级只是包装结构，不进入正式物模型。

## 3. 原始上报结构解读

### 3.1 监测数据

真实监测数据结构为：

`deviceCode -> pointCode -> timestamp -> leafFields`

样例中体现为：

1. 设备编码：`SJ11F4148737700A`
2. 倾角测点：`L1_QJ_1`
3. 加速度测点：`L1_JS_1`
4. 时间戳：`2026-04-04T21:20:38.000Z`
5. 叶子字段：
   - 倾角：`X / Y / Z / angle / AZI`
   - 加速度：`gX / gY / gZ`

根据仓库当前协议文档，这类嵌套 JSON 会被拍平成属性标识，例如：

1. `L1_QJ_1.X`
2. `L1_QJ_1.angle`
3. `L1_JS_1.gX`

因此，这批数据当前应进入 `property`，不是 `event`。

### 3.2 状态数据

真实状态数据结构同样为：

`deviceCode -> pointCode -> timestamp -> leafFields`

样例中体现为：

1. 状态测点：`S1_ZT_1`
2. 叶子字段：
   - 电源、电量、温湿度、经纬度、通信信号、版本号
   - `sensor_state` 子对象

`sensor_state` 仍建议继续拍平成叶子级属性，而不是只保留整块 JSON：

1. `S1_ZT_1.sensor_state.L1_QJ_1`
2. `S1_ZT_1.sensor_state.L1_JS_1`
3. `S1_ZT_1.sensor_state.L1_GP_1`

## 4. 规范与真实证据对照结论

### 4.1 双证据直接对齐的字段

以下字段同时具备“规范证据 + 真实上报证据”，应优先纳入正式 `property`：

1. 倾角：
   - `L1_QJ_1.X`
   - `L1_QJ_1.Y`
   - `L1_QJ_1.Z`
   - `L1_QJ_1.angle`
   - `L1_QJ_1.AZI`
2. 加速度：
   - `L1_JS_1.gX`
   - `L1_JS_1.gY`
   - `L1_JS_1.gZ`
3. 状态参数：
   - `S1_ZT_1.ext_power_volt`
   - `S1_ZT_1.solar_volt`
   - `S1_ZT_1.battery_dump_energy`
   - `S1_ZT_1.temp`
   - `S1_ZT_1.humidity`
   - `S1_ZT_1.lon`
   - `S1_ZT_1.lat`
   - `S1_ZT_1.signal_4g`
   - `S1_ZT_1.signal_NB`
   - `S1_ZT_1.signal_db`
   - `S1_ZT_1.sw_version`
   - `S1_ZT_1.sensor_state.L1_QJ_1`
   - `S1_ZT_1.sensor_state.L1_JS_1`

### 4.2 真实上报存在但当前截图规范未直接确认的字段

以下字段在真实 payload 中出现，但本轮提供的规范截图中未直接确认，应先列为`待确认扩展属性`：

1. `S1_ZT_1.battery_volt`
2. `S1_ZT_1.supply_power`
3. `S1_ZT_1.consume_power`
4. `S1_ZT_1.temp_out`
5. `S1_ZT_1.humidity_out`
6. `S1_ZT_1.sensor_state.L1_GP_1`

这些字段不建议在首版正式模型中直接落库，除非补齐以下任一证据：

1. 厂家字段定义表或接口文档
2. 同型号稳定多批次真实上报
3. 业务确认它们属于本产品长期维护口径

### 4.3 规范截图出现但当前设备不建议纳入的字段

状态截图中还出现了以下字段，但当前不建议纳入这台产品的首版正式模型：

1. `S1_ZT_1.pa_state`
2. `S1_ZT_1.sound_state`

原因：

1. 截图注释明确指向“预警喇叭专有”类能力。
2. 当前设备真实上报中未提供这两个字段。
3. 当前设备能力说明以“倾角 + 加速度监测型设备”为主，不是声光告警设备。

## 5. 正式物模型建议

### 5.1 建议正式纳入的 property

#### 5.1.1 监测属性

| identifier | modelName | dataType | unit | requiredFlag | 说明 |
|---|---|---|---|---|---|
| `L1_QJ_1.X` | 倾角测点 X 轴倾角 | `double` | `°` | `1` | X 轴与水平面的夹角 |
| `L1_QJ_1.Y` | 倾角测点 Y 轴倾角 | `double` | `°` | `1` | Y 轴与水平面的夹角 |
| `L1_QJ_1.Z` | 倾角测点 Z 轴倾角 | `double` | `°` | `1` | Z 轴与水平面的夹角 |
| `L1_QJ_1.angle` | 倾角测点平面夹角 | `double` | `°` | `1` | X、Y 轴形成平面与水平面的夹角 |
| `L1_QJ_1.AZI` | 倾角测点方位角 | `double` | `°` | `1` | X 轴投影与磁北夹角 |
| `L1_JS_1.gX` | 加速度测点 X 轴加速度 | `double` | `mg` | `1` | X 轴加速度 |
| `L1_JS_1.gY` | 加速度测点 Y 轴加速度 | `double` | `mg` | `1` | Y 轴加速度 |
| `L1_JS_1.gZ` | 加速度测点 Z 轴加速度 | `double` | `mg` | `1` | Z 轴加速度 |

说明：

1. `gX / gY / gZ` 当前建议先按仓库预设口径维护为 `mg`。
2. 若厂家后续确认该型号固定以 `m/s²` 为正式单位，应三轴一起统一修订，不要只改单个字段。

#### 5.1.2 设备状态属性

| identifier | modelName | dataType | unit | requiredFlag | 说明 |
|---|---|---|---|---|---|
| `S1_ZT_1.ext_power_volt` | 外接电源电压 | `double` | `V` | `0` | 外接供电电压 |
| `S1_ZT_1.solar_volt` | 太阳能板电压 | `double` | `V` | `0` | 太阳能板电压 |
| `S1_ZT_1.battery_dump_energy` | 电池剩余电量 | `double` | `%` | `0` | 电池剩余电量 |
| `S1_ZT_1.temp` | 环境温度 | `double` | `℃` | `0` | 环境温度 |
| `S1_ZT_1.humidity` | 相对湿度 | `double` | `%` | `0` | 相对湿度 |
| `S1_ZT_1.lon` | 设备经度 | `string` | `°` | `0` | 安装位置经度 |
| `S1_ZT_1.lat` | 设备纬度 | `string` | `°` | `0` | 安装位置纬度 |
| `S1_ZT_1.signal_4g` | 4G 信号强度 | `integer` | `dB` | `0` | 4G 通信强度 |
| `S1_ZT_1.signal_NB` | 窄带信号强度 | `integer` | `dB` | `0` | NB 信号强度 |
| `S1_ZT_1.signal_db` | 北斗信号强度 | `integer` | `dBm` | `0` | 北斗信号强度 |
| `S1_ZT_1.sw_version` | 固件版本号 | `string` | `-` | `0` | 固件版本号 |
| `S1_ZT_1.sensor_state.L1_QJ_1` | 1号倾角测点传感器状态 | `integer` | `enum` | `0` | 倾角测点状态码 |
| `S1_ZT_1.sensor_state.L1_JS_1` | 1号加速度测点传感器状态 | `integer` | `enum` | `0` | 加速度测点状态码 |

状态码说明建议写入 `specsJson`：

1. `0 = 无错误`
2. `-1 = 供电异常`
3. `-2 = 传感器数据异常`
4. `-3 = 采样间隔内无数据`

补充口径：

1. 这批状态字段可以进入正式产品物模型。
2. 但它们默认不应被当成风险绑定主测点，也不应与倾角、加速度主监测值混成同一类业务字段。

### 5.2 待确认扩展 property

| identifier | modelName | dataType | 当前建议 | 原因 |
|---|---|---|---|---|
| `S1_ZT_1.battery_volt` | 电池电压 | `double` | 暂缓 | 真实有值，当前截图规范未直接确认 |
| `S1_ZT_1.supply_power` | 供电功率 | `double` | 暂缓 | 真实有值，当前截图规范未直接确认 |
| `S1_ZT_1.consume_power` | 功耗 | `double` | 暂缓 | 真实有值，当前截图规范未直接确认 |
| `S1_ZT_1.temp_out` | 外部温度 | `double` | 暂缓 | 真实有值，但尚未确认与 `temp` 的业务区分 |
| `S1_ZT_1.humidity_out` | 外部湿度 | `double` | 暂缓 | 真实有值，但尚未确认与 `humidity` 的业务区分 |
| `S1_ZT_1.sensor_state.L1_GP_1` | GNSS 测点传感器状态 | `integer` | 暂缓 | 当前设备能力说明未确认 GNSS 已启用 |

### 5.3 当前不建立的 event

当前建议 `event = 0`，原因如下：

1. 现有两段真实数据都属于周期性属性快照。
2. 协议文档中同类嵌套 JSON 当前按属性拍平进入主链路。
3. `sensor_state` 虽然表达故障语义，但现在仍是“状态码属性”，不是独立事件报文。

因此，本轮不建议建立如下伪事件：

1. 低电压事件
2. 传感器故障事件
3. 通信异常事件
4. 数据缺失事件

如果后续厂家提供独立事件报文、事件 topic 或事件确认机制，再单独治理 `event`。

### 5.4 当前不建立的 service

当前建议 `service = 0`，原因如下：

1. 当前证据中没有下行命令定义。
2. 当前证据中没有服务调用请求 / 响应模型。
3. 当前证据中没有控制类报文，如设置采样周期、远程重启、校准等。

因此，本轮不建议建立如下伪服务：

1. 远程重启
2. 参数设置
3. 零点校准
4. 固件升级触发

如厂家后续补充命令协议或设备控制接口，再治理 `service`。

## 6. 标识与字段归一规则

### 6.1 不进入正式模型的包装层

以下结构仅用于解析，不进入正式 `identifier`：

1. 最外层设备编码：`SJ11F4148737700A`
2. 时间戳节点：`2026-04-04T21:20:38.000Z`

正式模型必须落到叶子字段层。

### 6.2 字段别名归一

真实上报里存在拼写漂移，正式模型必须归一：

1. `singal_NB -> signal_NB`
2. `singal_db -> signal_db`

原始字段名不应直接成为正式 `identifier`，建议仅保留在 `specsJson.aliases` 或 compare 证据说明中。

### 6.3 sensor_state 拆叶子规则

`sensor_state` 不建议只保留整块 JSON 字符串，建议拆成：

1. `S1_ZT_1.sensor_state.L1_QJ_1`
2. `S1_ZT_1.sensor_state.L1_JS_1`
3. `S1_ZT_1.sensor_state.L1_GP_1`

理由：

1. 叶子级状态更利于 latest 查询。
2. 更利于后续设备健康、状态面板和治理差异比对。
3. 可以避免后续页面和规则层频繁解析内嵌 JSON。

## 7. 首版正式模型建议范围

如果按“当前即可维护进 `iot_product_model` 的首版正式模型”收口，建议如下：

1. 正式 `property`
   - `21` 个
   - 其中监测属性 `8` 个
   - 设备状态属性 `13` 个
2. 待确认扩展 `property`
   - `6` 个
3. 正式 `event`
   - `0` 个
4. 正式 `service`
   - `0` 个

## 8. 建模建议顺序

为了与仓库当前治理方法保持一致，建议按以下顺序落模：

1. 第一批先建 `8` 个核心监测属性
   - 倾角 `5`
   - 加速度 `3`
2. 第二批补建 `13` 个设备状态属性
3. 第三批再决定是否纳入 `battery_volt / temp_out / humidity_out` 等扩展状态字段
4. 等厂家补齐下行和事件协议后，再补 `event / service`

这样做的好处是：

1. 先把业务主监测值定稳。
2. 再把状态信息作为产品附属属性纳入。
3. 避免“真实报文里出现过”就一次性把所有字段都写成正式契约。

## 9. 结论

对南方测绘设备 `SJ11F4148737700A` 这类“倾角 + 加速度 + 状态上报”的多维监测型产品，当前正式物模型应明确收口为：

1. 以 `property` 为主。
2. 监测属性与状态属性分组维护。
3. 只把叶子级正式字段写入 `iot_product_model`。
4. 当前不建立 `event`。
5. 当前不建立 `service`。

换句话说，这台设备当前最合理的正式产品物模型，不是“把原始 JSON 原样搬进系统”，而是：

1. 用规范化 `identifier` 固定监测主字段。
2. 用状态属性承接设备健康与运行状态。
3. 用待确认扩展区隔离未完全证实的厂家私有字段。
