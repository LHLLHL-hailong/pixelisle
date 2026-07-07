<template>
  <div id="vipExchangePage">
    <h2 style="margin-bottom: 16px">会员码兑换</h2>
    <!-- 兑换码表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="vipCode" label="兑换码">
        <a-input
          v-model:value="formData.vipCode"
          placeholder="请输入会员兑换码"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          兑换
        </a-button>
      </a-form-item>
    </a-form>
    <div style="text-align: center; margin-top: 16px;">
      <a-button type="link" @click="showContactAdmin = true">
        还没有兑换码？联系管理员
      </a-button>
    </div>
    <ContactAdminDialog
      :visible="showContactAdmin"
      @close="showContactAdmin = false"
      @done="showContactAdmin = false"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { exchangeVipUsingPost } from '@/api/userController.ts'
import ContactAdminDialog from '@/components/notification/ContactAdminDialog.vue'
import { useRouter } from 'vue-router'

const showContactAdmin = ref(false)

// 表单数据
const formData = reactive<API.VipExchangeRequest>({
  vipCode: '',
})

// 提交任务状态
const loading = ref(false)

const router = useRouter()

/**
 * 提交表单
 */
const handleSubmit = async () => {
  // 校验兑换码是否为空
  if (!formData.vipCode) {
    message.error('请输入兑换码')
    return
  }

  loading.value = true

  try {
    // 调用兑换 API
    const res = await exchangeVipUsingPost({
      vipCode: formData.vipCode,
    })

    // 操作成功
    if (res.data.code === 0 && res.data.data) {
      message.success('兑换成功！')
      // 跳转到主页或其他页面
      router.push({
        path: `/`,
      })
    } else {
      message.error('兑换失败：' + res.data.message)
    }
  } catch (error) {
    message.error('兑换失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#vipExchangePage {
  max-width: 720px;
  margin: 0 auto;
}

#vipExchangePage h2 {
  color: var(--yu-text-primary);
}

#vipExchangePage :deep(.ant-form-item-label > label) {
  color: var(--yu-text-secondary);
}
</style>
