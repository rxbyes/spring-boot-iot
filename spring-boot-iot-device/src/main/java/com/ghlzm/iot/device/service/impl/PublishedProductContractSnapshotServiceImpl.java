package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 已发布产品合同快照服务实现。
 */
@Service
public class PublishedProductContractSnapshotServiceImpl implements PublishedProductContractSnapshotService {

    private final ProductModelMapper productModelMapper;
    private final ProductContractReleaseBatchMapper releaseBatchMapper;

    public PublishedProductContractSnapshotServiceImpl(ProductModelMapper productModelMapper,
                                                       ProductContractReleaseBatchMapper releaseBatchMapper) {
        this.productModelMapper = productModelMapper;
        this.releaseBatchMapper = releaseBatchMapper;
    }

    @Override
    public PublishedProductContractSnapshot getRequiredSnapshot(Long productId) {
        if (productId == null) {
            return PublishedProductContractSnapshot.empty(null);
        }
        PublishedProductContractSnapshot.Builder builder = PublishedProductContractSnapshot.builder()
                .productId(productId)
                .releaseBatchId(resolveLatestReleasedBatchId(productId));
        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
                        .orderByAsc(ProductModel::getSortNo)
                        .orderByAsc(ProductModel::getIdentifier)
        );
        for (ProductModel productModel : productModels) {
            String identifier = productModel.getIdentifier();
            String canonicalIdentifier = toCanonicalIdentifier(identifier);
            if (canonicalIdentifier == null) {
                continue;
            }
            builder.publishedIdentifier(canonicalIdentifier);
            builder.canonicalAlias(identifier, canonicalIdentifier);
            builder.canonicalAlias(canonicalIdentifier, canonicalIdentifier);
        }
        return builder.build();
    }

    private Long resolveLatestReleasedBatchId(Long productId) {
        if (releaseBatchMapper == null || productId == null) {
            return null;
        }
        List<ProductContractReleaseBatch> batches = releaseBatchMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getProductId, productId)
                        .eq(ProductContractReleaseBatch::getReleaseStatus, ProductContractReleaseServiceImpl.RELEASE_STATUS_RELEASED)
                        .isNull(ProductContractReleaseBatch::getRollbackTime)
                        .orderByDesc(ProductContractReleaseBatch::getCreateTime)
                        .orderByDesc(ProductContractReleaseBatch::getId)
                        .last("limit 1")
        );
        if (batches == null || batches.isEmpty()) {
            return null;
        }
        return batches.get(0).getId();
    }

    private String toCanonicalIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int idx = trimmed.lastIndexOf('.');
        if (idx >= 0 && idx < trimmed.length() - 1) {
            return trimmed.substring(idx + 1);
        }
        return trimmed;
    }
}
