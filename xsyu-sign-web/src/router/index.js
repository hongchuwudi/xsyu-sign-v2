import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 页面按 公共 / 用户 / 管理员 三大类组织，模块陆续迁移中，路由逐步接入
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/common/login/LoginView.vue')
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/user/home/HomeView.vue')
  }
  // —— 用户 ——
  // path: '/sign'           -> @/views/user/sign/SignListView.vue
  // —— 管理员 ——
  // path: '/admin/users'    -> @/views/admin/users/UsersView.vue
  // path: '/admin/task'     -> @/views/admin/task/TaskConfigView.vue
  // path: '/admin/queue'    -> @/views/admin/queue/RedisQueueView.vue
  // path: '/admin/announcement' -> @/views/admin/announcement/AnnouncementView.vue
  // path: '/admin/logs'     -> @/views/admin/operation-logs/OperationLogsView.vue
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 未登录时只允许访问登录页
router.beforeEach(to => {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn && to.name !== 'login') return { name: 'login' }
})

export default router
