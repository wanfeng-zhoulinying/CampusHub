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

/* ============ 状态 → 徽标语义色映射 ============
 * 统一配色语义：绿=可行动/顺利完成，蓝=进行中，橙=待处理/候补/错过提醒，
 * 红=异常态（违约/驳回/取消），灰=已结束/已取消的弱化态 */

/** van-tag type 属性接受的字面量集合（Vant TagType 的子集） */
export type TagType = 'default' | 'primary' | 'success' | 'warning' | 'danger'

/** 活动状态徽标色：报名中绿、进行中蓝、报名结束橙（错过提醒）、已取消红 */
export const activityTagType: Record<number, TagType> = {
  1: 'default',
  2: 'success',
  3: 'primary',
  4: 'default',
  5: 'danger',
  6: 'warning',
}

/** 预约状态徽标色：已预约蓝、已核销绿、已取消灰、已违约红、申诉通过绿（恢复） */
export const bookingTagType: Record<number, TagType> = {
  1: 'primary',
  2: 'success',
  3: 'default',
  4: 'danger',
  5: 'success',
}

/** 报名状态徽标色：已报名绿、已取消灰、候补中橙、候补转正蓝 */
export const signupTagType: Record<number, TagType> = {
  1: 'success',
  2: 'default',
  3: 'warning',
  4: 'primary',
}

/** 审核/申诉状态徽标色：待审核橙、已通过绿、已驳回红 */
export const auditTagType: Record<number, TagType> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
}

export const appealTagType: Record<number, TagType> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
}
