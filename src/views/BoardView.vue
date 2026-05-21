<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MoreHorizontal, Plus, ChevronDown } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import { postService, type Post, type PostStatus, getPostId, getPostClientId } from '@/services/postService'
import { clientService, type Client } from '@/services/clientService'
import { approvalService, type PostApproval } from '@/services/approvalService'
import { mediaService } from '@/services/mediaService'
import { getCurrentUserId } from '@/lib/api'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import draggable from 'vuedraggable'
import { z } from 'zod'

// Components
import BoardCard from '@/components/board/BoardCard.vue'
import PostModal from '@/components/board/PostModal.vue'
import ApprovalModal from '@/components/board/ApprovalModal.vue'
import ArtPreviewModal from '@/components/board/ArtPreviewModal.vue'

type BoardColumn = {
  id: PostStatus
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
const fieldErrors = ref<Record<string, string>>({})
const postToEdit = ref<Post | null>(null)

// AI Approval Modal State
const isApprovalModalOpen = ref(false)
const existingApprovalId = ref<string | number | null>(null)
const selectedPostForApproval = ref<Post | null>(null)
const approvalData = ref({
  caption: '',
  artS3Key: '',
  artName: '',
  artPreviewUrl: '',
  isUploading: false,
  isGenerating: false,
  isSending: false
})
const feedback = useFeedback()
const approvalsByPostId = ref<Record<string, PostApproval>>({})
const sendingApprovalByPostId = ref<Record<string, boolean>>({})
const expandedCardsByPostId = ref<Record<string, boolean>>({})
const artPreviewUrlsByPostId = ref<Record<string, string>>({})
const isArtPreviewModalOpen = ref(false)
const artPreviewMode = ref<ArtPreviewMode>('preview')
const selectedPostForPreview = ref<Post | null>(null)
const selectedApprovalForPreview = ref<PostApproval | null>(null)
const selectedArtPreviewUrl = ref('')
const isLoadingArtPreview = ref(false)
const internalReviewAction = ref<'send' | 'reject' | null>(null)
const isRejectReasonModalOpen = ref(false)
const rejectionReason = ref('')
const rejectionReasonError = ref('')
const scheduledAtForApproval = ref('')
const internalRevisionNotes = ref('')

const columnThemeMap: Record<PostStatus, ColumnTheme> = {
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

function getColumnTheme(columnId: PostStatus): ColumnTheme {
  return columnThemeMap[columnId] || columnThemeMap.DEMAND
}

function getColumnLabel(columnId: PostStatus) {
  return columns.value.find((column) => column.id === columnId)?.label || columnId
}

function getResponsibleLabel(post: Post) {
  return post.userId ? `U${post.userId}` : 'Não definido'
}

function getPostTypeLabel(post: Post) {
  const raw = post as Record<string, unknown>
  const typeCandidate = raw.postType ?? raw.type ?? raw.format ?? raw.mediaType
  if (typeof typeCandidate === 'string' && typeCandidate.trim()) return typeCandidate
  return 'Post'
}

function formatDueDate(dateString?: string) {
  if (!dateString) return 'Sem prazo'
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return 'Sem prazo'
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function isOverdue(post: Post) {
  if (!post.scheduledAt) return false
  const due = new Date(post.scheduledAt).getTime()
  if (Number.isNaN(due)) return false
  return due < Date.now()
}

function getPriorityInfo(post: Post) {
  if (!post.scheduledAt) {
    return {
      label: 'MÉDIA',
      className: 'text-amber-700 bg-amber-50 border border-amber-200'
    }
  }

  const due = new Date(post.scheduledAt).getTime()
  if (Number.isNaN(due)) {
    return {
      label: 'MÉDIA',
      className: 'text-amber-700 bg-amber-50 border border-amber-200'
    }
  }

  const diffDays = (due - Date.now()) / (1000 * 60 * 60 * 24)
  if (diffDays <= 2) {
    return {
      label: 'ALTA',
      className: 'text-red-700 bg-red-50 border border-red-200'
    }
  }
  if (diffDays <= 5) {
    return {
      label: 'MÉDIA',
      className: 'text-amber-700 bg-amber-50 border border-amber-200'
    }
  }
  return {
    label: 'BAIXA',
    className: 'text-emerald-700 bg-emerald-50 border border-emerald-200'
  }
}

function getClientName(clientId: string | number | undefined) {
  if (!clientId) return '—'
  const client = clients.value.find(c => String(c.id) === String(clientId))
  return client ? client.name : `ID: ${clientId}`
}

async function loadArtPreviewUrl(post: Post) {
  const postId = getPostId(post)
  if (!postId || !post.id) return ''

  if (artPreviewUrlsByPostId.value[postId]) {
    return artPreviewUrlsByPostId.value[postId]
  }

  const approval = getApprovalByPost(post)
  if (!approval?.artS3Key) return ''

  const response = await mediaService.getArtUrl(post.id)
  const mediaUrl = response.mediaUrl || ''
  if (mediaUrl) {
    artPreviewUrlsByPostId.value = {
      ...artPreviewUrlsByPostId.value,
      [postId]: mediaUrl
    }
  }
  return mediaUrl
}

async function openArtPreviewModal(post: Post, mode: ArtPreviewMode = 'preview') {
  const approval = getApprovalByPost(post)
  if (!approval?.artS3Key) {
    feedback.warning('Este post ainda não possui imagem adicionada.')
    return
  }

  selectedPostForPreview.value = post
  selectedApprovalForPreview.value = approval
  selectedArtPreviewUrl.value = ''
  artPreviewMode.value = mode

  // Inicializa agendamento e notas
  scheduledAtForApproval.value = post.scheduledAt ? post.scheduledAt.substring(0, 16) : ''
  internalRevisionNotes.value = approval.internalRevisionNotes || ''

  isArtPreviewModalOpen.value = true
  isLoadingArtPreview.value = true

  try {
    selectedArtPreviewUrl.value = await loadArtPreviewUrl(post)
  } catch (e: unknown) {
    feedback.error(`Erro ao carregar imagem: ${getErrorMessage(e, 'Tente novamente.')}`)
  } finally {
    isLoadingArtPreview.value = false
  }
}

function closeArtPreviewModal() {
  isArtPreviewModalOpen.value = false
  isRejectReasonModalOpen.value = false
  selectedPostForPreview.value = null
  selectedApprovalForPreview.value = null
  selectedArtPreviewUrl.value = ''
  rejectionReason.value = ''
  rejectionReasonError.value = ''
  internalReviewAction.value = null
}

async function openApprovalModal(post: Post) {
  selectedPostForApproval.value = post
  existingApprovalId.value = null
  approvalData.value = {
    caption: '',
    artS3Key: '',
    artName: '',
    artPreviewUrl: '',
    isUploading: false,
    isGenerating: false,
    isSending: false
  }
  isApprovalModalOpen.value = true

  const existing = getApprovalByPost(post)
  if (post.id && existing) {
    existingApprovalId.value = existing.id || null
    approvalData.value.caption = existing.caption || ''
    approvalData.value.artS3Key = existing.artS3Key || ''
    approvalData.value.artName = existing.artName || ''

    try {
      approvalData.value.artPreviewUrl = await loadArtPreviewUrl(post)
    } catch (err) {
      console.warn('Erro ao carregar preview da arte existente:', err)
    }
  }
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !selectedPostForApproval.value) return

  const postId = selectedPostForApproval.value.id

  approvalData.value.isUploading = true
  try {
    const { uploadUrl, s3Key } = await mediaService.getUploadUrl(postId!, file.name, file.type)
    await mediaService.uploadToS3(uploadUrl, file)
    approvalData.value.artS3Key = s3Key
    approvalData.value.artName = file.name
    approvalData.value.artPreviewUrl = URL.createObjectURL(file)
  } catch (e: unknown) {
    feedback.error(`Erro no upload: ${getErrorMessage(e, 'Tente novamente.')}`)
  } finally {
    approvalData.value.isUploading = false
  }
}

async function generateAICaption() {
  if (!selectedPostForApproval.value?.id) return
  approvalData.value.isGenerating = true
  
  try {
    const res = await postService.generateCaption(selectedPostForApproval.value.id, approvalData.value.artS3Key)
    approvalData.value.caption = res.caption
    feedback.success('Legenda gerada com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao gerar legenda: ${getErrorMessage(e, 'Tente novamente.')}`)
  } finally {
    approvalData.value.isGenerating = false
  }
}

async function saveApproval() {
  if (!selectedPostForApproval.value) return
  if (!approvalData.value.artS3Key) {
    feedback.warning('Faça o upload da arte antes de salvar.')
    return
  }

  approvalData.value.isSending = true
  try {
    const payload = {
      postId: selectedPostForApproval.value.id!,
      artS3Key: approvalData.value.artS3Key,
      artName: approvalData.value.artName,
      caption: approvalData.value.caption
    }

    if (existingApprovalId.value) {
      await approvalService.update(existingApprovalId.value, payload)
    } else {
      await approvalService.create(payload)
    }
    await movePostToFinished(selectedPostForApproval.value)
    
    isApprovalModalOpen.value = false
    await fetchInitialData()
    feedback.success('Aprovação salva e demanda movida para Finalizado.')
  } catch (e: unknown) {
    feedback.error(`Erro ao salvar aprovação: ${getErrorMessage(e, 'Tente novamente.')}`)
  } finally {
    approvalData.value.isSending = false
  }
}

function isCardExpanded(post: Post) {
  const postId = getPostId(post)
  return Boolean(postId && expandedCardsByPostId.value[postId])
}

function toggleCardExpanded(post: Post) {
  const postId = getPostId(post)
  if (!postId) return
  expandedCardsByPostId.value = {
    ...expandedCardsByPostId.value,
    [postId]: !expandedCardsByPostId.value[postId]
  }
}

async function movePostToFinished(post: Post) {
  if (!post.id) throw new Error('Demanda sem ID.')
  const clientId = getPostClientId(post)
  if (!clientId) throw new Error('Demanda sem cliente associado.')

  const payload = {
    id: post.id,
    title: post.title,
    theme: post.theme,
    objective: post.objective,
    status: 'FINISHED',
    scheduledAt: post.scheduledAt,
    clientId,
    userId: getUserOrFallback()
  }
  await postService.update(post.id, payload as unknown as Post)
}

function mapApprovalsByPostId(list: PostApproval[]) {
  const entries = list
    .filter((approval) => approval.post?.id !== undefined && approval.post?.id !== null)
    .map((approval) => [String(approval.post!.id), approval] as const)
  approvalsByPostId.value = Object.fromEntries(entries)
}

function getApprovalByPost(post: Post) {
  const postId = getPostId(post)
  if (!postId) return null
  return approvalsByPostId.value[postId] || null
}

function getApprovalStatusLabel(post: Post) {
  const approval = getApprovalByPost(post)
  if (!approval) {
    return { text: 'Não preparada', className: 'text-amber-700' }
  }
  if (post.status === 'REJECTED' || approval.status === 'REJECT') {
    return { text: 'Rejeitado', className: 'text-red-700' }
  }
  if (approval.whatsappResponseText?.trim()) {
    return { text: 'Cliente respondeu', className: 'text-violet-700' }
  }
  if (approval.whatsappSentAt) {
    return { text: `Enviada`, className: 'text-emerald-700' }
  }
  return { text: 'Pronta para envio', className: 'text-blue-700' }
}

function getRejectionMessage(post: Post) {
  const approval = getApprovalByPost(post)
  const reason = approval?.rejectionReason?.trim()
  return reason || 'Rejeitado na aprovação interna.'
}

function isSendingApproval(post: Post) {
  const postId = getPostId(post)
  return Boolean(postId && sendingApprovalByPostId.value[postId])
}

async function handleSendApprovalFromFinished(post: Post, isResend = false) {
  const postId = getPostId(post)
  if (!postId || !post.id) return false
  if (isSendingApproval(post)) return false

  const approval = getApprovalByPost(post)
  if (!approval?.artS3Key || !approval.artName) {
    feedback.warning('Esta demanda ainda não possui aprovação preparada. Prepare a aprovação primeiro.')
    openApprovalModal(post)
    return false
  }

  sendingApprovalByPostId.value = { ...sendingApprovalByPostId.value, [postId]: true }
  try {
    const completion = await mediaService.completeUpload(post.id, approval.artS3Key, approval.artName)
    try {
      const refreshedApproval = await approvalService.getByPostId(post.id)
      approvalsByPostId.value = { ...approvalsByPostId.value, [postId]: refreshedApproval }
    } catch {}

    if (completion.webhookDispatched) {
      feedback.success(isResend ? 'Aprovação reenviada ao cliente.' : 'Aprovação enviada ao cliente.')
    } else {
      feedback.warning('Aprovação processada, mas o webhook do n8n não foi disparado.')
    }
    return true
  } catch (e: unknown) {
    feedback.error(`Erro ao ${isResend ? 'reenviar' : 'enviar'} aprovação: ${getErrorMessage(e, 'Tente novamente.')}`)
    return false
  } finally {
    sendingApprovalByPostId.value = { ...sendingApprovalByPostId.value, [postId]: false }
  }
}

async function handleSendApprovalFromInternalReview() {
  const post = selectedPostForPreview.value
  if (!post?.id) return

  if (!scheduledAtForApproval.value) {
    feedback.warning('Informe a data e hora programada para postagem.')
    return
  }

  internalReviewAction.value = 'send'
  try {
    // 1. Salva a aprovação interna com agendamento e notas. 
    // O backend agora mudará o status para SCHEDULE e o Scheduler fará o envio na hora certa.
    await approvalService.internalApprove(post.id, {
      scheduledAt: scheduledAtForApproval.value.length === 16 ? scheduledAtForApproval.value + ":00" : scheduledAtForApproval.value,
      internalRevisionNotes: internalRevisionNotes.value
    })

    feedback.success('Post agendado com sucesso! O envio ao n8n ocorrerá na data programada.')
    closeArtPreviewModal()
    await fetchInitialData()
  } catch (e: unknown) {
    feedback.error(`Erro na aprovação interna: ${getErrorMessage(e)}`)
  } finally {
    internalReviewAction.value = null
  }
}

async function confirmInternalRejection() {
  const post = selectedPostForPreview.value
  if (!post?.id) return

  const reason = rejectionReason.value.trim()
  if (reason.length < 5) {
    rejectionReasonError.value = 'Informe uma justificativa com pelo menos 5 caracteres.'
    return
  }

  internalReviewAction.value = 'reject'
  try {
    await approvalService.rejectInternalApproval(post.id, { rejectionReason: reason })
    closeArtPreviewModal()
    await fetchInitialData()
    feedback.success('Post rejeitado e movido para Rejeitado.')
  } catch (e: unknown) {
    feedback.error(`Erro ao rejeitar aprovação interna: ${getErrorMessage(e, 'Tente novamente.')}`)
  } finally {
    internalReviewAction.value = null
  }
}

const postSchema = z.object({
  clientId: z.union([z.string(), z.number()]).refine(v => String(v).length > 0, 'Selecione um cliente'),
  title: z.string().min(3, 'Título muito curto'),
  theme: z.string().min(3, 'Tema obrigatório'),
  objective: z.string().min(5, 'Descreva o objetivo'),
  scheduledAt: z.string().min(10, 'Data inválida')
})

const search = ref('')

const itemsPerPage = 10
const columnPages = ref<Record<string, number>>({ SCHEDULE: 1, FINISHED: 1 })

async function fetchInitialData() {
  try {
    const [postsData, clientsData] = await Promise.all([postService.getAll(), clientService.getAll()])
    
    interface ApiResponse<T> { data?: T[] }
    const posts = Array.isArray(postsData) ? postsData : ((postsData as unknown as ApiResponse<Post>).data || [])
    clients.value = Array.isArray(clientsData) ? clientsData : ((clientsData as unknown as ApiResponse<Client>).data || [])
    
    columns.value.forEach(col => col.cards = [])
    posts.forEach((post: Post) => {
      const col = columns.value.find(c => c.id === post.status)
      if (col) col.cards.push(post)
      else columns.value[0]?.cards.push(post)
    })

    try {
      const approvalsData = await approvalService.getAll()
      mapApprovalsByPostId(Array.isArray(approvalsData) ? approvalsData : [])
    } catch {
      approvalsByPostId.value = {}
    }
  } catch (error) {
    console.error('Failed to load board data:', error)
  }
}

const filteredColumns = computed(() => {
  if (!search.value) return columns.value
  const term = search.value.toLowerCase()
  return columns.value.map(col => ({
    ...col,
    cards: col.cards.filter(card => {
      const titleMatches = card.title.toLowerCase().includes(term)
      const cid = getPostClientId(card)
      const clientMatches = getClientName(cid || '-').toLowerCase().includes(term)
      return titleMatches || clientMatches
    })
  }))
})

const paginatedColumns = computed(() => {
  return filteredColumns.value.map(col => {
    const isPaginatable = col.id === 'SCHEDULE' || col.id === 'FINISHED'
    const totalCards = col.cards.length
    
    if (isPaginatable && !search.value) {
      const page = columnPages.value[col.id] || 1
      const start = (page - 1) * itemsPerPage
      const end = start + itemsPerPage
      return {
        ...col,
        cards: col.cards.slice(start, end),
        totalCards,
        totalPages: Math.ceil(totalCards / itemsPerPage),
        currentPage: page
      }
    }
    
    return { ...col, totalCards, totalPages: 1, currentPage: 1 }
  })
})

async function handleBoardChange(evt: { added?: { element: Post } }, columnId: string) {
  if (evt.added) {
    const post = evt.added.element
    const clientId = getPostClientId(post)
    if (!clientId) {
      feedback.error('Erro: o post não possui cliente associado. O board será recarregado.')
      fetchInitialData()
      return
    }

    try {
      const payload = {
        id: post.id,
        title: post.title,
        theme: post.theme,
        objective: post.objective,
        status: columnId,
        scheduledAt: post.scheduledAt,
        clientId: Number(clientId),
        userId: getUserOrFallback()
      }
      await postService.update(post.id!, payload as unknown as Post)
      post.status = columnId
    } catch (error) {
      console.error('Erro na API ao mudar status:', error)
      fetchInitialData()
    }
  }
}

function openAddModal(status: string = 'DEMAND') {
  postToEdit.value = null
  isModalOpen.value = true
}

function openEditModal(post: Post) {
  postToEdit.value = post
  isModalOpen.value = true
}

async function handleSavePost(form: any) {
  fieldErrors.value = {}
  const result = postSchema.safeParse(form)
  
  if (!result.success) {
    result.error.issues.forEach(issue => {
      const key = issue.path[0] as string
      fieldErrors.value[key] = issue.message
    })
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      id: postToEdit.value?.id,
      title: form.title,
      theme: form.theme,
      objective: form.objective,
      status: form.status,
      scheduledAt: form.scheduledAt.length === 16 ? form.scheduledAt + ":00" : form.scheduledAt,
      clientId: Number(form.clientId),
      userId: getUserOrFallback()
    }

    if (postToEdit.value) {
      await postService.update(postToEdit.value.id!, payload as unknown as Post)
    } else {
      await postService.create(payload as unknown as Post)
    }

    await fetchInitialData()
    isModalOpen.value = false
    feedback.success(postToEdit.value ? 'Demanda atualizada com sucesso.' : 'Demanda criada com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao salvar post: ${getErrorMessage(e)}`)
  } finally {
    isSubmitting.value = false
  }
}

async function handleDeletePost(post: Post) {
  const confirmed = await feedback.confirm({
    title: 'Excluir demanda',
    message: `Excluir "${post.title}"? Esta ação não pode ser desfeita.`,
    confirmText: 'Excluir',
    tone: 'danger',
  })
  if (!confirmed) return

  try {
    await postService.delete(post.id!)
    await fetchInitialData()
    if (isModalOpen.value) isModalOpen.value = false
    feedback.success('Demanda excluída com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao excluir: ${getErrorMessage(e)}`)
  }
}

function prevPage(colId: string) {
  if (columnPages.value[colId] && columnPages.value[colId] > 1) {
    columnPages.value[colId]--
  }
}

function nextPage(colId: string, totalPages: number) {
  if (columnPages.value[colId] && columnPages.value[colId] < totalPages) {
    columnPages.value[colId]++
  } else if (!columnPages.value[colId]) {
    columnPages.value[colId] = 2
  }
}

const route = useRoute()

function getUserOrFallback(): number {
  const id = getCurrentUserId()
  if (id) return id
  const stored = localStorage.getItem('userId') || sessionStorage.getItem('userId')
  return stored ? Number(stored) : 1
}

function getQueryString(value: unknown) {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : ''
  return typeof value === 'string' ? value : ''
}

function openModalFromRoute() {
  if (isModalOpen.value) return
  const clientId = getQueryString(route.query.clientId)
  if (clientId && clients.value.length > 0) {
    const clientExists = clients.value.some(c => String(c.id) === clientId)
    if (clientExists) {
      postToEdit.value = null
      isModalOpen.value = true
      return
    }
  }
  if (route.query.new) openAddModal('DEMAND')
}

watch(() => [route.query.clientId, route.query.new], () => openModalFromRoute())
onMounted(async () => {
  await fetchInitialData()
  openModalFromRoute()
})
</script>

<template>
  <AppLayout v-model:search="search" topbar-placeholder="Buscar demandas por título ou cliente...">
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Board de Produção</h1>
        <p class="text-gray-500 mt-1">Gerencie o fluxo criativo do ateliê em tempo real.</p>
      </div>
      <div class="flex items-center gap-3">
        <Button class="gap-2" @click="openAddModal()">
          <Plus class="w-4 h-4" />
          Nova Demanda
        </Button>
      </div>
    </div>

    <div class="flex gap-4 overflow-x-auto overflow-y-hidden pb-4 h-[calc(100vh-220px)]">
      <div
        v-for="(col, colIdx) in paginatedColumns"
        :key="col.id"
        class="flex flex-col w-[308px] shrink-0 bg-gray-50/50 rounded-2xl border border-gray-100 overflow-hidden h-full max-h-full"
      >
        <div :class="['h-1.5 w-full shrink-0', col.color]" />
        <div class="p-3 flex flex-col flex-1 overflow-hidden">
          <div class="flex items-center justify-between mb-4 px-1 shrink-0">
            <div class="flex items-center gap-2">
              <span class="text-sm font-bold text-gray-700 uppercase tracking-wider">{{ col.label }}</span>
              <span class="text-xs font-bold bg-white text-gray-400 border border-gray-100 rounded-full px-2 py-0.5 shadow-sm">
                {{ col.totalCards }}
              </span>
            </div>
            <button class="p-1 hover:bg-white rounded-lg transition-colors"><MoreHorizontal class="w-4 h-4 text-gray-400" /></button>
          </div>

          <draggable
            v-if="columns[colIdx]"
            v-model="columns[colIdx].cards"
            group="posts"
            item-key="id"
            :disabled="!!search || col.totalPages > 1"
            @change="(evt: { added?: { element: Post } }) => handleBoardChange(evt, col.id)"
            class="flex-1 space-y-3 overflow-y-auto pr-1 custom-scrollbar min-h-0"
            ghost-class="opacity-50"
            drag-class="rotate-2"
          >
            <template #item="{ element: card }">
              <BoardCard
                :card="card"
                :column-id="col.id"
                :column-theme="getColumnTheme(col.id)"
                :is-expanded="isCardExpanded(card)"
                :approval="getApprovalByPost(card)"
                :client-name="getClientName(getPostClientId(card) ?? undefined)"
                :overdue="isOverdue(card)"
                :priority-info="getPriorityInfo(card)"
                :due-date-label="formatDueDate(card.scheduledAt)"
                :responsible-label="getResponsibleLabel(card)"
                :post-type-label="getPostTypeLabel(card)"
                :approval-status="getApprovalStatusLabel(card)"
                :rejection-message="getRejectionMessage(card)"
                :is-sending-approval="isSendingApproval(card)"
                @preview="openArtPreviewModal(card)"
                @toggle-expand="toggleCardExpanded(card)"
                @edit="openEditModal(card)"
                @prepare-approval="openApprovalModal(card)"
                @internal-review="openArtPreviewModal(card, 'internal-review')"
              />
            </template>
          </draggable>

          <div v-if="col.totalPages > 1" class="flex items-center justify-between px-1 py-2 border-t border-gray-100 bg-white/50 shrink-0 mt-2 rounded-lg">
            <button @click="prevPage(col.id)" :disabled="col.currentPage === 1" class="p-1 rounded hover:bg-white disabled:opacity-30 transition-colors">
              <ChevronDown class="w-4 h-4 text-gray-400 rotate-90" />
            </button>
            <span class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Página {{ col.currentPage }} de {{ col.totalPages }}</span>
            <button @click="nextPage(col.id, col.totalPages)" :disabled="col.currentPage === col.totalPages" class="p-1 rounded hover:bg-white disabled:opacity-30 transition-colors">
              <ChevronDown class="w-4 h-4 text-gray-400 -rotate-90" />
            </button>
          </div>

          <button
            @click="openAddModal(col.id)"
            class="w-full mt-3 py-2 text-xs font-semibold text-gray-400 border border-dashed border-gray-200 rounded-xl hover:border-primary/30 hover:text-primary hover:bg-white transition-all flex items-center justify-center gap-1 shrink-0"
          >
            <Plus class="w-3.5 h-3.5" />
            Adicionar na coluna
          </button>
        </div>
      </div>
    </div>

    <PostModal
      :is-open="isModalOpen"
      :post-to-edit="postToEdit"
      :clients="clients"
      :is-submitting="isSubmitting"
      :field-errors="fieldErrors"
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
      @generate-caption="generateAICaption"
      @save="saveApproval"
      @update:caption="approvalData.caption = $event"
    />

    <ArtPreviewModal
      :is-open="isArtPreviewModalOpen"
      :mode="artPreviewMode"
      :post="selectedPostForPreview"
      :approval="selectedApprovalForPreview"
      :art-url="selectedArtPreviewUrl"
      :is-loading="isLoadingArtPreview"
      :internal-review-action="internalReviewAction"
      :rejection-reason="rejectionReason"
      :rejection-reason-error="rejectionReasonError"
      :is-reject-reason-modal-open="isRejectReasonModalOpen"
      :client-name="getClientName(getPostClientId(selectedPostForPreview || {} as Post) ?? undefined)"
      v-model:scheduled-at="scheduledAtForApproval"
      v-model:internal-revision-notes="internalRevisionNotes"
      @close="closeArtPreviewModal"
      @send-to-client="handleSendApprovalFromInternalReview"
      @open-reject-modal="isRejectReasonModalOpen = true"
      @close-reject-modal="isRejectReasonModalOpen = false"
      @confirm-rejection="confirmInternalRejection"
      @update:rejection-reason="rejectionReason = $event"
    />
  </AppLayout>
</template>
