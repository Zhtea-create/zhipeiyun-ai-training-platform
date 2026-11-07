export const CHART_COLORS = [
  '#6366f1',
  '#22d3ee',
  '#f59e0b',
  '#f472b6',
  '#34d399',
  '#a78bfa',
  '#fb7185',
]

const axisLabel = { color: '#94a3b8', fontSize: 11 }

export function donutOption(data) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#64748b', fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 8 },
        label: { show: false },
        data,
      },
    ],
  }
}

export function barOption({ categories = [], values = [], color = '#6366f1', horizontal = false }) {
  const categoryAxis = {
    type: 'category',
    data: categories,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel,
  }

  const valueAxis = {
    type: 'value',
    splitLine: { lineStyle: { color: '#eef2f7' } },
    axisLabel,
  }

  return {
    grid: { left: 8, right: 24, top: 24, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: horizontal ? valueAxis : categoryAxis,
    yAxis: horizontal ? categoryAxis : valueAxis,
    series: [
      {
        type: 'bar',
        data: values,
        barWidth: horizontal ? 16 : 28,
        itemStyle: {
          color,
          borderRadius: horizontal ? [0, 8, 8, 0] : [8, 8, 0, 0],
        },
        emphasis: { itemStyle: { opacity: 0.82 } },
      },
    ],
  }
}
