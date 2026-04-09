# 数据字典字典项接口缺口设计

**日期：** 2026-04-02

## 背景

平台治理的“数据字典”页面在点击字典项时，会调用 `GET /api/dict/{dictId}/items` 读取字典项列表，并在同一抽屉内继续使用新增、编辑、删除字典项接口。

当前前端契约已经存在，但后端 `spring-boot-iot-system` 仅实现了字典主表 CRUD，没有实现字典项 CRUD，导致点击字典项时请求落空，前端统一显示“系统繁忙，请稍后重试！”。

## 目标

补齐字典项后端接口，使现有前端页面可以在不改请求契约的前提下完成字典项查询、新增、编辑、删除。

## 方案

1. 保持前端 `spring-boot-iot-ui/src/api/dict.ts` 的接口路径不变。
2. 在 `DictController` 中补齐以下接口：
   - `GET /api/dict/{dictId}/items`
   - `POST /api/dict/{dictId}/items`
   - `PUT /api/dict/{dictId}/items`
   - `DELETE /api/dict/items/{id}`
3. 继续由 `DictService`/`DictServiceImpl` 承载字典项服务逻辑，避免为本次缺陷新增不必要的层级。
4. 字典项列表排序与现有 `getByCode` 保持一致，按 `sortNo`、`id` 升序返回。
5. 新增/编辑时补齐最小校验：
   - 所属字典必须存在且未删除。
   - 同一租户、同一字典下 `itemValue` 不允许重复，和数据库唯一键保持一致。
   - 更新时字典项必须存在。
6. 删除继续使用逻辑删除能力，沿用 MyBatis Plus `removeById`。

## 测试与验证

1. 先补 `DictController` 路由测试，复现原始缺口。
2. 再补 `DictServiceImpl` 字典项行为测试，覆盖查询排序、重复校验、字典不存在和删除路径。
3. 最后运行 `spring-boot-iot-system` 定向测试验证红绿闭环。

## 文档影响

需要同步更新：

- `docs/03-接口规范与接口清单.md`
- `docs/08-变更记录与技术债清单.md`
