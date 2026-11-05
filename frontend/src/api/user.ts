import { http, setToken, clearToken } from '@/utils/request'

export interface UserInfo {
  id: string
  username: string
  nickname?: string
  avatar?: string
  phone?: string
  role: number // 0-用户, 1-骑手, 2-商家, 9-管理员
  status: number
  createdAt?: string
}

export interface UserPreferences {
  id?: string
  userId?: string
  recommendMode: 'slim' | 'normal' | 'indulge'
  flavorTags: string[]
  calorieLimit: number
  allergyTags: string[]
}

export async function register(data: { username: string; password: string; nickname?: string; phone?: string }) {
  const res = await http.post<UserInfo>('/api/user/register', data)
  // 后端业务失败仍返回 HTTP 200/400 + success:false，必须显式抛出，否则调用方会误判为成功
  if (!res.success) {
    throw new Error(res.message || '注册失败')
  }
  return res
}

export async function login(data: { username: string; password: string }) {
  // 后端 LoginResponse 字段为 accessToken / refreshToken / user
  const res = await http.post<{ accessToken: string; refreshToken: string; user: UserInfo }>('/api/user/login', data)
  if (!res.success || !res.data?.accessToken) {
    throw new Error(res.message || '用户名或密码错误')
  }
  setToken(res.data.accessToken)
  return res
}

export function getUserInfo() {
  return http.get<UserInfo>('/api/user/info')
}

export function updateUserInfo(data: { nickname?: string; avatar?: string }) {
  return http.put<UserInfo>('/api/user/info', data)
}

export function getUserPreferences() {
  return http.get<UserPreferences>('/api/user/preferences')
}

export function updateUserPreferences(data: Partial<UserPreferences>) {
  return http.put<UserPreferences>('/api/user/preferences', data)
}

export function logout() {
  clearToken()
}

