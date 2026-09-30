<template>
  <AppShell>
    <div class="section-title">
      <h2>我的收藏</h2>
    </div>
    <div v-if="items.length" class="grid">
      <article v-for="item in items" :key="item.favoriteId" class="card" @click="$router.push(`/activities/${item.activityId}`)">
        <img v-if="item.coverUrl" class="cover" :src="assetUrl(item.coverUrl)" :alt="item.activityTitle" />
        <div class="card__body">
          <div class="line">
            <h3 class="title">{{ item.activityTitle }}</h3>
            <van-tag>{{ activityStatusText[item.status] }}</van-tag>
          </div>
          <p class="meta">{{ item.location }} · {{ shortTime(item.activityStartTime) }}</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无收藏活动" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { socialApi } from '../api'
import { activityStatusText } from '../constants/status'
import { assetUrl, shortTime } from '../utils/format'
import type { ActivityFavoriteVO } from '../types/backend'

const items = ref<ActivityFavoriteVO[]>([])

onMounted(async () => {
  items.value = await socialApi.favorites()
})
</script>
