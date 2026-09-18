<script setup>
// 公告页：最新公告（原"使用教程"内容）+ 关键源代码
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { api } from '@/utils/api'
import BottomNav from '@/components/BottomNav.vue'
import UserMenuButton from '@/components/UserMenuButton.vue'

const activeTab = ref('tutorial')
const announcements = ref([])
const announcement = ref(null)
const isLoadingAnnouncement = ref(false)
const announcementError = ref('')
const historyOpen = ref(false)
const historyMenu = ref(null)

onMounted(() => {
  loadAnnouncements()
  document.addEventListener('click', closeHistoryOnOutsideClick)
})

onBeforeUnmount(() => document.removeEventListener('click', closeHistoryOnOutsideClick))

async function loadAnnouncements() {
  isLoadingAnnouncement.value = true
  announcementError.value = ''
  try {
    const response = await api.getUserAnnouncements()
    if (response.data.code !== 200) throw new Error(response.data.message || '公告加载失败')
    announcements.value = response.data.data || []
    announcement.value = announcements.value[0] || null
  } catch (err) {
    console.error('加载公告失败:', err)
    announcements.value = []
    announcement.value = null
    announcementError.value = err.response?.data?.message || err.message || '公告加载失败，请稍后重试'
  } finally {
    isLoadingAnnouncement.value = false
  }
}

function selectAnnouncement(item) {
  announcement.value = item
  historyOpen.value = false
}

function closeHistoryOnOutsideClick(event) {
  if (historyOpen.value && !historyMenu.value?.contains(event.target)) historyOpen.value = false
}

function formatDate(d) {
  if (!d) return ''
  const t = new Date(d)
  return t.getFullYear() + '-' + String(t.getMonth() + 1).padStart(2, '0') + '-' + String(t.getDate()).padStart(2, '0')
    + ' ' + String(t.getHours()).padStart(2, '0') + ':' + String(t.getMinutes()).padStart(2, '0')
}

</script>

<template>
  <div class="min-h-screen bg-white pb-14">
    <!-- 顶部标题栏 -->
    <div class="bg-white shadow-sm border-b border-pink-100">
      <div class="max-w-5xl mx-auto px-4 py-2 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <i class="fas fa-bullhorn text-pink-400 text-xl"></i>
          <h1 class="text-lg font-bold text-gray-800">公告</h1>
        </div>
        <div class="flex items-center gap-2">
          <!-- 标签切换 -->
          <div class="flex bg-pink-50 rounded-lg p-1">
            <button @click="activeTab = 'tutorial'"
                    :class="activeTab === 'tutorial' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                    class="px-3 sm:px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
              <i class="fas fa-book-open mr-1"></i>教程
            </button>
            <button @click="activeTab = 'source'; historyOpen = false"
                    :class="activeTab === 'source' ? 'bg-white text-purple-600 shadow-sm' : 'text-purple-400 hover:text-purple-600'"
                    class="px-3 sm:px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
              <i class="fas fa-code mr-1"></i>源码
            </button>
          </div>
          <UserMenuButton />
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="max-w-5xl mx-auto">
      <!-- 使用教程 -->
      <div v-show="activeTab === 'tutorial'" class="bg-white p-4">
        <div v-if="isLoadingAnnouncement" class="text-center py-12">
          <i class="fas fa-spinner fa-spin text-pink-400 text-3xl mb-3"></i>
          <p class="text-pink-400">加载中...</p>
        </div>
        <div v-else-if="announcementError" class="text-center py-12">
          <i class="fas fa-triangle-exclamation text-rose-300 text-4xl mb-3"></i>
          <p class="text-sm text-gray-500">{{ announcementError }}</p>
          <button type="button" class="mt-4 rounded-lg border border-pink-200 px-4 py-2 text-sm font-medium text-pink-600 hover:bg-pink-50" @click="loadAnnouncements">重新加载</button>
        </div>
        <div v-else-if="announcement">
          <div class="mb-2 flex items-start justify-between gap-3">
            <h2 class="min-w-0 text-2xl font-bold text-gray-800">{{ announcement.title }}</h2>
            <div v-if="announcements.length > 1" ref="historyMenu" class="relative shrink-0">
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg border border-pink-200 bg-white px-2.5 py-1.5 text-xs font-medium text-pink-600 shadow-sm transition-colors hover:bg-pink-50"
                :aria-expanded="historyOpen"
                aria-haspopup="listbox"
                @click.stop="historyOpen = !historyOpen"
              >
                <i class="fas fa-clock-rotate-left" aria-hidden="true"></i>
                <span>历史公告</span>
                <i class="fas fa-chevron-down text-[10px] transition-transform" :class="{ 'rotate-180': historyOpen }" aria-hidden="true"></i>
              </button>
              <div v-if="historyOpen" class="absolute right-0 z-20 mt-2 max-h-80 w-80 max-w-[calc(100vw-2rem)] overflow-y-auto rounded-xl border border-pink-100 bg-white p-1.5 shadow-xl" role="listbox">
                <button
                  v-for="(item, index) in announcements"
                  :key="item.id"
                  type="button"
                  role="option"
                  :aria-selected="announcement.id === item.id"
                  :class="announcement.id === item.id ? 'bg-pink-50 text-pink-700' : 'text-gray-700 hover:bg-gray-50'"
                  class="flex w-full items-start justify-between gap-3 rounded-lg px-3 py-2.5 text-left transition-colors"
                  @click="selectAnnouncement(item)"
                >
                  <span class="min-w-0">
                    <span class="flex items-center gap-2">
                      <span class="truncate text-sm font-medium">{{ item.title }}</span>
                      <span v-if="index === 0" class="shrink-0 rounded-full bg-rose-100 px-1.5 py-0.5 text-[10px] font-semibold text-rose-600">最新</span>
                    </span>
                    <span class="mt-1 block text-[11px] text-gray-400">{{ formatDate(item.createdAt) }}</span>
                  </span>
                  <span v-if="item.appVersion" class="shrink-0 rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-medium text-blue-600">v{{ item.appVersion }}</span>
                </button>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-3 text-xs text-gray-400 mb-6 pb-4 border-b border-gray-200">
            <span v-if="announcement.appVersion">
              <i class="fas fa-code-branch mr-1"></i>{{ announcement.appVersion }}
            </span>
            <span><i class="far fa-clock mr-1"></i>{{ formatDate(announcement.createdAt) }}</span>
          </div>
          <MdPreview :model-value="announcement.content || ''" preview-theme="github" />
        </div>
        <div v-else class="text-center py-12">
          <i class="fas fa-inbox text-gray-200 text-4xl mb-3"></i>
          <p class="text-gray-400">暂无教程公告</p>
        </div>
      </div>

      <!-- 关键源代码 -->
      <div v-show="activeTab === 'source'" class="overflow-hidden" style="height: calc(100vh - 56px);">
        <iframe src="/code/XSYUOneKeySign.html" class="w-full h-full border-0"></iframe>
      </div>
    </div>

    <BottomNav />
  </div>
</template>
