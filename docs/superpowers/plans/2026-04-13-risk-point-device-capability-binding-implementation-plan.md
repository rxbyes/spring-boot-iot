# 风险绑定工作台设备能力分流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让风险绑定工作台同时支持监测型测点绑定和预警/视频类设备级正式绑定，并为视频 AI 事件分析保留扩展位。

**Architecture:** 保持 `risk_point_device` 只服务监测型测点绑定，新增 `risk_point_device_capability_binding` 作为设备级正式绑定真相表；前后端按统一设备能力类型分流，并在绑定摘要/分组读侧聚合两类正式绑定。

**Tech Stack:** Spring Boot、MyBatis-Plus、Vue 3、Vitest、JUnit 5

---

### Task 1: 补齐后端数据模型与能力判定

**Files:**
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/device/DeviceBindingCapabilityType.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/device/DeviceBindingCapabilitySupport.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDeviceCapabilityBinding.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDeviceCapabilityBindingMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointDeviceCapabilityBindingRequest.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceOptionVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `sql/init.sql`

- [ ] 写后端失败测试，先覆盖能力判定与设备级正式绑定真相结构
- [ ] 新增设备能力枚举与关键词解析
- [ ] 给设备候选 VO 增补能力字段
- [ ] 新增设备级正式绑定表结构、实体和 Mapper

### Task 2: 扩展风险点正式绑定写侧

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java`

- [ ] 先补失败测试：预警型/视频类走设备级正式绑定；监测型禁止设备级绑定
- [ ] 新增 `POST /api/risk-point/bind-device-capability`
- [ ] 让审批载荷支持 `bindingMode=DEVICE_ONLY`
- [ ] 扩展整机解绑，让其同时删除测点级和设备级正式绑定

### Task 3: 聚合绑定摘要与绑定分组读侧

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingDeviceGroupVO.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingSummaryVO.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`

- [ ] 先补失败测试：摘要与分组能同时返回两类正式绑定
- [ ] 绑定摘要聚合设备总数与正式测点数
- [ ] 绑定分组返回 `bindingMode / deviceCapabilityType / aiEventExpandable / extensionStatus`
- [ ] 可绑定设备候选排除当前风险点已存在的设备级正式绑定

### Task 4: 前端工作台分流渲染

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Create: `spring-boot-iot-ui/src/utils/riskPointDeviceBindingCapability.ts`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointDetailDrawer.vue`

- [ ] 先补失败测试：监测型显示测点，预警/视频类隐藏测点并显示设备级正式绑定说明
- [ ] 新增前端设备能力解析与标签
- [ ] 监测型继续调 formal-metrics；预警/视频类改调设备级正式绑定提交
- [ ] 当前正式绑定列表与详情抽屉渲染设备级正式绑定卡片

### Task 5: 文档与验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] 更新正式绑定业务口径、接口、表结构和页面行为
- [ ] 运行后端定向测试
- [ ] 运行前端定向测试
- [ ] 运行 `git diff --check`

