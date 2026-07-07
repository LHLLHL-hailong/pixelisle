import { ref, watchEffect } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'dark' | 'light' | 'system'

const STORAGE_KEY = 'pi-theme-mode'

function getSystemIsDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function resolveTheme(mode: ThemeMode): 'dark' | 'light' {
  return mode === 'system' ? (getSystemIsDark() ? 'dark' : 'light') : mode
}

export const useThemeStore = defineStore('theme', () => {
  // 从 localStorage 读取用户偏好
  const saved = (localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'dark'
  const mode = ref<ThemeMode>(saved)
  const resolved = ref<'dark' | 'light'>(resolveTheme(saved))

  /** 应用主题到 DOM */
  function apply(resolvedTheme: 'dark' | 'light') {
    document.documentElement.setAttribute('data-theme', resolvedTheme)
  }

  /** 切换主题模式 */
  function setMode(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem(STORAGE_KEY, newMode)
    resolved.value = resolveTheme(newMode)
    apply(resolved.value)
  }

  /** 循环切换：dark → light → system → dark */
  function cycleMode() {
    const sequence: ThemeMode[] = ['dark', 'light', 'system']
    const idx = sequence.indexOf(mode.value)
    setMode(sequence[(idx + 1) % sequence.length])
  }

  // 初始化
  apply(resolved.value)

  // 监听系统主题变化（仅在 system 模式下生效）
  const systemMedia = window.matchMedia('(prefers-color-scheme: dark)')
  systemMedia.addEventListener('change', () => {
    if (mode.value === 'system') {
      resolved.value = getSystemIsDark() ? 'dark' : 'light'
      apply(resolved.value)
    }
  })

  return { mode, resolved, setMode, cycleMode }
})
