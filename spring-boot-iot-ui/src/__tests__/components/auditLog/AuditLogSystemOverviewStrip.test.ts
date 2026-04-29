import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuditLogSystemOverviewStrip from '@/components/auditLog/AuditLogSystemOverviewStrip.vue';

describe('AuditLogSystemOverviewStrip', () => {
  it('renders lightweight overview chips with a single active state and emits the requested tab key', async () => {
    const wrapper = mount(AuditLogSystemOverviewStrip, {
      props: {
        activeTab: 'hotspots',
        activeItemKey: 'hotspots',
        items: [
          { key: 'errors', label: '异常', value: '896019', targetTab: 'errors' },
          { key: 'hotspots', label: '慢点', value: '5', targetTab: 'hotspots' },
          { key: 'tasks', label: '调度', value: '9236', targetTab: 'hotspots' },
          { key: 'archives', label: '异常批次', value: '3', targetTab: 'archives' }
        ]
      }
    });

    expect(wrapper.get('[data-testid="system-log-overview-hotspots"]').classes()).toContain('is-active');
    expect(wrapper.get('[data-testid="system-log-overview-tasks"]').classes()).not.toContain('is-active');
    await wrapper.get('[data-testid="system-log-overview-hotspots"]').trigger('click');
    expect(wrapper.emitted('change-tab')?.[0]).toEqual(['hotspots']);
  });
});
