import { defineStore } from 'pinia';
import { productApi } from '../api';
import type { Product, ProductAddPayload } from '../types/api';

/**
 * 产品状态
 */
export interface ProductState {
  products: Product[];
  currentProduct: Product | null;
  loading: boolean;
  error: string | null;
}

/**
 * 产品状态管理
 */
export const useProductStore = defineStore('product', {
  state: (): ProductState => ({
    products: [],
    currentProduct: null,
    loading: false,
    error: null
  }),
  getters: {
    /**
     * 获取产品列表
     */
    productList: (state) => state.products,
    /**
     * 获取当前产品
     */
    getCurrentProduct: (state) => state.currentProduct,
    /**
     * 检查是否加载中
     */
    isLoading: (state) => state.loading,
    /**
     * 获取错误信息
     */
    getError: (state) => state.error
  },
  actions: {
    /**
     * 加载所有产品
     */
    async loadProducts() {
      this.loading = true;
      this.error = null;
      try {
        const response = await productApi.getAllProducts();
        this.products = response.data || [];
      } catch (error) {
        this.error = error instanceof Error ? error.message : '加载产品失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    /**
     * 根据ID加载产品
     */
    async loadProductById(id: string | number) {
      this.loading = true;
      this.error = null;
      try {
        const response = await productApi.getProductById(id);
        this.currentProduct = response.data;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '加载产品详情失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    /**
     * 添加产品
     */
    async addProduct(payload: ProductAddPayload) {
      this.loading = true;
      this.error = null;
      try {
        const response = await productApi.addProduct(payload);
        this.products.push(response.data);
        return response.data;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '添加产品失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    /**
     * 更新产品
     */
    async updateProduct(id: string | number, payload: Partial<ProductAddPayload>) {
      this.loading = true;
      this.error = null;
      try {
        const response = await productApi.updateProduct(id, payload);
        const index = this.products.findIndex(p => p.id === id);
        if (index !== -1) {
          this.products[index] = response.data;
        }
        if (this.currentProduct && this.currentProduct.id === id) {
          this.currentProduct = response.data;
        }
        return response.data;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '更新产品失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    /**
     * 删除产品
     */
    async deleteProduct(id: string | number) {
      this.loading = true;
      this.error = null;
      try {
        await productApi.deleteProduct(id);
        this.products = this.products.filter(p => p.id !== id);
        if (this.currentProduct && this.currentProduct.id === id) {
          this.currentProduct = null;
        }
      } catch (error) {
        this.error = error instanceof Error ? error.message : '删除产品失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    /**
     * 设置当前产品
     */
    setCurrentProduct(product: Product | null) {
      this.currentProduct = product;
    },
    /**
     * 清空状态
     */
    resetState() {
      this.products = [];
      this.currentProduct = null;
      this.loading = false;
      this.error = null;
    }
  }
});