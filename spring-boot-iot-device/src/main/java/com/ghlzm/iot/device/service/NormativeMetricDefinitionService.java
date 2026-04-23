package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import java.util.List;

/**
 * 规范字段定义服务。
 */
public interface NormativeMetricDefinitionService {

    List<NormativeMetricDefinition> listByScenario(String scenarioCode);

    List<NormativeMetricDefinition> listActive();
}
