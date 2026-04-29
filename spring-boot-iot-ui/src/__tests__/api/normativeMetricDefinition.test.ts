import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  applyNormativeMetricImport,
  previewNormativeMetricImport
} from '@/api/normativeMetricDefinition'
import { request } from '@/api/request'
import type { NormativeMetricDefinitionImportPayload } from '@/types/api'

vi.mock('@/api/request', () => ({
  request: vi.fn()
}))

function importPayload(): NormativeMetricDefinitionImportPayload {
  return {
    items: [
      {
        scenarioCode: 'phase4-mud-level',
        deviceFamily: 'NF_MONITOR',
        identifier: 'value',
        displayName: '泥水位',
        unit: 'm',
        monitorContentCode: 'L4',
        monitorTypeCode: 'NW',
        metadataJson: {
          riskCategory: 'MUD_LEVEL'
        }
      }
    ]
  }
}

describe('normativeMetricDefinition api', () => {
  beforeEach(() => {
    vi.mocked(request).mockReset()
    vi.mocked(request).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        readyCount: 1,
        conflictCount: 0,
        appliedCount: 0,
        rows: []
      }
    })
  })

  it('posts preview import payload to the normative preview endpoint', async () => {
    const payload = importPayload()

    const response = await previewNormativeMetricImport(payload)

    expect(response.data?.readyCount).toBe(1)
    expect(request).toHaveBeenCalledWith('/api/device/normative-metrics/import/preview', {
      method: 'POST',
      body: payload
    })
  })

  it('posts apply import payload to the normative apply endpoint', async () => {
    const payload = importPayload()

    await applyNormativeMetricImport(payload)

    expect(request).toHaveBeenCalledWith('/api/device/normative-metrics/import/apply', {
      method: 'POST',
      body: payload
    })
  })

  it('propagates preview request errors without rewriting the backend message', async () => {
    const error = Object.assign(new Error('缺少产品契约治理权限'), {
      handled: true,
      status: 500,
      rawMessage: '缺少产品契约治理权限'
    })
    vi.mocked(request).mockRejectedValueOnce(error)

    await expect(previewNormativeMetricImport(importPayload())).rejects.toBe(error)
  })

  it('propagates apply request errors without swallowing conflict evidence', async () => {
    const error = Object.assign(new Error('规范字段导入存在冲突，请先预检并修正'), {
      handled: true,
      status: 500,
      rawMessage: '规范字段导入存在冲突，请先预检并修正'
    })
    vi.mocked(request).mockRejectedValueOnce(error)

    await expect(applyNormativeMetricImport(importPayload())).rejects.toBe(error)
  })
})
