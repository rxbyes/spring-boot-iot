package com.ghlzm.iot.framework.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 读取生成后的 schema runtime manifest。
 */
@Component
public class SchemaManifestLoader {

    static final String MYSQL_MANIFEST_LOCATION = "classpath:schema/runtime-bootstrap/mysql-active-schema.json";
    static final String TDENGINE_MANIFEST_LOCATION = "classpath:schema/runtime-bootstrap/tdengine-active-schema.json";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String mysqlManifestLocation;
    private final String tdengineManifestLocation;

    public SchemaManifestLoader() {
        this(new ObjectMapper());
    }

    public SchemaManifestLoader(ObjectMapper objectMapper) {
        this(objectMapper, new DefaultResourceLoader(), MYSQL_MANIFEST_LOCATION, TDENGINE_MANIFEST_LOCATION);
    }

    SchemaManifestLoader(ObjectMapper objectMapper,
                         ResourceLoader resourceLoader,
                         String mysqlManifestLocation,
                         String tdengineManifestLocation) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.mysqlManifestLocation = mysqlManifestLocation;
        this.tdengineManifestLocation = tdengineManifestLocation;
    }

    public static SchemaManifestLoader forClasspath(String mysqlManifestLocation) {
        return forClasspath(mysqlManifestLocation, TDENGINE_MANIFEST_LOCATION);
    }

    public static SchemaManifestLoader forClasspath(String mysqlManifestLocation, String tdengineManifestLocation) {
        return new SchemaManifestLoader(
                new ObjectMapper(),
                new DefaultResourceLoader(),
                mysqlManifestLocation,
                tdengineManifestLocation
        );
    }

    public MySqlRuntimeManifest loadMySql() {
        return read(mysqlManifestLocation, MySqlRuntimeManifest.class);
    }

    public TdengineRuntimeManifest loadTdengine() {
        return read(tdengineManifestLocation, TdengineRuntimeManifest.class);
    }

    private <T> T read(String location, Class<T> manifestType) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, manifestType);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load schema manifest: " + location, ex);
        }
    }

    public record MySqlRuntimeManifest(List<MySqlTableManifest> tables, List<MySqlViewManifest> views) {

        public MySqlRuntimeManifest {
            tables = safeList(tables);
            views = safeList(views);
        }
    }

    public record MySqlTableManifest(String name,
                                     String storageType,
                                     String ownerModule,
                                     String runtimeBootstrapMode,
                                     String createSql,
                                     List<MySqlColumnManifest> columns,
                                     List<MySqlIndexManifest> indexes) {

        public MySqlTableManifest {
            columns = safeList(columns);
            indexes = safeList(indexes);
        }
    }

    public record MySqlColumnManifest(String name, String addSql) {
    }

    public record MySqlIndexManifest(String name, String addSql) {
    }

    public record MySqlViewManifest(String name,
                                    String storageType,
                                    String ownerModule,
                                    String runtimeBootstrapMode,
                                    String createOrReplaceSql) {
    }

    public record TdengineRuntimeManifest(List<TdengineSchemaObject> objects) {

        public TdengineRuntimeManifest {
            objects = safeList(objects);
        }
    }

    public record TdengineSchemaObject(String name,
                                       String storageType,
                                       String ownerModule,
                                       String runtimeBootstrapMode,
                                       String createSql,
                                       List<TdengineFieldManifest> fieldDictionary) {

        public TdengineSchemaObject {
            fieldDictionary = safeList(fieldDictionary);
        }
    }

    public record TdengineFieldManifest(String name, String commentZh, boolean isTag) {
    }

    private static <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : List.copyOf(source);
    }
}
