<template>
  <view class="page-container">
    <!-- 未登录状态卡片（登录 / 注册切换） -->
    <view v-if="!isLoggedIn" class="food-card auth-card">
      <view class="auth-tabs">
        <text 
          class="auth-tab" 
          :class="{ active: authMode === 'login' }"
          @tap="authMode = 'login'"
        >
          账号登录
        </text>
        <text 
          class="auth-tab" 
          :class="{ active: authMode === 'register' }"
          @tap="authMode = 'register'"
        >
          快速注册
        </text>
      </view>

      <view class="auth-form">
        <input class="input-field" v-model="authForm.username" placeholder="请输入用户名（4~20位字母数字）" />
        <input class="input-field" v-model="authForm.password" password placeholder="请输入登录密码" />
        <input v-if="authMode === 'register'" class="input-field" v-model="authForm.nickname" placeholder="用户昵称（选填）" />
        <input v-if="authMode === 'register'" class="input-field" v-model="authForm.phone" placeholder="手机号（加密存储，选填）" />
        
        <button 
          class="btn-primary-gradient auth-submit-btn" 
          :loading="submitting"
          @tap="handleAuthSubmit"
        >
          {{ authMode === 'login' ? '登 录' : '注 册 并 登 录' }}
        </button>
      </view>
    </view>

    <!-- 已登录用户资料卡片 -->
    <view v-else>
      <view class="food-card user-profile-card">
        <view class="profile-header">
          <view class="profile-avatar">👤</view>
          <view class="profile-info">
            <text class="profile-name">{{ userInfo?.nickname || userInfo?.username || '美食探险家' }}</text>
            <view class="role-badge-row">
              <text class="role-badge">{{ getRoleDesc(userInfo?.role) }}</text>
              <text class="username-sub">@{{ userInfo?.username }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 饮食偏好与卡路里目标设置卡片 -->
      <view class="food-card">
        <view class="section-header">
          <text class="section-title">🥗 AI 饮食偏好设定</text>
          <text class="section-extra" @tap="savePreferences">保存设定</text>
        </view>

        <view class="pref-field-row">
          <text class="pref-label">AI 推荐模式</text>
          <view class="pref-mode-selector">
            <text 
              class="pref-mode-opt" 
              :class="{ selected: preferences.recommendMode === 'slim' }"
              @tap="preferences.recommendMode = 'slim'"
            >
              减脂
            </text>
            <text 
              class="pref-mode-opt" 
              :class="{ selected: preferences.recommendMode === 'normal' }"
              @tap="preferences.recommendMode = 'normal'"
            >
              均衡
            </text>
            <text 
              class="pref-mode-opt" 
              :class="{ selected: preferences.recommendMode === 'indulge' }"
              @tap="preferences.recommendMode = 'indulge'"
            >
              放纵
            </text>
          </view>
        </view>

        <view class="pref-field-row">
          <text class="pref-label">单餐热量上限 (大卡)</text>
          <input 
            class="input-field pref-input" 
            type="number" 
            v-model.number="preferences.calorieLimit" 
            placeholder="如 500、800"
          />
        </view>

        <view class="pref-field-row">
          <text class="pref-label">偏好口味标签 (逗号分隔)</text>
          <input 
            class="input-field pref-input" 
            v-model="flavorTagsInput" 
            placeholder="如 川味, 清淡, 少油"
          />
        </view>

        <view class="pref-field-row">
          <text class="pref-label">忌口/过敏原标签</text>
          <input 
            class="input-field pref-input" 
            v-model="allergyTagsInput" 
            placeholder="如 花生, 海鲜, 香菜"
          />
        </view>
      </view>

      <!-- 快捷角色入口卡片 -->
      <view class="food-card quick-links-card">
        <view class="quick-link-item" @tap="navigateTo('/pages/merchant/dashboard')">
          <text class="link-icon">🏪</text>
          <view class="link-text-box">
            <text class="link-title">商家工作台</text>
            <text class="link-sub">核销自提订单、查看经营流水</text>
          </view>
          <text class="link-arrow">›</text>
        </view>
        <view class="quick-link-item" @tap="navigateTo('/pages/admin/index')">
          <text class="link-icon">🛡️</text>
          <view class="link-text-box">
            <text class="link-title">平台管理后台</text>
            <text class="link-sub">审核入驻申请、平台数据监控</text>
          </view>
          <text class="link-arrow">›</text>
        </view>
      </view>

      <!-- 退出登录按钮 -->
      <button class="logout-btn" @tap="handleLogout">退出当前账号</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
  login, 
  register, 
  getUserInfo, 
  getUserPreferences, 
  updateUserPreferences, 
  logout, 
  type UserInfo, 
  type UserPreferences 
} from '@/api/user'

const isLoggedIn = ref(false)
const authMode = ref<'login' | 'register'>('login')
const submitting = ref(false)

const authForm = ref({
  username: '',
  password: '',
  nickname: '',
  phone: ''
})

const userInfo = ref<UserInfo | null>(null)
const preferences = ref<UserPreferences>({
  recommendMode: 'normal',
  calorieLimit: 800,
  flavorTags: [],
  allergyTags: []
})

const flavorTagsInput = ref('')
const allergyTagsInput = ref('')

onMounted(async () => {
  await checkLogin()
})

async function checkLogin() {
  try {
    const res = await getUserInfo()
    if (res.data) {
      userInfo.value = res.data
      isLoggedIn.value = true
      await fetchPreferences()
    }
  } catch (e) {
    isLoggedIn.value = false
  }
}

async function handleAuthSubmit() {
  if (!authForm.value.username.trim() || !authForm.value.password.trim()) {
    uni.showToast({ title: '请填写用户名和密码', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    if (authMode.value === 'login') {
      await login({
        username: authForm.value.username,
        password: authForm.value.password
      })
      uni.showToast({ title: '登录成功 ✓', icon: 'success' })
    } else {
      await register({
        username: authForm.value.username,
        password: authForm.value.password,
        nickname: authForm.value.nickname,
        phone: authForm.value.phone
      })
      await login({
        username: authForm.value.username,
        password: authForm.value.password
      })
      uni.showToast({ title: '注册并登录成功 ✓', icon: 'success' })
    }
    await checkLogin()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '操作失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function fetchPreferences() {
  try {
    const res = await getUserPreferences()
    if (res.data) {
      preferences.value = res.data
      flavorTagsInput.value = (res.data.flavorTags || []).join(', ')
      allergyTagsInput.value = (res.data.allergyTags || []).join(', ')
    }
  } catch (e) {
    console.error(e)
  }
}

async function savePreferences() {
  try {
    const flavors = flavorTagsInput.value
      .split(/[,，]/)
      .map(s => s.trim())
      .filter(Boolean)
    const allergies = allergyTagsInput.value
      .split(/[,，]/)
      .map(s => s.trim())
      .filter(Boolean)

    await updateUserPreferences({
      recommendMode: preferences.value.recommendMode,
      calorieLimit: preferences.value.calorieLimit,
      flavorTags: flavors,
      allergyTags: allergies
    })
    uni.showToast({ title: '饮食偏好已保存 ✓', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

function handleLogout() {
  logout()
  isLoggedIn.value = false
  userInfo.value = null
  uni.showToast({ title: '已退出登录', icon: 'none' })
}

function getRoleDesc(role?: number): string {
  if (role === 9) return '平台管理员'
  if (role === 2) return '已入驻商家'
  if (role === 1) return '配送骑手'
  return '普通会员'
}

function navigateTo(url: string) {
  uni.navigateTo({ url })
}
</script>

<style scoped>
.auth-tabs {
  display: flex;
  border-bottom: 2rpx solid #E2E8F0;
  margin-bottom: 32rpx;
}
.auth-tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 30rpx;
  font-weight: 700;
  color: #64748B;
}
.auth-tab.active {
  color: #FF5E00;
  border-bottom: 4rpx solid #FF5E00;
}
.auth-submit-btn {
  margin-top: 16rpx;
  width: 100%;
}

.profile-header {
  display: flex;
  align-items: center;
}
.profile-avatar {
  font-size: 72rpx;
  margin-right: 24rpx;
  background: #F1F5F9;
  width: 120rpx;
  height: 120rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.profile-name {
  font-size: 34rpx;
  font-weight: 800;
  color: #0F172A;
  display: block;
  margin-bottom: 8rpx;
}
.role-badge-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.role-badge {
  font-size: 20rpx;
  background: #EFF6FF;
  color: #2563EB;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-weight: 700;
}
.username-sub {
  font-size: 22rpx;
  color: #94A3B8;
}

.pref-field-row {
  margin-bottom: 24rpx;
}
.pref-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #334155;
  margin-bottom: 12rpx;
  display: block;
}
.pref-mode-selector {
  display: flex;
  gap: 16rpx;
}
.pref-mode-opt {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 12rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  font-size: 26rpx;
  color: #475569;
}
.pref-mode-opt.selected {
  background: #FFF7ED;
  border-color: #FF5E00;
  color: #FF5E00;
  font-weight: 700;
}
.pref-input {
  margin-bottom: 0;
}

.quick-link-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F1F5F9;
}
.quick-link-item:last-child {
  border-bottom: none;
}
.link-icon {
  font-size: 44rpx;
  margin-right: 20rpx;
}
.link-text-box {
  flex: 1;
}
.link-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0F172A;
  display: block;
}
.link-sub {
  font-size: 22rpx;
  color: #94A3B8;
}
.link-arrow {
  font-size: 32rpx;
  color: #CBD5E1;
}

.logout-btn {
  background: #FEE2E2;
  color: #EF4444;
  font-size: 28rpx;
  font-weight: 700;
  border-radius: 999rpx;
  padding: 16rpx 0;
  margin-top: 40rpx;
  border: none;
}
</style>

