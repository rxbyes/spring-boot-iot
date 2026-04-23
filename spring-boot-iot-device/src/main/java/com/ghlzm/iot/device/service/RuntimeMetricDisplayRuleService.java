package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.RuntimeMetricDisplayRuleUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.vo.RuntimeMetricDisplayRuleVO;

/**
 * 运行态字段显示规则服务。
 */
public interface RuntimeMetricDisplayRuleService {

    PageResult<RuntimeMetricDisplayRuleVO> pageRules(Long productId, String status, Long pageNum, Long pageSize);

    RuntimeMetricDisplayRuleVO createAndGet(Long productId, Long operatorId, RuntimeMetricDisplayRuleUpsertDTO dto);

    RuntimeMetricDisplayRuleVO updateAndGet(Long productId, Long ruleId, Long operatorId, RuntimeMetricDisplayRuleUpsertDTO dto);

    DisplayResolution resolveForDisplay(Product product, String rawIdentifier);

    record DisplayResolution(Long ruleId, String scopeType, String displayName, String unit) {
    }
}
