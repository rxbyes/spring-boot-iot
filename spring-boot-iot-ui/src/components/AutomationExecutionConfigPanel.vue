<template>
  <PanelCard title="执行配置" description="这里定义目标系统、账号、输出策略与阻断范围。">
    <el-form label-width="110px" class="automation-execution-config__form">
      <el-form-item label="计划名称">
        <el-input v-model="target.planName" placeholder="请输入计划名称" />
      </el-form-item>
      <el-form-item label="前端地址">
        <el-input v-model="target.frontendBaseUrl" placeholder="例如：http://127.0.0.1:5174" />
      </el-form-item>
      <el-form-item label="后端地址">
        <el-input v-model="target.backendBaseUrl" placeholder="例如：http://127.0.0.1:9999" />
      </el-form-item>
      <el-form-item label="登录路由">
        <el-input v-model="target.loginRoute" placeholder="/login" />
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="target.username" placeholder="admin" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="target.password" type="password" show-password placeholder="123456" />
      </el-form-item>
      <el-form-item label="浏览器路径">
        <el-input v-model="target.browserPath" placeholder="可留空，走自动识别" />
      </el-form-item>
      <el-form-item label="问题文档">
        <el-input v-model="target.issueDocPath" placeholder="docs/archive/22-automation-test-issues-20260316.md" />
      </el-form-item>
      <el-form-item label="输出前缀">
        <el-input v-model="target.outputPrefix" placeholder="config-browser" />
      </el-form-item>
      <el-form-item label="基线路径">
        <el-input v-model="target.baselineDir" placeholder="config/automation/baselines" />
      </el-form-item>
      <el-form-item label="执行模式">
        <el-switch
          v-model="target.headless"
          inline-prompt
          active-text="无头"
          inactive-text="有头"
        />
      </el-form-item>
    </el-form>

    <div class="automation-execution-config__scope-grid">
      <label class="automation-execution-config__scope-card">
        <span>场景范围</span>
        <el-select v-model="target.scenarioScopes" multiple collapse-tags placeholder="选择执行范围">
          <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
        </el-select>
      </label>

      <label class="automation-execution-config__scope-card">
        <span>阻断范围</span>
        <el-select v-model="target.failScopes" multiple collapse-tags placeholder="选择阻断范围">
          <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
        </el-select>
      </label>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import PanelCard from './PanelCard.vue';
import type { AutomationTargetConfig } from '../types/automation';

defineProps<{
  target: AutomationTargetConfig;
  scopeOptions: string[];
}>();
</script>

<style scoped>
.automation-execution-config__form {
  margin-bottom: 1rem;
}

.automation-execution-config__scope-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.automation-execution-config__scope-card {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.automation-execution-config__scope-card span {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

@media (max-width: 1024px) {
  .automation-execution-config__scope-grid {
    grid-template-columns: 1fr;
  }
}
</style>
