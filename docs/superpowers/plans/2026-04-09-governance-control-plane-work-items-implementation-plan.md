# Governance Control Plane Work Items And Ops Alerts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add first-class governance work items and persistent governance ops alerts so the cockpit, product workbench, and governance control surfaces stop depending on computed-only backlog cards and can track open, blocked, resolved, and closed control-plane actions in the real `application-dev.yml` baseline.

**Architecture:** This plan implements only track 3 from the 2026-04-08 governance object model spec. It adds `iot_governance_work_item` and `iot_governance_ops_alert` in `spring-boot-iot-system`, publishes minimal cross-domain events from `spring-boot-iot-device` and `spring-boot-iot-alarm`, and upgrades the Vue cockpit/workbench pages to read formal control-plane objects. It assumes the bridge metadata plan (`docs/superpowers/plans/2026-04-08-bridge-object-model-foundation-implementation-plan.md`) and the explicit binding truth plan (`docs/superpowers/plans/2026-04-08-risk-metric-linkage-plan-binding-implementation-plan.md`) are the prerequisite tracks.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, Spring application events, Vue 3, TypeScript, Vitest, Maven, Python unittest

---

## Scope Check

This plan covers only track 3 from the approved governance spec:

1. `iot_governance_work_item` control-plane truth
2. `iot_governance_ops_alert` persistence and lifecycle
3. cross-domain event wiring from contract/risk flows into the control plane
4. Vue task/ops workbenches and cockpit consumption

Do not pull protocol-family profiles, decrypt profiles, or replay-case tables into this implementation. Do not duplicate work already covered by the 2026-04-08 bridge or binding-truth plans.

## File Structure

### Database / schema sync
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`

### Common events
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/ProductContractReleasedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/RiskMetricCatalogPublishedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/RiskMetricCoverageChangedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/GovernanceOpsAlertRaisedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/GovernanceOpsAlertRecoveredEvent.java`

### System control-plane backend
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceWorkItem.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceOpsAlert.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceWorkItemMapper.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceOpsAlertMapper.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceWorkItemService.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceOpsAlertService.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/listener/GovernanceWorkItemEventListener.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/listener/GovernanceOpsAlertEventListener.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceWorkItemController.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceOpsAlertController.java`

### Producers / consumers / frontend
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImpl.java`
- Create: `spring-boot-iot-ui/src/api/governanceWorkItem.ts`
- Create: `spring-boot-iot-ui/src/api/governanceOpsAlert.ts`
- Create: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Create: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/views/CockpitView.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`

## Task 1: Add control-plane table truth and schema-sync coverage

**Files:**
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`

- [ ] **Step 1: Write the failing schema-sync tests**

```python
class SchemaSyncCoverageTest(unittest.TestCase):
    def test_create_table_sql_covers_governance_control_plane(self):
        work_item_sql = schema_sync.CREATE_TABLE_SQL.get("iot_governance_work_item")
        ops_alert_sql = schema_sync.CREATE_TABLE_SQL.get("iot_governance_ops_alert")

        self.assertIsNotNone(work_item_sql)
        self.assertIsNotNone(ops_alert_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS iot_governance_work_item", work_item_sql)
        self.assertIn("work_status", work_item_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS iot_governance_ops_alert", ops_alert_sql)
        self.assertIn("alert_status", ops_alert_sql)
```

```python
class SchemaSyncCoverageTest(unittest.TestCase):
    def test_indexes_cover_control_plane_tables(self):
        self.assertIn(
            (
                "idx_governance_work_item_subject",
                "ALTER TABLE `iot_governance_work_item` ADD INDEX `idx_governance_work_item_subject` (`subject_type`, `subject_id`, `work_status`, `deleted`)",
            ),
            schema_sync.INDEXES_TO_ADD["iot_governance_work_item"],
        )
        self.assertIn(
            (
                "uk_governance_ops_alert_code",
                "ALTER TABLE `iot_governance_ops_alert` ADD UNIQUE INDEX `uk_governance_ops_alert_code` (`tenant_id`, `alert_type`, `alert_code`, `deleted`)",
            ),
            schema_sync.INDEXES_TO_ADD["iot_governance_ops_alert"],
        )
```

- [ ] **Step 2: Run the schema-sync tests to verify the tables do not exist yet**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected:

- `FAIL`
- missing `iot_governance_work_item` / `iot_governance_ops_alert` entries in `CREATE_TABLE_SQL` or `INDEXES_TO_ADD`

- [ ] **Step 3: Implement the DDL and schema-sync entries**

```sql
CREATE TABLE iot_governance_work_item (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    work_item_code VARCHAR(64) NOT NULL,
    subject_type VARCHAR(64) NOT NULL,
    subject_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    risk_metric_id BIGINT DEFAULT NULL,
    release_batch_id BIGINT DEFAULT NULL,
    approval_order_id BIGINT DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    device_code VARCHAR(64) DEFAULT NULL,
    product_key VARCHAR(64) DEFAULT NULL,
    work_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    priority_level VARCHAR(16) NOT NULL DEFAULT 'P2',
    assignee_user_id BIGINT DEFAULT NULL,
    source_stage VARCHAR(64) DEFAULT NULL,
    blocking_reason VARCHAR(255) DEFAULT NULL,
    snapshot_json JSON DEFAULT NULL,
    due_time DATETIME DEFAULT NULL,
    resolved_time DATETIME DEFAULT NULL,
    closed_time DATETIME DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理与运营工作项表';

CREATE TABLE iot_governance_ops_alert (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    alert_type VARCHAR(64) NOT NULL,
    alert_code VARCHAR(128) NOT NULL,
    subject_type VARCHAR(64) NOT NULL,
    subject_id BIGINT DEFAULT NULL,
    product_id BIGINT DEFAULT NULL,
    risk_metric_id BIGINT DEFAULT NULL,
    release_batch_id BIGINT DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    device_code VARCHAR(64) DEFAULT NULL,
    product_key VARCHAR(64) DEFAULT NULL,
    alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    severity_level VARCHAR(16) NOT NULL DEFAULT 'WARN',
    affected_count BIGINT NOT NULL DEFAULT 0,
    alert_title VARCHAR(255) NOT NULL,
    alert_message VARCHAR(1000) DEFAULT NULL,
    dimension_key VARCHAR(128) DEFAULT NULL,
    dimension_label VARCHAR(255) DEFAULT NULL,
    source_stage VARCHAR(64) DEFAULT NULL,
    snapshot_json JSON DEFAULT NULL,
    assignee_user_id BIGINT DEFAULT NULL,
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_time DATETIME DEFAULT NULL,
    closed_time DATETIME DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理运维告警表';
```

- [ ] **Step 4: Re-run the schema-sync tests**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
```

Expected: `OK`

- [ ] **Step 5: Commit the schema truth**

```bash
git add sql/init.sql scripts/run-real-env-schema-sync.py scripts/tests/test_run_real_env_schema_sync.py
git commit -m "feat: add governance control plane tables"
```

## Task 2: Implement system-domain work item and ops-alert services with TDD

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceWorkItem.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceOpsAlert.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceWorkItemMapper.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceOpsAlertMapper.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceWorkItemService.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceOpsAlertService.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceWorkItemController.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceOpsAlertController.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImplTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImplTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernanceWorkItemControllerTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernanceOpsAlertControllerTest.java`

- [ ] **Step 1: Write the failing backend unit tests**

```java
@Test
void openOrRefreshWorkItemShouldCreateOpenItemForSubject() {
    GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper);

    service.openOrRefresh(new GovernanceWorkItemCommand(
            "PENDING_CONTRACT_RELEASE",
            "PRODUCT",
            1001L,
            1001L,
            null,
            null,
            null,
            null,
            "MODEL_GOVERNANCE",
            "合同尚未发布",
            "{\"publishedRiskMetricCount\":0}",
            "P1",
            10001L
    ));

    verify(workItemMapper).insert(argThat(item ->
            "PENDING_CONTRACT_RELEASE".equals(item.getWorkItemCode())
                    && "PRODUCT".equals(item.getSubjectType())
                    && Long.valueOf(1001L).equals(item.getSubjectId())
                    && "OPEN".equals(item.getWorkStatus())
    ));
}
```

```java
@Test
void upsertAlertShouldReuseExistingAlertCodeAndRefreshLastSeenTime() {
    GovernanceOpsAlert existing = new GovernanceOpsAlert();
    existing.setId(9001L);
    existing.setAlertType("FIELD_DRIFT");
    existing.setAlertCode("product:1001:value");
    existing.setAlertStatus("OPEN");
    when(alertMapper.selectOne(any())).thenReturn(existing);

    GovernanceOpsAlertServiceImpl service = new GovernanceOpsAlertServiceImpl(alertMapper);
    service.raiseOrRefresh(new GovernanceOpsAlertCommand(
            "FIELD_DRIFT", "product:1001:value", "PRODUCT", 1001L, 1001L,
            null, null, null, null, null,
            "WARN", 3L, "字段漂移告警", "value 已偏离正式合同",
            "product:1001:value", "产品1001/value", "PAYLOAD_APPLY", "{}", 10001L
    ));

    verify(alertMapper).updateById(argThat(alert ->
            Long.valueOf(9001L).equals(alert.getId()) && Long.valueOf(3L).equals(alert.getAffectedCount())
    ));
}
```

- [ ] **Step 2: Run the focused backend tests to verify the types are missing**

Run:

```bash
mvn -pl spring-boot-iot-system -am -Dtest=GovernanceWorkItemServiceImplTest,GovernanceOpsAlertServiceImplTest,GovernanceWorkItemControllerTest,GovernanceOpsAlertControllerTest test
```

Expected:

- `BUILD FAILURE`
- compile failures mentioning missing entities, services, or controller endpoints

- [ ] **Step 3: Implement the minimal entities, services, and controllers**

```java
public interface GovernanceWorkItemService {

    void openOrRefresh(GovernanceWorkItemCommand command);

    void resolve(String workItemCode, String subjectType, Long subjectId, Long operatorUserId, String comment);

    PageResult<GovernanceWorkItemVO> pageWorkItems(GovernanceWorkItemPageQuery query, Long currentUserId);

    void ack(Long workItemId, Long currentUserId, String comment);

    void block(Long workItemId, Long currentUserId, String comment);

    void close(Long workItemId, Long currentUserId, String comment);
}
```

```java
public interface GovernanceOpsAlertService {

    void raiseOrRefresh(GovernanceOpsAlertCommand command);

    void recover(String alertType, String alertCode, Long operatorUserId, String comment);

    PageResult<GovernanceOpsAlertVO> pageAlerts(GovernanceOpsAlertPageQuery query, Long currentUserId);

    void ack(Long alertId, Long currentUserId, String comment);

    void suppress(Long alertId, Long currentUserId, String comment);

    void close(Long alertId, Long currentUserId, String comment);
}
```

```java
@GetMapping("/api/governance/work-items")
public R<PageResult<GovernanceWorkItemVO>> pageWorkItems(GovernanceWorkItemPageQuery query,
                                                         Authentication authentication) {
    return R.ok(governanceWorkItemService.pageWorkItems(query, requireCurrentUserId(authentication)));
}

@PostMapping("/api/governance/ops-alerts/{id}/suppress")
public R<Void> suppressAlert(@PathVariable Long id,
                             @RequestBody(required = false) GovernanceOpsAlertTransitionDTO dto,
                             Authentication authentication) {
    governanceOpsAlertService.suppress(id, requireCurrentUserId(authentication), dto == null ? null : dto.getComment());
    return R.ok();
}
```

- [ ] **Step 4: Re-run the focused backend tests**

Run:

```bash
mvn -pl spring-boot-iot-system -am -Dtest=GovernanceWorkItemServiceImplTest,GovernanceOpsAlertServiceImplTest,GovernanceWorkItemControllerTest,GovernanceOpsAlertControllerTest test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit the system control-plane slice**

```bash
git add spring-boot-iot-system/src/main/java spring-boot-iot-system/src/test/java
git commit -m "feat: add governance control plane services"
```

## Task 3: Publish bridge/risk events and sync work items plus ops alerts

**Files:**
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/ProductContractReleasedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/RiskMetricCatalogPublishedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/RiskMetricCoverageChangedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/GovernanceOpsAlertRaisedEvent.java`
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/GovernanceOpsAlertRecoveredEvent.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/listener/GovernanceWorkItemEventListener.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/listener/GovernanceOpsAlertEventListener.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImplTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/listener/GovernanceWorkItemEventListenerTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/listener/GovernanceOpsAlertEventListenerTest.java`

- [ ] **Step 1: Write the failing producer/listener tests**

```java
@Test
void applyGovernanceShouldPublishReleasedEventWithReleaseBatchId() {
    ProductModelGovernanceApplyResultVO result = service.applyGovernance(1001L, request(), 10001L, 88001L);

    verify(applicationEventPublisher).publishEvent(argThat((ProductContractReleasedEvent event) ->
            Long.valueOf(1001L).equals(event.productId())
                    && Long.valueOf(result.getReleaseBatchId()).equals(event.releaseBatchId())
                    && Long.valueOf(88001L).equals(event.approvalOrderId())
    ));
}
```

```java
@Test
void publishFromReleasedContractsShouldPublishCatalogEvent() {
    service.publishFromReleasedContracts(1001L, 7001L, List.of(releasedValue()), Set.of("value"));

    verify(applicationEventPublisher).publishEvent(argThat((RiskMetricCatalogPublishedEvent event) ->
            Long.valueOf(1001L).equals(event.productId())
                    && Long.valueOf(7001L).equals(event.releaseBatchId())
    ));
}
```

```java
@Test
void raisedOpsAlertEventShouldCreatePersistentOpenAlert() {
    listener.onRaised(new GovernanceOpsAlertRaisedEvent(
            1L, "FIELD_DRIFT", "product:1001:value", "PRODUCT", 1001L, 1001L,
            null, null, null, null, "WARN", 2L,
            "字段漂移告警", "value 已偏离正式合同", "product:1001:value",
            "产品1001/value", "PAYLOAD_APPLY", "{}", 10001L
    ));

    verify(governanceOpsAlertService).raiseOrRefresh(any());
}
```

- [ ] **Step 2: Run the producer/listener test slice to verify the event layer is absent**

Run:

```bash
mvn -pl spring-boot-iot-common,spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=ProductModelServiceImplTest,RiskMetricCatalogServiceImplTest,RiskGovernanceOpsServiceImplTest,GovernanceWorkItemEventListenerTest,GovernanceOpsAlertEventListenerTest test
```

Expected:

- `BUILD FAILURE`
- compile failures for missing event types, listeners, or publisher wiring

- [ ] **Step 3: Implement the event flow and control-plane sync rules**

```java
public record ProductContractReleasedEvent(
        Long tenantId,
        Long productId,
        Long releaseBatchId,
        String scenarioCode,
        List<String> releasedIdentifiers,
        Long operatorUserId,
        Long approvalOrderId
) {
}
```

```java
applicationEventPublisher.publishEvent(new ProductContractReleasedEvent(
        1L,
        product.getId(),
        batchId,
        scenarioCode,
        afterSnapshot.stream().map(ReleaseModelSnapshotItem::identifier).toList(),
        operatorId,
        approvalOrderId
));
```

```java
@EventListener
public void onCatalogPublished(RiskMetricCatalogPublishedEvent event) {
    governanceWorkItemService.resolve("PENDING_CONTRACT_RELEASE", "PRODUCT", event.productId(), SYSTEM_OPERATOR_ID, "目录已发布");
    governanceWorkItemService.openOrRefresh(new GovernanceWorkItemCommand(
            "PENDING_RISK_BINDING",
            "PRODUCT",
            event.productId(),
            event.productId(),
            null,
            event.releaseBatchId(),
            null,
            null,
            "RISK_BINDING",
            "目录已发布，待完成风险点绑定",
            "{\"publishedRiskMetricIds\":1}",
            "P1",
            SYSTEM_OPERATOR_ID
    ));
}
```

```java
@EventListener
public void onRaised(GovernanceOpsAlertRaisedEvent event) {
    governanceOpsAlertService.raiseOrRefresh(new GovernanceOpsAlertCommand(
            event.alertType(),
            event.alertCode(),
            event.subjectType(),
            event.subjectId(),
            event.productId(),
            event.riskMetricId(),
            event.releaseBatchId(),
            event.traceId(),
            event.deviceCode(),
            event.productKey(),
            event.severityLevel(),
            event.affectedCount(),
            event.alertTitle(),
            event.alertMessage(),
            event.dimensionKey(),
            event.dimensionLabel(),
            event.sourceStage(),
            event.snapshotJson(),
            event.operatorUserId()
    ));
}
```

- [ ] **Step 4: Re-run the producer/listener test slice**

Run:

```bash
mvn -pl spring-boot-iot-common,spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=ProductModelServiceImplTest,RiskMetricCatalogServiceImplTest,RiskGovernanceOpsServiceImplTest,GovernanceWorkItemEventListenerTest,GovernanceOpsAlertEventListenerTest test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit the event-driven sync slice**

```bash
git add spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/listener spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImpl.java spring-boot-iot-system/src/test/java spring-boot-iot-device/src/test/java spring-boot-iot-alarm/src/test/java
git commit -m "feat: sync governance control plane from bridge events"
```

## Task 4: Add frontend workbenches, docs, and verification

**Files:**
- Create: `spring-boot-iot-ui/src/api/governanceWorkItem.ts`
- Create: `spring-boot-iot-ui/src/api/governanceOpsAlert.ts`
- Create: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Create: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/views/CockpitView.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Write the failing frontend tests**

```ts
it('builds the governance work item page query string', async () => {
  const requestSpy = vi.spyOn(apiModule, 'request').mockResolvedValue({ code: 200, data: null, msg: 'ok' } as never)

  await pageGovernanceWorkItems({ workStatus: 'OPEN', workItemCode: 'PENDING_CONTRACT_RELEASE', pageNum: 1, pageSize: 20 })

  expect(requestSpy).toHaveBeenCalledWith(
    '/api/governance/work-items?workStatus=OPEN&workItemCode=PENDING_CONTRACT_RELEASE&pageNum=1&pageSize=20',
    { method: 'GET' }
  )
})
```

```ts
it('renders governance task rows from backend work items', async () => {
  governanceWorkItemApi.pageWorkItems = vi.fn().mockResolvedValue({
    code: 200,
    data: { total: 1, records: [{ id: 1, workItemCode: 'PENDING_CONTRACT_RELEASE', workStatus: 'OPEN', blockingReason: '合同尚未发布' }] }
  })

  const wrapper = mount(GovernanceTaskView)
  await flushPromises()

  expect(wrapper.text()).toContain('待发布合同')
  expect(wrapper.text()).toContain('合同尚未发布')
})
```

```ts
it('builds the governance ops alert page query string', async () => {
  const requestSpy = vi.spyOn(apiModule, 'request').mockResolvedValue({ code: 200, data: null, msg: 'ok' } as never)

  await pageGovernanceOpsAlerts({ alertStatus: 'OPEN', alertType: 'FIELD_DRIFT', pageNum: 1, pageSize: 20 })

  expect(requestSpy).toHaveBeenCalledWith(
    '/api/governance/ops-alerts?alertStatus=OPEN&alertType=FIELD_DRIFT&pageNum=1&pageSize=20',
    { method: 'GET' }
  )
})
```

- [ ] **Step 2: Run the focused frontend tests to verify the new APIs/views do not exist yet**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/api/governanceWorkItem.test.ts src/__tests__/api/governanceOpsAlert.test.ts src/__tests__/views/GovernanceTaskView.test.ts src/__tests__/views/GovernanceOpsWorkbenchView.test.ts
```

Expected: `FAIL`

- [ ] **Step 3: Implement the minimal frontend control-plane slice**

```ts
export function pageGovernanceWorkItems(params: GovernanceWorkItemPageQuery = {}) {
  const queryString = buildQueryString(params)
  const path = queryString ? `/api/governance/work-items?${queryString}` : '/api/governance/work-items'
  return request<PageResult<GovernanceWorkItemItem>>(path, { method: 'GET' })
}
```

```ts
export function pageGovernanceOpsAlerts(params: GovernanceOpsAlertPageQuery = {}) {
  const queryString = buildQueryString(params)
  const path = queryString ? `/api/governance/ops-alerts?${queryString}` : '/api/governance/ops-alerts'
  return request<PageResult<GovernanceOpsAlertItem>>(path, { method: 'GET' })
}
```

```ts
{
  path: '/governance-tasks',
  name: 'governance-tasks',
  component: () => import('../views/GovernanceTaskView.vue'),
  meta: routeMeta('/governance-tasks')
}
```

```vue
<StandardWorkbenchPanel
  title="治理任务台"
  description="统一查看待治理产品、待发布合同、待绑定风险点、待补策略和待复盘事项。"
  show-toolbar
  show-pagination
/>
```

```vue
<StandardWorkbenchPanel
  title="治理运维台"
  description="统一查看字段漂移、合同差异和风险指标缺失等治理运维告警。"
  show-toolbar
  show-pagination
/>
```

- [ ] **Step 4: Update docs and run verification**

Run:

```bash
git grep -n "治理工作项\|运维告警\|/api/governance/work-items\|/api/governance/ops-alerts" -- docs README.md AGENTS.md
python -m unittest scripts.tests.test_run_real_env_schema_sync
mvn -pl spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=GovernanceWorkItemServiceImplTest,GovernanceOpsAlertServiceImplTest,ProductModelServiceImplTest,RiskMetricCatalogServiceImplTest,RiskGovernanceOpsServiceImplTest test
cd spring-boot-iot-ui && npm test -- src/__tests__/api/governanceWorkItem.test.ts src/__tests__/api/governanceOpsAlert.test.ts src/__tests__/views/GovernanceTaskView.test.ts src/__tests__/views/GovernanceOpsWorkbenchView.test.ts
cd ..
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:

- docs mention `iot_governance_work_item` and `iot_governance_ops_alert`
- schema-sync tests `OK`
- focused backend tests `BUILD SUCCESS`
- focused frontend tests `PASS`
- final package build `BUILD SUCCESS`

- [ ] **Step 5: Commit the frontend/docs slice**

```bash
git add spring-boot-iot-ui docs README.md AGENTS.md
git commit -m "feat: expose governance control plane workbenches"
```

## Self-Review

- Spec coverage: this plan implements track 3 only and explicitly depends on the existing 2026-04-08 bridge foundation and explicit binding plans.
- Placeholder scan: no `TODO`/`TBD` placeholders remain; each task includes concrete files, code, and commands.
- Type consistency: this plan consistently uses `iot_governance_work_item`, `iot_governance_ops_alert`, `ProductContractReleasedEvent`, `RiskMetricCatalogPublishedEvent`, `GovernanceWorkItemService`, and `GovernanceOpsAlertService` across later tasks.
