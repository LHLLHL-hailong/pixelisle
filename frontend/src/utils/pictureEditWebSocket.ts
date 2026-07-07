export default class PictureEditWebSocket {
  private pictureId: number
  private socket: WebSocket | null
  private eventHandlers: any
  private reconnectAttempts = 0
  private readonly maxReconnectAttempts = 5
  private readonly baseReconnectDelay = 1000
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private intentionalClose = false

  constructor(pictureId: number) {
    this.pictureId = pictureId // 当前编辑的图片 ID
    this.socket = null // WebSocket 实例
    this.eventHandlers = {} // 自定义事件处理器
  }

  /**
   * 初始化 WebSocket 连接
   */
  connect() {
    this.intentionalClose = false
    const url = `ws://${window.location.host}/api/ws/picture/edit?pictureId=${this.pictureId}`
    this.socket = new WebSocket(url)

    // 设置携带 cookie
    this.socket.binaryType = 'blob'

    // 监听连接成功事件
    this.socket.onopen = () => {
      console.log('WebSocket 连接已建立')
      if (this.reconnectAttempts > 0) {
        this.reconnectAttempts = 0
        this.triggerEvent('reconnected')
      }
      this.triggerEvent('open')
    }

    // 监听消息事件
    this.socket.onmessage = (event) => {
      const message = JSON.parse(event.data)
      console.log('收到消息:', message)

      // 根据消息类型触发对应事件
      const type = message.type
      this.triggerEvent(type, message)
    }

    // 监听连接关闭事件
    this.socket.onclose = (event) => {
      console.log('WebSocket 连接已关闭 — code:', event.code, 'reason:', event.reason, 'wasClean:', event.wasClean)
      this.triggerEvent('close', event)
      this.tryReconnect()
    }

    // 监听错误事件
    this.socket.onerror = (error) => {
      console.error('WebSocket 发生错误:', error)
      this.triggerEvent('error', error)
      // onerror 之后通常会跟着 onclose，由 onclose 统一触发重连
    }
  }

  /**
   * 关闭 WebSocket 连接（主动关闭，不触发重连）
   */
  disconnect() {
    this.intentionalClose = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.socket) {
      this.socket.close()
      console.log('WebSocket 连接已手动关闭')
    }
  }

  /**
   * 尝试自动重连（指数退避）
   */
  private tryReconnect() {
    if (this.intentionalClose) return
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('WebSocket 重连次数已达上限（' + this.maxReconnectAttempts + '次）')
      this.triggerEvent('reconnectFailed')
      return
    }
    this.reconnectAttempts++
    // 指数退避: 1s → 2s → 4s → 8s → 16s
    const delay = this.baseReconnectDelay * Math.pow(2, this.reconnectAttempts - 1)
    console.log('WebSocket 将在 ' + delay + 'ms 后第 ' + this.reconnectAttempts + ' 次重连')
    this.triggerEvent('reconnecting', { attempt: this.reconnectAttempts, max: this.maxReconnectAttempts })
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, delay)
  }

  /** 连接是否打开 */
  isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN
  }

  /**
   * 发送消息到后端
   * @param {Object} message 消息对象
   */
  sendMessage(message: object) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message))
      console.log('消息已发送:', (message as any).type || (message as any).editAction || '?')
    } else {
      console.error('WebSocket 未连接，无法发送消息 — readyState:', this.socket?.readyState, 'message:', message)
    }
  }

  /**
   * 添加自定义事件监听
   * @param {string} type 消息类型
   * @param {Function} handler 消息处理函数
   */
  on(type: string, handler: (data?: any) => void) {
    if (!this.eventHandlers[type]) {
      this.eventHandlers[type] = []
    }
    this.eventHandlers[type].push(handler)
  }

  /**
   * 触发事件
   * @param {string} type 消息类型
   * @param {Object} data 消息数据
   */
  triggerEvent(type: string, data?: any) {
    const handlers = this.eventHandlers[type]
    if (handlers) {
      handlers.forEach((handler: any) => handler(data))
    }
  }
}
