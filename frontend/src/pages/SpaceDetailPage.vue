<template>
  <div id="spaceDetailPage">
    <!-- 空间信息玻璃卡片 -->
    <div class="space-header">
      <div class="header-left">
        <h2 class="space-name">{{ space.spaceName }}</h2>
        <a-tag :color="space.spaceType === 1 ? 'blue' : 'default'">
          {{ SPACE_TYPE_MAP[space.spaceType] }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button v-if="canUploadPicture" type="primary" :href="`/add_picture?spaceId=${id}`" target="_blank">
          + 创建图片
        </a-button>
        <a-button v-if="canManageSpaceUser" type="default" :icon="h(TeamOutlined)" :href="`/spaceUserManage/${id}`" target="_blank">
          成员管理
        </a-button>
        <a-button v-if="canAccessSpaceAnalyze" type="default" :icon="h(BarChartOutlined)" :href="`/space_analyze?spaceId=${id}`" target="_blank">
          空间分析
        </a-button>
        <a-button v-if="canEditPicture" type="default" :icon="h(EditOutlined)" @click="doBatchEdit">批量编辑</a-button>
        <a-tooltip :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`">
          <a-progress
            type="circle"
            :size="44"
            :percent="space.maxSize ? Number(((space.totalSize * 100) / space.maxSize).toFixed(1)) : 0"
            :stroke-color="{ '0%': '#3B82F6', '100%': '#F59E0B' }"
          />
        </a-tooltip>
      </div>
    </div>

    <!-- 搜索表单 -->
    <PictureSearchForm :onSearch="onSearch" />

    <div style="margin-bottom: 16px" />

    <!-- 按颜色搜索 -->
    <div class="color-search">
      <span class="color-label">按颜色搜索：</span>
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </div>

    <!-- 图片列表 -->
    <PictureList
      ref="pictureListRef"
      :dataList="list"
      :loading="loading && currentPage === 1"
      :loadingMore="loadingMore"
      :hasMore="hasMore"
      :showOp="true"
      :canEdit="canEditPicture"
      :canDelete="canDeletePicture"
      :onReload="reset"
    />

    <!-- 分页 -->
    <div class="pagination-wrap" />

    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="list"
      :onSuccess="onBatchEditPictureSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onMounted, ref, watch } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import {
  listPictureVoByPageWithCacheUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { BarChartOutlined, EditOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_MAP } from '../constants/space.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})
const loginUserStore = useLoginUserStore()

function createPermissionChecker(permission: string) {
  return computed(() => (space.value.permissionList ?? []).includes(permission))
}

const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
// 空间分析：与后端 checkSpaceAuth 对齐 — 仅空间创建者或系统管理员可用
const canAccessSpaceAnalyze = computed(() =>
  space.value.userId != null &&
  (space.value.userId === loginUserStore.loginUser?.id ||
   loginUserStore.loginUser?.userRole === 'admin')
)

const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

const currentSearchParams = ref<API.PictureQueryRequest>({
  sortField: 'createTime',
  sortOrder: 'descend',
})

const {
  list,
  loading,
  hasMore,
  reset,
  observeSentinel,
  currentPage,
  invalidate,
} = useInfiniteScroll<API.PictureVO>({
  pageSize: 12,
  async fetcher(page, pageSize) {
    const params = { spaceId: props.id, ...currentSearchParams.value, current: page, pageSize }
    const res = await listPictureVoByPageWithCacheUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      return { records: res.data.data.records ?? [], total: res.data.data.total ?? 0 }
    }
    message.error('获取数据失败，' + res.data.message)
    return null
  },
})

const loadingMore = computed(() => loading.value && currentPage.value > 1)

const pictureListRef = ref()

function connectSentinel() {
  setTimeout(() => {
    const sentinel = pictureListRef.value?.gridRef?.sentinelRef
    if (sentinel) observeSentinel(sentinel)
  }, 200)
}

watch(hasMore, (val) => {
  if (val) connectSentinel()
})

onMounted(async () => {
  fetchSpaceDetail()
  reset()
  await nextTick()
  connectSentinel()
})

const colorSearchActive = ref(false)
let colorRequestId = 0

const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  colorRequestId++
  colorSearchActive.value = false
  Object.assign(currentSearchParams.value, newSearchParams)
  reset()
}

const onColorChange = async (color: string) => {
  invalidate()
  hasMore.value = false
  colorSearchActive.value = true
  loading.value = true
  const reqId = ++colorRequestId
  try {
    const res = await searchPictureByColorUsingPost({ picColor: color, spaceId: props.id })
    if (reqId !== colorRequestId) return
    if (res.data.code === 0 && res.data.data) {
      list.value = res.data.data ?? []
      hasMore.value = false
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (e: any) {
    if (reqId !== colorRequestId) return
    message.error('颜色搜索失败：' + (e?.message || '网络异常'))
  } finally {
    if (reqId === colorRequestId) {
      loading.value = false
    }
  }
}

const batchEditPictureModalRef = ref()
const onBatchEditPictureSuccess = () => { colorRequestId++; reset() }
const doBatchEdit = () => { batchEditPictureModalRef.value?.openModal() }

watch(() => props.id, () => {
  colorRequestId++
  fetchSpaceDetail()
  reset()
})
</script>

<style scoped>
#spaceDetailPage {
  max-width: var(--yu-content-max-width);
  margin: 0 auto;
}

.space-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  margin-bottom: 20px;
  background: var(--yu-glass-bg);
  backdrop-filter: blur(var(--yu-glass-blur));
  -webkit-backdrop-filter: blur(var(--yu-glass-blur));
  border: 1px solid var(--yu-glass-border);
  border-radius: var(--yu-radius-lg);
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.space-name {
  margin: 0;
  color: var(--yu-text-primary);
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.header-right :deep(.ant-btn) {
  white-space: nowrap;
}

.color-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.color-label {
  color: var(--yu-text-secondary);
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .space-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-right {
    width: 100%;
  }
}
</style>
