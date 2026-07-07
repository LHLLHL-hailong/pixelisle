<template>
  <div id="spaceManagePage">
    <a-flex justify="space-between">
      <h2>空间成员管理</h2>
      <a-space>
        <a-button type="primary" href="/add_space" target="_blank">+ 创建空间</a-button>
        <a-button type="primary" ghost href="/space_analyze?queryPublic=1" target="_blank"
          >分析公共图库
        </a-button>
        <a-button type="primary" ghost href="/space_analyze?queryAll=1" target="_blank"
          >分析全部空间
        </a-button>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 添加成员表单 -->
    <a-form layout="inline" :model="formData" @finish="handleSubmit">
      <a-form-item label="用户" name="userId">
        <a-select
          v-model:value="formData.userId"
          show-search
          :filter-option="false"
          :loading="userSearchLoading"
          :options="userOptions"
          placeholder="输入用户名搜索"
          style="width: 240px"
          @search="handleUserSearch"
        />
      </a-form-item>
      <a-form-item label="角色" name="spaceRole">
        <a-select
          v-model:value="formData.spaceRole"
          :options="SPACE_ROLE_OPTIONS"
          placeholder="请选择角色"
          style="width: 120px"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">邀请加入</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />
    <!-- 表格 -->
    <a-table :columns="columns" :data-source="dataList">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userInfo'">
          <a-space>
            <a-avatar :src="record.user?.userAvatar" />
            {{ record.user?.userName }}
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'spaceRole'">
          <a-select
            v-model:value="record.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            @change="(value) => editSpaceRole(value, record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SPACE_ROLE_OPTIONS } from '../../constants/space.ts'
import {
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserUsingPost,
} from '@/api/spaceUserController.ts'
import { sendInvitationUsingPost } from '@/api/notificationController'
import { listUserVoByPageUsingPost } from '@/api/userController.ts'
import dayjs from 'dayjs'

interface Props {
  id: string
}

const props = defineProps<Props>()

const columns = [
  {
    title: '用户',
    dataIndex: 'userInfo',
  },
  {
    title: '角色',
    dataIndex: 'spaceRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.SpaceUserVO[]>([])

// 获取数据
const fetchData = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await listSpaceUserUsingPost({
    spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 添加成员表单
const formData = reactive<API.SpaceUserAddRequest>({ spaceRole: 'viewer' })

// 用户搜索
const userSearchLoading = ref(false)
const userOptions = ref<{ label: string; value: string }[]>([])
let userSearchTimer: ReturnType<typeof setTimeout> | null = null

const handleUserSearch = (keyword: string) => {
  if (userSearchTimer) clearTimeout(userSearchTimer)
  if (!keyword || keyword.trim().length === 0) {
    userOptions.value = []
    return
  }
  userSearchTimer = setTimeout(async () => {
    userSearchLoading.value = true
    try {
      const res = await listUserVoByPageUsingPost({
        userAccount: keyword.trim(),
        current: 1,
        pageSize: 20,
      })
      if (res.data.code === 0 && res.data.data?.records) {
        userOptions.value = res.data.data.records.map((user) => {
          const label =
            user.userName === user.userAccount
              ? user.userName
              : `${user.userName} (${user.userAccount})`
          return { label, value: String(user.id) }
        })
      }
    } finally {
      userSearchLoading.value = false
    }
  }, 300)
}

// 发起邀请
const handleSubmit = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  if (!formData.userId) {
    message.warning('请先搜索并选择一个用户')
    return
  }
  const res = await sendInvitationUsingPost({
    spaceId,
    receiverId: formData.userId,
    invitedRole: formData.spaceRole,
  })
  if (res.data.code === 0) {
    message.success('邀请已发送')
    formData.userId = undefined
  } else {
    message.error('邀请失败，' + res.data.message)
  }
}

// 编辑成员角色
const editSpaceRole = async (value, record) => {
  const res = await editSpaceUserUsingPost({
    id: record.id,
    spaceRole: value,
  })
  if (res.data.code === 0) {
    message.success('修改成功')
  } else {
    message.error('修改失败，' + res.data.message)
  }
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteSpaceUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}
</script>
<style scoped>
h2 { color: var(--yu-text-primary); }
</style>
