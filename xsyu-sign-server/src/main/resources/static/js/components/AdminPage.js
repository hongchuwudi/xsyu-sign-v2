// 管理员页面组件
Vue.component('admin-page', {
    props: ['isLoading', 'users', 'total', 'currentPage', 'stats'],
    data() {
        return {
            searchQuery: '',
            pageSize: 10,
            filter: 'all',
            showDeleteConfirm: false,
            userToDelete: null,
            showRefreshConfirm: false,
            userToRefresh: null,
            showSignConfirm: false,
            userToSign: null,
            showEditModal: false,
            userToEdit: null,
            showDetailModal: false,
            userDetail: null,
            userSigns: [],
            isLoadingSigns: false,
            showAddUserModal: false,
            debounceTimer: null,
            editForm: {
                name: '',
                email: '',
                signDays: ''
            },
            editSignDaysConfig: [],
            editSignDayOptions: [
                { value: 0, label: '周日' },
                { value: 1, label: '周一' },
                { value: 2, label: '周二' },
                { value: 3, label: '周三' },
                { value: 4, label: '周四' },
                { value: 5, label: '周五' },
                { value: 6, label: '周六' }
            ],
            editSignDayPresets: [
                { label: '每天', value: [0, 1, 2, 3, 4, 5, 6] },
                { label: '在校时间', value: [0, 1, 2, 3, 4] },
                { label: '仅周日', value: [0] }
            ],
            addUserForm: {
                username: '',
                password: '',
                name: '',
                email: '',
                autoSign: false,
                signDays: '0,1,2,3,4,5,6'
            },
            addSignDaysConfig: [0, 1, 2, 3, 4, 5, 6]
        };
    },
    computed: {
        totalPages() {
            return Math.ceil(this.total / this.pageSize);
        }
    },
    template: /*html*/ `
        <div class="min-h-screen flex flex-col bg-gradient-to-br from-pink-50 to-rose-50">
            <!-- 顶部导航 -->
            <header class="bg-gradient-to-r from-pink-400 to-rose-400 text-white border-b border-pink-300 sticky top-0 z-10">
                <div class="container mx-auto px-4">
                    <div class="flex items-center justify-between py-3">
                        <div class="flex items-center space-x-2">
                            <i class="fas fa-shield-alt text-xl"></i>
                            <h1 class="text-lg font-semibold">管理员控制台</h1>
                        </div>

                        <div class="flex items-center space-x-3">
                            <button @click="$emit('logout')"
                                    class="px-4 py-2 bg-rose-400 hover:bg-rose-500 rounded-lg text-sm font-medium transition-colors flex items-center">
                                <i class="fas fa-sign-out-alt mr-2"></i>退出登录
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            <!-- 主要内容 -->
            <main class="flex-1 container mx-auto px-4 py-6">
                <!-- 统计卡片 -->
                <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm min-h-[100px]">
                        <div class="flex items-center justify-between h-full">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">总用户数</p>
                                <p class="text-3xl font-bold text-pink-500">{{ total }}</p>
                            </div>
                            <div class="w-14 h-14 bg-pink-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-users text-pink-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm min-h-[100px]">
                        <div class="flex items-center justify-between h-full">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">自动签到开启</p>
                                <p class="text-3xl font-bold text-rose-400">{{ stats.autoSignCount }}</p>
                            </div>
                            <div class="w-14 h-14 bg-rose-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-robot text-rose-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm min-h-[100px]">
                        <div class="flex items-center justify-between h-full">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">JWS 失效</p>
                                <p class="text-3xl font-bold text-rose-500">{{ stats.invalidJwsCount }}</p>
                            </div>
                            <div class="w-14 h-14 bg-rose-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-exclamation-triangle text-rose-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm min-h-[100px]">
                        <div class="flex items-center justify-between h-full">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">今日活跃</p>
                                <p class="text-3xl font-bold text-fuchsia-500">{{ stats.todayActiveCount }}</p>
                            </div>
                            <div class="w-14 h-14 bg-fuchsia-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-chart-line text-fuchsia-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 操作栏 -->
                <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-4 mb-6 shadow-sm">
                    <div class="flex flex-wrap items-center gap-3">
                        <button @click="$emit('refresh-users')" :disabled="isLoading"
                                class="bg-pink-400 text-white px-4 py-2 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center">
                            <i class="fas fa-sync-alt mr-2" :class="{'fa-spin': isLoading}"></i>刷新
                        </button>

                        <button @click="openAddUserModal"
                                class="bg-rose-400 text-white px-4 py-2 rounded-lg font-medium hover:opacity-90 transition-opacity flex items-center">
                            <i class="fas fa-plus mr-2"></i>新增
                        </button>

                        <button @click="$emit('sign-all-users')" :disabled="isLoading"
                                class="bg-fuchsia-400 text-white px-4 py-2 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center">
                            <i class="fas fa-check-double mr-2"></i>全签
                        </button>

                        <div class="flex items-center gap-2">
                            <button @click="setFilter('all')"
                                    :class="filter === 'all' ? 'bg-pink-400 text-white' : 'bg-pink-100 text-pink-600'"
                                    class="px-3 py-2 rounded-lg text-sm font-medium transition-colors">
                                全部
                            </button>
                            <button @click="setFilter('autoSign')"
                                    :class="filter === 'autoSign' ? 'bg-rose-400 text-white' : 'bg-pink-100 text-pink-600'"
                                    class="px-3 py-2 rounded-lg text-sm font-medium transition-colors">
                                自动签到
                            </button>
                            <button @click="setFilter('noJws')"
                                    :class="filter === 'noJws' ? 'bg-fuchsia-400 text-white' : 'bg-pink-100 text-pink-600'"
                                    class="px-3 py-2 rounded-lg text-sm font-medium transition-colors">
                                JWS失效
                            </button>
                        </div>

                        <div class="flex-1 min-w-[200px]">
                            <div class="relative">
                                <input v-model="searchQuery" @input="onSearchInput" type="text" placeholder="搜索用户名、姓名、邮箱..."
                                       class="w-full px-4 py-2 pl-10 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                                <i class="fas fa-search absolute left-3 top-2.5 text-pink-300"></i>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 用户列表 -->
                <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 shadow-sm overflow-hidden">
                    <div class="overflow-x-auto">
                        <table class="w-full">
                            <thead class="bg-pink-50 border-b border-pink-200">
                                <tr>
                                    <th class="px-2 py-3 text-left text-xs font-medium text-pink-700 w-20">姓名</th>
                                    <th class="px-2 py-3 text-center text-xs font-medium text-pink-700 w-16">签到</th>
                                    <th class="px-2 py-3 text-center text-xs font-medium text-pink-700 w-16">JWS</th>
                                    <th class="px-2 py-3 text-center text-xs font-medium text-pink-700 w-20">续签时间</th>
                                    <th class="px-2 py-3 text-center text-xs font-medium text-pink-700">操作</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-pink-100">
                                <tr v-for="user in users" :key="user.id" class="hover:bg-pink-50/50">
                                    <td class="px-2 py-3 text-xs text-pink-600 truncate max-w-[80px]">{{ user.name || '-' }}</td>
                                    <td class="px-2 py-3 text-center">
                                        <div @click="toggleAutoSign(user)"
                                             :class="user.autoSign ? 'bg-pink-400' : 'bg-pink-200'"
                                             class="w-10 h-5 rounded-full relative cursor-pointer transition-colors duration-200">
                                            <div :class="user.autoSign ? 'translate-x-5' : 'translate-x-0'"
                                                 class="w-5 h-5 bg-white rounded-full shadow-md transform transition-transform duration-200"></div>
                                        </div>
                                    </td>
                                    <td class="px-2 py-3 text-center">
                                        <span :class="user.jws ? 'text-pink-500' : 'text-rose-400'" class="text-lg font-bold">
                                            {{ user.jws ? '✓' : '✗' }}
                                        </span>
                                    </td>
                                    <td class="px-2 py-3 text-center text-xs text-pink-600">
                                        {{ formatDateOnly(user.jwsRefreshedAt) || '-' }}
                                    </td>
                                    <td class="px-2 py-3">
                                        <div class="flex items-center justify-center space-x-2">
                                            <button @click="openSignConfirm(user)"
                                                    class="w-8 h-8 rounded-full bg-pink-400 hover:bg-pink-500 text-white flex items-center justify-center shadow-sm"
                                                    title="一键签到">
                                                <i class="fas fa-check text-xs"></i>
                                            </button>
                                            <button @click="openDetailModal(user)"
                                                    class="w-8 h-8 rounded-full bg-rose-400 hover:bg-rose-500 text-white flex items-center justify-center shadow-sm"
                                                    title="查看详情">
                                                <i class="fas fa-eye text-xs"></i>
                                            </button>
                                            <button @click="openRefreshConfirm(user)"
                                                    class="w-8 h-8 rounded-full bg-fuchsia-400 hover:bg-fuchsia-500 text-white flex items-center justify-center shadow-sm"
                                                    title="续签 JWS">
                                                <i class="fas fa-key text-xs"></i>
                                            </button>
                                            <button @click="openEditModal(user)"
                                                    class="w-8 h-8 rounded-full bg-pink-300 hover:bg-pink-400 text-white flex items-center justify-center shadow-sm"
                                                    title="编辑">
                                                <i class="fas fa-edit text-xs"></i>
                                            </button>
                                            <button @click="openDeleteConfirm(user)"
                                                    class="w-8 h-8 rounded-full bg-rose-400 hover:bg-rose-500 text-white flex items-center justify-center shadow-sm"
                                                    title="删除">
                                                <i class="fas fa-trash-alt text-xs"></i>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <!-- 分页 -->
                    <div v-if="totalPages > 1" class="border-t border-pink-200 p-4 flex justify-center">
                        <div class="flex items-center gap-2">
                            <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1"
                                    class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-pink-100 disabled:opacity-50 text-pink-600">
                                <i class="fas fa-chevron-left"></i>
                            </button>
                            <span class="text-sm text-pink-600 px-2">第 {{ currentPage }} 页 / 共 {{ totalPages }} 页</span>
                            <button @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages"
                                    class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-pink-100 disabled:opacity-50 text-pink-600">
                                <i class="fas fa-chevron-right"></i>
                            </button>
                        </div>
                    </div>
                </div>

                <!-- 空数据 -->
                <div v-if="users.length === 0 && !isLoading" class="text-center py-20">
                    <i class="fas fa-inbox text-pink-300 text-4xl mb-4"></i>
                    <p class="text-pink-500">暂无用户数据</p>
                </div>
            </main>

            <!-- 底部导航栏 -->
            <nav class="bg-white/90 backdrop-blur-sm border-t border-pink-200 sticky bottom-0 z-10">
                <div class="container mx-auto px-4">
                    <div class="flex items-center justify-around py-3">
                        <button @click="$emit('go-to-users')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors"
                                :class="$parent.currentPage === 'admin' ? 'text-pink-500 bg-pink-50' : 'text-pink-400 hover:bg-pink-50'">
                            <i class="fas fa-home text-xl mb-1"></i>
                            <span class="text-xs font-medium">首页</span>
                        </button>

                        <button @click="$emit('go-to-task-config')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors"
                                :class="$parent.currentPage === 'task-config' ? 'text-pink-500 bg-pink-50' : 'text-pink-400 hover:bg-pink-50'">
                            <i class="fas fa-cog text-xl mb-1"></i>
                            <span class="text-xs font-medium">Task配置</span>
                        </button>

                        <button @click="$emit('go-to-redis-queue')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors"
                                :class="$parent.currentPage === 'redis-queue' ? 'text-pink-500 bg-pink-50' : 'text-pink-400 hover:bg-pink-50'">
                            <i class="fas fa-list-ol text-xl mb-1"></i>
                            <span class="text-xs font-medium">Redis队列</span>
                        </button>

                        <button @click="$emit('go-to-announcements')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors"
                                :class="$parent.currentPage === 'announcements' ? 'text-pink-500 bg-pink-50' : 'text-pink-400 hover:bg-pink-50'">
                            <i class="fas fa-bullhorn text-xl mb-1"></i>
                            <span class="text-xs font-medium">公告</span>
                        </button>

                        <button @click="$emit('go-to-logs')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors"
                                :class="$parent.currentPage === 'logs' ? 'text-pink-500 bg-pink-50' : 'text-pink-400 hover:bg-pink-50'">
                            <i class="fas fa-history text-xl mb-1"></i>
                            <span class="text-xs font-medium">日志</span>
                        </button>
                    </div>
                </div>
            </nav>

            <!-- 删除确认弹窗 -->
            <div v-if="showDeleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeDeleteConfirm">
                <div class="bg-white rounded-xl w-full max-w-md mx-4 p-6">
                    <div class="text-center mb-6">
                        <div class="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-exclamation-triangle text-rose-400 text-2xl"></i>
                        </div>
                        <h3 class="text-lg font-semibold text-rose-800">确认删除用户？</h3>
                        <p class="text-rose-600 mt-2">用户: {{ userToDelete?.username }}</p>
                        <p class="text-rose-400 text-sm mt-1">此操作不可恢复！</p>
                    </div>
                    <div class="flex justify-center gap-4">
                        <button @click="closeDeleteConfirm" class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50">
                            取消
                        </button>
                        <button @click="confirmDelete" class="px-6 py-2 bg-rose-400 text-white rounded-lg hover:bg-rose-500">
                            确认删除
                        </button>
                    </div>
                </div>
            </div>

            <!-- 续签确认弹窗 -->
            <div v-if="showRefreshConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeRefreshConfirm">
                <div class="bg-white rounded-xl w-full max-w-md mx-4 p-6">
                    <div class="text-center mb-6">
                        <div class="w-16 h-16 bg-fuchsia-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-key text-fuchsia-400 text-2xl"></i>
                        </div>
                        <h3 class="text-lg font-semibold text-fuchsia-800">确认续签 JWS？</h3>
                        <p class="text-fuchsia-600 mt-2">用户: {{ userToRefresh?.username }}</p>
                    </div>
                    <div class="flex justify-center gap-4">
                        <button @click="closeRefreshConfirm" class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50">
                            取消
                        </button>
                        <button @click="confirmRefresh" class="px-6 py-2 bg-fuchsia-400 text-white rounded-lg hover:bg-fuchsia-500">
                            确认续签
                        </button>
                    </div>
                </div>
            </div>

            <!-- 签到确认弹窗 -->
            <div v-if="showSignConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeSignConfirm">
                <div class="bg-white rounded-xl w-full max-w-md mx-4 p-6">
                    <div class="text-center mb-6">
                        <div class="w-16 h-16 bg-pink-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-check text-pink-400 text-2xl"></i>
                        </div>
                        <h3 class="text-lg font-semibold text-pink-800">确认一键签到？</h3>
                        <p class="text-pink-600 mt-2">用户: {{ userToSign?.username }}</p>
                    </div>
                    <div class="flex justify-center gap-4">
                        <button @click="closeSignConfirm" class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50">
                            取消
                        </button>
                        <button @click="confirmSign" class="px-6 py-2 bg-pink-400 text-white rounded-lg hover:bg-pink-500">
                            确认签到
                        </button>
                    </div>
                </div>
            </div>

            <!-- 编辑用户弹窗 -->
            <div v-if="showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeEditModal">
                <div class="bg-white rounded-xl w-full max-w-md mx-4 p-6 max-h-[90vh] overflow-y-auto">
                    <h3 class="text-lg font-semibold text-gray-800 mb-4">编辑用户信息</h3>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">用户名</label>
                            <input :value="userToEdit?.username" disabled
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-100 text-gray-500">
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">姓名</label>
                            <input v-model="editForm.name" type="text" placeholder="请输入姓名"
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">邮箱</label>
                            <input v-model="editForm.email" type="email" placeholder="请输入邮箱"
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">签到时间范围</label>
                            <div class="flex items-center gap-2">
                                <input v-model="editForm.signStartTime" type="time" min="18:30" step="60"
                                       class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none text-sm">
                                <span class="text-gray-400">至</span>
                                <input v-model="editForm.signEndTime" type="time" max="23:59" step="60"
                                       class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none text-sm">
                            </div>
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">签到日期</label>
                            <div class="flex flex-wrap gap-2 mb-2">
                                <button v-for="preset in editSignDayPresets" :key="preset.label"
                                        type="button"
                                        @click="applyEditPreset(preset)"
                                        :class="isEditPresetActive(preset) ? 'bg-purple-500 text-white' : 'bg-purple-50 text-purple-600 border border-purple-200'"
                                        class="px-3 py-1 rounded-full text-sm font-medium hover:bg-purple-100 transition-colors">
                                    {{ preset.label }}
                                </button>
                            </div>
                            <div class="flex flex-wrap gap-2">
                                <button v-for="day in editSignDayOptions" :key="day.value"
                                        type="button"
                                        @click="toggleEditSignDay(day.value)"
                                        :class="editSignDaysConfig.includes(day.value) ? 'bg-purple-500 text-white border-purple-500' : 'bg-white text-purple-600 border-purple-200'"
                                        class="px-4 py-2 rounded-lg border text-sm font-medium hover:bg-purple-50 transition-colors">
                                    {{ day.label }}
                                </button>
                            </div>
                        </div>
                    </div>
                    <div class="flex justify-end gap-4 mt-6">
                        <button @click="closeEditModal" class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
                            取消
                        </button>
                        <button @click="confirmEdit" class="px-6 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600">
                            保存
                        </button>
                    </div>
                </div>
            </div>

            <!-- 用户详情弹窗 -->
            <div v-if="showDetailModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeDetailModal">
                <div class="bg-white rounded-xl w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto">
                    <h3 class="text-lg font-semibold text-gray-800 mb-4">用户详情</h3>

                    <!-- 用户基本信息 -->
                    <div class="space-y-3 mb-6">
                        <div class="flex justify-between py-2 border-b border-gray-100">
                            <span class="text-gray-600">用户名</span>
                            <span class="font-medium text-gray-800">{{ userDetail?.username }}</span>
                        </div>
                        <div class="flex justify-between py-2 border-b border-gray-100">
                            <span class="text-gray-600">姓名</span>
                            <span class="font-medium text-gray-800">{{ userDetail?.name || '-' }}</span>
                        </div>
                        <div class="flex justify-between py-2 border-b border-pink-100">
                            <span class="text-pink-600">邮箱</span>
                            <span class="font-medium text-pink-800">{{ userDetail?.email || '-' }}</span>
                        </div>
                        <div class="flex justify-between py-2 border-b border-pink-100">
                            <span class="text-pink-600">自动签到</span>
                            <span :class="userDetail?.autoSign ? 'text-rose-500' : 'text-pink-500'" class="font-medium">
                                {{ userDetail?.autoSign ? '开启' : '关闭' }}
                            </span>
                        </div>
                        <div class="flex justify-between py-2 border-b border-pink-100">
                            <span class="text-pink-600">JWS 状态</span>
                            <span :class="userDetail?.jws ? 'text-rose-500' : 'text-fuchsia-500'" class="font-medium">
                                {{ userDetail?.jws ? '有效' : '失效' }}
                            </span>
                        </div>
                        <div class="flex justify-between py-2 border-b border-pink-100">
                            <span class="text-fuchsia-600">JWS 续签时间</span>
                            <span class="font-medium text-pink-800">
                                {{ formatDateTime(userDetail?.jwsRefreshedAt) || '暂无' }}
                            </span>
                        </div>
                        <div class="flex justify-between py-2 border-b border-pink-100">
                            <span class="text-pink-600">签到日期</span>
                            <span class="font-medium text-rose-500">{{ formatSignDays(userDetail?.signDays) }}</span>
                        </div>
                    </div>

                    <!-- 最近的签到记录 -->
                    <div class="border-t border-pink-200 pt-4">
                        <h4 class="text-md font-semibold text-pink-700 mb-3">
                            <i class="fas fa-clipboard-check mr-2"></i>最近的签到记录
                            <span v-if="isLoadingSigns" class="text-sm font-normal text-pink-400 ml-2">
                                <i class="fas fa-spinner fa-spin"></i> 加载中...
                            </span>
                        </h4>

                        <div v-if="userSigns.length === 0 && !isLoadingSigns" class="text-center py-4 text-pink-400">
                            <i class="fas fa-inbox text-2xl mb-2"></i>
                            <p class="text-sm">暂无签到记录</p>
                        </div>

                        <div v-else class="space-y-2 max-h-64 overflow-y-auto">
                            <div v-for="(sign, index) in userSigns" :key="sign.id || index"
                                 class="bg-pink-50 rounded-lg p-3 border border-pink-100">
                                <div class="flex justify-between items-start">
                                    <div class="flex-1 min-w-0">
                                        <p class="font-medium text-pink-800 text-sm truncate">{{ sign.signTitle || '未命名签到' }}</p>
                                        <p v-if="sign.signContext" class="text-pink-600 text-xs mt-1 line-clamp-1">{{ sign.signContext }}</p>
                                        <div class="flex items-center gap-3 mt-2 text-xs text-pink-500">
                                            <span v-if="sign.start">
                                                <i class="fas fa-clock mr-1"></i>{{ formatTime(sign.start) }}
                                            </span>
                                            <span v-if="sign.end">
                                                <i class="fas fa-hourglass-end mr-1"></i>{{ formatTime(sign.end) }}
                                            </span>
                                        </div>
                                    </div>
                                    <span :class="getSignStatusClass(sign)" class="ml-2 px-2 py-1 rounded-full text-xs font-medium whitespace-nowrap">
                                        {{ getSignStatusDesc(sign) }}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="flex justify-end mt-6">
                        <button @click="closeDetailModal" class="px-6 py-2 bg-pink-400 text-white rounded-lg hover:bg-pink-500">
                            关闭
                        </button>
                    </div>
                </div>
            </div>

            <!-- 新增用户弹窗 -->
            <div v-if="showAddUserModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeAddUserModal">
                <div class="bg-white rounded-xl w-full max-w-md mx-4 p-6 max-h-[90vh] overflow-y-auto">
                    <h3 class="text-lg font-semibold text-gray-800 mb-4">新增用户</h3>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">用户名 <span class="text-red-500">*</span></label>
                            <input v-model="addUserForm.username" type="text" placeholder="请输入用户名"
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">密码 <span class="text-red-500">*</span></label>
                            <input v-model="addUserForm.password" type="password" placeholder="请输入密码"
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">姓名</label>
                            <input v-model="addUserForm.name" type="text" placeholder="请输入姓名"
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">邮箱</label>
                            <input v-model="addUserForm.email" type="email" placeholder="请输入邮箱"
                                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-2 focus:ring-purple-200 outline-none">
                        </div>
                        <div class="flex items-center gap-2">
                            <input v-model="addUserForm.autoSign" type="checkbox" id="autoSign" class="w-4 h-4 text-purple-600">
                            <label for="autoSign" class="text-gray-700 text-sm">开启自动签到</label>
                        </div>
                        <div>
                            <label class="block text-gray-700 text-sm font-medium mb-2">签到日期</label>
                            <div class="flex flex-wrap gap-2 mb-2">
                                <button v-for="preset in editSignDayPresets" :key="preset.label"
                                        type="button"
                                        @click="applyAddPreset(preset)"
                                        :class="isAddPresetActive(preset) ? 'bg-purple-500 text-white' : 'bg-purple-50 text-purple-600 border border-purple-200'"
                                        class="px-3 py-1 rounded-full text-sm font-medium hover:bg-purple-100 transition-colors">
                                    {{ preset.label }}
                                </button>
                            </div>
                            <div class="flex flex-wrap gap-2">
                                <button v-for="day in editSignDayOptions" :key="day.value"
                                        type="button"
                                        @click="toggleAddSignDay(day.value)"
                                        :class="addSignDaysConfig.includes(day.value) ? 'bg-purple-500 text-white border-purple-500' : 'bg-white text-purple-600 border-purple-200'"
                                        class="px-4 py-2 rounded-lg border text-sm font-medium hover:bg-purple-50 transition-colors">
                                    {{ day.label }}
                                </button>
                            </div>
                        </div>
                    </div>
                    <div class="flex justify-end gap-4 mt-6">
                        <button @click="closeAddUserModal" class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
                            取消
                        </button>
                        <button @click="confirmAddUser" class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
                            确认添加
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `,
    methods: {
        openDeleteConfirm(user) {
            this.userToDelete = user;
            this.showDeleteConfirm = true;
        },
        closeDeleteConfirm() {
            this.showDeleteConfirm = false;
            this.userToDelete = null;
        },
        confirmDelete() {
            this.$emit('delete-user', this.userToDelete);
            this.closeDeleteConfirm();
        },
        openRefreshConfirm(user) {
            this.userToRefresh = user;
            this.showRefreshConfirm = true;
        },
        closeRefreshConfirm() {
            this.showRefreshConfirm = false;
            this.userToRefresh = null;
        },
        confirmRefresh() {
            this.$emit('refresh-jws', this.userToRefresh);
            this.closeRefreshConfirm();
        },
        openEditModal(user) {
            this.userToEdit = user;
            this.editForm = {
                name: user.name || '',
                email: user.email || '',
                signDays: user.signDays || '',
                signStartTime: user.signStartTime || '19:00',
                signEndTime: user.signEndTime || '22:00'
            };
            if (user.signDays) {
                this.editSignDaysConfig = user.signDays.split(',').map(Number).filter(n => !isNaN(n));
            } else {
                this.editSignDaysConfig = [0, 1, 2, 3, 4, 5, 6];
            }
            this.showEditModal = true;
        },
        closeEditModal() {
            this.showEditModal = false;
            this.userToEdit = null;
        },
        confirmEdit() {
            const start = this.editForm.signStartTime;
            const end = this.editForm.signEndTime;
            if (start && start < '18:30') {
                alert('签到开始时间不能早于18:30');
                return;
            }
            if (end && end > '23:59') {
                alert('签到结束时间不能晚于23:59');
                return;
            }
            if (start && end && start >= end) {
                alert('签到开始时间必须早于结束时间');
                return;
            }
            this.editForm.signDays = this.editSignDaysConfig.join(',');
            this.$emit('edit-user', { ...this.userToEdit, ...this.editForm });
            this.closeEditModal();
        },
        toggleEditSignDay(day) {
            const index = this.editSignDaysConfig.indexOf(day);
            if (index > -1) {
                this.editSignDaysConfig.splice(index, 1);
            } else {
                this.editSignDaysConfig.push(day);
            }
            this.editSignDaysConfig.sort((a, b) => a - b);
        },
        applyEditPreset(preset) {
            this.editSignDaysConfig = [...preset.value];
        },
        isEditPresetActive(preset) {
            return preset.value.length === this.editSignDaysConfig.length &&
                   preset.value.every(v => this.editSignDaysConfig.includes(v));
        },
        async openDetailModal(user) {
            this.userDetail = user;
            this.showDetailModal = true;
            this.isLoadingSigns = true;
            this.userSigns = [];

            // 获取用户签到记录
            try {
                // 直接调用API获取签到记录
                const response = await api.getUserSigns(this.$parent.userInfo.jwt, user.username, 10);
                if (response.data.code === 200) {
                    const signs = response.data.data || [];
                    console.log('获取到的签到记录:', signs);
                    this.userSigns = signs;
                } else {
                    console.error('获取签到记录失败:', response.data.message);
                }
            } catch (err) {
                console.error('获取用户签到记录失败:', err);
            } finally {
                this.isLoadingSigns = false;
            }
        },
        closeDetailModal() {
            this.showDetailModal = false;
            this.userDetail = null;
            this.userSigns = [];
        },
        getSignStatusDesc(sign) {
            // 有签到时间 = 真正签到成功
            if (sign.date) return '已签到';

            // signStatus=2 但无 date：签到周期已结束但未成功签到
            if (sign.signStatus === 2) {
                const currentTime = new Date().getTime();
                if (sign.start && sign.end) {
                    if (currentTime < sign.start) return '未开始';
                    if (currentTime > sign.end) return '已过期';
                }
                return '已过期';
            }
            // signStatus=1: 未签到
            if (sign.signStatus === 1) {
                const currentTime = new Date().getTime();
                if (sign.start && sign.end) {
                    if (currentTime < sign.start) return '未开始';
                    if (currentTime > sign.end) return '已过期';
                    return '待签到';
                }
                return '待签到';
            }
            if (sign.signStatus === 3) return '已结束';
            return '未知';
        },
        getSignStatusClass(sign) {
            const status = this.getSignStatusDesc(sign);
            const classMap = {
                '待签到': 'bg-pink-100 text-pink-700',
                '已签到': 'bg-rose-100 text-rose-700',
                '未开始': 'bg-pink-50 text-pink-500',
                '已过期': 'bg-fuchsia-100 text-fuchsia-700',
                '已结束': 'bg-pink-50 text-pink-500'
            };
            return classMap[status] || 'bg-pink-50 text-pink-500';
        },
        formatTime(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const day = date.getDate().toString().padStart(2, '0');
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            return `${month}-${day} ${hours}:${minutes}`;
        },
        openSignConfirm(user) {
            this.userToSign = user;
            this.showSignConfirm = true;
        },
        closeSignConfirm() {
            this.showSignConfirm = false;
            this.userToSign = null;
        },
        confirmSign() {
            if (this.userToSign) {
                this.$emit('sign-user', this.userToSign.username);
                this.closeSignConfirm();
            }
        },
        toggleAutoSign(user) {
            if (!user || !user.username) {
                console.error('toggleAutoSign: user or user.username is undefined', user);
                return;
            }
            this.$emit('toggle-auto-sign', user);
        },
        openAddUserModal() {
            this.showAddUserModal = true;
        },
        closeAddUserModal() {
            this.showAddUserModal = false;
            this.resetAddUserForm();
        },
        resetAddUserForm() {
            this.addUserForm = {
                username: '',
                password: '',
                name: '',
                email: '',
                autoSign: false,
                signDays: '0,1,2,3,4,5,6'
            };
            this.addSignDaysConfig = [0, 1, 2, 3, 4, 5, 6];
        },
        confirmAddUser() {
            if (!this.addUserForm.username || !this.addUserForm.password) {
                alert('用户名和密码为必填项');
                return;
            }
            this.addUserForm.signDays = this.addSignDaysConfig.join(',');
            this.$emit('add-user', { ...this.addUserForm });
            this.closeAddUserModal();
        },
        toggleAddSignDay(day) {
            const index = this.addSignDaysConfig.indexOf(day);
            if (index > -1) {
                this.addSignDaysConfig.splice(index, 1);
            } else {
                this.addSignDaysConfig.push(day);
            }
            this.addSignDaysConfig.sort((a, b) => a - b);
        },
        applyAddPreset(preset) {
            this.addSignDaysConfig = [...preset.value];
        },
        isAddPresetActive(preset) {
            return preset.value.length === this.addSignDaysConfig.length &&
                   preset.value.every(v => this.addSignDaysConfig.includes(v));
        },
        formatDateTime(dt) {
            if (!dt) return '';
            const d = new Date(dt);
            const pad = n => String(n).padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
        },
        formatDateOnly(dt) {
            if (!dt) return '';
            const d = new Date(dt);
            const pad = n => String(n).padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
        },
        formatSignDays(signDays) {
            if (!signDays) return '每天';
            const dayMap = { '0': '周日', '1': '周一', '2': '周二', '3': '周三', '4': '周四', '5': '周五', '6': '周六' };
            const days = signDays.split(',').map(d => dayMap[d.trim()]).filter(Boolean);
            if (days.length === 7) return '每天';
            if (days.length === 5 && signDays === '0,1,2,3,4') return '在校时间';
            if (days.length === 1 && signDays === '0') return '仅周日';
            return days.join('、');
        },
        setFilter(filter) {
            this.filter = filter;
            this.$emit('change-filter', filter);
        },
        changePage(page) {
            if (page >= 1 && page <= this.totalPages) {
                this.$emit('change-page', page);
            }
        },
        onSearchInput(event) {
            const query = event.target.value;
            this.searchQuery = query;

            if (this.debounceTimer) {
                clearTimeout(this.debounceTimer);
            }

            this.debounceTimer = setTimeout(() => {
                this.$emit('search', query);
            }, 500);
        }
    }
});
