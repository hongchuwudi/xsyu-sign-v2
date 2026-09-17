<script setup>
// 弹窗外壳组件：所有模态框复用
// props: visible/title/subtitle/icon/theme(pink|rose|red|purple)/width/scrollable/closable
// slots: 默认(内容) + footer(底部按钮区)
import { computed } from 'vue'

const props = defineProps({
  visible: Boolean,
  title: String,
  subtitle: String,
  icon: String,
  theme: { type: String, default: 'pink' },
  width: { type: String, default: 'max-w-md' },
  scrollable: Boolean,
  closable: { type: Boolean, default: true }
})
const emit = defineEmits(['close'])

const themeClass = computed(() => ({
  pink: 'bg-gradient-to-r from-pink-400 to-rose-400',
  rose: 'bg-gradient-to-r from-rose-400 to-rose-500',
  red: 'bg-gradient-to-r from-red-500 to-red-600',
  purple: 'bg-gradient-to-r from-purple-400 to-fuchsia-400'
}[props.theme] || 'bg-gradient-to-r from-pink-400 to-rose-400'))
</script>

<template>
  <div :class="{ active: visible }" class="modal-overlay" @click.self="emit('close')">
    <div :class="width" class="modal-box w-full mx-4">
      <div :class="themeClass" class="text-white p-6 rounded-t-lg">
        <div class="flex justify-between items-start">
          <div class="pr-4">
            <h3 class="text-xl font-bold">
              <i v-if="icon" :class="icon" class="mr-2"></i>{{ title }}
            </h3>
            <p v-if="subtitle" class="text-pink-100 text-sm mt-1">{{ subtitle }}</p>
          </div>
          <button v-if="closable" @click="emit('close')" class="text-white hover:text-pink-200 text-xl">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
      <div class="p-6" :class="{ 'max-h-[70vh] overflow-y-auto': scrollable }">
        <slot></slot>
      </div>
      <div v-if="$slots.footer" class="border-t border-pink-100 p-6">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</template>
