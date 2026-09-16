<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { api } from '@/utils/api'

const userStore = useUserStore()
const backendStatus = ref('未检测')

async function checkBackend() {
  try {
    const { data } = await api.getPublicKey()
    backendStatus.value = data.code === 200 ? '已连接 ✓' : '异常'
  } catch {
    backendStatus.value = '无法连接（后端没启动？）'
  }
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-pink-50 to-rose-50 flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-white/90 backdrop-blur-sm rounded-xl shadow-lg p-8 border border-pink-200 text-center">
      <h1 class="text-2xl font-bold text-pink-800 mb-2">油签机前端</h1>
      <p class="text-pink-500 text-sm mb-6">Vue 3 + Vite + Pinia + Tailwind CSS</p>

      <div class="space-y-3 text-sm">
        <p class="text-gray-600">
          登录状态：
          <span :class="userStore.isLoggedIn ? 'text-green-600' : 'text-gray-400'">
            {{ userStore.isLoggedIn ? '已登录' : '未登录' }}
          </span>
        </p>
        <p class="text-gray-600">后端接口：{{ backendStatus }}</p>
      </div>

      <button
        @click="checkBackend"
        class="mt-6 w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity">
        检测后端连接
      </button>
    </div>
  </div>
</template>
