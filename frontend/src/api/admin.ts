import { http } from '@/utils/request'
import type { MerchantInfo } from './merchant'

export interface AdminUserItem {
  id: string
  username: string
  nickname: string
  phone: string
  role: number
  status: number
  createdAt: string
}

export function getAdminMerchants(params?: { status?: number; page?: number; size?: number }) {
  return http.get<{ content: MerchantInfo[]; totalElements: number }>('/api/admin/merchants', params)
}

export function approveMerchant(id: string) {
  return http.post<void>(`/api/admin/merchant/${id}/approve`)
}

export function rejectMerchant(id: string, reason: string) {
  return http.post<void>(`/api/admin/merchant/${id}/reject`, { reason })
}

export function getAdminUsers(params?: { role?: number; status?: number; page?: number; size?: number }) {
  return http.get<{ content: AdminUserItem[]; totalElements: number }>('/api/admin/users', params)
}

export function updateUserStatus(id: string, status: number) {
  return http.put<void>(`/api/admin/user/${id}/status`, { status })
}
