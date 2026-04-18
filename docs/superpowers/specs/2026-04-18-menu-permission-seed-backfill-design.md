# 菜单与权限种子补齐设计

> 日期：2026-04-18
> 主题：仅补齐当前真实业务工作页、按钮与重页面内部功能点到 `sys_menu / sys_role_menu`

## 1. 背景

当前前端实际已存在一批业务工作页、页面按钮和重页面内部功能域，但数据库初始化种子 `sys_menu / sys_role_menu` 与前端实际能力之间存在脱节，主要表现为：

- 个别真实工作页已上线使用，但 `sys_menu` 未建对应页面节点。
- 部分页面已有页面节点，但按钮权限项不完整。
- 若干重页面内部已经形成稳定工作区或关键治理动作，但数据库里没有对应功能权限真相。
- 现有角色授权主要覆盖页面级菜单和少量按钮，导致后续细粒度授权扩展缺少统一基线。

本次任务目标不是重做导航架构，而是在最小迁移前提下，把当前“真实承载业务”的页面、按钮和重页面内部功能点补齐到种子，确保数据库权限真相与当前系统能力一致。

## 2. 目标

本次交付目标：

1. 为当前真实业务工作页补齐缺失的 `sys_menu` 页面节点。
2. 为当前页面上真实存在的按钮和关键操作补齐 `sys_menu` 权限项。
3. 为重页面内部已稳定存在的关键功能域补齐 `sys_menu` 隐藏权限项。
4. 按“默认继承父页面当前已有角色”的规则，补齐 `sys_role_menu` 绑定。
5. 保持现有前端路由、页面结构、按钮代码和后端鉴权逻辑不变。

## 3. 非目标

本次明确不做以下事项：

- 不做一级菜单、二级菜单重组。
- 不做前端导航、路由、页面布局或信息架构调整。
- 不做新的三级菜单设计。
- 不改后端权限计算逻辑、首页跳转逻辑或角色模型。
- 不新增数据库表，不改 schema，不改 registry。
- 不把总览页、兼容入口、规划页、重定向页一并落库。
- 不把尚未正式纳入主交付基线的试验页一并放开。
- 不要求前端本轮立即消费所有新增的内部功能点权限。

## 4. 范围

### 4.1 纳入范围

#### 4.1.1 缺失页面菜单

当前明确纳入的缺失页面菜单：

- `/protocol-governance`

该页面已经具备真实业务承载能力，属于接入治理主链路的一部分，应作为正式工作页进入 `sys_menu`。

#### 4.1.2 现有页面按钮权限补齐

补齐当前真实存在但数据库未完整表达的按钮和操作权限，覆盖：

- 风险对象中心
- 告警运营台
- 事件协同台
- 组织架构
- 区域版图
- 数据字典
- 通知编排
- 治理审批台
- 质量工场相关执行/编排页

同时保留现有已被前端直接消费的权限码，不做改名。

#### 4.1.3 重页面内部功能点权限

为以下重页面补齐“页面内部功能域/工作区/治理动作”的隐藏权限项：

- `/products`
- `/devices`
- `/protocol-governance`
- `/risk-point`
- `/governance-task`
- `/governance-ops`
- `/in-app-message`
- `/reporting`
- `/message-trace`

### 4.2 排除范围

以下路由或页面本次不作为独立菜单落库：

#### 4.2.1 总览页

- `/device-access`
- `/risk-disposal`
- `/risk-config`
- `/system-management`
- `/quality-workbench`

#### 4.2.2 兼容入口

- `/automation-assets`
- `/automation-test`

#### 4.2.3 规划/重定向页

- `/future-lab`
- `/risk-enhance`

#### 4.2.4 尚未正式纳入主交付基线的试验页

- `/device-onboarding`

#### 4.2.5 参数化结果页

- `/business-acceptance/results/:runId`

这类页后续如需授权控制，只作为所属工作页下的功能点处理，不单独建页面菜单。

## 5. 设计原则

### 5.1 最小迁移

优先补齐数据库权限真相，不改变现有页面、路由、导航和鉴权链路。

### 5.2 页面层级保持两层

仍维持“一级中心 -> 二级页面菜单”两层结构；页面内部功能点全部以下沉权限项表达，不新增可见的第三层菜单。

### 5.3 兼容优先

现有前端已经使用的权限码继续保留原语义，避免前后端联调风险。

### 5.4 可扩展

新增内部功能点权限码按统一命名方式扩展，保证后续可以渐进接入前端控制，而不是再做一轮权限迁移。

## 6. 数据库落库策略

### 6.1 页面节点

页面级工作页继续使用页面菜单节点表达：

- `type = 1`
- `menu_type = 1`

新增缺失页面时，延续当前 `sql/init-data.sql` 的页面节点风格：

- 有明确 `path / route_path / component / permission`
- 作为对应一级中心的直接子节点
- 可见、启用、可被导航树使用

### 6.2 按钮与内部功能点

页面内按钮与内部功能点统一落为权限节点：

- `type = 2`
- `menu_type = 2`
- 不新增路由
- 不在导航树展示

这类权限项包含两类语义：

1. 前端已经显式使用的按钮权限
2. 当前未直接显式消费、但业务上已经稳定存在的重页面内部功能域权限

### 6.3 编码策略

编码策略分两类：

#### 6.3.1 既有权限码

对前端或后端已在使用的权限码，原样保留，例如：

- `iot:products:add`
- `iot:devices:replace`
- `system:user:reset-password`
- `iot:product-contract:approve`
- `iot:protocol-governance:edit`

#### 6.3.2 新增内部功能点权限码

新增功能点统一按“所属页面编码 + 动作/功能域”的方式命名，例如：

- `iot:products:workbench-overview`
- `iot:products:contract-ledger`
- `iot:devices:detail`
- `risk:point:binding-maintain`
- `system:governance-task:replay`

本轮设计不要求前端立即使用这些新增权限码，但数据库必须先具备真相。

### 6.4 父节点归属修正

对于已存在但挂载位置不合理的权限项，允许在种子中调整其 `parent_id`，前提是：

- 不改变 `menu_code`
- 不改变权限语义
- 调整后归属更符合真实页面承载关系

当前最典型的修正是：

- 协议治理相关权限从“产品定义中心”迁回“协议治理工作台”页面下

## 7. 角色继承策略

### 7.1 基本规则

新增页面或功能点，不重做角色矩阵；默认继承父页面当前已有角色授权。

即：

- 若父页面当前授权给 `管理/运维/开发`
- 则该页面下新增的按钮或内部功能点也默认授权给 `管理/运维/开发`

### 7.2 超级管理员

超级管理员继续依赖现有“全量 active menu 自动授权”逻辑，不单独手工枚举新增菜单项。

### 7.3 实现方式

`sys_role_menu` 优先通过“根据父页面已授权角色自动补子权限”方式实现，而不是在种子中为每个角色重复手写整套绑定。

这样有三个收益：

- 与最小迁移目标一致
- 降低漏绑风险
- 后续新增功能点可以继续复用同一补齐模式

## 8. 具体补齐清单

### 8.1 页面菜单

新增页面菜单：

- `/protocol-governance`

### 8.2 现有页面按钮权限

#### 8.2.1 保留并延续现有已使用按钮权限

- `/products`
  - `iot:products:add`
  - `iot:products:update`
  - `iot:products:delete`
  - `iot:products:export`
- `/devices`
  - `iot:devices:add`
  - `iot:devices:update`
  - `iot:devices:delete`
  - `iot:devices:export`
  - `iot:devices:import`
  - `iot:devices:replace`
- `/user`
  - `system:user:add`
  - `system:user:update`
  - `system:user:delete`
  - `system:user:reset-password`
- `/role`
  - `system:role:add`
  - `system:role:update`
  - `system:role:delete`
- `/menu`
  - `system:menu:add`
  - `system:menu:update`
  - `system:menu:delete`
- `/in-app-message`
  - `system:in-app-message:add`
  - `system:in-app-message:update`
  - `system:in-app-message:delete`
- `/help-doc`
  - `system:help-doc:add`
  - `system:help-doc:update`
  - `system:help-doc:delete`

#### 8.2.2 新增当前页面真实动作权限

- `/risk-point`
  - 风险点新增
  - 风险点编辑
  - 风险点删除
  - 设备绑定
  - 正式绑定维护
  - 待治理转正
- `/alarm-center`
  - 详情
  - 确认
  - 抑制
  - 关闭
- `/event-disposal`
  - 详情
  - 派发
  - 关闭
- `/rule-definition`
  - 使用现有治理口径：
    - `risk:rule-definition:edit`
    - `risk:rule-definition:approve`
- `/linkage-rule`
  - 使用现有治理口径：
    - `risk:linkage-rule:edit`
    - `risk:linkage-rule:approve`
- `/emergency-plan`
  - 使用现有治理口径：
    - `risk:emergency-plan:edit`
    - `risk:emergency-plan:approve`
- `/organization`
  - 新增
  - 编辑
  - 删除
  - 新增下级
  - 导出
- `/region`
  - 新增
  - 编辑
  - 删除
  - 新增下级
  - 导出
- `/dict`
  - 字典新增
  - 字典编辑
  - 字典删除
  - 字典导出
  - 字典项新增
  - 字典项编辑
  - 字典项删除
  - 字典项导出
- `/channel`
  - 新增
  - 编辑
  - 删除
  - 渠道测试
  - 导出
- `/governance-approval`
  - 详情
  - 审批通过
  - 审批驳回
  - 撤销审批
  - 原单重提
- `/governance-security`
  - 将密钥治理类权限归位到本页下：
    - `iot:secret-custody:view`
    - `iot:secret-custody:rotate`
    - `iot:secret-custody:approve`
- `/business-acceptance`
  - 发起验收
  - 打开最近结果
- `/rd-automation-inventory`
  - 刷新盘点
  - 勾选未覆盖
  - 一键补齐脚手架
- `/rd-automation-templates`
  - 新增页面冒烟模板
  - 新增表单提交模板
  - 新增列表详情模板
- `/rd-automation-plans`
  - 导入计划
  - 导出 JSON
  - 恢复默认计划
- `/rd-automation-handoff`
  - 复制命令
  - 导出计划
- `/automation-execution`
  - 复制命令

### 8.3 重页面内部功能点权限

#### 8.3.1 `/products`

工作区：

- 总览
- 契约字段
- 关联设备
- 编辑

治理动作：

- 规范库维护
- 规范库复核
- 契约治理
- 契约发布
- 契约回滚
- 契约复核

内嵌重功能：

- 版本台账
- 发布批次差异
- 厂商映射建议
- 厂商映射台账
- 映射命中预览
- 映射回放

#### 8.3.2 `/devices`

- 详情抽屉
- 编辑
- 批量导入
- 更换设备
- 导出列设置
- 导出选中
- 导出当前结果
- 跳转对象洞察

#### 8.3.3 `/protocol-governance`

- 协议族草稿维护
- 协议族发布申请
- 协议族回滚申请
- 解密档案草稿维护
- 解密试算
- 解密回放
- 解密档案发布申请
- 解密档案回滚申请
- 协议模板草稿维护
- 协议模板回放
- 协议模板快照发布

#### 8.3.4 `/risk-point`

- 风险点详情
- 正式绑定维护
- 待治理转正
- 治理历史查看

#### 8.3.5 `/governance-task`

- 决策说明
- 去处理
- 复盘
- 确认
- 阻塞
- 关闭
- 提交复盘结论

#### 8.3.6 `/governance-ops`

- 复盘
- 确认
- 抑制
- 关闭
- 提交复盘结论

#### 8.3.7 `/governance-approval`

- 审批详情
- 审批预演查看
- 发布影响分析查看
- 状态流转查看

#### 8.3.8 `/in-app-message`

- 消息详情
- 桥接统计查看
- 桥接结果查看
- 桥接详情查看

#### 8.3.9 `/reporting`

工作区：

- 结果复盘
- 模拟上报
- 最近会话

辅助动作：

- 继续链路追踪
- 复制实际 payload
- 复制响应

#### 8.3.10 `/message-trace`

- 链路详情
- 时间线查看
- Payload 对比查看

## 9. 实施方案

### 9.1 主改文件

主改文件：

- `sql/init-data.sql`

文档更新文件：

- `docs/02-业务功能与流程说明.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`

如有必要补充权限口径说明，再更新：

- `docs/03-接口规范与接口清单.md`

### 9.2 SQL 实现策略

#### 9.2.1 `sys_menu`

对页面节点和权限节点采用幂等插入或更新：

- 新节点直接插入
- 既有节点通过 `ON DUPLICATE KEY UPDATE` 对齐属性
- 父节点调整直接更新 `parent_id`

#### 9.2.2 `sys_role_menu`

新增功能点的角色绑定不重建全量矩阵，而是根据父页面已授权角色自动补齐：

- 找出已绑定父页面的角色
- 为这些角色补充子权限节点绑定
- 避免重复插入

## 10. 验证方案

### 10.1 静态核对

核对以下事项：

1. 当前真实工作页是否都能在 `sys_menu` 找到对应页面节点。
2. 本次定义的按钮/功能点是否都能在 `sys_menu` 找到对应权限项。
3. 新增权限项是否都挂在正确父页面下。
4. 新增权限项是否都已继承父页面角色绑定。

### 10.2 种子级自检

至少检查：

1. `menu_code` 无重复冲突。
2. 未误把总览页、兼容入口、规划页补进菜单。
3. 协议治理权限已从产品页下迁回协议治理页下。

### 10.3 验证边界

若仓库中无现成权限种子校验器，本轮允许通过文本级核对、SQL 级核对和变更说明完成交付，但必须在交付说明中明确：

- 已做哪些静态核对
- 未做哪些运行态验证
- 后续如需实库回灌应如何执行

## 11. 风险与取舍

### 11.1 不立即接前端消费

新增内部功能点权限项本轮主要用于补齐数据库真相，不强制前端立即接显隐控制。这是为了降低当前改动面，避免影响现网页面行为。

### 11.2 `/device-onboarding` 暂不入库

尽管代码和工作台入口已经存在，但主文档尚未将其纳入正式交付基线。本轮按“混合最小迁移”策略，将其排除，避免提前暴露试验页。

### 11.3 角色继承优先于重新分配

本轮不重新定义角色矩阵，只做父页面角色继承。这样能保持最小风险，但也意味着细粒度权限治理会留待后续专门一轮再收口。

## 12. 预期结果

实施完成后，系统会获得以下状态：

1. 当前真实业务工作页、按钮和重页面内部功能点在 `sys_menu` 中有完整真相。
2. 新增项在 `sys_role_menu` 中默认继承父页面现有角色授权。
3. 前端现有路由、导航和按钮行为保持不变。
4. 后续若要推进菜单重构或细粒度权限收口，可以直接基于本轮种子继续演进，而不需要先补历史缺口。
