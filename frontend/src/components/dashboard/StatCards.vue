<script setup lang="ts">
import {
  CalendarClock,
  CheckCircle2,
  CircleAlert,
  ClipboardCheck,
  ListTodo,
  PanelsTopLeft,
  Users,
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import Card from '@/components/ui/Card.vue'

type DashboardStat = {
  label: string
  value: number
  detail: string
  icon: 'approval' | 'production' | 'schedule' | 'published' | 'clients' | 'tasks' | 'alert'
  tone: 'amber' | 'blue' | 'violet' | 'green' | 'slate' | 'red'
  route?: string
}

defineProps<{
  stats: DashboardStat[]
}>()

const router = useRouter()

const iconMap = {
  approval: ClipboardCheck,
  production: PanelsTopLeft,
  schedule: CalendarClock,
  published: CheckCircle2,
  clients: Users,
  tasks: ListTodo,
  alert: CircleAlert,
}

const toneMap = {
  amber: 'bg-amber-50 text-amber-600',
  blue: 'bg-blue-50 text-blue-600',
  violet: 'bg-violet-50 text-violet-600',
  green: 'bg-emerald-50 text-emerald-600',
  slate: 'bg-gray-100 text-gray-600',
  red: 'bg-red-50 text-red-600',
}
</script>

<template>
  <div class="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
    <Card
      v-for="stat in stats"
      :key="stat.label"
      :class="[
        'min-h-[116px] rounded-lg p-4 transition',
        stat.route ? 'cursor-pointer hover:border-gray-300 hover:shadow-md' : '',
      ]"
      @click="stat.route && router.push(stat.route)"
    >
      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <p class="line-clamp-2 text-[11px] font-semibold uppercase leading-4 text-gray-400">{{ stat.label }}</p>
          <p class="mt-1 text-2xl font-bold text-gray-900">{{ stat.value }}</p>
        </div>
        <span :class="['flex h-8 w-8 shrink-0 items-center justify-center rounded-lg', toneMap[stat.tone]]">
          <component :is="iconMap[stat.icon]" class="h-4 w-4" />
        </span>
      </div>
      <p class="mt-2 truncate text-[11px] text-gray-500">{{ stat.detail }}</p>
    </Card>
  </div>
</template>
