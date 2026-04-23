package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;

/**
 * 已发布产品合同快照服务。
 */
public interface PublishedProductContractSnapshotService {

    PublishedProductContractSnapshot getRequiredSnapshot(Long productId);
}
