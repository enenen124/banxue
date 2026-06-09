// 处理图片URL：绝对路径直接用，相对路径拼接Render后端地址
const API_BASE = window.location.hostname === 'localhost' ? '' : 'https://banxue-backend-klem.onrender.com'

export const imgUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return API_BASE + path
}
