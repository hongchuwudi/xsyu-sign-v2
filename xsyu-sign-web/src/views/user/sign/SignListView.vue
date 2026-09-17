<script setup>
// 签到列表页：操作栏 + 筛选 + 卡片列表 + 分页（用户菜单/弹窗由 HeaderNav 内的 UserMenuButton 提供）
// 迁移来源：旧前端 SignList.js
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import { getSignStatusDesc, pendingCount } from '@/utils/signStatus'
import HeaderNav from '@/components/HeaderNav.vue'
import BottomNav from '@/components/BottomNav.vue'
import SignCard from '@/components/SignCard.vue'
import SignDetailModal from '@/components/SignDetailModal.vue'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const headerRef = ref(null)

const signs = ref([])
const isLoading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const searchQuery = ref('')
const filterStatus = ref(null)
const showDetailModal = ref(false)
const selectedSign = ref({})

const totalPages = computed(() => Math.ceil(total.value / pageSize))
const pending = computed(() => pendingCount(signs.value))

const filteredSigns = computed(() => {
  let filtered = signs.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    filtered = filtered.filter(s =>
      (s.signTitle && s.signTitle.toLowerCase().includes(q)) ||
      (s.signContext && s.signContext.toLowerCase().includes(q)) ||
      (s.college && s.college.toLowerCase().includes(q))
    )
  }
  if (filterStatus.value !== null) {
    filtered = filtered.filter(s => getSignStatusDesc(s) === filterStatus.value)
  }
  return filtered
})

onMounted(() => {
  getAllSigns(1)
  if (userInfo.value && !userInfo.value.signDays) showSignDaysPrompt()
})

// ==================== 签到列表 ====================

async function getAllSigns(p) {
  isLoading.value = true
  page.value = p
  try {
    const res = await api.getAllSigns(p, pageSize)
    if (res.data.code === 200) {
      signs.value = res.data.data || []
      total.value = res.data.total || signs.value.length
    } else {
      showMessage(res.data.message || '获取签到列表失败', 'error')
    }
  } catch (e) {
    showMessage(e.response?.data?.message || '获取签到列表失败', 'error')
  } finally {
    isLoading.value = false
  }
}

async function handleOneKeySign() {
  isLoading.value = true
  try {
    const res = await api.oneKeySign()
    // 后端该接口直接返回文本结果
    showMessage(res.data || '一键签到完成')
    getAllSigns(page.value)
  } catch (e) {
    showMessage(e.response?.data?.message || '一键签到失败', 'error')
  } finally {
    isLoading.value = false
  }
}

async function handleSignSingle(sign) {
  isLoading.value = true
  try {
    const res = await api.signSingle(sign)
    showMessage(res.data || '签到成功')
    getAllSigns(page.value)
  } catch (e) {
    showMessage(e.response?.data?.message || '签到失败', 'error')
  } finally {
    isLoading.value = false
  }
}

function prevPage() {
  if (page.value > 1) getAllSigns(page.value - 1)
}

function nextPage() {
  if (page.value < totalPages.value) getAllSigns(page.value + 1)
}

function openSignDetail(sign) {
  selectedSign.value = sign
  showDetailModal.value = true
}

// 首次进入且未设置签到日期时的引导
function showSignDaysPrompt() {
  const ok = confirm('您还未设置签到日期配置，系统将默认每天签到。\n\n是否现在设置签到日期？\n（点击"确定"前往设置，点击"取消"保持默认每天签到）')
  if (ok) {
    headerRef.value?.openEditProfile()
  } else {
    api.setSignDays('0,1,2,3,4,5,6')
    userStore.setUserInfo({ ...userInfo.value, signDays: '0,1,2,3,4,5,6' })
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col pb-14">
    <HeaderNav ref="headerRef" :user-info="userInfo" />

    <main class="flex-1 container mx-auto px-4 py-6">
      <!-- 操作栏 -->
      <div class="mb-6 space-y-4">
        <div class="flex flex-wrap items-center gap-3">
          <button @click="getAllSigns(1)" :disabled="isLoading"
                  class="bg-pink-400 text-white px-4 py-2 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center">
            <i class="fas fa-sync-alt mr-2" :class="{ 'fa-spin': isLoading }"></i>刷新列表
          </button>

          <button @click="handleOneKeySign" :disabled="isLoading"
                  class="bg-rose-400 hover:opacity-90 text-white px-4 py-2 rounded-lg font-medium transition-opacity flex items-center">
            <i class="fas fa-bolt mr-2"></i>一键签到
            <span v-if="pending > 0" class="ml-2 bg-white text-rose-400 text-xs px-2 py-0.5 rounded-full">
              {{ pending }}
            </span>
          </button>

          <div class="flex-1 min-w-[200px]">
            <div class="relative">
              <input v-model="searchQuery" type="text" placeholder="搜索签到标题、内容..."
                     class="w-full px-4 py-2 pl-10 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
              <i class="fas fa-search absolute left-3 top-2.5 text-pink-300"></i>
            </div>
          </div>
        </div>

        <!-- 状态筛选 -->
        <div class="flex flex-wrap gap-2">
          <button @click="filterStatus = null"
                  :class="filterStatus === null ? 'bg-pink-400 text-white' : 'bg-pink-100 text-pink-600'"
                  class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors">
            全部
          </button>
          <button @click="filterStatus = '待签到'"
                  :class="filterStatus === '待签到' ? 'bg-fuchsia-400 text-white' : 'bg-pink-100 text-pink-600'"
                  class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center">
            <i class="fas fa-clock mr-1"></i>待签到
          </button>
          <button @click="filterStatus = '已签到'"
                  :class="filterStatus === '已签到' ? 'bg-rose-400 text-white' : 'bg-pink-100 text-pink-600'"
                  class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center">
            <i class="fas fa-check mr-1"></i>已签到
          </button>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="isLoading && signs.length === 0" class="flex justify-center items-center py-20">
        <div class="text-center">
          <i class="fas fa-spinner fa-spin text-pink-400 text-3xl mb-4"></i>
          <p class="text-pink-600">加载中...</p>
        </div>
      </div>

      <!-- 空数据 -->
      <div v-else-if="filteredSigns.length === 0" class="text-center py-20">
        <i class="fas fa-inbox text-pink-300 text-4xl mb-4"></i>
        <p class="text-pink-500 mb-2">暂无签到记录</p>
        <button @click="getAllSigns(1)" class="text-pink-500 hover:text-pink-600 text-sm">
          <i class="fas fa-redo mr-1"></i>刷新试试
        </button>
      </div>

      <!-- 签到卡片列表 -->
      <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <SignCard v-for="sign in filteredSigns" :key="sign.id"
                  :sign="sign"
                  @view-detail="openSignDetail"
                  @sign-single="handleSignSingle" />
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <div class="flex items-center gap-2 bg-white/80 backdrop-blur-sm rounded-lg shadow-sm p-2 border border-pink-200">
          <button @click="prevPage" :disabled="page === 1"
                  class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-pink-100 disabled:opacity-50 text-pink-600">
            <i class="fas fa-chevron-left"></i>
          </button>
          <span class="text-sm text-pink-600 px-2">第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
          <button @click="nextPage" :disabled="page === totalPages"
                  class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-pink-100 disabled:opacity-50 text-pink-600">
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </div>
    </main>

    <BottomNav />

    <SignDetailModal :visible="showDetailModal"
                     :sign="selectedSign"
                     @close="showDetailModal = false"
                     @sign="handleSignSingle" />
  </div>
</template>
