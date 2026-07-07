<template>
  <div id="homePage">
    <!-- Hero 搜索区 -->
    <div class="hero-section">
      <h1 class="hero-title">发现精彩图片</h1>
      <p class="hero-subtitle">探索海量高质量照片，寻找灵感</p>
      <div class="hero-search">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索图片名称、简介、分类..."
          enter-button="搜索"
          size="large"
          @search="doSearch"
          class="hero-search-input"
        />
      </div>
    </div>

    <!-- 分类 pill 标签 -->
    <div class="category-bar">
      <a-button
        :type="selectedCategory === 'all' ? 'primary' : 'default'"
        shape="round"
        size="small"
        @click="selectedCategory = 'all'; doSearch()"
        class="cat-btn"
      >全部</a-button>
      <a-button
        v-for="cat in categoryList"
        :key="cat"
        :type="selectedCategory === cat ? 'primary' : 'default'"
        shape="round"
        size="small"
        @click="selectedCategory = cat; doSearch()"
        class="cat-btn"
      >{{ cat }}</a-button>
    </div>

    <!-- 标签 -->
    <div v-if="tagList.length" class="tag-bar">
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>

    <!-- 图片列表 -->
    <PictureList
      ref="pictureListRef"
      :dataList="list"
      :loading="loading && currentPage === 1"
      :loadingMore="loadingMore"
      :hasMore="hasMore"
    />

    <!-- 无限滚动已内置于 PictureList → JustifiedGrid -->
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageWithCacheUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

// 筛选状态
const selectedCategory = ref<string>('all')
const selectedTagList = ref<boolean[]>([])
const searchText = ref<string>('')
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])

const buildSearchParams = (): API.PictureQueryRequest => {
  const params: API.PictureQueryRequest = {
    sortField: 'createTime',
    sortOrder: 'descend',
    searchText: searchText.value || undefined,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags!.push(tagList.value[index])
    }
  })
  return params
}

const {
  list,
  loading,
  hasMore,
  reset,
  loadMore,
  observeSentinel,
  currentPage,
} = useInfiniteScroll<API.PictureVO>({
  pageSize: 12,
  async fetcher(page, pageSize) {
    const params = buildSearchParams()
    const res = await listPictureVoByPageWithCacheUsingPost({ ...params, current: page, pageSize })
    if (res.data.code === 0 && res.data.data) {
      return { records: res.data.data.records ?? [], total: res.data.data.total ?? 0 }
    } else {
      message.error('获取数据失败，' + res.data.message)
      return null
    }
  },
})

// 判断是否正在加载更多（非首次加载）
const loadingMore = computed(() => loading.value && currentPage.value > 1)

const doSearch = () => {
  reset()
}

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

const pictureListRef = ref()

/** 连接 sentinel，延迟等 DOM 就绪 */
function connectSentinel() {
  setTimeout(() => {
    const sentinel = pictureListRef.value?.gridRef?.sentinelRef
    if (sentinel) observeSentinel(sentinel)
  }, 200)
}

// hasMore 变为 true 时（切换分类后 sentinel 重新创建），自动重新观测
watch(hasMore, (val) => {
  if (val) connectSentinel()
})

onMounted(async () => {
  reset()
  getTagCategoryOptions()
  await nextTick()
  connectSentinel()
})
</script>

<style scoped>
#homePage {
  max-width: var(--yu-content-max-width);
  margin: 0 auto;
}

/* Hero 区域 */
.hero-section {
  text-align: center;
  padding: 48px 20px 32px;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--yu-text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.hero-subtitle {
  font-size: 16px;
  color: var(--yu-text-secondary);
  margin-bottom: 28px;
}

.hero-search {
  max-width: 520px;
  margin: 0 auto;
}

.hero-search-input :deep(.ant-input-search-button) {
  border-radius: 0 var(--yu-radius-md) var(--yu-radius-md) 0 !important;
}

.hero-search-input :deep(.ant-input) {
  border-radius: var(--yu-radius-md) 0 0 var(--yu-radius-md) !important;
}

/* 分类 bar */
.category-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 12px;
}

.cat-btn {
  transition: all var(--yu-transition-fast);
}

/* 标签 */
.tag-bar {
  margin-bottom: 20px;
  text-align: center;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 16px;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 24px;
  }
  .hero-section {
    padding: 24px 12px 16px;
  }
}
</style>
