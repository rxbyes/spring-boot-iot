package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.mapper.CommandRecordMapper;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandRecordServiceImplTest {

    @Mock
    private CommandRecordMapper commandRecordMapper;
    @Mock
    private DeviceService deviceService;

    @Test
    void pageByDeviceShouldFilterByCapabilityAndStatus() {
        CommandRecordServiceImpl service = new CommandRecordServiceImpl(commandRecordMapper, deviceService);

        CommandRecord record = new CommandRecord();
        record.setId(1L);
        record.setCommandId("1776999000000");
        record.setDeviceCode("demo-device-01");
        record.setProductKey("demo-product");
        record.setTopic("/iot/broadcast/demo-device-01");
        record.setCommandType("capability");
        record.setServiceIdentifier("broadcast_play");
        record.setStatus("SENT");
        record.setSendTime(LocalDateTime.parse("2026-04-24T10:50:00"));

        Page<CommandRecord> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(record));
        when(commandRecordMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<CommandRecordPageItemVO> result = service.pageByDevice(9L, "demo-device-01", "broadcast_play", "SENT", 1L, 10L);

        assertEquals(1L, result.getTotal());
        assertEquals("1776999000000", result.getRecords().get(0).getCommandId());
        ArgumentCaptor<String> deviceCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(deviceService).getRequiredByCode(9L, "demo-device-01");
    }
}
