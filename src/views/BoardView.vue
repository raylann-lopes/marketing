<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MoreHorizontal, Plus, X, ChevronDown, Pencil, Trash2, Sparkles, Upload, MessageSquare, Check } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Button from '@/components/ui/Button.vue'
import { postService, type Post, type PostStatus } from '@/services/postService'
import { clientService, type Client } from '@/services/clientService'
import { approvalService, type PostApproval } from '@/services/approvalService'
import { mediaService } from '@/services/mediaService'
import { getCurrentUserId, apiFetch } from '@/lib/api'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import draggable from 'vuedraggable'
import { z } from 'zod'

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

const columns = ref<BoardColumn[]>([
  { id: 'DEMAND', label: 'Demanda', color: 'bg-gray-400', cards: [] },
  { id: 'IN_PRODUCTION', label: 'Em Produção', color: 'bg-blue-500', cards: [] },
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
  artPreviewUrl: '',   // URL local para preview (URL.createObjectURL)
  isUploading: false,
  isGenerating: false,
  isSending: false
})
const fileInputRef = ref<HTMLInputElement | null>(null)
const feedback = useFeedback()
const approvalsByPostId = ref<Record<string, PostApproval>>({})
const sendingApprovalByPostId = ref<Record<string, boolean>>({})
const expandedCardsByPostId = ref<Record<string, boolean>>({})
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

function getClientName(clientId: string | number) {
  const client = clients.value.find(c => String(c.id) === String(clientId))
  return client ? client.name : `ID: ${clientId}`
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
      // A API retorna um MediaUrlResponseDTO que contém o campo mediaUrl
      const res = await apiFetch<{ mediaUrl: string }>(`/api/media/art-url?postId=${post.id}`)
      if (res && res.mediaUrl) {
        approvalData.value.artPreviewUrl = res.mediaUrl
      }
    } catch (err) {
      console.warn('Erro ao carregar preview da arte existente:', err)
    }
  }
}

function isVideo(url: string, filename?: string) {
  const check = (str: string) => {
    if (!str) return false
    const clean = (str.split('?')[0] ?? '').toLowerCase()
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mov')
  }
  return check(url) || (filename ? check(filename) : false)
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !selectedPostForApproval.value) return

  const postId = selectedPostForApproval.value.id

  approvalData.value.isUploading = true
  try {
    // 1. Obtém URL presigned do backend
    const { uploadUrl, s3Key } = await mediaService.getUploadUrl(
      postId!, file.name, file.type
    )
    // 2. Faz upload direto ao S3
    await mediaService.uploadToS3(uploadUrl, file)
    // 3. Armazena a chave S3 e preview local
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
    // Passamos a chave S3 que está em memória (seja carregada ou acabada de subir)
    const res = await postService.generateCaption(
      selectedPostForApproval.value.id, 
      approvalData.value.artS3Key
    )
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

function getPostId(post: Post) {
  return String(post.id ?? '')
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

function getPostClientId(post: Post) {
  const rawClient = (post as unknown as { client?: unknown }).client
  if (typeof rawClient === 'number' || typeof rawClient === 'string') return Number(rawClient)
  if (rawClient && typeof rawClient === 'object') {
    const rawClientId = (rawClient as { id?: unknown }).id
    if (rawClientId !== undefined && rawClientId !== null) return Number(rawClientId)
  }
  if (post.clientId !== undefined && post.clientId !== null) return Number(post.clientId)
  return null
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
  if (approval.whatsappResponseText?.trim()) {
    return { text: 'Cliente respondeu', className: 'text-violet-700' }
  }
  if (approval.whatsappSentAt) {
    return { text: `Enviada em ${formatSentAt(approval.whatsappSentAt)}`, className: 'text-emerald-700' }
  }
  return { text: 'Pronta para envio', className: 'text-blue-700' }
}

function formatSentAt(sentAt?: string) {
  if (!sentAt) return ''
  const date = new Date(sentAt)
  if (Number.isNaN(date.getTime())) return sentAt
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function isSendingApproval(post: Post) {
  const postId = getPostId(post)
  return Boolean(postId && sendingApprovalByPostId.value[postId])
}

async function handleSendApprovalFromFinished(post: Post, isResend = false) {
  const postId = getPostId(post)
  if (!postId || !post.id) return
  if (isSendingApproval(post)) return

  const approval = getApprovalByPost(post)
  if (!approval?.artS3Key || !approval.artName) {
    feedback.warning('Esta demanda ainda não possui aprovação preparada. Prepare a aprovação primeiro.')
    openApprovalModal(post)
    return
  }

  sendingApprovalByPostId.value = { ...sendingApprovalByPostId.value, [postId]: true }
  try {
    const completion = await mediaService.completeUpload(post.id, approval.artS3Key, approval.artName)
    try {
      const refreshedApproval = await approvalService.getByPostId(post.id)
      approvalsByPostId.value = {
        ...approvalsByPostId.value,
        [postId]: refreshedApproval
      }
    } catch {
      // fallback: mantém estado anterior se o refresh pontual falhar
    }

    if (completion.webhookDispatched) {
      feedback.success(isResend ? 'Aprovação reenviada ao cliente.' : 'Aprovação enviada ao cliente.')
    } else {
      feedback.warning('Aprovação processada, mas o webhook do n8n não foi disparado.')
    }
  } catch (e: unknown) {
    feedback.error(`Erro ao ${isResend ? 'reenviar' : 'enviar'} aprovação: ${getErrorMessage(e, 'Tente novamente.')}`)
  } finally {
    sendingApprovalByPostId.value = { ...sendingApprovalByPostId.value, [postId]: false }
  }
}

const newPost = ref({
  clientId: '',
  title: '',
  theme: '',
  objective: '',
  status: 'DEMAND',
  scheduledAt: new Date().toISOString().slice(0, 16)
})

const postSchema = z.object({
  clientId: z.union([z.string(), z.number()]).refine(v => String(v).length > 0, 'Selecione um cliente'),
  title: z.string().min(3, 'Título muito curto'),
  theme: z.string().min(3, 'Tema obrigatório'),
  objective: z.string().min(5, 'Descreva o objetivo'),
  scheduledAt: z.string().min(10, 'Data inválida')
})

const search = ref('')

async function fetchInitialData() {
  try {
    const [postsData, clientsData] = await Promise.all([
      postService.getAll(),
      clientService.getAll()
    ])
    
    interface ApiResponse<T> { data?: T[] }
    const posts = Array.isArray(postsData) ? postsData : ((postsData as unknown as ApiResponse<Post>).data || [])
    clients.value = Array.isArray(clientsData) ? clientsData : ((clientsData as unknown as ApiResponse<Client>).data || [])
    
    columns.value.forEach(col => col.cards = [])
    
    posts.forEach((post: Post) => {
      const col = columns.value.find(c => c.id === post.status)
      if (col) {
        col.cards.push(post)
      } else {
        columns.value[0]?.cards.push(post)
      }
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

// Compute filtered columns based on search
const filteredColumns = computed(() => {
  if (!search.value) return columns.value
  const term = search.value.toLowerCase()
  return columns.value.map(col => ({
    ...col,
    cards: col.cards.filter(card => {
      const titleMatches = card.title.toLowerCase().includes(term)
      const raw = (card as Record<string, unknown>).client
      const cid = raw && typeof raw === 'object'
        ? String((raw as { id?: unknown }).id ?? '')
        : card.clientId
      const clientMatches = getClientName(cid || '-').toLowerCase().includes(term)
      return titleMatches || clientMatches
    })
  }))
})

async function handleBoardChange(evt: { added?: { element: Post } }, columnId: string) {
  if (evt.added) {
    const post = evt.added.element
    const clientId = (post as unknown as { client: number }).client || post.clientId
    
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
  newPost.value = {
    clientId: '',
    title: '',
    theme: '',
    objective: '',
    status: status,
    scheduledAt: new Date().toISOString().slice(0, 16)
  }
  fieldErrors.value = {}
  isModalOpen.value = true
}

function openEditModal(post: Post) {
  postToEdit.value = post
  newPost.value = {
    clientId: String((post as unknown as { client: number }).client || post.clientId),
    title: post.title,
    theme: post.theme,
    objective: post.objective,
    status: post.status,
    scheduledAt: post.scheduledAt.substring(0, 16)
  }
  fieldErrors.value = {}
  isModalOpen.value = true
}

async function handleSavePost() {
  fieldErrors.value = {}
  const result = postSchema.safeParse(newPost.value)
  
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
      title: newPost.value.title,
      theme: newPost.value.theme,
      objective: newPost.value.objective,
      status: newPost.value.status,
      scheduledAt: newPost.value.scheduledAt.length === 16 ? newPost.value.scheduledAt + ":00" : newPost.value.scheduledAt,
      clientId: Number(newPost.value.clientId),
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
    feedback.success('Demanda excluída com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao excluir: ${getErrorMessage(e)}`)
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
      newPost.value.clientId = clientId
      openAddModal('DEMAND')
      return
    }
  }

  if (route.query.new) {
    openAddModal('DEMAND')
  }
}

watch(() => [route.query.clientId, route.query.new], () => {
  openModalFromRoute()
})

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
        v-for="col in filteredColumns"
        :key="col.id"
        class="flex flex-col w-[308px] shrink-0 bg-gray-50/50 rounded-2xl border border-gray-100 overflow-hidden h-full max-h-full"
      >
        <div :class="['h-1.5 w-full shrink-0', col.color]" />
        <div class="p-3 flex flex-col flex-1 overflow-hidden">
          <div class="flex items-center justify-between mb-4 px-1 shrink-0">
            <div class="flex items-center gap-2">
              <span class="text-sm font-bold text-gray-700 uppercase tracking-wider">{{ col.label }}</span>
              <span class="text-xs font-bold bg-white text-gray-400 border border-gray-100 rounded-full px-2 py-0.5 shadow-sm">
                {{ col.cards.length }}
              </span>
            </div>
            <button class="p-1 hover:bg-white rounded-lg transition-colors"><MoreHorizontal class="w-4 h-4 text-gray-400" /></button>
          </div>

          <draggable
            v-model="col.cards"
            group="posts"
            item-key="id"
            :disabled="!!search"
            @change="(evt: { added?: { element: Post } }) => handleBoardChange(evt, col.id)"
            class="flex-1 space-y-3 overflow-y-auto pr-1 custom-scrollbar min-h-0"
            ghost-class="opacity-50"
            drag-class="rotate-2"
          >
            <template #item="{ element: card }">
              <div class="bg-white rounded-xl border border-gray-100 p-4 shadow-sm hover:shadow-md transition-all cursor-grab active:cursor-grabbing group">
                <div class="flex items-start justify-between gap-2 mb-3">
                  <div class="flex flex-wrap items-center gap-1.5">
                    <span :class="['text-[10px] font-bold px-2 py-0.5 rounded-full uppercase leading-relaxed', getColumnTheme(col.id).clientBadge]">
                      {{ getClientName((card as unknown as { client: number }).client || card.clientId) }}
                    </span>
                  </div>
                  <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      @click.stop="toggleCardExpanded(card)"
                      class="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary/10 transition-colors"
                      :title="isCardExpanded(card) ? 'Ocultar dados' : 'Expandir dados'"
                    >
                      <ChevronDown :class="['w-3.5 h-3.5 transition-transform', isCardExpanded(card) ? 'rotate-180' : '']" />
                    </button>
                    <button
                      @click.stop="openEditModal(card)"
                      class="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary/10 transition-colors"
                      title="Editar"
                    >
                      <Pencil class="w-3.5 h-3.5" />
                    </button>
                    <button
                      @click.stop="handleDeletePost(card)"
                      class="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                      title="Excluir"
                    >
                      <Trash2 class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <p class="text-sm font-semibold text-gray-800 leading-snug line-clamp-2">{{ card.title }}</p>
                <p class="text-xs text-gray-500 mt-1 line-clamp-1">{{ card.theme || 'Sem tema definido' }}</p>

                <div v-if="isCardExpanded(card)" class="mt-3 grid grid-cols-2 gap-2 text-[10px]">
                  <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
                    <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Etapa</p>
                    <p class="mt-0.5 font-semibold text-gray-700 truncate">{{ getColumnLabel(col.id) }}</p>
                  </div>
                  <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
                    <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Prioridade</p>
                    <p
                      :class="[
                        'mt-0.5 font-semibold truncate',
                        getPriorityInfo(card).label === 'ALTA'
                          ? 'text-red-700'
                          : getPriorityInfo(card).label === 'MÉDIA'
                            ? 'text-amber-700'
                            : 'text-emerald-700'
                      ]"
                    >
                      {{ getPriorityInfo(card).label }}
                    </p>
                  </div>
                  <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
                    <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Responsável</p>
                    <p class="mt-0.5 font-semibold text-gray-700 truncate">{{ getResponsibleLabel(card) }}</p>
                  </div>
                  <div class="rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
                    <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Prazo</p>
                    <p :class="['mt-0.5 font-semibold truncate', isOverdue(card) ? 'text-red-600' : 'text-gray-700']">
                      {{ formatDueDate(card.scheduledAt) }}
                    </p>
                  </div>
                  <div class="col-span-2 rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
                    <div class="flex items-center justify-between gap-2">
                      <div class="min-w-0">
                        <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Tipo</p>
                        <p class="mt-0.5 font-semibold text-gray-700 truncate">{{ getPostTypeLabel(card) }}</p>
                      </div>
                      <Avatar v-if="card.userId" :name="'U' + card.userId" size="sm" class="w-6 h-6 shrink-0 text-[10px]" />
                      <div v-else class="w-6 h-6 shrink-0 rounded-full bg-gray-100 flex items-center justify-center text-[10px] text-gray-400 border border-gray-200">-</div>
                    </div>
                  </div>
                  <div class="col-span-2 rounded-lg border border-gray-100 bg-gray-50 px-2 py-1.5">
                    <p class="text-[9px] font-semibold uppercase tracking-wide text-gray-400">Aprovação</p>
                    <p :class="['mt-0.5 font-semibold truncate', getApprovalStatusLabel(card).className]">
                      {{ getApprovalStatusLabel(card).text }}
                    </p>
                  </div>
                </div>

                <button 
                  v-if="col.id === 'IN_PRODUCTION'"
                  @click.stop="openApprovalModal(card)"
                  :class="['mt-3 w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all', getColumnTheme(col.id).primaryButton]"
                >
                  <Sparkles class="w-3 h-3" />
                  {{ getApprovalByPost(card) ? 'EDITAR APROVAÇÃO' : 'PREPARAR APROVAÇÃO' }}
                </button>

                <div v-if="col.id === 'FINISHED'" class="mt-3">
                  <button
                    v-if="!getApprovalByPost(card)"
                    @click.stop="openApprovalModal(card)"
                    :class="['w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all', getColumnTheme(col.id).primaryButton]"
                  >
                    <Sparkles class="w-3 h-3" />
                    PREPARAR APROVAÇÃO
                  </button>

                  <button
                    v-else-if="!getApprovalByPost(card)?.whatsappSentAt"
                    @click.stop="handleSendApprovalFromFinished(card)"
                    :disabled="isSendingApproval(card)"
                    :class="['w-full py-1.5 text-[10px] font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all disabled:opacity-60 disabled:cursor-not-allowed', getColumnTheme(col.id).primaryButton]"
                  >
                    <template v-if="isSendingApproval(card)">ENVIANDO...</template>
                    <template v-else>ENVIAR PARA O CLIENTE</template>
                  </button>

                  <div
                    v-else
                    :class="['flex items-center justify-between gap-2 rounded-lg px-2 py-1.5', getColumnTheme(col.id).softContainer]"
                  >
                    <span :class="['text-[10px] font-medium truncate', getColumnTheme(col.id).softText]">
                      Enviado em {{ formatSentAt(getApprovalByPost(card)?.whatsappSentAt) }}
                    </span>
                    <button
                      @click.stop="handleSendApprovalFromFinished(card, true)"
                      :disabled="isSendingApproval(card)"
                      :class="['shrink-0 rounded-md px-2 py-0.5 text-[10px] font-bold transition-colors disabled:opacity-60 disabled:cursor-not-allowed', getColumnTheme(col.id).ghostButton]"
                    >
                      <template v-if="isSendingApproval(card)">ENVIANDO...</template>
                      <template v-else>REENVIAR</template>
                    </button>
                  </div>
                </div>
              </div>
            </template>
          </draggable>

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

    <!-- Modal de Aprovação (IA + Upload) -->
    <div v-if="isApprovalModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden animate-in fade-in zoom-in duration-300">
        <div class="flex items-center justify-between p-6 border-b border-gray-100 bg-gray-50/50">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
              <Sparkles class="w-5 h-5" />
            </div>
            <div>
              <h2 class="text-xl font-bold text-gray-900">{{ existingApprovalId ? 'Editar Aprovação' : 'Preparar Aprovação' }}</h2>
              <p class="text-xs text-gray-500 font-medium uppercase tracking-wider">Demanda: {{ selectedPostForApproval?.title }}</p>
            </div>
          </div>
          <button @click="isApprovalModalOpen = false" class="p-2 hover:bg-white rounded-xl text-gray-400 transition-colors shadow-sm"><X class="w-5 h-5" /></button>
        </div>

        <div class="p-8 grid grid-cols-2 gap-8">
          <!-- Coluna 1: Upload da Arte -->
          <div class="space-y-4">
            <label class="text-xs font-bold text-gray-400 uppercase tracking-widest">Arte do Post</label>
            <input ref="fileInputRef" type="file" accept="image/*,video/*" class="hidden" @change="handleFileSelect" />
            <div
              class="relative aspect-square rounded-2xl border-2 border-dashed border-gray-200 bg-gray-50 flex flex-col items-center justify-center gap-3 group hover:border-primary/40 hover:bg-primary/5 transition-all cursor-pointer overflow-hidden"
              @click="fileInputRef?.click()"
            >
              <template v-if="approvalData.artPreviewUrl">
                <video 
                  v-if="isVideo(approvalData.artPreviewUrl, approvalData.artName)"
                  :src="approvalData.artPreviewUrl" 
                  class="absolute inset-0 w-full h-full object-cover animate-in fade-in duration-500"
                  autoplay muted loop
                ></video>
                <img 
                  v-else
                  :src="approvalData.artPreviewUrl" 
                  class="absolute inset-0 w-full h-full object-cover animate-in fade-in duration-500" 
                />
              </template>
              
              <div v-else-if="approvalData.isUploading" class="flex flex-col items-center gap-2 text-primary">
                <div class="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
                <span class="text-xs font-bold">Enviando...</span>
              </div>
              <div v-else class="flex flex-col items-center gap-2 text-gray-400 group-hover:text-primary transition-colors">
                <div class="w-12 h-12 rounded-full bg-white shadow-sm flex items-center justify-center">
                  <Upload class="w-6 h-6" />
                </div>
                <span class="text-xs font-bold">Clique para upload</span>
                <span class="text-[10px] text-gray-300">JPG, PNG, MP4</span>
              </div>
              <div v-if="approvalData.artPreviewUrl" class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <Button variant="outline" class="bg-white border-none text-xs">Trocar {{ isVideo(approvalData.artPreviewUrl, approvalData.artName) ? 'Vídeo' : 'Arquivo' }}</Button>
              </div>
            </div>
            <p v-if="approvalData.artName" class="text-[10px] text-gray-400 truncate text-center">{{ approvalData.artName }}</p>
          </div>

          <!-- Coluna 2: Legenda e IA -->
          <div class="space-y-4 flex flex-col">
            <div class="flex items-center justify-between">
              <label class="text-xs font-bold text-gray-400 uppercase tracking-widest">Legenda Sugerida</label>
              <button 
                @click="generateAICaption"
                :disabled="approvalData.isGenerating"
                class="flex items-center gap-1.5 text-[10px] font-bold text-primary hover:text-primary/80 transition-colors disabled:opacity-50"
              >
                <Sparkles :class="['w-3 h-3', approvalData.isGenerating ? 'animate-pulse' : '']" />
                {{ approvalData.isGenerating ? 'GERANDO...' : 'GERAR COM IA' }}
              </button>
            </div>
            
            <textarea 
              v-model="approvalData.caption"
              rows="8"
              class="w-full p-4 rounded-2xl border border-gray-100 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm leading-relaxed resize-none flex-1"
              placeholder="Aguardando geração da IA ou digite aqui..."
            ></textarea>
            
            <div class="bg-blue-50/50 p-3 rounded-xl border border-blue-100">
              <div class="flex items-center gap-2 text-blue-600 mb-1">
                <MessageSquare class="w-3.5 h-3.5" />
                <span class="text-[10px] font-bold uppercase tracking-wider">Fluxo N8n</span>
              </div>
              <p class="text-[10px] text-blue-600/70 leading-normal">
                Ao salvar, a demanda vai para Finalizado. Depois, use o botão da coluna Finalizado para enviar ao cliente.
              </p>
            </div>
          </div>
        </div>

        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-4">
          <Button variant="outline" class="flex-1 h-12 rounded-xl font-bold" @click="isApprovalModalOpen = false">Cancelar</Button>
          <Button
            class="flex-[2] h-12 rounded-xl font-bold gap-2 shadow-lg shadow-primary/20"
            :disabled="approvalData.isSending || approvalData.isUploading || !approvalData.caption || !approvalData.artS3Key"
            @click="saveApproval"
          >
            <template v-if="approvalData.isSending">
              <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
              SALVANDO...
            </template>
            <template v-else>
              <Check class="w-5 h-5" />
              {{ existingApprovalId ? 'SALVAR ALTERAÇÕES' : 'SALVAR APROVAÇÃO' }}
            </template>
          </Button>
        </div>
      </div>
    </div>

    <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between p-6 border-b border-gray-100">
          <h2 class="text-xl font-bold text-gray-900">{{ postToEdit ? 'Editar Demanda' : 'Nova Demanda' }}</h2>
          <button @click="isModalOpen = false" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
        </div>
        <div class="p-6 space-y-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
            <div class="relative">
              <select v-model="newPost.clientId" :class="['w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer', fieldErrors.clientId ? 'border-red-500' : 'border-gray-200']">
                <option value="">Selecione o cliente</option>
                <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
            </div>
            <p v-if="fieldErrors.clientId" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.clientId }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Título da Demanda</label>
            <input v-model="newPost.title" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.title ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Campanha de Black Friday" />
            <p v-if="fieldErrors.title" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.title }}</p>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Tema</label>
              <input v-model="newPost.theme" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.theme ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Vendas / Social Media" />
              <p v-if="fieldErrors.theme" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.theme }}</p>
            </div>
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Prazo / Entrega</label>
              <input v-model="newPost.scheduledAt" type="datetime-local" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.scheduledAt ? 'border-red-500' : 'border-gray-200']" />
              <p v-if="fieldErrors.scheduledAt" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.scheduledAt }}</p>
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Objetivo da Demanda</label>
            <textarea v-model="newPost.objective" rows="3" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.objective ? 'border-red-500' : 'border-gray-200']" placeholder="O que precisamos entregar neste projeto?"></textarea>
            <p v-if="fieldErrors.objective" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.objective }}</p>
          </div>
        </div>
        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
          <Button variant="outline" class="flex-1" @click="isModalOpen = false">Cancelar</Button>
          <Button class="flex-1" :disabled="isSubmitting" @click="handleSavePost">
            {{ isSubmitting ? 'Salvando...' : (postToEdit ? 'Atualizar Demanda' : 'Criar Demanda') }}
          </Button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
