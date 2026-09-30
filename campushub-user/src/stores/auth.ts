import { defineStore } from 'pinia'
import { authApi, messageApi } from '../api'
import { UserRole } from '../constants/status'
import type { UserInfoVO, UserLoginDTO, UserLoginVO, UserRegisterDTO } from '../types/backend'

const USER_TOKEN_KEY = 'campushub_user_token'
const ADMIN_TOKEN_KEY = 'campushub_admin_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    userToken: localStorage.getItem(USER_TOKEN_KEY) || '',
    adminToken: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    profile: null as UserInfoVO | UserLoginVO | null,
    unreadCount: 0,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.userToken || state.adminToken),
    isAdmin: (state) => state.profile?.role === UserRole.ADMIN || Boolean(state.adminToken),
    displayName: (state) => state.profile?.realName || state.profile?.username || '未登录',
  },
  actions: {
    async loginAsUser(payload: UserLoginDTO) {
      const login = await authApi.userLogin(payload)
      this.userToken = login.token
      this.profile = login
      localStorage.setItem(USER_TOKEN_KEY, login.token)
      await this.loadProfile()
    },
    async loginAsAdmin(payload: UserLoginDTO) {
      const login = await authApi.adminLogin(payload)
      this.adminToken = login.token
      this.profile = login
      localStorage.setItem(ADMIN_TOKEN_KEY, login.token)
      await this.loadProfile()
    },
    async register(payload: UserRegisterDTO) {
      await authApi.register(payload)
      await this.loginAsUser({ username: payload.username, password: payload.password })
    },
    async loadProfile() {
      if (this.adminToken) {
        this.profile = await authApi.adminMe()
      } else if (this.userToken) {
        this.profile = await authApi.userMe()
      }
      await this.refreshUnread()
    },
    async refreshUnread() {
      if (!this.userToken && !this.adminToken) return
      try {
        this.unreadCount = await messageApi.unreadCount()
      } catch {
        this.unreadCount = 0
      }
    },
    logout() {
      this.userToken = ''
      this.adminToken = ''
      this.profile = null
      this.unreadCount = 0
      localStorage.removeItem(USER_TOKEN_KEY)
      localStorage.removeItem(ADMIN_TOKEN_KEY)
    },
  },
})
