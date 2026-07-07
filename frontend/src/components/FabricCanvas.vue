<template>
  <div class="fabric-canvas-container" ref="containerRef">
    <canvas ref="canvasRef"></canvas>
    <div v-if="loading" class="canvas-loading">
      <span>图片加载中...</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Canvas, FabricImage, Rect, Point, IText, PencilBrush, util, type TPointerEventInfo } from 'fabric'

const props = defineProps<{
  imageUrl: string
  /** 是否可编辑（非编辑者只看不能动） */
  editable?: boolean
  /** 剪切框是否锁定（阻止本地拖拽，远程协同不受影响） */
  cropLocked?: boolean
}>()

const emit = defineEmits<{
  (e: 'ready'): void
  (e: 'cropChange', params: { left: number; top: number; width: number; height: number }): void
  /** 向父组件发出协同编辑事件 */
  (e: 'editEvent', msg: {
    type: string
    editAction: string
    targetId?: string
    params?: Record<string, unknown>
    cropParams?: Record<string, number>
    objectJSON?: string
    timestamp: number
  }): void
  /** 阶段5：选中文字时通知父组件显示文字工具栏 */
  (e: 'textSelected', info: { customId: string; fontSize: number; fill: string; fontWeight: string; fontStyle: string } | null): void
}>()

const containerRef = ref<HTMLDivElement>()
const canvasRef = ref<HTMLCanvasElement>()

let canvas: Canvas | null = null
let image: FabricImage | null = null
let cropRect: Rect | null = null
let isPanning = false
let lastPanPoint: Point | null = null
const loading = ref(true)  // ★ 图片加载状态
/** 当前选中的文字对象引用（避免失焦后 getActiveObject 返回 null） */
let selectedTextObj: any = null

/** 缓冲区：canvas 未就绪时暂存远程编辑消息，就绪后一次性回放 */
let pendingEdits: any[] = []

// ── 画笔状态 ──
let isDrawingMode = false
let isEraserMode = false
let drawingColor = '#ff0000'
let drawingWidth = 3

// ── 远程选中指示（targetId → 用户信息）──
const remoteSelections = new Map<string, { color: string; userName: string }>()

// ═══════════ 日志 ═══════════
function log(label: string, data: Record<string, unknown>) {
  console.log(`[FabricCanvas] ${label}:`, JSON.parse(JSON.stringify(data)))
}

// ═══════════ 初始化 ═══════════

function initCanvas() {
  if (!canvasRef.value || !containerRef.value) return
  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight || 400

  canvas = new Canvas(canvasRef.value, {
    width: w, height: h,
    backgroundColor: '#1a1a1a',
    selection: false,
    allowTouchScrolling: true,
  })

  canvas.on('mouse:wheel', onMouseWheel)
  canvas.on('mouse:down', onMouseDown)
  canvas.on('mouse:move', onMouseMove)
  canvas.on('mouse:up', onMouseUp)
  // ★ 阶段5：画笔路径创建
  canvas.on('path:created', onPathCreated)
  // ★ 阶段5e：对象选中通知
  canvas.on('object:selected', onObjectSelected)
  canvas.on('object:deselected', onObjectDeselected)
  // ★ 对象修改（移动/缩放结束）→ 协同同步（object:moving 太高频，不监听）
  canvas.on('object:modified', onObjectModified)
  window.addEventListener('resize', onResize)

  loadImage()
}

// ═══════════ 图片加载 ═══════════

function loadImage() {
  if (!canvas || !props.imageUrl) return
  if (image) { canvas.remove(image); image = null }
  if (cropRect) { canvas.remove(cropRect); cropRect = null }
  pendingEdits = []  // ★ 清空旧缓冲区（新图片开始加载）

  function doLoad(retryCount = 0) {
    const MAX_RETRIES = 2
    loading.value = true

    FabricImage.fromURL(props.imageUrl, { crossOrigin: 'anonymous' }).then((img) => {
      if (!canvas) { loading.value = false; return }
      image = img
      image.customId = 'main-image'

      const cw = canvas.getWidth(), ch = canvas.getHeight()
      const nw = img.width || cw, nh = img.height || ch
      const fit = Math.min(cw / nw, ch / nh, 1)

      img.set({
        scaleX: fit, scaleY: fit,
        left: cw / 2, top: ch / 2,
        selectable: false, evented: false,
        strokeWidth: 0,  // ★ Fabric.js 默认 strokeWidth=1，getBoundingRect() 会多算 1px
      })

      canvas.add(img)
      canvas.renderAll()

      const b = img.getBoundingRect()
      log('loadImage', { bounds_left: b.left, bounds_top: b.top, bounds_w: b.width, bounds_h: b.height,
        img_naturalW: nw, img_naturalH: nh, scaleX: fit, scaleY: fit, canvasW: cw, canvasH: ch })

      createCropRect()
      loading.value = false
      emit('ready')
    }).catch((err: Error) => {
      console.error(`[FabricCanvas] crossOrigin load failed (retry ${retryCount}/${MAX_RETRIES})`, err.message)
      if (retryCount < MAX_RETRIES) {
        // ★ 延时重试：CORS 可能因网络波动临时失败
        setTimeout(() => doLoad(retryCount + 1), 500 * (retryCount + 1))
      } else {
        // 最终兜底：无 crossOrigin（可显示但 toDataURL 会报 SecurityError）
        console.warn('[FabricCanvas] falling back to non-crossOrigin load')
        FabricImage.fromURL(props.imageUrl).then((img2) => {
          if (!canvas) { loading.value = false; return }
          image = img2
          image.customId = 'main-image'
          const cw = canvas.getWidth(), ch = canvas.getHeight()
          const nw = img2.width || cw, nh = img2.height || ch
          const fit = Math.min(cw / nw, ch / nh, 1)
          img2.set({ scaleX: fit, scaleY: fit, left: cw / 2, top: ch / 2, selectable: false, evented: false, strokeWidth: 0 })
          canvas.add(img2)
          canvas.renderAll()
          createCropRect()
          loading.value = false
          emit('ready')
        }).catch((err2: Error) => {
          console.error('[FabricCanvas] load failed after all retries', err2)
          loading.value = false
          emit('ready')
        })
      }
    })
  }
  doLoad()
}

// ═══════════ 裁切框锁定 ═══════════

/** 根据 editable + cropLocked 状态更新裁切框交互 */
function applyCropLock() {
  if (!cropRect) return
  const e = props.editable !== false
  const locked = props.cropLocked !== false // 默认锁定
  if (!e || locked) {
    cropRect.set({ selectable: false, evented: false, hasBorders: false, hasControls: false })
  } else {
    cropRect.set({ selectable: true, evented: true, hasBorders: true, hasControls: true })
  }
}

// ═══════════ 裁切框 ═══════════

function createCropRect() {
  if (!canvas || !image) return
  if (cropRect) canvas.remove(cropRect)

  // 无 clipPath → getBoundingRect = 完整图片视觉边界
  const b = image.getBoundingRect()
  const m = 0
  const cw = b.width * (1 - 2 * m)
  const ch = b.height * (1 - 2 * m)
  const cl = b.left + b.width * m
  const ct = b.top + b.height * m

  cropRect = new Rect({
    left: cl, top: ct, width: cw, height: ch,
    originX: 'left', originY: 'top',
    fill: 'rgba(255, 255, 255, 0.1)',
    stroke: '#39f', strokeWidth: 2, strokeDashArray: [6, 3],
    selectable: true, evented: true,
    hasBorders: true, hasControls: true,
    lockRotation: true, lockSkewingX: true, lockSkewingY: true,
    cornerColor: '#39f', cornerStrokeColor: '#fff',
    cornerSize: 10, cornerStyle: 'circle', transparentCorners: false,
    padding: 0, minScaleLimit: 0.01,
  })
  // 初始应用 editable 权限 + 锁定状态
  applyCropLock()
  cropRect.customId = 'crop-rect'

  // ★ 创建后立即验证位置
  console.log('[FabricCanvas] cropRect AFTER create:', {
    cropRect: { left: cropRect.left, top: cropRect.top, w: cropRect.width, h: cropRect.height, ox: cropRect.originX, oy: cropRect.originY },
    image: { left: image.left, top: image.top, ox: image.originX, oy: image.originY, scaleX: image.scaleX, scaleY: image.scaleY },
    imgBounds: b,
    margin: m,
    cropCreated: { left: cl, top: ct, w: cw, h: ch }
  })
  canvas.add(cropRect)

  cropRect.on('moving', onCropChanged)
  cropRect.on('scaling', onCropChanged)
  cropRect.on('modified', onCropChanged)

  canvas.setActiveObject(cropRect)
  canvas.renderAll()
  emitCropParams()

  // ★ 回放就绪前暂存的远程编辑消息
  if (pendingEdits.length > 0) {
    console.log(`[FabricCanvas] Flushing ${pendingEdits.length} pending edits`)
    const batch = pendingEdits
    pendingEdits = []
    for (const msg of batch) {
      applyRemoteEdit(msg)
    }
  }
}

// 上次远程应用的位置
let lastRemoteCrop = { left: -1, top: -1, width: -1, height: -1 }

function onCropChanged() {
  emitCropParams()
  const cb = cropRect?.getBoundingRect()
  log('onCropChanged', {
    cropBounds: cb,
    lastRemoteCrop,
    match: cb ? (
      Math.abs(cb.left - lastRemoteCrop.left) < 0.5 &&
      Math.abs(cb.top - lastRemoteCrop.top) < 0.5 &&
      Math.abs(cb.width - lastRemoteCrop.width) < 0.5 &&
      Math.abs(cb.height - lastRemoteCrop.height) < 0.5
    ) : 'no_cropRect',
  })
  if (cb && (
    Math.abs(cb.left - lastRemoteCrop.left) >= 0.5 ||
    Math.abs(cb.top - lastRemoteCrop.top) >= 0.5 ||
    Math.abs(cb.width - lastRemoteCrop.width) >= 0.5 ||
    Math.abs(cb.height - lastRemoteCrop.height) >= 0.5
  )) {
    console.log('[FabricCanvas] EMITTING editEvent', getCropMetrics())
    emit('editEvent', {
      type: 'CROP_CHANGE',
      editAction: 'CROP_CHANGE',
      targetId: 'crop-rect',
      cropParams: getCropMetrics(),
      timestamp: Date.now(),
    })
  }
}

function emitCropParams() {
  if (!cropRect || !image) return

  // 无 clipPath → getBoundingRect = 完整图片边界
  const imgB = image.getBoundingRect()
  const cropB = cropRect.getBoundingRect()

  log('emitCropParams', {
    imgBounds: { left: imgB.left, top: imgB.top, w: imgB.width, h: imgB.height },
    cropBounds: { left: cropB.left, top: cropB.top, w: cropB.width, h: cropB.height },
  })

  emit('cropChange', {
    left: cropB.left - imgB.left,
    top: cropB.top - imgB.top,
    width: cropB.width,
    height: cropB.height,
  })
}

// ═══════════ 缩放 & 平移 ═══════════

let zoomEmitTimer: ReturnType<typeof setTimeout> | null = null

function onMouseWheel(opt: TPointerEventInfo<WheelEvent>) {
  if (!canvas) return
  opt.e.preventDefault(); opt.e.stopPropagation()
  let zoom = canvas.getZoom()
  zoom *= 0.999 ** opt.e.deltaY
  zoom = Math.max(0.1, Math.min(10, zoom))
  canvas.zoomToPoint(new Point(opt.e.offsetX, opt.e.offsetY), zoom)
  canvas.renderAll()

  // ★ Zoom 不同步：各端视口独立（保留 emit 调用骨架，日后如需恢复解开注释即可）
  // if (zoomEmitTimer) clearTimeout(zoomEmitTimer)
  // zoomEmitTimer = setTimeout(() => {
  //   emit('editEvent', {
  //     type: 'CANVAS_ZOOM',
  //     editAction: 'CANVAS_ZOOM',
  //     params: { zoom: canvas?.getZoom() },
  //     timestamp: Date.now(),
  //   })
  // }, 300)
}

function onMouseDown(opt: TPointerEventInfo<MouseEvent>) {
  if (!canvas) return
  if (isDrawingMode) return
  if (isEraserMode) return   // ★ 橡皮模式：仅 onEraserClick 响应，不触发平移
  const t = opt.target as any
  // ★ 点击标注对象（文字/路径）→ 直接设为 activeObject + 手动更新工具栏
  if (t?.customId && t.customId !== 'main-image' && t.customId !== 'crop-rect') {
    canvas.setActiveObject(t)
    canvas.renderAll()
    // 手动触发文字选中回调（绕过 Fabric.js object:selected 可能不触发的问题）
    if (t.type === 'i-text' || t.type === 'textbox') {
      emit('textSelected', {
        customId: t.customId as string,
        fontSize: t.fontSize || 20,
        fill: t.fill || '#ffffff',
        fontWeight: t.fontWeight || 'normal',
        fontStyle: t.fontStyle || 'normal',
      })
    }
    return
  }
  // ★ 点击剪切框 → 让 Fabric 处理（拖拽/缩放），不启动平移
  if (t?.customId === 'crop-rect') return
  // ★ 点击空白区域 → 取消所有选中 + 启动平移
  canvas.discardActiveObject()
  canvas.renderAll()
  // 手动触发取消选中
  emit('textSelected', null)
  isPanning = true
  lastPanPoint = new Point(opt.e.clientX, opt.e.clientY)
  canvas.setCursor('grabbing')
}

function onMouseMove(opt: TPointerEventInfo<MouseEvent>) {
  if (isDrawingMode || !isPanning || !canvas || !lastPanPoint) return
  const np = new Point(opt.e.clientX, opt.e.clientY)
  const d = np.subtract(lastPanPoint)
  const vpt = canvas.viewportTransform
  if (vpt) { vpt[4] += d.x; vpt[5] += d.y; canvas.requestRenderAll() }
  lastPanPoint = np
}

function onMouseUp() {
  if (!canvas) return
  if (isDrawingMode) return
  isPanning = false; lastPanPoint = null
  if (!isEraserMode) {
    canvas.setCursor(isDrawingMode ? 'crosshair' : 'default')
  }
}

function onResize() {
  if (!canvas || !containerRef.value) return
  const { clientWidth: w, clientHeight: h } = containerRef.value
  canvas.setDimensions({ width: w, height: h || 400 })
  canvas.renderAll()
}

// ═══════════ 公开方法 ═══════════

function changeScale(delta: number) {
  if (!canvas) return
  let z = canvas.getZoom()
  z += delta * 0.05
  z = Math.max(0.1, Math.min(10, z))
  canvas.zoomToPoint(new Point(canvas.getWidth() / 2, canvas.getHeight() / 2), z)
  canvas.renderAll()
}

function rotateLeft() {
  if (!image || !canvas) return
  image.rotate((image.angle || 0) - 90)
  canvas.renderAll()
}

function rotateRight() {
  if (!image || !canvas) return
  image.rotate((image.angle || 0) + 90)
  canvas.renderAll()
}

function reset() {
  if (!image || !canvas) return
  image.set({ angle: 0, scaleX: 1, scaleY: 1 })
  canvas.setZoom(1)
  canvas.setViewportTransform([1, 0, 0, 1, 0, 0])
  loadImage()
}

/** 获取裁切框参数（相对坐标——解决多端 zoom/pan 不一致问题） */
function getCropMetrics(): Record<string, number> {
  if (!cropRect || !image) return {}
  const cb = cropRect.getBoundingRect()
  const ib = image.getBoundingRect()
  if (ib.width === 0 || ib.height === 0) return {}
  // ★ 相对坐标：不受各端 zoom/pan 影响
  return {
    relLeft: (cb.left - ib.left) / ib.width,
    relTop: (cb.top - ib.top) / ib.height,
    relWidth: cb.width / ib.width,
    relHeight: cb.height / ib.height,
  }
}

/** 从相对坐标还原绝对位置 */
function relToAbsolute(params: any, fallback?: any) {
  const ib = image?.getBoundingRect()
  if (ib && ib.width > 0 && ib.height > 0 && params?.relLeft !== undefined) {
    return {
      left: ib.left + params.relLeft * ib.width,
      top: ib.top + params.relTop * ib.height,
    }
  }
  // 兜底：用绝对坐标
  return { left: params?.left ?? fallback?.left ?? 0, top: params?.top ?? fallback?.top ?? 0 }
}

function applyRelPosition(params: any, obj: any) {
  const ib = image?.getBoundingRect()
  if (ib && ib.width > 0 && ib.height > 0 && params.relLeft !== undefined) {
    obj.set({
      left: ib.left + params.relLeft * ib.width,
      top: ib.top + params.relTop * ib.height,
    })
    obj.setCoords()
  }
}

/** 从远程消息还原编辑操作 */
function applyRemoteEdit(msg: any) {
  // ★ canvas 未就绪 → 暂存到缓冲区，等就绪后回放
  if (!canvas || !image || !cropRect) {
    pendingEdits.push(msg)
    return
  }
  const a = msg.editAction
  // ★ SYNC_HISTORY 消息没有 editAction，直接跳过
  if (!a) return
  const p = msg.params || {}
  const cp = msg.cropParams || {}

  switch (a) {
    case 'ZOOM_IN':
      changeScale(1)
      break
    case 'ZOOM_OUT':
      changeScale(-1)
      break
    case 'ROTATE_LEFT':
      rotateLeft()
      break
    case 'ROTATE_RIGHT':
      rotateRight()
      break
    case 'CROP_CHANGE':
    case 'CROP_MOVE':
      if (cropRect && cp.relLeft !== undefined && image) {
        const ib = image.getBoundingRect()
        const targetLeft = ib.left + cp.relLeft * ib.width
        const targetTop = ib.top + cp.relTop * ib.height
        // stroke=2 → 左右各1px，实际box比width大2px
        const targetWidth = cp.relWidth * ib.width - 2
        const targetHeight = cp.relHeight * ib.height - 2

        console.log('[FabricCanvas] REMOTE target=', { l: targetLeft, t: targetTop, w: targetWidth, h: targetHeight }, 'ib=', { left: ib.left, top: ib.top, w: ib.width, h: ib.height })

        // ★ 用 setPositionByOrigin 强制按左上角定位（无视 originX/originY）
        cropRect.setPositionByOrigin(
          new Point(targetLeft, targetTop),
          'left', 'top'
        )
        cropRect.set({ width: targetWidth, height: targetHeight, scaleX: 1, scaleY: 1 })
        cropRect.setCoords()
        canvas.renderAll()

        // 更新 lastRemoteCrop（用于 onCropChanged 比对防反馈环）
        const after = cropRect.getBoundingRect()
        lastRemoteCrop = { left: after.left, top: after.top, width: after.width, height: after.height }
        console.log('[FabricCanvas] AFTER SET bounds=', { left: after.left, top: after.top, w: after.width, h: after.height })
      }
      break
    case 'CANVAS_ZOOM':
      if (p.zoom) {
        canvas.setZoom(p.zoom)
        canvas.renderAll()
      }
      break

    // ── 对象修改（文字移动/内容变更、路径移动）──
    case 'OBJECT_MODIFIED':
    case 'OBJECT_MOVING': {
      const modTargetId = msg.targetId
      if (!modTargetId) break
      const modObj = canvas.getObjects().find((o: any) => o.customId === modTargetId)
      if (!modObj) break
      if (msg.objectJSON) {
        try {
          const j = typeof msg.objectJSON === 'string' ? JSON.parse(msg.objectJSON) : msg.objectJSON
          const p = msg.params || {}
          // ★ 用相对坐标还原位置（解决各端画布尺寸不同）
          const pos = relToAbsolute(p, j)
          if (modObj.type === 'i-text' || modObj.type === 'textbox') {
            modObj.set({ text: j.text, fontSize: j.fontSize, fill: j.fill, fontWeight: j.fontWeight, fontStyle: j.fontStyle, left: pos.left, top: pos.top })
          } else {
            modObj.set({ left: pos.left, top: pos.top, scaleX: j.scaleX, scaleY: j.scaleY, angle: j.angle || 0 })
          }
          modObj.setCoords()
          canvas.renderAll()
        } catch (err) { console.error('[FabricCanvas] OBJECT_MODIFIED parse failed', err) }
      } else if (msg.params) {
        // ★ 相对坐标还原
        applyRelPosition(msg.params, modObj)
        modObj.setCoords()
        canvas.renderAll()
      }
      break
    }

    // ── 阶段5：对象新增/删除 ──
    case 'DRAW_PATH':
    case 'ADD_TEXT':
    case 'ADD_RECT':
    case 'OBJECT_ADDED': {
      if (!msg.objectJSON) break
      try {
        const json = typeof msg.objectJSON === 'string' ? JSON.parse(msg.objectJSON) : msg.objectJSON
        util.enlivenObjects([json]).then((objects: any[]) => {
          if (!canvas) return
          const e = props.editable !== false
          objects.forEach((obj: any) => {
            if (obj) {
              obj.customId = msg.targetId || json.customId
              if (!e) {
                obj.set({ selectable: false, evented: false, hasControls: false, hasBorders: false, editable: false })
              }
              canvas!.add(obj)
              // ★ 用相对坐标还原位置
              if (msg.params) applyRelPosition(msg.params, obj)
              console.log(`[FabricCanvas] Remote added: ${obj.type} id=${obj.customId}`)
            }
          })
          canvas.renderAll()
        }).catch((err: Error) => console.error('[FabricCanvas] enlivenObjects failed', err))
      } catch (err) {
        console.error('[FabricCanvas] OBJECT_ADDED parse failed', err)
      }
      break
    }
    case 'DELETE_OBJECT':
    case 'OBJECT_REMOVED': {
      const targetId = msg.targetId
      if (targetId) {
        const obj = canvas.getObjects().find((o: any) => o.customId === targetId)
        if (obj) { canvas.remove(obj); canvas.renderAll() }
      }
      break
    }

    // ── 阶段5e：远程选中指示 ──
    case 'OBJECT_SELECTED': {
      const selTargetId = msg.targetId
      const selUser = msg.user
      if (selTargetId && selUser) {
        const color = getUserColor(selUser.id || selUser.userName)
        const obj = canvas.getObjects().find((o: any) => o.customId === selTargetId)
        if (obj) {
          obj.set({ stroke: color, strokeWidth: 3 })
          canvas.renderAll()
          remoteSelections.set(selTargetId, { color, userName: selUser.userName || '其他用户' })
        }
      }
      break
    }
    case 'OBJECT_DESELECTED': {
      const deselTargetId = msg.targetId
      if (deselTargetId) {
        remoteSelections.delete(deselTargetId)
        const obj = canvas.getObjects().find((o: any) => o.customId === deselTargetId)
        if (obj) {
          obj.set({ stroke: obj.originalStroke || '', strokeWidth: obj.originalStrokeWidth || 0 })
          canvas.renderAll()
        }
      }
      break
    }

    // ── 阶段5c：滤镜 ──
    case 'APPLY_FILTER': {
      const ft = p?.filter
      const el = canvas.getElement()
      if (ft === 'none') {
        el.style.filter = ''
      } else if (ft) {
        const cssMap: Record<string, string> = {
          grayscale: 'grayscale(100%)',
          sepia: 'sepia(100%)',
          invert: 'invert(100%)',
          blur: 'blur(3px)',
          sharpen: 'contrast(150%) brightness(110%)',
        }
        el.style.filter = cssMap[ft] || ''
      }
      canvas.renderAll()
      break
    }

    // ── 阶段6：服务端快照恢复（新用户加入时一次性同步整个画布状态）──
    case 'SNAPSHOT': {
      if (!msg.objectJSON) break
      try {
        const snap = typeof msg.objectJSON === 'string' ? JSON.parse(msg.objectJSON) : msg.objectJSON
        // 1. 清除旧的标注对象（保留主图和剪切框）
        const toRemove: any[] = []
        canvas.getObjects().forEach((o: any) => {
          if (o.customId && o.customId !== 'main-image' && o.customId !== 'crop-rect') {
            toRemove.push(o)
          }
        })
        toRemove.forEach((o: any) => canvas.remove(o))

        // 2. 恢复滤镜
        if (snap.filter) {
          const cssMap: Record<string, string> = {
            grayscale: 'grayscale(100%)', sepia: 'sepia(100%)', invert: 'invert(100%)',
            blur: 'blur(3px)', sharpen: 'contrast(150%) brightness(110%)',
          }
          canvas.getElement().style.filter = cssMap[snap.filter] || ''
        }

        // 3. 恢复剪切框
        if (snap.crop && cropRect && image) {
          const cp = snap.crop
          const ib = image.getBoundingRect()
          cropRect.setPositionByOrigin(
            new Point(ib.left + cp.relLeft * ib.width, ib.top + cp.relTop * ib.height),
            'left', 'top'
          )
          cropRect.set({ width: cp.relWidth * ib.width - 2, height: cp.relHeight * ib.height - 2, scaleX: 1, scaleY: 1 })
          cropRect.setCoords()
        }

        // 4. 恢复标注对象
        const objects = snap.objects || []
        if (objects.length > 0) {
          const objDataList = objects.map((entry: any) => {
            const obj = typeof entry.data === 'string' ? JSON.parse(entry.data) : entry.data
            obj.customId = entry.id
            return obj
          })
          util.enlivenObjects(objDataList).then((enlivened: any[]) => {
            if (!canvas) return
            const e = props.editable !== false
            for (const obj of enlivened) {
              if (!obj) continue
              if (!e) {
                obj.set({ selectable: false, evented: false, hasControls: false, hasBorders: false, editable: false })
              }
              canvas!.add(obj)
            }
            canvas.renderAll()
            console.log(`[FabricCanvas] Snapshot restored: ${objects.length} objects`)
          }).catch((err: Error) => console.error('[FabricCanvas] SNAPSHOT enliven failed', err))
        } else {
          canvas.renderAll()
        }
      } catch (err) {
        console.error('[FabricCanvas] SNAPSHOT restore failed', err)
      }
      break
    }

    default:
      log('applyRemoteEdit: unknown action', { action: a, msg })
  }
}

async function getCropBlob(): Promise<Blob> {
  return new Promise((resolve, reject) => {
    if (!image || !cropRect || !canvas) { reject(new Error('Not ready')); return }

    const imgEl = (image as any).getElement?.() as HTMLImageElement
    if (!imgEl) { reject(new Error('No image element')); return }

    const imgB = image.getBoundingRect()
    // ★ 用 cropRect 自身属性而非 getBoundingRect()：后者包含 strokeWidth 描边区域导致多裁
    const crLeft = cropRect.left
    const crTop = cropRect.top
    const crWidth = cropRect.width * cropRect.scaleX!
    const crHeight = cropRect.height * cropRect.scaleY!

    // ★ 诊断日志：对比 cropRect 自身属性 vs getBoundingRect vs 图片边界
    const cropBB = cropRect.getBoundingRect()
    console.log('[getCropBlob] cropRect.self:', { l: crLeft, t: crTop, w: crWidth, h: crHeight, sx: cropRect.scaleX, sy: cropRect.scaleY, ox: cropRect.originX, oy: cropRect.originY })
    console.log('[getCropBlob] cropRect.bbox:', { l: cropBB.left, t: cropBB.top, w: cropBB.width, h: cropBB.height })
    console.log('[getCropBlob] image.bbox  :', { l: imgB.left, t: imgB.top, w: imgB.width, h: imgB.height })
    console.log('[getCropBlob] viewportTfm:', canvas?.viewportTransform)

    log('getCropBlob', {
      imgBounds: { left: imgB.left, top: imgB.top, w: imgB.width, h: imgB.height },
      cropRects: { left: crLeft, top: crTop, w: crWidth, h: crHeight },
      img_naturalW: imgEl.naturalWidth, img_naturalH: imgEl.naturalHeight,
    })

    const cssFilter = canvas.getElement().style.filter
    const multiplier = Math.min(imgEl.naturalWidth / imgB.width, 4) // cap 4x 防超分辨率

    // ★ 导出前重置 viewportTransform：用户缩放/平移后 toDataURL 会偏移裁剪坐标
    const savedVpt = canvas.viewportTransform ? [...canvas.viewportTransform] : null
    if (savedVpt) {
      canvas.setViewportTransform([1, 0, 0, 1, 0, 0])
      canvas.renderAll()
    }

    // 导出前隐藏裁切框
    cropRect.set({ visible: false })
    canvas.renderAll()

    const dataURL = canvas.toDataURL({
      format: 'webp',
      quality: 1.0,
      left: crLeft,
      top: crTop,
      width: crWidth,
      height: crHeight,
      multiplier,
    })

    cropRect.set({ visible: true })
    // ★ 恢复 viewportTransform
    if (savedVpt) {
      canvas.setViewportTransform(savedVpt)
    }
    canvas.renderAll()

    // 如果有 CSS 滤镜 → 加载导出图像，用 ctx.filter 重绘
    if (cssFilter) {
      const img = new Image()
      img.src = dataURL
      img.onload = () => {
        const out = document.createElement('canvas')
        out.width = img.width; out.height = img.height
        const outCtx = out.getContext('2d')!
        outCtx.filter = cssFilter
        outCtx.drawImage(img, 0, 0)
        outCtx.filter = 'none'
        out.toBlob((blob) => {
          if (blob) resolve(blob)
          else reject(new Error('Blob failed'))
        }, 'image/webp', 1.0)
      }
      img.onerror = () => reject(new Error('Image load failed'))
    } else {
      fetch(dataURL).then(r => r.blob()).then(resolve).catch(reject)
    }
  })
}

// ═══════════ 阶段5：画笔 ═══════════

function toggleDrawing(enabled: boolean, color?: string, width?: number) {
  if (!canvas) return
  if (color) drawingColor = color
  if (width) drawingWidth = width
  isDrawingMode = enabled

  canvas.isDrawingMode = enabled
  if (enabled) {
    canvas.freeDrawingBrush = new PencilBrush(canvas)
    canvas.freeDrawingBrush.color = drawingColor
    canvas.freeDrawingBrush.width = drawingWidth
    canvas.setCursor('crosshair')
    if (cropRect) cropRect.set({ selectable: false, evented: false, hasBorders: false, hasControls: false })
  } else {
    canvas.setCursor('default')
    applyCropLock()
  }
  canvas.renderAll()
}

function setDrawingColor(color: string) {
  drawingColor = color
  if (canvas?.freeDrawingBrush) {
    canvas.freeDrawingBrush.color = color
  }
}

function setDrawingWidth(width: number) {
  drawingWidth = width
  if (canvas?.freeDrawingBrush) {
    canvas.freeDrawingBrush.width = width
  }
}

// ── 橡皮擦：滑动擦除 ──
let erasedObjectIds = new Set<string>()

function toggleEraser(enabled: boolean) {
  if (!canvas) return
  isEraserMode = enabled
  erasedObjectIds = new Set()
  if (enabled) {
    canvas.isDrawingMode = true
    canvas.freeDrawingBrush = new PencilBrush(canvas)
    canvas.freeDrawingBrush.color = 'rgba(180,180,180,0.5)'
    canvas.freeDrawingBrush.width = 16
    canvas.setCursor('cell')
    canvas.on('path:created', onEraserPathCreated)
    canvas.on('mouse:move', onEraserSwipe)
  } else {
    canvas.isDrawingMode = false
    canvas.setCursor(isDrawingMode ? 'crosshair' : 'default')
    if (isDrawingMode) canvas.isDrawingMode = true
    canvas.off('path:created', onEraserPathCreated)
    canvas.off('mouse:move', onEraserSwipe)
  }
}

function onEraserSwipe(opt: any) {
  if (!isEraserMode || !canvas) return
  // Fabric.js v7: 使用 scenePoint（画布逻辑坐标）做碰撞检测
  const p = opt.scenePoint || opt.viewportPoint
  if (!p) return
  const all = canvas.getObjects()
  for (const o of all) {
    const obj = o as any
    if (!obj.customId || obj.customId === 'main-image' || obj.customId === 'crop-rect') continue
    if (erasedObjectIds.has(obj.customId)) continue
    const bounds = obj.getBoundingRect()
    if (p.x >= bounds.left && p.x <= bounds.left + bounds.width &&
        p.y >= bounds.top && p.y <= bounds.top + bounds.height) {
      erasedObjectIds.add(obj.customId as string)
    }
  }
}

function onEraserPathCreated(e: any) {
  if (!isEraserMode || !canvas) return
  const eraserPath = e.path
  console.log('[Eraser] swipe ended, removing', erasedObjectIds.size, 'objects')
  for (const id of erasedObjectIds) {
    const obj = canvas.getObjects().find((o: any) => o.customId === id)
    if (obj) {
      emitEditEvent('OBJECT_REMOVED', 'DELETE_OBJECT', id, undefined)
      canvas.remove(obj)
    }
  }
  canvas.remove(eraserPath)
  erasedObjectIds = new Set()
  canvas.renderAll()
}

// ═══════════ 阶段5：文字标注 ═══════════

function addText(textStr?: string) {
  if (!canvas || !image) return
  // ★ 非编辑者不能添加文字
  if (props.editable === false) return
  const txt = textStr || '双击编辑文字'
  const editable = props.editable !== false
  const text = new IText(txt, {
    left: canvas.getWidth() / 2,
    top: canvas.getHeight() / 2,
    fontSize: 24,
    fill: '#ffffff',
    fontFamily: 'sans-serif',
    editable,
    selectable: editable,
    evented: editable,
  })
  text.customId = 'text-' + Date.now()
  canvas.add(text)
  canvas.setActiveObject(text)
  canvas.renderAll()

  // ★ 直接绑定引用（selection=false 时 object:selected 可能不触发）
  selectedTextObj = text
  // 手动触发文字工具栏
  emitTextSelection(text)

  // 发送新增消息
  emitEditEvent('OBJECT_ADDED', 'ADD_TEXT', text.customId as string, text.toJSON())

  // 内容变更（双击编辑完成）
  text.on('editing:exited', () => {
    emitEditEvent('OBJECT_MODIFIED', 'OBJECT_MODIFIED', text.customId as string, text.toJSON())
  })
}

// ═══════════ 阶段5：滤镜 ═══════════

function applyFilter(filterType: string) {
  if (!canvas) return
  const cssMap: Record<string, string> = {
    grayscale: 'grayscale(100%)',
    sepia: 'sepia(100%)',
    invert: 'invert(100%)',
    blur: 'blur(3px)',
    sharpen: 'contrast(150%) brightness(110%)',
  }
  canvas.getElement().style.filter = cssMap[filterType] || ''
  canvas.renderAll()
  emitEditEvent('OBJECT_MODIFIED', 'APPLY_FILTER', 'main-image', { filter: filterType })
}

function removeFilters() {
  if (!canvas) return
  canvas.getElement().style.filter = ''
  canvas.renderAll()
  emitEditEvent('OBJECT_MODIFIED', 'APPLY_FILTER', 'main-image', { filter: 'none' })
}

// ═══════════ 阶段5：标注清除 ═══════════

function clearAnnotations() {
  if (!canvas) return
  const toRemove: any[] = []
  canvas.getObjects().forEach((obj: any) => {
    if (obj.customId && obj.customId !== 'crop-rect' && obj.customId !== 'main-image') {
      toRemove.push(obj)
      emitEditEvent('OBJECT_REMOVED', 'DELETE_OBJECT', obj.customId, undefined)
    }
  })
  toRemove.forEach((obj: any) => canvas!.remove(obj))
  canvas.renderAll()
}

// ═══════════ 画笔事件 ═══════════

// ── 画笔计数 ──
let pathCreateCount = 0

function onPathCreated(e: any) {
  if (!isDrawingMode) return
  const path = e.path
  if (!path) return
  pathCreateCount++
  path.customId = 'path-' + Date.now() + '-' + Math.random().toString(36).slice(2, 6)
  console.log(`[FabricCanvas] onPathCreated #${pathCreateCount} id=${path.customId}, points=${path.path?.length || '?'}`)
  emitEditEvent('OBJECT_ADDED', 'DRAW_PATH', path.customId as string, path.toJSON())
}

// ═══════════ 选中事件（阶段5e） ═══════════

function onObjectSelected(e: any) {
  const obj = e.selected?.[0] || e.target
  if (!obj?.customId || obj.customId === 'main-image' || obj.customId === 'crop-rect') return
  console.log('[FabricCanvas] object:selected type=', obj.type, 'customId=', obj.customId)
  emit('editEvent', {
    type: 'OBJECT_SELECTED',
    editAction: 'OBJECT_SELECTED',
    targetId: obj.customId as string,
    timestamp: Date.now(),
  })
  // ★ 文字对象 → 通知父组件显示文字工具栏
  if (obj.type === 'i-text' || obj.type === 'textbox') {
    selectedTextObj = obj
    console.log('[FabricCanvas] selectedTextObj updated to', obj.customId, 'fill=', obj.fill, 'size=', obj.fontSize)
    emit('textSelected', {
      customId: obj.customId as string,
      fontSize: obj.fontSize || 20,
      fill: obj.fill || '#ffffff',
      fontWeight: obj.fontWeight || 'normal',
      fontStyle: obj.fontStyle || 'normal',
    })
  }
}

function onObjectModified(e: any) {
  const obj = e.target
  if (!obj?.customId || obj.customId === 'main-image' || obj.customId === 'crop-rect') return
  // 文字/路径 → 发全量 JSON（含内容/样式/位置）
  if (obj.type === 'i-text' || obj.type === 'textbox' || obj.type === 'path') {
    emitEditEvent('OBJECT_MODIFIED', 'OBJECT_MODIFIED', obj.customId as string, obj.toJSON())
  }
}

function onObjectDeselected(e: any) {
  const obj = e.deselected?.[0] || e.target
  if (!obj?.customId || obj.customId === 'main-image' || obj.customId === 'crop-rect') return
  emit('editEvent', {
    type: 'OBJECT_DESELECTED',
    editAction: 'OBJECT_DESELECTED',
    targetId: obj.customId as string,
    timestamp: Date.now(),
  })
  // 文字取消选中 → 隐藏工具栏
  if (obj.type === 'i-text' || obj.type === 'textbox') {
    selectedTextObj = null
    emit('textSelected', null)
  }
}

// ═══════════ 用户颜色映射 ═══════════

const userColorMap = new Map<string, string>()
const USER_COLORS = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', '#a55eea', '#26de81', '#fd9644', '#2bcbba']

function getUserColor(key: string): string {
  if (!userColorMap.has(key)) {
    userColorMap.set(key, USER_COLORS[userColorMap.size % USER_COLORS.length])
  }
  return userColorMap.get(key)!
}

// ═══════════ 辅助：发送编辑事件 ═══════════

function emitEditEvent(type: string, action: string, targetId?: string, data?: any) {
  let objectJSON: string | undefined
  if (data) {
    try { objectJSON = JSON.stringify(data) } catch (_) { /* ignore */ }
  }
  const params: any = data && typeof data === 'object' && !Array.isArray(data) ? { ...data } : undefined
  // ★ 注入相对坐标：解决各端画布尺寸不同导致绝对坐标错位
  if (params && typeof params.left === 'number' && image) {
    const ib = image.getBoundingRect()
    if (ib.width > 0 && ib.height > 0) {
      params.relLeft = (params.left - ib.left) / ib.width
      params.relTop = (params.top - ib.top) / ib.height
    }
  }
  emit('editEvent', {
    type, editAction: action, targetId,
    params,
    objectJSON,
    timestamp: Date.now(),
  })
}

// ═══════════ 生命周期 ═══════════

onMounted(() => nextTick(() => initCanvas()))
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  canvas?.dispose(); canvas = null
})
watch(() => props.imageUrl, () => { if (canvas && props.imageUrl) loadImage() })
watch(() => props.editable, () => {
  if (!cropRect || !canvas) return
  applyCropLock()
  const e = props.editable !== false
  canvas.getObjects().forEach((obj: any) => {
    if (obj.customId && obj.customId !== 'crop-rect' && obj.customId !== 'main-image') {
      obj.set({ selectable: e, evented: e, hasControls: e, hasBorders: e, editable: e })
    }
  })
  canvas.renderAll()
})
watch(() => props.cropLocked, () => {
  if (!cropRect || !canvas) return
  applyCropLock()
  canvas.renderAll()
})

// ═══════════ 阶段5：文字属性操作 ═══════════

/** 获取当前选中的文字对象（直接用 Fabric.js 的 activeObject） */
function getSelectedText(): any {
  const ao = canvas?.getActiveObject()
  if (ao && (ao.type === 'i-text' || ao.type === 'textbox')) return ao
  return null
}

function setTextFontSize(delta: number) {
  const text = getSelectedText()
  if (!text) return
  const newSize = Math.max(8, Math.min(128, (text.fontSize || 20) + delta))
  text.set('fontSize', newSize)
  canvas?.renderAll()
  emitTextUpdate(text)
  emitTextSelection(text)
}

function setTextColor(color: string) {
  const text = getSelectedText()
  if (!text) return
  text.set('fill', color)
  canvas?.renderAll()
  emitTextUpdate(text)
  emitTextSelection(text)
}

function toggleTextBold() {
  const text = getSelectedText()
  if (!text) return
  text.set('fontWeight', text.fontWeight === 'bold' ? 'normal' : 'bold')
  canvas?.renderAll()
  emitTextUpdate(text)
  emitTextSelection(text)
}

function toggleTextItalic() {
  const text = getSelectedText()
  if (!text) return
  text.set('fontStyle', text.fontStyle === 'italic' ? 'normal' : 'italic')
  canvas?.renderAll()
  emitTextUpdate(text)
  emitTextSelection(text)
}

function deleteSelectedObject() {
  if (!canvas) return
  const ao = canvas.getActiveObject()
  if (!ao?.customId || ao.customId === 'main-image' || ao.customId === 'crop-rect') return
  emitEditEvent('OBJECT_REMOVED', 'DELETE_OBJECT', ao.customId as string, undefined)
  canvas.remove(ao)
  canvas.renderAll()
  // 文字删除后隐藏工具栏
  if (ao.type === 'i-text' || ao.type === 'textbox') {
    selectedTextObj = null
    emit('textSelected', null)
  }
}

function emitTextUpdate(text: any) {
  emitEditEvent('OBJECT_MODIFIED', 'OBJECT_MODIFIED', text.customId as string, text.toJSON())
}

function emitTextSelection(text: any) {
  emit('textSelected', {
    customId: text.customId as string,
    fontSize: text.fontSize || 20,
    fill: text.fill || '#ffffff',
    fontWeight: text.fontWeight || 'normal',
    fontStyle: text.fontStyle || 'normal',
  })
}

defineExpose({
  changeScale, rotateLeft, rotateRight, reset, getCropBlob, applyRemoteEdit,
  toggleDrawing, toggleEraser, setDrawingColor, setDrawingWidth, addText, applyFilter,
  removeFilters, clearAnnotations,
  setTextFontSize, setTextColor, toggleTextBold, toggleTextItalic, deleteSelectedObject,
})
</script>

<style scoped>
.fabric-canvas-container {
  width: 100%; height: 500px;
  overflow: hidden; border-radius: 4px;
  background: #1a1a1a;
  position: relative;
}
.fabric-canvas-container canvas { display: block; }
.canvas-loading {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(26, 26, 26, 0.9);
  color: #aaa; font-size: 14px; z-index: 10;
}
</style>
