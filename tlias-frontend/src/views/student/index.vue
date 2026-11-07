<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Warning } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { queryAllClazzApi } from '@/api/clazz'
import {
  addStudentApi,
  deleteStudentsApi,
  handleViolationApi,
  queryStudentByIdApi,
  queryStudentPageApi,
  updateStudentApi,
} from '@/api/student'

const degrees = [
  { label: '初中', value: 1 },
  { label: '高中', value: 2 },
  { label: '大专', value: 3 },
  { label: '本科', value: 4 },
  { label: '硕士', value: 5 },
  { label: '博士', value: 6 },
]

const degreeMap = Object.fromEntries(degrees.map((item) => [item.value, item.label]))

const students = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const clazzOptions = ref([])
const selectedIds = ref([])

const searchForm = reactive({
  name: '',
  degree: null,
  clazzId: null,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增学员')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  no: '',
  gender: 1,
  phone: '',
  idCard: '',
  isCollege: 1,
  address: '',
  degree: null,
  graduationDate: '',
  clazzId: null,
})

const rules = {
  name: [{ required: true, message: '请输入学员姓名', trigger: 'blur' }],
  no: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' },
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入有效的身份证号', trigger: 'blur' },
  ],
  isCollege: [{ required: true, message: '请选择是否来自院校', trigger: 'change' }],
  clazzId: [{ required: true, message: '请选择班级', trigger: 'change' }],
}

const violationVisible = ref(false)
const violationTarget = ref(null)
const violationScore = ref(5)
const violationSubmitting = ref(false)

const loadStudents = async () => {
  loading.value = true
  try {
    const res = await queryStudentPageApi({
      name: searchForm.name,
      degree: searchForm.degree,
      clazzId: searchForm.clazzId,
      page: page.value,
      pageSize: pageSize.value,
    })
    students.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } catch {
    students.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadClazzOptions = async () => {
  const [res] = await Promise.allSettled([queryAllClazzApi()])
  if (res.status === 'fulfilled') {
    clazzOptions.value = res.value.data || []
  }
}

const search = () => {
  page.value = 1
  loadStudents()
}

const reset = () => {
  searchForm.name = ''
  searchForm.degree = null
  searchForm.clazzId = null
  search()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    no: '',
    gender: 1,
    phone: '',
    idCard: '',
    isCollege: 1,
    address: '',
    degree: null,
    graduationDate: '',
    clazzId: null,
  })
  formRef.value?.clearValidate()
}

const openCreate = () => {
  dialogTitle.value = '新增学员'
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  try {
    const res = await queryStudentByIdApi(row.id)
    Object.assign(form, res.data)
    dialogTitle.value = '编辑学员'
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
      if (form.id) {
        await updateStudentApi(form)
        ElMessage.success('学员信息已更新')
      } else {
        await addStudentApi(form)
        ElMessage.success('学员已创建')
      }
      dialogVisible.value = false
      loadStudents()
    } catch {
      // 拦截器已提示
    } finally {
      submitting.value = false
    }
  })
}

const openViolation = (row) => {
  violationTarget.value = row
  violationScore.value = 5
  violationVisible.value = true
}

const submitViolation = async () => {
  if (!violationTarget.value) return
  violationSubmitting.value = true
  try {
    await handleViolationApi(violationTarget.value.id, violationScore.value)
    ElMessage.success('违纪记录已保存')
    violationVisible.value = false
    loadStudents()
  } catch {
    // 拦截器已提示
  } finally {
    violationSubmitting.value = false
  }
}

const removeOne = (row) => {
  ElMessageBox.confirm(`确定删除学员「${row.name}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteStudentsApi(row.id)
        ElMessage.success('删除成功')
        loadStudents()
      } catch {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

const removeSelected = () => {
  if (!selectedIds.value.length) {
    ElMessage.info('请先勾选要删除的学员')
    return
  }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 名学员吗？`, '批量删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteStudentsApi(selectedIds.value)
        ElMessage.success('删除成功')
        selectedIds.value = []
        loadStudents()
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
  loadStudents()
}
const handleCurrentChange = () => loadStudents()

onMounted(() => {
  loadStudents()
  loadClazzOptions()
})
</script>

<template>
  <div class="page">
    <PageHeader title="学员管理" subtitle="维护学员档案、班级归属与违纪记录">
      <template #actions>
        <el-button :icon="Refresh" @click="loadStudents">刷新</el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="removeSelected">
          批量删除
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增学员</el-button>
      </template>
    </PageHeader>

    <div class="card filter-card">
      <el-form :inline="true" :model="searchForm" @submit.prevent>
        <el-form-item label="姓名">
          <el-input
            v-model="searchForm.name"
            placeholder="输入学员姓名"
            clearable
            :prefix-icon="Search"
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="学历">
          <el-select v-model="searchForm.degree" placeholder="全部学历" clearable style="width: 150px">
            <el-option v-for="d in degrees" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="searchForm.clazzId" placeholder="全部班级" clearable filterable style="width: 200px">
            <el-option v-for="c in clazzOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
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
        :data="students"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column label="学员" min-width="170">
          <template #default="{ row }">
            <div class="stu-cell">
              <div class="stu-avatar">{{ row.name?.slice(0, 1) }}</div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.no }}</span>
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
            <span class="muted">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="班级" min-width="140">
          <template #default="{ row }">
            <span>{{ row.clazzName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学历" width="80">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ degreeMap[row.degree] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="违纪" width="145">
          <template #default="{ row }">
            <el-tag v-if="row.violationCount" size="small" type="danger" effect="light">
              {{ row.violationCount }} 次 · 扣 {{ row.violationScore }} 分
            </el-tag>
            <span v-else class="ok">无违纪</span>
          </template>
        </el-table-column>
        <el-table-column label="毕业时间" width="110">
          <template #default="{ row }">
            <span class="muted">{{ row.graduationDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="175" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openViolation(row)">违纪</el-button>
            <el-button link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无学员数据</div>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入学员姓名" maxlength="10" />
          </el-form-item>
          <el-form-item label="学号" prop="no">
            <el-input v-model="form.no" placeholder="请输入学号" maxlength="10" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio-button :value="1">男</el-radio-button>
              <el-radio-button :value="2">女</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
          </el-form-item>
          <el-form-item label="是否来自院校" prop="isCollege">
            <el-select v-model="form.isCollege" style="width: 100%">
              <el-option label="是" :value="1" />
              <el-option label="否" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="最高学历" prop="degree">
            <el-select v-model="form.degree" placeholder="选择学历" style="width: 100%">
              <el-option v-for="d in degrees" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="毕业时间">
            <el-date-picker
              v-model="form.graduationDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="所属班级" prop="clazzId">
            <el-select v-model="form.clazzId" placeholder="选择班级" filterable style="width: 100%">
              <el-option v-for="c in clazzOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系地址">
            <el-input v-model="form.address" placeholder="请输入联系地址" maxlength="100" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="violationVisible" title="违纪处理" width="440px">
      <div class="violation-head">
        <div class="warn-icon"><el-icon><Warning /></el-icon></div>
        <div>
          <strong>{{ violationTarget?.name }}</strong>
          <span>当前已违纪 {{ violationTarget?.violationCount || 0 }} 次，累计扣 {{ violationTarget?.violationScore || 0 }} 分</span>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="本次扣分">
          <el-input-number v-model="violationScore" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="violationVisible = false">取消</el-button>
        <el-button type="danger" :loading="violationSubmitting" @click="submitViolation">
          确认记录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-card {
  padding: 18px 20px 0;
  margin-bottom: 16px;
}

.stu-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stu-avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #e0f2fe, #ede9fe);
  color: #4338ca;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.stu-cell strong {
  display: block;
  color: var(--ink-800);
  font-size: 13.5px;
}

.stu-cell span {
  display: block;
  margin-top: 2px;
  color: var(--ink-400);
  font-size: 10.5px;
  font-family: ui-monospace, Consolas, monospace;
}

.muted {
  color: var(--ink-400);
  font-size: 12px;
}

.ok {
  color: #10b981;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.violation-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.warn-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #ffedd5;
  color: #ea580c;
  font-size: 20px;
}

.violation-head strong {
  display: block;
  color: #9a3412;
  font-size: 14px;
}

.violation-head span {
  display: block;
  margin-top: 3px;
  color: #c2410c;
  font-size: 11px;
}

@media (max-width: 700px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
