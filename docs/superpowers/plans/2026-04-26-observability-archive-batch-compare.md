# G3 归档批次 dry-run / apply 对比 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/system-log` 的归档批次详情抽屉中新增 dry-run / apply compare 结论，让运维可以直接判断批次是否按确认结果落地。

**Architecture:** 后端新增 `message-archive-batches/compare` 只读接口，统一拼接批次真相、dry-run 报告和 apply 报告并输出 `MATCHED / DRIFTED / PARTIAL / UNAVAILABLE`。前端在现有 `/system-log` 抽屉中增加“批次对比”总览卡和分表对比，保留原有 `确认报告预览` 作为原始证据入口。

**Tech Stack:** Spring Boot 4、Java 17、JdbcTemplate、Vue 3、TypeScript、Vitest、JUnit 5、Maven

---

### Task 1: 后端 compare 读侧

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareVO.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareSummaryVO.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareSourceVO.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareTableVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java`

- [ ] **Step 1: 先写 controller 失败用例**

```java
@Test
void getMessageArchiveBatchCompareShouldDelegateByBatchNo() {
    ObservabilityMessageArchiveBatchCompareVO compare = new ObservabilityMessageArchiveBatchCompareVO();
    compare.setBatchNo("iot_message_log-20260426000119");
    compare.setCompareStatus("MATCHED");
    when(observabilityEvidenceQueryService.getMessageArchiveBatchCompare(
            "iot_message_log-20260426000119", 10001L
    )).thenReturn(compare);

    R<ObservabilityMessageArchiveBatchCompareVO> response =
            controller.getMessageArchiveBatchCompare("iot_message_log-20260426000119", authentication(10001L));

    assertThat(response.getData().getCompareStatus()).isEqualTo("MATCHED");
    verify(observabilityEvidenceQueryService).getMessageArchiveBatchCompare(
            "iot_message_log-20260426000119", 10001L
    );
}
```

- [ ] **Step 2: 跑 controller 定向测试，确认 compare 接口还不存在**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `ObservabilityEvidenceControllerTest` 因 `getMessageArchiveBatchCompare(String, Long)` 缺失而失败。

- [ ] **Step 3: 补 service 接口、controller 路由和 compare VO**

```java
public interface ObservabilityEvidenceQueryService {
    ObservabilityMessageArchiveBatchCompareVO getMessageArchiveBatchCompare(String batchNo, Long currentUserId);
}

@GetMapping("/message-archive-batches/compare")
public R<ObservabilityMessageArchiveBatchCompareVO> getMessageArchiveBatchCompare(
        String batchNo,
        Authentication authentication
) {
    return R.ok(observabilityEvidenceQueryService.getMessageArchiveBatchCompare(
            batchNo,
            requireCurrentUserId(authentication)
    ));
}
```

```java
@Data
public class ObservabilityMessageArchiveBatchCompareVO {
    private String batchNo;
    private String sourceTable;
    private String status;
    private String compareStatus;
    private String compareMessage;
    private ObservabilityMessageArchiveBatchCompareSourceVO sources;
    private ObservabilityMessageArchiveBatchCompareSummaryVO summaryCompare;
    private List<ObservabilityMessageArchiveBatchCompareTableVO> tableComparisons;
}
```

- [ ] **Step 4: 写 service impl 的最小 compare 路径**

实现要点：

```java
public ObservabilityMessageArchiveBatchCompareVO getMessageArchiveBatchCompare(String batchNo, Long currentUserId) {
    permissionService.requirePermission(currentUserId, "system:observability:query");
    Map<String, Object> batch = loadMessageArchiveBatchByBatchNo(batchNo);
    if (batch == null || batch.isEmpty()) {
        throw new BizException(404, "归档批次不存在");
    }
    return buildMessageArchiveBatchCompare(batch);
}

private ObservabilityMessageArchiveBatchCompareVO buildMessageArchiveBatchCompare(Map<String, Object> batch) {
    Path dryRunJsonPath = resolveAllowedObservabilityPath(stringValue(batch.get("confirmReportPath")));
    Path applyJsonPath = resolveAllowedObservabilityPath(extractReportJsonPath(batch.get("artifactsJson")));
    // 1. dry-run 不可用 => UNAVAILABLE
    // 2. dry-run 可用但 apply 不可用 => PARTIAL
    // 3. 两者都可用，计算 summary / tables => MATCHED or DRIFTED
}
```

必要辅助函数：

```java
private String extractReportJsonPath(Object artifactsJson) {
    Map<String, Object> artifacts = parseJsonObject(stringValue(artifactsJson));
    return stringValue(artifacts.get("reportJsonPath"));
}

private ObservabilityMessageArchiveBatchCompareVO buildUnavailableCompare(
        Map<String, Object> batch,
        String reasonCode,
        String reasonMessage
) {
    ObservabilityMessageArchiveBatchCompareVO compare = buildBaseCompare(batch);
    compare.setCompareStatus("UNAVAILABLE");
    compare.setCompareMessage(reasonMessage);
    compare.setSummaryCompare(null);
    compare.setTableComparisons(List.of());
    compare.getSources().setDryRunAvailable(false);
    compare.getSources().setApplyAvailable(false);
    compare.getSources().setReasonCode(reasonCode);
    return compare;
}

private ObservabilityMessageArchiveBatchCompareVO buildPartialCompare(
        Map<String, Object> batch,
        Path dryRunJsonPath,
        Map<String, Object> dryRunPayload,
        String reasonMessage
) {
    ObservabilityMessageArchiveBatchCompareVO compare = buildBaseCompare(batch);
    compare.setCompareStatus("PARTIAL");
    compare.setCompareMessage(reasonMessage);
    compare.setSummaryCompare(buildPartialSummary(batch, dryRunPayload));
    compare.setTableComparisons(List.of());
    compare.getSources().setResolvedDryRunJsonPath(toDisplayPath(dryRunJsonPath));
    compare.getSources().setDryRunAvailable(true);
    compare.getSources().setApplyAvailable(false);
    return compare;
}

private ObservabilityMessageArchiveBatchCompareVO buildCompleteCompare(
        Map<String, Object> batch,
        Path dryRunJsonPath,
        Path applyJsonPath,
        Map<String, Object> dryRunPayload,
        Map<String, Object> applyPayload
) {
    ObservabilityMessageArchiveBatchCompareVO compare = buildBaseCompare(batch);
    compare.setSummaryCompare(buildCompleteSummary(batch, dryRunPayload, applyPayload));
    compare.setTableComparisons(buildTableComparisons(dryRunPayload, applyPayload));
    compare.setCompareStatus(resolveCompareStatus(compare.getSummaryCompare(), compare.getTableComparisons()));
    compare.setCompareMessage("MATCHED".equals(compare.getCompareStatus()) ? "已按确认结果落地" : "执行结果与确认结果存在偏差");
    compare.getSources().setResolvedDryRunJsonPath(toDisplayPath(dryRunJsonPath));
    compare.getSources().setResolvedApplyJsonPath(toDisplayPath(applyJsonPath));
    compare.getSources().setDryRunAvailable(true);
    compare.getSources().setApplyAvailable(true);
    return compare;
}

private String resolveCompareStatus(
        ObservabilityMessageArchiveBatchCompareSummaryVO summary,
        List<ObservabilityMessageArchiveBatchCompareTableVO> tableComparisons
) {
    boolean summaryMatched = summary != null && Boolean.TRUE.equals(summary.getMatched());
    boolean tablesMatched = tableComparisons.stream().allMatch(item -> Boolean.TRUE.equals(item.getMatched()));
    return summaryMatched && tablesMatched ? "MATCHED" : "DRIFTED";
}
```

- [ ] **Step 5: 补 compare 失败/降级/成功测试**

至少覆盖：

```java
@Test
void getMessageArchiveBatchCompareShouldReturnUnavailableWhenDryRunPathRejected() {
    mockArchiveBatchLookup("../../../tmp/outside.json", "{\"reportJsonPath\":\"logs/observability/apply.json\"}");

    ObservabilityMessageArchiveBatchCompareVO result =
            service.getMessageArchiveBatchCompare("iot_message_log-20260426000119", 10001L);

    assertThat(result.getCompareStatus()).isEqualTo("UNAVAILABLE");
    assertThat(result.getCompareMessage()).contains("确认报告");
}

@Test
void getMessageArchiveBatchCompareShouldReturnPartialWhenApplyJsonMissing() throws IOException {
    writeObservabilityReportFile("dry-run.json", DRY_RUN_JSON);
    mockArchiveBatchLookup("logs/observability/dry-run.json", "{\"reportJsonPath\":\"logs/observability/missing-apply.json\"}");

    ObservabilityMessageArchiveBatchCompareVO result =
            service.getMessageArchiveBatchCompare("iot_message_log-20260426000119", 10001L);

    assertThat(result.getCompareStatus()).isEqualTo("PARTIAL");
    assertThat(result.getSummaryCompare().getDryRunExpiredRows()).isEqualTo(16098L);
}

@Test
void getMessageArchiveBatchCompareShouldReturnMatchedWhenDryRunAndApplyAlign() throws IOException {
    writeObservabilityReportFile("dry-run.json", DRY_RUN_JSON);
    writeObservabilityReportFile("apply.json", APPLY_MATCHED_JSON);
    mockArchiveBatchLookup("logs/observability/dry-run.json", "{\"reportJsonPath\":\"logs/observability/apply.json\"}");

    ObservabilityMessageArchiveBatchCompareVO result =
            service.getMessageArchiveBatchCompare("iot_message_log-20260426000119", 10001L);

    assertThat(result.getCompareStatus()).isEqualTo("MATCHED");
    assertThat(result.getSummaryCompare().getDeltaConfirmedVsDeleted()).isEqualTo(0L);
}

@Test
void getMessageArchiveBatchCompareShouldReturnDriftedWhenRemainingExpiredRowsIsPositive() throws IOException {
    writeObservabilityReportFile("dry-run.json", DRY_RUN_JSON);
    writeObservabilityReportFile("apply.json", APPLY_DRIFTED_JSON);
    mockArchiveBatchLookup("logs/observability/dry-run.json", "{\"reportJsonPath\":\"logs/observability/apply.json\"}");

    ObservabilityMessageArchiveBatchCompareVO result =
            service.getMessageArchiveBatchCompare("iot_message_log-20260426000119", 10001L);

    assertThat(result.getCompareStatus()).isEqualTo("DRIFTED");
    assertThat(result.getTableComparisons()).anyMatch(item -> Boolean.FALSE.equals(item.getMatched()));
}
```

测试数据约定：

```json
{
  "summary": { "expiredRows": 16098, "archivedRows": 16098, "deletedRows": 16098 },
  "tables": {
    "iot_message_log": {
      "label": "设备消息日志",
      "expiredRows": 16098,
      "archivedRows": 16098,
      "deletedRows": 16098,
      "remainingExpiredRows": 0
    }
  }
}
```

- [ ] **Step 6: 跑后端定向回归**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`，compare 新增用例与既有 report-preview 用例全部通过。

- [ ] **Step 7: 提交后端 compare**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareVO.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareSummaryVO.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareSourceVO.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchCompareTableVO.java \
        spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java \
        spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java
git commit -m "feat: add archive batch compare api"
```

### Task 2: 前端 compare 工作台

**Files:**
- Modify: `spring-boot-iot-ui/src/api/observability.ts`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 先写 API client 失败测试**

```ts
it('loads message archive batch compare by batch number', async () => {
  await getObservabilityMessageArchiveBatchCompare('iot_message_log-20260426000119')

  expect(request).toHaveBeenCalledWith(
    '/api/system/observability/message-archive-batches/compare?batchNo=iot_message_log-20260426000119',
    { method: 'GET' }
  )
})
```

- [ ] **Step 2: 先写视图失败测试**

```ts
it('renders archive batch compare summary and drift rows inside the same drawer', async () => {
  vi.mocked(getObservabilityMessageArchiveBatchCompare).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      batchNo: 'iot_message_log-20260426000119',
      compareStatus: 'DRIFTED',
      compareMessage: '执行结果与确认结果存在偏差',
      summaryCompare: {
        confirmedExpiredRows: 16098,
        applyDeletedRows: 15980,
        deltaConfirmedVsDeleted: 118
      },
      tableComparisons: [
        {
          tableName: 'iot_message_log',
          label: '设备消息日志',
          dryRunExpiredRows: 16098,
          applyDeletedRows: 15980,
          applyRemainingExpiredRows: 118,
          matched: false
        }
      ]
    }
  })
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  const detailButton = wrapper.findAll('button').find((button) => button.text().includes('详情'))
  await detailButton!.trigger('click')
  await flushPromises()
  await nextTick()

  const drawer = wrapper.findAll('.observability-evidence-drawer-stub').find((item) => item.text().includes('归档批次详情'))
  expect(drawer?.text()).toContain('批次对比')
  expect(drawer?.text()).toContain('118')
  expect(drawer?.text()).toContain('设备消息日志')
})
```

- [ ] **Step 3: 跑前端定向测试，确认 compare 还未接入**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: compare API / compare drawer 相关断言失败。

- [ ] **Step 4: 补 API types 和 compare client**

```ts
export interface ObservabilityMessageArchiveBatchCompareSummary {
  confirmedExpiredRows?: number | null
  dryRunExpiredRows?: number | null
  applyArchivedRows?: number | null
  applyDeletedRows?: number | null
  remainingExpiredRows?: number | null
  deltaConfirmedVsDeleted?: number | null
  deltaDryRunVsDeleted?: number | null
  matched?: boolean | null
}

export interface ObservabilityMessageArchiveBatchCompareTable {
  tableName?: string | null
  label?: string | null
  dryRunExpiredRows?: number | null
  applyArchivedRows?: number | null
  applyDeletedRows?: number | null
  applyRemainingExpiredRows?: number | null
  deltaDryRunVsDeleted?: number | null
  matched?: boolean | null
  reason?: string | null
}

export interface ObservabilityMessageArchiveBatchCompareSource {
  confirmReportPath?: string | null
  resolvedDryRunJsonPath?: string | null
  resolvedApplyJsonPath?: string | null
  dryRunAvailable?: boolean | null
  applyAvailable?: boolean | null
}

export interface ObservabilityMessageArchiveBatchCompare {
  batchNo?: string | null
  sourceTable?: string | null
  status?: string | null
  compareStatus?: string | null
  compareMessage?: string | null
  sources?: ObservabilityMessageArchiveBatchCompareSource | null
  summaryCompare?: ObservabilityMessageArchiveBatchCompareSummary | null
  tableComparisons?: ObservabilityMessageArchiveBatchCompareTable[]
}

export function getObservabilityMessageArchiveBatchCompare(
  batchNo: string
): Promise<ApiEnvelope<ObservabilityMessageArchiveBatchCompare>> {
  const query = buildQueryString({ batchNo })
  return request(`/api/system/observability/message-archive-batches/compare?${query}`, { method: 'GET' })
}
```

- [ ] **Step 5: 在 `AuditLogView.vue` 接 compare 状态与视图**

新增状态：

```ts
const messageArchiveBatchCompareLoading = ref(false)
const messageArchiveBatchCompareErrorMessage = ref('')
const activeMessageArchiveBatchCompare = ref<ObservabilityMessageArchiveBatchCompare | null>(null)
```

新增加载逻辑：

```ts
const loadMessageArchiveBatchCompare = async (row: ObservabilityMessageArchiveBatch) => {
  const batchNo = String(row.batchNo || '').trim()
  if (!batchNo) {
    messageArchiveBatchCompareErrorMessage.value = '当前批次缺少批次号，无法加载批次对比'
    activeMessageArchiveBatchCompare.value = null
    return
  }
  const res = await getObservabilityMessageArchiveBatchCompare(batchNo)
  activeMessageArchiveBatchCompare.value = res.data
}
```

打开抽屉时并行加载：

```ts
await Promise.all([
  loadMessageArchiveBatchReportPreview(row),
  loadMessageArchiveBatchCompare(row)
])
```

模板增加 compare 区块：

```vue
<section class="observability-evidence-section">
  <header class="observability-evidence-section__header">
    <h3>批次对比</h3>
  </header>
  <div v-if="messageArchiveBatchCompareLoading" class="observability-evidence-empty">正在加载批次对比</div>
  <div v-else-if="messageArchiveBatchCompareErrorMessage" class="observability-evidence-empty">{{ messageArchiveBatchCompareErrorMessage }}</div>
  <div v-else-if="!activeMessageArchiveBatchCompare" class="observability-evidence-empty">暂无批次对比</div>
  <div v-else class="observability-archive-batch-compare">
    <section class="observability-evidence-summary">
      <article v-for="item in messageArchiveBatchCompareSummaryCards" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>
    <div v-if="isArchiveBatchCompareUnavailable" class="observability-evidence-empty">
      {{ activeMessageArchiveBatchCompare.compareMessage }}
    </div>
    <div v-else class="observability-archive-batch-compare__rows">
      <article
        v-for="item in activeMessageArchiveBatchCompare.tableComparisons || []"
        :key="item.tableName"
        :class="archiveBatchCompareRowClass(item)"
      >
        {{ item.label || item.tableName }} / {{ item.deltaDryRunVsDeleted }}
      </article>
    </div>
  </div>
</section>
```

总览卡展示：

```ts
const messageArchiveBatchCompareSummaryCards = computed(() => [
  { label: '结论', value: formatValue(compare?.compareStatus) },
  { label: 'dry-run 确认', value: formatCount(compare?.summaryCompare?.confirmedExpiredRows) },
  { label: 'apply 删除', value: formatCount(compare?.summaryCompare?.applyDeletedRows) },
  { label: '偏差', value: formatCount(compare?.summaryCompare?.deltaConfirmedVsDeleted) }
])
```

- [ ] **Step 6: 让 `PARTIAL / UNAVAILABLE / DRIFTED` 都有清晰空态或高亮**

```ts
const isArchiveBatchCompareUnavailable = computed(() => compare?.compareStatus === 'UNAVAILABLE')
const isArchiveBatchComparePartial = computed(() => compare?.compareStatus === 'PARTIAL')
const archiveBatchCompareRowClass = (item: ObservabilityMessageArchiveBatchCompareTable) =>
  item.matched === false ? 'observability-archive-batch-compare__row--drifted' : ''
```

- [ ] **Step 7: 跑前端定向回归**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: `2 passed`，compare 与 preview 相关断言全部通过。

- [ ] **Step 8: 提交前端 compare**

```bash
git add spring-boot-iot-ui/src/api/observability.ts \
        spring-boot-iot-ui/src/views/AuditLogView.vue \
        spring-boot-iot-ui/src/__tests__/api/observability.test.ts \
        spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: add archive batch compare workbench"
```

### Task 3: 文档、回归与分支收口

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`

- [ ] **Step 1: 更新接口与页面文档**

文档至少补齐：

```md
- 新增 `GET /api/system/observability/message-archive-batches/compare`
- compare 状态固定为 `MATCHED / DRIFTED / PARTIAL / UNAVAILABLE`
- `/system-log` 的归档批次详情抽屉新增“批次对比”，总览优先显示 dry-run / apply 偏差
```

- [ ] **Step 2: 跑格式与回归检查**

Run:

```bash
git diff --check
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected:

- `git diff --check` 无输出
- Maven `BUILD SUCCESS`
- Vitest `2 passed`

- [ ] **Step 3: 提交文档与回归收口**

```bash
git add README.md AGENTS.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md docs/11-可观测性、日志追踪与消息通知治理.md
git commit -m "docs: document archive batch compare workflow"
```

- [ ] **Step 4: 完成开发分支**

```bash
git status --short
git log --oneline --decorate -5
```

Expected:

- 工作树干净
- 最近提交包含 compare API、compare workbench、文档更新

然后按当前仓库约定把任务分支合回 `codex/dev`，同时不要覆盖主工作区已有并行未提交改动。
