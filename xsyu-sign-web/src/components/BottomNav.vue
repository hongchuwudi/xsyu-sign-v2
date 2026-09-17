<script setup>
// 用户端底部导航：首页 / 公告 / 签到
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const items = [
  { name: 'home', path: '/', label: '首页', icon: 'fas fa-home' },
  { name: 'announcement', path: '/announcement', label: '公告', icon: 'fas fa-bullhorn' },
  { name: 'sign', path: '/sign', label: '签到', icon: 'fas fa-calendar-check' },
  { name: 'mine', path: '/mine', label: '我的', icon: 'fas fa-user' }
]
</script>

<template>
  <nav class="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-sm border-t border-pink-200 z-20">
    <div class="max-w-lg mx-auto flex justify-around py-2">
      <button v-for="item in items" :key="item.name"
              @click="router.push(item.path)"
              :class="route.name === item.name ? 'text-pink-500' : 'text-gray-400'"
              class="flex flex-col items-center px-4 py-1 transition-colors">
        <i :class="item.icon" class="text-xl"></i>
        <span class="text-xs mt-1 font-medium">{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>
