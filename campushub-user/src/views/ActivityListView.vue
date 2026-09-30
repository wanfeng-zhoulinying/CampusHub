<template>
  <AppShell>
    <div class="section-title">
      <h2>活动报名</h2>
      <van-tag type="success">审核通过</van-tag>
    </div>
    <div class="toolbar">
      <van-search v-model="keyword" placeholder="搜索活动" @search="load" />
      <van-dropdown-menu>
        <van-dropdown-item v-model="status" :options="statusOptions" @change="load" />
      </van-dropdown-menu>
    </div>
    <div v-if="activities.length" class="grid">
      <article v-for="item in activities" :key="item.id" class="card" @click="$router.push(`/activities/${item.id}`)">
        <img v-if="item.coverUrl" class="cover" :src="assetUrl(item.coverUrl)" :alt="item.title" />
        <div class="card__body stack">
          <div class="line">
            <h3 class="title">{{ item.title }}</h3>
            <van-tag :type="item.status === ActivityStatus.SIGNING_UP ? 'success' : 'default'">
              {{ activityStatusText[item.status] }}
            </van-tag>
          </div>
          <p class="meta">{{ item.location }}</p>
          <van-progress :percentage="percent(item.currentSignupCount, item.signupLimit)" />
          <p class="meta">报名 {{ item.currentSignupCount }} / {{ item.signupLimit }} · {{ shortTime(item.activityStartTime) }}</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="没有匹配的活动" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { activityApi } from '../api'
import { ActivityAuditStatus, ActivityStatus, activityStatusText } from '../constants/status'
import { assetUrl, percent, shortTime } from '../utils/format'
import type { ActivityListVO } from '../types/backend'

const keyword = ref('')
const status = ref(-1)
const activities = ref<ActivityListVO[]>([])
const statusOptions = [
  { text: '默认报名中', value: -1 },
  { text: '未开始', value: ActivityStatus.NOT_STARTED },
  { text: '报名中', value: ActivityStatus.SIGNING_UP },
  { text: '进行中', value: ActivityStatus.IN_PROGRESS },
  { text: '已结束', value: ActivityStatus.FINISHED },
]

async function load() {
  activities.value = await activityApi.list({
    keyword: keyword.value,
    status: status.value === -1 ? undefined : status.value,
    auditStatus: ActivityAuditStatus.APPROVED,
  })
}

onMounted(load)
</script>
