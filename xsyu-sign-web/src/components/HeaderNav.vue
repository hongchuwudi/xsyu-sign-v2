<script setup>
// 顶部导航（签到页）：标题 + 用户名 + 用户菜单按钮
import { ref } from 'vue'
import UserMenuButton from '@/components/UserMenuButton.vue'

defineProps({
  userInfo: { type: Object, required: true }
})

const menuRef = ref(null)
// 透传 openEditProfile（如签到页"设置签到日期"引导）
defineExpose({ openEditProfile: () => menuRef.value?.openEditProfile() })
</script>

<template>
  <header class="bg-white/90 backdrop-blur-sm border-b border-pink-200 sticky top-0 z-10">
    <div class="container mx-auto px-4">
      <div class="flex items-center justify-between py-3">
        <div class="flex items-center space-x-2">
          <i class="fas fa-calendar-check text-pink-400 text-xl"></i>
          <h1 class="text-lg font-semibold text-pink-800">签到系统</h1>
        </div>

        <div class="flex items-center space-x-3">
          <!-- 用户信息 -->
          <div class="hidden sm:flex items-center space-x-2">
            <div class="w-8 h-8 bg-pink-100 rounded-full flex items-center justify-center">
              <i class="fas fa-user text-pink-400 text-sm"></i>
            </div>
            <span class="text-sm font-medium text-pink-700">{{ userInfo.name || userInfo.username }}</span>
          </div>

          <UserMenuButton ref="menuRef" />
        </div>
      </div>
    </div>
  </header>
</template>
