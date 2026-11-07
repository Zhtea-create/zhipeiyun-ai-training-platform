<script setup>
import { computed, onMounted, ref } from 'vue'
import { Male, PieChart, User } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ChartCard from '@/components/ChartCard.vue'
import { barOption, donutOption } from '@/utils/charts'
import { getEmpGenderDataApi, getEmpJobDataApi } from '@/api/report'

const loading = ref(false)
const jobData = ref({ jobList: [], dataList: [] })
const genderData = ref([])

const total = computed(() =>
  (jobData.value.dataList || []).reduce((sum, value) => sum + Number(value || 0), 0),
)

const topJob = computed(() => {
  const list = jobData.value.jobList || []
  const values = jobData.value.dataList || []
  if (!list.length) return '-'
  let index = 0
  values.forEach((value, i) => {
    if (Number(value) > Number(values[index] || 0)) index = i
  })
  return list[index]
})

const genderCount = computed(() =>
  (genderData.value || []).reduce((sum, item) => sum + Number(item.value || 0), 0),
)

const maleCount = computed(
  () => genderData.value.find((item) => item.name?.includes('男'))?.value || 0,
)
const femaleCount = computed(
  () => genderData.value.find((item) => item.name?.includes('女'))?.value || 0,
)

const jobOption = computed(() =>
  barOption({
    categories: jobData.value.jobList || [],
    values: jobData.value.dataList || [],
    color: '#6366f1',
  }),
)

const genderOption = computed(() =>
  donutOption(
    (genderData.value || []).map((item) => ({
      name: item.name,
      value: Number(item.value) || 0,
    })),
  ),
)

const loadData = async () => {
  loading.value = true
  const [jobRes, genderRes] = await Promise.allSettled([
    getEmpJobDataApi(),
    getEmpGenderDataApi(),
  ])
  if (jobRes.status === 'fulfilled' && jobRes.value.data) {
    jobData.value = jobRes.value.data
  }
  if (genderRes.status === 'fulfilled' && genderRes.value.data) {
    genderData.value = genderRes.value.data
  }
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <div class="page" v-loading="loading">
    <PageHeader title="员工统计" subtitle="从职位与性别两个维度了解当前团队构成" />

    <div class="mini-grid">
      <div class="mini-card card">
        <div class="mini-icon indigo"><el-icon><User /></el-icon></div>
        <div>
          <span>员工总数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
      <div class="mini-card card">
        <div class="mini-icon cyan"><el-icon><PieChart /></el-icon></div>
        <div>
          <span>人数最多的职位</span>
          <strong>{{ topJob }}</strong>
        </div>
      </div>
      <div class="mini-card card">
        <div class="mini-icon violet"><el-icon><Male /></el-icon></div>
        <div>
          <span>男 / 女</span>
          <strong>{{ maleCount }} / {{ femaleCount }}</strong>
        </div>
      </div>
    </div>

    <div class="report-grid">
      <ChartCard
        title="员工职位分布"
        subtitle="各职位员工人数"
        :option="jobOption"
        :empty="!jobData.jobList?.length"
        height="340px"
      />
      <ChartCard
        title="员工性别比例"
        subtitle="男女员工占比"
        :option="genderOption"
        :empty="!genderData.length"
        height="340px"
      />
    </div>

    <div class="card detail-card">
      <div class="detail-head">
        <h3>职位明细</h3>
        <span>共 {{ total }} 名员工</span>
      </div>
      <el-table :data="(jobData.jobList || []).map((job, i) => ({ job, count: jobData.dataList?.[i] || 0 }))">
        <el-table-column prop="job" label="职位" />
        <el-table-column prop="count" label="人数" width="160" />
        <el-table-column label="占比" width="240">
          <template #default="{ row }">
            <div class="ratio">
              <div class="ratio-bar">
                <i :style="{ width: `${total ? (row.count / total) * 100 : 0}%` }"></i>
              </div>
              <span>{{ total ? Math.round((row.count / total) * 100) : 0 }}%</span>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无员工统计数据</div>
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

.mini-icon.indigo {
  background: linear-gradient(135deg, #818cf8, #4f46e5);
}
.mini-icon.cyan {
  background: linear-gradient(135deg, #67e8f9, #0891b2);
}
.mini-icon.violet {
  background: linear-gradient(135deg, #c4b5fd, #7c3aed);
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
  background: linear-gradient(90deg, #818cf8, #06b6d4);
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
