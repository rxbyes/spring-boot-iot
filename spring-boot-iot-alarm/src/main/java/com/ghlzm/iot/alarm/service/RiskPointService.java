package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;

import java.util.List;

/**
 * 风险点Service
 */
public interface RiskPointService extends IService<RiskPoint> {

      /**
       * 新增风险点
       */
      RiskPoint addRiskPoint(RiskPoint riskPoint);

      /**
       * 更新风险点
       */
      RiskPoint updateRiskPoint(RiskPoint riskPoint);

      /**
       * 删除风险点
       */
      void deleteRiskPoint(Long id);

      /**
       * 根据ID查询风险点
       */
      RiskPoint getById(Long id);

      /**
       * 查询风险点列表
       */
      List<RiskPoint> listRiskPoints(String riskPointCode, String riskLevel, Integer status);

      /**
       * 绑定风险点与设备
       */
      void bindDevice(RiskPointDevice riskPointDevice);

      /**
       * 解绑风险点与设备
       */
      void unbindDevice(Long riskPointId, Long deviceId);

      /**
       * 查询风险点绑定的设备列表
       */
      List<RiskPointDevice> listBoundDevices(Long riskPointId);
}
