<template>
  <div id="notificationListPage">
    <h2 style="margin-bottom: 16px">消息中心</h2>
    <a-tabs v-model:activeKey="activeTab" @change="fetchList">
      <a-tab-pane key="" tab="全部" />
      <a-tab-pane key="INVITATION" tab="团队邀请" />
      <a-tab-pane key="CONTACT_ADMIN" tab="留言" />
    </a-tabs>
    <a-spin :spinning="loading">
      <div v-if="list.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: #999;">
        暂无消息
      </div>
      <a-list
        v-else
        :data-source="list"
        :pagination="{ current, pageSize: size, total, onChange: onPageChange }"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <TeamOutlined v-if="item.type === 'INVITATION'" style="font-size: 24px; color: #1677ff" />
                <MessageOutlined v-else style="font-size: 24px; color: #fa8c16" />
              </template>
              <template #title>
                <span :style="{ fontWeight: item.isRead === 0 ? 'bold' : 'normal' }">
                  {{ item.type === 'INVITATION' ? '团队邀请' : '联系管理员' }}
                  <a-tag v-if="item.type === 'INVITATION'" :color="statusColor(item.status)" style="margin-left: 8px">
                    {{ statusText(item.status) }}
                  </a-tag>
                </span>
              </template>
              <template #description>
                <div>{{ item.content || '无附加信息' }}</div>
                <div style="margin-top: 4px; color: #999; font-size: 12px">
                  {{ formatTime(item.createTime) }}
                </div>
                <div v-if="item.type === 'INVITATION' && item.status === 'PENDING'" style="margin-top: 8px">
                  <a-button size="small" type="primary" @click="handleAccept(item)">接受</a-button>
                  <a-button size="small" style="margin-left: 8px" @click="handleReject(item)">拒绝</a-button>
                </div>
                <!-- 留言类型：回复 -->
                <div v-if="item.type === 'CONTACT_ADMIN'" style="margin-top: 8px">
                  <a-button v-if="replyTargetId !== item.id" size="small" @click="replyTargetId = item.id; replyText = ''">
                    回复
                  </a-button>
                  <div v-else style="margin-top: 6px;">
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
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-spin>
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

const loading = ref(false)
const list = ref<API.NotificationVO[]>([])
const activeTab = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const replyTargetId = ref<number | null>(null)
const replyText = ref('')
const replying = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listNotificationsUsingPost({
      current: current.value,
      pageSize: size.value,
      type: activeTab.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      list.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number) => {
  current.value = page
  fetchList()
}

const statusColor = (s?: string) => {
  if (s === 'PENDING') return 'orange'
  if (s === 'ACCEPTED') return 'green'
  return 'red'
}

const statusText = (s?: string) => {
  const m: Record<string, string> = {
    PENDING: '待处理',
    ACCEPTED: '已接受',
    REJECTED: '已拒绝',
    EXPIRED: '已过期',
    UNREAD: '未读',
    READ: '已读',
  }
  return m[s || ''] || s || ''
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

const handleAccept = async (item: API.NotificationVO) => {
  try {
    const res = await acceptInvitationUsingPost({ notificationId: item.id })
    if (res.data.code === 0) {
      message.success('已加入空间')
      item.status = 'ACCEPTED'
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
#notificationListPage {
  max-width: 800px;
  margin: 0 auto;
}

#notificationListPage h2 {
  color: var(--yu-text-primary);
}
</style>
