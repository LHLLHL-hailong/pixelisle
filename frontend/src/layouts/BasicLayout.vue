<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout>
        <GlobalSider class="sider" />
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from "@/components/GlobalSider.vue";
</script>

<style scoped>
#basicLayout .header {
  padding-inline: 20px;
  background: var(--yu-glass-bg);
  backdrop-filter: blur(var(--yu-glass-blur));
  -webkit-backdrop-filter: blur(var(--yu-glass-blur));
  border-bottom: 1px solid var(--yu-glass-border);
  color: unset;
  height: var(--yu-header-height);
  line-height: var(--yu-header-height);
  position: sticky;
  top: 0;
  z-index: 100;
}

#basicLayout .sider {
  background: transparent;
  border-right: 1px solid var(--yu-border);
  padding-top: 20px;
}

#basicLayout :deep(.ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
}

#basicLayout .content {
  padding: 28px;
  background: var(--yu-bg);
  min-height: calc(100vh - var(--yu-header-height));
}

/* 响应式 */
@media (max-width: 768px) {
  #basicLayout .content {
    padding: 12px;
  }
}
</style>
