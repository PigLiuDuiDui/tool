const KEY = 'lt_favorite_tools'

export function getFavorites() {
  const list = uni.getStorageSync(KEY)
  return Array.isArray(list) ? list : []
}

export function isFavorite(code) {
  return getFavorites().some((t) => t.code === code)
}

export function toggleFavorite(tool) {
  if (!tool?.code) return false
  const list = getFavorites().filter((t) => t.code !== tool.code)
  const exists = getFavorites().some((t) => t.code === tool.code)
  if (!exists) {
    list.unshift({
      code: tool.code,
      name: tool.name,
      desc: tool.desc || '',
      category: tool.category || '',
    })
  }
  uni.setStorageSync(KEY, list.slice(0, 30))
  return !exists
}
