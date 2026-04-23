import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const DEFAULT_LEVELS = [
  {
    name: 'blue',
    value: 0.0014,
    expectedRiskLevel: 'blue',
    expectedMonitorStatus: 'NORMAL',
    expectedAlarmDelta: 0,
    expectedEventDelta: 0,
    expectedWorkOrderDelta: 0
  },
  {
    name: 'yellow',
    value: 6.2,
    expectedRiskLevel: 'yellow',
    expectedMonitorStatus: 'ALARM',
    expectedAlarmDelta: 1,
    expectedEventDelta: 0,
    expectedWorkOrderDelta: 0
  },
  {
    name: 'orange',
    value: 12.8,
    expectedRiskLevel: 'orange',
    expectedMonitorStatus: 'ALARM',
    expectedAlarmDelta: 2,
    expectedEventDelta: 1,
    expectedWorkOrderDelta: 1
  },
  {
    name: 'red',
    value: 21.6,
    expectedRiskLevel: 'red',
    expectedMonitorStatus: 'ALARM',
    expectedAlarmDelta: 3,
    expectedEventDelta: 2,
    expectedWorkOrderDelta: 2
  }
];

const SHARED_FIXTURES = {
  'risk.full-drill.red-chain': {
    'http://127.0.0.1:10099': {
      deviceCode: 'CDXDD10099A1',
      deviceId: '2039578962673213442',
      productKey: 'nf-monitor-deep-displacement-v1',
      bindingId: '8176',
      riskPointId: '8',
      riskPointName: 'codex-drill-risk-point-a2',
      metricIdentifier: 'dispsY',
      receiveUser: 1,
      tenantId: '1',
      protocolCode: 'mqtt-json',
      levels: DEFAULT_LEVELS
    }
  },
  'risk.live-deep-displacement.real-device-red-chain': {
    'http://127.0.0.1:9999': {
      mode: 'existing_device_live',
      productKey: 'nf-monitor-deep-displacement-v1',
      metricIdentifier: 'dispsY',
      metricName: '垂直坡面方向累计变形量',
      receiveUser: 1,
      tenantId: '1',
      thresholdUnit: 'mm',
      triggerThreshold: 0.001,
      cleanupAfterRun: true,
      duplicateCooldownMinutes: 30,
      maxReportAgeMinutes: 30,
      liveTracePollIntervalMs: 15000,
      liveTraceTimeoutMs: 900000,
      expectedAlarmDelta: 1,
      expectedEventDelta: 1,
      expectedWorkOrderDelta: 1,
      candidates: [
        {
          deviceCode: 'SK00EB0D1308310',
          metricIdentifier: 'dispsY'
        },
        {
          deviceCode: 'SK00EB0D1308314',
          metricIdentifier: 'dispsY'
        },
        {
          deviceCode: '84330699',
          metricIdentifier: 'dispsY'
        },
        {
          deviceCode: '84330697',
          metricIdentifier: 'dispsY'
        }
      ],
      levels: [
        {
          name: 'red-live',
          thresholdValue: 0.001,
          expression: 'value >= 0.001',
          expectedRiskLevel: 'red',
          expectedMonitorStatus: 'ALARM',
          expectedAlarmDelta: 1,
          expectedEventDelta: 1,
          expectedWorkOrderDelta: 1
        }
      ]
    }
  }
};

function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function parseArgs(argv) {
  const options = {
    scenarioId: 'risk.full-drill.red-chain',
    backendBaseUrl: 'http://127.0.0.1:9999',
    username: 'admin',
    password: '123456',
    outputPrefix: 'risk-drill'
  };

  argv.forEach((arg) => {
    if (arg.startsWith('--scenario=')) {
      options.scenarioId = arg.slice('--scenario='.length).trim();
      return;
    }
    if (arg.startsWith('--backend-base-url=')) {
      options.backendBaseUrl = trimTrailingSlash(
        arg.slice('--backend-base-url='.length)
      );
      return;
    }
    if (arg.startsWith('--username=')) {
      options.username = arg.slice('--username='.length).trim();
      return;
    }
    if (arg.startsWith('--password=')) {
      options.password = arg.slice('--password='.length).trim();
      return;
    }
    if (arg.startsWith('--output-prefix=')) {
      options.outputPrefix = arg.slice('--output-prefix='.length).trim();
      return;
    }
    throw new Error(`Unknown argument: ${arg}`);
  });

  return options;
}

function sortByIdDesc(items = []) {
  return [...items].sort((left, right) => Number(right?.id || 0) - Number(left?.id || 0));
}

function parseTimestampMs(value) {
  if (!value) {
    return Number.NaN;
  }
  const normalized = String(value).includes('T')
    ? String(value)
    : String(value).replace(' ', 'T');
  const parsed = Date.parse(normalized);
  return Number.isFinite(parsed) ? parsed : Number.NaN;
}

export function normalizeEntityId(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value);
}

export function findBindingForDeviceMetric(bindings = [], device = {}, metric = {}) {
  const expectedDeviceId = normalizeEntityId(device?.id);
  return (
    bindings.find(
      (item) =>
        normalizeEntityId(item?.deviceId) === expectedDeviceId &&
        item?.metricIdentifier === metric?.identifier
    ) || null
  );
}

export async function resolveMetricOptionWithWarmup({
  metricIdentifier,
  loadMetrics,
  warmupMetric,
  maxAttempts = 5,
  waitMs = 1000
}) {
  if (typeof loadMetrics !== 'function') {
    throw new Error('loadMetrics must be provided.');
  }

  let warmedUp = false;
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const metrics = await loadMetrics();
    const items = Array.isArray(metrics) ? metrics : [];
    const matched = items.find((item) => item.identifier === metricIdentifier) || items[0] || null;
    if (matched?.identifier) {
      return matched;
    }

    if (warmedUp || typeof warmupMetric !== 'function') {
      continue;
    }

    await warmupMetric();
    warmedUp = true;
    if (waitMs > 0) {
      await new Promise((resolve) => setTimeout(resolve, waitMs));
    }
  }

  return null;
}

export function selectExistingDeviceLiveCandidate(
  candidateSnapshots = [],
  {
    nowText = new Date().toISOString(),
    maxReportAgeMinutes = 30,
    requiredAlarmLevel = ''
  } = {}
) {
  const nowMs = parseTimestampMs(nowText);
  const maxAgeMs = Math.max(Number(maxReportAgeMinutes) || 0, 1) * 60 * 1000;
  const normalizedAlarmLevel = String(requiredAlarmLevel || '').trim().toLowerCase();
  return (
    candidateSnapshots.find((item) => {
      const reportMs = parseTimestampMs(item?.reportTime);
      const latestValue = Number(item?.latestValue);
      const recentAlarmLevels = Array.isArray(item?.recentAlarmLevels)
        ? item.recentAlarmLevels.map((entry) => String(entry || '').trim().toLowerCase())
        : [];
      if (!item?.hasFormalMetric || !item?.metricIdentifier) {
        return false;
      }
      if (!Number.isFinite(reportMs) || !Number.isFinite(nowMs)) {
        return false;
      }
      if (Math.abs(nowMs - reportMs) > maxAgeMs) {
        return false;
      }
      if (!Number.isFinite(latestValue) || Math.abs(latestValue) <= 0) {
        return false;
      }
      if (normalizedAlarmLevel && recentAlarmLevels.includes(normalizedAlarmLevel)) {
        return false;
      }
      return true;
    }) || null
  );
}

export function summarizeRiskClosureDrill(checkpoints = []) {
  const final = checkpoints.at(-1) || {};
  return {
    finalLevel: final.level || '',
    finalRiskLevel: final.riskLevel || '',
    finalMonitorStatus: final.monitorStatus || '',
    alarmCount: final.alarmCount || 0,
    eventCount: final.eventCount || 0,
    workOrderCount: final.workOrderCount || 0,
    alarmDelta: final.alarmDelta || 0,
    eventDelta: final.eventDelta || 0,
    workOrderDelta: final.workOrderDelta || 0
  };
}

export function resolveRiskClosureFixture({
  backendBaseUrl,
  scenarioId
}) {
  const scenarioFixtures = SHARED_FIXTURES[scenarioId] || {};
  const fixture = scenarioFixtures[trimTrailingSlash(backendBaseUrl)];
  if (!fixture) {
    throw new Error(
      `No risk closure fixture is configured for ${scenarioId} at ${backendBaseUrl}`
    );
  }
  return {
    ...fixture,
    backendBaseUrl: trimTrailingSlash(backendBaseUrl)
  };
}

async function requestEnvelope(baseUrl, pathName, { method = 'GET', headers = {}, body } = {}) {
  const response = await fetch(`${trimTrailingSlash(baseUrl)}${pathName}`, {
    method,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...headers
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${pathName}: ${text}`);
  }
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code') && payload.code !== 200) {
    throw new Error(`Envelope code ${payload.code} for ${pathName}: ${payload.msg || text}`);
  }
  return payload?.data ?? payload;
}

async function login(baseUrl, username, password) {
  const payload = await requestEnvelope(baseUrl, '/api/auth/login', {
    method: 'POST',
    body: {
      username,
      password
    }
  });
  if (!payload?.token) {
    throw new Error('Login did not return token.');
  }
  return payload.token;
}

function buildAuthHeaders(token) {
  return {
    Authorization: `Bearer ${token}`
  };
}

function flattenTreeNodes(nodes = []) {
  const items = [];
  nodes.forEach((node) => {
    if (!node) {
      return;
    }
    items.push(node);
    if (Array.isArray(node.children) && node.children.length > 0) {
      items.push(...flattenTreeNodes(node.children));
    }
  });
  return items;
}

async function getFirstActiveOrganization(baseUrl, token) {
  const tree = await requestEnvelope(baseUrl, '/api/organization/tree', {
    headers: buildAuthHeaders(token)
  });
  return flattenTreeNodes(Array.isArray(tree) ? tree : []).find(
    (item) => Number(item?.status) === 1
  );
}

async function getFirstActiveRegion(baseUrl, token) {
  const list = await requestEnvelope(baseUrl, '/api/region/list', {
    headers: buildAuthHeaders(token)
  });
  return (Array.isArray(list) ? list : []).find((item) => Number(item?.status) === 1);
}

async function resolveGovernanceApproverId(baseUrl, token, username = 'governance_reviewer') {
  const encodedUsername = encodeURIComponent(username);
  const user = await requestEnvelope(baseUrl, `/api/user/username/${encodedUsername}`, {
    headers: buildAuthHeaders(token)
  });
  if (!user?.id) {
    throw new Error(`Governance approver ${username} was not found.`);
  }
  return normalizeEntityId(user.id);
}

function buildGovernedHeaders(token, approverId) {
  return {
    ...buildAuthHeaders(token),
    'X-Governance-Approver-Id': normalizeEntityId(approverId)
  };
}

async function getDeviceByCode(baseUrl, token, deviceCode) {
  const encodedDeviceCode = encodeURIComponent(deviceCode);
  return requestEnvelope(baseUrl, `/api/device/code/${encodedDeviceCode}`, {
    headers: buildAuthHeaders(token)
  });
}

async function getLatestTelemetry(baseUrl, token, deviceId) {
  const normalizedDeviceId = normalizeEntityId(deviceId);
  if (!normalizedDeviceId) {
    throw new Error('Device id is required for latest telemetry lookup.');
  }
  return requestEnvelope(baseUrl, `/api/telemetry/latest?deviceId=${encodeURIComponent(normalizedDeviceId)}`, {
    headers: buildAuthHeaders(token)
  });
}

async function listFormalMetricOptions(baseUrl, token, deviceId) {
  const normalizedDeviceId = normalizeEntityId(deviceId);
  if (!normalizedDeviceId) {
    throw new Error('Device id is unavailable for formal metric lookup.');
  }
  return requestEnvelope(baseUrl, `/api/risk-point/devices/${normalizedDeviceId}/formal-metrics`, {
    headers: buildAuthHeaders(token)
  });
}

async function listDeviceAlarms(baseUrl, token, deviceCode) {
  const encodedDeviceCode = encodeURIComponent(deviceCode);
  return requestEnvelope(baseUrl, `/api/alarm/list?deviceCode=${encodedDeviceCode}`, {
    headers: buildAuthHeaders(token)
  });
}

async function createFreshDevice(baseUrl, token, fixture, suffix) {
  return requestEnvelope(baseUrl, '/api/device/add', {
    method: 'POST',
    headers: buildAuthHeaders(token),
    body: {
      productKey: fixture.productKey,
      deviceName: `Codex drill ${suffix}`,
      deviceCode: `CDXACC${suffix}`,
      deviceSecret: '123456',
      clientId: `CDXACC${suffix}`,
      username: `CDXACC${suffix}`,
      password: '123456'
    }
  });
}

async function listMetricOptions(baseUrl, token, deviceId) {
  const normalizedDeviceId = normalizeEntityId(deviceId);
  if (!normalizedDeviceId) {
    throw new Error('Device id is unavailable for metric lookup.');
  }
  return requestEnvelope(baseUrl, `/api/device/${normalizedDeviceId}/metrics`, {
    headers: buildAuthHeaders(token)
  });
}

async function getMetricOption(baseUrl, token, deviceId, metricIdentifier, warmupFixture) {
  return resolveMetricOptionWithWarmup({
    metricIdentifier,
    loadMetrics: async () => listMetricOptions(baseUrl, token, deviceId),
    warmupMetric:
      warmupFixture == null
        ? undefined
        : async () => {
            await sendMetricReport(
              baseUrl,
              warmupFixture,
              warmupFixture.warmupMetricValue
            );
          }
  });
}

async function createFreshRiskPoint(baseUrl, token, organization, region, suffix) {
  return requestEnvelope(baseUrl, '/api/risk-point/add', {
    method: 'POST',
    headers: buildAuthHeaders(token),
    body: {
      riskPointName: `codex-registry-risk-${suffix}`,
      orgId: organization.id,
      regionId: region.id,
      responsibleUser: 1,
      responsiblePhone: '13800000000',
      riskPointLevel: 'level_1',
      status: 0,
      tenantId: 1
    }
  });
}

async function bindRiskPointDevice(baseUrl, token, riskPoint, device, metric, fixture = {}) {
  await requestEnvelope(baseUrl, '/api/risk-point/bind-device', {
    method: 'POST',
    headers: buildAuthHeaders(token),
    body: {
      riskPointId: riskPoint.id,
      deviceId: device.id,
      deviceCode: device.deviceCode,
      deviceName: device.deviceName,
      riskMetricId: metric.riskMetricId,
      metricIdentifier: metric.identifier,
      metricName: metric.name,
      defaultThreshold: String(
        fixture.triggerThreshold ?? fixture.defaultThreshold ?? 20
      ),
      thresholdUnit: fixture.thresholdUnit || 'mm'
    }
  });
  const bindings = await requestEnvelope(
    baseUrl,
    `/api/risk-point/bound-devices/${riskPoint.id}`,
    {
      headers: buildAuthHeaders(token)
    }
  );
  const bindingItems = Array.isArray(bindings) ? bindings : [];
  return findBindingForDeviceMetric(bindingItems, device, metric);
}

async function provisionFreshFixture(baseUrl, token, templateFixture) {
  const suffix = Date.now().toString().slice(-10);
  const organization = await getFirstActiveOrganization(baseUrl, token);
  const region = await getFirstActiveRegion(baseUrl, token);
  if (!organization) {
    throw new Error('No active organization is available for risk drill provisioning.');
  }
  if (!region) {
    throw new Error('No active region is available for risk drill provisioning.');
  }

  const device = await createFreshDevice(baseUrl, token, templateFixture, suffix);
  const warmupMetricValue = templateFixture.levels?.[0]?.value ?? 0.0014;
  const metric = await getMetricOption(
    baseUrl,
    token,
    device.id,
    templateFixture.metricIdentifier,
    {
      ...templateFixture,
      deviceCode: device.deviceCode,
      metricIdentifier: templateFixture.metricIdentifier,
      warmupMetricValue
    }
  );
  if (!metric?.identifier) {
    throw new Error(
      `Metric ${templateFixture.metricIdentifier} is unavailable for device ${device.deviceCode}`
    );
  }

  const riskPoint = await createFreshRiskPoint(
    baseUrl,
    token,
    organization,
    region,
    suffix
  );
  const binding = await bindRiskPointDevice(
    baseUrl,
    token,
    riskPoint,
    device,
    metric
  );
  if (!binding?.id) {
    throw new Error('Risk point binding was not created.');
  }

  return {
    ...templateFixture,
    bindingId: normalizeEntityId(binding.id),
    deviceId: normalizeEntityId(device.id),
    deviceCode: device.deviceCode,
    deviceName: device.deviceName,
    riskPointId: normalizeEntityId(riskPoint.id),
    riskPointCode: riskPoint.riskPointCode,
    riskPointName: riskPoint.riskPointName,
    metricIdentifier: metric.identifier,
    metricName: metric.name,
    orgId: normalizeEntityId(organization.id),
    orgName: organization.orgName,
    regionId: normalizeEntityId(region.id),
    regionName: region.regionName
  };
}

async function resolveExistingDeviceLiveCandidate(baseUrl, token, templateFixture) {
  const candidateSnapshots = [];
  const duplicateCooldownMinutes = Number(templateFixture.duplicateCooldownMinutes || 30);
  const cooldownCutoffMs = Date.now() - duplicateCooldownMinutes * 60 * 1000;
  for (const candidate of templateFixture.candidates || []) {
    try {
      const device = await getDeviceByCode(baseUrl, token, candidate.deviceCode);
      if (!device?.id) {
        continue;
      }
      const latest = await getLatestTelemetry(baseUrl, token, device.id);
      const metricOptions = await listFormalMetricOptions(baseUrl, token, device.id);
      const alarms = await listDeviceAlarms(baseUrl, token, candidate.deviceCode);
      const selectedMetric = (Array.isArray(metricOptions) ? metricOptions : []).find(
        (item) => item?.identifier === (candidate.metricIdentifier || templateFixture.metricIdentifier)
      ) || null;
      const latestValue = selectedMetric?.identifier
        ? latest?.properties?.[selectedMetric.identifier]
        : undefined;
      const recentAlarmLevels = (Array.isArray(alarms) ? alarms : [])
        .filter((item) => {
          const triggerMs = parseTimestampMs(item?.triggerTime);
          return Number.isFinite(triggerMs) && triggerMs >= cooldownCutoffMs;
        })
        .map((item) => item?.alarmLevel)
        .filter(Boolean);
      candidateSnapshots.push({
        ...candidate,
        device,
        latest,
        metric: selectedMetric,
        reportTime: latest?.reportTime,
        traceId: latest?.traceId,
        metricIdentifier: selectedMetric?.identifier || candidate.metricIdentifier || '',
        hasFormalMetric: Boolean(selectedMetric?.identifier),
        latestValue,
        recentAlarmLevels
      });
    } catch (error) {
      candidateSnapshots.push({
        ...candidate,
        hasFormalMetric: false,
        error: error instanceof Error ? error.message : String(error)
      });
    }
  }

  const selected = selectExistingDeviceLiveCandidate(candidateSnapshots, {
    maxReportAgeMinutes: templateFixture.maxReportAgeMinutes,
    requiredAlarmLevel: 'red'
  });
  if (selected) {
    return selected;
  }

  throw new Error(
    `No eligible live device candidate was found: ${JSON.stringify(
      candidateSnapshots.map((item) => ({
        deviceCode: item.deviceCode,
        metricIdentifier: item.metricIdentifier,
        reportTime: item.reportTime || '',
        hasFormalMetric: Boolean(item.hasFormalMetric),
        latestValue: item.latestValue ?? null,
        error: item.error || ''
      }))
    )}`
  );
}

async function createGovernedRuleDefinition(baseUrl, token, approverId, fixture, candidate, suffix) {
  const level = fixture.levels?.[0] || {};
  return requestEnvelope(baseUrl, '/api/rule-definition/add', {
    method: 'POST',
    headers: buildGovernedHeaders(token, approverId),
    body: {
      ruleName: `codex-live-rule-${suffix}`,
      riskMetricId: candidate.metric?.riskMetricId,
      metricIdentifier: candidate.metric?.identifier || fixture.metricIdentifier,
      metricName: candidate.metric?.name || fixture.metricName || fixture.metricIdentifier,
      expression: level.expression || `value >= ${fixture.triggerThreshold ?? 0.001}`,
      duration: 0,
      alarmLevel: 'red',
      notificationMethods: 'in_app',
      convertToEvent: 1,
      status: 0,
      tenantId: Number(fixture.tenantId || 1)
    }
  });
}

async function createGovernedLinkageRule(baseUrl, token, approverId, fixture, candidate, suffix) {
  const level = fixture.levels?.[0] || {};
  const thresholdValue = Number(level.thresholdValue ?? fixture.triggerThreshold ?? 0.001);
  return requestEnvelope(baseUrl, '/api/linkage-rule/add', {
    method: 'POST',
    headers: buildGovernedHeaders(token, approverId),
    body: {
      ruleName: `codex-live-link-${suffix}`,
      description: `live deep displacement linkage for ${candidate.device?.deviceCode} ${candidate.metric?.identifier || fixture.metricIdentifier}`,
      triggerCondition: JSON.stringify({
        metricIdentifier: candidate.metric?.identifier || fixture.metricIdentifier,
        operator: '>=',
        value: thresholdValue
      }),
      actionList: JSON.stringify([
        {
          type: 'notify',
          channel: 'in_app',
          metricIdentifier: candidate.metric?.identifier || fixture.metricIdentifier
        }
      ]),
      status: 0,
      tenantId: Number(fixture.tenantId || 1)
    }
  });
}

async function createGovernedEmergencyPlan(baseUrl, token, approverId, fixture, candidate, suffix) {
  const metricIdentifier = candidate.metric?.identifier || fixture.metricIdentifier;
  return requestEnvelope(baseUrl, '/api/emergency-plan/add', {
    method: 'POST',
    headers: buildGovernedHeaders(token, approverId),
    body: {
      planName: `codex-live-plan-${suffix}-${metricIdentifier}`,
      alarmLevel: 'red',
      description: `deep displacement ${metricIdentifier} emergency for ${candidate.device?.deviceCode}`,
      responseSteps: JSON.stringify([
        {
          step: 1,
          action: `check ${metricIdentifier} live device`
        }
      ]),
      contactList: JSON.stringify([
        {
          name: 'admin',
          phone: '13800000000'
        }
      ]),
      status: 0,
      tenantId: Number(fixture.tenantId || 1)
    }
  });
}

async function provisionExistingDeviceLiveFixture(baseUrl, token, templateFixture) {
  const suffix = Date.now().toString().slice(-10);
  const organization = await getFirstActiveOrganization(baseUrl, token);
  const region = await getFirstActiveRegion(baseUrl, token);
  if (!organization) {
    throw new Error('No active organization is available for live risk drill provisioning.');
  }
  if (!region) {
    throw new Error('No active region is available for live risk drill provisioning.');
  }

  const candidate = await resolveExistingDeviceLiveCandidate(baseUrl, token, templateFixture);
  const riskPoint = await createFreshRiskPoint(baseUrl, token, organization, region, suffix);
  const binding = await bindRiskPointDevice(
    baseUrl,
    token,
    riskPoint,
    candidate.device,
    candidate.metric,
    templateFixture
  );
  if (!binding?.id) {
    throw new Error('Live device risk point binding was not created.');
  }

  const approverId = await resolveGovernanceApproverId(baseUrl, token);
  const rule = await createGovernedRuleDefinition(baseUrl, token, approverId, templateFixture, candidate, suffix);
  const linkageRule = await createGovernedLinkageRule(baseUrl, token, approverId, templateFixture, candidate, suffix);
  const emergencyPlan = await createGovernedEmergencyPlan(baseUrl, token, approverId, templateFixture, candidate, suffix);

  return {
    ...templateFixture,
    deviceId: normalizeEntityId(candidate.device?.id),
    deviceCode: candidate.device?.deviceCode,
    deviceName: candidate.device?.deviceName,
    productKey: candidate.device?.productKey || templateFixture.productKey,
    metricIdentifier: candidate.metric?.identifier || templateFixture.metricIdentifier,
    metricName: candidate.metric?.name || templateFixture.metricName || templateFixture.metricIdentifier,
    riskMetricId: normalizeEntityId(candidate.metric?.riskMetricId),
    bindingId: normalizeEntityId(binding.id),
    riskPointId: normalizeEntityId(riskPoint.id),
    riskPointCode: riskPoint.riskPointCode,
    riskPointName: riskPoint.riskPointName,
    orgId: normalizeEntityId(organization.id),
    orgName: organization.orgName,
    regionId: normalizeEntityId(region.id),
    regionName: region.regionName,
    baselineTraceId: candidate.traceId || '',
    baselineReportTime: candidate.reportTime || '',
    baselineValue: candidate.latestValue ?? null,
    ruleId: normalizeEntityId(rule?.id),
    ruleName: rule?.ruleName || '',
    linkageRuleId: normalizeEntityId(linkageRule?.id),
    linkageRuleName: linkageRule?.ruleName || '',
    emergencyPlanId: normalizeEntityId(emergencyPlan?.id),
    emergencyPlanName: emergencyPlan?.planName || ''
  };
}

async function waitForNextLiveTrace(baseUrl, token, fixture) {
  const timeoutMs = Number(fixture.liveTraceTimeoutMs || 900000);
  const pollIntervalMs = Number(fixture.liveTracePollIntervalMs || 15000);
  const startedAt = Date.now();
  while (Date.now() - startedAt <= timeoutMs) {
    const latest = await getLatestTelemetry(baseUrl, token, fixture.deviceId);
    const nextTraceId = latest?.traceId || '';
    const nextReportTime = latest?.reportTime || '';
    if (
      (nextTraceId && nextTraceId !== fixture.baselineTraceId) ||
      (nextReportTime && nextReportTime !== fixture.baselineReportTime)
    ) {
      return latest;
    }
    await new Promise((resolve) => setTimeout(resolve, pollIntervalMs));
  }

  throw new Error(
    `Timed out waiting for next live trace for ${fixture.deviceCode}; baselineTrace=${fixture.baselineTraceId}, baselineReportTime=${fixture.baselineReportTime}`
  );
}

async function fetchCurrentCounts(baseUrl, token, fixture) {
  const authHeaders = buildAuthHeaders(token);
  const alarms = await requestEnvelope(
    baseUrl,
    `/api/alarm/list?deviceCode=${encodeURIComponent(fixture.deviceCode)}`,
    {
      headers: authHeaders
    }
  );
  const events = await requestEnvelope(
    baseUrl,
    `/api/event/list?deviceCode=${encodeURIComponent(fixture.deviceCode)}`,
    {
      headers: authHeaders
    }
  );
  const workOrders = await requestEnvelope(
    baseUrl,
    `/api/event/work-orders?receiveUser=${fixture.receiveUser}`,
    {
      headers: authHeaders
    }
  );
  const alarmItems = Array.isArray(alarms) ? alarms : [];
  const eventItems = Array.isArray(events) ? events : [];
  const relevantEventCodes = new Set(
    eventItems.map((item) => item.eventCode).filter(Boolean)
  );
  const workOrderItems = Array.isArray(workOrders)
    ? workOrders.filter((item) => relevantEventCodes.has(item.eventCode))
    : [];
  return {
    alarmCount: alarmItems.length,
    eventCount: eventItems.length,
    workOrderCount: workOrderItems.length
  };
}

async function cleanupRiskDrillArtifacts(baseUrl, token, fixture) {
  const result = {
    attempted: Boolean(fixture?.cleanupAfterRun),
    removed: [],
    failed: []
  };
  if (!fixture?.cleanupAfterRun) {
    return result;
  }

  const directHeaders = buildAuthHeaders(token);
  let governedHeaders = null;
  try {
    const approverId = await resolveGovernanceApproverId(baseUrl, token);
    governedHeaders = buildGovernedHeaders(token, approverId);
  } catch (error) {
    result.failed.push(
      `resolve-governance-approver: ${error instanceof Error ? error.message : String(error)}`
    );
  }

  async function tryCleanup(name, pathName, options = {}) {
    try {
      await requestEnvelope(baseUrl, pathName, options);
      result.removed.push(name);
    } catch (error) {
      result.failed.push(
        `${name}: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  }

  if (fixture.riskPointId && fixture.deviceId) {
    await tryCleanup(
      'unbind-device',
      `/api/risk-point/unbind-device?riskPointId=${encodeURIComponent(fixture.riskPointId)}&deviceId=${encodeURIComponent(fixture.deviceId)}`,
      {
        method: 'POST',
        headers: directHeaders
      }
    );
  }
  if (fixture.ruleId && governedHeaders) {
    await tryCleanup(`delete-rule:${fixture.ruleId}`, `/api/rule-definition/delete/${encodeURIComponent(fixture.ruleId)}`, {
      method: 'POST',
      headers: governedHeaders
    });
  }
  if (fixture.linkageRuleId && governedHeaders) {
    await tryCleanup(
      `delete-linkage-rule:${fixture.linkageRuleId}`,
      `/api/linkage-rule/delete/${encodeURIComponent(fixture.linkageRuleId)}`,
      {
        method: 'POST',
        headers: governedHeaders
      }
    );
  }
  if (fixture.emergencyPlanId && governedHeaders) {
    await tryCleanup(
      `delete-emergency-plan:${fixture.emergencyPlanId}`,
      `/api/emergency-plan/delete/${encodeURIComponent(fixture.emergencyPlanId)}`,
      {
        method: 'POST',
        headers: governedHeaders
      }
    );
  }
  if (fixture.riskPointId) {
    await tryCleanup(`delete-risk-point:${fixture.riskPointId}`, `/api/risk-point/delete/${encodeURIComponent(fixture.riskPointId)}`, {
      method: 'POST',
      headers: directHeaders
    });
  }

  return result;
}

async function sendMetricReport(baseUrl, fixture, value) {
  const payload = {
    protocolCode: fixture.protocolCode,
    productKey: fixture.productKey,
    deviceCode: fixture.deviceCode,
    payload: JSON.stringify({
      messageType: 'property',
      properties: {
        [fixture.metricIdentifier]: value
      }
    }),
    topic: `/sys/${fixture.productKey}/${fixture.deviceCode}/thing/property/post`,
    clientId: fixture.deviceCode,
    tenantId: fixture.tenantId
  };
  await requestEnvelope(baseUrl, '/api/message/http/report', {
    method: 'POST',
    body: payload
  });
}

async function fetchCurrentSnapshot(baseUrl, token, fixture, baseline) {
  const authHeaders = buildAuthHeaders(token);
  const list = fixture.bindingId
    ? null
    : await requestEnvelope(
        baseUrl,
        `/api/risk-monitoring/realtime/list?deviceCode=${encodeURIComponent(fixture.deviceCode)}&pageNum=1&pageSize=10`,
        {
          headers: authHeaders
        }
      );
  const listItems =
    Array.isArray(list?.rows) ? list.rows : Array.isArray(list?.list) ? list.list : [];
  const listItem =
    listItems.find((item) => item.deviceCode === fixture.deviceCode) || listItems[0];
  const bindingId = fixture.bindingId || listItem?.bindingId;
  if (!bindingId) {
    throw new Error(`Risk monitoring binding was not found for ${fixture.deviceCode}`);
  }

  const detail = await requestEnvelope(baseUrl, `/api/risk-monitoring/realtime/${bindingId}`, {
    headers: authHeaders
  });
  const alarms = await requestEnvelope(
    baseUrl,
    `/api/alarm/list?deviceCode=${encodeURIComponent(fixture.deviceCode)}`,
    {
      headers: authHeaders
    }
  );
  const events = await requestEnvelope(
    baseUrl,
    `/api/event/list?deviceCode=${encodeURIComponent(fixture.deviceCode)}`,
    {
      headers: authHeaders
    }
  );
  const workOrders = await requestEnvelope(
    baseUrl,
    `/api/event/work-orders?receiveUser=${fixture.receiveUser}`,
    {
      headers: authHeaders
    }
  );

  const alarmItems = Array.isArray(alarms) ? alarms : [];
  const eventItems = Array.isArray(events) ? events : [];
  const relevantEventCodes = new Set(
    eventItems.map((item) => item.eventCode).filter(Boolean)
  );
  const workOrderItems = Array.isArray(workOrders)
    ? workOrders.filter((item) => relevantEventCodes.has(item.eventCode))
    : [];
  const latestAlarm = sortByIdDesc(alarmItems)[0] || null;
  const latestEvent = sortByIdDesc(eventItems)[0] || null;

  return {
    bindingId,
    riskLevel:
      detail?.riskLevel ||
      detail?.currentRiskLevel ||
      listItem?.riskLevel ||
      latestEvent?.riskLevel ||
      latestAlarm?.alarmLevel ||
      '',
    monitorStatus: detail?.monitorStatus || listItem?.monitorStatus || '',
    alarmCount: alarmItems.length,
    eventCount: eventItems.length,
    workOrderCount: workOrderItems.length,
    alarmDelta: alarmItems.length - baseline.alarmCount,
    eventDelta: eventItems.length - baseline.eventCount,
    workOrderDelta: workOrderItems.length - baseline.workOrderCount,
    latestAlarm,
    latestEvent
  };
}

async function waitForCheckpoint(baseUrl, token, fixture, levelConfig, baseline) {
  let lastSnapshot = null;
  for (let attempt = 0; attempt < 20; attempt += 1) {
    lastSnapshot = await fetchCurrentSnapshot(baseUrl, token, fixture, baseline);
    if (
      lastSnapshot.riskLevel === levelConfig.expectedRiskLevel &&
      lastSnapshot.monitorStatus === levelConfig.expectedMonitorStatus &&
      lastSnapshot.alarmDelta >= levelConfig.expectedAlarmDelta &&
      lastSnapshot.eventDelta >= levelConfig.expectedEventDelta &&
      lastSnapshot.workOrderDelta >= levelConfig.expectedWorkOrderDelta
    ) {
      return lastSnapshot;
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }

  throw new Error(
    `Risk checkpoint ${levelConfig.name} did not converge. Last snapshot: ${JSON.stringify(lastSnapshot)}`
  );
}

function createMarkdown({
  runId,
  fixture,
  checkpoints,
  summary
}) {
  const lines = [
    '# Risk Closure Drill',
    '',
    `- Run ID: \`${runId}\``,
    `- Device: \`${fixture.deviceCode}\``,
    `- Product: \`${fixture.productKey}\``,
    `- Binding: \`${fixture.bindingId}\``,
    `- Metric: \`${fixture.metricIdentifier}\``,
    fixture.ruleId ? `- Rule: \`${fixture.ruleId}\`` : '',
    fixture.linkageRuleId ? `- Linkage Rule: \`${fixture.linkageRuleId}\`` : '',
    fixture.emergencyPlanId ? `- Emergency Plan: \`${fixture.emergencyPlanId}\`` : '',
    '',
    '## Replay Result',
    ''
  ].filter(Boolean);

  checkpoints.forEach((checkpoint) => {
    lines.push(`- ${checkpoint.level} \`${checkpoint.value}\`:`);
    lines.push(`  - monitoring: \`${checkpoint.monitorStatus} / ${checkpoint.riskLevel}\``);
    lines.push(`  - alarms: \`${checkpoint.alarmCount}\``);
    lines.push(`  - events: \`${checkpoint.eventCount}\``);
    lines.push(`  - work orders: \`${checkpoint.workOrderCount}\``);
  });

  lines.push('', '## Summary', '', `- final risk: \`${summary.finalRiskLevel}\``, `- final status: \`${summary.finalMonitorStatus}\``);
  return lines.join('\n');
}

export async function runRiskClosureDrill({
  workspaceRoot = process.cwd(),
  scenarioId = 'risk.full-drill.red-chain',
  backendBaseUrl = 'http://127.0.0.1:9999',
  username = 'admin',
  password = '123456',
  outputPrefix = 'risk-drill'
} = {}) {
  const templateFixture = resolveRiskClosureFixture({
    backendBaseUrl,
    scenarioId
  });
  const token = await login(backendBaseUrl, username, password);
  let fixture = null;
  let baseline = {
    alarmCount: 0,
    eventCount: 0,
    workOrderCount: 0
  };
  let checkpoints = [];
  let summary = null;
  let capturedError = null;
  let cleanup = {
    attempted: false,
    removed: [],
    failed: []
  };

  try {
    fixture =
      templateFixture.mode === 'existing_device_live'
        ? await provisionExistingDeviceLiveFixture(
            backendBaseUrl,
            token,
            templateFixture
          )
        : await provisionFreshFixture(backendBaseUrl, token, templateFixture);
    baseline =
      templateFixture.mode === 'existing_device_live'
        ? await fetchCurrentCounts(backendBaseUrl, token, fixture)
        : baseline;

    checkpoints = [];
    for (const levelConfig of fixture.levels || DEFAULT_LEVELS) {
      let checkpointValue = levelConfig.value;
      let liveTraceId = '';
      let liveReportTime = '';
      if (templateFixture.mode === 'existing_device_live') {
        const latest = await waitForNextLiveTrace(backendBaseUrl, token, fixture);
        checkpointValue = latest?.properties?.[fixture.metricIdentifier] ?? checkpointValue;
        liveTraceId = latest?.traceId || '';
        liveReportTime = latest?.reportTime || '';
      } else {
        await sendMetricReport(backendBaseUrl, fixture, levelConfig.value);
      }
      const snapshot = await waitForCheckpoint(
        backendBaseUrl,
        token,
        fixture,
        levelConfig,
        baseline
      );
      checkpoints.push({
        level: levelConfig.name,
        value: checkpointValue,
        traceId: liveTraceId,
        reportTime: liveReportTime,
        ...snapshot
      });
    }

    summary = summarizeRiskClosureDrill(checkpoints);
  } catch (error) {
    capturedError = error instanceof Error ? error : new Error(String(error));
  } finally {
    if (fixture?.cleanupAfterRun) {
      cleanup = await cleanupRiskDrillArtifacts(backendBaseUrl, token, fixture);
    }
  }

  if (capturedError) {
    throw capturedError;
  }

  const runId = `${outputPrefix}-${Date.now()}`;
  const logsDir = path.join(workspaceRoot, 'logs', 'acceptance');
  const jsonPath = path.join(logsDir, `${runId}.json`);
  const mdPath = path.join(logsDir, `${runId}.md`);

  await fs.mkdir(logsDir, { recursive: true });
  await fs.writeFile(
    jsonPath,
    JSON.stringify(
      {
        runId,
        scenarioId,
        fixture,
        baseline,
        checkpoints,
        summary,
        cleanup
      },
      null,
      2
    ),
    'utf8'
  );
  await fs.writeFile(
    mdPath,
    createMarkdown({
      runId,
      fixture,
      checkpoints,
      summary
    }),
    'utf8'
  );

  return {
    scenarioId,
    runnerType: 'riskDrill',
    status: summary.finalRiskLevel === 'red' ? 'passed' : 'failed',
    blocking: 'blocker',
    summary: `Final risk ${summary.finalRiskLevel} / ${summary.finalMonitorStatus}`,
    evidenceFiles: [jsonPath, mdPath],
    details: {
      runId,
      summary,
      baseline,
      cleanup
    }
  };
}

const currentFilePath = fileURLToPath(import.meta.url);

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(currentFilePath)
) {
  try {
    const options = parseArgs(process.argv.slice(2));
    const result = await runRiskClosureDrill(options);
    process.stdout.write(`JSON_PATH=${result.evidenceFiles[0]}\n`);
    process.stdout.write(`MD_PATH=${result.evidenceFiles[1]}\n`);
    process.stdout.write(`SUMMARY=${result.summary}\n`);
    process.exitCode = result.status === 'passed' ? 0 : 1;
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.stack || error.message : String(error)}\n`
    );
    process.exitCode = 1;
  }
}
