<script setup>
import { reactive, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'

const props = defineProps({
  visible: Boolean,
  mode: { type: String, default: 'edit' },
  user: { type: Object, default: null },
  loading: Boolean
})

const emit = defineEmits(['close', 'submit'])

const dayOptions = [
  { value: 0, label: '周日' },
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' }
]

const presets = [
  { label: '每天', value: [0, 1, 2, 3, 4, 5, 6] },
  { label: '在校时间', value: [0, 1, 2, 3, 4] },
  { label: '仅周日', value: [0] }
]

const form = reactive({
  username: '',
  password: '',
  name: '',
  email: '',
  autoSign: false,
  signStartTime: '19:00',
  signEndTime: '22:00'
})
const selectedDays = ref([])
const formError = ref('')

watch(
  () => [props.visible, props.mode, props.user],
  () => {
    if (!props.visible) return
    const user = props.user || {}
    form.username = props.mode === 'add' ? '' : (user.username || '')
    form.password = ''
    form.name = user.name || ''
    form.email = user.email || ''
    form.autoSign = Boolean(user.autoSign)
    form.signStartTime = user.signStartTime || '19:00'
    form.signEndTime = user.signEndTime || '22:00'
    selectedDays.value = (user.signDays || '0,1,2,3,4,5,6')
      .split(',')
      .map(Number)
      .filter(day => Number.isInteger(day) && day >= 0 && day <= 6)
    formError.value = ''
  },
  { immediate: true }
)

function toggleDay(day) {
  selectedDays.value = selectedDays.value.includes(day)
    ? selectedDays.value.filter(item => item !== day)
    : [...selectedDays.value, day].sort((a, b) => a - b)
}

function applyPreset(days) {
  selectedDays.value = [...days]
}

function isPresetActive(days) {
  return days.length === selectedDays.value.length && days.every(day => selectedDays.value.includes(day))
}

function submit() {
  formError.value = ''
  if (props.mode === 'add' && !form.username.trim()) return (formError.value = '请输入学号')
  if (props.mode === 'add' && !form.password) return (formError.value = '请输入学校密码')
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return (formError.value = '邮箱格式不正确')
  if (!selectedDays.value.length) return (formError.value = '请至少选择一个签到日')
  if (props.mode === 'edit') {
    if (form.signStartTime < '18:30') return (formError.value = '签到开始时间不能早于 18:30')
    if (form.signEndTime > '23:59') return (formError.value = '签到结束时间不能晚于 23:59')
    if (form.signStartTime >= form.signEndTime) return (formError.value = '签到开始时间必须早于结束时间')
  }

  const data = {
    name: form.name.trim(),
    email: form.email.trim(),
    signDays: selectedDays.value.join(',')
  }
  if (props.mode === 'add') {
    Object.assign(data, {
      username: form.username.trim(),
      password: form.password,
      autoSign: form.autoSign
    })
  } else {
    Object.assign(data, {
      signStartTime: form.signStartTime,
      signEndTime: form.signEndTime
    })
  }
  emit('submit', data)
}
</script>

<template>
  <BaseModal
    :visible="visible"
    :title="mode === 'add' ? '新增用户' : '编辑用户'"
    :subtitle="mode === 'add' ? '使用学校账号完成首次登录并创建用户' : user?.username"
    :icon="mode === 'add' ? 'fas fa-user-plus' : 'fas fa-user-pen'"
    width="max-w-lg"
    scrollable
    :closable="!loading"
    @close="emit('close')"
  >
    <form id="admin-user-form" class="space-y-4" @submit.prevent="submit">
      <div v-if="mode === 'add'" class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label class="block text-sm font-medium text-gray-700">
          学号 <span class="text-red-500">*</span>
          <input
            v-model="form.username"
            type="text"
            autocomplete="username"
            placeholder="请输入学号"
            class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none transition focus:border-pink-400 focus:ring-2 focus:ring-pink-100"
          >
        </label>
        <label class="block text-sm font-medium text-gray-700">
          学校密码 <span class="text-red-500">*</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="统一认证密码"
            class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none transition focus:border-pink-400 focus:ring-2 focus:ring-pink-100"
          >
        </label>
      </div>

      <label v-else class="block text-sm font-medium text-gray-700">
        学号
        <input :value="form.username" disabled class="mt-2 w-full rounded-lg border border-gray-200 bg-gray-100 px-3 py-2.5 text-gray-500">
      </label>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label class="block text-sm font-medium text-gray-700">
          姓名
          <input v-model="form.name" type="text" placeholder="未填写" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none transition focus:border-pink-400 focus:ring-2 focus:ring-pink-100">
        </label>
        <label class="block text-sm font-medium text-gray-700">
          邮箱
          <input v-model="form.email" type="email" placeholder="用于接收提醒" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none transition focus:border-pink-400 focus:ring-2 focus:ring-pink-100">
        </label>
      </div>

      <div v-if="mode === 'edit'">
        <p class="mb-2 text-sm font-medium text-gray-700">签到时间</p>
        <div class="flex items-center gap-2">
          <input v-model="form.signStartTime" type="time" min="18:30" step="60" class="min-w-0 flex-1 rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100">
          <span class="text-sm text-gray-400">至</span>
          <input v-model="form.signEndTime" type="time" max="23:59" step="60" class="min-w-0 flex-1 rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100">
        </div>
      </div>

      <label v-if="mode === 'add'" class="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 px-3 py-3">
        <span>
          <span class="block text-sm font-medium text-gray-800">自动签到</span>
          <span class="block text-xs text-gray-400">创建后立即加入自动签到任务</span>
        </span>
        <input v-model="form.autoSign" type="checkbox" class="h-5 w-5 rounded border-gray-300 text-rose-500 focus:ring-pink-300">
      </label>

      <div>
        <p class="mb-2 text-sm font-medium text-gray-700">签到日期</p>
        <div class="mb-2 flex flex-wrap gap-2">
          <button
            v-for="preset in presets"
            :key="preset.label"
            type="button"
            :class="isPresetActive(preset.value) ? 'border-rose-500 bg-rose-500 text-white' : 'border-pink-200 bg-pink-50 text-pink-600 hover:bg-pink-100'"
            class="rounded-full border px-3 py-1 text-xs font-medium transition-colors"
            @click="applyPreset(preset.value)"
          >
            {{ preset.label }}
          </button>
        </div>
        <div class="grid grid-cols-4 gap-2 sm:grid-cols-7">
          <button
            v-for="day in dayOptions"
            :key="day.value"
            type="button"
            :class="selectedDays.includes(day.value) ? 'border-rose-400 bg-rose-50 text-rose-600' : 'border-gray-200 bg-white text-gray-500 hover:border-pink-200'"
            class="rounded-lg border px-1 py-2 text-xs font-medium transition-colors"
            @click="toggleDay(day.value)"
          >
            {{ day.label }}
          </button>
        </div>
      </div>

      <p v-if="formError" class="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
        <i class="fas fa-circle-exclamation mr-1" aria-hidden="true"></i>{{ formError }}
      </p>
    </form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <button type="button" :disabled="loading" class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50" @click="emit('close')">
          取消
        </button>
        <button type="submit" form="admin-user-form" :disabled="loading" class="min-w-[104px] rounded-lg bg-rose-500 px-4 py-2 text-sm font-medium text-white hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-50">
          <i v-if="loading" class="fas fa-spinner fa-spin mr-1" aria-hidden="true"></i>
          {{ loading ? '保存中' : (mode === 'add' ? '创建用户' : '保存修改') }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>
