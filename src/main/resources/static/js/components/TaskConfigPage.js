// 定时任务配置页面组件
Vue.component('task-config-page', {
    props: ['isLoading', 'taskConfigs'],
    data() {
        return {
            showEditModal: false,
            showScheduleModal: false,
            editingTask: null,
            editForm: {
                // 调度任务
                scheduleHour: 18,
                scheduleMinute: 31,
                delayRange: 30,
                // 间隔执行
                intervalStartHour: 18,
                intervalStartMinute: 0,
                intervalEndHour: 20,
                intervalEndMinute: 0,
                intervalMinutes: 1,
                // JWS刷新
                jwsHour: 18,
                jwsMinute: 0,
                jwsStartDate: '',
                jwsIntervalWeeks: 1,
                // 通用
                enabled: true
            },
            // 日历数据（用对象替代Set，Vue 2才能响应）
            calendarYear: new Date().getFullYear(),
            scheduleDates: {},       // { '2026-03-02': true, ... }
            holidayDates: {},        // 法定节假日
            skippedDates: {},        // 跳过日期（假期+寒暑假+周五周六）
            autoSelectedDates: {},   // 自动推荐日期
            currentMonth: 0,         // 当前显示的月份 (0-11)
            // JWS间隔选项
            jwsIntervalOptions: [
                { value: 1, label: '每周' },
                { value: 2, label: '每2周' },
                { value: 3, label: '每3周' },
                { value: 4, label: '每4周' }
            ],
            // 延迟范围选项
            delayRangeOptions: [
                { value: 30, label: '30分钟' },
                { value: 45, label: '45分钟' },
                { value: 60, label: '60分钟' }
            ]
        };
    },
    computed: {
        scheduleTask() {
            return this.taskConfigs.find(t => t.taskKey === 'schedule_users');
        },
        intervalTask() {
            return this.taskConfigs.find(t => t.taskKey === 'interval_sign');
        },
        jwsTask() {
            return this.taskConfigs.find(t => t.taskKey === 'refresh_jws');
        },
        // 生成当前月份的日历格子
        calendarDays() {
            const year = this.calendarYear;
            const month = this.currentMonth;
            const firstDay = new Date(year, month, 1);
            const lastDay = new Date(year, month + 1, 0);
            const startDow = firstDay.getDay(); // 0=周日
            const daysInMonth = lastDay.getDate();

            const days = [];
            // 填充前面的空白
            for (let i = 0; i < startDow; i++) {
                days.push({ dayOfMonth: '', isCurrentMonth: false, dateStr: '' });
            }
            // 填充本月日期
            for (let d = 1; d <= daysInMonth; d++) {
                const date = new Date(year, month, d);
                const ds = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
                days.push({
                    dayOfMonth: d,
                    dateStr: ds,
                    isCurrentMonth: true,
                    isSelected: !!this.scheduleDates[ds],
                    isHoliday: !!this.holidayDates[ds],
                    isSkipped: !!this.skippedDates[ds],
                    isAuto: !!this.autoSelectedDates[ds]
                });
            }
            return days;
        },
        // 当前月份名称
        currentMonthLabel() {
            return `${this.calendarYear}年 ${this.currentMonth + 1}月`;
        },
        // 已选日期统计
        selectedCount() {
            return Object.keys(this.scheduleDates).length;
        },
        skippedCount() {
            return Object.keys(this.skippedDates).length;
        }
    },
    template: `
        <div class="min-h-screen flex flex-col bg-gradient-to-br from-pink-50 to-rose-50">
            <!-- 顶部导航 -->
            <header class="bg-gradient-to-r from-pink-400 to-rose-400 text-white border-b border-pink-300 sticky top-0 z-10">
                <div class="container mx-auto px-4">
                    <div class="flex items-center justify-between py-3">
                        <div class="flex items-center space-x-2">
                            <i class="fas fa-clock text-xl"></i>
                            <h1 class="text-lg font-semibold">定时任务配置</h1>
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
                <div class="grid grid-cols-1 gap-4">
                    <!-- 调度所有用户 -->
                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm">
                        <div class="flex items-center justify-between mb-4">
                            <div class="flex items-center space-x-3">
                                <div class="w-12 h-12 bg-pink-100 rounded-full flex items-center justify-center">
                                    <i class="fas fa-users text-pink-400 text-xl"></i>
                                </div>
                                <div>
                                    <h3 class="text-lg font-semibold text-pink-800">调度所有用户</h3>
                                    <p class="text-sm text-pink-500">按日历调度签到（自动跳过寒暑假和节假日）</p>
                                </div>
                            </div>
                            <span :class="scheduleTask?.enabled ? 'bg-pink-100 text-pink-700' : 'bg-pink-50 text-pink-400'"
                                  class="px-3 py-1 rounded-full text-sm font-medium">
                                {{ scheduleTask?.enabled ? '已启用' : '已禁用' }}
                            </span>
                        </div>

                        <div class="bg-pink-50/50 rounded-lg p-4 mb-4">
                            <div class="grid grid-cols-3 gap-4">
                                <div>
                                    <p class="text-xs text-pink-500 mb-1">执行时间</p>
                                    <p class="font-medium text-pink-700">{{ formatTime(scheduleTask?.parsedCron?.hour, scheduleTask?.parsedCron?.minute) }}</p>
                                </div>
                                <div>
                                    <p class="text-xs text-pink-500 mb-1">调度模式</p>
                                    <p class="font-medium text-pink-700">年历调度</p>
                                </div>
                                <div>
                                    <p class="text-xs text-pink-500 mb-1">已选日期</p>
                                    <p class="font-medium text-pink-700">{{ scheduleTask?.scheduleConfig?.scheduleDates?.length || 0 }} 天</p>
                                </div>
                            </div>
                        </div>

                        <div class="flex space-x-3">
                            <button @click="openEditModal('schedule_users')"
                                    class="flex-1 py-2.5 bg-pink-400 text-white rounded-lg hover:bg-pink-500 transition-colors flex items-center justify-center font-medium">
                                <i class="fas fa-edit mr-2"></i>修改配置
                            </button>
                            <button @click="triggerImmediateSchedule"
                                    :disabled="isLoading"
                                    class="flex-1 py-2.5 bg-rose-400 text-white rounded-lg hover:bg-rose-500 transition-colors flex items-center justify-center font-medium disabled:opacity-50 disabled:cursor-not-allowed">
                                <i class="fas fa-play mr-2"></i>立即调度
                            </button>
                        </div>
                    </div>

                    <!-- 间隔执行签到 -->
                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm">
                        <div class="flex items-center justify-between mb-4">
                            <div class="flex items-center space-x-3">
                                <div class="w-12 h-12 bg-rose-100 rounded-full flex items-center justify-center">
                                    <i class="fas fa-sync-alt text-rose-400 text-xl"></i>
                                </div>
                                <div>
                                    <h3 class="text-lg font-semibold text-pink-800">间隔执行签到</h3>
                                    <p class="text-sm text-pink-500">设置签到检查的时间范围</p>
                                </div>
                            </div>
                            <span :class="intervalTask?.enabled ? 'bg-rose-100 text-rose-700' : 'bg-rose-50 text-rose-400'"
                                  class="px-3 py-1 rounded-full text-sm font-medium">
                                {{ intervalTask?.enabled ? '已启用' : '已禁用' }}
                            </span>
                        </div>

                        <div class="bg-rose-50/50 rounded-lg p-4 mb-4">
                            <div class="grid grid-cols-3 gap-4">
                                <div>
                                    <p class="text-xs text-rose-500 mb-1">开始时间</p>
                                    <p class="font-medium text-rose-700">{{ formatTime(intervalTask?.parsedCron?.startHour, intervalTask?.parsedCron?.startMinute || 0) }}</p>
                                </div>
                                <div>
                                    <p class="text-xs text-rose-500 mb-1">结束时间</p>
                                    <p class="font-medium text-rose-700">{{ formatTime(intervalTask?.parsedCron?.endHour, intervalTask?.parsedCron?.endMinute || 0) }}</p>
                                </div>
                                <div>
                                    <p class="text-xs text-rose-500 mb-1">间隔</p>
                                    <p class="font-medium text-rose-700">每 {{ intervalTask?.parsedCron?.interval }} 分钟</p>
                                </div>
                            </div>
                        </div>

                        <button @click="openEditModal('interval_sign')"
                                class="w-full py-2.5 bg-rose-400 text-white rounded-lg hover:bg-rose-500 transition-colors flex items-center justify-center font-medium">
                            <i class="fas fa-edit mr-2"></i>修改配置
                        </button>
                    </div>

                    <!-- JWS续签 -->
                    <div class="bg-white/80 backdrop-blur-sm rounded-xl border border-pink-200 p-5 shadow-sm">
                        <div class="flex items-center justify-between mb-4">
                            <div class="flex items-center space-x-3">
                                <div class="w-12 h-12 bg-fuchsia-100 rounded-full flex items-center justify-center">
                                    <i class="fas fa-key text-fuchsia-400 text-xl"></i>
                                </div>
                                <div>
                                    <h3 class="text-lg font-semibold text-pink-800">JWS续签</h3>
                                    <p class="text-sm text-pink-500">按间隔自动续签JWS</p>
                                </div>
                            </div>
                            <span :class="jwsTask?.enabled ? 'bg-fuchsia-100 text-fuchsia-700' : 'bg-fuchsia-50 text-fuchsia-400'"
                                  class="px-3 py-1 rounded-full text-sm font-medium">
                                {{ jwsTask?.enabled ? '已启用' : '已禁用' }}
                            </span>
                        </div>

                        <div class="bg-fuchsia-50/50 rounded-lg p-4 mb-4">
                            <div class="grid grid-cols-3 gap-4">
                                <div>
                                    <p class="text-xs text-fuchsia-500 mb-1">执行时间</p>
                                    <p class="font-medium text-fuchsia-700">{{ formatTime(jwsTask?.parsedCron?.jwsHour, jwsTask?.parsedCron?.jwsMinute) }}</p>
                                </div>
                                <div>
                                    <p class="text-xs text-fuchsia-500 mb-1">续签间隔</p>
                                    <p class="font-medium text-fuchsia-700">{{ formatJwsInterval(jwsTask?.parsedCron?.jwsIntervalWeeks) }}</p>
                                </div>
                                <div>
                                    <p class="text-xs text-fuchsia-500 mb-1">下次续签</p>
                                    <p class="font-medium text-fuchsia-700">{{ jwsTask?.parsedCron?.jwsNextRefresh || '-' }}</p>
                                </div>
                            </div>
                        </div>

                        <button @click="openEditModal('refresh_jws')"
                                class="w-full py-2.5 bg-fuchsia-400 text-white rounded-lg hover:bg-fuchsia-500 transition-colors flex items-center justify-center font-medium">
                            <i class="fas fa-edit mr-2"></i>修改配置
                        </button>
                    </div>
                </div>
            </main>

            <!-- 编辑弹窗 -->
            <div v-if="showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeEditModal">
                <div class="bg-white/95 backdrop-blur-sm rounded-xl w-full max-w-3xl mx-4 p-6 max-h-[90vh] overflow-y-auto border border-pink-200">
                    <h3 class="text-lg font-semibold text-pink-800 mb-4">
                        {{ getTaskTitle(editingTask) }}
                    </h3>

                    <!-- 调度任务配置 -->
                    <div v-if="editingTask === 'schedule_users'" class="space-y-4">
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-pink-700 text-sm font-medium mb-2">小时 (0-23)</label>
                                <input v-model.number="editForm.scheduleHour" type="number" min="0" max="23"
                                       class="w-full px-4 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                            </div>
                            <div>
                                <label class="block text-pink-700 text-sm font-medium mb-2">分钟 (0-59)</label>
                                <input v-model.number="editForm.scheduleMinute" type="number" min="0" max="59"
                                       class="w-full px-4 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                            </div>
                        </div>
                        <div>
                            <label class="block text-pink-700 text-sm font-medium mb-2">延迟范围 (分钟)</label>
                            <div class="flex items-center gap-3">
                                <select v-model.number="editForm.delayRange"
                                        class="flex-1 px-4 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                                    <option v-for="option in delayRangeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                                </select>
                                <input v-model.number="editForm.delayRange" type="number" min="1" max="120"
                                       class="w-20 px-3 py-2 border border-pink-200 rounded-lg focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none">
                            </div>
                        </div>

                        <!-- 年历 -->
                        <div>
                            <div class="flex items-center justify-between mb-3">
                                <label class="text-pink-700 text-sm font-medium">签到日期日历</label>
                                <div class="flex items-center gap-2">
                                    <button @click="applyAutoSelect" type="button"
                                            class="px-3 py-1 bg-pink-100 text-pink-600 rounded-full text-xs font-medium hover:bg-pink-200 transition-colors">
                                        按默认规则填充
                                    </button>
                                    <select v-model.number="calendarYear"
                                            class="px-3 py-1 border border-pink-200 rounded-lg text-sm focus:border-pink-400 outline-none">
                                        <option :value="2025">2025年</option>
                                        <option :value="2026">2026年</option>
                                        <option :value="2027">2027年</option>
                                    </select>
                                </div>
                            </div>

                            <!-- 月份导航 -->
                            <div class="flex items-center justify-between mb-2">
                                <button @click="prevMonth" class="w-8 h-8 rounded-full bg-pink-100 hover:bg-pink-200 flex items-center justify-center text-pink-600">
                                    <i class="fas fa-chevron-left text-xs"></i>
                                </button>
                                <span class="text-pink-700 font-medium">{{ currentMonthLabel }}</span>
                                <button @click="nextMonth" class="w-8 h-8 rounded-full bg-pink-100 hover:bg-pink-200 flex items-center justify-center text-pink-600">
                                    <i class="fas fa-chevron-right text-xs"></i>
                                </button>
                            </div>

                            <!-- 图例 -->
                            <div class="flex flex-wrap gap-3 mb-2 text-xs">
                                <span class="flex items-center gap-1"><span class="inline-block w-3 h-3 bg-green-400 rounded"></span>已选</span>
                                <span class="flex items-center gap-1"><span class="inline-block w-3 h-3 bg-red-200 rounded"></span>假期/寒暑假</span>
                                <span class="flex items-center gap-1"><span class="inline-block w-3 h-3 bg-pink-50 rounded"></span>可选</span>
                            </div>

                            <!-- 星期头 -->
                            <div class="grid grid-cols-7 gap-1 text-center text-xs font-medium text-pink-500 mb-1">
                                <div>日</div><div>一</div><div>二</div><div>三</div><div>四</div><div>五</div><div>六</div>
                            </div>

                            <!-- 日历格子 -->
                            <div class="grid grid-cols-7 gap-1">
                                <div v-for="(day, idx) in calendarDays" :key="idx"
                                     @click="toggleCalendarDay(day)"
                                     :class="getDayClass(day)"
                                     class="h-9 rounded flex items-center justify-center text-xs transition-colors"
                                     :style="day.isCurrentMonth ? 'cursor:pointer' : ''">
                                    {{ day.dayOfMonth }}
                                </div>
                            </div>

                            <!-- 统计 -->
                            <div class="flex justify-between text-xs text-pink-500 mt-2">
                                <span>已选: {{ selectedCount }} 天</span>
                                <span>假期跳过: {{ skippedCount }} 天</span>
                            </div>
                        </div>
                    </div>

                    <!-- 间隔执行配置 -->
                    <div v-if="editingTask === 'interval_sign'" class="space-y-4">
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-rose-700 text-sm font-medium mb-2">开始时</label>
                                <input v-model.number="editForm.intervalStartHour" type="number" min="0" max="23"
                                       class="w-full px-4 py-2 border border-rose-200 rounded-lg focus:border-rose-400 focus:ring-2 focus:ring-rose-200 outline-none">
                            </div>
                            <div>
                                <label class="block text-rose-700 text-sm font-medium mb-2">开始分</label>
                                <input v-model.number="editForm.intervalStartMinute" type="number" min="0" max="59"
                                       class="w-full px-4 py-2 border border-rose-200 rounded-lg focus:border-rose-400 focus:ring-2 focus:ring-rose-200 outline-none">
                            </div>
                        </div>
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-rose-700 text-sm font-medium mb-2">结束时</label>
                                <input v-model.number="editForm.intervalEndHour" type="number" min="0" max="23"
                                       class="w-full px-4 py-2 border border-rose-200 rounded-lg focus:border-rose-400 focus:ring-2 focus:ring-rose-200 outline-none">
                            </div>
                            <div>
                                <label class="block text-rose-700 text-sm font-medium mb-2">结束分</label>
                                <input v-model.number="editForm.intervalEndMinute" type="number" min="0" max="59"
                                       class="w-full px-4 py-2 border border-rose-200 rounded-lg focus:border-rose-400 focus:ring-2 focus:ring-rose-200 outline-none">
                            </div>
                        </div>
                        <div>
                            <label class="block text-rose-700 text-sm font-medium mb-2">间隔分钟数</label>
                            <select v-model.number="editForm.intervalMinutes"
                                    class="w-full px-4 py-2 border border-rose-200 rounded-lg focus:border-rose-400 focus:ring-2 focus:ring-rose-200 outline-none">
                                <option :value="1">1分钟</option>
                                <option :value="5">5分钟</option>
                                <option :value="10">10分钟</option>
                                <option :value="15">15分钟</option>
                                <option :value="30">30分钟</option>
                            </select>
                        </div>
                    </div>

                    <!-- JWS刷新配置 -->
                    <div v-if="editingTask === 'refresh_jws'" class="space-y-4">
                        <div>
                            <label class="block text-fuchsia-700 text-sm font-medium mb-2">起始日期</label>
                            <input v-model="editForm.jwsStartDate" type="date"
                                   class="w-full px-4 py-2 border border-fuchsia-200 rounded-lg focus:border-fuchsia-400 focus:ring-2 focus:ring-fuchsia-200 outline-none">
                        </div>
                        <div>
                            <label class="block text-fuchsia-700 text-sm font-medium mb-2">续签间隔</label>
                            <select v-model.number="editForm.jwsIntervalWeeks"
                                    class="w-full px-4 py-2 border border-fuchsia-200 rounded-lg focus:border-fuchsia-400 focus:ring-2 focus:ring-fuchsia-200 outline-none">
                                <option v-for="opt in jwsIntervalOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                            </select>
                        </div>
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-fuchsia-700 text-sm font-medium mb-2">小时 (0-23)</label>
                                <input v-model.number="editForm.jwsHour" type="number" min="0" max="23"
                                       class="w-full px-4 py-2 border border-fuchsia-200 rounded-lg focus:border-fuchsia-400 focus:ring-2 focus:ring-fuchsia-200 outline-none">
                            </div>
                            <div>
                                <label class="block text-fuchsia-700 text-sm font-medium mb-2">分钟 (0-59)</label>
                                <input v-model.number="editForm.jwsMinute" type="number" min="0" max="59"
                                       class="w-full px-4 py-2 border border-fuchsia-200 rounded-lg focus:border-fuchsia-400 focus:ring-2 focus:ring-fuchsia-200 outline-none">
                            </div>
                        </div>
                        <!-- 预览下次刷新 -->
                        <div v-if="editForm.jwsStartDate" class="bg-fuchsia-50 rounded-lg p-3 text-sm text-fuchsia-700">
                            <i class="fas fa-info-circle mr-1"></i>
                            起始 {{ editForm.jwsStartDate }}，{{ formatJwsInterval(editForm.jwsIntervalWeeks) }}
                        </div>
                    </div>

                    <!-- 通用设置 -->
                    <div class="mt-4 pt-4 border-t border-pink-200">
                        <label class="flex items-center gap-2">
                            <input v-model="editForm.enabled" type="checkbox" class="w-4 h-4 text-pink-500">
                            <span class="text-pink-700">启用此任务</span>
                        </label>
                    </div>

                    <div class="flex justify-end gap-4 mt-6">
                        <button @click="closeEditModal" class="px-6 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50">
                            取消
                        </button>
                        <button @click="saveConfig" class="px-6 py-2 bg-pink-400 text-white rounded-lg hover:bg-pink-500">
                            保存
                        </button>
                    </div>
                </div>
            </div>

            <!-- 立即调度确认弹窗 -->
            <div v-if="showScheduleModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="cancelSchedule">
                <div class="bg-white/95 backdrop-blur-sm rounded-xl w-full max-w-md mx-4 p-6 border border-pink-200">
                    <h3 class="text-lg font-semibold text-pink-800 mb-2">立即调度确认</h3>
                    <p class="text-pink-600 text-sm mb-6">确定要立即执行调度所有用户任务吗？</p>
                    <div class="flex gap-3">
                        <button @click="cancelSchedule"
                                class="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-600 hover:bg-gray-50 transition-colors">
                            取消
                        </button>
                        <button @click="confirmSchedule(false)"
                                class="flex-1 px-4 py-2 border border-pink-300 rounded-lg text-pink-600 hover:bg-pink-50 transition-colors">
                            仅调度
                        </button>
                        <button @click="confirmSchedule(true)"
                                class="flex-1 px-4 py-2 bg-pink-400 text-white rounded-lg hover:bg-pink-500 transition-colors">
                            调度并发送邮件
                        </button>
                    </div>
                </div>
            </div>

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
        </div>
    `,
    methods: {
        getTaskTitle(taskKey) {
            const titles = {
                'schedule_users': '修改调度日历',
                'interval_sign': '修改间隔执行时间',
                'refresh_jws': '修改JWS续签间隔'
            };
            return titles[taskKey] || '修改配置';
        },
        formatTime(hour, minute) {
            if (hour === undefined || minute === undefined) return '-';
            return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
        },
        formatJwsInterval(weeks) {
            if (!weeks) return '-';
            if (weeks === 1) return '每周';
            return `每${weeks}周`;
        },

        // ========== 日历相关方法 ==========

        getDayClass(day) {
            if (!day.isCurrentMonth) return 'bg-transparent';
            if (day.isSelected) return 'bg-green-400 text-white font-medium';
            if (day.isSkipped) return 'bg-red-100 text-red-500 hover:bg-red-200';
            return 'bg-pink-50 text-pink-600 hover:bg-pink-100';
        },

        toggleCalendarDay(day) {
            if (!day.isCurrentMonth) return;
            if (this.scheduleDates[day.dateStr]) {
                this.$delete(this.scheduleDates, day.dateStr);
            } else {
                this.$set(this.scheduleDates, day.dateStr, true);
            }
        },

        prevMonth() {
            if (this.currentMonth === 0) {
                this.calendarYear--;
                this.currentMonth = 11;
            } else {
                this.currentMonth--;
            }
        },

        nextMonth() {
            if (this.currentMonth === 11) {
                this.calendarYear++;
                this.currentMonth = 0;
            } else {
                this.currentMonth++;
            }
        },

        async applyAutoSelect() {
            try {
                const response = await api.getScheduleCalendar(this.$parent.userInfo.jwt, this.calendarYear);
                if (response.data.code === 200) {
                    const data = response.data.data;
                    this.scheduleDates = this.arrayToObj(data.autoSelectedDates || []);
                    this.skippedDates = this.arrayToObj(data.skippedDates || []);
                    this.holidayDates = this.arrayToObj(data.holidayDates || []);
                    this.autoSelectedDates = this.arrayToObj(data.autoSelectedDates || []);
                }
            } catch (err) {
                console.error('获取日历数据失败:', err);
            }
        },

        async loadCalendarData(task) {
            const year = this.calendarYear;
            try {
                const response = await api.getScheduleCalendar(this.$parent.userInfo.jwt, year);
                if (response.data.code === 200) {
                    const data = response.data.data;
                    this.holidayDates = this.arrayToObj(data.holidayDates || []);
                    this.skippedDates = this.arrayToObj(data.skippedDates || []);
                    this.autoSelectedDates = this.arrayToObj(data.autoSelectedDates || []);
                }
            } catch (err) {
                console.error('获取日历数据失败:', err);
            }
            if (task && task.scheduleConfig && task.scheduleConfig.scheduleDates) {
                this.scheduleDates = this.arrayToObj(task.scheduleConfig.scheduleDates);
            } else {
                this.scheduleDates = {};
            }
        },

        arrayToObj(arr) {
            const obj = {};
            arr.forEach(k => { obj[k] = true; });
            return obj;
        },

        // ========== 编辑弹窗 ==========

        async openEditModal(taskKey) {
            this.editingTask = taskKey;
            const task = this.taskConfigs.find(t => t.taskKey === taskKey);
            if (task && task.parsedCron) {
                this.editForm.enabled = task.enabled;

                if (taskKey === 'schedule_users') {
                    this.editForm.scheduleHour = parseInt(task.parsedCron.hour) || 18;
                    this.editForm.scheduleMinute = parseInt(task.parsedCron.minute) || 31;
                    this.editForm.delayRange = task.scheduleConfig?.delayRange || 30;
                    this.calendarYear = task.scheduleConfig?.scheduleYear || new Date().getFullYear();
                    this.currentMonth = 0;
                    await this.loadCalendarData(task);
                } else if (taskKey === 'interval_sign') {
                    this.editForm.intervalStartHour = parseInt(task.parsedCron.startHour) || 18;
                    this.editForm.intervalStartMinute = parseInt(task.parsedCron.startMinute) || 0;
                    this.editForm.intervalEndHour = parseInt(task.parsedCron.endHour) || 20;
                    this.editForm.intervalEndMinute = parseInt(task.parsedCron.endMinute) || 0;
                    this.editForm.intervalMinutes = parseInt(task.parsedCron.interval) || 1;
                } else if (taskKey === 'refresh_jws') {
                    this.editForm.jwsHour = parseInt(task.parsedCron.jwsHour) || 18;
                    this.editForm.jwsMinute = parseInt(task.parsedCron.jwsMinute) || 0;
                    this.editForm.jwsStartDate = task.parsedCron.jwsStartDate || '';
                    this.editForm.jwsIntervalWeeks = task.parsedCron.jwsIntervalWeeks || 1;
                }
            }
            this.showEditModal = true;
        },

        closeEditModal() {
            this.showEditModal = false;
            this.editingTask = null;
        },

        triggerImmediateSchedule() {
            this.showScheduleModal = true;
        },
        confirmSchedule(sendEmail) {
            this.showScheduleModal = false;
            this.$emit('immediate-schedule', sendEmail);
        },
        cancelSchedule() {
            this.showScheduleModal = false;
        },

        saveConfig() {
            let configData = {
                taskKey: this.editingTask,
                enabled: this.editForm.enabled
            };

            if (this.editingTask === 'schedule_users') {
                const datesArray = Object.keys(this.scheduleDates).sort();
                configData.scheduleConfig = {
                    scheduleDates: datesArray,
                    scheduleYear: this.calendarYear,
                    hour: this.editForm.scheduleHour,
                    minute: this.editForm.scheduleMinute,
                    delayRange: this.editForm.delayRange
                };
            } else if (this.editingTask === 'interval_sign') {
                const startTotalMinutes = this.editForm.intervalStartHour * 60 + this.editForm.intervalStartMinute;
                const endTotalMinutes = this.editForm.intervalEndHour * 60 + this.editForm.intervalEndMinute;
                if (startTotalMinutes >= endTotalMinutes) {
                    alert('开始时间必须小于结束时间');
                    return;
                }
                configData.intervalConfig = {
                    startHour: this.editForm.intervalStartHour,
                    startMinute: this.editForm.intervalStartMinute,
                    endHour: this.editForm.intervalEndHour,
                    endMinute: this.editForm.intervalEndMinute,
                    intervalMinutes: this.editForm.intervalMinutes
                };
            } else if (this.editingTask === 'refresh_jws') {
                if (!this.editForm.jwsStartDate) {
                    alert('请选择起始日期');
                    return;
                }
                configData.jwsConfig = {
                    startDate: this.editForm.jwsStartDate,
                    intervalWeeks: this.editForm.jwsIntervalWeeks,
                    hour: this.editForm.jwsHour,
                    minute: this.editForm.jwsMinute
                };
            }

            this.$emit('update-task-config', configData);
            this.closeEditModal();
        }
    }
});
