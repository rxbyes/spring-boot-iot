package com.ghlzm.iot.protocol.core.registry;

import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
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
        for (ProtocolAdapter adapter : adapters) {
            adapterMap.put(adapter.getProtocolCode(), adapter);
        }
    }

    public ProtocolAdapter getAdapter(String protocolCode) {
        return adapterMap.get(protocolCode);
    }
}

