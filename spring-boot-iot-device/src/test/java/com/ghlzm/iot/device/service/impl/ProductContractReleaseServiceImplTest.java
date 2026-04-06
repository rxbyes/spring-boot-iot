package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
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
}
