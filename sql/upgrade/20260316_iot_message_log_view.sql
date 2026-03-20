-- Primary naming: iot_message_log
-- Compatibility: keep physical table as iot_device_message_log for now, expose view iot_message_log.
-- This keeps existing Phase 1-3 main flow stable while letting new code standardize on iot_message_log.

DROP VIEW IF EXISTS iot_message_log;
CREATE VIEW iot_message_log AS
SELECT * FROM iot_device_message_log;

