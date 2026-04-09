# Risk Metric Linkage And Emergency Plan Binding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace guessed linkage/emergency-plan coverage with explicit MySQL binding truth while keeping the existing `/api/linkage-rule/*`, `/api/emergency-plan/*`, `/api/risk-governance/*` contracts and the real `application-dev.yml` acceptance baseline intact.

**Architecture:** Add `risk_metric_linkage_binding` and `risk_metric_emergency_plan_binding` as first-class truth tables in `spring-boot-iot-alarm`, keep semantic inference only in write-time sync and history backfill, and switch `coverage-overview` plus `dashboard-overview` to read those tables instead of re-guessing from free text on every request. Historical environments are repaired through `sql/init.sql`, `scripts/run-real-env-schema-sync.py`, and a one-shot read-side fallback backfill service.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, Python 3 unittest, Vue 3 TypeScript, Maven

---

## Scope Check

This plan covers only Track 2 from the 2026-04-08 governance object model spec: explicit linkage/plan bindings and coverage truth. Do not pull task workbenches, `iot_governance_task`, replay UI redesign, or protocol-family extensibility objects into this implementation.

## File Structure

### Database / schema / sync

- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`

### Alarm domain binding truth

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricLinkageBinding.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricEmergencyPlanBinding.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricLinkageBindingMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricEmergencyPlanBindingMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricActionBindingSyncService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricActionBindingBackfillService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingSyncServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingBackfillServiceImpl.java`

### Alarm domain write-path wiring

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/LinkageRuleService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/EmergencyPlanService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/LinkageRuleServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/LinkageRuleController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/EmergencyPlanController.java`

### Alarm domain read-side coverage

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskGovernanceCoverageOverviewVO.java`

### Tests

- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/LinkageRuleServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingSyncServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingBackfillServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/LinkageRuleControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/EmergencyPlanControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java`

### Frontend typing

- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`

### Documentation

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

## Task 1: Add binding-table schema truth and schema-sync regression coverage

**Files:**
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`

- [ ] **Step 1: Write the failing schema-sync tests**

```python
class SchemaSyncCoverageTest(unittest.TestCase):
    def test_create_table_sql_covers_action_binding_tables(self):
        linkage_sql = schema_sync.CREATE_TABLE_SQL.get("risk_metric_linkage_binding")
        plan_sql = schema_sync.CREATE_TABLE_SQL.get("risk_metric_emergency_plan_binding")

        self.assertIsNotNone(linkage_sql)
        self.assertIsNotNone(plan_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS risk_metric_linkage_binding", linkage_sql)
        self.assertIn("uk_risk_metric_linkage_active", linkage_sql)
        self.assertIn("idx_risk_metric_linkage_metric", linkage_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS risk_metric_emergency_plan_binding", plan_sql)
        self.assertIn("uk_risk_metric_plan_active", plan_sql)
        self.assertIn("idx_risk_metric_plan_metric", plan_sql)
```

```python
class SchemaSyncCoverageTest(unittest.TestCase):
    def test_index_specs_cover_action_binding_tables(self):
        self.assertIn(
            (
                "idx_risk_metric_linkage_rule",
                "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_rule` (`linkage_rule_id`, `binding_status`, `deleted`)",
            ),
            schema_sync.INDEXES_TO_ADD["risk_metric_linkage_binding"],
        )
        self.assertIn(
            (
                "idx_risk_metric_linkage_metric",
                "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
            ),
            schema_sync.INDEXES_TO_ADD["risk_metric_linkage_binding"],
        )
        self.assertIn(
            (
                "idx_risk_metric_plan_rule",
                "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_rule` (`emergency_plan_id`, `binding_status`, `deleted`)",
            ),
            schema_sync.INDEXES_TO_ADD["risk_metric_emergency_plan_binding"],
        )
        self.assertIn(
            (
                "idx_risk_metric_plan_metric",
                "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
            ),
            schema_sync.INDEXES_TO_ADD["risk_metric_emergency_plan_binding"],
        )
```

- [ ] **Step 2: Run the schema-sync tests to verify the new tables do not exist yet**

Run:

```bash
python3 -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected:

- `FAIL`
- missing `risk_metric_linkage_binding` / `risk_metric_emergency_plan_binding` entries in `CREATE_TABLE_SQL` or `INDEXES_TO_ADD`

- [ ] **Step 3: Implement the DDL and idempotent sync entries**

```sql
CREATE TABLE risk_metric_linkage_binding (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
    linkage_rule_id BIGINT NOT NULL COMMENT '联动规则ID',
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT 'AUTO_INFERRED/MANUAL_CONFIRMED/BACKFILL',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_linkage_active (tenant_id, risk_metric_id, linkage_rule_id, deleted),
    KEY idx_risk_metric_linkage_rule (linkage_rule_id, binding_status, deleted),
    KEY idx_risk_metric_linkage_metric (risk_metric_id, binding_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与联动规则绑定表';

CREATE TABLE risk_metric_emergency_plan_binding (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
    emergency_plan_id BIGINT NOT NULL COMMENT '应急预案ID',
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT 'AUTO_INFERRED/MANUAL_CONFIRMED/BACKFILL',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_plan_active (tenant_id, risk_metric_id, emergency_plan_id, deleted),
    KEY idx_risk_metric_plan_rule (emergency_plan_id, binding_status, deleted),
    KEY idx_risk_metric_plan_metric (risk_metric_id, binding_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与应急预案绑定表';
```

```python
CREATE_TABLE_SQL["risk_metric_linkage_binding"] = """
CREATE TABLE IF NOT EXISTS risk_metric_linkage_binding (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
    linkage_rule_id BIGINT NOT NULL COMMENT '联动规则ID',
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT 'AUTO_INFERRED/MANUAL_CONFIRMED/BACKFILL',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_linkage_active (tenant_id, risk_metric_id, linkage_rule_id, deleted),
    KEY idx_risk_metric_linkage_rule (linkage_rule_id, binding_status, deleted),
    KEY idx_risk_metric_linkage_metric (risk_metric_id, binding_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与联动规则绑定表'
"""
```

```python
INDEXES_TO_ADD["risk_metric_linkage_binding"] = [
    (
        "idx_risk_metric_linkage_rule",
        "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_rule` (`linkage_rule_id`, `binding_status`, `deleted`)",
    ),
    (
        "idx_risk_metric_linkage_metric",
        "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
    ),
]
INDEXES_TO_ADD["risk_metric_emergency_plan_binding"] = [
    (
        "idx_risk_metric_plan_rule",
        "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_rule` (`emergency_plan_id`, `binding_status`, `deleted`)",
    ),
    (
        "idx_risk_metric_plan_metric",
        "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
    ),
]
```

Implementation rules:

- `sql/init.sql` must place both tables in the alarm/risk domain section, after `risk_metric_catalog` and before `risk_point_device`.
- `scripts/run-real-env-schema-sync.py` must create the tables when missing and keep the index creation idempotent for historical libraries.
- Do not add foreign keys; stay consistent with the rest of this repository’s soft-delete schema style.

- [ ] **Step 4: Run the schema-sync regression tests until they pass**

Run:

```bash
python3 -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected:

- `OK`
- the new binding tables are now covered by the schema-sync regression suite

- [ ] **Step 5: Commit the schema truth foundation**

```bash
git add sql/init.sql scripts/run-real-env-schema-sync.py scripts/tests/test_run_real_env_schema_sync.py
git commit -m "feat: add risk metric action binding schema"
```

## Task 2: Build binding entities, mappers, and write-time sync service with TDD

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricLinkageBinding.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricEmergencyPlanBinding.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricLinkageBindingMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricEmergencyPlanBindingMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricActionBindingSyncService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingSyncServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingSyncServiceImplTest.java`

- [ ] **Step 1: Write the failing sync-service tests**

```java
@Test
void rebuildLinkageBindingsForRuleShouldInsertNewBindingsAndRetireStaleOnes() {
    LinkageRule rule = new LinkageRule();
    rule.setId(7001L);
    rule.setTenantId(1L);
    rule.setTriggerCondition("[{\"metricIdentifier\":\"value\"},{\"metricIdentifier\":\"gpsTotalX\"}]");

    when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
            enabledCatalog(9101L, "value"),
            enabledCatalog(9102L, "gpsTotalX")
    ));
    when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
            linkageBinding(9901L, 1L, 9103L, 7001L, "ACTIVE", "AUTO_INFERRED", 0)
    ));

    service.rebuildLinkageBindingsForRule(rule, 1001L, "AUTO_INFERRED");

    verify(linkageBindingMapper).insert(argThat(binding ->
            Long.valueOf(9101L).equals(binding.getRiskMetricId())
                    && Long.valueOf(7001L).equals(binding.getLinkageRuleId())
                    && "ACTIVE".equals(binding.getBindingStatus())
                    && "AUTO_INFERRED".equals(binding.getBindingOrigin())
                    && Long.valueOf(1001L).equals(binding.getCreateBy())
    ));
    verify(linkageBindingMapper).updateById(argThat(binding ->
            Long.valueOf(9901L).equals(binding.getId())
                    && "INACTIVE".equals(binding.getBindingStatus())
                    && Integer.valueOf(1).equals(binding.getDeleted())
    ));
}
```

```java
@Test
void rebuildEmergencyPlanBindingsForPlanShouldMatchMetricsFromPlanSearchText() {
    EmergencyPlan plan = new EmergencyPlan();
    plan.setId(7101L);
    plan.setTenantId(1L);
    plan.setPlanName("裂缝 value 指标预案");
    plan.setDescription("针对 gpsTotalX 和 value 的处置说明");

    when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
            enabledCatalog(9101L, "value"),
            enabledCatalog(9102L, "gpsTotalX")
    ));
    when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

    service.rebuildEmergencyPlanBindingsForPlan(plan, 1002L, "AUTO_INFERRED");

    verify(emergencyPlanBindingMapper).insert(argThat(binding ->
            Long.valueOf(9101L).equals(binding.getRiskMetricId())
                    && Long.valueOf(7101L).equals(binding.getEmergencyPlanId())
                    && "ACTIVE".equals(binding.getBindingStatus())
                    && "AUTO_INFERRED".equals(binding.getBindingOrigin())
                    && Long.valueOf(1002L).equals(binding.getCreateBy())
    ));
    verify(emergencyPlanBindingMapper).insert(argThat(binding ->
            Long.valueOf(9102L).equals(binding.getRiskMetricId())
                    && Long.valueOf(7101L).equals(binding.getEmergencyPlanId())
    ));
}
```

- [ ] **Step 2: Run the targeted Maven tests to verify the new binding types do not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RiskMetricActionBindingSyncServiceImplTest test
```

Expected:

- `BUILD FAILURE`
- compile failures for missing `RiskMetricLinkageBinding`, `RiskMetricEmergencyPlanBinding`, mapper classes, or `RiskMetricActionBindingSyncServiceImpl`

- [ ] **Step 3: Implement the binding entities, mappers, and sync service**

```java
@Data
@TableName("risk_metric_linkage_binding")
public class RiskMetricLinkageBinding {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long riskMetricId;
    private Long linkageRuleId;
    private String bindingStatus;
    private String bindingOrigin;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer deleted;
}
```

```java
public interface RiskMetricActionBindingSyncService {

    void rebuildLinkageBindingsForRule(LinkageRule rule, Long operatorId, String bindingOrigin);

    void rebuildEmergencyPlanBindingsForPlan(EmergencyPlan plan, Long operatorId, String bindingOrigin);

    void deactivateLinkageBindings(Long linkageRuleId, Long operatorId);

    void deactivateEmergencyPlanBindings(Long emergencyPlanId, Long operatorId);
}
```

```java
private Set<Long> resolveMatchedRiskMetricIdsFromTriggerCondition(String triggerCondition) {
    Set<String> identifiers = extractMetricIdentifiersFromTriggerCondition(triggerCondition);
    if (identifiers.isEmpty()) {
        return Set.of();
    }
    return riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                    .eq(RiskMetricCatalog::getDeleted, 0)
                    .eq(RiskMetricCatalog::getEnabled, 1))
            .stream()
            .filter(catalog -> identifiers.contains(normalize(catalog.getContractIdentifier())))
            .map(RiskMetricCatalog::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
}
```

```java
private Set<Long> resolveMatchedRiskMetricIdsFromEmergencyPlan(EmergencyPlan plan) {
    String searchableText = buildEmergencyPlanSearchText(plan);
    if (!StringUtils.hasText(searchableText)) {
        return Set.of();
    }
    return riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                    .eq(RiskMetricCatalog::getDeleted, 0)
                    .eq(RiskMetricCatalog::getEnabled, 1))
            .stream()
            .filter(catalog -> searchableText.contains(normalize(catalog.getContractIdentifier()))
                    || searchableText.contains(normalize(catalog.getRiskMetricName())))
            .map(RiskMetricCatalog::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
}
```

Implementation rules:

- Reuse the existing JSON metric extraction and emergency-plan searchable-text logic from `RiskGovernanceServiceImpl` instead of inventing a second inference algorithm.
- New bindings use `binding_status=ACTIVE`, `deleted=0`; stale rows become `binding_status=INACTIVE`, `deleted=1`.
- Use `IdType.ASSIGN_ID` for both binding entities to align with the repository’s non-auto-increment governance tables.

- [ ] **Step 4: Run the sync-service tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RiskMetricActionBindingSyncServiceImplTest test
```

Expected:

- `BUILD SUCCESS`
- the sync service persists active bindings and retires stale bindings for both linkage rules and emergency plans

- [ ] **Step 5: Commit the sync-service foundation**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricLinkageBinding.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricEmergencyPlanBinding.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricLinkageBindingMapper.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricEmergencyPlanBindingMapper.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricActionBindingSyncService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingSyncServiceImpl.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingSyncServiceImplTest.java
git commit -m "feat: add risk metric action binding sync service"
```

## Task 3: Wire existing linkage/emergency APIs to sync and add history backfill

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricActionBindingBackfillService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingBackfillServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/LinkageRuleService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/EmergencyPlanService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/LinkageRuleServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/LinkageRuleController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/EmergencyPlanController.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/LinkageRuleServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingBackfillServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/LinkageRuleControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/EmergencyPlanControllerTest.java`

- [ ] **Step 1: Write the failing write-path and backfill tests**

```java
@Test
void addRuleShouldPersistRuleAndTriggerBindingSyncWithCurrentOperator() {
    LinkageRule rule = new LinkageRule();
    rule.setRuleName("裂缝联动");
    doAnswer(invocation -> {
        LinkageRule saved = invocation.getArgument(0);
        saved.setId(7001L);
        return true;
    }).when(service).save(any(LinkageRule.class));

    service.addRule(rule, 1001L);

    verify(bindingSyncService).rebuildLinkageBindingsForRule(rule, 1001L, "AUTO_INFERRED");
    assertEquals(1001L, rule.getCreateBy());
    assertEquals(1001L, rule.getUpdateBy());
}
```

```java
@Test
void addPlanShouldNormalizeAlarmLevelAndTriggerBindingSync() {
    EmergencyPlan plan = new EmergencyPlan();
    plan.setAlarmLevel("critical");
    doReturn(true).when(service).save(any(EmergencyPlan.class));

    service.addPlan(plan, 1002L);

    assertEquals("red", plan.getAlarmLevel());
    assertEquals("red", plan.getRiskLevel());
    verify(bindingSyncService).rebuildEmergencyPlanBindingsForPlan(plan, 1002L, "AUTO_INFERRED");
}
```

```java
@Test
void ensureBindingsReadyForReadShouldBackfillOnlyMissingSides() {
    when(linkageRuleMapper.selectCount(any())).thenReturn(2L);
    when(emergencyPlanMapper.selectCount(any())).thenReturn(1L);
    when(linkageBindingMapper.selectCount(any())).thenReturn(0L);
    when(emergencyPlanBindingMapper.selectCount(any())).thenReturn(3L);

    backfillService.ensureBindingsReadyForRead();

    verify(backfillService).rebuildAllLinkageBindings();
    verify(backfillService, never()).rebuildAllEmergencyPlanBindings();
}
```

```java
@Test
void addRuleShouldPassCurrentUserIdIntoService() {
    LinkageRule rule = new LinkageRule();

    controller.addRule(rule, 2002L, authentication(1001L));

    verify(linkageRuleService).addRule(rule, 1001L);
}
```

- [ ] **Step 2: Run the write-path Maven tests to verify the new signatures do not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=LinkageRuleServiceImplTest,EmergencyPlanServiceImplTest,RiskMetricActionBindingBackfillServiceImplTest,LinkageRuleControllerTest,EmergencyPlanControllerTest test
```

Expected:

- `BUILD FAILURE`
- compile errors for missing `operatorId` service signatures, missing backfill service, or outdated controller delegation assertions

- [ ] **Step 3: Implement operator-aware service signatures, sync wiring, and backfill service**

```java
public interface LinkageRuleService extends IService<LinkageRule> {

    List<LinkageRule> getRuleList(String ruleName, Integer status);

    PageResult<LinkageRule> pageRuleList(String ruleName, Integer status, Long pageNum, Long pageSize);

    void addRule(LinkageRule rule, Long operatorId);

    void updateRule(LinkageRule rule, Long operatorId);

    void deleteRule(Long id, Long operatorId);
}
```

```java
@Transactional(rollbackFor = Exception.class)
public void addRule(LinkageRule rule, Long operatorId) {
    rule.setDeleted(0);
    rule.setCreateBy(operatorId);
    rule.setUpdateBy(operatorId);
    save(rule);
    bindingSyncService.rebuildLinkageBindingsForRule(rule, operatorId, "AUTO_INFERRED");
}
```

```java
@Transactional(rollbackFor = Exception.class)
public void deletePlan(Long id, Long operatorId) {
    removeById(id);
    bindingSyncService.deactivateEmergencyPlanBindings(id, operatorId);
}
```

```java
public interface RiskMetricActionBindingBackfillService {

    void rebuildAllLinkageBindings();

    void rebuildAllEmergencyPlanBindings();

    void ensureBindingsReadyForRead();
}
```

```java
public void ensureBindingsReadyForRead() {
    long activeLinkageRuleCount = linkageRuleMapper.selectCount(new LambdaQueryWrapper<LinkageRule>()
            .eq(LinkageRule::getDeleted, 0)
            .eq(LinkageRule::getStatus, 0));
    long activeEmergencyPlanCount = emergencyPlanMapper.selectCount(new LambdaQueryWrapper<EmergencyPlan>()
            .eq(EmergencyPlan::getDeleted, 0)
            .eq(EmergencyPlan::getStatus, 0));
    long activeLinkageBindingCount = linkageBindingMapper.selectCount(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
            .eq(RiskMetricLinkageBinding::getDeleted, 0)
            .eq(RiskMetricLinkageBinding::getBindingStatus, "ACTIVE"));
    long activePlanBindingCount = emergencyPlanBindingMapper.selectCount(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
            .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
            .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, "ACTIVE"));

    if (activeLinkageRuleCount > 0 && activeLinkageBindingCount == 0) {
        rebuildAllLinkageBindings();
    }
    if (activeEmergencyPlanCount > 0 && activePlanBindingCount == 0) {
        rebuildAllEmergencyPlanBindings();
    }
}
```

Implementation rules:

- The external HTTP contracts stay unchanged; only controller-to-service delegation becomes operator-aware.
- `LinkageRuleController` and `EmergencyPlanController` must pass `currentUserId` into the service after dual-control validation.
- History backfill uses the same sync service and must mark generated rows with `binding_origin=BACKFILL`.

- [ ] **Step 4: Run the write-path and backfill tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=LinkageRuleServiceImplTest,EmergencyPlanServiceImplTest,RiskMetricActionBindingBackfillServiceImplTest,LinkageRuleControllerTest,EmergencyPlanControllerTest test
```

Expected:

- `BUILD SUCCESS`
- controllers pass current user IDs into services
- add/update/delete operations now keep action bindings in sync
- one-shot backfill only triggers for the missing side

- [ ] **Step 5: Commit the write-path wiring**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricActionBindingBackfillService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingBackfillServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/LinkageRuleService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/EmergencyPlanService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/LinkageRuleServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/LinkageRuleController.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/EmergencyPlanController.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/LinkageRuleServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricActionBindingBackfillServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/LinkageRuleControllerTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/EmergencyPlanControllerTest.java
git commit -m "feat: sync action bindings from linkage and plan writes"
```

## Task 4: Switch governance coverage and dashboard read-side to explicit binding truth

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskGovernanceCoverageOverviewVO.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java`
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`

- [ ] **Step 1: Write the failing read-side tests**

```java
@Test
void getCoverageOverviewShouldUseExplicitBindingsInsteadOfTextGuessing() {
    when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
            enabledCatalog(9101L, 1001L, "value"),
            enabledCatalog(9102L, 1001L, "gpsTotalX")
    ));
    when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
            binding(8001L, 5001L, 9101L, "value", "裂缝监测值"),
            binding(8001L, 5001L, 9102L, "gpsTotalX", "X向累计位移")
    ));
    when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule(6001L, 9101L, "value")));
    when(linkageBindingMapper.selectList(any())).thenReturn(List.of(linkageBinding(9901L, 1L, 9101L, 7001L, "ACTIVE", "AUTO_INFERRED", 0)));
    when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of(planBinding(9951L, 1L, 9102L, 7101L, "ACTIVE", "BACKFILL", 0)));

    RiskGovernanceCoverageOverviewVO overview = service.getCoverageOverview(1001L);

    assertEquals(1L, overview.getLinkageCoveredRiskMetricCount());
    assertEquals(1L, overview.getEmergencyPlanCoveredRiskMetricCount());
    assertEquals(0L, overview.getLinkagePlanCoveredRiskMetricCount());
    assertEquals(50.0, overview.getLinkageCoverageRate());
    assertEquals(50.0, overview.getEmergencyPlanCoverageRate());
    assertEquals(0.0, overview.getLinkagePlanCoverageRate());
}
```

```java
@Test
void getDashboardOverviewShouldTriggerOneShotBackfillBeforeCountingBindings() {
    when(productMapper.selectList(any())).thenReturn(List.of(product(1001L)));
    when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of(releaseBatch(5001L, 1001L)));
    when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(enabledCatalog(9101L, 1001L, "value")));
    when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding(8001L, 5001L, 9101L, "value", "裂缝监测值")));
    when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule(6001L, 9101L, "value")));
    when(linkageBindingMapper.selectList(any())).thenReturn(List.of(linkageBinding(9901L, 1L, 9101L, 7001L, "ACTIVE", "BACKFILL", 0)));
    when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

    service.getDashboardOverview();

    verify(backfillService).ensureBindingsReadyForRead();
}
```

```java
@Test
void getCoverageOverviewShouldExposeActionBindingFields() {
    RiskGovernanceCoverageOverviewVO overview = new RiskGovernanceCoverageOverviewVO();
    overview.setLinkageCoveredRiskMetricCount(3L);
    overview.setEmergencyPlanCoveredRiskMetricCount(2L);
    overview.setLinkagePlanCoveredRiskMetricCount(1L);
    when(service.getCoverageOverview(1001L)).thenReturn(overview);

    R<RiskGovernanceCoverageOverviewVO> response = controller.getCoverageOverview(1001L);

    assertEquals(3L, response.getData().getLinkageCoveredRiskMetricCount());
    assertEquals(2L, response.getData().getEmergencyPlanCoveredRiskMetricCount());
    assertEquals(1L, response.getData().getLinkagePlanCoveredRiskMetricCount());
}
```

- [ ] **Step 2: Run the read-side Maven tests to verify the new fields and dependencies do not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RiskGovernanceServiceImplTest,RiskGovernanceControllerTest test
```

Expected:

- `BUILD FAILURE`
- compile failures for missing coverage fields, missing binding mappers in the service constructor, or missing backfill call

- [ ] **Step 3: Implement the read-side switch and type exposure**

```java
public class RiskGovernanceCoverageOverviewVO {

    private Long productId;
    private Long contractPropertyCount;
    private Long publishedRiskMetricCount;
    private Long boundRiskMetricCount;
    private Long ruleCoveredRiskMetricCount;
    private Long linkageCoveredRiskMetricCount;
    private Long emergencyPlanCoveredRiskMetricCount;
    private Long linkagePlanCoveredRiskMetricCount;
    private Double contractMetricCoverageRate;
    private Double bindingCoverageRate;
    private Double ruleCoverageRate;
    private Double linkageCoverageRate;
    private Double emergencyPlanCoverageRate;
    private Double linkagePlanCoverageRate;
}
```

```java
public RiskGovernanceCoverageOverviewVO getCoverageOverview(Long productId) {
    backfillService.ensureBindingsReadyForRead();
    List<RiskMetricLinkageBinding> linkageBindings = linkageBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
            .eq(RiskMetricLinkageBinding::getDeleted, 0)
            .eq(RiskMetricLinkageBinding::getBindingStatus, "ACTIVE"));
    List<RiskMetricEmergencyPlanBinding> planBindings = emergencyPlanBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
            .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
            .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, "ACTIVE"));

    long linkageCoveredRiskMetricCount = countCoveredDimensionsFromLinkageBindings(boundMetricDimensions, linkageBindings);
    long emergencyPlanCoveredRiskMetricCount = countCoveredDimensionsFromPlanBindings(boundMetricDimensions, planBindings);
    long linkagePlanCoveredRiskMetricCount = countCoveredDimensionsWithBoth(linkageBindings, planBindings, boundMetricDimensions);

    overview.setLinkageCoveredRiskMetricCount(linkageCoveredRiskMetricCount);
    overview.setEmergencyPlanCoveredRiskMetricCount(emergencyPlanCoveredRiskMetricCount);
    overview.setLinkagePlanCoveredRiskMetricCount(linkagePlanCoveredRiskMetricCount);
    overview.setLinkageCoverageRate(calculateRate(linkageCoveredRiskMetricCount, boundMetricDimensionCount));
    overview.setEmergencyPlanCoverageRate(calculateRate(emergencyPlanCoveredRiskMetricCount, boundMetricDimensionCount));
    overview.setLinkagePlanCoverageRate(calculateRate(linkagePlanCoveredRiskMetricCount, boundMetricDimensionCount));
    return overview;
}
```

```java
private long countCoveredDimensionsWithBoth(List<RiskMetricLinkageBinding> linkageBindings,
                                            List<RiskMetricEmergencyPlanBinding> planBindings,
                                            List<MetricBindingDimension> dimensions) {
    Set<String> linkageKeys = linkageBindings.stream()
            .map(binding -> "ID:" + binding.getRiskMetricId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<String> planKeys = planBindings.stream()
            .map(binding -> "ID:" + binding.getRiskMetricId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return dimensions.stream()
            .map(MetricBindingDimension::dimensionKey)
            .filter(linkageKeys::contains)
            .filter(planKeys::contains)
            .count();
}
```

```ts
export interface RiskGovernanceCoverageOverview {
  productId?: IdType | null;
  contractPropertyCount?: number | null;
  publishedRiskMetricCount?: number | null;
  boundRiskMetricCount?: number | null;
  ruleCoveredRiskMetricCount?: number | null;
  linkageCoveredRiskMetricCount?: number | null;
  emergencyPlanCoveredRiskMetricCount?: number | null;
  linkagePlanCoveredRiskMetricCount?: number | null;
  contractMetricCoverageRate?: number | null;
  bindingCoverageRate?: number | null;
  ruleCoverageRate?: number | null;
  linkageCoverageRate?: number | null;
  emergencyPlanCoverageRate?: number | null;
  linkagePlanCoverageRate?: number | null;
}
```

Implementation rules:

- `dashboard-overview` keeps its existing field names; only the truth source changes.
- `coverage-overview` gains the three explicit action-binding count fields and three rates.
- The read path must no longer call the old text-guess helpers for its final answers. If helper extraction logic is still needed, keep it only inside the sync service.

- [ ] **Step 4: Run the read-side tests and UI build until they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RiskGovernanceServiceImplTest,RiskGovernanceControllerTest test
npm --prefix spring-boot-iot-ui run build
```

Expected:

- Maven `BUILD SUCCESS`
- Vite/TypeScript build succeeds with the expanded `RiskGovernanceCoverageOverview` typing

- [ ] **Step 5: Commit the read-side switch**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskGovernanceCoverageOverviewVO.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java \
  spring-boot-iot-ui/src/api/riskGovernance.ts
git commit -m "feat: switch governance coverage to explicit action bindings"
```

## Task 5: Update docs and run full verification in sequence

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update the business, API, database, and change-log docs**

```md
- `GET /api/risk-governance/coverage-overview` 当前新增 `linkageCoveredRiskMetricCount / emergencyPlanCoveredRiskMetricCount / linkagePlanCoveredRiskMetricCount`，
  并明确“联动覆盖率 / 预案覆盖率 / 联动+预案覆盖率”正式真相来自 `risk_metric_linkage_binding` 与 `risk_metric_emergency_plan_binding`。
- `GET /api/risk-governance/dashboard-overview` 保持字段名不变，但底层不再从联动规则文本和预案描述实时猜测命中结果。
```

```md
- `risk_metric_linkage_binding`：风险指标与联动规则绑定表，记录 `risk_metric_id / linkage_rule_id / binding_status / binding_origin`。
- `risk_metric_emergency_plan_binding`：风险指标与应急预案绑定表，记录 `risk_metric_id / emergency_plan_id / binding_status / binding_origin`。
- 历史库对齐脚本 `python scripts/run-real-env-schema-sync.py` 已补齐上述两张表，绑定表可由读侧首次访问触发幂等补账。
```

Documentation rules:

- `README.md` and `AGENTS.md` only change if this work alters the top-level delivered baseline or mandatory reading path. If they do not need content changes, note that explicitly in `docs/08-变更记录与技术债清单.md`.

- [ ] **Step 2: Run the final verification commands sequentially**

Run:

```bash
python3 -m unittest scripts.tests.test_run_real_env_schema_sync
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=LinkageRuleServiceImplTest,EmergencyPlanServiceImplTest,RiskMetricActionBindingSyncServiceImplTest,RiskMetricActionBindingBackfillServiceImplTest,RiskGovernanceServiceImplTest,LinkageRuleControllerTest,EmergencyPlanControllerTest,RiskGovernanceControllerTest test
npm --prefix spring-boot-iot-ui run build
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
git diff --check
```

Expected:

- all Python schema-sync tests pass
- all targeted `spring-boot-iot-alarm` tests pass
- frontend build passes
- `spring-boot-iot-admin` fat package build passes
- `git diff --check` reports no whitespace or conflict-marker issues

- [ ] **Step 3: Commit the docs and verification pass**

```bash
git add docs/02-业务功能与流程说明.md \
  docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md \
  docs/08-变更记录与技术债清单.md
git commit -m "docs: document explicit action binding coverage truth"
```

- [ ] **Step 4: Prepare the handoff note**

```md
本轮完成后必须能明确回答：
1. 联动覆盖率正式真相来自 `risk_metric_linkage_binding`
2. 预案覆盖率正式真相来自 `risk_metric_emergency_plan_binding`
3. 历史规则和预案可通过幂等 backfill 自动补齐绑定
4. `/api/linkage-rule/*`、`/api/emergency-plan/*`、`/api/risk-governance/dashboard-overview` 路径保持兼容
```
