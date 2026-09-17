// API 工具类
const API_BASE = 'http://localhost:11451'

function createRequest(jwt) {
    const instance = axios.create({
        baseURL: API_BASE,
        timeout: 10000,
        headers: { 'Content-Type': 'application/json' }
    });
    instance.interceptors.request.use(config => {
        if (jwt) config.headers.Authorization = `Bearer ${jwt}`;
        return config;
    });
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

// 统一请求助手：http('post', '/user/xxx', {jwt, data, params})
function http(method, url, options = {}) {
    const { jwt, data, params } = options;
    const request = createRequest(jwt);
    const config = {};
    if (data !== undefined) config.data = data;
    if (params !== undefined) config.params = params;
    return request.request({ method, url, ...config });
}

const api = {
    // ===== 认证 =====
    getPublicKey: () => http('get', '/user/public-key'),
    xsyLogin: (username, casPsd, captchaSessionId, captchaCode) =>
        http('post', '/user/xsy-login', { data: { username, casPsd, captchaSessionId, captchaCode } }),
    smsSend: phone => http('post', '/user/sms/send', { data: { phone } }),
    smsLogin: (smsSessionId, username, phone, smsCode) =>
        http('post', '/user/sms/login', { data: { smsSessionId, username, phone, smsCode } }),
    qrCreate: username => http('post', '/user/qr/create', { data: { username } }),
    qrPoll: qrSessionId => http('post', '/user/qr/poll', { data: { qrSessionId } }),
    bindByPassword: (jwt, casPsd, captchaSessionId, captchaCode) =>
        http('post', '/user/bind/password', { jwt, data: { casPsd, captchaSessionId, captchaCode } }),
    logout: jwt => http('post', '/user/logout', { jwt }),
    unregister: jwt => http('post', '/user/unregister', { jwt }),

    // ===== 用户 =====
    toggleAutoSign: (jwt, isAuto) => http('put', `/user/auto-sign/${isAuto}`, { jwt }),
    setSignDays: (jwt, signDays) => http('put', '/user/sign-days', { jwt, data: { signDays } }),
    getUserInfo: jwt => http('get', '/user/info', { jwt }),
    updateUserInfo: (jwt, data) => {
        const params = {};
        ['name', 'email', 'signStartTime', 'signEndTime'].forEach(k => {
            if (data[k]) params[k] = data[k];
        });
        return http('put', '/user/info/', { jwt, params });
    },

    // ===== 签到 =====
    getAllSigns: (jwt, page, size) => http('get', '/sign/allSign', { jwt, params: { page, size } }),
    oneKeySign: jwt => http('post', '/sign/all', { jwt }),
    signSingle: (jwt, sign) =>
        http('post', '/sign/one', { jwt, params: { id: sign.id, signId: sign.signId, schoolId: sign.schoolId } }),

    // ===== 管理员 =====
    getUserStats: jwt => http('get', '/admin/users/stats', { jwt }),
    getUsersByPage: (jwt, page, size, keyword, filter) =>
        http('get', '/admin/users', { jwt, params: { page, size, keyword, filter } }),
    refreshUserJws: (jwt, username) => http('post', `/admin/refresh-jws/${username}`, { jwt }),
    deleteUser: (jwt, username) => http('delete', `/admin/users/${username}`, { jwt }),
    updateUser: (jwt, username, data) => http('put', `/admin/users/${username}`, { jwt, data }),
    getUserDetail: (jwt, username) => http('get', `/admin/users/${username}/detail`, { jwt }),
    getUserSigns: (jwt, username, limit = 10) =>
        http('get', `/admin/users/${username}/signs`, { jwt, params: { limit } }),
    toggleUserAutoSign: (jwt, username, autoSign) =>
        http('post', `/admin/users/${username}/auto-sign`, { jwt, params: { autoSign } }),
    addUser: (jwt, userData) => http('post', '/admin/users', { jwt, data: userData }),
    signByAdmin: (jwt, username) => http('post', `/sign/all-admin/${username}`, { jwt }),
    signAllUsers: jwt => http('post', '/sign/all-all', { jwt }),

    // ===== 定时任务 =====
    getTaskConfigs: jwt => http('get', '/admin/task-config', { jwt }),
    getTaskConfigByKey: (jwt, taskKey) => http('get', `/admin/task-config/${taskKey}`, { jwt }),
    updateTaskConfig: (jwt, taskKey, data) => http('put', `/admin/task-config/${taskKey}`, { jwt, data }),
    triggerImmediateSchedule: (jwt, sendEmail) =>
        http('post', '/admin/task-config/schedule-users/immediate', { jwt, params: { sendEmail } }),
    getScheduleCalendar: (jwt, year) =>
        http('get', '/admin/task-config/schedule-users/calendar', { jwt, params: { year } }),

    // ===== 公告 =====
    getLatestAnnouncement: jwt => http('get', '/user/announcement/latest', { jwt }),
    getAnnouncements: jwt => http('get', '/admin/announcements', { jwt }),
    addAnnouncement: (jwt, data) => http('post', '/admin/announcements', { jwt, data }),
    updateAnnouncement: (jwt, id, data) => http('put', `/admin/announcements/${id}`, { jwt, data }),
    deleteAnnouncement: (jwt, id) => http('delete', `/admin/announcements/${id}`, { jwt }),

    // ===== Redis 队列 =====
    getRedisQueueInfo: jwt => http('get', '/admin/redis-queue', { jwt }),
    clearRedisQueue: jwt => http('delete', '/admin/redis-queue', { jwt }),

    // ===== 操作日志 =====
    getOperationLogs: (jwt, params) => http('get', '/admin/operation-logs', { jwt, params }),
    deleteOperationLogs: (jwt, ids) => http('delete', '/admin/operation-logs', { jwt, data: { ids } })
};
