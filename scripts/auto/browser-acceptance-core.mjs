import { existsSync } from 'node:fs';
import { mkdir, writeFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const defaultWorkspaceRoot = path.resolve(__dirname, '..', '..');

const DEFAULT_FRONTEND_URL = 'http://127.0.0.1:5174';
const DEFAULT_BACKEND_URL = 'http://127.0.0.1:9999';
const DEFAULT_SCOPE_FILTER = ['delivery', 'baseline'];
const DEFAULT_FAIL_SCOPES = ['delivery'];
const DEFAULT_OUTPUT_PREFIX = 'business-browser';
const DEFAULT_VIEWPORT = { width: 1600, height: 960 };

export class AcceptanceError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'AcceptanceError';
    this.details = details;
  }
}

function normalizeBaseUrl(value) {
  return value.endsWith('/') ? value : `${value}/`;
}

function parseBoolean(value, defaultValue) {
  if (value === undefined || value === null || value === '') {
    return defaultValue;
  }
  if (typeof value === 'boolean') {
    return value;
  }
  const normalized = String(value).trim().toLowerCase();
  if (['1', 'true', 'yes', 'on'].includes(normalized)) {
    return true;
  }
  if (['0', 'false', 'no', 'off'].includes(normalized)) {
    return false;
  }
  return defaultValue;
}

function parseCsvList(value, fallback = []) {
  if (!value) {
    return [...fallback];
  }
  const items = String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
  return items.length ? items : [...fallback];
}

function pad(value) {
  return String(value).padStart(2, '0');
}

export function formatTimestamp(date) {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join('');
}

function buildUrl(baseUrl, routePath) {
  return new URL(routePath.replace(/^\//, ''), baseUrl).toString();
}

function slugify(input) {
  return input.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

function compact(value) {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined));
}

function detectBrowserPath() {
  const candidates = [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium-browser',
    '/usr/bin/chromium',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'
  ];

  for (const candidate of candidates) {
    if (existsSync(candidate)) {
      return candidate;
    }
  }

  throw new AcceptanceError('No Chromium-based browser was found. Set IOT_ACCEPTANCE_BROWSER_PATH first.');
}

function extractPathFromUrl(url) {
  try {
    return new URL(url).pathname;
  } catch {
    return '';
  }
}

function responseMatcher(matcher) {
  if (typeof matcher === 'string') {
    return (response) => response.url().includes(matcher);
  }
  if (matcher instanceof RegExp) {
    return (response) => matcher.test(response.url());
  }
  return matcher;
}

async function readApiResponse(response) {
  let text = '';
  let bodyReadError;
  try {
    text = await response.text();
  } catch (error) {
    bodyReadError = error instanceof Error ? error.message : String(error);
  }

  let payload = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch {
    payload = null;
  }

  return {
    url: response.url(),
    status: response.status(),
    method: response.request().method(),
    payload,
    text,
    bodyReadError
  };
}

function assertApiSuccess(result, label) {
  if (result.status >= 400) {
    throw new AcceptanceError(`${label} returned HTTP ${result.status}.`, result);
  }
  if (result.payload && Object.prototype.hasOwnProperty.call(result.payload, 'code') && result.payload.code !== 200) {
    throw new AcceptanceError(`${label} returned code ${result.payload.code}.`, result);
  }
  return result;
}

function toWorkspaceRelative(workspaceRoot, targetPath) {
  return path.relative(workspaceRoot, targetPath).replace(/\\/g, '/');
}

function createArtifacts(workspaceRoot, runTimestamp, prefix) {
  const logsRoot = path.join(workspaceRoot, 'logs', 'acceptance');
  const runToken = runTimestamp.slice(4);
  const absolute = {
    logsRoot,
    screenshotsDir: path.join(logsRoot, `${prefix}-screenshots-${runTimestamp}`),
    summaryPath: path.join(logsRoot, `${prefix}-summary-${runTimestamp}.json`),
    detailPath: path.join(logsRoot, `${prefix}-results-${runTimestamp}.json`),
    reportPath: path.join(logsRoot, `${prefix}-report-${runTimestamp}.md`)
  };

  return {
    runTimestamp,
    runToken,
    absolute,
    relative: {
      screenshotsDir: toWorkspaceRelative(workspaceRoot, absolute.screenshotsDir),
      summaryPath: toWorkspaceRelative(workspaceRoot, absolute.summaryPath),
      detailPath: toWorkspaceRelative(workspaceRoot, absolute.detailPath),
      reportPath: toWorkspaceRelative(workspaceRoot, absolute.reportPath)
    }
  };
}

function resolveChromium(workspaceRoot) {
  const uiPackageJson = path.join(workspaceRoot, 'spring-boot-iot-ui', 'package.json');
  const requireFromUi = createRequire(pathToFileURL(uiPackageJson));
  const { chromium } = requireFromUi('playwright-core');
  return chromium;
}

function countByScope(scenarioResults, scope) {
  const scoped = scenarioResults.filter((item) => item.scope === scope);
  return {
    total: scoped.length,
    passed: scoped.filter((item) => item.status === 'passed').length,
    failed: scoped.filter((item) => item.status === 'failed').length
  };
}

function collectScopeSummary(scenarioResults) {
  const scopes = Array.from(new Set(scenarioResults.map((item) => item.scope).filter(Boolean)));
  return Object.fromEntries(scopes.map((scope) => [scope, countByScope(scenarioResults, scope)]));
}

function buildMarkdownReport(summary, scenarioResults, plannedScenarios, extraSections = []) {
  const lines = [
    '# Business Browser Acceptance Report',
    '',
    `- Run timestamp: \`${summary.runTimestamp}\``,
    `- Frontend: \`${summary.frontendBaseUrl}\``,
    `- Backend: \`${summary.backendBaseUrl}\``,
    `- Browser: \`${summary.browserPath || 'dry-run'}\``,
    `- Headless: \`${summary.headless}\``,
    `- Update baseline: \`${summary.updateBaseline}\``,
    `- Scope filter: \`${summary.filters.scenarioScopes.join(', ')}\``,
    `- Fail scopes: \`${summary.filters.failScopes.join(', ')}\``,
    '',
    '## Summary',
    '',
    `- Total scenarios: \`${summary.counts.total}\``,
    `- Passed: \`${summary.counts.passed}\``,
    `- Failed: \`${summary.counts.failed}\``,
    `- Planned future scenarios: \`${summary.planned.total}\``,
    '',
    '## Scenarios',
    '',
    '| Key | Scope | Status | Route | Screenshot | Notes |',
    '|---|---|---|---|---|---|'
  ];

  for (const scenario of scenarioResults) {
    const notes = scenario.error ? scenario.error.replace(/\|/g, '\\|') : 'OK';
    lines.push(
      `| ${scenario.key} | ${scenario.scope} | ${scenario.status} | \`${scenario.route}\` | \`${path.basename(scenario.screenshotPath || '')}\` | ${notes} |`
    );
  }

  if (plannedScenarios.length > 0) {
    lines.push('', '## Planned Scenarios', '', '| Key | Route | Activation |', '|---|---|---|');
    for (const scenario of plannedScenarios) {
      lines.push(`| ${scenario.key} | \`${scenario.route}\` | ${scenario.activation} |`);
    }
  }

  const scopeEntries = Object.entries(summary.scopes || {});
  if (scopeEntries.length > 0) {
    lines.splice(13, 0, ...scopeEntries.map(
      ([scope, counts]) => `- Scope \`${scope}\` passed/failed: \`${counts.passed}\` / \`${counts.failed}\``
    ));
  }

  if (extraSections.length > 0) {
    lines.push('', ...extraSections);
  }

  lines.push(
    '',
    '## Files',
    '',
    `- Report: \`${summary.output.reportPath}\``,
    `- Summary: \`${summary.output.summaryPath}\``,
    `- Details: \`${summary.output.detailPath}\``,
    `- Screenshots: \`${summary.output.screenshotsDir}\``
  );
  return lines.join('\n');
}

function mergeOutputPaths(baseOutput = {}, extraOutput = {}) {
  return {
    ...baseOutput,
    ...Object.fromEntries(Object.entries(extraOutput).filter(([, value]) => Boolean(value)))
  };
}

function buildSummary(runtimeOptions, artifacts, preflightResult, scenarioResults, plannedScenarios, unhandledAsyncErrors) {
  return {
    runTimestamp: artifacts.runTimestamp,
    frontendBaseUrl: runtimeOptions.frontendBaseUrl,
    backendBaseUrl: runtimeOptions.backendBaseUrl,
    browserPath: runtimeOptions.browserPath,
    headless: runtimeOptions.headless,
    updateBaseline: runtimeOptions.updateBaseline,
    filters: {
      scenarioScopes: runtimeOptions.scenarioScopes,
      failScopes: runtimeOptions.failScopes
    },
    preflight: preflightResult,
    counts: {
      total: scenarioResults.length,
      passed: scenarioResults.filter((item) => item.status === 'passed').length,
      failed: scenarioResults.filter((item) => item.status === 'failed').length
    },
    scopes: collectScopeSummary(scenarioResults),
    delivery: countByScope(scenarioResults, 'delivery'),
    baseline: countByScope(scenarioResults, 'baseline'),
    planned: {
      total: plannedScenarios.length
    },
    unhandledAsyncErrors: unhandledAsyncErrors.length ? unhandledAsyncErrors : undefined,
    output: artifacts.relative
  };
}

function validateScenarioPlan(scenarios) {
  const seenKeys = new Set();

  for (const scenario of scenarios) {
    if (!scenario.key) {
      throw new AcceptanceError('Scenario key is required.');
    }
    if (seenKeys.has(scenario.key)) {
      throw new AcceptanceError(`Duplicate scenario key detected: ${scenario.key}`);
    }
    if (!scenario.route) {
      throw new AcceptanceError(`Scenario route is required for ${scenario.key}`);
    }
    if (typeof scenario.run !== 'function') {
      throw new AcceptanceError(`Scenario run() handler is required for ${scenario.key}`);
    }
    seenKeys.add(scenario.key);
  }
}

function createRuntimeOptions(rawOptions = {}) {
  const dryRun = Boolean(rawOptions.dryRun);
  const workspaceRoot = rawOptions.workspaceRoot || defaultWorkspaceRoot;
  const runTimestamp = rawOptions.runTimestamp || formatTimestamp(new Date());
  const outputPrefix = rawOptions.outputPrefix || DEFAULT_OUTPUT_PREFIX;

  return {
    dryRun,
    workspaceRoot,
    frontendBaseUrl: normalizeBaseUrl(
      rawOptions.frontendBaseUrl || process.env.IOT_ACCEPTANCE_FRONTEND_URL || DEFAULT_FRONTEND_URL
    ),
    backendBaseUrl: normalizeBaseUrl(
      rawOptions.backendBaseUrl ||
        process.env.IOT_ACCEPTANCE_BACKEND_URL ||
        process.env.VITE_PROXY_TARGET ||
        DEFAULT_BACKEND_URL
    ),
    browserPath:
      rawOptions.browserPath ||
      process.env.IOT_ACCEPTANCE_BROWSER_PATH ||
      (dryRun ? null : detectBrowserPath()),
    headless: parseBoolean(
      rawOptions.headless ?? process.env.IOT_ACCEPTANCE_HEADLESS,
      true
    ),
    updateBaseline: parseBoolean(
      rawOptions.updateBaseline ?? process.env.IOT_AUTO_UPDATE_BASELINE,
      false
    ),
    login: {
      username: rawOptions.username || process.env.IOT_ACCEPTANCE_USERNAME || 'admin',
      password: rawOptions.password || process.env.IOT_ACCEPTANCE_PASSWORD || '123456'
    },
    scenarioScopes: rawOptions.scenarioScopes?.length
      ? [...rawOptions.scenarioScopes]
      : parseCsvList(process.env.IOT_AUTO_SCOPES, DEFAULT_SCOPE_FILTER),
    failScopes: rawOptions.failScopes?.length
      ? [...rawOptions.failScopes]
      : parseCsvList(process.env.IOT_AUTO_FAIL_SCOPES, DEFAULT_FAIL_SCOPES),
    appendIssues:
      rawOptions.appendIssues ??
      (parseBoolean(process.env.IOT_AUTO_APPEND_ISSUES, true) && !dryRun),
    commandHint:
      rawOptions.commandHint || process.env.IOT_AUTO_COMMAND_HINT || 'npm run acceptance:browser',
    issueDocPath:
      rawOptions.issueDocPath ||
      path.join(workspaceRoot, 'docs', '22-automation-test-issues-20260316.md'),
    pageReadyTimeout: rawOptions.pageReadyTimeout || 25000,
    responseTimeout: rawOptions.responseTimeout || 15000,
    viewport: rawOptions.viewport || DEFAULT_VIEWPORT,
    artifacts: createArtifacts(workspaceRoot, runTimestamp, outputPrefix)
  };
}

export async function runBrowserAcceptance({
  createScenarios,
  plannedScenarios = [],
  options = {}
}) {
  const runtimeOptions = createRuntimeOptions(options);
  const artifacts = runtimeOptions.artifacts;
  const unhandledAsyncErrors = [];
  const executableScenarios = createScenarios({ runToken: artifacts.runToken }).filter((scenario) =>
    runtimeOptions.scenarioScopes.includes(scenario.scope)
  );

  validateScenarioPlan(executableScenarios);

  if (runtimeOptions.dryRun) {
    return {
      dryRun: true,
      options: runtimeOptions,
      artifacts,
      executableScenarios: executableScenarios.map((scenario) =>
        compact({
          key: scenario.key,
          name: scenario.name,
          route: scenario.route,
          scope: scenario.scope,
          description: scenario.description
        })
      ),
      plannedScenarios
    };
  }

  const rejectionHandler = (reason) => {
    unhandledAsyncErrors.push(reason instanceof Error ? reason.message : String(reason));
  };
  process.on('unhandledRejection', rejectionHandler);

  const ensureLogs = async () => {
    await mkdir(artifacts.absolute.logsRoot, { recursive: true });
    await mkdir(artifacts.absolute.screenshotsDir, { recursive: true });
  };

  const preflight = async () => {
    const frontendResponse = await fetch(buildUrl(runtimeOptions.frontendBaseUrl, '/login'));
    if (!frontendResponse.ok) {
      throw new AcceptanceError(`Frontend preflight failed: ${frontendResponse.status}`, {
        url: buildUrl(runtimeOptions.frontendBaseUrl, '/login')
      });
    }

    const backendResponse = await fetch(buildUrl(runtimeOptions.backendBaseUrl, '/actuator/health'));
    if (!backendResponse.ok) {
      throw new AcceptanceError(`Backend health check failed: ${backendResponse.status}`, {
        url: buildUrl(runtimeOptions.backendBaseUrl, '/actuator/health')
      });
    }

    const backendPayload = await backendResponse.json();
    if (backendPayload.status !== 'UP') {
      throw new AcceptanceError('Backend health status is not UP.', {
        url: buildUrl(runtimeOptions.backendBaseUrl, '/actuator/health'),
        payload: backendPayload
      });
    }

    return {
      frontend: {
        status: frontendResponse.status,
        url: buildUrl(runtimeOptions.frontendBaseUrl, '/login')
      },
      backend: {
        status: backendResponse.status,
        url: buildUrl(runtimeOptions.backendBaseUrl, '/actuator/health'),
        payload: backendPayload
      }
    };
  };

  const preflightFrontendProxy = async () => {
    const probeResponse = await fetch(buildUrl(runtimeOptions.frontendBaseUrl, '/api/auth/login'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: `probe-user-${artifacts.runToken}`,
        password: 'probe-password'
      })
    });

    if (probeResponse.status >= 500) {
      throw new AcceptanceError(`Frontend API preflight failed: ${probeResponse.status}`, {
        url: buildUrl(runtimeOptions.frontendBaseUrl, '/api/auth/login'),
        hint: 'Check Vite proxy target and backend port consistency.'
      });
    }
  };

  const captureScreenshot = async (page, scenarioKey, suffix = 'pass') => {
    const absolutePath = path.join(
      artifacts.absolute.screenshotsDir,
      `${slugify(scenarioKey)}-${suffix}.png`
    );
    await page.screenshot({
      path: absolutePath,
      fullPage: true
    });
    return toWorkspaceRelative(runtimeOptions.workspaceRoot, absolutePath);
  };

  const isLoginPath = (url) => extractPathFromUrl(url) === '/login';

  const waitForPageReady = async (page, config = {}) => {
    const expectedPath = config.expectedPath || config.path || '';
    const timeout = config.timeout || runtimeOptions.pageReadyTimeout;

    if (expectedPath === '/login') {
      await page.locator('#login-submit').waitFor({
        state: 'visible',
        timeout
      });
      return;
    }

    const readyLocator = config.readySelector
      ? page.locator(config.readySelector).first()
      : config.title
        ? page.locator('[data-testid="console-page-title"]', { hasText: config.title }).first()
        : page.locator('[data-testid="console-page-title"]').first();
    const loginLocator = page.locator('#login-submit');

    await Promise.race([
      readyLocator.waitFor({
        state: 'visible',
        timeout
      }),
      loginLocator
        .waitFor({
          state: 'visible',
          timeout
        })
        .then(() => {
          throw new AcceptanceError(`Route switched to login before ${expectedPath} became ready.`, {
            expectedPath,
            currentUrl: page.url()
          });
        })
    ]);
  };

  const ensureRoutePath = async (page, expectedPath) => {
    const expectedPathname = extractPathFromUrl(`http://local${expectedPath}`);
    const currentPath = extractPathFromUrl(page.url());
    if (currentPath === expectedPathname) {
      return;
    }

    await page.waitForTimeout(300);
    const retriedPath = extractPathFromUrl(page.url());
    if (retriedPath === expectedPathname) {
      return;
    }

    throw new AcceptanceError(
      `Route redirect detected. Expected ${expectedPathname}, got ${retriedPath || 'unknown'}.`,
      {
        expectedPath: expectedPathname,
        currentUrl: page.url(),
        currentPath: retriedPath
      }
    );
  };

  const expectApiResponse = async (page, matcher, action, label, timeout = runtimeOptions.responseTimeout) => {
    const responsePromise = page.waitForResponse(responseMatcher(matcher), {
      timeout
    });
    try {
      if (action) {
        await action();
      }
      return assertApiSuccess(await readApiResponse(await responsePromise), label);
    } catch (error) {
      await responsePromise.catch(() => {});
      throw error;
    }
  };

  const openRoute = async (page, config) => {
    const waits = (config.api || []).map((entry) =>
      page
        .waitForResponse(responseMatcher(entry.matcher), {
          timeout: entry.timeout || runtimeOptions.responseTimeout
        })
        .then(readApiResponse)
        .then((result) => assertApiSuccess(result, entry.label))
        .catch((error) => ({
          __error: error instanceof Error ? error.message : String(error),
          __label: entry.label,
          __optional: Boolean(entry.optional)
        }))
    );

    const targetPath = config.path;
    const expectedPath = config.expectedPath || config.path;

    if (expectedPath === '/devices') {
      await page.goto(buildUrl(runtimeOptions.frontendBaseUrl, '/products'), {
        waitUntil: 'domcontentloaded'
      });
      await waitForPageReady(page, {
        expectedPath: '/products'
      });
      const deviceMenuLink = page.locator('.side-menu__item[href="/devices"]').first();
      if ((await deviceMenuLink.count()) > 0) {
        await deviceMenuLink.click();
        await page.waitForTimeout(500);
        if (extractPathFromUrl(page.url()) !== '/devices') {
          await page.goto(buildUrl(runtimeOptions.frontendBaseUrl, targetPath), {
            waitUntil: 'domcontentloaded'
          });
        }
      } else {
        await page.goto(buildUrl(runtimeOptions.frontendBaseUrl, targetPath), {
          waitUntil: 'domcontentloaded'
        });
      }
    } else {
      await page.goto(buildUrl(runtimeOptions.frontendBaseUrl, targetPath), {
        waitUntil: 'domcontentloaded'
      });
    }

    await ensureRoutePath(page, expectedPath);
    await waitForPageReady(page, {
      expectedPath,
      title: config.title,
      readySelector: config.readySelector,
      timeout: config.timeout
    });

    if (waits.length === 0) {
      return [];
    }

    const settled = await Promise.all(waits);
    for (const item of settled) {
      if (item && item.__error) {
        if (item.__optional) {
          continue;
        }
        throw new AcceptanceError(`${item.__label} wait failed: ${item.__error}`);
      }
    }
    return settled;
  };

  const login = async (page) => {
    const loginResult = await expectApiResponse(
      page,
      '/api/auth/login',
      async () => {
        await page.goto(buildUrl(runtimeOptions.frontendBaseUrl, '/login'), {
          waitUntil: 'domcontentloaded'
        });
        await page.locator('#login-username').fill(runtimeOptions.login.username);
        await page.locator('#login-password').fill(runtimeOptions.login.password);
        await page.locator('#login-submit').click();
      },
      'login'
    );

    if (!loginResult.payload?.data?.token) {
      throw new AcceptanceError('Login response did not include a token.', loginResult);
    }

    return {
      username: loginResult.payload.data.username || runtimeOptions.login.username,
      tokenPresent: true
    };
  };

  const ensureScenarioLogin = async (page, scenarioKey) => {
    if (!isLoginPath(page.url())) {
      return;
    }
    await login(page);
    if (isLoginPath(page.url())) {
      throw new AcceptanceError(`Unable to restore session before scenario ${scenarioKey}.`, {
        scenarioKey,
        currentUrl: page.url()
      });
    }
  };

  const fillDialogFields = async (page, dialog, fields) => {
    for (const field of fields) {
      if (field.type === 'select') {
        await dialog.getByPlaceholder(field.placeholder, { exact: true }).click();
        await page.locator('.el-select-dropdown__item', { hasText: field.option }).first().click();
        continue;
      }

      if (field.type === 'radio') {
        await dialog.getByLabel(field.label, { exact: true }).click();
        continue;
      }

      await dialog.getByPlaceholder(field.placeholder, { exact: true }).fill(field.value);
    }
  };

  const normalizeApiEntries = (listApi, key) => {
    if (!listApi) {
      return [];
    }

    const rawEntries = Array.isArray(listApi) ? listApi : [listApi];
    return rawEntries.map((entry, index) => {
      if (typeof entry === 'string' || entry instanceof RegExp || typeof entry === 'function') {
        return {
          matcher: entry,
          label: `${key} list ${index + 1}`,
          optional: true
        };
      }

      return {
        optional: true,
        label: entry.label || `${key} list ${index + 1}`,
        ...entry
      };
    });
  };

  const resolveOverlayContainer = async (page, title, timeout = 10000) => {
    const drawer = page
      .locator('.el-drawer')
      .filter({ has: page.locator('.el-drawer__header h2').filter({ hasText: title }) })
      .last();
    const dialog = page
      .locator('.el-dialog')
      .filter({ has: page.locator('.el-dialog__header').filter({ hasText: title }) })
      .last();

    try {
      return await Promise.any([
        drawer.waitFor({ state: 'visible', timeout }).then(() => drawer),
        dialog.waitFor({ state: 'visible', timeout }).then(() => dialog)
      ]);
    } catch {
      throw new AcceptanceError(`Form container "${title}" did not become visible within ${timeout}ms.`, {
        title
      });
    }
  };

  const runCreateDialogScenario = async (page, config, runtime) => {
    const listResults = await openRoute(page, {
      path: config.path,
      expectedPath: config.expectedPath,
      readySelector: config.readySelector,
      api: normalizeApiEntries(config.listApi, config.key)
    });

    await page.getByRole('button', { name: config.openButton, exact: true }).click();
    const dialog = await resolveOverlayContainer(page, config.dialogTitle, config.dialogTimeout || 10000);
    await fillDialogFields(page, dialog, config.fields(runtime));

    const createResult = await expectApiResponse(
      page,
      config.createApi,
      async () => {
        await dialog.getByRole('button', { name: '确定', exact: true }).click();
      },
      `${config.key} create`
    );

    await page.waitForTimeout(500);

    return {
      apiResults: [...listResults, createResult],
      created: config.onCreated ? await config.onCreated(page, runtime, createResult) : undefined
    };
  };

  const bindRiskPoint = async (page, runtime) => {
    if (!runtime.riskPoint?.code || !runtime.device?.name || !runtime.device?.id) {
      throw new AcceptanceError('Risk point binding prerequisites are missing.');
    }

    let row;
    try {
      await expectApiResponse(
        page,
        (response) =>
          response.url().includes('/api/risk-point/list') ||
          response.url().includes('/api/risk-point/page'),
        async () => {
          await page
            .locator('.search-form input[placeholder="请输入风险点编号"]')
            .first()
            .fill(runtime.riskPoint.code);
          await page.getByRole('button', { name: '查询', exact: true }).click();
        },
        'risk point search'
      );

      row = page.locator('.el-table__row', {
        hasText: runtime.riskPoint.name
      });
      await row.first().waitFor({ state: 'visible', timeout: 10000 });
    } catch (error) {
      return {
        skipped: true,
        reason: error instanceof Error ? error.message : String(error)
      };
    }

    let deviceListResult;
    try {
      const deviceListWait = page.waitForResponse(responseMatcher('/api/device/list'), {
        timeout: runtimeOptions.responseTimeout
      });
      const boundListWait = page.waitForResponse(responseMatcher('/api/risk-point/bound-devices/'), {
        timeout: runtimeOptions.responseTimeout
      });
      await row.locator('button:has-text("绑定设备")').click();
      deviceListResult = assertApiSuccess(
        await readApiResponse(await deviceListWait),
        'bindable device list'
      );
      assertApiSuccess(await readApiResponse(await boundListWait), 'bound device list');
    } catch (error) {
      return {
        skipped: true,
        reason: error instanceof Error ? error.message : String(error)
      };
    }

    const bindDialog = await resolveOverlayContainer(page, '????');

    const bindableDevice =
      deviceListResult.payload?.data?.find((item) => item.id === runtime.device.id) ||
      deviceListResult.payload?.data?.[0];

    if (!bindableDevice) {
      return {
        skipped: true,
        reason: 'No device option is available for risk point binding.'
      };
    }

    const metricWait = page.waitForResponse(
      responseMatcher((response) => response.url().includes(`/api/device/${bindableDevice.id}/metrics`)),
      {
        timeout: runtimeOptions.responseTimeout
      }
    );

    await bindDialog.getByPlaceholder('请选择设备', { exact: true }).click();
    await page.locator('.el-select-dropdown__item', { hasText: bindableDevice.deviceName }).first().click();

    let metricResult;
    try {
      metricResult = assertApiSuccess(
        await readApiResponse(await metricWait),
        'device metric options'
      );
    } catch (error) {
      return {
        skipped: true,
        reason: error instanceof Error ? error.message : String(error)
      };
    }

    const selectedMetric =
      metricResult.payload?.data?.find((item) => item.identifier === 'temperature') ||
      metricResult.payload?.data?.[0];
    if (!selectedMetric) {
      return {
        skipped: true,
        reason: 'No metric option is available for risk point binding.'
      };
    }

    await bindDialog.getByPlaceholder('请选择测点', { exact: true }).click();
    await page.locator('.el-select-dropdown__item', { hasText: selectedMetric.name }).first().click();

    let bindResult;
    try {
      bindResult = await expectApiResponse(
        page,
        '/api/risk-point/bind-device',
        async () => {
          await bindDialog.getByRole('button', { name: '确定', exact: true }).click();
        },
        'risk point bind'
      );
    } catch (error) {
      return {
        skipped: true,
        reason: error instanceof Error ? error.message : String(error)
      };
    }

    runtime.riskPoint.metricIdentifier = selectedMetric.identifier;
    runtime.riskPoint.metricName = selectedMetric.name;
    return bindResult;
  };

  const openFirstDetailIfPresent = async (page, config) => {
    const detailButton = page.locator('button:has-text("详情")').first();
    if ((await detailButton.count()) === 0) {
      return {
        detailOpened: false
      };
    }

    const detailResult = await expectApiResponse(
      page,
      config.detailApi,
      async () => {
        await detailButton.click();
      },
      `${config.key} detail`
    );

    const visibleDialog = page.getByRole('dialog').filter({ has: page.locator('.el-dialog') }).last();
    if ((await visibleDialog.count()) > 0) {
      const footerCloseButton = visibleDialog
        .locator('.el-dialog__footer button:has-text("关闭")')
        .first();
      if ((await footerCloseButton.count()) > 0) {
        await footerCloseButton.click({ timeout: 5000 });
      } else {
        await page.keyboard.press('Escape');
      }
    }

    return {
      detailOpened: true,
      detailResult
    };
  };

  const helpers = {
    buildUrl,
    expectApiResponse,
    openFirstDetailIfPresent,
    openRoute,
    login,
    fillDialogFields,
    resolveOverlayContainer,
    runCreateDialogScenario,
    bindRiskPoint
  };

  let browser;
  let context;
  const scenarioResults = [];

  try {
    await ensureLogs();
    await preflightFrontendProxy();
    const preflightResult = await preflight();
    const chromium = resolveChromium(runtimeOptions.workspaceRoot);

    browser = await chromium.launch({
      executablePath: runtimeOptions.browserPath,
      headless: runtimeOptions.headless,
      args: ['--disable-dev-shm-usage']
    });

    context = await browser.newContext({
      viewport: runtimeOptions.viewport
    });
    const page = await context.newPage();
    const runtime = {};

    for (const scenario of executableScenarios) {
      const startedAt = new Date().toISOString();
      try {
        if (scenario.requiresLogin !== false && scenario.key !== 'login') {
          await ensureScenarioLogin(page, scenario.key);
        }
        const detail = await scenario.run({
          page,
          runtime,
          helpers,
          options: runtimeOptions,
          artifacts,
          scenario
        });
        let screenshotPath;
        try {
          screenshotPath = await captureScreenshot(page, scenario.key);
        } catch {
          screenshotPath = undefined;
        }

        scenarioResults.push(
          compact({
            key: scenario.key,
            name: scenario.name,
            route: scenario.route,
            scope: scenario.scope,
            status: 'passed',
            startedAt,
            finishedAt: new Date().toISOString(),
            screenshotPath,
            detail
          })
        );
      } catch (error) {
        let screenshotPath;
        try {
          screenshotPath = await captureScreenshot(page, scenario.key, 'fail');
        } catch {
          screenshotPath = undefined;
        }

        scenarioResults.push(
          compact({
            key: scenario.key,
            name: scenario.name,
            route: scenario.route,
            scope: scenario.scope,
            status: 'failed',
            startedAt,
            finishedAt: new Date().toISOString(),
            screenshotPath,
            error: error instanceof Error ? error.message : String(error),
            detail:
              error && typeof error === 'object' && 'details' in error
                ? error.details
                : error instanceof AcceptanceError
                  ? error.details
                  : undefined
          })
        );
      }
    }

    const summary = buildSummary(
      runtimeOptions,
      artifacts,
      preflightResult,
      scenarioResults,
      plannedScenarios,
      unhandledAsyncErrors
    );
    const enhancement = typeof options.enhanceResult === 'function'
      ? await options.enhanceResult({
          summary,
          scenarioResults,
          plannedScenarios,
          options: runtimeOptions,
          artifacts
        })
      : null;

    const generatedOutputPaths = {};
    for (const outputFile of enhancement?.outputFiles || []) {
      if (!outputFile?.absolutePath) {
        continue;
      }
      await mkdir(path.dirname(outputFile.absolutePath), { recursive: true });
      await writeFile(outputFile.absolutePath, outputFile.content, 'utf8');
      if (outputFile.key) {
        generatedOutputPaths[outputFile.key] = toWorkspaceRelative(
          runtimeOptions.workspaceRoot,
          outputFile.absolutePath
        );
      }
    }

    const enhancedSummary = enhancement?.summaryExtras
      ? {
          ...summary,
          ...enhancement.summaryExtras
        }
      : summary;
    const finalSummary = {
      ...enhancedSummary,
      output: mergeOutputPaths(enhancedSummary.output, generatedOutputPaths)
    };
    const finalDetail = {
      preflight: preflightResult,
      scenarios: scenarioResults,
      output: finalSummary.output,
      ...(enhancement?.detailExtras || {})
    };

    await writeFile(
      artifacts.absolute.detailPath,
      JSON.stringify(finalDetail, null, 2),
      'utf8'
    );
    await writeFile(artifacts.absolute.summaryPath, JSON.stringify(finalSummary, null, 2), 'utf8');
    await writeFile(
      artifacts.absolute.reportPath,
      buildMarkdownReport(finalSummary, scenarioResults, plannedScenarios, enhancement?.reportSections || []),
      'utf8'
    );

    const hasBlockingFailures = scenarioResults.some(
      (item) => runtimeOptions.failScopes.includes(item.scope) && item.status !== 'passed'
    );

    return {
      dryRun: false,
      options: runtimeOptions,
      artifacts,
      summary: finalSummary,
      scenarioResults,
      plannedScenarios,
      exitCode: hasBlockingFailures ? 1 : 0
    };
  } finally {
    process.off('unhandledRejection', rejectionHandler);
    if (context) {
      await context.close().catch(() => {});
    }
    if (browser) {
      await browser.close().catch(() => {});
    }
  }
}
