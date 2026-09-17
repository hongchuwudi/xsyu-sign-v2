<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import AdminUserDetailModal from '@/components/AdminUserDetailModal.vue'
import AdminUserFormModal from '@/components/AdminUserFormModal.vue'
import ConfirmActionModal from '@/components/ConfirmActionModal.vue'

const router = useRouter()
const userStore = useUserStore()
const stats = ref({ total: 0, autoSignCount: 0, invalidJwsCount: 0, todayActiveCount: 0 })
const users = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const serverPages = ref(0)
const keyword = ref('')
const filter = ref('all')
const isLoading = ref(true)
const loadError = ref('')
const busyAction = ref('')
const formMode = ref('edit')
const formUser = ref(null)
const showForm = ref(false)
const formLoading = ref(false)
const confirmAction = ref(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailUser = ref(null)
const detailSigns = ref([])

let searchTimer = null
let requestSequence = 0

const totalPages = computed(() => Math.max(1, serverPages.value || Math.ceil(total.value / pageSize.value)))
const rangeStart = computed(() => total.value ? (currentPage.value - 1) * pageSize.value + 1 : 0)
const rangeEnd = computed(() => Math.min(currentPage.value * pageSize.value, total.value))
const filters = computed(() => [
  { value: 'all', label: '全部', count: stats.value.total },
  { value: 'autoSign', label: '自动签到', count: stats.value.autoSignCount },
  { value: 'noJws', label: 'JWS 失效', count: stats.value.invalidJwsCount }
])
const statCards = computed(() => [
  { label: '总用户', value: stats.value.total, icon: 'fas fa-users', tone: 'pink' },
  { label: '自动签到', value: stats.value.autoSignCount, icon: 'fas fa-robot', tone: 'emerald' },
  { label: 'JWS 失效', value: stats.value.invalidJwsCount, icon: 'fas fa-shield-circle-exclamation', tone: 'rose' },
  { label: '今日活跃', value: stats.value.todayActiveCount, icon: 'fas fa-chart-line', tone: 'amber' }
])

function assertSuccess(response, fallback) {
  if (response.data?.code !== 200) throw new Error(response.data?.message || fallback)
  return response.data.data
}

function errorMessage(error, fallback = '操作失败') {
  return error.response?.data?.message || error.message || fallback
}

async function loadStats() {
  const response = await api.getUserStats()
  stats.value = { ...stats.value, ...assertSuccess(response, '统计数据加载失败') }
}

async function loadUsers() {
  const sequence = ++requestSequence
  isLoading.value = true
  loadError.value = ''
  try {
    const response = await api.getUsersByPage(
      currentPage.value,
      pageSize.value,
      keyword.value.trim(),
      filter.value === 'all' ? '' : filter.value
    )
    const data = assertSuccess(response, '用户列表加载失败') || {}
    if (sequence !== requestSequence) return
    users.value = data.records || []
    total.value = Number(data.total || 0)
    serverPages.value = Number(data.pages || 0)
    currentPage.value = Number(data.current || currentPage.value)
  } catch (error) {
    if (sequence !== requestSequence) return
    users.value = []
    loadError.value = errorMessage(error, '用户列表加载失败')
  } finally {
    if (sequence === requestSequence) isLoading.value = false
  }
}

async function loadDashboard() {
  const results = await Promise.allSettled([loadStats(), loadUsers()])
  if (results[0].status === 'rejected') showMessage(errorMessage(results[0].reason, '统计数据加载失败'), 'error')
}

function setFilter(value) {
  if (filter.value === value) return
  filter.value = value
  currentPage.value = 1
  loadUsers()
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadUsers()
  }, 400)
}

function clearSearch() {
  if (!keyword.value) return
  keyword.value = ''
  currentPage.value = 1
  loadUsers()
}

function clearFilters() {
  keyword.value = ''
  filter.value = 'all'
  currentPage.value = 1
  loadUsers()
}

function changePage(page) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  loadUsers()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function runAction(key, request, successMessage) {
  busyAction.value = key
  try {
    assertSuccess(await request(), successMessage)
    showMessage(successMessage)
    await Promise.all([loadStats(), loadUsers()])
    return true
  } catch (error) {
    showMessage(errorMessage(error), 'error')
    return false
  } finally {
    busyAction.value = ''
  }
}

async function toggleAutoSign(user) {
  await runAction(
    `toggle-${user.username}`,
    () => api.toggleUserAutoSign(user.username, !user.autoSign),
    `${user.username} 已${user.autoSign ? '关闭' : '开启'}自动签到`
  )
}

function askAction(type, user = null) {
  const definitions = {
    refresh: { title: '刷新 JWS', message: '将使用该用户保存的学校密码重新获取登录状态。', detail: user?.username, confirmText: '确认刷新', icon: 'fas fa-arrows-rotate' },
    sign: { title: '为用户签到', message: '立即拉取该用户当前可签到任务并依次执行。', detail: user?.username, confirmText: '开始签到', icon: 'fas fa-calendar-check' },
    delete: { title: '删除用户', message: '该用户的本地账号与配置将被永久删除，此操作无法撤销。', detail: `${user?.name || '未命名用户'} (${user?.username})`, confirmText: '确认删除', icon: 'fas fa-trash-can', danger: true },
    signAll: { title: '为全部用户签到', message: '将立即为所有符合条件的普通用户执行签到，任务可能需要一些时间。', detail: `当前共 ${stats.value.total} 个用户`, confirmText: '开始全签', icon: 'fas fa-check-double' }
  }
  confirmAction.value = { type, user, ...definitions[type] }
}

async function confirmCurrentAction() {
  const action = confirmAction.value
  if (!action) return
  const username = action.user?.username
  const requests = {
    refresh: () => api.refreshUserJws(username),
    sign: () => api.signByAdmin(username),
    delete: () => api.deleteUser(username),
    signAll: () => api.signAllUsers()
  }
  const messages = {
    refresh: `${username} 的 JWS 已刷新`,
    sign: `${username} 的签到任务已执行`,
    delete: `${username} 已删除`,
    signAll: '全部用户签到任务已执行'
  }
  if (await runAction(`confirm-${action.type}`, requests[action.type], messages[action.type])) confirmAction.value = null
}

function openAdd() {
  formMode.value = 'add'
  formUser.value = null
  showForm.value = true
}

function openEdit(user) {
  formMode.value = 'edit'
  formUser.value = user
  showForm.value = true
}

async function submitForm(data) {
  formLoading.value = true
  try {
    const response = formMode.value === 'add' ? await api.addUser(data) : await api.updateUser(formUser.value.username, data)
    assertSuccess(response, formMode.value === 'add' ? '用户创建失败' : '用户更新失败')
    showMessage(formMode.value === 'add' ? '用户创建成功' : '用户信息已更新')
    showForm.value = false
    await Promise.all([loadStats(), loadUsers()])
  } catch (error) {
    showMessage(errorMessage(error), 'error')
  } finally {
    formLoading.value = false
  }
}

async function openDetail(user) {
  detailVisible.value = true
  detailLoading.value = true
  detailUser.value = user
  detailSigns.value = []
  const [userResult, signsResult] = await Promise.allSettled([api.getUserDetail(user.username), api.getUserSigns(user.username, 10)])
  if (userResult.status === 'fulfilled') {
    try {
      detailUser.value = assertSuccess(userResult.value, '用户详情加载失败')
    } catch (error) {
      showMessage(errorMessage(error), 'error')
    }
  } else {
    showMessage(errorMessage(userResult.reason, '用户详情加载失败'), 'error')
  }
  if (signsResult.status === 'fulfilled' && signsResult.value.data?.code === 200) detailSigns.value = signsResult.value.data.data || []
  detailLoading.value = false
}

async function logout() {
  try {
    await api.logout()
  } catch {
    // 即使服务端会话已失效，也必须清理本地登录状态
  } finally {
    userStore.clear()
    router.replace('/login')
  }
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

function formatDays(value) {
  if (!value) return '未设置'
  const days = value.split(',').filter(Boolean)
  return days.length === 7 ? '每天' : `${days.length} 天/周`
}

onMounted(loadDashboard)
onBeforeUnmount(() => clearTimeout(searchTimer))
</script>

<template>
  <div>
    <header class="sticky top-0 z-20 border-b border-pink-200 bg-white/95 backdrop-blur-sm">
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-rose-500 text-white shadow-sm"><i class="fas fa-shield-halved" aria-hidden="true"></i></div>
          <div class="min-w-0">
            <h1 class="truncate text-base font-bold text-gray-900 sm:text-lg">用户管理</h1>
            <p class="hidden text-xs text-gray-400 sm:block">油签机管理员控制台</p>
          </div>
        </div>
        <button type="button" class="rounded-lg border border-pink-200 px-3 py-2 text-sm font-medium text-pink-600 transition-colors hover:bg-pink-50 focus:outline-none focus:ring-2 focus:ring-pink-300" @click="logout">
          <i class="fas fa-right-from-bracket sm:mr-1" aria-hidden="true"></i><span class="hidden sm:inline">退出登录</span>
        </button>
      </div>
    </header>

    <main class="mx-auto max-w-7xl space-y-4 px-4 py-5 sm:px-6 sm:py-6">
      <section aria-label="用户概览" class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div v-for="card in statCards" :key="card.label" class="flex min-h-[92px] items-center justify-between rounded-lg border bg-white p-4 shadow-sm" :class="{ 'border-pink-200': card.tone === 'pink', 'border-emerald-200': card.tone === 'emerald', 'border-rose-200': card.tone === 'rose', 'border-amber-200': card.tone === 'amber' }">
          <div><p class="text-xs font-medium text-gray-500 sm:text-sm">{{ card.label }}</p><p class="mt-1 text-2xl font-bold text-gray-900 sm:text-3xl">{{ card.value }}</p></div>
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg sm:h-11 sm:w-11" :class="{ 'bg-pink-50 text-pink-500': card.tone === 'pink', 'bg-emerald-50 text-emerald-500': card.tone === 'emerald', 'bg-rose-50 text-rose-500': card.tone === 'rose', 'bg-amber-50 text-amber-500': card.tone === 'amber' }"><i :class="card.icon" class="text-lg" aria-hidden="true"></i></div>
        </div>
      </section>

      <section class="rounded-lg border border-pink-200 bg-white shadow-sm">
        <div class="border-b border-pink-100 p-4">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div class="flex flex-wrap gap-2">
              <button type="button" :disabled="isLoading" title="刷新数据" class="flex h-10 items-center gap-2 rounded-lg bg-rose-500 px-3 text-sm font-medium text-white hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-50" @click="loadDashboard"><i class="fas fa-rotate" :class="{ 'fa-spin': isLoading }" aria-hidden="true"></i><span>刷新</span></button>
              <button type="button" class="flex h-10 items-center gap-2 rounded-lg border border-pink-200 bg-pink-50 px-3 text-sm font-medium text-pink-700 hover:bg-pink-100" @click="openAdd"><i class="fas fa-user-plus" aria-hidden="true"></i><span>新增用户</span></button>
              <button type="button" class="flex h-10 items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100" @click="askAction('signAll')"><i class="fas fa-check-double" aria-hidden="true"></i><span>全部签到</span></button>
            </div>
            <div class="relative w-full lg:max-w-sm">
              <i class="fas fa-magnifying-glass pointer-events-none absolute left-3 top-3 text-sm text-pink-300" aria-hidden="true"></i>
              <input v-model="keyword" type="search" placeholder="搜索学号、姓名或邮箱" class="h-10 w-full rounded-lg border border-gray-300 pl-9 pr-9 text-sm outline-none transition focus:border-pink-400 focus:ring-2 focus:ring-pink-100" @input="onSearchInput">
              <button v-if="keyword" type="button" aria-label="清除搜索" title="清除搜索" class="absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="clearSearch"><i class="fas fa-xmark" aria-hidden="true"></i></button>
            </div>
          </div>
          <div class="mt-4 flex gap-1 overflow-x-auto rounded-lg bg-pink-50 p-1 sm:w-fit">
            <button v-for="item in filters" :key="item.value" type="button" :class="filter === item.value ? 'bg-white text-rose-600 shadow-sm' : 'text-gray-500 hover:text-pink-600'" class="whitespace-nowrap rounded-md px-3 py-1.5 text-sm font-medium transition-colors" @click="setFilter(item.value)">{{ item.label }} <span class="ml-1 text-xs opacity-70">{{ item.count }}</span></button>
          </div>
        </div>

        <div v-if="isLoading && !users.length" class="py-20 text-center text-pink-400"><i class="fas fa-spinner fa-spin mb-3 text-3xl" aria-hidden="true"></i><p class="text-sm">正在加载用户</p></div>
        <div v-else-if="loadError" class="p-8 text-center"><i class="fas fa-triangle-exclamation mb-3 text-3xl text-rose-400" aria-hidden="true"></i><p class="text-sm font-medium text-gray-700">{{ loadError }}</p><button type="button" class="mt-4 rounded-lg border border-pink-200 px-4 py-2 text-sm font-medium text-pink-600 hover:bg-pink-50" @click="loadUsers">重新加载</button></div>
        <div v-else-if="!users.length" class="py-20 text-center"><i class="fas fa-user-slash mb-3 text-3xl text-pink-200" aria-hidden="true"></i><p class="text-sm font-medium text-gray-600">没有找到匹配的用户</p><button v-if="keyword || filter !== 'all'" type="button" class="mt-2 text-sm text-pink-500 hover:text-pink-600" @click="clearFilters">清除筛选</button></div>

        <template v-else>
          <div class="hidden overflow-x-auto md:block">
            <table class="w-full min-w-[920px] text-left">
              <thead class="border-b border-pink-100 bg-pink-50/60 text-xs font-semibold text-gray-500"><tr><th class="px-4 py-3">用户</th><th class="px-4 py-3">自动签到</th><th class="px-4 py-3">JWS</th><th class="px-4 py-3">签到计划</th><th class="px-4 py-3">最近更新</th><th class="px-4 py-3 text-right">操作</th></tr></thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="user in users" :key="user.id || user.username" class="transition-colors hover:bg-pink-50/40">
                  <td class="px-4 py-3"><button type="button" class="text-left" @click="openDetail(user)"><span class="block text-sm font-semibold text-gray-900 hover:text-rose-600">{{ user.name || '未填写姓名' }}</span><span class="block text-xs text-gray-400">{{ user.username }}<template v-if="user.email"> · {{ user.email }}</template></span></button></td>
                  <td class="px-4 py-3"><button type="button" role="switch" :aria-checked="Boolean(user.autoSign)" :aria-label="`${user.username} 自动签到`" :disabled="Boolean(busyAction)" :class="user.autoSign ? 'bg-emerald-500' : 'bg-gray-300'" class="relative h-6 w-11 rounded-full transition-colors disabled:cursor-not-allowed disabled:opacity-50" @click="toggleAutoSign(user)"><span :class="user.autoSign ? 'translate-x-5' : 'translate-x-0.5'" class="absolute left-0 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform"></span></button></td>
                  <td class="px-4 py-3"><span :class="user.jws ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'" class="inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium"><i :class="user.jws ? 'fas fa-circle-check' : 'fas fa-circle-xmark'" aria-hidden="true"></i>{{ user.jws ? '有效' : '失效' }}</span></td>
                  <td class="px-4 py-3 text-xs text-gray-600"><span class="block">{{ formatDays(user.signDays) }}</span><span class="mt-0.5 block text-gray-400">{{ user.signStartTime || '--' }} - {{ user.signEndTime || '--' }}</span></td>
                  <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(user.updatedAt) }}</td>
                  <td class="px-4 py-3"><div class="flex justify-end gap-1">
                    <button type="button" title="查看详情" :aria-label="`查看 ${user.username} 详情`" class="flex h-8 w-8 items-center justify-center rounded-lg text-gray-400 hover:bg-pink-50 hover:text-pink-600" @click="openDetail(user)"><i class="fas fa-eye" aria-hidden="true"></i></button>
                    <button type="button" title="立即签到" :aria-label="`为 ${user.username} 签到`" class="flex h-8 w-8 items-center justify-center rounded-lg text-emerald-500 hover:bg-emerald-50" @click="askAction('sign', user)"><i class="fas fa-calendar-check" aria-hidden="true"></i></button>
                    <button type="button" title="刷新 JWS" :aria-label="`刷新 ${user.username} JWS`" class="flex h-8 w-8 items-center justify-center rounded-lg text-amber-500 hover:bg-amber-50" @click="askAction('refresh', user)"><i class="fas fa-arrows-rotate" aria-hidden="true"></i></button>
                    <button type="button" title="编辑用户" :aria-label="`编辑 ${user.username}`" class="flex h-8 w-8 items-center justify-center rounded-lg text-blue-500 hover:bg-blue-50" @click="openEdit(user)"><i class="fas fa-pen" aria-hidden="true"></i></button>
                    <button type="button" title="删除用户" :aria-label="`删除 ${user.username}`" class="flex h-8 w-8 items-center justify-center rounded-lg text-rose-500 hover:bg-rose-50" @click="askAction('delete', user)"><i class="fas fa-trash-can" aria-hidden="true"></i></button>
                  </div></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="divide-y divide-gray-100 md:hidden">
            <article v-for="user in users" :key="user.id || user.username" class="p-4">
              <div class="flex items-start justify-between gap-3"><button type="button" class="min-w-0 text-left" @click="openDetail(user)"><h3 class="truncate text-sm font-semibold text-gray-900">{{ user.name || '未填写姓名' }}</h3><p class="mt-0.5 truncate text-xs text-gray-400">{{ user.username }}<template v-if="user.email"> · {{ user.email }}</template></p></button><span :class="user.jws ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'" class="shrink-0 rounded-full px-2 py-1 text-xs font-medium">{{ user.jws ? 'JWS 有效' : 'JWS 失效' }}</span></div>
              <div class="mt-3 flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2"><div class="text-xs text-gray-500"><span>{{ formatDays(user.signDays) }}</span><span class="mx-1 text-gray-300">·</span><span>{{ user.signStartTime || '--' }} - {{ user.signEndTime || '--' }}</span></div><button type="button" role="switch" :aria-checked="Boolean(user.autoSign)" :aria-label="`${user.username} 自动签到`" :disabled="Boolean(busyAction)" :class="user.autoSign ? 'bg-emerald-500' : 'bg-gray-300'" class="relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-50" @click="toggleAutoSign(user)"><span :class="user.autoSign ? 'translate-x-5' : 'translate-x-0.5'" class="absolute left-0 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform"></span></button></div>
              <div class="mt-3 grid grid-cols-5 gap-1">
                <button type="button" title="详情" class="rounded-lg py-2 text-gray-400 hover:bg-pink-50 hover:text-pink-600" @click="openDetail(user)"><i class="fas fa-eye" aria-hidden="true"></i><span class="sr-only">详情</span></button>
                <button type="button" title="签到" class="rounded-lg py-2 text-emerald-500 hover:bg-emerald-50" @click="askAction('sign', user)"><i class="fas fa-calendar-check" aria-hidden="true"></i><span class="sr-only">签到</span></button>
                <button type="button" title="刷新 JWS" class="rounded-lg py-2 text-amber-500 hover:bg-amber-50" @click="askAction('refresh', user)"><i class="fas fa-arrows-rotate" aria-hidden="true"></i><span class="sr-only">刷新 JWS</span></button>
                <button type="button" title="编辑" class="rounded-lg py-2 text-blue-500 hover:bg-blue-50" @click="openEdit(user)"><i class="fas fa-pen" aria-hidden="true"></i><span class="sr-only">编辑</span></button>
                <button type="button" title="删除" class="rounded-lg py-2 text-rose-500 hover:bg-rose-50" @click="askAction('delete', user)"><i class="fas fa-trash-can" aria-hidden="true"></i><span class="sr-only">删除</span></button>
              </div>
            </article>
          </div>

          <footer class="flex flex-col gap-3 border-t border-pink-100 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"><p class="text-center text-xs text-gray-400 sm:text-left">显示 {{ rangeStart }}-{{ rangeEnd }}，共 {{ total }} 个用户</p><div class="flex items-center justify-center gap-2"><button type="button" aria-label="上一页" title="上一页" :disabled="currentPage <= 1 || isLoading" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:border-pink-200 hover:bg-pink-50 disabled:cursor-not-allowed disabled:opacity-40" @click="changePage(currentPage - 1)"><i class="fas fa-chevron-left" aria-hidden="true"></i></button><span class="min-w-[92px] text-center text-xs font-medium text-gray-600">第 {{ currentPage }} / {{ totalPages }} 页</span><button type="button" aria-label="下一页" title="下一页" :disabled="currentPage >= totalPages || isLoading" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:border-pink-200 hover:bg-pink-50 disabled:cursor-not-allowed disabled:opacity-40" @click="changePage(currentPage + 1)"><i class="fas fa-chevron-right" aria-hidden="true"></i></button></div></footer>
        </template>
      </section>
    </main>

    <AdminUserFormModal :visible="showForm" :mode="formMode" :user="formUser" :loading="formLoading" @close="!formLoading && (showForm = false)" @submit="submitForm" />
    <AdminUserDetailModal :visible="detailVisible" :user="detailUser" :signs="detailSigns" :loading="detailLoading" @close="detailVisible = false" />
    <ConfirmActionModal :visible="Boolean(confirmAction)" :title="confirmAction?.title" :message="confirmAction?.message" :detail="confirmAction?.detail" :confirm-text="confirmAction?.confirmText" :icon="confirmAction?.icon" :danger="confirmAction?.danger" :loading="busyAction.startsWith('confirm-')" @close="!busyAction && (confirmAction = null)" @confirm="confirmCurrentAction" />
  </div>
</template>
