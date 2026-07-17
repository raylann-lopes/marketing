<script setup lang="ts">
import { CheckCircle, XCircle, Eye, Clock, ImageOff } from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'
import Avatar from '@/components/ui/Avatar.vue'
import { type PostApproval } from '@/services/approvalService'
import { getDemandTitle, statusLabel, statusVariant } from '@/lib/approvals'
import { isVideo } from '@/lib/media'

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
</script>

<template>
  <div
    class="group w-full cursor-pointer overflow-hidden rounded-lg border border-gray-100 bg-white shadow-sm transition-all hover:shadow-md"
    @click="$emit('click')"
  >
    <!-- Art thumbnail -->
    <div
      class="relative flex h-24 items-center justify-center overflow-hidden border-b border-gray-100 bg-gray-50"
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
      <div v-else class="flex flex-col items-center gap-1 text-gray-300">
        <ImageOff class="w-5 h-5" />
        <span class="text-[9px] font-medium">Sem arte</span>
      </div>
      <div
        class="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
      >
        <div class="bg-white rounded-full p-1.5 shadow-sm">
          <Eye class="w-4 h-4 text-primary" />
        </div>
      </div>
      <!-- Status badge top-right -->
      <div class="absolute top-1 right-1">
        <Badge :variant="statusVariant(approval.status)" class="text-[9px] px-1.5 py-0">{{ statusLabel(approval.status) }}</Badge>
      </div>
    </div>

    <div class="p-2">
      <p class="text-xs font-semibold text-gray-800 truncate">
        {{ getDemandTitle(approval) }}
      </p>
      <p class="mt-0.5 text-[10px] text-gray-400 line-clamp-1">
        {{ approval.caption || approval.post?.theme || '—' }}
      </p>

      <div class="flex items-center justify-between mt-1.5">
        <div v-if="approval.approvedUser" class="flex items-center gap-1">
          <Avatar :name="approval.approvedUser" size="sm" class="w-4 h-4 text-[8px]" />
          <span class="text-[9px] text-gray-400 truncate max-w-[64px]">{{ approval.approvedUser }}</span>
        </div>
        <div v-else class="flex items-center gap-1 text-[9px] text-amber-500 font-medium">
          <Clock class="w-2.5 h-2.5" />
          Pendente
        </div>

        <div v-if="isAdmin && approval.status === 'PENDING'" class="flex items-center gap-0.5">
          <button
            @click.stop="$emit('approve')"
            class="p-1 rounded-md bg-green-50 text-green-600 hover:bg-green-100 transition-colors"
            title="Aprovar"
          >
            <CheckCircle class="w-3.5 h-3.5" />
          </button>
          <button
            @click.stop="$emit('reject')"
            class="p-1 rounded-md bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
            title="Rejeitar"
          >
            <XCircle class="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
