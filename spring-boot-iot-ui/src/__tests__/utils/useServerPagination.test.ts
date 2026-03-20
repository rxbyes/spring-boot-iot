import { describe, expect, it } from 'vitest';
import { useServerPagination } from '@/composables/useServerPagination';

describe('useServerPagination', () => {
  it('should initialize with default values', () => {
    const { pagination } = useServerPagination();

    expect(pagination.pageNum).toBe(1);
    expect(pagination.pageSize).toBe(10);
    expect(pagination.total).toBe(0);
  });

  it('should apply page result to pagination state', () => {
    const { pagination, applyPageResult } = useServerPagination();
    const records = applyPageResult({
      total: 35,
      pageNum: 2,
      pageSize: 20,
      records: [{ id: '1' }, { id: '2' }]
    });

    expect(records).toHaveLength(2);
    expect(pagination.total).toBe(35);
    expect(pagination.pageNum).toBe(2);
    expect(pagination.pageSize).toBe(20);
  });

  it('should support setting total manually', () => {
    const { pagination, setTotal } = useServerPagination();

    setTotal(120);
    expect(pagination.total).toBe(120);

    setTotal(-9);
    expect(pagination.total).toBe(0);
  });

  it('should paginate local records and auto-correct overflow page number', () => {
    const { pagination, applyLocalRecords, setPageSize, setPageNum } = useServerPagination();
    const records = Array.from({ length: 7 }, (_, index) => ({ id: index + 1 }));

    setPageSize(3);
    setPageNum(3);
    const page3 = applyLocalRecords(records);
    expect(page3.map((item) => item.id)).toEqual([7]);
    expect(pagination.total).toBe(7);
    expect(pagination.pageNum).toBe(3);

    setPageNum(10);
    const overflowPage = applyLocalRecords(records);
    expect(overflowPage.map((item) => item.id)).toEqual([7]);
    expect(pagination.pageNum).toBe(3);
  });

  it('should reset page number when page size changes', () => {
    const { pagination, setPageNum, setPageSize } = useServerPagination();

    setPageNum(3);
    setPageSize(50);

    expect(pagination.pageNum).toBe(1);
    expect(pagination.pageSize).toBe(50);
  });

  it('should reset page number and total independently', () => {
    const { pagination, resetPage, resetTotal, setPageNum, applyPageResult } = useServerPagination();

    applyPageResult({
      total: 88,
      pageNum: 4,
      pageSize: 10,
      records: []
    });
    setPageNum(5);
    resetPage();
    resetTotal();

    expect(pagination.pageNum).toBe(1);
    expect(pagination.total).toBe(0);
  });
});
