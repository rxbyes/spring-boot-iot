# 自动化测试问题清单（2026-03-16）

基线文档：`docs/21-business-functions-and-acceptance.md`  
执行时间：2026-03-16 21:34（Asia/Shanghai）  
执行环境：`application-dev.yml` 对应真实环境，后端 `http://127.0.0.1:9999`

## 0. 持续维护约定

1. 后续每轮测试发现的问题，统一追加到本文档，不再分散到其他临时问题文档。
2. 每次追加至少包含：测试时间、测试方式（浏览器自动化/API/手工）、问题现象、影响范围、根因判断、处理状态。
3. 已修复问题保留历史记录，并标注修复版本或修复时间，避免回归时丢失上下文。

## 1. 执行说明

1. 仓库自带脚本 `scripts/run-business-function-smoke.ps1` 依赖 PowerShell；当前环境未安装 `pwsh/powershell`，无法直接执行。
2. 为保证同等业务覆盖，采用本地 API 自动化冒烟替代执行（覆盖 ENV、IOT 产品/设备、HTTP 上报、MQTT 下发、告警、报表、系统管理）。
3. 结果文件：`/tmp/business-function-smoke-20260316213412.json`

## 2. 自动化结果总览

- 总用例：26
- 通过：25
- 失败：1
- 失败功能点：`MQTT-DOWN`

## 3. 失败问题明细

### 问题 1：MQTT 下发接口在冒烟脚本中未带 JWT，导致 401

- 用例：`MQTT-DOWN / publish-down`
- 请求：`POST /message/mqtt/down/publish`
- 现象：返回 `HTTP 401`，业务返回 `code=401`，`msg=未认证或登录已过期`
- 复现证据：`/tmp/business-function-smoke-20260316213412.json` 中该用例状态 `FAIL`
- 根因判断：当前鉴权基线要求该接口携带 Bearer token；自动化脚本按“公共接口”方式调用，未附带 `Authorization`

补充验证（同一时段）：
- 携带 JWT 后重试 `POST /message/mqtt/down/publish`，返回 `code=200`，说明服务功能可用，失败点在自动化脚本鉴权策略而非业务实现。

## 4. 建议处理

1. 修正 `scripts/run-business-function-smoke.ps1`：`/message/mqtt/down/publish` 改为携带登录后 token。
2. 在脚本中为“公共接口白名单”增加显式注释，避免后续鉴权策略变化后出现同类误判。
3. 保留本次结果文件，后续以修正版脚本复跑一次，确认 `MQTT-DOWN` 从 `FAIL` 转为 `PASS`。

## 5. 浏览器自动化专项（2026-03-16 22:01）

测试方式：前后端重启后，使用 Google Chrome + Playwright 浏览器自动化（不走 API 冒烟链路）  
执行命令：`npm run acceptance:browser`（携带 `IOT_ACCEPTANCE_BROWSER_PATH` 指向 mac Chrome）  
结果文件：

- `logs/acceptance/business-browser-summary-20260316220124.json`
- `logs/acceptance/business-browser-results-20260316220124.json`
- `logs/acceptance/business-browser-report-20260316220124.md`
- `logs/acceptance/business-browser-screenshots-20260316220124/`

### 5.1 本轮通过/失败概览

- 场景总数：21
- 通过：9
- 失败：12
- 基线页（`/risk-monitoring`、`/risk-monitoring-gis`）均通过

### 5.2 已完成的自动化优化

1. 浏览器脚本增加 macOS / Linux 浏览器路径自动识别，避免只支持 Windows 路径。
2. 产品场景提交与查询从“中文按钮文案定位”改为“输入框回车提交”，降低文案漂移与乱码对脚本的影响。
3. 路由 API 等待逻辑增加异常收敛，避免 `waitForResponse` 未处理拒绝导致整轮脚本提前崩溃。
4. 场景截图写入增加容错，截图失败不再中断整轮执行。

### 5.3 主要失败问题清单（浏览器自动化）

问题 A：产品创建步骤返回 401  
- 场景：`product-workbench`  
- 现象：`product add returned HTTP 401`  
- 影响：后续依赖产品/设备前置数据的场景级联失败  
- 初步判断：浏览器脚本的鉴权状态在进入该场景后未正确附着到请求链路，或接口鉴权策略与脚本预期不一致  
- 状态：待修复

问题 B：多页面标题断言与当前页面文案/编码不一致  
- 场景：`device-workbench`、`alarm-center`、`event-disposal`  
- 现象：等待标题文本超时（如“设备运维中心/告警中心/事件处置”）  
- 影响：页面未进入后续操作，导致对应业务点无法验证  
- 初步判断：页面文案发生变化或存在字符编码异常，导致基于中文文本的断言失效  
- 状态：待修复

问题 C：部分列表接口等待超时  
- 场景：`rule-definition`、`linkage-rule`、`emergency-plan`、`user`  
- 现象：`waitForResponse` 15s 超时  
- 影响：中后段配置与系统管理场景失败  
- 初步判断：接口实际请求路径/触发时机与脚本 matcher 不一致，或页面未触发预期请求  
- 状态：待修复

问题 D：响应体读取偶发 `Network.getResponseBody` 协议错误  
- 场景：`risk-point`、`audit-log`  
- 现象：`No resource with given identifier found`  
- 影响：即使请求可能成功，脚本仍判定失败  
- 初步判断：Playwright 在快速重载/请求完成后读取 body 存在竞态，需改为更稳健的响应解析策略  
- 状态：待修复

### 5.4 下一轮优化优先级（浏览器自动化）

1. 优先修复登录态与请求拦截链路，消除 `product-workbench` 的 401（解除级联失败）。
2. 将页面就绪判断从“标题中文文本”迁移为“稳定 DOM 标识（id/data-testid/结构锚点）”。
3. 统一梳理每个场景 API matcher，改为“主路径 + 备选路径 + 方法断言”。
4. 对 `readApiResponse` 增加 body 读取失败回退（仅用 status + url 判定），消除协议层偶发误报。

## 6. 浏览器自动化复测（2026-03-16 22:22）

测试方式：前后端重启后，使用 Google Chrome 自动化回归（不做 API 冒烟）  
说明：本轮仅记录系统问题，不修改业务源代码  
结果文件：

- `logs/acceptance/business-browser-summary-20260316222252.json`
- `logs/acceptance/business-browser-results-20260316222252.json`
- `logs/acceptance/business-browser-report-20260316222252.md`
- `logs/acceptance/business-browser-screenshots-20260316222252/`

### 6.1 结果概览

- 总场景：21
- 通过：10
- 失败：11
- 交付范围：8 通过 / 11 失败
- 基线范围（风险监测与 GIS）：2 通过 / 0 失败

与上一轮（22:01）相比：
- 失败场景从 12 降到 11
- `audit-log` 从失败恢复为通过
- 新增确认 3 个后端接口级 500 问题（告警、事件、风险点）

### 6.2 本轮系统问题（新增/仍存在）

问题 1：产品创建接口在浏览器链路中返回 401  
- 场景：`product-workbench`  
- 现象：`product add returned HTTP 401`  
- 影响：后续依赖产品/设备前置数据的场景继续级联失败  
- 状态：未修复

问题 2：设备页面标题断言超时  
- 场景：`device-workbench`  
- 现象：等待“设备运维中心”标题超时（15s）  
- 影响：设备创建/查询流程无法继续  
- 状态：未修复

问题 3：告警中心列表接口返回 500  
- 场景：`alarm-center`  
- 现象：`alarm list returned code 500`  
- 影响：告警中心场景失败  
- 状态：新增确认

问题 4：事件处置详情接口返回 500  
- 场景：`event-disposal`  
- 现象：`event-disposal detail returned code 500`  
- 影响：事件处置流程无法完成  
- 状态：新增确认

问题 5：风险点列表接口返回 500  
- 场景：`risk-point`  
- 现象：`risk-point list returned code 500`  
- 影响：风险点管理场景失败  
- 状态：新增确认

问题 6：规则、联动、预案页面接口等待超时  
- 场景：`rule-definition`、`linkage-rule`、`emergency-plan`  
- 现象：`waitForResponse` 15 秒超时  
- 影响：中段配置类能力无法自动化验收  
- 状态：未修复

问题 7：用户页面角色选项接口等待超时  
- 场景：`user`  
- 现象：`user role options wait failed`（15 秒超时）  
- 影响：用户管理链路不完整  
- 状态：未修复

### 6.3 当前可用结论

1. 前后端重启成功，`/actuator/health` 为 `UP`，前端登录页可达。
2. 风险监测与 GIS 基线在浏览器自动化下连续通过。
3. 系统仍存在“鉴权异常 + 多模块接口 500 + 部分页面请求未命中”三类主问题，需在后续轮次持续跟踪。

## 7. 浏览器自动化全流程复测（2026-03-17 00:35）

测试方式：Google Chrome + Playwright 全流程自动化（`npm run acceptance:browser`）  
执行环境：前端 `http://127.0.0.1:5174`，后端 `http://127.0.0.1:9999`（`application-dev.yml` 真实环境）

结果文件：

- `logs/acceptance/business-browser-summary-20260317003522.json`
- `logs/acceptance/business-browser-results-20260317003522.json`
- `logs/acceptance/business-browser-report-20260317003522.md`
- `logs/acceptance/business-browser-screenshots-20260317003522/`

### 7.1 本轮概览

- 总场景：21
- 通过：3
- 失败：18
- 交付范围：3 通过 / 16 失败
- 基线范围（风险监测与 GIS）：0 通过 / 2 失败

本轮通过场景：
1. `login`
2. `product-workbench`
3. `event-disposal`

### 7.2 环境阻塞与前置问题（已定位）

问题 1：后端端口不一致导致整轮自动化假失败（已定位）  
- 现象：00:24 一轮执行（`20260317002404`）中，后端指向 `19999`，但前端 Vite 代理默认转发 `9999`，导致登录链路异常，最终 0/21 通过。  
- 证据：`business-browser-summary-20260317002404.json` 显示 0 通过；`business-browser-results-20260317002404.json` 登录场景报 `POST /api/auth/login` HTTP 500。  
- 根因判断：自动化运行时后端端口与前端代理目标不一致，属于环境配置问题。  
- 状态：已在 00:35 轮修正为后端 9999 后复测。

### 7.3 本轮新增/确认失败问题（00:35）

问题 2：设备工作台标题断言超时  
- 场景：`device-workbench`  
- 现象：等待 `设备运维中心` 标题 15s 超时。  
- 影响：设备创建失败，后续 `report-workbench`、`device-insight` 前置数据缺失并级联失败。  
- 状态：未修复。

问题 3：告警中心弹窗关闭按钮被遮罩拦截，点击超时  
- 场景：`alarm-center`  
- 现象：`getByRole('button', { name: '关闭' })` 点击 30s 超时，日志显示 dialog overlay 持续拦截 pointer events。  
- 影响：告警详情交互无法闭环，场景失败。  
- 状态：未修复。

问题 4：风险点新增接口返回 HTTP 500  
- 场景：`risk-point`  
- 现象：`POST /api/risk-point/add` 返回 HTTP 500（响应体为空）。  
- 证据：`business-browser-results-20260317003522.json` 中 `risk-point.detail`。  
- 影响：风险点场景失败，且后续规则/预案相关场景缺少有效数据联动。  
- 状态：未修复。

问题 5：多模块页面统一出现标题等待超时（路由进入后未到达预期可操作态）  
- 场景：`rule-definition`、`linkage-rule`、`emergency-plan`、`report-analysis`、`organization`、`role`、`user`、`region`、`dict`、`channel`、`audit-log`、`risk-monitoring`、`risk-monitoring-gis`。  
- 现象：均为 `locator.waitFor` 15s 超时，等待各页面标题可见失败。  
- 影响：系统管理、配置能力、风险监测基线均无法自动化验收。  
- 状态：未修复。

### 7.4 下一步修复建议（供后续开发）

1. 统一前后端自动化端口配置，避免 `backendBaseUrl` 与 Vite `proxyTarget` 不一致。
2. 为关键页面提供稳定测试锚点（如 `data-testid`），替代纯中文标题断言。
3. 修复告警详情弹窗遮罩层点击穿透/关闭按钮层级问题。
4. 排查 `POST /api/risk-point/add` 的 500（后端日志 + 参数校验 + 事务回滚信息）。
5. 对级联场景增加前置失败快速终止与明确标记，降低噪声失败数量。
