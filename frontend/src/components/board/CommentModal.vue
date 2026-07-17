<script setup lang="ts">
import { X, MessageCircle } from 'lucide-vue-next'
import CommentThread from '@/components/board/CommentThread.vue'
import { type Post } from '@/services/postService'

defineProps<{
  isOpen: boolean
  post: Post | null
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'sent'): void
}>()
</script>

<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
  >
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="flex items-center justify-between p-5 border-b border-gray-100">
        <div class="flex items-center gap-2.5 min-w-0">
          <div class="w-9 h-9 shrink-0 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
            <MessageCircle class="w-4 h-4" />
          </div>
          <div class="min-w-0">
            <h2 class="text-sm font-bold text-gray-900 truncate">{{ post?.title || 'Demanda' }}</h2>
            <p class="text-[10px] text-gray-400 uppercase tracking-wide font-semibold">Comentários</p>
          </div>
        </div>
        <button @click="$emit('close')" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400 shrink-0">
          <X class="w-5 h-5" />
        </button>
      </div>
      <div class="p-5">
        <CommentThread :post-id="post?.id" @sent="$emit('sent')" />
      </div>
    </div>
  </div>
</template>
