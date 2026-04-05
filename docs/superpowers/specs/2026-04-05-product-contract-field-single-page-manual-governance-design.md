# 产品契约字段单页手动提炼收口设计

> 日期：2026-04-05
> 适用范围：`/products` 产品定义中心、产品经营工作台、契约字段工作区、产品物模型治理后端
> 目标：把当前“契约字段页 + 二层物模型提炼抽屉 + 自动候选链路”收口为“契约字段单页工作台 + 手动样本提炼 + compare/apply”

## 1. 背景

当前 `/products` 已形成以下结构：

1. 产品定义中心列表页。
2. `ProductBusinessWorkbenchDrawer` 产品经营工作台。
3. `契约字段` 页签中的 `ProductModelDesignerWorkspace`。
4. 再由 `ProductModelDesignerWorkspace` 打开 `ProductModelDesignerDrawer` 二层抽屉，承载样本输入、识别结果和确认生效。

同时，前后端仍保留了两类并行能力：

1. `手动样本提炼`
2. `自动候选 / 运行期候选`

这套结构已经能工作，但存在 6 个明显问题：

1. `契约字段` 已经是一个工作区，却还要再弹一层抽屉，交互层级过深。
2. “开始补齐契约”会把用户带入新的承载层，而不是在当前页完成。
3. `自动提炼`、`includeRuntimeCandidates`、`model-candidates` 等概念把页面重新拉回研发语境。
4. 手动提炼与自动候选双轨并存，造成前后端存在大量无效或低价值代码。
5. `样本类型`、`提炼模式`、`父设备样本归一到子产品` 当前表达割裂，用户不容易理解。
6. 产品经营工作台头部仍存在视觉和信息层级不统一的问题，例如：
   - `产品Key` 字号不够突出
   - 经营页卡片和关联设备卡片高度不一致
   - `编辑档案` 按钮没有与头部底边统一对齐
   - “当前已有运行设备，可继续补齐并核对契约字段”这类动态提示语过强

本轮目标不是扩功能，而是把已有能力收成一条更短、更干净、更利于长期维护的正式路径。

## 2. 设计目标

本轮固定服务以下目标：

1. `契约字段` 页签内直接完成样本输入、字段提炼、结果确认和正式字段查看，不再弹二层抽屉。
2. 全面删除“自动候选 / 运行期候选”链路，只保留手动样本提炼 + compare/apply。
3. 页面只保留业务需要理解的输入项，不继续暴露底层治理开关。
4. 对复合设备场景，支持在当前页通过`父设备编码 + 映射关系`完成提炼。
5. 手动复制的 JSON 文本支持自动格式化，提升录入体验。
6. 顺手把产品经营工作台头部、契约页摘要和按钮对齐关系一起收口。
7. 对无用代码执行“有入口才保留、无入口即删除”的清理原则。

## 3. 非目标

本轮明确不做：

1. 不新增新的一级路由或独立“契约管理中心”页面。
2. 不重做正式物模型 CRUD 本身。
3. 不新增运行期契约健康统计接口。
4. 不引入新的数据库表。
5. 不保留“先隐藏、以后再删”的自动候选兼容链路。
6. 不把这一轮扩成新的通用规则平台。

## 4. 方案对比与选型

### 4.1 方案 A：同页契约台 + 保留 compare/apply 骨架，删除自动候选链路

做法：

1. 把 `ProductModelDesignerDrawer` 平铺回 `契约字段` 页。
2. 保留 `compare/apply` 作为正式治理主链路。
3. 删除 `model-candidates`、`includeRuntimeCandidates` 和运行期自动候选实现。

优点：

1. 改动聚焦。
2. 行为变化清晰。
3. 容易删除废弃代码。
4. 与你当前希望的“基于现有上报进行手动提取”最一致。

缺点：

1. 需要联动前端组件、类型、后端 compare 契约和文档。

### 4.2 方案 B：只取消二层抽屉，自动候选链路先隐藏

做法：

1. 页面改为同页展示。
2. 自动候选相关接口和实现先不删。

优点：

1. 短期改动更少。

缺点：

1. 会保留大量无入口垃圾代码。
2. 后续一定反复回流。
3. 前后端语义仍是双轨。

### 4.3 方案 C：重写为一次提取一次保存的新接口

做法：

1. 重做前后端契约，不再沿用 compare/apply。

优点：

1. 结构最纯净。

缺点：

1. 风险最高。
2. 变更面过大。
3. 不适合本轮快速收口。

### 4.4 选型

本轮采用 `方案 A`。

原因：

1. 能在不新增页面的前提下完成交互收口。
2. 保留现有正式治理骨架，风险可控。
3. 能彻底删除自动候选链路，避免继续堆积无效代码。

## 5. 总体设计

### 5.1 契约字段页改为单页纵向工作台

落点：

1. `ProductModelDesignerWorkspace.vue`
2. `ProductWorkbenchView.vue`
3. `ProductBusinessWorkbenchDrawer.vue`

当前 `契约字段` 页不再打开 `ProductModelDesignerDrawer`，而是自己承载完整流程。

页面从上到下固定为四段：

1. `样本输入`
2. `识别结果`
3. `本次生效`
4. `当前已生效字段`

用户行为链路固定为：

1. 进入 `契约字段`
2. 选择样本类型和设备结构
3. 粘贴上报 JSON
4. 执行提取
5. 查看识别结果
6. 选择需要纳入的字段
7. 确认生效
8. 查看正式字段结果

### 5.2 样本输入区

#### 5.2.1 样本类型

样本类型只保留：

1. `业务数据`
2. `状态数据`

不再保留：

1. `other`
2. 其他兜底样本类型

#### 5.2.2 设备结构

设备结构独立于样本类型，单独表达：

1. `单台设备`
2. `复合设备`

因此最终页面语义是：

1. 先回答“当前录入的是业务数据还是状态数据”
2. 再回答“当前样本来自单台设备还是复合设备”

而不是把这两类语义混成一个字段。

这里再补一条强约束，避免后续页面误导：

1. `复合设备` 表示“本次样本需要按父设备编码 + 映射关系归一到子产品”，不是“现场这台设备物理上挂了多个测点就一律选复合设备”。
2. 因此，采集型父产品自身做状态字段治理时，仍应选择 `单台设备`，直接提取父设备自己的状态字段。
3. 只有当当前产品上下文是某类子产品，并且样本来自其父设备集中上报时，才选择 `复合设备`。

#### 5.2.3 复合设备扩展区

当 `设备结构 = 复合设备` 时，展开：

1. `父设备编码`
2. `映射关系`

其中映射关系使用轻量表格维护，当前只暴露：

1. `逻辑通道编码`
2. `子设备编码`

不直接暴露给业务用户的字段：

1. `relationType`
2. `canonicalizationStrategy`
3. `statusMirrorStrategy`

这些值由页面内部按复合设备契约固定补齐：

1. `relationType = collector_child`
2. `canonicalizationStrategy = LF_VALUE`
3. `statusMirrorStrategy = SENSOR_STATE`

#### 5.2.4 关系填充策略

复合设备关系采用以下主路径：

1. 用户输入 `父设备编码`
2. 页面先读取当前已有 `iot_device_relation`
3. 若存在关系，则先回填到映射表格
4. 用户可继续补充、修改或删除行
5. 本次提取优先使用当前页表格中的映射关系

这样既复用已有主数据，又允许首次治理时直接在当前页完成补充。

本轮再明确一条收口原则：

1. 当前页对映射关系的补充和修改，默认先作为“本次提取上下文”使用。
2. `compare/apply` 不应隐式把当前表格改动直接回写成正式设备关系，避免用户在核对契约字段时产生额外副作用。
3. 若后续确实需要把当前页补录的映射同步回 `iot_device_relation`，应以显式动作接入现有设备关系 API，而不是夹带在提取主链路里。

### 5.3 JSON 输入区

落点：

1. 当前 `ProductModelDesignerDrawer` 中的文本域能力平移到 `ProductModelDesignerWorkspace`

保留一个主输入框，但交互升级为“手动输入优先”：

1. 支持粘贴后自动尝试格式化
2. 仍保留显式 `格式化 JSON` 按钮
3. 若 JSON 非法，不自动改写原文
4. 非法时只在输入区内给出明确错误提示

本轮不再出现：

1. `自动提炼开启/关闭`
2. `自动提炼字段`
3. `运行期证据`

对应主操作文案改为：

1. `提取契约字段`

### 5.4 识别结果区

落点：

1. `ProductModelGovernanceCompareTable.vue`

继续保留 compare 行为，但收口为“手动样本 compare”。

结果区保留当前分组优势，但语义从“自动候选对比”改为“手动提取结果”：

1. `可直接生效`
2. `待确认`
3. `继续观察`
4. `存在差异`

结果区仍支持按：

1. `全部`
2. `属性`
3. `事件`
4. `服务`

筛选，但本轮手动提取默认只生成 `property`。

识别结果区不再出现：

1. runtime 候选来源
2. 自动提炼来源标签
3. 自动证据强弱提示

### 5.5 本次生效区

本次生效区继续保留当前“待应用项”表达，但承载层从二层抽屉移到 `契约字段` 主页。

底部确认区继续保留一句总结：

`已选 X 项，确认后将写入正式字段`

主按钮文案固定为：

`确认并生效`

### 5.6 当前已生效字段区

当前已生效字段继续展示正式模型列表，但不再切去另一套“正式字段”独立承载层。

因此：

1. `待核对字段 / 正式字段` 双视角保留在同一页内部
2. 不再用二层抽屉分流
3. 不再要求用户在两套承载层之间跳转

## 6. 前端重构设计

### 6.1 组件边界调整

建议调整为：

1. `ProductModelDesignerWorkspace`
   - 升级为完整契约字段工作台
2. `ProductModelDesignerDrawer`
   - 删除
3. `ProductModelGovernanceCompareTable`
   - 保留，改成纯手动提取 compare 结果表

### 6.2 工作台内的入口动作

当前 `开始补齐契约 / 继续核对字段` 的动作调整为：

1. 不再打开新页面或新抽屉
2. 只负责把视图切到 `契约字段`
3. 若已在 `契约字段`，则滚动到样本输入区

### 6.3 前端需要删除的内容

建议直接删除：

1. `ProductModelDesignerDrawer.vue`
2. 其对应测试
3. `manualExtractMode = direct / relation_child`
4. `includeRuntimeCandidates`
5. `自动提炼：开启/关闭`
6. `自动提炼字段`
7. `listProductModelCandidates`
8. `manualExtractProductModelCandidates`
9. `confirmProductModelCandidates`
10. 与自动候选链路相关的文案、状态和测试桩

### 6.4 前端保留的内容

继续保留：

1. `compareProductModelGovernance`
2. `applyProductModelGovernance`
3. 正式物模型 CRUD
4. 设备关系 API
5. 契约字段页本身

## 7. 后端契约重构设计

### 7.1 接口保留与删除

#### 保留

1. `GET /api/device/product/{productId}/models`
2. `POST /api/device/product/{productId}/models`
3. `PUT /api/device/product/{productId}/models/{modelId}`
4. `DELETE /api/device/product/{productId}/models/{modelId}`
5. `POST /api/device/product/{productId}/model-governance/compare`
6. `POST /api/device/product/{productId}/model-governance/apply`

#### 删除

1. `GET /api/device/product/{productId}/model-candidates`
2. `POST /api/device/product/{productId}/model-candidates/manual-extract`
3. `POST /api/device/product/{productId}/model-candidates/confirm`

### 7.2 compare 新输入语义

`compare` 由“双证据 compare”改为“手动样本 + 正式基线 compare”。

建议把 `manualExtract` 收口为：

1. `sampleType`
   - `business`
   - `status`
2. `deviceStructure`
   - `single`
   - `composite`
3. `samplePayload`
4. `parentDeviceCode`
5. `relationMappings[]`
   - `logicalChannelCode`
   - `childDeviceCode`

本轮不再保留：

1. `includeRuntimeCandidates`
2. `extractMode`
3. `sourceDeviceCode`

原因：

1. 页面不应再让用户理解“direct / relation_child”这种提炼模式。
2. `parentDeviceCode + relationMappings[] + deviceStructure` 已足够表达复合设备场景。

### 7.3 手动提取规则

#### 单台设备

1. 输入一个设备样本
2. 样本根层只允许一个设备编码
3. 自动剥离时间戳层级
4. 递归下钻到叶子标量生成候选

#### 复合设备

1. 仍要求一次只处理一个父设备样本
2. 样本根层仍只允许一个父设备编码
3. 使用当前页提交的 `relationMappings[]` 做通道归一
4. 不再依赖 compare 阶段自动加载运行期证据

补充归一口径：

1. 当 `sampleType=business` 且 `deviceStructure=composite` 时，类似 `L1_LF_n` 这类逻辑通道值应按固定策略归一到子产品正式字段，例如裂缝场景统一归一为 `value`。
2. 当 `sampleType=status` 且 `deviceStructure=composite` 时，只提取与逻辑通道对应的子设备状态镜像，例如 `S1_ZT_1.sensor_state.L1_LF_n -> sensor_state`。
3. 复合设备状态提取不应把父设备自身的 `temp`、`humidity`、`signal_4g` 等终端状态字段混入子产品候选。
4. `parentDeviceCode`、`relationMappings[]` 仅在 `deviceStructure=composite` 时生效；`single` 模式下即使传入也应忽略。

### 7.4 后端需要删除的内容

建议直接删除：

1. `ProductModelService.listModelCandidates`
2. `ProductModelService.manualExtractModelCandidates`
3. `ProductModelService.confirmModelCandidates`
4. `ProductModelController` 中对应三个接口
5. `ProductModelCandidate*` 相关 DTO / VO / service 分支
6. `includeRuntimeCandidates` 全部字段与判断
7. `buildRuntimeGovernanceCandidates`
8. 基于 `iot_device_property / iot_device_message_log` 自动拼运行期候选的实现
9. 只服务自动候选回执的辅助存储与测试

### 7.5 后端保留的内容

继续保留：

1. 手动样本解析核心逻辑
2. compare 结果构建
3. apply 正式入库
4. 关系感知子设备归一逻辑
5. 正式物模型 CRUD

## 8. 产品经营工作台视觉收口

### 8.1 产品 Key 字号上调

调整范围：

1. 产品经营工作台头部标题区
2. 身份信息区中的 `产品Key`

规则：

1. 字号整体提高一档
2. 采用与 `档案摘要` 标题同一级别的字感
3. 标题和内容都统一调大，不只放大其中一处

### 8.2 卡片高度统一

当前产品经营页的主卡与关联设备卡高度需要对齐。

调整目标：

1. 产品经营页卡片高度
2. 关联设备卡片高度

规则：

1. 两张卡在同一行时应具备统一的底边
2. 不再出现一高一矮的视觉跳动

### 8.3 编辑档案按钮底对齐

`编辑档案` 按钮当前需要与产品经营工作台头部底边对齐。

规则：

1. 按钮不再悬浮在头部上沿
2. 与头部信息区、tab strip 或底边形成统一基线

### 8.4 动态提示语收口

当前类似：

`当前已有运行设备，可继续补齐并核对契约字段`

这类运营式动态提示建议取消。

原因：

1. 文案抢戏
2. 易与页面真实状态混淆
3. 不利于统一工作台头部语义

替代方案：

1. 用更稳定的摘要文案
2. 把状态判断更多交给 summary card 与契约字段页内部空态

## 9. 错误处理设计

### 9.1 输入错误

1. JSON 非法
   - 在输入区给出行内错误
2. 单台设备模式下不是 1 台设备样本
   - 给出“单次只支持 1 台设备样本”
3. 复合设备缺少父设备编码
   - 仅在复合设备模式下阻止提取
4. 复合设备没有任何映射关系
   - 阻止提取并高亮映射区

### 9.2 关系错误

1. 同一逻辑通道重复
   - 在映射表格中高亮
2. 逻辑通道为空
   - 行内提示
3. 子设备编码为空
   - 行内提示

### 9.3 结果错误

1. 提取结果为空
   - 页面内空态回答“当前样本没有识别出可纳入契约的字段”
2. compare 失败
   - 页面内错误提示，不使用全局红色弹错打断流程

## 10. 影响模块

### 10.1 前端

1. `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
2. `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
3. `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
4. `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
5. `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
6. `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
7. `spring-boot-iot-ui/src/api/product.ts`
8. `spring-boot-iot-ui/src/types/api.ts`
9. 对应测试文件

### 10.2 后端

1. `spring-boot-iot-device/.../ProductModelController.java`
2. `spring-boot-iot-device/.../ProductModelService.java`
3. `spring-boot-iot-device/.../ProductModelServiceImpl.java`
4. compare/manual 提炼相关 DTO / VO / tests

### 10.3 文档

1. `docs/02-业务功能与流程说明.md`
2. `docs/03-接口规范与接口清单.md`
3. `docs/08-变更记录与技术债清单.md`
4. `docs/15-前端优化与治理计划.md`
5. 如 README 中仍保留自动提炼表达，则同步更新 `README.md`
6. 若本轮要把“无入口即删代码”写成长期协作规则，再评估是否同步 `AGENTS.md`

## 11. 回归与验收建议

本轮至少需要覆盖以下回归：

1. `契约字段` 页不再弹二层抽屉
2. `开始补齐契约` 在当前页进入样本输入区
3. `业务数据` 样本可提取
4. `状态数据` 样本可提取
5. `单台设备` 模式可提取
6. `复合设备` 模式可提取
7. 复合设备模式可读取已有关系并人工补充
8. 前端不再请求 `model-candidates`
9. 后端不再暴露 `model-candidates` 接口
10. 产品经营页视觉收口生效：
   - `产品Key` 字号上调
   - 卡片高度一致
   - `编辑档案` 底对齐
   - 动态提示语移除

## 12. 实施顺序

建议按以下顺序实施：

1. 前端：把契约字段改成同页工作台
2. 前端：移除自动候选相关类型、状态和 API
3. 后端：重构 compare 契约，只保留手动样本输入
4. 后端：删除 `model-candidates` 和 runtime 候选链路
5. 测试：补齐前后端回归
6. 文档：同步更新权威文档

## 13. 最终统一话术

推荐团队统一使用以下表述：

1. `契约字段` 页在同一页面完成样本录入、字段提取、结果确认和正式字段查看。
2. 当前只支持基于手动粘贴的上报 JSON 提取契约字段，不再支持自动候选提炼。
3. 样本类型只区分`业务数据`和`状态数据`。
4. 设备结构只区分`单台设备`和`复合设备`。
5. 复合设备通过`父设备编码 + 映射关系`完成字段归一。
6. 无入口、无业务价值的自动候选代码应一并删除，不持续保留。
