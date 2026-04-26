# Database Schema Governance Catalog

Generated from the schema-governance registry. Do not edit by hand.

| Domain | Object | Stage | Seed Packages | Audit Profile | Owner Module | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| alarm | risk_point_highway_detail | archived | highway_archived_risk_points_seed | mysql_archived_object_with_seed | spring-boot-iot-alarm | 高速项目归档观察对象，不进入默认主链路。 |
| device | iot_message_log | freeze_candidate | - | mysql_hot_table_with_cold_archive | spring-boot-iot-device | 设备消息日志热表进入冷归档治理阶段，保留 active 热表定位，不直接退场。 |
