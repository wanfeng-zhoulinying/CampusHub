<template>
  <AppShell>
    <article v-if="venue" class="card">
      <img v-if="venue.coverUrl" class="cover" :src="assetUrl(venue.coverUrl)" :alt="venue.name" />
      <div class="card__body stack">
        <div class="line">
          <h2 class="title">{{ venue.name }}</h2>
          <van-tag>{{ venue.category }}</van-tag>
        </div>
        <p class="meta">{{ venue.location }} · 容量 {{ venue.capacity }}</p>
        <p>{{ venue.description || '暂无场地说明' }}</p>
      </div>
    </article>

    <div class="section-title">
      <h2>选择时间段</h2>
      <van-button size="small" icon="calendar-o" @click="showCalendar = true">{{ date }}</van-button>
    </div>
    <div v-if="slots.length" class="grid">
      <article v-for="slot in slots" :key="slot.id" class="card">
        <div class="card__body stack">
          <div class="line">
            <h3 class="title">{{ slot.startTime.slice(0, 5) }} - {{ slot.endTime.slice(0, 5) }}</h3>
            <van-tag :type="slot.status === VenueSlotStatus.AVAILABLE ? 'success' : 'default'">
              {{ slot.status === VenueSlotStatus.AVAILABLE ? '可预约' : '不可约' }}
            </van-tag>
          </div>
          <van-progress :percentage="percent(slot.maxCapacity - slot.availableCapacity, slot.maxCapacity)" />
          <p class="meta">剩余 {{ slot.availableCapacity }} / {{ slot.maxCapacity }}</p>
          <van-button
            block
            type="primary"
            :disabled="slot.status !== VenueSlotStatus.AVAILABLE || slot.availableCapacity <= 0"
            @click="openBooking(slot.id, slot.availableCapacity)"
          >
            预约这个时段
          </van-button>
        </div>
      </article>
    </div>
    <EmptyState v-else text="当天暂无可预约时段" />

    <van-calendar v-model:show="showCalendar" @confirm="selectDate" />
    <van-dialog v-model:show="bookingDialog" title="创建预约" show-cancel-button @confirm="createBooking">
      <van-cell-group inset>
        <van-field v-model.number="bookingForm.personCount" type="number" label="人数" :placeholder="`最多 ${maxCapacity}`" />
        <van-field v-model="bookingForm.remark" label="备注" placeholder="社团活动使用" />
      </van-cell-group>
    </van-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { showSuccessToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { bookingApi, venueApi } from '../api'
import { VenueSlotStatus } from '../constants/status'
import { useAuthStore } from '../stores/auth'
import { assetUrl, percent, today } from '../utils/format'
import type { VenueDetailVO, VenueSlotVO } from '../types/backend'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const venueId = Number(route.params.id)
const venue = ref<VenueDetailVO>()
const slots = ref<VenueSlotVO[]>([])
const date = ref(today())
const showCalendar = ref(false)
const bookingDialog = ref(false)
const maxCapacity = ref(1)
const bookingForm = reactive({ slotId: 0, personCount: 1, remark: '' })

async function load() {
  venue.value = await venueApi.detail(venueId)
  slots.value = await venueApi.slots(venueId, date.value)
}

function selectDate(value: Date) {
  date.value = value.toISOString().slice(0, 10)
  showCalendar.value = false
  void load()
}

function openBooking(slotId: number, capacity: number) {
  if (!auth.isAuthenticated) {
    void router.push('/login')
    return
  }
  bookingForm.slotId = slotId
  bookingForm.personCount = 1
  maxCapacity.value = capacity
  bookingDialog.value = true
}

async function createBooking() {
  await bookingApi.create({
    venueId,
    slotId: bookingForm.slotId,
    personCount: bookingForm.personCount,
    remark: bookingForm.remark,
  })
  showSuccessToast('预约成功')
  await load()
}

onMounted(load)
</script>
