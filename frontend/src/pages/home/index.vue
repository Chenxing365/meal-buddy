<template>
  <view class="page-container">
    <!-- 顶部定位与快捷入口 -->
    <view class="home-header">
      <view class="location-box">
        <text class="location-icon">📍</text>
        <text class="location-text">天府软件园 · 研发中心</text>
      </view>
      <view class="debug-entry" v-if="isAdmin" @tap="navigateTo('/pages/index/index')">
        <text>⚡ 联调看板</text>
      </view>
    </view>

    <!-- AI 推荐模式切换 -->
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

    <!-- AI 推荐区域 -->
    <view v-if="loadingRecommend" class="empty-container">
      <text class="empty-icon">🧠</text>
      <text class="empty-text">AI 正在精密计算最佳菜品组合...</text>
    </view>

    <view v-else-if="recommendData">
      <!-- AI 推荐理由卡片 -->
      <view class="food-card" :class="activeMode === 'slim' ? 'food-card-slim' : 'food-card-featured'">
        <view class="reason-header">
          <text class="reason-tag">AI 推荐引擎结论</text>
          <text class="reason-time">{{ recommendData.createdAt ? recommendData.createdAt.substring(11, 19) : '刚刚' }}</text>
        </view>
        <text class="reason-content">{{ recommendData.matchReason }}</text>

        <!-- 点赞/点踩 -->
        <view class="feedback-row">
          <text class="feedback-label">对本次推荐满意吗？</text>
          <view class="feedback-btns">
            <view class="feedback-btn" :class="{ selected: feedbackState === 1 }" @tap="handleFeedback(1)">
              <text>👍 满意</text>
            </view>
            <view class="feedback-btn" :class="{ selected: feedbackState === -1 }" @tap="handleFeedback(-1)">
              <text>👎 不符合</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 推荐菜品清单 -->
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
            <!-- 商户标签 -->
            <view class="dish-merchant-tag" v-if="dishMerchantName(dish)">
              <text class="merchant-tag-text">🏪 {{ dishMerchantName(dish) }}</text>
            </view>
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
                <!-- 会员价优先显示 -->
                <text v-if="dish.memberPrice && dish.memberPrice > 0 && dish.memberPrice < dish.price" class="price-member">
                  ¥{{ dish.memberPrice }}
                </text>
                <text v-if="dish.memberPrice && dish.memberPrice > 0 && dish.memberPrice < dish.price" class="price-origin">
                  ¥{{ dish.price }}
                </text>
                <text v-else class="price-value">¥{{ dish.price }}</text>
              </view>
              <button class="order-btn" @tap="quickOrder(dish)">进店下单</button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 推荐失败/未登录提示 -->
    <view v-else class="recommend-placeholder food-card">
      <text class="placeholder-icon">🤖</text>
      <text class="placeholder-text">登录后 AI 将根据你的口味与卡路里目标精准推荐</text>
      <button class="btn-login-hint" @tap="goToProfile">去登录体验</button>
    </view>

    <!-- 精选入驻好店 -->
    <view class="section-header" style="margin-top: 32rpx;">
      <text class="section-title">精选入驻好店</text>
    </view>

    <view v-if="loadingMerchants" class="empty-container">
      <text class="empty-icon">⏳</text>
      <text class="empty-text">正在加载美食好店...</text>
    </view>

    <view v-else-if="merchantList.length === 0" class="empty-container">
      <text class="empty-icon">🍽️</text>
      <text class="empty-text">暂无营业中的门店，请在联调看板或商家端入驻</text>
    </view>

    <view v-else class="merchant-list">
      <view
        v-for="item in merchantList"
        :key="item.id"
        class="food-card merchant-card"
        @tap="goToDetail(item.id)"
      >
        <view class="merchant-logo-box">
          <text class="merchant-logo-emoji">🏪</text>
        </view>
        <view class="merchant-info">
          <view class="merchant-name-row">
            <text class="merchant-name">{{ item.name }}</text>
            <text class="tag-badge tag-flavor">{{ item.category }}</text>
          </view>
          <text class="merchant-address">📍 {{ item.address }}</text>
          <view class="merchant-meta-row">
            <text class="merchant-status">🟢 正常营业</text>
            <view class="action-arrow">进入点餐 ›</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部留白（防 TabBar 遮挡） -->
    <view style="height: 120rpx;"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMerchantList, type MerchantInfo } from '@/api/merchant'
import { getUserInfo } from '@/api/user'
import {
  getRecommendMode,
  switchRecommendMode,
  getRecommendList,
  submitRecommendFeedback,
  type RecommendResponse
} from '@/api/recommend'
import type { DishInfo } from '@/api/dish'
import { getDishEmoji } from '@/utils/dish'

const loadingMerchants = ref(false)
const loadingRecommend = ref(false)
const isAdmin = ref(false)
const merchantList = ref<MerchantInfo[]>([])
const activeMode = ref<'slim' | 'normal' | 'indulge'>('normal')
const recommendData = ref<RecommendResponse | null>(null)
const feedbackState = ref<number>(0)
// 商户 ID → 商户名 映射，用于推荐菜品上展示商户标签
const merchantMap = ref<Record<string, string>>({})

onMounted(async () => {
  await loadMode()
  // 并行拉取商家列表 + AI 推荐 + 当前角色（联调看板仅管理员可见）
  await Promise.all([fetchMerchants(), fetchRecommendations(), loadRole()])
  buildMerchantMap()
})

async function loadRole() {
  try {
    const res = await getUserInfo()
    isAdmin.value = res.data?.role === 9
  } catch (e) { /* 未登录静默 */ }
}

async function loadMode() {
  try {
    const res = await getRecommendMode()
    if (res.data?.mode) activeMode.value = res.data.mode
  } catch (e) { /* 未登录静默 */ }
}

async function switchMode(mode: 'slim' | 'normal' | 'indulge') {
  if (activeMode.value === mode) return
  activeMode.value = mode
  loadingRecommend.value = true
  try {
    await switchRecommendMode(mode)
    await fetchRecommendations()
  } catch (e) {
    uni.showToast({ title: '切换失败', icon: 'none' })
  } finally {
    loadingRecommend.value = false
  }
}

async function fetchRecommendations() {
  loadingRecommend.value = true
  feedbackState.value = 0
  try {
    const res = await getRecommendList()
    if (res.data) {
      recommendData.value = res.data
      buildMerchantMap()
    }
  } catch (e) {
    recommendData.value = null
  } finally {
    loadingRecommend.value = false
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

async function fetchMerchants() {
  loadingMerchants.value = true
  try {
    const res = await getMerchantList({ size: 10 })
    if (res.data?.content) {
      merchantList.value = res.data.content
    }
  } catch (e) {
    console.error(e)
  } finally {
    loadingMerchants.value = false
  }
}

// 构建 merchantId → name 映射，用于推荐卡片商户标签
function buildMerchantMap() {
  const map: Record<string, string> = {}
  merchantList.value.forEach(m => { map[m.id] = m.name })
  merchantMap.value = map
}

function quickOrder(dish: DishInfo) {
  if (!dish.merchantId) {
    uni.showToast({ title: '暂无关联门店', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/merchant/detail?id=${dish.merchantId}` })
}

function goToDetail(id: string) {
  uni.navigateTo({ url: `/pages/merchant/detail?id=${id}` })
}

function goToProfile() {
  uni.switchTab({ url: '/pages/profile/index' })
}

function navigateTo(url: string) {
  uni.navigateTo({ url })
}

// 推荐结果已带门店名称，回退到本地映射以兼容历史接口
function dishMerchantName(dish: DishInfo): string {
  return dish.merchantName || merchantMap.value[dish.merchantId] || ''
}
</script>

<style scoped>
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.location-box {
  display: flex;
  align-items: center;
}
.location-icon {
  font-size: 32rpx;
  margin-right: 8rpx;
}
.location-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #0F172A;
}
.debug-entry {
  font-size: 24rpx;
  background: #EFF6FF;
  color: #2563EB;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

/* 模式切换 tabs */
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

/* 推荐理由卡 */
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

/* 菜品卡片 */
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
  flex-shrink: 0;
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
.dish-merchant-tag {
  margin: 4rpx 0;
}
.merchant-tag-text {
  font-size: 20rpx;
  color: #FF5E00;
  background: #FFF7ED;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}
.dish-tags-row {
  margin: 6rpx 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}
.dish-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4rpx;
}
.price-box {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.price-member {
  font-size: 32rpx;
  font-weight: 800;
  color: #FF5E00;
}
.price-origin {
  font-size: 24rpx;
  color: #94A3B8;
  text-decoration: line-through;
}
.price-value {
  font-size: 32rpx;
  font-weight: 800;
  color: #FF5E00;
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

/* 未登录占位 */
.recommend-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx;
  margin-bottom: 24rpx;
}
.placeholder-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}
.placeholder-text {
  font-size: 26rpx;
  color: #64748B;
  text-align: center;
  margin-bottom: 24rpx;
}
.btn-login-hint {
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 999rpx;
  padding: 12rpx 48rpx;
  border: none;
}

/* 商户列表 */
.merchant-card {
  display: flex;
  padding: 24rpx;
}
.merchant-logo-box {
  width: 110rpx;
  height: 110rpx;
  background: #F1F5F9;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.merchant-logo-emoji {
  font-size: 56rpx;
}
.merchant-info {
  flex: 1;
}
.merchant-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.merchant-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}
.merchant-address {
  font-size: 24rpx;
  color: #64748B;
  margin-bottom: 12rpx;
  display: block;
}
.merchant-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.merchant-status {
  font-size: 22rpx;
  color: #10B981;
  font-weight: 600;
}
.action-arrow {
  font-size: 24rpx;
  color: #FF5E00;
  font-weight: 600;
}
</style>
