#!/usr/bin/env node

import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptPath = fileURLToPath(import.meta.url);
const repoRoot = path.resolve(path.dirname(scriptPath), '..');

const ROUTE_EXCLUDES = new Set(['/', '/login', '/risk-enhance']);
const PERMISSION_EXACT_EXCLUDES = new Set(['system:root', 'system:menu:refresh']);
const PERMISSION_PREFIX_EXCLUDES = [
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
  'iot:shell-notice-sync-event',
  'iot:device-capability-execute:'
];

const SOURCE_EXCLUDED_DIRS = new Set(['node_modules', 'target', 'logs', '.git', '__tests__', '__mocks__']);
const SOURCE_EXTENSIONS = new Set(['.vue', '.ts', '.tsx']);
const TEST_SOURCE_FILE_PATTERN = /(?:^|[./_-])(?:test|spec)\.(?:vue|tsx?|jsx?)$/i;
const ACTION_BUTTON_COMPONENT_PATTERN = /<(StandardButton|StandardActionLink|button)\b([\s\S]*?)>([\s\S]*?)<\/\1>/g;
const ACTION_BUTTON_LABEL_EXCLUDES = new Set([
  '查询',
  '重置',
  '刷新列表',
  '清空表单',
  '取消编辑',
  '清空选择',
  '取消',
  '关闭弹窗'
]);
const ACTION_BUTTON_KEYWORD_PATTERN = /(新增|创建|编辑|保存|删除|导入|导出|提交|发布|回滚|确认|阻塞|关闭|抑制|复盘|触发|批量|执行|生成|复制|刷新|套用|预填|应用|恢复默认|清空结果|查看原文|查看详情)/;
const ACTION_CLICK_KEYWORD_PATTERN = /(?:^|[.(])(?:handle|submit|confirm|create|add|edit|save|delete|remove|import|export|publish|rollback|approve|reject|cancel|resubmit|dispatch|close|suppress|execute|trigger|batch|apply|promote|ignore|accept|disable|enable)[A-Z_('"]?/i;

function sortValues(values) {
  return Array.from(values).sort((left, right) => left.localeCompare(right));
}

function normalizePath(value) {
  const normalized = String(value || '').trim().replace(/\/+$/, '');
  return normalized || '/';
}

function isRelevantPermissionCode(value) {
  const code = String(value || '').trim();
  if (!/^(iot|risk|system):[a-z0-9][a-z0-9:_-]*$/i.test(code)) {
    return false;
  }
  if ((code.match(/:/g) || []).length < 2) {
    return false;
  }
  if (PERMISSION_EXACT_EXCLUDES.has(code)) {
    return false;
  }
  if (PERMISSION_PREFIX_EXCLUDES.some((prefix) => code === prefix || code.startsWith(prefix))) {
    return false;
  }
  return true;
}

async function readText(relativePath) {
  return readFile(path.join(repoRoot, relativePath), 'utf8');
}

async function listSourceFiles(rootDir) {
  const result = [];

  async function visit(currentDir) {
    const entries = (await readdir(currentDir, { withFileTypes: true }))
      .sort((left, right) => left.name.localeCompare(right.name));
    for (const entry of entries) {
      if (SOURCE_EXCLUDED_DIRS.has(entry.name)) {
        continue;
      }
      const absolutePath = path.join(currentDir, entry.name);
      if (entry.isDirectory()) {
        await visit(absolutePath);
        continue;
      }
      if (
        entry.isFile()
        && SOURCE_EXTENSIONS.has(path.extname(entry.name))
        && !TEST_SOURCE_FILE_PATTERN.test(entry.name)
      ) {
        result.push(absolutePath);
      }
    }
  }

  await visit(rootDir);
  return result.sort((left, right) => left.localeCompare(right));
}

async function findGovernancePermissionCodesFile() {
  const preferred = path.join(
    repoRoot,
    'spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/security/GovernancePermissionCodes.java'
  );
  try {
    await readFile(preferred, 'utf8');
    return preferred;
  } catch {
    // Fall through to repository search.
  }

  async function visit(currentDir) {
    const entries = (await readdir(currentDir, { withFileTypes: true }))
      .sort((left, right) => left.name.localeCompare(right.name));
    for (const entry of entries) {
      if (SOURCE_EXCLUDED_DIRS.has(entry.name)) {
        continue;
      }
      const absolutePath = path.join(currentDir, entry.name);
      if (entry.isDirectory()) {
        const found = await visit(absolutePath);
        if (found) {
          return found;
        }
      } else if (entry.isFile() && entry.name === 'GovernancePermissionCodes.java') {
        return absolutePath;
      }
    }
    return null;
  }

  return visit(repoRoot);
}

function extractPathLiterals(text) {
  const paths = new Set();
  const pathPattern = /\bpath\s*:\s*(['"`])([^'"`]+)\1/g;
  for (const match of text.matchAll(pathPattern)) {
    paths.add(normalizePath(match[2]));
  }
  return paths;
}

function extractRoutePaths(routerText) {
  const routeArrayStart = routerText.indexOf('const routes');
  const assignmentStart = routerText.indexOf('=', routeArrayStart);
  const arrayStart = routerText.indexOf('[', assignmentStart);
  if (routeArrayStart === -1 || assignmentStart === -1 || arrayStart === -1) {
    return extractPathLiterals(routerText);
  }

  const routePaths = new Set();
  let depth = 0;
  let inString = false;
  let quote = '';
  let recordStart = -1;

  for (let index = arrayStart; index < routerText.length; index += 1) {
    const char = routerText[index];
    const next = routerText[index + 1];

    if (inString) {
      if (char === '\\') {
        index += 1;
      } else if (char === quote) {
        inString = false;
        quote = '';
      }
      continue;
    }

    if (char === "'" || char === '"' || char === '`') {
      inString = true;
      quote = char;
      continue;
    }
    if (char === '[' || char === '{' || char === '(') {
      if (char === '{' && depth === 1) {
        recordStart = index;
      }
      depth += 1;
      continue;
    }
    if (char === ']' || char === '}' || char === ')') {
      depth -= 1;
      if (char === '}' && depth === 1 && recordStart >= 0) {
        const record = routerText.slice(recordStart, index + 1);
        const pathMatch = record.match(/\bpath\s*:\s*(['"`])([^'"`]+)\1/);
        if (pathMatch && !/\bredirect\s*:/.test(record)) {
          routePaths.add(normalizePath(pathMatch[2]));
        }
        recordStart = -1;
      }
      if (char === ']' && depth === 0) {
        break;
      }
    }
  }
  return routePaths;
}

function extractPermissionCodesFromSource(text) {
  const permissions = new Set();
  const literalPattern = /(['"`])((?:iot|risk|system):[a-z0-9][a-z0-9:_-]*)\1/gi;
  for (const match of text.matchAll(literalPattern)) {
    if (isRelevantPermissionCode(match[2])) {
      permissions.add(match[2]);
    }
  }
  return permissions;
}

function splitSqlValues(tupleText) {
  const values = [];
  let current = '';
  let inString = false;
  let depth = 0;

  for (let index = 0; index < tupleText.length; index += 1) {
    const char = tupleText[index];
    const next = tupleText[index + 1];

    if (inString) {
      current += char;
      if (char === "'" && next === "'") {
        current += next;
        index += 1;
      } else if (char === "'" && next !== "'") {
        inString = false;
      }
      continue;
    }

    if (char === "'") {
      inString = true;
      current += char;
      continue;
    }
    if (char === '(') {
      depth += 1;
      current += char;
      continue;
    }
    if (char === ')') {
      depth -= 1;
      current += char;
      continue;
    }
    if (char === ',' && depth === 0) {
      values.push(current.trim());
      current = '';
      continue;
    }
    current += char;
  }

  if (current.trim()) {
    values.push(current.trim());
  }
  return values;
}

function unquoteSqlValue(value) {
  const trimmed = String(value || '').trim();
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed.slice(1, -1).replace(/''/g, "'");
  }
  if (/^-?\d+$/.test(trimmed)) {
    return Number.parseInt(trimmed, 10);
  }
  if (/^null$/i.test(trimmed)) {
    return null;
  }
  return trimmed;
}

function splitTopLevelTuples(valuesText) {
  const tuples = [];
  let start = -1;
  let depth = 0;
  let inString = false;

  for (let index = 0; index < valuesText.length; index += 1) {
    const char = valuesText[index];
    const next = valuesText[index + 1];

    if (inString) {
      if (char === "'" && next === "'") {
        index += 1;
      } else if (char === "'") {
        inString = false;
      }
      continue;
    }

    if (char === "'") {
      inString = true;
      continue;
    }
    if (char === '(') {
      if (depth === 0) {
        start = index + 1;
      }
      depth += 1;
      continue;
    }
    if (char === ')') {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        tuples.push(valuesText.slice(start, index));
        start = -1;
      }
    }
  }

  return tuples;
}

function parseSysMenuRows(sqlText) {
  const rows = [];
  const insertPattern = /INSERT(?:\s+IGNORE)?\s+INTO\s+sys_menu\s*\(([\s\S]*?)\)\s*VALUES\s*([\s\S]*?)(?:ON\s+DUPLICATE\s+KEY\s+UPDATE|;)/gi;

  for (const match of sqlText.matchAll(insertPattern)) {
    const columns = match[1].split(',').map((column) => column.trim().replace(/`/g, ''));
    for (const tuple of splitTopLevelTuples(match[2])) {
      const values = splitSqlValues(tuple).map(unquoteSqlValue);
      const row = {};
      columns.forEach((column, index) => {
        row[column] = values[index];
      });
      rows.push({
        id: row.id,
        parentId: row.parent_id,
        menuName: row.menu_name,
        menuCode: row.menu_code,
        path: normalizePath(row.path),
        type: row.type,
        status: row.status,
        deleted: row.deleted,
        visible: row.visible
      });
    }
  }

  const softDeletePattern = /UPDATE\s+sys_menu[\s\S]*?menu_code\s+IN\s*\(([\s\S]*?)\)[\s\S]*?deleted\s*=\s*0\s*;/gi;
  for (const match of sqlText.matchAll(softDeletePattern)) {
    for (const codeMatch of match[1].matchAll(/'([^']+)'/g)) {
      rows.push({
        id: null,
        parentId: null,
        menuName: 'soft-deleted by seed update',
        menuCode: codeMatch[1],
        path: '/',
        type: 2,
        status: 0,
        deleted: 1,
        visible: 0
      });
    }
  }

  return rows;
}

function normalizeButtonLabel(content) {
  const text = String(content || '');
  const interpolationLabels = [];
  for (const interpolation of text.matchAll(/\{\{([\s\S]*?)\}\}/g)) {
    for (const literal of interpolation[1].matchAll(/(['"`])([^'"`]+)\1/g)) {
      interpolationLabels.push(literal[2]);
    }
  }
  return `${text} ${interpolationLabels.join(' ')}`
    .replace(/<[^>]*>/g, ' ')
    .replace(/\{\{[\s\S]*?\}\}/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function extractClickExpression(attrs) {
  const match = String(attrs || '').match(/@click(?:\.[\w-]+)*\s*=\s*(['"`])([\s\S]*?)\1/);
  return match ? match[2].trim() : '';
}

function isLocalUiActionCandidate({ file, label, click }) {
  const normalizedFile = String(file || '').replace(/\\/g, '/');
  const normalizedLabel = String(label || '').trim();
  const normalizedClick = String(click || '').trim();

  if ([
    'spring-boot-iot-ui/src/components/AutomationRecentRunsPanel.vue',
    'spring-boot-iot-ui/src/components/AutomationResultEvidencePanel.vue',
    'spring-boot-iot-ui/src/components/AutomationResultImportPanel.vue',
    'spring-boot-iot-ui/src/components/AutomationScenarioEditor.vue',
    'spring-boot-iot-ui/src/components/AutomationStepEditor.vue',
    'spring-boot-iot-ui/src/components/CsvColumnSettingDialog.vue',
    'spring-boot-iot-ui/src/components/ResponsePanel.vue',
    'spring-boot-iot-ui/src/components/StandardChoiceGroup.vue',
    'spring-boot-iot-ui/src/components/StandardListFilterHeader.vue',
    'spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue',
    'spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue',
    'spring-boot-iot-ui/src/components/reporting/ReportingRecentDiagnosisList.vue'
  ].includes(normalizedFile)) {
    return true;
  }

  if (/^复制/.test(normalizedLabel) && /^(?:copy|handleCopy)[A-Z\w]*(?:\(|$)/.test(normalizedClick)) {
    return true;
  }
  if (/^(清空筛选条件|重置筛选|返回列表|查看结果|重新填入草稿|查询台账|查询中\.\.\. 查询设备|分析中\.\.\. 开始分析|加载中\.\.\. 刷新数据)$/.test(normalizedLabel)) {
    return true;
  }
  if (/^(?:handleClearAppliedFilters|handleReset|handleBackToList|handleSearch|handleQuery(?:Device)?|refreshAll|applyActionDraftComment|resetForm|handleOpenAcceptance)(?:\(|$)/.test(normalizedClick)
    || /^handleNext(?:\(|$)/.test(normalizedClick)) {
    return true;
  }
  if (/^handleLoad[A-Z]\w*Detail\(/.test(normalizedClick)) {
    return true;
  }
  const isLocalCloseClick = /[Vv]isible\s*=\s*false/.test(normalizedClick)
    || /update[A-Z\w]*Dialog\(false\)/.test(normalizedClick)
    || /^emit\('(?:close|cancel)'/.test(normalizedClick);
  if (/^(关闭|取消)/.test(normalizedLabel) && isLocalCloseClick) {
    return true;
  }
  if (/导出列设置/.test(normalizedLabel) && /^open\w*ExportColumnSetting$/.test(normalizedClick)) {
    return true;
  }
  if (/刷新/.test(normalizedLabel) && /^(?:handle(?:Bridge)?Refresh|handleRefreshMessageFlow|emit\('refresh(?:Commands)?'\)|\$emit\('refresh'\))$/.test(normalizedClick)) {
    return true;
  }

  if (normalizedFile.endsWith('/ShellAccountDrawers.vue')
    && ['updateAccountDialog(false)', 'submitProfileUpdate', 'submitChangePassword'].includes(normalizedClick)) {
    return true;
  }
  if ((normalizedFile.endsWith('/ShellHelpCenterDrawer.vue') || normalizedFile.endsWith('/ShellNoticeCenterDrawer.vue'))
    && /^emit\('(?:select|refresh|read|readAll)'/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/DeviceCapabilityExecuteDrawer.vue')
    && /^(?:applyTemplate|saveCurrentAsFavorite|applyFavorite|applyCapabilityPreset|confirmFavoriteRename|deleteFavorite)(?:\(|$)/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/DeviceBatchImportDrawer.vue') && normalizedClick === 'triggerFileSelect') {
    return true;
  }
  if (normalizedFile.endsWith('/DeviceCapabilityPanel.vue') && normalizedClick === "emit('refreshCommands')") {
    return true;
  }
  if (normalizedFile.endsWith('/DeviceWorkbenchView.vue') && /^handleJumpToInsight\(/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/ProductModelDesignerWorkspace.vue')
    && /^(?:addRelationRow|handleGovernanceStepAction|selectLedgerBatch|selectComparisonBaselineBatch|handleDeviceStructureChange|handleLoadRelations|removeRelationRow|refresh(?:Rollback|Apply)ApprovalDetail)(?:\(|$)/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/ProductRuntimeMetricDisplayRulePanel.vue') && /^startEdit\(/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/ReportWorkbenchView.vue')
    && /^(?:copyActualPayloadPreview|copyResponse|handleRefreshMessageFlow|syncTopic)$/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/RiskGisView.vue')
    && (normalizedClick === 'handleSearch' || normalizedClick === 'handleRefresh')) {
    return true;
  }
  if (normalizedFile.endsWith('/ProductDetailWorkbenchView.vue') && normalizedClick === 'handleRefresh') {
    return true;
  }
  if (normalizedFile.endsWith('/HeaderPopoverPanel.vue') && normalizedClick === "$emit('select', item)") {
    return true;
  }
  if (normalizedFile.endsWith('/ProductDeviceMap.vue') && normalizedClick === 'handleRetry') {
    return true;
  }
  if (normalizedFile.endsWith('/ShellCommandPalette.vue') && normalizedClick === 'closePalette') {
    return true;
  }
  if (normalizedFile.endsWith('/ReportWorkbenchView.vue') && /^applyTemplate\(/.test(normalizedClick)) {
    return true;
  }
  if (normalizedFile.endsWith('/RiskPointView.vue')
    && /^(?:handleBindingWorkbenchModeChange|handleSelectPendingRow)\(/.test(normalizedClick)) {
    return true;
  }

  return false;
}

function extractUnguardedActionButtons(sourceText, absolutePath) {
  const candidates = [];
  const file = path.relative(repoRoot, absolutePath);
  for (const match of sourceText.matchAll(ACTION_BUTTON_COMPONENT_PATTERN)) {
    const attrs = match[2] || '';
    if (!/@click(?:\.[\w-]+)*\s*=/.test(attrs) || /\bv-permission\b/.test(attrs)) {
      continue;
    }
    const label = normalizeButtonLabel(match[3]);
    const click = extractClickExpression(attrs);
    if (
      ACTION_BUTTON_LABEL_EXCLUDES.has(label)
      || (!ACTION_BUTTON_KEYWORD_PATTERN.test(label) && !ACTION_CLICK_KEYWORD_PATTERN.test(click))
    ) {
      continue;
    }
    if (isLocalUiActionCandidate({ file, label, click })) {
      continue;
    }
    candidates.push({
      file,
      line: sourceText.slice(0, match.index).split('\n').length,
      component: match[1],
      label,
      click
    });
  }
  return candidates;
}

function buildAudit({
  routePaths,
  workspacePaths,
  uiPermissionCodes,
  backendPermissionCodes,
  menuRows,
  unguardedActionButtonCandidates
}) {
  const requiredRoutePaths = new Set([...routePaths, ...workspacePaths].map(normalizePath));
  for (const excludedPath of ROUTE_EXCLUDES) {
    requiredRoutePaths.delete(excludedPath);
  }

  const requiredPermissionCodes = new Set();
  for (const code of [...uiPermissionCodes, ...backendPermissionCodes]) {
    if (isRelevantPermissionCode(code)) {
      requiredPermissionCodes.add(code);
    }
  }

  const activePagePaths = new Set(
    menuRows
      .filter((row) => (row.type === 0 || row.type === 1) && row.status === 1 && row.deleted === 0)
      .map((row) => row.path)
      .filter((rowPath) => rowPath && rowPath !== '/')
  );
  const activeRouteMenus = new Map(
    menuRows
      .filter((row) => (row.type === 0 || row.type === 1) && row.status === 1 && row.deleted === 0)
      .map((row) => [row.path, row])
  );
  const activePermissionCodes = new Set(
    menuRows
      .filter((row) => row.type === 2 && row.status === 1 && row.deleted === 0)
      .map((row) => row.menuCode)
      .filter(Boolean)
  );
  const inactivePermissionCodes = new Set(
    menuRows
      .filter((row) => row.type === 2 && row.menuCode && (row.status !== 1 || row.deleted !== 0))
      .map((row) => row.menuCode)
  );

  const missingRoutePaths = sortValues([...requiredRoutePaths].filter((routePath) => !activePagePaths.has(routePath)));
  const missingPermissionCodes = sortValues(
    [...requiredPermissionCodes].filter((code) => !activePermissionCodes.has(code))
  );
  const softDeletedPermissionCodes = sortValues(
    missingPermissionCodes.filter((code) => inactivePermissionCodes.has(code))
  );
  const failures = [
    ...missingRoutePaths.map((routePath) => `missing route menu: ${routePath}`),
    ...missingPermissionCodes.map((code) => `missing permission menu: ${code}`),
    ...unguardedActionButtonCandidates.map((candidate) => {
      const actionLabel = candidate.label || candidate.click || candidate.component;
      return `unguarded action button: ${candidate.file}:${candidate.line} ${actionLabel}`;
    })
  ].sort((left, right) => left.localeCompare(right));

  return {
    counts: {
      routePaths: routePaths.size,
      workspacePaths: workspacePaths.size,
      requiredRoutePaths: requiredRoutePaths.size,
      uiPermissionCodes: uiPermissionCodes.size,
      backendPermissionCodes: backendPermissionCodes.size,
      requiredPermissionCodes: requiredPermissionCodes.size,
      sysMenuRows: menuRows.length,
      activePageRows: activePagePaths.size,
      activePermissionRows: activePermissionCodes.size,
      unguardedActionButtonCandidates: unguardedActionButtonCandidates.length
    },
    coveredRouteMenus: sortValues([...requiredRoutePaths].filter((routePath) => activePagePaths.has(routePath)))
      .map((routePath) => {
        const row = activeRouteMenus.get(routePath);
        return {
          path: routePath,
          menuName: row?.menuName || '',
          menuCode: row?.menuCode || '',
          visible: row?.visible ?? null
        };
      }),
    missingRoutePaths,
    missingPermissionCodes,
    softDeletedPermissionCodes,
    unguardedActionButtonCandidates: unguardedActionButtonCandidates.slice(0, 80),
    failures
  };
}

async function main() {
  const routerText = await readText('spring-boot-iot-ui/src/router/index.ts');
  const sectionWorkspacesText = await readText('spring-boot-iot-ui/src/utils/sectionWorkspaces.ts');
  const sqlText = await readText('sql/init-data.sql');
  const governancePermissionCodesFile = await findGovernancePermissionCodesFile();

  const routePaths = extractRoutePaths(routerText);
  const workspacePaths = extractPathLiterals(sectionWorkspacesText);
  const uiPermissionCodes = new Set();
  const unguardedActionButtonCandidates = [];
  const frontendSourceFiles = await listSourceFiles(path.join(repoRoot, 'spring-boot-iot-ui/src'));
  for (const sourceFile of frontendSourceFiles) {
    const sourceText = await readFile(sourceFile, 'utf8');
    for (const permissionCode of extractPermissionCodesFromSource(sourceText)) {
      uiPermissionCodes.add(permissionCode);
    }
    unguardedActionButtonCandidates.push(...extractUnguardedActionButtons(sourceText, sourceFile));
  }

  const backendPermissionCodes = new Set();
  if (governancePermissionCodesFile) {
    const javaText = await readFile(governancePermissionCodesFile, 'utf8');
    for (const permissionCode of extractPermissionCodesFromSource(javaText)) {
      backendPermissionCodes.add(permissionCode);
    }
  }

  const summary = buildAudit({
    routePaths,
    workspacePaths,
    uiPermissionCodes,
    backendPermissionCodes,
    menuRows: parseSysMenuRows(sqlText),
    unguardedActionButtonCandidates
  });

  console.log(JSON.stringify(summary, null, 2));
  process.exitCode = summary.failures.length > 0 ? 1 : 0;
}

main().catch((error) => {
  console.error(JSON.stringify({
    error: error instanceof Error ? error.message : String(error)
  }, null, 2));
  process.exitCode = 1;
});
