/** 前端 → 后端消息 */
export interface PictureEditRequestMessage {
  type: string
  editAction?: string
  targetId?: string
  params?: Record<string, any>
  objectJSON?: string
  cropParams?: Record<string, number>
  timestamp?: number
}

/** 后端 → 前端消息 */
export interface PictureEditResponseMessage {
  type: string
  message?: string
  editAction?: string
  user?: API.UserVO
  targetId?: string
  params?: Record<string, any>
  cropParams?: Record<string, number>
  pictureUrl?: string
  timestamp?: number
}
