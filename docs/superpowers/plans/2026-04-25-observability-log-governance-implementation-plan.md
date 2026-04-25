# Observability Log Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an executable E5 observability log governance flow with policy-driven retention cleanup, dry-run/apply reporting, and stronger write-side masking/truncation for lightweight evidence tables.

**Architecture:** Keep the existing evidence schema untouched. Add a focused Python governance CLI alongside the existing observability health tooling, driven by a JSON policy file and real-environment MySQL connection defaults from `application-dev.yml`. In Java, keep masking inside the framework/system observability helpers by expanding `SensitiveLogSanitizer` and routing `tags_json` / `metadata_json` through a new bounded JSON normalizer before `JdbcObservabilityEvidenceRecorder` writes rows.

**Tech Stack:** Python 3, `unittest`, `pymysql`, Java 17, JUnit 5, Mockito, Spring `JdbcTemplate`, Markdown docs.

---

## File Map

- Create: `config/automation/observability-log-governance-policy.json`
  - Owns table retention days, batch sizes, sample size, report directory, and lightweight evidence JSON bounds.
- Create: `scripts/govern-observability-logs.py`
  - CLI entrypoint plus reusable helpers for runtime config, policy loading, snapshot collection, dry-run/apply cleanup, and JSON/Markdown report rendering.
- Create: `scripts/tests/test_govern_observability_logs.py`
  - Python unit tests for policy loading, report rendering, dry-run/apply behavior, and exit codes.
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiter.java`
  - Central helper to sanitize, bound, and serialize lightweight evidence maps without changing schema.
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiterTest.java`
  - Unit tests for string truncation, nested collections, and final JSON size limits.
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorderTest.java`
  - Mockito-based tests capturing inserted arguments for `tags_json` / `metadata_json` masking and truncation.
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizer.java`
  - Expand sensitive-key coverage.
- Modify: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizerTest.java`
  - Adds new key coverage assertions.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorder.java`
  - Route `tags_json` / `metadata_json` through the limiter and keep error text sanitization as-is.
- Modify: `README.md`
  - Adds the governance CLI usage next to observability tooling.
- Modify: `AGENTS.md`
  - Adds current-state note for log retention / dry-run / apply governance.
- Modify: `docs/07-部署运行与配置说明.md`
  - Documents governance CLI execution and outputs.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Logs E5 delivery note.
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
  - Documents retention defaults, write-side bounding, and dry-run/apply behavior.

## Task 1: Add Failing Python Tests For Governance CLI

**Files:**
- Create: `scripts/tests/test_govern_observability_logs.py`
- Create later: `scripts/govern-observability-logs.py`
- Create later: `config/automation/observability-log-governance-policy.json`

- [ ] **Step 1: Write the failing CLI tests**

Create `scripts/tests/test_govern_observability_logs.py` with:

```python
import importlib.util
import json
import pathlib
import tempfile
import unittest
from unittest import mock


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "govern-observability-logs.py"
SPEC = importlib.util.spec_from_file_location("observability_log_governance", SCRIPT_PATH)
observability_log_governance = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(observability_log_governance)


def create_snapshot_fixture():
    return {
        "generatedAt": "2026-04-25T22:30:00",
        "mode": "DRY_RUN",
        "tables": {
            "sys_observability_span_log": {
                "retentionDays": 30,
                "expiredRows": 12,
                "deletedRows": 0,
                "remainingExpiredRows": 12,
                "samples": [{"traceId": "trace-span-1", "spanType": "HTTP_REQUEST"}],
            },
            "sys_business_event_log": {
                "retentionDays": 90,
                "expiredRows": 4,
                "deletedRows": 0,
                "remainingExpiredRows": 4,
                "samples": [{"traceId": "trace-event-1", "eventCode": "governance.publish"}],
            },
            "iot_message_log": {
                "retentionDays": 30,
                "expiredRows": 20,
                "deletedRows": 0,
                "remainingExpiredRows": 20,
                "samples": [{"traceId": "trace-msg-1", "topic": "/sys/demo/up"}],
            },
        },
        "summary": {"expiredRows": 36, "deletedRows": 0, "tablesWithExpiredRows": 3},
    }
```

Add tests:

```python
class LoadPolicyTest(unittest.TestCase):
    def test_load_policy_exposes_expected_defaults(self):
        policy = observability_log_governance.load_policy(
            pathlib.Path("config/automation/observability-log-governance-policy.json")
        )
        self.assertIn("tables", policy)
        self.assertEqual(30, policy["tables"]["sys_observability_span_log"]["retentionDays"])
        self.assertEqual(90, policy["tables"]["sys_business_event_log"]["retentionDays"])
        self.assertEqual(30, policy["tables"]["iot_message_log"]["retentionDays"])


class RenderMarkdownTest(unittest.TestCase):
    def test_render_markdown_includes_table_summary_and_mode(self):
        markdown = observability_log_governance.render_markdown(create_snapshot_fixture())
        self.assertIn("# 可观测日志治理报告", markdown)
        self.assertIn("DRY_RUN", markdown)
        self.assertIn("sys_observability_span_log", markdown)
        self.assertIn("iot_message_log", markdown)


class RunCliTest(unittest.TestCase):
    def test_run_cli_writes_artifacts_in_dry_run(self):
        class FakeConnection:
            def close(self):
                return None

        with tempfile.TemporaryDirectory(prefix="observability-log-governance-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy = {
                "reportDirectory": "logs/observability",
                "tables": {
                    "sys_observability_span_log": {"retentionDays": 30, "timeField": "started_at", "deleteBatchSize": 100},
                    "sys_business_event_log": {"retentionDays": 90, "timeField": "occurred_at", "deleteBatchSize": 100},
                    "iot_message_log": {"retentionDays": 30, "timeField": "report_time", "deleteBatchSize": 100},
                },
            }
            policy_path = workspace_root / "observability-log-policy.json"
            policy_path.write_text(json.dumps(policy, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_log_governance,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ), mock.patch.object(
                observability_log_governance,
                "open_db",
                return_value=FakeConnection(),
            ), mock.patch.object(
                observability_log_governance,
                "collect_governance_snapshot",
                return_value=create_snapshot_fixture(),
            ):
                result = observability_log_governance.run_governance_cli(
                    argv=[f"--policy-path={policy_path}"],
                    workspace_root=workspace_root,
                )

            self.assertEqual(0, result["exitCode"])
            self.assertTrue(pathlib.Path(result["jsonPath"]).exists())
            self.assertTrue(pathlib.Path(result["markdownPath"]).exists())

    def test_run_cli_marks_apply_mode_and_delete_count(self):
        snapshot = create_snapshot_fixture()
        snapshot["mode"] = "APPLY"
        snapshot["summary"]["deletedRows"] = 16
        snapshot["tables"]["iot_message_log"]["deletedRows"] = 10

        class FakeConnection:
            def close(self):
                return None

        with tempfile.TemporaryDirectory(prefix="observability-log-governance-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy_path = workspace_root / "observability-log-policy.json"
            policy_path.write_text(json.dumps({"tables": {}}, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_log_governance,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ), mock.patch.object(
                observability_log_governance,
                "open_db",
                return_value=FakeConnection(),
            ), mock.patch.object(
                observability_log_governance,
                "collect_governance_snapshot",
                return_value=snapshot,
            ):
                result = observability_log_governance.run_governance_cli(
                    argv=[f"--policy-path={policy_path}", "--apply"],
                    workspace_root=workspace_root,
                )

            written = json.loads(pathlib.Path(result["jsonPath"]).read_text(encoding="utf-8"))
            self.assertEqual("APPLY", written["mode"])
            self.assertEqual(16, written["summary"]["deletedRows"])
```

- [ ] **Step 2: Run the Python tests and verify they fail**

Run:

```bash
python3 scripts/tests/test_govern_observability_logs.py
```

Expected: FAIL because `scripts/govern-observability-logs.py` does not exist yet.

- [ ] **Step 3: Commit the failing Python tests**

```bash
git add scripts/tests/test_govern_observability_logs.py
git commit -m "test: cover observability log governance cli"
```

## Task 2: Implement Policy File And Governance CLI

**Files:**
- Create: `config/automation/observability-log-governance-policy.json`
- Create: `scripts/govern-observability-logs.py`
- Test: `scripts/tests/test_govern_observability_logs.py`

- [ ] **Step 1: Create the policy file**

Create `config/automation/observability-log-governance-policy.json` with:

```json
{
  "reportDirectory": "logs/observability",
  "sampleLimit": 5,
  "tables": {
    "sys_observability_span_log": {
      "label": "可观测调用片段日志",
      "timeField": "started_at",
      "retentionDays": 30,
      "deleteBatchSize": 500
    },
    "sys_business_event_log": {
      "label": "业务事件日志",
      "timeField": "occurred_at",
      "retentionDays": 90,
      "deleteBatchSize": 500
    },
    "iot_message_log": {
      "label": "设备消息日志",
      "timeField": "report_time",
      "retentionDays": 30,
      "deleteBatchSize": 500
    }
  },
  "lightweightEvidence": {
    "maxStringLength": 255,
    "maxObjectEntries": 30,
    "maxArrayEntries": 10,
    "maxSerializedLength": 2000
  }
}
```

- [ ] **Step 2: Implement the CLI**

Create `scripts/govern-observability-logs.py` with helpers that mirror the existing health script patterns:

```python
def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Govern observability logs via the real environment.")
    parser.add_argument("--jdbc-url")
    parser.add_argument("--user")
    parser.add_argument("--password")
    parser.add_argument("--policy-path")
    parser.add_argument("--json-out")
    parser.add_argument("--md-out")
    parser.add_argument("--apply", action="store_true")
    return parser.parse_args(argv)


def load_policy(policy_path):
    path = pathlib.Path(policy_path) if policy_path else DEFAULT_POLICY_PATH
    return json.loads(path.read_text(encoding="utf-8"))


def build_table_snapshot(cur, table_name, config, apply_mode):
    ...


def delete_expired_rows(cur, table_name, time_field, cutoff_at, delete_batch_size):
    ...


def collect_governance_snapshot(conn, policy, apply_mode):
    ...


def render_markdown(report):
    ...


def run_governance_cli(argv=None, workspace_root=REPO_ROOT):
    ...
```

Implementation rules:

- Reuse `application-dev.yml` parsing and JDBC resolution approach from `generate-observability-health.py`.
- For each table, count total rows, expired rows, earliest/latest timestamp, and sample rows.
- In `--apply` mode, delete expired rows in batches and `commit()` after completion.
- Emit JSON and Markdown artifacts under `logs/observability/`.

- [ ] **Step 3: Run the Python tests and verify they pass**

Run:

```bash
python3 scripts/tests/test_govern_observability_logs.py
```

Expected: PASS.

- [ ] **Step 4: Run the CLI once in dry-run mode**

Run:

```bash
python3 scripts/govern-observability-logs.py
```

Expected: JSON / Markdown artifacts created under `logs/observability/` using the real-environment connection defaults.

- [ ] **Step 5: Commit the CLI implementation**

```bash
git add config/automation/observability-log-governance-policy.json scripts/govern-observability-logs.py scripts/tests/test_govern_observability_logs.py
git commit -m "feat: add observability log governance cli"
```

## Task 3: Add Failing Java Tests For Masking And Lightweight Evidence Bounding

**Files:**
- Create: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiterTest.java`
- Modify: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizerTest.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorderTest.java`
- Create later: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiter.java`
- Modify later: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorder.java`

- [ ] **Step 1: Extend sanitizer test coverage**

Update `SensitiveLogSanitizerTest` with one more test:

```java
@Test
void sanitizeShouldMaskExpandedSecretKeys() {
    String raw = """
            {"apiKey":"demo-api","accessKey":"demo-access","privateKey":"demo-private",
             "deviceSecret":"demo-device","merchantKey":"demo-merchant","signatureSecret":"demo-sign"}
            """;

    String sanitized = SensitiveLogSanitizer.sanitize(raw);

    assertTrue(sanitized.contains("\"apiKey\":\"***\""));
    assertTrue(sanitized.contains("\"accessKey\":\"***\""));
    assertTrue(sanitized.contains("\"privateKey\":\"***\""));
    assertTrue(sanitized.contains("\"deviceSecret\":\"***\""));
    assertTrue(sanitized.contains("\"merchantKey\":\"***\""));
    assertTrue(sanitized.contains("\"signatureSecret\":\"***\""));
}
```

- [ ] **Step 2: Add payload limiter tests**

Create `ObservabilityPayloadLimiterTest.java` with:

```java
@Test
void toJsonShouldMaskSecretsAndTruncateLargeValues() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("apiKey", "secret-demo");
    payload.put("longValue", "x".repeat(400));

    String json = ObservabilityPayloadLimiter.toJson(payload);

    assertTrue(json.contains("\"apiKey\":\"***\""));
    assertTrue(json.contains("...(truncated)"));
    assertTrue(json.length() <= 2000);
}

@Test
void toJsonShouldLimitNestedCollections() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("items", IntStream.range(0, 20).boxed().toList());

    String json = ObservabilityPayloadLimiter.toJson(payload);

    assertTrue(json.contains("...(truncated)"));
}
```

- [ ] **Step 3: Add recorder tests**

Create `JdbcObservabilityEvidenceRecorderTest.java` with Mockito:

```java
@ExtendWith(MockitoExtension.class)
class JdbcObservabilityEvidenceRecorderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcObservabilityEvidenceRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new JdbcObservabilityEvidenceRecorder(jdbcTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any()))
                .thenReturn(List.of("id", "tenant_id", "trace_id", "span_type", "span_name", "status",
                        "started_at", "finished_at", "tags_json", "create_time", "deleted",
                        "event_code", "event_name", "domain_code", "action_code", "occurred_at", "metadata_json"));
        lenient().when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    }
}
```

Add assertions:

```java
@Test
void recordSpanShouldSanitizeAndBoundTagsJson() {
    ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
    span.setTraceId("trace-1");
    span.setSpanType("HTTP_REQUEST");
    span.setSpanName("HTTP_REQUEST");
    span.setTags(Map.of("apiKey", "secret-demo", "payload", "x".repeat(400)));

    recorder.recordSpan(span);

    ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
    verify(jdbcTemplate).update(anyString(), argsCaptor.capture());
    String storedJson = findJsonArg(argsCaptor.getValue());
    assertTrue(storedJson.contains("\"apiKey\":\"***\""));
    assertTrue(storedJson.contains("...(truncated)"));
}
```

Do the same for `recordBusinessEventShouldSanitizeAndBoundMetadataJson`.

- [ ] **Step 4: Run Java tests and verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-framework,spring-boot-iot-system -am test -DskipTests=false -Dtest=SensitiveLogSanitizerTest,ObservabilityPayloadLimiterTest,JdbcObservabilityEvidenceRecorderTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the limiter class and recorder behavior do not exist yet.

- [ ] **Step 5: Commit the failing Java tests**

```bash
git add spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizerTest.java spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiterTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorderTest.java
git commit -m "test: cover observability evidence payload governance"
```

## Task 4: Implement Java Masking And Payload Limiting

**Files:**
- Create: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiter.java`
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizer.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorder.java`
- Test: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizerTest.java`
- Test: `spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiterTest.java`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorderTest.java`

- [ ] **Step 1: Expand the sensitive-key patterns**

Update `SensitiveLogSanitizer.java` so every pattern includes:

```java
password|token|secret|authorization|accessToken|refreshToken|clientSecret|apiKey|accessKey|privateKey|deviceSecret|merchantKey|signatureSecret
```

- [ ] **Step 2: Add the payload limiter**

Create `ObservabilityPayloadLimiter.java` with:

```java
public final class ObservabilityPayloadLimiter {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
    private static final int MAX_STRING_LENGTH = 255;
    private static final int MAX_OBJECT_ENTRIES = 30;
    private static final int MAX_ARRAY_ENTRIES = 10;
    private static final int MAX_SERIALIZED_LENGTH = 2000;
    private static final String TRUNCATED_SUFFIX = "...(truncated)";

    public static String toJson(Map<String, Object> value) {
        ...
    }
}
```

Implementation rules:

- Sanitize string values through `SensitiveLogSanitizer`.
- Bound nested maps and arrays recursively.
- Keep deterministic order for maps.
- Apply a final serialized-length clamp with `...(truncated)`.

- [ ] **Step 3: Wire recorder writes through the limiter**

Update `JdbcObservabilityEvidenceRecorder.java`:

```java
put(values, columns, "tags_json", ObservabilityPayloadLimiter.toJson(normalized.getTags()));
put(values, columns, "metadata_json", ObservabilityPayloadLimiter.toJson(normalized.getMetadata()));
```

- [ ] **Step 4: Run Java tests and verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-framework,spring-boot-iot-system -am test -DskipTests=false -Dtest=SensitiveLogSanitizerTest,ObservabilityPayloadLimiterTest,JdbcObservabilityEvidenceRecorderTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 5: Commit the Java implementation**

```bash
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizer.java spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiter.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorder.java spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/SensitiveLogSanitizerTest.java spring-boot-iot-framework/src/test/java/com/ghlzm/iot/framework/observability/ObservabilityPayloadLimiterTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/JdbcObservabilityEvidenceRecorderTest.java
git commit -m "feat: govern observability evidence payloads"
```

## Task 5: Update Docs And Final Verification

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`

- [ ] **Step 1: Update documentation**

Document:

- governance CLI command and outputs
- retention defaults per table
- dry-run / apply behavior
- write-side masking and JSON bounding scope

- [ ] **Step 2: Run targeted verification**

Run:

```bash
python3 scripts/tests/test_govern_observability_logs.py
mvn -pl spring-boot-iot-framework,spring-boot-iot-system -am test -DskipTests=false -Dtest=SensitiveLogSanitizerTest,ObservabilityPayloadLimiterTest,JdbcObservabilityEvidenceRecorderTest -Dsurefire.failIfNoSpecifiedTests=false
python3 scripts/govern-observability-logs.py
git diff --check
```

Expected: all commands pass and the governance CLI emits artifacts under `logs/observability/`.

- [ ] **Step 3: Commit docs and final polish**

```bash
git add README.md AGENTS.md docs/07-部署运行与配置说明.md docs/08-变更记录与技术债清单.md docs/11-可观测性、日志追踪与消息通知治理.md docs/superpowers/plans/2026-04-25-observability-log-governance-implementation-plan.md
git commit -m "docs: document observability log governance"
```
