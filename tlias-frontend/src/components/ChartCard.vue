<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '300px' },
  empty: { type: Boolean, default: false },
})

const chartEl = ref(null)
let chart = null

const render = () => {
  if (!chartEl.value || props.empty) return
  if (!chart) chart = echarts.init(chartEl.value)
  chart.setOption(props.option || {}, true)
  chart.resize()
}

const resize = () => chart?.resize()

onMounted(() => {
  nextTick(render)
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})

watch(
  () => props.option,
  () => nextTick(render),
  { deep: true },
)

watch(
  () => props.empty,
  () => nextTick(render),
)
</script>

<template>
  <div class="chart-card card">
    <div class="chart-head">
      <div>
        <h3>{{ title }}</h3>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <slot name="extra" />
    </div>
    <div v-if="empty" class="chart-empty" :style="{ height }">
      <el-icon :size="28"><DataLine /></el-icon>
      <span>暂无数据</span>
    </div>
    <div v-show="!empty" ref="chartEl" :style="{ height }"></div>
  </div>
</template>

<style scoped>
.chart-card {
  padding: 22px 22px 14px;
}

.chart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.chart-head h3 {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--ink-900);
}

.chart-head p {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--ink-400);
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-300);
  font-size: 13px;
}
</style>
