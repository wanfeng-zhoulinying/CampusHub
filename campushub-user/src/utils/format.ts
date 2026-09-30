export function assetUrl(url?: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `/api${url}`
}

export function shortTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

export function today() {
  return new Date().toISOString().slice(0, 10)
}

export function percent(current: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((current / total) * 100))
}
