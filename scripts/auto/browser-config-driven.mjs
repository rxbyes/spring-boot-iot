import path from 'node:path';
import { readFile } from 'node:fs/promises';

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

function buildTemplateContext(plan, runToken, variables, scenario) {
  return {
    runToken,
    variables,
    target: plan.target,
    scenario
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
      scenarioScopes: ['delivery', 'baseline'],
      failScopes: ['delivery'],
      ...(plan?.target || {})
    }
  };
}

export async function loadAutomationPlan(workspaceRoot, planPath) {
  const absolutePath = path.isAbsolute(planPath) ? planPath : path.resolve(workspaceRoot, planPath);
  const raw = await readFile(absolutePath, 'utf8');
  return {
    absolutePath,
    plan: normalizePlan(JSON.parse(raw))
  };
}

async function executePlanStep({ step, page, helpers, context, stepResults }) {
  const startedAt = new Date().toISOString();

  const finish = (status, extra = {}) => {
    stepResults.push({
      label: step.label,
      type: step.type,
      status,
      startedAt,
      finishedAt: new Date().toISOString(),
      ...extra
    });
  };

  try {
    switch (step.type) {
      case 'fill': {
        const locator = resolveLocator(page, step.locator, context);
        await locator.fill(interpolateTemplate(step.value || '', context));
        finish('passed');
        return;
      }
      case 'click': {
        const locator = resolveLocator(page, step.locator, context);
        await locator.click();
        finish('passed');
        return;
      }
      case 'press': {
        const locator = resolveLocator(page, step.locator, context);
        await locator.press(interpolateTemplate(step.value || 'Enter', context));
        finish('passed');
        return;
      }
      case 'selectOption': {
        const locator = resolveLocator(page, step.locator, context);
        await locator.click();
        const optionText = interpolateTemplate(step.optionText || '', context);
        await page.locator('.el-select-dropdown__item', { hasText: optionText }).first().click();
        finish('passed', {
          optionText
        });
        return;
      }
      case 'waitVisible': {
        const locator = resolveLocator(page, step.locator, context);
        await locator.waitFor({
          state: 'visible',
          timeout: step.timeout || 10000
        });
        finish('passed');
        return;
      }
      case 'assertText': {
        const locator = resolveLocator(page, step.locator, context);
        const actual = await locator.textContent();
        const expected = interpolateTemplate(step.value || '', context);
        if (!String(actual || '').includes(expected)) {
          throw new Error(`Expected text to include "${expected}", got "${String(actual || '').trim()}".`);
        }
        finish('passed', {
          expected
        });
        return;
      }
      case 'assertUrlIncludes': {
        const expected = interpolateTemplate(step.value || '', context);
        if (!page.url().includes(expected)) {
          throw new Error(`Expected URL to include "${expected}", got "${page.url()}".`);
        }
        finish('passed', {
          expected
        });
        return;
      }
      case 'sleep': {
        const timeout = Number(step.timeout || step.value || 500);
        await page.waitForTimeout(timeout);
        finish('passed', {
          timeout
        });
        return;
      }
      case 'triggerApi': {
        const matcher = interpolateTemplate(step.matcher || '', context);
        const result = await helpers.expectApiResponse(
          page,
          matcher,
          async () => performNestedAction(page, step.action, context),
          step.label,
          step.timeout
        );

        for (const capture of step.captures || []) {
          const captureValue = readPathValue(result, capture.path);
          if (captureValue === undefined || captureValue === null || captureValue === '') {
            throw new Error(`Capture "${capture.variable}" could not be resolved from "${capture.path}".`);
          }
          context.variables[capture.variable] = String(captureValue);
        }

        finish('passed', {
          matcher,
          captures: step.captures || []
        });
        return;
      }
      default:
        throw new Error(`Unsupported step type: ${step.type}`);
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    if (step.optional) {
      finish('optional_failed', {
        error: message
      });
      return;
    }
    throw new Error(`${step.label}: ${message}`);
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
      run: async ({ page, runtime, helpers }) => {
        if (!runtime.planVariables) {
          runtime.planVariables = {};
        }

        const route = interpolateTemplate(scenario.route, buildTemplateContext(plan, runToken, runtime.planVariables, scenario));
        const expectedPath = interpolateTemplate(
          scenario.expectedPath || String(route).split('?')[0],
          buildTemplateContext(plan, runToken, runtime.planVariables, scenario)
        );
        const readySelector = interpolateTemplate(scenario.readySelector || '', buildTemplateContext(plan, runToken, runtime.planVariables, scenario));
        const initialApis = (scenario.initialApis || []).map((item) => ({
          matcher: interpolateTemplate(item.matcher, buildTemplateContext(plan, runToken, runtime.planVariables, scenario)),
          label: item.label,
          optional: item.optional,
          timeout: item.timeout
        }));

        const routeApiResults = await helpers.openRoute(page, {
          path: route,
          expectedPath,
          readySelector: readySelector || undefined,
          api: initialApis
        });

        const stepResults = [];
        for (const step of scenario.steps || []) {
          await executePlanStep({
            step,
            page,
            helpers,
            context: buildTemplateContext(plan, runToken, runtime.planVariables, scenario),
            stepResults
          });
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

export function buildPlanExecutionEnhancement(planInfo) {
  const { plan, absolutePath } = planInfo;

  return ({ scenarioResults, options }) => {
    const workspaceRoot = options?.workspaceRoot || process.cwd();
    const relativePlanPath = path.relative(workspaceRoot, absolutePath).replace(/\\/g, '/');
    const recommendations = [];
    const failedResults = scenarioResults.filter((item) => item.status === 'failed');
    const missingAssertionScenarios = (plan.scenarios || []).filter(
      (scenario) => !(scenario.steps || []).some((step) => ['assertText', 'assertUrlIncludes'].includes(step.type))
    );

    if (failedResults.some((item) => /401|login|redirect/i.test(item.error || ''))) {
      recommendations.push('鉴权建议：检查登录步骤、token 注入链路与会话是否在页面跳转后丢失。');
    }

    if (failedResults.some((item) => /locator|wait failed|timeout|visible/i.test(item.error || ''))) {
      recommendations.push('选择器建议：优先使用稳定的 `id`、`data-testid` 或角色定位，减少文案漂移带来的波动。');
    }

    if (missingAssertionScenarios.length > 0) {
      recommendations.push(
        `断言建议：为以下场景补充页面断言，提升结果可信度：${missingAssertionScenarios.map((item) => item.name).join('、')}。`
      );
    }

    if (failedResults.length === 0) {
      recommendations.push('扩面建议：当前计划已跑通，可继续补充详情抽屉、导出、批量操作与异常路径场景。');
    }

    return {
      summaryExtras: {
        planPath: relativePlanPath,
        recommendations
      },
      detailExtras: {
        planPath: absolutePath,
        planName: plan.target?.planName || '',
        recommendations
      },
      reportSections: [
        '## Plan',
        '',
        `- Plan file: \`${relativePlanPath}\``,
        `- Plan name: \`${plan.target?.planName || 'unnamed-plan'}\``,
        `- Scenario count: \`${(plan.scenarios || []).length}\``,
        '',
        '## Suggestions',
        '',
        ...(recommendations.length > 0
          ? recommendations.map((item) => `- ${item}`)
          : ['- 当前暂无额外建议。'])
      ]
    };
  };
}
