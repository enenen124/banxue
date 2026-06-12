<script setup>
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import { imgUrl } from '../utils/image'

const userStore = useUserStore()
const tab = ref('users')

// 用户列表
const users = ref([])
const loadingUsers = ref(false)

const fetchUsers = async () => {
  loadingUsers.value = true
  try { users.value = await request.get('/admin/users') } catch (e) { console.error('获取用户失败:', e.message) }
  loadingUsers.value = false
}

const updateUser = async (id, data) => {
  try {
    await request.put(`/admin/users/${id}`, data)
    await fetchUsers()
  } catch (e) { alert(e.response?.data?.message || '操作失败') }
}

// 自习室列表
const rooms = ref([])
const loadingRooms = ref(false)

const fetchRooms = async () => {
  loadingRooms.value = true
  try { rooms.value = await request.get('/admin/rooms') } catch (e) { console.error('获取自习室失败:', e.message) }
  loadingRooms.value = false
}

const adminSettle = async (id) => {
  try {
    const res = await request.post(`/admin/rooms/${id}/settle`)
    alert(`结算完成！达标${res.qualifiedCount}人，未达标${res.unqualifiedCount}人`)
    await fetchRooms()
  } catch (e) { alert(e.response?.data?.message || '结算失败') }
}

const adminDeleteRoom = async (id) => {
  if (!confirm('确定删除该自习室？')) return
  try { await request.delete(`/admin/rooms/${id}`); await fetchRooms() } catch {}
}

// 帖子列表
const posts = ref([])
const loadingPosts = ref(false)
const postSearch = ref('')

const fetchPosts = async () => {
  loadingPosts.value = true
  try { posts.value = await request.get('/admin/posts') } catch (e) { console.error('获取帖子失败:', e.message) }
  loadingPosts.value = false
}

const filteredPosts = () => {
  if (!postSearch.value.trim()) return posts.value
  const keyword = postSearch.value.toLowerCase()
  return posts.value.filter(p =>
    p.content?.toLowerCase().includes(keyword) ||
    p.author?.nickname?.toLowerCase().includes(keyword)
  )
}

const adminDeletePost = async (id) => {
  if (!confirm('确定删除该帖子及所有评论？')) return
  try { await request.delete(`/admin/posts/${id}`); await fetchPosts() } catch {}
}

// 帖子编辑弹窗
const showPostEdit = ref(null)
const postEditContent = ref('')
const openPostEdit = (post) => {
  showPostEdit.value = post
  postEditContent.value = post.content
}
const savePostEdit = async () => {
  try {
    const res = await request.put(`/admin/posts/${showPostEdit.value.id}`, { content: postEditContent.value })
    showPostEdit.value.content = res.content || postEditContent.value
    showPostEdit.value = null
  } catch (e) { alert(e.response?.data?.message || '保存失败') }
}

// 积分编辑弹窗
const showPointsEdit = ref(null)
const pointsValue = ref(0)
const openPointsEdit = (user) => { showPointsEdit.value = user; pointsValue.value = user.points }
const savePoints = async () => {
  await updateUser(showPointsEdit.value.id, { points: pointsValue.value })
  showPointsEdit.value = null
}

onMounted(() => fetchUsers())
</script>

<template>
  <div class="admin">
    <h2>🛡️ 管理后台</h2>

    <!-- Tab -->
    <div class="tabs">
      <button :class="{ active: tab === 'users' }" @click="tab = 'users'; fetchUsers()">用户管理</button>
      <button :class="{ active: tab === 'rooms' }" @click="tab = 'rooms'; fetchRooms()">自习室管理</button>
      <button :class="{ active: tab === 'posts' }" @click="tab = 'posts'; fetchPosts()">内容审核</button>
    </div>

    <!-- 用户管理 -->
    <div v-if="tab === 'users'" class="panel">
      <table>
        <thead>
          <tr>
            <th>头像</th><th>昵称</th><th>积分</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id" :class="{ banned: u.status === 1 }">
            <td><img v-if="u.avatar" :src="imgUrl(u.avatar)" class="avatar" @error="e => e.target.style.display = 'none'" /><span v-else>—</span></td>
            <td>{{ u.nickname }}</td>
            <td>{{ u.points }}</td>
            <td><span class="tag" :class="u.role">{{ u.role === 'admin' ? '管理员' : '用户' }}</span></td>
            <td><span class="tag" :class="u.status === 1 ? 'banned-tag' : 'normal-tag'">{{ u.status === 1 ? '封禁' : '正常' }}</span></td>
            <td>{{ new Date(u.createdAt).toLocaleDateString() }}</td>
            <td class="actions">
              <button class="btn-sm" @click="openPointsEdit(u)">修改积分</button>
              <button v-if="u.role !== 'admin'" class="btn-sm danger" @click="updateUser(u.id, { status: u.status === 1 ? 0 : 1 })">
                {{ u.status === 1 ? '解封' : '封禁' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 自习室管理 -->
    <div v-if="tab === 'rooms'" class="panel">
      <div v-for="r in rooms" :key="r.id" class="room-card">
        <h3>{{ r.name }} <span class="tag" :class="r.status === '已关闭' ? 'closed' : ''">{{ r.status }}</span></h3>
        <div class="room-info">
          创建者：{{ r.creator?.nickname }} | 人数：{{ r.memberCount }} | 目标：{{ r.targetMinutes }}分钟
        </div>
        <div class="room-actions">
          <button v-if="r.status === '进行中'" class="btn-sm" @click="adminSettle(r.id)">强制结算</button>
          <button class="btn-sm danger" @click="adminDeleteRoom(r.id)">删除</button>
        </div>
      </div>
    </div>

    <!-- 内容审核 -->
    <div v-if="tab === 'posts'" class="panel">
      <div class="post-toolbar">
        <input v-model="postSearch" class="search-input" placeholder="搜索帖子内容或作者..." />
        <span class="post-count">共 {{ filteredPosts().length }} 条帖子</span>
      </div>
      <div v-if="loadingPosts" class="loading">加载中...</div>
      <div v-else-if="filteredPosts().length === 0" class="empty-tip">暂无帖子</div>
      <div v-else v-for="p in filteredPosts()" :key="p.id" class="post-card">
        <div class="post-header">
          <span class="post-author">{{ p.author?.nickname }}</span>
          <span class="post-time">{{ new Date(p.createdAt).toLocaleString() }}</span>
        </div>
        <div class="post-content">{{ p.content }}</div>
        <div v-if="p.images?.length" class="post-images">
          <img v-for="(img, i) in p.images" :key="i" :src="imgUrl(img)" @error="e => e.target.style.display = 'none'" />
        </div>
        <div class="post-footer">
          <div class="post-stats">💬{{ p.commentCount }} ❤️{{ p.likeCount }}</div>
          <div class="post-actions">
            <button class="btn-sm" @click="openPostEdit(p)">编辑</button>
            <button class="btn-sm danger" @click="adminDeletePost(p.id)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 积分修改弹窗 -->
    <div v-if="showPointsEdit" class="modal-overlay" @click.self="showPointsEdit = null">
      <div class="modal">
        <h3>修改 {{ showPointsEdit.nickname }} 的积分</h3>
        <input v-model.number="pointsValue" type="number" class="modal-input" />
        <div class="modal-btns">
          <button class="btn-cancel" @click="showPointsEdit = null">取消</button>
          <button class="btn-confirm" @click="savePoints">保存</button>
        </div>
      </div>
    </div>

    <!-- 帖子编辑弹窗 -->
    <div v-if="showPostEdit" class="modal-overlay" @click.self="showPostEdit = null">
      <div class="modal modal-wide">
        <h3>编辑帖子</h3>
        <p class="edit-author">作者：{{ showPostEdit.author?.nickname }}</p>
        <textarea v-model="postEditContent" class="modal-textarea" rows="6" placeholder="帖子内容..."></textarea>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showPostEdit = null">取消</button>
          <button class="btn-confirm" @click="savePostEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin { padding: 20px 28px; }
.admin h2 { font-size: 22px; margin-bottom: 20px; color: #111; }

.tabs { display: flex; gap: 4px; margin-bottom: 20px; background: rgba(255,255,255,0.7); border-radius: 8px; padding: 4px; width: fit-content; }
.tabs button { padding: 8px 18px; border: none; background: transparent; border-radius: 6px; font-size: 13px; cursor: pointer; color: #666; }
.tabs button.active { background: #fff; color: #111; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.08); }

.panel { background: rgba(255,255,255,0.9); border-radius: 12px; padding: 20px; }

/* 用户表 */
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th, td { padding: 10px 8px; text-align: left; border-bottom: 1px solid #f0f0f0; }
th { color: #999; font-weight: 500; font-size: 12px; }
tr.banned { background: #fff5f5; }
.avatar { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; }
.tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.tag.admin { background: #e3f2fd; color: #1565c0; }
.tag.user { background: #f5f5f5; color: #666; }
.normal-tag { background: #e8f5e9; color: #2e7d32; }
.banned-tag { background: #ffebee; color: #c62828; }
.closed { background: #eceff1; color: #78909c; }
.actions { white-space: nowrap; }
.btn-sm { padding: 4px 12px; border: none; border-radius: 6px; font-size: 11px; cursor: pointer; background: #e0e0e0; color: #333; margin-right: 4px; }
.btn-sm:hover { background: #ccc; }
.btn-sm.danger { background: #ffebee; color: #c62828; }
.btn-sm.danger:hover { background: #ffcdd2; }

/* 自习室卡片 */
.room-card { border: 1px solid #f0f0f0; border-radius: 10px; padding: 14px; margin-bottom: 10px; }
.room-card h3 { font-size: 15px; margin-bottom: 6px; }
.room-info { font-size: 12px; color: #999; margin-bottom: 8px; }
.room-actions { display: flex; gap: 6px; }

/* 帖子卡片 */
.post-toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.search-input { flex: 1; height: 36px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px; outline: none; }
.search-input:focus { border-color: #2E7D32; }
.post-count { font-size: 12px; color: #999; white-space: nowrap; }
.loading, .empty-tip { text-align: center; padding: 30px; color: #999; font-size: 14px; }
.post-card { border: 1px solid #f0f0f0; border-radius: 10px; padding: 14px; margin-bottom: 10px; }
.post-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.post-author { font-weight: 600; font-size: 13px; }
.post-time { font-size: 11px; color: #999; }
.post-content { font-size: 13px; margin-bottom: 6px; white-space: pre-wrap; word-break: break-all; }
.post-images { display: flex; gap: 8px; margin-bottom: 6px; }
.post-images img { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; }
.post-footer { display: flex; justify-content: space-between; align-items: center; }
.post-stats { font-size: 12px; color: #999; }
.post-actions { display: flex; gap: 6px; }

/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.3); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: #fff; border-radius: 14px; padding: 24px; width: 320px; }
.modal h3 { font-size: 16px; margin-bottom: 12px; }
.modal-input { width: 100%; height: 40px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; margin-bottom: 12px; }
.modal-btns { display: flex; gap: 8px; justify-content: flex-end; }
.btn-cancel, .btn-confirm { padding: 8px 20px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-confirm { background: #1c1d1c; color: #fff; }
.modal-wide { width: 480px; }
.edit-author { font-size: 12px; color: #999; margin-bottom: 10px; }
.modal-textarea { width: 100%; padding: 10px 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; resize: vertical; margin-bottom: 12px; font-family: inherit; }
.modal-textarea:focus { border-color: #2E7D32; }
</style>
