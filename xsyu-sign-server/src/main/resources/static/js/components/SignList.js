// 签到列表组件
Vue.component('sign-list', {
    props: ['signs', 'isLoading', 'currentPage', 'totalPages', 'pendingCount'],
    data() {
        return {
            searchQuery: '',
            filterStatus: null
        };
    },
    computed: {
        filteredSigns() {
            let filtered = this.signs;

            // 搜索过滤
            if (this.searchQuery) {
                const query = this.searchQuery.toLowerCase();
                filtered = filtered.filter(sign =>
                    (sign.signTitle && sign.signTitle.toLowerCase().includes(query)) ||
                    (sign.signContext && sign.signContext.toLowerCase().includes(query)) ||
                    (sign.college && sign.college.toLowerCase().includes(query))
                );
            }

            // 状态过滤
            if (this.filterStatus !== null) {
                filtered = filtered.filter(sign => {
                    const statusDesc = sign.signStatusDesc || this.getSignStatusDesc(sign);
                    return statusDesc === this.filterStatus;
                });
            }

            return filtered;
        }
    },
    template: /*html*/ `
        <div>
            <!-- 操作栏 -->
            <div class="mb-6 space-y-4">
                <div class="flex flex-wrap items-center gap-3">
                    <button @click="$emit('refresh', 1)" :disabled="isLoading"
                            class="bg-pink-400 text-white px-4 py-2 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center">
                        <i class="fas fa-sync-alt mr-2" :class="{'fa-spin': isLoading}"></i>刷新列表
                    </button>

                    <button @click="$emit('one-key-sign')" :disabled="isLoading"
                            class="bg-rose-400 hover:opacity-90 text-white px-4 py-2 rounded-lg font-medium transition-opacity flex items-center">
                        <i class="fas fa-bolt mr-2"></i>一键签到
                        <span v-if="pendingCount > 0"
                              class="ml-2 bg-white text-rose-400 text-xs px-2 py-0.5 rounded-full">
                            {{ pendingCount }}
                        </span>
                    </button>

                    <div class="flex-1 min-w-[200px]">
                        <div class="relative">
                            <input v-model="searchQuery" type="text" placeholder="搜索签到标题、内容..."
                                   class="w-full px-4 py-2 pl-10 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                            <i class="fas fa-search absolute left-3 top-2.5 text-pink-300"></i>
                        </div>
                    </div>
                </div>

                <!-- 状态筛选 -->
                <div class="flex flex-wrap gap-2">
                    <button @click="filterStatus = null"
                            :class="filterStatus === null ? 'bg-pink-400 text-white' : 'bg-pink-100 text-pink-600'"
                            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors">
                        全部
                    </button>
                    <button @click="filterStatus = '待签到'"
                            :class="filterStatus === '待签到' ? 'bg-fuchsia-400 text-white' : 'bg-pink-100 text-pink-600'"
                            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center">
                        <i class="fas fa-clock mr-1"></i>待签到
                    </button>
                    <button @click="filterStatus = '已签到'"
                            :class="filterStatus === '已签到' ? 'bg-rose-400 text-white' : 'bg-pink-100 text-pink-600'"
                            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center">
                        <i class="fas fa-check mr-1"></i>已签到
                    </button>
                </div>
            </div>

            <!-- 加载中 -->
            <div v-if="isLoading && signs.length === 0" class="flex justify-center items-center py-20">
                <div class="text-center">
                    <i class="fas fa-spinner fa-spin text-pink-400 text-3xl mb-4"></i>
                    <p class="text-pink-600">加载中...</p>
                </div>
            </div>

            <!-- 空数据 -->
            <div v-else-if="filteredSigns.length === 0" class="text-center py-20">
                <i class="fas fa-inbox text-pink-300 text-4xl mb-4"></i>
                <p class="text-pink-500 mb-2">暂无签到记录</p>
                <button @click="$emit('refresh', 1)" class="text-pink-500 hover:text-pink-600 text-sm">
                    <i class="fas fa-redo mr-1"></i>刷新试试
                </button>
            </div>

            <!-- 签到卡片列表 -->
            <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <sign-card v-for="sign in filteredSigns" :key="sign.id"
                           :sign="sign"
                           @view-detail="$emit('view-detail', $event)"
                           @sign-single="$emit('sign-single', $event)">
                </sign-card>
            </div>

            <!-- 分页 -->
            <div v-if="totalPages > 1" class="mt-6 flex justify-center">
                <div class="flex items-center gap-2 bg-white/80 backdrop-blur-sm rounded-lg shadow-sm p-2 border border-pink-200">
                    <button @click="$emit('prev-page')" :disabled="currentPage === 1"
                            class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-pink-100 disabled:opacity-50 text-pink-600">
                        <i class="fas fa-chevron-left"></i>
                    </button>

                    <span class="text-sm text-pink-600 px-2">第 {{ currentPage }} 页 / 共 {{ totalPages }} 页</span>

                    <button @click="$emit('next-page')" :disabled="currentPage === totalPages"
                            class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-pink-100 disabled:opacity-50 text-pink-600">
                        <i class="fas fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        </div>
    `,
    methods: {
        getSignStatusDesc(sign) {
            if (sign.signStatus === 2 && !sign.date) {
                const currentTime = new Date().getTime();
                if (sign.start && sign.end) {
                    if (currentTime < sign.start) return '未开始';
                    if (currentTime > sign.end) return '已过期';
                    return '待签到';
                }
                return '待签到';
            }
            if (sign.date) return '已签到';
            if (sign.signStatus === 1) return '未开始';
            if (sign.signStatus === 3) return '已结束';
            return '未知';
        }
    }
});

// 签到卡片组件
Vue.component('sign-card', {
    props: ['sign'],
    template: /*html*/ `
        <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-4 shadow hover:shadow-md transition-shadow">
            <div class="flex justify-between items-start mb-3">
                <div class="flex-1 mr-2">
                    <div class="flex items-center gap-2 mb-1">
                        <span class="font-semibold text-pink-800 text-sm truncate">{{ sign.signTitle || '未命名签到' }}</span>
                        <span v-if="sign.isRead === 1"
                              class="bg-pink-100 text-pink-700 text-xs px-2 py-0.5 rounded-full">已读</span>
                    </div>
                    <p v-if="sign.signContext" class="text-pink-600 text-sm line-clamp-2">{{ sign.signContext }}</p>
                </div>

                <span :class="getSignStatusClass(sign)"
                      class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium">
                    {{ getSignStatusDesc(sign) }}
                </span>
            </div>

            <!-- 课程信息 -->
            <div class="flex items-center text-sm text-pink-500 mb-3">
                <i class="fas fa-graduation-cap mr-2"></i>
                <span>{{ sign.college || '未设置学院' }}</span>
                <span v-if="sign.major" class="mx-1">·</span>
                <span>{{ sign.major || '' }}</span>
            </div>

            <!-- 时间和位置 -->
            <div class="space-y-2 text-sm">
                <div class="flex items-center text-pink-700">
                    <i class="fas fa-clock mr-2 w-4 text-pink-400"></i>
                    <span class="font-medium">{{ formatDateTime(sign.start) }}</span>
                    <span class="mx-2 text-pink-300">~</span>
                    <span class="font-medium">{{ formatTime(sign.end) }}</span>
                </div>

                <div v-if="sign.area" class="flex items-center text-pink-600">
                    <i class="fas fa-map-marker-alt mr-2 w-4 text-pink-400"></i>
                    <span class="truncate">{{ sign.area }}</span>
                </div>

                <div v-if="sign.createName" class="flex items-center text-pink-600">
                    <i class="fas fa-user-tie mr-2 w-4 text-pink-400"></i>
                    <span>创建: {{ sign.createName }}</span>
                    <span v-if="sign.teacher" class="ml-2">({{ sign.teacher }})</span>
                </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex justify-between items-center mt-4 pt-4 border-t border-pink-100">
                <button @click="$emit('view-detail', sign)"
                        class="text-pink-500 hover:text-pink-600 text-sm font-medium flex items-center">
                    <i class="fas fa-eye mr-1"></i>查看详情
                </button>

                <button v-if="isValidSign(sign)" @click="$emit('sign-single', sign)"
                        class="bg-pink-400 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity flex items-center">
                    <i class="fas fa-check mr-1"></i>立即签到
                </button>
            </div>
        </div>
    `,
    methods: {
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
        isValidSign(sign) {
            // signStatus: 1=未签到(待签到), 2=已签到
            // 只有未签到且未过期才能签到
            if (sign.signStatus !== 1 || sign.date) return false;
            const currentTime = new Date().getTime();
            if (sign.start && sign.end) {
                return currentTime >= sign.start && currentTime <= sign.end;
            }
            return true;
        },
        formatTime(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            return `${hours}:${minutes}`;
        },
        formatDateTime(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const day = date.getDate().toString().padStart(2, '0');
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            return `${month}月${day}日 ${hours}:${minutes}`;
        },
        formatDate(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const day = date.getDate().toString().padStart(2, '0');
            return `${month}月${day}日`;
        }
    }
});
