<template>
  <div class="notification-panel" @click.stop>
    <div class="panel-header">
      <span>消息中心</span>
      <a @click="$router.push('/notifications'); $emit('close')">查看全部 →</a>
    </div>
    <div class="panel-body">
      <a-spin :spinning="loading" />
      <div v-if="notifications.length === 0 && !loading" class="empty-tip">
        暂无消息
      </div>
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: item.isRead === 0 }"
      >
        <div class="item-icon">
          <TeamOutlined v-if="item.type === 'INVITATION'" />
          <MessageOutlined v-else />
        </div>
        <div class="item-content">
          <div class="item-text">{{ item.content || formatInvite(item) }}</div>
          <div class="item-time">{{ formatTime(item.createTime) }}</div>
          <!-- 邀请类型 -->
          <div v-if="item.type === 'INVITATION' && item.status === 'PENDING'" class="item-actions">
            <a-button size="small" type="primary" @click="handleAccept(item)">接受</a-button>
            <a-button size="small" @click="handleReject(item)">拒绝</a-button>
          </div>
          <div v-else-if="item.type === 'INVITATION'" class="item-status">
            <a-tag :color="item.status === 'ACCEPTED' ? 'green' : 'red'">
              {{ item.status === 'ACCEPTED' ? '已接受' : '已拒绝' }}
            </a-tag>
          </div>
          <!-- 留言类型：回复按钮 + 输入框 -->
          <div v-if="item.type === 'CONTACT_ADMIN'" class="item-actions">
            <a-button v-if="replyTargetId !== item.id" size="small" @click="replyTargetId = item.id; replyText = ''">
              回复
            </a-button>
            <div v-else class="reply-box">
              <a-textarea
                v-model:value="replyText"
                placeholder="输入回复..."
                :rows="2"
                size="small"
              />
              <div style="margin-top: 6px; display: flex; gap: 6px;">
                <a-button size="small" type="primary" :loading="replying" @click="handleReply(item)">发送</a-button>
                <a-button size="small" @click="replyTargetId = null">取消</a-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TeamOutlined, MessageOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  listNotificationsUsingPost,
  acceptInvitationUsingPost,
  rejectInvitationUsingPost,
  replyNotificationUsingPost,
} from '@/api/notificationController'

const props = defineProps<{ unreadCount: number }>()
const emit = defineEmits(['close', 'accepted'])

const notifications = ref<API.NotificationVO[]>([])
const loading = ref(false)
const replyTargetId = ref<number | null>(null)
const replyText = ref('')
const replying = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listNotificationsUsingPost({ current: 1, pageSize: 5 })
    if (res.data.code === 0 && res.data.data) {
      notifications.value = res.data.data.records || []
    }
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

const formatInvite = (item: API.NotificationVO) => {
  const roleMap: Record<string, string> = { viewer: '浏览者', editor: '编辑者', admin: '管理员' }
  const role = roleMap[item.invitedRole || ''] || item.invitedRole
  return `邀请你加入空间，角色：${role}`
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleDateString()
}

const handleAccept = async (item: API.NotificationVO) => {
  try {
    const res = await acceptInvitationUsingPost({ notificationId: item.id })
    if (res.data.code === 0) {
      message.success('已加入空间')
      item.status = 'ACCEPTED'
      emit('accepted')
      fetchList()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  }
}

const handleReject = async (item: API.NotificationVO) => {
  try {
    const res = await rejectInvitationUsingPost({ notificationId: item.id })
    if (res.data.code === 0) {
      message.success('已拒绝')
      item.status = 'REJECTED'
      fetchList()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  }
}

const handleReply = async (item: API.NotificationVO) => {
  if (!replyText.value.trim()) {
    message.warning('请输入内容')
    return
  }
  replying.value = true
  try {
    const res = await replyNotificationUsingPost({
      notificationId: item.id,
      content: replyText.value,
    })
    if (res.data.code === 0) {
      message.success('回复已发送')
      replyTargetId.value = null
      replyText.value = ''
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch {
    message.error('发送失败')
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.notification-panel {
  position: absolute;
  top: 40px;
  right: 0;
  width: 380px;
  max-width: calc(100vw - 24px);
  max-height: 480px;
  background: var(--yu-glass-bg);
  backdrop-filter: blur(var(--yu-glass-blur));
  -webkit-backdrop-filter: blur(var(--yu-glass-blur));
  border: 1px solid var(--yu-glass-border);
  border-radius: var(--yu-radius-lg);
  box-shadow: var(--yu-shadow-lg);
  z-index: 1050;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--yu-border);
  font-weight: 500;
  color: var(--yu-text-primary);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.empty-tip {
  text-align: center;
  color: var(--yu-text-tertiary);
  padding: 40px 0;
}

.notification-item {
  display: flex;
  padding: 10px 16px;
  gap: 10px;
  border-bottom: 1px solid var(--yu-border-light);
  transition: background 0.2s;
}

.notification-item:hover {
  background: var(--yu-bg-hover);
}

.notification-item.unread {
  background: var(--yu-primary-bg);
}

.item-icon {
  font-size: 20px;
  color: var(--yu-primary);
  margin-top: 2px;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-text {
  font-size: 13px;
  color: var(--yu-text-primary);
  word-break: break-all;
}

.item-time {
  font-size: 12px;
  color: var(--yu-text-tertiary);
  margin-top: 4px;
}

.item-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.item-status {
  margin-top: 4px;
}
</style>
