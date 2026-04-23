# 统一 CI 与契约测试门禁设计

## 1. 背景

当前仓库已经把产品物模型治理主链路逐步收口到真实可用状态，最近连续补齐了以下能力：

- 深部位移 `phase3-deep-displacement` 规范治理
- 激光测距复用裂缝 canonical 契约治理
- 翻斗式雨量计 `phase4-rain-gauge` 规范治理
- 产品级 `vendor mapping rule` 的 compare / apply / runtime 三段消费
- 产品合同固定复核人策略

但当前“质量门禁”仍主要停留在以下层级：

- `mvn clean package -DskipTests`
- 前端 `build`
- `component/list/style` guard
- 个别 schema guard
- 文档拓扑检查

现状缺口：

1. 物模型治理主链路虽然已有定向单测和真实环境复验，但没有被统一纳入“仓库级必跑门禁”。
2. 最近补齐的深部位移、激光测距、雨量计、固定复核人、mapping runtime 等治理能力，仍依赖人工记忆来决定“本次要跑哪些测试”。
3. 现有 `run-quality-gates` 更像“基础构建门禁”，无法直接回答“语义契约治理这一刀有没有回退”。
4. 仓库当前没有稳定绑定某一个 CI 平台；若现在直接设计某家平台工作流，容易把任务做散。

因此，下阶段更合适的主体任务不是继续扩治理场景，而是先补一条**平台无关、仓库内可复用、能保护现有治理成果**的统一契约门禁。

目标：

1. 保持现有 `run-quality-gates` 作为统一入口，不引入第二套平行门禁体系。
2. 新增一条“治理契约门禁”最小切片，专门覆盖已经收口的产品物模型治理主线。
3. 门禁结果必须可在本地、未来 CI、以及共享研发环境外的普通开发机上复用。
4. 真实 `dev` 环境验收继续保留为独立 acceptance 体系，不强行塞进 CI 门禁。

## 2. 现状约束

### 2.1 现有质量门禁入口

当前统一入口为：

- `node scripts/run-quality-gates.mjs`

该入口会按操作系统分发到：

- `scripts/run-quality-gates.ps1`
- `scripts/run-quality-gates.sh`

当前执行内容主要是：

- Maven 打包
- 前端 build
- 前端静态 guard
- 风险点 schema baseline guard
- 文档拓扑检查

结论：

1. 现有脚本结构已经具备“统一入口 + 跨平台分发”基础。
2. 更合理的做法是继续复用这套入口，而不是另起一个新的总控脚本。

### 2.2 当前待保护的治理主线

结合最近已经落地并反复修正的能力，本轮真正需要进入门禁的主线是：

- `model-governance/compare`
- `model-governance/apply`
- `VendorMetricMappingRuntimeServiceImpl` 的运行时归一化
- `risk_metric_catalog` 发布规则
- 固定复核人策略解析与产品合同审批接线
- `/products` 产品经营工作台中的关键治理交互

这些能力横跨：

- `spring-boot-iot-device`
- `spring-boot-iot-alarm`
- `spring-boot-iot-system`
- `spring-boot-iot-ui`
- `docs/`

结论：

1. 新门禁必须是“跨模块但聚焦治理链路”的定向回归。
2. 不应升级为全仓 `mvn test`，否则成本高且信号不聚焦。

### 2.3 明确不纳入本轮

本轮不做以下事项：

1. 不直接落某一家 CI 平台工作流 YAML。
2. 不把真实 `dev` 环境登录、接口烟测、浏览器巡检强行纳入本地门禁。
3. 不把所有风险运营、设备资产、首页驾驶舱回归都并入治理门禁。
4. 不新建平行质量体系，例如“contract-quality-gates-v2”。

## 3. 方案选型

### 3.1 方案 A：保持现状，只在文档中补“推荐测试命令”

做法：

- 继续依赖人工在提交前手动执行治理相关测试命令。
- 文档只补命令清单，不改任何脚本。

优点：

- 开发量最低。

缺点：

- 仍然没有统一门禁。
- 无法防止后续提交遗漏治理回归。
- 未来接入 CI 时仍要重新设计入口。

结论：

- 不采用。

### 3.2 方案 B：复用现有 `run-quality-gates`，新增治理契约门禁脚本

做法：

- 保留 `run-quality-gates.mjs` 为唯一总入口。
- 新增一条“治理契约门禁”脚本，由总入口调用。
- 该脚本内部统一编排后端治理定向测试、前端治理定向测试与必要文档/脚本检查。

优点：

- 平台无关，适合本地与未来 CI 复用。
- 与现有质量门禁结构一致，接入成本低。
- 能直接保护当前最脆弱的治理主线。

缺点：

- 比单纯补文档多一层脚本维护成本。

结论：

- 采用本方案。

### 3.3 方案 C：直接落 GitHub Actions / Gitee CI 工作流

做法：

- 直接在仓库中补 CI 平台工作流文件，把治理测试串起来。

优点：

- 平台上能立即自动执行。

缺点：

- 当前仓库尚未形成稳定单一 CI 平台口径。
- 先绑平台，后续很可能还要回头做“仓库内统一入口”。
- 与“先收口主体、后细化平台接入”的原则相反。

结论：

- 本轮不采用。

## 4. 目标设计

### 4.1 门禁分层

统一质量门禁分三层表达：

1. `基础门禁`
   - Maven 打包
   - 前端 build
   - 前端静态 guard
   - schema baseline guard
   - docs topology check
2. `治理契约门禁`
   - 物模型治理与审批相关定向回归
   - 风险目录发布规则回归
   - 前端产品经营工作台治理回归
3. `真实环境验收`
   - 继续通过 acceptance 脚本、注册表和共享 `dev` 环境执行
   - 不并入本地质量门禁

执行关系：

- `run-quality-gates` 继续作为总入口。
- `治理契约门禁` 作为总入口中的一个新增步骤。
- `真实环境验收` 仍属于发布/阶段验收层，不属于本地最小门禁。

### 4.2 目标脚本结构

建议新增：

- `scripts/run-governance-contract-gates.mjs`

职责：

1. 作为平台无关的治理契约门禁执行器。
2. 统一组装后端 Maven 定向测试命令。
3. 统一组装前端 Vitest 定向测试命令。
4. 输出清晰的步骤日志与非零退出码。

现有脚本调整：

- `scripts/run-quality-gates.ps1`
- `scripts/run-quality-gates.sh`

调整方式：

1. 保持现有步骤顺序基本不变。
2. 在静态 guard 之后、docs topology check 之前插入“governance contract gates”步骤。
3. 由 PowerShell / Shell 直接调用 Node 执行器，而不是各自重复拼装一套治理测试命令。

保留不变：

- `scripts/run-quality-gates.mjs` 仍只负责分发到 `ps1/sh`。

这样可以避免：

- 同一套治理命令在 Windows 与 Linux 脚本中重复维护两遍。

### 4.3 治理契约门禁覆盖范围

后端定向回归应覆盖以下主题：

1. 产品物模型 compare / apply 语义归一化
2. 产品级 mapping rule 运行态消费
3. 风险指标目录发布规则
4. 固定复核人策略与产品合同审批接线

建议纳入的后端测试集合：

- `GovernanceApprovalPolicyResolverImplTest`
- `GovernanceApprovalServiceImplTest`
- `ProductModelServiceImplTest`
- `VendorMetricMappingRuntimeServiceImplTest`
- `ProductModelControllerTest`
- `ProductContractReleaseControllerTest`
- `ProductGovernanceApprovalControllerTest`
- `DefaultRiskMetricCatalogPublishRuleTest`

对应模块：

- `spring-boot-iot-system`
- `spring-boot-iot-device`
- `spring-boot-iot-alarm`

前端定向回归应覆盖以下主题：

1. `/products -> 契约字段` compare / apply / 当前已生效字段关键交互
2. 固定复核人模式下的产品工作台行为
3. 产品经营工作台治理文案与工作区关键状态

建议纳入的前端测试集合：

- `src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- 如当前实现已存在治理审批工作台耦合回归，再补 `src/__tests__/views/GovernanceApprovalView.test.ts`

当前推荐原则：

- 先以 `ProductModelDesignerWorkspace.test.ts` 为必选门禁。
- 只有当治理审批页对产品工作台行为构成直接依赖时，才把 `GovernanceApprovalView.test.ts` 纳入本轮最小门禁。

### 4.4 命令口径

治理契约门禁内部建议固定成两条核心命令：

后端：

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest" test
```

前端：

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

兼容规则：

1. 若仓库不存在 `.mvn/settings.xml`，自动退化为 plain `mvn`。
2. 前端仍通过现有 `npm --prefix spring-boot-iot-ui` 方式执行，不引入新包管理器。
3. 任何一步失败都应返回非零退出码并中断总门禁。

### 4.5 日志与可观测性

建议新增单独日志文件：

- `logs/governance-contract-gates.log`

用途：

1. 单独记录治理契约门禁执行过程。
2. 便于和 `logs/quality-gates.log` 区分“基础构建问题”与“治理回归问题”。
3. 未来若接入 CI，可直接上传该日志作为构建产物。

总门禁日志策略：

1. `quality-gates.log` 继续记录总流程。
2. `governance-contract-gates.log` 只记录治理契约门禁的子步骤。

### 4.6 文档口径

需要同步更新的文档：

- `docs/05-自动化测试与质量保障.md`
- `docs/21-业务功能清单与验收标准.md`
- 如命令或入口说明变化涉及仓库总览，再补 `README.md`
- 如执行规则变化涉及协作约束，再补 `AGENTS.md`

文档新增口径：

1. `run-quality-gates` 已包含治理契约门禁。
2. 治理契约门禁的覆盖范围、目的和边界。
3. 它不替代真实 `dev` 环境验收。

## 5. 架构与职责边界

### 5.1 `run-quality-gates` 继续做总控

职责：

- 串起仓库级最小质量门禁。

不新增职责：

- 不理解治理业务语义细节。
- 不直接拼装大量模块级测试清单。

### 5.2 `run-governance-contract-gates` 负责治理专项

职责：

- 只服务“产品物模型治理主链路”的专项回归。
- 维护专项命令、日志与失败语义。

不负责：

- 真实环境启动
- 浏览器冒烟
- schema 自动修复
- 任意业务场景全量回归

### 5.3 acceptance 体系继续独立

继续保留：

- `scripts/start-backend-acceptance.ps1`
- `scripts/start-frontend-acceptance.ps1`
- `scripts/run-message-flow-acceptance.py`
- `scripts/auto/run-acceptance-registry.mjs`

边界：

1. `quality gates` 负责“代码层最小回归”。
2. `acceptance` 负责“真实环境业务可用性”。
3. 两者互补，不互相替代。

## 6. 失败语义

治理契约门禁失败时，应明确告诉开发者失败属于哪一类：

1. 固定复核人审批链路回退
2. compare / apply 治理行为回退
3. mapping runtime 归一化回退
4. 风险目录发布规则回退
5. 前端产品经营工作台治理回退

最低要求：

1. 日志中必须打印当前执行到的步骤名。
2. 脚本退出码必须非零。
3. 不吞异常，不把失败伪装成 warning。

## 7. 验证策略

本轮设计落地后，验证分三层：

### 7.1 脚本级验证

验证点：

1. 新增脚本在 Windows / Linux 路径解析下都能运行。
2. `run-quality-gates` 能成功调起治理专项门禁。
3. 失败时能正确中断并返回非零退出码。

### 7.2 测试级验证

验证点：

1. 后端治理定向测试命令可通过。
2. 前端产品经营工作台治理定向测试可通过。

### 7.3 文档级验证

验证点：

1. `docs/05` 已明确写入治理契约门禁入口与范围。
2. `docs/21` 中“待补齐：统一 CI / 契约测试门禁”更新为已落地的最小口径。
3. 若 `README.md` / `AGENTS.md` 行为口径受影响，也同步回写。

## 8. 实施边界与后续演进

### 8.1 本轮完成定义

本轮完成后，应满足：

1. 仓库内存在统一治理契约门禁脚本。
2. `run-quality-gates` 已纳入该脚本。
3. 治理主线关键测试能通过单一入口执行。
4. 文档已把该门禁固化为正式口径。

### 8.2 明确留到后续

后续再做的事项：

1. 将 `run-quality-gates` 或治理专项门禁接入具体 CI 平台。
2. 根据新增治理场景继续扩充专项测试集，例如更多厂商协议模板。
3. 若未来产品工作台拆出更多治理读写页，再评估是否扩充前端门禁集合。
4. 若 acceptance 注册表进一步稳定，可考虑在发布级门禁中追加“轻量真实环境探活”，但不在本轮实现。

## 9. 结论

本轮最合理的收口方式不是“先选 CI 平台”，而是先把**仓库内、平台无关、聚焦治理主线**的契约门禁建立起来。

采用方案 B 后：

1. 现有 `run-quality-gates` 仍是唯一统一入口。
2. 最近已经收口的深部位移、激光测距、雨量计、固定复核人、mapping runtime 能获得稳定保护。
3. 未来无论接入 Gitee CI、GitHub Actions 还是其他执行器，都只需要调用现成仓库脚本，而不是重新理解治理测试矩阵。
