# 产品物模型设计器二期治理工作流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `/products -> models` 工作区内，把产品契约治理流程收束成一条可操作、可审批、可追溯、可回滚、可验收的主流程，并固定采集器 / 子设备治理边界。

**Architecture:** 前端继续复用 `ProductModelDesignerWorkspace` 作为唯一治理工作区，不新增路由，通过“阶段总览 + 主流程区 + 历史辅助区”重组现有 compare、审批、批次和正式字段能力。后端不新增平行治理模型，继续复用现有 compare/apply/release-batch/rollback 链路，仅补足“审批提交回执”和“正式发布结果”的语义字段与测试，最后把正式业务、接口和交付边界文档同步到同一口径。

**Tech Stack:** Vue 3, TypeScript, Vitest, Spring Boot 4, Java 17, MyBatis-Plus, Maven

---

## 文件结构

- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  - 重组工作区布局，增加治理阶段总览，明确主流程区和历史辅助区。
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
  - 强化 compare 行的“治理候选快照”表达，补足规范字段、风险就绪和决策提示。
- Modify: `spring-boot-iot-ui/src/types/api.ts`
  - 对齐 apply 回执和 compare 结果的前端类型语义。
- Modify: `spring-boot-iot-ui/src/api/product.ts`
  - 保持 API 路径不变，只在注释和返回类型口径上同步语义。
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
  - 增加阶段总览、主流程区 / 历史区、审批提交回执文案的前端测试。
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
  - 增加 compare 行治理候选快照与规范 / 风险提示测试。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
  - 增加提交层计数字段，显式区分“审批提交回执”和“正式发布结果”。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalPayloads.java`
  - 在 pending apply 回执中写入提交层数量字段。
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
  - 固化 apply 回执“已提交审批而非已发布”的返回语义。
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java`
  - 固化 rollback 回执“待审批执行”的返回语义。
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalPayloadsTest.java`
  - 单测 pending apply payload 和 execution payload 的提交层 / 执行层语义。
- Modify: `docs/02-业务功能与流程说明.md`
  - 说明产品物模型设计器二期的治理工作流。
- Modify: `docs/03-接口规范与接口清单.md`
  - 说明 compare/apply/release-batch/rollback 的稳定语义。
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
  - 把“产品物模型设计器二期”登记为设备中心当前冻结增强主线的实施状态。
- Modify: `docs/21-业务功能清单与验收标准.md`
  - 补齐二期验收问题清单和最小回归口径。

## Task 1: 重组前端工作区为治理主流程

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: 先写失败的前端测试，锁定阶段总览和主流程 / 历史分区**

```ts
it('renders workflow stage rail and separates live governance flow from history panes', async () => {
  const wrapper = mountWorkspace()

  expect(wrapper.get('[data-testid="contract-governance-workflow"]').text()).toContain('待输入样本')
  expect(wrapper.get('[data-testid="contract-governance-workflow"]').text()).toContain('待确认识别结果')
  expect(wrapper.get('[data-testid="contract-governance-workflow"]').text()).toContain('待提交审批')
  expect(wrapper.get('[data-testid="contract-governance-workflow"]').text()).toContain('审批中')
  expect(wrapper.get('[data-testid="contract-governance-workflow"]').text()).toContain('已发布 / 可回滚')
  expect(wrapper.get('[data-testid="contract-governance-primary"]').text()).toContain('样本输入')
  expect(wrapper.get('[data-testid="contract-governance-primary"]').text()).toContain('识别结果')
  expect(wrapper.get('[data-testid="contract-governance-history"]').text()).toContain('发布批次与风险联动')
  expect(wrapper.get('[data-testid="contract-governance-history"]').text()).toContain('当前已生效字段')
})
```

- [ ] **Step 2: 运行前端定向测试，确认当前结构还没有二期工作流壳层**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

```text
FAIL src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
+ Unable to find an element by: [data-testid="contract-governance-workflow"]
```

- [ ] **Step 3: 在工作区组件中实现治理阶段总览和主流程 / 历史分区**

```ts
type GovernanceWorkflowStage = 'pending_sample' | 'review_compare' | 'ready_submit' | 'approval_pending' | 'released'

const workflowStage = computed<GovernanceWorkflowStage>(() => {
  if (applyReceiptStatus.value === 'PENDING' || rollbackReceiptStatus.value === 'PENDING') return 'approval_pending'
  if (applyExecutionCompleted.value || rollbackExecutionCompleted.value || latestReleaseBatchId.value) return 'released'
  if (selectedApplyItems.value.length) return 'ready_submit'
  if (compareRows.value.length) return 'review_compare'
  return 'pending_sample'
})

const workflowStageItems = computed(() => [
  { key: 'pending_sample', label: '待输入样本' },
  { key: 'review_compare', label: '待确认识别结果' },
  { key: 'ready_submit', label: '待提交审批' },
  { key: 'approval_pending', label: '审批中' },
  { key: 'released', label: '已发布 / 可回滚' }
])
```

```vue
<section class="product-model-designer__workflow" data-testid="contract-governance-workflow">
  <article
    v-for="stage in workflowStageItems"
    :key="stage.key"
    class="product-model-designer__workflow-stage"
    :class="{ 'is-active': workflowStage === stage.key }"
  >
    <strong>{{ stage.label }}</strong>
  </article>
</section>

<div class="product-model-designer__workspace-grid">
  <div class="product-model-designer__workspace-primary" data-testid="contract-governance-primary">
    <!-- 样本输入 / 识别结果 / 本次生效 / 审批反馈 -->
  </div>
  <aside class="product-model-designer__workspace-history" data-testid="contract-governance-history">
    <!-- 发布批次与风险联动 / 当前已生效字段 -->
  </aside>
</div>
```

- [ ] **Step 4: 重跑前端定向测试，确认二期工作流壳层通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

```text
PASS src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

- [ ] **Step 5: 提交工作区主流程重构**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "feat: stage product governance workflow"
```

## Task 2: 强化 compare 行与审批反馈的治理语义

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: 先写失败的前端测试，锁定“治理候选快照”和“审批提交回执”文案**

```ts
it('renders governance snapshot cues and approval submission wording', async () => {
  const wrapper = mountWorkspace()

  await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
  await flushPromises()

  expect(wrapper.text()).toContain('治理候选快照')
  expect(wrapper.text()).toContain('规范字段')
  expect(wrapper.text()).toContain('可进入风险闭环')
  expect(wrapper.text()).toContain('审批提交回执')
})
```

```ts
it('renders governance snapshot labels instead of generic compare wording', () => {
  const wrapper = mountTable()

  expect(wrapper.text()).toContain('治理候选快照')
  expect(wrapper.text()).toContain('规范字段：设备重启')
  expect(wrapper.text()).toContain('可进入风险闭环')
  expect(wrapper.text()).toContain('当前建议')
})
```

- [ ] **Step 2: 运行 compare table 与 workspace 定向测试，确认当前文案和状态还未收口**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

```text
FAIL ... ProductModelGovernanceCompareTable.test.ts
Expected text to contain: 治理候选快照
```

- [ ] **Step 3: 在 compare table 与 workspace 中实现治理语义表达**

```vue
<span class="product-model-governance-compare-table__row-kicker">治理候选快照</span>
<div class="product-model-governance-compare-table__row-meta">
  <span>identifier: {{ row.identifier }}</span>
  <span>类型: {{ rowTypeLabel(row) }}</span>
  <span v-if="rowNormativeLabel(row)">规范字段：{{ rowNormativeLabel(row) }}</span>
</div>
<p class="product-model-governance-compare-table__row-reason">
  当前建议：{{ decisionSummary(row) }}
</p>
```

```ts
const applyReceiptTitle = computed(() => (
  applyExecutionCompleted.value ? '正式发布结果' : '审批提交回执'
))

const applySubmittedCount = computed(() =>
  applyExecutedResult.value?.submittedItemCount
  ?? applyResult.value?.submittedItemCount
  ?? (applyReceiptCounts.value.created + applyReceiptCounts.value.updated + applyReceiptCounts.value.skipped)
)
```

```vue
<section v-if="applyResult" class="product-model-designer__approval-stage" data-testid="contract-field-apply-approval-status">
  <div class="product-model-designer__approval-head">
    <div>
      <strong>{{ applyReceiptTitle }}</strong>
      <p>{{ applyApprovalSummaryText }}</p>
    </div>
  </div>
  <div class="product-model-designer__receipt">
    <article class="product-model-designer__summary-card">
      <span>本次提交</span>
      <strong>{{ applySubmittedCount }}</strong>
    </article>
  </div>
</section>
```

- [ ] **Step 4: 重跑前端定向测试，确认治理语义渲染通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

```text
PASS src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts
PASS src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

- [ ] **Step 5: 提交 compare / apply 前端语义收口**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/api/product.ts spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "feat: refine product governance semantics"
```

## Task 3: 固化后端审批提交回执与执行结果语义

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalPayloads.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalPayloadsTest.java`
- Modify: `spring-boot-iot-ui/src/types/api.ts`

- [ ] **Step 1: 先写失败的后端测试，锁定 pending apply 的提交层计数**

```java
@Test
void buildPendingApplyResultShouldExposeSubmissionLayerCounts() {
    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    dto.setItems(List.of(item("create", "value"), item("update", "sensor_state"), item("skip", "temp")));

    ProductModelGovernanceApplyResultVO result =
            ProductContractGovernanceApprovalPayloads.buildPendingApplyResult(88001L, dto);

    assertEquals(3, result.getSubmittedItemCount());
    assertEquals(88001L, result.getApprovalOrderId());
    assertEquals("PENDING", result.getApprovalStatus());
    assertNull(result.getReleaseBatchId());
    assertEquals(Boolean.TRUE, result.getExecutionPending());
}
```

```java
@Test
void applyGovernanceShouldReturnPendingSubmissionCountsBeforeReleaseBatchExists() {
    R<ProductModelGovernanceApplyResultVO> response = controller.applyGovernance(1001L, dto, 2002L, authentication);

    assertEquals(1, response.getData().getSubmittedItemCount());
    assertNull(response.getData().getReleaseBatchId());
    assertEquals("PENDING", response.getData().getApprovalStatus());
}
```

- [ ] **Step 2: 运行后端定向测试，确认当前回执还没有提交层数量字段**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest,ProductContractGovernanceApprovalPayloadsTest test
```

Expected:

```text
FAIL ... cannot find symbol: method getSubmittedItemCount()
```

- [ ] **Step 3: 在 VO 和 payload helper 中增加 submittedItemCount，并回填前端类型**

```java
@Data
public class ProductModelGovernanceApplyResultVO {

    private Integer submittedItemCount;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer conflictCount;
    private LocalDateTime lastAppliedAt;
    private Long releaseBatchId;
    private Long approvalOrderId;
    private String approvalStatus;
    private Boolean executionPending;
    private List<ProductModelGovernanceAppliedItemVO> appliedItems;
}
```

```java
public static ProductModelGovernanceApplyResultVO buildPendingApplyResult(Long approvalOrderId,
                                                                          ProductModelGovernanceApplyDTO dto) {
    ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
    int submittedItemCount = safeItems(dto).size();
    // created / updated / skipped 统计保持不变
    result.setSubmittedItemCount(submittedItemCount);
    result.setReleaseBatchId(null);
    result.setApprovalOrderId(approvalOrderId);
    result.setApprovalStatus(APPROVAL_STATUS_PENDING);
    result.setExecutionPending(Boolean.TRUE);
    return result;
}
```

```ts
export interface ProductModelGovernanceApplyResult {
  submittedItemCount?: number | null;
  createdCount?: number | null;
  updatedCount?: number | null;
  skippedCount?: number | null;
  conflictCount?: number | null;
  releaseBatchId?: IdType | null;
  approvalOrderId?: IdType | null;
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | null;
  executionPending?: boolean | null;
}
```

- [ ] **Step 4: 重跑后端和前端定向测试，确认提交层 / 执行层语义同时通过**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=ProductModelControllerTest,ProductContractReleaseControllerTest,ProductContractGovernanceApprovalPayloadsTest,ProductModelServiceImplTest test
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

```text
BUILD SUCCESS
PASS src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts
PASS src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

- [ ] **Step 5: 提交后端语义固化**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalPayloads.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/governance/ProductContractGovernanceApprovalPayloadsTest.java spring-boot-iot-ui/src/types/api.ts
git commit -m "feat: separate governance submission semantics"
```

## Task 4: 回写文档并跑完整回归

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 先写文档改动，明确二期工作流和验收问题清单**

```md
## 产品物模型设计器二期

当前 `/products -> models` 已收束为产品契约治理工作台，固定主流程为：

1. 样本输入
2. compare 决策
3. 提交审批
4. 审批 / 发布反馈
5. 发布批次追溯与回滚

采集器产品页只治理采集器自身状态字段；子设备监测值与子设备状态需在子产品治理。
```

```md
### 验收关注点

1. 当前治理处于哪个阶段。
2. compare 中每个字段为何出现。
3. 审批提交与正式发布是否被明确区分。
4. 当前发布批次影响了哪些风险治理对象。
5. 回滚前是否能看到影响试算。
```

- [ ] **Step 2: 运行文档拓扑检查**

Run:

```bash
node scripts/docs/check-topology.mjs
```

Expected:

```text
Docs topology check passed
```

- [ ] **Step 3: 运行二期最小回归**

Run:

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
npm run build
node ../scripts/docs/check-topology.mjs
```

Expected:

```text
BUILD SUCCESS
PASS src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts
PASS src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
vite build completed successfully
Docs topology check passed
```

- [ ] **Step 4: 提交文档和最终回归结果**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document product governance workflow phase2"
```

## 完成标准

1. `/products -> models` 出现明确的治理阶段总览，并把主流程区与历史辅助区分开。
2. compare 行以“治理候选快照”表达规范字段、风险就绪和当前建议。
3. apply 回执明确区分“审批提交回执”和“正式发布结果”。
4. 后端回执增加提交层计数字段，并通过控制器 / payload helper 测试固化。
5. 采集器 / 子设备治理边界保持现有后端规则，并在工作区中被清晰表达。
6. `docs/02`、`docs/03`、`docs/19`、`docs/21` 与实现保持一致。
