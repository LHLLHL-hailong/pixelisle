<template>
  <div id="app">
    <a-config-provider :locale="zhCN" :theme="antTheme">
      <BasicLayout />
    </a-config-provider>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { theme } from 'ant-design-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')

import BasicLayout from '@/layouts/BasicLayout.vue'
import { useThemeStore } from '@/stores/useThemeStore'

const themeStore = useThemeStore()

const antTheme = computed(() => ({
  algorithm: themeStore.resolved === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    // 主色覆盖，确保 Antd 组件与我们的色板一致
    colorPrimary: '#3B82F6',
    colorSuccess: '#10B981',
    colorWarning: '#F59E0B',
    colorError: '#EF4444',
    colorInfo: '#6366F1',
    // 圆角统一
    borderRadius: 8,
  },
}))
</script>

<style scoped></style>
