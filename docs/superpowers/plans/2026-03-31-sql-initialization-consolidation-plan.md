# SQL Initialization Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把仓库中的 SQL 收口为仅保留 `sql/init.sql` 与 `sql/init-data.sql` 两个最新初始化文件，并删除其余 SQL 文件。

**Architecture:** 直接把已进入默认交付基线的 DDL、索引、兼容视图和初始化数据并回两个 init 文件；随后删除 `sql/upgrade` 中的历史、专项和过渡脚本，并同步更新数据库文档口径。

**Tech Stack:** MySQL DDL/DML、PowerShell、Git、Markdown 文档

---

### Task 1: 收口 `sql/init.sql`

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\sql\init.sql`

- [ ] **Step 1: 补齐当前基线缺失结构**

把以下内容直接写入 `sql/init.sql`：

```sql
DROP TABLE IF EXISTS iot_device_metric_latest;

CREATE TABLE iot_device_metric_latest (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    metric_id VARCHAR(128) NOT NULL,
    metric_code VARCHAR(128) NOT NULL,
    metric_name VARCHAR(128) DEFAULT NULL,
    value_type VARCHAR(32) DEFAULT NULL,
    value_double DOUBLE DEFAULT NULL,
    value_long BIGINT DEFAULT NULL,
    value_bool TINYINT(1) DEFAULT NULL,
    value_text TEXT DEFAULT NULL,
    quality_code VARCHAR(32) DEFAULT NULL,
    alarm_flag TINYINT(1) DEFAULT NULL,
    reported_at DATETIME DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tel_latest_tenant_device_metric (tenant_id, device_id, metric_id),
    KEY idx_tel_latest_device_reported (device_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: 补齐消息日志兼容视图**

在 `iot_device_message_log` 建表之后增加视图定义：

```sql
CREATE VIEW iot_message_log AS
SELECT * FROM iot_device_message_log;
```

- [ ] **Step 3: 自检 `init.sql` 不再依赖 upgrade 里的正式基线 DDL**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init.sql' -Pattern 'iot_device_metric_latest|CREATE VIEW iot_message_log'
```

Expected: 能直接在 `init.sql` 中看到 `iot_device_metric_latest` 建表和 `iot_message_log` 视图定义。

### Task 2: 复核 `sql/init-data.sql`

**Files:**
- Modify if needed: `E:\idea\ghatg\spring-boot-iot\sql\init-data.sql`

- [ ] **Step 1: 确认最新基线数据已经内收**

检查以下内容已经直接存在于 `init-data.sql`：

```text
五大工作台菜单
产品定义中心与设备资产中心按钮权限
站内消息 / 帮助文档菜单与按钮
自动化工场菜单
webhook-default 场景配置
sys_in_app_message / sys_help_document 样例
```

- [ ] **Step 2: 仅在存在缺口时补齐数据**

如果核对发现正式基线样例仍缺失，再直接补到 `sql/init-data.sql`，不新增任何 upgrade 脚本。

- [ ] **Step 3: 自检 `init-data.sql` 包含系统内容基线**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\sql\init-data.sql' -Pattern 'system:in-app-message|system:help-doc|webhook-default|quality-workbench|system:automation-test'
```

Expected: 所有当前正式基线菜单、帮助中心和通知渠道配置都直接落在 `init-data.sql` 中。

### Task 3: 删除其余 SQL 文件并同步文档

**Files:**
- Delete: `E:\idea\ghatg\spring-boot-iot\sql\upgrade\*.sql`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify if needed: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify if needed: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: 删除非 init SQL 文件**

删除 `sql/upgrade` 下全部 `.sql` 文件；若目录清空，则删除目录本身。

- [ ] **Step 2: 更新数据库文档口径**

把 `docs/04-数据库设计与初始化数据.md` 改为：

```text
仓库 SQL 入口只剩 sql/init.sql 与 sql/init-data.sql
不再引用 sql/upgrade/*.sql 作为仓库内入口
说明 TDengine 兼容表由运行时启动自动补齐
说明旧 upgrade 文件已并回基线或从仓库移除
```

- [ ] **Step 3: 检查 README / AGENTS 是否仍引用 upgrade 目录**

Run:

```powershell
Select-String -Path 'E:\idea\ghatg\spring-boot-iot\README.md','E:\idea\ghatg\spring-boot-iot\AGENTS.md','E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md' -Pattern 'sql/upgrade|upgrade/'
```

Expected: 仓库入口文档不再把 `sql/upgrade` 描述为需要保留或执行的初始化路径。

### Task 4: 最终核对仓库 SQL 清单

**Files:**
- Verify: `E:\idea\ghatg\spring-boot-iot\sql`

- [ ] **Step 1: 检查 `sql` 目录只剩两个初始化文件**

Run:

```powershell
Get-ChildItem -Path 'E:\idea\ghatg\spring-boot-iot\sql' -Recurse | Select-Object FullName
```

Expected: 最终只看到：

```text
E:\idea\ghatg\spring-boot-iot\sql\init.sql
E:\idea\ghatg\spring-boot-iot\sql\init-data.sql
```

- [ ] **Step 2: 提交变更前检查 git diff**

Run:

```powershell
git status --short
git diff -- 'sql' 'docs/04-数据库设计与初始化数据.md' 'README.md' 'AGENTS.md'
```

Expected: 只包含本轮 SQL 收口和文档同步改动，不混入无关文件。
