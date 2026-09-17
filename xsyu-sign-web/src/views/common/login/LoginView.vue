<script setup>
// 登录模块：密码（学号+学校密码，含风控验证码）/ 短信 / 扫码 三种方式
// 迁移来源：旧前端 static/js/components/LoginPage.js（登录状态机内聚到本组件）
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { api } from '@/utils/api'
import { rsaEncrypt } from '@/utils/crypto'

const router = useRouter()
const userStore = useUserStore()

const tab = ref('password')
const username = ref('')
const casPsd = ref('')
const captchaCode = ref('')
const captchaSession = ref(null)
const phone = ref('')
const smsCode = ref('')
const smsSessionId = ref('')
const smsCountdown = ref(0)
const qrSession = ref(null)
const isLoading = ref(false)
const error = ref('')

let smsTimer = null
let qrTimer = null

function showError(msg) {
  error.value = msg
}

onMounted(() => {if (userStore.isLoggedIn) router.push('/')})

onBeforeUnmount(() => {
  stopSmsCountdown()
  stopQrPolling()
})

// ==================== 登录成功 ====================

function onLoginSuccess(data) {
  stopSmsCountdown()
  stopQrPolling()
  userStore.setUserInfo(data)
  router.push('/')
}

async function encryptPassword(password) {
  const res = await api.getPublicKey()
  return rsaEncrypt(password, res.data.data.publicKey)
}

// ==================== 密码登录 ====================

async function handlePasswordLogin() {
  if (!username.value) return showError('请输入学号')
  if (!casPsd.value) return showError('请输入学校密码')
  isLoading.value = true
  error.value = ''
  try {
    const enc = await encryptPassword(casPsd.value)
    const res = await api.xsyLogin(
      username.value, enc,
      captchaSession.value ? captchaSession.value.captchaSessionId : null,
      captchaCode.value || null
    )
    if (res.data.code === 200) {
      onLoginSuccess(res.data.data)
    } else if (res.data.code === 1004 && res.data.data?.captchaSessionId) {
      captchaSession.value = res.data.data
      captchaCode.value = ''
      showError('学校系统要求输入验证码')
    } else {
      showError(res.data.message || '登录失败')
    }
  } catch (e) {
    showError(e.response?.data?.message || '登录请求失败')
  } finally {
    isLoading.value = false
  }
}

// ==================== 短信登录 ====================

function startSmsCountdown() {
  stopSmsCountdown()
  smsCountdown.value = 60
  smsTimer = setInterval(() => {
    smsCountdown.value--
    if (smsCountdown.value <= 0) stopSmsCountdown()
  }, 1000)
}

function stopSmsCountdown() {
  if (smsTimer) clearInterval(smsTimer)
  smsTimer = null
  smsCountdown.value = 0
}

async function handleSendSms() {
  if (!username.value) return showError('请先输入学号')
  if (!/^1[3-9]\d{9}$/.test(phone.value)) return showError('请输入正确的手机号')
  error.value = ''
  // 点击立即倒计时防止连点，失败回滚
  startSmsCountdown()
  try {
    const res = await api.smsSend(phone.value)
    if (res.data.code === 200) {
      smsSessionId.value = res.data.data.smsSessionId
    } else {
      stopSmsCountdown()
      showError(res.data.message || '发送失败')
    }
  } catch (e) {
    stopSmsCountdown()
    showError(e.response?.data?.message || '发送失败')
  }
}

async function handleSmsLogin() {
  if (!username.value) return showError('请先输入学号')
  if (!phone.value || !smsCode.value) return showError('请输入手机号和验证码')
  isLoading.value = true
  error.value = ''
  try {
    const res = await api.smsLogin(smsSessionId.value, username.value, phone.value, smsCode.value)
    if (res.data.code === 200) {
      onLoginSuccess(res.data.data)
    } else {
      showError(res.data.message || '登录失败')
    }
  } catch (e) {
    showError(e.response?.data?.message || '登录失败')
  } finally {
    isLoading.value = false
  }
}

// ==================== 扫码登录 ====================

async function handleQrCreate() {
  if (!username.value) return showError('请先输入学号')
  isLoading.value = true
  error.value = ''
  try {
    const res = await api.qrCreate(username.value)
    if (res.data.code === 200) {
      qrSession.value = { ...res.data.data, status: 'WAITING' }
      startQrPolling()
    } else {
      showError(res.data.message || '创建二维码失败')
    }
  } catch (e) {
    showError(e.response?.data?.message || '创建二维码失败')
  } finally {
    isLoading.value = false
  }
}

function startQrPolling() {
  stopQrPolling()
  let polls = 0
  qrTimer = setInterval(async () => {
    polls++
    if (!qrSession.value) return stopQrPolling()
    if (polls > 150) {
      qrSession.value.status = 'EXPIRED'
      stopQrPolling()
      return showError('二维码已过期，请点击刷新')
    }
    try {
      const res = await api.qrPoll(qrSession.value.qrSessionId)
      if (res.data.code !== 200) return
      const result = res.data.data
      if (result.status === 'SUCCESS' && result.loginVO) {
        onLoginSuccess(result.loginVO)
      } else if (result.status === 'EXPIRED') {
        qrSession.value.status = 'EXPIRED'
        stopQrPolling()
        showError('二维码已过期，请点击刷新')
      }
    } catch {
      // 单次轮询失败忽略，下次继续
    }
  }, 2000)
}

function stopQrPolling() {
  if (qrTimer) clearInterval(qrTimer)
  qrTimer = null
}
</script>

<template>
  <div class="flex items-center justify-center min-h-screen p-4 bg-gradient-to-br from-pink-50 to-rose-50">
    <div class="w-full max-w-md bg-white/90 backdrop-blur-sm rounded-xl shadow-lg p-8 border border-pink-200">
      <div class="text-center mb-6">
        <div class="w-16 h-16 bg-gradient-to-r from-pink-400 to-rose-400 rounded-full flex items-center justify-center mx-auto mb-4">
          <i class="fas fa-user-graduate text-white text-2xl"></i>
        </div>
        <h2 class="text-2xl font-bold text-pink-800">油签机系统</h2>
        <p class="text-pink-500 mt-2">选择登录方式</p>
      </div>

      <!-- 方式切换 -->
      <div class="flex bg-pink-50 rounded-lg p-1 mb-6">
        <button @click="tab = 'password'"
                :class="tab === 'password' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                class="flex-1 px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
          <i class="fas fa-key mr-1"></i>密码
        </button>
        <button @click="tab = 'sms'"
                :class="tab === 'sms' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                class="flex-1 px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
          <i class="fas fa-comment-sms mr-1"></i>短信
        </button>
        <button @click="tab = 'qr'"
                :class="tab === 'qr' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                class="flex-1 px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
          <i class="fas fa-qrcode mr-1"></i>扫码
        </button>
      </div>

      <!-- 学号（三种方式共用，作为唯一标识） -->
      <div class="mb-4">
        <label class="block text-pink-700 text-sm font-medium mb-2">学号</label>
        <input v-model="username" type="text" placeholder="请输入学号"
               class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
      </div>

      <!-- 密码登录 -->
      <form v-if="tab === 'password'" @submit.prevent="handlePasswordLogin" class="space-y-4">
        <div>
          <label class="block text-pink-700 text-sm font-medium mb-2">学校密码（统一认证密码）</label>
          <input v-model="casPsd" type="password" required placeholder="请输入学校统一认证密码"
                 class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
        </div>

        <!-- 学校系统验证码（风控触发时显示） -->
        <div v-if="captchaSession" class="p-4 bg-gray-50 rounded-lg border border-pink-200 space-y-3">
          <p class="text-sm text-pink-600 font-medium">
            <i class="fas fa-shield-alt mr-2"></i>学校系统要求输入验证码
          </p>
          <div class="flex justify-center">
            <img :src="captchaSession.captchaImageBase64" alt="验证码"
                 class="rounded border border-gray-300 bg-white"
                 style="max-height: 60px;">
          </div>
          <p class="text-xs text-gray-400 text-center">验证码输错后提交会自动刷新</p>
          <input v-model="captchaCode" type="text"
                 placeholder="请输入验证码（不区分大小写）"
                 class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
        </div>

        <button type="submit" :disabled="isLoading"
                class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
          <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>登录中...</span>
          <span v-else>{{ captchaSession ? '验证并登录' : '登录' }}</span>
        </button>

        <div class="bg-amber-50 border border-amber-300 text-amber-700 px-4 py-3 rounded-lg text-sm">
          <i class="fas fa-info-circle mr-2"></i>首次登录自动创建账号；连续输错学校密码可能触发验证码或账号锁定
        </div>
      </form>

      <!-- 短信登录 -->
      <div v-else-if="tab === 'sms'" class="space-y-4">
        <div>
          <label class="block text-pink-700 text-sm font-medium mb-2">学校系统绑定的手机号</label>
          <div class="flex gap-2">
            <input v-model="phone" type="tel" placeholder="请输入手机号"
                   class="flex-1 px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
            <button @click="handleSendSms" :disabled="smsCountdown > 0"
                    class="px-4 py-3 rounded-lg bg-pink-100 text-pink-600 text-sm font-medium hover:bg-pink-200 transition-colors whitespace-nowrap disabled:opacity-50">
              {{ smsCountdown > 0 ? smsCountdown + 's' : '发送验证码' }}
            </button>
          </div>
        </div>

        <div>
          <label class="block text-pink-700 text-sm font-medium mb-2">短信验证码</label>
          <input v-model="smsCode" type="text" placeholder="请输入短信验证码"
                 class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
        </div>

        <button @click="handleSmsLogin" :disabled="isLoading"
                class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
          <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>登录中...</span>
          <span v-else>登录</span>
        </button>

        <div class="bg-amber-50 border border-amber-300 text-amber-700 px-4 py-3 rounded-lg text-sm">
          <i class="fas fa-info-circle mr-2"></i>手机号需与输入的学号在同一学校账号下绑定
        </div>
      </div>

      <!-- 扫码登录 -->
      <div v-else-if="tab === 'qr'" class="space-y-4">
        <template v-if="qrSession && qrSession.qrImageBase64">
          <div class="flex justify-center">
            <img :src="qrSession.qrImageBase64" alt="登录二维码"
                 class="w-56 h-56 rounded-lg border border-pink-200 bg-white">
          </div>
          <p class="text-center text-sm text-pink-500">
            <i class="fas fa-mobile-alt mr-1"></i>请用手机校园APP扫描二维码确认登录
          </p>
          <p v-if="qrSession.status === 'WAITING'" class="text-center text-xs text-gray-400">
            <i class="fas fa-spinner fa-spin mr-1"></i>等待扫码中...
          </p>
          <p v-else class="text-center text-xs text-amber-600">二维码已过期，请刷新</p>
          <button @click="handleQrCreate"
                  class="w-full border border-pink-300 text-pink-600 py-2.5 rounded-lg text-sm font-medium hover:bg-pink-50 transition-colors">
            <i class="fas fa-sync-alt mr-1"></i>刷新二维码
          </button>
        </template>
        <template v-else>
          <p class="text-center text-sm text-pink-500 py-6">
            <i class="fas fa-qrcode mr-2"></i>生成二维码后，用手机校园APP扫码即可登录
          </p>
          <button @click="handleQrCreate" :disabled="isLoading"
                  class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
            <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>生成中...</span>
            <span v-else>生成二维码</span>
          </button>
        </template>
      </div>

      <div v-if="error" class="bg-rose-50 border border-rose-200 text-rose-600 px-4 py-3 rounded-lg text-sm mt-4">
        <i class="fas fa-exclamation-circle mr-2"></i>{{ error }}
      </div>

      <div class="text-center mt-6 pt-4 border-t border-pink-100">
        <p class="text-xs text-pink-400">陕ICP备2026004528号-1</p>
      </div>
    </div>
  </div>
</template>
