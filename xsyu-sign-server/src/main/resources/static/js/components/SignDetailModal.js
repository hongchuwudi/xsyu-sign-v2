// 签到详情模态框组件
Vue.component('sign-detail-modal', {
    props: ['visible', 'sign'],
    methods: {
        handleSign() {
            this.$emit('sign', this.sign);
        },
        getSignStatusDesc(sign) {
            if (sign.date) return '已签到';
            if (sign.signStatus === 2) {
                const currentTime = new Date().getTime();
                if (sign.start && sign.end) {
                    if (currentTime < sign.start) return '未开始';
                    if (currentTime > sign.end) return '已过期';
                }
                return '已过期';
            }
            if (sign.signStatus === 1) {
                const currentTime = new Date().getTime();
                if (sign.start && sign.end) {
                    if (currentTime < sign.start) return '未开始';
                    if (currentTime > sign.end) return '已过期';
                }
                return '待签到';
            }
            if (sign.signStatus === 3) return '已结束';
            return '未知';
        },
        getSignStatusClass(sign) {
            const classMap = {
                '待签到': 'bg-pink-100 text-pink-700',
                '已签到': 'bg-rose-100 text-rose-700',
                '未开始': 'bg-pink-50 text-pink-500',
                '已过期': 'bg-fuchsia-100 text-fuchsia-700',
                '已结束': 'bg-pink-50 text-pink-500'
            };
            return classMap[this.getSignStatusDesc(sign)] || 'bg-pink-50 text-pink-500';
        },
        isValidSign(sign) {
            if (sign.signStatus !== 1 || sign.date) return false;
            const currentTime = new Date().getTime();
            if (sign.start && sign.end) {
                return currentTime >= sign.start && currentTime <= sign.end;
            }
            return true;
        },
        formatDateTime(timestamp) {
            if (!timestamp) return '';
            return new Date(timestamp).toLocaleString('zh-CN', {
                month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
            });
        },
        formatDate(timestamp) {
            if (!timestamp) return '';
            return new Date(timestamp).toISOString().split('T')[0].replace(/-/g, '');
        }
    },
    template: /*html*/ `
        <base-modal :visible="visible"
                    :title="sign.signTitle || '签到详情'"
                    :subtitle="sign.signContext || '无详细内容'"
                    width="max-w-2xl" scrollable
                    @close="$emit('close')">
            <div class="space-y-6">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                        <p class="text-sm text-pink-500 mb-1">签到状态</p>
                        <span :class="getSignStatusClass(sign)"
                              class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium">
                            {{ getSignStatusDesc(sign) }}
                        </span>
                    </div>

                    <div>
                        <p class="text-sm text-pink-500 mb-1">签到时间</p>
                        <p class="font-medium text-pink-800">{{ formatDateTime(sign.start) }} - {{ formatDateTime(sign.end) }}</p>
                    </div>

                    <div>
                        <p class="text-sm text-pink-500 mb-1">签到日期</p>
                        <p class="font-medium text-pink-800">{{ sign.signDay || formatDate(sign.start) }}</p>
                    </div>

                    <div v-if="sign.area">
                        <p class="text-sm text-pink-500 mb-1">签到地点</p>
                        <p class="font-medium text-pink-800">{{ sign.area }}</p>
                    </div>
                </div>

                <div class="bg-pink-50/50 rounded-lg p-4">
                    <h4 class="font-medium text-pink-800 mb-3 flex items-center">
                        <i class="fas fa-user-graduate text-pink-400 mr-2"></i>用户信息
                    </h4>
                    <div class="grid grid-cols-2 gap-3 text-sm">
                        <div>
                            <p class="text-pink-500">姓名</p>
                            <p class="font-medium text-pink-800">{{ sign.name || '未设置' }}</p>
                        </div>
                        <div>
                            <p class="text-pink-500">学号</p>
                            <p class="font-medium text-pink-800">{{ sign.number || '未设置' }}</p>
                        </div>
                        <div>
                            <p class="text-pink-500">学院</p>
                            <p class="font-medium text-pink-800">{{ sign.college || '未设置' }}</p>
                        </div>
                        <div>
                            <p class="text-pink-500">专业</p>
                            <p class="font-medium text-pink-800">{{ sign.major || '未设置' }}</p>
                        </div>
                    </div>
                </div>

                <div v-if="sign.createName" class="bg-pink-50/50 rounded-lg p-4">
                    <h4 class="font-medium text-pink-800 mb-3 flex items-center">
                        <i class="fas fa-chalkboard-teacher text-fuchsia-400 mr-2"></i>创建者信息
                    </h4>
                    <div class="flex items-center">
                        <div class="w-10 h-10 bg-fuchsia-100 rounded-full flex items-center justify-center mr-3">
                            <i class="fas fa-user-tie text-fuchsia-400"></i>
                        </div>
                        <div>
                            <p class="font-medium text-pink-800">{{ sign.createName }}</p>
                            <p v-if="sign.teacher" class="text-sm text-pink-500">{{ sign.teacher }}</p>
                        </div>
                    </div>
                </div>

                <div v-if="isValidSign(sign)" class="flex justify-center">
                    <button @click="handleSign"
                            class="bg-pink-400 text-white px-8 py-3 rounded-lg font-medium hover:bg-pink-500 transition-colors flex items-center">
                        <i class="fas fa-check mr-2"></i>立即签到
                    </button>
                </div>
            </div>
        </base-modal>
    `
});
