package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.schema.SchemaManifestLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TDengine runtime manifest 访问支持。
 */
@Component
public class TdengineSchemaManifestSupport {

    private final SchemaManifestLoader manifestLoader;
    private final AtomicReference<SchemaManifestLoader.TdengineRuntimeManifest> cache = new AtomicReference<>();

    public TdengineSchemaManifestSupport(SchemaManifestLoader manifestLoader) {
        this.manifestLoader = manifestLoader;
    }

    public List<SchemaManifestLoader.TdengineSchemaObject> autoBootstrapObjects() {
        return manifest().objects().stream()
                .filter(item -> "auto_bootstrap".equals(item.runtimeBootstrapMode()))
                .toList();
    }

    public SchemaManifestLoader.TdengineSchemaObject requireObject(String name) {
        return manifest().objects().stream()
                .filter(item -> item.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing tdengine schema object: " + name));
    }

    private SchemaManifestLoader.TdengineRuntimeManifest manifest() {
        SchemaManifestLoader.TdengineRuntimeManifest current = cache.get();
        if (current != null) {
            return current;
        }
        SchemaManifestLoader.TdengineRuntimeManifest loaded = manifestLoader.loadTdengine();
        cache.compareAndSet(null, loaded);
        return cache.get();
    }
}
