import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/home', component: () => import('../views/HomeView.vue') },
    { path: '/venues', component: () => import('../views/VenueListView.vue') },
    { path: '/venues/:id', component: () => import('../views/VenueDetailView.vue') },
    { path: '/activities', component: () => import('../views/ActivityListView.vue') },
    { path: '/activities/:id', component: () => import('../views/ActivityDetailView.vue') },
    { path: '/bookings', component: () => import('../views/BookingsView.vue'), meta: { auth: true } },
    { path: '/my-activities', component: () => import('../views/MyActivitiesView.vue'), meta: { auth: true } },
    { path: '/credit', component: () => import('../views/CreditView.vue'), meta: { auth: true } },
    { path: '/messages', component: () => import('../views/MessagesView.vue'), meta: { auth: true } },
    { path: '/favorites', component: () => import('../views/FavoritesView.vue'), meta: { auth: true } },
    { path: '/profile', component: () => import('../views/ProfileView.vue') },
    { path: '/admin', component: () => import('../views/AdminWorkspaceView.vue'), meta: { auth: true, admin: true } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.isAuthenticated && !auth.profile) {
    await auth.loadProfile()
  }
  if (to.meta.auth && !auth.isAuthenticated) return '/login'
  if (to.meta.admin && !auth.isAdmin) return '/profile'
  return true
})
