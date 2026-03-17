export const plannedScenarioBacklog = [
  {
    key: 'future-cockpit-kpi',
    name: '首页 KPI 数据卡片巡检',
    route: '/',
    activation: 'Phase 5 首页驾驶舱增强接口交付后启用'
  },
  {
    key: 'future-monitoring-export',
    name: '实时监测导出链路巡检',
    route: '/risk-monitoring',
    activation: '实时监测导出能力交付后启用'
  },
  {
    key: 'future-product-model',
    name: '产品物模型管理巡检',
    route: '/products',
    activation: '产品物模型增强交付后启用'
  },
  {
    key: 'future-device-binding',
    name: '设备中心增强巡检',
    route: '/devices',
    activation: '设备中心风险点绑定与运行态增强交付后启用'
  }
];

export function createExecutableScenarios({ runToken }) {
  return [
    {
      key: 'login',
      name: 'Login and auth bootstrap',
      route: '/login',
      scope: 'delivery',
      run: async ({ page, helpers }) => helpers.login(page)
    },
    {
      key: 'product-workbench',
      name: 'Product create and query',
      route: '/products',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) => {
        await helpers.openRoute(page, {
          path: '/products'
        });

        runtime.product = {
          key: `accept-ui-product-${runToken}`,
          name: `UI Product ${runToken}`
        };

        const createResult = await helpers.expectApiResponse(
          page,
          '/api/device/product/add',
          async () => {
            await page.locator('#product-key').fill(runtime.product.key);
            await page.locator('#product-name').fill(runtime.product.name);
            await page.locator('#protocol-code').fill('mqtt-json');
            await page.locator('#data-format').fill('JSON');
            await page.locator('#data-format').press('Enter');
          },
          'product add'
        );

        const productId = createResult.payload?.data?.id;
        if (!productId) {
          throw new Error('Product create response does not contain an id.');
        }
        runtime.product.id = productId;

        const queryResult = await helpers.expectApiResponse(
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
          created: runtime.product
        };
      }
    },
    {
      key: 'device-workbench',
      name: 'Device create and query',
      route: '/devices',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) => {
        if (!runtime.product?.key) {
          throw new Error('Device scenario requires a created product.');
        }

        await helpers.openRoute(page, {
          path: '/devices'
        });

        runtime.device = {
          name: `UI Device ${runToken}`,
          code: `accept-ui-device-${runToken}`,
          secret: '123456'
        };

        const createResult = await helpers.expectApiResponse(
          page,
          '/api/device/add',
          async () => {
            await page.locator('#device-product-key').fill(runtime.product.key);
            await page.locator('#device-name').fill(runtime.device.name);
            await page.locator('#device-code').fill(runtime.device.code);
            await page.locator('#device-secret').fill(runtime.device.secret);
            await page.locator('#client-id').fill(runtime.device.code);
            await page.locator('#username').fill(runtime.device.code);
            await page.locator('#password').fill(runtime.device.secret);
            await page.locator('#firmware').fill('1.0.0');
            await page.locator('#ip-address').fill('127.0.0.1');
            await page.locator('#address').fill('UI browser acceptance');
            await page.getByRole('button', { name: '提交设备建档', exact: true }).click();
          },
          'device add'
        );

        const deviceId = createResult.payload?.data?.id;
        if (!deviceId) {
          throw new Error('Device create response does not contain an id.');
        }
        runtime.device.id = deviceId;

        const queryByIdResult = await helpers.expectApiResponse(
          page,
          (response) => response.url().includes(`/api/device/${deviceId}`),
          async () => {
            await page.locator('#query-device-id').fill(String(deviceId));
            await page.getByRole('button', { name: '按 ID 查询', exact: true }).click();
          },
          'device query by id'
        );

        const queryByCodeResult = await helpers.expectApiResponse(
          page,
          (response) => response.url().includes(`/api/device/code/${runtime.device.code}`),
          async () => {
            await page.locator('#query-device-code').fill(runtime.device.code);
            await page.getByRole('button', { name: '按编码查询', exact: true }).click();
          },
          'device query by code'
        );

        return {
          apiResults: [createResult, queryByIdResult, queryByCodeResult],
          created: runtime.device
        };
      }
    },
    {
      key: 'report-workbench',
      name: 'HTTP report submission',
      route: '/reporting',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) => {
        if (!runtime.product?.key || !runtime.device?.code) {
          throw new Error('HTTP report scenario requires a created product and device.');
        }

        await helpers.openRoute(page, {
          path: '/reporting'
        });

        runtime.report = {
          topic: `/sys/${runtime.product.key}/${runtime.device.code}/thing/property/post`,
          payload: JSON.stringify({
            messageType: 'property',
            properties: {
              temperature: 26.5,
              humidity: 68
            }
          })
        };

        const reportResult = await helpers.expectApiResponse(
          page,
          '/message/http/report',
          async () => {
            await page.locator('#report-protocol').fill('mqtt-json');
            await page.locator('#report-product-key').fill(runtime.product.key);
            await page.locator('#report-device-code').fill(runtime.device.code);
            await page.locator('#report-client-id').fill(runtime.device.code);
            await page.locator('#report-tenant').fill('1');
            await page.locator('#report-topic').fill(runtime.report.topic);
            await page.locator('#payload').fill(runtime.report.payload);
            await page.getByRole('button', { name: '发送上报', exact: true }).click();
          },
          'http report'
        );

        return {
          apiResults: [reportResult],
          report: runtime.report
        };
      }
    },
    {
      key: 'device-insight',
      name: 'Device insight refresh',
      route: '/insight',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) => {
        if (!runtime.device?.code) {
          throw new Error('Insight scenario requires a created device.');
        }

        return {
          apiResults: await helpers.openRoute(page, {
            path: `/insight?deviceCode=${encodeURIComponent(runtime.device.code)}`,
            expectedPath: '/insight',
            api: [
              {
                matcher: (response) => response.url().includes(`/api/device/code/${runtime.device.code}`),
                label: 'device detail for insight'
              },
              {
                matcher: (response) =>
                  response.url().includes(`/api/device/${runtime.device.code}/properties`),
                label: 'device property snapshot'
              },
              {
                matcher: (response) =>
                  response.url().includes(`/api/device/${runtime.device.code}/message-logs`),
                label: 'device message logs'
              }
            ]
          })
        };
      }
    },
    {
      key: 'alarm-center',
      name: 'Alarm list and detail',
      route: '/alarm-center',
      scope: 'delivery',
      run: async ({ page, helpers }) => ({
        apiResults: await helpers.openRoute(page, {
          path: '/alarm-center',
          api: [
            {
              matcher: '/api/alarm/list',
              label: 'alarm list'
            }
          ]
        }),
        detail: await helpers.openFirstDetailIfPresent(page, {
          key: 'alarm-center',
          detailApi: /\/api\/alarm\/\d+$/
        })
      })
    },
    {
      key: 'event-disposal',
      name: 'Event list and detail',
      route: '/event-disposal',
      scope: 'delivery',
      run: async ({ page, helpers }) => ({
        apiResults: await helpers.openRoute(page, {
          path: '/event-disposal',
          api: [
            {
              matcher: '/api/event/list',
              label: 'event list'
            }
          ]
        }),
        detail: await helpers.openFirstDetailIfPresent(page, {
          key: 'event-disposal',
          detailApi: /\/api\/event\/\d+$/
        })
      })
    },
    {
      key: 'risk-point',
      name: 'Risk point create and bind device',
      route: '/risk-point',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) => {
        runtime.riskPoint = {
          code: `ACCEPT-RP-${runToken}`,
          name: `UI Risk Point ${runToken}`
        };

        const createResult = await helpers.runCreateDialogScenario(
          page,
          {
            key: 'risk-point',
            path: '/risk-point',
            listApi: '/api/risk-point/list',
            openButton: '新增风险点',
            dialogTitle: '新增风险点',
            createApi: '/api/risk-point/add',
            fields: () => [
              { placeholder: '请输入风险点编号', value: runtime.riskPoint.code },
              { placeholder: '请输入风险点名称', value: runtime.riskPoint.name },
              { placeholder: '请输入区域名称', value: 'Browser Region' },
              { placeholder: '请输入负责人电话', value: '13800138000' },
              { placeholder: '请输入描述', value: 'Created by browser acceptance.' }
            ],
            onCreated: () => runtime.riskPoint
          },
          runtime
        );

        const bindResult = await helpers.bindRiskPoint(page, runtime);

        return {
          apiResults: bindResult?.skipped
            ? [...createResult.apiResults]
            : [...createResult.apiResults, bindResult],
          binding: bindResult,
          created: runtime.riskPoint
        };
      }
    },
    {
      key: 'rule-definition',
      name: 'Rule definition create',
      route: '/rule-definition',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'rule-definition',
            path: '/rule-definition',
            listApi: '/api/rule-definition/list',
            openButton: '新增规则',
            dialogTitle: '新增规则',
            createApi: '/api/rule-definition/add',
            fields: () => [
              { placeholder: '请输入规则名称', value: `UI Rule ${runToken}` },
              {
                placeholder: '请输入测点标识符',
                value: runtime.riskPoint?.metricIdentifier || 'temperature'
              },
              {
                placeholder: '请输入测点名称',
                value: runtime.riskPoint?.metricName || 'Temperature'
              },
              { placeholder: '例如：value > 100', value: 'value > 20' },
              { placeholder: '请输入描述', value: 'Browser acceptance threshold rule.' }
            ]
          },
          runtime
        )
    },
    {
      key: 'linkage-rule',
      name: 'Linkage rule create',
      route: '/linkage-rule',
      scope: 'delivery',
      run: async ({ page, runtime, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'linkage-rule',
            path: '/linkage-rule',
            listApi: '/api/linkage-rule/list',
            openButton: '新增规则',
            dialogTitle: '新增规则',
            createApi: '/api/linkage-rule/add',
            fields: () => [
              { placeholder: '请输入规则名称', value: `UI Linkage ${runToken}` },
              { placeholder: '请输入描述', value: 'Browser acceptance linkage rule.' },
              {
                placeholder: '请输入触发条件（JSON格式）',
                value: JSON.stringify([
                  {
                    metricIdentifier: runtime.riskPoint?.metricIdentifier || 'temperature',
                    operator: '>',
                    threshold: 20
                  }
                ])
              },
              {
                placeholder: '请输入动作列表（JSON格式）',
                value: JSON.stringify([{ actionType: 'notify', channel: 'email' }])
              }
            ]
          },
          runtime
        )
    },
    {
      key: 'emergency-plan',
      name: 'Emergency plan create',
      route: '/emergency-plan',
      scope: 'delivery',
      run: async ({ page, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'emergency-plan',
            path: '/emergency-plan',
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
      run: async ({ page, helpers }) => ({
        apiResults: await helpers.openRoute(page, {
          path: '/report-analysis',
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
      run: async ({ page, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'organization',
            path: '/organization',
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
      run: async ({ page, runtime, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'role',
            path: '/role',
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
              runtime.role = {
                name: `UI Role ${runToken}`,
                code: `ACCEPT_ROLE_${runToken}`
              };
              return runtime.role;
            }
          },
          runtime
        )
    },
    {
      key: 'user',
      name: 'User create',
      route: '/user',
      scope: 'delivery',
      run: async ({ page, helpers }) => {
        const [userListResult] = await helpers.openRoute(page, {
          path: '/user',
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

        await helpers.fillDialogFields(page, dialog, [
          { placeholder: '请输入用户名', value: createdUser.username },
          { placeholder: '请输入真实姓名', value: createdUser.realName },
          { placeholder: '请输入手机号', value: createdUser.phone },
          { placeholder: '请输入邮箱', value: createdUser.email },
          { placeholder: '请输入密码', value: '123456' }
        ]);

        const createResult = await helpers.expectApiResponse(
          page,
          '/api/user/add',
          async () => {
            await dialog.getByRole('button', { name: '确定', exact: true }).click();
          },
          'user create'
        );

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
      run: async ({ page, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'region',
            path: '/region',
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
      run: async ({ page, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'dict',
            path: '/dict',
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
      run: async ({ page, helpers }) =>
        helpers.runCreateDialogScenario(
          page,
          {
            key: 'channel',
            path: '/channel',
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
      run: async ({ page, helpers }) => ({
        apiResults: await helpers.openRoute(page, {
          path: '/audit-log',
          api: [
            {
              matcher: (response) =>
                response.url().includes('/api/system/audit-log/list') ||
                response.url().includes('/api/system/audit-log/page'),
              label: 'audit log list'
            }
          ]
        }),
        detail: await helpers.openFirstDetailIfPresent(page, {
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
      run: async ({ page, helpers }) => ({
        apiResults: await helpers.openRoute(page, {
          path: '/risk-monitoring',
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
      run: async ({ page, helpers }) => ({
        apiResults: await helpers.openRoute(page, {
          path: '/risk-monitoring-gis',
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
