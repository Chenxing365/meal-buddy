<template>
  <view class="merchant-detail-page">
    <view v-if="loading" class="empty-container">
      <text class="empty-icon">⏳</text>
      <text class="empty-text">正在加载门店详情与菜品...</text>
    </view>

    <view v-else-if="merchant">
      <!-- 门店头部信息卡片 -->
      <view class="merchant-hero">
        <view class="hero-top">
          <text class="hero-emoji">🏬</text>
          <view class="hero-info">
            <text class="hero-name">{{ merchant.name }}</text>
            <view class="hero-tags">
              <text class="tag-badge tag-flavor">{{ merchant.category }}</text>
              <text class="tag-badge tag-calorie">抽成 {{ (merchant.commissionRate * 100).toFixed(0) }}%</text>
            </view>
            <text class="hero-address">📍 {{ merchant.address }}</text>
          </view>
        </view>
      </view>

      <!-- 菜品列表区 -->
      <view class="dish-section">
        <view class="section-header">
          <text class="section-title">门店招牌菜品</text>
          <text class="section-extra">{{ dishes.length }} 款热销好味</text>
        </view>

        <view v-if="dishes.length === 0" class="empty-container">
          <text class="empty-icon">🥘</text>
          <text class="empty-text">本店菜品正在上架中，敬请期待</text>
        </view>

        <view v-else class="dish-item-list">
          <view 
            v-for="dish in dishes" 
            :key="dish.id" 
            class="food-card dish-row-card"
          >
            <view class="dish-avatar-box">
              <text class="dish-avatar-emoji">🥗</text>
            </view>
            <view class="dish-content">
              <text class="dish-title">{{ dish.name }}</text>
              <text class="dish-desc" v-if="dish.description">{{ dish.description }}</text>
              <view class="dish-badges">
                <text class="tag-badge tag-calorie">🔥 {{ dish.calorie }} 大卡</text>
                <text 
                  v-for="tag in (dish.flavorTags || [])" 
                  :key="tag" 
                  class="tag-badge tag-flavor"
                >
                  {{ tag }}
                </text>
              </view>
              <view class="dish-action-row">
                <view class="price-box">
                  <text class="price-symbol">¥</text>
                  <text class="price-value">{{ dish.price }}</text>
                </view>
                <!-- 加减购物车控制器 -->
                <view class="cart-stepper">
                  <text 
                    v-if="cart[dish.id]" 
                    class="step-btn minus" 
                    @tap="decreaseCart(dish)"
                  >
                    -
                  </text>
                  <text v-if="cart[dish.id]" class="step-num">{{ cart[dish.id].quantity }}</text>
                  <text class="step-btn plus" @tap="increaseCart(dish)">+</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部购物车结算浮层 -->
      <view class="cart-bar" v-if="totalCartCount > 0">
        <view class="cart-info">
          <view class="cart-badge-icon">
            <text class="cart-icon">🛒</text>
            <text class="cart-count">{{ totalCartCount }}</text>
          </view>
          <view class="cart-total">
            <text class="total-label">合计:</text>
            <text class="price-symbol">¥</text>
            <text class="price-value total-val">{{ totalCartPrice.toFixed(2) }}</text>
          </view>
        </view>
        <button class="checkout-btn" @tap="goToConfirmOrder">去结算</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMerchantDetail, type MerchantInfo } from '@/api/merchant'
import type { DishInfo } from '@/api/dish'

const loading = ref(true)
const merchantId = ref('')
const merchant = ref<MerchantInfo | null>(null)
const dishes = ref<DishInfo[]>([])

// 购物车状态结构：dishId -> { dish, quantity }
const cart = ref<Record<string, { dish: DishInfo; quantity: number }>>({})

const totalCartCount = computed(() => {
  return Object.values(cart.value).reduce((sum, item) => sum + item.quantity, 0)
})

const totalCartPrice = computed(() => {
  return Object.values(cart.value).reduce((sum, item) => sum + (item.dish.price * item.quantity), 0)
})

onMounted(async () => {
  // 解析页面参数
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1] as any
  const options = currentPage.options || {}
  merchantId.value = options.id || ''

  if (merchantId.value) {
    await fetchDetail(merchantId.value)
  } else {
    // 默认查第一个门店
    await fetchDetail('default')
  }
})

async function fetchDetail(id: string) {
  loading.value = true
  try {
    const res = await getMerchantDetail(id)
    if (res.data) {
      merchant.value = res.data.merchant
      dishes.value = res.data.dishes || []
    }
  } catch (e) {
    uni.showToast({ title: '加载门店失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function increaseCart(dish: DishInfo) {
  if (!cart.value[dish.id]) {
    cart.value[dish.id] = { dish, quantity: 1 }
  } else {
    cart.value[dish.id].quantity++
  }
}

function decreaseCart(dish: DishInfo) {
  if (cart.value[dish.id]) {
    cart.value[dish.id].quantity--
    if (cart.value[dish.id].quantity <= 0) {
      delete cart.value[dish.id]
    }
  }
}

function goToConfirmOrder() {
  const items = Object.values(cart.value).map(item => ({
    dishId: item.dish.id,
    name: item.dish.name,
    price: item.dish.price,
    quantity: item.quantity
  }))

  const orderDraft = {
    merchantId: merchant.value?.id,
    merchantName: merchant.value?.name,
    items,
    totalAmount: totalCartPrice.value
  }

  uni.setStorageSync('__current_order_draft__', orderDraft)
  uni.navigateTo({ url: '/pages/order/confirm' })
}
</script>

<style scoped>
.merchant-detail-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding-bottom: 160rpx;
}

.merchant-hero {
  background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%);
  color: #FFFFFF;
  padding: 40rpx 32rpx;
}
.hero-top {
  display: flex;
  align-items: center;
}
.hero-emoji {
  font-size: 72rpx;
  margin-right: 20rpx;
}
.hero-name {
  font-size: 36rpx;
  font-weight: 800;
  display: block;
  margin-bottom: 8rpx;
}
.hero-tags {
  margin-bottom: 8rpx;
}
.hero-address {
  font-size: 22rpx;
  color: #94A3B8;
  display: block;
}

.dish-section {
  padding: 24rpx;
}
.dish-row-card {
  display: flex;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.dish-avatar-box {
  width: 140rpx;
  height: 140rpx;
  background: #F1F5F9;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}
.dish-avatar-emoji {
  font-size: 64rpx;
}
.dish-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.dish-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}
.dish-desc {
  font-size: 22rpx;
  color: #64748B;
  margin: 4rpx 0;
}
.dish-action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10rpx;
}

.cart-stepper {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.step-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: bold;
}
.step-btn.plus {
  background: #FF5E00;
  color: #FFFFFF;
}
.step-btn.minus {
  background: #E2E8F0;
  color: #475569;
}
.step-num {
  font-size: 28rpx;
  font-weight: 700;
}

.cart-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  background: #1E293B;
  border-radius: 999rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx;
  box-shadow: 0 16rpx 36rpx rgba(15, 23, 42, 0.25);
}
.cart-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.cart-badge-icon {
  position: relative;
  background: #FF5E00;
  width: 72rpx;
  height: 72rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cart-icon {
  font-size: 36rpx;
}
.cart-count {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  background: #EF4444;
  color: #FFFFFF;
  font-size: 20rpx;
  font-weight: bold;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
}
.cart-total {
  display: flex;
  align-items: baseline;
}
.total-label {
  color: #94A3B8;
  font-size: 24rpx;
  margin-right: 8rpx;
}
.total-val {
  color: #FFFFFF;
}
.checkout-btn {
  background: linear-gradient(135deg, #FF6B00 0%, #FF3D00 100%);
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 700;
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
  margin: 0;
}
</style>

