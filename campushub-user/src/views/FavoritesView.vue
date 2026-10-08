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
            <van-tag :type="activityTagType[item.status]">{{ activityStatusText[item.status] }}</van-tag>
          </div>
          <p class="meta">{{ item.location }} · {{ shortTime(item.activityStartTime) }}</p>
          <div class="quick-actions">
            <van-button
              size="small"
              plain
              :loading="removingId === item.activityId"
              @click.stop="removeFavorite(item.activityId)"
            >
              取消收藏
            </van-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无收藏活动" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { socialApi } from '../api'
import { activityStatusText, activityTagType } from '../constants/status'
import { assetUrl, shortTime } from '../utils/format'
import type { ActivityFavoriteVO } from '../types/backend'

const items = ref<ActivityFavoriteVO[]>([])
const removingId = ref(0)

async function load() {
  try {
    items.value = await socialApi.favorites()
  } catch (error) {
    showFailToast((error as Error).message)
  }
}

/** 取消收藏：复用详情页的收藏开关接口，成功后本地移除，避免整页刷新闪烁 */
async function removeFavorite(activityId: number) {
  removingId.value = activityId
  try {
    await socialApi.favorite(activityId)
    items.value = items.value.filter((item) => item.activityId !== activityId)
    showSuccessToast('已取消收藏')
  } catch (error) {
    showFailToast((error as Error).message)
  } finally {
    removingId.value = 0
  }
}

onMounted(load)
</script>
