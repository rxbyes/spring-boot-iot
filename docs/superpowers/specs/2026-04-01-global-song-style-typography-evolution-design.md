# Global Song-Style Typography Evolution Design

**Date:** 2026-04-01

**Scope:** `spring-boot-iot-ui` 全站中文排版基线

## Goal

在当前“全站已切到宋感字体栈”的基础上继续进化，不把这次优化停留在“单纯换字体”。目标是把整个系统的排版气质统一提升到“庄重、克制、清晰”的政企刊物感，同时保留业务系统所需的高可读性和高扫描效率。

## Confirmed Direction

- 用户目标：系统所有页面都带宋感字体气质，接近附件示例中的庄重中文标题效果
- 最终采用：`B. 强标题宋感，正文克制宋感`
- 已确认原则：
  - 全站继续使用统一宋感字体栈
  - 技术文本继续保留等宽字体
  - 不走“全站所有字都同强度宋体”的方向
  - 不是海报站，而是业务系统，需要保留表格、表单、工具条的扫描效率

## Why This Direction

如果标题、正文、表格、表单全部使用同一强度的宋体表达，页面会出现三个问题：

1. 信息密度高的列表和表单会发闷，阅读成本上升
2. 真正需要被看见的标题层级反而不够突出
3. 整体会更像静态印刷品，而不是可操作的经营系统

因此最稳妥的做法是：保留统一宋感基线，但把“宋感强度”做层级分配，而不是平均分配。

## Visual Grammar

### 1. Title System

一级标题、抽屉标题、工作台标题使用最强的宋感表达：

- 更明确的字体尺寸梯度
- 更克制但更稳的字重
- 更短的行高
- 更少的字距噪音

这类标题要接近“刊头”效果，承担视觉压场职责。

### 2. Secondary Hierarchy

二级区块标题、页签标题、表头、工具条标题使用中等强度的宋感表达：

- 保留 serif 气质
- 降低字号冲击
- 提高可扫读性

这一层负责承上启下，不能和一级标题抢焦点。

### 3. Body And Dense Content

正文、表格单元格、表单内容、筛选项、说明文字继续沿用宋感字体栈，但表现必须更克制：

- 字号略收紧
- 行高更稳定
- 字重不再偏重
- 优先保证字段和值的扫描效率

这部分目标不是“更有设计感”，而是“更顺、更稳、更耐看”。

### 4. Technical Text Exception

以下内容继续保留 monospace：

- TraceId
- Topic
- JSON / payload
- 代码片段
- 日志明细
- 调试响应

这些区域不参与宋感强化，避免技术信息失真。

## Implementation Scope

### A. Global Tokens

统一收口到：

- `src/styles/tokens.css`
- `src/styles/global.css`
- `src/styles/element-overrides.css`

通过 token 驱动全站标题、正文、表头、按钮、表单、Element Plus 组件，不做逐页硬编码铺改。

### B. Typography Scale

新增或重构以下排版基线：

- 一级标题字号、字重、行高
- 二级标题字号、字重、行高
- 正文字号与行高
- 表格表头字号与字重
- 按钮与页签字号
- caption / meta / 辅助说明文字等级

### C. High-Impact Shared Surfaces

优先影响以下共享层：

- `StandardPageShell`
- `StandardWorkbenchPanel`
- `StandardDetailDrawer`
- `StandardFormDrawer`
- `StandardListFilterHeader`
- `StandardTableToolbar`
- `StandardWorkbenchRowActions`
- 全局 `body / h1-h4 / p / button / input / textarea / select`

### D. Verification

需要补齐至少一条排版 token 合同测试，确保：

- `--font-display`
- `--font-body`
- `--font-mono`
- `global.css`
- `element-overrides.css`

之间的全局关系不会被后续样式调整破坏。

## Non-Goals

本轮不做：

- 新配色方案
- 新主题模式
- 大规模逐页私有样式重写
- 引入重型在线字体依赖
- 改变现有信息架构或业务交互

## Acceptance Criteria

完成后应满足：

1. 全站标题层级比当前更稳，明显更接近“刊物式”气质
2. 正文、表格、表单没有因为宋感增强而变得难读
3. 技术文本仍保持等宽，不被全局宋感误伤
4. 样式守卫、构建和相关测试全部通过
5. 文档中明确记录“全站宋感排版基线”规则

## Risks

- 如果标题和正文都加重，会让页面变得沉闷
- 如果标题梯度拉得不够开，用户只会感知为“字体变了”，不会感知为“层级升级了”
- 如果技术文本一起切成宋体，会直接影响调试和表格可读性

因此本轮核心不是“更像宋体”，而是“更有层级的宋感系统”。
