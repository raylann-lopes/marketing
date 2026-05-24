import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setCurrentUserId } from '@/lib/api'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/dashboard', component: () => import('@/views/DashboardView.vue') },
  { path: '/board', component: () => import('@/views/BoardView.vue') },
  { path: '/calendar', component: () => import('@/views/CalendarView.vue') },
  { path: '/clients', component: () => import('@/views/ClientsView.vue') },
  { path: '/clients/:id/workspace', component: () => import('@/views/ClientWorkspaceView.vue') },
  { path: '/ideas', component: () => import('@/views/ContentIdeasView.vue') },
  {
    path: '/client-reports',
    component: () => import('@/views/UnderConstructionView.vue'),
    meta: {
      title: 'Relatórios do Cliente',
      description: 'Área reservada para relatórios e comparativos de resultados.',
    },
  },
  { path: '/finance', component: () => import('@/views/FinanceView.vue'), meta: { requiresAdmin: true } },
  { path: '/approvals', component: () => import('@/views/ApprovalsView.vue') },
  { path: '/settings', component: () => import('@/views/SettingsView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Restore userId from storage on app load
const savedUserId = localStorage.getItem('userId') || sessionStorage.getItem('userId')
if (savedUserId) setCurrentUserId(Number(savedUserId))

router.beforeEach((to) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.meta.requiresAdmin) {
    const role = localStorage.getItem('role') || sessionStorage.getItem('role')
    if (role !== 'ADMIN') return '/dashboard'
  }
  return true
})

export default router
