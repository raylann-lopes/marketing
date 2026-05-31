<script setup lang="ts">
import { Eye } from 'lucide-vue-next'

type IdeaStatus = 'SUGGESTED' | 'SAVED' | 'DISMISSED' | 'CONVERTED'

interface ContentIdea {
  id: number
  format: string
  previewTitle: string
  viewsLabel: string
  status: IdeaStatus
  cardClass: string
  profile: string
  postedAt: string
}

interface Props {
  idea: ContentIdea
  statusLabel: string
}

defineProps<Props>()
defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <button
    type="button"
    class="group mx-auto w-full max-w-[190px] text-left focus:outline-none"
    @click="$emit('click')"
  >
    <div
      :class="[
        'relative aspect-[9/16] overflow-hidden rounded-lg p-2 text-white shadow-sm transition-all group-hover:-translate-y-0.5 group-hover:shadow-md group-focus:ring-2 group-focus:ring-primary/30',
        idea.cardClass,
      ]"
    >
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(255,255,255,0.22),transparent_34%)]" />
      <div class="relative flex h-full flex-col">
        <div class="flex items-center justify-end">
          <span class="rounded-full bg-white/18 px-1.5 py-0.5 text-[9px] font-semibold text-white/90 backdrop-blur">
            {{ idea.format }}
          </span>
        </div>

        <div class="flex flex-1 items-center justify-center px-1 text-center">
          <h2 class="text-[13px] font-extrabold leading-tight tracking-normal text-white drop-shadow-sm md:text-sm">
            {{ idea.previewTitle }}
          </h2>
        </div>

        <div class="mb-3 flex items-center justify-between gap-1">
          <span class="inline-flex items-center gap-0.5 rounded-full bg-black/28 px-1.5 py-0.5 text-[9px] font-bold text-white backdrop-blur">
            <Eye class="h-2.5 w-2.5" />
            {{ idea.viewsLabel }}
          </span>
          <span class="shrink-0 whitespace-nowrap rounded-full bg-white/90 px-1.5 py-0.5 text-[8px] font-bold leading-none text-gray-700">
            {{ statusLabel }}
          </span>
        </div>
      </div>
    </div>

    <div class="mt-0.5 px-0.5 leading-tight">
      <p class="truncate text-[11px] font-semibold text-gray-800">{{ idea.profile }}</p>
      <p class="text-[10px] text-gray-400">{{ idea.postedAt }}</p>
    </div>
  </button>
</template>
