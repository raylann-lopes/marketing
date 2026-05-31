<script setup lang="ts">
import Card from '@/components/ui/Card.vue'
import { type Client } from '@/services/clientService'

interface Props {
  clients: Client[]
}

defineProps<Props>()
</script>

<template>
  <Card class="p-5 bg-gradient-to-br from-primary to-north-purple-light text-white shadow-lg">
    <h2 class="font-semibold">Saúde dos Clientes</h2>
    <p v-if="clients.length > 0" class="text-sm text-purple-100 mt-1">
      {{ clients.filter(c => c.status === 'ACTIVE').length }} de {{ clients.length }} clientes ativos.
    </p>
    <p v-else class="text-sm text-purple-100 mt-1">Nenhum cliente cadastrado ainda.</p>
    <div class="flex items-center gap-2 mt-4">
      <div class="flex -space-x-2">
        <div
          v-for="i in Math.min(clients.length, 4)"
          :key="i"
          class="w-8 h-8 rounded-full bg-purple-200 border-2 border-primary flex items-center justify-center text-xs font-bold text-primary"
        >
          {{ clients[i - 1]?.name?.charAt(0)?.toUpperCase() || '?' }}
        </div>
      </div>
      <span v-if="clients.length > 4" class="text-sm font-medium bg-white/20 rounded-full px-2 py-0.5">+{{ clients.length - 4 }}</span>
    </div>
  </Card>
</template>
