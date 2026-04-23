package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.device.vo.DevicePropertyInsightVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采集器子设备总览控制器。
 */
@RestController
public class DeviceCollectorInsightController {

    private final CollectorChildInsightService collectorChildInsightService;
    private final DeviceService deviceService;
    private final DeviceTopologyRoleResolver topologyRoleResolver;
    private final ProductService productService;

    public DeviceCollectorInsightController(CollectorChildInsightService collectorChildInsightService,
                                            DeviceService deviceService,
                                            DeviceTopologyRoleResolver topologyRoleResolver,
                                            ProductService productService) {
        this.collectorChildInsightService = collectorChildInsightService;
        this.deviceService = deviceService;
        this.topologyRoleResolver = topologyRoleResolver;
        this.productService = productService;
    }

    @GetMapping("/api/device/{deviceCode}/collector-children/overview")
    public R<CollectorChildInsightOverviewVO> getOverview(@PathVariable String deviceCode,
                                                          Authentication authentication) {
        return R.ok(collectorChildInsightService.getOverview(requireCurrentUserId(authentication), deviceCode));
    }

    @GetMapping("/api/device/product/{productId}/collector-children/recommended-metrics")
    public R<List<String>> listRecommendedMetrics(@PathVariable Long productId,
                                                  Authentication authentication) {
        requireCurrentUserId(authentication);
        return R.ok(collectorChildInsightService.listRecommendedMetrics(productId));
    }

    @GetMapping("/api/device/{deviceCode}/topology-role")
    public R<DeviceTopologyRole> getTopologyRole(@PathVariable String deviceCode,
                                                  Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        Device device = deviceService.getRequiredByCode(userId, deviceCode);
        String productKey = null;
        if (device.getProductId() != null) {
            Product product = productService.getById(device.getProductId());
            if (product != null) {
                productKey = product.getProductKey();
            }
        }
        return R.ok(topologyRoleResolver.resolve(device.getProductId(), device.getNodeType(), productKey));
    }

    @GetMapping("/api/device/{deviceCode}/insight/properties")
    public R<DevicePropertyInsightVO> listInsightProperties(@PathVariable String deviceCode,
                                                             Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        List<DeviceProperty> properties = deviceService.listPropertiesForInsight(userId, deviceCode);
        Device device = deviceService.getRequiredByCode(userId, deviceCode);
        String productKey = null;
        if (device.getProductId() != null) {
            Product product = productService.getById(device.getProductId());
            if (product != null) {
                productKey = product.getProductKey();
            }
        }
        DeviceTopologyRole role = topologyRoleResolver.resolve(device.getProductId(), device.getNodeType(), productKey);
        DevicePropertyInsightVO vo = new DevicePropertyInsightVO();
        vo.setTopologyRole(role);
        vo.setProperties(properties);
        return R.ok(vo);
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
