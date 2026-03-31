package com.ghlzm.iot.telemetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.telemetry.service.TelemetryHistoryMigrationService;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TDengine 历史数据迁移服务实现。
 */
@Service
public class TelemetryHistoryMigrationServiceImpl implements TelemetryHistoryMigrationService {

    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final DeviceTelemetryMappingService deviceTelemetryMappingService;
    private final NormalizedTelemetryHistoryReader normalizedReader;
    private final LegacyTelemetryHistoryReader legacyReader;
    private final TelemetryRawBatchWriter rawBatchWriter;
    private final TelemetryLatestProjector latestProjector;

    public TelemetryHistoryMigrationServiceImpl(DeviceMapper deviceMapper,
                                                ProductMapper productMapper,
                                                DevicePropertyMetadataService devicePropertyMetadataService,
                                                DeviceTelemetryMappingService deviceTelemetryMappingService,
                                                NormalizedTelemetryHistoryReader normalizedReader,
                                                LegacyTelemetryHistoryReader legacyReader,
                                                TelemetryRawBatchWriter rawBatchWriter,
                                                TelemetryLatestProjector latestProjector) {
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
        this.deviceTelemetryMappingService = deviceTelemetryMappingService;
        this.normalizedReader = normalizedReader;
        this.legacyReader = legacyReader;
        this.rawBatchWriter = rawBatchWriter;
        this.latestProjector = latestProjector;
    }

    @Override
    public TelemetryHistoryMigrationResult migrate(TelemetryHistoryMigrationRequest request) {
        List<Device> devices = resolveDevices(request);
        TelemetryHistoryMigrationResult result = new TelemetryHistoryMigrationResult();
        if (devices.isEmpty()) {
            result.setSource("none");
            return result;
        }
        String source = null;
        int totalScanned = 0;
        int totalWritten = 0;
        int totalProjected = 0;
        int migratedDevices = 0;
        int batchSize = normalizeBatchSize(request);
        for (Device device : devices) {
            if (device == null || device.getProductId() == null) {
                continue;
            }
            Product product = productMapper.selectById(device.getProductId());
            Map<String, DevicePropertyMetadata> metadataMap =
                    devicePropertyMetadataService.listPropertyMetadataMap(device.getProductId());
            List<TelemetryV2Point> points;
            String deviceSource;
            if (!Boolean.TRUE.equals(request.getPreferLegacy()) && normalizedReader.hasHistory(device.getId())) {
                points = normalizedReader.listHistory(device, product, metadataMap, batchSize);
                deviceSource = "normalized";
            } else {
                Map<String, TelemetryMetricMapping> mappingMap =
                        deviceTelemetryMappingService.listMetricMappingMap(device.getProductId());
                points = legacyReader.listHistory(device, product, metadataMap, mappingMap, batchSize);
                deviceSource = "legacy";
            }
            if (points.isEmpty()) {
                continue;
            }
            TelemetryPersistResult persistResult = rawBatchWriter.write(points);
            latestProjector.project(buildProjectionTask(device, product, points));
            source = mergeSource(source, deviceSource);
            migratedDevices++;
            totalScanned += points.size();
            totalWritten += persistResult.getPointCount() == null ? 0 : persistResult.getPointCount();
            totalProjected += points.size();
        }
        result.setSource(source == null ? "none" : source);
        result.setMigratedDeviceCount(migratedDevices);
        result.setScannedPointCount(totalScanned);
        result.setWrittenPointCount(totalWritten);
        result.setLatestProjectedPointCount(totalProjected);
        return result;
    }

    private List<Device> resolveDevices(TelemetryHistoryMigrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("迁移请求不能为空");
        }
        if (request.getDeviceId() != null) {
            Device device = deviceMapper.selectById(request.getDeviceId());
            return device == null ? List.of() : List.of(device);
        }
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("deviceId 或 productId 至少需要一个");
        }
        return new ArrayList<>(deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getProductId, request.getProductId())
                        .eq(Device::getDeleted, 0)
        ));
    }

    private int normalizeBatchSize(TelemetryHistoryMigrationRequest request) {
        if (request == null || request.getBatchSize() == null || request.getBatchSize() <= 0) {
            return 500;
        }
        return request.getBatchSize();
    }

    private String mergeSource(String existingSource, String currentSource) {
        if (existingSource == null || existingSource.isBlank()) {
            return currentSource;
        }
        if (existingSource.equals(currentSource)) {
            return existingSource;
        }
        return "mixed";
    }

    private TelemetryProjectionTask buildProjectionTask(Device device,
                                                        Product product,
                                                        List<TelemetryV2Point> points) {
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.LATEST);
        task.setTenantId(device == null ? null : device.getTenantId());
        task.setDeviceId(device == null ? null : device.getId());
        task.setProductId(device == null ? null : device.getProductId());
        task.setDeviceCode(device == null ? null : device.getDeviceCode());
        task.setProductKey(product == null ? null : product.getProductKey());
        task.setPoints(points == null ? List.of() : points);
        return task;
    }
}
