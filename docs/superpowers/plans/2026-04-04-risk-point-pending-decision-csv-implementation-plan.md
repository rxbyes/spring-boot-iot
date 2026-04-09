# Risk Point Pending Decision CSV Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the pending governance script so a filled business-decision CSV can be validated, dry-run previewed, and applied to execute `PROMOTE / IGNORE / KEEP_PENDING` for `manual_review` items.

**Architecture:** Reuse `scripts/manage-risk-point-pending-governance.py` as the single entrypoint. Add pure helpers for CSV parsing, decision validation, and request construction first, then expose them through a new `apply-manual-review-decisions` subcommand that routes `PROMOTE` to the existing promote API, `IGNORE` to the existing ignore API, and `KEEP_PENDING` to the existing note-update path.

**Tech Stack:** Python 3, argparse, csv, json, pymysql, unittest

---

### Task 1: Lock CSV Parsing and Validation with Tests

**Files:**
- Modify: `scripts/test_manage_risk_point_pending_governance.py`

- [ ] **Step 1: Write a failing test for reading decision CSV rows**

Add a test that writes a temp CSV with these columns:

- `pending_id`
- `business_decision`
- `canonical_metrics`
- `notes`

and rows for:

- `PROMOTE` with `gpsTotalX,gpsTotalY,gpsTotalZ`
- `IGNORE` with empty `canonical_metrics`
- `KEEP_PENDING` with empty `canonical_metrics`

Assert the CSV reader returns normalized rows with:

- integer `pendingId`
- trimmed `businessDecision`
- parsed `canonicalMetrics`
- preserved `notes`

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_read_manual_review_decision_csv`

Expected: FAIL because the CSV reader helper does not exist yet.

- [ ] **Step 3: Write a failing test for invalid decision rows**

Add a test covering:

- missing `canonical_metrics` for `PROMOTE`
- non-empty `canonical_metrics` for `IGNORE`
- unknown `business_decision`

Assert validation raises a descriptive error.

- [ ] **Step 4: Run the targeted validation test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_validate_manual_review_decision_rows`

Expected: FAIL because the validation helper does not exist yet.

### Task 2: Lock Execution Routing with Tests

**Files:**
- Modify: `scripts/test_manage_risk_point_pending_governance.py`

- [ ] **Step 1: Write a failing test for decision-to-request mapping**

Add a test that passes three validated decision rows into a request builder and asserts:

- `PROMOTE` builds the same `metrics[] + completePending + promotionNote` shape used by current `promote`
- `IGNORE` builds `ignoreNote`
- `KEEP_PENDING` builds only `resolutionNote`

- [ ] **Step 2: Run the targeted mapping test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_build_manual_review_decision_actions`

Expected: FAIL because the action builder does not exist yet.

- [ ] **Step 3: Write a failing CLI dry-run test**

Add a test that writes a temp decision CSV and calls:

`python3 scripts/manage-risk-point-pending-governance.py apply-manual-review-decisions --csv <temp-csv>`

Assert:

- command exits `0`
- stdout contains `dryRun=True`
- stdout shows `PROMOTE`, `IGNORE`, and `KEEP_PENDING`

- [ ] **Step 4: Run the targeted CLI test to verify it fails**

Run: `python3 -m unittest scripts.test_manage_risk_point_pending_governance.PendingBatchGovernanceScriptTest.test_cli_apply_manual_review_decisions_dry_run`

Expected: FAIL because the subcommand does not exist yet.

### Task 3: Implement Decision CSV Support

**Files:**
- Modify: `scripts/manage-risk-point-pending-governance.py`

- [ ] **Step 1: Add CSV parsing and normalization helpers**

Implement helpers to:

- read a decision CSV
- normalize `pending_id`
- normalize `business_decision`
- split `canonical_metrics`

- [ ] **Step 2: Add validation helpers**

Implement validation rules for:

- allowed decisions
- required/forbidden `canonical_metrics`
- optional manifest cross-check

- [ ] **Step 3: Add decision action builders**

Implement helpers that convert one decision row into:

- `PROMOTE` request body
- `IGNORE` request body
- `KEEP_PENDING` note update payload

- [ ] **Step 4: Add subcommand wiring**

Extend `argparse` with:

- `apply-manual-review-decisions`

Arguments:

- `--csv`
- `--manifest`
- `--pending-ids`
- `--limit`
- `--apply`
- `--result-output`
- login arguments
- connection arguments

- [ ] **Step 5: Add execution engine**

Implement one execution path that:

- routes `PROMOTE` to the existing promote API
- routes `IGNORE` to the existing ignore API
- routes `KEEP_PENDING` to the existing note-update logic
- defaults to dry-run
- writes result JSON

- [ ] **Step 6: Update export CSV columns**

Ensure `export-manual-review` includes blank execution columns for future filling:

- `business_decision`
- `canonical_metrics`
- `owner`
- `due_date`
- `notes`

- [ ] **Step 7: Run the full script unit tests**

Run: `python3 -m unittest scripts/test_manage_risk_point_pending_governance.py`

Expected: PASS

### Task 4: Update Docs

**Files:**
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`

- [ ] **Step 1: Document the new decision CSV command**

Explain:

- `apply-manual-review-decisions`
- supported decisions
- dry-run default
- apply guardrail

- [ ] **Step 2: Document the CSV filling contract**

Clarify:

- `PROMOTE` requires `canonical_metrics`
- `IGNORE / KEEP_PENDING` must keep `canonical_metrics` empty

- [ ] **Step 3: Record the change summary**

Add a `2026-04-04` changelog entry for this round.

### Task 5: Verify Against Real Residual CSV

**Files:**
- Modify: none

- [ ] **Step 1: Regenerate a fresh manual-review CSV with execution columns**

Run:

`python3 scripts/manage-risk-point-pending-governance.py export-manual-review --manifest /tmp/risk-point-pending-operational-manifest-residual-20260404.json --output-prefix /tmp/risk-point-pending-decision-csv-20260404`

Expected:

- CSV generated
- execution columns present but empty

- [ ] **Step 2: Run decision apply in dry-run mode against the unfilled CSV**

Run:

`python3 scripts/manage-risk-point-pending-governance.py apply-manual-review-decisions --csv /tmp/risk-point-pending-decision-csv-20260404.csv --manifest /tmp/risk-point-pending-operational-manifest-residual-20260404.json`

Expected:

- command exits `0`
- rows with empty `business_decision` are skipped

- [ ] **Step 3: Create a tiny temp decision CSV with one row per decision type**

Use a temp CSV outside the repo to simulate:

- one `PROMOTE`
- one `IGNORE`
- one `KEEP_PENDING`

Run dry-run only.

- [ ] **Step 4: Run decision apply dry-run against the temp CSV**

Expected:

- all three decisions preview correctly
- no real writes occur

- [ ] **Step 5: Summarize changed files and evidence**

Report:

- changed script, tests, docs
- dry-run evidence paths
- whether README.md / AGENTS.md changed
