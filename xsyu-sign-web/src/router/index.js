import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 页面按 公共 / 用户 / 管理员 三大类组织，模块陆续迁移中，路由逐步接入
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/common/login/LoginView.vue')
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('@/views/common/about/AboutView.vue')
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/user/home/HomeView.vue')
  },
  // —— 用户 ——
  {
    path: '/announcement',
    name: 'announcement',
    component: () => import('@/views/user/announcement/AnnouncementView.vue')
  },
  {
    path: '/sign',
    name: 'sign',
    component: () => import('@/views/user/sign/SignListView.vue')
  },
  {
    path: '/mine',
    name: 'mine',
    component: () => import('@/views/user/mine/MineView.vue')
  },
  // —— 管理员 ——
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    redirect: '/admin/users',
    children: [
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/users/UsersView.vue')
      },
      {
        path: 'task',
        name: 'admin-task',
        component: () => import('@/views/admin/task/TaskConfigView.vue')
      },
      {
        path: 'queue',
        name: 'admin-queue',
        component: () => import('@/views/admin/queue/RedisQueueView.vue')
      },
      {
        path: 'announcement',
        name: 'admin-announcement',
        component: () => import('@/views/admin/announcement/AnnouncementView.vue')
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/operation-logs/OperationLogsView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 登录、角色与默认落地页统一在路由层处理，刷新页面也能保持一致行为
router.beforeEach(to => {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn && to.name !== 'login') return { name: 'login' }
  if (userStore.isLoggedIn && to.name === 'login') {
    return userStore.role === 'ADMIN' ? { name: 'admin-users' } : { name: 'home' }
  }
  if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') return { name: 'home' }
  if (to.name === 'home' && userStore.role === 'ADMIN') return { name: 'admin-users' }
})

export default router
