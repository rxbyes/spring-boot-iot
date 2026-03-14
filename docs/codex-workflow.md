# Codex Workflow

## 推荐任务格式
每次在 Codex App 中发任务，建议使用以下模板：

Task:
<要实现的目标>

Context:
- docs/01-architecture.md
- docs/03-database.md
- docs/05-protocol.md
- docs/07-message-flow.md

Constraints:
- keep module boundaries
- do not add unnecessary dependencies
- use com.ghlzm.iot

Output:
- implementation plan
- changed files
- run/test steps

## 推荐执行顺序
1. 先让 Codex 阅读 AGENTS.md 和 docs
2. 再执行 codex-roadmap 的单个任务
3. 每次只做一个小阶段
4. 变更后审查 diff
5. 合并后更新 change-log 和 todo
