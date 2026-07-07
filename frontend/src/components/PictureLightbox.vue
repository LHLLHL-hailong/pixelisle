<template>
  <teleport to="body">
    <Transition name="lightbox-fade">
      <div
        v-if="visible"
        class="lightbox-overlay"
        @click.self="close"
        @keydown="onKeydown"
        tabindex="-1"
      >
        <!-- 顶部工具栏 -->
        <div class="lightbox-toolbar">
          <div class="toolbar-left">
            <span class="toolbar-title">{{ picture?.name ?? '图片详情' }}</span>
            <span v-if="pictureIndex != null && totalCount" class="toolbar-counter">
              {{ pictureIndex + 1 }} / {{ totalCount }}
            </span>
          </div>
          <div class="toolbar-right">
            <a-button type="text" class="toolbar-btn" @click="doDownload" title="下载">
              <template #icon><DownloadOutlined /></template>
            </a-button>
            <a-button type="text" class="toolbar-btn" @click="doShare" title="分享">
              <template #icon><ShareAltOutlined /></template>
            </a-button>
            <a-button v-if="canEdit" type="text" class="toolbar-btn" @click="doEdit" title="编辑">
              <template #icon><EditOutlined /></template>
            </a-button>
            <a-button v-if="canDelete" type="text" class="toolbar-btn danger-btn" @click="doDelete" title="删除">
              <template #icon><DeleteOutlined /></template>
            </a-button>
            <a-button type="text" class="toolbar-btn" @click="close" title="关闭 (ESC)">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
        </div>

        <!-- 主体区域 -->
        <div class="lightbox-body" :style="{ paddingRight: showInfoPanel ? '320px' : '0px' }">
          <!-- 左箭头 -->
          <div v-if="hasPrev" class="nav-arrow nav-prev" @click.stop="goPrev">
            <LeftOutlined />
          </div>

          <!-- 图片展示区 -->
          <div class="image-stage" :style="{ paddingRight: showInfoPanel ? '4px' : '20px' }" @click.stop>
            <img
              :src="picture?.url"
              :alt="picture?.name"
              class="stage-image"
            />
          </div>

          <!-- 右箭头 -->
          <div v-if="hasNext" class="nav-arrow nav-next" @click.stop="goNext">
            <RightOutlined />
          </div>

          <!-- 右侧信息面板（可收起） -->
          <Transition name="panel-slide">
            <div v-if="showInfoPanel" class="info-panel">
              <div class="panel-inner">
                <div class="panel-section">
                  <div class="panel-author">
                    <a-avatar :src="picture?.user?.userAvatar" :size="36" />
                    <div>
                      <div class="author-name">{{ picture?.user?.userName }}</div>
                      <div class="author-desc">{{ picture?.user?.userProfile ?? '暂无简介' }}</div>
                    </div>
                  </div>
                </div>
                <div class="panel-divider" />
                <div class="panel-section">
                  <div class="info-row">
                    <span class="info-label">名称</span>
                    <span class="info-value">{{ picture?.name ?? '未命名' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">分类</span>
                    <a-tag color="blue">{{ picture?.category ?? '默认' }}</a-tag>
                  </div>
                  <div class="info-row" v-if="picture?.tags?.length">
                    <span class="info-label">标签</span>
                    <span>
                      <a-tag v-for="tag in picture.tags" :key="tag" class="info-tag">{{ tag }}</a-tag>
                    </span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">简介</span>
                    <span class="info-value">{{ picture?.introduction ?? '-' }}</span>
                  </div>
                </div>
                <div class="panel-divider" />
                <div class="panel-section">
                  <div class="info-row">
                    <span class="info-label">格式</span>
                    <span class="info-value">{{ picture?.picFormat?.toUpperCase() ?? '-' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">尺寸</span>
                    <span class="info-value">{{ picture?.picWidth }} × {{ picture?.picHeight }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">大小</span>
                    <span class="info-value">{{ picture?.picSize ? formatSize(picture.picSize) : '-' }}</span>
                  </div>
                  <div v-if="picture?.picColor" class="info-row">
                    <span class="info-label">主色调</span>
                    <span class="info-color-chip" :style="{ background: toHexColor(picture.picColor) }" />
                  </div>
                </div>
              </div>
            </div>
          </Transition>

          <!-- 信息面板切换按钮 -->
          <div
            class="panel-toggle"
            :style="{ right: showInfoPanel ? '320px' : '4px' }"
            @click.stop="showInfoPanel = !showInfoPanel"
          >
            <InfoCircleOutlined />
          </div>
        </div>

        <!-- 底部缩略图导航条 -->
        <div v-if="flatPictureList.length > 1" class="thumbnail-strip">
          <div
            v-for="(pic, i) in flatPictureList"
            :key="pic.id"
            class="thumb-item"
            :class="{ active: i === pictureIndex }"
            @click="goTo(i)"
          >
            <img :src="pic.thumbnailUrl ?? pic.url" :alt="pic.name" />
          </div>
        </div>
      </div>
    </Transition>
  </teleport>

  <!-- 分享弹窗 -->
  <ShareModal ref="shareModalRef" :link="shareLink" :z-index="2100" />
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  CloseOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  InfoCircleOutlined,
  LeftOutlined,
  RightOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { deletePictureUsingPost } from '@/api/pictureController'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import ShareModal from '@/components/ShareModal.vue'
import { SPACE_PERMISSION_ENUM } from '@/constants/space'

const props = defineProps<{
  visible: boolean
  picture: API.PictureVO | null
  pictureList?: API.PictureVO[]
  pictureIndex?: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'goto', index: number): void
}>()

const showInfoPanel = ref(true)
const shareModalRef = ref()
const shareLink = ref<string>()

// 扁平化的图片列表用于缩略图导航
const flatPictureList = computed(() => props.pictureList ?? [])

const totalCount = computed(() => props.pictureList?.length ?? 0)
const hasPrev = computed(() => props.pictureIndex != null && props.pictureIndex > 0)
const hasNext = computed(() => {
  if (props.pictureIndex == null || !props.pictureList) return false
  return props.pictureIndex < props.pictureList.length - 1
})

const picture = computed(() => props.picture)

// 权限
const canEdit = computed(() =>
  (picture.value?.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
)
const canDelete = computed(() =>
  (picture.value?.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
)

function close() {
  emit('close')
}

function goPrev() {
  if (hasPrev.value) emit('prev')
}

function goNext() {
  if (hasNext.value) emit('next')
}

function goTo(index: number) {
  emit('goto', index)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
  if (e.key === 'ArrowLeft') goPrev()
  if (e.key === 'ArrowRight') goNext()
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})

// 编辑
const router = useRouter()
const doEdit = () => {
  if (!picture.value) return
  router.push({
    path: '/add_picture',
    query: { id: picture.value.id, spaceId: picture.value.spaceId },
  })
}

// 删除
const doDelete = async () => {
  const id = picture.value?.id
  if (!id) return
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    close()
  } else {
    message.error('删除失败')
  }
}

// 下载
const doDownload = () => {
  if (picture.value?.url) downloadImage(picture.value.url)
}

// 分享
const doShare = () => {
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.value?.id}`
  shareModalRef.value?.openModal()
}
</script>

<style scoped>
/* 覆盖层 */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.94);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  outline: none;
}

/* 过渡动画 */
.lightbox-fade-enter-active,
.lightbox-fade-leave-active {
  transition: opacity 0.3s ease;
}
.lightbox-fade-enter-from,
.lightbox-fade-leave-to {
  opacity: 0;
}

/* 顶部工具栏 */
.lightbox-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  height: 52px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}

.toolbar-counter {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.toolbar-right {
  display: flex;
  gap: 4px;
}

.toolbar-btn {
  color: rgba(255, 255, 255, 0.85) !important;
  font-size: 16px;
  border-radius: var(--yu-radius-sm);
}

.toolbar-btn:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.1) !important;
}

.danger-btn:hover {
  color: var(--yu-error) !important;
}

/* 主体 */
.lightbox-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: padding-right 0.3s ease;
  min-height: 0;
  overflow: hidden;
}

/* 图片舞台 */
.image-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  max-height: 100%;
  max-width: 100%;
  padding: 20px;
}

.stage-image {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 4px;
  user-select: none;
  -webkit-user-drag: none;
}

/* 左右导航箭头 */
.nav-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: all var(--yu-transition-fast);
  z-index: 10;
}

.nav-arrow:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-50%) scale(1.1);
}

.nav-prev { left: 16px; }
.nav-next { right: 16px; }

/* 右侧信息面板 */
.info-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 320px;
  background: rgba(30, 30, 32, 0.92);
  backdrop-filter: blur(16px);
  border-left: 1px solid rgba(255, 255, 255, 0.08);
  overflow-y: auto;
  z-index: 5;
}

.panel-inner {
  padding: 20px;
}

.panel-section {
  margin-bottom: 4px;
}

.panel-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 16px 0;
}

.panel-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}

.author-desc {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.info-label {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  min-width: 48px;
  flex-shrink: 0;
}

.info-value {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  word-break: break-all;
}

.info-tag {
  margin-right: 4px;
}

.info-color-chip {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.15);
}

/* 面板切换按钮 */
.panel-toggle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 56px;
  background: rgba(30, 30, 32, 0.92);
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  z-index: 6;
}

.panel-toggle:hover {
  color: #fff;
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* 底部缩略图导航条 */
.thumbnail-strip {
  display: flex;
  gap: 4px;
  padding: 10px 16px;
  overflow-x: auto;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
}

.thumb-item {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0.5;
  transition: all var(--yu-transition-fast);
  border: 2px solid transparent;
}

.thumb-item:hover {
  opacity: 0.8;
}

.thumb-item.active {
  opacity: 1;
  border-color: var(--yu-primary);
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 768px) {
  .info-panel {
    width: 100%;
  }
  .nav-arrow {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
  .image-stage {
    max-width: 100%;
    padding: 8px;
  }
}
</style>
