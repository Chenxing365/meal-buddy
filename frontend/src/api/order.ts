import { http } from '@/utils/request'

export interface OrderItem {
  dishId: string
  name: string
  price: number
  quantity: number
}

export interface OrderInfo {
  id: string
  orderNo: string
  userId: string
  merchantId: string
  merchantName: string
  fulfillType: number // 0-外卖, 1-自提, 2-堂食
  items: OrderItem[]
  totalAmount: number
  status: number // 0-待支付, 1-已支付, 2-配送中, 3-已核销, 4-已完成, 5-已取消
  statusDesc: string
  address?: string
  pickupCode?: string
  paymentTime?: string
  createdAt: string
}

export function createOrder(data: { merchantId: string; fulfillType: number; items: { dishId: string; quantity: number }[]; address?: string }) {
  return http.post<OrderInfo>('/api/order', data)
}

export function payOrder(orderId: string) {
  return http.post<{ orderId: string; orderNo: string; status: number; totalAmount: number; pickupCode?: string }>(`/api/order/${orderId}/pay`)
}

export function getOrderDetail(id: string) {
  return http.get<OrderInfo>(`/api/order/${id}`)
}

export function getUserOrders(params?: { page?: number; size?: number }) {
  return http.get<{ content: OrderInfo[]; totalElements: number }>('/api/order/list', params)
}

