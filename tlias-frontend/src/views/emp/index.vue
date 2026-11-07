<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  addEmpApi,
  deleteEmpsApi,
  queryEmpByIdApi,
  queryEmpPageApi,
  updateEmpApi,
} from '@/api/emp'
import { queryAllDeptsApi } from '@/api/dept'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const jobs = [
  { label: '班主任', value: 1 },
  { label: '讲师', value: 2 },
  { label: '学工主管', value: 3 },
  { label: '教研主管', value: 4 },
  { label: '咨询师', value: 5 },
]
const jobMap = Object.fromEntries(jobs.map((item) => [item.value, item.label]))
const jobType = { 1: 'success', 2: 'primary', 3: 'warning', 4: 'danger', 5: 'info' }

const empList = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const depts = ref([])
const selectedIds = ref([])

const searchForm = reactive({
  name: '',
  gender: null,
  dateRange: [],
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增员工')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  username: '',
  name: '',
  gender: 1,
  phone: '',
  job: null,
  salary: null,
  deptId: null,
  entryDate: '',
  image: '',
  exprList: [],
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '姓名长度为 2-10 个字符', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' },
  ],
  job: [{ required: true, message: '请选择职位', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
}

const loadEmployees = async () => {
  loading.value = true
  try {
    const res = await queryEmpPageApi({
      name: searchForm.name,
      gender: searchForm.gender,
      begin: searchForm.dateRange?.[0],
      end: searchForm.dateRange?.[1],
      page: page.value,
      pageSize: pageSize.value,
    })
    empList.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } catch {
    empList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadDepts = async () => {
  const [res] = await Promise.allSettled([queryAllDeptsApi()])
  if (res.status === 'fulfilled') {
    depts.value = res.value.data || []
  }
}

const search = () => {
  page.value = 1
  loadEmployees()
}

const reset = () => {
  searchForm.name = ''
  searchForm.gender = null
  searchForm.dateRange = []
  search()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    username: '',
    name: '',
    gender: 1,
    phone: '',
    job: null,
    salary: null,
    deptId: null,
    entryDate: '',
    image: '',
    exprList: [],
  })
  formRef.value?.clearValidate()
}

const openCreate = () => {
  dialogTitle.value = '新增员工'
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  try {
    const res = await queryEmpByIdApi(row.id)
    const data = res.data || {}
    Object.assign(form, {
      ...data,
      exprList: (data.exprList || []).map((expr) => ({
        ...expr,
        exprDate: [expr.begin, expr.end],
      })),
    })
    dialogTitle.value = '编辑员工'
    dialogVisible.value = true
    formRef.value?.clearValidate()
  } catch {
    // 拦截器已提示
  }
}

const addExpr = () => {
  form.exprList.push({ company: '', job: '', exprDate: [] })
}

const removeExpr = (index) => {
  form.exprList.splice(index, 1)
}

const handleAvatarSuccess = (response) => {
  if (response?.code === 1 && response.data) {
    form.image = response.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response?.msg || '头像上传失败')
  }
}

const beforeAvatarUpload = (file) => {
  const okType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  const okSize = file.size / 1024 / 1024 <= 10
  if (!okType) {
    ElMessage.error('仅支持 JPG / PNG / WebP 图片')
    return false
  }
  if (!okSize) {
    ElMessage.error('图片不能超过 10MB')
    return false
  }
  return true
}

const normalizePayload = () => {
  const payload = {
    ...form,
    exprList: (form.exprList || []).map(({ exprDate, ...rest }) => ({
      ...rest,
      begin: exprDate?.[0] || rest.begin || null,
      end: exprDate?.[1] || rest.end || null,
    })),
  }
  delete payload.exprDate
  return payload
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (form.id) {
        await updateEmpApi(normalizePayload())
        ElMessage.success('员工信息已更新')
      } else {
        await addEmpApi(normalizePayload())
        ElMessage.success('员工已创建')
      }
      dialogVisible.value = false
      loadEmployees()
    } catch {
      // 拦截器已提示
    } finally {
      submitting.value = false
    }
  })
}

const removeOne = (row) => {
  ElMessageBox.confirm(`确定删除员工「${row.name}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteEmpsApi(row.id)
        ElMessage.success('删除成功')
        loadEmployees()
      } catch {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

const removeSelected = () => {
  if (!selectedIds.value.length) {
    ElMessage.info('请先勾选要删除的员工')
    return
  }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 名员工吗？`, '批量删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteEmpsApi(selectedIds.value)
        ElMessage.success('删除成功')
        selectedIds.value = []
        loadEmployees()
      } catch {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handleSizeChange = () => {
  page.value = 1
  loadEmployees()
}
const handleCurrentChange = () => loadEmployees()

onMounted(() => {
  loadEmployees()
  loadDepts()
})
</script>

<template>
  <div class="page">
    <PageHeader title="员工管理" subtitle="维护员工档案、职位、部门与工作经历">
      <template #actions>
        <el-button :icon="Refresh" @click="loadEmployees">刷新</el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeSelected">
          批量删除
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
      </template>
    </PageHeader>

    <div class="card filter-card">
      <el-form :inline="true" :model="searchForm" @submit.prevent>
        <el-form-item label="姓名">
          <el-input
            v-model="searchForm.name"
            placeholder="输入员工姓名"
            clearable
            :prefix-icon="Search"
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="searchForm.gender" placeholder="全部" clearable style="width: 120px">
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="入职时间">
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
      <el-table
        v-loading="loading"
        :data="empList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column label="员工" min-width="200">
          <template #default="{ row }">
            <div class="emp-cell">
              <el-avatar :size="40" :src="row.image">
                {{ row.name?.slice(0, 1) }}
              </el-avatar>
              <div>
                <strong>{{ row.name }}</strong>
                <span>@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.gender === 1 ? '' : 'danger'" effect="plain">
              {{ row.gender === 1 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="125">
          <template #default="{ row }">
            <span class="muted">{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="部门" width="100">
          <template #default="{ row }">
            <span>{{ row.deptName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="职位" width="105">
          <template #default="{ row }">
            <el-tag size="small" :type="jobType[row.job] || 'info'" effect="light">
              {{ jobMap[row.job] || '未设置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入职日期" width="105">
          <template #default="{ row }">
            <span class="muted">{{ row.entryDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最后操作" width="145">
          <template #default="{ row }">
            <span class="muted">
              {{ row.updateTime ? dayjs(row.updateTime).format('YYYY-MM-DD HH:mm') : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无员工数据</div>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="860px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="avatar-row">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :headers="{ token: userStore.token }"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.image" :src="form.image" class="avatar-img" alt="avatar" />
            <div v-else class="avatar-empty">
              <el-icon :size="22"><Plus /></el-icon>
              <span>上传头像</span>
            </div>
          </el-upload>
          <div class="avatar-tip">
            <strong>员工头像</strong>
            <span>支持 JPG / PNG / WebP，大小不超过 10MB</span>
          </div>
        </div>

        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="登录用户名" maxlength="20" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="员工姓名" maxlength="10" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio-button :value="1">男</el-radio-button>
              <el-radio-button :value="2">女</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="职位" prop="job">
            <el-select v-model="form.job" placeholder="选择职位" style="width: 100%">
              <el-option v-for="j in jobs" :key="j.value" :label="j.label" :value="j.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="薪资">
            <el-input-number
              v-model="form.salary"
              :min="0"
              :max="999999"
              :step="500"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入薪资"
            />
          </el-form-item>
          <el-form-item label="所属部门" prop="deptId">
            <el-select v-model="form.deptId" placeholder="选择部门" style="width: 100%">
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="入职日期">
            <el-date-picker
              v-model="form.entryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <div class="expr-head">
          <div>
            <strong>工作经历</strong>
            <span>可添加多段工作经历，按时间顺序填写</span>
          </div>
          <el-button type="primary" plain size="small" :icon="Plus" @click="addExpr">
            添加经历
          </el-button>
        </div>

        <div v-if="!form.exprList.length" class="expr-empty">
          暂无工作经历，点击右上角「添加经历」补充
        </div>

        <div v-for="(expr, index) in form.exprList" :key="index" class="expr-row">
          <el-date-picker
            v-model="expr.exprDate"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
          <el-input v-model="expr.company" placeholder="公司名称" maxlength="50" />
          <el-input v-model="expr.job" placeholder="职位" maxlength="50" />
          <el-button link type="danger" @click="removeExpr(index)">删除</el-button>
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

.emp-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.emp-cell strong {
  display: block;
  color: var(--ink-800);
  font-size: 13.5px;
}

.emp-cell span {
  display: block;
  margin-top: 2px;
  color: var(--ink-400);
  font-size: 10.5px;
}

.muted {
  color: var(--ink-400);
  font-size: 12px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 14px;
  border-radius: 14px;
  background: #f8faff;
  border: 1px dashed #ddd6fe;
}

.avatar-uploader :deep(.el-upload) {
  width: 74px;
  height: 74px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px dashed #c7d2fe;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s ease;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--brand-500);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 100%;
  color: var(--ink-400);
  font-size: 10px;
}

.avatar-tip strong {
  display: block;
  color: var(--ink-700);
  font-size: 13px;
}

.avatar-tip span {
  display: block;
  margin-top: 4px;
  color: var(--ink-400);
  font-size: 11.5px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.expr-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 12px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.expr-head strong {
  display: block;
  color: var(--ink-800);
  font-size: 13.5px;
}

.expr-head span {
  display: block;
  margin-top: 3px;
  color: var(--ink-400);
  font-size: 11.5px;
}

.expr-empty {
  padding: 18px;
  border-radius: 12px;
  background: #f8fafc;
  color: var(--ink-400);
  font-size: 12px;
  text-align: center;
}

.expr-row {
  display: grid;
  grid-template-columns: 260px 1fr 1fr 60px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

@media (max-width: 800px) {
  .form-grid,
  .expr-row {
    grid-template-columns: 1fr;
  }
}
</style>
