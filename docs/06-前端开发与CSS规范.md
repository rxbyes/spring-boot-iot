# 06 前端开发与 CSS 规范

更新时间：2026-03-18

## 1. 技术栈与目录

- 框架：Vue 3 + TypeScript + Vite。
- UI：Element Plus。
- 图表：ECharts。
- 关键目录：
  - `spring-boot-iot-ui/src/views`
  - `spring-boot-iot-ui/src/components`
  - `spring-boot-iot-ui/src/api`
  - `spring-boot-iot-ui/src/router`
  - `spring-boot-iot-ui/src/styles`

## 2. 前端架构基线

### 2.1 鉴权与请求

- 请求入口：`src/api/request.ts`。
- 拦截器：`src/api/interceptors.ts`。
- 行为基线：
  - 自动注入 `Authorization: Bearer <token>`。
  - 当后端返回 `code=401`，清理登录态并跳转 `/login`。
  - 统一错误提示与空数据兜底。

### 2.2 路由与权限

- 路由定义：`src/router/index.ts`。
- 登录页：`/login`。
- 除登录页外，业务页面默认要求鉴权。
- 菜单与按钮权限来源：后端动态菜单与权限编码。

## 3. 页面开发统一模式

根据现有共享能力，新增页面优先复用以下标准件：

1. `PanelCard`：卡片容器与页内分区。
2. `StandardPagination`：统一分页组件。
3. `useServerPagination`：统一分页状态与翻页行为。
4. 全局列表样式（`standard-list-view` 体系）。
5. 统一详情抽屉：`StandardDetailDrawer`。
6. 统一表单抽屉：`StandardFormDrawer`。

禁止：
- 在同类列表页重复造分页样式和状态管理。
- 同时存在“本地散装分页”与“标准分页契约”的双轨实现。

## 4. CSS 与视觉 Token 规范

### 4.1 Token 文件

- `src/styles/tokens.css`：颜色、字体、圆角、阴影、间距、过渡。
- `src/styles/global.css`：全局布局、公共组件样式、列表基线。
- `src/styles/element-overrides.css`：Element Plus 主题映射。

### 4.2 主色与主题

- 当前主品牌色通过 `--brand` 及其衍生变量统一控制。
- Element Plus 主色通过 `--el-color-primary` 等变量映射到 `--brand`。

### 4.3 列表页约定

- 结构：筛选区 -> 操作栏 -> 表格区 -> 分页区。
- 长文本默认单行省略，悬浮显示完整内容。
- 表格操作和导出行为保持统一入口。

## 5. 文本与编码规范

- 统一 UTF-8。
- 禁止提交乱码文本（如 `鍒�`、`褰�`、`璇�`、`鐢�`）。
- 涉及文案修改时必须自检中文可读性。

## 6. 回归检查清单（前端改动后）

1. 登录态恢复、401 回退流程是否正常。
2. 菜单与按钮权限是否按角色生效。
3. 列表分页是否统一使用 `StandardPagination` + `useServerPagination`。
4. 详情/表单交互是否使用标准抽屉组件。
5. 视觉 Token 是否复用，无页面级“临时色值漂移”。

## 7. 已知问题与技术债

- `RealTimeMonitoringView` 仍存在本地分页状态实现，未完全复用 `useServerPagination`。
- `【待确认】` 前端是否已建立强制 lint/style 检查门禁；仓库中未见明确质量闸门说明。
- `【文档缺失】` 缺少“前端组件分层与命名规则”正式文档（如 atoms/molecules/templates 级别约定）。
