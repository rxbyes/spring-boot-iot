# System Log Error Dual-Table Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `/system-log` error tab's split summary strip plus detail table with a dual-table workbench that shows clustered system errors on top and linked raw audit-log rows underneath.

**Architecture:** Add a backend cluster-page endpoint on top of the existing `sys_audit_log` truth so the UI can render real grouped system-error data instead of client-side partial aggregation. Then refactor the error-tab workbench to load cluster state, drive the detail table from the selected cluster, and fall back to the existing full-detail flow if the cluster endpoint fails.

**Tech Stack:** Spring Boot, Java 17, Spring MVC, `JdbcTemplate`, Vue 3 `script setup`, TypeScript, Element Plus, Vitest.

---

## File Map

- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/SystemErrorClusterRowVO.java`
  - Carries one grouped system-error cluster row for the new page endpoint.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/AuditLogService.java`
  - Declares the new clustered page query contract.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImpl.java`
  - Implements grouped SQL, latest-row lookups, and scoped filtering reuse.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/AuditLogController.java`
  - Exposes `GET /api/system/audit-log/system-error/clusters/page`.
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImplTest.java`
  - Locks the grouped query contract and tenant-scoping behavior.

- Modify: `spring-boot-iot-ui/src/api/auditLog.ts`
  - Adds cluster-row types and the new API client function.
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
  - Owns selected cluster state, cluster loading, fallback behavior, and error-tab props.
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`
  - Renders the cluster table plus linked detail table header/actions.
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts`
  - Verifies dual-table rendering and the “view all” recovery action.
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - Verifies new API orchestration, cluster selection, and fallback.

## Task 1: Add Backend System-Error Cluster Paging

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/SystemErrorClusterRowVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/AuditLogService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/AuditLogController.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImplTest.java`

- [ ] **Step 1: Write the failing service test for grouped cluster paging**

```java
@Test
void pageSystemErrorClustersShouldGroupByModuleExceptionAndErrorCode() throws Exception {
    when(permissionService.getDataPermissionContext(99L))
            .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

    when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

    when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0, String.class);
                if (sql.contains("GROUP BY operation_module")) {
                    return List.of(
                            Map.of(
                                    "operation_module", "PROTOCOL_DECODE",
                                    "exception_class", "DecodeException",
                                    "error_code", "payload_invalid",
                                    "count", 12L,
                                    "distinct_trace_count", 4L,
                                    "distinct_device_count", 3L,
                                    "latest_operation_time", Timestamp.valueOf("2026-04-27 09:00:00")
                            )
                    );
                }
                return List.of(
                        Map.of(
                                "request_url", "/mqtt/up",
                                "request_method", "MQTT",
                                "result_message", "payload schema mismatch"
                        )
                );
            });

    AuditLog log = new AuditLog();
    log.setRequestMethod("MQTT");

    PageResult<SystemErrorClusterRowVO> page = invokeClusterPage(99L, log, 1, 10);

    assertEquals(2L, page.getTotal());
    assertEquals(1, page.getRecords().size());
    SystemErrorClusterRowVO row = page.getRecords().get(0);
    assertEquals("PROTOCOL_DECODE", row.getOperationModule());
    assertEquals("DecodeException", row.getExceptionClass());
    assertEquals("payload_invalid", row.getErrorCode());
    assertEquals(12L, row.getCount());
    assertEquals(4L, row.getDistinctTraceCount());
    assertEquals(3L, row.getDistinctDeviceCount());
    assertEquals("/mqtt/up", row.getLatestRequestUrl());
}
```

- [ ] **Step 2: Run the new backend test to verify it fails**

Run: `mvn -pl spring-boot-iot-system -Dtest=AuditLogServiceImplTest#pageSystemErrorClustersShouldGroupByModuleExceptionAndErrorCode test`

Expected: FAIL with missing `pageSystemErrorClusters(...)`, missing `SystemErrorClusterRowVO`, or cluster query path not implemented.

- [ ] **Step 3: Add the new VO, service contract, controller endpoint, and grouped SQL implementation**

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/SystemErrorClusterRowVO.java
@Data
public class SystemErrorClusterRowVO {
    private String clusterKey;
    private String operationModule;
    private String exceptionClass;
    private String errorCode;
    private Long count = 0L;
    private Long distinctTraceCount = 0L;
    private Long distinctDeviceCount = 0L;
    private String latestOperationTime;
    private String latestRequestUrl;
    private String latestRequestMethod;
    private String latestResultMessage;
}
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/AuditLogService.java
PageResult<SystemErrorClusterRowVO> pageSystemErrorClusters(AuditLog log, Integer pageNum, Integer pageSize);
PageResult<SystemErrorClusterRowVO> pageSystemErrorClusters(Long currentUserId, AuditLog log, Integer pageNum, Integer pageSize);
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/AuditLogController.java
@GetMapping("/system-error/clusters/page")
public R<Map<String, Object>> pageSystemErrorClusters(
        AuditLog log,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        Authentication authentication
) {
    PageResult<SystemErrorClusterRowVO> page =
            auditLogService.pageSystemErrorClusters(requireCurrentUserId(authentication), log, pageNum, pageSize);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("total", page.getTotal());
    payload.put("pageNum", page.getPageNum());
    payload.put("pageSize", page.getPageSize());
    payload.put("records", page.getRecords());
    return R.ok(payload);
}
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImpl.java
@Override
public PageResult<SystemErrorClusterRowVO> pageSystemErrorClusters(Long currentUserId, AuditLog log, Integer pageNum, Integer pageSize) {
    Set<String> columns = auditLogSchemaSupport.getColumns();
    QuerySpec querySpec = buildQuerySpec(normalizeSystemErrorFilter(scopedLog(currentUserId, log)), false, columns);
    if (querySpec.emptyResult()) {
        return PageResult.empty(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
    }
    String operationModuleColumn = resolveColumn(columns, "operation_module");
    String exceptionClassColumn = resolveColumn(columns, "exception_class");
    String errorCodeColumn = resolveColumn(columns, "error_code");
    String traceIdColumn = resolveColumn(columns, "trace_id");
    String deviceCodeColumn = resolveColumn(columns, "device_code");
    String timeColumn = resolveColumn(columns, "operation_time", "create_time");
    String groupSelect = operationModuleColumn + ", " + exceptionClassColumn + ", " + errorCodeColumn
            + ", COUNT(*) AS cluster_count"
            + ", COUNT(DISTINCT " + traceIdColumn + ") AS distinct_trace_count"
            + ", COUNT(DISTINCT " + deviceCodeColumn + ") AS distinct_device_count"
            + ", MAX(" + timeColumn + ") AS latest_operation_time";
    String groupedSql = "SELECT " + groupSelect
            + " FROM " + TABLE_NAME
            + querySpec.whereClause()
            + " GROUP BY " + operationModuleColumn + ", " + exceptionClassColumn + ", " + errorCodeColumn
            + " ORDER BY cluster_count DESC, latest_operation_time DESC LIMIT ? OFFSET ?";
    List<Object> params = new ArrayList<>(querySpec.params());
    params.add(pageSize);
    params.add((Math.max(pageNum, 1) - 1L) * pageSize);
    List<SystemErrorClusterRowVO> rows = jdbcTemplate.query(groupedSql, (rs, rowNum) -> {
        SystemErrorClusterRowVO row = new SystemErrorClusterRowVO();
        row.setOperationModule(rs.getString(operationModuleColumn));
        row.setExceptionClass(rs.getString(exceptionClassColumn));
        row.setErrorCode(rs.getString(errorCodeColumn));
        row.setCount(rs.getLong("cluster_count"));
        row.setDistinctTraceCount(rs.getLong("distinct_trace_count"));
        row.setDistinctDeviceCount(rs.getLong("distinct_device_count"));
        row.setLatestOperationTime(formatTimestamp(rs.getTimestamp("latest_operation_time")));
        row.setClusterKey(String.join("|",
                defaultString(row.getOperationModule()),
                defaultString(row.getExceptionClass()),
                defaultString(row.getErrorCode())));
        return row;
    }, params.toArray());
    for (SystemErrorClusterRowVO row : rows) {
        fillClusterLatestPayload(querySpec, columns, row);
    }
    return PageResult.of(queryGroupedClusterCount(querySpec, columns), pageNum, pageSize, rows);
}
```

- [ ] **Step 4: Run the backend service test and a focused controller compile check**

Run: `mvn -pl spring-boot-iot-system -Dtest=AuditLogServiceImplTest test`

Expected: PASS with the new cluster query test and existing audit-log tests staying green.

- [ ] **Step 5: Commit the backend cluster endpoint**

```bash
git add \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/SystemErrorClusterRowVO.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/AuditLogService.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImpl.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/AuditLogController.java \
  spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/AuditLogServiceImplTest.java
git commit -m "feat: add system error cluster paging"
```

## Task 2: Wire Cluster API State Into the System-Log View

**Files:**
- Modify: `spring-boot-iot-ui/src/api/auditLog.ts`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Write the failing UI orchestration test**

```ts
import { pageSystemErrorClusters } from '@/api/auditLog';

vi.mock('@/api/auditLog', () => ({
  pageLogs: vi.fn(),
  pageSystemErrorClusters: vi.fn(),
  getAuditLogById: vi.fn(),
  deleteAuditLog: vi.fn(),
  getSystemErrorStats: vi.fn(),
  getBusinessAuditStats: vi.fn()
}));

it('loads system error clusters and filters detail rows by the selected cluster', async () => {
  vi.mocked(pageSystemErrorClusters).mockResolvedValueOnce({
    code: 200,
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [
        {
          clusterKey: 'PROTOCOL_DECODE|DecodeException|payload_invalid',
          operationModule: 'PROTOCOL_DECODE',
          exceptionClass: 'DecodeException',
          errorCode: 'payload_invalid',
          count: 12,
          distinctTraceCount: 4,
          distinctDeviceCount: 3,
          latestOperationTime: '2026-04-27 09:00:00',
          latestRequestUrl: '/mqtt/up',
          latestRequestMethod: 'MQTT',
          latestResultMessage: 'payload schema mismatch'
        }
      ]
    }
  } as any);

  mountView();
  await flushPromises();

  expect(pageSystemErrorClusters).toHaveBeenCalledTimes(1);
  expect(pageLogs).toHaveBeenLastCalledWith(expect.objectContaining({
    operationType: 'system_error',
    operationModule: 'PROTOCOL_DECODE',
    exceptionClass: 'DecodeException',
    errorCode: 'payload_invalid'
  }));
});
```

- [ ] **Step 2: Run the view test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts`

Expected: FAIL because `pageSystemErrorClusters` is missing and `AuditLogView.vue` does not manage cluster state yet.

- [ ] **Step 3: Add API types plus cluster-loading state in the view**

```ts
// spring-boot-iot-ui/src/api/auditLog.ts
export interface SystemErrorClusterRow {
  clusterKey: string;
  operationModule?: string;
  exceptionClass?: string;
  errorCode?: string;
  count: number;
  distinctTraceCount: number;
  distinctDeviceCount: number;
  latestOperationTime?: string;
  latestRequestUrl?: string;
  latestRequestMethod?: string;
  latestResultMessage?: string;
}

export function pageSystemErrorClusters(params: AuditLogQueryParams = {}): Promise<ApiEnvelope<PageResult<SystemErrorClusterRow>>> {
  const query = toQueryString(params);
  const path = `/api/system/audit-log/system-error/clusters/page${query ? `?${query}` : ''}`;
  return request<PageResult<SystemErrorClusterRow>>(path, { method: 'GET' });
}
```

```ts
// spring-boot-iot-ui/src/views/AuditLogView.vue
const clusterLoading = ref(false);
const clusterErrorMessage = ref('');
const clusterRows = ref<SystemErrorClusterRow[]>([]);
const selectedClusterKey = ref('');
const detailClusterMode = ref<'clustered' | 'all'>('clustered');

const selectedCluster = computed(() =>
  clusterRows.value.find((item) => item.clusterKey === selectedClusterKey.value) || null
);

const buildClusterQueryParams = (): AuditLogQueryParams => ({
  ...buildAuditLogQueryParams(),
  operationType: 'system_error'
});

const loadSystemErrorClusters = async () => {
  clusterLoading.value = true;
  clusterErrorMessage.value = '';
  try {
    const res = await pageSystemErrorClusters({ ...buildClusterQueryParams(), pageNum: 1, pageSize: 10 });
    clusterRows.value = applyPageResult<SystemErrorClusterRow>(res.data).records;
    selectedClusterKey.value = clusterRows.value[0]?.clusterKey || '';
    detailClusterMode.value = selectedClusterKey.value ? 'clustered' : 'all';
  } catch (error) {
    clusterRows.value = [];
    selectedClusterKey.value = '';
    detailClusterMode.value = 'all';
    clusterErrorMessage.value = error instanceof Error ? error.message : '加载异常概览失败';
  } finally {
    clusterLoading.value = false;
  }
};
```

- [ ] **Step 4: Make detail-table queries respect the selected cluster and keep the fallback path**

```ts
const buildAuditLogQueryParams = () => {
  const selected = detailClusterMode.value === 'clustered' ? selectedCluster.value : null;
  return {
    traceId: appliedFilters.traceId,
    operationType: 'system_error',
    operationModule: selected?.operationModule || appliedFilters.operationModule,
    exceptionClass: selected?.exceptionClass || appliedFilters.exceptionClass,
    errorCode: selected?.errorCode || appliedFilters.errorCode,
    deviceCode: appliedFilters.deviceCode,
    productKey: appliedFilters.productKey,
    requestMethod: appliedFilters.requestMethod,
    requestUrl: appliedFilters.requestUrl,
    operationResult: appliedFilters.operationResult
  };
};
```

- [ ] **Step 5: Run the view test and verify cluster fallback logic**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts`

Expected: PASS, including the new cluster-driven request assertions and the existing system-log tab tests.

## Task 3: Render the Dual-Table Error Workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts`

- [ ] **Step 1: Write the failing component test for dual-table rendering**

```ts
it('shows a cluster summary table above the linked detail table', async () => {
  const wrapper = mount(AuditLogErrorTabPanel, {
    props: {
      searchForm: createSearchForm(),
      quickSearchKeyword: '',
      showAdvancedFilters: false,
      advancedFilterHint: '',
      requestMethodOptions: [],
      appliedQuickSearchValue: '',
      activeFilterTags: [],
      hasAppliedFilters: false,
      showInlineState: false,
      inlineMessage: '',
      loading: false,
      tableData: [],
      pagination: { pageNum: 1, pageSize: 10, total: 0 },
      auditActionColumnWidth: 140,
      formatValue: (value: unknown) => String(value ?? '--'),
      getOperationResultName: () => '失败',
      getOperationResultTag: () => 'danger',
      getAuditDirectActions: () => [],
      clusterLoading: false,
      clusterErrorMessage: '',
      clusterRows: [
        {
          clusterKey: 'PROTOCOL_DECODE|DecodeException|payload_invalid',
          operationModule: 'PROTOCOL_DECODE',
          exceptionClass: 'DecodeException',
          errorCode: 'payload_invalid',
          count: 12,
          distinctTraceCount: 4,
          distinctDeviceCount: 3,
          latestOperationTime: '2026-04-27 09:00:00',
          latestRequestUrl: '/mqtt/up',
          latestResultMessage: 'payload schema mismatch'
        }
      ],
      selectedClusterKey: 'PROTOCOL_DECODE|DecodeException|payload_invalid',
      detailClusterMode: 'clustered'
    }
  });

  expect(wrapper.text()).toContain('异常概览');
  expect(wrapper.text()).toContain('关联异常明细');
  expect(wrapper.text()).toContain('查看全部异常明细');
});
```

- [ ] **Step 2: Run the component test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts`

Expected: FAIL because the component has no cluster props or summary table yet.

- [ ] **Step 3: Add the top cluster table, linked-detail header, and cluster actions**

```vue
<section class="audit-log-system-panel__cluster-stage">
  <header class="audit-log-system-panel__stage-header">
    <h3>异常概览</h3>
    <p>先定位最集中的异常簇，再下钻真实异常记录。</p>
  </header>
  <el-table
    :data="clusterRows"
    :row-class-name="({ row }) => row.clusterKey === selectedClusterKey ? 'is-selected' : ''"
    @row-click="emit('select-cluster', row.clusterKey)"
  >
    <StandardTableTextColumn prop="operationModule" label="异常模块" :min-width="160" />
    <StandardTableTextColumn prop="exceptionClass" label="异常类型" :min-width="180" />
    <StandardTableTextColumn prop="errorCode" label="异常编码" :min-width="140" />
    <el-table-column prop="count" label="发生次数" width="110" />
    <el-table-column prop="distinctDeviceCount" label="影响设备数" width="120" />
    <StandardTableTextColumn prop="latestOperationTime" label="最近发生时间" :min-width="180" />
  </el-table>
</section>

<header class="audit-log-system-panel__stage-header">
  <h3>关联异常明细</h3>
  <StandardButton
    v-if="detailClusterMode === 'clustered'"
    action="reset"
    text
    @click="emit('show-all-details')"
  >
    查看全部异常明细
  </StandardButton>
</header>
```

- [ ] **Step 4: Re-run the component test and the full system-log view test**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts src/__tests__/views/AuditLogView.test.ts`

Expected: PASS with the new component test and no regressions in the view suite.

- [ ] **Step 5: Commit the dual-table UI refactor**

```bash
git add \
  spring-boot-iot-ui/src/api/auditLog.ts \
  spring-boot-iot-ui/src/views/AuditLogView.vue \
  spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue \
  spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts \
  spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts
git commit -m "feat: add linked system-log error tables"
```

## Task 4: Remove Redundant Overview Strip and Finish End-to-End Verification

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Add the failing view assertion that the top overview strip is gone**

```ts
it('does not render the legacy system overview strip in system mode', async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.findComponent({ name: 'AuditLogSystemOverviewStrip' }).exists()).toBe(false);
  expect(wrapper.find('[data-testid="system-log-overview-errors"]').exists()).toBe(false);
});
```

- [ ] **Step 2: Run the view suite to confirm the legacy strip assertion fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts`

Expected: FAIL while the strip is still mounted in `AuditLogView.vue`.

- [ ] **Step 3: Remove the strip from the system-mode layout and keep tab-level counts only**

```vue
<div v-if="isSystemMode" class="audit-log-system-workbench">
  <StandardTableToolbar compact :meta-items="systemToolbarMetaItems">
    <template #right>
      <StandardButton action="refresh" link @click="handleSystemTabRefresh">刷新</StandardButton>
    </template>
  </StandardTableToolbar>

  <IotAccessTabWorkspace
    :model-value="activeSystemLogTab"
    :items="systemLogTabItems"
    default-key="errors"
    query-key="systemLogTab"
    :sync-query="false"
    @update:model-value="handleSystemLogTabChange"
  >
```

- [ ] **Step 4: Run focused frontend verification and one backend regression suite**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts src/__tests__/components/auditLog/AuditLogErrorTabPanel.test.ts`

Expected: PASS

Run: `mvn -pl spring-boot-iot-system -Dtest=AuditLogServiceImplTest test`

Expected: PASS

- [ ] **Step 5: Commit the system-log cleanup and verification result**

```bash
git add \
  spring-boot-iot-ui/src/views/AuditLogView.vue \
  spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "refactor: simplify system-log error workbench"
```

## Self-Review

- Spec coverage:
  - `异常概览表 + 关联异常明细表` is implemented by Task 2 and Task 3.
  - `新增真实聚合接口` is implemented by Task 1.
  - `聚合失败时回退到全量明细` is implemented by Task 2.
  - `移除重复导航式总览条` is implemented by Task 4.
- Placeholder scan:
  - No `TODO`, `TBD`, “handle later”, or “similar to previous task” placeholders remain.
- Type consistency:
  - Backend row type is consistently named `SystemErrorClusterRowVO`.
  - Frontend row type is consistently named `SystemErrorClusterRow`.
  - The UI selection key is consistently named `selectedClusterKey`.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-27-system-log-error-dual-table-implementation-plan.md`. Since the user already asked to begin execution in this session, proceed with **Inline Execution** after setting up an isolated worktree per `superpowers:using-git-worktrees`.
