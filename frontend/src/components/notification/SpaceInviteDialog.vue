<template>
  <a-modal
    :visible="visible"
    title="邀请成员"
    @cancel="$emit('close')"
    @ok="handleSend"
    :confirmLoading="sending"
    okText="发送邀请"
  >
    <a-form layout="vertical">
      <a-form-item label="搜索用户">
        <a-select
          v-model:value="selectedUserId"
          show-search
          placeholder="输入用户名搜索"
          :filter-option="false"
          :options="userOptions"
          @search="handleSearch"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="分配角色">
        <a-radio-group v-model:value="invitedRole">
          <a-radio-button value="viewer">浏览者</a-radio-button>
          <a-radio-button value="editor">编辑者</a-radio-button>
          <a-radio-button value="admin">管理员</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="附言（选填）">
        <a-textarea v-model:value="content" placeholder="欢迎加入团队！" :maxlength="200" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { sendInvitationUsingPost } from '@/api/notificationController'
import { listUserVoByPageUsingPost } from '@/api/userController'

const props = defineProps<{
  visible: boolean
  spaceId: number
}>()
const emit = defineEmits(['close', 'done'])

const selectedUserId = ref<number | null>(null)
const invitedRole = ref('editor')
const content = ref('')
const sending = ref(false)
const userOptions = ref<{ label: string; value: number }[]>([])

const handleSearch = async (keyword: string) => {
  if (!keyword || keyword.length < 1) {
    userOptions.value = []
    return
  }
  try {
    const res = await listUserVoByPageUsingPost({
      current: 1,
      pageSize: 10,
      userName: keyword,
    })
    if (res.data.code === 0 && res.data.data) {
      userOptions.value = (res.data.data.records || []).map((u: any) => ({
        label: `${u.userName} (${u.userAccount || ''})`,
        value: u.id,
      }))
    }
  } catch {
    // 静默
  }
}

const handleSend = async () => {
  if (!selectedUserId.value) {
    message.warning('请选择用户')
    return
  }
  sending.value = true
  try {
    const res = await sendInvitationUsingPost({
      spaceId: props.spaceId,
      receiverId: selectedUserId.value,
      invitedRole: invitedRole.value,
      content: content.value || undefined,
    })
    if (res.data.code === 0) {
      message.success('邀请已发送')
      emit('done')
      emit('close')
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch {
    message.error('发送失败')
  } finally {
    sending.value = false
  }
}
</script>
