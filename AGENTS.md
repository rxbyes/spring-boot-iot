# AGENTS.md

## 项目
spring-boot-iot

## 基础包
com.ghlzm.iot

## 使命
基于 Spring Boot 4 + Java 17 构建并持续维护一个模块化 IoT 网关平台，并以真实共享开发环境作为验收基线。

## 当前状态
第一至第三阶段主链路是长期稳定基线。第四阶段风险平台能力仍在推进中，但已经具备可用的真实环境基线。
当前设备接入固定 Pipeline 为 `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE`。
当前 `spring-boot-iot-telemetry` 已纳入活跃构建链路；`application-dev.yml` / `application-prod.yml` 默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`、`iot.telemetry.read-routing.latest-source=v2`，`application-test.yml` 继续保留 `mysql`。当前 `POST /api/telemetry/history/batch` 已固定先按请求 `rangeCode` 限定历史窗口，再从 v2 raw、`iot_device_telemetry_point` 兼容表与 legacy fallback 读取窗口内点位并统一补零，避免对象洞察台被长历史设备的早期数据挤占后整段显示 `0` 趋势。
当前 `iot_agg_measure_hour` 已纳入 `sql/init-tdengine.sql` 手动初始化基线；应用运行时只会自动派生 `tb_ah_<tenantId>_<deviceId>` child table，不会自动创建该 stable。当前小时聚合仅覆盖 `MEASURE` 数值点位，且需同时开启 `iot.telemetry.aggregate.enabled=true` 与 `iot.telemetry.aggregate.hourly-enabled=true`。
当前 `application-dev.yml` / `application-prod.yml` / `application-test.yml` 已显式固化 MySQL 主库 Hikari 基线，默认 `maximum-pool-size=30`、`minimum-idle=5`、`keepalive-time=300000`、`max-lifetime=1800000`、`leak-detection-threshold=20000`；dev 的 `slave_1` 也补齐了独立 Hikari 基线，不再依赖默认 `10` 连接。
质量工场当前已新增 `/business-acceptance` 业务验收台：面向验收人员、产品和项目经理只暴露 `环境 / 账号模板 / 模块范围` 三类轻配置，并在结果首屏直接回答“是否通过”“哪些模块没过”；`/automation-results` 继续作为底层结果与证据中心，并支持通过 `runId` query 直接预选同一次运行。
产品契约字段工作台已于 2026-04-05 收口为 `/products` 内的单页工作区。当前 `契约字段` 与 `契约字段描述` 在同页完成样本输入、识别结果、本次生效和正式字段查看，不再打开二层抽屉，也不再提供 `model-candidates`、规范预设、运行期自动提炼或 `manualDraftItems` 人工补录。样本类型固定为 `业务数据 / 状态数据`，设备结构固定为 `单台设备 / 复合设备`；单次只支持 `1` 台设备样本 JSON。复合设备模式需显式提交 `父设备编码 + 逻辑通道编码/子设备编码` 映射，平台会优先沿用 `relationMappings[]` 或 `iot_device_relation` 里的 `canonicalization_strategy / status_mirror_strategy`，缺省时再按逻辑通道类型兼容推断：`L1_LF_* -> collector_child + LF_VALUE + SENSOR_STATE`，`L1_SW_* -> collector_child + LEGACY + SENSOR_STATE`。其中裂缝业务样本会归一为子产品 `value`，深部位移业务样本会保留 `dispsX / dispsY`；状态样本会把 `S1_ZT_1.sensor_state.<logicalChannelCode>` 归一为子产品 `sensor_state`。采集器自身 `temp / humidity / signal_4g` 等状态字段继续留在采集器产品，不会带入子产品；其中 `nf-collect-rtu-v1`（`南方测绘 采集型 遥测终端`）虽然初始化数据仍按 `nodeType=1` 直连产品建档，但在 `状态数据 + 复合设备` compare/apply 中也必须命中同一采集器父产品边界：`S1_ZT_1.*` 中除 `S1_ZT_1.sensor_state.<logicalChannelCode>` 外的父设备状态叶子字段都应继续保留为父产品候选，例如 `ext_power_volt / solar_volt / battery_dump_energy / battery_volt / supply_power / consume_power / temp / humidity / temp_out / humidity_out / lon / lat / signal_4g / signal_NB / signal_db / sw_version`；单台深部位移设备仍沿用 `dispsX / dispsY / sensor_state` 正式物模型，只是在尚未收到状态报文时可以暂时没有 `sensor_state` 最新值。`2026-04-11` 起，采集器产品在复合设备 compare/apply 写侧只治理采集器自身运行状态字段，`value / sensor_state / dispsX / dispsY` 一类子设备正式字段不再允许从采集器产品发布；上述 `nodeType=1` 的采集型遥测终端同样适用这条边界。compare/apply 仍直接写入 `iot_product_model`，不新增平行草稿表。`2026-04-06` 起首批 `phase1-crack + phase2-gnss` 双场景已补齐规范字段库、厂商字段证据、合同发布批次和风险指标目录：裂缝场景当前治理 `value / sensor_state`，GNSS 场景当前治理 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ / sensor_state`，其中仅 `value` 与 `gpsTotalX / gpsTotalY / gpsTotalZ` 允许进入风险闭环。`2026-04-10` 起，南方测绘激光测距产品 `nf-monitor-laser-rangefinder-v1` 也复用 `phase1-crack` 底层治理链：canonical `identifier` 仍固定为 `value / sensor_state`，但 compare、正式字段中文名和对象洞察重点指标对外统一显示为 `激光测距值 / 传感器状态`，且风险指标目录当前只发布 `value`。`2026-04-11` 起，南方测绘深部位移产品又补齐 `phase3-deep-displacement` 规范链：canonical `identifier` 固定为 `dispsX / dispsY / sensor_state`，其中风险指标目录只发布 `dispsX / dispsY`，`sensor_state` 继续保留治理语义。同日起，翻斗式雨量计产品 `nf-monitor-tipping-bucket-rain-gauge-v1` 也补齐 `phase4-rain-gauge` 规范链：canonical `identifier` 固定为 `value / totalValue`，compare、正式字段中文名和对象洞察重点指标对外统一显示为 `当前雨量 / 累计雨量`，其中风险指标目录只发布 `value`。compare 行会补充 `normativeIdentifier / normativeName / riskReady / rawIdentifiers`，apply 成功后会返回 `releaseBatchId`。`2026-04-08` 起，审批执行真正落库后的发布批次还会补齐 `approvalOrderId / releaseReason / releaseStatus`，风险指标目录也会沉淀 `releaseBatchId / normativeIdentifier / riskCategory / metricRole / lifecycleStatus` 与扩展语义能力字段，供风险对象、对象洞察和运营分析统一按目录谱系读取；`2026-04-10` 起，产品合同发布 / 回滚 / 原单重提又继续切换为“系统固定复核人”模式：`/products` 不再要求手工填写复核人用户 ID，后端会按 `sys_governance_approval_policy` 自动解析固定复核人，初始化基线账号为 `governance_reviewer`。同日 `契约字段` 还继续补齐首版 `版本台账`：页面会按产品读取最近 `20` 个正式合同发布批次，并按选中 `releaseBatchId` 联动风险指标目录、按基线批次对比跨批次字段 / 风险指标目录差异，直接回答“哪一版合同发布了哪些风险指标、这一版相对上一版变了什么”。`2026-04-13` 起，正式合同发布后的 canonical resolver 真相也会物化到 `iot_product_metric_resolver_snapshot`，设备域按 `productId + 最新 releaseBatchId` 在内存优先复用同一份发布快照，避免对象洞察、history/latest、风险目录和运行时解析重复各自猜 alias。桥层同时新增 first-class `iot_vendor_metric_mapping_rule` 与 `/api/device/product/{productId}/vendor-mapping-rules` 后端 CRUD，当前产品级治理入口继续沿用 `productId` 路径承载规则归属，但写侧已支持 `PRODUCT / DEVICE_FAMILY / SCENARIO / PROTOCOL` 四级 scope，运行时另预留 `TENANT_DEFAULT` 作为兜底层，命中优先级固定为 `PRODUCT > DEVICE_FAMILY > SCENARIO > PROTOCOL > TENANT_DEFAULT`，用于维护 `rawIdentifier -> targetNormativeIdentifier` 的显式治理规则、关系条件 JSON 和归一化规则 JSON，作为后续免代码接入扩展点。该能力当前只覆盖裂缝、GNSS、南方激光测距、深部位移与翻斗式雨量计治理最小切片，不代表任意设备厂商已经实现零代码接入。
`2026-04-09` 起，平台治理控制面前端已补齐 `/governance-task` 与 `/governance-ops`，统一消费 `/api/governance/work-items` 与 `/api/governance/ops-alerts`；首页管理视角相关待办会直接深链到对应控制面工作台。`2026-04-13` 起，`/products` 中当前聚焦产品的“待发布合同”提示不再先跳治理任务台，而是直接打开当前产品 `/products?openProductId=...&workbenchView=models` 契约工作区；“待绑定风险点”等控制面型事项继续深链到治理任务台。同日已在共享 `dev` 环境通过治理控制面浏览器冒烟计划，完成首页管理视角治理经营切片、治理任务台、治理运维台 `4/4` 场景复验；控制面读侧还已补齐运行态同步：`/api/governance/work-items` 分页前会把六类治理积压固化为正式工作项，`/api/governance/ops-alerts` 刷新同一告警时会保留人工状态，不再把 `ACKED / SUPPRESSED / CLOSED` 悄悄刷回 `OPEN`。当前首轮仍只交付查询与列表收口，不代表首页其余角色视角已纳入完整驾驶舱验收。
`2026-04-09` 起，平台治理同时显式开放 `/governance-approval` 治理审批台与 `/governance-security` 权限与密钥治理页，并通过 `sql/init-data.sql` 默认回填管理/运维/开发角色菜单授权；后者默认展示治理权限矩阵和设备密钥轮换台账，只暴露密钥摘要与审计语义，不暴露明文。

### 当前构建模块基线
当前父 `pom.xml` 激活 `12` 个模块：
- `spring-boot-iot-common`
- `spring-boot-iot-framework`
- `spring-boot-iot-auth`
- `spring-boot-iot-system`
- `spring-boot-iot-device`
- `spring-boot-iot-protocol`
- `spring-boot-iot-message`
- `spring-boot-iot-telemetry`
- `spring-boot-iot-rule`
- `spring-boot-iot-alarm`
- `spring-boot-iot-report`
- `spring-boot-iot-admin`

仓库里仍然保留了 `spring-boot-iot-gateway`、`spring-boot-iot-ota` 等额外模块目录，但当前活跃构建模块仍以父 `pom.xml` 为准。

### 已验证业务基线
当前已验证的基线包括：
- 产品新增 / 查询
- 产品物模型设计器（产品维度列表、新增、编辑、删除、类型切换、JSON 校验提示）
- 设备新增 / 查询
- HTTP 模拟设备上报
- MQTT 真实接入
- 通过 `mqtt-json` 进行协议解码
- 消息日志持久化
- 最新属性更新
- 设备在线状态更新
- 告警中心（告警列表、告警详情、告警确认、告警抑制、告警关闭）
- 事件处置（事件列表、事件详情、工单派发、工单接收 / 开始 / 完成、事件反馈、事件关闭）
- 风险点管理（风险点增删改查、风险点绑定）
- 阈值规则配置（规则增删改查）
- 联动规则与应急预案（规则增删改查、预案增删改查）
- 报表分析（风险趋势、告警统计、事件闭环、设备健康）
- 组织管理（树结构、增删改查）
- 用户管理（用户增删改查、密码重置）
- 角色管理（角色增删改查、用户角色查询）
- 区域管理
- 字典管理
- 通知渠道管理
- 审计日志管理

当前代码中还包含风险监测基线：
- `/risk-monitoring`
- `/risk-monitoring-gis`
- `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- `spring-boot-iot-alarm/.../RiskMonitoringController.java`

但除非 [docs/19-第四阶段交付边界与复验进展.md](docs/19-%E7%AC%AC%E5%9B%9B%E9%98%B6%E6%AE%B5%E4%BA%A4%E4%BB%98%E8%BE%B9%E7%95%8C%E4%B8%8E%E5%A4%8D%E9%AA%8C%E8%BF%9B%E5%B1%95.md) 已同步更新，否则这部分风险监测基线暂不计入正式交付范围。

## 真实环境规则
后续所有验收工作都必须使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml`，或使用覆盖该文件的环境变量。

禁止重新引入以下已废弃验收路径：
- 旧 H2 验收 profile
- 独立 H2 schema 验收脚本
- 仅 H2 验收路径
- 已废弃的前端浏览器自动化验收路径

如果环境访问受阻，必须明确报告环境阻塞，不得用已废弃的 H2 回退链路替代真实环境验收。

## 文档维护规则
- 任何影响行为、API、流程、页面结构、启动步骤、校验流程、配置预期或产品定位的前后端改动，都必须原位更新现有文档。
- 必须同步更新 `docs/` 下对应文档。
- 必须检查 `README.md` 和 `AGENTS.md` 是否也需要同步更新。
- 不得创建 `README-v2.md`、`api-new.md`、`new-frontend-doc.md` 之类的平行替代文档。
- 该规则适用于所有编码助手和编码模型，包括 Codex、Qwen Code 等。

## 数据库 Schema 协作规则
- 数据库结构真相源固定为 `schema/` registry；禁止直接手改 `sql/init.sql`、`sql/init-tdengine.sql`、`schema/generated/mysql-schema-sync.json`、runtime bootstrap manifest，或 `scripts/run-real-env-schema-sync.py` 顶部结构清单而不回写 registry。
- 新增表、扩字段、改中文注释、调整生命周期的标准流程固定为：修改 `schema/` -> 执行 `python scripts/schema/render_artifacts.py --write` -> 执行 `python scripts/schema/check_schema_registry.py` -> 同步更新 `docs/04-数据库设计与初始化数据.md`。
- `sql/init-data.sql` 继续只承载演示数据、权限基线和共享环境 seed，不并入运行时 bootstrap，也不由 MySQL active schema runner 自动执行。
- `risk_point_highway_detail` 当前生命周期固定为 `archived`，不进入默认 init / schema sync / runtime bootstrap；若后续要恢复或删除，必须先更新 registry 生命周期和本文档，再实施。
- TDengine 运行时自动补齐边界固定为 `iot_device_telemetry_point`、`iot_raw_measure_point`、`iot_raw_status_point`、`iot_raw_event_point` 四个对象；`iot_agg_measure_hour` 继续要求先执行 `sql/init-tdengine.sql` 手动初始化。

## 智能助手协作入口规则
- 你本人日常发任务时，优先使用 `docs/skills/ai-task-intake/SKILL.md` 中的“你本人专用版六条超短清单”。
- 如果你已经明确知道任务场景，只想直接复制现成提示词，优先使用 `docs/17-智能助手任务发起模板速查.md`。
- 如果六条超短清单装不下，再使用 `docs/09-GPT接管提示模板.md` 中的任务卡模板。
- 如果你已经知道要做什么，但拿不准该调用哪些技能或按什么顺序协作，再查阅 `docs/10-智能助手技能与任务选型指南.md`。
- 只有任务跨模块、跨验收、跨数据库，或短任务卡仍然装不下时，再使用 `docs/template/README.md` 索引的长模板。

## Git 分支治理规则
- `master` 是生产分支，禁止本地直接开发、直接提交、直接合并、直接推送。
- 智能助手默认只允许在 `codex/dev` 上实施编码、验证、整理提交与交付准备。
- 如因隔离或排障临时使用其他本地分支，产出也必须先回到 `codex/dev`；未经用户明确授权，禁止把任何本地分支直接合入或推送到 `master`。
- 每次编码前必须先检查当前分支；若当前不在 `codex/dev`，必须先停止并报告原因，经用户确认后切回 `codex/dev` 或按其指定流程处理。
- 如果任务涉及生产发布或 `master` 相关操作，智能助手只能提供建议流程、整理说明或等待明确授权，不得自行在本地 `master` 上开发、修复或合并代码。

## 工作区路径兼容规则
- 共享 Windows 10 环境的工作区根目录可能是 `E:\idea\ghatg\spring-boot-iot`。
- 其他环境可能使用不同的绝对路径。
- 不得把某一个绝对工作区路径写死回脚本或文档，作为唯一合法路径。
- 优先通过当前脚本位置、当前工作目录或环境配置推导仓库根目录。
- 当文档需要展示绝对路径示例时，必须明确标注 `E:\idea\ghatg\spring-boot-iot` 只是 Windows 共享环境示例，而不是通用固定路径。

## 前端编码与一致性规则
- 任何 `spring-boot-iot-ui` 下的页面或样式改动，都必须保证 UTF-8 可读，不得把终端乱码写进 `.vue`、`.ts`、`.css`、`.json`、`.md` 文件。
- 在 Windows 终端编辑前端文件前，优先使用 UTF-8 查看 / 校验方式，例如 `chcp 65001` 加 `Get-Content -Encoding UTF8`，确保终端显示内容与文件真实内容一致。
- 修改前端文本、标签、占位符、注释或文档后，必须自检是否出现 `鍒�`、`褰�`、`璇�`、`鐢�` 这类乱码，发现后必须修复。
- 新的页面优化工作必须优先复用现有共享页面模式：`PanelCard`、`StandardWorkbenchPanel`、`StandardListFilterHeader`、`StandardPagination`、`useServerPagination`、`StandardTableToolbar`、`StandardTableTextColumn`、`StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter`、`confirmAction`、`StandardInlineState`、`IotAccessPageShell`、`IotAccessTabWorkspace`、共享全局列表样式和现有设计令牌。若现有标准模式已经适配，不要再新增页面私有列表 / 分页 / 详情弹层样式。
- `接入智维` 下的页面如果存在多个并列内容域，只有在确有两个及以上真实业务视图时才使用同页 Tab 工作区表达，不要再新增页面私有 pill、假页签、说明墙或二级轻路由；同一路由 query 变更必须复用共享滚动行为，避免筛选 / 分页 / Tab 同步后内容区跳顶。
- `接入智维` 单主列表页统一只保留“全局壳层面包屑 + 当前模块主工作台”两层结构；`IotAccessPageShell` 不得再为同一路径重复渲染页内面包屑，中间重复层级、说明墙、第三层标题壳和第二条“接入智维 / 当前模块”导航条以后都不得回流。
- `链路追踪台`、`数据校验台`、`链路验证中心` 等诊断页顶部不得再回流右上角跨页功能菜单；跨页协同统一下沉到正文判断区、行级动作、详情抽屉提示或来源上下文恢复，不要再把 `异常观测台 / 数据校验台 / 链路追踪台` 一类按钮放回页头。
- 总览、工作台、抽屉和确认弹窗交互必须保持统一品牌 / 强调配色体系。除非产品需求明确记录例外，否则不要再为单页引入新的蓝 / 橙 / 紫色私有配色。
- 如果前端改动引入或暴露了样式漂移、重复列表布局或分页行为不一致问题，结束任务前必须把问题和预防规则记录到 `docs/15-前端优化与治理计划.md`。

## 编码前必读

### 所有任务的最小阅读集
- `README.md`
- `docs/README.md`
- `docs/01-系统概览与架构说明.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/07-部署运行与配置说明.md`
- `docs/08-变更记录与技术债清单.md`

### 按需补读
- 测试 / 验收 / 回归：`docs/05-自动化测试与质量保障.md`、`docs/真实环境测试与验收手册.md`、`docs/21-业务功能清单与验收标准.md`
- 前端工作：`docs/06-前端开发与CSS规范.md`、`docs/15-前端优化与治理计划.md`
- MQTT / 协议 / 载荷解析：`docs/05-protocol.md`、`docs/14-MQTTX真实环境联调手册.md`
- 可观测 / Trace / 通知：`docs/11-可观测性、日志追踪与消息通知治理.md`
- 帮助中心 / 系统内容治理：`docs/12-帮助文档与系统内容治理.md`
- 多租户 / 数据权限 / 组织范围：`docs/13-数据权限与多租户模型.md`
- 第四阶段范围或交付边界：`docs/19-第四阶段交付边界与复验进展.md`、`docs/21-业务功能清单与验收标准.md`
- 阶段规划 / 下一轮迭代：`docs/16-阶段规划与迭代路线图.md`、`docs/19-第四阶段交付边界与复验进展.md`
- 智能助手协作 / 接手模板：`docs/skills/ai-task-intake/SKILL.md`、`docs/09-GPT接管提示模板.md`、`docs/10-智能助手技能与任务选型指南.md`、`docs/17-智能助手任务发起模板速查.md`、`docs/template/README.md`

### 不再视为主编码依赖
- 兼容入口页
- `docs/archive/*`
- 历史问题台账 / 复盘记录
- 之前位于 `docs/template/*` 下的薄包装页；如需入口请使用 `docs/template/README.md`

## 硬约束
- 项目名必须保持：`spring-boot-iot`
- 基础包必须保持：`com.ghlzm.iot`
- 第一阶段必须保持模块化单体
- `spring-boot-iot-admin` 是唯一启动模块
- 不得破坏模块边界
- 不得把持久化逻辑移入协议适配层
- 不得把业务逻辑写进 Controller
- 除非确有必要，不要引入重型依赖

## 模块边界
- `spring-boot-iot-common`：常量、异常、响应模型、工具类
- `spring-boot-iot-framework`：配置、安全、Redis、MyBatis、全局处理器
- `spring-boot-iot-auth`：只负责认证
- `spring-boot-iot-system`：用户、角色、组织、区域、字典、渠道、审计
- `spring-boot-iot-device`：产品、设备、影子、属性、消息日志
- `spring-boot-iot-gateway`：网关与子设备拓扑
- `spring-boot-iot-protocol`：协议适配器、协议模型、编解码
- `spring-boot-iot-message`：接入入口与分发，仅负责入口和调度
- `spring-boot-iot-telemetry`：TDengine 时序落库、MEASURE 小时聚合写入、latest 查询与历史遥测存储抽象
- `spring-boot-iot-rule`：规则引擎
- `spring-boot-iot-alarm`：告警中心、事件、风险点、规则、预案、风险监测
- `spring-boot-iot-report`：报表分析
- `spring-boot-iot-ota`：OTA 升级
- `spring-boot-iot-admin`：应用装配与启动

## 代码风格
- Controller 只处理请求 / 响应
- Service 负责编排
- Mapper 负责数据库访问
- 业务错误使用 `BizException`
- 统一 API 响应使用 `R`
- 命名保持与文档一致
- 核心逻辑意图不明显时，补短小中文注释
- 优先提交小而聚焦的改动

## 编码前
1. 先检查当前分支是否符合分支治理规则；默认必须是 `codex/dev`，若当前不是则先停止并报告。
2. 先总结任务
3. 列出受影响模块
4. 说明实现计划
5. 说明假设

## 编码后
1. 列出变更文件
2. 说明改了什么
3. 说明如何运行或验证
4. 列出未完成部分
5. 如果行为发生变化，必须原位更新现有文档
6. 说明本次更新了哪些文档，包括是否更新了 `README.md` 和 `AGENTS.md`

## 推荐命令
- 构建：`mvn -s .mvn/settings.xml clean install -DskipTests`；若仓库不存在 `.mvn/settings.xml`，直接改为 `mvn clean install -DskipTests`
- 本地质量门禁：`node scripts/run-quality-gates.mjs`；当前已包含治理契约专项门禁，若只需验证产品物模型治理主链路，可执行 `node scripts/run-governance-contract-gates.mjs`
- 启动应用（macOS / Linux、Windows CMD）：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`；若仓库不存在 `.mvn/settings.xml`，直接省略 `-s .mvn/settings.xml`
- 启动应用（Windows PowerShell）：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"`；若仓库不存在 `.mvn/settings.xml`，直接省略 `-s .mvn/settings.xml`
- 严格真实环境验收启动（跨模块改动推荐）：`mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 后执行 `java -jar spring-boot-iot-admin/target/spring-boot-iot-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev`
- 后端验收：`powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1`
- 前端验收：`powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1`
- message-flow 验收：`python scripts/run-message-flow-acceptance.py --expired-trace-id <已过期TraceId>`
- 测试：`mvn -s .mvn/settings.xml test`；若仓库不存在 `.mvn/settings.xml`，直接改为 `mvn test`

## 阶段执行顺序

### 第一阶段
1. 创建 Maven 多模块结构
2. 增加基础基础设施类
3. 增加数据库实体和 Mapper
4. 增加 Service 和 Controller
5. 实现 HTTP 上报链路
6. 验证属性和消息日志持久化
7. 验证在线状态更新

### 第二阶段
1. 实现 MQTT 接入骨架
2. 实现 MQTT Topic 解析
3. 实现基础设备认证
4. 实现设备会话和在线状态处理
5. 完成真实 MQTT 上行验收
6. 实现最小 MQTT 下行发布
7. 预留子设备 Topic 解析扩展点

### 第三阶段
1. 实现命令闭环
2. 实现网关 / 子设备业务闭环
3. 实现基础规则引擎

### 第四阶段
1. 实现告警中心基线
2. 实现事件处置基线
3. 实现风险点管理
4. 实现阈值规则配置
5. 实现联动规则和应急预案
6. 实现报表分析
7. 实现系统治理
8. 只有在进度文档同步更新后，才交付风险监测和 GIS

## 完成定义

### 第一阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 使用 `application-dev.yml` 的 HTTP 主链路真实环境验收通过
- 以下 API 必须保持可用：
  - `POST /device/product/add`
  - `GET /device/product/{id}`
  - `POST /device/add`
  - `GET /device/{id}`
  - `GET /device/code/{deviceCode}`
  - `POST /message/http/report`
  - `GET /device/{deviceCode}/properties`
  - `GET /device/{deviceCode}/message-logs`

### 第二阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 真实环境下 MQTT 标准 Topic 上行验收通过
- 真实环境下旧 `$dp` 兼容链路验收通过
- 真实环境下 MQTT 最小下行发布验收通过

### 第三阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 命令闭环真实环境验收通过
- 网关 / 子设备真实环境验收通过
- 规则引擎真实环境验收通过

### 第四阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 告警中心真实环境验收通过
- 事件处置真实环境验收通过
- 风险配置真实环境验收通过
- 报表分析真实环境验收通过
- 系统治理真实环境验收通过
- `docs/19-第四阶段交付边界与复验进展.md` 与 `docs/21-业务功能清单与验收标准.md` 必须与真实已交付范围保持一致

## 已知环境说明
- 在部分 JDK 17 环境下，`DeviceMessageServiceImplTest` 仍可能失败，因为 Mockito inline mock maker 无法自附加 ByteBuddy agent。
- 除非有真实业务回归证据，否则把它视为本地测试环境问题。

## 鉴权基线说明（2026-03-16）
- `/api/auth/login` 是 Web 客户端默认登录入口。
- `/message/http/report`、`/api/cockpit/**`、actuator 与 swagger / doc 端点继续保持公开。
- 其他 API 默认都受 JWT Bearer 鉴权保护。
- 前端在登录后应附加 `Authorization: Bearer <token>`，并在收到 `401` 时清理本地认证状态。
