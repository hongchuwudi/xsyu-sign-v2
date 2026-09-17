import { defineStore } from 'pinia'

// 未登录时的默认值：保持对象形态（而非 null），
// 避免退出登录时页面组件重渲染瞬间读到 null 崩溃
const EMPTY_USER_INFO = {
  id: '',
  name: '',
  username: '',
  email: '',
  jwt: '',
  autoSign: false,
  signDays: '',
  role: 'USER'
}

// 全局用户状态：登录信息持久化到 localStorage，刷新页面不丢
export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null') || { ...EMPTY_USER_INFO }
  }),

  getters: {
    isLoggedIn: state => !!state.userInfo?.jwt,
    jwt: state => state.userInfo?.jwt || '',
    role: state => state.userInfo?.role || 'USER'
  },

  actions: {
    setUserInfo(info) {
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    clear() {
      this.userInfo = { ...EMPTY_USER_INFO }
      localStorage.removeItem('userInfo')
    }
  }
})
