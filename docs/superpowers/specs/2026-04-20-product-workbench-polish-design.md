# 产品定义中心列表与详情精修设计

- 日期：2026-04-20
- 状态：设计已确认，待进入实施计划
- 适用仓库：`spring-boot-iot`
- 相关模块：`spring-boot-iot-ui`

## 1. 背景

`/products` 与 `/products/:productId/*` 已完成主列表 + 五段详情子路由收口，但这轮回看又暴露出四个相互关联的体验问题：

1. `/products` 主列表里，`编辑` 的直达入口不够显眼，用户仍需要绕到工作台语义里找编辑动作。
2. `关联设备` 当前仍是一次性拉全量后前端展示，缺少标准服务端分页。
3. `产品总览` 的字号层级偏大，和 `设备资产中心` 等同类页面相比显得过重，层次不够清楚。
4. 详情页页签上下间距偏紧，标题、页签和内容区之间没有足够呼吸感。

这四个问题本质上不是独立缺陷，而是同一套工作台视觉与交互节奏没有完全收口：主列表的高频动作不够直接，详情页的列表读写节奏不够标准，总览区的字号和页签间距又把页面抬得太满。

## 2. 用户确认后的约束

- `/products` 主列表必须恢复直达 `编辑`。
- `编辑` 继续复用现有列表页编辑抽屉原地编辑，不新增编辑路由，不跳详情页。
- `关联设备` 必须改成标准服务端分页，不能继续前端一次性分页。
- `产品总览` 的字号和层级要向 `设备资产中心` 对齐，不能再单页放大。
- 页签上下间距必须放松，避免内容与导航挤在一起。
- 本次只做前端结构、样式和分页行为收口，不改后端接口语义。

## 3. 方案比较

### 3.1 方案 A：只补 `/products` 的编辑直达

优点：

- 改动最小。

缺点：

- 只能修复列表入口，不解决详情页分页、字号和页签节奏问题。

### 3.2 方案 B：列表直达编辑 + 详情页服务端分页 + 视觉层级收口（推荐）

优点：

- 一次性收口同一产品工作台的“入口、阅读、分页”三类问题。
- 继续沿用现有编辑抽屉和共享分页组件，不引入新交互范式。
- 兼容当前 `/products` 主列表与详情子路由结构。

缺点：

- 需要同时改列表页、详情页、子组件和测试。

### 3.3 方案 C：把编辑改到详情页统一入口

优点：

- 入口更集中。

缺点：

- 会破坏现有列表页原地编辑习惯，也会让 `/products` 失去最直接的高频动作。

## 4. 确认设计

### 4.1 `/products` 主列表恢复直达编辑

主列表的行级操作需要重新显式提供 `编辑`，并且继续调用现有的原地编辑抽屉链路。

- `进入工作台` 继续负责打开产品详情子路由。
- `编辑` 直接打开现有列表页编辑抽屉，和 `进入工作台 / 删除` 一起保持在同一行级动作语义里。
- 不新增编辑详情页，不把编辑动作先导到 `/products/:productId/overview` 再二次进入。
- 现有 `handleEdit` / `openEditWorkbench` / `ProductEditWorkspace` 继续作为编辑真相源。

### 4.2 `关联设备` 改为标准服务端分页

`ProductDetailWorkbenchView.vue` 负责设备页的分页状态和后端请求，`ProductDeviceListWorkspace.vue` 负责分页表格本身。

- 设备页默认页大小为 `10`。
- 页大小选项固定为 `10 / 20 / 50 / 100`。
- 页码切换和页大小切换都必须重新请求后端。
- `ProductDeviceListWorkspace.vue` 继续负责设备表格与分页展示，不再只渲染纯表格。
- 设备列表加载必须使用后端返回的 `total / pageNum / pageSize / records`，不再靠前端切片。

### 4.3 `产品总览` 字号与层级收口

`ProductDetailWorkbench.vue` 的总览区要从“单页大标题卡”收回到“轻量产品摘要”。

- 主标题、指标值、卡片标签三层字号必须拉开差异。
- `产品总览` 不再使用明显高于同类工作台的标题尺寸。
- 数值强调保留，但不能和页面标题抢主视觉。
- 整体视觉目标对齐 `设备资产中心` 的稳定台账感，而不是放大版概览页。

### 4.4 页签上下间距放松

`ProductDetailWorkbenchView.vue` 的页签区需要增加呼吸感。

- hero 到页签之间留出更明确的垂直间距。
- 页签内边距和行高适度放松。
- 页签与正文之间保留稳定分隔，不再显得“贴边排版”。

### 4.5 仍然保留的边界

- 不改 `/products` 主列表筛选、分页、导出和治理提示的现有行为。
- 不改 `overview / devices / contracts / mapping-rules / releases` 的路由结构。
- 不改 `deviceApi.pageDevices` 的接口契约。
- 不改产品编辑抽屉的字段、提交和回填逻辑。

## 5. 影响文件

预计至少涉及：

- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
- `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- `docs/02-业务功能与流程说明.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

`README.md` 与 `AGENTS.md` 预计无需变更，除非实施中发现全局规则需要同步收口。

## 6. 测试与验证

实现阶段至少覆盖以下验证：

- `ProductWorkbenchView` 的行级动作测试需要锁定 `编辑` 重新可见，且点击后仍然打开原地编辑抽屉。
- `ProductDetailWorkbenchView` 的视图测试需要锁定设备页会按当前页码与页大小请求后端，而不是固定 `pageNum=1&pageSize=100`。
- `ProductDeviceListWorkspace` 的组件测试需要锁定分页组件出现、分页事件可驱动重新加载语义。
- `ProductDetailWorkbench` 的组件测试需要锁定总览区标题、指标值和摘要卡的层级仍然存在，并保留可供浏览器核对的结构化 class hook。
- 需要补一轮浏览器核对，重点看 `/products` 主列表、`/products/:productId/overview` 和 `/products/:productId/devices` 三处。

## 7. 文档更新要求

本次实现完成后必须原位更新：

- `docs/02-业务功能与流程说明.md`
  - 明确主列表恢复直达编辑。
  - 明确关联设备改为标准服务端分页。
  - 明确详情页总览和页签节奏已收口。
- `docs/08-变更记录与技术债清单.md`
  - 记录本轮产品定义中心的入口、分页和视觉收口。
- `docs/15-前端优化与治理计划.md`
  - 将“产品定义中心主列表直达编辑”和“详情页关联设备必须走标准服务端分页”沉淀为长期规则。
