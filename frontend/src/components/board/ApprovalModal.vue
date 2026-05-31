<script setup lang="ts">
import { ref } from 'vue'
import { X, Sparkles, Upload, MessageSquare, Check, Maximize } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { type Post } from '@/services/postService'

interface Props {
  isOpen: boolean
  post: Post | null
  existingApprovalId: string | number | null
  data: {
    caption: string
    artS3Key: string
    artName: string
    artPreviewUrl: string
    isUploading: boolean
    isGenerating: boolean
    isSending: boolean
  }
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'fileSelect', event: Event): void
  (e: 'generateCaption'): void
  (e: 'save'): void
  (e: 'update:caption', val: string): void
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)

function isVideo(url: string, filename?: string) {
  const check = (str: string) => {
    if (!str) return false
    const clean = (str.split('?')[0] ?? '').toLowerCase()
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mov')
  }
  return check(url) || (filename ? check(filename) : false)
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden animate-in fade-in zoom-in duration-300">
      <div class="flex items-center justify-between p-6 border-b border-gray-100 bg-gray-50/50">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <Sparkles class="w-5 h-5" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-gray-900">{{ existingApprovalId ? 'Editar Aprovação' : 'Preparar Aprovação' }}</h2>
            <p class="text-xs text-gray-500 font-medium uppercase tracking-wider">Demanda: {{ post?.title }}</p>
          </div>
        </div>
        <button @click="$emit('close')" class="p-2 hover:bg-white rounded-xl text-gray-400 transition-colors shadow-sm"><X class="w-5 h-5" /></button>
      </div>
      <div class="grid grid-cols-2 gap-6 p-6">
        <!-- Coluna 1: Upload da Arte -->
        <div class="space-y-4">
          <label class="text-xs font-bold text-gray-400 uppercase tracking-widest">Arte do Post</label>
          <input ref="fileInputRef" type="file" accept="image/*,video/*" class="hidden" @change="$emit('fileSelect', $event)" />
          <div
            class="relative aspect-square rounded-2xl border border-gray-100 bg-gray-950 flex flex-col items-center justify-center gap-3 group hover:border-primary/40 transition-all cursor-pointer overflow-hidden shadow-2xl"
            @click="fileInputRef?.click()"
          >
            <template v-if="data.artPreviewUrl">
              <video 
                v-if="isVideo(data.artPreviewUrl, data.artName)"
                :src="data.artPreviewUrl" 
                class="absolute inset-0 w-full h-full object-contain animate-in fade-in duration-500 shadow-inner"
                autoplay muted loop
              ></video>
              <img 
                v-else
                :src="data.artPreviewUrl" 
                class="absolute inset-0 w-full h-full object-contain animate-in fade-in duration-500 shadow-inner" 
              />

              <!-- Botão Tela Cheia -->
              <a 
                :href="data.artPreviewUrl" 
                target="_blank" 
                @click.stop
                class="absolute top-4 right-4 p-2 rounded-xl bg-black/40 hover:bg-black/60 text-white backdrop-blur-md transition-all opacity-0 group-hover:opacity-100 shadow-lg border border-white/10 z-10"
              >
                <Maximize class="w-4 h-4" />
              </a>
            </template>
            
            <div v-else-if="data.isUploading" class="flex flex-col items-center gap-2 text-primary">
              <div class="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
              <span class="text-xs font-bold">Enviando...</span>
            </div>
            <div v-else class="flex flex-col items-center gap-2 text-gray-400 group-hover:text-primary transition-colors">
              <div class="w-12 h-12 rounded-full bg-white shadow-sm flex items-center justify-center">
                <Upload class="w-6 h-6" />
              </div>
              <span class="text-xs font-bold">Clique para upload</span>
              <span class="text-[10px] text-gray-300">JPG, PNG, MP4</span>
            </div>
            <div v-if="data.artPreviewUrl" class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Button variant="outline" class="bg-white border-none text-xs">Trocar {{ isVideo(data.artPreviewUrl, data.artName) ? 'Vídeo' : 'Arquivo' }}</Button>
            </div>
          </div>
          <p v-if="data.artName" class="text-[10px] text-gray-400 truncate text-center">{{ data.artName }}</p>
        </div>

        <!-- Coluna 2: Legenda e IA -->
        <div class="space-y-4 flex flex-col">
          <div class="flex items-center justify-between">
            <label class="text-xs font-bold text-gray-400 uppercase tracking-widest">Legenda Sugerida</label>
            <button 
              @click="$emit('generateCaption')"
              :disabled="data.isGenerating"
              class="flex items-center gap-1.5 text-[10px] font-bold text-primary hover:text-primary/80 transition-colors disabled:opacity-50"
            >
              <Sparkles :class="['w-3 h-3', data.isGenerating ? 'animate-pulse' : '']" />
              {{ data.isGenerating ? 'GERANDO...' : 'GERAR COM IA' }}
            </button>
          </div>
          
          <textarea 
            :value="data.caption"
            @input="$emit('update:caption', ($event.target as HTMLTextAreaElement).value)"
            rows="8"
            class="w-full p-4 rounded-2xl border border-gray-100 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm leading-relaxed resize-none flex-1"
            placeholder="Aguardando geração da IA ou digite aqui..."
          ></textarea>
          
          <div class="bg-blue-50/50 p-3 rounded-xl border border-blue-100">
            <div class="flex items-center gap-2 text-blue-600 mb-1">
              <MessageSquare class="w-3.5 h-3.5" />
              <span class="text-[10px] font-bold uppercase tracking-wider">Fluxo N8n</span>
            </div>
            <p class="text-[10px] text-blue-600/70 leading-normal">
              Ao salvar, a demanda vai para Finalizado. Depois, use o botão da coluna Finalizado para enviar ao cliente.
            </p>
          </div>
        </div>
      </div>

      <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-4">
        <Button variant="outline" class="flex-1 h-12 rounded-xl font-bold" @click="$emit('close')">Cancelar</Button>
        <Button
          class="flex-[2] h-12 rounded-xl font-bold gap-2 shadow-lg shadow-primary/20"
          :disabled="data.isSending || data.isUploading || !data.caption || !data.artS3Key"
          @click="$emit('save')"
        >
          <template v-if="data.isSending">
            <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
            SALVANDO...
          </template>
          <template v-else>
            <Check class="w-5 h-5" />
            {{ existingApprovalId ? 'SALVAR ALTERAÇÕES' : 'SALVAR APROVAÇÃO' }}
          </template>
        </Button>
      </div>
    </div>
  </div>
</template>
