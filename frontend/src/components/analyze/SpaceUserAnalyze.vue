<template>
  <div class="space-user-analyze">
    <a-card title="空间图片用户分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      <template #extra>
        <a-space>
          <a-segmented v-model:value="timeDimension" :options="timeDimensionOptions" />
          <a-input-search placeholder="请输入用户 id" enter-button="搜索用户" @search="doSearch" />
        </a-space>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceUserAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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

const timeDimension = ref<'day' | 'week' | 'month'>('day')
const timeDimensionOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
]
const userId = ref<string>()
const doSearch = (value: string) => { userId.value = value }

const dataList = ref<API.SpaceCategoryAnalyzeResponse>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceUserAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    timeDimension: timeDimension.value,
    userId: userId.value,
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
  const periods = dataList.value.map((item) => item.period)
  const counts = dataList.value.map((item) => item.count)

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category', data: periods, name: '时间区间',
      nameTextStyle: { color: textColor.value },
      axisLabel: { color: textColor.value },
      axisLine: { lineStyle: { color: axisColor.value } },
    },
    yAxis: {
      type: 'value', name: '上传数量',
      nameTextStyle: { color: textColor.value },
      axisLabel: { color: textColor.value },
      splitLine: { lineStyle: { color: splitColor.value } },
    },
    series: [{
      name: '上传数量', type: 'line', data: counts, smooth: true,
      emphasis: { focus: 'series' },
      itemStyle: { color: '#3B82F6' },
    }],
  }
})
</script>

<style scoped></style>
