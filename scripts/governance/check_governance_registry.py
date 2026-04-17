from __future__ import annotations

from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.governance.render_governance_docs import check_governance_docs


def main() -> int:
    mismatches = check_governance_docs(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    if mismatches:
        for path in mismatches:
            print(f"OUT_OF_DATE {path.relative_to(REPO_ROOT)}")
        return 1
    print("governance registry check passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
