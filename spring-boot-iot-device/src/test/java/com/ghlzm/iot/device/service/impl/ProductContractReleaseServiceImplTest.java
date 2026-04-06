package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductContractReleaseServiceImplTest {

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    @Test
    void createBatchShouldPersistProductVersionMetadata() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(releaseBatchMapper);

        Long batchId = service.createBatch(1001L, "phase1-crack", "manual_compare_apply", 3, 10001L);

        assertNotNull(batchId);
        verify(releaseBatchMapper).insert(org.mockito.ArgumentMatchers.<com.ghlzm.iot.device.entity.ProductContractReleaseBatch>argThat(batch ->
                Long.valueOf(1001L).equals(batch.getProductId())
                        && "phase1-crack".equals(batch.getScenarioCode())
                        && "manual_compare_apply".equals(batch.getReleaseSource())
                        && Integer.valueOf(3).equals(batch.getReleasedFieldCount())
                        && Long.valueOf(10001L).equals(batch.getCreateBy())
        ));
    }

    @Test
    void pageBatchesShouldReturnProductScopedReleaseBatches() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(releaseBatchMapper);
        com.ghlzm.iot.device.entity.ProductContractReleaseBatch batch =
                new com.ghlzm.iot.device.entity.ProductContractReleaseBatch();
        batch.setId(7001L);
        batch.setProductId(1001L);
        batch.setScenarioCode("phase1-crack");
        batch.setReleaseSource("manual_compare_apply");
        batch.setReleasedFieldCount(3);
        batch.setCreateBy(10001L);
        batch.setCreateTime(LocalDateTime.of(2026, 4, 6, 9, 30));
        when(releaseBatchMapper.selectPage(any(), any())).thenReturn(new Page<com.ghlzm.iot.device.entity.ProductContractReleaseBatch>(1L, 10L, 1L)
                .setRecords(List.of(batch)));

        com.ghlzm.iot.common.response.PageResult<ProductContractReleaseBatchVO> page =
                service.pageBatches(1001L, 1L, 10L);

        assertEquals(1L, page.getTotal());
        assertEquals(7001L, page.getRecords().get(0).getId());
        assertEquals("phase1-crack", page.getRecords().get(0).getScenarioCode());
    }

    @Test
    void getBatchShouldThrowWhenBatchMissing() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(releaseBatchMapper);
        when(releaseBatchMapper.selectById(7001L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.getBatch(7001L));
    }
}
