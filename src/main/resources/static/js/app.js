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

        // 当前页面：user 或 admin
        currentPage: 'user',

        // 签到数据
        signs: [],
        selectedSign: {},

        // 分页
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
        taskConfigs: []
    },
    computed: {
        // 待签到数量
        pendingCount() {
            return this.signs.filter(sign => {
                if (sign.signStatus === 2 && !sign.date) {
                    const currentTime = new Date().getTime();
                    if (sign.start && sign.end) {
                        return currentTime >= sign.start && currentTime <= sign.end;
                    }
                }
                return false;
            }).length;
        },

        // 总页数
        totalPages() {
            return Math.ceil(this.total / this.pageSize);
        }
    },
    mounted() {
        // 检查本地存储的登录信息
        const savedUserInfo = localStorage.getItem('userInfo');
        if (savedUserInfo) {
            this.userInfo = JSON.parse(savedUserInfo);
            // 兼容旧数据：如果没有 role 字段，默认为 USER
            if (!this.userInfo.role) {
                this.userInfo.role = 'USER';
            }
            this.isLoggedIn = true;
            // 根据角色加载对应页面
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
        // 显示消息
        showMessage(msg, type = 'success') {
            this.message = msg;
            this.messageType = type;
            setTimeout(() => {
                this.message = '';
            }, 3000);
        },

        // RSA加密密码
        rsaEncrypt(password, publicKey) {
            const encrypt = new JSEncrypt();
            encrypt.setPublicKey(publicKey);
            return encrypt.encrypt(password);
        },

        // 登录
        async handleLogin({ username, password }) {
            this.isLoading = true;
            this.error = '';

            try {
                // 1. 先获取RSA公钥
                const publicKeyResponse = await api.getPublicKey();
                if (publicKeyResponse.data.code !== 200) {
                    throw new Error('获取加密公钥失败');
                }
                const publicKey = publicKeyResponse.data.data.publicKey;

                // 2. 使用RSA加密密码
                const encryptedPassword = this.rsaEncrypt(password, publicKey);
                if (!encryptedPassword) {
                    throw new Error('密码加密失败');
                }

                // 3. 发送登录请求
                const response = await api.login(username, encryptedPassword);

                if (response.data.code === 200) {
                    this.userInfo = response.data.data;
                    this.isLoggedIn = true;
                    localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
                    this.showMessage('登录成功！');
                    
                    // 如果是管理员，默认显示管理员页面
                    if (this.userInfo.role === 'ADMIN') {
                        this.currentPage = 'admin';
                        this.getAllUsers();
                    } else {
                        this.currentPage = 'home';
                        // 检查普通用户是否设置了签到日期
                        if (!this.userInfo.signDays && this.userInfo.role === 'USER') {
                            this.$nextTick(() => {
                                this.showSignDaysPrompt();
                            });
                        }
                    }
                } else {
                    this.error = response.data.message || '登录失败';
                    this.showMessage(this.error, 'error');
                }
            } catch (err) {
                this.error = err.response?.data?.message || '登录请求失败';
                this.showMessage(this.error, 'error');
                console.error('登录错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 退出登录
        async handleLogout() {
            try {
                await api.logout(this.userInfo.jwt);
            } catch (err) {
                console.error('退出登录错误:', err);
            } finally {
                this.isLoggedIn = false;
                this.userInfo = {
                    id: '',
                    name: '',
                    username: '',
                    email: '',
                    jwt: '',
                    autoSign: false,
                    signDays: '',
                    role: 'USER'
                };
                this.signs = [];
                this.currentPage = 'user';
                localStorage.removeItem('userInfo');
                this.showMessage('已退出登录');
            }
        },

        // 注销信息（删除数据库）
        async handleUnregister() {
            try {
                await api.unregister(this.userInfo.jwt);
            } catch (err) {
                console.error('注销信息错误:', err);
            } finally {
                this.isLoggedIn = false;
                this.userInfo = {
                    id: '',
                    name: '',
                    username: '',
                    email: '',
                    jwt: '',
                    autoSign: false,
                    signDays: '',
                    role: 'USER'
                };
                this.signs = [];
                this.currentPage = 'user';
                localStorage.removeItem('userInfo');
                this.showMessage('注销信息成功');
            }
        },

        // 切换到用户页面
        switchToUserPage() {
            this.currentPage = 'user';
            this.getAllSigns(1);
        },

        // 切换到管理员页面
        switchToAdminPage() {
            this.currentPage = 'admin';
            this.getAllUsers();
            this.fetchAdminStats();
        },

        // 获取管理端统计数据
        async fetchAdminStats() {
            try {
                const response = await api.getUserStats(this.userInfo.jwt);
                if (response.data.code === 200) {
                    this.adminStats = response.data.data;
                }
            } catch (err) {
                console.error('获取统计数据失败:', err);
            }
        },

        // 切换自动签到
        async handleToggleAutoSign() {
            const newAutoSign = !this.userInfo.autoSign;
            try {
                await api.toggleAutoSign(this.userInfo.jwt, newAutoSign);
                this.userInfo.autoSign = newAutoSign;
                this.showMessage(`自动签到已${newAutoSign ? '开启' : '关闭'}`);
                this.showUserMenu = false;
            } catch (err) {
                this.showMessage('修改失败', 'error');
                console.error('修改自动签到错误:', err);
            }
        },

        // 更新用户信息
        async handleUpdateUserInfo(data) {
            this.isUpdating = true;
            try {
                const response = await api.updateUserInfo(this.userInfo.jwt, data);
                if (response.data.code === 200) {
                    this.userInfo.name = data.name || this.userInfo.name;
                    this.userInfo.email = data.email || this.userInfo.email;
                    if (data.signStartTime) this.userInfo.signStartTime = data.signStartTime;
                    if (data.signEndTime) this.userInfo.signEndTime = data.signEndTime;
                    if (data.signDays) {
                        this.userInfo.signDays = data.signDays;
                        await api.setSignDays(this.userInfo.jwt, data.signDays);
                    }
                    localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
                    this.showEditProfileModal = false;
                    this.showMessage('个人信息更新成功');
                }
            } catch (err) {
                this.showMessage('更新失败', 'error');
                console.error('更新用户信息错误:', err);
            } finally {
                this.isUpdating = false;
            }
        },

        // 提示用户设置签到日期
        showSignDaysPrompt() {
            const result = confirm('您还未设置签到日期配置，系统将默认每天签到。\n\n是否现在设置签到日期？\n（点击"确定"前往设置，点击"取消"保持默认每天签到）');
            if (result) {
                this.showEditProfileModal = true;
            } else {
                api.setSignDays(this.userInfo.jwt, '0,1,2,3,4,5,6');
                this.userInfo.signDays = '0,1,2,3,4,5,6';
                localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
            }
        },

        // 获取所有签到
        async getAllSigns(page) {
            this.isLoading = true;
            this.signCurrentPage = page;

            try {
                const response = await api.getAllSigns(this.userInfo.jwt, page, this.pageSize);
                if (response.data.code === 200) {
                    this.signs = response.data.data || [];
                    this.total = response.data.total || this.signs.length;
                } else {
                    this.showMessage('获取签到列表失败', 'error');
                }
            } catch (err) {
                this.showMessage('获取签到列表失败', 'error');
                console.error('获取签到列表错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 一键签到
        async handleOneKeySign() {
            this.isLoading = true;
            try {
                const response = await api.oneKeySign(this.userInfo.jwt);
                this.showMessage(response.data || '一键签到完成');
                this.getAllSigns(this.signCurrentPage);
            } catch (err) {
                this.showMessage('一键签到失败', 'error');
                console.error('一键签到错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 单个签到
        async handleSignSingle(sign) {
            this.isLoading = true;
            try {
                const response = await api.signSingle(this.userInfo.jwt, sign);
                this.showMessage(response.data || '签到成功');
                this.getAllSigns(this.signCurrentPage);
            } catch (err) {
                this.showMessage('签到失败', 'error');
                console.error('签到错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 上一页
        prevPage() {
            if (this.signCurrentPage > 1) {
                this.getAllSigns(this.signCurrentPage - 1);
            }
        },

        // 下一页
        nextPage() {
            if (this.signCurrentPage < this.totalPages) {
                this.getAllSigns(this.signCurrentPage + 1);
            }
        },

        // 打开签到详情
        openSignDetail(sign) {
            this.selectedSign = sign;
            this.showUserMenu = false;
            this.showEditProfileModal = false;
            this.showDetailModal = true;
        },

        // 关闭签到详情
        closeSignDetail() {
            this.showDetailModal = false;
        },

        // 打开编辑资料
        openEditProfile() {
            this.showUserMenu = false;
            this.showDetailModal = false;
            this.showEditProfileModal = true;
        },

        // 关闭编辑资料
        closeEditProfile() {
            this.showEditProfileModal = false;
        },

        // 打开关于
        openAbout() {
            this.showUserMenu = false;
            this.showDetailModal = false;
            this.showEditProfileModal = false;
            this.showAboutModal = true;
        },

        // 关闭关于
        closeAbout() {
            this.showAboutModal = false;
        },

        // 显示注销确认
        showUnregisterConfirm() {
            this.showUserMenu = false;
            this.showUnregisterConfirmModal = true;
        },

        // 关闭注销确认
        closeUnregisterConfirm() {
            this.showUnregisterConfirmModal = false;
        },

        // 切换用户菜单
        toggleUserMenu() {
            this.showUserMenu = !this.showUserMenu;
        },

        // ========== 管理员功能 ==========

        // 获取用户列表（分页）
        async getAllUsers() {
            this.isLoading = true;
            try {
                const response = await api.getUsersByPage(
                    this.userInfo.jwt,
                    this.adminCurrentPage,
                    this.pageSize,
                    this.adminSearchQuery,
                    this.adminFilter
                );
                if (response.data.code === 200) {
                    this.users = response.data.data.records || [];
                    this.total = response.data.data.total || 0;
                } else {
                    this.showMessage(response.data.message || '获取用户列表失败', 'error');
                }
            } catch (err) {
                console.error('获取用户列表错误:', err);
                console.error('错误状态码:', err.response?.status);
                console.error('错误信息:', err.response?.data);
                if (err.response?.status === 403) {
                    this.showMessage('无权限访问，仅管理员可操作', 'error');
                    this.switchToUserPage();
                } else {
                    this.showMessage('获取用户列表失败', 'error');
                }
            } finally {
                this.isLoading = false;
            }
        },

        // 删除用户
        async handleDeleteUser(user) {
            this.isLoading = true;
            try {
                const response = await api.deleteUser(this.userInfo.jwt, user.username);
                if (response.data.code === 200) {
                    this.showMessage('用户删除成功');
                    this.getAllUsers();
                } else {
                    this.showMessage(response.data.message || '删除失败', 'error');
                }
            } catch (err) {
                if (err.response?.status === 403) {
                    this.showMessage('无权限操作，仅管理员可删除用户', 'error');
                } else {
                    this.showMessage('删除用户失败', 'error');
                }
                console.error('删除用户错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 续签用户 JWS
        async handleRefreshUserJws(user) {
            this.isLoading = true;
            try {
                const response = await api.refreshUserJws(this.userInfo.jwt, user.username);
                if (response.data.code === 200) {
                    this.showMessage('JWS 续签成功');
                    this.getAllUsers();
                } else {
                    this.showMessage(response.data.message || '续签失败', 'error');
                }
            } catch (err) {
                if (err.response?.status === 403) {
                    this.showMessage('无权限操作，仅管理员可续签 JWS', 'error');
                } else {
                    this.showMessage('续签 JWS 失败', 'error');
                }
                console.error('续签 JWS 错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 编辑用户信息
        async handleEditUser(user) {
            this.isLoading = true;
            try {
                const data = {
                    name: user.name,
                    email: user.email
                };
                if (user.signDays) data.signDays = user.signDays;
                if (user.signStartTime) data.signStartTime = user.signStartTime;
                if (user.signEndTime) data.signEndTime = user.signEndTime;
                const response = await api.updateUser(this.userInfo.jwt, user.username, data);
                if (response.data.code === 200) {
                    this.showMessage('用户信息更新成功');
                    this.getAllUsers();
                } else {
                    this.showMessage(response.data.message || '更新失败', 'error');
                }
            } catch (err) {
                if (err.response?.status === 403) {
                    this.showMessage('无权限操作，仅管理员可修改用户信息', 'error');
                } else {
                    this.showMessage('更新用户信息失败', 'error');
                }
                console.error('更新用户信息错误:', err);
            } finally {
                this.isLoading = false;
            }
        },

        // 处理分页变化
        handleAdminPageChange(page) {
            this.adminCurrentPage = page;
            this.getAllUsers();
        },

        // 处理筛选变化
        handleAdminFilterChange(filter) {
            this.adminFilter = filter;
            this.adminCurrentPage = 1;
            this.getAllUsers();
        },

        // 处理搜索变化
        handleAdminSearch(query) {
            this.adminSearchQuery = query;
            this.adminCurrentPage = 1;
            this.getAllUsers();
        },

        // 获取用户详情
        async handleGetUserDetail(username) {
            try {
                const response = await api.getUserDetail(this.userInfo.jwt, username);
                if (response.data.code === 200) {
                    console.log('用户详情:', response.data.data);
                } else {
                    this.showMessage(response.data.message || '获取用户详情失败', 'error');
                }
            } catch (err) {
                console.error('获取用户详情错误:', err);
                this.showMessage('获取用户详情失败', 'error');
            }
        },

        // 获取用户签到记录
        async handleGetUserSigns(username) {
            try {
                const response = await api.getUserSigns(this.userInfo.jwt, username, 10);
                if (response.data.code === 200) {
                    return response.data.data || [];
                } else {
                    this.showMessage(response.data.message || '获取签到记录失败', 'error');
                    return [];
                }
            } catch (err) {
                console.error('获取用户签到记录错误:', err);
                this.showMessage('获取签到记录失败', 'error');
                return [];
            }
        },

        // 切换用户自动签到状态（管理员用）
        async handleToggleUserAutoSign(user) {
            this.isLoading = true;
            try {
                const response = await api.toggleUserAutoSign(this.userInfo.jwt, user.username, !user.autoSign);
                if (response.data.code === 200) {
                    this.showMessage(`用户 ${user.username} 自动签到已${!user.autoSign ? '开启' : '关闭'}`);
                    this.getAllUsers();
                } else {
                    this.showMessage(response.data.message || '操作失败', 'error');
                }
            } catch (err) {
                console.error('切换自动签到错误:', err);
                this.showMessage('切换自动签到失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // 新增用户
        async handleAddUser(userData) {
            this.isLoading = true;
            try {
                const response = await api.addUser(this.userInfo.jwt, userData);
                if (response.data.code === 200) {
                    this.showMessage('用户添加成功');
                    this.getAllUsers();
                } else {
                    this.showMessage(response.data.message || '添加失败', 'error');
                }
            } catch (err) {
                console.error('添加用户错误:', err);
                this.showMessage('添加用户失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // 管理员为用户签到
        async handleSignUser(username) {
            this.isLoading = true;
            try {
                const response = await api.signByAdmin(this.userInfo.jwt, username);
                if (response.data) {
                    this.showMessage(`用户 ${username} 签到成功: ${response.data}`);
                } else {
                    this.showMessage('签到失败', 'error');
                }
            } catch (err) {
                console.error('签到错误:', err);
                this.showMessage('签到失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // 一键为所有用户签到
        async handleSignAllUsers() {
            this.isLoading = true;
            try {
                await api.signAllUsers(this.userInfo.jwt);
                this.showMessage('已为所有用户执行签到');
            } catch (err) {
                console.error('一键签到错误:', err);
                this.showMessage('一键签到失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // ========== 定时任务配置相关方法 ==========

        // 切换到定时任务配置页面
        switchToTaskConfigPage() {
            this.currentPage = 'task-config';
            this.getTaskConfigs();
        },

        // 获取所有任务配置
        async getTaskConfigs() {
            this.isLoading = true;
            try {
                const response = await api.getTaskConfigs(this.userInfo.jwt);
                if (response.data.code === 200) {
                    this.taskConfigs = response.data.data || [];
                } else {
                    this.showMessage(response.data.message || '获取任务配置失败', 'error');
                }
            } catch (err) {
                console.error('获取任务配置错误:', err);
                this.showMessage('获取任务配置失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // 更新任务配置
        async handleUpdateTaskConfig(configData) {
            this.isLoading = true;
            try {
                const response = await api.updateTaskConfig(this.userInfo.jwt, configData.taskKey, configData);
                if (response.data.code === 200) {
                    this.showMessage('任务配置更新成功');
                    this.getTaskConfigs();
                } else {
                    this.showMessage(response.data.message || '更新失败', 'error');
                }
            } catch (err) {
                console.error('更新任务配置错误:', err);
                this.showMessage('更新任务配置失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // 切换到 Redis 队列页面
        switchToRedisQueuePage() {
            this.currentPage = 'redis-queue';
        },

        switchToAnnouncementPage() {
            this.currentPage = 'announcements';
        },

        switchToLogPage() {
            this.currentPage = 'logs';
        },

        // 切换到首页
        switchToHomePage() {
            this.currentPage = 'home';
        },

        // 立即执行调度任务
        async handleImmediateSchedule(sendEmail) {
            this.isLoading = true;
            try {
                const response = await api.triggerImmediateSchedule(this.userInfo.jwt, sendEmail);
                if (response.data.code === 200) {
                    this.showMessage(sendEmail ? '调度任务已立即执行，邮件已发送' : '调度任务已立即执行（未发送邮件）');
                    // 刷新任务配置
                    this.getTaskConfigs();
                } else {
                    this.showMessage(response.data.message || '执行失败', 'error');
                }
            } catch (err) {
                console.error('立即执行调度任务错误:', err);
                this.showMessage('立即执行调度任务失败', 'error');
            } finally {
                this.isLoading = false;
            }
        },

        // 开始加载
        startLoading() {
            this.isLoading = true;
        },

        // 结束加载
        endLoading() {
            this.isLoading = false;
        }
    }
});
