<script setup lang="ts">
import { X, Eye, XCircle, Send, MessageSquare, Check, Maximize } from 'lucide-vue-next'
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
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
  >
    <div
      class="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-4xl overflow-hidden animate-in fade-in zoom-in duration-300"
    >
      <!-- Header -->
      <div class="flex items-center justify-between p-6 border-b border-gray-100 bg-gray-50/50">
        <div class="flex items-center gap-3">
          <div
            :class="[
              'w-10 h-10 rounded-xl flex items-center justify-center shadow-sm',
              mode === 'internal-review'
                ? 'bg-indigo-100 text-indigo-600'
                : 'bg-primary/10 text-primary',
            ]"
          >
            <Eye v-if="mode === 'preview'" class="w-5 h-5" />
            <Check v-else class="w-5 h-5" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-gray-900">
              {{
                mode === 'internal-review'
                  ? 'Aprovação Interna'
                  : approval?.artName === 'Referencia.jpg'
                    ? 'Ver Referência'
                    : 'Visualizar Post'
              }}
            </h2>
            <p class="text-xs text-gray-500 font-medium uppercase tracking-wider">
              Demanda: {{ post?.title }}
            </p>
          </div>
        </div>
        <button
          @click="$emit('close')"
          class="p-2 hover:bg-white rounded-xl text-gray-400 transition-colors shadow-sm"
        >
          <X class="w-5 h-5" />
        </button>
      </div>

      <div class="p-8 grid grid-cols-1 md:grid-cols-2 gap-8">
        <!-- Coluna 1: Visualização da Mídia -->
        <div class="space-y-4">
          <label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest"
            >Mídia do Conteúdo</label
          >
          <div
            class="relative aspect-square rounded-[2rem] border border-gray-100 bg-gray-950 flex items-center justify-center overflow-hidden shadow-2xl group"
          >
            <div v-if="isLoading" class="flex flex-col items-center gap-2 text-white/50">
              <div
                class="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin"
              ></div>
              <span class="text-[10px] font-bold uppercase tracking-widest">Carregando...</span>
            </div>

            <template v-else-if="artUrl">
              <video
                v-if="isVideo(artUrl, approval?.artName)"
                :src="artUrl"
                class="absolute inset-0 w-full h-full object-contain animate-in fade-in duration-500 shadow-inner"
                controls
                autoplay
              ></video>
              <img
                v-else
                :src="artUrl"
                class="absolute inset-0 w-full h-full object-contain animate-in fade-in duration-500 shadow-inner"
                alt="Mídia"
              />

              <!-- Botão Tela Cheia -->
              <a
                :href="artUrl"
                target="_blank"
                class="absolute top-4 right-4 p-2.5 rounded-xl bg-black/40 hover:bg-black/60 text-white backdrop-blur-md transition-all opacity-0 group-hover:opacity-100 shadow-lg border border-white/10"
                title="Abrir em nova aba"
              >
                <Maximize class="w-4 h-4" />
              </a>
            </template>

            <div v-else class="text-center text-gray-400">
              <Eye class="w-10 h-10 mx-auto mb-2 opacity-20" />
              <p class="text-xs font-bold uppercase tracking-widest">Indisponível</p>
            </div>
          </div>
        </div>

        <!-- Coluna 2: Informações -->
        <div class="space-y-5 flex flex-col">
          <div class="flex items-center justify-between px-1">
            <div class="flex flex-col">
              <span class="text-[9px] font-bold text-gray-400 uppercase tracking-widest"
                >Cliente</span
              >
              <span class="text-sm font-bold text-gray-700">{{ clientName }}</span>
            </div>
            <div v-if="approval?.status" class="flex flex-col items-end">
              <span class="text-[9px] font-bold text-gray-400 uppercase tracking-widest"
                >Status</span
              >
              <span
                :class="[
                  'text-[10px] font-bold px-2 py-0.5 rounded-lg border',
                  approval.status === 'APPROVE'
                    ? 'bg-emerald-50 border-emerald-100 text-emerald-600'
                    : 'bg-amber-50 border-amber-100 text-amber-600',
                ]"
              >
                {{ approval.status === 'APPROVE' ? 'APROVADO' : 'PENDENTE' }}
              </span>
            </div>
          </div>

          <!-- Legenda -->
          <div class="flex-1 space-y-2">
            <div class="flex items-center gap-2 text-gray-400 px-1">
              <MessageSquare class="w-3.5 h-3.5" />
              <label class="text-[10px] font-bold uppercase tracking-widest">Legenda do Post</label>
            </div>
            <div
              class="w-full p-5 rounded-[1.5rem] border border-gray-100 bg-gray-50/50 text-sm leading-relaxed text-gray-600 max-h-[220px] overflow-y-auto whitespace-pre-line shadow-inner"
            >
              {{ approval?.caption || 'Sem legenda cadastrada.' }}
            </div>
          </div>

          <!-- Agendamento e Notas -->
          <div
            v-if="mode === 'internal-review'"
            class="space-y-4 animate-in slide-in-from-bottom-2 duration-300"
          >
            <div class="grid grid-cols-1 gap-4">
              <div>
                <label
                  class="text-[10px] font-bold text-indigo-500 uppercase tracking-widest block mb-1.5 ml-1"
                  >Data de Postagem</label
                >
                <input
                  type="datetime-local"
                  :value="scheduledAt"
                  @input="$emit('update:scheduledAt', ($event.target as HTMLInputElement).value)"
                  class="w-full h-11 px-4 rounded-xl border border-gray-200 bg-white text-sm font-medium focus:outline-none focus:ring-4 focus:ring-indigo-50 focus:border-indigo-300 transition-all shadow-sm"
                />
              </div>
              <div>
                <label
                  class="text-[10px] font-bold text-gray-400 uppercase tracking-widest block mb-1.5 ml-1"
                  >Notas de Revisão</label
                >
                <textarea
                  :value="internalRevisionNotes"
                  @input="
                    $emit(
                      'update:internalRevisionNotes',
                      ($event.target as HTMLTextAreaElement).value,
                    )
                  "
                  rows="2"
                  class="w-full p-3 rounded-xl border border-gray-200 bg-white text-sm focus:outline-none focus:ring-4 focus:ring-primary/5 focus:border-primary/20 transition-all shadow-sm resize-none"
                  placeholder="Observações operacionais..."
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Histórico de Envio -->
          <div
            v-if="approval?.whatsappSentAt"
            class="p-3 rounded-xl border border-emerald-100 bg-emerald-50/30 flex items-center gap-3"
          >
            <div
              class="w-7 h-7 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-600"
            >
              <Send class="w-3.5 h-3.5" />
            </div>
            <span class="text-[10px] font-bold text-emerald-700"
              >Enviado ao cliente em {{ formatSentAt(approval.whatsappSentAt) }}</span
            >
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-4">
        <Button
          variant="outline"
          class="flex-1 h-12 rounded-xl font-bold border-gray-200"
          @click="$emit('close')"
        >
          {{ mode === 'internal-review' ? 'CANCELAR' : 'FECHAR' }}
        </Button>

        <template v-if="mode === 'internal-review'">
          <Button
            variant="outline"
            class="flex-1 h-12 rounded-xl font-bold border-red-200 text-red-500 hover:bg-red-50"
            :disabled="Boolean(internalReviewAction)"
            @click="$emit('openRejectModal')"
          >
            <XCircle class="w-4 h-4 mr-2" /> REJEITAR
          </Button>
          <Button
            class="flex-[1.5] h-12 rounded-xl font-bold shadow-lg shadow-primary/20 gap-2"
            :disabled="Boolean(internalReviewAction) || !scheduledAt"
            @click="$emit('sendToClient')"
          >
            <template v-if="internalReviewAction === 'send'">
              <div
                class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"
              ></div>
              ENVIANDO...
            </template>
            <template v-else> <Send class="w-4 h-4" /> ENVIAR AO CLIENTE </template>
          </Button>
        </template>
      </div>
    </div>

    <!-- Reject Reason Modal -->
    <div
      v-if="isRejectReasonModalOpen"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
    >
      <div
        class="bg-white rounded-[2rem] shadow-2xl w-full max-w-md overflow-hidden animate-in zoom-in duration-200"
      >
        <div class="p-6 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
          <div class="flex items-center gap-3">
            <div
              class="w-10 h-10 rounded-xl bg-red-50 text-red-500 flex items-center justify-center"
            >
              <XCircle class="w-5 h-5" />
            </div>
            <h3 class="text-lg font-bold text-gray-900">Motivo da Rejeição</h3>
          </div>
          <button
            @click="$emit('closeRejectModal')"
            class="p-2 hover:bg-gray-100 rounded-xl text-gray-400"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        <div class="p-7">
          <textarea
            :value="rejectionReason"
            @input="$emit('update:rejectionReason', ($event.target as HTMLTextAreaElement).value)"
            rows="4"
            class="w-full p-4 rounded-2xl border border-gray-100 bg-gray-50 text-sm focus:outline-none focus:ring-4 focus:ring-red-50 focus:border-red-200 transition-all resize-none"
            placeholder="O que precisa ser corrigido?"
          ></textarea>
          <p
            v-if="rejectionReasonError"
            class="mt-2 text-[10px] font-bold text-red-500 uppercase tracking-widest"
          >
            {{ rejectionReasonError }}
          </p>
        </div>
        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
          <Button
            variant="outline"
            class="flex-1 h-12 rounded-xl font-bold"
            @click="$emit('closeRejectModal')"
            >CANCELAR</Button
          >
          <Button
            variant="destructive"
            class="flex-1 h-12 rounded-xl font-bold"
            :disabled="internalReviewAction === 'reject'"
            @click="$emit('confirmRejection')"
          >
            CONFIRMAR
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #f1f5f9;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #e2e8f0;
}
</style>
