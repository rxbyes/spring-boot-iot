# 系统治理逻辑删除补漏设计

## 背景

2026-04-02 的真实环境验收已经确认：`用户/角色` 删除接口此前存在“接口返回成功，但记录未真正进入逻辑删除态”的问题，根因是带 `@TableLogic` 的实体仍在服务层使用 `setDeleted(1) + updateById(...)`。

继续横向排查 `spring-boot-iot-system` 后，当前同类风险还存在于：

- `HelpDocumentServiceImpl#deleteDocument`
- `InAppMessageServiceImpl#deleteMessage`

这两个能力都属于系统治理闭环的一部分，若继续保留当前写法，会导致平台治理页删除反馈与真实数据库状态不一致。

## 目标

把帮助文档与站内消息的删除实现统一收口到 MyBatis-Plus 逻辑删除路径，确保：

- 删除接口返回成功时，记录真实进入逻辑删除态。
- 原有的业务前置校验不变。
- 现有查询接口继续通过 `deleted=0` 自动过滤已删记录。

## 方案

采用与本轮 `用户/角色` 修复相同的最小方案：

1. 保留现有“先查存在性/可删性，再删除”的服务边界。
2. 把 `setDeleted(1) + mapper.updateById(...)` 改为 `removeById(id)`。
3. 当 `removeById(id)` 返回 `false` 时抛出明确业务错误，避免接口静默成功。
4. 先补单元测试锁定“必须调用逻辑删除入口，而不是 updateById”。

## 范围

只处理以下文件，不扩展到其它模块：

- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImpl.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/InAppMessageServiceImpl.java`
- 对应测试文件
- `docs/08-变更记录与技术债清单.md`

## 非目标

- 不改帮助文档和站内消息的权限口径。
- 不改自动消息“只能查看或停用”的业务规则。
- 不在本轮扩展到 `spring-boot-iot-alarm` 或其它模块的全量删除治理。

## 验证

需要完成三层验证：

1. 单元测试红绿，证明删除逻辑从 `updateById` 切换到 `removeById`。
2. `spring-boot-iot-system` 相关测试组合回归通过。
3. 真实环境 `10099` 侧实例上完成最小删除复核，确认记录删后不可再被查询接口读到。
