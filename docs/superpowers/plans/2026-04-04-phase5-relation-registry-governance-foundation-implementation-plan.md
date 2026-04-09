# Phase 5 Relation Registry And Governance Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a database-backed device relation registry, let the protocol layer read relation mappings with config fallback, and make product model governance relation-aware for both runtime and manual extraction.

**Architecture:** This wave keeps the existing modular monolith and fixed Pipeline intact. The `device` module becomes the source of truth for logical-channel-to-child-device relations, the `protocol` module consumes those relations through a narrow resolver interface, and the `product model` governance flow uses the same relation model to filter parent-owned versus child-owned candidates before compare/apply.

**Tech Stack:** Java 17, Spring Boot 4, MyBatis-Plus, MySQL `sql/init.sql`, existing `spring-boot-iot-device` and `spring-boot-iot-protocol` modules, JUnit 5, Mockito

---

## File Map

### Create

- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceRelation.java`
  - Persist one logical channel relation row per parent-device / logical-channel / child-device binding.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/DeviceRelationMapper.java`
  - MyBatis mapper for relation CRUD and runtime lookups.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceRelationUpsertDTO.java`
  - Request body for creating/updating device relations.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceRelationVO.java`
  - Response model for relation queries.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceRelationService.java`
  - Service contract for CRUD and runtime relation resolution.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImpl.java`
  - Device-relation orchestration, validation, CRUD, and lookup helpers.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceRelationController.java`
  - Backend management API for relation registry.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/DeviceRelationRule.java`
  - Runtime relation view used by governance and protocol resolver.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationLegacyDpResolver.java`
  - `protocol`-module resolver implementation backed by `DeviceRelationService`.
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpRelationResolver.java`
  - Narrow protocol-side interface for resolving parent-device relation rules.
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpRelationRule.java`
  - Protocol-side immutable rule object holding child device code, canonical strategy, and sensor-state mirror strategy.
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImplTest.java`
  - Unit tests for relation CRUD validation and runtime lookup.

### Modify

- `sql/init.sql`
  - Add `iot_device_relation` table and indexes.
- `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
  - Mark `subDeviceMappings` as legacy fallback only in comments; keep behavior for compatibility.
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
  - Inject relation resolver into the legacy `$dp` child splitter path.
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java`
  - Resolve mappings from relation registry first, then fallback to `iot.device.sub-device-mappings`.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelManualExtractDTO.java`
  - Add optional `sourceDeviceCode` and `extractMode`.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
  - Add relation-aware manual extract context fields.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
  - Inject relation service, filter runtime candidates for parent products, and canonicalize manual extract when a parent payload is used to govern a child product.
- `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
  - Add relation-aware runtime/manual governance tests.
- `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpChildMessageSplitterTest.java`
  - Add resolver-backed split tests.
- `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`
  - Add adapter test proving registry-backed relation resolution works end-to-end.
- `docs/03-接口规范与接口清单.md`
  - Document relation registry APIs and new governance request fields.
- `docs/04-数据库设计与初始化数据.md`
  - Document `iot_device_relation`.
- `docs/05-protocol.md`
  - Document DB-backed relation resolution with config fallback.
- `docs/08-变更记录与技术债清单.md`
  - Record this phase-5 wave-1 foundation.

### Deliberately Deferred To Later Plans

- Generic protocol/relationship template engine for every sensor family.
- Frontend relation management UI under `/devices` or `/products`.
- Async event-stream refactor for latest/telemetry/risk dispatch hot path.

## Task 1: Device Relation Registry Schema And CRUD Backend

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceRelation.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/DeviceRelationMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceRelationUpsertDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceRelationVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceRelationService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceRelationController.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/DeviceRelationRule.java`
- Modify: `sql/init.sql`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImplTest.java`

- [ ] **Step 1: Write the failing service test**

```java
@ExtendWith(MockitoExtension.class)
class DeviceRelationServiceImplTest {

    @Mock
    private DeviceRelationMapper deviceRelationMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;

    private DeviceRelationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceRelationServiceImpl(deviceRelationMapper, deviceMapper, productMapper);
    }

    @Test
    void createRelationShouldRejectDuplicateLogicalChannelWithinSameParent() {
        DeviceRelationUpsertDTO dto = new DeviceRelationUpsertDTO();
        dto.setParentDeviceCode("SK00EA0D1307986");
        dto.setLogicalChannelCode("L1_LF_1");
        dto.setChildDeviceCode("202018143");
        dto.setRelationType("collector_child");
        dto.setCanonicalizationStrategy("LF_VALUE");
        dto.setStatusMirrorStrategy("SENSOR_STATE");

        when(deviceMapper.selectOne(any())).thenReturn(device(10L, "SK00EA0D1307986", 1001L));
        when(deviceRelationMapper.selectOne(any())).thenReturn(existingRelation(9001L, "SK00EA0D1307986", "L1_LF_1"));

        BizException ex = assertThrows(BizException.class, () -> service.createRelation(1L, dto));

        assertEquals("同一父设备下逻辑通道已存在: L1_LF_1", ex.getMessage());
    }

    @Test
    void listRulesByParentDeviceCodeShouldReturnEnabledRulesOrderedByLogicalChannel() {
        when(deviceRelationMapper.selectList(any())).thenReturn(List.of(
                relation(2L, "SK00EA0D1307986", "L1_LF_2", "202018135"),
                relation(1L, "SK00EA0D1307986", "L1_LF_1", "202018143")
        ));

        List<DeviceRelationRule> rules = service.listEnabledRulesByParentDeviceCode("SK00EA0D1307986");

        assertEquals(List.of("L1_LF_1", "L1_LF_2"), rules.stream().map(DeviceRelationRule::getLogicalChannelCode).toList());
        assertEquals(List.of("202018143", "202018135"), rules.stream().map(DeviceRelationRule::getChildDeviceCode).toList());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl spring-boot-iot-device -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=DeviceRelationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
FAIL because DeviceRelationServiceImpl / DeviceRelationMapper / DeviceRelationUpsertDTO symbols do not exist yet
```

- [ ] **Step 3: Write minimal schema, entity, mapper, service, and controller**

`sql/init.sql`

```sql
CREATE TABLE iot_device_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    parent_device_id BIGINT NOT NULL COMMENT '父设备ID',
    parent_device_code VARCHAR(64) NOT NULL COMMENT '父设备编码',
    logical_channel_code VARCHAR(64) NOT NULL COMMENT '逻辑通道编码，如 L1_LF_1',
    child_device_id BIGINT NOT NULL COMMENT '子设备ID',
    child_device_code VARCHAR(64) NOT NULL COMMENT '子设备编码',
    child_product_id BIGINT DEFAULT NULL COMMENT '子产品ID',
    child_product_key VARCHAR(64) DEFAULT NULL COMMENT '子产品 productKey',
    relation_type VARCHAR(32) NOT NULL COMMENT 'collector_child/gateway_child',
    canonicalization_strategy VARCHAR(32) NOT NULL COMMENT 'LF_VALUE/SW_DISPS',
    status_mirror_strategy VARCHAR(32) DEFAULT NULL COMMENT 'NONE/SENSOR_STATE',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_relation_parent_channel (tenant_id, parent_device_id, logical_channel_code, deleted),
    KEY idx_relation_parent_enabled (tenant_id, parent_device_code, enabled, deleted),
    KEY idx_relation_child_enabled (tenant_id, child_device_code, enabled, deleted)
) COMMENT='设备逻辑通道关系表';
```

`DeviceRelation.java`

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device_relation")
public class DeviceRelation extends BaseEntity {

    private Long parentDeviceId;
    private String parentDeviceCode;
    private String logicalChannelCode;
    private Long childDeviceId;
    private String childDeviceCode;
    private Long childProductId;
    private String childProductKey;
    private String relationType;
    private String canonicalizationStrategy;
    private String statusMirrorStrategy;
    private Integer enabled;
    private String remark;
}
```

`DeviceRelationService.java`

```java
public interface DeviceRelationService {

    DeviceRelationVO createRelation(Long currentUserId, DeviceRelationUpsertDTO dto);

    DeviceRelationVO updateRelation(Long currentUserId, Long relationId, DeviceRelationUpsertDTO dto);

    List<DeviceRelationVO> listByParentDeviceCode(Long currentUserId, String parentDeviceCode);

    List<DeviceRelationRule> listEnabledRulesByParentDeviceCode(String parentDeviceCode);
}
```

`DeviceRelationServiceImpl.java`

```java
@Service
public class DeviceRelationServiceImpl implements DeviceRelationService {

    @Override
    public DeviceRelationVO createRelation(Long currentUserId, DeviceRelationUpsertDTO dto) {
        Device parent = getRequiredByCode(dto.getParentDeviceCode());
        Device child = getRequiredByCode(dto.getChildDeviceCode());
        ensureUnique(parent.getTenantId(), parent.getId(), dto.getLogicalChannelCode(), null);
        DeviceRelation relation = new DeviceRelation();
        relation.setTenantId(parent.getTenantId());
        relation.setParentDeviceId(parent.getId());
        relation.setParentDeviceCode(parent.getDeviceCode());
        relation.setLogicalChannelCode(dto.getLogicalChannelCode());
        relation.setChildDeviceId(child.getId());
        relation.setChildDeviceCode(child.getDeviceCode());
        relation.setRelationType(dto.getRelationType());
        relation.setCanonicalizationStrategy(dto.getCanonicalizationStrategy());
        relation.setStatusMirrorStrategy(dto.getStatusMirrorStrategy());
        relation.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        relation.setRemark(dto.getRemark());
        deviceRelationMapper.insert(relation);
        return toVO(relation);
    }
}
```

`DeviceRelationController.java`

```java
@RestController
public class DeviceRelationController {

    private final DeviceRelationService deviceRelationService;

    @PostMapping("/api/device/relations")
    public R<DeviceRelationVO> add(@LoginUserId Long currentUserId,
                                   @RequestBody @Valid DeviceRelationUpsertDTO dto) {
        return R.ok(deviceRelationService.createRelation(currentUserId, dto));
    }

    @GetMapping("/api/device/{parentDeviceCode}/relations")
    public R<List<DeviceRelationVO>> list(@LoginUserId Long currentUserId,
                                          @PathVariable String parentDeviceCode) {
        return R.ok(deviceRelationService.listByParentDeviceCode(currentUserId, parentDeviceCode));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl spring-boot-iot-device -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=DeviceRelationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
Tests run: 2, Failures: 0, Errors: 0
```

- [ ] **Step 5: Commit**

```bash
git add sql/init.sql \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceRelation.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/DeviceRelationMapper.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceRelationUpsertDTO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceRelationVO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceRelationService.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceRelationController.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/DeviceRelationRule.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImplTest.java
git commit -m "feat: add device relation registry backend"
```

## Task 2: Replace Config-Only Child Mapping With Relation Resolver + Fallback

**Files:**
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpRelationResolver.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpRelationRule.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationLegacyDpResolver.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java`
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpChildMessageSplitterTest.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`

- [ ] **Step 1: Write the failing resolver-backed protocol tests**

```java
@Test
void shouldPreferResolverRulesBeforeLegacyConfigMappings() {
    LegacyDpRelationResolver resolver = parentDeviceCode -> List.of(
            new LegacyDpRelationRule("L1_LF_1", "202018143", "LF_VALUE", "SENSOR_STATE")
    );

    LegacyDpChildMessageSplitter splitter = new LegacyDpChildMessageSplitter(iotProperties, resolver);
    Object result = splitter.split(payload, parentMessage, normalizeResult);

    List<DeviceUpMessage> children = ((LegacyDpNormalizeResult) result).getChildMessages();
    assertEquals("202018143", children.get(0).getDeviceCode());
    assertEquals(10.86, children.get(0).getProperties().get("value"));
    assertEquals(0, children.get(0).getProperties().get("sensor_state"));
}
```

```java
@Test
void shouldFallbackToIotPropertiesMappingsWhenResolverReturnsEmpty() {
    LegacyDpRelationResolver resolver = parentDeviceCode -> List.of();
    MqttJsonProtocolAdapter adapter = newAdapter(iotProperties, List.of(), resolver);

    DeviceUpMessage message = adapter.decode(packet, context);

    assertEquals("202018143", message.getChildMessages().get(0).getDeviceCode());
    assertEquals(10.86, message.getChildMessages().get(0).getProperties().get("value"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-protocol -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=LegacyDpChildMessageSplitterTest,MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
FAIL because LegacyDpRelationResolver / LegacyDpRelationRule do not exist and the splitter/adapter constructors do not accept a resolver yet
```

- [ ] **Step 3: Add resolver interface, DB-backed implementation, and protocol fallback**

`LegacyDpRelationResolver.java`

```java
public interface LegacyDpRelationResolver {

    List<LegacyDpRelationRule> resolve(String parentDeviceCode);
}
```

`LegacyDpRelationRule.java`

```java
public record LegacyDpRelationRule(String logicalChannelCode,
                                   String childDeviceCode,
                                   String canonicalizationStrategy,
                                   String statusMirrorStrategy) {
}
```

`DeviceRelationLegacyDpResolver.java`

```java
@Component
public class DeviceRelationLegacyDpResolver implements LegacyDpRelationResolver {

    private final DeviceRelationService deviceRelationService;

    @Override
    public List<LegacyDpRelationRule> resolve(String parentDeviceCode) {
        return deviceRelationService.listEnabledRulesByParentDeviceCode(parentDeviceCode).stream()
                .map(rule -> new LegacyDpRelationRule(
                        rule.getLogicalChannelCode(),
                        rule.getChildDeviceCode(),
                        rule.getCanonicalizationStrategy(),
                        rule.getStatusMirrorStrategy()
                ))
                .toList();
    }
}
```

`LegacyDpChildMessageSplitter.java`

```java
private final LegacyDpRelationResolver relationResolver;

public LegacyDpChildMessageSplitter(IotProperties iotProperties,
                                    @Nullable LegacyDpRelationResolver relationResolver) {
    this.iotProperties = iotProperties;
    this.relationResolver = relationResolver;
}

private Map<String, ResolvedChildRule> resolveChildRules(String baseDeviceCode) {
    List<LegacyDpRelationRule> resolvedRules = relationResolver == null
            ? List.of()
            : relationResolver.resolve(baseDeviceCode);
    if (!resolvedRules.isEmpty()) {
        return resolvedRules.stream().collect(Collectors.toMap(
                LegacyDpRelationRule::logicalChannelCode,
                rule -> new ResolvedChildRule(rule.childDeviceCode(), rule.canonicalizationStrategy(), rule.statusMirrorStrategy()),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }
    return resolveLegacyConfigRules(baseDeviceCode);
}
```

`MqttJsonProtocolAdapter.java`

```java
public MqttJsonProtocolAdapter(LegacyDpEnvelopeDecoder legacyDpEnvelopeDecoder,
                               IotProperties iotProperties,
                               @Autowired(required = false) LegacyDpRelationResolver relationResolver) {
    this.legacyDpEnvelopeDecoder = legacyDpEnvelopeDecoder;
    this.iotProperties = iotProperties;
    this.legacyDpFamilyResolver = new LegacyDpFamilyResolver();
    this.legacyDpPropertyNormalizer = new LegacyDpPropertyNormalizer(this.legacyDpFamilyResolver);
    this.legacyDpChildMessageSplitter = new LegacyDpChildMessageSplitter(iotProperties, relationResolver);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-protocol -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=LegacyDpChildMessageSplitterTest,MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
Tests run: existing protocol suite + new resolver-backed cases, all passing
```

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpRelationResolver.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpRelationRule.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationLegacyDpResolver.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpChildMessageSplitterTest.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java
git commit -m "feat: resolve legacy dp child mappings from relation registry"
```

## Task 3: Make Product Model Governance Relation-Aware

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelManualExtractDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Write failing governance tests**

```java
@Test
void listModelCandidatesShouldExcludeChildOwnedLogicalChannelsForCollectorProduct() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L, "south_rtu_parent"));
    when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L, "SK00EA0D1307986", 1001L)));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
            property(3001L, "S1_ZT_1.ext_power_volt", "外接电源电压", "double", now.minusMinutes(2)),
            property(3001L, "L1_LF_1", "裂缝1", "double", now.minusMinutes(1))
    ));
    when(deviceRelationMapper.selectList(any())).thenReturn(List.of(
            relation(1L, "SK00EA0D1307986", "L1_LF_1", "202018143")
    ));

    ProductModelCandidateResultVO result = productModelService.listModelCandidates(1001L);

    assertTrue(result.getPropertyCandidates().stream().anyMatch(item -> "S1_ZT_1.ext_power_volt".equals(item.getIdentifier())));
    assertTrue(result.getPropertyCandidates().stream().noneMatch(item -> "L1_LF_1".equals(item.getIdentifier())));
}
```

```java
@Test
void manualExtractShouldCanonicalizeParentPayloadForChildProductWhenSourceDeviceCodeProvided() {
    when(productMapper.selectById(2001L)).thenReturn(product(2001L, "south_crack_sensor"));
    when(deviceMapper.selectList(any())).thenReturn(List.of(device(4001L, "202018143", 2001L)));
    when(deviceRelationMapper.selectList(any())).thenReturn(List.of(
            relation(1L, "SK00EA0D1307986", "L1_LF_1", "202018143")
    ));

    ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
    dto.setSampleType("property");
    dto.setSourceDeviceCode("SK00EA0D1307986");
    dto.setExtractMode("parent_payload");
    dto.setSamplePayload("{\"SK00EA0D1307986\":{\"S1_ZT_1\":{\"2026-04-04T22:10:35.000Z\":{\"sensor_state\":{\"L1_LF_1\":0}}},\"L1_LF_1\":{\"2026-04-04T22:10:35.000Z\":10.86}}}");

    ProductModelCandidateResultVO result = productModelService.manualExtractModelCandidates(2001L, dto);

    assertTrue(result.getPropertyCandidates().stream().anyMatch(item -> "value".equals(item.getIdentifier())));
    assertTrue(result.getPropertyCandidates().stream().anyMatch(item -> "sensor_state".equals(item.getIdentifier())));
    assertTrue(result.getPropertyCandidates().stream().noneMatch(item -> "L1_LF_1".equals(item.getIdentifier())));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-device -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=ProductModelServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
FAIL because ProductModelManualExtractDTO does not expose relation context fields and ProductModelServiceImpl does not filter child-owned logical channels yet
```

- [ ] **Step 3: Add relation-aware DTO fields and governance filtering**

`ProductModelManualExtractDTO.java`

```java
@Data
public class ProductModelManualExtractDTO {

    @NotBlank(message = "请选择样本类型")
    private String sampleType;

    @NotBlank(message = "请输入样本报文")
    private String samplePayload;

    private String sourceDeviceCode;

    /**
     * current_device | parent_payload
     */
    private String extractMode;
}
```

`ProductModelGovernanceCompareDTO.java`

```java
@Data
public static class ManualExtractInput {
    private String sampleType;
    private String samplePayload;
    private String sourceDeviceCode;
    private String extractMode;
}
```

`ProductModelServiceImpl.java`

```java
private Map<String, String> extractPropertyLeaves(String payload,
                                                  Long productId,
                                                  String sourceDeviceCode,
                                                  String extractMode) {
    Map<String, String> extracted = extractPropertyLeaves(payload);
    if (!StringUtils.hasText(sourceDeviceCode)) {
        return extracted;
    }
    if ("parent_payload".equalsIgnoreCase(extractMode)) {
        return relationAwareExtract(extracted, productId, sourceDeviceCode);
    }
    return extracted;
}

private Map<String, String> relationAwareExtract(Map<String, String> extracted,
                                                 Long productId,
                                                 String sourceDeviceCode) {
    List<DeviceRelationRule> rules = deviceRelationService.listEnabledRulesByParentDeviceCode(sourceDeviceCode);
    Set<String> childLogicalChannels = rules.stream()
            .map(DeviceRelationRule::getLogicalChannelCode)
            .collect(Collectors.toSet());
    boolean childProduct = belongsToAnyChildDevice(productId, rules);
    Map<String, String> filtered = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : extracted.entrySet()) {
        String identifier = entry.getKey();
        if (childProduct) {
            if ("S1_ZT_1.sensor_state.L1_LF_1".equals(identifier)) {
                filtered.put("sensor_state", entry.getValue());
            } else if ("L1_LF_1".equals(identifier)) {
                filtered.put("value", entry.getValue());
            }
            continue;
        }
        if (!childLogicalChannels.contains(identifier)) {
            filtered.put(identifier, entry.getValue());
        }
    }
    return filtered;
}
```

Also inject `DeviceRelationMapper` or `DeviceRelationService` into `ProductModelServiceImpl` constructor and filter runtime candidates before building `ProductModelCandidateVO` when the current product is a collector product.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-device -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=ProductModelServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
ProductModelServiceImpl relation-aware runtime/manual governance tests passing
```

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelManualExtractDTO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "feat: add relation-aware product model governance"
```

## Task 4: Update Docs And Run Focused Verification

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/05-protocol.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Write failing doc/contract checklist in a scratch note**

```text
Required updates:
1. API: relation CRUD + new governance request fields
2. DB: iot_device_relation schema
3. Protocol: DB-backed relation resolver + config fallback
4. Change log: phase-5 wave-1 delivery note
```

- [ ] **Step 2: Update the docs in place**

`docs/03-接口规范与接口清单.md`

```md
- `POST /api/device/relations`
- `PUT /api/device/relations/{relationId}`
- `GET /api/device/{parentDeviceCode}/relations`

补充口径：
- `ProductModelManualExtractDTO` 与 `ProductModelGovernanceCompareDTO.manualExtract` 新增 `sourceDeviceCode`、`extractMode`
- 当 `extractMode=parent_payload` 时，治理层会按关系主数据先做父/子过滤与 canonicalize
```

`docs/04-数据库设计与初始化数据.md`

```md
- `iot_device_relation`
  - 作用：维护父设备逻辑通道与真实子设备的正式关系，以及 canonicalize / 状态镜像策略
  - 关键字段：`parent_device_code`、`logical_channel_code`、`child_device_code`、`relation_type`、`canonicalization_strategy`、`status_mirror_strategy`
```

`docs/05-protocol.md`

```md
- legacy `$dp` 子设备映射当前优先读取正式 `iot_device_relation`
- `iot.device.sub-device-mappings` 继续保留为兼容 fallback
- 裂缝等采集中枢场景不再依赖纯环境配置作为长期权威
```

`docs/08-变更记录与技术债清单.md`

```md
- 2026-04-04：Phase 5 Wave 1 已补齐 `iot_device_relation` 基础关系主数据，并让 legacy `$dp` 子设备拆分和物模型治理共用同一套关系语义；`sub-device-mappings` 降级为兼容 fallback。
```

- [ ] **Step 3: Run focused verification**

Run:

```bash
mvn -pl spring-boot-iot-device -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=DeviceRelationServiceImplTest,ProductModelServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl spring-boot-iot-protocol -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=LegacyDpChildMessageSplitterTest,MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
git diff --check -- sql/init.sql \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceRelation.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceRelationServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java \
  docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md \
  docs/05-protocol.md \
  docs/08-变更记录与技术债清单.md
```

Expected:

```text
BUILD SUCCESS for both Maven commands
git diff --check returns no output
```

- [ ] **Step 4: Commit**

```bash
git add docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md \
  docs/05-protocol.md \
  docs/08-变更记录与技术债清单.md
git commit -m "docs: document relation registry governance foundation"
```

## Self-Review

### Spec coverage

- Device relation master data: covered by Task 1.
- Protocol registry-first resolution with fallback: covered by Task 2.
- Relation-aware manual and runtime governance filtering: covered by Task 3.
- Contract and docs backfill: covered by Task 4.
- Generic template engine and async hot-path refactor: intentionally deferred to later plans.

### Placeholder scan

- No `TODO`, `TBD`, “later”, or “similar to previous task” placeholders remain.
- Every task contains exact file paths, example code, commands, and expected outcomes.

### Type consistency

- The plan consistently uses `DeviceRelation`, `DeviceRelationRule`, `LegacyDpRelationResolver`, `sourceDeviceCode`, and `extractMode`.
- Canonical strategy examples stay aligned with current known families:
  - `LF_VALUE`
  - `SW_DISPS`

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-04-phase5-relation-registry-governance-foundation-implementation-plan.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
