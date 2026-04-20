# 产品详情工作台选项卡友好化设计

- 日期：2026-04-20
- 状态：设计已确认，待进入实施计划
- 适用仓库：`spring-boot-iot`
- 相关模块：`spring-boot-iot-ui`

## 1. 背景

`/products/:productId/overview|devices|contracts|mapping-rules|releases` 已完成五段详情子路由拆分，也已经在上一轮修复了全局壳层 breadcrumb 去重、主列表直达编辑、关联设备服务端分页和总览区字号初步收口。

这一轮浏览器回看，仍然暴露出三个连续的视觉问题：

1. 子页导航仍然使用“两行标题 + 缩写副标题”的表达，`产品总览 / 关联设备 / 契约字段 / 映射规则 / 版本台账` 下方的 `概览 / 设备 / 契约 / 映射 / 版本` 语义重复，反而让 tab 区显得拥挤。
2. 当前 tab 样式更像信息卡片，而不是“当前工作台视图”的真选项卡，切换友好度不够。
3. hero 区右侧三项指标里，`最新批次` 抢占了首屏权重；同时页头标题和产品名仍然偏大，和已经收紧的其他工作台层级不完全一致。

这轮不是信息架构调整，而是对产品详情工作台首屏层级的第二次收口：让路由不变、内容不丢，但把导航、标题和指标权重重新排平。

## 2. 用户确认后的范围

- 本次只优化产品详情工作台首屏视觉，不改五段子路由结构。
- 5 个入口都要取消缩写副标题，只保留主标签。
- tab 必须设计成更友好的选项卡，而不是继续保留当前双行卡片式导航。
- hero 区只保留 `关联设备` 与 `正式字段` 两项指标。
- `最新批次` 不再留在 hero 区，但总览页里的发布时间、发布字段数等信息继续保留。
- 页头标题和产品名需要进一步收紧字号层级。
- 不改 `/products` 主列表编辑抽屉、不改关联设备服务端分页方案、不改后端接口。

## 3. 方案比较

### 3.1 方案 A：只删除 tab 副标题

优点：

- 改动最小。

缺点：

- 只能去掉冗余文案，无法解决 tab 仍像卡片、切换体验不够友好的问题。
- hero 区的首屏权重问题仍然存在。

### 3.2 方案 B：保留现有子路由与 `RouterLink`，重做为单行真选项卡，并同步收口 hero 指标与标题层级（推荐）

优点：

- 不改变路由真相和深链行为。
- 只在现有页面内重排视觉层级，风险最低。
- 能一次性解决 tab、hero 指标和标题字号三类问题。

缺点：

- 需要同时调整模板、样式和测试断言。

### 3.3 方案 C：直接切到 `IotAccessTabWorkspace` 同款细线页签

优点：

- 复用现成组件最多。

缺点：

- 这套页签更适合诊断页或轻量模式切换，视觉权重过轻。
- 放在产品详情主工作台里，容易显得像工具栏，而不是当前主视图导航。

## 4. 确认设计

### 4.1 页面层级

`StandardPageShell` 继续承接当前子页标题和说明，不改全局壳层规则。

- 页面标题仍然按当前子路由显示 `产品总览 / 关联设备 / 契约字段 / 映射规则 / 版本台账`。
- 页面标题字号不做全局覆写，只通过本页 hero 区和导航区收口首屏权重。
- hero 区的产品名进一步缩小，从“接近页级标题”收回到“产品身份标题”。
- 产品描述继续存在，但保持说明文字语气，不与导航争主视觉。

### 4.2 hero 指标

hero 区只保留两张高频指标卡：

- `关联设备`
- `正式字段`

具体规则：

- `最新批次` 从 hero 区移除。
- `最新批次` 相关信息继续留在总览内容区的 `最新发布时间 / 最近发布字段数` 等卡片中。
- 两张指标卡宽度、内边距和数值字号按“双卡首屏”重新分配，避免右侧出现过宽留白。

### 4.3 选项卡导航

产品详情页顶部导航改成单行主标签的真选项卡。

- 仅保留主标签：
  - `产品总览`
  - `关联设备`
  - `契约字段`
  - `映射规则`
  - `版本台账`
- 删除所有副标题缩写，不再显示 `概览 / 设备 / 契约 / 映射 / 版本`。
- 继续使用当前子路由与 `RouterLink` 跳转，不改 URL。
- tab 容器采用浅底、轻描边的导航条语法，tab 本身采用圆角分段卡表达。
- 默认态为白底或近白底，hover 只做轻微上浮与品牌浅底提示。
- 激活态通过品牌浅色填充、文字加深和更明确的边框表达“当前所在子页”。
- tab 内只保留单行文字，增加垂直内边距和点击热区，让切换更像“选项卡”，而不是压缩信息块。

### 4.4 间距与响应式

tab 区需要从“密集堆叠”调整为“轻分区”。

- hero 与 tab 之间增加稳定的垂直留白。
- tab 与正文内容区之间保留明确分隔距离。
- 桌面端优先单行自适应排列。
- 中等屏宽允许切成两列，避免文案挤压。
- 小屏下切成单列，保证点按区域与可读性。

### 4.5 仍然保留的边界

- 不改 `/products/:productId/*` 路由结构与兼容深链。
- 不改 `ProductDeviceListWorkspace` 的服务端分页契约。
- 不改 `ProductModelDesignerWorkspace` 在 `contracts / mapping-rules / releases` 子页中的内容逻辑。
- 不把 hero 区扩展成新的经营摘要墙，不新增 badge、状态条或第二行导航。
- 不改 `/products` 主列表的原地编辑抽屉链路。

## 5. 实施触点

预计至少涉及：

- `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
- `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- `docs/02-业务功能与流程说明.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

`README.md` 与 `AGENTS.md` 预计无需改动，除非实施中发现需要补充全局约束。

## 6. 测试与验证

实现阶段至少覆盖以下验证：

- `ProductDetailWorkbenchView` 视图测试需要锁定：
  - hero 指标只剩 `关联设备 / 正式字段`
  - tab 不再渲染缩写副标题
  - 5 个子路由入口仍然完整可见
- `ProductDetailWorkbench` 组件测试需要锁定总览卡仍保持现有数据表达，不因标题和字号收口丢失内容节点。
- `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- `npm --prefix spring-boot-iot-ui run build`
- 浏览器核对重点为：
  - `/products/:productId/overview`
  - `/products/:productId/devices`
  - `/products/:productId/contracts`
  - `/products/:productId/mapping-rules`
  - `/products/:productId/releases`

## 7. 文档更新要求

实现完成后必须原位更新：

- `docs/02-业务功能与流程说明.md`
  - 补充产品详情页已切换为单行真选项卡表达。
  - 补充 hero 区只保留 `关联设备 / 正式字段` 两项高频指标。
- `docs/08-变更记录与技术债清单.md`
  - 记录本轮产品详情工作台首屏导航和 hero 层级收口。
- `docs/15-前端优化与治理计划.md`
  - 沉淀“产品定义中心详情页 tab 不再使用重复缩写副标题”的长期规则。
  - 沉淀“产品详情首屏 hero 指标优先保留高频经营量，不堆叠低频发布信息”的长期规则。
