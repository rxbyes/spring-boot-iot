package com.ghlzm.iot.protocol.core.registry;

import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议适配器注册中心。
 * 系统启动时收集所有适配器，分发层按 protocolCode 查找对应实现。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:05
 */
@Component
public class ProtocolAdapterRegistry {

    private final List<ProtocolAdapter> adapters;
    private final Map<String, ProtocolAdapter> adapterMap = new ConcurrentHashMap<>();

    public ProtocolAdapterRegistry(List<ProtocolAdapter> adapters) {
        this.adapters = adapters;
    }

    @PostConstruct
    public void init() {
        // 一期协议数量很少，启动时直接构建内存索引即可。
        for (ProtocolAdapter adapter : adapters) {
            adapterMap.put(adapter.getProtocolCode(), adapter);
        }
    }

    public ProtocolAdapter getAdapter(String protocolCode) {
        return adapterMap.get(protocolCode);
    }
}
