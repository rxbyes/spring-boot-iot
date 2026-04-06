# 产品定义中心快速搜索关键词扩展设计

> 日期：2026-04-05
> 范围：`spring-boot-iot-ui`、`spring-boot-iot-device`
> 主题：让 `/products` 页面保留一个快速搜索框，同时支持产品名称、产品 Key、厂商搜索

## 1. 背景

当前 `/products` 页面的快速搜索框虽然复用了 `productName` 这一现有查询字段，但占位文案、本地命中和服务端分页请求没有形成统一的三字段关键词搜索闭环。

这会导致用户输入产品 Key 时无法稳定命中，也会让“厂商搜索”只在局部语义上存在，无法保证服务端分页结果正确。

## 2. 目标

- 保留一个快速搜索框，不新增第二个输入项。
- `/products` 快速搜索统一支持产品名称、产品 Key、厂商三项关键词匹配。
- 保持分页、路由 query、会话缓存和已生效筛选标签语义一致。
- 后端继续保留现有产品分页接口，只补齐可选关键词搜索能力。

## 3. 非目标

- 不重做 `/products` 页面的筛选区结构。
- 不新增单独“厂商”高级筛选字段。
- 不改动产品新增/编辑抽屉或产品治理抽屉。

## 4. 决策

采用“保留现有接口签名、补齐现有快速搜索语义”的收口方案：

- 前端继续复用现有快速搜索状态，不新增第二个字段。
- `/api/device/product/page` 继续沿用现有 `productName` 查询入参，但服务端按 `product_name OR product_key OR manufacturer` 执行模糊匹配。
- 已生效筛选标签与输入框占位文案统一收口为“快速搜索”语义，避免误导成“只查产品名称”。

这样可以保证：

- `/products` 的快速搜索语义和分页结果保持一致。
- 服务端分页结果正确，不依赖当前页本地过滤兜底。
- 改动范围更小，不需要额外扩张产品分页接口签名。

## 5. 受影响文件

- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/productWorkbenchState.ts`
- `spring-boot-iot-ui/src/api/product.ts`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/utils/productWorkbenchState.test.ts`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductController.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductService.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/08-变更记录与技术债清单.md`

## 6. 验证方式

- 先补前端状态测试，确认关键词匹配产品 Key / 名称 / 厂商。
- 再补后端服务测试，确认关键词会走三字段 OR 模糊检索。
- 实现后运行前端定向测试与后端定向测试。

## 7. 风险与控制

- 风险：快速搜索继续复用 `productName` 字段名，后续维护者可能误以为它只查产品名称。
- 控制：把页面文案、接口文档和测试统一写明“三字段快速搜索”语义，避免再次回退。
