package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.util.List;
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

    @Override
    public PageResult<ProductContractReleaseBatchVO> pageBatches(Long productId, Long pageNum, Long pageSize) {
        Page<ProductContractReleaseBatch> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<ProductContractReleaseBatch> result = releaseBatchMapper.selectPage(page, new LambdaQueryWrapper<ProductContractReleaseBatch>()
                .eq(productId != null, ProductContractReleaseBatch::getProductId, productId)
                .orderByDesc(ProductContractReleaseBatch::getCreateTime)
                .orderByDesc(ProductContractReleaseBatch::getId));
        List<ProductContractReleaseBatchVO> records = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public ProductContractReleaseBatchVO getBatch(Long batchId) {
        ProductContractReleaseBatch batch = releaseBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("契约发布批次不存在: " + batchId);
        }
        return toVO(batch);
    }

    private ProductContractReleaseBatchVO toVO(ProductContractReleaseBatch batch) {
        ProductContractReleaseBatchVO vo = new ProductContractReleaseBatchVO();
        vo.setId(batch.getId());
        vo.setProductId(batch.getProductId());
        vo.setScenarioCode(batch.getScenarioCode());
        vo.setReleaseSource(batch.getReleaseSource());
        vo.setReleasedFieldCount(batch.getReleasedFieldCount());
        vo.setCreateBy(batch.getCreateBy());
        vo.setCreateTime(batch.getCreateTime());
        return vo;
    }
}
