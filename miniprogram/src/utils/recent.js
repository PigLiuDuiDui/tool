const KEY = 'lt_recent_tools'

export function pushRecent(tool) {
  if (!tool?.code) return
  const list = getRecent().filter((t) => t.code !== tool.code)
  list.unshift({
    code: tool.code,
    name: tool.name,
    desc: tool.desc,
    tone: tool.tone,
    enabled: tool.enabled !== false,
  })
  uni.setStorageSync(KEY, list.slice(0, 12))
}

export function getRecent() {
  return uni.getStorageSync(KEY) || []
}
