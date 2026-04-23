# 雨量计契约字段双模式实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or inline execution to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复雨量计状态数据提炼中 full-path / 短标识不一致的问题，并统一 compare/apply 与前端文案。

**Architecture:** 先用测试锁定雨量计与采集器两条关键路径的 identifier 口径，再在 `ProductModelServiceImpl` 里引入自动识别上下文与统一归一入口。compare 负责决定并回显模式，apply 负责校验最终 identifier；前端只做文案和 DTO 同步，不新增交互切换。

**Tech Stack:** Spring Boot 4, Java 17, Vue 3, TypeScript, JUnit 5, Mockito

---

### Task 1: 写后端 failing tests

**Files:**
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Add a rain-gauge regression test**

```java
@Test
void compareGovernanceShouldKeepRainGaugeStatusFullPathIdentifiersConsistentAcrossSuggestedActions() {
    // sample: {"SK00...":{"S1_ZT_1":{"2026-04-13T17:34:04.000Z":{"ext_power_volt":12.12,"humidity":89.04}}}}
    // expect identifiers to stay on S1_ZT_1.ext_power_volt / S1_ZT_1.humidity
}
```

- [ ] **Step 2: Add a direct-collector guard test**

```java
@Test
void compareGovernanceShouldKeepDirectCollectorRtuStatusFieldsAsFullPathInSingleMode() {
    // expect S1_ZT_1.ext_power_volt instead of ext_power_volt when the product is single-mode auto-recognized
}
```

- [ ] **Step 3: Run the focused test class**

Run:
```powershell
mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest test
```

Expected: the two new tests fail because current code still shortens status identifiers in one path.

### Task 2: Implement backend identifier mode unification

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelCandidateSummaryVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java`

- [ ] **Step 1: Add the minimal DTO / summary fields**

```java
private String contractIdentifierMode;
private String resolvedContractIdentifierMode;
```

- [ ] **Step 2: Add the smallest compare-context helper**

```java
private record ContractIdentifierContext(String requestedMode, String resolvedMode) {}
```

- [ ] **Step 3: Route manual/runtime evidence through the same resolution**

```java
String resolvedIdentifier = resolveGovernanceCandidateIdentifier(product, normalizedIdentifier.identifier(), observedRawIdentifiers, context);
```

- [ ] **Step 4: Stop the collector policy from truncating identifiers**

```java
String toCollectorIdentifier(String sampleType, String rawIdentifier, String resolvedMode) {
    return "FULL_PATH".equals(resolvedMode) ? normalizeText(rawIdentifier) : legacyShortening(rawIdentifier);
}
```

- [ ] **Step 5: Make comparator consume the already-unified identifier keys**

```java
String key = toKey(candidate.getModelType(), candidate.getIdentifier());
```

- [ ] **Step 6: Re-run the focused backend tests**

Run:
```powershell
mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest test
```

Expected: the two regression tests pass.

### Task 3: Sync frontend types and copy

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: Add the optional request field to types**

```ts
contractIdentifierMode?: 'DIRECT' | 'FULL_PATH' | null;
```

- [ ] **Step 2: Update the note copy**

```vue
单台多能力保留 full-path，单能力产品才收口为直接字段。
```

- [ ] **Step 3: Ensure the compare payload still omits the field by default**

```ts
manualExtract: {
  sampleType: sampleType.value,
  deviceStructure: deviceStructure.value,
  samplePayload: formattedSamplePayload.value
}
```

- [ ] **Step 4: Run the frontend unit tests that cover the workspace payload**

Run:
```powershell
npm test -- spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: tests still pass and payload shape remains backward compatible.

### Task 4: Update docs and verify

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update the contract-field rules in business docs**
- [ ] **Step 2: Update the compare/apply API notes**
- [ ] **Step 3: Add the new regression note to the change log**
- [ ] **Step 4: Run the targeted backend tests and the local quality gate**

Run:
```powershell
mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest test
node scripts/run-governance-contract-gates.mjs
```

Expected: all targeted checks pass and the rain-gauge contract-field rule is documented in place.
