<template>
  <AppShell>
    <section class="hero-panel">
      <div class="hero-panel__body">
        <p class="eyebrow">个人中心</p>
        <h1 class="hero-title">{{ auth.isAuthenticated ? auth.displayName : '欢迎来到 CampusHub' }}</h1>
        <p class="muted">
          {{ auth.isAuthenticated ? roleText : '登录后可以预约场地、报名活动、接收消息并查看信用记录。' }}
        </p>
        <van-button v-if="!auth.isAuthenticated" type="primary" to="/login" style="margin-top: 16px">登录 / 注册</van-button>
      </div>
    </section>

    <div class="section-title">
      <h2>常用功能</h2>
    </div>
    <div class="quick-actions">
      <van-button icon="records-o" to="/bookings">我的预约</van-button>
      <van-button icon="flag-o" to="/my-activities">我的活动</van-button>
      <van-button icon="balance-o" to="/credit">信用中心</van-button>
      <van-button icon="star-o" to="/favorites">我的收藏</van-button>
    </div>

    <template v-if="auth.isAdmin">
      <div class="section-title">
        <h2>管理员能力</h2>
      </div>
      <article class="card">
        <div class="card__body stack">
          <p class="meta">当前账号拥有管理员角色，可以进入移动工作台处理场地、活动、申诉和统计。</p>
          <van-button type="primary" block to="/admin">进入管理工作台</van-button>
        </div>
      </article>
    </template>

    <van-button v-if="auth.isAuthenticated" block plain type="danger" style="margin-top: 18px" @click="logout">退出登录</van-button>
  </AppShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const roleText = computed(() => (auth.isAdmin ? '管理员账号，同时拥有用户端和管理工作台能力。' : '普通用户账号'))

async function logout() {
  auth.logout()
  await router.replace('/home')
}
</script>
