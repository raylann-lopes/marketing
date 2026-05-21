<script setup lang="ts">
import { Clock3 } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'

type UploadStatus = 'PLANNED' | 'UPLOADED' | 'APPROVED'

interface ClientUploadItem {
  id: string
  date: string // YYYY-MM-DD
  title: string
  format: string
  channel: string
  notes: string
  status: UploadStatus
}

interface Props {
  isOpen: boolean
  isSavingUpload: boolean
  uploadDraft: ClientUploadItem
  uploadFieldErrors: Record<string, string>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saveUpload'): void
  (e: 'update:uploadDraft', val: ClientUploadItem): void
}>()

function updateDraft(key: keyof ClientUploadItem, value: string) {
  emit('update:uploadDraft', { ...props.uploadDraft, [key]: value })
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
    <div class="w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-gray-100 p-5">
        <h3 class="text-lg font-bold text-gray-900">Agendar Post</h3>
        <button class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100" @click="$emit('close')">×</button>
      </div>

      <div class="space-y-4 p-5">
        <div class="space-y-1.5">
          <label class="text-xs font-semibold uppercase text-gray-500">Data</label>
          <input
            :value="uploadDraft.date"
            @input="updateDraft('date', ($event.target as HTMLInputElement).value)"
            type="date"
            :class="['w-full rounded-lg border bg-gray-50 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20', uploadFieldErrors.date ? 'border-red-500' : 'border-gray-200']"
          />
          <p v-if="uploadFieldErrors.date" class="text-[10px] font-medium text-red-500">{{ uploadFieldErrors.date }}</p>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold uppercase text-gray-500">Título do Post</label>
          <input
            :value="uploadDraft.title"
            @input="updateDraft('title', ($event.target as HTMLInputElement).value)"
            type="text"
            :class="['w-full rounded-lg border bg-gray-50 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20', uploadFieldErrors.title ? 'border-red-500' : 'border-gray-200']"
            placeholder="Ex: Reels - oferta da semana"
          />
          <p v-if="uploadFieldErrors.title" class="text-[10px] font-medium text-red-500">{{ uploadFieldErrors.title }}</p>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Formato</label>
            <select
              :value="uploadDraft.format"
              @change="updateDraft('format', ($event.target as HTMLSelectElement).value)"
              class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option>Feed</option>
              <option>Reels</option>
              <option>Story</option>
              <option>Carrossel</option>
              <option>Vídeo Curto</option>
            </select>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Canal</label>
            <input
              :value="uploadDraft.channel"
              @input="updateDraft('channel', ($event.target as HTMLInputElement).value)"
              type="text"
              :class="['w-full rounded-lg border bg-gray-50 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20', uploadFieldErrors.channel ? 'border-red-500' : 'border-gray-200']"
              placeholder="Instagram"
            />
            <p v-if="uploadFieldErrors.channel" class="text-[10px] font-medium text-red-500">{{ uploadFieldErrors.channel }}</p>
          </div>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold uppercase text-gray-500">Status inicial</label>
          <select
            :value="uploadDraft.status"
            @change="updateDraft('status', ($event.target as HTMLSelectElement).value)"
            class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option value="PLANNED">Planejado</option>
            <option value="UPLOADED">Enviado</option>
            <option value="APPROVED">Aprovado</option>
          </select>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold uppercase text-gray-500">Observações</label>
          <textarea
            :value="uploadDraft.notes"
            @input="updateDraft('notes', ($event.target as HTMLTextAreaElement).value)"
            rows="3"
            class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            placeholder="Briefing curto, CTA ou observações para equipe."
          />
        </div>
      </div>

      <div class="flex gap-3 border-t border-gray-100 bg-gray-50 p-5">
        <Button variant="outline" class="flex-1" @click="$emit('close')">Cancelar</Button>
        <Button class="flex-1 gap-2" :disabled="isSavingUpload" @click="$emit('saveUpload')">
          <Clock3 class="h-4 w-4" />
          {{ isSavingUpload ? 'Salvando...' : 'Salvar Agendamento' }}
        </Button>
      </div>
    </div>
  </div>
</template>
