# Risk Point Organization And Auto-Code Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让风险对象中心改为“所属组织”主语义，新增风险点时由后端自动生成固定编号，并恢复新增、编辑、绑定设备、删除主链路的可用性和可见反馈。

**Architecture:** 后端在 `risk_point` 新增 `org_id`、`org_name`，并由 `RiskPointServiceImpl` 统一校验组织、生成风险点编号、回填组织名称；前端 `RiskPointView.vue` 改为组织树选择和只读编号展示，同时补齐标准列表骨架、动作列合同与错误提示。实现时不要覆盖当前工作区里未提交的风险策略列表合同改动，而是把本轮功能叠加到现有页面治理方向上。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis-Plus、Vue 3、TypeScript、Vitest、Element Plus、现有共享工作台组件。

---

## File Map

- Modify: `sql/init.sql`
  - 给 `risk_point` 增加 `org_id`、`org_name` 列。
- Modify: `sql/init-data.sql`
  - 初始化风险点样例补齐所属组织字段。
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java`
  - 增加 `orgId`、`orgName`。
- Modify: `spring-boot-iot-alarm/pom.xml`
  - 给 `alarm` 模块补齐对 `spring-boot-iot-system` 的依赖，允许风险点服务复用组织主数据。
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
  - 注入组织服务、生成自动编号、校验组织、修复删除/绑定错误路径。
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`
  - 覆盖自动编号、固定编号、组织校验和重复绑定。
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
  - 补齐 `orgId`、`orgName` 前端类型。
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
  - 改成组织树下拉、编号只读、标准列表骨架、`max-direct-items=3` 和显式错误提示。
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
  - 锁定分页、加载骨架、组织字段和自动编号表单合同。
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskStrategyListContract.test.ts`
  - 锁定风险对象中心的共享列表和三直出动作合同。
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Check: `README.md`
- Check: `AGENTS.md`

### Task 1: Lock The Backend Contract With Failing Risk-Point Service Tests

**Files:**
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`

- [ ] **Step 1: Write the failing backend tests**

```java
package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.service.OrganizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class RiskPointServiceImplTest {

    @Test
    void addRiskPointShouldGenerateCodeFromOrganizationAndRiskLevel() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        Organization organization = new Organization();
        organization.setId(7101L);
        organization.setOrgName("ops-center");
        organization.setStatus(1);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRiskLevel("critical");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(null).doReturn(existingRiskPoint("RP-OPSCEN-NORTHS-CRIT-001")).when(service).getOne(any());
        doAnswer(invocation -> {
            RiskPoint saved = invocation.getArgument(0);
            saved.setId(9001L);
            return true;
        }).when(service).save(any(RiskPoint.class));

        RiskPoint saved = service.addRiskPoint(input);

        assertEquals("ops-center", saved.getOrgName());
        assertEquals("critical", saved.getRiskLevel());
        assertEquals("RP-OPSCEN-NORTHS-CRIT-002", saved.getRiskPointCode());
        assertEquals(0, saved.getDeleted());
    }

    @Test
    void updateRiskPointShouldKeepExistingCodeWhileRefreshingOrganizationName() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        RiskPoint existing = existingRiskPoint("RP-OLD-001");
        existing.setId(12L);
        existing.setOrgId(7101L);
        existing.setOrgName("legacy-org");

        Organization organization = new Organization();
        organization.setId(7102L);
        organization.setOrgName("alarm-team");
        organization.setStatus(1);

        RiskPoint update = new RiskPoint();
        update.setId(12L);
        update.setRiskPointName("north-slope");
        update.setOrgId(7102L);
        update.setRiskLevel("warning");

        doReturn(existing).when(service).getById(12L);
        doReturn(null).when(service).getOne(any());
        doReturn(organization).when(organizationService).getById(7102L);

        assertDoesNotThrow(() -> service.updateRiskPoint(update));
        assertEquals("RP-OLD-001", update.getRiskPointCode());
        assertEquals("alarm-team", update.getOrgName());
    }

    @Test
    void addRiskPointShouldRejectDisabledOrganization() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        Organization organization = new Organization();
        organization.setId(7101L);
        organization.setOrgName("ops-center");
        organization.setStatus(0);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRiskLevel("critical");

        doReturn(organization).when(organizationService).getById(7101L);

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input));
        assertEquals("所属组织已停用", error.getMessage());
    }

    private RiskPoint existingRiskPoint(String code) {
        RiskPoint riskPoint = new RiskPoint();
        riskPoint.setRiskPointCode(code);
        riskPoint.setDeleted(0);
        return riskPoint;
    }
}
```

- [ ] **Step 2: Run backend tests to verify they fail**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest test`
Expected: FAIL with constructor mismatch and missing `orgId`/`orgName` or auto-code assertions.

- [ ] **Step 3: Add one more failing duplicate-binding test before implementation**

```java
@Test
void bindDeviceShouldRejectDuplicateMetricBinding() {
    RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
    OrganizationService organizationService = mock(OrganizationService.class);
    RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

    RiskPointDevice existing = new RiskPointDevice();
    existing.setRiskPointId(12L);
    existing.setDeviceId(2001L);
    existing.setMetricIdentifier("temperature");
    existing.setDeleted(0);

    RiskPointDevice request = new RiskPointDevice();
    request.setRiskPointId(12L);
    request.setDeviceId(2001L);
    request.setMetricIdentifier("temperature");

    doReturn(existing).when(deviceMapper).selectOne(any());

    BizException error = assertThrows(BizException.class, () -> service.bindDevice(request));
    assertEquals("设备已绑定到该风险点", error.getMessage());
}
```

- [ ] **Step 4: Re-run backend tests to confirm the whole contract is red**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest test`
Expected: FAIL, with the duplicate-binding assertion green only after service logic is implemented.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java
git commit -m "test: lock risk point organization and auto-code contract"
```

### Task 2: Implement The Backend Schema, Entity, And Service Logic

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-alarm/pom.xml`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

- [ ] **Step 1: Add the schema columns and seed values**

```sql
ALTER TABLE risk_point
    ADD COLUMN org_id BIGINT DEFAULT NULL COMMENT '所属组织ID' AFTER risk_point_name,
    ADD COLUMN org_name VARCHAR(128) DEFAULT NULL COMMENT '所属组织名称' AFTER org_id;
```

Use the actual `sql/init.sql` create-table block instead of a standalone migration and update the insert seed to:

```sql
INSERT INTO risk_point (
    id, risk_point_code, risk_point_name, org_id, org_name, region_id, region_name, responsible_user, responsible_phone,
    risk_level, description, status, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8001, 'RP-HP-001', '锅炉温压监测点', 7101, '平台运维中心', 7002, '黄浦厂区', 1, '13800000000', 'critical', '锅炉区高温高压风险监测', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8002, 'RP-HP-002', '振动监测点', 7102, '告警处置组', 7002, '黄浦厂区', 1, '13800000000', 'warning', '关键设备振动风险监测', 0, 1, 1, NOW(), 1, NOW(), 0)
```

- [ ] **Step 2: Add the new entity fields**

```java
private Long orgId;

private String orgName;
```

Insert them in `RiskPoint.java` between `riskPointName` and `regionId` so the model matches the table order and future JSON payloads.

- [ ] **Step 3: Implement organization resolution and auto-code generation**

Add these members to `RiskPointServiceImpl.java`:

```java
private final RiskPointDeviceMapper riskPointDeviceMapper;
private final OrganizationService organizationService;

public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper,
                            OrganizationService organizationService) {
    this.riskPointDeviceMapper = riskPointDeviceMapper;
    this.organizationService = organizationService;
}

private Organization resolveRequiredOrganization(Long orgId) {
    if (orgId == null || orgId <= 0) {
        throw new BizException("请选择所属组织");
    }
    Organization organization = organizationService.getById(orgId);
    if (organization == null || organization.getDeleted() == 1) {
        throw new BizException("所属组织不存在");
    }
    if (!Integer.valueOf(1).equals(organization.getStatus())) {
        throw new BizException("所属组织已停用");
    }
    return organization;
}

private String generateRiskPointCode(String riskPointName, String orgName, String riskLevel) {
    String orgSegment = buildCodeSegment(orgName, 6);
    String nameSegment = buildCodeSegment(riskPointName, 6);
    String levelSegment = buildCodeSegment(riskLevel, 4);
    String base = "RP-" + orgSegment + "-" + nameSegment + "-" + levelSegment;
    int suffix = 1;
    while (existsActiveCode(base + "-" + String.format("%03d", suffix))) {
        suffix++;
    }
    return base + "-" + String.format("%03d", suffix);
}

private boolean existsActiveCode(String code) {
    return lambdaQuery()
            .eq(RiskPoint::getRiskPointCode, code)
            .eq(RiskPoint::getDeleted, 0)
            .count() > 0;
}

private String buildCodeSegment(String source, int maxLength) {
    if (source == null || source.isBlank()) {
        return "GEN";
    }
    String normalized = source.replaceAll("[^A-Za-z0-9]+", "").toUpperCase();
    if (normalized.isBlank()) {
        normalized = "CN" + Integer.toHexString(Math.abs(source.hashCode())).toUpperCase();
    }
    if (normalized.isBlank()) {
        return "GEN";
    }
    return normalized.substring(0, Math.min(normalized.length(), maxLength));
}
```

- [ ] **Step 4: Apply the new add/update logic**

Use this shape in `addRiskPoint` and `updateRiskPoint`:

```java
Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
riskPoint.setOrgName(organization.getOrgName());

if (riskPoint.getId() == null) {
    riskPoint.setRiskPointCode(generateRiskPointCode(
            riskPoint.getRiskPointName(),
            organization.getOrgName(),
            riskPoint.getRiskLevel()
    ));
} else {
    RiskPoint existing = getById(riskPoint.getId());
    riskPoint.setRiskPointCode(existing.getRiskPointCode());
}
```

Also keep the existing deleted filter and explicit `updateTime` assignment; do not bring back manual code input validation.

- [ ] **Step 5: Run backend tests to verify the implementation passes**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest test`
Expected: PASS with `RiskPointServiceImplTest` green.

- [ ] **Step 6: Commit**

```bash
git add sql/init.sql sql/init-data.sql \
  spring-boot-iot-alarm/pom.xml \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java
git commit -m "feat: add risk point organization and auto-code backend"
```

### Task 3: Lock The Frontend Contract With Failing Risk-Point View Tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskStrategyListContract.test.ts`

- [ ] **Step 1: Expand the view test to expect the new organization and auto-code contract**

Replace the lightweight row factory with the new fields and add this assertion block:

```ts
function createRiskPointRow() {
  return {
    id: 1,
    riskPointCode: 'RP-PTYWZX-CRITICAL-001',
    riskPointName: '一号风险对象',
    orgId: 7101,
    orgName: '平台运维中心',
    regionId: 1,
    regionName: '东区',
    responsibleUser: 1,
    responsiblePhone: '13800000000',
    riskLevel: 'critical',
    description: 'desc',
    status: 0,
    tenantId: 1,
    remark: '',
    createBy: 1,
    createTime: '2026-04-01 08:00:00',
    updateBy: 1,
    updateTime: '2026-04-01 09:00:00',
    deleted: 0
  }
}

it('renders the organization column and read-only auto-code form contract', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 24,
      pageNum: 1,
      pageSize: 10,
      records: [createRiskPointRow()]
    }
  })

  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.html()).toContain('所属组织')
  expect(wrapper.html()).toContain('保存后自动生成')
})
```

- [ ] **Step 2: Add a source-contract test for the standardized risk-point list**

Append this to `RiskStrategyListContract.test.ts`:

```ts
it('keeps risk point on the standard list surface with three direct row actions', () => {
  const source = readViewSource('RiskPointView.vue')

  expect(source).toContain('standard-list-surface')
  expect(source).toContain('EmptyState')
  expect(source).toContain('v-loading="loading && hasRecords"')
  expect(source).toContain('page-sizes="[10, 20, 50, 100]"')
  expect(source).toContain('layout="total, sizes, prev, pager, next, jumper"')
  expect(source).toContain(':max-direct-items="3"')
  expect(source).toContain('listOrganizationTree')
  expect(source).toContain('form.orgId')
}
```

- [ ] **Step 3: Run the frontend tests to verify they fail**

Run: `cd spring-boot-iot-ui && npm run test -- src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RiskStrategyListContract.test.ts`
Expected: FAIL because `RiskPointView.vue` still uses `regionName`, lacks the standard list surface contract, and does not load organizations.

- [ ] **Step 4: Commit**

```bash
git add spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/RiskStrategyListContract.test.ts
git commit -m "test: lock risk point frontend organization contract"
```

### Task 4: Implement The Frontend Organization Flow, Read-Only Code, And Action Feedback

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskStrategyListContract.test.ts`

- [ ] **Step 1: Extend the risk-point API types**

Add these properties to the `RiskPoint` interface in `spring-boot-iot-ui/src/api/riskPoint.ts`:

```ts
orgId: IdType
orgName: string
```

Do not remove the existing `regionId`/`regionName` fields.

- [ ] **Step 2: Rebuild the list container on the shared list contract**

Use the same contract already present in `RuleDefinitionView.vue`:

```vue
<div
  v-loading="loading && hasRecords"
  class="ops-list-result-panel standard-list-surface"
  element-loading-text="正在刷新风险点列表"
  element-loading-background="var(--loading-mask-bg)"
>
  <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
    <div class="ops-list-loading-state__summary">
      <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
    </div>
    <div class="ops-list-loading-table ops-list-loading-table--header">
      <span v-for="item in 6" :key="`risk-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
    </div>
  </div>

  <template v-else-if="hasRecords">
    <!-- existing table -->
  </template>

  <div v-else-if="!loading" class="standard-list-empty-state">
    <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
    <div class="standard-list-empty-state__actions">
      <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
      <StandardButton v-else action="add" @click="handleAdd">新增风险点</StandardButton>
    </div>
  </div>
</div>
```

Also add:

```ts
const hasRecords = computed(() => riskPointList.value.length > 0)
const showListSkeleton = computed(() => loading.value && !hasRecords.value)
const emptyStateTitle = computed(() => hasAppliedFilters.value ? '暂无符合条件的风险点' : '暂无风险点记录')
const emptyStateDescription = computed(() => hasAppliedFilters.value ? '调整筛选条件后重试，或清空条件查看全部风险点。' : '请先新增风险点并补齐所属组织。')
```

- [ ] **Step 3: Replace region editing with organization-tree selection and read-only code**

Update the form state and organization loading in `RiskPointView.vue`:

```ts
import { listOrganizationTree } from '@/api/organization'
import type { Organization } from '@/api/organization'

const organizationOptions = ref<Organization[]>([])

const form = reactive({
  id: undefined as number | undefined,
  riskPointCode: '',
  riskPointName: '',
  orgId: '' as '' | number,
  orgName: '',
  regionId: 0,
  regionName: '',
  responsibleUser: 0,
  responsiblePhone: '',
  riskLevel: 'info',
  description: '',
  status: 0
})

const loadOrganizationOptions = async () => {
  try {
    const res = await listOrganizationTree()
    if (res.code === 200) {
      organizationOptions.value = (res.data || []).filter((item) => item.status === 1)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载组织树失败')
  }
}

const findOrganizationById = (nodes: Organization[], targetId: number): Organization | null => {
  for (const node of nodes) {
    if (Number(node.id) === targetId) {
      return node
    }
    const childMatch = node.children?.length ? findOrganizationById(node.children, targetId) : null
    if (childMatch) {
      return childMatch
    }
  }
  return null
}
```

Replace the two form items with:

```vue
<el-form-item label="风险点编号">
  <el-input :model-value="form.riskPointCode || '保存后自动生成'" readonly />
</el-form-item>
<el-form-item label="所属组织" prop="orgId">
  <el-tree-select
    v-model="form.orgId"
    :data="organizationOptions"
    node-key="id"
    check-strictly
    clearable
    default-expand-all
    :props="{ label: 'orgName', children: 'children', value: 'id' }"
    placeholder="请选择所属组织"
  />
</el-form-item>
```

And add rules:

```ts
orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }]
```

- [ ] **Step 4: Restore action reliability and visible error feedback**

In `RiskPointView.vue`, keep row actions fully direct and make every failure user-visible:

```vue
<StandardWorkbenchRowActions
  variant="table"
  :direct-items="getRiskPointRowActions()"
  :max-direct-items="3"
  @command="(command) => handleRiskPointRowAction(command, row)"
/>
```

```ts
const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true
    const selectedOrg = findOrganizationById(organizationOptions.value, Number(form.orgId))
    form.orgName = selectedOrg?.orgName || ''
    const res = form.id ? await updateRiskPoint(form) : await addRiskPoint(form)
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功')
      formVisible.value = false
      await loadRiskPointList()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交风险点失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: RiskPoint) => {
  try {
    await confirmDelete('风险点', row.riskPointName)
    const res = await deleteRiskPoint(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadRiskPointList()
    }
  } catch (error) {
    if (isConfirmCancelled(error)) return
    ElMessage.error(error instanceof Error ? error.message : '删除风险点失败')
  }
}
```

Apply the same `ElMessage.error(...)` pattern to organization load, device load, metric load, and bind submit.

- [ ] **Step 5: Run the frontend tests to verify the implementation passes**

Run: `cd spring-boot-iot-ui && npm run test -- src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RiskStrategyListContract.test.ts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-ui/src/api/riskPoint.ts \
  spring-boot-iot-ui/src/views/RiskPointView.vue \
  spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/RiskStrategyListContract.test.ts
git commit -m "feat: refresh risk point organization workflow"
```

### Task 5: Sync Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Check: `README.md`
- Check: `AGENTS.md`

- [ ] **Step 1: Update the business and API docs**

Apply these content changes:

```md
- 风险对象中心：新增/编辑风险点时，风险点编号由后端按“风险点名称 + 所属组织 + 风险等级”自动生成并固定，不再要求人工录入。
- 风险对象中心：主维护字段从页面层收口为“所属组织”，数据来源改为平台治理组织树；`region_id / region_name` 继续保留地理语义，不作为本轮主页面主字段。
```

And in the API doc add:

```md
- 风险点对象当前返回 `orgId`、`orgName`，用于组织范围治理承接。
- `POST /api/risk-point/add`、`POST /api/risk-point/update` 的请求体当前要求 `orgId` 必填，`riskPointCode` 由后端生成或沿用历史值。
```

- [ ] **Step 2: Update the database and data-permission docs**

Add this to `docs/04-数据库设计与初始化数据.md`:

```md
- `org_id` / `org_name`：风险点所属组织，用于承接后续组织范围权限控制。
- `region_id` / `region_name`：继续表达地理区域语义，不等同于组织归属。
```

Add this to `docs/13-数据权限与多租户模型.md`:

```md
- `risk_point` 当前已补齐 `org_id`、`org_name` 作为后续组织范围过滤挂点，但本轮真实环境基线仍未把登录态组织范围过滤正式收口到风险点查询。
```

- [ ] **Step 3: Record the frontend governance and change log**

Append to `docs/08-变更记录与技术债清单.md` and `docs/21-业务功能清单与验收标准.md`:

```md
- 2026-04-01：风险对象中心当前改为“所属组织”主维护口径，新增风险点编号由后端自动生成，页面同步恢复新增、编辑、绑定设备、删除主链路的可见反馈。
- 风险对象中心验收补充：新增风险点时编号自动生成；编辑不改历史编号；所属组织改为组织树选择；列表动作为新增、编辑、绑定设备、删除四项直出。
```

Only update `README.md` and `AGENTS.md` if their顶层基线文字已因“风险点自动编号 / 所属组织主口径”发生行为变化；如果主入口文档不需要新增说明，保持不动并在最终说明里明确“已检查，无需更新”。

- [ ] **Step 4: Run final targeted verification**

Run:

```bash
mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest test
cd spring-boot-iot-ui && npm run test -- src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RiskStrategyListContract.test.ts
```

Expected: PASS for the backend risk-point service tests and the two frontend risk-point view contracts.

- [ ] **Step 5: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md \
  docs/13-数据权限与多租户模型.md docs/21-业务功能清单与验收标准.md \
  README.md AGENTS.md
git commit -m "docs: sync risk point organization workflow"
```

## Self-Review

- Spec coverage:
  - 组织字段拆分：Task 1-2
  - 自动编号且仅新增生成：Task 1-2
  - 页面主视图只展示所属组织：Task 3-4
  - 新增/编辑/绑定/删除动作修复：Task 3-4
  - 文档同步：Task 5
- Placeholder scan:
  - 计划中未使用 `TODO`、`TBD`、`implement later` 之类占位。
- Type consistency:
  - 后端统一使用 `orgId`、`orgName`。
  - 前端 API、页面状态和文档同步均以 `orgId`、`orgName` 命名。

Plan complete and saved to `docs/superpowers/plans/2026-04-01-risk-point-organization-auto-code-implementation-plan.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration

2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?

---

## 2026-04-01 Execution Snapshot

- [x] 已完成风险点 `org_id / org_name` 后端档案落库。
- [x] 已完成新增风险点自动编号，编号基于“所属组织 + 风险点名称 + 风险等级”生成。
- [x] 已完成编辑保留历史编号、绑定重复校验与删除/绑定错误链路修复。
- [x] 已完成前端“所属组织”树选择、只读编号展示和失败显式反馈。
- [x] 已完成 `docs/02`、`03`、`04`、`08`、`13`、`21` 同步。
- [x] 已完成定向验证：
  - `mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=RiskPointServiceImplTest test`
  - `cd spring-boot-iot-ui && npx vitest run src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RiskStrategyListContract.test.ts`

当前刻意未交付的范围：

- [ ] 未按登录用户自动过滤风险点列表。
- [ ] 未给 `sys_user`、`UserAuthContextVO` 和 `/api/auth/me` 增加组织归属 / 组织范围字段。
- [ ] 未在风险点详情、更新、删除、绑定接口上增加“越权对象不可操作”的组织范围校验。

下次继续前必须先记住的事实：

- 当前 `risk_point` 已有 `org_id / org_name`，但 `sys_user` 还没有组织归属字段。
- 当前 `UserAuthContextVO` 只有用户、角色、菜单、按钮权限，没有组织范围。
- 所以下一轮“不同组织人员登录后只看到各自风险点”的工作，第一步不是直接改 `RiskPointView.vue`，而是先补齐用户组织归属和鉴权上下文。

## Next Session Plan: Deliver Login-Based Risk Point Visibility

**Goal:** 在保持本轮自动编号与组织树录入口径不回退的前提下，补齐“用户所属组织 -> 登录态组织范围 -> 风险点可见范围”的完整闭环，让不同组织人员登录后只看到本组织范围内的风险点。

**Architecture:** 先在 `sys_user` 上补齐所属组织字段，并把组织信息透出到 `UserAuthContextVO` / `/api/auth/me`；再由风险点查询与操作接口基于当前登录用户的组织范围过滤数据，超级管理员保留全量视图。前端沿用现有 `所属组织` 主语义，只增加登录态范围提示与回归测试，不回退到手工区域文本。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis-Plus、Spring Security JWT、Vue 3、TypeScript、Vitest、现有组织树与权限服务。

### Follow-up Task 1: Add User Organization Ownership

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/User.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java`
- Modify: `spring-boot-iot-ui/src/api/user.ts`
- Modify: `spring-boot-iot-ui/src/views/UserView.vue`

- [ ] 在 `sys_user` 表增加 `org_id` 字段，并给初始化账号补齐演示组织归属；不要新开平行迁移脚本，直接同步 `sql/init.sql` 和 `sql/init-data.sql`。
- [ ] 在 `User.java`、`user.ts`、`UserView.vue` 增加 `orgId`，账号中心新增/编辑统一复用 `/api/organization/tree` 下拉选择所属组织。
- [ ] 在 `UserServiceImpl` 的新增、编辑逻辑里校验组织存在且启用；若用户未选择组织则返回明确业务错误。
- [ ] 定向验证：
  - `mvn -pl spring-boot-iot-system -DskipTests=false test`
  - `cd spring-boot-iot-ui && npx vitest run src/__tests__/views/UserView.test.ts`

### Follow-up Task 2: Extend Auth Context With Organization Scope

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/UserAuthContextVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
- Modify: `spring-boot-iot-auth/src/main/java/com/ghlzm/iot/auth/service/impl/AuthServiceImpl.java`
- Check: `spring-boot-iot-auth/src/main/java/com/ghlzm/iot/auth/controller/AuthController.java`

- [ ] 在 `UserAuthContextVO` 增加 `orgId`、`orgName` 和 `orgScopeIds`，供页面和后端范围判断共用。
- [ ] 在 `PermissionServiceImpl#getUserAuthContext` 内根据用户所属组织递归计算“本人组织 + 子组织”范围；超级管理员返回全量标识或空限制语义。
- [ ] 继续保持 `/api/auth/me` 的既有菜单/按钮权限返回结构不破坏，只向 `authContext` 增量补充组织字段。
- [ ] 定向验证：
  - `mvn -pl spring-boot-iot-auth -pl spring-boot-iot-system -DskipTests=false test`
  - 用两个不同组织账号调用 `GET /api/auth/me`，确认 `orgId` 与 `orgScopeIds` 有差异。

### Follow-up Task 3: Enforce Organization Scope In Risk Point APIs

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

- [ ] 让风险点列表、分页、详情、更新、删除、绑定、解绑都能拿到当前登录用户 `userId` 或组织范围，不再只按公开参数查询。
- [ ] 普通账号只能看到并操作 `orgScopeIds` 内的风险点；越权访问统一返回明确业务错误；超级管理员保留全量查询与操作。
- [ ] 查询侧至少覆盖 `/list`、`/page`、`/get/{id}`，操作侧至少覆盖 `/update`、`/delete/{id}`、`/bind-device`、`/unbind-device`。
- [ ] 定向验证：
  - `mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=RiskPointServiceImplTest test`
  - 增补“不同组织账号查不到对方风险点、管理员仍可见全量”的服务层测试。

### Follow-up Task 4: Frontend Visibility Feedback And Real-Env Acceptance

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] 在 `RiskPointView.vue` 增加轻量提示，明确当前列表已按登录组织范围过滤；不要回流第二套说明墙或页头私有导航。
- [ ] 补齐前端测试，锁定“页面继续用组织树维护所属组织，且列表结果依赖登录态组织范围”的合同。
- [ ] 把组织范围过滤规则同步写回接口、数据库、权限和验收文档；说明超级管理员全量可见、普通账号受组织范围约束。
- [ ] 用真实环境做最小验收：
  - 账号 A 绑定组织 7101，只看到 7101 范围风险点。
  - 账号 B 绑定组织 7102，只看到 7102 范围风险点。
  - `admin` 仍可看到全部风险点。
  - 新增风险点的自动编号、编辑保留编号、绑定设备与删除链路不因组织过滤退化。

## Suggested Start Order For Next Time

1. 先补 `sys_user.org_id` 与账号中心组织选择。
2. 再补 `/api/auth/me` 的组织上下文。
3. 然后给风险点接口加登录态组织过滤与越权校验。
4. 最后做前端提示、文档同步和真实环境验收。
