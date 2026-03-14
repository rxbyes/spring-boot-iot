USE rm_iot;

INSERT INTO sys_tenant (id, tenant_name, tenant_code, status, create_time, update_time, deleted)
VALUES (1, '默认租户', 'default', 1, NOW(), NOW(), 0);

INSERT INTO sys_role (id, tenant_id, role_name, role_code, status, create_time, update_time, deleted)
VALUES (1, 1, '超级管理员', 'SUPER_ADMIN', 1, NOW(), NOW(), 0);

INSERT INTO sys_user (
    id, tenant_id, username, password, nickname, real_name, status, is_admin, create_time, update_time, deleted
) VALUES (
    1, 1, 'admin', '$2a$10$replace_with_bcrypt_password', '管理员', '系统管理员', 1, 1, NOW(), NOW(), 0
);

INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time)
VALUES (1, 1, 1, 1, NOW());

INSERT INTO iot_product (
    id, tenant_id, product_key, product_name, protocol_code, node_type, data_format, status, create_time, update_time, deleted
) VALUES (
    1001, 1, 'demo-product', '演示产品', 'mqtt-json', 1, 'JSON', 1, NOW(), NOW(), 0
);

INSERT INTO iot_device (
    id, tenant_id, product_id, device_name, device_code, device_secret, protocol_code,
    node_type, online_status, activate_status, device_status, create_time, update_time, deleted
) VALUES (
    2001, 1, 1001, '演示设备-01', 'demo-device-01', '123456',
    'mqtt-json', 1, 0, 1, 1, NOW(), NOW(), 0
);

INSERT INTO iot_product_model (
    id, tenant_id, product_id, model_type, identifier, model_name, data_type, specs_json,
    sort_no, required_flag, description, create_time, update_time, deleted
) VALUES
(3001, 1, 1001, 'property', 'temperature', '温度', 'double', JSON_OBJECT('unit','℃'), 1, 0, '温度属性', NOW(), NOW(), 0),
(3002, 1, 1001, 'property', 'humidity', '湿度', 'double', JSON_OBJECT('unit','%'), 2, 0, '湿度属性', NOW(), NOW(), 0);
