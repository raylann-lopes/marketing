<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, LayoutDashboard, ListChecks } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import { useIsMobile } from '@/lib/breakpoint'
import { type Post, getPostClientId } from '@/services/postService'
import { type Client } from '@/services/clientService'
import { type TaskRecord } from '@/services/taskService'
import { combineLocalDateTime, formatDateTime, formatTaskDateTime } from '@/lib/dateTime'

const { isMobile } = useIsMobile()

const props = defineProps<{
  posts: Post[]
  tasks?: TaskRecord[]
  clients: Client[]
  showTasks?: boolean
}>()

const router = useRouter()

type QueueItem = {
  id: string
  title: string
  client: string
  kind: 'Post' | 'Tarefa'
  status: string
  statusVariant: 'secondary' | 'default' | 'warning' | 'success' | 'purple' | 'outline' | 'destructive'
  dateLabel: string
  sortDate: string
  rank: number
  route: string
}

const actionablePostStatus: Record<string, Omit<QueueItem, 'id' | 'title' | 'client' | 'dateLabel' | 'sortDate'>> = {
  REJECTED: { kind: 'Post', status: 'Revisão solicitada', statusVariant: 'destructive', rank: 0, route: '/board' },
  WAITING_APPROVAL: { kind: 'Post', status: 'Aguardando aprovação', statusVariant: 'warning', rank: 1, route: '/approvals' },
  IN_PRODUCTION: { kind: 'Post', status: 'Em produção', statusVariant: 'default', rank: 3, route: '/board' },
  DEMAND: { kind: 'Post', status: 'Nova demanda', statusVariant: 'secondary', rank: 4, route: '/board' },
  FINISHED: { kind: 'Post', status: 'Finalizado', statusVariant: 'success', rank: 5, route: '/board' },
  SCHEDULE: { kind: 'Post', status: 'Agendado', statusVariant: 'purple', rank: 7, route: '/calendar' },
}

const operationItems = computed<QueueItem[]>(() => {
  const todayKey = toDateKey(new Date())

  const postItems = props.posts.flatMap(post => {
    const config = actionablePostStatus[post.status]
    if (!config || !post.scheduledAt) return []
    return [{
      id: `post-${post.id}`,
      title: post.title,
      client: getClientName(getPostClientId(post) || undefined),
      dateLabel: formatDateTime(post.scheduledAt),
      sortDate: post.scheduledAt,
      ...config,
    }]
  })

  const taskItems = (props.tasks ?? []).map(task => {
    const overdue = task.dateExpires < todayKey
    const isToday = task.dateExpires === todayKey
    const isCritical = task.priority === 'URGENT' || task.priority === 'HIGH'
    const status = overdue ? 'Atrasada' : isCritical ? 'Alta prioridade' : isToday ? 'Para hoje' : 'Pendente'

    return {
      id: `task-${task.id}`,
      title: task.title,
      client: task.clientName || 'Sem cliente',
      kind: 'Tarefa' as const,
      status,
      statusVariant: overdue ? 'destructive' as const : isCritical ? 'warning' as const : 'outline' as const,
      dateLabel: formatTaskDateTime(task.dateExpires, task.timeExpires),
      sortDate: combineLocalDateTime(task.dateExpires, task.timeExpires, '23:59') ?? task.dateExpires,
      rank: overdue ? 0 : isCritical ? 2 : isToday ? 3 : 6,
      route: '/tasks',
    }
  })

  return [...postItems, ...taskItems]
    .sort((a, b) => a.rank - b.rank || new Date(a.sortDate).getTime() - new Date(b.sortDate).getTime())
    .slice(0, 8)
})

function toDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function getClientName(clientId: string | number | undefined) {
  if (!clientId) return 'Sem cliente'
  return props.clients.find(client => String(client.id) === String(clientId))?.name || 'Sem cliente'
}

</script>

<template>
  <Card class="overflow-hidden rounded-lg">
    <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="font-semibold text-gray-900">Prioridades da operação</h2>
        <p class="mt-0.5 text-xs text-gray-500">Tarefas, aprovações e conteúdos que exigem atenção.</p>
      </div>
      <div class="flex gap-2">
        <Button variant="ghost" size="sm" class="h-8 px-2 text-xs" @click="router.push('/board')">
          <LayoutDashboard class="h-3.5 w-3.5" /> Board
        </Button>
        <Button
          v-if="showTasks"
          variant="ghost"
          size="sm"
          class="h-8 px-2 text-xs"
          @click="router.push('/tasks')"
        >
          <ListChecks class="h-3.5 w-3.5" /> Tarefas
        </Button>
      </div>
    </div>

    <div v-if="operationItems.length === 0" class="py-10 text-center text-sm text-gray-400">
      Nenhuma pendência operacional encontrada.
    </div>

    <table v-else-if="!isMobile" class="w-full table-fixed">
      <thead>
        <tr class="border-b border-gray-100">
          <th class="w-[34%] px-5 py-3 text-left text-xs font-semibold uppercase text-gray-400">Item</th>
          <th class="w-[22%] px-4 py-3 text-left text-xs font-semibold uppercase text-gray-400">Cliente</th>
          <th class="w-[20%] px-4 py-3 text-left text-xs font-semibold uppercase text-gray-400">Situação</th>
          <th class="w-[19%] px-4 py-3 text-left text-xs font-semibold uppercase text-gray-400">Data</th>
          <th class="w-[5%] px-3 py-3" />
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="item in operationItems"
          :key="item.id"
          class="cursor-pointer border-b border-gray-50 transition-colors hover:bg-gray-50"
          @click="router.push(item.route)"
        >
          <td class="px-5 py-3">
            <p class="truncate text-sm font-medium text-gray-800">{{ item.title }}</p>
            <p class="mt-0.5 text-[11px] text-gray-400">{{ item.kind }}</p>
          </td>
          <td class="truncate px-4 py-3 text-sm text-gray-500">{{ item.client }}</td>
          <td class="px-4 py-3"><Badge :variant="item.statusVariant" class="text-[10px]">{{ item.status }}</Badge></td>
          <td class="px-4 py-3 text-xs font-medium text-gray-500">{{ item.dateLabel }}</td>
          <td class="px-3 py-3"><ArrowRight class="h-4 w-4 text-gray-300" /></td>
        </tr>
      </tbody>
    </table>

    <div v-else class="divide-y divide-gray-50">
      <button
        v-for="item in operationItems"
        :key="item.id"
        class="flex w-full items-center justify-between gap-3 p-4 text-left transition-colors active:bg-gray-50"
        @click="router.push(item.route)"
      >
        <div class="min-w-0">
          <p class="truncate text-sm font-medium text-gray-800">{{ item.title }}</p>
          <p class="mt-0.5 truncate text-xs text-gray-500">{{ item.client }} · {{ item.dateLabel }}</p>
        </div>
        <Badge :variant="item.statusVariant" class="shrink-0 text-[10px]">{{ item.status }}</Badge>
      </button>
    </div>
  </Card>
</template>
