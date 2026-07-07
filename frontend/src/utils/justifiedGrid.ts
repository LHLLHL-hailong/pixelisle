import justifiedLayout from 'justified-layout'

export interface JustifiedGridItem {
  /** 图片宽度（自然像素） */
  width: number
  /** 图片高度（自然像素） */
  height: number
  /** 自定义数据（透传） */
  data: any
}

export interface JustifiedBox {
  top: number
  left: number
  width: number
  height: number
  aspectRatio: number
  data: any
}

export interface JustifiedGridConfig {
  /** 容器宽度 px */
  containerWidth: number
  /** 目标行高 px，默认 240 */
  targetRowHeight?: number
  /** 最大拉伸比例，默认 1.2 */
  maxStretch?: number
  /** 水平间距 px，默认 8 */
  horizontalSpacing?: number
  /** 垂直间距 px，默认 8 */
  verticalSpacing?: number
}

/**
 * 计算 Justified Grid 布局
 * 输入图片列表（含宽高），输出每个图片的定位信息
 */
export function computeJustifiedGrid(
  items: JustifiedGridItem[],
  config: JustifiedGridConfig
): { boxes: JustifiedBox[]; containerHeight: number } {
  const {
    containerWidth,
    targetRowHeight = 240,
    maxStretch = 1.2,
    horizontalSpacing = 8,
    verticalSpacing = 8,
  } = config

  if (!items.length || containerWidth <= 0) {
    return { boxes: [], containerHeight: 0 }
  }

  // 将 items 转为 justified-layout 的 input 格式
  // 对于没有宽高信息的图片，使用默认 aspect ratio
  const input = items.map((item) => ({
    width: item.width || 4,
    height: item.height || 3,
  }))

  try {
    const result = justifiedLayout(input, {
      containerWidth,
      targetRowHeight,
      maxStretch,
      boxSpacing: {
        horizontal: horizontalSpacing,
        vertical: verticalSpacing,
      },
    })

    const boxes: JustifiedBox[] = result.boxes.map((box: any, i: number) => ({
      top: box.top,
      left: box.left,
      width: box.width,
      height: box.height,
      aspectRatio: box.aspectRatio,
      data: items[i].data,
    }))

    return { boxes, containerHeight: result.containerHeight }
  } catch {
    // 容错：退化为简单的等高等宽网格
    return fallbackGrid(items, config)
  }
}

/**
 * 降级方案：等高等宽网格
 */
function fallbackGrid(
  items: JustifiedGridItem[],
  config: JustifiedGridConfig
): { boxes: JustifiedBox[]; containerHeight: number } {
  const { containerWidth, targetRowHeight = 240, horizontalSpacing = 8, verticalSpacing = 8 } = config
  const cols = Math.max(1, Math.floor(containerWidth / (targetRowHeight * 1.5)))
  const cellWidth = (containerWidth - horizontalSpacing * (cols - 1)) / cols

  const boxes: JustifiedBox[] = items.map((item, i) => {
    const row = Math.floor(i / cols)
    const col = i % cols
    return {
      top: row * (targetRowHeight + verticalSpacing),
      left: col * (cellWidth + horizontalSpacing),
      width: cellWidth,
      height: targetRowHeight,
      aspectRatio: cellWidth / targetRowHeight,
      data: item.data,
    }
  })

  const rows = Math.ceil(items.length / cols)
  const containerHeight = rows * targetRowHeight + (rows - 1) * verticalSpacing

  return { boxes, containerHeight }
}
