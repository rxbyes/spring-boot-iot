import path from 'node:path';

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function resolveAbsolutePath(workspaceRoot, targetPath) {
  if (!targetPath) {
    return '';
  }
  return path.isAbsolute(targetPath) ? targetPath : path.resolve(workspaceRoot, targetPath);
}

function toPosixRelative(fromPath, targetPath) {
  return path.relative(path.dirname(fromPath), targetPath).replace(/\\/g, '/');
}

function formatPercent(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return 'n/a';
  }
  return `${(value * 100).toFixed(2)}%`;
}

function renderStatusBadge(category) {
  const labelMap = {
    passed: '通过',
    mismatch: '差异',
    missing: '缺失基线',
    updated: '已刷新'
  };

  return `<span class="badge badge--${escapeHtml(category)}">${escapeHtml(labelMap[category] || category)}</span>`;
}

function renderRunCards(runs) {
  if (!runs.length) {
    return '<div class="card"><p class="muted">当前没有可分析的视觉历史记录。</p></div>';
  }

  return runs
    .map(
      (run) => [
        '<article class="card run-card">',
        `<h3>${escapeHtml(run.displayTime)}</h3>`,
        `<p class="muted">${escapeHtml(run.inputPath)}</p>`,
        '<div class="mini-grid">',
        `<div><span class="mini-label">断言数</span><strong>${escapeHtml(String(run.summary.total))}</strong></div>`,
        `<div><span class="mini-label">通过</span><strong>${escapeHtml(String(run.summary.passed))}</strong></div>`,
        `<div><span class="mini-label">差异</span><strong>${escapeHtml(String(run.summary.mismatch))}</strong></div>`,
        `<div><span class="mini-label">缺失</span><strong>${escapeHtml(String(run.summary.missing))}</strong></div>`,
        `<div><span class="mini-label">刷新</span><strong>${escapeHtml(String(run.summary.updated))}</strong></div>`,
        `<div><span class="mini-label">待治理</span><strong>${escapeHtml(String(run.summary.actionable))}</strong></div>`,
        '</div>',
        '</article>'
      ].join('')
    )
    .join('');
}

function renderRecentHistory(assertion) {
  if (!Array.isArray(assertion.recentHistory) || assertion.recentHistory.length === 0) {
    return '<span class="muted">无</span>';
  }

  return assertion.recentHistory
    .map(
      (item) =>
        `<span class="history-chip history-chip--${escapeHtml(item.category)}">${escapeHtml(item.shortTime)} · ${escapeHtml(item.label)}</span>`
    )
    .join('');
}

function renderAssertionTableRows(assertions) {
  if (!assertions.length) {
    return '<tr><td colspan="9">暂无数据</td></tr>';
  }

  return assertions
    .map(
      (assertion) => [
        '<tr>',
        `<td>${escapeHtml(assertion.scenarioName || assertion.scenarioKey || 'n/a')}</td>`,
        `<td>${escapeHtml(assertion.label || assertion.stepId || 'n/a')}</td>`,
        `<td>${escapeHtml(String(assertion.totalRuns))}</td>`,
        `<td>${escapeHtml(String(assertion.passed))}</td>`,
        `<td>${escapeHtml(String(assertion.mismatch))}</td>`,
        `<td>${escapeHtml(String(assertion.missing))}</td>`,
        `<td>${escapeHtml(formatPercent(assertion.instabilityRate))}</td>`,
        `<td>${escapeHtml(formatPercent(assertion.maxMismatchRatio))}</td>`,
        `<td>${renderRecentHistory(assertion)}</td>`,
        '</tr>'
      ].join('')
    )
    .join('');
}

function renderLatestActionableCards(records, workspaceRoot, outputPath) {
  if (!records.length) {
    return '<div class="card"><p class="muted">最新一轮没有待治理的 mismatch / missing 视觉断言。</p></div>';
  }

  return records
    .map((record) => {
      const actualAbsolutePath = resolveAbsolutePath(workspaceRoot, record.actualPath);
      const diffAbsolutePath = resolveAbsolutePath(workspaceRoot, record.diffPath);
      const baselineAbsolutePath = resolveAbsolutePath(workspaceRoot, record.baselinePath);

      const actualHref = actualAbsolutePath ? toPosixRelative(outputPath, actualAbsolutePath) : '';
      const diffHref = diffAbsolutePath ? toPosixRelative(outputPath, diffAbsolutePath) : '';
      const baselineHref = baselineAbsolutePath ? toPosixRelative(outputPath, baselineAbsolutePath) : '';

      return [
        '<article class="card">',
        `<h3>${renderStatusBadge(record.category)} ${escapeHtml(record.scenarioName || record.scenarioKey || 'n/a')} / ${escapeHtml(record.label || record.stepId || 'n/a')}</h3>`,
        `<p class="muted">${escapeHtml(record.displayTime)} · 差异比例 ${escapeHtml(formatPercent(record.mismatchRatio))} · 阈值 ${escapeHtml(formatPercent(record.threshold))}</p>`,
        '<ul class="path-list">',
        `<li><strong>Baseline</strong> ${baselineHref ? `<a href="${escapeHtml(baselineHref)}" target="_blank" rel="noreferrer"><code>${escapeHtml(record.baselinePath)}</code></a>` : '<code>n/a</code>'}</li>`,
        `<li><strong>Actual</strong> ${actualHref ? `<a href="${escapeHtml(actualHref)}" target="_blank" rel="noreferrer"><code>${escapeHtml(record.actualPath)}</code></a>` : '<code>n/a</code>'}</li>`,
        `<li><strong>Diff</strong> ${diffHref ? `<a href="${escapeHtml(diffHref)}" target="_blank" rel="noreferrer"><code>${escapeHtml(record.diffPath || 'n/a')}</code></a>` : '<code>n/a</code>'}</li>`,
        '</ul>',
        record.error ? `<p class="muted">失败原因：${escapeHtml(record.error)}</p>` : '',
        '</article>'
      ].join('');
    })
    .join('');
}

function renderRecommendationList(recommendations) {
  if (!recommendations.length) {
    return '<li>暂无额外建议。</li>';
  }

  return recommendations.map((item) => `<li>${escapeHtml(item)}</li>`).join('');
}

function renderFileList(files) {
  if (!files.length) {
    return '<li>暂无文件</li>';
  }

  return files
    .map((file) => `<li><code>${escapeHtml(file)}</code></li>`)
    .join('');
}

export function buildVisualHistoryMarkdown(report) {
  const lines = [
    '# Visual Regression History Report',
    '',
    `- 生成时间：\`${report.generatedAt}\``,
    `- 分析文件数：\`${report.summary.fileCount}\``,
    `- 覆盖轮次：\`${report.summary.runCount}\``,
    `- 视觉断言总数：\`${report.summary.total}\``,
    `- 通过：\`${report.summary.passed}\``,
    `- 差异：\`${report.summary.mismatch}\``,
    `- 缺失基线：\`${report.summary.missing}\``,
    `- 已刷新：\`${report.summary.updated}\``,
    `- 待治理：\`${report.summary.actionable}\``,
    '',
    '## 过滤条件',
    '',
    `- 输入：\`${report.requestedInputs.join(', ') || 'logs/acceptance'}\``,
    `- 状态：\`${report.filters.statuses.join(', ') || 'all'}\``,
    `- 场景：\`${report.filters.scenario || 'all'}\``,
    `- 步骤：\`${report.filters.label || 'all'}\``,
    `- 文件上限：\`${report.filters.limit}\``,
    '',
    '## 时间线',
    '',
    '| 时间 | 文件 | 总数 | 通过 | 差异 | 缺失 | 刷新 | 待治理 |',
    '|---|---|---|---|---|---|---|---|'
  ];

  for (const run of report.runs) {
    lines.push(
      `| ${run.displayTime} | \`${run.inputPath}\` | ${run.summary.total} | ${run.summary.passed} | ${run.summary.mismatch} | ${run.summary.missing} | ${run.summary.updated} | ${run.summary.actionable} |`
    );
  }

  lines.push(
    '',
    '## Top Unstable Assertions',
    '',
    '| 场景 | 步骤 | 轮次 | 差异 | 缺失 | 不稳定率 | 最大差异 | 最近状态 |',
    '|---|---|---|---|---|---|---|---|'
  );

  for (const item of report.hotspots.unstableAssertions) {
    lines.push(
      `| ${item.scenarioName || item.scenarioKey || 'n/a'} | ${item.label || item.stepId || 'n/a'} | ${item.totalRuns} | ${item.mismatch} | ${item.missing} | ${formatPercent(item.instabilityRate)} | ${formatPercent(item.maxMismatchRatio)} | ${item.latestCategory} |`
    );
  }

  lines.push(
    '',
    '## Recommendations',
    ''
  );

  for (const recommendation of report.recommendations) {
    lines.push(`- ${recommendation}`);
  }

  lines.push(
    '',
    '## Files',
    ''
  );

  for (const file of report.analyzedFiles) {
    lines.push(`- \`${file}\``);
  }

  return lines.join('\n');
}

export function buildVisualHistoryDashboardHtml({ report, workspaceRoot, outputPath }) {
  const html = [
    '<!DOCTYPE html>',
    '<html lang="zh-CN">',
    '<head>',
    '<meta charset="utf-8" />',
    '<meta name="viewport" content="width=device-width, initial-scale=1" />',
    '<title>Visual Regression History Dashboard</title>',
    '<style>',
    'body{font-family:"Segoe UI",system-ui,sans-serif;background:#0b1220;color:#e5eefb;margin:0;padding:24px;}',
    '.page{max-width:1600px;margin:0 auto;}',
    'h1,h2,h3{margin:0 0 12px;}',
    '.subtitle{color:#8fa8cf;line-height:1.7;margin:10px 0 20px;}',
    '.summary{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin:20px 0 28px;}',
    '.metric{background:#111c31;border:1px solid #223455;border-radius:16px;padding:16px;}',
    '.metric .label{display:block;color:#87a0c7;font-size:13px;margin-bottom:8px;}',
    '.metric .value{font-size:30px;font-weight:700;}',
    '.section{margin-top:26px;}',
    '.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:14px;}',
    '.card{background:#111c31;border:1px solid #223455;border-radius:16px;padding:18px;box-shadow:0 10px 28px rgba(0,0,0,.16);}',
    '.run-card h3{margin-bottom:6px;}',
    '.muted{color:#8fa8cf;}',
    '.mini-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-top:14px;}',
    '.mini-label{display:block;color:#7f95b8;font-size:12px;margin-bottom:4px;}',
    '.badge{display:inline-flex;align-items:center;border-radius:999px;padding:4px 10px;font-size:12px;font-weight:700;}',
    '.badge--passed{background:#123321;color:#8df2b1;}',
    '.badge--mismatch{background:#40211c;color:#ffb4a8;}',
    '.badge--missing{background:#403519;color:#ffd88d;}',
    '.badge--updated{background:#1a2d48;color:#98c6ff;}',
    '.table-wrap{overflow:auto;background:#111c31;border:1px solid #223455;border-radius:16px;}',
    'table{width:100%;border-collapse:collapse;min-width:960px;}',
    'th,td{padding:12px 14px;border-bottom:1px solid #1f3152;text-align:left;vertical-align:top;}',
    'th{color:#9fb6da;font-weight:700;background:#0f1830;position:sticky;top:0;}',
    'tr:last-child td{border-bottom:none;}',
    '.history-chip{display:inline-flex;align-items:center;border-radius:999px;padding:4px 8px;margin:0 6px 6px 0;font-size:12px;font-weight:700;}',
    '.history-chip--passed{background:#123321;color:#8df2b1;}',
    '.history-chip--mismatch{background:#40211c;color:#ffb4a8;}',
    '.history-chip--missing{background:#403519;color:#ffd88d;}',
    '.history-chip--updated{background:#1a2d48;color:#98c6ff;}',
    '.filter-list,.path-list{margin:0;padding-left:18px;line-height:1.7;}',
    'a{color:#9fd6ff;text-decoration:none;}',
    'a:hover{text-decoration:underline;}',
    'code{word-break:break-all;color:#9fd6ff;}',
    '</style>',
    '</head>',
    '<body>',
    '<div class="page">',
    '<h1>Visual Regression History Dashboard</h1>',
    `<p class="subtitle">生成时间：${escapeHtml(report.generatedAt)}。该看板聚合多轮 visual manifest / browser results，帮助识别高频差异、缺失基线和基线刷新波动。</p>`,
    '<section class="summary">',
    `<div class="metric"><span class="label">分析文件数</span><span class="value">${escapeHtml(String(report.summary.fileCount))}</span></div>`,
    `<div class="metric"><span class="label">覆盖轮次</span><span class="value">${escapeHtml(String(report.summary.runCount))}</span></div>`,
    `<div class="metric"><span class="label">视觉断言总数</span><span class="value">${escapeHtml(String(report.summary.total))}</span></div>`,
    `<div class="metric"><span class="label">通过</span><span class="value">${escapeHtml(String(report.summary.passed))}</span></div>`,
    `<div class="metric"><span class="label">差异</span><span class="value">${escapeHtml(String(report.summary.mismatch))}</span></div>`,
    `<div class="metric"><span class="label">缺失基线</span><span class="value">${escapeHtml(String(report.summary.missing))}</span></div>`,
    `<div class="metric"><span class="label">已刷新</span><span class="value">${escapeHtml(String(report.summary.updated))}</span></div>`,
    `<div class="metric"><span class="label">待治理</span><span class="value">${escapeHtml(String(report.summary.actionable))}</span></div>`,
    '</section>',
    '<section class="section grid">',
    '<article class="card">',
    '<h2>过滤条件</h2>',
    '<ul class="filter-list">',
    `<li><strong>输入</strong> <code>${escapeHtml(report.requestedInputs.join(', ') || 'logs/acceptance')}</code></li>`,
    `<li><strong>状态</strong> <code>${escapeHtml(report.filters.statuses.join(', ') || 'all')}</code></li>`,
    `<li><strong>场景</strong> <code>${escapeHtml(report.filters.scenario || 'all')}</code></li>`,
    `<li><strong>步骤</strong> <code>${escapeHtml(report.filters.label || 'all')}</code></li>`,
    `<li><strong>文件上限</strong> <code>${escapeHtml(String(report.filters.limit))}</code></li>`,
    '</ul>',
    '</article>',
    '<article class="card">',
    '<h2>治理建议</h2>',
    `<ul class="filter-list">${renderRecommendationList(report.recommendations)}</ul>`,
    '</article>',
    '</section>',
    '<section class="section">',
    '<h2>执行时间线</h2>',
    `<div class="grid">${renderRunCards(report.runs)}</div>`,
    '</section>',
    '<section class="section">',
    '<h2>高风险断言</h2>',
    '<div class="table-wrap"><table><thead><tr><th>场景</th><th>步骤</th><th>轮次</th><th>通过</th><th>差异</th><th>缺失</th><th>不稳定率</th><th>最大差异</th><th>最近状态序列</th></tr></thead><tbody>',
    renderAssertionTableRows(report.hotspots.unstableAssertions),
    '</tbody></table></div>',
    '</section>',
    '<section class="section">',
    '<h2>基线刷新波动</h2>',
    '<div class="table-wrap"><table><thead><tr><th>场景</th><th>步骤</th><th>轮次</th><th>通过</th><th>差异</th><th>缺失</th><th>不稳定率</th><th>最大差异</th><th>最近状态序列</th></tr></thead><tbody>',
    renderAssertionTableRows(report.hotspots.churnAssertions),
    '</tbody></table></div>',
    '</section>',
    '<section class="section">',
    '<h2>最新一轮待治理项</h2>',
    `<div class="grid">${renderLatestActionableCards(report.latestActionableRecords, workspaceRoot, outputPath)}</div>`,
    '</section>',
    '<section class="section grid">',
    '<article class="card">',
    '<h2>分析文件</h2>',
    `<ul class="filter-list">${renderFileList(report.analyzedFiles)}</ul>`,
    '</article>',
    '<article class="card">',
    '<h2>输出文件</h2>',
    '<ul class="filter-list">',
    `<li><code>${escapeHtml(report.output.summaryPath)}</code></li>`,
    `<li><code>${escapeHtml(report.output.reportPath)}</code></li>`,
    `<li><code>${escapeHtml(report.output.dashboardPath)}</code></li>`,
    '</ul>',
    '</article>',
    '</section>',
    '</div>',
    '</body>',
    '</html>'
  ];

  return html.join('');
}
