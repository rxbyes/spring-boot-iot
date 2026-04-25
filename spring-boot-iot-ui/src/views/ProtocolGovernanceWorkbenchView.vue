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
        <article class="protocol-governance-workbench__summary-card">
          <span>协议模板</span>
          <strong>{{ templateRows.length }}</strong>
        </article>
      </div>

      <StandardWorkbenchPanel
        title="协议族定义"
        description="发布后成为运行时协议族真相；未发布时仍可保留草稿继续维护。"
      >
        <section class="protocol-governance-workbench__editor">
          <div class="protocol-governance-workbench__editor-head">
            <div class="protocol-governance-workbench__editor-copy">
              <strong>{{ familyDraftModeTitle }}</strong>
              <p>编辑态仍按 `familyCode` 作为业务键保存；如修改编码，将按新的协议族草稿写入。</p>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton v-permission="'iot:protocol-governance:family-draft'" action="reset" @click="resetFamilyDraft">清空草稿</StandardButton>
              <StandardButton
                v-permission="'iot:protocol-governance:family-draft'"
                action="confirm"
                data-testid="protocol-family-save"
                :disabled="familySaving"
                v-permission="'iot:protocol-governance:family-draft'"
                @click="handleSaveFamily"
              >
                {{ familySaving ? '保存中...' : '保存草稿' }}
              </StandardButton>
            </div>
          </div>

          <div class="protocol-governance-workbench__form-grid">
            <label class="protocol-governance-workbench__field">
              <span>协议族编码</span>
              <input
                data-testid="protocol-family-family-code"
                v-model="familyDraft.familyCode"
                type="text"
                placeholder="例如 legacy-dp-crack"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>协议编码</span>
              <input
                data-testid="protocol-family-protocol-code"
                v-model="familyDraft.protocolCode"
                type="text"
                placeholder="例如 mqtt-json"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>显示名称</span>
              <input
                data-testid="protocol-family-display-name"
                v-model="familyDraft.displayName"
                type="text"
                placeholder="输入当前协议族的展示名称"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>解密档案编码</span>
              <input
                data-testid="protocol-family-decrypt-profile-code"
                v-model="familyDraft.decryptProfileCode"
                type="text"
                placeholder="可选"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>签名算法</span>
              <input
                data-testid="protocol-family-sign-algorithm"
                v-model="familyDraft.signAlgorithm"
                type="text"
                placeholder="可选"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>归一化策略</span>
              <input
                data-testid="protocol-family-normalization-strategy"
                v-model="familyDraft.normalizationStrategy"
                type="text"
                placeholder="可选"
              />
            </label>
          </div>
        </section>

        <p v-if="familyLoading" class="protocol-governance-workbench__hint">正在加载协议族定义...</p>
        <p v-else-if="familyErrorMessage" class="protocol-governance-workbench__hint">{{ familyErrorMessage }}</p>
        <template v-else-if="familyRows.length">
          <div class="protocol-governance-workbench__batch">
            <span>{{ `已选 ${selectedFamilyIds.length} 项` }}</span>
            <div class="protocol-governance-workbench__actions">
              <StandardButton
                v-permission="'iot:protocol-governance:family-publish'"
                data-testid="protocol-family-batch-submit-publish"
                :disabled="selectedFamilyIds.length === 0 || familyBatchSubmitting"
                v-permission="'iot:protocol-governance:family-publish'"
                @click="handleSubmitFamilyBatchPublish"
              >
                {{ familyBatchSubmitting ? '提交中...' : '批量提交发布审批' }}
              </StandardButton>
              <StandardButton
                v-permission="'iot:protocol-governance:family-rollback'"
                data-testid="protocol-family-batch-submit-rollback"
                :disabled="selectedFamilyIds.length === 0 || familyBatchSubmitting"
                v-permission="'iot:protocol-governance:family-rollback'"
                @click="handleSubmitFamilyBatchRollback"
              >
                {{ familyBatchSubmitting ? '提交中...' : '批量提交回滚审批' }}
              </StandardButton>
            </div>
          </div>
          <div class="protocol-governance-workbench__list">
            <article
              v-for="row in familyRows"
              :key="String(row.id ?? row.familyCode ?? '--')"
              class="protocol-governance-workbench__item"
            >
              <div class="protocol-governance-workbench__item-head">
                <label class="protocol-governance-workbench__checkbox">
                  <input
                    v-if="row.id != null"
                    :data-testid="`protocol-family-select-${row.id}`"
                    :value="row.id"
                    v-model="selectedFamilyIds"
                    type="checkbox"
                  />
                  <span>{{ row.id == null ? '未持久化' : '批量' }}</span>
                </label>
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
                  v-permission="'iot:protocol-governance:family-draft'"
                  :data-testid="`protocol-family-edit-${row.id}`"
                  action="default"
                  v-permission="'iot:protocol-governance:family-draft'"
                  @click="applyFamilyDraft(row)"
                >
                  编辑草稿
                </StandardButton>
                <StandardButton
                  :data-testid="`protocol-family-detail-${row.id}`"
                  :disabled="row.id == null || familyDetailLoadingKey === `family-${row.id}`"
                  action="default"
                  @click="handleLoadFamilyDetail(row)"
                >
                  {{ familyDetailLoadingKey === `family-${row.id}` ? '加载中...' : '查看详情' }}
                </StandardButton>
                <StandardButton
                  v-permission="'iot:protocol-governance:family-publish'"
                  :data-testid="`protocol-family-submit-publish-${row.id}`"
                  :disabled="row.id == null || submittingKey === `family-publish-${row.id}`"
                  v-permission="'iot:protocol-governance:family-publish'"
                  @click="handleSubmitFamilyPublish(row)"
                >
                  {{ submittingKey === `family-publish-${row.id}` ? '提交中...' : '提交发布审批' }}
                </StandardButton>
                <StandardButton
                  v-permission="'iot:protocol-governance:family-rollback'"
                  :data-testid="`protocol-family-submit-rollback-${row.id}`"
                  :disabled="
                    row.id == null ||
                    row.publishedStatus !== 'PUBLISHED' ||
                    submittingKey === `family-rollback-${row.id}`
                  "
                  v-permission="'iot:protocol-governance:family-rollback'"
                  @click="handleSubmitFamilyRollback(row)"
                >
                  {{ submittingKey === `family-rollback-${row.id}` ? '提交中...' : '提交回滚审批' }}
                </StandardButton>
              </div>
            </article>
          </div>
        </template>
        <div v-else class="protocol-governance-workbench__empty">
          <strong>当前还没有协议族定义</strong>
          <p>先创建协议族草稿，再通过本页提交发布审批。</p>
        </div>
      </StandardWorkbenchPanel>

      <StandardWorkbenchPanel
        title="解密档案"
        description="发布后优先于 YAML fallback；回滚后运行时将继续回退到已发布快照或 YAML。"
      >
        <section class="protocol-governance-workbench__editor">
          <div class="protocol-governance-workbench__editor-head">
            <div class="protocol-governance-workbench__editor-copy">
              <strong>{{ decryptProfileDraftModeTitle }}</strong>
              <p>解密档案编辑同样按 `profileCode` 落草稿，便于共享环境快速维护供应商密钥配置。</p>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton v-permission="'iot:protocol-governance:decrypt-draft'" action="reset" @click="resetDecryptProfileDraft">清空草稿</StandardButton>
              <StandardButton
                v-permission="'iot:protocol-governance:decrypt-draft'"
                action="confirm"
                data-testid="protocol-profile-save"
                :disabled="decryptProfileSaving"
                v-permission="'iot:protocol-governance:decrypt-draft'"
                @click="handleSaveDecryptProfile"
              >
                {{ decryptProfileSaving ? '保存中...' : '保存草稿' }}
              </StandardButton>
            </div>
          </div>

          <div class="protocol-governance-workbench__form-grid">
            <label class="protocol-governance-workbench__field">
              <span>档案编码</span>
              <input
                data-testid="protocol-profile-profile-code"
                v-model="decryptProfileDraft.profileCode"
                type="text"
                placeholder="例如 aes-62000002"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>算法</span>
              <input
                data-testid="protocol-profile-algorithm"
                v-model="decryptProfileDraft.algorithm"
                type="text"
                placeholder="例如 AES"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>商户来源</span>
              <input
                data-testid="protocol-profile-merchant-source"
                v-model="decryptProfileDraft.merchantSource"
                type="text"
                placeholder="例如 IOT_PROTOCOL_CRYPTO"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>商户键</span>
              <input
                data-testid="protocol-profile-merchant-key"
                v-model="decryptProfileDraft.merchantKey"
                type="text"
                placeholder="例如 aes-62000002"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>Transformation</span>
              <input
                data-testid="protocol-profile-transformation"
                v-model="decryptProfileDraft.transformation"
                type="text"
                placeholder="可选"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>签名密钥</span>
              <input
                data-testid="protocol-profile-signature-secret"
                v-model="decryptProfileDraft.signatureSecret"
                type="text"
                placeholder="可选"
              />
            </label>
          </div>
        </section>

        <section class="protocol-governance-workbench__preview">
          <div class="protocol-governance-workbench__editor-head">
            <div class="protocol-governance-workbench__editor-copy">
              <strong>解密命中试算</strong>
              <p>直接按协议族 + 协议编码 + appId 试算，优先命中当前草稿，再回退到已发布快照。</p>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton
                v-permission="'iot:protocol-governance:decrypt-preview'"
                action="confirm"
                data-testid="protocol-preview-submit"
                :disabled="previewLoading"
                v-permission="'iot:protocol-governance:decrypt-preview'"
                @click="handlePreviewDecrypt"
              >
                {{ previewLoading ? '试算中...' : '运行试算' }}
              </StandardButton>
              <StandardButton
                v-permission="'iot:protocol-governance:decrypt-replay'"
                action="default"
                data-testid="protocol-replay-submit"
                :disabled="replayLoading"
                v-permission="'iot:protocol-governance:decrypt-replay'"
                @click="handleReplayDecrypt"
              >
                {{ replayLoading ? '回放中...' : '运行回放' }}
              </StandardButton>
            </div>
          </div>

          <div class="protocol-governance-workbench__form-grid protocol-governance-workbench__form-grid--preview">
            <label class="protocol-governance-workbench__field">
              <span>协议族编码</span>
              <input
                data-testid="protocol-preview-family-code"
                v-model="previewDraft.familyCode"
                type="text"
                placeholder="例如 legacy-dp-crack"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>协议编码</span>
              <input
                data-testid="protocol-preview-protocol-code"
                v-model="previewDraft.protocolCode"
                type="text"
                placeholder="例如 mqtt-json"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>appId</span>
              <input
                data-testid="protocol-preview-app-id"
                v-model="previewDraft.appId"
                type="text"
                placeholder="例如 62000001"
              />
            </label>
          </div>

          <article v-if="previewResult" class="protocol-governance-workbench__preview-result">
            <strong>最近一次试算</strong>
            <p>{{ `命中来源 ${previewResult.hitSource || '--'}` }}</p>
            <p>{{ `解析档案 ${previewResult.resolvedProfileCode || '--'}` }}</p>
            <p>{{ `算法 ${previewResult.algorithm || '--'} / 商户来源 ${previewResult.merchantSource || '--'}` }}</p>
          </article>
          <article v-if="replayResult" class="protocol-governance-workbench__preview-result">
            <strong>最近一次回放</strong>
            <p>{{ `命中来源 ${replayResult.hitSource || '--'}` }}</p>
            <p>{{ `协议族 ${replayResult.familyCode || '--'} / 协议编码 ${replayResult.protocolCode || '--'}` }}</p>
            <p>{{ `解析档案 ${replayResult.resolvedProfileCode || '--'} / appId ${replayResult.appId || '--'}` }}</p>
          </article>
        </section>

        <p v-if="decryptProfileLoading" class="protocol-governance-workbench__hint">正在加载解密档案...</p>
        <p v-else-if="decryptProfileErrorMessage" class="protocol-governance-workbench__hint">
          {{ decryptProfileErrorMessage }}
        </p>
        <template v-else-if="decryptProfileRows.length">
          <div class="protocol-governance-workbench__batch">
            <span>{{ `已选 ${selectedDecryptProfileIds.length} 项` }}</span>
            <div class="protocol-governance-workbench__actions">
              <StandardButton
                v-permission="'iot:protocol-governance:decrypt-publish'"
                data-testid="protocol-profile-batch-submit-publish"
                :disabled="selectedDecryptProfileIds.length === 0 || decryptProfileBatchSubmitting"
                v-permission="'iot:protocol-governance:decrypt-publish'"
                @click="handleSubmitDecryptProfileBatchPublish"
              >
                {{ decryptProfileBatchSubmitting ? '提交中...' : '批量提交发布审批' }}
              </StandardButton>
              <StandardButton
                v-permission="'iot:protocol-governance:decrypt-rollback'"
                data-testid="protocol-profile-batch-submit-rollback"
                :disabled="selectedDecryptProfileIds.length === 0 || decryptProfileBatchSubmitting"
                v-permission="'iot:protocol-governance:decrypt-rollback'"
                @click="handleSubmitDecryptProfileBatchRollback"
              >
                {{ decryptProfileBatchSubmitting ? '提交中...' : '批量提交回滚审批' }}
              </StandardButton>
            </div>
          </div>
          <div class="protocol-governance-workbench__list">
            <article
              v-for="row in decryptProfileRows"
              :key="String(row.id ?? row.profileCode ?? '--')"
              class="protocol-governance-workbench__item"
            >
              <div class="protocol-governance-workbench__item-head">
                <label class="protocol-governance-workbench__checkbox">
                  <input
                    v-if="row.id != null"
                    :data-testid="`protocol-profile-select-${row.id}`"
                    :value="row.id"
                    v-model="selectedDecryptProfileIds"
                    type="checkbox"
                  />
                  <span>{{ row.id == null ? '未持久化' : '批量' }}</span>
                </label>
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
                  v-permission="'iot:protocol-governance:decrypt-draft'"
                  :data-testid="`protocol-profile-edit-${row.id}`"
                  action="default"
                  v-permission="'iot:protocol-governance:decrypt-draft'"
                  @click="applyDecryptProfileDraft(row)"
                >
                  编辑草稿
                </StandardButton>
                <StandardButton
                  :data-testid="`protocol-profile-detail-${row.id}`"
                  :disabled="row.id == null || decryptProfileDetailLoadingKey === `profile-${row.id}`"
                  action="default"
                  @click="handleLoadDecryptProfileDetail(row)"
                >
                  {{ decryptProfileDetailLoadingKey === `profile-${row.id}` ? '加载中...' : '查看详情' }}
                </StandardButton>
                <StandardButton
                  v-permission="'iot:protocol-governance:decrypt-publish'"
                  :data-testid="`protocol-profile-submit-publish-${row.id}`"
                  :disabled="row.id == null || submittingKey === `profile-publish-${row.id}`"
                  v-permission="'iot:protocol-governance:decrypt-publish'"
                  @click="handleSubmitDecryptProfilePublish(row)"
                >
                  {{ submittingKey === `profile-publish-${row.id}` ? '提交中...' : '提交发布审批' }}
                </StandardButton>
                <StandardButton
                  v-permission="'iot:protocol-governance:decrypt-rollback'"
                  :data-testid="`protocol-profile-submit-rollback-${row.id}`"
                  :disabled="
                    row.id == null ||
                    row.publishedStatus !== 'PUBLISHED' ||
                    submittingKey === `profile-rollback-${row.id}`
                  "
                  v-permission="'iot:protocol-governance:decrypt-rollback'"
                  @click="handleSubmitDecryptProfileRollback(row)"
                >
                  {{ submittingKey === `profile-rollback-${row.id}` ? '提交中...' : '提交回滚审批' }}
                </StandardButton>
              </div>
            </article>
          </div>
        </template>
        <div v-else class="protocol-governance-workbench__empty">
          <strong>当前还没有解密档案</strong>
          <p>共享环境会继续沿用 YAML fallback，直到正式发布的解密档案进入运行时真相。</p>
        </div>
      </StandardWorkbenchPanel>

      <StandardWorkbenchPanel
        title="协议模板"
        description="先以模板草稿和模板回放收口 legacy `$dp` 子模板，后续运行时会切到配置化解释执行。"
      >
        <section class="protocol-governance-workbench__editor">
          <div class="protocol-governance-workbench__editor-head">
            <div class="protocol-governance-workbench__editor-copy">
              <strong>{{ templateDraftModeTitle }}</strong>
              <p>模板草稿当前按 `templateCode` 作为业务键保存，先服务治理校验和后续解释执行接入。</p>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton v-permission="'iot:protocol-governance:template-draft'" action="reset" @click="resetTemplateDraft">清空草稿</StandardButton>
              <StandardButton
                v-permission="'iot:protocol-governance:template-draft'"
                action="confirm"
                data-testid="protocol-template-save"
                :disabled="templateSaving"
                v-permission="'iot:protocol-governance:template-draft'"
                @click="handleSaveTemplate"
              >
                {{ templateSaving ? '保存中...' : '保存草稿' }}
              </StandardButton>
            </div>
          </div>

          <div class="protocol-governance-workbench__form-grid">
            <label class="protocol-governance-workbench__field">
              <span>模板编码</span>
              <input
                data-testid="protocol-template-template-code"
                v-model="templateDraft.templateCode"
                type="text"
                placeholder="例如 legacy-dp-crack-v1"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>协议族编码</span>
              <input
                data-testid="protocol-template-family-code"
                v-model="templateDraft.familyCode"
                type="text"
                placeholder="例如 legacy-dp"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>协议编码</span>
              <input
                data-testid="protocol-template-protocol-code"
                v-model="templateDraft.protocolCode"
                type="text"
                placeholder="例如 mqtt-json"
              />
            </label>
            <label class="protocol-governance-workbench__field">
              <span>显示名称</span>
              <input
                data-testid="protocol-template-display-name"
                v-model="templateDraft.displayName"
                type="text"
                placeholder="输入模板展示名称"
              />
            </label>
            <label class="protocol-governance-workbench__field protocol-governance-workbench__field--full">
              <span>表达式 JSON</span>
              <textarea
                data-testid="protocol-template-expression-json"
                v-model="templateDraft.expressionJson"
                rows="4"
                placeholder='例如 {"logicalPattern":"^L1_LF_\\d+$"}'
              />
            </label>
            <label class="protocol-governance-workbench__field protocol-governance-workbench__field--full">
              <span>输出映射 JSON</span>
              <textarea
                data-testid="protocol-template-output-mapping-json"
                v-model="templateDraft.outputMappingJson"
                rows="3"
                placeholder='例如 {"value":"$.value"}'
              />
            </label>
          </div>
        </section>

        <section class="protocol-governance-workbench__preview">
          <div class="protocol-governance-workbench__editor-head">
            <div class="protocol-governance-workbench__editor-copy">
              <strong>模板回放</strong>
              <p>输入模板编码和示例载荷，校验当前模板桥接到 legacy 运行时后的提取结果。</p>
            </div>
            <div class="protocol-governance-workbench__actions">
              <StandardButton
                v-permission="'iot:protocol-governance:template-replay'"
                action="default"
                data-testid="protocol-template-replay-submit"
                :disabled="templateReplayLoading"
                v-permission="'iot:protocol-governance:template-replay'"
                @click="handleReplayTemplate"
              >
                {{ templateReplayLoading ? '回放中...' : '运行模板回放' }}
              </StandardButton>
            </div>
          </div>

          <div class="protocol-governance-workbench__form-grid">
            <label class="protocol-governance-workbench__field">
              <span>模板编码</span>
              <input
                data-testid="protocol-template-replay-template-code"
                v-model="templateReplayDraft.templateCode"
                type="text"
                placeholder="例如 legacy-dp-crack-v1"
              />
            </label>
            <label class="protocol-governance-workbench__field protocol-governance-workbench__field--full">
              <span>载荷 JSON</span>
              <textarea
                data-testid="protocol-template-replay-payload-json"
                v-model="templateReplayDraft.payloadJson"
                rows="6"
                placeholder='例如 {"L1_LF_1":{"2026-04-05T08:23:10.000Z":0.2136}}'
              />
            </label>
          </div>

          <article v-if="templateReplayResult" class="protocol-governance-workbench__preview-result">
            <strong>最近一次模板回放</strong>
            <p>{{ templateReplayResult.summary || '--' }}</p>
            <p>{{ `桥接模板 ${templateReplayResult.resolvedTemplateCode || '--'}` }}</p>
            <p>{{ `提取子通道 ${(templateReplayResult.extractedChildren ?? []).length}` }}</p>
          </article>
        </section>

        <p v-if="templateLoading" class="protocol-governance-workbench__hint">正在加载协议模板...</p>
        <p v-else-if="templateErrorMessage" class="protocol-governance-workbench__hint">{{ templateErrorMessage }}</p>
        <template v-else-if="templateRows.length">
          <div class="protocol-governance-workbench__list">
            <article
              v-for="row in templateRows"
              :key="String(row.id ?? row.templateCode ?? '--')"
              class="protocol-governance-workbench__item"
            >
              <div class="protocol-governance-workbench__item-head">
                <div class="protocol-governance-workbench__item-title">
                  <strong>{{ row.templateCode || '--' }}</strong>
                  <span>{{ `${row.familyCode || '--'} · ${row.protocolCode || '--'}` }}</span>
                  <span>{{ versionLabel(row.versionNo, row.publishedVersionNo, row.status, row.publishedStatus) }}</span>
                </div>
                <span class="protocol-governance-workbench__item-side">
                  {{ row.displayName || '--' }}
                </span>
              </div>
              <div class="protocol-governance-workbench__actions">
                <StandardButton
                  v-permission="'iot:protocol-governance:template-draft'"
                  :data-testid="`protocol-template-edit-${row.id}`"
                  action="default"
                  v-permission="'iot:protocol-governance:template-draft'"
                  @click="applyTemplateDraft(row)"
                >
                  编辑草稿
                </StandardButton>
                <StandardButton
                  :data-testid="`protocol-template-detail-${row.id}`"
                  :disabled="row.id == null || templateDetailLoadingKey === `template-${row.id}`"
                  action="default"
                  @click="handleLoadTemplateDetail(row)"
                >
                  {{ templateDetailLoadingKey === `template-${row.id}` ? '加载中...' : '查看详情' }}
                </StandardButton>
                <StandardButton
                  v-permission="'iot:protocol-governance:template-publish'"
                  :data-testid="`protocol-template-publish-${row.id}`"
                  :disabled="row.id == null || submittingKey === `template-publish-${row.id}`"
                  v-permission="'iot:protocol-governance:template-publish'"
                  @click="handlePublishTemplate(row)"
                >
                  {{ submittingKey === `template-publish-${row.id}` ? '发布中...' : '发布快照' }}
                </StandardButton>
              </div>
            </article>
          </div>
        </template>
        <div v-else class="protocol-governance-workbench__empty">
          <strong>当前还没有协议模板</strong>
          <p>先保存模板草稿，再做模板回放和发布快照。</p>
        </div>
      </StandardWorkbenchPanel>
    </section>
  </IotAccessPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getProtocolDecryptProfileDetail,
  getProtocolFamilyDetail,
  getProtocolTemplateDetail,
  pageProtocolDecryptProfiles,
  pageProtocolFamilies,
  pageProtocolTemplates,
  publishProtocolTemplate,
  previewProtocolDecrypt,
  replayProtocolDecrypt,
  replayProtocolTemplate,
  saveProtocolDecryptProfile,
  saveProtocolFamily,
  saveProtocolTemplate,
  submitProtocolDecryptProfileBatchPublish,
  submitProtocolDecryptProfileBatchRollback,
  submitProtocolDecryptProfilePublish,
  submitProtocolDecryptProfileRollback,
  submitProtocolFamilyBatchPublish,
  submitProtocolFamilyBatchRollback,
  submitProtocolFamilyPublish,
  submitProtocolFamilyRollback
} from '@/api/protocolGovernance'
import { resolveRequestErrorMessage } from '@/api/request'
import IotAccessPageShell from '@/components/iotAccess/IotAccessPageShell.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { ElMessage } from '@/utils/message'
import type {
  IdType,
  ProtocolGovernanceBatchSubmitResult,
  ProtocolDecryptPreview,
  ProtocolDecryptProfile,
  ProtocolDecryptProfileUpsertPayload,
  ProtocolGovernanceReplay,
  ProtocolFamilyDefinition,
  ProtocolFamilyDefinitionUpsertPayload,
  ProtocolTemplateDefinition,
  ProtocolTemplateDefinitionUpsertPayload,
  ProtocolTemplateReplay,
  ProtocolTemplateReplayPayload
} from '@/types/api'

interface ProtocolFamilyDraftForm {
  familyCode: string
  protocolCode: string
  displayName: string
  decryptProfileCode: string
  signAlgorithm: string
  normalizationStrategy: string
}

interface ProtocolDecryptProfileDraftForm {
  profileCode: string
  algorithm: string
  merchantSource: string
  merchantKey: string
  transformation: string
  signatureSecret: string
}

interface ProtocolDecryptPreviewForm {
  familyCode: string
  protocolCode: string
  appId: string
}

interface ProtocolTemplateDraftForm {
  templateCode: string
  familyCode: string
  protocolCode: string
  displayName: string
  expressionJson: string
  outputMappingJson: string
}

interface ProtocolTemplateReplayForm {
  templateCode: string
  payloadJson: string
}

const familyRows = ref<ProtocolFamilyDefinition[]>([])
const decryptProfileRows = ref<ProtocolDecryptProfile[]>([])
const templateRows = ref<ProtocolTemplateDefinition[]>([])
const familyLoading = ref(false)
const decryptProfileLoading = ref(false)
const templateLoading = ref(false)
const familySaving = ref(false)
const decryptProfileSaving = ref(false)
const templateSaving = ref(false)
const familyBatchSubmitting = ref(false)
const decryptProfileBatchSubmitting = ref(false)
const previewLoading = ref(false)
const replayLoading = ref(false)
const templateReplayLoading = ref(false)
const familyErrorMessage = ref('')
const decryptProfileErrorMessage = ref('')
const templateErrorMessage = ref('')
const submittingKey = ref('')
const familyDetailLoadingKey = ref('')
const decryptProfileDetailLoadingKey = ref('')
const templateDetailLoadingKey = ref('')
const editingFamilyCode = ref('')
const editingDecryptProfileCode = ref('')
const editingTemplateCode = ref('')
const selectedFamilyIds = ref<IdType[]>([])
const selectedDecryptProfileIds = ref<IdType[]>([])
const previewResult = ref<ProtocolDecryptPreview | null>(null)
const replayResult = ref<ProtocolGovernanceReplay | null>(null)
const templateReplayResult = ref<ProtocolTemplateReplay | null>(null)

const familyDraft = reactive<ProtocolFamilyDraftForm>(createEmptyFamilyDraft())
const decryptProfileDraft = reactive<ProtocolDecryptProfileDraftForm>(createEmptyDecryptProfileDraft())
const previewDraft = reactive<ProtocolDecryptPreviewForm>(createEmptyPreviewDraft())
const templateDraft = reactive<ProtocolTemplateDraftForm>(createEmptyTemplateDraft())
const templateReplayDraft = reactive<ProtocolTemplateReplayForm>(createEmptyTemplateReplayDraft())

const publishedItemCount = computed(() => {
  const familyPublished = familyRows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length
  const profilePublished = decryptProfileRows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length
  const templatePublished = templateRows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length
  return familyPublished + profilePublished + templatePublished
})

const familyDraftModeTitle = computed(() =>
  editingFamilyCode.value ? `编辑协议族草稿 · ${editingFamilyCode.value}` : '新建协议族草稿'
)

const decryptProfileDraftModeTitle = computed(() =>
  editingDecryptProfileCode.value
    ? `编辑解密档案草稿 · ${editingDecryptProfileCode.value}`
    : '新建解密档案草稿'
)

const templateDraftModeTitle = computed(() =>
  editingTemplateCode.value ? `编辑协议模板草稿 · ${editingTemplateCode.value}` : '新建协议模板草稿'
)

onMounted(() => {
  void Promise.all([loadFamilies(), loadDecryptProfiles(), loadTemplates()])
})

function createEmptyFamilyDraft(): ProtocolFamilyDraftForm {
  return {
    familyCode: '',
    protocolCode: '',
    displayName: '',
    decryptProfileCode: '',
    signAlgorithm: '',
    normalizationStrategy: ''
  }
}

function createEmptyDecryptProfileDraft(): ProtocolDecryptProfileDraftForm {
  return {
    profileCode: '',
    algorithm: '',
    merchantSource: '',
    merchantKey: '',
    transformation: '',
    signatureSecret: ''
  }
}

function createEmptyPreviewDraft(): ProtocolDecryptPreviewForm {
  return {
    familyCode: '',
    protocolCode: '',
    appId: ''
  }
}

function createEmptyTemplateDraft(): ProtocolTemplateDraftForm {
  return {
    templateCode: '',
    familyCode: '',
    protocolCode: '',
    displayName: '',
    expressionJson: '',
    outputMappingJson: ''
  }
}

function createEmptyTemplateReplayDraft(): ProtocolTemplateReplayForm {
  return {
    templateCode: '',
    payloadJson: ''
  }
}

function versionLabel(
  draftVersionNo?: number | null,
  publishedVersionNo?: number | null,
  draftStatus?: string | null,
  publishedStatus?: string | null
) {
  return `v${draftVersionNo ?? '--'} / v${publishedVersionNo ?? '--'} · ${draftStatus || '--'} / ${publishedStatus || '未发布'}`
}

function trimRequired(value: string) {
  return value.trim()
}

function trimOptional(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

function sanitizeSelectedIds(value: IdType[]) {
  return value.filter((item) => item != null)
}

function retainPublishedSelectedIds<T extends { id?: IdType | null; publishedStatus?: string | null }>(
  rows: T[],
  selectedIds: IdType[]
) {
  const publishedIdSet = new Set(
    rows.filter((row) => row.publishedStatus === 'PUBLISHED').map((row) => row.id).filter((id): id is IdType => id != null)
  )
  return sanitizeSelectedIds(selectedIds).filter((id) => publishedIdSet.has(id))
}

function retainSelectedIds<T extends { id?: IdType | null }>(rows: T[], selectedIds: IdType[]) {
  const idSet = new Set(rows.map((row) => row.id).filter((id): id is IdType => id != null))
  return selectedIds.filter((id) => idSet.has(id))
}

function resolveBatchSubmitFeedback(
  result: ProtocolGovernanceBatchSubmitResult | null | undefined,
  fallbackSuccessMessage: string
) {
  const submittedCount = typeof result?.submittedCount === 'number' ? result.submittedCount : null
  const failedCount = typeof result?.failedCount === 'number' ? result.failedCount : null
  const failedItems = (result?.items ?? []).filter((item) => item.success === false)
  const hasFailure = (failedCount ?? 0) > 0 || failedItems.length > 0
  if (!hasFailure) {
    return {
      level: 'success' as const,
      message: submittedCount != null ? `批量提交成功 ${submittedCount} 项` : fallbackSuccessMessage
    }
  }
  const totalCount = typeof result?.totalCount === 'number' ? result.totalCount : null
  const resolvedFailedCount = failedCount ?? failedItems.length
  const resolvedSubmittedCount =
    submittedCount ?? (totalCount != null ? Math.max(totalCount - resolvedFailedCount, 0) : 0)
  const failedDetail = failedItems
    .map((item) => {
      const recordId = item.recordId != null ? String(item.recordId) : '--'
      const reason = item.errorMessage?.trim()
      return reason ? `${recordId}(${reason})` : recordId
    })
    .filter((item) => item)
    .slice(0, 3)
    .join('，')
  const detailSuffix = failedDetail ? `，失败项：${failedDetail}${failedItems.length > 3 ? ' 等' : ''}` : ''
  return {
    level: 'warning' as const,
    message: `批量提交部分成功 ${resolvedSubmittedCount} 项，失败 ${resolvedFailedCount} 项${detailSuffix}`
  }
}

function applyFamilyDraft(source?: Partial<ProtocolFamilyDefinition> | Partial<ProtocolFamilyDefinitionUpsertPayload> | null) {
  familyDraft.familyCode = source?.familyCode ?? ''
  familyDraft.protocolCode = source?.protocolCode ?? ''
  familyDraft.displayName = source?.displayName ?? ''
  familyDraft.decryptProfileCode = source?.decryptProfileCode ?? ''
  familyDraft.signAlgorithm = source?.signAlgorithm ?? ''
  familyDraft.normalizationStrategy = source?.normalizationStrategy ?? ''
  editingFamilyCode.value = source?.familyCode ?? ''
}

function resetFamilyDraft() {
  applyFamilyDraft(createEmptyFamilyDraft())
}

function applyDecryptProfileDraft(
  source?: Partial<ProtocolDecryptProfile> | Partial<ProtocolDecryptProfileUpsertPayload> | null
) {
  decryptProfileDraft.profileCode = source?.profileCode ?? ''
  decryptProfileDraft.algorithm = source?.algorithm ?? ''
  decryptProfileDraft.merchantSource = source?.merchantSource ?? ''
  decryptProfileDraft.merchantKey = source?.merchantKey ?? ''
  decryptProfileDraft.transformation = source?.transformation ?? ''
  decryptProfileDraft.signatureSecret = source?.signatureSecret ?? ''
  editingDecryptProfileCode.value = source?.profileCode ?? ''
}

function resetDecryptProfileDraft() {
  applyDecryptProfileDraft(createEmptyDecryptProfileDraft())
}

function applyTemplateDraft(
  source?: Partial<ProtocolTemplateDefinition> | Partial<ProtocolTemplateDefinitionUpsertPayload> | null
) {
  templateDraft.templateCode = source?.templateCode ?? ''
  templateDraft.familyCode = source?.familyCode ?? ''
  templateDraft.protocolCode = source?.protocolCode ?? ''
  templateDraft.displayName = source?.displayName ?? ''
  templateDraft.expressionJson = source?.expressionJson ?? ''
  templateDraft.outputMappingJson = source?.outputMappingJson ?? ''
  editingTemplateCode.value = source?.templateCode ?? ''
}

function resetTemplateDraft() {
  applyTemplateDraft(createEmptyTemplateDraft())
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
    selectedFamilyIds.value = retainSelectedIds(familyRows.value, selectedFamilyIds.value)
  } catch (error) {
    familyRows.value = []
    selectedFamilyIds.value = []
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
    selectedDecryptProfileIds.value = retainSelectedIds(
      decryptProfileRows.value,
      selectedDecryptProfileIds.value
    )
  } catch (error) {
    decryptProfileRows.value = []
    selectedDecryptProfileIds.value = []
    decryptProfileErrorMessage.value = resolveRequestErrorMessage(error, '解密档案加载失败')
  } finally {
    decryptProfileLoading.value = false
  }
}

async function loadTemplates() {
  templateLoading.value = true
  templateErrorMessage.value = ''
  try {
    const response = await pageProtocolTemplates({
      pageNum: 1,
      pageSize: 20
    })
    templateRows.value = response.data?.records ?? []
  } catch (error) {
    templateRows.value = []
    templateErrorMessage.value = resolveRequestErrorMessage(error, '协议模板加载失败')
  } finally {
    templateLoading.value = false
  }
}

async function handleSaveFamily() {
  const payload: ProtocolFamilyDefinitionUpsertPayload = {
    familyCode: trimRequired(familyDraft.familyCode),
    protocolCode: trimRequired(familyDraft.protocolCode),
    displayName: trimRequired(familyDraft.displayName),
    decryptProfileCode: trimOptional(familyDraft.decryptProfileCode),
    signAlgorithm: trimOptional(familyDraft.signAlgorithm),
    normalizationStrategy: trimOptional(familyDraft.normalizationStrategy)
  }
  if (!payload.familyCode || !payload.protocolCode || !payload.displayName) {
    ElMessage.error('协议族编码、协议编码和显示名称不能为空')
    return
  }
  familySaving.value = true
  try {
    const response = await saveProtocolFamily(payload)
    applyFamilyDraft(response.data ?? payload)
    ElMessage.success('协议族草稿已保存')
    await loadFamilies()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议族草稿保存失败'))
  } finally {
    familySaving.value = false
  }
}

async function handleSaveDecryptProfile() {
  const payload: ProtocolDecryptProfileUpsertPayload = {
    profileCode: trimRequired(decryptProfileDraft.profileCode),
    algorithm: trimRequired(decryptProfileDraft.algorithm),
    merchantSource: trimRequired(decryptProfileDraft.merchantSource),
    merchantKey: trimRequired(decryptProfileDraft.merchantKey),
    transformation: trimOptional(decryptProfileDraft.transformation),
    signatureSecret: trimOptional(decryptProfileDraft.signatureSecret)
  }
  if (!payload.profileCode || !payload.algorithm || !payload.merchantSource || !payload.merchantKey) {
    ElMessage.error('档案编码、算法、商户来源和商户键不能为空')
    return
  }
  decryptProfileSaving.value = true
  try {
    const response = await saveProtocolDecryptProfile(payload)
    applyDecryptProfileDraft(response.data ?? payload)
    ElMessage.success('解密档案草稿已保存')
    await loadDecryptProfiles()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '解密档案草稿保存失败'))
  } finally {
    decryptProfileSaving.value = false
  }
}

async function handleSaveTemplate() {
  const payload: ProtocolTemplateDefinitionUpsertPayload = {
    templateCode: trimRequired(templateDraft.templateCode),
    familyCode: trimRequired(templateDraft.familyCode),
    protocolCode: trimRequired(templateDraft.protocolCode),
    displayName: trimRequired(templateDraft.displayName),
    expressionJson: trimRequired(templateDraft.expressionJson),
    outputMappingJson: trimOptional(templateDraft.outputMappingJson)
  }
  if (
    !payload.templateCode ||
    !payload.familyCode ||
    !payload.protocolCode ||
    !payload.displayName ||
    !payload.expressionJson
  ) {
    ElMessage.error('模板编码、协议族编码、协议编码、显示名称和表达式 JSON 不能为空')
    return
  }
  templateSaving.value = true
  try {
    const response = await saveProtocolTemplate(payload)
    applyTemplateDraft(response.data ?? payload)
    ElMessage.success('协议模板草稿已保存')
    await loadTemplates()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议模板草稿保存失败'))
  } finally {
    templateSaving.value = false
  }
}

async function handlePreviewDecrypt() {
  previewLoading.value = true
  try {
    const response = await previewProtocolDecrypt({
      familyCode: trimOptional(previewDraft.familyCode),
      protocolCode: trimOptional(previewDraft.protocolCode),
      appId: trimOptional(previewDraft.appId)
    })
    previewResult.value = response.data ?? null
  } catch (error) {
    previewResult.value = null
    ElMessage.error(resolveRequestErrorMessage(error, '解密试算失败'))
  } finally {
    previewLoading.value = false
  }
}

async function handleReplayDecrypt() {
  replayLoading.value = true
  try {
    const response = await replayProtocolDecrypt({
      familyCode: trimOptional(previewDraft.familyCode),
      protocolCode: trimOptional(previewDraft.protocolCode),
      appId: trimOptional(previewDraft.appId)
    })
    replayResult.value = response.data ?? null
  } catch (error) {
    replayResult.value = null
    ElMessage.error(resolveRequestErrorMessage(error, '解密回放失败'))
  } finally {
    replayLoading.value = false
  }
}

async function handleReplayTemplate() {
  const payload: ProtocolTemplateReplayPayload = {
    templateCode: trimRequired(templateReplayDraft.templateCode),
    payloadJson: trimRequired(templateReplayDraft.payloadJson)
  }
  if (!payload.templateCode || !payload.payloadJson) {
    ElMessage.error('模板编码和载荷 JSON 不能为空')
    return
  }
  templateReplayLoading.value = true
  try {
    const response = await replayProtocolTemplate(payload)
    templateReplayResult.value = response.data ?? null
  } catch (error) {
    templateReplayResult.value = null
    ElMessage.error(resolveRequestErrorMessage(error, '模板回放失败'))
  } finally {
    templateReplayLoading.value = false
  }
}

async function handleLoadFamilyDetail(row: ProtocolFamilyDefinition) {
  if (row.id == null) {
    return
  }
  familyDetailLoadingKey.value = `family-${row.id}`
  try {
    const response = await getProtocolFamilyDetail(row.id)
    applyFamilyDraft(response.data ?? row)
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议族详情加载失败'))
  } finally {
    familyDetailLoadingKey.value = ''
  }
}

async function handleLoadDecryptProfileDetail(row: ProtocolDecryptProfile) {
  if (row.id == null) {
    return
  }
  decryptProfileDetailLoadingKey.value = `profile-${row.id}`
  try {
    const response = await getProtocolDecryptProfileDetail(row.id)
    applyDecryptProfileDraft(response.data ?? row)
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '解密档案详情加载失败'))
  } finally {
    decryptProfileDetailLoadingKey.value = ''
  }
}

async function handleLoadTemplateDetail(row: ProtocolTemplateDefinition) {
  if (row.id == null) {
    return
  }
  templateDetailLoadingKey.value = `template-${row.id}`
  try {
    const response = await getProtocolTemplateDetail(row.id)
    applyTemplateDraft(response.data ?? row)
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议模板详情加载失败'))
  } finally {
    templateDetailLoadingKey.value = ''
  }
}

async function handlePublishTemplate(row: ProtocolTemplateDefinition) {
  if (row.id == null) {
    return
  }
  submittingKey.value = `template-publish-${row.id}`
  try {
    await publishProtocolTemplate(row.id, {
      submitReason: `发布协议模板：${row.templateCode || '--'}`
    })
    ElMessage.success('协议模板已发布快照')
    await loadTemplates()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议模板发布失败'))
  } finally {
    submittingKey.value = ''
  }
}

async function handleSubmitFamilyBatchPublish() {
  const recordIds = sanitizeSelectedIds(selectedFamilyIds.value)
  if (recordIds.length === 0) {
    ElMessage.error('请先选择至少一条协议族定义记录')
    return
  }
  familyBatchSubmitting.value = true
  try {
    const response = await submitProtocolFamilyBatchPublish({
      recordIds,
      submitReason: '批量提交协议族定义发布审批'
    })
    const feedback = resolveBatchSubmitFeedback(response.data, '协议族定义批量发布审批已提交')
    if (feedback.level === 'warning') {
      ElMessage.warning(feedback.message)
    } else {
      ElMessage.success(feedback.message)
    }
    await loadFamilies()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议族定义批量发布审批提交失败'))
  } finally {
    familyBatchSubmitting.value = false
  }
}

async function handleSubmitFamilyBatchRollback() {
  const selectedIds = sanitizeSelectedIds(selectedFamilyIds.value)
  if (selectedIds.length === 0) {
    ElMessage.error('请先选择至少一条协议族定义记录')
    return
  }
  const recordIds = retainPublishedSelectedIds(familyRows.value, selectedIds)
  if (recordIds.length === 0) {
    ElMessage.error('仅已发布协议族定义支持回滚，请先选择已发布记录')
    return
  }
  familyBatchSubmitting.value = true
  try {
    const response = await submitProtocolFamilyBatchRollback({
      recordIds,
      submitReason: '批量提交协议族定义回滚审批'
    })
    const feedback = resolveBatchSubmitFeedback(response.data, '协议族定义批量回滚审批已提交')
    if (feedback.level === 'warning') {
      ElMessage.warning(feedback.message)
    } else {
      ElMessage.success(feedback.message)
    }
    await loadFamilies()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '协议族定义批量回滚审批提交失败'))
  } finally {
    familyBatchSubmitting.value = false
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

async function handleSubmitDecryptProfileBatchPublish() {
  const recordIds = sanitizeSelectedIds(selectedDecryptProfileIds.value)
  if (recordIds.length === 0) {
    ElMessage.error('请先选择至少一条解密档案记录')
    return
  }
  decryptProfileBatchSubmitting.value = true
  try {
    const response = await submitProtocolDecryptProfileBatchPublish({
      recordIds,
      submitReason: '批量提交解密档案发布审批'
    })
    const feedback = resolveBatchSubmitFeedback(response.data, '解密档案批量发布审批已提交')
    if (feedback.level === 'warning') {
      ElMessage.warning(feedback.message)
    } else {
      ElMessage.success(feedback.message)
    }
    await loadDecryptProfiles()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '解密档案批量发布审批提交失败'))
  } finally {
    decryptProfileBatchSubmitting.value = false
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

async function handleSubmitDecryptProfileBatchRollback() {
  const selectedIds = sanitizeSelectedIds(selectedDecryptProfileIds.value)
  if (selectedIds.length === 0) {
    ElMessage.error('请先选择至少一条解密档案记录')
    return
  }
  const recordIds = retainPublishedSelectedIds(decryptProfileRows.value, selectedIds)
  if (recordIds.length === 0) {
    ElMessage.error('仅已发布解密档案支持回滚，请先选择已发布记录')
    return
  }
  decryptProfileBatchSubmitting.value = true
  try {
    const response = await submitProtocolDecryptProfileBatchRollback({
      recordIds,
      submitReason: '批量提交解密档案回滚审批'
    })
    const feedback = resolveBatchSubmitFeedback(response.data, '解密档案批量回滚审批已提交')
    if (feedback.level === 'warning') {
      ElMessage.warning(feedback.message)
    } else {
      ElMessage.success(feedback.message)
    }
    await loadDecryptProfiles()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '解密档案批量回滚审批提交失败'))
  } finally {
    decryptProfileBatchSubmitting.value = false
  }
}
</script>

<style scoped>
.protocol-governance-workbench,
.protocol-governance-workbench__summary,
.protocol-governance-workbench__list,
.protocol-governance-workbench__form-grid,
.protocol-governance-workbench__editor,
.protocol-governance-workbench__preview,
.protocol-governance-workbench__field,
.protocol-governance-workbench__editor-copy {
  display: grid;
}

.protocol-governance-workbench {
  gap: 0.82rem;
}

.protocol-governance-workbench__notice,
.protocol-governance-workbench__summary-card,
.protocol-governance-workbench__item,
.protocol-governance-workbench__empty,
.protocol-governance-workbench__editor,
.protocol-governance-workbench__preview,
.protocol-governance-workbench__preview-result {
  padding: 0.88rem 0.96rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: 0.78rem;
  background: white;
}

.protocol-governance-workbench__notice,
.protocol-governance-workbench__summary-card,
.protocol-governance-workbench__item,
.protocol-governance-workbench__empty,
.protocol-governance-workbench__item-title,
.protocol-governance-workbench__editor,
.protocol-governance-workbench__preview,
.protocol-governance-workbench__preview-result {
  gap: 0.24rem;
}

.protocol-governance-workbench__notice strong,
.protocol-governance-workbench__summary-card strong,
.protocol-governance-workbench__item-title strong,
.protocol-governance-workbench__empty strong,
.protocol-governance-workbench__editor-copy strong,
.protocol-governance-workbench__preview-result strong {
  color: var(--text-heading);
}

.protocol-governance-workbench__notice p,
.protocol-governance-workbench__summary-card span,
.protocol-governance-workbench__item-title span,
.protocol-governance-workbench__item-side,
.protocol-governance-workbench__hint,
.protocol-governance-workbench__empty p,
.protocol-governance-workbench__editor-copy p,
.protocol-governance-workbench__preview-result p,
.protocol-governance-workbench__field span {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.56;
}

.protocol-governance-workbench__summary {
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.72rem;
}

.protocol-governance-workbench__editor,
.protocol-governance-workbench__preview {
  margin-bottom: 0.72rem;
  gap: 0.72rem;
}

.protocol-governance-workbench__editor-head,
.protocol-governance-workbench__item-head,
.protocol-governance-workbench__actions {
  display: flex;
  justify-content: space-between;
  gap: 0.72rem;
  align-items: flex-start;
}

.protocol-governance-workbench__form-grid {
  grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
  gap: 0.72rem;
}

.protocol-governance-workbench__form-grid--preview {
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
}

.protocol-governance-workbench__field {
  gap: 0.35rem;
}

.protocol-governance-workbench__field input {
  width: 100%;
  min-height: 2.35rem;
  padding: 0.58rem 0.72rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.62rem;
  background: var(--surface-muted);
  color: var(--text-primary);
  font: inherit;
  box-sizing: border-box;
}

.protocol-governance-workbench__field input:focus {
  outline: 2px solid color-mix(in srgb, var(--brand) 28%, white);
  outline-offset: 1px;
  border-color: color-mix(in srgb, var(--brand) 38%, var(--panel-border));
}

.protocol-governance-workbench__field textarea {
  width: 100%;
  min-height: 5.2rem;
  padding: 0.58rem 0.72rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.62rem;
  background: var(--surface-muted);
  color: var(--text-primary);
  font: inherit;
  box-sizing: border-box;
  resize: vertical;
}

.protocol-governance-workbench__field textarea:focus {
  outline: 2px solid color-mix(in srgb, var(--brand) 28%, white);
  outline-offset: 1px;
  border-color: color-mix(in srgb, var(--brand) 38%, var(--panel-border));
}

.protocol-governance-workbench__field--full {
  grid-column: 1 / -1;
}

.protocol-governance-workbench__list {
  gap: 0.72rem;
}

.protocol-governance-workbench__batch {
  display: flex;
  justify-content: space-between;
  gap: 0.72rem;
  align-items: center;
  margin-bottom: 0.72rem;
}

.protocol-governance-workbench__checkbox {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
}

.protocol-governance-workbench__actions {
  flex-wrap: wrap;
}

.protocol-governance-workbench__empty {
  margin-top: 0.12rem;
}

@media (max-width: 720px) {
  .protocol-governance-workbench__editor-head,
  .protocol-governance-workbench__item-head,
  .protocol-governance-workbench__actions,
  .protocol-governance-workbench__batch {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
