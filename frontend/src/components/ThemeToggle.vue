<template>
  <span class="theme-toggle" :title="tooltip" @click="themeStore.cycleMode()">
    <Moon v-if="themeStore.mode === 'dark'" :size="16" />
    <Sun v-else-if="themeStore.mode === 'light'" :size="16" />
    <Monitor v-else :size="16" />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/useThemeStore'
import { Moon, Sun, Monitor } from 'lucide-vue-next'

const themeStore = useThemeStore()

const labels: Record<string, string> = { dark: '暗色模式', light: '亮色模式', system: '跟随系统' }
const tooltip = computed(() => labels[themeStore.mode] || '')
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
  transition: all var(--yu-transition-fast);
  margin-right: 12px;
  user-select: none;
}

.theme-toggle:hover {
  background: var(--yu-bg-hover);
}
</style>
