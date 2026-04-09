# 列表操作列间距统一设计

> 日期：2026-04-01
> 范围：`spring-boot-iot-ui`
> 主题：统一全系统数据列表“操作”列按钮间距，并兼容移动端自适应

## 1. 背景

当前多个纳管列表页虽然已经接入 `StandardWorkbenchRowActions`、`standard-row-actions-column` 和 `resolveWorkbenchActionColumnWidth`，但桌面端表格“操作”列仍然存在视觉分叉：

- 早期 `设备资产中心` 通过页面私有分布策略和额外列宽下限，形成了和其他台账不同的按钮节奏。
- `产品定义中心`、`链路追踪台`、`异常台账` 等页面虽然也复用共享组件，但此前仍会因为可见动作数量不同，呈现出两套桌面按钮间距。
- 现有守卫只要求“必须使用共享组件”，没有要求“必须使用同一桌面分布策略”，所以问题会反复回流。

## 2. 目标

- 统一纳管列表页桌面端表格“操作”列的按钮间距与列宽基线。
- 以 `设备资产中心` 当前桌面端的按钮密度和可读性作为共享基调，但最终收口为固定共享间距，而不是继续逐页覆写或按动作数量切换分布。
- 保持移动端卡片操作区继续自适应，不能因为桌面收口而破坏触控布局。
- 把这套规则写进共享组件、列宽工具、门禁和文档，避免后续回流。

## 3. 非目标

- 不调整 `editor` 场景下的可换行编辑区按钮组。
- 不重做 `StandardActionLink`、`StandardActionMenu` 的颜色语义。
- 不为个别页面保留第二套桌面“操作”列布局例外。

## 4. 决策

采用“共享组件默认策略 + 共享列宽分层 + 门禁固化”的方案。

### 4.1 共享布局策略

- `StandardWorkbenchRowActions` 的 `table` 变体默认不再依赖页面显式传 `distribution="between"`。
- 桌面表格动作组统一使用固定共享间距；无论可见操作项是 `2` 个还是 `详情 / 编辑 / 更多` 这类 `3` 个，都保持同一套相邻间距。
- 当桌面表格中只有 `1` 个可见操作项时，继续使用 `start`，避免单按钮被强行拉宽。
- `card` 变体继续保持移动端自适应布局，不套用桌面等距规则。

### 4.2 共享列宽分层

- `resolveWorkbenchActionColumnWidth` 在保留自适应测算的同时，补充桌面操作列的统一下限：
  - `1` 个可见动作：继续沿用现有最小下限。
- `2` 个可见动作：统一使用贴近内容的双动作桌面基线宽度，避免短文案动作列右侧留白过大。
  - `2` 个直出动作 + `更多`：统一使用三段式桌面基线宽度。
- 页面不再自行追加 `Math.max(160, ...)` 一类列宽 hack。

## 5. 受影响文件

- `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
- `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- `spring-boot-iot-ui/src/views/AuditLogView.vue`
- `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
- `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- `spring-boot-iot-ui/scripts/list-page-guard.mjs`
- `docs/06-前端开发与CSS规范.md`
- `docs/15-前端优化与治理计划.md`

## 6. 验证方式

- 先修改共享动作组件和列宽工具测试，明确桌面默认分布与列宽分层，并验证测试先失败。
- 再收口共享实现与纳管页面，确认设备、链路追踪、异常台账等页面宽度和分布规则转绿。
- 最后运行 `vitest` 聚焦测试、`component:guard`、`list:guard` 和 `style:guard`，确保规则已被守卫覆盖。

## 7. 风险与控制

- 风险：部分桌面列表页的操作列宽会同步变化。
- 控制：本次就是要把现有页面拉回同一套共享节奏，并通过测试与门禁防止后续再出现“同组件不同风格”。

- 风险：移动端卡片操作区可能意外被桌面规则影响。
- 控制：共享默认分布只对 `table` 变体按可见动作数量自动启用；`card` 变体保持现有触控布局，并通过现有视图测试覆盖。
