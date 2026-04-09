# Product Model Manual Extraction Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将产品物模型治理默认流程切换为“手动提炼 -> 正式模型 -> 确认写库”，支持当前选中产品下的单设备样本 JSON 手动提炼。

**Architecture:** 后端在 `spring-boot-iot-device` 中新增手动提炼接口与单设备样本解析逻辑，继续复用现有候选项和确认写库结构；前端在现有 `/products` 物模型治理工作台内切换默认入口，抽屉以手动提炼为主、正式模型为辅，不新增独立路由或草稿表。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis-Plus、Vue 3、TypeScript、Vitest、JUnit 5、Mockito

---

### Task 1: 后端手动提炼接口与解析逻辑

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelManualExtractDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelCandidateSummaryVO.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: 写失败测试，覆盖手动提炼主场景**

```java
@Test
void manualExtractShouldFlattenSingleDeviceSampleIntoPropertyCandidates() {
    ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
    dto.setSampleType("business");
    dto.setSamplePayload("{\"SK11\":{\"L1_QJ_1\":{\"2026-03-31T04:05:55.000Z\":{\"X\":-0.0376,\"AZI\":-8.6772}}}}");

    ProductModelCandidateResultVO result = productModelService.manualExtractModelCandidates(1001L, dto);

    assertEquals("SK11", result.getSummary().getSampleDeviceCode());
    assertEquals("manual", result.getSummary().getExtractionMode());
    assertEquals(2, result.getPropertyCandidates().size());
}
```

- [ ] **Step 2: 写失败测试，覆盖单次仅允许一个设备样本**

```java
@Test
void manualExtractShouldRejectMultipleDeviceRoots() {
    ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
    dto.setSampleType("business");
    dto.setSamplePayload("{\"A\":{\"L1_QJ_1\":{\"2026\":{\"X\":1}}},\"B\":{\"L1_QJ_1\":{\"2026\":{\"X\":2}}}}");

    BizException ex = assertThrows(BizException.class, () -> productModelService.manualExtractModelCandidates(1001L, dto));

    assertTrue(ex.getMessage().contains("单次只支持解析一个设备样本"));
}
```

- [ ] **Step 3: 实现 DTO、Controller、Service 接口和服务逻辑**

```java
@PostMapping("/api/device/product/{productId}/model-candidates/manual-extract")
public R<ProductModelCandidateResultVO> manualExtract(@PathVariable Long productId,
                                                      @RequestBody @Valid ProductModelManualExtractDTO dto) {
    return R.ok(productModelService.manualExtractModelCandidates(productId, dto));
}
```

- [ ] **Step 4: 实现样本解析与候选归一**

```java
private ManualSampleSnapshot parseManualSample(ProductModelManualExtractDTO dto) {
    JsonNode root = objectMapper.readTree(normalizeRequired(dto.getSamplePayload(), "样本报文"));
    if (!(root instanceof ObjectNode objectNode) || objectNode.size() != 1) {
        throw new BizException("单次只支持解析一个设备样本");
    }
    // 提取 deviceCode、时间层、标量叶子和值类型
}
```

- [ ] **Step 5: 运行后端目标测试**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelServiceImplTest,ProductModelControllerTest test`
Expected: `BUILD SUCCESS`

### Task 2: 前端工作台切换为手动提炼主流程

**Files:**
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`

- [ ] **Step 1: 写失败测试，覆盖抽屉中的手动提炼与确认入口**

```ts
it('extracts candidates from a manual sample payload', async () => {
  mockManualExtractProductModelCandidates.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: manualResult()
  })

  const wrapper = mountDrawer()
  await wrapper.find('[data-testid=\"manual-sample-input\"]').setValue(samplePayload)
  await wrapper.find('[data-testid=\"manual-extract-submit\"]').trigger('click')

  expect(mockManualExtractProductModelCandidates).toHaveBeenCalled()
  expect(wrapper.text()).toContain('L1_QJ_1.X')
})
```

- [ ] **Step 2: 写失败测试，覆盖工作台默认不再自动拉取运行期候选**

```ts
expect(mockListProductModelCandidates).not.toHaveBeenCalled()
expect(wrapper.text()).toContain('手动提炼')
```

- [ ] **Step 3: 增加手动提炼 API 与类型**

```ts
manualExtractProductModelCandidates(productId: IdType, payload: ProductModelManualExtractPayload) {
  return request<ProductModelCandidateResult>(`/api/device/product/${productId}/model-candidates/manual-extract`, {
    method: 'POST',
    body: payload
  })
}
```

- [ ] **Step 4: 将抽屉候选模式改为“手动提炼”**

```ts
const designerMode = ref<'manual' | 'formal'>('manual')
const manualSampleType = ref<ProductModelManualSampleType>('business')
const manualSamplePayload = ref('')
```

- [ ] **Step 5: 运行前端目标测试**

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: `2 passed`

### Task 3: 文档与验证收口

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`

- [ ] **Step 1: 补充手动提炼治理口径、接口和不落草稿表说明**

```md
- 产品物模型治理默认流程更新为：手动提炼 -> 正式模型 -> 确认写库
- 单次只解析一个设备样本，并始终作用于当前选中的产品
- “其他数据”可提炼，但默认 `needsReview=true`
```

- [ ] **Step 2: 运行本次改动的最终验证**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelServiceImplTest,ProductModelControllerTest test`
Expected: `BUILD SUCCESS`

Run: `pnpm --dir spring-boot-iot-ui vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: `passed`

- [ ] **Step 3: 自检交付项**

```text
1. 手动提炼接口返回现有候选结构
2. 正式模型读写仍落在 iot_product_model
3. 工作台默认入口已切为手动提炼
4. 文档已原位更新，无并行替代文档
```
