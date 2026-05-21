<script setup lang="ts">
import { computed } from 'vue'
import { Eye, ChevronDown, Pencil, Sparkles, Image, AlertTriangle, Send, Clock } from 'lucide-vue-next'
import Avatar from '@/components/ui/Avatar.vue'
import { type Post, getPostId, getPostClientId } from '@/services/postService'
import type { PostApproval } from '@/services/approvalService'

interface Props {
  card: Post
  columnId: string
  columnTheme: any
  isExpanded: boolean
  approval?: PostApproval | null
  clientName: string
  overdue: boolean
  priorityInfo: { label: string; className: string }
  dueDateLabel: string
  responsibleLabel: string
  postTypeLabel: string
  approvalStatus: { text: string; className: string }
  rejectionMessage: string
  isSendingApproval: boolean
}

const props = defineProps<Props>()

defineEmits<{
  (e: 'preview'): void
  (e: 'toggleExpand'): void
  (e: 'edit'): void
  (e: 'prepareApproval'): void
  (e: 'internalReview'): void
  (e: 'addReference'): void
  (e: 'viewReference'): void
  (e: 'resend'): void
}>()

function formatSentAt(dateStr?: string) {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch (e) {
    return dateStr
  }
}

const canShowReference = computed(() => ['DEMAND', 'IN_PRODUCTION', 'FINISHED'].includes(props.columnId))
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-100 p-4 shadow-sm hover:shadow-md transition-all cursor-grab active:cursor-grabbing group">
    <div v-if="card.isUrgent" class="mb-2 flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-red-50 border border-red-100 animate-pulse">
      <AlertTriangle class="w-3 h-3 text-red-500" />
      <span class="text-[9px] font-bold text-red-600 uppercase tracking-wider">URGENTE</span>
    </div>

    <div class="flex items-start justify-between gap-2 mb-3">
      <div class="flex flex-wrap items-center gap-1.5 min-w-0">
        <span :class="['text-[10px] font-bold px-2 py-0.5 rounded-full uppercase leading-relaxed whitespace-nowrap overflow-hidden text-ellipsis', columnTheme.clientBadge]">
          {{ clientName }}
        </span>
        <div v-if="canShowReference && card.referenceImageS3Key" @click.stop="$emit('viewReference')" class="cursor-pointer p-1 rounded-md bg-amber-50 border border-amber-100" title="Ver imagem de referência">
          <Image class="w-3 h-3 text-amber-600" />
        </div>
      </div>
      <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          @click.stop="$emit('preview')"
          class="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary/10 transition-colors"
          title="Visualizar imagem"
        >
          <Eye class="w-3.5 h-3.5" />
        </button>
        <button
          @click.stop="$emit('toggleExpand')"
          class="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary/10 transition-colors"
          :title="isExpanded ? 'Ocultar dados' : 'Expandir dados'"
        >
          <ChevronDown :class="['w-3.5 h-3.5 transition-transform', isExpanded ? 'rotate-180' : '']" />
        </button>
        <button
          @click.stop="$emit('edit')"
          class="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary/10 transition-colors"
          title="Editar"
        >
          <Pencil class="w-3.5 h-3.5" />
        </button>
      </div>
    </div>

    <p class="text-sm font-semibold text-gray-800 leading-snug line-clamp-2">{{ card.title }}</p>
    <p class="text-xs text-gray-500 mt-1 line-clamp-1">{{ card.theme || 'Sem tema definido' }}</p>

    <div
      v-if="columnId === 'REJECTED'"
      :class="['mt-3 rounded-lg px-2.5 py-2', columnTheme.softContainer]"
    >
      <p class="text-[9px] font-bold uppercase tracking-wide text-red-500">Mensagem de rejeição</p>
      <p :class="['mt-1 text-[11px] font-medium leading-snug', columnTheme.softText]">
        {{ rejectionMessage }}
      </p>
    </div>

    <div
      v-if="columnId === 'WAITING_APPROVAL' && approval?.whatsappSentAt"
      class="mt-3 flex items-center justify-between gap-2 px-2 py-1.5 rounded-lg border border-amber-100 bg-amber-50/50"
    >
      <p class="text-[10px] font-semibold text-amber-700 truncate">
        <span class="font-bold text-amber-600 uppercase text-[9px] mr-1">Enviado:</span>
        {{ formatSentAt(approval.whatsappSentAt) }}
      </p>
      <button
        @click.stop="$emit('resend')"
        :disabled="isSendingApproval"
        class="p-1 rounded-md bg-white border border-amber-200 text-amber-600 hover:bg-amber-100 transition-colors disabled:opacity-50 shrink-0"
      >
        <Send class="w-2.5 h-2.5" />
      </button>
    </div>

    <div
      v-if="columnId === 'SCHEDULE' && card.scheduledAt"
      class="mt-3 flex items-center gap-1.5 px-2 py-1.5 rounded-lg border border-indigo-100 bg-indigo-50/50"
    >
      <Clock class="w-3 h-3 text-indigo-500 shrink-0" />
      <p class="text-[10px] font-semibold text-indigo-700 truncate">
        <span class="font-bold text-indigo-600 uppercase text-[9px] mr-1">Programado:</span>
        {{ formatSentAt(card.scheduledAt) }}
      </p>
    </div>

    <div v-if="isExpanded" class="mt-3 grid grid-cols-2 gap-2 text-[10px]">
      <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
        <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Etapa</p>
        <p class="mt-0.5 font-semibold text-gray-700 truncate">{{ columnId }}</p>
      </div>
      <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
        <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Prioridade</p>
        <p :class="['mt-0.5 font-semibold truncate', priorityInfo.className.split(' ')[0]]">
          {{ priorityInfo.label }}
        </p>
      </div>
      <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
        <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Responsável</p>
        <p class="mt-0.5 font-semibold text-gray-700 truncate">{{ responsibleLabel }}</p>
      </div>
      <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
        <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Prazo</p>
        <p :class="['mt-0.5 font-semibold truncate', overdue ? 'text-red-600' : 'text-gray-700']">
          {{ dueDateLabel }}
        </p>
      </div>
      <div class="col-span-2 rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
        <div class="flex items-center justify-between gap-2">
          <div class="min-w-0">
            <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Tipo</p>
            <p class="mt-0.5 font-semibold text-gray-700 truncate">{{ postTypeLabel }}</p>
          </div>
          <Avatar v-if="card.userId" :name="'U' + card.userId" size="sm" class="w-6 h-6 shrink-0 text-[10px]" />
          <div v-else class="w-6 h-6 shrink-0 rounded-full bg-gray-100 flex items-center justify-center text-[10px] text-gray-400 border border-gray-200">-</div>
        </div>
      </div>
      <div class="col-span-2 rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
        <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Aprovação</p>
        <p :class="['mt-0.5 font-semibold truncate', approvalStatus.className]">
          {{ approvalStatus.text }}
        </p>
      </div>
    </div>

    <!-- Botões de ação na Demanda -->
    <div v-if="columnId === 'DEMAND'" class="mt-3">
      <button 
        v-if="!card.referenceImageS3Key"
        @click.stop="$emit('addReference')"
        :class="['w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all', columnTheme.primaryButton]"
      >
        <Image class="w-3 h-3" />
        ADD REFERÊNCIA
      </button>

      <button 
        v-else
        @click.stop="$emit('addReference')"
        :class="['w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all', columnTheme.primaryButton]"
      >
        <Pencil class="w-3 h-3" />
        ALTERAR REFERÊNCIA
      </button>
    </div>

    <button 
      v-if="columnId === 'IN_PRODUCTION'"
      @click.stop="$emit('prepareApproval')"
      :class="['mt-3 w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all', columnTheme.primaryButton]"
    >
      <Sparkles class="w-3 h-3" />
      {{ approval ? 'EDITAR APROVAÇÃO' : 'PREPARAR APROVAÇÃO' }}
    </button>

    <div v-if="columnId === 'FINISHED'" class="mt-3">
      <button
        v-if="!approval"
        @click.stop="$emit('prepareApproval')"
        :class="['w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all', columnTheme.primaryButton]"
      >
        <Sparkles class="w-3 h-3" />
        PREPARAR APROVAÇÃO
      </button>

      <button
        v-else
        @click.stop="$emit('internalReview')"
        :disabled="isSendingApproval"
        :class="['w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all disabled:opacity-60 disabled:cursor-not-allowed', columnTheme.primaryButton]"
      >
        <Eye class="w-3 h-3" />
        APROVAÇÃO INTERNA
      </button>
    </div>
  </div>
</template>
