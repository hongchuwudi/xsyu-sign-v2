<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import BaseModal from '@/components/BaseModal.vue'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])

const tabs = [
  { key: 'compose', label: '撰写邮件', icon: 'fas fa-pen-to-square' },
  { key: 'templates', label: '模板', icon: 'fas fa-file-lines' },
  { key: 'groups', label: '用户组', icon: 'fas fa-user-group' },
  { key: 'history', label: '发送记录', icon: 'fas fa-clock-rotate-left' }
]
const activeTab = ref('compose')
const loading = ref(false)
const saving = ref(false)
const templates = ref([])
const groups = ref([])
const users = ref([])
const tasks = ref([])
const userKeyword = ref('')
const selectedTask = ref(null)
const recipients = ref([])
const recipientsLoading = ref(false)
let searchTimer

const composer = reactive({
  templateId: '',
  subject: '',
  content: '',
  userIds: [],
  groupIds: []
})
const templateForm = reactive({ id: null, name: '', subject: '', content: '' })
const groupForm = reactive({ id: null, name: '', description: '', userIds: [] })

const selectedRecipientIds = computed(() => {
  const ids = new Set(composer.userIds)
  groups.value.filter(group => composer.groupIds.includes(group.id))
    .forEach(group => group.members?.forEach(user => ids.add(user.id)))
  return ids
})

function responseData(response, fallback) {
  if (response.data?.code !== 200) throw new Error(response.data?.message || fallback)
  return response.data.data
}

function requestMessage(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

async function loadWorkspace() {
  loading.value = true
  try {
    const [templateResponse, groupResponse, userResponse, taskResponse] = await Promise.all([
      api.getEmailTemplates(),
      api.getEmailGroups(),
      api.searchEmailUsers(),
      api.getEmailTasks()
    ])
    templates.value = responseData(templateResponse, '获取邮件模板失败') || []
    groups.value = responseData(groupResponse, '获取用户组失败') || []
    users.value = responseData(userResponse, '获取用户失败') || []
    tasks.value = responseData(taskResponse, '获取发送记录失败') || []
  } catch (error) {
    showMessage(requestMessage(error, '加载邮件通知数据失败'), 'error')
  } finally {
    loading.value = false
  }
}

async function searchUsers() {
  try {
    users.value = responseData(await api.searchEmailUsers(userKeyword.value), '搜索用户失败') || []
  } catch (error) {
    showMessage(requestMessage(error, '搜索用户失败'), 'error')
  }
}

function queueUserSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(searchUsers, 300)
}

function toggleId(list, id) {
  const index = list.indexOf(id)
  if (index >= 0) list.splice(index, 1)
  else list.push(id)
}

function applyTemplate() {
  const template = templates.value.find(item => item.id === Number(composer.templateId))
  if (!template) return
  composer.subject = template.subject
  composer.content = template.content
}

function insertVariable(variable) {
  composer.content += variable
}

async function submitEmail() {
  if (!composer.userIds.length && !composer.groupIds.length) return showMessage('请至少选择一个用户或用户组', 'error')
  if (!composer.subject.trim()) return showMessage('请输入邮件主题', 'error')
  if (!composer.content.trim()) return showMessage('请输入邮件内容', 'error')
  saving.value = true
  const payload = {
    subject: composer.subject,
    content: composer.content,
    userIds: [...composer.userIds],
    groupIds: [...composer.groupIds]
  }
  try {
    responseData(await api.sendEmailNow(payload), '发送邮件失败')
    showMessage('邮件任务已开始发送')
    composer.userIds = []
    composer.groupIds = []
    tasks.value = responseData(await api.getEmailTasks(), '刷新发送记录失败') || []
    activeTab.value = 'history'
  } catch (error) {
    showMessage(requestMessage(error, '发送邮件失败'), 'error')
  } finally {
    saving.value = false
  }
}

function resetTemplateForm() {
  Object.assign(templateForm, { id: null, name: '', subject: '', content: '' })
}

function editTemplate(template) {
  Object.assign(templateForm, { id: template.id, name: template.name, subject: template.subject, content: template.content })
}

async function saveTemplate() {
  if (!templateForm.name.trim() || !templateForm.subject.trim() || !templateForm.content.trim()) {
    return showMessage('请完整填写模板名称、主题和内容', 'error')
  }
  saving.value = true
  try {
    const payload = { name: templateForm.name, subject: templateForm.subject, content: templateForm.content }
    if (templateForm.id) responseData(await api.updateEmailTemplate(templateForm.id, payload), '更新模板失败')
    else responseData(await api.addEmailTemplate(payload), '创建模板失败')
    showMessage(templateForm.id ? '模板已更新' : '模板已创建')
    templates.value = responseData(await api.getEmailTemplates(), '刷新模板失败') || []
    resetTemplateForm()
  } catch (error) {
    showMessage(requestMessage(error, '保存模板失败'), 'error')
  } finally {
    saving.value = false
  }
}

async function removeTemplate(template) {
  if (!window.confirm(`确定删除模板“${template.name}”吗？`)) return
  try {
    responseData(await api.deleteEmailTemplate(template.id), '删除模板失败')
    templates.value = templates.value.filter(item => item.id !== template.id)
    if (templateForm.id === template.id) resetTemplateForm()
    showMessage('模板已删除')
  } catch (error) {
    showMessage(requestMessage(error, '删除模板失败'), 'error')
  }
}

function resetGroupForm() {
  Object.assign(groupForm, { id: null, name: '', description: '', userIds: [] })
}

function editGroup(group) {
  Object.assign(groupForm, {
    id: group.id,
    name: group.name,
    description: group.description || '',
    userIds: group.members?.map(user => user.id) || []
  })
}

async function saveGroup() {
  if (!groupForm.name.trim()) return showMessage('请输入用户组名称', 'error')
  saving.value = true
  try {
    const payload = { name: groupForm.name, description: groupForm.description, userIds: [...groupForm.userIds] }
    if (groupForm.id) responseData(await api.updateEmailGroup(groupForm.id, payload), '更新用户组失败')
    else responseData(await api.addEmailGroup(payload), '创建用户组失败')
    showMessage(groupForm.id ? '用户组已更新' : '用户组已创建')
    groups.value = responseData(await api.getEmailGroups(), '刷新用户组失败') || []
    resetGroupForm()
  } catch (error) {
    showMessage(requestMessage(error, '保存用户组失败'), 'error')
  } finally {
    saving.value = false
  }
}

async function removeGroup(group) {
  if (!window.confirm(`确定删除用户组“${group.name}”吗？`)) return
  try {
    responseData(await api.deleteEmailGroup(group.id), '删除用户组失败')
    groups.value = groups.value.filter(item => item.id !== group.id)
    composer.groupIds = composer.groupIds.filter(id => id !== group.id)
    if (groupForm.id === group.id) resetGroupForm()
    showMessage('用户组已删除')
  } catch (error) {
    showMessage(requestMessage(error, '删除用户组失败'), 'error')
  }
}

async function openTask(task) {
  selectedTask.value = task
  recipientsLoading.value = true
  try {
    recipients.value = responseData(await api.getEmailTaskRecipients(task.id), '获取收件人明细失败') || []
  } catch (error) {
    showMessage(requestMessage(error, '获取收件人明细失败'), 'error')
  } finally {
    recipientsLoading.value = false
  }
}

async function refreshTasks() {
  try {
    tasks.value = responseData(await api.getEmailTasks(), '刷新发送记录失败') || []
    if (selectedTask.value) {
      selectedTask.value = tasks.value.find(task => task.id === selectedTask.value.id) || null
      if (selectedTask.value) await openTask(selectedTask.value)
    }
  } catch (error) {
    showMessage(requestMessage(error, '刷新发送记录失败'), 'error')
  }
}

async function cancelTask(task) {
  if (!window.confirm(`确定取消邮件任务“${task.subject}”吗？`)) return
  try {
    responseData(await api.cancelEmailTask(task.id), '取消任务失败')
    tasks.value = responseData(await api.getEmailTasks(), '刷新发送记录失败') || []
    showMessage('定时邮件已取消')
  } catch (error) {
    showMessage(requestMessage(error, '取消任务失败'), 'error')
  }
}

function formatDateTime(value) {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 16)
}

function statusText(status) {
  return { PENDING: '等待发送', SENDING: '发送中', COMPLETED: '已完成', CANCELLED: '已取消', FAILED: '执行失败', SUCCESS: '成功' }[status] || status
}

function statusClass(status) {
  return {
    PENDING: 'bg-amber-50 text-amber-700',
    SENDING: 'bg-sky-50 text-sky-700',
    COMPLETED: 'bg-emerald-50 text-emerald-700',
    SUCCESS: 'bg-emerald-50 text-emerald-700',
    CANCELLED: 'bg-gray-100 text-gray-500',
    FAILED: 'bg-red-50 text-red-600'
  }[status] || 'bg-gray-100 text-gray-600'
}

watch(() => props.visible, visible => {
  if (visible) loadWorkspace()
})
</script>

<template>
  <BaseModal :visible="visible" title="邮件通知" subtitle="选择收件人，编辑内容并立即或定时发送" icon="fas fa-envelope" width="max-w-6xl" scrollable :closable="!saving" @close="emit('close')">
    <div v-if="loading" class="flex min-h-[420px] items-center justify-center text-pink-500">
      <i class="fas fa-spinner fa-spin mr-2" aria-hidden="true"></i>正在加载
    </div>
    <div v-else class="min-h-[520px]">
      <nav class="mb-5 grid grid-cols-2 gap-2 border-b border-gray-200 pb-3 sm:grid-cols-4" aria-label="邮件通知功能">
        <button v-for="tab in tabs" :key="tab.key" type="button" :class="activeTab === tab.key ? 'bg-rose-500 text-white' : 'bg-gray-50 text-gray-600 hover:bg-gray-100'" class="h-10 rounded-lg px-3 text-sm font-medium" @click="activeTab = tab.key">
          <i :class="tab.icon" class="mr-1.5" aria-hidden="true"></i>{{ tab.label }}
        </button>
      </nav>

      <section v-if="activeTab === 'compose'" class="grid gap-5 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
        <div class="space-y-4">
          <div>
            <div class="mb-2 flex items-center justify-between"><h4 class="text-sm font-bold text-gray-800">选择用户组</h4><span class="text-xs text-gray-400">可多选</span></div>
            <div v-if="groups.length" class="grid max-h-36 grid-cols-1 gap-2 overflow-y-auto sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
              <label v-for="group in groups" :key="group.id" :class="composer.groupIds.includes(group.id) ? 'border-rose-300 bg-rose-50' : 'border-gray-200 bg-white'" class="flex cursor-pointer items-center gap-3 rounded-lg border p-3">
                <input type="checkbox" :checked="composer.groupIds.includes(group.id)" class="h-4 w-4 rounded border-gray-300 text-rose-500 focus:ring-rose-200" @change="toggleId(composer.groupIds, group.id)">
                <span class="min-w-0"><span class="block truncate text-sm font-medium text-gray-800">{{ group.name }}</span><span class="text-xs text-gray-400">{{ group.memberCount }} 人</span></span>
              </label>
            </div>
            <p v-else class="rounded-lg border border-dashed border-gray-200 py-5 text-center text-xs text-gray-400">暂无用户组</p>
          </div>

          <div>
            <div class="mb-2 flex items-center justify-between"><h4 class="text-sm font-bold text-gray-800">选择用户</h4><span class="text-xs text-gray-400">最多显示 100 条</span></div>
            <div class="relative mb-2"><i class="fas fa-magnifying-glass absolute left-3 top-3 text-xs text-gray-400" aria-hidden="true"></i><input v-model="userKeyword" type="search" placeholder="搜索姓名、学号或邮箱" class="w-full rounded-lg border border-gray-300 py-2 pl-9 pr-3 text-sm outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100" @input="queueUserSearch"></div>
            <div class="max-h-64 space-y-1 overflow-y-auto rounded-lg border border-gray-200 p-2">
              <label v-for="user in users" :key="user.id" class="flex cursor-pointer items-center gap-3 rounded-md px-2 py-2 hover:bg-gray-50">
                <input type="checkbox" :checked="composer.userIds.includes(user.id)" class="h-4 w-4 rounded border-gray-300 text-rose-500 focus:ring-rose-200" @change="toggleId(composer.userIds, user.id)">
                <span class="min-w-0 flex-1"><span class="block truncate text-sm font-medium text-gray-800">{{ user.name || user.username }}</span><span class="block truncate text-xs text-gray-400">{{ user.username }} · {{ user.email }}</span></span>
              </label>
              <p v-if="!users.length" class="py-6 text-center text-xs text-gray-400">没有找到已设置邮箱的用户</p>
            </div>
          </div>
        </div>

        <form class="space-y-4" @submit.prevent="submitEmail">
          <div class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
            <label class="block text-sm font-medium text-gray-700">使用模板<select v-model="composer.templateId" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"><option value="">不使用模板</option><option v-for="template in templates" :key="template.id" :value="template.id">{{ template.name }}</option></select></label>
            <button type="button" :disabled="!composer.templateId" class="h-[42px] rounded-lg border border-rose-200 px-4 text-sm font-medium text-rose-600 hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40" @click="applyTemplate"><i class="fas fa-arrow-down mr-1" aria-hidden="true"></i>填入模板</button>
          </div>
          <label class="block text-sm font-medium text-gray-700">邮件主题<input v-model="composer.subject" type="text" maxlength="255" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></label>
          <div>
            <div class="mb-1.5 flex flex-wrap items-center justify-between gap-2"><label for="email-content" class="text-sm font-medium text-gray-700">邮件内容</label><div class="flex flex-wrap gap-1"><button v-for="variable in ['{{name}}','{{username}}','{{email}}']" :key="variable" type="button" class="rounded border border-gray-200 bg-gray-50 px-2 py-1 font-mono text-[11px] text-gray-600 hover:border-rose-200 hover:text-rose-600" @click="insertVariable(variable)">{{ variable }}</button></div></div>
            <textarea id="email-content" v-model="composer.content" rows="10" class="w-full resize-y rounded-lg border border-gray-300 px-3 py-2.5 text-sm leading-6 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></textarea>
          </div>
          <div class="flex flex-col gap-3 border-t border-gray-100 pt-4 sm:flex-row sm:items-center sm:justify-between"><p class="text-sm text-gray-500">已选择 <strong class="text-gray-900">{{ selectedRecipientIds.size }}</strong> 位去重收件人</p><button type="submit" :disabled="saving" class="h-10 min-w-[132px] rounded-lg bg-rose-500 px-5 text-sm font-medium text-white hover:bg-rose-600 disabled:opacity-50"><i :class="saving ? 'fa-spinner fa-spin' : 'fa-paper-plane'" class="fas mr-1.5" aria-hidden="true"></i>立即发送</button></div>
        </form>
      </section>

      <section v-else-if="activeTab === 'templates'" class="grid gap-5 lg:grid-cols-[minmax(240px,0.7fr)_minmax(0,1.3fr)]">
        <div><div class="mb-3 flex items-center justify-between"><h4 class="text-sm font-bold text-gray-800">模板列表</h4><button type="button" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50" title="新建模板" @click="resetTemplateForm"><i class="fas fa-plus" aria-hidden="true"></i></button></div><div class="max-h-[430px] space-y-2 overflow-y-auto"><button v-for="template in templates" :key="template.id" type="button" :class="templateForm.id === template.id ? 'border-rose-300 bg-rose-50' : 'border-gray-200 bg-white hover:bg-gray-50'" class="w-full rounded-lg border p-3 text-left" @click="editTemplate(template)"><span class="block truncate text-sm font-semibold text-gray-800">{{ template.name }}</span><span class="mt-1 block truncate text-xs text-gray-400">{{ template.subject }}</span></button><p v-if="!templates.length" class="rounded-lg border border-dashed border-gray-200 py-12 text-center text-sm text-gray-400">暂无模板</p></div></div>
        <form class="space-y-4" @submit.prevent="saveTemplate"><div class="flex items-center justify-between"><h4 class="text-sm font-bold text-gray-800">{{ templateForm.id ? '编辑模板' : '新建模板' }}</h4><button v-if="templateForm.id" type="button" class="flex h-8 w-8 items-center justify-center rounded-lg text-red-500 hover:bg-red-50" title="删除模板" @click="removeTemplate(templateForm)"><i class="fas fa-trash" aria-hidden="true"></i></button></div><label class="block text-sm font-medium text-gray-700">模板名称<input v-model="templateForm.name" maxlength="100" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></label><label class="block text-sm font-medium text-gray-700">邮件主题<input v-model="templateForm.subject" maxlength="255" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></label><label class="block text-sm font-medium text-gray-700">邮件内容<textarea v-model="templateForm.content" rows="9" class="mt-1.5 w-full resize-y rounded-lg border border-gray-300 px-3 py-2.5 leading-6 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></textarea></label><div class="flex justify-end"><button type="submit" :disabled="saving" class="rounded-lg bg-rose-500 px-5 py-2.5 text-sm font-medium text-white hover:bg-rose-600 disabled:opacity-50">{{ saving ? '保存中' : '保存模板' }}</button></div></form>
      </section>

      <section v-else-if="activeTab === 'groups'" class="grid gap-5 lg:grid-cols-[minmax(240px,0.7fr)_minmax(0,1.3fr)]">
        <div><div class="mb-3 flex items-center justify-between"><h4 class="text-sm font-bold text-gray-800">用户组列表</h4><button type="button" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50" title="新建用户组" @click="resetGroupForm"><i class="fas fa-plus" aria-hidden="true"></i></button></div><div class="max-h-[430px] space-y-2 overflow-y-auto"><button v-for="group in groups" :key="group.id" type="button" :class="groupForm.id === group.id ? 'border-rose-300 bg-rose-50' : 'border-gray-200 bg-white hover:bg-gray-50'" class="w-full rounded-lg border p-3 text-left" @click="editGroup(group)"><span class="flex items-center justify-between gap-2"><span class="truncate text-sm font-semibold text-gray-800">{{ group.name }}</span><span class="shrink-0 text-xs text-gray-400">{{ group.memberCount }} 人</span></span><span class="mt-1 block truncate text-xs text-gray-400">{{ group.description || '无说明' }}</span></button><p v-if="!groups.length" class="rounded-lg border border-dashed border-gray-200 py-12 text-center text-sm text-gray-400">暂无用户组</p></div></div>
        <form class="space-y-4" @submit.prevent="saveGroup"><div class="flex items-center justify-between"><h4 class="text-sm font-bold text-gray-800">{{ groupForm.id ? '编辑用户组' : '新建用户组' }}</h4><button v-if="groupForm.id" type="button" class="flex h-8 w-8 items-center justify-center rounded-lg text-red-500 hover:bg-red-50" title="删除用户组" @click="removeGroup(groupForm)"><i class="fas fa-trash" aria-hidden="true"></i></button></div><div class="grid gap-3 sm:grid-cols-2"><label class="block text-sm font-medium text-gray-700">用户组名称<input v-model="groupForm.name" maxlength="100" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></label><label class="block text-sm font-medium text-gray-700">说明<input v-model="groupForm.description" maxlength="500" class="mt-1.5 w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"></label></div><div><div class="mb-2 flex items-center justify-between"><span class="text-sm font-medium text-gray-700">组成员</span><span class="text-xs text-gray-400">已选 {{ groupForm.userIds.length }} 人</span></div><div class="grid max-h-72 gap-1 overflow-y-auto rounded-lg border border-gray-200 p-2 sm:grid-cols-2"><label v-for="user in users" :key="user.id" class="flex cursor-pointer items-center gap-2 rounded-md px-2 py-2 hover:bg-gray-50"><input type="checkbox" :checked="groupForm.userIds.includes(user.id)" class="h-4 w-4 rounded border-gray-300 text-rose-500 focus:ring-rose-200" @change="toggleId(groupForm.userIds, user.id)"><span class="min-w-0"><span class="block truncate text-sm text-gray-800">{{ user.name || user.username }}</span><span class="block truncate text-xs text-gray-400">{{ user.username }}</span></span></label></div></div><div class="flex justify-end"><button type="submit" :disabled="saving" class="rounded-lg bg-rose-500 px-5 py-2.5 text-sm font-medium text-white hover:bg-rose-600 disabled:opacity-50">{{ saving ? '保存中' : '保存用户组' }}</button></div></form>
      </section>

      <section v-else class="grid gap-5 lg:grid-cols-[minmax(0,1.25fr)_minmax(280px,0.75fr)]">
        <div><div class="mb-2 flex justify-end"><button type="button" class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50" title="刷新发送记录" @click="refreshTasks"><i class="fas fa-arrows-rotate" aria-hidden="true"></i></button></div><div class="overflow-x-auto rounded-lg border border-gray-200"><table class="w-full min-w-[680px] text-left text-sm"><thead class="bg-gray-50 text-xs text-gray-500"><tr><th class="px-3 py-3 font-medium">主题</th><th class="px-3 py-3 font-medium">状态</th><th class="px-3 py-3 font-medium">结果</th><th class="px-3 py-3 font-medium">发送时间</th><th class="w-20 px-3 py-3"></th></tr></thead><tbody class="divide-y divide-gray-100"><tr v-for="task in tasks" :key="task.id" :class="selectedTask?.id === task.id ? 'bg-rose-50/60' : 'hover:bg-gray-50'"><td class="max-w-[240px] px-3 py-3"><button type="button" class="block w-full truncate text-left font-medium text-gray-800" @click="openTask(task)">{{ task.subject }}</button><span class="text-xs text-gray-400">{{ formatDateTime(task.createdAt) }}</span></td><td class="px-3 py-3"><span :class="statusClass(task.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">{{ statusText(task.status) }}</span></td><td class="px-3 py-3 text-xs"><span class="text-emerald-600">{{ task.successCount }}</span> / <span class="text-red-500">{{ task.failureCount }}</span> / {{ task.totalCount }}</td><td class="px-3 py-3 text-xs text-gray-500">{{ formatDateTime(task.scheduledAt || task.startedAt) }}</td><td class="px-3 py-3 text-right"><button v-if="task.status === 'PENDING'" type="button" class="rounded-lg px-2 py-1.5 text-xs text-red-500 hover:bg-red-50" @click="cancelTask(task)">取消</button><button v-else type="button" class="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100" title="查看收件人" @click="openTask(task)"><i class="fas fa-chevron-right" aria-hidden="true"></i></button></td></tr><tr v-if="!tasks.length"><td colspan="5" class="py-16 text-center text-gray-400">暂无发送记录</td></tr></tbody></table></div></div>
        <aside class="rounded-lg border border-gray-200 p-4"><h4 class="text-sm font-bold text-gray-800">收件人明细</h4><p v-if="!selectedTask" class="py-20 text-center text-sm text-gray-400">选择一条发送记录查看</p><div v-else><p class="mt-1 truncate text-xs text-gray-400">{{ selectedTask.subject }}</p><div v-if="recipientsLoading" class="py-20 text-center text-rose-500"><i class="fas fa-spinner fa-spin" aria-hidden="true"></i></div><div v-else class="mt-3 max-h-[390px] space-y-2 overflow-y-auto"><div v-for="recipient in recipients" :key="recipient.id" class="rounded-lg bg-gray-50 p-3"><div class="flex items-start justify-between gap-2"><span class="min-w-0"><span class="block truncate text-sm font-medium text-gray-800">{{ recipient.name || recipient.username }}</span><span class="block truncate text-xs text-gray-400">{{ recipient.email }}</span></span><span :class="statusClass(recipient.status)" class="shrink-0 rounded-full px-2 py-1 text-[11px]">{{ statusText(recipient.status) }}</span></div><p v-if="recipient.errorMessage" class="mt-2 break-words text-xs leading-5 text-red-500">{{ recipient.errorMessage }}</p></div></div></div></aside>
      </section>
    </div>
    <template #footer><div class="flex justify-end"><button type="button" :disabled="saving" class="rounded-lg border border-gray-300 px-5 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50" @click="emit('close')">关闭</button></div></template>
  </BaseModal>
</template>
