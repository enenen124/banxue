<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单数据
const username = ref('')
const password = ref('')

// true=注册模式，false=登录模式
const isRegister = ref(false)

// 密码是否可见
const showPwd = ref(false)

// 提交中状态（防止重复点击）
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')    // 成功提示

// 提交表单
const submit = async () => {
  if (!username.value.trim() || !password.value.trim()) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''

  try {
    if (isRegister.value) {
      await userStore.register(username.value, password.value)
      successMsg.value = '🎉 注册成功！即将跳转...'
    } else {
      await userStore.login(username.value, password.value)
      successMsg.value = '👋 登录成功！即将跳转...'
    }
    // 1秒后跳转
    setTimeout(() => router.push('/discover'), 1000)
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '操作失败，请重试'
  } finally {
    loading.value = false
  }
}

// 切换登录/注册模式
const toggleMode = () => {
  isRegister.value = !isRegister.value
  errorMsg.value = ''
}

// GitHub登录 — 支持真实OAuth和模拟模式
const githubLoading = ref(false)
const githubLogin = async () => {
  const API_BASE = window.location.hostname === 'localhost' ? '' : 'https://banxue-backend-klem.onrender.com'
  githubLoading.value = true
  errorMsg.value = ''

  try {
    const resp = await fetch(API_BASE + '/api/auth/github', { redirect: 'manual' })

    // 如果返回 302（重定向），说明是真实 OAuth 模式，跟随跳转
    if (resp.type === 'opaqueredirect' || resp.status === 302) {
      window.location.href = API_BASE + '/api/auth/github'
      return
    }

    // 如果返回 JSON（模拟模式），直接处理
    if (resp.ok) {
      const data = await resp.json()
      if (data.token) {
        userStore.token = data.token
        localStorage.setItem('token', data.token)
        successMsg.value = '👋 GitHub登录成功！即将跳转...'
        setTimeout(() => router.push('/discover'), 1000)
      } else {
        errorMsg.value = 'GitHub登录返回异常'
      }
    } else {
      errorMsg.value = 'GitHub登录失败'
    }
  } catch (e) {
    errorMsg.value = 'GitHub登录失败，请重试'
  } finally {
    githubLoading.value = false
  }
}

// 页面加载时检查URL参数（GitHub回调回来的token或error）
onMounted(async () => {
  const urlToken = route.query.token
  const urlError = route.query.error

  if (urlError) {
    errorMsg.value = urlError
    // 清除URL参数，保持地址栏干净
    router.replace({ query: {} })
    return
  }

  if (urlToken) {
    // 拿到token → 存入store和localStorage → 拉取用户信息 → 跳转
    userStore.token = urlToken
    localStorage.setItem('token', urlToken)
    successMsg.value = '👋 GitHub登录成功！即将跳转...'
    // 拉取昵称和头像，导航栏才能显示
    await userStore.fetchProfile()
    setTimeout(() => router.push('/discover'), 1000)
  }
})
</script>

<template>
  <div class="login-page">
    <!-- Logo -->
    <img class="logo" src="@/assets/Logo.svg" alt="伴学" @click="router.push('/')" />

    <!-- 标题 -->
    <h2 class="title">{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>

    <!-- 表单 -->
    <form class="form" @submit.prevent="submit">
      <!-- 用户名 -->
      <input
        v-model="username"
        class="input"
        type="text"
        placeholder="用户名"
        autocomplete="username"
      />

      <!-- 密码 -->
      <div class="pwd-wrap">
        <input
          v-model="password"
          class="input"
          :type="showPwd ? 'text' : 'password'"
          placeholder="密码"
          autocomplete="current-password"
        />
        <span class="pwd-toggle" @click="showPwd = !showPwd">
          {{ showPwd ? '👁️' : '👁️‍🗨️' }}
        </span>
      </div>

      <!-- 提示信息 -->
      <p v-if="errorMsg" class="msg error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="msg success">{{ successMsg }}</p>

      <!-- 提交按钮 -->
      <button class="btn-submit" :disabled="loading">
        {{ loading ? '处理中...' : (isRegister ? '注 册' : '登 录') }}
      </button>
    </form>

    <!-- 分割线 -->
    <div class="divider"><span>或者</span></div>

    <!-- GitHub 登录 -->
    <button class="btn-github" @click="githubLogin">
      <svg class="github-icon" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.387.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.73.083-.73 1.205.085 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 21.795 24 17.295 24 12 24 5.37 18.63 0 12 0z"/>
      </svg>
      GitHub 登录 
    </button>

    <!-- 底部切换 -->
    <p class="switch-text">
      {{ isRegister ? '已有账号？' : '还没有账号？' }}
      <a class="switch-link" @click="toggleMode">{{ isRegister ? '去登录' : '去注册' }}</a>
    </p>
  </div>
</template>

<style scoped>
.login-page {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: calc(100vh - 68px);   /* 扣掉导航栏 */
  justify-content: center;
}

/* Logo */
.logo {
  height: 48px;
  margin-bottom: 32px;
  cursor: pointer;
}

/* 标题 */
.title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 32px;
}

/* 表单 */
.form {
  width: 100%;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 输入框 */
.input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  transition: border .2s;
  background: rgba(255,255,255,0.9);
}

.input:focus {
  border-color: #2E7D32;
}

/* 密码框 + 眼睛 */
.pwd-wrap {
  position: relative;
}

.pwd-toggle {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  font-size: 18px;
  user-select: none;
}

/* 提示信息 */
.msg {
  font-size: 13px;
  text-align: center;
  padding: 8px 12px;
  border-radius: 8px;
}

.msg.error {
  color: #e53e3e;
  background: #fff5f5;
}

.msg.success {
  color: #2E7D32;
  background: #f0fff4;
}

/* 提交按钮 — 深色不对称圆角 */
.btn-submit {
  width: 100%;
  height: 48px;
  background: #1c1d1c;
  color: #fff;
  border: none;
  border-radius: 16px 0 16px 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3), 0 1px 3px rgba(0,0,0,0.2);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;
  margin-top: 6px;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.4), 0 2px 4px rgba(0,0,0,0.3);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 分割线 */
.divider {
  width: 100%;
  max-width: 340px;
  text-align: center;
  color: #aaa;
  font-size: 13px;
  margin: 24px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: #e0e0e0;
}

.divider::before { left: 0; }
.divider::after  { right: 0; }

/* GitHub 按钮 */
.btn-github {
  width: 100%;
  max-width: 340px;
  height: 48px;
  background: #24292e;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  opacity: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: transform .2s;
}

.github-icon {
  width: 20px;
  height: 20px;
}

/* 底部切换 */
.switch-text {
  margin-top: 24px;
  font-size: 14px;
  color: #666;
}

.switch-link {
  color: #2E7D32;
  cursor: pointer;
  font-weight: 600;
}

.switch-link:hover {
  text-decoration: underline;
}
</style>
