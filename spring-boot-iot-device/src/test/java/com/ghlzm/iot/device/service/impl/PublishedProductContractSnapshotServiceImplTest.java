package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductMetricResolverSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMetricResolverSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishedProductContractSnapshotServiceImplTest {

    @Mock
    private ProductModelMapper productModelMapper;

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    @Mock
    private ProductMetricResolverSnapshotMapper snapshotMapper;

    private PublishedProductContractSnapshotServiceImpl snapshotService;

    @BeforeEach
    void setUp() {
        snapshotService = new PublishedProductContractSnapshotServiceImpl(
                productModelMapper,
                releaseBatchMapper,
                snapshotMapper
        );
    }

    @Test
    void shouldExposeOnlyPublishedPropertyIdentifiersForObjectInsight() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                property("L1_LF_1.value"),
                property("L1_GNSS_1.gpsTotalX"),
                property("value"),
                property("gpsTotalX")
        ));
        ProductContractReleaseBatch latestBatch = new ProductContractReleaseBatch();
        latestBatch.setId(9001L);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latestBatch));

        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(1001L);
        assertTrue(snapshot.publishedIdentifiers().contains("value"));
        assertTrue(snapshot.publishedIdentifiers().contains("gpsTotalX"));
        assertFalse(snapshot.publishedIdentifiers().contains("L1_GNSS_1.gpsTotalX"));
        assertFalse(snapshot.publishedIdentifiers().contains("gpstotalx"));
    }

    @Test
    void shouldPreferPersistedResolverSnapshotForLatestReleasedBatch() {
        ProductContractReleaseBatch latestBatch = new ProductContractReleaseBatch();
        latestBatch.setId(9001L);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latestBatch));

        ProductMetricResolverSnapshot persisted = new ProductMetricResolverSnapshot();
        persisted.setProductId(1001L);
        persisted.setReleaseBatchId(9001L);
        persisted.setSnapshotJson("""
                {
                  "publishedIdentifiers": ["value", "sensor_state"],
                  "canonicalAliases": {
                    "L1_LF_1.value": "value",
                    "value": "value",
                    "sensor_state": "sensor_state"
                  }
                }
                """);
        when(snapshotMapper.selectList(any())).thenReturn(List.of(persisted));

        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(1001L);

        assertTrue(snapshot.publishedIdentifiers().contains("value"));
        assertEquals("value", snapshot.canonicalAliasOf("L1_LF_1.value").orElse(null));
        verify(productModelMapper, never()).selectList(any());
    }

    private ProductModel property(String identifier) {
        ProductModel model = new ProductModel();
        model.setIdentifier(identifier);
        return model;
    }
}
