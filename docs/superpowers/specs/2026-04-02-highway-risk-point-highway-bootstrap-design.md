# 2026-04-02 高速公路风险点扩表与初始化设计

## 1. 背景

当前 `risk_point` 主表已经承接风险对象 CRUD、规则绑定上游、告警/事件归属、风险监测读侧与 GIS 聚合读侧，但表结构仍偏向通用演示场景，无法直接承接 `项目.xls` 中的高速公路项目台账。

本次输入数据具有以下特点：

- 来源文件：`C:/Users/rxbye/Downloads/项目.xls`
- 当前共 `65` 条有效项目记录
- 项目类型分布：`边坡 47`、`桥梁 13`、`隧道 5`
- 列结构以“项目档案”字段为主，包含项目名称、项目类型、项目简介、路线编号、路线名称、公路等级、项目风险等级、经纬度、行政区域、管养单位名称
- 不包含设备编码、测点标识、阈值、责任人用户 ID 等运行期绑定字段

因此，本轮目标不是生成 `risk_point_device` 绑定数据，而是把这批项目沉淀成可直接落库、可继续扩展、可被当前风险点主链路引用的风险对象基础档案。

## 2. 设计目标

本轮设计必须同时满足：

1. 与现有 `risk_point`、`risk_point_device`、告警、事件、风险监测、GIS 读侧职责兼容
2. 能承接高速公路项目档案，而不把大量行业专属字段硬塞进 `risk_point`
3. 后续可扩展到桥梁、边坡、隧道之外的其他风险点监测场景
4. 初始化数据可直接导入当前系统，而不是停留在纯示例 SQL
5. 不伪造 `device_id / metric_identifier / responsible_user` 之类运行期主数据

## 3. 当前事实与约束

### 3.1 当前 `risk_point` 基线

现有主表字段：

- `risk_point_code`
- `risk_point_name`
- `org_id / org_name`
- `region_id / region_name`
- `responsible_user / responsible_phone`
- `risk_level`
- `description`
- `status`
- `tenant_id`
- 审计字段与逻辑删除字段

现有接口与文档口径明确要求：

- 风险点仍是风险平台通用对象主表
- `org_id / region_id` 应引用平台治理主数据
- `risk_point_device` 负责设备测点绑定，不应承接静态项目档案

### 3.2 当前 `项目.xls` 的限制

Excel 中缺少以下关键字段：

- 系统内 `org_id`
- 系统内 `region_id`
- 系统内 `responsible_user`
- `device_id / device_code`
- `metric_identifier / metric_name`
- 运行期阈值或默认阈值

因此本轮必须同步补齐最小 `sys_region / sys_organization` 初始化映射，且不能生成 `risk_point_device` 假绑定数据。

### 3.3 GIS 现状

当前 `RiskMonitoringServiceImpl#listGisPoints` 仍通过已绑定设备坐标均值回填风险点坐标。该实现不阻塞本轮 SQL 设计，但本轮新增 `risk_point.longitude / latitude` 后，后续如需让“未绑定设备的风险点”也能参与 GIS 已定位表达，可单独追加 Java 读侧改造。

本轮不把 GIS 读侧改造纳入实现范围。

## 4. 方案选择

评估过三类方案：

### 方案 A：所有字段都加到 `risk_point`

优点：

- SQL 最少

缺点：

- 高速公路专属字段污染通用主表
- 后续扩展到其他行业风险点时表会迅速失控

### 方案 B：只做 `risk_point` 初始化，不补区域和组织映射

优点：

- 初始成本最低

缺点：

- 无法真实落到现有系统主数据模型
- `org_id / region_id` 只能为空或填假值
- 后续仍需返工

### 方案 C：推荐方案，主表做公共扩展，行业字段落扩展表

做法：

- `risk_point` 只新增跨行业也成立的公共字段
- 新增 `risk_point_highway_detail` 承接高速公路项目档案
- 同步补最小 `sys_region / sys_organization` 初始化映射

结论：采用方案 C。

## 5. 目标数据模型

### 5.1 `risk_point` 扩展原则

`risk_point` 继续承担“平台通用风险对象主表”职责，只补跨行业公共字段。

建议新增字段：

- `risk_type varchar(32) not null default 'GENERAL' comment '风险点类型，如 SLOPE/BRIDGE/TUNNEL'`
- `location_text varchar(255) default null comment '位置描述/桩号/区间'`
- `longitude decimal(10,6) default null comment '风险点经度'`
- `latitude decimal(10,6) default null comment '风险点纬度'`

新增索引建议：

- `idx_risk_type_status (risk_type, status)`
- `idx_geo (longitude, latitude)` 可选；若担心 MySQL 普通索引收益有限，可先不加

字段设计原则：

- `risk_type` 承接主表筛选语义，供后续类型化治理使用
- `location_text` 承接项目位点表达，避免把桩号塞进 `description`
- 经纬度直接落主表，便于后续 GIS 和风险点档案直接消费

### 5.2 `risk_point_highway_detail`

新增表：`risk_point_highway_detail`

建议字段：

- `id bigint`
- `risk_point_id bigint not null`
- `project_name varchar(255) not null`
- `project_type varchar(32) not null`
- `project_summary text null`
- `route_code varchar(64) not null`
- `route_name varchar(128) default null`
- `road_level varchar(64) default null`
- `project_risk_level varchar(32) default null`
- `admin_region_code varchar(32) default null`
- `admin_region_path_json varchar(255) default null`
- `maintenance_org_name varchar(128) default null`
- `source_row_no int default null`
- `tenant_id bigint not null default 1`
- `create_by bigint default null`
- `create_time datetime not null default current_timestamp`
- `update_by bigint default null`
- `update_time datetime not null default current_timestamp on update current_timestamp`
- `deleted tinyint not null default 0`

约束建议：

- `unique key uk_risk_point_highway (risk_point_id)`
- `key idx_route_code (route_code)`
- `key idx_admin_region_code (admin_region_code)`
- `key idx_project_type (project_type)`

职责边界：

- 一条 `risk_point` 对应一条高速公路项目扩展记录
- 高速公路项目档案专属字段只落该表，不反向污染主表

## 6. Excel 字段映射

### 6.1 主表映射

| Excel 列 | 目标字段 | 说明 |
|---|---|---|
| 项目名称 | `risk_point.risk_point_name` | 风险点主名称 |
| 项目类型 | `risk_point.risk_type` | 需归一化为枚举值 |
| 项目简介 | `risk_point.description` | 主表只保留摘要或原文，视字段长度而定 |
| 路线编号 | `risk_point.location_text` | 优先承接桩号/区间表达 |
| 经度 | `risk_point.longitude` | 允许为空 |
| 纬度 | `risk_point.latitude` | 允许为空 |
| 管养单位名称 | `risk_point.org_id/org_name` | 通过组织映射获得 |
| 行政区域 | `risk_point.region_id/region_name` | 通过区划码映射获得 |

### 6.2 扩展表映射

| Excel 列 | 目标字段 |
|---|---|
| 项目名称 | `project_name` |
| 项目类型 | `project_type` |
| 项目简介 | `project_summary` |
| 路线编号 | `route_code` |
| 路线名称 | `route_name` |
| 公路等级 | `road_level` |
| 项目风险等级 | `project_risk_level` |
| 行政区域 | `admin_region_path_json` |
| 行政区域末级码 | `admin_region_code` |
| 管养单位名称 | `maintenance_org_name` |
| Excel 行号 | `source_row_no` |

## 7. 清洗与归一化规则

### 7.1 项目类型归一化

建议统一为主表英文内码：

- `边坡 -> SLOPE`
- `桥梁 -> BRIDGE`
- `隧道 -> TUNNEL`

扩展表 `project_type` 可继续保留中文原值，便于追溯原始台账。

### 7.2 路线编号清洗

需要处理以下异常：

- 去掉首尾空格
- 去掉中英文混排导致的多余空格
- 保留原桩号语义，例如 `K1458+75`、`SK384+870-SK384+920`

特殊说明：

- 个别 `路线编号` 并非标准路线编码，而是完整区间文本，如 `SXK595+818-SX595+960`
- 这类值仍应保留原文写入，不强行改写

### 7.3 路线名称清洗

需要做最小纠偏：

- 去首尾空格
- `兰海高速兰` 归一为 `兰海高速`

不做过度语义修正，避免引入新误差。

### 7.4 行政区域清洗

Excel 中行政区域值形如：

- `["62","6212","621221"]`

处理规则：

- 原值原样保留到 `admin_region_path_json`
- 取最后一级编码作为 `admin_region_code`
- 若单元格为空或文本值为 `null`，则视为缺失

### 7.5 描述字段策略

部分项目简介为长文本。

建议：

- `risk_point.description` 扩容到 `1000`，并允许落原文
- `risk_point_highway_detail.project_summary` 使用 `text`

原因：

- 现有 `description(512)` 对长文本边坡/隧道简介不足
- 直接截断会损失业务语义

### 7.6 风险等级初始化策略

由于 Excel 中 `项目风险等级` 基本为空，本轮不从台账推导真实四色等级。

建议：

- `risk_point.risk_level` 统一初始化为 `blue`
- `risk_point_highway_detail.project_risk_level` 原样保留 Excel 值，允许为空

这样既符合当前四色治理基线，也避免伪造运行态风险等级。

## 8. 主数据初始化策略

### 8.1 `sys_region`

按 Excel 实际出现的行政区划路径补最小区域树：

- 省级：`62`
- 市级：路径中的第二级
- 区县级：路径中的第三级或末级

写入原则：

- `region_code` 直接使用行政区划码
- `region_name` 若当前初始化基线未提供完整国家区划字典，则可使用编码名称或按已知行政区名称补齐
- `parent_id` 按区划层级挂载

优先目标是让 `risk_point.region_id` 可稳定引用，而不是一次性完成整省全量区域治理。

### 8.2 `sys_organization`

按 Excel 唯一 `管养单位名称` 初始化最小组织档案。

当前识别出的组织包括：

- 成县所
- 成武所
- 武都所
- 甘肃公航旅高速公路运营管理兰州分公司
- 甘肃公航旅高速公路运营管理临夏分公司
- 甘肃公航旅高速公路运营管理平凉分公司
- 甘肃公航旅高速公路运营管理武威分公司
- 甘肃公航旅高速公路运营管理天水分公司
- 甘肃公航旅高速公路运营管理定西分公司
- 甘肃公航旅高速公路运营管理敦煌分公司

组织层级策略：

- 本轮采用“最小可落地扁平组织”初始化
- 不强行推导“分公司 -> 所站”父子关系
- `leader_user_id` 统一回退用户 `1`
- `phone` 使用默认电话或空值

原因：

- 现有 Excel 无法可靠推导完整组织树
- 强构造父子关系的返工成本高于本轮收益

## 9. 初始化数据生成规则

### 9.1 风险点编码

不复用运行时服务端自动编码逻辑，初始化脚本直接给出稳定编码。

建议规则：

- `RP-HW-{TYPE}-{NNN}`

示例：

- `RP-HW-SLOPE-001`
- `RP-HW-BRIDGE-001`
- `RP-HW-TUNNEL-001`

原因：

- 便于脚本幂等导入与人工核查
- 避免初始化 SQL 依赖服务端生成逻辑

### 9.2 主表风险点

每条 Excel 记录生成一条 `risk_point`：

- `risk_point_name`：项目名称
- `risk_type`：归一化后的项目类型
- `location_text`：优先使用路线编号
- `longitude/latitude`：原样写入
- `org_id/org_name`：按组织映射
- `region_id/region_name`：按区域映射
- `responsible_user`：默认组织负责人或用户 `1`
- `responsible_phone`：优先组织联系电话，缺失则空
- `risk_level`：默认 `blue`
- `description`：项目简介
- `status`：默认 `0`

### 9.3 高速扩展明细

每条 Excel 记录生成一条 `risk_point_highway_detail`：

- 与主表一对一
- 完整保留路线、公路等级、项目简介、行政区划路径和管养单位名称
- `source_row_no` 固化 Excel 原始行号，便于后续追溯

### 9.4 不生成的数据

本轮明确不生成：

- `risk_point_device`
- `rule_definition`
- `iot_alarm_record`
- `iot_event_record`
- 设备、产品或物模型数据

## 10. 交付清单

本轮实现应产出：

1. `sql/init.sql` 中的 `risk_point` 扩表
2. `sql/init.sql` 中新增 `risk_point_highway_detail`
3. `sql/init-data.sql` 中新增最小 `sys_region` 初始化
4. `sql/init-data.sql` 中新增最小 `sys_organization` 初始化
5. `sql/init-data.sql` 中新增基于 Excel 的 `risk_point` 初始化
6. `sql/init-data.sql` 中新增基于 Excel 的 `risk_point_highway_detail` 初始化
7. `docs/04-数据库设计与初始化数据.md` 同步更新
8. `docs/08-变更记录与技术债清单.md` 同步留痕

## 11. 非目标

以下内容不纳入本轮：

- `risk_point_device` 真设备绑定
- 风险点新增/编辑接口入参扩展
- GIS 读侧优先读取风险点静态坐标
- 基于项目台账自动推导真实运行期风险等级
- 组织树精细化建模
- 行政区划名称全量国家标准字典治理

## 12. 风险与后续建议

### 12.1 当前风险

- Excel 缺少真实组织负责人、设备、测点和运行期风险等级，无法一步直达完整风险闭环
- 行政区划只有编码路径，没有标准名称字典时，区域初始化需要最小命名策略
- 坐标缺失记录较多，GIS 已定位覆盖率不会达到 100%

### 12.2 后续建议

后续可在独立任务中继续补：

1. GIS 读侧优先读 `risk_point.longitude/latitude`，设备坐标只作回退
2. 风险点类型字典化，而不是只用字符串内码
3. 高速公路场景下的桥梁/边坡/隧道专项扩展表继续拆分
4. 基于真实设备测点再补 `risk_point_device`
5. 用正式行政区划字典替换本轮最小区域初始化数据
