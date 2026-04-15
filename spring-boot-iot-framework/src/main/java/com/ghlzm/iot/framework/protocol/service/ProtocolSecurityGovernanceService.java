package com.ghlzm.iot.framework.protocol.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptPreviewDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptProfileUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolFamilyDefinitionUpsertDTO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptPreviewVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptProfileVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolFamilyDefinitionVO;

public interface ProtocolSecurityGovernanceService {

    PageResult<ProtocolFamilyDefinitionVO> pageFamilies(String keyword, String status, Long pageNum, Long pageSize);

    PageResult<ProtocolDecryptProfileVO> pageDecryptProfiles(String keyword, String status, Long pageNum, Long pageSize);

    ProtocolFamilyDefinitionVO saveFamily(ProtocolFamilyDefinitionUpsertDTO dto, Long operatorUserId);

    ProtocolDecryptProfileVO saveDecryptProfile(ProtocolDecryptProfileUpsertDTO dto, Long operatorUserId);

    ProtocolDecryptPreviewVO previewDecrypt(ProtocolDecryptPreviewDTO dto);
}
