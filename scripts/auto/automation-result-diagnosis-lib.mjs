const CATEGORY_PRIORITY = ['权限', '环境', '接口', 'UI', '数据', '断言', '其他'];

function cleanText(value) {
  return String(value || '').trim();
}

function uniqueParts(parts) {
  return Array.from(new Set(parts.map((item) => cleanText(item)).filter(Boolean)));
}

function truncateText(value, maxLength = 160) {
  const text = cleanText(value);
  if (!text || text.length <= maxLength) {
    return text;
  }
  return `${text.slice(0, maxLength - 1)}...`;
}

function collectSignalText({ result, evidenceTexts = [] }) {
  const details = result?.details && typeof result.details === 'object' ? result.details : {};
  return [
    result?.summary,
    details.stepLabel,
    details.apiRef,
    details.pageAction,
    ...evidenceTexts
  ]
    .map((item) => cleanText(item))
    .filter(Boolean)
    .join('\n')
    .toLowerCase();
}

function buildEvidenceSummary({ result, evidenceTexts = [] }) {
  const details = result?.details && typeof result.details === 'object' ? result.details : {};
  const parts = uniqueParts([
    result?.summary,
    details.stepLabel,
    details.apiRef,
    details.pageAction,
    evidenceTexts[0]
  ]);
  return truncateText(parts.slice(0, 3).join('；')) || '未记录证据摘要';
}

function inferCategory(signalText) {
  if (
    signalText.includes('401') ||
    signalText.includes('403') ||
    signalText.includes('unauthorized') ||
    signalText.includes('forbidden') ||
    signalText.includes('无权限') ||
    signalText.includes('未授权') ||
    signalText.includes('登录失效') ||
    signalText.includes('菜单不可见') ||
    signalText.includes('权限不足')
  ) {
    return { category: '权限', reason: '命中 401/403 或未授权信号' };
  }
  if (
    signalText.includes('econnrefused') ||
    signalText.includes('connection refused') ||
    signalText.includes('timeout') ||
    signalText.includes('timed out') ||
    signalText.includes('etimedout') ||
    signalText.includes('dns') ||
    signalText.includes('服务未启动') ||
    signalText.includes('页面不可达') ||
    signalText.includes('依赖不可用')
  ) {
    return { category: '环境', reason: '命中连接拒绝、超时或依赖不可用信号' };
  }
  if (
    signalText.includes('500') ||
    signalText.includes('502') ||
    signalText.includes('503') ||
    signalText.includes('504') ||
    signalText.includes('接口响应异常') ||
    signalText.includes('response missing') ||
    signalText.includes('响应缺字段') ||
    signalText.includes('contract mismatch')
  ) {
    return { category: '接口', reason: '命中 5xx、响应异常或契约缺口信号' };
  }
  if (
    signalText.includes('selector not found') ||
    signalText.includes('element not found') ||
    signalText.includes('not clickable') ||
    signalText.includes('页面未渲染') ||
    signalText.includes('按钮不可点击') ||
    signalText.includes('对话框未出现')
  ) {
    return { category: 'UI', reason: '命中页面元素未渲染或不可交互信号' };
  }
  if (
    signalText.includes('数据不存在') ||
    signalText.includes('记录为空') ||
    signalText.includes('样本缺失') ||
    signalText.includes('前置数据缺失') ||
    signalText.includes('列表为空')
  ) {
    return { category: '数据', reason: '命中前置数据缺失或结果为空信号' };
  }
  if (
    signalText.includes('asserttext') ||
    signalText.includes('asserturlincludes') ||
    signalText.includes('assertvariableequals') ||
    signalText.includes('assertion failed') ||
    signalText.includes('断言失败')
  ) {
    return { category: '断言', reason: '流程可达但断言不成立' };
  }
  return { category: '其他', reason: '未命中已知规则，建议查看原始证据' };
}

function compareCategoryPriority(left, right) {
  return CATEGORY_PRIORITY.indexOf(left) - CATEGORY_PRIORITY.indexOf(right);
}

function inferModuleCode(result) {
  const details = result?.details && typeof result.details === 'object' ? result.details : {};
  if (cleanText(details.moduleCode)) {
    return cleanText(details.moduleCode);
  }
  const scenarioId = cleanText(result?.scenarioId);
  if (!scenarioId) {
    return 'unknown';
  }
  return cleanText(scenarioId.split('.')[0]) || scenarioId;
}

function inferScenarioTitle(result) {
  const details = result?.details && typeof result.details === 'object' ? result.details : {};
  return cleanText(details.scenarioTitle) || cleanText(result?.scenarioTitle) || cleanText(result?.scenarioId);
}

export function diagnoseFailedScenario({ result, evidenceTexts = [] }) {
  const signalText = collectSignalText({ result, evidenceTexts });
  const diagnosis = inferCategory(signalText);
  const details = result?.details && typeof result.details === 'object' ? result.details : {};
  return {
    scenarioId: cleanText(result?.scenarioId),
    scenarioTitle: inferScenarioTitle(result),
    moduleCode: inferModuleCode(result),
    moduleName: cleanText(details.moduleName) || inferModuleCode(result),
    runnerType: cleanText(result?.runnerType),
    stepLabel: cleanText(details.stepLabel),
    apiRef: cleanText(details.apiRef),
    pageAction: cleanText(details.pageAction),
    diagnosis: {
      category: diagnosis.category,
      reason: diagnosis.reason,
      evidenceSummary: buildEvidenceSummary({ result, evidenceTexts })
    }
  };
}

export function summarizeFailureCategories(failedScenarios = []) {
  const countsByCategory = {};
  for (const scenario of failedScenarios) {
    const category = cleanText(scenario?.diagnosis?.category) || '其他';
    countsByCategory[category] = (countsByCategory[category] || 0) + 1;
  }
  const categories = Object.keys(countsByCategory);
  const primaryCategory = categories.sort((left, right) => {
    const countDiff = countsByCategory[right] - countsByCategory[left];
    return countDiff !== 0 ? countDiff : compareCategoryPriority(left, right);
  })[0] || '其他';
  return {
    primaryCategory,
    countsByCategory
  };
}

export function aggregateFailedModules(failedScenarios = []) {
  const moduleMap = new Map();
  for (const scenario of failedScenarios) {
    const moduleCode = cleanText(scenario?.moduleCode) || 'unknown';
    const bucket = moduleMap.get(moduleCode) || {
      moduleCode,
      moduleName: cleanText(scenario?.moduleName) || moduleCode,
      scenarios: []
    };
    bucket.scenarios.push(scenario);
    moduleMap.set(moduleCode, bucket);
  }

  return Array.from(moduleMap.values()).map((bucket) => {
    const counts = summarizeFailureCategories(bucket.scenarios);
    const topCategoryCount = counts.countsByCategory[counts.primaryCategory] || 0;
    const extraCategories = Object.entries(counts.countsByCategory)
      .filter(([category]) => category !== counts.primaryCategory)
      .map(([category, count]) => `${count} 个 ${category} 问题`);
    const reason = extraCategories.length
      ? `${bucket.scenarios.length} 个失败场景中 ${topCategoryCount} 个命中${counts.primaryCategory}问题，另有 ${extraCategories.join('、')}`
      : `${bucket.scenarios.length} 个失败场景中 ${topCategoryCount} 个命中${counts.primaryCategory}问题`;
    return {
      moduleCode: bucket.moduleCode,
      moduleName: bucket.moduleName,
      failedScenarioCount: bucket.scenarios.length,
      diagnosis: {
        category: counts.primaryCategory,
        reason,
        evidenceSummary: truncateText(
          uniqueParts(bucket.scenarios.map((item) => item?.diagnosis?.evidenceSummary))
            .slice(0, 2)
            .join('；')
        )
      }
    };
  });
}

export function buildRunFailureDiagnosis(results = [], scenarioEvidenceTexts = new Map()) {
  const failedScenarios = results
    .filter((item) => cleanText(item?.status).toLowerCase() !== 'passed')
    .map((item) =>
      diagnoseFailedScenario({
        result: item,
        evidenceTexts: scenarioEvidenceTexts.get(cleanText(item?.scenarioId)) || []
      })
    );

  return {
    failureSummary: summarizeFailureCategories(failedScenarios),
    failedModules: aggregateFailedModules(failedScenarios),
    failedScenarios
  };
}
