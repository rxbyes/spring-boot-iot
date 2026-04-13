package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishedProductContractSnapshotServiceImplTest {

    @Mock
    private ProductModelMapper productModelMapper;

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    private PublishedProductContractSnapshotServiceImpl snapshotService;

    @BeforeEach
    void setUp() {
        snapshotService = new PublishedProductContractSnapshotServiceImpl(productModelMapper, releaseBatchMapper);
    }

    @Test
    void shouldExposeOnlyPublishedPropertyIdentifiersForObjectInsight() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                property("L1_LF_1.value"),
                property("value")
        ));
        ProductContractReleaseBatch latestBatch = new ProductContractReleaseBatch();
        latestBatch.setId(9001L);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latestBatch));

        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(1001L);
        assertTrue(snapshot.publishedIdentifiers().contains("value"));
        assertFalse(snapshot.publishedIdentifiers().contains("L1_LF_1.value"));
    }

    private ProductModel property(String identifier) {
        ProductModel model = new ProductModel();
        model.setIdentifier(identifier);
        return model;
    }
}
