<template>
  <view class="page-container">
    <!-- 顶部定位与搜索/快捷入口 -->
    <view class="home-header">
      <view class="location-box">
        <text class="location-icon">📍</text>
        <text class="location-text">天府软件园 · 研发中心</text>
      </view>
      <view class="debug-entry" @tap="navigateTo('/pages/index/index')">
        <text>⚡ 联调看板</text>
      </view>
    </view>

    <!-- AI 饭搭子特色快捷卡片 -->
    <view class="ai-banner food-card-featured" @tap="navigateTo('/pages/recommend/index')">
      <view class="ai-banner-left">
        <view class="ai-title-row">
          <text class="ai-badge">AI 智能体</text>
          <text class="ai-title">今天吃什么？</text>
        </view>
        <text class="ai-subtitle">根据卡路里目标与口味偏好，秒级精选搭配</text>
        <view class="ai-btn">立即推荐 ›</view>
      </view>
      <view class="ai-icon-large">🥗</view>
    </view>

    <!-- 模式快速选择器 -->
    <view class="mode-bar">
      <view class="mode-pill" :class="{ active: currentMode === 'slim' }" @tap="handleSwitchMode('slim')">
        <text class="mode-icon">🌱</text>
        <text class="mode-label">减脂≤500卡</text>
      </view>
      <view class="mode-pill" :class="{ active: currentMode === 'normal' }" @tap="handleSwitchMode('normal')">
        <text class="mode-icon">🍱</text>
        <text class="mode-label">均衡≤800卡</text>
      </view>
      <view class="mode-pill" :class="{ active: currentMode === 'indulge' }" @tap="handleSwitchMode('indulge')">
        <text class="mode-icon">🍔</text>
        <text class="mode-label">多巴胺放纵</text>
      </view>
    </view>

    <!-- 附近热门商家 -->
    <view class="section-header">
      <text class="section-title">精选入驻好店</text>
      <text class="section-extra" @tap="navigateTo('/pages/recommend/chat')">💬 问AI饭搭子</text>
    </view>

    <view v-if="loading" class="empty-container">
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
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMerchantList, type MerchantInfo } from '@/api/merchant'
import { getRecommendMode, switchRecommendMode } from '@/api/recommend'

const loading = ref(false)
const merchantList = ref<MerchantInfo[]>([])
const currentMode = ref<'slim' | 'normal' | 'indulge'>('normal')

onMounted(async () => {
  await fetchMode()
  await fetchMerchants()
})

async function fetchMode() {
  try {
    const res = await getRecommendMode()
    if (res.data?.mode) {
      currentMode.value = res.data.mode
    }
  } catch (e) {
    // 未登录时静默兼容
  }
}

async function handleSwitchMode(mode: 'slim' | 'normal' | 'indulge') {
  currentMode.value = mode
  try {
    await switchRecommendMode(mode)
    uni.showToast({ title: `已切换至 ${mode === 'slim' ? '减脂' : mode === 'indulge' ? '放纵' : '均衡'}模式`, icon: 'none' })
  } catch (e) {
    uni.showToast({ title: '登录后可保存模式偏好', icon: 'none' })
  }
}

async function fetchMerchants() {
  loading.value = true
  try {
    const res = await getMerchantList({ size: 10 })
    if (res.data?.content) {
      merchantList.value = res.data.content
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goToDetail(id: string) {
  uni.navigateTo({ url: `/pages/merchant/detail?id=${id}` })
}

function navigateTo(url: string) {
  uni.navigateTo({ url })
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

.ai-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-radius: 28rpx;
  margin-bottom: 24rpx;
}
.ai-badge {
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 20rpx;
  font-weight: 700;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  margin-right: 12rpx;
}
.ai-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.ai-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #0F172A;
}
.ai-subtitle {
  font-size: 24rpx;
  color: #64748B;
  margin-bottom: 16rpx;
  display: block;
}
.ai-btn {
  display: inline-block;
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 600;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
}
.ai-icon-large {
  font-size: 84rpx;
}

.mode-bar {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}
.mode-pill {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #FFFFFF;
  padding: 18rpx 0;
  border-radius: 20rpx;
  border: 2rpx solid #E2E8F0;
  box-shadow: 0 4rpx 12rpx rgba(15, 23, 42, 0.03);
  transition: all 0.2s;
}
.mode-pill.active {
  border-color: #FF5E00;
  background: #FFF7ED;
}
.mode-icon {
  font-size: 32rpx;
  margin-bottom: 6rpx;
}
.mode-label {
  font-size: 22rpx;
  font-weight: 600;
  color: #334155;
}
.mode-pill.active .mode-label {
  color: #FF5E00;
}

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

