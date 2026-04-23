# Fixed Governance Approver Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove manual governance approver input from the product contract workbench and replace it with a system-managed fixed approver policy for product contract apply, rollback, and resubmit flows.

**Architecture:** Add a first-class approval policy table and resolver in `spring-boot-iot-system`, then wire `spring-boot-iot-device` contract controllers to resolve approvers server-side instead of reading `X-Governance-Approver-Id`. Keep governance approval orders and transitions unchanged, and update the Vue workbench to remove the approver input and call the new backend contract-specific resubmit path. Execution stays on `codex/dev` in the current workspace because repository governance forbids shifting implementation to another branch or detached worktree.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Vitest, Maven, MySQL init SQL.

---

### Task 1: Add governance approval policy storage and resolver

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceApprovalPolicy.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceApprovalPolicyMapper.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceApprovalPolicyResolver.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalPolicyResolverImpl.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalPolicyResolverImplTest.java`
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`

- [ ] **Step 1: Write the failing resolver test**

```java
@Test
void resolveApproverUserIdShouldPreferTenantPolicyAndFallbackToGlobal() {
    GovernanceApprovalPolicy tenantPolicy = policy(91001L, 1L, "TENANT", "PRODUCT_CONTRACT_RELEASE_APPLY", 30003L, 1);
    GovernanceApprovalPolicy globalPolicy = policy(91002L, 0L, "GLOBAL", "PRODUCT_CONTRACT_RELEASE_APPLY", 20002L, 1);
    User operator = user(10001L, 1L, 7101L, 1);
    User approver = user(30003L, 1L, 7101L, 1);
    Role superAdmin = role(92000005L, 1L, "SUPER_ADMIN", 1);

    when(userMapper.selectById(10001L)).thenReturn(operator);
    when(userMapper.selectById(30003L)).thenReturn(approver);
    when(policyMapper.selectList(any())).thenReturn(List.of(tenantPolicy, globalPolicy));
    when(userRoleMapper.selectRoleIdsByUserId(30003L)).thenReturn(List.of(92000005L));
    when(roleMapper.selectBatchIds(List.of(92000005L))).thenReturn(List.of(superAdmin));

    Long approverUserId = resolver.resolveApproverUserId("PRODUCT_CONTRACT_RELEASE_APPLY", 10001L);

    assertEquals(30003L, approverUserId);
}
```

- [ ] **Step 2: Run the resolver test to verify it fails**

Run:

```powershell
mvn -pl spring-boot-iot-system -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceApprovalPolicyResolverImplTest" test
```

Expected: FAIL because the resolver and policy table classes do not exist yet.

- [ ] **Step 3: Add the SQL schema and seed rows**

```sql
CREATE TABLE sys_governance_approval_policy (
    id BIGINT NOT NULL COMMENT 'approval policy id',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'tenant id, 0 means global',
    scope_type VARCHAR(16) NOT NULL COMMENT 'GLOBAL/TENANT',
    action_code VARCHAR(64) NOT NULL COMMENT 'approval action code',
    approver_mode VARCHAR(32) NOT NULL COMMENT 'FIXED_USER',
    approver_user_id BIGINT NOT NULL COMMENT 'fixed approver user id',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT 'enabled',
    remark VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_governance_approval_policy_scope_action (scope_type, tenant_id, action_code, deleted),
    KEY idx_governance_approval_policy_enabled (enabled, scope_type, tenant_id, action_code, deleted),
    KEY idx_governance_approval_policy_approver (approver_user_id, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='governance approval policy';
```

```sql
INSERT INTO sys_user (
    id, tenant_id, org_id, username, password, nickname, real_name, phone, email, status, is_admin,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES (
    99000001, 1, 7101, 'governance_reviewer', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
    '治理复核专员', '治理复核专员', '13800009900', 'governance-reviewer@ghlzm.com', 1, 1,
    '系统级固定治理复核人', 1, NOW(), 1, NOW(), 0
) ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    is_admin = VALUES(is_admin),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    update_time = NOW(),
    deleted = 0;
```

```sql
INSERT INTO sys_governance_approval_policy (
    id, tenant_id, scope_type, action_code, approver_mode, approver_user_id, enabled, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (99001001, 0, 'GLOBAL', 'PRODUCT_CONTRACT_RELEASE_APPLY', 'FIXED_USER', 99000001, 1, '产品契约发布固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001002, 0, 'GLOBAL', 'PRODUCT_CONTRACT_ROLLBACK', 'FIXED_USER', 99000001, 1, '产品契约回滚固定复核人', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    approver_user_id = VALUES(approver_user_id),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    update_time = NOW(),
    deleted = 0;
```

- [ ] **Step 4: Implement the minimal resolver**

```java
public interface GovernanceApprovalPolicyResolver {

    Long resolveApproverUserId(String actionCode, Long operatorUserId);
}
```

```java
@Service
public class GovernanceApprovalPolicyResolverImpl implements GovernanceApprovalPolicyResolver {

    @Override
    public Long resolveApproverUserId(String actionCode, Long operatorUserId) {
        User operator = requireActiveUser(operatorUserId, "审批执行人无效");
        GovernanceApprovalPolicy policy = loadPolicy(actionCode, operator.getTenantId());
        User approver = requireActiveUser(policy.getApproverUserId(), "固定复核人不存在或已停用");
        ensureNotSelf(operatorUserId, approver.getId());
        ensureSuperAdmin(approver.getId());
        return approver.getId();
    }
}
```

- [ ] **Step 5: Run the resolver test to verify it passes**

Run:

```powershell
mvn -pl spring-boot-iot-system -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceApprovalPolicyResolverImplTest" test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add sql/init.sql sql/init-data.sql spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceApprovalPolicy.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceApprovalPolicyMapper.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceApprovalPolicyResolver.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalPolicyResolverImpl.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalPolicyResolverImplTest.java
git commit -m "feat(system): add fixed governance approver policy"
```

### Task 2: Route product contract apply, rollback, and resubmit through the fixed approver resolver

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductContractReleaseController.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductGovernanceApprovalController.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductGovernanceApprovalControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

```java
@Test
void applyGovernanceShouldResolveFixedApproverWhenHeaderIsMissing() {
    Authentication authentication = authentication(1001L);
    ProductModelGovernanceApplyDTO dto = applyDto("value");
    when(governanceApprovalPolicyResolver.resolveApproverUserId("PRODUCT_CONTRACT_RELEASE_APPLY", 1001L)).thenReturn(99000001L);
    when(governanceApprovalService.submitAction(any())).thenReturn(88001L);

    R<ProductModelGovernanceApplyResultVO> response = controller.applyGovernance(1001L, dto, null, authentication);

    assertEquals(88001L, response.getData().getApprovalOrderId());
    verify(governanceApprovalPolicyResolver).resolveApproverUserId("PRODUCT_CONTRACT_RELEASE_APPLY", 1001L);
    verify(permissionGuard).requireDualControl(1001L, 99000001L, "产品契约发布", "iot:product-contract:release", "iot:product-contract:approve");
}
```

```java
@Test
void resubmitProductApprovalShouldUseResolvedFixedApprover() {
    Authentication authentication = authentication(10001L);
    GovernanceApprovalOrderDetailVO order = orderDetail(88001L, "PRODUCT_CONTRACT_RELEASE_APPLY");
    when(governanceApprovalQueryService.getOrderDetail(10001L, 88001L)).thenReturn(order);
    when(governanceApprovalPolicyResolver.resolveApproverUserId("PRODUCT_CONTRACT_RELEASE_APPLY", 10001L)).thenReturn(99000001L);

    R<Void> response = controller.resubmitOrder(88001L, authentication);

    assertEquals(200, response.getCode());
    verify(governanceApprovalService).resubmitOrder(88001L, 10001L, 99000001L, null);
}
```

- [ ] **Step 2: Run the device controller tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest" test
```

Expected: FAIL because the controllers still require manual approver IDs and the new product governance resubmit controller does not exist.

- [ ] **Step 3: Implement minimal controller changes**

```java
private Long resolveApproverUserId(Long currentUserId, Long headerApproverUserId, String actionCode) {
    if (headerApproverUserId != null && headerApproverUserId > 0) {
        return headerApproverUserId;
    }
    return governanceApprovalPolicyResolver.resolveApproverUserId(actionCode, currentUserId);
}
```

```java
@PostMapping("/api/device/product/governance-approval/{orderId}/resubmit")
public R<Void> resubmitOrder(@PathVariable Long orderId, Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    GovernanceApprovalOrderDetailVO detail = governanceApprovalQueryService.getOrderDetail(currentUserId, orderId);
    String actionCode = detail.getOrder().getActionCode();
    if (!ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY.equals(actionCode)
            && !ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK.equals(actionCode)) {
        throw new BizException("当前审批单不支持固定复核人自动重提");
    }
    Long approverUserId = governanceApprovalPolicyResolver.resolveApproverUserId(actionCode, currentUserId);
    governanceApprovalService.resubmitOrder(orderId, currentUserId, approverUserId, null);
    return R.ok();
}
```

- [ ] **Step 4: Run the device controller tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductContractReleaseController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductGovernanceApprovalController.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductGovernanceApprovalControllerTest.java
git commit -m "feat(device): auto-resolve fixed product contract approver"
```

### Task 3: Remove manual approver input from the product workbench UI

**Files:**
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/api/governanceApproval.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Write the failing frontend tests**

```ts
it('submits apply approval without manual approver input', async () => {
  const wrapper = mountWorkspace()
  await flushPromises()
  await wrapper.get('[data-testid="sample-payload"]').setValue(JSON.stringify(samplePayload))
  await wrapper.find('[data-testid="extract-contract-fields"]').trigger('click')
  await flushPromises()

  await findApplyButton(wrapper)?.trigger('click')
  await flushPromises()

  expect(mockApplyProductModelGovernance).toHaveBeenCalledWith(
    1001,
    expect.objectContaining({ items: expect.any(Array) }),
    {}
  )
  expect(wrapper.find('[data-testid="governance-approver-id"]').exists()).toBe(false)
})
```

```ts
it('resubmits rejected approval through product governance endpoint', async () => {
  mockResubmitProductGovernanceApproval.mockResolvedValue({ code: 200, msg: 'success', data: null })
  const wrapper = mountWorkspace()
  await flushPromises()
  await openRejectedApplyReceipt(wrapper)

  await wrapper.find('[data-testid="contract-field-apply-resubmit"]').trigger('click')
  await flushPromises()

  expect(mockResubmitProductGovernanceApproval).toHaveBeenCalledWith(88001)
  expect(mockResubmitGovernanceApprovalOrder).not.toHaveBeenCalled()
})
```

- [ ] **Step 2: Run the frontend test to verify it fails**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: FAIL because the component still requires `governance-approver-id` and still calls the generic governance resubmit API with `approverUserId`.

- [ ] **Step 3: Implement the minimal UI and API changes**

```ts
resubmitProductGovernanceApproval(orderId: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/device/product/governance-approval/${orderId}/resubmit`, {
    method: 'POST'
  })
}
```

```ts
await productApi.applyProductModelGovernance(productId, payload, {})
await productApi.rollbackProductContractReleaseBatch(latestReleaseBatchId.value)
await productApi.resubmitProductGovernanceApproval(applyApprovalOrderId.value)
```

```vue
<div class="product-model-designer__approval-inline">
  <span>复核机制</span>
  <strong>系统固定复核人</strong>
</div>
```

- [ ] **Step 4: Run the frontend test to verify it passes**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-ui/src/api/product.ts spring-boot-iot-ui/src/api/governanceApproval.ts spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "feat(ui): remove manual governance approver input"
```

### Task 4: Update docs and run focused verification

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Write failing assertions in doc-adjacent tests or checks**

Use command-based checks to lock the changed API and text:

```powershell
Select-String -Path 'docs/03-接口规范与接口清单.md' -Pattern 'X-Governance-Approver-Id|/api/device/product/governance-approval/{orderId}/resubmit'
Select-String -Path 'docs/04-数据库设计与初始化数据.md' -Pattern 'sys_governance_approval_policy'
```

Expected before edits: the new product governance resubmit endpoint and policy table are missing.

- [ ] **Step 2: Update the docs**

```md
- `/products` 契约字段工作台当前不再要求业务用户填写复核人用户 ID；产品契约发布、回滚和工作台内重提统一由后端按 `sys_governance_approval_policy` 解析固定复核人。
- `sys_governance_approval_policy` 当前用于“动作 -> 固定复核人”策略解析，首批覆盖 `PRODUCT_CONTRACT_RELEASE_APPLY / PRODUCT_CONTRACT_ROLLBACK`。
- `POST /api/device/product/governance-approval/{orderId}/resubmit` 当前只服务 `/products` 工作台，对产品契约审批单执行固定复核人自动重提。
```

- [ ] **Step 3: Run focused verification**

Run:

```powershell
mvn -pl spring-boot-iot-system -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest" test
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git diff --check
```

Expected: all tests PASS and `git diff --check` returns no whitespace errors.

- [ ] **Step 4: Commit**

```powershell
git add README.md AGENTS.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/13-数据权限与多租户模型.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document fixed governance approver flow"
```
