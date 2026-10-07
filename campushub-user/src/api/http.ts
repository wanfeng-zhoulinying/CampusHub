import { showToast } from 'vant'
import { useAuthStore } from '../stores/auth'
import type { ApiResult } from '../types/backend'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

type TokenMode = 'public' | 'user' | 'admin'

interface RequestOptions extends RequestInit {
  tokenMode?: TokenMode
}

function withQuery(url: string, query?: Record<string, string | number | undefined>) {
  if (!query) return url
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  const search = params.toString()
  return search ? `${url}?${search}` : url
}

function tokenFor(mode: TokenMode) {
  const auth = useAuthStore()
  if (mode === 'admin') return auth.adminToken
  if (mode === 'user') return auth.userToken || auth.adminToken
  return ''
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const mode = options.tokenMode ?? 'public'
  const token = tokenFor(mode)
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${url}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {}),
      },
    })
  } catch {
    // fetch 本身失败（网络不可达等），转成可读的业务错误由调用方提示
    throw new Error('网络异常，请检查网络或稍后重试')
  }
  let body: ApiResult<T>
  try {
    body = (await response.json()) as ApiResult<T>
  } catch {
    // 网关/代理返回非 JSON（如后端宕机时代理抛出的 HTML 错误页），统一转成业务错误
    throw new Error(response.ok ? '响应格式异常' : `服务异常（HTTP ${response.status}）`)
  }
  if (body.code !== 1) {
    const message = body.message || '请求失败'
    showToast(message)
    if (['请先登录', '登录凭证无效', '登录凭证无效或已过期'].some((text) => message.includes(text))) {
      useAuthStore().logout()
      window.location.hash = '#/login'
    }
    throw new Error(message)
  }
  return body.data as T
}

export const apiGet = <T>(url: string, query?: Record<string, string | number | undefined>, tokenMode?: TokenMode) =>
  request<T>(withQuery(url, query), { method: 'GET', tokenMode })

export const apiPost = <T>(url: string, body?: unknown, tokenMode?: TokenMode) =>
  request<T>(url, { method: 'POST', body: JSON.stringify(body ?? {}), tokenMode })

export const apiPut = <T>(url: string, body?: unknown, tokenMode?: TokenMode) =>
  request<T>(url, { method: 'PUT', body: JSON.stringify(body ?? {}), tokenMode })

export { withQuery }
