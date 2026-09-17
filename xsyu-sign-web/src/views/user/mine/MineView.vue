<script setup>
// 我的页面：与三个点菜单同一套动作（useUserActions），保证行为一致
import { useUserActions } from '@/composables/useUserActions'
import BottomNav from '@/components/BottomNav.vue'
import UserProfileModal from '@/components/UserProfileModal.vue'
import BindXsyModal from '@/components/BindXsyModal.vue'
import UnregisterConfirmModal from '@/components/UnregisterConfirmModal.vue'

const {
  userInfo, isLoading, captchaSession,
  showEditProfileModal, showBindModal, showUnregisterConfirmModal,
  toggleAutoSign, handleUpdateUserInfo, handleBindByPassword, handleLogout, handleUnregister,
  openEditProfile, openBindModal, closeBindModal, goAbout, openUnregisterConfirm
} = useUserActions()
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-pink-50 to-rose-50 pb-14">
    <!-- 用户卡片 -->
    <div class="bg-white shadow-sm border-b border-pink-100">
      <div class="max-w-3xl mx-auto px-4 py-6 flex items-center gap-4">
        <div class="w-14 h-14 bg-gradient-to-r from-pink-400 to-rose-400 rounded-full flex items-center justify-center flex-shrink-0">
          <i class="fas fa-user text-white text-xl"></i>
        </div>
        <div class="min-w-0">
          <p class="font-bold text-gray-800 truncate">{{ userInfo.name || userInfo.username || '未登录' }}</p>
          <p class="text-sm text-pink-500 mt-0.5">学号 {{ userInfo.username || '--' }}</p>
        </div>
      </div>
    </div>

    <div class="max-w-3xl mx-auto p-4 space-y-3">
      <!-- 常用设置 -->
      <div class="bg-white rounded-xl border border-pink-200 shadow-sm overflow-hidden divide-y divide-pink-50">
        <button @click="openEditProfile"
                class="w-full flex items-center px-4 py-3.5 text-sm text-gray-700 hover:bg-pink-50 transition-colors">
          <i class="fas fa-user-edit w-5 text-pink-400 mr-3"></i>
          <span>修改个人信息</span>
          <i class="fas fa-chevron-right ml-auto text-gray-300 text-xs"></i>
        </button>

        <button @click="toggleAutoSign"
                class="w-full flex items-center px-4 py-3.5 text-sm text-gray-700 hover:bg-pink-50 transition-colors">
          <i class="fas fa-robot w-5 text-pink-400 mr-3"></i>
          <span>自动签到</span>
          <span class="ml-auto flex items-center gap-2">
            <span class="text-xs" :class="userInfo.autoSign ? 'text-pink-500' : 'text-gray-400'">
              {{ userInfo.autoSign ? '已开启' : '已关闭' }}
            </span>
            <span class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
                  :class="userInfo.autoSign ? 'bg-pink-400' : 'bg-gray-200'">
              <span class="inline-block h-4 w-4 bg-white rounded-full shadow transition-transform"
                    :class="userInfo.autoSign ? 'translate-x-6' : 'translate-x-1'"></span>
            </span>
          </span>
        </button>

        <button v-if="userInfo.role !== 'ADMIN'" @click="openBindModal"
                class="w-full flex items-center px-4 py-3.5 text-sm text-gray-700 hover:bg-pink-50 transition-colors">
          <i class="fas fa-link w-5 text-pink-400 mr-3"></i>
          <span>更新学校密码</span>
          <span class="ml-auto text-xs text-gray-400 mr-1">用于JWS自动续签</span>
          <i class="fas fa-chevron-right text-gray-300 text-xs"></i>
        </button>

        <button @click="goAbout"
                class="w-full flex items-center px-4 py-3.5 text-sm text-gray-700 hover:bg-pink-50 transition-colors">
          <i class="fas fa-info-circle w-5 text-pink-400 mr-3"></i>
          <span>关于软件</span>
          <i class="fas fa-chevron-right ml-auto text-gray-300 text-xs"></i>
        </button>
      </div>

      <!-- 账号操作 -->
      <div class="bg-white rounded-xl border border-pink-200 shadow-sm overflow-hidden divide-y divide-pink-50">
        <button @click="handleLogout"
                class="w-full flex items-center px-4 py-3.5 text-sm text-pink-700 hover:bg-pink-50 transition-colors">
          <i class="fas fa-sign-out-alt w-5 text-pink-400 mr-3"></i>
          <span>退出登录</span>
        </button>
        <button @click="openUnregisterConfirm"
                class="w-full flex items-center px-4 py-3.5 text-sm text-rose-600 hover:bg-rose-50 transition-colors">
          <i class="fas fa-trash-alt w-5 mr-3"></i>
          <span>注销信息</span>
        </button>
      </div>
    </div>

    <BottomNav />

    <!-- 与三个点菜单同一套弹窗 -->
    <UserProfileModal :visible="showEditProfileModal"
                      :user-info="userInfo"
                      @close="showEditProfileModal = false"
                      @save="handleUpdateUserInfo" />
    <BindXsyModal :visible="showBindModal"
                  :is-loading="isLoading"
                  :captcha-session="captchaSession"
                  :username="userInfo.username"
                  @close="closeBindModal"
                  @bind-password="handleBindByPassword" />
    <UnregisterConfirmModal :visible="showUnregisterConfirmModal"
                            @close="showUnregisterConfirmModal = false"
                            @confirm="handleUnregister" />
  </div>
</template>
