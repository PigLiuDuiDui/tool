import { BASE_URL } from './config'

function getToken() {
  return uni.getStorageSync('accessToken') || ''
}

export function request({ url, method = 'GET', data, header = {} }) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        Authorization: getToken() ? `Bearer ${getToken()}` : '',
        ...header,
      },
      success: (res) => {
        const body = res.data || {}
        if (body.code === 0) {
          resolve(body.data)
        } else {
          reject(new Error(body.message || '请求失败'))
        }
      },
      fail: (err) => reject(err),
    })
  })
}

function uploadOne(url, filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: `${BASE_URL}${url}`,
      filePath,
      name: 'file',
      formData,
      header: { Authorization: token ? `Bearer ${token}` : '' },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code !== 0) {
            reject(new Error(body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch (e) {
          reject(e)
        }
      },
      fail: reject,
    })
  })
}

/** 暂存额外文件，返回服务端绝对路径 */
export function stageFile(filePath) {
  return uploadOne('/jobs/stage', filePath, {})
}

/**
 * 创建并执行任务。
 * @param {string} toolCode
 * @param {string} filePath 主文件
 * @param {object} formData
 * @param {string[]} extraFilePaths 额外文件本地路径（图片转PDF / PDF合并）
 */
export async function uploadJob(toolCode, filePath, formData = {}, extraFilePaths = []) {
  const extras = []
  for (const p of extraFilePaths || []) {
    if (!p) continue
    const staged = await stageFile(p)
    if (staged?.path) extras.push(staged.path)
  }
  const data = { toolCode, ...formData }
  if (extras.length) {
    data.extraPaths = extras.join(',')
  }
  return uploadOne('/jobs', filePath, data)
}

export function estimateJob(toolCode, filePath, formData = {}) {
  return uploadOne('/jobs/estimate', filePath, { toolCode, ...formData })
}

/** 证件照预览，不扣次数。返回透明底 PNG 的 base64。 */
export function previewJob(toolCode, filePath, formData = {}) {
  return uploadOne('/jobs/preview', filePath, { toolCode, ...formData })
}
