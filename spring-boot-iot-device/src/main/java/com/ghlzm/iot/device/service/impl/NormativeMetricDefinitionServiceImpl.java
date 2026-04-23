package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.mapper.NormativeMetricDefinitionMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 规范字段定义服务实现。
 */
@Service
public class NormativeMetricDefinitionServiceImpl implements NormativeMetricDefinitionService {

    private final NormativeMetricDefinitionMapper mapper;

    public NormativeMetricDefinitionServiceImpl(NormativeMetricDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<NormativeMetricDefinition> listByScenario(String scenarioCode) {
        if (!StringUtils.hasText(scenarioCode)) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<NormativeMetricDefinition>()
                .eq(NormativeMetricDefinition::getDeleted, 0)
                .eq(NormativeMetricDefinition::getScenarioCode, scenarioCode)
                .orderByAsc(NormativeMetricDefinition::getIdentifier));
    }

    @Override
    public List<NormativeMetricDefinition> listActive() {
        return mapper.selectList(new LambdaQueryWrapper<NormativeMetricDefinition>()
                .eq(NormativeMetricDefinition::getDeleted, 0)
                .orderByAsc(NormativeMetricDefinition::getScenarioCode)
                .orderByAsc(NormativeMetricDefinition::getIdentifier));
    }
}
