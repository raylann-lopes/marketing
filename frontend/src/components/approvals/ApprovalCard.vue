<script setup lang="ts">
import { CheckCircle, XCircle, Eye, Clock, ImageOff } from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'
import Avatar from '@/components/ui/Avatar.vue'
import { type PostApproval } from '@/services/approvalService'

interface Props {
  approval: PostApproval
  previewUrl: string
  isAdmin: boolean
}

defineProps<Props>()

defineEmits<{
  (e: 'click'): void
  (e: 'approve'): void
  (e: 'reject'): void
}>()

function getDemandTitle(approval: PostApproval) {
  return approval.post?.title || `Demanda #${approval.post?.id ?? approval.id ?? '-'}`
}

function isVideo(url: string, filename?: string) {
  const check = (value: string) => {
    if (!value) return false
    const clean = (value.split('?')[0] ?? '').toLowerCase()
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mov')
  }
  return check(url) || (filename ? check(filename) : false)
}

function statusLabel(s: string) {
  return { PENDING: 'Pendente', APPROVE: 'Aprovado', REJECT: 'Rejeitado' }[s] ?? s
}

function statusVariant(s: string): 'warning' | 'success' | 'destructive' | 'secondary' {
  return (
    ({ PENDING: 'warning', APPROVE: 'success', REJECT: 'destructive' } as const)[s] ?? 'secondary'
  )
}
</script>

<template>
  <div
    :class="[
      'group cursor-pointer overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm transition-all hover:shadow-md',
      approval.status === 'APPROVE' ? 'w-full justify-self-start md:max-w-[280px]' : 'w-full',
    ]"
    @click="$emit('click')"
  >
    <!-- Art thumbnail -->
    <div
      :class="[
        'relative flex items-center justify-center overflow-hidden border-b border-gray-100 bg-gray-50',
        approval.status === 'APPROVE' ? 'h-28' : 'h-40',
      ]"
    >
      <video
        v-if="previewUrl && isVideo(previewUrl, approval.artName)"
        :src="previewUrl"
        class="h-full w-full object-cover"
        muted
        playsinline
        preload="metadata"
      />
      <img
        v-else-if="previewUrl"
        :src="previewUrl"
        class="h-full w-full object-cover"
        alt="Arte da demanda"
      />
      <div v-else class="flex flex-col items-center gap-2 text-gray-300">
        <ImageOff class="w-8 h-8" />
        <span class="text-[11px] font-medium">Arte indisponível</span>
      </div>
      <div
        class="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
      >
        <div class="bg-white rounded-full p-2 shadow-sm">
          <Eye class="w-5 h-5 text-primary" />
        </div>
      </div>
      <!-- Status badge top-right -->
      <div class="absolute top-2 right-2">
        <Badge :variant="statusVariant(approval.status)">{{ statusLabel(approval.status) }}</Badge>
      </div>
    </div>

    <div :class="approval.status === 'APPROVE' ? 'p-3' : 'p-4'">
      <p class="text-[11px] font-semibold text-gray-400 uppercase tracking-wide">Demanda</p>
      <p class="text-sm font-semibold text-gray-800 truncate mt-0.5">
        {{ getDemandTitle(approval) }}
      </p>
      <p v-if="approval.post?.theme" class="text-xs text-gray-400 mt-1 truncate">
        {{ approval.post.theme }}
      </p>
      <p
        :class="[
          'mt-1 text-xs text-gray-400',
          approval.status === 'APPROVE' ? 'line-clamp-1' : 'line-clamp-2',
        ]"
      >
        {{ approval.caption }}
      </p>

      <div class="flex items-center justify-between mt-4">
        <div v-if="approval.approvedUser" class="flex items-center gap-1.5">
          <Avatar :name="approval.approvedUser" size="sm" class="w-5 h-5 text-[9px]" />
          <span class="text-[10px] text-gray-400">{{ approval.approvedUser }}</span>
        </div>
        <div v-else class="flex items-center gap-1 text-[10px] text-amber-500 font-medium">
          <Clock class="w-3 h-3" />
          Aguardando revisão
        </div>

        <div v-if="isAdmin && approval.status === 'PENDING'" class="flex items-center gap-1">
          <button
            @click.stop="$emit('approve')"
            class="p-1.5 rounded-lg bg-green-50 text-green-600 hover:bg-green-100 transition-colors"
            title="Aprovar"
          >
            <CheckCircle class="w-4 h-4" />
          </button>
          <button
            @click.stop="$emit('reject')"
            class="p-1.5 rounded-lg bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
            title="Rejeitar"
          >
            <XCircle class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
