# Device Organization Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成设备归属机构落表、风险点唯一绑定和设备资产中心机构范围治理闭环，并同步前端、文档与验证。

**Architecture:** 以 `iot_device.org_id / org_name` 作为设备机构事实来源，设备写入链路统一继承当前登录人主机构；设备查询链路从租户侧升级为 `tenant + dataScopeType + org` 收口；风险点绑定链路强制“一设备一风险点 + 机构一致 + 已绑定设备不再出现在候选列表”，前端继续保持现有页面风格，只补展示与候选过滤。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Element Plus, Vitest, Maven

---

## File Structure

### Existing files to modify

- `sql/init.sql`
- `sql/init-data.sql`
- `scripts/run-real-env-schema-sync.py`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/Device.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceAddDTO.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceDetailVO.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DevicePageVO.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceOptionVO.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceMessageLogControllerTest.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceAccessErrorLogControllerTest.java`
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`
- `spring-boot-iot-ui/src/types/api.ts`
- `spring-boot-iot-ui/src/api/device.ts`
- `spring-boot-iot-ui/src/api/riskPoint.ts`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/RiskPointView.vue`
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/13-数据权限与多租户模型.md`
- `docs/21-业务功能清单与验收标准.md`

### Notes

- 当前分支固定为 `codex/dev`，不切换到 `master`，不使用 `git add .`。
- 当前仓库中设备归属机构与风险点唯一绑定已经有部分未提交实现，本计划的执行目标是“接管现有半落地状态并补齐闭环”，不是推翻重写。
- 当前工作区还存在与本任务无关的 `report`、UI 壳层等脏改动，执行时只能暂存本计划涉及文件。

## Task 1: 补齐数据库、实体与设备机构写入链路

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/Device.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceAddDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceDetailVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DevicePageVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceOptionVO.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`

- [ ] **Step 1: 先补设备服务失败测试，锁定机构回填与机构范围过滤**

Test cases:
- `addDeviceShouldAssignCurrentUserPrimaryOrganization`
- `addDeviceShouldRejectUserWithoutPrimaryOrganization`
- `pageDevicesShouldFilterByOrgScope`
- `getRequiredByIdShouldRejectCrossOrgDevice`

- [ ] **Step 2: 运行设备服务定向测试，确认新增断言先失败**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dtest=DeviceServiceImplTest" test
```

Expected:
- 先出现与 `org_id / org_name` 落库、组织范围过滤或缺字段相关的失败

- [ ] **Step 3: 补齐 `iot_device` 的 `org_id / org_name`、索引和 schema sync**

Required result:
- `sql/init.sql` 包含 `org_id / org_name`
- `sql/init.sql` 包含 `idx_device_tenant_org_deleted`
- `scripts/run-real-env-schema-sync.py` 能对旧库补齐这两个字段与索引
- `sql/init-data.sql` 的示例设备数据与新字段兼容

- [ ] **Step 4: 落实体、VO 与设备写入逻辑**

Required result:
- `Device` 实体包含 `orgId / orgName`
- `DeviceDetailVO / DevicePageVO / DeviceOptionVO` 返回 `orgId / orgName`
- `DeviceServiceImpl` 在新增、批量新增、编辑、设备更换时统一继承当前登录人的主机构
- `DeviceServiceImpl` 在 `ORG / ORG_AND_CHILDREN / SELF` 范围下按机构收口设备查询
- [ ] **Step 5: 重新运行设备服务定向测试，确认通过**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dtest=DeviceServiceImplTest" test
```

Expected:
- `BUILD SUCCESS`

## Task 2: 收口风险点唯一绑定与可绑定设备候选

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

- [ ] **Step 1: 先写风险点绑定失败测试**

Test cases:
- `bindDeviceShouldRejectDeviceBoundToOtherRiskPoint`
- `bindDeviceShouldRejectDeviceWithoutOrganization`
- `bindDeviceShouldRejectCrossOrganizationDevice`
- `listBindableDevicesShouldExcludeOccupiedDevicesAndKeepCurrentBindings`

- [ ] **Step 2: 运行风险点服务定向测试，确认当前断言先失败**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-alarm -am -DskipTests=false "-Dtest=RiskPointServiceImplTest" test
```

Expected:
- 先出现绑定唯一性、机构一致性或候选设备过滤相关失败

- [ ] **Step 3: 补齐服务与控制器实现**

Required result:
- `bindDevice` 强制一设备一风险点
- 绑定前校验设备机构存在且与风险点机构一致
- 新增可绑定设备查询接口或现有接口改为返回“当前风险点可继续选择”的设备集合
- 当前风险点已绑定设备允许回显，其他风险点已占用设备必须排除

- [ ] **Step 4: 重新运行风险点服务定向测试，确认通过**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-alarm -am -DskipTests=false "-Dtest=RiskPointServiceImplTest" test
```

Expected:
- `BUILD SUCCESS`

## Task 3: 补齐前端类型、设备详情展示与风险点候选过滤

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/device.ts`
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: 先写前端失败测试**

Test cases:
- `DeviceWorkbenchView` 详情区包含“所属机构”只读展示
- `DeviceWorkbenchView` 不出现设备归属机构编辑控件
- `RiskPointView` 绑定设备候选接口改为只消费后端可绑定设备清单
- `RiskPointView` 仍能回显当前风险点已绑定设备

- [ ] **Step 2: 运行前端定向测试，确认断言先失败**

Run:

```bash
cd spring-boot-iot-ui
npm test -- --run DeviceWorkbenchView RiskPointView
```

Expected:
- 先出现设备 org 展示或风险点候选过滤相关失败

- [ ] **Step 3: 补齐前端实现**

Required result:
- `Device` / `DeviceOption` / `DeviceAddPayload` 等类型与后端机构字段保持一致
- 设备详情抽屉展示 `orgName`
- 设备新增/编辑/更换抽屉不新增机构编辑控件
- 风险点页绑定设备选择器只消费“可绑定设备”接口结果

- [ ] **Step 4: 重新运行前端定向测试，确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm test -- --run DeviceWorkbenchView RiskPointView
```

Expected:
- 所有相关测试通过

## Task 4: 回填文档并完成整体验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 更新文档事实**

Required result:
- 设备归属正式来源改为 `iot_device.org_id / org_name`
- 风险点绑定明确为“一设备一风险点”
- 设备资产中心从租户侧升级为机构范围治理
- 真实环境回填和未绑定历史设备的边界写清楚

- [ ] **Step 2: 运行后端聚合打包**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:
- `BUILD SUCCESS`

- [ ] **Step 3: 运行本轮关键回归**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dtest=DeviceServiceImplTest,DeviceMessageServiceImplTest,DeviceAccessErrorLogServiceImplTest,DeviceMessageLogControllerTest,DeviceAccessErrorLogControllerTest" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-alarm -am -DskipTests=false "-Dtest=RiskPointServiceImplTest" test
```

Expected:
- 本轮设备与风险点治理相关定向测试全部通过

- [ ] **Step 4: 复核工作区边界**

Run:

```bash
git status --short
```

Expected:
- 仅包含本计划涉及文件和已知无关脏文件
