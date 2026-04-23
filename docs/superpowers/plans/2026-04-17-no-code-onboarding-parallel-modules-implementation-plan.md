# No-Code Onboarding Parallel Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前“部分配置化、部分硬编码”的设备接入链路，落成一个按阶段顺序推进、阶段内可并行拆分、最终可在真实 `dev` 环境完成集成验收的无代码接入实施方案。

**Architecture:** 严格遵循 roadmap 的四期顺序，把剩余工作拆成 `M1-M9` 九个模块，并把并行范围限制在每一期内部。第 1 期先补齐规则治理尾项，第 2 期把协议模板从 Java 注册迁到配置化治理与解释执行，第 3 期把未登记设备名单推进到“建议优先、批量激活受控”，第 4 期再把对象洞察和风险侧收口到同一套配置化建议结果；固定接入 Pipeline 保持不变，不额外引入第二条运行时主链。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vite, JUnit 5, Vitest, Maven, Node.js, MySQL, TDengine

---

## Scope Check

这份计划只覆盖“无代码接入传感器”剩余工作，不重做已经稳定的第一至第三阶段主链路，也不把对象洞察趋势为 `0` 的历史修复重新纳入范围。当前仓库已经具备的基础能力包括：

1. 产品级厂商字段映射规则治理对象、审批、发布、回滚、命中预览。
2. `/protocol-governance` 对协议族定义与解密档案的草稿写侧、审批和解密试算。
3. 已发布 resolver snapshot、对象洞察/风险目录/历史 latest 的第一轮 canonical 收口。
4. 未登记设备名单、同页建档和设备资产工作台的真实环境基线。

这份计划解决的是剩余缺口：

1. Phase 1 尾项仍缺批量治理、回放校验、共享 scope 运营化视角。
2. Phase 2 还没有模板治理对象与模板解释执行器，`LegacyDpChildTemplateRegistry` 仍然靠 `new LegacyDpCrackChildTemplate()`、`new LegacyDpDeepDisplacementChildTemplate()` 这类硬编码注册。
3. Phase 3 还没有把“未登记名单 -> 建议 -> 批量激活 -> 正式设备”做成配置驱动闭环。
4. Phase 4 还没有把对象洞察、风险点绑定、联动规则这些下游全面升级为“建议优先”的自动联动层。

roadmap 已明确写死三条约束：

1. 四期必须按顺序推进。
2. 没有第 2 期，新协议模板仍然要写代码。
3. 没有第 3 期，报文接进来以后设备主档仍然是人工补录。

因此，本计划只允许“阶段内并行”，不允许“跨阶段并行”。

## Parallelization Matrix

| Window | Gate | Modules | Parallel Rule | Exit Criterion |
|---|---|---|---|---|
| A | Phase 1 Gate | `M1 Vendor Rule Tail` + `M2 Protocol Governance Tail` | 可并行，写集合基本独立 | 规则与协议治理都补齐批量/详情/回放/命中观测 |
| B | Phase 2 Foundation Gate | `M3 Template Governance Domain` + `M5 Template Validation Replay` | 可并行，前者产出治理对象，后者先搭回放壳层 | 模板对象、审批、回放接口和页面都可用 |
| C | Phase 2 Runtime Gate | `M4 Template Interpreter Runtime` | 依赖 Window B 产物 | 运行时不再需要新增 Java 模板类才能接新模板 |
| D | Phase 3 Suggestion Gate | `M6 Onboarding Suggestion Engine` | 单模块先行 | 未登记设备可得到配置化建议结果 |
| E | Phase 3 Activation Gate | `M7 Batch Activation Orchestration` | 依赖 `M6` | 建议结果可批量确认并转正为正式设备 |
| F | Phase 4 Downstream Gate | `M8 Downstream Suggestion Engine` | 依赖 `M7` | 对象洞察/风险绑定可消费建议结果 |
| G | Final Gate | `M9 Integration And Acceptance` | 串行收口 | 文档、门禁、真实环境验收全部通过 |

## File Structure

### M1 Vendor Rule Tail

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\VendorMetricMappingRuleService.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\VendorMetricMappingRuleBatchStatusDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\VendorMetricMappingRuleReplayDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\VendorMetricMappingRuleReplayVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\vendorMetricMappingRule.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\product\ProductVendorMappingRuleLedgerPanel.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\product\ProductVendorMappingSuggestionPanel.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleControllerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImplTest.java`

### M2 Protocol Governance Tail

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\controller\ProtocolGovernanceController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\service\ProtocolSecurityGovernanceService.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\service\impl\ProtocolSecurityGovernanceServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\dto\ProtocolGovernanceBatchSubmitDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\dto\ProtocolGovernanceReplayDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\vo\ProtocolGovernanceReplayVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\protocolGovernance.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProtocolGovernanceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\controller\ProtocolGovernanceControllerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\test\java\com\ghlzm\iot\framework\protocol\service\impl\ProtocolSecurityGovernanceServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\ProtocolGovernanceWorkbenchView.test.ts`

### M3 Template Governance Domain

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\controller\ProtocolGovernanceController.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\entity\ProtocolTemplateDefinitionRecord.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\entity\ProtocolTemplateDefinitionSnapshot.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\mapper\ProtocolTemplateDefinitionRecordMapper.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\mapper\ProtocolTemplateDefinitionSnapshotMapper.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\dto\ProtocolTemplateUpsertDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\dto\ProtocolTemplateSubmitDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\service\ProtocolTemplateGovernanceService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\service\impl\ProtocolTemplateGovernanceServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\vo\ProtocolTemplateDefinitionVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\protocolGovernance.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProtocolGovernanceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\test\java\com\ghlzm\iot\framework\protocol\template\service\impl\ProtocolTemplateGovernanceServiceImplTest.java`

### M4 Template Interpreter Runtime

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateRegistry.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateExecutor.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\LegacyDpPropertyNormalizer.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\runtime\CompiledLegacyDpTemplate.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\runtime\LegacyDpTemplateCompiler.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\runtime\ConfigDrivenLegacyDpChildTemplate.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\test\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateRegistryTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\test\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateExecutorTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\test\java\com\ghlzm\iot\protocol\mqtt\legacy\LegacyDpPropertyNormalizerTest.java`

### M5 Template Validation Replay

- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\dto\ProtocolTemplateReplayDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\vo\ProtocolTemplateReplayVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\replay\LegacyDpTemplateReplayService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\replay\LegacyDpTemplateReplayServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\controller\ProtocolGovernanceController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\protocolGovernance.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProtocolGovernanceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\test\java\com\ghlzm\iot\protocol\mqtt\legacy\template\replay\LegacyDpTemplateReplayServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\ProtocolGovernanceWorkbenchView.test.ts`

### M6 Onboarding Suggestion Engine

- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingSuggestionQuery.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingSuggestionService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingSuggestionServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingSuggestionVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\UnregisteredDeviceRosterServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\device.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\device\DeviceOnboardingSuggestionDrawer.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingSuggestionServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\UnregisteredDeviceRosterServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceWorkbenchView.test.ts`

### M7 Batch Activation Orchestration

- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingBatchActivateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingActivationService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingActivationServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingBatchResultVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\device.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceControllerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingActivationServiceImplTest.java`

### M8 Downstream Suggestion Engine

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\CollectorChildInsightServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceCollectorInsightController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\service\impl\RiskPointPendingRecommendationServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\product\ProductObjectInsightConfigEditor.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceInsightView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\RiskPointView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceCollectorInsightControllerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\test\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceInsightView.test.ts`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\RiskPointView.test.ts`

### M9 Integration And Acceptance

- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\governance-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\alarm-domain.json`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\sql\init.sql`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\schema\generated\mysql-schema-sync.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\01-系统概览与架构说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\16-阶段规划与迭代路线图.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\19-第四阶段交付边界与复验进展.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

## Implementation Notes

1. `M1` 与 `M2` 可以并行，但都必须在 `M3` 启动前完成；否则模板治理会继续绑死在不完整的规则与协议对象上。
2. `M3` 与 `M5` 可以并行起步，但 `M4` 合并前必须以 `M3` 的正式模板对象和 `M5` 的回放校验接口作为输入。
3. `M6` 开始前，运行时模板必须已经配置化，否则建议引擎仍然只能给出“需要写代码”的假建议。
4. `M7` 必须建立在 `M6` 的建议证据之上，不允许直接从未登记名单做全自动注册。
5. `M8` 固定为“建议优先”，不做无审查的自动风险绑定或自动改写对象洞察正式配置。

### Task 1: Finish M1 Vendor Rule Tail

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\product\ProductVendorMappingRuleLedgerPanel.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\VendorMetricMappingRuleControllerTest.java`

- [ ] **Step 1: Write the failing backend tests for detail, batch status, and replay**

```java
@Test
void shouldBatchChangeRuleStatusWithinSameScope() throws Exception {
    VendorMetricMappingRuleBatchStatusDTO dto = new VendorMetricMappingRuleBatchStatusDTO();
    dto.setRuleIds(List.of(11L, 12L));
    dto.setTargetStatus("DISABLED");

    mockMvc.perform(post("/api/device/product/1001/vendor-mapping-rules/batch-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.changedCount").value(2));
}
```

```java
@Test
void shouldReplayRuleHitAgainstSamplePayload() {
    VendorMetricMappingRuleReplayDTO dto = new VendorMetricMappingRuleReplayDTO();
    dto.setRawIdentifier("L1_LF_1.value");
    dto.setSampleValue("0.2136");

    VendorMetricMappingRuleReplayVO result = service.replay(1001L, dto);

    assertEquals("value", result.getCanonicalIdentifier());
    assertEquals("PRODUCT", result.getMatchedScopeType());
}
```

- [ ] **Step 2: Run the focused tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-device -am test "-Dtest=VendorMetricMappingRuleControllerTest,VendorMetricMappingRuleServiceImplTest"`

Expected: FAIL because batch/replay endpoints or service methods do not exist yet.

- [ ] **Step 3: Implement DTO/VO, controller, and service methods**

```java
@PostMapping("/{productId}/vendor-mapping-rules/batch-status")
public R<BatchResultVO> batchStatus(@PathVariable Long productId,
                                    @Validated @RequestBody VendorMetricMappingRuleBatchStatusDTO dto) {
    return R.ok(vendorMetricMappingRuleService.batchStatus(productId, dto));
}

@PostMapping("/{productId}/vendor-mapping-rules/replay")
public R<VendorMetricMappingRuleReplayVO> replay(@PathVariable Long productId,
                                                 @Validated @RequestBody VendorMetricMappingRuleReplayDTO dto) {
    return R.ok(vendorMetricMappingRuleService.replay(productId, dto));
}
```

```java
public VendorMetricMappingRuleReplayVO replay(Long productId, VendorMetricMappingRuleReplayDTO dto) {
    VendorMetricMappingRuntimeHit hit = runtimeService.previewHit(productId, dto.getRawIdentifier(), dto.getSampleValue());
    return VendorMetricMappingRuleReplayVO.from(hit);
}
```

- [ ] **Step 4: Extend the `/products` ledger panel with batch and replay UI**

```ts
export function replayVendorMappingRule(productId: number, payload: VendorMetricMappingRuleReplayRequest) {
  return request.post(`/api/device/product/${productId}/vendor-mapping-rules/replay`, payload)
}
```

```vue
<el-button :disabled="selectedRuleIds.length === 0" @click="handleBatchDisable">
  批量停用
</el-button>
<el-button @click="openReplayDialog(row)">回放校验</el-button>
```

- [ ] **Step 5: Re-run backend and frontend tests**

Run: `mvn -pl spring-boot-iot-device -am test "-Dtest=VendorMetricMappingRuleControllerTest,VendorMetricMappingRuleServiceImplTest,VendorMetricMappingRuntimeServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- ProductWorkbenchView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device spring-boot-iot-device/src/test/java/com/ghlzm/iot/device spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue
git commit -m "feat: complete vendor mapping governance tail"
```

### Task 2: Finish M2 Protocol Governance Tail

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\controller\ProtocolGovernanceController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\service\impl\ProtocolSecurityGovernanceServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProtocolGovernanceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\test\java\com\ghlzm\iot\system\controller\ProtocolGovernanceControllerTest.java`

- [ ] **Step 1: Write the failing tests for protocol detail, batch submit, and replay**

```java
@Test
void shouldReplayDecryptProfileAgainstCipherSample() throws Exception {
    ProtocolGovernanceReplayDTO dto = new ProtocolGovernanceReplayDTO();
    dto.setFamilyCode("legacy-dp");
    dto.setProtocolCode("mqtt-json");
    dto.setAppId("62000001");
    dto.setCiphertext("7B226B6579223A2276616C7565227D");

    mockMvc.perform(post("/api/protocol-governance/decrypt-profiles/replay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hit").value(true));
}
```

- [ ] **Step 2: Run the focused tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-system,spring-boot-iot-framework -am test "-Dtest=ProtocolGovernanceControllerTest,ProtocolSecurityGovernanceServiceImplTest"`

Expected: FAIL because replay/batch/detail API surface is incomplete.

- [ ] **Step 3: Implement controller/service support for replay and batch operations**

```java
@PostMapping("/decrypt-profiles/replay")
public R<ProtocolGovernanceReplayVO> replayDecryptProfile(@Validated @RequestBody ProtocolGovernanceReplayDTO dto) {
    return R.ok(protocolSecurityGovernanceService.replayDecryptProfile(dto));
}
```

```java
public ProtocolGovernanceReplayVO replayDecryptProfile(ProtocolGovernanceReplayDTO dto) {
    ProtocolDecryptPreviewVO preview = previewDecrypt(dto.toPreviewDTO());
    return ProtocolGovernanceReplayVO.from(preview);
}
```

- [ ] **Step 4: Surface detail/replay/batch actions in `/protocol-governance`**

```ts
export function replayDecryptProfile(payload: ProtocolGovernanceReplayRequest) {
  return request.post('/api/protocol-governance/decrypt-profiles/replay', payload)
}
```

```vue
<el-button link type="primary" @click="openReplayDialog(item)">回放校验</el-button>
<el-button link @click="openDetailDrawer(item)">查看详情</el-button>
```

- [ ] **Step 5: Re-run backend and frontend tests**

Run: `mvn -pl spring-boot-iot-system,spring-boot-iot-framework -am test "-Dtest=ProtocolGovernanceControllerTest,ProtocolSecurityGovernanceServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- ProtocolGovernanceWorkbenchView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ProtocolGovernanceController.java spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ProtocolGovernanceControllerTest.java spring-boot-iot-ui/src/api/protocolGovernance.ts spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue
git commit -m "feat: finish protocol governance tail operations"
```

### Task 3: Build M3 Template Governance Domain

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\entity\ProtocolTemplateDefinitionRecord.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\service\ProtocolTemplateGovernanceService.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\controller\ProtocolGovernanceController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProtocolGovernanceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\test\java\com\ghlzm\iot\framework\protocol\template\service\impl\ProtocolTemplateGovernanceServiceImplTest.java`

- [ ] **Step 1: Write the failing service test for draft/save/publish of a template definition**

```java
@Test
void shouldCreateTemplateDraftAndPublishSnapshot() {
    ProtocolTemplateUpsertDTO dto = new ProtocolTemplateUpsertDTO();
    dto.setTemplateCode("legacy-dp-crack-v1");
    dto.setFamilyCode("legacy-dp");
    dto.setExpressionJson("{\"childrenPath\":\"$.L1_LF_*\"}");

    ProtocolTemplateDefinitionVO draft = service.saveDraft(dto);
    ProtocolTemplateDefinitionVO published = service.submitPublish(draft.getId(), new ProtocolTemplateSubmitDTO("首次发布"));

    assertEquals("DRAFT", draft.getStatus());
    assertEquals("PUBLISHED", published.getStatus());
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `mvn -pl spring-boot-iot-framework -am test "-Dtest=ProtocolTemplateGovernanceServiceImplTest"`

Expected: FAIL because template governance objects and service do not exist yet.

- [ ] **Step 3: Implement entity, mapper, service, and controller endpoints**

```java
@Data
@TableName("iot_protocol_template_definition")
public class ProtocolTemplateDefinitionRecord {
    private Long id;
    private String templateCode;
    private String familyCode;
    private String protocolCode;
    private String status;
    private String expressionJson;
}
```

```java
@PostMapping("/templates")
public R<ProtocolTemplateDefinitionVO> saveTemplate(@Validated @RequestBody ProtocolTemplateUpsertDTO dto) {
    return R.ok(protocolTemplateGovernanceService.saveDraft(dto));
}
```

- [ ] **Step 4: Add a template ledger region to `/protocol-governance`**

```vue
<el-tab-pane label="协议模板" name="templates">
  <protocol-template-ledger
    :items="templateRows"
    @create="openTemplateEditor"
    @publish="submitTemplatePublish"
  />
</el-tab-pane>
```

- [ ] **Step 5: Re-run the focused service and page tests**

Run: `mvn -pl spring-boot-iot-framework -am test "-Dtest=ProtocolTemplateGovernanceServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- ProtocolGovernanceWorkbenchView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/template spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/template spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ProtocolGovernanceController.java spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue
git commit -m "feat: add protocol template governance domain"
```

### Task 4: Build M5 Template Validation Replay

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\dto\ProtocolTemplateReplayDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\protocol\template\vo\ProtocolTemplateReplayVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\replay\LegacyDpTemplateReplayService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\replay\LegacyDpTemplateReplayServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-system\src\main\java\com\ghlzm\iot\system\controller\ProtocolGovernanceController.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\test\java\com\ghlzm\iot\protocol\mqtt\legacy\template\replay\LegacyDpTemplateReplayServiceImplTest.java`

- [ ] **Step 1: Write the failing replay test using an existing sample payload**

```java
@Test
void shouldReplayDraftTemplateAgainstCollectorPayload() {
    ProtocolTemplateReplayDTO dto = new ProtocolTemplateReplayDTO();
    dto.setTemplateCode("legacy-dp-crack-v1");
    dto.setPayloadJson("{\"L1_LF_1\":{\"value\":0.2136}}");

    ProtocolTemplateReplayVO result = replayService.replay(dto);

    assertTrue(result.isMatched());
    assertEquals(1, result.getExtractedChildren().size());
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `mvn -pl spring-boot-iot-protocol,spring-boot-iot-framework -am test "-Dtest=LegacyDpTemplateReplayServiceImplTest"`

Expected: FAIL because replay service and DTO/VO do not exist yet.

- [ ] **Step 3: Implement replay DTO/VO, service, and controller endpoint**

```java
public interface LegacyDpTemplateReplayService {
    ProtocolTemplateReplayVO replay(ProtocolTemplateReplayDTO dto);
}
```

```java
@PostMapping("/templates/replay")
public R<ProtocolTemplateReplayVO> replayTemplate(@Validated @RequestBody ProtocolTemplateReplayDTO dto) {
    return R.ok(legacyDpTemplateReplayService.replay(dto));
}
```

- [ ] **Step 4: Add replay panel to protocol governance page**

```vue
<el-button type="primary" @click="handleTemplateReplay">模板回放</el-button>
<el-alert v-if="templateReplayResult" type="success" :title="templateReplayResult.summary" />
```

- [ ] **Step 5: Re-run replay tests**

Run: `mvn -pl spring-boot-iot-protocol,spring-boot-iot-framework -am test "-Dtest=LegacyDpTemplateReplayServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- ProtocolGovernanceWorkbenchView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/template spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/template/replay spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/legacy/template/replay spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue
git commit -m "feat: add protocol template replay validation"
```

### Task 5: Build M4 Template Interpreter Runtime

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateRegistry.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateExecutor.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\LegacyDpPropertyNormalizer.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\runtime\CompiledLegacyDpTemplate.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\runtime\LegacyDpTemplateCompiler.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\main\java\com\ghlzm\iot\protocol\mqtt\legacy\template\runtime\ConfigDrivenLegacyDpChildTemplate.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-protocol\src\test\java\com\ghlzm\iot\protocol\mqtt\legacy\template\LegacyDpChildTemplateRegistryTest.java`

- [ ] **Step 1: Write the failing registry test proving templates load from published definitions instead of hardcoded classes**

```java
@Test
void shouldLoadPublishedTemplateWithoutInstantiatingJavaSubclass() {
    when(templateProvider.listPublished()).thenReturn(List.of(compiledTemplate("legacy-dp-crack-v1")));

    List<LegacyDpChildTemplate> templates = registry.listTemplates();

    assertEquals(1, templates.size());
    assertEquals("legacy-dp-crack-v1", templates.get(0).templateCode());
}
```

- [ ] **Step 2: Run the focused protocol tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-protocol -am test "-Dtest=LegacyDpChildTemplateRegistryTest,LegacyDpChildTemplateExecutorTest,LegacyDpPropertyNormalizerTest"`

Expected: FAIL because the registry still builds templates with explicit `new LegacyDpCrackChildTemplate()` style registration.

- [ ] **Step 3: Implement compiler and config-driven template adapter**

```java
public record CompiledLegacyDpTemplate(
        String templateCode,
        String familyCode,
        JsonNode expression,
        JsonNode outputMapping
) {
}
```

```java
public class ConfigDrivenLegacyDpChildTemplate implements LegacyDpChildTemplate {
    private final CompiledLegacyDpTemplate compiled;

    @Override
    public String templateCode() {
        return compiled.templateCode();
    }
}
```

- [ ] **Step 4: Replace registry construction with provider + compiler**

```java
public List<LegacyDpChildTemplate> listTemplates() {
    return templateProvider.listPublished().stream()
            .map(templateCompiler::compile)
            .map(ConfigDrivenLegacyDpChildTemplate::new)
            .toList();
}
```

- [ ] **Step 5: Re-run protocol tests**

Run: `mvn -pl spring-boot-iot-protocol -am test "-Dtest=LegacyDpChildTemplateRegistryTest,LegacyDpChildTemplateExecutorTest,LegacyDpPropertyNormalizerTest"`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/legacy
git commit -m "feat: move legacy dp templates to config driven runtime"
```

### Task 6: Build M6 Onboarding Suggestion Engine

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingSuggestionService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingSuggestionServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\UnregisteredDeviceRosterServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\device\DeviceOnboardingSuggestionDrawer.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingSuggestionServiceImplTest.java`

- [ ] **Step 1: Write the failing suggestion service test**

```java
@Test
void shouldSuggestProductFamilyTemplateAndRuleForUnregisteredDevice() {
    DeviceOnboardingSuggestionVO result = service.suggestByTrace("trace-unregistered-001");

    assertEquals("south_rtu", result.getRecommendedProductKey());
    assertEquals("legacy-dp", result.getRecommendedFamilyCode());
    assertEquals("legacy-dp-crack-v1", result.getRecommendedTemplateCode());
    assertFalse(result.getRuleGaps().isEmpty());
}
```

- [ ] **Step 2: Run the focused suggestion tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-device -am test "-Dtest=DeviceOnboardingSuggestionServiceImplTest,UnregisteredDeviceRosterServiceImplTest,DeviceServiceImplTest"`

Expected: FAIL because suggestion service and suggestion fields do not exist yet.

- [ ] **Step 3: Implement suggestion service by combining roster, resolver snapshot, protocol template, and evidence**

```java
public DeviceOnboardingSuggestionVO suggestByTrace(String traceId) {
    UnregisteredDeviceCandidate candidate = rosterService.getRequiredCandidate(traceId);
    return DeviceOnboardingSuggestionVO.builder()
            .recommendedProductKey(productMatcher.match(candidate))
            .recommendedFamilyCode(protocolMatcher.matchFamily(candidate))
            .recommendedTemplateCode(templateMatcher.matchTemplate(candidate))
            .ruleGaps(ruleGapAnalyzer.findGaps(candidate))
            .build();
}
```

- [ ] **Step 4: Show suggestion drawer in device workbench**

```vue
<el-button link type="primary" @click="openSuggestion(row)">接入建议</el-button>
<device-onboarding-suggestion-drawer
  v-model="suggestionVisible"
  :suggestion="currentSuggestion"
/>
```

- [ ] **Step 5: Re-run backend and frontend tests**

Run: `mvn -pl spring-boot-iot-device -am test "-Dtest=DeviceOnboardingSuggestionServiceImplTest,UnregisteredDeviceRosterServiceImplTest,DeviceServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- DeviceWorkbenchView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device spring-boot-iot-device/src/test/java/com/ghlzm/iot/device spring-boot-iot-ui/src/api/device.ts spring-boot-iot-ui/src/components/device/DeviceOnboardingSuggestionDrawer.vue spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue
git commit -m "feat: add onboarding suggestion engine"
```

### Task 7: Build M7 Batch Activation Orchestration

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingActivationService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingActivationServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingActivationServiceImplTest.java`

- [ ] **Step 1: Write the failing activation test requiring explicit confirmation of suggestion results**

```java
@Test
void shouldBatchActivateOnlyConfirmedSuggestionRows() {
    DeviceOnboardingBatchActivateDTO dto = new DeviceOnboardingBatchActivateDTO();
    dto.setTraceIds(List.of("trace-unregistered-001", "trace-unregistered-002"));
    dto.setConfirmed(true);

    DeviceOnboardingBatchResultVO result = service.activate(dto);

    assertEquals(2, result.getActivatedCount());
    assertEquals(0, result.getRejectedCount());
}
```

- [ ] **Step 2: Run the focused activation tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-device -am test "-Dtest=DeviceOnboardingActivationServiceImplTest,DeviceServiceImplTest"`

Expected: FAIL because activation service and confirmation flow do not exist yet.

- [ ] **Step 3: Implement activation service and controller API**

```java
@PostMapping("/onboarding/batch-activate")
public R<DeviceOnboardingBatchResultVO> batchActivate(@Validated @RequestBody DeviceOnboardingBatchActivateDTO dto) {
    return R.ok(deviceOnboardingActivationService.activate(dto));
}
```

```java
public DeviceOnboardingBatchResultVO activate(DeviceOnboardingBatchActivateDTO dto) {
    BizAssert.isTrue(Boolean.TRUE.equals(dto.getConfirmed()), "请先确认接入建议");
    return activationOrchestrator.activateConfirmedSuggestions(dto);
}
```

- [ ] **Step 4: Add batch confirmation UX to the device workbench**

```vue
<el-button type="primary" :disabled="selectedTraceIds.length === 0" @click="openBatchActivateDialog">
  批量转正式设备
</el-button>
```

- [ ] **Step 5: Re-run backend and frontend tests**

Run: `mvn -pl spring-boot-iot-device -am test "-Dtest=DeviceOnboardingActivationServiceImplTest,DeviceServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- DeviceWorkbenchView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device spring-boot-iot-device/src/test/java/com/ghlzm/iot/device spring-boot-iot-ui/src/api/device.ts spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue
git commit -m "feat: add onboarding batch activation flow"
```

### Task 8: Build M8 Downstream Suggestion Engine

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\CollectorChildInsightServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\service\impl\RiskPointPendingRecommendationServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\product\ProductObjectInsightConfigEditor.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceInsightView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\RiskPointView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\test\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImplTest.java`

- [ ] **Step 1: Write the failing downstream recommendation tests**

```java
@Test
void shouldRecommendObjectInsightMetricFromPublishedOnboardingOutput() {
    List<String> recommendations = collectorChildInsightService.listRecommendedMetrics(1001L);
    assertTrue(recommendations.contains("value"));
}
```

```java
@Test
void shouldRecommendRiskMetricBindingOnlyForRiskReadyCanonicalMetrics() {
    List<String> recommendations = riskPointPendingRecommendationService.listRecommendedMetricIdentifiers(1001L);
    assertEquals(List.of("value"), recommendations);
}
```

- [ ] **Step 2: Run the focused downstream tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am test "-Dtest=DeviceCollectorInsightControllerTest,RiskMetricCatalogServiceImplTest"`

Expected: FAIL because downstream recommendation APIs are not reading onboarding output yet.

- [ ] **Step 3: Wire collector insight and risk recommendation services to published onboarding outputs**

```java
public List<String> listRecommendedMetrics(Long productId) {
    PublishedOnboardingOutput output = onboardingOutputService.getRequiredOutput(productId);
    return output.canonicalMetrics().stream()
            .filter(PublishedMetric::objectInsightRecommended)
            .map(PublishedMetric::identifier)
            .toList();
}
```

- [ ] **Step 4: Add suggestion-first UI in object insight and risk point workbenches**

```vue
<el-alert
  v-if="recommendedMetrics.length"
  type="info"
  :title="`建议先采用 ${recommendedMetrics.join(' / ')} 作为重点指标`"
/>
```

- [ ] **Step 5: Re-run backend and frontend tests**

Run: `mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am test "-Dtest=DeviceCollectorInsightControllerTest,RiskMetricCatalogServiceImplTest"`

Run: `npm --prefix spring-boot-iot-ui run test -- DeviceInsightView RiskPointView`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue spring-boot-iot-ui/src/views/DeviceInsightView.vue spring-boot-iot-ui/src/views/RiskPointView.vue
git commit -m "feat: add downstream onboarding recommendations"
```

### Task 9: Finish M9 Integration, Schema, Docs, And Real-Env Acceptance

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\governance-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\alarm-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\01-系统概览与架构说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\16-阶段规划与迭代路线图.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\19-第四阶段交付边界与复验进展.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update schema registry first**

```json
{
  "tableName": "iot_protocol_template_definition",
  "domain": "governance",
  "columns": [
    { "name": "template_code", "type": "varchar(128)" },
    { "name": "family_code", "type": "varchar(64)" },
    { "name": "status", "type": "varchar(32)" }
  ]
}
```

- [ ] **Step 2: Regenerate schema artifacts and verify registry consistency**

Run: `python scripts/schema/render_artifacts.py --write`

Run: `python scripts/schema/check_schema_registry.py`

Expected: PASS

- [ ] **Step 3: Run module tests and quality gates in gate order**

Run: `mvn -pl spring-boot-iot-device,spring-boot-iot-framework,spring-boot-iot-system,spring-boot-iot-protocol,spring-boot-iot-alarm -am test`

Run: `npm --prefix spring-boot-iot-ui run test`

Run: `node scripts/run-governance-contract-gates.mjs`

Expected: PASS except the known local Mockito inline issue already documented in `AGENTS.md`.

- [ ] **Step 4: Run build and strict real-environment acceptance**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`

Run: `powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1`

Run: `powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1`

Expected: PASS in shared `dev` environment using `application-dev.yml`.

- [ ] **Step 5: Execute the integration test matrix below and record evidence**

| Case | Input | Expected Result |
|---|---|---|
| Rule replay | raw alias `L1_LF_1.value` | 命中 canonical `value`，可看到命中 scope 和版本 |
| Protocol replay | `familyCode + protocolCode + appId + ciphertext` | 能返回解密结果与命中的解密档案 |
| Template replay | 未发布模板样本 payload | 能看到提取出的子设备与 canonical 字段 |
| Runtime ingest | 新模板正式发布后真实上报 | 无需新增 Java 模板类即可入链 |
| Suggestion | 未登记 trace | 返回产品、协议族、模板、规则缺口建议 |
| Activation | 批量确认的 trace 列表 | 生成正式设备，未登记名单减少 |
| Object insight | 新设备进入对象洞察 | 可消费推荐指标，不再要求手工猜字段 |
| Risk point | 风险待绑定视图 | 只推荐 `riskReady` 的 canonical 指标 |

- [ ] **Step 6: Update docs in place**

```md
- `README.md`: 新增“无代码接入分期能力边界”说明
- `AGENTS.md`: 新增阶段内并行、阶段间串行的执行约束
- `docs/02-业务功能与流程说明.md`: 更新接入智维、未登记设备、模板治理工作流
- `docs/03-接口规范与接口清单.md`: 补充模板治理、建议、批量激活 API
- `docs/04-数据库设计与初始化数据.md`: 补充模板治理表与建议结果表
- `docs/07-部署运行与配置说明.md`: 补充模板治理与真实环境验收前置项
- `docs/08-变更记录与技术债清单.md`: 补记本轮阶段收口与剩余边界
- `docs/19-第四阶段交付边界与复验进展.md`: 同步阶段 4 实际交付边界
- `docs/21-业务功能清单与验收标准.md`: 同步“无代码接入”验收标准
```

- [ ] **Step 7: Commit**

```bash
git add schema sql README.md AGENTS.md docs
git commit -m "docs: finalize no-code onboarding parallel delivery plan outputs"
```

## Acceptance Notes

1. Window A 可以由两个 worker 并行执行，因为 `M1` 主要落在 `spring-boot-iot-device` 与 `/products`，`M2` 主要落在 `spring-boot-iot-framework`、`spring-boot-iot-system` 与 `/protocol-governance`。
2. Window B 也可以并行，但 `M3` 负责治理真相，`M5` 负责回放壳层；两边都不能擅自定义不同的 `templateCode`、`familyCode`、`protocolCode` 语义。
3. Window C 以后不再建议并行拆更多 worker，因为运行时模板解释器、建议引擎和批量激活的耦合开始明显增大。
4. 每个 window 合并后都要先跑本模块测试，再进入下一 gate，避免把问题带到后续阶段。

## Self-Review

### Spec coverage

1. Phase 1 尾项由 Task 1 和 Task 2 覆盖。
2. Phase 2 模板治理、回放和解释执行分别由 Task 3、Task 4、Task 5 覆盖。
3. Phase 3 建议与批量激活由 Task 6 和 Task 7 覆盖。
4. Phase 4 下游联动由 Task 8 覆盖。
5. 文档、schema、真实环境验收由 Task 9 覆盖。

### Placeholder scan

1. 本计划没有使用 `TODO`、`TBD`、`implement later` 一类占位词。
2. 每个任务都给出具体文件、具体测试、具体命令和最小实现骨架。
3. 对需要并行的阶段，已经明确 gate 与退出条件，避免“先做一半再说”。

### Type consistency

1. 模块编号固定为 `M1-M9`，窗口编号固定为 `A-G`。
2. `templateCode / familyCode / protocolCode / canonicalIdentifier / riskReady` 在所有任务中保持同一命名。
3. 建议结果统一叫 `DeviceOnboardingSuggestionVO`，批量执行结果统一叫 `DeviceOnboardingBatchResultVO`，避免后续实现时再次分叉。
