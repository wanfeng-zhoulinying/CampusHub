<template>
  <AppShell>
    <div class="section-title">
      <h2>活动报名</h2>
      <van-tag type="success">审核通过</van-tag>
    </div>
    <div class="toolbar">
      <van-search
        v-model="keyword"
        placeholder="搜索标题、内容、地点"
        @search="submitSearch"
        @clear="clearSearch"
        @click-left-icon="submitSearch"
      />
      <!-- 状态筛选只作用于浏览模式；ES 搜索接口不支持状态过滤，进入搜索时隐藏 -->
      <van-dropdown-menu v-if="!isSearchMode">
        <van-dropdown-item v-model="status" :options="statusOptions" @change="restart" />
      </van-dropdown-menu>
    </div>
    <p v-if="isSearchMode && total" class="meta search-summary">
      共 {{ total }} 条与「{{ activeKeyword }}」相关的结果
    </p>

    <van-list
      v-model:loading="loading"
      :finished="finished"
      :finished-text="items.length ? '没有更多了' : ''"
      @load="load"
    >
      <div v-if="items.length" class="grid">
        <article
          v-for="item in items"
          :key="item.id"
          class="card"
          @click="$router.push(`/activities/${item.id}`)"
        >
          <img
            v-if="item.coverUrl"
            class="cover"
            :src="assetUrl(item.coverUrl)"
            :alt="plainTitle(item.title)"
          />
          <div class="card__body stack">
            <div class="line">
              <!-- 搜索结果标题含 ES 高亮标签，消毒后以 v-html 渲染命中词 -->
              <h3 class="title" v-html="renderHighlight(item.title)"></h3>
              <van-tag :type="item.status === ActivityStatus.SIGNING_UP ? 'success' : 'default'">
                {{ activityStatusText[item.status] }}
              </van-tag>
            </div>
            <p v-if="item.content" class="snippet" v-html="renderHighlight(item.content)"></p>
            <p class="meta">{{ item.location }}</p>
            <van-progress :percentage="percent(item.currentSignupCount, item.signupLimit)" />
            <p class="meta">
              报名 {{ item.currentSignupCount }} / {{ item.signupLimit }} · {{ shortTime(item.activityStartTime) }}
            </p>
          </div>
        </article>
      </div>
    </van-list>

    <EmptyState
      v-if="finished && !items.length"
      :text="loadFailed ? '加载失败，请稍后重试' : isSearchMode ? '没有匹配的活动，换个关键词试试' : '没有匹配的活动'"
    />
  </AppShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { showFailToast } from 'vant'
import AppShell from '../components/AppShell.vue'
import EmptyState from '../components/EmptyState.vue'
import { activityApi } from '../api'
import { ActivityAuditStatus, ActivityStatus, activityStatusText } from '../constants/status'
import { assetUrl, percent, shortTime } from '../utils/format'
import type { ActivityListVO, ActivitySearchItemVO } from '../types/backend'

/** 搜索接口每页条数（后端默认 10，上限 50） */
const PAGE_SIZE = 10

/** 列表/搜索两种来源统一的卡片展示结构 */
interface DisplayItem {
  id: number
  /** 搜索模式下含 <em> 高亮标签 */
  title: string
  /** 搜索模式的内容命中摘要（降级路径为空） */
  content?: string
  location: string
  coverUrl?: string
  signupLimit: number
  currentSignupCount: number
  status: number
  activityStartTime: string
}

const keyword = ref('')
/** 已提交、当前结果实际使用的搜索词（输入过程中不切换模式） */
const activeKeyword = ref('')
const status = ref(-1)
const items = ref<DisplayItem[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const finished = ref(false)
const loadFailed = ref(false)

const isSearchMode = computed(() => activeKeyword.value !== '')

const statusOptions = [
  { text: '默认报名中', value: -1 },
  { text: '未开始', value: ActivityStatus.NOT_STARTED },
  { text: '报名中', value: ActivityStatus.SIGNING_UP },
  { text: '进行中', value: ActivityStatus.IN_PROGRESS },
  { text: '已结束', value: ActivityStatus.FINISHED },
]

function fromList(item: ActivityListVO): DisplayItem {
  return {
    id: item.id,
    title: item.title,
    location: item.location,
    coverUrl: item.coverUrl,
    signupLimit: item.signupLimit,
    currentSignupCount: item.currentSignupCount,
    status: item.status,
    activityStartTime: item.activityStartTime,
  }
}

function fromSearch(item: ActivitySearchItemVO): DisplayItem {
  return {
    id: item.id,
    title: item.title,
    content: item.content,
    location: item.location,
    coverUrl: item.coverUrl,
    signupLimit: item.signupLimit,
    currentSignupCount: item.currentSignupCount,
    status: item.status,
    activityStartTime: item.activityStartTime,
  }
}

/**
 * 渲染 ES 高亮文本：先整体 HTML 转义，再放行 <em> 高亮标签，防注入。
 * 浏览模式的纯文本标题经转义后显示不变，两态复用。
 */
function renderHighlight(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/&lt;em&gt;/g, '<em>')
    .replace(/&lt;\/em&gt;/g, '</em>')
}

/** 去掉高亮标签后的纯文本，用于图片 alt */
function plainTitle(raw: string): string {
  return raw.replace(/<[^>]+>/g, '')
}

async function load() {
  loadFailed.value = false
  try {
    if (isSearchMode.value) {
      // 搜索模式：ES 分页（BM25 相关性 + 高亮）；ES 不可用时后端自动降级 MySQL LIKE，结构不变
      const result = await activityApi.search({
        keyword: activeKeyword.value,
        pageNum: page.value,
        pageSize: PAGE_SIZE,
      })
      const records = (result?.records ?? []).map(fromSearch)
      total.value = Number(result?.total ?? 0)
      items.value = page.value === 1 ? records : [...items.value, ...records]
      page.value += 1
      finished.value = records.length === 0 || items.value.length >= total.value
    } else {
      // 浏览模式：按状态筛选，接口全量返回
      const list = await activityApi.list({
        status: status.value === -1 ? undefined : status.value,
        auditStatus: ActivityAuditStatus.APPROVED,
      })
      items.value = (list ?? []).map(fromList)
      finished.value = true
    }
  } catch {
    loadFailed.value = true
    // 置为已结束，避免 van-list 在失败后滚动触底反复重试
    finished.value = true
    showFailToast(isSearchMode.value ? '搜索失败，请稍后重试' : '加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 重置分页状态并重新加载（搜索提交、清空、切换筛选时调用） */
function restart() {
  page.value = 1
  items.value = []
  total.value = 0
  finished.value = false
  loading.value = true
  void load()
}

function submitSearch() {
  activeKeyword.value = keyword.value.trim()
  restart()
}

function clearSearch() {
  keyword.value = ''
  activeKeyword.value = ''
  restart()
}
</script>

<style scoped>
.search-summary {
  margin: 0 0 10px;
}

.snippet {
  display: -webkit-box;
  overflow: hidden;
  color: #697986;
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

/* ES 命中词高亮（v-html 内容需用 :deep 穿透） */
.title :deep(em),
.snippet :deep(em) {
  padding: 0 2px;
  border-radius: 3px;
  color: #198754;
  background: rgba(25, 135, 84, 0.12);
  font-style: normal;
  font-weight: 600;
}
</style>
