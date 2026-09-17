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
            detailLog: null,
            logTypes: [
                { value: '', label: '全部类型' },
                { value: 'API', label: 'API接口' },
                { value: 'SCHEDULE', label: '定时调度' },
                { value: 'INTERVAL_SIGN', label: '间隔执行签到' },
                { value: 'JWS_REFRESH', label: 'JWS续签' }
            ],
            resultLabels: { SUCCESS: '成功', FAIL: '失败', PARTIAL: '部分成功' },
            resultColors: { SUCCESS: 'text-green-600', FAIL: 'text-red-600', PARTIAL: 'text-yellow-600' },
            resultBg: { SUCCESS: 'bg-green-50 border-green-200', FAIL: 'bg-red-50 border-red-200', PARTIAL: 'bg-yellow-50 border-yellow-200' },
            typeLabels: { API: 'API', SCHEDULE: '调度', INTERVAL_SIGN: '间隔签到', JWS_REFRESH: 'JWS续签' },
            typeColors: { API: 'bg-blue-100 text-blue-700', SCHEDULE: 'bg-purple-100 text-purple-700', INTERVAL_SIGN: 'bg-green-100 text-green-700', JWS_REFRESH: 'bg-yellow-100 text-yellow-700' }
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
            this.detailLog = null;
            this.deleteByIds([id]);
        },

        showDetail(log) {
            this.detailLog = log;
        },

        closeDetail() {
            this.detailLog = null;
        },

        formatTime(t) {
            if (!t) return '-';
            return t.replace('T', ' ');
        },

        formatDetailTime(t) {
            if (!t) return '-';
            const d = new Date(t.replace('T', ' ').replace(' ', 'T') + '+08:00');
            if (isNaN(d.getTime())) return t.replace('T', ' ');
            const month = d.getMonth() + 1;
            const day = d.getDate();
            const hours = String(d.getHours()).padStart(2, '0');
            const minutes = String(d.getMinutes()).padStart(2, '0');
            const seconds = String(d.getSeconds()).padStart(2, '0');
            return `${month}月${day}日 ${hours}:${minutes}:${seconds}`;
        }
    },
    template: /*html*/ `
<div class="min-h-screen bg-gray-50 pb-16">
    <div class="container mx-auto px-2 sm:px-4 py-3 sm:py-4">
        <!-- 头部 -->
        <div class="flex items-center justify-between mb-3">
            <div>
                <h2 class="text-lg sm:text-xl font-bold text-rose-700">
                    <i class="fas fa-history mr-2"></i>操作日志
                </h2>
            </div>
            <button @click="$emit('go-to-users')"
                class="px-3 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg transition-colors text-sm">
                <i class="fas fa-arrow-left mr-1"></i>返回
            </button>
        </div>

        <!-- 筛选区域 -->
        <div class="bg-white rounded-xl shadow-sm border border-pink-200 p-3 mb-3">
            <div class="grid grid-cols-2 gap-2 items-end">
                <div>
                    <label class="block text-xs text-rose-700 font-medium mb-1">开始日期</label>
                    <input v-model="startDate" type="date"
                        class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm focus:ring-2 focus:ring-pink-300 focus:border-pink-400 outline-none">
                </div>
                <div>
                    <label class="block text-xs text-rose-700 font-medium mb-1">结束日期</label>
                    <input v-model="endDate" type="date"
                        class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm focus:ring-2 focus:ring-pink-300 focus:border-pink-400 outline-none">
                </div>
                <div>
                    <label class="block text-xs text-rose-700 font-medium mb-1">日志类型</label>
                    <select v-model="logType"
                        class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-sm focus:ring-2 focus:ring-pink-300 focus:border-pink-400 outline-none">
                        <option v-for="t in logTypes" :value="t.value">{{ t.label }}</option>
                    </select>
                </div>
                <div class="flex space-x-2">
                    <button @click="onSearch"
                        class="flex-1 px-3 py-1.5 bg-pink-500 hover:bg-pink-600 text-white rounded-lg transition-colors text-sm">
                        <i class="fas fa-search mr-1"></i>查询
                    </button>
                    <button @click="deleteSelected" :disabled="selectedIds.length === 0"
                        :class="selectedIds.length === 0 ? 'bg-gray-300 cursor-not-allowed' : 'bg-red-500 hover:bg-red-600'"
                        class="px-3 py-1.5 text-white rounded-lg transition-colors text-sm">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="mt-2 text-xs text-gray-400">
                <i class="fas fa-hand-pointer mr-1"></i>点击卡片查看详情
            </div>
        </div>

        <!-- 日志卡片列表 -->
        <div class="space-y-1.5">
            <div v-if="logs.length === 0" class="text-center py-16 text-gray-400 bg-white rounded-xl">
                <i class="fas fa-inbox text-4xl mb-3 block"></i>暂无日志记录
            </div>

            <div v-for="log in logs" :key="log.id"
                @click="showDetail(log)"
                class="bg-white rounded-lg shadow-sm border border-pink-100 p-2.5 flex items-center active:bg-rose-50/50 cursor-pointer transition-colors">

                <input type="checkbox" :checked="selectedIds.includes(log.id)"
                    @click.stop @change="toggleOne(log.id)"
                    class="rounded border-gray-300 text-pink-500 focus:ring-pink-400 mr-2 flex-shrink-0">

                <span :class="typeColors[log.logType] || 'bg-gray-100 text-gray-600'"
                    class="px-1.5 py-0.5 rounded-full text-xs font-medium flex-shrink-0 mr-2">
                    {{ typeLabels[log.logType] || log.logType }}
                </span>

                <span class="flex-1 text-sm text-gray-800 truncate mr-2">{{ log.operation }}</span>

                <span :class="resultColors[log.result] || 'text-gray-500'" class="flex-shrink-0 text-xs font-medium mr-1">
                    {{ resultLabels[log.result] || log.result }}
                </span>

                <i class="fas fa-chevron-right text-gray-300 text-xs flex-shrink-0"></i>
            </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="flex items-center justify-between px-4 py-2.5 mt-3 bg-white rounded-xl shadow-sm border border-pink-100">
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

    <!-- ========== 详情弹窗 ========== -->
    <div v-if="detailLog" class="fixed inset-0 z-50 flex items-end sm:items-center justify-center"
        @click.self="closeDetail">
        <!-- 遮罩 -->
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>

        <!-- 弹窗内容 -->
        <div class="relative bg-white rounded-t-2xl sm:rounded-2xl w-full sm:max-w-lg sm:w-full max-h-[85vh] overflow-y-auto shadow-2xl animate-slide-up">
            <!-- 拖拽条 -->
            <div class="flex justify-center pt-3 pb-1 sm:hidden">
                <div class="w-10 h-1 bg-gray-300 rounded-full"></div>
            </div>

            <!-- 头部 -->
            <div class="px-4 pb-2 flex items-center justify-between border-b border-gray-100">
                <span :class="typeColors[detailLog.logType] || 'bg-gray-100 text-gray-600'"
                    class="px-2 py-0.5 rounded-full text-xs font-medium">
                    {{ typeLabels[detailLog.logType] || detailLog.logType }}
                </span>
                <span :class="resultColors[detailLog.result] || 'text-gray-500'" class="text-sm font-medium">
                    {{ resultLabels[detailLog.result] || detailLog.result }}
                </span>
                <button @click="closeDetail" class="text-gray-400 hover:text-gray-600 p-1">
                    <i class="fas fa-times text-lg"></i>
                </button>
            </div>

            <!-- 操作名 -->
            <div class="px-4 py-3">
                <h3 class="text-base font-bold text-gray-800">{{ detailLog.operation }}</h3>
                <div class="flex items-center justify-between mt-1.5 text-xs text-gray-400">
                    <span><i class="far fa-user mr-1"></i>{{ detailLog.operator === 'SYSTEM' ? '系统' : detailLog.operator }}</span>
                    <span><i class="far fa-clock mr-1"></i>{{ formatDetailTime(detailLog.createdAt) }}</span>
                </div>
            </div>

            <!-- 详情内容 -->
            <div class="px-4 pb-4">
                <div v-if="detailLog.detail"
                    :class="resultBg[detailLog.result] || 'bg-gray-50 border-gray-100'"
                    class="rounded-xl p-3 border text-sm text-gray-700 whitespace-pre-wrap break-all leading-relaxed">
                    {{ detailLog.detail }}
                </div>
                <div v-else class="rounded-xl p-3 bg-gray-50 text-sm text-gray-400 italic text-center">
                    无详情
                </div>
            </div>

            <!-- 底部操作 -->
            <div class="px-4 pb-4 flex space-x-2">
                <button @click="deleteOne(detailLog.id)"
                    class="flex-1 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl transition-colors text-sm font-medium">
                    <i class="fas fa-trash-alt mr-1.5"></i>删除此条
                </button>
                <button @click="closeDetail"
                    class="flex-1 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-xl transition-colors text-sm font-medium">
                    关闭
                </button>
            </div>
        </div>
    </div>
</div>`
});
