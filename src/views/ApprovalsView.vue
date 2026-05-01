<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { CheckCircle, XCircle, Eye, X, Clock, ImageOff, Sparkles } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Avatar from '@/components/ui/Avatar.vue'
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
      getDemandTitle(a).toLowerCase().includes(q) ||
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

function getDemandTitle(approval: PostApproval) {
  return approval.post?.title || `Demanda #${approval.post?.id ?? approval.id ?? '-'}`
}

function getPreviewUrl(approval: PostApproval) {
  return previewUrls.value[getApprovalKey(approval)] || ''
}

function isVideo(url: string, filename?: string) {
  const check = (value: string) => {
    if (!value) return false
    const clean = (value.split('?')[0] ?? '').toLowerCase()
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mov')
  }
  return check(url) || (filename ? check(filename) : false)
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
    isDetailOpen.value = false
    feedback.success('Arte aprovada com sucesso.')
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, 'Erro ao aprovar'))
  }
}

async function handleReject(postId: string | number) {
  try {
    await approvalService.reject(postId)
    await fetchApprovals()
    isDetailOpen.value = false
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

function statusLabel(s: string) {
  return { PENDING: 'Pendente', APPROVE: 'Aprovado', REJECT: 'Rejeitado' }[s] ?? s
}
function statusVariant(s: string): 'warning' | 'success' | 'destructive' | 'secondary' {
  return ({ PENDING: 'warning', APPROVE: 'success', REJECT: 'destructive' } as const)[s] ?? 'secondary'
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
      <div
        v-for="approval in filtered"
        :key="approval.id"
        :class="[
          'group cursor-pointer overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm transition-all hover:shadow-md',
          approval.status === 'APPROVE' ? 'w-full justify-self-start md:max-w-[280px]' : 'w-full',
        ]"
        @click="openDetail(approval)"
      >
        <!-- Art thumbnail -->
        <div
          :class="[
            'relative flex items-center justify-center overflow-hidden border-b border-gray-100 bg-gray-50',
            approval.status === 'APPROVE' ? 'h-28' : 'h-40',
          ]"
        >
          <video
            v-if="getPreviewUrl(approval) && isVideo(getPreviewUrl(approval), approval.artName)"
            :src="getPreviewUrl(approval)"
            class="h-full w-full object-cover"
            muted
            playsinline
            preload="metadata"
          />
          <img
            v-else-if="getPreviewUrl(approval)"
            :src="getPreviewUrl(approval)"
            class="h-full w-full object-cover"
            alt="Arte da demanda"
          />
          <div v-else class="flex flex-col items-center gap-2 text-gray-300">
            <ImageOff class="w-8 h-8" />
            <span class="text-[11px] font-medium">Arte indisponível</span>
          </div>
          <div class="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
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
          <p class="text-sm font-semibold text-gray-800 truncate mt-0.5">{{ getDemandTitle(approval) }}</p>
          <p v-if="approval.post?.theme" class="text-xs text-gray-400 mt-1 truncate">{{ approval.post.theme }}</p>
          <p :class="['mt-1 text-xs text-gray-400', approval.status === 'APPROVE' ? 'line-clamp-1' : 'line-clamp-2']">
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
                @click.stop="handleApprove(approval.post!.id!)"
                class="p-1.5 rounded-lg bg-green-50 text-green-600 hover:bg-green-100 transition-colors"
                title="Aprovar"
              >
                <CheckCircle class="w-4 h-4" />
              </button>
              <button
                @click.stop="handleReject(approval.post!.id!)"
                class="p-1.5 rounded-lg bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
                title="Rejeitar"
              >
                <XCircle class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Detail modal -->
    <div v-if="isDetailOpen && selectedApproval" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-white rounded-3xl shadow-2xl w-full max-w-3xl overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between p-6 border-b border-gray-100">
          <div>
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Demanda</p>
            <h2 class="text-xl font-bold text-gray-900">{{ getDemandTitle(selectedApproval) }}</h2>
            <Badge :variant="statusVariant(selectedApproval.status)" class="mt-1">{{ statusLabel(selectedApproval.status) }}</Badge>
          </div>
          <button @click="isDetailOpen = false" class="p-2 hover:bg-gray-100 rounded-xl text-gray-400">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="grid grid-cols-2 gap-0">
          <!-- Arte -->
          <div class="border-r border-gray-100 bg-gray-50 flex items-center justify-center min-h-[320px] relative">
            <div v-if="loadingPreview" class="flex flex-col items-center gap-2 text-gray-400">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
              <span class="text-xs">Carregando arte...</span>
            </div>
            <video
              v-else-if="artPreviewUrl && isVideo(artPreviewUrl, selectedApproval.artName)"
              :src="artPreviewUrl"
              class="max-h-80 max-w-full object-contain rounded-lg"
              controls
              playsinline
            />
            <img
              v-else-if="artPreviewUrl"
              :src="artPreviewUrl"
              class="max-h-80 max-w-full object-contain rounded-lg"
              alt="Arte do post"
            />
            <div v-else class="flex flex-col items-center gap-2 text-gray-300">
              <ImageOff class="w-12 h-12" />
              <span class="text-xs">Arte indisponível</span>
            </div>
          </div>

          <!-- Detalhes -->
          <div class="p-6 space-y-4 flex flex-col">
            <div v-if="selectedApproval.post?.theme || selectedApproval.post?.objective">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-1">Detalhes da demanda</p>
              <div class="text-sm text-gray-700 bg-gray-50 rounded-lg p-3 space-y-2">
                <p v-if="selectedApproval.post?.theme">
                  <span class="font-semibold text-gray-800">Tema:</span> {{ selectedApproval.post.theme }}
                </p>
                <p v-if="selectedApproval.post?.objective">
                  <span class="font-semibold text-gray-800">Objetivo:</span> {{ selectedApproval.post.objective }}
                </p>
              </div>
            </div>

            <div>
              <div class="flex items-center justify-between mb-1">
                <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Legenda</p>
                <button 
                  v-if="isAdmin && selectedApproval.status === 'PENDING'"
                  @click="handleGenerateCaption"
                  :disabled="loadingCaption"
                  class="flex items-center gap-1.5 text-[10px] font-bold text-primary hover:text-primary/80 transition-colors disabled:opacity-50"
                >
                  <Sparkles v-if="!loadingCaption" class="w-3 h-3" />
                  <div v-else class="w-3 h-3 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
                  {{ loadingCaption ? 'Gerando...' : 'GERAR COM IA' }}
                </button>
              </div>
              <p class="text-sm text-gray-700 bg-gray-50 rounded-lg p-3 leading-relaxed whitespace-pre-wrap max-h-40 overflow-y-auto">{{ selectedApproval.caption || 'Sem legenda gerada.' }}</p>
            </div>

            <div v-if="selectedApproval.approvedUser">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-1">Revisado por</p>
              <p class="text-sm text-gray-700">{{ selectedApproval.approvedUser }}</p>
            </div>

            <!-- Admin actions -->
            <div v-if="isAdmin && selectedApproval.status === 'PENDING'" class="mt-auto flex gap-3 pt-4 border-t border-gray-100">
              <Button
                class="flex-1 bg-green-600 hover:bg-green-700 gap-2"
                @click="handleApprove(selectedApproval.post!.id!)"
              >
                <CheckCircle class="w-4 h-4" />
                Aprovar Arte
              </Button>
              <Button
                variant="outline"
                class="flex-1 border-red-200 text-red-500 hover:bg-red-50 gap-2"
                @click="handleReject(selectedApproval.post!.id!)"
              >
                <XCircle class="w-4 h-4" />
                Rejeitar
              </Button>
            </div>

            <div v-else-if="selectedApproval.status === 'APPROVE'" class="mt-auto pt-4 border-t border-gray-100">
              <div class="inline-flex w-fit max-w-full items-center gap-2 rounded-lg bg-green-50 px-2.5 py-2 text-green-700">
                <CheckCircle class="h-4 w-4 shrink-0" />
                <span class="text-xs font-medium">Arte aprovada — pronta para publicação</span>
              </div>
            </div>

            <div v-else-if="selectedApproval.status === 'REJECT'" class="mt-auto pt-4 border-t border-gray-100">
              <div class="flex items-center gap-2 bg-red-50 text-red-600 rounded-lg px-3 py-2">
                <XCircle class="w-4 h-4" />
                <span class="text-sm font-medium">Arte rejeitada — aguardando revisão</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </AppLayout>
</template>
