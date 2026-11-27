const KEY = 'lt_feedback'

export function getFeedback() {
  const list = uni.getStorageSync(KEY)
  return Array.isArray(list) ? list : []
}

export function saveFeedback(text) {
  const content = String(text || '').trim()
  if (!content) return false
  const list = getFeedback()
  list.unshift({ text: content, at: Date.now() })
  uni.setStorageSync(KEY, list.slice(0, 20))
  return true
}
