<template>
  <div class="space-rank-analyze">
    <a-card title="空间使用排行分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%;" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceRankAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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

const dataList = ref<API.Space[]>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceRankAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    topN: 10,
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
  const spaceNames = dataList.value.map((item) => item.spaceName)
  const usageData = dataList.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2))

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category', data: spaceNames,
      axisLabel: { color: textColor.value, rotate: 30 },
      axisLine: { lineStyle: { color: axisColor.value } },
    },
    yAxis: {
      type: 'value', name: '空间使用量 (MB)',
      nameTextStyle: { color: textColor.value },
      axisLabel: { color: textColor.value },
      splitLine: { lineStyle: { color: splitColor.value } },
    },
    series: [{
      name: '空间使用量 (MB)', type: 'bar', data: usageData,
      itemStyle: { color: '#3B82F6', borderRadius: [4, 4, 0, 0] },
    }],
  }
})
</script>

<style scoped></style>
