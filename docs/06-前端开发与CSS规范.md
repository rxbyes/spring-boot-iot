# 06 前端开发与 CSS 规范

更新时间：2026-03-19

## 1. 技术栈与目录

- 框架：Vue 3 + TypeScript + Vite。
- UI：Element Plus。
- 图表：ECharts。
- 关键目录：
  - `spring-boot-iot-ui/src/views`
  - `spring-boot-iot-ui/src/components`
  - `spring-boot-iot-ui/src/api`
  - `spring-boot-iot-ui/src/router`
  - `spring-boot-iot-ui/src/styles`

## 2. 前端架构基线

### 2.1 鉴权与请求

- 请求入口：`src/api/request.ts`。
- 拦截器：`src/api/interceptors.ts`。
- 行为基线：
  - 自动注入 `Authorization: Bearer <token>`。
  - 当后端返回业务 `code=401` 或 HTTP `401`，统一清理登录态并跳转 `/login`。
  - 统一错误提示与空数据兜底。

### 2.2 路由与权限

- 路由定义：`src/router/index.ts`。
- 登录页：`/login`。
- 除登录页外，业务页面默认要求鉴权。
- 浏览器刷新当前业务路由时，应先回到 SPA 壳层，再由前端优先恢复本地 token + `authContext`；后续若 `/api/auth/me` 或业务接口返回 `401`，再统一跳回 `/login`。
- 访问未授权路由或无资源链接时，前端统一清理登录态并跳转 `/login`，不保留原始 401 页面。
- 菜单与按钮权限来源：后端动态菜单与权限编码。

## 3. 页面开发统一模式

根据现有共享能力，新增页面优先复用以下标准件：

1. `PanelCard`：卡片容器与页内分区。
2. `MetricCard`：统一概览页、报表页、工作台概况区的 KPI 卡片。
3. `StandardPagination`：统一分页组件。
4. `useServerPagination`：统一分页状态与翻页行为。
5. `StandardTableToolbar`：统一列表操作条与统计信息展示。
6. `StandardTableTextColumn`：统一长文本列省略与悬浮展示完整内容。
7. `StandardInfoGrid`：统一工作台查询结果、协议预演等只读摘要卡片布局。
8. `StandardFlowRail`：统一流程导引、检查步骤、路线衔接等纵向步骤条布局。
9. `StandardInlineSectionHeader`：统一编辑器、编排页、复杂表单中的区块标题与右侧轻操作入口。
10. `StandardActionGroup`：统一工作台、表单区、编排页和卡片操作按钮组的排列、换行与常用间距。
11. 全局列表样式（`standard-list-view` 体系）。
12. 统一详情抽屉：`StandardDetailDrawer`。
13. 统一表单抽屉：`StandardFormDrawer`。
14. `StandardDrawerFooter` + `confirmAction`：统一新增/修改/删除/确认类底部按钮和二次确认交互。

禁止：
- 在同类列表页重复造分页样式和状态管理。
- 在列表页重复实现“表格工具条 + 文本溢出 tooltip + 删除确认弹窗 + 抽屉底部按钮”。
- 同时存在“本地散装分页”与“标准分页契约”的双轨实现。

## 4. CSS 与视觉 Token 规范

### 4.1 Token 文件

- `src/styles/tokens.css`：颜色、字体、圆角、阴影、间距、过渡。
- `src/styles/global.css`：全局布局、公共组件样式、列表基线。
- `src/styles/element-overrides.css`：Element Plus 主题映射。

### 4.2 主色与主题

- 当前主品牌色通过 `--brand` 及其衍生变量统一控制。
- Element Plus 主色通过 `--el-color-primary` 等变量映射到 `--brand`。

### 4.3 列表页约定

- 结构：筛选区 -> 操作栏 -> 表格区 -> 分页区。
- 五个一级工作台下的功能页顶部导航统一为仅保留阿里云式面包屑的极简结构；不得再重复渲染最近访问标签栏、主标题、同组横向切换，以及 `当前分区`、`页面类型`、`可见菜单`、`返回概览` 这类说明性文案。
- 页面说明文案只在分组首页、空态页或确有必要的引导场景保留；普通功能页默认不再展示“列表/筛选/详情抽屉”一类低信息密度说明。
- 长文本默认通过 `StandardTableTextColumn` 单行省略，悬浮显示完整内容。
- 表格操作和导出行为保持统一入口。
- 概览页、报表页、工作台概况区的 KPI 卡优先通过 `MetricCard` 统一数值层级、徽标和卡片密度。
- 查询结果、协议预演、只读摘要卡优先通过 `StandardInfoGrid` 统一信息密度、对齐和长值展示策略。
- 流程导引、发送后检查、路线桥接等步骤型内容优先通过 `StandardFlowRail` 统一索引、标题和说明布局；不再在页面内重复手写 `flow-rail` 模板。
- 工作台、表单和编排页中的按钮组优先通过 `StandardActionGroup` 统一，不再继续保留 `button-row + 内联 style`、`action-row + 局部样式` 的双轨实现。
- 场景/步骤编排编辑器中的区块头、场景卡头、步骤卡头等“标题 + 轻操作”结构优先通过 `StandardInlineSectionHeader` 统一；不再新增页内独立的 `inline-block__header`、`scenario-card__header`、`step-editor__header` 一类样式实现。
- 共享组件接管后，必须在同一变更中同步删除页面内对应的旧结构 class 与伴生 scoped CSS；禁止保留失效的 `info-grid / info-chip / button-row / flow-rail` 等遗留样式造成“模板已统一、CSS 仍分叉”。

### 4.4 概览页与抽屉配色约定

- 分组概览、风险处置工作台、详情抽屉、表单抽屉与确认弹窗统一复用 `--brand`、`--accent`、`--panel-border` 等全局 token。
- 不再在概览页或工作台局部写死另一套蓝色主题；局部强调色只能作为 `--accent` 的补充，而不是替换整页品牌色。
- 如需局部例外主题，必须在 `docs/15-frontend-optimization-plan.md` 记录原因、影响范围和回退策略。

## 5. 文本与编码规范

- 统一 UTF-8。
- 禁止提交乱码文本（如 `鍒�`、`褰�`、`璇�`、`鐢�`）。
- 涉及文案修改时必须自检中文可读性。

## 6. 回归检查清单（前端改动后）

1. 登录态恢复、401 回退流程是否正常。
2. 菜单与按钮权限是否按角色生效。
3. 列表分页是否统一使用 `StandardPagination` + `useServerPagination`。
4. 列表操作条和长文本列是否使用 `StandardTableToolbar` + `StandardTableTextColumn`。
5. 摘要卡、流程导引和按钮组是否分别使用 `StandardInfoGrid`、`StandardFlowRail`、`StandardActionGroup`。
6. 详情/表单交互是否使用 `StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter` 和 `confirmAction`。
7. 视觉 Token 是否复用，无页面级“临时色值漂移”。
8. 共享组件替换后，页面私有结构类和失效 scoped CSS 是否已同步清理。
9. 功能页顶部是否仍残留最近访问标签栏、主标题、同组横向导航或“当前分区 / 页面类型 / 可见菜单 / 返回概览”等重复说明文案；若有，必须在交付前删除。

## 7. 已知问题与技术债

- `RealTimeMonitoringView`、`UserView`、`RoleView`、`OrganizationView`、`RegionView`、`DictView`、`ChannelView`、`MessageTraceView`、`AuditLogView`、`RiskPointView`、`RuleDefinitionView`、`LinkageRuleView`、`EmergencyPlanView`、`RiskGisView`、`AutomationTestCenterView`、`DeviceInsightView`、`ProductWorkbenchView`、`DeviceWorkbenchView`、`FilePayloadDebugView`、`ReportWorkbenchView`、`FutureLabView` 已完成当前批次的统一组件化迁移。
- 当前剩余债务主要集中在自动化测试中心执行配置与页面盘点的父级状态编排继续收口，以及后续新增页面若继续直接书写原生工具栏/文本列/确认弹窗的防回退治理。
- `【待确认】` 前端是否已建立强制 lint/style 检查门禁；仓库中未见明确质量闸门说明。
- `【文档缺失】` 缺少“前端组件分层与命名规则”正式文档（如 atoms/molecules/templates 级别约定）。
