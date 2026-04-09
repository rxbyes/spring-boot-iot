# Highway Risk Point Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the SQL baseline so the project can initialize highway risk points, supporting organization/region mapping, generic risk-point archive fields, and highway-specific detail records from `项目.xls`.

**Architecture:** Keep `risk_point` as the platform-wide risk object table and add only cross-domain fields there. Store highway-specific archive fields in a new `risk_point_highway_detail` table, then seed minimal `sys_region` and `sys_organization` records plus `risk_point` and `risk_point_highway_detail` rows derived from the Excel source.

**Tech Stack:** MySQL DDL/DML in `sql/init.sql` and `sql/init-data.sql`, Markdown docs in `docs/04-数据库设计与初始化数据.md` and `docs/08-变更记录与技术债清单.md`, Excel source inspection via PowerShell COM automation.

---

### Task 1: Extend the SQL schema baseline

**Files:**
- Modify: `sql/init.sql`
- Modify: `docs/04-数据库设计与初始化数据.md`

- [ ] **Step 1: Add generic highway-ready columns to `risk_point`**

Add these columns after `risk_level` and before `description`:

```sql
    risk_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '风险点类型 SLOPE/BRIDGE/TUNNEL/GENERAL',
    location_text VARCHAR(255) DEFAULT NULL COMMENT '位置描述/桩号/区间',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '风险点经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '风险点纬度',
```

Also update indexes:

```sql
    KEY idx_region (region_id),
    KEY idx_status (status),
    KEY idx_risk_type_status (risk_type, status)
```

- [ ] **Step 2: Create `risk_point_highway_detail`**

Add this table immediately after `risk_point` and before `risk_point_device`:

```sql
CREATE TABLE risk_point_highway_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    project_type VARCHAR(32) NOT NULL COMMENT '项目类型',
    project_summary TEXT DEFAULT NULL COMMENT '项目简介',
    route_code VARCHAR(64) NOT NULL COMMENT '路线编号',
    route_name VARCHAR(128) DEFAULT NULL COMMENT '路线名称',
    road_level VARCHAR(64) DEFAULT NULL COMMENT '公路等级',
    project_risk_level VARCHAR(32) DEFAULT NULL COMMENT '项目风险等级原始值',
    admin_region_code VARCHAR(32) DEFAULT NULL COMMENT '行政区域末级编码',
    admin_region_path_json VARCHAR(255) DEFAULT NULL COMMENT '行政区域路径JSON',
    maintenance_org_name VARCHAR(128) DEFAULT NULL COMMENT '管养单位名称',
    source_row_no INT DEFAULT NULL COMMENT 'Excel来源行号',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_highway (risk_point_id),
    KEY idx_route_code (route_code),
    KEY idx_admin_region_code (admin_region_code),
    KEY idx_project_type (project_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高速公路风险点扩展表';
```

- [ ] **Step 3: Verify schema text after editing**

Run:

```powershell
Select-String -Path 'sql\init.sql' -Pattern 'risk_type|location_text|risk_point_highway_detail' -Encoding UTF8
```

Expected:
- `risk_point` contains the four new generic fields
- `risk_point_highway_detail` appears once with the new indexes

- [ ] **Step 4: Update database documentation**

Document:
- new `risk_point` fields and their purpose
- new `risk_point_highway_detail` table
- note that highway archive initialization does not create `risk_point_device`

- [ ] **Step 5: Review the diff**

Run:

```powershell
git diff -- sql/init.sql docs/04-数据库设计与初始化数据.md
```

Expected:
- only the intended schema and documentation changes appear

### Task 2: Seed minimal region and organization mappings for highway data

**Files:**
- Modify: `sql/init-data.sql`

- [ ] **Step 1: Add minimal region seed block for the Excel paths**

Insert a new block near the existing `sys_region` seed section. Use actual administrative codes from the Excel file, keeping a minimal province -> city -> district tree only for the codes present in the spreadsheet.

Required coverage:

```text
62
6201 6202 6205 6206 6208 6210 6211 6212 6229
620102 620103 620104 620111 620121 620122 620123
620201101
620503 620522
620602 620622
620881
621021 621026
621102 621123
621202 621221 621222 621225
622901 622923 622925
```

- [ ] **Step 2: Add organization seed block for the maintenance units**

Insert a new block near the existing `sys_organization` seed section covering these unique names:

```text
成县所
成武所
武都所
甘肃公航旅高速公路运营管理兰州分公司
甘肃公航旅高速公路运营管理临夏分公司
甘肃公航旅高速公路运营管理平凉分公司
甘肃公航旅高速公路运营管理武威分公司
甘肃公航旅高速公路运营管理天水分公司
甘肃公航旅高速公路运营管理定西分公司
甘肃公航旅高速公路运营管理敦煌分公司
```

Use:
- flat `parent_id = 0`
- `leader_user_id = 1`
- deterministic `org_code`
- `phone = '13800000000'`

- [ ] **Step 3: Verify region and organization counts**

Run:

```powershell
Select-String -Path 'sql\init-data.sql' -Pattern 'INSERT INTO sys_region|INSERT INTO sys_organization|620121|成武所' -Encoding UTF8
```

Expected:
- the new seed blocks exist
- a district-level code and a maintenance unit sample are visible in the SQL

- [ ] **Step 4: Review the seed section diff**

Run:

```powershell
git diff -- sql/init-data.sql
```

Expected:
- only the intended new `sys_region` and `sys_organization` additions appear in this stage

### Task 3: Seed highway risk points and highway detail records

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Build deterministic risk-point codes and mapped values**

Generate `65` `risk_point` records from the Excel file using these rules:

```text
边坡 -> risk_type=SLOPE  -> code prefix RP-HW-SLOPE-
桥梁 -> risk_type=BRIDGE -> code prefix RP-HW-BRIDGE-
隧道 -> risk_type=TUNNEL -> code prefix RP-HW-TUNNEL-
risk_level -> blue
location_text -> 路线编号
region_id -> mapped from 行政区域末级编码 when present
org_id -> mapped from 管养单位名称
responsible_user -> 1
responsible_phone -> 13800000000
```

- [ ] **Step 2: Add `risk_point` seed SQL**

Insert one new `INSERT INTO risk_point (...) VALUES ... ON DUPLICATE KEY UPDATE ...` block after the existing demo risk-point data or replace the old demo block if the baseline should become highway-first.

Each row must include the new columns:

```sql
    risk_level, risk_type, location_text, longitude, latitude, description,
```

- [ ] **Step 3: Add `risk_point_highway_detail` seed SQL**

Insert one `INSERT INTO risk_point_highway_detail (...) VALUES ... ON DUPLICATE KEY UPDATE ...` block with exactly one record per seeded highway risk point.

Each row must preserve:
- original project name
- original project type text
- route code/name
- road level
- raw project risk level
- administrative region JSON path
- maintenance organization name
- Excel source row number

- [ ] **Step 4: Document the baseline change in the changelog**

Add an entry to `docs/08-变更记录与技术债清单.md` describing:
- `risk_point` generic archive expansion
- new `risk_point_highway_detail`
- minimal highway `sys_region / sys_organization` seed bootstrap
- initialization now includes `65` highway risk points from `项目.xls`

- [ ] **Step 5: Verify the generated record counts**

Run a verification command that counts inserted rows by seed prefix:

```powershell
Get-Content 'sql\init-data.sql' -Encoding UTF8 | Select-String 'RP-HW-SLOPE-|RP-HW-BRIDGE-|RP-HW-TUNNEL-' | Measure-Object
```

Expected:
- total count reflects all seeded highway `risk_point` rows

Run:

```powershell
Get-Content 'sql\init-data.sql' -Encoding UTF8 | Select-String 'INSERT INTO risk_point_highway_detail|source_row_no' | Measure-Object
```

Expected:
- the detail seed block is present and contains highway detail data

### Task 4: Final review and verification

**Files:**
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Review final diffs**

Run:

```powershell
git diff -- sql/init.sql sql/init-data.sql docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md
```

Expected:
- only the schema, seed, and documentation updates for highway risk-point bootstrap appear

- [ ] **Step 2: Re-check spec coverage**

Manually verify the final changes cover:
- generic `risk_point` extension
- new `risk_point_highway_detail`
- minimal region/organization mapping
- `65` highway seeds
- documentation updates
- no `risk_point_device` seed fabrication

- [ ] **Step 3: Run a final grep verification**

Run:

```powershell
Select-String -Path 'sql\init.sql','sql\init-data.sql','docs\04-数据库设计与初始化数据.md','docs\08-变更记录与技术债清单.md' -Pattern 'risk_point_highway_detail|risk_type|location_text|RP-HW-SLOPE|项目.xls' -Encoding UTF8
```

Expected:
- all new schema, seed, and doc anchors are present

- [ ] **Step 4: Prepare close-out notes**

Summarize:
- changed files
- schema additions
- seeded entity counts
- docs updated
- verification commands run
