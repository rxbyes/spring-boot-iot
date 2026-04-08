# Governance Approval Execution Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把合同生效与合同回滚改成“先提交审批、审批通过后执行”的控制面写侧闭环，并同步把 `/products` 工作台切到待审批语义。

**Architecture:** `spring-boot-iot-system` 负责审批单状态机与执行 SPI，`spring-boot-iot-device` 负责把产品合同 apply/rollback 载荷序列化并在审批通过时真正执行业务。控制器只提交审批单，审批执行结果回写到审批单 `payloadJson`，前端只展示待审批回执，不再假定即时生效。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Vitest, Maven

---

### Task 1: 锁定后端待审批语义

**Files:**
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImplTest.java`

- [ ] **Step 1: 写控制器红灯测试**

```java
when(governanceApprovalService.submitAction(any())).thenReturn(88001L);
R<ProductModelGovernanceApplyResultVO> response = controller.applyGovernance(1001L, dto, 2002L, authentication);
assertEquals("PENDING", response.getData().getApprovalStatus());
assertEquals(Boolean.TRUE, response.getData().getExecutionPending());
verify(governanceApprovalService).submitAction(any());
verify(productModelService, never()).applyGovernance(any(), any(), any());
```

- [ ] **Step 2: 写审批服务红灯测试**

```java
when(orderMapper.selectById(88001L)).thenReturn(order);
when(executor.execute(order)).thenReturn(new GovernanceApprovalActionExecutionResult("{\"result\":true}"));
service.approveOrder(88001L, 20002L, "approve");
verify(executor).execute(order);
verify(orderMapper).updateById(argThat(updated -> "APPROVED".equals(updated.getStatus())));
```

- [ ] **Step 3: 运行红灯测试确认旧行为失败**

Run: `mvn -pl spring-boot-iot-device "-Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest" "-DskipTests=false" "-Dmaven.test.skip=false" test`

Run: `mvn -pl spring-boot-iot-system "-Dtest=GovernanceApprovalServiceImplTest" "-DskipTests=false" "-Dmaven.test.skip=false" test`

Expected: 至少出现 “wanted but not invoked submitAction” 与 “expected executor.execute” 一类失败。

### Task 2: 实现审批执行 SPI 与设备执行器

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceApprovalActionExecutor.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceApprovalActionExecutionResult.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractGovernanceApprovalExecutor.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductContractReleaseController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductContractReleaseRollbackResultVO.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractGovernanceApprovalExecutorTest.java`

- [ ] **Step 1: 定义系统侧 SPI**

```java
public interface GovernanceApprovalActionExecutor {
    boolean supports(String actionCode);
    GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order);
}
```

- [ ] **Step 2: 在审批通过时执行并回写 payload**

```java
GovernanceApprovalActionExecutionResult executionResult = executeAction(order);
approvedOrder.setPayloadJson(executionResult == null ? order.getPayloadJson() : executionResult.payloadJson());
```

- [ ] **Step 3: 控制器改为 submit-only**

```java
Long approvalOrderId = governanceApprovalService.submitAction(command);
result.setApprovalOrderId(approvalOrderId);
result.setApprovalStatus("PENDING");
result.setExecutionPending(Boolean.TRUE);
result.setReleaseBatchId(null);
```

- [ ] **Step 4: 设备执行器复用原业务服务**

```java
ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(payload.productId(), payload.request(), order.getOperatorUserId());
return new GovernanceApprovalActionExecutionResult(writePayload(payload.withResult(result)));
```

- [ ] **Step 5: 运行绿灯测试**

Run: `mvn -pl spring-boot-iot-device "-Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest,ProductContractGovernanceApprovalExecutorTest" "-DskipTests=false" "-Dmaven.test.skip=false" test`

Run: `mvn -pl spring-boot-iot-system "-Dtest=GovernanceApprovalServiceImplTest,GovernanceApprovalControllerTest,GovernanceApprovalQueryServiceImplTest" "-DskipTests=false" "-Dmaven.test.skip=false" test`

Expected: 相关单测全部通过。

### Task 3: 切前端待审批回执与文档

**Files:**
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 更新前端类型**

```ts
approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | null
executionPending?: boolean | null
targetBatchId?: IdType | null
```

- [ ] **Step 2: 调整工作台文案**

```ts
ElMessage.success('合同发布审批已提交')
ElMessage.success('合同回滚审批已提交')
```

- [ ] **Step 3: 调整 UI 断言**

```ts
expect(wrapper.text()).toContain('待审批')
expect(wrapper.text()).toContain('审批单')
expect(wrapper.text()).not.toContain('合同字段已生效')
```

- [ ] **Step 4: 跑前端与总构建验证**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`

Expected: UI 定向测试通过，后端聚合构建通过。
