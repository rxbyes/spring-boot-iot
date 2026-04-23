# Navigation Menu Title Live Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让导航编排修改后的菜单名称能够在左侧工作台导航中按当前授权菜单实时显示，并在保存后立即刷新当前登录态菜单缓存。

**Architecture:** 左侧壳层导航继续复用现有“静态工作台骨架 + 后端授权树”的组合模式，但同一路径下改为以后端动态菜单名称优先，静态配置只保留排序与补充说明。导航编排页保存成功后，主动刷新 `permissionStore` 的当前用户授权上下文，避免旧菜单名继续停留在本地缓存里。

**Tech Stack:** Vue 3、Pinia、Vue Router、Vitest、Element Plus

---

### Task 1: 锁定左侧菜单名称覆盖规则

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useShellNavigation.test.ts`
- Modify: `spring-boot-iot-ui/src/composables/useShellNavigation.ts`

- [ ] 写失败测试，覆盖“同一路径时左侧导航名称优先使用后端 `menuName`”
- [ ] 运行该测试，确认当前实现失败
- [ ] 最小修改导航合并逻辑，让静态配置只负责顺序/补充信息，动态菜单负责名称
- [ ] 再次运行测试，确认通过

### Task 2: 锁定导航编排保存后的菜单缓存刷新

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/SystemGovernanceRefinementBatchTwo.test.ts`
- Modify: `spring-boot-iot-ui/src/views/MenuView.vue`

- [ ] 写失败测试，覆盖“编辑菜单保存成功后调用 `permissionStore.fetchCurrentUser()`”
- [ ] 运行该测试，确认当前实现失败
- [ ] 最小修改导航编排保存成功链路，补上当前登录态授权菜单刷新
- [ ] 再次运行测试，确认通过

### Task 3: 回归与文档

**Files:**
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] 运行导航与菜单编排相关前端定向测试
- [ ] 更新变更记录，说明菜单名称动态优先级与保存后的授权缓存刷新
- [ ] 自检 `git diff --check`
