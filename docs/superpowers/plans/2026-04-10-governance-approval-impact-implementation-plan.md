# Governance Approval Impact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose contract release impact analysis directly inside the governance approval detail drawer so the control plane can answer what a release changed and what rollback would affect.

**Architecture:** Reuse the existing contract release impact read API instead of introducing new backend objects. Parse `releaseBatchId` or `rolledBackBatchId` from approval execution payloads, fetch the batch impact only for contract release / rollback approvals, and render a readable impact summary plus changed field rows in the approval detail drawer.

**Tech Stack:** Vue 3, TypeScript, Vitest, existing `productApi`, existing governance approval APIs, existing device contract release backend endpoints.

---

### Task 1: Add Approval Impact Frontend Contract

**Files:**
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/GovernanceApprovalView.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
it('loads and renders release batch impact when approval detail exposes releaseBatchId', async () => {
  mockGetProductContractReleaseBatchImpact.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      batchId: 99001,
      addedCount: 1,
      removedCount: 1,
      changedCount: 2,
      unchangedCount: 3,
      impactItems: [
        { changeType: 'UPDATED', modelType: 'property', identifier: 'value', changedFields: ['modelName'] }
      ]
    }
  })

  const wrapper = mountView()
  await flushPromises()
  await wrapper.findAll('button').find((button) => button.text().includes('详情'))?.trigger('click')
  await flushPromises()

  expect(mockGetProductContractReleaseBatchImpact).toHaveBeenCalledWith(99001)
  expect(wrapper.text()).toContain('发布影响分析')
  expect(wrapper.text()).toContain('新增 1')
  expect(wrapper.text()).toContain('删除 1')
  expect(wrapper.text()).toContain('变更 2')
  expect(wrapper.text()).toContain('value')
  expect(wrapper.text()).toContain('modelName')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceApprovalView.test.ts`
Expected: FAIL because the approval view does not call the impact API and does not render the impact section.

- [ ] **Step 3: Write minimal implementation**

```ts
export interface ProductContractReleaseImpact {
  batchId?: IdType | null
  addedCount?: number | null
  removedCount?: number | null
  changedCount?: number | null
  unchangedCount?: number | null
  impactItems?: Array<{
    changeType?: string | null
    modelType?: string | null
    identifier?: string | null
    changedFields?: string[] | null
  }> | null
}

getProductContractReleaseBatchImpact(batchId: IdType): Promise<ApiEnvelope<ProductContractReleaseImpact>> {
  return request<ProductContractReleaseImpact>(`/api/device/product/contract-release-batches/${batchId}/impact`, { method: 'GET' })
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceApprovalView.test.ts`
Expected: PASS for the new impact-analysis case.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/api/product.ts spring-boot-iot-ui/src/__tests__/views/GovernanceApprovalView.test.ts
git commit -m "feat: expose release impact api in approval ui"
```

### Task 2: Render Impact Analysis In Governance Approval Drawer

**Files:**
- Modify: `spring-boot-iot-ui/src/views/GovernanceApprovalView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceApprovalView.test.ts`

- [ ] **Step 1: Extend the failing test with non-impact guard behavior**

```ts
it('does not load release impact when approval detail has no release batch context', async () => {
  mockGetOrderDetail.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      order: {
        ...createPendingOrder(),
        payloadJson: JSON.stringify({ version: 1, execution: { result: {} } })
      },
      transitions: []
    }
  })

  const wrapper = mountView()
  await flushPromises()
  await wrapper.findAll('button').find((button) => button.text().includes('详情'))?.trigger('click')
  await flushPromises()

  expect(mockGetProductContractReleaseBatchImpact).not.toHaveBeenCalled()
  expect(wrapper.text()).not.toContain('发布影响分析')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceApprovalView.test.ts`
Expected: FAIL because the view has no conditional impact section.

- [ ] **Step 3: Write minimal implementation**

```ts
const releaseBatchId = computed(() => {
  const result = parsedExecutionResult.value
  return normalizeId(result?.releaseBatchId ?? result?.rolledBackBatchId)
})

watch([detailVisible, releaseBatchId], async ([visible, batchId]) => {
  if (!visible || batchId == null) {
    detailImpact.value = null
    return
  }
  const response = await productApi.getProductContractReleaseBatchImpact(batchId)
  detailImpact.value = response.data ?? null
})
```

```vue
<section v-if="detailImpact" class="governance-approval-detail-section">
  <h3>发布影响分析</h3>
  <div class="governance-approval-impact-kpis">
    <span>新增 {{ detailImpact.addedCount ?? 0 }}</span>
    <span>删除 {{ detailImpact.removedCount ?? 0 }}</span>
    <span>变更 {{ detailImpact.changedCount ?? 0 }}</span>
    <span>未变更 {{ detailImpact.unchangedCount ?? 0 }}</span>
  </div>
  <article v-for="item in detailImpact.impactItems ?? []" :key="`${item.changeType}-${item.identifier}`">
    <strong>{{ item.changeType }} · {{ item.modelType }} · {{ item.identifier }}</strong>
    <span>{{ (item.changedFields ?? []).join(' / ') || '无字段差异明细' }}</span>
  </article>
</section>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceApprovalView.test.ts`
Expected: PASS for both impact and non-impact cases.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/views/GovernanceApprovalView.vue spring-boot-iot-ui/src/__tests__/views/GovernanceApprovalView.test.ts
git commit -m "feat: show release impact in approval drawer"
```

### Task 3: Sync Documentation And Verify

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update docs in place**

```md
- `2026-04-10` 起，治理审批台详情抽屉会在审批执行结果存在 `releaseBatchId / rolledBackBatchId` 时自动加载合同发布影响分析，直接展示新增/删除/变更字段统计与影响项清单。
```

- [ ] **Step 2: Run focused verification**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceApprovalView.test.ts`
Expected: PASS

Run: `git diff --check`
Expected: no whitespace errors

- [ ] **Step 3: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md
git commit -m "docs: describe approval impact analysis surfacing"
```
