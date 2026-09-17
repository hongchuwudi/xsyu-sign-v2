<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: String,
  icon: { type: String, required: true },
  loading: Boolean
})

defineEmits(['refresh'])
</script>

<template>
  <header class="sticky top-0 z-20 border-b border-pink-200 bg-white/95 backdrop-blur-sm">
    <div class="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
      <div class="flex min-w-0 items-center gap-3">
        <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-rose-500 text-white shadow-sm">
          <i :class="icon" aria-hidden="true"></i>
        </div>
        <div class="min-w-0">
          <h1 class="truncate text-base font-bold text-gray-900 sm:text-lg">{{ title }}</h1>
          <p v-if="subtitle" class="hidden truncate text-xs text-gray-400 sm:block">{{ subtitle }}</p>
        </div>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <slot name="actions"></slot>
        <button
          type="button"
          title="刷新"
          aria-label="刷新"
          :disabled="loading"
          class="flex h-9 w-9 items-center justify-center rounded-lg border border-pink-200 text-pink-600 hover:bg-pink-50 disabled:cursor-not-allowed disabled:opacity-50"
          @click="$emit('refresh')"
        >
          <i class="fas fa-rotate" :class="{ 'fa-spin': loading }" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </header>
</template>
