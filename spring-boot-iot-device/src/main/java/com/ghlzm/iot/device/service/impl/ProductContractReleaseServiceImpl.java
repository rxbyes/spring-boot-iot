package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductContractReleaseSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contract release batch service implementation.
 */
@Service
public class ProductContractReleaseServiceImpl implements ProductContractReleaseService {

    static final String SNAPSHOT_STAGE_BEFORE_APPLY = "BEFORE_APPLY";
    static final String SNAPSHOT_STAGE_AFTER_APPLY = "AFTER_APPLY";

    private final ProductContractReleaseBatchMapper releaseBatchMapper;
    private final ProductContractReleaseSnapshotMapper releaseSnapshotMapper;
    private final ProductModelMapper productModelMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public ProductContractReleaseServiceImpl(ProductContractReleaseBatchMapper releaseBatchMapper,
                                             ProductContractReleaseSnapshotMapper releaseSnapshotMapper,
                                             ProductModelMapper productModelMapper) {
        this.releaseBatchMapper = releaseBatchMapper;
        this.releaseSnapshotMapper = releaseSnapshotMapper;
        this.productModelMapper = productModelMapper;
    }

    @Override
    public Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId) {
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(IdWorker.getId());
        batch.setProductId(productId);
        batch.setScenarioCode(scenarioCode);
        batch.setReleaseSource(releaseSource);
        batch.setReleasedFieldCount(releasedFieldCount);
        batch.setCreateBy(normalizeOperatorId(operatorId));
        releaseBatchMapper.insert(batch);
        return batch.getId();
    }

    @Override
    public void saveBatchSnapshot(Long batchId,
                                  Long productId,
                                  String snapshotStage,
                                  String snapshotJson,
                                  Long operatorId) {
        if (batchId == null || productId == null || !StringUtils.hasText(snapshotStage) || !StringUtils.hasText(snapshotJson)) {
            return;
        }
        ProductContractReleaseSnapshot snapshot = new ProductContractReleaseSnapshot();
        snapshot.setId(IdWorker.getId());
        snapshot.setBatchId(batchId);
        snapshot.setProductId(productId);
        snapshot.setSnapshotStage(snapshotStage.trim().toUpperCase());
        snapshot.setSnapshotJson(snapshotJson);
        snapshot.setCreateBy(normalizeOperatorId(operatorId));
        releaseSnapshotMapper.insert(snapshot);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductContractReleaseRollbackResultVO rollbackLatestBatch(Long batchId, Long operatorId) {
        ProductContractReleaseBatch target = releaseBatchMapper.selectById(batchId);
        if (target == null) {
            throw new BizException("契约发布批次不存在: " + batchId);
        }
        if (target.getRollbackTime() != null) {
            throw new BizException("目标批次已回滚: " + batchId);
        }
        List<ProductContractReleaseBatch> batches = releaseBatchMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getProductId, target.getProductId())
                        .isNull(ProductContractReleaseBatch::getRollbackTime)
                        .orderByDesc(ProductContractReleaseBatch::getCreateTime)
                        .orderByDesc(ProductContractReleaseBatch::getId)
        );
        if (batches == null || batches.isEmpty()) {
            throw new BizException("未找到可回滚的契约发布批次");
        }
        ProductContractReleaseBatch latest = batches.get(0);
        if (!latest.getId().equals(batchId)) {
            throw new BizException("仅支持回滚当前产品最新发布批次");
        }

        ProductContractReleaseSnapshot beforeSnapshot = loadBatchSnapshot(batchId, SNAPSHOT_STAGE_BEFORE_APPLY);
        int restoredFieldCount = restoreProductModels(target.getProductId(), beforeSnapshot.getSnapshotJson());

        target.setRollbackBy(normalizeOperatorId(operatorId));
        target.setRollbackTime(LocalDateTime.now());
        int affectedRows = releaseBatchMapper.updateById(target);
        if (affectedRows <= 0) {
            throw new BizException("契约发布批次回滚失败，请稍后重试");
        }

        ProductContractReleaseRollbackResultVO result = new ProductContractReleaseRollbackResultVO();
        result.setRolledBackBatchId(target.getId());
        result.setProductId(target.getProductId());
        result.setScenarioCode(target.getScenarioCode());
        result.setReleaseSource(target.getReleaseSource());
        result.setReleasedFieldCount(target.getReleasedFieldCount());
        result.setRestoredFieldCount(restoredFieldCount);
        result.setRollbackMode("SNAPSHOT_FIELD_RESTORE");
        result.setRollbackLimitations("当前仅支持回滚当前产品最新发布批次，且依赖发布前快照恢复合同字段。");
        result.setRollbackTime(target.getRollbackTime());
        return result;
    }

    private ProductContractReleaseSnapshot loadBatchSnapshot(Long batchId, String snapshotStage) {
        List<ProductContractReleaseSnapshot> snapshots = releaseSnapshotMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseSnapshot>()
                        .eq(ProductContractReleaseSnapshot::getBatchId, batchId)
                        .eq(ProductContractReleaseSnapshot::getSnapshotStage, snapshotStage)
                        .orderByDesc(ProductContractReleaseSnapshot::getCreateTime)
                        .orderByDesc(ProductContractReleaseSnapshot::getId)
                        .last("limit 1")
        );
        if (snapshots == null || snapshots.isEmpty()) {
            throw new BizException("发布批次缺少快照: " + snapshotStage);
        }
        ProductContractReleaseSnapshot snapshot = snapshots.get(0);
        if (!StringUtils.hasText(snapshot.getSnapshotJson())) {
            throw new BizException("发布批次快照为空: " + snapshotStage);
        }
        return snapshot;
    }

    private int restoreProductModels(Long productId, String snapshotJson) {
        if (productId == null) {
            return 0;
        }
        List<ReleaseModelSnapshotItem> snapshotItems = parseSnapshotItems(snapshotJson);
        List<ProductModel> activeModels = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getProductId, productId)
                .eq(ProductModel::getDeleted, 0));
        Map<String, ProductModel> activeByKey = new LinkedHashMap<>();
        for (ProductModel model : activeModels) {
            String key = buildModelKey(model.getModelType(), model.getIdentifier());
            if (StringUtils.hasText(key) && !activeByKey.containsKey(key)) {
                activeByKey.put(key, model);
            }
        }

        Set<String> retainedKeys = new LinkedHashSet<>();
        int restoredCount = 0;
        for (ReleaseModelSnapshotItem snapshotItem : snapshotItems) {
            String key = buildModelKey(snapshotItem.modelType(), snapshotItem.identifier());
            if (!StringUtils.hasText(key) || retainedKeys.contains(key)) {
                continue;
            }
            ProductModel existing = activeByKey.get(key);
            if (existing == null) {
                ProductModel model = new ProductModel();
                model.setProductId(productId);
                applySnapshot(model, snapshotItem);
                productModelMapper.insert(model);
            } else {
                applySnapshot(existing, snapshotItem);
                existing.setDeleted(0);
                productModelMapper.updateById(existing);
            }
            retainedKeys.add(key);
            restoredCount++;
        }

        for (Map.Entry<String, ProductModel> entry : activeByKey.entrySet()) {
            if (!retainedKeys.contains(entry.getKey()) && entry.getValue() != null && entry.getValue().getId() != null) {
                productModelMapper.hardDeleteById(entry.getValue().getId());
            }
        }
        return restoredCount;
    }

    private List<ReleaseModelSnapshotItem> parseSnapshotItems(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return List.of();
        }
        try {
            List<ReleaseModelSnapshotItem> parsed = objectMapper.readValue(
                    snapshotJson,
                    new TypeReference<List<ReleaseModelSnapshotItem>>() {
                    }
            );
            if (parsed == null || parsed.isEmpty()) {
                return List.of();
            }
            List<ReleaseModelSnapshotItem> result = new ArrayList<>();
            for (ReleaseModelSnapshotItem item : parsed) {
                if (item == null) {
                    continue;
                }
                if (!StringUtils.hasText(item.modelType()) || !StringUtils.hasText(item.identifier())) {
                    continue;
                }
                result.add(item);
            }
            result.sort(Comparator
                    .comparing(ReleaseModelSnapshotItem::modelType)
                    .thenComparing(ReleaseModelSnapshotItem::identifier));
            return result;
        } catch (Exception ex) {
            throw new BizException("发布批次快照格式无效，无法回滚");
        }
    }

    private void applySnapshot(ProductModel model, ReleaseModelSnapshotItem snapshot) {
        model.setModelType(normalize(snapshot.modelType()));
        model.setIdentifier(normalize(snapshot.identifier()));
        model.setModelName(normalize(snapshot.modelName()));
        model.setDataType(normalize(snapshot.dataType()));
        model.setSpecsJson(normalize(snapshot.specsJson()));
        model.setEventType(normalize(snapshot.eventType()));
        model.setServiceInputJson(normalize(snapshot.serviceInputJson()));
        model.setServiceOutputJson(normalize(snapshot.serviceOutputJson()));
        model.setSortNo(snapshot.sortNo());
        model.setRequiredFlag(snapshot.requiredFlag());
        model.setDescription(normalize(snapshot.description()));
    }

    private String buildModelKey(String modelType, String identifier) {
        String normalizedModelType = normalize(modelType);
        String normalizedIdentifier = normalize(identifier);
        if (!StringUtils.hasText(normalizedModelType) || !StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return normalizedModelType + "#" + normalizedIdentifier;
    }

    private Long normalizeOperatorId(Long operatorId) {
        return operatorId != null && operatorId > 0 ? operatorId : null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
        vo.setRollbackBy(batch.getRollbackBy());
        vo.setRollbackTime(batch.getRollbackTime());
        return vo;
    }

    record ReleaseModelSnapshotItem(
            String modelType,
            String identifier,
            String modelName,
            String dataType,
            String specsJson,
            String eventType,
            String serviceInputJson,
            String serviceOutputJson,
            Integer sortNo,
            Integer requiredFlag,
            String description
    ) {
    }
}
