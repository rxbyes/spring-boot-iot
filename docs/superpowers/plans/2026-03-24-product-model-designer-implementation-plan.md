# 产品物模型设计器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不扩散设备资产扩展范围的前提下，为现有产品定义中心补齐一个独立的产品物模型设计器，支持 `property / event / service` 的列表、增改删、基础校验和风险监测字段治理。

**Architecture:** 本方案继续复用现有 `iot_product_model` 表、`ProductModel` 实体和 `/products` 单路由工作台，不新增顶层页面，也不与设备资产扩展、上下架审批、远程维护或维修工单联动并行开发。后端在 `spring-boot-iot-device` 内补齐独立 DTO / VO / Service / Controller，前端在现有 `ProductWorkbenchView` 中增加一个独立的“产品物模型设计器”入口，并拆出专用组件，避免继续把产品台账和物模型治理混写在一个超大页面文件里。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Element Plus, Vitest, Maven

**2026-03-25 回填说明：** 当前仓库已具备完整实现；以下勾选依据现有代码、权威文档与本会话 fresh 验证结果回填，不再追溯最初 red 阶段的命令输出。当前仓库未归档独立 `product-model-designer-acceptance-20260325003151.json`，验收留痕以 `docs/19`、`docs/21` 与《真实环境测试与验收手册》补充章节为准。`2026-03-27` 已补做只读 live 复核：MySQL 中产品 `2036481128347844609 / accept-product-model-20260325003151` 当前保留 `property=1 / event=1 / service=0`，且 `GET /api/device/product/2036481128347844609/models` 返回 `200`、共 `2` 条记录，与数据库一致。

---

## 范围约束

1. 只做“产品物模型设计器”一条增强线，不并行带入设备资产扩展、远程维护、维修工单联动或上下架审批。
2. 不新增顶层菜单或一级路由，入口继续挂在现有 `/products` 产品定义中心内。
3. 优先复用现有 `iot:products:view` / `iot:products:update` 权限边界，不在首轮实现里引入新的菜单种子和按钮权限模型。
4. 不改动 `iot_product_model` 现有物理表结构，首轮仅围绕现有字段做治理；若发现真实环境字段不足，再单独补 SQL 任务卡。

## 文件结构

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductModel.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductModelMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelUpsertDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Create: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

## Task 1: 固定后端 API 契约与领域校验

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductModel.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductModelMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelUpsertDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductModelService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelVO.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [x] **Step 1: 先写失败的服务测试，锁定最小行为**

Test cases:
- 同一产品下 `identifier` 必须唯一
- `modelType` 只允许 `property`、`event`、`service`
- `property` 必须校验 `dataType`
- `event` 只允许写 `eventType`
- `service` 只允许写 `serviceInputJson` / `serviceOutputJson`
- 列表返回必须按 `sortNo`、`identifier` 稳定排序

- [x] **Step 2: 运行后端定向测试，确认当前缺少实现**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=ProductModelServiceImplTest test
```

Expected:
- 测试失败
- 失败原因来自缺少 `ProductModelServiceImpl` 或缺少预期校验逻辑

- [x] **Step 3: 实现独立后端服务与 VO**

API contract:
- `GET /api/device/product/{productId}/models`
- `POST /api/device/product/{productId}/models`
- `PUT /api/device/product/{productId}/models/{modelId}`
- `DELETE /api/device/product/{productId}/models/{modelId}`

Payload fields:
- `modelType`
- `identifier`
- `modelName`
- `dataType`
- `specsJson`
- `eventType`
- `serviceInputJson`
- `serviceOutputJson`
- `sortNo`
- `requiredFlag`
- `description`

Implementation rules:
- 所有操作都必须先校验产品存在
- 更新 / 删除都必须校验 `modelId` 属于当前 `productId`
- `specsJson`、`serviceInputJson`、`serviceOutputJson` 必须在服务层做 JSON 合法性校验
- 首轮不做 schema migration，只复用现有 `iot_product_model`

- [x] **Step 4: 重新运行后端定向测试，确认服务层通过**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=ProductModelServiceImplTest test
```

Expected:
- `ProductModelServiceImplTest` 通过

## Task 2: 补齐控制器与产品工作台设计器入口

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Create: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [x] **Step 1: 先写失败的控制器与前端测试**

Test scope:
- 控制器能返回产品维度的物模型列表
- 控制器的新增 / 更新 / 删除直接透传到服务层
- `ProductWorkbenchView` 出现“物模型设计器”入口
- 打开设计器后，能展示 `property / event / service` 三类视图切换和列表空态

- [x] **Step 2: 运行定向测试，确认当前前后端入口缺失**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=ProductModelControllerTest test
cd spring-boot-iot-ui && npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:
- 后端控制器测试失败
- 前端测试失败
- 失败原因来自缺少控制器、类型定义或设计器入口

- [x] **Step 3: 实现设计器入口，但不新增顶层页面**

Frontend rules:
- 入口继续挂在 `ProductWorkbenchView` 的详情 / 行操作内
- 设计器主体放入 `ProductModelDesignerDrawer.vue`
- 继续复用 `StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter`、共享列表操作样式
- 不把物模型编辑表单直接堆进现有产品新增 / 编辑抽屉
- 首轮先交付“列表 + 新增 + 编辑 + 删除 + 类型切换 + JSON 合法性提示”
- 继续保留产品台账、关联设备跳转、导出和详情闭环，不回退现有产品中心结构

- [x] **Step 4: 重跑控制器、前端测试与前端构建**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=ProductModelControllerTest,ProductModelServiceImplTest test
cd spring-boot-iot-ui && npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
cd spring-boot-iot-ui && npm run build
cd spring-boot-iot-ui && npm run component:guard
cd spring-boot-iot-ui && npm run list:guard
cd spring-boot-iot-ui && npm run style:guard
```

Expected:
- 后端定向测试通过
- 前端定向测试通过
- 前端 build 与 guard 命令通过

## Task 3: 回写文档并补齐验收口径

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [x] **Step 1: 回写业务语义、API 和数据库说明**

Clarify:
- 产品物模型设计器是 Phase 5 已冻结的唯一设备中心增强口
- `iot_product_model` 的运行期用途与设计器用途要分别说明
- 对外 API 新增产品物模型 CRUD

- [x] **Step 2: 运行文档拓扑校验**

Run:

```bash
node scripts/docs/check-topology.mjs
```

Expected:
- 文档无坏链
- 新任务卡链接可达

- [x] **Step 3: 跑最终最小回归**

Run:

```bash
mvn clean package -DskipTests
cd spring-boot-iot-ui && npm run build
node scripts/docs/check-topology.mjs
```

Expected:
- Maven 构建通过
- 前端 build 通过
- 文档拓扑校验通过

## 完成标准

1. 产品定义中心出现独立的产品物模型设计器入口，但不新增顶层路由。
2. 后端补齐产品维度的物模型列表、新增、更新、删除接口，并由单独服务承接校验。
3. `iot_product_model` 的运行期消费与人工设计器口径在 `02 / 03 / 04 / 19 / 21` 中同步一致。
4. 不把设备资产扩展、上下架审批、远程维护或维修工单联动混入同一轮实现。
