import { config } from '@vue/test-utils';

config.global.directives = {
      ...(config.global.directives ?? {}),
      permission: () => {}
};

// 全局测试配置
beforeEach(() => {
      // 重置测试环境
});

// 清理测试环境
afterEach(() => {
      // 清理测试资源
});
