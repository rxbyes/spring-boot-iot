# Message Trace Search And Observe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `/message-trace` 支持统一快速搜索 `TraceId / 设备编码 / 产品标识`，修复消息类型筛选失效，并让 `链路追踪` 模式隐藏正常链路上的 `观测` 动作。

**Architecture:** 后端先补 `keyword` 查询契约和 `messageType` 旧值归一，把快速搜索真正落到服务层 `OR` 查询上，再让前端 `MessageTraceView` 改为“一个快速搜索 + 更多筛选”的结构，并把 `观测` 从链路追踪模式移除。跨页诊断上下文继续沿用现有 `traceId / deviceCode / productKey / topic` 协议，不把新的 `keyword` 扩散到其他页面。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis Plus、JUnit 5、Mockito、Vue 3、Element Plus、Vitest、Markdown 文档

---

## File Structure

### Backend

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceMessageTraceQuery.java`
  - 为 `message-trace/page` 和 `message-trace/stats` 增加 `keyword` 查询字段。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java`
  - 在分页和统计查询中新增 `keyword` 的 `trace_id / device_code / product_key` 同组 `OR` 命中。
  - 统一把历史 `messageType=report` 归一为 `property`。
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImplTest.java`
  - 补 `keyword` 与 `messageType` 归一的服务层回归。

### Frontend

- Modify: `spring-boot-iot-ui/src/api/message.ts`
  - 在 `MessageTraceQueryParams` 里补 `keyword`。
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
  - 把快速搜索从 `traceId` 单字段切到 `keyword`。
  - 移除首屏 `设备编码 / 产品标识` 输入框，把 `消息类型 / Topic` 收到“更多筛选”。
  - `链路追踪` 行内动作只保留 `详情`，同步收口桌面和移动端操作列。
  - 删除链路追踪模式下不再使用的 `system-log` 跳转与观测上下文持久化逻辑。
- Test: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
  - 补快速搜索 `keyword`、真实消息类型过滤、隐藏 `观测` 的前端回归。

### Docs

- Modify: `docs/03-接口规范与接口清单.md`
  - 增加 `keyword` 参数和 `messageType` 兼容归一说明。
- Modify: `docs/06-前端开发与CSS规范.md`
  - 固化链路追踪筛选结构与动作语义。
- Modify: `docs/08-变更记录与技术债清单.md`
  - 记录本轮行为调整。
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
  - 更新链路追踪页的筛选与动作口径。
- Modify: `docs/15-前端优化与治理计划.md`
  - 记录“快速搜索优先、异常动作不回流正常链路”的治理规则。
- Inspect: `README.md`
  - 检查现有 `/message-trace` 摘要是否需要补一句“统一快速搜索”的最终口径；若原文已准确，可不改。

## Task 1: Backend Query Contract

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceMessageTraceQuery.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImplTest.java`

- [ ] **Step 1: Write the failing backend tests for `keyword` and `report -> property` normalization**

```java
@Test
void pageMessageTraceLogsShouldApplyKeywordAcrossTraceDeviceAndProduct() {
    DeviceMessageTraceQuery query = new DeviceMessageTraceQuery();
    query.setKeyword("demo-device-01");
    query.setMessageType("report");

    when(permissionService.getDataPermissionContext(99L))
            .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
    when(deviceMessageLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Page.class));

    deviceMessageService.pageMessageTraceLogs(99L, query, 1, 10);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<LambdaQueryWrapper<DeviceMessageLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    verify(deviceMessageLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());

    String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
    assertTrue(sqlSegment.contains("trace_id"));
    assertTrue(sqlSegment.contains("device_code"));
    assertTrue(sqlSegment.contains("product_key"));
    assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().containsValue("demo-device-01"));
    assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().containsValue("property"));
}

@Test
void getMessageTraceStatsShouldApplyKeywordAndNormalizeLegacyReportMessageType() {
    DeviceMessageTraceQuery query = new DeviceMessageTraceQuery();
    query.setKeyword("demo-device-01");
    query.setMessageType("report");

    List<String> executedSql = new ArrayList<>();
    List<Object[]> executedArgs = new ArrayList<>();

    when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
            .thenAnswer(invocation -> {
                executedSql.add(invocation.getArgument(0, String.class));
                executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                return 1L;
            });
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
            .thenAnswer(invocation -> {
                executedSql.add(invocation.getArgument(0, String.class));
                executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                return List.of();
            });

    deviceMessageService.getMessageTraceStats(99L, query);

    assertTrue(executedSql.stream().allMatch(sql -> sql.contains("(trace_id = ? OR device_code = ? OR product_key = ?)")));
    assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains("demo-device-01")));
    assertTrue(executedArgs.stream().anyMatch(args -> Arrays.asList(args).contains("property")));
}
```

- [ ] **Step 2: Run the backend tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceMessageServiceImplTest test
```

Expected:

```text
FAILURE
... cannot find symbol: method setKeyword(java.lang.String)
... expected true but was false
```

- [ ] **Step 3: Implement the minimal backend query contract**

```java
// spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceMessageTraceQuery.java
@Data
public class DeviceMessageTraceQuery {

    private String keyword;
    private String deviceCode;
    private String productKey;
    private String traceId;
    private String messageType;
    private String topic;
}
```

```java
// spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java
private LambdaQueryWrapper<DeviceMessageLog> buildMessageTraceQueryWrapper(DeviceMessageTraceQuery query, Long currentUserId) {
    LambdaQueryWrapper<DeviceMessageLog> queryWrapper = new LambdaQueryWrapper<>();
    Long tenantId = resolveScopedTenantId(currentUserId);
    if (tenantId != null) {
        queryWrapper.eq(DeviceMessageLog::getTenantId, tenantId);
    }
    applyMessageTraceDeviceScope(queryWrapper, currentUserId);
    if (query != null) {
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper.eq(DeviceMessageLog::getTraceId, keyword)
                    .or()
                    .eq(DeviceMessageLog::getDeviceCode, keyword)
                    .or()
                    .eq(DeviceMessageLog::getProductKey, keyword));
        }
        if (StringUtils.hasText(query.getDeviceCode())) {
            queryWrapper.eq(DeviceMessageLog::getDeviceCode, query.getDeviceCode().trim());
        }
        if (StringUtils.hasText(query.getProductKey())) {
            queryWrapper.eq(DeviceMessageLog::getProductKey, query.getProductKey().trim());
        }
        if (StringUtils.hasText(query.getTraceId())) {
            queryWrapper.eq(DeviceMessageLog::getTraceId, query.getTraceId().trim());
        }
        String normalizedMessageType = normalizeTraceMessageType(query.getMessageType());
        if (StringUtils.hasText(normalizedMessageType)) {
            queryWrapper.eq(DeviceMessageLog::getMessageType, normalizedMessageType);
        }
        if (StringUtils.hasText(query.getTopic())) {
            queryWrapper.like(DeviceMessageLog::getTopic, query.getTopic().trim());
        }
    }
    queryWrapper.orderByDesc(DeviceMessageLog::getReportTime)
            .orderByDesc(DeviceMessageLog::getCreateTime);
    return queryWrapper;
}

private QuerySpec buildMessageTraceQuerySpec(DeviceMessageTraceQuery query, Long currentUserId) {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> params = new ArrayList<>();
    Long tenantId = resolveScopedTenantId(currentUserId);
    if (tenantId != null) {
        where.append(" AND tenant_id = ?");
        params.add(tenantId);
    }
    appendAccessibleDeviceScope(where, params, currentUserId);
    if (query == null) {
        return new QuerySpec(where.toString(), params);
    }
    appendTraceKeyword(where, params, query.getKeyword());
    appendTextEquals(where, params, "device_code", query.getDeviceCode());
    appendTextEquals(where, params, "product_key", query.getProductKey());
    appendTextEquals(where, params, "trace_id", query.getTraceId());
    appendTextEquals(where, params, "message_type", normalizeTraceMessageType(query.getMessageType()));
    appendTextLike(where, params, "topic", query.getTopic());
    return new QuerySpec(where.toString(), params);
}

private void appendTraceKeyword(StringBuilder where, List<Object> params, String keyword) {
    if (!hasText(keyword)) {
        return;
    }
    String trimmedKeyword = keyword.trim();
    where.append(" AND (trace_id = ? OR device_code = ? OR product_key = ?)");
    params.add(trimmedKeyword);
    params.add(trimmedKeyword);
    params.add(trimmedKeyword);
}

private String normalizeTraceMessageType(String messageType) {
    if (!hasText(messageType)) {
        return null;
    }
    String normalized = messageType.trim();
    if ("report".equalsIgnoreCase(normalized)) {
        return "property";
    }
    return normalized;
}
```

- [ ] **Step 4: Run the backend tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceMessageServiceImplTest test
```

Expected:

```text
BUILD SUCCESS
... Tests run: ... Failures: 0
```

- [ ] **Step 5: Commit the backend contract change**

```bash
git add \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceMessageTraceQuery.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImplTest.java
git commit -m "fix: add keyword search to message trace queries"
```

## Task 2: Frontend Trace Filters And Row Actions

**Files:**
- Modify: `spring-boot-iot-ui/src/api/message.ts`
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: Write the failing frontend tests for quick search, real message types, and hidden observe**

```ts
it('submits quick search through keyword instead of traceId-only filtering', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  vi.mocked(messageApi.pageMessageTraceLogs).mockClear();
  vi.mocked(messageApi.pageMessageTraceStats).mockClear();

  const quickSearch = wrapper.get('input#quick-search');
  await quickSearch.setValue('demo-device-01');
  await quickSearch.trigger('keyup.enter');
  await flushPromises();

  expect(messageApi.pageMessageTraceLogs).toHaveBeenLastCalledWith(expect.objectContaining({
    keyword: 'demo-device-01',
    traceId: '',
    deviceCode: '',
    productKey: ''
  }));
  expect(wrapper.text()).toContain('快速搜索：demo-device-01');
});

it('uses real stored message types when restoring filters from route query', async () => {
  mockRoute.query = { messageType: 'status' };
  vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [
        {
          id: 1,
          traceId: 'trace-status-001',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product',
          messageType: 'status',
          topic: '/sys/demo-product/demo-device-01/thing/status/post',
          payload: '{"status":"online"}',
          reportTime: '2026-03-23 10:00:00',
          createTime: '2026-03-23 10:00:00'
        }
      ]
    }
  });

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(messageApi.pageMessageTraceLogs).toHaveBeenCalledWith(expect.objectContaining({
    messageType: 'status'
  }));
  expect(wrapper.text()).toContain('状态上报');
});

it('renders only detail actions in trace mode', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  const buttonTexts = wrapper.findAll('button').map((button) => button.text());
  expect(buttonTexts.some((text) => text.includes('详情'))).toBe(true);
  expect(buttonTexts.some((text) => text.includes('观测'))).toBe(false);
});
```

- [ ] **Step 2: Run the frontend test file to verify it fails**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/views/MessageTraceView.test.ts
```

Expected:

```text
FAIL
... expected "keyword" to be present
... expected false to be true
```

- [ ] **Step 3: Implement the minimal frontend view changes**

```ts
// spring-boot-iot-ui/src/api/message.ts
export interface MessageTraceQueryParams {
  keyword?: string;
  deviceCode?: string;
  productKey?: string;
  traceId?: string;
  messageType?: string;
  topic?: string;
  pageNum?: number;
  pageSize?: number;
}
```

```vue
<!-- spring-boot-iot-ui/src/views/MessageTraceView.vue -->
<el-input
  id="quick-search"
  v-model="quickSearchKeyword"
  placeholder="快速搜索（TraceId / 设备编码 / 产品标识）"
  clearable
  prefix-icon="Search"
  @keyup.enter="handleQuickSearch"
  @clear="handleClearQuickSearch"
/>
```

```vue
<!-- spring-boot-iot-ui/src/views/MessageTraceView.vue -->
<template #advanced>
  <el-form-item>
    <el-select v-model="searchForm.messageType" placeholder="消息类型" clearable>
      <el-option v-for="item in messageTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
  </el-form-item>
  <el-form-item>
    <el-input v-model="searchForm.topic" placeholder="Topic" clearable @keyup.enter="handleSearch" />
  </el-form-item>
</template>
```

```ts
// spring-boot-iot-ui/src/views/MessageTraceView.vue
const messageTraceActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [{ command: 'detail', label: '详情' }]
});

const messageTypeOptions = [
  { label: '属性上报', value: 'property' },
  { label: '事件上报', value: 'event' },
  { label: '状态上报', value: 'status' },
  { label: '命令回执', value: 'reply' },
  { label: '服务调用', value: 'service' }
];

const searchForm = reactive({
  keyword: '',
  deviceCode: '',
  productKey: '',
  traceId: '',
  messageType: '',
  topic: ''
});

const appliedFilters = reactive({
  keyword: '',
  deviceCode: '',
  productKey: '',
  traceId: '',
  messageType: '',
  topic: ''
});

function syncQuickSearchKeywordFromFilters() {
  quickSearchKeyword.value = searchForm.keyword;
}

function applyQuickSearchKeywordToFilters() {
  searchForm.keyword = quickSearchKeyword.value.trim();
}

function buildFilterQueryParams(): MessageTraceQueryParams {
  return {
    keyword: appliedFilters.keyword,
    deviceCode: appliedFilters.deviceCode,
    productKey: appliedFilters.productKey,
    traceId: appliedFilters.traceId,
    messageType: appliedFilters.messageType,
    topic: appliedFilters.topic
  };
}

function triggerSearch(resetPageFirst = false) {
  applyQuickSearchKeywordToFilters();
  syncAdvancedFilterState();
  syncAppliedFilters();
  appliedFilters.keyword = searchForm.keyword.trim();
  if (resetPageFirst) {
    resetPage();
  }
  loadTableData();
  loadTraceStats();
}

function getTraceDirectActions(_row: DeviceMessageLog) {
  return [{ command: 'detail', label: '详情' }];
}
```

```ts
// spring-boot-iot-ui/src/views/MessageTraceView.vue
const {
  tags: activeFilterTags,
  advancedAppliedCount,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'traceId', label: 'TraceId' },
    { key: 'deviceCode', label: '设备编码' },
    { key: 'productKey', label: '产品标识' },
    { key: 'messageType', label: (value) => `消息类型：${getMessageTypeLabel(value)}`, clearValue: '' },
    { key: 'topic', label: 'Topic', advanced: true }
  ],
  defaults: {
    keyword: '',
    deviceCode: '',
    productKey: '',
    traceId: '',
    messageType: '',
    topic: ''
  }
});

const hasAppliedFilters = computed(() =>
  Boolean(appliedFilters.keyword.trim()) || activeFilterTags.value.length > 0
);
```

- [ ] **Step 4: Remove trace-mode-only observability jump code and keep inbound context restoration**

```ts
// spring-boot-iot-ui/src/views/MessageTraceView.vue
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import {
  describeDiagnosticSource,
  resolveDiagnosticContext,
  type DiagnosticContext
} from '@/utils/iotAccessDiagnostics';

function handleTraceRowAction(command: string | number, row: DeviceMessageLog) {
  if (command === 'detail') {
    openDetail(row);
  }
}

function resetSearchForm() {
  searchForm.keyword = '';
  searchForm.deviceCode = '';
  searchForm.productKey = '';
  searchForm.traceId = '';
  searchForm.messageType = '';
  searchForm.topic = '';
  quickSearchKeyword.value = '';
  appliedFilters.keyword = '';
  showAdvancedFilters.value = false;
}
```

```vue
<!-- spring-boot-iot-ui/src/views/MessageTraceView.vue -->
<div v-if="appliedFilters.keyword.trim()" class="message-trace-quick-search-tag">
  <el-tag closable class="message-trace-quick-search-tag__chip" @close="handleClearQuickSearch">
    快速搜索：{{ appliedFilters.keyword.trim() }}
  </el-tag>
</div>
```

- [ ] **Step 5: Run the frontend tests and guard to verify the view is stable**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/views/MessageTraceView.test.ts
npm run list:guard
```

Expected:

```text
PASS src/__tests__/views/MessageTraceView.test.ts
... list page guard passed
```

- [ ] **Step 6: Commit the frontend message-trace UI change**

```bash
git add \
  spring-boot-iot-ui/src/api/message.ts \
  spring-boot-iot-ui/src/views/MessageTraceView.vue \
  spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "fix: streamline message trace search and actions"
```

## Task 3: Documentation Sync

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Inspect: `README.md`

- [ ] **Step 1: Update API and observability docs with the new query contract**

```md
<!-- docs/03-接口规范与接口清单.md -->
- `GET /api/device/message-trace/page` 与 `GET /api/device/message-trace/stats` 当前支持 `keyword`、`traceId`、`deviceCode`、`productKey`、`messageType`、`topic`、`pageNum`、`pageSize`。
- `keyword` 固定用于统一快速搜索 `trace_id / device_code / product_key` 三类标识，并按同组 `OR` 精确命中。
- `messageType` 当前真实值统一以 `property / event / status / reply / service` 为准；历史 `report` 查询值会在服务层自动归一为 `property`。
```

```md
<!-- docs/11-可观测性、日志追踪与消息通知治理.md -->
- 前端 `message-trace` 页面当前以“一个快速搜索 + 更多筛选”承接链路追踪：快速搜索统一命中 `TraceId / 设备编码 / 产品标识`，`消息类型` 与 `Topic` 收口到“更多筛选”。
- `链路追踪` 模式的行内动作当前固定只保留 `详情`；`观测` 继续只保留在 `失败归档` 模式，避免把正常链路误导成异常样本。
```

- [ ] **Step 2: Update frontend governance docs with the new page rules**

```md
<!-- docs/06-前端开发与CSS规范.md -->
- `/message-trace` 这类链路诊断台账，首屏必须优先使用统一快速搜索承接高频标识定位；`TraceId / 设备编码 / 产品标识` 不再与快速搜索并列重复摆放。
- `链路追踪` 与 `失败归档` 共路由时，异常动作只允许出现在异常模式；正常链路列表不得继续常驻 `观测`。
```

```md
<!-- docs/15-前端优化与治理计划.md -->
- `链路追踪台` 当前统一采用“快速搜索优先、更多筛选补充”的筛选结构；快速搜索固定服务 `TraceId / 设备编码 / 产品标识`。
- `观测` 一类异常动作不得回流到正常链路台账；后续若要在链路追踪里按行显示异常动作，必须基于明确的后端异常标记，而不是前端文案猜测。
```

- [ ] **Step 3: Record the shipped behavior change and inspect the README summary**

```md
<!-- docs/08-变更记录与技术债清单.md -->
- 2026-04-06：`/message-trace` 当前已补齐统一快速搜索和动作语义收口。链路追踪首屏改为一个快速搜索框统一命中 `TraceId / 设备编码 / 产品标识`，`消息类型 / Topic` 收口到“更多筛选”；前端与后端同步补齐 `keyword` 查询契约和 `report -> property` 兼容归一。与此同时，`链路追踪` 模式行内动作已收口为仅保留 `详情`，`观测` 继续只在 `失败归档` 中承接异常样本跳转。
```

```md
<!-- README.md：仅当现有 /message-trace 摘要未提到统一快速搜索时补这一句 -->
- `/message-trace` 当前已收口为 `链路追踪 / 失败归档` 同路由双模式；链路追踪首屏统一通过快速搜索命中 `TraceId / 设备编码 / 产品标识`，其余 `消息类型 / Topic` 条件按需收口到更多筛选。
```

- [ ] **Step 4: Review doc diffs and commit the documentation update**

Run:

```bash
git diff -- \
  docs/03-接口规范与接口清单.md \
  docs/06-前端开发与CSS规范.md \
  docs/08-变更记录与技术债清单.md \
  docs/11-可观测性、日志追踪与消息通知治理.md \
  docs/15-前端优化与治理计划.md \
  README.md
```

Expected:

```text
diff --git a/docs/03-接口规范与接口清单.md b/docs/03-接口规范与接口清单.md
... keyword / messageType compatibility wording
```

Then commit:

```bash
git add \
  docs/03-接口规范与接口清单.md \
  docs/06-前端开发与CSS规范.md \
  docs/08-变更记录与技术债清单.md \
  docs/11-可观测性、日志追踪与消息通知治理.md \
  docs/15-前端优化与治理计划.md \
  README.md
git commit -m "docs: update message trace search guidance"
```

## Task 4: Final Verification And Handoff

**Files:**
- Verify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java`
- Verify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Verify: `docs/03-接口规范与接口清单.md`
- Verify: `docs/11-可观测性、日志追踪与消息通知治理.md`

- [ ] **Step 1: Run the full targeted verification set**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceMessageServiceImplTest test
cd spring-boot-iot-ui
npm test -- src/__tests__/views/MessageTraceView.test.ts
npm run list:guard
cd ..
```

Expected:

```text
BUILD SUCCESS
PASS src/__tests__/views/MessageTraceView.test.ts
... list page guard passed
```

- [ ] **Step 2: Verify the working tree is clean after the task commits**

Run:

```bash
git status --short
```

Expected:

命令不输出任何改动行。

- [ ] **Step 3: Record the verification evidence for handoff**

```bash
git log --oneline -n 3
```

Expected:

```text
docs: update message trace search guidance
fix: streamline message trace search and actions
fix: add keyword search to message trace queries
```

## Self-Review

- Spec coverage:
  - `keyword` 查询契约：Task 1
  - `report -> property` 兼容：Task 1
  - 快速搜索与更多筛选结构：Task 2
  - 链路追踪隐藏 `观测`：Task 2
  - 测试与文档同步：Task 3、Task 4
- Placeholder scan:
  - 未使用 `TODO/TBD/implement later/similar to` 之类占位表述。
- Type consistency:
  - `keyword` 在 DTO、前端 API 类型、前端视图状态中保持同名。
  - `normalizeTraceMessageType` 在分页与统计查询中复用同一个归一逻辑。
