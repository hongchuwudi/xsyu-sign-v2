<script setup>
// 全局消息提示宿主（样式与旧前端一致）
import { toastState } from '@/utils/message'
</script>

<template>
  <div v-for="t in toastState.list" :key="t.id"
       :class="t.type === 'success' ? 'bg-rose-400' : 'bg-pink-400'"
       class="fixed top-4 right-4 text-white px-6 py-3 rounded-lg shadow-lg z-50 max-w-md">
    <div class="flex items-center">
      <i :class="t.type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'" class="mr-2"></i>
      {{ t.msg }}
    </div>
  </div>
</template>
