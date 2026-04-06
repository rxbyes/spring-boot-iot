package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.service.handler.DeviceContractStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceMessageLogStageHandler;
import com.ghlzm.iot.device.service.handler.DevicePayloadApplyStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceRiskDispatchStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceStateStageHandler;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.MessageTracePayloadRecovery;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
import com.ghlzm.iot.device.vo.DeviceStatsBucketVO;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备上行消息处理服务。
 */
@Service
public class DeviceMessageServiceImpl implements DeviceMessageService {

    private static final Logger log = LoggerFactory.getLogger(DeviceMessageServiceImpl.class);
    private static final String TABLE_NAME = "iot_device_message_log";
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long UNKNOWN_DEVICE_ID = 0L;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DISPATCH_FAILED_MESSAGE_TYPE = "dispatch_failed";
    private static final String MQTT_JSON_PROTOCOL_CODE = "mqtt-json";
    private static final List<String> COMMAND_ID_ALIASES = List.of("commandId", "messageId");
    private static final List<String> ERROR_MESSAGE_ALIASES = List.of("errorMessage", "error", "msg", "message");

    private final DeviceMapper deviceMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;
    private final CommandRecordService commandRecordService;
    private final DeviceFileService deviceFileService;
    private final DeviceOnlineSessionService deviceOnlineSessionService;
    private final JdbcTemplate jdbcTemplate;
    private final IotProperties iotProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceContractStageHandler deviceContractStageHandler;
    private final DeviceMessageLogStageHandler deviceMessageLogStageHandler;
    private final DevicePayloadApplyStageHandler devicePayloadApplyStageHandler;
    private final DeviceStateStageHandler deviceStateStageHandler;
    private final DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler;
    private final PermissionService permissionService;
    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceMessageServiceImpl(DeviceMapper deviceMapper,
                                    DeviceMessageLogMapper deviceMessageLogMapper,
                                    DevicePropertyMapper devicePropertyMapper,
                                    ProductMapper productMapper,
                                    ProductModelMapper productModelMapper,
                                    CommandRecordService commandRecordService,
                                    DeviceFileService deviceFileService,
                                    DeviceOnlineSessionService deviceOnlineSessionService,
                                    JdbcTemplate jdbcTemplate,
                                    IotProperties iotProperties,
                                    ApplicationEventPublisher eventPublisher,
                                    DeviceContractStageHandler deviceContractStageHandler,
                                    DeviceMessageLogStageHandler deviceMessageLogStageHandler,
                                    DevicePayloadApplyStageHandler devicePayloadApplyStageHandler,
                                    DeviceStateStageHandler deviceStateStageHandler,
                                    DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler,
                                    PermissionService permissionService,
                                    ProtocolAdapterRegistry protocolAdapterRegistry) {
        this.deviceMapper = deviceMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
        this.commandRecordService = commandRecordService;
        this.deviceFileService = deviceFileService;
        this.deviceOnlineSessionService = deviceOnlineSessionService;
        this.jdbcTemplate = jdbcTemplate;
        this.iotProperties = iotProperties;
        this.eventPublisher = eventPublisher;
        this.deviceContractStageHandler = deviceContractStageHandler;
        this.deviceMessageLogStageHandler = deviceMessageLogStageHandler;
        this.devicePayloadApplyStageHandler = devicePayloadApplyStageHandler;
        this.deviceStateStageHandler = deviceStateStageHandler;
        this.deviceRiskDispatchStageHandler = deviceRiskDispatchStageHandler;
        this.permissionService = permissionService;
        this.protocolAdapterRegistry = protocolAdapterRegistry;
    }

    @Override
    public List<DeviceMessageLog> listMessageLogs(Long currentUserId, String deviceCode) {
        Device device = findDeviceByCode(deviceCode);
        if (device == null) {
            throw new BizException("设备不存在: " + deviceCode);
        }
        ensureAccessibleDevice(currentUserId, device);
        Long tenantId = resolveScopedTenantId(currentUserId);
        return deviceMessageLogMapper.selectList(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(tenantId != null, DeviceMessageLog::getTenantId, tenantId)
                        .eq(DeviceMessageLog::getDeviceId, device.getId())
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .last("limit 20")
        );
    }

    @Override
    public PageResult<DeviceMessageLog> pageMessageTraceLogs(Long currentUserId,
                                                             DeviceMessageTraceQuery query,
                                                             Integer pageNum,
                                                             Integer pageSize) {
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<DeviceMessageLog> page = new Page<>(safePageNum, safePageSize);
        Page<DeviceMessageLog> result = deviceMessageLogMapper.selectPage(
                page,
                buildMessageTraceQueryWrapper(query, currentUserId)
        );
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public DeviceMessageTraceStatsVO getMessageTraceStats(Long currentUserId, DeviceMessageTraceQuery query) {
        DeviceMessageTraceStatsVO stats = new DeviceMessageTraceStatsVO();
        QuerySpec querySpec = buildMessageTraceQuerySpec(query, currentUserId);

        stats.setTotal(queryCount(querySpec, null));
        if (stats.getTotal() == null || stats.getTotal() < 1) {
            return stats;
        }

        stats.setRecentHourCount(queryRecentCount(querySpec, 1));
        stats.setRecent24HourCount(queryRecentCount(querySpec, 24));
        stats.setDistinctTraceCount(queryDistinctCount(querySpec, "trace_id"));
        stats.setDistinctDeviceCount(queryDistinctCount(querySpec, "device_code"));
        stats.setDispatchFailureCount(queryCount(querySpec, " AND message_type = ?", DISPATCH_FAILED_MESSAGE_TYPE));
        stats.setTopMessageTypes(queryTopBuckets(querySpec, "message_type"));
        stats.setTopProductKeys(queryTopBuckets(querySpec, "product_key"));
        stats.setTopDeviceCodes(queryTopBuckets(querySpec, "device_code"));
        stats.setTopTopics(queryTopBuckets(querySpec, "topic"));
        return stats;
    }

    @Override
    public MessageTraceDetailVO getMessageTraceDetail(Long currentUserId, Long id) {
        if (id == null) {
            throw new BizException("消息日志不存在或无权访问");
        }
        LambdaQueryWrapper<DeviceMessageLog> queryWrapper = buildMessageTraceQueryWrapper(null, currentUserId);
        queryWrapper.eq(DeviceMessageLog::getId, id).last("limit 1");
        DeviceMessageLog logRecord = deviceMessageLogMapper.selectOne(queryWrapper);
        if (logRecord == null) {
            throw new BizException("消息日志不存在或无权访问");
        }

        MessageTraceDetailVO detail = new MessageTraceDetailVO();
        detail.setId(logRecord.getId());
        detail.setTraceId(logRecord.getTraceId());
        detail.setDeviceCode(logRecord.getDeviceCode());
        detail.setProductKey(logRecord.getProductKey());
        detail.setMessageType(logRecord.getMessageType());
        detail.setTopic(logRecord.getTopic());
        detail.setReportTime(logRecord.getReportTime());
        detail.setCreateTime(logRecord.getCreateTime());

        MessageTracePayloadRecovery recovery = recoverPayloadComparison(logRecord);
        detail.setRawPayload(recovery.getRawPayload());
        detail.setDecryptedPayload(recovery.getDecryptedPayload());
        detail.setDecodedPayload(recovery.getDecodedPayload());
        detail.setProtocolMetadata(recovery.getProtocolMetadata());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleUpMessage(DeviceUpMessage upMessage) {
        DeviceProcessingTarget target = deviceContractStageHandler.resolve(upMessage);
        deviceMessageLogStageHandler.save(target);
        devicePayloadApplyStageHandler.apply(target);
        deviceStateStageHandler.refresh(target);
        deviceRiskDispatchStageHandler.dispatch(target);
        handleChildMessages(upMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordDispatchFailureTrace(String topic, byte[] payload, RawDeviceMessage rawDeviceMessage) {
        String traceId = rawDeviceMessage == null ? null : rawDeviceMessage.getTraceId();
        if (!hasText(traceId)) {
            traceId = TraceContextHolder.currentOrCreate();
        }

        String deviceCode = rawDeviceMessage == null ? null : rawDeviceMessage.getDeviceCode();
        String productKey = rawDeviceMessage == null ? null : rawDeviceMessage.getProductKey();
        Device device = hasText(deviceCode) ? findDeviceByCode(deviceCode) : null;
        Product product = hasText(productKey) ? findProductByKey(productKey) : null;

        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setTenantId(resolveTenantId(rawDeviceMessage, device));
        // 前置校验失败时设备可能不存在，使用 0 作为占位，保证轨迹记录可落库可检索。
        logRecord.setDeviceId(device == null ? UNKNOWN_DEVICE_ID : device.getId());
        logRecord.setProductId(resolveProductId(device, product));
        logRecord.setTraceId(traceId);
        logRecord.setDeviceCode(deviceCode);
        logRecord.setProductKey(productKey);
        logRecord.setMessageType(DISPATCH_FAILED_MESSAGE_TYPE);
        logRecord.setTopic(topic);
        logRecord.setPayload(resolvePayloadText(payload));
        logRecord.setReportTime(LocalDateTime.now());
        logRecord.setCreateTime(LocalDateTime.now());
        deviceMessageLogMapper.insert(logRecord);
    }

    private Product getRequiredProduct(Device device) {
        if (device.getProductId() == null) {
            throw new BizException("设备未绑定产品: " + device.getDeviceCode());
        }
        Product product = productMapper.selectById(device.getProductId());
        if (product == null || Integer.valueOf(1).equals(product.getDeleted())) {
            throw new BizException("设备所属产品不存在: " + device.getDeviceCode());
        }
        return product;
    }

    private void ensureProductEnabledForAccess(Product product) {
        if (product != null && ProductStatusEnum.DISABLED.getCode().equals(product.getStatus())) {
            throw new BizException("产品已停用，拒绝设备接入: " + product.getProductKey());
        }
    }

    private void validateProductMatched(DeviceUpMessage upMessage, Device device, Product product) {
        String expectedProductKey = normalizeText(product == null ? null : product.getProductKey());
        String actualProductKey = hasText(upMessage.getProductKey())
                ? upMessage.getProductKey().trim()
                : expectedProductKey;
        if (!hasText(expectedProductKey) || !hasText(actualProductKey)
                || !expectedProductKey.equalsIgnoreCase(actualProductKey)) {
            throw new BizException("设备所属产品不匹配: " + device.getDeviceCode()
                    + ", expected=" + displayText(expectedProductKey)
                    + ", actual=" + displayText(actualProductKey));
        }
        upMessage.setProductKey(expectedProductKey);
    }

    private void validateProtocolMatched(DeviceUpMessage upMessage, Device device, Product product) {
        String deviceProtocolCode = normalizeText(device == null ? null : device.getProtocolCode());
        String productProtocolCode = normalizeText(product == null ? null : product.getProtocolCode());
        if (hasText(deviceProtocolCode) && hasText(productProtocolCode)
                && !deviceProtocolCode.equalsIgnoreCase(productProtocolCode)) {
            throw new BizException("设备协议配置异常: " + device.getDeviceCode()
                    + ", deviceProtocol=" + deviceProtocolCode
                    + ", productProtocol=" + productProtocolCode);
        }

        String expectedProtocolCode = hasText(deviceProtocolCode) ? deviceProtocolCode : productProtocolCode;
        String actualProtocolCode = hasText(upMessage.getProtocolCode())
                ? upMessage.getProtocolCode().trim()
                : expectedProtocolCode;
        if (!hasText(expectedProtocolCode)) {
            throw new BizException("设备接入协议未配置: " + device.getDeviceCode()
                    + ", deviceProtocol=" + displayText(deviceProtocolCode)
                    + ", productProtocol=" + displayText(productProtocolCode));
        }
        if (!hasText(actualProtocolCode) || !expectedProtocolCode.equalsIgnoreCase(actualProtocolCode)) {
            throw new BizException("设备协议不匹配: " + device.getDeviceCode()
                    + ", expected=" + expectedProtocolCode
                    + ", actual=" + displayText(actualProtocolCode));
        }
        upMessage.setProtocolCode(expectedProtocolCode);
    }

    private Device findDeviceByCode(String deviceCode) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, deviceCode)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private Product findProductByKey(String productKey) {
        return productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getProductKey, productKey)
                        .eq(Product::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private Long resolveTenantId(RawDeviceMessage rawDeviceMessage, Device device) {
        if (device != null && device.getTenantId() != null) {
            return device.getTenantId();
        }
        if (rawDeviceMessage == null || !hasText(rawDeviceMessage.getTenantId())) {
            return DEFAULT_TENANT_ID;
        }
        try {
            return Long.parseLong(rawDeviceMessage.getTenantId().trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_TENANT_ID;
        }
    }

    private Long resolveProductId(Device device, Product product) {
        if (device != null && device.getProductId() != null) {
            return device.getProductId();
        }
        return product == null ? null : product.getId();
    }

    private String resolvePayloadText(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        String decoded = tryDecodeUtf8(payload);
        if (decoded != null) {
            String normalizedJson = tryNormalizeJsonPayload(decoded);
            if (hasText(normalizedJson)) {
                return normalizedJson;
            }
            return wrapNonJsonPayload("UTF-8", decoded, payload);
        }
        return wrapNonJsonPayload("BASE64", null, payload);
    }

    private String tryNormalizeJsonPayload(String payloadText) {
        String candidate = JsonPayloadUtils.normalizeJsonDocument(payloadText);
        if (!hasText(candidate)) {
            return null;
        }
        try {
            objectMapper.readTree(candidate);
            return candidate;
        } catch (Exception ex) {
            return null;
        }
    }

    private String wrapNonJsonPayload(String encoding, String decodedPayload, byte[] originalPayload) {
        Map<String, Object> payloadWrapper = new LinkedHashMap<>();
        payloadWrapper.put("encoding", encoding);
        if (decodedPayload != null) {
            payloadWrapper.put("rawText", decodedPayload);
            String jsonCandidate = JsonPayloadUtils.normalizeJsonDocument(decodedPayload);
            if (hasText(jsonCandidate) && !jsonCandidate.equals(decodedPayload.trim())) {
                payloadWrapper.put("jsonCandidate", jsonCandidate);
            }
        }
        payloadWrapper.put("payloadBase64", Base64.getEncoder().encodeToString(originalPayload));
        try {
            return objectMapper.writeValueAsString(payloadWrapper);
        } catch (Exception ex) {
            return "{\"encoding\":\"BASE64\",\"payloadBase64\":\""
                    + Base64.getEncoder().encodeToString(originalPayload)
                    + "\"}";
        }
    }

    private String tryDecodeUtf8(byte[] payload) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private Long queryCount(QuerySpec querySpec, String extraCondition, Object... extraParams) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM ")
                .append(TABLE_NAME)
                .append(querySpec.whereClause());
        List<Object> params = new ArrayList<>(querySpec.params());
        appendExtraCondition(sql, params, extraCondition, extraParams);
        Long value = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return value == null ? 0L : value;
    }

    private Long queryRecentCount(QuerySpec querySpec, int hours) {
        if (hours < 1) {
            return 0L;
        }
        return queryCount(
                querySpec,
                " AND COALESCE(report_time, create_time) >= DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)"
        );
    }

    private Long queryDistinctCount(QuerySpec querySpec, String column) {
        String sql = "SELECT COUNT(DISTINCT " + column + ") FROM " + TABLE_NAME
                + querySpec.whereClause()
                + " AND " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, querySpec.params().toArray());
        return value == null ? 0L : value;
    }

    private List<DeviceStatsBucketVO> queryTopBuckets(QuerySpec querySpec, String column) {
        String bucketExpression = "TRIM(" + column + ")";
        String sql = "SELECT " + bucketExpression + " AS bucket_value, COUNT(1) AS bucket_count"
                + " FROM " + TABLE_NAME
                + querySpec.whereClause()
                + " AND " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''"
                + " GROUP BY " + bucketExpression
                + " ORDER BY bucket_count DESC, bucket_value ASC"
                + " LIMIT 5";
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DeviceStatsBucketVO(
                        rs.getString("bucket_value"),
                        rs.getString("bucket_value"),
                        rs.getLong("bucket_count")
                ),
                querySpec.params().toArray()
        );
    }

    private void appendExtraCondition(StringBuilder sql, List<Object> params, String extraCondition, Object... extraParams) {
        if (!hasText(extraCondition)) {
            return;
        }
        sql.append(extraCondition);
        if (extraParams == null || extraParams.length == 0) {
            return;
        }
        for (Object extraParam : extraParams) {
            params.add(extraParam);
        }
    }

    private LambdaQueryWrapper<DeviceMessageLog> buildMessageTraceQueryWrapper(DeviceMessageTraceQuery query, Long currentUserId) {
        LambdaQueryWrapper<DeviceMessageLog> queryWrapper = new LambdaQueryWrapper<>();
        Long tenantId = resolveScopedTenantId(currentUserId);
        if (tenantId != null) {
            queryWrapper.eq(DeviceMessageLog::getTenantId, tenantId);
        }
        applyMessageTraceDeviceScope(queryWrapper, currentUserId);
        if (query != null) {
            if (hasText(query.getKeyword())) {
                String keyword = query.getKeyword().trim();
                queryWrapper.and(wrapper -> wrapper.eq(DeviceMessageLog::getTraceId, keyword)
                        .or()
                        .eq(DeviceMessageLog::getDeviceCode, keyword)
                        .or()
                        .eq(DeviceMessageLog::getProductKey, keyword));
            }
            if (StringUtils.hasText(query.getDeviceCode())) {
                queryWrapper.eq(DeviceMessageLog::getDeviceCode, query.getDeviceCode().trim());
            }
            if (StringUtils.hasText(query.getProductKey())) {
                queryWrapper.eq(DeviceMessageLog::getProductKey, query.getProductKey().trim());
            }
            if (StringUtils.hasText(query.getTraceId())) {
                queryWrapper.eq(DeviceMessageLog::getTraceId, query.getTraceId().trim());
            }
            String normalizedMessageType = normalizeTraceMessageType(query.getMessageType());
            if (StringUtils.hasText(normalizedMessageType)) {
                queryWrapper.eq(DeviceMessageLog::getMessageType, normalizedMessageType);
            }
            if (StringUtils.hasText(query.getTopic())) {
                queryWrapper.like(DeviceMessageLog::getTopic, query.getTopic().trim());
            }
        }
        queryWrapper.orderByDesc(DeviceMessageLog::getReportTime)
                .orderByDesc(DeviceMessageLog::getCreateTime);
        return queryWrapper;
    }

    private QuerySpec buildMessageTraceQuerySpec(DeviceMessageTraceQuery query, Long currentUserId) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        Long tenantId = resolveScopedTenantId(currentUserId);
        if (tenantId != null) {
            where.append(" AND tenant_id = ?");
            params.add(tenantId);
        }
        appendAccessibleDeviceScope(where, params, currentUserId);
        if (query == null) {
            return new QuerySpec(where.toString(), params);
        }
        appendTraceKeyword(where, params, query.getKeyword());
        appendTextEquals(where, params, "device_code", query.getDeviceCode());
        appendTextEquals(where, params, "product_key", query.getProductKey());
        appendTextEquals(where, params, "trace_id", query.getTraceId());
        appendTextEquals(where, params, "message_type", normalizeTraceMessageType(query.getMessageType()));
        appendTextLike(where, params, "topic", query.getTopic());
        return new QuerySpec(where.toString(), params);
    }

    private void applyMessageTraceDeviceScope(LambdaQueryWrapper<DeviceMessageLog> queryWrapper, Long currentUserId) {
        String deviceScopeSql = buildAccessibleDeviceIdInSql(currentUserId);
        if (deviceScopeSql == null) {
            return;
        }
        if ("SELECT -1".equals(deviceScopeSql)) {
            queryWrapper.eq(DeviceMessageLog::getDeviceId, -1L);
            return;
        }
        queryWrapper.inSql(DeviceMessageLog::getDeviceId, deviceScopeSql);
    }

    private void appendAccessibleDeviceScope(StringBuilder where, List<Object> params, Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return;
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.TENANT) {
            return;
        }
        where.append(" AND device_id IN (SELECT id FROM iot_device WHERE deleted = 0");
        if (context.tenantId() != null) {
            where.append(" AND tenant_id = ?");
            params.add(context.tenantId());
        }
        if (dataScopeType == DataScopeType.ORG) {
            if (context.orgId() == null || context.orgId() <= 0) {
                where.append(" AND 1 = 0)");
                return;
            }
            where.append(" AND org_id = ?)");
            params.add(context.orgId());
            return;
        }
        Set<Long> accessibleOrgIds = listAccessibleOrganizationIds(currentUserId);
        if (accessibleOrgIds.isEmpty()) {
            where.append(" AND 1 = 0)");
            return;
        }
        where.append(" AND org_id IN (");
        appendSqlPlaceholders(where, accessibleOrgIds.size());
        where.append("))");
        params.addAll(accessibleOrgIds);
    }

    private String buildAccessibleDeviceIdInSql(Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return null;
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.TENANT) {
            return null;
        }
        StringBuilder subquery = new StringBuilder("SELECT id FROM iot_device WHERE deleted = 0");
        if (context.tenantId() != null) {
            subquery.append(" AND tenant_id = ").append(context.tenantId());
        }
        if (dataScopeType == DataScopeType.ORG) {
            if (context.orgId() == null || context.orgId() <= 0) {
                return "SELECT -1";
            }
            subquery.append(" AND org_id = ").append(context.orgId());
            return subquery.toString();
        }
        Set<Long> accessibleOrgIds = listAccessibleOrganizationIds(currentUserId);
        if (accessibleOrgIds.isEmpty()) {
            return "SELECT -1";
        }
        subquery.append(" AND org_id IN (")
                .append(accessibleOrgIds.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                .append(")");
        return subquery.toString();
    }

    private void ensureAccessibleDevice(Long currentUserId, Device device) {
        if (device == null) {
            return;
        }
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return;
        }
        if (context.tenantId() != null && !context.tenantId().equals(device.getTenantId())) {
            throw new BizException("设备不存在或无权访问");
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.TENANT) {
            return;
        }
        if (dataScopeType == DataScopeType.ORG) {
            if (!java.util.Objects.equals(context.orgId(), device.getOrgId())) {
                throw new BizException("设备不存在或无权访问");
            }
            return;
        }
        Set<Long> accessibleOrgIds = listAccessibleOrganizationIds(currentUserId);
        if (device.getOrgId() == null || !accessibleOrgIds.contains(device.getOrgId())) {
            throw new BizException("设备不存在或无权访问");
        }
    }

    private Long resolveScopedTenantId(Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return null;
        }
        return context.tenantId();
    }

    private DataPermissionContext resolveDataPermissionContext(Long currentUserId) {
        if (currentUserId == null || permissionService == null) {
            return null;
        }
        return permissionService.getDataPermissionContext(currentUserId);
    }

    private DataScopeType normalizeDeviceDataScope(DataScopeType dataScopeType) {
        if (dataScopeType == null) {
            return DataScopeType.TENANT;
        }
        return dataScopeType == DataScopeType.SELF ? DataScopeType.ORG : dataScopeType;
    }

    private Set<Long> listAccessibleOrganizationIds(Long currentUserId) {
        if (currentUserId == null || permissionService == null) {
            return Set.of();
        }
        Set<Long> accessibleOrgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
        return accessibleOrgIds == null ? Set.of() : accessibleOrgIds;
    }

    private void appendSqlPlaceholders(StringBuilder sql, int size) {
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
    }

    private void appendTextEquals(StringBuilder where, List<Object> params, String column, String value) {
        if (!hasText(value)) {
            return;
        }
        where.append(" AND ").append(column).append(" = ?");
        params.add(value.trim());
    }

    private void appendTraceKeyword(StringBuilder where, List<Object> params, String keyword) {
        if (!hasText(keyword)) {
            return;
        }
        String normalizedKeyword = keyword.trim();
        where.append(" AND (trace_id = ? OR device_code = ? OR product_key = ?)");
        params.add(normalizedKeyword);
        params.add(normalizedKeyword);
        params.add(normalizedKeyword);
    }

    private void appendTextLike(StringBuilder where, List<Object> params, String column, String value) {
        if (!hasText(value)) {
            return;
        }
        where.append(" AND ").append(column).append(" LIKE ?");
        params.add("%" + value.trim() + "%");
    }

    private void saveMessageLog(Device device, DeviceUpMessage upMessage) {
        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setTenantId(device.getTenantId());
        logRecord.setDeviceId(device.getId());
        logRecord.setProductId(device.getProductId());
        logRecord.setTraceId(hasText(upMessage.getTraceId()) ? upMessage.getTraceId() : TraceContextHolder.getTraceId());
        logRecord.setDeviceCode(device.getDeviceCode());
        logRecord.setProductKey(upMessage.getProductKey());
        logRecord.setMessageType(upMessage.getMessageType());
        logRecord.setTopic(upMessage.getTopic());
        logRecord.setPayload(upMessage.getRawPayload());
        logRecord.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
        logRecord.setCreateTime(LocalDateTime.now());
        deviceMessageLogMapper.insert(logRecord);
    }

    private boolean isCommandReply(DeviceUpMessage upMessage) {
        return upMessage != null && "reply".equalsIgnoreCase(upMessage.getMessageType());
    }

    private void handleCommandReply(Device device, DeviceUpMessage upMessage) {
        Map<String, Object> replyPayload = parseReplyPayload(upMessage.getRawPayload());
        if (replyPayload.isEmpty()) {
            log.warn("设备 ACK 回执无法解析为 JSON, deviceCode={}, topic={}", device.getDeviceCode(), upMessage.getTopic());
            return;
        }

        String commandId = resolveCommandId(replyPayload);
        if (!hasText(commandId)) {
            log.warn("设备 ACK 回执缺少 commandId/messageId, deviceCode={}, topic={}", device.getDeviceCode(), upMessage.getTopic());
            return;
        }

        LocalDateTime ackTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();
        boolean updated;
        if (isReplySuccess(replyPayload)) {
            updated = commandRecordService.markSuccessByCommandId(commandId, upMessage.getRawPayload(), ackTime);
        } else {
            updated = commandRecordService.markFailedByCommandId(
                    commandId,
                    upMessage.getRawPayload(),
                    resolveReplyErrorMessage(replyPayload),
                    ackTime
            );
        }

        if (!updated) {
            log.warn("设备 ACK 回执未找到匹配命令记录, deviceCode={}, commandId={}, topic={}",
                    device.getDeviceCode(), commandId, upMessage.getTopic());
        }
    }

    private void updateLatestProperties(Device device, DeviceUpMessage upMessage) {
        Map<String, Object> properties = upMessage.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }

        Map<String, ProductModel> propertyModels = listPropertyModels(device.getProductId());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String identifier = entry.getKey();
            Object value = entry.getValue();
            ProductModel productModel = propertyModels.get(identifier);

            DeviceProperty property = devicePropertyMapper.selectOne(
                    new LambdaQueryWrapper<DeviceProperty>()
                            .eq(DeviceProperty::getDeviceId, device.getId())
                            .eq(DeviceProperty::getIdentifier, identifier)
                            .last("limit 1")
            );

            if (property == null) {
                property = new DeviceProperty();
                property.setTenantId(device.getTenantId());
                property.setDeviceId(device.getId());
                property.setIdentifier(identifier);
                property.setPropertyName(productModel == null ? identifier : productModel.getModelName());
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value, productModel));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setCreateTime(LocalDateTime.now());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.insert(property);
            } else {
                property.setPropertyName(productModel == null ? property.getPropertyName() : productModel.getModelName());
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value, productModel));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.updateById(property);
            }
        }
    }

    private void updateDeviceOnlineStatus(Device device, DeviceUpMessage upMessage) {
        LocalDateTime reportTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();
        deviceOnlineSessionService.recordOnlineHeartbeat(device, reportTime);

        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(reportTime);
        update.setLastReportTime(reportTime);
        boolean activateDefault = iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getActivateDefault());
        if (activateDefault) {
            update.setActivateStatus(1);
        }
        deviceMapper.updateById(update);
    }

    private void publishRiskEvaluationEvent(Device device, DeviceUpMessage upMessage) {
        if (eventPublisher == null || upMessage == null || upMessage.getProperties() == null || upMessage.getProperties().isEmpty()) {
            return;
        }
        Map<String, Object> copiedProperties = new LinkedHashMap<>(upMessage.getProperties());
        DeviceRiskEvaluationEvent event = new DeviceRiskEvaluationEvent(
                device.getTenantId(),
                device.getId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getProductId(),
                upMessage.getProductKey(),
                upMessage.getProtocolCode(),
                upMessage.getMessageType(),
                upMessage.getTopic(),
                hasText(upMessage.getTraceId()) ? upMessage.getTraceId() : TraceContextHolder.getTraceId(),
                upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp(),
                copiedProperties
        );
        eventPublisher.publishEvent(event);
    }

    private void handleChildMessages(DeviceUpMessage parentMessage) {
        if (parentMessage == null || parentMessage.getChildMessages() == null || parentMessage.getChildMessages().isEmpty()) {
            return;
        }
        for (DeviceUpMessage childMessage : parentMessage.getChildMessages()) {
            if (childMessage == null || !hasText(childMessage.getDeviceCode())) {
                continue;
            }
            normalizeChildMessage(parentMessage, childMessage);
            handleUpMessage(childMessage);
        }
    }

    private void normalizeChildMessage(DeviceUpMessage parentMessage, DeviceUpMessage childMessage) {
        if (!hasText(childMessage.getTenantId())) {
            childMessage.setTenantId(parentMessage.getTenantId());
        }
        if (!hasText(childMessage.getProtocolCode())) {
            childMessage.setProtocolCode(parentMessage.getProtocolCode());
        }
        if (!hasText(childMessage.getTraceId())) {
            childMessage.setTraceId(parentMessage.getTraceId());
        }
        if (!hasText(childMessage.getMessageType())) {
            childMessage.setMessageType(parentMessage.getMessageType());
        }
        if (!hasText(childMessage.getTopic())) {
            childMessage.setTopic(parentMessage.getTopic());
        }
        if (childMessage.getTimestamp() == null) {
            childMessage.setTimestamp(parentMessage.getTimestamp());
        }
        if (!hasText(childMessage.getRawPayload())) {
            childMessage.setRawPayload(parentMessage.getRawPayload());
        }
    }

    private Map<String, ProductModel> listPropertyModels(Long productId) {
        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
        );
        return productModels.stream()
                .collect(Collectors.toMap(ProductModel::getIdentifier, Function.identity(), (left, right) -> left));
    }

    private String resolveValueType(Object value, ProductModel productModel) {
        if (productModel != null && hasText(productModel.getDataType())) {
            return productModel.getDataType();
        }
        if (value == null) {
            return "string";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Float || value instanceof Double) {
            return "double";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        return "string";
    }

    private Map<String, Object> parseReplyPayload(String rawPayload) {
        if (!hasText(rawPayload)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawPayload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveCommandId(Map<String, Object> replyPayload) {
        for (String alias : COMMAND_ID_ALIASES) {
            Object value = replyPayload.get(alias);
            if (value != null && hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private boolean isReplySuccess(Map<String, Object> replyPayload) {
        Object success = replyPayload.get("success");
        if (success != null) {
            return parseBooleanLike(success);
        }

        Object code = replyPayload.get("code");
        if (code != null) {
            String normalized = String.valueOf(code).trim();
            return "0".equals(normalized)
                    || "200".equals(normalized)
                    || "ok".equalsIgnoreCase(normalized)
                    || "success".equalsIgnoreCase(normalized);
        }

        Object status = replyPayload.get("status");
        if (status != null) {
            String normalized = String.valueOf(status).trim();
            if ("failed".equalsIgnoreCase(normalized) || "error".equalsIgnoreCase(normalized)) {
                return false;
            }
            if ("success".equalsIgnoreCase(normalized) || "ok".equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        return true;
    }

    private boolean parseBooleanLike(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = String.valueOf(value).trim();
        return "1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "ok".equalsIgnoreCase(normalized)
                || "success".equalsIgnoreCase(normalized);
    }

    private String resolveReplyErrorMessage(Map<String, Object> replyPayload) {
        for (String alias : ERROR_MESSAGE_ALIASES) {
            Object value = replyPayload.get(alias);
            if (value != null && hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        Object code = replyPayload.get("code");
        return code == null ? "设备返回失败回执" : "设备返回失败回执, code=" + code;
    }

    private MessageTracePayloadRecovery recoverPayloadComparison(DeviceMessageLog logRecord) {
        MessageTracePayloadRecovery recovery = new MessageTracePayloadRecovery();
        String rawPayload = normalizeJsonPayloadSafely(logRecord == null ? null : logRecord.getPayload());
        recovery.setRawPayload(rawPayload);
        if (!hasText(rawPayload) || logRecord == null) {
            return recovery;
        }

        try {
            Device device = resolveTraceDetailDevice(logRecord);
            Product product = resolveTraceDetailProduct(logRecord, device);
            List<String> protocolCandidates = resolveTraceDetailProtocolCandidates(device, product, logRecord, rawPayload);
            RuntimeException lastRecoveryFailure = null;
            boolean encryptedEnvelopePayload = isEncryptedEnvelopePayload(rawPayload);
            for (String protocolCode : protocolCandidates) {
                ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(protocolCode);
                if (adapter == null) {
                    continue;
                }
                try {
                    MessageTracePayloadRecovery candidateRecovery = recoverPayloadComparisonWithAdapter(
                            adapter,
                            logRecord,
                            device,
                            product,
                            rawPayload
                    );
                    if (!encryptedEnvelopePayload || isMeaningfulPayloadRecovery(candidateRecovery, rawPayload)) {
                        recovery.setDecryptedPayload(candidateRecovery.getDecryptedPayload());
                        recovery.setDecodedPayload(candidateRecovery.getDecodedPayload());
                        recovery.setProtocolMetadata(candidateRecovery.getProtocolMetadata());
                        return recovery;
                    }
                } catch (RuntimeException ex) {
                    lastRecoveryFailure = ex;
                }
            }
            if (lastRecoveryFailure != null) {
                log.warn("恢复链路追踪详情 payload 对照失败, logId={}", logRecord.getId(), lastRecoveryFailure);
            }
        } catch (RuntimeException ex) {
            log.warn("恢复链路追踪详情 payload 对照失败, logId={}", logRecord.getId(), ex);
        }
        applyFallbackPayloadRecovery(recovery, logRecord, rawPayload);
        return recovery;
    }

    private MessageTracePayloadRecovery recoverPayloadComparisonWithAdapter(ProtocolAdapter adapter,
                                                                           DeviceMessageLog logRecord,
                                                                           Device device,
                                                                           Product product,
                                                                           String rawPayload) {
        DeviceUpMessage upMessage = adapter.decode(
                rawPayload.getBytes(StandardCharsets.UTF_8),
                buildTraceDetailProtocolContext(logRecord, device, product)
        );
        DeviceUpProtocolMetadata metadata = upMessage == null ? null : upMessage.getProtocolMetadata();

        MessageTracePayloadRecovery recovery = new MessageTracePayloadRecovery();
        String decryptedPayload = metadata == null ? null : normalizeJsonPayloadSafely(metadata.getDecryptedPayloadPreview());
        if (!hasText(decryptedPayload)) {
            decryptedPayload = rawPayload;
        }
        recovery.setDecryptedPayload(decryptedPayload);
        recovery.setDecodedPayload(extractDecodedPayload(metadata, upMessage, logRecord, rawPayload));
        recovery.setProtocolMetadata(metadata);
        return recovery;
    }

    private Device resolveTraceDetailDevice(DeviceMessageLog logRecord) {
        if (logRecord == null || logRecord.getDeviceId() == null || logRecord.getDeviceId() <= 0L) {
            return null;
        }
        return deviceMapper.selectById(logRecord.getDeviceId());
    }

    private Product resolveTraceDetailProduct(DeviceMessageLog logRecord, Device device) {
        Long productId = device != null && device.getProductId() != null
                ? device.getProductId()
                : logRecord == null ? null : logRecord.getProductId();
        if (productId != null) {
            return productMapper.selectById(productId);
        }
        if (logRecord != null && hasText(logRecord.getProductKey())) {
            return findProductByKey(logRecord.getProductKey());
        }
        return null;
    }

    private List<String> resolveTraceDetailProtocolCandidates(Device device,
                                                              Product product,
                                                              DeviceMessageLog logRecord,
                                                              String rawPayload) {
        List<String> candidates = new ArrayList<>();
        addProtocolCandidate(candidates, device == null ? null : device.getProtocolCode());
        addProtocolCandidate(candidates, product == null ? null : product.getProtocolCode());
        if (looksLikeMqttJsonPayload(logRecord, rawPayload)) {
            addProtocolCandidate(candidates, MQTT_JSON_PROTOCOL_CODE);
        }
        return candidates;
    }

    private void addProtocolCandidate(List<String> candidates, String protocolCode) {
        String normalizedProtocolCode = normalizeText(protocolCode);
        if (!hasText(normalizedProtocolCode) || candidates.contains(normalizedProtocolCode)) {
            return;
        }
        candidates.add(normalizedProtocolCode);
    }

    private boolean looksLikeMqttJsonPayload(DeviceMessageLog logRecord, String rawPayload) {
        String topic = normalizeText(logRecord == null ? null : logRecord.getTopic());
        if ("$dp".equals(topic) || (hasText(topic) && topic.startsWith("/sys/"))) {
            return true;
        }
        Object parsedPayload = parseJsonPayload(rawPayload);
        if (!(parsedPayload instanceof Map<?, ?> payloadMap) || payloadMap.isEmpty()) {
            return false;
        }
        if (payloadMap.get("header") instanceof Map<?, ?> && payloadMap.get("bodies") instanceof Map<?, ?>) {
            return true;
        }
        return payloadMap.containsKey("deviceCode")
                || payloadMap.containsKey("productKey")
                || payloadMap.containsKey("messageType");
    }

    private boolean isEncryptedEnvelopePayload(String rawPayload) {
        Object parsedPayload = parseJsonPayload(rawPayload);
        return parsedPayload instanceof Map<?, ?> payloadMap
                && payloadMap.get("header") instanceof Map<?, ?>
                && payloadMap.get("bodies") instanceof Map<?, ?>;
    }

    private boolean isMeaningfulPayloadRecovery(MessageTracePayloadRecovery recovery, String rawPayload) {
        if (recovery == null) {
            return false;
        }
        boolean decryptedRecovered = hasText(recovery.getDecryptedPayload())
                && !rawPayload.equals(recovery.getDecryptedPayload());
        if (decryptedRecovered) {
            return true;
        }
        Map<String, Object> decodedPayload = recovery.getDecodedPayload();
        return decodedPayload != null
                && !decodedPayload.isEmpty()
                && !(decodedPayload.containsKey("header") && decodedPayload.containsKey("bodies"));
    }

    private ProtocolContext buildTraceDetailProtocolContext(DeviceMessageLog logRecord, Device device, Product product) {
        ProtocolContext context = new ProtocolContext();
        context.setTenantCode(logRecord.getTenantId() == null ? null : String.valueOf(logRecord.getTenantId()));
        context.setProductKey(hasText(logRecord.getProductKey())
                ? logRecord.getProductKey()
                : product == null ? null : product.getProductKey());
        context.setDeviceCode(hasText(logRecord.getDeviceCode())
                ? logRecord.getDeviceCode()
                : device == null ? null : device.getDeviceCode());
        context.setMessageType(logRecord.getMessageType());
        context.setTopic(logRecord.getTopic());
        if ("$dp".equals(logRecord.getTopic())) {
            context.setTopicRouteType("legacy");
        }
        return context;
    }

    private Map<String, Object> extractDecodedPayload(DeviceUpProtocolMetadata metadata,
                                                      DeviceUpMessage upMessage,
                                                      DeviceMessageLog logRecord,
                                                      String rawPayload) {
        if (metadata != null && metadata.getDecodedPayloadPreview() != null && !metadata.getDecodedPayloadPreview().isEmpty()) {
            return new LinkedHashMap<>(metadata.getDecodedPayloadPreview());
        }
        if (upMessage != null) {
            Map<String, Object> decodedPayload = new LinkedHashMap<>();
            if (hasText(upMessage.getMessageType())) {
                decodedPayload.put("messageType", upMessage.getMessageType());
            } else if (hasText(logRecord.getMessageType())) {
                decodedPayload.put("messageType", logRecord.getMessageType());
            }
            if (hasText(upMessage.getDeviceCode())) {
                decodedPayload.put("deviceCode", upMessage.getDeviceCode());
            } else if (hasText(logRecord.getDeviceCode())) {
                decodedPayload.put("deviceCode", logRecord.getDeviceCode());
            }
            if (hasText(upMessage.getProductKey())) {
                decodedPayload.put("productKey", upMessage.getProductKey());
            } else if (hasText(logRecord.getProductKey())) {
                decodedPayload.put("productKey", logRecord.getProductKey());
            }
            if (upMessage.getProperties() != null && !upMessage.getProperties().isEmpty()) {
                decodedPayload.put("properties", new LinkedHashMap<>(upMessage.getProperties()));
            }
            if (upMessage.getEvents() != null && !upMessage.getEvents().isEmpty()) {
                decodedPayload.put("events", new LinkedHashMap<>(upMessage.getEvents()));
            }
            if (!decodedPayload.isEmpty()) {
                return decodedPayload;
            }
        }
        return buildFallbackDecodedPayload(logRecord, rawPayload);
    }

    private void applyFallbackPayloadRecovery(MessageTracePayloadRecovery recovery,
                                              DeviceMessageLog logRecord,
                                              String rawPayload) {
        recovery.setDecryptedPayload(rawPayload);
        recovery.setDecodedPayload(buildFallbackDecodedPayload(logRecord, rawPayload));
    }

    private Map<String, Object> buildFallbackDecodedPayload(DeviceMessageLog logRecord, String rawPayload) {
        Object parsedPayload = parseJsonPayload(rawPayload);
        if (parsedPayload instanceof Map<?, ?> map && !map.isEmpty()) {
            Map<String, Object> decodedPayload = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    decodedPayload.put(key, entry.getValue());
                }
            }
            return decodedPayload;
        }

        Map<String, Object> decodedPayload = new LinkedHashMap<>();
        if (hasText(logRecord == null ? null : logRecord.getMessageType())) {
            decodedPayload.put("messageType", logRecord.getMessageType());
        }
        if (hasText(logRecord == null ? null : logRecord.getDeviceCode())) {
            decodedPayload.put("deviceCode", logRecord.getDeviceCode());
        }
        if (hasText(logRecord == null ? null : logRecord.getProductKey())) {
            decodedPayload.put("productKey", logRecord.getProductKey());
        }
        decodedPayload.put("rawPayload", hasText(rawPayload) ? rawPayload : "{}");
        return decodedPayload;
    }

    private String normalizeJsonPayloadSafely(String payloadText) {
        String normalized = tryNormalizeJsonPayload(payloadText);
        if (hasText(normalized)) {
            return normalized;
        }
        return normalizeText(payloadText);
    }

    private Object parseJsonPayload(String payloadText) {
        if (!hasText(payloadText)) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadText, Object.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeTraceMessageType(String messageType) {
        if (!hasText(messageType)) {
            return null;
        }
        String normalized = messageType.trim();
        if ("report".equalsIgnoreCase(normalized)) {
            return "property";
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String displayText(String value) {
        return hasText(value) ? value.trim() : "<empty>";
    }

    private record QuerySpec(String whereClause, List<Object> params) {
    }
}
