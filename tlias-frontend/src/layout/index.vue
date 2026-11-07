<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '仪表盘')

onMounted(() => {
  collapsed.value = window.innerWidth < 1100
})

const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      userStore.logout()
      ElMessage.success('已安全退出')
      router.push('/login')
    })
    .catch(() => {})
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-mark">Z</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>智培云</strong>
          <span>智能培训管理平台</span>
        </div>
      </div>

      <el-menu
        class="sidebar-menu"
        :collapse="collapsed"
        :default-active="activeMenu"
        :collapse-transition="false"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <el-sub-menu index="teach">
          <template #title>
            <el-icon><School /></el-icon>
            <span>教学管理</span>
          </template>
          <el-menu-item index="/clazzs">
            <el-icon><Reading /></el-icon>
            <template #title>班级管理</template>
          </el-menu-item>
          <el-menu-item index="/students">
            <el-icon><UserFilled /></el-icon>
            <template #title>学员管理</template>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="org">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>组织管理</span>
          </template>
          <el-menu-item index="/depts">
            <el-icon><Collection /></el-icon>
            <template #title>部门管理</template>
          </el-menu-item>
          <el-menu-item index="/emps">
            <el-icon><Avatar /></el-icon>
            <template #title>员工管理</template>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="data">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>数据中心</span>
          </template>
          <el-menu-item index="/reports/emp">
            <el-icon><PieChart /></el-icon>
            <template #title>员工统计</template>
          </el-menu-item>
          <el-menu-item index="/reports/student">
            <el-icon><Histogram /></el-icon>
            <template #title>学员统计</template>
          </el-menu-item>
          <el-menu-item index="/logs">
            <el-icon><Document /></el-icon>
            <template #title>操作日志</template>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="ai">
          <template #title>
            <el-icon><MagicStick /></el-icon>
            <span>智能助手</span>
          </template>
          <el-menu-item index="/ai/chat">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>AI 助手</template>
          </el-menu-item>
          <el-menu-item index="/ai/report">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>AI 报告</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div v-show="!collapsed" class="sidebar-foot">
        <div class="ai-tip">
          <el-icon><MagicStick /></el-icon>
          <div>
            <strong>AI 已就绪</strong>
            <span>试着问：哪个班人数最多？</span>
          </div>
        </div>
        <div class="version">智培云 v2.0 · AI Edition</div>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-btn" @click="collapsed = !collapsed">
            <el-icon :size="18">
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </button>
          <div class="crumb">
            <span class="crumb-root">智培云</span>
            <span class="crumb-sep">/</span>
            <span class="crumb-current">{{ currentTitle }}</span>
          </div>
        </div>

        <div class="topbar-right">
          <button class="ai-btn" @click="router.push('/ai/chat')">
            <el-icon><MagicStick /></el-icon>
            AI 助手
          </button>

          <el-dropdown trigger="click">
            <div class="user-chip">
              <div class="avatar">{{ userStore.initial }}</div>
              <div class="user-meta">
                <strong>{{ userStore.name || '管理员' }}</strong>
                <span>管理员</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  {{ userStore.user?.username || 'admin' }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 248px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #0b1220 0%, #101b31 52%, #0a1120 100%);
  transition: width 0.24s ease;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: -120px;
  left: -80px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.28), transparent 68%);
  pointer-events: none;
}

.sidebar::after {
  content: '';
  position: absolute;
  bottom: -140px;
  right: -100px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.18), transparent 68%);
  pointer-events: none;
}

.sidebar.collapsed {
  width: 78px;
}

.brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 20px;
  flex-shrink: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, #818cf8, #06b6d4);
  color: #fff;
  font-weight: 800;
  font-size: 20px;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4);
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
}

.brand-text strong {
  color: #fff;
  font-size: 18px;
  letter-spacing: 0.02em;
}

.brand-text span {
  color: #64748b;
  font-size: 10.5px;
  margin-top: 1px;
  letter-spacing: 0.06em;
}

.sidebar-menu {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 6px 0;
  overflow-y: auto;
  overflow-x: hidden;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #94a3b8;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #ffffff;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 3px 12px;
  border-radius: 11px;
  font-size: 13.5px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.95), rgba(79, 70, 229, 0.82));
  color: #fff;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.35);
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 46px !important;
}

.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  color: #64748b;
}

.sidebar-menu :deep(.el-menu--collapse .el-menu-item),
.sidebar-menu :deep(.el-menu--collapse .el-sub-menu__title) {
  margin: 3px 8px;
}

.sidebar-foot {
  position: relative;
  z-index: 1;
  padding: 14px;
  flex-shrink: 0;
}

.ai-tip {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(6, 182, 212, 0.12));
  border: 1px solid rgba(129, 140, 248, 0.2);
  color: #a5b4fc;
}

.ai-tip > .el-icon {
  margin-top: 2px;
}

.ai-tip strong {
  display: block;
  color: #e0e7ff;
  font-size: 12px;
}

.ai-tip span {
  display: block;
  margin-top: 3px;
  color: #818cf8;
  font-size: 10.5px;
  line-height: 1.4;
}

.version {
  margin-top: 12px;
  text-align: center;
  color: #334155;
  font-size: 10px;
}

.main-area {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  height: 68px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  color: var(--ink-500);
  cursor: pointer;
  transition: all 0.18s ease;
}

.icon-btn:hover {
  color: var(--brand-600);
  border-color: #c7d2fe;
  background: #f5f7ff;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.crumb-root,
.crumb-sep {
  color: var(--ink-400);
}

.crumb-current {
  color: var(--ink-800);
  font-weight: 600;
}

.ai-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 11px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6 55%, #06b6d4);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.3);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.ai-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(99, 102, 241, 0.38);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px 5px 5px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.18s ease;
}

.user-chip:hover {
  background: #f4f6fb;
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #c7d2fe, #a5f3fc);
  color: #4338ca;
  font-weight: 700;
  font-size: 14px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-meta strong {
  font-size: 12.5px;
  color: var(--ink-800);
}

.user-meta span {
  font-size: 10px;
  color: var(--ink-400);
}

.content {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-app);
}

@media (max-width: 900px) {
  .user-meta {
    display: none;
  }
  .topbar {
    padding: 0 14px;
  }
}
</style>
