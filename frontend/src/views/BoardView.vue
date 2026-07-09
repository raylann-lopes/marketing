<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import { postService, type Post, type PostStatus, getPostClientId, type PostFormData } from '@/services/postService'
import { clientService, type Client } from '@/services/clientService'
import { approvalService, type PostApproval } from '@/services/approvalService'
import { mediaService } from '@/services/mediaService'
import { getCurrentUserId } from '@/lib/api'
import { useFeedback } from '@/lib/feedback'
import draggable from 'vuedraggable'

// Components
import BoardCard from '@/components/board/BoardCard.vue'
import PostModal from '@/components/board/PostModal.vue'
import ApprovalModal from '@/components/board/ApprovalModal.vue'
import ArtPreviewModal from '@/components/board/ArtPreviewModal.vue'
import ReferenceModal from '@/components/board/ReferenceModal.vue'

type DraggableChangeEvent<T> = {
  added?: { element: T; newIndex: number }
  removed?: { element: T; oldIndex: number }
  moved?: { element: T; newIndex: number; oldIndex: number }
}

type BoardColumn = {
  id: PostStatus | string
  label: string
  color: string
  cards: Post[]
}

type ColumnTheme = {
  clientBadge: string
  primaryButton: string
  filledButton: string
  softContainer: string
  softText: string
  ghostButton: string
}

type ArtPreviewMode = 'preview' | 'internal-review'

type ArtItem = { s3Key: string; artName: string; previewUrl: string }

const MAX_CAROUSEL_ITEMS = 10

function isVideoFileName(name?: string) {
  if (!name) return false
  const clean = (name.split('?')[0] ?? '').toLowerCase()
  return ['.mp4', '.webm', '.mov', '.avi', '.mkv'].some((ext) => clean.endsWith(ext))
}

const columns = ref<BoardColumn[]>([
  { id: 'DEMAND', label: 'Demanda', color: 'bg-gray-400', cards: [] },
  { id: 'IN_PRODUCTION', label: 'Em Produção', color: 'bg-blue-500', cards: [] },
  { id: 'REJECTED', label: 'Rejeitado', color: 'bg-red-500', cards: [] },
  { id: 'FINISHED', label: 'Finalizado', color: 'bg-green-500', cards: [] },
  { id: 'WAITING_APPROVAL', label: 'Aguardando Aprovação', color: 'bg-orange-400', cards: [] },
  { id: 'SCHEDULE', label: 'Agendado', color: 'bg-indigo-500', cards: [] },
  { id: 'PUBLISHED', label: 'Publicado', color: 'bg-purple-600', cards: [] },
])

const clients = ref<Client[]>([])
const isModalOpen = ref(false)
const isSubmitting = ref(false)
const postToEdit = ref<Post | null>(null)

// Modal States
const isApprovalModalOpen = ref(false)
const existingApprovalId = ref<string | number | null>(null)
const selectedPostForApproval = ref<Post | null>(null)
const approvalData = ref({
  caption: '',
  arts: [] as ArtItem[],
  isUploading: false,
  isGenerating: false,
  isSending: false,
})

const isReferenceModalOpen = ref(false)
const selectedPostForReference = ref<Post | null>(null)
const referenceData = ref({
  arts: [] as ArtItem[],
  isUploading: false,
  isSaving: false,
})

const feedback = useFeedback()
const approvalsByPostId = ref<Record<string, PostApproval>>({})
const expandedCardsByPostId = ref<Record<string, boolean>>({})

const isArtPreviewModalOpen = ref(false)
const artPreviewMode = ref<ArtPreviewMode>('preview')
const selectedPostForPreview = ref<Post | null>(null)
const selectedApprovalForPreview = ref<PostApproval | null>(null)
const selectedArtPreviewUrls = ref<string[]>([])
const isLoadingArtPreview = ref(false)
const internalReviewAction = ref<'send' | 'reject' | null>(null)
const isRejectReasonModalOpen = ref(false)
const rejectionReason = ref('')
const rejectionReasonError = ref('')
const scheduledAtForApproval = ref('')
const internalRevisionNotes = ref('')

const columnThemeMap: Record<string, ColumnTheme> = {
  DEMAND: {
    clientBadge: 'text-slate-700 bg-slate-100 border border-slate-200',
    primaryButton: 'bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200',
    filledButton: 'bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300',
    softContainer: 'border border-slate-200 bg-slate-50/80',
    softText: 'text-slate-700',
    ghostButton: 'border border-slate-300 text-slate-700 hover:bg-slate-100',
  },
  IN_PRODUCTION: {
    clientBadge: 'text-blue-700 bg-blue-50 border border-blue-200',
    primaryButton: 'bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200',
    filledButton: 'bg-blue-100 hover:bg-blue-200 text-blue-800 border border-blue-300',
    softContainer: 'border border-blue-100 bg-blue-50/70',
    softText: 'text-blue-700',
    ghostButton: 'border border-blue-300 text-blue-700 hover:bg-blue-100',
  },
  FINISHED: {
    clientBadge: 'text-emerald-700 bg-emerald-50 border border-emerald-200',
    primaryButton: 'bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200',
    filledButton: 'bg-emerald-100 hover:bg-emerald-200 text-emerald-800 border border-emerald-300',
    softContainer: 'border border-emerald-100 bg-emerald-50/60',
    softText: 'text-emerald-700',
    ghostButton: 'border border-emerald-300 text-emerald-700 hover:bg-emerald-100',
  },
  WAITING_APPROVAL: {
    clientBadge: 'text-amber-700 bg-amber-50 border border-amber-200',
    primaryButton: 'bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200',
    filledButton: 'bg-amber-100 hover:bg-amber-200 text-amber-800 border border-amber-300',
    softContainer: 'border border-amber-100 bg-amber-50/70',
    softText: 'text-amber-700',
    ghostButton: 'border border-amber-300 text-amber-700 hover:bg-amber-100',
  },
  REJECTED: {
    clientBadge: 'text-red-700 bg-red-50 border border-red-200',
    primaryButton: 'bg-red-50 hover:bg-red-100 text-red-700 border border-red-200',
    filledButton: 'bg-red-100 hover:bg-red-200 text-red-800 border border-red-300',
    softContainer: 'border border-red-100 bg-red-50/70',
    softText: 'text-red-700',
    ghostButton: 'border border-red-300 text-red-700 hover:bg-blue-100',
  },
  SCHEDULE: {
    clientBadge: 'text-indigo-700 bg-indigo-50 border border-indigo-200',
    primaryButton: 'bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200',
    filledButton: 'bg-indigo-100 hover:bg-indigo-200 text-indigo-800 border border-indigo-300',
    softContainer: 'border border-indigo-100 bg-indigo-50/70',
    softText: 'text-indigo-700',
    ghostButton: 'border border-indigo-300 text-indigo-700 hover:bg-indigo-100',
  },
  PUBLISHED: {
    clientBadge: 'text-violet-700 bg-violet-50 border border-violet-200',
    primaryButton: 'bg-violet-50 hover:bg-violet-100 text-violet-700 border border-violet-200',
    filledButton: 'bg-violet-100 hover:bg-violet-200 text-violet-800 border border-violet-300',
    softContainer: 'border border-violet-100 bg-violet-50/70',
    softText: 'text-violet-700',
    ghostButton: 'border border-violet-300 text-violet-700 hover:bg-violet-100',
  },
}

function getColumnTheme(columnId: string): ColumnTheme {
  return columnThemeMap[columnId] ?? (columnThemeMap['DEMAND'] as ColumnTheme)
}

async function fetchInitialData() {
  try {
    const [postsData, clientsData] = await Promise.all([
      postService.getAll(),
      clientService.getAll(),
    ])

    interface ApiResponse<T> {
      data?: T[]
    }
    const posts = Array.isArray(postsData)
      ? postsData
      : (postsData as unknown as ApiResponse<Post>).data || []
    clients.value = Array.isArray(clientsData)
      ? clientsData
      : (clientsData as unknown as ApiResponse<Client>).data || []

    columns.value.forEach((col) => (col.cards = []))
    posts.forEach((post: Post) => {
      const col = columns.value.find((c) => c.id === post.status)
      if (col) col.cards.push(post)
    })

    const approvalList = await approvalService.getAll()
    mapApprovalsByPostId(approvalList)
  } catch (error) {
    console.error('Erro ao carregar dados:', error)
  }
}

const paginatedColumns = computed(() => {
  return columns.value.map((col) => {
    return { ...col, totalCards: col.cards.length }
  })
})

function getClientName(clientId?: number) {
  if (!clientId) return 'Sem cliente'
  return clients.value.find((c) => c.id === clientId)?.name || 'Cliente'
}

function formatDueDate(dateStr?: string) {
  if (!dateStr) return 'Sem prazo'
  const date = new Date(dateStr)
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
}

function isOverdue(post: Post) {
  if (!post.scheduledAt) return false
  return new Date(post.scheduledAt) < new Date() && post.status !== 'PUBLISHED'
}

function getPriorityInfo(post: Post) {
  if (post.isUrgent) return { label: 'URGENTE', className: 'text-red-600 bg-red-50 border-red-100' }
  return { label: 'Normal', className: 'text-gray-600 bg-gray-50 border-gray-100' }
}

async function handleBoardChange(evt: DraggableChangeEvent<Post>, columnId: string) {
  if (evt.added) {
    const post = evt.added.element
    try {
      const payload = {
        ...post,
        status: columnId,
        clientId: Number(getPostClientId(post)),
        userId: Number(getUserOrFallback()),
      }
      await postService.update(post.id!, payload as unknown as Post)
      post.status = columnId
    } catch {
      fetchInitialData()
    }
  }
}

async function handleDeletePost(post: Post) {
  if (!post.id) return
  try {
    await postService.delete(post.id)
    isModalOpen.value = false
    await fetchInitialData()
    feedback.success('Demanda removida.')
  } catch {
    feedback.error('Erro ao remover demanda.')
  }
}

async function handleSavePost(form: PostFormData) {
  isSubmitting.value = true
  try {
    const payload = {
      ...form,
      scheduledAt: form.scheduledAt.length === 16 ? form.scheduledAt + ':00' : form.scheduledAt,
      clientId: Number(form.clientId),
      userId: Number(getUserOrFallback()),
    }
    if (postToEdit.value) await postService.update(postToEdit.value.id!, payload)
    else await postService.create(payload)
    await fetchInitialData()
    isModalOpen.value = false
    feedback.success('Post salvo com sucesso.')
  } catch {
    feedback.error('Erro ao salvar post.')
  } finally {
    isSubmitting.value = false
  }
}

async function openArtPreviewModal(post: Post, mode: ArtPreviewMode = 'preview') {
  const approval = getApprovalByPost(post)
  if (!approval?.artS3Key) {
    feedback.warning('Sem imagem cadastrada.')
    return
  }

  selectedPostForPreview.value = post
  selectedApprovalForPreview.value = approval
  selectedArtPreviewUrls.value = []
  artPreviewMode.value = mode
  scheduledAtForApproval.value = post.scheduledAt ? post.scheduledAt.substring(0, 16) : ''
  internalRevisionNotes.value = approval.internalRevisionNotes || ''
  isArtPreviewModalOpen.value = true
  isLoadingArtPreview.value = true

  try {
    const urls = await mediaService.getArtPreviewUrls(post.id!)
    selectedArtPreviewUrls.value = urls
  } catch {
    feedback.error('Erro ao carregar imagem.')
    isArtPreviewModalOpen.value = false
  } finally {
    isLoadingArtPreview.value = false
  }
}

function closeArtPreviewModal() {
  isArtPreviewModalOpen.value = false
}

async function openApprovalModal(post: Post) {
  selectedPostForApproval.value = post
  isApprovalModalOpen.value = true
  approvalData.value = {
    caption: '',
    arts: [],
    isUploading: false,
    isGenerating: false,
    isSending: false,
  }
  const existing = getApprovalByPost(post)
  if (existing) {
    existingApprovalId.value = existing.id ?? null
    approvalData.value.caption = existing.caption
    const keys = existing.artS3Keys?.length ? existing.artS3Keys : [existing.artS3Key]
    try {
      const urls = await mediaService.getArtPreviewUrls(post.id!)
      approvalData.value.arts = keys.map((s3Key, i) => ({
        s3Key,
        artName: existing.artName,
        previewUrl: urls[i] ?? urls[0] ?? '',
      }))
    } catch {}
  } else {
    existingApprovalId.value = null
  }
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = '' // permite re-selecionar o mesmo arquivo depois de remover
  if (!files.length || !selectedPostForApproval.value) return
  if (approvalData.value.arts.length + files.length > MAX_CAROUSEL_ITEMS) {
    feedback.warning(`Máximo de ${MAX_CAROUSEL_ITEMS} imagens por carrossel.`)
    return
  }
  if (approvalData.value.arts.length + files.length > 1 && files.some((f) => isVideoFileName(f.name))) {
    feedback.warning('Carrossel suporta apenas imagens. Envie vídeos como post único.')
    return
  }
  approvalData.value.isUploading = true
  try {
    for (const file of files) {
      const { uploadUrl, s3Key } = await mediaService.getUploadUrl(
        selectedPostForApproval.value.id!,
        file.name,
        file.type,
      )
      await mediaService.uploadToS3(uploadUrl, file)
      approvalData.value.arts.push({
        s3Key,
        artName: file.name,
        previewUrl: URL.createObjectURL(file),
      })
    }
  } catch {
    feedback.error('Erro no upload.')
  } finally {
    approvalData.value.isUploading = false
  }
}

function removeApprovalArt(index: number) {
  approvalData.value.arts.splice(index, 1)
}

async function saveApproval() {
  if (!selectedPostForApproval.value || !approvalData.value.arts.length) return
  if (
    approvalData.value.arts.length > 1 &&
    approvalData.value.arts.some((a) => isVideoFileName(a.artName) || isVideoFileName(a.s3Key))
  ) {
    feedback.error('Carrossel suporta apenas imagens. Remova os vídeos.')
    return
  }
  approvalData.value.isSending = true
  try {
    const arts = approvalData.value.arts.map(({ s3Key, artName }) => ({ s3Key, artName }))
    const payload = {
      postId: selectedPostForApproval.value.id!,
      artS3Key: arts[0]!.s3Key,
      artName: arts[0]!.artName,
      caption: approvalData.value.caption,
      arts: arts.length > 1 ? arts : undefined,
    }
    if (existingApprovalId.value) await approvalService.update(existingApprovalId.value, payload)
    else await approvalService.create(payload)
    await postService.update(selectedPostForApproval.value.id!, {
      ...selectedPostForApproval.value,
      status: 'FINISHED',
    } as Partial<Post>)
    isApprovalModalOpen.value = false
    await fetchInitialData()
    feedback.success('Aprovação preparada.')
  } catch {
    feedback.error('Erro ao salvar aprovação.')
  } finally {
    approvalData.value.isSending = false
  }
}

async function handleSendApprovalFromInternalReview() {
  const post = selectedPostForPreview.value
  if (!post?.id || !scheduledAtForApproval.value) {
    feedback.warning('Informe a data programada.')
    return
  }
  internalReviewAction.value = 'send'
  try {
    await approvalService.internalApprove(post.id, {
      scheduledAt: scheduledAtForApproval.value,
      internalRevisionNotes: internalRevisionNotes.value,
    })
    await handleSendApprovalFromFinished(post, false)
    closeArtPreviewModal()
    await fetchInitialData()
  } catch {
    feedback.error('Erro na aprovação interna.')
  } finally {
    internalReviewAction.value = null
  }
}

async function handleSendApprovalFromFinished(post: Post, _isResend = false) {
  const approval = getApprovalByPost(post)
  if (!approval?.artS3Key) return false
  try {
    const keys = approval.artS3Keys?.length ? approval.artS3Keys : [approval.artS3Key]
    if (keys.length > 1 && keys.some((k) => isVideoFileName(k))) {
      feedback.error('Carrossel suporta apenas imagens. Edite a aprovação e remova os vídeos.')
      return false
    }
    const arts = keys.map((s3Key) => ({ s3Key, artName: approval.artName }))
    const completion = await mediaService.completeUpload(post.id!, arts)
    if (completion.webhookDispatched) feedback.success('Enviado ao cliente.')
    return true
  } catch {
    feedback.error('Erro ao enviar.')
    return false
  }
}

async function confirmInternalRejection() {
  const post = selectedPostForPreview.value
  if (!post?.id || rejectionReason.value.length < 5) return
  internalReviewAction.value = 'reject'
  try {
    await approvalService.rejectInternalApproval(post.id, {
      rejectionReason: rejectionReason.value,
    })
    closeArtPreviewModal()
    await fetchInitialData()
    feedback.success('Post rejeitado.')
  } catch {
    feedback.error('Erro ao rejeitar.')
  } finally {
    internalReviewAction.value = null
  }
}

// Reference Modal Logic
async function openReferenceUpload(post: Post) {
  selectedPostForReference.value = post
  referenceData.value = {
    arts: [],
    isUploading: false,
    isSaving: false,
  }
  if (post.id && post.referenceImageS3Key) {
    try {
      const keys = post.referenceImageS3Keys?.length
        ? post.referenceImageS3Keys
        : [post.referenceImageS3Key]
      const urls = await mediaService.getReferencePreviewUrls(post.id)
      referenceData.value.arts = keys.map((s3Key, i) => ({
        s3Key,
        artName: 'Referência Atual',
        previewUrl: urls[i] ?? urls[0] ?? '',
      }))
    } catch {}
  }
  isReferenceModalOpen.value = true
}

async function handleReferenceFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = '' // permite re-selecionar o mesmo arquivo depois de remover
  if (!files.length || !selectedPostForReference.value) return
  if (referenceData.value.arts.length + files.length > MAX_CAROUSEL_ITEMS) {
    feedback.warning(`Máximo de ${MAX_CAROUSEL_ITEMS} arquivos de referência.`)
    return
  }
  referenceData.value.isUploading = true
  try {
    for (const file of files) {
      const { uploadUrl, s3Key } = await mediaService.getReferenceUploadUrl(
        selectedPostForReference.value.id!,
        file.name,
        file.type,
      )
      await mediaService.uploadToS3(uploadUrl, file)
      referenceData.value.arts.push({
        s3Key,
        artName: file.name,
        previewUrl: URL.createObjectURL(file),
      })
    }
  } catch {
    feedback.error('Erro no upload.')
  } finally {
    referenceData.value.isUploading = false
  }
}

function removeReferenceArt(index: number) {
  referenceData.value.arts.splice(index, 1)
}

async function saveReference() {
  if (!selectedPostForReference.value?.id || !referenceData.value.arts.length) return
  referenceData.value.isSaving = true
  try {
    await postService.updateReference(
      selectedPostForReference.value.id,
      referenceData.value.arts.map((a) => a.s3Key),
    )
    await fetchInitialData()
    isReferenceModalOpen.value = false
    feedback.success('Referência salva.')
  } catch {
    feedback.error('Erro ao salvar.')
  } finally {
    referenceData.value.isSaving = false
  }
}

async function viewReference(post: Post) {
  if (!post.id || !post.referenceImageS3Key) return
  isLoadingArtPreview.value = true
  selectedPostForPreview.value = post
  selectedApprovalForPreview.value = {
    postId: post.id,
    artS3Key: post.referenceImageS3Key,
    artName: 'Referencia.jpg',
    caption: 'Referência visual.',
    status: 'PENDING',
  } as PostApproval
  artPreviewMode.value = 'preview'
  isArtPreviewModalOpen.value = true
  try {
    const urls = await mediaService.getReferencePreviewUrls(post.id)
    selectedArtPreviewUrls.value = urls
  } catch {
    feedback.error('Erro ao carregar referência.')
    closeArtPreviewModal()
  } finally {
    isLoadingArtPreview.value = false
  }
}

function isCardExpanded(post: Post) {
  return Boolean(post.id && expandedCardsByPostId.value[post.id])
}
function toggleCardExpanded(post: Post) {
  if (post.id) expandedCardsByPostId.value[post.id] = !expandedCardsByPostId.value[post.id]
}
function mapApprovalsByPostId(list: PostApproval[]) {
  approvalsByPostId.value = Object.fromEntries(
    list.filter((a) => a.postId).map((a) => [String(a.postId), a]),
  )
}
function getApprovalByPost(post: Post) {
  return post.id ? approvalsByPostId.value[post.id] : null
}
function getApprovalStatusLabel(post: Post) {
  const app = getApprovalByPost(post)
  if (!app) return { text: 'Não preparada', className: 'text-amber-700' }
  if (post.status === 'REJECTED' || app.status === 'REJECT')
    return { text: 'Rejeitado', className: 'text-red-700' }
  if (app.whatsappSentAt) return { text: 'Enviada', className: 'text-emerald-700' }
  return { text: 'Pronta', className: 'text-blue-700' }
}
function getUserOrFallback(): number {
  return Number(getCurrentUserId()) || 1
}

const search = ref('')

async function generateAICaption() {
  const post = selectedPostForApproval.value
  if (!post?.id) return
  approvalData.value.isGenerating = true
  try {
    const result = await postService.generateCaption(
      post.id,
      approvalData.value.arts[0]?.s3Key,
    )
    approvalData.value.caption = result.caption
  } catch {
    feedback.error('Erro ao gerar legenda com IA.')
  } finally {
    approvalData.value.isGenerating = false
  }
}

onMounted(() => {
  fetchInitialData()
})
</script>

<template>
  <AppLayout v-model:search="search" topbar-placeholder="Buscar demandas...">
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Board de Produção</h1>
        <p class="text-gray-500 mt-1">Fluxo operacional North Produções.</p>
      </div>
      <Button class="gap-2" @click="((isModalOpen = true), (postToEdit = null))"
        ><Plus class="w-4 h-4" /> Nova Demanda</Button
      >
    </div>

    <div class="flex gap-4 overflow-x-auto pb-4 h-[calc(100vh-220px)]">
      <div
        v-for="(col, colIdx) in paginatedColumns"
        :key="col.id"
        class="flex flex-col w-[308px] shrink-0 bg-gray-50/50 rounded-2xl border border-gray-100 overflow-hidden h-full"
      >
        <div :class="['h-1.5 w-full', col.color]" />
        <div class="p-3 flex flex-col flex-1 overflow-hidden">
          <div class="flex items-center gap-2 mb-4 px-1">
            <span class="text-sm font-bold text-gray-700 uppercase">{{ col.label }}</span
            ><span
              class="text-xs font-bold bg-white text-gray-400 border border-gray-100 rounded-full px-2 py-0.5"
              >{{ col.totalCards }}</span
            >
          </div>
          <draggable
            v-model="columns[colIdx]!.cards"
            group="posts"
            item-key="id"
            class="flex-1 space-y-3 overflow-y-auto pr-1 custom-scrollbar min-h-0"
            @change="(evt: any) => handleBoardChange(evt, col.id)"
          >
            <template #item="{ element: card }">
              <BoardCard
                :card="card"
                :column-id="String(col.id)"
                :column-theme="getColumnTheme(String(col.id))"
                :is-expanded="isCardExpanded(card)"
                :approval="getApprovalByPost(card)"
                :client-name="getClientName(Number(getPostClientId(card)))"
                :overdue="isOverdue(card)"
                :priority-info="getPriorityInfo(card)"
                :due-date-label="formatDueDate(card.scheduledAt)"
                :responsible-label="'Equipe North'"
                :post-type-label="'Social Media'"
                :approval-status="getApprovalStatusLabel(card)"
                :rejection-message="getApprovalByPost(card)?.rejectionReason || 'Rejeitado'"
                :is-sending-approval="false"
                @preview="openArtPreviewModal(card)"
                @toggle-expand="toggleCardExpanded(card)"
                @edit="((postToEdit = card), (isModalOpen = true))"
                @prepare-approval="openApprovalModal(card)"
                @internal-review="openArtPreviewModal(card, 'internal-review')"
                @add-reference="openReferenceUpload(card)"
                @view-reference="viewReference(card)"
                @resend="handleSendApprovalFromFinished(card, true)"
              />
            </template>
          </draggable>
        </div>
      </div>
    </div>

    <PostModal
      :is-open="isModalOpen"
      :post-to-edit="postToEdit"
      :clients="clients"
      :is-submitting="isSubmitting"
      :field-errors="{}"
      @close="isModalOpen = false"
      @save="handleSavePost"
      @delete="handleDeletePost"
    />
    <ApprovalModal
      :is-open="isApprovalModalOpen"
      :post="selectedPostForApproval"
      :existing-approval-id="existingApprovalId"
      :data="approvalData"
      @close="isApprovalModalOpen = false"
      @file-select="handleFileSelect"
      @remove-art="removeApprovalArt"
      @generate-caption="generateAICaption"
      @save="saveApproval"
      @update:caption="approvalData.caption = $event"
    />
    <ArtPreviewModal
      :is-open="isArtPreviewModalOpen"
      :mode="artPreviewMode"
      :post="selectedPostForPreview"
      :approval="selectedApprovalForPreview"
      :art-urls="selectedArtPreviewUrls"
      :is-loading="isLoadingArtPreview"
      :internal-review-action="internalReviewAction"
      :rejection-reason="rejectionReason"
      :rejection-reason-error="rejectionReasonError"
      :is-reject-reason-modal-open="isRejectReasonModalOpen"
      :client-name="getClientName(Number(getPostClientId(selectedPostForPreview || ({} as Post))))"
      v-model:scheduled-at="scheduledAtForApproval"
      v-model:internal-revision-notes="internalRevisionNotes"
      @close="closeArtPreviewModal"
      @send-to-client="handleSendApprovalFromInternalReview"
      @open-reject-modal="isRejectReasonModalOpen = true"
      @close-reject-modal="isRejectReasonModalOpen = false"
      @confirm-rejection="confirmInternalRejection"
      @update:rejection-reason="rejectionReason = $event"
    />
    <ReferenceModal
      :is-open="isReferenceModalOpen"
      :post="selectedPostForReference"
      :data="referenceData"
      @close="isReferenceModalOpen = false"
      @file-select="handleReferenceFileSelect"
      @remove-art="removeReferenceArt"
      @save="saveReference"
    />
  </AppLayout>
</template>
