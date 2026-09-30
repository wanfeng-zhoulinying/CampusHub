import { apiGet, apiPost, apiPut, request, withQuery } from './http'
import type {
  ActivityCommentVO,
  ActivityDetailVO,
  ActivityFavoriteVO,
  ActivityListVO,
  ActivitySignupVO,
  AdminActivityListVO,
  AdminActivitySaveDTO,
  AdminBookingBreachAppealVO,
  AdminBookingStatusStatVO,
  AdminDashboardOverviewVO,
  AdminHotActivityVO,
  AdminHotVenueVO,
  AdminVenueListVO,
  AdminVenueSaveDTO,
  BookingCreateDTO,
  BookingListVO,
  CreditOverviewVO,
  CreditRecordVO,
  HotActivityVO,
  HotVenueVO,
  MessageVO,
  UserInfoVO,
  UserLoginDTO,
  UserLoginVO,
  UserRegisterDTO,
  VenueDetailVO,
  VenueListVO,
  VenueSlotVO,
} from '../types/backend'

export const authApi = {
  register: (body: UserRegisterDTO) => apiPost<number>('/user/register', body),
  userLogin: (body: UserLoginDTO) => apiPost<UserLoginVO>('/user/login', body),
  adminLogin: (body: UserLoginDTO) => apiPost<UserLoginVO>('/admin/login', body),
  userMe: () => apiGet<UserInfoVO>('/user/me', undefined, 'user'),
  adminMe: () => apiGet<UserInfoVO>('/admin/me', undefined, 'admin'),
}

export const venueApi = {
  list: (query?: { category?: string; keyword?: string; status?: number }) => apiGet<VenueListVO[]>('/venue/list', query),
  hot: (limit = 10) => apiGet<HotVenueVO[]>('/venue/hot', { limit }),
  detail: (venueId: number) => apiGet<VenueDetailVO>(`/venue/${venueId}`),
  slots: (venueId: number, date: string) => apiGet<VenueSlotVO[]>(`/venue/${venueId}/slots`, { date }),
}

export const activityApi = {
  list: (query?: { keyword?: string; status?: number; auditStatus?: number }) => apiGet<ActivityListVO[]>('/activity/list', query),
  hot: (limit = 10) => apiGet<HotActivityVO[]>('/activity/hot', { limit }),
  detail: (activityId: number) => apiGet<ActivityDetailVO>(`/activity/${activityId}`),
  signup: (activityId: number) => apiPost<number>('/activity/signup', { activityId }, 'user'),
  my: (signupStatus?: number) => apiGet<ActivitySignupVO[]>('/activity/my', { signupStatus }, 'user'),
  cancel: (signupId: number) => apiPut<void>(`/activity/${signupId}/cancel`, undefined, 'user'),
  checkin: (signupId: number) => apiPut<void>(`/activity/${signupId}/checkin`, undefined, 'user'),
}

export const bookingApi = {
  create: (body: BookingCreateDTO) => apiPost<number>('/booking/create', body, 'user'),
  my: (status?: number) => apiGet<BookingListVO[]>('/booking/my', { status }, 'user'),
  cancel: (bookingId: number, cancelReason?: string) => apiPut<void>(`/booking/${bookingId}/cancel`, { cancelReason }, 'user'),
  checkin: (bookingId: number) => apiPut<void>(`/booking/${bookingId}/checkin`, undefined, 'user'),
}

export const creditApi = {
  overview: () => apiGet<CreditOverviewVO>('/credit/my', undefined, 'user'),
  records: () => apiGet<CreditRecordVO[]>('/credit/records/my', undefined, 'user'),
  appeal: (bookingId: number, reason: string) => apiPost<number>(`/credit/booking/${bookingId}/appeal`, { reason }, 'user'),
  appeals: (appealStatus?: number) => apiGet<AdminBookingBreachAppealVO[]>('/credit/appeals/my', { appealStatus }, 'user'),
}

export const messageApi = {
  list: (readStatus?: number) => apiGet<MessageVO[]>('/message/my', { readStatus }, 'user'),
  unreadCount: () => apiGet<number>('/message/unread/count', undefined, 'user'),
  read: (messageId: number) => apiPut<void>(`/message/${messageId}/read`, undefined, 'user'),
}

export const socialApi = {
  comments: (activityId: number) => apiGet<ActivityCommentVO[]>(`/social/activity/${activityId}/comments`, undefined, 'user'),
  comment: (activityId: number, content: string) => apiPost<number>('/social/activity/comment', { activityId, content }, 'user'),
  like: (commentId: number) => apiPost<boolean>(`/social/comment/${commentId}/like`, undefined, 'user'),
  favorite: (activityId: number) => apiPost<boolean>(`/social/activity/${activityId}/favorite`, undefined, 'user'),
  favorites: () => apiGet<ActivityFavoriteVO[]>('/social/activity/favorites/my', undefined, 'user'),
}

export const adminApi = {
  overview: () => apiGet<AdminDashboardOverviewVO>('/admin/statistics/overview', undefined, 'admin'),
  bookingStatus: () => apiGet<AdminBookingStatusStatVO[]>('/admin/statistics/booking/status', undefined, 'admin'),
  hotVenues: (limit = 5) => apiGet<AdminHotVenueVO[]>('/admin/statistics/venue/hot', { limit }, 'admin'),
  hotActivities: (limit = 5) => apiGet<AdminHotActivityVO[]>('/admin/statistics/activity/hot', { limit }, 'admin'),
  venues: (query?: { name?: string; category?: string; status?: number }) => apiGet<AdminVenueListVO[]>('/admin/venue/list', query, 'admin'),
  createVenue: (body: AdminVenueSaveDTO) => apiPost<number>('/admin/venue', body, 'admin'),
  updateVenue: (venueId: number, body: AdminVenueSaveDTO) => apiPut<void>(`/admin/venue/${venueId}`, body, 'admin'),
  updateVenueStatus: (venueId: number, status: number) =>
    request<void>(withQuery(`/admin/venue/${venueId}/status`, { status }), { method: 'PUT', tokenMode: 'admin' }),
  activities: (query?: { title?: string; status?: number; auditStatus?: number }) =>
    apiGet<AdminActivityListVO[]>('/admin/activity/list', query, 'admin'),
  createActivity: (body: AdminActivitySaveDTO) => apiPost<number>('/admin/activity', body, 'admin'),
  updateActivity: (activityId: number, body: AdminActivitySaveDTO) => apiPut<void>(`/admin/activity/${activityId}`, body, 'admin'),
  auditActivity: (activityId: number, auditUserId: number, auditStatus: number, auditRemark?: string) =>
    apiPut<void>(`/admin/activity/${activityId}/audit`, { auditUserId, auditStatus, auditRemark }, 'admin'),
  updateActivityStatus: (activityId: number, status: number) =>
    request<void>(withQuery(`/admin/activity/${activityId}/status`, { status }), { method: 'PUT', tokenMode: 'admin' }),
  appeals: (appealStatus?: number) => apiGet<AdminBookingBreachAppealVO[]>('/admin/credit/appeals', { appealStatus }, 'admin'),
  markBreach: (bookingId: number, reason: string, deductScore?: number) =>
    apiPut<void>(`/admin/credit/booking/${bookingId}/breach`, { reason, deductScore }, 'admin'),
  auditAppeal: (appealId: number, auditStatus: number, auditRemark?: string) =>
    apiPut<void>(`/admin/credit/appeals/${appealId}/audit`, { auditStatus, auditRemark }, 'admin'),
}
