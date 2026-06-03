<script setup lang="ts">
import { X, Plus, Pencil } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'

interface DayPost {
  time: string | null
  tag: string
  tagColor: string
  title: string
  description: string
  client: string | null
  status: string | null
}

interface Props {
  isOpen: boolean
  selectedDay: number | null
  currentMonth: string
  posts: DayPost[]
}

defineProps<Props>()
defineEmits<{
  (e: 'close'): void
  (e: 'openPostModal', day: number | null): void
}>()
</script>

<template>
  <transition name="slide">
    <div v-if="isOpen && selectedDay" class="w-80 bg-white rounded-xl border border-gray-100 shadow-sm flex flex-col shrink-0">
      <div class="flex items-start justify-between p-5 border-b border-gray-100">
        <div>
          <p class="text-xs text-gray-400 uppercase tracking-wide">Agenda do Dia</p>
          <p class="text-4xl font-bold text-gray-900">{{ selectedDay }} {{ currentMonth?.substring(0,3) }}</p>
        </div>
        <button class="p-1.5 hover:bg-gray-100 rounded-lg" @click="$emit('close')">
          <X class="w-4 h-4 text-gray-500" />
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-4 space-y-3">
        <div v-if="posts.length === 0" class="flex flex-col items-center justify-center h-40 text-center text-gray-400">
          <Plus class="w-8 h-8 mb-2 opacity-20" />
          <p class="text-sm">Nenhum post agendado para este dia.</p>
        </div>
        <div
          v-for="(post, i) in posts"
          :key="i"
          class="bg-gray-50 rounded-xl p-4 space-y-2 border border-transparent hover:border-primary/20 transition-all"
        >
          <div class="flex items-center justify-between">
            <span :class="['text-[10px] font-bold px-2 py-0.5 rounded-full uppercase', post.tagColor]">{{ post.tag }}</span>
            <span v-if="post.time" class="text-xs font-semibold text-primary">{{ post.time }}</span>
          </div>
          <p class="text-sm font-semibold text-gray-800 leading-snug">{{ post.title }}</p>
          <p class="text-xs text-gray-500 line-clamp-2">{{ post.description }}</p>
          <p v-if="post.client" class="text-[10px] text-gray-400 font-medium">● {{ post.client }}</p>
        </div>
      </div>

      <div class="p-4 border-t border-gray-100">
        <Button class="w-full gap-2" @click="$emit('openPostModal', selectedDay)">
          <Pencil class="w-4 h-4" />
          Adicionar Novo no Dia
        </Button>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateX(20px); }
</style>
