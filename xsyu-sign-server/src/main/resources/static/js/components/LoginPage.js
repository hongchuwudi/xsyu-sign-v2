// 登录页面组件（学号密码 / 短信 / 扫码三种登录方式，均以学号为唯一标识）
Vue.component('login-page', {
    props: ['isLoading', 'error', 'qrSession', 'smsCountdown', 'captchaSession'],
    data() {
        return {
            tab: 'password',
            username: '',
            casPsd: '',
            captchaCode: '',
            phone: '',
            smsCode: ''
        };
    },
    watch: {
        captchaSession(val) {
            if (!val) this.captchaCode = '';
        }
    },
    methods: {
        handlePasswordLogin() {
            if (!this.username) return alert('请输入学号');
            this.$emit('xsy-login', {
                username: this.username,
                casPsd: this.casPsd,
                captchaCode: this.captchaCode
            });
        },
        handleSendSms() {
            if (!this.username) return alert('请先输入学号');
            if (!this.phone || !/^1[3-9]\d{9}$/.test(this.phone)) return alert('请输入正确的手机号');
            this.$emit('sms-send', this.phone);
        },
        handleSmsLogin() {
            if (!this.username) return alert('请先输入学号');
            if (!this.phone || !this.smsCode) return alert('请输入手机号和验证码');
            this.$emit('sms-login', { username: this.username, phone: this.phone, smsCode: this.smsCode });
        },
        handleQrCreate() {
            if (!this.username) return alert('请先输入学号');
            this.$emit('qr-create', this.username);
        }
    },
    template: /*html*/ `
        <div class="flex items-center justify-center min-h-screen p-4 bg-gradient-to-br from-pink-50 to-rose-50">
            <div class="w-full max-w-md bg-white/90 backdrop-blur-sm rounded-xl shadow-lg p-8 border border-pink-200">
                <div class="text-center mb-6">
                    <div class="w-16 h-16 bg-gradient-to-r from-pink-400 to-rose-400 rounded-full flex items-center justify-center mx-auto mb-4">
                        <i class="fas fa-user-graduate text-white text-2xl"></i>
                    </div>
                    <h2 class="text-2xl font-bold text-pink-800">油签机系统</h2>
                    <p class="text-pink-500 mt-2">选择登录方式</p>
                </div>

                <!-- 方式切换 -->
                <div class="flex bg-pink-50 rounded-lg p-1 mb-6">
                    <button @click="tab = 'password'"
                            :class="tab === 'password' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                            class="flex-1 px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
                        <i class="fas fa-key mr-1"></i>密码
                    </button>
                    <button @click="tab = 'sms'"
                            :class="tab === 'sms' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                            class="flex-1 px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
                        <i class="fas fa-comment-sms mr-1"></i>短信
                    </button>
                    <button @click="tab = 'qr'"
                            :class="tab === 'qr' ? 'bg-white text-pink-600 shadow-sm' : 'text-pink-400 hover:text-pink-600'"
                            class="flex-1 px-4 py-1.5 rounded-md text-sm font-medium transition-colors">
                        <i class="fas fa-qrcode mr-1"></i>扫码
                    </button>
                </div>

                <!-- 学号（两种方式共用，作为唯一标识） -->
                <div class="mb-4">
                    <label class="block text-pink-700 text-sm font-medium mb-2">学号</label>
                    <input v-model="username" type="text" placeholder="请输入学号"
                           class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                </div>

                <!-- 密码登录 -->
                <form v-if="tab === 'password'" @submit.prevent="handlePasswordLogin" class="space-y-4">
                    <div>
                        <label class="block text-pink-700 text-sm font-medium mb-2">学校密码（统一认证密码）</label>
                        <input v-model="casPsd" type="password" required placeholder="请输入学校统一认证密码"
                               class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                    </div>

                    <!-- 学校系统验证码（风控触发时显示） -->
                    <div v-if="captchaSession" class="p-4 bg-gray-50 rounded-lg border border-pink-200 space-y-3">
                        <p class="text-sm text-pink-600 font-medium">
                            <i class="fas fa-shield-alt mr-2"></i>学校系统要求输入验证码
                        </p>
                        <div class="flex justify-center">
                            <img :src="captchaSession.captchaImageBase64" alt="验证码"
                                 class="rounded border border-gray-300 bg-white"
                                 style="max-height: 60px;">
                        </div>
                        <p class="text-xs text-gray-400 text-center">验证码输错后提交会自动刷新</p>
                        <input v-model="captchaCode" type="text"
                               placeholder="请输入验证码（不区分大小写）"
                               class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                    </div>

                    <button type="submit" :disabled="isLoading"
                            class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
                        <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>登录中...</span>
                        <span v-else>{{ captchaSession ? '验证并登录' : '登录' }}</span>
                    </button>

                    <div class="bg-amber-50 border border-amber-300 text-amber-700 px-4 py-3 rounded-lg text-sm">
                        <i class="fas fa-info-circle mr-2"></i>首次登录自动创建账号；连续输错学校密码可能触发验证码或账号锁定
                    </div>
                </form>

                <!-- 短信登录 -->
                <div v-else-if="tab === 'sms'" class="space-y-4">
                    <div>
                        <label class="block text-pink-700 text-sm font-medium mb-2">学校系统绑定的手机号</label>
                        <div class="flex gap-2">
                            <input v-model="phone" type="tel" placeholder="请输入手机号"
                                   class="flex-1 px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                            <button @click="handleSendSms" :disabled="smsCountdown > 0"
                                    class="px-4 py-3 rounded-lg bg-pink-100 text-pink-600 text-sm font-medium hover:bg-pink-200 transition-colors whitespace-nowrap disabled:opacity-50">
                                {{ smsCountdown > 0 ? smsCountdown + 's' : '发送验证码' }}
                            </button>
                        </div>
                    </div>

                    <div>
                        <label class="block text-pink-700 text-sm font-medium mb-2">短信验证码</label>
                        <input v-model="smsCode" type="text" placeholder="请输入短信验证码"
                               class="w-full px-4 py-3 rounded-lg border border-pink-200 focus:border-pink-400 focus:ring-2 focus:ring-pink-200 outline-none transition-colors">
                    </div>

                    <button @click="handleSmsLogin" :disabled="isLoading"
                            class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
                        <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>登录中...</span>
                        <span v-else>登录</span>
                    </button>

                    <div class="bg-amber-50 border border-amber-300 text-amber-700 px-4 py-3 rounded-lg text-sm">
                        <i class="fas fa-info-circle mr-2"></i>手机号需与输入的学号在同一学校账号下绑定
                    </div>
                </div>

                <!-- 扫码登录 -->
                <div v-else-if="tab === 'qr'" class="space-y-4">
                    <template v-if="qrSession && qrSession.qrImageBase64">
                        <div class="flex justify-center">
                            <img :src="qrSession.qrImageBase64" alt="登录二维码"
                                 class="w-56 h-56 rounded-lg border border-pink-200 bg-white">
                        </div>
                        <p class="text-center text-sm text-pink-500">
                            <i class="fas fa-mobile-alt mr-1"></i>请用手机校园APP扫描二维码确认登录
                        </p>
                        <p v-if="qrSession.status === 'WAITING'" class="text-center text-xs text-gray-400">
                            <i class="fas fa-spinner fa-spin mr-1"></i>等待扫码中...
                        </p>
                        <p v-else class="text-center text-xs text-amber-600">二维码已过期，请刷新</p>
                        <button @click="handleQrCreate"
                                class="w-full border border-pink-300 text-pink-600 py-2.5 rounded-lg text-sm font-medium hover:bg-pink-50 transition-colors">
                            <i class="fas fa-sync-alt mr-1"></i>刷新二维码
                        </button>
                    </template>
                    <template v-else>
                        <p class="text-center text-sm text-pink-500 py-6">
                            <i class="fas fa-qrcode mr-2"></i>生成二维码后，用手机校园APP扫码即可登录
                        </p>
                        <button @click="handleQrCreate" :disabled="isLoading"
                                class="w-full bg-gradient-to-r from-pink-400 to-rose-400 text-white py-3 rounded-lg font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed">
                            <span v-if="isLoading"><i class="fas fa-spinner fa-spin mr-2"></i>生成中...</span>
                            <span v-else>生成二维码</span>
                        </button>
                    </template>
                </div>

                <div v-if="error" class="bg-rose-50 border border-rose-200 text-rose-600 px-4 py-3 rounded-lg text-sm mt-4">
                    <i class="fas fa-exclamation-circle mr-2"></i>{{ error }}
                </div>

                <div class="text-center mt-6 pt-4 border-t border-pink-100">
                    <p class="text-xs text-pink-400">陕ICP备2026004528号-1</p>
                </div>
            </div>
        </div>
    `
});
