package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.DeviceCapabilityRegistry;
import com.ghlzm.iot.device.capability.DeviceCapabilityType;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadata;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadataParser;
import com.ghlzm.iot.device.capability.WarningDeviceKind;
import com.ghlzm.iot.device.capability.VideoDeviceKind;
import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceCapabilityCommandGateway;
import com.ghlzm.iot.device.service.DeviceCapabilityService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceCapabilityServiceImpl implements DeviceCapabilityService {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final ProductCapabilityMetadataParser productCapabilityMetadataParser;
    private final DeviceCapabilityRegistry deviceCapabilityRegistry;
    private final DeviceCapabilityCommandGateway deviceCapabilityCommandGateway;
    private final CommandRecordService commandRecordService;

    public DeviceCapabilityServiceImpl(DeviceService deviceService,
                                       ProductService productService,
                                       ProductCapabilityMetadataParser productCapabilityMetadataParser,
                                       DeviceCapabilityRegistry deviceCapabilityRegistry,
                                       DeviceCapabilityCommandGateway deviceCapabilityCommandGateway,
                                       CommandRecordService commandRecordService) {
        this.deviceService = deviceService;
        this.productService = productService;
        this.productCapabilityMetadataParser = productCapabilityMetadataParser;
        this.deviceCapabilityRegistry = deviceCapabilityRegistry;
        this.deviceCapabilityCommandGateway = deviceCapabilityCommandGateway;
        this.commandRecordService = commandRecordService;
    }

    @Override
    public DeviceCapabilityOverviewVO getCapabilities(Long currentUserId, String deviceCode) {
        Device device = deviceService.getRequiredByCode(currentUserId, deviceCode);
        Product product = requireProduct(device);
        ProductCapabilityMetadata metadata = parseMetadata(product);
        List<DeviceCapabilityDefinition> definitions = deviceCapabilityRegistry.resolve(metadata);

        DeviceCapabilityOverviewVO overview = new DeviceCapabilityOverviewVO();
        overview.setDeviceCode(device.getDeviceCode());
        overview.setProductId(product.getId());
        overview.setProductKey(product.getProductKey());
        overview.setProductCapabilityType(metadata.capabilityType().name());
        overview.setSubType(resolveSubType(metadata));
        overview.setOnlineExecutable(isExecutableDevice(device, product, true));
        overview.setDisabledReason(resolveDisabledReason(device, product, true));
        overview.setCapabilities(definitions.stream().map(definition -> toCapabilityVO(device, product, definition)).toList());
        return overview;
    }

    @Override
    public DeviceCapabilityExecuteResultVO execute(Long currentUserId,
                                                   String deviceCode,
                                                   String capabilityCode,
                                                   DeviceCapabilityExecuteDTO dto) {
        Device device = deviceService.getRequiredByCode(currentUserId, deviceCode);
        Product product = requireProduct(device);
        ProductCapabilityMetadata metadata = parseMetadata(product);
        DeviceCapabilityDefinition capability = deviceCapabilityRegistry.require(capabilityCode, metadata);
        if (capability == null) {
            throw new BizException("当前产品不支持该设备能力: " + capabilityCode);
        }
        if (!isExecutableDevice(device, product, capability.requiresOnline())) {
            throw new BizException(resolveDisabledReason(device, product, capability.requiresOnline()));
        }

        DeviceCapabilityCommandRequest request = new DeviceCapabilityCommandRequest();
        request.setCurrentUserId(currentUserId);
        request.setDevice(device);
        request.setProduct(product);
        request.setMetadata(metadata);
        request.setCapability(capability);
        if (dto != null && dto.getParams() != null) {
            request.setParams(dto.getParams());
        }

        DeviceCapabilityCommandResult commandResult = deviceCapabilityCommandGateway.execute(request);
        DeviceCapabilityExecuteResultVO vo = new DeviceCapabilityExecuteResultVO();
        vo.setCommandId(commandResult.getCommandId());
        vo.setDeviceCode(commandResult.getDeviceCode());
        vo.setCapabilityCode(commandResult.getCapabilityCode());
        vo.setStatus(commandResult.getStatus());
        vo.setTopic(commandResult.getTopic());
        vo.setSentAt(commandResult.getSentAt());
        return vo;
    }

    @Override
    public PageResult<CommandRecordPageItemVO> pageCommands(Long currentUserId,
                                                            String deviceCode,
                                                            String capabilityCode,
                                                            String status,
                                                            Long pageNum,
                                                            Long pageSize) {
        return commandRecordService.pageByDevice(currentUserId, deviceCode, capabilityCode, status, pageNum, pageSize);
    }

    private Product requireProduct(Device device) {
        if (device == null || device.getProductId() == null) {
            throw new BizException("设备未绑定产品，无法查看设备能力");
        }
        return productService.getRequiredById(device.getProductId());
    }

    private ProductCapabilityMetadata parseMetadata(Product product) {
        return productCapabilityMetadataParser.parse(product == null ? null : product.getMetadataJson());
    }

    private DeviceCapabilityVO toCapabilityVO(Device device, Product product, DeviceCapabilityDefinition definition) {
        DeviceCapabilityVO vo = new DeviceCapabilityVO();
        vo.setCode(definition.code());
        vo.setName(definition.name());
        vo.setGroup(definition.group());
        vo.setRequiresOnline(definition.requiresOnline());
        vo.setParamsSchema(definition.paramsSchema());

        String disabledReason = resolveDisabledReason(device, product, definition.requiresOnline());
        vo.setEnabled(disabledReason == null);
        vo.setDisabledReason(disabledReason);
        return vo;
    }

    private boolean isExecutableDevice(Device device, Product product, boolean requiresOnline) {
        return resolveDisabledReason(device, product, requiresOnline) == null;
    }

    private String resolveDisabledReason(Device device, Product product, boolean requiresOnline) {
        if (product != null && ProductStatusEnum.DISABLED.getCode().equals(product.getStatus())) {
            return "产品已停用，暂不支持下发";
        }
        if (device != null && DeviceStatusEnum.DISABLED.getCode().equals(device.getDeviceStatus())) {
            return "设备已停用，暂不支持下发";
        }
        if (device != null && !Integer.valueOf(1).equals(device.getActivateStatus())) {
            return "设备未激活，暂不支持下发";
        }
        if (requiresOnline && device != null && !Integer.valueOf(1).equals(device.getOnlineStatus())) {
            return "设备离线，当前能力需要在线执行";
        }
        return null;
    }

    private String resolveSubType(ProductCapabilityMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        if (metadata.capabilityType() == DeviceCapabilityType.WARNING && metadata.warningDeviceKind() != WarningDeviceKind.UNKNOWN) {
            return metadata.warningDeviceKind().name();
        }
        if (metadata.capabilityType() == DeviceCapabilityType.VIDEO && metadata.videoDeviceKind() != VideoDeviceKind.UNKNOWN) {
            return metadata.videoDeviceKind().name();
        }
        return null;
    }
}
