<template>
  <main class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <RouterLink class="brand" to="/home">
          <strong>CampusHub</strong>
          <span>{{ subtitle }}</span>
        </RouterLink>
        <nav class="desktop-nav">
          <van-button size="small" plain hairline to="/venues">场地</van-button>
          <van-button size="small" plain hairline to="/activities">活动</van-button>
          <van-button size="small" plain hairline to="/messages">
            消息
            <van-badge v-if="auth.unreadCount" :content="auth.unreadCount" />
          </van-button>
          <van-button size="small" plain hairline to="/profile">我的</van-button>
        </nav>
      </div>
    </header>
    <section class="page">
      <slot />
    </section>
    <van-tabbar class="mobile-tabbar" route fixed safe-area-inset-bottom>
      <van-tabbar-item to="/home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/venues" icon="shop-o">场地</van-tabbar-item>
      <van-tabbar-item to="/activities" icon="flag-o">活动</van-tabbar-item>
      <van-tabbar-item to="/messages" icon="envelop-o" :badge="auth.unreadCount || undefined">消息</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="contact-o">我的</van-tabbar-item>
    </van-tabbar>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const subtitle = computed(() => (auth.isAuthenticated ? `${auth.displayName} 的校园服务台` : '校园场地与活动平台'))

onMounted(() => {
  void auth.refreshUnread()
})
</script>
