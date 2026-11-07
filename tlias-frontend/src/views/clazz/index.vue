<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  addClazzApi,
  deleteClazzApi,
  queryClazzByIdApi,
  queryClazzPageApi,
  updateClazzApi,
} from '@/api/clazz'
import { queryAllEmpsApi } from '@/api/emp'

const subjects = [
  { label: 'Java', value: 1 },
  { label: '前端', value: 2 },
  { label: '大数据', value: 3 },
  { label: 'Python', value: 4 },
  { label: 'Go', value: 5 },
  { label: '嵌入式', value: 6 },
]

const subjectMap = Object.fromEntries(subjects.map((item) => [item.value, item.label]))

const statusType = (status) => {
  if (status === '在读中') return 'success'
  if (status === '已结课') return 'info'
  return 'warning'
}

const clazzList = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const employees = ref([])

const searchForm = reactive({
  name: '',
  dateRange: [],
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增班级')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  room: '',
  masterId: null,
  subject: null,
  beginDate: '',
  endDate: '',
})

const rules = {
  name: [
    { required: true, message: '请输入班级名称', trigger: 'blur' },
    { max: 30, message: '班级名称最多 30 个字符', trigger: 'blur' },
  ],
  room: [{ required: true, message: '请输入教室', trigger: 'blur' }],
  masterId: [{ required: true, message: '请选择班主任', trigger: 'change' }],
  subject: [{ required: true, message: '请选择学科', trigger: 'change' }],
  beginDate: [{ required: true, message: '请选择开课时间', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结课时间', trigger: 'change' }],
}

const loadClazz = async () => {
  loading.value = true
  try {
    const res = await queryClazzPageApi({
      name: searchForm.name,
      begin: searchForm.dateRange?.[0],
      end: searchForm.dateRange?.[1],
      page: page.value,
      pageSize: pageSize.value,
    })
    clazzList.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } catch {
    clazzList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  const [empRes] = await Promise.allSettled([queryAllEmpsApi()])
  if (empRes.status === 'fulfilled') employees.value = empRes.value.data || []
}

const search = () => {
  page.value = 1
  loadClazz()
}

const reset = () => {
  searchForm.name = ''
  searchForm.dateRange = []
  search()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    room: '',
    masterId: null,
    subject: null,
    beginDate: '',
    endDate: '',
  })
  formRef.value?.clearValidate()
}

const openCreate = () => {
  dialogTitle.value = '新增班级'
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  try {
    const res = await queryClazzByIdApi(row.id)
    Object.assign(form, res.data)
    dialogTitle.value = '编辑班级'
    dialogVisible.value = true
    formRef.value?.clearValidate()
  } catch {
    // 拦截器已提示
  }
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { ...form }
      if (payload.id) {
        await updateClazzApi(payload)
        ElMessage.success('班级已更新')
      } else {
        await addClazzApi(payload)
        ElMessage.success('班级已创建')
      }
      dialogVisible.value = false
      loadClazz()
    } catch {
      // 拦截器已提示
    } finally {
      submitting.value = false
    }
  })
}

const remove = (row) => {
  ElMessageBox.confirm(
    `确定删除班级「${row.name}」吗？班级下若有学员将无法删除。`,
    '删除确认',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      try {
        await deleteClazzApi(row.id)
        ElMessage.success('删除成功')
        loadClazz()
      } catch {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

const handleSizeChange = () => {
  page.value = 1
  loadClazz()
}
const handleCurrentChange = () => loadClazz()

onMounted(() => {
  loadClazz()
  loadOptions()
})
</script>

<template>
  <div class="page">
    <PageHeader title="班级管理" subtitle="维护班级信息、班主任与课程学科">
      <template #actions>
        <el-button :icon="Refresh" @click="loadClazz">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增班级</el-button>
      </template>
    </PageHeader>

    <div class="card filter-card">
      <el-form :inline="true" :model="searchForm" @submit.prevent>
        <el-form-item label="班级名称">
          <el-input
            v-model="searchForm.name"
            placeholder="输入班级名称"
            clearable
            :prefix-icon="Search"
            style="width: 220px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="结课时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card table-card">
      <el-table v-loading="loading" :data="clazzList" style="width: 100%">
        <el-table-column label="班级" min-width="200">
          <template #default="{ row }">
            <div class="clazz-cell">
              <div class="clazz-avatar">{{ row.name?.slice(0, 1) }}</div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.room || '未设置教室' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="班主任" width="120">
          <template #default="{ row }">
            <span>{{ row.masterName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学科" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ subjectMap[row.subject] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开课时间" width="110">
          <template #default="{ row }">
            <span class="muted">{{ row.beginDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结课时间" width="110">
          <template #default="{ row }">
            <span class="muted">{{ row.endDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">
              <span class="status-dot" :class="row.status"></span>
              {{ row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后操作" width="150">
          <template #default="{ row }">
            <span class="muted">
              {{ row.updateTime ? dayjs(row.updateTime).format('YYYY-MM-DD HH:mm') : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无班级数据</div>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[5, 10, 20, 50]"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="班级名称" prop="name">
            <el-input v-model="form.name" placeholder="例如：Java 就业 102 班" maxlength="30" />
          </el-form-item>
          <el-form-item label="教室" prop="room">
            <el-input v-model="form.room" placeholder="例如：301 教室" maxlength="20" />
          </el-form-item>
          <el-form-item label="班主任" prop="masterId">
            <el-select v-model="form.masterId" placeholder="选择班主任" filterable style="width: 100%">
              <el-option
                v-for="emp in employees"
                :key="emp.id"
                :label="emp.name"
                :value="emp.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="学科" prop="subject">
            <el-select v-model="form.subject" placeholder="选择学科" style="width: 100%">
              <el-option v-for="s in subjects" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="开课时间" prop="beginDate">
            <el-date-picker
              v-model="form.beginDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结课时间" prop="endDate">
            <el-date-picker
              v-model="form.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-card {
  padding: 18px 20px 0;
  margin-bottom: 16px;
}

.clazz-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.clazz-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e0e7ff, #cffafe);
  color: #4338ca;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.clazz-cell strong {
  display: block;
  color: var(--ink-800);
  font-size: 13.5px;
}

.clazz-cell span {
  display: block;
  margin-top: 2px;
  color: var(--ink-400);
  font-size: 10.5px;
}

.muted {
  color: var(--ink-400);
  font-size: 12px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 50%;
  background: currentColor;
  vertical-align: middle;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

@media (max-width: 700px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
