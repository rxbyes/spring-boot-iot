# Risk Point Pending Binding Promotion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `/risk-point` pending-governance promotion workflow that lets one `risk_point_device_pending_binding` record be reviewed, recommended, and promoted into one or more formal `risk_point_device` bindings without writing placeholder metrics.

**Architecture:** Keep `risk_point_device` as the only runtime source of truth, add `risk_point_device_pending_promotion` as a process/audit table, and build a read/write split inside `spring-boot-iot-alarm`: read-side services page pending rows and merge recommendation evidence from product model, latest properties, and message logs; write-side services lock a single pending row, promote selected metrics transactionally, and write promotion history. Extend `/api/risk-point` under a dedicated pending controller and wire the existing `/risk-point` Vue page with a second drawer for pending promotion.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, Jackson, Vue 3, TypeScript, Element Plus, Vitest, Python `unittest`, Maven, npm

---

## File Map

### Backend schema and persistence

- Modify: `sql/init.sql`
  - Add `risk_point_device_pending_promotion` DDL and indexes.
- Modify: `scripts/run-real-env-schema-sync.py`
  - Add real-environment sync support for the new promotion table.
- Create: `scripts/test_risk_point_pending_promotion_schema.py`
  - Guard that `sql/init.sql` and `scripts/run-real-env-schema-sync.py` both contain the promotion-table baseline.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevicePendingBinding.java`
  - MyBatis-Plus entity for the existing pending table so read/write code stops relying on ad hoc maps.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevicePendingPromotion.java`
  - MyBatis-Plus entity for promotion history rows.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDevicePendingBindingMapper.java`
  - Base mapper plus `selectByIdForUpdate`.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDevicePendingPromotionMapper.java`
  - Base mapper for promotion history rows.

### Backend read-side services

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingBindingQuery.java`
  - Query DTO for per-risk-point pending rows.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingBindingItemVO.java`
  - Page row VO for pending bindings.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingMetricCandidateVO.java`
  - Candidate metric row with score, sources, last-seen data, and reason summary.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionHistoryVO.java`
  - Read model for prior promotion attempts.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingCandidateBundleVO.java`
  - Bundle of pending metadata, candidates, and promotion history for the drawer.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingBindingService.java`
  - Page and lookup behavior for pending rows.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingRecommendationService.java`
  - Candidate recommendation orchestration API.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingBindingServiceImpl.java`
  - Query pending rows by `riskPointId` plus optional `deviceCode/resolutionStatus/batchNo`.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
  - Merge product-model, latest-property, and message-log evidence into a sorted candidate bundle.
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
  - Service-level recommendation/ordering tests.

### Backend write-side services and controllers

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingPromotionMetricDTO.java`
  - Selected metric payload for promotion.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingPromotionRequest.java`
  - Promotion request body containing selected metrics, note, and `completePending`.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingIgnoreRequest.java`
  - Ignore request body containing ignore note.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionItemVO.java`
  - Per-metric result row for `SUCCESS / DUPLICATE_SKIPPED / INVALID_METRIC / BLOCKED`.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionResultVO.java`
  - Aggregate result for one promotion submission.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingPromotionService.java`
  - Write-side API for promote/ignore.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`
  - Transactional implementation with row lock and promotion-history writes.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointPendingController.java`
  - `/api/risk-point/pending-bindings*` endpoints without bloating the existing formal-binding controller.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
  - Add a return-value variant of formal binding for reuse inside promotion.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
  - Implement `bindDeviceAndReturn` while preserving current `bindDevice` behavior.
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
  - Transaction and duplicate-skip tests.
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java`
  - Controller delegation tests for page, candidates, promote, and ignore endpoints.

### Frontend API and page wiring

- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
  - Add pending page/candidate/promote/ignore API contracts and types.
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
  - Add pending-governance drawer, state, row action, evidence display, and submission flow.
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
  - Cover pending drawer visibility, candidate rendering, and promotion submit payload.

### Documentation

- Modify: `docs/02-业务功能与流程说明.md`
  - Describe the pending-governance-to-formal-binding flow.
- Modify: `docs/04-数据库设计与初始化数据.md`
  - Add the new promotion table and update pending-table semantics.
- Modify: `docs/07-部署运行与配置说明.md`
  - Explain schema sync and promotion-table rollout.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Record the new promotion workflow and any first-iteration constraints.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Add `/api/risk-point/pending-bindings*` acceptance and `/risk-point` pending drawer behavior.
- Review: `README.md`, `AGENTS.md`
  - Update only if the new workflow changes repo-wide onboarding or mandatory execution rules.

## Task 1: Add the Promotion Table Baseline and Alarm Persistence Scaffolding

**Files:**
- Create: `scripts/test_risk_point_pending_promotion_schema.py`
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevicePendingBinding.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevicePendingPromotion.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDevicePendingBindingMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDevicePendingPromotionMapper.java`
- Test: `scripts/test_risk_point_pending_promotion_schema.py`

- [ ] **Step 1: Write the failing schema regression test**

```python
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = ROOT / "sql" / "init.sql"
SYNC_PATH = ROOT / "scripts" / "run-real-env-schema-sync.py"


class RiskPointPendingPromotionSchemaTest(unittest.TestCase):

    def test_init_sql_contains_pending_promotion_table(self) -> None:
        sql_text = SQL_PATH.read_text(encoding="utf-8")
        self.assertIn("CREATE TABLE risk_point_device_pending_promotion", sql_text)
        self.assertIn("KEY idx_pending_promotion_pending_id", sql_text)

    def test_schema_sync_contains_pending_promotion_table(self) -> None:
        sync_text = SYNC_PATH.read_text(encoding="utf-8")
        self.assertIn('"risk_point_device_pending_promotion": """', sync_text)
        self.assertIn("CREATE TABLE IF NOT EXISTS risk_point_device_pending_promotion", sync_text)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the schema regression test and confirm it fails**

Run: `python3 -m unittest scripts/test_risk_point_pending_promotion_schema.py -v`

Expected: FAIL because neither `sql/init.sql` nor `scripts/run-real-env-schema-sync.py` contains `risk_point_device_pending_promotion` yet.

- [ ] **Step 3: Add the DDL, sync script entry, entities, and mappers**

```sql
CREATE TABLE risk_point_device_pending_promotion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    pending_binding_id BIGINT NOT NULL COMMENT '来源待治理记录ID',
    risk_point_device_id BIGINT DEFAULT NULL COMMENT '正式绑定ID',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '测点名称',
    promotion_status VARCHAR(32) NOT NULL COMMENT '转正结果',
    recommendation_level VARCHAR(16) DEFAULT NULL COMMENT '推荐等级',
    recommendation_score INT DEFAULT NULL COMMENT '推荐评分',
    evidence_snapshot_json JSON DEFAULT NULL COMMENT '证据快照',
    promotion_note VARCHAR(500) DEFAULT NULL COMMENT '治理说明',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '操作人姓名',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pending_promotion_pending_id (pending_binding_id),
    KEY idx_pending_promotion_binding_id (risk_point_device_id),
    KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备待治理转正明细表';
```

```python
"risk_point_device_pending_promotion": """
CREATE TABLE IF NOT EXISTS risk_point_device_pending_promotion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    pending_binding_id BIGINT NOT NULL COMMENT '来源待治理记录ID',
    risk_point_device_id BIGINT DEFAULT NULL COMMENT '正式绑定ID',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '测点名称',
    promotion_status VARCHAR(32) NOT NULL COMMENT '转正结果',
    recommendation_level VARCHAR(16) DEFAULT NULL COMMENT '推荐等级',
    recommendation_score INT DEFAULT NULL COMMENT '推荐评分',
    evidence_snapshot_json JSON DEFAULT NULL COMMENT '证据快照',
    promotion_note VARCHAR(500) DEFAULT NULL COMMENT '治理说明',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '操作人姓名',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pending_promotion_pending_id (pending_binding_id),
    KEY idx_pending_promotion_binding_id (risk_point_device_id),
    KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备待治理转正明细表';
""",
```

```java
@Data
@TableName("risk_point_device_pending_binding")
public class RiskPointDevicePendingBinding implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String sourceFileName;
    private Integer sourceRowNo;
    private String riskPointName;
    private Long riskPointId;
    private String riskPointCode;
    private String deviceCode;
    private Long deviceId;
    private String deviceName;
    private String resolutionStatus;
    private String resolutionNote;
    private String metricIdentifier;
    private String metricName;
    private Long promotedBindingId;
    private Date promotedTime;
    private Long tenantId;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
    private Integer deleted;
}
```

```java
@Data
@TableName("risk_point_device_pending_promotion")
public class RiskPointDevicePendingPromotion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pendingBindingId;
    private Long riskPointDeviceId;
    private Long riskPointId;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String metricIdentifier;
    private String metricName;
    private String promotionStatus;
    private String recommendationLevel;
    private Integer recommendationScore;
    private String evidenceSnapshotJson;
    private String promotionNote;
    private Long operatorId;
    private String operatorName;
    private Long tenantId;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
    private Integer deleted;
}
```

```java
@Mapper
public interface RiskPointDevicePendingBindingMapper extends BaseMapper<RiskPointDevicePendingBinding> {

    @Select("""
        SELECT *
        FROM risk_point_device_pending_binding
        WHERE id = #{id}
          AND deleted = 0
        FOR UPDATE
        """)
    RiskPointDevicePendingBinding selectByIdForUpdate(@Param("id") Long id);
}
```

```java
@Mapper
public interface RiskPointDevicePendingPromotionMapper extends BaseMapper<RiskPointDevicePendingPromotion> {
}
```

- [ ] **Step 4: Re-run the schema regression test**

Run: `python3 -m unittest scripts/test_risk_point_pending_promotion_schema.py -v`

Expected: PASS with both assertions finding the new promotion-table baseline.

- [ ] **Step 5: Commit the persistence baseline only**

```bash
git add \
  scripts/test_risk_point_pending_promotion_schema.py \
  sql/init.sql \
  scripts/run-real-env-schema-sync.py \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevicePendingBinding.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevicePendingPromotion.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDevicePendingBindingMapper.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDevicePendingPromotionMapper.java
git commit -m "feat: add risk point pending promotion persistence baseline"
```

## Task 2: Implement Pending Read APIs and Real-Time Candidate Recommendation

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingBindingQuery.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingBindingItemVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingMetricCandidateVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionHistoryVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingCandidateBundleVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingBindingService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingRecommendationService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingBindingServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointPendingController.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java`

- [ ] **Step 1: Write the failing recommendation and controller tests**

```java
@Test
void getCandidatesShouldMergeModelLatestPropertyAndMessageLogEvidence() {
    RiskPointDevicePendingBinding pending = new RiskPointDevicePendingBinding();
    pending.setId(99L);
    pending.setRiskPointId(12L);
    pending.setDeviceId(2001L);
    pending.setDeviceCode("DEVICE-2001");
    pending.setResolutionStatus("PENDING_METRIC_GOVERNANCE");

    ProductModel modelMetric = new ProductModel();
    modelMetric.setIdentifier("dispsX");
    modelMetric.setModelName("X轴位移");
    modelMetric.setDataType("double");

    DeviceProperty latestMetric = new DeviceProperty();
    latestMetric.setIdentifier("dispsX");
    latestMetric.setPropertyName("X轴位移");
    latestMetric.setValueType("double");

    DeviceMessageLog messageLog = new DeviceMessageLog();
    messageLog.setPayload("{\"dispsX\":25.6,\"batteryVoltage\":3.7}");
    messageLog.setReportTime(LocalDateTime.of(2026, 4, 3, 11, 40, 0));

    RiskPoint riskPoint = new RiskPoint();
    riskPoint.setId(12L);
    riskPoint.setRiskPointCode("RP-OPS-001");
    riskPoint.setRiskPointName("一号风险点");

    when(pendingBindingMapper.selectById(99L)).thenReturn(pending);
    when(productModelMapper.selectList(any())).thenReturn(List.of(modelMetric));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of(latestMetric));
    when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(messageLog));
    doReturn(riskPoint).when(riskPointService).getById(12L, 1001L);

    RiskPointPendingCandidateBundleVO bundle = service.getCandidates(99L, 1001L);

    assertEquals(2, bundle.getCandidates().size());
    assertEquals("dispsX", bundle.getCandidates().get(0).getMetricIdentifier());
    assertEquals("HIGH", bundle.getCandidates().get(0).getRecommendationLevel());
    assertTrue(bundle.getCandidates().get(0).getEvidenceSources().contains("MODEL"));
    assertTrue(bundle.getCandidates().get(0).getEvidenceSources().contains("LATEST_PROPERTY"));
    assertTrue(bundle.getCandidates().get(0).getEvidenceSources().contains("MESSAGE_LOG"));
}
```

```java
@Test
void pagePendingBindingsShouldDelegateToService() {
    RiskPointPendingBindingService bindingService = mock(RiskPointPendingBindingService.class);
    RiskPointPendingRecommendationService recommendationService = mock(RiskPointPendingRecommendationService.class);
    RiskPointPendingPromotionService promotionService = mock(RiskPointPendingPromotionService.class);
    RiskPointPendingController controller = new RiskPointPendingController(bindingService, recommendationService, promotionService);

    RiskPointPendingBindingQuery query = new RiskPointPendingBindingQuery();
    query.setRiskPointId(12L);
    PageResult<RiskPointPendingBindingItemVO> page = PageResult.of(1L, 1L, 10L, List.of(new RiskPointPendingBindingItemVO()));
    when(bindingService.pagePendingBindings(query, 1001L)).thenReturn(page);

    Authentication authentication = new UsernamePasswordAuthenticationToken(new JwtUserPrincipal(1001L, "tester"), null, List.of());

    R<PageResult<RiskPointPendingBindingItemVO>> response = controller.pagePendingBindings(query, authentication);

    assertEquals(1L, response.getData().getTotal());
}
```

- [ ] **Step 2: Run the new backend tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingRecommendationServiceImplTest,RiskPointPendingControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because the DTOs, controller, and recommendation services do not exist yet.

- [ ] **Step 3: Implement pending page/candidate DTOs, services, and controller**

```java
@Data
public class RiskPointPendingBindingQuery {

    @NotNull(message = "riskPointId不能为空")
    private Long riskPointId;
    private String deviceCode;
    private String resolutionStatus;
    private String batchNo;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
```

```java
@Data
public class RiskPointPendingMetricCandidateVO {

    private String metricIdentifier;
    private String metricName;
    private String dataType;
    private List<String> evidenceSources;
    private LocalDateTime lastSeenTime;
    private String sampleValue;
    private Integer seenCount;
    private Integer recommendationScore;
    private String recommendationLevel;
    private String reasonSummary;
}
```

```java
public interface RiskPointPendingBindingService {

    PageResult<RiskPointPendingBindingItemVO> pagePendingBindings(RiskPointPendingBindingQuery query, Long currentUserId);

    RiskPointDevicePendingBinding getRequiredPending(Long pendingId, Long currentUserId);
}
```

```java
@Service
public class RiskPointPendingBindingServiceImpl implements RiskPointPendingBindingService {

    @Override
    public PageResult<RiskPointPendingBindingItemVO> pagePendingBindings(RiskPointPendingBindingQuery query, Long currentUserId) {
        riskPointService.getById(query.getRiskPointId(), currentUserId);
        LambdaQueryWrapper<RiskPointDevicePendingBinding> wrapper = new LambdaQueryWrapper<RiskPointDevicePendingBinding>()
                .eq(RiskPointDevicePendingBinding::getDeleted, 0)
                .eq(RiskPointDevicePendingBinding::getRiskPointId, query.getRiskPointId())
                .like(StringUtils.hasText(query.getDeviceCode()), RiskPointDevicePendingBinding::getDeviceCode, query.getDeviceCode().trim())
                .eq(StringUtils.hasText(query.getResolutionStatus()), RiskPointDevicePendingBinding::getResolutionStatus, query.getResolutionStatus().trim())
                .eq(StringUtils.hasText(query.getBatchNo()), RiskPointDevicePendingBinding::getBatchNo, query.getBatchNo().trim())
                .orderByDesc(RiskPointDevicePendingBinding::getCreateTime)
                .orderByAsc(RiskPointDevicePendingBinding::getSourceRowNo);
        Page<RiskPointDevicePendingBinding> page = pendingBindingMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), page.getRecords().stream().map(this::toItem).toList());
    }
}
```

```java
@Service
public class RiskPointPendingRecommendationServiceImpl implements RiskPointPendingRecommendationService {

    @Override
    public RiskPointPendingCandidateBundleVO getCandidates(Long pendingId, Long currentUserId) {
        RiskPointDevicePendingBinding pending = pendingBindingService.getRequiredPending(pendingId, currentUserId);
        ensurePromotable(pending);

        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = new LinkedHashMap<>();
        mergeProductModelEvidence(pending, candidateMap);
        mergeLatestPropertyEvidence(pending, candidateMap);
        mergeMessageLogEvidence(pending, candidateMap);

        List<RiskPointPendingMetricCandidateVO> candidates = candidateMap.values().stream()
                .peek(this::applyRecommendationMetadata)
                .sorted(Comparator
                        .comparing(this::recommendationLevelWeight).reversed()
                        .thenComparing(RiskPointPendingMetricCandidateVO::getLastSeenTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RiskPointPendingMetricCandidateVO::getSeenCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RiskPointPendingMetricCandidateVO::getMetricIdentifier))
                .toList();

        RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
        bundle.setPending(toPendingItem(pending));
        bundle.setCandidates(candidates);
        bundle.setHistory(listPromotionHistory(pendingId));
        return bundle;
    }

    private void ensurePromotable(RiskPointDevicePendingBinding pending) {
        if (pending == null || pending.getDeleted() != 0) {
            throw new BizException("待治理记录不存在");
        }
        if (!StringUtils.hasText(pending.getResolutionStatus())
                || (!"PENDING_METRIC_GOVERNANCE".equals(pending.getResolutionStatus())
                && !"PARTIALLY_PROMOTED".equals(pending.getResolutionStatus()))) {
            throw new BizException("当前治理状态不允许查看候选测点");
        }
        if (pending.getRiskPointId() == null || pending.getDeviceId() == null) {
            throw new BizException("待治理记录缺少风险点或设备，禁止转正");
        }
    }

    private void mergeProductModelEvidence(RiskPointDevicePendingBinding pending,
                                           Map<String, RiskPointPendingMetricCandidateVO> candidateMap) {
        List<ProductModel> productModels = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getProductId, deviceService.getRequiredById(pending.getDeviceId()).getProductId())
                .eq(ProductModel::getModelType, "property")
                .eq(ProductModel::getDeleted, 0)
                .orderByAsc(ProductModel::getSortNo)
                .orderByAsc(ProductModel::getIdentifier));
        for (ProductModel productModel : productModels) {
            RiskPointPendingMetricCandidateVO candidate = candidateMap.computeIfAbsent(productModel.getIdentifier(), key -> new RiskPointPendingMetricCandidateVO());
            candidate.setMetricIdentifier(productModel.getIdentifier());
            candidate.setMetricName(StringUtils.hasText(productModel.getModelName()) ? productModel.getModelName() : productModel.getIdentifier());
            candidate.setDataType(productModel.getDataType());
            candidate.setEvidenceSources(mergeEvidenceSources(candidate.getEvidenceSources(), "MODEL"));
        }
    }

    private void mergeLatestPropertyEvidence(RiskPointDevicePendingBinding pending,
                                             Map<String, RiskPointPendingMetricCandidateVO> candidateMap) {
        List<DeviceProperty> deviceProperties = devicePropertyMapper.selectList(new LambdaQueryWrapper<DeviceProperty>()
                .eq(DeviceProperty::getDeviceId, pending.getDeviceId())
                .orderByDesc(DeviceProperty::getUpdateTime));
        for (DeviceProperty property : deviceProperties) {
            RiskPointPendingMetricCandidateVO candidate = candidateMap.computeIfAbsent(property.getIdentifier(), key -> new RiskPointPendingMetricCandidateVO());
            candidate.setMetricIdentifier(property.getIdentifier());
            candidate.setMetricName(StringUtils.hasText(property.getPropertyName()) ? property.getPropertyName() : property.getIdentifier());
            candidate.setDataType(property.getValueType());
            candidate.setEvidenceSources(mergeEvidenceSources(candidate.getEvidenceSources(), "LATEST_PROPERTY"));
            candidate.setLastSeenTime(property.getUpdateTime());
            candidate.setSampleValue(property.getPropertyValue());
            candidate.setSeenCount(candidate.getSeenCount() == null ? 1 : candidate.getSeenCount() + 1);
        }
    }

    private void mergeMessageLogEvidence(RiskPointDevicePendingBinding pending,
                                         Map<String, RiskPointPendingMetricCandidateVO> candidateMap) {
        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .eq(DeviceMessageLog::getDeviceId, pending.getDeviceId())
                .orderByDesc(DeviceMessageLog::getReportTime)
                .last("LIMIT 20"));
        for (DeviceMessageLog log : logs) {
            Map<String, Object> payload = readPayload(log.getPayload());
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                RiskPointPendingMetricCandidateVO candidate = candidateMap.computeIfAbsent(entry.getKey(), key -> new RiskPointPendingMetricCandidateVO());
                candidate.setMetricIdentifier(entry.getKey());
                candidate.setMetricName(StringUtils.hasText(candidate.getMetricName()) ? candidate.getMetricName() : entry.getKey());
                candidate.setEvidenceSources(mergeEvidenceSources(candidate.getEvidenceSources(), "MESSAGE_LOG"));
                candidate.setLastSeenTime(log.getReportTime());
                candidate.setSampleValue(String.valueOf(entry.getValue()));
                candidate.setSeenCount(candidate.getSeenCount() == null ? 1 : candidate.getSeenCount() + 1);
            }
        }
    }

    private void applyRecommendationMetadata(RiskPointPendingMetricCandidateVO candidate) {
        int score = 0;
        if (candidate.getEvidenceSources().contains("MODEL")) {
            score += 40;
        }
        if (candidate.getEvidenceSources().contains("LATEST_PROPERTY")) {
            score += 40;
        }
        if (candidate.getEvidenceSources().contains("MESSAGE_LOG")) {
            score += 20;
        }
        candidate.setRecommendationScore(score);
        if (score >= 80) {
            candidate.setRecommendationLevel("HIGH");
            candidate.setReasonSummary("同时命中物模型与真实上报证据");
        } else if (score >= 40) {
            candidate.setRecommendationLevel("MEDIUM");
            candidate.setReasonSummary("命中单路强证据或多路弱证据");
        } else {
            candidate.setRecommendationLevel("LOW");
            candidate.setReasonSummary("仅命中历史日志弱证据");
        }
    }

    private int recommendationLevelWeight(RiskPointPendingMetricCandidateVO candidate) {
        return switch (candidate.getRecommendationLevel()) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private List<String> mergeEvidenceSources(List<String> current, String source) {
        LinkedHashSet<String> sources = current == null ? new LinkedHashSet<>() : new LinkedHashSet<>(current);
        sources.add(source);
        return new ArrayList<>(sources);
    }

    private Map<String, Object> readPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!root.isObject()) {
                return Map.of();
            }
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            Map<String, Object> values = new LinkedHashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (value.isNumber()) {
                    values.put(entry.getKey(), value.numberValue());
                } else if (value.isTextual()) {
                    values.put(entry.getKey(), value.textValue());
                } else if (value.isBoolean()) {
                    values.put(entry.getKey(), value.booleanValue());
                }
            }
            return values;
        } catch (IOException ex) {
            return Map.of();
        }
    }

    private RiskPointPendingBindingItemVO toPendingItem(RiskPointDevicePendingBinding pending) {
        RiskPointPendingBindingItemVO item = new RiskPointPendingBindingItemVO();
        item.setId(pending.getId());
        item.setRiskPointId(pending.getRiskPointId());
        item.setRiskPointCode(pending.getRiskPointCode());
        item.setRiskPointName(pending.getRiskPointName());
        item.setDeviceId(pending.getDeviceId());
        item.setDeviceCode(pending.getDeviceCode());
        item.setDeviceName(pending.getDeviceName());
        item.setResolutionStatus(pending.getResolutionStatus());
        item.setResolutionNote(pending.getResolutionNote());
        item.setBatchNo(pending.getBatchNo());
        item.setSourceRowNo(pending.getSourceRowNo());
        item.setPromotedBindingId(pending.getPromotedBindingId());
        item.setPromotedTime(pending.getPromotedTime() == null ? null : pending.getPromotedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        return item;
    }

    private List<RiskPointPendingPromotionHistoryVO> listPromotionHistory(Long pendingId) {
        return promotionMapper.selectList(new LambdaQueryWrapper<RiskPointDevicePendingPromotion>()
                        .eq(RiskPointDevicePendingPromotion::getPendingBindingId, pendingId)
                        .eq(RiskPointDevicePendingPromotion::getDeleted, 0)
                        .orderByDesc(RiskPointDevicePendingPromotion::getCreateTime))
                .stream()
                .map(row -> {
                    RiskPointPendingPromotionHistoryVO history = new RiskPointPendingPromotionHistoryVO();
                    history.setMetricIdentifier(row.getMetricIdentifier());
                    history.setMetricName(row.getMetricName());
                    history.setPromotionStatus(row.getPromotionStatus());
                    history.setRiskPointDeviceId(row.getRiskPointDeviceId());
                    history.setOperatorId(row.getOperatorId());
                    history.setOperatorName(row.getOperatorName());
                    history.setCreateTime(row.getCreateTime() == null ? null : row.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    return history;
                })
                .toList();
    }
}
```

```java
@RestController
@RequestMapping("/api/risk-point")
public class RiskPointPendingController {

    @GetMapping("/pending-bindings")
    public R<PageResult<RiskPointPendingBindingItemVO>> pagePendingBindings(RiskPointPendingBindingQuery query, Authentication authentication) {
        return R.ok(bindingService.pagePendingBindings(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/pending-bindings/{pendingId}/candidates")
    public R<RiskPointPendingCandidateBundleVO> getCandidates(@PathVariable Long pendingId, Authentication authentication) {
        return R.ok(recommendationService.getCandidates(pendingId, requireCurrentUserId(authentication)));
    }
}
```

- [ ] **Step 4: Re-run the targeted backend tests**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingRecommendationServiceImplTest,RiskPointPendingControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS with controller delegation and candidate-evidence merging verified.

- [ ] **Step 5: Commit the read-side work**

```bash
git add \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingBindingQuery.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingBindingItemVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingMetricCandidateVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionHistoryVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingCandidateBundleVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingBindingService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingRecommendationService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingBindingServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointPendingController.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java
git commit -m "feat: add risk point pending read APIs"
```

## Task 3: Implement Transactional Promotion, Ignore, and Reusable Formal Binding

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingPromotionMetricDTO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingPromotionRequest.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingIgnoreRequest.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionItemVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionResultVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingPromotionService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointPendingController.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java`

- [ ] **Step 1: Write the failing promotion tests**

```java
@Test
void promoteShouldCreateMultipleFormalBindingsAndWritePromotionHistory() {
    RiskPointDevicePendingBinding pending = new RiskPointDevicePendingBinding();
    pending.setId(77L);
    pending.setRiskPointId(12L);
    pending.setDeviceId(2001L);
    pending.setDeviceCode("DEVICE-2001");
    pending.setDeviceName("一号设备");
    pending.setResolutionStatus("PENDING_METRIC_GOVERNANCE");

    RiskPoint riskPoint = new RiskPoint();
    riskPoint.setId(12L);
    riskPoint.setRiskPointCode("RP-OPS-001");
    riskPoint.setRiskPointName("一号风险点");

    RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
    RiskPointPendingMetricCandidateVO dispsX = new RiskPointPendingMetricCandidateVO();
    dispsX.setMetricIdentifier("dispsX");
    dispsX.setMetricName("X轴位移");
    dispsX.setRecommendationLevel("HIGH");
    dispsX.setRecommendationScore(100);
    RiskPointPendingMetricCandidateVO batteryVoltage = new RiskPointPendingMetricCandidateVO();
    batteryVoltage.setMetricIdentifier("batteryVoltage");
    batteryVoltage.setMetricName("电池电压");
    batteryVoltage.setRecommendationLevel("MEDIUM");
    batteryVoltage.setRecommendationScore(60);
    bundle.setCandidates(List.of(dispsX, batteryVoltage));

    when(pendingBindingMapper.selectByIdForUpdate(77L)).thenReturn(pending);
    doReturn(riskPoint).when(riskPointService).getById(12L, 1001L);
    when(recommendationService.getCandidates(77L, 1001L)).thenReturn(bundle);
    when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
    when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), eq(1001L))).thenAnswer(inv -> {
        RiskPointDevice binding = inv.getArgument(0);
        binding.setId("dispsX".equals(binding.getMetricIdentifier()) ? 9001L : 9002L);
        return binding;
    });

    RiskPointPendingPromotionRequest request = new RiskPointPendingPromotionRequest();
    request.setCompletePending(true);
    RiskPointPendingPromotionMetricDTO firstMetric = new RiskPointPendingPromotionMetricDTO();
    firstMetric.setMetricIdentifier("dispsX");
    firstMetric.setMetricName("X轴位移");
    RiskPointPendingPromotionMetricDTO secondMetric = new RiskPointPendingPromotionMetricDTO();
    secondMetric.setMetricIdentifier("batteryVoltage");
    secondMetric.setMetricName("电池电压");
    request.setMetrics(List.of(firstMetric, secondMetric));

    RiskPointPendingPromotionResultVO result = service.promote(77L, request, 1001L);

    assertEquals("PROMOTED", result.getPendingStatus());
    assertEquals(2, result.getItems().size());
    assertEquals("SUCCESS", result.getItems().get(0).getPromotionStatus());
    verify(promotionMapper, times(2)).insert(any(RiskPointDevicePendingPromotion.class));
    verify(pendingBindingMapper).updateById(argThat(row ->
            "PROMOTED".equals(row.getResolutionStatus()) && Long.valueOf(9002L).equals(row.getPromotedBindingId())));
}
```

```java
@Test
void promoteShouldMarkDuplicateSkippedAndReuseExistingFormalBindingId() {
    RiskPointDevice existing = new RiskPointDevice();
    existing.setId(9010L);
    existing.setRiskPointId(12L);
    existing.setDeviceId(2001L);
    existing.setMetricIdentifier("dispsX");

    RiskPointDevicePendingBinding pending = new RiskPointDevicePendingBinding();
    pending.setId(77L);
    pending.setRiskPointId(12L);
    pending.setDeviceId(2001L);
    pending.setDeviceCode("DEVICE-2001");
    pending.setDeviceName("一号设备");
    pending.setResolutionStatus("PENDING_METRIC_GOVERNANCE");

    RiskPoint riskPoint = new RiskPoint();
    riskPoint.setId(12L);
    riskPoint.setRiskPointCode("RP-OPS-001");
    riskPoint.setRiskPointName("一号风险点");

    RiskPointPendingMetricCandidateVO candidate = new RiskPointPendingMetricCandidateVO();
    candidate.setMetricIdentifier("dispsX");
    candidate.setMetricName("X轴位移");
    candidate.setRecommendationLevel("HIGH");
    candidate.setRecommendationScore(100);
    RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
    bundle.setCandidates(List.of(candidate));

    when(pendingBindingMapper.selectByIdForUpdate(77L)).thenReturn(pending);
    doReturn(riskPoint).when(riskPointService).getById(12L, 1001L);
    when(recommendationService.getCandidates(77L, 1001L)).thenReturn(bundle);
    when(riskPointDeviceMapper.selectOne(any())).thenReturn(existing);

    RiskPointPendingPromotionRequest request = new RiskPointPendingPromotionRequest();
    RiskPointPendingPromotionMetricDTO metric = new RiskPointPendingPromotionMetricDTO();
    metric.setMetricIdentifier("dispsX");
    metric.setMetricName("X轴位移");
    request.setMetrics(List.of(metric));

    RiskPointPendingPromotionResultVO result = service.promote(77L, request, 1001L);

    assertEquals("DUPLICATE_SKIPPED", result.getItems().get(0).getPromotionStatus());
    verify(riskPointService, never()).bindDeviceAndReturn(any(), any());
    verify(promotionMapper).insert(argThat(row -> Long.valueOf(9010L).equals(row.getRiskPointDeviceId())));
}
```

- [ ] **Step 2: Run the write-side tests and confirm they fail**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingPromotionServiceImplTest,RiskPointPendingControllerTest,RiskPointServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because the promotion DTOs, service, and `bindDeviceAndReturn` do not exist yet.

- [ ] **Step 3: Implement the transactional promotion path and ignore endpoint**

```java
public interface RiskPointService extends IService<RiskPoint> {

    void bindDevice(RiskPointDevice riskPointDevice, Long currentUserId);

    RiskPointDevice bindDeviceAndReturn(RiskPointDevice riskPointDevice, Long currentUserId);
}
```

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void bindDevice(RiskPointDevice riskPointDevice, Long currentUserId) {
    bindDeviceAndReturn(riskPointDevice, currentUserId);
}

@Override
@Transactional(rollbackFor = Exception.class)
public RiskPointDevice bindDeviceAndReturn(RiskPointDevice riskPointDevice, Long currentUserId) {
    if (riskPointDevice == null || riskPointDevice.getRiskPointId() == null) {
        throw new BizException("风险点不存在");
    }
    if (riskPointDevice.getDeviceId() == null) {
        throw new BizException("请选择设备");
    }
    if (!StringUtils.hasText(riskPointDevice.getMetricIdentifier())) {
        throw new BizException("请选择测点");
    }
    if (hasDataPermissionSupport() && currentUserId != null) {
        getById(riskPointDevice.getRiskPointId(), currentUserId);
    } else {
        getById(riskPointDevice.getRiskPointId());
    }
    LambdaQueryWrapper<RiskPointDevice> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(RiskPointDevice::getRiskPointId, riskPointDevice.getRiskPointId());
    queryWrapper.eq(RiskPointDevice::getDeviceId, riskPointDevice.getDeviceId());
    queryWrapper.eq(RiskPointDevice::getMetricIdentifier, riskPointDevice.getMetricIdentifier());
    queryWrapper.eq(RiskPointDevice::getDeleted, 0);
    RiskPointDevice existing = riskPointDeviceMapper.selectOne(queryWrapper);
    if (existing != null) {
        throw new BizException("设备已绑定到该风险点");
    }
    riskPointDevice.setCreateTime(new Date());
    riskPointDevice.setUpdateTime(new Date());
    riskPointDevice.setDeleted(0);
    riskPointDeviceMapper.insert(riskPointDevice);
    return riskPointDevice;
}
```

```java
@Data
public class RiskPointPendingPromotionRequest {

    @NotEmpty(message = "至少选择一个测点")
    private List<RiskPointPendingPromotionMetricDTO> metrics;
    private Boolean completePending = Boolean.FALSE;
    private String promotionNote;
}
```

```java
@Service
public class RiskPointPendingPromotionServiceImpl implements RiskPointPendingPromotionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskPointPendingPromotionResultVO promote(Long pendingId, RiskPointPendingPromotionRequest request, Long currentUserId) {
        RiskPointDevicePendingBinding pending = pendingBindingMapper.selectByIdForUpdate(pendingId);
        if (pending == null || pending.getDeleted() != 0) {
            throw new BizException("待治理记录不存在");
        }
        if (!"PENDING_METRIC_GOVERNANCE".equals(pending.getResolutionStatus())
                && !"PARTIALLY_PROMOTED".equals(pending.getResolutionStatus())) {
            throw new BizException("当前治理状态不允许转正");
        }
        if (pending.getRiskPointId() == null || pending.getDeviceId() == null) {
            throw new BizException("待治理记录缺少风险点或设备，禁止转正");
        }
        riskPointService.getById(pending.getRiskPointId(), currentUserId);

        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = recommendationService.getCandidates(pendingId, currentUserId)
                .getCandidates()
                .stream()
                .collect(Collectors.toMap(RiskPointPendingMetricCandidateVO::getMetricIdentifier, Function.identity()));

        List<RiskPointPendingPromotionItemVO> items = new ArrayList<>();
        Long latestBindingId = pending.getPromotedBindingId();
        Date latestPromotionTime = pending.getPromotedTime();

        for (RiskPointPendingPromotionMetricDTO metric : request.getMetrics()) {
            RiskPointPendingMetricCandidateVO candidate = candidateMap.get(metric.getMetricIdentifier());
            if (candidate == null) {
                items.add(insertHistory(pending, metric, null, "INVALID_METRIC", null, currentUserId, request.getPromotionNote()));
                continue;
            }

            RiskPointDevice existing = riskPointDeviceMapper.selectOne(new LambdaQueryWrapper<RiskPointDevice>()
                    .eq(RiskPointDevice::getRiskPointId, pending.getRiskPointId())
                    .eq(RiskPointDevice::getDeviceId, pending.getDeviceId())
                    .eq(RiskPointDevice::getMetricIdentifier, metric.getMetricIdentifier())
                    .eq(RiskPointDevice::getDeleted, 0));
            if (existing != null) {
                items.add(insertHistory(pending, metric, candidate, "DUPLICATE_SKIPPED", existing.getId(), currentUserId, request.getPromotionNote()));
                latestBindingId = existing.getId();
                latestPromotionTime = new Date();
                continue;
            }

            RiskPointDevice binding = new RiskPointDevice();
            binding.setRiskPointId(pending.getRiskPointId());
            binding.setDeviceId(pending.getDeviceId());
            binding.setDeviceCode(pending.getDeviceCode());
            binding.setDeviceName(pending.getDeviceName());
            binding.setMetricIdentifier(metric.getMetricIdentifier());
            binding.setMetricName(metric.getMetricName());
            RiskPointDevice saved = riskPointService.bindDeviceAndReturn(binding, currentUserId);
            items.add(insertHistory(pending, metric, candidate, "SUCCESS", saved.getId(), currentUserId, request.getPromotionNote()));
            latestBindingId = saved.getId();
            latestPromotionTime = new Date();
        }

        boolean hasSuccessLikeResult = items.stream().anyMatch(item ->
                "SUCCESS".equals(item.getPromotionStatus()) || "DUPLICATE_SKIPPED".equals(item.getPromotionStatus()));
        String nextStatus = Boolean.TRUE.equals(request.getCompletePending()) && hasSuccessLikeResult
                ? "PROMOTED"
                : hasSuccessLikeResult ? "PARTIALLY_PROMOTED" : pending.getResolutionStatus();
        pending.setResolutionStatus(nextStatus);
        pending.setResolutionNote(request.getPromotionNote());
        pending.setPromotedBindingId(latestBindingId);
        pending.setPromotedTime(latestPromotionTime);
        pending.setUpdateBy(currentUserId);
        pendingBindingMapper.updateById(pending);

        RiskPointPendingPromotionResultVO result = new RiskPointPendingPromotionResultVO();
        result.setPendingId(pending.getId());
        result.setPendingStatus(nextStatus);
        result.setItems(items);
        return result;
    }

    private RiskPointPendingPromotionItemVO insertHistory(RiskPointDevicePendingBinding pending,
                                                          RiskPointPendingPromotionMetricDTO metric,
                                                          RiskPointPendingMetricCandidateVO candidate,
                                                          String status,
                                                          Long bindingId,
                                                          Long currentUserId,
                                                          String note) {
        RiskPointDevicePendingPromotion row = new RiskPointDevicePendingPromotion();
        row.setPendingBindingId(pending.getId());
        row.setRiskPointDeviceId(bindingId);
        row.setRiskPointId(pending.getRiskPointId());
        row.setDeviceId(pending.getDeviceId());
        row.setDeviceCode(pending.getDeviceCode());
        row.setDeviceName(pending.getDeviceName());
        row.setMetricIdentifier(metric.getMetricIdentifier());
        row.setMetricName(metric.getMetricName());
        row.setPromotionStatus(status);
        row.setRecommendationLevel(candidate == null ? null : candidate.getRecommendationLevel());
        row.setRecommendationScore(candidate == null ? null : candidate.getRecommendationScore());
        row.setEvidenceSnapshotJson(candidate == null ? null : writeEvidenceJson(candidate));
        row.setPromotionNote(note);
        row.setOperatorId(currentUserId);
        row.setCreateBy(currentUserId);
        row.setUpdateBy(currentUserId);
        promotionMapper.insert(row);

        RiskPointPendingPromotionItemVO item = new RiskPointPendingPromotionItemVO();
        item.setMetricIdentifier(metric.getMetricIdentifier());
        item.setMetricName(metric.getMetricName());
        item.setPromotionStatus(status);
        item.setBindingId(bindingId);
        return item;
    }

    private String writeEvidenceJson(RiskPointPendingMetricCandidateVO candidate) {
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("metricIdentifier", candidate.getMetricIdentifier());
        snapshot.put("metricName", candidate.getMetricName());
        snapshot.put("recommendationLevel", candidate.getRecommendationLevel());
        snapshot.put("recommendationScore", candidate.getRecommendationScore());
        ArrayNode sources = snapshot.putArray("evidenceSources");
        for (String source : candidate.getEvidenceSources()) {
            sources.add(source);
        }
        if (candidate.getLastSeenTime() != null) {
            snapshot.put("lastSeenTime", candidate.getLastSeenTime().toString());
        }
        if (candidate.getSampleValue() != null) {
            snapshot.put("sampleValue", candidate.getSampleValue());
        }
        if (candidate.getSeenCount() != null) {
            snapshot.put("seenCount", candidate.getSeenCount());
        }
        return snapshot.toString();
    }
}
```

```java
@PostMapping("/pending-bindings/{pendingId}/promote")
public R<RiskPointPendingPromotionResultVO> promote(@PathVariable Long pendingId,
                                                    @RequestBody RiskPointPendingPromotionRequest request,
                                                    Authentication authentication) {
    return R.ok(promotionService.promote(pendingId, request, requireCurrentUserId(authentication)));
}

@PostMapping("/pending-bindings/{pendingId}/ignore")
public R<Void> ignore(@PathVariable Long pendingId,
                      @RequestBody RiskPointPendingIgnoreRequest request,
                      Authentication authentication) {
    promotionService.ignore(pendingId, request, requireCurrentUserId(authentication));
    return R.ok();
}
```

- [ ] **Step 4: Re-run the write-side backend tests**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingPromotionServiceImplTest,RiskPointPendingControllerTest,RiskPointServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS with multi-metric promotion, duplicate skip, and controller delegation covered.

- [ ] **Step 5: Commit the write-side workflow**

```bash
git add \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingPromotionMetricDTO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingPromotionRequest.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointPendingIgnoreRequest.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionItemVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointPendingPromotionResultVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointPendingPromotionService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointPendingController.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointPendingControllerTest.java
git commit -m "feat: add risk point pending promotion workflow"
```

## Task 4: Wire the `/risk-point` Vue Page and API Client

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Extend the failing Vue test with pending-drawer behavior**

```ts
it('loads pending bindings for the selected risk point and renders candidate evidence', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: { total: 1, pageNum: 1, pageSize: 10, records: [createRiskPointRow()] }
  })
  mockListPendingBindings.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [{ id: 77, riskPointId: 1, deviceCode: 'DEVICE-2001', deviceName: '一号设备', resolutionStatus: 'PENDING_METRIC_GOVERNANCE' }]
    }
  })
  mockGetPendingCandidates.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      pending: { id: 77, riskPointId: 1, deviceCode: 'DEVICE-2001', deviceName: '一号设备', resolutionStatus: 'PENDING_METRIC_GOVERNANCE' },
      candidates: [
        { metricIdentifier: 'dispsX', metricName: 'X轴位移', recommendationLevel: 'HIGH', evidenceSources: ['MODEL', 'LATEST_PROPERTY', 'MESSAGE_LOG'] }
      ],
      history: []
    }
  })

  const wrapper = mountView()
  await flushPromises()
  await (wrapper.vm as any).handleOpenPendingPromotion(createRiskPointRow())
  await flushPromises()

  expect(mockListPendingBindings).toHaveBeenCalledWith({ riskPointId: 1, pageNum: 1, pageSize: 10 })
  expect(wrapper.text()).toContain('待治理转正')
  expect(wrapper.text()).toContain('X轴位移')
  expect(wrapper.text()).toContain('MODEL')
})
```

```ts
it('submits the selected pending metrics through the promote API', async () => {
  mockPromotePendingBinding.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      pendingId: 77,
      pendingStatus: 'PROMOTED',
      items: [{ metricIdentifier: 'dispsX', promotionStatus: 'SUCCESS', bindingId: 9001 }]
    }
  })

  const wrapper = mountView()
  ;(wrapper.vm as any).pendingPromotionForm.pendingId = 77
  ;(wrapper.vm as any).pendingPromotionForm.selectedMetrics = [{ metricIdentifier: 'dispsX', metricName: 'X轴位移' }]
  ;(wrapper.vm as any).pendingPromotionForm.completePending = true

  await (wrapper.vm as any).handlePendingPromotionSubmit()

  expect(mockPromotePendingBinding).toHaveBeenCalledWith(77, {
    metrics: [{ metricIdentifier: 'dispsX', metricName: 'X轴位移' }],
    completePending: true,
    promotionNote: ''
  })
})
```

- [ ] **Step 2: Run the frontend test and confirm it fails**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts`

Expected: FAIL because the pending API methods, mocks, state, and handlers do not exist yet.

- [ ] **Step 3: Add API types and the pending promotion drawer**

```ts
export interface RiskPointPendingBindingItem {
  id: IdType;
  riskPointId: IdType;
  riskPointCode?: string | null;
  riskPointName?: string | null;
  deviceId?: IdType | null;
  deviceCode: string;
  deviceName?: string | null;
  resolutionStatus: string;
  resolutionNote?: string | null;
  batchNo?: string | null;
  sourceRowNo?: number | null;
  promotedBindingId?: IdType | null;
  promotedTime?: string | null;
}

export interface RiskPointPendingMetricCandidate {
  metricIdentifier: string;
  metricName: string;
  dataType?: string | null;
  evidenceSources: string[];
  lastSeenTime?: string | null;
  sampleValue?: string | null;
  seenCount?: number | null;
  recommendationScore?: number | null;
  recommendationLevel?: string | null;
  reasonSummary?: string | null;
}

export function listPendingBindings(params: { riskPointId: IdType; deviceCode?: string; resolutionStatus?: string; batchNo?: string; pageNum?: number; pageSize?: number }) {
  const queryString = buildQueryString(params)
  return request<PageResult<RiskPointPendingBindingItem>>(`/api/risk-point/pending-bindings?${queryString}`, { method: 'GET' })
}

export function getPendingBindingCandidates(pendingId: IdType) {
  return request<RiskPointPendingCandidateBundle>(`/api/risk-point/pending-bindings/${pendingId}/candidates`, { method: 'GET' })
}

export function promotePendingBinding(pendingId: IdType, body: RiskPointPendingPromotionRequest) {
  return request<RiskPointPendingPromotionResult>(`/api/risk-point/pending-bindings/${pendingId}/promote`, { method: 'POST', body })
}

export function ignorePendingBinding(pendingId: IdType, body: { ignoreNote?: string }) {
  return request<void>(`/api/risk-point/pending-bindings/${pendingId}/ignore`, { method: 'POST', body })
}
```

```ts
const pendingPromotionVisible = ref(false)
const pendingBindings = ref<RiskPointPendingBindingItem[]>([])
const pendingCandidates = ref<RiskPointPendingMetricCandidate[]>([])
const pendingHistory = ref<RiskPointPendingPromotionHistory[]>([])
const pendingLoading = ref(false)

const pendingPromotionForm = reactive({
  riskPointId: undefined as number | undefined,
  pendingId: undefined as number | undefined,
  selectedMetrics: [] as Array<{ metricIdentifier: string; metricName: string }>,
  completePending: true,
  promotionNote: ''
})

const handleOpenPendingPromotion = async (row: RiskPoint) => {
  pendingPromotionForm.riskPointId = Number(row.id)
  pendingPromotionVisible.value = true
  await loadPendingBindings()
}

const handleSelectPendingRow = async (pending: RiskPointPendingBindingItem) => {
  pendingPromotionForm.pendingId = Number(pending.id)
  const res = await getPendingBindingCandidates(pending.id)
  if (res.code === 200) {
    pendingCandidates.value = res.data.candidates || []
    pendingHistory.value = res.data.history || []
  }
}

const handlePendingPromotionSubmit = async () => {
  if (!pendingPromotionForm.pendingId || pendingPromotionForm.selectedMetrics.length === 0) {
    ElMessage.warning('请至少选择一个测点')
    return
  }
  const res = await promotePendingBinding(pendingPromotionForm.pendingId, {
    metrics: pendingPromotionForm.selectedMetrics,
    completePending: pendingPromotionForm.completePending,
    promotionNote: pendingPromotionForm.promotionNote
  })
  if (res.code === 200) {
    ElMessage.success('待治理转正成功')
    await loadPendingBindings()
    void loadRiskPointList()
  }
}
```

- [ ] **Step 4: Re-run the Vue test**

Run: `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts`

Expected: PASS with the pending drawer loading rows, rendering recommendation evidence, and submitting the correct promote payload.

- [ ] **Step 5: Commit the frontend integration**

```bash
git add \
  spring-boot-iot-ui/src/api/riskPoint.ts \
  spring-boot-iot-ui/src/views/RiskPointView.vue \
  spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: add risk point pending promotion drawer"
```

## Task 5: Update Documentation and Run Full Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update the business-flow and database docs**

```md
- 风险对象中心 `/risk-point` 新增“待治理转正”副流程：当 `risk_point_device_pending_binding` 已具备 `risk_point_id + device_id` 但尚未确认正式测点时，治理人员可在单条 pending 抽屉内查看系统推荐候选，勾选一个或多个测点后转正为正式 `risk_point_device`。
- 新增 `risk_point_device_pending_promotion` 表：记录 pending 转正过程明细，包括正式绑定主键、测点、推荐等级、证据快照和操作人留痕。
```

- [ ] **Step 2: Update deployment, change-log, and acceptance docs**

```md
- `python scripts/run-real-env-schema-sync.py` 当前还会补齐 `risk_point_device_pending_promotion`，用于旧共享环境对齐待治理转正明细表。
- `/api/risk-point/pending-bindings`、`/api/risk-point/pending-bindings/{pendingId}/candidates`、`/api/risk-point/pending-bindings/{pendingId}/promote`、`/api/risk-point/pending-bindings/{pendingId}/ignore` 纳入风险对象中心最小验收链路。
- 第一版约束：pending 列表入口按单风险点治理展开，正式运行侧仍只消费 `risk_point_device`。
```

- [ ] **Step 3: Review `README.md` and `AGENTS.md`, then only edit if needed**

Run: `rg -n "risk-point|pending|schema sync|application-dev.yml" README.md AGENTS.md`

Expected: If neither file documents this workflow or changes a mandatory repo-wide rule, leave both files untouched and note “checked, no update required” in the task log. If either file already documents `/risk-point` governance workflow, update that exact section in place instead of creating a parallel note.

- [ ] **Step 4: Run the complete verification stack**

Run:

```bash
python3 -m unittest scripts/test_risk_point_pending_promotion_schema.py -v
mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingRecommendationServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointPendingControllerTest,RiskPointServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RiskPointView.test.ts
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:

- Python schema test: PASS
- Alarm-module tests: PASS
- Vue risk-point test: PASS
- Admin package build: PASS

Then do a real-environment smoke check with `application-dev.yml`:

```bash
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/risk-point/pending-bindings?riskPointId=<riskPointId>&pageNum=1&pageSize=10"
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/risk-point/pending-bindings/<pendingId>/candidates"
curl -X POST -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"metrics":[{"metricIdentifier":"dispsX","metricName":"X轴位移"}],"completePending":true,"promotionNote":"shared-dev smoke"}' \
  "http://localhost:8080/api/risk-point/pending-bindings/<pendingId>/promote"
```

Expected:

- First call returns `PENDING_METRIC_GOVERNANCE` rows for the selected risk point.
- Second call returns merged recommendation evidence.
- Third call returns `SUCCESS` or `DUPLICATE_SKIPPED` per metric and the new formal binding is visible in `/api/risk-point/bound-devices/<riskPointId>`.

- [ ] **Step 5: Commit docs and verification-ready changes**

```bash
git add \
  docs/02-业务功能与流程说明.md \
  docs/04-数据库设计与初始化数据.md \
  docs/07-部署运行与配置说明.md \
  docs/08-变更记录与技术债清单.md \
  docs/21-业务功能清单与验收标准.md
git commit -m "docs: document risk point pending promotion workflow"
```

## Self-Review Checklist

- Spec coverage:
  - Pending table/process table split: Task 1
  - `/api/risk-point/pending-bindings*` endpoints: Tasks 2 and 3
  - Real-time recommendation from product model + latest properties + message log: Task 2
  - One pending to many formal bindings: Task 3
  - Duplicate skip and process audit trail: Task 3
  - `/risk-point` drawer integration: Task 4
  - Docs and real-env verification: Task 5
- Placeholder scan:
  - No `TODO`, `TBD`, or “similar to above” shortcuts remain.
- Type consistency:
  - `RiskPointPendingBindingQuery`, `RiskPointPendingCandidateBundleVO`, `RiskPointPendingPromotionRequest`, and `RiskPointPendingPromotionResultVO` are referenced consistently across controller, service, and frontend tasks.
