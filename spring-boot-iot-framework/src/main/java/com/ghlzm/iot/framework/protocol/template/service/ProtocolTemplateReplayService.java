package com.ghlzm.iot.framework.protocol.template.service;

import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateReplayDTO;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateReplayVO;

public interface ProtocolTemplateReplayService {

    ProtocolTemplateReplayVO replay(ProtocolTemplateReplayDTO dto);
}
