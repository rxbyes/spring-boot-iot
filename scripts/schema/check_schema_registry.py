from __future__ import annotations

import sys
from pathlib import Path


if str(Path(__file__).resolve().parents[2]) not in sys.path:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from scripts.schema.load_registry import load_registry
from scripts.schema.render_artifacts import REPO_ROOT, check_artifacts


def main() -> int:
    schema_root = REPO_ROOT / "schema"
    load_registry(schema_root)
    mismatches = check_artifacts(schema_root)
    if mismatches:
        for path in mismatches:
            print(f"OUT_OF_DATE {path.relative_to(REPO_ROOT)}")
        return 1
    print("Schema registry is valid and generated artifacts are up to date.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
