# Help Doc And Channel Dict Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract `help_doc_category` and `notification_channel_type` into authoritative `sys_dict` data, then make the shared environment seed/sync path, backend write validation, and the management pages all read from the same dictionary semantics.

**Architecture:** Keep the source of truth in `sql/init-data.sql` plus `scripts/run-real-env-schema-sync.py`, not in frontend-only option constants. Backend write paths should validate against the active dict values with fixed fallback codes, while frontend management pages should load dict-backed options at runtime and fall back to the existing constant list if the dict API is unavailable.

**Tech Stack:** MySQL seed SQL, Python 3 `unittest`, Spring Boot 4, MyBatis-Plus, Vue 3, Vitest

---

**Scope note:** This plan intentionally covers the low-coupling first wave only:
- `help_doc_category`
- `notification_channel_type`

`in_app_message_type / priority / target_type / source_type` need a separate follow-up plan because they are tied to unread-bridge thresholds, automatic source restrictions, and default expire-time logic.

### Task 1: Seed And Sync The Two New Governance Dicts

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `scripts/tests/test_dict_seed_snapshot.py`

- [ ] **Step 1: Write the failing Python snapshot and schema-sync tests**

Extend `scripts/tests/test_run_real_env_schema_sync.py` with dict target coverage for the two new dict codes:

```python
class GovernanceDictTargetDefinitionTest(unittest.TestCase):
    def test_help_doc_category_targets_match_three_business_categories(self):
        targets = schema_sync.system_governance_dict_targets()["help_doc_category"]["target_items"]
        self.assertEqual(
            [item[0] for item in targets],
            ["business", "technical", "faq"],
        )
        self.assertEqual(
            [item[1] for item in targets],
            ["业务类", "技术类", "常见问题"],
        )

    def test_notification_channel_type_targets_keep_six_governed_values(self):
        targets = schema_sync.system_governance_dict_targets()["notification_channel_type"]["target_items"]
        self.assertEqual(
            [item[0] for item in targets],
            ["email", "sms", "webhook", "wechat", "feishu", "dingtalk"],
        )
        self.assertEqual(targets[2][1], "Webhook")
        self.assertEqual(targets[5][1], "钉钉")
```

Extend `scripts/tests/test_dict_seed_snapshot.py` with seed assertions for the same dicts:

```python
    def test_seed_contains_help_doc_and_channel_type_dicts(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("'help_doc_category'", content)
        self.assertIn("'business'", content)
        self.assertIn("'technical'", content)
        self.assertIn("'faq'", content)
        self.assertIn("'notification_channel_type'", content)
        self.assertIn("'webhook'", content)
        self.assertIn("'dingtalk'", content)

    def test_seed_soft_deletes_non_target_help_doc_and_channel_type_items(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("dict_code IN ('risk_point_level', 'alarm_level', 'risk_level', 'help_doc_category', 'notification_channel_type')", content)
        self.assertIn("dict_id = 7204 AND item_value NOT IN ('business', 'technical', 'faq')", content)
        self.assertIn("dict_id = 7205 AND item_value NOT IN ('email', 'sms', 'webhook', 'wechat', 'feishu', 'dingtalk')", content)
```

- [ ] **Step 2: Run the Python tests to verify they fail**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync scripts.tests.test_dict_seed_snapshot
```

Expected:
- FAIL because `system_governance_dict_targets()` does not exist yet.
- FAIL because `sql/init-data.sql` does not yet contain `help_doc_category` or `notification_channel_type`.

- [ ] **Step 3: Add the new dict seeds and historical-environment sync logic**

Refactor `scripts/run-real-env-schema-sync.py` so the new dicts live in a dedicated helper and are aligned through the existing dict-pruning path:

```python
def system_governance_dict_targets() -> Dict[str, Dict[str, object]]:
    return {
        "help_doc_category": {
            "dict_name": "帮助文档分类",
            "sort_no": 4,
            "dict_remark": "帮助文档分类字典",
            "preferred_dict_id": 7204,
            "target_items": [
                ("business", "业务类", 1, "帮助文档分类-业务类", [], 7312),
                ("technical", "技术类", 2, "帮助文档分类-技术类", [], 7313),
                ("faq", "常见问题", 3, "帮助文档分类-常见问题", [], 7314),
            ],
        },
        "notification_channel_type": {
            "dict_name": "通知渠道类型",
            "sort_no": 5,
            "dict_remark": "通知渠道类型字典",
            "preferred_dict_id": 7205,
            "target_items": [
                ("email", "邮件", 1, "通知渠道类型-邮件", [], 7315),
                ("sms", "短信", 2, "通知渠道类型-短信", [], 7316),
                ("webhook", "Webhook", 3, "通知渠道类型-Webhook", [], 7317),
                ("wechat", "微信", 4, "通知渠道类型-微信", [], 7318),
                ("feishu", "飞书", 5, "通知渠道类型-飞书", [], 7319),
                ("dingtalk", "钉钉", 6, "通知渠道类型-钉钉", [], 7320),
            ],
        },
    }


def ensure_system_governance_dicts(cur: pymysql.cursors.Cursor, db: str) -> None:
    for dict_code, definition in system_governance_dict_targets().items():
        ensure_level_dict(cur, db, dict_code=dict_code, **definition)
```

Call the new helper from `main()` after the existing risk/alarm dict alignment:

```python
            ensure_level_dicts(cur, args.db)
            ensure_system_governance_dicts(cur, args.db)
            migrate_level_values(cur, args.db)
```

Add the authoritative rows to `sql/init-data.sql`:

```sql
INSERT INTO sys_dict (
    id, tenant_id, dict_name, dict_code, dict_type, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7204, 1, '帮助文档分类', 'help_doc_category', 'text', 1, 4, '帮助文档分类字典', 1, NOW(), 1, NOW(), 0),
    (7205, 1, '通知渠道类型', 'notification_channel_type', 'text', 1, 5, '通知渠道类型字典', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    dict_name = VALUES(dict_name),
    dict_type = VALUES(dict_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

UPDATE sys_dict
SET status = 0,
    deleted = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_code IN ('risk_point_level', 'alarm_level', 'risk_level', 'help_doc_category', 'notification_channel_type')
  AND id NOT IN (7201, 7202, 7203, 7204, 7205);

INSERT INTO sys_dict_item (
    id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7312, 1, 7204, '业务类', 'business', 'string', 1, 1, '帮助文档分类-业务类', 1, NOW(), 1, NOW(), 0),
    (7313, 1, 7204, '技术类', 'technical', 'string', 1, 2, '帮助文档分类-技术类', 1, NOW(), 1, NOW(), 0),
    (7314, 1, 7204, '常见问题', 'faq', 'string', 1, 3, '帮助文档分类-常见问题', 1, NOW(), 1, NOW(), 0),
    (7315, 1, 7205, '邮件', 'email', 'string', 1, 1, '通知渠道类型-邮件', 1, NOW(), 1, NOW(), 0),
    (7316, 1, 7205, '短信', 'sms', 'string', 1, 2, '通知渠道类型-短信', 1, NOW(), 1, NOW(), 0),
    (7317, 1, 7205, 'Webhook', 'webhook', 'string', 1, 3, '通知渠道类型-Webhook', 1, NOW(), 1, NOW(), 0),
    (7318, 1, 7205, '微信', 'wechat', 'string', 1, 4, '通知渠道类型-微信', 1, NOW(), 1, NOW(), 0),
    (7319, 1, 7205, '飞书', 'feishu', 'string', 1, 5, '通知渠道类型-飞书', 1, NOW(), 1, NOW(), 0),
    (7320, 1, 7205, '钉钉', 'dingtalk', 'string', 1, 6, '通知渠道类型-钉钉', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    dict_id = VALUES(dict_id),
    item_name = VALUES(item_name),
    item_value = VALUES(item_value),
    item_type = VALUES(item_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

UPDATE sys_dict_item
SET status = 0,
    deleted = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND (
      (dict_id = 7201 AND item_value NOT IN ('level_1', 'level_2', 'level_3'))
      OR (dict_id = 7202 AND item_value NOT IN ('red', 'orange', 'yellow', 'blue'))
      OR (dict_id = 7203 AND item_value NOT IN ('red', 'orange', 'yellow', 'blue'))
      OR (dict_id = 7204 AND item_value NOT IN ('business', 'technical', 'faq'))
      OR (dict_id = 7205 AND item_value NOT IN ('email', 'sms', 'webhook', 'wechat', 'feishu', 'dingtalk'))
  );
```

- [ ] **Step 4: Run the Python tests to verify they pass**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync scripts.tests.test_dict_seed_snapshot
```

Expected:
- PASS
- The seed file and the historical-environment sync script now share the same dict scope for `help_doc_category` and `notification_channel_type`.

- [ ] **Step 5: Commit the seed/sync slice**

Run:

```bash
git add sql/init-data.sql scripts/run-real-env-schema-sync.py scripts/tests/test_run_real_env_schema_sync.py scripts/tests/test_dict_seed_snapshot.py
git commit -m "feat: seed help doc and channel type dicts"
```

### Task 2: Add Backend Dict-Backed Validation With Fixed Fallback Codes

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/SystemDictValueSupport.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/SystemDictValueSupportTest.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/NotificationChannelServiceImpl.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImplTest.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/NotificationChannelServiceImplTest.java`

- [ ] **Step 1: Write the failing backend tests**

Create `SystemDictValueSupportTest.java`:

```java
@ExtendWith(MockitoExtension.class)
class SystemDictValueSupportTest {

    @Mock
    private DictService dictService;

    private SystemDictValueSupport systemDictValueSupport;

    @BeforeEach
    void setUp() {
        systemDictValueSupport = new SystemDictValueSupport(dictService);
    }

    @Test
    void shouldFallbackToDefaultValuesWhenDictMissing() {
        when(dictService.getByCode(99L, "help_doc_category")).thenReturn(null);

        String normalized = systemDictValueSupport.normalizeRequiredLowerCase(
                99L,
                "help_doc_category",
                "BUSINESS",
                "文档分类",
                Set.of("business", "technical", "faq")
        );

        assertEquals("business", normalized);
    }

    @Test
    void shouldRejectValueOutsideDictAndFallback() {
        when(dictService.getByCode(99L, "help_doc_category")).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () ->
                systemDictValueSupport.normalizeRequiredLowerCase(
                        99L,
                        "help_doc_category",
                        "whitepaper",
                        "文档分类",
                        Set.of("business", "technical", "faq")
                ));

        assertEquals("文档分类不合法", exception.getMessage());
    }
}
```

Extend `NotificationChannelServiceImplTest.java` with invalid-type coverage:

```java
    @Test
    void shouldRejectUnknownChannelTypeWhenAddingChannel() {
        NotificationChannel channel = new NotificationChannel();
        channel.setTenantId(1L);
        channel.setChannelCode("bad-type");
        channel.setChannelName("坏渠道");
        channel.setChannelType("slack");
        channel.setConfig("{\"url\":\"https://example.com\"}");

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> notificationChannelService.addChannel(99L, channel));
        assertEquals("渠道类型不合法", exception.getMessage());
    }
```

Extend `HelpDocumentServiceImplTest.java` with invalid-category coverage that still has to pass after the hard-coded set is removed:

```java
    @Test
    void shouldRejectUnknownHelpDocumentCategory() {
        HelpDocument document = new HelpDocument();
        document.setTitle("帮助文档");
        document.setContent("正文");
        document.setDocCategory("whitepaper");

        BizException exception = assertThrows(BizException.class, () -> helpDocumentService.addDocument(document, 1L));
        assertEquals("文档分类不合法", exception.getMessage());
    }
```

- [ ] **Step 2: Run the backend tests to verify they fail**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-system -Dtest=SystemDictValueSupportTest,HelpDocumentServiceImplTest,NotificationChannelServiceImplTest test
```

Expected:
- FAIL because `SystemDictValueSupport` does not exist yet.
- FAIL because `NotificationChannelServiceImpl` still accepts arbitrary `channelType`.

- [ ] **Step 3: Implement the dict-backed validator and wire it into the two services**

Create `SystemDictValueSupport.java`:

```java
@Service
public class SystemDictValueSupport {

    private final DictService dictService;

    public SystemDictValueSupport(DictService dictService) {
        this.dictService = dictService;
    }

    public String normalizeRequiredLowerCase(Long currentUserId,
                                             String dictCode,
                                             String rawValue,
                                             String fieldName,
                                             Set<String> fallbackValues) {
        String normalized = StringUtils.hasText(rawValue) ? rawValue.trim().toLowerCase(Locale.ROOT) : null;
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(fieldName + "不能为空");
        }
        if (!resolveAllowedValues(currentUserId, dictCode, fallbackValues).contains(normalized)) {
            throw new BizException(fieldName + "不合法");
        }
        return normalized;
    }

    private Set<String> resolveAllowedValues(Long currentUserId,
                                             String dictCode,
                                             Set<String> fallbackValues) {
        Dict dict = dictService.getByCode(currentUserId, dictCode);
        if (dict == null || dict.getItems() == null || dict.getItems().isEmpty()) {
            return fallbackValues;
        }
        Set<String> dictValues = dict.getItems().stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .filter(item -> !Integer.valueOf(0).equals(item.getStatus()))
                .map(DictItem::getItemValue)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return dictValues.isEmpty() ? fallbackValues : dictValues;
    }
}
```

Replace the hard-coded help-doc category set with the helper:

```java
private static final String HELP_DOC_CATEGORY_DICT_CODE = "help_doc_category";
private static final Set<String> DEFAULT_HELP_DOC_CATEGORIES = Set.of("business", "technical", "faq");

private final SystemDictValueSupport systemDictValueSupport;

public HelpDocumentServiceImpl(HelpDocumentMapper helpDocumentMapper,
                               PermissionService permissionService,
                               SystemContentSchemaSupport systemContentSchemaSupport,
                               SystemDictValueSupport systemDictValueSupport) {
    this.helpDocumentMapper = helpDocumentMapper;
    this.permissionService = permissionService;
    this.systemContentSchemaSupport = systemContentSchemaSupport;
    this.systemDictValueSupport = systemDictValueSupport;
}

private void normalizeAndValidateDocument(HelpDocument document, HelpDocument existing, Long operatorId) {
    if (document == null) {
        throw new BizException("帮助文档不能为空");
    }
    document.setTenantId(existing == null ? defaultTenantId(document.getTenantId()) : existing.getTenantId());
    document.setDocCategory(systemDictValueSupport.normalizeRequiredLowerCase(
            operatorId,
            HELP_DOC_CATEGORY_DICT_CODE,
            document.getDocCategory(),
            "文档分类",
            DEFAULT_HELP_DOC_CATEGORIES
    ));
    document.setTitle(requireText(document.getTitle(), "文档标题"));
    document.setSummary(nullableText(document.getSummary()));
    document.setContent(requireText(document.getContent(), "文档正文"));
    document.setKeywords(SystemContentAccessSupport.normalizeCsv(document.getKeywords()));
    document.setRelatedPaths(SystemContentAccessSupport.normalizePathCsv(document.getRelatedPaths()));
    document.setVisibleRoleCodes(SystemContentAccessSupport.normalizeUpperCaseCsv(document.getVisibleRoleCodes()));
    if (document.getStatus() == null) {
        document.setStatus(existing == null ? 1 : existing.getStatus());
    }
    if (document.getSortNo() == null) {
        document.setSortNo(existing == null ? 0 : existing.getSortNo());
    }
}
```

Add the missing channel-type validation:

```java
private static final String CHANNEL_TYPE_DICT_CODE = "notification_channel_type";
private static final Set<String> DEFAULT_CHANNEL_TYPES = Set.of("email", "sms", "webhook", "wechat", "feishu", "dingtalk");

private final PermissionService permissionService;
private final SystemDictValueSupport systemDictValueSupport;

public NotificationChannelServiceImpl(PermissionService permissionService,
                                      SystemDictValueSupport systemDictValueSupport) {
    this.permissionService = permissionService;
    this.systemDictValueSupport = systemDictValueSupport;
}

@Override
@Transactional(rollbackFor = Exception.class)
public NotificationChannel addChannel(Long currentUserId, NotificationChannel channel) {
    Long tenantId = resolveTenantId(currentUserId, channel.getTenantId());
    LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(NotificationChannel::getTenantId, tenantId)
            .eq(NotificationChannel::getChannelCode, channel.getChannelCode())
            .eq(NotificationChannel::getDeleted, 0);
    if (this.count(queryWrapper) > 0) {
        throw new BizException("渠道编码已存在");
    }

    channel.setChannelType(systemDictValueSupport.normalizeRequiredLowerCase(
            currentUserId,
            CHANNEL_TYPE_DICT_CODE,
            channel.getChannelType(),
            "渠道类型",
            DEFAULT_CHANNEL_TYPES
    ));
    if (channel.getSortNo() == null) {
        channel.setSortNo(0);
    }
    if (channel.getStatus() == null) {
        channel.setStatus(1);
    }
    channel.setTenantId(tenantId == null ? 1L : tenantId);
    if (channel.getCreateBy() == null) {
        channel.setCreateBy(currentUserId == null ? 1L : currentUserId);
    }
    this.save(channel);
    return channel;
}
```

Normalize the update and query filter path too:

```java
if (StringUtils.hasText(channelType)) {
    queryWrapper.eq(NotificationChannel::getChannelType, channelType.trim().toLowerCase(Locale.ROOT));
}
```

- [ ] **Step 4: Run the backend tests to verify they pass**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-system -Dtest=SystemDictValueSupportTest,HelpDocumentServiceImplTest,NotificationChannelServiceImplTest test
```

Expected:
- PASS
- Both write paths reject invalid business codes even if the frontend is bypassed.

- [ ] **Step 5: Commit the backend validation slice**

Run:

```bash
git add spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/SystemDictValueSupport.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImpl.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/NotificationChannelServiceImpl.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/SystemDictValueSupportTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImplTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/NotificationChannelServiceImplTest.java
git commit -m "feat: validate help doc and channel types via dicts"
```

### Task 3: Make The Management Pages Load Dict-Backed Options At Runtime

**Files:**
- Modify: `spring-boot-iot-ui/src/api/helpDoc.ts`
- Modify: `spring-boot-iot-ui/src/api/channel.ts`
- Modify: `spring-boot-iot-ui/src/views/HelpDocView.vue`
- Modify: `spring-boot-iot-ui/src/views/ChannelView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/HelpDocView.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/views/ChannelView.test.ts`

- [ ] **Step 1: Write the failing Vitest coverage**

Update `HelpDocView.test.ts` so the view must call a dict-backed fetch function:

```ts
const { mockPageHelpDocuments, mockListRoles, mockFetchHelpDocCategoryOptions } = vi.hoisted(() => ({
  mockPageHelpDocuments: vi.fn(),
  mockListRoles: vi.fn(),
  mockFetchHelpDocCategoryOptions: vi.fn()
}))

vi.mock('@/api/helpDoc', () => ({
  HELP_DOC_CATEGORY_OPTIONS: [
    { label: '业务类', value: 'business' },
    { label: '技术类', value: 'technical' },
    { label: '常见问题', value: 'faq' }
  ],
  fetchHelpDocCategoryOptions: mockFetchHelpDocCategoryOptions,
  pageHelpDocuments: mockPageHelpDocuments,
  getHelpDocument: vi.fn(),
  addHelpDocument: vi.fn(),
  updateHelpDocument: vi.fn(),
  deleteHelpDocument: vi.fn()
}))

beforeEach(() => {
  mockFetchHelpDocCategoryOptions.mockReset()
  mockFetchHelpDocCategoryOptions.mockResolvedValue([
    { label: '业务类', value: 'business', sortNo: 1 },
    { label: '技术类', value: 'technical', sortNo: 2 },
    { label: '常见问题', value: 'faq', sortNo: 3 }
  ])
})

it('loads dict-backed help doc category options on mount', async () => {
  mountView()
  await flushPromises()
  expect(mockFetchHelpDocCategoryOptions).toHaveBeenCalledTimes(1)
})
```

Create `ChannelView.test.ts` with the same runtime-loading expectation:

```ts
import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ChannelView from '@/views/ChannelView.vue'

const { mockPageChannels, mockFetchChannelTypeOptions } = vi.hoisted(() => ({
  mockPageChannels: vi.fn(),
  mockFetchChannelTypeOptions: vi.fn()
}))

vi.mock('@/api/channel', () => ({
  CHANNEL_TYPES: [
    { label: '邮件', value: 'email' },
    { label: '短信', value: 'sms' },
    { label: 'Webhook', value: 'webhook' },
    { label: '微信', value: 'wechat' },
    { label: '飞书', value: 'feishu' },
    { label: '钉钉', value: 'dingtalk' }
  ],
  fetchChannelTypeOptions: mockFetchChannelTypeOptions,
  pageChannels: mockPageChannels,
  getChannelByCode: vi.fn(),
  addChannel: vi.fn(),
  updateChannel: vi.fn(),
  deleteChannel: vi.fn(),
  testChannel: vi.fn()
}))

describe('ChannelView', () => {
  beforeEach(() => {
    mockFetchChannelTypeOptions.mockReset()
    mockFetchChannelTypeOptions.mockResolvedValue([
      { label: 'Webhook', value: 'webhook', sortNo: 3 },
      { label: '微信', value: 'wechat', sortNo: 4 }
    ])
    mockPageChannels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
    })
  })

  it('loads dict-backed channel type options on mount', async () => {
    mount(ChannelView, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          StandardWorkbenchPanel: defineComponent({ template: '<div><slot name="filters" /><slot /></div>' }),
          StandardListFilterHeader: defineComponent({ template: '<div><slot name="primary" /><slot name="actions" /></div>' }),
          StandardTableToolbar: true,
          StandardButton: true,
          StandardAppliedFiltersBar: true,
          StandardPagination: true,
          StandardTableTextColumn: true,
          StandardWorkbenchRowActions: true,
          StandardFormDrawer: true,
          StandardDrawerFooter: true,
          CsvColumnSettingDialog: true,
          EmptyState: true,
          ElTable: true,
          ElTableColumn: true,
          ElForm: true,
          ElFormItem: true,
          ElInput: true,
          ElSelect: true,
          ElOption: true,
          ElTag: true,
          ElInputNumber: true,
          ElRadioGroup: true,
          ElRadio: true
        }
      }
    })
    await nextTick()
    await Promise.resolve()
    expect(mockFetchChannelTypeOptions).toHaveBeenCalledTimes(1)
  })
})
```

- [ ] **Step 2: Run the frontend tests to verify they fail**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- src/__tests__/views/HelpDocView.test.ts src/__tests__/views/ChannelView.test.ts
```

Expected:
- FAIL because neither view calls `fetchHelpDocCategoryOptions()` or `fetchChannelTypeOptions()` yet.

- [ ] **Step 3: Add option builders/fetchers and switch the two views to runtime dict loading**

Add dict-backed helpers to `spring-boot-iot-ui/src/api/helpDoc.ts`:

```ts
import { getDictByCode, type DictItem } from './dict'

export interface HelpDocCategoryOption {
  label: string
  value: string
  sortNo: number
}

export const HELP_DOC_CATEGORY_OPTIONS: HelpDocCategoryOption[] = [
  { value: 'business', label: '业务类', sortNo: 1 },
  { value: 'technical', label: '技术类', sortNo: 2 },
  { value: 'faq', label: '常见问题', sortNo: 3 }
]

export function buildHelpDocCategoryOptions(items: Partial<DictItem>[] = []) {
  const normalized = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || String(item.itemValue || ''),
      value: String(item.itemValue || '').trim().toLowerCase(),
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const unique = new Map<string, HelpDocCategoryOption>()
  normalized
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!unique.has(item.value)) {
        unique.set(item.value, item)
      }
    })

  return unique.size > 0 ? Array.from(unique.values()) : HELP_DOC_CATEGORY_OPTIONS.map((item) => ({ ...item }))
}

export async function fetchHelpDocCategoryOptions() {
  try {
    const response = await getDictByCode('help_doc_category')
    return response.code === 200
      ? buildHelpDocCategoryOptions(response.data?.items || [])
      : HELP_DOC_CATEGORY_OPTIONS.map((item) => ({ ...item }))
  } catch {
    return HELP_DOC_CATEGORY_OPTIONS.map((item) => ({ ...item }))
  }
}
```

Add the matching helper to `spring-boot-iot-ui/src/api/channel.ts`:

```ts
import { getDictByCode, type DictItem } from './dict'

export interface ChannelTypeOption {
  label: string
  value: string
  sortNo: number
}

export const CHANNEL_TYPES: ChannelTypeOption[] = [
  { value: 'email', label: '邮件', sortNo: 1 },
  { value: 'sms', label: '短信', sortNo: 2 },
  { value: 'webhook', label: 'Webhook', sortNo: 3 },
  { value: 'wechat', label: '微信', sortNo: 4 },
  { value: 'feishu', label: '飞书', sortNo: 5 },
  { value: 'dingtalk', label: '钉钉', sortNo: 6 }
]

export function buildChannelTypeOptions(items: Partial<DictItem>[] = []) {
  const normalized = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || String(item.itemValue || ''),
      value: String(item.itemValue || '').trim().toLowerCase(),
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const unique = new Map<string, ChannelTypeOption>()
  normalized
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!unique.has(item.value)) {
        unique.set(item.value, item)
      }
    })

  return unique.size > 0 ? Array.from(unique.values()) : CHANNEL_TYPES.map((item) => ({ ...item }))
}

export async function fetchChannelTypeOptions() {
  try {
    const response = await getDictByCode('notification_channel_type')
    return response.code === 200
      ? buildChannelTypeOptions(response.data?.items || [])
      : CHANNEL_TYPES.map((item) => ({ ...item }))
  } catch {
    return CHANNEL_TYPES.map((item) => ({ ...item }))
  }
}
```

Wire `HelpDocView.vue` to the fetched options:

```ts
import { HELP_DOC_CATEGORY_OPTIONS, fetchHelpDocCategoryOptions, pageHelpDocuments } from '@/api/helpDoc'

const categoryOptions = ref(HELP_DOC_CATEGORY_OPTIONS.map((item) => ({ ...item })))

async function loadHelpDocCategoryOptions() {
  categoryOptions.value = await fetchHelpDocCategoryOptions()
}

function getCategoryLabel(value?: string) {
  return categoryOptions.value.find((item) => item.value === value)?.label || '--'
}

onMounted(async () => {
  await Promise.all([loadHelpDocCategoryOptions(), getTableData(), loadRoles()])
})
```

Update the template loops from `HELP_DOC_CATEGORY_OPTIONS` to `categoryOptions`.

Wire `ChannelView.vue` to the fetched channel-type options while keeping the fixed runtime subset for test/send actions:

```ts
import { CHANNEL_TYPES, fetchChannelTypeOptions, pageChannels } from '@/api/channel'

const channelTypeOptions = ref(CHANNEL_TYPES.map((item) => ({ ...item })))
const TESTABLE_CHANNEL_TYPES = ['webhook', 'wechat', 'feishu', 'dingtalk']

async function loadChannelTypeOptions() {
  channelTypeOptions.value = await fetchChannelTypeOptions()
}

const getChannelTypeName = (type: string) => {
  const normalized = String(type || '').trim().toLowerCase()
  const matched = channelTypeOptions.value.find((item) => item.value === normalized)
  return matched?.label || normalized
}

onMounted(async () => {
  await Promise.all([loadChannelTypeOptions(), getTableData()])
})
```

Update the template loops from `CHANNEL_TYPES` to `channelTypeOptions`.

- [ ] **Step 4: Run the frontend tests to verify they pass**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- src/__tests__/views/HelpDocView.test.ts src/__tests__/views/ChannelView.test.ts
```

Expected:
- PASS
- The two management pages now source option labels from dicts at runtime and fall back to the previous defaults if the dict API is unavailable.

- [ ] **Step 5: Commit the frontend slice**

Run:

```bash
git add spring-boot-iot-ui/src/api/helpDoc.ts spring-boot-iot-ui/src/api/channel.ts spring-boot-iot-ui/src/views/HelpDocView.vue spring-boot-iot-ui/src/views/ChannelView.vue spring-boot-iot-ui/src/__tests__/views/HelpDocView.test.ts spring-boot-iot-ui/src/__tests__/views/ChannelView.test.ts
git commit -m "feat: load help doc and channel options from dicts"
```

### Task 4: Update Docs And Record The New Dictionary Baseline

**Files:**
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update the database/init-data documentation**

Add a short governance-dict note to `docs/04-数据库设计与初始化数据.md` near the `sys_dict` / `sys_dict_item` description:

```md
- `sys_dict` / `sys_dict_item` 当前还承载系统治理域的权威业务选项，至少包括：
  - `risk_point_level`：`level_1 / level_2 / level_3`
  - `alarm_level`：`red / orange / yellow / blue`
  - `risk_level`：`red / orange / yellow / blue`
  - `help_doc_category`：`business / technical / faq`
  - `notification_channel_type`：`email / sms / webhook / wechat / feishu / dingtalk`
- `sql/init-data.sql` 与 `scripts/run-real-env-schema-sync.py` 必须对上述字典保持同一权威口径；前端页面不再单独持有帮助文档分类和通知渠道类型的最终事实来源。
```

- [ ] **Step 2: Update the change log / technical debt record**

Append one entry to `docs/08-变更记录与技术债清单.md`:

```md
### 2026-04-03 帮助文档分类与通知渠道类型字典收口

- `help_doc_category` 与 `notification_channel_type` 已补齐到 `sql/init-data.sql` 和 `scripts/run-real-env-schema-sync.py` 的同一权威基线。
- 帮助文档管理与通知编排页改为优先通过 `/api/dict/code/{dictCode}` 读取字典项，前端常量仅保留降级兜底。
- 后端新增基于字典的写入校验，避免绕过前端直接写入未知 `docCategory` 或 `channelType`。
```

- [ ] **Step 3: Check README.md and AGENTS.md before deciding not to edit**

Run:

```bash
git diff --name-only
```

Expected:
- `README.md` and `AGENTS.md` do not need content changes for this slice.
- If either file already has user-owned dirty changes, leave them untouched and record “checked, no update needed” in the delivery note instead of editing them.

- [ ] **Step 4: Commit the docs slice**

Run:

```bash
git add docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md
git commit -m "docs: record help doc and channel dict baseline"
```

### Task 5: Run Real-Environment Verification Against The Shared Dev Baseline

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/NotificationChannelServiceImpl.java`
- Modify: `spring-boot-iot-ui/src/views/HelpDocView.vue`
- Modify: `spring-boot-iot-ui/src/views/ChannelView.vue`

- [ ] **Step 1: Run the focused automated checks**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync scripts.tests.test_dict_seed_snapshot
mvn -s .mvn/settings.xml -pl spring-boot-iot-system -Dtest=SystemDictValueSupportTest,HelpDocumentServiceImplTest,NotificationChannelServiceImplTest test
npm --prefix spring-boot-iot-ui test -- src/__tests__/views/HelpDocView.test.ts src/__tests__/views/ChannelView.test.ts
```

Expected:
- All three command groups pass.
- No new failures appear outside this first-wave slice.

- [ ] **Step 2: Apply the schema sync to the shared MySQL environment**

Run:

```bash
python scripts/run-real-env-schema-sync.py
```

Expected:
- Exit code `0`
- The script prints the normal `Schema sync completed successfully.` tail line.

- [ ] **Step 3: Verify the dict API read side through the real backend**

Run in PowerShell:

```powershell
$loginBody = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:9999/api/auth/login' -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.data.token)" }

Invoke-RestMethod -Headers $headers -Uri 'http://localhost:9999/api/dict/code/help_doc_category'
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:9999/api/dict/code/notification_channel_type'
```

Expected:
- `help_doc_category` returns only `business / technical / faq`
- `notification_channel_type` returns only `email / sms / webhook / wechat / feishu / dingtalk`

- [ ] **Step 4: Verify the business read side still accepts the normalized values**

Run in PowerShell:

```powershell
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:9999/api/system/help-doc/page?pageNum=1&pageSize=5&docCategory=business'
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:9999/api/system/channel/page?pageNum=1&pageSize=5&channelType=webhook'
```

Expected:
- Both APIs return `code = 200`
- The filters still work with the dict-backed codes after the backend validation changes.

- [ ] **Step 5: Commit the final verification checkpoint**

Run:

```bash
git status --short
git commit --allow-empty -m "chore: verify help doc and channel dict rollout"
```

Expected:
- `git status --short` shows a clean worktree before the empty verification commit.
- The empty commit records that the shared-environment verification was actually executed after the code slices landed.
