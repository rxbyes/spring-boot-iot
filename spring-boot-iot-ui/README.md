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
- 未来实验室

## 启动
1. 安装依赖：`npm install`
2. 启动开发环境：`npm run dev`
3. 构建：`npm run build`

## 联调说明
- 默认通过 Vite 代理访问 `http://localhost:9999`
- 若需要直连其他环境，可设置 `VITE_API_BASE_URL`
- 若需要调整本地代理目标，可设置 `VITE_PROXY_TARGET`

可参考：
- [docs/13-frontend-debug-console.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/13-frontend-debug-console.md)

## 环境说明
- 当前 `package.json` 采用 Vue 3 + Vite 新版本组合
- 若本机 Node 版本过低，需要先升级 Node 再安装依赖
