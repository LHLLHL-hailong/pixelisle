<template>
  <a-modal
    class="image-cropper"
    v-model:visible="visible"
    :footer="false"
    :width="cropperWidth"
    :bodyStyle="{ padding: 0 }"
    :destroyOnClose="true"
    @cancel="closeModal"
  >
    <div class="editor-layout">
      <!-- ===== Top Bar ===== -->
      <div class="editor-topbar">
        <div class="topbar-left">
          <span class="editor-title">Edit Image</span>
          <template v-if="isTeamSpace">
            <a-tag v-if="editingUser" color="blue" size="small">{{ editingUser.userName }} editing</a-tag>
            <a-button v-if="canEnterEdit" type="primary" size="small" @click="enterEdit">Enter Edit</a-button>
            <a-button v-if="canExitEdit" size="small" @click="exitEdit">Exit Edit</a-button>
          </template>
        </div>
        <div class="topbar-right">
          <a-button size="small" :disabled="!canEdit" @click="rotateLeft" title="Rotate Left">
            <template #icon><span style="font-size:16px">&#x21B6;</span></template>
          </a-button>
          <a-button size="small" :disabled="!canEdit" @click="rotateRight" title="Rotate Right">
            <template #icon><span style="font-size:16px">&#x21B7;</span></template>
          </a-button>
          <a-divider type="vertical" />
          <a-button size="small" :disabled="!canEdit" @click="changeScale(-1)" title="Zoom Out">
            <template #icon><span style="font-size:16px">&#x2212;</span></template>
          </a-button>
          <span class="zoom-label">100%</span>
          <a-button size="small" :disabled="!canEdit" @click="changeScale(1)" title="Zoom In">
            <template #icon><span style="font-size:16px">+</span></template>
          </a-button>
          <a-divider type="vertical" />
          <a-button size="small" :type="cropLocked ? 'default' : 'primary'" @click="toggleCropLock()" :disabled="!canEdit" :title="cropLocked ? 'Unlock Crop' : 'Lock Crop'">
            <template #icon>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            </template>
            {{ cropLocked ? 'Locked' : 'Unlocked' }}
          </a-button>
          <a-divider type="vertical" />
          <a-button size="small" danger @click="clearAnnotationsBtn" :disabled="!canEdit">Clear</a-button>
          <a-button type="primary" size="small" :loading="loading" :disabled="!canEdit" @click="handleConfirm">Save</a-button>
        </div>
      </div>

      <div class="editor-body">
        <!-- ===== Left Toolbar ===== -->
        <div class="editor-left">
          <div
            class="tool-btn"
            :class="{ active: activeTool === 'brush', disabled: !canEdit || !cropLocked }"
            @click="canEdit && cropLocked && switchTool('brush')"
            :title="!cropLocked ? 'Lock crop first' : 'Brush'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="M2 2l7.586 7.586"/><circle cx="11" cy="11" r="2"/></svg>
            <span>Brush</span>
          </div>
          <div
            class="tool-btn"
            :class="{ active: activeTool === 'eraser', disabled: !canEdit || !cropLocked }"
            @click="canEdit && cropLocked && switchTool('eraser')"
            :title="!cropLocked ? 'Lock crop first' : 'Eraser'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 20H7L3 16c-.8-.8-.8-2 0-2.8L14 2.2c.8-.8 2-.8 2.8 0L20 5.2c.8.8.8 2 0 2.8L12 16"/><line x1="6" y1="20" x2="10" y2="20"/></svg>
            <span>Eraser</span>
          </div>
          <div
            class="tool-btn"
            :class="{ active: activeTool === 'text', disabled: !canEdit || !cropLocked }"
            @click="canEdit && cropLocked && switchTool('text')"
            :title="!cropLocked ? 'Lock crop first' : 'Text'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="4 7 4 4 20 4 20 7"/><line x1="9" y1="20" x2="15" y2="20"/><line x1="12" y1="4" x2="12" y2="20"/></svg>
            <span>Text</span>
          </div>
          <div
            class="tool-btn"
            :class="{ active: activeTool === 'filter', disabled: !canEdit || !cropLocked }"
            @click="canEdit && cropLocked && switchTool('filter')"
            :title="!cropLocked ? 'Lock crop first' : 'Filter'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
            <span>Filter</span>
          </div>
        </div>

        <!-- ===== Center Canvas ===== -->
        <div class="editor-center">
          <FabricCanvas
            ref="fabricCanvasRef"
            :key="canvasKey"
            :imageUrl="imageUrl"
            :editable="canEdit"
            :cropLocked="cropLocked"
            @editEvent="onEditEvent"
            @textSelected="onTextSelected"
          />
        </div>

        <!-- ===== Right Properties Panel ===== -->
        <div class="editor-right" v-if="canEdit">
          <!-- Brush Properties -->
          <template v-if="activeTool === 'brush'">
            <div class="prop-section">
              <div class="prop-label">Brush Color</div>
              <div class="color-presets">
                <span
                  v-for="c in brushColors" :key="c"
                  class="color-dot"
                  :class="{ active: brushColor === c }"
                  :style="{ background: c }"
                  @click="pickBrushColor(c)"
                ></span>
              </div>
              <input type="color" :value="brushColor" @input="pickBrushColor(($event.target as HTMLInputElement).value)" class="color-picker-native" />
            </div>
            <div class="prop-section">
              <div class="prop-label">Brush Size</div>
              <a-slider :min="1" :max="20" v-model:value="brushWidth" @change="onBrushWidthChange" />
              <span class="prop-value">{{ brushWidth }}px</span>
            </div>
            <div class="prop-section">
              <a-button size="small" block @click="toggleDrawingMode()" :type="isDrawing ? 'default' : 'primary'">
                {{ isDrawing ? 'Exit Brush' : 'Start Brush' }}
              </a-button>
            </div>
          </template>

          <!-- Text Properties -->
          <template v-if="activeTool === 'text' && selectedText">
            <div class="prop-section">
              <div class="prop-label">Text Size</div>
              <div class="prop-row">
                <a-button size="small" @click="fabricCanvasRef?.setTextFontSize(-2)">A-</a-button>
                <span class="prop-value">{{ selectedText.fontSize }}px</span>
                <a-button size="small" @click="fabricCanvasRef?.setTextFontSize(2)">A+</a-button>
              </div>
            </div>
            <div class="prop-section">
              <div class="prop-label">Text Color</div>
              <input type="color" :value="selectedText.fill" @input="fabricCanvasRef?.setTextColor(($event.target as HTMLInputElement).value)" class="color-picker-native" />
            </div>
            <div class="prop-section">
              <div class="prop-label">Style</div>
              <div class="prop-row">
                <a-button size="small" :type="selectedText.fontWeight === 'bold' ? 'primary' : 'default'" @click="fabricCanvasRef?.toggleTextBold()">B</a-button>
                <a-button size="small" :type="selectedText.fontStyle === 'italic' ? 'primary' : 'default'" @click="fabricCanvasRef?.toggleTextItalic()">I</a-button>
                <a-button size="small" danger @click="fabricCanvasRef?.deleteSelectedObject()">Delete</a-button>
              </div>
            </div>
            <div class="prop-section">
              <a-button size="small" block @click="addTextAnnotation()">Add New Text</a-button>
            </div>
          </template>

          <!-- Text - No Selection -->
          <template v-if="activeTool === 'text' && !selectedText">
            <div class="prop-section">
              <div class="prop-hint">Click "Add New Text" to create a text label, then double-click to edit content.</div>
              <a-button size="small" block type="primary" @click="addTextAnnotation()">Add New Text</a-button>
            </div>
          </template>

          <!-- Filter Properties -->
          <template v-if="activeTool === 'filter'">
            <div class="prop-section">
              <div class="prop-label">Apply Filter</div>
              <div class="filter-grid">
                <div
                  v-for="f in filters" :key="f.key"
                  class="filter-card"
                  @click="onFilterClick({ key: f.key })"
                >
                  <span
                    class="filter-thumb"
                    :style="{ backgroundImage: `url(${imageUrl})`, filter: f.css }"
                  ></span>
                  <span class="filter-name">{{ f.label }}</span>
                </div>
              </div>
            </div>
            <div class="prop-section">
              <a-button size="small" block @click="onFilterClick({ key: 'none' })">Reset Filters</a-button>
            </div>
          </template>

          <!-- Default -->
          <template v-if="!activeTool">
            <div class="prop-hint">Select a tool from the left panel to start editing.</div>
          </template>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref, watchEffect } from 'vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import PictureEditWebSocket from '@/utils/pictureEditWebSocket.ts'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'
import FabricCanvas from './FabricCanvas.vue'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

const isTeamSpace = computed(() => {
  return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})

const fabricCanvasRef = ref<InstanceType<typeof FabricCanvas>>()

const changeScale = (num: number) => {
  fabricCanvasRef.value?.changeScale(num)
  if (num > 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
  } else {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}

const rotateLeft = () => {
  fabricCanvasRef.value?.rotateLeft()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}

const rotateRight = () => {
  fabricCanvasRef.value?.rotateRight()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

const handleConfirm = async () => {
  if (!fabricCanvasRef.value) return
  try {
    const blob = await fabricCanvasRef.value.getCropBlob()
    const fileName = (props.picture?.name || 'image') + '.webp'
    const file = new File([blob], fileName, { type: blob.type })
    handleUpload({ file })
  } catch (err: any) {
    console.error('Crop failed', err)
    message.error('Crop failed: ' + (err.message || 'Unknown error'))
  }
}

const loading = ref(false)

const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('Image saved')
      if (websocket && isTeamSpace.value) {
        websocket.sendMessage({
          type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.SAVE_IMAGE,
          params: { pictureUrl: res.data.data.url },
        })
      }
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('Upload failed: ' + res.data.message)
    }
  } catch (error: any) {
    console.error('Upload failed', error)
    message.error('Upload failed: ' + (error.message || 'Unknown error'))
  }
  loading.value = false
}

const visible = ref(false)
const canvasKey = ref(0)
const windowWidth = ref(window.innerWidth)
const cropperWidth = computed(() => Math.min(1100, windowWidth.value - 32))
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const openModal = () => {
  visible.value = true
  canvasKey.value++
}

const closeModal = () => {
  visible.value = false
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  if (websocket) {
    websocket.disconnect()
    websocket = null
  }
  editingUser.value = undefined
}

defineExpose({ openModal })

const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

const editingUser = ref<API.UserVO>()
const canEnterEdit = computed(() => !editingUser.value)
const canExitEdit = computed(() => editingUser.value?.id === loginUser.id)
const canEdit = computed(() => {
  if (!isTeamSpace.value) return true
  if (isSyncing) return false
  return editingUser.value?.id === loginUser.id
})

let websocket: PictureEditWebSocket | null
let isSyncing = false
let heartbeatTimer: ReturnType<typeof setInterval> | null = null

// ── Crop Lock ──
const cropLocked = ref(true)

// ── Active Tool ──
const activeTool = ref<'brush' | 'eraser' | 'text' | 'filter' | null>(null)

/** 统一工具切换：关闭旧工具状态，激活新工具。选工具时自动上锁剪切框 */
function switchTool(tool: 'brush' | 'eraser' | 'text' | 'filter') {
  // 剪切框解锁时不允许使用标注工具
  if (!cropLocked.value) return
  // 点击当前已激活的工具 → 取消激活
  if (activeTool.value === tool) {
    deactivateAllTools()
    activeTool.value = null
    return
  }
  // 关闭所有旧工具
  deactivateAllTools()
  // 激活新工具
  activeTool.value = tool
  if (tool === 'brush') {
    toggleDrawingMode()
  } else if (tool === 'eraser') {
    toggleEraserMode()
  }
}

/** 切换剪切框锁定：解锁时退出当前编辑工具 */
function toggleCropLock() {
  cropLocked.value = !cropLocked.value
  if (!cropLocked.value) {
    // 解锁剪切框 → 强制退出当前工具
    deactivateAllTools()
    activeTool.value = null
  }
}

function deactivateAllTools() {
  if (isDrawing.value) {
    isDrawing.value = false
    fabricCanvasRef.value?.toggleDrawing(false)
  }
  if (isEraser.value) {
    isEraser.value = false
    fabricCanvasRef.value?.toggleEraser(false)
  }
}

// ── Brush ──
const isDrawing = ref(false)
const isEraser = ref(false)
const brushColor = ref('#ff0000')
const brushColors = ['#ff0000', '#00ff00', '#0000ff', '#ffff00', '#ff00ff', '#00ffff', '#ffffff', '#000000']
const brushWidth = ref(3)

// ── Text ──
const selectedText = ref<{ customId: string; fontSize: number; fill: string; fontWeight: string; fontStyle: string } | null>(null)

// ── Filters ──
const filters = [
  { key: 'grayscale', label: 'Grayscale',  css: 'grayscale(100%)' },
  { key: 'sepia',     label: 'Sepia',      css: 'sepia(100%)' },
  { key: 'invert',    label: 'Invert',     css: 'invert(100%)' },
  { key: 'blur',      label: 'Blur',       css: 'blur(2px)' },
  { key: 'sharpen',   label: 'Sharpen',    css: 'contrast(150%) brightness(110%)' },
]

function onTextSelected(info: typeof selectedText.value) {
  console.log('[ImageCropper] onTextSelected customId=', info?.customId, 'fill=', info?.fill, 'size=', info?.fontSize, 'infoNull=', info === null)
  selectedText.value = info
  if (info) activeTool.value = 'text'
}

function toggleDrawingMode() {
  isDrawing.value = !isDrawing.value
  if (isDrawing.value) {
    isEraser.value = false
    fabricCanvasRef.value?.toggleEraser(false)
  }
  fabricCanvasRef.value?.toggleDrawing(isDrawing.value, brushColor.value, brushWidth.value)
}

function toggleEraserMode() {
  isEraser.value = !isEraser.value
  if (isEraser.value) {
    isDrawing.value = false
    fabricCanvasRef.value?.toggleDrawing(false)
  }
  fabricCanvasRef.value?.toggleEraser(isEraser.value)
}

function onBrushWidthChange(v: number) {
  brushWidth.value = v
  fabricCanvasRef.value?.setDrawingWidth(v)
}

function pickBrushColor(color: string) {
  brushColor.value = color
  fabricCanvasRef.value?.setDrawingColor(color)
}

watchEffect(() => {
  if (isDrawing.value) {
    fabricCanvasRef.value?.setDrawingWidth(brushWidth.value)
  }
})

function addTextAnnotation() {
  fabricCanvasRef.value?.addText()
}

function onFilterClick({ key }: { key: string }) {
  if (key === 'none') {
    fabricCanvasRef.value?.removeFilters()
  } else {
    fabricCanvasRef.value?.applyFilter(key)
  }
}

function clearAnnotationsBtn() {
  fabricCanvasRef.value?.clearAnnotations()
}

const initWebsocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !visible.value) return

  if (websocket) { websocket.disconnect(); websocket = null }
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
  websocket = new PictureEditWebSocket(pictureId)
  websocket.connect()

  websocket.on('open', () => {
    heartbeatTimer = setInterval(() => {
      websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.PING })
    }, 60000)
  })
  websocket.on('close', () => {
    if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
  })
  websocket.on('reconnecting', (msg: any) => {
    message.warning('协同编辑连接断开，正在重连...（' + msg.attempt + '/' + msg.max + '）')
  })
  websocket.on('reconnected', () => {
    message.success('协同编辑已重新连接')
  })
  websocket.on('reconnectFailed', () => {
    message.error('协同编辑连接失败，请刷新页面重试')
    editingUser.value = undefined
  })

  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg: any) => { message.info(msg.message) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg: any) => { message.error(msg.message) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg: any) => { editingUser.value = msg.user })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg: any) => { editingUser.value = undefined })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg: any) => { applyRemoteAction(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_MOVING, (msg: any) => { applyRemoteAction(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_MODIFIED, (msg: any) => { applyRemoteAction(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_SCALING, (msg: any) => { applyRemoteAction(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.CROP_CHANGE, (msg: any) => { fabricCanvasRef.value?.applyRemoteEdit(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.CANVAS_ZOOM, (msg: any) => { fabricCanvasRef.value?.applyRemoteEdit(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.SAVE_COMPLETE, (msg: any) => {
    message.info(msg.message || 'Image saved')
    if (msg.pictureUrl && props.onSuccess) {
      props.onSuccess({ ...props.picture, url: msg.pictureUrl } as any)
    }
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.SYNC_START, () => { isSyncing = true })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.SYNC_HISTORY, (msg: any) => { fabricCanvasRef.value?.applyRemoteEdit(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.SYNC_END, () => { isSyncing = false })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_ADDED, (msg: any) => { fabricCanvasRef.value?.applyRemoteEdit(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_REMOVED, (msg: any) => { fabricCanvasRef.value?.applyRemoteEdit(msg) })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_SELECTED, (msg: any) => {
    fabricCanvasRef.value?.applyRemoteEdit(msg)
    if (msg.user?.id !== loginUser.id) {
      message.info(`${msg.user?.userName || 'Other user'} selected ${msg.targetId}`)
    }
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.OBJECT_DESELECTED, (msg: any) => { fabricCanvasRef.value?.applyRemoteEdit(msg) })
}

function applyRemoteAction(msg: any) {
  if (isSyncing) { fabricCanvasRef.value?.applyRemoteEdit(msg); return }
  switch (msg.editAction) {
    case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT: fabricCanvasRef.value?.rotateLeft(); break
    case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT: fabricCanvasRef.value?.rotateRight(); break
    case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN: fabricCanvasRef.value?.changeScale(1); break
    case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT: fabricCanvasRef.value?.changeScale(-1); break
    default: fabricCanvasRef.value?.applyRemoteEdit(msg); break
  }
}

watchEffect(() => { if (isTeamSpace.value) { initWebsocket() } })
onUnmounted(() => {
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
  if (websocket) { websocket.disconnect(); websocket = null }
  editingUser.value = undefined
})

const enterEdit = () => { websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT }) }
const exitEdit = () => { websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT }) }
const editAction = (action: string) => { websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, editAction: action }) }

function onEditEvent(msg: {
  type: string; editAction: string; targetId?: string;
  params?: Record<string, unknown>; cropParams?: Record<string, number>;
  objectJSON?: string; timestamp: number
}) {
  if (!websocket || !isTeamSpace.value) return
  websocket.sendMessage({
    type: msg.type, editAction: msg.editAction, targetId: msg.targetId,
    params: msg.params, cropParams: msg.cropParams, objectJSON: msg.objectJSON, timestamp: msg.timestamp,
  })
}
</script>

<style scoped>
/* ===== Layout ===== */
.editor-layout {
  display: flex;
  flex-direction: column;
  height: 620px;
  background: var(--yu-bg);
  font-family: Inter, -apple-system, sans-serif;
  border-radius: 4px;
  overflow: hidden;
}

/* ===== Top Bar ===== */
.editor-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 12px;
  background: var(--yu-bg-card);
  border-bottom: 1px solid var(--yu-border);
  flex-shrink: 0;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
.editor-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--yu-text-primary);
  letter-spacing: -0.01em;
}
.zoom-label {
  font-size: 12px;
  color: var(--yu-text-secondary);
  min-width: 36px;
  text-align: center;
}

/* ===== Body ===== */
.editor-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ===== Left Toolbar ===== */
.editor-left {
  width: 64px;
  background: var(--yu-bg-card);
  border-right: 1px solid var(--yu-border);
  display: flex;
  flex-direction: column;
  padding: 8px 0;
  gap: 2px;
  flex-shrink: 0;
}
.tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 52px;
  cursor: pointer;
  color: var(--yu-text-secondary);
  font-size: 10px;
  gap: 3px;
  border-left: 2px solid transparent;
  transition: all 0.15s;
}
.tool-btn:hover {
  color: var(--yu-text-primary);
  background: var(--yu-bg-hover);
}
.tool-btn.active {
  color: var(--yu-primary);
  border-left-color: var(--yu-primary);
  background: var(--yu-primary-bg);
}
.tool-btn.disabled {
  opacity: 0.35;
  cursor: not-allowed;
  pointer-events: none;
}

/* ===== Center Canvas ===== */
.editor-center {
  flex: 1;
  display: flex;
  padding: 8px;
  min-width: 0;
}

/* ===== Right Panel ===== */
.editor-right {
  width: 200px;
  background: var(--yu-bg-card);
  border-left: 1px solid var(--yu-border);
  padding: 12px;
  overflow-y: auto;
  flex-shrink: 0;
}
.prop-section {
  margin-bottom: 16px;
}
.prop-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--yu-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}
.prop-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.prop-value {
  font-size: 12px;
  color: var(--yu-text-primary);
  min-width: 32px;
  text-align: center;
}
.prop-hint {
  font-size: 12px;
  color: var(--yu-text-tertiary);
  line-height: 1.5;
}
.color-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 6px;
}
.color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid var(--yu-border);
  transition: border-color 0.15s;
}
.color-dot:hover {
  border-color: var(--yu-primary);
}
.color-dot.active {
  border-color: var(--yu-primary);
  box-shadow: 0 0 0 2px rgba(59,130,246,.25);
}
.color-picker-native {
  width: 100%;
  height: 28px;
  border: 1px solid var(--yu-border);
  border-radius: 4px;
  cursor: pointer;
  padding: 2px;
  background: var(--yu-bg);
}
.filter-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.filter-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  overflow: hidden;
}
.filter-card:hover {
  background: var(--yu-primary-bg);
  border-color: var(--yu-primary);
}
.filter-thumb {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 4px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  background-color: var(--yu-bg-hover);
}
.filter-name {
  font-size: 11px;
  color: var(--yu-text-secondary);
  padding: 3px 0 5px;
}
</style>
