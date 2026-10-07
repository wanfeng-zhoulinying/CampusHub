<template>
  <AppShell>
    <div class="section-title">
      <h2>消息中心</h2>
      <van-dropdown-menu>
        <van-dropdown-item v-model="readStatus" :options="statusOptions" @change="load" />
      </van-dropdown-menu>
    </div>
    <div v-if="messages.length" class="stack">
      <article v-for="item in messages" :key="item.id" class="card">
        <div class="card__body stack">
          <div class="line">
            <h3 class="title">{{ item.title }}</h3>
            <van-tag :type="item.readStatus === 0 ? 'danger' : 'default'">{{ messageTypeText[item.type] }}</van-tag>
          </div>
          <p>{{ item.content }}</p>
          <div class="line">
            <p class="meta">{{ shortTime(item.createTime) }}</p>
            <van-button v-if="item.readStatus === 0" size="small" plain @click="read(item.id)">标记已读</van-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无消息" />
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showFailToast } from 'vant'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { messageApi } from '../api'
import { MessageReadStatus, messageTypeText } from '../constants/status'
import { useAuthStore } from '../stores/auth'
import { shortTime } from '../utils/format'
import type { MessageVO } from '../types/backend'

const auth = useAuthStore()
const readStatus = ref(-1)
const messages = ref<MessageVO[]>([])
const statusOptions = [
  { text: '全部消息', value: -1 },
  { text: '未读', value: MessageReadStatus.UNREAD },
  { text: '已读', value: MessageReadStatus.READ },
]

async function load() {
  try {
    messages.value = await messageApi.list(readStatus.value === -1 ? undefined : readStatus.value)
  } catch (error) {
    showFailToast((error as Error).message)
  }
}

async function read(id: number) {
  try {
    await messageApi.read(id)
    await auth.refreshUnread()
    await load()
  } catch (error) {
    showFailToast((error as Error).message)
  }
}

onMounted(load)
</script>
