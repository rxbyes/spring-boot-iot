# spring-boot-iot-ui

`spring-boot-iot-ui` 是 `spring-boot-iot` 的独立 Vue 3 调试前端工作区。

## 设计目标
- 对接当前 Phase 1 已落地的产品、设备、HTTP 上报、属性和消息日志接口
- 以 `vue-element-admin` 的后台信息架构为参考，重构为更适合物联网联调的工作台
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
- 增加类似 `vue-element-admin` 的最近访问标签视图
- 已接入 `Element Plus` 作为真实组件体系，核心表单、表格、描述面板和消息提示已切换
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
- [docs/13-frontend-debug-console.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/13-frontend-debug-console.md)

## 环境说明
- 当前 `package.json` 已包含 Vue 3、Element Plus、ECharts 和 Vite 新版本组合
- 若本机 Node 版本过低，需要先升级到 Node 20.19+ 再安装依赖
