<template>
  <IotAccessPageShell
    title="协议治理工作台"
    description="集中查看协议族定义与解密档案的草稿、发布状态和审批入口，运行时优先命中已发布快照。"
  >
    <section class="protocol-governance-workbench">
      <article class="protocol-governance-workbench__notice">
        <strong>治理边界</strong>
        <p>协议族定义与解密档案在这里治理，厂商字段映射规则仍留在 `/products -> 契约字段` 内收口。</p>
      </article>

      <div class="protocol-governance-workbench__summary">
        <article class="protocol-governance-workbench__summary-card">
          <span>协议族定义</span>
          <strong>{{ familyRows.length }}</strong>
        </article>
        <article class="protocol-governance-workbench__summary-card">
          <span>解密档案</span>
          <strong>{{ decryptProfileRows.length }}</strong>
        </article>
        <article class="protocol-governance-workbench__summary-card">
          <span>已发布协议对象</span>
          <strong>{{ publishedItemCount }}</strong>
        </article>
      </div>

      <StandardWorkbenchPanel
        title="协议族定义"
        description="发布后成为运行时协议族真相；未发布时仍可保留草稿继续维护。"
      >
        <p v-if="familyLoading" class="protocol-governance-workbench__hint">正在加载协议族定义...</p>
        <p v-else-if="familyErrorMessage" class="protocol-governance-workbench__hint">{{ familyErrorMessage }}</p>
        <div v-else-if="familyRows.length" class="protocol-governance-workbench__list">
          <article
            v-for="row in familyRows"
            :key="String(row.id ?? row.familyCode ?? '--')"
            class="protocol-governance-workbench__item"
          >
            <div class="protocol-governance-workbench__item-head">
              <div class="protocol-governance-workbench__item-title">
                <strong>{{ row.familyCode || '--' }}</strong>
                <span>{{ row.displayName || row.protocolCode || '--' }}</span>
                <span>{{ versionLabel(row.versionNo, row.publishedVersionNo, row.status, row.publishedStatus) }}</span>
              </div>
              <span class="protocol-governance-workbench__item-side">
                {{ `解密档案 ${row.decryptProfileCode || '--'}` }}
              </span>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton
                :data-testid="`protocol-family-submit-publish-${row.id}`"
                :disabled="row.id == null || submittingKey === `family-publish-${row.id}`"
                @click="handleSubmitFamilyPublish(row)"
              >
                {{ submittingKey === `family-publish-${row.id}` ? '提交中...' : '提交发布审批' }}
              </StandardButton>
              <StandardButton
                :data-testid="`protocol-family-submit-rollback-${row.id}`"
                :disabled="row.id == null || row.publishedStatus !== 'PUBLISHED' || submittingKey === `family-rollback-${row.id}`"
                @click="handleSubmitFamilyRollback(row)"
              >
                {{ submittingKey === `family-rollback-${row.id}` ? '提交中...' : '提交回滚审批' }}
              </StandardButton>
            </div>
          </article>
        </div>
        <div v-else class="protocol-governance-workbench__empty">
          <strong>当前还没有协议族定义</strong>
          <p>先在后端草稿表维护协议族，再通过本页提交发布审批。</p>
        </div>
      </StandardWorkbenchPanel>

      <StandardWorkbenchPanel
        title="解密档案"
        description="发布后优先于 YAML fallback；回滚后运行时将继续回退到已发布快照或 YAML。"
      >
        <p v-if="decryptProfileLoading" class="protocol-governance-workbench__hint">正在加载解密档案...</p>
        <p v-else-if="decryptProfileErrorMessage" class="protocol-governance-workbench__hint">{{ decryptProfileErrorMessage }}</p>
        <div v-else-if="decryptProfileRows.length" class="protocol-governance-workbench__list">
          <article
            v-for="row in decryptProfileRows"
            :key="String(row.id ?? row.profileCode ?? '--')"
            class="protocol-governance-workbench__item"
          >
            <div class="protocol-governance-workbench__item-head">
              <div class="protocol-governance-workbench__item-title">
                <strong>{{ row.profileCode || '--' }}</strong>
                <span>{{ row.algorithm || '--' }} · {{ row.merchantSource || '--' }}</span>
                <span>{{ versionLabel(row.versionNo, row.publishedVersionNo, row.status, row.publishedStatus) }}</span>
              </div>
              <span class="protocol-governance-workbench__item-side">
                {{ `Transformation ${row.transformation || '--'}` }}
              </span>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton
                :data-testid="`protocol-profile-submit-publish-${row.id}`"
                :disabled="row.id == null || submittingKey === `profile-publish-${row.id}`"
                @click="handleSubmitDecryptProfilePublish(row)"
              >
                {{ submittingKey === `profile-publish-${row.id}` ? '提交中...' : '提交发布审批' }}
              </StandardButton>
              <StandardButton
                :data-testid="`protocol-profile-submit-rollback-${row.id}`"
                :disabled="row.id == null || row.publishedStatus !== 'PUBLISHED' || submittingKey === `profile-rollback-${row.id}`"
                @click="handleSubmitDecryptProfileRollback(row)"
              >
                {{ submittingKey === `profile-rollback-${row.id}` ? '提交中...' : '提交回滚审批' }}
              </StandardButton>
            </div>
          </article>
        </div>
        <div v-else class="protocol-governance-workbench__empty">
          <strong>当前还没有解密档案</strong>
          <p>共享环境会继续沿用 YAML fallback，直到正式发布的解密档案进入运行时真相。</p>
        </div>
      </StandardWorkbenchPanel>
    </section>
  </IotAccessPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  pageProtocolDecryptProfiles,
  pageProtocolFamilies,
  submitProtocolDecryptProfilePublish,
  submitProtocolDecryptProfileRollback,
  submitProtocolFamilyPublish,
  submitProtocolFamilyRollback
} from '@/api/protocolGovernance'
import { resolveRequestErrorMessage } from '@/api/request'
import IotAccessPageShell from '@/components/iotAccess/IotAccessPageShell.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { ElMessage } from '@/utils/message'
import type { ProtocolDecryptProfile, ProtocolFamilyDefinition } from '@/types/api'

const familyRows = ref<ProtocolFamilyDefinition[]>([])
const decryptProfileRows = ref<ProtocolDecryptProfile[]>([])
const familyLoading = ref(false)
const decryptProfileLoading = ref(false)
const familyErrorMessage = ref('')
const decryptProfileErrorMessage = ref('')
const submittingKey = ref('')

const publishedItemCount = computed(() => {
  const familyPublished = familyRows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length
  const profilePublished = decryptProfileRows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length
  return familyPublished + profilePublished
})

onMounted(() => {
  void Promise.all([loadFamilies(), loadDecryptProfiles()])
})

function versionLabel(
  draftVersionNo?: number | null,
  publishedVersionNo?: number | null,
  draftStatus?: string | null,
  publishedStatus?: string | null
) {
  return `v${draftVersionNo ?? '--'} / v${publishedVersionNo ?? '--'} · ${draftStatus || '--'} / ${publishedStatus || '未发布'}`
}

async function loadFamilies() {
  familyLoading.value = true
  familyErrorMessage.value = ''
  try {
    const response = await pageProtocolFamilies({
      pageNum: 1,
      pageSize: 20
    })
    familyRows.value = response.data?.records ?? []
  } catch (error) {
    familyRows.value = []
    familyErrorMessage.value = resolveRequestErrorMessage(error, '协议族定义加载失败')
  } finally {
    familyLoading.value = false
  }
}

async function loadDecryptProfiles() {
  decryptProfileLoading.value = true
  decryptProfileErrorMessage.value = ''
  try {
    const response = await pageProtocolDecryptProfiles({
      pageNum: 1,
      pageSize: 20
    })
    decryptProfileRows.value = response.data?.records ?? []
  } catch (error) {
    decryptProfileRows.value = []
    decryptProfileErrorMessage.value = resolveRequestErrorMessage(error, '解密档案加载失败')
  } finally {
    decryptProfileLoading.value = false
  }
}

async function handleSubmitFamilyPublish(row: ProtocolFamilyDefinition) {
  if (row.id == null) {
    return
  }
  submittingKey.value = `family-publish-${row.id}`
  try {
    const response = await submitProtocolFamilyPublish(
      row.id,
      `提交协议族定义发布审批：${row.familyCode || '--'}`
    )
    ElMessage.success(
      response.data?.approvalOrderId != null
        ? `协议族定义发布审批已提交，审批单 ${response.data.approvalOrderId}`
        : '协议族定义发布审批已提交'
    )
    await loadFamilies()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议族定义发布审批提交失败'))
  } finally {
    submittingKey.value = ''
  }
}

async function handleSubmitFamilyRollback(row: ProtocolFamilyDefinition) {
  if (row.id == null) {
    return
  }
  submittingKey.value = `family-rollback-${row.id}`
  try {
    const response = await submitProtocolFamilyRollback(
      row.id,
      `提交协议族定义回滚审批：${row.familyCode || '--'}`
    )
    ElMessage.success(
      response.data?.approvalOrderId != null
        ? `协议族定义回滚审批已提交，审批单 ${response.data.approvalOrderId}`
        : '协议族定义回滚审批已提交'
    )
    await loadFamilies()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议族定义回滚审批提交失败'))
  } finally {
    submittingKey.value = ''
  }
}

async function handleSubmitDecryptProfilePublish(row: ProtocolDecryptProfile) {
  if (row.id == null) {
    return
  }
  submittingKey.value = `profile-publish-${row.id}`
  try {
    const response = await submitProtocolDecryptProfilePublish(
      row.id,
      `提交解密档案发布审批：${row.profileCode || '--'}`
    )
    ElMessage.success(
      response.data?.approvalOrderId != null
        ? `解密档案发布审批已提交，审批单 ${response.data.approvalOrderId}`
        : '解密档案发布审批已提交'
    )
    await loadDecryptProfiles()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '解密档案发布审批提交失败'))
  } finally {
    submittingKey.value = ''
  }
}

async function handleSubmitDecryptProfileRollback(row: ProtocolDecryptProfile) {
  if (row.id == null) {
    return
  }
  submittingKey.value = `profile-rollback-${row.id}`
  try {
    const response = await submitProtocolDecryptProfileRollback(
      row.id,
      `提交解密档案回滚审批：${row.profileCode || '--'}`
    )
    ElMessage.success(
      response.data?.approvalOrderId != null
        ? `解密档案回滚审批已提交，审批单 ${response.data.approvalOrderId}`
        : '解密档案回滚审批已提交'
    )
    await loadDecryptProfiles()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '解密档案回滚审批提交失败'))
  } finally {
    submittingKey.value = ''
  }
}
</script>

<style scoped>
.protocol-governance-workbench,
.protocol-governance-workbench__summary,
.protocol-governance-workbench__list {
  display: grid;
}

.protocol-governance-workbench {
  gap: 0.82rem;
}

.protocol-governance-workbench__notice,
.protocol-governance-workbench__summary-card,
.protocol-governance-workbench__item,
.protocol-governance-workbench__empty {
  padding: 0.88rem 0.96rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: 0.78rem;
  background: white;
}

.protocol-governance-workbench__notice,
.protocol-governance-workbench__summary-card,
.protocol-governance-workbench__item,
.protocol-governance-workbench__empty,
.protocol-governance-workbench__item-title {
  display: grid;
  gap: 0.24rem;
}

.protocol-governance-workbench__notice strong,
.protocol-governance-workbench__summary-card strong,
.protocol-governance-workbench__item-title strong,
.protocol-governance-workbench__empty strong {
  color: var(--text-heading);
}

.protocol-governance-workbench__notice p,
.protocol-governance-workbench__summary-card span,
.protocol-governance-workbench__item-title span,
.protocol-governance-workbench__item-side,
.protocol-governance-workbench__hint,
.protocol-governance-workbench__empty p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.56;
}

.protocol-governance-workbench__summary {
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.72rem;
}

.protocol-governance-workbench__list {
  gap: 0.72rem;
}

.protocol-governance-workbench__item-head,
.protocol-governance-workbench__actions {
  display: flex;
  justify-content: space-between;
  gap: 0.72rem;
  align-items: flex-start;
}

.protocol-governance-workbench__actions {
  flex-wrap: wrap;
}

.protocol-governance-workbench__empty {
  margin-top: 0.12rem;
}

@media (max-width: 720px) {
  .protocol-governance-workbench__item-head,
  .protocol-governance-workbench__actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
