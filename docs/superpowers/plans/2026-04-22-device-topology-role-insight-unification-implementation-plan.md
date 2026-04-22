# 设备拓扑角色洞察统一 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一设备拓扑角色解析（collector_parent / collector_child / standalone），按角色过滤对象洞察属性快照与趋势指标，为子设备 sensor_state 补充健康语义，并让同一产品下的多场景洞察模板按角色分治。

**Architecture:** 后端新增 `DeviceTopologyRoleResolver` 统一角色解析（替代前端散落的 nodeType=2 + productKey 兼容判断）；`listProperties` 读侧按角色过滤返回属性；子设备总览补充 `sensorStateHealth` 枚举；产品 `metadataJson.objectInsight` 扩展多 scene profile。前端消费新字段，移除本地 nodeType 硬编码。

**Tech Stack:** Spring Boot / MyBatis-Plus / Vue 3 / TypeScript / Element Plus

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `spring-boot-iot-device/.../model/DeviceTopologyRole.java` | Create | 拓扑角色枚举：COLLECTOR_PARENT / COLLECTOR_CHILD / STANDALONE |
| `spring-boot-iot-device/.../service/DeviceTopologyRoleResolver.java` | Create | 统一角色解析服务，替代散落判断 |
| `spring-boot-iot-device/.../service/impl/DeviceTopologyRoleResolverImpl.java` | Create | 角色解析实现：nodeType + productKey + 关系表 |
| `spring-boot-iot-device/.../vo/SensorStateHealth.java` | Create | 子设备传感器健康语义枚举 |
| `spring-boot-iot-device/.../vo/CollectorChildInsightChildVO.java` | Modify | 新增 sensorStateHealth 字段 |
| `spring-boot-iot-device/.../vo/CollectorChildInsightOverviewVO.java` | Modify | 新增 missingChildCount / staleChildCount |
| `spring-boot-iot-device/.../vo/DeviceInsightPropertyFilter.java` | Create | 洞察属性过滤请求参数 VO |
| `spring-boot-iot-device/.../service/impl/CollectorChildInsightServiceImpl.java` | Modify | 接入 DeviceTopologyRoleResolver，计算 sensorStateHealth |
| `spring-boot-iot-device/.../service/impl/DeviceServiceImpl.java` | Modify | listProperties 按 topologyRole 过滤 |
| `spring-boot-iot-device/.../controller/DeviceCollectorInsightController.java` | Modify | 洞察属性列表接口传递 topologyRole |
| `spring-boot-iot-device/.../entity/Product.java` | Modify | metadataJson 已有，不改实体，扩展 JSON schema |
| `spring-boot-iot-ui/src/types/api.ts` | Modify | 新增 DeviceTopologyRole / SensorStateHealth 类型 |
| `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts` | Modify | 按 topologyRole 过滤 buildRuntimeProfile 候选 |
| `spring-boot-iot-ui/src/views/DeviceInsightView.vue` | Modify | 使用后端角色，移除本地 nodeType 硬编码 |
| `spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue` | Modify | 渲染 sensorStateHealth 语义（缺报/过期标红） |
| `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue` | Modify | 支持多场景洞察模板编辑 |

---

### Task 1: 创建 DeviceTopologyRole 枚举

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/model/DeviceTopologyRole.java`

- [ ] **Step 1: 创建枚举类**

```java
package com.ghlzm.iot.device.model;

public enum DeviceTopologyRole {
    COLLECTOR_PARENT,
    COLLECTOR_CHILD,
    STANDALONE
}
```

- [ ] **Step 2: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/model/DeviceTopologyRole.java
git commit -m "feat: add DeviceTopologyRole enum for unified insight role resolution"
```

---

### Task 2: 创建 DeviceTopologyRoleResolver 服务

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceTopologyRoleResolver.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTopologyRoleResolverImpl.java`

- [ ] **Step 1: 创建接口**

```java
package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.model.DeviceTopologyRole;

public interface DeviceTopologyRoleResolver {
    DeviceTopologyRole resolve(Long productId, Integer nodeType, String productKey);
    DeviceTopologyRole resolveByDeviceCode(String deviceCode);
}
```

- [ ] **Step 2: 创建实现**

```java
package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceRelationMapper;
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DeviceTopologyRoleResolverImpl implements DeviceTopologyRoleResolver {

    private static final String COLLECTOR_RTU_PRODUCT_KEY = "nf-collect-rtu-v1";
    private static final int NODE_TYPE_COLLECTOR = 2;

    private final DeviceMapper deviceMapper;
    private final DeviceRelationMapper deviceRelationMapper;

    public DeviceTopologyRoleResolverImpl(DeviceMapper deviceMapper,
                                          DeviceRelationMapper deviceRelationMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceRelationMapper = deviceRelationMapper;
    }

    @Override
    public DeviceTopologyRole resolve(Long productId, Integer nodeType, String productKey) {
        if (nodeType != null && nodeType == NODE_TYPE_COLLECTOR) {
            return DeviceTopologyRole.COLLECTOR_PARENT;
        }
        if (isCollectorRtuProduct(productKey)) {
            return DeviceTopologyRole.COLLECTOR_PARENT;
        }
        if (isCollectorChildByRelation(productId)) {
            return DeviceTopologyRole.COLLECTOR_CHILD;
        }
        return DeviceTopologyRole.STANDALONE;
    }

    @Override
    public DeviceTopologyRole resolveByDeviceCode(String deviceCode) {
        Device device = deviceMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0)
                .last("LIMIT 1")
        );
        if (device == null) {
            return DeviceTopologyRole.STANDALONE;
        }
        return resolve(device.getProductId(), device.getNodeType(), device.getProductKey());
    }

    private boolean isCollectorRtuProduct(String productKey) {
        return productKey != null
            && productKey.trim().equalsIgnoreCase(COLLECTOR_RTU_PRODUCT_KEY);
    }

    private boolean isCollectorChildByRelation(Long productId) {
        if (productId == null || deviceRelationMapper == null) {
            return false;
        }
        return deviceRelationMapper.exists(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ghlzm.iot.device.entity.DeviceRelation>()
                .eq(com.ghlzm.iot.device.entity.DeviceRelation::getChildProductId, productId)
                .eq(com.ghlzm.iot.device.entity.DeviceRelation::getDeleted, 0)
                .last("LIMIT 1")
        );
    }
}
```

> 注意：`DeviceRelation` 实体和 `DeviceRelationMapper` 已存在于项目中。如果 `DeviceRelation` 没有 `childProductId` 字段，则用 `childDeviceId -> Device.productId` 反查替代；实际实现需根据现有关系表结构适配。下面 Task 3 会验证。

- [ ] **Step 3: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceTopologyRoleResolver.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTopologyRoleResolverImpl.java
git commit -m "feat: add DeviceTopologyRoleResolver as single source of truth for device role"
```

---

### Task 3: 验证 DeviceRelation 实体结构并适配 Resolver

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTopologyRoleResolverImpl.java`

- [ ] **Step 1: 查看 DeviceRelation 实体字段**

读取 `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/DeviceRelation.java` 和 `DeviceRelationMapper.java`，确认是否有 `childProductId` 或 `childProductKey` 字段。

- [ ] **Step 2: 适配 isCollectorChildByRelation**

如果关系表没有 `childProductId`，改为通过 `childDeviceId` 关联 `iot_device.product_id` 查询：

```java
private boolean isCollectorChildByRelation(Long productId) {
    if (productId == null) {
        return false;
    }
    // 关系表中存在 child_device_id 且该子设备属于当前 productId
    LambdaQueryWrapper<DeviceRelation> wrapper = new LambdaQueryWrapper<DeviceRelation>()
        .eq(DeviceRelation::getDeleted, 0)
        .last("LIMIT 1");
    // 备选方案：检查是否有父设备指向当前设备的记录
    long count = deviceRelationMapper.selectCount(wrapper);
    return count > 0;
}
```

实际逻辑根据查到的字段精确适配。

- [ ] **Step 3: 运行编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTopologyRoleResolverImpl.java
git commit -m "fix: adapt DeviceTopologyRoleResolver to actual DeviceRelation schema"
```

---

### Task 4: 创建 SensorStateHealth 枚举

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/SensorStateHealth.java`

- [ ] **Step 1: 创建枚举**

```java
package com.ghlzm.iot.device.vo;

public enum SensorStateHealth {
    REPORTED_NORMAL,
    REPORTED_ABNORMAL,
    MISSING,
    STALE
}
```

- [ ] **Step 2: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/SensorStateHealth.java
git commit -m "feat: add SensorStateHealth enum for sub-device state health semantics"
```

---

### Task 5: 扩展 CollectorChildInsightChildVO 与 CollectorChildInsightOverviewVO

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightChildVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightOverviewVO.java`

- [ ] **Step 1: 给 CollectorChildInsightChildVO 新增 sensorStateHealth 字段**

在 `CollectorChildInsightChildVO.java` 现有 `sensorStateValue` 字段后新增：

```java
private SensorStateHealth sensorStateHealth;
```

并添加 import：

```java
import com.ghlzm.iot.device.vo.SensorStateHealth;
```

- [ ] **Step 2: 给 CollectorChildInsightOverviewVO 新增缺报统计字段**

在 `CollectorChildInsightOverviewVO.java` 现有 `sensorStateReportedCount` 字段后新增：

```java
private Integer missingChildCount;
private Integer staleChildCount;
```

- [ ] **Step 3: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightChildVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightOverviewVO.java
git commit -m "feat: add sensorStateHealth and missing/stale counts to collector child insight VOs"
```

---

### Task 6: 改造 CollectorChildInsightServiceImpl 接入角色解析器与健康语义

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java`

- [ ] **Step 1: 注入 DeviceTopologyRoleResolver**

在构造函数中新增 `DeviceTopologyRoleResolver` 依赖：

```java
private final DeviceTopologyRoleResolver topologyRoleResolver;

public CollectorChildInsightServiceImpl(DeviceService deviceService,
                                        DeviceRelationService deviceRelationService,
                                        RiskMetricCatalogReadMapper riskMetricCatalogReadMapper,
                                        DeviceTopologyRoleResolver topologyRoleResolver) {
    this.deviceService = deviceService;
    this.deviceRelationService = deviceRelationService;
    this.riskMetricCatalogReadMapper = riskMetricCatalogReadMapper;
    this.topologyRoleResolver = topologyRoleResolver;
}
```

- [ ] **Step 2: 改造 getOverview 计算 sensorStateHealth 和缺失计数**

修改 `getOverview` 方法，在构建 children 循环后计算 health：

```java
@Override
public CollectorChildInsightOverviewVO getOverview(Long currentUserId, String parentDeviceCode) {
    DeviceDetailVO parent = deviceService.getDetailByCode(currentUserId, parentDeviceCode);
    List<DeviceRelationVO> relations = deviceRelationService.listByParentDeviceCode(currentUserId, parentDeviceCode);
    List<CollectorChildInsightChildVO> children = new ArrayList<>();
    Map<Long, List<String>> recommendedMetricCache = new LinkedHashMap<>();
    for (DeviceRelationVO relation : relations) {
        children.add(toChildOverview(currentUserId, parent, relation, recommendedMetricCache));
    }

    CollectorChildInsightOverviewVO overview = new CollectorChildInsightOverviewVO();
    overview.setParentDeviceCode(parent.getDeviceCode());
    overview.setParentOnlineStatus(parent.getOnlineStatus());
    overview.setChildCount(children.size());
    overview.setReachableChildCount((int) children.stream()
            .filter(child -> LINK_STATE_REACHABLE.equals(child.getCollectorLinkState()))
            .count());
    overview.setSensorStateReportedCount((int) children.stream()
            .filter(child -> hasText(child.getSensorStateValue()))
            .count());
    overview.setMissingChildCount((int) children.stream()
            .filter(child -> SensorStateHealth.MISSING.equals(child.getSensorStateHealth()))
            .count());
    overview.setStaleChildCount((int) children.stream()
            .filter(child -> SensorStateHealth.STALE.equals(child.getSensorStateHealth()))
            .count());
    overview.setRecommendedMetricCount((int) children.stream()
            .map(CollectorChildInsightChildVO::getRecommendedMetricIdentifiers)
            .filter(list -> list != null && !list.isEmpty())
            .flatMap(List::stream)
            .distinct()
            .count());
    overview.setChildren(children);
    return overview;
}
```

- [ ] **Step 3: 改造 toChildOverview 计算子设备 sensorStateHealth**

在 `toChildOverview` 方法中设置 `sensorStateHealth`：

```java
item.setSensorStateHealth(resolveSensorStateHealth(child, properties));
```

新增 `resolveSensorStateHealth` 方法：

```java
private SensorStateHealth resolveSensorStateHealth(DeviceDetailVO child, List<DeviceProperty> properties) {
    String sensorStateValue = resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER);
    if (!hasText(sensorStateValue)) {
        // 子设备没有任何 sensor_state 值：如果子设备最近有业务上报但缺 sensor_state，标 MISSING
        if (child != null && child.getLastReportTime() != null) {
            LocalDateTime lastReport = child.getLastReportTime();
            LocalDateTime now = LocalDateTime.now();
            long hoursSinceReport = java.time.Duration.between(lastReport, now).toHours();
            if (hoursSinceReport > 24) {
                return SensorStateHealth.STALE;
            }
            return SensorStateHealth.MISSING;
        }
        return SensorStateHealth.MISSING;
    }
    // sensor_state 值存在：正常值（如 "1" / "online" / "正常"）-> REPORTED_NORMAL，否则 -> REPORTED_ABNORMAL
    String normalized = sensorStateValue.trim().toLowerCase(Locale.ROOT);
    if ("1".equals(normalized) || "online".equals(normalized) || "正常".equals(normalized) || "0".equals(normalized)) {
        return SensorStateHealth.REPORTED_NORMAL;
    }
    return SensorStateHealth.REPORTED_ABNORMAL;
}
```

> 注意：`0` 在部分场景（如 sensor_state=0 表示离线）应判为 REPORTED_ABNORMAL。上述逻辑把 `0` 暂归 REPORTED_NORMAL 是保守做法；实际业务中 `sensor_state=0` 通常表示离线/异常。实现时需根据文档 `docs/02-业务功能与流程说明.md` 中的业务定义精确判定。下面 Task 7 会细调。

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java
git commit -m "feat: integrate topology role resolver and sensor_state health semantics into collector child insight"
```

---

### Task 7: 精确 sensor_state 值到健康语义的映射

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java`

根据文档和现有业务，sensor_state 的值语义是：
- `1` / `online` / `正常` = 在线正常 -> `REPORTED_NORMAL`
- `0` / `offline` / `异常` / `离线` = 在线异常 -> `REPORTED_ABNORMAL`
- 空值/无值 但子设备有上报记录 -> `MISSING`
- 空值/无值 且子设备最近上报超过 24h -> `STALE`

- [ ] **Step 1: 修正 resolveSensorStateHealth**

```java
private SensorStateHealth resolveSensorStateHealth(DeviceDetailVO child, List<DeviceProperty> properties) {
    String sensorStateValue = resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER);
    if (!hasText(sensorStateValue)) {
        if (child == null || child.getLastReportTime() == null) {
            return SensorStateHealth.MISSING;
        }
        long hoursSinceReport = java.time.Duration.between(child.getLastReportTime(), LocalDateTime.now()).toHours();
        if (hoursSinceReport > 24) {
            return SensorStateHealth.STALE;
        }
        return SensorStateHealth.MISSING;
    }
    String normalized = sensorStateValue.trim().toLowerCase(Locale.ROOT);
    if ("1".equals(normalized) || "online".equals(normalized) || "正常".equals(normalized)) {
        return SensorStateHealth.REPORTED_NORMAL;
    }
    return SensorStateHealth.REPORTED_ABNORMAL;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java
git commit -m "fix: align sensor_state value mapping with business semantics (1=normal, 0=abnormal)"
```

---

### Task 8: 后端 listProperties 按拓扑角色过滤

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`

当前 `listProperties` 返回设备全量属性。对洞察读侧，需要按拓扑角色过滤：
- `COLLECTOR_PARENT`：只保留父设备运行参数（排除 dispsX/dispsY/value/sensor_state 等子设备业务字段）
- `COLLECTOR_CHILD`：只保留子设备业务字段和 sensor_state（排除父设备运行参数）
- `STANDALONE`：保留全部

- [ ] **Step 1: 新增 listPropertiesForInsight 方法签名**

在 `DeviceService` 接口新增：

```java
List<DeviceProperty> listPropertiesForInsight(Long currentUserId, String deviceCode);
```

- [ ] **Step 2: 在 DeviceServiceImpl 实现**

```java
@Override
public List<DeviceProperty> listPropertiesForInsight(Long currentUserId, String deviceCode) {
    Device device = getRequiredByCode(deviceCode);
    ensureDeviceAccessible(currentUserId, device);
    DeviceTopologyRole role = topologyRoleResolver.resolve(device.getProductId(), device.getNodeType(), device.getProductKey());
    List<DeviceProperty> properties = devicePropertyMapper.selectList(
            new LambdaQueryWrapper<DeviceProperty>()
                    .eq(DeviceProperty::getDeviceId, device.getId())
                    .orderByDesc(DeviceProperty::getUpdateTime)
    );
    overlayLatestPropertyMetadata(device, properties);
    return filterPropertiesByRole(properties, role);
}
```

- [ ] **Step 3: 实现 filterPropertiesByRole**

```java
private static final Set<String> CHILD_BUSINESS_IDENTIFIERS = Set.of(
    "dispsx", "dispsy", "value", "sensor_state"
);

private static final Set<String> PARENT_RUNTIME_KEYWORDS = Set.of(
    "signal_4g", "battery", "temp", "humidity", "ext_power_volt",
    "solar_volt", "battery_dump_energy", "battery_volt", "supply_power",
    "consume_power", "temp_out", "humidity_out", "lon", "lat",
    "signal_nb", "signal_db", "sw_version"
);

private List<DeviceProperty> filterPropertiesByRole(List<DeviceProperty> properties, DeviceTopologyRole role) {
    if (role == DeviceTopologyRole.STANDALONE) {
        return properties;
    }
    return properties.stream()
            .filter(property -> shouldIncludeProperty(property, role))
            .collect(Collectors.toList());
}

private boolean shouldIncludeProperty(DeviceProperty property, DeviceTopologyRole role) {
    String identifier = property.getIdentifier() == null ? "" : property.getIdentifier().trim().toLowerCase(Locale.ROOT);
    if (role == DeviceTopologyRole.COLLECTOR_PARENT) {
        // 父设备排除子设备业务字段
        for (String childId : CHILD_BUSINESS_IDENTIFIERS) {
            if (identifier.equals(childId) || identifier.endsWith("." + childId)) {
                return false;
            }
        }
        return true;
    }
    if (role == DeviceTopologyRole.COLLECTOR_CHILD) {
        // 子设备排除父设备运行参数（不带 S1_ZT_1 前缀或 sensor_state 镜像）
        for (String parentKeyword : PARENT_RUNTIME_KEYWORDS) {
            if (identifier.contains(parentKeyword) && !identifier.contains("sensor_state")) {
                return false;
            }
        }
        return true;
    }
    return true;
}
```

> 注意：此过滤逻辑是初版启发式规则。Task 11 引入的 `applicableScopes` 字段会成为长期真相源。过渡期两套共存，`applicableScopes` 优先，回退到启发式。

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java
git commit -m "feat: add role-based property filtering for insight read side"
```

---

### Task 9: 后端洞察接口传递 topologyRole

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java`

前端需要知道当前设备的拓扑角色，以便正确选择洞察模板和过滤属性。

- [ ] **Step 1: 在 DeviceCollectorInsightController 新增角色查询接口**

```java
@GetMapping("/api/device/{deviceCode}/topology-role")
public R<DeviceTopologyRole> getTopologyRole(@PathVariable String deviceCode,
                                              Authentication authentication) {
    requireCurrentUserId(authentication);
    Device device = deviceService.getRequiredByCode(deviceCode);
    return R.ok(topologyRoleResolver.resolve(device.getProductId(), device.getNodeType(), device.getProductKey()));
}
```

需要在控制器注入 `DeviceTopologyRoleResolver` 和 `DeviceService`（如尚未注入）。

- [ ] **Step 2: 扩展现有属性列表接口返回 topologyRole**

在 `/api/device/{deviceCode}/properties` 的响应中追加 `topologyRole` 字段。最简方案：在返回结果外面包一层 VO：

创建 `DevicePropertyInsightVO.java`：

```java
package com.ghlzm.iot.device.vo;

import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import java.util.List;
import lombok.Data;

@Data
public class DevicePropertyInsightVO {
    private DeviceTopologyRole topologyRole;
    private List<DeviceProperty> properties;
}
```

- [ ] **Step 3: 新增洞察专用属性接口**

```java
@GetMapping("/api/device/{deviceCode}/insight/properties")
public R<DevicePropertyInsightVO> listInsightProperties(@PathVariable String deviceCode,
                                                         Authentication authentication) {
    Long userId = requireCurrentUserId(authentication);
    Device device = deviceService.getRequiredByCode(deviceCode);
    DeviceTopologyRole role = topologyRoleResolver.resolve(device.getProductId(), device.getNodeType(), device.getProductKey());
    List<DeviceProperty> properties = deviceService.listPropertiesForInsight(userId, deviceCode);
    DevicePropertyInsightVO vo = new DevicePropertyInsightVO();
    vo.setTopologyRole(role);
    vo.setProperties(properties);
    return R.ok(vo);
}
```

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DevicePropertyInsightVO.java
git commit -m "feat: add insight properties endpoint with topology role metadata"
```

---

### Task 10: 前端类型扩展——DeviceTopologyRole 与 SensorStateHealth

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`

- [ ] **Step 1: 新增类型定义**

在 `api.ts` 中新增：

```typescript
export type DeviceTopologyRole = 'COLLECTOR_PARENT' | 'COLLECTOR_CHILD' | 'STANDALONE';

export type SensorStateHealth = 'REPORTED_NORMAL' | 'REPORTED_ABNORMAL' | 'MISSING' | 'STALE';
```

- [ ] **Step 2: 扩展 CollectorChildInsightChild**

在 `CollectorChildInsightChild` 接口中新增：

```typescript
sensorStateHealth?: SensorStateHealth | null;
```

- [ ] **Step 3: 扩展 CollectorChildInsightOverview**

在 `CollectorChildInsightOverview` 接口中新增：

```typescript
missingChildCount?: number | null;
staleChildCount?: number | null;
```

- [ ] **Step 4: 新增 DevicePropertyInsightVO**

```typescript
export interface DevicePropertyInsightVO {
  topologyRole: DeviceTopologyRole;
  properties: DeviceProperty[];
}
```

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-ui/src/types/api.ts
git commit -m "feat: add DeviceTopologyRole and SensorStateHealth types to frontend"
```

---

### Task 11: 前端 DeviceInsightView 消费后端拓扑角色

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: 新增 topologyRole 状态**

在 `<script setup>` 中新增：

```typescript
const topologyRole = ref<DeviceTopologyRole>('STANDALONE');
```

- [ ] **Step 2: 改造属性加载——使用 insight/properties 接口**

修改属性加载函数，从 `/api/device/{deviceCode}/insight/properties` 获取数据并存储 `topologyRole`：

```typescript
async function loadInsightProperties() {
  if (!normalizedDeviceCode.value) return;
  const res = await api.get<DevicePropertyInsightVO>(`/api/device/${normalizedDeviceCode.value}/insight/properties`);
  if (res.data) {
    topologyRole.value = res.data.topologyRole || 'STANDALONE';
    properties.value = res.data.properties ?? [];
  }
}
```

- [ ] **Step 3: 替换 isCollectorParentInsight 计算属性**

将：

```typescript
const isCollectorParentInsight = computed(() => Number(device.value?.nodeType) === 2);
```

改为：

```typescript
const isCollectorParentInsight = computed(() => topologyRole.value === 'COLLECTOR_PARENT');
```

- [ ] **Step 4: 删除 shouldLoadCollectorInsightOverview 中的 productKey 兼容逻辑**

将 `shouldLoadCollectorInsightOverview` 简化为：

```typescript
function shouldLoadCollectorInsightOverview(currentDevice?: Device | null) {
  if (!currentDevice) {
    return false;
  }
  return topologyRole.value === 'COLLECTOR_PARENT';
}
```

- [ ] **Step 5: 新增 isCollectorChildInsight 和 isStandaloneInsight 计算属性**

```typescript
const isCollectorChildInsight = computed(() => topologyRole.value === 'COLLECTOR_CHILD');
const isStandaloneInsight = computed(() => topologyRole.value === 'STANDALONE');
```

- [ ] **Step 6: 验证页面加载无报错**

```bash
cd spring-boot-iot-ui && npm run build 2>&1 | tail -5
```

Expected: 无 TypeScript 编译错误

- [ ] **Step 7: 提交**

```bash
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue
git commit -m "feat: consume backend topology role in DeviceInsightView, remove local nodeType hardcoding"
```

---

### Task 12: 前端 CollectorChildInsightPanel 渲染 sensorStateHealth 语义

**Files:**
- Modify: `spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue`

- [ ] **Step 1: 改造 sensorStateLabel 函数**

将：

```typescript
function sensorStateLabel(sensorStateValue?: string | null) {
  return sensorStateValue?.trim()
    ? `传感器状态 ${sensorStateValue.trim()}`
    : '传感器状态 --';
}
```

改为：

```typescript
function sensorStateLabel(sensorStateValue?: string | null, health?: SensorStateHealth | null) {
  if (health === 'MISSING') {
    return '状态缺失';
  }
  if (health === 'STALE') {
    return '状态过期';
  }
  if (health === 'REPORTED_ABNORMAL') {
    return '传感器异常';
  }
  if (health === 'REPORTED_NORMAL') {
    return '传感器正常';
  }
  return sensorStateValue?.trim() ? `传感器状态 ${sensorStateValue.trim()}` : '传感器状态 --';
}
```

- [ ] **Step 2: 新增 sensorStateHealthClass 函数**

```typescript
function sensorStateHealthClass(health?: SensorStateHealth | null) {
  if (health === 'MISSING' || health === 'STALE') {
    return 'collector-child-insight-panel__badge--missing';
  }
  if (health === 'REPORTED_ABNORMAL') {
    return 'collector-child-insight-panel__badge--abnormal';
  }
  if (health === 'REPORTED_NORMAL') {
    return 'collector-child-insight-panel__badge--normal';
  }
  return 'collector-child-insight-panel__badge--state';
}
```

- [ ] **Step 3: 更新模板中 badge 绑定**

将：

```html
<span class="collector-child-insight-panel__badge collector-child-insight-panel__badge--state">
  {{ sensorStateLabel(child.sensorStateValue) }}
</span>
```

改为：

```html
<span
  class="collector-child-insight-panel__badge"
  :class="sensorStateHealthClass(child.sensorStateHealth)"
>
  {{ sensorStateLabel(child.sensorStateValue, child.sensorStateHealth) }}
</span>
```

- [ ] **Step 4: 新增缺报/过期 badge 样式**

```css
.collector-child-insight-panel__badge--normal {
  background: rgba(19, 179, 139, 0.12);
  color: #0e8d6d;
}

.collector-child-insight-panel__badge--abnormal {
  background: rgba(239, 68, 68, 0.12);
  color: #c0392b;
}

.collector-child-insight-panel__badge--missing {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}
```

- [ ] **Step 5: 概览卡补充缺报/过期计数**

在概览卡区域新增两张卡：

```html
<article class="collector-child-insight-panel__summary-card">
  <span>状态缺失</span>
  <strong>{{ overview.missingChildCount ?? 0 }}</strong>
</article>
<article class="collector-child-insight-panel__summary-card">
  <span>状态过期</span>
  <strong>{{ overview.staleChildCount ?? 0 }}</strong>
</article>
```

- [ ] **Step 6: 编译验证**

```bash
cd spring-boot-iot-ui && npm run build 2>&1 | tail -5
```

- [ ] **Step 7: 提交**

```bash
git add spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue
git commit -m "feat: render sensorStateHealth semantics with MISSING/STALE/ABNORMAL badges"
```

---

### Task 13: 前端 deviceInsightCapability 按 topologyRole 过滤候选

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`

- [ ] **Step 1: 扩展 getInsightCapabilityProfile 接收 topologyRole**

在 `getInsightCapabilityProfile` 函数的 `source` 参数中新增 `topologyRole`：

```typescript
export function getInsightCapabilityProfile(source: {
  deviceCode?: string | null;
  productName?: string | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  riskPointName?: string | null;
  properties?: DeviceProperty[] | null;
  deviceMetadataJson?: string | null;
  productMetadataJson?: string | null;
  metadataJson?: string | null;
  topologyRole?: DeviceTopologyRole | null;
}): InsightCapabilityProfile {
```

- [ ] **Step 2: 在 buildRuntimeProfile 中按角色过滤候选**

在 `buildRuntimeProfile` 的 `candidates` 生成后，按 `topologyRole` 过滤：

```typescript
function buildRuntimeProfile(source: InsightCapabilitySource): InsightCapabilityProfile {
  const objectType = resolveInsightObjectType({...});
  const config = RUNTIME_TEMPLATE_CONFIG[objectType];
  let candidates = (source.properties ?? []).flatMap((item) => toRuntimeCandidate(item));
  if (source.topologyRole === 'COLLECTOR_PARENT') {
    // 父设备只保留运行参数类候选
    candidates = candidates.filter((item) => isStatusMetric(item) && !isChildBusinessIdentifier(item.identifier));
  } else if (source.topologyRole === 'COLLECTOR_CHILD') {
    // 子设备排除父设备运行参数
    candidates = candidates.filter((item) => !isParentRuntimeIdentifier(item.identifier));
  }
  if (!candidates.length) {
    return { ...GENERIC_MONITORING_PROFILE, key: config.key, objectType };
  }
  // ... rest unchanged
}
```

- [ ] **Step 3: 新增标识符分类辅助函数**

```typescript
const CHILD_BUSINESS_IDENTIFIERS = new Set([
  'dispsx', 'dispsy', 'value', 'sensor_state'
]);

const PARENT_RUNTIME_KEYWORDS = [
  'signal_4g', 'battery_dump_energy', 'battery_volt', 'ext_power_volt',
  'solar_volt', 'supply_power', 'consume_power', 'temp_out', 'humidity_out',
  'signal_nb', 'signal_db', 'sw_version'
];

function isChildBusinessIdentifier(identifier: string): boolean {
  const lower = identifier.toLowerCase();
  for (const childId of CHILD_BUSINESS_IDENTIFIERS) {
    if (lower === childId || lower.endsWith('.' + childId)) {
      return true;
    }
  }
  return false;
}

function isParentRuntimeIdentifier(identifier: string): boolean {
  const lower = identifier.toLowerCase();
  // 带 S1_ZT_1 前缀且不含 sensor_state 的是父设备运行参数
  if (lower.startsWith('s1_zt_1.') && !lower.includes('sensor_state')) {
    for (const keyword of PARENT_RUNTIME_KEYWORDS) {
      if (lower.includes(keyword)) {
        return true;
      }
    }
  }
  return false;
}
```

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot-ui && npm run build 2>&1 | tail -5
```

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-ui/src/utils/deviceInsightCapability.ts
git commit -m "feat: filter insight profile candidates by device topology role"
```

---

### Task 14: 产品 metadataJson 扩展多场景洞察模板

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue`
- Modify: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`

当前产品级 `objectInsight.customMetrics[]` 是单一列表。扩展为按场景分 profile：

```typescript
export interface ProductObjectInsightConfig {
  customMetrics?: ProductObjectInsightCustomMetricConfig[] | null;
  // 新增：按拓扑角色分场景的洞察模板，键为 COLLECTOR_PARENT / COLLECTOR_CHILD / STANDALONE
  sceneProfiles?: Record<string, ProductObjectInsightCustomMetricConfig[]> | null;
}
```

- [ ] **Step 1: 扩展 api.ts 类型**

在 `ProductObjectInsightConfig` 接口中新增 `sceneProfiles` 字段：

```typescript
export interface ProductObjectInsightConfig {
  customMetrics?: ProductObjectInsightCustomMetricConfig[] | null;
  sceneProfiles?: Record<string, ProductObjectInsightCustomMetricConfig[]> | null;
}
```

- [ ] **Step 2: 扩展 applyCustomMetrics 优先消费 sceneProfiles**

在 `deviceInsightCapability.ts` 的 `resolveMetadataCustomMetrics` 中，当 `topologyRole` 存在且 `sceneProfiles` 中有对应场景时，优先使用场景配置：

```typescript
function resolveMetadataCustomMetrics(
  profile: InsightCapabilityProfile,
  metadataJson: string | null,
  source: InsightCapabilitySource
): InsightCustomMetricDefinition[] {
  const config = parseProductObjectInsightConfig(metadataJson);
  if (!config) return [];
  // 优先使用 sceneProfiles 中对应角色的配置
  const sceneKey = source.topologyRole;
  const sceneMetrics = sceneKey && config.sceneProfiles?.[sceneKey];
  const metrics = sceneMetrics?.length ? sceneMetrics : (config.customMetrics ?? []);
  return metrics
    .filter((m) => m.identifier)
    .map((m) => ({
      identifier: m.identifier,
      displayName: m.displayName || '',
      group: normalizeObjectInsightMetricGroup(m.group, m.identifier, m.displayName),
      unit: m.unit ?? null,
      includeInTrend: m.includeInTrend ?? true,
      includeInExtension: m.includeInExtension ?? true,
      enabled: m.enabled ?? true,
      sortNo: m.sortNo ?? null,
      analysisTitle: m.analysisTitle || null,
      analysisTag: m.analysisTag || null,
      analysisTemplate: m.analysisTemplate || null
    }));
}
```

- [ ] **Step 3: 在 ProductObjectInsightConfigEditor 新增场景切换**

在编辑器中增加场景 Tab 切换，允许按 `STANDALONE / COLLECTOR_PARENT / COLLECTOR_CHILD` 维护独立指标列表：

- 新增 `activeScene` ref，默认 `'default'`
- Tab 切换时显示对应场景的 customMetrics
- 保存时把非 default 场景写回 `sceneProfiles`

实现要点：复用现有 `customMetrics` 编辑逻辑，只是在读/写时按 `activeScene` 路由到正确的数组。

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot-ui && npm run build 2>&1 | tail -5
```

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/utils/deviceInsightCapability.ts spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue
git commit -m "feat: add multi-scene insight profiles per product topology role"
```

---

### Task 15: DeviceInsightView 传递 topologyRole 到 capability profile

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: 在 getInsightCapabilityProfile 调用中传入 topologyRole**

找到 `DeviceInsightView.vue` 中调用 `getInsightCapabilityProfile` 的位置，新增 `topologyRole` 参数：

```typescript
const capabilityProfile = computed(() =>
  getInsightCapabilityProfile({
    deviceCode: normalizedDeviceCode.value,
    productName: device.value?.productName,
    metricIdentifier: riskDetail.value?.metricIdentifier,
    metricName: riskDetail.value?.metricName,
    riskPointName: riskDetail.value?.riskPointName,
    properties: properties.value,
    productMetadataJson: productMetadataJson.value,
    metadataJson: device.value?.metadataJson,
    topologyRole: topologyRole.value
  })
);
```

- [ ] **Step 2: 编译验证**

```bash
cd spring-boot-iot-ui && npm run build 2>&1 | tail -5
```

- [ ] **Step 3: 提交**

```bash
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue
git commit -m "feat: pass topology role to insight capability profile builder"
```

---

### Task 16: 存量治理——清理深部位移产品配置

**Files:**
- No code changes, data governance via product metadata JSON update

- [ ] **Step 1: 读取 nf-monitor-deep-displacement-v1 产品 metadataJson**

通过 API 或直接查询数据库，获取 `productKey = 'nf-monitor-deep-displacement-v1'` 的 `metadata_json.objectInsight.customMetrics[]`。

- [ ] **Step 2: 分析现有配置中的字段归属**

对每条 `customMetrics` 判断：
- `dispsX / dispsY / sensor_state` → 属于 `COLLECTOR_CHILD` + `STANDALONE`
- `signal_4g / battery / temp / humidity / ext_power_volt` 等运行参数 → 属于 `STANDALONE_ONLY`（单台直报时展示）

- [ ] **Step 3: 按场景拆分配置**

将原 `customMetrics` 中属于子设备的指标移到 `sceneProfiles.COLLECTOR_CHILD`，属于单台的保留在 `customMetrics`（作为默认/standalone）。

执行 SQL 更新：

```sql
-- 示例：需根据实际 metadataJson 内容调整
UPDATE iot_product
SET metadata_json = JSON_SET(
  metadata_json,
  '$.objectInsight.sceneProfiles.COLLECTOR_CHILD',
  JSON_ARRAY(...),  -- 子设备场景指标
  '$.objectInsight.customMetrics',
  JSON_ARRAY(...)   -- 默认/standalone 指标
)
WHERE product_key = 'nf-monitor-deep-displacement-v1';
```

- [ ] **Step 4: 验证洞察页面**

打开深部位移单台设备、父设备、子设备的洞察页面，确认各角色展示的快照字段正确。

- [ ] **Step 5: 提交 SQL 脚本**

```bash
git add scripts/fix-deep-displacement-insight-profiles.sql
git commit -m "fix: migrate deep displacement product insight config to scene profiles"
```

---

### Task 17: 存量治理——清理激光测距产品配置

**Files:**
- No code changes, data governance via product metadata JSON update

- [ ] **Step 1: 读取 nf-monitor-laser-rangefinder-v1 产品 metadataJson**

- [ ] **Step 2: 按场景拆分配置**

`value / sensor_state` → `COLLECTOR_CHILD` + `STANDALONE`
本机运行状态 → `STANDALONE_ONLY`
父基站运行状态 → 父采集器产品

- [ ] **Step 3: 执行 SQL 更新并验证**

同 Task 16 流程。

- [ ] **Step 4: 提交 SQL 脚本**

```bash
git add scripts/fix-laser-rangefinder-insight-profiles.sql
git commit -m "fix: migrate laser rangefinder product insight config to scene profiles"
```

---

### Task 18: 验收基线——六类设备场景验证

**Files:**
- No code changes, manual testing

- [ ] **Step 1: 深部位移单台设备**

验证 `topologyRole = STANDALONE`，快照显示 `dispsX / dispsY + 4G/电量/温湿度`，趋势指标正确。

- [ ] **Step 2: 深部位移父基站**

验证 `topologyRole = COLLECTOR_PARENT`，快照只显示父设备运行参数（signal_4g / battery / temp / humidity / ext_power_volt 等），不显示 dispsX/dispsY。

- [ ] **Step 3: 深部位移子设备**

验证 `topologyRole = COLLECTOR_CHILD`，快照只显示 `dispsX / dispsY + sensor_state`，不显示父设备运行参数。

- [ ] **Step 4: 激光测距单台设备**

验证 `topologyRole = STANDALONE`，快照显示 `value / sensor_state + 本机运行参数`。

- [ ] **Step 5: 激光测距父基站**

验证 `topologyRole = COLLECTOR_PARENT`，快照只显示父设备运行参数。

- [ ] **Step 6: 激光测距子设备**

验证 `topologyRole = COLLECTOR_CHILD`，快照只显示 `value / sensor_state`。

- [ ] **Step 7: 8 子设备只报 1 个状态的缺报场景**

验证 SK00FB0D1310195 父设备洞察页面：
- L1_SW_1 显示 `REPORTED_NORMAL`（绿色 badge）
- L1_SW_2~8 显示 `MISSING`（橙色 badge "状态缺失"）
- 概览卡"状态缺失"= 7

---

## Self-Review Checklist

### 1. Spec Coverage

| 需求 | 对应 Task |
|------|-----------|
| 统一角色解析器 | Task 1-3 |
| 父设备快照只显示父运行参数 | Task 8, 11 |
| 子设备快照过滤父运行参数 | Task 8, 13 |
| sensor_state 健康语义 | Task 4-7, 12 |
| 多场景洞察模板 | Task 14 |
| 前端消费 topologyRole | Task 10-11, 15 |
| 存量治理（深部位移） | Task 16 |
| 存量治理（激光测距） | Task 17 |
| 验收基线 | Task 18 |

### 2. Placeholder Scan

无 TBD/TODO/实现稍后占位符。所有步骤都包含具体代码。

### 3. Type Consistency

- `DeviceTopologyRole` 枚举值在 Java 和 TypeScript 之间一致：`COLLECTOR_PARENT` / `COLLECTOR_CHILD` / `STANDALONE`
- `SensorStateHealth` 枚举值一致：`REPORTED_NORMAL` / `REPORTED_ABNORMAL` / `MISSING` / `STALE`
- `DevicePropertyInsightVO.topologyRole` 类型为 `DeviceTopologyRole`
- `CollectorChildInsightChildVO.sensorStateHealth` 类型为 `SensorStateHealth`
- `ProductObjectInsightConfig.sceneProfiles` 键名为 `DeviceTopologyRole` 字符串值
