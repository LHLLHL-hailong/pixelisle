<template>
  <span ref="bellRef" class="notification-bell" @click.stop="togglePanel">
    <a-badge :count="unreadCount" :overflow-count="99" :show-zero="false">
      <BellOutlined :style="{ fontSize: '18px', cursor: 'pointer' }" />
    </a-badge>
    <NotificationPanel
      v-if="panelVisible"
      :unreadCount="unreadCount"
      @close="panelVisible = false"
      @accepted="$emit('accepted')"
    />
  </span>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { getUnreadCountUsingGet } from '@/api/notificationController'
import NotificationPanel from './NotificationPanel.vue'
import { useRouter } from 'vue-router'

const unreadCount = ref(0)
const panelVisible = ref(false)
const bellRef = ref<HTMLElement | null>(null)
const router = useRouter()

let pollTimer: number | null = null
let titleTimer: number | null = null
const originalTitle = document.title

const emit = defineEmits(['accepted'])

/** 点击外部关闭面板 */
const handleClickOutside = (e: MouseEvent) => {
  if (panelVisible.value && bellRef.value && !bellRef.value.contains(e.target as Node)) {
    panelVisible.value = false
    fetchUnreadCount()
    stopTitleBlink()
  }
}

/** 拉取未读数量 */
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCountUsingGet()
    if (res.data.code === 0 && res.data.data) {
      unreadCount.value = res.data.data.total || 0
    }
  } catch {
    // 静默失败
  }
}

/** 切换面板 */
const togglePanel = () => {
  panelVisible.value = !panelVisible.value
  if (!panelVisible.value && unreadCount.value > 0) {
    // 关闭面板后静默刷新
    fetchUnreadCount()
    stopTitleBlink()
  }
}

/** 页面标题闪烁 */
const startTitleBlink = () => {
  if (titleTimer) return
  titleTimer = window.setInterval(() => {
    document.title = document.title.startsWith('●')
      ? originalTitle
      : `● (${unreadCount.value}) ${originalTitle}`
  }, 1500)
}

const stopTitleBlink = () => {
  if (titleTimer) {
    clearInterval(titleTimer)
    titleTimer = null
    document.title = originalTitle
  }
}

/** 页面可见性变化 */
const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    fetchUnreadCount()
    stopTitleBlink()
  }
}

onMounted(() => {
  fetchUnreadCount()
  pollTimer = window.setInterval(fetchUnreadCount, 30000)
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (titleTimer) clearInterval(titleTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('click', handleClickOutside)
})

// 暴露给父组件
defineExpose({ fetchUnreadCount })
</script>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  margin-right: 16px;
}
</style>
