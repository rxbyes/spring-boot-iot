import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProtocolGovernanceWorkbenchView from '@/views/ProtocolGovernanceWorkbenchView.vue'

const {
  mockPageProtocolFamilies,
  mockPageProtocolDecryptProfiles,
  mockGetProtocolFamilyDetail,
  mockGetProtocolDecryptProfileDetail,
  mockSaveProtocolFamily,
  mockSaveProtocolDecryptProfile,
  mockPreviewProtocolDecrypt,
  mockReplayProtocolDecrypt,
  mockSubmitProtocolFamilyPublish,
  mockSubmitProtocolFamilyRollback,
  mockSubmitProtocolFamilyBatchPublish,
  mockSubmitProtocolFamilyBatchRollback,
  mockSubmitProtocolDecryptProfilePublish,
  mockSubmitProtocolDecryptProfileRollback,
  mockSubmitProtocolDecryptProfileBatchPublish,
  mockSubmitProtocolDecryptProfileBatchRollback,
  mockMessageSuccess,
  mockMessageWarning,
  mockMessageError
} = vi.hoisted(() => ({
  mockPageProtocolFamilies: vi.fn(),
  mockPageProtocolDecryptProfiles: vi.fn(),
  mockGetProtocolFamilyDetail: vi.fn(),
  mockGetProtocolDecryptProfileDetail: vi.fn(),
  mockSaveProtocolFamily: vi.fn(),
  mockSaveProtocolDecryptProfile: vi.fn(),
  mockPreviewProtocolDecrypt: vi.fn(),
  mockReplayProtocolDecrypt: vi.fn(),
  mockSubmitProtocolFamilyPublish: vi.fn(),
  mockSubmitProtocolFamilyRollback: vi.fn(),
  mockSubmitProtocolFamilyBatchPublish: vi.fn(),
  mockSubmitProtocolFamilyBatchRollback: vi.fn(),
  mockSubmitProtocolDecryptProfilePublish: vi.fn(),
  mockSubmitProtocolDecryptProfileRollback: vi.fn(),
  mockSubmitProtocolDecryptProfileBatchPublish: vi.fn(),
  mockSubmitProtocolDecryptProfileBatchRollback: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageWarning: vi.fn(),
  mockMessageError: vi.fn()
}))

vi.mock('@/api/protocolGovernance', () => ({
  pageProtocolFamilies: mockPageProtocolFamilies,
  pageProtocolDecryptProfiles: mockPageProtocolDecryptProfiles,
  getProtocolFamilyDetail: mockGetProtocolFamilyDetail,
  getProtocolDecryptProfileDetail: mockGetProtocolDecryptProfileDetail,
  saveProtocolFamily: mockSaveProtocolFamily,
  saveProtocolDecryptProfile: mockSaveProtocolDecryptProfile,
  previewProtocolDecrypt: mockPreviewProtocolDecrypt,
  replayProtocolDecrypt: mockReplayProtocolDecrypt,
  submitProtocolFamilyPublish: mockSubmitProtocolFamilyPublish,
  submitProtocolFamilyRollback: mockSubmitProtocolFamilyRollback,
  submitProtocolFamilyBatchPublish: mockSubmitProtocolFamilyBatchPublish,
  submitProtocolFamilyBatchRollback: mockSubmitProtocolFamilyBatchRollback,
  submitProtocolDecryptProfilePublish: mockSubmitProtocolDecryptProfilePublish,
  submitProtocolDecryptProfileRollback: mockSubmitProtocolDecryptProfileRollback,
  submitProtocolDecryptProfileBatchPublish: mockSubmitProtocolDecryptProfileBatchPublish,
  submitProtocolDecryptProfileBatchRollback: mockSubmitProtocolDecryptProfileBatchRollback
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: mockMessageSuccess,
    warning: mockMessageWarning,
    error: mockMessageError
  }
}))

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['title', 'description'],
  template: `
    <section class="protocol-governance-page-shell">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="protocol-governance-panel">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountView() {
  return mount(ProtocolGovernanceWorkbenchView, {
    global: {
      stubs: {
        IotAccessPageShell: IotAccessPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardButton: StandardButtonStub
      }
    }
  })
}

describe('ProtocolGovernanceWorkbenchView', () => {
  beforeEach(() => {
    mockPageProtocolFamilies.mockReset()
    mockPageProtocolDecryptProfiles.mockReset()
    mockGetProtocolFamilyDetail.mockReset()
    mockGetProtocolDecryptProfileDetail.mockReset()
    mockSaveProtocolFamily.mockReset()
    mockSaveProtocolDecryptProfile.mockReset()
    mockPreviewProtocolDecrypt.mockReset()
    mockReplayProtocolDecrypt.mockReset()
    mockSubmitProtocolFamilyPublish.mockReset()
    mockSubmitProtocolFamilyRollback.mockReset()
    mockSubmitProtocolFamilyBatchPublish.mockReset()
    mockSubmitProtocolFamilyBatchRollback.mockReset()
    mockSubmitProtocolDecryptProfilePublish.mockReset()
    mockSubmitProtocolDecryptProfileRollback.mockReset()
    mockSubmitProtocolDecryptProfileBatchPublish.mockReset()
    mockSubmitProtocolDecryptProfileBatchRollback.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageWarning.mockReset()
    mockMessageError.mockReset()

    mockPageProtocolFamilies.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9101,
            familyCode: 'legacy-dp-crack',
            protocolCode: 'mqtt-json',
            displayName: '裂缝 legacy $dp',
            status: 'DRAFT',
            versionNo: 2,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 1
          }
        ]
      }
    })
    mockPageProtocolDecryptProfiles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9201,
            profileCode: 'des-62000001',
            algorithm: 'DES',
            merchantSource: 'tenant-default',
            status: 'DRAFT',
            versionNo: 3,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 2
          }
        ]
      }
    })
    mockSaveProtocolFamily.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9101,
        familyCode: 'legacy-dp-crack',
        protocolCode: 'mqtt-json',
        displayName: '裂缝 legacy $dp',
        decryptProfileCode: 'des-62000001',
        signAlgorithm: 'MD5',
        normalizationStrategy: 'LEGACY_DP',
        status: 'DRAFT',
        versionNo: 3,
        publishedStatus: 'PUBLISHED',
        publishedVersionNo: 1
      }
    })
    mockGetProtocolFamilyDetail.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9101,
        familyCode: 'legacy-dp-crack',
        protocolCode: 'mqtt-json',
        displayName: '裂缝 legacy $dp 详情',
        decryptProfileCode: 'des-62000001',
        signAlgorithm: 'MD5',
        normalizationStrategy: 'LEGACY_DP',
        status: 'DRAFT',
        versionNo: 3,
        publishedStatus: 'PUBLISHED',
        publishedVersionNo: 1
      }
    })
    mockGetProtocolDecryptProfileDetail.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9201,
        profileCode: 'des-62000001',
        algorithm: 'DES',
        merchantSource: 'tenant-default',
        merchantKey: '62000001',
        transformation: 'DES/ECB/PKCS5Padding',
        signatureSecret: 'sig-001',
        status: 'DRAFT',
        versionNo: 3,
        publishedStatus: 'PUBLISHED',
        publishedVersionNo: 2
      }
    })
    mockSaveProtocolDecryptProfile.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9301,
        profileCode: 'aes-62000002',
        algorithm: 'AES',
        merchantSource: 'IOT_PROTOCOL_CRYPTO',
        merchantKey: 'aes-62000002',
        transformation: 'AES/CBC/PKCS5Padding',
        signatureSecret: 'sig-002',
        status: 'DRAFT',
        versionNo: 1,
        publishedStatus: null,
        publishedVersionNo: null
      }
    })
    mockPreviewProtocolDecrypt.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        matched: true,
        hitSource: 'FAMILY_DRAFT',
        familyCode: 'legacy-dp-crack',
        resolvedProfileCode: 'des-62000001',
        algorithm: 'DES',
        merchantSource: 'tenant-default',
        merchantKey: '62000001',
        transformation: 'DES/ECB/PKCS5Padding'
      }
    })
    mockReplayProtocolDecrypt.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        matched: true,
        hitSource: 'FAMILY_DRAFT',
        familyCode: 'legacy-dp-crack',
        protocolCode: 'mqtt-json',
        appId: '62000001',
        resolvedProfileCode: 'des-62000001',
        algorithm: 'DES',
        merchantSource: 'tenant-default',
        merchantKey: '62000001',
        transformation: 'DES/ECB/PKCS5Padding'
      }
    })
    mockSubmitProtocolFamilyBatchPublish.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        submittedCount: 1,
        failedCount: 0,
        items: [{ recordId: 9101, success: true, approvalOrderId: 99101 }]
      }
    })
  })

  it('shows families and decrypt profiles under the access workbench without replacing /device-access', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('协议治理工作台')
    expect(wrapper.text()).toContain('legacy-dp-crack')
    expect(wrapper.text()).toContain('des-62000001')
    expect(wrapper.text()).toContain('协议族定义')
    expect(wrapper.text()).toContain('解密档案')
  })

  it('loads a family row into the draft editor and saves the updated draft', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-family-edit-9101"]').trigger('click')
    expect((wrapper.get('[data-testid="protocol-family-family-code"]').element as HTMLInputElement).value).toBe(
      'legacy-dp-crack'
    )

    await wrapper.get('[data-testid="protocol-family-display-name"]').setValue('裂缝 legacy $dp v2')
    await wrapper.get('[data-testid="protocol-family-save"]').trigger('click')
    await flushPromises()

    expect(mockSaveProtocolFamily).toHaveBeenCalledWith({
      familyCode: 'legacy-dp-crack',
      protocolCode: 'mqtt-json',
      displayName: '裂缝 legacy $dp v2',
      decryptProfileCode: undefined,
      signAlgorithm: undefined,
      normalizationStrategy: undefined
    })
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('loads family detail before applying it to the draft editor', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-family-detail-9101"]').trigger('click')
    await flushPromises()

    expect(mockGetProtocolFamilyDetail).toHaveBeenCalledWith(9101)
    expect((wrapper.get('[data-testid="protocol-family-display-name"]').element as HTMLInputElement).value).toBe(
      '裂缝 legacy $dp 详情'
    )
  })

  it('supports batch family publish submit from selected rows', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-family-select-9101"]').setValue(true)
    await wrapper.get('[data-testid="protocol-family-batch-submit-publish"]').trigger('click')
    await flushPromises()

    expect(mockSubmitProtocolFamilyBatchPublish).toHaveBeenCalledWith({
      recordIds: [9101],
      submitReason: '批量提交协议族定义发布审批'
    })
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('filters non-published families out of batch rollback submit', async () => {
    mockPageProtocolFamilies.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9101,
            familyCode: 'legacy-dp-crack',
            protocolCode: 'mqtt-json',
            displayName: '裂缝 legacy $dp',
            status: 'DRAFT',
            versionNo: 2,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 1
          },
          {
            id: 9102,
            familyCode: 'legacy-dp-gnss',
            protocolCode: 'mqtt-json',
            displayName: 'GNSS legacy $dp',
            status: 'DRAFT',
            versionNo: 1,
            publishedStatus: 'DRAFT',
            publishedVersionNo: null
          }
        ]
      }
    })
    mockSubmitProtocolFamilyBatchRollback.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        submittedCount: 1,
        failedCount: 0,
        items: [{ recordId: 9101, success: true, approvalOrderId: 99102 }]
      }
    })
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-family-select-9101"]').setValue(true)
    await wrapper.get('[data-testid="protocol-family-select-9102"]').setValue(true)
    await wrapper.get('[data-testid="protocol-family-batch-submit-rollback"]').trigger('click')
    await flushPromises()

    expect(mockSubmitProtocolFamilyBatchRollback).toHaveBeenCalledWith({
      recordIds: [9101],
      submitReason: '批量提交协议族定义回滚审批'
    })
  })

  it('enforces published-only constraint for decrypt profile batch rollback', async () => {
    mockPageProtocolDecryptProfiles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9201,
            profileCode: 'des-62000001',
            algorithm: 'DES',
            merchantSource: 'tenant-default',
            status: 'DRAFT',
            versionNo: 3,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 2
          },
          {
            id: 9202,
            profileCode: 'aes-62000002',
            algorithm: 'AES',
            merchantSource: 'tenant-default',
            status: 'DRAFT',
            versionNo: 1,
            publishedStatus: 'DRAFT',
            publishedVersionNo: null
          }
        ]
      }
    })
    mockSubmitProtocolDecryptProfileBatchRollback.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        submittedCount: 1,
        failedCount: 0,
        items: [{ recordId: 9201, success: true, approvalOrderId: 99201 }]
      }
    })
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-profile-select-9201"]').setValue(true)
    await wrapper.get('[data-testid="protocol-profile-select-9202"]').setValue(true)
    await wrapper.get('[data-testid="protocol-profile-batch-submit-rollback"]').trigger('click')
    await flushPromises()

    expect(mockSubmitProtocolDecryptProfileBatchRollback).toHaveBeenCalledWith({
      recordIds: [9201],
      submitReason: '批量提交解密档案回滚审批'
    })

    mockSubmitProtocolDecryptProfileBatchRollback.mockReset()
    mockMessageError.mockReset()

    await wrapper.get('[data-testid="protocol-profile-select-9201"]').setValue(false)
    await wrapper.get('[data-testid="protocol-profile-batch-submit-rollback"]').trigger('click')
    await flushPromises()

    expect(mockSubmitProtocolDecryptProfileBatchRollback).not.toHaveBeenCalled()
    expect(mockMessageError).toHaveBeenCalled()
  })

  it('shows partial failure details for decrypt profile batch publish', async () => {
    mockPageProtocolDecryptProfiles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9201,
            profileCode: 'des-62000001',
            algorithm: 'DES',
            merchantSource: 'tenant-default',
            status: 'DRAFT',
            versionNo: 3,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 2
          },
          {
            id: 9202,
            profileCode: 'aes-62000002',
            algorithm: 'AES',
            merchantSource: 'tenant-default',
            status: 'DRAFT',
            versionNo: 1,
            publishedStatus: 'DRAFT',
            publishedVersionNo: null
          }
        ]
      }
    })
    mockSubmitProtocolDecryptProfileBatchPublish.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 2,
        submittedCount: 1,
        failedCount: 1,
        items: [
          { recordId: 9201, success: true, approvalOrderId: 99211 },
          { recordId: 9202, success: false, errorMessage: '当前记录不满足发布条件' }
        ]
      }
    })
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-profile-select-9201"]').setValue(true)
    await wrapper.get('[data-testid="protocol-profile-select-9202"]').setValue(true)
    await wrapper.get('[data-testid="protocol-profile-batch-submit-publish"]').trigger('click')
    await flushPromises()

    expect(mockSubmitProtocolDecryptProfileBatchPublish).toHaveBeenCalledWith({
      recordIds: [9201, 9202],
      submitReason: '批量提交解密档案发布审批'
    })
    expect(mockMessageWarning).toHaveBeenCalledWith(
      expect.stringContaining('批量提交部分成功 1 项，失败 1 项')
    )
    expect(mockMessageWarning).toHaveBeenCalledWith(expect.stringContaining('9202'))
    expect(mockMessageSuccess).not.toHaveBeenCalledWith(expect.stringContaining('批量提交成功 1 项'))
  })

  it('saves a decrypt profile draft and previews decrypt hit from the same workbench', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-profile-profile-code"]').setValue('aes-62000002')
    await wrapper.get('[data-testid="protocol-profile-algorithm"]').setValue('AES')
    await wrapper.get('[data-testid="protocol-profile-merchant-source"]').setValue('IOT_PROTOCOL_CRYPTO')
    await wrapper.get('[data-testid="protocol-profile-merchant-key"]').setValue('aes-62000002')
    await wrapper.get('[data-testid="protocol-profile-transformation"]').setValue('AES/CBC/PKCS5Padding')
    await wrapper.get('[data-testid="protocol-profile-signature-secret"]').setValue('sig-002')
    await wrapper.get('[data-testid="protocol-profile-save"]').trigger('click')
    await flushPromises()

    expect(mockSaveProtocolDecryptProfile).toHaveBeenCalledWith({
      profileCode: 'aes-62000002',
      algorithm: 'AES',
      merchantSource: 'IOT_PROTOCOL_CRYPTO',
      merchantKey: 'aes-62000002',
      transformation: 'AES/CBC/PKCS5Padding',
      signatureSecret: 'sig-002'
    })

    await wrapper.get('[data-testid="protocol-preview-family-code"]').setValue('legacy-dp-crack')
    await wrapper.get('[data-testid="protocol-preview-protocol-code"]').setValue('mqtt-json')
    await wrapper.get('[data-testid="protocol-preview-app-id"]').setValue('62000001')
    await wrapper.get('[data-testid="protocol-preview-submit"]').trigger('click')
    await flushPromises()

    expect(mockPreviewProtocolDecrypt).toHaveBeenCalledWith({
      familyCode: 'legacy-dp-crack',
      protocolCode: 'mqtt-json',
      appId: '62000001'
    })
    expect(wrapper.text()).toContain('FAMILY_DRAFT')
    expect(wrapper.text()).toContain('des-62000001')
  })

  it('runs replay through dedicated replay endpoint', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="protocol-preview-family-code"]').setValue('legacy-dp-crack')
    await wrapper.get('[data-testid="protocol-preview-protocol-code"]').setValue('mqtt-json')
    await wrapper.get('[data-testid="protocol-preview-app-id"]').setValue('62000001')
    await wrapper.get('[data-testid="protocol-replay-submit"]').trigger('click')
    await flushPromises()

    expect(mockReplayProtocolDecrypt).toHaveBeenCalledWith({
      familyCode: 'legacy-dp-crack',
      protocolCode: 'mqtt-json',
      appId: '62000001'
    })
    expect(wrapper.text()).toContain('FAMILY_DRAFT')
  })
})
