# Risk Point Level And Alarm Level Semantics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split “风险点档案等级” and “告警等级” into independent, business-correct semantics: `riskPointLevel = level_1/level_2/level_3`, `alarmLevel = red/orange/yellow/blue`, while keeping a separate runtime risk color for monitoring and automatic closure.

**Architecture:** Use additive migration instead of in-place semantic overwrite. Introduce `risk_point_level` as the archive-grade field, convert `alarm_level` to four colors, and keep runtime risk color on a dedicated API/property path (`currentRiskLevel`) so risk point master data no longer shares storage semantics with monitoring state.

**Tech Stack:** Spring Boot, MyBatis Plus, Vue 3, TypeScript, Vitest, JUnit 5, Mockito, SQL init scripts, `scripts/run-real-env-schema-sync.py`

---

### Task 1: Introduce Shared Semantics And Transitional Schema

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `spring-boot-iot-ui/src/utils/alarmLevel.ts`
- Create: `spring-boot-iot-ui/src/utils/riskPointLevel.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/alarmLevel.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/utils/riskPointLevel.test.ts`

- [ ] **Step 1: Write the failing frontend semantics tests**

```ts
import { describe, expect, it } from 'vitest'
import { normalizeAlarmLevel, getAlarmLevelText } from '@/utils/alarmLevel'
import { normalizeRiskPointLevel, getRiskPointLevelText } from '@/utils/riskPointLevel'

describe('alarm level semantics', () => {
  it('normalizes legacy values into four-color alarm levels', () => {
    expect(normalizeAlarmLevel('critical')).toBe('red')
    expect(normalizeAlarmLevel('warning')).toBe('orange')
    expect(normalizeAlarmLevel('info')).toBe('blue')
  })

  it('uses four-color labels', () => {
    expect(getAlarmLevelText('red')).toBe('红色')
    expect(getAlarmLevelText('orange')).toBe('橙色')
    expect(getAlarmLevelText('yellow')).toBe('黄色')
    expect(getAlarmLevelText('blue')).toBe('蓝色')
  })
})

describe('risk point level semantics', () => {
  it('normalizes archive grades', () => {
    expect(normalizeRiskPointLevel('level_1')).toBe('level_1')
    expect(normalizeRiskPointLevel('LEVEL_2')).toBe('level_2')
  })

  it('uses archive-grade labels', () => {
    expect(getRiskPointLevelText('level_1')).toBe('一级风险点')
    expect(getRiskPointLevelText('level_2')).toBe('二级风险点')
    expect(getRiskPointLevelText('level_3')).toBe('三级风险点')
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/utils/alarmLevel.test.ts src/__tests__/utils/riskPointLevel.test.ts
```

Expected:
- `alarmLevel.test.ts` fails because current utility still collapses colors to `critical / warning / info`
- `riskPointLevel.test.ts` fails because the file does not exist yet

- [ ] **Step 3: Write minimal shared semantics and schema changes**

Implement:

```sql
ALTER TABLE risk_point
  ADD COLUMN risk_point_level VARCHAR(16) DEFAULT NULL COMMENT '风险点档案等级 level_1/level_2/level_3' AFTER responsible_phone,
  ADD COLUMN current_risk_level VARCHAR(16) DEFAULT NULL COMMENT '当前风险态势等级 red/orange/yellow/blue' AFTER risk_point_level;

ALTER TABLE emergency_plan
  ADD COLUMN alarm_level VARCHAR(16) DEFAULT NULL COMMENT '适用告警等级 red/orange/yellow/blue' AFTER plan_name;
```

`sql/init-data.sql` target dictionaries:

```sql
(7201, 1, '风险点等级', 'risk_point_level', 'text', 1, 1, '风险点档案等级字典', 1, NOW(), 1, NOW(), 0),
(7202, 1, '告警等级', 'alarm_level', 'text', 1, 2, '告警等级四色字典', 1, NOW(), 1, NOW(), 0),
(7203, 1, '风险态势等级', 'risk_level', 'text', 1, 3, '运行态风险颜色字典', 1, NOW(), 1, NOW(), 0)
```

Backfill rules in `scripts/run-real-env-schema-sync.py`:

```python
# risk_point_level cannot be inferred from legacy severity safely
# keep NULL for legacy rows and require governance completion

# current_risk_level can inherit from legacy risk_level
critical -> red
warning  -> orange
info     -> blue

# alarm_level / emergency_plan.alarm_level use the same mapping
critical -> red
warning  -> orange
info     -> blue
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/utils/alarmLevel.test.ts src/__tests__/utils/riskPointLevel.test.ts
```

Expected: PASS

### Task 2: Refactor Risk Point Archive Level In Backend

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPoint.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImplTest.java`

- [ ] **Step 1: Write the failing backend tests**

Add tests that prove:

```java
@Test
void addRiskPointShouldPersistArchiveRiskPointLevel() { ... }

@Test
void addRiskPointShouldRejectUnknownArchiveLevel() { ... }

@Test
void legacyCurrentRiskLevelShouldNotOverwriteArchiveLevel() { ... }
```

Expected behavior:
- `riskPointLevel` is validated against dict code `risk_point_level`
- `currentRiskLevel` is never taken from the form drawer
- generated `riskPointCode` uses `riskPointLevel`, not runtime `currentRiskLevel`

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false "-Dmaven.test.skip=false" "-Dtest=RiskPointServiceImplTest" test
```

Expected: FAIL because `RiskPoint` and `RiskPointServiceImpl` only know `riskLevel`

- [ ] **Step 3: Write minimal implementation**

Entity shape:

```java
private String riskPointLevel;
private String currentRiskLevel;
```

Service rules:

```java
riskPoint.setRiskPointLevel(normalizeAndValidateRiskPointLevel(riskPoint.getRiskPointLevel()));
if (!StringUtils.hasText(riskPoint.getCurrentRiskLevel())) {
    riskPoint.setCurrentRiskLevel("blue");
}
```

Controller request/response contract:

```java
@RequestParam(required = false) String riskPointLevel
```

Compatibility rule during transition:

```java
// if old payload still sends riskLevel and riskPointLevel is blank, reject with BizException
throw new BizException("风险点档案等级已改为 riskPointLevel，请补录一级/二级/三级");
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false "-Dmaven.test.skip=false" "-Dtest=RiskPointServiceImplTest" test
```

Expected: PASS

### Task 3: Convert Alarm Level To Four Colors Across Rule, Alarm, Event And Emergency Plan

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RuleDefinition.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/AlarmRecord.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/EventRecord.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/EmergencyPlan.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/AutoClosureSeverity.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskPolicyDecision.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskRuntimeLevelResolver.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/EmergencyPlanServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/AutoClosureSeverityTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureServiceTest.java`

- [ ] **Step 1: Write the failing backend tests**

Add tests that prove:

```java
@Test
void normalizeAlarmLevelShouldReturnFourColorValues() { ... }

@Test
void autoClosureShouldPersistRedOrangeYellowBlueAlarmLevels() { ... }

@Test
void emergencyPlanShouldMatchAlarmLevelNotLegacyRiskLevel() { ... }
```

Specific expected mapping:

```java
critical -> red
warning  -> orange
info     -> blue
```

Yellow remains available for newly configured rules and plans; it is not losslessly derivable from legacy `warning`.

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false "-Dmaven.test.skip=false" "-Dtest=RuleDefinitionServiceImplTest,EmergencyPlanServiceImplTest,AutoClosureSeverityTest,DeepDisplacementAutoClosureServiceTest" test
```

Expected: FAIL because current services still normalize to `critical / warning / info`

- [ ] **Step 3: Write minimal implementation**

New shared backend mapping:

```java
red -> red
orange -> orange
yellow -> yellow
blue -> blue
critical -> red
high -> orange
warning -> orange
medium -> yellow
info -> blue
low -> blue
```

Auto closure enum should become:

```java
BLUE("blue", "蓝", "blue", "blue", false, false, 0),
YELLOW("yellow", "黄", "yellow", "yellow", true, false, 1),
ORANGE("orange", "橙", "orange", "orange", true, true, 2),
RED("red", "红", "red", "red", true, true, 3);
```

Emergency plan matching should switch from `plan.getRiskLevel()` to `plan.getAlarmLevel()`.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false "-Dmaven.test.skip=false" "-Dtest=RuleDefinitionServiceImplTest,EmergencyPlanServiceImplTest,AutoClosureSeverityTest,DeepDisplacementAutoClosureServiceTest" test
```

Expected: PASS

### Task 4: Cut Frontend Pages To Archive-Level And Alarm-Level Contracts

**Files:**
- Modify: `spring-boot-iot-ui/src/api/alarm.ts`
- Modify: `spring-boot-iot-ui/src/api/emergencyPlan.ts`
- Modify: `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/views/AlarmCenterView.vue`
- Modify: `spring-boot-iot-ui/src/views/EmergencyPlanView.vue`
- Modify: `spring-boot-iot-ui/src/components/AlarmDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/EventDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/RiskMonitoringDetailDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskGisView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RuleDefinitionView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/EmergencyPlanView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/AlarmDetailDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/EventDetailDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/RiskMonitoringDetailDrawer.test.ts`

- [ ] **Step 1: Write the failing view/component tests**

Add or update tests that expect:

```ts
expect(screen.getByText('一级风险点')).toBeInTheDocument()
expect(screen.getByText('红色')).toBeInTheDocument()
expect(screen.getByText('当前风险态势')).toBeInTheDocument()
expect(screen.getByLabelText('适用告警等级')).toBeInTheDocument()
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RuleDefinitionView.test.ts src/__tests__/views/EmergencyPlanView.test.ts src/__tests__/components/AlarmDetailDrawer.test.ts src/__tests__/components/EventDetailDrawer.test.ts src/__tests__/components/RiskMonitoringDetailDrawer.test.ts
```

Expected: FAIL because pages still use `riskLevel` as the risk point archive label and `alarmLevel` still renders old severity labels

- [ ] **Step 3: Write minimal implementation**

Risk point page contract:

```ts
form.riskPointLevel = ''
const riskPointLevelOptions = await fetchRiskPointLevelOptions()
```

Alarm UI labels:

```ts
getAlarmLevelText('red') === '红色'
getAlarmLevelText('orange') === '橙色'
getAlarmLevelText('yellow') === '黄色'
getAlarmLevelText('blue') === '蓝色'
```

Monitoring/detail labels:

```vue
<el-descriptions-item label="风险点等级">{{ getRiskPointLevelText(detail.riskPointLevel) }}</el-descriptions-item>
<el-descriptions-item label="当前风险态势">{{ getRiskLevelText(detail.currentRiskLevel) }}</el-descriptions-item>
<el-descriptions-item label="告警等级">{{ getAlarmLevelText(detail.alarmLevel) }}</el-descriptions-item>
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RuleDefinitionView.test.ts src/__tests__/views/EmergencyPlanView.test.ts src/__tests__/components/AlarmDetailDrawer.test.ts src/__tests__/components/EventDetailDrawer.test.ts src/__tests__/components/RiskMonitoringDetailDrawer.test.ts
```

Expected: PASS

### Task 5: Sync Authoritative Docs And Run Focused Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md` (only if payload shape changes affect governance API examples)

- [ ] **Step 1: Update docs in place**

Document the settled semantics:

- `riskPointLevel` = 一级/二级/三级风险点
- `alarmLevel` = 红/橙/黄/蓝
- `currentRiskLevel` = 运行态风险态势
- legacy `critical / warning / info` only remain as migration input, not target output

- [ ] **Step 2: Run focused backend verification**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false "-Dmaven.test.skip=false" "-Dtest=RiskPointServiceImplTest,RuleDefinitionServiceImplTest,EmergencyPlanServiceImplTest,AutoClosureSeverityTest,DeepDisplacementAutoClosureServiceTest,AlarmRecordServiceImplTest,EventRecordServiceImplTest,RiskMonitoringServiceImplTest" test
```

Expected: PASS

- [ ] **Step 3: Run focused frontend verification**

Run:

```bash
cd spring-boot-iot-ui
npm test -- src/__tests__/utils/alarmLevel.test.ts src/__tests__/utils/riskPointLevel.test.ts src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RuleDefinitionView.test.ts src/__tests__/views/EmergencyPlanView.test.ts src/__tests__/components/AlarmDetailDrawer.test.ts src/__tests__/components/EventDetailDrawer.test.ts src/__tests__/components/RiskMonitoringDetailDrawer.test.ts
```

Expected: PASS

- [ ] **Step 4: Review diff**

Run:

```bash
git diff -- sql/init.sql sql/init-data.sql scripts/run-real-env-schema-sync.py docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md spring-boot-iot-alarm spring-boot-iot-ui/src
```

Expected:
- archive risk-point grade and alarm four-color changes only
- no unrelated page layout churn
- no silent fallback to legacy `critical / warning / info` display labels
