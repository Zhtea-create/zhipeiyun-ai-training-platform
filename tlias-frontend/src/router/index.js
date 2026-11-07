import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'depts',
        name: 'depts',
        component: () => import('@/views/dept/index.vue'),
        meta: { title: '部门管理' },
      },
      {
        path: 'emps',
        name: 'emps',
        component: () => import('@/views/emp/index.vue'),
        meta: { title: '员工管理' },
      },
      {
        path: 'clazzs',
        name: 'clazzs',
        component: () => import('@/views/clazz/index.vue'),
        meta: { title: '班级管理' },
      },
      {
        path: 'students',
        name: 'students',
        component: () => import('@/views/student/index.vue'),
        meta: { title: '学员管理' },
      },
      {
        path: 'reports/emp',
        name: 'emp-report',
        component: () => import('@/views/report/emp.vue'),
        meta: { title: '员工统计' },
      },
      {
        path: 'reports/student',
        name: 'student-report',
        component: () => import('@/views/report/student.vue'),
        meta: { title: '学员统计' },
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('@/views/log/index.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'ai/chat',
        name: 'ai-chat',
        component: () => import('@/views/ai/chat.vue'),
        meta: { title: 'AI 助手' },
      },
      {
        path: 'ai/report',
        name: 'ai-report',
        component: () => import('@/views/ai/report.vue'),
        meta: { title: 'AI 报告' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser') || 'null')

  if (to.meta.public) {
    if (loginUser?.token && to.path === '/login') {
      return '/dashboard'
    }
    return true
  }

  if (!loginUser?.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
