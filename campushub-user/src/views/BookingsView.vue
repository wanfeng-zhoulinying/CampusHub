<template>
  <AppShell>
    <div class="section-title">
      <h2>我的预约</h2>
      <van-dropdown-menu>
        <van-dropdown-item v-model="status" :options="statusOptions" @change="load" />
      </van-dropdown-menu>
    </div>
    <div v-if="bookings.length" class="stack">
      <article v-for="item in bookings" :key="item.id" class="card">
        <div class="card__body stack">
          <div class="line">
            <h3 class="title">{{ item.venueName }}</h3>
            <van-tag>{{ bookingStatusText[item.status] }}</van-tag>
          </div>
          <p class="meta">{{ item.venueLocation }}</p>
          <p>{{ item.bookingDate }} {{ item.startTime.slice(0, 5) }} - {{ item.endTime.slice(0, 5) }}</p>
          <p class="meta">人数 {{ item.personCount }} · {{ item.bookingNo }}</p>
          <div class="quick-actions">
            <van-button size="small" plain :disabled="item.status !== BookingStatus.BOOKED" @click="openCancel(item.id)">取消</van-button>
            <van-button size="small" type="primary" :disabled="item.status !== BookingStatus.BOOKED" @click="checkin(item.id)">核销</van-button>
            <van-button size="small" plain :disabled="item.status !== BookingStatus.BREACHED" @click="openAppeal(item.id)">申诉</van-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else text="暂无预约记录" />

    <van-dialog v-model:show="cancelDialog" title="取消预约" show-cancel-button @confirm="submitCancel">
      <van-field v-model="cancelReason" type="textarea" rows="2" placeholder="取消原因（选填，如：时间冲突）" />
    </van-dialog>

    <van-dialog v-model:show="appealDialog" title="违约申诉" show-cancel-button @confirm="submitAppeal">
      <van-field v-model="appealReason" type="textarea" rows="3" placeholder="说明申诉原因" />
    </van-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showSuccessToast } from 'vant'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { bookingApi, creditApi } from '../api'
import { BookingStatus, bookingStatusText } from '../constants/status'
import type { BookingListVO } from '../types/backend'

const status = ref(-1)
const bookings = ref<BookingListVO[]>([])
const cancelDialog = ref(false)
const cancelBookingId = ref(0)
const cancelReason = ref('')
const appealDialog = ref(false)
const appealBookingId = ref(0)
const appealReason = ref('')
const statusOptions = [
  { text: '全部状态', value: -1 },
  { text: '已预约', value: BookingStatus.BOOKED },
  { text: '已核销', value: BookingStatus.CHECKED_IN },
  { text: '已取消', value: BookingStatus.CANCELED },
  { text: '已违约', value: BookingStatus.BREACHED },
]

async function load() {
  bookings.value = await bookingApi.my(status.value === -1 ? undefined : status.value)
}

function openCancel(id: number) {
  cancelBookingId.value = id
  cancelReason.value = ''
  cancelDialog.value = true
}

async function submitCancel() {
  await bookingApi.cancel(cancelBookingId.value, cancelReason.value.trim() || '用户主动取消')
  showSuccessToast('已取消')
  await load()
}

async function checkin(id: number) {
  await bookingApi.checkin(id)
  showSuccessToast('核销成功')
  await load()
}

function openAppeal(id: number) {
  appealBookingId.value = id
  appealReason.value = ''
  appealDialog.value = true
}

async function submitAppeal() {
  await creditApi.appeal(appealBookingId.value, appealReason.value || '申请复核本次违约')
  showSuccessToast('申诉已提交')
}

onMounted(load)
</script>
