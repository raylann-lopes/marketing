<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  Kanban,
  CalendarDays,
  CheckCircle2,
  Users,
  Lightbulb,
  Bot,
  FileText,
  CreditCard,
  PlusCircle,
  Settings,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
} from 'lucide-vue-next'
import type { Component } from 'vue'
import { ref } from 'vue'
import { setCurrentUserId } from '@/lib/api'

const router = useRouter()
const role = localStorage.getItem('role') || sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'
const SIDEBAR_COLLAPSED_KEY = 'north_sidebar_collapsed'
const isCollapsed = ref(localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true')

type NavItem = {
  label: string
  to: string
  icon: Component
}

const navItems: NavItem[] = [
  { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Board de Produção', to: '/board', icon: Kanban },
  { label: 'Aprovações', to: '/approvals', icon: CheckCircle2 },
  { label: 'Calendário Editorial', to: '/calendar', icon: CalendarDays },
  { label: 'Clientes', to: '/clients', icon: Users },
  { label: 'Ideias de Conteúdo', to: '/ideas', icon: Lightbulb },
  { label: 'Hub de Automação', to: '/automation-hub', icon: Bot },
  { label: 'Relatórios do Cliente', to: '/client-reports', icon: FileText },
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
  navItems.push({ label: 'Financeiro', to: '/finance', icon: CreditCard })
}

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(isCollapsed.value))
}
</script>

<template>
  <aside
    :class="[
      'min-h-screen bg-[#EEF2FF] flex flex-col shrink-0 transition-all duration-200',
      isCollapsed ? 'w-16' : 'w-60'
    ]"
  >
    <!-- Logo -->
    <div :class="['pt-6 pb-4', isCollapsed ? 'px-2' : 'px-6']">
      <div :class="['flex items-start gap-2', isCollapsed ? 'justify-center' : 'justify-between']">
        <div v-if="!isCollapsed">
          <h1 class="text-xl font-bold text-[#6B21A8]">North Produções</h1>
          <p class="text-xs text-gray-500 mt-0.5">Digital Ateliê</p>
        </div>
        <button
          type="button"
          :title="isCollapsed ? 'Abrir menu' : 'Fechar menu'"
          :aria-label="isCollapsed ? 'Abrir menu lateral' : 'Fechar menu lateral'"
          class="flex h-9 w-9 items-center justify-center rounded-lg text-gray-500 transition-colors hover:bg-indigo-100/70 hover:text-primary"
          @click="toggleSidebar"
        >
          <PanelLeftOpen v-if="isCollapsed" class="h-4 w-4" />
          <PanelLeftClose v-else class="h-4 w-4" />
        </button>
      </div>
    </div>

    <!-- Nav -->
    <nav :class="['flex-1 space-y-1', isCollapsed ? 'px-2' : 'px-3']">
      <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" v-slot="{ isActive }">
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
          isCollapsed ? 'h-10 px-0' : 'gap-2 px-4 py-2.5'
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
          isCollapsed ? 'justify-center px-0 py-2.5' : 'gap-3 px-3 py-2.5'
        ]"
        @click="logout"
      >
        <LogOut class="w-4 h-4" />
        <span v-if="!isCollapsed">Sair</span>
      </button>
    </div>
  </aside>
</template>
