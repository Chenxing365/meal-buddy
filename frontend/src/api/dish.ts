import { http } from '@/utils/request'

export interface DishInfo {
  id: string
  merchantId: string
  name: string
  description?: string
  price: number
  memberPrice?: number
  coverImage?: string
  calorie: number
  flavorTags: string[]
  ingredientTags: string[]
  status: number // 0-下架, 1-上架
  createdAt?: string
}

export function getDishDetail(id: string) {
  return http.get<DishInfo>(`/api/dish/${id}`)
}

export function getMerchantDishes(merchantId: string, params?: { page?: number; size?: number }) {
  return http.get<{ content: DishInfo[]; totalElements: number }>(`/api/dish/merchant/${merchantId}`, params)
}

export function createDish(data: { merchantId: string; name: string; price: number; calorie: number; flavorTags: string[]; description?: string; coverImage?: string; memberPrice?: number }) {
  return http.post<DishInfo>('/api/dish', data)
}

export function updateDish(id: string, data: Partial<DishInfo>) {
  return http.put<DishInfo>(`/api/dish/${id}`, data)
}

export function toggleDishStatus(id: string, status: number) {
  return http.put<void>(`/api/dish/${id}/status`, { status })
}

export function deleteDish(id: string) {
  return http.delete<void>(`/api/dish/${id}`)
}

