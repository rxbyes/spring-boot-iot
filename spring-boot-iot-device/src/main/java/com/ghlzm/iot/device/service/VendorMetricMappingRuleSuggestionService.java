package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.vo.VendorMetricMappingRuleSuggestionVO;
import java.util.List;

/**
 * 厂商字段映射规则建议服务。
 */
public interface VendorMetricMappingRuleSuggestionService {

    List<VendorMetricMappingRuleSuggestionVO> listSuggestions(Long productId,
                                                              boolean includeCovered,
                                                              boolean includeIgnored,
                                                              int minEvidenceCount);
}
