// 个人资料编辑模态框组件
Vue.component('user-profile-modal', {
    props: ['visible', 'userInfo'],
    data() {
        return {
            editForm: {
                name: '',
                email: '',
                signStartTime: '19:00',
                signEndTime: '22:00'
            },
            signDaysConfig: [],
            signDayOptions: [
                { value: 0, label: '周日' },
                { value: 1, label: '周一' },
                { value: 2, label: '周二' },
                { value: 3, label: '周三' },
                { value: 4, label: '周四' },
                { value: 5, label: '周五' },
                { value: 6, label: '周六' }
            ],
            signDayPresets: [
                { label: '每天', value: [0, 1, 2, 3, 4, 5, 6] },
                { label: '在校时间', value: [0, 1, 2, 3, 4] },
                { label: '仅周日', value: [0] }
            ]
        };
    },
    watch: {
        visible(newVal) {
            if (newVal) {
                this.editForm.name = this.userInfo.name || '';
                this.editForm.email = this.userInfo.email || '';
                this.editForm.signStartTime = this.userInfo.signStartTime || '19:00';
                this.editForm.signEndTime = this.userInfo.signEndTime || '22:00';
                this.initSignDays();
            }
        }
    },
    methods: {
        initSignDays() {
            if (this.userInfo.signDays) {
                this.signDaysConfig = this.userInfo.signDays.split(',').map(Number).filter(n => !isNaN(n));
            } else {
                this.signDaysConfig = [0, 1, 2, 3, 4, 5, 6];
            }
        },
        toggleSignDay(day) {
            const index = this.signDaysConfig.indexOf(day);
            if (index > -1) {
                this.signDaysConfig.splice(index, 1);
            } else {
                this.signDaysConfig.push(day);
            }
            this.signDaysConfig.sort((a, b) => a - b);
        },
        applyPreset(preset) {
            this.signDaysConfig = [...preset.value];
        },
        isPresetActive(preset) {
            return preset.value.length === this.signDaysConfig.length &&
                   preset.value.every(v => this.signDaysConfig.includes(v));
        },
        close() {
            this.$emit('close');
        },
        handleSubmit() {
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
            this.$emit('save', {
                name: this.editForm.name,
                email: this.editForm.email,
                signDays: this.signDaysConfig.join(','),
                signStartTime: this.editForm.signStartTime,
                signEndTime: this.editForm.signEndTime
            });
        }
    },
    template: `
        <div :class="{'active': visible}" class="modal-overlay" @click.self="close">
            <div class="modal-box w-full max-w-md mx-4">
                <div class="bg-gradient-to-r from-pink-400 to-rose-400 text-white p-6 rounded-t-lg">
                    <div class="flex justify-between items-center">
                        <h3 class="text-xl font-bold">修改个人信息</h3>
                        <button @click="close" class="text-white hover:text-pink-200 text-xl">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>

                <div class="p-6">
                    <form @submit.prevent="handleSubmit" class="space-y-4">
                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">姓名</label>
                            <input v-model="editForm.name" type="text" placeholder="请输入姓名"
                                   class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                        </div>

                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">邮箱</label>
                            <input v-model="editForm.email" type="email" placeholder="请输入邮箱"
                                   class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                            <p class="text-xs text-pink-400 mt-1">用于接收签到成功/失败通知</p>
                        </div>

                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">签到时间范围</label>
                            <div class="flex items-center gap-2">
                                <input v-model="editForm.signStartTime" type="time" min="18:30" step="60"
                                       class="flex-1 px-3 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none text-sm">
                                <span class="text-pink-400">至</span>
                                <input v-model="editForm.signEndTime" type="time" max="23:59" step="60"
                                       class="flex-1 px-3 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none text-sm">
                            </div>
                        </div>

                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">签到日期</label>
                            <div class="flex flex-wrap gap-2 mb-2">
                                <button v-for="preset in signDayPresets" :key="preset.label"
                                        type="button"
                                        @click="applyPreset(preset)"
                                        :class="isPresetActive(preset) ? 'bg-pink-400 text-white' : 'bg-pink-50 text-pink-600 border border-pink-200'"
                                        class="px-3 py-1 rounded-full text-sm font-medium hover:bg-pink-100 transition-colors">
                                    {{ preset.label }}
                                </button>
                            </div>
                            <div class="flex flex-wrap gap-2">
                                <button v-for="day in signDayOptions" :key="day.value"
                                        type="button"
                                        @click="toggleSignDay(day.value)"
                                        :class="signDaysConfig.includes(day.value) ? 'bg-pink-400 text-white border-pink-400' : 'bg-white text-pink-600 border-pink-200'"
                                        class="px-4 py-2 rounded-lg border text-sm font-medium hover:bg-pink-100 transition-colors">
                                    {{ day.label }}
                                </button>
                            </div>
                        </div>

                        <div class="flex justify-end gap-3 pt-4">
                            <button type="button" @click="close"
                                    class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50 transition-colors">
                                取消
                            </button>
                            <button type="submit"
                                    class="bg-pink-400 text-white px-6 py-2 rounded-lg font-medium hover:bg-pink-500 transition-colors">
                                保存
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `
});
