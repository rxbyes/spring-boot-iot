# G2 归档批次筛选与确认报告预览 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/system-log` 中补齐归档批次台账的局部筛选和确认报告预览，让运维能直接在现有工作台内完成批次核账。

**Architecture:** 保留现有 `message-archive-batches/page` 作为唯一批次分页接口，在后端新增按 `batchNo` 查询的只读确认报告预览接口。前端继续复用 `AuditLogView.vue` 的同页卡片与详情抽屉，只增加局部筛选状态、预览请求状态与报告摘要展示，不新增独立治理页。

**Tech Stack:** Spring Boot 4, Java 17, JdbcTemplate, Vue 3, TypeScript, Vitest, Maven Surefire

---

## File Map

- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java`
  - 新增确认报告预览读接口。
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java`
  - 暴露报告预览查询方法。
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchReportPreviewVO.java`
  - 承载预览响应主体。
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchReportTableSummaryVO.java`
  - 承载报告中的分表摘要。
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java`
  - 实现按批次号查表、白名单路径校验、JSON/Markdown 解析与截断逻辑。
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java`
  - 验证控制器委派。
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java`
  - 验证文件读取、路径边界、部分可用与预览构造。
- Modify: `spring-boot-iot-ui/src/api/observability.ts`
  - 暴露报告预览接口与类型。
- Test: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`
  - 验证 query builder。
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
  - 增加批次筛选 UI、报告预览状态、抽屉展示。
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - 验证筛选请求、详情预览请求和三种状态渲染。
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

### Task 1: 后端预览接口契约与控制器委派

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchReportPreviewVO.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchReportTableSummaryVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java`

- [ ] **Step 1: 写控制器委派失败测试**

```java
@Test
void getMessageArchiveBatchReportPreviewShouldDelegateByBatchNo() {
    ObservabilityMessageArchiveBatchReportPreviewVO preview = new ObservabilityMessageArchiveBatchReportPreviewVO();
    preview.setBatchNo("iot_message_log-20260426000119");
    preview.setAvailable(true);
    when(observabilityEvidenceQueryService.getMessageArchiveBatchReportPreview(
            "iot_message_log-20260426000119",
            10001L
    )).thenReturn(preview);

    R<ObservabilityMessageArchiveBatchReportPreviewVO> response =
            controller.getMessageArchiveBatchReportPreview(
                    "iot_message_log-20260426000119",
                    authentication(10001L)
            );

    assertEquals("iot_message_log-20260426000119", response.getData().getBatchNo());
    assertTrue(Boolean.TRUE.equals(response.getData().getAvailable()));
    verify(observabilityEvidenceQueryService).getMessageArchiveBatchReportPreview(
            "iot_message_log-20260426000119",
            10001L
    );
}
```

- [ ] **Step 2: 运行控制器测试并确认失败**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
cannot find symbol: method getMessageArchiveBatchReportPreview(...)
```

- [ ] **Step 3: 写最小 VO、服务接口和控制器实现**

```java
// ObservabilityEvidenceQueryService.java
ObservabilityMessageArchiveBatchReportPreviewVO getMessageArchiveBatchReportPreview(
        String batchNo,
        Long currentUserId
);
```

```java
// ObservabilityEvidenceController.java
@GetMapping("/message-archive-batches/report-preview")
public R<ObservabilityMessageArchiveBatchReportPreviewVO> getMessageArchiveBatchReportPreview(
        String batchNo,
        Authentication authentication
) {
    return R.ok(observabilityEvidenceQueryService.getMessageArchiveBatchReportPreview(
            batchNo,
            requireCurrentUserId(authentication)
    ));
}
```

```java
// ObservabilityMessageArchiveBatchReportPreviewVO.java
@Data
public class ObservabilityMessageArchiveBatchReportPreviewVO {
    private String batchNo;
    private String sourceTable;
    private String status;
    private String confirmReportPath;
    private LocalDateTime confirmReportGeneratedAt;
    private Boolean available;
    private String reasonCode;
    private String reasonMessage;
    private String resolvedJsonPath;
    private String resolvedMarkdownPath;
    private Boolean markdownAvailable;
    private Boolean markdownTruncated;
    private String markdownPreview;
    private LocalDateTime fileLastModifiedAt;
    private Map<String, Object> summary;
    private List<ObservabilityMessageArchiveBatchReportTableSummaryVO> tableSummaries;
}
```

- [ ] **Step 4: 运行控制器测试并确认通过**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: 提交这一小步**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchReportPreviewVO.java \
        spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchReportTableSummaryVO.java \
        spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java
git commit -m "feat: add archive batch report preview contract"
```

### Task 2: 后端报告读取、白名单校验与预览构造

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java`

- [ ] **Step 1: 写服务层失败测试，先覆盖路径和部分可用语义**

```java
@Test
void getMessageArchiveBatchReportPreviewShouldRejectPathOutsideObservabilityLogs() {
    ObservabilityMessageArchiveBatchVO row = new ObservabilityMessageArchiveBatchVO();
    row.setBatchNo("iot_message_log-20260426000119");
    row.setConfirmReportPath("../secrets/report.json");
    mockArchiveBatchLookup(row);

    ObservabilityMessageArchiveBatchReportPreviewVO result =
            service.getMessageArchiveBatchReportPreview("iot_message_log-20260426000119", 10001L);

    assertFalse(Boolean.TRUE.equals(result.getAvailable()));
    assertEquals("REPORT_PATH_REJECTED", result.getReasonCode());
}

@Test
void getMessageArchiveBatchReportPreviewShouldKeepJsonSummaryWhenMarkdownMissing() {
    Path jsonPath = writePreviewJson("""
            {"generatedAt":"2026-04-25T23:59:00","mode":"APPLY","summary":{"expiredRows":16098,"deletedRows":16098},"tables":{"iot_message_log":{"label":"设备消息日志","retentionDays":30,"expiredRows":16098,"deletedRows":16098}}}
            """);
    ObservabilityMessageArchiveBatchVO row = new ObservabilityMessageArchiveBatchVO();
    row.setBatchNo("iot_message_log-20260426000119");
    row.setConfirmReportPath(toRepoRelativePath(jsonPath));
    mockArchiveBatchLookup(row);

    ObservabilityMessageArchiveBatchReportPreviewVO result =
            service.getMessageArchiveBatchReportPreview("iot_message_log-20260426000119", 10001L);

    assertTrue(Boolean.TRUE.equals(result.getAvailable()));
    assertFalse(Boolean.TRUE.equals(result.getMarkdownAvailable()));
    assertEquals(16098, ((Number) result.getSummary().get("expiredRows")).intValue());
    assertEquals(1, result.getTableSummaries().size());
}
```

- [ ] **Step 2: 运行服务层测试并确认失败**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
cannot find symbol: method getMessageArchiveBatchReportPreview(...)
```

- [ ] **Step 3: 写最小实现，固定按批次查表并限制读取目录**

```java
private static final Path OBSERVABILITY_LOG_DIR =
        Paths.get("").toAbsolutePath().normalize().resolve("logs").resolve("observability");

@Override
public ObservabilityMessageArchiveBatchReportPreviewVO getMessageArchiveBatchReportPreview(
        String batchNo,
        Long currentUserId
) {
    String normalizedBatchNo = normalize(batchNo);
    if (!StringUtils.hasText(normalizedBatchNo)) {
        throw new BizException(400, "batchNo 不能为空");
    }
    ObservabilityMessageArchiveBatchVO batch = loadMessageArchiveBatchByBatchNo(normalizedBatchNo);
    return buildMessageArchiveBatchReportPreview(batch);
}

private ObservabilityMessageArchiveBatchReportPreviewVO buildMessageArchiveBatchReportPreview(
        ObservabilityMessageArchiveBatchVO batch
) {
    ObservabilityMessageArchiveBatchReportPreviewVO preview = new ObservabilityMessageArchiveBatchReportPreviewVO();
    preview.setBatchNo(batch.getBatchNo());
    preview.setSourceTable(batch.getSourceTable());
    preview.setStatus(batch.getStatus());
    preview.setConfirmReportPath(batch.getConfirmReportPath());
    preview.setConfirmReportGeneratedAt(batch.getConfirmReportGeneratedAt());
    // 下面继续补全 path resolve、json parse、markdown preview
    return preview;
}
```

- [ ] **Step 4: 补齐 JSON/Markdown 解析与截断逻辑**

```java
private Path resolveReportPath(String rawPath) {
    Path candidate = Paths.get(rawPath);
    Path resolved = candidate.isAbsolute()
            ? candidate.normalize()
            : Paths.get("").toAbsolutePath().normalize().resolve(candidate).normalize();
    if (!resolved.startsWith(OBSERVABILITY_LOG_DIR)) {
        throw new BizException(400, "REPORT_PATH_REJECTED");
    }
    return resolved;
}

private String readMarkdownPreview(Path markdownPath) throws IOException {
    String text = Files.readString(markdownPath, StandardCharsets.UTF_8);
    String[] lines = text.split("\\R");
    String preview = Arrays.stream(lines).limit(80).collect(Collectors.joining(System.lineSeparator()));
    return preview.length() <= 6000 ? preview : preview.substring(0, 6000);
}
```

- [ ] **Step 5: 运行服务层测试并确认通过**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: 提交这一小步**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java \
        spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java
git commit -m "feat: add archive batch report preview service"
```

### Task 3: 前端 API 层与 query builder

**Files:**
- Modify: `spring-boot-iot-ui/src/api/observability.ts`
- Test: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`

- [ ] **Step 1: 写 API 层失败测试**

```ts
it('loads archive batch report preview by batch number', async () => {
  await getObservabilityMessageArchiveBatchReportPreview('iot_message_log-20260426000119')

  expect(request).toHaveBeenCalledWith(
    '/api/system/observability/message-archive-batches/report-preview?batchNo=iot_message_log-20260426000119',
    { method: 'GET' }
  )
})
```

- [ ] **Step 2: 运行 API 测试并确认失败**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts
```

Expected:

```text
ReferenceError: getObservabilityMessageArchiveBatchReportPreview is not defined
```

- [ ] **Step 3: 写最小 API 类型和请求函数**

```ts
export interface ObservabilityMessageArchiveBatchReportTableSummary {
  tableName?: string | null
  label?: string | null
  retentionDays?: number | null
  expiredRows?: number | null
  deletedRows?: number | null
  remainingExpiredRows?: number | null
  cutoffAt?: string | null
}

export interface ObservabilityMessageArchiveBatchReportPreview {
  batchNo?: string | null
  sourceTable?: string | null
  status?: string | null
  confirmReportPath?: string | null
  available?: boolean | null
  reasonCode?: string | null
  reasonMessage?: string | null
  markdownAvailable?: boolean | null
  markdownTruncated?: boolean | null
  markdownPreview?: string | null
  summary?: Record<string, unknown> | null
  tableSummaries?: ObservabilityMessageArchiveBatchReportTableSummary[]
}

export function getObservabilityMessageArchiveBatchReportPreview(batchNo: string) {
  const query = buildQueryString({ batchNo })
  return request<ObservabilityMessageArchiveBatchReportPreview>(
    `/api/system/observability/message-archive-batches/report-preview?${query}`,
    { method: 'GET' }
  )
}
```

- [ ] **Step 4: 运行 API 测试并确认通过**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts
```

Expected:

```text
✓ src/__tests__/api/observability.test.ts
```

- [ ] **Step 5: 提交这一小步**

```bash
git add spring-boot-iot-ui/src/api/observability.ts \
        spring-boot-iot-ui/src/__tests__/api/observability.test.ts
git commit -m "feat: add archive batch report preview api"
```

### Task 4: `/system-log` 批次筛选与报告预览交互

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 写前端失败测试，覆盖筛选和预览请求**

```ts
it('queries archive batches with local filters and loads report preview on detail open', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  await wrapper.find('input[placeholder=\"批次号\"]').setValue('iot_message_log-20260426000119')
  await findButtonByText(wrapper, '查询')!.trigger('click')
  await flushPromises()

  expect(pageObservabilityMessageArchiveBatches).toHaveBeenLastCalledWith({
    batchNo: 'iot_message_log-20260426000119',
    sourceTable: 'iot_message_log',
    pageNum: 1,
    pageSize: 5
  })

  await wrapper.findAll('button').find((button) => button.text().includes('详情'))!.trigger('click')
  await flushPromises()

  expect(getObservabilityMessageArchiveBatchReportPreview)
    .toHaveBeenCalledWith('iot_message_log-20260426000119')
})
```

- [ ] **Step 2: 运行视图测试并确认失败**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

Expected:

```text
Unable to find input[placeholder="批次号"] or preview api mock not called
```

- [ ] **Step 3: 写最小本地筛选状态与请求组装**

```ts
const archiveBatchFilters = reactive({
  batchNo: '',
  status: '',
  dateRange: [] as string[]
})

const buildMessageArchiveBatchQueryParams = (): ObservabilityMessageArchiveBatchPageQuery => ({
  batchNo: archiveBatchFilters.batchNo.trim() || undefined,
  status: archiveBatchFilters.status || undefined,
  dateFrom: archiveBatchFilters.dateRange[0] || undefined,
  dateTo: archiveBatchFilters.dateRange[1] || undefined,
  sourceTable: 'iot_message_log',
  pageNum: 1,
  pageSize: 5
})
```

- [ ] **Step 4: 写详情抽屉预览状态与渲染**

```ts
const messageArchiveBatchReportPreviewLoading = ref(false)
const messageArchiveBatchReportPreviewError = ref('')
const messageArchiveBatchReportPreviewData = ref<ObservabilityMessageArchiveBatchReportPreview | null>(null)

const loadMessageArchiveBatchReportPreview = async (batchNo?: string | null) => {
  if (!batchNo) {
    messageArchiveBatchReportPreviewData.value = null
    return
  }
  messageArchiveBatchReportPreviewLoading.value = true
  messageArchiveBatchReportPreviewError.value = ''
  try {
    const res = await getObservabilityMessageArchiveBatchReportPreview(batchNo)
    if (res.code === 200) {
      messageArchiveBatchReportPreviewData.value = res.data || null
    }
  } catch (error) {
    messageArchiveBatchReportPreviewError.value =
      error instanceof Error ? error.message : '获取确认报告预览失败'
  } finally {
    messageArchiveBatchReportPreviewLoading.value = false
  }
}
```

- [ ] **Step 5: 运行视图测试并确认通过**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

Expected:

```text
✓ src/__tests__/views/AuditLogView.test.ts
```

- [ ] **Step 6: 提交这一小步**

```bash
git add spring-boot-iot-ui/src/views/AuditLogView.vue \
        spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: add archive batch filters and preview drawer"
```

### Task 5: 文档、全量验证与收口

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 更新文档中的接口与工作台行为**

```md
- `GET /api/system/observability/message-archive-batches/report-preview`
- `/system-log` 的归档批次台账支持 `批次号 / 状态 / 时间范围` 筛选
- 详情抽屉新增确认报告 JSON 摘要与 Markdown 预览
```

- [ ] **Step 2: 跑后端定向验证**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 跑前端定向验证**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected:

```text
✓ src/__tests__/api/observability.test.ts
✓ src/__tests__/views/AuditLogView.test.ts
```

- [ ] **Step 4: 做最终代码与文档提交**

```bash
git add docs/03-接口规范与接口清单.md \
        docs/08-变更记录与技术债清单.md \
        docs/11-可观测性、日志追踪与消息通知治理.md \
        README.md \
        AGENTS.md
git commit -m "feat: add archive batch report preview workflow"
```

- [ ] **Step 5: 记录验证结果并准备合回 `codex/dev`**

```bash
git status --short
git log --oneline --max-count=5
```

Expected:

```text
working tree clean
recent commits include archive batch preview changes
```
