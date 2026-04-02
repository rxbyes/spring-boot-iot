package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.service.DictService;
import com.ghlzm.iot.system.service.OrganizationService;
import com.ghlzm.iot.system.service.RegionService;
import com.ghlzm.iot.system.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 风险点Service实现类
 */
@Service
public class RiskPointServiceImpl extends ServiceImpl<RiskPointMapper, RiskPoint> implements RiskPointService {

      private static final String RISK_LEVEL_DICT_CODE = "risk_level";
      private final RiskPointDeviceMapper riskPointDeviceMapper;
      private final OrganizationService organizationService;
      private final RegionService regionService;
      private final UserService userService;
      private final DictService dictService;

      public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper,
                                  OrganizationService organizationService,
                                  RegionService regionService,
                                  UserService userService,
                                  DictService dictService) {
            this.riskPointDeviceMapper = riskPointDeviceMapper;
            this.organizationService = organizationService;
            this.regionService = regionService;
            this.userService = userService;
            this.dictService = dictService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint addRiskPoint(RiskPoint riskPoint, Long currentUserId) {
            validateRiskPointForWrite(riskPoint);
            Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
            Region region = resolveRequiredRegion(riskPoint.getRegionId());
            validateResponsibleUser(riskPoint.getResponsibleUser());
            riskPoint.setOrgName(organization.getOrgName());
            riskPoint.setRegionName(region.getRegionName());
            riskPoint.setResponsibleUser(normalizeResponsibleUser(riskPoint.getResponsibleUser()));
            riskPoint.setRiskLevel(normalizeAndValidateRiskLevel(riskPoint.getRiskLevel()));
            riskPoint.setCreateBy(currentUserId);
            riskPoint.setUpdateBy(currentUserId);

            riskPoint.setCreateTime(new Date());
            riskPoint.setUpdateTime(new Date());
            riskPoint.setDeleted(0);
            saveRiskPointWithGeneratedCode(riskPoint, organization.getOrgName());
            return normalizeRiskPointForRead(riskPoint);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint updateRiskPoint(RiskPoint riskPoint, Long currentUserId) {
            if (riskPoint.getId() == null) {
                  throw new BizException("风险点不存在");
            }
            validateRiskPointForWrite(riskPoint);
            RiskPoint existing = getById(riskPoint.getId());
            Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
            Region region = resolveRequiredRegion(riskPoint.getRegionId());
            validateResponsibleUser(riskPoint.getResponsibleUser());
            riskPoint.setOrgName(organization.getOrgName());
            riskPoint.setRegionName(region.getRegionName());
            riskPoint.setRiskPointCode(existing.getRiskPointCode());
            riskPoint.setRiskLevel(normalizeAndValidateRiskLevel(riskPoint.getRiskLevel()));
            riskPoint.setResponsibleUser(normalizeResponsibleUser(riskPoint.getResponsibleUser()));
            riskPoint.setCreateBy(existing.getCreateBy());
            riskPoint.setCreateTime(existing.getCreateTime());
            riskPoint.setUpdateBy(currentUserId);

            riskPoint.setUpdateTime(new Date());
            updateById(riskPoint);
            return normalizeRiskPointForRead(riskPoint);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRiskPoint(Long id) {
            getById(id);
            if (!removeById(id)) {
                  throw new BizException("风险点删除失败");
            }
      }

      @Override
      public RiskPoint getById(Long id) {
            RiskPoint riskPoint = super.getById(id);
            if (riskPoint == null || riskPoint.getDeleted() == 1) {
                  throw new BizException("风险点不存在");
            }
            return normalizeRiskPointForRead(riskPoint);
      }

      @Override
      public List<RiskPoint> listRiskPoints(String riskPointCode, String riskLevel, Integer status) {
            return normalizeRiskPoints(list(buildRiskPointWrapper(riskPointCode, riskLevel, status)));
      }

      @Override
      public PageResult<RiskPoint> pageRiskPoints(String riskPointCode, String riskLevel, Integer status, Long pageNum, Long pageSize) {
            Page<RiskPoint> page = new Page<>(pageNum, pageSize);
            Page<RiskPoint> result = page(page, buildRiskPointWrapper(riskPointCode, riskLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, normalizeRiskPoints(result.getRecords()));
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void bindDevice(RiskPointDevice riskPointDevice) {
            if (riskPointDevice == null || riskPointDevice.getRiskPointId() == null) {
                  throw new BizException("风险点不存在");
            }
            if (riskPointDevice.getDeviceId() == null) {
                  throw new BizException("请选择设备");
            }
            if (!StringUtils.hasText(riskPointDevice.getMetricIdentifier())) {
                  throw new BizException("请选择测点");
            }
            getById(riskPointDevice.getRiskPointId());

            // 检查是否已绑定
            LambdaQueryWrapper<RiskPointDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RiskPointDevice::getRiskPointId, riskPointDevice.getRiskPointId());
            queryWrapper.eq(RiskPointDevice::getDeviceId, riskPointDevice.getDeviceId());
            queryWrapper.eq(RiskPointDevice::getMetricIdentifier, riskPointDevice.getMetricIdentifier());
            queryWrapper.eq(RiskPointDevice::getDeleted, 0);
            RiskPointDevice existing = riskPointDeviceMapper.selectOne(queryWrapper);
            if (existing != null) {
                  throw new BizException("设备已绑定到该风险点");
            }

            riskPointDevice.setCreateTime(new Date());
            riskPointDevice.setUpdateTime(new Date());
            riskPointDevice.setDeleted(0);
            riskPointDeviceMapper.insert(riskPointDevice);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void unbindDevice(Long riskPointId, Long deviceId) {
            LambdaQueryWrapper<RiskPointDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RiskPointDevice::getRiskPointId, riskPointId);
            queryWrapper.eq(RiskPointDevice::getDeviceId, deviceId);
            queryWrapper.eq(RiskPointDevice::getDeleted, 0);
            Long activeBindingCount = riskPointDeviceMapper.selectCount(queryWrapper);
            if (activeBindingCount == null || activeBindingCount <= 0) {
                  throw new BizException("设备未绑定到该风险点");
            }

            int deletedRows = riskPointDeviceMapper.delete(queryWrapper);
            if (deletedRows <= 0) {
                  throw new BizException("设备解绑失败");
            }
      }

      @Override
      public List<RiskPointDevice> listBoundDevices(Long riskPointId) {
            LambdaQueryWrapper<RiskPointDevice> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskPointDevice::getRiskPointId, riskPointId);
            wrapper.eq(RiskPointDevice::getDeleted, 0);
            return riskPointDeviceMapper.selectList(wrapper);
      }

      private LambdaQueryWrapper<RiskPoint> buildRiskPointWrapper(String riskPointCode, String riskLevel, Integer status) {
            LambdaQueryWrapper<RiskPoint> wrapper = new LambdaQueryWrapper<>();
            if (riskPointCode != null && !riskPointCode.isEmpty()) {
                  wrapper.like(RiskPoint::getRiskPointCode, riskPointCode);
            }
            if (riskLevel != null && !riskLevel.isEmpty()) {
                  wrapper.in(RiskPoint::getRiskLevel, buildRiskLevelQueryValues(riskLevel));
            }
            if (status != null) {
                  wrapper.eq(RiskPoint::getStatus, status);
            }
            wrapper.eq(RiskPoint::getDeleted, 0);
            wrapper.orderByDesc(RiskPoint::getCreateTime);
            return wrapper;
      }

      private void validateRiskPointForWrite(RiskPoint riskPoint) {
            if (riskPoint == null) {
                  throw new BizException("风险点不存在");
            }
            if (!StringUtils.hasText(riskPoint.getRiskPointName())) {
                  throw new BizException("请输入风险点名称");
            }
            if (riskPoint.getOrgId() == null || riskPoint.getOrgId() <= 0) {
                  throw new BizException("请选择所属组织");
            }
            if (riskPoint.getRegionId() == null || riskPoint.getRegionId() <= 0) {
                  throw new BizException("请选择所属区域");
            }
            if (!StringUtils.hasText(riskPoint.getRiskLevel())) {
                  throw new BizException("请选择风险等级");
            }
      }

      private Organization resolveRequiredOrganization(Long orgId) {
            Organization organization = organizationService.getById(orgId);
            if (organization == null || Integer.valueOf(1).equals(organization.getDeleted())) {
                  throw new BizException("所属组织不存在");
            }
            if (!Integer.valueOf(1).equals(organization.getStatus())) {
                  throw new BizException("所属组织已停用");
            }
            return organization;
      }

      private Region resolveRequiredRegion(Long regionId) {
            Region region = regionService.getById(regionId);
            if (region == null || Integer.valueOf(1).equals(region.getDeleted())) {
                  throw new BizException("所属区域不存在");
            }
            if (!Integer.valueOf(1).equals(region.getStatus())) {
                  throw new BizException("所属区域已停用");
            }
            return region;
      }

      private void validateResponsibleUser(Long responsibleUserId) {
            if (responsibleUserId == null || responsibleUserId <= 0) {
                  return;
            }
            User responsibleUser = userService.getById(responsibleUserId);
            if (responsibleUser == null || Integer.valueOf(1).equals(responsibleUser.getDeleted())) {
                  throw new BizException("负责人不存在");
            }
            if (!Integer.valueOf(1).equals(responsibleUser.getStatus())) {
                  throw new BizException("负责人已停用");
            }
      }

      private String normalizeAndValidateRiskLevel(String riskLevel) {
            String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
            if (!StringUtils.hasText(normalizedRiskLevel)) {
                  throw new BizException("请选择风险等级");
            }
            Set<String> enabledRiskLevels = loadEnabledRiskLevels();
            if (enabledRiskLevels.isEmpty()) {
                  throw new BizException("风险等级字典未配置");
            }
            if (!enabledRiskLevels.contains(normalizedRiskLevel)) {
                  throw new BizException("风险等级不在允许范围内");
            }
            return normalizedRiskLevel;
      }

        private Set<String> loadEnabledRiskLevels() {
              Dict dict = dictService.getByCode(RISK_LEVEL_DICT_CODE);
              if (dict == null || dict.getItems() == null) {
                    return Set.of();
              }
              return dict.getItems().stream()
                      .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                      .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                      .map(DictItem::getItemValue)
                      .filter(StringUtils::hasText)
                      .map(this::normalizeRiskLevel)
                      .filter(StringUtils::hasText)
                      .collect(Collectors.toCollection(LinkedHashSet::new));
        }

      private List<String> buildRiskLevelQueryValues(String riskLevel) {
            String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
            if (!StringUtils.hasText(normalizedRiskLevel)) {
                  return List.of();
            }
            return switch (normalizedRiskLevel) {
                  case "red" -> List.of("red", "critical");
                  case "orange" -> List.of("orange", "warning");
                  case "blue" -> List.of("blue", "info");
                  default -> List.of(normalizedRiskLevel);
            };
      }

      private String normalizeRiskLevel(String riskLevel) {
            if (!StringUtils.hasText(riskLevel)) {
                  return "";
            }
            return switch (riskLevel.trim().toLowerCase(Locale.ROOT)) {
                  case "critical" -> "red";
                  case "warning" -> "orange";
                  case "info" -> "blue";
                  default -> riskLevel.trim().toLowerCase(Locale.ROOT);
            };
      }

      private Long normalizeResponsibleUser(Long responsibleUserId) {
            return responsibleUserId == null || responsibleUserId <= 0 ? null : responsibleUserId;
      }

      private List<RiskPoint> normalizeRiskPoints(List<RiskPoint> riskPoints) {
            if (riskPoints == null) {
                  return List.of();
            }
            riskPoints.forEach(this::normalizeRiskPointForRead);
            return riskPoints;
      }

      private RiskPoint normalizeRiskPointForRead(RiskPoint riskPoint) {
            if (riskPoint == null) {
                  return null;
            }
            riskPoint.setRiskLevel(normalizeRiskLevel(riskPoint.getRiskLevel()));
            return riskPoint;
      }

      private void saveRiskPointWithGeneratedCode(RiskPoint riskPoint, String orgName) {
            String base = buildRiskPointCodeBase(riskPoint.getRiskPointName(), orgName, riskPoint.getRiskLevel());
            int suffix = 1;
            while (true) {
                  String code = appendCodeSuffix(base, suffix);
                  if (existsCode(code)) {
                        suffix++;
                        continue;
                  }
                  riskPoint.setRiskPointCode(code);
                  try {
                        save(riskPoint);
                        return;
                  } catch (DuplicateKeyException ex) {
                        suffix++;
                  }
            }
      }

      private String buildRiskPointCodeBase(String riskPointName, String orgName, String riskLevel) {
            return String.format(
                    "RP-%s-%s-%s",
                    buildCodeSegment(orgName, 6),
                    buildCodeSegment(riskPointName, 6),
                    buildCodeSegment(riskLevel, 4)
            );
      }

      private boolean existsCode(String code) {
            LambdaQueryWrapper<RiskPoint> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskPoint::getRiskPointCode, code);
            return getOne(wrapper) != null;
      }

      private String appendCodeSuffix(String base, int suffix) {
            return base + "-" + String.format("%03d", suffix);
      }

      private String buildCodeSegment(String source, int maxLength) {
            if (!StringUtils.hasText(source)) {
                  return "GEN";
            }
            String normalized = source.trim().replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "");
            if (normalized.isBlank()) {
                  return "GEN";
            }
            StringBuilder builder = new StringBuilder();
            normalized.codePoints().forEach(codePoint -> {
                  if (builder.length() >= maxLength) {
                        return;
                  }
                  if (codePoint <= 127) {
                        builder.appendCodePoint(Character.toUpperCase(codePoint));
                  } else {
                        builder.appendCodePoint(codePoint);
                  }
            });
            return builder.isEmpty() ? "GEN" : builder.toString();
      }
}
