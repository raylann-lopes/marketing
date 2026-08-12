<script setup lang="ts">
import {
  CalendarDays,
  Clock3,
  FileText,
  Flag,
  ListChecks,
  MessageCircle,
  Tag,
  UserRound,
  X,
} from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import type { TaskPriority, TaskRecord, TaskStatus, TaskType } from '@/services/taskService'
import { formatDate, formatTime } from '@/lib/dateTime'

defineProps<{
  task: TaskRecord | null
}>()

defineEmits<{
  (e: 'close'): void
}>()

const typeLabels: Record<TaskType, string> = {
  TAREFA: 'Tarefa',
  REUNIAO: 'Reunião',
  LEMBRETE: 'Lembrete',
  COBRANCA: 'Cobrança',
  PRAZO: 'Prazo',
  FOLLOW_UP: 'Follow-up',
}

const priorityLabels: Record<TaskPriority, string> = {
  LOW: 'Baixa',
  NORMAL: 'Normal',
  HIGH: 'Alta',
  URGENT: 'Urgente',
}

const statusLabels: Record<TaskStatus, string> = {
  PENDING: 'Pendente',
  DONE: 'Concluída',
  CANCELED: 'Cancelada',
}

</script>

<template>
  <Teleport to="body">
    <div
      v-if="task"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      @click.self="$emit('close')"
    >
      <section
        class="max-h-[90vh] w-full max-w-lg overflow-hidden rounded-lg bg-white shadow-2xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-details-title"
      >
        <header class="flex items-start justify-between gap-4 border-b border-gray-100 px-6 py-5">
          <div class="min-w-0">
            <div class="mb-2 flex flex-wrap items-center gap-2">
              <Badge :variant="task.status === 'PENDING' ? 'warning' : task.status === 'DONE' ? 'success' : 'secondary'">
                {{ statusLabels[task.status] }}
              </Badge>
              <span class="text-xs font-medium text-gray-400">Tarefa #{{ task.id }}</span>
            </div>
            <h2 id="task-details-title" class="break-words text-lg font-bold leading-6 text-gray-900">
              {{ task.title }}
            </h2>
          </div>
          <Button variant="ghost" size="icon" class="h-8 w-8 shrink-0 text-gray-400" title="Fechar" @click="$emit('close')">
            <X class="h-4 w-4" />
          </Button>
        </header>

        <div class="max-h-[calc(90vh-150px)] overflow-y-auto px-6 py-5">
          <div class="flex items-center gap-2 text-xs font-semibold uppercase text-gray-400">
            <FileText class="h-4 w-4" />
            Descrição
          </div>
          <p v-if="task.description" class="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">
            {{ task.description }}
          </p>
          <p v-else class="mt-2 text-sm text-gray-400">Nenhuma descrição informada.</p>

          <div class="mt-6 grid gap-x-6 gap-y-5 border-t border-gray-100 pt-5 sm:grid-cols-2">
            <div class="flex min-w-0 gap-3">
              <CalendarDays class="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
              <div class="min-w-0">
                <p class="text-xs font-medium text-gray-400">Data</p>
                <p class="mt-0.5 break-words text-sm font-medium capitalize text-gray-700">
                  {{ formatDate(task.dateExpires, { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }) }}
                </p>
              </div>
            </div>

            <div class="flex min-w-0 gap-3">
              <Clock3 class="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
              <div>
                <p class="text-xs font-medium text-gray-400">Horário</p>
                <p class="mt-0.5 text-sm font-medium text-gray-700">{{ formatTime(task.timeExpires) || 'Não informado' }}</p>
              </div>
            </div>

            <div class="flex min-w-0 gap-3">
              <UserRound class="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
              <div class="min-w-0">
                <p class="text-xs font-medium text-gray-400">Cliente</p>
                <p class="mt-0.5 break-words text-sm font-medium text-gray-700">{{ task.clientName || 'Sem cliente' }}</p>
              </div>
            </div>

            <div class="flex min-w-0 gap-3">
              <Tag class="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
              <div>
                <p class="text-xs font-medium text-gray-400">Tipo</p>
                <p class="mt-0.5 text-sm font-medium text-gray-700">{{ typeLabels[task.type] }}</p>
              </div>
            </div>

            <div class="flex min-w-0 gap-3">
              <Flag class="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
              <div>
                <p class="text-xs font-medium text-gray-400">Prioridade</p>
                <p class="mt-0.5 text-sm font-medium text-gray-700">{{ priorityLabels[task.priority] }}</p>
              </div>
            </div>

            <div class="flex min-w-0 gap-3">
              <MessageCircle v-if="task.source === 'WHATSAPP'" class="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
              <ListChecks v-else class="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
              <div>
                <p class="text-xs font-medium text-gray-400">Origem</p>
                <p class="mt-0.5 text-sm font-medium text-gray-700">{{ task.source === 'WHATSAPP' ? 'WhatsApp' : 'Cadastro manual' }}</p>
              </div>
            </div>
          </div>
        </div>

        <footer class="flex justify-end border-t border-gray-100 px-6 py-4">
          <Button variant="outline" @click="$emit('close')">Fechar</Button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
