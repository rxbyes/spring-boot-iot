# IoT Business Governance Docs Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把已确认的三域关系与 IoT 五层治理设计，收口到现有权威文档与一份可执行的业务附录指引中，形成业务人员可直接查阅和执行的文档体系。

**Architecture:** 采用“权威摘要 + 附录详表 + 导航入口”的文档结构：`docs/02` 负责业务语义和五层治理摘要，`docs/21` 负责交付边界与引用说明，`docs/appendix/iot-field-governance-and-sop.md` 负责字段治理表与建档联调 SOP，`docs/README.md` 负责导航。这样既不新增平行主文档，也能把细节沉到单一补充文档。

**Tech Stack:** Markdown、PowerShell、Git、仓库既有 docs 体系

---

## File Map

- `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
  - 权威业务语义文档；补入三域关系、IoT 五层治理模型、产品/设备/物模型录入规则摘要。
- `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
  - 交付边界文档；补入“正式交付 / 当前可用 / 规划中”的引用说明，避免业务指引被误读为新增交付范围。
- `E:\idea\ghatg\spring-boot-iot\docs\appendix\iot-field-governance-and-sop.md`
  - 新增单一补充文档；承载变形监测字段治理标准表、业务建档与联调 SOP、字段分类和核对项。
- `E:\idea\ghatg\spring-boot-iot\docs\README.md`
  - 文档入口；增加对新附录的导航。
- `E:\idea\ghatg\spring-boot-iot\README.md`
  - 只检查是否需要同步，不预期修改。
- `E:\idea\ghatg\spring-boot-iot\AGENTS.md`
  - 只检查是否需要同步，不预期修改。

### Task 1: 更新权威业务语义文档

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`

- [ ] **Step 1: 在业务域划分区域补入三域关系摘要**

在 `docs/02-业务功能与流程说明.md` 的业务域划分附近补入如下结构：

```md
### 1.x 三域关系总览

1. `IoT 主链路`
   - 负责产品、设备、物模型、属性/遥测、消息日志，是上游数据源。
2. `风险平台`
   - 消费 IoT 数据，完成风险点、规则、告警、事件、工单闭环。
3. `系统管理`
   - 提供组织、区域、字典、通知渠道、审计日志等治理底座。

主关系：
1. `IoT 主链路 -> 风险平台`
2. `系统管理 -> IoT 主链路`
3. `系统管理 -> 风险平台`
4. `风险平台 -> IoT 主链路`
```

- [ ] **Step 2: 在 IoT 主流程附近补入五层治理模型与录入规则摘要**

把以下内容以小节方式并入 `docs/02-业务功能与流程说明.md` 的设备接入域说明中：

```md
### 2.x IoT 五层治理模型

`产品 -> 设备 -> 物模型 -> 属性/遥测 -> 消息日志`

1. 产品：定义一类设备的接入模板和业务身份。
2. 设备：定义某台实际资产实例。
3. 物模型：定义平台认可的正式字段标准。
4. 属性/遥测：定义运行期最新值和历史值。
5. 消息日志：定义原始报文证据和处理留痕。

最小规则：
1. 先建产品，后建设备。
2. 同一产品下 `identifier` 必须唯一。
3. 核心判定字段必须同时明确 latest 和 telemetry 口径。
4. 失败上报也必须保留消息日志。
5. 父设备 / 子设备场景必须明确风险责任归属。
```

- [ ] **Step 3: 增加指向附录的单一引用**

在上述新小节末尾增加一条引用，固定写法如下：

```md
详细字段治理表与业务建档联调步骤，参考 [appendix/iot-field-governance-and-sop.md](../../appendix/iot-field-governance-and-sop.md)。
```

- [ ] **Step 4: 运行文档存在性检查**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md' -Pattern '三域关系总览|IoT 五层治理模型|iot-field-governance-and-sop'
```

Expected: 至少能检索到以上三个关键字。

- [ ] **Step 5: 提交**

```bash
git add docs/02-业务功能与流程说明.md
git commit -m "docs: add iot business governance summary"
```

### Task 2: 更新交付边界文档

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: 在 IoT 基础域或 Phase 4 说明处补入引用说明**

把以下说明并入 `docs/21-业务功能清单与验收标准.md`：

```md
补充说明：
1. 业务治理层面的“产品 -> 设备 -> 物模型 -> 属性/遥测 -> 消息日志”关系说明，以及变形监测字段治理标准，属于业务口径整理资产。
2. 这些说明用于指导建档、联调、风险绑定和字段治理，不自动扩大正式交付范围。
3. 当前正式交付、当前可用和规划中的边界，仍以本文件和 `docs/19` 为准。
```

- [ ] **Step 2: 在 IoT 基础域条目后补一条阅读指引**

加入如下引用：

```md
业务人员如需查看字段治理标准表和建档联调步骤，参考 [appendix/iot-field-governance-and-sop.md](../../appendix/iot-field-governance-and-sop.md)。
```

- [ ] **Step 3: 运行边界一致性检查**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md' -Pattern '不自动扩大正式交付范围|iot-field-governance-and-sop'
```

Expected: 能看到“边界不扩大”和附录引用两类文案。

- [ ] **Step 4: 提交**

```bash
git add docs/21-业务功能清单与验收标准.md
git commit -m "docs: clarify governance guidance delivery boundary"
```

### Task 3: 新增字段治理与建档联调附录

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\docs\appendix\iot-field-governance-and-sop.md`

- [ ] **Step 1: 写入附录头部和适用范围**

用以下头部起草新文档：

```md
# IoT Field Governance And SOP

> 文档定位：面向业务人员的字段治理与建档联调补充指引。
> 适用角色：业务、交付、实施、联调、运维。
> 权威级别：附录补充，业务语义仍以 `docs/02` 为准，交付边界仍以 `docs/21` 与 `docs/19` 为准。
> 更新时间：2026-03-31

## 1. 使用方式

本附录只回答三件事：

1. 如何把监测规范表转成平台字段治理标准。
2. 业务人员建产品、建设备、配模型时该填什么。
3. 联调时应该检查 latest、telemetry、message-log 哪些结果。
```

- [ ] **Step 2: 写入字段治理标准表骨架和首批样例**

加入如下表头与样例行：

```md
## 2. 变形监测字段治理标准表

| 监测内容编码 | 监测类型编码 | 字段标识 | 中文名称 | 单位 | latest | telemetry | 核心判定字段 | 平台计算 | 上报要求 | 业务解释 |
|---|---|---|---|---|---|---|---|---|---|---|
| L1 | LF | value | 裂缝张开度 | mm | 是 | 是 | 是 | 否 | 必传 | 裂缝位移随时间的累计变化量 |
| L1 | GP | gpsInitial | GNSS 原始观测基础数据 | - | 可选 | 可选 | 否 | 否 | 条件必传 | 作为累计位移的计算基础 |
| L1 | GP | gpsTotalX | X 方向累计位移量 | mm | 是 | 是 | 是 | 是 | 建议必传 | 相对初始位置的累计变形量 |
| L1 | GP | gpsTotalY | Y 方向累计位移量 | mm | 是 | 是 | 是 | 是 | 建议必传 | 相对初始位置的累计变形量 |
| L1 | GP | gpsTotalZ | Z 方向累计位移量 | mm | 是 | 是 | 是 | 是 | 建议必传 | 相对初始位置的累计变形量 |
| L1 | SW | dispsX | 顺滑方向累计变形量 | mm | 是 | 是 | 是 | 是 | 必传 | 深部位移主判定指标 |
| L1 | SW | dispsY | 垂直坡面方向累计变形量 | mm | 是 | 是 | 是 | 是 | 必传 | 深部位移主判定指标 |
| L1 | JS | gX | X 轴加速度周期代表值 | m/s² | 是 | 是 | 视规则而定 | 是 | 必传 | 一个上传周期内绝对值最大时刻的轴向值 |
| L1 | JS | gY | Y 轴加速度周期代表值 | m/s² | 是 | 是 | 视规则而定 | 是 | 必传 | 一个上传周期内绝对值最大时刻的轴向值 |
| L1 | JS | gZ | Z 轴加速度周期代表值 | m/s² | 是 | 是 | 视规则而定 | 是 | 必传 | 一个上传周期内绝对值最大时刻的轴向值 |
```

- [ ] **Step 3: 写入业务建档与联调 SOP**

把以下步骤直接写成文档正文：

```md
## 3. 业务建档与联调 SOP

1. 先确认监测内容编码和监测类型编码。
2. 创建产品，固定 `productKey / productName / manufacturer / protocolCode / nodeType / dataFormat`。
3. 配置正式物模型，优先录入核心判定字段。
4. 创建设备，明确 `deviceCode`、区域、组织、安装位置和父子关系。
5. 发起联调，至少同时检查：
   - `GET /api/device/{deviceCode}/properties`
   - `GET /api/device/{deviceCode}/message-logs`
   - `GET /api/telemetry/latest`
6. 完成风险点绑定和规则配置，再验证告警 / 事件 / 工单闭环。
```

- [ ] **Step 4: 写入核对清单**

加入如下核对清单：

```md
## 4. 联调核对清单

1. latest 字段名是否与物模型一致
2. telemetry 是否按预期落库
3. message-log 是否保留原始报文和失败阶段
4. 核心判定字段是否已绑定风险点
5. 父设备 / 子设备责任是否清晰
```

- [ ] **Step 5: 运行附录内容检查**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\docs\appendix\iot-field-governance-and-sop.md' -Pattern '变形监测字段治理标准表|业务建档与联调 SOP|联调核对清单|gpsTotalX|dispsX|gX'
```

Expected: 上述标题和字段样例都能被检索到。

- [ ] **Step 6: 提交**

```bash
git add docs/appendix/iot-field-governance-and-sop.md
git commit -m "docs: add iot field governance appendix"
```

### Task 4: 更新文档导航并做最终一致性检查

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\README.md`
- Verify only: `E:\idea\ghatg\spring-boot-iot\README.md`
- Verify only: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: 在 docs 首页增加附录入口**

在 `docs/README.md` 的常见任务或附录入口中加入如下描述：

```md
| IoT 字段治理与建档联调指引 | [appendix/iot-field-governance-and-sop.md](../../appendix/iot-field-governance-and-sop.md) |
```

- [ ] **Step 2: 检查根 README 和 AGENTS 是否需要同步**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\README.md','E:\idea\ghatg\spring-boot-iot\AGENTS.md' -Pattern 'iot-field-governance-and-sop|字段治理|建档联调'
```

Expected: 无命中或无需修改；本轮默认不改根 `README.md` 与 `AGENTS.md`，除非实现者判断新增了必须暴露的主入口。

- [ ] **Step 3: 运行最终一致性检查**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md','E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md','E:\idea\ghatg\spring-boot-iot\docs\README.md' -Pattern 'iot-field-governance-and-sop'
git diff --check
git status --short
```

Expected:

```text
三个 docs 文件都已引用新附录
git diff --check 无输出
git status 只包含本轮文档改动
```

- [ ] **Step 4: 提交**

```bash
git add docs/README.md docs/02-业务功能与流程说明.md docs/21-业务功能清单与验收标准.md docs/appendix/iot-field-governance-and-sop.md
git commit -m "docs: consolidate iot governance guidance"
```

## Self-Review

### Spec coverage

本计划覆盖了 spec 中的以下要求：

1. 三域关系与五层治理模型回写到权威业务文档
2. `产品 -> 设备 -> 物模型 -> 属性/遥测 -> 消息日志` 的业务规则落到 `docs/02`
3. “正式交付 / 当前可用 / 规划中” 的边界说明落到 `docs/21`
4. 图片规范提炼为结构化字段治理表和业务 SOP，落到单一附录文档
5. 导航入口更新到 `docs/README.md`

无明显遗漏。

### Placeholder scan

已检查：

1. 无禁用占位词和“后补再说”类描述
2. 无“自行补充”式模糊描述
3. 每个任务都给出了明确文件、文本骨架或命令

### Type consistency

计划中统一使用以下命名：

1. `IoT 主链路`
2. `风险平台`
3. `系统管理`
4. `productKey / deviceCode / identifier`
5. `latest / telemetry / message-log`
6. `iot-field-governance-and-sop.md`
