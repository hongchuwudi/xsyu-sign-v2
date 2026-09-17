<script setup>
// 用户菜单按钮（⋮）+ 其触发的全部弹窗
// 动作逻辑与"我的"页面共用 useUserActions（行为一致）
import { ref } from 'vue'
import { useUserActions } from '@/composables/useUserActions'
import UserProfileModal from '@/components/UserProfileModal.vue'
import BindXsyModal from '@/components/BindXsyModal.vue'
import UnregisterConfirmModal from '@/components/UnregisterConfirmModal.vue'

const {
  userInfo, isLoading, captchaSession,
  showEditProfileModal, showBindModal, showUnregisterConfirmModal,
  toggleAutoSign, handleUpdateUserInfo, handleBindByPassword, handleLogout, handleUnregister,
  openEditProfile, openBindModal, closeBindModal, goAbout, openUnregisterConfirm
} = useUserActions()

const showMenu = ref(false)

// 菜单项：先收起菜单再执行动作（打开弹窗/请求）
function item(fn) {
  showMenu.value = false
  return fn()
}

// 供外部引导打开（如签到页"设置签到日期"）
defineExpose({ openEditProfile })
</script>

<template>
  <div class="relative">
    <button @click="showMenu = !showMenu" class="p-2 rounded-lg hover:bg-pink-50">
      <i class="fas fa-ellipsis-v text-pink-400"></i>
    </button>

    <!-- 用户菜单 -->
    <div v-if="showMenu"
         class="absolute right-0 mt-2 w-48 bg-white/95 backdrop-blur-sm rounded-lg shadow-lg border border-pink-200 py-2 z-20">
      <button @click="item(openEditProfile)"
              class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
        <i class="fas fa-user-edit mr-2 text-pink-400"></i>修改个人信息
      </button>
      <button @click="item(toggleAutoSign)"
              class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
        <i class="fas fa-robot mr-2 text-pink-400"></i>
        自动签到: {{ userInfo.autoSign ? '开启' : '关闭' }}
      </button>
      <button v-if="userInfo.role !== 'ADMIN'" @click="item(openBindModal)"
              class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
        <i class="fas fa-link mr-2 text-pink-400"></i>更新学校密码
      </button>
      <button @click="item(goAbout)"
              class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
        <i class="fas fa-info-circle mr-2 text-pink-400"></i>关于软件
      </button>
      <div class="border-t border-pink-100 my-1"></div>
      <button @click="item(handleLogout)"
              class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
        <i class="fas fa-sign-out-alt mr-2 text-pink-400"></i>退出登录
      </button>
      <button @click="item(openUnregisterConfirm)"
              class="w-full text-left px-4 py-2 text-sm text-rose-600 hover:bg-rose-50 flex items-center">
        <i class="fas fa-trash-alt mr-2"></i>注销信息
      </button>
    </div>

    <!-- 菜单触发的弹窗 -->
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
