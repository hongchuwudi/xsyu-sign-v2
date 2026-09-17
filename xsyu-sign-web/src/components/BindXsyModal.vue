<script setup>
// 更新学校密码弹窗（用 CAS 密码验证当前账号，保存后启用 JWS 自动续签）
import { ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'

const props = defineProps({
  visible: Boolean,
  isLoading: Boolean,
  captchaSession: { type: Object, default: null },
  username: String
})
const emit = defineEmits(['close', 'bind-password'])

const casPassword = ref('')
const captchaCode = ref('')

watch(() => props.visible, val => {
  if (val) {
    casPassword.value = ''
    captchaCode.value = ''
  }
})

watch(() => props.captchaSession, val => {
  if (!val) captchaCode.value = ''
})

function handleSubmit() {
  if (!casPassword.value) return alert('请输入学校密码')
  emit('bind-password', {
    casPsd: casPassword.value,
    captchaCode: captchaCode.value
  })
}
</script>

<template>
  <BaseModal :visible="visible"
             title="更新学校密码"
             :subtitle="'账号 ' + (username || '') + '：保存后用于JWS自动续签'"
             @close="emit('close')">
    <div class="space-y-4">
      <div>
        <label class="block text-pink-700 text-sm font-medium mb-2">学校密码（统一认证密码）</label>
        <input v-model="casPassword" type="password" placeholder="请输入学校统一认证密码"
               class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
      </div>

      <!-- 验证码区域 -->
      <div v-if="captchaSession" class="p-4 bg-gray-50 rounded-lg border border-pink-200 space-y-3">
        <p class="text-sm text-pink-600 font-medium">
          <i class="fas fa-shield-alt mr-2"></i>学校系统要求输入验证码
        </p>
        <div class="flex justify-center">
          <img :src="captchaSession.captchaImageBase64" alt="验证码"
               class="rounded border border-gray-300 bg-white"
               style="max-height: 60px;">
        </div>
        <input v-model="captchaCode" type="text"
               placeholder="请输入验证码（不区分大小写）"
               class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
      </div>

      <button @click="handleSubmit" :disabled="isLoading"
              class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
        <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>验证中...</span>
        <span v-else>验证并保存</span>
      </button>

      <div class="bg-amber-50 border border-amber-300 text-amber-700 px-4 py-3 rounded-lg text-sm">
        <i class="fas fa-info-circle mr-2"></i>短信/扫码登录的账号没有保存学校密码，无法自动续签 JWS，可在此补上
      </div>
    </div>
  </BaseModal>
</template>
