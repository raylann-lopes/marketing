<script setup lang="ts">
import { ref } from 'vue'
import { X, Eye, XCircle, Send } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { type Post } from '@/services/postService'
import { type PostApproval } from '@/services/approvalService'

type ArtPreviewMode = 'preview' | 'internal-review'

interface Props {
  isOpen: boolean
  mode: ArtPreviewMode
  post: Post | null
  approval: PostApproval | null
  artUrl: string
  isLoading: boolean
  internalReviewAction: 'send' | 'reject' | null
  rejectionReason: string
  rejectionReasonError: string
  isRejectReasonModalOpen: boolean
  clientName: string
  scheduledAt: string
  internalRevisionNotes: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'sendToClient'): void
  (e: 'openRejectModal'): void
  (e: 'closeRejectModal'): void
  (e: 'confirmRejection'): void
  (e: 'update:rejectionReason', val: string): void
  (e: 'update:scheduledAt', val: string): void
  (e: 'update:internalRevisionNotes', val: string): void
}>()

function isVideo(url: string, filename?: string) {
  const check = (str: string) => {
    if (!str) return false
    const clean = (str.split('?')[0] ?? '').toLowerCase()
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mov')
  }
  return check(url) || (filename ? check(filename) : false)
}

function formatSentAt(sentAt?: string) {
  if (!sentAt) return ''
  const date = new Date(sentAt)
  if (Number.isNaN(date.getTime())) return sentAt
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/55 backdrop-blur-sm">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-5xl overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="flex items-center justify-between p-5 border-b border-gray-100 bg-gray-50/70">
        <div class="min-w-0">
          <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">
            {{ mode === 'internal-review' ? 'Aprovação interna' : 'Imagem do post' }}
          </p>
          <h2 class="mt-1 text-lg font-bold text-gray-900 truncate">{{ post?.title }}</h2>
        </div>
        <button @click="$emit('close')" class="p-2 hover:bg-white rounded-xl text-gray-400 transition-colors shadow-sm">
          <X class="w-5 h-5" />
        </button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-[1.2fr_0.8fr] gap-0">
        <div class="bg-gray-950 min-h-[420px] flex items-center justify-center">
          <div v-if="isLoading" class="flex flex-col items-center gap-3 text-white/70">
            <div class="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin"></div>
            <span class="text-xs font-bold uppercase tracking-widest">Carregando imagem</span>
          </div>
          <template v-else-if="artUrl">
            <video
              v-if="isVideo(artUrl, approval?.artName)"
              :src="artUrl"
              class="max-h-[70vh] w-full object-contain"
              controls
            ></video>
            <img
              v-else
              :src="artUrl"
              class="max-h-[70vh] w-full object-contain"
              alt="Imagem adicionada ao post"
            />
          </template>
          <div v-else class="text-center text-white/60 px-8">
            <Eye class="w-8 h-8 mx-auto mb-3" />
            <p class="text-sm font-semibold">Imagem indisponível</p>
          </div>
        </div>

        <div class="p-6 flex flex-col gap-5">
          <div>
            <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">Cliente</p>
            <p class="mt-1 text-sm font-semibold text-gray-800">{{ clientName }}</p>
          </div>

          <div>
            <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">Legenda</p>
            <p class="mt-2 max-h-40 overflow-y-auto whitespace-pre-line rounded-xl border border-gray-100 bg-gray-50 p-3 text-sm leading-relaxed text-gray-700">
              {{ approval?.caption || 'Sem legenda cadastrada.' }}
            </p>
          </div>

          <div v-if="mode === 'internal-review'" class="space-y-4 py-2 border-y border-gray-100 my-2">
            <div>
              <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">Programar Postagem (Obrigatório)</p>
              <input
                type="datetime-local"
                :value="scheduledAt"
                @input="$emit('update:scheduledAt', ($event.target as HTMLInputElement).value)"
                class="mt-1 w-full rounded-xl border border-gray-200 bg-white p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/10 focus:border-primary/30 shadow-sm"
              />
            </div>
            <div>
              <p class="text-[10px] font-bold uppercase tracking-widest text-gray-400">Notas de Revisão Interna</p>
              <textarea
                :value="internalRevisionNotes"
                @input="$emit('update:internalRevisionNotes', ($event.target as HTMLTextAreaElement).value)"
                rows="2"
                class="mt-1 w-full rounded-xl border border-gray-200 bg-white p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/10 focus:border-primary/30 shadow-sm resize-none"
                placeholder="Observações para o designer/copy..."
              ></textarea>
            </div>
          </div>

          <div v-if="approval?.whatsappSentAt" class="rounded-xl border border-emerald-100 bg-emerald-50 p-3">
            <p class="text-[10px] font-bold uppercase tracking-widest text-emerald-700">Envio ao cliente</p>
            <p class="mt-1 text-xs font-medium text-emerald-700">
              Enviado em {{ formatSentAt(approval.whatsappSentAt) }}
            </p>
          </div>

          <div v-if="mode === 'internal-review'" class="mt-auto grid grid-cols-2 gap-3 border-t border-gray-100 pt-5">
            <Button
              variant="destructive"
              class="h-11 rounded-xl font-bold"
              :disabled="Boolean(internalReviewAction)"
              @click="$emit('openRejectModal')"
            >
              <XCircle class="w-4 h-4" />
              Rejeitar
            </Button>
            <Button
              class="h-11 rounded-xl font-bold"
              :disabled="Boolean(internalReviewAction)"
              @click="$emit('sendToClient')"
            >
              <template v-if="internalReviewAction === 'send'">Enviando...</template>
              <template v-else>
                <Send class="w-4 h-4" />
                Enviar ao cliente
              </template>
            </Button>
          </div>
        </div>
      </div>
    </div>

    <!-- Reject Reason Modal (Nested or separate? Let's keep it here for simplicity of state or emit) -->
    <div v-if="isRejectReasonModalOpen" class="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/45 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between p-5 border-b border-gray-100">
          <div>
            <p class="text-[10px] font-bold uppercase tracking-widest text-red-500">Rejeição interna</p>
            <h3 class="mt-1 text-lg font-bold text-gray-900">Justificar rejeição</h3>
          </div>
          <button @click="$emit('closeRejectModal')" class="p-2 hover:bg-gray-100 rounded-xl text-gray-400 transition-colors">
            <X class="w-5 h-5" />
          </button>
        </div>
        <div class="p-5 space-y-2">
          <textarea
            :value="rejectionReason"
            @input="$emit('update:rejectionReason', ($event.target as HTMLTextAreaElement).value)"
            rows="5"
            class="w-full rounded-xl border border-gray-200 bg-gray-50 p-3 text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-red-100 focus:border-red-300 resize-none"
            placeholder="Informe o motivo da rejeição..."
          ></textarea>
          <p v-if="rejectionReasonError" class="text-xs font-medium text-red-600">{{ rejectionReasonError }}</p>
        </div>
        <div class="p-5 bg-gray-50 border-t border-gray-100 flex gap-3">
          <Button variant="outline" class="flex-1" :disabled="internalReviewAction === 'reject'" @click="$emit('closeRejectModal')">Cancelar</Button>
          <Button variant="destructive" class="flex-1" :disabled="internalReviewAction === 'reject'" @click="$emit('confirmRejection')">
            {{ internalReviewAction === 'reject' ? 'Rejeitando...' : 'Rejeitar' }}
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
