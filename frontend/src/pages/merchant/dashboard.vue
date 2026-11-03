<template>
  <view class="page-container">
    <!-- 头部门店大盘概览 -->
    <view class="merchant-hero food-card">
      <view class="hero-header">
        <text class="hero-name">{{ dashboard?.merchantName || '商家经营工作台' }}</text>
        <text class="hero-tag">🟢 营业中</text>
      </view>
      <view class="metrics-grid">
        <view class="metric-item">
          <text class="metric-num">{{ dashboard?.todayOrderCount || 0 }}</text>
          <text class="metric-label">今日订单量</text>
        </view>
        <view class="metric-item">
          <text class="metric-num price-color">¥{{ (dashboard?.todayRevenue || 0).toFixed(2) }}</text>
          <text class="metric-label">今日营业额</text>
        </view>
        <view class="metric-item">
          <text class="metric-num">{{ dashboard?.todayVerifiedCount || 0 }}</text>
          <text class="metric-label">今日核销数</text>
        </view>
        <view class="metric-item">
          <text class="metric-num">{{ ((dashboard?.todayVerifyRate || 0) * 100).toFixed(0) }}%</text>
          <text class="metric-label">核销履约率</text>
        </view>
      </view>
    </view>

    <!-- 快捷核销操作区 -->
    <view class="food-card verify-section">
      <view class="section-header">
        <text class="section-title">自提 / 堂食快速核销</text>
        <button class="scan-camera-btn" @tap="handleScanCode">📷 扫码核销</button>
      </view>
      <view class="verify-input-row">
        <input 
          class="input-field verify-code-input" 
          v-model="inputCode" 
          placeholder="手动输入顾客的 6 位核销码" 
          maxlength="6"
          type="number"
        />
        <button 
          class="verify-submit-btn" 
          :loading="verifying"
          :disabled="!inputCode || inputCode.length !== 6" 
          @tap="submitVerify"
        >
          立即核销
        </button>
      </view>
      <text class="verify-tips">支持使用摄像头直接扫顾客小程序二维码，或手动输入顾客出示的 6 位数字码</text>
    </view>

    <!-- 门店订单管理 -->
    <view class="section-header">
      <text class="section-title">门店最新订单 ({{ orders.length }})</text>
      <text class="section-extra" @tap="fetchDashboard">🔄 刷新数据</text>
    </view>

    <view v-if="orders.length === 0" class="empty-container">
      <text class="empty-icon">📭</text>
      <text class="empty-text">今日暂无门店新订单</text>
    </view>

    <view v-else class="order-list">
      <view v-for="order in orders" :key="order.id" class="food-card m-order-card">
        <view class="m-order-header">
          <text class="m-order-no">单号: {{ order.orderNo }}</text>
          <text class="m-order-status" :class="'status-' + order.status">{{ order.statusDesc }}</text>
        </view>
        <view class="m-order-body">
          <text class="m-order-fulfill">
            {{ order.fulfillType === 0 ? '🛵 外卖配送' : order.fulfillType === 1 ? '🛍️ 到店自提' : '🍽️ 堂食就餐' }}
            <text v-if="order.pickupCode" style="color: #FF5E00; font-weight: bold; margin-left: 12rpx;">
              [核销码: {{ order.pickupCode }}]
            </text>
          </text>
          <view class="m-items-summary">
            <text v-for="(i, idx) in order.items" :key="idx" class="m-item-text">
              {{ i.name }} x{{ i.quantity }}
            </text>
          </view>
        </view>
        <view class="m-order-footer">
          <text class="m-order-time">{{ order.createdAt ? order.createdAt.substring(11, 16) : '' }}</text>
          <text class="m-order-price">¥{{ order.totalAmount?.toFixed(2) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMerchantDashboard, verifyOrder, getMerchantOrders, type MerchantDashboard } from '@/api/merchant'

const dashboard = ref<MerchantDashboard | null>(null)
const orders = ref<any[]>([])
const inputCode = ref('')
const verifying = ref(false)

onMounted(async () => {
  await fetchDashboard()
})

async function fetchDashboard() {
  try {
    const res = await getMerchantDashboard()
    if (res.data) {
      dashboard.value = res.data
    }
    const orderRes = await getMerchantOrders({ size: 10 })
    if (orderRes.data?.content) {
      orders.value = orderRes.data.content
    }
  } catch (e) {
    uni.showToast({ title: '获取商家数据失败，请确认是否为已认证商家账号', icon: 'none' })
  }
}

async function submitVerify() {
  if (!inputCode.value || inputCode.value.length !== 6) {
    uni.showToast({ title: '请输入正确的 6 位核销码', icon: 'none' })
    return
  }

  verifying.value = true
  try {
    const res = await verifyOrder(inputCode.value, 1)
    uni.showToast({ title: res.message || '核销成功！', icon: 'success' })
    inputCode.value = ''
    await fetchDashboard()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '核销失败', icon: 'none' })
  } finally {
    verifying.value = false
  }
}

function handleScanCode() {
  // uni.scanCode 跨端相机扫码标准封装
  uni.scanCode({
    onlyFromCamera: true,
    success: async (res) => {
      const code = res.result
      if (code) {
        inputCode.value = code
        await submitVerify()
      }
    },
    fail: () => {
      uni.showToast({ title: '已取消扫码', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.merchant-hero {
  background: linear-gradient(145deg, #1E293B 0%, #0F172A 100%);
  color: #FFFFFF;
  padding: 32rpx;
}
.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.hero-name {
  font-size: 34rpx;
  font-weight: 800;
}
.hero-tag {
  font-size: 20rpx;
  background: rgba(16, 185, 129, 0.2);
  color: #34D399;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.metrics-grid {
  display: flex;
  justify-content: space-around;
  text-align: center;
  border-top: 1rpx solid rgba(255, 255, 255, 0.1);
  padding-top: 20rpx;
}
.metric-item {
  display: flex;
  flex-direction: column;
}
.metric-num {
  font-size: 36rpx;
  font-weight: 800;
  margin-bottom: 4rpx;
}
.price-color {
  color: #FB923C;
}
.metric-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.verify-section {
  padding: 24rpx;
}
.scan-camera-btn {
  background: #EFF6FF;
  color: #2563EB;
  font-size: 24rpx;
  font-weight: 600;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  margin: 0;
}
.verify-input-row {
  display: flex;
  gap: 16rpx;
  margin: 16rpx 0;
}
.verify-code-input {
  flex: 1;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
  margin-bottom: 0;
}
.verify-submit-btn {
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 700;
  padding: 0 32rpx;
  border-radius: 16rpx;
  margin: 0;
  display: flex;
  align-items: center;
}
.verify-tips {
  font-size: 20rpx;
  color: #94A3B8;
  display: block;
}

.m-order-card {
  padding: 20rpx;
  margin-bottom: 16rpx;
}
.m-order-header {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  border-bottom: 1rpx solid #F1F5F9;
  padding-bottom: 12rpx;
}
.m-order-no {
  color: #64748B;
}
.m-order-status {
  font-weight: 700;
}
.status-1 { color: #10B981; }
.status-3 { color: #6366F1; }

.m-order-body {
  padding: 12rpx 0;
}
.m-order-fulfill {
  font-size: 26rpx;
  color: #0F172A;
  font-weight: 600;
  margin-bottom: 6rpx;
  display: block;
}
.m-item-text {
  font-size: 24rpx;
  color: #64748B;
  margin-right: 16rpx;
}

.m-order-footer {
  display: flex;
  justify-content: space-between;
  border-top: 1rpx dashed #E2E8F0;
  padding-top: 12rpx;
}
.m-order-time {
  font-size: 22rpx;
  color: #94A3B8;
}
.m-order-price {
  font-size: 28rpx;
  font-weight: 800;
  color: #FF3D00;
}
</style>

