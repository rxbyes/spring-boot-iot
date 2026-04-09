package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.dto.DeviceRelationUpsertDTO;
import com.ghlzm.iot.device.service.model.DeviceRelationRule;
import com.ghlzm.iot.device.vo.DeviceRelationVO;

import java.util.List;

/**
 * 设备关系服务。
 */
public interface DeviceRelationService {

    DeviceRelationVO createRelation(Long currentUserId, DeviceRelationUpsertDTO dto);

    DeviceRelationVO updateRelation(Long currentUserId, Long relationId, DeviceRelationUpsertDTO dto);

    void deleteRelation(Long currentUserId, Long relationId);

    List<DeviceRelationVO> listByParentDeviceCode(Long currentUserId, String parentDeviceCode);

    List<DeviceRelationRule> listEnabledRulesByParentDeviceCode(String parentDeviceCode);
}
