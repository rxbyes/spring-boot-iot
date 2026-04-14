import pathlib
import unittest


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SQL_FILES = [
    REPO_ROOT / "sql" / "init.sql",
    REPO_ROOT / "sql" / "init-tdengine.sql",
    REPO_ROOT / "sql" / "init-data.sql",
]
JSON_FILES = sorted((REPO_ROOT / "schema").rglob("*.json"))
MARKDOWN_FILES = [
    REPO_ROOT / "docs" / "appendix" / "database-schema-object-catalog.generated.md",
]

MOJIBAKE_MARKERS = (
    "�",
    "鐪熷",
    "鍩虹",
    "绉熸",
    "璁惧",
    "鍛婅",
    "椋庨",
    "閾捐",
    "寮€",
    "锛",
    "銆",
    "鈥",
)


class SqlUtf8IntegrityTest(unittest.TestCase):
    def test_repository_schema_artifacts_are_utf8_and_free_of_known_mojibake_markers(self):
        paths = [*SQL_FILES, *JSON_FILES, *MARKDOWN_FILES]
        for path in paths:
            with self.subTest(path=path.name):
                self.assertTrue(path.exists(), msg=f"Expected artifact file is missing: {path}")
                content = path.read_text(encoding="utf-8")
                for marker in MOJIBAKE_MARKERS:
                    self.assertNotIn(
                        marker,
                        content,
                        msg=f"{path.name} still contains mojibake marker: {marker}",
                    )


if __name__ == "__main__":
    unittest.main()
