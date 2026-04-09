# 查询按钮配色对齐设计

> 日期：2026-03-29
> 范围：`spring-boot-iot-ui`
> 主题：让全局列表页“查询”按钮与对象洞察台“刷新对象洞察”按钮使用同一主按钮配色

## 1. 背景

当前全局列表页中的查询按钮大多通过 `StandardButton action="query"` 渲染，而对象洞察台中的“刷新对象洞察”按钮通过 `StandardButton action="confirm"` 渲染。

共享按钮组件中，`query` 额外走了 `palette-query` 配色分支，`confirm` 走默认主按钮分支，因此两类按钮虽然都属于主操作，但颜色不一致。

## 2. 目标

- 让全局列表页查询按钮与对象洞察台刷新按钮颜色一致。
- 保持改动集中在共享按钮组件，避免逐页覆写样式。
- 同步更新测试与前端规范文档，防止后续回退。

## 3. 非目标

- 不调整对象洞察台“刷新对象洞察”按钮本身的文案、语义或位置。
- 不重做全部按钮体系，也不新增第三套“查询主按钮”语义。
- 不修改非 `action="query"` 的其它页面私有按钮。

## 4. 决策

采用共享组件收口方案：

- 保留 `StandardButton` 的 `query` 语义和 `primary + solid` 层级。
- 取消 `query` 的专属 `palette-query` 配色分支。
- 让 `query` 与 `confirm` 一起复用默认主按钮 token。

这样可以一次性覆盖所有已经规范使用 `action="query"` 的全局列表页，而不需要逐页修改模板。

## 5. 受影响文件

- `spring-boot-iot-ui/src/components/StandardButton.vue`
- `spring-boot-iot-ui/src/__tests__/components/StandardButton.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/15-前端优化与治理计划.md`

## 6. 验证方式

- 先修改 `StandardButton` 单测，让查询按钮不再断言 `palette-query`，并验证测试先失败。
- 再修改共享按钮实现，使测试转绿。
- 最后运行按钮单测与前端构建，确认没有引入回归。

## 7. 风险与控制

- 风险：现有所有 `action="query"` 的按钮会同步改变颜色。
- 控制：这是本次目标范围，且与现行对象洞察主按钮对齐；通过共享组件测试和文档同步固化新的视觉规则。
