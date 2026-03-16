import { existsSync } from 'node:fs';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import { chromium } from 'playwright-core';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const workspaceRoot = path.resolve(__dirname, '..', '..');
const logsRoot = path.join(workspaceRoot, 'logs', 'acceptance');
const runTimestamp = formatTimestamp(new Date());
const runToken = runTimestamp.slice(4);
const screenshotsDir = path.join(logsRoot, `business-browser-screenshots-${runTimestamp}`);
const summaryPath = path.join(logsRoot, `business-browser-summary-${runTimestamp}.json`);
const detailPath = path.join(logsRoot, `business-browser-results-${runTimestamp}.json`);
const reportPath = path.join(logsRoot, `business-browser-report-${runTimestamp}.md`);

const frontendBaseUrl = normalizeBaseUrl(process.env.IOT_ACCEPTANCE_FRONTEND_URL || 'http://127.0.0.1:5174');
const backendBaseUrl = normalizeBaseUrl(
  process.env.IOT_ACCEPTANCE_BACKEND_URL || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:9999'
);
const browserPath = process.env.IOT_ACCEPTANCE_BROWSER_PATH || detectBrowserPath();
const headless = process.env.IOT_ACCEPTANCE_HEADLESS !== 'false';
const unhandledAsyncErrors = [];

process.on('unhandledRejection', (reason) => {
  unhandledAsyncErrors.push(reason instanceof Error ? reason.message : String(reason));
});

class AcceptanceError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'AcceptanceError';
    this.details = details;
  }
}

function normalizeBaseUrl(value) {
  return value.endsWith('/') ? value : `${value}/`;
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

function formatTimestamp(date) {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join('');
}

function pad(value) {
  return String(value).padStart(2, '0');
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

async function ensureLogs() {
  await mkdir(logsRoot, { recursive: true });
  await mkdir(screenshotsDir, { recursive: true });
}

async function preflight() {
  const frontendResponse = await fetch(buildUrl(frontendBaseUrl, '/login'));
  if (!frontendResponse.ok) {
    throw new AcceptanceError(`Frontend preflight failed: ${frontendResponse.status}`, {
      url: buildUrl(frontendBaseUrl, '/login')
    });
  }

  const backendResponse = await fetch(buildUrl(backendBaseUrl, '/actuator/health'));
  if (!backendResponse.ok) {
    throw new AcceptanceError(`Backend health check failed: ${backendResponse.status}`, {
      url: buildUrl(backendBaseUrl, '/actuator/health')
    });
  }

  const backendPayload = await backendResponse.json();
  if (backendPayload.status !== 'UP') {
    throw new AcceptanceError('Backend health status is not UP.', {
      url: buildUrl(backendBaseUrl, '/actuator/health'),
      payload: backendPayload
    });
  }

  return {
    frontend: {
      status: frontendResponse.status,
      url: buildUrl(frontendBaseUrl, '/login')
    },
    backend: {
      status: backendResponse.status,
      url: buildUrl(backendBaseUrl, '/actuator/health'),
      payload: backendPayload
    }
  };
}

async function preflightFrontendProxy() {
  const probeResponse = await fetch(buildUrl(frontendBaseUrl, '/api/auth/login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: `probe-user-${runToken}`,
      password: 'probe-password'
    })
  });

  if (probeResponse.status >= 500) {
    throw new AcceptanceError(`Frontend API preflight failed: ${probeResponse.status}`, {
      url: buildUrl(frontendBaseUrl, '/api/auth/login'),
      hint: 'Check Vite proxy target and backend port consistency.'
    });
  }
}

async function captureScreenshot(page, scenarioKey, suffix = 'pass') {
  const filePath = path.join(screenshotsDir, `${slugify(scenarioKey)}-${suffix}.png`);
  await page.screenshot({
    path: filePath,
    fullPage: true
  });
  return filePath;
}

function isLoginPath(url) {
  return extractPathFromUrl(url) === '/login';
}

async function waitForToolbarHeading(page, title, expectedPath) {
  const headingLocator = page.locator('[data-testid="console-page-title"]', { hasText: title });
  const timeout = 25000;

  if (!expectedPath || expectedPath === '/login') {
    await headingLocator.waitFor({
      state: 'visible',
      timeout
    });
    return;
  }

  const loginLocator = page.locator('#login-submit');
  await Promise.race([
    headingLocator.waitFor({
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
}

function extractPathFromUrl(url) {
  try {
    return new URL(url).pathname;
  } catch {
    return '';
  }
}

async function ensureRoutePath(page, expectedPath) {
  const expectedPathname = expectedPath ? extractPathFromUrl(`http://local${expectedPath}`) : expectedPath;
  const currentPath = extractPathFromUrl(page.url());
  if (currentPath === expectedPathname) {
    return;
  }

  await page.waitForTimeout(300);
  const retriedPath = extractPathFromUrl(page.url());
  if (retriedPath === expectedPathname) {
    return;
  }

  throw new AcceptanceError(`Route redirect detected. Expected ${expectedPathname}, got ${retriedPath || 'unknown'}.`, {
    expectedPath: expectedPathname,
    currentUrl: page.url(),
    currentPath: retriedPath
  });
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

async function expectApiResponse(page, matcher, action, label) {
  const responsePromise = page.waitForResponse(responseMatcher(matcher), {
    timeout: 15000
  });
  try {
    if (action) {
      await action();
    }
    return assertApiSuccess(await readApiResponse(await responsePromise), label);
  } catch (error) {
    // 避免 action 失败后遗留未处理的 waitForResponse rejection，导致脚本提前崩溃
    await responsePromise.catch(() => {});
    throw error;
  }
}

async function openRoute(page, config) {
  const waits = (config.api || []).map((entry) =>
    page
      .waitForResponse(responseMatcher(entry.matcher), {
        timeout: entry.timeout || 15000
      })
      .then(readApiResponse)
      .then((result) => assertApiSuccess(result, entry.label))
      .catch((error) => ({
        __error: error instanceof Error ? error.message : String(error),
        __label: entry.label
      }))
  );

  if (config.path === '/devices') {
    await page.goto(buildUrl(frontendBaseUrl, '/products'), {
      waitUntil: 'domcontentloaded'
    });
    await waitForToolbarHeading(page, '产品模板中心', '/products');
    const deviceMenuLink = page.locator('.side-menu__item[href="/devices"]').first();
    if ((await deviceMenuLink.count()) > 0) {
      await deviceMenuLink.click();
    } else {
      await page.goto(buildUrl(frontendBaseUrl, config.path), {
        waitUntil: 'domcontentloaded'
      });
    }
  } else {
    await page.goto(buildUrl(frontendBaseUrl, config.path), {
      waitUntil: 'domcontentloaded'
    });
  }

  await ensureRoutePath(page, config.path);
  await waitForToolbarHeading(page, config.heading, config.path);

  if (waits.length === 0) {
    return [];
  }

  const settled = await Promise.all(waits);
  for (const item of settled) {
    if (item && item.__error) {
      throw new AcceptanceError(`${item.__label} wait failed: ${item.__error}`);
    }
  }
  return settled;
}

async function login(page) {
  const loginResult = await expectApiResponse(
    page,
    '/api/auth/login',
    async () => {
      await page.goto(buildUrl(frontendBaseUrl, '/login'), {
        waitUntil: 'domcontentloaded'
      });
      await page.locator('#login-username').fill('admin');
      await page.locator('#login-password').fill('123456');
      await page.locator('#login-submit').click();
    },
    'login'
  );

  if (!loginResult.payload?.data?.token) {
    throw new AcceptanceError('Login response did not include a token.', loginResult);
  }
  return {
    username: loginResult.payload.data.username || 'admin',
    tokenPresent: true
  };
}

async function ensureScenarioLogin(page, scenarioKey) {
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
}

async function fillDialogFields(page, dialog, fields) {
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
}

async function runCreateDialogScenario(page, config, runtime) {
  const listResults = await openRoute(page, {
    path: config.path,
    heading: config.heading,
    api: config.listApi
      ? [
          {
            matcher: config.listApi,
            label: `${config.key} list`
          }
        ]
      : []
  });

  await page.getByRole('button', { name: config.openButton, exact: true }).click();
  const dialog = page.getByRole('dialog', { name: config.dialogTitle, exact: true });
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
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
}

async function bindRiskPoint(page, runtime) {
  if (!runtime.riskPoint?.code || !runtime.device?.name || !runtime.device?.id) {
    throw new AcceptanceError('Risk point binding prerequisites are missing.');
  }

  await expectApiResponse(
    page,
    '/api/risk-point/list',
    async () => {
      await page.locator('.search-form input[placeholder="请输入风险点编号"]').first().fill(runtime.riskPoint.code);
      await page.getByRole('button', { name: '查询', exact: true }).click();
    },
    'risk point search'
  );

  const row = page.locator('.el-table__row', {
    hasText: runtime.riskPoint.name
  });
  await row.first().waitFor({ state: 'visible', timeout: 10000 });

  let deviceListResult;
  try {
    const deviceListWait = page.waitForResponse(responseMatcher('/api/device/list'), {
      timeout: 15000
    });
    const boundListWait = page.waitForResponse(responseMatcher('/api/risk-point/bound-devices/'), {
      timeout: 15000
    });
    await row.locator('button:has-text("绑定设备")').click();
    deviceListResult = assertApiSuccess(await readApiResponse(await deviceListWait), 'bindable device list');
    assertApiSuccess(await readApiResponse(await boundListWait), 'bound device list');
  } catch (error) {
    return {
      skipped: true,
      reason: error instanceof Error ? error.message : String(error)
    };
  }

  const bindDialog = page.getByRole('dialog', { name: '绑定设备', exact: true });
  await bindDialog.waitFor({ state: 'visible', timeout: 10000 });

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
      timeout: 15000
    }
  );

  await bindDialog.getByPlaceholder('请选择设备', { exact: true }).click();
  await page.locator('.el-select-dropdown__item', { hasText: bindableDevice.deviceName }).first().click();

  let metricResult;
  try {
    metricResult = assertApiSuccess(await readApiResponse(await metricWait), 'device metric options');
  } catch (error) {
    return {
      skipped: true,
      reason: error instanceof Error ? error.message : String(error)
    };
  }
  const selectedMetric = metricResult.payload?.data?.find((item) => item.identifier === 'temperature') || metricResult.payload?.data?.[0];
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
}

async function openFirstDetailIfPresent(page, config) {
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
    const footerCloseButton = visibleDialog.locator('.el-dialog__footer button:has-text("关闭")').first();
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
}

function createScenarios() {
  return [
    {
      key: 'login',
      name: 'Login and auth bootstrap',
      route: '/login',
      scope: 'delivery',
      run: async (page) => login(page)
    },
    {
      key: 'product-workbench',
      name: 'Product create and query',
      route: '/products',
      scope: 'delivery',
      run: async (page, state) => {
        await openRoute(page, {
          path: '/products',
          heading: '产品模板中心'
        });

        state.product = {
          key: `accept-ui-product-${runToken}`,
          name: `UI Product ${runToken}`
        };

        const createResult = await expectApiResponse(
          page,
          '/api/device/product/add',
          async () => {
            await page.locator('#product-key').fill(state.product.key);
            await page.locator('#product-name').fill(state.product.name);
            await page.locator('#protocol-code').fill('mqtt-json');
            await page.locator('#data-format').fill('JSON');
            await page.locator('#data-format').press('Enter');
          },
          'product add'
        );

        const productId = createResult.payload?.data?.id;
        if (!productId) {
          throw new AcceptanceError('Product create response does not contain an id.', createResult);
        }
        state.product.id = productId;

        const queryResult = await expectApiResponse(
          page,
          (response) => response.url().includes(`/api/device/product/${productId}`),
          async () => {
            await page.locator('#query-product-id').fill(String(productId));
            await page.locator('#query-product-id').press('Enter');
          },
          'product query'
        );

        return {
          apiResults: [createResult, queryResult],
          created: state.product
        };
      }
    },
    {
      key: 'device-workbench',
      name: 'Device create and query',
      route: '/devices',
      scope: 'delivery',
      run: async (page, state) => {
        if (!state.product?.key) {
          throw new AcceptanceError('Device scenario requires a created product.');
        }

        await openRoute(page, {
          path: '/devices',
          heading: '设备运维中心'
        });

        state.device = {
          name: `UI Device ${runToken}`,
          code: `accept-ui-device-${runToken}`,
          secret: '123456'
        };

        const createResult = await expectApiResponse(
          page,
          '/api/device/add',
          async () => {
            await page.locator('#device-product-key').fill(state.product.key);
            await page.locator('#device-name').fill(state.device.name);
            await page.locator('#device-code').fill(state.device.code);
            await page.locator('#device-secret').fill(state.device.secret);
            await page.locator('#client-id').fill(state.device.code);
            await page.locator('#username').fill(state.device.code);
            await page.locator('#password').fill(state.device.secret);
            await page.locator('#firmware').fill('1.0.0');
            await page.locator('#ip-address').fill('127.0.0.1');
            await page.locator('#address').fill('UI browser acceptance');
            await page.getByRole('button', { name: '提交设备建档', exact: true }).click();
          },
          'device add'
        );

        const deviceId = createResult.payload?.data?.id;
        if (!deviceId) {
          throw new AcceptanceError('Device create response does not contain an id.', createResult);
        }
        state.device.id = deviceId;

        const queryByIdResult = await expectApiResponse(
          page,
          (response) => response.url().includes(`/api/device/${deviceId}`),
          async () => {
            await page.locator('#query-device-id').fill(String(deviceId));
            await page.getByRole('button', { name: '按 ID 查询', exact: true }).click();
          },
          'device query by id'
        );

        const queryByCodeResult = await expectApiResponse(
          page,
          (response) => response.url().includes(`/api/device/code/${state.device.code}`),
          async () => {
            await page.locator('#query-device-code').fill(state.device.code);
            await page.getByRole('button', { name: '按编码查询', exact: true }).click();
          },
          'device query by code'
        );

        return {
          apiResults: [createResult, queryByIdResult, queryByCodeResult],
          created: state.device
        };
      }
    },
    {
      key: 'report-workbench',
      name: 'HTTP report submission',
      route: '/reporting',
      scope: 'delivery',
      run: async (page, state) => {
        if (!state.product?.key || !state.device?.code) {
          throw new AcceptanceError('HTTP report scenario requires a created product and device.');
        }

        await openRoute(page, {
          path: '/reporting',
          heading: '接入回放'
        });

        state.report = {
          topic: `/sys/${state.product.key}/${state.device.code}/thing/property/post`,
          payload: JSON.stringify({
            messageType: 'property',
            properties: {
              temperature: 26.5,
              humidity: 68
            }
          })
        };

        const reportResult = await expectApiResponse(
          page,
          '/message/http/report',
          async () => {
            await page.locator('#report-protocol').fill('mqtt-json');
            await page.locator('#report-product-key').fill(state.product.key);
            await page.locator('#report-device-code').fill(state.device.code);
            await page.locator('#report-client-id').fill(state.device.code);
            await page.locator('#report-tenant').fill('1');
            await page.locator('#report-topic').fill(state.report.topic);
            await page.locator('#payload').fill(state.report.payload);
            await page.getByRole('button', { name: '发送上报', exact: true }).click();
          },
          'http report'
        );

        return {
          apiResults: [reportResult],
          report: state.report
        };
      }
    },
    {
      key: 'device-insight',
      name: 'Device insight refresh',
      route: '/insight',
      scope: 'delivery',
      run: async (page, state) => {
        if (!state.device?.code) {
          throw new AcceptanceError('Insight scenario requires a created device.');
        }

        const apiResults = await openRoute(page, {
          path: `/insight?deviceCode=${encodeURIComponent(state.device.code)}`,
          heading: '风险点工作台',
          api: [
            {
              matcher: (response) => response.url().includes(`/api/device/code/${state.device.code}`),
              label: 'device detail for insight'
            },
            {
              matcher: (response) => response.url().includes(`/api/device/${state.device.code}/properties`),
              label: 'device property snapshot'
            },
            {
              matcher: (response) => response.url().includes(`/api/device/${state.device.code}/message-logs`),
              label: 'device message logs'
            }
          ]
        });

        return {
          apiResults
        };
      }
    },
    {
      key: 'alarm-center',
      name: 'Alarm list and detail',
      route: '/alarm-center',
      scope: 'delivery',
      run: async (page) => {
        const apiResults = await openRoute(page, {
          path: '/alarm-center',
          heading: '告警中心',
          api: [
            {
              matcher: '/api/alarm/list',
              label: 'alarm list'
            }
          ]
        });

        return {
          apiResults,
          detail: await openFirstDetailIfPresent(page, {
            key: 'alarm-center',
            detailApi: /\/api\/alarm\/\d+$/
          })
        };
      }
    },
    {
      key: 'event-disposal',
      name: 'Event list and detail',
      route: '/event-disposal',
      scope: 'delivery',
      run: async (page) => {
        const apiResults = await openRoute(page, {
          path: '/event-disposal',
          heading: '事件处置',
          api: [
            {
              matcher: '/api/event/list',
              label: 'event list'
            }
          ]
        });

        return {
          apiResults,
          detail: await openFirstDetailIfPresent(page, {
            key: 'event-disposal',
            detailApi: /\/api\/event\/\d+$/
          })
        };
      }
    },
    {
      key: 'risk-point',
      name: 'Risk point create and bind device',
      route: '/risk-point',
      scope: 'delivery',
      run: async (page, state) => {
        state.riskPoint = {
          code: `ACCEPT-RP-${runToken}`,
          name: `UI Risk Point ${runToken}`
        };

        const createResult = await runCreateDialogScenario(
          page,
          {
            key: 'risk-point',
            path: '/risk-point',
            heading: '风险点管理',
            listApi: '/api/risk-point/list',
            openButton: '新增风险点',
            dialogTitle: '新增风险点',
            createApi: '/api/risk-point/add',
            fields: () => [
              { placeholder: '请输入风险点编号', value: state.riskPoint.code },
              { placeholder: '请输入风险点名称', value: state.riskPoint.name },
              { placeholder: '请输入区域名称', value: 'Browser Region' },
              { placeholder: '请输入负责人电话', value: '13800138000' },
              { placeholder: '请输入描述', value: 'Created by browser acceptance.' }
            ],
            onCreated: () => state.riskPoint
          },
          state
        );

        const bindResult = await bindRiskPoint(page, state);

        return {
          apiResults: bindResult?.skipped ? [...createResult.apiResults] : [...createResult.apiResults, bindResult],
          binding: bindResult,
          created: state.riskPoint
        };
      }
    },
    {
      key: 'rule-definition',
      name: 'Rule definition create',
      route: '/rule-definition',
      scope: 'delivery',
      run: async (page, state) =>
        runCreateDialogScenario(
          page,
          {
            key: 'rule-definition',
            path: '/rule-definition',
            heading: '阈值规则',
            listApi: '/api/rule-definition/list',
            openButton: '新增规则',
            dialogTitle: '新增规则',
            createApi: '/api/rule-definition/add',
            fields: () => [
              { placeholder: '请输入规则名称', value: `UI Rule ${runToken}` },
              { placeholder: '请输入测点标识符', value: state.riskPoint?.metricIdentifier || 'temperature' },
              { placeholder: '请输入测点名称', value: state.riskPoint?.metricName || 'Temperature' },
              { placeholder: '例如：value > 100', value: 'value > 20' },
              { placeholder: '请输入描述', value: 'Browser acceptance threshold rule.' }
            ]
          },
          state
        )
    },
    {
      key: 'linkage-rule',
      name: 'Linkage rule create',
      route: '/linkage-rule',
      scope: 'delivery',
      run: async (page, state) =>
        runCreateDialogScenario(
          page,
          {
            key: 'linkage-rule',
            path: '/linkage-rule',
            heading: '联动规则',
            listApi: '/api/linkage-rule/list',
            openButton: '新增规则',
            dialogTitle: '新增规则',
            createApi: '/api/linkage-rule/add',
            fields: () => [
              { placeholder: '请输入规则名称', value: `UI Linkage ${runToken}` },
              { placeholder: '请输入描述', value: 'Browser acceptance linkage rule.' },
              {
                placeholder: '请输入触发条件（JSON格式）',
                value: JSON.stringify([{ metricIdentifier: state.riskPoint?.metricIdentifier || 'temperature', operator: '>', threshold: 20 }])
              },
              {
                placeholder: '请输入动作列表（JSON格式）',
                value: JSON.stringify([{ actionType: 'notify', channel: 'email' }])
              }
            ]
          },
          state
        )
    },
    {
      key: 'emergency-plan',
      name: 'Emergency plan create',
      route: '/emergency-plan',
      scope: 'delivery',
      run: async (page) =>
        runCreateDialogScenario(
          page,
          {
            key: 'emergency-plan',
            path: '/emergency-plan',
            heading: '应急预案',
            listApi: '/api/emergency-plan/list',
            openButton: '新增预案',
            dialogTitle: '新增预案',
            createApi: '/api/emergency-plan/add',
            fields: () => [
              { placeholder: '请输入预案名称', value: `UI Plan ${runToken}` },
              { placeholder: '请输入描述', value: 'Browser acceptance emergency plan.' },
              {
                placeholder: '请输入响应步骤（JSON格式）',
                value: JSON.stringify([{ step: 1, action: 'notify operator' }])
              },
              {
                placeholder: '请输入联系人列表（JSON格式）',
                value: JSON.stringify([{ name: 'Ops User', phone: '13800138000' }])
              }
            ]
          },
          {}
        )
    },
    {
      key: 'report-analysis',
      name: 'Report analysis page load',
      route: '/report-analysis',
      scope: 'delivery',
      run: async (page) => ({
        apiResults: await openRoute(page, {
          path: '/report-analysis',
          heading: '分析报表',
          api: [
            { matcher: '/api/report/risk-trend', label: 'risk trend' },
            { matcher: '/api/report/alarm-statistics', label: 'alarm statistics' },
            { matcher: '/api/report/event-closure', label: 'event closure' },
            { matcher: '/api/report/device-health', label: 'device health' }
          ]
        })
      })
    },
    {
      key: 'organization',
      name: 'Organization create',
      route: '/organization',
      scope: 'delivery',
      run: async (page) =>
        runCreateDialogScenario(
          page,
          {
            key: 'organization',
            path: '/organization',
            heading: '组织机构',
            listApi: '/api/organization/tree',
            openButton: '新增',
            dialogTitle: '新增组织机构',
            createApi: '/api/organization',
            fields: () => [
              { placeholder: '请输入组织名称', value: `UI Org ${runToken}` },
              { placeholder: '请输入组织编码', value: `ACCEPT-ORG-${runToken}` },
              { placeholder: '请输入负责人姓名', value: 'Ops Lead' },
              { placeholder: '请输入联系电话', value: '13800138001' },
              { placeholder: '请输入邮箱', value: `org-${runToken}@example.com` },
              { placeholder: '请输入备注', value: 'Created by browser acceptance.' }
            ]
          },
          {}
        )
    },
    {
      key: 'role',
      name: 'Role create',
      route: '/role',
      scope: 'delivery',
      run: async (page, state) =>
        runCreateDialogScenario(
          page,
          {
            key: 'role',
            path: '/role',
            heading: '角色管理',
            listApi: '/api/role/list',
            openButton: '新增',
            dialogTitle: '新增角色',
            createApi: '/api/role/add',
            fields: () => [
              { placeholder: '请输入角色名称', value: `UI Role ${runToken}` },
              { placeholder: '请输入角色编码', value: `ACCEPT_ROLE_${runToken}` },
              { placeholder: '请输入角色描述', value: 'Browser acceptance role.' }
            ],
            onCreated: () => {
              state.role = {
                name: `UI Role ${runToken}`,
                code: `ACCEPT_ROLE_${runToken}`
              };
              return state.role;
            }
          },
          state
        )
    },
    {
      key: 'user',
      name: 'User create',
      route: '/user',
      scope: 'delivery',
      run: async (page, state) => {
        const [userListResult] = await openRoute(page, {
          path: '/user',
          heading: '用户管理',
          api: [
            {
              matcher: '/api/user/list',
              label: 'user list'
            }
          ]
        });

        await page.getByRole('button', { name: '新增', exact: true }).click();
        const dialog = page.getByRole('dialog', { name: '新增用户', exact: true });
        await dialog.waitFor({ state: 'visible', timeout: 10000 });

        const createdUser = {
          username: `accept_ui_${runToken}`,
          realName: `UI User ${runToken}`,
          phone: `139${runToken.slice(-8)}`,
          email: `user-${runToken}@example.com`
        };

        await fillDialogFields(page, dialog, [
          { placeholder: '请输入用户名', value: createdUser.username },
          { placeholder: '请输入真实姓名', value: createdUser.realName },
          { placeholder: '请输入手机号', value: createdUser.phone },
          { placeholder: '请输入邮箱', value: createdUser.email },
          { placeholder: '请输入密码', value: '123456' }
        ]);

        const createResult = await expectApiResponse(
          page,
          '/api/user/add',
          async () => {
            await dialog.getByRole('button', { name: '确定', exact: true }).click();
          },
          'user create'
        )

        return {
          apiResults: [userListResult, createResult],
          created: createdUser
        };
      }
    },
    {
      key: 'region',
      name: 'Region create',
      route: '/region',
      scope: 'delivery',
      run: async (page) =>
        runCreateDialogScenario(
          page,
          {
            key: 'region',
            path: '/region',
            heading: '区域管理',
            listApi: '/api/region/tree',
            openButton: '新增',
            dialogTitle: '新增区域',
            createApi: '/api/region',
            fields: () => [
              { placeholder: '请输入区域名称', value: `UI Region ${runToken}` },
              { placeholder: '请输入区域编码', value: `ACCEPT-REG-${runToken}` },
              { placeholder: '请输入备注', value: 'Browser acceptance region.' }
            ]
          },
          {}
        )
    },
    {
      key: 'dict',
      name: 'Dictionary create',
      route: '/dict',
      scope: 'delivery',
      run: async (page) =>
        runCreateDialogScenario(
          page,
          {
            key: 'dict',
            path: '/dict',
            heading: '字典配置',
            listApi: '/api/dict/list',
            openButton: '新增',
            dialogTitle: '新增字典',
            createApi: '/api/dict',
            fields: () => [
              { placeholder: '请输入字典名称', value: `UI Dict ${runToken}` },
              { placeholder: '请输入字典编码', value: `ACCEPT_DICT_${runToken}` },
              { placeholder: '请输入备注', value: 'Browser acceptance dictionary.' }
            ]
          },
          {}
        )
    },
    {
      key: 'channel',
      name: 'Notification channel create',
      route: '/channel',
      scope: 'delivery',
      run: async (page) =>
        runCreateDialogScenario(
          page,
          {
            key: 'channel',
            path: '/channel',
            heading: '通知渠道',
            listApi: '/api/system/channel/list',
            openButton: '新增',
            dialogTitle: '新增通知渠道',
            createApi: '/api/system/channel/add',
            fields: () => [
              { placeholder: '请输入渠道名称', value: `UI Channel ${runToken}` },
              { placeholder: '请输入渠道编码', value: `accept-webhook-${runToken}` },
              { placeholder: '请输入备注', value: 'Browser acceptance channel.' }
            ]
          },
          {}
        )
    },
    {
      key: 'audit-log',
      name: 'Audit log list and detail',
      route: '/audit-log',
      scope: 'delivery',
      run: async (page) => ({
        apiResults: await openRoute(page, {
          path: '/audit-log',
          heading: '审计日志',
          api: [
            {
              matcher: (response) =>
                response.url().includes('/api/system/audit-log/list') ||
                response.url().includes('/api/system/audit-log/page'),
              label: 'audit log list'
            }
          ]
        }),
        detail: await openFirstDetailIfPresent(page, {
          key: 'audit-log',
          detailApi: /\/api\/system\/audit-log\/get\/\d+$/
        })
      })
    },
    {
      key: 'risk-monitoring',
      name: 'Real-time monitoring baseline route',
      route: '/risk-monitoring',
      scope: 'baseline',
      run: async (page) => ({
        apiResults: await openRoute(page, {
          path: '/risk-monitoring',
          heading: '实时监测',
          api: [
            {
              matcher: '/api/risk-monitoring/realtime/list',
              label: 'risk monitoring list'
            }
          ]
        })
      })
    },
    {
      key: 'risk-monitoring-gis',
      name: 'GIS monitoring baseline route',
      route: '/risk-monitoring-gis',
      scope: 'baseline',
      run: async (page) => ({
        apiResults: await openRoute(page, {
          path: '/risk-monitoring-gis',
          heading: 'GIS 风险态势',
          api: [
            {
              matcher: '/api/risk-monitoring/gis/points',
              label: 'risk monitoring gis points'
            }
          ]
        })
      })
    }
  ];
}

function buildSummary(preflightResult, scenarioResults) {
  const counts = {
    total: scenarioResults.length,
    passed: scenarioResults.filter((item) => item.status === 'passed').length,
    failed: scenarioResults.filter((item) => item.status === 'failed').length
  };

  const deliveryResults = scenarioResults.filter((item) => item.scope === 'delivery');
  const baselineResults = scenarioResults.filter((item) => item.scope === 'baseline');

  return {
    runTimestamp,
    frontendBaseUrl,
    backendBaseUrl,
    browserPath,
    headless,
    preflight: preflightResult,
    counts,
    delivery: {
      total: deliveryResults.length,
      passed: deliveryResults.filter((item) => item.status === 'passed').length,
      failed: deliveryResults.filter((item) => item.status === 'failed').length
    },
    baseline: {
      total: baselineResults.length,
      passed: baselineResults.filter((item) => item.status === 'passed').length,
      failed: baselineResults.filter((item) => item.status === 'failed').length
    },
    unhandledAsyncErrors: unhandledAsyncErrors.length ? unhandledAsyncErrors : undefined,
    output: {
      reportPath,
      summaryPath,
      detailPath,
      screenshotsDir
    }
  };
}

function buildMarkdownReport(summary, scenarioResults) {
  const lines = [
    '# Business Browser Acceptance Report',
    '',
    `- Run timestamp: \`${summary.runTimestamp}\``,
    `- Frontend: \`${summary.frontendBaseUrl}\``,
    `- Backend: \`${summary.backendBaseUrl}\``,
    `- Browser: \`${summary.browserPath}\``,
    `- Headless: \`${summary.headless}\``,
    '',
    '## Summary',
    '',
    `- Total scenarios: \`${summary.counts.total}\``,
    `- Passed: \`${summary.counts.passed}\``,
    `- Failed: \`${summary.counts.failed}\``,
    `- Delivery scope passed/failed: \`${summary.delivery.passed}\` / \`${summary.delivery.failed}\``,
    `- Baseline scope passed/failed: \`${summary.baseline.passed}\` / \`${summary.baseline.failed}\``,
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

  lines.push('', '## Files', '', `- Report: \`${reportPath}\``, `- Summary: \`${summaryPath}\``, `- Details: \`${detailPath}\``, `- Screenshots: \`${screenshotsDir}\``);
  return lines.join('\n');
}

async function writeOutputs(preflightResult, scenarioResults, summary) {
  await writeFile(
    detailPath,
    JSON.stringify(
      {
        preflight: preflightResult,
        scenarios: scenarioResults
      },
      null,
      2
    ),
    'utf8'
  );
  await writeFile(summaryPath, JSON.stringify(summary, null, 2), 'utf8');
  await writeFile(reportPath, buildMarkdownReport(summary, scenarioResults), 'utf8');
}

async function runAcceptance() {
  await ensureLogs();
  await preflightFrontendProxy();
  const preflightResult = await preflight();

  const browser = await chromium.launch({
    executablePath: browserPath,
    headless,
    args: ['--disable-dev-shm-usage']
  });

  const context = await browser.newContext({
    viewport: { width: 1600, height: 960 }
  });
  const page = await context.newPage();
  const runtime = {};
  const scenarioResults = [];

  for (const scenario of createScenarios()) {
    const startedAt = new Date().toISOString();
    try {
      if (scenario.key !== 'login') {
        await ensureScenarioLogin(page, scenario.key);
      }
      const detail = await scenario.run(page, runtime);
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
          detail: error instanceof AcceptanceError ? error.details : undefined
        })
      );
    }
  }

  await context.close();
  await browser.close();

  const summary = buildSummary(preflightResult, scenarioResults);
  await writeOutputs(preflightResult, scenarioResults, summary);

  const hasDeliveryFailures = scenarioResults.some(
    (item) => item.scope === 'delivery' && item.status !== 'passed'
  );
  if (hasDeliveryFailures) {
    process.exitCode = 1;
  }
}

await runAcceptance();
