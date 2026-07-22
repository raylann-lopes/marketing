<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { X, ChevronDown, Trash2 } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { type Post, type PostFormData } from '@/services/postService'
import { type Client } from '@/services/clientService'

interface Props {
  isOpen: boolean
  postToEdit: Post | null
  clients: Client[]
  isSubmitting: boolean
  fieldErrors: Record<string, string>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', data: PostFormData): void
  (e: 'delete', post: Post): void
}>()

const form = ref({
  clientId: '',
  title: '',
  theme: '',
  objective: '',
  status: 'DEMAND',
  scheduledAt: '',
  isUrgent: false
})

const isClientDropdownOpen = ref(false)
const clientButtonRef = ref<HTMLElement | null>(null)
const clientDropdownPosition = ref({ top: 0, left: 0, width: 0 })

const selectedClientName = computed(() =>
  props.clients.find(c => String(c.id) === form.value.clientId)?.name
)

function toggleClientDropdown() {
  if (isClientDropdownOpen.value) {
    isClientDropdownOpen.value = false
    return
  }
  const rect = clientButtonRef.value!.getBoundingClientRect()
  clientDropdownPosition.value = { top: rect.bottom + 4, left: rect.left, width: rect.width }
  isClientDropdownOpen.value = true
}

function selectClient(id: string | number) {
  form.value.clientId = String(id)
  isClientDropdownOpen.value = false
}

function handleClientDropdownOutsideClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.client-dropdown')) isClientDropdownOpen.value = false
}

onMounted(() => document.addEventListener('click', handleClientDropdownOutsideClick))
onUnmounted(() => document.removeEventListener('click', handleClientDropdownOutsideClick))

watch(() => props.isOpen, (newVal) => {
  isClientDropdownOpen.value = false
  if (newVal) {
    if (props.postToEdit) {
      const p = props.postToEdit
      const cid = (p as Post & { client?: { id?: string | number } }).client?.id || p.clientId
      form.value = {
        clientId: String(cid || ''),
        title: p.title,
        theme: p.theme,
        objective: p.objective,
        status: p.status,
        scheduledAt: p.scheduledAt.substring(0, 16),
        isUrgent: p.isUrgent || false
      }
    } else {
      form.value = {
        clientId: '',
        title: '',
        theme: '',
        objective: '',
        status: 'DEMAND',
        scheduledAt: toLocalDateTimeInput(new Date()),
        isUrgent: false
      }
    }
  }
}, { immediate: true })

function toLocalDateTimeInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function handleSave() {
  emit('save', { ...form.value })
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="shrink-0 flex items-center justify-between p-6 border-b border-gray-100">
        <h2 class="text-xl font-bold text-gray-900">{{ postToEdit ? 'Editar Demanda' : 'Nova Demanda' }}</h2>
        <button @click="$emit('close')" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
      </div>
      <div class="p-6 space-y-4 overflow-y-auto flex-1 min-h-0">
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
          <div class="relative client-dropdown">
            <button
              type="button"
              ref="clientButtonRef"
              @click.stop="toggleClientDropdown"
              :class="['w-full flex items-center justify-between gap-2 p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm text-left', fieldErrors.clientId ? 'border-red-500' : 'border-gray-200']"
            >
              <span :class="selectedClientName ? 'text-gray-800' : 'text-gray-400'">{{ selectedClientName || 'Selecione o cliente' }}</span>
            </button>
            <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
          </div>
          <p v-if="fieldErrors.clientId" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.clientId }}</p>
        </div>
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Título da Demanda</label>
          <input v-model="form.title" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.title ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Campanha de Black Friday" />
          <p v-if="fieldErrors.title" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.title }}</p>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Tema</label>
            <input v-model="form.theme" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.theme ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Vendas / Social Media" />
            <p v-if="fieldErrors.theme" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.theme }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Prazo / Entrega</label>
            <input v-model="form.scheduledAt" type="datetime-local" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.scheduledAt ? 'border-red-500' : 'border-gray-200']" />
            <p v-if="fieldErrors.scheduledAt" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.scheduledAt }}</p>
          </div>
        </div>

        <div class="flex items-center gap-2 px-1 py-1">
          <input id="isUrgent" v-model="form.isUrgent" type="checkbox" class="w-4 h-4 text-primary bg-gray-100 border-gray-300 rounded focus:ring-primary" />
          <label for="isUrgent" class="text-xs font-bold text-red-500 uppercase cursor-pointer">Marcar como URGENTE</label>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Objetivo da Demanda</label>
          <textarea v-model="form.objective" rows="3" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.objective ? 'border-red-500' : 'border-gray-200']" placeholder="O que precisamos entregar neste projeto?"></textarea>
          <p v-if="fieldErrors.objective" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.objective }}</p>
        </div>
      </div>
      <div class="shrink-0 p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
        <Button v-if="postToEdit" variant="destructive" class="px-4" @click="$emit('delete', postToEdit!)">
          <Trash2 class="w-4 h-4" />
        </Button>
        <div class="flex-1" />
        <Button variant="outline" class="w-32" @click="$emit('close')">Cancelar</Button>
        <Button class="min-w-[140px]" :disabled="isSubmitting" @click="handleSave">
          {{ isSubmitting ? 'Salvando...' : (postToEdit ? 'Atualizar Demanda' : 'Criar Demanda') }}
        </Button>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="isClientDropdownOpen"
        class="client-dropdown fixed z-[60] rounded-lg border border-gray-200 bg-white shadow-xl py-1 max-h-56 overflow-y-auto"
        :style="{
          top: clientDropdownPosition.top + 'px',
          left: clientDropdownPosition.left + 'px',
          width: clientDropdownPosition.width + 'px',
        }"
      >
        <button
          v-for="c in clients"
          :key="c.id"
          type="button"
          @click.stop="selectClient(c.id!)"
          :class="[
            'w-full text-left px-3 py-1.5 text-sm hover:bg-gray-50',
            form.clientId === String(c.id) ? 'font-bold text-primary' : 'text-gray-700',
          ]"
        >
          {{ c.name }}
        </button>
      </div>
    </Teleport>
  </div>
</template>
