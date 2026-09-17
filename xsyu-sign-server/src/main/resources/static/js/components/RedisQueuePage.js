// Redis 队列信息组件
Vue.component('redis-queue-page', {
    props: ['isLoading'],
    data() {
        return {
            queueInfo: null,
            showClearConfirm: false,
            refreshInterval: null
        };
    },
    mounted() {
        this.loadQueueInfo();
        this.startAutoRefresh();
    },
    beforeDestroy() {
        this.stopAutoRefresh();
    },
    methods: {
        async loadQueueInfo() {
            this.$emit('loading-start');
            try {
                const response = await api.getRedisQueueInfo(this.$parent.userInfo.jwt);
                if (response.data.code === 200) {
                    this.queueInfo = response.data.data;
                } else {
                    this.$emit('show-message', response.data.message || '获取队列信息失败', 'error');
                }
            } catch (err) {
                console.error('获取队列信息错误:', err);
                this.$emit('show-message', '获取队列信息失败', 'error');
            } finally {
                this.$emit('loading-end');
            }
        },

        startAutoRefresh() {
            this.refreshInterval = setInterval(() => {
                this.loadQueueInfo();
            }, 5000);
        },

        stopAutoRefresh() {
            if (this.refreshInterval) {
                clearInterval(this.refreshInterval);
                this.refreshInterval = null;
            }
        },

        openClearConfirm() {
            if (this.queueInfo && this.queueInfo.queueSize > 0) {
                this.showClearConfirm = true;
            }
        },

        closeClearConfirm() {
            this.showClearConfirm = false;
        },

        async confirmClear() {
            this.$emit('loading-start');
            try {
                const response = await api.clearRedisQueue(this.$parent.userInfo.jwt);
                if (response.data.code === 200) {
                    this.$emit('show-message', '队列已清空');
                    this.loadQueueInfo();
                } else {
                    this.$emit('show-message', response.data.message || '清空队列失败', 'error');
                }
            } catch (err) {
                console.error('清空队列错误:', err);
                this.$emit('show-message', '清空队列失败', 'error');
            } finally {
                this.$emit('loading-end');
                this.closeClearConfirm();
            }
        }
    },
    template: /*html*/ `
        <div class="min-h-screen flex flex-col bg-gradient-to-br from-pink-50 to-rose-50">
            <!-- 顶部导航 -->
            <header class="bg-gradient-to-r from-pink-400 to-rose-400 text-white border-b border-pink-300 sticky top-0 z-10">
                <div class="container mx-auto px-4">
                    <div class="flex items-center justify-between py-3">
                        <div class="flex items-center space-x-2">
                            <i class="fas fa-list-ol text-xl"></i>
                            <h1 class="text-lg font-semibold">Redis 队列监控</h1>
                        </div>

                        <div class="flex items-center space-x-3">
                            <button @click="loadQueueInfo" :disabled="isLoading"
                                    class="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg text-sm font-medium transition-colors flex items-center">
                                <i class="fas fa-sync-alt mr-2" :class="{'fa-spin': isLoading}"></i>刷新
                            </button>
                            <button @click="openClearConfirm" :disabled="isLoading || !queueInfo || queueInfo.queueSize === 0"
                                    class="px-4 py-2 bg-rose-400 hover:bg-rose-500 rounded-lg text-sm font-medium transition-colors flex items-center">
                                <i class="fas fa-trash mr-2"></i>清空队列
                            </button>
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
                <!-- 队列统计卡片 -->
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">队列名称</p>
                                <p class="text-xl font-bold text-pink-800">{{ queueInfo?.queueName || '-' }}</p>
                            </div>
                            <div class="w-14 h-14 bg-pink-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-database text-pink-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">任务数量</p>
                                <p class="text-3xl font-bold text-rose-400">{{ queueInfo?.queueSize || 0 }}</p>
                            </div>
                            <div class="w-14 h-14 bg-rose-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-tasks text-rose-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-pink-600 text-sm mb-1">自动刷新</p>
                                <p class="text-xl font-bold text-fuchsia-400">5秒</p>
                            </div>
                            <div class="w-14 h-14 bg-fuchsia-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <i class="fas fa-sync text-fuchsia-400 text-2xl"></i>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 任务列表 -->
                <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 shadow-sm">
                    <div class="px-6 py-4 border-b border-pink-200">
                        <h2 class="text-lg font-semibold text-pink-800">等待签到任务</h2>
                    </div>

                    <div v-if="isLoading" class="p-12 text-center">
                        <i class="fas fa-spinner fa-spin text-4xl text-pink-300 mb-4"></i>
                        <p class="text-pink-500">加载中...</p>
                    </div>

                    <div v-else-if="!queueInfo || queueInfo.tasks.length === 0" class="p-12 text-center">
                        <i class="fas fa-inbox text-pink-300 text-4xl mb-4"></i>
                        <p class="text-pink-500">队列为空</p>
                    </div>

                    <div v-else class="overflow-x-auto">
                        <table class="w-full">
                            <thead class="bg-pink-50">
                                <tr>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-pink-600 uppercase tracking-wider">序号</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-pink-600 uppercase tracking-wider">用户名</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-pink-600 uppercase tracking-wider">执行时间</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-pink-600 uppercase tracking-wider">等待时长</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-pink-100">
                                <tr v-for="(task, index) in queueInfo.tasks" :key="task.username"
                                    class="hover:bg-pink-50/50 transition-colors">
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-pink-800">{{ index + 1 }}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-pink-800">{{ task.username }}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-pink-600">{{ task.executeTimeFormatted }}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm">
                                        <span :class="task.waitSeconds < 60 ? 'text-pink-500' : task.waitSeconds < 3600 ? 'text-rose-400' : 'text-fuchsia-500'"
                                              class="font-medium">
                                            {{ task.waitTimeFormatted }}
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
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
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors text-pink-400 hover:bg-pink-50">
                            <i class="fas fa-history text-xl mb-1"></i>
                            <span class="text-xs font-medium">日志</span>
                        </button>
                    </div>
                </div>
            </nav>

            <!-- 清空确认弹窗 -->
            <div v-if="showClearConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeClearConfirm">
                <div class="bg-white/95 backdrop-blur-sm rounded-xl w-full max-w-md mx-4 p-6 border border-rose-200">
                    <div class="text-center mb-6">
                        <div class="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-exclamation-triangle text-rose-400 text-2xl"></i>
                        </div>
                        <h3 class="text-lg font-semibold text-rose-800">确认清空队列？</h3>
                        <p class="text-rose-600 mt-2">队列中有 {{ queueInfo?.queueSize || 0 }} 个任务</p>
                        <p class="text-rose-400 text-sm mt-1">此操作不可恢复！</p>
                    </div>
                    <div class="flex justify-center gap-4">
                        <button @click="closeClearConfirm" class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50">
                            取消
                        </button>
                        <button @click="confirmClear" class="px-6 py-2 bg-rose-400 text-white rounded-lg hover:bg-rose-500">
                            确认清空
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `
});
