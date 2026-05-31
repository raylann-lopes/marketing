<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import { type Post, getPostClientId } from '@/services/postService'
import { type Client } from '@/services/clientService'

interface Props {
  posts: Post[]
  clients: Client[]
}

const props = defineProps<Props>()
const router = useRouter()

const statusLabel: Record<string, string> = {
  DEMAND: 'Demanda',
  IN_PRODUCTION: 'Em Produção',
  WAITING_APPROVAL: 'Aguard. Aprovação',
  FINISHED: 'Finalizado',
  SCHEDULE: 'Agendado',
  PUBLISHED: 'Publicado',
}

const statusVariant = computed(() => (status: string) => {
  const map: Record<string, string> = {
    DEMAND: 'secondary',
    IN_PRODUCTION: 'default',
    WAITING_APPROVAL: 'warning',
    FINISHED: 'success',
    SCHEDULE: 'outline',
    PUBLISHED: 'purple',
  }
  return (map[status] || 'outline') as 'secondary' | 'default' | 'warning' | 'success' | 'purple' | 'outline'
})

function getClientName(clientId: string | number | undefined) {
  if (!clientId) return '—'
  const client = props.clients.find(c => String(c.id) === String(clientId))
  return client ? client.name : `ID: ${clientId}`
}
</script>

<template>
  <Card class="overflow-hidden">
    <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
      <h2 class="font-semibold text-gray-900">Demandas Recentes</h2>
      <button class="text-xs text-primary hover:underline" @click="router.push('/board')">Ver board →</button>
    </div>
    <div v-if="posts.length === 0" class="py-8 text-center text-sm text-gray-400">Nenhuma demanda cadastrada.</div>
    <table v-else class="w-full">
      <thead>
        <tr class="border-b border-gray-100">
          <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Título</th>
          <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Cliente</th>
          <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Status</th>
          <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Data</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="post in posts"
          :key="post.id"
          class="border-b border-gray-50 hover:bg-gray-50 transition-colors cursor-pointer"
          @click="router.push('/board')"
        >
          <td class="px-5 py-3 text-sm font-medium text-gray-800 max-w-[180px] truncate">{{ post.title }}</td>
          <td class="px-5 py-3 text-sm text-gray-500">
            {{ getClientName(getPostClientId(post) || undefined) }}
          </td>
          <td class="px-5 py-3">
            <Badge :variant="statusVariant(post.status)" class="text-[10px]">
              {{ statusLabel[post.status] || post.status }}
            </Badge>
          </td>
          <td class="px-5 py-3 text-xs text-gray-400">
            {{ post.scheduledAt ? new Date(post.scheduledAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }) : '—' }}
          </td>
        </tr>
      </tbody>
    </table>
  </Card>
</template>
