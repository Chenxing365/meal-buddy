<template>
  <view class="chat-page">
    <!-- 顶部 AI 助手状态栏 -->
    <view class="chat-header">
      <view class="assistant-avatar">🤖</view>
      <view class="assistant-meta">
        <text class="assistant-name">AI 饭搭子（对话式选餐）</text>
        <text class="assistant-status">🟢 智能体在线 · 忌口搭配、换菜、热量咨询</text>
      </view>
    </view>

    <!-- 会话消息流 -->
    <scroll-view
      class="message-list"
      scroll-y
      :scroll-top="scrollTop"
      scroll-with-animation
    >
      <!-- 欢迎卡片（固定首条） -->
      <view class="message-item assistant">
        <view class="bubble-avatar">🤖</view>
        <view class="bubble-content">
          <text>您好！我是您的 AI 营养咨询助手。如果您对今日配餐有特殊需求（如：换掉生洋葱、控制20元以内、吃辣长痘等），随时在此跟我多轮互动！</text>
        </view>
      </view>

      <!-- 快捷提问气泡（无历史消息时显示） -->
      <view class="quick-questions-box" v-if="messages.length === 0">
        <text class="quick-title">💡 常见提问引导（点击直接提问）：</text>
        <view class="quick-tags">
          <text class="quick-tag" @tap="sendQuickText('推荐一份低卡少油且不吃辣的午餐')">🌱 低卡少油不吃辣</text>
          <text class="quick-tag" @tap="sendQuickText('今天训练完，想要高蛋白增肌搭配')">💪 运动高蛋白增肌</text>
          <text class="quick-tag" @tap="sendQuickText('预算30元以内，有什么好吃的推荐？')">💰 30元内高性价比</text>
        </view>
      </view>

      <!-- 历史消息 -->
      <view
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="msg.role"
      >
        <view class="bubble-avatar">{{ msg.role === 'assistant' ? '🤖' : '👤' }}</view>
        <view class="bubble-content">
          <text>{{ msg.content }}</text>
        </view>
      </view>

      <!-- AI 思考中动效 -->
      <view v-if="sending" class="message-item assistant">
        <view class="bubble-avatar">🤖</view>
        <view class="bubble-content thinking">
          <text class="dot-typing">AI 正在组织膳食营养建议...</text>
        </view>
      </view>

      <!-- 垫高区域，防键盘遮挡 -->
      <view class="bottom-spacer"></view>
    </scroll-view>

    <!-- 底部固定输入栏（flex 布局固定在页面底部，不再被 TabBar 遮挡） -->
    <view class="input-bar" :style="keyboardHeight ? { marginBottom: keyboardHeight + 'px' } : {}">
      <input
        class="chat-input"
        v-model="inputMsg"
        placeholder="向 AI 饭搭子咨询营养或点餐..."
        :adjust-position="false"
        confirm-type="send"
        @confirm="sendMessage"
        @focus="onKeyboardShow"
        @blur="onKeyboardHide"
      />
      <button
        class="send-btn"
        :disabled="!inputMsg.trim() || sending"
        @tap="sendMessage"
      >
        发送
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { chatWithAi, getChatHistory, type AiChatMessage } from '@/api/recommend'

const messages = ref<AiChatMessage[]>([])
const inputMsg = ref('')
const sending = ref(false)
const scrollTop = ref(99999)
const keyboardHeight = ref(0)

onMounted(async () => {
  await fetchHistory()
})

async function fetchHistory() {
  try {
    const res = await getChatHistory()
    if (res.data && Array.isArray(res.data)) {
      messages.value = res.data
      scrollToBottom()
    }
  } catch (e) {
    // 未登录静默
  }
}

async function sendMessage() {
  const text = inputMsg.value.trim()
  if (!text || sending.value) return

  messages.value.push({
    role: 'user',
    content: text,
    timestamp: new Date().toISOString()
  })
  inputMsg.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const res = await chatWithAi(text)
    if (res.data?.reply) {
      messages.value.push({
        role: 'assistant',
        content: res.data.reply,
        timestamp: res.data.timestamp || new Date().toISOString()
      })
    }
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，当前网络异常，请稍后向我提问。',
      timestamp: new Date().toISOString()
    })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    scrollTop.value = scrollTop.value + 1000
  })
}

function sendQuickText(text: string) {
  inputMsg.value = text
  sendMessage()
}

function onKeyboardShow(e: any) {
  // H5 下键盘高度由浏览器控制，小程序下 adjust-position=false 时手动偏移
  keyboardHeight.value = e?.detail?.height || 0
}

function onKeyboardHide() {
  keyboardHeight.value = 0
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  /* 扣除导航栏与 TabBar 高度，避免输入栏被 TabBar 压住 */
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  overflow: hidden;
  background: #F8FAFC;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  background: #FFFFFF;
  border-bottom: 1rpx solid #E2E8F0;
  flex-shrink: 0;
}
.assistant-avatar {
  font-size: 52rpx;
  margin-right: 16rpx;
}
.assistant-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #0F172A;
  display: block;
}
.assistant-status {
  font-size: 20rpx;
  color: #10B981;
  display: block;
}

.quick-questions-box {
  margin: 16rpx 24rpx;
  padding: 20rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  border: 1rpx solid #F1F5F9;
}
.quick-title {
  font-size: 24rpx;
  color: #64748B;
  display: block;
  margin-bottom: 12rpx;
}
.quick-tags {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.quick-tag {
  font-size: 24rpx;
  color: #0F172A;
  background: #F8FAFC;
  padding: 10rpx 16rpx;
  border-radius: 10rpx;
  border: 1rpx solid #E2E8F0;
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 24rpx;
  box-sizing: border-box;
}

.message-item {
  display: flex;
  margin-bottom: 32rpx;
  align-items: flex-start;
}
.message-item.user {
  flex-direction: row-reverse;
}

.bubble-avatar {
  font-size: 40rpx;
  margin: 0 12rpx;
  flex-shrink: 0;
}

.bubble-content {
  max-width: 70%;
  padding: 20rpx 28rpx;
  border-radius: 24rpx;
  font-size: 28rpx;
  line-height: 1.5;
  word-break: break-all;
}

.message-item.assistant .bubble-content {
  background: #FFFFFF;
  color: #1E293B;
  border-top-left-radius: 4rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
  border: 1rpx solid #E2E8F0;
}

.message-item.user .bubble-content {
  background: linear-gradient(135deg, #FF6B00 0%, #FF3D00 100%);
  color: #FFFFFF;
  border-top-right-radius: 4rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 94, 0, 0.2);
}

.thinking {
  color: #94A3B8;
  font-style: italic;
}

.bottom-spacer {
  height: 24rpx;
}

/* 底部固定输入栏：作为 flex 子项常驻底部 */
.input-bar {
  flex-shrink: 0;
  display: flex;
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  border-top: 1rpx solid #E2E8F0;
  align-items: center;
  gap: 20rpx;
  box-shadow: 0 -4rpx 24rpx rgba(15, 23, 42, 0.06);
  z-index: 100;
}

.chat-input {
  flex: 1;
  height: 100rpx;
  line-height: 100rpx;
  background: #F1F5F9;
  border-radius: 999rpx;
  padding: 0 32rpx;
  font-size: 30rpx;
  color: #0F172A;
  box-sizing: border-box;
}

.send-btn {
  height: 100rpx;
  line-height: 100rpx;
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 999rpx;
  padding: 0 44rpx;
  margin: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.send-btn:disabled {
  background: #CBD5E1;
  color: #FFFFFF;
}
</style>
