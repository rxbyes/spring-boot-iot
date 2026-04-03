package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.common.response.PageResult;

import java.util.List;

/**
 * 风险点Service
 */
public interface RiskPointService extends IService<RiskPoint> {

      /**
       * 新增风险点
       */
      RiskPoint addRiskPoint(RiskPoint riskPoint, Long currentUserId);

      /**
       * 更新风险点
       */
      RiskPoint updateRiskPoint(RiskPoint riskPoint, Long currentUserId);

      /**
       * 删除风险点
       */
      void deleteRiskPoint(Long id);

      void deleteRiskPoint(Long id, Long currentUserId);

      /**
       * 根据ID查询风险点
       */
      RiskPoint getById(Long id);

      RiskPoint getById(Long id, Long currentUserId);

      /**
       * 查询风险点列表
       */
      List<RiskPoint> listRiskPoints(String riskPointCode, String riskPointLevel, Integer status);

      List<RiskPoint> listRiskPoints(Long currentUserId, String riskPointCode, String riskPointLevel, Integer status);

      /**
       * 分页查询风险点列表
       */
      PageResult<RiskPoint> pageRiskPoints(String riskPointCode, String riskPointLevel, Integer status, Long pageNum, Long pageSize);

      PageResult<RiskPoint> pageRiskPoints(Long currentUserId, String riskPointCode, String riskPointLevel, Integer status, Long pageNum, Long pageSize);

      /**
       * 绑定风险点与设备
       */
      void bindDevice(RiskPointDevice riskPointDevice);

      void bindDevice(RiskPointDevice riskPointDevice, Long currentUserId);

      RiskPointDevice bindDeviceAndReturn(RiskPointDevice riskPointDevice, Long currentUserId);

      /**
       * 解绑风险点与设备
       */
      void unbindDevice(Long riskPointId, Long deviceId);

      void unbindDevice(Long riskPointId, Long deviceId, Long currentUserId);

      /**
       * 查询风险点绑定的设备列表
       */
      List<RiskPointDevice> listBoundDevices(Long riskPointId);

      List<RiskPointDevice> listBoundDevices(Long riskPointId, Long currentUserId);
}
