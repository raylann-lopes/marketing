<script setup lang="ts">
import { type Component } from 'vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'

type AutomationStatus = 'ACTIVE' | 'PAUSED' | 'ERROR' | 'MISSING_SETUP'

interface AutomationModule {
  id: number
  clientId: number
  title: string
  description: string
  icon: Component
  status: AutomationStatus
  lastRun: string
  nextRun: string
  impact: string
  primaryAction: string
}

interface Props {
  automation: AutomationModule
}

defineProps<Props>()

const statusLabels: Record<AutomationStatus, string> = {
  ACTIVE: 'Ativa',
  PAUSED: 'Pausada',
  ERROR: 'Erro',
  MISSING_SETUP: 'Configurar',
}

const statusVariants: Record<AutomationStatus, 'success' | 'warning' | 'destructive' | 'purple'> = {
  ACTIVE: 'success',
  PAUSED: 'warning',
  ERROR: 'destructive',
  MISSING_SETUP: 'purple',
}
</script>

<template>
  <div class="rounded-lg border border-gray-100 bg-white p-3 transition-shadow hover:shadow-sm">
    <div class="flex items-start justify-between gap-3">
      <div class="flex min-w-0 items-start gap-3">
        <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gray-50 text-primary">
          <component :is="automation.icon" class="h-4 w-4" />
        </div>
        <div class="min-w-0">
          <p class="text-sm font-semibold text-gray-900">{{ automation.title }}</p>
          <p class="mt-1 line-clamp-2 text-xs leading-5 text-gray-500">{{ automation.description }}</p>
        </div>
      </div>
      <Badge :variant="statusVariants[automation.status]" class="shrink-0 text-[10px]">
        {{ statusLabels[automation.status] }}
      </Badge>
    </div>

    <div class="mt-3 grid grid-cols-2 gap-3 rounded-lg bg-gray-50 p-2.5">
      <div>
        <p class="text-[10px] font-semibold uppercase text-gray-400">Última</p>
        <p class="mt-1 text-xs font-medium text-gray-800">{{ automation.lastRun }}</p>
      </div>
      <div>
        <p class="text-[10px] font-semibold uppercase text-gray-400">Próxima</p>
        <p class="mt-1 text-xs font-medium text-gray-800">{{ automation.nextRun }}</p>
      </div>
    </div>

    <div class="mt-3 flex items-center justify-between gap-3">
      <p class="text-xs font-medium text-gray-500">{{ automation.impact }}</p>
      <Button size="sm" :variant="automation.status === 'ERROR' ? 'default' : 'outline'" class="h-8">
        {{ automation.primaryAction }}
      </Button>
    </div>
  </div>
</template>
