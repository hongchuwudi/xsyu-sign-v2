import { defineStore } from 'pinia'

// 全局用户状态：登录信息持久化到 localStorage，刷新页面不丢
export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
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
      this.userInfo = null
      localStorage.removeItem('userInfo')
    }
  }
})
