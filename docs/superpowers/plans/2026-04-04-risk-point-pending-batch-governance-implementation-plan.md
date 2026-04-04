# Risk Point Pending Batch Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a script-driven batch governance workflow for `/risk-point` pending records, including manifest generation, summary output, and dry-run-first promote/ignore execution.

**Architecture:** Implement a standalone Python script in `scripts/` that reuses the real dev baseline from `application-dev.yml`, scans pending rows and candidate APIs to build an operational manifest, then consumes that manifest through `plan`, `promote`, and `ignore` subcommands. Keep classification logic as pure functions so unit tests can lock the current monitoring-family governance heuristics without requiring network access.

**Tech Stack:** Python 3, argparse, json, urllib.request, pymysql, unittest

---

## File Map

- Create: `scripts/manage-risk-point-pending-governance.py`
  - Batch governance CLI for manifest generation and dry-run/apply execution.
- Create: `scripts/test_manage_risk_point_pending_governance.py`
  - Unit tests for family inference, manifest classification, payload construction, and CLI plan mode.
- Modify: `docs/02-业务功能与流程说明.md`
  - Document script-assisted pending governance workflow.
- Modify: `docs/07-部署运行与配置说明.md`
  - Document script usage and dry-run/apply guardrail.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Record the new batch governance tooling.

## Task 1: Lock the Classification Rules with Tests

**Files:**
- Create: `scripts/test_manage_risk_point_pending_governance.py`

- [ ] **Step 1: Write unit tests for manifest classification**

Cover at least these cases:

- `多维位移监测仪 + gX/gY/gZ` -> `promote_candidates`
- `GNSS位移监测仪 + gpsTotalX/gpsTotalY/gpsTotalZ + extra orientation fields` -> promote only `gpsTotalX/Y/Z`
- `雨量计 + temp/totalValue/value` -> promote only `value/totalValue`
- `声光报警系统` -> `exclude_candidates`
- `激光测距仪 + empty` -> `need_runtime_evidence`

- [ ] **Step 2: Add a CLI smoke test for `plan`**

Use a temp manifest JSON and assert the subprocess output contains the four bucket counts.

- [ ] **Step 3: Run the script test file**

Run: `python3 -m unittest scripts/test_manage_risk_point_pending_governance.py`

Expected: FAIL before implementation exists.

## Task 2: Implement the Batch Governance Script

**Files:**
- Create: `scripts/manage-risk-point-pending-governance.py`

- [ ] **Step 1: Add shared env/login/db helpers**

Implement:

- real dev MySQL resolution from `application-dev.yml`
- login to `/api/auth/login`
- pending row query
- candidate API fetch

- [ ] **Step 2: Add pure classification helpers**

Implement focused helpers for:

- family inference
- exclude keyword detection
- manifest bucket assignment
- promote payload generation
- ignore payload generation

- [ ] **Step 3: Add CLI subcommands**

Implement:

- `build-manifest`
- `plan`
- `promote`
- `ignore`

All write-side subcommands must default to dry-run and require `--apply` for real execution.

- [ ] **Step 4: Persist JSON outputs**

Ensure manifest generation and promote/ignore execution can write JSON output files for later review.

- [ ] **Step 5: Run the script unit tests**

Run: `python3 -m unittest scripts/test_manage_risk_point_pending_governance.py`

Expected: PASS

## Task 3: Update Existing Docs

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Document batch governance workflow**

Explain that pending治理 now has a script-assisted batch path built on top of existing single-record promote/ignore APIs.

- [ ] **Step 2: Document operational safety**

Explain `dry-run` default and explicit `--apply` guardrail.

- [ ] **Step 3: Record the change summary**

Update the changelog entry for this round.

## Task 4: Verify with Real Dry-Run Evidence

**Files:**
- Modify: none

- [ ] **Step 1: Build a fresh manifest in real dev**

Run the new script in `build-manifest` mode and write the output to a temp JSON file.

- [ ] **Step 2: Run `plan` on the generated manifest**

Verify the printed counts align with the current pending inventory.

- [ ] **Step 3: Run `promote` in dry-run mode**

Use a small `--limit` batch and verify the payload only contains重点测点。

- [ ] **Step 4: Run `ignore` in dry-run mode**

Use a small `--limit` batch and verify the ignore payload is generated for non-monitoring devices.

- [ ] **Step 5: Summarize operational next step**

Report:

- which files changed
- which docs changed
- the dry-run evidence
- whether README.md / AGENTS.md were updated
