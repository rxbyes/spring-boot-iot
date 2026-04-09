# Risk Point Pending Manual Review Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the pending batch governance script so `manual_review` items can be exported as business confirmation templates and batch-annotated back into `resolution_note` with a dry-run/apply safety rail.

**Architecture:** Reuse the existing manifest-driven script in `scripts/manage-risk-point-pending-governance.py` rather than introducing a new tool. Keep manual-review export and annotation logic as pure helpers first, then expose them through `export-manual-review` and `annotate-manual-review` CLI subcommands so unit tests can lock behavior without touching the real database.

**Tech Stack:** Python 3, argparse, json, csv, pymysql, unittest

---

### Task 1: Lock Manual Review Export Behavior with Tests

**Files:**
- Modify: `scripts/test_manage_risk_point_pending_governance.py`

- [ ] **Step 1: Write a failing test for grouped manual-review export records**

Add a test that builds a small in-memory `manual_review` list containing one tilt meter row, one GNSS monitor row, and one GNSS base station row, then asserts the export helper returns:

- stable groups
- expected suggested actions
- expected system recommendations
- expected business follow-up text

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_build_manual_review_export_records`

Expected: FAIL because the export helper does not exist yet.

- [ ] **Step 3: Write a failing test for CLI export output**

Add a test that writes a temp manifest with `manual_review` items, calls:

`python3 scripts/manage-risk-point-pending-governance.py export-manual-review --manifest <temp-manifest> --output-prefix <temp-prefix>`

Assert:

- command exits `0`
- generated `.json` exists
- exported row count matches manifest

- [ ] **Step 4: Run the targeted CLI export test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_cli_export_manual_review_writes_json`

Expected: FAIL because the subcommand does not exist yet.

### Task 2: Lock Annotation Request Behavior with Tests

**Files:**
- Modify: `scripts/test_manage_risk_point_pending_governance.py`

- [ ] **Step 1: Write a failing test for annotation note generation**

Add a test that passes grouped manual-review items into a note builder and asserts:

- tilt rows mention `AZI/X/Y/Z/angle`
- GNSS monitor rows mention `gpsTotalX/gpsTotalY/gpsTotalZ`
- GNSS base station rows mention `姿态/健康参数`

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_build_manual_review_annotation_rows`

Expected: FAIL because the annotation helper does not exist yet.

- [ ] **Step 3: Write a failing test for dry-run annotation execution**

Add a test that writes a temp manifest with `manual_review` items, runs:

`python3 scripts/manage-risk-point-pending-governance.py annotate-manual-review --manifest <temp-manifest>`

Assert:

- command exits `0`
- stdout contains `dryRun=True`
- stdout includes pending ids and generated note preview

- [ ] **Step 4: Run the targeted CLI annotation test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_cli_annotate_manual_review_dry_run`

Expected: FAIL because the subcommand does not exist yet.

### Task 3: Implement Manual Review Export / Annotate Support

**Files:**
- Modify: `scripts/manage-risk-point-pending-governance.py`

- [ ] **Step 1: Add pure helper functions for manual-review grouping and recommendations**

Implement helpers that derive:

- export group name
- suggested action
- system recommendation
- business follow-up text

from each `manual_review` manifest item.

- [ ] **Step 2: Add export writers**

Implement helpers that can serialize manual-review rows to:

- JSON
- CSV
- Markdown

using one `--output-prefix`.

- [ ] **Step 3: Add annotation row builder**

Implement a helper that converts `manual_review` manifest items into:

- `pendingId`
- generated `resolutionNote`

without changing the record status.

- [ ] **Step 4: Add CLI subcommands**

Extend `argparse` with:

- `export-manual-review`
- `annotate-manual-review`

Rules:

- `export-manual-review` reads the manifest and writes template files
- `annotate-manual-review` defaults to dry-run
- `annotate-manual-review --apply` updates only `resolution_note`, `update_by`, `update_time`
- no status transition is allowed in annotation mode

- [ ] **Step 5: Add DB write path for annotation apply**

Reuse the existing MySQL connection resolver and update `risk_point_device_pending_binding` by id with:

- unchanged `resolution_status`
- new grouped `resolution_note`
- refreshed `update_by/update_time`

- [ ] **Step 6: Run the script unit tests**

Run: `python3 -m unittest scripts/test_manage_risk_point_pending_governance.py`

Expected: PASS

### Task 4: Update Docs

**Files:**
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`

- [ ] **Step 1: Document the new manual-review export / annotate commands**

Explain:

- `export-manual-review`
- `annotate-manual-review`
- dry-run default
- apply guardrail

- [ ] **Step 2: Document the business effect**

Clarify that annotation mode only improves pending治理备注，不会直接转正或忽略。

- [ ] **Step 3: Record this round in the changelog**

Add a new `2026-04-04` change summary entry.

### Task 5: Verify in Real Dev

**Files:**
- Modify: none

- [ ] **Step 1: Run manual-review export against the residual manifest**

Run:

`python3 scripts/manage-risk-point-pending-governance.py export-manual-review --manifest /tmp/risk-point-pending-operational-manifest-residual-20260404.json --output-prefix /tmp/risk-point-pending-manual-review-automation-20260404`

Expected:

- `.json/.csv/.md` all generated
- row count `17`

- [ ] **Step 2: Run annotation dry-run against the residual manifest**

Run:

`python3 scripts/manage-risk-point-pending-governance.py annotate-manual-review --manifest /tmp/risk-point-pending-operational-manifest-residual-20260404.json`

Expected:

- no database writes
- stdout shows generated annotation preview

- [ ] **Step 3: Run annotation apply against the residual manifest**

Run:

`python3 scripts/manage-risk-point-pending-governance.py annotate-manual-review --manifest /tmp/risk-point-pending-operational-manifest-residual-20260404.json --apply --result-output /tmp/risk-point-pending-manual-review-annotate-apply-20260404.json`

Expected:

- `17` records updated
- `resolution_status` remains `PENDING_METRIC_GOVERNANCE`

- [ ] **Step 4: Re-query the real database to verify status counts and note updates**

Verify:

- `PENDING_METRIC_GOVERNANCE` total remains unchanged
- sample ids `418 / 370 / 12` show grouped notes

- [ ] **Step 5: Summarize changed files and docs**

Report:

- changed script and tests
- changed docs
- real-env verification evidence
- whether README.md / AGENTS.md changed
