#!/usr/bin/env node

import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '../..');
const docsRoot = path.join(repoRoot, 'docs');

const ROOT_DOC_WHITELIST = new Set([
  'README.md',
  '历史兼容入口索引.md',
  '真实环境测试与验收手册.md',
  '01-系统概览与架构说明.md',
  '02-业务功能与流程说明.md',
  '03-接口规范与接口清单.md',
  '04-数据库设计与初始化数据.md',
  '05-protocol.md',
  '05-自动化测试与质量保障.md',
  '06-前端开发与CSS规范.md',
  '07-部署运行与配置说明.md',
  '08-变更记录与技术债清单.md',
  '09-GPT接管提示模板.md',
  '10-智能助手技能与任务选型指南.md',
  '11-可观测性、日志追踪与消息通知治理.md',
  '12-帮助文档与系统内容治理.md',
  '13-数据权限与多租户模型.md',
  '14-MQTTX真实环境联调手册.md',
  '15-前端优化与治理计划.md',
  '16-阶段规划与迭代路线图.md',
  '17-智能助手任务发起模板速查.md',
  '19-第四阶段交付边界与复验进展.md',
  '21-业务功能清单与验收标准.md',
]);

const LEGACY_DOC_NAMES = [
  '00-overview',
  '01-architecture',
  '02-module-structure',
  '03-database',
  '04-api',
  '06-thing-model',
  '07-message-flow',
  '10-deploy',
  '12-change-log',
  '14-mqtt-live-runbook',
  '22-automation-test-issues-20260316',
  '23-frontend-detail-optimization',
];

const RETIRED_ACTIVE_DOC_NAMES = [
  ['14', 'mqttx-live-runbook.md'],
  ['15', 'frontend-optimization-plan.md'],
  ['19', 'phase4-progress.md'],
  ['21', 'business-functions-and-acceptance.md'],
  ['legacy', 'entrypoints.md'],
  ['test', 'scenarios.md'],
].map((parts) => parts.join('-'));

const EXACT_21_HISTORY_HEADINGS = [
  '## 7. 认证与权限验收基线（2026-03-16）',
  '## 8. 真实环境全链路打勾清单（2026-03-16）',
  '## 9. 2026-03-16 第二轮真实环境复验结果（有效结论）',
  '## 10. 2026-03-18 Frontend UI Unification Progress',
  '## 11. 日志链路补充（2026-03-18）',
];

const ENTRY_DOC_EXPECTATIONS = {
  'README.md': [
    'docs/README.md',
    'docs/01-系统概览与架构说明.md',
    'docs/02-业务功能与流程说明.md',
    'docs/03-接口规范与接口清单.md',
    'docs/04-数据库设计与初始化数据.md',
    'docs/07-部署运行与配置说明.md',
    'docs/08-变更记录与技术债清单.md',
    'docs/05-自动化测试与质量保障.md',
    'docs/真实环境测试与验收手册.md',
    'docs/21-业务功能清单与验收标准.md',
    'docs/06-前端开发与CSS规范.md',
    'docs/15-前端优化与治理计划.md',
    'docs/05-protocol.md',
    'docs/14-MQTTX真实环境联调手册.md',
    'docs/11-可观测性、日志追踪与消息通知治理.md',
    'docs/12-帮助文档与系统内容治理.md',
    'docs/13-数据权限与多租户模型.md',
    'docs/19-第四阶段交付边界与复验进展.md',
    'docs/16-阶段规划与迭代路线图.md',
    'docs/09-GPT接管提示模板.md',
    'docs/10-智能助手技能与任务选型指南.md',
    'docs/17-智能助手任务发起模板速查.md',
  ],
  'AGENTS.md': [
    'docs/README.md',
    'docs/01-系统概览与架构说明.md',
    'docs/02-业务功能与流程说明.md',
    'docs/03-接口规范与接口清单.md',
    'docs/04-数据库设计与初始化数据.md',
    'docs/07-部署运行与配置说明.md',
    'docs/08-变更记录与技术债清单.md',
    'docs/05-自动化测试与质量保障.md',
    'docs/真实环境测试与验收手册.md',
    'docs/21-业务功能清单与验收标准.md',
    'docs/06-前端开发与CSS规范.md',
    'docs/15-前端优化与治理计划.md',
    'docs/05-protocol.md',
    'docs/14-MQTTX真实环境联调手册.md',
    'docs/11-可观测性、日志追踪与消息通知治理.md',
    'docs/12-帮助文档与系统内容治理.md',
    'docs/13-数据权限与多租户模型.md',
    'docs/19-第四阶段交付边界与复验进展.md',
    'docs/16-阶段规划与迭代路线图.md',
    'docs/09-GPT接管提示模板.md',
    'docs/10-智能助手技能与任务选型指南.md',
    'docs/17-智能助手任务发起模板速查.md',
  ],
  'docs/README.md': [
    '../README.md',
    '01-系统概览与架构说明.md',
    '02-业务功能与流程说明.md',
    '03-接口规范与接口清单.md',
    '04-数据库设计与初始化数据.md',
    '07-部署运行与配置说明.md',
    '08-变更记录与技术债清单.md',
    '05-自动化测试与质量保障.md',
    '真实环境测试与验收手册.md',
    '21-业务功能清单与验收标准.md',
    '06-前端开发与CSS规范.md',
    '15-前端优化与治理计划.md',
    '05-protocol.md',
    '14-MQTTX真实环境联调手册.md',
    '11-可观测性、日志追踪与消息通知治理.md',
    '12-帮助文档与系统内容治理.md',
    '13-数据权限与多租户模型.md',
    '19-第四阶段交付边界与复验进展.md',
    '16-阶段规划与迭代路线图.md',
    '09-GPT接管提示模板.md',
    '10-智能助手技能与任务选型指南.md',
    '17-智能助手任务发起模板速查.md',
  ],
};

const SCAN_EXTENSIONS = new Set([
  '.java',
  '.json',
  '.md',
  '.mjs',
  '.js',
  '.ps1',
  '.py',
  '.sql',
  '.ts',
  '.tsx',
  '.vue',
  '.xml',
  '.yaml',
  '.yml',
  '.txt',
  '.css',
  '.scss',
  '.properties',
  '.html',
  '.sh',
  '.gitignore',
]);

const EXCLUDED_DIRS = new Set([
  '.git',
  '.idea',
  '.codex-runtime',
  'node_modules',
  'target',
  'dist',
  'coverage',
  'logs',
  '.next',
  'build',
]);

const errors = [];

function toRepoPath(filePath) {
  return path.relative(repoRoot, filePath).split(path.sep).join('/');
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function readText(filePath) {
  return fs.readFile(filePath, 'utf8');
}

async function collectFiles(dirPath, predicate) {
  const result = [];
  const entries = await fs.readdir(dirPath, { withFileTypes: true });
  for (const entry of entries) {
    const absolutePath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      if (EXCLUDED_DIRS.has(entry.name)) {
        continue;
      }
      result.push(...(await collectFiles(absolutePath, predicate)));
      continue;
    }
    if (!predicate(absolutePath, entry)) {
      continue;
    }
    result.push(absolutePath);
  }
  return result;
}

function pushError(message) {
  errors.push(message);
}

async function checkRootDocsWhitelist() {
  const entries = await fs.readdir(docsRoot, { withFileTypes: true });
  const actualDocs = entries
    .filter((entry) => entry.isFile() && entry.name.endsWith('.md'))
    .map((entry) => entry.name)
    .sort();

  for (const docName of actualDocs) {
    if (!ROOT_DOC_WHITELIST.has(docName)) {
      pushError(`docs 根目录出现白名单外 Markdown：docs/${docName}`);
    }
  }

  for (const docName of ROOT_DOC_WHITELIST) {
    if (!actualDocs.includes(docName)) {
      pushError(`docs 根目录缺少白名单文档：docs/${docName}`);
    }
  }
}

async function checkLegacyEntryReferences() {
  const candidateFiles = await collectFiles(repoRoot, (absolutePath) => {
    const ext = path.extname(absolutePath);
    return SCAN_EXTENSIONS.has(ext) || path.basename(absolutePath) === '.gitignore';
  });
  const legacyReferencePatterns = LEGACY_DOC_NAMES.flatMap((legacyName) => [
    {
      label: `docs/${legacyName}.md`,
      regex: new RegExp(escapeRegExp(`docs/${legacyName}.md`), 'g'),
    },
    {
      label: `./${legacyName}.md`,
      regex: new RegExp(escapeRegExp(`./${legacyName}.md`), 'g'),
    },
    {
      label: `../${legacyName}.md`,
      regex: new RegExp(escapeRegExp(`../${legacyName}.md`), 'g'),
    },
    {
      label: `](${legacyName}.md`,
      regex: new RegExp(escapeRegExp(`](${legacyName}.md`), 'g'),
    },
  ]);

  for (const filePath of candidateFiles) {
    const repoPath = toRepoPath(filePath);
    if (
      repoPath === 'docs/历史兼容入口索引.md' ||
      repoPath === 'scripts/docs/check-topology.mjs' ||
      repoPath.startsWith('docs/archive/')
    ) {
      continue;
    }

    let content = '';
    try {
      content = await readText(filePath);
    } catch {
      continue;
    }

    for (const pattern of legacyReferencePatterns) {
      if (pattern.regex.test(content)) {
        pushError(`检测到已删除兼容页引用：${repoPath} -> ${pattern.label}`);
      }
    }
  }
}

async function checkRetiredEnglishDocReferences() {
  const candidateFiles = await collectFiles(repoRoot, (absolutePath) => {
    const ext = path.extname(absolutePath);
    return SCAN_EXTENSIONS.has(ext) || path.basename(absolutePath) === '.gitignore';
  });

  for (const filePath of candidateFiles) {
    const repoPath = toRepoPath(filePath);
    if (repoPath === 'scripts/docs/check-topology.mjs' || repoPath.startsWith('docs/archive/')) {
      continue;
    }

    let content = '';
    try {
      content = await readText(filePath);
    } catch {
      continue;
    }

    for (const retiredName of RETIRED_ACTIVE_DOC_NAMES) {
      if (content.includes(retiredName)) {
        pushError(`${repoPath} 仍引用已退役英文文件名：${retiredName}`);
      }
    }
  }
}

async function checkSection21HistoryAppendix() {
  const filePath = path.join(docsRoot, '21-业务功能清单与验收标准.md');
  const content = await readText(filePath);

  for (const heading of EXACT_21_HISTORY_HEADINGS) {
    if (content.includes(heading)) {
      pushError(`docs/21-业务功能清单与验收标准.md 不应再包含历史附录标题：${heading}`);
    }
  }
}

async function checkEntryConsistency() {
  for (const [repoPath, requiredStrings] of Object.entries(ENTRY_DOC_EXPECTATIONS)) {
    const content = await readText(path.join(repoRoot, repoPath));
    for (const expected of requiredStrings) {
      if (!content.includes(expected)) {
        pushError(`${repoPath} 缺少关键阅读入口：${expected}`);
      }
    }
  }
}

async function checkDocQuestionRegistry() {
  const registryPath = path.join(docsRoot, '08-变更记录与技术债清单.md');
  const registryContent = await readText(registryPath);
  const registryIds = new Set();

  for (const line of registryContent.split('\n')) {
    if (!line.startsWith('| DOC-Q-')) {
      continue;
    }
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length < 7) {
      pushError(`DOC-Q 台账列数不足：${line}`);
      continue;
    }
    const [docId, category, subject, ownerDoc, evidenceSource, closeCondition, currentStatus] = cells;
    if (registryIds.has(docId)) {
      pushError(`DOC-Q 台账编号重复：${docId}`);
    }
    registryIds.add(docId);
    if (!category || !subject || !ownerDoc || !evidenceSource || !closeCondition || !currentStatus) {
      pushError(`DOC-Q 台账存在空字段：${line}`);
    }
  }

  if (registryIds.size === 0) {
    pushError('docs/08-变更记录与技术债清单.md 未找到 DOC-Q 台账编号');
  }

  const activeDocFiles = await fs.readdir(docsRoot, { withFileTypes: true });
  for (const entry of activeDocFiles) {
    if (!entry.isFile() || !entry.name.endsWith('.md') || entry.name === '08-变更记录与技术债清单.md') {
      continue;
    }
    const filePath = path.join(docsRoot, entry.name);
    const repoPath = toRepoPath(filePath);
    const content = await readText(filePath);

    if (/【待确认】/.test(content)) {
      pushError(`${repoPath} 仍存在未编号的【待确认】标记`);
    }

    const matches = [...content.matchAll(/【待确认\s+(DOC-Q-\d{3})】/g)];
    for (const match of matches) {
      const docId = match[1];
      if (!registryIds.has(docId)) {
        pushError(`${repoPath} 引用了未在 docs/08 台账登记的编号：${docId}`);
      }
    }
  }
}

async function checkMarkdownRelativeLinks() {
  const markdownFiles = await collectFiles(docsRoot, (absolutePath) => absolutePath.endsWith('.md'));
  const linkPattern = /!?\[[^\]]*]\(([^)]+)\)/g;

  for (const filePath of markdownFiles) {
    const repoPath = toRepoPath(filePath);
    const content = await readText(filePath);

    for (const match of content.matchAll(linkPattern)) {
      let target = match[1].trim().replace(/^<|>$/g, '');
      if (!target || target.startsWith('#')) {
        continue;
      }
      if (/^(https?:|mailto:|tel:|data:)/i.test(target)) {
        continue;
      }
      if (path.isAbsolute(target)) {
        continue;
      }

      if (target.includes(' ')) {
        target = target.split(/\s+/)[0];
      }

      const cleanTarget = target.split('#')[0];
      if (!cleanTarget) {
        continue;
      }

      const resolvedPath = path.resolve(path.dirname(filePath), cleanTarget);
      try {
        await fs.access(resolvedPath);
      } catch {
        pushError(`${repoPath} 存在失效相对链接：${target}`);
      }
    }
  }
}

async function main() {
  await checkRootDocsWhitelist();
  await checkLegacyEntryReferences();
  await checkRetiredEnglishDocReferences();
  await checkSection21HistoryAppendix();
  await checkEntryConsistency();
  await checkDocQuestionRegistry();
  await checkMarkdownRelativeLinks();

  if (errors.length > 0) {
    console.error('Document topology check failed:');
    for (const message of errors) {
      console.error(`- ${message}`);
    }
    process.exitCode = 1;
    return;
  }

  console.log('Document topology check passed.');
}

await main();
