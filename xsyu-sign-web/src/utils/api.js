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

// 示例接口（按需扩展）
export const api = {
  getPublicKey: () => request.get('/user/public-key')
}
