# spring-boot-iot-ui

`spring-boot-iot-ui` 是 `spring-boot-iot` 的独立 Vue 3 调试前端工作区。

## 设计目标
- 对接当前 Phase 1 已落地的产品、设备、HTTP 上报、属性和消息日志接口
- 以“云控制台”信息架构为参考，构建顶部双层导航 + 左侧业务菜单的公共壳层
- 为图表、数字孪生、拓扑、规则、告警、OTA 等后续能力预留前端入口

## 当前页面
- 调试驾驶舱
- 产品工作台
- 设备工作台
- HTTP 上报实验台
- 设备洞察
- 文件调试台
- 未来实验室

## 已完成增强
- 公共布局已重构为阿里云风格控制台（顶部横向导航 + 左侧分组菜单 + 右侧工作区）
- 全局主题 Token、Element Plus 覆盖样式和核心卡片组件已统一为浅色控制台风格
- 风险监测增强页（实时监测、GIS）及统一详情抽屉已同步到浅色控制台视觉
- 驾驶舱与风险监测图表已统一浅色主题（tooltip、坐标轴、风险等级配色）
- 增加类似 `vue-element-admin` 的最近访问标签视图
- 已接入 `Element Plus` 作为真实组件体系，核心表单、表格、描述面板和消息提示已切换
- `Element Plus` 已切换为按需自动导入，入口文件不再做全局组件注册；组件与 `v-loading` 指令会跟随页面异步 chunk 自动拆分
- 构建分包已进一步收敛为稳定共享策略：`vendor-element-core`、`vendor-element-form`、`vendor-element-table`、`vendor-element-panel` 负责承接高频 UI 依赖；图表依赖则拆为 `vendor-echarts-runtime`、`vendor-echarts-legend`、`vendor-echarts-trend`、`vendor-echarts-stat` 与 `vendor-zrender`，当前构建已消除循环 chunk 告警，`tooltip` 仍并入 `vendor-echarts-runtime` 以规避 ECharts 互引导致的循环分块；2026-03-16 追加验证后未保留 `title` 与 `visualMap` 的微拆分，原因分别是 `title` 收益仅约 2.41 kB 而会增加额外请求，`visualMap` 会产生 empty chunk；同日也验证过将 `@floating-ui`、`@popperjs/core`、`normalize-wheel-es`、`async-validator` 下沉到 `vendor-element-form` 会引入循环 chunk 告警，因此继续保留在 `vendor-element-core`
- 表格工作页共用的 `table / table-column / pagination / checkbox` 已并入 `vendor-element-table`，首页壳层不预加载该块，列表类页面按路由异步命中共享依赖
- 首页根入口当前仍会预加载 `vendor-element-form`，因为公共壳层中的接入配置区仍使用 `el-input / el-button`；共享壳层进一步脱离表单块仍是后续优化项
- 驾驶舱会按需加载 `trend + stat + legend + runtime`，设备洞察与详情趋势图主要加载 `trend + legend + runtime`，报表页则同时命中 `trend + stat + legend + runtime`
- 设备洞察页支持基于 `ECharts` 的数值属性趋势图
- 关键异步响应区域增加 `aria-live` 提示
- 设备洞察页的 `deviceCode` 会同步到 URL 查询参数

## 启动
1. 安装依赖：`npm install`
2. 启动开发环境：`npm run dev`
3. 构建：`npm run build`

## 联调说明
- 默认通过 Vite 代理访问 `http://localhost:9999`
- 若需要直连其他环境，可设置 `VITE_API_BASE_URL`
- 若需要调整本地代理目标，可设置 `VITE_PROXY_TARGET`
- 开发环境下建议保持 `VITE_API_BASE_URL` 为空，这样页面会优先走相对路径和 Vite 代理，避免 `localhost:5173 -> 127.0.0.1:9999` 这类跨域问题
- 顶部 `API Base URL` 输入框支持运行时切换直连地址；清空并保存后会恢复代理模式
- 后端默认允许 `http://localhost:*` 与 `http://127.0.0.1:*` 的最小 CORS 开发来源，便于需要直连时做浏览器联调
- 风险监测驾驶舱 `/` 当前为公开首页，未登录状态也可访问；其他受保护页面在未登录时会回退到驾驶舱

可参考：
- [docs/13-frontend-debug-console.md](../docs/13-frontend-debug-console.md)

## 环境说明
- 当前 `package.json` 已包含 Vue 3、Element Plus、ECharts、Vite 和 `unplugin-vue-components`
- 若本机 Node 版本过低，需要先升级到 Node 24+ 再安装依赖
- 当前构建配置将 `build.chunkSizeWarningLimit` 设为 `700`，用于约束稳定共享分组后的大块告警阈值
