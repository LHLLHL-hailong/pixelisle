<template>
  <div id="spaceAnalyzePage">
    <div class="analyze-header">
      <h2>
        空间图库分析 —
        <span v-if="queryAll">全部空间</span>
        <span v-else-if="queryPublic">公共图库</span>
        <span v-else>
          <a :href="`/space/${spaceId}`" target="_blank">空间 id：{{ spaceId }}</a>
        </span>
      </h2>
    </div>

    <!-- Bento Grid 布局 -->
    <div class="bento-grid">
      <!-- 空间使用分析 — 大卡片 2x1 -->
      <div class="bento-card bento-2x1">
        <SpaceUsageAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </div>
      <!-- 分类分析 — 1x1 -->
      <div class="bento-card bento-1x1">
        <SpaceCategoryAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </div>
      <!-- 标签分析 — 1x1 -->
      <div class="bento-card bento-1x1">
        <SpaceTagAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </div>
      <!-- 大小分布 — 1x1 -->
      <div class="bento-card bento-1x1">
        <SpaceSizeAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </div>
      <!-- 用户行为分析 — 全宽 -->
      <div class="bento-card bento-full">
        <SpaceUserAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </div>
      <!-- 空间排行 — 全宽（仅管理员可见） -->
      <div v-if="isAdmin" class="bento-card bento-full">
        <SpaceRankAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import SpaceUsageAnalyze from '@/components/analyze/SpaceUsageAnalyze.vue'
import SpaceCategoryAnalyze from '@/components/analyze/SpaceCategoryAnalyze.vue'
import SpaceTagAnalyze from '@/components/analyze/SpaceTagAnalyze.vue'
import SpaceSizeAnalyze from '@/components/analyze/SpaceSizeAnalyze.vue'
import SpaceUserAnalyze from '@/components/analyze/SpaceUserAnalyze.vue'
import SpaceRankAnalyze from '@/components/analyze/SpaceRankAnalyze.vue'

const route = useRoute()
const spaceId = computed(() => route.query?.spaceId as string)
const queryAll = computed(() => !!route.query?.queryAll)
const queryPublic = computed(() => !!route.query?.queryPublic)

const loginUserStore = useLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
</script>

<style scoped>
#spaceAnalyzePage {
  max-width: var(--yu-content-max-width);
  margin: 0 auto;
}

.analyze-header {
  margin-bottom: 20px;
}

.analyze-header h2 {
  color: var(--yu-text-primary);
  margin: 0;
}

/* Bento Grid */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.bento-card {
  background: var(--yu-bg-card);
  border: 1px solid var(--yu-border);
  border-radius: var(--yu-radius-lg);
  padding: 16px;
  transition: all var(--yu-transition-normal);
}

.bento-card:hover {
  border-color: var(--yu-border);
  box-shadow: var(--yu-shadow-md);
}

.bento-2x1 {
  grid-column: span 2;
}

.bento-1x1 {
  grid-column: span 1;
}

.bento-full {
  grid-column: span 4;
}

@media (max-width: 1200px) {
  .bento-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bento-2x1,
  .bento-full {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .bento-grid {
    grid-template-columns: 1fr;
  }
  .bento-2x1,
  .bento-1x1,
  .bento-full {
    grid-column: span 1;
  }
}
</style>
