<template>
  <view class="page-container">
    <view class="section-header">
      <text class="section-title">我的点餐订单</text>
      <text class="section-extra" @tap="fetchOrders">🔄 刷新</text>
    </view>

    <view v-if="loading" class="empty-container">
      <text class="empty-icon">⏳</text>
      <text class="empty-text">正在查询您的订单...</text>
    </view>

    <view v-else-if="orderList.length === 0" class="empty-container">
      <text class="empty-icon">🧾</text>
      <text class="empty-text">暂无历史订单，快去挑选美食吧</text>
    </view>

    <view v-else class="order-cards-list">
      <view 
        v-for="order in orderList" 
        :key="order.id" 
        class="food-card order-card"
      >
        <!-- 订单头部：门店名与状态流转标签 -->
        <view class="order-card-header">
          <view class="merchant-name-box">
            <text class="order-merchant-name">{{ order.merchantName || '美食门店' }}</text>
            <text class="fulfill-badge">
              {{ order.fulfillType === 0 ? '🛵 外卖' : order.fulfillType === 1 ? '🛍️ 自提' : '🍽️ 堂食' }}
            </text>
          </view>
          <text class="status-badge" :class="'status-' + order.status">
            {{ order.statusDesc }}
          </text>
        </view>

        <!-- 自提/堂食 6位核销码专属票据高光卡片 -->
        <view 
          v-if="(order.fulfillType === 1 || order.fulfillType === 2) && order.status === 1 && order.pickupCode" 
          class="pickup-ticket-box"
        >
          <view class="ticket-header">
            <text class="ticket-title">🎟️ 到店出示核销码（截屏可用）</text>
          </view>
          <view class="ticket-code-row">
            <text class="ticket-code">{{ order.pickupCode }}</text>
          </view>
          <text class="ticket-tip">请向店员出示此 6 位码完成自提取餐或入座核销</text>
        </view>

        <!-- 菜品列表快照 -->
        <view class="order-items-preview">
          <view 
            v-for="(item, idx) in order.items" 
            :key="idx" 
            class="item-preview-row"
          >
            <text class="item-title">{{ item.name }}</text>
            <text class="item-count">x{{ item.quantity }}</text>
            <text class="item-money">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
          </view>
        </view>

        <!-- 订单底部时间与总价 -->
        <view class="order-card-footer">
          <text class="order-time">{{ order.createdAt ? order.createdAt.substring(5, 16) : '' }}</text>
          <view class="order-amount-box">
            <text class="amount-tip">合计实付:</text>
            <text class="price-symbol">¥</text>
            <text class="price-value" style="font-size: 34rpx;">{{ order.totalAmount?.toFixed(2) }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserOrders, type OrderInfo } from '@/api/order'

const loading = ref(false)
const orderList = ref<OrderInfo[]>([])

onMounted(async () => {
  await fetchOrders()
})

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getUserOrders({ size: 20 })
    if (res.data?.content) {
      orderList.value = res.data.content
    }
  } catch (e) {
    uni.showToast({ title: '加载订单失败，请先登录', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.order-card {
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid #F1F5F9;
  padding-bottom: 16rpx;
  margin-bottom: 16rpx;
}
.merchant-name-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.order-merchant-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}
.fulfill-badge {
  font-size: 20rpx;
  background: #F1F5F9;
  color: #475569;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.status-badge {
  font-size: 24rpx;
  font-weight: 700;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.status-0 { color: #F59E0B; background: #FEF3C7; } /* 待支付 */
.status-1 { color: #10B981; background: #D1FAE5; } /* 已支付 */
.status-2 { color: #3B82F6; background: #DBEAFE; } /* 配送中 */
.status-3 { color: #6366F1; background: #EEF2FF; } /* 已核销 */
.status-4 { color: #64748B; background: #F1F5F9; } /* 已完成 */

.pickup-ticket-box {
  background: linear-gradient(135deg, #FFF7ED 0%, #FFEDD5 100%);
  border: 2rpx dashed #FB923C;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  text-align: center;
}
.ticket-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #EA580C;
}
.ticket-code-row {
  margin: 12rpx 0;
}
.ticket-code {
  font-size: 54rpx;
  font-weight: 900;
  letter-spacing: 12rpx;
  color: #C2410C;
  font-family: monospace;
}
.ticket-tip {
  font-size: 20rpx;
  color: #9A3412;
}

.order-items-preview {
  margin-bottom: 16rpx;
}
.item-preview-row {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  color: #334155;
  padding: 8rpx 0;
}
.item-title {
  flex: 1;
}
.item-count {
  color: #94A3B8;
  margin: 0 20rpx;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1rpx solid #F1F5F9;
  padding-top: 16rpx;
}
.order-time {
  font-size: 22rpx;
  color: #94A3B8;
}
.order-amount-box {
  display: flex;
  align-items: baseline;
}
.amount-tip {
  font-size: 24rpx;
  color: #64748B;
  margin-right: 6rpx;
}
</style>

