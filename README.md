# spring-boot-iot

> 文档定位：项目总入口，提供当前交付基线、真实环境启动口径和核心文档导航。
> 适用角色：研发、运维、测试、交付、智能助手协作人员。
> 权威级别：一级入口。
> 上游来源：当前代码、`pom.xml`、`application-dev.yml`、核心权威文档。
> 下游消费：接手研发、环境启动、任务实施、帮助中心选题。
> 变更触发条件：交付边界、启动方式、最小阅读集、文档体系结构变化。
> 更新时间：2026-04-26

## 项目简介

`spring-boot-iot` 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 平台，当前覆盖：

- 设备接入：产品定义、设备资产、HTTP / MQTT 上报、协议解析、消息日志、最新属性、在线状态、最小下行
- 风险处置：实时监测、GIS 态势图、告警中心、事件协同、风险对象、阈值策略、联动编排、应急预案、运营分析
- 平台治理：组织、账号中心、用户、角色、菜单、区域、字典、治理任务台、治理运维台、治理审批台、权限与密钥治理、通知渠道、站内消息、帮助文档、审计中心
- 质量与协作：质量工场（业务验收台、研发工场、执行中心、结果与基线中心；业务验收台支持按预置验收包选择环境/账号模板/模块范围并一键查看是否通过，质量工场支持 `P0 全流程业务验收` 预置包，按同一 `runId` 贯通业务验收台结果页与结果与基线中心证据，结果与基线中心支持最近运行读取、证据清单/原文预览与兼容手工导入）、真实环境验收、智能助手接手模板、帮助中心消费层治理
- 质量工场覆盖治理：`node scripts/auto/generate-acceptance-coverage.mjs` 可从统一验收注册表与业务验收包生成覆盖矩阵，用于检查 P0/P1/P2 覆盖、执行器分布、责任域和缺失引用；`node scripts/auto/diff-acceptance-coverage.mjs` 可对比两份覆盖矩阵并输出趋势证据；`node scripts/auto/run-acceptance-readiness.mjs` 可聚合覆盖矩阵、策略门禁和覆盖趋势，生成封板或 CI 前的 readiness evidence

当前唯一启动模块是 `spring-boot-iot-admin`，当前父 `pom.xml` 激活 `12` 个模块：

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

## 当前交付基线

- Phase 1~3 主链路已形成长期稳定基线。
- 设备接入主链路当前已重构为固定 `Pipeline`：HTTP / MQTT 统一按 `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE` 执行，不再依赖“旁路日志推断”判断阶段顺序。
- `spring-boot-iot-telemetry` 已进入 telemetry v2 基线；`application-dev.yml` / `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`。标准化后的 `properties` 会在 `PAYLOAD_APPLY` 之后优先批量写入 TDengine v2 raw stable，再异步投影 MySQL latest、按配置镜像 legacy stable，并可在 `iot.telemetry.aggregate.enabled=true` 且 `iot.telemetry.aggregate.hourly-enabled=true` 时把 `MEASURE` 数值点位异步写入 `iot_agg_measure_hour`。应用启动时会自动补齐 `iot_device_telemetry_point` 兼容表和 `iot_raw_measure_point / iot_raw_status_point / iot_raw_event_point` 三张 raw stable；当前共享 TDengine 基线不再依赖 `COMPOSITE KEY`，raw 写入改由“以 `ingested_at` 为基准的唯一单调 `ts` 行键”保留同一次 `$dp` 上报中的多指标点位，并继续用 `reported_at` 保存设备真实采集时间。对象洞察等历史趋势默认优先按 `ingested_at/ts` 聚桶，不再把传感器采集时间直接当作趋势主时间轴；`POST /api/telemetry/history/batch` 会先按请求 `rangeCode` 限定查询窗口，再从 v2 raw / 兼容表 / legacy fallback 读取窗口内历史点并补零，避免长历史设备被早期数据挤占后出现整段 `0` 趋势。`iot_agg_measure_hour` 则继续要求先执行 `sql/init-tdengine.sql` 手动初始化，运行时只自动派生 `tb_ah_*` child table。
- `application-dev.yml`、`application-prod.yml`、`application-test.yml` 当前已显式固化 MySQL 主库 Hikari 基线，默认 `maximum-pool-size=30`、`minimum-idle=5`、`keepalive-time=300000`、`max-lifetime=1800000`、`leak-detection-threshold=20000`；dev 的 TDengine `slave_1` 也已补齐独立 Hikari 基线，避免共享环境继续沿用默认 `10` 连接池。
- `TELEMETRY_PERSIST` 当前采用非阻塞失败语义：TDengine 写失败只会把该步骤标记为 `FAILED` 并写结构化日志，不回滚 MySQL 消息日志、最新属性和设备在线状态。
- `GET /api/telemetry/latest` 当前已改为真实查询：`tdengine` 模式默认先读 telemetry v2 latest MySQL 投影表 `iot_device_metric_latest`，并在 `legacy-read-fallback-enabled=true` 时继续补齐 legacy stable + `iot_device_telemetry_point` 缺失指标；`mysql` 模式兼容回退到 `iot_device_property`。
- `2026-04-24` 起，`PAYLOAD_APPLY` 写侧会继续复用 published resolver snapshot 做运行态字段归一：当上报裸标识（如 `gX / angle / value`）能在当前产品已发布正式字段中唯一命中同名后缀（如 `L1_JS_1.gX / L1_QJ_1.angle / L1_LF_1.value`）时，latest 写入、telemetry 后续持久化和运行态证据统一使用正式全路径标识；若同名后缀存在多个候选则保持原始标识，不做猜测归一。若发布快照、正式映射快照与草稿映射规则都未命中，则最后按活动规范库执行 `Lx_XX_n + 叶子字段` 兜底归一，当前覆盖 `L3_QW/L3_YL/L3_DB/L4_NW/L4_LD`，并扩展到附件规范中的 `L1_ZD/L2_SF/L3_CJ/L3_QY/L4_BMLS`；其中泥位计原始 `L4_NW_1` 会收口为 canonical `value`。`2026-04-25` 起，规范兜底从“命中/未命中”升级为 `MATCHED / AMBIGUOUS / MISSED`：冲突候选不自动归一，compare 行需展示命中来源、命中依据和候选项，并进入人工确认或映射规则建议；前端不会把冲突候选自动计入待生效或提交审批；运行态规范库可使用轻量缓存，但仍只在 published snapshot 与映射规则之后兜底，不改变正式合同真相。`2026-04-25` 起，compare 结果里的 `AMBIGUOUS` 行提供“去映射规则治理”，会携带首个 `rawIdentifiers[]` 到映射建议高亮；`MISSED` 行提供“补名称/单位”，会把 raw identifier 预填到运行态名称/单位治理表单。
- `2026-04-25` 起，真实环境同步脚本在写入 `iot_normative_metric_definition` 种子前会先检查规范字段 seed 冲突：`id` 必须唯一，且活动定义按运行态兜底实际使用的 `监测内容编码 / 监测类型编码 / identifier` 也必须唯一；若发现重复会停止同步并输出冲突明细，避免共享环境把同一上报编码刷成多条 active 候选。
- `2026-04-25` 起，规范字段库补齐轻量导入治理入口：后端新增 `POST /api/device/normative-metrics/import/preview` 与 `POST /api/device/normative-metrics/import/apply`，前端在 `/products/:productId/mapping-rules` 增加“规范字段库导入”区块。该入口支持粘贴 JSON 数组或 `{ "items": [...] }`，先按必填项、重复 `id` 和活动兜底键冲突完成预检，无冲突后再落库到 `iot_normative_metric_definition`；它只维护规范字段库，不替代 `contracts` 的正式合同 compare/apply，也不绕过后续合同发布与风险目录治理。
- `POST /api/telemetry/migrate-history` 当前提供 TDengine 历史补迁入口：默认优先读取 `iot_device_telemetry_point` 标准化兼容表，缺失时再按 `specsJson.tdengineLegacy` 与 `metadataJson.tdengineLegacy` 映射回放 legacy stable；该迁移由业务代码手动触发，不会在启动阶段自动全量回灌历史数据。
- MQTT consumer 当前默认启用 Redis 租约式 `cluster-singleton`：同一套共享 MySQL / Redis / TDengine 环境里只允许 1 个 leader 节点订阅 Broker；`/api/message/mqtt/report/publish` 当前统一改为走独立 publisher 客户端，leader 不再复用订阅连接做模拟上行，standby 也继续保持可调用。
- 设备离线超时调度当前也启用独立 Redis 单实例锁：默认通过 `iot:device:offline-timeout:leader` 只允许 1 个 leader 节点执行离线收口，避免共享环境多实例或旧节点把设备状态反复写回离线。
- 2026-03-27 起，MQTT 无效上报治理已进入默认基线：首批对 `DEVICE_NOT_FOUND` 与 `EMPTY_DECRYPTED_PAYLOAD` 执行 Redis 分钟桶计数与冷却抑制，`iot_device_access_error_log` 收口为失败样本归档，未登记设备最新态改由 `iot_device_invalid_report_state` 承载，设备补录 / 更换成功后会自动标记对应记录为已解封。
- Phase 4 已完成并纳入真实环境验收基线的能力，包括实时监测、GIS 态势图、告警、事件、风险策略、报表分析、系统治理和系统内容治理。
- 平台治理身份基线已补齐到“账号中心可用闭环 + 用户/机构/角色/数据范围基础模型”：右上角账号菜单统一为 `账号中心 / 修改密码 / 退出登录`；账号中心抽屉当前集中展示并维护 `基础资料 / 机构与角色 / 安全信息 / 实名资料`；`/api/auth/me` 统一返回租户、主机构、登录方式、最近登录和角色数据范围摘要，`PUT /api/user/profile` 用于当前登录用户维护昵称、实名、手机号、邮箱和头像。2026-04-03 起，角色治理以及 `区域版图 / 数据字典 / 通知编排 / 帮助文档 / 审计中心` 也已并入第一批查询侧收口：对应列表、分页、详情与新增/编辑/删除/测试接口统一按当前登录人的 `tenantId` 执行；帮助文档消费端 `access/list`、`access/page`、`access/{id}` 也已先按租户边界过滤，再叠加角色与页面路径权限。
- 当前共享开发环境已于 2026-03-24 完成 `/api/risk-monitoring/*`、`/risk-monitoring`、`/risk-monitoring-gis` 真实环境复验，风险监测基线正式纳入交付。
- `/products` 当前正式定位为 `产品定义中心`：列表主台账继续承担产品定义主入口，同时显式承接契约治理、版本治理与风险目录入口。产品定义中心自 2026-04-19 起已拆为 `/products` 主列表 + `/products/:productId/overview`、`/products/:productId/devices`、`/products/:productId/contracts`、`/products/:productId/mapping-rules`、`/products/:productId/releases` 五段详情子路由；`GET /api/device/product/{id}/overview-summary` 负责总览聚合，旧 `openProductId + workbenchView=models` 深链仅作为兼容入口并会落到 `contracts` 子页。`2026-04-20` 起，`/products` 列表中的新增/编辑共用同一套抽屉表单布局；编辑页只保留与新增产品一致的基础属性项和 `对象洞察配置`，旧 `产品经营工作台 / 编辑档案` 壳层已下线。对象洞察配置当前补充支持趋势重点指标单位维护，快捷加入趋势时会继承正式字段 `specsJson.unit`，候选状态会区分“已加入趋势”与“当前未加入趋势”。当前 `contracts` 页承接 `契约字段` 与 `契约字段描述`：同页完成样本输入、识别结果、本次生效和正式字段查看，不再打开二层抽屉，也不再提供 `model-candidates`、规范预设、运行期自动提炼或 `manualDraftItems` 人工补录。样本类型固定为 `业务数据 / 状态数据`，设备结构固定为 `单台设备 / 复合设备`；单次只支持 `1` 台设备样本 JSON，页面默认显示 `单台设备（按产品形态自动识别）`，不再新增第三组切换。正式字段标识的收口不由 `single / composite` 两个枚举单独决定：单台业务样本默认使用 direct canonical 字段；单台状态样本会按“样本是否命中状态前缀 + 已发布状态字段”自动判定是否保留全路径；复合设备则把逻辑通道编码仅作为归属与证据，并按关系映射归一为子产品 canonical 字段。compare 请求还支持隐藏透传 `manualExtract.contractIdentifierMode=DIRECT|FULL_PATH`，前台默认不传，由后端按产品形态自动判定，并把 `resolvedContractIdentifierMode` 回写到 `manualSummary / runtimeSummary`。复合设备模式需显式提交 `父设备编码 + 逻辑通道编码/子设备编码` 映射；compare 会优先沿用 `relationMappings[]` 中的 `canonicalizationStrategy / statusMirrorStrategy`，未显式给出时按逻辑通道类型兼容推断：`L1_LF_* -> value / sensor_state`，`L1_SW_* -> 保留子设备业务字段（如 dispsX / dispsY），并把 S1_ZT_1.sensor_state.<logicalChannelCode> 归一为子产品 sensor_state`。采集器自身 `temp / humidity / signal_4g` 等状态字段继续留在采集器产品，不会带入子产品；其中 `nf-collect-rtu-v1`（`南方测绘 采集型 遥测终端`）虽然仍按 `nodeType=1` 直连产品建档，但在 `状态数据 + 复合设备` 的 compare/apply 中也必须命中同一采集器父产品边界：`S1_ZT_1.*` 中除 `S1_ZT_1.sensor_state.<logicalChannelCode>` 外的父设备状态叶子字段都应继续保留为父产品候选，例如 `ext_power_volt / solar_volt / battery_dump_energy / battery_volt / supply_power / consume_power / temp / humidity / temp_out / humidity_out / lon / lat / signal_4g / signal_NB / signal_db / sw_version`；单台深部位移设备沿用同一正式物模型，若运行期尚未收到状态报文，可以暂时只有 `dispsX / dispsY` 而没有 `sensor_state` 最新值。`2026-04-11` 起，采集器产品在复合设备 compare/apply 写侧只治理采集器自身运行状态字段，`value / sensor_state / dispsX / dispsY` 一类子设备正式字段不再允许从采集器产品发布；上述 `nodeType=1` 的采集型遥测终端同样适用这条边界。`2026-04-20` 起，同一次 compare 的运行态补证 / `继续观察` 行也会沿用同一 `resolvedContractIdentifierMode`，不再把雨量计这类单台状态字段拆成一部分全路径、一部分尾字段短标识。compare/apply 仍直接写入 `iot_product_model`，不新增平行草稿表。`2026-04-06` 起首批 `phase1-crack + phase2-gnss` 双场景已补齐规范字段库、厂商字段证据、合同发布批次和风险指标目录：裂缝场景当前治理 `value / sensor_state`，GNSS 场景当前治理 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ / sensor_state`，其中仅 `value` 与 `gpsTotalX / gpsTotalY / gpsTotalZ` 允许进入风险闭环。`2026-04-10` 起，南方测绘激光测距产品 `nf-monitor-laser-rangefinder-v1` 也复用 `phase1-crack` 底层治理链：canonical `identifier` 仍固定为 `value / sensor_state`，但 compare、正式字段中文名和对象洞察重点指标对外统一显示为 `激光测距值 / 传感器状态`，且风险指标目录当前只发布 `value`。`2026-04-11` 起，南方测绘深部位移产品又补齐 `phase3-deep-displacement` 规范链：canonical `identifier` 固定为 `dispsX / dispsY / sensor_state`，其中风险指标目录只发布 `dispsX / dispsY`，`sensor_state` 继续保留治理语义。同日起，翻斗式雨量计产品 `nf-monitor-tipping-bucket-rain-gauge-v1` 也补齐 `phase4-rain-gauge` 规范链：canonical `identifier` 固定为 `value / totalValue`，compare、正式字段中文名和对象洞察重点指标对外统一显示为 `当前雨量 / 累计雨量`，其中风险指标目录只发布 `value`。compare 行会补充 `normativeIdentifier / normativeName / riskReady / rawIdentifiers`，apply 成功后会返回 `releaseBatchId`。若产品暂未配置专用场景识别，compare 还会按原始上报标识的 `Lx_XX_n` 前缀与叶子字段在规范库做兜底匹配。`2026-04-08` 起，审批执行真正落库后的发布批次还会补齐 `approvalOrderId / releaseReason / releaseStatus`，风险指标目录也会沉淀 `releaseBatchId / normativeIdentifier / riskCategory / metricRole / lifecycleStatus` 与扩展语义能力字段，供风险对象、对象洞察和运营分析统一按目录谱系读取。`2026-04-10` 起，产品合同发布 / 回滚 / 原单重提又继续切换为“系统固定复核人”模式：`/products` 不再要求手工填写复核人用户 ID，后端会按 `sys_governance_approval_policy` 自动解析固定复核人，初始化基线账号为 `governance_reviewer`。`2026-04-20` 起，映射规则发布 / 回滚审批也沿用同一固定复核人口径；历史共享库若缺 `VENDOR_MAPPING_RULE_PUBLISH / VENDOR_MAPPING_RULE_ROLLBACK`，需执行 `python scripts/run-real-env-schema-sync.py` 补齐，映射规则台账提交失败时页面当前只保留后端业务提示，不再叠加通用“系统繁忙”。同日 `契约字段` 还继续补齐首版 `版本台账`：页面会按产品读取最近 `20` 个正式合同发布批次，并按选中 `releaseBatchId` 联动风险指标目录、按基线批次对比跨批次字段 / 风险指标目录差异，直接回答“哪一版合同发布了哪些风险指标、这一版相对上一版变了什么”。`2026-04-21` 起，`/products/:productId/mapping-rules` 又新增 `运行态名称/单位治理` 区块：面向尚未形成正式字段、但已在 latest / history / `/insight` 中出现的 raw identifier，支持按 `PRODUCT / DEVICE_FAMILY / SCENARIO / PROTOCOL / TENANT_DEFAULT` 维护显示名称与单位；该能力只影响读侧展示，不改写 `iot_product_model`、正式合同发布批次或 resolver 快照。对象洞察与属性/历史读侧当前统一按 `正式字段名称/单位 -> 运行态显示规则 -> latest 属性 -> raw identifier` 的顺序取展示信息。`2026-04-09` 起，`/products` 的 `当前已生效字段` 支持直接维护正式中文名称，对象洞察读取 `设备属性快照` 与 `属性趋势预览` 时会优先复用最新正式字段名；趋势时间维度固定为 `近一天 / 近一周 / 近一月 / 近一年` 四档，但页头不再重复渲染时间按钮，时间切换只保留在趋势卡内，默认进入 `近一天` 并按小时桶展示最近 `24` 小时；24 小时视图会收窄两端纯补零空窗，且不再额外渲染“支持按近一天查看设备监测数据趋势”说明和图前指标标签胶囊。对象洞察重要指标的正式真相源当前也已固定为 `iot_product.metadata_json.objectInsight.customMetrics[]`：产品定义中心新增/编辑表单，以及 `契约字段 -> 当前已生效字段` 的趋势快捷动作都会共用这同一份产品元数据；其中只允许正式 `property` 字段进入趋势快捷治理，`/insight` 运行时会严格消费 `enabled / includeInTrend / includeInExtension / sortNo`，禁用或取消趋势的字段不会进入趋势查询，`includeInExtension=false` 的重点趋势指标会按 `sortNo` 前置展示。`/insight` 单位回退顺序固定为 `property.unit -> objectInsight.customMetrics[].unit -> specsJson.unit`。`2026-04-21` 起，`设备属性快照` 还会把“短标识 + 唯一同名全路径正式字段”与“非正式裸逻辑通道状态别名 + 对应 `S1_ZT_1.sensor_state.<logicalChannelCode>` 正式字段”统一收口为单行，避免 `AZI / L1_QJ_1.AZI`、`L1_QJ_1 / S1_ZT_1.sensor_state.L1_QJ_1` 这类重复展示；若当前行命中正式 `property` 字段，行内会提供 `修改名称/单位` 直达 `/products/:productId/contracts` 对应正式字段编辑态，不在 `/insight` 内维护第二套名称/单位编辑器。平台也不再默认按运行时属性自动生成 `监测数据 / 状态事件 / 运行参数` 三组趋势项，趋势预览默认空白，需在 `/products` 手工增加后才展示。`2026-04-19` 起，对象洞察读侧又补齐两条兼容边界：`nf-collect-rtu-v1` 在 `/insight` 中继续按采集器父设备处理，会读取 `子设备总览`；历史遗留短标识若能在当前 latest 属性里唯一命中同名后缀全路径字段，也会先升级为该运行态标识后再发起趋势查询。`2026-04-09` 起，平台治理同时显式开放 `/governance-approval` 治理审批台与 `/governance-security` 权限与密钥治理页，并通过 `sql/init-data.sql` 默认回填管理/运维/开发角色菜单授权；后者默认展示治理权限矩阵和设备密钥轮换台账，只暴露密钥摘要与审计语义，不暴露明文。该能力当前只覆盖裂缝、GNSS、南方激光测距、深部位移、翻斗式雨量计、泥位计，以及 `L3_QW/L3_YL/L3_DB/L4_NW/L4_LD` 与附件规范 `L1_ZD/L2_SF/L3_CJ/L3_QY/L4_BMLS` 的编码兜底治理最小切片，不代表任意设备厂商已经实现零代码接入。
- `2026-04-25` 起，风险指标目录发布链路补齐监测型产品字段级风险语义识别：多维位移产品仅将 `L1_LF_1.value` 作为裂缝量发布到风险目录，GNSS 位移产品仅发布 `L1_GP_1.gpsTotalX / gpsTotalY / gpsTotalZ`；短标识 `value / gpsTotalX / gpsTotalY / gpsTotalZ / dispsX / dispsY` 只有在已解析到裂缝、激光测距、雨量、GNSS 或深部位移产品/场景上下文时才会发布，`L1_LF_1` 这类无叶子通道占位不进入正式风险目录。倾角、加速度、泥位和 GNSS 基准站仍不进入本轮正式风险绑定目录。目录行会保留真实正式合同标识作为 `contractIdentifier`，并以 `normativeIdentifier / sourceScenarioCode` 记录规范语义；正式绑定候选仍只返回 `risk_metric_catalog` 字段，历史产品若尚未生成目录，`formal-metrics` 读侧会先按同一规则补齐目录再返回，不从 `iot_product_model` 或设备全量属性旁路兜底。
- `2026-04-18` 起，`接入智维` 资产底座新增 `/device-onboarding` 无代码接入台，作为零代码接入的第一阶段基础工作台。当前统一以 `iot_device_onboarding_case` 承载接入案例，并补齐 `iot_onboarding_template_pack` 模板包治理：页面支持案例分页、新建、编辑、状态刷新、单条触发标准接入验收、模板包分页/新建/编辑，以及对选中案例执行 `批量创建 / 批量套用模板包 / 批量触发验收`。服务端固定按 `PROTOCOL_GOVERNANCE / PRODUCT_GOVERNANCE / CONTRACT_RELEASE / ACCEPTANCE` 四步派生当前卡点：缺协议治理三件套时直接引导 `/protocol-governance`，协议信息齐全但未绑定产品或未发布正式合同批次时优先引导 `/products/:productId/contracts`；若缺产品主键则回到 `/products` 主列表，旧 query 深链继续兼容。已具备正式合同但缺 `deviceCode` 时会固定阻塞在 `ACCEPTANCE + BLOCKED`，只有补齐验收设备编码后才进入 `ACCEPTANCE + READY`。触发验收后页面会回显 `jobId / runId / status / failedLayers`，并可直接深链 `/automation-governance?tab=evidence&runId=...` 查看结果；批量操作失败结果会按原因分组收口。`2026-04-18` 同日，`/products/:productId/mapping-rules` 采纳草稿前也已允许显式选择 `PRODUCT / DEVICE_FAMILY / SCENARIO / PROTOCOL / TENANT_DEFAULT` 作用域，并按所选 scope 填写 `deviceFamily / scenarioCode / protocolCode`；`映射规则台账` 读侧会直接展示共享 scope 签名。该页当前仍只交付“统一 intake + 模板预填 + 验收触发 + 批量编排”的第一阶段能力，不代表平台已经完成任意设备的零代码接入闭环。
- `2026-04-18` 起，`/devices` 又补齐“建议优先、人工确认”的半自动接入最小闭环：带 `TraceId` 的未登记线索当前支持行级 `接入建议`，按 `traceId` 返回推荐产品、协议族、解密档案、协议模板与 `ruleGaps[]`；工具条新增 `批量转正式设备`，只允许对当前建议已达 `READY` 且无缺口的线索执行，并继续复用既有 `POST /api/device/add` 真相链创建设备。该能力当前仍属于半自动接入，不代表平台已经支持任意设备全自动零代码接入。
- `2026-04-18` 起，`/insight` 与 `/risk-point` 又补齐下游“建议优先”收口：采集器子设备总览与 `GET /api/device/product/{productId}/collector-children/recommended-metrics` 会基于已启用且 `insightEnabled=1` 的正式风险目录标识返回对象洞察建议指标；风险点待治理候选若命中当前产品已启用风险目录，也会回传 `catalogRecommended=true`、提升为 `HIGH` 优先级并追加“已命中正式风险目录，建议优先绑定”。该阶段仍只提供读侧建议，不会自动改写正式对象洞察配置，也不会自动完成风险绑定。
- `2026-04-09` 起，平台治理控制面前端已补齐 `/governance-task` 与 `/governance-ops`，统一读取 `/api/governance/work-items` 与 `/api/governance/ops-alerts`；首页管理视角相关待办仍会深链到对应控制面工作台。`2026-04-13` 起，`/products` 中当前聚焦产品的 `待发布合同` 提示不再先跳 `/governance-task`，而是直接打开 `/products/:productId/contracts` 的契约子页；旧 query 深链继续兼容到同一页面；`待绑定风险点` 等控制面型事项继续深链治理任务台。这样产品页基于 `coverage-overview + contract-release-batches` 的治理提示，与控制面基于已落库 `OPEN` 工作项的列表查询不再互相打架。同日已在共享 `dev` 环境通过治理控制面浏览器冒烟计划，完成首页管理视角治理经营切片、治理任务台、治理运维台 `4/4` 场景复验，证据见 `logs/acceptance/governance-control-plane-browser-summary-20260409222522.json` 与 `logs/acceptance/registry-run-20260409223438.json`。同日控制面读侧还补齐了运行态同步：`/api/governance/work-items` 分页前会把六类治理积压固化为正式工作项，`/api/governance/ops-alerts` 刷新同一告警时会保留人工状态，不再把 `ACKED / SUPPRESSED / CLOSED` 悄悄刷回 `OPEN`。当前首轮仍以前台列表与 query 驱动上下文查看为主，不把首页其余角色视角一并视为完整驾驶舱验收。
- 通知中心当前已具备 `system_error` 自动消息、工单相关自动消息，以及高优未读桥接既有通知渠道能力。
- 可观测当前已补齐规则化运维告警闭环：通过 `iot.observability.alerting` 在现有审计、MQTT 运行态、接入失败聚合和站内信桥接统计之上评估 4 类固定规则，并通过新场景 `observability_alert` 复用既有通知渠道。
- `2026-04-25` 起，可观测证据链第一期新增 `sys_business_event_log` 与 `sys_observability_span_log` 两张持久化证据表：`/api/*` 会沉淀接口调用片段，登录与非 GET 业务动作会沉淀业务事件；设备上行 Pipeline 在 Redis `message-flow` 之外会保留持久化调用片段摘要；调度任务运行台账与慢 SQL 也会进入统一可检索证据链。后端读侧已开放 `/api/system/observability/business-events/page`、`/api/system/observability/spans/page`、`/api/system/observability/scheduled-tasks/page`、`/api/system/observability/message-archive-batches/page`、`/api/system/observability/spans/slow-summary`、`/api/system/observability/spans/slow-trends` 与 `/api/system/observability/trace/{traceId}` 作为证据包入口和慢点/调度热点入口；其中调度台账按每次 `@Scheduled` 执行沉淀 `taskCode / triggerType / triggerExpression / duration / status / traceId`，归档批次台账分页用于检索 `iot_message_log_archive_batch` 的确认报告、归档行数、删除行数和失败原因。前端 `/system-log` 已接入 `调度任务台账`、`性能慢点 Top`、`归档批次台账`、慢点 span 明细下钻、慢点趋势下钻、归档批次详情抽屉与行级 `证据` 抽屉，支持从最近调度任务、最新慢点、归档批次或异常记录复盘同一证据链上的调用片段、批次确认结果与合并时间线。
- 同日 HTTP 业务事件字典进入 B1 轻侵入阶段：产品契约、映射规则、协议治理、设备操作和验收运行等高价值路径会优先写入稳定 `event_code`，未纳入字典的接口继续回退到通用 `module.action`。
- 同日又补齐 E1 可观测健康门禁脚本：`python3 scripts/generate-observability-health.py --hours=24 [--policy-path=config/automation/observability-health-policy.json] [--fail-on-breaches]` 会基于真实环境库输出 `logs/acceptance/observability-health-*.json/.md`，检查 Trace 覆盖率、HTTP/MESSAGE_FLOW 留痕关联率、证据包就绪率和关键标签缺失率。
- 同日继续补齐 E5/F1/F2 可观测日志治理：底层执行器仍是 `python3 scripts/govern-observability-logs.py [--policy-path=config/automation/observability-log-governance-policy.json] [--apply]`，但夜间 `dry-run` 与人工确认 `apply` 现统一收口到 `node scripts/auto/run-observability-log-governance.mjs [--mode=dry-run|apply]`。新入口默认 `dry-run` 输出 `logs/observability/observability-log-governance-*.json/.md`；`apply` 必须显式携带最近 `24` 小时内的 `dry-run` 报告和匹配的 `expiredRows` 确认值，例如 `--mode=apply --confirm-report=logs/observability/observability-log-governance-<timestamp>.json --confirm-expired-rows=<dry-run expiredRows>`。通过后，`sys_observability_span_log` 与 `sys_business_event_log` 继续按默认 `30 / 90` 天保留期分批清理，`iot_message_log` 则会先归档到 `iot_message_log_archive`、登记 `iot_message_log_archive_batch`，再删除已归档热表行；统一入口和底层报告会同步输出 `archiveBatch` 摘要，供夜间巡检与人工复核串联证据链。`sys_observability_span_log.tags_json` 与 `sys_business_event_log.metadata_json` 写侧当前也已统一扩展脱敏 `apiKey / accessKey / privateKey / deviceSecret / merchantKey / signatureSecret` 等敏感键，并限制字符串、对象项、数组项与最终 JSON 长度；`iot_message_log.payload` 继续保留主链路原始证据，不在写侧改写。
- `2026-04-26` 起，`python3 scripts/run-real-env-schema-sync.py` 会对共享环境已知的 `sys_dict.uk_dict_code_tenant` 历史索引漂移执行窄口自修复：只有当索引列顺序一致、差异仅体现在 `UNIQUE / INDEX` 唯一性时，才会先检查重复数据，再自动重建为期望索引；若存在重复行或结构差异超过该边界，脚本仍会直接停止，避免静默跳过高风险漂移。
- 同日业务事件字典进入 E2 Service 事实补证阶段：除 HTTP submit/view 事件外，正式合同发布/回滚、映射规则发布/回滚、协议族/解密档案发布/回滚、风险指标目录发布、设备命令下发完成、接入案例验收启动完成与业务验收运行启动完成，也会继续写入 `sys_business_event_log`，补齐 `releaseBatchId / approvalOrderId / ruleId / familyCode / deviceCode / jobId` 等领域上下文。
- `message-flow` 时间线当前已纳入真实环境基线：每次 HTTP / MQTT 接入都会生成 `sessionId / traceId` 与 Redis 短期时间线，`/reporting` 与 `/message-trace` 共享同一条处理阶段复盘结果。
- 数据库治理当前已拆成“双真相源”模式：结构真相固定为 `schema/**/*.json`，承载 MySQL `69` 张 active 表、`1` 张 archived 表与 TDengine `5` 个对象的结构、中文注释、生命周期、关系与 bootstrap 策略；当前不再保留 MySQL 兼容视图。`sql/init.sql`、`sql/init-tdengine.sql`、`schema/generated/mysql-schema-sync.json`、[docs/appendix/database-schema-object-catalog.generated.md](docs/appendix/database-schema-object-catalog.generated.md) 与 [docs/appendix/database-schema-lineage.generated.md](docs/appendix/database-schema-lineage.generated.md) 都由 `python scripts/schema/render_artifacts.py --write` 统一生成。对象退场、seed 包归属和真实库审计真相则固定为 `schema-governance/*.json`，当前已接入 `alarm` 域的 `risk_point_highway_detail` archived 样板，以及 `device` 域 `iot_message_log` 的 `mysql_hot_table_with_cold_archive` 治理档案，并通过 `scripts/governance/check_governance_registry.py`、`scripts/governance/run_domain_audit.py`、`scripts/governance/export_object_backup.py`、[docs/appendix/database-schema-governance-catalog.generated.md](docs/appendix/database-schema-governance-catalog.generated.md) 与 [docs/appendix/database-schema-domain-governance.generated.md](docs/appendix/database-schema-domain-governance.generated.md) 统一收口；其中域级治理台账会按域汇总结构对象、生命周期、治理对象与血缘摘要，而真实库审计事实继续原位维护在 `docs/04` / `docs/08`。运行时仍只会自动补齐 active MySQL 结构对象，以及 TDengine 的 `iot_device_telemetry_point` 与 `3` 张 raw stable；`risk_point_highway_detail` 已降级为 archived，`iot_agg_measure_hour` 继续要求脚本手动初始化。
- `/message-trace` 当前已收口为 `链路追踪 / 失败归档` 同路由双模式：链路追踪列表通过共享工具条展示最近 1h / 24h 与失败摘要，首屏固定同一行展示 `快速搜索（TraceId / 设备编码 / 产品标识） / 消息类型 / Topic / 查询 / 重置`，不再保留该模式下的“更多筛选”切换；正常链路行内只保留 `详情`，失败样本继续复用失败归档详情、`追踪 / 观测` 等异常排障动作，不再保留独立运维看板辅战区。
- `2026-04-12` 起，`接入智维` 总览已把标准排障路径显式固化为 `链路验证中心 -> 链路追踪台 / 异常观测台 / 数据校验台 -> 产品定义中心 / 设备资产中心`。完整决策树只保留在 `/device-access` 总览页；`/reporting`、`/message-trace`、`/system-log`、`/file-debug` 这些诊断子页只继续回答“当前节点 + 下一步”，不在子页重复整棵决策树。
- `/message-trace` 详情抽屉当前已切到“主链路复盘”语义：标题区仅保留轻刊头，正文固定为 `消息态势与处理概况 / 链路与接入台账 / Payload 对照 / 处理时间线` 四段，命中模板化拆分时再补轻量 `协议模板证据`。`TraceId / 日志 ID / 创建时间 / 设备编码 / 路由类型 / 产品标识 / Topic` 统一由纵向台账行承接，不再回流 `lead-sheet`、左右分栏或重复 topic 标签。`Payload 对照` 保持 `原始 Payload / 解密后明文 / 解析结果` 三条纵向折叠板块，默认收起、按需展开并提供复制动作；即使 Redis 时间线已过期或查询异常，仍继续展示从消息日志恢复出的明文与解析结果。`处理时间线` 默认折叠，但首屏必须固定回答 `当前状态 / 处理节点 / Trace 归属 / 存储提示` 四张摘要卡；展开后再进入完整 Pipeline 复盘。
- `接入智维` 页面当前统一减少重复导航层级：只保留全局壳层面包屑这一处“接入智维 / 当前模块”定位。单主列表页收口为“全局面包屑 + 主列表标题 + 共享筛选/工具条/结果区”两层语法；`/products` 主卡标题保留 `产品定义中心`，`/devices` / `/system-log` 分别保留 `设备资产中心` / `异常观测台`。`/reporting`、`/message-trace` 与 `/file-debug` 这类真实页签诊断页则保留“全局面包屑 + 真实页签 + 业务标题”，不再额外渲染页内同路径面包屑、页内标题壳或跨页功能菜单。
- `/reporting` 当前已升级为“结果复盘优先”的链路验证中心：默认进入 `结果复盘`，并把自身语义固定为排障起点；共享诊断状态头会明确回答“当前节点：链路验证”以及下一步要进入哪条诊断分支，`最近记录` 也已升级为诊断清单；HTTP 缺失、MQTT pending 超时、trace 过期和 Redis 异常四类 `message-flow` 降级态继续在同页稳定表达。
- `/in-app-message` 当前已在同页补齐“桥接效果运营”专区，可直接查看桥接成功率、待重试记录、渠道/来源分布、桥接日志与逐次尝试明细。
- 帮助中心当前按“权威资料层 + 消费层”治理：`docs/` 继续做权威源，`/api/system/help-doc/**` 做角色化消费层。

## 真实环境规则

- 唯一验收基线：`spring-boot-iot-admin/src/main/resources/application-dev.yml`
- 可通过环境变量覆盖数据库、Redis、MQTT、TDengine 和可观测配置
- `application-dev.yml` 与 `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`、`iot.telemetry.read-routing.latest-source=v2`、`iot.telemetry.read-routing.history-source=v2`；`application-test.yml` 保持 `mysql`
- 当前主库 Hikari 连接池不再依赖框架默认值；如共享环境再次出现 `HikariPool-1 - Connection is not available`，优先检查 `IOT_MYSQL_HIKARI_*` 覆盖值、慢 SQL 摘要与 `/actuator/metrics/hikaricp.connections.*`
- Redis 当前同时承担 `message-flow` 时间线短期留存和 MQTT consumer 领导权租约；若 Redis 不可用，本轮链路时间线 / MQTT 集群单消费者验收都视为环境阻塞，不回退到仅控制台日志模式
- 不允许回退到旧 H2 验收 profile、独立 H2 schema 脚本或 H2-only 验收路径
- 当真实环境访问受阻时，必须明确报告环境阻塞，不用降级链路替代验收结论

## 快速开始

### 1. 初始化数据库

```bash
mysql < sql/init.sql
mysql < sql/init-data.sql
```

TDengine 时序库可按需单独初始化：

```bash
taos -f sql/init-tdengine.sql
```

历史库升级请先阅读 [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)。

若本轮需要新增表、扩字段、补中文注释或调整生命周期，请固定执行：

```bash
python scripts/schema/render_artifacts.py --write
python scripts/schema/check_schema_registry.py
```

不要直接手改生成后的 `sql/init.sql`、`sql/init-tdengine.sql`、runtime manifest 或 `scripts/run-real-env-schema-sync.py` 顶部结构清单而不回写 registry。

若本轮涉及 archived / pending_delete 对象、seed 退场或真实库删除前置条件，请固定执行：

```bash
python scripts/governance/render_governance_docs.py --write
python scripts/governance/check_governance_registry.py
python scripts/governance/run_domain_audit.py --domain <domain>
python scripts/governance/export_object_backup.py --domain <domain> --object <objectName>
```

其中 `python scripts/governance/check_governance_registry.py` 当前还会校验治理附录是否最新；若输出 `OUT_OF_DATE docs/appendix/...`，先重新执行 `python scripts/governance/render_governance_docs.py --write`，再继续后续校验。不要只改文档或只写一次性导出脚本而绕过 `schema-governance/`。

### 2. 安装后端依赖

若仓库存在 `.mvn/settings.xml`，可继续沿用 `-s .mvn/settings.xml`；若当前工作区未提供该文件，直接执行 plain `mvn`。

```bash
mvn -s .mvn/settings.xml clean install -DskipTests
```

### 3. 启动后端

macOS / Linux、Windows CMD：

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows PowerShell：

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

若本轮需要严格验证刚修改过的上游模块是否已经进入运行时，优先改用 fat jar 启动：

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
java -jar spring-boot-iot-admin/target/spring-boot-iot-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

- `spring-boot:run` 适合日常开发；跨模块真实环境取证时，fat jar 更容易避免 `device`、`alarm`、`framework` 等依赖模块仍引用旧类。

### 4. 启动前端

```bash
cd spring-boot-iot-ui
npm install
npm run acceptance:dev
```

### 5. 执行验收

- 本地最小质量门禁脚本：

```bash
node scripts/run-quality-gates.mjs
```

  - 当前已串联 Maven 打包、前端 `build`、`component:guard`、`list:guard`、`style:guard`、schema baseline guard、`schema-governance` checker、治理契约专项门禁与 docs topology check
  - 若治理附录未按最新 registry / 渲染逻辑更新，`python scripts/governance/check_governance_registry.py` 会在该入口内直接报 `OUT_OF_DATE docs/appendix/...`
  - Windows 底层脚本：`powershell -ExecutionPolicy Bypass -File scripts/run-quality-gates.ps1`
  - macOS / Linux 底层脚本：`sh scripts/run-quality-gates.sh`
  - 若只需验证产品物模型治理主链路，可单独执行：`node scripts/run-governance-contract-gates.mjs`

- 质量工场覆盖治理矩阵：

```bash
node scripts/auto/generate-acceptance-coverage.mjs
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
node scripts/auto/diff-acceptance-coverage.mjs
node scripts/auto/diff-acceptance-coverage.mjs --baseline-path=logs/acceptance/acceptance-coverage-<old>.json --current-path=logs/acceptance/acceptance-coverage-<new>.json
node scripts/auto/run-acceptance-readiness.mjs
node scripts/auto/run-acceptance-readiness.mjs --baseline-coverage-path=logs/acceptance/acceptance-coverage-<old>.json --current-coverage-path=logs/acceptance/acceptance-coverage-<new>.json
```

  - 输出 `logs/acceptance/acceptance-coverage-<timestamp>.json` 与 `.md`
  - `--policy-path` 会把覆盖矩阵按默认 readiness policy 评估；policy error 会让命令退出 `1`，warning 只进入报告。
  - 需要把缺失场景引用、未消费场景或 P1/P2 元数据缺口纳入准入门禁时，再追加 `--fail-on-gaps`
  - 覆盖趋势对比输出 `logs/acceptance/acceptance-coverage-diff-<timestamp>.json` 与 `.md`
  - 覆盖趋势默认比较最近两份覆盖矩阵；封板或 CI readiness 复盘建议显式传入 baseline/current 路径
  - 覆盖趋势只做覆盖治理趋势分析，不替代真实环境业务验收
  - readiness 聚合输出 `logs/acceptance/acceptance-readiness-<timestamp>.json` 与 `.md`，并统一回答 `passed / warning / failed`
  - readiness 只证明自动化资产治理状态，不证明真实业务链路已经跑通；真实环境验收仍需使用 `application-dev.yml`

- 后端验收脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1
```

  - 若本机 `9999` 端口被占用，可直接改用 `powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1 -Port 10099`，或先设置 `IOT_BACKEND_ACCEPTANCE_PORT=10099` 再继续沿用原命令。
  - 脚本当前会在构建成功后把产物复制到 `logs/backend-runtime/` 下的运行副本再启动，避免上一轮验收进程锁住 `target/*.jar` 影响下一轮 `clean package`。

- message-flow 真实环境验收脚本：

```bash
python scripts/run-message-flow-acceptance.py --expired-trace-id <已过期TraceId>
```

- 可观测日志治理脚本：

```bash
node scripts/auto/run-observability-log-governance.mjs
node scripts/auto/run-observability-log-governance.mjs --mode=apply --confirm-report=logs/observability/observability-log-governance-<timestamp>.json --confirm-expired-rows=<dry-run expiredRows>
python3 scripts/govern-observability-logs.py
python3 scripts/govern-observability-logs.py --policy-path=config/automation/observability-log-governance-policy.json
python3 scripts/govern-observability-logs.py --apply --confirm-report-path=logs/observability/observability-log-governance-<timestamp>.json --confirm-report-generated-at=<dry-run generatedAt> --confirmed-expired-rows=<dry-run expiredRows>
```

  - 推荐入口是 `node scripts/auto/run-observability-log-governance.mjs`，默认读取 `config/automation/observability-log-governance-runbook.json`
  - 默认输出 `logs/observability/observability-log-governance-<timestamp>.json` 与 `.md`
  - 默认 `dry-run`，只统计和抽样，不删除真实环境数据
  - `--mode=apply` 必须带上最近 `24` 小时内的 `dry-run` 报告和匹配的 `expiredRows`
  - `iot_message_log` 在 `apply` 时会先写入 `iot_message_log_archive` 与 `iot_message_log_archive_batch`，只删除已成功归档的过期热表行
  - 统一入口与底层报告里的 `archiveBatch` 字段用于串联“确认报告 -> 归档批次 -> 删除结果”证据链
  - `python3 scripts/govern-observability-logs.py` 继续保留为底层执行器，供排障或手工核对时直接调用

- 前端验收脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1
```

- 真实环境验收手册： [docs/真实环境测试与验收手册.md](docs/真实环境测试与验收手册.md)

## 文档操作系统

### 默认最小阅读集

进入编码前，先读本文件，再补齐以下 `7` 份主文档：

1. [docs/README.md](docs/README.md)
2. [docs/01-系统概览与架构说明.md](docs/01-系统概览与架构说明.md)
3. [docs/02-业务功能与流程说明.md](docs/02-业务功能与流程说明.md)
4. [docs/03-接口规范与接口清单.md](docs/03-接口规范与接口清单.md)
5. [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)
6. [docs/07-部署运行与配置说明.md](docs/07-部署运行与配置说明.md)
7. [docs/08-变更记录与技术债清单.md](docs/08-变更记录与技术债清单.md)

### 按任务补充阅读

- 协议 / MQTT / 接入： [docs/05-protocol.md](docs/05-protocol.md)、[docs/14-MQTTX真实环境联调手册.md](docs/14-MQTTX真实环境联调手册.md)、[docs/真实环境测试与验收手册.md](docs/真实环境测试与验收手册.md)
- 前端： [docs/06-前端开发与CSS规范.md](docs/06-前端开发与CSS规范.md)、[docs/15-前端优化与治理计划.md](docs/15-前端优化与治理计划.md)
- 测试 / 验收： [docs/05-自动化测试与质量保障.md](docs/05-自动化测试与质量保障.md)、[docs/真实环境测试与验收手册.md](docs/真实环境测试与验收手册.md)、[docs/21-业务功能清单与验收标准.md](docs/21-业务功能清单与验收标准.md)
- 可观测 / 日志 / 通知： [docs/11-可观测性、日志追踪与消息通知治理.md](docs/11-可观测性、日志追踪与消息通知治理.md)
- 帮助中心 / 系统内容： [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)
- 多租户 / 数据权限： [docs/13-数据权限与多租户模型.md](docs/13-数据权限与多租户模型.md)
- Phase 边界 / 下阶段规划： [docs/19-第四阶段交付边界与复验进展.md](docs/19-第四阶段交付边界与复验进展.md)、[docs/16-阶段规划与迭代路线图.md](docs/16-阶段规划与迭代路线图.md)
- 智能助手协作： [docs/skills/ai-task-intake/SKILL.md](docs/skills/ai-task-intake/SKILL.md)、[docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md)、[docs/10-智能助手技能与任务选型指南.md](docs/10-智能助手技能与任务选型指南.md)、[docs/17-智能助手任务发起模板速查.md](docs/17-%E6%99%BA%E8%83%BD%E5%8A%A9%E6%89%8B%E4%BB%BB%E5%8A%A1%E5%8F%91%E8%B5%B7%E6%A8%A1%E6%9D%BF%E9%80%9F%E6%9F%A5.md)、[docs/template/README.md](docs/template/README.md)
- 不再作为编码主依赖：兼容入口页、`docs/archive/*`、历史台账页、`docs/template/*` 薄包装页

## 当前维护规则

1. 任何影响行为、接口、数据库、配置、流程、页面结构、帮助中心消费口径的变更，都必须同步更新权威文档。
2. 帮助中心不是第二真相源；若帮助中心与 `docs/` 冲突，以 `docs/` 与代码事实为准。
3. `docs/README.md` 只保留摘要映射和阅读导航，不再承载帮助治理细则；帮助中心治理规则统一收口到 [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)。
4. 旧入口文档和历史路线图默认进入 `docs/archive/`，不再作为主维护链路。
5. 日常向智能助手发任务时，优先使用 [docs/skills/ai-task-intake/SKILL.md](docs/skills/ai-task-intake/SKILL.md) 中“你本人专用版六条超短清单”；如果装不下，再使用 [docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md) 中的任务卡模板；只有跨模块、跨验收、跨数据库的大任务再使用 [docs/template/README.md](docs/template/README.md) 指向的长模板。
6. Git 分支治理：`master` 只作为生产分支，本地禁止直接开发、提交、合并或推送；日常编码、验证和交付准备默认只在 `codex/dev` 进行，若当前不在允许分支上，必须先停止并处理分支问题。
7. 多对话并行协作：同一仓库若同时开启多个新对话或编码助手，默认一个任务一个 `.worktrees/` 隔离 worktree，主工作区只做集成与最终验证；任何不属于本轮任务的未跟踪文件或修改都必须保留原状，不得删除、移动、格式化或顺手合并。
8. 数据库结构协作规则：新增表、扩字段、改字段注释、调整生命周期时，必须先修改 `schema/` registry，再执行 `python scripts/schema/render_artifacts.py --write`、`python scripts/schema/check_schema_registry.py`，并同步更新 [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)。

## 相关入口

- 文档总入口： [docs/README.md](docs/README.md)
- 智能助手技能选型： [docs/10-智能助手技能与任务选型指南.md](docs/10-智能助手技能与任务选型指南.md)
- 智能助手任务模板速查： [docs/17-智能助手任务发起模板速查.md](docs/17-%E6%99%BA%E8%83%BD%E5%8A%A9%E6%89%8B%E4%BB%BB%E5%8A%A1%E5%8F%91%E8%B5%B7%E6%A8%A1%E6%9D%BF%E9%80%9F%E6%9F%A5.md)
- 可观测与通知治理： [docs/11-可观测性、日志追踪与消息通知治理.md](docs/11-可观测性、日志追踪与消息通知治理.md)
- 帮助文档与系统内容治理： [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)
- 阶段规划与迭代路线： [docs/16-阶段规划与迭代路线图.md](docs/16-阶段规划与迭代路线图.md)
- 过程设计与实施计划索引： [docs/superpowers/README.md](docs/superpowers/README.md)
- 历史归档入口： [docs/archive/README.md](docs/archive/README.md)
