import pathlib
import unittest


INIT_DATA_SQL = pathlib.Path(__file__).resolve().parents[2] / "sql" / "init-data.sql"


class DictSeedSnapshotTest(unittest.TestCase):
    def test_seed_contains_current_level_dict_definitions(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("'risk_point_level'", content)
        self.assertIn("'level_1'", content)
        self.assertIn("'alarm_level'", content)
        self.assertIn("'red'", content)
        self.assertIn("'risk_level'", content)
        self.assertIn("'黄色'", content)

    def test_seed_soft_deletes_duplicate_level_dict_rows_and_non_target_items(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("UPDATE sys_dict", content)
        self.assertIn(
            "dict_code IN ('risk_point_level', 'alarm_level', 'risk_level', 'help_doc_category', 'notification_channel_type')",
            content,
        )
        self.assertIn("UPDATE sys_dict_item", content)
        self.assertIn("item_value NOT IN ('level_1', 'level_2', 'level_3')", content)
        self.assertIn("item_value NOT IN ('red', 'orange', 'yellow', 'blue')", content)

    def test_seed_contains_system_governance_dict_definitions(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("(7204, 1, '帮助文档分类', 'help_doc_category'", content)
        self.assertIn("(7205, 1, '通知渠道类型', 'notification_channel_type'", content)
        self.assertIn("(7312, 1, 7204, '业务类', 'business'", content)
        self.assertIn("(7313, 1, 7204, '技术类', 'technical'", content)
        self.assertIn("(7314, 1, 7204, '常见问题', 'faq'", content)
        self.assertIn("(7315, 1, 7205, '邮件', 'email'", content)
        self.assertIn("(7316, 1, 7205, '短信', 'sms'", content)
        self.assertIn("(7317, 1, 7205, 'Webhook', 'webhook'", content)
        self.assertIn("(7318, 1, 7205, '微信', 'wechat'", content)
        self.assertIn("(7319, 1, 7205, '飞书', 'feishu'", content)
        self.assertIn("(7320, 1, 7205, '钉钉', 'dingtalk'", content)

    def test_seed_soft_deletes_non_target_system_governance_dict_items(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn(
            "dict_code IN ('risk_point_level', 'alarm_level', 'risk_level', 'help_doc_category', 'notification_channel_type')",
            content,
        )
        self.assertIn("AND id NOT IN (7201, 7202, 7203, 7204, 7205)", content)
        self.assertIn(
            "OR (dict_id = 7204 AND item_value NOT IN ('business', 'technical', 'faq'))",
            content,
        )
        self.assertIn(
            "OR (dict_id = 7205 AND item_value NOT IN ('email', 'sms', 'webhook', 'wechat', 'feishu', 'dingtalk'))",
            content,
        )


if __name__ == "__main__":
    unittest.main()
