<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Filter, Lightbulb, Loader2, RefreshCw, Search, Sparkles } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import { useFeedback } from '@/lib/feedback'
import {
  contentIdeaService,
  type ContentIdea as ApiContentIdea,
  type ContentIdeaStatus,
} from '@/services/contentIdeaService'
import { clientService, type Client } from '@/services/clientService'

import IdeaCard from '@/components/ideas/IdeaCard.vue'
import IdeaModal from '@/components/ideas/IdeaModal.vue'

type IdeaStatus = ContentIdeaStatus
type IdeaPriority = 'ALTA' | 'MEDIA' | 'BAIXA'
type SortMode = 'ALL' | 'RECENT' | 'VIEWS'

// View model consumido por IdeaCard/IdeaModal
type ContentIdea = {
  id: number
  client: string
  niche: string
  format: string
  priority: IdeaPriority
  title: string
  previewTitle: string
  theme: string
  objective: string
  reason: string
  sourceSignal: string
  profile: string
  postedAt: string
  viewsLabel: string
  viewsCount: number
  cardClass: string
  status: IdeaStatus
}

const feedback = useFeedback()

const searchTerm = ref('')
const selectedClient = ref('Todos')
const selectedNiche = ref('Todos')
// Abre a página listando apenas as sugestões novas — salvas/descartadas/
// convertidas ficam acessíveis pelo filtro de status
const selectedStatus = ref<IdeaStatus | 'Todos'>('SUGGESTED')
const sortMode = ref<SortMode>('ALL')
const activeIdeaId = ref<number | null>(null)

const ideas = ref<ContentIdea[]>([])
const isLoading = ref(false)
const isCollecting = ref(false)
const showCollectPanel = ref(false)
const clients = ref<Client[]>([])
const collectClientId = ref<number | string | ''>('')

const cardPalette = [
  'bg-gradient-to-br from-indigo-500 to-violet-600',
  'bg-gradient-to-br from-pink-500 to-rose-500',
  'bg-gradient-to-br from-cyan-500 to-teal-500',
  'bg-gradient-to-br from-emerald-500 to-green-600',
  'bg-gradient-to-br from-orange-500 to-red-500',
  'bg-gradient-to-br from-red-500 to-rose-600',
  'bg-gradient-to-br from-blue-500 to-indigo-600',
  'bg-gradient-to-br from-fuchsia-500 to-purple-600',
]

const formatLabels: Record<string, string> = {
  REELS: 'Reels',
  CAROUSEL: 'Carrossel',
  STORIES: 'Stories',
  FEED: 'Feed',
}

const priorityLabels: Record<string, IdeaPriority> = {
  HIGH: 'ALTA',
  MEDIUM: 'MEDIA',
  LOW: 'BAIXA',
}

function formatViews(score: number | null): string {
  if (!score || score <= 0) return '—'
  if (score >= 1_000_000) return `${(score / 1_000_000).toFixed(1).replace('.0', '')}M`
  if (score >= 1_000) return `${(score / 1_000).toFixed(1).replace('.0', '')}K`
  return String(Math.round(score))
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
  } catch {
    return iso
  }
}

function toViewModel(idea: ApiContentIdea): ContentIdea {
  return {
    id: idea.id,
    client: idea.clientName ?? '—',
    niche: idea.clientNiche ?? '—',
    format: formatLabels[idea.format] ?? idea.format,
    priority: priorityLabels[idea.priority] ?? 'MEDIA',
    title: idea.title,
    previewTitle: idea.hook || idea.title,
    theme: idea.theme ?? '—',
    objective: idea.objective ?? '—',
    reason: idea.reason ?? '—',
    sourceSignal: idea.signalSummary || idea.sourceTerms || '—',
    profile: idea.clientName ?? '—',
    postedAt: formatDate(idea.createdAt),
    viewsLabel: formatViews(idea.engagementScore),
    viewsCount: idea.engagementScore ?? 0,
    cardClass: cardPalette[idea.id % cardPalette.length]!,
    status: idea.status,
  }
}

async function fetchIdeas() {
  isLoading.value = true
  try {
    const data = await contentIdeaService.getAll()
    ideas.value = data.map(toViewModel)
  } catch {
    feedback.error('Erro ao carregar ideias de conteúdo.')
  } finally {
    isLoading.value = false
  }
}

async function openCollectPanel() {
  showCollectPanel.value = !showCollectPanel.value
  if (showCollectPanel.value && clients.value.length === 0) {
    try {
      clients.value = await clientService.getAll()
    } catch {
      feedback.error('Erro ao carregar clientes.')
    }
  }
}

async function runCollect() {
  if (!collectClientId.value) {
    feedback.warning('Selecione um cliente para a coleta.')
    return
  }
  isCollecting.value = true
  try {
    const created = await contentIdeaService.collect(collectClientId.value)
    feedback.success(`Coleta concluída: ${created.length} nova(s) ideia(s).`)
    showCollectPanel.value = false
    await fetchIdeas()
  } catch (e) {
    feedback.error(e instanceof Error ? e.message : 'Erro na coleta de ideias.')
  } finally {
    isCollecting.value = false
  }
}

const statusLabels: Record<IdeaStatus, string> = {
  SUGGESTED: 'Sugerida',
  SAVED: 'Salva',
  DISMISSED: 'Descartada',
  CONVERTED: 'Convertida',
}

const statusVariants: Record<IdeaStatus, 'secondary' | 'success' | 'destructive' | 'purple'> = {
  SUGGESTED: 'secondary',
  SAVED: 'success',
  DISMISSED: 'destructive',
  CONVERTED: 'purple',
}

const priorityClasses: Record<IdeaPriority, string> = {
  ALTA: 'bg-red-50 text-red-700 border-red-100',
  MEDIA: 'bg-amber-50 text-amber-700 border-amber-100',
  BAIXA: 'bg-gray-50 text-gray-600 border-gray-200',
}

const sortOptions: { id: SortMode; label: string }[] = [
  { id: 'ALL', label: 'Todos' },
  { id: 'RECENT', label: 'Mais recentes' },
  { id: 'VIEWS', label: 'Mais engajamento' },
]

const clientOptions = computed(() => ['Todos', ...new Set(ideas.value.map((idea) => idea.client))])
const nicheOptions = computed(() => ['Todos', ...new Set(ideas.value.map((idea) => idea.niche))])
const statusOptions: Array<IdeaStatus | 'Todos'> = [
  'Todos',
  'SUGGESTED',
  'SAVED',
  'CONVERTED',
  'DISMISSED',
]

const filteredIdeas = computed(() => {
  const normalizedSearch = searchTerm.value.trim().toLowerCase()

  const result = ideas.value.filter((idea) => {
    const matchesSearch =
      !normalizedSearch ||
      idea.title.toLowerCase().includes(normalizedSearch) ||
      idea.previewTitle.toLowerCase().includes(normalizedSearch) ||
      idea.client.toLowerCase().includes(normalizedSearch) ||
      idea.niche.toLowerCase().includes(normalizedSearch) ||
      idea.theme.toLowerCase().includes(normalizedSearch)

    const matchesClient = selectedClient.value === 'Todos' || idea.client === selectedClient.value
    const matchesNiche = selectedNiche.value === 'Todos' || idea.niche === selectedNiche.value
    const matchesStatus = selectedStatus.value === 'Todos' || idea.status === selectedStatus.value

    return matchesSearch && matchesClient && matchesNiche && matchesStatus
  })

  if (sortMode.value === 'RECENT') {
    return [...result].sort((a, b) => b.id - a.id)
  }

  if (sortMode.value === 'VIEWS') {
    return [...result].sort((a, b) => b.viewsCount - a.viewsCount)
  }

  return result
})

const activeIdea = computed(
  () => ideas.value.find((idea) => idea.id === activeIdeaId.value) || null,
)

function openIdea(id: number) {
  activeIdeaId.value = id
}

function closeIdea() {
  activeIdeaId.value = null
}

async function setIdeaStatus(id: number, status: IdeaStatus) {
  try {
    await contentIdeaService.updateStatus(id, status)
    ideas.value = ideas.value.map((idea) => (idea.id === id ? { ...idea, status } : idea))

    if (status === 'SAVED') feedback.success('Ideia salva.')
    if (status === 'DISMISSED') feedback.info('Ideia descartada.')
    if (status === 'CONVERTED') feedback.success('Demanda criada no board de produção.')
  } catch {
    feedback.error('Erro ao atualizar o status da ideia.')
  }
}

function resetFilters() {
  searchTerm.value = ''
  selectedClient.value = 'Todos'
  selectedNiche.value = 'Todos'
  // Volta ao padrão da página (sugestões novas), não a "Todos"
  selectedStatus.value = 'SUGGESTED'
  sortMode.value = 'ALL'
}

onMounted(fetchIdeas)
</script>

<template>
  <AppLayout v-model:search="searchTerm" topbar-placeholder="Buscar ideias, clientes ou nichos...">
    <div class="space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="flex items-center gap-2">
          <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Lightbulb class="h-5 w-5" />
          </div>
          <div>
            <h1 class="text-2xl font-bold text-gray-900">Ideias Virais</h1>
            <p class="text-sm text-gray-500">Inspire-se em ganchos de Reels e adapte para cada cliente.</p>
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <Button variant="outline" class="gap-2" :disabled="isLoading" @click="fetchIdeas">
            <RefreshCw :class="['h-4 w-4', isLoading ? 'animate-spin' : '']" />
            Atualizar
          </Button>
          <Button class="gap-2" @click="openCollectPanel">
            <Sparkles class="h-4 w-4" />
            Nova coleta
          </Button>
        </div>
      </div>

      <Card v-if="showCollectPanel" class="p-4">
        <div class="flex flex-wrap items-end gap-3">
          <div class="min-w-[240px] flex-1">
            <label class="mb-1 block text-xs font-semibold uppercase text-gray-500">
              Cliente para coleta
            </label>
            <select
              v-model="collectClientId"
              class="h-10 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="" disabled>Selecione um cliente...</option>
              <option v-for="client in clients" :key="client.id" :value="client.id">
                {{ client.name }} — {{ client.niche }}
              </option>
            </select>
          </div>
          <Button class="gap-2" :disabled="isCollecting" @click="runCollect">
            <Loader2 v-if="isCollecting" class="h-4 w-4 animate-spin" />
            <Sparkles v-else class="h-4 w-4" />
            {{ isCollecting ? 'Coletando... (pode levar minutos)' : 'Iniciar coleta' }}
          </Button>
        </div>
        <p class="mt-2 text-xs text-gray-400">
          A coleta busca posts virais do nicho na Apify e usa IA para gerar até 5 ideias adaptadas ao cliente.
        </p>
      </Card>

      <div class="flex flex-wrap gap-2">
        <button
          v-for="option in sortOptions"
          :key="option.id"
          type="button"
          :class="[
            'rounded-full border px-5 py-2 text-sm font-semibold transition-colors',
            sortMode === option.id
              ? 'border-primary bg-primary text-white shadow-sm'
              : 'border-gray-200 bg-white text-gray-500 hover:border-primary/30 hover:text-primary',
          ]"
          @click="sortMode = option.id"
        >
          {{ option.label }}
        </button>
      </div>

      <Card class="p-4">
        <div class="flex flex-wrap items-end gap-3">
          <div class="min-w-[220px] flex-1">
            <label class="mb-1 block text-xs font-semibold uppercase text-gray-500">Busca local</label>
            <div class="relative">
              <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                v-model="searchTerm"
                type="text"
                class="h-10 w-full rounded-lg border border-gray-200 bg-gray-50 pl-9 pr-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                placeholder="Título, cliente, nicho..."
              />
            </div>
          </div>

          <div class="min-w-[180px]">
            <label class="mb-1 block text-xs font-semibold uppercase text-gray-500">Cliente</label>
            <select
              v-model="selectedClient"
              class="h-10 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option v-for="client in clientOptions" :key="client" :value="client">
                {{ client }}
              </option>
            </select>
          </div>

          <div class="min-w-[180px]">
            <label class="mb-1 block text-xs font-semibold uppercase text-gray-500">Nicho</label>
            <select
              v-model="selectedNiche"
              class="h-10 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option v-for="niche in nicheOptions" :key="niche" :value="niche">{{ niche }}</option>
            </select>
          </div>

          <div class="min-w-[170px]">
            <label class="mb-1 block text-xs font-semibold uppercase text-gray-500">Status</label>
            <select
              v-model="selectedStatus"
              class="h-10 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option v-for="status in statusOptions" :key="status" :value="status">
                {{ status === 'Todos' ? 'Todos' : statusLabels[status] }}
              </option>
            </select>
          </div>

          <Button variant="outline" class="gap-2" @click="resetFilters">
            <Filter class="h-4 w-4" />
            Limpar
          </Button>
        </div>
      </Card>

      <section class="space-y-3">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-gray-700">{{ filteredIdeas.length }} ideias encontradas</p>
          <p class="text-xs text-gray-400">Clique no card para abrir a demanda</p>
        </div>

        <Card v-if="isLoading" class="p-8 text-center">
          <Loader2 class="mx-auto mb-2 h-8 w-8 animate-spin text-gray-300" />
          <p class="text-sm font-semibold text-gray-700">Carregando ideias...</p>
        </Card>

        <template v-else>
          <div class="grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-7">
            <IdeaCard
              v-for="idea in filteredIdeas"
              :key="idea.id"
              :idea="idea"
              :status-label="statusLabels[idea.status]"
              @click="openIdea(idea.id)"
            />
          </div>

          <Card v-if="filteredIdeas.length === 0" class="p-8 text-center">
            <Lightbulb class="mx-auto mb-2 h-8 w-8 text-gray-300" />
            <p class="text-sm font-semibold text-gray-700">
              {{ selectedStatus === 'SUGGESTED' && ideas.length > 0 ? 'Nenhuma sugestão nova.' : 'Nenhuma ideia encontrada.' }}
            </p>
            <p class="mt-1 text-sm text-gray-500">
              {{ selectedStatus === 'SUGGESTED' && ideas.length > 0
                ? 'Todas as ideias já foram tratadas — veja salvas e convertidas no filtro de status.'
                : 'Use "Nova coleta" para buscar tendências virais e gerar ideias com IA.' }}
            </p>
          </Card>
        </template>
      </section>
    </div>

    <IdeaModal
      v-if="activeIdea"
      :idea="activeIdea"
      :status-label="statusLabels[activeIdea.status]"
      :status-variant="statusVariants[activeIdea.status]"
      :priority-class="priorityClasses[activeIdea.priority]"
      @close="closeIdea"
      @set-status="setIdeaStatus"
    />
  </AppLayout>
</template>
