package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogRebuildService;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 风险指标目录统一重建服务实现。
 */
@Service
public class RiskMetricCatalogRebuildServiceImpl implements RiskMetricCatalogRebuildService {

    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;
    private final ProductContractReleaseBatchMapper releaseBatchMapper;
    private final RiskMetricCatalogPublishRule publishRule;
    private final RiskMetricCatalogService riskMetricCatalogService;

    public RiskMetricCatalogRebuildServiceImpl(ProductMapper productMapper,
                                               ProductModelMapper productModelMapper,
                                               ProductContractReleaseBatchMapper releaseBatchMapper,
                                               RiskMetricCatalogPublishRule publishRule,
                                               RiskMetricCatalogService riskMetricCatalogService) {
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
        this.releaseBatchMapper = releaseBatchMapper;
        this.publishRule = publishRule;
        this.riskMetricCatalogService = riskMetricCatalogService;
    }

    @Override
    public boolean rebuildReleasedContracts(Long productId, Long releaseBatchId, List<ProductModel> releasedContracts) {
        if (productId == null || releasedContracts == null || releasedContracts.isEmpty()) {
            return false;
        }
        Product product = productMapper == null ? null : productMapper.selectById(productId);
        if (product == null || publishRule == null || riskMetricCatalogService == null) {
            return false;
        }
        Set<String> enabledIdentifiers = publishRule.resolveRiskEnabledIdentifiers(product, null, null, releasedContracts);
        riskMetricCatalogService.publishFromReleasedContracts(
                productId,
                releaseBatchId,
                releasedContracts,
                enabledIdentifiers == null ? Set.of() : new LinkedHashSet<>(enabledIdentifiers)
        );
        return true;
    }

    @Override
    public boolean rebuildLatestRelease(Long productId) {
        if (productId == null || productModelMapper == null) {
            return false;
        }
        ProductContractReleaseBatch latestBatch = loadLatestReleaseBatch(productId);
        List<ProductModel> releasedContracts = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, productId)
                .eq(ProductModel::getModelType, "property")
                .orderByAsc(ProductModel::getSortNo)
                .orderByAsc(ProductModel::getIdentifier));
        if (releasedContracts == null || releasedContracts.isEmpty()) {
            return false;
        }
        Long releaseBatchId = latestBatch == null ? null : latestBatch.getId();
        return rebuildReleasedContracts(productId, releaseBatchId, releasedContracts);
    }

    private ProductContractReleaseBatch loadLatestReleaseBatch(Long productId) {
        if (productId == null || releaseBatchMapper == null) {
            return null;
        }
        List<ProductContractReleaseBatch> batches = releaseBatchMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getDeleted, 0)
                        .eq(ProductContractReleaseBatch::getProductId, productId)
                        .orderByDesc(ProductContractReleaseBatch::getCreateTime)
                        .orderByDesc(ProductContractReleaseBatch::getId)
                        .last("limit 1")
        );
        return batches == null || batches.isEmpty() ? null : batches.get(0);
    }
}
