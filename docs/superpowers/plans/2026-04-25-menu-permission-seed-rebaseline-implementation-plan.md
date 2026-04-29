# Menu Permission Seed Rebaseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the `sys_menu` and `sys_role_menu` initialization baseline so every implemented page, route-level workbench, visible button, and backend governance permission is represented and grantable.

**Architecture:** Add a static audit script that compares route, workspace, UI permission, backend governance permission, and SQL seed facts. Then rebaseline `sql/init-data.sql` in three bounded layers: page nodes, button nodes, and role grants. Finish by syncing docs and proving the new seed passes static and SQL/runtime checks without changing route guards or table schemas.

**Tech Stack:** MySQL seed SQL, Node.js static audit script, Spring Boot 4 / Java 17 source scanning, Vue 3 / TypeScript source scanning, Markdown docs.

---

## Scope Check

This plan implements the approved full rebaseline design in `docs/superpowers/specs/2026-04-25-menu-permission-seed-rebaseline-design.md`.

The plan covers:

1. A reusable static audit script for menu/permission seed drift.
2. `sql/init-data.sql` `sys_menu` page and button seed rebaseline.
3. `sql/init-data.sql` role grant rebaseline for business, management, ops, developer, and super-admin roles.
4. Docs updates in `docs/02`、`docs/04`、`docs/08`, plus review of `README.md` and `AGENTS.md`.
5. Static verification and real dev-profile verification commands.

The plan does not cover:

1. Changing `sys_menu` or `sys_role_menu` schema.
2. Changing Vue route guard behavior.
3. Changing `PermissionService` or authentication response shape.
4. Running H2 validation.
5. Refactoring unrelated dirty worktree changes.

## File Structure

### Create

- `scripts/audit-menu-permission-seed.mjs`
  - Static auditor for routes, section workspace paths, UI permission literals, backend governance constants, SQL menu rows, and known exclusions.

### Modify

- `sql/init-data.sql`
  - Rebaseline menu rows and role grants.
- `docs/02-业务功能与流程说明.md`
  - Document menu/workbench visibility and role-facing entry model.
- `docs/04-数据库设计与初始化数据.md`
  - Document the new menu/permission seed baseline and audit command.
- `docs/08-变更记录与技术债清单.md`
  - Record the rebaseline and known exclusion categories.

### Review Only

- `README.md`
  - Update only if the quick-start or project baseline description becomes stale.
- `AGENTS.md`
  - Update only if collaboration or seed-maintenance rules change.

## Execution Guardrails

- [ ] **Step 1: Confirm branch**

Run:

```bash
git branch --show-current
```

Expected:

```text
codex/dev
```

- [ ] **Step 2: Capture dirty state before editing**

Run:

```bash
git status --short
```

Expected: existing unrelated modifications may be present. Do not revert or stage unrelated files. This task owns only:

```text
scripts/audit-menu-permission-seed.mjs
sql/init-data.sql
docs/02-业务功能与流程说明.md
docs/04-数据库设计与初始化数据.md
docs/08-变更记录与技术债清单.md
README.md
AGENTS.md
```

README and AGENTS are review-only unless their content is stale after the seed change.

---

### Task 1: Add Static Menu Permission Auditor

**Files:**
- Create: `scripts/audit-menu-permission-seed.mjs`
- Test: run `node scripts/audit-menu-permission-seed.mjs`

- [ ] **Step 1: Write the failing auditor**

Create `scripts/audit-menu-permission-seed.mjs`:

```javascript
#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');

const routeExclusions = new Set([
  '/',
  '/login',
  '/risk-enhance'
]);

const permissionPrefixExclusions = [
  'iot:message-flow:',
  'iot:mqtt:consumer:leader',
  'iot:invalid-report:',
  'iot:observability:alerting:',
  'iot:telemetry:',
  'iot:protocol:replay',
  'iot:device:file',
  'iot:device:firmware',
  'iot:device:session',
  'iot:device:offline-timeout:leader',
  'iot:shell-notice-sync',
  'iot:shell-notice-sync-event'
];

const permissionExactExclusions = new Set([
  'system:root',
  'system:menu:refresh'
]);

function read(file) {
  return fs.readFileSync(path.join(repoRoot, file), 'utf8');
}

function walk(dir, predicate) {
  const result = [];
  const stack = [path.join(repoRoot, dir)];
  while (stack.length > 0) {
    const current = stack.pop();
    if (!current || !fs.existsSync(current)) {
      continue;
    }
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) {
        if (!['node_modules', 'target', 'logs', '.git'].includes(entry.name)) {
          stack.push(full);
        }
        continue;
      }
      if (predicate(full)) {
        result.push(full);
      }
    }
  }
  return result;
}

function extractRouteRecords() {
  const router = read('spring-boot-iot-ui/src/router/index.ts');
  const records = [];
  const routeRegex = /\{\s*path:\s*'([^']+)'([\s\S]*?)\n\s*\}/g;
  for (const match of router.matchAll(routeRegex)) {
    const routePath = match[1];
    const body = match[2];
    const redirect = /redirect\s*:/.test(body);
    records.push({ path: routePath, redirect });
  }
  return records;
}

function extractSectionPaths() {
  const workspaces = read('spring-boot-iot-ui/src/utils/sectionWorkspaces.ts');
  const paths = new Set();
  for (const match of workspaces.matchAll(/\bpath:\s*'([^']+)'/g)) {
    const value = match[1];
    if (value.startsWith('/')) {
      paths.add(value);
    }
  }
  return paths;
}

function extractSqlMenuRows() {
  const sql = read('sql/init-data.sql');
  const rows = [];
  const rowRegex = /\((\d+),\s*1,\s*([^,]+),\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',[\s\S]*?\s(\d+),\s*(\d+),\s*(\d+),\s*'([^']*)',\s*'([^']*)',/g;
  for (const match of sql.matchAll(rowRegex)) {
    rows.push({
      id: Number(match[1]),
      parentId: match[2].trim(),
      menuName: match[3],
      menuCode: match[4],
      path: match[5],
      component: match[6],
      sort: Number(match[7]),
      type: Number(match[8]),
      menuType: Number(match[9]),
      routePath: match[10],
      permission: match[11]
    });
  }
  return rows;
}

function extractUiPermissionLiterals() {
  const files = walk('spring-boot-iot-ui/src', (file) => /\.(vue|ts)$/.test(file));
  const permissions = new Map();
  for (const file of files) {
    const text = fs.readFileSync(file, 'utf8');
    const relative = path.relative(repoRoot, file);
    const patterns = [
      /v-permission\s*=\s*(?:'|")'([^'"]+)'(?:'|")/g,
      /hasPermission\(\s*(?:'|")([^'"]+)(?:'|")\s*\)/g
    ];
    for (const pattern of patterns) {
      for (const match of text.matchAll(pattern)) {
        addPermission(permissions, match[1], relative, text, match.index || 0);
      }
    }
  }
  return permissions;
}

function extractGovernancePermissionConstants() {
  const file = 'spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java';
  const text = read(file);
  const permissions = new Map();
  for (const match of text.matchAll(/=\s*"([^"]+)"/g)) {
    addPermission(permissions, match[1], file, text, match.index || 0);
  }
  return permissions;
}

function addPermission(map, permission, file, text, index) {
  if (!/^(iot|risk|system):[A-Za-z0-9:_-]+$/.test(permission)) {
    return;
  }
  if (isExcludedPermission(permission)) {
    return;
  }
  if (!map.has(permission)) {
    map.set(permission, []);
  }
  const line = text.slice(0, index).split('\n').length;
  map.get(permission).push(`${file}:${line}`);
}

function isExcludedPermission(permission) {
  if (permissionExactExclusions.has(permission)) {
    return true;
  }
  if (/:\d{6,}:/.test(permission)) {
    return true;
  }
  return permissionPrefixExclusions.some((prefix) => permission === prefix || permission.startsWith(prefix));
}

function mergePermissionMaps(...maps) {
  const merged = new Map();
  for (const map of maps) {
    for (const [permission, refs] of map.entries()) {
      if (!merged.has(permission)) {
        merged.set(permission, []);
      }
      merged.get(permission).push(...refs);
    }
  }
  return merged;
}

function isDynamicRoute(pathValue) {
  return pathValue.includes(':');
}

function main() {
  const menuRows = extractSqlMenuRows();
  const menuPaths = new Set(menuRows.filter((row) => row.type !== 2 && row.path).map((row) => row.path));
  const menuPermissions = new Set(menuRows.filter((row) => row.permission).map((row) => row.permission));
  const buttonPermissions = new Set(menuRows.filter((row) => row.type === 2 && row.permission).map((row) => row.permission));

  const routes = extractRouteRecords();
  const sectionPaths = extractSectionPaths();
  const requiredStaticRoutes = routes
    .filter((route) => !route.redirect)
    .map((route) => route.path)
    .filter((routePath) => !routeExclusions.has(routePath))
    .filter((routePath) => !isDynamicRoute(routePath));
  const missingStaticRoutes = requiredStaticRoutes.filter((routePath) => !menuPaths.has(routePath));

  const missingSectionPaths = Array.from(sectionPaths)
    .filter((routePath) => !routeExclusions.has(routePath))
    .filter((routePath) => !menuPaths.has(routePath));

  const requiredDynamicRoutes = [
    '/products/:productId/overview',
    '/products/:productId/devices',
    '/products/:productId/contracts',
    '/products/:productId/mapping-rules',
    '/products/:productId/releases',
    '/business-acceptance/results/:runId'
  ];
  const missingDynamicRoutes = requiredDynamicRoutes.filter((routePath) => !menuPaths.has(routePath));

  const codePermissions = mergePermissionMaps(
    extractUiPermissionLiterals(),
    extractGovernancePermissionConstants()
  );
  const missingPermissions = Array.from(codePermissions.keys())
    .filter((permission) => !buttonPermissions.has(permission) && !menuPermissions.has(permission))
    .map((permission) => ({
      permission,
      refs: codePermissions.get(permission).slice(0, 5)
    }));

  const failures = [];
  if (missingStaticRoutes.length > 0) {
    failures.push(['Missing static route menu paths', missingStaticRoutes]);
  }
  if (missingSectionPaths.length > 0) {
    failures.push(['Missing section workspace menu paths', missingSectionPaths]);
  }
  if (missingDynamicRoutes.length > 0) {
    failures.push(['Missing required dynamic route menu paths', missingDynamicRoutes]);
  }
  if (missingPermissions.length > 0) {
    failures.push(['Missing permission menu nodes', missingPermissions]);
  }

  const summary = {
    menuRowCount: menuRows.length,
    pageNodeCount: menuRows.filter((row) => row.type !== 2).length,
    buttonNodeCount: menuRows.filter((row) => row.type === 2).length,
    routeCount: routes.length,
    requiredStaticRouteCount: requiredStaticRoutes.length,
    codePermissionCount: codePermissions.size,
    failures
  };

  if (failures.length > 0) {
    console.error(JSON.stringify(summary, null, 2));
    process.exit(1);
  }

  console.log(JSON.stringify(summary, null, 2));
}

main();
```

- [ ] **Step 2: Run the auditor and confirm it currently fails**

Run:

```bash
node scripts/audit-menu-permission-seed.mjs
```

Expected: non-zero exit with failures including at least:

```text
/device-access
/risk-disposal
/risk-config
/system-management
/quality-workbench
/products/:productId/overview
/products/:productId/contracts
/business-acceptance/results/:runId
iot:device-capability:view
```

- [ ] **Step 3: Commit only the auditor when it fails for the expected current gaps**

Run:

```bash
git add scripts/audit-menu-permission-seed.mjs
git commit -m "test: add menu permission seed audit"
```

Expected: one commit containing only the new audit script.

---

### Task 2: Rebaseline Page Menu Nodes

**Files:**
- Modify: `sql/init-data.sql`
- Test: `node scripts/audit-menu-permission-seed.mjs`

- [ ] **Step 1: Add explicit workbench overview and hidden deep-link page rows**

In the first `INSERT INTO sys_menu` block that seeds page nodes, add these rows. Keep existing root rows and existing page rows intact unless the same `menu_code` already exists.

```sql
    (93000011, 1, 93000001, '智维总览', 'iot-access:overview', '/device-access', 'SectionLandingView', 'connection', '{"caption":"查看接入智维分组能力与标准排障路径","hiddenInSidebar":false}', 9, 1, 1, '/device-access', 'iot-access:overview', 9, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000012, 1, 93000002, '运营总览', 'risk:ops-overview', '/risk-disposal', 'SectionLandingView', 'warning', '{"caption":"查看风险运营主链路与能力入口","hiddenInSidebar":false}', 19, 1, 1, '/risk-disposal', 'risk:ops-overview', 19, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000013, 1, 93000004, '策略总览', 'risk:config-overview', '/risk-config', 'SectionLandingView', 'operation', '{"caption":"查看风险策略分组能力与配置入口","hiddenInSidebar":false}', 29, 1, 1, '/risk-config', 'risk:config-overview', 29, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000014, 1, 93000003, '治理总览', 'system:governance-overview', '/system-management', 'SectionLandingView', 'setting', '{"caption":"查看平台治理分组能力与常用入口","hiddenInSidebar":false}', 39, 1, 1, '/system-management', 'system:governance-overview', 39, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000015, 1, 93000005, '工场总览', 'system:quality-workbench-overview', '/quality-workbench', 'QualityWorkbenchLandingView', 'monitor', '{"caption":"查看质量工场能力与专项入口","hiddenInSidebar":false}', 49, 1, 1, '/quality-workbench', 'system:quality-workbench-overview', 49, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000016, 1, 93000005, '演进蓝图', 'system:future-lab', '/future-lab', 'FutureLabView', 'guide', '{"caption":"预研能力展示与未来扩展方向说明","hiddenInSidebar":true}', 69, 1, 1, '/future-lab', 'system:future-lab', 69, 0, 1, 1, NOW(), 1, NOW(), 0),
```

Then add the dynamic/deep-link page rows near their parent page rows:

```sql
    (93001065, 1, 93001001, '产品总览', 'iot:products:detail-overview', '/products/:productId/overview', 'ProductDetailWorkbenchView', 'box', '{"caption":"查看产品概览、正式字段规模与最新合同发布状态","hiddenInSidebar":true}', 1165, 1, 1, '/products/:productId/overview', 'iot:products:detail-overview', 1165, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93001066, 1, 93001001, '产品关联设备', 'iot:products:detail-devices', '/products/:productId/devices', 'ProductDetailWorkbenchView', 'cpu', '{"caption":"查看当前产品下的设备清单、在线状态与最近上报","hiddenInSidebar":true}', 1166, 1, 1, '/products/:productId/devices', 'iot:products:detail-devices', 1166, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93001067, 1, 93001001, '契约字段', 'iot:products:detail-contracts', '/products/:productId/contracts', 'ProductDetailWorkbenchView', 'document', '{"caption":"样本输入、识别结果、本次生效与当前已生效字段","hiddenInSidebar":true}', 1167, 1, 1, '/products/:productId/contracts', 'iot:products:detail-contracts', 1167, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93001068, 1, 93001001, '映射规则', 'iot:products:detail-mapping-rules', '/products/:productId/mapping-rules', 'ProductDetailWorkbenchView', 'connection', '{"caption":"厂商字段映射建议、映射规则台账和运行态名称单位治理","hiddenInSidebar":true}', 1168, 1, 1, '/products/:productId/mapping-rules', 'iot:products:detail-mapping-rules', 1168, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93001069, 1, 93001001, '版本台账', 'iot:products:detail-releases', '/products/:productId/releases', 'ProductDetailWorkbenchView', 'tickets', '{"caption":"发布批次、回滚试算与跨批次差异对账","hiddenInSidebar":true}', 1169, 1, 1, '/products/:productId/releases', 'iot:products:detail-releases', 1169, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93003715, 1, 93003020, '业务验收结果', 'system:business-acceptance:result', '/business-acceptance/results/:runId', 'BusinessAcceptanceResultView', 'finished', '{"caption":"查看业务验收包的模块结论与失败明细","hiddenInSidebar":true}', 5715, 1, 1, '/business-acceptance/results/:runId', 'system:business-acceptance:result', 5715, 0, 1, 1, NOW(), 1, NOW(), 0),
```

- [ ] **Step 2: Hide compatibility page entries from the main sidebar**

Update the existing rows for `/automation-assets` and `/automation-test` to keep them active but hidden:

```sql
    (93003012, 1, 93000005, '自动化资产中心（兼容入口）', 'system:automation-assets', '/automation-assets', 'AutomationAssetsView', 'document', '{"caption":"兼容旧入口，第一轮直接落到研发工场总览","hiddenInSidebar":true}', 59, 1, 1, '/automation-assets', 'system:automation-assets', 59, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93003009, 1, 93000005, '自动化工场（兼容入口）', 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor', '{"caption":"兼容旧入口，第一轮直接落到研发工场总览","hiddenInSidebar":true}', 60, 1, 1, '/automation-test', 'system:automation-test', 60, 0, 1, 1, NOW(), 1, NOW(), 0),
```

- [ ] **Step 3: Verify page rows are now present**

Run:

```bash
node scripts/audit-menu-permission-seed.mjs
```

Expected: route/path failures for the page rows added in this task are gone. Permission failures may remain until Task 3.

- [ ] **Step 4: Commit page node rebaseline**

Run:

```bash
git add sql/init-data.sql
git commit -m "feat: rebaseline menu page seed"
```

Expected: commit contains only `sql/init-data.sql` page-node changes.

---

### Task 3: Rebaseline Button Permission Nodes

**Files:**
- Modify: `sql/init-data.sql`
- Test: `node scripts/audit-menu-permission-seed.mjs`

- [ ] **Step 1: Add missing high-confidence button permissions**

Add `iot:device-capability:view` under 设备资产中心:

```sql
    (93001070, 1, 93001002, '设备操作', 'iot:device-capability:view', '', '', '', '{"caption":"打开设备操作抽屉并查看可执行能力"}', 1270, 2, 2, '', 'iot:device-capability:view', 1270, 1, 1, 1, NOW(), 1, NOW(), 0),
```

Add the risk binding governance permissions currently defined in `GovernancePermissionCodes` but not represented in the seed:

```sql
    (93002055, 1, 93002003, '风险绑定执行', 'risk:risk-point-binding:execute', '', '', '', '{"caption":"执行风险点正式绑定治理动作"}', 3155, 2, 2, '', 'risk:risk-point-binding:execute', 3155, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002056, 1, 93002003, '风险绑定复核', 'risk:risk-point-binding:approve', '', '', '', '{"caption":"复核风险点正式绑定治理动作"}', 3156, 2, 2, '', 'risk:risk-point-binding:approve', 3156, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002057, 1, 93002003, '待治理转正执行', 'risk:risk-point-pending-promotion:execute', '', '', '', '{"caption":"执行待治理绑定转正动作"}', 3157, 2, 2, '', 'risk:risk-point-pending-promotion:execute', 3157, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002058, 1, 93002003, '待治理转正复核', 'risk:risk-point-pending-promotion:approve', '', '', '', '{"caption":"复核待治理绑定转正动作"}', 3158, 2, 2, '', 'risk:risk-point-pending-promotion:approve', 3158, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002059, 1, 93002005, '联动预案复核', 'risk:linkage-plan:approve', '', '', '', '{"caption":"复核联动预案关键治理动作"}', 3359, 2, 2, '', 'risk:linkage-plan:approve', 3359, 1, 1, 1, NOW(), 1, NOW(), 0),
```

- [ ] **Step 2: Keep deprecated coarse risk write permissions soft-deleted**

Preserve the existing soft-delete block for:

```sql
risk:rule-definition:write
risk:linkage-rule:write
risk:emergency-plan:write
```

Expected: these coarse permissions remain `deleted=1, visible=0, status=0` after the seed runs, while the fine-grained `:edit` and `:approve` permissions remain active.

- [ ] **Step 3: Run the auditor and confirm permission gaps are gone**

Run:

```bash
node scripts/audit-menu-permission-seed.mjs
```

Expected: exit code `0`, and the JSON summary contains:

```json
{
  "failures": []
}
```

- [ ] **Step 4: Commit button node rebaseline**

Run:

```bash
git add sql/init-data.sql
git commit -m "feat: rebaseline menu button seed"
```

Expected: commit contains only the button-node changes in `sql/init-data.sql`.

---

### Task 4: Rebaseline Role Grants

**Files:**
- Modify: `sql/init-data.sql`
- Test: SQL static review plus optional dev database check.

- [ ] **Step 1: Replace brittle role-menu ID lists with menu-code scoped grants**

After all `sys_menu` rows have been inserted and soft-deleted rows have been marked, add a role grant rebaseline block that grants by `menu_code` instead of hardcoding only `menu_id` values.

Use this structure:

```sql
DELETE FROM sys_role_menu
WHERE role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id, @role_super_admin_id);

SET @role_menu_rebaseline_id := 96010000;

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_rebaseline_id := @role_menu_rebaseline_id + 1),
       1,
       grant_scope.role_id,
       m.id,
       1,
       NOW(),
       1,
       NOW(),
       0
FROM (
    SELECT @role_business_id AS role_id, 'risk-ops' AS menu_code
    UNION ALL SELECT @role_business_id, 'risk:ops-overview'
    UNION ALL SELECT @role_business_id, 'risk:monitoring'
    UNION ALL SELECT @role_business_id, 'risk:monitoring-gis'
    UNION ALL SELECT @role_business_id, 'risk:alarm'
    UNION ALL SELECT @role_business_id, 'risk:alarm:detail'
    UNION ALL SELECT @role_business_id, 'risk:alarm:confirm'
    UNION ALL SELECT @role_business_id, 'risk:alarm:suppress'
    UNION ALL SELECT @role_business_id, 'risk:alarm:close'
    UNION ALL SELECT @role_business_id, 'risk:event'
    UNION ALL SELECT @role_business_id, 'risk:event:detail'
    UNION ALL SELECT @role_business_id, 'risk:event:dispatch'
    UNION ALL SELECT @role_business_id, 'risk:event:close'
    UNION ALL SELECT @role_business_id, 'iot:insight'
    UNION ALL SELECT @role_business_id, 'risk:report'
    UNION ALL SELECT @role_business_id, 'quality-workbench'
    UNION ALL SELECT @role_business_id, 'system:quality-workbench-overview'
    UNION ALL SELECT @role_business_id, 'system:business-acceptance'
    UNION ALL SELECT @role_business_id, 'system:business-acceptance:launch'
    UNION ALL SELECT @role_business_id, 'system:business-acceptance:open-result'
    UNION ALL SELECT @role_business_id, 'system:business-acceptance:result'
    UNION ALL SELECT @role_business_id, 'iot-access'
    UNION ALL SELECT @role_business_id, 'iot:products'
    UNION ALL SELECT @role_business_id, 'iot:products:detail-overview'
    UNION ALL SELECT @role_business_id, 'iot:products:detail-devices'
    UNION ALL SELECT @role_business_id, 'iot:products:export'
    UNION ALL SELECT @role_business_id, 'iot:devices'
    UNION ALL SELECT @role_business_id, 'iot:devices:detail'
    UNION ALL SELECT @role_business_id, 'iot:devices:export'
    UNION ALL SELECT @role_business_id, 'iot:devices:insight'

    UNION ALL SELECT @role_management_id, 'risk-ops'
    UNION ALL SELECT @role_management_id, 'risk:ops-overview'
    UNION ALL SELECT @role_management_id, 'risk:monitoring'
    UNION ALL SELECT @role_management_id, 'risk:monitoring-gis'
    UNION ALL SELECT @role_management_id, 'risk:alarm'
    UNION ALL SELECT @role_management_id, 'risk:event'
    UNION ALL SELECT @role_management_id, 'iot:insight'
    UNION ALL SELECT @role_management_id, 'risk:report'
    UNION ALL SELECT @role_management_id, 'risk-config'
    UNION ALL SELECT @role_management_id, 'risk:config-overview'
    UNION ALL SELECT @role_management_id, 'risk:point'
    UNION ALL SELECT @role_management_id, 'risk:rule-definition'
    UNION ALL SELECT @role_management_id, 'risk:linkage-rule'
    UNION ALL SELECT @role_management_id, 'risk:emergency-plan'
    UNION ALL SELECT @role_management_id, 'system-governance'
    UNION ALL SELECT @role_management_id, 'system:governance-overview'
    UNION ALL SELECT @role_management_id, 'system:organization'
    UNION ALL SELECT @role_management_id, 'system:user'
    UNION ALL SELECT @role_management_id, 'system:role'
    UNION ALL SELECT @role_management_id, 'system:menu'
    UNION ALL SELECT @role_management_id, 'system:region'
    UNION ALL SELECT @role_management_id, 'system:dict'
    UNION ALL SELECT @role_management_id, 'system:channel'
    UNION ALL SELECT @role_management_id, 'system:in-app-message'
    UNION ALL SELECT @role_management_id, 'system:help-doc'
    UNION ALL SELECT @role_management_id, 'system:governance-task'
    UNION ALL SELECT @role_management_id, 'system:governance-ops'
    UNION ALL SELECT @role_management_id, 'system:governance-approval'
    UNION ALL SELECT @role_management_id, 'system:governance-security'
    UNION ALL SELECT @role_management_id, 'system:audit'
    UNION ALL SELECT @role_management_id, 'iot-access'
    UNION ALL SELECT @role_management_id, 'iot-access:overview'
    UNION ALL SELECT @role_management_id, 'iot:device-onboarding'
    UNION ALL SELECT @role_management_id, 'iot:protocol-governance'
    UNION ALL SELECT @role_management_id, 'iot:products'
    UNION ALL SELECT @role_management_id, 'iot:devices'

    UNION ALL SELECT @role_ops_id, 'iot-access'
    UNION ALL SELECT @role_ops_id, 'iot-access:overview'
    UNION ALL SELECT @role_ops_id, 'iot:device-onboarding'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance'
    UNION ALL SELECT @role_ops_id, 'iot:products'
    UNION ALL SELECT @role_ops_id, 'iot:devices'
    UNION ALL SELECT @role_ops_id, 'iot:reporting'
    UNION ALL SELECT @role_ops_id, 'iot:system-log'
    UNION ALL SELECT @role_ops_id, 'iot:message-trace'
    UNION ALL SELECT @role_ops_id, 'iot:file-debug'
    UNION ALL SELECT @role_ops_id, 'risk-ops'
    UNION ALL SELECT @role_ops_id, 'risk:ops-overview'
    UNION ALL SELECT @role_ops_id, 'risk:monitoring'
    UNION ALL SELECT @role_ops_id, 'risk:alarm'
    UNION ALL SELECT @role_ops_id, 'risk:event'
    UNION ALL SELECT @role_ops_id, 'iot:insight'
    UNION ALL SELECT @role_ops_id, 'system-governance'
    UNION ALL SELECT @role_ops_id, 'system:governance-security'

    UNION ALL SELECT @role_developer_id, 'iot-access'
    UNION ALL SELECT @role_developer_id, 'iot-access:overview'
    UNION ALL SELECT @role_developer_id, 'iot:device-onboarding'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance'
    UNION ALL SELECT @role_developer_id, 'iot:products'
    UNION ALL SELECT @role_developer_id, 'iot:devices'
    UNION ALL SELECT @role_developer_id, 'iot:reporting'
    UNION ALL SELECT @role_developer_id, 'iot:system-log'
    UNION ALL SELECT @role_developer_id, 'iot:message-trace'
    UNION ALL SELECT @role_developer_id, 'iot:file-debug'
    UNION ALL SELECT @role_developer_id, 'quality-workbench'
    UNION ALL SELECT @role_developer_id, 'system:quality-workbench-overview'
    UNION ALL SELECT @role_developer_id, 'system:rd-workbench'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-inventory'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-templates'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-plans'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-handoff'
    UNION ALL SELECT @role_developer_id, 'system:automation-execution'
    UNION ALL SELECT @role_developer_id, 'system:automation-results'
    UNION ALL SELECT @role_developer_id, 'risk-config'
    UNION ALL SELECT @role_developer_id, 'risk:config-overview'
    UNION ALL SELECT @role_developer_id, 'risk:rule-definition'
    UNION ALL SELECT @role_developer_id, 'risk:linkage-rule'
    UNION ALL SELECT @role_developer_id, 'risk:emergency-plan'
) grant_scope
JOIN sys_menu m
  ON m.tenant_id = 1
 AND m.menu_code = grant_scope.menu_code
 AND m.deleted = 0
WHERE grant_scope.role_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    deleted = 0,
    update_by = VALUES(update_by),
    update_time = NOW();
```

- [ ] **Step 2: Add role-specific button grants without broad inheritance for restricted roles**

After the page grant block, add explicit business/ops/developer button grants. This avoids accidentally granting add/delete/admin buttons to roles that only need read or diagnostic actions.

```sql
SET @role_button_rebaseline_id := 96018000;

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_button_rebaseline_id := @role_button_rebaseline_id + 1),
       1,
       grant_scope.role_id,
       m.id,
       1,
       NOW(),
       1,
       NOW(),
       0
FROM (
    SELECT @role_business_id AS role_id, 'risk:alarm:detail' AS menu_code
    UNION ALL SELECT @role_business_id, 'risk:alarm:confirm'
    UNION ALL SELECT @role_business_id, 'risk:alarm:suppress'
    UNION ALL SELECT @role_business_id, 'risk:alarm:close'
    UNION ALL SELECT @role_business_id, 'risk:event:detail'
    UNION ALL SELECT @role_business_id, 'risk:event:dispatch'
    UNION ALL SELECT @role_business_id, 'risk:event:close'
    UNION ALL SELECT @role_business_id, 'system:business-acceptance:launch'
    UNION ALL SELECT @role_business_id, 'system:business-acceptance:open-result'
    UNION ALL SELECT @role_business_id, 'iot:products:export'
    UNION ALL SELECT @role_business_id, 'iot:devices:detail'
    UNION ALL SELECT @role_business_id, 'iot:devices:export'
    UNION ALL SELECT @role_business_id, 'iot:devices:insight'

    UNION ALL SELECT @role_ops_id, 'iot:devices:add'
    UNION ALL SELECT @role_ops_id, 'iot:devices:update'
    UNION ALL SELECT @role_ops_id, 'iot:devices:export'
    UNION ALL SELECT @role_ops_id, 'iot:devices:import'
    UNION ALL SELECT @role_ops_id, 'iot:devices:replace'
    UNION ALL SELECT @role_ops_id, 'iot:devices:detail'
    UNION ALL SELECT @role_ops_id, 'iot:devices:export-config'
    UNION ALL SELECT @role_ops_id, 'iot:devices:export-selected'
    UNION ALL SELECT @role_ops_id, 'iot:devices:export-current'
    UNION ALL SELECT @role_ops_id, 'iot:devices:insight'
    UNION ALL SELECT @role_ops_id, 'iot:device-capability:view'
    UNION ALL SELECT @role_ops_id, 'iot:products:add'
    UNION ALL SELECT @role_ops_id, 'iot:products:update'
    UNION ALL SELECT @role_ops_id, 'iot:products:export'
    UNION ALL SELECT @role_ops_id, 'iot:products:detail-overview'
    UNION ALL SELECT @role_ops_id, 'iot:products:detail-devices'
    UNION ALL SELECT @role_ops_id, 'iot:products:detail-contracts'
    UNION ALL SELECT @role_ops_id, 'iot:products:detail-mapping-rules'
    UNION ALL SELECT @role_ops_id, 'iot:products:detail-releases'
    UNION ALL SELECT @role_ops_id, 'iot:product-contract:govern'
    UNION ALL SELECT @role_ops_id, 'iot:product-contract:release'
    UNION ALL SELECT @role_ops_id, 'iot:product-contract:rollback'
    UNION ALL SELECT @role_ops_id, 'iot:product-contract:approve'
    UNION ALL SELECT @role_ops_id, 'iot:vendor-mapping-rule:suggestion'
    UNION ALL SELECT @role_ops_id, 'iot:vendor-mapping-rule:ledger'
    UNION ALL SELECT @role_ops_id, 'iot:vendor-mapping-rule:preview'
    UNION ALL SELECT @role_ops_id, 'iot:vendor-mapping-rule:replay'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:edit'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:approve'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:family-draft'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:family-publish'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:family-rollback'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:decrypt-draft'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:decrypt-preview'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:decrypt-replay'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:template-draft'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:template-replay'
    UNION ALL SELECT @role_ops_id, 'iot:protocol-governance:template-publish'
    UNION ALL SELECT @role_ops_id, 'iot:reporting:replay-workspace'
    UNION ALL SELECT @role_ops_id, 'iot:reporting:simulate-workspace'
    UNION ALL SELECT @role_ops_id, 'iot:reporting:recent-workspace'
    UNION ALL SELECT @role_ops_id, 'iot:reporting:jump-message-trace'
    UNION ALL SELECT @role_ops_id, 'iot:reporting:copy-actual-payload'
    UNION ALL SELECT @role_ops_id, 'iot:reporting:copy-response'
    UNION ALL SELECT @role_ops_id, 'iot:message-trace:detail'
    UNION ALL SELECT @role_ops_id, 'iot:message-trace:timeline'
    UNION ALL SELECT @role_ops_id, 'iot:message-trace:payload-comparison'
    UNION ALL SELECT @role_ops_id, 'iot:secret-custody:view'
    UNION ALL SELECT @role_ops_id, 'iot:secret-custody:rotate'
    UNION ALL SELECT @role_ops_id, 'iot:secret-custody:approve'
    UNION ALL SELECT @role_ops_id, 'risk:alarm:detail'
    UNION ALL SELECT @role_ops_id, 'risk:alarm:confirm'
    UNION ALL SELECT @role_ops_id, 'risk:alarm:suppress'
    UNION ALL SELECT @role_ops_id, 'risk:alarm:close'
    UNION ALL SELECT @role_ops_id, 'risk:event:detail'
    UNION ALL SELECT @role_ops_id, 'risk:event:dispatch'
    UNION ALL SELECT @role_ops_id, 'risk:event:close'

    UNION ALL SELECT @role_developer_id, 'iot:normative-library:write'
    UNION ALL SELECT @role_developer_id, 'iot:products:add'
    UNION ALL SELECT @role_developer_id, 'iot:products:update'
    UNION ALL SELECT @role_developer_id, 'iot:products:export'
    UNION ALL SELECT @role_developer_id, 'iot:products:detail-overview'
    UNION ALL SELECT @role_developer_id, 'iot:products:detail-devices'
    UNION ALL SELECT @role_developer_id, 'iot:products:detail-contracts'
    UNION ALL SELECT @role_developer_id, 'iot:products:detail-mapping-rules'
    UNION ALL SELECT @role_developer_id, 'iot:products:detail-releases'
    UNION ALL SELECT @role_developer_id, 'iot:product-contract:govern'
    UNION ALL SELECT @role_developer_id, 'iot:product-contract:release'
    UNION ALL SELECT @role_developer_id, 'iot:product-contract:rollback'
    UNION ALL SELECT @role_developer_id, 'iot:product-contract:ledger'
    UNION ALL SELECT @role_developer_id, 'iot:product-contract:diff'
    UNION ALL SELECT @role_developer_id, 'iot:vendor-mapping-rule:suggestion'
    UNION ALL SELECT @role_developer_id, 'iot:vendor-mapping-rule:ledger'
    UNION ALL SELECT @role_developer_id, 'iot:vendor-mapping-rule:preview'
    UNION ALL SELECT @role_developer_id, 'iot:vendor-mapping-rule:replay'
    UNION ALL SELECT @role_developer_id, 'iot:devices:detail'
    UNION ALL SELECT @role_developer_id, 'iot:devices:export'
    UNION ALL SELECT @role_developer_id, 'iot:devices:insight'
    UNION ALL SELECT @role_developer_id, 'iot:device-capability:view'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:edit'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:family-draft'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:family-publish'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:family-rollback'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:decrypt-draft'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:decrypt-preview'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:decrypt-replay'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:template-draft'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:template-replay'
    UNION ALL SELECT @role_developer_id, 'iot:protocol-governance:template-publish'
    UNION ALL SELECT @role_developer_id, 'iot:reporting:replay-workspace'
    UNION ALL SELECT @role_developer_id, 'iot:reporting:simulate-workspace'
    UNION ALL SELECT @role_developer_id, 'iot:reporting:recent-workspace'
    UNION ALL SELECT @role_developer_id, 'iot:reporting:jump-message-trace'
    UNION ALL SELECT @role_developer_id, 'iot:reporting:copy-actual-payload'
    UNION ALL SELECT @role_developer_id, 'iot:reporting:copy-response'
    UNION ALL SELECT @role_developer_id, 'iot:message-trace:detail'
    UNION ALL SELECT @role_developer_id, 'iot:message-trace:timeline'
    UNION ALL SELECT @role_developer_id, 'iot:message-trace:payload-comparison'
    UNION ALL SELECT @role_developer_id, 'risk:rule-definition:edit'
    UNION ALL SELECT @role_developer_id, 'risk:linkage-rule:edit'
    UNION ALL SELECT @role_developer_id, 'risk:emergency-plan:edit'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-inventory:refresh'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-inventory:select-uncovered'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-inventory:generate-scaffold'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-templates:add-page-smoke'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-templates:add-form-submit'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-templates:add-list-detail'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-plans:import'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-plans:export'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-plans:reset'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-handoff:copy-command'
    UNION ALL SELECT @role_developer_id, 'system:rd-automation-handoff:export-plan'
    UNION ALL SELECT @role_developer_id, 'system:automation-execution:copy-command'
) grant_scope
JOIN sys_menu m
  ON m.tenant_id = 1
 AND m.menu_code = grant_scope.menu_code
 AND m.deleted = 0
WHERE grant_scope.role_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    deleted = 0,
    update_by = VALUES(update_by),
    update_time = NOW();
```

- [ ] **Step 3: Add broad management button grants**

Management is intentionally broad. Grant all active buttons under pages already granted to management:

```sql
SET @role_governance_button_id := 96019000;

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_governance_button_id := @role_governance_button_id + 1),
       1,
       @role_management_id,
       child.id,
       1,
       NOW(),
       1,
       NOW(),
       0
FROM sys_menu child
JOIN sys_role_menu parent_rm
  ON parent_rm.tenant_id = 1
 AND parent_rm.role_id = @role_management_id
 AND parent_rm.menu_id = child.parent_id
 AND parent_rm.deleted = 0
WHERE child.tenant_id = 1
  AND child.deleted = 0
  AND child.status = 1
  AND child.type = 2
  AND @role_management_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    deleted = 0,
    update_by = VALUES(update_by),
    update_time = NOW();
```

- [ ] **Step 4: Keep super-admin full active-menu grant last**

At the end of role grants, keep or move the super-admin grant so it runs after all active menu rows exist:

```sql
SET @role_super_admin_menu_id := 96030000;

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_super_admin_menu_id := @role_super_admin_menu_id + 1), 1, @role_super_admin_id, m.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.deleted = 0
  AND @role_super_admin_id IS NOT NULL
ORDER BY m.sort, m.id
ON DUPLICATE KEY UPDATE
    deleted = 0,
    update_by = VALUES(update_by),
    update_time = NOW();
```

- [ ] **Step 5: Commit role grant rebaseline**

Run:

```bash
git add sql/init-data.sql
git commit -m "feat: rebaseline role menu grants"
```

Expected: commit contains only role grant changes.

---

### Task 5: Update Documentation

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update business flow documentation**

In `docs/02-业务功能与流程说明.md`, add a short subsection near the five-workbench/menu description:

```markdown
### 菜单与权限初始化基线

2026-04-25 起，初始化菜单基线按“一级工作台目录 -> 总览页面 -> 主导航页面 -> 隐藏深链页面 -> 按钮权限”五层表达。`/device-access`、`/risk-disposal`、`/risk-config`、`/system-management`、`/quality-workbench` 作为工作台总览页面显式进入 `sys_menu`；产品详情五段子路由与业务验收结果页作为隐藏页面节点进入角色权限树，但不进入主侧边栏。按钮权限以页面最近承载点为父级，设备资产中心的 `设备操作` 使用 `iot:device-capability:view` 控制入口可见性。
```

- [ ] **Step 2: Update database initialization documentation**

In `docs/04-数据库设计与初始化数据.md`, update the `sql/init-data.sql` bullet with:

```markdown
- `sql/init-data.sql`：全量初始化样例数据。主要采用 `INSERT ... ON DUPLICATE KEY UPDATE`、变量查询和幂等删除重建关联数据的方式，适合在同一环境重复执行；当前菜单权限基线按页面节点与按钮节点分层维护，并通过 `node scripts/audit-menu-permission-seed.mjs` 校验前端路由、工作台入口、前端按钮权限、后端治理权限常量与 `sys_menu / sys_role_menu` seed 是否一致。
```

- [ ] **Step 3: Update changelog/technical debt**

In `docs/08-变更记录与技术债清单.md`, add an entry dated `2026-04-25`:

```markdown
- `2026-04-25`：重编菜单与按钮权限初始化基线。工作台总览、产品详情深链、业务验收结果页和设备操作入口统一纳入 `sys_menu`，角色授权改为按菜单编码重建并补齐父子授权闭包；新增 `node scripts/audit-menu-permission-seed.mjs` 用于防止页面/按钮实现与初始化权限 seed 再次漂移。内部 Redis key、localStorage key 和测试伪权限进入排除清单，不作为菜单权限。
```

- [ ] **Step 4: Review README and AGENTS**

Run:

```bash
rg -n "菜单|权限|init-data|sys_menu|sys_role_menu|device-capability" README.md AGENTS.md
```

Expected: if no stale menu-permission seed statement exists, leave both files unchanged. If a statement says hidden workbench pages are not in menu seed, update that statement only.

- [ ] **Step 5: Commit docs**

Run:

```bash
git add docs/02-业务功能与流程说明.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md README.md AGENTS.md
git diff --cached --name-only
git commit -m "docs: document menu permission rebaseline"
```

Expected: staged files include only docs actually changed. If README or AGENTS did not need edits, they should not appear in the staged list.

---

### Task 6: Verification

**Files:**
- Read/execute only unless a verification failure exposes a task-owned issue.

- [ ] **Step 1: Run static audit**

Run:

```bash
node scripts/audit-menu-permission-seed.mjs
```

Expected: exit code `0` and `"failures": []`.

- [ ] **Step 2: Run SQL syntax check through MySQL client if available**

Run against a disposable/dev database only:

```bash
mysql --defaults-extra-file=.mysql-dev.cnf < sql/init-data.sql
```

If `.mysql-dev.cnf` is not present, use the repository's existing dev MySQL credentials from `spring-boot-iot-admin/src/main/resources/application-dev.yml` or report that SQL runtime execution was not run. Do not use H2.

Expected: `sql/init-data.sql` completes without duplicate key or syntax errors.

- [ ] **Step 3: Verify role/menu parent closure in the dev database**

Run:

```sql
SELECT r.role_code, child.menu_code, parent.menu_code AS parent_menu_code
FROM sys_role_menu child_rm
JOIN sys_role r ON r.id = child_rm.role_id AND r.deleted = 0
JOIN sys_menu child ON child.id = child_rm.menu_id AND child.deleted = 0
LEFT JOIN sys_role_menu parent_rm
  ON parent_rm.tenant_id = child_rm.tenant_id
 AND parent_rm.role_id = child_rm.role_id
 AND parent_rm.menu_id = child.parent_id
 AND parent_rm.deleted = 0
LEFT JOIN sys_menu parent ON parent.id = child.parent_id
WHERE child_rm.deleted = 0
  AND child.parent_id <> 0
  AND parent_rm.id IS NULL
  AND r.role_code IN ('BUSINESS_STAFF', 'MANAGEMENT_STAFF', 'OPS_STAFF', 'DEVELOPER_STAFF', 'SUPER_ADMIN')
ORDER BY r.role_code, child.sort, child.id;
```

Expected: zero rows.

- [ ] **Step 4: Verify known page paths are available in seed data**

Run:

```sql
SELECT path, menu_name, visible, status
FROM sys_menu
WHERE deleted = 0
  AND path IN (
      '/device-access',
      '/risk-disposal',
      '/risk-config',
      '/system-management',
      '/quality-workbench',
      '/future-lab',
      '/products/:productId/contracts',
      '/products/:productId/mapping-rules',
      '/business-acceptance/results/:runId'
  )
ORDER BY sort, id;
```

Expected: all listed paths appear; `/future-lab` and dynamic/deep-link paths have `visible=0,status=1`.

- [ ] **Step 5: Verify known button permissions are available in seed data**

Run:

```sql
SELECT permission, menu_name, type, status, deleted
FROM sys_menu
WHERE permission IN (
    'iot:device-capability:view',
    'iot:product-contract:govern',
    'iot:protocol-governance:edit',
    'risk:risk-point-binding:execute',
    'risk:risk-point-binding:approve',
    'risk:linkage-plan:approve'
)
ORDER BY permission;
```

Expected: every row has `type=2,status=1,deleted=0`.

- [ ] **Step 6: Run targeted frontend permission tests**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- DeviceWorkbenchView
```

Expected: tests pass or report unrelated pre-existing failures. The important assertion is that `iot:device-capability:view` remains the permission controlling the device operation entry.

- [ ] **Step 7: Run final status review**

Run:

```bash
git status --short
git log -5 --oneline
```

Expected: only intentional task files are modified or committed. Any unrelated dirty files remain untouched and uncommitted.
