<template>
  <view class="page-container">
    <!-- 顶部标题与对话助手快捷入口 -->
    <view class="section-header">
      <view>
        <text class="section-title">🤖 AI 饭搭子精选推荐</text>
        <text class="sub-header-text">基于健康目标、热量阈值与口味偏好实时推理</text>
      </view>
      <view class="chat-pill" @tap="goToChat">
        <text>💬 咨询助手</text>
      </view>
    </view>

    <!-- 模式切换控制器 -->
    <view class="mode-tabs food-card">
      <view 
        class="mode-tab-item" 
        :class="{ active: activeMode === 'slim' }"
        @tap="switchMode('slim')"
      >
        <text class="tab-emoji">🌱</text>
        <text class="tab-title">低卡减脂</text>
        <text class="tab-sub">≤ 500 大卡</text>
      </view>
      <view 
        class="mode-tab-item" 
        :class="{ active: activeMode === 'normal' }"
        @tap="switchMode('normal')"
      >
        <text class="tab-emoji">🍱</text>
        <text class="tab-title">营养均衡</text>
        <text class="tab-sub">≤ 800 大卡</text>
      </view>
      <view 
        class="mode-tab-item" 
        :class="{ active: activeMode === 'indulge' }"
        @tap="switchMode('indulge')"
      >
        <text class="tab-emoji">🍔</text>
        <text class="tab-title">放纵美味</text>
        <text class="tab-sub">多巴胺快乐</text>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="empty-container">
      <text class="empty-icon">🧠</text>
      <text class="empty-text">AI 正在根据健康规则引擎精密计算最佳菜品组合...</text>
    </view>

    <view v-else-if="recommendData">
      <!-- AI 推荐理由与话术卡片 -->
      <view class="food-card" :class="activeMode === 'slim' ? 'food-card-slim' : 'food-card-featured'">
        <view class="reason-header">
          <text class="reason-tag">AI 推荐引擎结论</text>
          <text class="reason-time">{{ recommendData.createdAt ? recommendData.createdAt.substring(11, 19) : '刚刚' }}</text>
        </view>
        <text class="reason-content">{{ recommendData.matchReason }}</text>
        
        <!-- 点赞 / 点踩反馈区 -->
        <view class="feedback-row">
          <text class="feedback-label">对本次推荐满意吗？</text>
          <view class="feedback-btns">
            <view 
              class="feedback-btn" 
              :class="{ selected: feedbackState === 1 }"
              @tap="handleFeedback(1)"
            >
              <text>👍 满意</text>
            </view>
            <view 
              class="feedback-btn" 
              :class="{ selected: feedbackState === -1 }"
              @tap="handleFeedback(-1)"
            >
              <text>👎 不符合</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 命中推荐菜品列表 -->
      <view class="section-header">
        <text class="section-title">推荐菜品清单 ({{ recommendData.dishes?.length || 0 }})</text>
        <text class="section-extra" @tap="fetchRecommendations">🔄 重新生成</text>
      </view>

      <view v-if="!recommendData.dishes || recommendData.dishes.length === 0" class="empty-container">
        <text class="empty-icon">🍃</text>
        <text class="empty-text">当前模式下暂无可匹配菜品，去个人中心放宽热量阈值试试</text>
      </view>

      <view v-else class="dish-list">
        <view 
          v-for="dish in recommendData.dishes" 
          :key="dish.id" 
          class="food-card dish-card"
        >
          <view class="dish-emoji-cover">
            <text class="dish-emoji">{{ getDishEmoji(dish.name) }}</text>
          </view>
          <view class="dish-info">
            <text class="dish-name">{{ dish.name }}</text>
            <view class="dish-tags-row">
              <text class="tag-badge tag-calorie">🔥 {{ dish.calorie }} 大卡</text>
              <text 
                v-for="tag in (dish.flavorTags || []).slice(0, 2)" 
                :key="tag" 
                class="tag-badge tag-flavor"
              >
                {{ tag }}
              </text>
            </view>
            <view class="dish-bottom-row">
              <view class="price-box">
                <text class="price-symbol">¥</text>
                <text class="price-value">{{ dish.price }}</text>
              </view>
              <button class="order-btn" @tap="quickOrder(dish)">进店下单</button>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
  getRecommendMode, 
  switchRecommendMode, 
  getRecommendList, 
  submitRecommendFeedback, 
  type RecommendResponse 
} from '@/api/recommend'
import type { DishInfo } from '@/api/dish'

const loading = ref(false)
const activeMode = ref<'slim' | 'normal' | 'indulge'>('normal')
const recommendData = ref<RecommendResponse | null>(null)
const feedbackState = ref<number>(0)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const modeRes = await getRecommendMode()
    if (modeRes.data?.mode) {
      activeMode.value = modeRes.data.mode
    }
    await fetchRecommendations()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function switchMode(mode: 'slim' | 'normal' | 'indulge') {
  if (activeMode.value === mode) return
  activeMode.value = mode
  loading.value = true
  try {
    await switchRecommendMode(mode)
    await fetchRecommendations()
  } catch (e) {
    uni.showToast({ title: '切换失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function fetchRecommendations() {
  loading.value = true
  feedbackState.value = 0
  try {
    const res = await getRecommendList()
    if (res.data) {
      recommendData.value = res.data
    }
  } catch (e) {
    uni.showToast({ title: '请先登录以体验AI个性化推荐', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleFeedback(feedback: number) {
  if (!recommendData.value?.logId) return
  try {
    await submitRecommendFeedback(recommendData.value.logId, feedback)
    feedbackState.value = feedback
    uni.showToast({ title: feedback === 1 ? '感谢点赞反馈 👍' : '已记录，将优化推荐 📝', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '反馈提交失败', icon: 'none' })
  }
}

function quickOrder(dish: DishInfo) {
  uni.navigateTo({ url: `/pages/merchant/detail?id=${dish.merchantId}` })
}

function goToChat() {
  uni.navigateTo({ url: '/pages/recommend/chat' })
}

function getDishEmoji(name: string): string {
  if (name.includes('沙拉') || name.includes('轻食')) return '🥗'
  if (name.includes('鸡') || name.includes('肉')) return '🍗'
  if (name.includes('牛')) return '🥩'
  if (name.includes('虾') || name.includes('海鲜')) return '🍤'
  if (name.includes('面') || name.includes('粉')) return '🍜'
  if (name.includes('堡')) return '🍔'
  return '🍱'
}
</script>

<style scoped>
.sub-header-text {
  font-size: 22rpx;
  color: #64748B;
  display: block;
  margin-top: 4rpx;
}
.chat-pill {
  font-size: 24rpx;
  background: #EFF6FF;
  color: #2563EB;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.mode-tabs {
  display: flex;
  padding: 16rpx;
  margin-bottom: 24rpx;
}
.mode-tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;
  border-radius: 16rpx;
  transition: all 0.2s;
}
.mode-tab-item.active {
  background: #FFF7ED;
  box-shadow: 0 4rpx 16rpx rgba(255, 94, 0, 0.1);
}
.tab-emoji {
  font-size: 36rpx;
  margin-bottom: 4rpx;
}
.tab-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #334155;
}
.mode-tab-item.active .tab-title {
  color: #FF5E00;
}
.tab-sub {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 2rpx;
}

.reason-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.reason-tag {
  font-size: 22rpx;
  font-weight: 700;
  color: #FF5E00;
  background: #FFF7ED;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.reason-time {
  font-size: 22rpx;
  color: #94A3B8;
}
.reason-content {
  font-size: 28rpx;
  line-height: 1.6;
  color: #1E293B;
  margin-bottom: 20rpx;
  display: block;
}

.feedback-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1rpx dashed #E2E8F0;
  padding-top: 18rpx;
}
.feedback-label {
  font-size: 24rpx;
  color: #64748B;
}
.feedback-btns {
  display: flex;
  gap: 16rpx;
}
.feedback-btn {
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  background: #F1F5F9;
  color: #475569;
  font-weight: 600;
}
.feedback-btn.selected {
  background: #FF5E00;
  color: #FFFFFF;
}

.dish-card {
  display: flex;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.dish-emoji-cover {
  width: 130rpx;
  height: 130rpx;
  background: #F8FAFC;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  border: 1rpx solid #E2E8F0;
}
.dish-emoji {
  font-size: 64rpx;
}
.dish-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.dish-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}
.dish-tags-row {
  margin: 8rpx 0;
}
.dish-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-btn {
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 999rpx;
  padding: 6rpx 24rpx;
  margin: 0;
}
</style>

