# Message Trace Payload Comparison Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a three-panel payload comparison section to `/message-trace` detail drawers so operators can compare the raw message-log payload, decrypted plaintext, and protocol-decoded result without changing the existing timeline interaction model.

**Architecture:** Keep raw payload sourced from `DeviceMessageLog`, extend protocol decoding metadata so encrypted plaintext and decoded snapshots survive into the `PROTOCOL_DECODE` timeline summary, and let the Vue detail drawer compose three fixed comparison panels from `detailData + detailTimeline`. Isolate the UI parsing/rendering into a small helper and a focused component so timeline-expired behavior stays explicit and testable.

**Tech Stack:** Spring Boot 4 / Java 17, Vue 3 + TypeScript, Vitest, JUnit 5, existing `message-flow` timeline store and `StandardDetailDrawer` / `StandardTraceTimeline` patterns.

---

## File Structure

- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpProtocolMetadata.java`
  Responsibility: carry protocol-layer diagnostic previews that survive beyond the adapter call.
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`
  Responsibility: expose decrypted plaintext separately from the raw logged payload for encrypted `$dp` envelopes.
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
  Responsibility: populate protocol metadata with decrypted plaintext preview and decoded-result preview.
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java`
  Responsibility: lock the decoder contract for encrypted plaintext exposure.
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`
  Responsibility: lock protocol metadata previews for encrypted and plain payloads.

- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/ProtocolDecodeSummarySupport.java`
  Responsibility: copy protocol preview metadata into `PROTOCOL_DECODE.summary` without bloating `UpMessageProcessingPipeline`.
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
  Responsibility: call the new support helper after decode and before timeline persistence.
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`
  Responsibility: verify the `PROTOCOL_DECODE` step now carries the two new preview fields.

- Modify: `spring-boot-iot-ui/src/types/api.ts`
  Responsibility: add typed access to decode-step summary previews.
- Create: `spring-boot-iot-ui/src/utils/messageTracePayloadComparison.ts`
  Responsibility: derive the three display panels and their empty-state copy from `detailData`, `detailTimeline`, and the timeline-expired flag.
- Create: `spring-boot-iot-ui/src/__tests__/utils/messageTracePayloadComparison.test.ts`
  Responsibility: unit-test helper output for encrypted, plain, and expired-timeline scenarios.

- Create: `spring-boot-iot-ui/src/components/messageTrace/MessageTracePayloadComparisonSection.vue`
  Responsibility: render the three equal-weight comparison cards with shared code-block styling and honest empty states.
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
  Responsibility: replace the single `消息内容` block with the new `Payload 对照` section while preserving timeline and route advice behavior.
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
  Responsibility: prove the drawer shows raw/decrypted/decoded panels and keeps placeholders when timeline data is missing or expired.

- Modify: `docs/06-前端开发与CSS规范.md`
  Responsibility: codify the `/message-trace` detail-drawer payload comparison layout rule.
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: record the governance decision so the page does not regress to a single payload block.
- Modify: `docs/08-变更记录与技术债清单.md`
  Responsibility: log the behavior change and the accepted “timeline expiry only keeps raw payload” tradeoff.
- Modify: `README.md`
  Responsibility: update the message-trace baseline sentence so repo-level docs match the shipped behavior.

### Task 1: Expose Protocol Diagnostic Previews

**Files:**
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpProtocolMetadata.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`

- [ ] **Step 1: Write the failing decoder and adapter tests**

Add these assertions before implementation:

```java
@Test
void shouldExposePlaintextPayloadForEncryptedEnvelope() {
    Object decoder = newDecoder(List.of(new StubDecryptor(
            "62000001",
            buildPacket((byte) 2, """
                    {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                    """)
    )));

    Object decoded = decode(decoder, """
            {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
            """.getBytes(StandardCharsets.UTF_8));

    assertTrue(String.valueOf(invoke(decoded, "plaintextPayload")).contains("\"17165802\""));
}
```

```java
@Test
void shouldExposePayloadComparisonMetadataForEncryptedLegacyDpPayload() {
    DeviceUpMessage message = configuredAdapter.decode("""
            {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
            """.getBytes(StandardCharsets.UTF_8), context);

    Object protocolMetadata = getProtocolMetadata(message);
    assertTrue(String.valueOf(readMetadata(protocolMetadata, "getDecryptedPayloadPreview")).contains("\"17165802\""));

    @SuppressWarnings("unchecked")
    Map<String, Object> decodedPreview = (Map<String, Object>) readMetadata(protocolMetadata, "getDecodedPayloadPreview");
    assertEquals("property", decodedPreview.get("messageType"));
    assertEquals("17165802", decodedPreview.get("deviceCode"));
    assertTrue(decodedPreview.containsKey("properties"));
}
```

- [ ] **Step 2: Run the protocol tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=LegacyDpEnvelopeDecoderTest,MqttJsonProtocolAdapterTest test
```

Expected: FAIL because `LegacyDpEnvelopeDecoder.DecodedEnvelope` does not expose `plaintextPayload()` yet and `DeviceUpProtocolMetadata` does not expose `getDecryptedPayloadPreview()` / `getDecodedPayloadPreview()` yet.

- [ ] **Step 3: Add the metadata fields and decoder plumbing**

Update the protocol metadata model and decoder contract:

```java
// spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpProtocolMetadata.java
import java.util.List;
import java.util.Map;

@Data
public class DeviceUpProtocolMetadata {

    private String appId;
    private List<String> familyCodes;
    private String normalizationStrategy;
    private String timestampSource;
    private Boolean childSplitApplied;
    private String routeType;
    private String decryptedPayloadPreview;
    private Map<String, Object> decodedPayloadPreview;
}
```

```java
// spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java
return new DecodedEnvelope(
        decryptedMap,
        payloadText,
        plaintext,
        decryptedFrame.dataFormatType(),
        buildFilePayload(decryptedMap, decryptedFrame),
        appId
);

public record DecodedEnvelope(Map<String, Object> payload,
                              String rawPayload,
                              String plaintextPayload,
                              MqttDataFormatType dataFormatType,
                              DeviceFilePayload filePayload,
                              String appId) {
}
```

- [ ] **Step 4: Populate preview metadata in the MQTT JSON adapter**

Add a focused helper so the adapter emits a stable decoded snapshot:

```java
// spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java
message.setRawPayload(decodedPayload.rawPayload());
DeviceUpProtocolMetadata protocolMetadata = buildProtocolMetadata(
        context,
        decodedPayload.appId(),
        normalizeResult == null
                ? legacyDpFamilyResolver.detectFamilyCodes(map, resolvedDeviceCode)
                : normalizeResult.getFamilyCodes(),
        normalizeResult == null
                ? resolvedTimestamp.timestampSource()
                : normalizeResult.getTimestampSource(),
        childSplitApplied
);
protocolMetadata.setDecryptedPayloadPreview(decodedPayload.plaintextPayload());
protocolMetadata.setDecodedPayloadPreview(buildDecodedPayloadPreview(message));
message.setProtocolMetadata(protocolMetadata);
```

```java
private Map<String, Object> buildDecodedPayloadPreview(DeviceUpMessage message) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("messageType", message.getMessageType());
    preview.put("deviceCode", message.getDeviceCode());
    preview.put("productKey", message.getProductKey());
    preview.put("dataFormatType", message.getDataFormatType());
    if (message.getProperties() != null && !message.getProperties().isEmpty()) {
        preview.put("properties", message.getProperties());
    }
    if (message.getEvents() != null && !message.getEvents().isEmpty()) {
        preview.put("events", message.getEvents());
    }
    if (message.getFilePayload() != null) {
        preview.put("filePayload", Map.of(
                "fileType", message.getFilePayload().getFileType(),
                "dataSetId", message.getFilePayload().getDataSetId(),
                "binaryLength", message.getFilePayload().getBinaryLength()
        ));
    }
    if (message.getChildMessages() != null && !message.getChildMessages().isEmpty()) {
        preview.put("childMessageCount", message.getChildMessages().size());
    }
    return preview;
}
```

- [ ] **Step 5: Run the protocol tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=LegacyDpEnvelopeDecoderTest,MqttJsonProtocolAdapterTest test
```

Expected: PASS with encrypted-envelope plaintext exposure and protocol metadata preview assertions green.

- [ ] **Step 6: Commit**

```bash
git add \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpProtocolMetadata.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java
git commit -m "feat: expose protocol payload comparison previews"
```

### Task 2: Publish Decode Previews into `PROTOCOL_DECODE` Timeline Summaries

**Files:**
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/ProtocolDecodeSummarySupport.java`
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`

- [ ] **Step 1: Write the failing pipeline test**

Add a focused timeline assertion:

```java
@Test
void processShouldExposeProtocolPayloadComparisonSummary() {
    UpMessageProcessingRequest request = buildMqttRequest("$dp", "cipher-text");
    RawDeviceMessage rawDeviceMessage = buildRawMessage("$dp", "legacy", "demo-device-01", "demo-product");
    DeviceUpMessage upMessage = buildUpMessage("demo-device-01", "demo-product", "property", "$dp");
    DeviceUpProtocolMetadata protocolMetadata = new DeviceUpProtocolMetadata();
    protocolMetadata.setRouteType("legacy");
    protocolMetadata.setDecryptedPayloadPreview("{\"plain\":true}");
    protocolMetadata.setDecodedPayloadPreview(Map.of(
            "messageType", "property",
            "deviceCode", "demo-device-01",
            "properties", Map.of("temperature", 26.5)
    ));
    upMessage.setProtocolMetadata(protocolMetadata);

    when(mqttTopicRouter.toRawMessage(anyString(), any(MqttMessage.class))).thenReturn(rawDeviceMessage);
    when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
    when(protocolAdapter.decode(any(), any())).thenReturn(upMessage);
    when(deviceContractStageHandler.resolve(any())).thenReturn(buildTarget("demo-device-01", upMessage));
    when(devicePayloadApplyStageHandler.apply(any())).thenReturn(buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 1)));
    when(telemetryPersistStageHandler.persist(any())).thenReturn(TelemetryPersistResult.persisted(1));

    MessageFlowExecutionResult result = pipeline.process(request);

    MessageFlowStep decodeStep = findStep(result.getTimeline(), MessageFlowStages.PROTOCOL_DECODE);
    assertEquals("{\"plain\":true}", decodeStep.getSummary().get("decryptedPayloadPreview"));
    assertEquals("demo-device-01", ((Map<?, ?>) decodeStep.getSummary().get("decodedPayloadPreview")).get("deviceCode"));
}
```

- [ ] **Step 2: Run the message-pipeline test to verify it fails**

Run:

```bash
mvn -pl spring-boot-iot-message -DskipTests=false -Dtest=UpMessageProcessingPipelineTest#processShouldExposeProtocolPayloadComparisonSummary test
```

Expected: FAIL because `PROTOCOL_DECODE.summary` does not yet include the two preview keys.

- [ ] **Step 3: Implement the summary support helper and wire it into the pipeline**

Create a tiny helper with one job:

```java
// spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/ProtocolDecodeSummarySupport.java
package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStageResult;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;

import java.util.Map;

final class ProtocolDecodeSummarySupport {

    private ProtocolDecodeSummarySupport() {
    }

    static void append(MessageFlowStageResult result, DeviceUpMessage upMessage) {
        if (result == null || upMessage == null || upMessage.getProtocolMetadata() == null) {
            return;
        }
        DeviceUpProtocolMetadata metadata = upMessage.getProtocolMetadata();
        if (metadata.getDecryptedPayloadPreview() != null && !metadata.getDecryptedPayloadPreview().isBlank()) {
            result.getSummary().put("decryptedPayloadPreview", metadata.getDecryptedPayloadPreview());
        }
        if (metadata.getDecodedPayloadPreview() != null && !metadata.getDecodedPayloadPreview().isEmpty()) {
            result.getSummary().put("decodedPayloadPreview", metadata.getDecodedPayloadPreview());
        }
    }
}
```

Call it from the decode stage:

```java
// spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java
DeviceUpProtocolMetadata protocolMetadata = upMessage.getProtocolMetadata();
if (protocolMetadata != null) {
    ...
}
ProtocolDecodeSummarySupport.append(result, upMessage);
return result;
```

- [ ] **Step 4: Run the message-pipeline tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-message -DskipTests=false -Dtest=UpMessageProcessingPipelineTest test
```

Expected: PASS, including the new `processShouldExposeProtocolPayloadComparisonSummary` case and existing decode-summary regressions.

- [ ] **Step 5: Commit**

```bash
git add \
  spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/ProtocolDecodeSummarySupport.java \
  spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java \
  spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java
git commit -m "feat: publish decode payload previews to message flow"
```

### Task 3: Add UI Types and a Comparison-Derivation Helper

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Create: `spring-boot-iot-ui/src/utils/messageTracePayloadComparison.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/messageTracePayloadComparison.test.ts`

- [ ] **Step 1: Write the failing helper tests**

Create targeted cases for encrypted, plain, and expired traces:

```ts
import { describe, expect, it } from 'vitest';
import { resolveMessageTracePayloadComparison } from '@/utils/messageTracePayloadComparison';

describe('resolveMessageTracePayloadComparison', () => {
  it('builds three panels from raw payload and protocol decode previews', () => {
    const model = resolveMessageTracePayloadComparison({
      rawPayload: '{"cipher":true}',
      timelineExpired: false,
      timeline: {
        traceId: 'trace-001',
        steps: [
          {
            stage: 'PROTOCOL_DECODE',
            summary: {
              decryptedPayloadPreview: '{"temperature":26.5}',
              decodedPayloadPreview: {
                messageType: 'property',
                deviceCode: 'demo-device-01',
                properties: { temperature: 26.5 }
              }
            }
          }
        ]
      } as never
    });

    expect(model.panels.map((item) => item.title)).toEqual(['原始 Payload', '解密后明文', '解析结果']);
    expect(model.panels[1]?.content).toContain('"temperature":26.5');
    expect(model.panels[2]?.content).toContain('"deviceCode": "demo-device-01"');
  });

  it('keeps explicit placeholders when the timeline is expired', () => {
    const model = resolveMessageTracePayloadComparison({
      rawPayload: '{"temperature":26.5}',
      timelineExpired: true,
      timeline: null
    });

    expect(model.panels[0]?.content).toContain('"temperature":26.5');
    expect(model.panels[1]?.emptyText).toBe('当前时间线已过期，无法恢复解密结果');
    expect(model.panels[2]?.emptyText).toBe('当前时间线已过期，无法恢复解析结果');
  });
});
```

- [ ] **Step 2: Run the helper test to verify it fails**

Run:

```bash
cd spring-boot-iot-ui
npm test -- --run src/__tests__/utils/messageTracePayloadComparison.test.ts
```

Expected: FAIL because the helper file and comparison model do not exist yet.

- [ ] **Step 3: Add typed summary fields and the helper implementation**

Type the decode summary and centralize panel derivation:

```ts
// spring-boot-iot-ui/src/types/api.ts
export interface ProtocolDecodeTimelineSummary {
  decryptedPayloadPreview?: string | null;
  decodedPayloadPreview?: Record<string, unknown> | null;
}
```

```ts
// spring-boot-iot-ui/src/utils/messageTracePayloadComparison.ts
import { prettyJson } from '@/utils/format';
import type { MessageFlowTimeline, ProtocolDecodeTimelineSummary } from '@/types/api';

export interface MessageTracePayloadComparisonPanel {
  key: 'raw' | 'decrypted' | 'decoded';
  title: string;
  description: string;
  content: string;
  emptyText: string;
  available: boolean;
}

export function resolveMessageTracePayloadComparison(input: {
  rawPayload?: string | null;
  timeline?: MessageFlowTimeline | null;
  timelineExpired: boolean;
}) {
  const summary = resolveProtocolDecodeSummary(input.timeline);
  return {
    panels: [
      buildTextPanel('raw', '原始 Payload', '保留消息日志中的原始报文。', input.rawPayload, '当前无原始 Payload'),
      buildTextPanel(
        'decrypted',
        '解密后明文',
        '展示协议解码阶段拿到的明文快照。',
        summary?.decryptedPayloadPreview,
        input.timelineExpired ? '当前时间线已过期，无法恢复解密结果' : '当前无解密结果'
      ),
      buildJsonPanel(
        'decoded',
        '解析结果',
        '展示协议层归一化后的结构化上行结果。',
        summary?.decodedPayloadPreview,
        input.timelineExpired ? '当前时间线已过期，无法恢复解析结果' : '当前无解析结果'
      )
    ] satisfies MessageTracePayloadComparisonPanel[]
  };
}
```

- [ ] **Step 4: Run the helper tests to verify they pass**

Run:

```bash
cd spring-boot-iot-ui
npm test -- --run src/__tests__/utils/messageTracePayloadComparison.test.ts
```

Expected: PASS with all three panel titles, content formatting, and expired-timeline placeholders locked in.

- [ ] **Step 5: Commit**

```bash
git add \
  spring-boot-iot-ui/src/types/api.ts \
  spring-boot-iot-ui/src/utils/messageTracePayloadComparison.ts \
  spring-boot-iot-ui/src/__tests__/utils/messageTracePayloadComparison.test.ts
git commit -m "feat: add message trace payload comparison helper"
```

### Task 4: Render the New Payload Comparison Section in the Drawer

**Files:**
- Create: `spring-boot-iot-ui/src/components/messageTrace/MessageTracePayloadComparisonSection.vue`
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: Add the failing view assertions**

Extend the existing drawer tests with explicit UI expectations:

```ts
it('renders raw, decrypted, and decoded payload panels in the detail drawer', async () => {
  vi.mocked(messageApi.getMessageFlowTrace).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      traceId: 'trace-001',
      steps: [
        {
          stage: 'PROTOCOL_DECODE',
          summary: {
            decryptedPayloadPreview: '{"temperature":26.5}',
            decodedPayloadPreview: {
              messageType: 'property',
              deviceCode: 'demo-device-01',
              properties: { temperature: 26.5 }
            }
          }
        }
      ]
    }
  } as never);

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  await findButtonByText(wrapper, '详情')!.trigger('click');
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('Payload 对照');
  expect(wrapper.text()).toContain('原始 Payload');
  expect(wrapper.text()).toContain('解密后明文');
  expect(wrapper.text()).toContain('解析结果');
  expect(wrapper.text()).toContain('"temperature":26.5');
  expect(wrapper.text()).toContain('"deviceCode": "demo-device-01"');
});
```

```ts
it('keeps decrypted and decoded placeholders when the timeline has expired', async () => {
  vi.mocked(messageApi.getMessageFlowTrace).mockResolvedValue({ code: 200, msg: 'success', data: null });

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  await findButtonByText(wrapper, '详情')!.trigger('click');
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('当前时间线已过期，无法恢复解密结果');
  expect(wrapper.text()).toContain('当前时间线已过期，无法恢复解析结果');
});
```

- [ ] **Step 2: Run the view test to verify it fails**

Run:

```bash
cd spring-boot-iot-ui
npm test -- --run src/__tests__/views/MessageTraceView.test.ts
```

Expected: FAIL because the drawer still renders a single `消息内容` block.

- [ ] **Step 3: Build the comparison component and integrate it into `MessageTraceView.vue`**

Create a focused rendering component:

```vue
<!-- spring-boot-iot-ui/src/components/messageTrace/MessageTracePayloadComparisonSection.vue -->
<template>
  <section class="detail-panel">
    <div class="detail-section-header">
      <div>
        <h3>Payload 对照</h3>
        <p>统一对照原始报文、解密明文和协议解析结果，帮助快速判断问题出在接入、解密还是协议归一化阶段。</p>
      </div>
    </div>

    <div class="message-trace-payload-comparison">
      <article
        v-for="panel in panels"
        :key="panel.key"
        class="message-trace-payload-comparison__card"
      >
        <div class="detail-section-header">
          <div>
            <h4>{{ panel.title }}</h4>
            <p>{{ panel.description }}</p>
          </div>
        </div>
        <div class="detail-field__value detail-field__value--pre">
          {{ panel.available ? panel.content : panel.emptyText }}
        </div>
      </article>
    </div>
  </section>
</template>
```

Wire the helper into the view:

```ts
// spring-boot-iot-ui/src/views/MessageTraceView.vue
import MessageTracePayloadComparisonSection from '@/components/messageTrace/MessageTracePayloadComparisonSection.vue';
import { resolveMessageTracePayloadComparison } from '@/utils/messageTracePayloadComparison';

const payloadComparison = computed(() =>
  resolveMessageTracePayloadComparison({
    rawPayload: detailData.value.payload,
    timeline: detailTimeline.value,
    timelineExpired: timelineExpired.value
  })
);
```

```vue
<MessageTracePayloadComparisonSection :panels="payloadComparison.panels" />
<div class="detail-notice">
  <span class="detail-notice__label">排查建议</span>
  <strong class="detail-notice__value">{{ detailRouteAdvice }}</strong>
</div>
```

- [ ] **Step 4: Run the UI tests to verify they pass**

Run:

```bash
cd spring-boot-iot-ui
npm test -- --run src/__tests__/utils/messageTracePayloadComparison.test.ts src/__tests__/views/MessageTraceView.test.ts
```

Expected: PASS with the new comparison panels rendered and the expired-timeline placeholders preserved.

- [ ] **Step 5: Commit**

```bash
git add \
  spring-boot-iot-ui/src/components/messageTrace/MessageTracePayloadComparisonSection.vue \
  spring-boot-iot-ui/src/views/MessageTraceView.vue \
  spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "feat: show payload comparison in message trace details"
```

### Task 5: Update Docs and Run Cross-Module Verification

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `README.md`

- [ ] **Step 1: Update the front-end governance docs**

Add the new rule text directly to the existing docs:

```md
<!-- docs/06-前端开发与CSS规范.md -->
- `/message-trace` 详情抽屉当前固定采用“处理时间线 + Payload 对照”语法；时间线下方必须同权展示 `原始 Payload / 解密后明文 / 解析结果` 三张内容卡，缺失结果保留占位提示，不得回退到单一 Payload 大块或私有二级页签。
```

```md
<!-- docs/15-前端优化与治理计划.md -->
- `链路追踪台` 详情抽屉当前已升级为 `Payload 对照` 账本：原始 Payload、解密后明文与解析结果必须同权并列或按固定顺序堆叠展示；时间线过期时保留原始 Payload，并对其余两层明确显示不可恢复提示，后续页面治理不得再回退到单块报文视图。
```

- [ ] **Step 2: Record the change log and repo-level baseline**

Append the implementation note and README baseline sentence:

```md
<!-- docs/08-变更记录与技术债清单.md -->
- `2026-04-04`：链路追踪台详情抽屉升级为三层 Payload 对照账本；原始 Payload 继续来自消息日志，解密后明文和解析结果来自 `PROTOCOL_DECODE` 时间线摘要。当前接受“时间线过期后仅保留原始 Payload”的设计取舍，暂不扩消息日志持久化字段。
```

```md
<!-- README.md -->
- `/message-trace` 详情抽屉当前在处理时间线下固定展示 `原始 Payload / 解密后明文 / 解析结果` 三层对照账本；当 Redis 时间线过期时，页面继续保留原始 Payload，并对另外两层给出明确缺失提示。
```

- [ ] **Step 3: Run the targeted verification suite**

Run:

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=LegacyDpEnvelopeDecoderTest,MqttJsonProtocolAdapterTest test
mvn -pl spring-boot-iot-message -DskipTests=false -Dtest=UpMessageProcessingPipelineTest test
cd spring-boot-iot-ui && npm test -- --run src/__tests__/utils/messageTracePayloadComparison.test.ts src/__tests__/views/MessageTraceView.test.ts
cd /Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot && node scripts/run-quality-gates.mjs
```

Expected:

- Maven commands exit `0`
- Vitest reports all targeted cases `PASS`
- `node scripts/run-quality-gates.mjs` exits `0`

- [ ] **Step 4: Review the final diff before handoff**

Run:

```bash
git diff --stat
git status --short
```

Expected: only the protocol, message-flow, UI, and documentation files listed in this plan are modified.

- [ ] **Step 5: Commit**

```bash
git add \
  docs/06-前端开发与CSS规范.md \
  docs/15-前端优化与治理计划.md \
  docs/08-变更记录与技术债清单.md \
  README.md
git commit -m "docs: document message trace payload comparison"
```
