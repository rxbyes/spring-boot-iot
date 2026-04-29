# Layered Threshold Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let bound monitoring metrics enter unified risk grading through device-specific, product-default, and existing metric/global fallback threshold policies.

**Architecture:** Extend `rule_definition` with optional scope columns while preserving existing metric-level rules. Runtime policy resolution checks enabled rules in precedence order: bound-device override, device override, product default, then legacy metric rule, and only then YAML auto-closure fallback. Governance missing-policy counts use the same precedence so product defaults remove bulk false-positive backlog.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JUnit 5, Mockito, MySQL schema SQL

---

### Task 1: Add Rule Scope Model

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RuleDefinition.java`
- Modify: `sql/init.sql`

- [ ] Add nullable fields `ruleScope`, `productId`, `deviceId`, and `riskPointDeviceId` to `RuleDefinition`.
- [ ] Add matching columns and indexes to `rule_definition` DDL.

### Task 2: Runtime Layered Policy Resolution

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/RiskPolicyResolverTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskPolicyResolver.java`

- [ ] Add failing tests for device-specific rules overriding product defaults, and product defaults preventing fallback to YAML.
- [ ] Implement scoped rule loading and precedence.
- [ ] Verify `RiskPolicyResolverTest`.

### Task 3: Governance Missing-Policy Counts

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`

- [ ] Add failing test proving a product default covers all bindings for that product metric.
- [ ] Reuse the same matching semantics in missing-policy filtering and overview coverage.
- [ ] Verify governance tests.

### Task 4: Validation and Docs

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] Ensure product scoped rules require `productId`, and device scoped rules require `deviceId` or `riskPointDeviceId`.
- [ ] Document the precedence: device personalized > product default > legacy metric rule > YAML fallback.
