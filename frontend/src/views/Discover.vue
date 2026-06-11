<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { imgUrl } from '../utils/image'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

// 帖子列表
const posts = ref([])
const loading = ref(true)
const page = ref(1)
const hasMore = ref(true)

// 发帖弹窗
const showModal = ref(false)
const postContent = ref('')
const posting = ref(false)
const postImage = ref(null)       // 选中的图片文件
const imagePreview = ref('')      // 图片预览URL
const uploading = ref(false)      // 上传中
const uploadedUrl = ref('')       // 上传后的URL
const fileInput = ref(null)       // 文件选择器引用

// 点赞状态：记录当前用户已点赞的帖子ID（reactive 让 Set 能响应式）
const likedPosts = reactive(new Set())

// 图片基础URL（本地开发空字符串，线上指向Render后端）
const IMG_BASE = window.location.hostname === 'localhost' ? '' : 'https://banxue-backend-klem.onrender.com'

// 评论区状态
const expandedComments = reactive(new Set())   // 展开评论的帖子ID
const commentText = ref({})                   // 每个帖子的评论输入 { postId: 'text' }
const commentLoading = ref(false)              // 评论提交中
const postComments = ref({})                  // 每个帖子的评论列表 { postId: [...] }

// 加载帖子
const fetchPosts = async () => {
  try {
    const data = await request.get('/posts', { params: { page: page.value, limit: 12 } })
    if (page.value === 1) {
      posts.value = data.posts
    } else {
      posts.value.push(...data.posts)
    }
    hasMore.value = page.value < data.pages
  } catch (e) {
    console.error('加载帖子失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  if (!hasMore.value || loading.value) return
  page.value++
  await fetchPosts()
}

// 点发帖按钮
const openEditor = () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  showModal.value = true
}

// 发布帖子
const submitPost = async () => {
  if (!postContent.value.trim()) return
  posting.value = true
  try {
    await request.post('/posts', {
      content: postContent.value,
      images: uploadedUrl.value ? [uploadedUrl.value] : []
    })
    showModal.value = false
    postContent.value = ''
    postImage.value = null
    imagePreview.value = ''
    uploadedUrl.value = ''
    page.value = 1
    await fetchPosts()
  } catch (e) {
    console.error('发布失败:', e)
  } finally {
    posting.value = false
  }
}

// 选择图片并预览
const selectImage = (e) => {
  const file = e.target.files[0]
  if (!file) return
  postImage.value = file
  imagePreview.value = URL.createObjectURL(file)
  // 选择后立即上传
  uploadImage(file)
}

// 上传图片到后端
const uploadImage = async (file) => {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const data = await request.post('/upload', formData)
    uploadedUrl.value = data.url
  } catch (e) {
    console.error('上传失败:', e)
    // 上传失败清除预览
    postImage.value = null
    imagePreview.value = ''
  } finally {
    uploading.value = false
  }
}

// 点击遮罩关闭（清空所有状态）
const closeModal = () => {
  showModal.value = false
  postContent.value = ''
  postImage.value = null
  imagePreview.value = ''
  uploadedUrl.value = ''
}

// 点赞/取消点赞
const toggleLike = async (post) => {
  try {
    const data = await request.put(`/posts/${post.id}/like`)
    // 更新帖子的点赞数和状态
    post.likeCount = data.likes
    if (data.isLiked) {
      likedPosts.add(post.id)
    } else {
      likedPosts.delete(post.id)
    }
  } catch (e) {
    console.error('点赞失败:', e)
  }
}

// 切换评论展开 + 拉取评论数据
const toggleComments = async (post) => {
  const pid = post.id
  if (expandedComments.has(pid)) {
    expandedComments.delete(pid)
  } else {
    expandedComments.add(pid)
    // 首次展开时拉取评论
    if (!postComments.value[pid]) {
      try {
        const data = await request.get(`/posts/${pid}`)
        postComments.value = { ...postComments.value, [pid]: data.comments }
      } catch (e) {
        console.error('加载评论失败:', e)
      }
    }
  }
}

// 提交评论
const submitComment = async (post) => {
  const pid = post.id
  const text = (commentText.value[pid] || '').trim()
  if (!text) return
  commentLoading.value = true
  try {
    const data = await request.post(`/posts/${pid}/comments`, { content: text })
    postComments.value = { ...postComments.value, [pid]: data }
    commentText.value = { ...commentText.value, [pid]: '' }
    post.commentCount = data.length
  } catch (e) {
    console.error('评论失败:', e)
  } finally {
    commentLoading.value = false
  }
}

// 时间格式化
const timeAgo = (dateStr) => {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => fetchPosts())
</script>

<template>
  <div class="discover">
    <!-- 顶部标题 -->
    <div class="page-header">
      <h2 class="page-title">发现</h2>
      <p class="page-desc">看看大家都在学什么</p>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">加载中...</div>

    <!-- 瀑布流 -->
    <div v-else class="masonry">
      <article
        v-for="post in posts"
        :key="post.id"
        class="card"
      >
        <!-- 作者信息 -->
        <div class="card-author">
          <img
            v-if="post.author?.avatar"
            :src="imgUrl(post.author.avatar)"
            class="author-avatar"
            @error="e => e.target.style.display = 'none'"
          />
          <span v-else class="author-avatar placeholder">😊</span>
          <div class="author-meta">
            <span class="author-name">{{ post.author?.nickname || '匿名' }}</span>
            <span class="post-time">{{ timeAgo(post.createdAt) }}</span>
          </div>
        </div>

        <!-- 内容 -->
        <p class="card-content">{{ post.content }}</p>

        <!-- 图片 -->
        <img
          v-if="post.images?.[0]"
          :src="imgUrl(post.images[0])"
          class="card-image"
          loading="lazy"
          @error="e => e.target.style.display = 'none'"
        />

        <!-- 底部互动 -->
        <div class="card-footer">
          <span
            class="stat like"
            :class="{ liked: likedPosts.has(post.id) }"
            @click="toggleLike(post)"
          >
            <Icon
              :icon="likedPosts.has(post.id) ? 'mdi:heart' : 'mdi:heart-outline'"
              class="stat-icon"
              :class="{ liked: likedPosts.has(post.id) }"
            />
            {{ post.likeCount }}
          </span>
          <span class="stat cmt-btn" @click="toggleComments(post)">
            <Icon icon="mdi:comment-outline" class="stat-icon" /> {{ post.commentCount }}
          </span>
        </div>

        <!-- 评论区（展开时显示） -->
        <div v-if="expandedComments.has(post.id)" class="comment-section">
          <!-- 已有评论列表 -->
          <div
            v-for="c in (postComments[post.id] || [])"
            :key="c.id"
            class="comment-item"
          >
            <span class="cmt-user">{{ c.user?.nickname || '匿名' }}</span>
            <span class="cmt-content">{{ c.content }}</span>
          </div>
          <!-- 发表评论 -->
          <div class="comment-input-row">
            <input
              v-model="commentText[post.id]"
              class="comment-input"
              placeholder="写评论..."
              @keyup.enter="submitComment(post)"
            />
            <button
              class="cmt-send"
              :disabled="commentLoading"
              @click="submitComment(post)"
            >
              发送
            </button>
          </div>
        </div>
      </article>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more" @click="loadMore">
      加载更多
    </div>

    <!-- 右下角发帖按钮 -->
    <button class="fab" @click="openEditor">+</button>

    <!-- 发帖弹窗 -->
    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal-card">
        <div class="modal-header">
          <span class="modal-title">写点什么</span>
          <span class="modal-close" @click="closeModal">✕</span>
        </div>
        <textarea
          v-model="postContent"
          class="modal-textarea"
          placeholder="分享你的学习心得..."
          maxlength="500"
          rows="5"
        ></textarea>

        <!-- 图片上传 -->
        <div class="upload-area">
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            class="file-input"
            @change="selectImage"
          />
          <div v-if="!imagePreview" class="upload-hint" @click="fileInput?.click()">
            📷 添加图片
          </div>
          <div v-else class="preview-wrap">
            <img :src="imagePreview" class="preview-img" />
            <span v-if="uploading" class="upload-status">上传中...</span>
            <span v-else-if="uploadedUrl" class="upload-status done">✓ 已上传</span>
          </div>
        </div>
        <div class="modal-footer">
          <span class="char-count">{{ postContent.length }}/500</span>
          <button class="btn-publish" :disabled="posting || !postContent.trim()" @click="submitPost">
            {{ posting ? '发布中...' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.discover {
  padding: 8px 16px 24px;
}

/* 页面标题 */
.page-header {
  padding: 16px 4px 20px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #111;
  letter-spacing: -0.5px;
}

.page-desc {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 加载中 */
.loading {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

/* 瀑布流（CSS Columns） */
.masonry {
  column-count: 3;
  column-gap: 14px;
}

/* 卡片 */
.card {
  break-inside: avoid;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: transform .15s;
}

.card:hover {
  transform: translateY(-1px);
}

/* 作者信息 */
.card-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.author-avatar.placeholder {
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.author-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.author-name {
  font-size: 13px;
  font-weight: 600;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-time {
  font-size: 11px;
  color: #aaa;
  margin-top: 1px;
}

/* 内容 */
.card-content {
  font-size: 14px;
  line-height: 1.65;
  color: #333;
  margin-bottom: 10px;
  word-break: break-word;
}

/* 图片 */
.card-image {
  width: 100%;
  border-radius: 8px;
  margin-bottom: 10px;
  display: block;
}

/* 底部互动 */
.card-footer {
  display: flex;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}

.stat {
  font-size: 12px;
  color: #999;
}

.stat.like {
  cursor: pointer;
  transition: transform .1s;
  user-select: none;
}

.stat.like:hover {
  transform: scale(1.15);
}

.stat-icon {
  font-size: 16px;
  vertical-align: -2px;
}

.stat-icon.liked {
  color: #e53e3e;
}

.stat.cmt-btn {
  cursor: pointer;
}

.stat.cmt-btn:hover {
  color: #2E7D32;
}

/* 评论区 */
.comment-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.comment-item {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.cmt-user {
  color: #2E7D32;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}

.cmt-content {
  color: #555;
  word-break: break-word;
}

.comment-input-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.comment-input {
  flex: 1;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #eee;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
}

.comment-input:focus {
  border-color: #2E7D32;
}

.cmt-send {
  height: 32px;
  padding: 0 14px;
  background: #2E7D32;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.cmt-send:disabled {
  opacity: 0.5;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 16px;
  color: #2E7D32;
  font-size: 13px;
  cursor: pointer;
  margin-top: 8px;
}

.load-more:hover {
  text-decoration: underline;
}

/* 右下角浮动发帖按钮 */
.fab {
  position: fixed;
  bottom: 28px;
  right: 28px;
  width: 52px;
  height: 52px;
  border-radius: 16px 0 16px 0;
  background: #1c1d1c;
  color: #fff;
  border: none;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0,0,0,0.3);
  z-index: 200;
  transition: transform .15s;
}

.fab:hover {
  transform: scale(1.06);
}

/* 弹窗遮罩 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
  padding: 20px;
}

/* 弹窗卡片 */
.modal-card {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 420px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.modal-title {
  font-size: 17px;
  font-weight: 700;
  color: #111;
}

.modal-close {
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.modal-close:hover {
  color: #333;
}

.modal-textarea {
  width: 100%;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  outline: none;
  font-family: inherit;
  transition: border .15s;
}

.modal-textarea:focus {
  border-color: #2E7D32;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.char-count {
  font-size: 12px;
  color: #bbb;
}

/* 图片上传 */
.upload-area {
  margin-top: 10px;
}

.file-input {
  display: none;
}

.upload-hint {
  padding: 10px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  text-align: center;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: border-color .15s;
}

.upload-hint:hover {
  border-color: #2E7D32;
  color: #2E7D32;
}

.preview-wrap {
  position: relative;
}

.preview-img {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: 8px;
  display: block;
}

.upload-status {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
}

.upload-status.done {
  background: #2E7D32;
}

.btn-publish {
  height: 38px;
  padding: 0 22px;
  background: #1c1d1c;
  color: #fff;
  border: none;
  border-radius: 12px 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform .15s;
}

.btn-publish:hover:not(:disabled) {
  transform: translateY(-1px);
}

.btn-publish:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 768px) {
  .masonry {
    column-count: 2;
    column-gap: 10px;
  }
  .card {
    margin-bottom: 10px;
  }
}

@media (max-width: 480px) {
  .masonry {
    column-count: 1;
  }
}
</style>

