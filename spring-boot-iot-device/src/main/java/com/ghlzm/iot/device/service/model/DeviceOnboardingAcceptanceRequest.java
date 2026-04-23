package com.ghlzm.iot.device.service.model;

/**
 * 接入案例验收执行上下文。
 */
public record DeviceOnboardingAcceptanceRequest(Long caseId,
                                                Long tenantId,
                                                String caseCode,
                                                String caseName,
                                                Long productId,
                                                Long releaseBatchId,
                                                String deviceCode,
                                                String protocolFamilyCode,
                                                String decryptProfileCode,
                                                String protocolTemplateCode) {
}
