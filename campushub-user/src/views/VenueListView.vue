<template>
  <AppShell>
    <div class="section-title">
      <h2>场地预约</h2>
      <van-tag type="primary">开放场地</van-tag>
    </div>
    <div class="toolbar">
      <van-search v-model="keyword" placeholder="搜索场地、位置" @search="load" />
      <van-dropdown-menu>
        <van-dropdown-item v-model="category" :options="categoryOptions" @change="load" />
      </van-dropdown-menu>
    </div>
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 首屏骨架占位 -->
      <div v-if="loading" class="grid">
        <van-skeleton v-for="i in 3" :key="i" class="skeleton-card" title :row="2" />
      </div>
      <div v-else-if="venues.length" class="grid">
        <article v-for="item in venues" :key="item.id" class="card" @click="$router.push(`/venues/${item.id}`)">
          <img v-if="item.coverUrl" class="cover" :src="assetUrl(item.coverUrl)" :alt="item.name" />
          <div class="card__body">
            <div class="line">
              <h3 class="title">{{ item.name }}</h3>
              <van-tag>{{ item.category }}</van-tag>
            </div>
            <p class="meta">{{ item.location }}</p>
            <p class="meta">容量 {{ item.capacity }} 人</p>
          </div>
        </article>
      </div>
      <EmptyState v-else text="没有匹配的场地" />
    </van-pull-refresh>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showFailToast } from 'vant'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { venueApi } from '../api'
import { assetUrl } from '../utils/format'
import type { VenueListVO } from '../types/backend'

const keyword = ref('')
const category = ref('')
const venues = ref<VenueListVO[]>([])
/** 首屏骨架开关：仅首次加载期间展示 */
const loading = ref(true)
const refreshing = ref(false)
const categoryOptions = computed(() => [
  { text: '全部分类', value: '' },
  ...Array.from(new Set(venues.value.map((item) => item.category))).map((item) => ({ text: item, value: item })),
])

async function load() {
  try {
    venues.value = await venueApi.list({ keyword: keyword.value, category: category.value || undefined })
  } catch {
    showFailToast('加载失败，请稍后重试')
  }
}

/** 下拉刷新：重拉场地列表，结束后收起动画 */
async function onRefresh() {
  try {
    await load()
  } finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  await load()
  loading.value = false
})
</script>
