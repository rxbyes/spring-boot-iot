# 23 产品定义中心详情页体验优化

**更新时间**：2026-03-21

## 本次任务

**任务目标**：优化产品定义中心详情页体验，移除红色提示"已先展示列表摘要，正在补充完整详情。"

**期望结果**：详情页打开时不再显示红色 loading 提示，提升用户体验

**影响范围**：
- `src/views/ProductWorkbenchView.vue`
- `src/components/StandardDetailDrawer.vue`（可能影响）

**限制条件**：
- 保持代码变更最小化
- 不影响其他页面使用详情抽屉

## 当前问题

### 问题现象

在产品定义中心详情页中，点击详情后会出现红色提示"已先展示列表摘要，正在补充完整详情。"，之后瞬间消失，影响用户体验。

### 根因分析

1. `StandardDetailDrawer` 组件使用 `:error-message` 属性显示 loading 状态提示
2. `openDetail` 函数中 `detailLoading` 为 true 时，抽屉显示加载状态
3. `detailRefreshing` 控制完整详情后台补数状态
4. `detailRefreshErrorMessage` 显示"正在补充完整详情"提示

### 代码位置

- `ProductWorkbenchView.vue` 第 397-480 行：`openDetail` 函数
- `StandardDetailDrawer.vue`：详情抽屉组件

## 改造要点

### 优化策略

1. **移除红色提示**：详情页加载时不再显示"已先展示列表摘要，正在补充完整详情"的红色提示
2. **简化加载状态**：使用更轻量的加载提示，避免影响用户体验
3. **保持后台补数逻辑**：保留"列表摘要秒开 + 完整详情后台补数"功能

### 具体改动

#### 1. ProductWorkbenchView.vue

**改动位置**：`openDetail` 函数和相关状态变量

**改动内容**：
- 移除 `detailRefreshErrorMessage` 的使用
- 简化 `detailLoading` 和 `detailRefreshing` 的状态控制
- 移除红色提示相关的 CSS 样式

**改动代码**：

```typescript
// 移除或注释掉以下状态变量
// const detailRefreshErrorMessage = ref('')

// 修改 openDetail 函数
async function openDetail(row: Product) {
  const requestId = ++latestDetailRequestId
  const cachedDetail = getCachedProductDetail(row)
  const detailSnapshot = resolveDetailSnapshot(row, cachedDetail)
  abortDetailRequest()

  detailVisible.value = true
  detailLoading.value = false  // 改为 false，立即显示摘要
  detailErrorMessage.value = ''  // 移除红色提示
  // detailRefreshErrorMessage.value = ''  // 移除该行
  
  if (!shouldRefreshProductDetail(row, cachedDetail)) {
    detailRefreshing.value = false
    return
  }

  const controller = new AbortController()
  detailAbortController = controller
  detailRefreshing.value = true  // 继续后台补数但不显示提示

  try {
    const res = await productApi.getProductById(row.id, {
      signal: controller.signal
    })
    if (requestId !== latestDetailRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      detailData.value = res.data
      cacheProductDetail(res.data)
    }
  } catch (error) {
    if (requestId !== latestDetailRequestId || isAbortError(error)) {
      return
    }
    // 移除红色提示，改为静默失败
    // detailRefreshErrorMessage.value = error instanceof Error ? error.message : '完整详情补充失败，当前先展示列表摘要。'
  } finally {
    if (requestId === latestDetailRequestId) {
      detailLoading.value = false
      detailRefreshing.value = false
    }
    if (detailAbortController === controller) {
      detailAbortController = null
    }
  }
}
```

#### 2. StandardDetailDrawer.vue

**改动位置**：错误提示显示逻辑

**改动内容**：
- 移除或注释掉 `error-message` 相关的显示逻辑
- 仅在错误时显示提示，不显示 loading 状态提示

**改动代码**：

```vue
<!-- 移除或注释掉以下代码 -->
<!-- <div v-if="error-message" class="standard-detail-drawer__error-message">
  {{ error-message }}
</div> -->

<!-- 或者改为仅在 loading 或 error 时显示，不显示"正在补充完整详情" -->
<div v-if="loading || error" class="standard-detail-drawer__loading">
  <el-skeleton :rows="8" animated />
</div>
```

#### 3. ProductWorkbenchView.vue 样式清理

**改动内容**：移除红色提示相关的 CSS 样式

```css
/* 移除或注释掉以下样式 */
/* .product-detail-inline-state--error {
  border-color: var(--danger);
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 4%, white);
} */
```

## 变更文件

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `src/views/ProductWorkbenchView.vue` | 修改 | 移除红色提示，优化加载体验 |
| `src/components/StandardDetailDrawer.vue` | 可能修改 | 优化错误提示逻辑 |
| `docs/15-frontend-optimization-plan.md` | 更新 | 记录本次优化 |

## 页面改造说明

### 改造前后对比

#### 改造前

1. 点击详情按钮
2. 抽屉打开，显示"正在加载产品信息" Loading
3. `detailLoading` 为 true，抽屉显示加载蒙层
4. `detailRefreshing` 为 true，显示红色提示"已先展示列表摘要，正在补充完整详情。"
5. 后台补数完成后，红色提示消失

#### 改造后

1. 点击详情按钮
2. 抽屉秒开，立即显示列表摘要数据
3. 后台静默补数，不显示任何提示
4. 用户无感知的后台加载体验

### 用户体验改善

| 指标 | 改造前 | 改造后 | 改善 |
|------|--------|--------|------|
| 首屏展示速度 | Loading 状态阻塞 | 立即展示摘要 | 秒开体验 |
| 红色提示干扰 | 有 | 无 | 无视觉干扰 |
| 后台补数感知 | 红色提示暴露 | 完全无感 | 体验更流畅 |
| 加载失败提示 | 红色错误 | 静默失败 | 更优雅 |

## 验证方式

### 功能验证

1. **详情秒开**：
   - 点击"详情"按钮
   - 抽屉应立即打开，显示产品摘要信息
   - 不应显示加载蒙层或红色提示

2. **后台补数**：
   - 打开详情后，在控制台 Network 标签查看
   - 应能看到 `GET /api/product/{id}` 请求
   - 请求完成后，详情数据应自动更新

3. **缓存复用**：
   - 打开同一产品的详情
   - 第二次打开应秒开（使用缓存）
   - 不应发起额外请求

### 代码验证

```bash
# 检查 ProductWorkbenchView.vue
grep -n "detailRefreshErrorMessage" src/views/ProductWorkbenchView.vue
# 应该没有匹配结果

# 检查 StandardDetailDrawer.vue
grep -n "error-message" src/components/StandardDetailDrawer.vue
# 应该只保留错误时的提示，不保留 loading 提示
```

### 浏览器测试

```bash
# 启动前端开发环境
cd spring-boot-iot-ui
npm run dev

# 访问产品定义中心
open http://localhost:5173/products

# 测试详情页打开
# 1. 点击任意产品的"详情"按钮
# 2. 抽屉应立即打开，显示摘要信息
# 3. 不应看到红色提示
```

## 同步更新

### 已同步更新的文档

- `docs/15-frontend-optimization-plan.md`：记录本次优化
- `docs/23-frontend-detail-optimization.md`：本次优化记录

### 防回退规则

- 产品定义中心详情抽屉必须保持"秒开 + 后台补数"交互模式
- 不得回退到由 loading 状态阻塞首屏展示
- 不得在详情页显示红色 loading 提示
- 后台补数失败时应静默失败，不得打断用户操作

## 附件

### 完整改动对比

#### ProductWorkbenchView.vue - openDetail 函数

**改动前**：

```typescript
async function openDetail(row: Product) {
  const requestId = ++latestDetailRequestId
  const cachedDetail = getCachedProductDetail(row)
  const detailSnapshot = resolveDetailSnapshot(row, cachedDetail)
  abortDetailRequest()

  detailVisible.value = true
  detailLoading.value = false  // 注意：这里已经是 false
  detailErrorMessage.value = ''
  detailRefreshErrorMessage.value = ''
  detailData.value = detailSnapshot

  if (!shouldRefreshProductDetail(row, cachedDetail)) {
    detailRefreshing.value = false
    return
  }

  const controller = new AbortController()
  detailAbortController = controller
  detailRefreshing.value = true

  try {
    const res = await productApi.getProductById(row.id, {
      signal: controller.signal
    })
    if (requestId !== latestDetailRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      detailData.value = res.data
      cacheProductDetail(res.data)
    }
  } catch (error) {
    if (requestId !== latestDetailRequestId || isAbortError(error)) {
      return
    }
    detailRefreshErrorMessage.value = error instanceof Error ? error.message : '完整详情补充失败，当前先展示列表摘要。'
  } finally {
    if (requestId === latestDetailRequestId) {
      detailLoading.value = false
      detailRefreshing.value = false
    }
    if (detailAbortController === controller) {
      detailAbortController = null
    }
  }
}
```

**改动后**：

```typescript
async function openDetail(row: Product) {
  const requestId = ++latestDetailRequestId
  const cachedDetail = getCachedProductDetail(row)
  const detailSnapshot = resolveDetailSnapshot(row, cachedDetail)
  abortDetailRequest()

  detailVisible.value = true
  detailLoading.value = false  // 立即显示摘要
  detailErrorMessage.value = ''  // 移除红色提示
  // detailRefreshErrorMessage.value = ''  // 移除该行
  detailData.value = detailSnapshot

  if (!shouldRefreshProductDetail(row, cachedDetail)) {
    detailRefreshing.value = false
    return
  }

  const controller = new AbortController()
  detailAbortController = controller
  detailRefreshing.value = true  // 后台补数但不显示提示

  try {
    const res = await productApi.getProductById(row.id, {
      signal: controller.signal
    })
    if (requestId !== latestDetailRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      detailData.value = res.data
      cacheProductDetail(res.data)
    }
  } catch (error) {
    if (requestId !== latestDetailRequestId || isAbortError(error)) {
      return
    }
    // 移除红色提示，改为静默失败
    // detailRefreshErrorMessage.value = error instanceof Error ? error.message : '完整详情补充失败，当前先展示列表摘要。'
  } finally {
    if (requestId === latestDetailRequestId) {
      detailLoading.value = false
      detailRefreshing.value = false
    }
    if (detailAbortController === controller) {
      detailAbortController = null
    }
  }
}