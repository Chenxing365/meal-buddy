<script setup lang="ts">
// UniApp 根组件
import { onLaunch } from '@dcloudio/uni-app'
import { autoLogin } from '@/utils/auth'
import { setAuthReady } from '@/utils/request'

// 演示环境：启动即自动登录；页面首个请求会等待该 Promise 完成后再发出
onLaunch(() => {
  setAuthReady(autoLogin())
})
</script>

<template>
  <page-meta root-font-size="16px" />
  <router-view />
</template>

<style>
@import './styles/theme.css';

/* 全局重置 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
button::after {
  border: none;
}
</style>
