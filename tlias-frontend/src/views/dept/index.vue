<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { OfficeBuilding, Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  addDeptApi,
  deleteDeptApi,
  queryAllDeptsApi,
  queryDeptByIdApi,
  updateDeptApi,
} from '@/api/dept'
import dayjs from 'dayjs'

const depts = ref([])
const loading = ref(false)
const keyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const submitting = ref(false)
const formRef = ref()
const form = ref({ name: '' })

const rules = {
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 2, max: 10, message: '部门名称长度为 2-10 个字符', trigger: 'blur' },
  ],
}

const filteredDepts = computed(() => {
  const value = keyword.value.trim()
  if (!value) return depts.value
  return depts.value.filter((item) => item.name?.includes(value))
})

const loadDepts = async () => {
  loading.value = true
  try {
    const res = await queryAllDeptsApi()
    depts.value = res.data || []
  } catch {
    depts.value = []
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  dialogTitle.value = '新增部门'
  form.value = { name: '' }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = async (row) => {
  try {
    const res = await queryDeptByIdApi(row.id)
    form.value = { ...res.data }
    dialogTitle.value = '编辑部门'
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
      if (form.value.id) {
        await updateDeptApi(form.value)
        ElMessage.success('部门已更新')
      } else {
        await addDeptApi(form.value)
        ElMessage.success('部门已创建')
      }
      dialogVisible.value = false
      loadDepts()
    } catch {
      // 拦截器已提示
    } finally {
      submitting.value = false
    }
  })
}

const remove = (row) => {
  ElMessageBox.confirm(`确定删除部门「${row.name}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deleteDeptApi(row.id)
        ElMessage.success('删除成功')
        loadDepts()
      } catch {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

onMounted(loadDepts)
</script>

<template>
  <div class="page">
    <PageHeader title="部门管理" subtitle="维护学校组织架构，员工将归属到对应部门">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增部门</el-button>
      </template>
    </PageHeader>

    <div class="card table-card">
      <div class="toolbar top-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索部门名称"
          :prefix-icon="Search"
          clearable
          style="width: 260px"
        />
        <span class="count">共 {{ filteredDepts.length }} 个部门</span>
      </div>

      <el-table v-loading="loading" :data="filteredDepts" style="width: 100%">
        <el-table-column type="index" label="#" width="70" align="center" />
        <el-table-column label="部门名称" min-width="240">
          <template #default="{ row }">
            <div class="dept-cell">
              <div class="dept-icon">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>ID · {{ row.id }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span class="muted">
              {{ row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" width="180">
          <template #default="{ row }">
            <span class="muted">
              {{ row.updateTime ? dayjs(row.updateTime).format('YYYY-MM-DD HH:mm') : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">还没有部门，点击右上角「新增部门」开始创建</div>
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" size="large" placeholder="例如：教研部" maxlength="10" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.top-toolbar {
  justify-content: space-between;
  padding: 14px 12px 16px;
}

.count {
  color: var(--ink-400);
  font-size: 12px;
}

.dept-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dept-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #eef2ff, #e0f2fe);
  color: #4f46e5;
  font-size: 17px;
}

.dept-cell strong {
  display: block;
  color: var(--ink-800);
  font-size: 13.5px;
}

.dept-cell span {
  display: block;
  margin-top: 2px;
  color: var(--ink-400);
  font-size: 10.5px;
}

.muted {
  color: var(--ink-400);
  font-size: 12px;
}
</style>
