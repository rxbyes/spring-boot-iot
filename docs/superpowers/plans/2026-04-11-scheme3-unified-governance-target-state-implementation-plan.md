# Scheme 3 Unified Governance Target-State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining gaps between today's control-plane dispatch slice and the long-term “统一控制面集中调度” target state by unifying governance task, approval, recommendation, execution, and replay into one auditable lifecycle.

**Architecture:** Keep `spring-boot-iot-system` as the control-plane orchestration layer, but keep domain truth writes inside `spring-boot-iot-device`, `spring-boot-iot-alarm`, and `spring-boot-iot-rule`. Phase `P0` upgrades `GovernanceWorkItem` from backlog record to lifecycle hub and bridges it with approval/execution. Phase `P1` adds a shared recommendation/evidence/impact contract and a priority engine. Phase `P2` adds dry-run, auto-draft, and replay feedback loops on top of the same contracts.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Element Plus, Vitest, Maven, existing governance control-plane modules

---

### Task 1: P0.1 Upgrade the governance work item into the lifecycle hub

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceWorkItem.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceWorkItemVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceWorkItemCommand.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceWorkItemController.java`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/governanceWorkItem.ts`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImplTest.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernanceWorkItemControllerTest.java`

- [ ] **Step 1: Write failing backend tests for the expanded task contract**

Add assertions that `GovernanceWorkItemServiceImpl` persists and returns the new lifecycle fields:

```java
assertThat(saved.getTaskCategory()).isEqualTo("RISK_BINDING");
assertThat(saved.getDomainCode()).isEqualTo("ALARM");
assertThat(saved.getActionCode()).isEqualTo("RISK_POINT_PENDING_PROMOTION");
assertThat(saved.getExecutionStatus()).isEqualTo("PENDING_APPROVAL");
assertThat(saved.getRecommendationSnapshotJson()).contains("confidence");
assertThat(saved.getEvidenceSnapshotJson()).contains("evidenceItems");
assertThat(saved.getImpactSnapshotJson()).contains("affectedRiskPointCount");
assertThat(saved.getRollbackSnapshotJson()).contains("rollbackable");
```

Run: `mvn -pl spring-boot-iot-system -Dtest=GovernanceWorkItemServiceImplTest,GovernanceWorkItemControllerTest test`
Expected: FAIL because the new fields do not exist yet.

- [ ] **Step 2: Extend the work-item table and Java model**

Add the following columns to `iot_governance_work_item` in `sql/init.sql`:

```sql
ALTER TABLE iot_governance_work_item
  ADD COLUMN task_category        varchar(64)  NULL COMMENT '治理任务分类',
  ADD COLUMN domain_code          varchar(64)  NULL COMMENT '领域编码',
  ADD COLUMN action_code          varchar(128) NULL COMMENT '治理动作编码',
  ADD COLUMN execution_status     varchar(64)  NULL COMMENT '执行状态',
  ADD COLUMN recommendation_snapshot_json longtext NULL COMMENT '推荐快照',
  ADD COLUMN evidence_snapshot_json       longtext NULL COMMENT '证据快照',
  ADD COLUMN impact_snapshot_json         longtext NULL COMMENT '影响分析快照',
  ADD COLUMN rollback_snapshot_json       longtext NULL COMMENT '回滚快照';
```

Mirror the same fields in `GovernanceWorkItem`, `GovernanceWorkItemVO`, `GovernanceWorkItemCommand`, and controller/API serialization.

- [ ] **Step 3: Keep backward compatibility for existing six task codes**

In `GovernanceWorkItemServiceImpl`, keep current `workItemCode` semantics intact and backfill the new hub fields by rule:

```java
taskCategory = switch (workItemCode) {
    case "PENDING_CONTRACT_RELEASE" -> "CONTRACT_RELEASE";
    case "PENDING_RISK_BINDING" -> "RISK_BINDING";
    case "PENDING_THRESHOLD_POLICY" -> "THRESHOLD_POLICY";
    case "PENDING_LINKAGE_PLAN" -> "LINKAGE_PLAN";
    case "PENDING_REPLAY" -> "REPLAY";
    default -> "PRODUCT_GOVERNANCE";
};
```

Do not remove or rename existing `workItemCode`, `workStatus`, or `snapshotJson`; treat them as compatibility fields during the transition.

- [ ] **Step 4: Re-run the targeted backend tests**

Run: `mvn -pl spring-boot-iot-system -Dtest=GovernanceWorkItemServiceImplTest,GovernanceWorkItemControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit the P0.1 hub-contract slice**

```bash
git add sql/init.sql sql/init-data.sql spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceWorkItem.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceWorkItemVO.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceWorkItemCommand.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceWorkItemController.java spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/api/governanceWorkItem.ts spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImplTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/GovernanceWorkItemControllerTest.java
git commit -m "feat: expand governance work item lifecycle hub"
```

### Task 2: P0.2 Bridge approval orders into the same task lifecycle

**Files:**
- Modify: `sql/init.sql`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceApprovalOrder.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceApprovalOrderVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceApprovalActionExecutionResult.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/GovernanceApprovalView.vue`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImplTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Write failing tests for work-item-linked approvals**

Add assertions that:

```java
assertThat(order.getWorkItemId()).isEqualTo(workItem.getId());
assertThat(updatedWorkItem.getApprovalOrderId()).isEqualTo(order.getId());
assertThat(updatedWorkItem.getExecutionStatus()).isEqualTo("APPROVED");
```

Run: `mvn -pl spring-boot-iot-system -Dtest=GovernanceApprovalServiceImplTest test`
Expected: FAIL because approval orders are not yet linked back to one lifecycle hub.

- [ ] **Step 2: Add `work_item_id` to approval orders and round-trip it**

Add:

```sql
ALTER TABLE sys_governance_approval_order
  ADD COLUMN work_item_id bigint NULL COMMENT '关联治理任务 ID';
```

Mirror `workItemId` in `GovernanceApprovalOrder`, `GovernanceApprovalOrderVO`, and `api.ts`.

- [ ] **Step 3: Update approval lifecycle to drive work-item execution states**

In `GovernanceApprovalServiceImpl`, update the linked work item on every approval transition:

```java
PENDING   -> executionStatus = "PENDING_APPROVAL"
APPROVED  -> executionStatus = "EXECUTED"
REJECTED  -> executionStatus = "REJECTED"
CANCELLED -> executionStatus = "CANCELLED"
```

Also persist executor output in both places:
- `payloadJson.execution.result` remains the approval-truth payload
- `GovernanceWorkItem.impactSnapshotJson` / `rollbackSnapshotJson` get a normalized summary for task list and replay

- [ ] **Step 4: Extend approval action permissions for future alarm/rule actions**

Reserve explicit permission constants now so new approval executors do not hardcode strings later:

```java
RISK_POINT_BIND_APPROVE
RISK_POINT_BIND_EXECUTE
RISK_POINT_PENDING_PROMOTION_APPROVE
RISK_POINT_PENDING_PROMOTION_EXECUTE
RULE_DEFINITION_APPROVE
LINKAGE_PLAN_APPROVE
```

Do not add menu exposure yet; only add constants and policy wiring points.

- [ ] **Step 5: Re-run approval and control-plane tests**

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceApprovalServiceImplTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: PASS

- [ ] **Step 6: Commit the approval-bridge slice**

```bash
git add sql/init.sql spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceApprovalOrder.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceApprovalOrderVO.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceApprovalActionExecutionResult.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/views/GovernanceApprovalView.vue spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImplTest.java spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts
git commit -m "feat: bridge governance approvals to work item lifecycle"
```

### Task 3: P0.3 Put risk binding and pending promotion behind approval-aware domain commands

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingPromotionService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceApprovalPolicyResolver.java`
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Write failing tests for approval-aware risk binding writes**

Cover both modes:

```java
assertThat(result.getApprovalOrderId()).isNotNull();
assertThat(result.getExecutionStatus()).isEqualTo("PENDING_APPROVAL");
assertThat(riskPointDeviceMapper.selectCount(query)).isZero();
```

and

```java
assertThat(result.getExecutionStatus()).isEqualTo("DIRECT_APPLIED");
assertThat(riskPointDeviceMapper.selectCount(query)).isEqualTo(1L);
```

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointControllerTest test`
Expected: FAIL because current writes are direct-only.

- [ ] **Step 2: Introduce a shared submission result contract for domain writes**

Add a response contract used by risk binding and future rule/linkage slices:

```ts
export interface GovernanceSubmissionResult {
  workItemId?: IdType | null;
  approvalOrderId?: IdType | null;
  approvalStatus?: GovernanceApprovalStatus | null;
  executionStatus?: 'DIRECT_APPLIED' | 'PENDING_APPROVAL' | 'REJECTED' | 'CANCELLED' | null;
}
```

Return it from:
- `POST /api/risk-point/bind-device`
- `POST /api/risk-point/unbind-device`
- `POST /api/risk-point/pending-bindings/{pendingId}/promote`

- [ ] **Step 3: Add alarm-domain approval executor instead of moving writes into system**

Create `RiskPointGovernanceApprovalExecutor` with action codes:

```java
RISK_POINT_BIND_DEVICE
RISK_POINT_UNBIND_DEVICE
RISK_POINT_PENDING_PROMOTION
```

Executor rules:
- read request payload from approval order
- call `RiskPointBindingMaintenanceService` / `RiskPointPendingPromotionService`
- write only alarm-domain truth tables
- return normalized execution summary for the work item and approval order

- [ ] **Step 4: Make controller flows policy-aware**

Resolve policy before executing the domain mutation:
- if policy says `direct`, execute service immediately and open/resolve the linked work item
- if policy says `approval`, create approval order + linked work item and return `PENDING_APPROVAL`

Do not fork the UI into two screens. Keep `/risk-point` as one workbench and render approval state inline beside the mutation result.

- [ ] **Step 5: Re-run backend and UI tests**

Run:
- `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointControllerTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts`

Expected: PASS

- [ ] **Step 6: Commit the risk-binding approval slice**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingPromotionService.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernanceApprovalPolicyResolver.java spring-boot-iot-ui/src/api/riskPoint.ts spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/views/RiskPointView.vue spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: add approval-aware risk binding governance flow"
```

### Task 4: P1.1 Add one shared recommendation, evidence, impact, and rollback contract

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceRecommendationSnapshot.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceEvidenceItem.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceImpactSnapshot.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceRollbackSnapshot.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceOpsAlert.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceOpsAlertVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImplTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Write failing tests for the unified recommendation contract**

Add assertions that a task or alert can expose one normalized structure:

```ts
expect(item.recommendation?.confidence).toBe(0.92)
expect(item.recommendation?.evidenceItems?.[0]?.evidenceType).toBe('RUNTIME_PAYLOAD')
expect(item.impact?.rollbackable).toBe(true)
```

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceOpsAlertServiceImplTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: FAIL

- [ ] **Step 2: Define one cross-layer contract and stop duplicating ad-hoc evidence shapes**

Use this exact shape in Java and TypeScript:

```ts
interface GovernanceRecommendationSnapshot {
  recommendationType: 'PROMOTE' | 'PUBLISH' | 'CREATE_POLICY' | 'REPLAY' | 'IGNORE';
  confidence: number;
  reasonCodes: string[];
  suggestedAction: string;
  evidenceItems: GovernanceEvidenceItem[];
}
```

```ts
interface GovernanceImpactSnapshot {
  affectedCount: number;
  affectedTypes: string[];
  rollbackable: boolean;
  rollbackPlanSummary?: string | null;
}
```

- [ ] **Step 3: Render the new contract in the two control-plane pages**

`GovernanceTaskView.vue` and `GovernanceOpsWorkbenchView.vue` should render:
- confidence tag
- top 3 evidence items
- impact summary
- rollbackability tag

Do not add action buttons that bypass approval. This phase is read-side only.

- [ ] **Step 4: Re-run tests**

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceOpsAlertServiceImplTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: PASS

- [ ] **Step 5: Commit the recommendation-contract slice**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceRecommendationSnapshot.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceEvidenceItem.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceImpactSnapshot.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceRollbackSnapshot.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceOpsAlert.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceOpsAlertVO.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/views/GovernanceTaskView.vue spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceOpsAlertServiceImplTest.java spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts
git commit -m "feat: unify governance recommendation and evidence contract"
```

### Task 5: P1.2 Add a deterministic priority engine and governance decision context

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernancePriorityScorer.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePriorityScorerImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceDecisionContextVO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceWorkItemController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceWorkItemContributor.java`
- Modify: `spring-boot-iot-ui/src/api/governanceWorkItem.ts`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImplTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Write failing tests for deterministic priority and context explanations**

Add assertions that a work item exposes:

```java
assertThat(item.getPriorityLevel()).isEqualTo("P1");
assertThat(context.getReasonCodes()).contains("LOW_BINDING_COVERAGE", "HIGH_IMPACT_RELEASE");
assertThat(context.getAffectedModules()).contains("PRODUCT", "RISK_POINT", "RULE");
```

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceWorkItemServiceImplTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: FAIL

- [ ] **Step 2: Implement a rule-first priority scorer**

Use one deterministic formula before any AI ranking:

```java
priorityScore =
    releaseImpactWeight * affectedCount
  + agingWeight * overdueHours
  + blockingWeight * blockingCount
  + replayWeight * failureSignalCount;
```

Persist both the final `priorityLevel` and the raw `reasonCodes`.

- [ ] **Step 3: Add `GET /api/governance/work-items/{id}/decision-context`**

Return one page-friendly context object that answers:
- what is wrong
- why this item is ranked here
- what downstream objects are affected
- what the recommended next action is

Keep it read-only. No write action belongs in this endpoint.

- [ ] **Step 4: Re-run tests**

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceWorkItemServiceImplTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: PASS

- [ ] **Step 5: Commit the priority-engine slice**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/GovernancePriorityScorer.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernancePriorityScorerImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/GovernanceDecisionContextVO.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceWorkItemController.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceWorkItemContributor.java spring-boot-iot-ui/src/api/governanceWorkItem.ts spring-boot-iot-ui/src/views/GovernanceTaskView.vue spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImplTest.java spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts
git commit -m "feat: add governance priority engine and decision context"
```

### Task 6: P2.1 Add dry-run and auto-draft for low-risk governance actions

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceSimulationResult.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceSimulationController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalExecutor.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java`
- Modify: `spring-boot-iot-ui/src/api/governanceApproval.ts`
- Modify: `spring-boot-iot-ui/src/views/GovernanceApprovalView.vue`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalExecutorTest.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImplTest.java`

- [ ] **Step 1: Write failing tests for dry-run summaries**

Add assertions that a simulation returns:

```java
assertThat(result.executable()).isTrue();
assertThat(result.rollbackable()).isTrue();
assertThat(result.affectedTypes()).contains("RISK_METRIC", "RISK_POINT", "RULE");
```

Run:
- `mvn -pl spring-boot-iot-system,spring-boot-iot-device -Dtest=GovernanceApprovalServiceImplTest,ProductContractGovernanceApprovalExecutorTest test`

Expected: FAIL

- [ ] **Step 2: Add a simulation endpoint before execution**

Expose:

```text
POST /api/system/governance-simulation/approval/{orderId}
```

The endpoint should:
- resolve the same domain executor as approve
- run a side-effect-free simulation path
- return impact + rollback summary

- [ ] **Step 3: Only auto-draft, never auto-execute, in this phase**

When confidence is high and policy allows it, create a draft approval suggestion:
- work item stays human-owned
- approval order stays `PENDING`
- no domain truth write happens until explicit approve

Do not add unattended execution in this phase.

- [ ] **Step 4: Re-run tests**

Run:
- `mvn -pl spring-boot-iot-system,spring-boot-iot-device -Dtest=GovernanceApprovalServiceImplTest,ProductContractGovernanceApprovalExecutorTest test`

Expected: PASS

- [ ] **Step 5: Commit the dry-run slice**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/GovernanceSimulationResult.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/GovernanceSimulationController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalExecutor.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java spring-boot-iot-ui/src/api/governanceApproval.ts spring-boot-iot-ui/src/views/GovernanceApprovalView.vue spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalExecutorTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/GovernanceApprovalServiceImplTest.java
git commit -m "feat: add governance dry-run and auto-draft support"
```

### Task 7: P2.2 Feed replay outcomes back into recommendation quality

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceReplayFeedback.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceReplayFeedbackMapper.java`
- Modify: `sql/init.sql`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/listener/GovernanceWorkItemEventListenerTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Write failing tests for replay feedback persistence**

Add assertions that replay closeout writes structured feedback:

```java
assertThat(feedback.getWorkItemId()).isEqualTo(workItem.getId());
assertThat(feedback.getAdoptedDecision()).isEqualTo("PROMOTE");
assertThat(feedback.getExecutionOutcome()).isEqualTo("SUCCESS");
assertThat(feedback.getRootCauseCode()).isEqualTo("MISSING_POLICY");
```

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceWorkItemEventListenerTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: FAIL

- [ ] **Step 2: Add replay feedback storage**

Create table:

```sql
CREATE TABLE sys_governance_replay_feedback (
  id bigint NOT NULL,
  work_item_id bigint NOT NULL,
  approval_order_id bigint NULL,
  release_batch_id bigint NULL,
  adopted_decision varchar(64) NULL,
  execution_outcome varchar(64) NULL,
  root_cause_code varchar(64) NULL,
  feedback_json longtext NULL,
  create_time datetime NULL,
  PRIMARY KEY (id)
);
```

- [ ] **Step 3: Update replay drawers to capture closeout**

When a replay concludes, capture:
- adopted recommendation vs overridden recommendation
- final execution result
- root cause code
- operator summary

Write feedback through one explicit closeout action. Do not infer it from drawer close.

- [ ] **Step 4: Re-run tests**

Run:
- `mvn -pl spring-boot-iot-system -Dtest=GovernanceWorkItemEventListenerTest test`
- `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts`

Expected: PASS

- [ ] **Step 5: Commit the replay-feedback slice**

```bash
git add sql/init.sql spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/GovernanceReplayFeedback.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/GovernanceReplayFeedbackMapper.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/GovernanceWorkItemServiceImpl.java spring-boot-iot-ui/src/views/GovernanceTaskView.vue spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue spring-boot-iot-ui/src/api/riskGovernance.ts spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/listener/GovernanceWorkItemEventListenerTest.java spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts
git commit -m "feat: persist governance replay feedback loop"
```

### Task 8: Update docs and run full verification gates

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update architecture and flow docs**

Document the final control-plane rule explicitly:
- control plane owns dispatch, approval orchestration, audit, replay, recommendation
- domain workbenches still own truth writes
- every governance write is either direct-under-policy or approval-backed under one work-item lifecycle

- [ ] **Step 2: Update API and schema docs**

Document:
- new work-item fields
- new approval `workItemId`
- approval-aware risk binding response contract
- simulation endpoint
- replay feedback table

- [ ] **Step 3: Review repo-level docs**

Check `README.md` and `AGENTS.md`. Only change them if repo-level behavior or operator rules changed; otherwise record “reviewed, no change needed” in the implementation summary.

- [ ] **Step 4: Run focused backend tests**

Run:

```bash
mvn -pl spring-boot-iot-system,spring-boot-iot-alarm,spring-boot-iot-device -Dtest=GovernanceWorkItemServiceImplTest,GovernanceApprovalServiceImplTest,GovernanceOpsAlertServiceImplTest,GovernanceWorkItemEventListenerTest,RiskPointBindingMaintenanceServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointControllerTest,ProductContractGovernanceApprovalExecutorTest test
```

Expected: PASS, except for the known Mockito inline agent issue already documented in `AGENTS.md`.

- [ ] **Step 5: Run focused UI tests**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts src/__tests__/views/RiskPointView.test.ts
```

Expected: PASS

- [ ] **Step 6: Run build verification**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
git diff --check
```

Expected:
- Vite build passes
- Maven package passes
- `git diff --check` reports no conflict markers or trailing whitespace errors

- [ ] **Step 7: Commit the closeout docs and verification slice**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: record unified governance target-state closeout"
```
