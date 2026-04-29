package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportDTO;
import com.ghlzm.iot.device.vo.NormativeMetricDefinitionImportResultVO;
import java.util.List;

/**
 * 规范字段定义服务。
 */
public interface NormativeMetricDefinitionService {

    List<NormativeMetricDefinition> listByScenario(String scenarioCode);

    List<NormativeMetricDefinition> listActive();

    NormativeMetricDefinitionImportResultVO previewImport(NormativeMetricDefinitionImportDTO dto);

    NormativeMetricDefinitionImportResultVO applyImport(NormativeMetricDefinitionImportDTO dto);
}
