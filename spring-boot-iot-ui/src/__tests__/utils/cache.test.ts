import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { cacheManager } from '@/utils/cache';

describe('CacheManager', () => {
      beforeEach(() => {
            cacheManager.clear();
      });

      afterEach(() => {
            cacheManager.clear();
      });

      it('should set and get data', () => {
            cacheManager.set('key', 'value');
            const result = cacheManager.get<string>('key');
            expect(result).toBe('value');
      });

      it('should return null for expired data', async () => {
            cacheManager.set('key', 'value', 100); // 100ms TTL
            expect(cacheManager.get<string>('key')).toBe('value');

            await new Promise(resolve => setTimeout(resolve, 150));
            expect(cacheManager.get<string>('key')).toBeNull();
      });

      it('should clear specific key', () => {
            cacheManager.set('key1', 'value1');
            cacheManager.set('key2', 'value2');
            cacheManager.clear('key1');
            expect(cacheManager.get<string>('key1')).toBeNull();
            expect(cacheManager.get<string>('key2')).toBe('value2');
      });

      it('should clear all data', () => {
            cacheManager.set('key1', 'value1');
            cacheManager.set('key2', 'value2');
            cacheManager.clear();
            expect(cacheManager.get<string>('key1')).toBeNull();
            expect(cacheManager.get<string>('key2')).toBeNull();
      });

      it('should check if key exists', () => {
            cacheManager.set('key', 'value');
            expect(cacheManager.has('key')).toBe(true);
            expect(cacheManager.has('nonexistent')).toBe(false);
      });

      it('should return correct size', () => {
            cacheManager.set('key1', 'value1');
            cacheManager.set('key2', 'value2');
            expect(cacheManager.size()).toBe(2);
      });

      it('should use default TTL', async () => {
            cacheManager.setDefaultTTL(100); // 100ms TTL
            cacheManager.set('key', 'value');
            expect(cacheManager.get<string>('key')).toBe('value');

            await new Promise(resolve => setTimeout(resolve, 150));
            expect(cacheManager.get<string>('key')).toBeNull();
      });
});
