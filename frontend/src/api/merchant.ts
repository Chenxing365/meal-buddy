import { http } from '@/utils/request'
import type { DishInfo } from './dish'

export interface MerchantInfo {
  id: string
  userId: string
  name: string
  logo?: string
  address: string
  longitude?: number
  latitude?: number
  category: string
  commissionRate: number
  status: number // 0-待审核, 1-正常营业, 2-打烊, 3-驳回
  rejectReason?: string
  createdAt?: string
}

export interface MerchantDetail {
  merchant: MerchantInfo
  dishes: DishInfo[]
}

export interface VerifyResponse {
  orderId: string
  orderNo: string
  merchantId: string
  operator: string
  verifyType: number
  verifiedAt: string
  message: string
}

export interface MerchantDashboard {
  merchantId: string
  merchantName: string
  todayOrderCount: number
  todayRevenue: number
  todayVerifiedCount: number
  todayVerifyRate: number
  totalOrderCount: number
}

export function getMerchantList(params?: { category?: string; longitude?: number; latitude?: number; distanceKm?: number; page?: number; size?: number }) {
  return http.get<{ content: MerchantInfo[]; totalElements: number }>('/api/merchant/list', params)
}

export function getMerchantDetail(id: string) {
  return http.get<MerchantDetail>(`/api/merchant/${id}`)
}

export function applyMerchant(data: { name: string; address: string; category: string; logo?: string; longitude?: number; latitude?: number }) {
  return http.post<MerchantInfo>('/api/merchant/apply', data)
}

export function updateMerchantInfo(id: string, data: Partial<MerchantInfo>) {
  return http.put<MerchantInfo>(`/api/merchant/info/${id}`, data)
}

export function verifyOrder(pickupCode: string, verifyType: number = 1) {
  return http.post<VerifyResponse>('/api/merchant/verify', { pickupCode, verifyType })
}

export function getMerchantDashboard() {
  return http.get<MerchantDashboard>('/api/merchant/dashboard')
}

export function getMerchantOrders(params?: { page?: number; size?: number }) {
  return http.get<{ content: any[]; totalElements: number }>('/api/merchant/orders', params)
}

