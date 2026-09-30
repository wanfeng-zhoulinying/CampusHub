<template>
  <AppShell>
    <div class="section-title">
      <h2>我的活动</h2>
      <van-dropdown-menu>
        <van-dropdown-item v-model="status" :options="statusOptions" @change="load" />
      </van-dropdown-menu>
    </div>
    <div v-if="items.length" class="stack">
      <article v-for="item in items" :key="item.id" class="card">
        <div class="card__body stack">
          <div class="line">
            <h3 class="title">{{ item.activityTitle }}</h3>
            <van-tag>{{ signupStatusText[item.signupStatus] }}</van-tag>
          </div>
          <p class="meta">{{ item.activityLocation }} · {{ shortTime(item.signupTime) }}</p>
          <p v-if="item.waitOrder" class="meta">候补顺位：{{ item.waitOrder }}</p>
          <div class="quick-actions">
            <van-button size="small" plain :disabled="item.signupStatus === ActivitySignupStatus.CANCELED" @click="cancel(item.id)">
              取消
            </van-button>
            <van-button size="small" type="primary" :disabled="item.signStatus === 1" @click="checkin(item.id)">签到</van-button>
            <van-button size="small" plain :to="`/activities/${item.activityId}`">详情</van-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无报名记录" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showSuccessToast } from 'vant'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { activityApi } from '../api'
import { ActivitySignupStatus, signupStatusText } from '../constants/status'
import { shortTime } from '../utils/format'
import type { ActivitySignupVO } from '../types/backend'

const status = ref(-1)
const items = ref<ActivitySignupVO[]>([])
const statusOptions = [
  { text: '全部报名', value: -1 },
  { text: '已报名', value: ActivitySignupStatus.SIGNED_UP },
  { text: '候补中', value: ActivitySignupStatus.WAITLISTED },
  { text: '已取消', value: ActivitySignupStatus.CANCELED },
]

async function load() {
  items.value = await activityApi.my(status.value === -1 ? undefined : status.value)
}

async function cancel(id: number) {
  await activityApi.cancel(id)
  showSuccessToast('已取消报名')
  await load()
}

async function checkin(id: number) {
  await activityApi.checkin(id)
  showSuccessToast('签到成功')
  await load()
}

onMounted(load)
</script>
