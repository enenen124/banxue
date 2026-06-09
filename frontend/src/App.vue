<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './stores/user'
import { onMounted } from 'vue'
import AiChat from './components/AiChat.vue'
import { imgUrl } from './utils/image'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 页面启动时：如果有token，恢复用户信息
onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchProfile()
  }
})

// 顶部导航链接
const navLinks = [
  { path: '/discover', label: '发现' },
  { path: '/focus',    label: '专注' },
  { path: '/mine',     label: '我的' }
]

// 跳转页面
const goTo = (path) => router.push(path)
</script>

<template>
  <div class="app">
    <!-- 顶部导航栏 -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <!-- Logo -->
        <img class="logo" src="@/assets/Logo.svg" alt="伴学" @click="goTo('/')" />

        <!-- 中间导航链接 -->
        <nav class="nav-links">
          <span
            v-for="link in navLinks"
            :key="link.path"
            class="nav-item"
            :class="{ active: route.path === link.path }"
            @click="goTo(link.path)"
          >
            {{ link.label }}
          </span>
        </nav>

        <!-- 右侧：已登录显示头像+昵称，未登录显示登录按钮 -->
        <div class="right-area">
          <div v-if="userStore.isLoggedIn && userStore.user" class="user-info">
            <img
              v-if="userStore.user.avatar"
              :src="imgUrl(userStore.user.avatar)"
              class="user-avatar"
              @error="e => e.target.style.display = 'none'"
            />
            <span v-else class="user-avatar placeholder">😊</span>
            <span class="user-name">{{ userStore.user.nickname }}</span>
            <a v-if="userStore.user.role === 'admin'" class="admin-link" @click="goTo('/admin')">管理</a>
          </div>
          <button v-else class="login-btn" @click="goTo('/login')">登录</button>
        </div>
      </div>
    </header>

    <!-- 页面内容区 -->
    <main class="main">
      <router-view />
    </main>
    <!-- AI 学习助手（全局浮动气泡） -->
    <AiChat />
  </div>
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background-image: url('@/assets/bg.svg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;   /* 背景固定，不随滚动 */
}

.app {
  min-height: 100vh;
}

/* 顶部导航栏 — 毛玻璃效果 */
.top-bar {
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.7);            /* 半透明白底 */
  backdrop-filter: blur(10px);                      /* 毛玻璃模糊 */
  -webkit-backdrop-filter: blur(10px);             /* Safari兼容 */
  z-index: 100;
  border-radius: 16px;                              /* 圆角 */
  margin: 12px 16px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);          /* 极淡阴影 */
}

.top-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
}

/* Logo */
.logo {
  height: 28px;
  cursor: pointer;
  flex-shrink: 0;
}

/* 中间导航链接 */
.nav-links {
  display: flex;
  gap: 32px;
}

.nav-item {
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
  position: relative;
}

.nav-item:hover {
  color: #000;
}

.nav-item.active {
  color: #000;
  font-weight: 600;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 18px;
  height: 3px;
  background: #2E7D32;
  border-radius: 2px;
}

/* 右侧区域 */
.right-area {
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar.placeholder {
  background: #f0f0f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-link {
  font-size: 12px;
  color: #1565c0;
  background: #e3f2fd;
  padding: 2px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  text-decoration: none;
}
.admin-link:hover { background: #bbdefb; }

.login-btn {
  background: #1c1d1c;                              /* 深色灰底按钮 */
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.login-btn:hover {
  background: #1B5E20;
}

/* 内容区 */
.main {
  padding: 16px;
}
</style>

