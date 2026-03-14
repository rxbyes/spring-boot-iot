DROP TABLE IF EXISTS iot_device_message_log;
DROP TABLE IF EXISTS iot_device_property;
DROP TABLE IF EXISTS iot_device;
DROP TABLE IF EXISTS iot_product_model;
DROP TABLE IF EXISTS iot_product;

CREATE TABLE iot_product (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_key VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    protocol_code VARCHAR(64) NOT NULL,
    node_type TINYINT NOT NULL DEFAULT 1,
    data_format VARCHAR(32) NOT NULL DEFAULT 'JSON',
    manufacturer VARCHAR(128),
    description VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_key_tenant UNIQUE (tenant_id, product_key)
);

CREATE TABLE iot_product_model (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_id BIGINT NOT NULL,
    model_type VARCHAR(32) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    specs_json VARCHAR(2000),
    event_type VARCHAR(32),
    service_input_json VARCHAR(2000),
    service_output_json VARCHAR(2000),
    sort_no INT NOT NULL DEFAULT 0,
    required_flag TINYINT NOT NULL DEFAULT 0,
    description VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_identifier UNIQUE (product_id, model_type, identifier)
);

CREATE TABLE iot_device (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_id BIGINT NOT NULL,
    gateway_id BIGINT,
    parent_device_id BIGINT,
    device_name VARCHAR(128) NOT NULL,
    device_code VARCHAR(64) NOT NULL,
    device_secret VARCHAR(128),
    client_id VARCHAR(128),
    username VARCHAR(128),
    password VARCHAR(128),
    protocol_code VARCHAR(64) NOT NULL,
    node_type TINYINT NOT NULL DEFAULT 1,
    online_status TINYINT NOT NULL DEFAULT 0,
    activate_status TINYINT NOT NULL DEFAULT 0,
    device_status TINYINT NOT NULL DEFAULT 1,
    firmware_version VARCHAR(64),
    ip_address VARCHAR(64),
    last_online_time TIMESTAMP,
    last_offline_time TIMESTAMP,
    last_report_time TIMESTAMP,
    longitude DECIMAL(10, 6),
    latitude DECIMAL(10, 6),
    address VARCHAR(255),
    metadata_json VARCHAR(2000),
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_device_code_tenant UNIQUE (tenant_id, device_code)
);

CREATE TABLE iot_device_property (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    device_id BIGINT NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    property_name VARCHAR(128),
    property_value VARCHAR(1024),
    value_type VARCHAR(32),
    report_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_device_identifier UNIQUE (device_id, identifier)
);

CREATE TABLE iot_device_message_log (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    device_id BIGINT NOT NULL,
    product_id BIGINT,
    message_type VARCHAR(32) NOT NULL,
    topic VARCHAR(255),
    payload VARCHAR(4000),
    report_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
