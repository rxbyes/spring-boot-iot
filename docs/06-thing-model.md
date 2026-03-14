# 物模型说明

## 物模型组成
- 属性 Property
- 事件 Event
- 服务 Service

## 示例：温湿度传感器
### 属性
- temperature：double，单位 ℃
- humidity：double，单位 %

### 事件
- overheat：超温告警

### 服务
- reboot：重启设备

## 设计原则
- 产品定义物模型，设备继承产品模型
- 设备上报属性尽量映射到物模型标识符
- 规则引擎、告警、前端展示统一依赖物模型
