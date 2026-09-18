import axios from 'axios'

// 开发环境走 vite 代理（相对路径），生产环境与后端同源部署，也无需配置
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截：自动携带 JWT
request.interceptors.request.use(config => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (userInfo?.jwt) config.headers.Authorization = `Bearer ${userInfo.jwt}`
  return config
})

// 响应拦截：401 自动登出
request.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('userInfo')
      window.location.reload()
    }
    return Promise.reject(error)
  }
)

export default request

export const api = {
  // ===== 认证（登录，学号即账号） =====
  getPublicKey: () => request.get('/user/public-key'),
  xsyLogin: (username, casPsd, captchaSessionId, captchaCode) => request.post('/user/xsy-login', { username, casPsd, captchaSessionId, captchaCode }),
  smsSend: phone => request.post('/user/sms/send', { phone }),
  smsLogin: (smsSessionId, username, phone, smsCode) => request.post('/user/sms/login', { smsSessionId, username, phone, smsCode }),
  qrCreate: username => request.post('/user/qr/create', { username }),
  qrPoll: qrSessionId => request.post('/user/qr/poll', { qrSessionId }),
  // 更新学校密码（登录态；触发验证码返回 1004）
  bindByPassword: (casPsd, captchaSessionId, captchaCode) =>
    request.post('/user/bind/password', { casPsd, captchaSessionId, captchaCode }),

  // ===== 用户 =====
  getUserInfo: () => request.get('/user/info'),
  toggleAutoSign: isAuto => request.put(`/user/auto-sign/${isAuto}`),
  setSignDays: signDays => request.put('/user/sign-days', { signDays }),
  updateUserInfo: data => {
    const params = {}
    ;['name', 'email', 'signStartTime', 'signEndTime'].forEach(k => {
      if (data[k]) params[k] = data[k]
    })
    return request.put('/user/info/', null, { params })
  },
  logout: () => request.post('/user/logout'),
  unregister: () => request.post('/user/unregister'),

  // ===== 签到 =====
  getAllSigns: (page, size) => request.get('/sign/allSign', { params: { page, size } }),
  oneKeySign: () => request.post('/sign/all'),
  signSingle: sign =>
    request.post('/sign/one', null, {
      params: { id: sign.id, signId: sign.signId, schoolId: sign.schoolId }
    }),

  // ===== 管理员：用户管理 =====
  getUserStats: () => request.get('/admin/users/stats'),
  getUsersByPage: (page, size, keyword = '', filter = '') =>
    request.get('/admin/users', { params: { page, size, keyword, filter } }),
  refreshUserJws: username => request.post(`/admin/refresh-jws/${username}`),
  deleteUser: username => request.delete(`/admin/users/${username}`),
  updateUser: (username, data) => request.put(`/admin/users/${username}`, data),
  getUserDetail: username => request.get(`/admin/users/${username}/detail`),
  getUserSigns: (username, limit = 10) =>
    request.get(`/admin/users/${username}/signs`, { params: { limit } }),
  toggleUserAutoSign: (username, autoSign) =>
    request.post(`/admin/users/${username}/auto-sign`, null, { params: { autoSign } }),
  addUser: data => request.post('/admin/users', data),
  signByAdmin: username => request.post(`/sign/all-admin/${username}`),
  signAllUsers: () => request.post('/sign/all-all'),

  // ===== 管理员：定时任务 =====
  getTaskConfigs: () => request.get('/admin/task-config'),
  getTaskConfigByKey: taskKey => request.get(`/admin/task-config/${taskKey}`),
  updateTaskConfig: (taskKey, data) => request.put(`/admin/task-config/${taskKey}`, data),
  triggerImmediateSchedule: sendEmail =>
    request.post('/admin/task-config/schedule-users/immediate', null, { params: { sendEmail } }),
  getScheduleCalendar: year =>
    request.get('/admin/task-config/schedule-users/calendar', { params: { year } }),

  // ===== 管理员：邮件通知 =====
  getEmailTemplates: () => request.get('/admin/email/templates'),
  addEmailTemplate: data => request.post('/admin/email/templates', data),
  updateEmailTemplate: (id, data) => request.put(`/admin/email/templates/${id}`, data),
  deleteEmailTemplate: id => request.delete(`/admin/email/templates/${id}`),
  getEmailGroups: () => request.get('/admin/email/groups'),
  addEmailGroup: data => request.post('/admin/email/groups', data),
  updateEmailGroup: (id, data) => request.put(`/admin/email/groups/${id}`, data),
  deleteEmailGroup: id => request.delete(`/admin/email/groups/${id}`),
  searchEmailUsers: (keyword = '') => request.get('/admin/email/users', { params: { keyword } }),
  getEmailTasks: () => request.get('/admin/email/tasks'),
  getEmailTaskRecipients: id => request.get(`/admin/email/tasks/${id}/recipients`),
  sendEmailNow: data => request.post('/admin/email/tasks/send-now', data),
  scheduleEmail: data => request.post('/admin/email/tasks/schedule', data),
  cancelEmailTask: id => request.post(`/admin/email/tasks/${id}/cancel`),

  // ===== 管理员：Redis 队列 =====
  getRedisQueueInfo: () => request.get('/admin/redis-queue'),
  clearRedisQueue: () => request.delete('/admin/redis-queue'),

  // ===== 管理员：公告 =====
  getAnnouncements: () => request.get('/admin/announcements'),
  addAnnouncement: data => request.post('/admin/announcements', data),
  updateAnnouncement: (id, data) => request.put(`/admin/announcements/${id}`, data),
  deleteAnnouncement: id => request.delete(`/admin/announcements/${id}`),
  uploadImage: (file, draftToken) => {
    const formData = new FormData()
    formData.append('file', file)
    if (draftToken) formData.append('draftToken', draftToken)
    return request.post('/admin/assets/images', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000 })
  },

  // ===== 管理员：操作日志 =====
  getOperationLogs: params => request.get('/admin/operation-logs', { params }),
  deleteOperationLogs: ids => request.delete('/admin/operation-logs', { data: { ids } }),

  // ===== 公告 =====
  getLatestAnnouncement: () => request.get('/user/announcement/latest')
}
