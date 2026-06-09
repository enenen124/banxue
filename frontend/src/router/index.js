import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Welcome',
    component: () => import('../views/Welcome.vue')  // 游客访问页
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('../views/Discover.vue') // 发现页
  },
  {
    path: '/focus',
    name: 'Focus',
    component: () => import('../views/Focus.vue')    // 专注页（自习室）
  },
  {
    path: '/mine',
    name: 'Mine',
    component: () => import('../views/Mine.vue')     // 我的页
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue')   // 管理后台
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
