export function assetUrl(url?: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `/api${url}`
}

export function shortTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

/** 取本地时区的 YYYY-MM-DD（toISOString 走 UTC，北京时间 0-8 点会取到前一天） */
export function toDateString(value: Date) {
  const y = value.getFullYear()
  const m = String(value.getMonth() + 1).padStart(2, '0')
  const d = String(value.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function today() {
  return toDateString(new Date())
}

export function percent(current: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((current / total) * 100))
}
