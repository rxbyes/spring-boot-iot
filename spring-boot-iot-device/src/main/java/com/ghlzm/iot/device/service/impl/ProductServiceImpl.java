package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import com.ghlzm.iot.device.vo.ProductDeviceStatRow;
import com.ghlzm.iot.device.vo.ProductPageVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 产品服务实现，负责产品台账、库存概览与基础维护。
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final DeviceMapper deviceMapper;

    public ProductServiceImpl(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailVO addProduct(ProductAddDTO dto) {
        String productKey = normalizeRequired(dto.getProductKey(), "产品Key");
        ensureProductKeyUnique(productKey, null);

        Product product = new Product();
        product.setProductKey(productKey);
        applyEditableFields(product, dto);
        save(product);
        return getDetailById(product.getId());
    }

    @Override
    public Product getRequiredById(Long id) {
        Product product = lambdaQuery()
                .eq(Product::getId, id)
                .eq(Product::getDeleted, 0)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + id);
        }
        return product;
    }

    @Override
    public ProductDetailVO getDetailById(Long id) {
        Product product = getRequiredById(id);
        ProductDeviceStatRow stat = loadProductDeviceStatMap(List.of(id)).get(id);
        return toDetailVO(product, stat);
    }

    @Override
    public PageResult<ProductPageVO> pageProducts(String productKey,
                                                  String productName,
                                                  String protocolCode,
                                                  Integer nodeType,
                                                  Integer status,
                                                  Long pageNum,
                                                  Long pageSize) {
        Page<Product> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<Product> result = page(page, buildProductQueryWrapper(
                productKey,
                productName,
                protocolCode,
                nodeType,
                status
        ));
        List<Product> records = result.getRecords();
        Map<Long, ProductDeviceStatRow> statMap = loadProductDeviceStatMap(records.stream().map(Product::getId).toList());
        List<ProductPageVO> rows = records.stream()
                .map(product -> toPageVO(product, statMap.get(product.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailVO updateProduct(Long id, ProductAddDTO dto) {
        Product product = getRequiredById(id);
        String productKey = normalizeRequired(dto.getProductKey(), "产品Key");
        if (!productKey.equals(product.getProductKey())) {
            throw new BizException("产品Key创建后不可修改");
        }

        boolean protocolChanged = !product.getProtocolCode().equals(normalizeRequired(dto.getProtocolCode(), "协议编码"));
        boolean nodeTypeChanged = !product.getNodeType().equals(dto.getNodeType());
        Integer targetStatus = resolveProductStatus(dto.getStatus());
        if (shouldDisableProduct(product.getStatus(), targetStatus)) {
            long activeDeviceCount = countActiveRelatedDevices(id);
            if (activeDeviceCount > 0) {
                throw new BizException("产品下仍有 " + activeDeviceCount + " 台启用设备，请先核查库存设备是否仍在使用后再停用");
            }
        }

        applyEditableFields(product, dto);
        updateById(product);

        if (protocolChanged || nodeTypeChanged) {
            syncRelatedDeviceBaseInfo(product);
        }
        return getDetailById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = getRequiredById(id);
        long deviceCount = countRelatedDevices(id);
        if (deviceCount > 0) {
            throw new BizException("产品下仍存在 " + deviceCount + " 台设备，请先处理库存设备后再删除");
        }
        if (!removeById(id)) {
            throw new BizException("产品删除失败，请稍后重试");
        }
    }

    @Override
    public List<Product> listAvailableProducts() {
        return lambdaQuery()
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, ProductStatusEnum.ENABLED.getCode())
                .orderByDesc(Product::getStatus)
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId)
                .list();
    }

    @Override
    public Product getRequiredByProductKey(String productKey) {
        Product product = lambdaQuery()
                .eq(Product::getProductKey, normalizeRequired(productKey, "产品Key"))
                .eq(Product::getDeleted, 0)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + productKey);
        }
        return product;
    }

    private void ensureProductKeyUnique(String productKey, Long excludeId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getProductKey, productKey)
                .eq(Product::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Product::getId, excludeId);
        }
        Product existing = getBaseMapper().selectOne(wrapper);
        if (existing != null) {
            throw new BizException("产品Key已存在: " + productKey);
        }
    }

    private void applyEditableFields(Product product, ProductAddDTO dto) {
        product.setProductName(normalizeRequired(dto.getProductName(), "产品名称"));
        product.setProtocolCode(normalizeRequired(dto.getProtocolCode(), "协议编码"));
        product.setNodeType(dto.getNodeType());
        product.setDataFormat(resolveOptionalText(dto.getDataFormat(), "JSON"));
        product.setManufacturer(resolveOptionalText(dto.getManufacturer(), null));
        product.setDescription(resolveOptionalText(dto.getDescription(), null));
        product.setStatus(resolveProductStatus(dto.getStatus()));
    }

    private LambdaQueryWrapper<Product> buildProductQueryWrapper(String productKey,
                                                                 String productName,
                                                                 String protocolCode,
                                                                 Integer nodeType,
                                                                 Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productKey)) {
            wrapper.like(Product::getProductKey, productKey.trim());
        }
        if (StringUtils.hasText(productName)) {
            wrapper.like(Product::getProductName, productName.trim());
        }
        if (StringUtils.hasText(protocolCode)) {
            wrapper.like(Product::getProtocolCode, protocolCode.trim());
        }
        if (nodeType != null) {
            wrapper.eq(Product::getNodeType, nodeType);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.eq(Product::getDeleted, 0)
                .orderByDesc(Product::getUpdateTime)
                .orderByDesc(Product::getId);
        return wrapper;
    }

    private void syncRelatedDeviceBaseInfo(Product product) {
        deviceMapper.update(
                null,
                new LambdaUpdateWrapper<Device>()
                        .eq(Device::getProductId, product.getId())
                        .eq(Device::getDeleted, 0)
                        .set(Device::getProtocolCode, product.getProtocolCode())
                        .set(Device::getNodeType, product.getNodeType())
        );
    }

    private long countRelatedDevices(Long productId) {
        Long count = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getProductId, productId)
                        .eq(Device::getDeleted, 0)
        );
        return count == null ? 0L : count;
    }

    private long countActiveRelatedDevices(Long productId) {
        Long count = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getProductId, productId)
                        .eq(Device::getDeleted, 0)
                        .eq(Device::getDeviceStatus, DeviceStatusEnum.ENABLED.getCode())
        );
        return count == null ? 0L : count;
    }

    private Integer resolveProductStatus(Integer status) {
        return status == null ? ProductStatusEnum.ENABLED.getCode() : status;
    }

    private boolean shouldDisableProduct(Integer currentStatus, Integer targetStatus) {
        return !ProductStatusEnum.DISABLED.getCode().equals(currentStatus)
                && ProductStatusEnum.DISABLED.getCode().equals(targetStatus);
    }

    private Map<Long, ProductDeviceStatRow> loadProductDeviceStatMap(List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Map.of();
        }
        List<ProductDeviceStatRow> rows = deviceMapper.selectProductStats(productIds);
        if (CollectionUtils.isEmpty(rows)) {
            return Map.of();
        }

        Map<Long, ProductDeviceStatRow> statMap = new LinkedHashMap<>();
        for (ProductDeviceStatRow row : rows) {
            if (row.getProductId() == null) {
                continue;
            }
            statMap.put(row.getProductId(), row);
        }
        return statMap;
    }

    private ProductPageVO toPageVO(Product product, ProductDeviceStatRow stat) {
        ProductPageVO row = new ProductPageVO();
        row.setId(product.getId());
        row.setProductKey(product.getProductKey());
        row.setProductName(product.getProductName());
        row.setProtocolCode(product.getProtocolCode());
        row.setNodeType(product.getNodeType());
        row.setDataFormat(product.getDataFormat());
        row.setManufacturer(product.getManufacturer());
        row.setStatus(product.getStatus());
        row.setDeviceCount(resolveDeviceCount(stat));
        row.setOnlineDeviceCount(resolveOnlineDeviceCount(stat));
        row.setLastReportTime(resolveLastReportTime(stat));
        row.setCreateTime(product.getCreateTime());
        row.setUpdateTime(product.getUpdateTime());
        return row;
    }

    private ProductDetailVO toDetailVO(Product product, ProductDeviceStatRow stat) {
        ProductDetailVO detail = new ProductDetailVO();
        detail.setId(product.getId());
        detail.setProductKey(product.getProductKey());
        detail.setProductName(product.getProductName());
        detail.setProtocolCode(product.getProtocolCode());
        detail.setNodeType(product.getNodeType());
        detail.setDataFormat(product.getDataFormat());
        detail.setManufacturer(product.getManufacturer());
        detail.setDescription(product.getDescription());
        detail.setStatus(product.getStatus());
        detail.setDeviceCount(resolveDeviceCount(stat));
        detail.setOnlineDeviceCount(resolveOnlineDeviceCount(stat));
        detail.setLastReportTime(resolveLastReportTime(stat));
        detail.setCreateTime(product.getCreateTime());
        detail.setUpdateTime(product.getUpdateTime());
        return detail;
    }

    private long resolveDeviceCount(ProductDeviceStatRow stat) {
        return stat == null || stat.getDeviceCount() == null ? 0L : stat.getDeviceCount();
    }

    private long resolveOnlineDeviceCount(ProductDeviceStatRow stat) {
        return stat == null || stat.getOnlineDeviceCount() == null ? 0L : stat.getOnlineDeviceCount();
    }

    private LocalDateTime resolveLastReportTime(ProductDeviceStatRow stat) {
        return stat == null ? null : stat.getLastReportTime();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String resolveOptionalText(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

}
