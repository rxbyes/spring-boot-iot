# Product Model Dual Evidence Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/products` 的 `产品经营工作台 -> 物模型治理` 内落地“手动提炼 + 自动提炼”双证据并列治理，并通过统一的 compare/apply 链路完成 `property / event / service` 三类模型的新增、修订和人工裁决。

**Architecture:** 后端继续以 `spring-boot-iot-device` 为编排中心，新增 `model-governance/compare` 与 `model-governance/apply` 两个接口，并把比对规则收口到服务层的独立 helper，避免前端重复实现治理规则。前端继续复用现有 `/products` 工作台与 `ProductModelDesignerWorkspace/ProductModelDesignerDrawer` 入口，在同一抽屉内组织证据入口、治理摘要、对比工作区和正式确认区，不新增草稿表或独立路由。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis-Plus、Vue 3、TypeScript、Vitest、JUnit 5、Mockito

---

## File Map

### Backend

- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
  - compare 请求体，承接 `manualExtract`、`manualDraftItems`、`includeRuntimeCandidates`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceApplyDTO.java`
  - apply 请求体，承接 `decision=create|update|skip` 与最终写库字段
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java`
  - 统一构建 `compareRows`、`compareStatus`、`suggestedAction`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceEvidenceVO.java`
  - 单侧证据快照，复用到手动/自动/正式三方
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareRowVO.java`
  - 单行对比结果，承接状态、风险标记、建议动作和疑似同义提示
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceSummaryVO.java`
  - 顶部治理摘要
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareVO.java`
  - compare 接口总响应
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
  - apply 接口结果
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
  - 暴露 compare/apply 接口
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
  - 增加 compare/apply 服务签名
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
  - 编排 compare/apply 流程，继续复用既有手动提炼、运行期候选和正式模型读写
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
  - 控制器委托测试
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
  - compare/apply 行为测试

### Frontend

- Modify: `spring-boot-iot-ui/src/api/product.ts`
  - 新增 compare/apply API
- Modify: `spring-boot-iot-ui/src/types/api.ts`
  - 新增 compare/apply 类型定义
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
  - 保持声明同步
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  - 把工作台文案和摘要收口到“双证据治理”
- Create: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
  - 统一渲染 `property / event / service` 对比结果
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  - 组织证据入口、治理摘要、对比工作区、正式确认区
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
  - 工作台入口文案与默认治理说明
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
  - 对比表状态渲染、建议动作和类型切换
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
  - compare/apply 整体交互

### Docs

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Check: `README.md`
- Check: `AGENTS.md`

### Verification

- Backend unit: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelControllerTest,ProductModelServiceImplTest test`
- Frontend unit: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Package smoke: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
- Quality gates: `node scripts/run-quality-gates.mjs`

### Task 1: 建 compare/apply HTTP 合同与服务签名

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceApplyDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`

- [ ] **Step 1: 写失败测试，覆盖 compare/apply 控制器委托**

```java
@Test
void compareGovernanceShouldDelegateToService() {
    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareVO result = new ProductModelGovernanceCompareVO();
    result.setProductId(1001L);
    when(productModelService.compareGovernance(1001L, dto)).thenReturn(result);

    R<ProductModelGovernanceCompareVO> response = controller.compareGovernance(1001L, dto);

    assertEquals(1001L, response.getData().getProductId());
    verify(productModelService).compareGovernance(1001L, dto);
}

@Test
void applyGovernanceShouldDelegateToService() {
    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
    result.setCreatedCount(1);
    when(productModelService.applyGovernance(1001L, dto)).thenReturn(result);

    R<ProductModelGovernanceApplyResultVO> response = controller.applyGovernance(1001L, dto);

    assertEquals(1, response.getData().getCreatedCount());
    verify(productModelService).applyGovernance(1001L, dto);
}
```

- [ ] **Step 2: 运行控制器测试，确认编译失败点是新接口缺失**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelControllerTest test`
Expected: FAIL with `cannot find symbol` for `compareGovernance` / `applyGovernance`

- [ ] **Step 3: 创建 DTO/VO 骨架和服务签名**

```java
@Data
public class ProductModelGovernanceCompareDTO {

    private ManualExtractInput manualExtract;

    private List<ManualDraftItem> manualDraftItems;

    private Boolean includeRuntimeCandidates;

    @Data
    public static class ManualExtractInput {
        private String sampleType;
        private String samplePayload;
    }

    @Data
    public static class ManualDraftItem {
        private String modelType;
        private String identifier;
        private String modelName;
        private String dataType;
        private String eventType;
        private String serviceInputJson;
        private String serviceOutputJson;
        private String description;
    }
}
```

```java
@Data
public class ProductModelGovernanceApplyDTO {

    private List<ApplyItem> items;

    @Data
    public static class ApplyItem {
        private String decision;
        private Long targetModelId;
        private String modelType;
        private String identifier;
        private String modelName;
        private String dataType;
        private String specsJson;
        private String eventType;
        private String serviceInputJson;
        private String serviceOutputJson;
        private Integer sortNo;
        private Integer requiredFlag;
        private String description;
        private String compareStatus;
    }
}
```

```java
public interface ProductModelService extends IService<ProductModel> {

    ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto);

    ProductModelGovernanceApplyResultVO applyGovernance(Long productId, ProductModelGovernanceApplyDTO dto);
}
```

```java
@PostMapping("/api/device/product/{productId}/model-governance/compare")
public R<ProductModelGovernanceCompareVO> compareGovernance(@PathVariable Long productId,
                                                            @RequestBody ProductModelGovernanceCompareDTO dto) {
    return R.ok(productModelService.compareGovernance(productId, dto));
}

@PostMapping("/api/device/product/{productId}/model-governance/apply")
public R<ProductModelGovernanceApplyResultVO> applyGovernance(@PathVariable Long productId,
                                                              @RequestBody ProductModelGovernanceApplyDTO dto) {
    return R.ok(productModelService.applyGovernance(productId, dto));
}
```

- [ ] **Step 4: 运行控制器测试，确认合同层通过**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelControllerTest test`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交合同层改动**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceApplyDTO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java
git commit -m "feat(device): add product model governance endpoints"
```

### Task 2: 实现 compare 对比域模型与行状态计算

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceEvidenceVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareRowVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceSummaryVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: 写失败测试，覆盖双证据一致、运行期独有和正式已存在三类 compare 行**

```java
@Test
void compareGovernanceShouldBuildRowsAcrossManualRuntimeAndFormalEvidence() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L));
    when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "L1_QJ_1.angle", 10)));
    when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
            property(3001L, "S1_ZT_1.signal_4g", "4G 信号强度", "int", LocalDateTime.of(2026, 4, 1, 10, 0))
    ));
    when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
            messageLog("event", "{\"eventId\":\"alarmRaised\"}", LocalDateTime.of(2026, 4, 1, 10, 2))
    ));
    when(commandRecordMapper.selectList(any())).thenReturn(List.of(commandRecord("service", "reboot")));

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    dto.setIncludeRuntimeCandidates(true);
    dto.setManualDraftItems(List.of(manualDraftItem("service", "reboot", "重启设备")));

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

    assertEquals("formal_exists", compareRow(result, "property", "L1_QJ_1.angle").getCompareStatus());
    assertEquals("runtime_only", compareRow(result, "property", "S1_ZT_1.signal_4g").getCompareStatus());
    assertEquals("double_aligned", compareRow(result, "service", "reboot").getCompareStatus());
}
```

- [ ] **Step 2: 写失败测试，覆盖同标识但定义冲突时优先判为 suspected_conflict**

```java
@Test
void compareGovernanceShouldFlagSameIdentifierDefinitionMismatchAsConflict() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L));
    when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "alarmRaised", 10)));

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    dto.setManualDraftItems(List.of(manualEventDraft("alarmRaised", "warning")));
    when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of());
    when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(messageLog("event", "{\"eventId\":\"alarmRaised\"}", LocalDateTime.of(2026, 4, 1, 11, 0))));
    when(commandRecordMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

    assertEquals("suspected_conflict", compareRow(result, "event", "alarmRaised").getCompareStatus());
    assertEquals("人工裁决", compareRow(result, "event", "alarmRaised").getSuggestedAction());
}
```

- [ ] **Step 3: 运行服务测试，确认 compare 入口与 helper 尚未实现**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelServiceImplTest test`
Expected: FAIL with `cannot find symbol` for `compareGovernance` / compare VO fields

- [ ] **Step 4: 实现 compare 编排与对比 helper**

```java
public ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
    getRequiredProduct(productId);
    List<ProductModel> existingModels = listActiveModels(productId);
    ProductModelCandidateResultVO manualResult = buildManualGovernanceCandidates(productId, dto, existingModels);
    ProductModelCandidateResultVO runtimeResult = shouldLoadRuntimeCandidates(dto)
            ? listModelCandidates(productId)
            : emptyCandidateResult(productId, existingModels.size());

    return governanceComparator.compare(productId, existingModels, manualResult, runtimeResult);
}
```

```java
private boolean shouldLoadRuntimeCandidates(ProductModelGovernanceCompareDTO dto) {
    return dto == null || dto.getIncludeRuntimeCandidates() == null || dto.getIncludeRuntimeCandidates();
}

private ProductModelCandidateResultVO buildManualGovernanceCandidates(Long productId,
                                                                      ProductModelGovernanceCompareDTO dto,
                                                                      List<ProductModel> existingModels) {
    ProductModelCandidateResultVO result = dto != null && dto.getManualExtract() != null
            ? manualExtractModelCandidates(productId, toManualExtractDTO(dto.getManualExtract()))
            : emptyCandidateResult(productId, existingModels.size());
    mergeManualDraftItems(result, dto == null ? List.of() : dto.getManualDraftItems());
    return result;
}

private ProductModelCandidateResultVO emptyCandidateResult(Long productId, int existingModelCount) {
    ProductModelCandidateResultVO result = new ProductModelCandidateResultVO();
    result.setProductId(productId);
    ProductModelCandidateSummaryVO summary = new ProductModelCandidateSummaryVO();
    summary.setExistingModelCount(existingModelCount);
    result.setSummary(summary);
    return result;
}
```

```java
String resolveCompareStatus(ProductModelGovernanceEvidenceVO manual,
                            ProductModelGovernanceEvidenceVO runtime,
                            ProductModelGovernanceEvidenceVO formal) {
    if (formal != null && definitionMismatch(manual, runtime, formal)) return "suspected_conflict";
    if (formal != null) return "formal_exists";
    if (manual != null && runtime != null) return "double_aligned";
    if (manual != null) return "manual_only";
    if (runtime != null && runtime.getCandidateStatus() != null) return "runtime_only";
    return "evidence_insufficient";
}

boolean definitionMismatch(ProductModelGovernanceEvidenceVO manual,
                           ProductModelGovernanceEvidenceVO runtime,
                           ProductModelGovernanceEvidenceVO formal) {
    String manualType = manual == null ? null : manual.getDataType();
    String runtimeType = runtime == null ? null : runtime.getDataType();
    String formalType = formal == null ? null : formal.getDataType();
    return formal != null && ((manualType != null && !manualType.equals(formalType))
            || (runtimeType != null && !runtimeType.equals(formalType)));
}
```

```java
private ProductModelGovernanceCompareDTO.ManualDraftItem manualDraftItem(String modelType,
                                                                         String identifier,
                                                                         String modelName) {
    ProductModelGovernanceCompareDTO.ManualDraftItem item = new ProductModelGovernanceCompareDTO.ManualDraftItem();
    item.setModelType(modelType);
    item.setIdentifier(identifier);
    item.setModelName(modelName);
    return item;
}

private ProductModelGovernanceCompareDTO.ManualDraftItem manualEventDraft(String identifier, String eventType) {
    ProductModelGovernanceCompareDTO.ManualDraftItem item = manualDraftItem("event", identifier, "事件-" + identifier);
    item.setEventType(eventType);
    return item;
}

private ProductModelGovernanceCompareRowVO compareRow(ProductModelGovernanceCompareVO result,
                                                      String modelType,
                                                      String identifier) {
    return result.getCompareRows().stream()
            .filter(row -> modelType.equals(row.getModelType()) && identifier.equals(row.getIdentifier()))
            .findFirst()
            .orElseThrow();
}
```

- [ ] **Step 5: 运行 compare 服务测试，确认状态计算通过**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelServiceImplTest test`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: 提交 compare 逻辑改动**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceEvidenceVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareRowVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceSummaryVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "feat(device): add product model governance compare flow"
```

### Task 3: 实现 apply 决策写库与显式 create/update/skip

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceApplyDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: 写失败测试，覆盖 create/update/skip 三个 apply 分支**

```java
@Test
void applyGovernanceShouldCreateUpdateAndSkipExplicitDecisions() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L));
    when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "alarmRaised", 10)));
    when(productModelMapper.selectById(2001L)).thenReturn(existingModel(2001L, "alarmRaised", 10));

    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    dto.setItems(List.of(
            applyItem("create", null, "property", "S1_ZT_1.signal_4g", "4G 信号强度"),
            applyItem("update", 2001L, "event", "alarmRaised", "告警触发"),
            applyItem("skip", null, "service", "reboot", "重启设备")
    ));

    ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto);

    verify(productModelMapper).insert(any(ProductModel.class));
    verify(productModelMapper).updateById(any(ProductModel.class));
    assertEquals(1, result.getCreatedCount());
    assertEquals(1, result.getUpdatedCount());
    assertEquals(1, result.getSkippedCount());
}
```

- [ ] **Step 2: 写失败测试，覆盖 update 缺少 targetModelId 的拒绝逻辑**

```java
@Test
void applyGovernanceShouldRejectUpdateWithoutTargetModelId() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L));

    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    dto.setItems(List.of(applyItem("update", null, "event", "alarmRaised", "告警触发")));

    BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(1001L, dto));

    assertEquals("治理修订必须指定 targetModelId", ex.getMessage());
}
```

- [ ] **Step 3: 运行服务测试，确认 apply 入口尚未实现**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelServiceImplTest test`
Expected: FAIL with `cannot find symbol method applyGovernance`

- [ ] **Step 4: 实现 apply 决策分支**

```java
public ProductModelGovernanceApplyResultVO applyGovernance(Long productId, ProductModelGovernanceApplyDTO dto) {
    getRequiredProduct(productId);
    int created = 0;
    int updated = 0;
    int skipped = 0;
    for (ProductModelGovernanceApplyDTO.ApplyItem item : safeItems(dto)) {
        switch (normalizeRequired(item.getDecision(), "治理决策")) {
            case "create" -> { createFromGovernanceItem(productId, item); created++; }
            case "update" -> { updateFromGovernanceItem(productId, item); updated++; }
            case "skip" -> skipped++;
            default -> throw new BizException("治理决策不支持: " + item.getDecision());
        }
    }
    ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
    result.setCreatedCount(created);
    result.setUpdatedCount(updated);
    result.setSkippedCount(skipped);
    result.setLastAppliedAt(LocalDateTime.now());
    return result;
}
```

```java
private void createFromGovernanceItem(Long productId, ProductModelGovernanceApplyDTO.ApplyItem item) {
    ProductModelUpsertDTO upsertDTO = toUpsertDTO(item);
    createModel(productId, upsertDTO);
}

private void updateFromGovernanceItem(Long productId, ProductModelGovernanceApplyDTO.ApplyItem item) {
    if (item.getTargetModelId() == null) {
        throw new BizException("治理修订必须指定 targetModelId");
    }
    ProductModelUpsertDTO upsertDTO = toUpsertDTO(item);
    updateModel(productId, item.getTargetModelId(), upsertDTO);
}

private ProductModelUpsertDTO toUpsertDTO(ProductModelGovernanceApplyDTO.ApplyItem item) {
    ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
    dto.setModelType(item.getModelType());
    dto.setIdentifier(item.getIdentifier());
    dto.setModelName(item.getModelName());
    dto.setDataType(item.getDataType());
    dto.setSpecsJson(item.getSpecsJson());
    dto.setEventType(item.getEventType());
    dto.setServiceInputJson(item.getServiceInputJson());
    dto.setServiceOutputJson(item.getServiceOutputJson());
    dto.setSortNo(item.getSortNo());
    dto.setRequiredFlag(item.getRequiredFlag());
    dto.setDescription(item.getDescription());
    return dto;
}

private ProductModelGovernanceApplyDTO.ApplyItem applyItem(String decision,
                                                           Long targetModelId,
                                                           String modelType,
                                                           String identifier,
                                                           String modelName) {
    ProductModelGovernanceApplyDTO.ApplyItem item = new ProductModelGovernanceApplyDTO.ApplyItem();
    item.setDecision(decision);
    item.setTargetModelId(targetModelId);
    item.setModelType(modelType);
    item.setIdentifier(identifier);
    item.setModelName(modelName);
    return item;
}

private List<ProductModelGovernanceApplyDTO.ApplyItem> safeItems(ProductModelGovernanceApplyDTO dto) {
    return dto == null || dto.getItems() == null ? List.of() : dto.getItems();
}
```

- [ ] **Step 5: 运行 apply 服务测试，确认显式决策通过**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelServiceImplTest test`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: 提交 apply 逻辑改动**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceApplyDTO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "feat(device): add product model governance apply flow"
```

### Task 4: 前端补齐 compare/apply API 与工作台入口语义

**Files:**
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: 写失败测试，覆盖工作台入口切到“双证据治理”**

```ts
it('shows dual-evidence governance copy in the workbench header', async () => {
  const wrapper = mountWorkspace()
  await flushPromises()
  await nextTick()

  expect(wrapper.text()).toContain('双证据治理')
  expect(wrapper.text()).toContain('手动提炼 + 自动提炼')
  expect(wrapper.text()).toContain('进入完整治理')
})
```

- [ ] **Step 2: 运行工作台测试，确认新文案和 API 类型尚未存在**

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
Expected: FAIL with missing copy assertions or missing governance type exports

- [ ] **Step 3: 增加 compare/apply API 与类型**

```ts
export interface ProductModelGovernanceManualDraftItem {
  modelType: ProductModelType
  identifier: string
  modelName: string
  dataType?: string
  eventType?: string
  serviceInputJson?: string
  serviceOutputJson?: string
  description?: string
}

export interface ProductModelGovernanceComparePayload {
  manualExtract?: {
    sampleType: ProductModelManualSampleType
    samplePayload: string
  } | null
  manualDraftItems?: ProductModelGovernanceManualDraftItem[]
  includeRuntimeCandidates?: boolean
}

export interface ProductModelGovernanceSummary {
  doubleAlignedCount: number
  manualOnlyCount: number
  runtimeOnlyCount: number
  formalExistsCount: number
  conflictCount: number
  evidenceInsufficientCount: number
}

export interface ProductModelGovernanceCompareRow {
  modelType: ProductModelType
  identifier: string
  compareStatus: string
  suggestedAction: string
  decision?: 'create' | 'update' | 'skip'
  formalModel?: ProductModel | null
  finalModelName?: string
  finalDataType?: string
  finalDescription?: string
}

export interface ProductModelGovernanceCompareResult {
  productId: IdType
  summary: ProductModelGovernanceSummary
  compareRows: ProductModelGovernanceCompareRow[]
}

export interface ProductModelGovernanceApplyItem {
  decision: 'create' | 'update' | 'skip'
  targetModelId?: IdType
  modelType: ProductModelType
  identifier: string
  modelName: string
  dataType?: string
  description?: string
}

export interface ProductModelGovernanceApplyPayload {
  items: ProductModelGovernanceApplyItem[]
}

export interface ProductModelGovernanceApplyResult {
  createdCount: number
  updatedCount: number
  skippedCount: number
  conflictCount: number
}

compareProductModelGovernance(productId: IdType, payload: ProductModelGovernanceComparePayload) {
  return request<ProductModelGovernanceCompareResult>(`/api/device/product/${productId}/model-governance/compare`, {
    method: 'POST',
    body: payload
  })
}

applyProductModelGovernance(productId: IdType, payload: ProductModelGovernanceApplyPayload) {
  return request<ProductModelGovernanceApplyResult>(`/api/device/product/${productId}/model-governance/apply`, {
    method: 'POST',
    body: payload
  })
}
```

- [ ] **Step 4: 更新工作台头部文案和摘要口径**

```ts
const designerStageTitle = computed(() =>
  designerMode.value === 'manual' ? '双证据治理入口' : '统一维护产品正式物模型'
)

const headerStatement = computed(() =>
  designerMode.value === 'formal'
    ? '正式模型继续维持当前契约总表，新增或修订都先回到双证据治理核对来源证据。'
    : '手动提炼与自动提炼并列进入同一治理会话，再统一确认新增或修订。'
)
```

- [ ] **Step 5: 运行工作台测试，确认入口语义通过**

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
Expected: `1 passed`

- [ ] **Step 6: 提交 API 与工作台入口改动**

```bash
git add spring-boot-iot-ui/src/api/product.ts \
        spring-boot-iot-ui/src/types/api.ts \
        spring-boot-iot-ui/src/types/api.d.ts \
        spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue \
        spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "feat(ui): add product model governance API contracts"
```

### Task 5: 落地抽屉内的双证据治理工作区

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`

- [ ] **Step 1: 写失败测试，覆盖 compare 结果渲染与 apply 提交**

```ts
it('loads compare rows and applies explicit create/update decisions', async () => {
  mockCompareProductModelGovernance.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: governanceResult()
  })
  mockApplyProductModelGovernance.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { createdCount: 1, updatedCount: 1, skippedCount: 0, conflictCount: 0 }
  })

  const wrapper = mountDrawer()
  await wrapper.find('[data-testid="manual-sample-input"] textarea').setValue(samplePayload)
  await wrapper.find('[data-testid="governance-compare-submit"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(wrapper.text()).toContain('双证据一致')
  expect(wrapper.text()).toContain('人工裁决')
  await wrapper.find('[data-testid="governance-apply-submit"]').trigger('click')
  expect(mockApplyProductModelGovernance).toHaveBeenCalled()
})
```

- [ ] **Step 2: 写失败测试，覆盖对比表的类型切换与建议动作展示**

```ts
it('switches compare rows by model type and renders suggested actions', async () => {
  const wrapper = mountCompareTable({ rows: governanceRows() })

  expect(wrapper.text()).toContain('S1_ZT_1.signal_4g')
  await wrapper.find('[data-testid="compare-type-event"]').trigger('click')
  expect(wrapper.text()).toContain('alarmRaised')
  expect(wrapper.text()).toContain('纳入修订')
})
```

- [ ] **Step 3: 运行前端抽屉测试，确认 compare/apply 流程尚未接通**

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: FAIL with missing component or missing `compareProductModelGovernance` / `applyProductModelGovernance`

- [ ] **Step 4: 创建对比表组件并重构抽屉状态**

```ts
const compareResult = ref<ProductModelGovernanceCompareResult | null>(null)
const activeCompareType = ref<ProductModelType>('property')
const selectedDecisionItems = computed(() =>
  (compareResult.value?.compareRows ?? [])
    .filter((row) => row.decision === 'create' || row.decision === 'update')
    .map((row) => ({
      decision: row.decision,
      targetModelId: row.formalModel?.id ?? undefined,
      modelType: row.modelType,
      identifier: row.identifier,
      modelName: row.finalModelName,
      dataType: row.finalDataType,
      description: row.finalDescription
    }))
)

function handleDecisionChange(identifier: string, decision: 'create' | 'update' | 'skip') {
  if (!compareResult.value) return
  compareResult.value.compareRows = compareResult.value.compareRows.map((row) =>
    row.identifier === identifier ? { ...row, decision } : row
  )
}
```

```vue
<ProductModelGovernanceCompareTable
  v-if="compareResult"
  v-model:active-type="activeCompareType"
  :rows="compareResult.compareRows"
  @update-decision="handleDecisionChange"
/>
```

```ts
const {
  mockCompareProductModelGovernance,
  mockApplyProductModelGovernance
} = vi.hoisted(() => ({
  mockCompareProductModelGovernance: vi.fn(),
  mockApplyProductModelGovernance: vi.fn()
}))

const samplePayload = '{"SK11E80D1307426AZ":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"X":-0.0376}}}}'

function governanceRows() {
  return [
    {
      modelType: 'property',
      identifier: 'S1_ZT_1.signal_4g',
      compareStatus: 'runtime_only',
      suggestedAction: '继续观察',
      decision: 'create'
    },
    {
      modelType: 'event',
      identifier: 'alarmRaised',
      compareStatus: 'suspected_conflict',
      suggestedAction: '人工裁决',
      decision: 'update'
    }
  ]
}

function governanceResult() {
  return {
    productId: 1001,
    summary: {
      doubleAlignedCount: 1,
      runtimeOnlyCount: 1,
      conflictCount: 1
    },
    compareRows: governanceRows()
  }
}
```

- [ ] **Step 5: 运行前端目标测试，确认 compare/apply 交互通过**

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: `2 passed`

- [ ] **Step 6: 提交抽屉工作区改动**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue \
        spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue \
        spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
        spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
git commit -m "feat(ui): add dual evidence governance workspace"
```

### Task 6: 原位更新文档并做最终验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Check: `README.md`
- Check: `AGENTS.md`

- [ ] **Step 1: 原位补充 compare/apply 流程、接口与治理边界**

```md
- `/products` 内物模型治理升级为“手动提炼 + 自动提炼 -> 对比治理 -> 正式确认”
- 新增 `POST /api/device/product/{productId}/model-governance/compare`
- 新增 `POST /api/device/product/{productId}/model-governance/apply`
- 对象洞察台继续只提供属性快照证据，不直接写入正式模型
- `继续观察 / 忽略` 只存在于治理会话中，不新增草稿表
```

- [ ] **Step 2: 检查 `README.md` 与 `AGENTS.md` 是否需要同步默认治理流程**

```text
1. 若 README 仍写“默认仅手动提炼为前台入口”，则更新为“双证据并列治理”
2. 若 AGENTS 仍写“运行期候选不再作为默认 UI 入口”，则改为“运行期候选回归为并列证据入口，但仍非自动落库入口”
```

- [ ] **Step 3: 运行后端与前端目标测试**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelControllerTest,ProductModelServiceImplTest test`
Expected: `BUILD SUCCESS`

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: `passed`

- [ ] **Step 4: 运行打包与质量门禁**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
Expected: `BUILD SUCCESS`

Run: `node scripts/run-quality-gates.mjs`
Expected: exit code `0`

- [ ] **Step 5: 提交文档与收尾验证改动**

```bash
git add docs/02-业务功能与流程说明.md \
        docs/03-接口规范与接口清单.md \
        docs/04-数据库设计与初始化数据.md \
        docs/06-前端开发与CSS规范.md \
        docs/08-变更记录与技术债清单.md \
        docs/15-前端优化与治理计划.md \
        README.md AGENTS.md
git commit -m "docs: document dual evidence product model governance"
```
