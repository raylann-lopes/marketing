<script setup lang="ts">
import { ref, watch } from 'vue'
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

watch(() => props.isOpen, (newVal) => {
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
        scheduledAt: new Date().toISOString().slice(0, 16),
        isUrgent: false
      }
    }
  }
}, { immediate: true })

function handleSave() {
  emit('save', { ...form.value })
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="flex items-center justify-between p-6 border-b border-gray-100">
        <h2 class="text-xl font-bold text-gray-900">{{ postToEdit ? 'Editar Demanda' : 'Nova Demanda' }}</h2>
        <button @click="$emit('close')" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
      </div>
      <div class="p-6 space-y-4">
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
          <div class="relative">
            <select v-model="form.clientId" :class="['w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer', fieldErrors.clientId ? 'border-red-500' : 'border-gray-200']">
              <option value="">Selecione o cliente</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
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
      <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
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
  </div>
</template>
