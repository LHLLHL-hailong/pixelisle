<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改' : '创建' }} {{ SPACE_TYPE_MAP[spaceType] }}
    </h2>
    <!-- 空间信息表单 -->
    <a-form name="spaceForm" layout="vertical" :model="spaceForm" @finish="handleSubmit">
      <a-form-item name="spaceName" label="空间名称">
        <a-input v-model:value="spaceForm.spaceName" placeholder="请输入空间" allow-clear />
      </a-form-item>
      <a-form-item name="spaceLevel" label="空间级别">
        <a-select
          v-model:value="spaceForm.spaceLevel"
          style="min-width: 180px"
          placeholder="请选择空间级别"
          :options="spaceLevelOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading" style="width: 100%">
          提交
        </a-button>
      </a-form-item>
    </a-form>
    <!-- 空间级别对比卡片 -->
    <a-row :gutter="16" style="margin-top: 24px">
      <a-col :span="8" v-for="level in spaceLevelList" :key="level.value">
        <a-card
          :bordered="true"
          :style="{
            textAlign: 'center',
            borderColor: getLevelAccessInfo(level.value, userRole).available ? '#1677ff' : '#d9d9d9',
          }"
        >
          <template #title>
            <span style="font-size: 16px">{{ level.text }}</span>
          </template>
          <p style="font-size: 24px; font-weight: bold; margin: 8px 0">
            {{ level.maxCount }} 张
          </p>
          <p style="color: #888; margin-bottom: 12px">
            {{ formatSize(level.maxSize) }}
          </p>
          <a-tag
            v-if="!getLevelAccessInfo(level.value, userRole).available"
            color="orange"
            style="margin-bottom: 8px"
          >
            {{ getLevelAccessInfo(level.value, userRole).reason }}
          </a-tag>
          <br />
          <a-button
            v-if="getLevelAccessInfo(level.value, userRole).available"
            type="primary"
            disabled
          >
            当前可用
          </a-button>
          <a-button
            v-else-if="level.value === SPACE_LEVEL_ENUM.PROFESSIONAL && userRole === 'user'"
            type="primary"
            @click="$router.push('/user_exchange_vip')"
          >
            升级VIP →
          </a-button>
          <a-button
            v-else-if="level.value === SPACE_LEVEL_ENUM.FLAGSHIP"
            type="default"
            @click="showContactAdmin = true"
          >
            联系管理员
          </a-button>
          <a-button v-else disabled>
            {{ getLevelActionLabel(level.value, userRole) }}
          </a-button>
        </a-card>
      </a-col>
    </a-row>
    <!-- 联系管理员弹窗 -->
    <ContactAdminDialog
      :visible="showContactAdmin"
      @close="showContactAdmin = false"
      @done="showContactAdmin = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  listSpaceLevelUsingGet,
  updateSpaceUsingPost,
} from '@/api/spaceController.ts'
import { useRoute, useRouter } from 'vue-router'
import {
  SPACE_LEVEL_ENUM,
  SPACE_LEVEL_MAP,
  SPACE_TYPE_ENUM,
  SPACE_TYPE_MAP,
  getSpaceLevelOptions,
  getLevelActionLabel,
  getLevelAccessInfo,
} from '@/constants/space.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import ContactAdminDialog from '@/components/notification/ContactAdminDialog.vue'
import { formatSize } from '../utils'

const loginUserStore = useLoginUserStore()
const userRole = computed(() => loginUserStore.loginUser.userRole)

// 按用户角色过滤级别选项（锁定项 disabled）
const spaceLevelOptions = computed(() => getSpaceLevelOptions(userRole.value))

const space = ref<API.SpaceVO>()
const spaceForm = reactive<API.SpaceAddRequest | API.SpaceEditRequest>({})
const loading = ref(false)

const route = useRoute()
// 空间类别，默认为私有空间
const spaceType = computed(() => {
  if (route.query?.type) {
    return Number(route.query.type)
  } else {
    return SPACE_TYPE_ENUM.PRIVATE
  }
})

const spaceLevelList = ref<API.SpaceLevel[]>([])
const showContactAdmin = ref(false)

// 获取空间级别
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('获取空间级别失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchSpaceLevelList()
})

const router = useRouter()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const spaceId = space.value?.id
  loading.value = true
  let res
  if (spaceId) {
    // 更新
    res = await updateSpaceUsingPost({
      id: spaceId,
      ...spaceForm,
    })
  } else {
    // 创建
    res = await addSpaceUsingPost({
      ...spaceForm,
      spaceType: spaceType.value,
    })
  }
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    if (spaceId) {
      // 更新 → 回到空间管理页
      router.push('/admin/spaceManage')
    } else {
      // 创建 → 跳转到新空间详情页
      router.push({
        path: `/space/${res.data.data}`,
      })
    }
  } else {
    message.error('操作失败，' + res.data.message)
  }
  loading.value = false
}

// 获取老数据
const getOldSpace = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
      // 填充表单
      spaceForm.spaceName = data.spaceName
      spaceForm.spaceLevel = data.spaceLevel
    }
  }
}

onMounted(() => {
  getOldSpace()
})
</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}

#addSpacePage h2 {
  color: var(--yu-text-primary);
}

#addSpacePage :deep(.ant-form-item-label > label) {
  color: var(--yu-text-secondary);
}

#addSpacePage :deep(.ant-card) {
  background: var(--yu-bg-card);
  border-color: var(--yu-border);
}

#addSpacePage :deep(.ant-card-head-title) {
  color: var(--yu-text-primary);
}

#addSpacePage :deep(.ant-card-body p) {
  color: var(--yu-text-primary) !important;
}

@media (max-width: 768px) {
  #addSpacePage { padding: 0 8px; }
}
</style>
