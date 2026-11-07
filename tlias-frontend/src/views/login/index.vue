<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Lock, MagicStick, TrendCharts, User } from '@element-plus/icons-vue'
import { loginApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const formRef = ref()
const loading = ref(false)
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  username: 'guansheng',
  password: '123456',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const fillDemo = () => {
  form.username = 'guansheng'
  form.password = '123456'
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await loginApi(form)
      userStore.setUser(res.data)
      ElMessage.success(`欢迎回来，${res.data?.name || res.data?.username || ''}`)
      router.push(route.query.redirect || '/dashboard')
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <section class="login-left">
      <div class="blob blob-a"></div>
      <div class="blob blob-b"></div>
      <div class="grid-mask"></div>

      <div class="left-inner">
        <div class="brand-row">
          <div class="brand-mark">Z</div>
          <div class="brand-name">
            <strong>智培云</strong>
            <span>智能培训管理平台</span>
          </div>
        </div>

        <div class="eyebrow">
          <el-icon><MagicStick /></el-icon>
          AI-Powered Training OS
        </div>

        <h1>
          让培训管理
          <br />
          <span class="gradient-text">更聪明一点</span>
        </h1>

        <p class="lead">
          统一管理班级、学员与员工数据，让 AI 帮你读懂数据、生成报告，
          把时间留给真正重要的教学工作。
        </p>

        <div class="feature-list">
          <div class="feature">
            <div class="f-icon"><el-icon><ChatDotRound /></el-icon></div>
            <div>
              <strong>智能问答</strong>
              <span>用自然语言查询员工、班级与学员数据</span>
            </div>
          </div>
          <div class="feature">
            <div class="f-icon"><el-icon><TrendCharts /></el-icon></div>
            <div>
              <strong>数据洞察</strong>
              <span>可视化统计与 AI 分析报告，一眼看清趋势</span>
            </div>
          </div>
          <div class="feature">
            <div class="f-icon"><el-icon><User /></el-icon></div>
            <div>
              <strong>全流程管理</strong>
              <span>部门、员工、班级、学员一站式管理</span>
            </div>
          </div>
        </div>

        <div class="left-foot">© 2026 智培云 · 智能培训管理平台</div>
      </div>
    </section>

    <section class="login-right">
      <div class="login-card">
        <div class="card-head">
          <h2>欢迎回来</h2>
          <p>登录以继续使用智培云管理后台</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入用户名"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-button
            class="login-btn"
            type="primary"
            size="large"
            :loading="loading"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="demo-tip">
          <span>演示账号 <code>guansheng</code> / <code>123456</code></span>
          <el-button link type="primary" size="small" @click="fillDemo">一键填入</el-button>
        </div>

        <div class="login-foot">登录即表示你同意系统的使用规范</div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  min-height: 100vh;
  background: #f5f7fc;
}

.login-left {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 64px 72px;
  background: radial-gradient(120% 120% at 10% 10%, #172554 0%, #0b1220 55%, #070d18 100%);
  color: #fff;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  opacity: 0.55;
  animation: float 9s ease-in-out infinite;
}

.blob-a {
  top: -160px;
  right: -120px;
  width: 480px;
  height: 480px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.55), transparent 68%);
}

.blob-b {
  bottom: -180px;
  left: -120px;
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.35), transparent 68%);
  animation-delay: -4s;
}

.grid-mask {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 52px 52px;
  mask-image: radial-gradient(circle at 30% 40%, #000 0%, transparent 72%);
}

@keyframes float {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -18px, 0) scale(1.04);
  }
}

.left-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 560px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #818cf8, #06b6d4);
  font-size: 23px;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.4);
}

.brand-name {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name strong {
  font-size: 20px;
  letter-spacing: 0.02em;
}

.brand-name span {
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 56px;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(129, 140, 248, 0.25);
  background: rgba(99, 102, 241, 0.12);
  color: #a5b4fc;
  font-size: 11.5px;
  letter-spacing: 0.04em;
}

h1 {
  margin-top: 22px;
  font-size: 52px;
  line-height: 1.14;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #f8fafc;
}

.lead {
  max-width: 480px;
  margin-top: 20px;
  color: #94a3b8;
  font-size: 14.5px;
  line-height: 1.8;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 40px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(255, 255, 255, 0.035);
  backdrop-filter: blur(8px);
}

.f-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, rgba(129, 140, 248, 0.22), rgba(6, 182, 212, 0.16));
  color: #a5b4fc;
  font-size: 18px;
  flex-shrink: 0;
}

.feature strong {
  display: block;
  font-size: 13.5px;
  color: #e2e8f0;
}

.feature span {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 11.5px;
}

.left-foot {
  margin-top: 52px;
  color: #475569;
  font-size: 11px;
  letter-spacing: 0.04em;
}

.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 430px;
  padding: 42px 40px 32px;
  border-radius: 28px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.1);
}

.card-head h2 {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ink-900);
}

.card-head p {
  margin-top: 8px;
  color: var(--ink-500);
  font-size: 13px;
}

.login-card :deep(.el-form) {
  margin-top: 28px;
}

.login-card :deep(.el-form-item__label) {
  color: var(--ink-600, #475569);
  font-size: 12.5px;
  font-weight: 600;
  padding-bottom: 6px;
}

.login-card :deep(.el-input__wrapper) {
  height: 46px;
  background: #f8fafc;
  border-radius: 12px;
}

.login-btn {
  width: 100%;
  height: 48px;
  margin-top: 8px;
  border-radius: 12px;
  font-size: 14.5px;
  letter-spacing: 0.08em;
}

.demo-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 22px;
  padding: 11px 14px;
  border-radius: 12px;
  background: #f8faff;
  border: 1px dashed #ddd6fe;
  color: var(--ink-500);
  font-size: 11.5px;
}

.demo-tip code {
  padding: 1px 5px;
  border-radius: 5px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 11px;
}

.login-foot {
  margin-top: 22px;
  text-align: center;
  color: var(--ink-300);
  font-size: 10.5px;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .login-left {
    display: none;
  }
  .login-right {
    padding: 24px;
  }
}
</style>
