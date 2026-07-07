/**
 * Fabric.js 工具函数
 */
import { Canvas, FabricImage, Rect, type FabricObject } from 'fabric'

/** 将 canvas 上的裁切区域导出为 Blob */
export async function exportCropToBlob(
  canvas: Canvas,
  image: FabricImage,
  cropRect: Rect,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const cropLeft = cropRect.left || 0
    const cropTop = cropRect.top || 0
    const cropW = (cropRect.width || 0) * (cropRect.scaleX || 1)
    const cropH = (cropRect.height || 0) * (cropRect.scaleY || 1)

    const imgElement = (image as any).getElement?.() as HTMLImageElement
    if (!imgElement) {
      reject(new Error('Image element not found'))
      return
    }

    const imgLeft = image.left || 0
    const imgTop = image.top || 0
    const imgScaleX = image.scaleX || 1
    const imgScaleY = image.scaleY || 1

    const srcX = (cropLeft - imgLeft) / imgScaleX
    const srcY = (cropTop - imgTop) / imgScaleY
    const srcW = cropW / imgScaleX
    const srcH = cropH / imgScaleY

    const outCanvas = document.createElement('canvas')
    const dpr = window.devicePixelRatio || 1
    outCanvas.width = Math.round(srcW * dpr)
    outCanvas.height = Math.round(srcH * dpr)
    const ctx = outCanvas.getContext('2d')
    if (!ctx) {
      reject(new Error('Cannot get 2d context'))
      return
    }

    ctx.drawImage(imgElement, srcX, srcY, srcW, srcH, 0, 0, outCanvas.width, outCanvas.height)

    outCanvas.toBlob(
      (blob) => {
        if (blob) resolve(blob)
        else reject(new Error('Failed to create blob'))
      },
      'image/png',
    )
  })
}

/** 限制数值在范围内 */
export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

/** 生成唯一 ID */
export function generateId(): string {
  return crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}
