<template>
  <div class="space-category-analyze">
    <a-card title="空间图片分类分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%;" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceCategoryAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { useChartTheme } from '@/utils/chartTheme'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const { textColor, axisColor, splitColor } = useChartTheme()

const dataList = ref<API.SpaceCategoryAnalyzeResponse>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceCategoryAnalyzeUsingPost({
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

const options = computed(() => {
  const categories = dataList.value.map((item) => item.category)
  const countData = dataList.value.map((item) => item.count)
  const sizeData = dataList.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2))

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: ['图片数量', '图片总大小'], top: 'bottom', textStyle: { color: textColor.value } },
    xAxis: {
      type: 'category', data: categories,
      axisLabel: { color: textColor.value },
      axisLine: { lineStyle: { color: axisColor.value } },
    },
    yAxis: [
      {
        type: 'value', name: '图片数量',
        nameTextStyle: { color: textColor.value },
        axisLabel: { color: textColor.value },
        axisLine: { show: true, lineStyle: { color: '#3B82F6' } },
        splitLine: { lineStyle: { color: splitColor.value, type: 'dashed' } },
      },
      {
        type: 'value', name: '图片总大小 (MB)', position: 'right',
        nameTextStyle: { color: textColor.value },
        axisLabel: { color: textColor.value },
        axisLine: { show: true, lineStyle: { color: '#10B981' } },
        splitLine: { show: false },
      },
    ],
    series: [
      { name: '图片数量', type: 'bar', data: countData, yAxisIndex: 0, color: '#3B82F6' },
      { name: '图片总大小', type: 'bar', data: sizeData, yAxisIndex: 1, color: '#10B981' },
    ],
  }
})
</script>

<style scoped></style>
