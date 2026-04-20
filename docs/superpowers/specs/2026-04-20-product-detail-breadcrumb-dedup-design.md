# 产品详情工作台面包屑去重设计

- 日期：2026-04-20
- 状态：设计已确认，待进入实施计划
- 适用仓库：`spring-boot-iot`
- 相关模块：`spring-boot-iot-ui`

## 1. 背景

`/products/:productId/*` 在 2026-04-19 完成五段子路由拆分后，信息架构已经清楚，但详情页首屏重新出现了层级回流：

1. 全局壳层 `ShellBreadcrumb` 已经展示当前入口，例如 `接入智维 / 产品总览`。
2. `ProductDetailWorkbenchView.vue` 又通过 `StandardPageShell` 额外渲染了一条页内 breadcrumb：`产品定义中心 / 产品名 / 当前子页`。
3. `StandardPageShell` headline 仍带有泛化眉题 `产品工作区`，而下方 hero 又再次展示产品名、产品 Key 和说明。

结果不是缺信息，而是同一层语义被连续重复表达，首屏阅读顺序被面包屑、眉题、产品名三次打断。当前问题已经直接影响验收观感，也违背了仓库里已经明确收口的“壳层面包屑 + 业务标题/主工作区”原则。

## 2. 用户确认后的约束

- 只保留全局壳层面包屑，不保留详情页内第二条 breadcrumb。
- 不改 `/products` 主列表入口，不改五段子路由，不改后端接口。
- 产品身份信息仍需保留，但只能收口到同一处，不允许页头和 hero 双写。
- 当前优化属于页面结构治理，必须同步更新现有文档，不新增平行说明文档。

## 3. 目标

1. 让产品详情页顶部重新回到单一导航真相源。
2. 让“当前子页语义”和“产品身份语义”各自只出现一次。
3. 保持 `/products/:productId/overview|devices|contracts|mapping-rules|releases` 的现有交互和数据加载逻辑不变。
4. 让详情页结构继续符合 `接入智维` 已确定的轻量页壳合同，避免再次回流三层抬头。

## 4. 非目标

- 不调整产品详情 hero 的指标口径和数据来源。
- 不改页签结构，不新增第六个子页，也不把五段子页回并到抽屉。
- 不修改全局壳层 `AppShell`、`ShellBreadcrumb` 或路由 meta 的真相源。
- 不涉及 `spring-boot-iot-device`、`spring-boot-iot-admin` 等后端模块。

## 5. 方案比较

### 5.1 方案 A：只删详情页内 breadcrumb

- `StandardPageShell` 不再渲染页内 breadcrumb。
- 其余眉题、标题和 hero 结构保持原样。

优点：

- 改动最小，风险低。

缺点：

- `产品工作区` 泛化眉题仍会保留。
- 页头标题与 hero 中的产品名仍然连续重复，首屏层级仍然偏厚。

### 5.2 方案 B：只保留全局壳层面包屑，并把页头改成“当前子页标题 + 动作”，产品身份只留在 hero（推荐）

- `StandardPageShell` 不再渲染页内 breadcrumb。
- 移除 `eyebrow="产品工作区"` 这类泛化眉题。
- `StandardPageShell` 只承接当前子页标题、说明和右侧动作。
- hero 成为唯一产品身份入口，保留 `productKey / productName / description / 指标卡`。

优点：

- 导航、页面标题、产品身份三层职责清晰。
- 改动只落在当前详情页，不影响全局壳层和路由真相。
- 与仓库既有“去重复眉题、去第二条面包屑”的治理方向一致。

缺点：

- 需要同时调整页面模板和对应测试，而不只是删除一段 markup。

### 5.3 方案 C：进一步去掉页头标题，只保留 hero 和动作

- 详情页顶部只保留全局壳层面包屑。
- `StandardPageShell` headline 基本清空，动作并入 hero。

优点：

- 视觉最平。

缺点：

- 会让产品详情页脱离共享页壳节奏。
- 不利于与其他工作台维持一致的“页面标题 + 页面动作”合同。

### 5.4 推荐结论

采用方案 B。它同时解决重复 breadcrumb、重复眉题和重复产品名三类问题，而且不破坏既有路由和壳层真相。

## 6. 已确认设计

### 6.1 顶部层级

详情页首屏固定收口为以下顺序：

1. 全局壳层 breadcrumb
2. 当前子页标题 + 子页说明 + 右侧动作
3. 产品身份 hero
4. 页签导航
5. 对应子页正文

禁止再次出现：

- 第二条页内 breadcrumb
- 泛化眉题 `产品工作区`
- 页头与 hero 连续重复同一个产品名

### 6.2 页头职责

`StandardPageShell` 在产品详情页中只承担以下职责：

- 标题：当前子页名
  - `产品总览`
  - `关联设备`
  - `契约字段`
  - `映射规则`
  - `版本台账`
- 描述：当前子页说明文案
- 右侧动作：`返回列表`、`刷新`

`StandardPageShell` 不再承担产品身份信息，也不再承载 breadcrumb 或眉题。

### 6.3 产品身份 hero

hero 保持为产品身份唯一真相源，继续承接：

- `productKey`
- `productName`
- `description`
- 三张产品摘要指标卡

hero 不再和页头争抢“当前页名”语义；它只回答“当前是哪个产品”，不回答“当前在哪个子页”。

### 6.4 路由与壳层边界

- 路由 `meta.title` 继续服务全局壳层 breadcrumb，无需改动。
- `ProductDetailWorkbenchView.vue` 不再传入 `show-breadcrumbs` 与 `breadcrumbs`。
- `AppShell.vue`、`ShellBreadcrumb.vue`、`router/index.ts` 本轮都不是必须改动对象，除非实现时发现测试桩依赖需要最小补位。

### 6.5 加载与异常态

- 详情页在 `loading && !product` 时仍显示当前加载态。
- `errorMessage && !product` 时仍显示错误态。
- 上述状态下不引入新的导航层，也不把产品名临时写回页头。

## 7. 影响文件

预计至少涉及：

- `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
- `spring-boot-iot-ui/src/__tests__/views/` 下的产品详情页测试文件
- `docs/02-业务功能与流程说明.md`
- `docs/08-变更记录与技术债清单.md`
- 如需补充“详情页禁止页内 breadcrumb / 泛化眉题回流”的长期规则，则同步更新 `docs/15-前端优化与治理计划.md`

## 8. 测试与验证

实现阶段至少覆盖以下验证：

- 新增或补齐产品详情页视图测试，锁定：
  - 不再给 `StandardPageShell` 传 `show-breadcrumbs`
  - 不再传 `eyebrow="产品工作区"`
  - 页头标题为当前子页名，而不是产品名
  - hero 中仍保留产品名与产品 Key
- 回归现有 `StandardPageShell` 组件测试，确保共享页壳合同未被破坏。
- 回归与产品工作台相关的列表/路由测试，确保从 `/products` 进入详情页的基本流程不受影响。

## 9. 文档更新要求

本次实现完成后必须原位更新：

- `docs/02-业务功能与流程说明.md`
  - 明确产品详情页只保留全局壳层 breadcrumb，不再额外渲染页内 breadcrumb。
- `docs/08-变更记录与技术债清单.md`
  - 记录本次页面结构收口与重复导航修复。
- `docs/15-前端优化与治理计划.md`
  - 若本轮把“详情页不得重复渲染 breadcrumb 与泛化眉题”沉淀为通用规则，则同步写入。
