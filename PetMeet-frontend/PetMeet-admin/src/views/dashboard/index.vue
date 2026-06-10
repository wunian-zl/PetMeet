<template>
  <div class="dashboard-container">
  <!-- 1. 顶部区域：时间筛选 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h2>工作台</h2>
        <span class="subtitle">欢迎回来，管理员。今天是 {{ currentDate }}</span>
      </div>
      <div class="header-right">
        <el-radio-group v-model="timeRange" @change="handleTimeChange" size="default">
          <el-radio-button label="today">今天</el-radio-button>
          <el-radio-button label="week">近7天</el-radio-button>
          <el-radio-button label="month">近30天</el-radio-button>
        </el-radio-group>
      </div>
    </div>

  <!-- 2. 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="(item, index) in statCards" :key="index">
        <el-card shadow="hover" class="stat-card" @click="handleCardClick(item)">
          <div class="stat-content">
            <div class="stat-icon" :class="item.type">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">{{ item.prefix }}{{ item.value }}</div>
              <div class="stat-compare">
                <span>较{{ timeRange === 'today' ? '昨日' : '上期' }}</span>
                <span :class="item.trend >= 0 ? 'trend-up' : 'trend-down'">
                  <el-icon><component :is="item.trend >= 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
                  {{ Math.abs(item.trend) }}%
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

  <!-- 3. 中部图表区 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 流量趋势 -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>{{ timeLabel }}流量与订单趋势</span>
              <el-radio-group v-model="trendType" size="small" @change="initLineChart">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="pv">访问量</el-radio-button>
                <el-radio-button label="order">订单量</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 分类占比 -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>商品分类销售占比</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

  <!-- 4. 底部模块 -->
    <el-row :gutter="20" class="bottom-row">
      <!-- 热门商品 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>🔥 热门商品 TOP5</span>
              <el-button type="primary" link @click="$router.push('/admin/product')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="topProducts" style="width: 100%" :show-header="true" size="small">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="name" label="商品名称" show-overflow-tooltip />
            <el-table-column prop="sales" label="销量" width="80" align="right" />
            <el-table-column label="库存" width="100">
               <template #default="{ row }">
                 <el-progress :percentage="row.stockPercent" :status="row.stockPercent < 20 ? 'exception' : 'success'" :show-text="false" />
                 <span style="font-size: 12px; color: #909399">{{ row.stock }}件</span>
               </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 待办列表 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>📋 待办事项</span>
            </div>
          </template>
          <div class="todo-list">
            <div 
              v-for="item in todoList" 
              :key="item.id" 
              class="todo-item" 
              @click="handleTodoClick(item)"
            >
              <div class="todo-icon" :class="item.type">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="todo-content">
                <div class="todo-text">{{ item.text }}</div>
                <div class="todo-time">{{ item.time }}</div>
              </div>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
      <!-- 空状态占位 -->
            <el-empty v-if="todoList.length === 0" description="暂无待办事项" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { 
  Money, ShoppingCart, User, Document, 
  CaretTop, CaretBottom, ArrowRight,
  Goods, Bell, Warning
} from '@element-plus/icons-vue'
import * as dashboardApi from '@/api/dashboard'

const router = useRouter()

// 状态
const timeRange = ref('week') // today, week, month
const trendType = ref('all') // all, pv, order
const currentDate = new Date().toLocaleDateString('zh-CN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })

// 数据引用
const statCards = ref([])
const topProducts = ref([])
const todoList = ref([])

// 图表引用
const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

// 计算属性
const timeLabel = computed(() => {
  const map = { today: '今日', week: '近7天', month: '近30天' }
  return map[timeRange.value]
})

// 接口数据加载

const loadStats = async () => {
    try {
        const res = await dashboardApi.getStats(timeRange.value)
        if (res.code === 200 && res.data) {
            const data = res.data
            statCards.value = [
                { 
                    title: '总销售额', 
                    value: Number(data.totalSales || 0).toLocaleString(), 
                    prefix: '¥', 
                    trend: data.salesChange || 0, 
                    icon: 'Money', 
                    type: 'primary',
                    route: '/admin/order' 
                },
                { 
                    title: '订单量', 
                    value: (data.orderCount || 0).toLocaleString(), 
                    prefix: '', 
                    trend: data.orderChange || 0, 
                    icon: 'ShoppingCart', 
                    type: 'success',
                    route: '/admin/order'
                },
                { 
                    title: '新增用户', 
                    value: (data.newUserCount || 0).toLocaleString(), 
                    prefix: '', 
                    trend: data.userChange || 0, 
                    icon: 'User', 
                    type: 'warning',
                    route: '/admin/user'
                },
                { 
                    title: '待审核笔记', 
                    value: String(data.pendingNoteCount || 0), 
                    prefix: '', 
                    trend: 0,
                    icon: 'Document', 
                    type: 'danger',
                    route: '/admin/content'
                }
            ]
        }
    } catch (e) {
        console.error('加载统计数据失败', e)
    }
}

const loadTopProducts = async () => {
    try {
        const res = await dashboardApi.getTopProducts()
        if (res.code === 200 && res.data) {
            topProducts.value = res.data.map(p => ({
                name: p.name,
                sales: p.sales || 0,
                stock: p.stock || 0,
                stockPercent: p.stock ? Math.min(100, (p.stock / 100) * 100) : 0
            }))
        }
    } catch (e) {
        console.error('加载热门商品失败', e)
    }
}

const normalizeAdminRoute = (link) => {
    if (!link || typeof link !== 'string') return ''
    const trimmed = link.trim()
    if (!trimmed || trimmed === '#') return ''
    if (trimmed.startsWith('/admin')) return trimmed
    if (trimmed.startsWith('/')) return `/admin${trimmed}`
    return trimmed
}

const loadTodos = async () => {
    try {
        const res = await dashboardApi.getTodos()
        if (res.code === 200 && res.data) {
            const iconMap = { note: Document, order: Money, product: Goods, complaint: Warning }
            todoList.value = res.data.map(t => ({
                id: t.id,
                text: t.description,
                time: '刚刚',
                icon: iconMap[t.type] || Bell,
                type: t.priority === 'high' ? 'danger' : (t.priority === 'medium' ? 'warning' : 'info'),
                route: normalizeAdminRoute(t.link)
            }))
        }
    } catch (e) {
        console.error('加载待办事项失败', e)
    }
}

// 交互处理

const handleTimeChange = async () => {
    // 1. 先拉最新数据
    await loadStats()
    // 2. 再刷新图表
    await initLineChart()
    initPieChart()
}

const handleCardClick = (item) => {
    if(!item.route) return;

    // 根据 timeRange 计算筛选日期区间
    const end = new Date();
    const start = new Date();
    
    if (timeRange.value === 'week') {
        start.setDate(start.getDate() - 6); // Last 7 days including today
    } else if (timeRange.value === 'month') {
        start.setDate(start.getDate() - 29); // Last 30 days
    }
    // today 默认就是当天，不需要额外回退

    const formatDate = (date) => {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }

    router.push({
        path: item.route,
        query: {
            startDate: formatDate(start),
            endDate: formatDate(end)
        }
    })
}

const handleTodoClick = (item) => {
    if (!item?.route) return
    router.push(item.route)
}

// ECharts 逻辑

const initLineChart = async () => {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)

  // 从接口加载趋势数据
  let xAxisData = []
  let pvData = []
  let orderData = []
  
  try {
      const res = await dashboardApi.getTrend(timeRange.value === 'today' ? 'week' : timeRange.value)
      if (res.code === 200 && res.data) {
          xAxisData = res.data.labels || []
          pvData = res.data.pageViews || []
          orderData = res.data.orderCounts || []
      }
  } catch (e) {
      console.error('加载趋势数据失败', e)
  }

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { 
        data: ['访问量(PV)', '订单量'],
        bottom: 0 
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true, top: '10%' },
    xAxis: { type: 'category', boundaryGap: false, data: xAxisData },
    yAxis: { type: 'value' },
    series: []
  }

  // 按趋势类型决定展示哪些折线
  if (['all', 'pv'].includes(trendType.value)) {
      option.series.push({
          name: '访问量(PV)', type: 'line', smooth: true, 
          data: pvData,
          itemStyle: { color: '#3f6fd9' },
          areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(63,111,217,0.24)' },
                  { offset: 1, color: 'rgba(63,111,217,0.03)' }
              ])
          }
      })
  }
  if (['all', 'order'].includes(trendType.value)) {
      option.series.push({
          name: '订单量', type: 'line', smooth: true, 
          data: orderData,
          itemStyle: { color: '#67C23A' },
          areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(103,194,58,0.3)' },
                  { offset: 1, color: 'rgba(103,194,58,0.01)' }
              ])
          }
      })
  }

  lineChart.setOption(option, true)
}

const initPieChart = async () => {
    if (!pieChartRef.value) return
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    
    // 从接口加载分类数据
    let data = []
    try {
        const res = await dashboardApi.getCategorySales()
        if (res.code === 200 && res.data) {
            data = res.data
        }
    } catch (e) {
        console.error('加载分类销售数据失败', e)
    }

    const option = {
        tooltip: { trigger: 'item' },
        legend: { top: 'bottom', left: 'center' },
        color: ['#3f6fd9', '#67C23A', '#E6A23C', '#F56C6C', '#909399'],
        series: [
            {
                name: '商品分类',
                type: 'pie',
                radius: ['40%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 10,
                    borderColor: '#fff',
                    borderWidth: 2
                },
                label: { 
                    show: true, 
                    position: 'inside',
                    formatter: '{b}',
                    color: '#fff',
                    fontWeight: 'bold'
                },
                emphasis: {
                    label: { show: true, fontWeight: 'bold' },
                    scale: true,
                    scaleSize: 10
                },
                labelLine: { show: false },
                data: data
            }
        ]
    }
    
    pieChart.setOption(option)
    
    // 点击饼图后，带筛选条件跳到商品列表
    pieChart.on('click', (params) => {
        const map = { '猫粮': 'cat_food', '狗粮': 'dog_food', '零食': 'snack', '玩具': 'toy', '用品': 'supplies' }
        const code = map[params.name] || ''
        router.push({ path: '/admin/product', query: { category: code } })
    })
}

const resizeCharts = () => {
  lineChart?.resize()
  pieChart?.resize()
}

// 生命周期

onMounted(async () => {
  // 先拉接口数据
  await loadStats()
  await loadTopProducts()
  await loadTodos()
  
  // 等 DOM 渲染完再初始化图表
  nextTick(async () => {
    await initLineChart()
    await initPieChart()
  })

  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  lineChart?.dispose()
  pieChart?.dispose()
})

</script>

<style scoped>
.dashboard-container {
  /* 外层留白交给父容器控制 */
}

/* 头部 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-left h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
  display: block;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 24px;
}
.stat-card {
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  background: #fff;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.08);
}
.stat-content {
  display: flex;
  align-items: center;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 16px;
}
.stat-icon.primary { background: #ecf1fd; color: #3f6fd9; }
.stat-icon.success { background: #f0f9eb; color: #67C23A; }
.stat-icon.warning { background: #fdf6ec; color: #E6A23C; }
.stat-icon.danger  { background: #fef0f0; color: #F56C6C; }

.stat-info {
  flex: 1;
}
.stat-title {
  font-size: 14px;
  color: #909399;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 4px 0;
}
.stat-compare {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}
.trend-up { color: #F56C6C; font-weight: bold; display: flex; align-items: center; }
.trend-down { color: #67C23A; font-weight: bold; display: flex; align-items: center; }

/* 图表区域 */
.chart-row {
  margin-bottom: 24px;
}
.chart-card {
  min-height: 400px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #303133;
}
.chart-container {
  height: 320px;
}

/* 底部模块 */
.bottom-row {
  margin-bottom: 24px;
}
.list-card {
  height: 100%;
}
/* 待办列表样式 */
.todo-list {
  display: flex;
  flex-direction: column;
}
.todo-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f2f2f2;
  cursor: pointer;
  transition: background 0.2s;
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-item:hover {
  background: #fdfdfd;
}
.todo-icon {
  margin-right: 12px;
  font-size: 18px;
  display: flex;
  align-items: center;
}
.todo-icon.info { color: #909399; }
.todo-icon.warning { color: #E6A23C; }
.todo-icon.danger { color: #F56C6C; }
.todo-icon.primary { color: #3f6fd9; }

.todo-content {
  flex: 1;
}
.todo-text {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}
.todo-time {
  font-size: 12px;
  color: #909399;
}
.arrow-icon {
  color: #dcdfe6;
}

/* 响应式 */
@media (max-width: 992px) {
  .stat-card { margin-bottom: 15px; }
  .chart-col { margin-bottom: 20px; }
  .bottom-row .el-col { margin-bottom: 20px; }
}
</style>
