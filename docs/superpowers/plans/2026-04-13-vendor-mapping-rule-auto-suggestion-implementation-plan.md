# Vendor Mapping Rule Auto Suggestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为单产品提供基于运行时字段证据的厂商字段映射规则自动建议读侧，并保持“只读 preview、人工确认后再建规则”的治理边界。

**Architecture:** 在 `spring-boot-iot-device` 中新增独立的 suggestion service，统一读取 `iot_vendor_metric_evidence`、已发布 resolver snapshot、规范场景定义和现有 mapping rules，输出 `READY_TO_CREATE / ALREADY_COVERED / CONFLICTS_WITH_EXISTING / LOW_CONFIDENCE / IGNORED_*` 状态。控制器只暴露只读接口，不自动写规则、不触发审批，也不改变 `PAYLOAD_APPLY`、compare/apply 现有真相。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JUnit 5, Mockito, Maven

---

## File Structure

### Create

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\VendorMetricMappingRuleSuggestionService.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleSuggestionServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\VendorMetricMappingRuleSuggestionVO.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleSuggestionServiceImplTest.java`

### Modify

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleController.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleControllerTest.java`
- `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

## Task 1: Add the suggestion service contract and failing tests

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\VendorMetricMappingRuleSuggestionService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\VendorMetricMappingRuleSuggestionVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleSuggestionServiceImplTest.java`

- [ ] **Step 1: Write the failing service tests**

```java
@Test
void listSuggestionsShouldReturnReadyToCreateForPublishedCanonicalEvidence() {
    when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
    when(evidenceMapper.selectList(any())).thenReturn(List.of(
            evidence("disp", "value", "L1_LF_1", 4, "0.2136")
    ));
    when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.builder()
            .productId(1001L)
            .releaseBatchId(9001L)
            .publishedIdentifier("value")
            .canonicalAlias("disp", "value")
            .build());
    when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1"))).thenReturn(null);

    List<VendorMetricMappingRuleSuggestionVO> result =
            service.listSuggestions(1001L, false, false, 1);

    assertEquals(1, result.size());
    assertEquals("READY_TO_CREATE", result.get(0).getStatus());
    assertEquals("PRODUCT", result.get(0).getRecommendedScopeType());
    assertEquals("high", result.get(0).getConfidence());
}
```

```java
@Test
void listSuggestionsShouldReturnAlreadyCoveredWhenGovernanceResolverMatchesExistingRule() {
    when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
    when(evidenceMapper.selectList(any())).thenReturn(List.of(
            evidence("disp", "value", "L1_LF_1", 5, "0.2136")
    ));
    when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.builder()
            .productId(1001L)
            .releaseBatchId(9001L)
            .publishedIdentifier("value")
            .build());
    when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1")))
            .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(7001L, "value", "disp", "L1_LF_1"));

    List<VendorMetricMappingRuleSuggestionVO> result =
            service.listSuggestions(1001L, true, false, 1);

    assertEquals("ALREADY_COVERED", result.get(0).getStatus());
    assertEquals(7001L, result.get(0).getExistingRuleId());
}
```

- [ ] **Step 2: Run the targeted test to verify RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuleSuggestionServiceImplTest" test
```

Expected: `BUILD FAILURE` because suggestion service and VO do not exist yet.

- [ ] **Step 3: Add the minimal contract types**

```java
public interface VendorMetricMappingRuleSuggestionService {

    List<VendorMetricMappingRuleSuggestionVO> listSuggestions(Long productId,
                                                              boolean includeCovered,
                                                              boolean includeIgnored,
                                                              int minEvidenceCount);
}
```

```java
@Data
public class VendorMetricMappingRuleSuggestionVO {
    private String rawIdentifier;
    private String logicalChannelCode;
    private String targetNormativeIdentifier;
    private String recommendedScopeType;
    private String status;
    private String confidence;
    private Integer evidenceCount;
    private String sampleValue;
    private String valueType;
    private String evidenceOrigin;
    private LocalDateTime lastSeenTime;
    private String reason;
    private Long existingRuleId;
    private String existingTargetNormativeIdentifier;
}
```

- [ ] **Step 4: Run the test again and keep it failing at implementation level**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuleSuggestionServiceImplTest" test
```

Expected: test compiles, then fails because service implementation still missing behavior.

## Task 2: Implement the suggestion engine

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleSuggestionServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleSuggestionServiceImplTest.java`

- [ ] **Step 1: Extend the failing tests for conflict / ignored / low-confidence**

```java
@Test
void listSuggestionsShouldReturnConflictsWithExistingWhenResolverPointsToDifferentCanonical() {
    when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1")))
            .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(7002L, "sensor_state", "disp", "L1_LF_1"));

    List<VendorMetricMappingRuleSuggestionVO> result =
            service.listSuggestions(1001L, true, false, 1);

    assertEquals("CONFLICTS_WITH_EXISTING", result.get(0).getStatus());
    assertEquals("sensor_state", result.get(0).getExistingTargetNormativeIdentifier());
}
```

```java
@Test
void listSuggestionsShouldFilterIgnoredRowsByDefault() {
    when(evidenceMapper.selectList(any())).thenReturn(List.of(
            evidence("value", "value", "L1_LF_1", 3, "0.2136")
    ));

    List<VendorMetricMappingRuleSuggestionVO> result =
            service.listSuggestions(1001L, false, false, 1);

    assertTrue(result.isEmpty());
}
```

- [ ] **Step 2: Run the service tests to confirm RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuleSuggestionServiceImplTest" test
```

Expected: `BUILD FAILURE` or test failures showing missing status classification.

- [ ] **Step 3: Implement the minimal suggestion service**

```java
@Service
public class VendorMetricMappingRuleSuggestionServiceImpl implements VendorMetricMappingRuleSuggestionService {

    @Override
    public List<VendorMetricMappingRuleSuggestionVO> listSuggestions(Long productId,
                                                                     boolean includeCovered,
                                                                     boolean includeIgnored,
                                                                     int minEvidenceCount) {
        Product product = requireProduct(productId);
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(productId);
        Set<String> normativeIdentifiers = loadNormativeIdentifiers(product);
        return loadEvidence(productId).stream()
                .map(evidence -> toSuggestion(product, snapshot, normativeIdentifiers, evidence))
                .filter(Objects::nonNull)
                .filter(item -> includeCovered || !"ALREADY_COVERED".equals(item.getStatus()))
                .filter(item -> includeIgnored || !item.getStatus().startsWith("IGNORED_"))
                .filter(item -> firstPositive(item.getEvidenceCount()) >= Math.max(1, minEvidenceCount))
                .sorted(suggestionComparator())
                .toList();
    }
}
```

Implementation rules:
- `rawIdentifier == canonicalIdentifier` -> `IGNORED_SAME_IDENTIFIER`
- canonical not in snapshot or normative identifiers -> `IGNORED_UNKNOWN_CANONICAL`
- `runtimeService.resolveForGovernance(...)` match same target -> `ALREADY_COVERED`
- resolver match different target or throws conflict `BizException` -> `CONFLICTS_WITH_EXISTING`
- legal candidate + `evidenceCount <= 1` -> `LOW_CONFIDENCE`
- legal candidate + no existing coverage + enough evidence -> `READY_TO_CREATE`

- [ ] **Step 4: Run the service tests to verify GREEN**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuleSuggestionServiceImplTest" test
```

Expected: PASS.

## Task 3: Expose the read-only API and document it

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleController.java`
- Modify: `E:\idea\\ghatg\\spring-boot-iot\\spring-boot-iot-device\\src\\test\\java\\com\\ghlzm\\iot\\device\\controller\\VendorMetricMappingRuleControllerTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`

- [ ] **Step 1: Add the failing controller test**

```java
@Test
void listSuggestionsShouldRequireGovernPermissionAndDelegateToService() {
    VendorMetricMappingRuleSuggestionVO row = new VendorMetricMappingRuleSuggestionVO();
    row.setRawIdentifier("disp");
    row.setStatus("READY_TO_CREATE");
    when(suggestionService.listSuggestions(1001L, false, false, 1)).thenReturn(List.of(row));

    R<List<VendorMetricMappingRuleSuggestionVO>> response =
            controller.listSuggestions(1001L, false, false, 1, authentication(10001L));

    assertEquals("READY_TO_CREATE", response.getData().get(0).getStatus());
    verify(permissionGuard).requireAnyPermission(10001L, "厂商字段映射规则建议", "iot:product-contract:govern");
}
```

- [ ] **Step 2: Run the controller test to confirm RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuleControllerTest" test
```

Expected: `BUILD FAILURE` because controller method and dependency are missing.

- [ ] **Step 3: Implement the controller endpoint and docs**

```java
@GetMapping("/api/device/product/{productId}/vendor-mapping-rule-suggestions")
public R<List<VendorMetricMappingRuleSuggestionVO>> listSuggestions(@PathVariable Long productId,
                                                                    @RequestParam(defaultValue = "false") boolean includeCovered,
                                                                    @RequestParam(defaultValue = "false") boolean includeIgnored,
                                                                    @RequestParam(defaultValue = "1") Integer minEvidenceCount,
                                                                    Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    permissionGuard.requireAnyPermission(currentUserId, "厂商字段映射规则建议", GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN);
    return R.ok(suggestionService.listSuggestions(productId, includeCovered, includeIgnored, minEvidenceCount == null ? 1 : minEvidenceCount));
}
```

Docs must state:
- endpoint path and query parameters
- status meanings
- “只读 preview，不自动建规则”

- [ ] **Step 4: Run focused verification**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuleSuggestionServiceImplTest,VendorMetricMappingRuleControllerTest" test
git diff --check
```

Expected: PASS, with no whitespace errors beyond existing CRLF warnings.
