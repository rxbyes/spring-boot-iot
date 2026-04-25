const METADATA_PRIORITY_RE = /^P[12]$/i;
const PRIORITY_RE = /(?:^|[^A-Za-z0-9])P([0-9])(?:[^A-Za-z0-9]|$)/i;
const KNOWN_PRIORITY_ORDER = ['P0', 'P1', 'P2', 'P3', 'UNASSIGNED'];
const REQUIRED_METADATA_FIELDS = [
  'ownerDomain',
  'failureCategory',
  'dataSetup.strategy',
  'cleanupPolicy.strategy'
];
const POLICY_RULE_IDS = new Set([
  'missingScenarioRefs',
  'unreferencedScenarios',
  'metadata',
  'minimumScenarioCountByPriority',
  'requiredRunnerTypes'
]);
const POLICY_SEVERITIES = new Set(['error', 'warning']);

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function cleanText(value) {
  return String(value || '').trim();
}

function normalizePriority(value) {
  const text = cleanText(value).toUpperCase();
  return /^P[0-9]$/.test(text) ? text : '';
}

function inferPriorityFromText(value) {
  const match = cleanText(value).match(PRIORITY_RE);
  return match ? `P${match[1]}` : '';
}

function rankPriority(priority) {
  if (priority === 'UNASSIGNED') {
    return Number.POSITIVE_INFINITY;
  }
  const match = normalizePriority(priority).match(/^P([0-9])$/);
  return match ? Number(match[1]) : Number.POSITIVE_INFINITY;
}

function chooseHighestPriority(priorities) {
  return priorities
    .map(normalizePriority)
    .filter(Boolean)
    .sort((left, right) => rankPriority(left) - rankPriority(right))[0];
}

function normalizeScenario(source = {}) {
  return {
    id: cleanText(source.id),
    title: cleanText(source.title),
    module: cleanText(source.module),
    runnerType: cleanText(source.runnerType) || 'unknown',
    scope: cleanText(source.scope),
    blocking: cleanText(source.blocking) || 'unknown',
    ownerDomain: cleanText(source.ownerDomain),
    priority: normalizePriority(source.priority),
    failureCategory: cleanText(source.failureCategory),
    dataSetup: source.dataSetup && typeof source.dataSetup === 'object' ? source.dataSetup : {},
    cleanupPolicy:
      source.cleanupPolicy && typeof source.cleanupPolicy === 'object'
        ? source.cleanupPolicy
        : {}
  };
}

function normalizeScenarioRefs(source = []) {
  return asArray(source).map(cleanText).filter(Boolean);
}

function normalizePackage(source = {}) {
  const packageCode = cleanText(source.packageCode);
  const packageName = cleanText(source.packageName);
  const inferredPriority =
    inferPriorityFromText(packageCode) || inferPriorityFromText(packageName);

  return {
    packageCode,
    packageName,
    priority: inferredPriority,
    modules: asArray(source.modules).map((module) => ({
      moduleCode: cleanText(module.moduleCode),
      moduleName: cleanText(module.moduleName),
      scenarioRefs: normalizeScenarioRefs(module.scenarioRefs)
    }))
  };
}

function createCountBucket() {
  return {
    total: 0,
    scenarioIds: []
  };
}

function incrementScenarioBucket(index, key, scenarioId) {
  if (!index[key]) {
    index[key] = createCountBucket();
  }
  index[key].total += 1;
  index[key].scenarioIds.push(scenarioId);
}

function normalizeCountIndex(index, preferredKeys = []) {
  const normalized = {};
  preferredKeys.forEach((key) => {
    normalized[key] = index[key] || createCountBucket();
  });
  Object.keys(index)
    .filter((key) => !(key in normalized))
    .sort()
    .forEach((key) => {
      normalized[key] = index[key];
    });
  return normalized;
}

function getMissingMetadataFields(scenario, resolvedPriority) {
  if (!METADATA_PRIORITY_RE.test(resolvedPriority)) {
    return [];
  }

  return REQUIRED_METADATA_FIELDS.filter((field) => {
    if (field === 'ownerDomain') {
      return !scenario.ownerDomain;
    }
    if (field === 'failureCategory') {
      return !scenario.failureCategory;
    }
    if (field === 'dataSetup.strategy') {
      return !cleanText(scenario.dataSetup?.strategy);
    }
    if (field === 'cleanupPolicy.strategy') {
      return !cleanText(scenario.cleanupPolicy?.strategy);
    }
    return false;
  });
}

function buildScenarioPackageRefs({ normalizedPackages, scenarioById }) {
  const scenarioPackageRefs = new Map();
  const missingScenarioRefs = [];
  let totalModules = 0;
  let totalScenarioReferences = 0;

  const packages = normalizedPackages.map((pkg) => {
    const modules = pkg.modules.map((module) => {
      totalModules += 1;
      const scenarioRefs = module.scenarioRefs.map((scenarioRef) => {
        totalScenarioReferences += 1;
        const scenario = scenarioById.get(scenarioRef);
        const ref = {
          scenarioRef,
          exists: Boolean(scenario),
          scenarioTitle: scenario?.title || '',
          scenarioModule: scenario?.module || '',
          runnerType: scenario?.runnerType || ''
        };

        if (scenario) {
          const refs = scenarioPackageRefs.get(scenarioRef) || [];
          refs.push({
            packageCode: pkg.packageCode,
            packageName: pkg.packageName,
            packagePriority: pkg.priority,
            moduleCode: module.moduleCode,
            moduleName: module.moduleName
          });
          scenarioPackageRefs.set(scenarioRef, refs);
        } else {
          missingScenarioRefs.push({
            packageCode: pkg.packageCode,
            packageName: pkg.packageName,
            moduleCode: module.moduleCode,
            moduleName: module.moduleName,
            scenarioRef
          });
        }

        return ref;
      });

      return {
        moduleCode: module.moduleCode,
        moduleName: module.moduleName,
        scenarioRefs
      };
    });

    return {
      packageCode: pkg.packageCode,
      packageName: pkg.packageName,
      priority: pkg.priority || 'UNASSIGNED',
      modules
    };
  });

  return {
    packages,
    scenarioPackageRefs,
    missingScenarioRefs,
    totalModules,
    totalScenarioReferences
  };
}

function requirePolicyObject(policy) {
  if (!policy || typeof policy !== 'object' || Array.isArray(policy)) {
    throw new Error('Coverage policy must be a JSON object.');
  }
  const rules = policy.rules;
  if (!rules || typeof rules !== 'object' || Array.isArray(rules)) {
    throw new Error('Coverage policy rules must be a JSON object.');
  }
  Object.entries(rules).forEach(([ruleId, rule]) => {
    if (!POLICY_RULE_IDS.has(ruleId)) {
      throw new Error(`Unknown coverage policy rule: ${ruleId}`);
    }
    if (!rule || typeof rule !== 'object' || Array.isArray(rule)) {
      throw new Error(`Coverage policy rule must be an object: ${ruleId}`);
    }
    if (!POLICY_SEVERITIES.has(rule.severity)) {
      throw new Error(
        `Unsupported coverage policy severity for ${ruleId}: ${rule.severity}`
      );
    }
  });
}

function policyArray(value) {
  return Array.isArray(value) ? value.map(cleanText).filter(Boolean) : [];
}

function createPolicyResult({
  ruleId,
  severity,
  failedDetails,
  passMessage,
  failMessage
}) {
  const failed = failedDetails.length > 0;
  return {
    ruleId,
    severity,
    status: failed ? 'failed' : 'passed',
    message: failed ? failMessage : passMessage,
    details: failedDetails
  };
}

function evaluateMissingScenarioRefs(matrix, rule) {
  const allowed = new Set(policyArray(rule.allowScenarioRefs));
  const failedDetails = matrix.gaps.missingScenarioRefs.filter(
    (item) => !allowed.has(item.scenarioRef)
  );
  return createPolicyResult({
    ruleId: 'missingScenarioRefs',
    severity: rule.severity,
    failedDetails,
    passMessage: 'No missing scenario references.',
    failMessage: `${failedDetails.length} missing scenario reference(s) are not allowed.`
  });
}

function evaluateUnreferencedScenarios(matrix, rule) {
  const allowedIds = new Set(policyArray(rule.allowScenarioIds));
  const allowedScopes = new Set(policyArray(rule.allowScopes));
  const failedDetails = matrix.gaps.unreferencedScenarios.filter(
    (item) => !allowedIds.has(item.scenarioId) && !allowedScopes.has(item.scope)
  );
  return createPolicyResult({
    ruleId: 'unreferencedScenarios',
    severity: rule.severity,
    failedDetails,
    passMessage: 'No disallowed unreferenced scenarios.',
    failMessage: `${failedDetails.length} unreferenced scenario(s) require packaging or explicit allowance.`
  });
}

function evaluateMetadata(matrix, rule) {
  const requiredPriorities = new Set(policyArray(rule.requiredPriorities));
  const failedDetails = matrix.gaps.missingMetadata.filter((item) =>
    requiredPriorities.has(item.resolvedPriority)
  );
  return createPolicyResult({
    ruleId: 'metadata',
    severity: rule.severity,
    failedDetails,
    passMessage: 'Required priority metadata is complete.',
    failMessage: `${failedDetails.length} scenario(s) are missing required governance metadata.`
  });
}

function evaluateMinimumScenarioCountByPriority(matrix, rule) {
  const minimums =
    rule.minimums && typeof rule.minimums === 'object' && !Array.isArray(rule.minimums)
      ? rule.minimums
      : {};
  const failedDetails = Object.entries(minimums)
    .map(([priority, minimum]) => ({
      priority,
      minimum: Number(minimum),
      actual: matrix.coverageByPriority[priority]?.total || 0
    }))
    .filter((item) => Number.isFinite(item.minimum) && item.actual < item.minimum);
  return createPolicyResult({
    ruleId: 'minimumScenarioCountByPriority',
    severity: rule.severity,
    failedDetails,
    passMessage: 'Priority scenario minimums are satisfied.',
    failMessage: `${failedDetails.length} priority scenario minimum(s) are not satisfied.`
  });
}

function evaluateRequiredRunnerTypes(matrix, rule) {
  const failedDetails = policyArray(rule.runnerTypes)
    .map((runnerType) => ({
      runnerType,
      actual: matrix.coverageByRunnerType[runnerType]?.total || 0
    }))
    .filter((item) => item.actual === 0);
  return createPolicyResult({
    ruleId: 'requiredRunnerTypes',
    severity: rule.severity,
    failedDetails,
    passMessage: 'Required runner types are represented.',
    failMessage: `${failedDetails.length} required runner type(s) are missing from coverage.`
  });
}

export function buildCoverageMatrix({ registry, packages } = {}) {
  const normalizedScenarios = asArray(registry?.scenarios)
    .map(normalizeScenario)
    .filter((scenario) => scenario.id);
  const normalizedPackages = asArray(packages?.packages).map(normalizePackage);
  const scenarioById = new Map(
    normalizedScenarios.map((scenario) => [scenario.id, scenario])
  );
  const {
    packages: packageRows,
    scenarioPackageRefs,
    missingScenarioRefs,
    totalModules,
    totalScenarioReferences
  } = buildScenarioPackageRefs({ normalizedPackages, scenarioById });

  const coverageByPriority = {};
  const coverageByRunnerType = {};
  const coverageByOwnerDomain = {};
  const coverageByBlocking = {};
  const missingMetadata = [];
  const unreferencedScenarios = [];

  const scenarioRows = normalizedScenarios.map((scenario) => {
    const packageRefs = scenarioPackageRefs.get(scenario.id) || [];
    const packagePriorities = packageRefs.map((item) => item.packagePriority);
    const resolvedPriority =
      scenario.priority || chooseHighestPriority(packagePriorities) || 'UNASSIGNED';
    const ownerDomain = scenario.ownerDomain || scenario.module || 'unassigned';
    const missingFields = getMissingMetadataFields(scenario, resolvedPriority);
    const metadataRequired = METADATA_PRIORITY_RE.test(resolvedPriority);

    if (packageRefs.length === 0) {
      unreferencedScenarios.push({
        scenarioId: scenario.id,
        module: scenario.module,
        runnerType: scenario.runnerType,
        scope: scenario.scope,
        resolvedPriority
      });
    }

    if (missingFields.length > 0) {
      missingMetadata.push({
        scenarioId: scenario.id,
        resolvedPriority,
        missingFields
      });
    }

    incrementScenarioBucket(coverageByPriority, resolvedPriority, scenario.id);
    incrementScenarioBucket(coverageByRunnerType, scenario.runnerType, scenario.id);
    incrementScenarioBucket(coverageByOwnerDomain, ownerDomain, scenario.id);
    incrementScenarioBucket(coverageByBlocking, scenario.blocking, scenario.id);

    return {
      scenarioId: scenario.id,
      title: scenario.title,
      module: scenario.module,
      runnerType: scenario.runnerType,
      scope: scenario.scope,
      blocking: scenario.blocking,
      ownerDomain,
      explicitPriority: scenario.priority,
      resolvedPriority,
      packageRefs,
      metadataReadiness: {
        required: metadataRequired,
        ready: missingFields.length === 0,
        missingFields
      }
    };
  });

  const hasGaps =
    missingScenarioRefs.length > 0 ||
    unreferencedScenarios.length > 0 ||
    missingMetadata.length > 0;

  return {
    generatedAt: new Date().toISOString(),
    registryVersion: cleanText(registry?.version) || '1.0.0',
    packageVersion: cleanText(packages?.version) || '1.0.0',
    summary: {
      totalScenarios: normalizedScenarios.length,
      totalPackages: normalizedPackages.length,
      totalModules,
      totalScenarioReferences,
      referencedScenarios: scenarioPackageRefs.size,
      missingScenarioRefs: missingScenarioRefs.length,
      unreferencedScenarios: unreferencedScenarios.length,
      metadataRequiredScenarios: scenarioRows.filter(
        (item) => item.metadataReadiness.required
      ).length,
      metadataMissingScenarios: missingMetadata.length,
      hasGaps
    },
    coverageByPriority: normalizeCountIndex(
      coverageByPriority,
      KNOWN_PRIORITY_ORDER
    ),
    coverageByRunnerType: normalizeCountIndex(coverageByRunnerType),
    coverageByOwnerDomain: normalizeCountIndex(coverageByOwnerDomain),
    coverageByBlocking: normalizeCountIndex(coverageByBlocking),
    scenarios: scenarioRows,
    packages: packageRows,
    gaps: {
      total:
        missingScenarioRefs.length +
        unreferencedScenarios.length +
        missingMetadata.length,
      missingScenarioRefs,
      unreferencedScenarios,
      missingMetadata
    }
  };
}

export function evaluateCoveragePolicy(matrix, policy) {
  requirePolicyObject(policy);
  const rules = policy.rules;
  const results = Object.entries(rules).map(([ruleId, rule]) => {
    if (ruleId === 'missingScenarioRefs') {
      return evaluateMissingScenarioRefs(matrix, rule);
    }
    if (ruleId === 'unreferencedScenarios') {
      return evaluateUnreferencedScenarios(matrix, rule);
    }
    if (ruleId === 'metadata') {
      return evaluateMetadata(matrix, rule);
    }
    if (ruleId === 'minimumScenarioCountByPriority') {
      return evaluateMinimumScenarioCountByPriority(matrix, rule);
    }
    return evaluateRequiredRunnerTypes(matrix, rule);
  });
  const failed = results.filter((item) => item.status === 'failed');
  const errors = failed.filter((item) => item.severity === 'error').length;
  const warnings = failed.filter((item) => item.severity === 'warning').length;

  return {
    policyName: cleanText(policy.policyName) || 'unnamed-policy',
    policyVersion: cleanText(policy.version) || '1.0.0',
    status: errors > 0 ? 'failed' : warnings > 0 ? 'warning' : 'passed',
    summary: {
      totalRules: results.length,
      passed: results.filter((item) => item.status === 'passed').length,
      failed: failed.length,
      warnings,
      errors
    },
    results
  };
}

function escapeTableText(value) {
  return cleanText(value).replace(/\|/g, '\\|');
}

function renderCountTable(title, index, keyLabel) {
  const lines = [
    `## ${title}`,
    '',
    `| ${keyLabel} | Total | Scenario IDs |`,
    '|---|---:|---|'
  ];

  Object.entries(index).forEach(([key, bucket]) => {
    lines.push(
      `| ${escapeTableText(key)} | ${bucket.total} | ${escapeTableText(
        bucket.scenarioIds.join(', ')
      )} |`
    );
  });

  return lines;
}

export function renderCoverageMarkdown(matrix) {
  const lines = [
    '# Acceptance Coverage Matrix',
    '',
    `- Generated At: \`${matrix.generatedAt}\``,
    `- Registry Version: \`${matrix.registryVersion}\``,
    `- Package Version: \`${matrix.packageVersion}\``,
    '',
    '## Summary',
    '',
    '| Metric | Value |',
    '|---|---:|',
    `| Total scenarios | ${matrix.summary.totalScenarios} |`,
    `| Total packages | ${matrix.summary.totalPackages} |`,
    `| Total modules | ${matrix.summary.totalModules} |`,
    `| Scenario references | ${matrix.summary.totalScenarioReferences} |`,
    `| Missing scenario refs | ${matrix.summary.missingScenarioRefs} |`,
    `| Unreferenced scenarios | ${matrix.summary.unreferencedScenarios} |`,
    `| Metadata missing scenarios | ${matrix.summary.metadataMissingScenarios} |`,
    '',
    ...renderCountTable('Coverage By Priority', matrix.coverageByPriority, 'Priority'),
    '',
    ...renderCountTable('Coverage By Runner Type', matrix.coverageByRunnerType, 'Runner'),
    '',
    ...renderCountTable(
      'Coverage By Owner Domain',
      matrix.coverageByOwnerDomain,
      'Owner Domain'
    ),
    '',
    '## Gaps',
    '',
    `- Total gaps: \`${matrix.gaps.total}\``,
    `- Missing scenario refs: \`${matrix.gaps.missingScenarioRefs.length}\``,
    `- Unreferenced scenarios: \`${matrix.gaps.unreferencedScenarios.length}\``,
    `- Missing metadata: \`${matrix.gaps.missingMetadata.length}\``,
    '',
    '### Missing Scenario References',
    '',
    '| Package | Module | Scenario Ref |',
    '|---|---|---|'
  ];

  if (matrix.gaps.missingScenarioRefs.length === 0) {
    lines.push('| - | - | - |');
  } else {
    matrix.gaps.missingScenarioRefs.forEach((gap) => {
      lines.push(
        `| ${escapeTableText(gap.packageCode)} | ${escapeTableText(
          gap.moduleCode
        )} | ${escapeTableText(gap.scenarioRef)} |`
      );
    });
  }

  lines.push(
    '',
    '### Unreferenced Scenarios',
    '',
    '| Scenario | Module | Runner | Priority |',
    '|---|---|---|---|'
  );

  if (matrix.gaps.unreferencedScenarios.length === 0) {
    lines.push('| - | - | - | - |');
  } else {
    matrix.gaps.unreferencedScenarios.forEach((gap) => {
      lines.push(
        `| ${escapeTableText(gap.scenarioId)} | ${escapeTableText(
          gap.module
        )} | ${escapeTableText(gap.runnerType)} | ${escapeTableText(
          gap.resolvedPriority
        )} |`
      );
    });
  }

  lines.push(
    '',
    '### Missing Metadata',
    '',
    '| Scenario | Priority | Missing Fields |',
    '|---|---|---|'
  );

  if (matrix.gaps.missingMetadata.length === 0) {
    lines.push('| - | - | - |');
  } else {
    matrix.gaps.missingMetadata.forEach((gap) => {
      lines.push(
        `| ${escapeTableText(gap.scenarioId)} | ${escapeTableText(
          gap.resolvedPriority
        )} | ${escapeTableText(gap.missingFields.join(', '))} |`
      );
    });
  }

  if (matrix.policyEvaluation) {
    lines.push(
      '',
      '## Policy Evaluation',
      '',
      `- Policy: \`${matrix.policyEvaluation.policyName}\``,
      `- Version: \`${matrix.policyEvaluation.policyVersion}\``,
      `- Status: \`${matrix.policyEvaluation.status}\``,
      '',
      '| Rule | Severity | Status | Message |',
      '|---|---|---|---|'
    );
    matrix.policyEvaluation.results.forEach((result) => {
      lines.push(
        `| ${escapeTableText(result.ruleId)} | ${escapeTableText(
          result.severity
        )} | ${escapeTableText(result.status)} | ${escapeTableText(
          result.message
        )} |`
      );
      if (result.details.length > 0) {
        lines.push(
          `| ${escapeTableText(`${result.ruleId} details`)} | ${escapeTableText(
            result.severity
          )} | ${escapeTableText(result.status)} | ${escapeTableText(
            JSON.stringify(result.details)
          )} |`
        );
      }
    });
  }

  return `${lines.join('\n')}\n`;
}
