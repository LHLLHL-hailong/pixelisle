<template>
  <div>
    <a-modal
      v-model:visible="visible"
      :title="title"
      :footer="false"
      :z-index="zIndex"
      @cancel="closeModal"
      class="share-modal"
    >
      <h4 style="color: var(--yu-text-primary)">复制分享链接</h4>
      <a-typography-link copyable>
        {{ link }}
      </a-typography-link>
      <div style="margin-bottom: 16px" />
      <h4 style="color: var(--yu-text-primary)">手机扫码查看</h4>
      <a-qrcode :value="link" :bg-color="qrBgColor" />
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useThemeStore } from '@/stores/useThemeStore'

interface Props {
  title?: string;
  link?: string;
  zIndex?: number;
}

const props = withDefaults(defineProps<Props>(), {
  title: "分享图片",
  link: '',
  zIndex: 1000,
})

const themeStore = useThemeStore()
const qrBgColor = computed(() => themeStore.resolved === 'dark' ? '#252528' : '#FFFFFF')

const visible = ref(false)

const openModal = () => {
  visible.value = true
}

const closeModal = () => {
  visible.value = false;
}

defineExpose({
  openModal,
})
</script>
