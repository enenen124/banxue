import axios from 'axios'

// 判断当前环境：本地开发走Vite代理，线上直接请求Render后端
const API_BASE = window.location.hostname === 'localhost'
  ? '/api'
  : 'https://banxue-backend-klem.onrender.com/api'

// 创建 axios 实例，设置后端地址
const request = axios.create({
  baseURL: API_BASE,
  timeout: 10000 // 10秒超时
})

// 请求拦截器：自动给每个请求加上 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  response => response.data,  // 直接返回 data，少写一层 .data
  error => {
    const message = error.response?.data?.message || '请求失败'
    console.error('请求错误:', message)
    return Promise.reject(error)
  }
)

export default request
