import { promises as fs } from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const workspaceRoot = process.cwd();
const sourceRoot = path.join(workspaceRoot, 'src');
const viewsRoot = path.join(sourceRoot, 'views');

const scanExtensions = new Set(['.vue', '.ts']);
const governedColumnAllowlist = new Map([
  [
    path.join(viewsRoot, 'ProductWorkbenchView.vue'),
    new Set(['节点类型', '产品状态', '在线设备数', '操作'])
  ],
  [
    path.join(viewsRoot, 'DeviceWorkbenchView.vue'),
    new Set(['在线状态', '激活状态', '设备状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'MessageTraceView.vue'),
    new Set(['操作'])
  ],
  [
    path.join(viewsRoot, 'AuditLogView.vue'),
    new Set(['操作类型', '请求方法/通道', '操作结果', '操作'])
  ],
  [
    path.join(viewsRoot, 'AlarmCenterView.vue'),
    new Set(['告警等级', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'EventDisposalView.vue'),
    new Set(['风险等级', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'RealTimeMonitoringView.vue'),
    new Set(['状态', '风险等级', '告警标记', '操作'])
  ],
  [
    path.join(viewsRoot, 'RiskGisView.vue'),
    new Set(['风险等级', '操作'])
  ],
  [
    path.join(viewsRoot, 'RiskPointView.vue'),
    new Set(['所属组织', '风险等级', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'RuleDefinitionView.vue'),
    new Set(['告警等级', '转事件', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'LinkageRuleView.vue'),
    new Set(['状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'EmergencyPlanView.vue'),
    new Set(['风险等级', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'OrganizationView.vue'),
    new Set(['组织类型', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'UserView.vue'),
    new Set(['状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'RoleView.vue'),
    new Set(['状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'MenuView.vue'),
    new Set(['类型', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'RegionView.vue'),
    new Set(['区域类型', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'DictView.vue'),
    new Set(['字典类型', '状态', '操作', '项类型'])
  ],
  [
    path.join(viewsRoot, 'ChannelView.vue'),
    new Set(['渠道类型', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'InAppMessageView.vue'),
    new Set([
      '消息分类',
      '优先级',
      '推送范围',
      '来源类型',
      '状态',
      '操作',
      '渠道类型',
      '桥接状态',
      '未读人数',
      '尝试次数',
      '最近尝试时间',
      '成功时间',
      '响应码',
      '尝试序号',
      '结果',
      '目标摘要',
      '响应摘要',
      '尝试时间'
    ])
  ],
  [
    path.join(viewsRoot, 'HelpDocView.vue'),
    new Set(['文档分类', '状态', '操作'])
  ],
  [
    path.join(viewsRoot, 'AutomationTestCenterView.vue'),
    new Set(['断言'])
  ]
]);

const governedActionColumnClassFiles = new Set([
  path.join(viewsRoot, 'ProductWorkbenchView.vue'),
  path.join(viewsRoot, 'DeviceWorkbenchView.vue'),
  path.join(viewsRoot, 'MessageTraceView.vue'),
  path.join(viewsRoot, 'AuditLogView.vue'),
  path.join(sourceRoot, 'components', 'AccessErrorArchivePanel.vue')
]);

function toRelative(filePath) {
  return path.relative(workspaceRoot, filePath);
}

function getLineNumber(content, index) {
  return content.slice(0, index).split(/\r?\n/).length;
}

async function collectFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectFiles(fullPath)));
      continue;
    }

    if (!scanExtensions.has(path.extname(entry.name))) {
      continue;
    }

    files.push(fullPath);
  }

  return files;
}

function collectMatches(content, pattern) {
  const matches = [];
  let match = pattern.exec(content);

  while (match) {
    matches.push(match);
    match = pattern.exec(content);
  }

  return matches;
}

function scanStandardActionLinkTone(filePath, content, errors) {
  const pattern = /<StandardActionLink\b[^>]*\btone\s*=/g;
  for (const match of collectMatches(content, pattern)) {
    errors.push({
      file: filePath,
      line: getLineNumber(content, match.index),
      message: 'StandardActionLink 已移除 tone，行内轻操作统一走共享品牌语义。'
    });
  }
}

function scanStandardActionMenuTone(filePath, content, errors) {
  if (!content.includes('StandardActionMenu')) {
    return;
  }

  const pattern = /\{[^{}]*\b(?:command\s*:[^{}]*\blabel\s*:|\blabel\s*:[^{}]*\bcommand\s*:)[^{}]*\btone\s*:[^{}]*\}/gs;
  for (const match of collectMatches(content, pattern)) {
    errors.push({
      file: filePath,
      line: getLineNumber(content, match.index),
      message: 'StandardActionMenu item 已移除 tone，请只保留 key / command / label / disabled / divided。'
    });
  }
}

function scanActionDeepOverrides(filePath, content, errors) {
  const pattern = /[.#][\w-]*actions[\w-]*(?:[\s>+~.:#()[\],-]+):deep\(\.el-(?:button|dropdown|button-group)\)/g;
  for (const match of collectMatches(content, pattern)) {
    errors.push({
      file: filePath,
      line: getLineNumber(content, match.index),
      message: '页面 *actions 区块禁止继续深度覆写 .el-button / .el-dropdown / .el-button-group，请改回共享动作组件。'
    });
  }
}

function scanGovernedRawColumns(filePath, content, errors) {
  const allowlist = governedColumnAllowlist.get(filePath);
  if (!allowlist) {
    return;
  }

  const pattern = /<el-table-column\b[\s\S]*?>/g;
  for (const match of collectMatches(content, pattern)) {
    const tag = match[0];
    if (/type\s*=\s*["']selection["']/.test(tag)) {
      continue;
    }

    const labelMatch = tag.match(/\blabel\s*=\s*["']([^"']+)["']/);
    if (!labelMatch) {
      errors.push({
        file: filePath,
        line: getLineNumber(content, match.index),
        message: '纳管页面中的原生 el-table-column 必须声明固定 allowlist 标签，其他固定宽度纯文本列请改用 StandardTableTextColumn。'
      });
      continue;
    }

    const label = labelMatch[1].trim();
    if (!allowlist.has(label)) {
      errors.push({
        file: filePath,
        line: getLineNumber(content, match.index),
        message: `纳管页面中的原生 el-table-column "${label}" 不在 allowlist 内，请改用 StandardTableTextColumn 或回到共享契约。`
      });
    }
  }
}

function scanGovernedActionColumnClass(filePath, content, errors) {
  if (!governedActionColumnClassFiles.has(filePath)) {
    return;
  }

  const pattern = /<el-table-column\b[\s\S]*?\blabel\s*=\s*["']操作["'][\s\S]*?>/g;
  for (const match of collectMatches(content, pattern)) {
    if (!/\bclass-name\s*=\s*["']standard-row-actions-column["']/.test(match[0])) {
      errors.push({
        file: filePath,
        line: getLineNumber(content, match.index),
        message: '纳管页面的“操作”列必须声明 class-name="standard-row-actions-column"，避免固定列尾部再次出现裁切圆点与间距漂移。'
      });
    }
  }
}

function printErrors(errors) {
  console.error('\nComponent contract guard failed:');
  for (const error of errors) {
    console.error(`- ${toRelative(error.file)}:${error.line} ${error.message}`);
  }
}

async function main() {
  const sourceFiles = await collectFiles(sourceRoot);
  const viewFiles = (await collectFiles(viewsRoot)).filter((filePath) => filePath.endsWith('.vue'));
  const errors = [];

  for (const filePath of sourceFiles) {
    const content = await fs.readFile(filePath, 'utf8');
    scanStandardActionLinkTone(filePath, content, errors);
    scanStandardActionMenuTone(filePath, content, errors);
  }

  for (const filePath of viewFiles) {
    const content = await fs.readFile(filePath, 'utf8');
    scanActionDeepOverrides(filePath, content, errors);
    scanGovernedRawColumns(filePath, content, errors);
    scanGovernedActionColumnClass(filePath, content, errors);
  }

  for (const filePath of governedActionColumnClassFiles) {
    if (viewFiles.includes(filePath)) {
      continue;
    }

    const content = await fs.readFile(filePath, 'utf8');
    scanGovernedActionColumnClass(filePath, content, errors);
  }

  if (errors.length > 0) {
    printErrors(errors);
    process.exitCode = 1;
    return;
  }

  console.log('Component contract guard passed.');
}

await main();
