<template>
  <div class="space-tag-analyze">
    <a-card title="空间图片标签分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import 'echarts-wordcloud'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceTagAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const dataList = ref<API.SpaceCategoryAnalyzeResponse>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceTagAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

watchEffect(() => { fetchData() })

const TAG_COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#6366F1', '#8B5CF6', '#EC4899']

const options = computed(() => {
  const tagData = dataList.value.map((item) => ({
    name: item.tag,
    value: item.count,
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: (params: any) => `${params.name}: ${params.value} 次` },
    series: [{
      type: 'wordCloud',
      gridSize: 10,
      sizeRange: [14, 48],
      rotationRange: [-90, 90],
      shape: 'circle',
      textStyle: {
        color: () => TAG_COLORS[Math.floor(Math.random() * TAG_COLORS.length)],
      },
      data: tagData,
    }],
  }
})
</script>

<style scoped></style>
