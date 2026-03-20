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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 风险点Service实现类
 */
@Service
public class RiskPointServiceImpl extends ServiceImpl<RiskPointMapper, RiskPoint> implements RiskPointService {

      private final RiskPointDeviceMapper riskPointDeviceMapper;

      public RiskPointServiceImpl(RiskPointDeviceMapper riskPointDeviceMapper) {
            this.riskPointDeviceMapper = riskPointDeviceMapper;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint addRiskPoint(RiskPoint riskPoint) {
            // 检查风险点编号是否已存在
            LambdaQueryWrapper<RiskPoint> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RiskPoint::getRiskPointCode, riskPoint.getRiskPointCode());
            queryWrapper.eq(RiskPoint::getDeleted, 0);
            RiskPoint existing = getOne(queryWrapper);
            if (existing != null) {
                  throw new BizException("风险点编号已存在");
            }

            riskPoint.setCreateTime(new Date());
            riskPoint.setUpdateTime(new Date());
            riskPoint.setDeleted(0);
            save(riskPoint);
            return riskPoint;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public RiskPoint updateRiskPoint(RiskPoint riskPoint) {
            RiskPoint existing = getById(riskPoint.getId());
            if (existing == null) {
                  throw new BizException("风险点不存在");
            }

            // 检查风险点编号是否冲突
            LambdaQueryWrapper<RiskPoint> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RiskPoint::getRiskPointCode, riskPoint.getRiskPointCode());
            queryWrapper.ne(RiskPoint::getId, riskPoint.getId());
            queryWrapper.eq(RiskPoint::getDeleted, 0);
            RiskPoint existingByCode = getOne(queryWrapper);
            if (existingByCode != null) {
                  throw new BizException("风险点编号已存在");
            }

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
            // 检查是否已绑定
            LambdaQueryWrapper<RiskPointDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RiskPointDevice::getRiskPointId, riskPointDevice.getRiskPointId());
            queryWrapper.eq(RiskPointDevice::getDeviceId, riskPointDevice.getDeviceId());
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
}
