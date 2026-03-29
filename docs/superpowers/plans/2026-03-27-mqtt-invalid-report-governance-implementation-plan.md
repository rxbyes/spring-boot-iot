# MQTT Invalid Report Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已批准设计落地 MQTT 无效上报治理：对 `DEVICE_NOT_FOUND` 与 `EMPTY_DECRYPTED_PAYLOAD` 实现入口熔断、失败样本采样、未登记设备最新态保留、失败频次计数分流，以及设备补录后的自动解封。

**Architecture:** 运行时治理拆成三层：`framework` 提供配置绑定与 Redis 计数/冷却存储，`message` 负责 MQTT 入口决策与失败回调抑制，`device` 负责无效上报最新态持久化、未登记名单查询和建档后解封。`iot_device_access_error_log` 继续承担失败样本详情，`iot_device_invalid_report_state` 承担最新态展示，最近 1h / 24h 与 `failure-stage-spike` 则改为读取 Redis 分钟桶聚合，避免“详情展示”和“频次统计”耦合在同一张表。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JdbcTemplate, Redis/StringRedisTemplate, Maven, JUnit 5, Mockito

---

## File Structure

### Existing files to modify

- `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- `spring-boot-iot-admin/src/main/resources/application-prod.yml`
- `spring-boot-iot-admin/src/main/resources/application-test.yml`
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttConnectionListener.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceAccessErrorLogService.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImpl.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImpl.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImplTest.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImplTest.java`
- `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumerTest.java`
- `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttConnectionListenerTest.java`
- `spring-boot-iot-admin/src/test/java/com/ghlzm/iot/admin/observability/alerting/ObservabilityAlertingServiceTest.java`
- `sql/init.sql`
- `README.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/07-部署运行与配置说明.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/11-可观测性、日志追踪与消息通知治理.md`
- `docs/21-业务功能清单与验收标准.md`

### New files to create

- `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/invalidreport/InvalidReportCounterStore.java`
- `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStore.java`
- `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStoreTest.java`
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/InvalidMqttReportReason.java`
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/InvalidMqttReportDecision.java`
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttInvalidReportGovernanceService.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceInvalidReportState.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceInvalidReportStateService.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateSchemaSupport.java`
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImpl.java`
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImplTest.java`
- `sql/upgrade/20260327_phase5_mqtt_invalid_report_state.sql`

### Review-only sync targets

- `AGENTS.md`
- `docs/superpowers/specs/2026-03-27-mqtt-invalid-report-governance-design.md`

说明：

- 用户已明确要求继续在 `codex/dev` 上工作，不新开分支；本计划不使用 git worktree。
- 当前工作区已有其他未提交修改，执行阶段必须只 `git add` 本计划所列文件，不得使用 `git add .`。
- `iot_device_access_error_log` 继续保留，不删除、不替换，只调整其写入语义和统计来源。

## Implementation Notes

- 首批治理原因固定为 `DEVICE_NOT_FOUND` 与 `EMPTY_DECRYPTED_PAYLOAD`，不要在本轮把 `UNSUPPORTED_TOPIC`、`PRODUCT_NOT_FOUND`、`PROTOCOL_MISMATCH` 一起打包做掉。
- `DEVICE_NOT_FOUND` 的标准 Topic 直连场景要尽量前移到 `TOPIC_ROUTE` 后做抑制；legacy `$dp` 仍允许最小解码一次后再进入冷却。
- 冷却期内重复命中只更新最新态和 Redis 计数，不再重复写 `dispatch_failed`、失败归档、`system_error`。
- `/devices` 未登记名单最终主来源切到 `iot_device_invalid_report_state`，但仍只展示 `DEVICE_NOT_FOUND` 且 `resolved=0` 的记录。
- `failure-stage-spike` 与失败归档统计摘要的最近 1h / 24h 数量，不再依赖失败归档详情表行数。
- 设备新增、批量新增、设备更换成功后，都要清理对应设备的抑制态并把最新态标记为 `resolved`。

### Task 1: 搭建治理配置与 Redis 计数/冷却底座

**Files:**
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-prod.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-test.yml`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/invalidreport/InvalidReportCounterStore.java`
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStore.java`
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStoreTest.java`

- [ ] **Step 1: 先写 Redis 计数/冷却存储的失败测试**

在 `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStoreTest.java` 新增：

```java
@ExtendWith(MockitoExtension.class)
class RedisInvalidReportCounterStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisInvalidReportCounterStore store;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getInvalidReportGovernance().setEnabled(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisInvalidReportCounterStore(stringRedisTemplate, properties, Clock.fixed(
                Instant.parse("2026-03-27T13:45:00Z"),
                ZoneId.of("Asia/Shanghai")
        ));
    }

    @Test
    void shouldIncrementFailureStageBucketAndReadWindowSummary() {
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(valueOperations.get("iot:invalid-report:bucket:202603272145:failure-stage:device_validate"))
                .thenReturn("3");

        long hitCount = store.incrementFailureStage("device_validate");
        long recentCount = store.sumFailureStageSince("device_validate", Instant.parse("2026-03-27T13:44:00Z"));

        assertEquals(3L, hitCount);
        assertEquals(3L, recentCount);
    }

    @Test
    void shouldUseCooldownKeyForSuppression() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true, false);

        boolean firstAcquired = store.tryOpenCooldown("tenant=1|device=missing-01", Duration.ofMinutes(30));
        boolean secondAcquired = store.tryOpenCooldown("tenant=1|device=missing-01", Duration.ofMinutes(30));

        assertTrue(firstAcquired);
        assertFalse(secondAcquired);
    }
}
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
mvn -pl spring-boot-iot-framework -am -Dtest=RedisInvalidReportCounterStoreTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 测试失败，因为 `RedisInvalidReportCounterStore` 与 `invalid-report-governance` 配置结构尚不存在。

- [ ] **Step 3: 写最小配置绑定与 Redis 存储实现**

在 `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java` 的 `Observability` 下新增：

```java
@Data
public static class InvalidReportGovernance {
    private Boolean enabled = Boolean.FALSE;
    private Integer bucketTtlHours = 26;
    private EmptyPayload emptyPayload = new EmptyPayload();
    private DeviceNotFound deviceNotFound = new DeviceNotFound();

    @Data
    public static class EmptyPayload {
        private Integer thresholdWindowSeconds = 60;
        private Integer thresholdCount = 3;
        private Integer cooldownMinutes = 15;
    }

    @Data
    public static class DeviceNotFound {
        private Integer thresholdWindowSeconds = 60;
        private Integer thresholdCount = 2;
        private Integer cooldownMinutes = 30;
    }
}
```

并在 `Observability` 根对象中挂入：

```java
private InvalidReportGovernance invalidReportGovernance = new InvalidReportGovernance();
```

创建 `InvalidReportCounterStore.java`：

```java
public interface InvalidReportCounterStore {

    long incrementFailureStage(String failureStage);

    long incrementReasonCode(String reasonCode);

    long sumFailureStageSince(String failureStage, Instant startInclusive);

    boolean tryOpenCooldown(String governanceKey, Duration ttl);

    void clearCooldown(String governanceKey);
}
```

创建 `RedisInvalidReportCounterStore.java`，键口径固定为：

```java
private static final String FAILURE_STAGE_PREFIX = "iot:invalid-report:bucket:";
private static final String REASON_PREFIX = "iot:invalid-report:reason:";
private static final String COOLDOWN_PREFIX = "iot:invalid-report:cooldown:";
```

实现细节：

```java
public long incrementFailureStage(String failureStage) {
    String key = buildMinuteBucketKey(FAILURE_STAGE_PREFIX, normalizeKey(failureStage));
    Long value = stringRedisTemplate.opsForValue().increment(key);
    stringRedisTemplate.expire(key, resolveBucketTtl());
    return value == null ? 0L : value;
}

public boolean tryOpenCooldown(String governanceKey, Duration ttl) {
    return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
            COOLDOWN_PREFIX + governanceKey.trim(),
            "1",
            ttl
    ));
}
```

在 `application-dev.yml` / `application-prod.yml` / `application-test.yml` 增加：

```yaml
  observability:
    invalid-report-governance:
      enabled: ${IOT_OBSERVABILITY_INVALID_REPORT_GOVERNANCE_ENABLED:true}
      bucket-ttl-hours: ${IOT_OBSERVABILITY_INVALID_REPORT_BUCKET_TTL_HOURS:26}
      empty-payload:
        threshold-window-seconds: ${IOT_OBSERVABILITY_INVALID_REPORT_EMPTY_PAYLOAD_WINDOW_SECONDS:60}
        threshold-count: ${IOT_OBSERVABILITY_INVALID_REPORT_EMPTY_PAYLOAD_THRESHOLD_COUNT:3}
        cooldown-minutes: ${IOT_OBSERVABILITY_INVALID_REPORT_EMPTY_PAYLOAD_COOLDOWN_MINUTES:15}
      device-not-found:
        threshold-window-seconds: ${IOT_OBSERVABILITY_INVALID_REPORT_DEVICE_NOT_FOUND_WINDOW_SECONDS:60}
        threshold-count: ${IOT_OBSERVABILITY_INVALID_REPORT_DEVICE_NOT_FOUND_THRESHOLD_COUNT:2}
        cooldown-minutes: ${IOT_OBSERVABILITY_INVALID_REPORT_DEVICE_NOT_FOUND_COOLDOWN_MINUTES:30}
```

- [ ] **Step 4: 运行测试确认转绿**

Run:

```bash
mvn -pl spring-boot-iot-framework -am -Dtest=RedisInvalidReportCounterStoreTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- `RedisInvalidReportCounterStoreTest` 通过。

- [ ] **Step 5: 提交当前任务涉及文件**

```bash
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/invalidreport/InvalidReportCounterStore.java
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStore.java
git add spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/invalidreport/RedisInvalidReportCounterStoreTest.java
git add spring-boot-iot-admin/src/main/resources/application-dev.yml
git add spring-boot-iot-admin/src/main/resources/application-prod.yml
git add spring-boot-iot-admin/src/main/resources/application-test.yml
git commit -m "feat: add invalid mqtt report governance config"
```

### Task 2: 新增无效上报最新态表与持久化服务

**Files:**
- Modify: `sql/init.sql`
- Create: `sql/upgrade/20260327_phase5_mqtt_invalid_report_state.sql`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceInvalidReportState.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceInvalidReportStateService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateSchemaSupport.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImpl.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImplTest.java`

- [ ] **Step 1: 先写最新态 upsert 与 resolve 的失败测试**

在 `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImplTest.java` 新增：

```java
@ExtendWith(MockitoExtension.class)
class DeviceInvalidReportStateServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DeviceInvalidReportStateSchemaSupport schemaSupport;

    private DeviceInvalidReportStateServiceImpl service;

    @BeforeEach
    void setUp() {
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of(
                "id", "governance_key", "reason_code", "device_code", "product_key",
                "failure_stage", "topic", "last_trace_id", "last_payload",
                "first_seen_time", "last_seen_time", "hit_count", "sampled_count",
                "suppressed_count", "suppressed_until", "resolved", "resolved_time", "deleted"
        )));
        service = new DeviceInvalidReportStateServiceImpl(jdbcTemplate, schemaSupport);
    }

    @Test
    void upsertStateShouldInsertOnFirstHitAndCarryCounters() {
        DeviceInvalidReportState state = new DeviceInvalidReportState();
        state.setGovernanceKey("tenant=1|product=obs-product|device=missing-01|reason=DEVICE_NOT_FOUND");
        state.setReasonCode("DEVICE_NOT_FOUND");
        state.setDeviceCode("missing-01");
        state.setProductKey("obs-product");
        state.setFailureStage("device_validate");
        state.setLastTraceId("trace-missing-001");
        state.setLastPayload("{\"deviceCode\":\"missing-01\"}");
        state.setHitCount(1L);

        service.upsertState(state);

        verify(jdbcTemplate).update(startsWith("INSERT INTO iot_device_invalid_report_state"), any(Object[].class));
    }

    @Test
    void markResolvedByDeviceShouldResolveAllMatchingOpenStates() {
        service.markResolvedByDevice("obs-product", "missing-01", LocalDateTime.of(2026, 3, 27, 22, 20, 0));

        verify(jdbcTemplate).update(
                contains("SET resolved = 1"),
                eq(LocalDateTime.of(2026, 3, 27, 22, 20, 0)),
                eq("obs-product"),
                eq("missing-01")
        );
    }
}
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceInvalidReportStateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 测试失败，因为最新态实体、schema support、service 实现和表结构还不存在。

- [ ] **Step 3: 新增表结构与服务最小实现**

在 `sql/init.sql` 与 `sql/upgrade/20260327_phase5_mqtt_invalid_report_state.sql` 中新增：

```sql
CREATE TABLE IF NOT EXISTS iot_device_invalid_report_state (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    governance_key VARCHAR(255) NOT NULL COMMENT '治理唯一键',
    reason_code VARCHAR(64) NOT NULL COMMENT '治理原因编码',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT '失败阶段',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品Key',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic 路由类型',
    topic VARCHAR(255) DEFAULT NULL COMMENT '最近 topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT '最近 clientId',
    payload_size INT DEFAULT NULL COMMENT '最近 payload 大小',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT '最近 payload 编码',
    last_payload LONGTEXT DEFAULT NULL COMMENT '最近 payload',
    last_trace_id VARCHAR(64) DEFAULT NULL COMMENT '最近 traceId',
    sample_error_message VARCHAR(500) DEFAULT NULL COMMENT '样本错误消息',
    sample_exception_class VARCHAR(255) DEFAULT NULL COMMENT '样本异常类',
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次命中时间',
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近命中时间',
    hit_count BIGINT NOT NULL DEFAULT 0 COMMENT '总命中次数',
    sampled_count BIGINT NOT NULL DEFAULT 0 COMMENT '已采样次数',
    suppressed_count BIGINT NOT NULL DEFAULT 0 COMMENT '被抑制次数',
    suppressed_until DATETIME DEFAULT NULL COMMENT '抑制截止时间',
    resolved TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解封',
    resolved_time DATETIME DEFAULT NULL COMMENT '解封时间',
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invalid_report_state_governance_key (governance_key),
    KEY idx_invalid_report_device_resolved (device_code, product_key, resolved, last_seen_time),
    KEY idx_invalid_report_reason_time (reason_code, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无效 MQTT 上报最新态表';
```

创建 `DeviceInvalidReportStateService.java`：

```java
public interface DeviceInvalidReportStateService {

    void upsertState(DeviceInvalidReportState state);

    void markResolvedByDevice(String productKey, String deviceCode, LocalDateTime resolvedTime);
}
```

在 `DeviceInvalidReportStateServiceImpl.java` 使用 `JdbcTemplate` 做显式 upsert：

```java
public void upsertState(DeviceInvalidReportState state) {
    String updateSql = """
            UPDATE iot_device_invalid_report_state
               SET failure_stage = ?, topic = ?, last_trace_id = ?, last_payload = ?,
                   hit_count = ?, sampled_count = ?, suppressed_count = ?, suppressed_until = ?,
                   last_seen_time = ?, resolved = 0, resolved_time = NULL
             WHERE governance_key = ? AND deleted = 0
            """;
    int updated = jdbcTemplate.update(updateSql, ...);
    if (updated > 0) {
        return;
    }
    jdbcTemplate.update("INSERT INTO iot_device_invalid_report_state (...) VALUES (...)", ...);
}
```

`markResolvedByDevice`：

```java
public void markResolvedByDevice(String productKey, String deviceCode, LocalDateTime resolvedTime) {
    jdbcTemplate.update("""
            UPDATE iot_device_invalid_report_state
               SET resolved = 1, resolved_time = ?
             WHERE deleted = 0
               AND resolved = 0
               AND product_key = ?
               AND device_code = ?
            """, resolvedTime, productKey, deviceCode);
}
```

- [ ] **Step 4: 运行测试确认转绿**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceInvalidReportStateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- `DeviceInvalidReportStateServiceImplTest` 通过。

- [ ] **Step 5: 提交当前任务涉及文件**

```bash
git add sql/init.sql
git add sql/upgrade/20260327_phase5_mqtt_invalid_report_state.sql
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceInvalidReportState.java
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceInvalidReportStateService.java
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateSchemaSupport.java
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImpl.java
git add spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceInvalidReportStateServiceImplTest.java
git commit -m "feat: persist invalid mqtt report latest state"
```

### Task 3: 在 MQTT 入口与失败回调中接入治理决策

**Files:**
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/InvalidMqttReportReason.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/InvalidMqttReportDecision.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttInvalidReportGovernanceService.java`
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttConnectionListener.java`
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumerTest.java`
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttConnectionListenerTest.java`

- [ ] **Step 1: 先写入口快速丢弃与失败抑制的失败测试**

在 `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumerTest.java` 追加：

```java
@Test
void messageArrivedShouldDropEmptyPayloadBeforePipelineWhenCooldownActive() {
    IotProperties properties = new IotProperties();
    MqttInvalidReportGovernanceService governanceService = mock(MqttInvalidReportGovernanceService.class);
    when(governanceService.handleRawEmptyPayload("$dp", null)).thenReturn(InvalidMqttReportDecision.dropSuppressed());

    MqttMessageConsumer consumer = new MqttMessageConsumer(
            properties,
            upMessageProcessingPipeline,
            mqttTopicRouter,
            mqttConnectionListener,
            mqttConsumerRuntimeState,
            mqttClusterLeadershipService,
            governanceService
    );

    consumer.messageArrived("$dp", new MqttMessage(new byte[0]));

    verifyNoInteractions(upMessageProcessingPipeline);
    verify(mqttConnectionListener, never()).onMessageDispatchFailed(anyString(), any(), any(), any());
}
```

在 `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttConnectionListenerTest.java` 追加：

```java
@Test
void shouldSkipArchiveAndBackendExceptionWhenFailureIsSuppressed() {
    AtomicReference<BackendExceptionEvent> captured = new AtomicReference<>();
    AtomicReference<ArchiveCall> archived = new AtomicReference<>();
    MqttInvalidReportGovernanceService governanceService = mock(MqttInvalidReportGovernanceService.class);
    when(governanceService.handleDispatchFailure(any(), any(), any(), any()))
            .thenReturn(InvalidMqttReportDecision.dropSuppressed());

    MqttConnectionListener listener = newListener(captured, archived, governanceService);
    listener.onMessageDispatchFailed("$dp", new byte[0], new RawDeviceMessage(), new BizException("设备不存在: missing-01"));

    assertNull(captured.get());
    assertNull(archived.get());
}
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
mvn -pl spring-boot-iot-message -am -Dtest=MqttMessageConsumerTest,MqttConnectionListenerTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 测试失败，因为 `MqttInvalidReportGovernanceService`、新构造参数与抑制分支尚不存在。

- [ ] **Step 3: 写最小治理服务与消息入口集成**

创建 `InvalidMqttReportReason.java`：

```java
public enum InvalidMqttReportReason {
    EMPTY_DECRYPTED_PAYLOAD,
    DEVICE_NOT_FOUND
}
```

创建 `InvalidMqttReportDecision.java`：

```java
public record InvalidMqttReportDecision(
        boolean suppressed,
        boolean sampleFailure,
        InvalidMqttReportReason reason
) {
    public static InvalidMqttReportDecision allowSample(InvalidMqttReportReason reason) {
        return new InvalidMqttReportDecision(false, true, reason);
    }
    public static InvalidMqttReportDecision dropSuppressed() {
        return new InvalidMqttReportDecision(true, false, null);
    }
}
```

在 `MqttInvalidReportGovernanceService.java` 实现两个入口：

```java
public InvalidMqttReportDecision handleRawEmptyPayload(String topic, RawDeviceMessage rawDeviceMessage) {
    return evaluateAndPersist(InvalidMqttReportReason.EMPTY_DECRYPTED_PAYLOAD, topic, rawDeviceMessage, null, null);
}

public InvalidMqttReportDecision handleDispatchFailure(String topic,
                                                       byte[] payload,
                                                       RawDeviceMessage rawDeviceMessage,
                                                       Throwable throwable) {
    InvalidMqttReportReason reason = resolveReason(throwable);
    if (reason == null) {
        return InvalidMqttReportDecision.allowSample(null);
    }
    return evaluateAndPersist(reason, topic, rawDeviceMessage, payload, throwable);
}
```

`MqttMessageConsumer#messageArrived` 在 `onMessageReceived` 后追加：

```java
if (message == null || message.getPayload() == null || message.getPayload().length == 0) {
    InvalidMqttReportDecision decision = mqttInvalidReportGovernanceService.handleRawEmptyPayload(topic, rawDeviceMessage);
    if (decision.suppressed()) {
        return;
    }
}
```

`MqttConnectionListener#onMessageDispatchFailed` 最前面改为：

```java
InvalidMqttReportDecision decision = mqttInvalidReportGovernanceService.handleDispatchFailure(
        topic, payload, rawDeviceMessage, throwable
);
if (decision.suppressed()) {
    runtimeState().ifPresent(state -> state.markFailure("suppressed", rawDeviceMessage == null ? null : rawDeviceMessage.getTraceId()));
    return;
}
```

并保持只有 `sampleFailure=true` 时才继续：

```java
archiveFailureTrace(...);
archiveAccessFailure(...);
recordBackendException(...);
```

- [ ] **Step 4: 运行测试确认转绿**

Run:

```bash
mvn -pl spring-boot-iot-message -am -Dtest=MqttMessageConsumerTest,MqttConnectionListenerTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 两个消息模块测试通过，证明入口快速丢弃和失败抑制都已生效。

- [ ] **Step 5: 提交当前任务涉及文件**

```bash
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/InvalidMqttReportReason.java
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/InvalidMqttReportDecision.java
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttInvalidReportGovernanceService.java
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttConnectionListener.java
git add spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumerTest.java
git add spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttConnectionListenerTest.java
git commit -m "feat: suppress repeated invalid mqtt reports"
```

### Task 4: 把最新态接入未登记名单，并在建档成功后自动解封

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`

- [ ] **Step 1: 先写未登记名单改读最新态与建档解封的失败测试**

在 `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImplTest.java` 追加：

```java
@Test
void listByFiltersShouldPreferInvalidReportStateForDeviceNotFoundRows() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DevicePageVO>>any(), any(Object[].class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0, String.class);
                @SuppressWarnings("unchecked")
                RowMapper<DevicePageVO> rowMapper = invocation.getArgument(1, RowMapper.class);
                if (sql.contains("iot_device_invalid_report_state")) {
                    return List.of(rowMapper.mapRow(mockInvalidStateResultSet(
                            2036009677345259601L,
                            "missing-01",
                            "obs-product",
                            "device_validate",
                            "设备不存在: missing-01",
                            "$dp",
                            "trace-state-001",
                            "{\"deviceCode\":\"missing-01\"}",
                            LocalDateTime.of(2026, 3, 27, 22, 10, 0)
                    ), 0));
                }
                return List.of();
            });

    List<DevicePageVO> records = service.listByFilters("obs-product", "missing", 0L, 10L);

    assertEquals(1, records.size());
    assertEquals("invalid_report_state", records.get(0).getAssetSourceType());
    assertEquals("missing-01", records.get(0).getDeviceCode());
}
```

在 `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java` 追加：

```java
@Test
void addDeviceShouldResolveInvalidReportStateAfterArchiveCreate() {
    DeviceInvalidReportStateService invalidReportStateService = mock(DeviceInvalidReportStateService.class);
    DeviceServiceImpl deviceService = new DeviceServiceImpl(
            productService,
            devicePropertyMapper,
            productModelMapper,
            unregisteredDeviceRosterService,
            iotProperties,
            invalidReportStateService
    );

    DeviceAddDTO dto = buildDeviceAddDTO("obs-product", "missing-01");
    when(productService.getRequiredByProductKey("obs-product")).thenReturn(enabledProduct("obs-product"));

    deviceService.addDevice(dto);

    verify(invalidReportStateService).markResolvedByDevice(eq("obs-product"), eq("missing-01"), any(LocalDateTime.class));
}
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=UnregisteredDeviceRosterServiceImplTest,DeviceServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 列表测试失败，因为 `UnregisteredDeviceRosterServiceImpl` 还不会查询 `iot_device_invalid_report_state`。
- 建档测试失败，因为 `DeviceServiceImpl` 还没有注入最新态服务并执行解封。

- [ ] **Step 3: 写最小查询切换与自动解封实现**

在 `UnregisteredDeviceRosterServiceImpl.java` 中新增最新态来源常量：

```java
private static final String INVALID_REPORT_STATE_TABLE = "iot_device_invalid_report_state";
private static final String INVALID_REPORT_STATE_SOURCE = "invalid_report_state";
private static final String DEVICE_NOT_FOUND_REASON = "DEVICE_NOT_FOUND";
```

将 `countFromMergedSources` / `listFromMergedSources` 的第一来源改为：

```sql
SELECT
  s.id AS source_record_id,
  s.device_code,
  s.product_key,
  s.protocol_code,
  'invalid_report_state' AS asset_source_type,
  s.failure_stage,
  s.sample_error_message AS error_message,
  s.topic,
  s.last_trace_id AS trace_id,
  s.last_payload AS payload,
  s.last_seen_time AS report_time
FROM iot_device_invalid_report_state s
LEFT JOIN iot_device d
  ON d.device_code = s.device_code
 AND d.deleted = 0
WHERE s.deleted = 0
  AND s.resolved = 0
  AND s.reason_code = 'DEVICE_NOT_FOUND'
  AND d.id IS NULL
```

在 `DeviceServiceImpl.java` 构造器中注入 `DeviceInvalidReportStateService`，并在以下成功路径后调用：

```java
invalidReportStateService.markResolvedByDevice(product.getProductKey(), device.getDeviceCode(), LocalDateTime.now());
```

调用位置：

1. `addDevice`
2. `batchAddDevices` 内每条成功创建后
3. `replaceDevice` 中新设备创建成功后

- [ ] **Step 4: 运行测试确认转绿**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=UnregisteredDeviceRosterServiceImplTest,DeviceServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 列表与建档解封测试通过。

- [ ] **Step 5: 提交当前任务涉及文件**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImpl.java
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java
git add spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/UnregisteredDeviceRosterServiceImplTest.java
git add spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java
git commit -m "feat: move unregistered device roster to invalid report state"
```

### Task 5: 把失败归档统计与告警切到 Redis 聚合计数

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceAccessErrorLogService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImplTest.java`
- Modify: `spring-boot-iot-admin/src/test/java/com/ghlzm/iot/admin/observability/alerting/ObservabilityAlertingServiceTest.java`

- [ ] **Step 1: 先写统计切到聚合计数的失败测试**

在 `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImplTest.java` 新增：

```java
@Test
void listFailureStageCountsSinceShouldReadAggregatedBucketsInsteadOfDetailRows() {
    InvalidReportCounterStore counterStore = mock(InvalidReportCounterStore.class);
    DeviceAccessErrorLogServiceImpl service = new DeviceAccessErrorLogServiceImpl(
            jdbcTemplate,
            schemaSupport,
            deviceMapper,
            productMapper,
            iotProperties,
            counterStore
    );
    when(counterStore.sumFailureStageSince("protocol_decode", Instant.parse("2026-03-27T13:30:00Z"))).thenReturn(12L);
    when(counterStore.sumFailureStageSince("device_validate", Instant.parse("2026-03-27T13:30:00Z"))).thenReturn(9L);

    List<DeviceAccessErrorLogService.FailureStageCount> counts = service.listFailureStageCountsSince(
            Date.from(Instant.parse("2026-03-27T13:30:00Z"))
    );

    assertEquals("protocol_decode", counts.get(0).failureStage());
    assertEquals(12L, counts.get(0).failureCount());
}
```

在 `spring-boot-iot-admin/src/test/java/com/ghlzm/iot/admin/observability/alerting/ObservabilityAlertingServiceTest.java` 的 `shouldTriggerFailureStageSpikeOnlyForThresholdHits` 中保留断言不变，用于回归证明业务语义未变。

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceAccessErrorLogServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
mvn -pl spring-boot-iot-admin -am -Dtest=ObservabilityAlertingServiceTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- `DeviceAccessErrorLogServiceImplTest` 失败，因为服务还没有接入 `InvalidReportCounterStore`。
- 告警测试当前保持绿灯，作为后续回归对照。

- [ ] **Step 3: 写最小统计分流实现**

在 `DeviceAccessErrorLogServiceImpl.java` 构造器中注入 `InvalidReportCounterStore`，并在 `archiveMqttFailure` 前后保留样本归档逻辑不变，同时新增：

```java
private final InvalidReportCounterStore invalidReportCounterStore;
```

将 `listFailureStageCountsSince` 改为固定枚举当前已知 stage 并按 Redis 求和：

```java
private static final List<String> FAILURE_STAGE_BUCKETS = List.of(
        "topic_route",
        "protocol_decode",
        "device_validate",
        "message_dispatch"
);

public List<FailureStageCount> listFailureStageCountsSince(Date startTime) {
    Instant start = startTime == null ? Instant.now().minus(Duration.ofMinutes(10)) : startTime.toInstant();
    return FAILURE_STAGE_BUCKETS.stream()
            .map(stage -> new FailureStageCount(stage, invalidReportCounterStore.sumFailureStageSince(stage, start)))
            .filter(item -> item.failureCount() > 0L)
            .sorted(Comparator.comparingLong(FailureStageCount::failureCount).reversed()
                    .thenComparing(FailureStageCount::failureStage))
            .toList();
}
```

同时把 `getStats` 的 `recentHourCount / recent24HourCount / topFailureStages` 切到 Redis 计数，其他桶继续用样本归档查询。

- [ ] **Step 4: 运行测试确认转绿**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=DeviceAccessErrorLogServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
mvn -pl spring-boot-iot-admin -am -Dtest=ObservabilityAlertingServiceTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Expected:

- 设备模块统计测试通过。
- 告警测试继续通过，证明 `failure-stage-spike` 语义未退化。

- [ ] **Step 5: 提交当前任务涉及文件**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceAccessErrorLogService.java
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImpl.java
git add spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImplTest.java
git add spring-boot-iot-admin/src/test/java/com/ghlzm/iot/admin/observability/alerting/ObservabilityAlertingServiceTest.java
git commit -m "feat: separate invalid report stats from detail samples"
```

### Task 6: 文档同步与最终验证

**Files:**
- Modify: `README.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 先补文档断言需要覆盖的事实**

在这些文档中明确写出以下新事实：

```md
- MQTT 无效上报治理当前首批覆盖 `DEVICE_NOT_FOUND` 与 `EMPTY_DECRYPTED_PAYLOAD`。
- `iot_device_access_error_log` 当前保留失败样本，不再要求每次重复坏报文都落一条详情。
- 未登记设备名单当前主来源改为 `iot_device_invalid_report_state`，默认只展示 `DEVICE_NOT_FOUND` 且未解封的最新态。
- 最近 1h / 24h 与 `failure-stage-spike` 当前读取 Redis 聚合计数，而不是失败样本详情行数。
- 设备补录建档成功后会自动清理对应设备的无效上报抑制态。
```

- [ ] **Step 2: 运行针对性测试与构建验证**

Run:

```bash
mvn -pl spring-boot-iot-framework -am -Dtest=RedisInvalidReportCounterStoreTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
mvn -pl spring-boot-iot-message -am -Dtest=MqttMessageConsumerTest,MqttConnectionListenerTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
mvn -pl spring-boot-iot-device -am -Dtest=DeviceInvalidReportStateServiceImplTest,DeviceAccessErrorLogServiceImplTest,DeviceServiceImplTest,UnregisteredDeviceRosterServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
mvn -pl spring-boot-iot-admin -am -Dtest=ObservabilityAlertingServiceTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:

- 所有新增和回归测试通过。
- `spring-boot-iot-admin` 打包成功。

- [ ] **Step 3: 真实环境验收准备说明**

Run:

```bash
python scripts/run-message-flow-acceptance.py --expired-trace-id <已过期TraceId>
```

并在验收记录中人工补充：

1. 连续发送空 payload 的 `$dp` 报文，确认只保留首次样本、后续进入抑制。
2. 连续发送不存在设备的标准 Topic 报文，确认未登记名单只更新最新态。
3. 补录该设备后再次发送正常上报，确认已自动解封。

- [ ] **Step 4: 复核 `README.md` 与 `AGENTS.md` 是否需要更新**

检查准则：

1. `README.md` 是否需要新增“无效 MQTT 上报治理”一句概览。
2. `AGENTS.md` 当前是否需要补充“未登记设备名单以最新态为准”的规则。

若现有文字已足够：

```md
已复核，当前无需更新 `AGENTS.md`。
```

- [ ] **Step 5: 提交当前任务涉及文件**

```bash
git add README.md
git add docs/03-接口规范与接口清单.md
git add docs/04-数据库设计与初始化数据.md
git add docs/07-部署运行与配置说明.md
git add docs/08-变更记录与技术债清单.md
git add docs/11-可观测性、日志追踪与消息通知治理.md
git add docs/21-业务功能清单与验收标准.md
git commit -m "docs: document invalid mqtt report governance"
```

## Final Verification Checklist

- [ ] `RedisInvalidReportCounterStoreTest` 通过
- [ ] `MqttMessageConsumerTest` 通过
- [ ] `MqttConnectionListenerTest` 通过
- [ ] `DeviceInvalidReportStateServiceImplTest` 通过
- [ ] `DeviceAccessErrorLogServiceImplTest` 通过
- [ ] `DeviceServiceImplTest` 通过
- [ ] `UnregisteredDeviceRosterServiceImplTest` 通过
- [ ] `ObservabilityAlertingServiceTest` 通过
- [ ] `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- [ ] 真实环境验收改用 `application-dev.yml`，且未回退 H2 验收链路

## Open Questions to Resolve During Execution

1. `application-prod.yml` 当前没有和 `application-dev.yml` 完全同构的 `observability.invalid-report-governance` 节点；执行时若生产配置结构不同，优先按现有 prod 文件组织方式补齐，不强行复制 dev 的缩进布局。
2. `listFailureStageCountsSince` 改为 Redis 聚合后，若需要支持动态未知 stage，执行时可在 `InvalidReportCounterStore` 中补一个“最近窗口内所有 stage key 枚举”接口；若这一步复杂度明显上升，则先按固定 stage 列表落地并在 `docs/08` 中记录限制。
3. 本轮不直接实现 Broker 认证/ACL，只更新代码预留和文档口径；如果执行时发现仓库里已经有可复用 Broker 接入点，再单独评估是否扩展。
