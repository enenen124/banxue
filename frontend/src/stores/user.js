import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export const useUserStore = defineStore('user', () => {
  // --- 状态 ---
  const user = ref(null)          // 用户信息
  const token = ref(localStorage.getItem('token') || '')

  // --- 计算属性 ---
  const isLoggedIn = computed(() => !!token.value)

  // --- 注册 ---
  const register = async (username, password) => {
    const data = await request.post('/auth/register', { username, password })
    token.value = data.token
    user.value = data
    localStorage.setItem('token', data.token)
    return data
  }

  // --- 登录 ---
  const login = async (username, password) => {
    const data = await request.post('/auth/login', { username, password })
    token.value = data.token
    user.value = data
    localStorage.setItem('token', data.token)
    return data
  }

  // --- 退出登录 ---
  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  // --- 获取个人信息 ---
  const fetchProfile = async () => {
    user.value = await request.get('/users/profile')
  }

  return { user, token, isLoggedIn, register, login, logout, fetchProfile }
})
