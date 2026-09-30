<template>
  <main class="login-page">
    <section class="login-card">
      <div>
        <p class="eyebrow">CampusHub</p>
        <h1>校园服务统一入口</h1>
        <p class="muted">学生和管理员共用一个移动优先前端，登录后按角色展示能力。</p>
      </div>
      <van-tabs v-model:active="active">
        <van-tab title="用户登录" name="user">
          <van-form @submit="submitUser">
            <van-cell-group inset>
              <van-field v-model="loginForm.username" name="username" label="账号" placeholder="test_user_1" required />
              <van-field v-model="loginForm.password" name="password" label="密码" type="password" placeholder="123456" required />
            </van-cell-group>
            <van-button block round type="primary" native-type="submit" :loading="loading">登录用户端</van-button>
          </van-form>
        </van-tab>
        <van-tab title="管理员登录" name="admin">
          <van-form @submit="submitAdmin">
            <van-cell-group inset>
              <van-field v-model="adminForm.username" name="username" label="账号" placeholder="admin" required />
              <van-field v-model="adminForm.password" name="password" label="密码" type="password" placeholder="123456" required />
            </van-cell-group>
            <van-button block round type="primary" native-type="submit" :loading="loading">进入管理工作台</van-button>
          </van-form>
        </van-tab>
        <van-tab title="注册" name="register">
          <van-form @submit="submitRegister">
            <van-cell-group inset>
              <van-field v-model="registerForm.username" label="账号" required />
              <van-field v-model="registerForm.password" label="密码" type="password" required />
              <van-field v-model="registerForm.realName" label="姓名" required />
              <van-field v-model="registerForm.studentNo" label="学号" />
              <van-field v-model="registerForm.phone" label="手机" />
              <van-field v-model="registerForm.email" label="邮箱" />
            </van-cell-group>
            <van-button block round type="primary" native-type="submit" :loading="loading">注册并登录</van-button>
          </van-form>
        </van-tab>
      </van-tabs>
      <van-button block plain hairline to="/home">先看看公开内容</van-button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { showSuccessToast } from 'vant'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const active = ref('user')
const loading = ref(false)

const loginForm = reactive({ username: 'test_user_1', password: '123456' })
const adminForm = reactive({ username: 'admin', password: '123456' })
const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  studentNo: '',
})

async function run(action: () => Promise<void>) {
  loading.value = true
  try {
    await action()
  } finally {
    loading.value = false
  }
}

function submitUser() {
  void run(async () => {
    await auth.loginAsUser(loginForm)
    showSuccessToast('登录成功')
    await router.replace('/home')
  })
}

function submitAdmin() {
  void run(async () => {
    await auth.loginAsAdmin(adminForm)
    showSuccessToast('管理员登录成功')
    await router.replace('/admin')
  })
}

function submitRegister() {
  void run(async () => {
    await auth.register(registerForm)
    showSuccessToast('注册成功')
    await router.replace('/home')
  })
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(223, 243, 238, 0.95), rgba(245, 247, 251, 0.98)),
    url('/src/assets/hero.png') center bottom / 210px auto no-repeat;
}

.login-card {
  display: grid;
  width: min(100%, 460px);
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(23, 32, 42, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 48px rgba(26, 62, 53, 0.14);
}

h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
}

.van-button {
  margin-top: 12px;
}
</style>
