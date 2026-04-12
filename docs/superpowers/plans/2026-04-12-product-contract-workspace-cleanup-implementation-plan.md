# Product Contract Workspace Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 精简 `/products -> 契约字段` 页面中重复、无状态增量的提示语与流程壳层，让页面聚焦真实可操作内容。

**Architecture:** 直接在 `ProductModelDesignerWorkspace.vue` 内做最小结构收口：删掉阶段轨道与占位壳层，保留真实业务区块，并同步更新前端测试和文档描述。整个改动不改变 compare/apply/审批/回滚 的数据流与接口契约。

**Tech Stack:** Vue 3、TypeScript、Vitest、现有共享工作台样式体系、Markdown 文档

---

### Task 1: 收口契约字段页面结构

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] 删除 `contract-governance-workflow` 阶段轨道及其渲染依赖，不再展示 `待输入样本 / 待确认识别结果 / 待提交审批 / 审批中 / 已发布 / 可回滚`
- [ ] 删除 `contract-governance-primary` / `contract-governance-history` 这组仅承载标题的占位壳层，不再展示 `发布批次与风险联动`
- [ ] 保留 `样本输入 / 识别结果 / 本次生效 / 当前已生效字段` 四类真实内容区块
- [ ] 将区块内重复说明压缩为短句，只在空态、边界约束和审批结果中保留必要文案
- [ ] 复查模板、计算属性和样式，移除只服务于被删壳层的无用代码

### Task 2: 更新前端测试合同

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] 删除对阶段轨道与历史壳层文案的断言
- [ ] 保留对核心区块 `样本输入 / 识别结果 / 当前已生效字段` 的断言
- [ ] 增加“页面不再显示阶段轨道与发布批次壳层文案”的断言
- [ ] 运行该组件测试，确认页面合同与新布局一致

### Task 3: 同步文档收口描述

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] 将契约字段页面描述从“阶段总览 + 主流程区 + 历史辅助区”更新为“聚焦主内容区块的轻量工作区”
- [ ] 明确前端治理规则：优先保留真实动作与结果区块，不再回流无状态增量的流程提示语
- [ ] 复查文档措辞，确保不再要求页面必须展示阶段轨道或历史占位标题

### Task 4: 验证与交付说明

**Files:**
- No file changes required unless发现文档补充点

- [ ] 运行 `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- [ ] 人工检查组件文本，确认页面不再包含 `待输入样本 / 待确认识别结果 / 待提交审批 / 审批中 / 已发布 / 可回滚 / 发布批次与风险联动`
- [ ] 整理本次变更文件、验证命令和文档更新说明，作为交付摘要
