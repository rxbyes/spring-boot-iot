import path from 'node:path';

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function slugify(value, fallback = 'item') {
  const normalized = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return normalized || fallback;
}

function toPosixRelative(fromPath, targetPath) {
  return path.relative(path.dirname(fromPath), targetPath).replace(/\\/g, '/');
}

function resolveAbsolutePath(workspaceRoot, targetPath) {
  if (!targetPath) {
    return '';
  }
  return path.isAbsolute(targetPath) ? targetPath : path.resolve(workspaceRoot, targetPath);
}

function formatRatio(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return 'n/a';
  }
  return `${(value * 100).toFixed(4)}%`;
}

function formatThreshold(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return 'n/a';
  }
  return `${(value * 100).toFixed(4)}%`;
}

export function resolveVisualRecordCategory(record) {
  if (record.baselineStatus === 'missing') {
    return 'missing';
  }
  if (record.baselineUpdated) {
    return 'updated';
  }
  if (record.stepStatus === 'failed') {
    return 'mismatch';
  }
  return 'passed';
}

export function collectVisualAssertionRecords(scenarios = []) {
  return scenarios.flatMap((scenario) => {
    const stepResults = Array.isArray(scenario?.detail?.stepResults)
      ? scenario.detail.stepResults
      : Array.isArray(scenario?.stepResults)
        ? scenario.stepResults
        : [];

    return stepResults
      .filter((step) => step?.type === 'assertScreenshot')
      .map((step) => {
        const anchor = slugify(
          `${scenario.key || scenario.name || 'scenario'}-${step.stepId || step.id || step.label || 'step'}`,
          'visual-record'
        );

        return {
          anchor,
          scenarioKey: scenario.key || '',
          scenarioName: scenario.name || '',
          scenarioScope: scenario.scope || '',
          scenarioStatus: scenario.status || '',
          scenarioRoute: scenario.route || '',
          scenarioScreenshotPath: scenario.screenshotPath || '',
          stepId: step.stepId || step.id || '',
          label: step.label || '',
          stepStatus: step.status || '',
          error: step.error || '',
          screenshotTarget: step.screenshotTarget || '',
          fullPage: step.fullPage,
          threshold: typeof step.threshold === 'number' ? step.threshold : undefined,
          baselinePath: step.baselinePath || '',
          actualPath: step.actualPath || '',
          diffPath: step.diffPath || '',
          baselineStatus: step.baselineStatus || '',
          baselineUpdated: Boolean(step.baselineUpdated),
          mismatchPixels: typeof step.mismatchPixels === 'number' ? step.mismatchPixels : 0,
          totalPixels: typeof step.totalPixels === 'number' ? step.totalPixels : 0,
          mismatchRatio: typeof step.mismatchRatio === 'number' ? step.mismatchRatio : undefined,
          baselineWidth: typeof step.baselineWidth === 'number' ? step.baselineWidth : undefined,
          baselineHeight: typeof step.baselineHeight === 'number' ? step.baselineHeight : undefined,
          actualWidth: typeof step.actualWidth === 'number' ? step.actualWidth : undefined,
          actualHeight: typeof step.actualHeight === 'number' ? step.actualHeight : undefined,
          visualReason: step.visualReason || '',
          startedAt: step.startedAt || '',
          finishedAt: step.finishedAt || '',
          category: resolveVisualRecordCategory(step)
        };
      });
  });
}

export function summarizeVisualAssertionRecords(records = []) {
  const summary = {
    total: records.length,
    passed: 0,
    mismatch: 0,
    missing: 0,
    updated: 0,
    actionable: 0
  };

  for (const record of records) {
    const category = record.category || resolveVisualRecordCategory(record);
    if (Object.prototype.hasOwnProperty.call(summary, category)) {
      summary[category] += 1;
    }
    if (['mismatch', 'missing'].includes(category)) {
      summary.actionable += 1;
    }
  }

  return summary;
}

function enrichRecordForHtml(record, workspaceRoot, outputPath) {
  const baselineAbsolutePath = resolveAbsolutePath(workspaceRoot, record.baselinePath);
  const actualAbsolutePath = resolveAbsolutePath(workspaceRoot, record.actualPath);
  const diffAbsolutePath = resolveAbsolutePath(workspaceRoot, record.diffPath);

  return {
    ...record,
    baselineAbsolutePath,
    actualAbsolutePath,
    diffAbsolutePath,
    baselineHref: baselineAbsolutePath ? toPosixRelative(outputPath, baselineAbsolutePath) : '',
    actualHref: actualAbsolutePath ? toPosixRelative(outputPath, actualAbsolutePath) : '',
    diffHref: diffAbsolutePath ? toPosixRelative(outputPath, diffAbsolutePath) : ''
  };
}

function renderImageFigure(title, href, pathLabel, emptyLabel) {
  if (!href || !pathLabel) {
    return [
      '<figure class="image-card image-card--empty">',
      `<figcaption>${escapeHtml(title)}</figcaption>`,
      `<div class="image-empty">${escapeHtml(emptyLabel)}</div>`,
      '</figure>'
    ].join('');
  }

  return [
    '<figure class="image-card">',
    `<figcaption>${escapeHtml(title)}</figcaption>`,
    `<a href="${escapeHtml(href)}" target="_blank" rel="noreferrer">`,
    `<img src="${escapeHtml(href)}" alt="${escapeHtml(title)}" loading="lazy" />`,
    '</a>',
    `<code>${escapeHtml(pathLabel)}</code>`,
    '</figure>'
  ].join('');
}

function renderStatusBadge(category) {
  const labelMap = {
    passed: '通过',
    mismatch: '差异',
    missing: '缺失基线',
    updated: '已刷新基线'
  };
  return `<span class="badge badge--${escapeHtml(category)}">${escapeHtml(labelMap[category] || category)}</span>`;
}

function renderRecordMeta(record) {
  const dimensions =
    typeof record.baselineWidth === 'number' &&
    typeof record.baselineHeight === 'number' &&
    typeof record.actualWidth === 'number' &&
    typeof record.actualHeight === 'number'
      ? `baseline ${record.baselineWidth}x${record.baselineHeight} / actual ${record.actualWidth}x${record.actualHeight}`
      : 'dimensions n/a';

  return [
    `<li><strong>场景</strong> ${escapeHtml(record.scenarioName || record.scenarioKey)}</li>`,
    `<li><strong>路由</strong> <code>${escapeHtml(record.scenarioRoute || 'n/a')}</code></li>`,
    `<li><strong>截图目标</strong> <code>${escapeHtml(record.screenshotTarget || 'page')}</code></li>`,
    `<li><strong>差异比例</strong> <code>${escapeHtml(formatRatio(record.mismatchRatio))}</code></li>`,
    `<li><strong>阈值</strong> <code>${escapeHtml(formatThreshold(record.threshold))}</code></li>`,
    `<li><strong>差异像素</strong> <code>${escapeHtml(String(record.mismatchPixels ?? 0))}</code></li>`,
    `<li><strong>像素尺寸</strong> <code>${escapeHtml(dimensions)}</code></li>`,
    `<li><strong>基线状态</strong> <code>${escapeHtml(record.baselineStatus || record.category)}</code></li>`
  ].join('');
}

function buildHtmlFrame({ title, subtitle, summary, body, sourcePath, commandHint }) {
  return [
    '<!DOCTYPE html>',
    '<html lang="zh-CN">',
    '<head>',
    '<meta charset="utf-8" />',
    `<title>${escapeHtml(title)}</title>`,
    '<meta name="viewport" content="width=device-width, initial-scale=1" />',
    '<style>',
    'body{font-family:"Segoe UI",system-ui,sans-serif;background:#0b1220;color:#e5eefb;margin:0;padding:24px;}',
    '.page{max-width:1500px;margin:0 auto;}',
    'h1,h2,h3{margin:0 0 12px;}',
    '.subtitle{color:#9fb3d1;margin:8px 0 20px;line-height:1.6;}',
    '.summary{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin:20px 0 24px;}',
    '.metric{background:#111c31;border:1px solid #223455;border-radius:14px;padding:16px;}',
    '.metric .label{display:block;color:#8aa1c4;font-size:13px;margin-bottom:8px;}',
    '.metric .value{font-size:28px;font-weight:700;}',
    '.card{background:#111c31;border:1px solid #223455;border-radius:16px;padding:18px;margin-bottom:18px;box-shadow:0 10px 30px rgba(0,0,0,.15);}',
    '.card h3{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}',
    '.badge{display:inline-flex;align-items:center;border-radius:999px;padding:4px 10px;font-size:12px;font-weight:700;}',
    '.badge--passed{background:#123321;color:#8df2b1;}',
    '.badge--mismatch{background:#40211c;color:#ffb4a8;}',
    '.badge--missing{background:#403519;color:#ffd88d;}',
    '.badge--updated{background:#1a2d48;color:#98c6ff;}',
    '.meta{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:8px 18px;padding-left:18px;margin:14px 0;color:#d5e3fb;}',
    '.images{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:14px;margin-top:14px;}',
    '.image-card{background:#0d1728;border:1px solid #263b61;border-radius:14px;padding:12px;}',
    '.image-card figcaption{font-weight:700;margin-bottom:10px;}',
    '.image-card img{width:100%;display:block;border-radius:10px;background:#08111e;}',
    '.image-card code,.command code,.path-list code{display:block;margin-top:8px;font-size:12px;word-break:break-all;color:#9fd6ff;}',
    '.image-card--empty{display:flex;flex-direction:column;justify-content:center;}',
    '.image-empty{min-height:180px;border:1px dashed #36507f;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#7f95b8;background:#0a1424;}',
    '.section-title{margin:28px 0 14px;}',
    '.muted{color:#8aa1c4;}',
    '.path-list{margin:14px 0 0;padding-left:18px;}',
    '.command{background:#0d1728;border:1px solid #24375a;border-radius:14px;padding:14px;margin:20px 0;}',
    '.top-links{display:flex;gap:12px;flex-wrap:wrap;margin:12px 0 0;}',
    '.top-links a{color:#9fd6ff;text-decoration:none;}',
    '.top-links a:hover{text-decoration:underline;}',
    '</style>',
    '</head>',
    '<body>',
    '<div class="page">',
    `<h1>${escapeHtml(title)}</h1>`,
    `<p class="subtitle">${escapeHtml(subtitle)}</p>`,
    '<section class="summary">',
    `<div class="metric"><span class="label">视觉断言总数</span><span class="value">${escapeHtml(String(summary.total))}</span></div>`,
    `<div class="metric"><span class="label">通过</span><span class="value">${escapeHtml(String(summary.passed))}</span></div>`,
    `<div class="metric"><span class="label">差异</span><span class="value">${escapeHtml(String(summary.mismatch))}</span></div>`,
    `<div class="metric"><span class="label">缺失基线</span><span class="value">${escapeHtml(String(summary.missing))}</span></div>`,
    `<div class="metric"><span class="label">已刷新基线</span><span class="value">${escapeHtml(String(summary.updated))}</span></div>`,
    `<div class="metric"><span class="label">待治理</span><span class="value">${escapeHtml(String(summary.actionable))}</span></div>`,
    '</section>',
    sourcePath
      ? `<p class="muted">数据来源：<code>${escapeHtml(sourcePath)}</code></p>`
      : '',
    commandHint
      ? `<div class="command"><strong>治理命令示例</strong><code>${escapeHtml(commandHint)}</code></div>`
      : '',
    body,
    '</div>',
    '</body>',
    '</html>'
  ].join('');
}

export function buildVisualDiffIndexHtml({
  outputPath,
  workspaceRoot,
  records = [],
  sourcePath = '',
  generatedAt = new Date().toISOString(),
  failureDetailPath = ''
}) {
  const summary = summarizeVisualAssertionRecords(records);
  const enriched = records.map((record) => enrichRecordForHtml(record, workspaceRoot, outputPath));
  const detailHref = failureDetailPath ? toPosixRelative(outputPath, failureDetailPath) : '';
  const sections = [];

  sections.push('<h2 class="section-title">Diff 图片索引</h2>');
  if (enriched.length === 0) {
    sections.push('<div class="card"><p class="muted">当前结果中没有视觉断言记录。</p></div>');
  } else {
    for (const record of enriched) {
      sections.push(
        [
          `<article class="card" id="${escapeHtml(record.anchor)}">`,
          `<h3>${renderStatusBadge(record.category)} ${escapeHtml(record.label || record.stepId || '未命名断言')}</h3>`,
          `<ul class="meta">${renderRecordMeta(record)}</ul>`,
          record.error ? `<p class="muted">失败原因：${escapeHtml(record.error)}</p>` : '',
          detailHref && ['mismatch', 'missing'].includes(record.category)
            ? `<div class="top-links"><a href="${escapeHtml(detailHref)}#${escapeHtml(record.anchor)}">查看失败明细</a></div>`
            : '',
          '<div class="images">',
          renderImageFigure('Baseline', record.baselineHref, record.baselinePath, '无 baseline 图'),
          renderImageFigure('Actual', record.actualHref, record.actualPath, '无 actual 图'),
          renderImageFigure('Diff', record.diffHref, record.diffPath, '无 diff 图'),
          '</div>',
          '</article>'
        ].join('')
      );
    }
  }

  return buildHtmlFrame({
    title: 'Visual Diff Index',
    subtitle: `生成时间：${generatedAt}。此页面索引本轮视觉断言的 baseline / actual / diff 产物，便于快速筛查视觉回归。`,
    summary,
    body: sections.join(''),
    sourcePath,
    commandHint: sourcePath
      ? `node scripts/auto/manage-visual-baselines.mjs --input=${sourcePath} --mode=promote --status=missing,mismatch --apply`
      : ''
  });
}

export function buildVisualFailureDetailHtml({
  outputPath,
  workspaceRoot,
  records = [],
  sourcePath = '',
  generatedAt = new Date().toISOString(),
  indexPath = ''
}) {
  const summary = summarizeVisualAssertionRecords(records);
  const actionable = records.filter((record) => ['mismatch', 'missing'].includes(record.category));
  const enriched = actionable.map((record) => enrichRecordForHtml(record, workspaceRoot, outputPath));
  const indexHref = indexPath ? toPosixRelative(outputPath, indexPath) : '';
  const sections = [];

  sections.push('<h2 class="section-title">失败截图明细</h2>');
  if (enriched.length === 0) {
    sections.push('<div class="card"><p class="muted">当前没有需要人工治理的视觉差异或缺失基线。</p></div>');
  } else {
    for (const record of enriched) {
      sections.push(
        [
          `<article class="card" id="${escapeHtml(record.anchor)}">`,
          `<h3>${renderStatusBadge(record.category)} ${escapeHtml(record.scenarioName || record.scenarioKey)} / ${escapeHtml(record.label || record.stepId || '未命名断言')}</h3>`,
          indexHref ? `<div class="top-links"><a href="${escapeHtml(indexHref)}#${escapeHtml(record.anchor)}">返回 diff 索引</a></div>` : '',
          `<ul class="meta">${renderRecordMeta(record)}</ul>`,
          `<p class="muted">失败原因：${escapeHtml(record.error || record.visualReason || 'n/a')}</p>`,
          '<div class="images">',
          renderImageFigure('Baseline', record.baselineHref, record.baselinePath, '缺失 baseline 图'),
          renderImageFigure('Actual', record.actualHref, record.actualPath, '缺失 actual 图'),
          renderImageFigure('Diff', record.diffHref, record.diffPath, '当前无 diff 图'),
          '</div>',
          '<ul class="path-list">',
          `<li><strong>Scenario Key</strong><code>${escapeHtml(record.scenarioKey || 'n/a')}</code></li>`,
          `<li><strong>Step Id</strong><code>${escapeHtml(record.stepId || 'n/a')}</code></li>`,
          `<li><strong>Baseline Path</strong><code>${escapeHtml(record.baselinePath || 'n/a')}</code></li>`,
          `<li><strong>Actual Path</strong><code>${escapeHtml(record.actualPath || 'n/a')}</code></li>`,
          `<li><strong>Diff Path</strong><code>${escapeHtml(record.diffPath || 'n/a')}</code></li>`,
          '</ul>',
          '</article>'
        ].join('')
      );
    }
  }

  return buildHtmlFrame({
    title: 'Visual Failure Detail',
    subtitle: `生成时间：${generatedAt}。此页面聚焦待治理的 mismatch / missing 项，便于评审后批量提升基线。`,
    summary,
    body: sections.join(''),
    sourcePath,
    commandHint: sourcePath
      ? `node scripts/auto/manage-visual-baselines.mjs --input=${sourcePath} --mode=promote --status=missing,mismatch --apply`
      : ''
  });
}

export function buildVisualManifest({
  sourcePath = '',
  generatedAt = new Date().toISOString(),
  records = []
}) {
  return {
    version: '1.0.0',
    generatedAt,
    sourcePath,
    summary: summarizeVisualAssertionRecords(records),
    visualResults: records
  };
}

export function resolveArtifactPrefix(artifacts) {
  const summaryName = path.basename(artifacts?.absolute?.summaryPath || '');
  const matched = summaryName.match(/^(.*)-summary-\d+\.json$/);
  return matched?.[1] || 'browser-acceptance';
}
