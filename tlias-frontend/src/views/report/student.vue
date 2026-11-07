<script setup>
import { computed, onMounted, ref } from 'vue'
import { Reading, School, UserFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ChartCard from '@/components/ChartCard.vue'
import { barOption, donutOption } from '@/utils/charts'
import { getStudentCountDataApi, getStudentDegreeDataApi } from '@/api/report'

const loading = ref(false)
const countData = ref({ clazzList: [], dataList: [] })
const degreeData = ref([])

const total = computed(() =>
  (countData.value.dataList || []).reduce((sum, value) => sum + Number(value || 0), 0),
)

const classCount = computed(() => (countData.value.clazzList || []).length)

const topClass = computed(() => {
  const list = countData.value.clazzList || []
  const values = countData.value.dataList || []
  if (!list.length) return '-'
  let index = 0
  values.forEach((value, i) => {
    if (Number(value) > Number(values[index] || 0)) index = i
  })
  return list[index]
})

const countOption = computed(() =>
  barOption({
    categories: countData.value.clazzList || [],
    values: countData.value.dataList || [],
    color: '#22d3ee',
    horizontal: true,
  }),
)

const degreeOption = computed(() =>
  donutOption(
    (degreeData.value || []).map((item) => ({
      name: item.name,
      value: Number(item.value) || 0,
    })),
  ),
)

const loadData = async () => {
  loading.value = true
  const [countRes, degreeRes] = await Promise.allSettled([
    getStudentCountDataApi(),
    getStudentDegreeDataApi(),
  ])
  if (countRes.status === 'fulfilled' && countRes.value.data) {
    countData.value = countRes.value.data
  }
  if (degreeRes.status === 'fulfilled' && degreeRes.value.data) {
    degreeData.value = degreeRes.value.data
  }
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <div class="page" v-loading="loading">
    <PageHeader title="学员统计" subtitle="查看学员人数分布与学历结构" />

    <div class="mini-grid">
      <div class="mini-card card">
        <div class="mini-icon cyan"><el-icon><UserFilled /></el-icon></div>
        <div>
          <span>学员总数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
      <div class="mini-card card">
        <div class="mini-icon indigo"><el-icon><School /></el-icon></div>
        <div>
          <span>班级数量</span>
          <strong>{{ classCount }}</strong>
        </div>
      </div>
      <div class="mini-card card">
        <div class="mini-icon amber"><el-icon><Reading /></el-icon></div>
        <div>
          <span>人数最多的班级</span>
          <strong>{{ topClass }}</strong>
        </div>
      </div>
    </div>

    <div class="report-grid">
      <ChartCard
        title="班级人数分布"
        subtitle="每个班级的学员人数"
        :option="countOption"
        :empty="!countData.clazzList?.length"
        height="360px"
      />
      <ChartCard
        title="学员学历分布"
        subtitle="不同学历层次的学员人数"
        :option="degreeOption"
        :empty="!degreeData.length"
        height="360px"
      />
    </div>

    <div class="card detail-card">
      <div class="detail-head">
        <h3>学历明细</h3>
        <span>共 {{ degreeData.length }} 个学历层次</span>
      </div>
      <el-table
        :data="degreeData.map((item) => ({ name: item.name, value: Number(item.value) || 0 }))"
      >
        <el-table-column prop="name" label="学历" />
        <el-table-column prop="value" label="人数" width="160" />
        <el-table-column label="占比" width="260">
          <template #default="{ row }">
            <div class="ratio">
              <div class="ratio-bar">
                <i :style="{ width: `${total ? (row.value / total) * 100 : 0}%` }"></i>
              </div>
              <span>{{ total ? Math.round((row.value / total) * 100) : 0 }}%</span>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无学员统计数据</div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.mini-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.mini-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
}

.mini-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  color: #fff;
  font-size: 20px;
}

.mini-icon.cyan {
  background: linear-gradient(135deg, #67e8f9, #0891b2);
}
.mini-icon.indigo {
  background: linear-gradient(135deg, #818cf8, #4f46e5);
}
.mini-icon.amber {
  background: linear-gradient(135deg, #fcd34d, #f59e0b);
}

.mini-card span {
  display: block;
  color: var(--ink-500);
  font-size: 12px;
}

.mini-card strong {
  display: block;
  margin-top: 5px;
  color: var(--ink-900);
  font-size: 22px;
  font-weight: 750;
}

.report-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
  margin-bottom: 18px;
}

.detail-card {
  padding: 20px 22px 10px;
}

.detail-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-head h3 {
  font-size: 15.5px;
  color: var(--ink-900);
}

.detail-head span {
  color: var(--ink-400);
  font-size: 12px;
}

.ratio {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ratio-bar {
  flex: 1;
  height: 7px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.ratio-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #22d3ee, #6366f1);
}

.ratio span {
  width: 38px;
  color: var(--ink-400);
  font-size: 11px;
}

@media (max-width: 1100px) {
  .mini-grid,
  .report-grid {
    grid-template-columns: 1fr;
  }
}
</style>
