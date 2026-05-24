const API_BASE = 'http://localhost:11451'
﻿// API 工具类
// 创建 axios 实例
function createRequest(jwt) {
    const instance = axios.create({
        baseURL: API_BASE,
        timeout: 10000,
        headers: {
            'Content-Type': 'application/json'
        }
    });

    // 请求拦截器
    instance.interceptors.request.use(config => {
        if (jwt) {
            config.headers.Authorization = `Bearer ${jwt}`;
        }
        return config;
    });

    // 响应拦截器
    instance.interceptors.response.use(
        response => response,
        error => {
            if (error.response && error.response.status === 401) {
                localStorage.removeItem('userInfo');
                window.location.reload();
            }
            return Promise.reject(error);
        }
    );

    return instance;
}

// API 方法
const api = {
    // 获取RSA公钥
    getPublicKey() {
        const request = createRequest();
        return request.get('/user/public-key');
    },

    // 登录
    login(username, password) {
        const request = createRequest();
        return request.post('/user/login', { username, psd: password });
    },

    // 退出登录（不删除数据库）
    logout(jwt) {
        const request = createRequest(jwt);
        return request.post('/user/logout');
    },

    // 注销信息（删除数据库）
    unregister(jwt) {
        const request = createRequest(jwt);
        return request.post('/user/unregister');
    },

    // 切换自动签到
    toggleAutoSign(jwt, isAuto) {
        const request = createRequest(jwt);
        return request.put(`/user/auto-sign/${isAuto}`);
    },

    // 设置签到日期
    setSignDays(jwt, signDays) {
        const request = createRequest(jwt);
        return request.put('/user/sign-days', { signDays });
    },

    // 获取用户信息
    getUserInfo(jwt) {
        const request = createRequest(jwt);
        return request.get('/user/info');
    },

    // 更新用户信息
    updateUserInfo(jwt, data) {
        const request = createRequest(jwt);
        const params = {};
        if (data.name) params.name = data.name;
        if (data.email) params.email = data.email;
        if (data.signStartTime) params.signStartTime = data.signStartTime;
        if (data.signEndTime) params.signEndTime = data.signEndTime;
        return request.put('/user/info/', null, { params });
    },

    // 获取所有签到
    getAllSigns(jwt, page, size) {
        const request = createRequest(jwt);
        return request.get('/sign/allSign', {
            params: { page, size }
        });
    },

    // 一键签到
    oneKeySign(jwt) {
        const request = createRequest(jwt);
        return request.post('/sign/all');
    },

    // 单个签到
    signSingle(jwt, sign) {
        const request = createRequest(jwt);
        return request.post('/sign/one', null, {
            params: {
                id: sign.id,
                signId: sign.signId,
                schoolId: sign.schoolId
            }
        });
    },

    // ========== 管理员接口 ==========

    // 获取用户统计数据（全局）
    getUserStats(jwt) {
        const request = createRequest(jwt);
        return request.get('/admin/users/stats');
    },

    // 分页查询用户列表
    getUsersByPage(jwt, page, size, keyword, filter) {
        const request = createRequest(jwt);
        return request.get('/admin/users', {
            params: { page, size, keyword, filter }
        });
    },

    // 获取所有用户（已废弃）
    getAllUsers(jwt) {
        const request = createRequest(jwt);
        return request.get('/admin/users/all');
    },

    // 续签用户 JWS
    refreshUserJws(jwt, username) {
        const request = createRequest(jwt);
        return request.post(`/admin/refresh-jws/${username}`);
    },

    // 删除用户
    deleteUser(jwt, username) {
        const request = createRequest(jwt);
        return request.delete(`/admin/users/${username}`);
    },

    // 修改用户信息
    updateUser(jwt, username, data) {
        const request = createRequest(jwt);
        return request.put(`/admin/users/${username}`, data);
    },

    // 获取用户详细信息
    getUserDetail(jwt, username) {
        const request = createRequest(jwt);
        return request.get(`/admin/users/${username}/detail`);
    },

    // 获取用户签到记录
    getUserSigns(jwt, username, limit = 10) {
        const request = createRequest(jwt);
        return request.get(`/admin/users/${username}/signs`, {
            params: { limit }
        });
    },

    // 切换用户自动签到状态（管理员用）
    toggleUserAutoSign(jwt, username, autoSign) {
        const request = createRequest(jwt);
        return request.post(`/admin/users/${username}/auto-sign`, null, {
            params: { autoSign }
        });
    },

    // 新增用户
    addUser(jwt, userData) {
        const request = createRequest(jwt);
        return request.post('/admin/users', userData);
    },

    // 管理员为用户签到
    signByAdmin(jwt, username) {
        const request = createRequest(jwt);
        return request.post(`/sign/all-admin/${username}`);
    },

    // 一键为所有用户签到
    signAllUsers(jwt) {
        const request = createRequest(jwt);
        return request.post('/sign/all-all');
    },

    // ========== 定时任务配置接口 ==========

    // 获取所有任务配置
    getTaskConfigs(jwt) {
        const request = createRequest(jwt);
        return request.get('/admin/task-config');
    },

    // 根据任务标识获取配置
    getTaskConfigByKey(jwt, taskKey) {
        const request = createRequest(jwt);
        return request.get(`/admin/task-config/${taskKey}`);
    },

    // 更新任务配置
    updateTaskConfig(jwt, taskKey, data) {
        const request = createRequest(jwt);
        return request.put(`/admin/task-config/${taskKey}`, data);
    },

    // 立即执行调度任务
    triggerImmediateSchedule(jwt, sendEmail) {
        const request = createRequest(jwt);
        return request.post('/admin/task-config/schedule-users/immediate', null, {
            params: { sendEmail }
        });
    },

    // 获取调度日历数据
    getScheduleCalendar(jwt, year) {
        const request = createRequest(jwt);
        return request.get('/admin/task-config/schedule-users/calendar', {
            params: { year }
        });
    },

    // ========== 公告接口 ==========

    // 用户端：获取最新公告
    getLatestAnnouncement(jwt) {
        const request = createRequest(jwt);
        return request.get('/user/announcement/latest');
    },

    // 管理端：获取所有公告
    getAnnouncements(jwt) {
        const request = createRequest(jwt);
        return request.get('/admin/announcements');
    },

    // 管理端：新增公告
    addAnnouncement(jwt, data) {
        const request = createRequest(jwt);
        return request.post('/admin/announcements', data);
    },

    // 管理端：更新公告
    updateAnnouncement(jwt, id, data) {
        const request = createRequest(jwt);
        return request.put(`/admin/announcements/${id}`, data);
    },

    // 管理端：删除公告
    deleteAnnouncement(jwt, id) {
        const request = createRequest(jwt);
        return request.delete(`/admin/announcements/${id}`);
    },

    // ========== Redis 队列接口 ==========

    // 获取 Redis 队列信息
    getRedisQueueInfo(jwt) {
        const request = createRequest(jwt);
        return request.get('/admin/redis-queue');
    },

    // 清空 Redis 队列
    clearRedisQueue(jwt) {
        const request = createRequest(jwt);
        return request.delete('/admin/redis-queue');
    },

    // ========== 操作日志接口 ==========

    // 分页查询日志
    getOperationLogs(jwt, params) {
        const request = createRequest(jwt);
        return request.get('/admin/operation-logs', { params });
    },

    // 批量删除日志
    deleteOperationLogs(jwt, ids) {
        const request = createRequest(jwt);
        return request.delete('/admin/operation-logs', { data: { ids } });
    }
};
