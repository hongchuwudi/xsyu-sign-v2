<script setup>
// 签到卡片组件（状态与操作入口）
import { getSignStatusDesc, getSignStatusClass, isValidSign, formatDateTime, formatTime } from '@/utils/signStatus'

defineProps({
  sign: { type: Object, required: true }
})
defineEmits(['view-detail', 'sign-single'])
</script>

<template>
  <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-4 shadow hover:shadow-md transition-shadow">
    <div class="flex justify-between items-start mb-3">
      <div class="flex-1 mr-2">
        <div class="flex items-center gap-2 mb-1">
          <span class="font-semibold text-pink-800 text-sm truncate">{{ sign.signTitle || '未命名签到' }}</span>
          <span v-if="sign.isRead === 1"
                class="bg-pink-100 text-pink-700 text-xs px-2 py-0.5 rounded-full">已读</span>
        </div>
        <p v-if="sign.signContext" class="text-pink-600 text-sm line-clamp-2">{{ sign.signContext }}</p>
      </div>

      <span :class="getSignStatusClass(sign)"
            class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium">
        {{ getSignStatusDesc(sign) }}
      </span>
    </div>

    <!-- 课程信息 -->
    <div class="flex items-center text-sm text-pink-500 mb-3">
      <i class="fas fa-graduation-cap mr-2"></i>
      <span>{{ sign.college || '未设置学院' }}</span>
      <span v-if="sign.major" class="mx-1">·</span>
      <span>{{ sign.major || '' }}</span>
    </div>

    <!-- 时间和位置 -->
    <div class="space-y-2 text-sm">
      <div class="flex items-center text-pink-700">
        <i class="fas fa-clock mr-2 w-4 text-pink-400"></i>
        <span class="font-medium">{{ formatDateTime(sign.start) }}</span>
        <span class="mx-2 text-pink-300">~</span>
        <span class="font-medium">{{ formatTime(sign.end) }}</span>
      </div>

      <div v-if="sign.area" class="flex items-center text-pink-600">
        <i class="fas fa-map-marker-alt mr-2 w-4 text-pink-400"></i>
        <span class="truncate">{{ sign.area }}</span>
      </div>

      <div v-if="sign.createName" class="flex items-center text-pink-600">
        <i class="fas fa-user-tie mr-2 w-4 text-pink-400"></i>
        <span>创建: {{ sign.createName }}</span>
        <span v-if="sign.teacher" class="ml-2">({{ sign.teacher }})</span>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex justify-between items-center mt-4 pt-4 border-t border-pink-100">
      <button @click="$emit('view-detail', sign)"
              class="text-pink-500 hover:text-pink-600 text-sm font-medium flex items-center">
        <i class="fas fa-eye mr-1"></i>查看详情
      </button>

      <button v-if="isValidSign(sign)" @click="$emit('sign-single', sign)"
              class="bg-pink-400 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity flex items-center">
        <i class="fas fa-check mr-1"></i>立即签到
      </button>
    </div>
  </div>
</template>
