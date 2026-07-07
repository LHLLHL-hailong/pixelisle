import { computed } from 'vue'
import { useThemeStore } from '@/stores/useThemeStore'

/**
 * 根据当前主题返回 ECharts 颜色配置
 */
export function useChartTheme() {
  const themeStore = useThemeStore()

  const isDark = computed(() => themeStore.resolved === 'dark')

  const textColor = computed(() => isDark.value ? '#98989D' : '#86868B')
  const axisColor = computed(() => isDark.value ? '#38383A' : '#E5E5EA')
  const splitColor = computed(() => isDark.value ? '#2E2E31' : '#F0F0F2')

  const colors = computed(() =>
    isDark.value
      ? ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#6366F1', '#8B5CF6', '#EC4899', '#06B6D4']
      : ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#6366F1', '#8B5CF6', '#EC4899', '#06B6D4']
  )

  /** 获取通用 ECharts 文本样式 */
  function textStyle() {
    return { color: textColor.value }
  }

  return { isDark, textColor, axisColor, splitColor, colors, textStyle }
}
