package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;

public interface DeviceOnboardingSuggestionService {

    DeviceOnboardingSuggestionVO suggest(Long tenantId, DeviceOnboardingSuggestionQuery query);
}
