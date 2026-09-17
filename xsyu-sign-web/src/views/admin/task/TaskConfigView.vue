<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import BaseModal from '@/components/BaseModal.vue'
import EmailNotificationModal from '@/components/EmailNotificationModal.vue'

const taskConfigs = ref([])
const loading = ref(false)
const saving = ref(false)
const scheduling = ref(false)
const error = ref('')
const editingTask = ref('')
const showScheduleConfirm = ref(false)
const showEmailWorkspace = ref(false)
const formError = ref('')

const now = new Date()
const calendarYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth())
const scheduleDates = ref([])
const holidayDates = ref([])
const skippedDates = ref([])
const autoSelectedDates = ref([])
const calendarLoading = ref(false)

const form = reactive({
  enabled: true,
  scheduleHour: 18,
  scheduleMinute: 31,
  delayRange: 30,
  intervalStartHour: 18,
  intervalStartMinute: 0,
  intervalEndHour: 20,
  intervalEndMinute: 0,
  intervalMinutes: 1,
  jwsHour: 18,
  jwsMinute: 0,
  jwsStartDate: '',
  jwsIntervalWeeks: 1
})

const scheduleTask = computed(() => taskConfigs.value.find(task => task.taskKey === 'schedule_users'))
const intervalTask = computed(() => taskConfigs.value.find(task => task.taskKey === 'interval_sign'))
const jwsTask = computed(() => taskConfigs.value.find(task => task.taskKey === 'refresh_jws'))
const currentMonthLabel = computed(() => `${calendarYear.value} 年 ${currentMonth.value + 1} 月`)
const calendarYears = computed(() => Array.from({ length: 5 }, (_, index) => now.getFullYear() - 1 + index))
const calendarDays = computed(() => {
  const firstDay = new Date(calendarYear.value, currentMonth.value, 1)
  const daysInMonth = new Date(calendarYear.value, currentMonth.value + 1, 0).getDate()
  const days = Array.from({ length: firstDay.getDay() }, () => ({ current: false }))
  for (let day = 1; day <= daysInMonth; day++) {
    const date = `${calendarYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    days.push({
      current: true,
      day,
      date,
      selected: scheduleDates.value.includes(date),
      holiday: holidayDates.value.includes(date),
      skipped: skippedDates.value.includes(date),
      auto: autoSelectedDates.value.includes(date)
    })
  }
  return days
})

function requestMessage(requestError, fallback) {
  return requestError.response?.data?.message || requestError.message || fallback
}

function responseData(response, fallback) {
  if (response.data?.code !== 200) throw new Error(response.data?.message || fallback)
  return response.data.data
}

function numberValue(value, fallback) {
  const parsed = Number.parseInt(value, 10)
  return Number.isFinite(parsed) ? parsed : fallback
}

function formatTime(hour, minute) {
  if (hour === undefined || hour === null || minute === undefined || minute === null) return '--'
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function formatInterval(weeks) {
  if (!weeks) return '--'
  return weeks === 1 ? '每周' : `每 ${weeks} 周`
}

async function loadTaskConfigs() {
  loading.value = true
  error.value = ''
  try {
    taskConfigs.value = responseData(await api.getTaskConfigs(), '获取任务配置失败') || []
  } catch (requestError) {
    error.value = requestMessage(requestError, '获取任务配置失败')
    showMessage(error.value, 'error')
  } finally {
    loading.value = false
  }
}

async function loadCalendarMetadata(year) {
  calendarLoading.value = true
  try {
    const data = responseData(await api.getScheduleCalendar(year), '获取调度日历失败') || {}
    holidayDates.value = data.holidayDates || []
    skippedDates.value = data.skippedDates || []
    autoSelectedDates.value = data.autoSelectedDates || []
  } catch (requestError) {
    showMessage(requestMessage(requestError, '获取调度日历失败'), 'error')
  } finally {
    calendarLoading.value = false
  }
}

async function openEditor(taskKey) {
  const task = taskConfigs.value.find(item => item.taskKey === taskKey)
  const parsed = task?.parsedCron || {}
  editingTask.value = taskKey
  form.enabled = task?.enabled !== false
  formError.value = ''
  if (taskKey === 'schedule_users') {
    form.scheduleHour = numberValue(parsed.hour, 18)
    form.scheduleMinute = numberValue(parsed.minute, 31)
    form.delayRange = numberValue(task?.scheduleConfig?.delayRange, 30)
    calendarYear.value = numberValue(task?.scheduleConfig?.scheduleYear, now.getFullYear())
    currentMonth.value = now.getFullYear() === calendarYear.value ? now.getMonth() : 0
    scheduleDates.value = [...(task?.scheduleConfig?.scheduleDates || [])]
    await loadCalendarMetadata(calendarYear.value)
  } else if (taskKey === 'interval_sign') {
    form.intervalStartHour = numberValue(parsed.startHour, 18)
    form.intervalStartMinute = numberValue(parsed.startMinute, 0)
    form.intervalEndHour = numberValue(parsed.endHour, 20)
    form.intervalEndMinute = numberValue(parsed.endMinute, 0)
    form.intervalMinutes = numberValue(parsed.interval, 1)
  } else if (taskKey === 'refresh_jws') {
    form.jwsHour = numberValue(parsed.jwsHour, 18)
    form.jwsMinute = numberValue(parsed.jwsMinute, 0)
    form.jwsStartDate = parsed.jwsStartDate || ''
    form.jwsIntervalWeeks = numberValue(parsed.jwsIntervalWeeks, 1)
  }
}

function closeEditor() {
  if (saving.value) return
  editingTask.value = ''
  formError.value = ''
}

function toggleDate(day) {
  if (!day.current) return
  scheduleDates.value = day.selected
    ? scheduleDates.value.filter(date => date !== day.date)
    : [...scheduleDates.value, day.date].sort()
}

function applyAutoDates() {
  scheduleDates.value = [...autoSelectedDates.value]
}

async function changeCalendarYear() {
  scheduleDates.value = scheduleDates.value.filter(date => date.startsWith(`${calendarYear.value}-`))
  currentMonth.value = 0
  await loadCalendarMetadata(calendarYear.value)
}

async function previousMonth() {
  if (currentMonth.value > 0) return currentMonth.value--
  calendarYear.value--
  currentMonth.value = 11
  scheduleDates.value = []
  await loadCalendarMetadata(calendarYear.value)
}

async function nextMonth() {
  if (currentMonth.value < 11) return currentMonth.value++
  calendarYear.value++
  currentMonth.value = 0
  scheduleDates.value = []
  await loadCalendarMetadata(calendarYear.value)
}

function dayClass(day) {
  if (!day.current) return 'invisible'
  if (day.selected) return 'border-emerald-500 bg-emerald-500 text-white'
  if (day.skipped) return 'border-red-100 bg-red-50 text-red-500 hover:bg-red-100'
  if (day.holiday) return 'border-amber-100 bg-amber-50 text-amber-700 hover:bg-amber-100'
  return 'border-gray-100 bg-white text-gray-600 hover:border-pink-200 hover:bg-pink-50'
}

function validateRange(value, min, max, label) {
  if (!Number.isInteger(value) || value < min || value > max) {
    formError.value = `${label}必须在 ${min}-${max} 之间`
    return false
  }
  return true
}

async function saveConfig() {
  formError.value = ''
  const payload = { taskKey: editingTask.value, enabled: form.enabled }
  if (editingTask.value === 'schedule_users') {
    if (!validateRange(form.scheduleHour, 0, 23, '小时') || !validateRange(form.scheduleMinute, 0, 59, '分钟')) return
    if (!validateRange(form.delayRange, 1, 120, '延迟范围')) return
    payload.scheduleConfig = {
      scheduleDates: [...scheduleDates.value].sort(),
      scheduleYear: calendarYear.value,
      hour: form.scheduleHour,
      minute: form.scheduleMinute,
      delayRange: form.delayRange
    }
  } else if (editingTask.value === 'interval_sign') {
    if (!validateRange(form.intervalStartHour, 0, 23, '开始小时') || !validateRange(form.intervalStartMinute, 0, 59, '开始分钟')) return
    if (!validateRange(form.intervalEndHour, 0, 23, '结束小时') || !validateRange(form.intervalEndMinute, 0, 59, '结束分钟')) return
    if (!validateRange(form.intervalMinutes, 1, 120, '执行间隔')) return
    const start = form.intervalStartHour * 60 + form.intervalStartMinute
    const end = form.intervalEndHour * 60 + form.intervalEndMinute
    if (start >= end) return (formError.value = '开始时间必须早于结束时间')
    payload.intervalConfig = {
      startHour: form.intervalStartHour,
      startMinute: form.intervalStartMinute,
      endHour: form.intervalEndHour,
      endMinute: form.intervalEndMinute,
      intervalMinutes: form.intervalMinutes
    }
  } else if (editingTask.value === 'refresh_jws') {
    if (!form.jwsStartDate) return (formError.value = '请选择首次续签日期')
    if (!validateRange(form.jwsHour, 0, 23, '小时') || !validateRange(form.jwsMinute, 0, 59, '分钟')) return
    payload.jwsConfig = {
      startDate: form.jwsStartDate,
      intervalWeeks: form.jwsIntervalWeeks,
      hour: form.jwsHour,
      minute: form.jwsMinute
    }
  }

  saving.value = true
  try {
    responseData(await api.updateTaskConfig(editingTask.value, payload), '保存任务配置失败')
    showMessage('任务配置已保存')
    editingTask.value = ''
    await loadTaskConfigs()
  } catch (requestError) {
    showMessage(requestMessage(requestError, '保存任务配置失败'), 'error')
  } finally {
    saving.value = false
  }
}

async function runImmediateSchedule(sendEmail) {
  scheduling.value = true
  try {
    responseData(await api.triggerImmediateSchedule(sendEmail), '立即调度失败')
    showMessage(sendEmail ? '调度已执行，结果邮件将发送' : '调度已执行')
    showScheduleConfirm.value = false
  } catch (requestError) {
    showMessage(requestMessage(requestError, '立即调度失败'), 'error')
  } finally {
    scheduling.value = false
  }
}

onMounted(loadTaskConfigs)
</script>

<template>
  <div>
    <AdminPageHeader title="定时任务" subtitle="配置用户调度、签到检查、JWS 续签和邮件通知" icon="fas fa-clock" :loading="loading" @refresh="loadTaskConfigs" />

    <main class="mx-auto max-w-6xl px-4 py-5 sm:px-6 sm:py-6">
      <div v-if="loading && !taskConfigs.length" class="py-24 text-center text-pink-400"><i class="fas fa-spinner fa-spin mb-3 text-2xl" aria-hidden="true"></i><p class="text-sm">正在加载任务配置</p></div>
      <div v-else-if="error && !taskConfigs.length" class="rounded-lg border border-rose-200 bg-white py-16 text-center"><i class="fas fa-triangle-exclamation mb-3 text-2xl text-rose-400" aria-hidden="true"></i><p class="text-sm text-gray-600">{{ error }}</p></div>

      <section v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <article class="flex flex-col rounded-lg border border-pink-200 bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-3"><div class="flex min-w-0 items-start gap-3"><div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-pink-50 text-pink-500"><i class="fas fa-users" aria-hidden="true"></i></div><div><h2 class="font-bold text-gray-900">调度所有用户</h2><p class="mt-1 text-xs leading-5 text-gray-400">按年历创建延迟签到任务</p></div></div><span :class="scheduleTask?.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'" class="shrink-0 rounded-full px-2 py-1 text-xs font-medium">{{ scheduleTask?.enabled ? '已启用' : '已禁用' }}</span></div>
          <dl class="my-5 grid grid-cols-3 gap-2 rounded-lg bg-pink-50/60 p-3 text-center"><div><dt class="text-[11px] text-gray-400">执行时间</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ formatTime(scheduleTask?.parsedCron?.hour, scheduleTask?.parsedCron?.minute) }}</dd></div><div><dt class="text-[11px] text-gray-400">延迟范围</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ scheduleTask?.scheduleConfig?.delayRange || '--' }} 分钟</dd></div><div><dt class="text-[11px] text-gray-400">调度日期</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ scheduleTask?.scheduleConfig?.scheduleDates?.length || 0 }} 天</dd></div></dl>
          <div class="mt-auto flex gap-2"><button type="button" class="flex-1 rounded-lg border border-pink-200 py-2 text-sm font-medium text-pink-600 hover:bg-pink-50" @click="openEditor('schedule_users')"><i class="fas fa-pen mr-1" aria-hidden="true"></i>修改配置</button><button type="button" class="flex-1 rounded-lg bg-rose-500 py-2 text-sm font-medium text-white hover:bg-rose-600" @click="showScheduleConfirm = true"><i class="fas fa-play mr-1" aria-hidden="true"></i>立即调度</button></div>
        </article>

        <article class="flex flex-col rounded-lg border border-emerald-200 bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-3"><div class="flex min-w-0 items-start gap-3"><div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-emerald-500"><i class="fas fa-arrows-rotate" aria-hidden="true"></i></div><div><h2 class="font-bold text-gray-900">间隔执行签到</h2><p class="mt-1 text-xs leading-5 text-gray-400">在时间窗口内重复检查签到</p></div></div><span :class="intervalTask?.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'" class="shrink-0 rounded-full px-2 py-1 text-xs font-medium">{{ intervalTask?.enabled ? '已启用' : '已禁用' }}</span></div>
          <dl class="my-5 grid grid-cols-3 gap-2 rounded-lg bg-emerald-50/60 p-3 text-center"><div><dt class="text-[11px] text-gray-400">开始</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ formatTime(intervalTask?.parsedCron?.startHour, intervalTask?.parsedCron?.startMinute) }}</dd></div><div><dt class="text-[11px] text-gray-400">结束</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ formatTime(intervalTask?.parsedCron?.endHour, intervalTask?.parsedCron?.endMinute) }}</dd></div><div><dt class="text-[11px] text-gray-400">间隔</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ intervalTask?.parsedCron?.interval || '--' }} 分钟</dd></div></dl>
          <button type="button" class="mt-auto w-full rounded-lg border border-emerald-200 py-2 text-sm font-medium text-emerald-700 hover:bg-emerald-50" @click="openEditor('interval_sign')"><i class="fas fa-pen mr-1" aria-hidden="true"></i>修改配置</button>
        </article>

        <article class="flex flex-col rounded-lg border border-amber-200 bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-3"><div class="flex min-w-0 items-start gap-3"><div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-amber-50 text-amber-500"><i class="fas fa-key" aria-hidden="true"></i></div><div><h2 class="font-bold text-gray-900">JWS 续签</h2><p class="mt-1 text-xs leading-5 text-gray-400">周期刷新用户登录状态</p></div></div><span :class="jwsTask?.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'" class="shrink-0 rounded-full px-2 py-1 text-xs font-medium">{{ jwsTask?.enabled ? '已启用' : '已禁用' }}</span></div>
          <dl class="my-5 grid grid-cols-3 gap-2 rounded-lg bg-amber-50/60 p-3 text-center"><div><dt class="text-[11px] text-gray-400">执行时间</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ formatTime(jwsTask?.parsedCron?.jwsHour, jwsTask?.parsedCron?.jwsMinute) }}</dd></div><div><dt class="text-[11px] text-gray-400">间隔</dt><dd class="mt-1 text-sm font-semibold text-gray-800">{{ formatInterval(jwsTask?.parsedCron?.jwsIntervalWeeks) }}</dd></div><div><dt class="text-[11px] text-gray-400">下次续签</dt><dd class="mt-1 truncate text-sm font-semibold text-gray-800">{{ jwsTask?.parsedCron?.jwsNextRefresh || '--' }}</dd></div></dl>
          <button type="button" class="mt-auto w-full rounded-lg border border-amber-200 py-2 text-sm font-medium text-amber-700 hover:bg-amber-50" @click="openEditor('refresh_jws')"><i class="fas fa-pen mr-1" aria-hidden="true"></i>修改配置</button>
        </article>

        <article class="flex flex-col rounded-lg border border-sky-200 bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-3"><div class="flex min-w-0 items-start gap-3"><div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-sky-50 text-sky-600"><i class="fas fa-envelope" aria-hidden="true"></i></div><div><h2 class="font-bold text-gray-900">邮件通知</h2><p class="mt-1 text-xs leading-5 text-gray-400">向用户或用户组发送通知</p></div></div><span class="shrink-0 rounded-full bg-sky-50 px-2 py-1 text-xs font-medium text-sky-700">手动任务</span></div>
          <dl class="my-5 grid grid-cols-3 gap-2 rounded-lg bg-sky-50/70 p-3 text-center"><div><dt class="text-[11px] text-gray-400">收件人</dt><dd class="mt-1 text-sm font-semibold text-gray-800">用户 / 组</dd></div><div><dt class="text-[11px] text-gray-400">内容</dt><dd class="mt-1 text-sm font-semibold text-gray-800">模板</dd></div><div><dt class="text-[11px] text-gray-400">发送</dt><dd class="mt-1 text-sm font-semibold text-gray-800">立即 / 定时</dd></div></dl>
          <button type="button" class="mt-auto w-full rounded-lg bg-sky-600 py-2 text-sm font-medium text-white hover:bg-sky-700" @click="showEmailWorkspace = true"><i class="fas fa-arrow-up-right-from-square mr-1" aria-hidden="true"></i>打开邮件通知</button>
        </article>
      </section>
    </main>

    <BaseModal :visible="Boolean(editingTask)" :title="editingTask === 'schedule_users' ? '调度日历配置' : editingTask === 'interval_sign' ? '间隔签到配置' : 'JWS 续签配置'" :subtitle="editingTask === 'schedule_users' ? '选择全年需要执行调度的日期' : '修改后立即更新定时任务'" icon="fas fa-clock" :width="editingTask === 'schedule_users' ? 'max-w-3xl' : 'max-w-lg'" scrollable :closable="!saving" @close="closeEditor">
      <form id="task-config-form" class="space-y-5" @submit.prevent="saveConfig">
        <label class="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 px-3 py-3"><span><span class="block text-sm font-medium text-gray-800">启用任务</span><span class="block text-xs text-gray-400">关闭后调度器不会自动执行</span></span><input v-model="form.enabled" type="checkbox" class="h-5 w-5 rounded border-gray-300 text-rose-500 focus:ring-pink-300"></label>

        <template v-if="editingTask === 'schedule_users'">
          <div class="grid grid-cols-3 gap-3"><label class="text-xs font-medium text-gray-600">执行小时<input v-model.number="form.scheduleHour" type="number" min="0" max="23" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label><label class="text-xs font-medium text-gray-600">执行分钟<input v-model.number="form.scheduleMinute" type="number" min="0" max="59" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label><label class="text-xs font-medium text-gray-600">延迟范围<input v-model.number="form.delayRange" type="number" min="1" max="120" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label></div>
          <section class="rounded-lg border border-pink-100 bg-pink-50/40 p-3 sm:p-4">
            <div class="flex flex-wrap items-center justify-between gap-2"><div class="flex items-center gap-2"><select v-model.number="calendarYear" class="h-8 rounded-lg border border-pink-200 bg-white px-2 text-sm outline-none" @change="changeCalendarYear"><option v-for="year in calendarYears" :key="year" :value="year">{{ year }} 年</option></select><button type="button" :disabled="calendarLoading" class="h-8 rounded-lg border border-pink-200 bg-white px-3 text-xs font-medium text-pink-600 hover:bg-pink-50 disabled:opacity-50" @click="applyAutoDates">按默认规则填充</button></div><span class="text-xs text-gray-500">已选 {{ scheduleDates.length }} 天</span></div>
            <div class="mt-4 flex items-center justify-between"><button type="button" aria-label="上个月" class="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-pink-600" @click="previousMonth"><i class="fas fa-chevron-left" aria-hidden="true"></i></button><p class="text-sm font-bold text-gray-800">{{ currentMonthLabel }}</p><button type="button" aria-label="下个月" class="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-pink-600" @click="nextMonth"><i class="fas fa-chevron-right" aria-hidden="true"></i></button></div>
            <div class="mt-3 grid grid-cols-7 gap-1 text-center text-[11px] text-gray-400"><span v-for="label in ['日','一','二','三','四','五','六']" :key="label" class="py-1">{{ label }}</span></div>
            <div class="grid grid-cols-7 gap-1"><button v-for="(day, index) in calendarDays" :key="day.date || `blank-${index}`" type="button" :disabled="!day.current" :title="day.date" :class="dayClass(day)" class="aspect-square rounded-lg border text-xs transition-colors" @click="toggleDate(day)">{{ day.day }}</button></div>
            <div class="mt-3 flex flex-wrap gap-3 text-[11px] text-gray-500"><span><i class="mr-1 inline-block h-2.5 w-2.5 rounded-sm bg-emerald-500"></i>已选择</span><span><i class="mr-1 inline-block h-2.5 w-2.5 rounded-sm bg-red-100"></i>跳过日期</span><span><i class="mr-1 inline-block h-2.5 w-2.5 rounded-sm bg-amber-100"></i>节假日</span></div>
          </section>
        </template>

        <template v-else-if="editingTask === 'interval_sign'">
          <div><p class="mb-2 text-sm font-medium text-gray-700">开始时间</p><div class="grid grid-cols-2 gap-3"><label class="text-xs text-gray-500">小时<input v-model.number="form.intervalStartHour" type="number" min="0" max="23" class="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label><label class="text-xs text-gray-500">分钟<input v-model.number="form.intervalStartMinute" type="number" min="0" max="59" class="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label></div></div>
          <div><p class="mb-2 text-sm font-medium text-gray-700">结束时间</p><div class="grid grid-cols-2 gap-3"><label class="text-xs text-gray-500">小时<input v-model.number="form.intervalEndHour" type="number" min="0" max="23" class="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label><label class="text-xs text-gray-500">分钟<input v-model.number="form.intervalEndMinute" type="number" min="0" max="59" class="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label></div></div>
          <label class="block text-sm font-medium text-gray-700">执行间隔（分钟）<input v-model.number="form.intervalMinutes" type="number" min="1" max="120" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label>
        </template>

        <template v-else-if="editingTask === 'refresh_jws'">
          <label class="block text-sm font-medium text-gray-700">首次续签日期<input v-model="form.jwsStartDate" type="date" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label>
          <label class="block text-sm font-medium text-gray-700">续签间隔<select v-model.number="form.jwsIntervalWeeks" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"><option v-for="weeks in [1,2,3,4]" :key="weeks" :value="weeks">{{ formatInterval(weeks) }}</option></select></label>
          <div><p class="mb-2 text-sm font-medium text-gray-700">执行时间</p><div class="grid grid-cols-2 gap-3"><label class="text-xs text-gray-500">小时<input v-model.number="form.jwsHour" type="number" min="0" max="23" class="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label><label class="text-xs text-gray-500">分钟<input v-model.number="form.jwsMinute" type="number" min="0" max="59" class="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label></div></div>
        </template>

        <p v-if="formError" class="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">{{ formError }}</p>
      </form>
      <template #footer><div class="flex justify-end gap-3"><button type="button" :disabled="saving" class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50" @click="closeEditor">取消</button><button type="submit" form="task-config-form" :disabled="saving" class="min-w-[96px] rounded-lg bg-rose-500 px-4 py-2 text-sm font-medium text-white hover:bg-rose-600 disabled:opacity-50"><i v-if="saving" class="fas fa-spinner fa-spin mr-1" aria-hidden="true"></i>{{ saving ? '保存中' : '保存配置' }}</button></div></template>
    </BaseModal>

    <BaseModal :visible="showScheduleConfirm" title="立即调度所有用户" subtitle="选择是否在执行完成后发送结果邮件" icon="fas fa-play" width="max-w-md" :closable="!scheduling" @close="!scheduling && (showScheduleConfirm = false)">
      <p class="text-sm leading-6 text-gray-600">该操作会立即创建本次用户签到调度任务，不受日历日期限制。</p>
      <template #footer><div class="grid grid-cols-1 gap-2 sm:grid-cols-3"><button type="button" :disabled="scheduling" class="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50" @click="showScheduleConfirm = false">取消</button><button type="button" :disabled="scheduling" class="rounded-lg border border-pink-200 px-3 py-2 text-sm font-medium text-pink-600 hover:bg-pink-50 disabled:opacity-50" @click="runImmediateSchedule(false)">仅调度</button><button type="button" :disabled="scheduling" class="rounded-lg bg-rose-500 px-3 py-2 text-sm font-medium text-white hover:bg-rose-600 disabled:opacity-50" @click="runImmediateSchedule(true)"><i v-if="scheduling" class="fas fa-spinner fa-spin mr-1" aria-hidden="true"></i>调度并邮件</button></div></template>
    </BaseModal>

    <EmailNotificationModal :visible="showEmailWorkspace" @close="showEmailWorkspace = false" />
  </div>
</template>
