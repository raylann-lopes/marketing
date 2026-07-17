<script setup lang="ts">
import { CheckCircle, XCircle, X, ImageOff, Sparkles } from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import { type PostApproval } from '@/services/approvalService'
// Helpers compartilhados — antes duplicados entre card, modal e view
import { getDemandTitle, statusLabel, statusVariant } from '@/lib/approvals'
import { isVideo } from '@/lib/media'

interface Props {
  isOpen: boolean
  approval: PostApproval | null
  artUrl: string
  loadingPreview: boolean
  loadingCaption: boolean
  isAdmin: boolean
}

defineProps<Props>()

defineEmits<{
  (e: 'close'): void
  (e: 'approve'): void
  (e: 'reject'): void
  (e: 'generateCaption'): void
}>()
</script>

<template>
  <div v-if="isOpen && approval" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-3xl shadow-2xl w-full max-w-3xl overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="flex items-center justify-between p-6 border-b border-gray-100">
        <div>
          <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Demanda</p>
          <h2 class="text-xl font-bold text-gray-900">{{ getDemandTitle(approval) }}</h2>
          <Badge :variant="statusVariant(approval.status)" class="mt-1">{{ statusLabel(approval.status) }}</Badge>
        </div>
        <button @click="$emit('close')" class="p-2 hover:bg-gray-100 rounded-xl text-gray-400">
          <X class="w-5 h-5" />
        </button>
      </div>

      <div class="grid grid-cols-2 gap-0">
        <!-- Arte -->
        <div class="border-r border-gray-100 bg-gray-50 flex items-center justify-center min-h-[320px] relative">
          <div v-if="loadingPreview" class="flex flex-col items-center gap-2 text-gray-400">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            <span class="text-xs">Carregando arte...</span>
          </div>
          <video
            v-else-if="artUrl && isVideo(artUrl, approval.artName)"
            :src="artUrl"
            class="max-h-80 max-w-full object-contain rounded-lg"
            controls
            playsinline
          />
          <img
            v-else-if="artUrl"
            :src="artUrl"
            class="max-h-80 max-w-full object-contain rounded-lg"
            alt="Arte do post"
          />
          <div v-else class="flex flex-col items-center gap-2 text-gray-300">
            <ImageOff class="w-12 h-12" />
            <span class="text-xs">Arte indisponível</span>
          </div>
        </div>

        <!-- Detalhes -->
        <div class="p-6 space-y-4 flex flex-col">
          <div v-if="approval.post?.theme || approval.post?.objective">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-1">Detalhes da demanda</p>
            <div class="text-sm text-gray-700 bg-gray-50 rounded-lg p-3 space-y-2">
              <p v-if="approval.post?.theme">
                <span class="font-semibold text-gray-800">Tema:</span> {{ approval.post.theme }}
              </p>
              <p v-if="approval.post?.objective">
                <span class="font-semibold text-gray-800">Objetivo:</span> {{ approval.post.objective }}
              </p>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-1">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Legenda</p>
              <button 
                v-if="isAdmin && approval.status === 'PENDING'"
                @click="$emit('generateCaption')"
                :disabled="loadingCaption"
                class="flex items-center gap-1.5 text-[10px] font-bold text-primary hover:text-primary/80 transition-colors disabled:opacity-50"
              >
                <Sparkles v-if="!loadingCaption" class="w-3 h-3" />
                <div v-else class="w-3 h-3 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
                {{ loadingCaption ? 'Gerando...' : 'GERAR COM IA' }}
              </button>
            </div>
            <p class="text-sm text-gray-700 bg-gray-50 rounded-lg p-3 leading-relaxed whitespace-pre-wrap max-h-40 overflow-y-auto">{{ approval.caption || 'Sem legenda gerada.' }}</p>
          </div>

          <div v-if="approval.approvedUser">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-1">Revisado por</p>
            <p class="text-sm text-gray-700">{{ approval.approvedUser }}</p>
          </div>

          <!-- Admin actions -->
          <div v-if="isAdmin && approval.status === 'PENDING'" class="mt-auto flex gap-3 pt-4 border-t border-gray-100">
            <Button
              class="flex-1 bg-green-600 hover:bg-green-700 gap-2"
              @click="$emit('approve')"
            >
              <CheckCircle class="w-4 h-4" />
              Aprovar Arte
            </Button>
            <Button
              variant="outline"
              class="flex-1 border-red-200 text-red-500 hover:bg-red-50 gap-2"
              @click="$emit('reject')"
            >
              <XCircle class="w-4 h-4" />
              Rejeitar
            </Button>
          </div>

          <div v-else-if="approval.status === 'APPROVE'" class="mt-auto pt-4 border-t border-gray-100">
            <div class="inline-flex w-fit max-w-full items-center gap-2 rounded-lg bg-green-50 px-2.5 py-2 text-green-700">
              <CheckCircle class="h-4 w-4 shrink-0" />
              <span class="text-xs font-medium">Arte aprovada — pronta para publicação</span>
            </div>
          </div>

          <div v-else-if="approval.status === 'REJECTED'" class="mt-auto pt-4 border-t border-gray-100">
            <div class="flex items-center gap-2 bg-red-50 text-red-600 rounded-lg px-3 py-2">
              <XCircle class="w-4 h-4" />
              <span class="text-sm font-medium">Arte rejeitada — aguardando revisão</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
