<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Document, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { queryLogPageApi } from '@/api/log'

const logs = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await queryLogPageApi({ page: page.value, pageSize: pageSize.value })
    logs.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } catch {
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  page.value = 1
  loadLogs()
}

const handleCurrentChange = () => loadLogs()

onMounted(loadLogs)
</script>

<template>
  <div class="page">
    <PageHeader title="操作日志" subtitle="由 AOP 切面自动记录关键操作，用于审计与问题排查">
      <template #actions>
        <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
      </template>
    </PageHeader>

    <div class="card table-card">
      <div class="log-tip">
        <el-icon><Document /></el-icon>
        <span>记录内容包含操作人、类名、方法名、请求参数、返回值与执行耗时。</span>
      </div>

      <el-table v-loading="loading" :data="logs" style="width: 100%">
        <el-table-column label="操作人" width="140">
          <template #default="{ row }">
            <div class="operator">
              <span class="mini-avatar">{{ (row.operateEmpName || '?').slice(0, 1) }}</span>
              <div>
                <strong>{{ row.operateEmpName || '未知' }}</strong>
                <span>ID {{ row.operateEmpId ?? '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">
            <span class="muted">
              {{ row.operateTime ? dayjs(row.operateTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类名" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方法" width="150">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.methodName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求参数" min-width="220">
          <template #default="{ row }">
            <el-popover placement="top-start" :width="420" trigger="hover">
              <template #reference>
                <span class="params">{{ row.methodParams || '-' }}</span>
              </template>
              <pre class="json-pop">{{ row.methodParams || '无参数' }}</pre>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.costTime > 500 ? 'warning' : 'success'">
              {{ row.costTime }} ms
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column type="expand" width="44">
          <template #default="{ row }">
            <div class="expand-body">
              <div class="expand-block">
                <span>请求参数</span>
                <pre>{{ row.methodParams || '无' }}</pre>
              </div>
              <div class="expand-block">
                <span>返回值</span>
                <pre>{{ row.returnValue || '无' }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            暂无操作日志。执行一次新增 / 修改 / 删除操作后，这里会自动出现记录。
          </div>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<style scoped>
.log-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 12px 16px;
  padding: 11px 14px;
  border-radius: 12px;
  background: #f8faff;
  border: 1px dashed #ddd6fe;
  color: #6366f1;
  font-size: 12px;
}

.operator {
  display: flex;
  align-items: center;
  gap: 9px;
}

.mini-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #eef2ff, #e0f2fe);
  color: #4338ca;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.operator strong {
  display: block;
  color: var(--ink-800);
  font-size: 12.5px;
}

.operator span {
  display: block;
  color: var(--ink-400);
  font-size: 10px;
}

.muted {
  color: var(--ink-400);
  font-size: 12px;
}

.mono {
  font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
  font-size: 11.5px;
  color: var(--ink-500);
}

.params {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink-400);
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
  vertical-align: middle;
}

.json-pop {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 11px;
  color: var(--ink-700);
}

.expand-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  padding: 8px 20px 16px;
}

.expand-block span {
  display: block;
  margin-bottom: 6px;
  color: var(--ink-400);
  font-size: 11px;
}

.expand-block pre {
  margin: 0;
  padding: 12px;
  max-height: 220px;
  overflow: auto;
  border-radius: 10px;
  background: #f8fafc;
  color: var(--ink-600, #475569);
  font-size: 11px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
