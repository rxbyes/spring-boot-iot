import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

function parseRunTimestamp(runTimestamp) {
  if (!/^\d{14}$/.test(runTimestamp || '')) {
    return runTimestamp || '';
  }
  return `${runTimestamp.slice(0, 4)}-${runTimestamp.slice(4, 6)}-${runTimestamp.slice(6, 8)} ${runTimestamp.slice(8, 10)}:${runTimestamp.slice(10, 12)}:${runTimestamp.slice(12, 14)}`;
}

function determineNextSectionNumber(content) {
  const matches = [...content.matchAll(/^##\s+(\d+)\./gm)].map((match) => Number(match[1]));
  return matches.length ? Math.max(...matches) + 1 : 1;
}

function normalizeInlineText(value) {
  return String(value || '')
    .replace(/\r?\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function classifyFailure(errorMessage) {
  const message = normalizeInlineText(errorMessage).toLowerCase();

  if (message.includes('401') || message.includes('未认证') || message.includes('login')) {
    return '登录态或 Bearer 鉴权链路异常，需要先确认会话恢复、路由守卫和接口鉴权是否一致。';
  }
  if (message.includes('500') || message.includes('系统繁忙')) {
    return '后端接口或真实环境依赖异常，需要结合后端日志和共享库状态继续定位。';
  }
  if (message.includes('preflight') || message.includes('health check')) {
    return '前后端可达性或代理目标存在环境阻塞，本轮结论不能替代真实环境复验。';
  }
  if (message.includes('timeout') || message.includes('wait failed') || message.includes('waitfor')) {
    return '页面未进入稳定可操作态，或预期接口未在当前交互链路中触发。';
  }
  if (message.includes('route redirect') || message.includes('switched to login')) {
    return '页面路由发生跳转，需排查权限、菜单授权或页面初始化逻辑。';
  }
  if (message.includes('chromium') || message.includes('browser')) {
    return '本地浏览器执行环境缺失或路径不正确，需要先修复自动化运行前置。';
  }
  return '需要结合截图、网络请求和后端日志进一步判断。';
}

function formatScopeLine(summary, scope, label) {
  const item = summary[scope];
  if (!item) {
    return null;
  }
  return `- ${label}：\`${item.passed}\` 通过 / \`${item.failed}\` 失败`;
}

export async function appendBrowserIssues({
  issueDocPath,
  summary,
  scenarioResults,
  commandHint,
  workspaceRoot
}) {
  const existing = await readFile(issueDocPath, 'utf8');
  if (existing.includes(summary.runTimestamp)) {
    return {
      appended: false,
      reason: 'run already recorded'
    };
  }

  const sectionNumber = determineNextSectionNumber(existing);
  const displayTime = parseRunTimestamp(summary.runTimestamp);
  const failures = scenarioResults.filter((item) => item.status === 'failed');

  const lines = [
    `## ${sectionNumber}. 浏览器自动化巡检记录（${displayTime.slice(0, 10)}）`,
    '',
    '测试方式：浏览器自动化（Playwright）  ',
    `执行时间：${displayTime}（Asia/Shanghai）  `,
    `执行命令：\`${commandHint}\`  `,
    `执行范围：\`${summary.filters.scenarioScopes.join(', ')}\`  `,
    '',
    '结果文件：',
    '',
    `- \`${summary.output.summaryPath}\``,
    `- \`${summary.output.detailPath}\``,
    `- \`${summary.output.reportPath}\``,
    `- \`${summary.output.screenshotsDir}/\``,
    '',
    '### 本轮概览',
    '',
    `- 总场景：\`${summary.counts.total}\``,
    `- 通过：\`${summary.counts.passed}\``,
    `- 失败：\`${summary.counts.failed}\``
  ];

  const deliveryLine = formatScopeLine(summary, 'delivery', '交付范围');
  const baselineLine = formatScopeLine(summary, 'baseline', '基线范围');
  if (deliveryLine) {
    lines.push(deliveryLine);
  }
  if (baselineLine) {
    lines.push(baselineLine);
  }

  if (failures.length === 0) {
    lines.push(
      '',
      '### 本轮结论',
      '',
      '- 本轮未发现新增失败问题。',
      '- 建议仍保留结果文件与截图，作为后续回归对照基线。'
    );
  } else {
    lines.push('', '### 本轮失败问题', '');

    failures.forEach((scenario, index) => {
      const screenshotPath = scenario.screenshotPath || '无截图';
      lines.push(
        `### 问题 ${index + 1}：${scenario.name} 巡检失败`,
        '',
        `- 场景：\`${scenario.key}\``,
        `- 路由：\`${scenario.route}\``,
        `- 范围：\`${scenario.scope}\``,
        `- 现象：${normalizeInlineText(scenario.error)}`,
        `- 初步判断：${classifyFailure(scenario.error)}`,
        `- 证据：\`${summary.output.detailPath}\`；\`${screenshotPath}\``,
        '- 状态：待处理',
        ''
      );
    });
  }

  const nextContent = `${existing.trimEnd()}\n\n${lines.join('\n')}\n`;
  await writeFile(issueDocPath, nextContent, 'utf8');

  return {
    appended: true,
    issueDocPath: path.relative(workspaceRoot, issueDocPath).replace(/\\/g, '/'),
    sectionNumber
  };
}
