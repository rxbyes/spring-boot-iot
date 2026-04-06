package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import java.util.List;

/**
 * 产品字段证据服务。
 */
public interface ProductMetricEvidenceService {

    void replaceManualEvidence(Long productId, String scenarioCode, List<VendorMetricEvidence> evidences);

    void captureRuntimeEvidence(Product product, DeviceUpMessage upMessage);
}
