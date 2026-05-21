<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { CheckCircle } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import ApprovalCard from '@/components/approvals/ApprovalCard.vue'
import ApprovalDetailModal from '@/components/approvals/ApprovalDetailModal.vue'
import { approvalService, type PostApproval } from '@/services/approvalService'
import { postService } from '@/services/postService'
import { apiFetch } from '@/lib/api'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'

const search = ref('')
const approvals = ref<PostApproval[]>([])
const previewUrls = ref<Record<string, string>>({})
const loading = ref(true)
const error = ref('')
const role = localStorage.getItem('role') || sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'
const feedback = useFeedback()

// Detail modal
const selectedApproval = ref<PostApproval | null>(null)
const artPreviewUrl = ref('')
const loadingPreview = ref(false)
const loadingCaption = ref(false)
const isDetailOpen = ref(false)

// Filter
const activeFilter = ref<'ALL' | 'PENDING' | 'APPROVE' | 'REJECT'>('ALL')

const filtered = computed(() => {
  let list = approvals.value
  if (activeFilter.value !== 'ALL') list = list.filter(a => a.status === activeFilter.value)
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(a =>
      (a.post?.title || `Demanda #${a.post?.id ?? a.id ?? '-'}`).toLowerCase().includes(q) ||
      a.post?.theme?.toLowerCase().includes(q) ||
      a.caption?.toLowerCase().includes(q)
    )
  }
  return list
})

const counts = computed(() => ({
  ALL: approvals.value.length,
  PENDING: approvals.value.filter(a => a.status === 'PENDING').length,
  APPROVE: approvals.value.filter(a => a.status === 'APPROVE').length,
  REJECT: approvals.value.filter(a => a.status === 'REJECT').length,
}))

async function fetchApprovals() {
  loading.value = true
  error.value = ''
  try {
    const data = await approvalService.getAll()
    approvals.value = Array.isArray(data) ? data : []
    await fetchPreviewUrls(approvals.value)
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Erro ao carregar aprovações')
  } finally {
    loading.value = false
  }
}

function getApprovalKey(approval: PostApproval) {
  return String(approval.id ?? approval.post?.id ?? approval.artS3Key)
}

function getPreviewUrl(approval: PostApproval) {
  return previewUrls.value[getApprovalKey(approval)] || ''
}

async function fetchPreviewUrls(list: PostApproval[]) {
  const pairs = await Promise.all(
    list.map(async (approval) => {
      if (!approval.post?.id) return [getApprovalKey(approval), ''] as const
      try {
        const res = await apiFetch<{ mediaUrl: string }>(`/api/media/art-url?postId=${approval.post.id}`)
        return [getApprovalKey(approval), res.mediaUrl || ''] as const
      } catch {
        return [getApprovalKey(approval), ''] as const
      }
    }),
  )

  previewUrls.value = Object.fromEntries(pairs)
}

async function openDetail(approval: PostApproval) {
  selectedApproval.value = approval
  artPreviewUrl.value = ''
  loadingPreview.value = false
  isDetailOpen.value = true

  const cachedUrl = getPreviewUrl(approval)
  if (cachedUrl) {
    artPreviewUrl.value = cachedUrl
    return
  }

  if (approval.post?.id) {
    loadingPreview.value = true
    try {
      const res = await apiFetch<{ mediaUrl: string }>(`/api/media/art-url?postId=${approval.post.id}`)
      artPreviewUrl.value = res.mediaUrl
    } catch {
      artPreviewUrl.value = ''
    } finally {
      loadingPreview.value = false
    }
  }
}

async function handleApprove(postId: string | number) {
  try {
    await approvalService.approve(postId)
    await fetchApprovals()
    if (isDetailOpen.value && selectedApproval.value?.post?.id === postId) {
      isDetailOpen.value = false
    }
    feedback.success('Arte aprovada com sucesso.')
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, 'Erro ao aprovar'))
  }
}

async function handleReject(postId: string | number) {
  try {
    await approvalService.reject(postId)
    await fetchApprovals()
    if (isDetailOpen.value && selectedApproval.value?.post?.id === postId) {
      isDetailOpen.value = false
    }
    feedback.info('Arte marcada como rejeitada.')
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, 'Erro ao rejeitar'))
  }
}

async function handleGenerateCaption() {
  if (!selectedApproval.value?.post?.id) return
  
  loadingCaption.value = true
  try {
    const updatedApproval = await postService.generateCaption(
      selectedApproval.value.post.id,
      selectedApproval.value.artS3Key
    )
    if (selectedApproval.value) {
      selectedApproval.value.caption = updatedApproval.caption
    }
    // Update in the main list too
    const index = approvals.value.findIndex(a => a.id === selectedApproval.value?.id)
    if (index !== -1 && approvals.value[index]) {
      approvals.value[index].caption = updatedApproval.caption
    }
    feedback.success('Legenda gerada com sucesso.')
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, 'Erro ao gerar legenda'))
  } finally {
    loadingCaption.value = false
  }
}

onMounted(fetchApprovals)
</script>

<template>
  <AppLayout v-model:search="search" topbar-placeholder="Buscar aprovações por demanda ou legenda...">

    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Aprovações</h1>
        <p class="text-gray-500 mt-1">Acompanhe e gerencie as artes enviadas para aprovação.</p>
      </div>
    </div>

    <!-- Filter tabs -->
    <div class="flex gap-2 mb-6">
      <button
        v-for="tab in (['ALL', 'PENDING', 'APPROVE', 'REJECT'] as const)"
        :key="tab"
        @click="activeFilter = tab"
        :class="[
          'px-4 py-2 rounded-lg text-sm font-semibold transition-all border',
          activeFilter === tab
            ? 'bg-primary text-white border-primary shadow-sm'
            : 'bg-white text-gray-500 border-gray-200 hover:border-primary/30 hover:text-primary'
        ]"
      >
        {{ { ALL: 'Todos', PENDING: 'Pendentes', APPROVE: 'Aprovados', REJECT: 'Rejeitados' }[tab] }}
        <span :class="['ml-1.5 text-xs px-1.5 py-0.5 rounded-full', activeFilter === tab ? 'bg-white/20' : 'bg-gray-100']">
          {{ counts[tab] }}
        </span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="flex flex-col items-center justify-center h-64">
      <p class="text-red-500 mb-4">{{ error }}</p>
      <Button variant="outline" @click="fetchApprovals">Tentar Novamente</Button>
    </div>

    <!-- Empty -->
    <div v-else-if="filtered.length === 0" class="flex flex-col items-center justify-center h-64 bg-white rounded-xl border border-gray-100">
      <CheckCircle class="w-10 h-10 text-gray-200 mb-3" />
      <p class="text-gray-400 font-medium">Nenhuma aprovação encontrada.</p>
    </div>

    <!-- Grid -->
    <div v-else class="grid grid-cols-1 items-start gap-4 md:grid-cols-2 lg:grid-cols-3">
      <ApprovalCard
        v-for="approval in filtered"
        :key="approval.id"
        :approval="approval"
        :preview-url="getPreviewUrl(approval)"
        :is-admin="isAdmin"
        @click="openDetail(approval)"
        @approve="handleApprove(approval.post!.id!)"
        @reject="handleReject(approval.post!.id!)"
      />
    </div>

    <!-- Detail modal -->
    <ApprovalDetailModal
      :is-open="isDetailOpen"
      :approval="selectedApproval"
      :art-url="artPreviewUrl"
      :loading-preview="loadingPreview"
      :loading-caption="loadingCaption"
      :is-admin="isAdmin"
      @close="isDetailOpen = false"
      @approve="handleApprove(selectedApproval!.post!.id!)"
      @reject="handleReject(selectedApproval!.post!.id!)"
      @generate-caption="handleGenerateCaption"
    />
  </AppLayout>
</template>
