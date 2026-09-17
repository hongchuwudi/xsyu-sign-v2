// 用户操作共享逻辑：三个点菜单（UserMenuButton）与"我的"页面（MineView）复用同一套动作，
// 保证两处行为一致；各组件自行实例化（页面互斥挂载，状态天然隔离）
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { api } from '@/utils/api'
import { showMessage } from '@/utils/message'
import { rsaEncrypt } from '@/utils/crypto'

export function useUserActions() {
  const router = useRouter()
  const userStore = useUserStore()
  const userInfo = computed(() => userStore.userInfo)

  const isLoading = ref(false)
  const captchaSession = ref(null)
  const showEditProfileModal = ref(false)
  const showBindModal = ref(false)
  const showUnregisterConfirmModal = ref(false)

  function updateUserInfo(partial) {
    userStore.setUserInfo({ ...userInfo.value, ...partial })
  }

  // ==================== 动作 ====================

  async function toggleAutoSign() {
    const newAutoSign = !userInfo.value.autoSign
    try {
      await api.toggleAutoSign(newAutoSign)
      updateUserInfo({ autoSign: newAutoSign })
      showMessage(`自动签到已${newAutoSign ? '开启' : '关闭'}`)
    } catch (e) {
      showMessage(e.response?.data?.message || '修改失败', 'error')
    }
  }

  async function handleUpdateUserInfo(data) {
    isLoading.value = true
    try {
      const res = await api.updateUserInfo(data)
      if (res.data.code !== 200) {
        return showMessage(res.data.message || '更新失败', 'error')
      }
      if (data.signDays) await api.setSignDays(data.signDays)
      updateUserInfo({
        name: data.name || userInfo.value.name,
        email: data.email || userInfo.value.email,
        signStartTime: data.signStartTime || userInfo.value.signStartTime,
        signEndTime: data.signEndTime || userInfo.value.signEndTime,
        signDays: data.signDays || userInfo.value.signDays
      })
      showEditProfileModal.value = false
      showMessage('个人信息更新成功')
    } catch (e) {
      showMessage(e.response?.data?.message || '更新失败', 'error')
    } finally {
      isLoading.value = false
    }
  }

  async function handleBindByPassword({ casPsd, captchaCode }) {
    isLoading.value = true
    try {
      const publicKeyRes = await api.getPublicKey()
      const enc = rsaEncrypt(casPsd, publicKeyRes.data.data.publicKey)
      const res = await api.bindByPassword(
        enc,
        captchaSession.value ? captchaSession.value.captchaSessionId : null,
        captchaCode || null
      )
      if (res.data.code === 200) {
        userStore.setUserInfo(res.data.data)
        closeBindModal()
        showMessage('学校密码已更新，JWS续签已启用')
      } else if (res.data.code === 1004 && res.data.data?.captchaSessionId) {
        captchaSession.value = res.data.data
        showMessage('学校系统要求输入验证码', 'error')
      } else {
        showMessage(res.data.message || '更新失败', 'error')
      }
    } catch (e) {
      showMessage(e.response?.data?.message || '更新失败', 'error')
    } finally {
      isLoading.value = false
    }
  }

  async function handleLogout() {
    try { await api.logout() } catch { /* 忽略退出失败 */ }
    userStore.clear()
    showMessage('已退出登录')
    router.push('/login')
  }

  async function handleUnregister() {
    try { await api.unregister() } catch { /* 忽略 */ }
    userStore.clear()
    showMessage('注销信息成功')
    router.push('/login')
  }

  // ==================== 弹窗开关 ====================

  function openEditProfile() {
    showEditProfileModal.value = true
  }

  function openBindModal() {
    captchaSession.value = null
    showBindModal.value = true
  }

  function closeBindModal() {
    showBindModal.value = false
    captchaSession.value = null
  }

  // 关于软件：独立路由页面
  function goAbout() {
    router.push('/about')
  }

  function openUnregisterConfirm() {
    showUnregisterConfirmModal.value = true
  }

  return {
    userInfo, isLoading, captchaSession,
    showEditProfileModal, showBindModal, showUnregisterConfirmModal,
    toggleAutoSign, handleUpdateUserInfo, handleBindByPassword, handleLogout, handleUnregister,
    openEditProfile, openBindModal, closeBindModal, goAbout, openUnregisterConfirm
  }
}
