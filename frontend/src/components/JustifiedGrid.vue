<template>
  <div ref="containerRef" class="justified-grid-container">
    <div v-if="boxes.length === 0 && !loading" class="empty-state">
      <PictureOutlined style="font-size: 48px; color: var(--yu-text-tertiary)" />
      <p>暂无图片</p>
    </div>
    <div v-if="loading" class="skeleton-grid">
      <div
        v-for="i in skeletonCount"
        :key="i"
        class="skeleton-item"
        :style="skeletonStyle(i)"
      />
    </div>
    <div
      v-else
      class="justified-grid"
      :style="{ height: containerHeight + 'px', position: 'relative' }"
    >
      <div
        v-for="(box, index) in boxes"
        :key="(box.data?.id ?? index)"
        class="grid-item"
        :style="{
          position: 'absolute',
          top: box.top + 'px',
          left: box.left + 'px',
          width: box.width + 'px',
          height: box.height + 'px',
        }"
        @click="emit('click', box.data, index)"
      >
        <!-- 占位渐变 -->
        <div class="placeholder" :style="{ background: placeholderGradient }" />
        <!-- 实际图片 -->
        <img
          :src="box.data?.thumbnailUrl ?? box.data?.url"
          :alt="box.data?.name ?? ''"
          class="grid-image"
          loading="lazy"
          @load="(e) => (e.target as HTMLElement).classList.add('loaded')"
        />
        <!-- Hover 叠加层 -->
        <div class="grid-overlay">
          <div class="overlay-info">
            <span class="overlay-name">{{ box.data?.name ?? '' }}</span>
            <span v-if="box.data?.category" class="overlay-category">
              {{ box.data?.category }}
            </span>
          </div>
          <div v-if="$slots.actions" class="overlay-actions" @click.stop>
            <slot name="actions" :item="box.data" />
          </div>
        </div>
        <!-- 选中框 -->
        <div v-if="selectedIds?.has(box.data?.id)" class="selected-border" />
      </div>
    </div>

    <!-- 无限滚动哨兵 + 加载指示器 -->
    <div
      v-if="hasMore"
      ref="sentinelRef"
      class="infinite-sentinel"
    />
    <div v-if="loadingMore" class="loading-more">
      <a-spin size="small" />
      <span>加载中...</span>
    </div>
    <div v-if="!hasMore && items.length > 0" class="no-more">
      已加载全部 {{ items.length }} 张图片
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import { computeJustifiedGrid, type JustifiedGridItem } from '@/utils/justifiedGrid'

interface Props {
  items: API.PictureVO[]
  loading?: boolean
  selectedIds?: Set<number>
  hasMore?: boolean
  loadingMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  loading: false,
  hasMore: false,
  loadingMore: false,
})

const emit = defineEmits<{
  (e: 'click', item: API.PictureVO, index: number): void
}>()

const sentinelRef = ref<HTMLElement | null>(null)
defineExpose({ sentinelRef })

const containerRef = ref<HTMLElement | null>(null)
const containerWidth = ref(800)

const skeletonCount = 12

/** 骨架屏随机宽度模拟真实布局 */
function skeletonStyle(i: number) {
  const widths = [240, 200, 260, 220, 180, 280, 210, 250, 190, 230, 270, 200]
  return {
    width: widths[i % widths.length] + 'px',
    height: '240px',
    animationDelay: (i * 0.05) + 's',
  }
}

const placeholderGradient = computed(() =>
  `linear-gradient(135deg, var(--yu-placeholder-1) 0%, var(--yu-placeholder-2) 50%, var(--yu-placeholder-1) 100%)`
)

const gridItems = computed<JustifiedGridItem[]>(() =>
  props.items.map((item) => ({
    width: item.picWidth || 4,
    height: item.picHeight || 3,
    data: item,
  }))
)

const boxes = ref<ReturnType<typeof computeJustifiedGrid>['boxes']>([])
const containerHeight = ref(0)

function recalc() {
  if (!containerRef.value) return
  const w = containerRef.value.clientWidth
  if (w <= 0) return
  containerWidth.value = w
  const result = computeJustifiedGrid(gridItems.value, {
    containerWidth: w,
    targetRowHeight: 240,
    maxStretch: 1.2,
  })
  boxes.value = result.boxes
  containerHeight.value = result.containerHeight
}

let resizeObserver: ResizeObserver | null = null

onMounted(async () => {
  await nextTick()
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => recalc())
    resizeObserver.observe(containerRef.value)
  }
  recalc()
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})

watch(() => props.items, () => {
  nextTick(() => recalc())
})
</script>

<style scoped>
.justified-grid-container {
  width: 100%;
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--yu-text-tertiary);
  gap: 12px;
}

/* 骨架屏 */
.skeleton-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton-item {
  background: linear-gradient(
    90deg,
    var(--yu-placeholder-1) 25%,
    var(--yu-placeholder-2) 37%,
    var(--yu-placeholder-1) 63%
  );
  background-size: 400% 100%;
  border-radius: var(--yu-radius-md);
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 网格项 */
.grid-item {
  border-radius: var(--yu-radius-md);
  overflow: hidden;
  cursor: pointer;
  background: var(--yu-placeholder-1);
  transition: transform var(--yu-transition-fast), box-shadow var(--yu-transition-fast);
}

.grid-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--yu-shadow-lg);
  z-index: 1;
}

.placeholder {
  position: absolute;
  inset: 0;
  background-size: 200% 200%;
  animation: placeholder-shimmer 2s ease-in-out infinite;
}
@keyframes placeholder-shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.grid-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.35s ease;
}

.grid-image.loaded {
  opacity: 1;
}

/* Hover 覆盖层 */
.grid-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 40%, transparent 100%);
  opacity: 0;
  transition: opacity var(--yu-transition-normal);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 12px;
}

.grid-item:hover .grid-overlay {
  opacity: 1;
}

.overlay-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.overlay-name {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-category {
  color: rgba(255,255,255,0.7);
  font-size: 12px;
}

.overlay-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
}

/* 选中边框 */
.selected-border {
  position: absolute;
  inset: 0;
  border: 3px solid var(--yu-primary);
  border-radius: var(--yu-radius-md);
  pointer-events: none;
}

/* 无限滚动 */
.infinite-sentinel {
  height: 1px;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: var(--yu-text-secondary);
  font-size: 13px;
}

.no-more {
  text-align: center;
  padding: 20px 0;
  color: var(--yu-text-tertiary);
  font-size: 12px;
}
</style>
