#!/usr/bin/env node

import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..');

const modulePointMap = {
  env: ['ENV'],
  device: ['IOT-PRODUCT', 'IOT-DEVICE', 'INGEST-HTTP', 'TELEMETRY', 'MQTT-DOWN'],
  telemetry: ['TELEMETRY'],
  system: ['SYS-ORG', 'SYS-USER', 'SYS-ROLE', 'SYS-AUDIT']
};

const pointDependencyMap = {
  TELEMETRY: ['IOT-PRODUCT', 'IOT-DEVICE', 'INGEST-HTTP']
};

const supportedPoints = new Set([
  'ENV',
  'IOT-PRODUCT',
  'IOT-DEVICE',
  'INGEST-HTTP',
  'TELEMETRY',
  'MQTT-DOWN',
  'SYS-ORG',
  'SYS-USER',
  'SYS-ROLE',
  'SYS-AUDIT'
]);

function createStamp(date = new Date()) {
  const parts = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
    String(date.getSeconds()).padStart(2, '0')
  ];
  return parts.join('');
}

function trimText(text) {
  if (!text) {
    return '';
  }
  const normalized = String(text).replace(/\r/g, ' ').replace(/\n/g, ' ');
  return normalized.length > 260 ? `${normalized.slice(0, 260)}...` : normalized;
}

function parseArgs(argv) {
  const options = {
    baseUrl: 'http://localhost:9999',
    pointFilters: [],
    moduleFilters: []
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    const next = () => {
      const value = argv[index + 1];
      if (!value) {
        throw new Error(`Missing value for ${arg}`);
      }
      index += 1;
      return value;
    };

    if (arg === '-BaseUrl') {
      options.baseUrl = next();
      continue;
    }
    if (arg === '-PointFilter') {
      options.pointFilters.push(next().trim());
      continue;
    }
    if (arg === '-ModuleFilter') {
      options.moduleFilters.push(next().trim().toLowerCase());
      continue;
    }
    throw new Error(`Unknown argument: ${arg}`);
  }

  return options;
}

function buildJsonHeaders(authHeaders = {}) {
  return {
    'Content-Type': 'application/json; charset=utf-8',
    ...authHeaders
  };
}

function shouldRunPoint(point, selectedPoints, selectedModules) {
  if (!point) {
    return true;
  }

  if (selectedPoints.length > 0 && !selectedPoints.includes(point)) {
    for (const selectedPoint of selectedPoints) {
      const dependencies = pointDependencyMap[selectedPoint] || [];
      if (dependencies.includes(point)) {
        return true;
      }
    }
    return false;
  }

  if (selectedModules.length === 0) {
    return true;
  }

  for (const moduleCode of selectedModules) {
    const points = modulePointMap[moduleCode] || [];
    if (points.includes(point)) {
      return true;
    }
  }

  return false;
}

function idOf(response) {
  const value = response?.data?.id;
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'string' && /^\d+$/.test(value.trim())) {
    return value.trim();
  }
  return null;
}

async function writeArtifacts({ outDir, stamp, baseUrl, results }) {
  const summary = Array.from(
    results.reduce((acc, item) => {
      if (!acc.has(item.point)) {
        acc.set(item.point, []);
      }
      acc.get(item.point).push(item);
      return acc;
    }, new Map())
  )
    .map(([point, rows]) => {
      const criticalRows = rows.filter((item) => item.critical !== false);
      const effectiveRows = criticalRows.length > 0 ? criticalRows : rows;
      const criticalPass = effectiveRows.filter((item) => item.status === 'PASS').length;
      const criticalTotal = effectiveRows.length;
      return {
        point,
        criticalPass,
        criticalTotal,
        status: criticalTotal > 0 && criticalPass === criticalTotal ? 'PASS' : 'FAIL'
      };
    })
    .sort((left, right) => left.point.localeCompare(right.point, 'en'));

  const jsonPath = path.join(outDir, `business-function-smoke-${stamp}.json`);
  const summaryPath = path.join(outDir, `business-function-summary-${stamp}.json`);
  const mdPath = path.join(outDir, `business-function-report-${stamp}.md`);

  await fs.mkdir(outDir, { recursive: true });
  await fs.writeFile(jsonPath, JSON.stringify(results, null, 2), 'utf8');
  await fs.writeFile(summaryPath, JSON.stringify(summary, null, 2), 'utf8');

  const failed = results.filter((item) => item.status !== 'PASS');
  const lines = [
    '# Business Function Smoke Report',
    '',
    `- Run time: ${new Date().toISOString()}`,
    `- Base URL: ${baseUrl}`,
    '- Baseline doc: docs/21-业务功能清单与验收标准.md',
    '',
    '## Function Point Summary',
    '',
    '| Function Point | Critical Passed | Status |',
    '|---|---:|---|',
    ...summary.map((item) => `| ${item.point} | ${item.criticalPass}/${item.criticalTotal} | ${item.status} |`),
    '',
    '## Failed Cases'
  ];

  if (failed.length === 0) {
    lines.push('- All cases passed.');
  } else {
    failed.forEach((item) => {
      lines.push(`- [${item.point}] ${item.case} (${item.method} ${item.path}): ${item.detail}`);
    });
  }
  lines.push('');
  await fs.writeFile(mdPath, lines.join('\n'), 'utf8');

  return {
    jsonPath,
    summaryPath,
    mdPath,
    failedCount: failed.length
  };
}

async function main(argv = process.argv.slice(2)) {
  const options = parseArgs(argv);
  const stamp = createStamp();
  const baseUrl = options.baseUrl.replace(/\/+$/, '');
  const selectedPoints = options.pointFilters.filter(Boolean);
  const selectedModules = options.moduleFilters.filter(Boolean);
  const unsupportedPoints = selectedPoints.filter((item) => !supportedPoints.has(item));
  if (unsupportedPoints.length > 0) {
    throw new Error(
      `Unsupported smoke point filters for Node runner: ${unsupportedPoints.join(', ')}`
    );
  }

  const outDir = path.join(repoRoot, 'logs', 'acceptance');
  const results = [];
  let authHeaders = {};
  let governanceApproverId = null;

  const addResult = ({
    point,
    caseName,
    method,
    requestPath,
    critical = true,
    status = 'FAIL',
    detail = ''
  }) => {
    results.push({
      point,
      case: caseName,
      method,
      path: requestPath,
      critical,
      status,
      detail,
      at: new Date().toISOString().replace('T', ' ').slice(0, 19)
    });
  };

  const skipStep = ({
    point,
    caseName,
    method,
    requestPath,
    reason,
    critical = true
  }) => {
    if (!shouldRunPoint(point, selectedPoints, selectedModules)) {
      return;
    }
    addResult({
      point,
      caseName,
      method,
      requestPath,
      critical,
      status: 'FAIL',
      detail: `SKIP: ${reason}`
    });
  };

  const fetchJson = async ({
    method,
    requestPath,
    body,
    headers = {},
    timeoutMs = 30000
  }) => {
    const response = await fetch(`${baseUrl}${requestPath}`, {
      method,
      headers: body === undefined ? headers : buildJsonHeaders(headers),
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(timeoutMs)
    });
    const text = await response.text();
    const payload = text ? JSON.parse(text) : null;
    return {
      status: response.status,
      payload
    };
  };

  const invokeApiRaw = async ({ method, requestPath, body, timeoutMs = 30000 }) =>
    fetchJson({
      method,
      requestPath,
      body,
      headers: authHeaders,
      timeoutMs
    }).then((result) => result.payload);

  const resolveGovernanceApproverId = async () => {
    try {
      const payload = await invokeApiRaw({
        method: 'GET',
        requestPath: '/api/user/username/governance_reviewer',
        timeoutMs: 15000
      });
      return payload?.code === 200 && payload?.data?.id ? payload.data.id : null;
    } catch {
      return null;
    }
  };

  const invokeStep = async ({
    point,
    caseName,
    method,
    requestPath,
    body,
    headers = {},
    critical = true,
    timeoutMs = 30000
  }) => {
    if (!shouldRunPoint(point, selectedPoints, selectedModules)) {
      return null;
    }

    try {
      const payload = await fetchJson({
        method,
        requestPath,
        body,
        headers: {
          ...authHeaders,
          ...headers
        },
        timeoutMs
      }).then((result) => result.payload);

      if (!payload) {
        addResult({
          point,
          caseName,
          method,
          requestPath,
          critical,
          status: 'FAIL',
          detail: 'empty response'
        });
        return null;
      }

      if (Object.prototype.hasOwnProperty.call(payload, 'code')) {
        const ok = payload.code === 200;
        addResult({
          point,
          caseName,
          method,
          requestPath,
          critical,
          status: ok ? 'PASS' : 'FAIL',
          detail: `code=${payload.code}; msg=${trimText(payload.msg)}`
        });
        return ok ? payload : null;
      }

      addResult({
        point,
        caseName,
        method,
        requestPath,
        critical,
        status: 'PASS',
        detail: 'non-envelope response'
      });
      return payload;
    } catch (error) {
      addResult({
        point,
        caseName,
        method,
        requestPath,
        critical,
        status: 'FAIL',
        detail: trimText(error instanceof Error ? error.message : String(error))
      });
      return null;
    }
  };

  const governanceHeaders = () =>
    governanceApproverId
      ? { 'X-Governance-Approver-Id': String(governanceApproverId) }
      : {};

  const loginPayload = await fetchJson({
    method: 'POST',
    requestPath: '/api/auth/login',
    body: {
      username: 'admin',
      password: '123456'
    },
    timeoutMs: 15000
  }).then((result) => result.payload).catch(() => null);

  if (loginPayload?.code === 200 && loginPayload?.data?.token) {
    authHeaders = { Authorization: `Bearer ${loginPayload.data.token}` };
    addResult({
      point: 'ENV',
      caseName: 'login-token',
      method: 'POST',
      requestPath: '/api/auth/login',
      status: 'PASS',
      detail: 'login succeeded'
    });

    try {
      const mePayload = await invokeApiRaw({
        method: 'GET',
        requestPath: '/api/auth/me',
        timeoutMs: 15000
      });
      addResult({
        point: 'ENV',
        caseName: 'token-check',
        method: 'GET',
        requestPath: '/api/auth/me',
        status: mePayload?.code === 200 ? 'PASS' : 'FAIL',
        detail: mePayload?.code === 200 ? 'token valid' : 'token check failed'
      });
    } catch (error) {
      addResult({
        point: 'ENV',
        caseName: 'token-check',
        method: 'GET',
        requestPath: '/api/auth/me',
        status: 'FAIL',
        detail: trimText(error instanceof Error ? error.message : String(error))
      });
    }

    governanceApproverId = await resolveGovernanceApproverId();
    addResult({
      point: 'ENV',
      caseName: 'governance-approver-check',
      method: 'GET',
      requestPath: '/api/user/username/governance_reviewer',
      status: governanceApproverId ? 'PASS' : 'FAIL',
      detail: governanceApproverId
        ? `approverId=${governanceApproverId}`
        : 'governance reviewer missing'
    });
  } else {
    addResult({
      point: 'ENV',
      caseName: 'login-token',
      method: 'POST',
      requestPath: '/api/auth/login',
      status: 'FAIL',
      detail: 'login failed or token missing'
    });
  }

  await invokeStep({
    point: 'ENV',
    caseName: 'backend-alive',
    method: 'GET',
    requestPath: '/api/device/list'
  });

  const productKey = `accept-auto-product-${stamp}`;
  const deviceCode = `accept-auto-device-${stamp}`;
  const deviceName = `auto-device-${stamp}`;

  const productResponse = await invokeStep({
    point: 'IOT-PRODUCT',
    caseName: 'add-product',
    method: 'POST',
    requestPath: '/api/device/product/add',
    body: {
      productKey,
      productName: `auto-product-${stamp}`,
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON'
    }
  });

  const productId = idOf(productResponse);
  if (productId) {
    await invokeStep({
      point: 'IOT-PRODUCT',
      caseName: 'get-product',
      method: 'GET',
      requestPath: `/api/device/product/${productId}`
    });
  } else {
    skipStep({
      point: 'IOT-PRODUCT',
      caseName: 'get-product',
      method: 'GET',
      requestPath: '/api/device/product/{id}',
      reason: 'productId missing'
    });
  }

  const deviceResponse = await invokeStep({
    point: 'IOT-DEVICE',
    caseName: 'add-device',
    method: 'POST',
    requestPath: '/api/device/add',
    body: {
      productKey,
      deviceName,
      deviceCode,
      deviceSecret: '123456',
      clientId: deviceCode,
      username: deviceCode,
      password: '123456'
    }
  });

  const deviceId = idOf(deviceResponse);
  if (deviceId) {
    await invokeStep({
      point: 'IOT-DEVICE',
      caseName: 'get-device-by-id',
      method: 'GET',
      requestPath: `/api/device/${deviceId}`
    });
    await invokeStep({
      point: 'IOT-DEVICE',
      caseName: 'get-device-by-code',
      method: 'GET',
      requestPath: `/api/device/code/${deviceCode}`
    });
    await invokeStep({
      point: 'IOT-DEVICE',
      caseName: 'list-device-options',
      method: 'GET',
      requestPath: '/api/device/list',
      critical: false
    });
  } else {
    skipStep({
      point: 'IOT-DEVICE',
      caseName: 'get-device-by-id',
      method: 'GET',
      requestPath: '/api/device/{id}',
      reason: 'deviceId missing'
    });
    skipStep({
      point: 'IOT-DEVICE',
      caseName: 'get-device-by-code',
      method: 'GET',
      requestPath: '/api/device/code/{code}',
      reason: 'deviceId missing'
    });
  }

  await invokeStep({
    point: 'INGEST-HTTP',
    caseName: 'http-report',
    method: 'POST',
    requestPath: '/api/message/http/report',
    body: {
      protocolCode: 'mqtt-json',
      productKey,
      deviceCode,
      payload: '{"messageType":"property","properties":{"temperature":26.5,"humidity":68}}',
      topic: `/sys/${productKey}/${deviceCode}/thing/property/post`,
      clientId: deviceCode,
      tenantId: '1'
    }
  });
  await invokeStep({
    point: 'INGEST-HTTP',
    caseName: 'get-properties',
    method: 'GET',
    requestPath: `/api/device/${deviceCode}/properties`
  });
  await invokeStep({
    point: 'INGEST-HTTP',
    caseName: 'get-message-logs',
    method: 'GET',
    requestPath: `/api/device/${deviceCode}/message-logs`
  });

  if (deviceId) {
    await invokeStep({
      point: 'TELEMETRY',
      caseName: 'latest',
      method: 'GET',
      requestPath: `/api/telemetry/latest?deviceId=${deviceId}`
    });
    await invokeStep({
      point: 'TELEMETRY',
      caseName: 'history-batch',
      method: 'POST',
      requestPath: '/api/telemetry/history/batch',
      body: {
        deviceId,
        rangeCode: '1d',
        identifiers: ['temperature'],
        fillPolicy: 'zero'
      }
    });
  } else {
    skipStep({
      point: 'TELEMETRY',
      caseName: 'latest',
      method: 'GET',
      requestPath: '/api/telemetry/latest?deviceId={id}',
      reason: 'deviceId missing'
    });
    skipStep({
      point: 'TELEMETRY',
      caseName: 'history-batch',
      method: 'POST',
      requestPath: '/api/telemetry/history/batch',
      reason: 'deviceId missing'
    });
  }

  await invokeStep({
    point: 'MQTT-DOWN',
    caseName: 'publish-down',
    method: 'POST',
    requestPath: '/api/message/mqtt/down/publish',
    body: {
      productKey,
      deviceCode,
      qos: 1,
      commandType: 'property',
      params: {
        switch: 1,
        targetTemperature: 23.0,
        requestId: `auto-down-${stamp}`
      }
    }
  });

  const orgCode = `AUTO_ORG_${stamp}`;
  const orgResponse = await invokeStep({
    point: 'SYS-ORG',
    caseName: 'add-org',
    method: 'POST',
    requestPath: '/api/organization',
    body: {
      tenantId: 1,
      parentId: 0,
      orgName: 'auto-org',
      orgCode,
      orgType: 'dept',
      leaderUserId: 1,
      leaderName: 'admin',
      phone: '13800000000',
      email: 'auto-org@test.com',
      status: 1,
      sortNo: 1
    }
  });
  const orgId = idOf(orgResponse);
  await invokeStep({
    point: 'SYS-ORG',
    caseName: 'list-org',
    method: 'GET',
    requestPath: '/api/organization/list'
  });
  await invokeStep({
    point: 'SYS-ORG',
    caseName: 'tree-org',
    method: 'GET',
    requestPath: '/api/organization/tree'
  });
  if (orgId) {
    await invokeStep({
      point: 'SYS-ORG',
      caseName: 'get-org',
      method: 'GET',
      requestPath: `/api/organization/${orgId}`
    });
    await invokeStep({
      point: 'SYS-ORG',
      caseName: 'update-org',
      method: 'PUT',
      requestPath: '/api/organization',
      body: {
        id: orgId,
        tenantId: 1,
        parentId: 0,
        orgName: 'auto-org-upd',
        orgCode,
        orgType: 'dept',
        leaderUserId: 1,
        leaderName: 'admin',
        phone: '13800000000',
        email: 'auto-org@test.com',
        status: 1,
        sortNo: 2
      }
    });
    await invokeStep({
      point: 'SYS-ORG',
      caseName: 'delete-org',
      method: 'DELETE',
      requestPath: `/api/organization/${orgId}`
    });
  }

  const username = `auto_user_${stamp}`;
  await invokeStep({
    point: 'SYS-USER',
    caseName: 'add-user',
    method: 'POST',
    requestPath: '/api/user/add',
    body: {
      tenantId: 1,
      username,
      password: '123456',
      realName: 'auto-user',
      phone: `1390000${stamp.slice(-4)}`,
      email: `auto.user.${stamp}@test.com`,
      createBy: 1
    }
  });
  await invokeStep({
    point: 'SYS-USER',
    caseName: 'list-user',
    method: 'GET',
    requestPath: `/api/user/list?username=${encodeURIComponent(username)}`
  });
  const userByName = await invokeStep({
    point: 'SYS-USER',
    caseName: 'get-user-by-username',
    method: 'GET',
    requestPath: `/api/user/username/${encodeURIComponent(username)}`
  });
  const userId = userByName?.data?.id ?? null;
  if (userId) {
    await invokeStep({
      point: 'SYS-USER',
      caseName: 'get-user',
      method: 'GET',
      requestPath: `/api/user/${userId}`
    });
    await invokeStep({
      point: 'SYS-USER',
      caseName: 'update-user',
      method: 'PUT',
      requestPath: '/api/user/update',
      body: {
        id: userId,
        tenantId: 1,
        username,
        realName: 'auto-user-upd',
        phone: '13900000001',
        email: `auto.user.${stamp}@test.com`,
        status: 1,
        updateBy: 1
      }
    });
    await invokeStep({
      point: 'SYS-USER',
      caseName: 'reset-password',
      method: 'POST',
      requestPath: `/api/user/reset-password/${userId}`
    });
  }

  const roleCode = `AUTO_ROLE_${stamp}`;
  await invokeStep({
    point: 'SYS-ROLE',
    caseName: 'add-role',
    method: 'POST',
    requestPath: '/api/role/add',
    body: {
      tenantId: 1,
      roleName: 'auto-role',
      roleCode,
      description: 'auto-role-desc',
      status: 1,
      createBy: 1
    }
  });
  const roleList = await invokeStep({
    point: 'SYS-ROLE',
    caseName: 'list-role',
    method: 'GET',
    requestPath: `/api/role/list?roleCode=${encodeURIComponent(roleCode)}`
  });
  const roleId = Array.isArray(roleList?.data) && roleList.data.length > 0
    ? roleList.data[0].id
    : null;
  if (roleId) {
    await invokeStep({
      point: 'SYS-ROLE',
      caseName: 'get-role',
      method: 'GET',
      requestPath: `/api/role/${roleId}`
    });
    await invokeStep({
      point: 'SYS-ROLE',
      caseName: 'update-role',
      method: 'PUT',
      requestPath: '/api/role/update',
      body: {
        id: roleId,
        tenantId: 1,
        roleName: 'auto-role-upd',
        roleCode,
        description: 'auto-role-desc-upd',
        status: 1,
        updateBy: 1
      }
    });
    if (userId) {
      await invokeStep({
        point: 'SYS-ROLE',
        caseName: 'list-user-roles',
        method: 'GET',
        requestPath: `/api/role/user/${userId}`
      });
    } else {
      skipStep({
        point: 'SYS-ROLE',
        caseName: 'list-user-roles',
        method: 'GET',
        requestPath: '/api/role/user/{userId}',
        reason: 'userId missing'
      });
    }
    await invokeStep({
      point: 'SYS-ROLE',
      caseName: 'delete-role',
      method: 'DELETE',
      requestPath: `/api/role/${roleId}`
    });
  }

  if (userId) {
    await invokeStep({
      point: 'SYS-USER',
      caseName: 'delete-user',
      method: 'DELETE',
      requestPath: `/api/user/${userId}`
    });
  }

  await invokeStep({
    point: 'SYS-AUDIT',
    caseName: 'add-audit',
    method: 'POST',
    requestPath: '/api/system/audit-log/add',
    body: {
      tenantId: 1,
      userId: 1,
      userName: 'auto',
      operationType: 'crud',
      operationModule: 'automation',
      operationMethod: 'runBusinessSmoke',
      requestUrl: '/automation/smoke',
      requestMethod: 'POST',
      requestParams: '{}',
      responseResult: '{}',
      ipAddress: '127.0.0.1',
      location: 'local',
      operationResult: 1,
      resultMessage: 'ok',
      operationTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
    }
  });
  const auditList = await invokeStep({
    point: 'SYS-AUDIT',
    caseName: 'list-audit',
    method: 'GET',
    requestPath: '/api/system/audit-log/list?operationModule=automation'
  });
  await invokeStep({
    point: 'SYS-AUDIT',
    caseName: 'page-audit',
    method: 'GET',
    requestPath: '/api/system/audit-log/page?pageNum=1&pageSize=10'
  });
  const auditId =
    Array.isArray(auditList?.data) && auditList.data.length > 0
      ? auditList.data[0].id
      : null;
  if (auditId) {
    await invokeStep({
      point: 'SYS-AUDIT',
      caseName: 'get-audit',
      method: 'GET',
      requestPath: `/api/system/audit-log/get/${auditId}`
    });
    await invokeStep({
      point: 'SYS-AUDIT',
      caseName: 'delete-audit',
      method: 'DELETE',
      requestPath: `/api/system/audit-log/delete/${auditId}`
    });
  }

  const artifacts = await writeArtifacts({
    outDir,
    stamp,
    baseUrl,
    results
  });

  process.stdout.write(`REPORT_JSON=${artifacts.jsonPath}\n`);
  process.stdout.write(`REPORT_SUMMARY=${artifacts.summaryPath}\n`);
  process.stdout.write(`REPORT_MD=${artifacts.mdPath}\n`);
  process.stdout.write(`TOTAL_CASES=${results.length}\n`);
  process.stdout.write(`TOTAL_FAILED=${artifacts.failedCount}\n`);

  if (artifacts.failedCount > 0) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
