# 设备拓扑角色洞察统一 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一设备拓扑角色解析（collector_parent / collector_child / standalone），按角色过滤对象洞察属性快照与趋势指标，为子设备 sensor_state 补充健康语义，并让同一产品下的多场景洞察模板按角色分治。

**Architecture:** 后端新增 `DeviceTopologyRoleResolver` 统一角色解析（替代前端散落的 nodeType=2 + productKey 兼容判断）；`listProperties` 读侧按角色过滤返回属性；子设备总览补充 `sensorStateHealth` 枚举；产品 `metadataJson.objectInsight` 扩展多 scene profile。前端消费新字段，移除本地 nodeType 硬编码。

**Tech Stack:** Spring Boot / MyBatis-Plus / Vue 3 / TypeScript / Element Plus

---

## Verification Status (2026-04-22)

- **`DeviceTopologyRole.java`** — 已存在，含 `code`/`desc` 字段（比初版更丰富）。Task 1 标记为 DONE。
- **`DeviceRelation.childProductId`** — 已确认存在（`Long` 类型），Task 2 的 `isCollectorChildByRelation` 实现无需修改。Task 3 简化为编译验证。
- **`DeviceInsightPropertyFilter.java`** — 原计划列出但无任务引用，已移除。
- **VO 类现状** — `CollectorChildInsightChildVO` 和 `CollectorChildInsightOverviewVO` 与计划"before"状态一致。

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `spring-boot-iot-device/.../model/DeviceTopologyRole.java` | **Already exists** | 拓扑角色枚举：COLLECTOR_PARENT / COLLECTOR_CHILD / STANDALONE（含 code/desc） |
| `spring-boot-iot-device/.../service/DeviceTopologyRoleResolver.java` | Create | 统一角色解析服务接口，替代散落判断 |
| `spring-boot-iot-device/.../service/impl/DeviceTopologyRoleResolverImpl.java` | Create | 角色解析实现：nodeType + productKey + 关系表 |
| `spring-boot-iot-device/.../vo/SensorStateHealth.java` | Create | 子设备传感器健康语义枚举 |
| `spring-boot-iot-device/.../vo/CollectorChildInsightChildVO.java` | Modify | 新增 sensorStateHealth 字段 |
| `spring-boot-iot-device/.../vo/CollectorChildInsightOverviewVO.java` | Modify | 新增 missingChildCount / staleChildCount |
| `spring-boot-iot-device/.../vo/DevicePropertyInsightVO.java` | Create | 洞察属性列表 + topologyRole 包装 VO |
| `spring-boot-iot-device/.../service/impl/CollectorChildInsightServiceImpl.java` | Modify | 接入 DeviceTopologyRoleResolver，计算 sensorStateHealth |
| `spring-boot-iot-device/.../service/impl/DeviceServiceImpl.java` | Modify | listProperties 按 topologyRole 过滤 |
| `spring-boot-iot-device/.../controller/DeviceCollectorInsightController.java` | Modify | 洞察属性列表接口传递 topologyRole |
| `spring-boot-iot-ui/src/types/api.ts` | Modify | 新增 DeviceTopologyRole / SensorStateHealth 类型 |
| `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts` | Modify | 按 topologyRole 过滤 buildRuntimeProfile 候选 |
| `spring-boot-iot-ui/src/views/DeviceInsightView.vue` | Modify | 使用后端角色，移除本地 nodeType 硬编码 |
| `spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue` | Modify | 渲染 sensorStateHealth 语义（缺报/过期标红） |
| `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue` | Modify | 支持多场景洞察模板编辑 |
| `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts` | Modify | sceneProfiles 读写逻辑 |

---

### Task 1: ~~创建 DeviceTopologyRole 枚举~~ [DONE — 已存在]

**Files:**
- Already exists: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/model/DeviceTopologyRole.java`

现有枚举含 `code` + `desc` 字段，比原计划更丰富，无需修改：

```java
public enum DeviceTopologyRole {
    COLLECTOR_PARENT("collector-parent", "采集器父设备"),
    COLLECTOR_CHILD("collector-child", "采集器子设备"),
    STANDALONE("standalone", "单台直报设备");
    private final String code;
    private final String desc;
}
```

- [x] **Step 1: 枚举已存在，无需操作**

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

已验证 `DeviceRelation` 实体有 `childProductId`（`Long`），`DeviceRelationMapper` 继承 `BaseMapper` 无自定义方法，可直接用 `LambdaQueryWrapper` 查询。

```java
package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceRelation;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceRelationMapper;
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
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
            new LambdaQueryWrapper<Device>()
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
        if (productId == null) {
            return false;
        }
        // DeviceRelation 实体已确认有 childProductId 字段 (Long)
        return deviceRelationMapper.exists(
            new LambdaQueryWrapper<DeviceRelation>()
                .eq(DeviceRelation::getChildProductId, productId)
                .eq(DeviceRelation::getDeleted, 0)
        );
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceTopologyRoleResolver.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTopologyRoleResolverImpl.java
git commit -m "feat: add DeviceTopologyRoleResolver as single source of truth for device role"
```

---

### Task 3: 创建 SensorStateHealth 枚举

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/SensorStateHealth.java`

- [ ] **Step 1: 创建枚举**

```java
package com.ghlzm.iot.device.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SensorStateHealth {
    REPORTED_NORMAL("reported_normal", "已上报-正常"),
    REPORTED_ABNORMAL("reported_abnormal", "已上报-异常"),
    MISSING("missing", "状态缺失"),
    STALE("stale", "状态过期");

    private final String code;
    private final String desc;
}
```

- [ ] **Step 2: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/SensorStateHealth.java
git commit -m "feat: add SensorStateHealth enum for sub-device state health semantics"
```

---

### Task 4: 扩展 CollectorChildInsightChildVO 与 CollectorChildInsightOverviewVO

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightChildVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightOverviewVO.java`

当前 `CollectorChildInsightChildVO.java` 内容（2026-04-22 确认）：

```java
@Data
public class CollectorChildInsightChildVO {
    private String logicalChannelCode;
    private String childDeviceCode;
    private String childDeviceName;
    private String childProductKey;
    private String collectorLinkState;
    private String sensorStateValue;
    private LocalDateTime lastReportTime;
    private List<String> recommendedMetricIdentifiers;
    private List<CollectorChildInsightMetricVO> metrics;
}
```

当前 `CollectorChildInsightOverviewVO.java` 内容（2026-04-22 确认）：

```java
@Data
public class CollectorChildInsightOverviewVO {
    private String parentDeviceCode;
    private Integer parentOnlineStatus;
    private Integer childCount;
    private Integer reachableChildCount;
    private Integer sensorStateReportedCount;
    private Integer recommendedMetricCount;
    private List<CollectorChildInsightChildVO> children;
}
```

- [ ] **Step 1: 给 CollectorChildInsightChildVO 新增 sensorStateHealth 字段**

在 `sensorStateValue` 字段后新增：

```java
private SensorStateHealth sensorStateHealth;
```

并添加 import：

```java
import com.ghlzm.iot.device.vo.SensorStateHealth;
```

注意：`SensorStateHealth` 和 `CollectorChildInsightChildVO` 在同一个 `vo` 包下，import 可能不需要（同包引用）。如果编译器报错则删除该 import。

- [ ] **Step 2: 给 CollectorChildInsightOverviewVO 新增缺报统计字段**

在 `sensorStateReportedCount` 字段后新增：

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

### Task 5: 改造 CollectorChildInsightServiceImpl——接入角色解析器与健康语义

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java`

当前 `CollectorChildInsightServiceImpl.java` 构造函数（2026-04-22 确认）：

```java
public CollectorChildInsightServiceImpl(DeviceService deviceService,
                                        DeviceRelationService deviceRelationService,
                                        RiskMetricCatalogReadMapper riskMetricCatalogReadMapper) {
    this.deviceService = deviceService;
    this.deviceRelationService = deviceRelationService;
    this.riskMetricCatalogReadMapper = riskMetricCatalogReadMapper;
}
```

- [ ] **Step 1: 注入 DeviceTopologyRoleResolver（构造函数新增依赖）**

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

添加 import：

```java
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
import com.ghlzm.iot.device.vo.SensorStateHealth;
import java.time.LocalDateTime;
import java.time.Duration;
```

- [ ] **Step 2: 改造 toChildOverview 计算 sensorStateHealth**

在 `toChildOverview` 方法的 `item.setSensorStateValue(resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER));` 之后新增：

```java
item.setSensorStateHealth(resolveSensorStateHealth(child, properties));
```

- [ ] **Step 3: 新增 resolveSensorStateHealth 方法**

在 `CollectorChildInsightServiceImpl` 类底部（`firstNonBlank` 方法之后）新增：

```java
private SensorStateHealth resolveSensorStateHealth(DeviceDetailVO child, List<DeviceProperty> properties) {
    String sensorStateValue = resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER);
    if (!hasText(sensorStateValue)) {
        if (child == null || child.getLastReportTime() == null) {
            return SensorStateHealth.MISSING;
        }
        long hoursSinceReport = Duration.between(child.getLastReportTime(), LocalDateTime.now()).toHours();
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

业务语义说明：
- `1` / `online` / `正常` → REPORTED_NORMAL
- `0` / `offline` / `异常` / 其他值 → REPORTED_ABNORMAL（sensor_state=0 在当前业务中表示离线/异常）
- 空值 + 最近有上报 → MISSING
- 空值 + 上报超过 24h → STALE

- [ ] **Step 4: 改造 getOverview 计算 missingChildCount 和 staleChildCount**

在 `getOverview` 方法中，`overview.setSensorStateReportedCount(...)` 之后新增：

```java
overview.setMissingChildCount((int) children.stream()
        .filter(child -> SensorStateHealth.MISSING.equals(child.getSensorStateHealth()))
        .count());
overview.setStaleChildCount((int) children.stream()
        .filter(child -> SensorStateHealth.STALE.equals(child.getSensorStateHealth()))
        .count());
```

- [ ] **Step 5: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java
git commit -m "feat: integrate topology role resolver and sensor_state health semantics into collector child insight"
```

---

### Task 6: 后端 listProperties 按拓扑角色过滤

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceService.java`（接口新增方法签名）
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`

当前 `DeviceServiceImpl.listProperties` 实现（2026-04-22 确认，lines 451-466）：

```java
public List<DeviceProperty> listProperties(Long currentUserId, String deviceCode) {
    Device device = getRequiredByCode(deviceCode);
    ensureDeviceAccessible(currentUserId, device);
    List<DeviceProperty> properties = devicePropertyMapper.selectList(
            new LambdaQueryWrapper<DeviceProperty>()
                    .eq(DeviceProperty::getDeviceId, device.getId())
                    .orderByDesc(DeviceProperty::getUpdateTime)
    );
    overlayLatestPropertyMetadata(device, properties);
    return properties;
}
```

- [ ] **Step 1: 注入 DeviceTopologyRoleResolver**

在 `DeviceServiceImpl` 中新增字段和构造函数参数：

```java
private final DeviceTopologyRoleResolver topologyRoleResolver;
```

在构造函数中新增对应参数。

- [ ] **Step 2: 新增 listPropertiesForInsight 方法签名**

在 `DeviceService` 接口新增：

```java
List<DeviceProperty> listPropertiesForInsight(Long currentUserId, String deviceCode);
```

- [ ] **Step 3: 在 DeviceServiceImpl 实现 listPropertiesForInsight**

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

- [ ] **Step 4: 实现 filterPropertiesByRole 和辅助方法**

在 `DeviceServiceImpl` 类中新增：

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
        for (String childId : CHILD_BUSINESS_IDENTIFIERS) {
            if (identifier.equals(childId) || identifier.endsWith("." + childId)) {
                return false;
            }
        }
        return true;
    }
    if (role == DeviceTopologyRole.COLLECTOR_CHILD) {
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

添加 import：

```java
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
import java.util.Set;
import java.util.stream.Collectors;
```

> 注意：此过滤逻辑是初版启发式规则。Task 10 引入的 `sceneProfiles` 会成为长期真相源。过渡期两套共存，`sceneProfiles` 优先，回退到启发式。

- [ ] **Step 5: 编译验证**

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java
git commit -m "feat: add role-based property filtering for insight read side"
```

---

### Task 7: 后端洞察接口——新增 topologyRole + insight/properties 端点

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DevicePropertyInsightVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java`

- [ ] **Step 1: 创建 DevicePropertyInsightVO**

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

- [ ] **Step 2: 在 DeviceCollectorInsightController 新增角色查询接口**

读取当前控制器文件 `DeviceCollectorInsightController.java`，在类中注入 `DeviceTopologyRoleResolver` 和 `DeviceService`（如尚未注入），然后新增：

```java
@GetMapping("/api/device/{deviceCode}/topology-role")
public R<DeviceTopologyRole> getTopologyRole(@PathVariable String deviceCode,
                                              Authentication authentication) {
    requireCurrentUserId(authentication);
    Device device = deviceService.getRequiredByCode(deviceCode);
    return R.ok(topologyRoleResolver.resolve(device.getProductId(), device.getNodeType(), device.getProductKey()));
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
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DevicePropertyInsightVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java
git commit -m "feat: add insight properties endpoint with topology role metadata"
```

---

### Task 8: 前端类型扩展——DeviceTopologyRole 与 SensorStateHealth

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`

- [ ] **Step 1: 新增类型定义**

在 `api.ts` 中合适位置新增：

```typescript
export type DeviceTopologyRole = 'COLLECTOR_PARENT' | 'COLLECTOR_CHILD' | 'STANDALONE';

export type SensorStateHealth = 'REPORTED_NORMAL' | 'REPORTED_ABNORMAL' | 'MISSING' | 'STALE';
```

- [ ] **Step 2: 扩展 CollectorChildInsightChild 接口**

在 `CollectorChildInsightChild` 接口中新增：

```typescript
sensorStateHealth?: SensorStateHealth | null;
```

- [ ] **Step 3: 扩展 CollectorChildInsightOverview 接口**

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

### Task 9: 前端 DeviceInsightView 消费后端拓扑角色

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

当前关键代码（2026-04-22 确认）：

- Line 280: `const isCollectorParentInsight = computed(() => Number(device.value?.nodeType) === 2);`
- Lines 897-906: `shouldLoadCollectorInsightOverview` 含 `nf-collect-rtu-v1` productKey 硬编码兼容

- [ ] **Step 1: 新增 topologyRole 状态和 import**

在 `<script setup>` 区域，找到 import 区块，新增：

```typescript
import type { DeviceTopologyRole, DevicePropertyInsightVO } from '@/types/api';
```

在状态定义区域新增：

```typescript
const topologyRole = ref<DeviceTopologyRole>('STANDALONE');
```

- [ ] **Step 2: 改造属性加载——使用 insight/properties 接口**

找到当前加载属性的代码（调用 `/api/device/{deviceCode}/properties`），替换为：

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

在 `loadInsight` 函数中，把原来直接调用 `listProperties` / `getProperties` 的位置替换为调用 `loadInsightProperties()`。

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

- [ ] **Step 6: 编译验证**

```bash
cd spring-boot-iot-ui && npx vue-tsc --noEmit 2>&1 | tail -20
```

Expected: 无 TypeScript 编译错误

- [ ] **Step 7: 提交**

```bash
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue
git commit -m "feat: consume backend topology role in DeviceInsightView, remove local nodeType hardcoding"
```

---

### Task 10: 前端 CollectorChildInsightPanel 渲染 sensorStateHealth 语义

**Files:**
- Modify: `spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue`

- [ ] **Step 1: 新增 import**

在 `<script setup>` 的 import 区块新增：

```typescript
import type { SensorStateHealth } from '@/types/api';
```

- [ ] **Step 2: 新增 sensorStateLabel 和 sensorStateHealthClass 函数**

在 `<script setup>` 中新增：

```typescript
function sensorStateLabel(sensorStateValue?: string | null, health?: SensorStateHealth | null): string {
  if (health === 'MISSING') return '状态缺失';
  if (health === 'STALE') return '状态过期';
  if (health === 'REPORTED_ABNORMAL') return '传感器异常';
  if (health === 'REPORTED_NORMAL') return '传感器正常';
  return sensorStateValue?.trim() ? `传感器状态 ${sensorStateValue.trim()}` : '传感器状态 --';
}

function sensorStateHealthClass(health?: SensorStateHealth | null): string {
  if (health === 'MISSING' || health === 'STALE') return 'collector-child-insight-panel__badge--missing';
  if (health === 'REPORTED_ABNORMAL') return 'collector-child-insight-panel__badge--abnormal';
  if (health === 'REPORTED_NORMAL') return 'collector-child-insight-panel__badge--normal';
  return 'collector-child-insight-panel__badge--state';
}
```

如果已有旧的 `sensorStateLabel` 函数（只接收 `sensorStateValue`），替换为上述新版本。

- [ ] **Step 3: 更新模板中 badge 绑定**

将模板中 sensor_state badge 部分改为：

```html
<span
  class="collector-child-insight-panel__badge"
  :class="sensorStateHealthClass(child.sensorStateHealth)"
>
  {{ sensorStateLabel(child.sensorStateValue, child.sensorStateHealth) }}
</span>
```

- [ ] **Step 4: 新增缺报/过期 badge 样式**

在 `<style>` 区块新增：

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

在概览卡区域（`collector-child-insight-panel__summary-card`）新增两张卡：

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
cd spring-boot-iot-ui && npx vue-tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 7: 提交**

```bash
git add spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue
git commit -m "feat: render sensorStateHealth semantics with MISSING/STALE/ABNORMAL badges"
```

---

### Task 11: 前端 deviceInsightCapability 按 topologyRole 过滤候选

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`

- [ ] **Step 1: 扩展 getInsightCapabilityProfile 参数类型**

在 `getInsightCapabilityProfile` 函数的 `source` 参数对象中新增 `topologyRole`：

```typescript
import type { DeviceTopologyRole } from '@/types/api';

// 在 InsightCapabilitySource 类型（或 source 参数类型定义处）新增：
topologyRole?: DeviceTopologyRole | null;
```

- [ ] **Step 2: 新增标识符分类辅助函数**

在 `deviceInsightCapability.ts` 中新增：

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

- [ ] **Step 3: 在 buildRuntimeProfile 中按角色过滤候选**

在 `buildRuntimeProfile` 函数中，`candidates` 生成后（`let candidates = ...` 之后），新增过滤：

```typescript
if (source.topologyRole === 'COLLECTOR_PARENT') {
  candidates = candidates.filter((item) => isStatusMetric(item.identifier) && !isChildBusinessIdentifier(item.identifier));
} else if (source.topologyRole === 'COLLECTOR_CHILD') {
  candidates = candidates.filter((item) => !isParentRuntimeIdentifier(item.identifier));
}
```

注意：`isStatusMetric` 是现有函数（或 `STATUS_METRIC_PATTERN` 匹配），用于判断是否为状态/运行参数类指标。如果 `buildRuntimeProfile` 中没有独立的 `isStatusMetric` 函数，则直接用 `STATUS_METRIC_PATTERN.test(item.identifier)` 替代。

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot-ui && npx vue-tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-ui/src/utils/deviceInsightCapability.ts
git commit -m "feat: filter insight profile candidates by device topology role"
```

---

### Task 12: DeviceInsightView 传递 topologyRole 到 capability profile

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
cd spring-boot-iot-ui && npx vue-tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 3: 提交**

```bash
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue
git commit -m "feat: pass topology role to insight capability profile builder"
```

---

### Task 13: 产品 metadataJson 扩展多场景洞察模板——类型 + 读侧

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
- Modify: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`

- [ ] **Step 1: 扩展 api.ts 类型**

在 `ProductObjectInsightConfig` 接口中新增 `sceneProfiles` 字段：

```typescript
export interface ProductObjectInsightConfig {
  customMetrics?: ProductObjectInsightCustomMetricConfig[] | null;
  sceneProfiles?: Record<string, ProductObjectInsightCustomMetricConfig[]> | null;
}
```

- [ ] **Step 2: 扩展 resolveMetadataCustomMetrics 优先消费 sceneProfiles**

在 `deviceInsightCapability.ts` 的 `resolveMetadataCustomMetrics`（或 `applyCustomMetrics` 中解析 metadataJson 的位置），当 `topologyRole` 存在且 `sceneProfiles` 中有对应场景时，优先使用场景配置：

```typescript
function resolveMetadataCustomMetrics(
  profile: InsightCapabilityProfile,
  metadataJson: string | null,
  source: InsightCapabilitySource
): InsightCustomMetricDefinition[] {
  const config = parseProductObjectInsightConfig(metadataJson);
  if (!config) return [];
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

- [ ] **Step 3: 扩展 productObjectInsightConfig.ts 的 parseProductObjectInsightConfig**

确认 `parseProductObjectInsightConfig` 能正确解析 `sceneProfiles` 字段。如果当前解析逻辑只读 `customMetrics`，需补充 `sceneProfiles` 的解析。

- [ ] **Step 4: 编译验证**

```bash
cd spring-boot-iot-ui && npx vue-tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 5: 提交**

```bash
git add spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/utils/deviceInsightCapability.ts spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts
git commit -m "feat: add sceneProfiles to product insight config, prefer scene-specific metrics when available"
```

---

### Task 14: ProductObjectInsightConfigEditor 支持多场景洞察模板编辑

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue`

- [ ] **Step 1: 新增 activeScene 状态**

在 `<script setup>` 中新增：

```typescript
import type { DeviceTopologyRole } from '@/types/api';

type InsightSceneKey = 'default' | DeviceTopologyRole;
const activeScene = ref<InsightSceneKey>('default');
```

- [ ] **Step 2: 新增场景 Tab UI**

在模板中，在指标列表上方新增 Tab 切换：

```html
<div class="product-object-insight-config-editor__scene-tabs">
  <button
    v-for="scene in sceneOptions"
    :key="scene.key"
    :class="['product-object-insight-config-editor__scene-tab', { 'is-active': activeScene === scene.key }]"
    @click="activeScene = scene.key"
  >
    {{ scene.label }}
  </button>
</div>
```

`sceneOptions` 定义：

```typescript
const sceneOptions = [
  { key: 'default' as InsightSceneKey, label: '默认配置' },
  { key: 'COLLECTOR_PARENT' as InsightSceneKey, label: '采集器父设备' },
  { key: 'COLLECTOR_CHILD' as InsightSceneKey, label: '采集器子设备' },
  { key: 'STANDALONE' as InsightSceneKey, label: '单台直报' },
];
```

- [ ] **Step 3: 按 activeScene 路由读/写**

读取时：如果 `activeScene` 非 `'default'`，从 `config.sceneProfiles?.[activeScene]` 读取指标列表；否则从 `config.customMetrics` 读取。

保存时：如果 `activeScene` 非 `'default'`，写入 `config.sceneProfiles[activeScene]`；否则写入 `config.customMetrics`。

核心改造：将现有 `customMetrics` ref 的读写路径改为根据 `activeScene` 路由。复用现有编辑逻辑，只在数据源/回写处做路由。

- [ ] **Step 4: 新增场景 Tab 样式**

```css
.product-object-insight-config-editor__scene-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
}

.product-object-insight-config-editor__scene-tab {
  padding: 4px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
}

.product-object-insight-config-editor__scene-tab.is-active {
  background: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}
```

- [ ] **Step 5: 编译验证**

```bash
cd spring-boot-iot-ui && npx vue-tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 6: 提交**

```bash
git add spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue
git commit -m "feat: add multi-scene insight profile tabs to ProductObjectInsightConfigEditor"
```

---

### Task 15: 存量治理——清理深部位移产品配置

**Files:**
- Create: `scripts/fix-deep-displacement-insight-profiles.sql`

- [ ] **Step 1: 读取 nf-monitor-deep-displacement-v1 产品 metadataJson**

通过 API 或直接查询数据库，获取 `product_key = 'nf-monitor-deep-displacement-v1'` 的 `metadata_json.objectInsight.customMetrics[]`。

当前 init-data 中该产品的 customMetrics（2026-04-22 确认）：
- `dispsX`（顺滑动方向累计变形量）
- `dispsY`（垂直坡面方向累计变形量）
- `sensor_state`（传感器状态）

- [ ] **Step 2: 分析字段归属**

- `dispsX / dispsY / sensor_state` → 属于 `COLLECTOR_CHILD` + `STANDALONE` 共享
- 深部位移产品自身无运行参数（温湿度/4G信号等属于父采集器产品 `nf-collect-rtu-v1`）
- 因此 `customMetrics`（默认/standalone）保留 `dispsX / dispsY / sensor_state`
- `sceneProfiles.COLLECTOR_CHILD` 同样保留 `dispsX / dispsY / sensor_state`
- `sceneProfiles.COLLECTOR_PARENT` 应为空（深部位移不是父设备产品）

- [ ] **Step 3: 创建 SQL 脚本**

```sql
-- 深部位移产品：为 collector_child 场景创建专用洞察模板
-- customMetrics 保持不变（作为 standalone 默认配置）
-- COLLECTOR_CHILD 场景复制同样的指标（未来可独立演化）

UPDATE iot_product
SET metadata_json = JSON_SET(
  metadata_json,
  '$.objectInsight.sceneProfiles',
  JSON_OBJECT(
    'COLLECTOR_CHILD', JSON_ARRAY(
      JSON_OBJECT('identifier', 'dispsX', 'displayName', '顺滑动方向累计变形量', 'group', 'measure', 'includeInTrend', true, 'enabled', true, 'sortNo', 1),
      JSON_OBJECT('identifier', 'dispsY', 'displayName', '垂直坡面方向累计变形量', 'group', 'measure', 'includeInTrend', true, 'enabled', true, 'sortNo', 2),
      JSON_OBJECT('identifier', 'sensor_state', 'displayName', '传感器状态', 'group', 'statusEvent', 'includeInTrend', true, 'enabled', true, 'sortNo', 3)
    )
  )
)
WHERE product_key = 'nf-monitor-deep-displacement-v1'
  AND metadata_json IS NOT NULL;
```

- [ ] **Step 4: 提交 SQL 脚本**

```bash
git add scripts/fix-deep-displacement-insight-profiles.sql
git commit -m "fix: add collector_child scene profile for deep displacement product"
```

---

### Task 16: 存量治理——清理激光测距产品配置

**Files:**
- Create: `scripts/fix-laser-rangefinder-insight-profiles.sql`

- [ ] **Step 1: 读取 nf-monitor-laser-rangefinder-v1 产品 metadataJson**

当前 init-data 中该产品的 customMetrics（2026-04-22 确认）：
- `value`（激光测距值）
- `sensor_state`（传感器状态）

- [ ] **Step 2: 分析字段归属**

- `value / sensor_state` → `COLLECTOR_CHILD` + `STANDALONE` 共享
- 激光测距产品自身无运行参数
- `customMetrics`（默认/standalone）保留 `value / sensor_state`
- `sceneProfiles.COLLECTOR_CHILD` 同样保留 `value / sensor_state`

- [ ] **Step 3: 创建 SQL 脚本**

```sql
-- 激光测距产品：为 collector_child 场景创建专用洞察模板
UPDATE iot_product
SET metadata_json = JSON_SET(
  metadata_json,
  '$.objectInsight.sceneProfiles',
  JSON_OBJECT(
    'COLLECTOR_CHILD', JSON_ARRAY(
      JSON_OBJECT('identifier', 'value', 'displayName', '激光测距值', 'group', 'measure', 'includeInTrend', true, 'enabled', true, 'sortNo', 1),
      JSON_OBJECT('identifier', 'sensor_state', 'displayName', '传感器状态', 'group', 'statusEvent', 'includeInTrend', true, 'enabled', true, 'sortNo', 2)
    )
  )
)
WHERE product_key = 'nf-monitor-laser-rangefinder-v1'
  AND metadata_json IS NOT NULL;
```

- [ ] **Step 4: 提交 SQL 脚本**

```bash
git add scripts/fix-laser-rangefinder-insight-profiles.sql
git commit -m "fix: add collector_child scene profile for laser rangefinder product"
```

---

### Task 17: 验收基线——六类设备场景 + 缺报场景

**Files:**
- No code changes, manual testing

- [ ] **Step 1: 深部位移单台设备**

验证 `topologyRole = STANDALONE`，快照显示 `dispsX / dispsY + sensor_state`（如该设备有本机运行参数也一并显示），趋势指标正确。

- [ ] **Step 2: 深部位移父基站**

验证 `topologyRole = COLLECTOR_PARENT`，快照只显示父设备运行参数（signal_4g / battery / temp / humidity / ext_power_volt 等），不显示 dispsX/dispsY/sensor_state。

- [ ] **Step 3: 深部位移子设备**

验证 `topologyRole = COLLECTOR_CHILD`，快照只显示 `dispsX / dispsY + sensor_state`，不显示父设备运行参数。

- [ ] **Step 4: 激光测距单台设备**

验证 `topologyRole = STANDALONE`，快照显示 `value / sensor_state`。

- [ ] **Step 5: 激光测距父基站**

验证 `topologyRole = COLLECTOR_PARENT`，快照只显示父设备运行参数。

- [ ] **Step 6: 激光测距子设备**

验证 `topologyRole = COLLECTOR_CHILD`，快照只显示 `value / sensor_state`。

- [ ] **Step 7: 8 子设备只报 1 个状态的缺报场景**

验证 SK00FB0D1310195 父设备洞察页面：
- L1_SW_1 显示 `REPORTED_NORMAL`（绿色 badge "传感器正常"）
- L1_SW_2~8 显示 `MISSING`（橙色 badge "状态缺失"）
- 概览卡"状态缺失"= 7

---

## Self-Review Checklist

### 1. Spec Coverage

| 需求 | 对应 Task |
|------|-----------|
| 统一角色解析器 | Task 1-2 |
| 父设备快照只显示父运行参数 | Task 6, 9 |
| 子设备快照过滤父运行参数 | Task 6, 11 |
| sensor_state 健康语义 | Task 3-5, 10 |
| 多场景洞察模板（类型+读侧） | Task 13 |
| 多场景洞察模板（编辑器） | Task 14 |
| 前端消费 topologyRole | Task 8-9, 12 |
| 存量治理（深部位移） | Task 15 |
| 存量治理（激光测距） | Task 16 |
| 验收基线 | Task 17 |

### 2. Placeholder Scan

无 TBD/TODO/实现稍后占位符。所有步骤都包含具体代码。

### 3. Type Consistency

- `DeviceTopologyRole` 枚举值在 Java 和 TypeScript 之间一致：`COLLECTOR_PARENT` / `COLLECTOR_CHILD` / `STANDALONE`
- `SensorStateHealth` 枚举值一致：`REPORTED_NORMAL` / `REPORTED_ABNORMAL` / `MISSING` / `STALE`
- `DevicePropertyInsightVO.topologyRole` 类型为 `DeviceTopologyRole`
- `CollectorChildInsightChildVO.sensorStateHealth` 类型为 `SensorStateHealth`
- `ProductObjectInsightConfig.sceneProfiles` 键名为 `DeviceTopologyRole` 字符串值
- Java 端 `DeviceTopologyRole` 已有 `code`/`desc` 字段，与前端 string union 类型通过 `COLLECTOR_PARENT` 等枚举名（Jackson 默认序列化为枚举名）对齐

### 4. Verified Against Codebase

- `DeviceTopologyRole.java` — 已存在，含 `code`/`desc`，无需修改
- `DeviceRelation.childProductId` — 已确认存在（`Long`），`isCollectorChildByRelation` 实现正确
- `DeviceRelationMapper` — 无自定义方法，使用 `BaseMapper` + `LambdaQueryWrapper` 即可
- `CollectorChildInsightChildVO` / `CollectorChildInsightOverviewVO` — 字段与计划 "before" 状态一致
- `CollectorChildInsightServiceImpl` — 构造函数、`toChildOverview`、`getOverview` 实现与计划描述一致
