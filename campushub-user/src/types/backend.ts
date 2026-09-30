export interface ApiResult<T> {
  code: number
  message: string
  data: T | null
}

export interface UserRegisterDTO {
  username: string
  password: string
  realName: string
  phone?: string
  email?: string
  studentNo?: string
}

export interface UserLoginDTO {
  username: string
  password: string
}

export interface UserLoginVO {
  id: number
  username: string
  realName: string
  role: number
  token: string
}

export interface UserInfoVO {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  studentNo?: string
  creditScore: number
  role: number
  status: number
}

export interface VenueQueryDTO {
  category?: string
  keyword?: string
  status?: number
}

export interface VenueListVO {
  id: number
  name: string
  category: string
  location: string
  capacity: number
  coverUrl?: string
  status: number
}

export interface AdminVenueListVO extends VenueListVO {
  bookingCount?: number
}

export interface VenueDetailVO extends VenueListVO {
  description?: string
  longitude?: number
  latitude?: number
}

export interface VenueSlotVO {
  id: number
  slotDate: string
  startTime: string
  endTime: string
  maxCapacity: number
  availableCapacity: number
  status: number
}

export interface HotVenueVO extends VenueListVO {
  hotScore: number
}

export interface ActivityQueryDTO {
  keyword?: string
  status?: number
  auditStatus?: number
}

export interface ActivityListVO {
  id: number
  title: string
  coverUrl?: string
  location: string
  signupLimit: number
  currentSignupCount: number
  waitLimit: number
  status: number
  auditStatus: number
  signupStartTime: string
  signupEndTime: string
  activityStartTime: string
  activityEndTime: string
}

export interface ActivityDetailVO extends ActivityListVO {
  publisherId: number
  content?: string
  venueId?: number
  auditRemark?: string
}

export interface HotActivityVO extends ActivityListVO {
  hotScore: number
}

export interface ActivitySignupVO {
  id: number
  activityId: number
  activityTitle: string
  activityLocation: string
  signupTime: string
  signupStatus: number
  signStatus: number
  signTime?: string
  cancelTime?: string
  waitOrder?: number
}

export interface BookingCreateDTO {
  venueId: number
  slotId: number
  personCount: number
  remark?: string
}

export interface BookingListVO {
  id: number
  bookingNo: string
  venueId: number
  venueName: string
  venueLocation: string
  bookingDate: string
  startTime: string
  endTime: string
  personCount: number
  status: number
  remark?: string
  createTime: string
}

export interface CreditOverviewVO {
  userId: number
  creditScore: number
  totalDeductScore: number
  totalRestoreScore: number
  netDeductScore: number
  breachCount: number
}

export interface CreditRecordVO {
  id: number
  changeType: number
  changeScore: number
  currentScore: number
  reason: string
  businessType: number
  businessId?: number
  createTime: string
}

export interface BookingBreachAppealVO {
  id: number
  bookingId: number
  bookingNo: string
  reason: string
  appealStatus: number
  deductScore: number
  auditRemark?: string
  appealTime: string
  auditTime?: string
}

export interface MessageVO {
  id: number
  title: string
  content: string
  type: number
  businessId?: number
  readStatus: number
  createTime: string
}

export interface ActivityCommentVO {
  id: number
  activityId: number
  userId: number
  username: string
  realName: string
  content: string
  likeCount: number
  liked: boolean
  createTime: string
}

export interface ActivityFavoriteVO {
  favoriteId: number
  activityId: number
  activityTitle: string
  coverUrl?: string
  location: string
  status: number
  auditStatus: number
  activityStartTime: string
  activityEndTime: string
  createTime: string
}

export interface AdminVenueSaveDTO {
  name: string
  category: string
  location: string
  capacity: number
  coverUrl?: string
  description?: string
  longitude?: number
  latitude?: number
  status?: number
}

export interface AdminActivitySaveDTO {
  publisherId: number
  title: string
  coverUrl?: string
  content?: string
  location: string
  venueId?: number
  signupStartTime: string
  signupEndTime: string
  activityStartTime: string
  activityEndTime: string
  signupLimit: number
  waitLimit: number
  status?: number
}

export interface AdminActivityListVO extends ActivityListVO {
  venueId?: number
}

export interface AdminBookingBreachAppealVO extends BookingBreachAppealVO {
  userId: number
  username: string
  realName: string
  auditUserId?: number
}

export interface AdminDashboardOverviewVO {
  userCount: number
  venueCount: number
  activityCount: number
  bookingCount: number
  messageCount: number
  breachBookingCount: number
}

export interface AdminBookingStatusStatVO {
  status: number
  count: number
}

export interface AdminHotVenueVO {
  venueId: number
  venueName: string
  bookingCount: number
}

export interface AdminHotActivityVO {
  activityId: number
  activityTitle: string
  signupCount: number
}
