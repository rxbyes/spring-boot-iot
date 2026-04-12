# 治理契约门禁二期覆盖增强设计

## 1. 背景

`2026-04-11` 已完成“产品物模型设计器二期治理工作流”主体收口，当前新增并已通过实现验证的关键语义包括：

- 前端 `治理阶段总览`
- 前端 `治理候选快照`
- 前端 `审批提交回执 / 正式发布结果` 区分
- 后端 `submittedItemCount` 提交层回执语义

现有本地专项门禁入口 [scripts/run-governance-contract-gates.mjs](../../../../scripts/run-governance-contract-gates.mjs) 已覆盖产品治理主线，但前端步骤当前只跑 `ProductModelDesignerWorkspace.test.ts`，后端显式测试清单也还没有把本轮新增的 `ProductContractGovernanceApprovalPayloadsTest` 写进固定门禁范围。这样会留下一个明显缺口：

1. 二期主流程虽已落地，但“治理候选快照”和“审批提交回执”这些新语义还没有全部进入本地专项门禁。
2. 后续如果有人回退 compare 文案、apply 回执或 `submittedItemCount` 语义，本地门禁不一定能第一时间拦住。

因此，本轮目标不是再做新的门禁体系，而是对现有专项门禁做一次**最小覆盖增强**。

## 2. 目标

本轮只达成以下目标：

1. 继续使用 `node scripts/run-governance-contract-gates.mjs` 作为唯一专项门禁入口。
2. 把二期新增语义稳定纳入门禁覆盖范围。
3. 让本地门禁对这些语义回退具备稳定的 fail/pass 信号。
4. 同步更新正式质量文档，确保脚本与文档口径一致。

## 3. 非目标

本轮明确不做以下事项：

1. 不引入 GitHub Actions、Jenkinsfile 或其他托管 CI。
2. 不修改统一入口 `node scripts/run-quality-gates.mjs` 的行为。
3. 不新增浏览器验收、真实环境验收或新的 acceptance 计划。
4. 不把门禁升级成新的平行体系，例如 `governance-contract-gates-v2`。

## 4. 方案选型

### 4.1 方案 A：最小增量增强现有专项门禁

做法：

- 修改 [scripts/run-governance-contract-gates.mjs](../../../../scripts/run-governance-contract-gates.mjs)
- 修改 [scripts/run-governance-contract-gates.test.mjs](../../../../scripts/run-governance-contract-gates.test.mjs)
- 修改 [docs/05-自动化测试与质量保障.md](../../../05-%E8%87%AA%E5%8A%A8%E5%8C%96%E6%B5%8B%E8%AF%95%E4%B8%8E%E8%B4%A8%E9%87%8F%E4%BF%9D%E9%9A%9C.md)

增强点固定为：

1. 前端门禁步骤同时跑 `ProductModelDesignerWorkspace.test.ts` 和 `ProductModelGovernanceCompareTable.test.ts`
2. 后端门禁步骤显式包含 `ProductContractGovernanceApprovalPayloadsTest`
3. 文档把专项门禁覆盖说明更新为“已覆盖二期治理工作流新增语义”

优点：

1. 改动最小，完全贴合“只做本地专项门禁增强”的目标
2. 不会影响统一质量门禁主脚本
3. 后续若接托管 CI，可直接复用当前脚本

缺点：

1. 仍然是一个聚合门禁步骤，不进一步细分失败类别

### 4.2 方案 B：增强现有专项门禁并拆细日志步骤

做法：

- 在方案 A 基础上，把前后端步骤继续拆成更多语义子步骤

优点：

1. 失败定位更细

缺点：

1. 脚本改动更大
2. 超出当前“最小增强”目标

### 4.3 结论

采用 **方案 A**。

原因：

1. 当前用户只要求本地专项门禁覆盖二期新增语义，并不要求门禁架构升级。
2. 二期新增语义已有稳定测试，最稳妥的做法是把这些既有测试纳入门禁，而不是再设计新的执行层。

## 5. 受影响文件

- 修改 [scripts/run-governance-contract-gates.mjs](../../../../scripts/run-governance-contract-gates.mjs)
- 修改 [scripts/run-governance-contract-gates.test.mjs](../../../../scripts/run-governance-contract-gates.test.mjs)
- 修改 [docs/05-自动化测试与质量保障.md](../../../05-%E8%87%AA%E5%8A%A8%E5%8C%96%E6%B5%8B%E8%AF%95%E4%B8%8E%E8%B4%A8%E9%87%8F%E4%BF%9D%E9%9A%9C.md)

本轮不预计改动其他代码模块。

## 6. 测试与验收

### 6.1 最小测试策略

按 TDD 执行：

1. 先让 `scripts/run-governance-contract-gates.test.mjs` 对新的覆盖清单提出失败断言
2. 再修改 `scripts/run-governance-contract-gates.mjs`
3. 跑脚本测试确认通过
4. 跑真实专项门禁命令 `node scripts/run-governance-contract-gates.mjs`
5. 更新文档后再跑 `node scripts/docs/check-topology.mjs`

### 6.2 完成定义

完成后，系统应能稳定回答：

1. 本地专项门禁是否已覆盖二期新增语义
2. 若 `治理候选快照` 相关前端测试回退，专项门禁是否会失败
3. 若 `审批提交回执 / submittedItemCount` 相关后端语义回退，专项门禁是否会失败

## 7. 文档影响

本轮除脚本外，只同步更新：

- [docs/05-自动化测试与质量保障.md](../../../05-%E8%87%AA%E5%8A%A8%E5%8C%96%E6%B5%8B%E8%AF%95%E4%B8%8E%E8%B4%A8%E9%87%8F%E4%BF%9D%E9%9A%9C.md)

`README.md` 与 `AGENTS.md` 本轮预计不需要改动。
