// 主应用入口
new Vue({
    el: '#app',
    data: {
        // 状态
        isLoggedIn: false,
        isLoading: false,
        isUpdating: false,
        error: '',
        message: '',
        messageType: 'success',
        showDetailModal: false,
        showEditProfileModal: false,
        showUserMenu: false,
        showAboutModal: false,
        showLogoutConfirmModal: false,
        showUnregisterConfirmModal: false,

        // 用户信息
        userInfo: {
            id: '',
            name: '',
            username: '',
            email: '',
            jwt: '',
            autoSign: false,
            signDays: '',
            role: 'USER'
        },

        // 当前页面
        currentPage: 'user',

        // 签到数据
        signs: [],
        selectedSign: {},
        signCurrentPage: 1,
        pageSize: 10,
        total: 0,

        // 管理员数据
        users: [],
        adminCurrentPage: 1,
        adminFilter: 'all',
        adminSearchQuery: '',
        adminStats: { total: 0, autoSignCount: 0, invalidJwsCount: 0, todayActiveCount: 0 },

        // 定时任务配置数据
        taskConfigs: [],

        // 验证码（学号密码登录/密码绑定时触发）
        captchaSession: null,

        // 短信/扫码登录
        smsSessionId: null,
        smsCountdown: 0,
        smsCountdownTimer: null,
        qrSession: null,
        qrPollTimer: null,

        // 学号绑定弹窗
        showBindModal: false,
    },
    computed: {
        pendingCount() {
            const now = new Date().getTime();
            return this.signs.filter(sign => {
                if (sign.signStatus !== 2 || sign.date) return false;
                if (sign.start && sign.end) return now >= sign.start && now <= sign.end;
                return false;
            }).length;
        },
        totalPages() {
            return Math.ceil(this.total / this.pageSize);
        }
    },
    mounted() {
        const savedUserInfo = localStorage.getItem('userInfo');
        if (savedUserInfo) {
            this.userInfo = JSON.parse(savedUserInfo);
            if (!this.userInfo.role) this.userInfo.role = 'USER';
            this.isLoggedIn = true;
            if (this.userInfo.role === 'ADMIN') {
                this.currentPage = 'admin';
                this.getAllUsers();
                this.fetchAdminStats();
            } else {
                this.currentPage = 'home';
            }
        }
    },
    methods: {
        // ==================== 基础工具 ====================

        showMessage(msg, type = 'success') {
            this.message = msg;
            this.messageType = type;
            setTimeout(() => { this.message = ''; }, 3000);
        },

        rsaEncrypt(password, publicKey) {
            const encrypt = new JSEncrypt();
            encrypt.setPublicKey(publicKey);
            return encrypt.encrypt(password);
        },

        // 用 RSA 公钥加密密码后执行请求
        async encryptPassword(password) {
            const response = await api.getPublicKey();
            if (response.data.code !== 200) throw new Error('获取加密公钥失败');
            const encrypted = this.rsaEncrypt(password, response.data.data.publicKey);
            if (!encrypted) throw new Error('密码加密失败');
            return encrypted;
        },

        // 统一请求处理：自动 loading、成功回调、失败提示
        // onSuccess(data, full)  full 为完整响应体（含分页 total 等）
        // onBusinessError(data) 返回 true 表示已自行处理（如 1004/1005）
        async callApi(fn, { loading = true, successMsg, errorMsg = '操作失败', onSuccess, onBusinessError } = {}) {
            if (loading) this.isLoading = true;
            try {
                const response = await fn();
                if (response.data.code === 200) {
                    if (onSuccess) await onSuccess(response.data.data, response.data);
                    if (successMsg) this.showMessage(successMsg);
                    return response.data.data;
                }
                if (onBusinessError && onBusinessError(response.data)) return null;
                this.error = response.data.message || errorMsg;
                this.showMessage(this.error, 'error');
                return null;
            } catch (err) {
                this.error = err.response?.data?.message || errorMsg;
                this.showMessage(this.error, 'error');
                return null;
            } finally {
                if (loading) this.isLoading = false;
            }
        },

        saveUserInfo() {
            localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
        },

        clearUserInfo() {
            this.isLoggedIn = false;
            this.userInfo = { id: '', name: '', username: '', email: '', jwt: '', autoSign: false, signDays: '', role: 'USER' };
            this.signs = [];
            this.currentPage = 'user';
            localStorage.removeItem('userInfo');
        },

        // ==================== 登录 ====================

        _onLoginSuccess(data) {
            this.captchaSession = null;
            this.userInfo = data;
            this.isLoggedIn = true;
            this.stopQrPolling();
            this.qrSession = null;
            this.smsSessionId = null;
            this.stopSmsCountdown();
            this.saveUserInfo();
            this.showMessage('登录成功！');
            if (this.userInfo.role === 'ADMIN') {
                this.currentPage = 'admin';
                this.getAllUsers();
            } else {
                this.currentPage = 'home';
                if (!this.userInfo.signDays && this.userInfo.role === 'USER') {
                    this.$nextTick(this.showSignDaysPrompt);
                }
            }
        },

        async handleXsyLogin({ username, casPsd, captchaCode }) {
            this.error = '';
            await this.callApi(
                async () => {
                    const encrypted = await this.encryptPassword(casPsd);
                    return api.xsyLogin(username, encrypted,
                        this.captchaSession ? this.captchaSession.captchaSessionId : null,
                        captchaCode || null);
                },
                {
                    errorMsg: '登录请求失败',
                    onSuccess: data => this._onLoginSuccess(data),
                    onBusinessError: data => {
                        if (data.code === 1004 && data.data && data.data.captchaSessionId) {
                            this.captchaSession = data.data;
                            this.showMessage('学校系统要求输入验证码', 'error');
                            return true;
                        }
                        return false;
                    }
                }
            );
        },

        // ==================== 短信登录 ====================

        async handleSmsSend(phone) {
            // 点击立即倒计时，防止连点触发学校风控；失败则回滚
            this.startSmsCountdown();
            const data = await this.callApi(
                () => api.smsSend(phone),
                {
                    loading: false,
                    errorMsg: '发送失败',
                    onSuccess: data => {
                        this.smsSessionId = data.smsSessionId;
                        this.showMessage('验证码已发送，请注意查收');
                    }
                }
            );
            if (!data) this.stopSmsCountdown();
        },

        startSmsCountdown() {
            this.stopSmsCountdown();
            this.smsCountdown = 60;
            this.smsCountdownTimer = setInterval(() => {
                this.smsCountdown--;
                if (this.smsCountdown <= 0) this.stopSmsCountdown();
            }, 1000);
        },

        stopSmsCountdown() {
            if (this.smsCountdownTimer) {
                clearInterval(this.smsCountdownTimer);
                this.smsCountdownTimer = null;
            }
            this.smsCountdown = 0;
        },

        async handleSmsLogin({ username, phone, smsCode }) {
            this.error = '';
            await this.callApi(
                () => api.smsLogin(this.smsSessionId, username, phone, smsCode),
                { errorMsg: '登录失败', onSuccess: data => this._onLoginSuccess(data) }
            );
        },

        // ==================== 扫码登录 ====================

        async handleQrCreate(username) {
            await this.callApi(
                () => api.qrCreate(username),
                {
                    errorMsg: '创建二维码失败',
                    onSuccess: data => {
                        this.qrSession = { ...data, status: 'WAITING' };
                        this.startQrPolling();
                    }
                }
            );
        },

        startQrPolling() {
            this.stopQrPolling();
            let polls = 0;
            this.qrPollTimer = setInterval(async () => {
                polls++;
                if (!this.qrSession) return this.stopQrPolling();
                if (polls > 150) {
                    this.qrSession.status = 'EXPIRED';
                    this.stopQrPolling();
                    return this.showMessage('二维码已过期，请点击刷新', 'error');
                }
                try {
                    const response = await api.qrPoll(this.qrSession.qrSessionId);
                    if (response.data.code !== 200) {
                        return this.showMessage(response.data.message || '扫码状态查询失败', 'error');
                    }
                    const result = response.data.data;
                    if (result.status === 'SUCCESS' && result.loginVO) {
                        this._onLoginSuccess(result.loginVO);
                    } else if (result.status === 'EXPIRED') {
                        this.qrSession.status = 'EXPIRED';
                        this.stopQrPolling();
                        this.showMessage('二维码已过期，请点击刷新', 'error');
                    }
                } catch (err) {
                    console.error('扫码轮询错误:', err);
                }
            }, 2000);
        },

        stopQrPolling() {
            if (this.qrPollTimer) {
                clearInterval(this.qrPollTimer);
                this.qrPollTimer = null;
            }
        },

        // ==================== 学号绑定（重新绑定/换绑） ====================

        openBindModal() {
            this.showUserMenu = false;
            this.captchaSession = null;
            this.showBindModal = true;
        },

        closeBindModal() {
            this.showBindModal = false;
            this.captchaSession = null;
        },

        _onBindSuccess(data) {
            this.userInfo = data;
            this.saveUserInfo();
            this.closeBindModal();
            this.showMessage('学校密码已更新，JWS续签已启用');
        },

        async handleBindByPassword({ casPsd, captchaCode }) {
            await this.callApi(
                async () => {
                    const encrypted = await this.encryptPassword(casPsd);
                    return api.bindByPassword(this.userInfo.jwt, encrypted,
                        this.captchaSession ? this.captchaSession.captchaSessionId : null,
                        captchaCode || null);
                },
                {
                    errorMsg: '更新失败',
                    onSuccess: data => this._onBindSuccess(data),
                    onBusinessError: data => {
                        if (data.code === 1004 && data.data && data.data.captchaSessionId) {
                            this.captchaSession = data.data;
                            this.showMessage('学校系统要求输入验证码', 'error');
                            return true;
                        }
                        return false;
                    }
                }
            );
        },

        // ==================== 退出/注销 ====================

        async handleLogout() {
            try { await api.logout(this.userInfo.jwt); } catch (err) { console.error('退出登录错误:', err); }
            this.clearUserInfo();
            this.showMessage('已退出登录');
        },

        async handleUnregister() {
            try { await api.unregister(this.userInfo.jwt); } catch (err) { console.error('注销信息错误:', err); }
            this.clearUserInfo();
            this.showMessage('注销信息成功');
        },

        // ==================== 页面切换 ====================

        switchToUserPage() { this.currentPage = 'user'; this.getAllSigns(1); },
        switchToAdminPage() { this.currentPage = 'admin'; this.getAllUsers(); this.fetchAdminStats(); },
        switchToTaskConfigPage() { this.currentPage = 'task-config'; this.getTaskConfigs(); },
        switchToRedisQueuePage() { this.currentPage = 'redis-queue'; },
        switchToAnnouncementPage() { this.currentPage = 'announcements'; },
        switchToLogPage() { this.currentPage = 'logs'; },
        switchToHomePage() { this.currentPage = 'home'; },

        // ==================== 个人设置 ====================

        async fetchAdminStats() {
            await this.callApi(() => api.getUserStats(this.userInfo.jwt), {
                loading: false, errorMsg: '获取统计数据失败',
                onSuccess: data => { this.adminStats = data; }
            });
        },

        async handleToggleAutoSign() {
            const newAutoSign = !this.userInfo.autoSign;
            await this.callApi(() => api.toggleAutoSign(this.userInfo.jwt, newAutoSign), {
                errorMsg: '修改失败',
                successMsg: `自动签到已${newAutoSign ? '开启' : '关闭'}`,
                onSuccess: () => {
                    this.userInfo.autoSign = newAutoSign;
                    this.saveUserInfo();
                    this.showUserMenu = false;
                }
            });
        },

        async handleUpdateUserInfo(data) {
            this.isUpdating = true;
            try {
                const response = await api.updateUserInfo(this.userInfo.jwt, data);
                if (response.data.code !== 200) {
                    return this.showMessage(response.data.message || '更新失败', 'error');
                }
                Object.assign(this.userInfo, {
                    name: data.name || this.userInfo.name,
                    email: data.email || this.userInfo.email,
                    signStartTime: data.signStartTime || this.userInfo.signStartTime,
                    signEndTime: data.signEndTime || this.userInfo.signEndTime,
                    signDays: data.signDays || this.userInfo.signDays
                });
                if (data.signDays) await api.setSignDays(this.userInfo.jwt, data.signDays);
                this.saveUserInfo();
                this.showEditProfileModal = false;
                this.showMessage('个人信息更新成功');
            } catch (err) {
                this.showMessage('更新失败', 'error');
                console.error('更新用户信息错误:', err);
            } finally {
                this.isUpdating = false;
            }
        },

        showSignDaysPrompt() {
            if (confirm('您还未设置签到日期配置，系统将默认每天签到。\n\n是否现在设置签到日期？\n（点击"确定"前往设置，点击"取消"保持默认每天签到）')) {
                this.showEditProfileModal = true;
            } else {
                api.setSignDays(this.userInfo.jwt, '0,1,2,3,4,5,6');
                this.userInfo.signDays = '0,1,2,3,4,5,6';
                this.saveUserInfo();
            }
        },

        // ==================== 签到 ====================

        async getAllSigns(page) {
            this.signCurrentPage = page;
            await this.callApi(() => api.getAllSigns(this.userInfo.jwt, page, this.pageSize), {
                errorMsg: '获取签到列表失败',
                onSuccess: (data, full) => {
                    this.signs = data || [];
                    this.total = full.total || this.signs.length;
                }
            });
        },

        async handleOneKeySign() {
            this.isLoading = true;
            try {
                const response = await api.oneKeySign(this.userInfo.jwt);
                this.showMessage(response.data || '一键签到完成');
                this.getAllSigns(this.signCurrentPage);
            } catch (err) {
                this.showMessage('一键签到失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        async handleSignSingle(sign) {
            this.isLoading = true;
            try {
                const response = await api.signSingle(this.userInfo.jwt, sign);
                this.showMessage(response.data || '签到成功');
                this.getAllSigns(this.signCurrentPage);
            } catch (err) {
                this.showMessage('签到失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        prevPage() { if (this.signCurrentPage > 1) this.getAllSigns(this.signCurrentPage - 1); },
        nextPage() { if (this.signCurrentPage < this.totalPages) this.getAllSigns(this.signCurrentPage + 1); },

        openSignDetail(sign) {
            this.selectedSign = sign;
            this.showUserMenu = false;
            this.showEditProfileModal = false;
            this.showDetailModal = true;
        },
        closeSignDetail() { this.showDetailModal = false; },
        openEditProfile() { this.showUserMenu = false; this.showDetailModal = false; this.showEditProfileModal = true; },
        closeEditProfile() { this.showEditProfileModal = false; },
        openAbout() { this.showUserMenu = false; this.showDetailModal = false; this.showEditProfileModal = false; this.showAboutModal = true; },
        closeAbout() { this.showAboutModal = false; },
        showUnregisterConfirm() { this.showUserMenu = false; this.showUnregisterConfirmModal = true; },
        closeUnregisterConfirm() { this.showUnregisterConfirmModal = false; },
        toggleUserMenu() { this.showUserMenu = !this.showUserMenu; },

        // ==================== 管理员 ====================

        async getAllUsers() {
            this.isLoading = true;
            try {
                const response = await api.getUsersByPage(
                    this.userInfo.jwt, this.adminCurrentPage, this.pageSize,
                    this.adminSearchQuery, this.adminFilter
                );
                if (response.data.code === 200) {
                    this.users = response.data.data.records || [];
                    this.total = response.data.data.total || 0;
                } else {
                    this.showMessage(response.data.message || '获取用户列表失败', 'error');
                }
            } catch (err) {
                if (err.response?.status === 403) {
                    this.showMessage('无权限访问，仅管理员可操作', 'error');
                    this.switchToUserPage();
                } else {
                    this.showMessage('获取用户列表失败', 'error');
                }
                console.error('获取用户列表错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        async handleDeleteUser(user) {
            await this.callApi(() => api.deleteUser(this.userInfo.jwt, user.username), {
                errorMsg: '删除用户失败', successMsg: '用户删除成功',
                onSuccess: () => this.getAllUsers()
            });
        },

        async handleRefreshUserJws(user) {
            await this.callApi(() => api.refreshUserJws(this.userInfo.jwt, user.username), {
                errorMsg: '续签 JWS 失败', successMsg: 'JWS 续签成功',
                onSuccess: () => this.getAllUsers()
            });
        },

        async handleEditUser(user) {
            const data = { name: user.name, email: user.email };
            ['signDays', 'signStartTime', 'signEndTime'].forEach(k => {
                if (user[k]) data[k] = user[k];
            });
            await this.callApi(() => api.updateUser(this.userInfo.jwt, user.username, data), {
                errorMsg: '更新用户信息失败', successMsg: '用户信息更新成功',
                onSuccess: () => this.getAllUsers()
            });
        },

        handleAdminPageChange(page) { this.adminCurrentPage = page; this.getAllUsers(); },
        handleAdminFilterChange(filter) { this.adminFilter = filter; this.adminCurrentPage = 1; this.getAllUsers(); },
        handleAdminSearch(query) { this.adminSearchQuery = query; this.adminCurrentPage = 1; this.getAllUsers(); },

        async handleGetUserDetail(username) {
            await this.callApi(() => api.getUserDetail(this.userInfo.jwt, username), {
                loading: false, errorMsg: '获取用户详情失败',
                onSuccess: data => console.log('用户详情:', data)
            });
        },

        async handleGetUserSigns(username) {
            try {
                const response = await api.getUserSigns(this.userInfo.jwt, username, 10);
                if (response.data.code === 200) return response.data.data || [];
                this.showMessage(response.data.message || '获取签到记录失败', 'error');
            } catch (err) {
                console.error('获取用户签到记录错误:', err);
                this.showMessage('获取签到记录失败', 'error');
            }
            return [];
        },

        async handleToggleUserAutoSign(user) {
            await this.callApi(
                () => api.toggleUserAutoSign(this.userInfo.jwt, user.username, !user.autoSign),
                {
                    errorMsg: '切换自动签到失败',
                    successMsg: `用户 ${user.username} 自动签到已${!user.autoSign ? '开启' : '关闭'}`,
                    onSuccess: () => this.getAllUsers()
                }
            );
        },

        async handleAddUser(userData) {
            await this.callApi(() => api.addUser(this.userInfo.jwt, userData), {
                errorMsg: '添加用户失败', successMsg: '用户添加成功',
                onSuccess: () => this.getAllUsers()
            });
        },

        async handleSignUser(username) {
            this.isLoading = true;
            try {
                const response = await api.signByAdmin(this.userInfo.jwt, username);
                if (response.data) this.showMessage(`用户 ${username} 签到成功: ${response.data}`);
                else this.showMessage('签到失败', 'error');
            } catch (err) {
                this.showMessage('签到失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        async handleSignAllUsers() {
            await this.callApi(() => api.signAllUsers(this.userInfo.jwt), {
                errorMsg: '一键签到失败', successMsg: '已为所有用户执行签到'
            });
        },

        // ==================== 定时任务 ====================

        async getTaskConfigs() {
            await this.callApi(() => api.getTaskConfigs(this.userInfo.jwt), {
                errorMsg: '获取任务配置失败',
                onSuccess: data => { this.taskConfigs = data || []; }
            });
        },

        async handleUpdateTaskConfig(configData) {
            await this.callApi(
                () => api.updateTaskConfig(this.userInfo.jwt, configData.taskKey, configData),
                { errorMsg: '更新任务配置失败', successMsg: '任务配置更新成功', onSuccess: () => this.getTaskConfigs() }
            );
        },

        async handleImmediateSchedule(sendEmail) {
            await this.callApi(
                () => api.triggerImmediateSchedule(this.userInfo.jwt, sendEmail),
                {
                    errorMsg: '立即执行调度任务失败',
                    successMsg: sendEmail ? '调度任务已立即执行，邮件已发送' : '调度任务已立即执行（未发送邮件）',
                    onSuccess: () => this.getTaskConfigs()
                }
            );
        },

        startLoading() { this.isLoading = true; },
        endLoading() { this.isLoading = false; }
    }
});
