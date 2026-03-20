/**
 * 请求缓存管理器
 */
class CacheManager {
      private cache = new Map<string, { data: unknown; timestamp: number; ttl?: number }>();
      private defaultTTL = 30000; // 30秒

      /**
       * 获取缓存数据
       * @param key 缓存键
       * @param ttl 自定义过期时间（毫秒），默认使用 TTL
       * @returns 缓存数据，如果过期或不存在则返回 null
       */
      get<T>(key: string, ttl?: number): T | null {
            const item = this.cache.get(key);
            if (!item) return null;

            // 过期时间优先级：读取参数 > 写入时指定 TTL > 默认 TTL
            const effectiveTTL = ttl ?? item.ttl ?? this.defaultTTL;
            if (Date.now() - item.timestamp > effectiveTTL) {
                  this.cache.delete(key);
                  return null;
            }

            return item.data as T;
      }

      /**
       * 设置缓存数据
       * @param key 缓存键
       * @param data 缓存数据
       * @param ttl 自定义过期时间（毫秒），默认使用 TTL
       */
      set<T>(key: string, data: T, ttl?: number): void {
            this.cache.set(key, {
                  data,
                  timestamp: Date.now(),
                  ttl
            });
      }

      /**
       * 清除缓存数据
       * @param key 缓存键，如果为空则清除所有缓存
       */
      clear(key?: string): void {
            if (key) {
                  this.cache.delete(key);
            } else {
                  this.cache.clear();
            }
      }

      /**
       * 检查缓存是否存在
       * @param key 缓存键
       * @param ttl 自定义过期时间（毫秒），默认使用 TTL
       * @returns 缓存是否存在且未过期
       */
      has(key: string, ttl?: number): boolean {
            const item = this.cache.get(key);
            if (!item) return false;

            const effectiveTTL = ttl ?? item.ttl ?? this.defaultTTL;
            return Date.now() - item.timestamp <= effectiveTTL;
      }

      /**
       * 获取缓存数量
       * @returns 缓存数量
       */
      size(): number {
            return this.cache.size;
      }

      /**
       * 设置默认过期时间
       * @param ttl 默认过期时间（毫秒）
       */
      setDefaultTTL(ttl: number): void {
            this.defaultTTL = ttl;
      }
}

// 导出单例
export const cacheManager = new CacheManager();
