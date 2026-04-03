# 结果与基线中心证据清单与原文预览设计

> 日期：2026-04-03  
> 适用范围：`spring-boot-iot-report`、`spring-boot-iot-ui`  
> 设计来源：在“最近运行读取”已落地的基础上，继续补齐结果中心的后台证据检索能力  

## 1. 目标

在 `/automation-results` 已支持“最近运行结果读取 + 兼容手工导入”的基础上，再补一层“证据清单 + 原文预览”能力，让用户不必离开结果中心就能查看：

1. 当前运行结果的原始 `registry-run-*.json`
2. 当前运行关联的 `json / md / txt` 文本证据
3. 当前运行缺少后台证据时的明确降级提示

本轮只做本机 `logs/acceptance` 目录的**只读检索**，不扩展到 CI 归档、对象存储或数据库。

## 2. 边界

### 2.1 纳入范围

- 后端新增只读证据接口
- 前端结果页新增证据面板
- 自动在载入最近运行结果后显示证据清单
- 支持文本类文件内容预览

### 2.2 不纳入范围

- 不修改 CLI 产物格式
- 不新增数据库表
- 不支持二进制文件下载、图片预览或压缩包解压
- 不做跨环境结果汇聚
- 不改变手工 JSON 导入路径

## 3. 方案选择

本轮采用“**run 级证据索引 + 文本预览接口**”。

### 方案说明

后端继续以 `registry-run-<runId>.json` 作为主入口：

1. 先读取指定 run 的详情
2. 从 `reportPath + relatedEvidenceFiles + result.evidenceFiles` 归并出证据列表
3. 仅允许访问：
   - 当前 run 的主结果文件
   - 当前 run 明确引用到的证据文件
   - 且路径必须落在 `logs/acceptance` 下
4. 前端在结果页中新增证据面板：
   - 左侧：证据清单
   - 右侧：文本预览

### 取舍

- 优点：实现轻、风险低、符合当前真实环境本地日志基线
- 缺点：仍依赖本机文件，不等同于团队级归档系统

## 4. 后端设计

### 4.1 接口

- `GET /api/report/automation-results/{runId}/evidence`
  - 返回当前 run 的证据条目列表
- `GET /api/report/automation-results/{runId}/evidence/content?path=...`
  - 返回指定证据文件的文本预览

### 4.2 数据结构

新增两个 VO：

- `AutomationResultEvidenceItemVO`
  - `path`
  - `fileName`
  - `category`（`run-summary / json / markdown / text / unknown`）
  - `source`（`report / related / scenario`）

- `AutomationResultEvidenceContentVO`
  - `path`
  - `fileName`
  - `category`
  - `content`
  - `truncated`

### 4.3 安全约束

- 只允许读取 `logs/acceptance` 下文件
- 只允许读取当前 run 关联的文件
- 禁止通过 `..` 或绝对路径逃逸目录
- 文件内容按 UTF-8 文本读取；读取失败则返回业务错误
- 预览内容可设置长度上限，超出时返回 `truncated=true`

## 5. 前端设计

### 5.1 页面结构

`AutomationResultsView.vue` 在“最近运行结果”与“手工导入”之间新增证据面板：

- 当用户通过“最近运行结果”载入某个 run 后：
  - 自动拉取证据列表
  - 默认预览主 `registry-run-*.json`
- 当用户使用手工导入时：
  - 不强行构造后台证据
  - 面板提示“当前为手工导入结果，暂无后台证据可读取”

### 5.2 状态

在 `useAutomationRegistryWorkbench.ts` 中补充：

- `evidenceItems`
- `evidenceLoading`
- `evidenceErrorMessage`
- `selectedEvidencePath`
- `evidencePreview`
- `evidencePreviewLoading`
- `fetchEvidenceItems(runId)`
- `selectEvidence(runId, path)`

### 5.3 展现

- 证据清单用共享表格/列表样式
- 文本预览复用 `ResponsePanel` 或同级只读内容容器
- 不新增花哨配色，继续沿品牌/共享面板口径

## 6. 测试设计

### 6.1 后端

- 服务测试：
  - 列出当前 run 关联证据
  - 自动包含主 `registry-run` 文件
  - 去重
  - 非关联路径拒绝读取
- 控制器测试：
  - 路由可达
  - 查询参数透传正确

### 6.2 前端

- composable 测试：
  - 载入 recent run 后会自动加载证据列表
  - 选择证据后能拿到文本预览
- 视图契约测试：
  - 结果页包含新证据面板

## 7. 文档影响

需要同步更新：

- `docs/03-接口规范与接口清单.md`
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`

如实现过程中发现页面职责变化超过“证据预览”范围，再补充更新 `docs/21`。
