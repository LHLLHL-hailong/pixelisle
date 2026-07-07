<template>
  <div class="picture-list">
    <JustifiedGrid
      ref="gridRef"
      :items="dataList"
      :loading="loading"
      :hasMore="hasMore"
      :loadingMore="loadingMore"
      @click="doClickPicture"
    >
      <template v-if="showOp" #actions="{ item }">
        <a-tooltip title="分享">
          <a-button
            size="small"
            type="text"
            class="overlay-btn"
            @click.stop="doShare(item)"
          >
            <template #icon><ShareAltOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="以图搜图">
          <a-button
            size="small"
            type="text"
            class="overlay-btn"
            @click.stop="doSearch(item)"
          >
            <template #icon><SearchOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="canEdit" title="编辑">
          <a-button
            size="small"
            type="text"
            class="overlay-btn"
            @click.stop="doEdit(item)"
          >
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="canDelete" title="删除">
          <a-button
            size="small"
            type="text"
            class="overlay-btn danger-btn"
            @click.stop="doDelete(item)"
          >
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </a-tooltip>
      </template>
    </JustifiedGrid>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import JustifiedGrid from '@/components/JustifiedGrid.vue'
import ShareModal from '@/components/ShareModal.vue'

const gridRef = ref<InstanceType<typeof JustifiedGrid> | null>(null)
defineExpose({ gridRef })

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
  hasMore?: boolean
  loadingMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
  hasMore: false,
  loadingMore: false,
})

const router = useRouter()

const doClickPicture = (picture: API.PictureVO) => {
  router.push({ path: `/picture/${picture.id}` })
}

const doSearch = (picture: API.PictureVO) => {
  window.open(`/search_picture?pictureId=${picture.id}`)
}

const doEdit = (picture: API.PictureVO) => {
  router.push({
    path: '/add_picture',
    query: { id: picture.id, spaceId: picture.spaceId },
  })
}

const doDelete = async (picture: API.PictureVO) => {
  const id = picture.id
  if (!id) return
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    props.onReload?.()
  } else {
    message.error('删除失败')
  }
}

// 分享
const shareModalRef = ref()
const shareLink = ref<string>()
const doShare = (picture: API.PictureVO) => {
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  shareModalRef.value?.openModal()
}
</script>

<style scoped>
.picture-list {
  width: 100%;
}

.overlay-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 15px;
  transition: all var(--yu-transition-fast);
  border-radius: var(--yu-radius-sm);
}

.overlay-btn:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

.danger-btn:hover {
  color: var(--yu-error) !important;
  background: rgba(239, 68, 68, 0.2) !important;
}
</style>
