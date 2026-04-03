# 风险对象中心待治理绑定转正设计

> 日期：2026-04-03
> 主题：将 `risk_point_device_pending_binding` 中的待治理关系，通过“系统推荐 + 人工确认”方式转正为正式 `risk_point_device` 绑定，并继续复用 `/risk-point` 风险对象中心

## 1. 背景

当前风险对象中心已经具备以下正式能力：

- 风险点主档维护继续由 `risk_point` 承载。
- 风险点正式绑定继续由 `risk_point_device` 承载。
- 正式绑定链路已经在 `RiskPointServiceImpl#bindDevice` 中固化为“风险点 + 设备 + 测点”三元组语义，且要求 `metricIdentifier` 必填，重复绑定会被拦截。
- 前端 `/risk-point` 页面已经具备“绑定设备”抽屉，支持为风险点选择设备和测点。

同时，离线治理链路已经补齐了过渡表能力：

- 当外部 Excel 只具备“风险点名称 + 设备编号”，而真实测点尚未确认时，数据必须先落到 `risk_point_device_pending_binding`，不能向 `risk_point_device` 写占位 `metric_identifier`。
- 已存在用于 Excel 导入的脚本 `scripts/export-risk-point-device-pending-bindings.py`，可按真实环境数据库把“风险点名称 + 设备编号”匹配到 `risk_point / iot_device`。
- 共享开发环境中已经导入一批真实待治理数据，批次号为 `rpd-pending-20260403114031`，其中 `441` 条为 `PENDING_METRIC_GOVERNANCE`，`3` 条为 `DEVICE_NOT_FOUND`。

当前缺口在于：

- `risk_point_device_pending_binding` 只解决了“先留痕，不写占位测点”的问题，但还没有形成一条可持续使用的“待治理转正”链路。
- 风险对象中心当前只能做正式绑定，不能直接消费待治理记录，也缺少系统辅助推荐能力。
- 用户已经明确要求待治理转正采用“系统推荐 + 人工确认”的方式，而不是全自动批量入正式表。
- 用户同时要求一条 pending 记录可以转正成多条正式 `risk_point_device`，这意味着现有单条 `promoted_binding_id` 字段只能作为兼容快捷指针，不能承担完整的多测点转正语义。

本轮目标不是新建一个独立的风险治理系统，而是在保留 `risk_point_device` 为唯一正式运行口径的前提下，把待治理记录转成一条可追溯、可人工复核、可在真实环境中持续演进的收口链路。

## 2. 目标

- 继续把 `risk_point_device` 作为风险策略、风险监测和自动闭环的唯一正式绑定来源。
- 在 `/risk-point` 风险对象中心内补齐待治理记录查询、候选推荐、人工确认和转正提交能力。
- 支持一条 `risk_point_device_pending_binding` 记录转正为多条正式 `risk_point_device` 记录。
- 推荐来源固定为三路证据合并：
  - 产品物模型
  - 最近上报属性
  - 历史消息日志中的实际字段
- 推荐必须可解释，前端需要看到候选测点对应的证据摘要，而不是仅返回裸字段名。
- 转正提交支持部分成功、重复跳过和完整过程留痕，避免并发或重复操作制造脏数据。
- 为后续“比较/应用”式治理模型保留演进空间，但本轮不新增第二套正式绑定真相源。

## 3. 非目标

- 本轮不新增独立前端路由或新的风险治理中心页面，入口仍复用 `/risk-point`。
- 本轮不把候选测点或推荐结果批量预生成并长期固化在数据库中，候选按打开抽屉时实时生成。
- 本轮不允许系统直接自动转正到 `risk_point_device`，最终动作必须由人工确认触发。
- 本轮不尝试从风险点名称、项目名称或设备名称中做激进语义推断，不引入不透明算法模型。
- 本轮不改写 `risk_point_device` 现有正式语义，不增加占位测点、候选草稿列或“推荐状态”列到正式表。
- 本轮不处理 `DEVICE_NOT_FOUND`、`RISK_POINT_NOT_FOUND` 这类档案阻塞问题的自动补录，仅提供明确阻塞反馈。

## 4. 已确认决策

本轮设计基于以下已确认用户决策：

1. 采用“系统推荐 + 人工确认”的转正模式。
2. 推荐证据来源固定为“产品物模型 + 最近上报属性 + 历史消息日志里的实际字段”。
3. 一条 pending 记录允许转正为多条正式 `risk_point_device`。
4. 入口继续复用 `/risk-point` 风险对象中心，不新增独立路由。
5. `risk_point_device` 继续作为唯一正式运行时数据源，pending 只是治理过渡层。

## 5. 方案对比

### 5.1 方案 A：复用 `/risk-point`，实时候选推荐，人工多选转正

- 在风险对象中心中新增“待治理转正”副流程。
- 打开转正抽屉时实时生成候选测点和推荐说明。
- 人工多选确认后，一次写入一条或多条 `risk_point_device`。
- 转正过程另存过程明细，保留审计留痕。

优点：

- 最大化复用现有风险点管理、正式绑定、设备测点查询和真实环境验收链路。
- 不会形成第二个正式绑定来源。
- 推荐结果始终使用最新运行证据，避免离线快照过期。
- 最符合用户已经确认的“系统推荐 + 人工确认 + 一条 pending 转多条正式绑定”方向。

缺点：

- 需要在现有风险对象中心里扩展一条新的次级治理流程。
- 需要补一张转正过程明细表，不能只靠 pending 主表表达全部过程。

### 5.2 方案 B：新增独立风险治理页面，统一处理 pending 与正式绑定

- 新增独立路由，集中展示所有 pending 记录。
- 在新页面里完成候选推荐、转正和归档。

优点：

- 治理场景集中，列表表达完整。

缺点：

- 与用户确认的“继续复用 `/risk-point`”冲突。
- 会新增一套路由、菜单、页面骨架和权限心智，抬高实现和验收成本。
- 容易把正式绑定和过渡治理拆成两套入口。

### 5.3 方案 C：系统自动批量转正，人工只做抽查

- 系统根据证据自动选择候选测点并写入正式绑定。
- 仅保留少量异常回查入口。

优点：

- 人工操作量最小。

缺点：

- 与用户已确认的“人工最终确认”冲突。
- 在 Excel 只提供“风险点名称 + 设备编号”的前提下，自动误绑风险很高。
- 一旦误绑，会直接污染正式运行时链路。

## 6. 选型

本轮采用方案 A。

原因：

- 方案 A 与用户四项关键选择完全一致。
- 当前仓库已经存在 `/risk-point` 正式绑定抽屉和 `DeviceServiceImpl#listMetricOptions(Long deviceId)` 这类现成能力，方案 A 能最大化复用。
- 其中 `DeviceServiceImpl#listMetricOptions(Long deviceId)` 已经覆盖“产品物模型 + 最近上报属性”两路基础候选来源，本轮只需在其上补齐历史消息日志字段证据。
- pending 治理的本质是“把离线导入过来的模糊关系，治理成正式监测对象绑定”，最终仍应收口回风险对象中心，而不是分散到新的治理入口。

## 7. 总体架构

### 7.1 职责分层

- `risk_point`
  - 风险点主档唯一来源。
- `risk_point_device`
  - 风险点与设备测点正式绑定唯一来源。
  - 风险监测、阈值策略、自动闭环、对象洞察等运行期能力只消费本表。
- `risk_point_device_pending_binding`
  - 待治理导入主表。
  - 负责承接 Excel 导入证据、当前治理状态和追溯信息。
- `risk_point_device_pending_promotion`
  - 待治理转正过程明细表。
  - 负责承接“一条 pending 转正出哪些正式绑定、由谁转、为什么转、当时有哪些证据”。

### 7.2 入口与交互模型

- 风险对象中心 `/risk-point` 保持正式绑定主入口不变。
- 在现有“绑定设备”能力旁新增“待治理转正”副流程。
- 风险点列表或风险点详情抽屉中继续显示待治理积压数量，并支持进入转正抽屉。
- 转正抽屉只针对单条 pending 记录操作，避免一次处理过多对象导致上下文混乱。

### 7.3 真相源约束

- 运行期唯一正式绑定真相源仍是 `risk_point_device`。
- `risk_point_device_pending_binding` 和 `risk_point_device_pending_promotion` 只承载治理过程与追溯，不参与运行期正式判级、监测和联动。

## 8. 数据模型设计

### 8.1 `risk_point_device_pending_binding`

继续作为待治理主记录，保留现有字段与导入职责。

需要明确以下补充语义：

- `promoted_binding_id`
  - 在允许一条 pending 转多条正式绑定后，此字段只作为兼容快捷指针保留。
  - 统一定义为“最近一次由该 pending 确认关联的正式 `risk_point_device.id`”，既可以是本次新建成功的绑定，也可以是判定为 `DUPLICATE_SKIPPED` 时命中的既有正式绑定。
  - 它不是完整真相，完整转正结果必须查询转正明细表。
- `promoted_time`
  - 统一定义为“最近一次确认正式绑定关系的时间”，与 `promoted_binding_id` 对应。

### 8.2 新增表 `risk_point_device_pending_promotion`

建议新增一张待治理转正过程明细表，例如：

- `id`
- `pending_binding_id`
- `risk_point_device_id`
- `risk_point_id`
- `device_id`
- `device_code`
- `device_name`
- `metric_identifier`
- `metric_name`
- `promotion_status`
- `recommendation_level`
- `recommendation_score`
- `evidence_snapshot_json`
- `promotion_note`
- `operator_id`
- `operator_name`
- `tenant_id`
- `create_by / create_time / update_by / update_time / deleted`

索引建议：

- `idx_pending_binding_id (pending_binding_id)`
- `idx_risk_point_device_id (risk_point_device_id)`
- `idx_promotion_status (tenant_id, promotion_status, deleted)`

字段语义：

- `pending_binding_id`
  - 回指来源 pending 主记录。
- `risk_point_device_id`
  - 当本次转正成功时，记录新建正式绑定主键。
  - 若因重复跳过等原因未新建，但已经命中既有正式绑定，也应回填对应正式绑定主键，便于后续跳转与追溯。
- `promotion_status`
  - 表示本次针对某测点的处理结果，而不是 pending 主记录总状态。
- `recommendation_level / recommendation_score`
  - 记录系统当时给该候选项的推荐等级和评分，便于事后复盘。
- `evidence_snapshot_json`
  - 只保留被选中测点的证据摘要，例如来源、最近出现时间、样例值、出现频次。
  - 不保存整包候选列表，避免无意义膨胀。

约束语义：

- 该表优先作为“过程明细/审计日志表”使用，而不是单纯的结果映射表。
- 不对 `(pending_binding_id, metric_identifier)` 施加唯一约束，允许同一测点在不同治理尝试中留下多条过程记录。
- 需要在服务层通过 pending 加锁和正式绑定去重保证运行时幂等，而不是依赖明细表唯一键硬拦截。

### 8.3 pending 主状态

pending 主表继续表达“当前治理进度”，状态统一收敛为：

- `PENDING_METRIC_GOVERNANCE`
  - 已匹配到 `risk_point_id + device_id`，但尚未形成正式测点绑定。
- `PARTIALLY_PROMOTED`
  - 至少已有部分测点转正成功，但治理人尚未确认该 pending 已收口。
- `PROMOTED`
  - 该 pending 的待治理事项已经处理完毕。
- `DEVICE_NOT_FOUND`
  - 设备未建档，当前禁止转正。
- `RISK_POINT_NOT_FOUND`
  - 风险点未匹配成功，当前禁止转正。
- 组合阻塞态
  - 例如 `RISK_POINT_NOT_FOUND;DEVICE_NOT_FOUND`，继续沿用现有导入脚本的组合表达。
- `IGNORED`
  - 人工确认该 pending 关系无需转正式绑定，但保留留痕。

## 9. 推荐生成设计

### 9.1 候选来源

候选测点按设备维度实时生成，数据来源固定为三路：

1. 产品物模型
   - 提供标准化 `identifier / name / dataType`。
   - 表达“该设备理论上应该具备哪些属性点”。
2. 最近上报属性
   - 提供设备当前已经真实出现并沉淀到最新属性侧的字段。
   - 表达“该设备最近实际在上报什么”。
3. 历史消息日志字段
   - 从历史消息日志或其解析结果中归纳最近出现过的字段。
   - 用于补齐尚未进入最新属性集合、但在真实报文里已经出现过的字段。

### 9.2 候选合并规则

- 按 `metricIdentifier` 归并为唯一候选项。
- 若产品物模型和运行期证据命中同一标识，则合并为同一项，并合并证据来源。
- 候选项统一返回：
  - `metricIdentifier`
  - `metricName`
  - `dataType`
  - `evidenceSources`
  - `lastSeenTime`
  - `sampleValue`
  - `seenCount`
  - `recommendationScore`
  - `recommendationLevel`
  - `reasonSummary`

### 9.3 推荐等级

第一版只使用可解释规则，不引入黑盒算法。

推荐等级分三档：

- `HIGH`
  - 同时命中物模型和真实上报证据；或最近真实上报频次明显较高，且字段标识稳定。
- `MEDIUM`
  - 命中真实上报，但缺物模型；或命中物模型，但运行证据较弱。
- `LOW`
  - 只在历史日志里偶发出现，或证据较弱，不建议默认优先选择。

### 9.4 排序规则

推荐结果排序固定为：

1. `recommendationLevel`
2. `lastSeenTime`
3. `seenCount`
4. `metricIdentifier`

保证同一设备、同一时点下结果稳定可预期。

### 9.5 推荐边界

- 不根据风险点名称做激进自动猜测。
- 不因为某个字段名看起来像“位移/倾角/温度”就自动转正。
- 若三路证据均拿不到候选，不允许写占位测点，pending 继续停留在待治理状态。

## 10. 后端设计

### 10.1 模块边界

- `spring-boot-iot-alarm`
  - 负责 pending 查询、候选推荐编排、转正提交、状态回写和过程留痕。
- `spring-boot-iot-device`
  - 继续提供设备、产品物模型和最新属性相关查询能力。
- `spring-boot-iot-message` / `spring-boot-iot-device`
  - 提供消息日志字段提取所需的查询能力，不把风险治理逻辑反向塞进协议适配层。

### 10.2 服务职责

建议新增一组面向转正治理的服务，例如：

- `RiskPointPendingBindingService`
  - 负责 pending 主记录查询、状态更新和可治理性校验。
- `RiskPointPendingRecommendationService`
  - 负责汇总物模型、最新属性、消息日志字段并生成候选测点与推荐结果。
- `RiskPointPendingPromotionService`
  - 负责单条 pending 的转正事务、并发控制、部分成功汇总和明细写入。

### 10.3 接口设计

接口继续归属 `/api/risk-point`，建议补齐：

- `GET /api/risk-point/pending-bindings`
  - 用途：分页查询 pending 记录。
  - 过滤条件建议支持：
    - `riskPointId`
    - `deviceCode`
    - `resolutionStatus`
    - `batchNo`
    - `pageNum`
    - `pageSize`
- `GET /api/risk-point/pending-bindings/{pendingId}/candidates`
  - 用途：获取该 pending 的候选测点、推荐证据和历史转正记录。
- `POST /api/risk-point/pending-bindings/{pendingId}/promote`
  - 用途：提交一个或多个测点进行转正。
  - 请求体建议包含：
    - `metrics[]`
    - `completePending`
    - `promotionNote`
- `POST /api/risk-point/pending-bindings/{pendingId}/ignore`
  - 用途：将无需转正的 pending 记录标记为 `IGNORED`。

## 11. 转正事务设计

### 11.1 并发控制

- 转正接口按单条 pending 记录处理。
- 提交时必须对 pending 主记录做串行化控制，例如行级锁或等价方案。
- 目标是避免两名治理人员同时操作同一条 pending，导致重复转正或状态覆盖。

### 11.2 事务内处理顺序

单次转正事务建议按以下顺序执行：

1. 查询并锁定 pending 主记录。
2. 校验 pending 当前状态是否允许转正。
3. 校验 `risk_point_id`、`device_id` 是否完整。
4. 校验当前登录用户对目标风险点具备访问与写入权限。
5. 针对前端提交的每个测点，校验测点标识是否在当前候选集合中。
6. 复用正式绑定校验逻辑，检查 `risk_point_id + device_id + metric_identifier + deleted = 0` 是否已存在。
7. 对不存在的记录写入 `risk_point_device`。
8. 对每个选中测点写入一条转正过程明细。
9. 汇总本次结果，回写 pending 主记录状态、`promoted_binding_id`、`promoted_time`、`resolution_note`。

### 11.3 部分成功策略

本轮采用“部分成功而非整单回滚”的治理策略。

逐测点结果建议至少包含：

- `SUCCESS`
  - 正式绑定新建成功。
- `DUPLICATE_SKIPPED`
  - 正式绑定已存在，跳过新增。
- `INVALID_METRIC`
  - 当前提交的测点不在有效候选集合中，或提交时已失效。
- `BLOCKED`
  - 设备、风险点、权限等前置条件不满足。

返回结果需要明确到每个测点，供前端提示“哪些成功、哪些跳过、哪些失败”。

### 11.4 主状态回写规则

- 本次至少成功转正 1 条，但治理人未确认该 pending 已处理完：
  - 回写 `PARTIALLY_PROMOTED`
- 本次成功后，治理人确认该 pending 已无后续治理事项：
  - 回写 `PROMOTED`
- 本次没有新建正式绑定，但全部是 `DUPLICATE_SKIPPED`，且治理人确认已收口：
  - 允许回写 `PROMOTED`
- 人工明确判定该 pending 不应形成正式绑定：
  - 通过 ignore 接口回写 `IGNORED`

## 12. 前端设计

### 12.1 页面落点

继续复用 `/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-ui/src/views/RiskPointView.vue`。

第一版页面策略：

- 不新增独立路由。
- 不改变当前风险对象中心主列表骨架。
- 在现有“绑定设备”旁新增“待治理转正”副流程。

### 12.2 交互结构

建议在转正抽屉中固定展示三块信息：

1. pending 基础信息
   - 风险点名称/编号
   - 设备编码/名称
   - 导入批次
   - 来源文件
   - 当前治理状态
   - 导入说明与阻塞备注
2. 推荐候选区
   - 候选测点列表
   - 推荐等级
   - 来源证据标签
   - 最近出现时间
   - 样例值
   - 已有正式绑定冲突提示
3. 历史转正记录区
   - 已经成功或跳过的测点明细
   - 操作人和时间

### 12.3 交互规则

- 默认高亮 `HIGH` 候选，但不自动提交。
- 允许多选，一次转正多个测点。
- 若没有候选，抽屉明确提示“需等待设备真实上报后再治理”，不显示占位提交入口。
- 若 pending 为阻塞态，例如 `DEVICE_NOT_FOUND`，抽屉只展示阻塞原因，不提供转正提交按钮。
- 对提交结果使用明确反馈：
  - 成功条数
  - 重复跳过条数
  - 无效条数

## 13. 权限与错误处理

### 13.1 权限

- pending 查询和转正提交都必须复用风险点现有数据权限口径。
- 若当前用户无权访问该风险点，应返回明确 `BizException`，不能让 pending 成为越权绕行入口。

### 13.2 错误处理

需要明确区分以下错误：

- pending 不存在
- pending 已不处于可治理状态
- 风险点不存在或不可访问
- 设备不存在
- 当前没有可用候选测点
- 提交测点已重复绑定
- 并发提交导致状态已变更

接口必须返回明确业务错误，而不是通用 `500`。

## 14. 验证与验收

### 14.1 后端单测

至少覆盖：

- 候选合并与排序
- 推荐等级判定
- 一条 pending 转多条正式绑定
- 重复绑定跳过
- `PARTIALLY_PROMOTED` 与 `PROMOTED` 状态切换
- `IGNORED` 归档
- 无权限访问拦截
- 并发或重复提交幂等保护

### 14.2 前端测试

至少覆盖：

- 待治理抽屉打开与候选加载
- 多选提交与结果提示
- 空候选提示
- 阻塞态只读展示
- 历史转正明细回显

### 14.3 真实环境验收

严格使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 或覆盖该文件的环境变量。

真实环境回归建议使用共享批次 `rpd-pending-20260403114031` 中的可治理记录，完成以下闭环：

1. 查询某条 `PENDING_METRIC_GOVERNANCE` pending。
2. 打开候选接口，确认可返回物模型、最新属性、消息日志字段合并结果。
3. 手工勾选一个或多个候选测点并提交转正。
4. 验证正式 `risk_point_device` 出现新增记录。
5. 验证 pending 主状态与转正明细同步落库。
6. 验证风险对象中心正式绑定列表可见新增结果。
7. 验证风险监测/对象洞察等运行期读侧可继续消费正式绑定，而不是读取 pending 表。

## 15. 文档影响面

实现本设计后，至少需要原位更新：

- `docs/02-业务功能与流程说明.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/21-业务功能清单与验收标准.md`
- 如新增表结构或真实环境同步脚本，还需同步 `docs/07-部署运行与配置说明.md`
- 如本轮沉淀新的治理断点或遗留边界，还需补充 `docs/08-变更记录与技术债清单.md`

本 spec 只定义设计，不等于实现已完成。

## 16. 成功标准

当以下条件同时满足时，视为本轮设计目标落地成功：

- `risk_point_device` 仍是唯一正式绑定来源。
- 一条 pending 可稳定转正为多条正式绑定。
- 推荐结果可解释，治理人员能看到系统依据。
- 人工最终确认后才会写入正式表。
- 重复提交不会制造正式重复数据。
- 能完整追溯“系统推荐了什么、人工选择了什么、最终写入了什么”。
- 正式链路不再接受占位 `metric_identifier` 回灌。
