# Account Center User Management Bugfix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复账号中心与用户管理中的中文显示、机构树选择、多角色绑定和删除后分页总数异常。

**Architecture:** 后端拆分组织查询树与可写机构树，并把用户写入校验切到新的可写范围；前端在 `UserView` 中统一按字符串处理 Long 主键，避免 JS 精度丢失；同时补齐用户分页逻辑删除过滤与 UTF-8 编码硬化。

**Tech Stack:** Spring Boot 4、MyBatis-Plus、Vue 3、TypeScript、Element Plus、JUnit 5、Vitest

---

### Task 1: 先补后端失败测试

**Files:**
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/UserServiceImplTest.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/OrganizationServiceImplTest.java`

- [ ] Step 1: 为用户分页补 `deleted = 0` 过滤断言
- [ ] Step 2: 为“可写机构范围”补 scope 覆盖断言
- [ ] Step 3: 运行 `mvn -pl spring-boot-iot-system -Dtest=UserServiceImplTest,OrganizationServiceImplTest test`

### Task 2: 先补前端失败测试

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/UserView.test.ts`

- [ ] Step 1: 为 `UserView` 补“机构/角色 ID 不做 Number 强转”的断言
- [ ] Step 2: 为“用户表单使用 writable tree 接口”补断言
- [ ] Step 3: 运行 `npm test -- UserView.test.ts`

### Task 3: 实现后端修复

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/PermissionService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/OrganizationController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java`
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-prod.yml`
- Modify: `spring-boot-iot-admin/src/main/resources/application-test.yml`

- [ ] Step 1: 新增“可写机构 ID / 树”服务接口
- [ ] Step 2: 用户新增/编辑改用可写机构范围校验
- [ ] Step 3: 用户查询补 `deleted = 0`
- [ ] Step 4: 增加 UTF-8 编码硬化配置
- [ ] Step 5: 重跑后端测试

### Task 4: 实现前端修复

**Files:**
- Modify: `spring-boot-iot-ui/src/api/organization.ts`
- Modify: `spring-boot-iot-ui/src/views/UserView.vue`

- [ ] Step 1: 新增 writable tree API
- [ ] Step 2: `UserView` 切换到 writable tree
- [ ] Step 3: `UserView` 统一用字符串比较机构/角色 ID
- [ ] Step 4: 重跑前端测试

### Task 5: 文档与验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] Step 1: 回写用户管理主机构树与多角色修复口径
- [ ] Step 2: 回写 UTF-8 编码基线
- [ ] Step 3: 运行后端测试、前端测试和必要的真实环境接口核验
