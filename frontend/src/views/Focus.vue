<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const userStore = useUserStore()

// Tab切换
const tab = ref('list') // 'list' | 'timer'

// 自习室数据
const rooms = ref([])
const myRooms = ref([])
const filterStatus = ref('all')  // 'all' | '进行中' | '已关闭'

// 过滤后的自习室列表
const filteredRooms = computed(() => {
  if (filterStatus.value === 'all') return rooms.value
  return rooms.value.filter(r => r.status === filterStatus.value)
})

// 计时器
const activeRoom = ref(null)       // 当前选中的自习室
const activeRoomDetail = ref(null) // 自习室详情（含成员列表）
const timerSeconds = ref(0)        // 当前计时秒数
const timerRunning = ref(false)
const prevMinutes = ref(0)         // 之前已上报的分钟数
let timerInterval = null

// 创建自习室弹窗
const showCreate = ref(false)
const createForm = reactive({ name: '', targetMinutes: 60, maxMembers: 5, minStake: 30 })
const creating = ref(false)
const createMsg = ref('')

// 加入确认
const showJoin = ref(null)  // 要加入的房间对象
const joinAmount = ref(0)
const joining = ref(false)

// 结算详情弹窗
const showSettleDetail = ref(null)  // 要查看详情的房间对象

// 格式化成员列表（按学习时长降序）
const sortedMembers = computed(() => {
  const members = activeRoomDetail.value?.members || []
  return [...members].sort((a, b) => b.studyMinutes - a.studyMinutes)
})

// 格式化剩余时间
const timeLeft = (endAt) => {
  const diff = new Date(endAt).getTime() - Date.now()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff / 3600000)
  if (h < 24) return `${h}小时后`
  return `${Math.floor(h / 24)}天${h % 24}小时`
}

// 格式化计时显示 hh:mm:ss
const timerDisplay = computed(() => {
  const h = Math.floor(timerSeconds.value / 3600)
  const m = Math.floor((timerSeconds.value % 3600) / 60)
  const s = timerSeconds.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const totalMinutes = computed(() => prevMinutes.value + Math.floor(timerSeconds.value / 60))
const targetMinutes = computed(() => activeRoom.value?.targetMinutes || 1)
const progressPercent = computed(() => Math.min(100, Math.round((totalMinutes.value / targetMinutes.value) * 100)))

// 加载自习室列表（不传status=全部）
const fetchRooms = async () => {
  try {
    const data = await request.get('/rooms')
    rooms.value = data
    myRooms.value = data.filter(r => r.isJoined)
  } catch (e) {
    console.error('加载自习室失败:', e)
  }
}

// 加载自习室详情（成员列表）
const fetchRoomDetail = async (roomId) => {
  try {
    activeRoomDetail.value = await request.get(`/rooms/${roomId}`)
  } catch (e) {
    console.error('加载详情失败:', e)
  }
}

// 加入自习室
const joinRoom = async () => {
  if (!showJoin.value) return
  joining.value = true
  try {
    const amount = joinAmount.value || showJoin.value.minStake
    await request.post(`/rooms/${showJoin.value.id}/join`, { stakeAmount: amount })
    showJoin.value = null
    await fetchRooms()
  } catch (e) {
    alert(e.response?.data?.message || '加入失败')
  } finally {
    joining.value = false
  }
}

// 开始计时
const startTimer = () => {
  if (!activeRoom.value) return
  timerRunning.value = true
  timerInterval = setInterval(() => {
    timerSeconds.value++
  }, 1000)
}

// 暂停计时（上报当前分钟数）
const pauseTimer = async () => {
  timerRunning.value = false
  clearInterval(timerInterval)
  const mins = Math.floor(timerSeconds.value / 60)
  if (mins > 0) {
    try {
      await request.put(`/rooms/${activeRoom.value.id}/timer`, { minutes: mins })
      prevMinutes.value += mins
      timerSeconds.value = 0
      await fetchRoomDetail(activeRoom.value.id)
    } catch (e) {
      console.error('上报失败:', e)
    }
  }
}

// 选自习室 → 切到计时tab
const selectRoom = async (room) => {
  activeRoom.value = room
  timerSeconds.value = 0
  timerRunning.value = false
  clearInterval(timerInterval)

  const detail = await request.get(`/rooms/${room.id}`)
  activeRoomDetail.value = detail
  const me = userStore.user
  const myMember = detail.members?.find(m => m.user?.id === me?.id)
  prevMinutes.value = myMember?.studyMinutes || 0
  tab.value = 'timer'
}

// 点计时tab：自动选第一个已加入的房间，都没有就显示提示
const goToTimer = async () => {
  tab.value = 'timer'
  if (!activeRoom.value && myRooms.value.length > 0) {
    await selectRoom(myRooms.value[0])
  }
}

// 创建自习室
const createRoom = async () => {
  if (!createForm.name.trim()) return
  creating.value = true
  createMsg.value = ''
  try {
    await request.post('/rooms', { ...createForm })
    showCreate.value = false
    createForm.name = ''
    createMsg.value = ''
    await fetchRooms()
  } catch (e) {
    createMsg.value = e.response?.data?.message || '创建失败'
  } finally {
    creating.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(fetchRooms)
onUnmounted(() => clearInterval(timerInterval))
</script>

<template>
  <div class="focus">
    <!-- 顶栏 -->
    <div class="focus-header">
      <h2>专注自习</h2>
      <button v-if="userStore.isLoggedIn" class="btn-create" @click="showCreate = true">
        <Icon icon="mdi:plus" /> 创建自习室
      </button>
    </div>

    <!-- 主Tab切换 -->
    <div class="tabs">
      <button :class="{ active: tab === 'list' }" @click="tab = 'list'">📋 自习室大厅</button>
      <button :class="{ active: tab === 'timer' }" @click="goToTimer">⏱️ 我的计时</button>
    </div>

    <!-- 自习室大厅 -->
    <div v-if="tab === 'list'" class="room-section">
      <!-- 状态筛选 -->
      <div class="filter-tabs">
        <button :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</button>
        <button :class="{ active: filterStatus === '进行中' }" @click="filterStatus = '进行中'">进行中</button>
        <button :class="{ active: filterStatus === '已关闭' }" @click="filterStatus = '已关闭'">已结束</button>
      </div>

      <div v-if="filteredRooms.length === 0" class="empty">还没有自习室，快来创建第一个吧！</div>

      <div class="room-grid">
        <div v-for="room in filteredRooms" :key="room.id" class="room-card" :class="{ closed: room.status === '已关闭' }">
          <div class="room-card-top">
            <h3>{{ room.name }}</h3>
            <span class="status-tag" :class="room.status === '进行中' ? 'active' : 'closed'">
              {{ room.status === '进行中' ? '进行中' : '已结束' }}
            </span>
          </div>

          <div class="room-info">
            <span><Icon icon="mdi:flag-checkered" /> 目标 {{ room.targetMinutes }} 分钟</span>
            <span><Icon icon="mdi:account-group" /> {{ room.memberCount }}/{{ room.maxMembers }} 人</span>
            <span><Icon icon="mdi:cash" /> 质押 ≥{{ room.minStake }} 分</span>
            <span><Icon icon="mdi:clock-outline" /> {{ room.status === '已关闭' ? '已结束' : '剩余 ' + timeLeft(room.endAt) }}</span>
          </div>
          <div class="room-creator">创建者：{{ room.creator?.nickname }}</div>

          <!-- 进行中：操作按钮 -->
          <div v-if="room.status === '进行中'" class="room-actions">
            <button v-if="!room.isJoined" class="btn-join" @click="showJoin = room">加入自习室</button>
            <button v-else class="btn-enter" @click="selectRoom(room)">进入计时</button>
          </div>

          <!-- 已关闭：结算摘要 + 查看详情 -->
          <div v-else-if="room.settleSummary" class="settle-summary">
            <div class="settle-stats">
              <span class="settle-stat ok">✅ {{ room.settleSummary.qualifiedCount }}人达标</span>
              <span class="settle-stat fail">❌ {{ room.settleSummary.unqualifiedCount }}人未达标</span>
            </div>
            <div class="settle-bonus">
              达标者每人瓜分 <strong>+{{ room.settleSummary.bonusPerPerson }}分</strong>
            </div>
            <div v-if="room.settleSummary.myResult" class="settle-my">
              你：
              <template v-if="room.settleSummary.myResult.qualified">
                <span class="text-ok">达标 ✅ · 返还{{ room.settleSummary.myResult.stakeReturn }}分 + 瓜分{{ room.settleSummary.myResult.bonus }}分</span>
              </template>
              <template v-else>
                <span class="text-fail">未达标 ❌ · 损失{{ room.settleSummary.myResult.stakeLost }}分</span>
              </template>
            </div>
            <button class="btn-detail" @click="showSettleDetail = room">查看详情</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的计时 -->
    <div v-if="tab === 'timer'" class="timer-panel">
      <div v-if="!activeRoom" class="empty" style="padding:40px">
        请先在「自习室大厅」加入一个房间，再点「进入计时」
      </div>
      <template v-else>
      <div class="timer-room-name">{{ activeRoom.name }}</div>

      <!-- 大计时器 -->
      <div class="timer-display">
        <div class="timer-clock">{{ timerDisplay }}</div>
        <div class="timer-label">已累计 {{ totalMinutes }} / {{ targetMinutes }} 分钟</div>

        <!-- 进度条 -->
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>

        <!-- 按钮 -->
        <div class="timer-btns" v-if="activeRoom.status === '进行中'">
          <button v-if="!timerRunning" class="btn-start" @click="startTimer">▶ 开始学习</button>
          <button v-else class="btn-pause" @click="pauseTimer">⏸ 结束报到</button>
        </div>
        <div v-else class="room-closed-hint">该自习室已结束</div>
      </div>

      <!-- 成员排行榜（按学习时长降序） -->
      <div class="member-list">
        <h4>成员排行榜</h4>
        <div
          v-for="(m, i) in sortedMembers"
          :key="m.user?.id"
          class="member-row"
          :class="{ me: m.user?.id === userStore.user?.id, first: i === 0 }"
        >
          <span class="member-rank">{{ i === 0 ? '🏆' : i + 1 }}</span>
          <span class="member-name">{{ m.user?.nickname }}</span>
          <div class="member-bar-wrap">
            <div
              class="member-bar"
              :class="{ full: m.studyMinutes >= targetMinutes }"
              :style="{ width: Math.min(100, Math.round((m.studyMinutes / targetMinutes) * 100)) + '%' }"
            ></div>
          </div>
          <span class="member-mins">{{ m.studyMinutes }}分钟</span>
        </div>
      </div>
      </template>
    </div>

    <!-- 创建自习室弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>创建自习室</h3>
        <input v-model="createForm.name" class="modal-input" placeholder="自习室名称" />
        <div class="modal-row">
          <label>目标时长(分钟)</label>
          <input v-model.number="createForm.targetMinutes" type="number" class="modal-input short" min="10" />
        </div>
        <div class="modal-row">
          <label>人数上限</label>
          <input v-model.number="createForm.maxMembers" type="number" class="modal-input short" min="2" max="20" />
        </div>
        <div class="modal-row">
          <label>最低质押积分</label>
          <input v-model.number="createForm.minStake" type="number" class="modal-input short" min="10" />
        </div>
        <p v-if="createMsg" class="modal-msg">{{ createMsg }}</p>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showCreate = false">取消</button>
          <button class="btn-confirm" :disabled="creating" @click="createRoom">
            {{ creating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加入确认弹窗 -->
    <div v-if="showJoin" class="modal-overlay" @click.self="showJoin = null">
      <div class="modal">
        <h3>加入「{{ showJoin.name }}」</h3>
        <p class="join-info">最低质押 {{ showJoin.minStake }} 积分，不达标将被没收。</p>
        <div class="modal-row">
          <label>质押积分</label>
          <input v-model.number="joinAmount" type="number" class="modal-input short" :placeholder="showJoin.minStake" />
        </div>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showJoin = null">取消</button>
          <button class="btn-confirm" :disabled="joining" @click="joinRoom">
            {{ joining ? '加入中...' : '确认加入' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 结算详情弹窗 -->
    <div v-if="showSettleDetail" class="modal-overlay" @click.self="showSettleDetail = null">
      <div class="modal settle-modal">
        <h3>🏆 「{{ showSettleDetail.name }}」结算结果</h3>
        <div class="settle-overview">
          <span class="settle-stat ok">✅ {{ showSettleDetail.settleSummary.qualifiedCount }}人达标</span>
          <span class="settle-stat fail">❌ {{ showSettleDetail.settleSummary.unqualifiedCount }}人未达标</span>
          <span>瓜分池 {{ showSettleDetail.settleSummary.poolStake }} 分</span>
        </div>

        <table class="settle-table">
          <thead>
            <tr>
              <th>成员</th><th>学习时长</th><th>达标</th><th>质押</th><th>返还</th><th>瓜分</th><th>净收益</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="m in showSettleDetail.members"
              :key="m.user?.id"
              :class="{ 'row-me': m.user?.id === userStore.user?.id }"
            >
              <td>{{ m.user?.nickname }}</td>
              <td>{{ m.studyMinutes }}分钟</td>
              <td>{{ m.qualified ? '✅' : '❌' }}</td>
              <td>{{ m.stakeAmount }}分</td>
              <td>{{ m.qualified ? m.stakeAmount : '0' }}分</td>
              <td>{{ m.qualified ? '+' + showSettleDetail.settleSummary.bonusPerPerson : '—' }}</td>
              <td :class="m.qualified ? 'gain' : 'loss'">
                {{ m.qualified ? '+' + showSettleDetail.settleSummary.bonusPerPerson : '-' + m.stakeAmount }}分
              </td>
            </tr>
          </tbody>
        </table>

        <div class="modal-btns">
          <button class="btn-cancel" @click="showSettleDetail = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.focus { padding: 20px 28px; }

.focus-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.focus-header h2 { font-size: 22px; font-weight: 700; color: #111; }

.btn-create {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #1c1d1c;
  color: #fff;
  border: none;
  border-radius: 10px 0 10px 0;
  font-size: 13px;
  cursor: pointer;
}

.btn-create:hover { background: #333; }

/* Tab */
.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  background: rgba(255,255,255,0.7);
  border-radius: 8px;
  padding: 4px;
  width: fit-content;
}

.tabs button {
  padding: 8px 18px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
}

.tabs button.active {
  background: #fff;
  color: #111;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

/* 状态筛选 */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.filter-tabs button {
  padding: 5px 14px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  color: #888;
}

.filter-tabs button.active {
  background: #1c1d1c;
  color: #fff;
  border-color: #1c1d1c;
}

/* 自习室网格 */
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.room-card {
  background: rgba(255,255,255,0.9);
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.room-card.closed { opacity: 0.85; }

.room-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.room-card h3 {
  font-size: 16px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
}

.status-tag.active { background: #e8f5e9; color: #2E7D32; }
.status-tag.closed { background: #f5f5f5; color: #999; }

.room-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.room-info span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.room-info :deep(.iconify) { font-size: 15px; color: #999; }

.room-creator {
  font-size: 11px;
  color: #aaa;
  margin-bottom: 10px;
}

.room-actions { text-align: right; }

.btn-join, .btn-enter {
  padding: 7px 18px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  font-weight: 500;
}

.btn-join { background: #2E7D32; color: #fff; }
.btn-enter { background: #1c1d1c; color: #fff; }
.btn-join:hover, .btn-enter:hover { opacity: 0.85; }

.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #bbb;
  padding: 60px 0;
  font-size: 14px;
}

/* 结算摘要 */
.settle-summary {
  margin-top: 8px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 12px;
}

.settle-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}

.settle-stat { font-weight: 600; }
.settle-stat.ok { color: #2E7D32; }
.settle-stat.fail { color: #e53e3e; }

.settle-bonus {
  color: #555;
  margin-bottom: 2px;
}

.settle-bonus strong { color: #2E7D32; }

.settle-my {
  color: #666;
  margin-bottom: 8px;
}

.text-ok { color: #2E7D32; }
.text-fail { color: #e53e3e; }

.btn-detail {
  padding: 4px 12px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 11px;
  cursor: pointer;
  color: #666;
}

.btn-detail:hover { border-color: #2E7D32; color: #2E7D32; }

/* 计时面板 */
.timer-panel {
  background: rgba(255,255,255,0.9);
  border-radius: 16px;
  padding: 28px;
  text-align: center;
}

.timer-room-name {
  font-size: 18px;
  font-weight: 700;
  color: #111;
  margin-bottom: 20px;
}

.timer-display { margin-bottom: 28px; }

.timer-clock {
  font-size: 64px;
  font-weight: 200;
  font-family: 'Courier New', monospace;
  color: #1c1d1c;
  letter-spacing: 4px;
}

.timer-label { font-size: 14px; color: #888; margin-top: 4px; }

.progress-bar {
  width: 240px;
  height: 6px;
  background: #eee;
  border-radius: 3px;
  margin: 16px auto;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #2E7D32;
  border-radius: 3px;
  transition: width .5s;
}

.timer-btns { display: flex; gap: 12px; justify-content: center; }

.btn-start, .btn-pause {
  padding: 10px 32px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
  font-weight: 600;
}

.btn-start { background: #2E7D32; color: #fff; }
.btn-pause { background: #e53e3e; color: #fff; }
.btn-start:hover, .btn-pause:hover { opacity: 0.85; }

.room-closed-hint {
  color: #999;
  font-size: 13px;
  padding: 10px;
  background: #fafafa;
  border-radius: 8px;
}

/* 成员排行榜 */
.member-list {
  text-align: left;
  max-width: 500px;
  margin: 0 auto;
}

.member-list h4 {
  font-size: 15px;
  color: #333;
  margin-bottom: 12px;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.member-row.me {
  background: #f0fff4;
  margin: 0 -8px;
  padding: 8px;
  border-radius: 6px;
}

.member-row.first .member-rank { font-size: 16px; }

.member-rank {
  width: 22px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.member-name {
  width: 80px;
  font-size: 13px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-bar-wrap {
  flex: 1;
  height: 6px;
  background: #eee;
  border-radius: 3px;
  overflow: hidden;
}

.member-bar {
  height: 100%;
  background: #2E7D32;
  border-radius: 3px;
  transition: width .5s;
}

.member-bar.full { background: #1B5E20; }

.member-mins {
  width: 56px;
  font-size: 11px;
  color: #999;
  text-align: right;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  width: 360px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}

.modal h3 { font-size: 18px; margin-bottom: 16px; }

.modal-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  margin-bottom: 12px;
}

.modal-input.short { width: 100px; }

.modal-input:focus { border-color: #2E7D32; }

.modal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.modal-row label { font-size: 13px; color: #666; }

.modal-msg { font-size: 12px; color: #e53e3e; margin-bottom: 8px; }

.modal-btns { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }

.btn-cancel, .btn-confirm {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}

.btn-cancel { background: #f0f0f0; color: #666; }
.btn-confirm { background: #1c1d1c; color: #fff; }
.btn-confirm:disabled { opacity: 0.5; }

.join-info { font-size: 13px; color: #888; margin-bottom: 12px; }

/* 结算详情弹窗 */
.settle-modal {
  width: 520px;
  max-height: 70vh;
  overflow-y: auto;
}

.settle-overview {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}

.settle-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin-bottom: 12px;
}

.settle-table th {
  text-align: left;
  padding: 6px 8px;
  border-bottom: 2px solid #eee;
  color: #999;
  font-weight: 600;
  white-space: nowrap;
}

.settle-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #f5f5f5;
  color: #333;
}

.row-me td { background: #f0fff4; font-weight: 500; }

.gain { color: #2E7D32 !important; font-weight: 600; }
.loss { color: #e53e3e !important; font-weight: 600; }
</style>
