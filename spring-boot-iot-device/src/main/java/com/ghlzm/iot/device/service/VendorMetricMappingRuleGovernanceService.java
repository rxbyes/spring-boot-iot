package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.dto.VendorMetricMappingRuleHitPreviewDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRulePublishSubmitDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleRollbackSubmitDTO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleHitPreviewVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleLedgerRowVO;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;

/**
 * 厂商字段映射规则治理服务。
 */
public interface VendorMetricMappingRuleGovernanceService {

    GovernanceSubmissionResultVO submitPublish(Long productId,
                                               Long ruleId,
                                               Long operatorUserId,
                                               VendorMetricMappingRulePublishSubmitDTO dto);

    GovernanceSubmissionResultVO submitRollback(Long productId,
                                                Long ruleId,
                                                Long operatorUserId,
                                                VendorMetricMappingRuleRollbackSubmitDTO dto);

    VendorMetricMappingRuleLedgerRowVO getLedgerRow(Long productId, Long ruleId);

    VendorMetricMappingRuleHitPreviewVO previewHit(Long productId, VendorMetricMappingRuleHitPreviewDTO dto);
}
