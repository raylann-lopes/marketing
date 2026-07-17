<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { CheckCircle, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import ApprovalCard from '@/components/approvals/ApprovalCard.vue'
import ApprovalDetailModal from '@/components/approvals/ApprovalDetailModal.vue'
import { approvalService, type PostApproval } from '@/services/approvalService'
import { postService } from '@/services/postService'
import { mediaService } from '@/services/mediaService'
import { postIdOf } from '@/lib/approvals'
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

// Filter — PUBLISHED não é status de aprovação, é status do post; tratado à parte
const activeFilter = ref<'ALL' | 'PENDING' | 'APPROVE' | 'REJECTED' | 'PUBLISHED'>('ALL')

const filtered = computed(() => {
  let list = approvals.value
  if (activeFilter.value === 'PUBLISHED') {
    list = list.filter(a => a.post?.status === 'PUBLISHED')
  } else if (activeFilter.value !== 'ALL') {
    list = list.filter(a => a.status === activeFilter.value)
  }
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(a =>
      (a.post?.title || `Demanda #${postIdOf(a) ?? '-'}`).toLowerCase().includes(q) ||
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
  REJECTED: approvals.value.filter(a => a.status === 'REJECTED').length,
  PUBLISHED: approvals.value.filter(a => a.post?.status === 'PUBLISHED').length,
}))

// Paginação — cards menores permitem mais por tela, mas sem limite a lista
// cresceria pra sempre (aprovações nunca são excluídas automaticamente)
const currentPage = ref(1)
const itemsPerPage = 24
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / itemsPerPage)))
const paginatedApprovals = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filtered.value.slice(start, start + itemsPerPage)
})
function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++
}
function prevPage() {
  if (currentPage.value > 1) currentPage.value--
}
watch([activeFilter, search], () => {
  currentPage.value = 1
})

async function fetchApprovals() {
  loading.value = true
  error.value = ''
  try {
    // A API de aprovações retorna só postId — busca os posts em paralelo e
    // anexa cada um à sua aprovação para exibir título/tema/objetivo no modal
    const [data, posts] = await Promise.all([
      approvalService.getAll(),
      postService.getAll().catch(() => []),
    ])
    const postById = new Map(posts.map((p) => [String(p.id), p]))
    approvals.value = (Array.isArray(data) ? data : []).map((a) => ({
      ...a,
      post: a.post ?? postById.get(String(postIdOf(a))),
    }))
    await fetchPreviewUrls(approvals.value)
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Erro ao carregar aprovações')
  } finally {
    loading.value = false
  }
}

function getApprovalKey(approval: PostApproval) {
  return String(approval.id ?? postIdOf(approval) ?? approval.artS3Key)
}

function getPreviewUrl(approval: PostApproval) {
  return previewUrls.value[getApprovalKey(approval)] || ''
}

async function fetchPreviewUrls(list: PostApproval[]) {
  const pairs = await Promise.all(
    list.map(async (approval) => {
      const postId = postIdOf(approval)
      if (!postId) return [getApprovalKey(approval), ''] as const
      try {
        const urls = await mediaService.getArtPreviewUrls(postId)
        return [getApprovalKey(approval), urls[0] || ''] as const
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

  const postId = postIdOf(approval)
  if (postId) {
    loadingPreview.value = true
    try {
      const urls = await mediaService.getArtPreviewUrls(postId)
      artPreviewUrl.value = urls[0] || ''
    } catch {
      artPreviewUrl.value = ''
    } finally {
      loadingPreview.value = false
    }
  }
}

/**
 * Aprova ou rejeita e atualiza o item localmente — refazer o fetch da lista
 * re-dispararia todas as N requests de preview para mudar um único status.
 */
async function changeApprovalStatus(postId: string | number | undefined, action: 'approve' | 'reject') {
  if (!postId) return
  try {
    const updated = action === 'approve'
      ? await approvalService.approve(postId)
      : await approvalService.reject(postId)

    const index = approvals.value.findIndex(a => postIdOf(a) === postId)
    if (index !== -1 && approvals.value[index]) {
      // Preserva o post anexado e as URLs de preview já em cache
      approvals.value[index] = { ...approvals.value[index], ...updated, post: approvals.value[index].post }
    }

    if (isDetailOpen.value && selectedApproval.value && postIdOf(selectedApproval.value) === postId) {
      isDetailOpen.value = false
    }
    if (action === 'approve') feedback.success('Arte aprovada com sucesso.')
    else feedback.info('Arte marcada como rejeitada.')
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, action === 'approve' ? 'Erro ao aprovar' : 'Erro ao rejeitar'))
  }
}

const handleApprove = (postId: string | number | undefined) => changeApprovalStatus(postId, 'approve')
const handleReject = (postId: string | number | undefined) => changeApprovalStatus(postId, 'reject')

async function handleGenerateCaption() {
  const postId = postIdOf(selectedApproval.value ?? ({} as PostApproval))
  if (!postId) return

  loadingCaption.value = true
  try {
    const updatedApproval = await postService.generateCaption(
      postId,
      selectedApproval.value!.artS3Key
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
    <div class="flex gap-2 mb-6 flex-wrap">
      <button
        v-for="tab in (['ALL', 'PENDING', 'APPROVE', 'REJECTED', 'PUBLISHED'] as const)"
        :key="tab"
        @click="activeFilter = tab"
        :class="[
          'px-4 py-2 rounded-lg text-sm font-semibold transition-all border',
          activeFilter === tab
            ? 'bg-primary text-white border-primary shadow-sm'
            : 'bg-white text-gray-500 border-gray-200 hover:border-primary/30 hover:text-primary'
        ]"
      >
        {{ { ALL: 'Todos', PENDING: 'Pendentes', APPROVE: 'Aprovados', REJECTED: 'Rejeitados', PUBLISHED: 'Publicados' }[tab] }}
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
    <div v-else class="grid grid-cols-2 items-start gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
      <ApprovalCard
        v-for="approval in paginatedApprovals"
        :key="approval.id"
        :approval="approval"
        :preview-url="getPreviewUrl(approval)"
        :is-admin="isAdmin"
        @click="openDetail(approval)"
        @approve="handleApprove(postIdOf(approval))"
        @reject="handleReject(postIdOf(approval))"
      />
    </div>

    <!-- Pagination -->
    <div v-if="!loading && !error && filtered.length > itemsPerPage" class="mt-4 flex items-center justify-between">
      <p class="text-xs text-gray-500">
        Mostrando <span class="font-semibold">{{ (currentPage - 1) * itemsPerPage + 1 }}</span> a
        <span class="font-semibold">{{ Math.min(currentPage * itemsPerPage, filtered.length) }}</span> de
        <span class="font-semibold">{{ filtered.length }}</span> aprovações
      </p>
      <div class="flex items-center gap-2">
        <button
          @click="prevPage"
          :disabled="currentPage === 1"
          class="p-1.5 rounded-lg border border-gray-200 bg-white text-gray-400 hover:text-primary hover:border-primary/30 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          <ChevronLeft class="w-4 h-4" />
        </button>
        <span class="text-xs font-bold text-gray-700 mx-2">Página {{ currentPage }} de {{ totalPages }}</span>
        <button
          @click="nextPage"
          :disabled="currentPage === totalPages"
          class="p-1.5 rounded-lg border border-gray-200 bg-white text-gray-400 hover:text-primary hover:border-primary/30 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>
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
      @approve="handleApprove(postIdOf(selectedApproval!))"
      @reject="handleReject(postIdOf(selectedApproval!))"
      @generate-caption="handleGenerateCaption"
    />
  </AppLayout>
</template>
