<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { MagicStick, OfficeBuilding, School, User, UserFilled } from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import StatCard from '@/components/StatCard.vue'
import { useUserStore } from '@/stores/user'
import { queryAllDeptsApi } from '@/api/dept'
import { queryEmpPageApi } from '@/api/emp'
import { queryClazzPageApi } from '@/api/clazz'
import { queryStudentPageApi } from '@/api/student'
import { queryLogPageApi } from '@/api/log'
import {
  getEmpGenderDataApi,
  getEmpJobDataApi,
  getStudentCountDataApi,
  getStudentDegreeDataApi,
} from '@/api/report'

dayjs.locale('zh-cn')

const router = useRouter()
const userStore = useUserStore()

const empTotal = ref(0)
const studentTotal = ref(0)
const clazzTotal = ref(0)
const deptTotal = ref(0)
const recentLogs = ref([])
const loading = ref(false)

const empJob = ref({ jobList: [], dataList: [] })
const empGender = ref([])
const studentDegree = ref([])
const studentCount = ref({ clazzList: [], dataList: [] })

const today = dayjs().format('YYYY年M月D日 dddd')

const greeting = computed(() => {
  const hour = dayjs().hour()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const palette = ['#6366f1', '#22d3ee', '#f59e0b', '#f472b6', '#34d399', '#a78bfa']

const axisCommon = {
  axisLine: { show: false },
  axisTick: { show: false },
  axisLabel: { color: '#94a3b8', fontSize: 11 },
}

const empJobOption = computed(() => ({
  grid: { left: 8, right: 16, top: 24, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: { type: 'category', data: empJob.value.jobList || [], ...axisCommon },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#eef2f7' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  series: [
    {
      type: 'bar',
      data: empJob.value.dataList || [],
      barWidth: 28,
      itemStyle: { color: '#6366f1', borderRadius: [8, 8, 0, 0] },
      emphasis: { itemStyle: { color: '#818cf8' } },
    },
  ],
}))

const genderOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: '#64748b', fontSize: 11 },
  },
  color: palette,
  series: [
    {
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 8 },
      label: { show: false },
      data: (empGender.value || []).map((item) => ({
        name: item.name,
        value: Number(item.value) || 0,
      })),
    },
  ],
}))

const studentCountOption = computed(() => ({
  grid: { left: 8, right: 24, top: 24, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#eef2f7' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  yAxis: { type: 'category', data: studentCount.value.clazzList || [], ...axisCommon },
  series: [
    {
      type: 'bar',
      data: studentCount.value.dataList || [],
      barWidth: 16,
      itemStyle: { color: '#22d3ee', borderRadius: [0, 8, 8, 0] },
      emphasis: { itemStyle: { color: '#06b6d4' } },
    },
  ],
}))

const degreeOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: '#64748b', fontSize: 11 },
  },
  color: palette,
  series: [
    {
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 8 },
      label: { show: false },
      data: (studentDegree.value || []).map((item) => ({
        name: item.name,
        value: Number(item.value) || 0,
      })),
    },
  ],
}))

const loadStats = async () => {
  loading.value = true
  try {
    const [empRes, stuRes, clazzRes, deptRes] = await Promise.allSettled([
      queryEmpPageApi({ page: 1, pageSize: 1 }),
      queryStudentPageApi({ page: 1, pageSize: 1 }),
      queryClazzPageApi({ page: 1, pageSize: 1 }),
      queryAllDeptsApi(),
    ])

    if (empRes.status === 'fulfilled') empTotal.value = empRes.value.data?.total || 0
    if (stuRes.status === 'fulfilled') studentTotal.value = stuRes.value.data?.total || 0
    if (clazzRes.status === 'fulfilled') clazzTotal.value = clazzRes.value.data?.total || 0
    if (deptRes.status === 'fulfilled') deptTotal.value = (deptRes.value.data || []).length

    const [jobRes, genderRes, degreeRes, countRes, logRes] = await Promise.allSettled([
      getEmpJobDataApi(),
      getEmpGenderDataApi(),
      getStudentDegreeDataApi(),
      getStudentCountDataApi(),
      queryLogPageApi({ page: 1, pageSize: 5 }),
    ])

    if (jobRes.status === 'fulfilled' && jobRes.value.data) {
      empJob.value = jobRes.value.data
    }
    if (genderRes.status === 'fulfilled' && genderRes.value.data) {
      empGender.value = genderRes.value.data
    }
    if (degreeRes.status === 'fulfilled' && degreeRes.value.data) {
      studentDegree.value = degreeRes.value.data
    }
    if (countRes.status === 'fulfilled' && countRes.value.data) {
      studentCount.value = countRes.value.data
    }
    if (logRes.status === 'fulfilled' && logRes.value.data) {
      recentLogs.value = logRes.value.data.rows || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="page dashboard">
    <div class="dash-head">
      <div>
        <p class="eyebrow">OVERVIEW</p>
        <h1>{{ greeting }}，{{ userStore.name || '管理员' }}</h1>
        <p class="sub">这是今天的系统概览，数据会随着操作实时更新。</p>
      </div>
      <div class="sync">
        <span class="dot"></span>
        <span>{{ loading ? '数据同步中' : '数据已同步' }}</span>
        <span class="sep">·</span>
        <span>{{ today }}</span>
      </div>
    </div>

    <div v-loading="loading" class="stat-grid">
      <StatCard label="员工总数" :value="empTotal" :icon="User" tone="indigo" hint="组织成员" />
      <StatCard label="学员总数" :value="studentTotal" :icon="UserFilled" tone="cyan" hint="在读学员" />
      <StatCard label="班级总数" :value="clazzTotal" :icon="School" tone="amber" hint="教学班级" />
      <StatCard label="部门总数" :value="deptTotal" :icon="OfficeBuilding" tone="rose" hint="组织架构" />
    </div>

    <div class="chart-grid">
      <ChartCard
        class="span-2"
        title="员工职位分布"
        subtitle="按职位统计当前员工人数"
        :option="empJobOption"
        :empty="!empJob.jobList?.length"
        height="300px"
      />
      <ChartCard
        title="员工性别比例"
        subtitle="男女员工占比"
        :option="genderOption"
        :empty="!empGender.length"
        height="300px"
      />
    </div>

    <div class="chart-grid">
      <ChartCard
        title="班级人数分布"
        subtitle="每个班级的学员人数"
        :option="studentCountOption"
        :empty="!studentCount.clazzList?.length"
        height="300px"
      />
      <ChartCard
        title="学员学历分布"
        subtitle="不同学历层次的学员人数"
        :option="degreeOption"
        :empty="!studentDegree.length"
        height="300px"
      />
    </div>

    <div class="bottom-grid">
      <div class="card logs-card">
        <div class="card-head">
          <div>
            <h3>最近操作</h3>
            <p>系统最近记录的操作日志</p>
          </div>
          <el-button link type="primary" @click="router.push('/logs')">查看全部</el-button>
        </div>

        <el-table :data="recentLogs" style="width: 100%">
          <el-table-column label="操作人" width="120">
            <template #default="{ row }">
              <div class="operator">
                <span class="mini-avatar">{{ (row.operateEmpName || '?').slice(0, 1) }}</span>
                <span>{{ row.operateEmpName || '未知' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180">
            <template #default="{ row }">
              <span class="mono">{{ row.className?.split('.').pop() }}#{{ row.methodName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.costTime > 500 ? 'warning' : 'success'">
                {{ row.costTime }}ms
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">
              {{ row.operateTime ? dayjs(row.operateTime).format('MM-DD HH:mm:ss') : '-' }}
            </template>
          </el-table-column>
          <template #empty>
            <div class="empty-state">暂无操作日志</div>
          </template>
        </el-table>
      </div>

      <div class="ai-card">
        <div class="ai-orb"></div>
        <div class="ai-content">
          <div class="ai-badge">
            <el-icon><MagicStick /></el-icon>
            智培云 AI
          </div>
          <h3>让 AI 帮你读懂这些数据</h3>
          <p>“哪个班人数最多？”“学员学历分布如何？”直接提问，AI 会结合当前系统数据回答。</p>
          <div class="ai-actions">
            <el-button type="primary" @click="router.push('/ai/chat')">开始对话</el-button>
            <el-button class="ghost-btn" @click="router.push('/ai/report')">生成报告</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1560px;
}

.dash-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.eyebrow {
  color: var(--brand-500);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.dash-head h1 {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ink-900);
}

.sub {
  margin-top: 7px;
  color: var(--ink-500);
  font-size: 13px;
}

.sync {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-400);
  font-size: 12px;
  padding-bottom: 4px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.14);
}

.sep {
  color: var(--ink-300);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
  min-height: 110px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 18px;
  margin-bottom: 18px;
}

.chart-grid:has(.span-2) {
  grid-template-columns: 1.35fr 1fr;
}

.span-2 {
  min-width: 0;
}

.bottom-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 18px;
}

.logs-card {
  padding: 20px 20px 8px;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.card-head h3 {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--ink-900);
}

.card-head p {
  margin-top: 4px;
  color: var(--ink-400);
  font-size: 12px;
}

.operator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 11px;
  font-weight: 700;
}

.mono {
  font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
  font-size: 11.5px;
  color: var(--ink-500);
}

.ai-card {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 30px;
  border-radius: var(--radius-lg);
  background: radial-gradient(120% 120% at 0% 0%, #1e1b4b 0%, #0f172a 58%, #082f49 100%);
  color: #fff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.2);
}

.ai-orb {
  position: absolute;
  right: -70px;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.55), transparent 66%);
  filter: blur(6px);
}

.ai-content {
  position: relative;
  z-index: 1;
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(129, 140, 248, 0.16);
  border: 1px solid rgba(129, 140, 248, 0.25);
  color: #c7d2fe;
  font-size: 11px;
  letter-spacing: 0.06em;
}

.ai-card h3 {
  margin-top: 18px;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.ai-card p {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12.5px;
  line-height: 1.75;
  max-width: 360px;
}

.ai-actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(148, 163, 184, 0.28);
  color: #e2e8f0;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .chart-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .dash-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
