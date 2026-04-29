# 产品定义中心待实现功能详细文档

## 一、阶段一尾部功能（1-2周）

### 1.1 搜索历史记录

**功能描述**：记录用户使用的搜索历史，支持快速复用

**前端实现**：

#### 1.1.1 存储设计
```typescript
// 存储在 sessionStorage
{
  "iot.products.search-history": [
    {
      keyword: "http",
      timestamp: 1677616800000,
      type: "quick-search" | "product-name" | "product-key" | "manufacturer"
    },
    ...
  ]
}
```

#### 1.1.2 存储限制
- 最多保存 20 条搜索历史
- 搜索历史保留 7 天
- 搜索历史按时间倒序排列

#### 1.1.3 功能点
- [ ] 在快速搜索输入框下显示搜索历史
- [ ] 点击历史搜索关键词自动填充并执行搜索
- [ ] 支持删除单条历史
- [ ] 支持清空全部历史
- [ ] 热键支持：Ctrl+Shift+H 显示历史

**UI 布局**：
```
┌───────────────────────────────────┐
│  快速搜索                          │
│  [http] [iot] [http-json] [test] │
│  清空全部                         │
└───────────────────────────────────┘
```

**API 变更**：无需后端 API 变更

---

### 1.2 个性化列设置

**功能描述**：允许用户自定义列表视图中显示/隐藏的列

**前端实现**：

#### 1.2.1 配置管理
```typescript
// 列配置
interface TableColumnConfig {
  key: string;
  label: string;
  visible: boolean;
  width?: number;
  order: number;
}

// 存储在 localStorage
{
  "iot.products.table-columns": {
    "table": [...],
    "card": [...]
  }
}
```

#### 1.2.2 可配置列
| 列名 | Key | 默认显示 |
|------|-----|----------|
| 产品 Key | productKey | 是 |
| 产品名称 | productName | 是 |
| 协议编码 | protocolCode | 是 |
| 节点类型 | nodeType | 是 |
| 数据格式 | dataFormat | 是 |
| 厂商 | manufacturer | 是 |
| 产品状态 | status | 是 |
| 在线设备数 | onlineDeviceCount | 是 |
| 最近上报 | lastReportTime | 是 |
| 更新时间 | updateTime | 是 |

#### 1.2.3 功能点
- [ ] 列设置弹窗
- [ ] 拖拽排序列
- [ ] 复选框控制显示/隐藏
- [ ] 预设模板（默认、精简、详细）
- [ ] 重置为默认设置

**弹窗 UI**：
```
┌───────────────────────────────────┐
│  列设置                            │
├───────────────────────────────────┤
│  ☑ 产品 Key                        │
│  ☑ 产品名称                        │
│  ☐ 协议编码                        │
│  ☐ 节点类型                        │
│  ☐ 数据格式                        │
│  ☐ 厂商                            │
│  ☑ 产品状态                        │
│  ☑ 在线设备数                      │
│  ☑ 最近上报                        │
│  ☐ 更新时间                        │
│                                   │
│  [预设：精简] [预设：详细]       │
│  [重置]          [确定] [取消]   │
└───────────────────────────────────┘
```

---

### 1.3 拖拽排序

**功能描述**：支持拖拽调整列顺序

**实现方案**：
- 使用 `@dnd-kit/core` 库实现拖拽
- 拖拽后更新列配置中的 `order` 字段
- 列表重新渲染时按 order 排序

**依赖**：
```bash
npm install @dnd-kit/core @dnd-kit/sortable
```

---

### 1.4 快速预览

**功能描述**：鼠标悬停产品行时显示产品关键指标卡片

**实现方案**：
```typescript
// 预览数据结构
interface ProductPreview {
  productKey: string;
  productName: string;
  deviceCount: number;
  onlineDeviceCount: number;
  lastReportTime: string;
  status: number;
}
```

**UI 布局**：
```
┌───────────────────────────────────┐
│ 产品名称：HTTP产品                 │
│ 产品 Key：accept-http-product-01  │
│ 关联设备：10 台                   │
│ 在线设备：5 台                    │
│ 最近上报：2026-03-21 17:00:00     │
└───────────────────────────────────┘
```

**实现要点**：
- 使用 `@vueuse/core` 的 `useMouseInElement` 获取鼠标位置
- 使用 `throttle` 防抖
- 悬停 300ms 后显示预览

---

## 二、阶段一尾部功能（1-2周）

### 2.1 右键菜单

**功能描述**：在产品行上右键显示操作菜单

**实现方案**：
```typescript
interface ContextMenuItems {
  label: string;
  icon?: string;
  command: string;
  divider?: boolean;
  disabled?: boolean;
}
```

**菜单内容**：
- 查看详情
- 编辑
- 启用/停用
- 删除
- 查看设备
- 导出选中

**UI 布局**：
```
┌────────────────┐
│ 查看详情       │
│ 编辑           │
│ ─────────────  │
│ 启用           │
│ 停用           │
│ ─────────────  │
│ 删除           │
│ ─────────────  │
│ 查看设备       │
└────────────────┘
```

---

### 2.2 键盘支持

**功能描述**：支持键盘导航和快捷操作

**快捷键映射**：

| 快捷键 | 功能 |
|--------|------|
| ↑ ↓    | 上下移动选中行 |
| Enter  | 查看详情 |
| Space  | 选择/取消选择 |
| Ctrl+A | 全选 |
| Ctrl+F | 打开搜索框 |
| Ctrl+E | 编辑选中项 |
| Ctrl+D | 删除选中项 |

**实现方案**：
```typescript
useKeyboardNavigation({
  onUp: () => {},
  onDown: () => {},
  onEnter: () => {},
  onSpace: () => {},
  onEscape: () => {},
})
```

---

## 三、高级搜索（1-2周）

### 3.1 日期范围搜索

**功能描述**：支持按创建时间、最近上报时间搜索

**实现方案**：
```typescript
interface DateRange {
  start: string;
  end: string;
}

// 搜索表单
{
  productName?: string;
  nodeType?: number;
  status?: number;
  createTime?: DateRange;
  lastReportTime?: DateRange;
  deviceCountRange?: { min: number; max: number };
}
```

**UI 控件**：
- 使用 `el-date-picker` 组件
- 支持快捷选项：今天、最近7天、最近30天

---

### 3.2 设备数量范围搜索

**功能描述**：支持按设备数量范围搜索

**实现方案**：
```typescript
{
  deviceCountMin?: number;
  deviceCountMax?: number;
}
```

**UI 布局**：
```
设备数量：[ 0 ] 至 [ 100 ] 台
```

---

## 四、二级进化：数据洞察增强（2-3周）

### 4.1 设备分组

**功能描述**：按在线状态、固件版本、地理位置分组

#### 4.1.1 按在线状态分组
```typescript
interface DeviceGroup {
  key: 'online' | 'offline' | 'unknown';
  label: string;
  count: number;
  devices: Device[];
}
```

#### 4.1.2 按固件版本分组
```typescript
interface FirmwareGroup {
  version: string;
  count: number;
  devices: Device[];
}
```

#### 4.1.3 按地理位置分组
```typescript
interface LocationGroup {
  province: string;
  city: string;
  district: string;
  count: number;
  devices: Device[];
}
```

**实现要点**：
- 使用 Vue 3 的 `v-for` 和 `groupby` 实现分组
- 使用 Element Plus 的 `el-collapse` 实现折叠面板

---

### 4.2 数据趋势图

**功能描述**：最近7天设备上报趋势图

**技术方案**：

#### 4.2.1 图表库选择
- **ECharts**（推荐）：功能强大，支持复杂图表
- **Chart.js**：轻量级，易于使用

#### 4.2.2 数据结构
```typescript
interface TrendData {
  date: string;
  count: number;
}
```

#### 4.2.3 API 需求
```http
GET /api/device/product/{id}/trend?days=7
```

```json
{
  "code": 200,
  "data": [
    {"date": "2026-03-15", "count": 100},
    {"date": "2026-03-16", "count": 120},
    {"date": "2026-03-17", "count": 90},
    ...
  ]
}
```

**图表类型**：折线图 + 柱状图

**UI 布局**：
```
┌───────────────────────────────────┐
│ 近7天设备上报趋势                  │
│                                   │
│   120 │      ●                    │
│   100 │   ●     ●                 │
│    80 │ ●          ●              │
│    60 │              ●            │
│    40 │                   ●       │
│    20 │                        ●  │
│     0 └───────────────────────────┘
│       15 16 17 18 19 20 21       │
└───────────────────────────────────┘
```

---

### 4.3 产品拓扑图

**功能描述**：展示使用该产品的设备拓扑图

**技术方案**：
- 使用 ECharts 的 `force` 布局
- 节点：产品、设备
- 连线：设备 belong to 产品

**数据结构**：
```typescript
interface TopologyNode {
  id: string;
  name: string;
  type: 'product' | 'device';
  size?: number;
}

interface TopologyLink {
  source: string;
  target: string;
}
```

**实现要点**：
- 限制节点数量（最多显示 100 个设备）
- 支持缩放、拖拽
- 支持节点点击事件

---

## 五、三级进化：产品生命周期管理（3-4周）

### 5.1 产品变更历史

**功能描述**：展示产品的所有变更记录

#### 5.1.1 API 需求
```http
GET /api/device/product/{id}/history
```

```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "productId": 456,
      "action": "create" | "update" | "delete" | "enable" | "disable",
      "before": {...},
      "after": {...},
      "operator": "admin",
      "operatorName": "管理员",
      "createdAt": "2026-03-20 10:00:00",
      "reason": "修复协议问题"
    }
  ]
}
```

#### 5.1.2 UI 布局
```
┌───────────────────────────────────┐
│ 变更历史                          │
├───────────────────────────────────┤
│ 2026-03-21 16:00:00              │
│ by 管理员                         │
│ 更新产品                          │
│ ───────────────────────────       │
│ 产品 Key: accept-http-product-01 │
│ 协议编码: http-json → http-xml   │
└───────────────────────────────────┘
```

#### 5.1.3 功能点
- [ ] 分页查询
- [ ] 筛选操作类型
- [ ] 查看变更详情（before/after 对比）
- [ ] 导出变更日志

---

### 5.2 产品预警机制

**功能描述**：配置预警规则，当满足条件时发送通知

#### 5.2.1 预警类型

**设备离线预警**
```
规则：离线设备超过阈值时提醒
配置：
  - 阈值：10 台
  - 时长：5 分钟
  - 通知方式：站内信 + 邮件
```

**数据异常预警**
```
规则：上报数据异常时提醒
配置：
  - 异常指标：上报间隔 > 1 小时
  - 通知方式：站内信
```

**状态变更预警**
```
规则：产品状态变更时提醒
配置：
  - 通知方式：站内信 + 邮件
```

**设备数量突变预警**
```
规则：设备数量突增/突减时提醒
配置：
  - 突增阈值：+50%
  - 突减阈值：-30%
  - 通知方式：站内信
```

#### 5.2.2 API 需求
```http
// 查询预警规则
GET /api/device/product/{id}/alert-rules

// 保存预警规则
POST /api/device/product/{id}/alert-rules
{
  "offlineThreshold": 10,
  "offlineDuration": 300,
  "dataAnomalyEnabled": true,
  "statusChangeEnabled": true,
  "deviceCountChangeEnabled": true
}
```

---

## 六、四级进化：产品物模型管理（4-6周）

### 6.1 物模型可视化

**功能描述**：展示产品的所有属性、服务、事件

#### 6.1.1 数据结构
```typescript
interface Property {
  identifier: string;
  name: string;
  datatype: 'int' | 'float' | 'string' | 'bool' | 'date' | 'array' | 'object';
  accessMode: 'r' | 'w' | 'rw';
  description: string;
  minLength?: number;
  maxLength?: number;
  min?: number;
  max?: number;
  unit?: string;
}

interface Service {
  identifier: string;
  name: string;
  description: string;
  input: Property[];
  output: Property[];
}

interface Event {
  identifier: string;
  name: string;
  type: 'info' | 'warning' | 'alarm';
  description: string;
  output: Property[];
}
```

#### 6.1.2 API 需求
```http
GET /api/device/product/{id}/model
```

#### 6.1.3 UI 布局
```
┌───────────────────────────────────┐
│ 物模型                            │
├───────────────────────────────────┤
│ 属性 (3)                          │
│  ├─ temperature (float, r/w)     │
│  ├─ humidity (float, r/w)        │
│  └─ status (bool, r)             │
│                                   │
│ 服务 (2)                          │
│  ├─ setTemperature               │
│  └─ reset                        │
│                                   │
│ 事件 (1)                          │
│  └─ alarm                        │
└───────────────────────────────────┘
```

---

### 6.2 物模型编辑器

**功能描述**：可视化编辑和 JSON 编辑

#### 6.2.1 可视化编辑
- 拖拽添加属性、服务、事件
- 表单编辑属性详情
- 支持复杂类型（数组、对象）

#### 6.2.2 JSON 编辑
- CodeMirror/VsCode 编辑器
- JSON Schema 校验
- 格式化代码

#### 6.2.3 预览功能
- 实时预览 JSON 格式
- 格式校验提示

---

### 6.3 模型模板库

**功能描述**：提供常见行业的物模型模板

#### 6.3.1 模板分类
- 工业设备
-家电设备
- 环境监测
- 安防设备
- 其他

#### 6.3.2 API 需求
```http
GET /api/device/product/templates
```

---

## 七、五级进化：智能化功能（6-8周）

### 7.1 相似产品推荐

**功能描述**：基于协议、节点类型、数据格式推荐相似产品

#### 7.1.1 推荐算法
```typescript
interface ProductSimilarity {
  productId: number;
  productName: string;
  similarityScore: number; // 0-100
  matchFields: string[];
}

// 匹配字段
matchFields: ["protocolCode", "nodeType", "dataFormat"]
```

#### 7.1.2 API 需求
```http
GET /api/device/product/{id}/similar
```

---

### 7.2 设备接入推荐

**功能描述**：基于设备类型推荐最合适的产品

#### 7.2.1 推荐算法
```typescript
interface DeviceRecommendation {
  productId: number;
  productName: string;
  confidence: number; // 0-100
  reason: string;
}
```

---

## 八、六级进化：协作与审核（8-10周）

### 8.1 多人协作

**功能描述**：支持多人同时编辑产品

#### 8.1.1 实现方案
- WebSocket 实时同步
- 操作冲突解决（Last Writer Wins）
- 编辑锁定机制

---

## 九、七级进化：产品生态系统（10-12周）

### 9.1 产品市场

**功能描述**：支持将产品分享给其他用户

#### 9.1.1 API 需求
```http
// 分享产品
POST /api/device/product/{id}/share
{
  "targetUserId": 123,
  "permissions": ["view", "edit"]
}

// 我的产品市场
GET /api/device/product/market
```

---

## 十、技术栈和依赖

### 10.1 前端依赖

| 功能 | 库 | 说明 |
|------|-----|------|
| 拖拽排序 | @dnd-kit/core | Drag and drop |
| 图表 | echarts | 数据可视化 |
| 地图 | mapbox-gl-js | 地图展示 |
| 富文本编辑 | Quill | 物模型编辑器 |
| 代码编辑 | @codemirror/lang-javascript | JSON 编辑 |
| 状态管理 | pinia | 全局状态 |
| 路由 | vue-router | 页面路由 |

### 10.2 后端依赖

| 功能 | 技术 |
|------|------|
| WebSocket | spring-websocket |
| 缓存 | redis |
| 搜索引擎 | elasticsearch |
| 消息通知 | rabbitmq |

---

## 十一、优先级建议

### P0 - 立即实施（1-2周）
1. 搜索历史记录
2. 日期范围搜索
3. 设备数量范围搜索

### P1 - 近期实施（2-3周）
1. 设备分组
2. 数据趋势图
3. 产品变更历史

### P2 - 中期实施（3-4周）
1. 产品预警机制
2. 物模型可视化

### P3 - 长期规划（4周+）
1. 物模型编辑器
2. 相似产品推荐
3. 多人协作
4. 产品市场

---

## 十二、实施路线图

### Week 1-2: 搜索增强
- [ ] 搜索历史
- [ ] 日期范围搜索
- [ ] 设备数量范围搜索

### Week 3-4: 数据洞察
- [ ] 设备分组
- [ ] 数据趋势图
- [ ] 产品拓扑图

### Week 5-6: 生命周期管理
- [ ] 产品变更历史
- [ ] 产品预警机制

### Week 7-8: 物模型管理
- [ ] 物模型可视化
- [ ] 物模型编辑器

### Week 9-10: 智能化
- [ ] 相似产品推荐
- [ ] 设备接入推荐

### Week 11-12: 协作与生态
- [ ] 多人协作
- [ ] 产品市场