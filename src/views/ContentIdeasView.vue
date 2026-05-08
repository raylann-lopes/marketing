<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Bookmark,
  Eye,
  Filter,
  Lightbulb,
  Plus,
  RefreshCw,
  Search,
  Sparkles,
  ThumbsDown,
  X,
} from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import { useFeedback } from '@/lib/feedback'

type IdeaStatus = 'SUGGESTED' | 'SAVED' | 'DISMISSED' | 'CONVERTED'
type IdeaPriority = 'ALTA' | 'MEDIA' | 'BAIXA'
type SortMode = 'ALL' | 'RECENT' | 'VIEWS'

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
const selectedStatus = ref<IdeaStatus | 'Todos'>('Todos')
const sortMode = ref<SortMode>('ALL')
const activeIdeaId = ref<number | null>(null)

const ideas = ref<ContentIdea[]>([
  {
    id: 1,
    client: 'Clínica Aurora',
    niche: 'Saúde e estética',
    format: 'Reels',
    priority: 'ALTA',
    title: 'Antes de marcar um procedimento, entenda o que muda no resultado',
    previewTitle: 'Isso muda o resultado antes de qualquer procedimento',
    theme: 'Educação rápida com quebra de objeção',
    objective: 'Aumentar confiança e gerar conversas no direct',
    reason:
      'Conteúdos curtos com explicação simples e CTA de avaliação aparecem com forte retenção no nicho.',
    sourceSignal: '#esteticaavancada · #cuidadoscomapele · vídeos educativos',
    profile: '@clinica.aurora',
    postedAt: '17 de abr.',
    viewsLabel: '2.4M',
    viewsCount: 2400000,
    cardClass: 'bg-gradient-to-br from-indigo-500 to-violet-600',
    status: 'SUGGESTED',
  },
  {
    id: 2,
    client: 'Imobiliária Norte',
    niche: 'Imobiliário',
    format: 'Carrossel',
    priority: 'ALTA',
    title: '5 detalhes que fazem um imóvel parecer mais caro nas fotos',
    previewTitle: 'O erro que todo corretor comete no anúncio',
    theme: 'Dicas práticas para compradores e proprietários',
    objective: 'Atrair leads de avaliação e venda de imóveis',
    reason:
      'Posts de checklist performam bem porque geram salvamentos e abrem conversa consultiva.',
    sourceSignal: '#mercadoimobiliario · #decoracaodeinteriores · checklists',
    profile: '@imobiliaria.norte',
    postedAt: '19 de abr.',
    viewsLabel: '1.9M',
    viewsCount: 1900000,
    cardClass: 'bg-gradient-to-br from-pink-500 to-rose-500',
    status: 'SAVED',
  },
  {
    id: 3,
    client: 'Bella Fit',
    niche: 'Fitness',
    format: 'Stories',
    priority: 'MEDIA',
    title: 'Desafio de 7 dias para voltar à rotina sem exageros',
    previewTitle: 'Por que você ainda está travado na rotina',
    theme: 'Sequência interativa com enquete diária',
    objective: 'Reativar audiência e gerar respostas nos stories',
    reason:
      'Desafios curtos e progressivos tendem a aumentar interação sem exigir produção pesada.',
    sourceSignal: '#vidasaudavel · #treinoemcasa · enquetes',
    profile: '@bellafit.studio',
    postedAt: '16 de abr.',
    viewsLabel: '3.2M',
    viewsCount: 3200000,
    cardClass: 'bg-gradient-to-br from-cyan-500 to-teal-500',
    status: 'SUGGESTED',
  },
  {
    id: 4,
    client: 'Café Jardim',
    niche: 'Gastronomia',
    format: 'Reels',
    priority: 'MEDIA',
    title: 'O caminho do café até a mesa em 20 segundos',
    previewTitle: 'Acordei às 4h por 30 dias e aprendi isso',
    theme: 'Bastidor sensorial do preparo',
    objective: 'Valorizar experiência e aumentar desejo de visita',
    reason:
      'Bastidores com cortes rápidos e close no produto têm boa resposta visual para negócios locais.',
    sourceSignal: '#cafeteria · #cafesespeciais · bastidores',
    profile: '@cafejardim',
    postedAt: '20 de abr.',
    viewsLabel: '980K',
    viewsCount: 980000,
    cardClass: 'bg-gradient-to-br from-emerald-500 to-green-600',
    status: 'CONVERTED',
  },
  {
    id: 5,
    client: 'Studio Forma',
    niche: 'Arquitetura',
    format: 'Carrossel',
    priority: 'BAIXA',
    title: 'Erros comuns que deixam a sala menor visualmente',
    previewTitle: 'Como fazer 10k em atenção sem parecer anúncio',
    theme: 'Conteúdo educativo com imagens de apoio',
    objective: 'Gerar autoridade e pedidos de orçamento',
    reason: 'Comparativos antes/depois geram leitura rápida e reforçam percepção de especialidade.',
    sourceSignal: '#arquiteturadeinteriores · #salapequena · antes e depois',
    profile: '@studioforma.arq',
    postedAt: '22 de abr.',
    viewsLabel: '742K',
    viewsCount: 742000,
    cardClass: 'bg-gradient-to-br from-orange-500 to-red-500',
    status: 'SUGGESTED',
  },
  {
    id: 6,
    client: 'Clínica Aurora',
    niche: 'Saúde e estética',
    format: 'Feed',
    priority: 'MEDIA',
    title: 'O que ninguém te explica sobre constância nos cuidados',
    previewTitle: 'Joguei fora esse hábito e minha pele respondeu',
    theme: 'Post institucional com autoridade leve',
    objective: 'Educar sem promessa exagerada e reforçar recorrência',
    reason:
      'Temas de rotina e manutenção ajudam a criar agenda de retorno sem parecer oferta direta.',
    sourceSignal: '#skincareroutine · #cuidadosdiarios · posts educativos',
    profile: '@clinica.aurora',
    postedAt: '23 de abr.',
    viewsLabel: '1.1M',
    viewsCount: 1100000,
    cardClass: 'bg-gradient-to-br from-red-500 to-rose-600',
    status: 'DISMISSED',
  },
])

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
  { id: 'VIEWS', label: 'Mais views' },
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

function setIdeaStatus(id: number, status: IdeaStatus) {
  ideas.value = ideas.value.map((idea) => (idea.id === id ? { ...idea, status } : idea))

  if (status === 'SAVED') feedback.success('Ideia salva na lista mockada.')
  if (status === 'DISMISSED') feedback.info('Ideia descartada nesta prévia.')
  if (status === 'CONVERTED') feedback.success('Ideia marcada como convertida em demanda.')
}

function resetFilters() {
  searchTerm.value = ''
  selectedClient.value = 'Todos'
  selectedNiche.value = 'Todos'
  selectedStatus.value = 'Todos'
  sortMode.value = 'ALL'
}
</script>

<template>
  <AppLayout v-model:search="searchTerm" topbar-placeholder="Buscar ideias, clientes ou nichos...">
    <div class="space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="flex items-center gap-2">
          <div
            class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary"
          >
            <Lightbulb class="h-5 w-5" />
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h1 class="text-2xl font-bold text-gray-900">Ideias Virais</h1>
              <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Demo</span>
            </div>
            <p class="text-sm text-gray-500">
              Inspire-se em ganchos de Reels e adapte para cada cliente.
            </p>
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <Button variant="outline" class="gap-2">
            <RefreshCw class="h-4 w-4" />
            Atualizar mock
          </Button>
          <Button class="gap-2">
            <Sparkles class="h-4 w-4" />
            Nova coleta
          </Button>
        </div>
      </div>

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
            <label class="mb-1 block text-xs font-semibold uppercase text-gray-500"
              >Busca local</label
            >
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
          <p class="text-sm font-semibold text-gray-700">
            {{ filteredIdeas.length }} ideias encontradas
          </p>
          <p class="text-xs text-gray-400">Clique no card para abrir a demanda</p>
        </div>

        <div
          class="grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-7"
        >
          <button
            v-for="idea in filteredIdeas"
            :key="idea.id"
            type="button"
            class="group mx-auto w-full max-w-[190px] text-left focus:outline-none"
            @click="openIdea(idea.id)"
          >
            <div
              :class="[
                'relative aspect-[9/16] overflow-hidden rounded-lg p-2 text-white shadow-sm transition-all group-hover:-translate-y-0.5 group-hover:shadow-md group-focus:ring-2 group-focus:ring-primary/30',
                idea.cardClass,
              ]"
            >
              <div
                class="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(255,255,255,0.22),transparent_34%)]"
              />
              <div class="relative flex h-full flex-col">
                <div class="flex items-center justify-end">
                  <span
                    class="rounded-full bg-white/18 px-1.5 py-0.5 text-[9px] font-semibold text-white/90 backdrop-blur"
                  >
                    {{ idea.format }}
                  </span>
                </div>

                <div class="flex flex-1 items-center justify-center px-1 text-center">
                  <h2
                    class="text-[13px] font-extrabold leading-tight tracking-normal text-white drop-shadow-sm md:text-sm"
                  >
                    {{ idea.previewTitle }}
                  </h2>
                </div>

                <div class="mb-3 flex items-center justify-between gap-1">
                  <span
                    class="inline-flex items-center gap-0.5 rounded-full bg-black/28 px-1.5 py-0.5 text-[9px] font-bold text-white backdrop-blur"
                  >
                    <Eye class="h-2.5 w-2.5" />
                    {{ idea.viewsLabel }}
                  </span>
                  <span
                    class="shrink-0 whitespace-nowrap rounded-full bg-white/90 px-1.5 py-0.5 text-[8px] font-bold leading-none text-gray-700"
                  >
                    {{ statusLabels[idea.status] }}
                  </span>
                </div>
              </div>
            </div>

            <div class="mt-0.5 px-0.5 leading-tight">
              <p class="truncate text-[11px] font-semibold text-gray-800">{{ idea.profile }}</p>
              <p class="text-[10px] text-gray-400">{{ idea.postedAt }}</p>
            </div>
          </button>
        </div>

        <Card v-if="filteredIdeas.length === 0" class="p-8 text-center">
          <Lightbulb class="mx-auto mb-2 h-8 w-8 text-gray-300" />
          <p class="text-sm font-semibold text-gray-700">Nenhuma ideia encontrada.</p>
          <p class="mt-1 text-sm text-gray-500">
            Ajuste os filtros para visualizar outros exemplos mockados.
          </p>
        </Card>
      </section>
    </div>

    <div
      v-if="activeIdea"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-950/50 p-4 backdrop-blur-sm"
      @click.self="closeIdea"
    >
      <section
        class="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-gray-200 bg-white shadow-2xl"
      >
        <div
          class="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-gray-100 bg-white px-5 py-4"
        >
          <div>
            <div class="mb-2 flex flex-wrap items-center gap-2">
              <Badge :variant="statusVariants[activeIdea.status]">{{
                statusLabels[activeIdea.status]
              }}</Badge>
              <span
                :class="[
                  'rounded-full border px-2.5 py-0.5 text-xs font-semibold',
                  priorityClasses[activeIdea.priority],
                ]"
              >
                {{ activeIdea.priority }}
              </span>
              <span
                class="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-semibold text-gray-600"
              >
                <Eye class="h-3.5 w-3.5" />
                {{ activeIdea.viewsLabel }}
              </span>
            </div>
            <h2 class="text-xl font-bold leading-tight text-gray-900">{{ activeIdea.title }}</h2>
            <p class="mt-1 text-sm text-gray-500">
              {{ activeIdea.client }} · {{ activeIdea.profile }}
            </p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            class="shrink-0"
            aria-label="Fechar janela"
            @click="closeIdea"
          >
            <X class="h-5 w-5" />
          </Button>
        </div>

        <div class="space-y-4 p-5">
          <div :class="['rounded-2xl p-6 text-center text-white', activeIdea.cardClass]">
            <p class="text-sm font-semibold uppercase tracking-wide text-white/70">Gancho viral</p>
            <p class="mx-auto mt-3 max-w-md text-3xl font-extrabold leading-tight text-white">
              {{ activeIdea.previewTitle }}
            </p>
          </div>

          <div class="grid gap-3 md:grid-cols-2">
            <div class="rounded-xl border border-gray-100 bg-gray-50 p-3">
              <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Tema</p>
              <p class="mt-1 text-sm font-semibold text-gray-900">{{ activeIdea.theme }}</p>
            </div>
            <div class="rounded-xl border border-gray-100 bg-gray-50 p-3">
              <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">
                Objetivo
              </p>
              <p class="mt-1 text-sm font-semibold text-gray-900">{{ activeIdea.objective }}</p>
            </div>
          </div>

          <div class="rounded-xl border border-primary/10 bg-primary/5 p-4">
            <p class="text-[11px] font-semibold uppercase tracking-wide text-primary">
              Ideia para produção
            </p>
            <p class="mt-2 text-sm leading-6 text-gray-700">{{ activeIdea.reason }}</p>
          </div>

          <div class="rounded-xl border border-gray-100 p-4">
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">
              Sinal usado pela IA
            </p>
            <p class="mt-2 text-sm text-gray-600">{{ activeIdea.sourceSignal }}</p>
          </div>
        </div>

        <div class="flex flex-wrap justify-end gap-2 border-t border-gray-100 bg-gray-50 px-5 py-4">
          <Button
            variant="outline"
            size="sm"
            class="gap-2"
            @click="setIdeaStatus(activeIdea.id, 'SAVED')"
          >
            <Bookmark class="h-4 w-4" />
            Salvar
          </Button>
          <Button
            variant="outline"
            size="sm"
            class="gap-2"
            @click="setIdeaStatus(activeIdea.id, 'DISMISSED')"
          >
            <ThumbsDown class="h-4 w-4" />
            Descartar
          </Button>
          <Button size="sm" class="gap-2" @click="setIdeaStatus(activeIdea.id, 'CONVERTED')">
            <Plus class="h-4 w-4" />
            Converter em demanda
          </Button>
        </div>
      </section>
    </div>
  </AppLayout>
</template>
