<script setup lang="ts">
import { X } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { type Client } from '@/services/clientService'

interface Props {
  isOpen: boolean
  isEdit: boolean
  client: Partial<Client>
  isSubmitting: boolean
  fieldErrors: Record<string, string>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save'): void
  (e: 'update:client', val: Partial<Client>): void
}>()

function formatPhone(value: string) {
  let d = value.replace(/\D/g, '')
  if (d.length > 0 && !d.startsWith('55')) {
    d = '55' + d
  }
  if (d.length === 0) return ''
  if (d.length <= 2) return `+${d}`
  if (d.length <= 4) return `+${d.substring(0, 2)} (${d.substring(2, 4)}`
  if (d.length <= 9) return `+${d.substring(0, 2)} (${d.substring(2, 4)}) ${d.substring(4)}`
  return `+${d.substring(0, 2)} (${d.substring(2, 4)}) ${d.substring(4, 9)}-${d.substring(9, 13)}`
}

function updateField(key: keyof Client, value: string) {
  emit('update:client', { ...props.client, [key]: value })
}

function handlePhoneInput(e: Event) {
  const input = e.target as HTMLInputElement
  updateField('number', formatPhone(input.value))
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="flex items-center justify-between p-6 border-b border-gray-100">
        <h2 class="text-xl font-bold text-gray-900">{{ isEdit ? 'Editar Cliente' : 'Cadastrar Novo Cliente' }}</h2>
        <button @click="$emit('close')" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
      </div>

      <div class="p-6 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Nome do Cliente</label>
            <input
              :value="client.name"
              @input="updateField('name', ($event.target as HTMLInputElement).value)"
              type="text"
              :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.name ? 'border-red-500' : 'border-gray-200']"
              placeholder="Ex: Raylan Lopes"
            />
            <p v-if="fieldErrors.name" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.name }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">E-mail</label>
            <input
              :value="client.email"
              @input="updateField('email', ($event.target as HTMLInputElement).value)"
              type="email"
              :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.email ? 'border-red-500' : 'border-gray-200']"
              placeholder="cliente@email.com"
            />
            <p v-if="fieldErrors.email" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.email }}</p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Telefone (WhatsApp)</label>
            <input
              :value="client.number"
              @input="handlePhoneInput"
              type="text"
              :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.number ? 'border-red-500' : 'border-gray-200']"
              placeholder="+55 (33) 99999-9999"
            />
            <p v-if="fieldErrors.number" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.number }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Nicho</label>
            <input
              :value="client.niche"
              @input="updateField('niche', ($event.target as HTMLInputElement).value)"
              type="text"
              :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.niche ? 'border-red-500' : 'border-gray-200']"
              placeholder="Ex: Imobiliária"
            />
            <p v-if="fieldErrors.niche" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.niche }}</p>
          </div>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Link do Google Drive</label>
          <input
            :value="client.driveLink"
            @input="updateField('driveLink', ($event.target as HTMLInputElement).value)"
            type="url"
            :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.driveLink ? 'border-red-500' : 'border-gray-200']"
            placeholder="https://drive.google.com/..."
          />
          <p v-if="fieldErrors.driveLink" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.driveLink }}</p>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Tom de Voz / Identidade</label>
          <textarea
            :value="client.voiceTone"
            @input="updateField('voiceTone', ($event.target as HTMLTextAreaElement).value)"
            rows="3"
            :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.voiceTone ? 'border-red-500' : 'border-gray-200']"
            placeholder="Descreva como a marca deve se comunicar..."
          />
          <p v-if="fieldErrors.voiceTone" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.voiceTone }}</p>
        </div>
      </div>

      <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
        <Button variant="outline" class="flex-1" @click="$emit('close')">Cancelar</Button>
        <Button class="flex-1" :disabled="isSubmitting" @click="$emit('save')">
          {{ isSubmitting ? 'Salvando...' : (isEdit ? 'Atualizar Cliente' : 'Salvar Cliente') }}
        </Button>
      </div>
    </div>
  </div>
</template>
