// 用户端公告查看弹窗组件
Vue.component('announcement-modal', {
    props: ['visible'],
    data() {
        return {
            announcement: null,
            isLoading: false
        };
    },
    watch: {
        visible(newVal) {
            if (newVal) this.loadLatest();
        }
    },
    methods: {
        formatDate(d) {
            if (!d) return '';
            const t = new Date(d);
            return `${t.getFullYear()}-${String(t.getMonth()+1).padStart(2,'0')}-${String(t.getDate()).padStart(2,'0')} ${String(t.getHours()).padStart(2,'0')}:${String(t.getMinutes()).padStart(2,'0')}`;
        },
        renderMarkdown(md) {
            if (!md) return '';
            return md
                .replace(/### (.+)/g, '<h3 class="text-lg font-semibold mt-3 mb-1">$1</h3>')
                .replace(/## (.+)/g, '<h2 class="text-xl font-bold mt-4 mb-2">$1</h2>')
                .replace(/# (.+)/g, '<h1 class="text-2xl font-bold mt-4 mb-2">$1</h1>')
                .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
                .replace(/- (.+)/g, '<li class="ml-4">$1</li>')
                .replace(/<li/g, '<ul class="list-disc ml-4 my-2"><li')
                .replace(/<\/li>(?!.*<\/li>)/, '</li></ul>')
                .replace(/\n\n/g, '<br><br>')
                .replace(/\n/g, '<br>');
        },
        async loadLatest() {
            this.isLoading = true;
            try {
                const response = await api.getLatestAnnouncement(this.$parent.userInfo.jwt);
                if (response.data.code === 200) this.announcement = response.data.data;
            } catch (err) {
                console.error('获取公告失败:', err);
            } finally {
                this.isLoading = false;
            }
        }
    },
    template: /*html*/ `
        <base-modal :visible="visible" title="系统公告" icon="fas fa-bullhorn"
                    theme="purple" width="max-w-lg" @close="$emit('close')">
            <div v-if="isLoading" class="text-center py-8">
                <i class="fas fa-spinner fa-spin text-pink-400 text-2xl"></i>
            </div>

            <div v-else-if="announcement">
                <h2 class="text-xl font-bold text-gray-800 mb-2">{{ announcement.title }}</h2>
                <div class="flex items-center gap-4 text-xs text-gray-400 mb-4">
                    <span v-if="announcement.appVersion">
                        <i class="fas fa-code-branch mr-1"></i>{{ announcement.appVersion }}
                    </span>
                    <span><i class="far fa-clock mr-1"></i>{{ formatDate(announcement.createdAt) }}</span>
                </div>
                <div class="border-t border-gray-200 pt-4">
                    <div class="prose prose-sm max-w-none text-gray-700" v-html="renderMarkdown(announcement.content)"></div>
                </div>
            </div>

            <div v-else class="text-center py-8">
                <i class="fas fa-inbox text-gray-300 text-3xl mb-3"></i>
                <p class="text-gray-500">暂无公告</p>
            </div>
        </base-modal>
    `
});
