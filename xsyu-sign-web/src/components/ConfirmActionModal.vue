<script setup>
import BaseModal from '@/components/BaseModal.vue'

defineProps({
  visible: Boolean,
  title: String,
  message: String,
  detail: String,
  confirmText: { type: String, default: '确认' },
  icon: { type: String, default: 'fas fa-circle-question' },
  danger: Boolean,
  loading: Boolean
})

defineEmits(['close', 'confirm'])
</script>

<template>
  <BaseModal
    :visible="visible"
    :title="title"
    :icon="icon"
    :theme="danger ? 'red' : 'pink'"
    width="max-w-sm"
    :closable="!loading"
    @close="$emit('close')"
  >
    <p class="text-sm leading-6 text-gray-700">{{ message }}</p>
    <p v-if="detail" class="mt-3 rounded-lg bg-gray-50 px-3 py-2 text-sm font-medium text-gray-800">
      {{ detail }}
    </p>

    <template #footer>
      <div class="flex justify-end gap-3">
        <button
          type="button"
          :disabled="loading"
          class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          @click="$emit('close')"
        >
          取消
        </button>
        <button
          type="button"
          :disabled="loading"
          :class="danger ? 'bg-red-500 hover:bg-red-600' : 'bg-rose-500 hover:bg-rose-600'"
          class="min-w-[96px] rounded-lg px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
          @click="$emit('confirm')"
        >
          <i v-if="loading" class="fas fa-spinner fa-spin mr-1" aria-hidden="true"></i>
          {{ loading ? '处理中' : confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>
