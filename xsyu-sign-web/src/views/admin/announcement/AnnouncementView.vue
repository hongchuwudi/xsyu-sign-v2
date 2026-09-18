<script setup>
import { reactive, ref, onMounted } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import BaseModal from '@/components/BaseModal.vue'
import ConfirmActionModal from '@/components/ConfirmActionModal.vue'

const announcements = ref([])
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const uploading = ref(0)
const error = ref('')
const showForm = ref(false)
const editingId = ref(null)
const deleteTarget = ref(null)
const formError = ref('')
const form = reactive({ title: '', content: '', appVersion: '', draftToken: '' })

function requestMessage(requestError, fallback) {
  return requestError.response?.data?.message || requestError.message || fallback
}

function newDraftToken() {
  return typeof crypto?.randomUUID === 'function'
    ? crypto.randomUUID().replaceAll('-', '')
    : `${Date.now()}${Math.random().toString(16).slice(2)}`
}

async function loadAnnouncements() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.getAnnouncements()
    if (response.data?.code !== 200) throw new Error(response.data?.message || '获取公告失败')
    announcements.value = response.data.data || []
  } catch (requestError) {
    error.value = requestMessage(requestError, '获取公告失败')
    showMessage(error.value, 'error')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  Object.assign(form, { title: '', content: '', appVersion: '', draftToken: newDraftToken() })
  formError.value = ''
  showForm.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, { title: item.title || '', content: item.content || '', appVersion: item.appVersion || '', draftToken: newDraftToken() })
  formError.value = ''
  showForm.value = true
}

async function onUploadImg(files, callback) {
  uploading.value += files.length
  const urls = []
  for (const file of files) {
    try {
      const response = await api.uploadImage(file, form.draftToken)
      if (response.data?.code !== 200 || !response.data.data?.url) throw new Error(response.data?.message || '图片上传失败')
      urls.push(response.data.data.url)
    } catch (requestError) {
      showMessage(requestMessage(requestError, `${file.name} 上传失败`), 'error')
    } finally {
      uploading.value -= 1
    }
  }
  if (urls.length) callback(urls)
}

async function saveAnnouncement() {
  formError.value = ''
  if (!form.title.trim()) return (formError.value = '请输入公告标题')
  if (!form.content.trim()) return (formError.value = '请输入公告内容')
  if (uploading.value) return (formError.value = '图片仍在上传，请稍候')
  saving.value = true
  const payload = { title: form.title.trim(), content: form.content.trim(), appVersion: form.appVersion.trim(), draftToken: form.draftToken }
  try {
    const response = editingId.value ? await api.updateAnnouncement(editingId.value, payload) : await api.addAnnouncement(payload)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '保存公告失败')
    showMessage(editingId.value ? '公告已更新' : '公告已发布')
    showForm.value = false
    await loadAnnouncements()
  } catch (requestError) {
    showMessage(requestMessage(requestError, '保存公告失败'), 'error')
  } finally {
    saving.value = false
  }
}

async function deleteAnnouncement() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    const response = await api.deleteAnnouncement(deleteTarget.value.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '删除公告失败')
    showMessage('公告已删除')
    deleteTarget.value = null
    await loadAnnouncements()
  } catch (requestError) {
    showMessage(requestMessage(requestError, '删除公告失败'), 'error')
  } finally {
    deleting.value = false
  }
}

function formatTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ')
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadAnnouncements)
</script>

<template>
  <div>
    <AdminPageHeader title="公告管理" subtitle="发布和维护用户端公告" icon="fas fa-bullhorn" :loading="loading" @refresh="loadAnnouncements">
      <template #actions><button type="button" class="h-9 rounded-lg bg-rose-500 px-3 text-sm font-medium text-white hover:bg-rose-600" @click="openAdd"><i class="fas fa-plus sm:mr-1" aria-hidden="true"></i><span class="hidden sm:inline">发布公告</span></button></template>
    </AdminPageHeader>

    <main class="mx-auto max-w-5xl px-4 py-5 sm:px-6 sm:py-6">
      <div v-if="loading && !announcements.length" class="py-24 text-center text-pink-400"><i class="fas fa-spinner fa-spin mb-3 text-2xl" aria-hidden="true"></i><p class="text-sm">正在加载公告</p></div>
      <div v-else-if="error && !announcements.length" class="rounded-lg border border-rose-200 bg-white py-16 text-center"><i class="fas fa-triangle-exclamation mb-3 text-2xl text-rose-400" aria-hidden="true"></i><p class="text-sm text-gray-600">{{ error }}</p></div>
      <div v-else-if="!announcements.length" class="rounded-lg border border-dashed border-pink-200 bg-white py-24 text-center"><i class="fas fa-bullhorn mb-3 block text-3xl text-pink-200" aria-hidden="true"></i><p class="text-sm font-medium text-gray-600">还没有发布公告</p><button type="button" class="mt-3 text-sm font-medium text-rose-500 hover:text-rose-600" @click="openAdd">发布第一条公告</button></div>
      <section v-else class="space-y-3">
        <article v-for="(item, index) in announcements" :key="item.id" class="rounded-lg border border-pink-200 bg-white p-4 shadow-sm sm:p-5">
          <div class="flex items-start justify-between gap-4"><div class="min-w-0 flex-1"><div class="flex flex-wrap items-center gap-2"><h2 class="break-words text-base font-bold text-gray-900">{{ item.title }}</h2><span v-if="index === 0" class="rounded-full bg-rose-50 px-2 py-0.5 text-xs font-medium text-rose-600">最新</span><span v-if="item.appVersion" class="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600">v{{ item.appVersion }}</span></div><p class="mt-2 line-clamp-4 whitespace-pre-wrap break-words text-sm leading-6 text-gray-600">{{ item.content }}</p><p class="mt-3 text-xs text-gray-400">发布于 {{ formatTime(item.createdAt) }}<template v-if="item.updatedAt && item.updatedAt !== item.createdAt"> · 更新于 {{ formatTime(item.updatedAt) }}</template></p></div><div class="flex shrink-0 gap-1"><button type="button" title="编辑公告" aria-label="编辑公告" class="flex h-8 w-8 items-center justify-center rounded-lg text-blue-500 hover:bg-blue-50" @click="openEdit(item)"><i class="fas fa-pen" aria-hidden="true"></i></button><button type="button" title="删除公告" aria-label="删除公告" class="flex h-8 w-8 items-center justify-center rounded-lg text-red-500 hover:bg-red-50" @click="deleteTarget = item"><i class="fas fa-trash-can" aria-hidden="true"></i></button></div></div>
        </article>
      </section>
    </main>

    <BaseModal :visible="showForm" :title="editingId ? '编辑公告' : '发布公告'" :subtitle="editingId ? '修改后会立即更新用户端内容' : '支持 Markdown、图片粘贴、拖拽和选择上传'" icon="fas fa-bullhorn" width="max-w-4xl" scrollable :closable="!saving && !uploading" @close="!saving && !uploading && (showForm = false)">
      <form id="announcement-form" class="space-y-4" @submit.prevent="saveAnnouncement">
        <label class="block text-sm font-medium text-gray-700">标题 <span class="text-red-500">*</span><input v-model="form.title" type="text" maxlength="100" placeholder="请输入公告标题" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label>
        <div><div class="mb-2 flex items-center justify-between text-sm font-medium text-gray-700"><span>内容 <span class="text-red-500">*</span></span><span v-if="uploading" class="font-normal text-rose-500"><i class="fas fa-spinner fa-spin mr-1"></i>图片上传中（{{ uploading }}）</span></div><MdEditor v-model="form.content" language="zh-CN" :toolbars="['bold', 'italic', 'strikeThrough', 'title', 'quote', 'unorderedList', 'orderedList', 'link', 'image', 'table', 'revoke', 'next']" :on-upload-img="onUploadImg" preview-theme="github" :style="{ minHeight: '360px' }" /></div>
        <label class="block text-sm font-medium text-gray-700">适用版本<input v-model="form.appVersion" type="text" maxlength="30" placeholder="可选，例如 2.1.0" class="mt-2 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"></label>
        <p v-if="formError" class="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">{{ formError }}</p>
      </form>
      <template #footer><div class="flex justify-end gap-3"><button type="button" :disabled="saving || uploading" class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50" @click="showForm = false">取消</button><button type="submit" form="announcement-form" :disabled="saving || uploading" class="min-w-[96px] rounded-lg bg-rose-500 px-4 py-2 text-sm font-medium text-white hover:bg-rose-600 disabled:opacity-50"><i v-if="saving" class="fas fa-spinner fa-spin mr-1" aria-hidden="true"></i>{{ saving ? '保存中' : (editingId ? '保存修改' : '发布公告') }}</button></div></template>
    </BaseModal>
    <ConfirmActionModal :visible="Boolean(deleteTarget)" title="删除公告" message="删除后用户端将无法再查看这条公告，此操作无法撤销。图片文件会进入孤立清理流程。" :detail="deleteTarget?.title" confirm-text="确认删除" icon="fas fa-trash-can" danger :loading="deleting" @close="!deleting && (deleteTarget = null)" @confirm="deleteAnnouncement" />
  </div>
</template>
