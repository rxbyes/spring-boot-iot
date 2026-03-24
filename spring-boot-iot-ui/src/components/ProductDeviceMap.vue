<template>
  <div class="product-device-map">
    <div v-if="loading" class="product-device-map__loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载地图...</span>
    </div>
    <div v-if="error" class="product-device-map__error">
      <el-icon><Warning /></el-icon>
      <span>{{ error }}</span>
      <StandardActionLink @click="handleRetry">重试</StandardActionLink>
    </div>
    <div ref="mapContainer" class="product-device-map__container" />
    <div v-if="!loading && !error && hasGeolocationData" class="product-device-map__legend">
      <div class="product-device-map__legend-item" :class="`product-device-map__legend--${item.type}`" v-for="item in legendItems" :key="item.type">
        <span class="product-device-map__legend-color" :style="{ backgroundColor: item.color }"></span>
        <span class="product-device-map__legend-label">{{ item.label }}</span>
        <span class="product-device-map__legend-count">{{ item.count }}</span>
      </div>
    </div>
    <div v-if="!loading && !error && !hasGeolocationData" class="product-device-map__empty">
      <el-icon><Position /></el-icon>
      <span>暂无设备地理分布数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, watch, onUnmounted } from 'vue'
import { ElIcon } from 'element-plus'
import { Loading, Warning, Position } from '@element-plus/icons-vue'

// 声明地图库类型
declare global {
  interface Window {
    MapboxGL?: any
    BMapGL?: any
  }
}

interface LocationData {
  province?: string | null;
  city?: string | null;
  district?: string | null;
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
}

interface Props {
  modelValue: boolean;
  productKey?: string | null;
  devices?: LocationData[];
  loading?: boolean;
}

interface Emits {
  (event: 'close'): void;
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const mapContainer = ref<HTMLDivElement | null>(null)
const error = ref<string | null>(null)
let mapInstance: any = null
let markers: any[] = []

// 图例配置
const legendItems = computed(() => {
  if (!props.devices || props.devices.length === 0) return []
  
  const hasGeoDevices = props.devices.filter(d => 
    d.latitude !== null && d.longitude !== null && 
    d.latitude !== undefined && d.longitude !== undefined
  )
  const noGeoDevices = props.devices.filter(d => 
    d.latitude === null || d.longitude === null || 
    d.latitude === undefined || d.longitude === undefined
  )
  
  const items: Array<{ type: string; label: string; count: number; color: string }> = []
  
  if (hasGeoDevices.length > 0) {
    items.push({ type: 'online', label: '已定位设备', count: hasGeoDevices.length, color: '#67c23a' })
  }
  if (noGeoDevices.length > 0) {
    items.push({ type: 'offline', label: '未定位设备', count: noGeoDevices.length, color: '#909399' })
  }
  
  return items
})

// 是否有地理定位数据
const hasGeolocationData = computed(() => {
  if (!props.devices) return false
  return props.devices.some(d => 
    d.latitude !== null && d.longitude !== null && 
    d.latitude !== undefined && d.longitude !== undefined
  )
})

// 销毁地图实例
function destroyMap() {
  if (mapInstance) {
    try {
      mapInstance.remove()
    } catch (e) {
      console.warn('销毁地图失败', e)
    }
    mapInstance = null
  }
  markers.forEach(marker => {
    try {
      marker.remove()
    } catch (e) {
      console.warn('销毁标记失败', e)
    }
  })
  markers = []
}

// 初始化地图
function initMap() {
  if (!mapContainer.value) return
  
  // 清空容器
  mapContainer.value.innerHTML = ''
  
  // 检查是否有地图 API Key
  // 这里应该使用 Mapbox GL JS 或百度地图
  // 需要配置 mapboxPublicKey 或 baiduMapPublicKey
  const mapboxKey = import.meta.env.VITE_MAPBOX_PUBLIC_KEY || ''
  const baiduKey = import.meta.env.VITE_BAIDU_MAP_PUBLIC_KEY || ''
  
  // 使用 Mapbox GL JS
  if (mapboxKey && typeof window !== 'undefined' && window.MapboxGL) {
    initMapboxMap(mapboxKey)
  } else if (baiduKey && typeof window !== 'undefined' && window.BMapGL) {
    initBaiduMap(baiduKey)
  } else {
    // 无 API Key，显示提示
    mapContainer.value.innerHTML = `
      <div style="display:flex;justify-content:center;align-items:center;height:100%;color:#909399;">
        <div style="text-align:center;">
          <el-icon style="font-size:48px;margin-bottom:16px;"><Position /></el-icon>
          <p style="margin:0 0 8px;">地图功能需要配置 API Key</p>
          <p style="margin:0;font-size:12px;">需要集成地图库（Mapbox GL JS 或 百度地图）</p>
          <p style="margin:8px 0 0;font-size:12px;">在 .env 文件中配置 VITE_MAPBOX_PUBLIC_KEY 或 VITE_BAIDU_MAP_PUBLIC_KEY</p>
        </div>
      </div>
    `
  }
}

// 初始化 Mapbox 地图
function initMapboxMap(apiKey: string) {
  if (!mapContainer.value || !window.MapboxGL) return
  
  try {
    const map = new window.MapboxGL.Map({
      container: mapContainer.value,
      style: 'mapbox://styles/mapbox/light-v11',
      center: [104.195397, 35.86166], // 中国中心点
      zoom: 5,
      attributionControl: true,
      pitch: 0
    })
    
    map.on('load', () => {
      mapInstance = map
      addMarkersToMap(map)
    })
    
    map.on('error', (e: any) => {
      console.error('Mapbox 地图加载失败', e)
      error.value = '地图加载失败，请检查 API Key'
    })
  } catch (e) {
    console.error('初始化 Mapbox 地图失败', e)
    error.value = '地图初始化失败'
  }
}

// 初始化百度地图
function initBaiduMap(apiKey: string) {
  if (!mapContainer.value || !window.BMapGL) return
  
  try {
    const map = new window.BMapGL.Map(mapContainer.value)
    const point = new window.BMapGL.Point(104.195397, 35.86166)
    map.centerAndZoom(point, 5)
    map.enableScrollWheelZoom(true)
    
    mapInstance = map
    addMarkersToBaiduMap(map)
  } catch (e) {
    console.error('初始化百度地图失败', e)
    error.value = '地图初始化失败'
  }
}

// 添加标记到 Mapbox 地图
function addMarkersToMap(map: any) {
  if (!props.devices || props.devices.length === 0) return
  
  markers = []
  
  props.devices.forEach((device, index) => {
    if (device.latitude !== null && device.longitude !== null && 
        device.latitude !== undefined && device.longitude !== undefined) {
      const marker = new window.MapboxGL.Marker({ color: '#67c23a' })
        .setLngLat([device.longitude, device.latitude])
        .setPopup(new window.MapboxGL.Popup({ offset: 25 }).setHTML(`
          <div style="padding:8px 12px;">
            <p style="margin:0 0 4px;font-weight:600;">设备 ${index + 1}</p>
            ${device.province ? `<p style="margin:0 0 2px;font-size:12px;">${device.province}${device.city || ''}${device.district || ''}</p>` : ''}
            ${device.address ? `<p style="margin:0 0 2px;font-size:12px;">${device.address}</p>` : ''}
            ${device.latitude !== null ? `<p style="margin:0;font-size:11px;color:#666;">${device.latitude.toFixed(6)}, ${device.longitude.toFixed(6)}</p>` : ''}
          </div>
        `))
        .addTo(map)
      markers.push(marker)
    }
  })
}

// 添加标记到百度地图
function addMarkersToBaiduMap(map: any) {
  if (!props.devices || props.devices.length === 0) return
  
  markers = []
  
  props.devices.forEach((device, index) => {
    if (device.latitude !== null && device.longitude !== null && 
        device.latitude !== undefined && device.longitude !== undefined) {
      const point = new window.BMapGL.Point(device.longitude, device.latitude)
      const marker = new window.BMapGL.Marker(point)
      map.addOverlay(marker)
      
      // 创建信息窗口
      const popupContent = `
        <div style="padding:8px 12px;">
          <p style="margin:0 0 4px;font-weight:600;">设备 ${index + 1}</p>
          ${device.province ? `<p style="margin:0 0 2px;font-size:12px;">${device.province}${device.city || ''}${device.district || ''}</p>` : ''}
          ${device.address ? `<p style="margin:0 0 2px;font-size:12px;">${device.address}</p>` : ''}
          ${device.latitude !== null ? `<p style="margin:0;font-size:11px;color:#666;">${device.latitude.toFixed(6)}, ${device.longitude.toFixed(6)}</p>` : ''}
        </div>
      `
      
      const infoWindow = new window.BMapGL.InfoWindow(popupContent)
      marker.addEventListener('click', () => {
        map.openInfoWindow(infoWindow, point)
      })
      
      markers.push({ marker, infoWindow })
    }
  })
}

// 重试
function handleRetry() {
  error.value = null
  destroyMap()
  initMap()
}

// 组件挂载
onMounted(() => {
  if (props.modelValue) {
    initMap()
  }
})

// 组件卸载
onBeforeUnmount(() => {
  if (mapContainer.value) {
    mapContainer.value.innerHTML = ''
  }
})

// 监听模型值变化
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    initMap()
  }
})
</script>

<style scoped>
.product-device-map {
  position: relative;
  width: 100%;
  height: 400px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-md) + 2px);
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
}

.product-device-map__loading,
.product-device-map__error,
.product-device-map__empty {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  gap: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
}

.product-device-map__error {
  color: var(--danger);
  background: linear-gradient(180deg, rgba(255, 246, 246, 0.98), rgba(255, 241, 241, 0.96));
}

.product-device-map__empty {
  color: var(--text-caption);
}

.product-device-map__loading el-icon,
.product-device-map__error el-icon,
.product-device-map__empty el-icon {
  font-size: 48px;
}

.product-device-map__error span,
.product-device-map__empty span {
  font-size: 14px;
}

.product-device-map__error .el-button {
  margin-top: 8px;
}

.product-device-map__container {
  width: 100%;
  height: 100%;
}

.product-device-map__legend {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: var(--shadow-surface-soft-xs);
  z-index: 100;
}

.product-device-map__legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.product-device-map__legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.product-device-map__legend-label {
  color: var(--text-heading);
}

.product-device-map__legend-count {
  margin-left: auto;
  color: var(--text-caption);
  font-weight: 500;
}
</style>
