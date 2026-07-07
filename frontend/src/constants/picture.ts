/**
 * 图片审核状态
 */
export const PIC_REVIEW_STATUS_ENUM = {
  REVIEWING: 0,
  PASS: 1,
  REJECT: 2,
}

/**
 * 图片审核状态文案
 */
export const PIC_REVIEW_STATUS_MAP = {
  0: '待审核',
  1: '通过',
  2: '拒绝',
}

/**
 * 图片审核下拉表单选项
 */
export const PIC_REVIEW_STATUS_OPTIONS = Object.keys(PIC_REVIEW_STATUS_MAP).map((key) => {
  return {
    label: PIC_REVIEW_STATUS_MAP[key],
    value: key,
  }
})

/**
 * 图片编辑消息类型枚举
 */
export const PICTURE_EDIT_MESSAGE_TYPE_ENUM = {
  INFO: 'INFO',
  ERROR: 'ERROR',
  ENTER_EDIT: 'ENTER_EDIT',
  EXIT_EDIT: 'EXIT_EDIT',
  EDIT_ACTION: 'EDIT_ACTION',
  // ── 阶段2新增 ──
  OBJECT_MODIFIED: 'OBJECT_MODIFIED',
  OBJECT_MOVING: 'OBJECT_MOVING',
  OBJECT_SCALING: 'OBJECT_SCALING',
  OBJECT_ROTATING: 'OBJECT_ROTATING',
  OBJECT_ADDED: 'OBJECT_ADDED',
  OBJECT_REMOVED: 'OBJECT_REMOVED',
  CANVAS_ZOOM: 'CANVAS_ZOOM',
  CANVAS_PAN: 'CANVAS_PAN',
  CROP_CHANGE: 'CROP_CHANGE',
  OBJECT_SELECTED: 'OBJECT_SELECTED',
  OBJECT_DESELECTED: 'OBJECT_DESELECTED',
  SAVE_IMAGE: 'SAVE_IMAGE',
  SAVE_COMPLETE: 'SAVE_COMPLETE',
  SYNC_START: 'SYNC_START',
  SYNC_HISTORY: 'SYNC_HISTORY',
  SYNC_END: 'SYNC_END',
  PING: 'PING',
  PONG: 'PONG',
};

export const PICTURE_EDIT_MESSAGE_TYPE_MAP = {
  INFO: '发送通知',
  ERROR: '发送错误',
  ENTER_EDIT: '进入编辑状态',
  EXIT_EDIT: '退出编辑状态',
  EDIT_ACTION: '执行编辑操作',
  OBJECT_MODIFIED: '对象修改完成',
  OBJECT_MOVING: '对象移动中',
  OBJECT_SCALING: '对象缩放中',
  OBJECT_ROTATING: '对象旋转中',
  OBJECT_ADDED: '对象新增',
  OBJECT_REMOVED: '对象删除',
  CANVAS_ZOOM: '画布缩放',
  CANVAS_PAN: '画布平移',
  CROP_CHANGE: '裁切框变化',
  OBJECT_SELECTED: '对象被选中',
  OBJECT_DESELECTED: '对象取消选中',
  SAVE_IMAGE: '请求保存图片',
  SAVE_COMPLETE: '保存完成',
  SYNC_START: '同步开始',
  SYNC_HISTORY: '历史消息',
  SYNC_END: '同步结束',
  PING: '心跳',
  PONG: '心跳响应',
};

export const PICTURE_EDIT_ACTION_ENUM = {
  ZOOM_IN: 'ZOOM_IN',
  ZOOM_OUT: 'ZOOM_OUT',
  ROTATE_LEFT: 'ROTATE_LEFT',
  ROTATE_RIGHT: 'ROTATE_RIGHT',
  // ── 阶段2新增 ──
  CROP_MOVE: 'CROP_MOVE',
  CROP_RESIZE: 'CROP_RESIZE',
  DRAW_PATH: 'DRAW_PATH',
  ADD_TEXT: 'ADD_TEXT',
  ADD_RECT: 'ADD_RECT',
  APPLY_FILTER: 'APPLY_FILTER',
  DELETE_OBJECT: 'DELETE_OBJECT',
  UNDO: 'UNDO',
  REDO: 'REDO',
};

export const PICTURE_EDIT_ACTION_MAP = {
  ZOOM_IN: '放大操作',
  ZOOM_OUT: '缩小操作',
  ROTATE_LEFT: '左旋操作',
  ROTATE_RIGHT: '右旋操作',
  CROP_MOVE: '裁切框移动',
  CROP_RESIZE: '裁切框调整',
  DRAW_PATH: '画笔路径',
  ADD_TEXT: '添加文字',
  ADD_RECT: '添加矩形',
  APPLY_FILTER: '应用滤镜',
  DELETE_OBJECT: '删除对象',
  UNDO: '撤销',
  REDO: '重做',
};
