# 数据字典语义种子与历史项对齐设计

**日期：** 2026-04-03

## 背景

风险等级与告警等级语义已在后端、前端和主文档中完成拆分：

- `riskPointLevel`：风险点档案等级，`level_1 / level_2 / level_3`
- `alarmLevel`：告警等级，`red / orange / yellow / blue`
- `currentRiskLevel` / 兼容 `riskLevel`：运行态风险等级，`red / orange / yellow / blue`

当前问题不在页面展示层，而在共享环境历史库仍可能保留旧字典项，例如 `critical / warning / info / high / medium / low`。数据字典页面直接读取 `sys_dict_item`，因此用户仍会看到历史值，造成“实现已切换，字典仍是旧口径”的不一致。

## 目标

把风险治理相关字典的权威数据源统一收口到当前业务口径，保证：

1. 新库执行 `sql/init-data.sql` 后，字典直接是新语义。
2. 旧库执行 `scripts/run-real-env-schema-sync.py` 后，历史字典项会被归并到新语义，不再继续作为独立可选项暴露。
3. 业务表中的历史值仍可被脚本归一，不要求先手工清库再使用。

## 范围

本次只处理以下三套字典及其历史兼容映射：

- `risk_point_level`
- `alarm_level`
- `risk_level`

不扩展到其他菜单图标、通知优先级、帮助中心内容或普通业务字典。

## 方案

1. `sql/init-data.sql` 保持三套权威字典种子：
   - `risk_point_level`：`level_1 / level_2 / level_3`
   - `alarm_level`：`red / orange / yellow / blue`
   - `risk_level`：`red / orange / yellow / blue`
2. `scripts/run-real-env-schema-sync.py` 继续作为历史库对齐入口，但行为从“补齐存在”升级为“补齐并收口”：
   - 确保三套 `sys_dict` 主记录名称、备注、排序符合当前基线。
   - 确保三套 `sys_dict_item` 只保留权威目标项。
   - 旧值通过 `legacy_values` 映射到目标项，例如：
     - `critical -> red`
     - `warning / high -> orange`
     - `medium -> yellow`
     - `info / low -> blue`
   - 对已存在的旧字典项，不再继续保留为独立可见项；脚本应把其内容归并到目标项并清理多余历史项。
3. 业务表兼容保持不变：
   - `risk_point.current_risk_level` / `risk_point.risk_level`
   - `iot_event_record.risk_level`
   - `rule_definition.alarm_level`
   - `emergency_plan.alarm_level` / `emergency_plan.risk_level`
   - `iot_alarm_record.alarm_level`
   这些字段继续由 schema sync 做值归一，避免历史库存阻塞页面与接口使用。
4. 前端和字典 API 不新增临时隐藏逻辑；字典页直接依赖库内正确数据，避免“页面看起来正确、数据库仍脏”的假修复。

## 测试与验证

1. 先补脚本行为测试，锁定三套字典的目标项集合与旧值映射。
2. 再执行定向脚本验证或最小回归，确认旧值不会继续作为独立 `sys_dict_item` 暴露。
3. 如需补文档，只更新最小必要文档，优先落在 `docs/08-变更记录与技术债清单.md`。

## 风险与约束

1. 本次会主动清理共享环境中的历史字典项，属于有意行为，不应再把旧值视作保留能力。
2. `risk_point_level` 无法从历史 severity 无损反推；脚本只负责字典基线和运行态/告警值归一，不负责替历史风险点自动补档案等级。
3. 现有工作区已有未提交改动，本次实施必须继续只触达字典种子、历史库对齐脚本、必要测试与最小文档。
