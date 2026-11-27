import { request } from './request'

export async function ensureLogin() {
  const token = uni.getStorageSync('accessToken')
  if (token) return token
  const data = await request({
    url: '/auth/wx-login',
    method: 'POST',
    data: { nickName: '微信用户' },
  })
  uni.setStorageSync('accessToken', data.accessToken)
  uni.setStorageSync('user', data.user)
  return data.accessToken
}

export function getUser() {
  return uni.getStorageSync('user') || { nickName: '微信用户' }
}
