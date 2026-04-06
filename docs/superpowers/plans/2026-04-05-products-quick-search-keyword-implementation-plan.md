# Products Quick Search Keyword Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep a single `/products` quick-search input while making it search product name, product key, and manufacturer consistently across frontend state and backend pagination.

**Architecture:** Keep the existing product paging API signature and upgrade the current quick-search semantics in place: the `/products` page still reuses `productName` as its quick-search input, but both local filter logic and backend pagination now match product name, product key, and manufacturer through one OR-search path. Lock the behavior with focused frontend and backend tests before implementation.

**Tech Stack:** Vue 3, TypeScript, Vitest, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, Markdown docs

---

### Task 1: Lock the frontend quick-search contract with TDD

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/utils/productWorkbenchState.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing frontend tests**

```ts
expect(
  matchesProductFilters(product, {
    productName: 'accept-http',
    nodeType: 2,
    status: 0
  })
).toBe(true)

expect(
  matchesProductFilters(product, {
    productName: 'ghlzm',
    nodeType: 2,
    status: 0
  })
).toBe(true)
```

```ts
expect((wrapper.vm as any).activeFilterTags[0].label).toBe('快速搜索：demo-product')
```

- [ ] **Step 2: Run the frontend tests to verify they fail**

Run: `cd spring-boot-iot-ui && npx vitest run src/__tests__/utils/productWorkbenchState.test.ts src/__tests__/views/ProductWorkbenchView.test.ts`
Expected: FAIL because the current quick search still uses `productName` only.

### Task 2: Lock the backend three-field search contract with TDD

**Files:**
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`

- [ ] **Step 1: Write the failing backend test**

```java
PageResult<ProductPageVO> result = productService.pageProducts(null, "accept-http", null, null, null, 1L, 10L);
assertEquals(1L, result.getTotal());
```

```java
String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
assertTrue(sqlSegment.contains("product_name"));
assertTrue(sqlSegment.contains("product_key"));
assertTrue(sqlSegment.contains("manufacturer"));
assertTrue(sqlSegment.contains("OR"));
```

- [ ] **Step 2: Run the backend test to verify it fails**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductServiceImplTest test`
Expected: FAIL because the current query wrapper still only matches `product_name`.

### Task 3: Implement the unified quick-search behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/productWorkbenchState.ts`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`

- [ ] **Step 1: Expand the frontend local quick-search match and applied-filter copy**

```ts
if (
  keyword &&
  ![product.productName, product.productKey, product.manufacturer]
    .map((value) => String(value || '').toLowerCase())
    .some((value) => value.includes(keyword))
) {
  return false
}
```

```ts
tags.push({ key: 'productName', label: `快速搜索：${productName}` })
```

- [ ] **Step 2: Upgrade the backend `productName` quick-search to OR-search the three fields**

```java
if (StringUtils.hasText(productName)) {
    String trimmedKeyword = productName.trim();
    wrapper.and(query -> query.like(Product::getProductName, trimmedKeyword)
            .or()
            .like(Product::getProductKey, trimmedKeyword)
            .or()
            .like(Product::getManufacturer, trimmedKeyword));
}
```

- [ ] **Step 3: Update the quick-search copy**

```vue
placeholder="快速搜索（产品名称、产品 Key、厂商）"
```

### Task 4: Update the docs in place

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update the business/page wording**

```md
- 产品定义中心当前闭环：分页筛选、详情查看、新增、编辑、单删、CSV 导出，快速搜索已统一支持产品名称、产品 Key、厂商关键词，以及按产品查看关联设备库存。
```

- [ ] **Step 2: Update the interface wording**

```md
- `GET /api/device/product/page` 当前继续沿用 `productName` 作为 `/products` 页面快速搜索入参，但服务端已按 `product_name / product_key / manufacturer` 执行统一关键词模糊检索。
```

- [ ] **Step 3: Record the change in the active changelog**

```md
- 2026-04-05：`/products` 页面快速搜索已统一支持产品名称、产品 Key、厂商三项关键词，并通过产品分页现有 `productName` 搜索语义收口到服务端分页检索。
```

### Task 5: Verify the slice end to end

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/productWorkbenchState.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/productWorkbenchState.test.ts`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Run the focused frontend tests**

Run: `cd spring-boot-iot-ui && npx vitest run src/__tests__/utils/productWorkbenchState.test.ts src/__tests__/views/ProductWorkbenchView.test.ts`
Expected: PASS

- [ ] **Step 2: Run the focused backend test**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductServiceImplTest test`
Expected: PASS
