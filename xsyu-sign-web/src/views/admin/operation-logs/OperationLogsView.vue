<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import BaseModal from '@/components/BaseModal.vue'
import ConfirmActionModal from '@/components/ConfirmActionModal.vue'

const logs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const startDate = ref('')
const endDate = ref('')
const logType = ref('')
const selectedIds = ref([])
const detailLog = ref(null)
const deleteIds = ref([])
const loading = ref(false)
const deleting = ref(false)
const error = ref('')

const logTypes = [
  { value: '', label: '全部类型' },
  { value: 'API', label: 'API 接口' },
  { value: 'SCHEDULE', label: '定时调度' },
  { value: 'INTERVAL_SIGN', label: '间隔签到' },
  { value: 'JWS_REFRESH', label: 'JWS 续签' }
]
const typeLabels = { API: 'API', SCHEDULE: '调度', INTERVAL_SIGN: '间隔签到', JWS_REFRESH: 'JWS 续签' }
const typeClasses = { API: 'bg-blue-50 text-blue-700', SCHEDULE: 'bg-purple-50 text-purple-700', INTERVAL_SIGN: 'bg-emerald-50 text-emerald-700', JWS_REFRESH: 'bg-amber-50 text-amber-700' }
const resultLabels = { SUCCESS: '成功', FAIL: '失败', PARTIAL: '部分成功' }
const resultClasses = { SUCCESS: 'bg-emerald-50 text-emerald-700', FAIL: 'bg-red-50 text-red-700', PARTIAL: 'bg-amber-50 text-amber-700' }

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const allSelected = computed(() => logs.value.length > 0 && logs.value.every(log => selectedIds.value.includes(log.id)))

function today() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function requestMessage(requestError, fallback) {
  return requestError.response?.data?.message || requestError.message || fallback
}

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    if (logType.value) params.logType = logType.value
    const response = await api.getOperationLogs(params)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '获取日志失败')
    logs.value = response.data.data?.records || []
    total.value = Number(response.data.data?.total || 0)
    selectedIds.value = []
  } catch (requestError) {
    error.value = requestMessage(requestError, '获取日志失败')
    showMessage(error.value, 'error')
  } finally {
    loading.value = false
  }
}

function search() {
  if (startDate.value && endDate.value && startDate.value > endDate.value) return showMessage('开始日期不能晚于结束日期', 'error')
  currentPage.value = 1
  loadLogs()
}

function changePage(page) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  loadLogs()
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : logs.value.map(log => log.id)
}

function toggleOne(id) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter(item => item !== id)
    : [...selectedIds.value, id]
}

function askDelete(ids) {
  if (!ids.length) return
  deleteIds.value = [...ids]
}

async function confirmDelete() {
  deleting.value = true
  try {
    const response = await api.deleteOperationLogs(deleteIds.value)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '删除日志失败')
    showMessage(`已删除 ${deleteIds.value.length} 条日志`)
    if (detailLog.value && deleteIds.value.includes(detailLog.value.id)) detailLog.value = null
    deleteIds.value = []
    await loadLogs()
  } catch (requestError) {
    showMessage(requestMessage(requestError, '删除日志失败'), 'error')
  } finally {
    deleting.value = false
  }
}

function formatTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ')
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  startDate.value = today()
  endDate.value = today()
  loadLogs()
})
</script>

<template>
  <div>
    <AdminPageHeader title="操作日志" subtitle="查询后台操作和定时任务执行记录" icon="fas fa-list-check" :loading="loading" @refresh="loadLogs" />

    <main class="mx-auto max-w-7xl space-y-4 px-4 py-5 sm:px-6 sm:py-6">
      <section class="rounded-lg border border-pink-200 bg-white p-4 shadow-sm">
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_1fr_auto] lg:items-end">
          <label class="text-xs font-medium text-gray-600">开始日期<input v-model="startDate" type="date" class="mt-1.5 h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label>
          <label class="text-xs font-medium text-gray-600">结束日期<input v-model="endDate" type="date" class="mt-1.5 h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label>
          <label class="text-xs font-medium text-gray-600">日志类型<select v-model="logType" class="mt-1.5 h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"><option v-for="item in logTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
          <div class="flex gap-2">
            <button type="button" class="h-10 flex-1 rounded-lg bg-rose-500 px-4 text-sm font-medium text-white hover:bg-rose-600 lg:flex-none" @click="search"><i class="fas fa-magnifying-glass mr-1" aria-hidden="true"></i>查询</button>
            <button type="button" title="删除选中日志" :disabled="!selectedIds.length" class="h-10 rounded-lg border border-red-200 px-3 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40" @click="askDelete(selectedIds)"><i class="fas fa-trash-can" aria-hidden="true"></i><span v-if="selectedIds.length" class="ml-1">{{ selectedIds.length }}</span></button>
          </div>
        </div>
      </section>

      <section class="overflow-hidden rounded-lg border border-pink-200 bg-white shadow-sm">
        <div v-if="loading && !logs.length" class="py-20 text-center text-pink-400"><i class="fas fa-spinner fa-spin mb-3 text-2xl" aria-hidden="true"></i><p class="text-sm">正在加载日志</p></div>
        <div v-else-if="error && !logs.length" class="py-16 text-center"><i class="fas fa-triangle-exclamation mb-3 text-2xl text-rose-400" aria-hidden="true"></i><p class="text-sm text-gray-600">{{ error }}</p></div>
        <div v-else-if="!logs.length" class="py-20 text-center text-gray-400"><i class="fas fa-inbox mb-3 block text-3xl text-pink-200" aria-hidden="true"></i><p class="text-sm">当前条件下没有日志</p></div>

        <template v-else>
          <div class="hidden overflow-x-auto md:block">
            <table class="w-full min-w-[900px] text-left">
              <thead class="border-b border-pink-100 bg-pink-50/60 text-xs font-semibold text-gray-500"><tr><th class="w-12 px-4 py-3"><input type="checkbox" :checked="allSelected" aria-label="选择当前页全部日志" class="rounded border-gray-300 text-rose-500 focus:ring-pink-300" @change="toggleAll"></th><th class="px-4 py-3">类型</th><th class="px-4 py-3">操作</th><th class="px-4 py-3">结果</th><th class="px-4 py-3">操作人</th><th class="px-4 py-3">耗时</th><th class="px-4 py-3">时间</th><th class="w-12 px-4 py-3"></th></tr></thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="log in logs" :key="log.id" class="cursor-pointer hover:bg-pink-50/40" @click="detailLog = log">
                  <td class="px-4 py-3" @click.stop><input type="checkbox" :checked="selectedIds.includes(log.id)" :aria-label="`选择日志 ${log.id}`" class="rounded border-gray-300 text-rose-500 focus:ring-pink-300" @change="toggleOne(log.id)"></td>
                  <td class="px-4 py-3"><span :class="typeClasses[log.logType] || 'bg-gray-100 text-gray-600'" class="rounded-full px-2 py-1 text-xs font-medium">{{ typeLabels[log.logType] || log.logType }}</span></td>
                  <td class="max-w-[320px] truncate px-4 py-3 text-sm font-medium text-gray-800">{{ log.operation }}</td>
                  <td class="px-4 py-3"><span :class="resultClasses[log.result] || 'bg-gray-100 text-gray-600'" class="rounded-full px-2 py-1 text-xs font-medium">{{ resultLabels[log.result] || log.result }}</span></td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ log.operator === 'SYSTEM' ? '系统' : log.operator }}</td>
                  <td class="px-4 py-3 text-xs text-gray-500">{{ log.duration != null ? `${log.duration} ms` : '--' }}</td>
                  <td class="px-4 py-3 text-xs text-gray-500">{{ formatTime(log.createdAt) }}</td>
                  <td class="px-4 py-3 text-gray-300"><i class="fas fa-chevron-right" aria-hidden="true"></i></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="divide-y divide-gray-100 md:hidden">
            <article v-for="log in logs" :key="log.id" class="flex cursor-pointer items-center gap-3 p-4 active:bg-pink-50" @click="detailLog = log">
              <input type="checkbox" :checked="selectedIds.includes(log.id)" :aria-label="`选择日志 ${log.id}`" class="shrink-0 rounded border-gray-300 text-rose-500 focus:ring-pink-300" @click.stop @change="toggleOne(log.id)">
              <div class="min-w-0 flex-1"><div class="flex items-center gap-2"><span :class="typeClasses[log.logType] || 'bg-gray-100 text-gray-600'" class="shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium">{{ typeLabels[log.logType] || log.logType }}</span><p class="truncate text-sm font-medium text-gray-800">{{ log.operation }}</p></div><p class="mt-1 truncate text-xs text-gray-400">{{ formatTime(log.createdAt) }} · {{ log.operator === 'SYSTEM' ? '系统' : log.operator }}</p></div>
              <span :class="resultClasses[log.result] || 'bg-gray-100 text-gray-600'" class="shrink-0 rounded-full px-2 py-1 text-xs font-medium">{{ resultLabels[log.result] || log.result }}</span>
            </article>
          </div>

          <footer class="flex items-center justify-between border-t border-pink-100 px-4 py-3"><span class="text-xs text-gray-400">共 {{ total }} 条</span><div class="flex items-center gap-2"><button type="button" aria-label="上一页" :disabled="currentPage <= 1 || loading" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 disabled:opacity-40" @click="changePage(currentPage - 1)"><i class="fas fa-chevron-left" aria-hidden="true"></i></button><span class="min-w-[74px] text-center text-xs text-gray-600">{{ currentPage }} / {{ totalPages }}</span><button type="button" aria-label="下一页" :disabled="currentPage >= totalPages || loading" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 disabled:opacity-40" @click="changePage(currentPage + 1)"><i class="fas fa-chevron-right" aria-hidden="true"></i></button></div></footer>
        </template>
      </section>
    </main>

    <BaseModal :visible="Boolean(detailLog)" title="日志详情" :subtitle="detailLog ? formatTime(detailLog.createdAt) : ''" icon="fas fa-file-lines" width="max-w-xl" scrollable @close="detailLog = null">
      <template v-if="detailLog"><div class="mb-4 flex flex-wrap gap-2"><span :class="typeClasses[detailLog.logType] || 'bg-gray-100 text-gray-600'" class="rounded-full px-2 py-1 text-xs font-medium">{{ typeLabels[detailLog.logType] || detailLog.logType }}</span><span :class="resultClasses[detailLog.result] || 'bg-gray-100 text-gray-600'" class="rounded-full px-2 py-1 text-xs font-medium">{{ resultLabels[detailLog.result] || detailLog.result }}</span></div><h3 class="font-bold text-gray-900">{{ detailLog.operation }}</h3><dl class="mt-4 grid grid-cols-1 gap-x-6 sm:grid-cols-2"><div v-for="item in [['操作人', detailLog.operator === 'SYSTEM' ? '系统' : detailLog.operator], ['IP 地址', detailLog.ip || '--'], ['执行耗时', detailLog.duration != null ? `${detailLog.duration} ms` : '--'], ['记录时间', formatTime(detailLog.createdAt)]]" :key="item[0]" class="flex justify-between gap-4 border-b border-gray-100 py-2 text-sm"><dt class="text-gray-400">{{ item[0] }}</dt><dd class="break-all text-right font-medium text-gray-700">{{ item[1] }}</dd></div></dl><div class="mt-5"><p class="mb-2 text-sm font-medium text-gray-700">详细信息</p><pre class="whitespace-pre-wrap break-words rounded-lg border border-gray-200 bg-gray-50 p-3 font-sans text-sm leading-6 text-gray-700">{{ detailLog.detail || '无详情' }}</pre></div></template>
      <template #footer><div class="flex justify-between gap-3"><button type="button" class="rounded-lg border border-red-200 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50" @click="askDelete([detailLog.id])">删除此条</button><button type="button" class="rounded-lg bg-rose-500 px-5 py-2 text-sm font-medium text-white hover:bg-rose-600" @click="detailLog = null">关闭</button></div></template>
    </BaseModal>

    <ConfirmActionModal :visible="deleteIds.length > 0" title="删除操作日志" message="选中的操作日志将被永久删除，此操作无法撤销。" :detail="`共 ${deleteIds.length} 条日志`" confirm-text="确认删除" icon="fas fa-trash-can" danger :loading="deleting" @close="!deleting && (deleteIds = [])" @confirm="confirmDelete" />
  </div>
</template>
