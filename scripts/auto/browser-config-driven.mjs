import path from 'node:path';
import { access, copyFile, mkdir, readFile } from 'node:fs/promises';

import { comparePngFiles } from './png-visual-regression.mjs';
import {
  buildVisualDiffIndexHtml,
  buildVisualFailureDetailHtml,
  buildVisualManifest,
  collectVisualAssertionRecords,
  resolveArtifactPrefix,
  summarizeVisualAssertionRecords
} from './visual-regression-report.mjs';

function readPathValue(source, pathExpression) {
  return String(pathExpression || '')
    .split('.')
    .filter(Boolean)
    .reduce((current, key) => (current == null ? undefined : current[key]), source);
}

function interpolateTemplate(value, context) {
  if (typeof value !== 'string') {
    return value;
  }

  return value.replace(/\$\{([^}]+)\}/g, (_, expression) => {
    const result = readPathValue(context, expression.trim());
    return result == null ? '' : String(result);
  });
}

async function pathExists(targetPath) {
  try {
    await access(targetPath);
    return true;
  } catch {
    return false;
  }
}

function toWorkspaceRelative(workspaceRoot, targetPath) {
  return path.relative(workspaceRoot, targetPath).replace(/\\/g, '/');
}

function slugifySegment(value, fallback = 'artifact') {
  const normalized = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\.png$/i, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return normalized || fallback;
}

function normalizeThreshold(value) {
  const numeric = Number(value ?? 0);
  if (!Number.isFinite(numeric) || numeric < 0) {
    return 0;
  }
  return numeric;
}

function resolveScreenshotTarget(step) {
  if (step.screenshotTarget === 'page') {
    return 'page';
  }
  if (step.screenshotTarget === 'locator') {
    return 'locator';
  }
  return step.locator ? 'locator' : 'page';
}

function resolvePlanSlug(plan) {
  const planFilePath = plan?.__meta?.absolutePath;
  if (planFilePath) {
    return slugifySegment(path.basename(planFilePath, path.extname(planFilePath)), 'automation-plan');
  }
  return slugifySegment(plan?.target?.planName, 'automation-plan');
}

function resolveBaselineName(step, context) {
  const rawValue =
    interpolateTemplate(step.baselineName || '', context) ||
    interpolateTemplate(step.id || step.label || '', context);
  return `${slugifySegment(rawValue, 'step')}.png`;
}

function resolveBaselinePath({ plan, scenario, step, context, workspaceRoot }) {
  const baselineRoot =
    interpolateTemplate(plan?.target?.baselineDir || 'config/automation/baselines', context) ||
    'config/automation/baselines';
  const absoluteBaselineRoot = path.isAbsolute(baselineRoot)
    ? baselineRoot
    : path.resolve(workspaceRoot, baselineRoot);

  return path.join(
    absoluteBaselineRoot,
    resolvePlanSlug(plan),
    slugifySegment(scenario.key || scenario.name, 'scenario'),
    resolveBaselineName(step, context)
  );
}

function resolveScreenshotArtifacts({ scenario, step, context, options }) {
  const scenarioArtifactDir = path.join(
    options.artifacts.absolute.screenshotsDir,
    slugifySegment(scenario.key || scenario.name, 'scenario')
  );
  const fileName = slugifySegment(
    interpolateTemplate(step.baselineName || '', context) || step.id || step.label || 'step',
    'step'
  );

  return {
    scenarioArtifactDir,
    actualAbsolutePath: path.join(scenarioArtifactDir, `${fileName}-actual.png`),
    diffAbsolutePath: path.join(scenarioArtifactDir, `${fileName}-diff.png`)
  };
}

async function captureVisualTarget({ page, locator, screenshotTarget, actualAbsolutePath, fullPage }) {
  if (screenshotTarget === 'locator') {
    await locator.screenshot({
      path: actualAbsolutePath
    });
    return;
  }

  await page.screenshot({
    path: actualAbsolutePath,
    fullPage
  });
}

function buildTemplateContext(plan, runToken, variables, scenario) {
  return {
    runToken,
    variables,
    target: plan.target,
    scenario,
    plan
  };
}

function resolveRoot(page, locator, context) {
  if (!locator?.container) {
    return page;
  }
  return page.locator(interpolateTemplate(locator.container, context)).first();
}

function resolveLocator(page, locator, context) {
  if (!locator?.type) {
    throw new Error('Locator type is required.');
  }

  const root = resolveRoot(page, locator, context);
  const value = interpolateTemplate(locator.value || '', context);
  const exact = Boolean(locator.exact);

  switch (locator.type) {
    case 'css':
      return root.locator(value).first();
    case 'placeholder':
      return root.getByPlaceholder(value, { exact }).first();
    case 'role': {
      const role = interpolateTemplate(locator.role || 'button', context);
      const name = interpolateTemplate(locator.name || locator.value || '', context);
      return root.getByRole(role, name ? { name, exact } : undefined).first();
    }
    case 'text':
      return root.getByText(value, { exact }).first();
    case 'label':
      return root.getByLabel(value, { exact }).first();
    case 'testId':
      return root.getByTestId(value).first();
    default:
      throw new Error(`Unsupported locator type: ${locator.type}`);
  }
}

async function performNestedAction(page, action, context) {
  if (!action?.type || !action.locator) {
    throw new Error('Nested action requires type and locator.');
  }

  const target = resolveLocator(page, action.locator, context);
  if (action.type === 'press') {
    await target.press(interpolateTemplate(action.value || 'Enter', context));
    return;
  }

  await target.click();
}

function normalizePlan(plan) {
  return {
    ...plan,
    scenarios: Array.isArray(plan?.scenarios) ? plan.scenarios : [],
    target: {
      planName: '',
      frontendBaseUrl: '',
      backendBaseUrl: '',
      loginRoute: '/login',
      username: '',
      password: '',
      browserPath: '',
      headless: true,
      issueDocPath: '',
      outputPrefix: 'config-browser',
      baselineDir: 'config/automation/baselines',
      scenarioScopes: ['delivery', 'baseline'],
      failScopes: ['delivery'],
      ...(plan?.target || {})
    }
  };
}

export async function loadAutomationPlan(workspaceRoot, planPath) {
  const absolutePath = path.isAbsolute(planPath) ? planPath : path.resolve(workspaceRoot, planPath);
  const raw = await readFile(absolutePath, 'utf8');
  const plan = normalizePlan(JSON.parse(raw));
  Object.defineProperty(plan, '__meta', {
    value: {
      absolutePath
    },
    enumerable: false,
    configurable: true
  });
  return {
    absolutePath,
    plan
  };
}

function parseBooleanStepValue(value) {
  if (typeof value === 'boolean') {
    return value;
  }

  const normalized = String(value || '')
    .trim()
    .toLowerCase();
  if (['true', '1', 'yes', 'checked', 'on'].includes(normalized)) {
    return true;
  }
  if (['false', '0', 'no', 'unchecked', 'off'].includes(normalized)) {
    return false;
  }
  return undefined;
}

function resolveFilePaths(workspaceRoot, rawValue, context) {
  const interpolated = interpolateTemplate(rawValue || '', context);
  if (!String(interpolated || '').trim()) {
    throw new Error('filePath is required.');
  }

  let values;
  if (String(interpolated).trim().startsWith('[')) {
    values = JSON.parse(String(interpolated));
  } else if (String(interpolated).includes('\n')) {
    values = String(interpolated)
      .split(/\r?\n/)
      .map((item) => item.trim())
      .filter(Boolean);
  } else {
    values = [String(interpolated).trim()];
  }

  return values.map((item) => (path.isAbsolute(item) ? item : path.resolve(workspaceRoot, item)));
}

async function getLocatorCheckedState(locator) {
  try {
    return await locator.isChecked();
  } catch {
    // ignore and fallback to attributes
  }

  const ariaChecked = await locator.getAttribute('aria-checked');
  if (ariaChecked === 'true') {
    return true;
  }
  if (ariaChecked === 'false') {
    return false;
  }

  const className = await locator.getAttribute('class');
  if (className) {
    if (/\bis-checked\b|\bchecked\b|\bactive\b/.test(className)) {
      return true;
    }
    if (/\bis-unchecked\b/.test(className)) {
      return false;
    }
  }

  return undefined;
}

async function getLocatorDisabledState(locator) {
  try {
    return await locator.isDisabled();
  } catch {
    // ignore and fallback to attributes
  }

  const disabled = await locator.getAttribute('disabled');
  if (disabled !== null) {
    return true;
  }

  const ariaDisabled = await locator.getAttribute('aria-disabled');
  if (ariaDisabled === 'true') {
    return true;
  }
  if (ariaDisabled === 'false') {
    return false;
  }

  return undefined;
}

async function clickFirstMatchingButton(root, labels) {
  for (const label of labels) {
    const exactButton = root.getByRole('button', { name: label, exact: true }).first();
    if ((await exactButton.count()) > 0) {
      await exactButton.click();
      return label;
    }

    const partialButton = root.locator('button, [role="button"]').filter({ hasText: label }).first();
    if ((await partialButton.count()) > 0) {
      await partialButton.click();
      return label;
    }
  }

  throw new Error(`Dialog button could not be resolved from labels: ${labels.join(', ')}.`);
}

async function resolveDialog(page, step, context) {
  const dialogTitle = interpolateTemplate(step.dialogTitle || '', context);
  const timeout = step.timeout || 10000;
  let dialog = dialogTitle
    ? page.getByRole('dialog', { name: dialogTitle }).last()
    : page.getByRole('dialog').last();

  if ((await dialog.count()) === 0) {
    dialog = dialogTitle
      ? page.locator('.el-dialog').filter({ hasText: dialogTitle }).last()
      : page.locator('.el-dialog').last();
  }

  await dialog.waitFor({
    state: 'visible',
    timeout
  });

  return {
    dialog,
    dialogTitle
  };
}

async function executeMatchedAction({ matcher, action, page, helpers, context, label, timeout }) {
  const resolvedMatcher = interpolateTemplate(matcher || '', context).trim();
  if (!resolvedMatcher) {
    await action();
    return {
      matcher: '',
      response: undefined
    };
  }

  const response = await helpers.expectApiResponse(page, resolvedMatcher, action, label, timeout);
  return {
    matcher: resolvedMatcher,
    response
  };
}

function applyStepCaptures(captures, result, context) {
  const capturedVariables = [];
  for (const capture of captures || []) {
    if (!result) {
      throw new Error(`Capture "${capture.variable}" requires a matched API response.`);
    }

    const captureValue = readPathValue(result, capture.path);
    if (captureValue === undefined || captureValue === null || captureValue === '') {
      throw new Error(`Capture "${capture.variable}" could not be resolved from "${capture.path}".`);
    }
    context.variables[capture.variable] = String(captureValue);
    capturedVariables.push({
      variable: capture.variable,
      path: capture.path,
      value: String(captureValue)
    });
  }
  return capturedVariables;
}

function createStepExecutionUtils({ page, helpers, context, options }) {
  return {
    readPathValue,
    interpolateTemplate: (value) => interpolateTemplate(value, context),
    resolveLocator: (root, locator) => resolveLocator(root, locator, context),
    performNestedAction: (root, action) => performNestedAction(root, action, context),
    executeMatchedAction: ({ matcher, action, label, timeout }) =>
      executeMatchedAction({
        matcher,
        action,
        page,
        helpers,
        context,
        label,
        timeout
    }),
    applyStepCaptures: (captures, result) => applyStepCaptures(captures, result, context),
    resolveFilePaths: (rawValue) => resolveFilePaths(options.workspaceRoot || process.cwd(), rawValue, context),
    getVariable: (name) => context.variables[String(name || '').trim()],
    getLocatorCheckedState,
    getLocatorDisabledState,
    resolveDialog: (step) => resolveDialog(page, step, context)
  };
}

const planStepHandlers = new Map();
let builtinPlanStepHandlersRegistered = false;

export function registerPlanStepHandler(type, handler) {
  if (!String(type || '').trim()) {
    throw new Error('Step handler type is required.');
  }
  if (typeof handler !== 'function') {
    throw new Error(`Step handler for "${type}" must be a function.`);
  }
  planStepHandlers.set(String(type).trim(), handler);
}

function registerBuiltinPlanStepHandlers() {
  if (builtinPlanStepHandlersRegistered) {
    return;
  }
  builtinPlanStepHandlersRegistered = true;

  registerPlanStepHandler('fill', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    await locator.fill(utils.interpolateTemplate(step.value || ''));
    return {};
  });

  registerPlanStepHandler('click', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    await locator.click();
    return {};
  });

  registerPlanStepHandler('press', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    await locator.press(utils.interpolateTemplate(step.value || 'Enter'));
    return {};
  });

  registerPlanStepHandler('setChecked', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const desired = parseBooleanStepValue(step.checked ?? utils.interpolateTemplate(step.value ?? ''));

    if (desired === undefined) {
      throw new Error('checked or value(true/false) is required.');
    }

    const current = await utils.getLocatorCheckedState(locator);
    if (current !== desired) {
      try {
        if (desired) {
          await locator.check({ force: true });
        } else {
          await locator.uncheck({ force: true });
        }
      } catch {
        await locator.click();
      }
    }

    const finalState = await utils.getLocatorCheckedState(locator);
    if (finalState !== undefined && finalState !== desired) {
      throw new Error(`Expected checked state ${desired}, got ${finalState}.`);
    }

    return {
      checked: desired
    };
  });

  registerPlanStepHandler('selectOption', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const optionText = utils.interpolateTemplate(step.optionText || '');
    const timeout = step.timeout || 30000;
    const selectContainer = locator.locator('xpath=ancestor::*[contains(@class, "el-select")][1]').first();
    const selectTrigger = (await selectContainer.count()) > 0
      ? selectContainer.locator('.el-select__wrapper, .el-select__selection, .el-input__wrapper').first()
      : locator.locator('.el-select__wrapper, .el-select__selection, .el-input__wrapper').first();

    if ((await selectTrigger.count()) > 0) {
      await selectTrigger.click({ timeout });
    } else {
      try {
        await locator.click({ timeout });
      } catch {
        await locator.focus();
        await locator.press('ArrowDown');
      }
    }

    const dropdownOption = page.locator('.el-select-dropdown__item', { hasText: optionText }).first();
    const roleOption = page.getByRole('option', { name: optionText, exact: true }).first();
    const targetOption = (await dropdownOption.count()) > 0 ? dropdownOption : roleOption;
    await targetOption.waitFor({
      state: 'visible',
      timeout
    });
    await targetOption.click({ timeout });
    return {
      optionText
    };
  });

  registerPlanStepHandler('uploadFile', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const files = utils.resolveFilePaths(step.filePath || step.value || '');
    await locator.setInputFiles(files);
    return {
      files
    };
  });

  registerPlanStepHandler('waitVisible', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    await locator.waitFor({
      state: 'visible',
      timeout: step.timeout || 10000
    });
    return {};
  });

  registerPlanStepHandler('assertText', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const actual = await locator.textContent();
    const expected = utils.interpolateTemplate(step.value || '');
    if (!String(actual || '').includes(expected)) {
      throw new Error(`Expected text to include "${expected}", got "${String(actual || '').trim()}".`);
    }
    return {
      expected
    };
  });

  registerPlanStepHandler('assertValue', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const expected = utils.interpolateTemplate(step.value || '');

    let actual = '';
    try {
      actual = await locator.inputValue();
    } catch {
      actual = (await locator.getAttribute('value')) || '';
    }

    if (actual !== expected) {
      throw new Error(`Expected value "${expected}", got "${actual}".`);
    }

    return {
      expected,
      actual
    };
  });

  registerPlanStepHandler('assertDisabled', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const disabled = await utils.getLocatorDisabledState(locator);

    if (disabled !== true) {
      throw new Error(`Expected locator to be disabled, got ${disabled}.`);
    }

    return {
      disabled
    };
  });

  registerPlanStepHandler('assertUrlIncludes', async ({ step, page, utils }) => {
    const expected = utils.interpolateTemplate(step.value || '');
    const timeout = Number(step.timeout || 0);
    if (!page.url().includes(expected) && timeout > 0) {
      await page.waitForURL((url) => url.toString().includes(expected), {
        timeout
      });
    }
    if (!page.url().includes(expected)) {
      throw new Error(`Expected URL to include "${expected}", got "${page.url()}".`);
    }
    return {
      expected,
      actual: page.url()
    };
  });

  registerPlanStepHandler('assertPathnameEquals', async ({ step, page, utils }) => {
    const expected = utils.interpolateTemplate(step.value || '');
    const readPathname = () => new URL(page.url()).pathname;
    const timeout = Number(step.timeout || 0);
    if (readPathname() !== expected && timeout > 0) {
      await page.waitForURL((url) => new URL(url.toString()).pathname === expected, {
        timeout
      });
    }
    const actual = readPathname();
    if (actual !== expected) {
      throw new Error(`Expected pathname "${expected}", got "${actual}".`);
    }
    return {
      expected,
      actual
    };
  });

  registerPlanStepHandler('assertVariableEquals', async ({ step, utils }) => {
    const variableName = String(step.variable || '').trim();
    if (!variableName) {
      throw new Error('variable is required.');
    }

    const actual = utils.getVariable(variableName);
    const expected = utils.interpolateTemplate(step.value || '');
    if (String(actual ?? '') !== expected) {
      throw new Error(`Expected variable "${variableName}" to equal "${expected}", got "${String(actual ?? '')}".`);
    }

    return {
      variable: variableName,
      expected
    };
  });

  registerPlanStepHandler('assertScreenshot', async ({ step, page, utils, context, options, scenario, plan }) => {
    const workspaceRoot = options.workspaceRoot || process.cwd();
    const screenshotTarget = resolveScreenshotTarget(step);
    const fullPage = screenshotTarget === 'page' ? step.fullPage !== false : false;
    const threshold = normalizeThreshold(step.threshold);
    const baselineAbsolutePath = resolveBaselinePath({
      plan,
      scenario,
      step,
      context,
      workspaceRoot
    });
    const { scenarioArtifactDir, actualAbsolutePath, diffAbsolutePath } = resolveScreenshotArtifacts({
      scenario,
      step,
      context,
      options
    });

    await mkdir(scenarioArtifactDir, { recursive: true });
    const locator = screenshotTarget === 'locator' ? utils.resolveLocator(page, step.locator) : null;
    await captureVisualTarget({
      page,
      locator,
      screenshotTarget,
      actualAbsolutePath,
      fullPage
    });

    const baselinePath = toWorkspaceRelative(workspaceRoot, baselineAbsolutePath);
    const actualPath = toWorkspaceRelative(workspaceRoot, actualAbsolutePath);
    const diffPath = toWorkspaceRelative(workspaceRoot, diffAbsolutePath);
    const baselineExists = await pathExists(baselineAbsolutePath);

    await mkdir(path.dirname(baselineAbsolutePath), { recursive: true });

    if (options.updateBaseline) {
      await copyFile(actualAbsolutePath, baselineAbsolutePath);
      return {
        screenshotTarget,
        fullPage,
        threshold,
        baselinePath,
        actualPath,
        baselineUpdated: true,
        baselineStatus: baselineExists ? 'updated' : 'created'
      };
    }

    if (!baselineExists) {
      const error = new Error(
        `Screenshot baseline is missing: ${baselinePath}. Re-run with --update-baseline to create it.`
      );
      error.details = {
        screenshotTarget,
        fullPage,
        threshold,
        baselinePath,
        actualPath,
        baselineStatus: 'missing'
      };
      throw error;
    }

    const comparison = await comparePngFiles({
      baselinePath: baselineAbsolutePath,
      actualPath: actualAbsolutePath,
      diffPath: diffAbsolutePath,
      maxDiffRatio: threshold
    });

    const visualResult = {
      screenshotTarget,
      fullPage,
      threshold,
      baselinePath,
      actualPath,
      diffPath:
        comparison.mismatchPixels > 0 && comparison.reason !== 'dimension_mismatch' ? diffPath : undefined,
      baselineUpdated: false,
      baselineStatus:
        comparison.reason === 'matched'
          ? 'matched'
          : comparison.pass
            ? 'within-threshold'
            : 'mismatch',
      mismatchPixels: comparison.mismatchPixels,
      totalPixels: comparison.totalPixels,
      mismatchRatio: comparison.mismatchRatio,
      baselineWidth: comparison.baselineWidth,
      baselineHeight: comparison.baselineHeight,
      actualWidth: comparison.actualWidth,
      actualHeight: comparison.actualHeight,
      visualReason: comparison.reason
    };

    if (!comparison.pass) {
      const percent = (comparison.mismatchRatio * 100).toFixed(4);
      const error = new Error(
        comparison.reason === 'dimension_mismatch'
          ? `Screenshot dimension mismatch: baseline ${comparison.baselineWidth}x${comparison.baselineHeight}, actual ${comparison.actualWidth}x${comparison.actualHeight}.`
          : `Screenshot mismatch ratio ${percent}% exceeded threshold ${(threshold * 100).toFixed(4)}%.`
      );
      error.details = visualResult;
      throw error;
    }

    return visualResult;
  });

  registerPlanStepHandler('sleep', async ({ step, page }) => {
    const timeout = Number(step.timeout || step.value || 500);
    await page.waitForTimeout(timeout);
    return {
      timeout
    };
  });

  registerPlanStepHandler('triggerApi', async ({ step, page, utils }) => {
    if (!step.action) {
      throw new Error('triggerApi requires action.');
    }

    const result = await utils.executeMatchedAction({
      matcher: step.matcher,
      action: async () => utils.performNestedAction(page, step.action),
      label: step.label,
      timeout: step.timeout
    });
    const captures = utils.applyStepCaptures(step.captures || [], result.response);

    return {
      matcher: result.matcher,
      captures
    };
  });

  registerPlanStepHandler('tableRowAction', async ({ step, page, utils }) => {
    const rowText = utils.interpolateTemplate(step.rowText || step.value || '');
    if (!rowText) {
      throw new Error('rowText or value is required.');
    }

    const tableRoot = step.locator
      ? utils.resolveLocator(page, step.locator)
      : page.locator('.el-table, table').first();
    await tableRoot.waitFor({
      state: 'visible',
      timeout: step.timeout || 10000
    });

    const row = tableRoot.locator('tr').filter({ hasText: rowText }).first();
    await row.waitFor({
      state: 'visible',
      timeout: step.timeout || 10000
    });

    if (!step.action) {
      return {
        rowText
      };
    }

    const result = await utils.executeMatchedAction({
      matcher: step.matcher,
      action: async () => utils.performNestedAction(row, step.action),
      label: step.label,
      timeout: step.timeout
    });
    const captures = utils.applyStepCaptures(step.captures || [], result.response);

    return {
      rowText,
      matcher: result.matcher,
      captures
    };
  });

  registerPlanStepHandler('dialogAction', async ({ step, page, utils }) => {
    const { dialog, dialogTitle } = await utils.resolveDialog(step);
    const dialogAction =
      step.dialogAction || (step.action ? 'custom' : step.matcher ? 'confirm' : 'waitVisible');

    if (dialogAction === 'waitVisible') {
      return {
        dialogTitle
      };
    }

    const performDialogAction = async () => {
      if (dialogAction === 'custom') {
        if (!step.action) {
          throw new Error('dialogAction=custom requires action.');
        }
        await utils.performNestedAction(dialog, step.action);
        return 'custom';
      }

      if (dialogAction === 'close') {
        const closeButton = dialog.locator('.el-dialog__headerbtn, [aria-label="Close"]').first();
        if ((await closeButton.count()) > 0) {
          await closeButton.click();
          return 'close';
        }
        await page.keyboard.press('Escape');
        return 'escape';
      }

      const labels =
        dialogAction === 'cancel'
          ? [step.actionText || '取消', '关闭']
          : [step.actionText || '确定', '保存', '提交', '确认'];
      return clickFirstMatchingButton(dialog, labels);
    };

    const result = await utils.executeMatchedAction({
      matcher: step.matcher,
      action: performDialogAction,
      label: step.label,
      timeout: step.timeout
    });
    const captures = utils.applyStepCaptures(step.captures || [], result.response);

    return {
      dialogTitle,
      dialogAction,
      matcher: result.matcher,
      captures
    };
  });
}

async function executePlanStep({ plan, scenario, step, page, helpers, context, options, stepResults }) {
  registerBuiltinPlanStepHandlers();
  const startedAt = new Date().toISOString();

  const finish = (status, extra = {}) => {
    const record = {
      stepId: step.id,
      label: step.label,
      type: step.type,
      status,
      startedAt,
      finishedAt: new Date().toISOString(),
      ...extra
    };
    stepResults.push(record);
    return record;
  };

  try {
    const handler = planStepHandlers.get(String(step.type || '').trim());
    if (!handler) {
      throw new Error(`Unsupported step type: ${step.type}`);
    }

    const utils = createStepExecutionUtils({
      page,
      helpers,
      context,
      options
    });
    const extra = await handler({
      step,
      page,
      helpers,
      plan,
      scenario,
      context,
      options,
      utils
    });

    finish('passed', extra || {});
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    const errorDetails =
      error && typeof error === 'object' && error.details && typeof error.details === 'object'
        ? error.details
        : {};
    if (step.optional) {
      finish('optional_failed', {
        error: message,
        ...errorDetails
      });
      return;
    }

    const failedRecord = finish('failed', {
      error: message,
      ...errorDetails
    });
    const failure = new Error(`${step.label}: ${message}`);
    failure.details = {
      ...errorDetails,
      stepResult: failedRecord,
      stepResults: [...stepResults]
    };
    throw failure;
  }
}

export function createConfigDrivenScenarios(plan) {
  return ({ runToken }) =>
    plan.scenarios.map((scenario) => ({
      key: scenario.key,
      name: scenario.name,
      route: scenario.route,
      scope: scenario.scope || 'delivery',
      description: scenario.description,
      requiresLogin: scenario.requiresLogin ?? false,
      run: async ({ page, runtime, helpers, options }) => {
        if (!runtime.planVariables) {
          runtime.planVariables = {};
        }

        const templateContext = buildTemplateContext(plan, runToken, runtime.planVariables, scenario);
        const route = interpolateTemplate(scenario.route, templateContext);
        const expectedPath = interpolateTemplate(
          scenario.expectedPath || String(route).split('?')[0],
          templateContext
        );
        const readySelector = interpolateTemplate(scenario.readySelector || '', templateContext);
        const initialApis = (scenario.initialApis || []).map((item) => ({
          matcher: interpolateTemplate(item.matcher, templateContext),
          label: item.label,
          optional: item.optional,
          timeout: item.timeout
        }));

        let routeApiResults = [];
        const stepResults = [];
        try {
          routeApiResults = await helpers.openRoute(page, {
            path: route,
            expectedPath,
            readySelector: readySelector || undefined,
            api: initialApis
          });

          for (const step of scenario.steps || []) {
            await executePlanStep({
              plan,
              scenario,
              step,
              page,
              helpers,
              context: buildTemplateContext(plan, runToken, runtime.planVariables, scenario),
              options,
              stepResults
            });
          }
        } catch (error) {
          if (error && typeof error === 'object') {
            const existingDetails =
              error.details && typeof error.details === 'object' ? error.details : {};
            error.details = {
              ...existingDetails,
              businessFlow: scenario.businessFlow,
              featurePoints: scenario.featurePoints || [],
              initialApis,
              routeApiResults,
              stepResults,
              variablesSnapshot: { ...runtime.planVariables }
            };
          }
          throw error;
        }

        return {
          businessFlow: scenario.businessFlow,
          featurePoints: scenario.featurePoints || [],
          initialApis,
          routeApiResults,
          stepResults,
          variablesSnapshot: { ...runtime.planVariables }
        };
      }
    }));
}

function extractScenarioStepResults(scenarioResult) {
  if (Array.isArray(scenarioResult?.detail?.stepResults)) {
    return scenarioResult.detail.stepResults;
  }
  return [];
}

export function buildPlanExecutionEnhancement(planInfo) {
  const { plan, absolutePath } = planInfo;

  return ({ scenarioResults, options, artifacts }) => {
    const workspaceRoot = options?.workspaceRoot || process.cwd();
    const relativePlanPath = path.relative(workspaceRoot, absolutePath).replace(/\\/g, '/');
    const recommendations = [];
    const failedResults = scenarioResults.filter((item) => item.status === 'failed');
    const visualResults = collectVisualAssertionRecords(scenarioResults);
    const visualSummary = summarizeVisualAssertionRecords(visualResults);
    const visualFailures = visualResults.filter((step) => ['mismatch', 'missing'].includes(step.category));
    const missingAssertionScenarios = (plan.scenarios || []).filter(
      (scenario) =>
        !(scenario.steps || []).some((step) =>
          ['assertText', 'assertUrlIncludes', 'assertScreenshot'].includes(step.type)
        )
    );
    const baselineScenariosWithoutVisualAssertion = (plan.scenarios || []).filter(
      (scenario) =>
        scenario.scope === 'baseline' &&
        !(scenario.steps || []).some((step) => step.type === 'assertScreenshot')
    );

    if (failedResults.some((item) => /401|login|redirect/i.test(item.error || ''))) {
      recommendations.push('鉴权建议：检查登录步骤、token 注入链路与会话是否在页面跳转后丢失。');
    }

    if (failedResults.some((item) => /locator|wait failed|timeout|visible/i.test(item.error || ''))) {
      recommendations.push('选择器建议：优先使用稳定的 `id`、`data-testid` 或角色定位，减少文案漂移带来的波动。');
    }

    if (missingAssertionScenarios.length > 0) {
      recommendations.push(
        `断言建议：为以下场景补充页面断言，提升结果可信度：${missingAssertionScenarios
          .map((item) => item.name)
          .join('、')}。`
      );
    }

    if (baselineScenariosWithoutVisualAssertion.length > 0) {
      recommendations.push(
        `视觉回归建议：为以下 baseline 场景补充 assertScreenshot，加强页面结构与样式回归监控：${baselineScenariosWithoutVisualAssertion
          .map((item) => item.name)
          .join('、')}。`
      );
    }

    if (visualSummary.missing > 0) {
      recommendations.push('基线维护建议：缺少截图基线时可先使用 `--update-baseline` 创建或刷新基线图。');
    }

    if (failedResults.length === 0) {
      recommendations.push('扩面建议：当前计划已跑通，可继续补充详情抽屉、导出、批量操作与异常路径场景。');
    }

    const prefix = resolveArtifactPrefix(artifacts);
    const visualManifestAbsolutePath = path.join(
      artifacts.absolute.logsRoot,
      `${prefix}-visual-manifest-${artifacts.runTimestamp}.json`
    );
    const visualIndexAbsolutePath = path.join(
      artifacts.absolute.logsRoot,
      `${prefix}-visual-index-${artifacts.runTimestamp}.html`
    );
    const visualFailuresAbsolutePath = path.join(
      artifacts.absolute.logsRoot,
      `${prefix}-visual-failures-${artifacts.runTimestamp}.html`
    );
    const visualManifestRelativePath = path.relative(workspaceRoot, visualManifestAbsolutePath).replace(/\\/g, '/');
    const visualIndexRelativePath = path.relative(workspaceRoot, visualIndexAbsolutePath).replace(/\\/g, '/');
    const visualFailuresRelativePath = path
      .relative(workspaceRoot, visualFailuresAbsolutePath)
      .replace(/\\/g, '/');

    const visualFailureSections =
      visualFailures.length > 0
        ? [
            '',
            '## Visual Diff Findings',
            '',
            ...visualFailures.slice(0, 10).map(
              (item) =>
                `- ${item.label}: baseline \`${item.baselinePath || 'n/a'}\`, actual \`${item.actualPath || 'n/a'}\`, diff \`${item.diffPath || 'n/a'}\`, ratio \`${typeof item.mismatchRatio === 'number' ? item.mismatchRatio.toFixed(6) : 'n/a'}\``
            )
          ]
        : [];

    return {
      summaryExtras: {
        planPath: relativePlanPath,
        visualSummary,
        recommendations
      },
      detailExtras: {
        planPath: absolutePath,
        planName: plan.target?.planName || '',
        visualSummary,
        visualResults,
        visualFailures,
        recommendations
      },
      outputFiles:
        visualResults.length > 0
          ? [
              {
                key: 'visualManifestPath',
                absolutePath: visualManifestAbsolutePath,
                content: JSON.stringify(
                  buildVisualManifest({
                    sourcePath: relativePlanPath,
                    records: visualResults
                  }),
                  null,
                  2
                )
              },
              {
                key: 'visualIndexPath',
                absolutePath: visualIndexAbsolutePath,
                content: buildVisualDiffIndexHtml({
                  outputPath: visualIndexAbsolutePath,
                  workspaceRoot,
                  records: visualResults,
                  sourcePath: relativePlanPath,
                  failureDetailPath: visualFailuresAbsolutePath
                })
              },
              {
                key: 'visualFailureDetailPath',
                absolutePath: visualFailuresAbsolutePath,
                content: buildVisualFailureDetailHtml({
                  outputPath: visualFailuresAbsolutePath,
                  workspaceRoot,
                  records: visualResults,
                  sourcePath: relativePlanPath,
                  indexPath: visualIndexAbsolutePath
                })
              }
            ]
          : [],
      reportSections: [
        '## Plan',
        '',
        `- Plan file: \`${relativePlanPath}\``,
        `- Plan name: \`${plan.target?.planName || 'unnamed-plan'}\``,
        `- Scenario count: \`${(plan.scenarios || []).length}\``,
        `- Visual assertions: \`${visualSummary.total}\``,
        '',
        '## Visual Regression',
        '',
        `- Passed visual checks: \`${visualSummary.passed}\``,
        `- Failed visual checks: \`${visualSummary.mismatch}\``,
        `- Updated baselines: \`${visualSummary.updated}\``,
        `- Missing baselines: \`${visualSummary.missing}\``,
        visualResults.length > 0 ? `- Visual manifest: \`${visualManifestRelativePath}\`` : '- Visual manifest: \`n/a\`',
        visualResults.length > 0 ? `- Visual diff index: \`${visualIndexRelativePath}\`` : '- Visual diff index: \`n/a\`',
        visualResults.length > 0
          ? `- Visual failure detail: \`${visualFailuresRelativePath}\``
          : '- Visual failure detail: \`n/a\`',
        '',
        '## Suggestions',
        '',
        ...(recommendations.length > 0 ? recommendations.map((item) => `- ${item}`) : ['- 当前暂无新增建议。']),
        ...visualFailureSections
      ]
    };
  };
}
