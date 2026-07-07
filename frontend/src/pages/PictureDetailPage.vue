<template>
  <PictureLightbox
    :visible="lightboxVisible"
    :picture="picture"
    :picture-list="pictureList"
    :picture-index="pictureIndex"
    @close="goBack"
    @prev="navigate(-1)"
    @next="navigate(1)"
    @goto="navigateTo"
  />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import PictureLightbox from '@/components/PictureLightbox.vue'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const router = useRouter()
const lightboxVisible = ref(true)
const picture = ref<API.PictureVO | null>(null)
const pictureList = ref<API.PictureVO[]>([])
const pictureIndex = ref(0)

const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      // 如果只有一张图，就用这个图片做列表
      if (pictureList.value.length === 0) {
        pictureList.value = [res.data.data]
        pictureIndex.value = 0
      }
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})

const goBack = () => {
  lightboxVisible.value = false
  router.back()
}

const navigate = (delta: number) => {
  const newIdx = pictureIndex.value + delta
  if (newIdx >= 0 && newIdx < pictureList.value.length) {
    pictureIndex.value = newIdx
    picture.value = pictureList.value[newIdx]
    // 更新 URL 但不刷新
    router.replace({ path: `/picture/${picture.value?.id}` })
  }
}

const navigateTo = (index: number) => {
  if (index >= 0 && index < pictureList.value.length) {
    pictureIndex.value = index
    picture.value = pictureList.value[index]
    router.replace({ path: `/picture/${picture.value?.id}` })
  }
}

// 当路由 props.id 改变时重新请求
watch(() => props.id, () => {
  fetchPictureDetail()
})
</script>
