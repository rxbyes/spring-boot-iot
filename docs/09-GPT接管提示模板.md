# 09 GPT 接管提示模板

更新时间：2026-03-21

本文件提供一份可直接投喂给 GPT / Codex 类编码模型的固定接管提示模板。

目标：
- 让模型优先读取最小阅读集，而不是全量扫描 `docs/`
- 让模型快速进入“可直接编码”的工作状态
- 明确项目硬约束、交付边界、真实环境基线与文档更新规则

## 1. 使用方式

1. 直接复制下方“标准模板”
2. 只替换 `【...】` 占位符
3. 如任务较明确，可再追加“按场景附加片段”

说明：
- 默认模板已经包含最小阅读集与关键约束
- 如果任务只涉及局部页面、单个接口或单个 Bug，不需要要求模型全量扫描仓库
- 如果任务影响协议、前端、验收、Phase 4 交付边界，再追加对应附加片段

## 2. 标准模板

```md
你现在接手的是项目 `spring-boot-iot`。

请先按“最小阅读集”建立认知，再根据任务类型按需补充阅读，不要先全量扫描整个 `docs` 目录。

## 一、最小阅读集

请先阅读以下文件：
1. `README.md`
2. `docs/README.md`
3. `docs/01-系统概览与架构说明.md`
4. `docs/02-业务功能与流程说明.md`
5. `docs/03-接口规范与接口清单.md`
6. `docs/04-数据库设计与初始化数据.md`
7. `docs/07-部署运行与配置说明.md`
8. `docs/08-变更记录与技术债清单.md`

## 二、项目硬约束

- 项目名必须保持：`spring-boot-iot`
- 基础包必须保持：`com.ghlzm.iot`
- 架构形态必须保持：模块化单体（Modular Monolith）
- 唯一启动模块：`spring-boot-iot-admin`
- 不要破坏模块边界
- 不要把业务逻辑写进 Controller
- 不要把持久化逻辑放进协议适配层
- 统一响应模型：`R`
- 业务异常：`BizException`

## 三、运行与验收基线

- 唯一真实环境验收基线：`spring-boot-iot-admin/src/main/resources/application-dev.yml`
- 可以通过环境变量覆盖该文件中的连接配置
- 不允许回退到旧 H2 验收路径、独立 H2 脚本或 H2-only 验收方案
- Windows 10 共享环境常见工作区示例路径可能是：`E:\idea\ghatg\spring-boot-iot`
- 其他系统路径可能不同，不要把某一个绝对路径当成唯一合法路径；优先按脚本位置、仓库根目录或当前环境推导路径
- 涉及后端联调、重启、发布验证时，必须优先复用现有已启动服务（默认 `9999`），不要改用容器、沙盒或其他替代运行链路
- 若后端本次没有新增更新内容，不要重复拉起其他运行方式；直接复用现有服务完成验证
- 若后端服务未启动或需要重启，先停止 `9999` 服务，再按下列命令执行打包和启动
- Windows 环境下执行：
  - `mvn -s .mvn/settings.xml clean install -DskipTests`
  - `mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"`
- Mac/Linux 环境下执行：
  - `mvn clean install -DskipTests`
  - `mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`

## 四、文档维护规则

- 任何影响行为、API、配置、流程、页面结构、验收步骤、产品定位的修改，都必须同步更新现有文档
- 统一更新已有文档，不要新增 `README-v2.md`、`api-new.md`、`xxx-new.md` 一类平行替代文档
- 优先维护核心文档 `docs/01` ~ `docs/08`
- 如果变更影响运行方式、接手方式或项目规则，也要检查是否需要更新 `README.md` 和 `AGENTS.md`

## 五、当前交付边界

- Phase 1~3 主链路已是稳定基线：产品、设备、HTTP 上报、MQTT 上下行、协议解析、消息日志、属性更新、在线状态
- Phase 4 核心能力当前可用：告警运营台、事件协同台、风险对象中心、阈值策略、联动编排、应急预案库、运营分析中心、组织架构/账号中心/角色权限/导航编排/区域版图/数据字典/通知编排/审计中心
- `/risk-monitoring`、`/risk-monitoring-gis` 已有代码基线，但是否计入已交付范围，以 `docs/19-phase4-progress.md` 为准

## 六、本次任务

任务目标：
【在这里填写本次要完成的目标】

期望结果：
【在这里填写期望交付，例如“修复某问题 / 新增某功能 / 重构某模块 / 补齐某文档”】

影响范围：
【可选，填写已知模块、页面、接口、表、脚本、文档】

限制条件：
【可选，填写不能改的内容、必须兼容的环境、时间要求、是否允许改表结构等】

## 七、执行要求

请按以下方式工作：

1. 先基于最小阅读集总结当前理解
2. 列出本次任务涉及的模块、页面、接口、表结构和文档
3. 给出实现计划与必要假设
4. 直接开始实际修改，不要只停留在方案层
5. 如需进一步阅读文档，请只按任务补充相关文档，不要无差别扫描全部资料
6. 完成后给出：
   - 变更文件清单
   - 变更说明
   - 验证方式 / 已执行命令
   - 未完成项 / 风险项
   - 本次同步更新了哪些文档

## 八、输出风格

- 结论优先，表达直接
- 不要泛泛而谈
- 发现信息不确定时明确标记：
  - `【待确认】`
  - `【根据代码推断】`
  - `【文档缺失】`

现在开始执行本次任务。
```

## 3. 按场景附加片段

以下片段按需追加到“标准模板”后面。

### 3.1 前端页面 / CSS / 交互任务

```md
这是一个前端任务，请额外补充阅读：
- `docs/06-前端开发与CSS规范.md`
- `docs/15-frontend-optimization-plan.md`

前端约束：
- 保持 UTF-8 可读，不能写入乱码中文
- 优先复用 `PanelCard`、`StandardPagination`、`useServerPagination`、`StandardTableToolbar`、`StandardTableTextColumn`、`StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter`、`confirmAction`、共享列表样式和现有 design tokens
- 分组概览、工作台、抽屉和确认弹窗必须保持统一 brand/accent token 体系，不要再给单页单独发明另一套主色
- 如发现样式漂移、分页不统一或新的前端治理问题，请同步更新 `docs/15-frontend-optimization-plan.md`
```

### 3.2 协议 / MQTT / 设备接入任务

```md
这是一个协议或设备接入任务，请额外补充阅读：
- `docs/05-protocol.md`
- `docs/14-mqttx-live-runbook.md`
- `docs/test-scenarios.md`

请重点关注：
- 标准 Topic 与 `$dp` 兼容链路
- 安全校验、解密、多商户 AES key 选择
- 真实环境验收基线，不允许用 H2 替代
```

### 3.3 测试 / 验收 / 质量保障任务

```md
这是一个测试或验收任务，请额外补充阅读：
- `docs/05-自动化测试与质量保障.md`
- `docs/test-scenarios.md`
- `docs/21-business-functions-and-acceptance.md`

请重点关注：
- 真实环境基线
- 当前脚本与验收产物
- 验收结论必须区分“通过 / 不通过 / 环境阻塞 / 待确认”
```

### 3.4 Phase 4 交付边界 / 风险监测任务

```md
这是一个涉及 Phase 4 交付边界的任务，请额外补充阅读：
- `docs/19-phase4-progress.md`
- `docs/21-business-functions-and-acceptance.md`

请重点区分：
- “代码已存在”
- “当前已交付”
- “待真实环境验收后才能计入交付”
```

### 3.5 文档治理 / 接手整理任务

```md
这是一个文档治理任务，请重点关注：
- 是否有重复文档、冲突文档、过时文档
- 是否应合并、归档或保留
- 新增内容是否真的属于核心文档，而不是平行替代文档

输出时请明确给出：
- 保留
- 合并后删除
- 废弃归档
- 【待确认】
```

## 4. 极简版模板

如果你只想用一段更短的提示词，可直接使用下面这一版：

```md
请接手 `spring-boot-iot` 项目，并先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/02-业务功能与流程说明.md`、`docs/03-接口规范与接口清单.md`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

项目硬约束：
- 项目名：`spring-boot-iot`
- 基础包：`com.ghlzm.iot`
- 模块化单体
- 唯一启动模块：`spring-boot-iot-admin`
- 验收基线：`application-dev.yml`
- 不允许回退到 H2 验收路径
- Windows 10 共享环境可能使用 `E:\idea\ghatg\spring-boot-iot`，但不要把绝对路径写死为唯一入口
- 任何行为/API/配置/流程变更都要同步更新现有文档，不要新增平行替代文档

本次任务：
【填写任务】

请先总结理解、列出影响范围和实现计划，然后直接开始修改。完成后输出变更文件、验证方式、未完成项和同步更新的文档。
```

## 5. 推荐投喂策略

推荐顺序：

1. 先投喂“极简版模板”或“标准模板”
2. 若任务明确，再追加一个对应“按场景附加片段”
3. 若你手头已有接口、报错、截图、SQL、日志，再追加到同一次提示中

不推荐：

- 一开始就让模型“扫描整个仓库所有文档”
- 把历史归档文档和核心文档混在一起投喂
- 不加任务边界就让模型自由发挥

## 6. 四类场景化快捷模板

以下 4 份模板已经按常见任务场景裁剪好，可直接复制使用。

### 6.1 文档治理 / 接手整理模板

```md
请接手 `spring-boot-iot` 的文档治理工作。

先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/02-业务功能与流程说明.md`、`docs/03-接口规范与接口清单.md`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

如任务涉及验收口径，再补读：
`docs/19-phase4-progress.md`、`docs/21-business-functions-and-acceptance.md`、`docs/test-scenarios.md`。

治理约束：
- 不要机械汇总原文档
- 以“降低 GPT 扫描成本”和“提升接手效率”为第一目标
- 不要新增平行替代文档
- 历史信息优先归档，不要轻易永久删除
- Windows 10 共享环境可能使用 `E:\idea\ghatg\spring-boot-iot`，但不要把该绝对路径写死为唯一入口

本次任务：
【填写文档治理目标】

请输出并执行：
1. 当前文档问题盘点
2. 新的文档结构建议
3. 核心文档重写或归并
4. 原文档处理建议：
   - 保留
   - 合并后删除
   - 废弃归档
   - 【待确认】
5. 实际文档修改，不要只停留在建议层

完成后输出：
- 变更文件
- 调整原因
- 仍待确认的信息
- 本轮归档/保留/删除建议
```

### 6.2 功能开发 / 需求实现模板

```md
请接手 `spring-boot-iot` 的功能开发任务。

先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/02-业务功能与流程说明.md`、`docs/03-接口规范与接口清单.md`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

如果涉及以下内容，再按需补读：
- 协议 / MQTT：`docs/05-protocol.md`、`docs/14-mqttx-live-runbook.md`
- 前端：`docs/06-前端开发与CSS规范.md`、`docs/15-frontend-optimization-plan.md`
- 验收：`docs/05-自动化测试与质量保障.md`、`docs/test-scenarios.md`
- Phase 4 范围：`docs/19-phase4-progress.md`、`docs/21-business-functions-and-acceptance.md`

项目硬约束：
- 项目名必须保持：`spring-boot-iot`
- 基础包必须保持：`com.ghlzm.iot`
- 保持模块化单体
- 唯一启动模块：`spring-boot-iot-admin`
- 不要破坏模块边界
- 不要把业务逻辑写进 Controller
- 不要把持久化逻辑放进协议适配层
- 验收基线固定为 `application-dev.yml`
- 不允许回退到旧 H2 验收路径
- 涉及后端支撑时必须复用现有服务；若服务未启动或需重启，先停止 `9999` 再按“运行与验收基线”中的命令启动；禁止容器/沙盒替代

本次任务：
【填写功能目标】

请按以下方式工作：
1. 先总结当前理解
2. 列出影响模块、接口、表、配置和文档
3. 给出实现计划与假设
4. 直接开始编码
5. 完成后给出：
   - 变更文件
   - 实现说明
   - 验证方式
   - 未完成项 / 风险项
   - 同步更新了哪些文档
```

### 6.3 Bug 修复 / 回归分析模板

```md
请接手 `spring-boot-iot` 的 Bug 修复与回归分析任务。

先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/02-业务功能与流程说明.md`、`docs/03-接口规范与接口清单.md`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

如问题涉及验收、协议或 Phase 4，再补读：
- `docs/05-自动化测试与质量保障.md`
- `docs/test-scenarios.md`
- `docs/05-protocol.md`
- `docs/19-phase4-progress.md`
- `docs/21-business-functions-and-acceptance.md`

问题现象：
【填写报错、异常现象、日志、接口失败信息、复现步骤】

修复目标：
【填写期望修复结果】

约束：
- 先定位根因，不要先大范围重构
- 不要为了通过本地验证而引入 H2 回退路径
- 如果涉及 Windows 10 共享环境示例路径 `E:\idea\ghatg\spring-boot-iot`，要兼容其他环境路径，不要写死
- 修复后要考虑回归影响面
- 涉及后端支撑时必须复用现有服务；若服务未启动或需重启，先停止 `9999` 再按“运行与验收基线”中的命令启动；禁止容器/沙盒替代

请按以下方式工作：
1. 先判断问题位于哪一层：配置 / 接口 / 业务 / 协议 / SQL / 前端 / 验收环境
2. 列出最可能根因
3. 直接定位并修改代码或文档
4. 说明修复是否属于：
   - 代码缺陷
   - 环境阻塞
   - 文档错误
   - 数据库结构不一致
5. 完成后给出：
   - 根因
   - 修复方案
   - 变更文件
   - 验证结果
   - 剩余风险
```

### 6.4 前端页面改造 / 样式治理模板

```md
请接手 `spring-boot-iot-ui` 的前端页面改造任务。

先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/02-业务功能与流程说明.md`、`docs/03-接口规范与接口清单.md`、`docs/06-前端开发与CSS规范.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

额外补读：
- `docs/15-frontend-optimization-plan.md`
- 如涉及验收或 Phase 4 页面，再补读 `docs/19-phase4-progress.md`、`docs/21-business-functions-and-acceptance.md`

前端约束：
- 保持 UTF-8 可读，不能写入乱码中文
- 优先复用 `PanelCard`、`StandardPagination`、`useServerPagination`、`StandardTableToolbar`、`StandardTableTextColumn`、`StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter`、`confirmAction`、共享列表样式和现有 design tokens
- 分组概览、工作台、抽屉和确认弹窗必须保持统一 brand/accent token 体系，不要再给单页单独发明另一套主色
- 不要新增一套页面本地分页/列表规范
- 如发现样式漂移、分页不统一、列表重复建设或新的治理问题，必须同步更新 `docs/15-frontend-optimization-plan.md`
- 不要破坏现有后端 API 契约

本次任务：
【填写页面、路由、交互或样式治理目标】

请按以下方式工作：
1. 先总结页面当前职责与目标用户
2. 列出涉及的视图、组件、API、样式文件和文档
3. 给出改造计划
4. 直接开始修改
5. 完成后给出：
   - 变更文件
   - 页面结构 / 交互 / 样式改动说明
   - 复用的公共能力
   - 验证方式
   - 同步更新的前端文档
```

### 6.5 接口开发 / 接口改造专用模板

```md
请接手 `spring-boot-iot` 的接口开发或接口改造任务。

先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/02-业务功能与流程说明.md`、`docs/03-接口规范与接口清单.md`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

如涉及验收、协议或 Phase 4 范围，再补读：
- `docs/05-自动化测试与质量保障.md`
- `docs/test-scenarios.md`
- `docs/05-protocol.md`
- `docs/19-phase4-progress.md`
- `docs/21-business-functions-and-acceptance.md`

本次接口任务：
【填写接口目标，例如“新增接口 / 修改返回结构 / 补齐分页接口 / 修复接口行为”】

接口信息：
- 所属模块：【填写】
- 目标路径：【填写】
- 请求方式：【填写】
- 请求参数：【填写】
- 返回结构：【填写】
- 关联表或数据源：【填写】
- 是否要求兼容旧接口：【填写】

实现约束：
- 不要破坏模块边界
- Controller 只处理请求/响应
- Service 负责业务编排
- Mapper 负责持久化
- 统一返回 `R`
- 业务错误使用 `BizException`
- 如果行为变化影响接口文档，必须同步更新 `docs/03-接口规范与接口清单.md`
- 如果行为变化影响验收口径，也要同步更新相关核心文档
- 涉及后端支撑时必须复用现有服务；若服务未启动或需重启，先停止 `9999` 再按“运行与验收基线”中的命令启动；禁止容器/沙盒替代

请按以下方式执行：
1. 先总结当前接口现状和差距
2. 列出涉及的 Controller / Service / Mapper / DTO / SQL / 文档
3. 给出实现计划
4. 直接开始编码
5. 完成后输出：
   - 接口变更说明
   - 变更文件
   - 验证方式
   - 是否兼容旧调用方
   - 更新了哪些文档
```

### 6.6 数据库 / SQL / 初始化脚本专用模板

```md
请接手 `spring-boot-iot` 的数据库 / SQL / 初始化脚本任务。

先只阅读：
`README.md`、`docs/README.md`、`docs/01-系统概览与架构说明.md`、`docs/03-接口规范与接口清单.md`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/08-变更记录与技术债清单.md`。

如涉及验收、Phase 4 或真实环境结构对齐，再补读：
- `docs/05-自动化测试与质量保障.md`
- `docs/test-scenarios.md`
- `docs/19-phase4-progress.md`
- `docs/21-business-functions-and-acceptance.md`

本次数据库任务：
【填写任务，例如“补字段 / 修初始化脚本 / 增加升级脚本 / 修正 SQL 示例 / 对齐真实环境 schema”】

数据库信息：
- 涉及文件：【填写，例如 `sql/init.sql`、`sql/init-data.sql`、`sql/upgrade/*.sql`】
- 涉及表/视图：【填写】
- 是新库初始化还是历史库升级：【填写】
- 是否要求幂等：【填写】
- 是否允许破坏性变更：【填写】
- 是否有真实环境兼容要求：【填写】

实现约束：
- 以当前 `sql/init.sql` 为主初始化事实依据
- 历史环境差异通过 `sql/upgrade/*.sql` 处理
- 不要把某个环境的绝对路径写死到脚本或说明中
- 如涉及真实环境基线，仍以 `application-dev.yml` 为准
- 任何表结构、初始化数据、升级脚本变化，都必须同步更新 `docs/04-数据库设计与初始化数据.md`
- 如影响验收口径，也同步更新 `docs/21-business-functions-and-acceptance.md` 或其他相关核心文档

请按以下方式执行：
1. 先判断这是“初始化问题”还是“升级兼容问题”
2. 列出受影响的表、视图、索引、脚本和文档
3. 给出修改方案与风险说明
4. 直接开始修改 SQL / 脚本 / 文档
5. 完成后输出：
   - 变更文件
   - 结构变化说明
   - 初始化 / 升级执行方式
   - 兼容性风险
   - 更新了哪些文档
```

## 7. 如何选择模板

可按以下规则直接选用：

- 要重构资料、整理知识库、归档文档：用 `6.1`
- 要实现新接口、新页面、新功能闭环：用 `6.2`
- 已经有报错、失败日志、回归缺陷：用 `6.3`
- 主要工作在 `spring-boot-iot-ui` 页面、样式、交互：用 `6.4`
- 主要工作是新增 / 修改 / 修复后端接口：用 `6.5`
- 主要工作是 `sql/init.sql`、`sql/init-data.sql`、`sql/upgrade/*.sql` 或 schema 对齐：用 `6.6`

如果任务同时跨两类场景，优先使用 `6.2` 或 `6.3` 作为主模板，再附加对应场景片段。
