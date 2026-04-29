# 全局工作台基础层视觉语言推广设计

**日期：** 2026-04-27

**范围：** `spring-boot-iot-ui` 全局基础样式层与共享工作台组件

**主题：** 以 `/system-log` 已验证的“轻量工作台 + 橙色品牌强调 + 灰蓝信息层级”设计语言为来源，推广到全局基础层，而不是继续停留在单页优化

**状态：** 设计已确认，待书面 spec 审阅

## 1. 背景

最近一轮 `/system-log` 的优化，已经把一套更稳定的工作台视觉语言收了出来：

- 顶部摘要不再像一排厚卡片，而是更轻的指标头带
- 页签、筛选带、主表、分页区的垂直节奏更连续
- 状态标签从“强提醒块”收成了更轻的状态丸
- 表格操作列从“满排橙色动作”收成了更安静的工具带
- 橙色品牌强调与灰蓝信息层级，已经能同时兼顾业务识别和长时间扫读

问题在于，这套语言目前大部分仍停留在 `/system-log` 及其局部样式中。仓库里虽然已经有比较成熟的全局 token、Element Plus override 和共享工作台组件，但全局仍存在以下分叉：

- 有些页面已经偏轻量工作台，有些页面仍保留更厚的卡片感
- 同类表格页在状态标签、表头亮度、操作列节奏、分页视觉重量上还不一致
- 筛选带和工作台头部在不同页面里密度差异较大
- 橙色品牌色已是系统主色，但不同页面对橙色的使用方式不一致，导致“像同一个品牌，但不像同一套产品”

因此这轮目标不是继续修单页，而是把 `/system-log` 这套已验证的观感提炼为**全局基础层设计语言**，让共享组件和高频工作台页面自然继承。

## 2. 目标

- 把 `/system-log` 现有的橙色 + 灰蓝观感提炼成全局基础层，而不是只做页面 scoped 样式
- 统一全局的颜色 token、状态标签、表格、分页、筛选带、工作台头带和行内操作语法
- 让工作台型页面在不失去业务差异的前提下，进入同一套“更轻、更稳、更耐看”的扫读语言
- 保留业务页自己的信息结构，不把所有页面做成同一张模板复刻品
- 为后续批量回归 `/audit-log`、`/devices`、`/products`、治理台等高频页面提供稳定基础

## 3. 非目标

- 本轮不直接重构所有业务页面的信息结构
- 本轮不改 API、权限、分页协议和后端字段口径
- 本轮不统一所有详情抽屉、表单抽屉和二级交互流程
- 本轮不把所有页面都做成 `/system-log` 的视觉复制版
- 本轮不重做品牌主色方向，明确沿用当前橙色品牌与灰蓝信息层级

## 4. 已确认方向

这轮已在对话中确认以下约束：

- 全局推广的是 `/system-log` 当前观感，而不是重新发明一套新风格
- 设计来源明确为“橙色品牌强调 + 灰蓝正文/辅助层级 + 更轻的工作台结构”
- 统一方式采用 `Token + 全局样式 + 共享工作台组件` 的双层收口
- 首轮只做**全局基础层**，不直接展开全站逐页重构
- 实施边界分三层：
  - 全局基础层
  - 共享工作台层
  - 业务局部层

## 5. 全局边界

### 5.1 全局基础层

这一层所有页面都会吃到，负责统一“产品语气”：

- `src/styles/tokens.css`
- `src/styles/global.css`
- `src/styles/element-overrides.css`

在这一层统一：

- 品牌橙与灰蓝信息层级
- 文本、边框、阴影、轻底色的强弱关系
- 表格表头、行 hover、分页、标签、链接动作的基础语法
- 基础按钮、link 按钮和轻动作的默认表达

### 5.2 共享工作台层

这一层只给工作台型页面复用，负责统一“首屏结构和节奏”：

- `src/components/StandardPageShell.vue`
- `src/components/StandardWorkbenchPanel.vue`
- `src/components/StandardListFilterHeader.vue`
- `src/components/iotAccess/IotAccessTabWorkspace.vue`
- `src/components/StandardRowActions.vue`
- `src/components/StandardWorkbenchRowActions.vue`

在这一层统一：

- 工作台头带与内容区之间的垂直节奏
- 筛选带密度
- 主表与分页区关系
- 行内操作列的轻量工具带语法
- 页签头的激活态和密度

### 5.3 业务局部层

这一层继续由业务页面自己控制，不进全局模板：

- `/system-log` 顶部 4 个摘要项的业务语义
- `异常排查 / 观测热点 / 归档治理` 的特殊结构
- 焦点条、趋势下钻、批次对比、证据抽屉等强业务组件
- 任何会改变业务理解路径的信息结构重组

原则是：**全局统一语言，不统一页面故事。**

## 6. 首轮全局化范围

### 6.1 颜色与状态

把当前 `/system-log` 已验证的观感提炼成更稳定的全局 token：

- 品牌橙仍作为主强调色
- 灰蓝继续承担正文、辅助说明、表头、次级动作语义
- 危险 / 成功 / 中性标签统一走“轻底色 + 轻边框 + 更克制的文字”路线
- 当前页、当前标签、当前页码、当前焦点继续使用薄强调，而不是厚色块

### 6.2 表格语法

全局统一以下表格语言：

- 表头更轻，不再像第二层卡片顶板
- 行 hover 更淡，保持可感知但不抢主内容
- 行间分隔线更柔和
- 主字段允许更深字重，次级说明默认更轻
- 操作列默认从“醒目动词排布”收成“轻工具带”

### 6.3 分页语法

分页统一成轻底栏，不再像新的操作面板：

- 默认按钮底色减轻
- 当前页码改为薄强调，不用重渐变主按钮
- total / sizes / jumper 统一降为次级文字层级
- 与主表之间的分隔改为轻线 + 小留白

### 6.4 筛选带语法

筛选区统一从“第二块浮卡”收向“查询工作带”：

- padding 和 shadow 更克制
- 输入、下拉、动作区处于同一视觉层级
- 高级筛选展开线与主操作区之间的分隔更轻
- 提示文字默认不抢第一视觉

### 6.5 工作台头带与页签

工作台头部统一成更轻的首屏入口：

- 指标带优先像监控摘要，而不是卡片墙
- 页签头更像分段导航，而不是第二排卡片
- 指标带 -> 页签 -> 内容区之间的过渡更紧
- 当前页签与摘要态保持同一套品牌强调节奏

### 6.6 行内操作语法

把 `/system-log` 最新这套行内动作语言提炼为全局默认：

- 默认动作文本不再整排高饱和橙色
- 默认使用较轻的灰蓝动作色
- hover / focus 再提升到品牌橙
- 动作之间允许轻分隔，减少“字堆字”的感觉
- 删除等高风险动作在语义上可继续保留更明确的危险色策略，但默认不做整列高饱和

## 7. 首轮落点文件

本轮优先改动以下文件：

- `spring-boot-iot-ui/src/styles/tokens.css`
- `spring-boot-iot-ui/src/styles/global.css`
- `spring-boot-iot-ui/src/styles/element-overrides.css`
- `spring-boot-iot-ui/src/components/StandardPageShell.vue`
- `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
- `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
- `spring-boot-iot-ui/src/components/StandardRowActions.vue`
- `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
- `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`

这些点改完以后，复用它们的页面会自然进入新语法，不需要每页重新写一遍。

## 8. 第一批直接受益页面

首轮基础层落下去后，以下高频页面会立刻受益：

- `/system-log`
- `/audit-log`
- `/devices`
- `/products`
- `/governance-task`
- `/governance-ops`
- `/governance-approval`
- `/device-onboarding`
- `/message-trace`

原因不是这些页都长得一样，而是它们都依赖同一批工作台壳子、筛选带、表格、分页和行内操作语法。

## 9. 风险与控制

### 9.1 主要风险

- token 影响面过宽，导致旧页面观感突变
- 工作台页和普通表单页密度需求不同，若一刀切会误伤普通页
- 状态色过轻后，失败/删除等动作辨识度下降
- 共享组件一改动，波及所有复用页，局部问题会在多页放大

### 9.2 控制策略

- 全局只先改“语气和节奏”，不改业务结构
- 对工作台组件做 scoped 补充，而不是只靠 token 粗暴全覆盖
- 保留局部页面 override 的能力，但要求它们只做业务差异，不再重写整套语法
- 每一批全局改动后都回归代表性高频页面，而不是最后一次性兜底

## 10. 推荐实施顺序

### 第一批：基础样式层

- `tokens.css`
- `element-overrides.css`
- `global.css`

目标：先把颜色、标签、表格、分页、链接动作的全局底色定下来。

### 第二批：共享工作台层

- `StandardPageShell`
- `StandardWorkbenchPanel`
- `StandardListFilterHeader`
- `StandardRowActions`
- `StandardWorkbenchRowActions`
- `IotAccessTabWorkspace`

目标：把工作台骨架和查询/列表语法统一。

### 第三批：高频页面回归对齐

优先检查并做少量局部修正：

- `/system-log`
- `/audit-log`
- `/devices`
- `/products`

目标：确保全局基础层落下去之后，这些高频页不是“被统一”，而是“更完整”。

## 11. 验收口径

完成首轮全局基础层后，应满足：

1. 全局页面进入一致的橙色 + 灰蓝工作台观感
2. 表格首屏更轻，但主字段、状态和动作仍清楚可扫
3. 状态标签、分页、筛选带不再像各页各长各的
4. 工作台页签、头带、内容区之间的节奏更统一
5. 业务页仍保留自己的信息结构，不会因为全局化而变得同质

## 12. 回归与验证

首轮至少回归以下页面：

- `/system-log`
- `/audit-log`
- `/devices`
- `/products`
- `/governance-task`
- `/governance-ops`
- `/governance-approval`
- `/device-onboarding`
- `/message-trace`

工程验证至少包含：

- 受影响组件测试
- 受影响视图测试
- `npm run build`
- 浏览器走查高频工作台页

重点观察：

- 表格首列是否更清楚
- 状态标签是否还够准
- 操作列是否更安静但不难点
- 筛选带是否统一但没变笨重

## 13. 后续衔接

这份 spec 只定义**全局基础层**，不直接展开批量实现细节。待用户审阅确认后，再进入 implementation plan，按“基础层 -> 共享工作台层 -> 高频页面回归”的顺序执行。
