<template>
  <AppShell>
    <article v-if="activity" class="card">
      <img v-if="activity.coverUrl" class="cover" :src="assetUrl(activity.coverUrl)" :alt="activity.title" />
      <div class="card__body stack">
        <div class="line">
          <h2 class="title">{{ activity.title }}</h2>
          <van-tag :type="activity.status === ActivityStatus.SIGNING_UP ? 'success' : 'default'">
            {{ activityStatusText[activity.status] }}
          </van-tag>
        </div>
        <p class="meta">{{ activity.location }}</p>
        <p>{{ activity.content || '暂无活动介绍' }}</p>
        <van-progress :percentage="percent(activity.currentSignupCount, activity.signupLimit)" />
        <p class="meta">
          报名 {{ activity.currentSignupCount }} / {{ activity.signupLimit }}，候补 {{ activity.waitLimit }} 人
        </p>
        <p class="meta">报名：{{ shortTime(activity.signupStartTime) }} 至 {{ shortTime(activity.signupEndTime) }}</p>
        <p class="meta">活动：{{ shortTime(activity.activityStartTime) }} 至 {{ shortTime(activity.activityEndTime) }}</p>
        <div class="quick-actions">
          <van-button type="primary" :loading="loading" :disabled="signupState.disabled || cooldown" @click="signup">
            {{ signupState.text }}
          </van-button>
          <!-- 已报名/候补中时提供取消入口，闭环不必跳转"我的报名"页 -->
          <van-button v-if="mySignup" plain :loading="loading" @click="cancelSignup">取消报名</van-button>
          <van-button plain icon="star-o" :loading="loading" @click="favorite">收藏</van-button>
        </div>
      </div>
    </article>

    <div class="section-title">
      <h2>活动评论</h2>
      <van-button size="small" icon="chat-o" @click="commentDialog = true">评论</van-button>
    </div>
    <div v-if="comments.length" class="stack">
      <article v-for="item in comments" :key="item.id" class="card">
        <div class="card__body">
          <div class="line">
            <strong>{{ item.realName || item.username }}</strong>
            <van-button size="mini" plain icon="good-job-o" @click="toggleLike(item.id)">
              {{ item.likeCount }}
            </van-button>
          </div>
          <p>{{ item.content }}</p>
          <p class="meta">{{ shortTime(item.createTime) }}</p>
        </div>
      </article>
    </div>
    <EmptyState v-else text="还没有评论" />

    <van-dialog v-model:show="commentDialog" title="发布评论" show-cancel-button @confirm="sendComment">
      <van-field v-model="comment" type="textarea" rows="3" placeholder="写下你的想法" />
    </van-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { activityApi, socialApi } from '../api'
import { ActivitySignupStatus, ActivityStatus, activityStatusText } from '../constants/status'
import { useAuthStore } from '../stores/auth'
import { assetUrl, percent, shortTime } from '../utils/format'
import type { ActivityCommentVO, ActivityDetailVO, ActivitySignupVO } from '../types/backend'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const activityId = Number(route.params.id)
const activity = ref<ActivityDetailVO>()
const comments = ref<ActivityCommentVO[]>([])
const comment = ref('')
const commentDialog = ref(false)
const loading = ref(false)
/** 我的报名记录，用于详情页判断"是否已报名/候补" */
const mySignups = ref<ActivitySignupVO[]>([])
/** 报名冷却 5 秒：与后端防重窗口对齐，双保险防连点 */
const cooldown = ref(false)

/** 当前用户在本活动的有效报名记录（已取消的不算） */
const mySignup = computed(() => {
  if (!auth.isAuthenticated) return undefined
  return mySignups.value.find(
    (item) => item.activityId === activityId && item.signupStatus !== ActivitySignupStatus.CANCELED,
  )
})

/** 报名按钮状态机：结合活动状态、名额与我的报名记录，给出按钮文案与可用性 */
const signupState = computed(() => {
  const a = activity.value
  if (!a) return { text: '加载中', disabled: true }
  const mine = mySignup.value
  if (mine) {
    if (mine.signupStatus === ActivitySignupStatus.WAITLISTED) {
      return { text: `候补中 · 第 ${mine.waitOrder ?? '-'} 位`, disabled: true }
    }
    return { text: '已报名', disabled: true }
  }
  switch (a.status) {
    case ActivityStatus.NOT_STARTED:
      return { text: '尚未开始报名', disabled: true }
    case ActivityStatus.SIGNING_UP: {
      const full = a.currentSignupCount >= a.signupLimit
      return { text: full ? '已满 · 提交候补' : '立即报名', disabled: false }
    }
    case ActivityStatus.SIGNUP_ENDED:
      return { text: '报名已截止', disabled: true }
    case ActivityStatus.IN_PROGRESS:
      return { text: '进行中', disabled: true }
    case ActivityStatus.FINISHED:
      return { text: '已结束', disabled: true }
    default:
      return { text: '活动已取消', disabled: true }
  }
})

async function load() {
  activity.value = await activityApi.detail(activityId)
  if (auth.isAuthenticated) {
    comments.value = await socialApi.comments(activityId)
    mySignups.value = await activityApi.my()
  }
}

async function requireLogin() {
  if (!auth.isAuthenticated) {
    await router.push('/login')
    return false
  }
  return true
}

async function signup() {
  if (!(await requireLogin())) return
  loading.value = true
  try {
    await activityApi.signup(activityId)
    showSuccessToast('报名成功，已刷新状态')
    cooldown.value = true
    setTimeout(() => {
      cooldown.value = false
    }, 5000)
    await load()
  } catch (error) {
    showFailToast((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function cancelSignup() {
  const mine = mySignup.value
  if (!mine) return
  loading.value = true
  try {
    await activityApi.cancel(mine.id)
    showSuccessToast('已取消报名')
    await load()
  } catch (error) {
    showFailToast((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function favorite() {
  if (!(await requireLogin())) return
  loading.value = true
  try {
    const favorited = await socialApi.favorite(activityId)
    showSuccessToast(favorited ? '已收藏' : '已取消收藏')
  } finally {
    loading.value = false
  }
}

async function sendComment() {
  if (!(await requireLogin())) return
  if (!comment.value.trim()) return
  await socialApi.comment(activityId, comment.value.trim())
  comment.value = ''
  showSuccessToast('评论已发布')
  comments.value = await socialApi.comments(activityId)
}

async function toggleLike(commentId: number) {
  if (!(await requireLogin())) return
  await socialApi.like(commentId)
  comments.value = await socialApi.comments(activityId)
}

onMounted(load)
</script>
