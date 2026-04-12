# Governance Task Search and Prioritization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable `/governance-task` to search by approval order / release batch / productKey / deviceCode / traceId, add task classifications including pending approval, and surface recommended-next-work behavior without breaking existing deep links.

**Architecture:** Extend the existing `GET /api/governance/work-items` query contract with `keyword` and `executionStatus`, implement keyword filtering and deterministic ordering in `GovernanceWorkItemServiceImpl`, then wire the same contract into `GovernanceTaskView.vue` with shared filter-header UI and query-backed category presets. Preserve the current single-page control-plane workbench, replay/decision drawers, and query-driven deep links while updating tests and docs in place.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vitest, JUnit 5, Mockito

---

## File Map

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\service\model\GovernanceWorkItemPageQuery.java`
  Responsibility: add `keyword` and `executionStatus` to the backend paging contract.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\service\impl\GovernanceWorkItemServiceImpl.java`
  Responsibility: apply keyword filtering, execution-status filtering, and deterministic default ordering for governance work items.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\controller\GovernanceWorkItemControllerTest.java`
  Responsibility: lock the new query fields into controller-to-service delegation.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\service\impl\GovernanceWorkItemServiceImplTest.java`
  Responsibility: cover backend keyword filtering and ordering rules.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
  Responsibility: expose `keyword` and `executionStatus` in the front-end query type.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
  Responsibility: add query-backed quick search, category presets, recommended view handling, and approval-first anchor text.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
  Responsibility: cover route query parsing, search interactions, category mapping, and anchor/summarization regressions.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
  Responsibility: document the `/governance-task` workbench behavior and recommended-next-work semantics.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
  Responsibility: document `keyword` and `executionStatus` for `GET /api/governance/work-items`.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
  Responsibility: record the shipped behavior and targeted verification commands.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\15-前端优化与治理计划.md`
  Responsibility: record the shared filter-header/category-strip pattern so governance-task does not drift back to private list controls.
- Inspect only: `E:\idea\ghatg\spring-boot-iot\README.md`
- Inspect only: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

Only touch the files above. The workspace already has unrelated dirty files; do not edit, stage, or revert them.

### Task 1: Extend the backend query contract

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\service\model\GovernanceWorkItemPageQuery.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\controller\GovernanceWorkItemControllerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\controller\GovernanceWorkItemControllerTest.java`

- [ ] **Step 1: Write the failing controller regression for `keyword` and `executionStatus`**

```java
@Test
void pageWorkItemsShouldDelegateKeywordAndExecutionStatusFilters() {
    GovernanceWorkItemPageQuery query = new GovernanceWorkItemPageQuery();
    query.setKeyword("2043187508765708289");
    query.setExecutionStatus("PENDING_APPROVAL");
    query.setWorkStatus("OPEN");
    query.setPageNum(1L);
    query.setPageSize(20L);

    when(governanceWorkItemService.pageWorkItems(query, 10001L))
            .thenReturn(PageResult.of(0L, 1L, 20L, List.of()));

    controller.pageWorkItems(query, authentication(10001L));

    verify(governanceWorkItemService).pageWorkItems(org.mockito.ArgumentMatchers.argThat(actual ->
            "2043187508765708289".equals(actual.getKeyword())
                    && "PENDING_APPROVAL".equals(actual.getExecutionStatus())
                    && "OPEN".equals(actual.getWorkStatus())
    ), org.mockito.ArgumentMatchers.eq(10001L));
}
```

- [ ] **Step 2: Run the controller test to verify the contract is currently missing**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceWorkItemControllerTest" test
```

Expected: FAIL or test compile failure because `GovernanceWorkItemPageQuery` does not yet expose `getKeyword()/setKeyword()` and `getExecutionStatus()/setExecutionStatus()`.

- [ ] **Step 3: Add the new query fields to `GovernanceWorkItemPageQuery`**

```java
@Data
public class GovernanceWorkItemPageQuery {

    private String workItemCode;
    private String workStatus;
    private String executionStatus;
    private String subjectType;
    private Long subjectId;
    private Long productId;
    private Long riskMetricId;
    private Long assigneeUserId;
    private String keyword;
    private Long pageNum;
    private Long pageSize;
}
```

- [ ] **Step 4: Re-run the controller test**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceWorkItemControllerTest" test
```

Expected: PASS with `Tests run:` including `GovernanceWorkItemControllerTest`.

- [ ] **Step 5: Commit the contract change**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceWorkItemPageQuery.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernanceWorkItemControllerTest.java
git commit -m "feat: extend governance work item page query"
```

### Task 2: Implement backend keyword filtering and priority-first ordering

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\service\impl\GovernanceWorkItemServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\service\impl\GovernanceWorkItemServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\service\impl\GovernanceWorkItemServiceImplTest.java`

- [ ] **Step 1: Write failing service tests for new filters and ordering**

```java
@Test
void pageWorkItemsShouldApplyKeywordAndExecutionStatusFilters() {
    when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
        Page<GovernanceWorkItem> page = invocation.getArgument(0);
        page.setRecords(List.of());
        page.setTotal(0);
        return page;
    });
    GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());
    GovernanceWorkItemPageQuery query = new GovernanceWorkItemPageQuery();
    query.setKeyword("2043187508765708289");
    query.setExecutionStatus("PENDING_APPROVAL");

    service.pageWorkItems(query, 10001L);

    org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GovernanceWorkItem>> wrapperCaptor =
            org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
    verify(workItemMapper).selectPage(any(), wrapperCaptor.capture());
    String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
    assertTrue(sqlSegment.contains("execution_status"));
    assertTrue(sqlSegment.contains("approval_order_id"));
    assertTrue(sqlSegment.contains("release_batch_id"));
    assertTrue(sqlSegment.contains("product_key"));
    assertTrue(sqlSegment.contains("device_code"));
    assertTrue(sqlSegment.contains("trace_id"));
}

@Test
void pageWorkItemsShouldBuildPriorityFirstOrdering() {
    when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
        Page<GovernanceWorkItem> page = invocation.getArgument(0);
        page.setRecords(List.of());
        page.setTotal(0);
        return page;
    });
    GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

    service.pageWorkItems(new GovernanceWorkItemPageQuery(), 10001L);

    org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GovernanceWorkItem>> wrapperCaptor =
            org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
    verify(workItemMapper).selectPage(any(), wrapperCaptor.capture());
    String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
    assertTrue(sqlSegment.contains("CASE work_status"));
    assertTrue(sqlSegment.contains("CASE priority_level"));
    assertTrue(sqlSegment.contains("update_time DESC"));
    assertTrue(sqlSegment.contains("id DESC"));
}
```

- [ ] **Step 2: Run the targeted service tests and confirm they fail**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceWorkItemServiceImplTest" test
```

Expected: FAIL because `buildPageWrapper(...)` only filters by the old fields and still orders by `createTime desc, id desc`.

- [ ] **Step 3: Implement keyword filtering and deterministic ordering in `GovernanceWorkItemServiceImpl`**

```java
private LambdaQueryWrapper<GovernanceWorkItem> buildPageWrapper(GovernanceWorkItemPageQuery query) {
    LambdaQueryWrapper<GovernanceWorkItem> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(GovernanceWorkItem::getDeleted, 0);
    if (query == null) {
        applyWorkItemOrdering(wrapper);
        return wrapper;
    }
    wrapper.eq(StringUtils.hasText(normalize(query.getWorkItemCode())), GovernanceWorkItem::getWorkItemCode, normalize(query.getWorkItemCode()));
    wrapper.eq(StringUtils.hasText(normalize(query.getWorkStatus())), GovernanceWorkItem::getWorkStatus, normalize(query.getWorkStatus()));
    wrapper.eq(StringUtils.hasText(normalize(query.getExecutionStatus())), GovernanceWorkItem::getExecutionStatus, normalize(query.getExecutionStatus()));
    wrapper.eq(StringUtils.hasText(normalize(query.getSubjectType())), GovernanceWorkItem::getSubjectType, normalize(query.getSubjectType()));
    wrapper.eq(query.getSubjectId() != null, GovernanceWorkItem::getSubjectId, query.getSubjectId());
    wrapper.eq(query.getProductId() != null, GovernanceWorkItem::getProductId, query.getProductId());
    wrapper.eq(query.getRiskMetricId() != null, GovernanceWorkItem::getRiskMetricId, query.getRiskMetricId());
    wrapper.eq(query.getAssigneeUserId() != null, GovernanceWorkItem::getAssigneeUserId, query.getAssigneeUserId());
    applyKeywordFilter(wrapper, normalize(query.getKeyword()));
    applyWorkItemOrdering(wrapper);
    return wrapper;
}

private void applyKeywordFilter(LambdaQueryWrapper<GovernanceWorkItem> wrapper, String keyword) {
    if (!StringUtils.hasText(keyword)) {
        return;
    }
    Long exactLong = parseExactLongKeyword(keyword);
    wrapper.and(nested -> {
        if (exactLong != null) {
            nested.eq(GovernanceWorkItem::getApprovalOrderId, exactLong)
                    .or()
                    .eq(GovernanceWorkItem::getReleaseBatchId, exactLong)
                    .or();
        }
        nested.eq(GovernanceWorkItem::getProductKey, keyword)
                .or()
                .eq(GovernanceWorkItem::getDeviceCode, keyword)
                .or()
                .eq(GovernanceWorkItem::getTraceId, keyword)
                .or()
                .like(GovernanceWorkItem::getProductKey, keyword)
                .or()
                .like(GovernanceWorkItem::getDeviceCode, keyword)
                .or()
                .like(GovernanceWorkItem::getTraceId, keyword);
    });
}

private Long parseExactLongKeyword(String keyword) {
    if (!StringUtils.hasText(keyword) || !keyword.chars().allMatch(Character::isDigit)) {
        return null;
    }
    try {
        return Long.parseLong(keyword);
    } catch (NumberFormatException ex) {
        return null;
    }
}

private void applyWorkItemOrdering(LambdaQueryWrapper<GovernanceWorkItem> wrapper) {
    wrapper.last("ORDER BY CASE work_status "
            + "WHEN 'OPEN' THEN 0 "
            + "WHEN 'ACKED' THEN 1 "
            + "WHEN 'BLOCKED' THEN 2 "
            + "WHEN 'RESOLVED' THEN 3 "
            + "WHEN 'CLOSED' THEN 4 ELSE 9 END, "
            + "CASE priority_level "
            + "WHEN 'P1' THEN 0 "
            + "WHEN 'P2' THEN 1 "
            + "WHEN 'P3' THEN 2 ELSE 9 END, "
            + "update_time DESC, id DESC");
}
```

- [ ] **Step 4: Re-run the targeted service tests**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceWorkItemServiceImplTest" test
```

Expected: PASS with the new assertions covering `execution_status`, the five-field keyword search, and the priority-first order clause.

- [ ] **Step 5: Commit the backend behavior**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImplTest.java
git commit -m "feat: add governance work item search filters"
```

### Task 3: Wire front-end query types and route-backed filter state

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Add failing Vitest coverage for route-query parsing and route updates**

```ts
it('loads governance task work items with keyword and executionStatus from route query', async () => {
  mockRoute.query = {
    keyword: '2043187508765708289',
    executionStatus: 'PENDING_APPROVAL',
    workStatus: 'OPEN'
  }
  mockPageWorkItems.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
  })

  mountWithStubs(GovernanceTaskView)
  await flushPromises()

  expect(mockPageWorkItems).toHaveBeenCalledWith(expect.objectContaining({
    keyword: '2043187508765708289',
    executionStatus: 'PENDING_APPROVAL',
    workStatus: 'OPEN'
  }))
})

it('writes quick-search keywords back into the route query', async () => {
  mockPageWorkItems.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
  })

  const wrapper = mountWithStubs(GovernanceTaskView)
  await flushPromises()

  const input = wrapper.get('input[placeholder*="审批单号"]')
  await input.setValue('2043187508765708289')
  await input.trigger('keyup.enter')

  expect(mockRouter.replace).toHaveBeenCalledWith({
    query: expect.objectContaining({
      keyword: '2043187508765708289',
      workStatus: 'OPEN'
    })
  })
})
```

- [ ] **Step 2: Run the front-end governance control-plane test file and confirm the new cases fail**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: FAIL because `GovernanceTaskView.vue` does not yet read `keyword`/`executionStatus`, and the current test harness does not expose `router.replace`-driven filter interactions.

- [ ] **Step 3: Implement the front-end query type and route-backed filter state**

```ts
export interface GovernanceWorkItemPageQuery {
  workItemCode?: string | null;
  workStatus?: GovernanceWorkItemStatus | string | null;
  executionStatus?: GovernanceWorkItemExecutionStatus | string | null;
  subjectType?: string | null;
  subjectId?: IdType | null;
  productId?: IdType | null;
  riskMetricId?: IdType | null;
  assigneeUserId?: IdType | null;
  keyword?: string | null;
  pageNum?: number;
  pageSize?: number;
}
```

```ts
const filters = reactive({
  keyword: '',
  workStatus: 'OPEN',
  executionStatus: ''
})
const activeView = ref('all')

watch(
  () => route.query,
  () => {
    syncFiltersFromRoute()
    void loadWorkItems()
  },
  { immediate: true }
)

function syncFiltersFromRoute() {
  filters.keyword = parseStringQuery(route.query.keyword) || ''
  filters.workStatus = parseStringQuery(route.query.workStatus) || 'OPEN'
  filters.executionStatus = parseStringQuery(route.query.executionStatus) || ''
  activeView.value = parseStringQuery(route.query.view) || 'all'
}

function buildQueryFromRoute(): GovernanceWorkItemPageQuery {
  return {
    workItemCode: parseStringQuery(route.query.workItemCode),
    workStatus: parseStringQuery(route.query.workStatus) || 'OPEN',
    executionStatus: parseStringQuery(route.query.executionStatus),
    subjectType: parseStringQuery(route.query.subjectType),
    subjectId: parseIdQuery(route.query.subjectId),
    productId: parseIdQuery(route.query.productId),
    riskMetricId: parseIdQuery(route.query.riskMetricId),
    assigneeUserId: parseIdQuery(route.query.assigneeUserId),
    keyword: parseStringQuery(route.query.keyword),
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

async function replaceTaskRouteQuery(nextQuery: Record<string, string | undefined>) {
  await router.replace({
    query: Object.fromEntries(
      Object.entries(nextQuery).filter(([, value]) => Boolean(value))
    )
  })
}
```

Also update the test harness in `GovernanceControlPlaneViews.test.ts` so `mockRouter` includes `replace: vi.fn()`, and add simple stubs for `StandardListFilterHeader` plus the input used by the quick-search field.

```ts
const mockRouter = {
  push: vi.fn(),
  replace: vi.fn()
}

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="standard-list-filter-header-stub">
      <slot name="primary" />
      <slot name="actions" />
    </section>
  `
})
```

- [ ] **Step 4: Re-run the front-end test file**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: PASS for the new route-query coverage, while keeping the existing replay/dispatch tests green.

- [ ] **Step 5: Commit the route-backed filter state**

```bash
git add spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/views/GovernanceTaskView.vue spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts
git commit -m "feat: wire governance task route filters"
```

### Task 4: Add quick-search UI, category presets, recommended view, and anchor prioritization

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Add failing UI regressions for pending-approval preset, recommended summary, and approval-first anchors**

```ts
it('maps the pending approval preset to executionStatus=PENDING_APPROVAL', async () => {
  mockPageWorkItems.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
  })

  const wrapper = mountWithStubs(GovernanceTaskView)
  await flushPromises()

  const pendingApprovalButton = wrapper.findAll('button').find((button) => button.text() === '待审批')
  expect(pendingApprovalButton).toBeTruthy()
  await pendingApprovalButton!.trigger('click')

  expect(mockRouter.replace).toHaveBeenCalledWith({
    query: expect.objectContaining({
      executionStatus: 'PENDING_APPROVAL',
      workStatus: 'OPEN'
    })
  })
})

it('renders approvalOrderId ahead of productKey in the task anchor and shows recommended counts', async () => {
  mockRoute.query = { view: 'recommended', workStatus: 'OPEN' }
  mockPageWorkItems.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 2,
      pageNum: 1,
      pageSize: 10,
      records: [
        {
          id: 1,
          workItemCode: 'PENDING_CONTRACT_RELEASE',
          workStatus: 'OPEN',
          priorityLevel: 'P1',
          approvalOrderId: 2043187508765708289,
          productKey: 'phase2-gnss',
          recommendation: { suggestedAction: '去审批' }
        },
        {
          id: 2,
          workItemCode: 'PENDING_RISK_BINDING',
          workStatus: 'OPEN',
          priorityLevel: 'P3',
          productKey: 'phase1-crack'
        }
      ]
    }
  })

  const wrapper = mountWithStubs(GovernanceTaskView)
  await flushPromises()

  expect(wrapper.text()).toContain('2043187508765708289')
  expect(wrapper.text()).toContain('推荐先处理 1 项')
})
```

- [ ] **Step 2: Run the front-end test file and confirm the UI cases fail**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: FAIL because the template does not yet expose category buttons, the summary does not calculate recommended counts, and `workItemAnchor(...)` still prefers `productKey`.

- [ ] **Step 3: Implement the filter header, preset buttons, recommended summary, and anchor precedence**

```vue
<StandardWorkbenchPanel
  title="治理任务台"
  :description="`统一查看合同发布、风险绑定等治理待办，当前共 ${pagination.total} 项。`"
  show-filters
  show-toolbar
  show-pagination
>
  <template #filters>
    <StandardListFilterHeader :model="filters">
      <template #primary>
        <label class="governance-task-filter-field">
          <span>快速搜索</span>
          <input
            v-model="filters.keyword"
            class="governance-task-filter-input"
            placeholder="快速搜索（审批单号、发布批次号、产品标识、设备编码、TraceId）"
            @keyup.enter="handleSearch"
          />
        </label>
        <label class="governance-task-filter-field">
          <span>工作状态</span>
          <select v-model="filters.workStatus" class="governance-task-filter-input">
            <option value="OPEN">待处理</option>
            <option value="ACKED">已确认</option>
            <option value="BLOCKED">已阻塞</option>
            <option value="RESOLVED">已解决</option>
            <option value="CLOSED">已关闭</option>
          </select>
        </label>
      </template>
      <template #actions>
        <StandardButton action="query" @click="handleSearch">查询</StandardButton>
        <StandardButton action="reset" @click="handleReset">重置</StandardButton>
      </template>
    </StandardListFilterHeader>
    <div class="governance-task-category-strip">
      <StandardButton
        v-for="preset in taskPresets"
        :key="preset.key"
        @click="handleSelectPreset(preset.key)"
      >
        {{ preset.label }}
      </StandardButton>
    </div>
  </template>
</StandardWorkbenchPanel>
```

```ts
const taskPresets = [
  { key: 'all', label: '全部' },
  { key: 'recommended', label: '推荐优先处理' },
  { key: 'pending-approval', label: '待审批' },
  { key: 'contract-release', label: '待发布合同' },
  { key: 'risk-binding', label: '待绑定风险点' },
  { key: 'threshold-policy', label: '待补阈值' },
  { key: 'linkage-plan', label: '待补联动/预案' },
  { key: 'replay', label: '待运营复盘' }
] as const

const displayTaskList = computed(() =>
  activeView.value === 'recommended'
    ? [...taskList.value].sort(compareRecommendedWorkItems)
    : taskList.value
)
const recommendedCount = computed(() =>
  taskList.value.filter((item) => item.workStatus === 'OPEN'
    && ['P1', 'P2'].includes(item.priorityLevel || '')
    && Boolean(normalizeText(item.recommendation?.suggestedAction))).length
)

function compareRecommendedWorkItems(left: GovernanceWorkItem, right: GovernanceWorkItem) {
  const score = (item: GovernanceWorkItem) => {
    const openScore = item.workStatus === 'OPEN' ? 100 : 0
    const priorityScore = item.priorityLevel === 'P1' ? 30 : item.priorityLevel === 'P2' ? 20 : 0
    const suggestionScore = normalizeText(item.recommendation?.suggestedAction) ? 10 : 0
    const executionScore = ['PENDING_APPROVAL', 'IN_PROGRESS'].includes(item.executionStatus || '') ? 5 : 0
    return openScore + priorityScore + suggestionScore + executionScore
  }
  return score(right) - score(left)
}

function preserveContextQuery() {
  return {
    productId: parseStringQuery(route.query.productId),
    subjectType: parseStringQuery(route.query.subjectType),
    subjectId: parseStringQuery(route.query.subjectId),
    riskMetricId: parseStringQuery(route.query.riskMetricId),
    assigneeUserId: parseStringQuery(route.query.assigneeUserId)
  }
}

function buildPresetQuery(key: string) {
  switch (key) {
    case 'recommended':
      return { view: 'recommended', workItemCode: undefined, executionStatus: undefined }
    case 'pending-approval':
      return { view: undefined, workItemCode: undefined, executionStatus: 'PENDING_APPROVAL' }
    case 'contract-release':
      return { view: undefined, workItemCode: 'PENDING_CONTRACT_RELEASE', executionStatus: undefined }
    case 'risk-binding':
      return { view: undefined, workItemCode: 'PENDING_RISK_BINDING', executionStatus: undefined }
    case 'threshold-policy':
      return { view: undefined, workItemCode: 'PENDING_THRESHOLD_POLICY', executionStatus: undefined }
    case 'linkage-plan':
      return { view: undefined, workItemCode: 'PENDING_LINKAGE_PLAN', executionStatus: undefined }
    case 'replay':
      return { view: undefined, workItemCode: 'PENDING_REPLAY', executionStatus: undefined }
    default:
      return { view: undefined, workItemCode: undefined, executionStatus: undefined }
  }
}

function workItemAnchor(item: GovernanceWorkItem) {
  return item.approvalOrderId != null
    ? String(item.approvalOrderId)
    : item.releaseBatchId != null
      ? String(item.releaseBatchId)
      : item.productKey
        || item.deviceCode
        || item.traceId
        || snapshotValue(item.snapshotJson, 'productKey')
        || snapshotValue(item.snapshotJson, 'deviceCode')
        || snapshotValue(item.snapshotJson, 'dimensionLabel')
        || snapshotValue(item.snapshotJson, 'metricIdentifier')
        || '--'
}

function handleSelectPreset(key: string) {
  const presetQuery = buildPresetQuery(key)
  void replaceTaskRouteQuery({
    ...preserveContextQuery(),
    ...presetQuery,
    keyword: normalizeText(filters.keyword),
    workStatus: filters.workStatus || 'OPEN',
    pageNum: '1',
    pageSize: String(pagination.pageSize)
  })
}
```

Render `displayTaskList` instead of `taskList` in the template, and update the summary card to include `推荐先处理 {{ recommendedCount }} 项`.

- [ ] **Step 4: Re-run the front-end governance control-plane tests**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: PASS with the new preset, summary, and anchor expectations green.

- [ ] **Step 5: Commit the governance-task UI refinement**

```bash
git add spring-boot-iot-ui/src/views/GovernanceTaskView.vue spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts
git commit -m "feat: add governance task quick search"
```

### Task 5: Update docs and run focused verification

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\15-前端优化与治理计划.md`
- Inspect: `E:\idea\ghatg\spring-boot-iot\README.md`
- Inspect: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: Confirm the current docs do not yet describe the new search/filter behavior**

Run:

```bash
Select-String -Path 'docs/02-业务功能与流程说明.md','docs/03-接口规范与接口清单.md','docs/08-变更记录与技术债清单.md','docs/15-前端优化与治理计划.md' -Pattern 'executionStatus|keyword|推荐优先处理|待审批'
```

Expected: partial or missing coverage for `/governance-task`, proving the docs need to be updated in place.

- [ ] **Step 2: Update the docs with the shipped behavior**

```md
<!-- docs/02-业务功能与流程说明.md -->
- `2026-04-12` 起，`/governance-task` 新增统一快速搜索与分类收口：支持按 `审批单号 / 发布批次号 / 产品标识 / 设备编码 / TraceId` 一框查询，并新增 `全部 / 推荐优先处理 / 待审批 / 待发布合同 / 待绑定风险点 / 待补阈值 / 待补联动预案 / 待运营复盘` 快捷视角；其中 `待审批` 直接按 `executionStatus=PENDING_APPROVAL` 收口，首屏还会直接回答“推荐先处理几条 P1/P2 任务”。

<!-- docs/03-接口规范与接口清单.md -->
- `GET /api/governance/work-items` 当前额外支持 `keyword / executionStatus`。`keyword` 统一匹配 `approvalOrderId / releaseBatchId / productKey / deviceCode / traceId`，其中审批单号与发布批次号按精确匹配，字符串类上下文先按等值命中，再按模糊查询兜底。

<!-- docs/08-变更记录与技术债清单.md -->
- 2026-04-12：治理任务台补齐统一搜索与推荐优先处理。`spring-boot-iot-system` 为 `/api/governance/work-items` 新增 `keyword / executionStatus` 查询能力，并把默认排序切到 `OPEN -> priorityLevel -> updateTime -> id`；`spring-boot-iot-ui` 则把 `/governance-task` 改造成共享筛选头 + 分类快捷入口工作台，支持直接搜索审批单 `2043187508765708289`、按 `待审批` 收口和首屏推荐下一步任务。定向验证：`mvn -pl spring-boot-iot-system -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceWorkItemControllerTest,GovernanceWorkItemServiceImplTest" test`、`npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`。

<!-- docs/15-前端优化与治理计划.md -->
- `/governance-task` 的快速搜索与分类入口必须继续复用 `StandardListFilterHeader + StandardButton` 的共享工作台结构，不得回流页面私有 pill、二级导航条或分页外悬浮筛选；推荐优先处理、待审批等视角统一通过 URL query 驱动。
```

- [ ] **Step 3: Run focused verification plus whitespace checks**

Run:

```bash
mvn -pl spring-boot-iot-system -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceWorkItemControllerTest,GovernanceWorkItemServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
git diff --check
```

Expected: both targeted test commands PASS and `git diff --check` returns no output.

- [ ] **Step 4: Inspect `README.md` and `AGENTS.md`, then record whether they stay unchanged**

Run:

```bash
Select-String -Path 'README.md','AGENTS.md' -Pattern 'governance-task|governance/work-items|待审批|推荐优先处理'
```

Expected: no required edits for this change set. If you discover a hard requirement there, update the relevant file in the same commit; otherwise explicitly note “README.md and AGENTS.md checked, no change required” in the final summary.

- [ ] **Step 5: Commit the docs and verification outcome**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: document governance task search workflow"
```
