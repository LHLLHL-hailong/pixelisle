// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** sendInvitation POST /api/notification/invitation/send */
export async function sendInvitationUsingPost(
  body: API.InvitationSendRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/notification/invitation/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** acceptInvitation POST /api/notification/invitation/accept */
export async function acceptInvitationUsingPost(
  body: API.InvitationActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/notification/invitation/accept', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** rejectInvitation POST /api/notification/invitation/reject */
export async function rejectInvitationUsingPost(
  body: API.InvitationActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/notification/invitation/reject', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** contactAdmin POST /api/notification/contactAdmin */
export async function contactAdminUsingPost(
  body: API.ContactAdminRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/notification/contactAdmin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** listNotifications POST /api/notification/list */
export async function listNotificationsUsingPost(
  body: API.NotificationQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageNotificationVO_>('/api/notification/list', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** reply POST /api/notification/reply */
export async function replyNotificationUsingPost(
  body: API.ReplyRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/notification/reply', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** unreadCount GET /api/notification/unreadCount */
export async function getUnreadCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMap_>('/api/notification/unreadCount', {
    method: 'GET',
    ...(options || {}),
  })
}
