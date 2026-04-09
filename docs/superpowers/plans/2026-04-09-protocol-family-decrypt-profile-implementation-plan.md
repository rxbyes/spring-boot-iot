# Protocol Family And Decrypt Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce YAML/IotProperties-driven protocol family definitions plus unified decrypt profiles so MQTT decrypt routing stops depending on `supports(appId)` scanning and starts flowing through a formal resolver/SPI layer without changing the real `application-dev.yml` baseline semantics.

**Architecture:** Keep `spring.cloud.aes.merchants` and `iot.protocol.crypto.merchants` as the only merchant truth sources, project them into `ProtocolFamilyDefinition` and `ProtocolDecryptProfile` objects, and add a resolver that first supports family-bound decrypt profiles when family context exists while preserving `appId` fallback for the current legacy `$dp` encrypted path. Refactor the decrypt registry to route by resolved profile algorithm, mirroring the existing signer registry pattern, and keep legacy `$dp` behavior stable by passing an empty family list at pre-decrypt time.

**Tech Stack:** Spring Boot 4, Java 17, Spring ConfigurationProperties, JUnit 5, Maven

---

## Scope Check

This plan covers only the approved Workflow D minimum slice:

1. `iot.protocol.family-definitions`
2. `iot.protocol.decrypt-profiles`
3. protocol decrypt profile resolver + executor SPI
4. legacy `$dp` decrypt path wiring and docs

Do not pull database tables, front-end CRUD, approval flows, normalization rule objects, or risk metric generation rule objects into this implementation.

## File Structure

### Framework configuration

- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`

### Protocol runtime objects and resolver

- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolFamilyDefinition.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptProfile.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptResolveContext.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptProfileResolver.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolver.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptExecutor.java`

### Protocol decrypt execution path

- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptorRegistry.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/SpringCloudAesMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/AbstractJceMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/DesMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/TripleDesMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`

### Tests

- Create: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolverTest.java`
- Create: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptorRegistryTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`

### Runtime configuration and docs

- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-prod.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-test.yml`
- Modify: `docs/01-系统概览与架构说明.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

## Task 1: Add protocol family/decrypt profile configuration objects and resolver

**Files:**
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolFamilyDefinition.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptProfile.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptResolveContext.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptProfileResolver.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolver.java`
- Create: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolverTest.java`

- [ ] **Step 1: Write the failing resolver tests**

```java
class IotPropertiesProtocolDecryptProfileResolverTest {

    @Test
    void shouldPreferFamilyBoundProfileOverAppIdFallback() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getDecryptProfiles().put("aes-62000000", decryptProfile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000"));
        properties.getProtocol().getDecryptProfiles().put("des-62000001", decryptProfile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001"));
        properties.getProtocol().getFamilyDefinitions().put("legacy-dp-crack", familyDefinition("legacy-dp-crack", "mqtt-json", "des-62000001"));

        IotPropertiesProtocolDecryptProfileResolver resolver = new IotPropertiesProtocolDecryptProfileResolver(properties);

        ProtocolDecryptProfile resolved = resolver.resolveOrThrow(new ProtocolDecryptResolveContext(
                "62000000",
                "mqtt-json",
                List.of("legacy-dp-crack")
        ));

        assertEquals("des-62000001", resolved.getProfileCode());
        assertEquals("DES", resolved.getAlgorithm());
        assertEquals("IOT_PROTOCOL_CRYPTO", resolved.getMerchantSource());
        assertEquals("62000001", resolved.getMerchantKey());
    }

    @Test
    void shouldFallbackToAppIdProfileWhenNoFamilyMatches() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getDecryptProfiles().put("aes-62000000", decryptProfile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000"));

        IotPropertiesProtocolDecryptProfileResolver resolver = new IotPropertiesProtocolDecryptProfileResolver(properties);

        ProtocolDecryptProfile resolved = resolver.resolveOrThrow(new ProtocolDecryptResolveContext(
                "62000000",
                "mqtt-json",
                List.of()
        ));

        assertEquals("aes-62000000", resolved.getProfileCode());
    }

    @Test
    void shouldRejectDisabledProfileBoundByFamily() {
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.DecryptProfile profile = decryptProfile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001");
        profile.setEnabled(Boolean.FALSE);
        properties.getProtocol().getDecryptProfiles().put("des-62000001", profile);
        properties.getProtocol().getFamilyDefinitions().put("legacy-dp-gnss", familyDefinition("legacy-dp-gnss", "mqtt-json", "des-62000001"));

        IotPropertiesProtocolDecryptProfileResolver resolver = new IotPropertiesProtocolDecryptProfileResolver(properties);

        BizException ex = assertThrows(BizException.class, () -> resolver.resolveOrThrow(new ProtocolDecryptResolveContext(
                "62000001",
                "mqtt-json",
                List.of("legacy-dp-gnss")
        )));

        assertTrue(ex.getMessage().contains("decrypt profile 未启用"));
    }
}
```

- [ ] **Step 2: Run the resolver test to verify the new configuration objects do not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-protocol,spring-boot-iot-framework -am "-Dtest=IotPropertiesProtocolDecryptProfileResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- compile errors mentioning missing `getDecryptProfiles`, `getFamilyDefinitions`, `ProtocolDecryptProfile`, or `IotPropertiesProtocolDecryptProfileResolver`

- [ ] **Step 3: Add the new `IotProperties.Protocol` configuration maps**

```java
@Data
public static class Protocol {
    private String defaultCode = "mqtt-json";
    private Integer timeoutMillis = 5000;
    private Integer retryTimes = 3;
    private Security security = new Security();
    private Crypto crypto = new Crypto();
    private LegacyDp legacyDp = new LegacyDp();
    private Map<String, FamilyDefinition> familyDefinitions = new LinkedHashMap<>();
    private Map<String, DecryptProfile> decryptProfiles = new LinkedHashMap<>();

    @Data
    public static class FamilyDefinition {
        private String familyCode;
        private String protocolCode;
        private String displayName;
        private String decryptProfileCode;
        private String signAlgorithm;
        private String normalizationStrategy;
        private Boolean enabled = Boolean.TRUE;
    }

    @Data
    public static class DecryptProfile {
        private String profileCode;
        private String algorithm;
        private String merchantSource;
        private String merchantKey;
        private String transformation;
        private String signatureSecret;
        private Boolean enabled = Boolean.TRUE;
    }
}
```

- [ ] **Step 4: Add the protocol runtime object classes**

```java
@Data
public class ProtocolFamilyDefinition {
    private String familyCode;
    private String protocolCode;
    private String displayName;
    private String decryptProfileCode;
    private String signAlgorithm;
    private String normalizationStrategy;
    private Boolean enabled;
}
```

```java
@Data
public class ProtocolDecryptProfile {
    private String profileCode;
    private String algorithm;
    private String merchantSource;
    private String merchantKey;
    private String transformation;
    private String signatureSecret;
    private Boolean enabled;
}
```

```java
public record ProtocolDecryptResolveContext(String appId,
                                            String protocolCode,
                                            List<String> familyCodes) {

    public ProtocolDecryptResolveContext {
        familyCodes = familyCodes == null ? List.of() : List.copyOf(familyCodes);
    }
}
```

```java
public interface ProtocolDecryptProfileResolver {

    ProtocolDecryptProfile resolveOrThrow(ProtocolDecryptResolveContext context);
}
```

- [ ] **Step 5: Implement the `IotProperties`-backed resolver**

```java
@Component
public class IotPropertiesProtocolDecryptProfileResolver implements ProtocolDecryptProfileResolver {

    private static final String DEFAULT_PROTOCOL_CODE = "mqtt-json";

    private final IotProperties iotProperties;

    public IotPropertiesProtocolDecryptProfileResolver(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    @Override
    public ProtocolDecryptProfile resolveOrThrow(ProtocolDecryptResolveContext context) {
        ProtocolDecryptProfile familyResolved = resolveByFamily(context);
        if (familyResolved != null) {
            return familyResolved;
        }
        ProtocolDecryptProfile appIdResolved = resolveByAppId(context == null ? null : context.appId());
        if (appIdResolved != null) {
            return appIdResolved;
        }
        throw new BizException("未找到 appId 对应的 decrypt profile: " + safeAppId(context));
    }

    private ProtocolDecryptProfile resolveByFamily(ProtocolDecryptResolveContext context) {
        if (context == null || context.familyCodes().isEmpty()) {
            return null;
        }
        String protocolCode = hasText(context.protocolCode()) ? context.protocolCode() : DEFAULT_PROTOCOL_CODE;
        for (String familyCode : context.familyCodes()) {
            IotProperties.Protocol.FamilyDefinition family = iotProperties.getProtocol().getFamilyDefinitions().get(familyCode);
            if (family == null || !Boolean.TRUE.equals(defaultEnabled(family.getEnabled()))) {
                continue;
            }
            if (hasText(family.getProtocolCode()) && !protocolCode.equalsIgnoreCase(family.getProtocolCode())) {
                continue;
            }
            return resolveProfileCode(family.getDecryptProfileCode());
        }
        return null;
    }

    private ProtocolDecryptProfile resolveByAppId(String appId) {
        if (!hasText(appId)) {
            return null;
        }
        return iotProperties.getProtocol().getDecryptProfiles().values().stream()
                .filter(this::isProfileEnabled)
                .filter(profile -> appId.equals(profile.getMerchantKey()))
                .findFirst()
                .map(this::toRuntimeProfile)
                .orElse(null);
    }
}
```

Implementation rules:

1. `resolveByAppId(...)` must support current YAML layout by scanning enabled profiles whose `merchantKey` equals `appId`.
2. If multiple enabled profiles share the same `merchantKey`, keep selection deterministic by using YAML insertion order from the `LinkedHashMap`.
3. Family resolution only applies when the caller provides non-empty `familyCodes`.
4. Current legacy encrypted `$dp` path will pass an empty family list; that is expected for this phase.

- [ ] **Step 6: Re-run the resolver tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-protocol,spring-boot-iot-framework -am "-Dtest=IotPropertiesProtocolDecryptProfileResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- `IotPropertiesProtocolDecryptProfileResolverTest` passes

- [ ] **Step 7: Commit the configuration-object foundation**

```bash
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolFamilyDefinition.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptProfile.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptResolveContext.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptProfileResolver.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolver.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/IotPropertiesProtocolDecryptProfileResolverTest.java
git commit -m "feat: add protocol decrypt profile resolver"
```

## Task 2: Refactor decrypt routing to resolved-profile + algorithm executor

**Files:**
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptExecutor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptorRegistry.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/SpringCloudAesMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/AbstractJceMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/DesMqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/TripleDesMqttPayloadDecryptor.java`
- Create: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptorRegistryTest.java`

- [ ] **Step 1: Write the failing registry tests**

```java
class MqttPayloadDecryptorRegistryTest {

    @Test
    void shouldRouteByResolvedProfileAlgorithm() {
        ProtocolDecryptProfileResolver resolver = context -> profile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001");
        ProtocolDecryptExecutor desExecutor = new StubExecutor("DES", "ok".getBytes(StandardCharsets.UTF_8));

        MqttPayloadDecryptorRegistry registry = new MqttPayloadDecryptorRegistry(resolver, List.of(desExecutor));

        byte[] decrypted = registry.decryptBytesOrThrow(new ProtocolDecryptResolveContext(
                "62000001",
                "mqtt-json",
                List.of("legacy-dp-gnss")
        ), "cipher-text");

        assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), decrypted);
    }

    @Test
    void shouldKeepLegacyAppIdOverloadForExistingDecoderPath() {
        ProtocolDecryptProfileResolver resolver = context -> profile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000");
        ProtocolDecryptExecutor aesExecutor = new StubExecutor("AES", "legacy".getBytes(StandardCharsets.UTF_8));

        MqttPayloadDecryptorRegistry registry = new MqttPayloadDecryptorRegistry(resolver, List.of(aesExecutor));

        byte[] decrypted = registry.decryptBytesOrThrow("62000000", "cipher-text");

        assertArrayEquals("legacy".getBytes(StandardCharsets.UTF_8), decrypted);
    }
}
```

- [ ] **Step 2: Run the registry test to verify the executor SPI does not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-protocol -am "-Dtest=MqttPayloadDecryptorRegistryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- compile errors mentioning missing `ProtocolDecryptExecutor`, new registry constructor, or new overload

- [ ] **Step 3: Add the new executor SPI**

```java
public interface ProtocolDecryptExecutor {

    boolean supports(String algorithm);

    byte[] decryptBytes(ProtocolDecryptProfile profile, String encryptedBody);
}
```

- [ ] **Step 4: Refactor the registry to resolve profile first and route by algorithm**

```java
@Component
public class MqttPayloadDecryptorRegistry {

    private final ProtocolDecryptProfileResolver protocolDecryptProfileResolver;
    private final List<ProtocolDecryptExecutor> executors;

    public MqttPayloadDecryptorRegistry(ProtocolDecryptProfileResolver protocolDecryptProfileResolver,
                                        List<ProtocolDecryptExecutor> executors) {
        this.protocolDecryptProfileResolver = protocolDecryptProfileResolver;
        this.executors = executors;
    }

    public byte[] decryptBytesOrThrow(ProtocolDecryptResolveContext context, String encryptedBody) {
        ProtocolDecryptProfile profile = protocolDecryptProfileResolver.resolveOrThrow(context);
        return executorFor(profile.getAlgorithm()).decryptBytes(profile, encryptedBody);
    }

    public byte[] decryptBytesOrThrow(String appId, String encryptedBody) {
        return decryptBytesOrThrow(new ProtocolDecryptResolveContext(appId, "mqtt-json", List.of()), encryptedBody);
    }
}
```

Implementation rules:

1. Keep the existing legacy overload so current callers and error messages stay stable.
2. Keep `decryptOrThrow(String appId, ...)` as a compatibility wrapper around the byte overload.
3. Throw `BizException("未找到解密算法实现: " + algorithm)` when no executor matches.

- [ ] **Step 5: Convert AES and JCE decryptors into algorithm executors**

```java
@Component
public class SpringCloudAesMqttPayloadDecryptor implements ProtocolDecryptExecutor {

    @Override
    public boolean supports(String algorithm) {
        return "AES".equalsIgnoreCase(algorithm) || "AES-ENCRYPT".equalsIgnoreCase(algorithm);
    }

    @Override
    public byte[] decryptBytes(ProtocolDecryptProfile profile, String encryptedBody) {
        String merchantKey = profile.getMerchantKey();
        AesEncryptor aesEncryptor = aesEncryptors.get(merchantKey);
        if (aesEncryptor == null) {
            throw new BizException("未找到 merchant 对应的 AES 解密器: " + merchantKey);
        }
        return aesEncryptor.decryptByte(encryptedBody);
    }
}
```

```java
abstract class AbstractJceMqttPayloadDecryptor implements ProtocolDecryptExecutor {

    @Override
    public boolean supports(String algorithm) {
        return algorithm != null && algorithmCode().equalsIgnoreCase(algorithm);
    }

    @Override
    public byte[] decryptBytes(ProtocolDecryptProfile profile, String encryptedBody) {
        IotProperties.Protocol.Crypto.Merchant merchant = findMerchant(profile.getMerchantKey());
        if (merchant == null) {
            throw new BizException("未找到 merchant 对应的 " + algorithmCode() + " 解密配置: " + profile.getMerchantKey());
        }
        String transformation = hasText(profile.getTransformation())
                ? profile.getTransformation()
                : firstNonBlank(merchant.getTransformation(), defaultTransformation());
        // 保持原有 JCE 解密主体逻辑不变
    }
}
```

Implementation rules:

1. `DesMqttPayloadDecryptor` and `TripleDesMqttPayloadDecryptor` remain thin algorithm classes.
2. AES executor keeps using `spring.cloud.aes` merchant truth.
3. JCE executors keep using `iot.protocol.crypto.merchants` truth.

- [ ] **Step 6: Re-run the registry tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-protocol -am "-Dtest=MqttPayloadDecryptorRegistryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- `MqttPayloadDecryptorRegistryTest` passes

- [ ] **Step 7: Commit the registry/executor refactor**

```bash
git add spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/ProtocolDecryptExecutor.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptorRegistry.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/SpringCloudAesMqttPayloadDecryptor.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/AbstractJceMqttPayloadDecryptor.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/DesMqttPayloadDecryptor.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/TripleDesMqttPayloadDecryptor.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptorRegistryTest.java
git commit -m "refactor: route mqtt decrypt by resolved profile"
```

## Task 3: Rewire the legacy `$dp` decrypt path without changing current business behavior

**Files:**
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`

- [ ] **Step 1: Update the legacy decoder tests to match the new registry contract**

```java
private Object newDecoder(ProtocolDecryptProfileResolver resolver, List<ProtocolDecryptExecutor> executors) {
    try {
        Class<?> decoderClass = Class.forName("com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpEnvelopeDecoder");
        Constructor<?> constructor = decoderClass.getConstructor(
                MqttPayloadDecryptorRegistry.class,
                MqttPayloadFrameParser.class,
                MqttPayloadSecurityValidator.class,
                MqttFirmwarePacketParser.class
        );
        IotProperties iotProperties = new IotProperties();
        return constructor.newInstance(
                new MqttPayloadDecryptorRegistry(resolver, executors),
                new MqttPayloadFrameParser(),
                new MqttPayloadSecurityValidator(
                        iotProperties,
                        new MqttMessageSignerRegistry(List.of()),
                        new DefaultListableBeanFactory().getBeanProvider(StringRedisTemplate.class)
                ),
                new MqttFirmwarePacketParser()
        );
    } catch (ReflectiveOperationException ex) {
        throw new AssertionError("Expected LegacyDpEnvelopeDecoder class with the planned constructor", ex);
    }
}
```

```java
@Test
void shouldKeepAppIdFallbackForEncryptedEnvelopePayload() {
    ProtocolDecryptProfileResolver resolver = context -> profile("aes-62000001", "AES", "SPRING_CLOUD_AES", "62000001");
    ProtocolDecryptExecutor executor = new StubExecutor("AES", buildPacket((byte) 2, """
            {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
            """));
    Object decoder = newDecoder(resolver, List.of(executor));

    Object decoded = decode(decoder, """
            {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
            """.getBytes(StandardCharsets.UTF_8));

    assertEquals("62000001", readAppId(decoded));
    assertEquals("STANDARD_TYPE_2", readDataFormatType(decoded));
}
```

- [ ] **Step 2: Run the legacy protocol tests to verify the helper signatures fail before the wiring change**

Run:

```bash
mvn -pl spring-boot-iot-protocol -am "-Dtest=LegacyDpEnvelopeDecoderTest,MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD FAILURE`
- test compile failures mentioning missing resolver/executor helper wiring

- [ ] **Step 3: Update `LegacyDpEnvelopeDecoder` to call the new resolve-context overload**

```java
String appId = extractAppId(payloadMap);
String encryptedBody = extractEncryptedBody(payloadMap);
mqttPayloadSecurityValidator.validateEnvelope(appId, payloadMap, encryptedBody);
MqttPayloadFrameParser.ParsedFrame decryptedFrame = mqttPayloadFrameParser.parse(
        "mqtt-json-decrypted",
        mqttPayloadDecryptorRegistry.decryptBytesOrThrow(
                new ProtocolDecryptResolveContext(appId, "mqtt-json", List.of()),
                encryptedBody
        )
);
```

Implementation rules:

1. Pass an empty family list here on purpose; encrypted legacy `$dp` envelopes do not expose family hints before decrypt.
2. Preserve current error surfaces for missing decrypt profile and missing algorithm executor.
3. Do not change decrypted payload parsing, file payload parsing, or raw payload logging.

- [ ] **Step 4: Update `MqttJsonProtocolAdapterTest` helper constructors only where needed**

```java
private static MqttJsonProtocolAdapter newAdapter(IotProperties properties,
                                                  ProtocolDecryptProfileResolver resolver,
                                                  List<ProtocolDecryptExecutor> executors,
                                                  LegacyDpRelationResolver relationResolver) {
    LegacyDpEnvelopeDecoder decoder = new LegacyDpEnvelopeDecoder(
            new MqttPayloadDecryptorRegistry(resolver, executors),
            new MqttPayloadFrameParser(),
            new MqttPayloadSecurityValidator(
                    properties,
                    new MqttMessageSignerRegistry(List.of()),
                    new DefaultListableBeanFactory().getBeanProvider(StringRedisTemplate.class)
            ),
            new MqttFirmwarePacketParser()
    );
    return new MqttJsonProtocolAdapter(decoder, properties, relationResolver);
}
```

Keep all existing assertions on:

1. `appId`
2. `familyCodes`
3. `decryptedPayloadPreview`
4. legacy `$dp` child split behavior

- [ ] **Step 5: Re-run the legacy protocol tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-protocol -am "-Dtest=LegacyDpEnvelopeDecoderTest,MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- existing legacy decode behavior remains green

- [ ] **Step 6: Commit the legacy compatibility wiring**

```bash
git add spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java
git commit -m "refactor: keep legacy dp decrypt path on resolver bridge"
```

## Task 4: Add YAML examples, document the new object layer, and verify package build

**Files:**
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-prod.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-test.yml`
- Modify: `docs/01-系统概览与架构说明.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Add minimal example configuration to the application YAML files**

```yaml
iot:
  protocol:
    family-definitions:
      legacy-dp-crack:
        family-code: legacy-dp-crack
        protocol-code: mqtt-json
        display-name: 裂缝 legacy dp
        decrypt-profile-code: aes-62000000
        sign-algorithm: AES
        normalization-strategy: legacy-dp-v1
        enabled: true
    decrypt-profiles:
      aes-62000000:
        profile-code: aes-62000000
        algorithm: AES
        merchant-source: SPRING_CLOUD_AES
        merchant-key: 62000000
        enabled: true
```

Implementation rules:

1. `application-dev.yml` should carry the concrete example.
2. `application-prod.yml` and `application-test.yml` may carry the same structure or empty map examples, but must expose the supported keys.
3. Do not duplicate merchant secrets into `decrypt-profiles`; they must remain references.

- [ ] **Step 2: Update docs to explain the new YAML-driven object layer**

```md
- `2026-04-09` 起，MQTT 协议安全配置新增 `iot.protocol.family-definitions` 与 `iot.protocol.decrypt-profiles`。
- `spring.cloud.aes.merchants` 与 `iot.protocol.crypto.merchants` 继续作为底层 merchant 真相源；`decrypt-profiles` 只做统一对象引用，不复制密钥。
- 当前 legacy `$dp` 加密链路在 pre-decrypt 阶段仍主要按 `appId` 回退解析 profile；resolver 已预留 `familyCodes` 优先路径，供后续协议族显式路由接入。
```

Doc update targets:

1. `docs/01-系统概览与架构说明.md`：补“协议族定义 + 解密配置对象”属于扩展模型底座
2. `docs/07-部署运行与配置说明.md`：补 YAML 结构与启动期校验规则
3. `docs/08-变更记录与技术债清单.md`：登记本轮协议对象化收口事实

Review rules:

1. Check whether `README.md` needs a short config note; update only if the top-level startup/config section would otherwise become stale.
2. Check `AGENTS.md`; leave unchanged unless workflow or required commands changed.

- [ ] **Step 3: Run the focused protocol verification suite**

Run:

```bash
mvn -pl spring-boot-iot-protocol,spring-boot-iot-framework -am "-Dtest=IotPropertiesProtocolDecryptProfileResolverTest,MqttPayloadDecryptorRegistryTest,LegacyDpEnvelopeDecoderTest,MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

- `BUILD SUCCESS`
- all new protocol config/resolver/decrypt tests pass

- [ ] **Step 4: Run docs topology and package verification**

Run:

```bash
node scripts/docs/check-topology.mjs
```

Expected:

- `Document topology check passed.`

Run:

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 5: Commit the config and documentation updates**

```bash
git add spring-boot-iot-admin/src/main/resources/application-dev.yml \
  spring-boot-iot-admin/src/main/resources/application-prod.yml \
  spring-boot-iot-admin/src/main/resources/application-test.yml \
  docs/01-系统概览与架构说明.md \
  docs/07-部署运行与配置说明.md \
  docs/08-变更记录与技术债清单.md
git commit -m "docs: document protocol family decrypt profile config"
```

## Self-Review Checklist

Spec coverage:

1. `ProtocolFamilyDefinition` object: covered in Task 1
2. `ProtocolDecryptProfile` object: covered in Task 1
3. Resolver/SPI path: covered in Tasks 1 and 2
4. Legacy `$dp` fallback compatibility: covered in Task 3
5. YAML examples and docs: covered in Task 4

Placeholder scan:

1. No `TODO`, `TBD`, or “similar to previous task” placeholders remain.
2. Every code-changing step includes concrete snippets and commands.

Type consistency:

1. Runtime context type is consistently `ProtocolDecryptResolveContext`
2. Resolver output type is consistently `ProtocolDecryptProfile`
3. Execution SPI type is consistently `ProtocolDecryptExecutor`
