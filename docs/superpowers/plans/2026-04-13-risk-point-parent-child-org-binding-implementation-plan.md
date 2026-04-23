# Risk Point Parent-Child Organization Binding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let risk-point formal bindings support parent-child organization pairing without widening non-super-admin tenant or data-permission boundaries.

**Architecture:** Keep the existing controller/API surface unchanged and implement the behavior entirely inside `RiskPointServiceImpl`. Non-super-admin callers still depend on existing risk-point access checks and device visibility checks, then add a same-tenant plus ancestor/descendant organization rule. Super admins bypass tenant and organization pairing checks, but devices without an organization remain ineligible.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JUnit 5, Mockito

---

## File Structure

### Task 1 ownership: regression tests in `spring-boot-iot-alarm`

- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

### Task 2 ownership: binding rule implementation in `spring-boot-iot-alarm`

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`

### Task 3 ownership: docs sync

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`

## Task 1: Write the regression tests first

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

- [ ] **Step 1: Add failing bind-write tests**

Add tests covering:

```java
@Test
void bindDeviceShouldAllowParentOrganizationWithinSameTenantForNonSuperAdmin() { ... }

@Test
void bindDeviceShouldAllowChildOrganizationWithinSameTenantForNonSuperAdmin() { ... }

@Test
void bindDeviceShouldRejectSiblingOrganizationWithinSameTenantForNonSuperAdmin() { ... }

@Test
void bindDeviceShouldRejectCrossTenantForNonSuperAdmin() { ... }

@Test
void bindDeviceShouldAllowCrossTenantForSuperAdmin() { ... }
```

- [ ] **Step 2: Add failing candidate-list test**

Add a test covering:

```java
@Test
void listBindableDevicesShouldKeepParentChildOrganizationsAndExcludeSiblingOrganizations() { ... }
```

- [ ] **Step 3: Run the focused service test and verify RED**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointServiceImplTest" test
```

Expected: `BUILD FAILURE`, because the new tests still observe the old exact-org restriction.

## Task 2: Implement the binding rule in `RiskPointServiceImpl`

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`

- [ ] **Step 1: Pass current-user context into write validation**

Change the validation calls so both write paths use:

```java
validateRiskPointDeviceBinding(riskPoint, device, requestRiskPointId, currentUserId);
```

- [ ] **Step 2: Implement the new rule**

Add helpers for:

```java
private DataPermissionContext resolveBindingPermissionContext(Long currentUserId) { ... }
private boolean isSuperAdminBinding(Long currentUserId) { ... }
private boolean hasBindableOrganizationRelation(Long leftOrgId, Long rightOrgId) { ... }
private boolean isAncestorOrSelf(Long ancestorOrgId, Long descendantOrgId, Map<Long, Organization> cache) { ... }
```

Rule details:

```java
if (device.getOrgId() == null || device.getOrgId() <= 0) {
    throw new BizException("设备未归属组织，禁止绑定风险点");
}
if (isSuperAdminBinding(currentUserId)) {
    return;
}
if (!Objects.equals(riskPoint.getTenantId(), device.getTenantId())) {
    throw new BizException("设备与风险点不属于同一租户，禁止绑定");
}
if (!hasBindableOrganizationRelation(riskPoint.getOrgId(), device.getOrgId())) {
    throw new BizException("设备所属组织与风险点所属组织不是父子组织");
}
```

- [ ] **Step 3: Apply the same rule to candidate filtering**

Update `listBindableDevices(...)` so:

```java
DataPermissionContext context = resolveBindingPermissionContext(currentUserId);
return deviceOptions.stream()
        .filter(device -> isDeviceOptionBindableForRiskPoint(riskPoint, device, currentUserId, context))
        .filter(device -> currentRiskPointDeviceIds.contains(device.getId()) || !occupiedDeviceIds.contains(device.getId()))
        .toList();
```

- [ ] **Step 4: Re-run the focused service test and verify GREEN**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointServiceImplTest" test
```

Expected: `BUILD SUCCESS`

## Task 3: Sync docs with the new binding rule

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`

- [ ] **Step 1: Update business and permission docs**

Document that:

1. Risk binding candidates and writes now allow parent-child organizations.
2. Non-super-admin users still require same-tenant and existing data-permission visibility.
3. Super admins remain exempt from tenant and organization pairing checks.

- [ ] **Step 2: Run verification for docs and source formatting**

Run:

```bash
git diff --check
```

Expected: no whitespace or patch-format issues.

- [ ] **Step 3: Run the final focused verification set**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointServiceImplTest,RiskPointControllerTest,RiskPointBindingMaintenanceServiceImplTest" test
git diff --check
```

Expected: `BUILD SUCCESS` and empty `git diff --check` output.
