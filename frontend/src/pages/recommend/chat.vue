<template>
  <view class="chat-page">
    <!-- 顶部状态栏与模式区分 -->
    <view class="chat-header">
      <view class="assistant-avatar">🤖</view>
      <view class="assistant-meta">
        <text class="assistant-name">AI 咨询助手（对话式选餐）</text>
        <text class="assistant-status">🟢 智能体在线 · 支持忌口搭配、换菜、热量咨询</text>
      </view>
      <view class="direct-rec-btn" @tap="goToRecommendBoard">
        <text>一键配餐 ›</text>
      </view>
    </view>

    <!-- 会话消息流容器 -->
    <scroll-view 
      class="message-list" 
      scroll-y 
      :scroll-top="scrollTop" 
      scroll-with-animation
    >
      <!-- 初始欢迎卡片 -->
      <view class="message-item assistant">
        <view class="bubble-avatar">🤖</view>
        <view class="bubble-content">
          <text>您好！我是您的 AI 营养咨询助手。如果您对今日配餐有特殊需求（如：换掉生洋葱、控制20元以内、吃辣长痘等），随时在此跟我多轮互动！</text>
        </view>
      </view>

      <!-- 快捷提问推荐气泡 -->
      <view class="quick-questions-box" v-if="messages.length === 0">
        <text class="quick-title">💡 常见提问引导（点击直接提问）：</text>
        <view class="quick-tags">
          <text class="quick-tag" @tap="sendQuickText('推荐一份低卡少油且不吃辣的午餐')">🌱 低卡少油不吃辣</text>
          <text class="quick-tag" @tap="sendQuickText('今天训练完，想要高蛋白增肌搭配')">💪 运动高蛋白增肌</text>
          <text class="quick-tag" @tap="sendQuickText('预算30元以内，有什么好吃的推荐？')">💰 30元内高性价比</text>
        </view>
      </view>

      <!-- 历史消息渲染 -->
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

      <!-- 发送中思考中动效 -->
      <view v-if="sending" class="message-item assistant">
        <view class="bubble-avatar">🤖</view>
        <view class="bubble-content thinking">
          <text class="dot-typing">AI 正在组织膳食营养建议...</text>
        </view>
      </view>

      <!-- 垫高区域，防键盘遮挡 -->
      <view class="bottom-spacer"></view>
    </scroll-view>

    <!-- 底部输入框区（防软键盘遮挡） -->
    <view class="input-bar" :style="{ bottom: keyboardHeight + 'px' }">
      <input 
        class="chat-input" 
        v-model="inputMsg" 
        placeholder="向 AI 饭搭子咨询营养或点餐..." 
        :adjust-position="false"
        confirm-type="send"
        @confirm="sendMessage"
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
    // 静默
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
function goToRecommendBoard() {
  uni.switchTab({ url: '/pages/recommend/index' })
}

function sendQuickText(text: string) {
  inputMsg.value = text
  sendMessage()
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F8FAFC;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  background: #FFFFFF;
  border-bottom: 1rpx solid #E2E8F0;
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
}

.direct-rec-btn {
  background: #FFF7ED;
  color: #FF5E00;
  border: 1rpx solid #FFEDD5;
  font-size: 22rpx;
  font-weight: 700;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
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
  height: 140rpx;
}

.input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  padding: 20rpx 24rpx 40rpx;
  background: #FFFFFF;
  border-top: 1rpx solid #E2E8F0;
  align-items: center;
  gap: 16rpx;
}

.chat-input {
  flex: 1;
  background: #F1F5F9;
  border-radius: 999rpx;
  padding: 16rpx 28rpx;
  font-size: 28rpx;
}

.send-btn {
  background: #FF5E00;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 999rpx;
  padding: 12rpx 32rpx;
  margin: 0;
}
.send-btn:disabled {
  background: #CBD5E1;
  color: #FFFFFF;
}
</style>

