<template>
  <AppShell>
    <section v-if="overview" class="hero-panel">
      <div class="hero-panel__body">
        <p class="eyebrow">信用中心</p>
        <h1 class="hero-title">{{ overview.creditScore }} 分</h1>
        <p class="muted">
          累计扣分 {{ overview.totalDeductScore }}，累计恢复 {{ overview.totalRestoreScore }}，违约 {{ overview.breachCount }} 次
        </p>
      </div>
    </section>

    <!-- 后端规则：信用分低于 60 分将无法预约场地、报名活动 -->
    <van-notice-bar
      v-if="overview && overview.creditScore < 60"
      left-icon="info-o"
      text="信用分低于 60 分：暂时无法预约场地、报名活动。按时核销与申诉通过可恢复分数。"
    />

    <div class="section-title">
      <h2>信用记录</h2>
    </div>
    <div v-if="records.length" class="stack">
      <article v-for="item in records" :key="item.id" class="card">
        <div class="card__body">
          <div class="line">
            <strong>{{ item.reason }}</strong>
            <van-tag :type="item.changeType === 1 ? 'success' : 'danger'">
              {{ item.changeType === 1 ? '+' : '-' }}{{ item.changeScore }}
            </van-tag>
          </div>
          <p class="meta">当前 {{ item.currentScore }} 分 · {{ shortTime(item.createTime) }}</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无信用记录" />

    <div class="section-title">
      <h2>申诉记录</h2>
    </div>
    <div v-if="appeals.length" class="stack">
      <article v-for="item in appeals" :key="item.id" class="card">
        <div class="card__body">
          <div class="line">
            <strong>{{ item.bookingNo }}</strong>
            <van-tag>{{ appealStatusText[item.appealStatus] }}</van-tag>
          </div>
          <p>{{ item.reason }}</p>
          <p class="meta">扣分 {{ item.deductScore }} · {{ shortTime(item.appealTime) }}</p>
          <p v-if="item.auditRemark" class="meta">审核意见：{{ item.auditRemark }}</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无申诉记录" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { creditApi } from '../api'
import { appealStatusText } from '../constants/status'
import { shortTime } from '../utils/format'
import type { AdminBookingBreachAppealVO, CreditOverviewVO, CreditRecordVO } from '../types/backend'

const overview = ref<CreditOverviewVO>()
const records = ref<CreditRecordVO[]>([])
const appeals = ref<AdminBookingBreachAppealVO[]>([])

onMounted(async () => {
  const [credit, recordList, appealList] = await Promise.allSettled([creditApi.overview(), creditApi.records(), creditApi.appeals()])
  if (credit.status === 'fulfilled') overview.value = credit.value
  if (recordList.status === 'fulfilled') records.value = recordList.value
  if (appealList.status === 'fulfilled') appeals.value = appealList.value
})
</script>
