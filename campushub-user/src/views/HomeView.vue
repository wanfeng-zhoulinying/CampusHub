<template>
  <AppShell>
    <section class="hero-panel">
      <div class="hero-panel__body">
        <p class="eyebrow">移动优先的一站式校园平台</p>
        <h1 class="hero-title">预约场地、报名活动、查看消息和信用记录，都在一个入口完成。</h1>
        <div class="quick-actions" style="margin-top: 18px">
          <van-button type="primary" icon="search" to="/venues">找场地</van-button>
          <van-button type="success" icon="flag-o" to="/activities">看活动</van-button>
          <van-button plain icon="records-o" to="/bookings">我的预约</van-button>
          <van-button plain icon="apps-o" to="/profile">个人中心</van-button>
        </div>
      </div>
    </section>

    <div class="section-title">
      <h2>热门活动</h2>
      <RouterLink class="muted" to="/activities">全部</RouterLink>
    </div>
    <div v-if="activities.length" class="grid">
      <article v-for="item in activities" :key="item.id" class="card" @click="$router.push(`/activities/${item.id}`)">
        <img v-if="item.coverUrl" class="cover" :src="assetUrl(item.coverUrl)" :alt="item.title" />
        <div class="card__body">
          <div class="line">
            <h3 class="title">{{ item.title }}</h3>
            <van-tag type="success">{{ activityStatusText[item.status] }}</van-tag>
          </div>
          <p class="meta">{{ item.location }}</p>
          <p class="meta">{{ shortTime(item.activityStartTime) }} 开始</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无热门活动" />

    <div class="section-title">
      <h2>热门场地</h2>
      <RouterLink class="muted" to="/venues">全部</RouterLink>
    </div>
    <div v-if="venues.length" class="grid">
      <article v-for="item in venues" :key="item.id" class="card" @click="$router.push(`/venues/${item.id}`)">
        <img v-if="item.coverUrl" class="cover" :src="assetUrl(item.coverUrl)" :alt="item.name" />
        <div class="card__body">
          <div class="line">
            <h3 class="title">{{ item.name }}</h3>
            <van-tag>{{ item.category }}</van-tag>
          </div>
          <p class="meta">{{ item.location }} · 容量 {{ item.capacity }}</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无热门场地" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { activityApi, venueApi } from '../api'
import { activityStatusText } from '../constants/status'
import { assetUrl, shortTime } from '../utils/format'
import type { HotActivityVO, HotVenueVO } from '../types/backend'

const activities = ref<HotActivityVO[]>([])
const venues = ref<HotVenueVO[]>([])

onMounted(async () => {
  const [hotActivities, hotVenues] = await Promise.allSettled([activityApi.hot(6), venueApi.hot(6)])
  if (hotActivities.status === 'fulfilled') activities.value = hotActivities.value
  if (hotVenues.status === 'fulfilled') venues.value = hotVenues.value
})
</script>
