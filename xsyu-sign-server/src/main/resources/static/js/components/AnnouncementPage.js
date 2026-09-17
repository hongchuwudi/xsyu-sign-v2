// 公告管理页面组件
Vue.component('announcement-page', {
    props: ['isLoading'],
    data() {
        return {
            announcements: [],
            showEditModal: false,
            editingId: null,
            editForm: {
                title: '',
                content: '',
                appVersion: ''
            }
        };
    },
    template: /*html*/ `
        <div class="min-h-screen flex flex-col bg-gradient-to-br from-pink-50 to-rose-50">
            <header class="bg-gradient-to-r from-pink-400 to-rose-400 text-white border-b border-pink-300 sticky top-0 z-10">
                <div class="container mx-auto px-4">
                    <div class="flex items-center justify-between py-3">
                        <div class="flex items-center space-x-2">
                            <i class="fas fa-bullhorn text-xl"></i>
                            <h1 class="text-lg font-semibold">公告管理</h1>
                        </div>
                        <div class="flex items-center space-x-3">
                            <button @click="$emit('logout')"
                                    class="px-4 py-2 bg-rose-400 hover:bg-rose-500 rounded-lg text-sm font-medium transition-colors">
                                <i class="fas fa-sign-out-alt mr-2"></i>退出登录
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            <main class="flex-1 container mx-auto px-4 py-6">
                <div class="flex justify-between items-center mb-4">
                    <h2 class="text-lg font-semibold text-pink-800">
                        <i class="fas fa-list mr-2"></i>公告列表
                    </h2>
                    <button @click="openAddModal"
                            class="px-4 py-2 bg-pink-400 text-white rounded-lg hover:bg-pink-500 transition-colors flex items-center">
                        <i class="fas fa-plus mr-2"></i>新增公告
                    </button>
                </div>

                <div v-if="announcements.length === 0" class="text-center py-20">
                    <i class="fas fa-bullhorn text-pink-300 text-4xl mb-4"></i>
                    <p class="text-pink-500">暂无公告</p>
                </div>

                <div class="space-y-3">
                    <div v-for="ann in announcements" :key="ann.id"
                         class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-4 shadow-sm">
                        <div class="flex items-start justify-between">
                            <div class="flex-1 min-w-0">
                                <h3 class="font-semibold text-pink-800">{{ ann.title }}</h3>
                                <div class="flex items-center gap-4 mt-1 text-xs text-pink-400">
                                    <span><i class="far fa-clock mr-1"></i>{{ formatDate(ann.createdAt) }}</span>
                                    <span v-if="ann.appVersion"><i class="fas fa-code-branch mr-1"></i>{{ ann.appVersion }}</span>
                                </div>
                                <p class="text-sm text-pink-500 mt-2 line-clamp-2">{{ ann.content ? ann.content.substring(0, 100) : '' }}</p>
                            </div>
                            <div class="flex items-center gap-2 ml-4 flex-shrink-0">
                                <button @click="openEditModal(ann)"
                                        class="w-8 h-8 rounded-full bg-pink-300 hover:bg-pink-400 text-white flex items-center justify-center"
                                        title="编辑">
                                    <i class="fas fa-edit text-xs"></i>
                                </button>
                                <button @click="deleteAnnouncement(ann)"
                                        class="w-8 h-8 rounded-full bg-rose-400 hover:bg-rose-500 text-white flex items-center justify-center"
                                        title="删除">
                                    <i class="fas fa-trash-alt text-xs"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            <!-- 编辑弹窗 -->
            <div v-if="showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeEditModal">
                <div class="bg-white rounded-xl w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto border border-pink-200">
                    <h3 class="text-lg font-semibold text-pink-800 mb-4">{{ editingId ? '编辑公告' : '新增公告' }}</h3>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">标题 <span class="text-red-500">*</span></label>
                            <input v-model="editForm.title" type="text" placeholder="请输入公告标题"
                                   class="w-full px-4 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">内容 (支持Markdown)</label>
                            <textarea v-model="editForm.content" rows="8" placeholder="请输入公告内容"
                                      class="w-full px-4 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none resize-none"></textarea>
                        </div>
                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">软件版本</label>
                            <input v-model="editForm.appVersion" type="text" placeholder="如 v1.0.0"
                                   class="w-full px-4 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                        </div>
                    </div>
                    <div class="flex justify-end gap-4 mt-6">
                        <button @click="closeEditModal" class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50">
                            取消
                        </button>
                        <button @click="saveAnnouncement" class="px-6 py-2 bg-pink-400 text-white rounded-lg hover:bg-pink-500">
                            保存
                        </button>
                    </div>
                </div>
            </div>

            <!-- 底部导航栏 -->
            <nav class="bg-white/90 backdrop-blur-sm border-t border-pink-200 sticky bottom-0 z-10">
                <div class="container mx-auto px-4">
                    <div class="flex items-center justify-around py-3">
                        <button @click="$emit('go-to-users')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors text-pink-400 hover:bg-pink-50">
                            <i class="fas fa-home text-xl mb-1"></i>
                            <span class="text-xs font-medium">首页</span>
                        </button>
                        <button @click="$emit('go-to-task-config')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors text-pink-400 hover:bg-pink-50">
                            <i class="fas fa-cog text-xl mb-1"></i>
                            <span class="text-xs font-medium">Task配置</span>
                        </button>
                        <button @click="$emit('go-to-redis-queue')"
                                class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors text-pink-400 hover:bg-pink-50">
                            <i class="fas fa-list-ol text-xl mb-1"></i>
                            <span class="text-xs font-medium">Redis队列</span>
                        </button>
                        <button class="flex flex-col items-center px-4 py-2 rounded-lg transition-colors text-pink-500 bg-pink-50">
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
        </div>
    `,
    methods: {
        formatDate(d) {
            if (!d) return '';
            const t = new Date(d);
            return `${t.getFullYear()}-${String(t.getMonth()+1).padStart(2,'0')}-${String(t.getDate()).padStart(2,'0')} ${String(t.getHours()).padStart(2,'0')}:${String(t.getMinutes()).padStart(2,'0')}`;
        },
        async loadAnnouncements() {
            try {
                const response = await api.getAnnouncements(this.$parent.userInfo.jwt);
                if (response.data.code === 200) {
                    this.announcements = response.data.data || [];
                }
            } catch (err) {
                console.error('获取公告列表失败:', err);
            }
        },
        openAddModal() {
            this.editingId = null;
            this.editForm = { title: '', content: '', appVersion: '' };
            this.showEditModal = true;
        },
        openEditModal(ann) {
            this.editingId = ann.id;
            this.editForm = {
                title: ann.title || '',
                content: ann.content || '',
                appVersion: ann.appVersion || ''
            };
            this.showEditModal = true;
        },
        closeEditModal() {
            this.showEditModal = false;
            this.editingId = null;
        },
        async saveAnnouncement() {
            if (!this.editForm.title.trim()) {
                alert('请输入公告标题');
                return;
            }
            try {
                if (this.editingId) {
                    await api.updateAnnouncement(this.$parent.userInfo.jwt, this.editingId, this.editForm);
                } else {
                    await api.addAnnouncement(this.$parent.userInfo.jwt, this.editForm);
                }
                this.closeEditModal();
                this.loadAnnouncements();
            } catch (err) {
                console.error('保存公告失败:', err);
                alert('保存失败');
            }
        },
        async deleteAnnouncement(ann) {
            if (!confirm('确定要删除公告：「' + ann.title + '」？')) return;
            try {
                await api.deleteAnnouncement(this.$parent.userInfo.jwt, ann.id);
                this.loadAnnouncements();
            } catch (err) {
                console.error('删除公告失败:', err);
                alert('删除失败');
            }
        }
    },
    mounted() {
        this.loadAnnouncements();
    }
});
