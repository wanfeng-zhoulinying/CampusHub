<template>
  <AppShell>
    <section class="hero-panel">
      <div class="hero-panel__body">
        <p class="eyebrow">管理工作台</p>
        <h1 class="hero-title">用手机也能完成场地维护、活动审核和信用申诉处理。</h1>
      </div>
    </section>

    <van-tabs v-model:active="tab" sticky offset-top="56">
      <van-tab title="看板" name="dashboard">
        <section v-if="overview" class="stat-grid" style="margin-top: 14px">
          <div class="stat"><span>用户</span><b>{{ overview.userCount }}</b></div>
          <div class="stat"><span>场地</span><b>{{ overview.venueCount }}</b></div>
          <div class="stat"><span>活动</span><b>{{ overview.activityCount }}</b></div>
          <div class="stat"><span>预约</span><b>{{ overview.bookingCount }}</b></div>
          <div class="stat"><span>消息</span><b>{{ overview.messageCount }}</b></div>
          <div class="stat"><span>违约</span><b>{{ overview.breachBookingCount }}</b></div>
        </section>
        <article class="card" style="margin-top: 14px">
          <div class="card__body">
            <div ref="chartRef" class="chart"></div>
          </div>
        </article>
      </van-tab>

      <van-tab title="场地" name="venues">
        <div class="section-title">
          <h2>场地管理</h2>
          <van-button size="small" type="primary" @click="openVenue()">新增</van-button>
        </div>
        <div class="stack">
          <article v-for="item in venues" :key="item.id" class="card">
            <div class="card__body stack">
              <div class="line">
                <h3 class="title">{{ item.name }}</h3>
                <van-switch :model-value="item.status === 1" size="22" @update:model-value="toggleVenue(item)" />
              </div>
              <p class="meta">{{ item.category }} · {{ item.location }} · 容量 {{ item.capacity }}</p>
              <van-button size="small" plain @click="openVenue(item)">编辑</van-button>
            </div>
          </article>
        </div>
      </van-tab>

      <van-tab title="活动" name="activities">
        <div class="section-title">
          <h2>活动管理</h2>
          <van-button size="small" type="primary" @click="openActivity()">新增</van-button>
        </div>
        <div class="stack">
          <article v-for="item in activities" :key="item.id" class="card">
            <div class="card__body stack">
              <div class="line">
                <h3 class="title">{{ item.title }}</h3>
                <van-tag :type="auditTagType[item.auditStatus]">{{ auditStatusText[item.auditStatus] }}</van-tag>
              </div>
              <p class="meta">{{ item.location }} · {{ activityStatusText[item.status] }}</p>
              <p class="meta">报名 {{ item.currentSignupCount }} / {{ item.signupLimit }}</p>
              <div class="quick-actions">
                <van-button size="small" plain @click="openActivity(item)">编辑</van-button>
                <van-button size="small" type="success" @click="audit(item.id, 1)">通过</van-button>
                <van-button size="small" type="danger" plain @click="audit(item.id, 2)">驳回</van-button>
              </div>
            </div>
          </article>
        </div>
      </van-tab>

      <van-tab title="申诉" name="appeals">
        <div class="section-title">
          <h2>违约申诉</h2>
          <van-button size="small" plain @click="loadAppeals">刷新</van-button>
        </div>
        <div class="stack">
          <article v-for="item in appeals" :key="item.id" class="card">
            <div class="card__body stack">
              <div class="line">
                <h3 class="title">{{ item.realName }} · {{ item.bookingNo }}</h3>
                <van-tag :type="appealTagType[item.appealStatus]">{{ appealStatusText[item.appealStatus] }}</van-tag>
              </div>
              <p>{{ item.reason }}</p>
              <p class="meta">扣分 {{ item.deductScore }} · {{ shortTime(item.appealTime) }}</p>
              <div class="quick-actions">
                <van-button size="small" type="success" :disabled="item.appealStatus !== 0" @click="auditAppeal(item.id, 1)">通过</van-button>
                <van-button size="small" type="danger" plain :disabled="item.appealStatus !== 0" @click="auditAppeal(item.id, 2)">驳回</van-button>
              </div>
            </div>
          </article>
        </div>
      </van-tab>
    </van-tabs>

    <van-dialog v-model:show="venueDialog" :title="venueForm.id ? '编辑场地' : '新增场地'" show-cancel-button @confirm="saveVenue">
      <van-cell-group inset>
        <van-field v-model="venueForm.name" label="名称" required />
        <van-field v-model="venueForm.category" label="分类" required />
        <van-field v-model="venueForm.location" label="位置" required />
        <van-field v-model.number="venueForm.capacity" type="number" label="容量" required />
        <van-field v-model="venueForm.coverUrl" label="图片URL" />
        <van-field v-model="venueForm.description" label="描述" type="textarea" rows="2" />
      </van-cell-group>
    </van-dialog>

    <van-dialog v-model:show="activityDialog" :title="activityForm.id ? '编辑活动' : '新增活动'" show-cancel-button @confirm="saveActivity">
      <van-cell-group inset>
        <van-field v-model="activityForm.title" label="标题" required />
        <van-field v-model="activityForm.location" label="地点" required />
        <van-field v-model.number="activityForm.signupLimit" type="number" label="名额" required />
        <van-field v-model.number="activityForm.waitLimit" type="number" label="候补" required />
        <van-field v-model="activityForm.signupStartTime" label="报名开始" placeholder="2026-08-17T09:00:00" required />
        <van-field v-model="activityForm.signupEndTime" label="报名结束" required />
        <van-field v-model="activityForm.activityStartTime" label="活动开始" required />
        <van-field v-model="activityForm.activityEndTime" label="活动结束" required />
        <van-field v-model="activityForm.coverUrl" label="图片URL" />
        <van-field v-model="activityForm.content" label="内容" type="textarea" rows="2" />
      </van-cell-group>
    </van-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { showSuccessToast, showToast } from 'vant'
import * as echarts from 'echarts'
import AppShell from '../components/AppShell.vue'
import { adminApi } from '../api'
import { activityStatusText, appealStatusText, appealTagType, auditStatusText, auditTagType } from '../constants/status'
import { useAuthStore } from '../stores/auth'
import { shortTime } from '../utils/format'
import type {
  AdminActivityListVO,
  AdminActivitySaveDTO,
  AdminBookingBreachAppealVO,
  AdminBookingStatusStatVO,
  AdminDashboardOverviewVO,
  AdminVenueListVO,
  AdminVenueSaveDTO,
} from '../types/backend'

const auth = useAuthStore()
const tab = ref('dashboard')
const overview = ref<AdminDashboardOverviewVO>()
const bookingStats = ref<AdminBookingStatusStatVO[]>([])
const venues = ref<AdminVenueListVO[]>([])
const activities = ref<AdminActivityListVO[]>([])
const appeals = ref<AdminBookingBreachAppealVO[]>([])
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

const venueDialog = ref(false)
const activityDialog = ref(false)
const venueForm = reactive<AdminVenueSaveDTO & { id?: number }>({
  name: '',
  category: '',
  location: '',
  capacity: 1,
  coverUrl: '',
  description: '',
  status: 1,
})
const activityForm = reactive<AdminActivitySaveDTO & { id?: number }>({
  publisherId: 0,
  title: '',
  coverUrl: '',
  content: '',
  location: '',
  signupStartTime: '',
  signupEndTime: '',
  activityStartTime: '',
  activityEndTime: '',
  signupLimit: 30,
  waitLimit: 5,
  status: 1,
})

async function loadDashboard() {
  const [overviewData, bookingData] = await Promise.all([adminApi.overview(), adminApi.bookingStatus()])
  overview.value = overviewData
  bookingStats.value = bookingData
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!chartRef.value) return
  chart = chart || echarts.init(chartRef.value)
  chart.setOption({
    title: { text: '预约状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['38%', '68%'],
        data: bookingStats.value.map((item) => ({ name: `状态 ${item.status}`, value: item.count })),
      },
    ],
  })
}

async function loadVenues() {
  venues.value = await adminApi.venues()
}

async function loadActivities() {
  activities.value = await adminApi.activities()
}

async function loadAppeals() {
  appeals.value = await adminApi.appeals()
}

function openVenue(item?: AdminVenueListVO) {
  Object.assign(venueForm, {
    id: item?.id,
    name: item?.name || '',
    category: item?.category || '',
    location: item?.location || '',
    capacity: item?.capacity || 1,
    coverUrl: item?.coverUrl || '',
    description: '',
    status: item?.status ?? 1,
  })
  venueDialog.value = true
}

async function saveVenue() {
  const payload: AdminVenueSaveDTO = {
    name: venueForm.name,
    category: venueForm.category,
    location: venueForm.location,
    capacity: venueForm.capacity,
    coverUrl: venueForm.coverUrl,
    description: venueForm.description,
    status: venueForm.status,
  }
  if (venueForm.id) {
    await adminApi.updateVenue(venueForm.id, payload)
  } else {
    await adminApi.createVenue(payload)
  }
  showSuccessToast('场地已保存')
  await loadVenues()
}

async function toggleVenue(item: AdminVenueListVO) {
  await adminApi.updateVenueStatus(item.id, item.status === 1 ? 0 : 1)
  await loadVenues()
}

function openActivity(item?: AdminActivityListVO) {
  Object.assign(activityForm, {
    id: item?.id,
    publisherId: auth.profile?.id || 1001,
    title: item?.title || '',
    coverUrl: '',
    content: '',
    location: item?.location || '',
    venueId: item?.venueId,
    signupStartTime: item?.signupStartTime || '',
    signupEndTime: item?.signupEndTime || '',
    activityStartTime: item?.activityStartTime || '',
    activityEndTime: item?.activityEndTime || '',
    signupLimit: item?.signupLimit || 30,
    waitLimit: item?.waitLimit || 5,
    status: item?.status || 1,
  })
  activityDialog.value = true
}

async function saveActivity() {
  const payload: AdminActivitySaveDTO = {
    publisherId: activityForm.publisherId,
    title: activityForm.title,
    coverUrl: activityForm.coverUrl,
    content: activityForm.content,
    location: activityForm.location,
    venueId: activityForm.venueId,
    signupStartTime: activityForm.signupStartTime,
    signupEndTime: activityForm.signupEndTime,
    activityStartTime: activityForm.activityStartTime,
    activityEndTime: activityForm.activityEndTime,
    signupLimit: activityForm.signupLimit,
    waitLimit: activityForm.waitLimit,
    status: activityForm.status,
  }
  if (activityForm.id) {
    await adminApi.updateActivity(activityForm.id, payload)
  } else {
    await adminApi.createActivity(payload)
  }
  showSuccessToast('活动已保存')
  await loadActivities()
}

async function audit(activityId: number, status: number) {
  await adminApi.auditActivity(activityId, auth.profile?.id || 1001, status, status === 1 ? '内容合规，准予发布' : '内容需要补充后再提交')
  showSuccessToast(status === 1 ? '已通过' : '已驳回')
  await loadActivities()
}

async function auditAppeal(appealId: number, status: number) {
  await adminApi.auditAppeal(appealId, status, status === 1 ? '申诉通过，恢复信用分' : '证据不足，驳回申诉')
  showSuccessToast(status === 1 ? '申诉已通过' : '申诉已驳回')
  await loadAppeals()
}

watch(tab, async (value) => {
  try {
    if (value === 'venues' && !venues.value.length) await loadVenues()
    if (value === 'activities' && !activities.value.length) await loadActivities()
    if (value === 'appeals' && !appeals.value.length) await loadAppeals()
    if (value === 'dashboard') await nextTick(renderChart)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '加载失败')
  }
})

onMounted(async () => {
  await loadDashboard()
})

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>
