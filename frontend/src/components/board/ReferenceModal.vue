<script setup lang="ts">
import { ref } from 'vue'
import { X, Image, Upload, Info, Check, Maximize } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { type Post } from '@/services/postService'

interface Props {
  isOpen: boolean
  post: Post | null
  data: {
    referenceS3Key: string
    referenceName: string
    referencePreviewUrl: string
    isUploading: boolean
    isSaving: boolean
  }
}

defineProps<Props>()

defineEmits<{
  (e: 'close'): void
  (e: 'fileSelect', event: Event): void
  (e: 'save'): void
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
    <div class="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-4xl overflow-hidden animate-in fade-in zoom-in duration-300">
      
      <!-- Header -->
      <div class="flex items-center justify-between p-7 border-b border-gray-100 bg-gray-50/50">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center shadow-sm">
            <Image class="w-6 h-6" />
          </div>
          <div>
            <h2 class="text-2xl font-bold text-gray-900">{{ post?.referenceImageS3Key ? 'Alterar Referência' : 'Adicionar Referência' }}</h2>
            <p class="text-xs text-gray-500 font-bold uppercase tracking-[0.15em] flex items-center gap-2">
              <span class="opacity-60">Demanda:</span> {{ post?.title }}
            </p>
          </div>
        </div>
        <button @click="$emit('close')" class="p-3 hover:bg-white rounded-2xl text-gray-400 transition-all shadow-sm hover:shadow-md"><X class="w-6 h-6" /></button>
      </div>
      <div class="grid grid-cols-2 gap-8 p-7">
        <!-- Coluna 1: Upload da Referência -->
        <div class="space-y-4">
          <label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Arquivo de Referência</label>
          <input ref="fileInputRef" type="file" accept="image/*,video/*" class="hidden" @change="$emit('fileSelect', $event)" />
          <div
            class="relative aspect-square rounded-[2rem] border border-gray-100 bg-gray-950 flex flex-col items-center justify-center gap-3 group hover:border-amber-400/50 transition-all cursor-pointer overflow-hidden shadow-2xl"
            @click="fileInputRef?.click()"
          >
            <template v-if="data.referencePreviewUrl">
              <video 
                v-if="isVideo(data.referencePreviewUrl, data.referenceName)"
                :src="data.referencePreviewUrl" 
                class="absolute inset-0 w-full h-full object-contain animate-in fade-in duration-500 shadow-inner"
                autoplay muted loop
              ></video>
              <img 
                v-else
                :src="data.referencePreviewUrl" 
                class="absolute inset-0 w-full h-full object-contain animate-in fade-in duration-500 shadow-inner" 
              />

              <!-- Botão Tela Cheia -->
              <a 
                :href="data.referencePreviewUrl" 
                target="_blank" 
                @click.stop
                class="absolute top-4 right-4 p-2.5 rounded-xl bg-black/40 hover:bg-black/60 text-white backdrop-blur-md transition-all opacity-0 group-hover:opacity-100 shadow-lg border border-white/10 z-10"
              >
                <Maximize class="w-4 h-4" />
              </a>
            </template>
            
            <div v-else-if="data.isUploading" class="flex flex-col items-center gap-3 text-amber-600">
              <div class="w-10 h-10 border-3 border-amber-200 border-t-amber-600 rounded-full animate-spin"></div>
              <span class="text-xs font-bold uppercase tracking-widest">Enviando...</span>
            </div>
            <div v-else class="flex flex-col items-center gap-3 text-gray-400 group-hover:text-amber-600 transition-colors">
              <div class="w-16 h-16 rounded-3xl bg-white shadow-sm flex items-center justify-center border border-gray-100 group-hover:border-amber-200">
                <Upload class="w-8 h-8" />
              </div>
              <div class="text-center">
                <span class="block text-sm font-bold">Clique para selecionar</span>
                <span class="text-[10px] text-gray-400 font-medium">Imagens ou Vídeos de inspiração</span>
              </div>
            </div>

            <div v-if="data.referencePreviewUrl" class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Button variant="outline" class="bg-white border-none text-xs font-bold px-6 h-10 rounded-xl">TROCAR ARQUIVO</Button>
            </div>
          </div>
        </div>

        <!-- Coluna 2: Detalhes da Demanda -->
        <div class="flex flex-col gap-6">
          <div>
            <div class="flex items-center gap-2 mb-2 text-gray-400">
              <Info class="w-3.5 h-3.5" />
              <label class="text-[10px] font-bold uppercase tracking-widest">Objetivo da Demanda</label>
            </div>
            <div class="p-6 rounded-[1.5rem] border border-gray-100 bg-gray-50/50 text-sm leading-relaxed text-gray-600 shadow-inner italic">
              "{{ post?.objective || 'Sem objetivo descrito.' }}"
            </div>
          </div>

          <div class="mt-auto p-5 rounded-2xl bg-amber-50 border border-amber-100">
            <h4 class="text-xs font-bold text-amber-700 uppercase mb-1">Por que adicionar referência?</h4>
            <p class="text-[11px] text-amber-600/80 leading-relaxed font-medium">
              Imagens de referência ajudam o time de criação a entender o estilo visual esperado, cores e composição, reduzindo o tempo de produção e revisões.
            </p>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-7 bg-gray-50 border-t border-gray-100 flex gap-4">
        <Button variant="outline" class="flex-1 h-14 rounded-2xl font-bold border-gray-200 text-gray-500" @click="$emit('close')">CANCELAR</Button>
        <Button
          class="flex-[2] h-14 rounded-2xl font-bold gap-2 shadow-xl shadow-amber-600/20 bg-amber-600 hover:bg-amber-700 text-white"
          :disabled="data.isSaving || data.isUploading || !data.referenceS3Key"
          @click="$emit('save')"
        >
          <template v-if="data.isSaving">
            <div class="w-5 h-5 border-3 border-white/30 border-t-white rounded-full animate-spin"></div>
            SALVANDO...
          </template>
          <template v-else>
            <Check class="w-6 h-6" />
            SALVAR REFERÊNCIA
          </template>
        </Button>
      </div>
    </div>
  </div>
</template>
