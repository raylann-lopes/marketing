<script setup lang="ts">
import Card from '@/components/ui/Card.vue'

type ClientHealth = 'READY' | 'ATTENTION' | 'BLOCKED'

interface AutomationClient {
  id: number
  name: string
  niche: string
  health: ClientHealth
  score: number
  summary: string
  nextAction: string
  missing: string[]
}

interface Props {
  clients: AutomationClient[]
  selectedClientId: number
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:selectedClientId', val: number): void
}>()

const healthLabels: Record<ClientHealth, string> = {
  READY: 'Pronto',
  ATTENTION: 'Atenção',
  BLOCKED: 'Bloqueado',
}

const healthClasses: Record<ClientHealth, string> = {
  READY: 'bg-emerald-50 text-emerald-700 border-emerald-100',
  ATTENTION: 'bg-orange-50 text-orange-700 border-orange-100',
  BLOCKED: 'bg-red-50 text-red-700 border-red-100',
}
</script>

<template>
  <Card class="flex min-h-0 flex-col overflow-hidden">
    <div class="shrink-0 border-b border-gray-100 px-4 py-3">
      <h2 class="font-semibold text-gray-900">Clientes</h2>
      <p class="mt-0.5 text-xs text-gray-500">Selecione para ver automações e bloqueios.</p>
    </div>

    <div class="min-h-0 space-y-2 overflow-hidden p-3">
      <button
        v-for="client in clients"
        :key="client.id"
        type="button"
        :class="[
          'w-full rounded-lg border p-2.5 text-left transition-colors',
          selectedClientId === client.id
            ? 'border-primary/30 bg-primary/5'
            : 'border-gray-100 bg-white hover:bg-gray-50',
        ]"
        @click="$emit('update:selectedClientId', client.id)"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="truncate text-sm font-semibold text-gray-900">{{ client.name }}</p>
            <p class="mt-0.5 truncate text-xs text-gray-500">{{ client.niche }}</p>
          </div>
          <span :class="['rounded-full border px-2 py-0.5 text-[10px] font-semibold', healthClasses[client.health]]">
            {{ healthLabels[client.health] }}
          </span>
        </div>
        <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-gray-100">
          <div class="h-full rounded-full bg-primary" :style="{ width: `${client.score}%` }" />
        </div>
      </button>
    </div>
  </Card>
</template>
