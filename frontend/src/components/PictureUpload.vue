<template>
  <div class="picture-upload">
    <!-- 已上传预览 -->
    <div v-if="picture?.url" class="upload-preview">
      <img :src="picture.url" :alt="picture.name" />
      <div class="preview-overlay">
        <a-button type="primary" size="small" @click="triggerUpload">更换图片</a-button>
      </div>
    </div>

    <!-- 上传拖拽区 -->
    <a-upload-dragger
      v-else
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
      :show-upload-list="false"
      :multiple="false"
      class="upload-dragger"
      :class="{ 'is-uploading': uploading }"
    >
      <div class="dragger-content">
        <div class="dragger-icon">
          <CloudUploadOutlined v-if="!uploading" />
          <LoadingOutlined v-else spin />
        </div>
        <p class="dragger-title">点击或拖拽图片到此区域上传</p>
        <p class="dragger-hint">支持 JPG / PNG 格式，单张不超过 15MB</p>
        <a-progress v-if="uploading" :percent="uploadPercent" :show-info="false" size="small" style="max-width: 240px; margin: 12px auto 0" />
      </div>
    </a-upload-dragger>

    <!-- 错误提示 -->
    <div v-if="errorMsg" class="upload-error">
      <a-alert :message="errorMsg" type="error" show-icon closable @close="errorMsg = ''" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CloudUploadOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

const uploading = ref(false)
const uploadPercent = ref(0)
const errorMsg = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const handleUpload = async ({ file }: any) => {
  uploading.value = true
  uploadPercent.value = 30
  errorMsg.value = ''
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    uploadPercent.value = 60
    const res = await uploadPictureUsingPost(params, {}, file)
    uploadPercent.value = 100
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
    } else {
      errorMsg.value = res.data.message || '上传失败'
    }
  } catch (error: any) {
    console.error('图片上传失败', error)
    errorMsg.value = error.message || '上传失败'
  }
  uploading.value = false
  uploadPercent.value = 0
}

const beforeUpload = (file: any) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('不支持上传该格式的图片，推荐 JPG 或 PNG')
    return false
  }
  const isLt15M = file.size / 1024 / 1024 < 15
  if (!isLt15M) {
    message.error('不能上传超过 15MB 的图片')
    return false
  }
  return true
}

const triggerUpload = () => {
  // 通过点击预览区的按钮触发文件选择
  const input = document.querySelector('.picture-upload .ant-upload input[type="file"]') as HTMLInputElement
  if (input) input.click()
}
</script>

<style scoped>
.picture-upload {
  width: 100%;
}

/* 预览区 */
.upload-preview {
  position: relative;
  border-radius: var(--yu-radius-lg);
  overflow: hidden;
  max-width: 480px;
}

.upload-preview img {
  width: 100%;
  max-height: 360px;
  object-fit: contain;
  background: var(--yu-bg-card);
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--yu-transition-normal);
}

.upload-preview:hover .preview-overlay {
  opacity: 1;
}

/* 拖拽上传区 */
.upload-dragger :deep(.ant-upload-drag) {
  border: 2px dashed var(--yu-border) !important;
  border-radius: var(--yu-radius-lg) !important;
  background: var(--yu-bg-card) !important;
  transition: all var(--yu-transition-normal);
  padding: 48px 24px;
}

.upload-dragger :deep(.ant-upload-drag:hover) {
  border-color: var(--yu-primary) !important;
  background: var(--yu-bg-hover) !important;
}

.upload-dragger :deep(.ant-upload-drag-hover) {
  border-color: var(--yu-success) !important;
  background: var(--yu-success-bg) !important;
}

.upload-dragger.is-uploading :deep(.ant-upload-drag) {
  border-color: var(--yu-primary) !important;
  pointer-events: none;
}

.dragger-content {
  text-align: center;
}

.dragger-icon {
  font-size: 40px;
  color: var(--yu-text-tertiary);
  margin-bottom: 12px;
}

.upload-dragger.is-uploading .dragger-icon {
  color: var(--yu-primary);
}

.dragger-title {
  font-size: 15px;
  color: var(--yu-text-primary);
  margin: 0 0 6px;
}

.dragger-hint {
  font-size: 13px;
  color: var(--yu-text-tertiary);
  margin: 0;
}

/* 错误 */
.upload-error {
  margin-top: 12px;
}
</style>
