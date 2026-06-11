<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Icon } from '@iconify/vue'
import VChart from 'vue-echarts'
import 'echarts'
import { BrowserProvider, Contract, formatEther } from 'ethers'
import { useUserStore } from '../stores/user'
import { imgUrl } from '../utils/image'
import request from '../utils/request'

const userStore = useUserStore()

// 图片基础URL（本地开发空字符串，线上指向Render后端）
const IMG_BASE = window.location.hostname === 'localhost' ? '' : 'https://banxue-backend-klem.onrender.com'

// 当前选中的标签
const currentTab = ref('profile')

// 侧边栏导航项（icon 使用 Material Design Icons）
const tabs = [
  { key: 'profile',    label: '个人资料', icon: 'mdi:account-edit' },
  { key: 'stats',      label: '学习数据', icon: 'mdi:chart-bar' },
  { key: 'favorites',  label: '我的点赞', icon: 'mdi:heart-outline' },
  { key: 'todos',      label: '待办事项', icon: 'mdi:checkbox-marked-circle-outline' },
  { key: 'achievements', label: '我的成就', icon: 'mdi:trophy-outline' }
]

// --- 个人资料 ---
const profile = ref({})
const editNickname = ref('')
const editAvatar = ref('')
const avatarUploading = ref(false)
const myPosts = ref([])           // 自己发布的帖子

// 密码修改
const oldPwd = ref('')
const newPwd = ref('')
const pwdMsg = ref('')

// --- 学习数据 ---
const stats = ref({})
const chartData = ref({ daily: [], hourly: [], calendar: [] })

// --- AI分析 ---
const aiLoading = ref(false)
const aiResult = ref('')

// ECharts 图表配置
const dailyOption = ref({})
const hourlyOption = ref({})
const roomOption = ref({})
const calendarOption = ref({})

// --- 点赞 ---
const likedPosts = ref([])

// --- 待办 ---
const todos = ref([])
const newTodo = ref('')

// --- 成就 ---
const achievements = ref([])

// --- Web3 ---
const CONTRACT_ADDRESS = '0xA928CD1fd83Bc8b422c3fca8027caF0496db0E33'
const CONTRACT_ABI = [
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "to",
				"type": "address"
			},
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "approve",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [],
		"stateMutability": "nonpayable",
		"type": "constructor"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "sender",
				"type": "address"
			},
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			},
			{
				"internalType": "address",
				"name": "owner",
				"type": "address"
			}
		],
		"name": "ERC721IncorrectOwner",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "operator",
				"type": "address"
			},
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "ERC721InsufficientApproval",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "approver",
				"type": "address"
			}
		],
		"name": "ERC721InvalidApprover",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "operator",
				"type": "address"
			}
		],
		"name": "ERC721InvalidOperator",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "owner",
				"type": "address"
			}
		],
		"name": "ERC721InvalidOwner",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "receiver",
				"type": "address"
			}
		],
		"name": "ERC721InvalidReceiver",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "sender",
				"type": "address"
			}
		],
		"name": "ERC721InvalidSender",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "ERC721NonexistentToken",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "uint8",
				"name": "achievementId",
				"type": "uint8"
			}
		],
		"name": "mint",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "owner",
				"type": "address"
			}
		],
		"name": "OwnableInvalidOwner",
		"type": "error"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "account",
				"type": "address"
			}
		],
		"name": "OwnableUnauthorizedAccount",
		"type": "error"
	},
	{
		"anonymous": false,
		"inputs": [
			{
				"indexed": true,
				"internalType": "address",
				"name": "user",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "uint8",
				"name": "achievementId",
				"type": "uint8"
			},
			{
				"indexed": true,
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "AchievementMinted",
		"type": "event"
	},
	{
		"anonymous": false,
		"inputs": [
			{
				"indexed": true,
				"internalType": "address",
				"name": "owner",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "address",
				"name": "approved",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "Approval",
		"type": "event"
	},
	{
		"anonymous": false,
		"inputs": [
			{
				"indexed": true,
				"internalType": "address",
				"name": "owner",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "address",
				"name": "operator",
				"type": "address"
			},
			{
				"indexed": false,
				"internalType": "bool",
				"name": "approved",
				"type": "bool"
			}
		],
		"name": "ApprovalForAll",
		"type": "event"
	},
	{
		"anonymous": false,
		"inputs": [
			{
				"indexed": true,
				"internalType": "address",
				"name": "previousOwner",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "address",
				"name": "newOwner",
				"type": "address"
			}
		],
		"name": "OwnershipTransferred",
		"type": "event"
	},
	{
		"inputs": [],
		"name": "renounceOwnership",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "from",
				"type": "address"
			},
			{
				"internalType": "address",
				"name": "to",
				"type": "address"
			},
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "safeTransferFrom",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "from",
				"type": "address"
			},
			{
				"internalType": "address",
				"name": "to",
				"type": "address"
			},
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			},
			{
				"internalType": "bytes",
				"name": "data",
				"type": "bytes"
			}
		],
		"name": "safeTransferFrom",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "operator",
				"type": "address"
			},
			{
				"internalType": "bool",
				"name": "approved",
				"type": "bool"
			}
		],
		"name": "setApprovalForAll",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"anonymous": false,
		"inputs": [
			{
				"indexed": true,
				"internalType": "address",
				"name": "from",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "address",
				"name": "to",
				"type": "address"
			},
			{
				"indexed": true,
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "Transfer",
		"type": "event"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "from",
				"type": "address"
			},
			{
				"internalType": "address",
				"name": "to",
				"type": "address"
			},
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "transferFrom",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "newOwner",
				"type": "address"
			}
		],
		"name": "transferOwnership",
		"outputs": [],
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "owner",
				"type": "address"
			}
		],
		"name": "balanceOf",
		"outputs": [
			{
				"internalType": "uint256",
				"name": "",
				"type": "uint256"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "getApproved",
		"outputs": [
			{
				"internalType": "address",
				"name": "",
				"type": "address"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "user",
				"type": "address"
			}
		],
		"name": "getMintedAchievements",
		"outputs": [
			{
				"internalType": "uint8[]",
				"name": "",
				"type": "uint8[]"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "",
				"type": "address"
			},
			{
				"internalType": "uint8",
				"name": "",
				"type": "uint8"
			}
		],
		"name": "hasMinted",
		"outputs": [
			{
				"internalType": "bool",
				"name": "",
				"type": "bool"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "owner",
				"type": "address"
			},
			{
				"internalType": "address",
				"name": "operator",
				"type": "address"
			}
		],
		"name": "isApprovedForAll",
		"outputs": [
			{
				"internalType": "bool",
				"name": "",
				"type": "bool"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [],
		"name": "name",
		"outputs": [
			{
				"internalType": "string",
				"name": "",
				"type": "string"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [],
		"name": "owner",
		"outputs": [
			{
				"internalType": "address",
				"name": "",
				"type": "address"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "ownerOf",
		"outputs": [
			{
				"internalType": "address",
				"name": "",
				"type": "address"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "bytes4",
				"name": "interfaceId",
				"type": "bytes4"
			}
		],
		"name": "supportsInterface",
		"outputs": [
			{
				"internalType": "bool",
				"name": "",
				"type": "bool"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [],
		"name": "symbol",
		"outputs": [
			{
				"internalType": "string",
				"name": "",
				"type": "string"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint256",
				"name": "",
				"type": "uint256"
			}
		],
		"name": "tokenAchievement",
		"outputs": [
			{
				"internalType": "uint8",
				"name": "",
				"type": "uint8"
			}
		],
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint256",
				"name": "tokenId",
				"type": "uint256"
			}
		],
		"name": "tokenURI",
		"outputs": [
			{
				"internalType": "string",
				"name": "",
				"type": "string"
			}
		],
		"stateMutability": "view",
		"type": "function"
	}
]

const walletAddress = ref('')           // 当前连接的钱包地址
const walletBalance = ref('')            // Sepolia ETH 余额
const mintingId = ref(null)              // 正在铸造的成就ID（null=没在铸造）
const mintedMap = ref({})               // 已铸造记录 { 0: { txHash, mintedAt } }
const mintTxHashes = ref({})            // 铸造后展示的交易哈希 { 0: '0x...' }

// 成就在配置列表中对应的索引
const ACH_ID_MAP = { first_focus: 0, focus_10h: 1, points_100: 2, first_post: 3, likes_10: 4 }

// 连接MetaMask
const connectWallet = async () => {
  if (!window.ethereum) {
    alert('请先安装 MetaMask 浏览器插件')
    return
  }
  try {
    const accounts = await window.ethereum.request({ method: 'eth_requestAccounts' })
    walletAddress.value = accounts[0]

    // 切换到Sepolia网络
    await window.ethereum.request({
      method: 'wallet_switchEthereumChain',
      params: [{ chainId: '0xaa36a7' }] // Sepolia chainId = 11155111
    })

    // 查询余额
    const provider = new BrowserProvider(window.ethereum)
    const balance = await provider.getBalance(walletAddress.value)
    walletBalance.value = formatEther(balance)
  } catch (e) {
    if (e.code === 4902) {
      alert('请先在 MetaMask 中添加 Sepolia 测试网络')
    } else {
      console.error('连接钱包失败:', e)
    }
  }
}

// 铸造NFT
const mintNFT = async (ach) => {
  if (!walletAddress.value) {
    alert('请先连接 MetaMask 钱包')
    return
  }

  const achId = ACH_ID_MAP[ach.code]
  if (achId === undefined) return

  mintingId.value = achId
  try {
    const provider = new BrowserProvider(window.ethereum)
    const signer = await provider.getSigner()
    const contract = new Contract(CONTRACT_ADDRESS, CONTRACT_ABI, signer)

    // 调用合约的 mint 函数
    const tx = await contract.mint(achId)
    await tx.wait() // 等待链上确认

    // 保存铸造记录到后端
    await request.post('/achievements/mint', {
      achievementCode: ach.code,
      txHash: tx.hash
    })

    mintTxHashes.value = { ...mintTxHashes.value, [achId]: tx.hash }
    mintedMap.value = { ...mintedMap.value, [achId]: { txHash: tx.hash, mintedAt: new Date().toISOString() } }

    alert(`🎉 铸造成功！交易哈希：${tx.hash}`)
  } catch (e) {
    console.error('铸造失败:', e)
    alert('铸造失败：' + (e.reason || e.message || '未知错误'))
  } finally {
    mintingId.value = null
  }
}

// 加载数据
const loadData = async () => {
  try {
    const [profileData, statsData, likedData, todoData, postsData, achData] = await Promise.all([
      request.get('/users/profile'),
      request.get('/users/stats'),
      request.get('/users/likes'),
      request.get('/todos'),
      request.get('/users/posts'),
      request.get('/users/achievements') // 完整成就列表（含解锁状态）
    ])
    profile.value = profileData
    editNickname.value = profileData.nickname || ''
    stats.value = statsData
    chartData.value = statsData.chartData || { daily: [], hourly: [], calendar: [] }
    buildCharts()
    likedPosts.value = likedData
    todos.value = todoData
    myPosts.value = postsData
    achievements.value = achData // 完整成就列表

    // 获取铸造记录
    try {
      const records = await request.get('/achievements/mint-records')
      const map = {}
      records.forEach(r => { const idx = ACH_ID_MAP[r.achievementCode]; if (idx !== undefined) map[idx] = r })
      mintedMap.value = map
    } catch {}
  } catch (e) {
    console.error('加载失败:', e)
  }
}

// 上传头像
const uploadAvatar = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const data = await request.post('/upload', formData)
    editAvatar.value = data.url
    // 立即保存头像
    await request.put('/users/profile', { avatar: data.url })
    profile.value.avatar = data.url
  } catch (err) {
    console.error('上传失败:', err)
  } finally {
    avatarUploading.value = false
  }
}

// 保存昵称
const saveNickname = async () => {
  if (!editNickname.value.trim()) return
  try {
    await request.put('/users/profile', { nickname: editNickname.value })
    profile.value.nickname = editNickname.value
  } catch (e) {
    console.error('保存失败:', e)
  }
}

// 修改密码
const changePassword = async () => {
  if (!oldPwd.value || !newPwd.value) {
    pwdMsg.value = '请填写旧密码和新密码'
    return
  }
  try {
    await request.put('/users/password', { oldPassword: oldPwd.value, newPassword: newPwd.value })
    pwdMsg.value = '✅ 密码修改成功'
    oldPwd.value = ''
    newPwd.value = ''
  } catch (e) {
    pwdMsg.value = e.response?.data?.message || '修改失败'
  }
}

// ==================== ECharts图表构建 ====================
// 根据后端返回的chartData生成四个图表配置
const buildCharts = () => {
  const cd = chartData.value

  // 1. 每日时长趋势（折线图）
  dailyOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 20, bottom: 24 },
    xAxis: { type: 'category', data: cd.daily.map(d => d.date), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '分钟', axisLabel: { fontSize: 11 } },
    series: [{
      type: 'line',
      data: cd.daily.map(d => d.minutes),
      smooth: true,
      lineStyle: { color: '#2E7D32', width: 2 },
      itemStyle: { color: '#2E7D32' },
      areaStyle: { color: 'rgba(46,125,50,0.08)' }
    }]
  }

  // 2. 时段分布（柱状图）
  hourlyOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 20, bottom: 24 },
    xAxis: { type: 'category', data: cd.hourly.map(h => h.hour), axisLabel: { fontSize: 10, rotate: 45 } },
    yAxis: { type: 'value', name: '次', axisLabel: { fontSize: 11 } },
    series: [{
      type: 'bar',
      data: cd.hourly.map(h => h.count),
      itemStyle: {
        color: '#4CAF50',
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }

  // 3. 达标率（环形图）
  const qualifiedCount = stats.value.qualifiedCount || 0
  const unqualifiedCount = stats.value.unqualifiedCount || 0
  roomOption.value = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: [
        { value: qualifiedCount, name: '达标', itemStyle: { color: '#4CAF50' } },
        { value: unqualifiedCount, name: '未达标', itemStyle: { color: '#FFA726' } }
      ]
    }]
  }

  // 4. 学习打卡日历（热力图）—— 浅色 GitHub 深色主题风格
  const maxVal = Math.max(...cd.calendar.map(([, v]) => v), 1)
  calendarOption.value = {
    backgroundColor: '#f9fbf9',                // 整体浅色背景
    tooltip: {
      formatter: (params) => params.value[0]
        ? `${params.value[0]}<br/>${params.value[1]} 分钟`
        : '暂无记录'
    },
    // 图例：右下角横向色阶，深绿→亮绿
    visualMap: {
      min: 0,
      max: maxVal,
      orient: 'horizontal',
      right: 10,
      bottom: 6,
      text: ['More', 'Less'],
      textStyle: { color: '#999', fontSize: 10 },
      inRange: { color: ['#145228', '#289949', '#62e272'] },
      calculable: false,
      showLabel: false,
      itemWidth: 14,
      itemHeight: 11
    },
    // 日历坐标系
    calendar: {
      range: new Date().getFullYear(),
      firstDay: 1,
      cellSize: [15, 15],
      dayLabel: { fontSize: 9, margin: 6, color: '#aaa' },
      monthLabel: { fontSize: 10, margin: 6, color: '#aaa' },
      yearLabel: { show: false },
      // 无数据格：非常浅的绿色
      itemStyle: {
        color: '#ebf2eb',
        borderRadius: 3,
        borderWidth: 2,
        borderColor: '#f9fbf9'
      },
      // 去掉坐标轴网格线
      splitLine: { show: false }
    },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: cd.calendar }]
  }
}

// ==================== AI学习分析（SSE 流式） ====================
const aiAnalyze = async () => {
  aiLoading.value = true
  aiResult.value = ''
  try {
    const { totalMinutes, totalHours, roomCount, qualifiedCount, unqualifiedCount, postCount } = stats.value
    const daily = chartData.value.daily

    const API_BASE = window.location.hostname === 'localhost' ? '' : 'https://banxue-backend-klem.onrender.com'
    const token = localStorage.getItem('token')

    const response = await fetch(API_BASE + '/api/ai/analysis', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ totalMinutes, totalHours, roomCount, qualifiedCount, unqualifiedCount, postCount, daily })
    })

    if (!response.ok) {
      aiResult.value = 'AI分析暂时不可用，请稍后再试~'
      aiLoading.value = false
      return
    }

    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('text/event-stream')) {
      // SSE 流式解析
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          aiLoading.value = false
          break
        }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop()

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed.startsWith('data:')) continue
          const payload = trimmed.slice(5).trim()
          if (!payload || payload === '[DONE]') continue
          try {
            const json = JSON.parse(payload)
            if (json.error) {
              aiResult.value = '⚠️ ' + json.error
              continue
            }
            let content = json.reply || json.choices?.[0]?.delta?.content || json.analysis || ''
            if (content) {
              aiResult.value += content
            }
          } catch { /* 跳过解析失败的行 */ }
        }
      }
    } else {
      // 普通 JSON 响应（兼容旧版后端）
      const data = await response.json()
      aiResult.value = data.analysis || data.suggestion || data.message || '暂无分析数据'
      aiLoading.value = false
    }
  } catch (e) {
    aiResult.value = 'AI分析暂时不可用，请稍后再试~'
    aiLoading.value = false
  }
}

// 添加待办
const addTodo = async () => {
  if (!newTodo.value.trim()) return
  try {
    const data = await request.post('/todos', { title: newTodo.value })
    todos.value.unshift(data)
    newTodo.value = ''
  } catch (e) {
    console.error('添加失败:', e)
  }
}

// 切换待办完成状态
const toggleTodo = async (todo) => {
  try {
    const data = await request.put(`/todos/${todo.id}`, { completed: !todo.completed })
    Object.assign(todo, data)
  } catch (e) {
    console.error('操作失败:', e)
  }
}

// 退出登录
const logout = () => {
  userStore.logout()
  window.location.href = '/'
}

onMounted(loadData)
</script>

<template>
  <div class="mine">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <!-- 头像区 -->
      <div class="sidebar-header">
        <img
          v-if="profile.avatar"
          :src="imgUrl(profile.avatar)"
          class="sidebar-avatar"
        />
        <span v-else class="sidebar-avatar placeholder">😊</span>
        <div class="sidebar-name">{{ profile.nickname || '用户' }}</div>
      </div>

      <!-- 导航 -->
      <nav class="sidebar-nav">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="sidebar-item"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          <Icon :icon="tab.icon" class="sidebar-icon" />
          {{ tab.label }}
        </button>
      </nav>

      <!-- 退出 -->
      <button class="btn-logout" @click="logout">退出登录</button>
    </aside>

    <!-- 右侧内容区 -->
    <main class="content">

      <!-- 个人资料 -->
      <section v-if="currentTab === 'profile'" class="panel">
        <h3>个人资料</h3>

        <!-- 头像 -->
        <div class="avatar-edit">
          <img
            v-if="profile.avatar"
            :src="imgUrl(profile.avatar)"
            class="profile-avatar"
          />
          <span v-else class="profile-avatar placeholder">😊</span>
          <label class="avatar-upload-btn">
            {{ avatarUploading ? '上传中...' : '更换头像' }}
            <input type="file" accept="image/*" hidden @change="uploadAvatar" />
          </label>
        </div>

        <div class="info-grid">
          <div class="info-item"><label>用户名</label><span>{{ profile.username }}</span></div>
          <div class="info-item">
            <label>昵称</label>
            <div class="inline-edit">
              <input v-model="editNickname" class="edit-input" />
              <button class="btn-save" @click="saveNickname">保存</button>
            </div>
          </div>
          <div class="info-item"><label>积分</label><span>{{ profile.points }}</span></div>
          <div class="info-item"><label>注册时间</label><span>{{ new Date(profile.createdAt).toLocaleDateString('zh-CN') }}</span></div>
        </div>

        <!-- 密码修改（仅密码登录用户可见） -->
        <div v-if="profile.username && !profile.githubId" class="pwd-section">
          <h4>修改密码</h4>
          <input v-model="oldPwd" type="password" class="edit-input" placeholder="旧密码" />
          <input v-model="newPwd" type="password" class="edit-input" placeholder="新密码" />
          <button class="btn-save" @click="changePassword">修改密码</button>
          <p v-if="pwdMsg" class="pwd-msg">{{ pwdMsg }}</p>
        </div>

        <!-- 我发布的帖子 -->
        <div class="my-posts-section">
          <h4>我发布的帖子</h4>
          <div v-if="myPosts.length === 0" class="empty">还没有发布过帖子</div>
          <div v-else class="masonry2">
            <div v-for="post in myPosts" :key="post.id" class="mine-card">
              <img v-if="post.images?.[0]" :src="imgUrl(post.images[0])" class="mine-card-img" @error="e => e.target.style.display = 'none'" />
              <p class="mine-card-content">{{ post.content }}</p>
              <div class="mine-card-footer">
                <span class="mine-card-stat"><Icon icon="mdi:heart-outline" class="mine-stat-icon" /> {{ post.likeCount }}</span>
                <span class="mine-card-stat"><Icon icon="mdi:comment-outline" class="mine-stat-icon" /> {{ post.commentCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 学习数据 -->
      <section v-if="currentTab === 'stats'" class="panel">
        <h3>学习数据</h3>

        <!-- 数据卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalMinutes || 0 }}</div>
            <div class="stat-label">总专注分钟</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalHours || 0 }}</div>
            <div class="stat-label">总时长(小时)</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.roomCount || 0 }}</div>
            <div class="stat-label">参与自习室</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.postCount || 0 }}</div>
            <div class="stat-label">发布帖子</div>
          </div>
        </div>

        <!-- AI 学习分析 -->
        <div class="ai-section">
          <button class="btn-ai" @click="aiAnalyze" :disabled="aiLoading">
            {{ aiLoading ? '分析中...' : 'AI数据学习分析' }}
          </button>
          <div v-if="aiResult" class="ai-result">{{ aiResult }}</div>
        </div>

        <!-- 图表区域 -->
        <div class="chart-section">
          <!-- 每日时长趋势 -->
          <div class="chart-box">
            <h4 class="chart-title">近7天学习时长</h4>
            <v-chart class="chart" :option="dailyOption" autoresize />
          </div>

          <!-- 时段分布 -->
          <div class="chart-box">
            <h4 class="chart-title">学习时段分布</h4>
            <v-chart class="chart" :option="hourlyOption" autoresize />
          </div>

          <!-- 达标率 -->
          <div class="chart-box">
            <h4 class="chart-title">自习室达标率</h4>
            <v-chart class="chart" :option="roomOption" autoresize />
          </div>

          <!-- 学习打卡日历 -->
          <div class="chart-box">
            <h4 class="chart-title">坚持打卡日历</h4>
            <v-chart class="chart" :option="calendarOption" autoresize />
          </div>
        </div>
      </section>

      <!-- 我的点赞 -->
      <section v-if="currentTab === 'favorites'" class="panel">
        <h3>我的点赞</h3>
        <div v-if="likedPosts.length === 0" class="empty">还没有点赞帖子</div>
        <div v-else class="masonry2">
          <div v-for="post in likedPosts" :key="post.id" class="mine-card">
            <div class="mine-card-author">
              <span>{{ post.author?.nickname }}</span>
            </div>
            <img v-if="post.images?.[0]" :src="imgUrl(post.images[0])" class="mine-card-img" @error="e => e.target.style.display = 'none'" />
            <p class="mine-card-content">{{ post.content }}</p>
            <div class="mine-card-footer">
                <span class="mine-card-stat"><Icon icon="mdi:heart-outline" class="mine-stat-icon" /> {{ post.likeCount }}</span>
                <span class="mine-card-stat"><Icon icon="mdi:comment-outline" class="mine-stat-icon" /> {{ post.commentCount }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 待办事项 -->
      <section v-if="currentTab === 'todos'" class="panel">
        <h3>待办事项</h3>
        <div class="todo-input-row">
          <input v-model="newTodo" class="todo-input" placeholder="添加新待办..." @keyup.enter="addTodo" />
          <button class="btn-add" @click="addTodo">添加</button>
        </div>
        <div v-for="todo in todos" :key="todo.id" class="todo-item" :class="{ done: todo.completed }">
          <span class="todo-check" @click="toggleTodo(todo)">
            <Icon
              :icon="todo.completed ? 'mdi:check-circle' : 'mdi:circle-outline'"
              class="todo-check-icon"
              :class="{ done: todo.completed }"
            />
          </span>
          <span class="todo-title">{{ todo.title }}</span>
        </div>
      </section>

      <!-- 我的成就 -->
      <section v-if="currentTab === 'achievements'" class="panel">
        <h3>我的成就</h3>

        <!-- 钱包连接区域 -->
        <div class="wallet-bar">
          <button v-if="!walletAddress" class="btn-wallet" @click="connectWallet">
            🔌 连接 MetaMask
          </button>
          <div v-else class="wallet-info">
            <span class="wallet-addr">{{ walletAddress.slice(0, 6) }}...{{ walletAddress.slice(-4) }}</span>
            <span class="wallet-balance">{{ Number(walletBalance).toFixed(4) }} Sepolia ETH</span>
          </div>
        </div>

        <!-- 成就卡片列表 -->
        <div v-if="achievements.length === 0" class="empty">暂无成就，快去自习室打卡吧！</div>
        <div v-else class="ach-grid">
          <div
            v-for="ach in achievements"
            :key="ach.code"
            class="ach-card"
            :class="{ locked: !ach.unlocked, minted: mintedMap[ACH_ID_MAP[ach.code]] }"
          >
            <!-- 成就图标 -->
            <div class="ach-icon">
              <template v-if="!ach.unlocked">🔒</template>
              <template v-else-if="mintedMap[ACH_ID_MAP[ach.code]]">✨</template>
              <template v-else>🏅</template>
            </div>

            <!-- 成就信息 -->
            <div class="ach-name">{{ ach.name }}</div>
            <div class="ach-desc">{{ ach.desc }}</div>

            <!-- 状态标签 -->
            <div v-if="!ach.unlocked" class="ach-status locked-tag">未解锁</div>
            <div v-else-if="mintedMap[ACH_ID_MAP[ach.code]]" class="ach-status minted-tag">
              已上链 ✨
              <a class="tx-link" :href="'https://sepolia.etherscan.io/tx/' + mintedMap[ACH_ID_MAP[ach.code]].txHash" target="_blank">
                查看交易
              </a>
            </div>
            <div v-else class="ach-status unlocked-tag">已解锁</div>

            <!-- 铸造按钮 -->
            <button
              v-if="ach.unlocked && !mintedMap[ACH_ID_MAP[ach.code]]"
              class="btn-mint"
              :disabled="mintingId === ACH_ID_MAP[ach.code] || !walletAddress"
              @click="mintNFT(ach)"
            >
              {{ mintingId === ACH_ID_MAP[ach.code] ? '铸造中...' : '铸造 NFT' }}
            </button>
          </div>
        </div>
      </section>

    </main>
  </div>
</template>

<style scoped>
.mine {
  display: flex;
  min-height: calc(100vh - 68px);
  gap: 0;
}

/* 侧边栏 */
.sidebar {
  width: 180px;
  flex-shrink: 0;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(10px);
  border-right: 1px solid #f0f0f0;
  border-radius: 12px;
  margin: 8px 0 8px 8px;
  padding: 24px 12px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  text-align: center;
  margin-bottom: 24px;
}

.sidebar-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 8px;
}

.sidebar-avatar.placeholder {
  background: #f0f0f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.sidebar-name {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: background .15s;
  text-align: left;
  width: 100%;
}

.sidebar-item:hover {
  background: #f5f5f5;
}

.sidebar-item.active {
  background: #e8f5e9;
  color: #2E7D32;
  font-weight: 600;
}

.sidebar-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.sidebar-item.active .sidebar-icon {
  color: #2E7D32;
}

.btn-logout {
  margin-top: auto;
  padding: 8px;
  border: none;
  background: transparent;
  color: #999;
  font-size: 12px;
  cursor: pointer;
}

.btn-logout:hover {
  color: #e53e3e;
}

/* 右侧内容区 */
.content {
  flex: 1;
  padding: 24px 28px;
  overflow-y: auto;
}

.panel h3 {
  font-size: 20px;
  font-weight: 700;
  color: #111;
  margin-bottom: 20px;
}

/* 个人资料 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.info-item {
  background: #fff;
  padding: 14px;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.info-item label {
  display: block;
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.info-item span {
  font-size: 15px;
  color: #222;
  font-weight: 500;
}

/* 头像编辑 */
.avatar-edit {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-avatar.placeholder {
  background: #f0f0f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.avatar-upload-btn {
  font-size: 13px;
  color: #2E7D32;
  cursor: pointer;
}

.avatar-upload-btn:hover {
  text-decoration: underline;
}

/* 行内编辑 */
.inline-edit {
  display: flex;
  gap: 6px;
  align-items: center;
}

.edit-input {
  flex: 1;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}

.edit-input:focus {
  border-color: #2E7D32;
}

.btn-save {
  height: 34px;
  padding: 0 14px;
  background: #1c1d1c;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

/* 密码修改 */
.pwd-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.pwd-section h4 {
  font-size: 15px;
  color: #333;
  margin-bottom: 12px;
}

.pwd-section .edit-input {
  display: block;
  width: 100%;
  margin-bottom: 8px;
}

.pwd-section .btn-save {
  margin-top: 4px;
}

.pwd-msg {
  font-size: 13px;
  color: #2E7D32;
  margin-top: 8px;
}

/* 我发布的帖子 + 我的点赞 卡片样式 */
.my-posts-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.my-posts-section h4 {
  font-size: 15px;
  color: #333;
  margin-bottom: 12px;
}

.mine-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.mine-card-author {
  font-size: 12px;
  color: #2E7D32;
  font-weight: 600;
  margin-bottom: 8px;
}

.mine-card-img {
  width: 100%;
  border-radius: 8px;
  margin-bottom: 10px;
  max-height: 200px;
  object-fit: cover;
}

.mine-card-content {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 8px;
  word-break: break-word;
}

.mine-card-footer {
  display: flex;
  gap: 14px;
}

.mine-card-stat {
  font-size: 11px;
  color: #bbb;
  display: flex;
  align-items: center;
  gap: 2px;
}

.mine-stat-icon {
  font-size: 15px;
}

/* 双列瀑布流 */
.masonry2 {
  column-count: 2;
  column-gap: 10px;
}

.masonry2 .mine-card {
  break-inside: avoid;
  margin-bottom: 10px;
}

/* 学习数据 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.stat-card {
  background: #fff;
  padding: 20px 16px;
  border-radius: 10px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #2E7D32;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* AI分析 */
.ai-section {
  margin: 16px 0;
}

.btn-ai {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #047927 0%, #acda56 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.btn-ai:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-result {
  margin-top: 12px;
  padding: 14px;
  background: #f3f0ff;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

/* 图表区域 */
.chart-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-box {
  background: #fff;
  border-radius: 12px;
  padding: 16px 12px 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.chart {
  height: 220px;
}

/* 待办 */
.todo-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.todo-input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.todo-input:focus {
  border-color: #2E7D32;
}

.btn-add {
  height: 40px;
  padding: 0 16px;
  background: #1c1d1c;
  color: #fff;
  border: none;
  border-radius: 10px 0 10px 0;
  font-size: 13px;
  cursor: pointer;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.todo-check {
  cursor: pointer;
}

.todo-check-icon {
  font-size: 22px;
  vertical-align: -4px;
}

.todo-check-icon.done {
  color: #2E7D32;
}

.todo-title {
  font-size: 14px;
  color: #333;
}

.todo-item.done .todo-title {
  text-decoration: line-through;
  color: #bbb;
}

/* 成就 */

/* 钱包连接栏 */
.wallet-bar { margin-bottom: 16px; }

.btn-wallet {
  padding: 10px 20px;
  background: #1e1e1e;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.btn-wallet:hover { background: #292929; }

.wallet-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 12px;
}
.wallet-addr { color: #333; font-family: monospace; }
.wallet-balance { color: #2E7D32; font-weight: 600; }

/* 成就网格 */
.ach-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.ach-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: transform .15s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.ach-card:hover { transform: translateY(-2px); }
.ach-card.locked { opacity: 0.5; background: #fafafa; }
.ach-card.minted { border: 2px solid #f6851b; background: #fff8f0; }

.ach-icon { font-size: 32px; margin-bottom: 4px; }
.ach-name { font-size: 14px; font-weight: 700; color: #222; }
.ach-desc { font-size: 11px; color: #999; }

.ach-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 4px;
  margin-top: 4px;
}
.locked-tag { background: #f0f0f0; color: #999; }
.unlocked-tag { background: #e8f5e9; color: #2E7D32; }
.minted-tag { background: #fff3e0; color: #f6851b; font-weight: 600; }

.tx-link {
  display: block;
  font-size: 10px;
  color: #f6851b;
  cursor: pointer;
  text-decoration: underline;
  margin-top: 2px;
}

.btn-mint {
  width: 100%;
  margin-top: 8px;
  padding: 8px;
  background: linear-gradient(135deg, #f6851b, #e2761b);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity .15s;
}
.btn-mint:hover:not(:disabled) { opacity: 0.9; }
.btn-mint:disabled { opacity: 0.5; cursor: not-allowed; }

/* 空状态 */
.empty {
  color: #bbb;
  font-size: 14px;
  text-align: center;
  padding: 40px 0;
}

/* 响应式：移动端侧边栏变成顶部tab */
@media (max-width: 640px) {
  .mine {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    padding: 8px;
    overflow-x: auto;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    border-radius: 8px;
  }

  .sidebar-header { display: none; }

  .sidebar-nav {
    flex-direction: row;
    gap: 4px;
  }

  .sidebar-item {
    white-space: nowrap;
    font-size: 12px;
    padding: 8px 10px;
  }

  .sidebar-icon { display: none; }

  .btn-logout { display: none; }

  .content {
    padding: 16px;
  }
}
</style>
