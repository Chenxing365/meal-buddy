import { http } from '@/utils/request'
import type { DishInfo } from './dish'

export interface RecommendResponse {
  logId: string
  mode: 'slim' | 'normal' | 'indulge'
  matchReason: string
  dishes: DishInfo[]
  ruleSnapshot: Record<string, any>
  feedback: number
  createdAt: string
}

export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
}

export interface AiChatResponse {
  reply: string
  timestamp: string
}

export function getRecommendMode() {
  return http.get<{ mode: 'slim' | 'normal' | 'indulge' }>('/api/recommend/mode')
}

export function switchRecommendMode(mode: 'slim' | 'normal' | 'indulge') {
  return http.post<string>('/api/recommend/mode', { mode })
}

export function getRecommendList() {
  return http.get<RecommendResponse>('/api/recommend/list')
}

export function submitRecommendFeedback(logId: string, feedback: number) {
  return http.post<void>('/api/recommend/feedback', { logId, feedback })
}

export function getRecommendHistory(params?: { page?: number; size?: number }) {
  return http.get<{ content: RecommendResponse[]; totalElements: number }>('/api/recommend/history', params)
}

export function chatWithAi(message: string) {
  return http.post<AiChatResponse>('/api/ai/chat', { message })
}

export function getChatHistory() {
  return http.get<AiChatMessage[]>('/api/ai/chat/history')
}

