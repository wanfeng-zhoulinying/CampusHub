export const UserRole = {
  USER: 0,
  ADMIN: 1,
} as const

export const VenueSlotStatus = {
  CLOSED: 0,
  AVAILABLE: 1,
  FULL: 2,
} as const

export const BookingStatus = {
  BOOKED: 1,
  CHECKED_IN: 2,
  CANCELED: 3,
  BREACHED: 4,
  APPEAL_APPROVED: 5,
} as const

export const ActivityStatus = {
  NOT_STARTED: 1,
  SIGNING_UP: 2,
  IN_PROGRESS: 3,
  FINISHED: 4,
  CANCELED: 5,
  SIGNUP_ENDED: 6,
} as const

export const ActivityAuditStatus = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const

export const ActivitySignupStatus = {
  SIGNED_UP: 1,
  CANCELED: 2,
  WAITLISTED: 3,
  WAITLIST_CONFIRMED: 4,
} as const

export const MessageReadStatus = {
  UNREAD: 0,
  READ: 1,
} as const

export const BookingAppealStatus = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const

export const activityStatusText: Record<number, string> = {
  1: '未开始',
  2: '报名中',
  3: '进行中',
  4: '已结束',
  5: '已取消',
  6: '报名结束',
}

export const auditStatusText: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
}

export const bookingStatusText: Record<number, string> = {
  1: '已预约',
  2: '已核销',
  3: '已取消',
  4: '已违约',
  5: '申诉通过',
}

export const signupStatusText: Record<number, string> = {
  1: '已报名',
  2: '已取消',
  3: '候补中',
  4: '候补转正',
}

export const appealStatusText: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
}

export const messageTypeText: Record<number, string> = {
  1: '预约',
  2: '活动',
  3: '审核',
  4: '信用',
}
