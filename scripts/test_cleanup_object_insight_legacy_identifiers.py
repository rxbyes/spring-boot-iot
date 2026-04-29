#!/usr/bin/env python3
"""Tests for cleanup-object-insight-legacy-identifiers.py."""

from __future__ import annotations

import importlib.util
import pathlib
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parent / "cleanup-object-insight-legacy-identifiers.py"
SPEC = importlib.util.spec_from_file_location("cleanup_object_insight", SCRIPT_PATH)
cleanup_script = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(cleanup_script)


class ResolveIdentifierDetailTest(unittest.TestCase):
    def test_resolve_identifier_detail_marks_short_tail_as_ambiguous(self):
        detail = cleanup_script.resolve_identifier_detail(
            "gX",
            ["L1_JS_1.gX", "L2_JS_3.gX"],
        )
        self.assertIsNone(detail["resolvedIdentifier"])
        self.assertEqual("ambiguous", detail["source"])
        self.assertEqual(["L1_JS_1.gX", "L2_JS_3.gX"], detail["tailMatches"])

    def test_resolve_identifier_detail_keeps_exact_full_path(self):
        detail = cleanup_script.resolve_identifier_detail(
            "L1_JS_1.gX",
            ["L1_JS_1.gX", "L2_JS_3.gX"],
        )
        self.assertEqual("L1_JS_1.gX", detail["resolvedIdentifier"])
        self.assertEqual("exact", detail["source"])


class GovernanceRecommendationTest(unittest.TestCase):
    def test_unresolved_without_runtime_evidence_recommends_deprecate(self):
        decision = cleanup_script.classify_governance_action(
            {"identifier": "gX", "source": "unresolved", "publishedTailMatches": []},
            {
                "candidateIdentifiers": [],
                "canonicalSuggestions": [],
                "evidenceTotalHits": 0,
            },
        )
        self.assertEqual("recommend_deprecate", decision["recommendedAction"])
        self.assertEqual("no_runtime_evidence", decision["reasonCode"])

    def test_unresolved_with_single_runtime_identifier_recommends_publish(self):
        decision = cleanup_script.classify_governance_action(
            {"identifier": "gX", "source": "unresolved", "publishedTailMatches": []},
            {
                "candidateIdentifiers": ["L1_JS_1.gX"],
                "canonicalSuggestions": [],
                "evidenceTotalHits": 10,
            },
        )
        self.assertEqual("recommend_publish", decision["recommendedAction"])
        self.assertEqual("L1_JS_1.gX", decision["targetIdentifier"])

    def test_unresolved_full_path_prefers_exact_runtime_identifier(self):
        decision = cleanup_script.classify_governance_action(
            {"identifier": "S1_ZT_1.signal_4g", "source": "unresolved", "publishedTailMatches": []},
            {
                "candidateIdentifiers": ["S1_ZT_1.signal_4g", "L1_JS_1.signal_4g"],
                "canonicalSuggestions": [],
                "evidenceTotalHits": 5,
            },
        )
        self.assertEqual("recommend_publish", decision["recommendedAction"])
        self.assertEqual("S1_ZT_1.signal_4g", decision["targetIdentifier"])

    def test_ambiguous_issue_requires_manual_decision(self):
        decision = cleanup_script.classify_governance_action(
            {"identifier": "gX", "source": "ambiguous", "publishedTailMatches": ["L1_JS_1.gX", "L2_JS_3.gX"]},
            {
                "candidateIdentifiers": ["L1_JS_1.gX"],
                "canonicalSuggestions": [],
                "evidenceTotalHits": 9,
            },
        )
        self.assertEqual("needs_manual_decision", decision["recommendedAction"])
        self.assertEqual("ambiguous_published_tail_match", decision["reasonCode"])


if __name__ == "__main__":
    unittest.main()
