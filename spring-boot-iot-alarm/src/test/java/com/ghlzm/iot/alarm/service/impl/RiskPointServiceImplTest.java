package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.DictService;
import com.ghlzm.iot.system.service.OrganizationService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.RegionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RiskPoint.class);
    }

    @Test
    void addRiskPointShouldPersistArchiveRiskPointLevel() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskPointLevel("LEVEL_2");
        input.setCurrentRiskLevel("red");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");
        doReturn(null).when(service).getOne(any());
        doAnswer(invocation -> {
            RiskPoint saved = invocation.getArgument(0);
            saved.setId(9001L);
            return true;
        }).when(service).save(any(RiskPoint.class));

        RiskPoint saved = service.addRiskPoint(input, 1001L);

        assertEquals("level_2", saved.getRiskPointLevel());
        assertEquals("blue", saved.getCurrentRiskLevel());
        assertTrue(saved.getRiskPointCode().contains("LEVEL2"));
        verify(dictService).getByCode("risk_point_level");
    }

    @Test
    void addRiskPointShouldRejectUnknownArchiveLevel() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskPointLevel("level_9");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input, 1001L));
        assertEquals("风险点档案等级不在允许范围内", error.getMessage());
    }

    @Test
    void legacyCurrentRiskLevelShouldNotOverwriteArchiveLevel() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskPointLevel("level_1");
        input.setCurrentRiskLevel("red");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");
        doReturn(null).when(service).getOne(any());
        doAnswer(invocation -> {
            RiskPoint saved = invocation.getArgument(0);
            saved.setId(9002L);
            return true;
        }).when(service).save(any(RiskPoint.class));

        RiskPoint saved = service.addRiskPoint(input, 1001L);

        assertEquals("level_1", saved.getRiskPointLevel());
        assertEquals("blue", saved.getCurrentRiskLevel());
        assertTrue(saved.getRiskPointCode().contains("LEVEL1"));
        assertTrue(!saved.getRiskPointCode().contains("RED"));
    }

    @Test
    void addRiskPointShouldGenerateCodeFromNormalizedRiskLevelAndFillAuditFields() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");
        User user = activeUser(88L);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setResponsibleUser(88L);
        input.setRiskPointLevel("level_1");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(user).when(userService).getById(88L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");
        doReturn(existingRiskPoint("RP-OPSCEN-NORTHS-RED-001")).doReturn(null).when(service).getOne(any());
        doAnswer(invocation -> {
            RiskPoint saved = invocation.getArgument(0);
            saved.setId(9001L);
            return true;
        }).when(service).save(any(RiskPoint.class));

        RiskPoint saved = service.addRiskPoint(input, 1001L);

        assertEquals("ops-center", saved.getOrgName());
        assertEquals("east-yard", saved.getRegionName());
        assertEquals("level_1", saved.getRiskPointLevel());
        assertEquals("blue", saved.getCurrentRiskLevel());
        assertEquals("blue", saved.getRiskLevel());
        assertEquals("RP-OPSCEN-NORTHS-LEVEL1-002", saved.getRiskPointCode());
        assertEquals(1001L, saved.getCreateBy());
        assertEquals(1001L, saved.getUpdateBy());
        assertEquals(0, saved.getDeleted());
    }

    @Test
    void updateRiskPointShouldKeepExistingCodeWhileRefreshingArchiveFieldsAndUpdateBy() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        RiskPoint existing = existingRiskPoint("RP-OLD-001");
        existing.setId(12L);
        existing.setOrgId(7101L);
        existing.setOrgName("legacy-org");
        existing.setRegionId(9101L);
        existing.setRegionName("legacy-yard");
        existing.setCreateBy(1000L);

        Organization organization = activeOrganization(7102L, "alarm-team");
        Region region = activeRegion(9102L, "west-yard");

        RiskPoint update = new RiskPoint();
        update.setId(12L);
        update.setRiskPointName("north-slope");
        update.setOrgId(7102L);
        update.setRegionId(9102L);
        update.setRiskPointLevel("level_2");

        doReturn(existing).when(service).getById(12L);
        doReturn(null).when(service).getOne(any());
        doReturn(organization).when(organizationService).getById(7102L);
        doReturn(region).when(regionService).getById(9102L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");
        doReturn(true).when(service).updateById(any(RiskPoint.class));

        assertDoesNotThrow(() -> service.updateRiskPoint(update, 1002L));
        assertEquals("RP-OLD-001", update.getRiskPointCode());
        assertEquals("alarm-team", update.getOrgName());
        assertEquals("west-yard", update.getRegionName());
        assertEquals("level_2", update.getRiskPointLevel());
        assertEquals("blue", update.getCurrentRiskLevel());
        assertEquals("blue", update.getRiskLevel());
        assertEquals(1000L, update.getCreateBy());
        assertEquals(1002L, update.getUpdateBy());
    }

    @Test
    void addRiskPointShouldRejectDisabledOrganization() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        organization.setStatus(0);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskPointLevel("level_1");

        doReturn(organization).when(organizationService).getById(7101L);

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input, 1001L));
        assertEquals("所属组织已停用", error.getMessage());
    }

    @Test
    void addRiskPointShouldRejectDisabledResponsibleUser() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");
        User responsibleUser = activeUser(88L);
        responsibleUser.setStatus(0);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setResponsibleUser(88L);
        input.setRiskPointLevel("level_1");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(responsibleUser).when(userService).getById(88L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input, 1001L));
        assertEquals("负责人已停用", error.getMessage());
    }

    @Test
    void addRiskPointShouldRejectRiskPointLevelOutsideEnabledDictionary() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskPointLevel("level_4");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input, 1001L));
        assertEquals("风险点档案等级不在允许范围内", error.getMessage());
    }

    @Test
    void addRiskPointShouldRejectLegacyRiskLevelPayloadWhenArchiveLevelMissing() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskLevel("info");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input, 1001L));
        assertEquals("风险点档案等级已改为 riskPointLevel，请补录一级/二级/三级", error.getMessage());
    }

    @Test
    void addRiskPointShouldRetryWhenGeneratedCodeCollidesWithSoftDeletedRecord() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        Organization organization = activeOrganization(7101L, "ops-center");
        Region region = activeRegion(9101L, "east-yard");

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRegionId(9101L);
        input.setRiskPointLevel("level_1");

        int[] saveAttempts = {0};

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(region).when(regionService).getById(9101L);
        doReturn(riskPointLevelDict("level_1", "level_2", "level_3")).when(dictService).getByCode("risk_point_level");
        doReturn(null).when(service).getOne(any());
        doAnswer(invocation -> {
            RiskPoint saved = invocation.getArgument(0);
            saveAttempts[0]++;
            if (saveAttempts[0] == 1) {
                throw new DuplicateKeyException("Duplicate entry for soft-deleted risk point code");
            }
            saved.setId(9002L);
            return true;
        }).when(service).save(any(RiskPoint.class));

        RiskPoint saved = service.addRiskPoint(input, 1001L);

        assertEquals("RP-OPSCEN-NORTHS-LEVEL1-002", saved.getRiskPointCode());
        assertEquals(2, saveAttempts[0]);
    }

    @Test
    void bindDeviceShouldRejectDuplicateMetricBinding() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        RiskPointDevice existing = new RiskPointDevice();
        existing.setRiskPointId(12L);
        existing.setDeviceId(2001L);
        existing.setMetricIdentifier("temperature");
        existing.setDeleted(0);

        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(12L);
        request.setDeviceId(2001L);
        request.setMetricIdentifier("temperature");

        doReturn(existingRiskPoint("RP-OLD-001")).when(service).getById(12L);
        doReturn(existing).when(deviceMapper).selectOne(any());

        BizException error = assertThrows(BizException.class, () -> service.bindDevice(request));
        assertEquals("设备已绑定到该风险点", error.getMessage());
    }

    @Test
    void bindDeviceShouldRejectWhenDeviceAlreadyBoundToAnotherRiskPoint() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService,
                null,
                deviceService
        ));

        RiskPoint riskPoint = existingRiskPoint("RP-OLD-001");
        riskPoint.setId(12L);
        riskPoint.setOrgId(7101L);
        riskPoint.setTenantId(1L);

        Device device = activeDevice(2001L, 7101L, "ops-device-01");
        RiskPointDevice otherBinding = new RiskPointDevice();
        otherBinding.setRiskPointId(13L);
        otherBinding.setDeviceId(2001L);
        otherBinding.setMetricIdentifier("pressure");
        otherBinding.setDeleted(0);

        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(12L);
        request.setDeviceId(2001L);
        request.setMetricIdentifier("temperature");

        doReturn(riskPoint).when(service).getById(12L);
        when(deviceService.getRequiredById(2001L)).thenReturn(device);
        when(deviceMapper.selectOne(any())).thenReturn(null);
        when(deviceMapper.selectList(any())).thenReturn(List.of(otherBinding));

        BizException error = assertThrows(BizException.class, () -> service.bindDevice(request));
        assertEquals("设备已绑定其他风险点，不能重复绑定", error.getMessage());
    }

    @Test
    void bindDeviceShouldRejectWhenDeviceOrganizationMissing() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService,
                null,
                deviceService
        ));

        RiskPoint riskPoint = existingRiskPoint("RP-OLD-001");
        riskPoint.setId(12L);
        riskPoint.setOrgId(7101L);
        riskPoint.setTenantId(1L);

        Device device = activeDevice(2001L, null, "ops-device-01");
        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(12L);
        request.setDeviceId(2001L);
        request.setMetricIdentifier("temperature");

        doReturn(riskPoint).when(service).getById(12L);
        when(deviceService.getRequiredById(2001L)).thenReturn(device);
        when(deviceMapper.selectOne(any())).thenReturn(null);
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        BizException error = assertThrows(BizException.class, () -> service.bindDevice(request));
        assertEquals("设备未归属组织，禁止绑定风险点", error.getMessage());
    }

    @Test
    void bindDeviceShouldRejectWhenDeviceOrganizationMismatch() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService,
                null,
                deviceService
        ));

        RiskPoint riskPoint = existingRiskPoint("RP-OLD-001");
        riskPoint.setId(12L);
        riskPoint.setOrgId(7101L);
        riskPoint.setTenantId(1L);

        Device device = activeDevice(2001L, 7102L, "ops-device-01");
        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(12L);
        request.setDeviceId(2001L);
        request.setMetricIdentifier("temperature");

        doReturn(riskPoint).when(service).getById(12L);
        when(deviceService.getRequiredById(2001L)).thenReturn(device);
        when(deviceMapper.selectOne(any())).thenReturn(null);
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        BizException error = assertThrows(BizException.class, () -> service.bindDevice(request));
        assertEquals("设备所属组织与风险点所属组织不一致", error.getMessage());
    }

    @Test
    void listBindableDevicesShouldExcludeOtherRiskPointBindingsAndKeepCurrentBindings() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        PermissionService permissionService = mock(PermissionService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService,
                permissionService,
                deviceService
        ));

        RiskPoint riskPoint = existingRiskPoint("RP-OLD-001");
        riskPoint.setId(12L);
        riskPoint.setTenantId(1L);
        riskPoint.setOrgId(7101L);

        DeviceOptionVO currentRiskPointDevice = deviceOption(2001L, 7101L, "device-a");
        DeviceOptionVO otherRiskPointDevice = deviceOption(2002L, 7101L, "device-b");
        DeviceOptionVO sameOrgUnboundDevice = deviceOption(2003L, 7101L, "device-c");
        DeviceOptionVO otherOrgDevice = deviceOption(2004L, 7102L, "device-d");

        RiskPointDevice currentBinding = new RiskPointDevice();
        currentBinding.setRiskPointId(12L);
        currentBinding.setDeviceId(2001L);
        currentBinding.setDeleted(0);

        RiskPointDevice otherBinding = new RiskPointDevice();
        otherBinding.setRiskPointId(13L);
        otherBinding.setDeviceId(2002L);
        otherBinding.setDeleted(0);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.ORG_AND_CHILDREN, false));
        when(permissionService.listAccessibleOrganizationIds(99L)).thenReturn(Set.of(7101L, 7102L));
        doReturn(riskPoint).when(service).getById(12L, 99L);
        when(deviceService.listDeviceOptions(99L, false)).thenReturn(List.of(
                currentRiskPointDevice,
                otherRiskPointDevice,
                sameOrgUnboundDevice,
                otherOrgDevice
        ));
        when(deviceMapper.selectList(any())).thenReturn(List.of(currentBinding, otherBinding));

        List<DeviceOptionVO> result = service.listBindableDevices(12L, 99L);

        assertEquals(List.of(2001L, 2003L), result.stream().map(DeviceOptionVO::getId).toList());
    }

    @Test
    void bindDeviceAndReturnShouldPersistAndReturnBinding() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(12L);
        request.setDeviceId(2001L);
        request.setDeviceCode("DEVICE-2001");
        request.setDeviceName("一号设备");
        request.setMetricIdentifier("temperature");
        request.setMetricName("温度");

        doReturn(existingRiskPoint("RP-OLD-001")).when(service).getById(12L);
        doReturn(existingRiskPoint("RP-OLD-001")).when(service).getById(12L, 1001L);
        doReturn(null).when(deviceMapper).selectOne(any());
        doAnswer(invocation -> {
            RiskPointDevice saved = invocation.getArgument(0);
            saved.setId(9003L);
            return 1;
        }).when(deviceMapper).insert(any(RiskPointDevice.class));

        RiskPointDevice saved = service.bindDeviceAndReturn(request, 1001L);

        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals(9003L, saved.getId());
        assertEquals(0, saved.getDeleted());
        verify(deviceMapper).insert(org.mockito.ArgumentMatchers.<RiskPointDevice>argThat(binding ->
                Long.valueOf(12L).equals(binding.getRiskPointId())
                        && Long.valueOf(2001L).equals(binding.getDeviceId())
                        && "temperature".equals(binding.getMetricIdentifier())
                        && Integer.valueOf(0).equals(binding.getDeleted())
                        && binding.getCreateTime() != null
                        && binding.getUpdateTime() != null
        ));
    }

    @Test
    void deleteRiskPointShouldUseLogicalDeleteApi() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        RiskPoint existing = existingRiskPoint("RP-OLD-001");
        existing.setId(12L);

        doReturn(existing).when(service).getById(12L);
        doReturn(true).when(service).removeById(12L);
        doReturn(true).when(service).updateById(any(RiskPoint.class));

        assertDoesNotThrow(() -> service.deleteRiskPoint(12L));

        verify(service).removeById(12L);
        verify(service, never()).updateById(any(RiskPoint.class));
    }

    @Test
    void unbindDeviceShouldUseLogicalDeleteForAllActiveBindings() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService
        ));

        doReturn(2L).when(deviceMapper).selectCount(any());
        doReturn(1).when(deviceMapper).delete(any());

        assertDoesNotThrow(() -> service.unbindDevice(12L, 2001L));

        verify(deviceMapper).delete(any());
        verify(deviceMapper, never()).updateById(any(RiskPointDevice.class));
    }

    @Test
    void pageRiskPointsShouldFilterToAccessibleOrganizationsForOrgChildrenScope() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        PermissionService permissionService = mock(PermissionService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService,
                permissionService
        ));

        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.ORG_AND_CHILDREN, false));
        when(permissionService.listAccessibleOrganizationIds(1L)).thenReturn(java.util.Set.of(7101L, 7102L));

        Page<RiskPoint> page = new Page<>(1L, 10L);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRiskPoints(1L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<RiskPoint>> wrapperCaptor = org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("org_id"));
    }

    @Test
    void pageRiskPointsShouldFilterToCurrentResponsibleOrCreatorForSelfScope() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RegionService regionService = mock(RegionService.class);
        UserService userService = mock(UserService.class);
        DictService dictService = mock(DictService.class);
        PermissionService permissionService = mock(PermissionService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(
                deviceMapper,
                organizationService,
                regionService,
                userService,
                dictService,
                permissionService
        ));

        when(permissionService.getDataPermissionContext(88L))
                .thenReturn(new DataPermissionContext(88L, 1L, 7101L, DataScopeType.SELF, false));

        Page<RiskPoint> page = new Page<>(1L, 10L);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRiskPoints(88L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<RiskPoint>> wrapperCaptor = org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("responsible_user") || sqlSegment.contains("create_by"));
    }

    private RiskPoint existingRiskPoint(String code) {
        RiskPoint riskPoint = new RiskPoint();
        riskPoint.setRiskPointCode(code);
        riskPoint.setDeleted(0);
        return riskPoint;
    }

    private Organization activeOrganization(Long id, String name) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setOrgName(name);
        organization.setStatus(1);
        organization.setDeleted(0);
        return organization;
    }

    private Region activeRegion(Long id, String name) {
        Region region = new Region();
        region.setId(id);
        region.setRegionName(name);
        region.setStatus(1);
        region.setDeleted(0);
        return region;
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }

    private Dict riskLevelDict(String... values) {
        Dict dict = new Dict();
        dict.setId(1L);
        dict.setDictCode("risk_level");
        dict.setItems(List.of(values).stream().map(this::riskLevelItem).toList());
        return dict;
    }

    private Dict riskPointLevelDict(String... values) {
        Dict dict = new Dict();
        dict.setId(1L);
        dict.setDictCode("risk_point_level");
        dict.setItems(List.of(values).stream().map(this::riskLevelItem).toList());
        return dict;
    }

    private DictItem riskLevelItem(String value) {
        DictItem item = new DictItem();
        item.setItemValue(value);
        item.setStatus(1);
        item.setDeleted(0);
        return item;
    }

    private Device activeDevice(Long id, Long orgId, String deviceCode) {
        Device device = new Device();
        device.setId(id);
        device.setTenantId(1L);
        device.setOrgId(orgId);
        device.setOrgName(orgId == null ? null : "org-" + orgId);
        device.setDeviceCode(deviceCode);
        device.setDeviceName(deviceCode);
        return device;
    }

    private DeviceOptionVO deviceOption(Long id, Long orgId, String deviceCode) {
        DeviceOptionVO option = new DeviceOptionVO();
        option.setId(id);
        option.setOrgId(orgId);
        option.setOrgName(orgId == null ? null : "org-" + orgId);
        option.setDeviceCode(deviceCode);
        option.setDeviceName(deviceCode);
        return option;
    }
}
