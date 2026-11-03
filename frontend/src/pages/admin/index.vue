<template>
  <view class="app-container admin-page">
    <!-- 顶部状态栏与统计 -->
    <view class="admin-header card-industrial">
      <view class="header-main">
        <view class="admin-title">平台综合治理看板</view>
        <view class="admin-role-badge">SUPER_ADMIN (Role: 9)</view>
      </view>
      <view class="stat-grid">
        <view class="stat-cell">
          <text class="stat-num text-brand">{{ merchantTotal }}</text>
          <text class="stat-label">商户总数</text>
        </view>
        <view class="stat-cell">
          <text class="stat-num text-warning">{{ pendingCount }}</text>
          <text class="stat-label">待审商户</text>
        </view>
        <view class="stat-cell">
          <text class="stat-num text-success">{{ userTotal }}</text>
          <text class="stat-label">注册用户</text>
        </view>
      </view>
    </view>

    <!-- 主 Tab 切换 -->
    <view class="main-tabs">
      <view
        class="main-tab-item"
        :class="{ active: currentTab === 'merchant' }"
        @click="currentTab = 'merchant'"
      >
        商户入驻审核
      </view>
      <view
        class="main-tab-item"
        :class="{ active: currentTab === 'user' }"
        @click="currentTab = 'user'"
      >
        用户账号治理
      </view>
    </view>

    <!-- Tab 1: 商户入驻审核 -->
    <view v-if="currentTab === 'merchant'" class="tab-content">
      <!-- 状态过滤小标签 -->
      <view class="filter-bar">
        <view
          class="filter-chip"
          :class="{ active: merchantStatusFilter === undefined }"
          @click="changeMerchantFilter(undefined)"
        >全部</view>
        <view
          class="filter-chip"
          :class="{ active: merchantStatusFilter === 0 }"
          @click="changeMerchantFilter(0)"
        >待审核</view>
        <view
          class="filter-chip"
          :class="{ active: merchantStatusFilter === 1 }"
          @click="changeMerchantFilter(1)"
        >已通过</view>
        <view
          class="filter-chip"
          :class="{ active: merchantStatusFilter === 3 }"
          @click="changeMerchantFilter(3)"
        >已驳回</view>
      </view>

      <view v-if="loadingMerchants" class="skeleton-card">
        <view class="skeleton-line" style="width: 50%; height: 36rpx;"></view>
        <view class="skeleton-line" style="width: 80%; height: 28rpx; margin-top: 16rpx;"></view>
      </view>

      <view v-else-if="merchants.length === 0" class="empty-state">
        <text class="empty-text">当前暂无匹配的商户申请记录</text>
      </view>

      <view v-else class="record-list">
        <view
          v-for="item in merchants"
          :key="item.id"
          class="card-industrial merchant-admin-card"
        >
          <view class="card-head">
            <view class="name-block">
              <text class="merchant-name">{{ item.name }}</text>
              <text class="badge-category">{{ item.category }}</text>
            </view>
            <text :class="getMerchantStatusBadge(item.status).cls">
              {{ getMerchantStatusBadge(item.status).text }}
            </text>
          </view>

          <view class="detail-row">
            <text class="field-label">联系电话：</text>
            <text class="field-val">{{ item.phone || '未填写' }}</text>
          </view>
          <view class="detail-row">
            <text class="field-label">经营地址：</text>
            <text class="field-val">{{ item.address || '未填写' }}</text>
          </view>
          <view class="detail-row">
            <text class="field-label">商户简介：</text>
            <text class="field-val">{{ item.description || '暂无简介' }}</text>
          </view>
          <view class="detail-row">
            <text class="field-label">申请时间：</text>
            <text class="field-val">{{ formatTime(item.createdAt) }}</text>
          </view>

          <!-- 操作栏 -->
          <view class="action-footer" v-if="item.status === 0">
            <button class="btn-sm btn-reject" @click="handleReject(item)">
              驳回申请
            </button>
            <button class="btn-sm btn-approve" @click="handleApprove(item)">
              通过入驻
            </button>
          </view>
          <view class="action-footer" v-else>
            <text class="resolved-note">
              {{ item.status === 1 ? '该商户已在营业中' : '申请已驳回处理' }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- Tab 2: 用户账号治理 -->
    <view v-if="currentTab === 'user'" class="tab-content">
      <view class="filter-bar">
        <view
          class="filter-chip"
          :class="{ active: userRoleFilter === undefined }"
          @click="changeUserFilter(undefined)"
        >全部用户</view>
        <view
          class="filter-chip"
          :class="{ active: userRoleFilter === 0 }"
          @click="changeUserFilter(0)"
        >普通消费者</view>
        <view
          class="filter-chip"
          :class="{ active: userRoleFilter === 1 }"
          @click="changeUserFilter(1)"
        >商户店主</view>
        <view
          class="filter-chip"
          :class="{ active: userRoleFilter === 9 }"
          @click="changeUserFilter(9)"
        >平台运维</view>
      </view>

      <view v-if="loadingUsers" class="skeleton-card">
        <view class="skeleton-line" style="width: 50%; height: 36rpx;"></view>
        <view class="skeleton-line" style="width: 80%; height: 28rpx; margin-top: 16rpx;"></view>
      </view>

      <view v-else-if="users.length === 0" class="empty-state">
        <text class="empty-text">当前分类下暂无注册用户</text>
      </view>

      <view v-else class="record-list">
        <view
          v-for="u in users"
          :key="u.id"
          class="card-industrial user-admin-card"
        >
          <view class="card-head">
            <view class="user-info-left">
              <text class="user-nickname">{{ u.nickname || u.username }}</text>
              <text class="user-account">@{{ u.username }}</text>
            </view>
            <text :class="getUserRoleBadge(u.role).cls">
              {{ getUserRoleBadge(u.role).text }}
            </text>
          </view>

          <view class="detail-row">
            <text class="field-label">绑定手机：</text>
            <text class="field-val">{{ u.phone || '未绑定' }}</text>
          </view>
          <view class="detail-row">
            <text class="field-label">账号状态：</text>
            <text :class="u.status === 1 ? 'text-success' : 'text-danger'">
              {{ u.status === 1 ? '正常活跃' : '已被封禁/禁用' }}
            </text>
          </view>
          <view class="detail-row">
            <text class="field-label">注册时间：</text>
            <text class="field-val">{{ formatTime(u.createdAt) }}</text>
          </view>

          <view class="action-footer" v-if="u.role !== 9">
            <button
              v-if="u.status === 1"
              class="btn-sm btn-reject"
              @click="toggleUserStatus(u, 0)"
            >
              禁用账号
            </button>
            <button
              v-else
              class="btn-sm btn-approve"
              @click="toggleUserStatus(u, 1)"
            >
              解封启用
            </button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getAdminMerchants,
  approveMerchant,
  rejectMerchant,
  getAdminUsers,
  updateUserStatus,
  type AdminUserItem
} from '@/api/admin'
import type { MerchantInfo } from '@/api/merchant'

const currentTab = ref<'merchant' | 'user'>('merchant')

// 商户数据
const merchants = ref<MerchantInfo[]>([])
const merchantTotal = ref(0)
const pendingCount = ref(0)
const merchantStatusFilter = ref<number | undefined>(undefined)
const loadingMerchants = ref(false)

// 用户数据
const users = ref<AdminUserItem[]>([])
const userTotal = ref(0)
const userRoleFilter = ref<number | undefined>(undefined)
const loadingUsers = ref(false)

async function fetchMerchants() {
  loadingMerchants.value = true
  try {
    const res = await getAdminMerchants({
      status: merchantStatusFilter.value,
      page: 0,
      size: 50
    })
    merchants.value = res.content || []
    merchantTotal.value = res.totalElements || 0
    if (merchantStatusFilter.value === undefined) {
      pendingCount.value = merchants.value.filter(m => m.status === 0).length
    }
  } catch (err: any) {
    uni.showToast({ title: err.message || '加载商户失败', icon: 'none' })
  } finally {
    loadingMerchants.value = false
  }
}

async function fetchUsers() {
  loadingUsers.value = true
  try {
    const res = await getAdminUsers({
      role: userRoleFilter.value,
      page: 0,
      size: 50
    })
    users.value = res.content || []
    userTotal.value = res.totalElements || 0
  } catch (err: any) {
    uni.showToast({ title: err.message || '加载用户失败', icon: 'none' })
  } finally {
    loadingUsers.value = false
  }
}

function changeMerchantFilter(status: number | undefined) {
  merchantStatusFilter.value = status
  fetchMerchants()
}

function changeUserFilter(role: number | undefined) {
  userRoleFilter.value = role
  fetchUsers()
}

async function handleApprove(item: MerchantInfo) {
  uni.showModal({
    title: '审批通过确认',
    content: `确认批准【${item.name}】入驻并开启营业吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await approveMerchant(item.id)
          uni.showToast({ title: '已审批通过', icon: 'success' })
          fetchMerchants()
        } catch (err: any) {
          uni.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleReject(item: MerchantInfo) {
  uni.showModal({
    title: '驳回入驻申请',
    content: `确认驳回【${item.name}】的入驻申请吗？`,
    editable: true,
    placeholderText: '请输入驳回理由（可选）',
    success: async (res) => {
      if (res.confirm) {
        const reason = res.content?.trim() || '资质材料不全或未达标'
        try {
          await rejectMerchant(item.id, reason)
          uni.showToast({ title: '已驳回申请', icon: 'success' })
          fetchMerchants()
        } catch (err: any) {
          uni.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

async function toggleUserStatus(u: AdminUserItem, newStatus: number) {
  const actionName = newStatus === 1 ? '解封启用' : '禁用封禁'
  uni.showModal({
    title: `${actionName}账号`,
    content: `确认对用户【${u.nickname || u.username}】执行${actionName}吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await updateUserStatus(u.id, newStatus)
          uni.showToast({ title: `${actionName}成功`, icon: 'success' })
          fetchUsers()
        } catch (err: any) {
          uni.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

function getMerchantStatusBadge(status?: number) {
  switch (status) {
    case 0:
      return { text: '待审核', cls: 'badge-status badge-warning' }
    case 1:
      return { text: '营业中', cls: 'badge-status badge-success' }
    case 2:
      return { text: '打烊歇业', cls: 'badge-status badge-neutral' }
    case 3:
      return { text: '已驳回', cls: 'badge-status badge-danger' }
    default:
      return { text: '未知', cls: 'badge-status badge-neutral' }
  }
}

function getUserRoleBadge(role?: number) {
  switch (role) {
    case 9:
      return { text: '系统管理员', cls: 'badge-status badge-danger' }
    case 1:
      return { text: '商户店主', cls: 'badge-status badge-brand' }
    case 2:
      return { text: '配送专员', cls: 'badge-status badge-info' }
    default:
      return { text: '注册会员', cls: 'badge-status badge-neutral' }
  }
}

function formatTime(isoStr?: string) {
  if (!isoStr) return '--'
  return isoStr.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  fetchMerchants()
  fetchUsers()
})
</script>

<style scoped>
.admin-page {
  padding-bottom: 60rpx;
}

.admin-header {
  padding: 32rpx;
  margin-bottom: 24rpx;
  background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%);
  color: #FFFFFF;
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.admin-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.admin-role-badge {
  font-size: 20rpx;
  color: #38BDF8;
  background: rgba(56, 189, 248, 0.15);
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.stat-cell {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16rpx;
  padding: 20rpx 16rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 36rpx;
  font-weight: 700;
  margin-bottom: 6rpx;
}

.stat-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.main-tabs {
  display: flex;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
}

.main-tab-item {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 28rpx;
  font-weight: 600;
  color: #64748B;
  border-radius: 14rpx;
  transition: all 0.2s ease;
}

.main-tab-item.active {
  background: #FF5E00;
  color: #FFFFFF;
  box-shadow: 0 4rpx 12rpx rgba(255, 94, 0, 0.25);
}

.filter-bar {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
  overflow-x: auto;
}

.filter-chip {
  padding: 10rpx 24rpx;
  background: #FFFFFF;
  border-radius: 999rpx;
  font-size: 24rpx;
  color: #64748B;
  white-space: nowrap;
  border: 1rpx solid #E2E8F0;
}

.filter-chip.active {
  background: #0F172A;
  color: #FFFFFF;
  border-color: #0F172A;
  font-weight: 600;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.merchant-admin-card,
.user-admin-card {
  padding: 28rpx;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
  padding-bottom: 14rpx;
  border-bottom: 1rpx solid #F1F5F9;
}

.name-block {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.merchant-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}

.badge-category {
  font-size: 20rpx;
  color: #FF5E00;
  background: #FFF7ED;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.user-info-left {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.user-nickname {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}

.user-account {
  font-size: 22rpx;
  color: #94A3B8;
}

.detail-row {
  display: flex;
  font-size: 24rpx;
  line-height: 1.6;
  margin-bottom: 8rpx;
}

.field-label {
  color: #94A3B8;
  width: 140rpx;
  flex-shrink: 0;
}

.field-val {
  color: #334155;
  flex: 1;
  word-break: break-all;
}

.action-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed #E2E8F0;
}

.btn-sm {
  font-size: 24rpx;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  border: none;
  font-weight: 600;
  line-height: 1.4;
}

.btn-approve {
  background: #10B981;
  color: #FFFFFF;
}

.btn-reject {
  background: #EF4444;
  color: #FFFFFF;
}

.resolved-note {
  font-size: 22rpx;
  color: #94A3B8;
}

.badge-info {
  background: #E0F2FE;
  color: #0284C7;
}

.badge-brand {
  background: #FFF7ED;
  color: #FF5E00;
}

.empty-state {
  padding: 80rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 26rpx;
  color: #94A3B8;
}
</style>

