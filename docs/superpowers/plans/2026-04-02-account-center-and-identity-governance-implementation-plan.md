# 账号中心闭环与身份治理链路 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real shell account center, connect users to a primary organization plus role data scope, expand `/api/auth/me` into a reusable identity context, and make three row actions render directly instead of folding into `更多`.

**Architecture:** Keep the existing shell and governance visual language, but replace static account drawers with live data sourced from a richer auth context and a dedicated current-user profile update endpoint. Extend the system domain with a primary organization on `sys_user`, a `data_scope_type` on `sys_role`, and a minimal tenant read model, then wire governance pages and the shell to the same identity source of truth.

**Tech Stack:** Vue 3, Pinia, Element Plus, Vitest, Spring Boot 4, MyBatis-Plus, JUnit 5, Mockito, MySQL init SQL.

---

## File Structure

### Backend files

- Modify: `sql/init.sql`
  - Add `sys_user.org_id` and `sys_role.data_scope_type`, plus supporting indexes/comments.
- Modify: `sql/init-data.sql`
  - Seed role data scopes and primary organization bindings for demo users.
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Tenant.java`
  - Minimal read model for `sys_tenant`.
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/TenantMapper.java`
  - Read tenant summaries by id for auth context enrichment.
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/enums/DataScopeType.java`
  - Strongly typed role data-scope enum plus precedence helpers.
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/dto/UserProfileUpdateDTO.java`
  - Payload for current-user profile updates.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/User.java`
  - Map `nickname`, `avatar`, `isAdmin`, `remark`, `orgId`, and transient `orgName`.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Role.java`
  - Map `dataScopeType`.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/UserService.java`
  - Add current-user profile update contract.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/RoleService.java`
  - Keep type-safe role data scope through CRUD.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/PermissionService.java`
  - Expose any small helper needed for scope resolution only if required.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java`
  - Load/save primary organization and current-user profile fields.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/RoleServiceImpl.java`
  - Persist `dataScopeType`.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
  - Enrich auth context with tenant/org/login/security/scope summary.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/UserController.java`
  - Add `PUT /api/user/profile`.
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/UserAuthContextVO.java`
  - Add tenant/org/nickname/avatar/last-login/data-scope fields.
- Modify: `spring-boot-iot-auth/src/main/java/com/ghlzm/iot/auth/service/impl/AuthServiceImpl.java`
  - Keep login response aligned with expanded auth context.
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/PermissionServiceImplTest.java`
  - Cover enriched auth context and scope precedence.
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/UserServiceImplTest.java`
  - Cover profile update behavior and organization binding.

### Frontend files

- Modify: `spring-boot-iot-ui/src/api/user.ts`
  - Add profile update API and new user/org fields.
- Modify: `spring-boot-iot-ui/src/api/role.ts`
  - Add `dataScopeType` and `dataScopeSummary`.
- Modify: `spring-boot-iot-ui/src/types/auth.ts`
  - Expand `UserAuthContext`.
- Modify: `spring-boot-iot-ui/src/types/shell.ts`
  - Align account summary and drawer props with real profile sections.
- Modify: `spring-boot-iot-ui/src/stores/permission.ts`
  - Expose enriched user identity state from `/api/auth/me`.
- Modify: `spring-boot-iot-ui/src/components/AppHeaderTools.vue`
  - Reduce account actions to `账号中心 / 修改密码 / 退出登录`.
- Modify: `spring-boot-iot-ui/src/components/ShellAccountDrawers.vue`
  - Replace static drawers with one real account center drawer plus password drawer.
- Modify: `spring-boot-iot-ui/src/composables/useShellAccountCenter.ts`
  - Load/edit current-user profile and keep password/logout behavior.
- Modify: `spring-boot-iot-ui/src/views/UserView.vue`
  - Add organization, nickname, role binding, and updated action column behavior.
- Modify: `spring-boot-iot-ui/src/views/RoleView.vue`
  - Add data scope maintenance.
- Modify: `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`
  - Default to 3 direct actions before `更多`.
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
  - Align default `maxDirectItems`.
- Modify: `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`
  - Assert 3-direct / 4-folded behavior.
- Modify: `spring-boot-iot-ui/src/__tests__/components/AppHeaderTools.test.ts`
  - Assert header menu no longer shows standalone static auth/login-method entries.
- Modify: `spring-boot-iot-ui/src/__tests__/components/ShellAccountDrawers.test.ts`
  - Assert unified account center sections and Chinese copy.
- Create: `spring-boot-iot-ui/src/__tests__/views/UserView.test.ts`
  - Cover org/role form controls and three direct row actions.
- Create: `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`
  - Cover `dataScopeType` field rendering and submission.

### Documentation files

- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/15-前端优化与治理计划.md`

## Task 1: Backend Identity Foundation

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/enums/DataScopeType.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Tenant.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/TenantMapper.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/User.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Role.java`
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Test: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/PermissionServiceImplTest.java`

- [ ] **Step 1: Extend the auth-context test to describe the new identity fields**

```java
@Test
void shouldExposeTenantOrganizationAndScopeSummary() {
    Long userId = 1004L;
    User user = new User();
    user.setId(userId);
    user.setTenantId(1L);
    user.setOrgId(5001L);
    user.setUsername("manager-demo");
    user.setNickname("运营管理负责人");
    user.setRealName("管理演示账号");
    user.setPhone("13800000002");
    user.setEmail("manager_demo@ghlzm.com");
    user.setLastLoginIp("10.10.10.8");
    user.setDeleted(0);

    Role role = new Role();
    role.setId(3003L);
    role.setRoleCode("MANAGEMENT_STAFF");
    role.setRoleName("管理人员");
    role.setDataScopeType("ORG_AND_CHILDREN");

    when(userMapper.selectById(userId)).thenReturn(user);
    when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
    when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

    UserAuthContextVO context = permissionService.getUserAuthContext(userId);

    assertEquals(1L, context.getTenantId());
    assertEquals(5001L, context.getOrgId());
    assertEquals("运营管理负责人", context.getNickname());
    assertEquals("ORG_AND_CHILDREN", context.getDataScopeType());
    assertTrue(context.getDataScopeSummary().contains("机构"));
}
```

- [ ] **Step 2: Run the backend auth-context test and confirm it fails on missing fields**

Run: `mvn -pl spring-boot-iot-system -am test -Dtest=PermissionServiceImplTest`

Expected: FAIL with compile errors for missing `orgId`, `nickname`, `dataScopeType`, `dataScopeSummary`, or equivalent missing API.

- [ ] **Step 3: Add the domain types and schema fields needed by the test**

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/enums/DataScopeType.java
public enum DataScopeType {
    ALL(5, "全局"),
    TENANT(4, "租户内全部"),
    ORG_AND_CHILDREN(3, "本机构及下级"),
    ORG(2, "仅本机构"),
    SELF(1, "仅本人");

    private final int priority;
    private final String label;

    DataScopeType(int priority, String label) {
        this.priority = priority;
        this.label = label;
    }

    public int getPriority() { return priority; }
    public String getLabel() { return label; }

    public static DataScopeType fromCode(String code) {
        return Arrays.stream(values())
            .filter(item -> item.name().equalsIgnoreCase(code))
            .findFirst()
            .orElse(TENANT);
    }
}
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/User.java
private String nickname;
private String avatar;
private Integer isAdmin;
private String remark;
private Long orgId;

@TableField(exist = false)
private String orgName;
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Role.java
private String dataScopeType;
```

```sql
ALTER TABLE sys_user
    ADD COLUMN org_id BIGINT DEFAULT NULL COMMENT '主机构ID' AFTER avatar,
    ADD KEY idx_user_org_id (org_id);

ALTER TABLE sys_role
    ADD COLUMN data_scope_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '数据范围类型' AFTER description;
```

```sql
UPDATE sys_role SET data_scope_type = 'ALL' WHERE tenant_id = 1 AND role_code = 'SUPER_ADMIN';
UPDATE sys_role SET data_scope_type = 'ORG_AND_CHILDREN' WHERE tenant_id = 1 AND role_code = 'MANAGEMENT_STAFF';
UPDATE sys_role SET data_scope_type = 'SELF' WHERE tenant_id = 1 AND role_code = 'BUSINESS_STAFF';
UPDATE sys_role SET data_scope_type = 'TENANT' WHERE tenant_id = 1 AND role_code IN ('OPS_STAFF', 'DEVELOPER_STAFF');
```

- [ ] **Step 4: Re-run the auth-context test to confirm the new fields compile, even if assertions still fail**

Run: `mvn -pl spring-boot-iot-system -am test -Dtest=PermissionServiceImplTest`

Expected: FAIL moves from compile errors to assertion/runtime failures inside `PermissionServiceImpl`.

- [ ] **Step 5: Commit the foundation changes**

```bash
git add sql/init.sql sql/init-data.sql \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/enums/DataScopeType.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Tenant.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/mapper/TenantMapper.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/User.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Role.java \
  spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/PermissionServiceImplTest.java
git commit -m "feat: add identity governance schema foundation"
```

## Task 2: Backend Auth Context And Current-User Profile API

**Files:**
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/dto/UserProfileUpdateDTO.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/UserService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/PermissionService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/UserController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/UserAuthContextVO.java`
- Modify: `spring-boot-iot-auth/src/main/java/com/ghlzm/iot/auth/service/impl/AuthServiceImpl.java`
- Create: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/UserServiceImplTest.java`

- [ ] **Step 1: Write the failing service tests for profile update and enriched auth context**

```java
@Test
void shouldUpdateCurrentUserProfileWithoutChangingRolesOrStatus() {
    User existing = new User();
    existing.setId(1L);
    existing.setTenantId(1L);
    existing.setStatus(1);
    existing.setUsername("admin");
    existing.setNickname("旧昵称");
    existing.setOrgId(5001L);

    when(userMapper.selectById(1L)).thenReturn(existing);

    userService.updateCurrentUserProfile(1L, new UserProfileUpdateDTO(
        "新昵称", "超级管理员", "13800000000", "admin@ghlzm.com", "/avatars/admin.png"
    ));

    verify(userMapper).updateById(argThat(user ->
        Objects.equals(user.getId(), 1L)
            && Objects.equals(user.getNickname(), "新昵称")
            && Objects.equals(user.getUsername(), null)
            && Objects.equals(user.getStatus(), null)
    ));
}
```

```java
assertEquals("默认租户", context.getTenantName());
assertEquals("平台治理中心", context.getOrgName());
assertEquals("账号登录 / 手机号登录", String.join(" / ", context.getLoginMethods()));
assertEquals("本机构及下级", context.getDataScopeSummary());
```

- [ ] **Step 2: Run backend tests and confirm failure**

Run: `mvn -pl spring-boot-iot-system -am test -Dtest=PermissionServiceImplTest,UserServiceImplTest`

Expected: FAIL because `updateCurrentUserProfile`, tenant/org lookup, and scope-summary calculation do not exist yet.

- [ ] **Step 3: Implement the current-user profile API and richer auth context**

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/dto/UserProfileUpdateDTO.java
public record UserProfileUpdateDTO(
    String nickname,
    String realName,
    String phone,
    String email,
    String avatar
) {}
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/UserService.java
void updateCurrentUserProfile(Long userId, UserProfileUpdateDTO dto);
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/UserController.java
@PutMapping("/profile")
public R<Void> updateCurrentUserProfile(@RequestBody UserProfileUpdateDTO dto,
                                        Authentication authentication) {
    JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
    userService.updateCurrentUserProfile(principal.userId(), dto);
    return R.ok();
}
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/UserAuthContextVO.java
private Long tenantId;
private String tenantName;
private Long orgId;
private String orgName;
private String nickname;
private String avatar;
private Date lastLoginTime;
private String lastLoginIp;
private String dataScopeType;
private String dataScopeSummary;
```

```java
// spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java
context.setTenantId(user.getTenantId());
context.setTenantName(resolveTenantName(user.getTenantId()));
context.setOrgId(user.getOrgId());
context.setOrgName(resolveOrganizationName(user.getOrgId()));
context.setNickname(user.getNickname());
context.setAvatar(user.getAvatar());
context.setLastLoginTime(user.getLastLoginTime());
context.setLastLoginIp(user.getLastLoginIp());

DataScopeType scopeType = resolveHighestScope(roles);
context.setDataScopeType(scopeType.name());
context.setDataScopeSummary(scopeType.getLabel());
context.setDisplayName(StringUtils.hasText(user.getNickname())
    ? user.getNickname()
    : (StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername()));
```

- [ ] **Step 4: Run backend tests to confirm pass**

Run: `mvn -pl spring-boot-iot-system -am test -Dtest=PermissionServiceImplTest,UserServiceImplTest`

Expected: PASS for both tests.

- [ ] **Step 5: Commit the backend identity API**

```bash
git add \
  spring-boot-iot-auth/src/main/java/com/ghlzm/iot/auth/service/impl/AuthServiceImpl.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/dto/UserProfileUpdateDTO.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/UserService.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/PermissionService.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/UserController.java \
  spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/UserAuthContextVO.java \
  spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/UserServiceImplTest.java
git commit -m "feat: expose enriched auth context and profile api"
```

## Task 3: Governance User And Role Screens

**Files:**
- Modify: `spring-boot-iot-ui/src/api/user.ts`
- Modify: `spring-boot-iot-ui/src/api/role.ts`
- Modify: `spring-boot-iot-ui/src/views/UserView.vue`
- Modify: `spring-boot-iot-ui/src/views/RoleView.vue`
- Modify: `spring-boot-iot-ui/src/api/organization.ts` (only if helper conversion is needed)
- Create: `spring-boot-iot-ui/src/__tests__/views/UserView.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`

- [ ] **Step 1: Write the failing governance view tests**

```ts
it('renders three direct row actions for user rows', async () => {
  const wrapper = mount(UserView, { /* existing shared stubs */ })
  expect(wrapper.text()).toContain('编辑')
  expect(wrapper.text()).toContain('重置密码')
  expect(wrapper.text()).toContain('删除')
  expect(wrapper.text()).not.toContain('更多')
})

it('shows org and role form controls in the user drawer', async () => {
  const wrapper = mount(UserView, { /* stubs */ })
  expect(wrapper.text()).toContain('所属机构')
  expect(wrapper.text()).toContain('角色绑定')
})
```

```ts
it('shows data scope controls in the role drawer', async () => {
  const wrapper = mount(RoleView, { /* stubs */ })
  expect(wrapper.text()).toContain('数据范围')
  expect(wrapper.text()).toContain('本机构及下级')
})
```

- [ ] **Step 2: Run the failing frontend tests**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/views/UserView.test.ts src/__tests__/views/RoleView.test.ts`

Expected: FAIL because the governance screens do not yet render org/role/data-scope controls.

- [ ] **Step 3: Extend user/role API types and wire the governance forms**

```ts
// spring-boot-iot-ui/src/api/user.ts
export interface User {
  id?: IdType
  tenantId?: IdType
  orgId?: IdType
  orgName?: string
  username: string
  nickname?: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  remark?: string
  password?: string
  status: number
  roleIds?: number[]
  roleNames?: string[]
}

export const updateCurrentUserProfile = (data: {
  nickname?: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
}) => request<void>('/api/user/profile', { method: 'PUT', body: data })
```

```ts
// spring-boot-iot-ui/src/api/role.ts
export interface Role {
  id?: IdType
  tenantId?: IdType
  roleName: string
  roleCode: string
  description?: string
  dataScopeType?: 'ALL' | 'TENANT' | 'ORG_AND_CHILDREN' | 'ORG' | 'SELF'
  dataScopeSummary?: string
  status: number
  menuIds?: number[]
}
```

```vue
<!-- spring-boot-iot-ui/src/views/UserView.vue -->
<StandardTableTextColumn prop="orgName" label="所属机构" :width="180" />
<el-form-item label="显示名称" prop="nickname">
  <el-input v-model="formData.nickname" placeholder="请输入显示名称" />
</el-form-item>
<el-form-item label="所属机构" prop="orgId">
  <el-tree-select v-model="formData.orgId" :data="organizationTreeOptions" node-key="id" check-strictly />
</el-form-item>
<el-form-item label="角色绑定" prop="roleIds">
  <el-select v-model="formData.roleIds" multiple collapse-tags>
    <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="Number(role.id)" />
  </el-select>
</el-form-item>
```

```vue
<!-- spring-boot-iot-ui/src/views/RoleView.vue -->
<el-form-item label="数据范围" prop="dataScopeType">
  <el-select v-model="formData.dataScopeType" placeholder="请选择数据范围">
    <el-option label="全局" value="ALL" />
    <el-option label="租户内全部" value="TENANT" />
    <el-option label="本机构及下级" value="ORG_AND_CHILDREN" />
    <el-option label="仅本机构" value="ORG" />
    <el-option label="仅本人" value="SELF" />
  </el-select>
</el-form-item>
```

- [ ] **Step 4: Run the governance view tests to confirm pass**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/views/UserView.test.ts src/__tests__/views/RoleView.test.ts`

Expected: PASS.

- [ ] **Step 5: Commit governance screen changes**

```bash
git add \
  spring-boot-iot-ui/src/api/user.ts \
  spring-boot-iot-ui/src/api/role.ts \
  spring-boot-iot-ui/src/views/UserView.vue \
  spring-boot-iot-ui/src/views/RoleView.vue \
  spring-boot-iot-ui/src/__tests__/views/UserView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts
git commit -m "feat: extend user and role governance screens"
```

## Task 4: Shell Account Center Integration

**Files:**
- Modify: `spring-boot-iot-ui/src/types/auth.ts`
- Modify: `spring-boot-iot-ui/src/types/shell.ts`
- Modify: `spring-boot-iot-ui/src/stores/permission.ts`
- Modify: `spring-boot-iot-ui/src/components/AppHeaderTools.vue`
- Modify: `spring-boot-iot-ui/src/components/ShellAccountDrawers.vue`
- Modify: `spring-boot-iot-ui/src/composables/useShellAccountCenter.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/AppHeaderTools.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ShellAccountDrawers.test.ts`

- [ ] **Step 1: Write the failing shell tests**

```ts
expect(wrapper.text()).toContain('账号中心')
expect(wrapper.text()).toContain('修改密码')
expect(wrapper.text()).toContain('退出登录')
expect(wrapper.text()).not.toContain('实名认证')
expect(wrapper.text()).not.toContain('登录方式')
```

```ts
expect(wrapper.text()).toContain('基础资料')
expect(wrapper.text()).toContain('机构与角色')
expect(wrapper.text()).toContain('安全信息')
expect(wrapper.text()).toContain('实名资料')
```

- [ ] **Step 2: Run the shell component tests and confirm failure**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/AppHeaderTools.test.ts src/__tests__/components/ShellAccountDrawers.test.ts`

Expected: FAIL because the old shell still renders standalone `实名认证` and `登录方式`.

- [ ] **Step 3: Implement the unified account-center shell flow**

```ts
// spring-boot-iot-ui/src/types/auth.ts
export interface UserAuthContext {
  userId: number
  tenantId?: number
  tenantName?: string
  orgId?: number
  orgName?: string
  username: string
  nickname?: string
  realName?: string
  displayName?: string
  phone?: string
  email?: string
  avatar?: string
  lastLoginTime?: string
  lastLoginIp?: string
  dataScopeType?: string
  dataScopeSummary?: string
  accountType?: string
  authStatus?: string
  loginMethods?: string[]
  superAdmin: boolean
  homePath?: string
  roleCodes: string[]
  permissions: string[]
  roles: RoleSummary[]
  menus: MenuTreeNode[]
}
```

```ts
// spring-boot-iot-ui/src/stores/permission.ts
const displayName = computed(() => {
  if (!authContext.value) return ''
  return authContext.value.displayName
    || authContext.value.nickname
    || authContext.value.realName
    || authContext.value.username
})
```

```vue
<!-- spring-boot-iot-ui/src/components/AppHeaderTools.vue -->
<button type="button" class="account-panel__action" data-action="account-center" @click="emitAction('open-account-center')">
  <span>账号中心</span>
  <small>查看并维护当前账号资料、机构角色和安全信息</small>
</button>
<button type="button" class="account-panel__action" data-action="change-password" @click="emitAction('open-change-password')">
  <span>修改密码</span>
  <small>修改当前登录账号密码</small>
</button>
<button type="button" class="account-panel__action account-panel__action--danger" data-action="logout" @click="emitAction('logout')">
  <span>退出登录</span>
  <small>退出当前会话并返回登录页</small>
</button>
```

```vue
<!-- spring-boot-iot-ui/src/components/ShellAccountDrawers.vue -->
<section class="account-dialog__section">
  <h3>基础资料</h3>
  <!-- editable nickname / realName / phone / email -->
</section>
<section class="account-dialog__section">
  <h3>机构与角色</h3>
  <!-- tenantName / orgName / roleName / dataScopeSummary -->
</section>
<section class="account-dialog__section">
  <h3>安全信息</h3>
  <!-- loginMethods / lastLoginTime / lastLoginIp -->
</section>
<section class="account-dialog__section">
  <h3>实名资料</h3>
  <!-- authStatus + realName -->
</section>
```

```ts
// spring-boot-iot-ui/src/composables/useShellAccountCenter.ts
async function submitProfile() {
  await updateCurrentUserProfile({
    nickname: profileForm.nickname,
    realName: profileForm.realName,
    phone: profileForm.phone,
    email: profileForm.email,
    avatar: profileForm.avatar
  })
  await permissionStore.fetchCurrentUser()
  ElMessage.success('账号资料已更新')
}
```

- [ ] **Step 4: Run the shell tests to confirm pass**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/AppHeaderTools.test.ts src/__tests__/components/ShellAccountDrawers.test.ts`

Expected: PASS.

- [ ] **Step 5: Commit the shell integration**

```bash
git add \
  spring-boot-iot-ui/src/types/auth.ts \
  spring-boot-iot-ui/src/types/shell.ts \
  spring-boot-iot-ui/src/stores/permission.ts \
  spring-boot-iot-ui/src/components/AppHeaderTools.vue \
  spring-boot-iot-ui/src/components/ShellAccountDrawers.vue \
  spring-boot-iot-ui/src/composables/useShellAccountCenter.ts \
  spring-boot-iot-ui/src/__tests__/components/AppHeaderTools.test.ts \
  spring-boot-iot-ui/src/__tests__/components/ShellAccountDrawers.test.ts
git commit -m "feat: wire shell account center to real identity data"
```

## Task 5: Shared Row Action Behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`

- [ ] **Step 1: Write the failing action-splitting test for the new 3-direct rule**

```ts
it('keeps three row actions direct before folding into more', () => {
  expect(
    splitWorkbenchRowActions({
      directItems: [
        { command: 'edit', label: '编辑' },
        { command: 'reset-password', label: '重置密码' },
        { command: 'delete', label: '删除' }
      ]
    })
  ).toEqual({
    directItems: [
      { command: 'edit', label: '编辑' },
      { command: 'reset-password', label: '重置密码' },
      { command: 'delete', label: '删除' }
    ],
    menuItems: []
  })
})
```

- [ ] **Step 2: Run the utility test and confirm failure**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/utils/adaptiveActionColumn.test.ts`

Expected: FAIL because the current default still truncates to 2 direct actions.

- [ ] **Step 3: Implement the new default split behavior**

```ts
// spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts
const DEFAULT_MAX_DIRECT_ITEMS = 3

const WORKBENCH_TABLE_MIN_WIDTH_BY_VISIBLE_COUNT: Record<number, number> = {
  1: ACTION_MIN_WIDTH_PX,
  2: 112,
  3: 160,
  4: 208
}
```

```ts
// spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue
{
  menuLabel: '更多',
  maxDirectItems: 3
}
```

- [ ] **Step 4: Re-run the utility test to confirm pass**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/utils/adaptiveActionColumn.test.ts`

Expected: PASS with the three-direct assertion green.

- [ ] **Step 5: Commit the shared row-action change**

```bash
git add \
  spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts \
  spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue \
  spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts
git commit -m "feat: show three direct row actions before folding"
```

## Task 6: Documentation And Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/13-数据权限与多租户模型.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update docs to match the new account-center and identity behavior**

```md
- 右上角账号菜单当前统一收敛为 `账号中心 / 修改密码 / 退出登录`，`实名认证 / 登录方式` 不再作为独立静态入口。
- `GET /api/auth/me` 当前返回租户、主机构、角色、登录方式、最近登录与数据范围摘要。
- `PUT /api/user/profile` 当前用于当前登录用户维护显示名称、真实姓名、手机号、邮箱与头像。
- `账号中心` 当前以共享抽屉展示基础资料、机构与角色、安全信息、实名资料四个轻量区块。
- 平台治理 `用户管理` 当前补齐所属机构与角色绑定，`角色权限` 当前补齐数据范围维护。
- 列表操作当前默认 `<= 3` 项直出，`> 3` 项才折叠到 `更多`。
```

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/utils/adaptiveActionColumn.test.ts \
  src/__tests__/components/AppHeaderTools.test.ts \
  src/__tests__/components/ShellAccountDrawers.test.ts \
  src/__tests__/views/UserView.test.ts \
  src/__tests__/views/RoleView.test.ts
npm --prefix spring-boot-iot-ui run component:guard
npm --prefix spring-boot-iot-ui run list:guard
```

Expected: all targeted tests and guards PASS.

- [ ] **Step 3: Run focused backend verification**

Run:

```bash
mvn -pl spring-boot-iot-system -am test -Dtest=PermissionServiceImplTest,UserServiceImplTest
```

Expected: PASS, with no compile errors in `system` or `auth`.

- [ ] **Step 4: Run package-level real-environment build verification**

Run:

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit docs and final verification outputs**

```bash
git add README.md \
  docs/02-业务功能与流程说明.md \
  docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md \
  docs/08-变更记录与技术债清单.md \
  docs/13-数据权限与多租户模型.md \
  docs/15-前端优化与治理计划.md
git commit -m "docs: align account center identity governance"
```
