<template>
  <view class="page-container order-detail-page">
    <view class="section-header">
      <text class="section-title">订单详情与履约凭据</text>
      <text class="section-extra" @tap="fetchDetail">🔄 刷新</text>
    </view>

    <view v-if="loading" class="empty-container">
      <text class="empty-icon">⏳</text>
      <text class="empty-text">正在查询订单详情...</text>
    </view>

    <view v-else-if="!order" class="empty-container">
      <text class="empty-icon">❓</text>
      <text class="empty-text">未找到相关订单信息</text>
      <button class="back-btn" @tap="goToOrderList">返回订单列表</button>
    </view>

    <view v-else class="detail-content">
      <!-- 状态与订单号卡片 -->
      <view class="food-card status-card">
        <view class="status-main">
          <text class="status-icon">
            {{ order.status === 1 ? '✅' : order.status === 2 ? '🎉' : '⏱️' }}
          </text>
          <view class="status-texts">
            <text class="status-title">{{ order.statusDesc || '已支付' }}</text>
            <text class="status-desc">
              {{ order.fulfillType === 0 ? '餐品制作中，稍后由骑手配送' : '餐品制作中，可凭下方核销码取餐/入座' }}
            </text>
          </view>
        </view>
        <view class="order-no-row">
          <text class="label">订单编号：</text>
          <text class="val">{{ order.orderNo }}</text>
        </view>
      </view>

      <!-- 自提/堂食 6 位核销凭据高亮大卡片 -->
      <view 
        v-if="(order.fulfillType === 1 || order.fulfillType === 2) && order.status === 1 && order.pickupCode" 
        class="food-card ticket-card"
      >
        <view class="ticket-badge">
          {{ order.fulfillType === 1 ? '🛍️ 到店自提凭证' : '🍽️ 堂食就餐凭证' }}
        </view>
        <view class="ticket-code-display">
          <text class="ticket-num">{{ order.pickupCode }}</text>
        </view>
        <text class="ticket-subtip">请向门店出示此 6 位核销码完成取餐或核销</text>
      </view>

      <!-- 菜品明细清单与价格汇总 -->
      <view class="food-card items-card">
        <view class="merchant-info-row">
          <text class="m-icon">🏪</text>
          <text class="m-name">{{ order.merchantName || '美食门店' }}</text>
          <text class="fulfill-tag">
            {{ order.fulfillType === 0 ? '外卖配送' : order.fulfillType === 1 ? '到店自提' : '堂食就餐' }}
          </text>
        </view>

        <view class="dish-item-list">
          <view 
            v-for="(item, idx) in order.items" 
            :key="idx" 
            class="dish-item-row"
          >
            <view class="dish-name-col">
              <text class="name">{{ item.name }}</text>
              <text class="qty">x {{ item.quantity }}</text>
            </view>
            <text class="price">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
          </view>
        </view>

        <!-- 价格汇总 -->
        <view class="price-summary-box">
          <view class="summary-line">
            <text class="s-label">商品总额</text>
            <text class="s-val">¥{{ (order.totalAmount || 0).toFixed(2) }}</text>
          </view>
          <view class="summary-line highlight">
            <text class="s-label">实付金额</text>
            <text class="s-val-large">¥{{ (order.totalAmount || 0).toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <!-- 履约信息与时间戳 -->
      <view class="food-card info-card">
        <view class="info-line" v-if="order.address">
          <text class="info-label">履约地址/备注：</text>
          <text class="info-val">{{ order.address }}</text>
        </view>
        <view class="info-line">
          <text class="info-label">创建时间：</text>
          <text class="info-val">{{ formatTime(order.createdAt) }}</text>
        </view>
        <view class="info-line" v-if="order.paymentTime">
          <text class="info-label">支付时间：</text>
          <text class="info-val">{{ formatTime(order.paymentTime) }}</text>
        </view>
      </view>

      <!-- 底部快捷导航 -->
      <view class="action-buttons">
        <button class="nav-btn secondary" @tap="goToHome">返回首页</button>
        <button class="nav-btn primary" @tap="goToOrderList">查看所有订单</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderDetail, type OrderResponse } from '@/api/order'

const orderId = ref('')
const order = ref<OrderResponse | null>(null)
const loading = ref(false)

onMounted(async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1] as any
  const options = currentPage.options || {}
  orderId.value = options.id || ''

  if (orderId.value) {
    await fetchDetail()
  }
})

async function fetchDetail() {
  if (!orderId.value) return
  loading.value = true
  try {
    const res = await getOrderDetail(orderId.value)
    if (res.data) {
      order.value = res.data
    }
  } catch (e) {
    uni.showToast({ title: '加载订单详情失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function formatTime(isoStr?: string) {
  if (!isoStr) return '--'
  return isoStr.replace('T', ' ').slice(0, 19)
}

function goToHome() {
  uni.switchTab({ url: '/pages/home/index' })
}

function goToOrderList() {
  uni.switchTab({ url: '/pages/order/list' })
}
</script>

<style scoped>
.order-detail-page {
  padding-bottom: 60rpx;
}

.status-card {
  padding: 30rpx;
  margin-bottom: 24rpx;
}
.status-main {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 20rpx;
}
.status-icon {
  font-size: 54rpx;
}
.status-texts {
  display: flex;
  flex-direction: column;
}
.status-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #0F172A;
}
.status-desc {
  font-size: 24rpx;
  color: #64748B;
  margin-top: 4rpx;
}
.order-no-row {
  display: flex;
  font-size: 24rpx;
  color: #94A3B8;
  border-top: 1rpx solid #F1F5F9;
  padding-top: 16rpx;
}
.order-no-row .val {
  color: #475569;
  font-family: monospace;
}

.ticket-card {
  background: linear-gradient(135deg, #FFF7ED 0%, #FFEDD5 100%);
  border: 2rpx dashed #FF5E00;
  text-align: center;
  padding: 36rpx 24rpx;
  margin-bottom: 24rpx;
}
.ticket-badge {
  display: inline-block;
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 22rpx;
  font-weight: 700;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  margin-bottom: 20rpx;
}
.ticket-code-display {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 20rpx 0;
  box-shadow: 0 4rpx 12rpx rgba(255, 94, 0, 0.12);
  margin: 0 40rpx;
}
.ticket-num {
  font-size: 68rpx;
  font-weight: 900;
  letter-spacing: 12rpx;
  color: #FF5E00;
  font-family: monospace;
}
.ticket-subtip {
  display: block;
  font-size: 24rpx;
  color: #9A3412;
  margin-top: 20rpx;
}

.items-card {
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.merchant-info-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-bottom: 1rpx solid #F1F5F9;
  padding-bottom: 16rpx;
  margin-bottom: 20rpx;
}
.m-icon {
  font-size: 32rpx;
}
.m-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
  flex: 1;
}
.fulfill-tag {
  font-size: 20rpx;
  background: #F1F5F9;
  color: #475569;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.dish-item-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}
.dish-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.dish-name-col {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.dish-name-col .name {
  font-size: 28rpx;
  color: #334155;
}
.dish-name-col .qty {
  font-size: 24rpx;
  color: #94A3B8;
}
.dish-item-row .price {
  font-size: 28rpx;
  font-weight: 700;
  color: #0F172A;
}

.price-summary-box {
  margin-top: 24rpx;
  border-top: 1rpx dashed #E2E8F0;
  padding-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.summary-line {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  color: #64748B;
}
.summary-line.highlight {
  align-items: baseline;
  margin-top: 10rpx;
}
.summary-line.highlight .s-label {
  font-size: 28rpx;
  font-weight: 700;
  color: #0F172A;
}
.s-val-large {
  font-size: 38rpx;
  font-weight: 800;
  color: #FF5E00;
}

.info-card {
  padding: 24rpx 28rpx;
  margin-bottom: 36rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}
.info-line {
  display: flex;
  font-size: 24rpx;
  line-height: 1.6;
}
.info-label {
  color: #94A3B8;
  width: 180rpx;
  flex-shrink: 0;
}
.info-val {
  color: #334155;
  flex: 1;
}

.action-buttons {
  display: flex;
  gap: 20rpx;
}
.nav-btn {
  flex: 1;
  font-size: 28rpx;
  font-weight: 700;
  padding: 20rpx 0;
  border-radius: 999rpx;
  text-align: center;
}
.nav-btn.primary {
  background: #FF5E00;
  color: #FFFFFF;
}
.nav-btn.secondary {
  background: #FFFFFF;
  color: #475569;
  border: 1rpx solid #E2E8F0;
}
.back-btn {
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 26rpx;
  padding: 12rpx 36rpx;
  border-radius: 999rpx;
  margin-top: 24rpx;
}
</style>

