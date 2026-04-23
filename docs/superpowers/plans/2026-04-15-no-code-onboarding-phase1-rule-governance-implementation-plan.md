# No-Code Onboarding Phase 1 Rule Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 Phase 1“规则治理平台化”落成可上线的最小闭环，让厂商字段映射规则、协议族定义、解密档案都具备正式治理对象、审批发布、回滚和运行时命中真相，并把入口收口到 `/products` 与新增的 `/protocol-governance`。

**Architecture:** 继续保持 `/products` 是产品治理主入口，新增 `/protocol-governance` 作为接入智维下的协议治理工作台，不引入第二个总览中心。后端把“已发布数据库对象”提升为协议安全定义的运行时真相，YAML `iot.protocol.family-definitions` / `iot.protocol.decrypt-profiles` 仅保留为 fallback；厂商字段映射规则继续归属产品，但用快照表承载正式发布真相，避免草稿改动直接污染运行时。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vite, JUnit 5, Vitest, Maven, Node.js

---

## Scope Check

本计划只展开已批准 spec 的 Phase 1“规则治理平台化”，只覆盖以下 6 个交付面：

1. 厂商字段映射规则的正式治理生命周期
2. 协议族定义与解密档案的持久化治理对象
3. 审批、发布、回滚、固定复核人和权限种子
4. 运行时 Published Provider 桥接，且保留 YAML fallback
5. `/products` 内的映射规则台账
6. `/protocol-governance` 协议治理工作台、文档和验收

本计划明确不做：

1. Phase 2 的模板 DSL / 模板解释执行器替换
2. Phase 3 的设备半自动建档
3. Phase 4 的对象洞察 / 风险目录自动联动
4. 新传输协议接入骨架或新的协议 Java 适配器

## File Structure

### Schema And Seed Data

- Modify: `schema/mysql/device-domain.json`
- Modify: `schema/mysql/governance-domain.json`
- Modify: `sql/init-data.sql`
- Regenerate: `sql/init.sql`
- Regenerate: `schema/generated/mysql-schema-sync.json`

### Vendor Mapping Governance Backend

- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricMappingRuleSnapshot.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricMappingRuleSnapshotMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRulePublishSubmitDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleRollbackSubmitDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleHitPreviewDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleGovernanceService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleGovernanceServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleLedgerRowVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleHitPreviewVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalPayloads.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalExecutor.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuntimeServiceImpl.java`

### Protocol Governance Persistence And APIs

- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolFamilyDefinitionRecord.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolDecryptProfileRecord.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolFamilyDefinitionSnapshot.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolDecryptProfileSnapshot.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolFamilyDefinitionRecordMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolDecryptProfileRecordMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolFamilyDefinitionSnapshotMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolDecryptProfileSnapshotMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolFamilyDefinitionUpsertDTO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolDecryptProfileUpsertDTO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolDecryptPreviewDTO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/service/ProtocolSecurityGovernanceService.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/service/impl/ProtocolSecurityGovernanceServiceImpl.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolFamilyDefinitionVO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolDecryptProfileVO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolDecryptPreviewVO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/governance/ProtocolSecurityGovernanceApprovalPayloads.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/governance/ProtocolSecurityGovernanceApprovalExecutor.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ProtocolGovernanceController.java`

### Runtime Provider Bridge

- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/ProtocolSecurityDefinitionProvider.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/PublishedProtocolSecurityDefinitionProvider.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/YamlProtocolSecurityDefinitionProvider.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/CompositeProtocolSecurityDefinitionProvider.java`
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolver.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpFamilyResolver.java`

### Governance Permissions And Approval Routing

- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java`

### Frontend Access Workbench

- Create: `spring-boot-iot-ui/src/api/protocolGovernance.ts`
- Create: `spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue`
- Create: `spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`

### Tests

- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleGovernanceServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalExecutorTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleControllerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuntimeServiceImplTest.java`
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/service/impl/ProtocolSecurityGovernanceServiceImplTest.java`
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/CompositeProtocolSecurityDefinitionProviderTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ProtocolGovernanceControllerTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolverTest.java`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingRuleLedgerPanel.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/views/ProtocolGovernanceWorkbenchView.test.ts`

### Docs

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

## Task 1: Add vendor mapping rule snapshot truth and governance service

**Files:**
- Modify: `schema/mysql/device-domain.json`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricMappingRuleSnapshot.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricMappingRuleSnapshotMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRulePublishSubmitDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleRollbackSubmitDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleHitPreviewDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleGovernanceService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleGovernanceServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleLedgerRowVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleHitPreviewVO.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleGovernanceServiceImplTest.java`

- [ ] **Step 1: Write the failing governance service tests**

```java
@Test
void submitPublishShouldCreatePendingApprovalUsingFixedReviewer() {
    when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "DRAFT", 3));
    when(policyResolver.resolveApproverUserId(
            VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH,
            10001L
    )).thenReturn(20001L);
    when(approvalService.submitAction(any())).thenReturn(99001L);

    GovernanceSubmissionResultVO result = service.submitPublish(
            1001L,
            7101L,
            10001L,
            new VendorMetricMappingRulePublishSubmitDTO("发布 value alias")
    );

    assertEquals(99001L, result.getApprovalOrderId());
    verify(approvalService).submitAction(argThat(command ->
            VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH.equals(command.actionCode())
                    && Long.valueOf(20001L).equals(command.approverUserId())
                    && command.payloadJson().contains("\"expectedVersionNo\":3")
                    && command.payloadJson().contains("\"ruleId\":7101")
    ));
}

@Test
void pageLedgerShouldPreferPublishedSnapshotStateOverDraftRow() {
    when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "DRAFT", 4));
    when(snapshotMapper.selectLatestPublishedByRuleId(7101L))
            .thenReturn(snapshot(8101L, 7101L, 1001L, "disp", "value", 3, 88001L));

    VendorMetricMappingRuleLedgerRowVO row = service.getLedgerRow(1001L, 7101L);

    assertEquals("DRAFT", row.getDraftStatus());
    assertEquals("PUBLISHED", row.getPublishedStatus());
    assertEquals(3, row.getPublishedVersionNo());
    assertEquals(4, row.getDraftVersionNo());
}
```

- [ ] **Step 2: Run the targeted device governance test to confirm RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-system -am "-Dtest=VendorMetricMappingRuleGovernanceServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- compile errors mentioning missing snapshot entity/mapper, governance service, or publish submit DTO

- [ ] **Step 3: Add the snapshot table to the schema registry and regenerate artifacts**

```json
{
  "name": "iot_vendor_metric_mapping_rule_snapshot",
  "comment": "厂商字段映射规则正式发布快照",
  "columns": [
    { "name": "id", "type": "bigint", "nullable": false, "comment": "主键" },
    { "name": "rule_id", "type": "bigint", "nullable": false, "comment": "规则主表 ID" },
    { "name": "product_id", "type": "bigint", "nullable": false, "comment": "产品 ID" },
    { "name": "approval_order_id", "type": "bigint", "nullable": false, "comment": "审批主单 ID" },
    { "name": "published_version_no", "type": "int", "nullable": false, "comment": "发布版本号" },
    { "name": "snapshot_json", "type": "json", "nullable": false, "comment": "规则发布快照" },
    { "name": "lifecycle_status", "type": "varchar(32)", "nullable": false, "default": "'PUBLISHED'", "comment": "快照生命周期" }
  ]
}
```

Run:

```powershell
python scripts/schema/render_artifacts.py --write
python scripts/schema/check_schema_registry.py
```

Expected:

- schema render succeeds
- registry check passes without drift

- [ ] **Step 4: Add the governance DTO/VO/service contract**

```java
public interface VendorMetricMappingRuleGovernanceService {

    GovernanceSubmissionResultVO submitPublish(Long productId,
                                               Long ruleId,
                                               Long operatorUserId,
                                               VendorMetricMappingRulePublishSubmitDTO dto);

    GovernanceSubmissionResultVO submitRollback(Long productId,
                                                Long ruleId,
                                                Long operatorUserId,
                                                VendorMetricMappingRuleRollbackSubmitDTO dto);

    VendorMetricMappingRuleLedgerRowVO getLedgerRow(Long productId, Long ruleId);

    VendorMetricMappingRuleHitPreviewVO previewHit(Long productId, VendorMetricMappingRuleHitPreviewDTO dto);
}
```

```java
@Data
public class VendorMetricMappingRuleLedgerRowVO {
    private Long ruleId;
    private Long productId;
    private String rawIdentifier;
    private String targetNormativeIdentifier;
    private String scopeType;
    private String draftStatus;
    private Integer draftVersionNo;
    private String publishedStatus;
    private Integer publishedVersionNo;
    private Long latestApprovalOrderId;
    private String publishedSource;
}
```

- [ ] **Step 5: Implement the governance service on top of fixed reviewer resolution and snapshot reads**

```java
@Service
public class VendorMetricMappingRuleGovernanceServiceImpl implements VendorMetricMappingRuleGovernanceService {

    @Override
    public GovernanceSubmissionResultVO submitPublish(Long productId,
                                                      Long ruleId,
                                                      Long operatorUserId,
                                                      VendorMetricMappingRulePublishSubmitDTO dto) {
        VendorMetricMappingRule rule = requireRule(productId, ruleId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                VendorMetricMappingRuleGovernanceApprovalPayloads.buildPublishCommand(
                        rule,
                        operatorUserId,
                        approverUserId,
                        dto == null ? null : dto.getSubmitReason()
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    @Override
    public VendorMetricMappingRuleLedgerRowVO getLedgerRow(Long productId, Long ruleId) {
        VendorMetricMappingRule rule = requireRule(productId, ruleId);
        VendorMetricMappingRuleSnapshot snapshot = snapshotMapper.selectLatestPublishedByRuleId(ruleId);
        VendorMetricMappingRuleLedgerRowVO row = new VendorMetricMappingRuleLedgerRowVO();
        row.setRuleId(rule.getId());
        row.setProductId(rule.getProductId());
        row.setRawIdentifier(rule.getRawIdentifier());
        row.setTargetNormativeIdentifier(rule.getTargetNormativeIdentifier());
        row.setScopeType(rule.getScopeType());
        row.setDraftStatus(rule.getStatus());
        row.setDraftVersionNo(rule.getVersionNo());
        row.setPublishedStatus(snapshot == null ? null : snapshot.getLifecycleStatus());
        row.setPublishedVersionNo(snapshot == null ? null : snapshot.getPublishedVersionNo());
        row.setLatestApprovalOrderId(snapshot == null ? null : snapshot.getApprovalOrderId());
        row.setPublishedSource(snapshot == null ? "draft_table" : "published_snapshot");
        return row;
    }
}
```

Implementation rules:

1. 草稿仍继续落在 `iot_vendor_metric_mapping_rule` 主表，不能新增平行 draft 主表。
2. 正式运行时真相来自 `iot_vendor_metric_mapping_rule_snapshot` 的最新 `PUBLISHED` 快照。
3. `submitPublish(...)` 只负责创建审批单，不直接改成 `ACTIVE`。
4. `previewHit(...)` 先查 published snapshot，再回退当前主表 DRAFT 行，明确返回命中来源。

- [ ] **Step 6: Re-run the governance service test until GREEN**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-system -am "-Dtest=VendorMetricMappingRuleGovernanceServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- `VendorMetricMappingRuleGovernanceServiceImplTest` passes

- [ ] **Step 7: Commit the vendor mapping snapshot/service foundation**

```powershell
git add schema/mysql/device-domain.json schema/generated/mysql-schema-sync.json sql/init.sql `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricMappingRuleSnapshot.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricMappingRuleSnapshotMapper.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRulePublishSubmitDTO.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleRollbackSubmitDTO.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleHitPreviewDTO.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleGovernanceService.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleGovernanceServiceImpl.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleLedgerRowVO.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleHitPreviewVO.java `
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleGovernanceServiceImplTest.java
git commit -m "feat: add vendor mapping rule governance snapshot base"
```

## Task 2: Wire vendor mapping rule approval execution and controller endpoints

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalPayloads.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalExecutor.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java`
- Modify: `sql/init-data.sql`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalExecutorTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleControllerTest.java`

- [ ] **Step 1: Write the failing executor and controller tests**

```java
@Test
void executePublishShouldWritePublishedSnapshotAndActivateRule() {
    GovernanceApprovalOrder order = approvalOrder(
            VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH,
            "{\"ruleId\":7101,\"productId\":1001,\"expectedVersionNo\":3}"
    );
    when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "DRAFT", 3));

    executor.execute(order);

    verify(snapshotMapper).insert(argThat(snapshot ->
            Long.valueOf(7101L).equals(snapshot.getRuleId())
                    && Integer.valueOf(3).equals(snapshot.getPublishedVersionNo())
    ));
    verify(ruleMapper).updateById(argThat(rule ->
            Long.valueOf(7101L).equals(rule.getId())
                    && "ACTIVE".equals(rule.getStatus())
    ));
}
```

```java
@Test
void submitPublishShouldRequireProductContractGovernPermission() {
    when(governanceService.submitPublish(eq(1001L), eq(7101L), eq(10001L), any()))
            .thenReturn(GovernanceSubmissionResultVO.pendingApproval(null, 99001L));

    R<GovernanceSubmissionResultVO> response = controller.submitPublish(
            1001L,
            7101L,
            new VendorMetricMappingRulePublishSubmitDTO("发布 value alias"),
            authentication(10001L)
    );

    assertEquals(99001L, response.getData().getApprovalOrderId());
    verify(permissionGuard).requireAnyPermission(10001L, "厂商字段映射规则发布", GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN);
}
```

- [ ] **Step 2: Run focused tests to confirm RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-system -am "-Dtest=VendorMetricMappingRuleGovernanceApprovalExecutorTest,VendorMetricMappingRuleControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- missing action payload helpers, executor, or controller endpoints

- [ ] **Step 3: Add approval payloads and executor**

```java
public final class VendorMetricMappingRuleGovernanceApprovalPayloads {

    public static final String ACTION_VENDOR_MAPPING_RULE_PUBLISH = "VENDOR_MAPPING_RULE_PUBLISH";
    public static final String ACTION_VENDOR_MAPPING_RULE_ROLLBACK = "VENDOR_MAPPING_RULE_ROLLBACK";

    public static GovernanceApprovalActionCommand buildPublishCommand(VendorMetricMappingRule rule,
                                                                      Long operatorUserId,
                                                                      Long approverUserId,
                                                                      String submitReason) {
        String payloadJson = writePublishPayload(rule, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                "厂商字段映射规则发布",
                "VENDOR_MAPPING_RULE",
                rule.getId(),
                null,
                operatorUserId,
                approverUserId,
                payloadJson,
                submitReason
        );
    }
}
```

```java
@Service
public class VendorMetricMappingRuleGovernanceApprovalExecutor implements GovernanceApprovalActionExecutor {

    @Override
    public boolean supports(String actionCode) {
        return ACTION_VENDOR_MAPPING_RULE_PUBLISH.equals(actionCode)
                || ACTION_VENDOR_MAPPING_RULE_ROLLBACK.equals(actionCode);
    }

    @Override
    public GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order) {
        return switch (order.getActionCode().trim()) {
            case ACTION_VENDOR_MAPPING_RULE_PUBLISH -> executePublish(order);
            case ACTION_VENDOR_MAPPING_RULE_ROLLBACK -> executeRollback(order);
            default -> throw new BizException("审批动作不支持执行: " + order.getActionCode());
        };
    }
}
```

- [ ] **Step 4: Expose publish/rollback/preview endpoints and approval permission routing**

```java
@PostMapping("/api/device/product/{productId}/vendor-mapping-rules/{ruleId}/submit-publish")
public R<GovernanceSubmissionResultVO> submitPublish(@PathVariable Long productId,
                                                     @PathVariable Long ruleId,
                                                     @RequestBody @Valid VendorMetricMappingRulePublishSubmitDTO dto,
                                                     Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    permissionGuard.requireAnyPermission(currentUserId, "厂商字段映射规则发布", GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN);
    return R.ok(governanceService.submitPublish(productId, ruleId, currentUserId, dto));
}

@PostMapping("/api/device/product/{productId}/vendor-mapping-rules/preview-hit")
public R<VendorMetricMappingRuleHitPreviewVO> previewHit(@PathVariable Long productId,
                                                         @RequestBody @Valid VendorMetricMappingRuleHitPreviewDTO dto,
                                                         Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    permissionGuard.requireAnyPermission(currentUserId, "厂商字段映射规则试命中", GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN);
    return R.ok(governanceService.previewHit(productId, dto));
}
```

```java
private void requireActionPermission(GovernanceApprovalOrder order, Long actorUserId, ApprovalOperation operation) {
    switch (normalizeRequiredText(order.getActionCode(), "审批动作编码不能为空")) {
        case ACTION_PRODUCT_CONTRACT_RELEASE_APPLY -> requireProductContractReleasePermissions(actorUserId, operation);
        case ACTION_PRODUCT_CONTRACT_ROLLBACK -> requireProductContractRollbackPermissions(actorUserId, operation);
        case ACTION_VENDOR_MAPPING_RULE_PUBLISH -> requireVendorMappingPublishPermissions(actorUserId, operation);
        case ACTION_VENDOR_MAPPING_RULE_ROLLBACK -> requireVendorMappingRollbackPermissions(actorUserId, operation);
        default -> throw new BizException("审批动作未配置权限映射: " + order.getActionCode());
    }
}
```

Implementation rules:

1. 映射规则发布/回滚继续复用 `PRODUCT_CONTRACT_GOVERN / PRODUCT_CONTRACT_APPROVE / PRODUCT_CONTRACT_ROLLBACK`，不要再造第二套产品治理权限族。
2. `sql/init-data.sql` 必须新增 `VENDOR_MAPPING_RULE_PUBLISH` / `VENDOR_MAPPING_RULE_ROLLBACK` 的固定复核人策略，默认仍是 `governance_reviewer`。
3. `VendorMetricMappingRuleServiceImpl` 的 `pageRules(...)` 要升级为能返回 scope、发布状态、审批单 ID 和最新 published version，供台账页直接消费。

- [ ] **Step 5: Re-run the approval/controller tests until GREEN**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-system -am "-Dtest=VendorMetricMappingRuleGovernanceApprovalExecutorTest,VendorMetricMappingRuleControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- publish/rollback executor and controller tests pass

- [ ] **Step 6: Commit the vendor mapping approval flow**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalPayloads.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalExecutor.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleController.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleService.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java `
  sql/init-data.sql `
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/VendorMetricMappingRuleGovernanceApprovalExecutorTest.java `
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleControllerTest.java
git commit -m "feat: add vendor mapping rule approval flow"
```

## Task 3: Add protocol family and decrypt profile governance objects, APIs, and approvals

**Files:**
- Modify: `schema/mysql/governance-domain.json`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolFamilyDefinitionRecord.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolDecryptProfileRecord.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolFamilyDefinitionSnapshot.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolDecryptProfileSnapshot.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolFamilyDefinitionRecordMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolDecryptProfileRecordMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolFamilyDefinitionSnapshotMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolDecryptProfileSnapshotMapper.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolFamilyDefinitionUpsertDTO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolDecryptProfileUpsertDTO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolDecryptPreviewDTO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/service/ProtocolSecurityGovernanceService.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/service/impl/ProtocolSecurityGovernanceServiceImpl.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolFamilyDefinitionVO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolDecryptProfileVO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolDecryptPreviewVO.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/governance/ProtocolSecurityGovernanceApprovalPayloads.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/governance/ProtocolSecurityGovernanceApprovalExecutor.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ProtocolGovernanceController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java`
- Modify: `sql/init-data.sql`
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/service/impl/ProtocolSecurityGovernanceServiceImplTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ProtocolGovernanceControllerTest.java`

- [ ] **Step 1: Write the failing protocol governance tests**

```java
@Test
void submitFamilyPublishShouldCreateApprovalUsingProtocolGovernanceReviewer() {
    when(familyRecordMapper.selectById(9101L)).thenReturn(familyRecord(9101L, "legacy-dp-crack", "DRAFT", 2));
    when(policyResolver.resolveApproverUserId(
            ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH,
            10001L
    )).thenReturn(20002L);
    when(approvalService.submitAction(any())).thenReturn(99101L);

    GovernanceSubmissionResultVO result = service.submitFamilyPublish(9101L, 10001L, "发布裂缝协议族");

    assertEquals(99101L, result.getApprovalOrderId());
    verify(approvalService).submitAction(argThat(command ->
            ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH.equals(command.actionCode())
                    && Long.valueOf(20002L).equals(command.approverUserId())
                    && command.payloadJson().contains("\"familyCode\":\"legacy-dp-crack\"")
    ));
}
```

```java
@Test
void saveDecryptProfileShouldRequireProtocolGovernanceEditPermission() {
    ProtocolDecryptProfileVO vo = new ProtocolDecryptProfileVO();
    vo.setId(9201L);
    vo.setProfileCode("des-62000001");
    vo.setStatus("DRAFT");
    when(service.saveDecryptProfile(any())).thenReturn(vo);

    R<ProtocolDecryptProfileVO> response = controller.saveDecryptProfile(
            new ProtocolDecryptProfileUpsertDTO("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001"),
            authentication(10001L)
    );

    assertEquals("des-62000001", response.getData().getProfileCode());
    verify(permissionGuard).requireAnyPermission(10001L, "协议解密档案维护", GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT);
}
```

- [ ] **Step 2: Run the focused protocol governance tests to confirm RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-framework,spring-boot-iot-system -am "-Dtest=ProtocolSecurityGovernanceServiceImplTest,ProtocolGovernanceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- missing protocol governance entities, service, controller, or permission constants

- [ ] **Step 3: Add protocol governance tables to the schema registry and regenerate artifacts**

```json
{
  "name": "iot_protocol_family_definition",
  "comment": "协议族治理主表",
  "columns": [
    { "name": "id", "type": "bigint", "nullable": false },
    { "name": "family_code", "type": "varchar(128)", "nullable": false },
    { "name": "protocol_code", "type": "varchar(64)", "nullable": false },
    { "name": "display_name", "type": "varchar(255)", "nullable": false },
    { "name": "decrypt_profile_code", "type": "varchar(128)", "nullable": true },
    { "name": "status", "type": "varchar(32)", "nullable": false, "default": "'DRAFT'" },
    { "name": "version_no", "type": "int", "nullable": false, "default": "1" }
  ]
}
```

```json
{
  "name": "iot_protocol_decrypt_profile",
  "comment": "协议解密档案治理主表",
  "columns": [
    { "name": "id", "type": "bigint", "nullable": false },
    { "name": "profile_code", "type": "varchar(128)", "nullable": false },
    { "name": "algorithm", "type": "varchar(64)", "nullable": false },
    { "name": "merchant_source", "type": "varchar(64)", "nullable": false },
    { "name": "merchant_key", "type": "varchar(128)", "nullable": false },
    { "name": "status", "type": "varchar(32)", "nullable": false, "default": "'DRAFT'" },
    { "name": "version_no", "type": "int", "nullable": false, "default": "1" }
  ]
}
```

Run:

```powershell
python scripts/schema/render_artifacts.py --write
python scripts/schema/check_schema_registry.py
```

Expected:

- registry passes
- regenerated `sql/init.sql` and `schema/generated/mysql-schema-sync.json` reflect the 4 new protocol tables and snapshot relations

- [ ] **Step 4: Implement protocol governance service and controller contracts**

```java
public interface ProtocolSecurityGovernanceService {

    PageResult<ProtocolFamilyDefinitionVO> pageFamilies(String keyword, String status, Long pageNum, Long pageSize);

    PageResult<ProtocolDecryptProfileVO> pageDecryptProfiles(String keyword, String status, Long pageNum, Long pageSize);

    ProtocolFamilyDefinitionVO saveFamily(ProtocolFamilyDefinitionUpsertDTO dto);

    ProtocolDecryptProfileVO saveDecryptProfile(ProtocolDecryptProfileUpsertDTO dto);

    GovernanceSubmissionResultVO submitFamilyPublish(Long familyId, Long operatorUserId, String submitReason);

    GovernanceSubmissionResultVO submitDecryptProfilePublish(Long profileId, Long operatorUserId, String submitReason);

    ProtocolDecryptPreviewVO previewDecrypt(ProtocolDecryptPreviewDTO dto);
}
```

```java
@RestController
public class ProtocolGovernanceController {

    @PostMapping("/api/governance/protocol/families")
    public R<ProtocolFamilyDefinitionVO> saveFamily(@RequestBody @Valid ProtocolFamilyDefinitionUpsertDTO dto,
                                                    Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(currentUserId, "协议族维护", GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT);
        return R.ok(service.saveFamily(dto));
    }

    @PostMapping("/api/governance/protocol/decrypt-profiles/preview")
    public R<ProtocolDecryptPreviewVO> previewDecrypt(@RequestBody @Valid ProtocolDecryptPreviewDTO dto,
                                                      Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(currentUserId, "协议解密验证", GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT);
        return R.ok(service.previewDecrypt(dto));
    }
}
```

- [ ] **Step 5: Add protocol-specific permissions, approval routing, and fixed reviewer seeds**

```java
public static final String PROTOCOL_GOVERNANCE_EDIT = "iot:protocol-governance:edit";
public static final String PROTOCOL_GOVERNANCE_APPROVE = "iot:protocol-governance:approve";
```

```java
case ACTION_PROTOCOL_FAMILY_PUBLISH,
     ACTION_PROTOCOL_FAMILY_ROLLBACK,
     ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH,
     ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK -> requireProtocolGovernancePermissions(actorUserId, operation);
```

Implementation rules:

1. 协议治理与产品契约治理权限分开，新增 `iot:protocol-governance:edit` 和 `iot:protocol-governance:approve`。
2. `sql/init-data.sql` 中新增 `/protocol-governance` 菜单、按钮权限和四个 action code 的固定复核人策略。
3. `previewDecrypt(...)` 只能做试算，不允许直接写入 published snapshot。

- [ ] **Step 6: Re-run the protocol governance tests until GREEN**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-framework,spring-boot-iot-system -am "-Dtest=ProtocolSecurityGovernanceServiceImplTest,ProtocolGovernanceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- protocol governance backend tests pass

- [ ] **Step 7: Commit the protocol governance backend**

```powershell
git add schema/mysql/governance-domain.json schema/generated/mysql-schema-sync.json sql/init.sql sql/init-data.sql `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolFamilyDefinitionRecord.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolDecryptProfileRecord.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolFamilyDefinitionSnapshot.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/entity/ProtocolDecryptProfileSnapshot.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolFamilyDefinitionRecordMapper.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolDecryptProfileRecordMapper.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolFamilyDefinitionSnapshotMapper.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/mapper/ProtocolDecryptProfileSnapshotMapper.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolFamilyDefinitionUpsertDTO.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolDecryptProfileUpsertDTO.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/dto/ProtocolDecryptPreviewDTO.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/service/ProtocolSecurityGovernanceService.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/service/impl/ProtocolSecurityGovernanceServiceImpl.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolFamilyDefinitionVO.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolDecryptProfileVO.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/vo/ProtocolDecryptPreviewVO.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/governance/ProtocolSecurityGovernanceApprovalPayloads.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/governance/ProtocolSecurityGovernanceApprovalExecutor.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ProtocolGovernanceController.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java `
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePermissionMatrixServiceImpl.java `
  spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/service/impl/ProtocolSecurityGovernanceServiceImplTest.java `
  spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ProtocolGovernanceControllerTest.java
git commit -m "feat: add protocol governance backend"
```

## Task 4: Bridge published protocol definitions into runtime with YAML fallback

**Files:**
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/ProtocolSecurityDefinitionProvider.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/PublishedProtocolSecurityDefinitionProvider.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/YamlProtocolSecurityDefinitionProvider.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/CompositeProtocolSecurityDefinitionProvider.java`
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolver.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpFamilyResolver.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuntimeServiceImpl.java`
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/CompositeProtocolSecurityDefinitionProviderTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolverTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuntimeServiceImplTest.java`

- [ ] **Step 1: Write the failing provider-bridge tests**

```java
@Test
void shouldPreferPublishedDefinitionsOverYamlFallback() {
    ProtocolSecurityDefinitionProvider published = stubProvider(
            List.of(family("legacy-dp-crack", "des-62000001")),
            List.of(profile("des-62000001", "DES"))
    );
    ProtocolSecurityDefinitionProvider yaml = stubProvider(
            List.of(family("legacy-dp-crack", "aes-62000000")),
            List.of(profile("aes-62000000", "AES"))
    );

    CompositeProtocolSecurityDefinitionProvider provider =
            new CompositeProtocolSecurityDefinitionProvider(List.of(published, yaml));

    assertEquals("des-62000001", provider.getFamilyDefinition("legacy-dp-crack").getDecryptProfileCode());
    assertEquals("DES", provider.getDecryptProfile("des-62000001").getAlgorithm());
}
```

```java
@Test
void resolveForGovernanceShouldReadPublishedSnapshotBeforeDraftTable() {
    VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
    snapshot.setRawIdentifier("disp");
    snapshot.setTargetNormativeIdentifier("value");
    when(snapshotMapper.selectPublishedByProductId(1001L)).thenReturn(List.of(snapshot));
    when(ruleMapper.selectList(any())).thenReturn(List.of(rule(7101L, 1001L, "disp", "sensor_state", "DRAFT", 4)));

    MappingResolution resolution = service.resolveForGovernance(product(1001L), "disp", "L1_LF_1");

    assertEquals("value", resolution.targetNormativeIdentifier());
}
```

- [ ] **Step 2: Run the bridge tests to confirm RED**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-framework,spring-boot-iot-protocol,spring-boot-iot-device -am "-Dtest=CompositeProtocolSecurityDefinitionProviderTest,IotPropertiesProtocolDecryptProfileResolverTest,VendorMetricMappingRuntimeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- compile or assertion failures because runtime still reads raw `IotProperties` / draft rule rows

- [ ] **Step 3: Add the runtime provider abstraction**

```java
public interface ProtocolSecurityDefinitionProvider {

    ProtocolFamilyDefinition getFamilyDefinition(String familyCode);

    ProtocolDecryptProfile getDecryptProfile(String profileCode);

    Map<String, ProtocolFamilyDefinition> listFamilyDefinitions();

    Map<String, ProtocolDecryptProfile> listDecryptProfiles();
}
```

```java
@Component
public class CompositeProtocolSecurityDefinitionProvider implements ProtocolSecurityDefinitionProvider {

    private final List<ProtocolSecurityDefinitionProvider> providers;

    @Override
    public ProtocolFamilyDefinition getFamilyDefinition(String familyCode) {
        return providers.stream()
                .map(provider -> provider.getFamilyDefinition(familyCode))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 4: Rewire runtime consumers to the provider and published snapshots**

```java
public class IotPropertiesProtocolDecryptProfileResolver implements ProtocolDecryptProfileResolver {

    private final ProtocolSecurityDefinitionProvider definitionProvider;

    @Override
    public ProtocolDecryptProfile resolveOrThrow(ProtocolDecryptResolveContext context) {
        ProtocolDecryptProfile familyResolved = resolveByFamily(context);
        if (familyResolved != null) {
            return familyResolved;
        }
        ProtocolDecryptProfile appIdResolved = resolveByAppId(context == null ? null : context.appId());
        if (appIdResolved != null) {
            return appIdResolved;
        }
        throw new BizException("未找到 appId 对应的 decrypt profile: " + safeAppId(context));
    }
}
```

```java
private boolean isConfiguredProtocolFamilySelector(String protocolCode) {
    String familyCode = normalizeProtocolSelector(protocolCode).substring(PROTOCOL_FAMILY_SELECTOR_PREFIX.length());
    ProtocolFamilyDefinition definition = protocolSecurityDefinitionProvider.getFamilyDefinition(familyCode);
    return definition != null && !Boolean.FALSE.equals(definition.getEnabled());
}
```

Implementation rules:

1. Published provider 优先读取 snapshot 表；只要 DB 已发布命中，就不能再回退到 YAML。
2. YAML provider 继续把 `iot.protocol.family-definitions` / `decrypt-profiles` 投影成 fallback 对象，不复制密钥真相。
3. `VendorMetricMappingRuntimeServiceImpl` 必须先读 `iot_vendor_metric_mapping_rule_snapshot`；只有还没有任何 published snapshot 时才回退主表 `ACTIVE/DRAFT` 兼容路径。
4. 保留 `IotPropertiesProtocolDecryptProfileResolver` 类名，避免 bean churn，但其内部不能再直接依赖 `IotProperties` 真相。

- [ ] **Step 5: Re-run the bridge tests until GREEN**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-framework,spring-boot-iot-protocol,spring-boot-iot-device -am "-Dtest=CompositeProtocolSecurityDefinitionProviderTest,IotPropertiesProtocolDecryptProfileResolverTest,VendorMetricMappingRuntimeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- provider priority, decrypt resolver, and mapping runtime tests all pass

- [ ] **Step 6: Commit the runtime bridge**

```powershell
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/ProtocolSecurityDefinitionProvider.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/PublishedProtocolSecurityDefinitionProvider.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/YamlProtocolSecurityDefinitionProvider.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/protocol/CompositeProtocolSecurityDefinitionProvider.java `
  spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java `
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolver.java `
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpFamilyResolver.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java `
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuntimeServiceImpl.java `
  spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/protocol/CompositeProtocolSecurityDefinitionProviderTest.java `
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolverTest.java `
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuntimeServiceImplTest.java
git commit -m "refactor: prefer published protocol definitions at runtime"
```

## Task 5: Add the `/products` vendor mapping rule ledger UI

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts`
- Create: `spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingRuleLedgerPanel.test.ts`

- [ ] **Step 1: Write the failing UI tests for the rule ledger panel**

```ts
it('renders published snapshot state and publish action for a draft mapping rule', async () => {
  vi.mocked(listVendorMetricMappingRuleLedger).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      {
        ruleId: 7101,
        rawIdentifier: 'disp',
        targetNormativeIdentifier: 'value',
        scopeType: 'PRODUCT',
        draftStatus: 'DRAFT',
        draftVersionNo: 4,
        publishedStatus: 'PUBLISHED',
        publishedVersionNo: 3,
        latestApprovalOrderId: 99001
      }
    ]
  })

  const wrapper = mount(ProductVendorMappingRuleLedgerPanel, {
    props: { productId: 1001 }
  })

  await flushPromises()

  expect(wrapper.text()).toContain('disp')
  expect(wrapper.text()).toContain('DRAFT')
  expect(wrapper.text()).toContain('PUBLISHED')
  expect(wrapper.text()).toContain('v4 / v3')
  expect(wrapper.find('[data-testid="rule-ledger-submit-publish-7101"]').exists()).toBe(true)
})
```

- [ ] **Step 2: Run the focused Vitest suite to confirm RED**

Run:

```powershell
if (!(Test-Path 'spring-boot-iot-ui\node_modules')) { npm --prefix spring-boot-iot-ui ci }
npm --prefix spring-boot-iot-ui test -- --run ProductVendorMappingRuleLedgerPanel ProductModelDesignerWorkspace
```

Expected:

- `FAIL`
- missing ledger component, missing API helpers, or outdated workspace assertions

- [ ] **Step 3: Extend frontend API types and request helpers**

```ts
export interface VendorMetricMappingRuleLedgerRow {
  ruleId?: IdType | null
  productId?: IdType | null
  rawIdentifier?: string | null
  targetNormativeIdentifier?: string | null
  scopeType?: VendorMetricMappingRuleScopeType | null
  draftStatus?: VendorMetricMappingRuleLifecycleStatus | null
  draftVersionNo?: number | null
  publishedStatus?: 'PUBLISHED' | 'ROLLED_BACK' | null
  publishedVersionNo?: number | null
  latestApprovalOrderId?: IdType | null
  publishedSource?: string | null
}
```

```ts
export function listVendorMetricMappingRuleLedger(productId: IdType) {
  return request<VendorMetricMappingRuleLedgerRow[]>(`/api/device/product/${productId}/vendor-mapping-rules/ledger`, {
    method: 'GET'
  })
}

export function submitVendorMetricMappingRulePublish(productId: IdType, ruleId: IdType, submitReason: string) {
  return request<GovernanceSubmissionResult>(`/api/device/product/${productId}/vendor-mapping-rules/${ruleId}/submit-publish`, {
    method: 'POST',
    body: { submitReason }
  })
}
```

- [ ] **Step 4: Implement the ledger panel and embed it into the contract workspace**

```vue
<IotAccessTabWorkspace
  title="映射规则台账"
  description="查看草稿、已发布版本、审批单和命中试算。"
>
  <StandardWorkbenchPanel>
    <ProductVendorMappingRuleLedgerPanel
      :product-id="product.id"
      @accepted="handleVendorSuggestionAccepted"
    />
  </StandardWorkbenchPanel>
</IotAccessTabWorkspace>
```

```vue
<template>
  <section class="product-vendor-rule-ledger" data-testid="product-vendor-rule-ledger">
    <article v-for="row in rows" :key="String(row.ruleId)" class="product-vendor-rule-ledger__item">
      <strong>{{ row.rawIdentifier }} -> {{ row.targetNormativeIdentifier }}</strong>
      <span>{{ row.scopeType }} · {{ `v${row.draftVersionNo ?? '--'} / v${row.publishedVersionNo ?? '--'}` }}</span>
      <span>{{ row.draftStatus || '--' }} · {{ row.publishedStatus || '未发布' }}</span>
      <StandardButton :data-testid="`rule-ledger-submit-publish-${row.ruleId}`" @click="submitPublish(row)">
        提交发布审批
      </StandardButton>
    </article>
  </section>
</template>
```

Implementation rules:

1. 不新增页面私有壳层；继续复用 `StandardWorkbenchPanel` 和现有产品工作区样式。
2. 文案必须保持 UTF-8，可读中文，结束前检查是否出现乱码。
3. 台账只回答“草稿是什么、正式是什么、审批到哪一步、能否试命中”，不在产品页复制一整套协议治理页面。

- [ ] **Step 5: Re-run the frontend tests until GREEN**

Run:

```powershell
npm --prefix spring-boot-iot-ui test -- --run ProductVendorMappingRuleLedgerPanel ProductModelDesignerWorkspace
```

Expected:

- targeted tests pass
- `/products` 契约工作区可显示映射规则台账区块

- [ ] **Step 6: Commit the `/products` rule ledger UI**

```powershell
git add spring-boot-iot-ui/src/types/api.ts `
  spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts `
  spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue `
  spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue `
  spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts `
  spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingRuleLedgerPanel.test.ts
git commit -m "feat: add product vendor mapping rule ledger"
```

## Task 6: Add `/protocol-governance` workbench, update docs, and run real-environment verification

**Files:**
- Create: `spring-boot-iot-ui/src/api/protocolGovernance.ts`
- Create: `spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue`
- Create: `spring-boot-iot-ui/src/__tests__/views/ProtocolGovernanceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Write the failing protocol governance workbench test**

```ts
it('shows families and decrypt profiles under the access workbench without replacing /device-access', async () => {
  vi.mocked(pageProtocolFamilies).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [{ id: 9101, familyCode: 'legacy-dp-crack', protocolCode: 'mqtt-json', status: 'DRAFT' }]
    }
  })
  vi.mocked(pageProtocolDecryptProfiles).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [{ id: 9201, profileCode: 'des-62000001', algorithm: 'DES', status: 'DRAFT' }]
    }
  })

  const wrapper = mount(ProtocolGovernanceWorkbenchView, {
    global: {
      plugins: [router]
    }
  })

  await flushPromises()

  expect(wrapper.text()).toContain('协议治理工作台')
  expect(wrapper.text()).toContain('legacy-dp-crack')
  expect(wrapper.text()).toContain('des-62000001')
})
```

- [ ] **Step 2: Run the protocol governance UI test to confirm RED**

Run:

```powershell
npm --prefix spring-boot-iot-ui test -- --run ProtocolGovernanceWorkbenchView
```

Expected:

- `FAIL`
- missing route, API client, or workbench view

- [ ] **Step 3: Build the new access-family page and workspace navigation entry**

```ts
{
  path: '/protocol-governance',
  name: 'protocol-governance',
  component: () => import('../views/ProtocolGovernanceWorkbenchView.vue'),
  meta: routeMeta('/protocol-governance')
}
```

```ts
{ path: '/protocol-governance', label: '协议治理工作台', description: '维护协议族定义、解密档案和共享 scope 规则。', short: '协', keywords: ['协议治理', '解密档案', '协议族'] }
```

```vue
<IotAccessPageShell
  title="协议治理工作台"
  description="集中维护协议族定义、解密档案和共享 scope 的映射规则。"
>
  <StandardWorkbenchPanel title="协议族定义" description="发布后成为运行时协议族真相。">
    <!-- family table -->
  </StandardWorkbenchPanel>
  <StandardWorkbenchPanel title="解密档案" description="发布后优先于 YAML fallback。">
    <!-- decrypt profile table -->
  </StandardWorkbenchPanel>
</IotAccessPageShell>
```

Implementation rules:

1. `/device-access` 仍然是接入智维总览，不能被改造成配置编辑页。
2. `/protocol-governance` 必须作为接入智维卡片页里的一个新 page，而不是第二个 overview。
3. 页面结构优先复用 `IotAccessPageShell`、`StandardWorkbenchPanel`、共享列表样式和现有品牌色。

- [ ] **Step 4: Update docs in place and review top-level docs**

Docs must state:

1. `docs/02-业务功能与流程说明.md`：新增协议治理工作台和厂商映射规则台账流程
2. `docs/03-接口规范与接口清单.md`：新增映射规则发布/回滚/试命中 API 与协议治理 CRUD/API
3. `docs/04-数据库设计与初始化数据.md`：新增 5 张治理表的职责、关系和 seed 说明
4. `docs/07-部署运行与配置说明.md`：说明 DB published provider 优先、YAML fallback 次之
5. `docs/08-变更记录与技术债清单.md`：记录本轮 Phase 1 平台化收口事实和未做边界
6. `docs/15-前端优化与治理计划.md`：记录 `/protocol-governance` 作为接入智维 page、不能回流为 overview
7. `docs/21-业务功能清单与验收标准.md`：补 Phase 1 新验收点

Review rules:

1. `README.md` 只有在启动、模块定位或真实环境验收口径变旧时才更新。
2. `AGENTS.md` 只有在协作规则、命令或边界发生变化时才更新；若无变化，显式保持不变。

- [ ] **Step 5: Run backend, frontend, docs, and real-environment verification**

Run:

```powershell
npm --prefix spring-boot-iot-ui test -- --run ProductVendorMappingRuleLedgerPanel ProductModelDesignerWorkspace ProtocolGovernanceWorkbenchView
node scripts/docs/check-topology.mjs
node scripts/run-governance-contract-gates.mjs
mvn -s .mvn/settings.xml -pl spring-boot-iot-framework,spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-protocol -am test
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:

- frontend targeted tests pass
- docs topology check passes
- governance contract gates pass
- module tests pass
- admin package build succeeds

Run real-environment smoke:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Expected manual checks in the shared dev environment:

1. `/products?openProductId=<productId>&workbenchView=models` 能看到映射规则台账、提交发布审批、试命中结果
2. `/protocol-governance` 能看到协议族定义和解密档案列表
3. 新发布的协议族/解密档案在运行时命中时优先于 YAML fallback
4. `/governance-approval` 能看到 Phase 1 新 action code 的审批单

- [ ] **Step 6: Commit UI, docs, and verification updates**

```powershell
git add spring-boot-iot-ui/src/api/protocolGovernance.ts `
  spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue `
  spring-boot-iot-ui/src/__tests__/views/ProtocolGovernanceWorkbenchView.test.ts `
  spring-boot-iot-ui/src/router/index.ts `
  spring-boot-iot-ui/src/types/api.ts `
  spring-boot-iot-ui/src/utils/sectionWorkspaces.ts `
  docs/02-业务功能与流程说明.md `
  docs/03-接口规范与接口清单.md `
  docs/04-数据库设计与初始化数据.md `
  docs/07-部署运行与配置说明.md `
  docs/08-变更记录与技术债清单.md `
  docs/15-前端优化与治理计划.md `
  docs/21-业务功能清单与验收标准.md
git commit -m "feat: add protocol governance workbench"
```

## Self-Review Checklist

Spec coverage:

1. 厂商字段映射规则正式治理生命周期：Tasks 1, 2, 5
2. 协议族定义与解密档案正式治理对象：Task 3
3. Published DB object priority + YAML fallback：Task 4
4. `/products` 规则台账：Task 5
5. `/protocol-governance` 页面与接入智维导航：Task 6
6. 审批、权限、固定复核人和真实环境验收：Tasks 2, 3, 6

Placeholder scan:

1. 完成后执行：

```powershell
$patterns = @('TO' + 'DO', 'TB' + 'D', 'implement' + ' later', 'similar' + ' to')
Select-String -Path 'docs\superpowers\plans\2026-04-15-no-code-onboarding-phase1-rule-governance-implementation-plan.md' -Pattern $patterns -Encoding UTF8
```

Expected:

- no matches

Type consistency:

1. 厂商映射规则动作编码统一使用 `VENDOR_MAPPING_RULE_PUBLISH` / `VENDOR_MAPPING_RULE_ROLLBACK`
2. 协议治理动作编码统一使用 `PROTOCOL_FAMILY_PUBLISH` / `PROTOCOL_FAMILY_ROLLBACK` / `PROTOCOL_DECRYPT_PROFILE_PUBLISH` / `PROTOCOL_DECRYPT_PROFILE_ROLLBACK`
3. 运行时 provider 统一使用 `ProtocolSecurityDefinitionProvider`
4. 协议治理权限统一使用 `PROTOCOL_GOVERNANCE_EDIT` / `PROTOCOL_GOVERNANCE_APPROVE`
