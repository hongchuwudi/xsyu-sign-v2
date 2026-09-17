<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const items = [
  { name: 'admin-users', path: '/admin/users', label: '用户', icon: 'fas fa-users' },
  { name: 'admin-task', path: '/admin/task', label: '任务', icon: 'fas fa-clock' },
  { name: 'admin-queue', path: '/admin/queue', label: '队列', icon: 'fas fa-layer-group' },
  { name: 'admin-announcement', path: '/admin/announcement', label: '公告', icon: 'fas fa-bullhorn' },
  { name: 'admin-logs', path: '/admin/logs', label: '日志', icon: 'fas fa-list-check' }
]
</script>

<template>
  <nav aria-label="管理员导航" class="fixed bottom-0 left-0 right-0 z-30 border-t border-pink-200 bg-white/95 backdrop-blur-sm">
    <div class="mx-auto grid max-w-2xl grid-cols-5 px-1 py-1.5">
      <button
        v-for="item in items"
        :key="item.name"
        type="button"
        :aria-current="route.name === item.name ? 'page' : undefined"
        :class="route.name === item.name ? 'text-rose-500' : 'text-gray-400 hover:text-pink-500'"
        class="flex min-w-0 flex-col items-center justify-center rounded-lg px-1 py-1.5 transition-colors focus:outline-none focus:ring-2 focus:ring-pink-300"
        @click="router.push(item.path)"
      >
        <i :class="item.icon" class="h-5 text-lg" aria-hidden="true"></i>
        <span class="mt-0.5 truncate text-[11px] font-medium sm:text-xs">{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>
