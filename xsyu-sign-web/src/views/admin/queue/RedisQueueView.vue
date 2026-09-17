<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import ConfirmActionModal from '@/components/ConfirmActionModal.vue'

const queueInfo = ref(null)
const loading = ref(false)
const clearing = ref(false)
const error = ref('')
const showClearConfirm = ref(false)
const lastUpdatedAt = ref(null)
let refreshTimer = null

const tasks = computed(() => queueInfo.value?.tasks || [])

function getMessage(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

async function loadQueueInfo(silent = false) {
  if (!silent) loading.value = true
  error.value = ''
  try {
    const response = await api.getRedisQueueInfo()
    if (response.data?.code !== 200) throw new Error(response.data?.message || '获取队列信息失败')
    queueInfo.value = response.data.data || { queueName: '', queueSize: 0, tasks: [] }
    lastUpdatedAt.value = new Date()
  } catch (requestError) {
    error.value = getMessage(requestError, '获取队列信息失败')
    if (!silent) showMessage(error.value, 'error')
  } finally {
    loading.value = false
  }
}

async function clearQueue() {
  clearing.value = true
  try {
    const response = await api.clearRedisQueue()
    if (response.data?.code !== 200) throw new Error(response.data?.message || '清空队列失败')
    showMessage('队列已清空')
    showClearConfirm.value = false
    await loadQueueInfo(true)
  } catch (requestError) {
    showMessage(getMessage(requestError, '清空队列失败'), 'error')
  } finally {
    clearing.value = false
  }
}

function formatUpdatedAt() {
  if (!lastUpdatedAt.value) return '--'
  return lastUpdatedAt.value.toLocaleTimeString('zh-CN', { hour12: false })
}

onMounted(() => {
  loadQueueInfo()
  refreshTimer = setInterval(() => {
    if (!document.hidden) loadQueueInfo(true)
  }, 5000)
})

onBeforeUnmount(() => clearInterval(refreshTimer))
</script>

<template>
  <div>
    <AdminPageHeader title="Redis 队列" subtitle="等待签到任务每 5 秒自动刷新" icon="fas fa-layer-group" :loading="loading" @refresh="loadQueueInfo()">
      <template #actions>
        <button
          type="button"
          :disabled="loading || clearing || !queueInfo?.queueSize"
          class="h-9 rounded-lg border border-red-200 px-3 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
          @click="showClearConfirm = true"
        >
          <i class="fas fa-trash-can sm:mr-1" aria-hidden="true"></i><span class="hidden sm:inline">清空队列</span>
        </button>
      </template>
    </AdminPageHeader>

    <main class="mx-auto max-w-7xl space-y-4 px-4 py-5 sm:px-6 sm:py-6">
      <section class="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div class="rounded-lg border border-pink-200 bg-white p-4 shadow-sm">
          <p class="text-xs font-medium text-gray-400">队列名称</p>
          <p class="mt-2 truncate text-lg font-bold text-gray-900">{{ queueInfo?.queueName || '--' }}</p>
        </div>
        <div class="rounded-lg border border-rose-200 bg-white p-4 shadow-sm">
          <p class="text-xs font-medium text-gray-400">等待任务</p>
          <p class="mt-1 text-3xl font-bold text-rose-500">{{ queueInfo?.queueSize || 0 }}</p>
        </div>
        <div class="rounded-lg border border-emerald-200 bg-white p-4 shadow-sm">
          <p class="text-xs font-medium text-gray-400">同步状态</p>
          <p class="mt-2 text-sm font-semibold text-emerald-600"><i class="fas fa-circle text-[8px] mr-2" aria-hidden="true"></i>自动刷新中</p>
          <p class="mt-1 text-xs text-gray-400">上次更新 {{ formatUpdatedAt() }}</p>
        </div>
      </section>

      <section class="overflow-hidden rounded-lg border border-pink-200 bg-white shadow-sm">
        <div class="flex items-center justify-between border-b border-pink-100 px-4 py-3">
          <h2 class="text-sm font-bold text-gray-900">等待签到任务</h2>
          <span class="text-xs text-gray-400">按执行时间排序</span>
        </div>

        <div v-if="loading && !queueInfo" class="py-20 text-center text-pink-400">
          <i class="fas fa-spinner fa-spin mb-3 text-2xl" aria-hidden="true"></i><p class="text-sm">正在读取队列</p>
        </div>
        <div v-else-if="error && !queueInfo" class="py-16 text-center">
          <i class="fas fa-triangle-exclamation mb-3 text-2xl text-rose-400" aria-hidden="true"></i><p class="text-sm text-gray-600">{{ error }}</p>
        </div>
        <div v-else-if="!tasks.length" class="py-20 text-center text-gray-400">
          <i class="fas fa-inbox mb-3 block text-3xl text-pink-200" aria-hidden="true"></i><p class="text-sm">队列为空</p>
        </div>

        <template v-else>
          <div class="hidden overflow-x-auto sm:block">
            <table class="w-full text-left">
              <thead class="bg-pink-50/60 text-xs font-semibold text-gray-500"><tr><th class="px-4 py-3">序号</th><th class="px-4 py-3">学号</th><th class="px-4 py-3">执行时间</th><th class="px-4 py-3">等待时长</th></tr></thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="(task, index) in tasks" :key="`${task.username}-${task.executeTime}`" class="hover:bg-pink-50/40">
                  <td class="px-4 py-3 text-xs text-gray-400">{{ index + 1 }}</td>
                  <td class="px-4 py-3 text-sm font-semibold text-gray-900">{{ task.username }}</td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ task.executeTimeFormatted || '--' }}</td>
                  <td class="px-4 py-3"><span :class="task.waitSeconds < 60 ? 'bg-rose-50 text-rose-600' : 'bg-amber-50 text-amber-700'" class="rounded-full px-2 py-1 text-xs font-medium">{{ task.waitTimeFormatted || `${task.waitSeconds || 0} 秒` }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="divide-y divide-gray-100 sm:hidden">
            <article v-for="task in tasks" :key="`${task.username}-${task.executeTime}`" class="p-4">
              <div class="flex items-center justify-between gap-3"><p class="font-semibold text-gray-900">{{ task.username }}</p><span class="rounded-full bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700">{{ task.waitTimeFormatted }}</span></div>
              <p class="mt-2 text-xs text-gray-400">执行时间 {{ task.executeTimeFormatted || '--' }}</p>
            </article>
          </div>
        </template>
      </section>
    </main>

    <ConfirmActionModal :visible="showClearConfirm" title="清空 Redis 队列" message="队列中的等待任务将全部移除，此操作无法撤销。" :detail="`当前共 ${queueInfo?.queueSize || 0} 个任务`" confirm-text="确认清空" icon="fas fa-trash-can" danger :loading="clearing" @close="!clearing && (showClearConfirm = false)" @confirm="clearQueue" />
  </div>
</template>
