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
- 一级导航改为固定分组并居中布局，统一适配大屏与中小屏视觉节奏
- 顶部右侧已补齐工具区（消息/帮助入口）与头像区，并显示登录账号和角色
- 顶部工具区已改为文字入口（消息通知、帮助中心），不再使用圆形图标按钮
- 右侧工具区已接入可用交互：消息与帮助入口打开下拉面板并支持常用页面跳转
- 消息/帮助面板已支持点击空白区关闭与 `Esc` 关闭，并补齐 `aria-expanded`、`aria-controls` 等可访问性属性
- 壳层头部已拆分为 `AppHeaderTools` 与 `HeaderPopoverPanel`，降低 `AppShell` 复杂度
- 消息通知增加未读计数徽标，打开通知面板后自动标记为已读
- 超窄屏（<=640px）启用头部压缩模式，头像仅保留图形位以避免拥挤
- 新增 `AppHeaderTools` 与 `HeaderPopoverPanel` 组件单测，覆盖按钮事件与弹层选择行为
- 分析报表页图表已改为“进入视口后再初始化”，减少首屏图表实例创建开销
- 全局主题 Token、Element Plus 覆盖样式和核心卡片组件已统一为浅色控制台风格
- 风险监测增强页（实时监测、GIS）及统一详情抽屉已同步到浅色控制台视觉
- 驾驶舱与风险监测图表已统一浅色主题（tooltip、坐标轴、风险等级配色）
- 增加类似 `vue-element-admin` 的最近访问标签视图
- 已接入 `Element Plus` 作为真实组件体系，核心表单、表格、描述面板和消息提示已切换
- `Element Plus` 已切换为按需自动导入，入口文件不再做全局组件注册；组件与 `v-loading` 指令会跟随页面异步 chunk 自动拆分
- 构建分包已进一步收敛为稳定共享策略：`vendor-element-core`、`vendor-element-form`、`vendor-element-table`、`vendor-element-panel` 负责承接高频 UI 依赖；图表依赖当前稳定拆分为 `vendor-echarts-core` 与 `vendor-zrender`，在保留按需引入的前提下避免了大块告警与 empty chunk
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
- 默认通过 Vite 代理访问 `http://127.0.0.1:9999`（可通过 `VITE_PROXY_TARGET` 覆盖）
- 若需要直连其他环境，可设置 `VITE_API_BASE_URL`
- 若需要调整本地代理目标，可设置 `VITE_PROXY_TARGET`
- 开发环境下建议保持 `VITE_API_BASE_URL` 为空，这样页面会优先走相对路径和 Vite 代理，避免 `localhost:5173 -> 127.0.0.1:9999` 这类跨域问题
- 登录页当前聚焦登录主流程与扫码入口，不再提供运行时接入地址输入框
- 后端默认允许 `http://localhost:*` 与 `http://127.0.0.1:*` 的最小 CORS 开发来源，便于需要直连时做浏览器联调
- 风险监测驾驶舱 `/` 当前为公开首页，未登录状态也可访问；其他受保护页面在未登录时会回退到驾驶舱
- 浏览器自动化脚本支持 `IOT_ACCEPTANCE_FRONTEND_URL`、`IOT_ACCEPTANCE_BACKEND_URL`，若未设置后端地址会回退读取 `VITE_PROXY_TARGET`（再回退 `http://127.0.0.1:9999`）

可参考：
- [docs/13-frontend-debug-console.md](../docs/13-frontend-debug-console.md)

## 环境说明
- 当前 `package.json` 已包含 Vue 3、Element Plus、ECharts、Vite 和 `unplugin-vue-components`
- 若本机 Node 版本过低，需要先升级到 Node 24+ 再安装依赖
- 当前构建配置将 `build.chunkSizeWarningLimit` 设为 `700`，用于约束稳定共享分组后的大块告警阈值
- ECharts 依赖当前拆分为 `vendor-echarts-core` 与 `vendor-zrender`，构建已消除大块告警与 empty chunk
