# 权限与密钥治理缺口收口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐治理权限矩阵读侧、密钥托管读侧与关键动作审计统一口径，并完成真实环境验收。

**Architecture:** 保持现有合同发布/回滚审批状态机不变，在 `system` 模块新增权限矩阵读侧，在 `device` 模块新增密钥轮换台账读侧，并统一密钥轮换审计 JSON 结构。前端只做轻量治理工作台表达，不新增重型系统。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Vitest, Maven

---

### Task 1: 治理权限矩阵读侧

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernancePermissionMatrixController.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernancePermissionMatrixService.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernancePermissionMatrixItemVO.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImplTest.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernancePermissionMatrixControllerTest.java`

- [ ] **Step 1: 写失败测试，定义矩阵最小协议**

```java
@Test
void listMatrixShouldExposeSecretCustodyAndContractDualControlRows() {
    GovernancePermissionMatrixServiceImpl service = new GovernancePermissionMatrixServiceImpl();

    List<GovernancePermissionMatrixItemVO> items = service.listMatrix();

    assertTrue(items.stream().anyMatch(item ->
            "iot:secret-custody:rotate".equals(item.getOperatorPermissionCode())
                    && "iot:secret-custody:approve".equals(item.getApproverPermissionCode())));
    assertTrue(items.stream().anyMatch(item ->
            "iot:product-contract:release".equals(item.getOperatorPermissionCode())
                    && "iot:product-contract:approve".equals(item.getApproverPermissionCode())));
}
```

- [ ] **Step 2: 运行失败测试，确认当前缺少实现**

Run: `mvn -pl spring-boot-iot-system -am "-Dtest=GovernancePermissionMatrixServiceImplTest,GovernancePermissionMatrixControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test`

Expected: `FAIL`，提示 `GovernancePermissionMatrixServiceImpl` / controller 尚不存在。

- [ ] **Step 3: 实现最小 VO、Service 和 Controller**

```java
@Data
public class GovernancePermissionMatrixItemVO {
    private String domainCode;
    private String domainName;
    private String actionCode;
    private String actionName;
    private String operatorPermissionCode;
    private String approverPermissionCode;
    private List<String> defaultRoleCodes;
    private List<String> defaultApproverRoleCodes;
    private Boolean dualControlRequired;
    private String auditModule;
}
```

```java
public interface GovernancePermissionMatrixService {
    List<GovernancePermissionMatrixItemVO> listMatrix();
}
```

```java
@RestController
public class GovernancePermissionMatrixController {

    private final GovernancePermissionMatrixService governancePermissionMatrixService;

    public GovernancePermissionMatrixController(GovernancePermissionMatrixService governancePermissionMatrixService) {
        this.governancePermissionMatrixService = governancePermissionMatrixService;
    }

    @GetMapping("/api/system/governance/permission-matrix")
    public R<List<GovernancePermissionMatrixItemVO>> listMatrix() {
        return R.ok(governancePermissionMatrixService.listMatrix());
    }
}
```

- [ ] **Step 4: 让测试转绿并补 controller 断言**

Run: `mvn -pl spring-boot-iot-system -am "-Dtest=GovernancePermissionMatrixServiceImplTest,GovernancePermissionMatrixControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test`

Expected: `PASS`

- [ ] **Step 5: 提交本任务**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernancePermissionMatrixController.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernancePermissionMatrixService.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernancePermissionMatrixItemVO.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImplTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernancePermissionMatrixControllerTest.java
git commit -m "feat: add governance permission matrix read model"
```

### Task 2: 密钥托管读侧

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceSecretRotationLogController.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceSecretRotationLogService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceSecretRotationLogServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceSecretRotationLogQuery.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceSecretRotationLogPageItemVO.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceSecretRotationLogServiceImplTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceSecretRotationLogControllerTest.java`

- [ ] **Step 1: 先写分页查询失败测试**

```java
@Test
void pageLogsShouldFilterByDeviceCodeAndHidePlainSecrets() {
    DeviceSecretRotationLogQuery query = new DeviceSecretRotationLogQuery();
    query.setDeviceCode("device-3001");

    PageResult<DeviceSecretRotationLogPageItemVO> page = service.pageLogs(1001L, query);

    assertEquals(1L, page.getTotal());
    assertEquals("device-3001", page.getList().get(0).getDeviceCode());
    assertNotNull(page.getList().get(0).getPreviousSecretDigest());
    assertNotNull(page.getList().get(0).getCurrentSecretDigest());
}
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl spring-boot-iot-device -am "-Dtest=DeviceSecretRotationLogServiceImplTest,DeviceSecretRotationLogControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test`

Expected: `FAIL`

- [ ] **Step 3: 实现 DTO、VO、Service 和 Controller**

```java
@Data
public class DeviceSecretRotationLogQuery extends PageQuery {
    private String deviceCode;
    private String productKey;
    private String rotationBatchId;
    private Long rotatedBy;
    private Long approvedBy;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
}
```

```java
@GetMapping("/api/device/secret-rotation-logs")
public R<PageResult<DeviceSecretRotationLogPageItemVO>> pageLogs(DeviceSecretRotationLogQuery query,
                                                                 Authentication authentication) {
    return R.ok(deviceSecretRotationLogService.pageLogs(requireCurrentUserId(authentication), query));
}
```

- [ ] **Step 4: 转绿并补权限守卫**

Run: `mvn -pl spring-boot-iot-device -am "-Dtest=DeviceSecretRotationLogServiceImplTest,DeviceSecretRotationLogControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test`

Expected: `PASS`

- [ ] **Step 5: 提交本任务**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceSecretRotationLogController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceSecretRotationLogService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceSecretRotationLogServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceSecretRotationLogQuery.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceSecretRotationLogPageItemVO.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceSecretRotationLogServiceImplTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceSecretRotationLogControllerTest.java
git commit -m "feat: add device secret rotation log read model"
```

### Task 3: 审计口径统一

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceSecretCustodyServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceSecretCustodyServiceImplTest.java`

- [ ] **Step 1: 先写失败测试，锁定稳定 JSON 审计结构**

```java
@Test
void rotateDeviceSecretShouldWriteStructuredAuditPayload() {
    service.rotateDeviceSecret(1001L, 3001L, 2002L, dto);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogService).addLog(captor.capture());
    assertTrue(captor.getValue().getRequestParams().contains("\"governanceAction\":\"DEVICE_SECRET_ROTATE\""));
    assertTrue(captor.getValue().getRequestParams().contains("\"dualControl\":true"));
}
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl spring-boot-iot-device -am "-Dtest=DeviceSecretCustodyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test`

Expected: `FAIL`

- [ ] **Step 3: 用对象序列化替代手拼 JSON**

```java
Map<String, Object> requestPayload = new LinkedHashMap<>();
requestPayload.put("governanceAction", "DEVICE_SECRET_ROTATE");
requestPayload.put("rotationBatchId", rotationBatchId);
requestPayload.put("approverUserId", approverUserId);
requestPayload.put("reason", reason);
requestPayload.put("dualControl", true);
auditLog.setRequestParams(writeAuditPayload(requestPayload));
```

- [ ] **Step 4: 运行测试确认转绿**

Run: `mvn -pl spring-boot-iot-device -am "-Dtest=DeviceSecretCustodyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test`

Expected: `PASS`

- [ ] **Step 5: 提交本任务**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceSecretCustodyServiceImpl.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceSecretCustodyServiceImplTest.java
git commit -m "refactor: normalize secret custody audit payload"
```

### Task 4: 前端、SQL 与文档同步

**Files:**
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Create or Modify: `spring-boot-iot-ui/src/views/GovernanceSecurityView.vue`
- Create or Modify: `spring-boot-iot-ui/src/api/governanceSecurity.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceSecurityView.test.ts`
- Modify: `sql/init-data.sql`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 先写前端失败测试，锁定矩阵与轮换台账的最小展示**

```ts
it('renders governance permission matrix and secret rotation log table', async () => {
  const wrapper = mount(GovernanceSecurityView)
  await flushPromises()

  expect(wrapper.text()).toContain('权限矩阵')
  expect(wrapper.text()).toContain('密钥轮换记录')
})
```

- [ ] **Step 2: 运行失败测试**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceSecurityView.test.ts`

Expected: `FAIL`

- [ ] **Step 3: 实现轻量页面与 API**

```ts
export function getGovernancePermissionMatrix() {
  return request('/api/system/governance/permission-matrix', { method: 'GET' })
}

export function pageDeviceSecretRotationLogs(params: Record<string, unknown>) {
  const query = new URLSearchParams(params as Record<string, string>).toString()
  return request(`/api/device/secret-rotation-logs${query ? `?${query}` : ''}`, { method: 'GET' })
}
```

- [ ] **Step 4: 跑前端测试并同步 SQL / 文档**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceSecurityView.test.ts`

Expected: `PASS`

- [ ] **Step 5: 统一验收**

Run:

```bash
mvn -pl spring-boot-iot-system,spring-boot-iot-device -am "-Dtest=GovernancePermissionMatrixServiceImplTest,GovernancePermissionMatrixControllerTest,DeviceSecretRotationLogServiceImplTest,DeviceSecretRotationLogControllerTest,DeviceSecretCustodyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipTests=false" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceSecurityView.test.ts
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected: all `PASS`

- [ ] **Step 6: 提交本任务**

```bash
git add spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/views/GovernanceSecurityView.vue spring-boot-iot-ui/src/api/governanceSecurity.ts spring-boot-iot-ui/src/__tests__/views/GovernanceSecurityView.test.ts sql/init-data.sql docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "feat: expose governance security read side"
```

### Task 5: 真实环境 smoke 与落地

**Files:**
- Reuse: `logs/acceptance/*`

- [ ] **Step 1: 启动 dev profile 后端并做真实 smoke**

Run:

```bash
java -jar spring-boot-iot-admin/target/spring-boot-iot-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port=10149
```

Smoke:

```bash
POST /api/auth/login
GET /api/system/governance/permission-matrix
GET /api/device/secret-rotation-logs?pageNum=1&pageSize=10
```

- [ ] **Step 2: 保存 smoke 结果到 `logs/acceptance`**

Expected: 三个接口都返回 `code=200`

- [ ] **Step 3: 合并完成后更新 `codex/dev`**

Run:

```bash
git update-ref refs/heads/codex/dev-backup-20260409-security-key-governance-merge <old-dev-sha>
git update-ref refs/heads/codex/dev <new-head-sha> <old-dev-sha>
```

## Self-Review

- Spec coverage: 已覆盖角色矩阵读侧、密钥台账读侧、审计统一化、前端展示、SQL 和文档、真实环境 smoke。
- Placeholder scan: 无 `TODO/TBD` 占位符。
- Type consistency: 权限矩阵使用 `GovernancePermissionMatrix*` 命名；密钥台账使用 `DeviceSecretRotationLog*` 命名；与执行范围一致。
