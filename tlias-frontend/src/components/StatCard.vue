<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: 0 },
  icon: { type: [Object, Function], default: null },
  tone: { type: String, default: 'indigo' },
  hint: { type: String, default: '' },
})
</script>

<template>
  <div class="stat-card card" :class="`tone-${tone}`">
    <div class="stat-icon">
      <el-icon :size="22">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
    <div v-if="hint" class="stat-hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.stat-card::after {
  content: '';
  position: absolute;
  right: -30px;
  top: -40px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0.12;
  background: currentColor;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
}

.stat-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: var(--ink-900);
}

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ink-500);
}

.stat-hint {
  position: absolute;
  right: 18px;
  bottom: 16px;
  font-size: 11.5px;
  color: var(--ink-400);
}

.tone-indigo {
  color: #6366f1;
}
.tone-indigo .stat-icon {
  background: linear-gradient(135deg, #818cf8, #4f46e5);
  box-shadow: 0 10px 22px rgba(79, 70, 229, 0.32);
}

.tone-cyan {
  color: #06b6d4;
}
.tone-cyan .stat-icon {
  background: linear-gradient(135deg, #67e8f9, #0891b2);
  box-shadow: 0 10px 22px rgba(8, 145, 178, 0.28);
}

.tone-amber {
  color: #f59e0b;
}
.tone-amber .stat-icon {
  background: linear-gradient(135deg, #fcd34d, #f59e0b);
  box-shadow: 0 10px 22px rgba(245, 158, 11, 0.28);
}

.tone-rose {
  color: #f43f5e;
}
.tone-rose .stat-icon {
  background: linear-gradient(135deg, #fda4af, #e11d48);
  box-shadow: 0 10px 22px rgba(225, 29, 72, 0.26);
}
</style>
