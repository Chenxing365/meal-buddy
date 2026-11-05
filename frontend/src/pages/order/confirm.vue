<template>
  <view class="page-container">
    <view class="section-header">
      <text class="section-title">确认订单与履约方式</text>
    </view>

    <!-- 履约方式选择卡片 -->
    <view class="food-card">
      <view class="fulfill-selector">
        <view 
          class="fulfill-option" 
          :class="{ active: fulfillType === 1 }"
          @tap="fulfillType = 1"
        >
          <text class="fulfill-icon">🛍️</text>
          <text class="fulfill-name">到店自提</text>
          <text class="fulfill-sub">凭6位码取餐</text>
        </view>
        <view 
          class="fulfill-option" 
          :class="{ active: fulfillType === 2 }"
          @tap="fulfillType = 2"
        >
          <text class="fulfill-icon">🍽️</text>
          <text class="fulfill-name">堂食就餐</text>
          <text class="fulfill-sub">到店报码入座</text>
        </view>
        <view 
          class="fulfill-option" 
          :class="{ active: fulfillType === 0 }"
          @tap="fulfillType = 0"
        >
          <text class="fulfill-icon">🛵</text>
          <text class="fulfill-name">外卖配送</text>
          <text class="fulfill-sub">专人极速送达</text>
        </view>
      </view>

      <!-- 地址或桌号输入 -->
      <view class="address-input-box">
        <input 
          class="input-field" 
          v-model="address" 
          :placeholder="fulfillType === 0 ? '请填写详细外卖收货地址...' : '可备注就餐桌号或取餐偏好（选填）'"
        />
      </view>
    </view>

    <!-- 下单菜品明细清单 -->
    <view class="food-card">
      <view class="merchant-header-row">
        <text class="merchant-tag">🏪 门店</text>
        <text class="merchant-title">{{ orderDraft?.merchantName || '精选美食门店' }}</text>
      </view>

      <view class="order-items-list">
        <view 
          v-for="item in (orderDraft?.items || [])" 
          :key="item.dishId" 
          class="order-item-row"
        >
          <text class="item-name">{{ item.name }}</text>
          <text class="item-qty">x {{ item.quantity }}</text>
          <view class="item-price">
            <text class="price-symbol">¥</text>
            <text class="price-value" style="font-size: 30rpx;">{{ (item.price * item.quantity).toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <view class="order-summary-row">
        <text class="summary-label">小计金额</text>
        <view class="summary-price">
          <text class="price-symbol">¥</text>
          <text class="price-value">{{ (orderDraft?.totalAmount || 0).toFixed(2) }}</text>
        </view>
      </view>
    </view>

    <!-- 底部固定结算提交栏 -->
    <view class="bottom-submit-bar">
      <view class="submit-price-box">
        <text class="pay-label">实付金额:</text>
        <text class="price-symbol">¥</text>
        <text class="price-value" style="color: #FF3D00;">{{ (orderDraft?.totalAmount || 0).toFixed(2) }}</text>
      </view>
      <button 
        class="pay-submit-btn" 
        :loading="submitting" 
        @tap="handleCreateAndPayOrder"
      >
        模拟免密支付
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createOrder, payOrder } from '@/api/order'

const submitting = ref(false)
const fulfillType = ref(1) // 默认自提
const address = ref('')
const orderDraft = ref<any>(null)

onMounted(() => {
  const draft = uni.getStorageSync('__current_order_draft__')
  if (draft) {
    orderDraft.value = draft
  }
})

async function handleCreateAndPayOrder() {
  if (!orderDraft.value || !orderDraft.value.items?.length) {
    uni.showToast({ title: '购物车暂无菜品', icon: 'none' })
    return
  }

  if (fulfillType.value === 0 && !address.value.trim()) {
    uni.showToast({ title: '外卖订单请填写收货地址', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    // 1. 创建订单
    const createRes = await createOrder({
      merchantId: orderDraft.value.merchantId,
      fulfillType: fulfillType.value,
      address: address.value,
      items: orderDraft.value.items.map((i: any) => ({
        dishId: i.dishId,
        quantity: i.quantity
      }))
    })

    const orderId = createRes.data?.id
    if (!orderId) {
      throw new Error('下单失败')
    }

    // 2. 模拟自动免密支付
    await payOrder(orderId)

    uni.removeStorageSync('__current_order_draft__')
    uni.showToast({ title: '支付成功！', icon: 'success' })

    // 核心修复：自动跳转至该笔订单的专属详情/凭据页
    setTimeout(() => {
      uni.redirectTo({ 
        url: `/pages/order/detail?id=${orderId}` 
      })
    }, 800)

  } catch (e) {
    uni.showToast({ title: (e as Error).message || '下单支付失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.fulfill-selector {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.fulfill-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
  border-radius: 16rpx;
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  transition: all 0.2s;
}
.fulfill-option.active {
  border-color: #FF5E00;
  background: #FFF7ED;
}
.fulfill-icon {
  font-size: 36rpx;
  margin-bottom: 4rpx;
}
.fulfill-name {
  font-size: 26rpx;
  font-weight: 700;
  color: #334155;
}
.fulfill-option.active .fulfill-name {
  color: #FF5E00;
}
.fulfill-sub {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 2rpx;
}

.merchant-header-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.merchant-tag {
  font-size: 24rpx;
  font-weight: bold;
}
.merchant-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0F172A;
}

.order-items-list {
  border-bottom: 1rpx dashed #E2E8F0;
  padding-bottom: 20rpx;
  margin-bottom: 20rpx;
}
.order-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
}
.item-name {
  flex: 1;
  font-size: 28rpx;
  color: #1E293B;
}
.item-qty {
  font-size: 24rpx;
  color: #64748B;
  margin: 0 24rpx;
}

.order-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-label {
  font-size: 26rpx;
  color: #64748B;
}

.bottom-submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #FFFFFF;
  border-top: 1rpx solid #E2E8F0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 32rpx 40rpx;
  box-shadow: 0 -4rpx 16rpx rgba(15, 23, 42, 0.05);
}
.submit-price-box {
  display: flex;
  align-items: baseline;
}
.pay-label {
  font-size: 26rpx;
  color: #64748B;
  margin-right: 8rpx;
}
.pay-submit-btn {
  background: linear-gradient(135deg, #FF6B00 0%, #FF3D00 100%);
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 700;
  padding: 18rpx 44rpx;
  border-radius: 999rpx;
  margin: 0;
}
</style>

