<script setup lang="ts">
// UniApp 根组件
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
