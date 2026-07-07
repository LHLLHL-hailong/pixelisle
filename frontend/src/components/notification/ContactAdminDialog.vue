<template>
  <a-modal
    :visible="visible"
    title="联系管理员"
    @cancel="$emit('close')"
    @ok="handleSend"
    :confirmLoading="sending"
    okText="发送留言"
  >
    <a-form layout="vertical">
      <a-form-item label="申请说明">
        <a-textarea
          v-model:value="content"
          placeholder="请描述你的需求，管理员会尽快回复..."
          :maxlength="500"
          :rows="4"
          showCount
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { contactAdminUsingPost } from '@/api/notificationController'

const props = defineProps<{
  visible: boolean
  spaceId?: number
}>()
const emit = defineEmits(['close', 'done'])

const content = ref('')
const sending = ref(false)

const handleSend = async () => {
  if (!content.value.trim()) {
    message.warning('请输入内容')
    return
  }
  sending.value = true
  try {
    const res = await contactAdminUsingPost({
      spaceId: props.spaceId,
      content: content.value,
    })
    if (res.data.code === 0) {
      message.success('留言已发送，管理员会尽快回复')
      content.value = ''
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
