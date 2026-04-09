# Governance Logic Delete Follow-Up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix remaining logic-delete gaps in system-governance help documents and in-app messages so delete success means the record is actually hidden from future reads.

**Architecture:** Reuse the same minimal pattern already applied to `用户/角色`: keep current existence and business-rule checks, then switch delete execution from manual `deleted=1` updates to MyBatis-Plus `removeById(...)`. Cover the behavior with focused service tests and verify the real environment side instance after packaging.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JUnit 5, Mockito.

---

## File Structure

### Backend files

- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/InAppMessageServiceImpl.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/HelpDocumentServiceImplTest.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/InAppMessageServiceImplTest.java`

### Documentation files

- Modify: `docs/08-变更记录与技术债清单.md`

## Task 1: Help Document Delete Red-Green

- [ ] Add a failing test in `HelpDocumentServiceImplTest` proving delete must use logic-delete execution rather than `updateById`.
- [ ] Run only that test and confirm it fails for the expected reason.
- [ ] Change `HelpDocumentServiceImpl#deleteDocument` to call `removeById(id)` after existing validation.
- [ ] Re-run the help-document test and confirm it passes.

## Task 2: In-App Message Delete Red-Green

- [ ] Add a failing test in `InAppMessageServiceImplTest` proving manual messages delete through logic-delete execution rather than `updateById`.
- [ ] Run only that test and confirm it fails for the expected reason.
- [ ] Change `InAppMessageServiceImpl#deleteMessage` to call `removeById(id)` after existing validation and auto-message guard.
- [ ] Re-run the in-app-message tests and confirm they pass.

## Task 3: Regression And Docs

- [ ] Update `docs/08-变更记录与技术债清单.md` with the new governance delete-path follow-up.
- [ ] Run the focused regression suite for `HelpDocumentServiceImplTest`, `InAppMessageServiceImplTest`, `PermissionServiceImplTest`, `UserServiceImplTest`, `RoleServiceImplTest`, and `OrganizationServiceImplTest`.
- [ ] Rebuild `spring-boot-iot-admin` with `-Dmaven.test.skip=true`.
- [ ] Restart the `10099` side instance on the fresh jar.
- [ ] Run a minimal real-environment delete verification and confirm deleted help-doc/message test data no longer appears in read APIs if test fixtures are created, or confirm service-level behavior is otherwise covered without mutating shared defaults.
