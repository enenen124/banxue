import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'

const app = createApp(App)

app.use(createPinia())  // 注册状态管理
app.use(router)         // 注册路由

app.mount('#app')
