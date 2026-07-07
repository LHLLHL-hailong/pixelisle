import { ref, onUnmounted } from 'vue'

interface InfiniteScrollOptions<T> {
  /** 每页大小 */
  pageSize?: number
  /** 请求函数，返回 { records, total } */
  fetcher: (page: number, pageSize: number) => Promise<{ records: T[]; total: number } | null>
}

export function useInfiniteScroll<T>(options: InfiniteScrollOptions<T>) {
  const { pageSize = 12, fetcher } = options

  const list = ref<T[]>([])
  const total = ref(0)
  const loading = ref(false)
  const hasMore = ref(true)
  const currentPage = ref(1)

  let observer: IntersectionObserver | null = null
  let fetching = false
  let requestVersion = 0

  /** 加载下一页 */
  async function loadMore() {
    if (fetching || !hasMore.value) return
    fetching = true
    loading.value = true
    const version = requestVersion
    try {
      const res = await fetcher(currentPage.value, pageSize)
      if (version !== requestVersion) return
      if (res) {
        list.value.push(...res.records)
        total.value = res.total
        hasMore.value = list.value.length < res.total
        if (res.records.length > 0) {
          currentPage.value++
        }
      } else {
        // API 返回异常（code ≠ 0），停止继续加载
        hasMore.value = false
      }
    } catch {
      if (version !== requestVersion) return
      // 网络异常等，停止继续加载避免无限重试
      hasMore.value = false
    } finally {
      if (version === requestVersion) {
        loading.value = false
        fetching = false
      }
    }
  }

  /** 重置并重新加载 */
  async function reset() {
    list.value = []
    total.value = 0
    hasMore.value = true
    currentPage.value = 1
    requestVersion++
    fetching = false
    await loadMore()
  }

  /** 使飞行中的请求失效（不重置列表，仅递增版本号） */
  function invalidate() {
    requestVersion++
    fetching = false
  }

  /** 绑定哨兵元素 */
  function observeSentinel(el: HTMLElement | null) {
    observer?.disconnect()
    if (!el) return
    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          loadMore()
        }
      },
      { rootMargin: '200px' }
    )
    observer.observe(el)
  }

  onUnmounted(() => {
    observer?.disconnect()
  })

  return { list, total, loading, hasMore, loadMore, reset, observeSentinel, currentPage, invalidate }
}
