<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  Kanban,
  CalendarDays,
  Users,
  CreditCard,
  PlusCircle,
  Settings,
  LogOut,
} from 'lucide-vue-next'
import type { Component } from 'vue'

const router = useRouter()

type NavItem = {
  label: string
  to: string
  icon: Component
}

const navItems: NavItem[] = [
  { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Board de Produção', to: '/board', icon: Kanban },
  { label: 'Calendário Editorial', to: '/calendar', icon: CalendarDays },
  { label: 'Clientes', to: '/clients', icon: Users },
  { label: 'Financeiro', to: '/finance', icon: CreditCard },
]

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
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
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        v-slot="{ isActive }"
      >
        <div
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer',
            isActive
              ? 'bg-primary text-white shadow-sm'
              : 'text-gray-600 hover:bg-indigo-100/50 hover:text-primary'
          ]"
        >
          <component :is="item.icon" class="w-4 h-4 shrink-0" />
          {{ item.label }}
        </div>
      </RouterLink>
    </nav>

    <!-- Bottom -->
    <div class="p-3 space-y-1">
      <button
        class="w-full flex items-center justify-center gap-2 bg-primary hover:bg-primary/90 text-white text-sm font-medium py-2.5 px-4 rounded-lg transition-all shadow-sm active:scale-[0.98]"
        @click="router.push('/dashboard')"
      >
        <PlusCircle class="w-4 h-4" />
        Novo Projeto
      </button>

      <RouterLink
        to="/settings"
        v-slot="{ isActive }"
      >
        <div
          :class="[
            'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer',
            isActive
              ? 'bg-primary text-white shadow-sm'
              : 'text-gray-600 hover:bg-indigo-100/50 hover:text-primary'
          ]"
        >
          <Settings class="w-4 h-4" />
          Configurações
        </div>
      </RouterLink>

      <button
        class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors"
        @click="logout"
      >
        <LogOut class="w-4 h-4" />
        Sair
      </button>
    </div>
  </aside>
</template>
