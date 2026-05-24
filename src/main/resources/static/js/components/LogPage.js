// 操作日志管理页面
Vue.component('log-page', {
    props: ['isLoading'],
    data() {
        return {
            logs: [],
            total: 0,
            currentPage: 1,
            pageSize: 20,
            startDate: '',
            endDate: '',
            logType: '',
            selectedIds: [],
            selectAll: false,
            logTypes: [
                { value: '', label: '全部类型' },
                { value: 'API', label: 'API接口' },
                { value: 'SCHEDULE', label: '定时调度' },
                { value: 'INTERVAL_SIGN', label: '间隔执行签到' },
                { value: 'JWS_REFRESH', label: 'JWS续签' }
            ],
            resultLabels: { SUCCESS: '成功', FAIL: '失败', PARTIAL: '部分成功' },
            resultColors: { SUCCESS: 'text-green-600', FAIL: 'text-red-600', PARTIAL: 'text-yellow-600' }
        };
    },
    mounted() {
        this.startDate = this.getTodayStr();
        this.endDate = this.getTodayStr();
        this.loadLogs();
    },
    computed: {
        totalPages() {
            return Math.ceil(this.total / this.pageSize) || 1;
        }
    },
    methods: {
        getTodayStr() {
            const d = new Date();
            return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
        },

        async loadLogs() {
            this.$emit('loading-start');
            try {
                const params = { page: this.currentPage, size: this.pageSize };
                if (this.startDate) params.startDate = this.startDate;
                if (this.endDate) params.endDate = this.endDate;
                if (this.logType) params.logType = this.logType;
                const response = await api.getOperationLogs(this.$parent.userInfo.jwt, params);
                if (response.data.code === 200) {
                    this.logs = response.data.data.records || [];
                    this.total = response.data.data.total || 0;
                    this.selectedIds = [];
                    this.selectAll = false;
                } else {
                    this.$emit('show-message', response.data.message || '获取日志失败', 'error');
                }
            } catch (err) {
                console.error('获取日志错误:', err);
                this.$emit('show-message', '获取日志失败', 'error');
            } finally {
                this.$emit('loading-end');
            }
        },

        onSearch() {
            this.currentPage = 1;
            this.loadLogs();
        },

        onPageChange(page) {
            this.currentPage = page;
            this.loadLogs();
        },

        toggleSelectAll() {
            this.selectAll = !this.selectAll;
            if (this.selectAll) {
                this.selectedIds = this.logs.map(l => l.id);
            } else {
                this.selectedIds = [];
            }
        },

        toggleOne(id) {
            const idx = this.selectedIds.indexOf(id);
            if (idx >= 0) {
                this.selectedIds.splice(idx, 1);
            } else {
                this.selectedIds.push(id);
            }
            this.selectAll = this.logs.length > 0 && this.selectedIds.length === this.logs.length;
        },

        async deleteByIds(ids) {
            if (ids.length === 0) {
                alert('请先选择要删除的日志');
                return;
            }
            if (!confirm(`确定要删除选中的 ${ids.length} 条日志吗？`)) return;
            this.$emit('loading-start');
            try {
                const response = await api.deleteOperationLogs(this.$parent.userInfo.jwt, ids);
                if (response.data.code === 200) {
                    this.$emit('show-message', `已删除 ${ids.length} 条日志`);
                    this.loadLogs();
                } else {
                    this.$emit('show-message', response.data.message || '删除失败', 'error');
                }
            } catch (err) {
                console.error('删除日志错误:', err);
                this.$emit('show-message', '删除失败', 'error');
            } finally {
                this.$emit('loading-end');
            }
        },

        deleteSelected() {
            this.deleteByIds(this.selectedIds);
        },

        deleteOne(id) {
            this.deleteByIds([id]);
        },

        formatTime(t) {
            if (!t) return '-';
            return t.replace('T', ' ');
        },

        truncate(text, max) {
            if (!text) return '-';
            return text.length > max ? text.substring(0, max) + '...' : text;
        }
    },
    template: `
<div class="min-h-screen bg-gray-50 pb-16">
    <div class="container mx-auto px-2 sm:px-4 py-4 sm:py-6">
        <!-- 头部 -->
        <div class="flex items-center justify-between mb-6">
            <div>
                <h2 class="text-2xl font-bold text-rose-700">
                    <i class="fas fa-history mr-2"></i>操作日志
                </h2>
                <p class="text-sm text-gray-500 mt-1">查看和删除系统操作日志</p>
            </div>
            <button @click="$emit('go-to-users')"
                class="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg transition-colors text-sm">
                <i class="fas fa-arrow-left mr-1"></i>返回
            </button>
        </div>

        <!-- 筛选区域 -->
        <div class="bg-white rounded-xl shadow-sm border border-pink-200 p-4 mb-6">
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
                <div>
                    <label class="block text-xs text-rose-700 font-medium mb-1">开始日期</label>
                    <input v-model="startDate" type="date"
                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-pink-300 focus:border-pink-400 outline-none">
                </div>
                <div>
                    <label class="block text-xs text-rose-700 font-medium mb-1">结束日期</label>
                    <input v-model="endDate" type="date"
                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-pink-300 focus:border-pink-400 outline-none">
                </div>
                <div>
                    <label class="block text-xs text-rose-700 font-medium mb-1">日志类型</label>
                    <select v-model="logType"
                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-pink-300 focus:border-pink-400 outline-none">
                        <option v-for="t in logTypes" :value="t.value">{{ t.label }}</option>
                    </select>
                </div>
                <div class="flex space-x-2">
                    <button @click="onSearch"
                        class="flex-1 px-4 py-2 bg-pink-500 hover:bg-pink-600 text-white rounded-lg transition-colors text-sm">
                        <i class="fas fa-search mr-1"></i>查询
                    </button>
                    <button @click="deleteSelected" :disabled="selectedIds.length === 0"
                        :class="selectedIds.length === 0 ? 'bg-gray-300 cursor-not-allowed' : 'bg-red-500 hover:bg-red-600'"
                        class="px-4 py-2 text-white rounded-lg transition-colors text-sm">
                        <i class="fas fa-trash mr-1"></i>删除({{ selectedIds.length }})
                    </button>
                </div>
            </div>
        </div>

        <!-- 日志表格 -->
        <div class="bg-white rounded-xl shadow-sm border border-pink-200 overflow-hidden">
            <div class="overflow-x-auto">
                <table class="w-full text-sm">
                    <thead class="bg-rose-50 text-rose-700">
                        <tr>
                            <th class="px-3 py-3 text-left w-10">
                                <input type="checkbox" :checked="selectAll" @change="toggleSelectAll"
                                    class="rounded border-gray-300 text-pink-500 focus:ring-pink-400">
                            </th>
                            <th class="px-3 py-3 text-left w-24">类型</th>
                            <th class="px-3 py-3 text-left">操作</th>
                            <th class="px-3 py-3 text-left hidden lg:table-cell">详情</th>
                            <th class="px-3 py-3 text-left w-16">结果</th>
                            <th class="px-3 py-3 text-left hidden md:table-cell w-20">操作人</th>
                            <th class="px-3 py-3 text-left hidden md:table-cell w-28">时间</th>
                            <th class="px-3 py-3 text-center w-16">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-if="logs.length === 0">
                            <td colspan="8" class="text-center py-10 text-gray-400">
                                <i class="fas fa-inbox text-3xl mb-2 block"></i>暂无日志记录
                            </td>
                        </tr>
                        <tr v-for="log in logs" :key="log.id" class="border-t border-gray-100 hover:bg-rose-50/30 transition-colors">
                            <td class="px-3 py-3">
                                <input type="checkbox" :checked="selectedIds.includes(log.id)" @change="toggleOne(log.id)"
                                    class="rounded border-gray-300 text-pink-500 focus:ring-pink-400">
                            </td>
                            <td class="px-3 py-3">
                                <span :class="{
                                    'bg-blue-100 text-blue-700': log.logType === 'API',
                                    'bg-purple-100 text-purple-700': log.logType === 'SCHEDULE',
                                    'bg-green-100 text-green-700': log.logType === 'INTERVAL_SIGN',
                                    'bg-yellow-100 text-yellow-700': log.logType === 'JWS_REFRESH'
                                }" class="px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap">
                                    {{ log.logType === 'API' ? 'API' : log.logType === 'SCHEDULE' ? '调度' : log.logType === 'INTERVAL_SIGN' ? '间隔签到' : 'JWS续签' }}
                                </span>
                            </td>
                            <td class="px-3 py-3 font-medium text-gray-800 max-w-[200px] truncate" :title="log.operation">
                                {{ log.operation }}
                            </td>
                            <td class="px-3 py-3 text-gray-500 hidden lg:table-cell max-w-[300px] truncate" :title="log.detail">
                                {{ truncate(log.detail, 60) }}
                            </td>
                            <td class="px-3 py-3">
                                <span :class="resultColors[log.result] || 'text-gray-600'" class="text-xs font-medium">
                                    {{ resultLabels[log.result] || log.result }}
                                </span>
                            </td>
                            <td class="px-3 py-3 text-gray-500 hidden md:table-cell text-xs">{{ log.operator === 'SYSTEM' ? '系统' : truncate(log.operator, 12) }}</td>
                            <td class="px-3 py-3 text-gray-500 hidden md:table-cell text-xs whitespace-nowrap">{{ formatTime(log.createdAt) }}</td>
                            <td class="px-3 py-3 text-center">
                                <button @click="deleteOne(log.id)"
                                    class="text-red-400 hover:text-red-600 transition-colors" title="删除">
                                    <i class="fas fa-trash-alt"></i>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 分页 -->
            <div v-if="total > 0" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/50">
                <span class="text-xs text-gray-500">共 {{ total }} 条</span>
                <div class="flex items-center space-x-2">
                    <button @click="onPageChange(currentPage - 1)" :disabled="currentPage <= 1"
                        :class="currentPage <= 1 ? 'opacity-40 cursor-not-allowed' : 'hover:bg-pink-100'"
                        class="px-3 py-1 rounded text-pink-600 transition-colors text-sm">
                        <i class="fas fa-chevron-left text-xs"></i>
                    </button>
                    <span class="text-sm text-gray-600">{{ currentPage }} / {{ totalPages }}</span>
                    <button @click="onPageChange(currentPage + 1)" :disabled="currentPage >= totalPages"
                        :class="currentPage >= totalPages ? 'opacity-40 cursor-not-allowed' : 'hover:bg-pink-100'"
                        class="px-3 py-1 rounded text-pink-600 transition-colors text-sm">
                        <i class="fas fa-chevron-right text-xs"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>`
});
