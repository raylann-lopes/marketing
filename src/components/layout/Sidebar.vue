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
} from 'lucide-vue-next'
import type { Component } from 'vue'
import { setCurrentUserId } from '@/lib/api'

const router = useRouter()
const role = localStorage.getItem('role') || sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'

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
</script>

<template>
  <aside class="w-60 min-h-screen bg-[#EEF2FF] flex flex-col shrink-0">
    <!-- Logo -->
    <div class="px-6 pt-6 pb-4">
      <h1 class="text-xl font-bold text-[#6B21A8]">North Produções</h1>
      <p class="text-xs text-gray-500 mt-0.5">Digital Ateliê</p>
    </div>

    <!-- Nav -->
    <nav class="flex-1 px-3 space-y-1">
      <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" v-slot="{ isActive }">
        <div
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer',
            isActive
              ? 'bg-primary text-white shadow-sm'
              : 'text-gray-600 hover:bg-indigo-100/50 hover:text-primary',
          ]"
        >
          <component :is="item.icon" class="w-4 h-4 shrink-0" />
          {{ item.label }}
        </div>
      </RouterLink>
    </nav>

    <!-- Bottom -->
    <div class="border-t border-indigo-100/70 p-3 space-y-2">
      <button
        type="button"
        class="my-2.5 w-full flex items-center justify-center gap-2 rounded-lg border border-primary/20 bg-white px-4 py-2.5 text-sm font-semibold text-primary shadow-sm transition-all hover:border-primary/30 hover:bg-primary/10 active:scale-[0.98]"
        @click="router.push({ path: '/board', query: { new: '1' } })"
      >
        <PlusCircle class="w-4 h-4" />
        Novo Projeto
      </button>

      <RouterLink to="/settings" v-slot="{ isActive }">
        <div
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer',
            isActive
              ? 'bg-primary text-white shadow-sm'
              : 'text-gray-600 hover:bg-indigo-100/50 hover:text-primary',
          ]"
        >
          <Settings class="w-4 h-4" />
          Configurações
        </div>
      </RouterLink>

      <button
        type="button"
        class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors"
        @click="logout"
      >
        <LogOut class="w-4 h-4" />
        Sair
      </button>
    </div>
  </aside>
</template>
