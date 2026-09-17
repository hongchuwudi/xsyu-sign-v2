<script setup>
// 公告页：最新公告（原"使用教程"内容）+ 关键源代码
import { ref, onMounted } from 'vue'
import { api } from '@/utils/api'
import BottomNav from '@/components/BottomNav.vue'
import UserMenuButton from '@/components/UserMenuButton.vue'

const activeTab = ref('tutorial')
const announcement = ref(null)
const isLoadingAnnouncement = ref(false)

onMounted(loadAnnouncement)

async function loadAnnouncement() {
  isLoadingAnnouncement.value = true
  try {
    const response = await api.getLatestAnnouncement()
    if (response.data.code === 200) announcement.value = response.data.data
  } catch (err) {
    console.error('加载公告失败:', err)
  } finally {
    isLoadingAnnouncement.value = false
  }
}

function formatDate(d) {
  if (!d) return ''
  const t = new Date(d)
  return t.getFullYear() + '-' + String(t.getMonth() + 1).padStart(2, '0') + '-' + String(t.getDate()).padStart(2, '0')
    + ' ' + String(t.getHours()).padStart(2, '0') + ':' + String(t.getMinutes()).padStart(2, '0')
}

function renderMarkdown(md) {
  if (!md) return ''
  return md
    .replace(/### (.+)/g, '<h3 class="text-lg font-semibold mt-3 mb-1">$1</h3>')
    .replace(/## (.+)/g, '<h2 class="text-xl font-bold mt-4 mb-2">$1</h2>')
    .replace(/# (.+)/g, '<h1 class="text-2xl font-bold mt-4 mb-2">$1</h1>')
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" class="rounded-lg my-3 max-w-full">')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/- (.+)/g, '<li class="ml-4">$1</li>')
    .replace(/<li/g, '<ul class="list-disc ml-4 my-2"><li')
    .replace(/<\/li>(?!.*<\/li>)/, '</li></ul>')
    .replace(/\n\n/g, '<br><br>')
    .replace(/\n/g, '<br>')
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
            <button @click="activeTab = 'source'"
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
        <div v-else-if="announcement">
          <h2 class="text-2xl font-bold text-gray-800 mb-2">{{ announcement.title }}</h2>
          <div class="flex items-center gap-3 text-xs text-gray-400 mb-6 pb-4 border-b border-gray-200">
            <span v-if="announcement.appVersion">
              <i class="fas fa-code-branch mr-1"></i>{{ announcement.appVersion }}
            </span>
            <span><i class="far fa-clock mr-1"></i>{{ formatDate(announcement.createdAt) }}</span>
          </div>
          <div class="prose prose-sm max-w-none text-gray-700" v-html="renderMarkdown(announcement.content)"></div>
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
