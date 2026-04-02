# 风险对象中心四色治理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将风险对象中心升级为“四色风险等级 + 数据字典驱动 + 区域/负责人/审计字段补齐”的统一治理基线，并完成历史三档数据兼容迁移。

**Architecture:** 后端继续沿用现有 `risk_level` 字段名，但将其正式口径原位升级为 `red/orange/yellow/blue`，并通过字典服务、风险等级兼容归一化工具和真实环境迁移脚本保证新旧值同时可读。前端不再硬编码正式风险等级选项，而是统一从 `risk_level` 数据字典读取并通过共享 helper 渲染标签、文案和排序，同时补齐风险点档案字段的新增、编辑和只读回显链路。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis-Plus、Vue 3、TypeScript、Vitest、SQL 初始化脚本、Python 真实环境同步脚本。

---

## 文件职责拆分

- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/DictServiceImpl.java`
  - 为 `getByCode("risk_level")` 补齐启用字典项装配能力，作为前端字典驱动的统一入口。
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Dict.java`
  - 继续承载 `items` 透出，不新增平行返回结构。
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java`
  - 修正 `createBy/updateBy` 字段映射，恢复真实持久化。
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
  - 将新增/编辑服务签名升级为接收当前用户编号。
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
  - 注入 `Authentication` 并提取 `JwtUserPrincipal.userId()`，把审计填充收口在风险点写入入口。
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
  - 增加区域/负责人/风险等级校验、旧值兼容归一化、创建人/更新人自动填充和编辑回显约束。
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/AutoClosureSeverity.java`
  - 让风险点等级回写值改为四色内码。
- `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureService.java`
  - 把风险等级比较、预案匹配 fallback、历史值兼容都切到四色标准。
- `spring-boot-iot-ui/src/api/dict.ts`
  - 复用已有 `getDictByCode`，消费 `items` 列表。
- `spring-boot-iot-ui/src/api/riskPoint.ts`
  - 更新风险点字段注释与类型语义，补齐创建人/更新人只读透出。
- `spring-boot-iot-ui/src/utils/riskLevel.ts`
  - 新增共享风险等级 helper，统一兼容旧值、渲染文案、颜色和排序。
- `spring-boot-iot-ui/src/views/RiskPointView.vue`
  - 改为字典驱动风险等级，新增区域/负责人/创建人/更新人展示与编辑逻辑，新增页隐藏编号。
- `spring-boot-iot-ui/src/components/RiskMonitoringDetailDrawer.vue`
  - 改成复用共享风险等级 helper。
- `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
  - 改成复用共享风险等级 helper。
- `spring-boot-iot-ui/src/views/RiskGisView.vue`
  - 改成复用共享风险等级 helper。
- `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
  - 改成复用共享风险等级 helper。
- `spring-boot-iot-ui/src/views/EventDisposalView.vue`
  - 改成复用共享风险等级 helper。
- `spring-boot-iot-ui/src/components/EventDetailDrawer.vue`
  - 改成复用共享风险等级 helper。
- `spring-boot-iot-ui/src/views/EmergencyPlanView.vue`
  - 改成字典驱动风险等级选项和展示。
- `sql/init-data.sql`
  - 把 `risk_level` 字典与演示数据升级到四色内码。
- `scripts/run-real-env-schema-sync.py`
  - 增加真实环境四色字典同步和旧值迁移语句。
- `docs/02-业务功能与流程说明.md`
  - 更新风险对象中心档案字段和四色等级口径。
- `docs/03-接口规范与接口清单.md`
  - 更新风险点写接口和字典接口的字段/返回说明。
- `docs/04-数据库设计与初始化数据.md`
  - 更新 `risk_point` 与 `risk_level` 字典初始化口径。
- `docs/08-变更记录与技术债清单.md`
  - 记录四色治理升级与兼容迁移。
- `docs/21-业务功能清单与验收标准.md`
  - 更新风险对象中心验收项。

### Task 1: 后端字典与风险点写链路

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/DictService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/DictServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

- [ ] **Step 1: 写失败测试，覆盖风险点新增/编辑的四色归一化、区域/负责人校验和审计字段自动填充**

```java
@Test
void addRiskPointShouldNormalizeLegacyLevelAndFillArchiveFields() {
    RiskPoint input = new RiskPoint();
    input.setRiskPointName("北侧边坡");
    input.setOrgId(7101L);
    input.setRegionId(9101L);
    input.setRegionName("东一区");
    input.setResponsibleUser(88L);
    input.setRiskLevel("warning");

    RiskPoint saved = service.addRiskPoint(input, 1001L);

    assertEquals("orange", saved.getRiskLevel());
    assertEquals(1001L, saved.getCreateBy());
    assertEquals(1001L, saved.getUpdateBy());
}
```

- [ ] **Step 2: 运行后端单测，确认按预期失败**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest test`

Expected: 编译失败或断言失败，原因是 `RiskPointService` 还未接收当前用户编号，且旧风险等级和档案字段还未归一化。

- [ ] **Step 3: 写最小实现，补齐字典项装配和风险点写链路**

```java
public Dict getByCode(String dictCode) {
    Dict dict = this.getOne(new LambdaQueryWrapper<Dict>()
            .eq(Dict::getDictCode, dictCode)
            .eq(Dict::getDeleted, 0));
    if (dict == null) {
        return null;
    }
    dict.setItems(dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
            .eq(DictItem::getDictId, dict.getId())
            .eq(DictItem::getDeleted, 0)
            .eq(DictItem::getStatus, 1)
            .orderByAsc(DictItem::getSortNo)
            .orderByAsc(DictItem::getId)));
    return dict;
}

private String normalizeRiskLevel(String rawLevel) {
    return switch (rawLevel == null ? "" : rawLevel.trim().toLowerCase()) {
        case "critical" -> "red";
        case "warning" -> "orange";
        case "info" -> "blue";
        default -> rawLevel == null ? "" : rawLevel.trim().toLowerCase();
    };
}
```

- [ ] **Step 4: 重跑后端单测，确认通过**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest test`

Expected: PASS，且新增/编辑都使用四色内码和自动审计字段。

- [ ] **Step 5: 提交这一轮后端写链路改动**

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/DictService.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/DictServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java
git commit -m "feat: normalize risk point four-color writes"
```

### Task 2: 自动闭环与风险等级兼容消费链路

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/AutoClosureSeverity.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureService.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureServiceTest.java`

- [ ] **Step 1: 写失败测试，覆盖四色风险点回写和旧值兼容比较**

```java
@Test
void processShouldUpdateRiskPointToYellowColorCode() {
    service.process(buildEvent("84330701", Map.of("dispsX", 7.5)));
    verify(riskPointMapper).updateById(argThat(point -> "yellow".equals(point.getRiskLevel())));
}
```

- [ ] **Step 2: 运行目标单测，确认仍按旧三档失败**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=DeepDisplacementAutoClosureServiceTest test`

Expected: FAIL，当前实现仍把黄色/橙色折叠成 `warning`。

- [ ] **Step 3: 写最小实现，切换四色风险等级并保留旧值兼容**

```java
enum AutoClosureSeverity {
    BLUE("blue", "蓝", "low", "blue", false, false, 0),
    YELLOW("yellow", "黄", "medium", "yellow", true, false, 1),
    ORANGE("orange", "橙", "high", "orange", true, true, 2),
    RED("red", "红", "critical", "red", true, true, 3);
}

private String normalizeLevel(String level) {
    return switch ((level == null ? "" : level.trim().toLowerCase(Locale.ROOT))) {
        case "critical" -> "red";
        case "warning" -> "orange";
        case "info" -> "blue";
        default -> level == null ? "" : level.trim().toLowerCase(Locale.ROOT);
    };
}
```

- [ ] **Step 4: 重跑目标单测，确认自动闭环链路通过**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=DeepDisplacementAutoClosureServiceTest test`

Expected: PASS，红橙黄蓝写入风险点，新旧值比较逻辑稳定。

- [ ] **Step 5: 提交自动闭环兼容改动**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/AutoClosureSeverity.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureService.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureServiceTest.java
git commit -m "feat: align auto closure risk levels"
```

### Task 3: 前端风险对象中心与共享风险等级 helper

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/api/dict.ts`
- Create: `spring-boot-iot-ui/src/utils/riskLevel.ts`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/components/RiskMonitoringDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskGisView.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/views/EventDisposalView.vue`
- Modify: `spring-boot-iot-ui/src/components/EventDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/EmergencyPlanView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: 先写失败测试，锁定新增页隐藏编号、编辑页显示只读编号和四色字典加载**

```ts
it('hides the risk point code field for create mode and shows audit fields for edit mode', async () => {
  const wrapper = mountView()
  await flushPromises()

  await wrapper.vm.handleAdd()
  expect(wrapper.text()).not.toContain('风险点编号')

  await wrapper.vm.handleEdit(createRiskPointRow())
  expect(wrapper.text()).toContain('风险点编号')
  expect(wrapper.text()).toContain('创建人编号')
  expect(wrapper.text()).toContain('更新人编号')
})
```

- [ ] **Step 2: 运行前端目标单测，确认按预期失败**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

Expected: FAIL，当前页面仍硬编码三档等级，新增页也仍展示“保存后自动生成”的编号字段。

- [ ] **Step 3: 写最小实现，新增共享 helper 并改造风险点页面**

```ts
export function normalizeRiskLevel(value?: string | null): string {
  switch ((value || '').trim().toLowerCase()) {
    case 'critical':
      return 'red'
    case 'warning':
      return 'orange'
    case 'info':
      return 'blue'
    default:
      return (value || '').trim().toLowerCase()
  }
}

export function getRiskLevelLabel(value?: string | null, options: RiskLevelOption[] = []) {
  const normalized = normalizeRiskLevel(value)
  return options.find((item) => item.value === normalized)?.label || normalized || '未标注'
}
```

- [ ] **Step 4: 重跑前端目标单测，确认通过**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

Expected: PASS，且风险点页面、预案页面和详情页统一消费 helper/字典数据。

- [ ] **Step 5: 提交前端四色治理改动**

```bash
git add spring-boot-iot-ui/src/api/riskPoint.ts \
  spring-boot-iot-ui/src/api/dict.ts \
  spring-boot-iot-ui/src/utils/riskLevel.ts \
  spring-boot-iot-ui/src/views/RiskPointView.vue \
  spring-boot-iot-ui/src/components/RiskMonitoringDetailDrawer.vue \
  spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue \
  spring-boot-iot-ui/src/views/RiskGisView.vue \
  spring-boot-iot-ui/src/views/DeviceInsightView.vue \
  spring-boot-iot-ui/src/views/EventDisposalView.vue \
  spring-boot-iot-ui/src/components/EventDetailDrawer.vue \
  spring-boot-iot-ui/src/views/EmergencyPlanView.vue \
  spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: drive risk levels from dictionary"
```

### Task 4: 初始化数据、真实环境同步与文档

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 写失败验证点，先确认脚本和文档还没体现四色口径**

```bash
Select-String -Path sql/init-data.sql,scripts/run-real-env-schema-sync.py,docs/02-业务功能与流程说明.md -Pattern "critical|warning|info"
```

Expected: 能搜到旧三档正式口径，说明迁移和文档尚未完成。

- [ ] **Step 2: 修改初始化数据、真实环境迁移和文档**

```sql
UPDATE risk_point
SET risk_level = CASE risk_level
    WHEN 'critical' THEN 'red'
    WHEN 'warning' THEN 'orange'
    WHEN 'info' THEN 'blue'
    ELSE risk_level
END
WHERE risk_level IN ('critical', 'warning', 'info');
```

```python
cur.execute(
    """
    UPDATE iot_event_record
    SET risk_level = CASE risk_level
        WHEN 'critical' THEN 'red'
        WHEN 'warning' THEN 'orange'
        WHEN 'info' THEN 'blue'
        ELSE risk_level
    END
    WHERE risk_level IN ('critical', 'warning', 'info')
    """
)
```

- [ ] **Step 3: 运行针对性验证，确认脚本和测试通过**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPointServiceImplTest,DeepDisplacementAutoClosureServiceTest test`

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

Expected: 后端目标单测通过，前端目标单测通过，文档与脚本不再把旧三档当作正式标准。

- [ ] **Step 4: 提交数据迁移和文档改动**

```bash
git add sql/init-data.sql scripts/run-real-env-schema-sync.py \
  docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md \
  docs/21-业务功能清单与验收标准.md
git commit -m "docs: document four-color risk governance"
```

## 自检

- 规格覆盖：
  - 四色等级内码、字典驱动、历史三档迁移、风险点字段补齐、自动闭环兼容、前端消费链路、真实环境脚本、文档更新，均已有对应任务。
- 占位符扫描：
  - 未使用 `TODO/TBD/稍后实现` 一类占位描述。
- 类型一致性：
  - 后端统一使用 `red/orange/yellow/blue` 作为正式内码，旧值只作为 `normalizeRiskLevel` 兼容输入。

计划文件已保存到 `docs/superpowers/plans/2026-04-02-risk-point-four-color-governance-implementation-plan.md`。用户已明确要求继续执行，因此后续直接按该计划进入实现，不再单独等待执行方式确认。
