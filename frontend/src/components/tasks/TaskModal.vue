<script setup lang="ts">
import { computed } from 'vue'
import { Loader2, Plus, X } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import type { Client } from '@/services/clientService'
import type { TaskPriority, TaskType } from '@/services/taskService'

export type TaskForm = {
  title: string
  description: string
  clientId: string
  clientName: string
  dateExpires: string
  timeExpires: string
  type: TaskType
  priority: TaskPriority
}

const props = defineProps<{
  isOpen: boolean
  form: TaskForm
  clients: Client[]
  isSubmitting: boolean
  loadingClients: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save'): void
  (e: 'update:form', value: TaskForm): void
}>()

const taskTypeOptions: Array<{ value: TaskType; label: string }> = [
  { value: 'TAREFA', label: 'Tarefa' },
  { value: 'REUNIAO', label: 'Reunião' },
  { value: 'LEMBRETE', label: 'Lembrete' },
  { value: 'COBRANCA', label: 'Cobrança' },
  { value: 'PRAZO', label: 'Prazo' },
  { value: 'FOLLOW_UP', label: 'Follow-up' },
]

const priorityOptions: Array<{ value: TaskPriority; label: string }> = [
  { value: 'LOW', label: 'Baixa' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'URGENT', label: 'Urgente' },
]

const sortedClients = computed(() => [...props.clients].sort((a, b) => a.name.localeCompare(b.name)))

function updateField<K extends keyof TaskForm>(field: K, value: TaskForm[K]) {
  emit('update:form', { ...props.form, [field]: value })
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      @click.self="$emit('close')"
    >
      <div class="max-h-[92vh] w-full max-w-xl overflow-hidden rounded-lg bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-gray-100 px-6 py-5">
          <div>
            <h2 class="text-lg font-bold text-gray-900">Nova tarefa</h2>
            <p class="text-sm text-gray-500">Registre um compromisso da sua agenda operacional.</p>
          </div>
          <button type="button" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="$emit('close')">
            <X class="h-5 w-5" />
          </button>
        </div>

        <form class="max-h-[calc(92vh-86px)] overflow-y-auto p-6" @submit.prevent="$emit('save')">
          <div class="space-y-4">
            <label class="block">
              <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Título</span>
              <input
                :value="form.title"
                maxlength="60"
                class="h-10 w-full rounded-md border border-gray-200 px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                placeholder="Cobrar fotos do cliente"
                @input="updateField('title', ($event.target as HTMLInputElement).value)"
              >
            </label>

            <label class="block">
              <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Descrição</span>
              <textarea
                :value="form.description"
                rows="4"
                maxlength="500"
                class="w-full resize-none rounded-md border border-gray-200 px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                placeholder="Detalhe o que precisa ser feito"
                @input="updateField('description', ($event.target as HTMLTextAreaElement).value)"
              />
            </label>

            <label class="block">
              <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Cliente</span>
              <select
                :value="form.clientId"
                :disabled="loadingClients"
                class="h-10 w-full rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10 disabled:bg-gray-50"
                @change="updateField('clientId', ($event.target as HTMLSelectElement).value)"
              >
                <option value="">Sem cliente vinculado</option>
                <option v-for="client in sortedClients" :key="client.id" :value="String(client.id)">
                  {{ client.name }}
                </option>
              </select>
            </label>

            <label v-if="!form.clientId" class="block">
              <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Nome do cliente</span>
              <input
                :value="form.clientName"
                class="h-10 w-full rounded-md border border-gray-200 px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                placeholder="Nome livre, se existir"
                @input="updateField('clientName', ($event.target as HTMLInputElement).value)"
              >
            </label>

            <div class="grid gap-3 sm:grid-cols-2">
              <label class="block">
                <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Data</span>
                <input
                  :value="form.dateExpires"
                  type="date"
                  class="h-10 w-full rounded-md border border-gray-200 px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                  @input="updateField('dateExpires', ($event.target as HTMLInputElement).value)"
                >
              </label>

              <label class="block">
                <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Hora</span>
                <input
                  :value="form.timeExpires"
                  type="time"
                  class="h-10 w-full rounded-md border border-gray-200 px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                  @input="updateField('timeExpires', ($event.target as HTMLInputElement).value)"
                >
              </label>
            </div>

            <div class="grid gap-3 sm:grid-cols-2">
              <label class="block">
                <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Tipo</span>
                <select
                  :value="form.type"
                  class="h-10 w-full rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                  @change="updateField('type', ($event.target as HTMLSelectElement).value as TaskType)"
                >
                  <option v-for="option in taskTypeOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label class="block">
                <span class="mb-1.5 block text-xs font-semibold uppercase text-gray-500">Prioridade</span>
                <select
                  :value="form.priority"
                  class="h-10 w-full rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                  @change="updateField('priority', ($event.target as HTMLSelectElement).value as TaskPriority)"
                >
                  <option v-for="option in priorityOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>
          </div>

          <div class="mt-6 flex justify-end gap-2 border-t border-gray-100 pt-5">
            <Button type="button" variant="outline" @click="$emit('close')">Cancelar</Button>
            <Button type="submit" :disabled="isSubmitting">
              <Loader2 v-if="isSubmitting" class="h-4 w-4 animate-spin" />
              <Plus v-else class="h-4 w-4" />
              Criar tarefa
            </Button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
