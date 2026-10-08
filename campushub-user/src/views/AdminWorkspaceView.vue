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
        <article class="card" style="margin-top: 14px">
          <div class="card__body">
            <div ref="hotVenueChartRef" class="chart"></div>
          </div>
        </article>
        <article class="card" style="margin-top: 14px">
          <div class="card__body">
            <div ref="hotActivityChartRef" class="chart"></div>
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
                <van-button size="small" type="success" @click="openAudit(item.id, 1)">通过</van-button>
                <van-button size="small" type="danger" plain @click="openAudit(item.id, 2)">驳回</van-button>
              </div>
            </div>
          </article>
        </div>
      </van-tab>

      <van-tab title="申诉" name="appeals">
        <div class="section-title">
          <h2>违约申诉</h2>
          <div style="display: flex; gap: 8px">
            <van-button size="small" plain @click="loadAppeals">刷新</van-button>
            <van-button size="small" type="danger" @click="breachDialog = true">登记违约</van-button>
          </div>
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
                <van-button size="small" type="success" :disabled="item.appealStatus !== 0" @click="openAppealAudit(item.id, 1)">通过</van-button>
                <van-button size="small" type="danger" plain :disabled="item.appealStatus !== 0" @click="openAppealAudit(item.id, 2)">驳回</van-button>
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

    <!-- 活动审核：意见可编辑，替换原写死的审核备注 -->
    <van-dialog
      v-model:show="auditDialog"
      :title="auditForm.status === 1 ? '审核通过' : '驳回活动'"
      show-cancel-button
      @confirm="confirmAudit"
    >
      <van-field v-model="auditForm.remark" label="审核意见" type="textarea" rows="2" placeholder="选填，展示给发布者" />
    </van-dialog>

    <!-- 申诉审核：意见可编辑 -->
    <van-dialog
      v-model:show="appealAuditDialog"
      :title="appealAuditForm.status === 1 ? '申诉通过' : '驳回申诉'"
      show-cancel-button
      @confirm="confirmAppealAudit"
    >
      <van-field v-model="appealAuditForm.remark" label="审核意见" type="textarea" rows="2" placeholder="选填" />
    </van-dialog>

    <!-- 登记违约：扣信用分并触发通知 -->
    <van-dialog v-model:show="breachDialog" title="登记违约" show-cancel-button @confirm="confirmBreach">
      <van-cell-group inset>
        <van-field v-model.number="breachForm.bookingId" type="number" label="预约ID" placeholder="预约记录 id" required />
        <van-field v-model.number="breachForm.deductScore" type="number" label="扣分" required />
        <van-field v-model="breachForm.reason" label="原因" type="textarea" rows="2" placeholder="违约事实描述" required />
      </van-cell-group>
    </van-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { showFailToast, showSuccessToast, showToast } from 'vant'
// echarts 按需引入：只打包饼图/柱状图与所需组件，替代整包 import 减小 chunk 体积
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import AppShell from '../components/AppShell.vue'
import { activityApi, adminApi, venueApi } from '../api'
import {
  activityStatusText,
  appealStatusText,
  appealTagType,
  auditStatusText,
  auditTagType,
  bookingStatusText,
} from '../constants/status'
import { useAuthStore } from '../stores/auth'
import { shortTime } from '../utils/format'
import type {
  AdminActivityListVO,
  AdminActivitySaveDTO,
  AdminBookingBreachAppealVO,
  AdminBookingStatusStatVO,
  AdminDashboardOverviewVO,
  AdminHotActivityVO,
  AdminHotVenueVO,
  AdminVenueListVO,
  AdminVenueSaveDTO,
} from '../types/backend'

echarts.use([BarChart, PieChart, GridComponent, TitleComponent, TooltipComponent, CanvasRenderer])

const auth = useAuthStore()
const tab = ref('dashboard')
const overview = ref<AdminDashboardOverviewVO>()
const bookingStats = ref<AdminBookingStatusStatVO[]>([])
const venues = ref<AdminVenueListVO[]>([])
const activities = ref<AdminActivityListVO[]>([])
const appeals = ref<AdminBookingBreachAppealVO[]>([])
const hotVenues = ref<AdminHotVenueVO[]>([])
const hotActivities = ref<AdminHotActivityVO[]>([])
const chartRef = ref<HTMLDivElement>()
const hotVenueChartRef = ref<HTMLDivElement>()
const hotActivityChartRef = ref<HTMLDivElement>()
let chart: ReturnType<typeof echarts.init> | undefined
let hotVenueChart: ReturnType<typeof echarts.init> | undefined
let hotActivityChart: ReturnType<typeof echarts.init> | undefined

/** 活动审核弹窗（意见可编辑，替换原写死文案） */
const auditDialog = ref(false)
const auditForm = reactive({ activityId: 0, status: 1, remark: '' })
/** 申诉审核弹窗 */
const appealAuditDialog = ref(false)
const appealAuditForm = reactive({ appealId: 0, status: 1, remark: '' })
/** 登记违约弹窗 */
const breachDialog = ref(false)
const breachForm = reactive({ bookingId: undefined as number | undefined, deductScore: 10, reason: '' })

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
  const [overviewData, bookingData, hotVenueData, hotActivityData] = await Promise.all([
    adminApi.overview(),
    adminApi.bookingStatus(),
    adminApi.hotVenues(),
    adminApi.hotActivities(),
  ])
  overview.value = overviewData
  bookingStats.value = bookingData
  hotVenues.value = hotVenueData
  hotActivities.value = hotActivityData
  await nextTick()
  renderCharts()
}

/** 渲染看板三张图：预约状态饼图 + 热门场地/热门活动条形图 */
function renderCharts() {
  if (chartRef.value) {
    chart = chart ?? echarts.init(chartRef.value)
    chart.setOption({
      title: { text: '预约状态分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['38%', '68%'],
          data: bookingStats.value.map((item) => ({
            name: bookingStatusText[item.status] || `状态 ${item.status}`,
            value: item.count,
          })),
        },
      ],
    })
  }
  if (hotVenueChartRef.value) {
    hotVenueChart = hotVenueChart ?? echarts.init(hotVenueChartRef.value)
    // reverse：echarts 类目轴自下而上绘制，反转后 TOP1 显示在最上方
    hotVenueChart.setOption({
      title: { text: '热门场地 TOP5（预约数）', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 24, bottom: 8, top: 36, containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: [...hotVenues.value].reverse().map((item) => item.venueName) },
      series: [{ type: 'bar', barWidth: 12, data: [...hotVenues.value].reverse().map((item) => item.bookingCount) }],
    })
  }
  if (hotActivityChartRef.value) {
    hotActivityChart = hotActivityChart ?? echarts.init(hotActivityChartRef.value)
    hotActivityChart.setOption({
      title: { text: '热门活动 TOP5（报名数）', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 24, bottom: 8, top: 36, containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: [...hotActivities.value].reverse().map((item) => item.activityTitle) },
      series: [{ type: 'bar', barWidth: 12, data: [...hotActivities.value].reverse().map((item) => item.signupCount) }],
    })
  }
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

async function openVenue(item?: AdminVenueListVO) {
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
  if (item) {
    // 编辑时列表接口不含 description，回源详情补齐，避免保存把原描述清空
    try {
      const detail = await venueApi.detail(item.id)
      venueForm.description = detail.description ?? ''
    } catch {
      // 详情拉取失败时仍可编辑其它字段
    }
  }
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

async function openActivity(item?: AdminActivityListVO) {
  Object.assign(activityForm, {
    id: item?.id,
    // 新增时发布者为当前管理员；编辑时以详情接口返回的原发布者为准
    publisherId: auth.profile?.id ?? 0,
    title: item?.title || '',
    coverUrl: item?.coverUrl || '',
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
  if (item) {
    // 编辑时列表接口不含 content/publisherId，回源详情补齐，避免保存清空正文或改错发布者
    try {
      const detail = await activityApi.detail(item.id)
      activityForm.content = detail.content ?? ''
      activityForm.publisherId = detail.publisherId
    } catch {
      // 详情拉取失败时仍可编辑其它字段
    }
  }
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

/** 打开活动审核弹窗：预填默认意见，管理员可修改后提交 */
function openAudit(activityId: number, status: number) {
  auditForm.activityId = activityId
  auditForm.status = status
  auditForm.remark = status === 1 ? '内容合规，准予发布' : '内容需要补充后再提交'
  auditDialog.value = true
}

async function confirmAudit() {
  if (!auth.profile?.id) {
    showFailToast('登录信息缺失，请重新登录')
    return
  }
  await adminApi.auditActivity(auditForm.activityId, auth.profile.id, auditForm.status, auditForm.remark.trim() || undefined)
  showSuccessToast(auditForm.status === 1 ? '已通过' : '已驳回')
  await loadActivities()
}

/** 打开申诉审核弹窗：预填默认意见 */
function openAppealAudit(appealId: number, status: number) {
  appealAuditForm.appealId = appealId
  appealAuditForm.status = status
  appealAuditForm.remark = status === 1 ? '申诉通过，恢复信用分' : '证据不足，驳回申诉'
  appealAuditDialog.value = true
}

async function confirmAppealAudit() {
  await adminApi.auditAppeal(appealAuditForm.appealId, appealAuditForm.status, appealAuditForm.remark.trim() || undefined)
  showSuccessToast(appealAuditForm.status === 1 ? '申诉已通过' : '申诉已驳回')
  await loadAppeals()
}

/** 登记违约：校验后调接口扣分，成功后复位表单 */
async function confirmBreach() {
  if (!breachForm.bookingId || !Number.isInteger(breachForm.bookingId) || breachForm.bookingId <= 0) {
    showFailToast('请填写有效的预约 ID')
    return
  }
  if (!breachForm.reason.trim()) {
    showFailToast('请填写违约原因')
    return
  }
  await adminApi.markBreach(breachForm.bookingId, breachForm.reason.trim(), breachForm.deductScore)
  showSuccessToast('违约已登记，已扣分并通知用户')
  breachDialog.value = false
  breachForm.bookingId = undefined
  breachForm.reason = ''
}

watch(tab, async (value) => {
  try {
    if (value === 'venues' && !venues.value.length) await loadVenues()
    if (value === 'activities' && !activities.value.length) await loadActivities()
    if (value === 'appeals' && !appeals.value.length) await loadAppeals()
    if (value === 'dashboard') await nextTick(renderCharts)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '加载失败')
  }
})

onMounted(async () => {
  await loadDashboard()
})

onBeforeUnmount(() => {
  // 三个图表实例统一释放，避免重复进出工作台造成内存泄漏
  chart?.dispose()
  hotVenueChart?.dispose()
  hotActivityChart?.dispose()
})
</script>
