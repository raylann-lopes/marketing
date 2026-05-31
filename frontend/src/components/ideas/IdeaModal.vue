<script setup lang="ts">
import { X, Eye, Bookmark, ThumbsDown, Plus } from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'

type IdeaStatus = 'SUGGESTED' | 'SAVED' | 'DISMISSED' | 'CONVERTED'
type IdeaPriority = 'ALTA' | 'MEDIA' | 'BAIXA'

interface ContentIdea {
  id: number
  client: string
  niche: string
  format: string
  priority: IdeaPriority
  title: string
  previewTitle: string
  theme: string
  objective: string
  reason: string
  sourceSignal: string
  profile: string
  postedAt: string
  viewsLabel: string
  viewsCount: number
  cardClass: string
  status: IdeaStatus
}

interface Props {
  idea: ContentIdea
  statusLabel: string
  statusVariant: 'secondary' | 'success' | 'destructive' | 'purple'
  priorityClass: string
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'setStatus', id: number, status: IdeaStatus): void
}>()
</script>

<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-gray-950/50 p-4 backdrop-blur-sm"
    @click.self="$emit('close')"
  >
    <section class="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-gray-200 bg-white shadow-2xl">
      <div class="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-gray-100 bg-white px-5 py-4">
        <div>
          <div class="mb-2 flex flex-wrap items-center gap-2">
            <Badge :variant="statusVariant">{{ statusLabel }}</Badge>
            <span :class="['rounded-full border px-2.5 py-0.5 text-xs font-semibold', priorityClass]">
              {{ idea.priority }}
            </span>
            <span class="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-semibold text-gray-600">
              <Eye class="h-3.5 w-3.5" />
              {{ idea.viewsLabel }}
            </span>
          </div>
          <h2 class="text-xl font-bold leading-tight text-gray-900">{{ idea.title }}</h2>
          <p class="mt-1 text-sm text-gray-500">
            {{ idea.client }} · {{ idea.profile }}
          </p>
        </div>
        <Button variant="ghost" size="icon" class="shrink-0" aria-label="Fechar janela" @click="$emit('close')">
          <X class="h-5 w-5" />
        </Button>
      </div>

      <div class="space-y-4 p-5">
        <div :class="['rounded-2xl p-6 text-center text-white', idea.cardClass]">
          <p class="text-sm font-semibold uppercase tracking-wide text-white/70">Gancho viral</p>
          <p class="mx-auto mt-3 max-w-md text-3xl font-extrabold leading-tight text-white">
            {{ idea.previewTitle }}
          </p>
        </div>

        <div class="grid gap-3 md:grid-cols-2">
          <div class="rounded-xl border border-gray-100 bg-gray-50 p-3">
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Tema</p>
            <p class="mt-1 text-sm font-semibold text-gray-900">{{ idea.theme }}</p>
          </div>
          <div class="rounded-xl border border-gray-100 bg-gray-50 p-3">
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Objetivo</p>
            <p class="mt-1 text-sm font-semibold text-gray-900">{{ idea.objective }}</p>
          </div>
        </div>

        <div class="rounded-xl border border-primary/10 bg-primary/5 p-4">
          <p class="text-[11px] font-semibold uppercase tracking-wide text-primary">Ideia para produção</p>
          <p class="mt-2 text-sm leading-6 text-gray-700">{{ idea.reason }}</p>
        </div>

        <div class="rounded-xl border border-gray-100 p-4">
          <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Sinal usado pela IA</p>
          <p class="mt-2 text-sm text-gray-600">{{ idea.sourceSignal }}</p>
        </div>
      </div>

      <div class="flex flex-wrap justify-end gap-2 border-t border-gray-100 bg-gray-50 px-5 py-4">
        <Button variant="outline" size="sm" class="gap-2" @click="$emit('setStatus', idea.id, 'SAVED')">
          <Bookmark class="h-4 w-4" />
          Salvar
        </Button>
        <Button variant="outline" size="sm" class="gap-2" @click="$emit('setStatus', idea.id, 'DISMISSED')">
          <ThumbsDown class="h-4 w-4" />
          Descartar
        </Button>
        <Button size="sm" class="gap-2" @click="$emit('setStatus', idea.id, 'CONVERTED')">
          <Plus class="h-4 w-4" />
          Converter em demanda
        </Button>
      </div>
    </section>
  </div>
</template>
