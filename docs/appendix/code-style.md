# 代码规范

## 分层职责
- controller：只接收入参与返回结果
- service：负责业务编排
- mapper：负责数据库访问
- protocol：负责协议转换
- message：负责接入与消息分发

## 异常规范
- 业务异常统一使用 BizException
- 全局异常统一由 GlobalExceptionHandler 处理

## 返回规范
- 接口统一使用 R 返回
- 分页统一使用 PageResult
