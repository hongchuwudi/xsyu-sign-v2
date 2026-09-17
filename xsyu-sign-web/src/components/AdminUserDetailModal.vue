<script setup>
import BaseModal from '@/components/BaseModal.vue'
import { formatDateTime, getSignStatusClass, getSignStatusDesc } from '@/utils/signStatus'

defineProps({
  visible: Boolean,
  user: { type: Object, default: null },
  signs: { type: Array, default: () => [] },
  loading: Boolean
})

defineEmits(['close'])

const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function formatSignDays(value) {
  if (!value) return '未设置'
  const days = value.split(',').map(Number).filter(day => day >= 0 && day <= 6)
  if (days.length === 7) return '每天'
  return days.map(day => dayNames[day]).join('、') || '未设置'
}

function formatFull(value) {
  if (!value) return '--'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<template>
  <BaseModal
    :visible="visible"
    title="用户详情"
    :subtitle="user?.username"
    icon="fas fa-address-card"
    width="max-w-2xl"
    scrollable
    @close="$emit('close')"
  >
    <div v-if="loading" class="py-14 text-center text-pink-400">
      <i class="fas fa-spinner fa-spin mb-3 text-2xl" aria-hidden="true"></i>
      <p class="text-sm">正在加载用户信息</p>
    </div>

    <template v-else-if="user">
      <dl class="grid grid-cols-1 gap-x-6 sm:grid-cols-2">
        <div v-for="item in [
          ['姓名', user.name || '--'],
          ['邮箱', user.email || '--'],
          ['自动签到', user.autoSign ? '已开启' : '已关闭'],
          ['JWS 状态', user.jws ? '有效' : '失效'],
          ['签到日期', formatSignDays(user.signDays)],
          ['签到时段', user.signStartTime && user.signEndTime ? `${user.signStartTime} - ${user.signEndTime}` : '--'],
          ['JWS 续签', formatFull(user.jwsRefreshedAt)],
          ['最近更新', formatFull(user.updatedAt)]
        ]" :key="item[0]" class="flex justify-between gap-4 border-b border-gray-100 py-3 text-sm">
          <dt class="shrink-0 text-gray-400">{{ item[0] }}</dt>
          <dd class="break-all text-right font-medium text-gray-800">{{ item[1] }}</dd>
        </div>
      </dl>

      <section class="mt-6 border-t border-pink-100 pt-5">
        <h4 class="mb-3 text-sm font-bold text-gray-800">
          <i class="fas fa-clock-rotate-left mr-2 text-pink-400" aria-hidden="true"></i>最近签到
        </h4>
        <div v-if="signs.length" class="space-y-2">
          <article v-for="(sign, index) in signs" :key="sign.id || index" class="rounded-lg border border-pink-100 bg-pink-50/50 p-3">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-gray-800">{{ sign.signTitle || '未命名签到' }}</p>
                <p class="mt-1 truncate text-xs text-gray-400">{{ sign.signContext || formatDateTime(sign.start) || '暂无描述' }}</p>
              </div>
              <span :class="getSignStatusClass(sign)" class="shrink-0 rounded-full px-2 py-1 text-xs font-medium">
                {{ getSignStatusDesc(sign) }}
              </span>
            </div>
          </article>
        </div>
        <div v-else class="rounded-lg border border-dashed border-pink-200 py-8 text-center text-sm text-gray-400">
          <i class="fas fa-inbox mb-2 block text-2xl text-pink-200" aria-hidden="true"></i>暂无签到记录
        </div>
      </section>
    </template>

    <template #footer>
      <div class="flex justify-end">
        <button type="button" class="rounded-lg bg-rose-500 px-5 py-2 text-sm font-medium text-white hover:bg-rose-600" @click="$emit('close')">关闭</button>
      </div>
    </template>
  </BaseModal>
</template>
