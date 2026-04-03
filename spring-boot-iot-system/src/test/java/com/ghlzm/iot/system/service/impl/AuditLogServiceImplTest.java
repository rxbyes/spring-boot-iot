package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuditLogSchemaSupport auditLogSchemaSupport;
    @Mock
    private PermissionService permissionService;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(jdbcTemplate, auditLogSchemaSupport, permissionService);
        when(auditLogSchemaSupport.getColumns()).thenReturn(mockColumns());
        lenient().when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    }

    @Test
    void addLogShouldTruncateOversizeResultMessageBeforeInsert() {
        AuditLog log = new AuditLog();
        log.setTenantId(1L);
        log.setUserName("SYSTEM");
        log.setOperationType("system_error");
        log.setOperationModule("mqtt");
        log.setRequestMethod("MQTT");
        log.setOperationResult(0);
        log.setResultMessage("x".repeat(520));

        auditLogService.addLog(log);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), argsCaptor.capture());

        Object[] args = argsCaptor.getValue();
        String storedMessage = findStringArgWithSuffix(args, "...(truncated)");
        assertNotNull(storedMessage);
        assertEquals(500, storedMessage.length());
        assertTrue(storedMessage.endsWith("...(truncated)"));
        assertTrue(sqlCaptor.getValue().startsWith("INSERT INTO sys_audit_log"));
    }

    @Test
    void addLogShouldFillDefaultTimestampsAndDeletedFlag() {
        AuditLog log = new AuditLog();
        log.setTenantId(1L);
        log.setUserName("SYSTEM");
        log.setOperationType("select");

        auditLogService.addLog(log);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());
        Object[] args = argsCaptor.getValue();

        assertTrue(containsType(args, Timestamp.class));
        assertTrue(containsValue(args, 0));
    }

    @Test
    void shouldFilterScopedAuditLogPageToCurrentTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenReturn(0L);

        AuditLog log = new AuditLog();
        log.setTenantId(2L);
        log.setUserName("other-tenant");

        PageResult<AuditLog> result = invokeScopedPageLogs(99L, log, false, 1, 10);

        assertEquals(0L, result.getTotal());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), org.mockito.ArgumentMatchers.eq(Long.class), argsCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("tenant_id = ?"));
        assertTrue(containsValue(argsCaptor.getValue(), 1L));
        assertTrue(!containsValue(argsCaptor.getValue(), 2L));
    }

    private Set<String> mockColumns() {
        Set<String> columns = new LinkedHashSet<>();
        columns.add("id");
        columns.add("tenant_id");
        columns.add("user_name");
        columns.add("operation_type");
        columns.add("operation_module");
        columns.add("request_method");
        columns.add("operation_result");
        columns.add("result_message");
        columns.add("operation_time");
        columns.add("create_time");
        columns.add("deleted");
        return columns;
    }

    private String findStringArgWithSuffix(Object[] args, String suffix) {
        for (Object arg : args) {
            if (arg instanceof String text && text.endsWith(suffix)) {
                return text;
            }
        }
        return null;
    }

    private boolean containsType(Object[] args, Class<?> targetType) {
        for (Object arg : args) {
            if (targetType.isInstance(arg)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsValue(Object[] args, Object targetValue) {
        for (Object arg : args) {
            if (targetValue.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private PageResult<AuditLog> invokeScopedPageLogs(Long currentUserId,
                                                      AuditLog log,
                                                      Boolean excludeSystemError,
                                                      Integer pageNum,
                                                      Integer pageSize) throws Exception {
        try {
            Method method = AuditLogServiceImpl.class.getMethod(
                    "pageLogs",
                    Long.class,
                    AuditLog.class,
                    Boolean.class,
                    Integer.class,
                    Integer.class
            );
            @SuppressWarnings("unchecked")
            PageResult<AuditLog> result = (PageResult<AuditLog>) method.invoke(
                    auditLogService,
                    currentUserId,
                    log,
                    excludeSystemError,
                    pageNum,
                    pageSize
            );
            return result;
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageLogs overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Exception unwrap(InvocationTargetException exception) throws Exception {
        if (exception.getTargetException() instanceof Exception target) {
            return target;
        }
        throw exception;
    }
}
