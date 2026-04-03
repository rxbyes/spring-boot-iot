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
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.service.DictService;
import com.ghlzm.iot.system.service.OrganizationService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.RegionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 风险点Service实现类
 */
@Service
public class RiskPointServiceImpl extends ServiceImpl<RiskPointMapper, RiskPoint> implements RiskPointService {

      private static final String RISK_POINT_LEVEL_DICT_CODE = "risk_point_level";
      private static final String DEFAULT_CURRENT_RISK_LEVEL = "blue";
      private final RiskPointDeviceMapper riskPointDeviceMapper;
      private final OrganizationService organizationService;
      private final RegionService regionService;
      private final UserService userService;
      private final DictService dictService;
      private final PermissionService permissionService;
      private final DeviceService deviceService;

      public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper,
                                  OrganizationService organizationService,
                                  RegionService regionService,
                                  UserService userService,
                                  DictService dictService) {
            this(riskPointDeviceMapper, organizationService, regionService, userService, dictService, null, null);
      }

      public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper,
                                  OrganizationService organizationService,
                                  RegionService regionService,
                                  UserService userService,
                                  DictService dictService,
                                  PermissionService permissionService) {
            this(riskPointDeviceMapper, organizationService, regionService, userService, dictService, permissionService, null);
      }

      @Autowired
      public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper,
                                  OrganizationService organizationService,
                                  RegionService regionService,
                                  UserService userService,
                                  DictService dictService,
                                  PermissionService permissionService,
                                  DeviceService deviceService) {
            this.riskPointDeviceMapper = riskPointDeviceMapper;
            this.organizationService = organizationService;
            this.regionService = regionService;
            this.userService = userService;
            this.dictService = dictService;
            this.permissionService = permissionService;
            this.deviceService = deviceService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint addRiskPoint(RiskPoint riskPoint, Long currentUserId) {
            validateRiskPointForWrite(riskPoint);
            ensureWritableRiskPointOrg(currentUserId, riskPoint.getOrgId());
            Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
            Region region = resolveRequiredRegion(riskPoint.getRegionId());
            validateResponsibleUser(riskPoint.getResponsibleUser());
            riskPoint.setOrgName(organization.getOrgName());
            riskPoint.setRegionName(region.getRegionName());
            riskPoint.setResponsibleUser(normalizeResponsibleUser(riskPoint.getResponsibleUser()));
            riskPoint.setRiskPointLevel(normalizeAndValidateRiskPointLevel(riskPoint.getRiskPointLevel()));
            riskPoint.setCurrentRiskLevel(DEFAULT_CURRENT_RISK_LEVEL);
            riskPoint.setRiskLevel(riskPoint.getCurrentRiskLevel());
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
            RiskPoint existing = hasDataPermissionSupport() && currentUserId != null
                    ? getById(riskPoint.getId(), currentUserId)
                    : getById(riskPoint.getId());
            ensureWritableRiskPointOrg(currentUserId, riskPoint.getOrgId());
            Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
            Region region = resolveRequiredRegion(riskPoint.getRegionId());
            validateResponsibleUser(riskPoint.getResponsibleUser());
            riskPoint.setOrgName(organization.getOrgName());
            riskPoint.setRegionName(region.getRegionName());
            riskPoint.setRiskPointCode(existing.getRiskPointCode());
            riskPoint.setRiskPointLevel(normalizeAndValidateRiskPointLevel(riskPoint.getRiskPointLevel()));
            riskPoint.setCurrentRiskLevel(resolveCurrentRiskLevel(existing));
            riskPoint.setRiskLevel(riskPoint.getCurrentRiskLevel());
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
            deleteRiskPoint(id, null);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRiskPoint(Long id, Long currentUserId) {
            if (hasDataPermissionSupport() && currentUserId != null) {
                  getById(id, currentUserId);
            } else {
                  getById(id);
            }
            if (!removeById(id)) {
                  throw new BizException("风险点删除失败");
            }
      }

      @Override
      public RiskPoint getById(Long id) {
            return getById(id, null);
      }

      @Override
      public RiskPoint getById(Long id, Long currentUserId) {
            RiskPoint riskPoint = super.getById(id);
            if (riskPoint == null || riskPoint.getDeleted() == 1) {
                  throw new BizException("风险点不存在");
            }
            ensureRiskPointAccessible(currentUserId, riskPoint);
            return normalizeRiskPointForRead(riskPoint);
      }

      @Override
      public List<RiskPoint> listRiskPoints(String riskPointCode, String riskPointLevel, Integer status) {
            return listRiskPoints(null, riskPointCode, riskPointLevel, status);
      }

      @Override
      public List<RiskPoint> listRiskPoints(Long currentUserId, String riskPointCode, String riskPointLevel, Integer status) {
            return normalizeRiskPoints(list(buildRiskPointWrapper(currentUserId, riskPointCode, riskPointLevel, status)));
      }

      @Override
      public PageResult<RiskPoint> pageRiskPoints(String riskPointCode, String riskPointLevel, Integer status, Long pageNum, Long pageSize) {
            return pageRiskPoints(null, riskPointCode, riskPointLevel, status, pageNum, pageSize);
      }

      @Override
      public PageResult<RiskPoint> pageRiskPoints(Long currentUserId, String riskPointCode, String riskPointLevel, Integer status, Long pageNum, Long pageSize) {
            Page<RiskPoint> page = new Page<>(pageNum, pageSize);
            Page<RiskPoint> result = page(page, buildRiskPointWrapper(currentUserId, riskPointCode, riskPointLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, normalizeRiskPoints(result.getRecords()));
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void bindDevice(RiskPointDevice riskPointDevice) {
            bindDevice(riskPointDevice, null);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void bindDevice(RiskPointDevice riskPointDevice, Long currentUserId) {
            bindDeviceAndReturn(riskPointDevice, currentUserId);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPointDevice bindDeviceAndReturn(RiskPointDevice riskPointDevice, Long currentUserId) {
            if (riskPointDevice == null || riskPointDevice.getRiskPointId() == null) {
                  throw new BizException("风险点不存在");
            }
            if (riskPointDevice.getDeviceId() == null) {
                  throw new BizException("请选择设备");
            }
            if (!StringUtils.hasText(riskPointDevice.getMetricIdentifier())) {
                  throw new BizException("请选择测点");
            }
            RiskPoint riskPoint = hasDataPermissionSupport() && currentUserId != null
                    ? getById(riskPointDevice.getRiskPointId(), currentUserId)
                    : getById(riskPointDevice.getRiskPointId());

            LambdaQueryWrapper<RiskPointDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RiskPointDevice::getRiskPointId, riskPointDevice.getRiskPointId());
            queryWrapper.eq(RiskPointDevice::getDeviceId, riskPointDevice.getDeviceId());
            queryWrapper.eq(RiskPointDevice::getMetricIdentifier, riskPointDevice.getMetricIdentifier());
            queryWrapper.eq(RiskPointDevice::getDeleted, 0);
            RiskPointDevice existing = riskPointDeviceMapper.selectOne(queryWrapper);
            if (existing != null) {
                  throw new BizException("设备已绑定到该风险点");
            }

            Device device = resolveRequiredDevice(currentUserId, riskPointDevice.getDeviceId());
            validateRiskPointDeviceBinding(riskPoint, device, riskPointDevice.getRiskPointId());
            riskPointDevice.setDeviceCode(device.getDeviceCode());
            riskPointDevice.setDeviceName(device.getDeviceName());

            riskPointDevice.setCreateTime(new Date());
            riskPointDevice.setUpdateTime(new Date());
            riskPointDevice.setCreateBy(currentUserId);
            riskPointDevice.setUpdateBy(currentUserId);
            riskPointDevice.setDeleted(0);
            riskPointDeviceMapper.insert(riskPointDevice);
            return riskPointDevice;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void unbindDevice(Long riskPointId, Long deviceId) {
            unbindDevice(riskPointId, deviceId, null);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void unbindDevice(Long riskPointId, Long deviceId, Long currentUserId) {
            if (hasDataPermissionSupport() && currentUserId != null) {
                  getById(riskPointId, currentUserId);
            }
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
            return listBoundDevices(riskPointId, null);
      }

      @Override
      public List<RiskPointDevice> listBoundDevices(Long riskPointId, Long currentUserId) {
            if (hasDataPermissionSupport() && currentUserId != null) {
                  getById(riskPointId, currentUserId);
            } else {
                  getById(riskPointId);
            }
            LambdaQueryWrapper<RiskPointDevice> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskPointDevice::getRiskPointId, riskPointId);
            wrapper.eq(RiskPointDevice::getDeleted, 0);
            return riskPointDeviceMapper.selectList(wrapper);
      }

      @Override
      public List<DeviceOptionVO> listBindableDevices(Long riskPointId) {
            return listBindableDevices(riskPointId, null);
      }

      @Override
      public List<DeviceOptionVO> listBindableDevices(Long riskPointId, Long currentUserId) {
            RiskPoint riskPoint = hasDataPermissionSupport() && currentUserId != null
                    ? getById(riskPointId, currentUserId)
                    : getById(riskPointId);
            if (deviceService == null) {
                  return List.of();
            }
            List<DeviceOptionVO> deviceOptions = currentUserId == null
                    ? deviceService.listDeviceOptions(false)
                    : deviceService.listDeviceOptions(currentUserId, false);
            if (deviceOptions.isEmpty()) {
                  return List.of();
            }
            List<RiskPointDevice> activeBindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                    .eq(RiskPointDevice::getDeleted, 0));
            Set<Long> currentRiskPointDeviceIds = activeBindings.stream()
                    .filter(binding -> Objects.equals(riskPointId, binding.getRiskPointId()))
                    .map(RiskPointDevice::getDeviceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<Long> occupiedDeviceIds = activeBindings.stream()
                    .filter(binding -> !Objects.equals(riskPointId, binding.getRiskPointId()))
                    .map(RiskPointDevice::getDeviceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return deviceOptions.stream()
                    .filter(device -> Objects.equals(riskPoint.getOrgId(), device.getOrgId()))
                    .filter(device -> currentRiskPointDeviceIds.contains(device.getId()) || !occupiedDeviceIds.contains(device.getId()))
                    .toList();
      }

      private Device resolveRequiredDevice(Long currentUserId, Long deviceId) {
            if (deviceService == null) {
                  throw new BizException("设备不存在或无权访问");
            }
            return currentUserId == null
                    ? deviceService.getRequiredById(deviceId)
                    : deviceService.getRequiredById(currentUserId, deviceId);
      }

      private void validateRiskPointDeviceBinding(RiskPoint riskPoint, Device device, Long currentRiskPointId) {
            if (device.getOrgId() == null || device.getOrgId() <= 0) {
                  throw new BizException("设备未归属组织，禁止绑定风险点");
            }
            if (!Objects.equals(riskPoint.getOrgId(), device.getOrgId())) {
                  throw new BizException("设备所属组织与风险点所属组织不一致");
            }
            LambdaQueryWrapper<RiskPointDevice> occupiedWrapper = new LambdaQueryWrapper<>();
            occupiedWrapper.eq(RiskPointDevice::getDeviceId, device.getId());
            occupiedWrapper.eq(RiskPointDevice::getDeleted, 0);
            occupiedWrapper.ne(currentRiskPointId != null, RiskPointDevice::getRiskPointId, currentRiskPointId);
            if (!riskPointDeviceMapper.selectList(occupiedWrapper).isEmpty()) {
                  throw new BizException("设备已绑定其他风险点，不能重复绑定");
            }
      }

      private LambdaQueryWrapper<RiskPoint> buildRiskPointWrapper(Long currentUserId, String riskPointCode, String riskPointLevel, Integer status) {
            LambdaQueryWrapper<RiskPoint> wrapper = new LambdaQueryWrapper<>();
            applyRiskPointScope(wrapper, currentUserId);
            if (riskPointCode != null && !riskPointCode.isEmpty()) {
                  wrapper.like(RiskPoint::getRiskPointCode, riskPointCode);
            }
            if (riskPointLevel != null && !riskPointLevel.isEmpty()) {
                  wrapper.in(RiskPoint::getRiskPointLevel, buildRiskPointLevelQueryValues(riskPointLevel));
            }
            if (status != null) {
                  wrapper.eq(RiskPoint::getStatus, status);
            }
            wrapper.eq(RiskPoint::getDeleted, 0);
            wrapper.orderByDesc(RiskPoint::getCreateTime);
            return wrapper;
      }

      private void applyRiskPointScope(LambdaQueryWrapper<RiskPoint> wrapper, Long currentUserId) {
            if (!hasDataPermissionSupport() || currentUserId == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            wrapper.eq(context.tenantId() != null, RiskPoint::getTenantId, context.tenantId());
            if (context.superAdmin() || context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.SELF) {
                  wrapper.and(scope -> scope.eq(RiskPoint::getResponsibleUser, currentUserId)
                          .or()
                          .eq(RiskPoint::getCreateBy, currentUserId));
                  return;
            }
            Set<Long> accessibleOrgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
            if (accessibleOrgIds.isEmpty()) {
                  wrapper.eq(RiskPoint::getId, -1L);
                  return;
            }
            wrapper.in(RiskPoint::getOrgId, accessibleOrgIds);
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
            if (!StringUtils.hasText(riskPoint.getRiskPointLevel())) {
                  if (StringUtils.hasText(riskPoint.getRiskLevel())) {
                        throw new BizException("风险点档案等级已改为 riskPointLevel，请补录一级/二级/三级");
                  }
                  throw new BizException("请选择风险点档案等级");
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

      private String normalizeAndValidateRiskPointLevel(String riskPointLevel) {
            String normalizedRiskPointLevel = normalizeRiskPointLevel(riskPointLevel);
            if (!StringUtils.hasText(normalizedRiskPointLevel)) {
                  throw new BizException("请选择风险点档案等级");
            }
            Set<String> enabledRiskPointLevels = loadEnabledRiskPointLevels();
            if (enabledRiskPointLevels.isEmpty()) {
                  throw new BizException("风险点等级字典未配置");
            }
            if (!enabledRiskPointLevels.contains(normalizedRiskPointLevel)) {
                  throw new BizException("风险点档案等级不在允许范围内");
            }
            return normalizedRiskPointLevel;
      }

        private Set<String> loadEnabledRiskPointLevels() {
              Dict dict = dictService.getByCode(RISK_POINT_LEVEL_DICT_CODE);
              if (dict == null || dict.getItems() == null) {
                    return Set.of();
              }
              return dict.getItems().stream()
                      .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                      .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                      .map(DictItem::getItemValue)
                      .filter(StringUtils::hasText)
                      .map(this::normalizeRiskPointLevel)
                      .filter(StringUtils::hasText)
                      .collect(Collectors.toCollection(LinkedHashSet::new));
        }

      private List<String> buildRiskPointLevelQueryValues(String riskPointLevel) {
            String normalizedRiskPointLevel = normalizeRiskPointLevel(riskPointLevel);
            if (!StringUtils.hasText(normalizedRiskPointLevel)) {
                  return List.of();
            }
            return List.of(normalizedRiskPointLevel);
      }

      private String normalizeRiskPointLevel(String riskPointLevel) {
            if (!StringUtils.hasText(riskPointLevel)) {
                  return "";
            }
            return riskPointLevel.trim().toLowerCase(Locale.ROOT);
      }

      private String normalizeCurrentRiskLevel(String riskLevel) {
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

      private void ensureWritableRiskPointOrg(Long currentUserId, Long orgId) {
            if (!hasDataPermissionSupport() || currentUserId == null || orgId == null || orgId <= 0) {
                  return;
            }
            if (!permissionService.listAccessibleOrganizationIds(currentUserId).contains(orgId)) {
                  throw new BizException("所属组织不在当前账号的数据范围内");
            }
      }

      private void ensureRiskPointAccessible(Long currentUserId, RiskPoint riskPoint) {
            if (!hasDataPermissionSupport() || currentUserId == null || riskPoint == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(riskPoint.getTenantId())) {
                  throw new BizException("风险点不存在或无权访问");
            }
            if (context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.SELF) {
                  if (currentUserId.equals(riskPoint.getResponsibleUser()) || currentUserId.equals(riskPoint.getCreateBy())) {
                        return;
                  }
                  throw new BizException("风险点不存在或无权访问");
            }
            if (riskPoint.getOrgId() == null || !permissionService.listAccessibleOrganizationIds(currentUserId).contains(riskPoint.getOrgId())) {
                  throw new BizException("风险点不存在或无权访问");
            }
      }

      private boolean hasDataPermissionSupport() {
            return permissionService != null;
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
            riskPoint.setRiskPointLevel(normalizeRiskPointLevel(riskPoint.getRiskPointLevel()));
            String currentRiskLevel = resolveCurrentRiskLevel(riskPoint);
            riskPoint.setCurrentRiskLevel(currentRiskLevel);
            riskPoint.setRiskLevel(currentRiskLevel);
            return riskPoint;
      }

      private void saveRiskPointWithGeneratedCode(RiskPoint riskPoint, String orgName) {
            String base = buildRiskPointCodeBase(riskPoint.getRiskPointName(), orgName, riskPoint.getRiskPointLevel());
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
                    buildCodeSegment(riskLevel, 6)
            );
      }

      private String resolveCurrentRiskLevel(RiskPoint riskPoint) {
            if (riskPoint == null) {
                  return DEFAULT_CURRENT_RISK_LEVEL;
            }
            String currentRiskLevel = normalizeCurrentRiskLevel(riskPoint.getCurrentRiskLevel());
            if (StringUtils.hasText(currentRiskLevel)) {
                  return currentRiskLevel;
            }
            String legacyRiskLevel = normalizeCurrentRiskLevel(riskPoint.getRiskLevel());
            return StringUtils.hasText(legacyRiskLevel) ? legacyRiskLevel : DEFAULT_CURRENT_RISK_LEVEL;
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
