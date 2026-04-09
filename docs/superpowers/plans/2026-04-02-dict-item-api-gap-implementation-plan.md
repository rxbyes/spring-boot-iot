# Dict Item API Gap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the data dictionary item drawer by implementing the missing backend dict-item CRUD contract expected by the existing frontend.

**Architecture:** Keep the existing frontend contract unchanged and add the missing `/api/dict/{dictId}/items` and `/api/dict/items/{id}` endpoints in `spring-boot-iot-system`. Reuse `DictService`/`DictServiceImpl` for item orchestration so the fix stays focused on the current contract gap.

**Tech Stack:** Spring Boot, Spring MVC, MyBatis Plus, JUnit 5, Mockito, MockMvc

---

### Task 1: Reproduce the Missing GET Route

**Files:**
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/DictControllerTest.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/DictController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/DictService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldExposeDictItemListRoute() throws Exception {
    when(dictService.listDictItems(9L)).thenReturn(List.of(dictItem(1L, "red")));

    mockMvc.perform(get("/api/dict/9/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].itemValue").value("red"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictControllerTest#shouldExposeDictItemListRoute test`
Expected: FAIL because `/api/dict/{dictId}/items` is not mapped.

- [ ] **Step 3: Write minimal implementation**

```java
@GetMapping("/{dictId}/items")
public R<List<DictItem>> listDictItems(@PathVariable Long dictId) {
    return R.ok(dictService.listDictItems(dictId));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictControllerTest#shouldExposeDictItemListRoute test`
Expected: PASS

### Task 2: Implement Item CRUD Service Rules

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/DictService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/DictServiceImpl.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/DictServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void listDictItemsShouldReturnSortedItems() { ... }

@Test
void addDictItemShouldRejectDuplicateValueWithinSameDict() { ... }

@Test
void updateDictItemShouldRejectMissingItem() { ... }

@Test
void deleteDictItemShouldDelegateToRemoveById() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictServiceImplTest test`
Expected: FAIL because dict-item service methods do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
List<DictItem> listDictItems(Long dictId);
DictItem addDictItem(DictItem dictItem);
void updateDictItem(DictItem dictItem);
void deleteDictItem(Long id);
```

Implement:

- dict existence check through current `Dict` table
- item query ordered by `sortNo`, `id`
- duplicate `itemValue` check scoped to `tenantId + dictId`
- default `sortNo=0`, `status=1`
- `removeById(id)` for delete

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictServiceImplTest test`
Expected: PASS

### Task 3: Expose POST/PUT/DELETE Item Routes

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/DictController.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/DictControllerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldCreateDictItemUnderDictRoute() throws Exception { ... }

@Test
void shouldUpdateDictItemUnderDictRoute() throws Exception { ... }

@Test
void shouldDeleteDictItemByItemId() throws Exception { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictControllerTest test`
Expected: FAIL because item mutation routes are not mapped.

- [ ] **Step 3: Write minimal implementation**

```java
@PostMapping("/{dictId}/items")
public R<DictItem> addDictItem(@PathVariable Long dictId, @RequestBody DictItem dictItem) { ... }

@PutMapping("/{dictId}/items")
public R<DictItem> updateDictItem(@PathVariable Long dictId, @RequestBody DictItem dictItem) { ... }

@DeleteMapping("/items/{id}")
public R<Void> deleteDictItem(@PathVariable Long id) { ... }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictControllerTest test`
Expected: PASS

### Task 4: Update Documentation and Verify

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update docs**

Document the restored dict-item CRUD endpoints and note that the 2026-04-02 fix closes the frontend/backend contract gap in data dictionary item management.

- [ ] **Step 2: Run focused verification**

Run: `mvn -pl spring-boot-iot-system -Dtest=DictControllerTest,DictServiceImplTest test`
Expected: PASS

- [ ] **Step 3: Review diff**

Run: `git diff -- docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/DictController.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/DictService.java spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/DictServiceImpl.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/DictControllerTest.java spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/DictServiceImplTest.java`
Expected: Only dict-item gap fixes and docs updates appear.
