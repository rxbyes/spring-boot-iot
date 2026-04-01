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
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 风险点Service实现类
 */
@Service
public class RiskPointServiceImpl extends ServiceImpl<RiskPointMapper, RiskPoint> implements RiskPointService {

      private final RiskPointDeviceMapper riskPointDeviceMapper;
      private final OrganizationService organizationService;

      public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper,
                                  OrganizationService organizationService) {
            this.riskPointDeviceMapper = riskPointDeviceMapper;
            this.organizationService = organizationService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint addRiskPoint(RiskPoint riskPoint) {
            validateRiskPointForWrite(riskPoint);
            Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
            riskPoint.setOrgName(organization.getOrgName());
            riskPoint.setRiskPointCode(generateRiskPointCode(
                    riskPoint.getRiskPointName(),
                    organization.getOrgName(),
                    riskPoint.getRiskLevel()
            ));

            riskPoint.setCreateTime(new Date());
            riskPoint.setUpdateTime(new Date());
            riskPoint.setDeleted(0);
            save(riskPoint);
            return riskPoint;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint updateRiskPoint(RiskPoint riskPoint) {
            if (riskPoint.getId() == null) {
                  throw new BizException("风险点不存在");
            }
            validateRiskPointForWrite(riskPoint);
            RiskPoint existing = getById(riskPoint.getId());
            Organization organization = resolveRequiredOrganization(riskPoint.getOrgId());
            riskPoint.setOrgName(organization.getOrgName());
            riskPoint.setRiskPointCode(existing.getRiskPointCode());

            riskPoint.setUpdateTime(new Date());
            updateById(riskPoint);
            return riskPoint;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRiskPoint(Long id) {
            RiskPoint existing = getById(id);
            if (existing == null) {
                  throw new BizException("风险点不存在");
            }

            // 逻辑删除
            existing.setDeleted(1);
            existing.setUpdateTime(new Date());
            updateById(existing);
      }

      @Override
      public RiskPoint getById(Long id) {
            RiskPoint riskPoint = super.getById(id);
            if (riskPoint == null || riskPoint.getDeleted() == 1) {
                  throw new BizException("风险点不存在");
            }
            return riskPoint;
      }

      @Override
      public List<RiskPoint> listRiskPoints(String riskPointCode, String riskLevel, Integer status) {
            return list(buildRiskPointWrapper(riskPointCode, riskLevel, status));
      }

      @Override
      public PageResult<RiskPoint> pageRiskPoints(String riskPointCode, String riskLevel, Integer status, Long pageNum, Long pageSize) {
            Page<RiskPoint> page = new Page<>(pageNum, pageSize);
            Page<RiskPoint> result = page(page, buildRiskPointWrapper(riskPointCode, riskLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
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
            RiskPointDevice existing = riskPointDeviceMapper.selectOne(queryWrapper);
            if (existing == null) {
                  throw new BizException("设备未绑定到该风险点");
            }

            existing.setDeleted(1);
            existing.setUpdateTime(new Date());
            riskPointDeviceMapper.updateById(existing);
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
                  wrapper.eq(RiskPoint::getRiskLevel, riskLevel);
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

      private String generateRiskPointCode(String riskPointName, String orgName, String riskLevel) {
            String base = String.format(
                    "RP-%s-%s-%s",
                    buildCodeSegment(orgName, 6),
                    buildCodeSegment(riskPointName, 6),
                    buildCodeSegment(riskLevel, 4)
            );
            int suffix = 1;
            String code = appendCodeSuffix(base, suffix);
            while (existsActiveCode(code)) {
                  suffix++;
                  code = appendCodeSuffix(base, suffix);
            }
            return code;
      }

      private boolean existsActiveCode(String code) {
            LambdaQueryWrapper<RiskPoint> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskPoint::getRiskPointCode, code);
            wrapper.eq(RiskPoint::getDeleted, 0);
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
