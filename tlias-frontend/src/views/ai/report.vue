<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Download, MagicStick, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { aiReportApi } from '@/api/ai'
import { queryAllClazzApi } from '@/api/clazz'
import { renderMarkdown } from '@/utils/markdown'

const reportTypes = [
  { label: '学员学情报告', value: 'student' },
  { label: '员工团队报告', value: 'employee' },
]

const clazzOptions = ref([])
const generating = ref(false)
const markdown = ref('')
const mode = ref('')

const form = reactive({
  type: 'student',
  clazzId: null,
})

const loadClazzOptions = async () => {
  const [res] = await Promise.allSettled([queryAllClazzApi()])
  if (res.status === 'fulfilled') {
    clazzOptions.value = res.value.data || []
  }
}

const generate = async () => {
  generating.value = true
  try {
    const res = await aiReportApi({
      type: form.type,
      clazzId: form.type === 'student' ? form.clazzId : null,
    })
    markdown.value = res.data?.markdown || ''
    mode.value = res.data?.mode || ''
    if (!markdown.value) {
      ElMessage.warning('报告内容为空，请稍后重试')
    }
  } catch {
    markdown.value = ''
  } finally {
    generating.value = false
  }
}

const copyMarkdown = async () => {
  if (!markdown.value) return
  try {
    await navigator.clipboard.writeText(markdown.value)
    ElMessage.success('已复制 Markdown')
  } catch {
    ElMessage.error('复制失败，请手动选择内容')
  }
}

const downloadMarkdown = () => {
  if (!markdown.value) return
  const blob = new Blob([markdown.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `tlias-ai-report-${form.type}-${Date.now()}.md`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadClazzOptions)
</script>

<template>
  <div class="page">
    <PageHeader title="AI 报告" subtitle="基于当前系统数据，一键生成结构化的分析报告">
      <template #actions>
        <el-tag v-if="mode" type="warning" effect="light" round>
          {{ mode === 'live' ? '真实模型' : '演示模式' }}
        </el-tag>
      </template>
    </PageHeader>

    <div class="report-layout">
      <div class="config-card card">
        <div class="config-head">
          <div class="config-orb"><el-icon><MagicStick /></el-icon></div>
          <div>
            <h3>报告设置</h3>
            <p>选择报告类型与范围，AI 会结合真实统计数据生成内容</p>
          </div>
        </div>

        <el-form label-position="top">
          <el-form-item label="报告类型">
            <el-radio-group v-model="form.type" class="type-group">
              <el-radio-button v-for="t in reportTypes" :key="t.value" :value="t.value">
                {{ t.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="form.type === 'student'" label="班级范围">
            <el-select
              v-model="form.clazzId"
              placeholder="全部班级"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option v-for="c in clazzOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>

        </el-form>

        <el-button class="generate-btn" type="primary" :icon="MagicStick" :loading="generating" @click="generate">
          生成报告
        </el-button>

        <div class="config-note">
          报告仅使用聚合统计数据，不会泄露学员手机号、身份证等敏感信息。
        </div>
      </div>

      <div class="preview-card card">
        <div class="preview-head">
          <div>
            <h3>报告预览</h3>
            <span>{{ markdown ? 'Markdown 格式，可复制或下载' : '生成后在这里查看' }}</span>
          </div>
          <div class="preview-actions">
            <el-button size="small" :icon="Refresh" :disabled="!markdown" @click="generate">重新生成</el-button>
            <el-button size="small" :icon="CopyDocument" :disabled="!markdown" @click="copyMarkdown">复制</el-button>
            <el-button size="small" :icon="Download" :disabled="!markdown" @click="downloadMarkdown">下载</el-button>
          </div>
        </div>

        <div v-if="!markdown && !generating" class="preview-empty">
          <div class="empty-orb"><el-icon :size="30"><MagicStick /></el-icon></div>
          <h4>还没有生成报告</h4>
          <p>在左侧选择报告类型，点击「生成报告」试试看。</p>
        </div>

        <div v-if="generating" class="preview-loading">
          <div class="loading-orb"></div>
          <p>AI 正在分析数据并撰写报告...</p>
        </div>

        <div v-if="markdown" class="markdown-body report-body" v-html="renderMarkdown(markdown)"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 18px;
  align-items: start;
}

.config-card {
  padding: 24px;
}

.config-head {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
}

.config-orb {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.3);
}

.config-head h3 {
  font-size: 16px;
  color: var(--ink-900);
}

.config-head p {
  margin-top: 4px;
  color: var(--ink-400);
  font-size: 11.5px;
  line-height: 1.6;
}

.type-group {
  display: flex;
  width: 100%;
}

.type-group :deep(.el-radio-button) {
  flex: 1;
}

.type-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.generate-btn {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border-radius: 12px;
}

.config-note {
  margin-top: 16px;
  padding: 11px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: var(--ink-400);
  font-size: 10.5px;
  line-height: 1.6;
}

.preview-card {
  min-height: 640px;
  padding: 22px 26px;
  display: flex;
  flex-direction: column;
}

.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.preview-head h3 {
  font-size: 15.5px;
  color: var(--ink-900);
}

.preview-head span {
  display: block;
  margin-top: 3px;
  color: var(--ink-400);
  font-size: 11.5px;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-empty,
.preview-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--ink-400);
}

.empty-orb,
.loading-orb {
  display: grid;
  place-items: center;
  width: 70px;
  height: 70px;
  margin-bottom: 16px;
  border-radius: 22px;
  background: linear-gradient(135deg, #eef2ff, #ecfeff);
  color: #6366f1;
  font-size: 28px;
}

.loading-orb {
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  animation: pulse 1.6s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 18px rgba(99, 102, 241, 0);
  }
}

.preview-empty h4 {
  color: var(--ink-700);
  font-size: 15px;
}

.preview-empty p {
  margin-top: 6px;
  font-size: 12px;
}

.preview-loading p {
  font-size: 12.5px;
}

.report-body {
  padding-top: 18px;
}

@media (max-width: 1100px) {
  .report-layout {
    grid-template-columns: 1fr;
  }
}
</style>
