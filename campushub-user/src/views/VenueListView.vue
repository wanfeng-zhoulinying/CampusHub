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
    <div v-if="venues.length" class="grid">
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
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { venueApi } from '../api'
import { assetUrl } from '../utils/format'
import type { VenueListVO } from '../types/backend'

const keyword = ref('')
const category = ref('')
const venues = ref<VenueListVO[]>([])
const categoryOptions = computed(() => [
  { text: '全部分类', value: '' },
  ...Array.from(new Set(venues.value.map((item) => item.category))).map((item) => ({ text: item, value: item })),
])

async function load() {
  venues.value = await venueApi.list({ keyword: keyword.value, category: category.value || undefined })
}

onMounted(load)
</script>
