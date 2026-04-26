# G4 归档批次异常总览 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/system-log` 的归档批次台账中补齐异常摘要卡、`compareStatus` 筛选和异常行高亮，让运维先在列表层看见异常，再决定是否进入 G3 抽屉深挖证据。

**Architecture:** 后端扩展 `message-archive-batches/page`，为每行补轻量 compare 投影，并新增 `message-archive-batches/overview` 聚合最近时间窗口的异常批次摘要。前端继续留在 `/system-log` 同一块台账区域，顶部消费 `overview` 摘要卡，列表消费增强后的 `page` 接口，详情抽屉继续沿用 G3 的 `批次对比 + 确认报告预览`。

**Tech Stack:** Spring Boot 4、Java 17、JdbcTemplate、Vue 3、TypeScript、Vitest、JUnit 5、Maven

---

### Task 1: 后端查询模型与 VO 扩展

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/ObservabilityMessageArchiveBatchOverviewQuery.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchOverviewVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/ObservabilityMessageArchiveBatchPageQuery.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java`

- [ ] **Step 1: 先在 controller 测试里写 overview 失败用例**

```java
@Test
void getMessageArchiveBatchOverviewShouldDelegateQuery() {
    ObservabilityMessageArchiveBatchOverviewQuery query = new ObservabilityMessageArchiveBatchOverviewQuery();
    query.setSourceTable("iot_message_log");
    query.setDateFrom("2026-04-26 00:00:00");
    query.setDateTo("2026-04-26 23:59:59");

    ObservabilityMessageArchiveBatchOverviewVO overview = new ObservabilityMessageArchiveBatchOverviewVO();
    overview.setAbnormalBatches(2L);
    overview.setTotalRemainingExpiredRows(118L);

    when(observabilityEvidenceQueryService.getMessageArchiveBatchOverview(query, 10001L))
            .thenReturn(overview);

    R<ObservabilityMessageArchiveBatchOverviewVO> response =
            controller.getMessageArchiveBatchOverview(query, authentication(10001L));

    assertThat(response.getData().getAbnormalBatches()).isEqualTo(2L);
    verify(observabilityEvidenceQueryService).getMessageArchiveBatchOverview(query, 10001L);
}
```

- [ ] **Step 2: 跑 controller 定向测试，确认 overview 路由和 service 签名尚不存在**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `ObservabilityEvidenceControllerTest` 因 `getMessageArchiveBatchOverview(...)` 缺失而失败。

- [ ] **Step 3: 给 page query 增加 G4 查询参数**

```java
@Data
public class ObservabilityMessageArchiveBatchPageQuery {

    private String batchNo;
    private String sourceTable;
    private String status;
    private String compareStatus;
    private Boolean onlyAbnormal;
    private String dateFrom;
    private String dateTo;
    private Long pageNum;
    private Long pageSize;
}
```

- [ ] **Step 4: 新建 overview query / VO，并扩展批次 VO 的轻量投影字段**

```java
@Data
public class ObservabilityMessageArchiveBatchOverviewQuery {
    private String sourceTable;
    private String dateFrom;
    private String dateTo;
}
```

```java
@Data
public class ObservabilityMessageArchiveBatchOverviewVO {
    private Long totalBatches;
    private Long matchedBatches;
    private Long driftedBatches;
    private Long partialBatches;
    private Long unavailableBatches;
    private Long abnormalBatches;
    private Long totalDeltaConfirmedVsDeleted;
    private Long totalRemainingExpiredRows;
    private String latestAbnormalBatch;
    private LocalDateTime latestAbnormalOccurredAt;
}
```

```java
@Data
public class ObservabilityMessageArchiveBatchVO {
    private Long id;
    private String batchNo;
    private String sourceTable;
    private String governanceMode;
    private String status;
    private Integer retentionDays;
    private LocalDateTime cutoffAt;
    private String confirmReportPath;
    private LocalDateTime confirmReportGeneratedAt;
    private Integer confirmedExpiredRows;
    private Integer candidateRows;
    private Integer archivedRows;
    private Integer deletedRows;
    private String failedReason;
    private String artifactsJson;
    private String compareStatus;
    private String compareStatusLabel;
    private Long deltaConfirmedVsDeleted;
    private Long deltaDryRunVsDeleted;
    private Long remainingExpiredRows;
    private Boolean previewAvailable;
    private String previewReasonCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 5: 给 service 接口和 controller 增加 overview 入口**

```java
public interface ObservabilityEvidenceQueryService {

    PageResult<ObservabilityMessageArchiveBatchVO> pageMessageArchiveBatches(
            ObservabilityMessageArchiveBatchPageQuery query,
            Long currentUserId
    );

    ObservabilityMessageArchiveBatchOverviewVO getMessageArchiveBatchOverview(
            ObservabilityMessageArchiveBatchOverviewQuery query,
            Long currentUserId
    );
}
```

```java
@GetMapping("/message-archive-batches/overview")
public R<ObservabilityMessageArchiveBatchOverviewVO> getMessageArchiveBatchOverview(
        ObservabilityMessageArchiveBatchOverviewQuery query,
        Authentication authentication
) {
    return R.ok(observabilityEvidenceQueryService.getMessageArchiveBatchOverview(
            query,
            requireCurrentUserId(authentication)
    ));
}
```

- [ ] **Step 6: 复跑 controller 定向测试，确保新路由和签名通过**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `ObservabilityEvidenceControllerTest` 通过或只剩 service impl 未实现导致的相关失败。

- [ ] **Step 7: Commit**

```bash
git add \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/ObservabilityMessageArchiveBatchOverviewQuery.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchOverviewVO.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/ObservabilityMessageArchiveBatchPageQuery.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchVO.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java \
  spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java
git commit -m "feat: add archive batch overview contract"
```

### Task 2: 后端列表投影与 overview 聚合实现

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java`

- [ ] **Step 1: 先写 page 筛选和 overview 聚合失败用例**

```java
@Test
void pageMessageArchiveBatchesShouldApplyCompareStatusAndOnlyAbnormalFilters() {
    ObservabilityMessageArchiveBatchPageQuery query = new ObservabilityMessageArchiveBatchPageQuery();
    query.setSourceTable("iot_message_log");
    query.setCompareStatus("MATCHED");
    query.setOnlyAbnormal(true);
    query.setPageNum(1L);
    query.setPageSize(20L);

    ObservabilityMessageArchiveBatchVO row = new ObservabilityMessageArchiveBatchVO();
    row.setBatchNo("iot_message_log-20260426000119");
    row.setCompareStatus("DRIFTED");
    row.setRemainingExpiredRows(12L);

    when(jdbcTemplate.queryForObject(contains("FROM iot_message_log_archive_batch"), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);
    doReturn(List.of(row)).when(jdbcTemplate).query(
            contains("ORDER BY create_time DESC"),
            any(RowMapper.class),
            any(Object[].class)
    );

    PageResult<ObservabilityMessageArchiveBatchVO> result = service.pageMessageArchiveBatches(query, 10001L);

    assertThat(result.getRecords()).hasSize(1);
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
    assertThat(sqlCaptor.getValue()).contains("compareStatus");
}
```

```java
@Test
void getMessageArchiveBatchOverviewShouldAggregateAbnormalBuckets() {
    mockOverviewRows(
            overviewRow("iot_message_log-1", "MATCHED", 0L, 0L, "2026-04-26 00:10:00"),
            overviewRow("iot_message_log-2", "DRIFTED", -18L, 18L, "2026-04-26 00:20:00"),
            overviewRow("iot_message_log-3", "PARTIAL", 0L, null, "2026-04-26 00:30:00"),
            overviewRow("iot_message_log-4", "UNAVAILABLE", null, null, "2026-04-26 00:40:00")
    );

    ObservabilityMessageArchiveBatchOverviewVO overview =
            service.getMessageArchiveBatchOverview(new ObservabilityMessageArchiveBatchOverviewQuery(), 10001L);

    assertThat(overview.getTotalBatches()).isEqualTo(4L);
    assertThat(overview.getAbnormalBatches()).isEqualTo(3L);
    assertThat(overview.getDriftedBatches()).isEqualTo(1L);
    assertThat(overview.getPartialBatches()).isEqualTo(1L);
    assertThat(overview.getUnavailableBatches()).isEqualTo(1L);
    assertThat(overview.getLatestAbnormalBatch()).isEqualTo("iot_message_log-4");
}
```

- [ ] **Step 2: 跑 service 定向测试，确认 G4 行为尚未实现**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `ObservabilityEvidenceQueryServiceImplTest` 因 page 筛选或 overview 缺失失败。

- [ ] **Step 3: 在 service impl 中补 page 轻量投影计算**

实现要点：

```java
@Override
public PageResult<ObservabilityMessageArchiveBatchVO> pageMessageArchiveBatches(
        ObservabilityMessageArchiveBatchPageQuery query,
        Long currentUserId
) {
    ObservabilityMessageArchiveBatchPageQuery criteria =
            query == null ? new ObservabilityMessageArchiveBatchPageQuery() : query;
    long pageNum = PageQueryUtils.normalizePageNum(criteria.getPageNum());
    long pageSize = PageQueryUtils.normalizePageSize(criteria.getPageSize());
    List<Object> args = new ArrayList<>();
    String where = buildMessageArchiveBatchWhere(criteria, args);
    Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM iot_message_log_archive_batch WHERE " + where,
            Long.class,
            args.toArray()
    );
    if (total == null || total == 0L) {
        return PageResult.empty(pageNum, pageSize);
    }
    List<ObservabilityMessageArchiveBatchVO> records = jdbcTemplate.query(...);
    records.forEach(this::decorateMessageArchiveBatchProjection);
    return PageResult.of(total, pageNum, pageSize, records);
}

private void decorateMessageArchiveBatchProjection(ObservabilityMessageArchiveBatchVO row) {
    ObservabilityMessageArchiveBatchCompareVO compare = buildMessageArchiveBatchCompare(row);
    row.setCompareStatus(compare.getCompareStatus());
    row.setCompareStatusLabel(resolveArchiveCompareStatusLabel(compare.getCompareStatus()));
    if (compare.getSummaryCompare() != null) {
        row.setDeltaConfirmedVsDeleted(compare.getSummaryCompare().getDeltaConfirmedVsDeleted());
        row.setDeltaDryRunVsDeleted(compare.getSummaryCompare().getDeltaDryRunVsDeleted());
        row.setRemainingExpiredRows(compare.getSummaryCompare().getRemainingExpiredRows());
    }
    if (compare.getSources() != null) {
        row.setPreviewAvailable(Boolean.TRUE.equals(compare.getSources().getDryRunAvailable()));
        if (!Boolean.TRUE.equals(compare.getSources().getDryRunAvailable())) {
            row.setPreviewReasonCode("REPORT_UNAVAILABLE");
        }
    }
}
```

- [ ] **Step 4: 在 buildMessageArchiveBatchWhere 中补 compare 维度筛选**

```java
private String buildMessageArchiveBatchWhere(ObservabilityMessageArchiveBatchPageQuery query, List<Object> args) {
    List<String> clauses = new ArrayList<>();
    clauses.add("1 = 1");
    appendEquals(clauses, args, "batch_no", query.getBatchNo());
    appendEquals(clauses, args, "source_table", query.getSourceTable());
    appendEquals(clauses, args, "status", query.getStatus());
    appendDateRange(clauses, args, "create_time", query.getDateFrom(), query.getDateTo());

    if (Boolean.TRUE.equals(query.getOnlyAbnormal())) {
        clauses.add("""
                COALESCE(
                    JSON_UNQUOTE(JSON_EXTRACT(artifacts_json, '$.compareStatus')),
                    status
                ) IN ('DRIFTED', 'PARTIAL', 'UNAVAILABLE')
                """);
    } else if (StringUtils.hasText(normalize(query.getCompareStatus()))) {
        clauses.add("""
                COALESCE(
                    JSON_UNQUOTE(JSON_EXTRACT(artifacts_json, '$.compareStatus')),
                    status
                ) = ?
                """);
        args.add(normalize(query.getCompareStatus()).toUpperCase());
    }
    return String.join(" AND ", clauses);
}
```

如果实现时决定不直接在 SQL 中用 `artifacts_json`，则改为“先查时间窗口内批次，再在 Java 中按投影结果过滤”，但必须保持：

1. `onlyAbnormal` 优先于 `compareStatus`
2. `DRIFTED + PARTIAL + UNAVAILABLE` 的语义不变

- [ ] **Step 5: 实现 overview 聚合**

```java
@Override
public ObservabilityMessageArchiveBatchOverviewVO getMessageArchiveBatchOverview(
        ObservabilityMessageArchiveBatchOverviewQuery query,
        Long currentUserId
) {
    ObservabilityMessageArchiveBatchOverviewQuery criteria =
            query == null ? new ObservabilityMessageArchiveBatchOverviewQuery() : query;
    List<Object> args = new ArrayList<>();
    String where = buildMessageArchiveBatchOverviewWhere(criteria, args);
    List<ObservabilityMessageArchiveBatchVO> rows = jdbcTemplate.query(
            """
            SELECT id, batch_no, source_table, governance_mode, status, retention_days,
                   cutoff_at, confirm_report_path, confirm_report_generated_at,
                   confirmed_expired_rows, candidate_rows, archived_rows, deleted_rows,
                   failed_reason, artifacts_json, create_time, update_time
            FROM iot_message_log_archive_batch
            WHERE %s
            ORDER BY create_time DESC, id DESC
            """.formatted(where),
            this::mapMessageArchiveBatch,
            args.toArray()
    );
    rows.forEach(this::decorateMessageArchiveBatchProjection);
    return aggregateMessageArchiveBatchOverview(rows);
}

private ObservabilityMessageArchiveBatchOverviewVO aggregateMessageArchiveBatchOverview(
        List<ObservabilityMessageArchiveBatchVO> rows
) {
    ObservabilityMessageArchiveBatchOverviewVO overview = new ObservabilityMessageArchiveBatchOverviewVO();
    overview.setTotalBatches((long) rows.size());
    // 逐条累计 matched/drifted/partial/unavailable
    // 累计 deltaConfirmedVsDeleted / remainingExpiredRows
    // 找最近异常批次
    return overview;
}
```

- [ ] **Step 6: 复跑 service 定向测试，确认 G4 读侧通过**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `ObservabilityEvidenceQueryServiceImplTest` 通过。

- [ ] **Step 7: 再跑 controller + service 合并回归**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`，相关 `23+` 用例全部通过。

- [ ] **Step 8: Commit**

```bash
git add \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java \
  spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java
git commit -m "feat: add archive batch anomaly overview backend"
```

### Task 3: 前端 API 与 `/system-log` 台账异常总览

**Files:**
- Modify: `spring-boot-iot-ui/src/api/observability.ts`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 先写 API client 与视图失败用例**

```ts
it('loads message archive batch overview by query', async () => {
  vi.mocked(request).mockResolvedValueOnce({
    code: 200,
    data: {
      abnormalBatches: 3,
      totalRemainingExpiredRows: 18,
      latestAbnormalBatch: 'iot_message_log-20260426000119'
    }
  })

  await getObservabilityMessageArchiveBatchOverview({
    sourceTable: 'iot_message_log',
    dateFrom: '2026-04-26 00:00:00',
    dateTo: '2026-04-26 23:59:59'
  })

  expect(request).toHaveBeenCalledWith(
    expect.stringContaining('/api/system/observability/message-archive-batches/overview?')
  )
})
```

```ts
it('renders archive batch anomaly overview cards and filters compare status', async () => {
  mockPageObservabilityMessageArchiveBatches({
    records: [
      { batchNo: 'batch-1', compareStatus: 'DRIFTED', deltaConfirmedVsDeleted: -12, remainingExpiredRows: 12 }
    ],
    total: 1
  })
  mockGetObservabilityMessageArchiveBatchOverview({
    abnormalBatches: 1,
    totalDeltaConfirmedVsDeleted: -12,
    totalRemainingExpiredRows: 12,
    latestAbnormalBatch: 'batch-1'
  })

  renderAuditLogViewSystemMode()

  expect(await screen.findByText('异常批次数')).toBeInTheDocument()
  expect(screen.getByText('batch-1')).toBeInTheDocument()
})
```

- [ ] **Step 2: 跑前端定向测试，确认新 client / UI 尚未实现**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: `observability.test.ts` 或 `AuditLogView.test.ts` 因 `overview` client、摘要卡或筛选缺失失败。

- [ ] **Step 3: 扩展前端 API 类型与 client**

```ts
export interface ObservabilityMessageArchiveBatchOverview {
  totalBatches?: number | null
  matchedBatches?: number | null
  driftedBatches?: number | null
  partialBatches?: number | null
  unavailableBatches?: number | null
  abnormalBatches?: number | null
  totalDeltaConfirmedVsDeleted?: number | null
  totalRemainingExpiredRows?: number | null
  latestAbnormalBatch?: string | null
  latestAbnormalOccurredAt?: string | null
}

export interface ObservabilityMessageArchiveBatchOverviewQuery {
  sourceTable?: string
  dateFrom?: string
  dateTo?: string
}

export const getObservabilityMessageArchiveBatchOverview = (
  query: ObservabilityMessageArchiveBatchOverviewQuery
) => request<ApiEnvelope<ObservabilityMessageArchiveBatchOverview>>(
  `/api/system/observability/message-archive-batches/overview${buildQueryString(query)}`
)
```

同时扩展：

```ts
export interface ObservabilityMessageArchiveBatch {
  ...
  compareStatus?: 'MATCHED' | 'DRIFTED' | 'PARTIAL' | 'UNAVAILABLE' | string | null
  compareStatusLabel?: string | null
  deltaConfirmedVsDeleted?: number | null
  deltaDryRunVsDeleted?: number | null
  remainingExpiredRows?: number | null
  previewAvailable?: boolean | null
  previewReasonCode?: string | null
}

export interface ObservabilityMessageArchiveBatchPageQuery {
  ...
  compareStatus?: string
  onlyAbnormal?: boolean
}
```

- [ ] **Step 4: 在 `AuditLogView.vue` 新增摘要状态与筛选状态**

```ts
const messageArchiveBatchOverview = ref<ObservabilityMessageArchiveBatchOverview | null>(null)
const messageArchiveBatchOverviewLoading = ref(false)
const messageArchiveBatchOverviewErrorMessage = ref('')

const messageArchiveBatchFilters = reactive({
  batchNo: '',
  status: '',
  compareStatus: '',
  onlyAbnormal: false,
  dateFrom: '',
  dateTo: ''
})
```

```ts
const buildMessageArchiveBatchQueryParams = (): ObservabilityMessageArchiveBatchPageQuery => ({
  sourceTable: 'iot_message_log',
  batchNo: messageArchiveBatchFilters.batchNo.trim() || undefined,
  status: messageArchiveBatchFilters.status.trim() || undefined,
  compareStatus: messageArchiveBatchFilters.onlyAbnormal
    ? undefined
    : messageArchiveBatchFilters.compareStatus.trim() || undefined,
  onlyAbnormal: messageArchiveBatchFilters.onlyAbnormal || undefined,
  dateFrom: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateFrom, 'start'),
  dateTo: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateTo, 'end'),
  pageNum: 1,
  pageSize: 5
})
```

- [ ] **Step 5: 实现 overview 拉取、摘要卡与异常行展示**

```ts
const getMessageArchiveBatchOverview = async () => {
  if (!isSystemMode.value) {
    messageArchiveBatchOverview.value = null
    return
  }
  messageArchiveBatchOverviewLoading.value = true
  try {
    const res = await getObservabilityMessageArchiveBatchOverview({
      sourceTable: 'iot_message_log',
      dateFrom: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateFrom, 'start'),
      dateTo: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateTo, 'end')
    })
    if (res.code === 200) {
      messageArchiveBatchOverview.value = res.data ?? null
    }
  } finally {
    messageArchiveBatchOverviewLoading.value = false
  }
}
```

```vue
<section class="audit-log-archive-overview">
  <div class="audit-log-archive-overview__cards">
    <article class="audit-log-archive-overview__card">
      <span class="audit-log-archive-overview__label">异常批次数</span>
      <strong>{{ formatOptionalCount(messageArchiveBatchOverview?.abnormalBatches) }}</strong>
    </article>
    <article class="audit-log-archive-overview__card">
      <span class="audit-log-archive-overview__label">执行偏差总量</span>
      <strong>{{ formatOptionalCount(messageArchiveBatchOverview?.totalDeltaConfirmedVsDeleted) }}</strong>
    </article>
    <article class="audit-log-archive-overview__card">
      <span class="audit-log-archive-overview__label">剩余过期总量</span>
      <strong>{{ formatOptionalCount(messageArchiveBatchOverview?.totalRemainingExpiredRows) }}</strong>
    </article>
    <article class="audit-log-archive-overview__card">
      <span class="audit-log-archive-overview__label">最近异常批次</span>
      <strong>{{ formatValue(messageArchiveBatchOverview?.latestAbnormalBatch) }}</strong>
    </article>
  </div>
</section>
```

列表筛选补：

```vue
<el-select v-model="messageArchiveBatchFilters.compareStatus" placeholder="对比结论" clearable>
  <el-option label="已对齐" value="MATCHED" />
  <el-option label="有偏差" value="DRIFTED" />
  <el-option label="部分可比" value="PARTIAL" />
  <el-option label="不可用" value="UNAVAILABLE" />
</el-select>
<el-switch
  v-model="messageArchiveBatchFilters.onlyAbnormal"
  active-text="仅看异常"
  inactive-text="全部批次"
/>
```

行级状态类：

```ts
const resolveArchiveBatchRowStateClass = (row: ObservabilityMessageArchiveBatch) => {
  switch (String(row.compareStatus || '').toUpperCase()) {
    case 'DRIFTED':
      return 'is-drifted'
    case 'PARTIAL':
      return 'is-partial'
    case 'UNAVAILABLE':
      return 'is-unavailable'
    default:
      return ''
  }
}
```

- [ ] **Step 6: 复跑前端定向回归**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: `BUILD SUCCESS`，相关 `observability` 与 `AuditLogView` 用例全部通过。

- [ ] **Step 7: Commit**

```bash
git add \
  spring-boot-iot-ui/src/api/observability.ts \
  spring-boot-iot-ui/src/views/AuditLogView.vue \
  spring-boot-iot-ui/src/__tests__/api/observability.test.ts \
  spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: add archive batch anomaly overview ui"
```

### Task 4: 文档同步与最终验证

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`

- [ ] **Step 1: 在 `docs/03` 补 page 新参数、新字段和 overview 接口**

要补的正文要点：

```md
- `GET /api/system/observability/message-archive-batches/page` 新增 `compareStatus / onlyAbnormal` 查询参数，并回传 `compareStatus / compareStatusLabel / deltaConfirmedVsDeleted / deltaDryRunVsDeleted / remainingExpiredRows / previewAvailable / previewReasonCode`。
- `GET /api/system/observability/message-archive-batches/overview` 按时间窗口汇总 `matchedBatches / driftedBatches / partialBatches / unavailableBatches / abnormalBatches / totalDeltaConfirmedVsDeleted / totalRemainingExpiredRows / latestAbnormalBatch / latestAbnormalOccurredAt`。
```

- [ ] **Step 2: 在 `docs/11` 补 `/system-log` G4 页面口径**

要补的正文要点：

```md
- `/system-log` 的归档批次台账顶部新增异常摘要卡，直接回答异常批次数、执行偏差总量、剩余过期总量和最近异常批次。
- 台账筛选新增 `对比结论` 与 `仅看异常`，其中 `仅看异常` 固定收口 `DRIFTED / PARTIAL / UNAVAILABLE`。
- 详情抽屉继续沿用 G3 的 `批次对比 + 确认报告预览`，G4 不新增第二套详情页。
```

- [ ] **Step 3: 在 `README.md`、`AGENTS.md` 和 `docs/08` 同步 G4 变更摘要**

要补的正文要点：

```md
- 可观测证据链 G4 已把归档批次异常摘要前移到 `/system-log` 列表层：顶部摘要卡、`compareStatus` 筛选和异常行高亮已可直接使用；抽屉仍保留详细 compare 与报告预览。
```

`docs/08` 需补一条明确的 2026-04-26 变更记录，注明验证命令。

- [ ] **Step 4: 跑最终验证**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
git diff --check
```

Expected:

1. Maven 定向回归通过
2. Vitest 定向回归通过
3. `git diff --check` 无格式问题

- [ ] **Step 5: Commit**

```bash
git add \
  README.md \
  AGENTS.md \
  docs/03-接口规范与接口清单.md \
  docs/08-变更记录与技术债清单.md \
  docs/11-可观测性、日志追踪与消息通知治理.md
git commit -m "docs: document archive batch anomaly overview"
```

