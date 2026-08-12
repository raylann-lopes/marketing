<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  Kanban,
  CalendarDays,
  CheckCircle2,
  Users,
  Lightbulb,
  FileText,
  CreditCard,
  PlusCircle,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  ListChecks,
} from 'lucide-vue-next'
import type { Component } from 'vue'
import { computed, ref } from 'vue'
import { setCurrentUserId } from '@/lib/api'
import { useIsMobile } from '@/lib/breakpoint'
import logoUrl from '@/assets/logo.png'

const router = useRouter()
const role = localStorage.getItem('role') || sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'
const { isMobile } = useIsMobile()
const SIDEBAR_COLLAPSED_KEY = 'north_sidebar_collapsed'
// Sem preferência salva ainda, começa fechada em telas pequenas — depois
// que o usuário alterna manualmente, a escolha fica salva e vale pra sempre
const savedCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY)
const isCollapsed = ref(savedCollapsed !== null ? savedCollapsed === 'true' : isMobile.value)

type NavItem = {
  label: string
  to: string
  icon: Component
}

const navItems: NavItem[] = [
  { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Board de Produção', to: '/board', icon: Kanban },
  { label: 'Calendário Editorial', to: '/calendar', icon: CalendarDays },
  { label: 'Aprovações', to: '/approvals', icon: CheckCircle2 },
  { label: 'Ideias de Conteúdo', to: '/ideas', icon: Lightbulb },
  { label: 'Tarefas', to: '/tasks', icon: ListChecks },
]

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('userId')
  setCurrentUserId(null)
  router.push('/login')
}

if (isAdmin) {
  navItems.push({ label: 'Relatórios do Cliente', to: '/client-reports', icon: FileText })
  navItems.push({ label: 'Clientes', to: '/clients', icon: Users })
  navItems.push({ label: 'Financeiro', to: '/finance', icon: CreditCard })
  navItems.push({ label: 'Usuários', to: '/admin/users', icon: ShieldCheck })
}

// Calendário não coube no celular (view própria já bloqueia o acesso) —
// removido do menu no mobile para não oferecer um link morto
const visibleNavItems = computed(() =>
  isMobile.value ? navItems.filter(item => item.to !== '/calendar') : navItems,
)

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(isCollapsed.value))
}
</script>

<template>
  <aside
    :class="[
      'relative min-h-screen bg-[#EEF2FF] flex flex-col shrink-0 transition-all duration-200',
      isCollapsed ? 'w-16' : 'w-60',
    ]"
  >
    <!-- Toggle — bolinha fixa na borda da sidebar, sempre visível -->
    <button
      type="button"
      :title="isCollapsed ? 'Abrir menu' : 'Fechar menu'"
      :aria-label="isCollapsed ? 'Abrir menu lateral' : 'Fechar menu lateral'"
      class="absolute -right-3 top-[60px] z-10 flex h-6 w-6 items-center justify-center rounded-full border border-indigo-100 bg-white text-gray-400 shadow-sm transition-colors hover:border-primary/30 hover:text-primary"
      @click="toggleSidebar"
    >
      <ChevronRight v-if="isCollapsed" class="h-3.5 w-3.5" />
      <ChevronLeft v-else class="h-3.5 w-3.5" />
    </button>

    <!-- Logo -->
    <div :class="['pt-6 pb-4', isCollapsed ? 'px-2' : 'px-5']">
      <div :class="['flex items-center', isCollapsed ? 'justify-center' : 'gap-2.5']">
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white shadow-sm ring-1 ring-indigo-100/80"
        >
          <img :src="logoUrl" alt="North Produções" class="h-7 w-7 object-contain" />
        </div>
        <div v-if="!isCollapsed" class="flex min-w-0 flex-col leading-tight">
          <span class="truncate text-[15px] font-bold tracking-tight text-gray-900">North</span>
          <span class="truncate text-[10px] font-semibold uppercase tracking-wider text-gray-400">Produções</span>
        </div>
      </div>
    </div>

    <!-- Nav -->
    <nav :class="['flex-1 space-y-1', isCollapsed ? 'px-2' : 'px-3']">
      <RouterLink v-for="item in visibleNavItems" :key="item.to" :to="item.to" v-slot="{ isActive }">
        <div
          :title="isCollapsed ? item.label : undefined"
          :class="[
            'flex items-center rounded-lg text-sm font-medium transition-colors cursor-pointer',
            isCollapsed ? 'justify-center px-0 py-2.5' : 'gap-3 px-3 py-2.5',
            isActive
              ? 'bg-primary text-white shadow-sm'
              : 'text-gray-600 hover:bg-indigo-100/50 hover:text-primary',
          ]"
        >
          <component :is="item.icon" class="w-4 h-4 shrink-0" />
          <span v-if="!isCollapsed">{{ item.label }}</span>
        </div>
      </RouterLink>
    </nav>

    <!-- Bottom -->
    <div :class="['border-t border-indigo-100/70 space-y-2', isCollapsed ? 'p-2' : 'p-3']">
      <button
        type="button"
        :title="isCollapsed ? 'Novo Projeto' : undefined"
        :class="[
          'my-2.5 w-full flex items-center justify-center rounded-lg border border-primary/20 bg-white text-sm font-semibold text-primary shadow-sm transition-all hover:border-primary/30 hover:bg-primary/10 active:scale-[0.98]',
          isCollapsed ? 'h-10 px-0' : 'gap-2 px-4 py-2.5',
        ]"
        @click="router.push({ path: '/board', query: { new: '1' } })"
      >
        <PlusCircle class="w-4 h-4" />
        <span v-if="!isCollapsed">Novo Projeto</span>
      </button>

      <RouterLink to="/settings" v-slot="{ isActive }">
        <div
          :title="isCollapsed ? 'Configurações' : undefined"
          :class="[
            'flex items-center rounded-lg text-sm font-medium transition-colors cursor-pointer',
            isCollapsed ? 'justify-center px-0 py-2.5' : 'gap-3 px-3 py-2.5',
            isActive
              ? 'bg-primary text-white shadow-sm'
              : 'text-gray-600 hover:bg-indigo-100/50 hover:text-primary',
          ]"
        >
          <Settings class="w-4 h-4" />
          <span v-if="!isCollapsed">Configurações</span>
        </div>
      </RouterLink>

      <button
        type="button"
        :title="isCollapsed ? 'Sair' : undefined"
        :class="[
          'w-full flex items-center rounded-lg text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors',
          isCollapsed ? 'justify-center px-0 py-2.5' : 'gap-3 px-3 py-2.5',
        ]"
        @click="logout"
      >
        <LogOut class="w-4 h-4" />
        <span v-if="!isCollapsed">Sair</span>
      </button>
    </div>
  </aside>
</template>
