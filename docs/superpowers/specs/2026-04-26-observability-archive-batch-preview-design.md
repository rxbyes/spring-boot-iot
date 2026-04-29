# G2 归档批次筛选与确认报告预览设计

## 1. 背景

`/system-log` 已经具备 `归档批次台账` 卡片和同页详情抽屉，后端也已经开放 `GET /api/system/observability/message-archive-batches/page`。当前真实问题不在“有没有批次真相”，而在“运维能不能直接用它”：

1. 前端现在固定只取最近 `5` 条 `iot_message_log_archive_batch`，没有把后端已支持的 `batchNo / status / dateFrom / dateTo` 过滤条件暴露出来。
2. 详情抽屉只能看到 `confirmReportPath` 文本，无法直接预览确认报告内容，运维仍需要跳到文件系统手工打开 `logs/observability/observability-log-governance-*.json/.md`。
3. 现有证据链已经覆盖批次真相、归档统计和删除统计，但最后一跳“报告核对”仍然脱离工作台，不利于夜间巡检和日常排障。

G2 的目标不是再创建一个新的治理工作台，而是把现有 `/system-log` 上的批次台账补成一个真正可操作、可核账的运营入口。

## 2. 目标与非目标

### 2.1 目标

1. 在 `/system-log` 中为 `归档批次台账` 提供可用的 `批次号 / 状态 / 时间范围` 筛选。
2. 新增只读确认报告预览能力，让运维在详情抽屉中直接核对报告摘要。
3. 保持 `iot_message_log_archive_batch` 作为唯一批次真相，不新增第二套批次存储。
4. 明确文件读取安全边界，只允许预览仓库 `logs/observability/` 下的治理报告。

### 2.2 非目标

1. 本阶段不新增独立治理路由，不创建新的“日志治理中心”页面。
2. 本阶段不新增报告下载、文件编辑、重跑治理或 apply 触发能力。
3. 本阶段不改变 `iot_message_log_archive_batch`、`iot_message_log_archive` 或治理脚本的数据结构。
4. 本阶段不把归档数据接回 `/message-trace`、`/automation-governance` 或其他读链路。

## 3. 方案对比与推荐

### 3.1 方案 A：仅补前端筛选

做法：

1. 只在 `/system-log` 上补 `batchNo / status / dateRange` 筛选。
2. 继续复用现有 `message-archive-batches/page` 接口。
3. 详情抽屉仍只展示 `confirmReportPath` 文本。

优点：

- 改动最小。
- 风险最低。

缺点：

- 运维仍然需要离开工作台手工打开报告。
- “批次真相已经有了，但最后一跳还要靠文件系统”这个体验问题没有解决。

### 3.2 方案 B：前端筛选 + 后端确认报告预览，只读增强

做法：

1. 保留现有分页接口，补前端筛选能力。
2. 后端新增一个只读预览接口，按 `batchNo` 查询批次，再从批次真相表里解析 `confirmReportPath`。
3. 详情抽屉新增 `确认报告预览` 区块，展示 JSON 摘要和 Markdown 预览片段。

优点：

- 不改变批次真相，不引入第二套存储。
- 运维可以在 `/system-log` 内完成“筛选 -> 查看批次 -> 核对确认报告”的闭环。
- 安全边界可控，因为服务端只按批次号查表、只读仓库 `logs/observability/` 白名单目录。

缺点：

- 需要在后端引入受控的本地文件读取逻辑。

### 3.3 方案 C：新建独立治理页

做法：

1. 把批次台账从 `/system-log` 拆到独立页面。
2. 在新页面上做完整筛选、分页、预览和后续治理动作。

优点：

- 后续扩展空间最大。

缺点：

- 明显超出 G2 的“增强现有运营入口”边界。
- 会把现在的工作台收口再拆开，增加页面复杂度。

### 3.4 推荐

采用 **方案 B**。

理由：

1. 与现有 `system-log -> 归档批次台账 -> 详情抽屉` 的使用路径最一致。
2. 只增强已存在的证据链，不发明新的治理真相或页面层级。
3. 能在不扩大范围的前提下，把“确认报告路径文本”升级成“可直接核对的报告摘要”。

## 4. 用户体验设计

### 4.1 `/system-log` 中的批次台账筛选

`归档批次台账` 卡片保留在系统模式首页，不新增跳转。

新增一组卡片内轻筛选控件：

1. `批次号`：文本输入，精确匹配 `batchNo`。
2. `状态`：下拉，至少支持 `RUNNING / SUCCEEDED / FAILED`，保留“全部”。
3. `时间范围`：基于 `createTime` 的日期时间范围。
4. `查询 / 重置`：只影响归档批次台账，不干扰审计日志主列表筛选。

展示策略：

1. 继续只展示最近 `5` 条命中记录，保持首页块状信息密度不失控。
2. 卡片头部继续显示 `当前展示条数 / 总命中数`，明确这是命中结果中的最近 `5` 条，而不是全部结果。
3. 未命中时明确显示 `暂无归档批次记录`，不与接口异常混淆。

### 4.2 详情抽屉中的报告预览

现有 `归档批次详情` 抽屉保留三个部分：

1. 批次摘要卡片。
2. 批次结果键值区。
3. 附加产物键值区。

在此基础上新增第四部分：`确认报告预览`。

预览区块展示：

1. `报告可用状态`：可用 / 未绑定报告 / 报告不存在 / 路径不合法 / 报告解析失败。
2. `文件元数据`：解析后的 JSON 路径、Markdown 路径、文件最后修改时间、是否截断。
3. `JSON 摘要卡片`：`generatedAt / mode / expiredRows / deletedRows / tablesWithExpiredRows`。
4. `分表摘要`：优先展示 `iot_message_log` 的 `retentionDays / cutoffAt / totalRows / expiredRows / deletedRows / remainingExpiredRows`，其余表按简表列出。
5. `Markdown 预览片段`：展示确认报告 Markdown 的前置片段，用于快速核对报告文本口径。

当报告不可用时：

1. 抽屉仍展示批次真相。
2. 报告预览区块显示受控空态或失败原因，不把整个抽屉打成错误状态。

## 5. 后端设计

### 5.1 保留现有分页接口

现有接口保持不变：

- `GET /api/system/observability/message-archive-batches/page`

当前接口已经支持：

- `batchNo`
- `sourceTable`
- `status`
- `dateFrom`
- `dateTo`
- `pageNum`
- `pageSize`

G2 不修改其参数和返回契约，只让前端真正消费这些过滤能力。

### 5.2 新增确认报告预览接口

新增接口：

- `GET /api/system/observability/message-archive-batches/report-preview?batchNo=<batchNo>`

接口语义：

1. 以 `batchNo` 作为唯一查询入口。
2. 服务端先从 `iot_message_log_archive_batch` 查询该批次真相。
3. 再从该批次的 `confirm_report_path` 解析、规范化并读取报告文件。

不接受客户端直接传文件路径，避免把文件系统路径当成开放式读取参数。

### 5.3 预览返回模型

建议新增 `ObservabilityMessageArchiveBatchReportPreviewVO`，字段至少包括：

1. `batchNo`
2. `sourceTable`
3. `status`
4. `confirmReportPath`
5. `confirmReportGeneratedAt`
6. `available`
7. `reasonCode`
8. `reasonMessage`
9. `resolvedJsonPath`
10. `resolvedMarkdownPath`
11. `fileLastModifiedAt`
12. `markdownAvailable`
13. `markdownTruncated`
14. `markdownPreview`
15. `summary`
16. `tableSummaries`

其中：

- `summary` 对应报告 JSON 顶层 `summary`。
- `tableSummaries` 对应报告 JSON 顶层 `tables` 的受控摘要列表。
- `available=true` 的判定以 JSON 报告可读、可解析为准。
- `markdownPreview` 只返回截断后的预览文本，不返回整份文件全文。
- `markdownAvailable=false` 时，仍允许返回 JSON 摘要；前端只把 Markdown 区块渲染为空态，不把整份报告视为不可用。

### 5.4 文件读取与安全边界

允许读取的根目录固定为：

- `<repo>/logs/observability`

服务端处理规则：

1. 将批次表中的 `confirm_report_path` 解析为仓库相对路径或绝对路径。
2. 统一规范化到 `Path.normalize()` 后，再校验最终路径必须位于 `logs/observability` 根目录内。
3. 若路径越界、为空、或文件不存在，则不抛出系统异常到前端，而是返回 `available=false` 与明确 `reasonCode`。
4. 只读取 `.json` 与同 stem 的 `.md` 配对文件，不允许任意扩展名读取。

建议 `reasonCode` 至少覆盖：

1. `MISSING_REPORT_PATH`
2. `REPORT_PATH_REJECTED`
3. `REPORT_JSON_NOT_FOUND`
4. `REPORT_MARKDOWN_NOT_FOUND`
5. `REPORT_PARSE_FAILED`

### 5.5 报告解析策略

对于 JSON 报告：

1. 解析顶层 `generatedAt / mode / summary / tables`。
2. 将 `tables` 中每张表转换为统一 VO：
   - `tableName`
   - `label`
   - `retentionDays`
   - `timeField`
   - `cutoffAt`
   - `totalRows`
   - `expiredRows`
   - `deletedRows`
   - `remainingExpiredRows`
   - `earliestRecordAt`
   - `latestRecordAt`

对于 Markdown 报告：

1. Markdown 属于可选增强，不作为整个预览是否可用的前置条件。
2. 存在时返回前 `80` 行以内且总长度不超过 `6000` 字符的预览片段。
3. 超限时标记 `markdownTruncated=true`。
4. 缺失时返回 `markdownAvailable=false` 与空预览文本，同时保留 JSON 摘要。
5. 不返回整份 Markdown 原文，避免把工作台变成文件浏览器。

## 6. 前端设计

### 6.1 API 层

在 `spring-boot-iot-ui/src/api/observability.ts` 中新增：

1. `ObservabilityMessageArchiveBatchReportPreview`
2. `getObservabilityMessageArchiveBatchReportPreview(batchNo: string)`

原有 `pageObservabilityMessageArchiveBatches()` 保持不变。

### 6.2 `AuditLogView.vue`

改动点：

1. 为 `messageArchiveBatch` 新增局部筛选状态，不复用审计日志主查询表单，避免互相污染。
2. 卡片顶部新增轻筛选区和局部 `查询 / 重置` 动作。
3. 点击 `详情` 打开抽屉时，同时触发报告预览请求。
4. 切换到其他批次时重新拉取该批次预览。
5. 关闭抽屉时重置预览状态。

状态拆分：

1. `messageArchiveBatchLoading`：列表加载。
2. `messageArchiveBatchReportPreviewLoading`：抽屉内报告预览加载。
3. `messageArchiveBatchReportPreviewError`：仅用于接口级错误。
4. `messageArchiveBatchReportPreviewData`：预览实体。

### 6.3 抽屉展示策略

抽屉中的新预览区块应遵循：

1. 报告可用时优先展示摘要卡片，再展示 Markdown 预览。
2. 报告不可用时显示受控空态，不遮挡批次结果区域。
3. 报告接口失败时显示轻量错误文案，但不清空当前批次上下文。

G2 不新增下载按钮，也不把 `confirmReportPath` 做成文件系统跳转。

## 7. 测试设计

### 7.1 后端

至少补齐以下测试：

1. `ObservabilityEvidenceControllerTest`
   - `report-preview` 参数委派正确
2. `ObservabilityEvidenceQueryServiceImplTest`
   - 按 `batchNo` 查询批次并构造预览
   - 缺失 `confirmReportPath` 时返回 `available=false`
   - 路径越界时返回 `REPORT_PATH_REJECTED`
   - JSON 解析成功时能提取 `summary / tableSummaries`
   - Markdown 过长时会截断并标记 `markdownTruncated=true`

### 7.2 前端

至少补齐以下测试：

1. `src/__tests__/api/observability.test.ts`
   - `report-preview` query builder 正确
2. `src/__tests__/views/AuditLogView.test.ts`
   - 批次筛选条件会形成正确请求
   - `重置` 会恢复默认筛选
   - 打开批次详情时会请求报告预览
   - 报告可用/不可用/失败三种状态渲染正确

## 8. 文档更新范围

实现完成后需要同步更新：

1. `docs/03-接口规范与接口清单.md`
2. `docs/11-可观测性、日志追踪与消息通知治理.md`
3. `docs/08-变更记录与技术债清单.md`
4. `README.md`
5. `AGENTS.md`

其中：

- `docs/03` 记录新接口与请求参数。
- `docs/11` 记录 `/system-log` 的新筛选和报告预览能力。
- `docs/08` 记录 G2 阶段事实。
- `README.md / AGENTS.md` 只补充到会影响后续协作与验收口径的层面，不重复展开实现细节。

## 9. 验证口径

实现阶段完成后，至少执行：

1. `mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
2. `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts`

如共享环境允许继续验证，再补：

1. 启动前端/后端后在 `/system-log` 手工确认筛选和报告预览链路可用。
2. 使用真实存在的批次 `iot_message_log-20260426000119` 验证预览摘要与 `logs/observability` 中的报告内容一致。

## 10. 风险与约束

1. 后端读取本地报告文件这件事本身有边界风险，因此 G2 必须固定为“按 `batchNo` 查表 -> 白名单目录内只读预览”，不能接受任意路径。
2. 当前批次真相表不含 `tenant_id`，G2 继续沿用现有登录态校验，不新增租户级裁剪。
3. `/system-log` 首页信息密度已经较高，因此筛选区必须保持轻量，不扩成第二套完整分页工作台。
4. 若历史批次没有 `confirm_report_path`，G2 也必须可正常展示批次详情，只是报告预览不可用。
