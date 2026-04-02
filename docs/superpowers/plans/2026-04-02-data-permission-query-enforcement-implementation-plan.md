# Data Permission Query Enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the current tenant/org/data-scope identity model from display-only into real query-side enforcement for the first batch of governance and risk endpoints.

**Architecture:** Reuse the existing identity model in `PermissionService`, add a focused data-permission context plus organization-scope resolution, and let controllers pass the current login user into scoped service methods. First batch lands in `用户管理 / 组织管理 / 风险点管理`, keeping raw persistence models unchanged and avoiding global SQL interceptors.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JUnit 5, Mockito.

---

## File Structure

### Backend files

- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/PermissionService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/DataPermissionContext.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/UserService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/OrganizationService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/OrganizationServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/UserController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/OrganizationController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/PermissionServiceImplTest.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/UserServiceImplTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/OrganizationServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

### Documentation files

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`

## Task 1: Shared Data Permission Context

- [ ] Add a focused `DataPermissionContext` model with `userId / tenantId / orgId / dataScopeType / superAdmin`.
- [ ] Extend `PermissionService` with methods for resolving data-scope context and accessible organization ids.
- [ ] Write failing tests for `ORG_AND_CHILDREN` subtree resolution and `SELF` scope fallback.
- [ ] Implement the minimal logic in `PermissionServiceImpl`.

## Task 2: Governance Query Enforcement

- [ ] Write failing tests for scoped `UserService` behavior:
  - `ORG_AND_CHILDREN` only returns users in the allowed organization subtree.
  - `SELF` only returns the current user.
- [ ] Write failing tests for scoped `OrganizationService` behavior:
  - `ORG_AND_CHILDREN` only returns the current organization subtree.
  - `ORG` and `SELF` only expose the current organization node.
- [ ] Add scoped service overloads and wire `UserController` / `OrganizationController` to authenticated user id.
- [ ] Keep write-side validation explicit: cross-scope target ids must be rejected with business errors.

## Task 3: Risk Point Query Enforcement

- [ ] Write failing tests for `RiskPointServiceImpl`:
  - `ORG_AND_CHILDREN` filters risk-point list/page by subtree org ids.
  - `SELF` only exposes records where `responsibleUser = currentUserId` or `createBy = currentUserId`.
- [ ] Wire `RiskPointController` list/page/get/delete/bind/unbind/bound-devices endpoints to authenticated user id.
- [ ] Implement scoped list/page/get/delete validation in `RiskPointServiceImpl`.
- [ ] Reuse the same organization-scope resolution for add/update org validation.

## Task 4: Verification And Docs

- [ ] Update `02 / 03 / 08 / 13` in place with the new “first-batch query-side enforcement” boundary.
- [ ] Run targeted backend tests for `PermissionServiceImplTest / UserServiceImplTest / OrganizationServiceImplTest / RiskPointServiceImplTest`.
- [ ] Run `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`.
