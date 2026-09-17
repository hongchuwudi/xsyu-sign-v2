<script setup>
// 用户首页（仪表盘）：账户信息 + 最近5次签到 + JWS 登录有效期（重点展示）
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/utils/api'
import { getSignStatusDesc, getSignStatusClass, formatDateTime } from '@/utils/signStatus'
import BottomNav from '@/components/BottomNav.vue'
import UserMenuButton from '@/components/UserMenuButton.vue'

const router = useRouter()

// JWS 有效期基准天数（后端约 28 天）
const JWS_VALID_DAYS = 28

const info = ref(null)
const recentSigns = ref([])
const isLoading = ref(false)
const loadError = ref('')

onMounted(async () => {
  isLoading.value = true
  try {
    const [infoRes, signRes] = await Promise.all([
      api.getUserInfo(),
      api.getAllSigns(1, 5)
    ])
    if (infoRes.data.code === 200) info.value = infoRes.data.data
    if (signRes.data.code === 200) recentSigns.value = (signRes.data.data || []).slice(0, 5)
  } catch (e) {
    loadError.value = e.response?.data?.message || '加载失败，请下拉检查网络后刷新'
    console.error('加载首页数据失败:', e)
  } finally {
    isLoading.value = false
  }
})

// ==================== JWS 剩余时间 ====================

const jws = computed(() => {
  const refreshedAt = info.value?.jwsRefreshedAt
  if (!refreshedAt) {
    return { known: false, expired: false, percent: 0 }
  }
  const start = new Date(refreshedAt).getTime()
  const end = start + JWS_VALID_DAYS * 24 * 3600 * 1000
  const remainingMs = end - Date.now()
  return {
    known: true,
    start: new Date(start),
    end: new Date(end),
    remainingMs: Math.max(remainingMs, 0),
    expired: remainingMs <= 0,
    percent: Math.max(0, Math.min(100, (remainingMs / (JWS_VALID_DAYS * 24 * 3600 * 1000)) * 100))
  }
})

const jwsRemainText = computed(() => {
  if (!jws.value.known) return '未知'
  if (jws.value.expired) return '已过期'
  const totalHours = Math.floor(jws.value.remainingMs / 3600000)
  const days = Math.floor(totalHours / 24)
  const hours = totalHours % 24
  return days > 0 ? `${days} 天 ${hours} 小时` : `${hours} 小时`
})

const jwsLevel = computed(() => {
  if (!jws.value.known || jws.value.expired) return 'danger'
  if (jws.value.remainingMs <= 3 * 24 * 3600 * 1000) return 'warn'
  return 'ok'
})

const jwsCardClass = computed(() => ({
  ok: 'border-emerald-200 bg-emerald-50/60',
  warn: 'border-amber-300 bg-amber-50/60',
  danger: 'border-rose-300 bg-rose-50/60'
}[jwsLevel.value]))

const jwsBarClass = computed(() => ({
  ok: 'bg-emerald-400',
  warn: 'bg-amber-400',
  danger: 'bg-rose-400'
}[jwsLevel.value]))

const jwsBadge = computed(() => ({
  ok: { text: '正常', cls: 'bg-emerald-100 text-emerald-700' },
  warn: { text: '即将到期', cls: 'bg-amber-100 text-amber-700' },
  danger: { text: jws.value.expired ? '已过期' : '未知', cls: 'bg-rose-100 text-rose-700' }
}[jwsLevel.value]))

function formatFull(d) {
  if (!d) return '--'
  const t = new Date(d)
  return `${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')} ${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-pink-50 to-rose-50 pb-14">
    <!-- 顶部标题栏 -->
    <div class="bg-white shadow-sm border-b border-pink-100 sticky top-0 z-10">
      <div class="max-w-3xl mx-auto px-4 py-2 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <i class="fas fa-home text-pink-400 text-xl"></i>
          <h1 class="text-lg font-bold text-gray-800">首页</h1>
        </div>
        <UserMenuButton />
      </div>
    </div>

    <div class="max-w-3xl mx-auto p-4 space-y-4">
      <!-- 加载中 -->
      <div v-if="isLoading" class="text-center py-16">
        <i class="fas fa-spinner fa-spin text-pink-400 text-3xl mb-3"></i>
        <p class="text-pink-400">加载中...</p>
      </div>

      <template v-else>
        <!-- ===== JWS 登录有效期（重点） ===== -->
        <div class="rounded-xl border p-5 shadow-sm" :class="jwsCardClass">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-bold text-gray-800 flex items-center">
              <i class="fas fa-shield-halved text-pink-400 mr-2"></i>JWS 登录有效期
            </h3>
            <span class="text-xs px-2.5 py-1 rounded-full font-medium" :class="jwsBadge.cls">
              {{ jwsBadge.text }}
            </span>
          </div>

          <div class="mb-3">
            <span v-if="jws.known && !jws.expired" class="text-3xl font-bold text-gray-800">
              剩余 {{ jwsRemainText }}
            </span>
            <span v-else-if="jws.expired" class="text-2xl font-bold text-rose-600">
              已过期，请重新登录
            </span>
            <span v-else class="text-2xl font-bold text-rose-600">
              暂无续签记录，请重新登录
            </span>
          </div>

          <!-- 进度条 -->
          <div class="w-full h-2.5 bg-white rounded-full overflow-hidden border border-gray-200">
            <div class="h-full transition-all" :class="jwsBarClass"
                 :style="{ width: jws.percent + '%' }"></div>
          </div>

          <div class="flex justify-between text-xs text-gray-500 mt-2">
            <span>上次续签 {{ jws.known ? formatFull(jws.start) : '--' }}</span>
            <span>预计到期 {{ jws.known ? formatFull(jws.end) : '--' }}</span>
          </div>

          <p class="text-xs text-gray-500 mt-3 leading-relaxed">
            <i class="fas fa-circle-info mr-1"></i>
            通过短信/扫码登录且未保存学校密码的账号，无法自动续期 token，需每隔 28 天重新登录以刷新 token。为方便及时处理，可填写邮箱以接收过期提醒。
            <span v-if="jwsLevel !== 'ok'" class="text-rose-600 font-medium">
              剩余时间不足，请尽快重新登录或更新学校密码，否则机器人将无法自动签到。
            </span>
          </p>
        </div>

        <!-- ===== 账户信息 ===== -->
        <div class="bg-white rounded-xl border border-pink-200 p-5 shadow-sm">
          <h3 class="font-bold text-gray-800 mb-4 flex items-center">
            <i class="fas fa-id-card text-pink-400 mr-2"></i>账户信息
          </h3>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3 text-sm">
            <div class="flex justify-between border-b border-pink-50 pb-2">
              <span class="text-gray-400">学号</span>
              <span class="font-medium text-gray-800">{{ info?.username || '--' }}</span>
            </div>
            <div class="flex justify-between border-b border-pink-50 pb-2">
              <span class="text-gray-400">姓名</span>
              <span class="font-medium text-gray-800">{{ info?.name || '--' }}</span>
            </div>
            <div class="flex justify-between border-b border-pink-50 pb-2">
              <span class="text-gray-400">手机号</span>
              <span class="font-medium text-gray-800">{{ info?.phone || '未绑定' }}</span>
            </div>
            <div class="flex justify-between border-b border-pink-50 pb-2">
              <span class="text-gray-400">邮箱</span>
              <span class="font-medium text-gray-800 truncate max-w-[60%]">{{ info?.email || '未设置' }}</span>
            </div>
            <div class="flex justify-between border-b border-pink-50 pb-2">
              <span class="text-gray-400">自动签到</span>
              <span class="px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="info?.autoSign ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'">
                {{ info?.autoSign ? '已开启' : '已关闭' }}
              </span>
            </div>
            <div class="flex justify-between border-b border-pink-50 pb-2">
              <span class="text-gray-400">签到时段</span>
              <span class="font-medium text-gray-800">
                {{ info?.signStartTime && info?.signEndTime ? info.signStartTime + ' - ' + info.signEndTime : '未设置' }}
              </span>
            </div>
          </div>
        </div>

        <!-- ===== 最近签到 ===== -->
        <div class="bg-white rounded-xl border border-pink-200 p-5 shadow-sm">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-gray-800 flex items-center">
              <i class="fas fa-clock-rotate-left text-pink-400 mr-2"></i>最近签到
            </h3>
            <button @click="router.push('/sign')" class="text-xs text-pink-500 hover:text-pink-600">
              查看全部 <i class="fas fa-chevron-right ml-0.5"></i>
            </button>
          </div>

          <div v-if="recentSigns.length === 0" class="text-center py-8">
            <i class="fas fa-inbox text-pink-200 text-3xl mb-2"></i>
            <p class="text-gray-400 text-sm">暂无签到记录</p>
          </div>

          <div v-else class="space-y-2">
            <div v-for="sign in recentSigns" :key="sign.id"
                 @click="router.push('/sign')"
                 class="flex items-center justify-between p-3 rounded-lg bg-pink-50/60 hover:bg-pink-50 cursor-pointer transition-colors">
              <div class="flex-1 mr-3 min-w-0">
                <p class="text-sm font-medium text-gray-800 truncate">{{ sign.signTitle || '未命名签到' }}</p>
                <p class="text-xs text-gray-400 mt-0.5">
                  <i class="far fa-clock mr-1"></i>{{ formatDateTime(sign.start) }}
                </p>
              </div>
              <span :class="getSignStatusClass(sign)"
                    class="flex-shrink-0 text-xs px-2.5 py-1 rounded-full font-medium">
                {{ getSignStatusDesc(sign) }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="loadError" class="bg-rose-50 border border-rose-200 text-rose-600 px-4 py-3 rounded-lg text-sm">
          <i class="fas fa-exclamation-circle mr-2"></i>{{ loadError }}
        </div>
      </template>
    </div>

    <BottomNav />
  </div>
</template>
