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

export function register(data: { username: string; password: string; nickname?: string; phone?: string }) {
  return http.post<UserInfo>('/api/user/register', data)
}

export async function login(data: { username: string; password: string }) {
  const res = await http.post<{ token: string; user: UserInfo }>('/api/user/login', data)
  if (res.data?.token) {
    setToken(res.data.token)
  }
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

