package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import org.springframework.stereotype.Service;

/**
 * 契约发布批次服务实现。
 */
@Service
public class ProductContractReleaseServiceImpl implements ProductContractReleaseService {

    private final ProductContractReleaseBatchMapper releaseBatchMapper;

    public ProductContractReleaseServiceImpl(ProductContractReleaseBatchMapper releaseBatchMapper) {
        this.releaseBatchMapper = releaseBatchMapper;
    }

    @Override
    public Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId) {
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(IdWorker.getId());
        batch.setProductId(productId);
        batch.setScenarioCode(scenarioCode);
        batch.setReleaseSource(releaseSource);
        batch.setReleasedFieldCount(releasedFieldCount);
        batch.setCreateBy(operatorId);
        releaseBatchMapper.insert(batch);
        return batch.getId();
    }
}
