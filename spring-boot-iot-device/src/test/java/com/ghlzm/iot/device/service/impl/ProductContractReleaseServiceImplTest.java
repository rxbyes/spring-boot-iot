package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductContractReleaseSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductContractReleaseServiceImplTest {

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    @Mock
    private ProductContractReleaseSnapshotMapper releaseSnapshotMapper;

    @Mock
    private ProductModelMapper productModelMapper;

    @Test
    void createBatchShouldPersistProductVersionMetadata() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
                releaseBatchMapper,
                releaseSnapshotMapper,
                productModelMapper
        );

        Long batchId = service.createBatch(1001L, "phase1-crack", "manual_compare_apply", 3, 10001L);

        assertNotNull(batchId);
        verify(releaseBatchMapper).insert(org.mockito.ArgumentMatchers.<ProductContractReleaseBatch>argThat(batch ->
                Long.valueOf(1001L).equals(batch.getProductId())
                        && "phase1-crack".equals(batch.getScenarioCode())
                        && "manual_compare_apply".equals(batch.getReleaseSource())
                        && Integer.valueOf(3).equals(batch.getReleasedFieldCount())
                        && Long.valueOf(10001L).equals(batch.getCreateBy())
        ));
    }

    @Test
    void saveBatchSnapshotShouldPersistSnapshotRow() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
                releaseBatchMapper,
                releaseSnapshotMapper,
                productModelMapper
        );

        service.saveBatchSnapshot(7001L, 1001L, "BEFORE_APPLY", "[{\"modelType\":\"property\",\"identifier\":\"value\"}]", 10001L);

        verify(releaseSnapshotMapper).insert(org.mockito.ArgumentMatchers.<ProductContractReleaseSnapshot>argThat(snapshot ->
                Long.valueOf(7001L).equals(snapshot.getBatchId())
                        && Long.valueOf(1001L).equals(snapshot.getProductId())
                        && "BEFORE_APPLY".equals(snapshot.getSnapshotStage())
                        && Long.valueOf(10001L).equals(snapshot.getCreateBy())
        ));
    }

    @Test
    void pageBatchesShouldReturnProductScopedReleaseBatches() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
                releaseBatchMapper,
                releaseSnapshotMapper,
                productModelMapper
        );
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(7001L);
        batch.setProductId(1001L);
        batch.setScenarioCode("phase1-crack");
        batch.setReleaseSource("manual_compare_apply");
        batch.setReleasedFieldCount(3);
        batch.setCreateBy(10001L);
        batch.setCreateTime(LocalDateTime.of(2026, 4, 6, 9, 30));
        when(releaseBatchMapper.selectPage(any(), any())).thenReturn(new Page<ProductContractReleaseBatch>(1L, 10L, 1L)
                .setRecords(List.of(batch)));

        com.ghlzm.iot.common.response.PageResult<ProductContractReleaseBatchVO> page =
                service.pageBatches(1001L, 1L, 10L);

        assertEquals(1L, page.getTotal());
        assertEquals(7001L, page.getRecords().get(0).getId());
        assertEquals("phase1-crack", page.getRecords().get(0).getScenarioCode());
    }

    @Test
    void getBatchShouldThrowWhenBatchMissing() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
                releaseBatchMapper,
                releaseSnapshotMapper,
                productModelMapper
        );
        when(releaseBatchMapper.selectById(7001L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.getBatch(7001L));
    }

    @Test
    void rollbackLatestBatchShouldRestoreSnapshotAndMarkBatchRolledBack() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
                releaseBatchMapper,
                releaseSnapshotMapper,
                productModelMapper
        );
        ProductContractReleaseBatch latest = batch(7001L, 1001L, "phase1-crack", "manual_compare_apply", 3);
        ProductContractReleaseBatch older = batch(7000L, 1001L, "phase1-crack", "manual_compare_apply", 2);
        ProductContractReleaseSnapshot snapshot = new ProductContractReleaseSnapshot();
        snapshot.setId(8001L);
        snapshot.setBatchId(7001L);
        snapshot.setProductId(1001L);
        snapshot.setSnapshotStage(ProductContractReleaseServiceImpl.SNAPSHOT_STAGE_BEFORE_APPLY);
        snapshot.setSnapshotJson("[{\"modelType\":\"property\",\"identifier\":\"value\",\"modelName\":\"裂缝监测值\",\"dataType\":\"double\",\"sortNo\":1,\"requiredFlag\":0}]");
        ProductModel existing = new ProductModel();
        existing.setId(3001L);
        existing.setProductId(1001L);
        existing.setModelType("property");
        existing.setIdentifier("temp");
        existing.setDeleted(0);

        when(releaseBatchMapper.selectById(7001L)).thenReturn(latest);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latest, older));
        when(releaseSnapshotMapper.selectList(any())).thenReturn(List.of(snapshot));
        when(productModelMapper.selectList(any())).thenReturn(List.of(existing));
        when(releaseBatchMapper.updateById(any(ProductContractReleaseBatch.class))).thenReturn(1);
        when(productModelMapper.hardDeleteById(3001L)).thenReturn(1);

        ProductContractReleaseRollbackResultVO result = service.rollbackLatestBatch(7001L, 10001L);

        assertEquals(7001L, result.getRolledBackBatchId());
        assertEquals("SNAPSHOT_FIELD_RESTORE", result.getRollbackMode());
        assertEquals(1, result.getRestoredFieldCount());
        verify(productModelMapper).insert(any(ProductModel.class));
        verify(productModelMapper).hardDeleteById(3001L);
        ArgumentCaptor<ProductContractReleaseBatch> captor = ArgumentCaptor.forClass(ProductContractReleaseBatch.class);
        verify(releaseBatchMapper).updateById(captor.capture());
        assertEquals(10001L, captor.getValue().getRollbackBy());
    }

    @Test
    void rollbackLatestBatchShouldRejectNonLatestBatch() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
                releaseBatchMapper,
                releaseSnapshotMapper,
                productModelMapper
        );
        ProductContractReleaseBatch target = batch(7000L, 1001L, "phase1-crack", "manual_compare_apply", 2);
        ProductContractReleaseBatch latest = batch(7001L, 1001L, "phase1-crack", "manual_compare_apply", 3);
        when(releaseBatchMapper.selectById(7000L)).thenReturn(target);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latest, target));

        assertThrows(BizException.class, () -> service.rollbackLatestBatch(7000L, 10001L));
        verify(releaseBatchMapper, never()).updateById(any(ProductContractReleaseBatch.class));
    }

    private ProductContractReleaseBatch batch(Long id,
                                              Long productId,
                                              String scenarioCode,
                                              String releaseSource,
                                              Integer releasedFieldCount) {
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(id);
        batch.setProductId(productId);
        batch.setScenarioCode(scenarioCode);
        batch.setReleaseSource(releaseSource);
        batch.setReleasedFieldCount(releasedFieldCount);
        batch.setCreateTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        return batch;
    }
}
