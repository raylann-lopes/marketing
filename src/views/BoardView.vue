<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MoreHorizontal, Plus, CalendarDays, X, ChevronDown, Pencil, Trash2, Sparkles, Upload, MessageSquare, Check } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Button from '@/components/ui/Button.vue'
import { postService, type Post } from '@/services/postService'
import { clientService, type Client } from '@/services/clientService'
import { approvalService } from '@/services/approvalService'
import { mediaService } from '@/services/mediaService'
import { getCurrentUserId } from '@/lib/api'
import draggable from 'vuedraggable'
import { z } from 'zod'

type BoardColumn = {
  id: string
  label: string
  color: string
  cards: Post[]
}

const columns = ref<BoardColumn[]>([
  { id: 'DEMAND', label: 'Demanda', color: 'bg-gray-400', cards: [] },
  { id: 'IN_PRODUCTION', label: 'Em Produção', color: 'bg-blue-500', cards: [] },
  { id: 'WAITING_APPROVAL', label: 'Aguardando Aprovação', color: 'bg-orange-400', cards: [] },
  { id: 'FINISHED', label: 'Finalizado', color: 'bg-green-500', cards: [] },
  { id: 'PUBLISHED', label: 'Publicado', color: 'bg-purple-600', cards: [] },
])

const clients = ref<Client[]>([])
const isModalOpen = ref(false)
const isSubmitting = ref(false)
const fieldErrors = ref<Record<string, string>>({})
const postToEdit = ref<Post | null>(null)

// AI Approval Modal State
const isApprovalModalOpen = ref(false)
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

function getClientName(clientId: string | number) {
  const client = clients.value.find(c => String(c.id) === String(clientId))
  return client ? client.name : `ID: ${clientId}`
}

function openApprovalModal(post: Post) {
  selectedPostForApproval.value = post
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
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !selectedPostForApproval.value) return

  const post = selectedPostForApproval.value
  const clientId = (post as unknown as { client: { id: number } }).client?.id ?? post.clientId
  if (!clientId) { alert('Post sem cliente associado.'); return }
  const postId = post.id

  approvalData.value.isUploading = true
  try {
    // 1. Obtém URL presigned do backend
    const { uploadUrl, s3Key } = await mediaService.getUploadUrl(
      clientId, postId!, file.name, file.type
    )
    // 2. Faz upload direto ao S3
    await mediaService.uploadToS3(uploadUrl, file)
    // 3. Armazena a chave S3 e preview local
    approvalData.value.artS3Key = s3Key
    approvalData.value.artName = file.name
    approvalData.value.artPreviewUrl = URL.createObjectURL(file)
  } catch (e: unknown) {
    alert('Erro no upload: ' + (e instanceof Error ? e.message : 'Tente novamente.'))
  } finally {
    approvalData.value.isUploading = false
  }
}

async function generateAICaption() {
  if (!selectedPostForApproval.value) return
  approvalData.value.isGenerating = true
  
  // Simulating AI generation based on post context
  setTimeout(() => {
    const post = selectedPostForApproval.value!
    approvalData.value.caption = `🚀 NOVIDADE NO AR!\n\n${post.title}\n\nTema: ${post.theme}\nObjetivo: ${post.objective}\n\nO que achou desse conteúdo? Me conta aqui nos comentários! 👇 #Criatividade #AgenciaNorth`
    approvalData.value.isGenerating = false
  }, 1500)
}

async function sendToClient() {
  if (!selectedPostForApproval.value) return
  if (!approvalData.value.artS3Key) {
    alert('Faça o upload da arte antes de enviar.')
    return
  }

  approvalData.value.isSending = true
  try {
    await approvalService.create({
      postId: selectedPostForApproval.value.id!,
      artS3Key: approvalData.value.artS3Key,
      artName: approvalData.value.artName,
      caption: approvalData.value.caption
    })
    isApprovalModalOpen.value = false
    await fetchInitialData()
  } catch (e: unknown) {
    alert('Erro ao salvar aprovação: ' + (e instanceof Error ? e.message : 'Tente novamente.'))
  } finally {
    approvalData.value.isSending = false
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
      alert('Erro: O post não possui um cliente associado no banco. O board será resetado.')
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
        client: { id: Number(clientId) },
        user: { id: getUserOrFallback() }
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
      client: { id: Number(newPost.value.clientId) },
      user: { id: getUserOrFallback() }
    }

    if (postToEdit.value) {
      await postService.update(postToEdit.value.id!, payload as unknown as Post)
    } else {
      await postService.create(payload as unknown as Post)
    }

    await fetchInitialData()
    isModalOpen.value = false
  } catch (e: unknown) {
    alert('Erro ao salvar post: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
  } finally {
    isSubmitting.value = false
  }
}

async function handleDeletePost(post: Post) {
  if (!confirm(`Excluir "${post.title}"? Esta ação não pode ser desfeita.`)) return
  try {
    await postService.delete(post.id!)
    await fetchInitialData()
  } catch (e: unknown) {
    alert('Erro ao excluir: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
  }
}

const route = useRoute()

function getUserOrFallback(): number {
  const id = getCurrentUserId()
  if (id) return id
  const stored = localStorage.getItem('userId') || sessionStorage.getItem('userId')
  return stored ? Number(stored) : 1
}

// Watch for clientId query param from ClientsView suggestion button
watch(() => route.query.clientId, (clientId) => {
  if (clientId && clients.value.length > 0) {
    const clientExists = clients.value.some(c => String(c.id) === String(clientId))
    if (clientExists && !isModalOpen.value) {
      newPost.value.clientId = String(clientId)
      openAddModal('DEMAND')
    }
  }
})

onMounted(async () => {
  await fetchInitialData()

  const cid = route.query.clientId
  if (cid && clients.value.length > 0 && !isModalOpen.value) {
    const clientExists = clients.value.some(c => String(c.id) === String(cid))
    if (clientExists) {
      newPost.value.clientId = String(cid)
      openAddModal('DEMAND')
    }
  }
})

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const d = new Date(dateString)
  return `${d.getDate()} / ${d.getMonth() + 1}`
}
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
            @change="(evt: { added?: { element: Post } }) => handleBoardChange(evt, col.id)"
            class="flex-1 space-y-3 overflow-y-auto pr-1 custom-scrollbar min-h-0"
            ghost-class="opacity-50"
            drag-class="rotate-2"
          >
            <template #item="{ element: card }">
              <div class="bg-white rounded-xl border border-gray-100 p-4 shadow-sm hover:shadow-md transition-all cursor-grab active:cursor-grabbing group">
                <div class="flex items-start justify-between mb-2">
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-full text-primary bg-primary/10 uppercase leading-relaxed">
                    {{ getClientName((card as unknown as { client: number }).client || card.clientId) }}
                  </span>
                  <!-- Ações: visíveis ao hover -->
                  <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
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
                <p class="text-sm font-semibold text-gray-800 leading-snug">{{ card.title }}</p>
                <p class="text-xs text-gray-400 mt-1 line-clamp-1">{{ card.theme }}</p>
                
                <!-- Botão de Aprovação visível apenas na coluna de Produção -->
                <button 
                  v-if="col.id === 'IN_PRODUCTION'"
                  @click.stop="openApprovalModal(card)"
                  class="mt-3 w-full py-1.5 bg-primary/5 hover:bg-primary/10 text-primary text-[10px] font-bold rounded-lg border border-primary/20 flex items-center justify-center gap-1.5 transition-all"
                >
                  <Sparkles class="w-3 h-3" />
                  PREPARAR APROVAÇÃO
                </button>

                <div class="flex items-center justify-between mt-4">
                  <div v-if="card.scheduledAt" class="flex items-center gap-1 text-[10px] font-medium text-gray-400">
                    <CalendarDays class="w-3 h-3" />
                    {{ formatDate(card.scheduledAt) }}
                  </div>
                  <Avatar v-if="card.userId" :name="'U' + card.userId" size="sm" class="w-6 h-6 text-[10px]" />
                  <div v-else class="w-6 h-6 rounded-full bg-gray-100 flex items-center justify-center text-[10px] text-gray-400 border border-gray-200">-</div>
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
              <h2 class="text-xl font-bold text-gray-900">Preparar Aprovação</h2>
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
              <img v-if="approvalData.artPreviewUrl" :src="approvalData.artPreviewUrl" class="absolute inset-0 w-full h-full object-cover animate-in fade-in duration-500" />
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
                <Button variant="outline" class="bg-white border-none text-xs">Trocar Arquivo</Button>
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
                Ao enviar, a arte e a legenda serão disparadas para o WhatsApp do cliente vinculado para aprovação.
              </p>
            </div>
          </div>
        </div>

        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-4">
          <Button variant="outline" class="flex-1 h-12 rounded-xl font-bold" @click="isApprovalModalOpen = false">Cancelar</Button>
          <Button
            class="flex-[2] h-12 rounded-xl font-bold gap-2 shadow-lg shadow-primary/20"
            :disabled="approvalData.isSending || approvalData.isUploading || !approvalData.caption || !approvalData.artS3Key"
            @click="sendToClient"
          >
            <template v-if="approvalData.isSending">
              <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
              ENVIANDO...
            </template>
            <template v-else>
              <Check class="w-5 h-5" />
              ENVIAR PARA O CLIENTE
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
