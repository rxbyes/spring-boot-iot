# No-Code Device Onboarding P0-1 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `/device-onboarding` 无代码接入台基础版，让平台能用一个统一案例对象回答“当前接入到哪一步、卡在哪里、下一步去哪处理”。

**Architecture:** 继续复用现有 `/products`、`/protocol-governance`、审批与质量工场真相，不引入第二套协议或合同治理体系。后端只新增一个编排对象 `iot_device_onboarding_case` 及最小 CRUD/刷新状态接口；前端新增一个工作台页面、路由和接入智维入口，用最小状态推导串起协议治理、产品治理和合同发布深链。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL Schema Registry, Vue 3, TypeScript, Vite, Vitest, JUnit 5, Maven

---

## Scope Check

本计划只覆盖已批准的首个垂直切片 `P0-1 统一接入工作台基础版`，只交付以下能力：

1. 持久化 `iot_device_onboarding_case`
2. 案例列表、创建、详情、更新、刷新状态 API
3. `/device-onboarding` 页面、路由、接入智维入口和菜单种子
4. 最小阻塞状态推导与跳转到 `/protocol-governance`、`/products`

本计划明确不做：

1. 模板包与模板继承
2. 质量工场验收触发
3. 批量接入与批量治理
4. 自动创建设备、自动审批或自动发布

## File Structure

### Schema And Seed

- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\sql\init.sql`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\schema\generated\mysql-schema-sync.json`

### Backend

- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\DeviceOnboardingCase.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\mapper\DeviceOnboardingCaseMapper.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseCreateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseUpdateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseQueryDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingCaseService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseController.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseSummaryVO.java`

### Frontend

- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\router\index.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\utils\sectionWorkspaces.ts`

### Tests

- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseControllerTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\SectionLandingView.test.ts`

### Docs

- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

## Task 1: Add onboarding case persistence and status derivation

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\DeviceOnboardingCase.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\mapper\DeviceOnboardingCaseMapper.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseCreateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseUpdateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseQueryDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingCaseService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseSummaryVO.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`

- [ ] **Step 1: Write the failing service tests**

```java
@Test
void refreshStatusShouldBlockAtProtocolGovernanceWhenProtocolFieldsMissing() {
    DeviceOnboardingCase entity = baseCase();
    entity.setProtocolFamilyCode(null);
    entity.setDecryptProfileCode(null);
    entity.setProtocolTemplateCode(null);
    when(mapper.selectById(9101L)).thenReturn(entity);

    DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

    assertEquals("PROTOCOL_GOVERNANCE", result.getCurrentStep());
    assertEquals("BLOCKED", result.getStatus());
    assertTrue(result.getBlockers().contains("待补齐协议族/解密档案/协议模板"));
}

@Test
void refreshStatusShouldMoveToAcceptanceWhenReleaseBatchExists() {
    DeviceOnboardingCase entity = baseCase();
    entity.setProtocolFamilyCode("legacy-dp-crack");
    entity.setDecryptProfileCode("aes-62000002");
    entity.setProtocolTemplateCode("nf-crack-v1");
    entity.setProductId(1001L);
    entity.setReleaseBatchId(88001L);
    when(mapper.selectById(9101L)).thenReturn(entity);

    DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

    assertEquals("ACCEPTANCE", result.getCurrentStep());
    assertEquals("READY", result.getStatus());
    assertTrue(result.getBlockers().isEmpty());
}
```

- [ ] **Step 2: Run the service test and verify RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -am "-Dtest=DeviceOnboardingCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- compile errors mentioning missing `DeviceOnboardingCaseServiceImpl`, `DeviceOnboardingCaseVO`, or mapper/entity

- [ ] **Step 3: Add the schema object and regenerate artifacts**

```json
{
  "name": "iot_device_onboarding_case",
  "storageType": "mysql_table",
  "ownerModule": "spring-boot-iot-device",
  "lifecycle": "active",
  "includedInInit": true,
  "includedInSchemaSync": true,
  "runtimeBootstrapMode": "schema_sync_managed",
  "tableCommentZh": "设备无代码接入案例表",
  "fields": [
    { "name": "id", "type": "BIGINT NOT NULL", "commentZh": "主键" },
    { "name": "tenant_id", "type": "BIGINT NOT NULL DEFAULT 1", "commentZh": "租户ID" },
    { "name": "case_code", "type": "VARCHAR(64) NOT NULL", "commentZh": "案例编码" },
    { "name": "case_name", "type": "VARCHAR(128) NOT NULL", "commentZh": "案例名称" },
    { "name": "scenario_code", "type": "VARCHAR(64) DEFAULT NULL", "commentZh": "场景编码" },
    { "name": "device_family", "type": "VARCHAR(64) DEFAULT NULL", "commentZh": "设备族" },
    { "name": "protocol_family_code", "type": "VARCHAR(64) DEFAULT NULL", "commentZh": "协议族编码" },
    { "name": "decrypt_profile_code", "type": "VARCHAR(64) DEFAULT NULL", "commentZh": "解密档案编码" },
    { "name": "protocol_template_code", "type": "VARCHAR(64) DEFAULT NULL", "commentZh": "协议模板编码" },
    { "name": "product_id", "type": "BIGINT DEFAULT NULL", "commentZh": "产品ID" },
    { "name": "release_batch_id", "type": "BIGINT DEFAULT NULL", "commentZh": "合同发布批次ID" },
    { "name": "current_step", "type": "VARCHAR(32) NOT NULL DEFAULT 'PROTOCOL_GOVERNANCE'", "commentZh": "当前步骤" },
    { "name": "status", "type": "VARCHAR(32) NOT NULL DEFAULT 'BLOCKED'", "commentZh": "状态" },
    { "name": "blocker_summary_json", "type": "JSON DEFAULT NULL", "commentZh": "阻塞摘要JSON" },
    { "name": "evidence_summary_json", "type": "JSON DEFAULT NULL", "commentZh": "证据摘要JSON" },
    { "name": "remark", "type": "VARCHAR(500) DEFAULT NULL", "commentZh": "备注" },
    { "name": "create_by", "type": "BIGINT DEFAULT NULL", "commentZh": "创建人用户ID" },
    { "name": "create_time", "type": "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP", "commentZh": "创建时间" },
    { "name": "update_by", "type": "BIGINT DEFAULT NULL", "commentZh": "更新人用户ID" },
    { "name": "update_time", "type": "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "commentZh": "更新时间" },
    { "name": "deleted", "type": "TINYINT NOT NULL DEFAULT 0", "commentZh": "逻辑删除标记" }
  ]
}
```

Run:

```powershell
python scripts/schema/render_artifacts.py --write
python scripts/schema/check_schema_registry.py
```

Expected: render 成功，registry 检查通过。

- [ ] **Step 4: Write the minimal service implementation**

```java
private DerivedStatus deriveStatus(DeviceOnboardingCase entity) {
    List<String> blockers = new ArrayList<>();
    if (!StringUtils.hasText(entity.getProtocolFamilyCode())
            || !StringUtils.hasText(entity.getDecryptProfileCode())
            || !StringUtils.hasText(entity.getProtocolTemplateCode())) {
        blockers.add("待补齐协议族/解密档案/协议模板");
        return new DerivedStatus("PROTOCOL_GOVERNANCE", "BLOCKED", blockers);
    }
    if (entity.getProductId() == null) {
        blockers.add("待绑定产品并完成契约治理");
        return new DerivedStatus("PRODUCT_GOVERNANCE", "BLOCKED", blockers);
    }
    if (entity.getReleaseBatchId() == null) {
        blockers.add("待发布正式合同批次");
        return new DerivedStatus("CONTRACT_RELEASE", "IN_PROGRESS", blockers);
    }
    return new DerivedStatus("ACCEPTANCE", "READY", List.of());
}
```

- [ ] **Step 5: Run the service test and verify GREEN**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -am "-Dtest=DeviceOnboardingCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add schema/mysql/device-domain.json schema/generated/mysql-schema-sync.json sql/init.sql spring-boot-iot-device/src/main/java/com/ghlzm/iot/device spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceOnboardingCaseServiceImplTest.java
git commit -m "feat: add device onboarding case foundation"
```

## Task 2: Expose onboarding APIs and permission entry

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseController.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseControllerTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

- [ ] **Step 1: Write the failing controller tests**

```java
@Test
void pageCasesShouldDelegateToService() {
    DeviceOnboardingCaseVO row = new DeviceOnboardingCaseVO();
    row.setId(9101L);
    row.setCaseName("裂缝传感器接入");
    when(service.pageCases(any())).thenReturn(PageResult.of(1L, 1L, 10L, List.of(row)));

    R<PageResult<DeviceOnboardingCaseVO>> response = controller.pageCases(new DeviceOnboardingCaseQueryDTO());

    assertEquals(1L, response.getData().getTotal());
    assertEquals("裂缝传感器接入", response.getData().getRecords().get(0).getCaseName());
}

@Test
void refreshStatusShouldUseCurrentUser() {
    DeviceOnboardingCaseVO row = new DeviceOnboardingCaseVO();
    row.setId(9101L);
    row.setCurrentStep("CONTRACT_RELEASE");
    when(service.refreshStatus(9101L, 10001L)).thenReturn(row);

    R<DeviceOnboardingCaseVO> response = controller.refreshStatus(9101L, authentication(10001L));

    assertEquals("CONTRACT_RELEASE", response.getData().getCurrentStep());
    verify(service).refreshStatus(9101L, 10001L);
}
```

- [ ] **Step 2: Run the controller test and verify RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -am "-Dtest=DeviceOnboardingCaseControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because controller does not exist yet.

- [ ] **Step 3: Implement the minimal controller and menu seed**

```java
@RestController
@RequestMapping("/api/device/onboarding/cases")
public class DeviceOnboardingCaseController {

    @GetMapping
    public R<PageResult<DeviceOnboardingCaseVO>> pageCases(DeviceOnboardingCaseQueryDTO query) { ... }

    @PostMapping
    public R<DeviceOnboardingCaseVO> createCase(@RequestBody @Valid DeviceOnboardingCaseCreateDTO dto,
                                                Authentication authentication) { ... }

    @GetMapping("/{caseId}")
    public R<DeviceOnboardingCaseVO> getCase(@PathVariable Long caseId) { ... }

    @PutMapping("/{caseId}")
    public R<DeviceOnboardingCaseVO> updateCase(@PathVariable Long caseId,
                                                @RequestBody @Valid DeviceOnboardingCaseUpdateDTO dto,
                                                Authentication authentication) { ... }

    @PostMapping("/{caseId}/refresh-status")
    public R<DeviceOnboardingCaseVO> refreshStatus(@PathVariable Long caseId, Authentication authentication) { ... }
}
```

```sql
INSERT INTO sys_menu (...) VALUES (..., '无代码接入台', '/device-onboarding', 'device-onboarding', ...);
```

- [ ] **Step 4: Run controller tests and a focused compile**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -am "-Dtest=DeviceOnboardingCaseControllerTest,DeviceOnboardingCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceOnboardingCaseControllerTest.java sql/init-data.sql
git commit -m "feat: expose device onboarding case apis"
```

## Task 3: Build the `/device-onboarding` workbench

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\router\index.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\utils\sectionWorkspaces.ts`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\SectionLandingView.test.ts`

- [ ] **Step 1: Write the failing UI tests**

```ts
it('renders onboarding summary cards and next-step actions', async () => {
  mockPageCases.mockResolvedValue({
    total: 1,
    pageNum: 1,
    pageSize: 10,
    records: [
      {
        id: 9101,
        caseCode: 'CASE-9101',
        caseName: '裂缝传感器接入',
        currentStep: 'PRODUCT_GOVERNANCE',
        status: 'BLOCKED',
        blockers: ['待绑定产品并完成契约治理'],
        productId: null
      }
    ]
  })

  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.text()).toContain('无代码接入台')
  expect(wrapper.text()).toContain('裂缝传感器接入')
  expect(wrapper.text()).toContain('待绑定产品并完成契约治理')
  expect(wrapper.text()).toContain('前往产品治理')
})
```

```ts
it('adds device onboarding card to iot access section', () => {
  const wrapper = mountSectionLanding()
  expect(wrapper.text()).toContain('无代码接入台')
})
```

- [ ] **Step 2: Run the UI tests and verify RED**

Run:

```powershell
npm run test -- DeviceOnboardingWorkbenchView SectionLandingView
```

Expected: FAIL because route, types, api module, and view do not exist yet.

- [ ] **Step 3: Implement the minimal page**

```ts
export interface DeviceOnboardingCase {
  id: IdType
  caseCode: string
  caseName: string
  scenarioCode?: string | null
  deviceFamily?: string | null
  protocolFamilyCode?: string | null
  decryptProfileCode?: string | null
  protocolTemplateCode?: string | null
  productId?: IdType | null
  releaseBatchId?: IdType | null
  currentStep: 'PROTOCOL_GOVERNANCE' | 'PRODUCT_GOVERNANCE' | 'CONTRACT_RELEASE' | 'ACCEPTANCE'
  status: 'BLOCKED' | 'IN_PROGRESS' | 'READY'
  blockers: string[]
}
```

```vue
<IotAccessPageShell title="无代码接入台" description="统一查看接入案例、当前步骤、阻塞原因和下一步动作。">
  <section class="device-onboarding-workbench__summary">...</section>
  <StandardWorkbenchPanel title="接入案例" description="先看当前卡点，再跳到协议治理或产品治理处理。">
    <article v-for="row in rows" :key="String(row.id)">
      <strong>{{ row.caseName }}</strong>
      <p>{{ row.currentStep }}</p>
      <p>{{ row.blockers[0] || '已具备进入验收条件' }}</p>
    </article>
  </StandardWorkbenchPanel>
</IotAccessPageShell>
```

- [ ] **Step 4: Run UI tests and a front-end build check**

Run:

```powershell
npm run test -- DeviceOnboardingWorkbenchView SectionLandingView
npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-ui/src/api spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/__tests__/views
git commit -m "feat: add device onboarding workbench"
```

## Task 4: Update docs and run final verification

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: Update behavior, API, schema and acceptance docs**

Add the following factual deltas:

```md
- 接入智维新增 `/device-onboarding` 无代码接入台，首版用于收口接入案例、当前步骤、阻塞原因和下一步动作。
- 新增 `GET /api/device/onboarding/cases`、`POST /api/device/onboarding/cases`、`GET /api/device/onboarding/cases/{caseId}`、`PUT /api/device/onboarding/cases/{caseId}`、`POST /api/device/onboarding/cases/{caseId}/refresh-status`。
- 新增表 `iot_device_onboarding_case`，只承载接入编排真相，不承载协议或合同正式内容。
```

- [ ] **Step 2: Run final verification**

Run:

```powershell
python scripts/schema/render_artifacts.py --write
python scripts/schema/check_schema_registry.py
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -am "-Dtest=DeviceOnboardingCaseServiceImplTest,DeviceOnboardingCaseControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
npm run test -- DeviceOnboardingWorkbenchView SectionLandingView
```

Expected: all pass.

- [ ] **Step 3: Commit**

```powershell
git add README.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/07-部署运行与配置说明.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document device onboarding foundation"
```

## Self-Review

### Spec coverage

已覆盖：

1. 统一接入入口
2. 接入案例编排对象
3. 当前步骤/阻塞原因/下一步动作
4. 接入智维入口、权限可见性与菜单种子

暂未覆盖但明确排除在本切片之外：

1. 模板包
2. 标准验收触发
3. 批量治理

### Placeholder scan

已检查，无 `TODO`、`TBD` 或“后续补充”式步骤。

### Type consistency

当前计划统一使用：

1. `DeviceOnboardingCaseCreateDTO`
2. `DeviceOnboardingCaseUpdateDTO`
3. `DeviceOnboardingCaseQueryDTO`
4. `DeviceOnboardingCaseVO`
5. `DeviceOnboardingCaseService`

这些名称在任务间保持一致。

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-18-no-code-device-onboarding-p0-1-foundation-implementation-plan.md`.

本会话已由用户明确选择直接执行，因此下一步按 `executing-plans` 方式在当前会话内联实现，不再等待二次确认。
