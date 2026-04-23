package com.ghlzm.iot.framework.protocol.template.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateSubmitDTO;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateUpsertDTO;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateDefinitionVO;

public interface ProtocolTemplateGovernanceService {

    PageResult<ProtocolTemplateDefinitionVO> pageTemplates(String keyword, String status, Long pageNum, Long pageSize);

    ProtocolTemplateDefinitionVO getTemplateDetail(Long templateId);

    ProtocolTemplateDefinitionVO saveTemplate(ProtocolTemplateUpsertDTO dto, Long operatorUserId);

    ProtocolTemplateDefinitionVO publishTemplate(Long templateId, Long operatorUserId, ProtocolTemplateSubmitDTO dto);
}
