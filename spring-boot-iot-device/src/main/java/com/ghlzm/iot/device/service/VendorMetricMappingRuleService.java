package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleBatchStatusDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleReplayDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleReplayVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleVO;
import java.util.Map;

/**
 * 厂商字段映射规则服务。
 */
public interface VendorMetricMappingRuleService {

    PageResult<VendorMetricMappingRuleVO> pageRules(Long productId, String status, Long pageNum, Long pageSize);

    Long createRule(Long productId, Long operatorId, VendorMetricMappingRuleUpsertDTO dto);

    VendorMetricMappingRuleVO createAndGet(Long productId, Long operatorId, VendorMetricMappingRuleUpsertDTO dto);

    VendorMetricMappingRuleVO updateAndGet(Long productId, Long ruleId, Long operatorId, VendorMetricMappingRuleUpsertDTO dto);

    Map<String, Object> batchStatus(Long productId, Long operatorId, VendorMetricMappingRuleBatchStatusDTO dto);

    VendorMetricMappingRuleReplayVO replay(Long productId, VendorMetricMappingRuleReplayDTO dto);
}
