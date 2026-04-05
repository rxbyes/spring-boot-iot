import importlib.util
import pathlib
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "run-message-flow-acceptance.py"
SPEC = importlib.util.spec_from_file_location("message_flow_acceptance", SCRIPT_PATH)
message_flow_acceptance = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(message_flow_acceptance)


class ClassifyMqttCorrelationCountsTest(unittest.TestCase):
    def test_accepts_published_and_matched_for_single_runtime(self):
        result = message_flow_acceptance.classify_mqtt_correlation_counts(
            {"published": "1", "matched": "1"}
        )

        self.assertEqual("published_and_matched", result)

    def test_accepts_published_only_for_shared_dev_runtime(self):
        result = message_flow_acceptance.classify_mqtt_correlation_counts(
            {"published": "1"}
        )

        self.assertEqual("published_only_current_runtime", result)

    def test_rejects_missing_published_baseline(self):
        with self.assertRaises(message_flow_acceptance.AcceptanceError):
            message_flow_acceptance.classify_mqtt_correlation_counts({"matched": "1"})


if __name__ == "__main__":
    unittest.main()
