<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 两行文字
const line1 = ref('')
const line2 = ref('')
const showCursor1 = ref(true)   // 第一行光标
const showCursor2 = ref(false)  // 第二行光标

// 打字机效果
const typeText = (text, targetRef, speed, callback) => {
  let i = 0
  const timer = setInterval(() => {
    targetRef.value = text.slice(0, i + 1) // 每次多取一个字
    i++
    if (i >= text.length) {
      clearInterval(timer)
      if (callback) callback()
    }
  }, speed)
}

onMounted(() => {
  // 打第一行
  typeText('聚一起，更好学。', line1, 120, () => {
    showCursor1.value = false  // 第一行打完，关光标
    // 停顿0.5秒后打第二行
    setTimeout(() => {
      showCursor2.value = true
      typeText('在这里，你能寻找你的专注搭子，创建线上自习室，获得独一无二的链上NFT，快来加入我们吧！', line2, 60, () => {
        showCursor2.value = false  // 全部打完，关光标
      })
    }, 500)
  })
})
</script>

<template>
  <div class="welcome">
    <!-- 主标题区域 -->
    <section class="hero">
      <img class="hero-logo" src="@/assets/Logo.svg" alt="伴学" />
      <p class="hero-subtitle">
        {{ line1 }}<span v-if="showCursor1" class="typing-cursor">|</span>
      </p>
      <p class="hero-subtitle">
        {{ line2 }}<span v-if="showCursor2" class="typing-cursor">|</span>
      </p>

      <!-- 按钮 -->
      <div class="actions">
        <button class="btn-primary" @click="router.push('/login')">快速开始</button>
      </div>
    </section>

    <!-- 特色介绍 -->
    <section class="features">
      <div class="feature-card">
        <span class="feature-icon">🔍</span>
        <h3>发现好帖</h3>
        <p>浏览全网学习心得，交流经验</p>
      </div>
      <div class="feature-card">
        <span class="feature-icon">🎯</span>
        <h3>专注自习</h3>
        <p>质押积分加入自习室，计时专注</p>
      </div>
      <div class="feature-card">
        <span class="feature-icon">🏆</span>
        <h3>成就体系</h3>
        <p>完成挑战解锁成就，数据可视化</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.welcome {
  padding: 0 20px 40px;
  text-align: center;
  min-height: 100vh;
}

/* 主标题 */
.hero {
  min-height: calc(100vh - 80px);  /* 撑满整屏，扣掉导航栏高度 */
  display: flex;
  flex-direction: column;
  justify-content: center;          /* 内容垂直居中 */
  align-items: center;              /* 内容水平居中 */
}

.hero-logo {
  height: 80px;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 16px;
  color: #000;
  line-height: 1.8;
  min-height: 29px;
}

/* 打字光标闪烁 */
.typing-cursor {
  color: #2E7D32;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 特色卡片 */
.features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 48px;
}

.feature-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: left;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.feature-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 12px;
}

.feature-card h3 {
  font-size: 17px;
  color: #333;
  margin-bottom: 6px;
}

.feature-card p {
  font-size: 14px;
  color: #999;
}

/* 按钮容器 — 回到正常文档流 */
.actions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}

/* 快速开始按钮 — 深色 + 不对称圆角 + 阴影 */
.btn-primary {
  height: 48px;
  width: 280px;
  padding: 0 36px;
  display: flex;
  align-items: center;
  justify-content: center;

  background: #1c1d1c;                    /* 深灰色底 */
  color: #fff;
  border: none;
  border-radius: 16px 0 16px 0;           /* 左上右下圆角，右上左下直角 */
  box-shadow:
    0 4px 12px rgba(0,0,0,0.3),
    0 1px 3px rgba(0,0,0,0.2);

  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover {
  transform: translateY(-2px);             /* 悬浮上浮 */
  box-shadow:
    0 6px 16px rgba(0,0,0,0.4),
    0 2px 4px rgba(0,0,0,0.3);
}

.btn-primary:active {
  transform: translateY(0);
  opacity: 0.9;
}
</style>
