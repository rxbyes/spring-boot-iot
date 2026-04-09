package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.vo.ProductModelProtocolTemplateEvidenceVO;
import java.util.Map;

/**
 * 产品治理 compare -> apply 之间的会话级模板回执存储。
 */
public interface ProductModelGovernanceReceiptStore {

    void replaceProtocolTemplateEvidence(Long productId,
                                         Map<String, ProductModelProtocolTemplateEvidenceVO> evidenceByIdentifier);

    Map<String, ProductModelProtocolTemplateEvidenceVO> loadProtocolTemplateEvidence(Long productId);
}
